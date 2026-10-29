package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.request.UpdateVenueRequest;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.mapper.UserMapper;
import com.eema.backend.mapper.VenueMapper;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VenueServiceTest {
    private final VenueRepository venues = mock(VenueRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final FileStorage storage = mock(FileStorage.class);
    private final UUID venueId = UUID.randomUUID();
    private final User owner = User.builder().id(UUID.randomUUID()).email("owner@example.test").role(UserRole.OWNER).build();
    private Venue venue;
    private VenueService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(null,
                new AppProperties.StorageProperties("/tmp/eema-venue-test", "/api/upload/files/"),
                null, null, null, null, null, null, null);
        VenueMapper mapper = new VenueMapper(new UserMapper());
        service = new VenueService(venues, users, mapper, currentUser, new VenueMediaPolicy(properties), storage);
        venue = Venue.builder().id(venueId).owner(owner).name("Old Studio").neighbourhood("West")
                .sizeM2(100).dayRateEur(new BigDecimal("500.00")).tags(List.of("old"))
                .description("Old description").timezone("Europe/Amsterdam")
                .photoPaths(List.of(photo("11111111-1111-4111-8111-111111111111"),
                        photo("22222222-2222-4222-8222-222222222222"))).build();
        when(venues.findByIdForUpdate(venueId)).thenReturn(Optional.of(venue));
        when(venues.save(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(venues.saveAndFlush(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void ownerUpdatesMetadataAndPersistsMediaOrderAndRemoval() {
        when(currentUser.getCurrentUser()).thenReturn(owner);
        String primary = photo("33333333-3333-4333-8333-333333333333");
        String secondary = photo("11111111-1111-4111-8111-111111111111");

        var result = service.updateVenue(venueId, request(List.of(primary, secondary)));

        assertEquals("Updated Studio", result.name());
        assertEquals("Centre", result.neighbourhood());
        assertEquals(250, result.sizeM2());
        assertEquals(new BigDecimal("750.00"), result.dayRateEur());
        assertEquals(List.of(primary, secondary), result.photoPaths());
        assertFalse(result.photoPaths().contains(photo("22222222-2222-4222-8222-222222222222")));
        assertEquals(primary, result.photoPaths().getFirst());
        assertEquals("Europe/Paris", result.timezone());
    }

    @Test
    void anotherOwnerAndScoutCannotUpdateVenue() {
        when(currentUser.getCurrentUser()).thenReturn(
                User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).build());
        assertThrows(ForbiddenException.class, () -> service.updateVenue(venueId, request(List.of())));

        when(currentUser.getCurrentUser()).thenReturn(
                User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build());
        assertThrows(ForbiddenException.class, () -> service.updateVenue(venueId, request(List.of())));
        verify(venues, never()).save(any());
    }

    @Test
    void invalidOrDuplicateStorageReferencesAreRejected() {
        when(currentUser.getCurrentUser()).thenReturn(owner);
        assertThrows(BadRequestException.class,
                () -> service.updateVenue(venueId, request(List.of("/etc/passwd"))));
        String photo = photo("11111111-1111-4111-8111-111111111111");
        assertThrows(BadRequestException.class,
                () -> service.updateVenue(venueId, request(List.of(photo, photo))));
        verify(venues, never()).save(any());
    }

    @Test
    void scopedUploadIsAttachedAndCleanupRunsIfPersistenceFails() {
        when(currentUser.getCurrentUser()).thenReturn(owner);
        String uploaded = photo("44444444-4444-4444-8444-444444444444");
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(storage.store(file)).thenReturn(uploaded);

        var result = service.uploadAndAttachPhoto(venueId, file);
        assertEquals(uploaded, result.photoPaths().getLast());

        venue.setPhotoPaths(List.of());
        when(venues.saveAndFlush(any())).thenThrow(new IllegalStateException("database failure"));
        assertThrows(IllegalStateException.class, () -> service.uploadAndAttachPhoto(venueId, file));
        verify(storage).delete(uploaded);
    }

    private UpdateVenueRequest request(List<String> photos) {
        return new UpdateVenueRequest("Updated Studio", "Centre", 250, new BigDecimal("750.00"),
                List.of("daylight", "industrial"), "Updated description", "https://video.example.test/clip",
                "Europe/Paris", photos);
    }

    private String photo(String uuid) { return "/api/upload/files/" + uuid + ".jpg"; }
}
