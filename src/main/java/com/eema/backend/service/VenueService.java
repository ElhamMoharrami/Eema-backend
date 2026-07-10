package com.eema.backend.service;

import com.eema.backend.dto.request.CreateVenueRequest;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.entity.User;
import com.eema.backend.entity.UserRole;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.VenueStatus;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.mapper.VenueMapper;
import com.eema.backend.repository.VenueRepository;
import com.eema.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing venues.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final VenueMapper venueMapper;

    /**
     * Creates a new venue.
     *
     * @param userId the ID of the user creating the venue
     * @param request the venue creation request
     * @return the created venue DTO
     */
    @Transactional
    public VenueDto createVenue(UUID userId, CreateVenueRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!owner.getRole().equals(UserRole.OWNER)) {
            throw new ForbiddenException("Only users with OWNER role can create venues");
        }

        Venue venue = venueMapper.toEntity(request);
        venue.setOwner(owner);
        Venue savedVenue = venueRepository.save(venue);
        return venueMapper.toDto(savedVenue);
    }

    /**
     * Gets all venues.
     *
     * @return list of venue DTOs
     */
    @Transactional(readOnly = true)
    public List<VenueDto> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(venueMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets a venue by its ID.
     *
     * @param id the venue ID
     * @return the venue DTO
     */
    @Transactional(readOnly = true)
    public VenueDto getVenueById(UUID id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));
        return venueMapper.toDto(venue);
    }

    /**
     * Updates a venue.
     *
     * @param id the venue ID
     * @param request the venue update request
     * @return the updated venue DTO
     */
    @Transactional
    public VenueDto updateVenue(UUID id, CreateVenueRequest request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        // Only owner can update venue
        User currentUser = userRepository.findById(venue.getOwner().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!currentUser.getRole().equals(UserRole.OWNER)) {
            throw new ForbiddenException("Only the venue owner can update the venue");
        }

        venueMapper.updateEntityFromDto(request, venue);
        Venue updatedVenue = venueRepository.save(venue);
        return venueMapper.toDto(updatedVenue);
    }

    /**
     * Deletes a venue.
     *
     * @param id the venue ID
     */
    @Transactional
    public void deleteVenue(UUID id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        // Only owner can delete venue
        User currentUser = userRepository.findById(venue.getOwner().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!currentUser.getRole().equals(UserRole.OWNER)) {
            throw new ForbiddenException("Only the venue owner can delete the venue");
        }

        venueRepository.delete(venue);
    }

    /**
     * Gets venues by owner ID.
     *
     * @param ownerId the owner ID
     * @return list of venue DTOs
     */
    @Transactional(readOnly = true)
    public List<VenueDto> getVenuesByOwner(UUID ownerId) {
        return venueRepository.findByOwnerId(ownerId).stream()
                .map(venueMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets all active venues, optionally filtered by tag.
     *
     * @param tag the tag to filter by (can be null or empty for all active venues)
     * @return list of venue list DTOs
     */
    @Transactional(readOnly = true)
    public List<VenueListDto> getAllActiveVenues(String tag) {
        List<Venue> venues;
        if (tag == null || tag.isBlank()) {
            venues = venueRepository.findByStatus(VenueStatus.ACTIVE);
        } else {
            venues = venueRepository.findByStatusAndTagsContaining(VenueStatus.ACTIVE.name(), tag);
        }
        return venues.stream()
                .map(venueMapper::toListDto)
                .collect(Collectors.toList());
    }
}
