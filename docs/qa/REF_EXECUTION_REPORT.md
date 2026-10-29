# REF execution report

Scope: REF-001–REF-004 and the shared refund/mark-paid race. Existing CANCEL and PAY suites are referenced, not re-created. Local mocks establish Eema behavior, not live Mollie behavior.

## Initial source audit (before REF changes)

RefundStatus is exactly `NONE, CREATING, QUEUED, PENDING, PROCESSING, REFUNDED, FAILED, CANCELED`.

| Current state | Accepted provider successors |
| --- | --- |
| NONE | None: only a local claim can enter CREATING |
| CREATING | QUEUED, PENDING, PROCESSING, REFUNDED, FAILED, CANCELED |
| QUEUED | PENDING, PROCESSING, REFUNDED, FAILED, CANCELED |
| PENDING | PROCESSING, REFUNDED, FAILED, CANCELED |
| PROCESSING | REFUNDED, FAILED |
| REFUNDED / FAILED / CANCELED | None through provider status processing |

Equal/stale responses are ignored. NONE and CREATING are local states, not accepted provider statuses. `refundedAt` is the first local observation of accepted provider completion; no provider-completion timestamp is exposed.

Provider validation requires nonempty `re_...` refund ID, matching booking payment ID, existing refund ID if attached, exact two-decimal EUR amount equal to persisted refund amount, and a supported provider status. `recordProviderState` additionally compares the submitted local claim's key/payment/amount with the persisted booking. The client does not expose provider-echoed refund metadata/key or list/search refunds; same-payment/same-amount unrelated responses cannot be independently distinguished before attaching a refund ID.

ADMIN full refund is `POST /bookings/{id}/refund`, returns 202 BookingDto, has no request body/amount parameter, and checks ADMIN in RefundService. Initially, the coordinator requires CONFIRMED and a payment ID but does not explicitly require paidAt/positive paid amount. It allows NONE, CREATING, FAILED and CANCELED; FAILED/CANCELED currently create a new key and recalculate the amount. CREATING retries preserve amount/key but allow the exact one-hour boundary, unlike CANCEL's exclusive bound. HTTP 5xx/transport timeout preserve CREATING; other HTTP errors (including 409/429) are marked FAILED. These are audit findings to reproduce before correction.

Origin-sensitive completion: ADMIN CONFIRMED → REFUNDED; normal cancellation stays CANCELLED; expired late payment stays EXPIRED. Completion zeros the normal administrative owner entitlement. Payout service preserves a PAID row and creates unique positive OWNER_PAYOUT_RECOVERY liability from the transferred amount less remaining entitlement. Active refund claims hold unpaid payouts; failed refunds do not falsely zero entitlement.

Notifications: REFUND_REQUESTED / REFUND_STATUS_CHANGED / PAYMENT_REFUNDED are booking audit events. Normal administrative refunds currently have **no dedicated requested/completion/failure email**. Cancellation completion has one CancellationRefundCompletedEvent and an asynchronous after-commit email listener. Late safety refund has its existing initiation notification and audit completion event. Do not invent an email contract during REF.

Schema: one refund slot on each booking; unique payment ID, refund key and refund ID; refund status CHECK and nonnegative refund amount CHECK. Currency is implicit EUR in this booking/refund model and validated at provider boundaries. There is no existing DB CHECK for refund amount <= totalPaidEur, and no separate provider refund FK table. Liability has positive amount and unique (booking_id,type). No generic status/amount/provider-ID override or custom partial admin-refund endpoint was found.

## Execution

| Scenario | Local result | Evidence |
| --- | --- | --- |
| REF-001 — admin full refund | PASS | ADMIN/state denial matrix; EUR 125.50 full; EUR 62.75 policy-owned partial |
| REF-002 — lifecycle | PASS | Six provider states, malformed facts, origin-aware completion, terminal replay |
| REF-003 — ambiguity | PASS | Durable claim, restart, exclusive replay boundary, repeated concurrent requests |
| REF-004 — paid payout recovery | PASS | Immutable PAID snapshot, single positive liability, both mark-paid race serializations |

Final shared REF PostgreSQL suite: **53 passed**. Full PostgreSQL regression: **692 cases, 686 passed, 6 opt-in browser skips, zero failures/errors**. Unit suite: **915 passed**. These are local Eema contract results, not live provider certification.

## Corrections reproduced during REF

1. **P0 — Refund identity replacement:** admin FAILED/CANCELED retries minted a new key and recalculated the amount. Now only NONE starts an operation; CREATING replays the existing one. Terminal failures require intervention. The same shared late-payment path now follows this rule.
2. **P1 — Replay boundary:** admin accepted exactly one hour. All shared claim paths now use `now < requestedAt + 1h`; exactly one hour and later return 409 without provider creation. This is a code constant, not a configurable provider guarantee.
3. **P0 safety boundary — Invalid paid claim:** admin did not require paidAt or positive paid total. Invalid payment identity, missing paidAt, zero/negative total now conflict before events or provider calls.
4. **P1 — Ambiguity misclassified:** HTTP 409/429 were recorded as definitive failures. HTTP 408/409/429/5xx and transport errors retain CREATING. Other HTTP rejections, represented by 422, enter FAILED. Logs contain status/error class, never raw provider bodies.
5. **P1 accounting — Fee after refund:** PostgreSQL reproduced rejection of fee reconciliation after admin completion. REFUNDED bookings now remain eligible for fee reconciliation and its queues/metrics; their owner entitlement stays zero.
6. **Database enforcement:** new common migration V12 adds the paid-total upper bound and prevents changing a claimed amount/key/payment association or attached provider refund ID. Existing nonnegative/status/unique constraints remain. No duplicate preview/refund tables or historical rewrites.

The first focused unit run reproduced 8 failures in 13 cases. The first PostgreSQL run had 43 passing cases, the reproduced fee defect, and one test-fixture insert missing liability status (fixture corrected to OPEN).

## Authorization and money authority

| Request | Result | Claim/provider/financial mutation |
| --- | --- | --- |
| ADMIN, successfully paid CONFIRMED, NONE | 202 with authoritative BookingDto | One full paid-amount claim; provider called after commit |
| ADMIN, CONFIRMED, CREATING before one hour | 202 if provider response succeeds | Same persisted amount/payment/key |
| ADMIN, REQUESTED/AWAITING_PAYMENT/DECLINED/EXPIRED/UNAVAILABLE/CANCELLED/REFUNDED | 409 | None |
| ADMIN, existing queued/pending/processing/failed/canceled claim | 409 | No replacement operation |
| OWNER or SCOUT | 403 | None |
| Anonymous | 401 | None |

The endpoint declares no request body. Supplied custom amount/percentage/currency JSON has no effect: the tested request still sends EUR 125.50, even after venue pricing changes. There is no arbitrary partial-refund endpoint or generic status/provider-ID/success override. One existing cancellation-policy evaluation produces 50%, EUR 62.75, and the refund provider call uses exactly EUR 62.75. The full CANCEL policy matrix is not duplicated.

## Durability, provider contract and concurrency

`RefundIntegrationTest` uses disposable PostgreSQL 16 and real transactions, HTTP security, booking/payout services and audit persistence. Provider behavior is a mock contract. The HTTP encoding/decoding tests remain separate in `RefundTransportContractTest`; policy/state-machine checks remain in `RefundCancellationContractTest` and `RefundClaimContractTest`.

The simulated provider reads the committed key through independent JDBC before accepting the request. Response loss leaves CREATING. A newly constructed coordinator, inside a fresh transaction, recovers key/payment/amount from PostgreSQL. Fixed Clock covers 3,599, 3,600 and 3,601 seconds; the inside-window restart case is also repeated three times. This restarts the service boundary, not the JVM or database.

Two concurrent HTTP refund requests meet at a provider barrier. Both share one durable key and one simulated provider idempotency-cache entry, with one REFUND_REQUESTED event and no 500. Two HTTP transports can occur; they represent one logical refund under the modeled provider idempotency contract. This is repeated three times and does not prove real Mollie deduplication.

The mark-paid race runs both PostgreSQL lock serializations three times each. Claim first holds the payout and rejects stale mark-paid. Transfer first keeps PAID historical and completion creates recovery liability. No sleeps are used. Repeated completion causes no additional payout recalculation, timestamp rewrite, audit completion or cancellation completion notification.

Existing shared failure injection is reused from `CancellationIntegrationTest.rollbackBeforeProviderLeavesNoCancellationOrClaim` and `acceptedRefundResponseLossOrPersistenceFailureResumesFromDatabase`: pre-call rollback makes no external call; post-provider local rollback leaves the pre-call identity recoverable. Existing cancellation concurrency and fee/liability assertions remain regression coverage, not newly repeated policy scenarios.

## Failure side-effect matrix

“Hold/recalculate” concerns an unpaid payout. PAID is always preserved. Completion notification below means the cancellation application event; admin has no dedicated refund email.

| Operation | Claim | Provider | BookingStatus | RefundStatus | Payout | Liability | Audit event | Completion notification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Auth/state/invalid paid denial | No new claim | None | Unchanged | Unchanged | Unchanged | None | None | None |
| Pre-call persistence rollback | Rolled back | None | Unchanged | Unchanged | Rolled back | None | Rolled back | None |
| Timeout / HTTP 408,409,429,5xx | Durable | Creation attempted | Unchanged | CREATING | Hold | None | REFUND_REQUESTED once | None |
| Definitive 422 rejection | Durable | Creation attempted | Unchanged | FAILED | Recalculate; no successful-refund zeroing | None | Requested + status change | None |
| Malformed creation response | Existing durable claim retained | Creation attempted | Unchanged | CREATING | Existing claim hold | None | No accepted provider transition | None |
| Malformed lookup response | Existing | Read only | Unchanged | Unchanged | Unchanged | None | None | None |
| Retry at/after one hour | Existing | None | Unchanged | CREATING | Existing hold | None | None | None |
| Provider accepted; follow-up persistence fails | Existing pre-call claim | Creation attempted | Follow-up rolled back | Pre-call state retained | Follow-up rolled back | None committed | Follow-up rolled back | None committed |
| Accepted queued/pending/processing | Existing | Creation/read | Unchanged | Accepted successor | Hold | None | Status change once | None |
| Accepted failed/canceled | Existing | Creation/read | Unchanged | FAILED/CANCELED | Recalculate; no false reversal | None | Status change once | None |
| Accepted completion | Existing | Creation/read | Origin-dependent | REFUNDED | Recalculate once; preserve PAID | One positive recovery if owed | PAYMENT_REFUNDED once | One for cancellation; zero admin |
| Terminal replay/stale status | Existing | Read only | Unchanged | Unchanged | Unchanged | No duplicate | None | None |

Malformed lookup cases cover wrong/missing payment ID, wrong/missing attached refund ID, wrong amount, fractional format, wrong/missing currency, missing amount/value, unknown status, local-only CREATING and null response. None can complete or alter booking/payout/liability/audit state. Existing shared tests also check initial response and local-claim mismatch.

## Payout history and fee integration

The fixture follows real readiness calculation: paid EUR 125.50, reconciled fee EUR 1.50, ended interval, verified destination, READY, then authorized mark-paid. The complete PAID row is compared byte-for-byte after refund and repeated recalculation, including amount EUR 98.90, paidAt, transfer reference and destination snapshot. One positive EUR 98.90 OWNER_PAYOUT_RECOVERY liability remains; no new transfer or negative payout exists. Liability uniqueness is exercised directly in PostgreSQL.

Unknown fees are not estimated. A later actual fee can reconcile after administrative completion without restoring owner entitlement. Currency remains implicit EUR in the schema, with explicit EUR validation at the provider boundary; no fictional provider foreign-key or currency column is claimed.

## Events, notifications and frontend audit

Requested audit events do not assert completion. Valid provider completion writes the first local refundedAt and one PAYMENT_REFUNDED audit event. Cancellation completion publishes its existing after-commit notification; email failure cannot roll back the refund transaction. Existing notification tests cover asynchronous failure handling. Administrative refund currently sends **zero dedicated lifecycle emails**; failed provider states do not generate success mail. Actual Resend delivery is untested locally.

Frontend `components/booking/RefundDetails.tsx` reads persisted amounts/status/timestamps. It distinguishes progress, processed and attention/failure, and does not locally compute refund economics. The admin booking detail exposes authoritative refund facts. There is no admin full-refund mutation UI to exercise or extend. No frontend change was needed for REF. UI copy and admin action deliberateness still need Eli's product review; the present admin action is API-only.

## Operational limitations and decisions

Normal admin refunds reconcile through payment callbacks/explicit service lookup of an attached refund ID; there is no standalone normal-refund polling worker. Older ambiguous creation without a returned refund ID still needs an operations reconciliation procedure. The current client cannot search provider refunds or validate echoed claim metadata; there is no generic force-success or rekey endpoint. Do not retry remotely beyond the safe window merely to close a queue item. The existing handling policy for these cases remains unresolved operational work.

No new accounting/waiver/partial-refund policy is introduced. Existing CANCEL accounting questions remain in its report. Real provider status vocabulary, idempotency retention and bank settlement timing require staging confirmation; the local one-hour rule is Eema's safety boundary.

## What Eli should test personally

- Is “refund in progress” clearly different from “refund processed”?
- Is failed-refund copy understandable without suggesting the booking was restored?
- Does admin full refund feel deliberate enough, given the current API-only action?

## Staging only

Real Mollie full refund; real partial cancellation refund; actual status values and reconciliation; provider timeout/idempotency behavior; bank settlement timing; real refund emails where the current lifecycle defines them. Mocks prove Eema safety logic only.

## Broad regression fixture corrections

The first full PostgreSQL run executed 687 cases (6 opt-in browser skips) and found two stale pre-CANCEL test calls: `PolicyIntegrationTest.replacementGatesCommitmentsAndPreservesBookingRightsAndSnapshots` and the cancellation variant of `VenueDomainIntegrationTest.avlConfirmationKeepsReservationAndCancellationOrRefundReleasesIt`. Both submitted paid cancellation without preview consent and correctly received 409. Their fixtures now obtain and submit the authoritative preview token. Production cancellation rules were not relaxed. All 48 REF cases present in that first full run passed; the final run adds five explicit HTTP rejection/ambiguity cases and stronger DB/query assertions.

## Reviewable sources and evidence

- [Refund PostgreSQL contract](../../src/test/java/com/eema/backend/integration/RefundIntegrationTest.java)
- [Claim contract unit tests](../../src/test/java/com/eema/backend/service/RefundClaimContractTest.java)
- [Shared lifecycle/validation contract](../../src/test/java/com/eema/backend/service/RefundCancellationContractTest.java)
- [Separate HTTP transport contract](../../src/test/java/com/eema/backend/service/RefundTransportContractTest.java)
- [Refund coordinator](../../src/main/java/com/eema/backend/service/RefundCoordinator.java), [provider orchestration](../../src/main/java/com/eema/backend/service/RefundService.java), [V12 invariants](../../src/main/resources/db/migration/common/V12__refund_identity_invariants.sql)
- [Existing CANCEL execution](CANCEL_EXECUTION_REPORT.md), [existing PAY execution](PAY_EXECUTION_REPORT.md)
- [Initial reproduced failures](evidence/refund/before.json), [first full PostgreSQL results](evidence/refund/postgres-first-full.json), [final unit results](evidence/refund/unit-final.json), [source hashes](evidence/refund/source-sha256.json)

Raw local logs remain under `/tmp/ref-*.log`; committed evidence contains counts/test names and hashes, not JWTs, provider credentials or raw provider bodies. Disposable PostgreSQL aggregates are removed by Testcontainers; no shared financial history is deleted.

## Final validation and disposition

| Check | Final result |
| --- | --- |
| `mvn test` | PASS — 915 tests, zero failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS — 692 cases: 686 passed, 6 opt-in browser skips; zero failures/errors |
| Shared REF PostgreSQL subset | PASS — 53 cases, including repeated ambiguity/duplicate-request tests and both payout race serializations |
| `mvn clean package` | PASS — clean executable JAR, all 915 unit tests passed again |
| Frontend `npm run typecheck` | PASS |
| Frontend `npm run lint` | PASS — no warnings/errors |
| Frontend `npm run build` | PASS |
| `git diff --check` in both repositories | PASS |
| Master-plan PDF | Regenerated, 646 pages; REF sections on pages 87–89; page 88 visually inspected without clipping |

Final evidence: [command summary](evidence/refund/validation.json), [53 REF case results](evidence/refund/ref-final.json), [full PostgreSQL results](evidence/refund/postgres-final.json), [PDF verification](evidence/refund/pdf.json), [page preview](evidence/refund/master-plan-ref-page88.png).

**REF-001–004 are locally clear to proceed.** No unresolved reproduced local refund defect remains. Real provider verification and the existing manual handling procedure for older ambiguous refunds remain separate staging/operations work. No new product or accounting decision is invented by this pass.
