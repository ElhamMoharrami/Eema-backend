package com.eema.backend.integration;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real PostgreSQL optional predicates through the ADMIN HTTP boundary. */
class AdminBookingSearchIntegrationTest extends AuthPostgresSupport {
    @Test void optionalFiltersPaginationAndAuthorization() throws Exception {
        var admin = actor("SCOUT");
        jdbc.update("update users set role='ADMIN' where id=?", id(admin));
        var scout = actor("SCOUT"); var owner = actor("OWNER");
        UUID venue = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        jdbc.update("insert into venues(id,owner_id,name,neighbourhood,size_m2,hourly_rate_eur,minimum_booking_minutes,status) values(?,?,'QA Venue','Test',100,100,60,'DRAFT')", venue,id(owner));
        int offset = 0;
        for (UUID booking : List.of(first,second)) {
            jdbc.update("""
                insert into bookings(id,venue_id,scout_id,status,shoot_date,start_time,end_time,
                venue_price_eur,hourly_rate_eur_snapshot,minimum_booking_minutes_snapshot,duration_minutes,
                billable_minutes,commission_percentage,commission_eur,total_paid_eur,scout_contact_name,
                scout_contact_email,owner_contact_name,owner_contact_email,owner_response_expires_at,created_at)
                values(?,?,?,'REQUESTED',current_date+10,'10:00','11:00',100,100,60,60,60,20,20,0,
                'Scout','qa@example.test','Owner','owner@example.test',now()+interval '1 day',now()+(? * interval '1 second'))
                """,booking,venue,id(scout),offset++);
        }
        String payment = "tr_qa_"+first;
        jdbc.update("update bookings set mollie_payment_id=?,payment_creation_status='CREATED',refund_status='PENDING' where id=?",payment,first);
        String token=access(admin);
        var all=read("/admin/bookings",token,200);
        assertTrue(all.path("totalElements").asInt()>=2);
        var filters=Map.of("bookingId",first.toString(),"venueId",venue.toString(),"scoutId",id(scout).toString(),
                "status","REQUESTED","molliePaymentId",payment,"refundStatus","PENDING","paymentCreationStatus","CREATED");
        for(var f:filters.entrySet()) {
            var page=read("/admin/bookings?"+f.getKey()+"="+f.getValue(),token,200);
            assertTrue(page.path("totalElements").asInt()>=1,f.getKey());
            assertTrue(java.util.stream.StreamSupport.stream(page.path("content").spliterator(),false)
                    .anyMatch(b->b.path("id").asText().equals(first.toString())),f.getKey());
        }
        String combined=filters.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("&"));
        assertEquals(1,read("/admin/bookings?"+combined,token,200).path("totalElements").asInt());
        assertEquals(0,read("/admin/bookings?bookingId="+first+"&status=DECLINED",token,200).path("totalElements").asInt());
        var page0=read("/admin/bookings?venueId="+venue+"&size=1&page=0",token,200);
        var page1=read("/admin/bookings?venueId="+venue+"&size=1&page=1",token,200);
        assertEquals(2,page0.path("totalElements").asInt());
        assertEquals(second.toString(),page0.path("content").get(0).path("id").asText());
        assertEquals(first.toString(),page1.path("content").get(0).path("id").asText());
        read("/admin/bookings",access(scout),403);read("/admin/bookings",access(owner),403);read("/admin/bookings",null,401);
    }
}
