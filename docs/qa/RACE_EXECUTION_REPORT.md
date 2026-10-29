# RACE execution report

**RACE-001–008: 8 PASS / 0 FAIL / 0 BLOCKED.** No new production code, test code, migration or frontend change was needed. This is an evidence reconciliation against the current application, not a claim that raw SQL tests alone prove application races.

## Audit and corrected contracts

The pre-implementation inventory and source audit are preserved in `evidence/race/history/pre-implementation-audit.md`. Read all eight generated scenarios, actual transactional services/controllers/repositories, V1 reservation exclusion, V4 payout uniqueness, V6 document uniqueness, V10 verified-destination partial uniqueness, V11 current-policy uniqueness, V14 outbox claims, and the corresponding test barriers/assertions. AUTH, BK/AVL, INQ, PAYOUT, DEST, POL, DOC, OPS and JOB reports remain the policy authorities.

Generic actor/provider matrices were removed. Internal workers and ledger calculation have no invented HTTP mutation endpoint. Refresh is credential-authenticated, with no email-verification or current-policy requirement. Historical documents retain their existing read authorization. Duplicate document reads and idempotent ledger commands may both succeed. Distinct destination verifications may both succeed in sequence, leaving one VERIFIED and one REPLACED record. This differs deliberately from a raw SQL uniqueness race's one-success/one-failure result.

## Scenario results and actual execution evidence

| ID | Result | Current application outcome and evidence |
|---|---|---|
| RACE-001 | PASS | Three actual HTTP acceptance races use a venue-lock barrier: 200/409, one AWAITING_PAYMENT winner and one UNAVAILABLE competitor, authoritative availability exclusion and later release. VenueDomainIntegrationTest plus separate raw exclusion race. |
| RACE-002 | PASS | Three repetitions each of two inquiry accepts and inquiry-versus-normal acceptance use the shared venue lock: 200/409, one reservation and no accepted inquiry without a booking. Duplicate same-offer race creates one booking/audit. Separate real PostgreSQL overlapping AWAITING_PAYMENT/CONFIRMED insertion proves exclusion. |
| RACE-003 | PASS | Concurrent HTTP refresh yields 200/401, one revoked original and one live successor, two total token rows. RefreshTokenRepository pessimistic lock and transactional AuthService inspected; AUTH browser/client single-flight and stale-session results reused. |
| RACE-004 | PASS | Concurrent real service recalculation produces one payout ledger and one calculation audit with exact existing amounts. Same manual transfer reference is idempotent; different reference conflicts; one PAID audit. No automatic external transfer exists. PAYOUT and separate unique constraint race. |
| RACE-005 | PASS | Three real HTTP distinct-verification races serialize owner routing: both 200, exactly one VERIFIED and one REPLACED. Same-record verify/reject gives 200/409 and one decision. READY/PAID historical destination preservation remains covered by DEST/PAYOUT. |
| RACE-006 | PASS | Three real HTTP replacement-publication races pause after the current-version read: one 200/one 409, one current version and loser still DRAFT. Separate SQL race covers first publication uniqueness. Actual legal content approval is outside this technical concurrency contract. |
| RACE-007 | PASS | Three receipt and three statement HTTP first-download races produce identical bytes, one stored object, one document and one audit. Storage/DB/late-commit failure cases prove rollback cleanup and retry. Current approved Unicode/immutable document policy is preserved. |
| RACE-008 | PASS | Actual JOB worker races, held-row skip/recovery, 101-row bounds and rollback checks; PAYOUT disjoint booking claims; OPS competing dispatchers, lease equality and stale-worker fencing. Underlying SQL disjoint-batch tests are separate corroboration. |

The detailed method mapping is `evidence/race/scenario-mapping.json`. All mapped classes were executed successfully in the full PostgreSQL run completed immediately before this RACE audit, against the same production/test source. Their ten complete classes contain 423 executed tests with zero failures/errors/skips; that is a class total, **not** 423 distinct RACE scenarios. No test is marked PASS merely because it exists. Source, enabled annotations, barriers, final persisted assertions and actual successful class results were checked together.

## Transaction, provider and UI boundaries

Booking acceptance serializes venue then booking and retains the exclusion constraint as a final guard. Inquiry acceptance shares that reservation boundary. Refresh locks its token row. Payout calculation locks the booking; destination mutation locks owner routing; document issuance locks its parent. Policy publication relies on its transaction/version/unique authority and safe conflict mapping. None of these authorities was replaced by a client-side assumption.

Worker contracts differ: request/inquiry/readiness batches retain transactional locks; approval selection is short-lived with a locked state recheck per mutation; fee reporting may overlap read-only lookups while financial reconciliation remains singular. Outbox dispatch has short fenced claims, no network call in a transaction, and recoverable leases. At-least-once notification semantics do not become exactly-once inbox delivery.

Existing frontend booking actions refetch after conflict, destination review reloads authoritative state on 409, and auth refresh uses the existing session-safe single-flight path. Their previously executed domain browser/client results are reused. No new browser journey or UI change was necessary to restate these database/application guarantees.

## Validation and defects

No new defect or unresolved P0/P1/product/business/legal ambiguity was found in RACE. Earlier fixes remain documented in their original reports; the generic “one request must fail” assumption was corrected where the approved contract allows serial replacement or idempotent success.

The required complete regression was already executed in this continuing validation on the identical production/test/frontend state and is reused without an unnecessary duplicate run:

| Gate | Actual result |
|---|---|
| `mvn test` | PASS: 1,078 tests, no failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS: 1,023 reported, 1,010 executed, 13 opt-in browser skips; no failures/errors |
| `mvn clean package` | PASS: 1,078 tests and packaged application |
| Backend diff check | PASS; repeated after RACE documentation |
| Frontend | Unchanged; OPS typecheck/lint/build PASS and current diff check reused |

See `evidence/job/` command summaries and `evidence/race/reused-full-suite.json`. No new tests were added because the executed domain suites already cover all corrected RACE requirements. The known cached critical-path scheduler/test-container cleanup warning remains separately characterized in OPS history; it did not fail an assertion or command. Real providers were disabled/mocked; no production policy, payment, refund or transfer was made.

The master plan and PDF now record all eight corrected RACE results. Scenario IDs/order are preserved, historical generated wording remains in evidence, and changed pages are visually checked.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review whether stale booking/destination conflict wording and authoritative reload make the outcome understandable. Manual race, lock, financial uniqueness or token-rotation reproduction is not requested.

## PRODUCT / OPERATIONS DECISIONS

None newly required for the eight technical race contracts. Existing legal policy content and operational/provider launch prerequisites remain separate; no new business policy was inferred.

## STAGING ONLY

Deployed multi-instance behavior under realistic load, real provider idempotency/delivery, storage infrastructure and production database/monitoring configuration. Local concurrent PostgreSQL connections do not certify those deployment conditions.

**Locally clear to proceed: YES. NEXT UNTESTED DOMAIN: UX**, subject to exact inventory/source audit before modifications. Automatic continuation remains conditional on no unresolved P0/P1 or applicable product/business/legal decision.
