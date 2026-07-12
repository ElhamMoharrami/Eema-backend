package com.eema.backend.entity;

import com.eema.backend.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(name = "scout_id", nullable = false)
    private UUID scoutId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "booking_status", nullable = false)
    private BookingStatus status;

    // ==========================
    // Booking details
    // ==========================

    @Column(name = "shoot_date", nullable = false)
    private LocalDate shootDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(columnDefinition = "text")
    private String rawBrief;

    // ==========================
    // Financial snapshot
    // ==========================

    // Price of the venue at booking time
    @Column(name = "venue_price_eur", precision = 10, scale = 2, nullable = false)
    private BigDecimal venuePriceEur;

    // e.g. 15.00
    @Column(name = "commission_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionPercentage;

    // e.g. €30.00
    @Column(name = "commission_eur", precision = 10, scale = 2, nullable = false)
    private BigDecimal commissionEur;

    // Fee charged by Mollie
    @Column(name = "mollie_fee_eur", precision = 10, scale = 2)
    private BigDecimal mollieFeeEur;

    // Amount transferred to venue owner
    @Column(name = "owner_payout_eur", precision = 10, scale = 2)
    private BigDecimal ownerPayoutEur;

    // Total charged to the scout
    @Column(name = "total_paid_eur", precision = 10, scale = 2)
    private BigDecimal totalPaidEur;

    // ==========================
    // Payment
    // ==========================

    @Column(name = "mollie_payment_id", unique = true)
    private String molliePaymentId;

    // ==========================
    // Contact snapshot
    // ==========================

    @Convert(converter = JsonbConverter.class)
    @Column(name = "scout_contact", columnDefinition = "jsonb")
    private String scoutContact;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "owner_contact", columnDefinition = "jsonb")
    private String ownerContact;

    // ==========================
    // Audit
    // ==========================

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private OffsetDateTime paidAt;

    private OffsetDateTime confirmedAt;

    private OffsetDateTime cancelledAt;
}