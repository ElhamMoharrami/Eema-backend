# JOB execution report

## Pre-implementation audit — 2026-09-20

OPS-001–009 completed 9 PASS, with 1,078 unit tests, 996 executed PostgreSQL tests (13 opt-in browser skips), clean package, frontend gates, updated PDF and diff checks. Automatic continuation is authorized. JOB-001–006 are NOT RUN; no prior dedicated JOB execution report exists. Complete master-plan and prior-report reading from this validation pass is retained.

| ID | Actual contract / existing evidence | Missing local evidence |
|---|---|---|
| JOB-001 | Request expiry is transactional, bounded 100 and SKIP LOCKED; approval selection currently occurs outside the per-booking mutation transaction. Both deadlines expire at equality. BK/PAY/EDGE prove business transitions and repeat safety. | Actual job 101-row batches, skipped locks and concurrent job execution, committed audit/outbox/counters; characterize approval selection lock lifetime |
| JOB-002 | Service transaction encloses 100 due OFFERED rows, SKIP LOCKED and expiry notifications. INQ proves state/deadline policy. Scheduler currently obtains wall time directly. | Actual job fixed-clock boundary, 101-row batches, concurrent workers and locked-row progress |
| JOB-003 | Transactional job claims at most 100 CALCULATED/HELD booking IDs due at equality, booking-first SKIP LOCKED. PAYOUT proves readiness rules, exact end, busy-row skipping and disjoint claims. No money transfer. | Batch/repeat/backlog behavior, terminal exclusion; reuse financial/race authority |
| JOB-004 | Transactional cleanup deletes at most 100 verification and 100 reset tokens; used OR expires_at strictly before now, not refresh tokens. Scheduler currently obtains wall time directly. AUTH proves token redemption. | 101-row boundaries, equality retention, concurrent cleanup and preservation of valid tokens/refresh sessions/auth outbox security |
| JOB-005 | Enabled job reads first 100 positive-paid unreconciled CONFIRMED/CANCELLED/REFUNDED bookings with provider ID; no claim or external-call transaction. Reporting lookup only; locked reconciliation is authoritative/idempotent. OPS/PAYOUT/REF prove provider response safety and allocation. | Actual repository batch/exclusion, overlapping workers, retry/deferred first-page progress; reuse provider transport and fee allocation tests |
| JOB-006 | Read-only full count queries, strict cutoff boundaries, aggregate gauges; lifecycle counters increment AFTER_COMMIT. OPS/ADMIN prove complete fee counts beyond 200 and shared thresholds. | Other gauge categories beyond 100, concurrent scans/no mutation and counter rollback/replay behavior |

## Corrected generated assumptions

These are internal scheduled entrypoints, not user endpoints. No scout/owner/admin denial matrix, verified-email prerequisite, policy acceptance or browser action belongs to the scheduler itself. Existing endpoint authorization/UI reports remain authoritative. Expiry emits only its existing lifecycle notifications through the approved OPS outbox; no provider call is needed in a job transaction. Readiness never transfers funds. Fee reconciliation also includes REFUNDED history under REF's correction. Token cleanup intentionally retains exact-expiry rows until a later pass while redemption rejects them at equality. Metrics scans are full counts, not 100-row mutation batches.

## Source and dependency audit

Read the six job classes, BookingService expiry/reconciliation methods, AvailabilityInquiryService expiry, repository selectors/locks, token cleanup JPQL, payout readiness query, workflow metric listeners, existing unit/job tests and PostgreSQL support. Reuse AUTH, BK, INQ, PAY, REF, PAYOUT, ADMIN, EDGE and OPS execution reports. Common V14 is current. No schema change is planned before evidence demonstrates a need.

## Strategy and boundaries

Use the existing disposable PostgreSQL/Testcontainers profile with synthetic fixtures, fixed/injected Clock, independent transactions and deterministic barriers/latches. Test actual scheduler entrypoints; disable uncontrolled scheduling in the harness. Seed batches efficiently through existing valid fixture shapes. Assert persisted state, audit/outbox uniqueness and no provider money movement. Add unit coverage only for deterministic dispatch/configuration behavior not already covered. Inspect existing frontend read/refetch contracts; do not add a browser journey for an internal job unless a user-visible defect requires one.

Potential concerns to reproduce, not yet classified defects: approval selector releases its SKIP LOCKED locks before per-booking mutation; older unresolved rows may repeatedly occupy bounded readiness/fee batches. No product policy or retry SLA is inferred. Record actual outcomes before choosing a fix or decision boundary.

## Status

Audit in progress. No JOB production changes yet. No JOB scenario is claimed PASS.

## First execution and corrections

Eight real PostgreSQL tests executed: five passed, three failed with no harness errors. History is retained in `evidence/job/history/initial-reproduction.log`. JOB-DEF-001 (P2 clock consistency/testability): inquiry expiry and auth cleanup bypass the application Clock; deterministic future-clock execution left all eligible rows unchanged. Both jobs now inject the existing Clock without changing expiry comparisons or schedules. The default production clock remains system UTC; this is not evidence of a default-production expiry outage. JOB-DEF-002 (P1 backlog progress): 100 unavailable old provider fees permanently hide the 101st available authoritative fee across repeated scans. A bounded keyset rotation is planned; it preserves 100 provider lookups per invocation, provider-outside-transaction execution and locked financial idempotency. No new payout/fee policy or external provider is involved.

Booking approval selection has a shorter lock lifetime than its mutation, but the actual concurrent/busy-row test passed with one committed expiry per booking. Its locked state recheck is the correctness authority; do not assert a shared batch transaction that does not exist.

Second reproduction: ordinary payout batches and concurrent read-only metric scans PASS. JOB-DEF-003 (P1 backlog progress): 100 older HELD payouts hid a later eligible CALCULATED payout across three actual readiness scans. History: `evidence/job/history/payout-reproduction.log`. The readiness worker now uses the same bounded keyset approach, retains booking-first SKIP LOCKED and advances its optimization cursor only AFTER_COMMIT. It does not change eligibility, hold reasons or transfer authority. Restart loses only scan position and safely revisits older rows. Multiple instances retain independent cursors and existing row locks; no durable money claim is replaced by memory.

## Final implementation contract under validation

Fee scan order is `(coalesce(paid_at, created_at), booking UUID)`, retaining paid chronology with deterministic ties and a safe fallback for historical malformed paid timestamps. Each invocation performs at most 100 provider lookups. A per-instance cursor advances after attempted rows, including unavailable/error results; an empty tail wraps to the oldest pending rows. The cursor is not a provider claim or persisted financial fact. Concurrent instances may perform the same read-only reporting lookup; locked `reconcileMollieFee` commits one result/audit and rejects conflicting later amounts. No provider call is made inside a database transaction. Existing disabled-job/no-fee/error continuation behavior remains applicable.

Readiness uses `(eligible_at, booking UUID)` keyset scans and holds booking locks through its batch transaction. At most 100 candidates are recalculated per invocation. Its per-instance cursor advances only after commit; rollback leaves it unchanged. Empty tail wraps. Restart merely revisits earlier candidates. Busy skipped records remain eligible for a later cycle. HELD remains HELD until its actual prerequisites resolve; PAID/READY/CANCELLED are excluded by the selector. Existing PAYOUT service state checks, destination snapshots, provider-cost policy and audit idempotency remain authoritative.

These scan changes prevent a fixed oldest page from starving later work while retaining existing configured invocation frequencies and batch bounds. No throughput/alerting SLA, persisted retry schedule, new ledger state or administrative action was introduced. Database migrations remain common V1–V14 unchanged in JOB.

Frontend audit: existing booking detail reload/mutation callbacks read Spring state through its `load` callback and `getBooking` API; no scheduler operation is exposed as a user action. Prior BK/INQ/PAYOUT/ADMIN browser/refetch evidence is reused. No JOB frontend changes or new browser claim.

JOB-DEF-004 (P1, introduced and fixed during implementation): PostgreSQL/Hibernate returns the native readiness timestamp projection as `Instant`; the initial `OffsetDateTime` projection failed conversion and rolled the batch back. The projection now uses `Instant`, converted explicitly to UTC for the next selector. This was an implementation defect found by the focused real PostgreSQL run, not a provider failure or a changed eligibility rule. Historical failed results are retained; no final PASS is inferred from this run.

Focused final regression: **38 PASS** — 14 ScheduledJobsIntegrationTest cases and all 24 existing PayoutIntegrationTest cases. No failures/errors/skips. Clock consistency, fee and payout backlog progress, cursor rollback, exact boundaries, bounded batches, replay, busy-row/concurrent-worker safety and complete read-only gauges are locally proven. Full regression remains required before closing JOB.
