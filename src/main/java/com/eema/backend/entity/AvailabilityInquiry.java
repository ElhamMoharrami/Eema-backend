package com.eema.backend.entity;

import com.eema.backend.entity.enums.AvailabilityInquiryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "availability_inquiries")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AvailabilityInquiry {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(name = "scout_id", nullable = false)
    private UUID scoutId;

    @Column(name = "requested_date", nullable = false)
    private LocalDate requestedDate;

    @Column(name = "requested_start_time", nullable = false)
    private LocalTime requestedStartTime;

    @Column(name = "requested_end_time", nullable = false)
    private LocalTime requestedEndTime;

    @Column(columnDefinition = "text", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvailabilityInquiryStatus status;

    @Column(name = "offered_date")
    private LocalDate offeredDate;

    @Column(name = "offered_start_time")
    private LocalTime offeredStartTime;

    @Column(name = "offered_end_time")
    private LocalTime offeredEndTime;

    @Column(name = "offer_expires_at")
    private OffsetDateTime offerExpiresAt;

    @Column(name = "accepted_booking_id", unique = true)
    private UUID acceptedBookingId;

    @Column(name = "declined_at")
    private OffsetDateTime declinedAt;

    @Column(name = "offered_at")
    private OffsetDateTime offeredAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "expired_at")
    private OffsetDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
