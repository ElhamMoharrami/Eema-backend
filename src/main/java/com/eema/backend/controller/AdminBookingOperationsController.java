package com.eema.backend.controller;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.service.AdminOperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingOperationsController {
    private final AdminOperationsService operations;

    @GetMapping
    public Page<AdminBookingSummaryDto> search(
            @RequestParam(required = false) UUID bookingId,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID scoutId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String molliePaymentId,
            @RequestParam(required = false) RefundStatus refundStatus,
            @RequestParam(required = false) PaymentCreationStatus paymentCreationStatus,
            @PageableDefault(size = 20) Pageable pageable) {
        return operations.searchBookings(bookingId, venueId, scoutId, status, molliePaymentId,
                refundStatus, paymentCreationStatus, pageable);
    }
    @GetMapping("/{id}") public AdminBookingDetailDto detail(@PathVariable UUID id) { return operations.bookingDetail(id); }
    @GetMapping("/{id}/events") public Page<AdminBookingEventDto> events(@PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) { return operations.events(id, pageable); }
}
