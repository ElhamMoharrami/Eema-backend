package com.eema.backend.service;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VenueModerationServiceTest {
    private final VenueRepository venues=mock(VenueRepository.class); private final CurrentUserService current=mock(CurrentUserService.class);
    private final OwnerVerificationService verification=mock(OwnerVerificationService.class);private final MarketplaceTrustAuditService audit=mock(MarketplaceTrustAuditService.class);
    private final MarketplaceTrustEventRepository auditEvents=mock(MarketplaceTrustEventRepository.class);private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    private final VenueModerationService service=new VenueModerationService(venues,current,verification,new VenueProductionProfileValidator(),audit,auditEvents,events);
    private final User owner=User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).emailVerified(true).active(true).build();
    private final User admin=User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).active(true).build();
    private Venue venue;
    @BeforeEach void setup(){venue=ready(owner);when(venues.findByIdForUpdate(venue.getId())).thenReturn(Optional.of(venue));when(verification.isVerified(owner.getId())).thenReturn(true);}
    @Test void verifiedOwnerSubmitsCompleteVenueButOtherOwnerCannot(){when(current.getCurrentUser()).thenReturn(owner);assertThat(service.submit(venue.getId()).status()).isEqualTo(VenueStatus.PENDING);
        venue.setStatus(VenueStatus.DRAFT);when(current.getCurrentUser()).thenReturn(User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).emailVerified(true).build());
        assertThatThrownBy(()->service.submit(venue.getId())).isInstanceOf(ForbiddenException.class);}
    @Test void unverifiedOrIncompleteOwnerCannotSubmit(){when(current.getCurrentUser()).thenReturn(owner);when(verification.isVerified(owner.getId())).thenReturn(false);
        assertThatThrownBy(()->service.submit(venue.getId())).isInstanceOf(ForbiddenException.class);
        when(verification.isVerified(owner.getId())).thenReturn(true);venue.setPhotoPaths(List.of());
        assertThatThrownBy(()->service.submit(venue.getId())).isInstanceOf(ConflictException.class).hasMessageContaining("photoPaths");}
    @Test void adminApprovesRequestsChangesSuspendsAndReactivatesWithoutTouchingBookings(){when(current.getCurrentUser()).thenReturn(admin);venue.setStatus(VenueStatus.PENDING);
        assertThat(service.approve(venue.getId(),null).status()).isEqualTo(VenueStatus.ACTIVE);
        Booking existing=Booking.builder().id(UUID.randomUUID()).venueId(venue.getId()).status(BookingStatus.CONFIRMED).build();
        assertThat(service.suspend(venue.getId(),"Safety review").status()).isEqualTo(VenueStatus.SUSPENDED);assertThat(existing.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(service.reactivate(venue.getId(),null).status()).isEqualTo(VenueStatus.ACTIVE);
        venue.setStatus(VenueStatus.PENDING);assertThat(service.requestChanges(venue.getId(),"Add clearer rules").status()).isEqualTo(VenueStatus.DRAFT);}
    @Test void approvalRechecksReadiness(){when(current.getCurrentUser()).thenReturn(admin);venue.setStatus(VenueStatus.PENDING);venue.setProductionRules(null);
        assertThatThrownBy(()->service.approve(venue.getId(),null)).isInstanceOf(ConflictException.class).hasMessageContaining("productionRules");}
    private static Venue ready(User owner){return Venue.builder().id(UUID.randomUUID()).owner(owner).name("Studio").neighbourhood("West").sizeM2(100)
            .hourlyRateEur(new BigDecimal("100")).minimumBookingMinutes(120).minimumLeadTimeMinutes(1440).maxCrewSize(10)
            .permittedActivities(List.of(ProductionActivity.PHOTO)).productionRules("No smoke").naturalLight(true).loadingAccess(true)
            .bathroomsAvailable(true).soundRecordingSuitable(true).timezone("Europe/Amsterdam").photoPaths(List.of("/api/upload/files/11111111-1111-4111-8111-111111111111.jpg"))
            .status(VenueStatus.DRAFT).build();}
}
