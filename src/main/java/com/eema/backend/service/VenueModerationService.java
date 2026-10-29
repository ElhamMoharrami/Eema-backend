package com.eema.backend.service;

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
import java.util.*;

@Service @RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class VenueModerationService {
    private final VenueRepository venues;
    private final CurrentUserService currentUsers;
    private final OwnerVerificationService ownerVerifications;
    private final VenueProductionProfileValidator readiness;
    private final MarketplaceTrustAuditService audit;
    private final MarketplaceTrustEventRepository auditEvents;
    private final ApplicationEventPublisher events;
    private final PolicyAcceptanceService policyAcceptances;
    VenueModerationService(VenueRepository a,CurrentUserService b,OwnerVerificationService c,VenueProductionProfileValidator d,MarketplaceTrustAuditService e,MarketplaceTrustEventRepository f,ApplicationEventPublisher g){this(a,b,c,d,e,f,g,null);}

    @Transactional
    public VenueModerationDto submit(UUID id) {
        User owner=currentUsers.getCurrentUser();
        if(policyAcceptances!=null) policyAcceptances.requireCurrentPolicies(owner);
        Venue venue=locked(id);
        if(owner.getRole()!=UserRole.OWNER || !venue.getOwner().getId().equals(owner.getId()))
            throw new ForbiddenException("Only the venue owner can submit this venue");
        if(!owner.isEmailVerified()) throw new ForbiddenException("Verified email is required");
        if(!ownerVerifications.isVerified(owner.getId())) throw new ForbiddenException("Approved owner verification is required");
        if(venue.getStatus()!=VenueStatus.DRAFT) throw new ConflictException("Only a draft venue can be submitted for review");
        requireReady(venue);
        venue.setStatus(VenueStatus.PENDING);
        venue.setSubmittedForReviewAt(OffsetDateTime.now());
        venue.setReviewedAt(null); venue.setReviewedBy(null); venue.setModerationReason(null);
        UUID notificationEventId=append(venue, TrustEventType.VENUE_SUBMITTED, owner, ActorType.OWNER, null);
        events.publishEvent(new TrustNotificationEvent(TrustEventType.VENUE_SUBMITTED, venue.getId(), null,notificationEventId));
        return ownerDto(venue);
    }

    @Transactional(readOnly=true)
    public VenueModerationDto ownerDetail(UUID id) {
        User user=currentUsers.getCurrentUser(); Venue venue=get(id);
        if(user.getRole()!=UserRole.ADMIN && !venue.getOwner().getId().equals(user.getId())) throw new ForbiddenException("Access denied");
        return ownerDto(venue);
    }

    @Transactional(readOnly=true)
    public Page<AdminVenueModerationDto> adminList(VenueStatus status, UUID ownerId, UUID venueId, Pageable pageable) {
        Specification<Venue> spec=Specification.where(null);
        if(status!=null) spec=spec.and((r,q,b)->b.equal(r.get("status"),status));
        if(ownerId!=null) spec=spec.and((r,q,b)->b.equal(r.get("owner").get("id"),ownerId));
        if(venueId!=null) spec=spec.and((r,q,b)->b.equal(r.get("id"),venueId));
        return venues.findAll(spec,AdminPagePolicy.bounded(pageable)).map(this::adminDto);
    }
    @Transactional(readOnly=true) public AdminVenueModerationDto adminGet(UUID id){return adminDto(get(id));}
    @Transactional(readOnly=true) public List<TrustEventDto> events(UUID id){get(id);return auditEvents.findBySubjectTypeAndSubjectIdOrderByCreatedAtAsc(TrustSubjectType.VENUE,id).stream().map(e->new TrustEventDto(e.getId(),e.getEventType(),e.getActorId(),e.getActorType(),e.getNote(),e.getCreatedAt())).toList();}

    @Transactional public AdminVenueModerationDto approve(UUID id, String note){return activate(id,VenueStatus.PENDING,TrustEventType.VENUE_APPROVED,note);}
    @Transactional public AdminVenueModerationDto reactivate(UUID id, String note){return activate(id,VenueStatus.SUSPENDED,TrustEventType.VENUE_REACTIVATED,note);}

    /** Owner changes to production/safety facts require another review; price and availability do not. */
    public void requireRereviewAfterCriticalEdit(Venue venue, User owner) {
        if (venue.getStatus() != VenueStatus.ACTIVE || owner.getRole() != UserRole.OWNER) return;
        venue.setStatus(VenueStatus.PENDING);
        venue.setSubmittedForReviewAt(OffsetDateTime.now());
        venue.setReviewedAt(null); venue.setReviewedBy(null); venue.setModerationReason(null);
        UUID notificationEventId=append(venue, TrustEventType.VENUE_SUBMITTED, owner, ActorType.OWNER, "Production-critical listing details changed");
        events.publishEvent(new TrustNotificationEvent(TrustEventType.VENUE_SUBMITTED, venue.getId(), null,notificationEventId));
    }

    @Transactional
    public AdminVenueModerationDto requestChanges(UUID id,String reason){
        User admin=requireAdmin();Venue v=locked(id);requireStatus(v,VenueStatus.PENDING);
        v.setStatus(VenueStatus.DRAFT); reviewed(v,admin,reason);
        UUID notificationEventId=append(v,TrustEventType.VENUE_CHANGES_REQUESTED,admin,ActorType.ADMIN,reason);
        events.publishEvent(new TrustNotificationEvent(TrustEventType.VENUE_CHANGES_REQUESTED,v.getId(),reason,notificationEventId));
        return adminDto(v);
    }
    @Transactional
    public AdminVenueModerationDto suspend(UUID id,String reason){
        User admin=requireAdmin();Venue v=locked(id);requireStatus(v,VenueStatus.ACTIVE);
        v.setStatus(VenueStatus.SUSPENDED);reviewed(v,admin,reason);
        UUID notificationEventId=append(v,TrustEventType.VENUE_SUSPENDED,admin,ActorType.ADMIN,reason);
        events.publishEvent(new TrustNotificationEvent(TrustEventType.VENUE_SUSPENDED,v.getId(),reason,notificationEventId));
        return adminDto(v);
    }
    private AdminVenueModerationDto activate(UUID id,VenueStatus source,TrustEventType type,String note){
        User admin=requireAdmin();Venue v=locked(id);requireStatus(v,source);
        if(!ownerVerifications.isVerified(v.getOwner().getId())) throw new ConflictException("Venue owner is not verified");
        requireReady(v);v.setStatus(VenueStatus.ACTIVE);reviewed(v,admin,clean(note));
        UUID notificationEventId=append(v,type,admin,ActorType.ADMIN,clean(note));events.publishEvent(new TrustNotificationEvent(type,v.getId(),clean(note),notificationEventId));
        return adminDto(v);
    }
    private void requireReady(Venue v){List<String> issues=readiness.readinessIssues(v);if(!issues.isEmpty())throw new ConflictException("Venue is not production-ready: "+String.join(", ",issues));}
    private void requireStatus(Venue v,VenueStatus s){if(v.getStatus()!=s)throw new ConflictException("Venue must be "+s+" for this action");}
    private Venue locked(UUID id){return venues.findByIdForUpdate(id).orElseThrow(()->new NotFoundException("Venue not found"));}
    private Venue get(UUID id){return venues.findById(id).orElseThrow(()->new NotFoundException("Venue not found"));}
    private User requireAdmin(){User u=currentUsers.getCurrentUser();if(u.getRole()!=UserRole.ADMIN)throw new ForbiddenException("ADMIN access required");return u;}
    private void reviewed(Venue v,User admin,String reason){v.setReviewedAt(OffsetDateTime.now());v.setReviewedBy(admin);v.setModerationReason(reason);}
    private UUID append(Venue v,TrustEventType type,User actor,ActorType actorType,String note){return audit.append(TrustSubjectType.VENUE,v.getId(),type,actor.getId(),actorType,note);}
    private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
    private VenueModerationDto ownerDto(Venue v){return new VenueModerationDto(v.getId(),v.getStatus(),v.getSubmittedForReviewAt(),v.getReviewedAt(),v.getModerationReason(),readiness.readinessIssues(v));}
    private AdminVenueModerationDto adminDto(Venue v){return new AdminVenueModerationDto(v.getId(),v.getOwner().getId(),v.getName(),v.getNeighbourhood(),v.getStatus(),v.getSubmittedForReviewAt(),v.getReviewedAt(),v.getReviewedBy()==null?null:v.getReviewedBy().getId(),v.getModerationReason(),readiness.readinessIssues(v),v.getCreatedAt(),v.getUpdatedAt());}
}
