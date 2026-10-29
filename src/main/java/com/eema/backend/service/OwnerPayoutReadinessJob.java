package com.eema.backend.service;

import com.eema.backend.entity.OwnerPayout;
import com.eema.backend.repository.OwnerPayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Component @RequiredArgsConstructor
public class OwnerPayoutReadinessJob {
    private final OwnerPayoutRepository payouts;
    private final OwnerPayoutService service;

    @Scheduled(fixedDelayString = "${app.payout.readiness-scan-delay-ms:300000}")
    @Transactional
    public void scan() {
        for (OwnerPayout payout : payouts.lockDueForReadiness(OffsetDateTime.now(), 100)) {
            service.evaluateReadiness(payout.getId());
        }
    }
}
