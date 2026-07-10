ALTER TYPE venue_status RENAME VALUE 'pending' TO 'PENDING';
ALTER TYPE venue_status RENAME VALUE 'active' TO 'ACTIVE';
ALTER TYPE venue_status RENAME VALUE 'suspended' TO 'SUSPENDED';

ALTER TYPE booking_status RENAME VALUE 'pending' TO 'PENDING';
ALTER TYPE booking_status RENAME VALUE 'paid' TO 'PAID';
ALTER TYPE booking_status RENAME VALUE 'confirmed' TO 'CONFIRMED';
ALTER TYPE booking_status RENAME VALUE 'cancelled' TO 'CANCELLED';
ALTER TYPE booking_status RENAME VALUE 'refunded' TO 'REFUNDED';

ALTER TYPE booking_event_type RENAME VALUE 'booking_created' TO 'BOOKING_CREATED';
ALTER TYPE booking_event_type RENAME VALUE 'payment_created' TO 'PAYMENT_CREATED';
ALTER TYPE booking_event_type RENAME VALUE 'payment_succeeded' TO 'PAYMENT_SUCCEEDED';
ALTER TYPE booking_event_type RENAME VALUE 'payment_failed' TO 'PAYMENT_FAILED';
ALTER TYPE booking_event_type RENAME VALUE 'payment_cancelled' TO 'PAYMENT_CANCELLED';
ALTER TYPE booking_event_type RENAME VALUE 'booking_confirmed' TO 'BOOKING_CONFIRMED';
ALTER TYPE booking_event_type RENAME VALUE 'cancellation_requested' TO 'CANCELLATION_REQUESTED';
ALTER TYPE booking_event_type RENAME VALUE 'refund_initiated' TO 'REFUND_INITIATED';
ALTER TYPE booking_event_type RENAME VALUE 'refund_completed' TO 'REFUND_COMPLETED';
ALTER TYPE booking_event_type RENAME VALUE 'webhook_received' TO 'WEBHOOK_RECEIVED';
ALTER TYPE booking_event_type RENAME VALUE 'email_sent' TO 'EMAIL_SENT';
ALTER TYPE booking_event_type RENAME VALUE 'owner_payout_released' TO 'OWNER_PAYOUT_RELEASED';
