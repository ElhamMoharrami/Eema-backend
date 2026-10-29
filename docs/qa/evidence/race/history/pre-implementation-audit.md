# RACE execution report

## Pre-implementation inventory and source audit — 2026-09-20

JOB completed 6 PASS with full gates and inspected PDF. Automatic continuation is authorized. RACE-001–008 have no dedicated report and retain generated NOT RUN labels. The complete master plan/prior reports were read during this continuing validation; all eight RACE scenarios were reread before this audit.

| Scenario | Current application authority | Existing executed evidence to verify |
|---|---|---|
| RACE-001 | Venue-first then booking lock, current-state/deadline recheck, exclusion constraint, loser UNAVAILABLE | VenueDomainIntegrationTest.avlConcurrentAcceptanceReservesOnceAndExpiryReopens (3 HTTP repetitions), BK/AVL |
| RACE-002 | Inquiry insert and ordinary acceptance share venue reservation lock; booking exclusion remains final DB guard | InquiryIntegrationTest.twoOffersAndNormalAcceptanceShareVenueReservationBoundary (3 repetitions × 2 variants), duplicate same-offer HTTP race; INQ/AVL |
| RACE-003 | Refresh token row lock, revoked check, one successor | AuthSessionIntegrationTest.concurrentHttpRefreshHasOneWinner, AUTH; SQL race separate |
| RACE-004 | Booking lock serializes ledger recalculation; unique booking payout; manual paid recording idempotency | PayoutIntegrationTest.concurrentRecalculationCreatesOneLedgerAndOneAudit plus paid-recording races, PAYOUT |
| RACE-005 | Owner routing lock serializes distinct destination verification; successive valid replacement is permitted | DestinationIntegrationTest.concurrentDistinctVerificationSerializesOwnerRouting (3), verify/reject race; DEST |
| RACE-006 | Publication transaction plus one-current partial unique constraint; loser rolls back safely | PolicyIntegrationTest.concurrentServicePublicationHasOneCurrentAndSafeConflict (3 real HTTP repetitions); POL |
| RACE-007 | Parent booking/payout lock plus document uniqueness; immutable stored bytes and rollback cleanup | DocumentIntegrationTest simultaneous receipt/statement requests (3 each), storage/commit failure cases; DOC |
| RACE-008 | Worker-specific transaction/claim contracts; no universal HTTP worker endpoint | Current JOB 14 cases, OPS 19 outbox cases, PAYOUT busy/disjoint workers, SQL disjoint batches |

## Corrected assumptions

The generated universal actor/provider matrix is not applicable. Refresh uses a credential rather than an authenticated/verified/policy-approved actor. Internal ledger recalculation and worker entrypoints have no invented mutation API. Distinct destination verifications may both return 200 sequentially, ending with one VERIFIED and one REPLACED destination; requiring one failed request would break the approved replacement contract. Receipt/statement duplicate downloads may both succeed with identical bytes. Repeated payout calculation may both succeed while creating one ledger/audit. Fees may have duplicate read-only provider lookups with a singular financial result. Approval worker initial selection does not share one enclosing transaction with every mutation.

## Strategy

Audit actual source, constraints, test barriers and persisted assertions. Map fresh completed full-suite evidence to the scenario contracts rather than count SQL-only races as HTTP proof. Add only genuinely missing coverage; do not rerun or rewrite all established domain suites unnecessarily. The unchanged-source full gates just completed for JOB are eligible regression evidence after mapping is verified. No new production or test change is assumed necessary. Real provider/deployed multi-instance behavior remains staging-only.

## Status

Audit in progress; no RACE PASS yet and no RACE production changes.
