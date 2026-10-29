package com.eema.backend.controller;

import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.*;
import com.eema.backend.service.VenueAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/venues/{venueId}/availability")
@RequiredArgsConstructor
public class VenueAvailabilityController {
    private final VenueAvailabilityService service;

    @GetMapping
    public VenueAvailabilityDto getAvailability(@PathVariable UUID venueId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.getAvailability(venueId, from, to);
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public VenueAvailabilitySettingsDto getSettings(@PathVariable UUID venueId) { return service.getSettings(venueId); }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public VenueAvailabilitySettingsDto updateSettings(@PathVariable UUID venueId, @Valid @RequestBody UpdateVenueAvailabilitySettingsRequest request) {
        return service.updateSettings(venueId, request);
    }

    @PutMapping("/weekly")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public VenueAvailabilitySettingsDto replaceWeekly(@PathVariable UUID venueId, @Valid @RequestBody UpdateWeeklyAvailabilityRequest request) {
        return service.replaceWeekly(venueId, request);
    }

    @PostMapping("/overrides")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AvailabilityOverrideDto addOverride(@PathVariable UUID venueId, @Valid @RequestBody CreateAvailabilityOverrideRequest request) {
        return service.addOverride(venueId, request);
    }

    @DeleteMapping("/overrides/{overrideId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deleteOverride(@PathVariable UUID venueId, @PathVariable UUID overrideId) { service.deleteOverride(venueId, overrideId); }
}
