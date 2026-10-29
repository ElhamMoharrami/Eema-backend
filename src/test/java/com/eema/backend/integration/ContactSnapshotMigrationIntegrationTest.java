package com.eema.backend.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Widening-only V15 must retain every byte of historical booking rows in all existing states. */
@Testcontainers(disabledWithoutDocker=false)
class ContactSnapshotMigrationIntegrationTest {
 @Container static final PostgreSQLContainer<?> PG=new PostgreSQLContainer<>("postgres:16-alpine");
 Flyway flyway(String target){return Flyway.configure().dataSource(PG.getJdbcUrl(),PG.getUsername(),PG.getPassword()).locations("classpath:db/migration/common").target(target).load();}
 String snapshot(Connection c)throws Exception{try(var r=c.createStatement().executeQuery("select json_agg(row_to_json(b) order by id)::text from bookings b")){r.next();return r.getString(1);}}
 @Test void existingAllStateRowsSurviveWideningWithoutRewritingHistory()throws Exception{
  flyway("14").migrate();
  try(var c=DriverManager.getConnection(PG.getJdbcUrl(),PG.getUsername(),PG.getPassword())){
   UUID owner=UUID.randomUUID(),scout=UUID.randomUUID(),venue=UUID.randomUUID();
   try(var p=c.prepareStatement("insert into users(id,email,password_hash,role,full_name) values (?,?,'synthetic-unused','OWNER',?), (?,?,'synthetic-unused','SCOUT',?)")){p.setObject(1,owner);p.setString(2,"owner@example.test");p.setString(3,"N".repeat(120));p.setObject(4,scout);p.setString(5,"scout@example.test");p.setString(6,"S".repeat(120));p.executeUpdate();}
   try(var p=c.prepareStatement("insert into venues(id,owner_id,name,neighbourhood,size_m2,hourly_rate_eur,minimum_booking_minutes) values (?,?,'QA Migration','QA',100,100,60)")){p.setObject(1,venue);p.setObject(2,owner);p.executeUpdate();}
   int day=1;for(String status:List.of("REQUESTED","AWAITING_PAYMENT","CONFIRMED","DECLINED","EXPIRED","UNAVAILABLE","CANCELLED","REFUNDED")){
    try(var p=c.prepareStatement("insert into bookings(id,venue_id,scout_id,status,shoot_date,start_time,end_time,venue_price_eur,hourly_rate_eur_snapshot,minimum_booking_minutes_snapshot,duration_minutes,billable_minutes,commission_percentage,commission_eur,total_paid_eur,scout_contact_name,scout_contact_email,owner_contact_name,owner_contact_email,owner_response_expires_at) values (?,?,?,?::booking_status,?::date,'09:00','10:00',100,100,60,60,60,20,20,0,?,'scout@example.test',?,'owner@example.test',now()+interval '1 day')")){
     p.setObject(1,UUID.randomUUID());p.setObject(2,venue);p.setObject(3,scout);p.setString(4,status);p.setString(5,"2027-06-"+String.format("%02d",day++));p.setString(6,"S".repeat(100));p.setString(7,"N".repeat(100));p.executeUpdate();
    }
   }
   try(var r=c.createStatement().executeQuery("select count(*),max(length(scout_contact_name)),max(length(owner_contact_name)) from bookings")){r.next();assertEquals(8,r.getInt(1));assertEquals(100,r.getInt(2));assertEquals(100,r.getInt(3));}
   String before=snapshot(c);assertEquals(1,flyway("latest").migrate().migrationsExecuted);assertEquals(before,snapshot(c));
   try(var r=c.createStatement().executeQuery("select character_maximum_length from information_schema.columns where table_name='bookings' and column_name in ('scout_contact_name','owner_contact_name')")){int count=0;while(r.next()){assertEquals(255,r.getInt(1));count++;}assertEquals(2,count);}
   assertEquals(0,flyway("latest").migrate().migrationsExecuted);
  }
 }
}
