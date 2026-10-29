package com.eema.backend.service;
import com.eema.backend.dto.request.CreatePolicyVersionRequest;
import com.eema.backend.dto.response.PolicyVersionDto;
import com.eema.backend.entity.PolicyVersion;
import com.eema.backend.entity.enums.PolicyStatus;
import com.eema.backend.exception.*;
import com.eema.backend.repository.PolicyVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdminPolicyService {
 private final PolicyVersionRepository versions;
 @Transactional(readOnly=true) public List<PolicyVersionDto> list(){return versions.findAllByOrderByPolicyTypeAscCreatedAtDesc().stream().map(p->PolicyAcceptanceService.dto(p,false)).toList();}
 @Transactional public PolicyVersionDto create(CreatePolicyVersionRequest r){
   try{return PolicyAcceptanceService.dto(versions.saveAndFlush(PolicyVersion.builder().policyType(r.policyType()).version(r.version().trim()).title(r.title().trim()).effectiveAt(r.effectiveAt()).content(r.content()).status(PolicyStatus.DRAFT).build()),false);}
   catch(DataIntegrityViolationException e){throw new ConflictException("That policy type and version already exists");}
 }
 @Transactional public PolicyVersionDto publish(UUID id){
   PolicyVersion target=locked(id);if(target.getStatus()!=PolicyStatus.DRAFT)throw new ConflictException("Only a draft policy can be published");
   versions.findByPolicyTypeAndStatus(target.getPolicyType(),PolicyStatus.PUBLISHED).ifPresent(old->{old.retire();versions.saveAndFlush(old);});
   target.publish(PolicyAcceptanceService.sha256(target.getContent()),OffsetDateTime.now());
   try{return PolicyAcceptanceService.dto(versions.saveAndFlush(target),false);}catch(DataIntegrityViolationException e){throw new ConflictException("Another published version already exists for this policy type");}
 }
 @Transactional public PolicyVersionDto retire(UUID id){PolicyVersion p=locked(id);if(p.getStatus()!=PolicyStatus.PUBLISHED)throw new ConflictException("Only a published policy can be retired");p.retire();return PolicyAcceptanceService.dto(p,false);}
 private PolicyVersion locked(UUID id){return versions.findByIdForUpdate(id).orElseThrow(()->new NotFoundException("Policy version not found"));}
}
