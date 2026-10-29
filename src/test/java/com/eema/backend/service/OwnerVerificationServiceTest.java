package com.eema.backend.service;
import com.eema.backend.dto.request.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OwnerVerificationServiceTest {
    private final OwnerVerificationRepository repo=mock(OwnerVerificationRepository.class);
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final MarketplaceTrustAuditService audit=mock(MarketplaceTrustAuditService.class);
    private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    private final OwnerVerificationService service=new OwnerVerificationService(repo,current,audit,events);
    private final User owner=User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).emailVerified(true).active(true).build();
    private final User admin=User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).active(true).build();
    @BeforeEach void setup(){when(repo.save(any())).thenAnswer(i->{OwnerVerification v=i.getArgument(0);if(v.getId()==null)setId(v,UUID.randomUUID());return v;});}
    @Test void ownerSubmitsAndCannotDowngradeVerifiedState(){
        when(current.getCurrentUser()).thenReturn(owner);when(repo.findByOwnerIdForUpdate(owner.getId())).thenReturn(Optional.empty());
        var submitted=service.submit(new OwnerVerificationSubmitRequest("Venue control evidence available"));
        assertThat(submitted.status()).isEqualTo(OwnerVerificationStatus.PENDING);
        OwnerVerification verified=OwnerVerification.builder().id(submitted.id()).owner(owner).status(OwnerVerificationStatus.VERIFIED).build();
        when(repo.findByOwnerIdForUpdate(owner.getId())).thenReturn(Optional.of(verified));
        assertThatThrownBy(()->service.submit(new OwnerVerificationSubmitRequest(null))).isInstanceOf(ConflictException.class);
    }
    @Test void adminApprovesPendingAndRejectsPendingWithReason(){
        when(current.getCurrentUser()).thenReturn(admin);
        OwnerVerification pending=OwnerVerification.builder().id(UUID.randomUUID()).owner(owner).status(OwnerVerificationStatus.PENDING).build();
        when(repo.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        assertThat(service.approve(pending.getId(),new OwnerVerificationApproveRequest(VerificationMethod.VIDEO_CALL,"checked")).status()).isEqualTo(OwnerVerificationStatus.VERIFIED);
        pending.setStatus(OwnerVerificationStatus.PENDING);pending.setReviewedAt(null);pending.setReviewedBy(null);
        assertThat(service.reject(pending.getId(),new ReasonRequest("Authority evidence unclear")).status()).isEqualTo(OwnerVerificationStatus.REJECTED);
        assertThat(pending.getReviewNote()).isEqualTo("Authority evidence unclear");
    }
    private static void setId(OwnerVerification v,UUID id){try{var f=OwnerVerification.class.getDeclaredField("id");f.setAccessible(true);f.set(v,id);}catch(Exception e){throw new RuntimeException(e);}}
}
