package com.eema.backend.controller;
import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.*;
import com.eema.backend.entity.enums.VenueStatus;
import com.eema.backend.service.VenueModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/admin/venues") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVenueModerationController {
    private final VenueModerationService service;
    @GetMapping public Page<AdminVenueModerationDto> list(@RequestParam(required=false) VenueStatus status,
            @RequestParam(required=false) UUID ownerId,@RequestParam(required=false) UUID venueId,Pageable pageable){return service.adminList(status,ownerId,venueId,pageable);}
    @GetMapping("/{id}") public AdminVenueModerationDto get(@PathVariable UUID id){return service.adminGet(id);}
    @GetMapping("/{id}/events") public List<TrustEventDto> events(@PathVariable UUID id){return service.events(id);}
    @PostMapping("/{id}/approve") public AdminVenueModerationDto approve(@PathVariable UUID id,@Valid @RequestBody(required=false) OptionalNoteRequest request){return service.approve(id,request==null?null:request.note());}
    @PostMapping("/{id}/request-changes") public AdminVenueModerationDto changes(@PathVariable UUID id,@Valid @RequestBody ReasonRequest request){return service.requestChanges(id,request.reason().trim());}
    @PostMapping("/{id}/suspend") public AdminVenueModerationDto suspend(@PathVariable UUID id,@Valid @RequestBody ReasonRequest request){return service.suspend(id,request.reason().trim());}
    @PostMapping("/{id}/reactivate") public AdminVenueModerationDto reactivate(@PathVariable UUID id,@Valid @RequestBody(required=false) OptionalNoteRequest request){return service.reactivate(id,request==null?null:request.note());}
}
