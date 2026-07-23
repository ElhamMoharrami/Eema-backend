package com.eema.backend.entity;

import com.eema.backend.entity.enums.VenueStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Size(min = 2, max = 120)
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String neighbourhood;

    @Positive
    @Column(name = "size_m2", nullable = false)
    private int sizeM2;

    @DecimalMin("0.00")
    @Column(name = "day_rate_eur", precision = 10, scale = 2, nullable = false)
    private BigDecimal dayRateEur;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "photo_paths", columnDefinition = "text[]")
    @Builder.Default
    private List<String> photoPaths = new ArrayList<>();

    private String clipUrl;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VenueStatus status = VenueStatus.DRAFT;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}