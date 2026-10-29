package com.eema.backend.service;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.*;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PolicyAcceptanceServiceTest {
 private PolicyVersionRepository versions;private PolicyAcceptanceRepository acceptances;private BookingPolicyVersionRepository snapshots;private PolicyAcceptanceService service;private User user;
 @BeforeEach void setup(){versions=mock(PolicyVersionRepository.class);acceptances=mock(PolicyAcceptanceRepository.class);snapshots=mock(BookingPolicyVersionRepository.class);service=new PolicyAcceptanceService(versions,acceptances,snapshots);user=User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).email("s@example.test").passwordHash("x").build();}
 @Test void publishingANewVersionMakesTheOldAcceptanceIncomplete(){List<PolicyVersion> v1=allCurrent("1");when(versions.findByStatusOrderByPolicyTypeAsc(PolicyStatus.PUBLISHED)).thenReturn(v1);when(acceptances.findPolicyVersionIdByUserId(user.getId())).thenReturn(ids(v1));assertTrue(service.status(user).allRequiredAccepted());List<PolicyVersion> v2=new ArrayList<>(v1);v2.set(0,published(v1.get(0).getPolicyType(),"2"));when(versions.findByStatusOrderByPolicyTypeAsc(PolicyStatus.PUBLISHED)).thenReturn(v2);assertFalse(service.status(user).allRequiredAccepted());assertEquals(1,service.status(user).missing().size());}
 @Test void duplicateAcceptanceIsIdempotent(){List<PolicyVersion> current=allCurrent("1");PolicyVersion p=current.get(0);when(versions.findAllById(Set.of(p.getId()))).thenReturn(List.of(p));when(versions.findByStatusOrderByPolicyTypeAsc(PolicyStatus.PUBLISHED)).thenReturn(current);when(acceptances.existsByUserIdAndPolicyVersionId(user.getId(),p.getId())).thenReturn(true);when(acceptances.findPolicyVersionIdByUserId(user.getId())).thenReturn(Set.of(p.getId()));service.accept(user,Set.of(p.getId()),"req",null);verify(acceptances,never()).insertIfAbsent(any(),any(),any(),any(),any(),any());}
 @Test void snapshotReferencesExactFourCurrentVersions(){List<PolicyVersion> current=allCurrent("1");when(versions.findByStatusOrderByPolicyTypeAsc(PolicyStatus.PUBLISHED)).thenReturn(current);when(acceptances.findPolicyVersionIdByUserId(user.getId())).thenReturn(ids(current));service.snapshotForBooking(UUID.randomUUID(),user);verify(snapshots,times(4)).save(any());}
 private List<PolicyVersion> allCurrent(String version){return Arrays.stream(PolicyType.values()).map(t->published(t,version)).toList();}
 private PolicyVersion published(PolicyType t,String version){String content="DEVELOPMENT PLACEHOLDER — NOT LEGAL TERMS "+t+version;return PolicyVersion.builder().id(UUID.randomUUID()).policyType(t).version(version).title("Placeholder").effectiveAt(OffsetDateTime.now()).publishedAt(OffsetDateTime.now()).content(content).contentHash(PolicyAcceptanceService.sha256(content)).status(PolicyStatus.PUBLISHED).createdAt(OffsetDateTime.now()).build();}
 private Set<UUID> ids(List<PolicyVersion> list){Set<UUID>s=new HashSet<>();list.forEach(p->s.add(p.getId()));return s;}
}
