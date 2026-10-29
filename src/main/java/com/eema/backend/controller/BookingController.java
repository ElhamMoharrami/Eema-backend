package com.eema.backend.controller;

import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.entity.User;
import com.eema.backend.service.BookingService;
import com.eema.backend.service.CurrentUserService;
import com.eema.backend.service.RefundService;
import com.eema.backend.service.CancellationService;
import com.eema.backend.dto.request.CancelBookingRequest;
import com.eema.backend.dto.response.CancellationPreviewDto;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;
    private final RefundService refundService;
    private final CancellationService cancellationService;

    @PostMapping
    @PreAuthorize("@verifiedUserPolicy.isVerified()")
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreateBookingRequest request) {

        User user = currentUserService.getCurrentUser();

        BookingDto booking = bookingService.createBooking(request, user.getId());

        return ResponseEntity.created(URI.create("/api/bookings/" + booking.id())).body(booking);
    }

    @GetMapping
    public ResponseEntity<List<BookingDto>> getBookings() {

        User user = currentUserService.getCurrentUser();

        List<BookingDto> bookings = bookingService.getBookingsForUser(user.getId(), user.getRole());

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDto> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingForCurrentUser(id));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("@verifiedUserPolicy.isVerified()")
    public ResponseEntity<BookingDto> acceptRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.acceptRequest(id));
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("@verifiedUserPolicy.isVerified()")
    public ResponseEntity<BookingDto> declineRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.declineRequest(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@verifiedUserPolicy.isVerified()")
    public ResponseEntity<BookingDto> cancelBooking(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelBookingRequest request) {
        return ResponseEntity.ok(cancellationService.cancel(id, request));
    }

    @GetMapping("/{id}/cancellation-preview")
    public ResponseEntity<CancellationPreviewDto> cancellationPreview(@PathVariable UUID id) {
        return ResponseEntity.ok(cancellationService.preview(id));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("@verifiedUserPolicy.isVerified()")
    public ResponseEntity<BookingDto> refundBooking(@PathVariable UUID id) {
        return ResponseEntity.accepted().body(refundService.requestFullRefund(id));
    }
}
