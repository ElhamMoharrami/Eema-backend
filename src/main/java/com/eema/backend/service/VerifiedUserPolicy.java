package com.eema.backend.service;

import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import org.springframework.stereotype.Component;

@Component("verifiedUserPolicy")
public class VerifiedUserPolicy {
    private final CurrentUserService currentUsers;
    public VerifiedUserPolicy(CurrentUserService currentUsers) { this.currentUsers = currentUsers; }

    public boolean isVerified() {
        User user = currentUsers.getCurrentUser();
        return user.isActive() && (user.getRole() == UserRole.ADMIN || user.isEmailVerified());
    }
}
