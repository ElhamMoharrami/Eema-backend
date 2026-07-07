package com.eema.backend.controller;

import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.entity.User;
import com.eema.backend.entity.UserRole;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<BookingDto>> getBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        // Get the user email from the principal (username)
        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found with email: " + email));

        UUID userId = user.getId();
        UserRole role = user.getRole();

        List<BookingDto> bookings = bookingService.getBookingsForUser(userId, role);
        return ResponseEntity.ok(bookings);
    }
}