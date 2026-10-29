package com.eema.backend.service;

import com.eema.backend.dto.response.OwnerFinancialLiabilityDto;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class OwnerFinancialLiabilityService {
    private final OwnerFinancialLiabilityRepository liabilities;
    private final BookingEventRepository events;

    @Transactional(readOnly = true)
    public Page<OwnerFinancialLiabilityDto> list(OwnerFinancialLiabilityStatus status,
            OwnerFinancialLiabilityType type, UUID ownerId, UUID bookingId, Pageable pageable) {
        return liabilities.search(status, type, ownerId, bookingId, AdminPagePolicy.bounded(pageable)).map(this::dto);
    }

    @Transactional(readOnly = true)
    public OwnerFinancialLiabilityDto get(UUID id) {
        return dto(liabilities.findById(id).orElseThrow(() -> new NotFoundException("Owner liability not found: " + id)));
    }

    @Transactional
    public OwnerFinancialLiabilityDto settle(UUID id, User admin) {
        requireAdmin(admin);
        OwnerFinancialLiability liability = liabilities.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Owner liability not found: " + id));
        if (liability.getStatus() == OwnerFinancialLiabilityStatus.SETTLED) return dto(liability);
        if (liability.getStatus() != OwnerFinancialLiabilityStatus.OPEN)
            throw new ConflictException("Only OPEN liabilities can be settled");
        liability.setStatus(OwnerFinancialLiabilityStatus.SETTLED);
        liability.setSettledAt(OffsetDateTime.now());
        events.save(BookingEvent.builder().bookingId(liability.getBookingId())
                .eventType(BookingEventType.OWNER_LIABILITY_SETTLED).actorId(admin.getId())
                .actorName(actorName(admin)).actorType(ActorType.ADMIN)
                .payload(new BookingEventPayload(Map.of("liabilityId", liability.getId(),
                        "type", liability.getType().name(), "amountEur", liability.getAmountEur()))).build());
        return dto(liabilities.save(liability));
    }

    public OwnerFinancialLiabilityDto dto(OwnerFinancialLiability l) {
        return new OwnerFinancialLiabilityDto(l.getId(), l.getOwnerId(), l.getBookingId(), l.getType(),
                l.getAmountEur(), l.getStatus(), l.getNote(), l.getCreatedAt(), l.getSettledAt());
    }
    private void requireAdmin(User user) { if (user == null || user.getRole() != UserRole.ADMIN) throw new ForbiddenException("Only administrators can settle liabilities"); }
    private String actorName(User u) { return u.getFullName() == null || u.getFullName().isBlank() ? u.getEmail() : u.getFullName(); }
}
