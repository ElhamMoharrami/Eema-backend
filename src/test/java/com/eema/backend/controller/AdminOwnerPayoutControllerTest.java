package com.eema.backend.controller;

import com.eema.backend.dto.request.MarkOwnerPayoutPaidRequest;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.service.CurrentUserService;
import com.eema.backend.service.OwnerPayoutService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdminOwnerPayoutControllerTest {
    private final OwnerPayoutService service = mock(OwnerPayoutService.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final AdminOwnerPayoutController controller = new AdminOwnerPayoutController(service, current);

    @Test
    void scoutAndOwnerCannotReadOrMarkPayouts() {
        for (UserRole role : new UserRole[]{UserRole.SCOUT, UserRole.OWNER}) {
            when(current.getCurrentUser()).thenReturn(User.builder().id(UUID.randomUUID()).role(role).build());
            assertThatThrownBy(() -> controller.list(null, null, null, null, org.springframework.data.domain.Pageable.unpaged())).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> controller.get(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> controller.markPaid(UUID.randomUUID(), new MarkOwnerPayoutPaidRequest("bank-1")))
                    .isInstanceOf(ForbiddenException.class);
        }
        verifyNoInteractions(service);
    }

    @Test
    void adminCanUseOperationsEndpoints() {
        User admin = User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).build();
        when(current.getCurrentUser()).thenReturn(admin);
        UUID id = UUID.randomUUID();
        controller.list(null, null, null, null, org.springframework.data.domain.Pageable.unpaged());
        controller.get(id);
        controller.markPaid(id, new MarkOwnerPayoutPaidRequest("bank-1"));
        verify(service).list(eq(null), eq(null), eq(null), eq(null), any());
        verify(service).get(id);
        verify(service).markPaid(id, "bank-1", admin);
    }
}
