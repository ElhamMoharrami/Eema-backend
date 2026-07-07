package com.eema.backend.service;

import com.eema.backend.dto.request.CreateVenueRequest;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.VenueStatus;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.mapper.UserMapper;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;
import com.eema.backend.config.AppProperties;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final UserMapper userMapper;

    /**
     * Get all active venues, optionally filtered by tag.
     *
     * @param tagFilter tag to filter by (can be null or blank)
     * @return list of venue DTOs
     */
    public List<VenueListDto> getAllActiveVenues(String tagFilter) {
        List<Venue> venues;
        if (tagFilter == null || tagFilter.isBlank()) {
            venues = venueRepository.findByStatus(VenueStatus.ACTIVE);
        } else {
            venues = venueRepository.findByStatusAndTagsContaining(VenueStatus.ACTIVE, tagFilter);
        }
        return venues.stream()
                .map(this::toVenueListDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a venue by its ID.
     *
     * @param id venue ID
     * @return venue DTO
     * @throws NotFoundException if venue not found
     */
    public VenueDto getVenueById(UUID id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Venue not found with id: " + id));
        return toVenueDto(venue);
    }

    /**
     * Create a new venue for the given owner.
     *
     * @param request venue creation request
     * @param ownerId owner's user ID
     * @return created venue DTO
     * @throws ForbiddenException if the user is not an owner
     * @throws NotFoundException if the owner user not found
     */
    public VenueDto createVenue(CreateVenueRequest request, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found with id: " + ownerId));

        if (!owner.getRole().equals(User.Role.OWNER)) {
            throw new ForbiddenException("Only users with OWNER role can create venues");
        }

        Venue venue = Venue.builder()
                .owner(owner)
                .name(request.name())
                .neighbourhood(request.neighbourhood())
                .sizeM2(request.sizeM2())
                .dayRateEur(request.dayRateEur())
                .tags(request.tags())
                .description(request.description())
                .clipUrl(request.clipUrl())
                .status(VenueStatus.PENDING)
                .build();

        Venue savedVenue = venueRepository.save(venue);
        return toVenueDto(savedVenue);
    }

    /**
     * Get all venues owned by a specific user.
     *
     * @param ownerId owner's user ID
     * @return list of venue DTOs owned by the user
     */
    public List<VenueDto> getVenuesByOwner(UUID ownerId) {
        List<Venue> venues = venueRepository.findByOwnerId(ownerId);
        return venues.stream()
                .map(this::toVenueDto)
                .collect(Collectors.toList());
    }

    /**
     * Add photos to a venue (append to existing photoPaths).
     *
     * @param venueId venue ID
     * @param ownerId owner's user ID (for ownership check)
     * @param paths list of photo paths to add
     * @return updated venue DTO
     * @throws NotFoundException if venue not found or not owned by the user
     */
    public VenueDto updateVenuePhotos(UUID venueId, UUID ownerId, List<String> paths) {
        Venue venue = venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new NotFoundException("Venue not found with id: " + venueId + " or not owned by user: " + ownerId));

        venue.getPhotoPaths().addAll(paths);
        Venue updatedVenue = venueRepository.save(venue);
        return toVenueDto(updatedVenue);
    }

    /**
     * Convert Venue entity to VenueDto.
     */
    private VenueDto toVenueDto(Venue venue) {
        return new VenueDto(
                venue.getId(),
                venue.getOwner(),
                venue.getName(),
                venue.getNeighbourhood(),
                venue.getSizeM2(),
                venue.getDayRateEur(),
                venue.getTags(),
                venue.getPhotoPaths(),
                venue.getClipUrl(),
                venue.getDescription(),
                venue.getStatus(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }

    /**
     * Convert Venue entity to VenueListDto.
     */
    private VenueListDto toVenueListDto(Venue venue) {
        String firstPhotoUrl = null;
        if (venue.getPhotoPaths() != null && !venue.getPhotoPaths().isEmpty()) {
            firstPhotoUrl = buildPhotoUrl(venue.getPhotoPaths().get(0));
        }
        return new VenueListDto(
                venue.getId(),
                venue.getName(),
                venue.getNeighbourhood(),
                venue.getSizeM2(),
                venue.getDayRateEur(),
                venue.getTags(),
                firstPhotoUrl,
                venue.getStatus()
        );
    }

    /**
     * Build a photo URL from a path.
     *
     * @param path photo path (relative to upload directory)
     * @return full URL to the photo
     */
    private String buildPhotoUrl(String path) {
        // Remove leading slash if present to avoid double slash
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return appProperties.getStorage().getFileUrlPrefix() + path;
    }
}