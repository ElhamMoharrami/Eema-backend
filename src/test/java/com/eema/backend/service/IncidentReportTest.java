package com.eema.backend.service;
import com.eema.backend.entity.IncidentReport;
import com.eema.backend.entity.enums.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;
class IncidentReportTest {
 @Test void transitionsAreNarrowAndExplicit(){IncidentReport incident=IncidentReport.builder().category(IncidentCategory.OTHER).description("Operational report").status(IncidentStatus.OPEN).build();incident.startReview();assertEquals(IncidentStatus.UNDER_REVIEW,incident.getStatus());OffsetDateTime resolved=OffsetDateTime.now();incident.resolve("Handled separately",resolved);assertEquals(IncidentStatus.RESOLVED,incident.getStatus());assertEquals(resolved,incident.getResolvedAt());incident.close();assertEquals(IncidentStatus.CLOSED,incident.getStatus());}
 @Test void cannotResolveBeforeReview(){IncidentReport incident=IncidentReport.builder().category(IncidentCategory.ACCESS_PROBLEM).description("Could not enter").status(IncidentStatus.OPEN).build();assertThrows(IllegalStateException.class,()->incident.resolve("Reviewed",OffsetDateTime.now()));}
}
