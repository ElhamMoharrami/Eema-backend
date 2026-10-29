package com.eema.backend.service;

import com.eema.backend.dto.response.CheckoutResponse;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class PaymentService {
    private final PaymentCreationCoordinator coordinator;
    private final MollieProviderClient mollie;
    private final CurrentUserService currentUserService;
    private final PolicyAcceptanceService policyAcceptances;
    PaymentService(PaymentCreationCoordinator coordinator,MollieProviderClient mollie){this(coordinator,mollie,null,null);}

    public CheckoutResponse createPayment(UUID bookingId, UUID scoutId) {
        if(policyAcceptances!=null) policyAcceptances.requireCurrentPolicies(currentUserService.getCurrentUser());
        PaymentCreationCoordinator.Claim claim = coordinator.claim(bookingId, scoutId);
        if (claim.alreadyCreated()) return claim.existing();
        try {
            MolliePaymentResponse payment = mollie.createPayment(
                    bookingId, claim.amount(), claim.idempotencyKey());
            CheckoutResponse response = coordinator.complete(claim, payment);
            log.info("Mollie payment creation completed: bookingId={}, paymentId={}", bookingId, response.molliePaymentId());
            return response;
        } catch (HttpStatusCodeException e) {
            log.warn("Mollie rejected payment creation: bookingId={}, status={}", bookingId, e.getStatusCode().value());
            throw new BadRequestException("Could not initiate payment");
        } catch (RestClientException e) {
            log.warn("Mollie payment creation temporarily failed: bookingId={}, errorType={}", bookingId, e.getClass().getSimpleName());
            throw new BadRequestException("Payment creation is pending; retry shortly");
        }
    }
}
