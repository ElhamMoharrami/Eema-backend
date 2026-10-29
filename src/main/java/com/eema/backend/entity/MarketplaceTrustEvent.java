package com.eema.backend.entity;

import com.eema.backend.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="marketplace_trust_events", indexes={
        @Index(name="idx_trust_event_subject", columnList="subject_type,subject_id,created_at")})
@Getter @Builder @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class MarketplaceTrustEvent {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Enumerated(EnumType.STRING) @Column(name="subject_type", nullable=false, length=30) private TrustSubjectType subjectType;
    @Column(name="subject_id", nullable=false) private UUID subjectId;
    @Enumerated(EnumType.STRING) @Column(name="event_type", nullable=false, length=50) private TrustEventType eventType;
    @Column(name="actor_id") private UUID actorId;
    @Enumerated(EnumType.STRING) @Column(name="actor_type", nullable=false, length=20) private ActorType actorType;
    @Column(length=1000) private String note;
    @CreationTimestamp @Column(name="created_at", nullable=false, updatable=false) private OffsetDateTime createdAt;
}
