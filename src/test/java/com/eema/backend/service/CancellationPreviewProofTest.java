package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.CancellationTermsChangedException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CancellationPreviewProofTest {
    final AppProperties properties = new AppProperties(new AppProperties.JwtProperties(
            Base64.getEncoder().encodeToString("QA-only-secret-for-cancellation-tests-32".getBytes()),15,7),null,null,null,null,null,null,null,null);
    final CancellationPreviewProof proofs = new CancellationPreviewProof(properties);
    final BookingCancellationPolicy policy = new BookingCancellationPolicy(new BookingFinancials());
    final OffsetDateTime now = OffsetDateTime.parse("2026-06-01T12:00:00Z");
    final User actor = User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build();
    final Booking booking = Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED)
            .molliePaymentId("tr_proof").totalPaidEur(new BigDecimal("1000.00"))
            .commissionPercentage(new BigDecimal("20.00")).cancellationPolicyVersion("EEMA_V1")
            .gracePeriodEndsAt(now.minusDays(2)).fullRefundUntil(now.plusMinutes(1))
            .partialRefundUntil(now.plusDays(2)).partialRefundPercentage(new BigDecimal("50.00")).build();
    @ParameterizedTest @ValueSource(ints={599,600,601}) void exactExpiry(int seconds) {
        var decision=policy.evaluate(booking,ActorType.SCOUT,now);
        var token=proofs.issue(booking,actor,decision,now);
        if(seconds<600) assertDoesNotThrow(()->proofs.validate(token,booking,actor,decision,now.plusSeconds(seconds)));
        else assertThrows(CancellationTermsChangedException.class,()->proofs.validate(token,booking,actor,decision,now.plusSeconds(seconds)));
    }
    @ParameterizedTest @ValueSource(strings={"actor","roleOwner","roleAdmin","booking","policy","amount","deadline","tamper","missing"})
    void bindingAndTampering(String variant) {
        var decision=policy.evaluate(booking,ActorType.SCOUT,now);
        String issued=proofs.issue(booking,actor,decision,now);
        switch(variant) {
            case "actor" -> actor.setId(UUID.randomUUID());
            case "roleOwner" -> actor.setRole(UserRole.OWNER);
            case "roleAdmin" -> actor.setRole(UserRole.ADMIN);
            case "booking" -> booking.setId(UUID.randomUUID());
            case "policy" -> booking.setCancellationPolicyVersion("v2");
            case "amount" -> booking.setTotalPaidEur(new BigDecimal("2000.00"));
            case "deadline" -> booking.setFullRefundUntil(now);
        }
        String token=variant.equals("missing")?null:variant.equals("tamper")?"x"+issued:issued;
        assertThrows(CancellationTermsChangedException.class,()->proofs.validate(token,booking,actor,
                (variant.equals("policy") ? decision : policy.evaluate(booking,ActorType.valueOf(actor.getRole().name()),now)),now));
    }
    @Test void resultChangeAtDeadlineRequiresNewConsent() {
        var token=proofs.issue(booking,actor,policy.evaluate(booking,ActorType.SCOUT,now),now);
        var at=booking.getFullRefundUntil();
        assertThrows(CancellationTermsChangedException.class,()->proofs.validate(token,booking,actor,policy.evaluate(booking,ActorType.SCOUT,at),at));
    }
    @Test void stableProofSurvivesNewServiceAndCannotAuthenticateAsAccessToken() {
        var decision=policy.evaluate(booking,ActorType.SCOUT,now);
        var token=proofs.issue(booking,actor,decision,now);
        new CancellationPreviewProof(properties).validate(token,booking,actor,decision,now);
        var jwt = new com.eema.backend.security.JwtService(properties); jwt.init();
        assertThrows(io.jsonwebtoken.JwtException.class,()->jwt.validateToken(token));
    }
}
