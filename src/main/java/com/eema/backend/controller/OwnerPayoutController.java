package com.eema.backend.controller;

import com.eema.backend.dto.response.OwnerPayoutSummaryDto;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/owner/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class OwnerPayoutController {
    private final OwnerPayoutService payouts;
    private final FinancialDocumentService documents;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Page<OwnerPayoutSummaryDto> list(@PageableDefault(size = 20) Pageable pageable) {
        User owner = requireOwner();
        return payouts.listForOwner(owner.getId(), pageable);
    }
    @GetMapping("/{id}")
    public OwnerPayoutSummaryDto detail(@PathVariable UUID id) {
        User owner = requireOwner();
        return payouts.getForOwner(id, owner.getId());
    }
    @GetMapping("/{id}/statement")
    public ResponseEntity<byte[]> statement(@PathVariable UUID id) {
        User actor = currentUserService.getCurrentUser();
        if (actor.getRole() != UserRole.OWNER && actor.getRole() != UserRole.ADMIN)
            throw new ForbiddenException("Only owners and administrators can access payout statements");
        return FinancialDocumentController.pdf(documents.payoutStatement(id, actor));
    }
    private User requireOwner() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != UserRole.OWNER) throw new ForbiddenException("Owner payout history is limited to owners");
        return user;
    }
}
