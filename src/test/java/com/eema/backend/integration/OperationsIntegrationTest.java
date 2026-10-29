package com.eema.backend.integration;

import com.eema.backend.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Small OPS persistence assertions; prior domain suites retain their race authorities. */
class OperationsIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @Autowired WorkflowObservability observability;
    @Autowired OperationalThresholds thresholds;
    @Autowired MeterRegistry meters;
    @AfterEach void noProviders(){verifyNoInteractions(mollie);}
    int feeQueue() throws Exception {
        int fees=0,page=0;while(true){var result=req(get("/api/admin/operations/exceptions?page="+page+"&size=100"),admin,null,200);for(var e:result.path("content"))if(e.path("type").asText().equals("MOLLIE_FEE_UNRECONCILED"))fees++;if(result.path("last").asBoolean())return fees;assertTrue(++page<100);}
    }
    double gauge(){observability.scanStuckStates();return meters.get("eema.workflow.mollie.fee.unreconciled").gauge().value();}
    @Test void feeGaugeAndCompleteQueueAgreeBeyondTwoHundredAcrossPaidHistory() throws Exception {
        UUID b=create(venue(owner,60),scout,"09:00","10:00");double before=gauge();int queued=feeQueue();
        OffsetDateTime cutoff=thresholds.feeCutoff(thresholds.now());
        jdbc.update("""
            insert into bookings select (jsonb_populate_record(null::bookings,to_jsonb(b)||jsonb_build_object(
             'id',gen_random_uuid(),'status',case n%3 when 0 then 'CONFIRMED' when 1 then 'CANCELLED' else 'REFUNDED' end,
             'shoot_date',b.shoot_date+n,'paid_at',cast(? as timestamptz),'total_paid_eur',b.venue_price_eur,'mollie_fee_reconciled',false))).*
            from bookings b cross join generate_series(1,205) n where b.id=?
            """,cutoff.minusNanos(1000),b);
        String snapshot=jdbc.queryForObject("select json_agg(b order by id)::text from bookings b",String.class);
        assertEquals(before+205,gauge());assertEquals(queued+205,feeQueue());
        assertEquals(snapshot,jdbc.queryForObject("select json_agg(b order by id)::text from bookings b",String.class));
    }
    @ParameterizedTest @ValueSource(longs={-1,0,1})
    void feeCutoffIsStrictAndShared(long micros)throws Exception {
        UUID b=create(venue(owner,60),scout,"09:00","10:00");double before=gauge();int queued=feeQueue();
        jdbc.update("update bookings set status='CANCELLED',total_paid_eur=venue_price_eur,paid_at=? where id=?",thresholds.feeCutoff(thresholds.now()).plusNanos(micros*1000),b);
        assertEquals(before+(micros<0?1:0),gauge());assertEquals(queued+(micros<0?1:0),feeQueue());
    }
    @Test void policyAndIncidentReferencesRejectParentDeletionAtomically() throws Exception {
        UUID b=create(venue(owner,60),scout,"09:00","10:00");UUID policy=policies.getFirst();
        String before=row(b);long policyCount=jdbc.queryForObject("select count(*) from policy_versions",Long.class);
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("delete from policy_versions where id=?",policy));
        assertEquals(policyCount,jdbc.queryForObject("select count(*) from policy_versions",Long.class));assertEquals(before,row(b));
        jdbc.update("delete from booking_policy_versions where booking_id=?",b); // isolate incident FK from the already-proven policy FK
        jdbc.update("insert into incident_reports(booking_id,reported_by_user_id,category,description) values(?,?,'OTHER','QA OPS incident reference')",b,id(scout));
        var failure=assertThrows(DataIntegrityViolationException.class,()->jdbc.update("delete from bookings where id=?",b));
        assertTrue(failure.getMostSpecificCause().getMessage().contains("incident_reports"));assertEquals(before,row(b));assertEquals(1,rows("incident_reports","booking_id",b));
        for(String table:List.of("financial_documents","owner_payouts","owner_payout_destinations","policy_acceptances","booking_policy_versions","incident_reports"))
            assertTrue(jdbc.queryForList("select confdeltype::text from pg_constraint where contype='f' and conrelid=?::regclass",String.class,table).stream().allMatch("r"::equals),table);
    }
}
