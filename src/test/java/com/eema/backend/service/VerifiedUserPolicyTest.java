package com.eema.backend.service;

import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VerifiedUserPolicyTest {
    private final CurrentUserService currentUsers = mock(CurrentUserService.class);
    private final VerifiedUserPolicy policy = new VerifiedUserPolicy(currentUsers);

    @Test
    void requiresCurrentDatabaseVerificationForMarketplaceUsers() {
        User user = User.builder().role(UserRole.SCOUT).active(true).emailVerified(false).build();
        when(currentUsers.getCurrentUser()).thenReturn(user);
        assertThat(policy.isVerified()).isFalse();

        user.setEmailVerified(true);
        assertThat(policy.isVerified()).isTrue();

        user.setActive(false);
        assertThat(policy.isVerified()).isFalse();
    }

    @Test
    void activeAdminOperationsAreNotBlockedByMarketplaceVerification() {
        when(currentUsers.getCurrentUser()).thenReturn(
                User.builder().role(UserRole.ADMIN).active(true).emailVerified(false).build());
        assertThat(policy.isVerified()).isTrue();
    }
}
