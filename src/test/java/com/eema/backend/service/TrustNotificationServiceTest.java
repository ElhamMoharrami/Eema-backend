package com.eema.backend.service;
import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.event.TrustNotificationEvent;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrustNotificationServiceTest {
    private final OwnerVerificationRepository verifications=mock(OwnerVerificationRepository.class);
    private final VenueRepository venues=mock(VenueRepository.class);private final EmailService email=mock(EmailService.class);
    private final OwnerPayoutDestinationRepository payoutDestinations=mock(OwnerPayoutDestinationRepository.class);
    private final UserRepository users=mock(UserRepository.class);
    private final AppProperties app=new AppProperties(null,null,null,null,null,null,null,"https://app.eema.example",null);
    private final TrustNotificationService service=new TrustNotificationService(verifications,venues,payoutDestinations,users,email,app);
    private final User owner=User.builder().id(UUID.randomUUID()).email("owner@example.com").role(UserRole.OWNER).build();
    @Test void sendsVerificationApprovalAndSafeRejectionReason(){OwnerVerification v=OwnerVerification.builder().id(UUID.randomUUID()).owner(owner).build();when(verifications.findById(v.getId())).thenReturn(Optional.of(v));
        service.send(new TrustNotificationEvent(TrustEventType.OWNER_VERIFICATION_APPROVED,v.getId(),null));
        service.send(new TrustNotificationEvent(TrustEventType.OWNER_VERIFICATION_REJECTED,v.getId(),"Need venue authority evidence"));
        verify(email).send(eq(owner.getEmail()),contains("approved"),anyString(),anyString());
        verify(email).send(eq(owner.getEmail()),contains("attention"),contains("Need venue authority evidence"),contains("Need venue authority evidence"));}
    @Test void sendsApprovalChangesAndSuspensionMessages(){Venue v=Venue.builder().id(UUID.randomUUID()).owner(owner).name("Canal Studio").build();when(venues.findById(v.getId())).thenReturn(Optional.of(v));
        service.send(new TrustNotificationEvent(TrustEventType.VENUE_APPROVED,v.getId(),null));
        service.send(new TrustNotificationEvent(TrustEventType.VENUE_CHANGES_REQUESTED,v.getId(),"Add safety rules"));
        service.send(new TrustNotificationEvent(TrustEventType.VENUE_SUSPENDED,v.getId(),"Safety review"));
        verify(email).send(eq(owner.getEmail()),eq("Your venue is live"),anyString(),anyString());
        verify(email).send(eq(owner.getEmail()),contains("Changes requested"),contains("Add safety rules"),contains("Add safety rules"));
        verify(email).send(eq(owner.getEmail()),contains("suspended"),contains("Safety review"),contains("Safety review"));}
}
