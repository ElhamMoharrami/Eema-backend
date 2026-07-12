package com.eema.backend.controller;

import com.eema.backend.dto.request.CreateVenueRequest;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.entity.User;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<VenueListDto>> getAllVenues(
            @RequestParam(required = false) String tag) {
        List<VenueListDto> venues;
        if (tag == null || tag.isBlank()) {
            venues = venueService.getAllActiveVenues("");
        } else {
            venues = venueService.getAllActiveVenues(tag);
        }
        return ResponseEntity.ok(venues);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueDto> getVenueById(@PathVariable UUID id) {
        VenueDto venue = venueService.getVenueById(id);
        return ResponseEntity.ok(venue);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<VenueDto> createVenue(@Valid @RequestBody CreateVenueRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found with email: " + email));

        UUID ownerId = user.getId();
        VenueDto venue = venueService.createVenue(ownerId, request);
        return ResponseEntity.status(201).body(venue);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<VenueDto>> getMyVenues() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found with email: " + email));

        UUID ownerId = user.getId();
        List<VenueDto> venues = venueService.getVenuesByOwner(ownerId);
        return ResponseEntity.ok(venues);
    }
}
