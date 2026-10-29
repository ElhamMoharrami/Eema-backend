package com.eema.backend.entity;

import com.eema.backend.entity.enums.OwnerPayoutHoldReason;
import com.eema.backend.entity.enums.OwnerPayoutStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "owner_payouts", uniqueConstraints = @UniqueConstraint(name = "owner_payouts_booking_uq", columnNames = "booking_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OwnerPayout {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;
    @Column(name = "venue_id", nullable = false, updatable = false)
    private UUID venueId;
    @Builder.Default @Column(nullable = false, length = 3)
    private String currency = "EUR";
    @Column(name = "gross_retained_amount_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossRetainedAmountEur;
    @Column(name = "commission_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionEur;
    @Column(name = "provider_cost_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal providerCostEur;
    @Column(name = "payout_amount_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutAmountEur;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OwnerPayoutStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "hold_reason", length = 40)
    private OwnerPayoutHoldReason holdReason;
    @Column(name = "eligible_at", nullable = false)
    private OffsetDateTime eligibleAt;
    @Column(name = "ready_at") private OffsetDateTime readyAt;
    @Column(name = "paid_at") private OffsetDateTime paidAt;
    @Column(name = "external_transfer_reference", length = 200)
    private String externalTransferReference;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_destination_id")
    private OwnerPayoutDestination payoutDestination;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Version private long version;
}
