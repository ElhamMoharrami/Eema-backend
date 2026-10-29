package com.eema.backend.service;

import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.event.TrustNotificationEvent;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class OwnerVerificationService {
    private final OwnerVerificationRepository verifications;
    private final CurrentUserService currentUsers;
    private final MarketplaceTrustAuditService audit;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly=true)
    public OwnerVerificationDto mine() {
        User owner = requireOwner();
        return verifications.findByOwnerId(owner.getId()).map(this::ownerDto)
                .orElse(new OwnerVerificationDto(null, owner.getId(), OwnerVerificationStatus.UNVERIFIED,
                        null, null, null, null));
    }

    @Transactional
    public OwnerVerificationDto submit(OwnerVerificationSubmitRequest request) {
        User owner = requireOwner();
        verifications.lockOwnerForSubmission(owner.getId());
        OwnerVerification verification = verifications.findByOwnerIdForUpdate(owner.getId())
                .orElseGet(() -> OwnerVerification.builder().owner(owner).build());
        if (verification.getStatus() == OwnerVerificationStatus.PENDING)
            return ownerDto(verification);
        if (verification.getStatus() == OwnerVerificationStatus.VERIFIED)
            throw new ConflictException("Owner verification is already approved");
        verification.setStatus(OwnerVerificationStatus.PENDING);
        verification.setOwnerNote(clean(request.note()));
        verification.setReviewNote(null);
        verification.setVerificationMethod(null);
        verification.setReviewedAt(null);
        verification.setReviewedBy(null);
        verification.setSubmittedAt(OffsetDateTime.now());
        OwnerVerification saved = verifications.save(verification);
        audit.append(TrustSubjectType.OWNER_VERIFICATION, saved.getId(), TrustEventType.OWNER_VERIFICATION_SUBMITTED,
                owner.getId(), ActorType.OWNER, null);
        events.publishEvent(new TrustNotificationEvent(TrustEventType.OWNER_VERIFICATION_SUBMITTED, saved.getId(), null));
        return ownerDto(saved);
    }

    @Transactional(readOnly=true)
    public Page<AdminOwnerVerificationDto> adminList(OwnerVerificationStatus status, UUID ownerId, Pageable pageable) {
        Specification<OwnerVerification> spec = Specification.where(null);
        if (status != null) spec = spec.and((r,q,b)->b.equal(r.get("status"), status));
        if (ownerId != null) spec = spec.and((r,q,b)->b.equal(r.get("owner").get("id"), ownerId));
        return verifications.findAll(spec, AdminPagePolicy.bounded(pageable)).map(this::adminDto);
    }

    @Transactional(readOnly=true)
    public AdminOwnerVerificationDto adminGet(UUID id) { return adminDto(get(id)); }

    @Transactional
    public AdminOwnerVerificationDto approve(UUID id, OwnerVerificationApproveRequest request) {
        User admin = requireAdmin();
        OwnerVerification v = lockedPending(id);
        v.setStatus(OwnerVerificationStatus.VERIFIED);
        v.setVerificationMethod(request.verificationMethod());
        v.setReviewNote(clean(request.note()));
        reviewed(v, admin);
        audit.append(TrustSubjectType.OWNER_VERIFICATION, v.getId(), TrustEventType.OWNER_VERIFICATION_APPROVED,
                admin.getId(), ActorType.ADMIN, null);
        events.publishEvent(new TrustNotificationEvent(TrustEventType.OWNER_VERIFICATION_APPROVED, v.getId(), null));
        return adminDto(v);
    }

    @Transactional
    public AdminOwnerVerificationDto reject(UUID id, ReasonRequest request) {
        User admin = requireAdmin();
        OwnerVerification v = lockedPending(id);
        v.setStatus(OwnerVerificationStatus.REJECTED);
        v.setReviewNote(request.reason().trim());
        v.setVerificationMethod(null);
        reviewed(v, admin);
        audit.append(TrustSubjectType.OWNER_VERIFICATION, v.getId(), TrustEventType.OWNER_VERIFICATION_REJECTED,
                admin.getId(), ActorType.ADMIN, v.getReviewNote());
        events.publishEvent(new TrustNotificationEvent(TrustEventType.OWNER_VERIFICATION_REJECTED, v.getId(), v.getReviewNote()));
        return adminDto(v);
    }

    public boolean isVerified(UUID ownerId) {
        return verifications.findByOwnerId(ownerId).map(v -> v.getStatus()==OwnerVerificationStatus.VERIFIED).orElse(false);
    }
    private OwnerVerification lockedPending(UUID id) {
        OwnerVerification v=verifications.findByIdForUpdate(id).orElseThrow(()->new NotFoundException("Owner verification not found"));
        if(v.getStatus()!=OwnerVerificationStatus.PENDING) throw new ConflictException("Owner verification is not pending");
        return v;
    }
    private OwnerVerification get(UUID id){return verifications.findById(id).orElseThrow(()->new NotFoundException("Owner verification not found"));}
    private User requireOwner(){User u=currentUsers.getCurrentUser(); if(u.getRole()!=UserRole.OWNER) throw new ForbiddenException("OWNER access required"); return u;}
    private User requireAdmin(){User u=currentUsers.getCurrentUser(); if(u.getRole()!=UserRole.ADMIN) throw new ForbiddenException("ADMIN access required"); return u;}
    private void reviewed(OwnerVerification v, User admin){v.setReviewedAt(OffsetDateTime.now());v.setReviewedBy(admin);}
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private OwnerVerificationDto ownerDto(OwnerVerification v){return new OwnerVerificationDto(v.getId(),v.getOwner().getId(),v.getStatus(),v.getSubmittedAt(),v.getReviewedAt(),v.getReviewNote(),v.getVerificationMethod());}
    private AdminOwnerVerificationDto adminDto(OwnerVerification v){return new AdminOwnerVerificationDto(v.getId(),v.getOwner().getId(),v.getStatus(),v.getOwnerNote(),v.getReviewNote(),v.getVerificationMethod(),v.getSubmittedAt(),v.getReviewedAt(),v.getReviewedBy()==null?null:v.getReviewedBy().getId(),v.getCreatedAt(),v.getUpdatedAt());}
}
