package com.eema.backend.controller;
import com.eema.backend.dto.request.CreatePolicyVersionRequest;
import com.eema.backend.dto.response.PolicyVersionDto;
import com.eema.backend.service.AdminPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/admin/policies") @RequiredArgsConstructor
public class AdminPolicyController {
 private final AdminPolicyService policies;
 @GetMapping public List<PolicyVersionDto> list(){return policies.list();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public PolicyVersionDto create(@Valid @RequestBody CreatePolicyVersionRequest r){return policies.create(r);}
 @PostMapping("/{id}/publish") public PolicyVersionDto publish(@PathVariable UUID id){return policies.publish(id);}
 @PostMapping("/{id}/retire") public PolicyVersionDto retire(@PathVariable UUID id){return policies.retire(id);}
}
