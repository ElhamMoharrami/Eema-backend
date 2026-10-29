package com.eema.backend.repository;

import com.eema.backend.dto.response.OperationalExceptionDto;
import com.eema.backend.entity.enums.OperationalExceptionSeverity;
import com.eema.backend.entity.enums.OperationalExceptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

/** Complete derived operator backlog; only the requested page crosses the database boundary. */
@Repository
@RequiredArgsConstructor
public class OperationalExceptionRepository {
    private final NamedParameterJdbcTemplate jdbc;

    // One statement gives totals and content the same snapshot, including empty/out-of-range pages.
    // Source-row IDs distinguish separate liabilities; type distinguishes exceptions on the same booking.
    private static final String PAGE_SQL = """
            WITH exceptions AS (
                SELECT 'PAYOUT_HELD' AS type, 'INFO' AS severity, 2 AS severity_rank,
                       booking_id, id AS payout_id, NULL::uuid AS liability_id,
                       updated_at AS detected_at, 'PAYOUT_' || COALESCE(hold_reason, 'null') AS reason,
                       id AS entity_id
                FROM owner_payouts WHERE status = 'HELD'
                UNION ALL
                SELECT 'PAYOUT_READY_OVERDUE', 'WARNING', 1, booking_id, id, NULL::uuid,
                       ready_at, 'READY_PAYOUT_NOT_RECORDED_PAID', id
                FROM owner_payouts WHERE status = 'READY' AND ready_at < :payoutCutoff
                UNION ALL
                SELECT 'OWNER_LIABILITY_OPEN', 'WARNING', 1, booking_id, NULL::uuid, id,
                       created_at, 'OWNER_LIABILITY_REQUIRES_RESOLUTION', id
                FROM owner_financial_liabilities WHERE status = 'OPEN'
                UNION ALL
                SELECT 'REFUND_STUCK', 'CRITICAL', 0, id, NULL::uuid, NULL::uuid,
                       refund_requested_at, 'REFUND_EXCEEDED_STUCK_THRESHOLD', id
                FROM bookings WHERE refund_status IN ('CREATING','QUEUED','PENDING','PROCESSING')
                    AND refund_requested_at < :refundCutoff
                UNION ALL
                SELECT 'PAYMENT_CREATION_STUCK', 'WARNING', 1, id, NULL::uuid, NULL::uuid,
                       payment_creation_started_at, 'PAYMENT_CREATION_EXCEEDED_STUCK_THRESHOLD', id
                FROM bookings WHERE payment_creation_status = 'CREATING'
                    AND payment_creation_started_at < :paymentCutoff
                UNION ALL
                SELECT 'MOLLIE_FEE_UNRECONCILED', 'WARNING', 1, id, NULL::uuid, NULL::uuid,
                       paid_at, 'AUTHORITATIVE_PROVIDER_FEE_UNAVAILABLE', id
                FROM bookings WHERE mollie_fee_reconciled = false AND total_paid_eur > 0
                    AND paid_at < :feeCutoff AND status IN ('CONFIRMED','CANCELLED','REFUNDED')
                UNION ALL
                SELECT 'LATE_PAYMENT_REFUND_IN_PROGRESS', 'WARNING', 1, id, NULL::uuid, NULL::uuid,
                       late_payment_detected_at, 'LATE_PAYMENT_REQUIRES_REFUND_COMPLETION', id
                FROM bookings WHERE late_payment_detected_at IS NOT NULL
                    AND refund_status IN ('CREATING','QUEUED','PENDING','PROCESSING')
                UNION ALL
                SELECT 'NOTIFICATION_DELIVERY_FAILED', 'WARNING', 1,
                       CASE WHEN aggregate_type='BOOKING' THEN aggregate_id ELSE NULL::uuid END,
                       NULL::uuid, NULL::uuid, updated_at,
                       'Notification ' || id::text || ': ' || notification_type || ' / ' || last_failure_category, id
                FROM notification_outbox WHERE status='FAILED'
            ), requested_page AS (
                SELECT * FROM exceptions
                ORDER BY severity_rank, detected_at ASC NULLS LAST, type COLLATE "C", entity_id
                LIMIT :limit OFFSET :offset
            )
            SELECT requested_page.*, totals.total
            FROM (SELECT count(*) AS total FROM exceptions) totals
            LEFT JOIN requested_page ON true
            ORDER BY severity_rank, detected_at ASC NULLS LAST, type COLLATE "C", entity_id
            """;

    public Page<OperationalExceptionDto> findPage(Pageable page, OffsetDateTime payoutCutoff,
            OffsetDateTime refundCutoff, OffsetDateTime paymentCutoff, OffsetDateTime feeCutoff) {
        var params = new MapSqlParameterSource()
                .addValue("payoutCutoff", payoutCutoff).addValue("refundCutoff", refundCutoff)
                .addValue("paymentCutoff", paymentCutoff).addValue("feeCutoff", feeCutoff)
                .addValue("limit", page.getPageSize()).addValue("offset", page.getOffset());
        return jdbc.query(PAGE_SQL, params, rs -> {
            var content = new ArrayList<OperationalExceptionDto>();
            long total = 0;
            while (rs.next()) {
                total = rs.getLong("total");
                if (rs.getString("type") == null) continue; // Count-only row for an empty page.
                content.add(new OperationalExceptionDto(
                        OperationalExceptionType.valueOf(rs.getString("type")),
                        OperationalExceptionSeverity.valueOf(rs.getString("severity")),
                        rs.getObject("booking_id", UUID.class), rs.getObject("payout_id", UUID.class),
                        rs.getObject("liability_id", UUID.class), rs.getObject("detected_at", OffsetDateTime.class),
                        rs.getString("reason")));
            }
            return new PageImpl<>(content, page, total);
        });
    }
}
