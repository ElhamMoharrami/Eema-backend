package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.request.UpdateVenueRequest;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.entity.enums.ProductionActivity;
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
    private final VenueModerationService moderation = mock(VenueModerationService.class);
    private final UUID venueId = UUID.randomUUID();
    private final User owner = User.builder().id(UUID.randomUUID()).email("owner@example.test").role(UserRole.OWNER).build();
    private Venue venue;
    private VenueService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(null,
                new AppProperties.StorageProperties("local", "/tmp/eema-venue-test", "/api/upload/files/", 5_242_880),
                null, null, null, null, null, null, null);
        VenueMapper mapper = new VenueMapper(new UserMapper());
        service = new VenueService(venues, users, mapper, currentUser, new VenueMediaPolicy(properties), storage,
                new VenueProductionProfileValidator(), moderation);
        venue = Venue.builder().id(venueId).owner(owner).name("Old Studio").neighbourhood("West")
                .sizeM2(100).hourlyRateEur(new BigDecimal("100.00"))
                .minimumBookingMinutes(240).fullDayRateEur(new BigDecimal("500.00")).tags(List.of("old"))
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
        assertEquals(new BigDecimal("175.00"), result.hourlyRateEur());
        assertEquals(240, result.minimumBookingMinutes());
        assertEquals(new BigDecimal("1200.00"), result.fullDayRateEur());
        assertEquals(List.of(primary, secondary), result.photoPaths());
        assertFalse(result.photoPaths().contains(photo("22222222-2222-4222-8222-222222222222")));
        assertEquals(primary, result.photoPaths().getFirst());
        assertEquals("Europe/Paris", result.timezone());
        assertEquals(30, result.maxCrewSize());
        assertEquals(List.of(ProductionActivity.PHOTO, ProductionActivity.FILM), result.permittedActivities());
        assertEquals(new BigDecimal("4.50"), result.ceilingHeightM());
        assertTrue(result.loadingAccess());
        assertEquals(3, result.parkingSpaces());
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
    void ownerMayRemoveOptionalFullDayRate() {
        when(currentUser.getCurrentUser()).thenReturn(owner);
        UpdateVenueRequest withoutCap = requestWithPricing(new BigDecimal("175.00"), 240, null,
                30, new BigDecimal("4.50"), 3);

        var result = service.updateVenue(venueId, withoutCap);

        assertNull(result.fullDayRateEur());
        assertNull(result.naturalLight());
        assertNull(result.permittedActivities());
    }

    @Test
    void venuePricingRequestValidationRejectsInvalidRequiredValuesAndAcceptsNullCap() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            UpdateVenueRequest valid = requestWithPricing(new BigDecimal("175.00"), 240, null,
                    null, null, null);
            assertTrue(validator.validate(valid).isEmpty());

            UpdateVenueRequest invalid = requestWithPricing(BigDecimal.ZERO, 0, BigDecimal.ZERO,
                    0, BigDecimal.ZERO, -1);
            assertEquals(6, validator.validate(invalid).stream()
                    .filter(v -> java.util.Set.of("hourlyRateEur", "minimumBookingMinutes", "fullDayRateEur",
                                    "maxCrewSize", "ceilingHeightM", "parkingSpaces")
                            .contains(v.getPropertyPath().toString())).count());
        }
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


    @Test
    void cleanupFailureNeverReplacesOriginalPersistenceFailure() {
        when(currentUser.getCurrentUser()).thenReturn(owner);
        String uploaded=photo("55555555-5555-4555-8555-555555555555");
        var file=mock(org.springframework.web.multipart.MultipartFile.class);
        when(storage.store(file)).thenReturn(uploaded);
        var original=new org.springframework.dao.DataIntegrityViolationException("QA original persistence failure");
        when(venues.saveAndFlush(any())).thenThrow(original);
        doThrow(new IllegalStateException("QA cleanup unavailable")).when(storage).delete(uploaded);
        assertSame(original,assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->service.uploadAndAttachPhoto(venueId,file)));
        verify(storage).delete(uploaded);
    }

    @Test
    void persistenceFailureCompensatesThroughRealS3Adapter() {
        when(currentUser.getCurrentUser()).thenReturn(owner);
        var client=mock(software.amazon.awssdk.services.s3.S3Client.class);
        var properties=MediaStorageContractTest.properties(java.nio.file.Path.of("/tmp/unused-media-mock"));
        var adapter=new S3PublicMediaStorage(client,properties,"qa-public","media/");
        var realAdapterService=new VenueService(venues,users,new VenueMapper(new UserMapper()),currentUser,
                new VenueMediaPolicy(properties),adapter,new VenueProductionProfileValidator(),moderation);
        var original=new org.springframework.dao.DataIntegrityViolationException("QA original DB failure");
        when(venues.saveAndFlush(any())).thenThrow(original);
        var file=new org.springframework.mock.web.MockMultipartFile("file","qa.jpg","image/jpeg",
                com.eema.backend.support.MediaFixtures.bytes("qa.jpg"));
        assertSame(original,assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->realAdapterService.uploadAndAttachPhoto(venueId,file)));
        var put=org.mockito.ArgumentCaptor.forClass(software.amazon.awssdk.services.s3.model.PutObjectRequest.class);
        verify(client).putObject(put.capture(),any(software.amazon.awssdk.core.sync.RequestBody.class));
        verify(client).deleteObject(argThat((software.amazon.awssdk.services.s3.model.DeleteObjectRequest r)->
                r.bucket().equals("qa-public")&&r.key().equals(put.getValue().key())));
    }

    private UpdateVenueRequest request(List<String> photos) {
        return new UpdateVenueRequest("Updated Studio", "Centre", 250, new BigDecimal("175.00"), 240, 1440,
                new BigDecimal("1200.00"), 30,
                List.of(ProductionActivity.PHOTO, ProductionActivity.FILM), List.of(ProductionActivity.MUSIC_VIDEO),
                true, true, new BigDecimal("4.50"), true, true, 3, 0, false, true, true, true,
                "No smoke without approval", "Three-phase 63A",
                List.of("daylight", "industrial"), "Updated description", "https://video.example.test/clip",
                "Europe/Paris", photos);
    }

    private UpdateVenueRequest requestWithPricing(BigDecimal hourlyRate, int minimumMinutes, BigDecimal fullDayRate,
                                                   Integer maxCrewSize, BigDecimal ceilingHeight, Integer parkingSpaces) {
        return new UpdateVenueRequest("Updated Studio", "Centre", 250, hourlyRate, minimumMinutes, 1440,
                fullDayRate, maxCrewSize, null, null, null, null, ceilingHeight, null,
                parkingSpaces == null ? null : true, parkingSpaces, null, null, null, null, null,
                null, null, List.of(), null, null, "Europe/Amsterdam", List.of());
    }

    private String photo(String uuid) { return "/api/upload/files/" + uuid + ".jpg"; }
}
