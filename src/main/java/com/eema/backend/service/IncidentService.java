package com.eema.backend.service;
import com.eema.backend.dto.request.CreateIncidentRequest;
import com.eema.backend.dto.response.IncidentDto;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class IncidentService {
 private final IncidentReportRepository incidents;private final BookingRepository bookings;private final VenueRepository venues;
 private final CurrentUserService current;private final MarketplaceTrustAuditService audit;
 @Transactional public IncidentDto report(UUID bookingId,CreateIncidentRequest request){User user=current.getCurrentUser();Booking booking=booking(bookingId);authorize(user,booking);IncidentReport incident=incidents.save(IncidentReport.builder().bookingId(bookingId).reportedByUserId(user.getId()).category(request.category()).description(request.description().trim()).status(IncidentStatus.OPEN).build());audit.append(TrustSubjectType.INCIDENT,incident.getId(),TrustEventType.INCIDENT_REPORTED,user.getId(),actor(user),null);return userDto(incident);}
 @Transactional(readOnly=true) public List<IncidentDto> forBooking(UUID bookingId){User user=current.getCurrentUser();authorize(user,booking(bookingId));return incidents.findByBookingIdOrderByCreatedAtAsc(bookingId).stream().map(this::userDto).toList();}
 @Transactional(readOnly=true) public Page<IncidentDto> adminList(IncidentStatus status,IncidentCategory category,UUID bookingId,Pageable pageable){Specification<IncidentReport>s=Specification.where(null);if(status!=null)s=s.and((r,q,b)->b.equal(r.get("status"),status));if(category!=null)s=s.and((r,q,b)->b.equal(r.get("category"),category));if(bookingId!=null)s=s.and((r,q,b)->b.equal(r.get("bookingId"),bookingId));return incidents.findAll(s,AdminPagePolicy.bounded(pageable)).map(this::adminDto);}
 @Transactional(readOnly=true) public IncidentDto adminGet(UUID id){return adminDto(get(id));}
 @Transactional public IncidentDto startReview(UUID id){User admin=requireAdmin();IncidentReport i=locked(id);try{i.startReview();}catch(IllegalStateException e){throw new ConflictException(e.getMessage());}audit.append(TrustSubjectType.INCIDENT,i.getId(),TrustEventType.INCIDENT_REVIEW_STARTED,admin.getId(),ActorType.ADMIN,null);return adminDto(i);}
 @Transactional public IncidentDto resolve(UUID id,String note){User admin=requireAdmin();IncidentReport i=locked(id);String clean=note.trim();try{i.resolve(clean,OffsetDateTime.now());}catch(IllegalStateException e){throw new ConflictException(e.getMessage());}audit.append(TrustSubjectType.INCIDENT,i.getId(),TrustEventType.INCIDENT_RESOLVED,admin.getId(),ActorType.ADMIN,null);return adminDto(i);}
 @Transactional public IncidentDto close(UUID id){User admin=requireAdmin();IncidentReport i=locked(id);try{i.close();}catch(IllegalStateException e){throw new ConflictException(e.getMessage());}audit.append(TrustSubjectType.INCIDENT,i.getId(),TrustEventType.INCIDENT_CLOSED,admin.getId(),ActorType.ADMIN,null);return adminDto(i);}
 private void authorize(User u,Booking b){if(u.getRole()==UserRole.ADMIN||b.getScoutId().equals(u.getId()))return;Venue v=venues.findById(b.getVenueId()).orElseThrow(()->new NotFoundException("Venue not found"));if(u.getRole()==UserRole.OWNER&&v.getOwner().getId().equals(u.getId()))return;throw new ForbiddenException("You are not a participant in this booking");}
 private User requireAdmin(){User u=current.getCurrentUser();if(u.getRole()!=UserRole.ADMIN)throw new ForbiddenException("ADMIN access required");return u;}
 private Booking booking(UUID id){return bookings.findById(id).orElseThrow(()->new NotFoundException("Booking not found"));}
 private IncidentReport get(UUID id){return incidents.findById(id).orElseThrow(()->new NotFoundException("Incident not found"));}
 private IncidentReport locked(UUID id){return incidents.findByIdForUpdate(id).orElseThrow(()->new NotFoundException("Incident not found"));}
 private ActorType actor(User u){return switch(u.getRole()){case SCOUT->ActorType.SCOUT;case OWNER->ActorType.OWNER;case ADMIN->ActorType.ADMIN;};}
 private IncidentDto userDto(IncidentReport i){return new IncidentDto(i.getId(),i.getBookingId(),i.getReportedByUserId(),i.getCategory(),i.getDescription(),i.getStatus(),i.getCreatedAt(),i.getUpdatedAt(),i.getResolvedAt(),null);}
 private IncidentDto adminDto(IncidentReport i){return new IncidentDto(i.getId(),i.getBookingId(),i.getReportedByUserId(),i.getCategory(),i.getDescription(),i.getStatus(),i.getCreatedAt(),i.getUpdatedAt(),i.getResolvedAt(),i.getAdminResolutionNote());}
}
