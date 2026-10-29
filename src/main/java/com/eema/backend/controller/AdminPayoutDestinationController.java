package com.eema.backend.controller;
import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.*;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.PayoutDestinationStatus;
import com.eema.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/admin/payout-destinations") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminPayoutDestinationController{
 private final OwnerPayoutDestinationService service;private final CurrentUserService current;
 @GetMapping public Page<PayoutDestinationDto> list(@RequestParam(required=false)PayoutDestinationStatus status,@RequestParam(required=false)UUID ownerId,@RequestParam(required=false)UUID destinationId,@PageableDefault(size=20)Pageable pageable){return service.adminList(status,ownerId,destinationId,pageable);}
 @GetMapping("/{id}") public PayoutDestinationDto get(@PathVariable UUID id){return service.adminGet(id);}
 @PostMapping("/{id}/verify") public PayoutDestinationDto verify(@PathVariable UUID id,@Valid @RequestBody(required=false)OptionalNoteRequest request){return service.verify(id,request==null?null:request.note(),current.getCurrentUser());}
 @PostMapping("/{id}/reject") public PayoutDestinationDto reject(@PathVariable UUID id,@Valid @RequestBody ReasonRequest request){return service.reject(id,request.reason(),current.getCurrentUser());}
 @PostMapping("/{id}/reveal") public ResponseEntity<PayoutDestinationRevealDto> reveal(@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.PRAGMA,"no-cache").body(service.reveal(id,current.getCurrentUser()));}
}
