# Eema backend architecture

Eema is a Spring Boot API backed by PostgreSQL. Owners may manage multiple venues. Each venue has its own timezone, weekly operating hours, dated availability overrides, ordered photos, an optional clip URL, and one pricing model: hourly rate, minimum booking minutes, and an optional full-day cap.

## Booking lifecycle

```text
REQUESTED -> AWAITING_PAYMENT -> CONFIRMED -> REFUNDED
     |              |
     |              +-> EXPIRED
     +-> DECLINED | EXPIRED | UNAVAILABLE | CANCELLED
```

`REQUESTED` is non-blocking and snapshots both the venue pricing inputs and final booking price. Accepting a request reserves inventory by moving it to `AWAITING_PAYMENT`; PostgreSQL's half-open exclusion constraint permits only one overlapping reservation. Other overlapping requests become `UNAVAILABLE`. Owner-response and payment deadlines are enforced by both service methods and scheduled expiration jobs.

Availability is weekly operating hours plus `AVAILABLE` overrides, minus `BLOCKED` overrides and bookings in `AWAITING_PAYMENT` or `CONFIRMED`.

## Pricing and payments

The booking price is `hourlyRateEur * max(durationMinutes, minimumBookingMinutes) / 60`, rounded to cents, capped by `fullDayRateEur` when present. The immutable `Booking.venuePriceEur` snapshot is used for checkout, commission, refund, and payout calculations; clients never submit an amount.

Checkout is `POST /api/bookings/{id}/checkout` for the requesting scout while the booking is `AWAITING_PAYMENT`. Mollie is authoritative: an on-time paid webhook moves the booking directly to `CONFIRMED`. A late payment leaves the booking `EXPIRED` and starts an idempotent full-refund lifecycle. Payment creation and refund provider states are stored separately from `BookingStatus`.

## Availability inquiries

Outside normal availability, scouts may open an inquiry. Owners decline it or offer an interval. The originating scout may accept a valid offer, creating an `AWAITING_PAYMENT` booking that follows the normal payment workflow.

## Database migrations

Common migrations contain schema only. The local profile additionally loads `db/migration/dev` seed data. This pre-launch schema baseline intentionally requires an empty database; developers migrating from the former development chain must run `docker compose down -v` before restarting.
