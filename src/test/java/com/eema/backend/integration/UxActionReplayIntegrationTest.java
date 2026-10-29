package com.eema.backend.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** A client retry after a lost successful response is another HTTP request.
 * Characterizes existing creation semantics without inventing idempotency keys. */
class UxActionReplayIntegrationTest extends InquirySupport {
    @BeforeAll static void freshActors() { owner=null; policies.clear(); }

    @Test void bookingCreationReplayCreatesRequestsButOnlyOneCanReserve() throws Exception {
        UUID v=inquiryVenue(); var before=available(v); var body=body(v,"09:00","10:00");
        UUID first=uuid(req(post("/api/bookings"),scout,body,201));
        UUID retry=uuid(req(post("/api/bookings"),scout,body,201));
        assertNotEquals(first,retry); assertEquals(2,rows("bookings","venue_id",v));
        assertEquals(before,available(v)); assertEquals("REQUESTED",state(first)); assertEquals("REQUESTED",state(retry));
        assertEquals(1,audit(first).size()); assertEquals(1,audit(retry).size());
        for (UUID booking : new UUID[]{first, retry})
            assertEquals(2,jdbc.queryForObject("select count(*) from notification_outbox where aggregate_id=? and notification_type='REQUEST_SUBMITTED'",Integer.class,booking));
        action(first,"accept",owner,200); action(retry,"accept",owner,409);
        assertEquals("AWAITING_PAYMENT",state(first)); assertEquals("UNAVAILABLE",state(retry));
        assertNull(detail(first).get("molliePaymentId").textValue());
    }

    @Test void inquiryCreationReplayCreatesOpenRecordsWithoutReservations() throws Exception {
        UUID v=inquiryVenue(); var before=calendar(v); var body=inquiryBody(v);
        UUID first=uuid(req(post("/api/availability-inquiries"),scout,body,201));
        UUID retry=uuid(req(post("/api/availability-inquiries"),scout,body,201));
        assertNotEquals(first,retry); assertEquals(2,rows("availability_inquiries","venue_id",v));
        assertEquals("OPEN",inquiryState(first)); assertEquals("OPEN",inquiryState(retry));
        assertEquals(0,rows("bookings","venue_id",v)); assertEquals(before,calendar(v));
        assertEquals(1,noticesFor(first)); assertEquals(1,noticesFor(retry));
    }

    @Test void incidentCreationReplayCreatesSupportRecordsWithoutFinancialEffects() throws Exception {
        UUID b=create(inquiryVenue(),scout,"09:00","10:00"); String before=row(b);
        var body=Map.of("category","OTHER","description","Synthetic repeated support report");
        UUID first=uuid(req(post("/api/bookings/"+b+"/incidents"),scout,body,201));
        UUID retry=uuid(req(post("/api/bookings/"+b+"/incidents"),scout,body,201));
        assertNotEquals(first,retry); assertEquals(2,rows("incident_reports","booking_id",b));
        assertEquals(1,rows("marketplace_trust_events","subject_id",first));
        assertEquals(1,rows("marketplace_trust_events","subject_id",retry));
        assertEquals(before,row(b)); assertEquals(0,rows("owner_payouts","booking_id",b));
        assertEquals(0,rows("owner_financial_liabilities","booking_id",b));
    }
}
