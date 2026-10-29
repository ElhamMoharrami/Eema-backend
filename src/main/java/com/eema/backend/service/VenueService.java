package com.eema.backend.service;

import com.eema.backend.dto.request.CreateVenueRequest;
import com.eema.backend.dto.request.UpdateVenueRequest;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;

import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;

import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.entity.enums.VenueStatus;

import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.NotFoundException;

import com.eema.backend.mapper.VenueMapper;

import com.eema.backend.repository.VenueRepository;
import com.eema.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final VenueMapper venueMapper;
    private final CurrentUserService currentUserService;
    private final VenueMediaPolicy mediaPolicy;
    private final FileStorage fileStorage;
    private final VenueProductionProfileValidator productionProfileValidator;
    private final VenueModerationService moderationService;

    @Transactional
    public VenueDto createVenue(
            UUID userId,
            CreateVenueRequest request
    ) {

        User owner =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "User not found"
                                )
                        );

        if (owner.getRole() != UserRole.OWNER) {
            throw new ForbiddenException(
                    "Only users with OWNER role can create venues"
            );
        }

        mediaPolicy.validateClipUrl(request.clipUrl());

        Venue venue =
                venueMapper.toEntity(request);

        venue.setOwner(owner);
        productionProfileValidator.validateConsistency(venue);

        Venue savedVenue =
                venueRepository.save(venue);

        log.info(
                "Venue created: venueId={}, ownerId={}",
                savedVenue.getId(),
                owner.getId()
        );

        return venueMapper.toDto(savedVenue);
    }

    @Transactional(readOnly = true)
    public List<VenueDto> getAllVenues() {

        return venueRepository
                .findAll()
                .stream()
                .map(venueMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VenueDto getVenueById(UUID id) {

        Venue venue =
                venueRepository.findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Venue not found"
                                )
                        );

        /*
         * ACTIVE venues are public.
         */
        if (venue.getStatus() == VenueStatus.ACTIVE) {
            return venueMapper.toDto(venue);
        }

        /*
         * Non-active venues require authentication.
         */
        Optional<User> currentUser =
                currentUserService
                        .getCurrentUserIfAuthenticated();

        if (currentUser.isEmpty()) {
            throw new ForbiddenException(
                    "Access denied to venue"
            );
        }

        User user = currentUser.get();

        if (isAdmin(user)
                || isVenueOwner(user, venue)) {

            return venueMapper.toDto(venue);
        }

        throw new ForbiddenException(
                "Access denied to venue"
        );
    }

    @Transactional
    public VenueDto updateVenue(
            UUID id,
            UpdateVenueRequest request
    ) {

        Venue venue =
                venueRepository.findByIdForUpdate(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Venue not found"
                                )
                        );

        User user =
                currentUserService.getCurrentUser();

        if (!isAdmin(user)
                && !isVenueOwner(user, venue)) {

            throw new ForbiddenException(
                    "Only the venue owner or admin can update the venue"
            );
        }

        List<String> validatedPhotos = mediaPolicy.validatePhotos(request.photoPaths());
        mediaPolicy.validateClipUrl(request.clipUrl());
        mediaPolicy.validateTimezone(request.timezone());

        CriticalProfile before = CriticalProfile.of(venue);
        venueMapper.updateEntityFromDto(request, venue);
        venue.setPhotoPaths(validatedPhotos);
        productionProfileValidator.validateConsistency(venue);
        if (!before.equals(CriticalProfile.of(venue))) moderationService.requireRereviewAfterCriticalEdit(venue, user);

        Venue updatedVenue =
                venueRepository.save(venue);

        log.info(
                "Venue updated: venueId={}, userId={}",
                venue.getId(),
                user.getId()
        );

        return venueMapper.toDto(
                updatedVenue
        );
    }

    @Transactional
    public VenueDto uploadAndAttachPhoto(UUID id, MultipartFile file) {
        Venue venue = venueRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Venue not found"));
        User user = currentUserService.getCurrentUser();
        if (!isAdmin(user) && !isVenueOwner(user, venue)) {
            throw new ForbiddenException("Only the venue owner or admin can add venue photos");
        }
        List<String> photos = new ArrayList<>(venue.getPhotoPaths() == null ? List.of() : venue.getPhotoPaths());
        if (photos.size() >= VenueMediaPolicy.MAX_PHOTOS) {
            throw new com.eema.backend.exception.BadRequestException(
                    "A venue can have at most " + VenueMediaPolicy.MAX_PHOTOS + " photos");
        }
        String reference = fileStorage.store(file);
        try {
            if (!mediaPolicy.isStoredReference(reference)) throw new IllegalStateException("Storage returned an invalid media reference");
            photos.add(reference);
            venue.setPhotoPaths(mediaPolicy.validatePhotos(photos));
            moderationService.requireRereviewAfterCriticalEdit(venue, user);
            Venue saved = venueRepository.saveAndFlush(venue);
            log.info("Venue photo attached: venueId={}, userId={}", id, user.getId());
            return venueMapper.toDto(saved);
        } catch (RuntimeException exception) {
            try { fileStorage.delete(reference); }
            catch (RuntimeException cleanupFailure) {
                log.error("Failed to clean up unattached upload: venueId={}, errorType={}",
                        id, cleanupFailure.getClass().getSimpleName());
            }
            throw exception;
        }
    }

    @Transactional
    public void deleteVenue(UUID id) {

        Venue venue =
                venueRepository.findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Venue not found"
                                )
                        );

        User user =
                currentUserService.getCurrentUser();

        if (!isAdmin(user)
                && !isVenueOwner(user, venue)) {

            throw new ForbiddenException(
                    "Only the venue owner or admin can delete the venue"
            );
        }

        venueRepository.delete(venue);

        log.info(
                "Venue deleted: venueId={}, userId={}",
                id,
                user.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<VenueDto> getVenuesByOwner(
            UUID ownerId
    ) {

        return venueRepository
                .findByOwnerId(ownerId)
                .stream()
                .map(venueMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VenueListDto> getAllActiveVenues(
            String tag
    ) {

        List<Venue> venues;

        if (tag == null || tag.isBlank()) {

            venues =
                    venueRepository.findByStatus(
                            VenueStatus.ACTIVE
                    );

        } else {

            venues =
                    venueRepository
                            .findByStatusAndTag(
                                    VenueStatus.ACTIVE.name(),
                                    tag.trim().toLowerCase(),
                                    Pageable.unpaged()
                            )
                            .getContent();
        }

        return venues
                .stream()
                .map(venueMapper::toListDto)
                .toList();
    }

    private boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    private boolean isVenueOwner(
            User user,
            Venue venue
    ) {
        return user.getRole() == UserRole.OWNER
                && venue.getOwner() != null
                && venue.getOwner()
                .getId()
                .equals(user.getId());
    }

    private record CriticalProfile(Integer maxCrew, Object permitted, Object prohibited, Boolean naturalLight,
                                   Boolean blackout, Object ceiling, Boolean loading, Boolean parking,
                                   Integer parkingSpaces, Integer floor, Boolean lift, Boolean wheelchair,
                                   Boolean bathrooms, Boolean sound, String rules, String power, Object photos) {
        static CriticalProfile of(Venue v) { return new CriticalProfile(v.getMaxCrewSize(), copy(v.getPermittedActivities()),
                copy(v.getProhibitedActivities()), v.getNaturalLight(), v.getBlackoutPossible(), v.getCeilingHeightM(),
                v.getLoadingAccess(), v.getParkingAvailable(), v.getParkingSpaces(), v.getFloorNumber(),
                v.getLiftAvailable(), v.getWheelchairAccessible(), v.getBathroomsAvailable(),
                v.getSoundRecordingSuitable(), v.getProductionRules(), v.getPowerDetails(), copy(v.getPhotoPaths())); }
        private static Object copy(List<?> values) { return values == null ? null : List.copyOf(values); }
    }
}
