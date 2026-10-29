# Production operations

## Flyway profiles

- Base and `prod` scan `classpath:db/migration/common` only. They never execute development seed migrations.
- `local` scans both `common` and `dev`, so a fresh local database receives deterministic sample users and venues.
- Tests that start Spring without `local` use common schema migrations only and must create their own fixtures.
- Migration files retained their original contents, names, versions, and checksums; only their classpath locations changed.

An existing database that already recorded V8/V11 must not simply start with the production location and ignore validation.
Before the first deployment of this layout, inspect those rows and any seeded fixed IDs. If the environment was previously
used as production, remove fake data only after confirming it has no legitimate references, then run an explicit reviewed
Flyway repair using the common location so the two now-missing seed migrations are marked as deleted. Take a database backup
first. Fresh production databases require no repair. Do not enable a blanket `*:missing` validation override.

## Storage

`FileStorage` is the controller-facing abstraction. The current `StorageService` implementation uses a local directory.
Single-instance production must mount `UPLOAD_DIR` on durable persistent storage. Multi-instance deployment requires an
object-storage implementation of `FileStorage`; local disk must not be shared implicitly.

## Owner payouts

`ownerPayoutEur` is a calculated payable amount, not evidence that funds were transferred. Owner payouts are manual for
the MVP and remain `CALCULATED_NOT_PAID` until a separately controlled manual accounting process records payment.

## Mollie reconciliation

Payment and refund POST requests use durable server-generated Mollie idempotency keys. Mollie's replay window is one
hour; ambiguous operations older than that window require manual provider reconciliation rather than an unsafe retry.
Fee reconciliation requires an advanced/OAuth token with `balances.read`, is disabled by default, and is configured via
`MOLLIE_REPORTING_ACCESS_TOKEN` and `MOLLIE_FEE_RECONCILIATION_ENABLED=true`. Test-mode balance reporting is unavailable.

## Refunds

`POST /api/bookings/{id}/refund` is ADMIN-only and requests a full refund of the authoritative amount paid. Refund state is
tracked independently as `CREATING`, `QUEUED`, `PENDING`, `PROCESSING`, `REFUNDED`, `FAILED`, or `CANCELED`. The booking is
not marked `REFUNDED` until Mollie returns that final refund state through authoritative retrieval/webhook reconciliation.
