package com.eema.backend.entity;

import com.eema.backend.entity.enums.BookingStatus;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

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
    @Column(
            name = "status",
            nullable = false,
            columnDefinition = "booking_status"
    )
    private BookingStatus status;

    // --------------------------------------------------
    // BOOKING DETAILS
    // --------------------------------------------------

    @Column(name = "shoot_date", nullable = false)
    private LocalDate shootDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "raw_brief", columnDefinition = "text")
    private String rawBrief;

    // --------------------------------------------------
    // FINANCIAL SNAPSHOT
    // --------------------------------------------------

    @Column(
            name = "venue_price_eur",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal venuePriceEur;

    @Column(
            name = "commission_percentage",
            precision = 5,
            scale = 2,
            nullable = false
    )
    private BigDecimal commissionPercentage;

    @Column(
            name = "commission_eur",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal commissionEur;

    @Column(
            name = "mollie_fee_eur",
            precision = 10,
            scale = 2
    )
    private BigDecimal mollieFeeEur;

    @Column(
            name = "owner_payout_eur",
            precision = 10,
            scale = 2
    )
    private BigDecimal ownerPayoutEur;

    @Column(
            name = "total_paid_eur",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal totalPaidEur;

    // --------------------------------------------------
    // PAYMENT
    // --------------------------------------------------

    @Column(
            name = "mollie_payment_id",
            unique = true
    )
    private String molliePaymentId;

    // --------------------------------------------------
    // CONTACT SNAPSHOTS
    // --------------------------------------------------

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "name",
                    column = @Column(
                            name = "scout_contact_name",
                            nullable = false
                    )
            ),
            @AttributeOverride(
                    name = "email",
                    column = @Column(
                            name = "scout_contact_email",
                            nullable = false
                    )
            ),
            @AttributeOverride(
                    name = "phone",
                    column = @Column(
                            name = "scout_contact_phone"
                    )
            )
    })
    private BookingContactSnapshot scoutContact;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "name",
                    column = @Column(
                            name = "owner_contact_name",
                            nullable = false
                    )
            ),
            @AttributeOverride(
                    name = "email",
                    column = @Column(
                            name = "owner_contact_email",
                            nullable = false
                    )
            ),
            @AttributeOverride(
                    name = "phone",
                    column = @Column(
                            name = "owner_contact_phone"
                    )
            )
    })
    private BookingContactSnapshot ownerContact;

    // --------------------------------------------------
    // AUDIT
    // --------------------------------------------------

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
}