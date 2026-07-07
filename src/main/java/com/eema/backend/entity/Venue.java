package com.eema.backend.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "venues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    @Size(min = 2, max = 120)
    private String name;

    @Column(nullable = false)
    private String neighbourhood;

    @Column(name = "size_m2", nullable = false)
    private int sizeM2;

    @Column(name = "day_rate_eur", nullable = false)
    private BigDecimal dayRateEur;

    @Type(value = ListArrayType.class)
    @Column(columnDefinition = "text[]")
    private List<String> tags = new ArrayList<>();

    @Type(value = ListArrayType.class)
    @Column(columnDefinition = "text[]")
    private List<String> photoPaths = new ArrayList<>();

    private String clipUrl;
    private String description;

    @Enumerated(EnumType.STRING)
    private VenueStatus status;

    @jakarta.persistence.CreationTimestamp
    private OffsetDateTime createdAt;

    @jakarta.persistence.UpdateTimestamp
    private OffsetDateTime updatedAt;
}