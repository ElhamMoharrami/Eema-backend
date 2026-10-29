package com.eema.backend.controller;

import com.eema.backend.dto.response.OwnerFinancialLiabilityDto;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/admin/liabilities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOwnerFinancialLiabilityController {
    private final OwnerFinancialLiabilityService liabilities;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Page<OwnerFinancialLiabilityDto> list(
            @RequestParam(required = false) OwnerFinancialLiabilityStatus status,
            @RequestParam(required = false) OwnerFinancialLiabilityType type,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) UUID bookingId,
            @PageableDefault(size = 20) Pageable pageable) {
        requireAdmin();
        return liabilities.list(status, type, ownerId, bookingId, pageable);
    }
    @GetMapping("/{id}") public OwnerFinancialLiabilityDto get(@PathVariable UUID id) { requireAdmin(); return liabilities.get(id); }
    @PostMapping("/{id}/settle") public OwnerFinancialLiabilityDto settle(@PathVariable UUID id) {
        User admin = requireAdmin();
        return liabilities.settle(id, admin);
    }
    private User requireAdmin() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != UserRole.ADMIN) throw new ForbiddenException("Only administrators can manage owner liabilities");
        return user;
    }
}
