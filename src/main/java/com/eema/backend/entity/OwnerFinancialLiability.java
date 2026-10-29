package com.eema.backend.entity;

import com.eema.backend.entity.enums.OwnerFinancialLiabilityStatus;
import com.eema.backend.entity.enums.OwnerFinancialLiabilityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "owner_financial_liabilities", uniqueConstraints =
        @UniqueConstraint(name = "owner_liability_booking_type_uq", columnNames = {"booking_id", "type"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OwnerFinancialLiability {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "owner_id", nullable = false, updatable = false) private UUID ownerId;
    @Column(name = "booking_id", nullable = false, updatable = false) private UUID bookingId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private OwnerFinancialLiabilityType type;
    @Column(name = "amount_eur", nullable = false, precision = 10, scale = 2) private BigDecimal amountEur;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OwnerFinancialLiabilityStatus status;
    @Column(columnDefinition = "text") private String note;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "settled_at") private OffsetDateTime settledAt;
}
