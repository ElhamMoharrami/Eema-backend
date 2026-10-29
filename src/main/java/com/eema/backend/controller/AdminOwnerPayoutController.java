package com.eema.backend.controller;

import com.eema.backend.dto.request.MarkOwnerPayoutPaidRequest;
import com.eema.backend.dto.response.OwnerPayoutDto;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.OwnerPayoutStatus;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.service.CurrentUserService;
import com.eema.backend.service.OwnerPayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.UUID;

@RestController
@RequestMapping("/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOwnerPayoutController {
    private final OwnerPayoutService payouts;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Page<OwnerPayoutDto> list(@RequestParam(required = false) OwnerPayoutStatus status,
                                    @RequestParam(required = false) UUID ownerId,
                                    @RequestParam(required = false) UUID bookingId,
                                    @RequestParam(required = false) UUID venueId,
                                    @PageableDefault(size = 20) Pageable pageable) {
        requireAdmin();
        return payouts.list(status, ownerId, bookingId, venueId, pageable);
    }

    @GetMapping("/{id}")
    public OwnerPayoutDto get(@PathVariable UUID id) {
        requireAdmin();
        return payouts.get(id);
    }

    @PostMapping("/{id}/mark-paid")
    public OwnerPayoutDto markPaid(@PathVariable UUID id, @Valid @RequestBody MarkOwnerPayoutPaidRequest request) {
        User admin = requireAdmin();
        return payouts.markPaid(id, request.externalTransferReference(), admin);
    }

    private User requireAdmin() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != UserRole.ADMIN) throw new ForbiddenException("Only administrators can manage payouts");
        return user;
    }
}
