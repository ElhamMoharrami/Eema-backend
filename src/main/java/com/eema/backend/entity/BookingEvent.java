package com.eema.backend.entity;

import com.eema.backend.entity.enums.ActorType;
import com.eema.backend.entity.enums.BookingEventType;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "booking_events",
        indexes = {
                @Index(
                        name = "idx_booking_events_booking_created",
                        columnList = "booking_id, created_at"
                ),
                @Index(
                        name = "idx_booking_events_type",
                        columnList = "event_type"
                ),
                @Index(
                        name = "idx_booking_events_actor",
                        columnList = "actor_type, actor_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BookingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "booking_id",
            nullable = false,
            updatable = false
    )
    private UUID bookingId;

    /*
     * Native PostgreSQL enum: booking_event_type
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
            name = "event_type",
            nullable = false,
            updatable = false,
            columnDefinition = "booking_event_type"
    )
    private BookingEventType eventType;

    /*
     * JSONB is appropriate here because event payload
     * structure varies between event types.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "payload",
            columnDefinition = "jsonb",
            updatable = false
    )
    private BookingEventPayload payload;

    @Column(
            name = "actor_id",
            updatable = false
    )
    private UUID actorId;

    @Column(
            name = "actor_name",
            nullable = false,
            updatable = false
    )
    private String actorName;

    /*
     * NOT a native PostgreSQL enum.
     * Database column is VARCHAR(20).
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "actor_type",
            nullable = false,
            updatable = false,
            length = 20
    )
    private ActorType actorType;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;
}