package com.eema.backend.controller;
import com.eema.backend.dto.request.OwnerVerificationSubmitRequest;
import com.eema.backend.dto.response.OwnerVerificationDto;
import com.eema.backend.service.OwnerVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/owner/verification") @RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerVerificationController {
    private final OwnerVerificationService service;
    @GetMapping public OwnerVerificationDto get(){return service.mine();}
    @PostMapping("/submit") @PreAuthorize("hasRole('OWNER') and @verifiedUserPolicy.isVerified()")
    public OwnerVerificationDto submit(@Valid @RequestBody(required=false) OwnerVerificationSubmitRequest request){
        return service.submit(request==null?new OwnerVerificationSubmitRequest(null):request);
    }
}
