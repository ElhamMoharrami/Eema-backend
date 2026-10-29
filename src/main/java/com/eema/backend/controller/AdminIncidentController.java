package com.eema.backend.controller;
import com.eema.backend.dto.request.ResolveIncidentRequest;
import com.eema.backend.dto.response.IncidentDto;
import com.eema.backend.entity.enums.*;
import com.eema.backend.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/admin/incidents") @RequiredArgsConstructor
public class AdminIncidentController {
 private final IncidentService incidents;
 @GetMapping public Page<IncidentDto> list(@RequestParam(required=false)IncidentStatus status,@RequestParam(required=false)IncidentCategory category,@RequestParam(required=false)UUID bookingId,Pageable pageable){return incidents.adminList(status,category,bookingId,pageable);}
 @GetMapping("/{id}") public IncidentDto get(@PathVariable UUID id){return incidents.adminGet(id);}
 @PostMapping("/{id}/start-review") public IncidentDto start(@PathVariable UUID id){return incidents.startReview(id);}
 @PostMapping("/{id}/resolve") public IncidentDto resolve(@PathVariable UUID id,@Valid @RequestBody ResolveIncidentRequest r){return incidents.resolve(id,r.resolutionNote());}
 @PostMapping("/{id}/close") public IncidentDto close(@PathVariable UUID id){return incidents.close(id);}
}
