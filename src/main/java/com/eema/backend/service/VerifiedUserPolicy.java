package com.eema.backend.service;

import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import org.springframework.stereotype.Component;
import com.eema.backend.exception.ForbiddenException;

@Component("verifiedUserPolicy")
public class VerifiedUserPolicy {
    private final CurrentUserService currentUsers;
    public VerifiedUserPolicy(CurrentUserService currentUsers) { this.currentUsers = currentUsers; }

    public boolean isVerified() {
        User user = currentUsers.getCurrentUser();
        return user.isActive() && (user.getRole() == UserRole.ADMIN || user.isEmailVerified());
    }

    public void requireVerified(User user) {
        if (user == null || !user.isActive() || (user.getRole() != UserRole.ADMIN && !user.isEmailVerified()))
            throw new ForbiddenException("Verified email is required for this action");
    }
}
