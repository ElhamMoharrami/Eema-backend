# OPS execution report — approved durable dispatch

**OPS-001–009: 9 PASS / 0 FAIL / 0 BLOCKED.** This result is local technical validation, not a real-provider or paid-launch certification.

## Approved decision and historical record

OPS-DEC-001 is resolved under the explicit user decision:

> Eema v1 requires durable dispatch of important lifecycle notifications through a PostgreSQL transactional outbox. Business transactions never depend on provider email success. Dispatch is at-least-once; actual inbox delivery is not guaranteed.

The previous result was 8 PASS / 0 FAIL / 1 BLOCKED. The actual packaged JVM lost its process-local notification after domain commit and forced death. That result, the open decision, executor defect and old crash evidence remain in `evidence/ops/outbox/history/OPS_BEFORE_DURABLE_DISPATCH.md`, `recovery-before-outbox.json`, and the original `evidence/ops/history` files. Durability was not present then. The pre-implementation audit and intermediate executions are preserved in the follow-up history archive.

## Scenario inventory and results

| Scenario | Result | Local evidence |
|---|---|---|
| OPS-001 | PASS | Packaged fresh common V1–V14, Hibernate validation, empty business tables and unchanged repeat startup; populated V13-to-V14 upgrade preserves history |
| OPS-002 | PASS | Common-only and development V1–V14/V100–V103 startup, seed login/reset; existing production-configuration guard regressions |
| OPS-003 | PASS | Complete PostgreSQL constraint/concurrency regression; new outbox uniqueness/state/auth/claim constraints and due/lease/failed indexes |
| OPS-004 | PASS | All-table backup/restore now includes outbox history; constraints, immutable document/media hashes, key recovery, authorized access and session rotation |
| OPS-005 | PASS | Ordinary restart preserves persisted state/files; forced process death retains notification intent and restart dispatches its existing row without a new transition |
| OPS-006 | PASS | V14 packaged development DB/directory reset and deterministic reseed; previous actual isolated Compose volume-reset evidence reused |
| OPS-007 | PASS | Existing complete fee/backlog metrics and cutoff tests; new aggregate outbox status gauges and safe derived failed-notification queue |
| OPS-008 | PASS | Transactional outbox, bounded executor, rollback/replay, provider failure, backoff/exhaustion, SKIP LOCKED, concurrent claims, fenced lease recovery, auth credential handling and packaged restart |
| OPS-009 | PASS | Existing reporting transport/pagination/unavailable-fee and job regression authorities; no invented provider costs or real-provider claim |

## Source audit, corrected assumptions and dependencies

The audit covered all existing email producers/listeners/renderers, recovery-token storage, executor/scheduler wiring, refund/cancellation event semantics, common/development migrations, Admin Operations SQL and the frontend exception label/type. Previous AUTH, BK, INQ, PAY, CANCEL, REF, PAYOUT, TRUST, VEN, DEST, DOC, SEC, ADMIN and EDGE reports retain their business authorities. This pass changes delivery reliability; it does not revise financial or booking policy.

All seven email listeners formerly dispatched only after commit from process memory. A bounded executor solved saturation but not crash loss. The new listeners synchronously compose and insert notification intent BEFORE_COMMIT in the same JPA/JDBC transaction. Enqueue/render failure aborts the transaction; an actual provider failure occurs outside it and cannot undo business state. Existing after-commit-only generated expectations and fault injections are superseded accordingly.

One financial event has a narrower email contract: LatePaymentRefundInitiatedEvent also counts unexpected money received after a CANCELLED unpaid withdrawal. That branch has no existing initiation template. Its metric and existing refund-completion email are preserved; the expired-window initiation template remains exclusive to EXPIRED bookings. No new email is inferred from the financial event's name.

## Durable notification matrix

| Existing source | Durable messages | Logical identity |
|---|---|---|
| BookingLifecycleEventListener | Request submitted (scout/owner), accepted, declined, expired, unavailable, approval expired | Booking UUID + type + recipient slot |
| BookingPaidEventListener | Existing paid confirmation to scout/owner | Booking UUID + PAYMENT_RECEIVED + slot |
| LatePaymentRefundEventListener | Existing EXPIRED booking's late-payment refund initiation | Booking UUID + LATE_PAYMENT_REFUND |
| BookingCancellationEventListener | Existing cancellation recipients and cancellation-refund completion | Booking UUID + distinct type + slot |
| AvailabilityInquiryLifecycleEventListener | CREATED owner message; OFFERED/DECLINED/OFFER_EXPIRED scout messages | Inquiry UUID + type + slot |
| TrustNotificationEventListener | Owner verification submitted/approved/rejected; venue submitted/approved/changes requested/suspended/reactivated; payout destination submitted/verified/rejected | Persisted trust-audit UUID + slot |
| AccountEmailEventListener | Existing verification and password-reset messages | Purpose + token hash + slot, with separate auth security representation |

No existing important email is classified as expendable. No generic refund-failure/admin-refund-completion, payout-paid, incident, replacement-destination, reminder, marketing or free-booking notification was added. Inquiry acceptance reuses booking acceptance. Recipient slots preserve distinct intended roles even when their email addresses match.

## Persistence, worker and provider contract

V14 creates the JDBC-owned `notification_outbox`, without modifying historical migrations. The artifact contains only the existing recipient/subject/text/HTML plus aggregate/type identity; it does not copy whole domain objects. Freezing the rendered artifact preserves event-time names, amounts, reasons, deadlines and provider request content across state/configuration/template changes. Polymorphic aggregate references intentionally do not cascade away delivery history. Pre-V14 events are not backfilled because their delivery cannot be reconstructed reliably.

A bounded worker claims one due row in a short transaction using SKIP LOCKED, commits, sends externally and completes in another short transaction. A random claim fence rejects stale completion. Equality at lease expiry permits another worker to recover. Defaults are batch size 20, one outstanding batch per instance, 10-second polling, 300-second leases, 20-second HTTP request timeout, 8 attempts and 30-second exponential backoff capped at 3600 seconds. Configuration has bounds. Disabled provider/dispatcher preserves pending rows without consuming attempts. Repeated worker deaths also exhaust into visible failure.

Successful provider acceptance becomes DELIVERED. Transient network/response/408/429/5xx and concurrent-idempotency conflicts retry; permanent rejection or exhaustion becomes FAILED. Terminal history is retained. Provider acceptance followed by lost local completion leaves a recoverable lease and may retry. The native Java HTTP adapter replaces the old SDK adapter to expose explicit timeout, HTTP classification and a stable `eema-notification-<UUID>` idempotency header. Resend documents a 24-hour key retention window; duplicates remain possible beyond that window. See [Resend idempotency](https://resend.com/docs/dashboard/emails/idempotency-keys) and [API errors](https://resend.com/docs/api-reference/errors). DELIVERED does not mean inbox delivery.

The existing bounded executor aliases/CallerRuns fix remains intact. No broker, microservice, unbounded application task queue, new notification preference or admin resend endpoint was introduced.

## Authentication security

Token tables remain hash-only. A new token uses a 32-byte random nonce plus HMAC-SHA256 over a versioned, domain-separated input under the existing JWT root secret. The outbox stores only the nonsecret nonce/hash/user reference and the existing credential-placeholder artifact. Raw credentials and full credential URLs exist only in memory for transport. Dispatch reconstructs the exact token, verifies its hash, then checks expiry/use, active account and unchanged recipient. Used/superseded/inactive/changed-recipient messages cancel; expired/missing references fail safely. Root-key mismatch becomes AUTH_DERIVATION_KEY_CHANGED. No expiry is extended; old opaque tokens still redeem by their stored hash.

The recovery runbook documents JWT-secret recovery/rotation, existing resend/reset behavior, uniform-version rollout and safe metadata-only diagnostics. No usable verification/reset credential, API key, access/refresh token, bank detail or provider response is stored in the generic payload or evidence.

## Operations and frontend

FAILED rows derive NOTIFICATION_DELIVERY_FAILED in the existing ADMIN-only queue. The row exposes outbox UUID/type/category and a booking link where applicable, without body or recipient data. The frontend change is limited to the existing enum and human-readable label. `eema.notifications.outbox{status=...}` contains aggregate counts with no user/email/aggregate identifiers as labels. Domain state remains authoritative regardless of notification status.

## Tests, defects and validation

Reused the previous OPS recovery/browser/configuration/reporting/constraint coverage and the cross-domain business suites. Added focused transport/token/backoff/bounded-submission tests and real PostgreSQL intent/rollback/replay/lease/race/security/upgrade checks; updated the actual packaged restart rehearsal to assert durable dispatch. Tests use independent transactions and barriers for races, injected Clock for retry/lease boundaries and local provider stubs. No sleep is used to decide race outcomes.

- OPS-DEF-001: prior executor-binding defect remains resolved.
- OPS-DEF-002: the new mandatory-recipient guard initially treated a non-email financial-event branch as an email intent, blocking an unpaid-withdrawal refund. Explicit routing now preserves its zero initiation emails and one existing completion email. The actual financial regression and full suite passed after correction.
- Harness corrections: an expiry fixture violated its existing creation/expiry constraint; old trust-renderer failure injection incorrectly represented provider failure after the transaction-boundary change. Historical failures and interrupted runs are retained separately.
- Unresolved local P0/P1 defects: none.

The pre-existing critical-path test-context cleanup warning remains separately characterized in `evidence/ops/history/full-suite-harness-note.json`: its cached scheduler can attempt its already-stopped class-scoped test database. This is not the active recovery database, and it is not evidence of a production data failure. Final classification uses executed assertions and command exit status.

| Gate | Result |
|---|---|
| `mvn test` | PASS: 1,078 tests, zero failures/errors/skips |
| Focused real PostgreSQL cancellation + outbox | PASS: 42 tests (23 + 19) after OPS-DEF-002 fix |
| Focused trust + outbox | PASS: 49 tests (30 + 19) |
| `mvn verify -Ppostgres-integration` | PASS: 1,009 reported, 996 executed, 13 opt-in browser skips; zero failures/errors |
| Packaged recovery rehearsal | PASS in focused and full PostgreSQL runs; local HTTP dispatch after forced JVM death |
| `mvn clean package` | PASS: 1,078 unit tests and packaged application |
| Backend `git diff --check` | PASS |
| Frontend typecheck / lint / build / diff check | All PASS |

The earlier four OPS browser role/session/receipt checks are reused; no new browser journey was required for the exception-label addition. Packaged process restart with the local HTTP provider was executed afresh. Real provider/deployment behavior is not inferred from these checks.

Evidence: `docs/qa/evidence/ops/outbox/`, original OPS evidence and history; [local recovery runbook](OPS_LOCAL_RECOVERY_RUNBOOK.md). The master plan and its PDF are updated without changing scenario IDs. Temporary packaged processes, disposable containers and synthetic recovery files are cleaned up after execution.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review whether “Notification delivery failed” and the retained failure explanation give operators a clear next step, and whether the recovery runbook clearly distinguishes committed booking state from email dispatch. No manual race, crypto, database or financial-arithmetic testing is requested.

## PRODUCT / OPERATIONS DECISIONS

OPS-DEC-001 is approved and resolved. No additional product/business/legal decision was required for this scope. Missing new notification types remain separate feature questions; they were not implemented.

## STAGING ONLY

Real Resend sender configuration, acceptance/deduplication and inbox behavior; deployed multi-instance/version rollout; production alerts and backup/secret-recovery operation; existing real Mollie reporting, S3 durability and disaster-recovery objectives. Local mocks and isolated recovery do not certify these.

**Locally clear to proceed: YES. NEXT UNTESTED DOMAIN: JOB-001–006**, scheduled booking/inquiry expiry, payout readiness, auth-token cleanup, fee reconciliation and workflow observability. Automatic continuation is authorized only while the domain remains green and introduces no unresolved P0/P1 or product/business/legal decision.
