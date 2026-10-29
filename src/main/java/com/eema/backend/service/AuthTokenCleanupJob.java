package com.eema.backend.service;

import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component @RequiredArgsConstructor
public class AuthTokenCleanupJob {
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;

    @Scheduled(fixedDelayString = "${app.auth-recovery.cleanup-delay-ms:3600000}")
    @Transactional
    public void cleanup() {
        OffsetDateTime now = OffsetDateTime.now();
        verificationTokens.deleteAllInBatch(verificationTokens.findCleanupCandidates(now, PageRequest.of(0, 100)));
        resetTokens.deleteAllInBatch(resetTokens.findCleanupCandidates(now, PageRequest.of(0, 100)));
    }
}
