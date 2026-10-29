package com.eema.backend.repository;
import com.eema.backend.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface IncidentReportRepository extends JpaRepository<IncidentReport,UUID>,JpaSpecificationExecutor<IncidentReport>{
 List<IncidentReport> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from IncidentReport i where i.id=:id") Optional<IncidentReport> findByIdForUpdate(@Param("id")UUID id);
}
