package com.eema.backend.service;

import com.eema.backend.dto.request.CreatePayoutDestinationRequest;
import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.eema.backend.event.TrustNotificationEvent;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class OwnerPayoutDestinationService {
 private final OwnerPayoutDestinationRepository destinations; private final CurrentUserService current;
 private final OwnerVerificationService ownerVerification; private final VerifiedUserPolicy verifiedUsers;
 private final IbanPolicy ibanPolicy; private final PayoutDestinationCrypto crypto;
 private final MarketplaceTrustAuditService audit; private final OwnerPayoutService payouts;
 private final ApplicationEventPublisher publisher; private final PolicyAcceptanceService policyAcceptances;

 @Transactional(readOnly=true) public List<PayoutDestinationDto> mine(){User owner=requireOwner();return destinations.findByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream().map(this::dto).toList();}
 @Transactional public PayoutDestinationDto submit(CreatePayoutDestinationRequest request){User owner=requireOwner();lockOwner(owner.getId());policyAcceptances.requireCurrentPolicies(owner);verifiedUsers.requireVerified(owner);if(!ownerVerification.isVerified(owner.getId()))throw new ForbiddenException("Owner verification is required before adding a payout destination");String iban=ibanPolicy.normalizeAndValidate(request.iban());String fingerprint=crypto.fingerprint(iban);if(destinations.existsByIbanFingerprintAndOwnerId(fingerprint,owner.getId()))throw new ConflictException("This payout destination was already submitted");OwnerPayoutDestination d=OwnerPayoutDestination.builder().ownerId(owner.getId()).accountHolderName(request.accountHolderName().trim()).ibanEncrypted(crypto.encrypt(iban)).ibanFingerprint(fingerprint).ibanLast4(iban.substring(iban.length()-4)).status(PayoutDestinationStatus.PENDING_VERIFICATION).submittedAt(OffsetDateTime.now()).build();d=destinations.save(d);UUID notificationEventId=audit.append(TrustSubjectType.PAYOUT_DESTINATION,d.getId(),TrustEventType.PAYOUT_DESTINATION_SUBMITTED,owner.getId(),ActorType.OWNER,null);publisher.publishEvent(new TrustNotificationEvent(TrustEventType.PAYOUT_DESTINATION_SUBMITTED,d.getId(),null,notificationEventId));return dto(d);}
 @Transactional(readOnly=true) public Page<PayoutDestinationDto> adminList(PayoutDestinationStatus status,UUID ownerId,UUID destinationId,Pageable pageable){Specification<OwnerPayoutDestination>s=Specification.where(null);if(status!=null)s=s.and((r,q,b)->b.equal(r.get("status"),status));if(ownerId!=null)s=s.and((r,q,b)->b.equal(r.get("ownerId"),ownerId));if(destinationId!=null)s=s.and((r,q,b)->b.equal(r.get("id"),destinationId));return destinations.findAll(s,AdminPagePolicy.bounded(pageable)).map(this::dto);}
 @Transactional(readOnly=true) public PayoutDestinationDto adminGet(UUID id){return dto(get(id));}
 @Transactional public PayoutDestinationDto verify(UUID id,String note,User admin){requireAdmin(admin);OwnerPayoutDestination d=lockedPending(id);OffsetDateTime now=OffsetDateTime.now();for(OwnerPayoutDestination old:destinations.findCurrentForUpdate(d.getOwnerId(),PayoutDestinationStatus.VERIFIED)){old.setStatus(PayoutDestinationStatus.REPLACED);old.setReplacedAt(now);destinations.save(old);audit.append(TrustSubjectType.PAYOUT_DESTINATION,old.getId(),TrustEventType.PAYOUT_DESTINATION_REPLACED,admin.getId(),ActorType.ADMIN,null);}
 // Release the partial unique-index slot before verifying the replacement.
 destinations.flush();
 d.setStatus(PayoutDestinationStatus.VERIFIED);d.setVerifiedAt(now);d.setVerifiedBy(admin.getId());d=destinations.save(d);UUID notificationEventId=audit.append(TrustSubjectType.PAYOUT_DESTINATION,d.getId(),TrustEventType.PAYOUT_DESTINATION_VERIFIED,admin.getId(),ActorType.ADMIN,safe(note));publisher.publishEvent(new TrustNotificationEvent(TrustEventType.PAYOUT_DESTINATION_VERIFIED,d.getId(),null,notificationEventId));payouts.reconcileHeldForOwner(d.getOwnerId());return dto(d);}
 @Transactional public PayoutDestinationDto reject(UUID id,String reason,User admin){requireAdmin(admin);if(reason==null||reason.isBlank())throw new BadRequestException("Rejection reason is required");OwnerPayoutDestination d=lockedPending(id);d.setStatus(PayoutDestinationStatus.REJECTED);d.setRejectedAt(OffsetDateTime.now());d.setRejectionReason(reason.trim());d=destinations.save(d);UUID notificationEventId=audit.append(TrustSubjectType.PAYOUT_DESTINATION,d.getId(),TrustEventType.PAYOUT_DESTINATION_REJECTED,admin.getId(),ActorType.ADMIN,d.getRejectionReason());publisher.publishEvent(new TrustNotificationEvent(TrustEventType.PAYOUT_DESTINATION_REJECTED,d.getId(),d.getRejectionReason(),notificationEventId));return dto(d);}
 @Transactional public PayoutDestinationRevealDto reveal(UUID id,User admin){requireAdmin(admin);OwnerPayoutDestination d=get(id);if(d.getStatus()!=PayoutDestinationStatus.PENDING_VERIFICATION&&d.getStatus()!=PayoutDestinationStatus.VERIFIED&&d.getStatus()!=PayoutDestinationStatus.REPLACED)throw new ConflictException("This payout destination is not available for operational reveal");audit.append(TrustSubjectType.PAYOUT_DESTINATION,d.getId(),TrustEventType.PAYOUT_DESTINATION_REVEALED,admin.getId(),ActorType.ADMIN,null);return new PayoutDestinationRevealDto(d.getId(),d.getAccountHolderName(),crypto.decrypt(d.getIbanEncrypted()));}
 public String masked(OwnerPayoutDestination d){return d==null?null:"••••••••••"+d.getIbanLast4();}
 private PayoutDestinationDto dto(OwnerPayoutDestination d){boolean other=destinations.findByOwnerIdAndStatus(d.getOwnerId(),PayoutDestinationStatus.VERIFIED).filter(x->!x.getId().equals(d.getId())).isPresent();return new PayoutDestinationDto(d.getId(),d.getOwnerId(),d.getType(),d.getCurrency(),d.getAccountHolderName(),masked(d),d.getStatus(),d.getSubmittedAt(),d.getVerifiedAt(),d.getRejectedAt(),d.getRejectionReason(),d.getReplacedAt(),other);}
 private void lockOwner(UUID ownerId){destinations.lockOwnerForMutation(ownerId).orElseThrow(()->new NotFoundException("Owner not found"));}
 private OwnerPayoutDestination lockedPending(UUID id){UUID ownerId=destinations.findOwnerIdById(id).orElseThrow(()->new NotFoundException("Payout destination not found"));lockOwner(ownerId);OwnerPayoutDestination d=destinations.findByIdForUpdate(id).orElseThrow(()->new NotFoundException("Payout destination not found"));if(d.getStatus()!=PayoutDestinationStatus.PENDING_VERIFICATION)throw new ConflictException("Payout destination is not pending verification");return d;}
 private OwnerPayoutDestination get(UUID id){return destinations.findById(id).orElseThrow(()->new NotFoundException("Payout destination not found"));}
 private User requireOwner(){User u=current.getCurrentUser();if(u.getRole()!=UserRole.OWNER)throw new ForbiddenException("Only owners can manage payout destinations");return u;}
 private void requireAdmin(User u){if(u==null||u.getRole()!=UserRole.ADMIN)throw new ForbiddenException("Only administrators can review payout destinations");}
 private String safe(String s){return s==null||s.isBlank()?null:s.trim();}
}
