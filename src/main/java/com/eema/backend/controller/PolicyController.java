package com.eema.backend.controller;
import com.eema.backend.dto.request.AcceptPoliciesRequest;
import com.eema.backend.dto.response.*;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.PolicyType;
import com.eema.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/policies") @RequiredArgsConstructor
public class PolicyController {
 private final PolicyAcceptanceService policies;private final CurrentUserService current;
 @GetMapping("/current") public List<PolicyVersionDto> current(){return policies.current(current.getCurrentUser());}
 @GetMapping("/{type}/current") public PolicyVersionDto current(@PathVariable PolicyType type){return policies.current(type,current.getCurrentUser());}
 @GetMapping("/status") public PolicyStatusDto status(){return policies.status(current.getCurrentUser());}
 @PostMapping("/accept") public PolicyStatusDto accept(@Valid @RequestBody AcceptPoliciesRequest request,HttpServletRequest http){User u=current.getCurrentUser();return policies.accept(u,request.policyVersionIds(),MDC.get("requestId"),http.getHeader("User-Agent"));}
}
