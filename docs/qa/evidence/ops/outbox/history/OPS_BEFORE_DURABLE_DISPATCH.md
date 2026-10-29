# OPS execution report

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
