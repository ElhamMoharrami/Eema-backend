package com.eema.backend.entity;

import com.eema.backend.entity.enums.VenueStatus;
import com.eema.backend.entity.enums.ProductionActivity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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

    @DecimalMin("0.01")
    @Column(name = "hourly_rate_eur", precision = 10, scale = 2, nullable = false)
    private BigDecimal hourlyRateEur;

    @Positive
    @Column(name = "minimum_booking_minutes", nullable = false)
    private Integer minimumBookingMinutes;

    @Min(60)
    @Max(43200)
    @Column(name = "minimum_lead_time_minutes")
    private Integer minimumLeadTimeMinutes;

    @DecimalMin("0.01")
    @Column(name = "full_day_rate_eur", precision = 10, scale = 2)
    private BigDecimal fullDayRateEur;

    @Positive
    @Column(name = "max_crew_size")
    private Integer maxCrewSize;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @org.hibernate.annotations.ColumnTransformer(write = "?::production_activity[]")
    @Column(name = "permitted_activities", columnDefinition = "production_activity[]")
    private List<ProductionActivity> permittedActivities;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @org.hibernate.annotations.ColumnTransformer(write = "?::production_activity[]")
    @Column(name = "prohibited_activities", columnDefinition = "production_activity[]")
    private List<ProductionActivity> prohibitedActivities;

    @Column(name = "natural_light")
    private Boolean naturalLight;

    @Column(name = "blackout_possible")
    private Boolean blackoutPossible;

    @DecimalMin("0.01")
    @Column(name = "ceiling_height_m", precision = 5, scale = 2)
    private BigDecimal ceilingHeightM;

    @Column(name = "loading_access")
    private Boolean loadingAccess;

    @Column(name = "parking_available")
    private Boolean parkingAvailable;

    @Column(name = "parking_spaces")
    private Integer parkingSpaces;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "lift_available")
    private Boolean liftAvailable;

    @Column(name = "wheelchair_accessible")
    private Boolean wheelchairAccessible;

    @Column(name = "bathrooms_available")
    private Boolean bathroomsAvailable;

    @Column(name = "sound_recording_suitable")
    private Boolean soundRecordingSuitable;

    @Column(name = "production_rules", columnDefinition = "text")
    private String productionRules;

    @Column(name = "power_details", columnDefinition = "text")
    private String powerDetails;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(
            name = "photo_paths",
            columnDefinition = "text[]"
    )
    @Builder.Default
    private List<String> photoPaths =
            new ArrayList<>();

    private String clipUrl;

    @Column(columnDefinition = "text")
    private String description;

    @Builder.Default
    @Column(nullable = false, length = 100)
    private String timezone = "Europe/Amsterdam";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
            nullable = false,
            columnDefinition = "venue_status"
    )
    @Builder.Default
    private VenueStatus status = VenueStatus.DRAFT;

    @Column(name = "submitted_for_review_at")
    private OffsetDateTime submittedForReviewAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "moderation_reason", length = 1000)
    private String moderationReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
