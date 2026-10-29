package com.eema.backend.integration;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = false)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test-postgres")
@TestMethodOrder(MethodOrderer.Random.class)
class PostgresCriticalPathIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("eema_it").withUsername("eema").withPassword("eema");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl); r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword); r.add("app.jwt.secret", () -> "bG9jYWwtZGV2ZWxvcG1lbnQtb25seS1qd3Qtc2VjcmV0LTMyaA==");
    }

    @Autowired JdbcTemplate jdbc; @Autowired DataSource dataSource;
    @Autowired VenueRepository venues; @Autowired BookingRepository bookings; @Autowired PolicyVersionRepository policies;

    @BeforeEach void clean() { jdbc.execute("TRUNCATE TABLE users, policy_versions CASCADE"); }

    @Test void commonFlywayChainAndHibernateValidationStartFromEmptyDatabase() {
        List<Integer> versions=jdbc.queryForList("select version::int from flyway_schema_history where success order by installed_rank",Integer.class);
        assertTrue(versions.containsAll(List.of(1,2,3,4,5,6,7,8,9,10,11)), ()->"common versions="+versions);
        assertTrue(versions.stream().noneMatch(v->v>=100), ()->"development migration leaked: "+versions);
        assertEquals("bookings_no_overlapping_reservations", jdbc.queryForObject("select conname from pg_constraint where conname='bookings_no_overlapping_reservations'",String.class));
    }

    @Test void nativeEnumsAndProductionActivityArraysRoundTripThroughHibernate() {
        Fixture f=fixture();
        jdbc.update("update venues set status='PENDING'::venue_status, permitted_activities=array['FILM','PHOTO']::production_activity[] where id=?",f.venue);
        Venue venue=venues.findById(f.venue).orElseThrow();
        assertEquals(VenueStatus.PENDING,venue.getStatus()); assertEquals(List.of(ProductionActivity.FILM,ProductionActivity.PHOTO),venue.getPermittedActivities());
        UUID booking=insertBooking(f,"REQUESTED","09:00","10:00");
        assertEquals(BookingStatus.REQUESTED,bookings.findById(booking).orElseThrow().getStatus());
        assertEquals(RefundStatus.NONE,bookings.findById(booking).orElseThrow().getRefundStatus());
    }

    @Test void exclusionRejectsOneOverlappingBlockingInsert() throws Exception {
        Fixture f=fixture(); RaceResult result=race(
                c->insertBooking(c,f,"AWAITING_PAYMENT","09:00","12:00"),
                c->insertBooking(c,f,"CONFIRMED","10:00","11:00"));
        assertEquals(1,result.successes(),result::diagnostic); assertEquals(1,result.failures(),result::diagnostic);
        assertEquals(1,jdbc.queryForObject("select count(*) from bookings",Integer.class));
    }

    @Test void halfOpenBackToBackReservationsBothPersist() {
        Fixture f=fixture();insertBooking(f,"AWAITING_PAYMENT","09:00","12:00");insertBooking(f,"CONFIRMED","12:00","15:00");
        assertEquals(2,jdbc.queryForObject("select count(*) from bookings",Integer.class));
    }

    @Test void overlappingRequestedRowsCoexistButConcurrentAcceptanceCannotDoubleReserve() throws Exception {
        Fixture f=fixture();UUID a=insertBooking(f,"REQUESTED","09:00","12:00"),b=insertBooking(f,"REQUESTED","10:00","11:00");
        RaceResult result=race(c->updateStatus(c,a,"AWAITING_PAYMENT"),c->updateStatus(c,b,"AWAITING_PAYMENT"));
        assertEquals(1,result.successes(),result::diagnostic);
        assertEquals(1,jdbc.queryForObject("select count(*) from bookings where status='AWAITING_PAYMENT'",Integer.class));
        assertEquals(1,jdbc.queryForObject("select count(*) from bookings where status='REQUESTED'",Integer.class));
    }

    @Test void twoSkipLockedWorkersReceiveDisjointBoundedBookingBatches() throws Exception {
        Fixture f=fixture();for(int i=0;i<5;i++)insertBooking(f,"AWAITING_PAYMENT",String.format("0%d:00",i),String.format("0%d:30",i));
        SkipLockedResult r=skipLocked("select id from bookings where status='AWAITING_PAYMENT' order by created_at for update skip locked limit 3");
        assertTrue(Collections.disjoint(r.first,r.second),r::toString);assertEquals(5,new HashSet<UUID>(){{addAll(r.first);addAll(r.second);}}.size());assertTrue(r.first.size()<=3&&r.second.size()<=3);
    }

    @Test void inquiryAndPayoutSkipLockedQueriesAreActuallyDisjoint() throws Exception {
        Fixture f=fixture();UUID booking=insertBooking(f,"CONFIRMED","09:00","10:00");
        for(int i=0;i<4;i++)jdbc.update("insert into availability_inquiries(venue_id,scout_id,requested_date,requested_start_time,requested_end_time,message,status,offered_date,offered_start_time,offered_end_time,offer_expires_at,offered_at) values(?,?,current_date+1,'09:00','10:00','test','OFFERED',current_date+1,'09:00','10:00',now()-interval '1 minute',now())",f.venue,f.scout);
        SkipLockedResult inquiries=skipLocked("select id from availability_inquiries where status='OFFERED' and offer_expires_at<=now() order by offer_expires_at for update skip locked limit 2");
        assertTrue(Collections.disjoint(inquiries.first,inquiries.second));assertEquals(4,inquiries.first.size()+inquiries.second.size());
        jdbc.update("insert into owner_payouts(booking_id,owner_id,venue_id,gross_retained_amount_eur,commission_eur,provider_cost_eur,payout_amount_eur,status,eligible_at) values(?,?,?,100,20,0,80,'CALCULATED',now()-interval '1 minute')",booking,f.owner,f.venue);
        SkipLockedResult payouts=skipLocked("select id from owner_payouts where status in('CALCULATED','HELD') and eligible_at<=now() order by eligible_at for update skip locked limit 1");
        assertEquals(1,payouts.first.size()+payouts.second.size());assertTrue(Collections.disjoint(payouts.first,payouts.second));
    }

    @Test void refreshTokenRotationLockAllowsOnlyOneWinner() throws Exception {
        Fixture f=fixture();String hash="a".repeat(64);jdbc.update("insert into refresh_tokens(user_id,token_hash,expires_at) values(?,?,now()+interval '1 hour')",f.scout,hash);
        RaceResult r=race(c->rotate(c,hash),c->rotate(c,hash));assertEquals(1,r.successes(),r::diagnostic);
        assertTrue(jdbc.queryForObject("select revoked from refresh_tokens where token_hash=?",Boolean.class,hash));
    }

    @Test void payoutAndDocumentUniqueConstraintsHaveSingleAuthorities() throws Exception {
        Fixture f=fixture();UUID booking=insertBooking(f,"CONFIRMED","09:00","10:00");
        RaceResult payout=race(c->insertPayout(c,f,booking),c->insertPayout(c,f,booking));assertEquals(1,payout.successes(),payout::diagnostic);
        RaceResult receipt=race(c->insertReceipt(c,f,booking,"REC-1","private/a.pdf"),c->insertReceipt(c,f,booking,"REC-2","private/b.pdf"));assertEquals(1,receipt.successes(),receipt::diagnostic);
        assertEquals(1,jdbc.queryForObject("select count(*) from financial_documents where booking_id=?",Integer.class,booking));
    }

    @Test void verifiedDestinationPartialIndexAndReplacementSemanticsHold() throws Exception {
        Fixture f=fixture();UUID first=destination(f,"PENDING_VERIFICATION","A"),second=destination(f,"PENDING_VERIFICATION","B");
        RaceResult r=race(c->verifyDestination(c,first),c->verifyDestination(c,second));assertEquals(1,r.successes(),r::diagnostic);
        UUID winner=jdbc.queryForObject("select id from owner_payout_destinations where status='VERIFIED'",UUID.class);
        UUID loser=winner.equals(first)?second:first;
        jdbc.update("update owner_payout_destinations set status='REPLACED',replaced_at=now() where id=?",winner);
        jdbc.update("update owner_payout_destinations set status='VERIFIED',verified_at=now(),verified_by=? where id=?",f.owner,loser);
        assertEquals(1,jdbc.queryForObject("select count(*) from owner_payout_destinations where owner_id=? and status='VERIFIED'",Integer.class,f.owner));
    }

    @Test void readyPayoutDestinationSnapshotDoesNotFollowReplacement() {
        Fixture f=fixture();UUID booking=insertBooking(f,"CONFIRMED","09:00","10:00"),old=destination(f,"VERIFIED","A");insertPayout(null,f,booking);
        jdbc.update("update owner_payouts set status='READY',ready_at=now(),payout_destination_id=? where booking_id=?",old,booking);
        UUID replacement=destination(f,"PENDING_VERIFICATION","B");jdbc.update("update owner_payout_destinations set status='REPLACED',replaced_at=now() where id=?",old);jdbc.update("update owner_payout_destinations set status='VERIFIED',verified_at=now(),verified_by=? where id=?",f.owner,replacement);
        assertEquals(old,jdbc.queryForObject("select payout_destination_id from owner_payouts where booking_id=?",UUID.class,booking));
    }

    @Test void policyPublishRaceLeavesExactlyOneCurrentVersion() throws Exception {
        UUID a=draftPolicy("1"),b=draftPolicy("2");RaceResult r=race(c->publish(c,a),c->publish(c,b));assertEquals(1,r.successes(),r::diagnostic);
        assertEquals(1,jdbc.queryForObject("select count(*) from policy_versions where policy_type='TERMS_OF_SERVICE' and status='PUBLISHED'",Integer.class));
        assertEquals(2,jdbc.queryForObject("select count(*) from policy_versions",Integer.class));
        assertNotNull(policies.findByPolicyTypeAndStatus(PolicyType.TERMS_OF_SERVICE,PolicyStatus.PUBLISHED).orElseThrow().getContentHash());
    }

    @Test void policyAcceptanceBookingSnapshotStaysHistoricalAndIncidentHasNoFinancialSideEffects() {
        Fixture f=fixture();Map<String,UUID> current=new HashMap<>();for(String type:List.of("TERMS_OF_SERVICE","PRIVACY_POLICY","ACCEPTABLE_USE_POLICY","PRODUCTION_POLICY","INSURANCE_REQUIREMENTS")){UUID id=UUID.randomUUID();jdbc.update("insert into policy_versions(id,policy_type,version,title,effective_at,published_at,content,content_hash,status) values(?,?, '1','Approved fixture',now(),now(),'fixture',?,'PUBLISHED')",id,type,"f".repeat(64));jdbc.update("insert into policy_acceptances(user_id,policy_version_id) values(?,?)",f.scout,id);current.put(type,id);}
        UUID booking=insertBooking(f,"REQUESTED","09:00","10:00");for(String type:List.of("TERMS_OF_SERVICE","ACCEPTABLE_USE_POLICY","PRODUCTION_POLICY","INSURANCE_REQUIREMENTS"))jdbc.update("insert into booking_policy_versions values(?,?,?)",booking,type,current.get(type));
        List<UUID> before=jdbc.queryForList("select policy_version_id from booking_policy_versions where booking_id=? order by policy_type",UUID.class,booking);jdbc.update("update policy_versions set status='RETIRED' where policy_type='TERMS_OF_SERVICE'");draftPolicy("2");
        jdbc.update("insert into incident_reports(booking_id,reported_by_user_id,category,description) values(?,?, 'OTHER','Operational fixture')",booking,f.scout);
        assertEquals(before,jdbc.queryForList("select policy_version_id from booking_policy_versions where booking_id=? order by policy_type",UUID.class,booking));
        assertEquals("REQUESTED",jdbc.queryForObject("select status::text from bookings where id=?",String.class,booking));assertEquals("NONE",jdbc.queryForObject("select refund_status from bookings where id=?",String.class,booking));assertEquals(0,jdbc.queryForObject("select count(*) from owner_payouts where booking_id=?",Integer.class,booking));
    }

    private Fixture fixture(){UUID owner=user("OWNER"),scout=user("SCOUT"),venue=UUID.randomUUID();jdbc.update("insert into venues(id,owner_id,name,neighbourhood,size_m2,hourly_rate_eur,minimum_booking_minutes,status,permitted_activities,timezone,minimum_lead_time_minutes) values(?,?,'IT Venue','Test',100,100,60,'ACTIVE',array['FILM']::production_activity[],'Europe/Amsterdam',60)",venue,owner);return new Fixture(owner,scout,venue);}
    private UUID user(String role){UUID id=UUID.randomUUID();jdbc.update("insert into users(id,email,password_hash,role,email_verified) values(?,?, 'hash',?::user_role,true)",id,id+"@example.test",role);return id;}
    private UUID insertBooking(Fixture f,String status,String start,String end){try(Connection c=dataSource.getConnection()){return insertBooking(c,f,status,start,end);}catch(Exception e){throw new RuntimeException(e);}}
    private UUID insertBooking(Connection c,Fixture f,String status,String start,String end) throws SQLException {UUID id=UUID.randomUUID();try(PreparedStatement p=c.prepareStatement("insert into bookings(id,venue_id,scout_id,status,shoot_date,start_time,end_time,venue_price_eur,hourly_rate_eur_snapshot,minimum_booking_minutes_snapshot,duration_minutes,billable_minutes,commission_percentage,commission_eur,total_paid_eur,scout_contact_name,scout_contact_email,owner_contact_name,owner_contact_email,owner_response_expires_at) values(?,?,?,?::booking_status,current_date+10,?::time,?::time,100,100,60,60,60,20,20,0,'Scout','s@test','Owner','o@test',now()+interval '1 day')")){p.setObject(1,id);p.setObject(2,f.venue);p.setObject(3,f.scout);p.setString(4,status);p.setString(5,start);p.setString(6,end);p.executeUpdate();}return id;}
    private void updateStatus(Connection c,UUID id,String status)throws SQLException{try(PreparedStatement p=c.prepareStatement("update bookings set status=?::booking_status where id=?")){p.setString(1,status);p.setObject(2,id);if(p.executeUpdate()!=1)throw new SQLException("missing booking");}}
    private void rotate(Connection c,String hash)throws SQLException{try(PreparedStatement p=c.prepareStatement("select revoked from refresh_tokens where token_hash=? for update")){p.setString(1,hash);try(ResultSet r=p.executeQuery()){r.next();if(r.getBoolean(1))throw new SQLException("already revoked");}}try(PreparedStatement p=c.prepareStatement("update refresh_tokens set revoked=true where token_hash=?")){p.setString(1,hash);p.executeUpdate();}}
    private void insertPayout(Connection supplied,Fixture f,UUID booking){boolean own=supplied==null;try(Connection ignored=own?dataSource.getConnection():null){Connection c=own?ignored:supplied;try(PreparedStatement p=c.prepareStatement("insert into owner_payouts(booking_id,owner_id,venue_id,gross_retained_amount_eur,commission_eur,provider_cost_eur,payout_amount_eur,status,eligible_at) values(?,?,?,100,20,0,80,'CALCULATED',now())")){p.setObject(1,booking);p.setObject(2,f.owner);p.setObject(3,f.venue);p.executeUpdate();}}catch(SQLException e){throw new RuntimeException(e);}}
    private void insertReceipt(Connection c,Fixture f,UUID booking,String number,String storage)throws SQLException{try(PreparedStatement p=c.prepareStatement("insert into financial_documents(id,document_type,booking_id,recipient_user_id,document_number,status,template_version,storage_reference,issued_at) values(?,'BOOKING_RECEIPT',?,?,?,'ISSUED',1,?,now())")){p.setObject(1,UUID.randomUUID());p.setObject(2,booking);p.setObject(3,f.scout);p.setString(4,number);p.setString(5,storage);p.executeUpdate();}}
    private UUID destination(Fixture f,String status,String suffix){UUID id=UUID.randomUUID();boolean verified=status.equals("VERIFIED");jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values(?,?,'IBAN','EUR','Fixture','cipher',?,'1234',?,now(),?,?)",id,f.owner,"fingerprint"+suffix,status,verified?new Timestamp(System.currentTimeMillis()):null,verified?f.owner:null);return id;}
    private void verifyDestination(Connection c,UUID id)throws SQLException{try(PreparedStatement p=c.prepareStatement("update owner_payout_destinations set status='VERIFIED',verified_at=now(),verified_by=owner_id where id=?")){p.setObject(1,id);p.executeUpdate();}}
    private UUID draftPolicy(String version){UUID id=UUID.randomUUID();jdbc.update("insert into policy_versions(id,policy_type,version,title,effective_at,content,status) values(?,'TERMS_OF_SERVICE',?,'Fixture',now(),'fixture','DRAFT')",id,version);return id;}
    private void publish(Connection c,UUID id)throws SQLException{try(PreparedStatement p=c.prepareStatement("update policy_versions set status='PUBLISHED',published_at=now(),content_hash=? where id=?")){p.setString(1,"a".repeat(64));p.setObject(2,id);p.executeUpdate();}}
    private RaceResult race(SqlAction first,SqlAction second)throws Exception{CyclicBarrier barrier=new CyclicBarrier(2);ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<String>a=pool.submit(()->tx(first,barrier));Future<String>b=pool.submit(()->tx(second,barrier));String x=a.get(10,TimeUnit.SECONDS),y=b.get(10,TimeUnit.SECONDS);return new RaceResult(List.of(x,y));}finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}}
    private String tx(SqlAction action,CyclicBarrier barrier){try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);barrier.await(5,TimeUnit.SECONDS);try{action.run(c);c.commit();return "SUCCESS";}catch(Exception e){c.rollback();return "FAIL:"+e.getClass().getSimpleName()+":"+e.getMessage();}}catch(Exception e){return "FAIL:"+e.getClass().getSimpleName()+":"+e.getMessage();}}
    private SkipLockedResult skipLocked(String sql)throws Exception{CountDownLatch selected=new CountDownLatch(2),release=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);Callable<List<UUID>> worker=()->{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);List<UUID> ids=new ArrayList<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){while(r.next())ids.add(r.getObject(1,UUID.class));}selected.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));c.rollback();return ids;}};try{Future<List<UUID>>a=pool.submit(worker),b=pool.submit(worker);assertTrue(selected.await(5,TimeUnit.SECONDS));release.countDown();return new SkipLockedResult(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS));}finally{release.countDown();pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}}
    @FunctionalInterface interface SqlAction{void run(Connection c)throws Exception;}
    record Fixture(UUID owner,UUID scout,UUID venue){}record RaceResult(List<String> outcomes){long successes(){return outcomes.stream().filter("SUCCESS"::equals).count();}long failures(){return outcomes.size()-successes();}String diagnostic(){return "transaction outcomes="+outcomes;}}record SkipLockedResult(List<UUID> first,List<UUID> second){}
}
