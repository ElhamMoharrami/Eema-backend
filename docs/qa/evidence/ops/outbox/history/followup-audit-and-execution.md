# OPS execution report

> Current follow-up: OPS-DEC-001 has been explicitly approved. Durable-dispatch implementation and final regression are in progress. Earlier final results below remain the historical pre-outbox record; the appended approved-policy audit and follow-up evidence supersede its open-decision wording.

**Final local outcome, 2026-09-20: OPS-001–009 — 8 PASS / 0 FAIL / 1 BLOCKED.** OPS-DEF-001 is resolved. OPS-008's technical checks pass, but OPS-DEC-001 (G08 notification delivery launch acceptance) is open. No further domain was started. EDGE remains6 PASS with all approved decisions resolved.

Initial audit 2026-09-20, before OPS test or production changes. EDGE is complete:6 PASS, required units1,040/PostgreSQL984 reported971 executed/clean package1,040 and frontend gates green. OPS-001–009 are NOT RUN with no earlier dedicated execution report. The complete master and prior reports have been read; this audit reconciles their current source contracts.

| Scenario | Source contract / dependencies | Missing focused evidence |
|---|---|---|
| OPS-001 | Common V1–V13, Hibernate validate; prior entire PostgreSQL suite migrates fresh | Packaged application common-only empty startup, exact migration list, empty business tables, idempotent restart |
| OPS-002 | Local common+dev V100–V103; prod common only and ProductionConfigurationValidator | Disposable local seed chain, production exclusion and existing config rejection matrix |
| OPS-003 | Local half-open booking exclusion, financial/destination/document/policy uniqueness and actual FK actions | Reuse fresh critical-path/domain races; smallest missing referenced-parent FK rollback assertions |
| OPS-004 | DB plus public/private storage and encryption/fingerprint keys are the recovery unit | Isolated pg_dump/restore, same release startup, snapshots/IDs/constraints, actual document/media bytes and key recovery |
| OPS-005 | Persistent rows/session hashes/claims and immutable objects survive restart; notifications are process-local | Actual backend process restart, same records/documents and representative role deep-link/reload |
| OPS-006 | Local reset is destructive; guarded script targets shared eema-backend project and must not be run against user data | Disposable project/DB/storage reset and reseed; targeted FK-safe reset; common chain has no policy content |
| OPS-007 | Earlier G04 mismatch/cap is stale: repository count includes positive paid CONFIRMED/CANCELLED/REFUNDED; ADMIN approved complete queue | Same fixed-clock threshold, metric/queue identity over200 exceptions; reuse ADMIN321 backlog proof |
| OPS-008 | AFTER_COMMIT, contained recipient failures, stale/missing entity suppression; no durable outbox/replay guarantee | Real transaction rollback/commit, executor binding and saturation, deterministic process-local delivery loss/restart characterization |
| OPS-009 | Fee reporting follows same-origin pages and rejects loops/invalid currency; disabled job skips provider, per-booking failure deferred | Focused HTTP transport matrix and enabled/disabled job continuation; reuse PAYOUT fee accounting |

Correct generated assumptions: migrations are V1–V13 and V100–V103, not V1–V11/V100–V102. No generic owner/scout authorization matrix applies to Flyway or database maintenance. Common migrations contain no legal policy text. Approved ADMIN backlog pagination replaced the200-per-category cap; fee metric predicates already match the paid-history contract. A transaction event is not proof of real email delivery. Reset will use only disposable QA resources, never the shared local Compose project's existing volumes.

Potential operational defect to reproduce: AsyncConfiguration declares applicationTaskExecutor/notificationExecutor, while listeners use unqualified @Async and Spring also sees taskScheduler. Actual regression logs warn that no unique executor or taskExecutor alias exists, then use SimpleAsyncTaskExecutor threads. This may bypass the already-configured pool/queue/backpressure contract. Do not introduce an outbox, reminder workflow or new delivery SLA to fix an executor binding issue.

Strategy: fixed Clock and real PostgreSQL for persistence/metrics/transaction boundaries; existing proven race suites reused through EDGE's fresh complete regression; isolated packaged-JVM/database/object copies for restore/restart/reset; focused mock reporting and notification failures; representative browser only for restart/reload/role/document recovery. Providers remain disabled or mocked. Store only sanitized counts/hashes/results, never credentials, tokens, full bank details or raw dumps.

## WHAT ELHAM SHOULD TEST PERSONALLY

Recovery/runbook clarity, operator understanding of pending reconciliation, and whether restored booking/document screens are understandable. No manual database race or financial calculation required.

## PRODUCT / OPERATIONS DECISIONS

No new decision at the initial audit. Existing email behavior is explicitly best effort without durable replay; technical characterization does not approve a delivery SLA. Any concrete new ambiguity will be recorded before continuation.

## STAGING ONLY

Production backup scheduling/retention and secret escrow, S3 durability/versioning, disaster recovery/RPO/RTO, real Mollie reporting access and fees, real email delivery, multi-instance workers and deployed monitoring. Local restore proves only the isolated tested recovery procedure.

## OPS-DEF-001 — reproduced P1 executor binding defect

A Spring proxy test imports the actual BookingLifecycleEventListener and AsyncConfiguration alongside the application's other TaskExecutor (taskScheduler). The production listener executes on `SimpleAsyncTaskExecutor-1`, not the configured `eema-async-*` pool. One test failed as expected before a fix; evidence is `history/executor-before.json`. Existing direct construction tests proved pool settings but did not prove listener binding. This bypasses the intended bounded2/4-thread,100-queue CallerRuns backpressure contract. Fix the existing executor binding using Spring's standard taskExecutor alias; no new delivery architecture or guarantee is involved.

## Additional local evidence (execution in progress)

- The actual proxy regression now binds to the configured `eema-async-*` executor. Saturation uses the existing bounded queue and CallerRuns backpressure; three submitted tasks are retained. Production change is the standard `taskExecutor` bean alias only.
- Five focused PostgreSQL assertions passed: fee gauge/complete queue agreement across205 paid-history rows, strict cutoff minus/equal/plus one microsecond, and referenced-parent deletion rollback. Existing critical-path financial/inventory constraints and races remain authorities.
- Disposable Compose reset passed with exact common1–13/dev100–103 migration history, successful seed login before/after, three actual project-owned volumes removed/recreated, marker absence and10 seeded venues. Public/private volume marker deletion and packaged-app database reseeding are separate assertions; the reset app uses temporary local directories, not those named file volumes. Shared local volumes were never targeted.
- Four browser restart assertions passed: existing scout session/deep link, identical authenticated receipt bytes, owner booking view, admin booking operations/timeline. Screenshots contain fictional QA fixtures only.
- The packaged process was deterministically killed while its after-commit listener was paused at a debugger breakpoint. The booking/event committed before death. Restart retained the booking and processed a new notification without replaying the pending old one. This proves a real process-local delivery gap, not a provider delivery result.
- First recovery attempt hit a readiness timeout (91-second startup versus90-second harness limit). The app started normally; the harness now allows240 seconds while polling readiness. Historical failure is retained separately.

The existing master-plan **G08 / OPS-008** separates technical characterization from launch acceptance. No durable replay or retry feature is authorized by the EDGE policies. Complete the remaining local tests and regression gates before final classification; do not silently treat this delivery limitation as an approved SLA.

## OPS-DEC-001 — notification delivery launch acceptance (OPEN)

The explicit G08 / OPS-008 launch gap is now reproduced against the packaged application, not inferred from source. A booking acceptance and its audit event commit successfully. The asynchronous listener is held before sending; the process is forcibly terminated. After restart the booking remains AWAITING_PAYMENT and a new notification processes, but the held notification is not replayed. There is no persisted delivery ledger or complete missing-email queue. The existing bounded executor prevents normal saturation from silently bypassing backpressure after OPS-DEF-001, but it cannot survive process death.

The outstanding decision is whether Eema v1 explicitly accepts best-effort lifecycle email, including possible unreplayed loss after crash and case-by-case operator follow-up, or requires durable delivery as a launch prerequisite. This is not permission to implement an outbox, replay worker, reminder feature or new communication SLA. The user-approved EDGE decisions do not decide this question. `VALIDATION_AGENT_INSTRUCTIONS.md` requires ambiguous product/operations decisions to be reported and permits automatic continuation only when none remains. OPS-008 is therefore BLOCKED on this decision even when its local technical assertions pass. JOB must not begin automatically.

## Rehearsal completion

The final focused recovery run passed all packaged-app steps and four browser checks. Snapshot comparisons cover every public table; tests also compare immutable receipt/media hashes, restored constraints, authorized receipt access, bank-data decryptability with recovered keys and failure with wrong keys, one-time restored refresh rotation and unchanged source DB. Common-only startup/restart and local seed/reset/reseed passed. PostgreSQL's equivalent cast/AND serialization required a narrow harness normalization; no schema or production change was made. The mistaken inquiry detail assertion was corrected to the existing `/availability-inquiries/mine` contract before the successful run.

Focused units now total44 PASS, including15 production-configuration cases,13 reporting transport/job cases,12 notification service cases,2 actual executor/proxy cases and the existing executor/job checks. Required complete regression subsequently passed; final totals are recorded below.

## Final scenario results

| Scenario | Result | Executed evidence |
|---|---|---|
| OPS-001 | PASS | Packaged common1–13 migration/Hibernate startup, empty business tables and unchanged restart hashes |
| OPS-002 | PASS | Packaged local1–13/100–103, seed login, prod common-only configuration audit and15 configuration unit cases |
| OPS-003 | PASS | Fresh complete PostgreSQL constraints/race regression plus referenced-parent rollback and restrict-FK checks |
| OPS-004 | PASS | Isolated dump/restore, all table hashes/constraints, actual files, key recovery/decryption, authorized document/inquiry access, refresh rotation, source isolation |
| OPS-005 | PASS | Actual packaged restart, persisted rows/payment claims and immutable files; four role/session/receipt browser checks |
| OPS-006 | PASS | Actual uniquely owned Compose volume reset, markers absent,10 venue seeds/login restored; packaged local DB/directory reset |
| OPS-007 | PASS |205-row paid-history gauge/complete-queue agreement and strict fixed-clock cutoff boundaries |
| OPS-008 | BLOCKED | All technical failure/rollback/saturation/crash assertions pass; explicit G08 launch acceptance is still missing (OPS-DEC-001) |
| OPS-009 | PASS |13 HTTP/job cases plus existing job/accounting regressions; no invented fee or live reporting claim |

## Changes, defects and reused coverage

The sole OPS production change adds Spring's standard `taskExecutor` alias to the existing notification executor. Actual unqualified listeners previously fell back to `SimpleAsyncTaskExecutor`; after the fix they use the existing2/4-thread pool,100-item queue and CallerRuns policy. OPS-DEF-001 was independently reproduced as P1 and is resolved. No migration, new provider behavior, outbox, delivery status, notification guarantee or OPS frontend feature was introduced.

Added NotificationExecutorBindingTest, OperationsReportingTest, OperationsIntegrationTest, OperationsNotificationIntegrationTest, OperationsRecoveryIntegrationTest, the opt-in browser harness and the disposable reset script. Extended existing production-configuration and notification-service tests. The real transaction test proves no delivery before commit, no rolled-back delivery and persistent committed state after a listener failure. The process-crash test uses a deterministic debugger breakpoint, not a timing race. Async saturation uses latches and a FIFO completion barrier.

Reused AUTH session authority/rotation, POL acceptance references, BK booking lifecycle/inventory, PAY durable claims, REF/PAYOUT financial identity and races, DEST bank encryption/authorization, DOC immutable private documents, ADMIN full-backlog pagination/thresholds and EDGE temporal/deadline/matching contracts. The complete PostgreSQL run executes the existing integration authorities afresh; no duplicate scenario expectations were introduced.

Historical evidence remains under `evidence/ops/history`: the executor failure, original generated scenarios, readiness timeout and PostgreSQL equivalent-expression harness correction. The full run also records a pre-existing critical-path test-context cleanup limitation: scheduled tasks can log connection refusal after their class-scoped container has stopped. This does not target the active recovery database or invalidate its assertions; see history/full-suite-harness-note.json. Those harness failures/warnings are not product defects. No unresolved implementation defect was found after the executor fix; OPS-DEC-001 remains an explicit operations/product decision.

## Final validation

- Focused units:44 PASS. Focused real PostgreSQL:6 PASS plus1 packaged recovery rehearsal PASS. Browser:4 PASS, screenshots inspected. Separate actual Compose reset:PASS, owned resources removed.
- `mvn test`: **1067 tests,0 failures,0 errors,0 skipped**.
- `mvn verify -Ppostgres-integration`: **991 reported,978 executed,13 opt-in browser skips,0 failures,0 errors**. Recovery and transaction tests are part of the ordinary complete profile; the browser was separately executed successfully.
- `mvn clean package`: **1067 tests,0 failures,0 errors,0 skipped**, packaged JAR built.
- Frontend: `npm run typecheck`, `npm run lint`, `npm run build` all PASS. No OPS frontend production change; the existing EDGE UI remains included.
- `git diff --check`:PASS in both repositories. Sanitized evidence contains counts, hashes and fictional fixtures; no access/refresh tokens, API keys, passwords, raw dumps or full bank details. Current PDF verification is recorded in `evidence/ops/document-check.json`.

Documentation includes this report, corrected master-plan OPS scenarios/G04/G08, historical evidence, local recovery runbook and regenerated complete master-plan PDF. Local providers were disabled or mocked throughout; no live payment, refund, payout or email was sent.

## WHAT ELHAM SHOULD TEST PERSONALLY — final

Review whether restored booking/receipt screens and the recovery runbook are understandable, and whether the explanation of pending reconciliation and uncertain email delivery gives operators/users an accurate sense of the workflow. No manual race, database, cryptography or arithmetic testing is needed.

## PRODUCT / OPERATIONS DECISIONS — final

**OPS-DEC-001 remains OPEN:** explicitly accept best-effort lifecycle email for v1 (possible unreplayed loss after crash and no complete missing-email inventory), or make durable delivery a launch requirement with separately approved scope. Testing cannot choose a delivery SLA or silently approve G08. No additional notification feature has been implemented.

## STAGING ONLY — final

Real Resend delivery and sender configuration; real Mollie reporting entitlement/data; S3 object durability/versioning; deployed backup schedules/retention and secret escrow; disaster recovery RPO/RTO; production monitoring and multi-instance workers. None is certified by local mocks, file copies or the disposable restore.

**Locally clear to proceed: NO — explicit notification launch acceptance is required.** All executable OPS technical checks are green, but the automatic-continuation condition is not met. The first unfinished domain remains OPS due OPS-008/OPS-DEC-001.

**NEXT UNTESTED DOMAIN: JOB-001–006**, scheduled expiry, payout readiness, recovery-token cleanup, fee reconciliation and workflow observability. Not started.

## Approved OPS-DEC-001 implementation audit — in progress, 2026-09-20

The user has approved durable dispatch through a PostgreSQL transactional outbox. The historical results and decision above describe the previous implementation; they are preserved, not current acceptance evidence. OPS-001–009 remain the active domain; OPS-008 cannot be marked PASS until implementation and regression finish. JOB-001–006 has not started.

Source audit: all existing lifecycle mail enters seven transactional event listeners. Booking request/decision/expiry, paid confirmation, late-payment refund initiation, cancellation/refund completion, inquiry creation/offer/decline/expiry, owner verification, venue moderation and payout-destination review currently send after commit. Account verification/reset likewise dispatch after commit but persist only token hashes. There are no existing generic refund-failure, payout-paid, incident or marketing emails to add. Existing renderers contain event-time amounts, deadlines, names and reasons; resolving them after a delayed dispatch could change historical meaning. Existing bounded executor aliases must remain.

Contract/design: ordinary messages retain only their immutable existing email artifact, aggregate reference, type and intended-recipient slot, not whole domain snapshots. Synchronous before-commit listeners compose and insert intents in the domain transaction; actual transport runs outside it. Uniqueness covers event/type/recipient. Trust transitions that legitimately repeat require separate event identities. Authentication uses a separately domain-separated keyed derivation from a nonsecret random nonce: the outbox retains only the nonce/hash reference and token-placeholder template. Raw credentials and full credential URLs exist only in memory; token tables remain hash-only. Pending auth messages are checked against authoritative expiry/use/account state before dispatch. Root-key rotation invalidates reconstruction and is operationally visible, never an expiry extension.

Missing coverage: same-transaction rollback/enqueue failure; all existing listener classes; stable historical content; auth credential absence and reconstructed-token validity/expiry; fresh and V13-upgrade PostgreSQL; unique logical recipients; bounded SKIP LOCKED claims, fencing, lease equality and recovery; provider failure/backoff/exhaustion/permanent rejection; accepted-provider/lost-completion retry; actual restart; aggregate metrics and safe Admin Operations visibility; bounded executor and packaged startup/scheduler. Tests must replace obsolete after-commit listener fault injection with provider/dispatcher failure while preserving historical evidence.

Local strategy: focused unit transport/token/backoff tests, real PostgreSQL transactions/claims/races and a packaged restart using a local provider stub. Existing domain suites retain business authorities. Required complete backend regression follows; minimal Admin Operations frontend contract changes require frontend gates. Real Resend acceptance/inbox delivery and deployed worker operations remain STAGING ONLY. Provider idempotency is a mitigation with a limited retention window, not an exactly-once guarantee.

### Durable notification matrix and implementation

| Existing source | Durable notifications | Logical identity |
|---|---|---|
| BookingLifecycleEventListener | REQUEST_SUBMITTED (scout/owner), REQUEST_ACCEPTED, REQUEST_DECLINED, REQUEST_EXPIRED, REQUEST_BECAME_UNAVAILABLE, APPROVAL_EXPIRED | Booking UUID + lifecycle type + recipient slot |
| BookingPaidEventListener | Payment received/confirmed (scout/owner) | Booking UUID + PAYMENT_RECEIVED + recipient slot |
| LatePaymentRefundEventListener | Existing expired-window late-payment refund initiation (scout); no initiation template exists for CANCELLED unpaid withdrawal | Booking UUID + LATE_PAYMENT_REFUND |
| BookingCancellationEventListener | Existing cancellation recipients and cancellation-refund completion | Booking UUID + distinct cancellation type + recipient slot |
| AvailabilityInquiryLifecycleEventListener | Existing CREATED owner email; OFFERED/DECLINED/OFFER_EXPIRED scout emails | Inquiry UUID + lifecycle type + recipient slot |
| TrustNotificationEventListener | Owner verification submitted/approved/rejected; venue submitted/approved/changes-requested/suspended/reactivated; payout destination submitted/verified/rejected | Persisted marketplace-trust audit UUID + recipient slot |
| AccountEmailEventListener | Verification and password-reset messages | Auth purpose + token hash + recipient slot; separate nonce/placeholder security representation |

No existing important email was classified as expendable. No generic refund-failure/refund-completion, payout-paid, incident, replacement-destination, reminder or marketing email was added. Normal accepted inquiries reuse the booking acceptance notification. Booking contact addresses can differ from account email; stable recipient slots preserve the existing intended recipients, even when two roles use the same address.

V14 adds a JDBC-owned outbox table, state/lease/auth-shape/uniqueness constraints and partial due/lease/failed indexes. Existing applied migrations are unchanged. Immutable email artifacts retain exactly the existing recipient/subject/text/HTML content needed for retry; they do not copy whole bookings, users, venues or bank records. Polymorphic aggregate references intentionally have no destructive cascade/FK so dispatch history survives domain cleanup. No attempt is made to reconstruct potentially already-sent pre-V14 messages.

The seven notification listeners now run synchronously BEFORE_COMMIT and enqueue using mandatory transaction propagation. JPA flush and JDBC insertion share the domain transaction. Rendering/enqueue failure aborts that transaction; actual provider failure can never roll it back because provider calls occur after claim commit and are guarded against active transactions. Event-time artifacts avoid the former stale-state skip or changed venue/amount/deadline problem after dispatch delay.

The dispatcher uses the existing bounded executor and one outstanding batch per application instance. Claim, send, completion are separate; SKIP LOCKED prevents simultaneous valid claims, random claim fences reject old completions, and expired leases recover at equality. Eight attempts,30-second doubling backoff capped at3600 seconds,300-second leases,20-second HTTP timeout and batch20 are configurable bounded defaults. Disabled dispatch/provider consumes no attempts. Exhausted/permanent failures remain FAILED, visible through the derived ADMIN queue; counters are aggregate status gauges without recipient/aggregate labels. No automatic terminal replay/admin action was introduced.

The native Java HTTP adapter replaces the old Resend2.1.0 SDK adapter because that installed SDK exposes neither idempotency options nor structured HTTP failure classification. Existing templates are unchanged. Resend's documented24-hour idempotency key window mitigates duplicate acceptance, but recovery beyond the window may duplicate a message. Provider success means accepted request, never inbox delivery. Official contract: https://resend.com/docs/dashboard/emails/idempotency-keys and https://resend.com/docs/api-reference/errors. The local loopback HTTP stub verifies headers, request stability and failure classification; real Resend remains unverified.

Authentication retains hash-only token tables. New token generation uses a cryptographically random32-byte nonce plus HMAC-SHA256 over a versioned, domain-separated input under the existing JWT root secret. Only the nonsecret nonce/hash reference and credential-placeholder template are persisted in the outbox. Dispatch reconstructs the original credential in memory, checks its hash, expiry/use/account/recipient and substitutes it only for transport. Old opaque tokens still redeem by hash. Superseded/used/inactive/changed-recipient messages cancel; expired or missing references fail safely; root-key mismatch is a visible terminal AUTH_DERIVATION_KEY_CHANGED. No new credential store, token lifetime extension or new recovery UI was added. Backup/key-rotation implications are explicit in the recovery runbook.

Focused execution so far:41 unit tests PASS and62 real PostgreSQL tests PASS across outbox/booking/registration, including a populated V13-to-V14 upgrade. The sole first-run failure was an invalid expiry fixture rejected by the existing token constraint; historical sanitized evidence is retained. Full regression and the updated packaged restart rehearsal remain pending; this is not yet an OPS completion claim.

### Follow-up execution progress

The packaged rehearsal passed16 checks (15 focused outbox cases plus the complete packaged recovery test). Forced JVM death after committed acceptance preserved the existing outbox row; restart initialized the scheduler and dispatched that exact row to a loopback HTTP provider without another domain transition. Backup/restore included the outbox and retained all table hashes, constraints, immutable files and recovered-key behavior. Fresh V14/common repeat startup and local development reset/reseed passed.

The first full PostgreSQL run exposed an obsolete trust fault-injection boundary: `OwnerTrustIntegrationTest` threw inside the former notification renderer and expected it to behave like provider delivery. Composition now intentionally participates in the domain transaction. The correction injects provider failure at `EmailService.deliver`, asserts no active transaction and verifies committed VERIFIED/audit state plus both retryable outbox rows. This was a stale test assumption, not an approved-policy violation. The owned run was stopped and its partial evidence retained; it is not counted as a complete regression pass.

After correction, `mvn test` passed1078 tests and the final focused PostgreSQL run passed49 tests (30 trust +19 outbox), including actual BEFORE_COMMIT insertion failure rollback, inactive/changed-recipient/used auth suppression, trust-audit replay identity and same-address recipient-slot separation. The full PostgreSQL/clean-package gates are running again. Frontend typecheck/lint/build/diff remain PASS; only the existing exception type/label changed.

### OPS-DEF-002 — financial event with no existing initiation email (P1, fixed; rerun pending)

The complete suite independently reproduced a new integration regression in `CancellationIntegrationTest.unexpectedPaymentAfterWithdrawalIsRefundedWithoutResurrectingBooking`: the outbox's mandatory-recipient guard rolled back a late-payment refund claim for an already CANCELLED unpaid withdrawal. `LatePaymentRefundInitiatedEvent` is also a financial-observability event; its existing email renderer is explicitly limited to EXPIRED bookings and describes an expired payment window. Treating every publication of this shared event as an email intent was incorrect.

The listener now explicitly preserves the existing CANCELLED branch's zero initiation emails, while leaving the financial event/metric and existing cancellation-refund completion email intact. No generic refund or withdrawn-booking initiation template was invented. Regression coverage requires completed refund, unchanged CANCELLED booking, zero owner entitlement, zero LATE_PAYMENT_REFUND outbox rows and one CANCELLATION_REFUND_COMPLETED row. The affected complete run was stopped and retained as historical failed evidence; focused and full gates must pass again before closure.

The OPS-DEF-002 correction passed all23 cancellation and19 outbox integration cases (42 total), with1078 unit tests green. The complete PostgreSQL and clean-package gates are running again against the corrected source.
