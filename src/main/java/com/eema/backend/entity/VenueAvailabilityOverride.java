package com.eema.backend.entity;

import com.eema.backend.entity.enums.AvailabilityOverrideType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "venue_availability_overrides")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VenueAvailabilityOverride {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;
    @Column(name = "override_date", nullable = false)
    private LocalDate date;
    @Column(name = "start_time")
    private LocalTime startTime;
    @Column(name = "end_time")
    private LocalTime endTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 10)
    private AvailabilityOverrideType type;
    @Column(name = "all_day", nullable = false)
    private boolean allDay;
}
