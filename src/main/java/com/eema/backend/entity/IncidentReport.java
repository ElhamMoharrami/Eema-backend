package com.eema.backend.entity;
import com.eema.backend.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="incident_reports")
@Getter @Builder @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class IncidentReport {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="booking_id",nullable=false) private UUID bookingId;
    @Column(name="reported_by_user_id",nullable=false) private UUID reportedByUserId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private IncidentCategory category;
    @Column(nullable=false,length=4000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private IncidentStatus status;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @Column(name="resolved_at") private OffsetDateTime resolvedAt;
    @Column(name="admin_resolution_note",length=2000) private String adminResolutionNote;
    public void startReview(){if(status!=IncidentStatus.OPEN)throw new IllegalStateException("Only open incidents can enter review");status=IncidentStatus.UNDER_REVIEW;}
    public void resolve(String note,OffsetDateTime when){if(status!=IncidentStatus.UNDER_REVIEW)throw new IllegalStateException("Only incidents under review can be resolved");status=IncidentStatus.RESOLVED;adminResolutionNote=note;resolvedAt=when;}
    public void close(){if(status!=IncidentStatus.RESOLVED)throw new IllegalStateException("Only resolved incidents can be closed");status=IncidentStatus.CLOSED;}
}
