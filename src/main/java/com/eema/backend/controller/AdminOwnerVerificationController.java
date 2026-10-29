package com.eema.backend.controller;
import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.AdminOwnerVerificationDto;
import com.eema.backend.entity.enums.OwnerVerificationStatus;
import com.eema.backend.service.OwnerVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/admin/owner-verifications") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOwnerVerificationController {
    private final OwnerVerificationService service;
    @GetMapping public Page<AdminOwnerVerificationDto> list(@RequestParam(required=false) OwnerVerificationStatus status,
            @RequestParam(required=false) UUID ownerId, Pageable pageable){return service.adminList(status,ownerId,pageable);}
    @GetMapping("/{id}") public AdminOwnerVerificationDto get(@PathVariable UUID id){return service.adminGet(id);}
    @PostMapping("/{id}/approve") public AdminOwnerVerificationDto approve(@PathVariable UUID id,@Valid @RequestBody OwnerVerificationApproveRequest request){return service.approve(id,request);}
    @PostMapping("/{id}/reject") public AdminOwnerVerificationDto reject(@PathVariable UUID id,@Valid @RequestBody ReasonRequest request){return service.reject(id,request);}
}
