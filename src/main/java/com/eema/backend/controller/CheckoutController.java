package com.eema.backend.controller;

import com.eema.backend.dto.request.CheckoutRequest;
import com.eema.backend.dto.response.CheckoutResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;
import com.eema.backend.service.PaymentService;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.exception.ForbiddenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/checkout")
@Validated
@PreAuthorize("isAuthenticated()")
public class CheckoutController {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public CheckoutController(VenueRepository venueRepository,
                              UserRepository userRepository,
                              BookingRepository bookingRepository,
                              PaymentService paymentService,
                              ObjectMapper objectMapper) {
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/checkout
     *
     * Create a booking and initiate Mollie payment.
     *
     * Input: CheckoutRequest (venueId, shootDate, brief, scoutContact)
     * Output: CheckoutResponse (bookingId, checkoutUrl, molliePaymentId, amount, expiresIn)
     * Status: 201 Created on success
     *
     * Flow:
     * 1. Validate venue exists and is active
     * 2. Get authenticated scout user
     * 3. Calculate total amount (dayRate + serviceFee)
     * 4. Create Booking with status = PENDING
     * 5. Call PaymentService to create Mollie payment
     * 6. Store molliePaymentId on booking
     * 7. Return CheckoutResponse with Mollie redirect URL
     */
    @PostMapping
    public ResponseEntity<CheckoutResponse> createCheckout(
            @RequestBody @Valid CheckoutRequest request,
            Authentication authentication
    ) {
        log.info("Checkout request received. Venue: {}, Shoot date: {}",
            request.venueId(), request.shootDate());

        // 1. Get and validate venue
        Venue venue = venueRepository.findById(request.venueId())
            .orElseThrow(() -> new NotFoundException("Venue not found"));

        if (!"active".equals(venue.getStatus().toString().toLowerCase())) {
            throw new ForbiddenException("This venue is not available for booking");
        }

        // 2. Get authenticated scout
        User scout = userRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new NotFoundException("User not found"));

        // 3. Calculate total amount
        BigDecimal totalAmount = PaymentService.calculateTotal(
            venue.getDayRateEur()
        );

        // 4. Calculate commission (5% platform fee)
        BigDecimal commissionPercentage = new BigDecimal("5.00"); // 5%
        BigDecimal commissionEur = venue.getDayRateEur().multiply(commissionPercentage).divide(new BigDecimal("100"));

        // 5. Calculate owner payout (venue price - commission)
        BigDecimal ownerPayoutEur = venue.getDayRateEur().subtract(commissionEur);

        // 6. Create booking in PENDING state
        Booking booking;
        try {
            booking = Booking.builder()
                .venueId(venue.getId())
                .scoutId(scout.getId())
                .status(BookingStatus.PENDING)
                .venuePriceEur(venue.getDayRateEur())
                .totalPaidEur(BigDecimal.ZERO) // Will be updated after payment
                .commissionPercentage(commissionPercentage)
                .commissionEur(commissionEur)
                .mollieFeeEur(BigDecimal.ZERO) // Will be set by webhook
                .ownerPayoutEur(ownerPayoutEur)
                .shootDate(request.shootDate())
                .startTime(LocalTime.of(9, 0)) // default start time
                .endTime(LocalTime.of(17, 0))  // default end time (8 hours)
                .rawBrief(request.rawBrief())
                .scoutContact(objectMapper.writeValueAsString(request.scoutContact()))
                .ownerContact("{}") // TODO: get from venue owner
                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize scout contact", e);
        }

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created. ID: {}. Status: PENDING", savedBooking.getId());

        // 7. Create Mollie payment
        CheckoutResponse checkoutResponse = paymentService.createPayment(
            savedBooking.getId(),
            totalAmount
        );

        // 8. Update booking with molliePaymentId
        savedBooking.setMolliePaymentId(checkoutResponse.molliePaymentId());
        bookingRepository.save(savedBooking);
        log.info("Booking updated with Mollie payment ID: {}", checkoutResponse.molliePaymentId());

        // 9. Return response
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutResponse);
    }
}