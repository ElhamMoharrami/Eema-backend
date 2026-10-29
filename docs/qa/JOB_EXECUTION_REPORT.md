# JOB execution report

**JOB-001–006: 6 PASS / 0 FAIL / 0 BLOCKED.** Local logic verified; real-provider/deployed scheduling is not certified.

## Scenario results

| Scenario | Result and evidence |
|---|---|
| JOB-001 | PASS: actual booking job expires 100 requests and 100 approvals per scan, then remainder; deadline equality, busy-row recovery, competing workers, one audit/outbox/counter per committed expiry and rollback safety |
| JOB-002 | PASS: actual inquiry job uses injected time, processes 100 then remainder, preserves future offer, skips/revisits held lock and does not duplicate expiry intent |
| JOB-003 | PASS: bounded readiness batches, later payout progresses past 100 persistent holds, cursor advances only after commit, no transfer/duplicate audit; existing PAYOUT exact-end/locking/concurrency regression passes |
| JOB-004 | PASS: 100 tokens per table then concurrent remainder; strict expiry cleanup, exact-expiry/future retention, early cleanup of used tokens and unchanged refresh sessions |
| JOB-005 | PASS: bounded fee batches with timestamp ties, progress past 100 unavailable older fees, wrap/retry, unpaid exclusion, concurrent lookup with one financial outcome; existing reporting/error/disabled/exact-money tests |
| JOB-006 | PASS: concurrent full counts beyond 100 for payment/refund/payout/liability gauges, no booking/payout mutation; prior OPS complete fee count beyond 200 and ADMIN strict-boundary evidence reused |

## Source audit, corrected assumptions and dependencies

The pre-implementation inventory, source audit and intermediate runs are preserved in `evidence/job/history/audit-and-execution.md`; original generated wording is retained in `master-job-before-validation.md`. Sources include all six scheduled components, their transactional service methods and repository queries, common migrations through V14, existing tests and frontend booking reload behavior. Prior AUTH, BK, INQ, PAY, REF, PAYOUT, ADMIN, EDGE and OPS reports remain the authorities for business policy.

These are internal scheduler entrypoints, not user endpoints. Generated role-denial/email-verification/policy matrices were removed. Metrics are full read-only counts, not bounded mutation batches. Fee eligibility includes REFUNDED under REF's correction. Cleanup deliberately uses expires_at < now; token redemption remains invalid at equality. No new UI, email, payout transfer, payment rule or provider feature was introduced.

## Authoritative execution contract

Booking scans use one injected time, 100 REQUESTED and 100 AWAITING_PAYMENT candidates, with equality included. Requests expire in a transactional SKIP LOCKED batch. Approval candidate locks are short-lived; each mutation independently locks and rechecks the current booking. Concurrent workers may select overlapping approval candidates, but committed mutation, audit and outbox remain singular. No false claim of one shared approval-batch transaction is made.

Inquiry expiry holds its 100-row SKIP LOCKED selection and transitions in one service transaction. Both inquiry expiry and token cleanup now use the application's injected Clock. The default production Clock remains system UTC. Token cleanup deletes used or strictly older verification/reset tokens, at most 100 of each; it never extends token validity or deletes refresh sessions. Existing OPS auth handling safely rejects missing/expired/used credentials at dispatch.

Payout readiness retains the transactional booking-first lock order and all existing financial/hold/destination rules. A keyset cursor walks eligible_at/booking UUID through batches of at most 100 and advances only AFTER_COMMIT. An empty tail wraps to the oldest due work. Persistently HELD rows no longer monopolize every invocation. PAID/READY/CANCELLED are not selected and no transfer occurs.

Fee reporting walks `(coalesce(paid_at, created_at), booking UUID)` through at most 100 read-only provider lookups per invocation. Unavailable/error rows advance the scan just like successful rows; the next cycle revisits deferred work. Lookup runs outside database transactions. Independent instances may repeat a lookup, but locked reconciliation persists one authoritative amount/audit/ledger effect and rejects conflicting amounts.

Both cursors are per-instance scan optimizations, not durable financial claims. Restart begins from older rows safely; fairness requires continued scans through a cycle. No data is discarded, deadline extended, provider amount invented or new retry SLA introduced. Existing delay defaults remain 60 seconds (booking/inquiry), 300 seconds (readiness/metrics), 3600 seconds (token cleanup/fees), with the existing 60-second fee initial delay. No JOB migration was added; common V1–V14 remain unchanged.

Workflow gauges use existing complete aggregate queries and shared strict cutoff rules. They do not remediate data. Lifecycle counters run AFTER_COMMIT and are operational process telemetry, not a durable accounting ledger. Metrics expose no user/email/aggregate labels.

## Defects and regression history

- JOB-DEF-001, P2: inquiry and token jobs bypassed the shared Clock. Fixed without changing comparison semantics or default production time.
- JOB-DEF-002, P1: 100 unavailable old fees permanently hid a later available fee. Reproduced across three actual scans; fixed with bounded keyset traversal/wrap.
- JOB-DEF-003, P1: 100 persistently HELD payouts hid a later eligible payout. Reproduced across three scans; fixed with transactional bounded keyset traversal/wrap.
- JOB-DEF-004, P1 introduced during implementation: native timestamp projection expected OffsetDateTime while Hibernate returned Instant. Corrected projection and explicit UTC conversion; affected batches had rolled back. All focused JOB/PAYOUT tests subsequently passed.
- No unresolved local P0/P1 defect or product/business/legal ambiguity remains in this domain.

Historical failing summaries remain under `evidence/job/history/`. The first run had five passes and three reproduced failures. The payout reproduction had two passes and one failure. The intermediate projection run had four errors; none of those runs is counted as final acceptance.

## Validation

| Gate | Result |
|---|---|
| Focused JOB + PAYOUT PostgreSQL | PASS: 14 + 24 = 38 tests, no failures/errors/skips |
| `mvn test` | PASS: 1,078 tests, no failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS: 1,023 reported, 1,010 executed, 13 opt-in browser skips; no failures/errors |
| `mvn clean package` | PASS: 1,078 unit tests and packaged application |
| Backend `git diff --check` | PASS |
| Frontend | Unchanged in JOB; preceding OPS typecheck/lint/build/diff PASS reused |

The full run includes the packaged OPS recovery rehearsal. The previously characterized cached critical-path scheduler/test-container warning is not a new product failure; see `evidence/ops/history/full-suite-harness-note.json`. Acceptance uses test assertions and successful command exits.

Fourteen new real PostgreSQL tests use fixed/injected time, independent connections, barriers/latches and persisted assertions. They cover 101-row batches, timestamp ties, backlogs, replay, concurrent workers, held locks, rollback/cursor/counters and no provider money movement. Existing 24-case PAYOUT integration and unit/reporting/domain suites were reused. No arbitrary sleep decides a race outcome.

Frontend source is unchanged in JOB. Existing booking reload/refetch and BK/INQ/PAYOUT/ADMIN browser results are reused; no new browser journey is claimed. The preceding OPS frontend typecheck/lint/build/diff checks passed against the unchanged frontend state. Opt-in browser skips in the full backend profile remain explicitly reported.

Reports, sanitized evidence, master-plan scenarios and the regenerated/inspected PDF are updated. Historical OPS crash-loss evidence and the now-resolved OPS outbox decision remain intact. No commit, deployment or live-provider call was performed.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review whether expired booking/offer wording and payout hold/readiness labels are clear after reloading the existing pages. Manual database, race, token or financial-arithmetic testing is not requested.

## PRODUCT / OPERATIONS DECISIONS

None newly required for JOB. Scan frequencies and batch sizes retain their existing defaults. No operational throughput/alerting SLA or new remediation action was invented.

## STAGING ONLY

Real Mollie reporting availability and fees; deployed multi-instance scheduling and monitoring; production backlog sizes/query performance and alerting. Local PostgreSQL concurrency proves application logic, not deployed operating conditions.

**Locally clear to proceed: YES. NEXT UNTESTED DOMAIN: RACE.** Confirm its exact unfinished IDs and dependencies before beginning it. Automatic continuation remains authorized only while the completed domain is fully green with no unresolved P0/P1 or applicable product/business/legal decision.
