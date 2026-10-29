package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Canonical CANCEL-005..014: exact elapsed boundaries, real proof and committed decision. */
class CancellationScenarioBoundaryTest {
    static Stream<Arguments> scenarios() {
        Object[][] rows = {
            {"CANCEL-005",1439L,11520L,"GRACE_PERIOD","100.00","100.01","0.00","0.00","0.00"},
            {"CANCEL-006",1440L,4320L,"GRACE_PERIOD","100.00","100.01","0.00","0.00","0.00"},
            {"CANCEL-007",1441L,4320L,"PARTIAL_REFUND","50.00","50.01","50.00","10.00","40.00"},
            {"CANCEL-008",2880L,10081L,"FLEXIBLE_FULL_REFUND","100.00","100.01","0.00","0.00","0.00"},
            {"CANCEL-009",2880L,10080L,"PARTIAL_REFUND","50.00","50.01","50.00","10.00","40.00"},
            {"CANCEL-010",2880L,4320L,"PARTIAL_REFUND","50.00","50.01","50.00","10.00","40.00"},
            {"CANCEL-011",2880L,2880L,"PARTIAL_REFUND","50.00","50.01","50.00","10.00","40.00"},
            {"CANCEL-012",2880L,2879L,"NON_REFUNDABLE","0.00","0.00","100.01","20.00","80.01"},
            {"CANCEL-013",60L,2880L,"PARTIAL_REFUND","50.00","50.01","50.00","10.00","40.00"},
            {"CANCEL-014",60L,2820L,"NON_REFUNDABLE","0.00","0.00","100.01","20.00","80.01"}
        };
        return Stream.of("2026-06-01T10:00:00Z", "2026-03-28T10:00:00Z", "2026-10-24T10:00:00Z")
            .flatMap(now -> Arrays.stream(rows).map(row -> {
                Object[] args = new Object[row.length+1]; args[0]=now;
                System.arraycopy(row,0,args,1,row.length); return Arguments.of(args);
            }));
    }

    @ParameterizedTest(name="{1} at {0}") @MethodSource("scenarios")
    void previewAndCancellationAgreeAtEveryBoundary(String instant, String scenario, long ageMinutes,
            long untilShootMinutes, String tier, String percentage, String refund, String retained,
            String commission, String gross) {
        var now=OffsetDateTime.parse(instant);
        var zone=ZoneId.of("Europe/Amsterdam");
        var shoot=now.plusMinutes(untilShootMinutes).atZoneSameInstant(zone);
        var actor=User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).email("qa@example.test").build();
        var booking=Booking.builder().id(UUID.randomUUID()).scoutId(actor.getId()).status(BookingStatus.CONFIRMED)
            .shootDate(shoot.toLocalDate()).startTime(shoot.toLocalTime()).confirmedAt(now.minusMinutes(ageMinutes))
            .totalPaidEur(new BigDecimal("100.01")).commissionPercentage(new BigDecimal("20.00"))
            .molliePaymentId("tr_boundary").mollieFeeEur(new BigDecimal("1.00")).mollieFeeReconciled(true).build();
        var financials=new BookingFinancials(); var policy=new BookingCancellationPolicy(financials);
        policy.snapshotAtConfirmation(booking,zone,booking.getConfirmedAt());
        assertEquals(shoot.toInstant().minus(Duration.ofDays(7)),booking.getFullRefundUntil().toInstant());
        assertEquals(shoot.toInstant().minus(Duration.ofHours(48)),booking.getPartialRefundUntil().toInstant());
        var repository=mock(BookingRepository.class);var refunds=mock(RefundCoordinator.class);
        when(repository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(repository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        var properties=new AppProperties(new AppProperties.JwtProperties(Base64.getEncoder().encodeToString(
            "QA-only-secret-for-cancellation-tests-32".getBytes(java.nio.charset.StandardCharsets.UTF_8)),15,7),null,null,null,null,null,null,null,null);
        var coordinator=new BookingCancellationCoordinator(repository,mock(VenueRepository.class),mock(BookingEventRepository.class),
            policy,financials,new BookingStateMachine(),refunds,mock(ApplicationEventPublisher.class),mock(OwnerPayoutService.class),
            new CancellationPreviewProof(properties),Clock.fixed(now.toInstant(),ZoneOffset.UTC));
        var preview=coordinator.preview(booking.getId(),actor,now);
        assertTrue(preview.canCancel()); assertNotNull(preview.previewToken());
        assertEquals(CancellationPolicyTier.valueOf(tier),preview.policyTier());
        assertEquals(new BigDecimal(percentage),preview.refundPercentage());
        assertEquals(new BigDecimal(refund),preview.refundAmountEur());
        assertEquals(new BigDecimal(retained),preview.retainedAmountEur());
        OffsetDateTime deadline=switch(preview.policyTier()) {
            case GRACE_PERIOD -> booking.getGracePeriodEndsAt();
            case FLEXIBLE_FULL_REFUND -> booking.getFullRefundUntil();
            case PARTIAL_REFUND -> booking.getPartialRefundUntil();
            default -> null;
        };
        assertEquals(deadline,preview.cancellationDeadline());
        // The same instant in another offset must not change the tier or money.
        assertEquals(policy.evaluate(booking,ActorType.SCOUT,now),
            policy.evaluate(booking,ActorType.SCOUT,now.withOffsetSameInstant(ZoneOffset.ofHours(-7))));
        coordinator.cancel(booking.getId(),actor,null,preview.previewToken());
        assertEquals(BookingStatus.CANCELLED,booking.getStatus());
        assertEquals(preview.policyTier(),booking.getCancellationPolicyTier());
        assertEquals(preview.refundAmountEur(),booking.getCancellationRefundAmountEur());
        assertEquals(preview.retainedAmountEur(),booking.getCancellationRetainedAmountEur());
        assertEquals(new BigDecimal(commission),booking.getCommissionEur());
        assertEquals(new BigDecimal(gross),booking.getOwnerCancellationSettlementEur());
        assertEquals(new BigDecimal(gross).subtract(new BigDecimal("1.00")).max(new BigDecimal("0.00")),booking.getOwnerPayoutEur());
        assertEquals(new BigDecimal("100.01"),booking.getTotalPaidEur());
        if(new BigDecimal(refund).signum()==0) verifyNoInteractions(refunds);
        else verify(refunds).claimCancellation(eq(booking.getId()),eq(new BigDecimal(refund)),eq(actor.getId()),anyString(),eq(ActorType.SCOUT));
    }
}
