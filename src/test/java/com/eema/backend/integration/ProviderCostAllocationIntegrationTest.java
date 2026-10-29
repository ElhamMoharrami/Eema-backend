package com.eema.backend.integration;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual allocation/reconciliation services, disposable PostgreSQL, no provider or bank movement. */
class ProviderCostAllocationIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @Autowired BookingRepository repository;
    @Autowired BookingFinancials financials;
    @Autowired OwnerPayoutService payouts;
    @Autowired BookingCancellationCoordinator cancellations;
    UUID confirmed()throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);
        String pid="tr_"+b.toString().replace("-","");jdbc.update("update bookings set mollie_payment_id=? where id=?",pid,b);
        bookings.processPaidPayment(new MolliePaymentResponse(pid,"paid",new MolliePaymentResponse.Amount("125.50","EUR"),null,Map.of("bookingId",b.toString())));return b;
    }
    BigDecimal money(String value){return new BigDecimal(value).setScale(2);}
    BigDecimal liability(UUID b){return jdbc.queryForObject("select coalesce(sum(amount_eur),0) from owner_financial_liabilities where booking_id=? and type='PROVIDER_COST'",BigDecimal.class,b).setScale(2);}
    UUID payout(UUID b){return jdbc.queryForObject("select id from owner_payouts where booking_id=?",UUID.class,b);}
    @ParameterizedTest @CsvSource({
        "NONE,16,5,16,0,5,0", "NONE,16,16,16,0,16,0", "NONE,16,25,16,0,25,0", "NONE,0,25,0,0,25,0",
        "EEMA,16,5,16,0,5,0", "EEMA,16,16,16,0,16,0", "EEMA,16,25,16,0,25,0", "EEMA,0,25,0,0,25,0",
        "OWNER_SETTLEMENT,16,5,11,5,0,0", "OWNER_SETTLEMENT,16,16,0,16,0,0", "OWNER_SETTLEMENT,16,25,0,16,9,0", "OWNER_SETTLEMENT,0,25,0,0,25,0",
        "OWNER,16,5,11,5,0,0", "OWNER,16,16,0,16,0,0", "OWNER,16,25,0,16,0,9", "OWNER,0,25,0,0,0,25"
    })
    void responsibilityMatrixPersistsExactAllocationUnderConcurrentReconciliation(String responsibility,String entitlement,String cost,String expectedPayout,String applied,String platform,String debt)throws Exception{
        UUID b=confirmed();
        // Controlled pre-fee snapshots exercise the requested generic positive OWNER/EEMA cases;
        // real cancellation policy assignments/full refunds are separately tested below.
        jdbc.update("update bookings set status='CANCELLED',cancellation_policy_tier='PARTIAL_REFUND',owner_cancellation_settlement_eur=?,cancellation_retained_amount_eur=?,commission_eur=0,provider_cost_responsibility=?,cancellation_refund_amount_eur=total_paid_eur-? where id=?",money(entitlement),money(entitlement),responsibility,money(entitlement),b);
        var refund=repository.findById(b).orElseThrow().getCancellationRefundAmountEur();
        race(()->bookings.reconcileMollieFee(b,money(cost)),()->bookings.reconcileMollieFee(b,money(cost)));
        var booking=repository.findById(b).orElseThrow();var allocation=financials.allocateProviderCost(financials.ownerEntitlementBeforeProviderCost(booking),booking.getMollieFeeEur(),booking.getProviderCostResponsibility());
        assertEquals(money(expectedPayout),booking.getOwnerPayoutEur());assertEquals(money(expectedPayout),payouts.get(payout(b)).payoutAmountEur());
        assertEquals(money(applied),allocation.appliedToSettlement());assertEquals(money(platform),allocation.eemaProviderCost());assertEquals(money(debt),liability(b));
        assertEquals(refund,booking.getCancellationRefundAmountEur());assertEquals(money(cost),booking.getMollieFeeEur());
        payouts.recalculateForBooking(b);bookings.reconcileMollieFee(b,money(cost));
        assertEquals(money(debt),liability(b));assertEquals(1,Collections.frequency(audit(b),"PAYMENT_RECONCILED"));
        assertEquals(money(debt).signum()>0?1:0,Collections.frequency(audit(b),"OWNER_LIABILITY_CREATED"));verifyNoInteractions(mollie);
    }
    @ParameterizedTest @CsvSource({"SCOUT,BEFORE","SCOUT,AFTER","SCOUT,CONCURRENT","OWNER,BEFORE","OWNER,AFTER","OWNER,CONCURRENT","ADMIN,BEFORE","ADMIN,AFTER","ADMIN,CONCURRENT"})
    void cancellationAndFeeOrderingConvergeWithoutChangingScoutRefund(String role,String order)throws Exception{
        UUID b=confirmed();var now=OffsetDateTime.now(clock);
        jdbc.update("update bookings set grace_period_ends_at=?,full_refund_until=?,partial_refund_until=? where id=?",now.minusDays(2),now.minusDays(1),now.plusDays(1),b);
        var actor=users.findById(id(role.equals("SCOUT")?scout:role.equals("OWNER")?owner:admin)).orElseThrow();
        String proof=cancellations.preview(b,actor,now).previewToken();Runnable cancel=()->cancellations.cancel(b,actor,"QA policy validation",proof);Runnable fee=()->bookings.reconcileMollieFee(b,money("75"));
        if(order.equals("BEFORE")){fee.run();cancel.run();}else if(order.equals("AFTER")){cancel.run();fee.run();}else race(cancel,fee);
        var booking=repository.findById(b).orElseThrow();assertEquals(money(role.equals("SCOUT")?"62.75":"125.50"),booking.getCancellationRefundAmountEur());
        assertEquals(money("0"),booking.getOwnerPayoutEur());assertEquals(money("0"),payouts.get(payout(b)).payoutAmountEur());
        assertEquals(money(role.equals("OWNER")?"75":"0"),liability(b));
        var allocation=financials.allocateProviderCost(financials.ownerEntitlementBeforeProviderCost(booking),booking.getMollieFeeEur(),booking.getProviderCostResponsibility());
        assertEquals(money(role.equals("SCOUT")?"24.80":role.equals("ADMIN")?"75":"0"),allocation.eemaProviderCost());
        for(int i=0;i<3;i++){fee.run();payouts.recalculateForBooking(b);}assertEquals(1,Collections.frequency(audit(b),"PAYMENT_RECONCILED"));assertEquals(money(role.equals("OWNER")?"75":"0"),liability(b));
    }
    @ParameterizedTest @NullSource @EnumSource(value=ProviderCostResponsibility.class,names={"NONE","EEMA","OWNER_SETTLEMENT"})
    void lateFeeOnHistoricalPaidSettlementCannotCreateLimitedRecourseDebt(ProviderCostResponsibility responsibility)throws Exception{
        UUID b=confirmed();bookings.reconcileMollieFee(b,money("0"));UUID p=payout(b),d=UUID.randomUUID();
        jdbc.update("update owner_payout_destinations set status='REPLACED',replaced_at=now() where owner_id=? and status='VERIFIED'",id(owner));
        jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values (?,?,'IBAN','EUR','QA Owner','qa-encrypted',?,'1234','VERIFIED',now(),now(),?)",d,id(owner),d.toString(),id(admin));
        jdbc.update("update bookings set shoot_date=? where id=?",LocalDate.now(clock).minusDays(1),b);payouts.recalculateForBooking(b);payouts.markPaid(p,"QA historical transfer",users.findById(id(admin)).orElseThrow());
        String snapshot=jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where id=?",String.class,p);
        // Defensive legacy snapshot: modern readiness requires reconciliation before PAID.
        jdbc.update("update bookings set mollie_fee_reconciled=false,provider_cost_responsibility=? where id=?",responsibility==null?null:responsibility.name(),b);
        bookings.reconcileMollieFee(b,money("125"));payouts.recalculateForBooking(b);
        assertEquals(snapshot,jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where id=?",String.class,p));
        assertEquals(0,jdbc.queryForObject("select count(*) from owner_financial_liabilities where booking_id=?",Integer.class,b));
    }
    @Test void normalBookingNullResponsibilityAlsoCapsFeesWithoutDebt()throws Exception{
        UUID b=confirmed();assertNull(repository.findById(b).orElseThrow().getProviderCostResponsibility());
        bookings.reconcileMollieFee(b,money("125"));payouts.recalculateForBooking(b);
        var booking=repository.findById(b).orElseThrow();
        var allocation=financials.allocateProviderCost(financials.ownerEntitlementBeforeProviderCost(booking),booking.getMollieFeeEur(),booking.getProviderCostResponsibility());
        assertEquals(money("0"),booking.getOwnerPayoutEur());assertEquals(money("0"),payouts.get(payout(b)).payoutAmountEur());
        assertEquals(money("24.60"),allocation.eemaProviderCost());assertEquals(money("100.40"),allocation.appliedToSettlement());
        assertEquals(0,jdbc.queryForObject("select count(*) from owner_financial_liabilities where booking_id=?",Integer.class,b));
    }
    @ParameterizedTest @EnumSource(value=ProviderCostResponsibility.class,names={"OWNER","NONE","EEMA"})
    void paidHistoryUsesAppliedDeductionRatherThanTheRawProviderCost(ProviderCostResponsibility responsibility)throws Exception{
        UUID b=confirmed();
        jdbc.update("update bookings set status='CANCELLED',cancellation_policy_tier='PARTIAL_REFUND',owner_cancellation_settlement_eur=16,cancellation_retained_amount_eur=16,commission_eur=0,provider_cost_responsibility=?,cancellation_refund_amount_eur=total_paid_eur-16 where id=?",responsibility.name(),b);
        bookings.reconcileMollieFee(b,money("5"));UUID p=payout(b),d=UUID.randomUUID();
        jdbc.update("update owner_payout_destinations set status='REPLACED',replaced_at=now() where owner_id=? and status='VERIFIED'",id(owner));
        jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values (?,?,'IBAN','EUR','QA Owner','qa-encrypted',?,'1234','VERIFIED',now(),now(),?)",d,id(owner),d.toString(),id(admin));
        jdbc.update("update bookings set shoot_date=? where id=?",LocalDate.now(clock).minusDays(1),b);payouts.recalculateForBooking(b);payouts.markPaid(p,"QA applied fee history",users.findById(id(admin)).orElseThrow());
        String snapshot=jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where id=?",String.class,p);
        if(responsibility!=ProviderCostResponsibility.OWNER){
            // Defensive legacy late reconciliation: raw historical fee was platform-borne, not deducted.
            jdbc.update("update bookings set mollie_fee_reconciled=false,provider_cost_responsibility='OWNER_SETTLEMENT' where id=?",b);
            bookings.reconcileMollieFee(b,money("25"));
        }
        for(int i=0;i<3;i++)payouts.recalculateForBooking(b);
        assertEquals(0,jdbc.queryForObject("select count(*) from owner_financial_liabilities where booking_id=?",Integer.class,b));
        assertEquals(snapshot,jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where id=?",String.class,p));
        if(responsibility==ProviderCostResponsibility.OWNER){
            // A true later reversal must still recover the net transfer and the explicit owner fee.
            jdbc.update("update bookings set owner_cancellation_settlement_eur=0,cancellation_retained_amount_eur=0,owner_payout_eur=0,cancellation_refund_amount_eur=total_paid_eur where id=?",b);
            payouts.recalculateForBooking(b);payouts.recalculateForBooking(b);
            assertEquals(money("5"),liability(b));
            assertEquals(money("11"),jdbc.queryForObject("select amount_eur from owner_financial_liabilities where booking_id=? and type='OWNER_PAYOUT_RECOVERY'",BigDecimal.class,b));
            assertEquals(snapshot,jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where id=?",String.class,p));
        }
    }
    void race(Runnable a,Runnable b)throws Exception{
        var gate=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);
        try{var one=pool.submit(()->{gate.await(10,TimeUnit.SECONDS);a.run();return true;});var two=pool.submit(()->{gate.await(10,TimeUnit.SECONDS);b.run();return true;});one.get(20,TimeUnit.SECONDS);two.get(20,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
    }
}
