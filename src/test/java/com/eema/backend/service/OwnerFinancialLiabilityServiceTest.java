package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnerFinancialLiabilityServiceTest {
    private final OwnerFinancialLiabilityRepository repository = mock(OwnerFinancialLiabilityRepository.class);
    private final BookingEventRepository events = mock(BookingEventRepository.class);
    private final OwnerFinancialLiabilityService service = new OwnerFinancialLiabilityService(repository, events);

    @Test
    void settleUsesStoredAmountAuditsAndIsIdempotent() {
        UUID id = UUID.randomUUID();
        OwnerFinancialLiability liability = OwnerFinancialLiability.builder().id(id).bookingId(UUID.randomUUID())
                .ownerId(UUID.randomUUID()).type(OwnerFinancialLiabilityType.PROVIDER_COST)
                .amountEur(new BigDecimal("15.00")).status(OwnerFinancialLiabilityStatus.OPEN).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(liability));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        User admin = User.builder().id(UUID.randomUUID()).email("admin@example.test").role(UserRole.ADMIN).build();

        assertThat(service.settle(id, admin).status()).isEqualTo(OwnerFinancialLiabilityStatus.SETTLED);
        assertThat(liability.getSettledAt()).isNotNull();
        verify(events).save(argThat(e -> e.getEventType() == BookingEventType.OWNER_LIABILITY_SETTLED
                && e.getActorType() == ActorType.ADMIN));
        service.settle(id, admin);
        verify(events, times(1)).save(any());
    }

    @Test
    void nonAdminCannotSettleAndWaivedCannotBeSettled() {
        assertThatThrownBy(() -> service.settle(UUID.randomUUID(), User.builder().role(UserRole.OWNER).build()))
                .isInstanceOf(ForbiddenException.class);
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(OwnerFinancialLiability.builder()
                .id(id).status(OwnerFinancialLiabilityStatus.WAIVED).build()));
        assertThatThrownBy(() -> service.settle(id, User.builder().role(UserRole.ADMIN).build()))
                .isInstanceOf(ConflictException.class);
    }
}
