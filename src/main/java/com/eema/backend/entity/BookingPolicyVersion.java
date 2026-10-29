package com.eema.backend.entity;
import com.eema.backend.entity.enums.PolicyType;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Entity @Table(name="booking_policy_versions")
@Getter @Builder @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
@IdClass(BookingPolicyVersion.Key.class)
public class BookingPolicyVersion {
    @Id @Column(name="booking_id") private UUID bookingId;
    @Id @Enumerated(EnumType.STRING) @Column(name="policy_type",length=40) private PolicyType policyType;
    @Column(name="policy_version_id",nullable=false) private UUID policyVersionId;
    @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode public static class Key implements Serializable {
        private UUID bookingId; private PolicyType policyType;
    }
}
