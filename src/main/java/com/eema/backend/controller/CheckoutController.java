package com.eema.backend.controller;

import com.eema.backend.dto.request.CheckoutRequest;
import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.dto.response.CheckoutResponse;
import com.eema.backend.entity.User;
import com.eema.backend.service.BookingService;
import com.eema.backend.service.CurrentUserService;
import com.eema.backend.service.PaymentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/checkout")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CheckoutController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<CheckoutResponse> createCheckout(@Valid @RequestBody CheckoutRequest request) {

        User scout = currentUserService.getCurrentUser();

        log.info("Checkout requested: scoutId={}, venueId={}, shootDate={}", scout.getId(), request.venueId(), request.shootDate());

        CreateBookingRequest bookingRequest = new CreateBookingRequest(request.venueId(), request.shootDate(), request.rawBrief(), request.scoutContact());

        BookingDto booking = bookingService.createBooking(bookingRequest, scout.getId());

        CheckoutResponse checkout = paymentService.createPayment(booking.id());

        log.info("Checkout created: bookingId={}, molliePaymentId={}", booking.id(), checkout.molliePaymentId());

        return ResponseEntity.status(HttpStatus.CREATED).body(checkout);
    }
}