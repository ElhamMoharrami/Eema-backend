package com.eema.backend.controller;
import com.eema.backend.dto.request.CreatePayoutDestinationRequest;
import com.eema.backend.dto.response.PayoutDestinationDto;
import com.eema.backend.service.OwnerPayoutDestinationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/owner/payout-destinations") @RequiredArgsConstructor @PreAuthorize("hasRole('OWNER')")
public class OwnerPayoutDestinationController{
 private final OwnerPayoutDestinationService service;
 @GetMapping public List<PayoutDestinationDto> list(){return service.mine();}
 @PostMapping public PayoutDestinationDto submit(@Valid @RequestBody CreatePayoutDestinationRequest request){return service.submit(request);}
}
