package com.eema.backend.controller;

import com.eema.backend.dto.response.CheckoutResponse;
import com.eema.backend.entity.User;
import com.eema.backend.service.CurrentUserService;
import com.eema.backend.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/bookings")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CheckoutController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    @PostMapping("/{bookingId}/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(@PathVariable java.util.UUID bookingId) {

        User scout = currentUserService.getCurrentUser();

        log.info("Checkout requested: scoutId={}, bookingId={}", scout.getId(), bookingId);

        CheckoutResponse checkout = paymentService.createPayment(bookingId, scout.getId());

        log.info("Checkout created: bookingId={}, molliePaymentId={}", bookingId, checkout.molliePaymentId());

        return ResponseEntity.status(HttpStatus.CREATED).body(checkout);
    }
}
