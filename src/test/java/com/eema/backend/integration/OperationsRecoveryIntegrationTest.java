package com.eema.backend.integration;

import com.eema.backend.service.*;
import com.eema.backend.config.PayoutDestinationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.*;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.net.*;
import java.net.http.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Disposable database/object recovery using the packaged application, never a shared local stack. */
class OperationsRecoveryIntegrationTest extends InquirySupport {
    static final Path ROOT=temp();
    static Path temp(){try{return Files.createTempDirectory("eema-ops-recovery-");}catch(Exception e){throw new RuntimeException(e);}}
    static String key(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getEncoder().encodeToString(b);}
    static final String ENCRYPTION=key(),FINGERPRINT=key();
    static final int PORT=18091,DEBUG_PORT=18092;
    static final List<Process> children=new ArrayList<>();
    @DynamicPropertySource static void storage(DynamicPropertyRegistry r){r.add("app.documents.storage-dir",()->ROOT.resolve("source/private").toString());r.add("app.storage.upload-dir",()->ROOT.resolve("source/public").toString());r.add("app.payout-destination.enabled",()->true);r.add("app.payout-destination.encryption-key",()->ENCRYPTION);r.add("app.payout-destination.fingerprint-key",()->FINGERPRINT);}
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @AfterAll static void cleanup()throws Exception{for(var p:children)stop(p);remove(ROOT);}
    @Autowired FinancialDocumentService documents;
    @Autowired PayoutDestinationCrypto crypto;
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    final List<String> checks=new ArrayList<>();
    record App(Process process,Path log){}
    static void remove(Path root)throws Exception{if(Files.exists(root))try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    static void stop(Process p)throws Exception{if(p.isAlive()){p.destroy();if(!p.waitFor(30,TimeUnit.SECONDS)){p.destroyForcibly();assertTrue(p.waitFor(10,TimeUnit.SECONDS));}}}
    void command(String... args)throws Exception{var r=POSTGRES.execInContainer(args);assertEquals(0,r.getExitCode(),"Disposable PostgreSQL command failed: "+args[0]);}
    JdbcTemplate database(String name){return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl().replace("/eema_it","/"+name),POSTGRES.getUsername(),POSTGRES.getPassword()));}
    void createDb(String name)throws Exception{command("createdb","-U",POSTGRES.getUsername(),name);}
    App start(String db,Path storage,boolean dev,String label)throws Exception{return start(db,storage,dev,label,null);}
    App start(String db,Path storage,boolean dev,String label,String providerUrl)throws Exception{
        Files.createDirectories(storage.resolve("public"));Files.createDirectories(storage.resolve("private"));
        Path log=Path.of("/tmp/ops-app-"+label+"-"+UUID.randomUUID()+".log");
        var args=new ArrayList<>(List.of(System.getProperty("java.home")+"/bin/java","-Xmx384m","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:"+DEBUG_PORT,"-jar",Path.of("target/backend-0.0.1-SNAPSHOT.jar").toAbsolutePath().toString(),"--server.port="+PORT,"--app.storage.provider=local","--app.storage.upload-dir="+storage.resolve("public"),"--app.documents.storage-dir="+storage.resolve("private"),"--app.resend.enabled=false","--app.mollie.fee-reconciliation-enabled=false"));
        if(providerUrl!=null){args.remove("--app.resend.enabled=false");args.add("--app.resend.enabled=true");args.add("--app.resend.api-url="+providerUrl);args.add("--app.notification-outbox.initial-delay-ms=1000");args.add("--app.notification-outbox.poll-delay-ms=1000");}
        if(dev)args.add("--spring.profiles.active=local");
        var pb=new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile());var env=pb.environment();env.clear();env.put("LANG","C.UTF-8");env.put("SPRING_DATASOURCE_URL",POSTGRES.getJdbcUrl().replace("/eema_it","/"+db));env.put("SPRING_DATASOURCE_USERNAME",POSTGRES.getUsername());env.put("SPRING_DATASOURCE_PASSWORD",POSTGRES.getPassword());env.put("JWT_SECRET",app.jwt().secret());env.put("PAYOUT_DATA_ENCRYPTION_KEY",ENCRYPTION);env.put("PAYOUT_DATA_FINGERPRINT_KEY",FINGERPRINT);
        if(providerUrl!=null){env.put("RESEND_API_KEY","synthetic-loopback-only-key");env.put("RESEND_FROM_EMAIL","qa@example.test");}
        Process process=pb.start();children.add(process);long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(240);
        while(System.nanoTime()<end){assertTrue(process.isAlive(),"Packaged application exited; inspect "+log);try{if(send("GET","/actuator/health",null,null).statusCode()==200)return new App(process,log);}catch(Exception ignored){}Thread.sleep(150);}
        fail("Packaged application health timeout; inspect "+log);return null;
    }
    HttpResponse<byte[]> send(String method,String path,JsonNode actor,Object body)throws Exception{
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+PORT+"/api"+path)).timeout(Duration.ofSeconds(20));if(actor!=null)req.header("Authorization","Bearer "+access(actor));if(body!=null)req.header("Content-Type","application/json");req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)));return http.send(req.build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    JsonNode api(String method,String path,JsonNode actor,Object body,int status)throws Exception{var r=send(method,path,actor,body);assertEquals(status,r.statusCode(),method+" "+path);return r.body().length==0?json.nullNode():json.readTree(r.body());}
    Map<String,String> snapshot(JdbcTemplate db)throws Exception{
        var result=new TreeMap<String,String>();for(String table:db.queryForList("select tablename from pg_tables where schemaname='public' order by tablename",String.class)){assertTrue(table.matches("[a-z_]+"));String value=db.queryForObject("select coalesce(json_agg(t order by row_to_json(t)::text)::text,'[]') from "+table+" t",String.class);result.put(table,hash(value.getBytes(StandardCharsets.UTF_8)));}return result;
    }
    String constraints(JdbcTemplate db){return db.queryForObject("select string_agg(conname||':'||pg_get_constraintdef(oid),E'\\n' order by conname) from pg_constraint where connamespace='public'::regnamespace",String.class);}
    // pg_dump reparses varchar-array casts and flattens the incident CHECK's associative AND.
    // Normalize only those known equivalent spellings; retain all names, operators and operands.
    static String canonicalConstraints(String sql) {
        return sql.replaceAll("\\('([^']*)'::character varying\\)::text", "'$1'")
                .replaceAll("'([^']*)'::character varying", "'$1'")
                .replaceAll("\\(ARRAY\\[([^]]*)]\\)::text\\[\\]", "ARRAY[$1]")
                .replace("incident_reports_description_check:CHECK ((((char_length", "incident_reports_description_check:CHECK (((char_length")
                .replace("(char_length(btrim((description)::text)) <= 4000)) AND", "(char_length(btrim((description)::text)) <= 4000) AND");
    }
    static String hash(byte[] value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}
    static void copy(Path source,Path target)throws Exception{try(var paths=Files.walk(source)){for(Path p:paths.toList()){Path to=target.resolve(source.relativize(p));if(Files.isDirectory(p))Files.createDirectories(to);else Files.copy(p,to);}}}
    UUID paid()throws Exception{UUID b=create(venue(owner,60),scout,"09:00","10:00");jdbc.update("update bookings set status='CONFIRMED',paid_at=?,confirmed_at=?,total_paid_eur=venue_price_eur,mollie_payment_id=? where id=?",OffsetDateTime.now(clock),OffsetDateTime.now(clock),"tr_ops_"+b,b);return b;}

    @Test void packagedMigrationsBackupRestoreRestartAndReset()throws Exception{
        UUID paid=paid(),cancelled=paid(),pending=create(venue(owner,60),scout,"09:00","10:00"),afterCrash=create(venue(owner,60),scout,"09:00","10:00");jdbc.update("update bookings set status='CANCELLED' where id=?",cancelled);UUID inquiry=open(inquiryVenue(),scout);
        UUID claim=create(venue(owner,60),scout,"09:00","10:00");action(claim,"accept",owner,200);jdbc.update("update bookings set payment_creation_status='CREATING',payment_idempotency_key=?,payment_creation_started_at=? where id=?",UUID.randomUUID(),OffsetDateTime.now(clock),claim);
        UUID venue=UUID.fromString(detail(paid).path("venueId").asText());jdbc.update("update venues set name='QA OPS Recovery Studio' where id=?",venue);
        var picture=new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(picture,"png",bytes);
        var upload=mvc.perform(multipart("/api/upload/venue-photo").file(new MockMultipartFile("file","qa.png","image/png",bytes.toByteArray())).contextPath("/api").header("Authorization","Bearer "+access(owner))).andReturn().getResponse();assertEquals(200,upload.getStatus());String media=json.readTree(upload.getContentAsByteArray()).path("url").asText();jdbc.update("update venues set photo_paths=array[?] where id=?",media,venue);byte[] mediaBytes=Files.readAllBytes(ROOT.resolve("source/public").resolve(Path.of(media).getFileName()));
        UUID destination=UUID.randomUUID();String iban="DE89370400440532013000";jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values(?,?,'IBAN','EUR','QA OPS Bank',?,?,'3000','VERIFIED',now(),now(),?)",destination,id(owner),crypto.encrypt(iban),crypto.fingerprint(iban),id(admin));
        byte[] receipt=documents.bookingReceipt(paid,users.findById(id(scout)).orElseThrow()).content();
        var accounts=new HashMap<String,JsonNode>();for(var a:List.of(owner,scout,admin))accounts.put(a==owner?"OWNER":a==scout?"SCOUT":"ADMIN",call("login",Map.of("email",email(a),"password",QA_PASSWORD),200));
        App live=start("eema_it",ROOT.resolve("source"),false,"source");var before=snapshot(jdbc);
        if(Boolean.getBoolean("eema.ops.browser")){
            Path config=ROOT.resolve("browser.json"),ready=ROOT.resolve("browser-ready"),resume=ROOT.resolve("browser-resume");Files.writeString(config,json.writeValueAsString(Map.of("accounts",accounts,"bookingId",paid,"receiptHash",hash(receipt),"ready",ready.toString(),"resume",resume.toString())));Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            Process browser=new ProcessBuilder("node","scripts/qa/ops-browser.cjs",config.toString()).inheritIO().start();children.add(browser);waitFile(ready,browser,45);stop(live.process());live=start("eema_it",ROOT.resolve("source"),false,"restart");assertEquals(before,snapshot(jdbc));Files.writeString(resume,"ready");assertTrue(browser.waitFor(120,TimeUnit.SECONDS));assertEquals(0,browser.exitValue());Files.delete(config);checks.add("Browser scout/owner/admin deep links and immutable receipt survive actual backend restart");
        }else{stop(live.process());live=start("eema_it",ROOT.resolve("source"),false,"restart");assertEquals(before,snapshot(jdbc));}
        assertArrayEquals(receipt,send("GET","/bookings/"+paid+"/receipt",scout,null).body());checks.add("Ordinary packaged restart preserves every table hash and document bytes");
        // Provider is disabled: committed intents stay durable without dispatch. Kill the actual JVM.
        api("POST","/bookings/"+pending+"/accept",owner,null,200);
        assertEquals("AWAITING_PAYMENT",state(pending));assertTrue(audit(pending).contains("REQUEST_ACCEPTED"));
        String intent=jdbc.queryForObject("select row_to_json(n)::text from notification_outbox n where aggregate_id=? and notification_type='REQUEST_ACCEPTED'",String.class,pending);
        live.process().destroyForcibly();assertTrue(live.process().waitFor(15,TimeUnit.SECONDS));
        // Restart with a loopback HTTP provider; the old committed row must dispatch without a new domain event.
        UUID pendingNotification=jdbc.queryForObject("select id from notification_outbox where aggregate_id=? and notification_type='REQUEST_ACCEPTED'",UUID.class,pending);
        var delivered=new java.util.concurrent.CountDownLatch(1);
        var provider=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        provider.createContext("/emails",exchange->{
            exchange.getRequestBody().readAllBytes();String identity=exchange.getRequestHeaders().getFirst("Idempotency-Key");
            byte[] response="{\"id\":\"qa-local-provider-id\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
            if(("eema-notification-"+pendingNotification).equals(identity))delivered.countDown();
        });provider.start();
        try{
            live=start("eema_it",ROOT.resolve("source"),false,"after-crash","http://127.0.0.1:"+provider.getAddress().getPort()+"/emails");
            assertTrue(delivered.await(30,TimeUnit.SECONDS),"Restart did not dispatch pre-crash intent");
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
            while(System.nanoTime()<end && jdbc.queryForObject("select count(*) from notification_outbox where status in ('PENDING','PROCESSING')",Integer.class)>0)Thread.sleep(50);
            assertEquals(0,jdbc.queryForObject("select count(*) from notification_outbox where status in ('PENDING','PROCESSING')",Integer.class));
            assertEquals("DELIVERED",jdbc.queryForObject("select status from notification_outbox where id=?",String.class,pendingNotification));
            assertEquals("AWAITING_PAYMENT",state(pending));stop(live.process());
        }finally{provider.stop(0);}
        checks.add("Forced JVM death after acceptance retains pending intent; packaged restart initializes bounded scheduler and dispatches the pre-crash message through loopback HTTP with stable idempotency identity");
        var source=snapshot(jdbc);String expectedConstraints=constraints(jdbc);command("pg_dump","-U",POSTGRES.getUsername(),"-d","eema_it","-Fc","--no-owner","--no-acl","-f","/tmp/ops-backup.dump");createDb("ops_restore");command("pg_restore","-U",POSTGRES.getUsername(),"-d","ops_restore","--no-owner","--no-acl","/tmp/ops-backup.dump");copy(ROOT.resolve("source"),ROOT.resolve("restore"));
        Path secrets=ROOT.resolve("key-backup.properties");var keys=new Properties();keys.setProperty("encryption",ENCRYPTION);keys.setProperty("fingerprint",FINGERPRINT);try(var out=Files.newOutputStream(secrets)){keys.store(out,"Synthetic isolated QA keys only");}Files.setPosixFilePermissions(secrets,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));var recovered=new Properties();try(var in=Files.newInputStream(secrets)){recovered.load(in);}assertEquals(ENCRYPTION,recovered.getProperty("encryption"));assertEquals(FINGERPRINT,recovered.getProperty("fingerprint"));
        App restored=start("ops_restore",ROOT.resolve("restore"),false,"restore");var target=database("ops_restore");assertEquals(source,snapshot(target));assertEquals(canonicalConstraints(expectedConstraints),canonicalConstraints(constraints(target)));assertArrayEquals(receipt,send("GET","/bookings/"+paid+"/receipt",scout,null).body());assertEquals(403,send("GET","/bookings/"+paid+"/receipt",owner,null).statusCode());assertArrayEquals(mediaBytes,send("GET",media.substring(4),null,null).body());var restoredInquiries=api("GET","/availability-inquiries/mine",scout,null,200);assertTrue(java.util.stream.StreamSupport.stream(restoredInquiries.spliterator(),false).anyMatch(i->i.path("id").asText().equals(inquiry.toString())&&i.path("status").asText().equals("OPEN")));assertTrue(iban.equals(api("POST","/admin/payout-destinations/"+destination+"/reveal",admin,null,200).path("iban").asText()));assertEquals(crypto.fingerprint(iban),target.queryForObject("select iban_fingerprint from owner_payout_destinations where id=?",String.class,destination));
        var wrong=new PayoutDestinationCrypto(new PayoutDestinationProperties(true,key(),key()));org.springframework.test.util.ReflectionTestUtils.invokeMethod(wrong,"initialize");assertThrows(IllegalStateException.class,()->wrong.decrypt(target.queryForObject("select iban_encrypted from owner_payout_destinations where id=?",String.class,destination)));
        api("POST","/auth/refresh",null,Map.of("refreshToken",refresh(accounts.get("SCOUT"))),200);api("POST","/auth/refresh",null,Map.of("refreshToken",refresh(accounts.get("SCOUT"))),401);assertEquals(source,snapshot(jdbc));stop(restored.process());checks.add("pg_dump/restore preserves all table hashes, IDs, constraints, paid/cancelled/inquiry/policy/session/destination/document state, files and decryptability; restored session rotation and source isolation verified");
        createDb("ops_common");App common=start("ops_common",ROOT.resolve("common"),false,"common");var commonDb=database("ops_common");assertEquals(java.util.stream.IntStream.rangeClosed(1,14).boxed().toList(),commonDb.queryForList("select version::int from flyway_schema_history order by installed_rank",Integer.class));for(String table:List.of("users","venues","policy_versions","bookings"))assertEquals(0,commonDb.queryForObject("select count(*) from "+table,Integer.class));var empty=snapshot(commonDb);stop(common.process());common=start("ops_common",ROOT.resolve("common"),false,"common-repeat");assertEquals(empty,snapshot(commonDb));stop(common.process());checks.add("Packaged common-only V1–V14 and Hibernate validation start empty and restart idempotently");
        createDb("ops_dev");App dev=start("ops_dev",ROOT.resolve("dev"),true,"dev");var devDb=database("ops_dev");assertEquals(List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,100,101,102,103),devDb.queryForList("select version::int from flyway_schema_history order by installed_rank",Integer.class));int seeds=devDb.queryForObject("select count(*) from venues",Integer.class);assertTrue(seeds>0);assertEquals(0,devDb.queryForObject("select count(*) from policy_versions",Integer.class));api("POST","/auth/login",null,Map.of("email","owner@eema-dev.local","password","password123"),200);stop(dev.process());
        Files.writeString(ROOT.resolve("dev/public/reset-marker"),"QA");Files.writeString(ROOT.resolve("dev/private/reset-marker"),"QA");command("dropdb","-U",POSTGRES.getUsername(),"ops_dev");remove(ROOT.resolve("dev"));createDb("ops_dev");dev=start("ops_dev",ROOT.resolve("dev"),true,"dev-reset");assertEquals(seeds,database("ops_dev").queryForObject("select count(*) from venues",Integer.class));assertFalse(Files.exists(ROOT.resolve("dev/public/reset-marker")));assertFalse(Files.exists(ROOT.resolve("dev/private/reset-marker")));assertEquals(0,database("ops_dev").queryForObject("select count(*) from policy_versions",Integer.class));stop(dev.process());checks.add("Disposable local V1–V14/V100–V103 seed/login and DB/object-directory reset/reseed passed without touching source/shared data");
        verifyNoInteractions(mollie);Path evidence=Path.of("docs/qa/evidence/ops/recovery.json");Files.writeString(evidence,json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("result","PASS","checks",checks,"tableHashes",source,"receiptSha256",hash(receipt),"publicMediaSha256",hash(mediaBytes),"provider","No live provider; synthetic local fixtures and keys only"))+"\n");
    }
    static void waitFile(Path file,Process p,int seconds)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);while(!Files.exists(file)&&System.nanoTime()<end){assertTrue(p.isAlive());Thread.sleep(100);}assertTrue(Files.exists(file),"Browser restart barrier timeout");}
    static void waitLog(Path log,String id,String type)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);while(System.nanoTime()<end){if(Files.readAllLines(log).stream().anyMatch(s->s.contains(id)&&s.contains(type)))return;Thread.sleep(100);}fail("New notification completion barrier timeout");}
    static VirtualMachine attach()throws Exception{var connector=Bootstrap.virtualMachineManager().attachingConnectors().stream().filter(c->c.name().equals("com.sun.jdi.SocketAttach")).findFirst().orElseThrow();var args=connector.defaultArguments();args.get("hostname").setValue("127.0.0.1");args.get("port").setValue(Integer.toString(DEBUG_PORT));args.get("timeout").setValue("10000");return connector.attach(args);}
    static UUID bookingAt(BreakpointEvent hit)throws Exception{var event=(ObjectReference)hit.thread().frame(0).getArgumentValues().getFirst();var uuid=(ObjectReference)event.getValue(event.referenceType().fieldByName("bookingId"));return new UUID(((LongValue)uuid.getValue(uuid.referenceType().fieldByName("mostSigBits"))).value(),((LongValue)uuid.getValue(uuid.referenceType().fieldByName("leastSigBits"))).value());}
}
