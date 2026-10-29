package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="eema.price.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=8080")
class PriceBrowserIntegrationTest extends InquirySupport {
 @BeforeAll static void freshBrowserAccounts(){owner=null;policies.clear();}
 @Test void browserJourney()throws Exception{
  var cases=new ArrayList<Map<String,Object>>();
  String[][] values={{"Minimum","100", "60", "", "09:30","100.00"},{"Prorated","19.99","1","","10:01","20.32"},{"Capped","100","60","500","17:00","500.00"},{"Hourly","100","60","","10:30","150.00"},{"Server Authority","100","60","","10:30","150.00"}};
  UUID featured=venue(owner,60);jdbc.update("update venues set name='QA Price Minimum',hourly_rate_eur=100,full_day_rate_eur=null where id=?",featured);
  for(var row:values)cases.add(Map.of("id",featured,"name","QA Price "+row[0],"rate",row[1],"minimum",row[2],"cap",row[3],"end",row[4],"expected",row[5]));
  UUID editor=venue(owner,60);jdbc.update("update venues set status='DRAFT',name='QA Price Editor',full_day_rate_eur=null where id=?",editor);
  var accounts=new HashMap<String,Object>();for(var a:List.of(owner,scout))accounts.put(a==owner?"OWNER":"SCOUT",call("login",Map.of("email",email(a),"password",QA_PASSWORD),200));
  Path config=Files.createTempFile("eema-price-browser-",".json");
  try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(Map.of("accounts",accounts,"cases",cases,"editor",editor,"date",date.toString())));var process=new ProcessBuilder("node","scripts/qa/price-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(240,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());assertEquals(0,jdbc.queryForObject("select count(*) from bookings where mollie_payment_id is not null",Integer.class));}finally{Files.deleteIfExists(config);}
 }
}
