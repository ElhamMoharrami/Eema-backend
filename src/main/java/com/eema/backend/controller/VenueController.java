package com.eema.backend.controller;

import com.eema.backend.dto.request.CreateVenueRequest;
import com.eema.backend.dto.request.UpdateVenueRequest;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.entity.User;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.service.VenueService;
import com.eema.backend.service.VenueModerationService;
import com.eema.backend.dto.response.VenueModerationDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final UserRepository userRepository;
    private final VenueModerationService moderationService;

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
    @PreAuthorize("hasRole('OWNER') and @verifiedUserPolicy.isVerified()")
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN') and @verifiedUserPolicy.isVerified()")
    public ResponseEntity<VenueDto> updateVenue(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateVenueRequest request) {
        return ResponseEntity.ok(venueService.updateVenue(id, request));
    }

    @PostMapping(value = "/{id}/photos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN') and @verifiedUserPolicy.isVerified()")
    public ResponseEntity<VenueDto> uploadAndAttachPhoto(@PathVariable UUID id,
                                                         @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(venueService.uploadAndAttachPhoto(id, file));
    }

    @PostMapping("/{id}/submit-for-review")
    @PreAuthorize("hasRole('OWNER') and @verifiedUserPolicy.isVerified()")
    public VenueModerationDto submitForReview(@PathVariable UUID id) { return moderationService.submit(id); }

    @GetMapping("/{id}/moderation")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public VenueModerationDto moderation(@PathVariable UUID id) { return moderationService.ownerDetail(id); }
}
