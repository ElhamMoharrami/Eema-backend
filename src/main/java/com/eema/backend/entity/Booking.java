package com.eema.backend.entity;

import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.entity.enums.RefundStatus;
import com.eema.backend.entity.enums.ActorType;
import com.eema.backend.entity.enums.CancellationPolicyTier;
import com.eema.backend.entity.enums.ProviderCostResponsibility;

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

    @Column(name = "hourly_rate_eur_snapshot", precision = 10, scale = 2, nullable = false)
    private BigDecimal hourlyRateEurSnapshot;

    @Column(name = "minimum_booking_minutes_snapshot", nullable = false)
    private Integer minimumBookingMinutesSnapshot;

    @Column(name = "full_day_rate_eur_snapshot", precision = 10, scale = 2)
    private BigDecimal fullDayRateEurSnapshot;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "billable_minutes", nullable = false)
    private Integer billableMinutes;

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

    @Builder.Default
    @Column(name = "mollie_fee_reconciled", nullable = false)
    private boolean mollieFeeReconciled = false;

    @Column(name = "mollie_fee_reconciled_at")
    private OffsetDateTime mollieFeeReconciledAt;

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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_creation_status", nullable = false, length = 20)
    private PaymentCreationStatus paymentCreationStatus = PaymentCreationStatus.NOT_STARTED;

    @Column(name = "payment_idempotency_key", unique = true)
    private UUID paymentIdempotencyKey;

    @Column(name = "payment_creation_started_at")
    private OffsetDateTime paymentCreationStartedAt;

    @Column(name = "mollie_checkout_url", length = 1000)
    private String mollieCheckoutUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 20)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "refund_idempotency_key", unique = true)
    private UUID refundIdempotencyKey;

    @Column(name = "mollie_refund_id", unique = true)
    private String mollieRefundId;

    @Column(name = "refund_amount_eur", precision = 10, scale = 2)
    private BigDecimal refundAmountEur;

    @Column(name = "refund_requested_at")
    private OffsetDateTime refundRequestedAt;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @Column(name = "cancellation_policy_version", length = 20)
    private String cancellationPolicyVersion;

    @Column(name = "grace_period_ends_at")
    private OffsetDateTime gracePeriodEndsAt;

    @Column(name = "full_refund_until")
    private OffsetDateTime fullRefundUntil;

    @Column(name = "partial_refund_until")
    private OffsetDateTime partialRefundUntil;

    @Column(name = "partial_refund_percentage", precision = 5, scale = 2)
    private BigDecimal partialRefundPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_policy_tier", length = 40)
    private CancellationPolicyTier cancellationPolicyTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by_actor_type", length = 20)
    private ActorType cancelledByActorType;

    @Column(name = "cancellation_refund_percentage", precision = 5, scale = 2)
    private BigDecimal cancellationRefundPercentage;

    @Column(name = "cancellation_refund_amount_eur", precision = 10, scale = 2)
    private BigDecimal cancellationRefundAmountEur;

    @Column(name = "cancellation_retained_amount_eur", precision = 10, scale = 2)
    private BigDecimal cancellationRetainedAmountEur;

    @Column(name = "owner_cancellation_settlement_eur", precision = 10, scale = 2)
    private BigDecimal ownerCancellationSettlementEur;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_cost_responsibility", length = 30)
    private ProviderCostResponsibility providerCostResponsibility;

    @Column(name = "cancellation_provider_cost_eur", precision = 10, scale = 2)
    private BigDecimal cancellationProviderCostEur;

    @Builder.Default
    @Column(name = "cancellation_provider_cost_reconciled", nullable = false)
    private boolean cancellationProviderCostReconciled = false;

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

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "owner_response_expires_at")
    private OffsetDateTime ownerResponseExpiresAt;

    @Column(name = "payment_expires_at")
    private OffsetDateTime paymentExpiresAt;

    @Column(name = "declined_at")
    private OffsetDateTime declinedAt;

    @Column(name = "expired_at")
    private OffsetDateTime expiredAt;

    @Column(name = "late_payment_detected_at")
    private OffsetDateTime latePaymentDetectedAt;

    @Column(name = "late_payment_notification_at")
    private OffsetDateTime latePaymentNotificationAt;
}
