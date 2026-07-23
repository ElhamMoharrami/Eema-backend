package com.eema.backend.entity;

import com.eema.backend.entity.enums.BookingEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    private BookingEventType eventType;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb", updatable = false)
    private BookingEventPayload payload;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_name", nullable = false, updatable = false)
    private String actorName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}