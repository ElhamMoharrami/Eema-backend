package com.eema.backend.controller;

import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.service.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdminOwnerFinancialLiabilityControllerTest {
    private final OwnerFinancialLiabilityService service = mock(OwnerFinancialLiabilityService.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final AdminOwnerFinancialLiabilityController controller = new AdminOwnerFinancialLiabilityController(service, current);

    @Test
    void scoutAndOwnerAreDeniedAllLiabilityOperations() {
        for (UserRole role : new UserRole[]{UserRole.SCOUT, UserRole.OWNER}) {
            when(current.getCurrentUser()).thenReturn(User.builder().role(role).build());
            assertThatThrownBy(() -> controller.list(null, null, null, null,
                    org.springframework.data.domain.Pageable.unpaged())).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> controller.get(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> controller.settle(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
        }
        verifyNoInteractions(service);
    }
}
