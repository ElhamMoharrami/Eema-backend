package com.eema.backend.controller;

import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.entity.User;
import com.eema.backend.service.BookingService;
import com.eema.backend.service.CurrentUserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    @PostMapping
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
}