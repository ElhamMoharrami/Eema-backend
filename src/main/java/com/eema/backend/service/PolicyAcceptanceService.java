package com.eema.backend.service;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class PolicyAcceptanceService {
    public static final Set<PolicyType> REQUIRED=Collections.unmodifiableSet(EnumSet.allOf(PolicyType.class));
    public static final Set<PolicyType> BOOKING_SNAPSHOT=Collections.unmodifiableSet(EnumSet.of(
            PolicyType.TERMS_OF_SERVICE,PolicyType.ACCEPTABLE_USE_POLICY,PolicyType.PRODUCTION_POLICY,PolicyType.INSURANCE_REQUIREMENTS));
    private final PolicyVersionRepository versions; private final PolicyAcceptanceRepository acceptances;
    private final BookingPolicyVersionRepository bookingPolicies;

    @Transactional(readOnly=true) public List<PolicyVersionDto> current(User user){
        Set<UUID> accepted=acceptances.findPolicyVersionIdByUserId(user.getId());
        return currentRequired().stream().map(p->dto(p,accepted.contains(p.getId()))).toList();
    }
    @Transactional(readOnly=true) public PolicyVersionDto current(PolicyType type,User user){
        PolicyVersion p=versions.findByPolicyTypeAndStatus(type,PolicyStatus.PUBLISHED).orElseThrow(()->new NotFoundException("No published policy for type "+type));
        return dto(p,acceptances.existsByUserIdAndPolicyVersionId(user.getId(),p.getId()));
    }
    @Transactional(readOnly=true) public PolicyStatusDto status(User user){
        Set<UUID> accepted=acceptances.findPolicyVersionIdByUserId(user.getId());
        List<PolicyVersion> current=currentRequired();
        List<PolicyVersionDto> missing=current.stream().filter(p->!accepted.contains(p.getId())).map(p->dto(p,false)).toList();
        return new PolicyStatusDto(missing.isEmpty()&&hasEveryRequiredType(current),missing);
    }
    @Transactional public PolicyStatusDto accept(User user,Set<UUID> ids,String requestId,String agent){
        List<PolicyVersion> supplied=new ArrayList<>(versions.findAllById(ids));
        if(supplied.size()!=ids.size())throw new BadRequestException("One or more policy versions do not exist");
        Map<PolicyType,PolicyVersion> current=byType(currentRequired());
        for(PolicyVersion p:supplied){PolicyVersion active=current.get(p.getPolicyType());if(p.getStatus()!=PolicyStatus.PUBLISHED||active==null||!active.getId().equals(p.getId()))throw new BadRequestException("Only exact current published policy versions may be accepted");}
        OffsetDateTime now=OffsetDateTime.now();
        // Consistent lock order also prevents deadlocks for overlapping acceptance sets.
        supplied.sort(Comparator.comparing(PolicyVersion::getId));
        for(PolicyVersion p:supplied)if(!acceptances.existsByUserIdAndPolicyVersionId(user.getId(),p.getId())){
            acceptances.insertIfAbsent(UUID.randomUUID(),user.getId(),p.getId(),now,requestId,summarize(agent));
        }
        return status(user);
    }
    @Transactional(readOnly=true) public void requireCurrentPolicies(User user){if(user.getRole()==UserRole.ADMIN)return;PolicyStatusDto s=status(user);if(!s.allRequiredAccepted())throw new ForbiddenException("Current required policies must be accepted before this action");}
    @Transactional public void snapshotForBooking(UUID bookingId,User user){
        requireCurrentPolicies(user); Map<PolicyType,PolicyVersion> current=byType(currentRequired());
        for(PolicyType type:BOOKING_SNAPSHOT)bookingPolicies.save(BookingPolicyVersion.builder().bookingId(bookingId).policyType(type).policyVersionId(current.get(type).getId()).build());
    }
    private List<PolicyVersion> currentRequired(){return versions.findByStatusOrderByPolicyTypeAsc(PolicyStatus.PUBLISHED).stream().filter(p->REQUIRED.contains(p.getPolicyType())).toList();}
    private boolean hasEveryRequiredType(List<PolicyVersion> p){return p.stream().map(PolicyVersion::getPolicyType).collect(java.util.stream.Collectors.toSet()).containsAll(REQUIRED);}
    private Map<PolicyType,PolicyVersion> byType(List<PolicyVersion> list){Map<PolicyType,PolicyVersion> m=new EnumMap<>(PolicyType.class);list.forEach(p->m.put(p.getPolicyType(),p));return m;}
    private String summarize(String value){if(value==null||value.isBlank())return null;String cleaned=value.replaceAll("[\\r\\n]"," ").trim();return cleaned.substring(0,Math.min(200,cleaned.length()));}
    public static String sha256(String content){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public static PolicyVersionDto dto(PolicyVersion p,boolean accepted){return new PolicyVersionDto(p.getId(),p.getPolicyType(),p.getVersion(),p.getTitle(),p.getEffectiveAt(),p.getPublishedAt(),p.getContent(),p.getContentHash(),p.getStatus(),p.getCreatedAt(),accepted);}
}
