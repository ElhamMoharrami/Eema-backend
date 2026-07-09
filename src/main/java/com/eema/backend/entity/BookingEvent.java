package com.eema.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;


@Entity
@Table(name = "booking_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    private BookingEventType eventType;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private String payload;

    private String createdBy;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
