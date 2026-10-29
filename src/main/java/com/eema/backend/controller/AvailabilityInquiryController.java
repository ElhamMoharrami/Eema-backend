package com.eema.backend.controller;

import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.*;
import com.eema.backend.service.AvailabilityInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/availability-inquiries")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AvailabilityInquiryController {
    private final AvailabilityInquiryService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SCOUT') and @verifiedUserPolicy.isVerified()")
    public AvailabilityInquiryDto create(@Valid @RequestBody CreateAvailabilityInquiryRequest request) { return service.create(request); }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SCOUT')")
    public List<AvailabilityInquiryDto> mine() { return service.mine(); }

    @GetMapping("/owned-venues")
    @PreAuthorize("hasRole('OWNER')")
    public List<AvailabilityInquiryDto> ownedVenueInquiries() { return service.ownedVenueInquiries(); }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('OWNER') and @verifiedUserPolicy.isVerified()")
    public AvailabilityInquiryDto decline(@PathVariable UUID id) { return service.decline(id); }

    @PostMapping("/{id}/offer")
    @PreAuthorize("hasRole('OWNER') and @verifiedUserPolicy.isVerified()")
    public AvailabilityInquiryDto offer(@PathVariable UUID id, @Valid @RequestBody OfferAvailabilityInquiryRequest request) { return service.offer(id, request); }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('SCOUT') and @verifiedUserPolicy.isVerified()")
    public BookingDto accept(@PathVariable UUID id) { return service.accept(id); }
}
