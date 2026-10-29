# PAYOUT execution report

**Completed 2026-09-19: PAYOUT-001–006 — 6 LOCAL PASS, 0 FAIL, 0 BLOCKED.** Five reproduced defects fixed (four P1, one P2); no unresolved reproduced PAYOUT defect. Local technical completion does not approve accounting policy or real-provider operations. Automatic continuation is stopped because the excess-provider-fee accounting decision remains unresolved. The source audit and inventory were recorded before production changes.

## Selection and dependencies

PAYOUT-001–006 were selected as the first locally unfinished scenarios. No prior PAYOUT execution report existed. Earlier AUTH, POL, TRUST, VEN, READY, MEDIA, AVL, MATCH, BK, INQ, PRICE, PAY and CANCEL have local execution evidence; REF also has cross-domain evidence. This audit follows the complete master plan and existing execution reports read for this validation run. Dependencies: PRICE (stored economics/commission), PAY (paid and late-payment identity), CANCEL (retained amounts and provider responsibility), REF (refund holds/reversals and recovery), AUTH (role and object scope), TRUST (verified destinations and audit).

## Scenario results and coverage

| Scenario | Result | Corrected contract | Coverage executed |
|---|---|---|---|
| PAYOUT-001 | PASS | One ledger per booking; stored economics, no negative normal payout; paid history immutable | Persisted repeat/concurrent creation, zero and ineligible rows, snapshots |
| PAYOUT-002 | PASS | Zero, active refund, missing fee, booking end, destination, READY in that order | Combined blockers; fixed-clock end equality and DST; PostgreSQL readiness worker locking |
| PAYOUT-003 | PASS | Actual fee is reconciled once; exact repeat harmless, changed fee conflicts; owner-caused cost creates liability | Fee validation, unpaid rejection, persistence/audit and role scope |
| PAYOUT-004 | PASS | Destination snapshots at first READY and survives replacement | Real destination A/B replacement and repeated readiness; masked reads |
| PAYOUT-005 | PASS | ADMIN records an external transfer on READY; this endpoint does not send money | HTTP validation, idempotency/concurrency, forbidden roles, stale browser state/refetch |
| PAYOUT-006 | PASS | ADMIN settles the stored OPEN liability, once; SETTLED retry harmless, WAIVED conflicts | Both liability types; HTTP scope, arbitrary input ignored, concurrent settlement, browser |

## Source audit and authoritative rules

Reviewed OwnerPayoutService, OwnerFinancialLiabilityService, OwnerPayoutReadinessJob, their repositories/controllers/DTOs, BookingService.reconcileMollieFee, BookingFinancials, OwnerPayoutDestinationService, migrations V4/V5/V10, existing payout/liability unit tests, cancellation/refund PostgreSQL suites, admin payout/liability pages and owner payout detail. Generated generic actor/provider matrices are not authoritative: owner reads are owner-scoped; admin manages recording/settlement; there is no automated bank payout, arbitrary amount adjustment or liability waive endpoint. Registration policy/email gates are not mechanically reapplied to admin financial operations.

Payout readiness uses the venue timezone and shoot end. Active refunds hold positive payouts; a zero payout is CANCELLED even when another blocker exists. Fee, booking-end and destination blockers follow in that order. PAID economics/routing/reference are historical; reversals create liabilities. Recording payment and settling liabilities are bookkeeping actions, not provider transfers. Persisted booking financial snapshots are authoritative; current listing price or client input must not alter them.

## Existing coverage reused

OwnerPayoutServiceTest and OwnerFinancialLiabilityServiceTest cover basic readiness, cancellation responsibilities, paid recovery, role checks and retries. CANCEL and REF PostgreSQL suites prove payout-versus-cancellation/refund serialization and paid history preservation. PRICE owns commission and rounding mathematics. The added tests reuse those proven state machines rather than duplicate their full boundary matrices.

## Initial audit hypotheses (before implementation)

Nullable UUID filters may fail on PostgreSQL admin list queries. The readiness worker currently acquires payout locks before booking locks, opposite to payment/cancellation mutations. Fee reconciliation accepts CANCELLED bookings without explicitly requiring prior payment. Admin conflict feedback is rendered only inside action forms and may disappear when authoritative state changes. These initial hypotheses were subsequently reproduced; the findings and fixes are recorded below.

## Local / external boundary

Local: deterministic rules, PostgreSQL transactions/constraints, mocked provider safety, browser authority and complete builds. STAGING ONLY: real bank transfer, Mollie fee/report matching, actual provider responses, deployed multi-instance scheduling and delivery.

## PRODUCT / OPERATIONS DECISIONS

CANCEL records the unresolved allocation of provider fees exceeding OWNER_SETTLEMENT entitlement; REF carries that question forward. PRICE separately retains its zero-price checkout decision. Preserve the implemented clamp/rejection behavior and explicit liabilities; do not invent debt policy. No new debt or waiver policy was introduced. The excess-fee decision prevents automatic continuation under the user's instruction. The inherited PRICE decision is also not resolved by successful local payout tests.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review clarity of held/readiness labels, the distinction between recording and sending a transfer, liability settlement wording, the owner-facing financial terminology and mobile comprehension. Do not manually retest money arithmetic, races, SQL constraints or authorization.

## Reproduced defects — all fixed

- PAYOUT-D1 (P1): both admin payout and liability list endpoints returned HTTP 500 with absent UUID filters. PostgreSQL rejected untyped nullable parameters. Reproduced in two separate database runs. Replaced nullable JPQL predicates with predicates only for supplied filters, retaining queue priority and pagination.
- PAYOUT-D2 (P1): CANCELLED without prior payment accepted fee reconciliation and acquired recalculated financial state. Reproduced twice; REQUESTED correctly rejected. Added the same paid-at/positive-total prerequisite used by the fee worker. The current automatic worker already excludes unpaid rows; this closes the service-level gap rather than claiming a reproduced external transfer.
- PAYOUT-D3 (P1): verifying destination B when A is VERIFIED violated the one-verified-destination partial unique index. Independently reproduced twice. Hibernate queued both updates in the wrong order. Flush A's REPLACED update before setting B VERIFIED; all changes remain in one transaction.
- PAYOUT-D4 (P1): readiness scan held payout locks and waited for a busy booking, opposite to the booking-first financial mutation order. Independent transaction/latch tests reproduced a five-second bounded completion failure twice. The regression does not use sleeps and does not claim a reproduced deadlock. Worker now selects/locks booking IDs with `FOR UPDATE OF b SKIP LOCKED`, then reads/recalculates payout state after acquiring the booking lock.

Clock injection in payout calculation/recording and the readiness job is a deterministic test seam using the existing production UTC Clock bean; it does not introduce a new payout policy.

- PAYOUT-D5 (P2): admin payout/liability conflict alerts were nested inside READY/OPEN action forms. When authoritative refetch changed the state, the message disappeared. Two independently changed payout records returned real HTTP 409 and reproduced the hidden alert; a controlled liability 409/WAIVED client response reproduced the same rendering failure (no public waive endpoint was invented). Moved alerts outside the conditional forms. Successful mutations still refetch authoritative state. Historical browser evidence records seven passing checks and three failing conflict checks before this fix. A first harness attempt failed on a cancelled Chrome interception during navigation; that harness failure is not classified as a product defect.


## Executed coverage and complete regression

Added 28 parameterized unit cases in `OwnerPayoutServiceTest` (13 blocker combinations, 9 end-time/DST boundaries, 6 ineligible booking states), 24 PostgreSQL cases in `PayoutIntegrationTest`, and one browser launcher with ten workflow/client checks. The focused unit run passed all 40 payout/liability tests; the first green PostgreSQL run passed 23 cases. The final full regression passed all 24 payout PostgreSQL cases, including the subsequently added worker equality case and destination-B assertion.

Full `mvn test`: **973 passed, 0 failed, 0 skipped**. Frontend `npm run typecheck`, `npm run lint`, `npm run build`, and its `git diff --check`: **PASS**. Final browser rerun: **10/10 checks PASS** against the rebuilt production frontend. Full `mvn verify -Ppostgres-integration`: **716 executed PASS, 7 opt-in browser launchers skipped (723 reported), 0 failures/errors**. The payout browser launcher was explicitly executed separately. `mvn clean package`: **PASS, 973 unit tests, 0 failures/errors/skips**. Both repositories' final `git diff --check` passed.

Browser screenshots were visually reviewed: manual-transfer confirmation, persistent conflict warning with authoritative PAID state, and owner-only masked financial history. The liability stale-state case is controlled transport evidence, while actual settlement, payout conflicts, owner scope and reload checks use real local API/persistence.

## STAGING ONLY

Real Mollie fee/report reconciliation and bank settlement; verification that a recorded manual transfer matches the actual external bank transfer; actual delivery of lifecycle/trust emails; deployed multi-instance worker operation and operational monitoring. Local Testcontainers and mocked provider tests do not verify these integrations.

## Local completion and continuation gate

The domain is locally technically complete and all required regression/build commands passed. The master-plan Markdown and 639-page PDF are updated; all 703 scenario IDs are preserved in order, with pages 83–87 visually reviewed. Stop because the excess-provider-fee accounting decision remains unresolved under the user's explicit continuation condition. No new accounting/waiver rule or automated payout is authorized by this validation. NEXT UNTESTED DOMAIN: DEST-001–004, covering destination submission/validation, verification/replacement, rejection and audited reveal. DEST has not been started.


## Documentation, evidence and scope

Updated this report, `docs/EEMA_COMPLETE_TEST_MASTER_PLAN.md` and its PDF. Sanitized [execution evidence](evidence/payout/README.md) includes baseline/reproduction failures, focused/full totals, browser before/after results, screenshots, [completion metadata](evidence/payout/completion.json) and [document checks](evidence/payout/document-check.json). Evidence excludes credentials, tokens, plaintext bank details and provider bodies. Temporary QA frontend/browser processes and browser profiles were cleaned up.

Production changes are limited to the two financial repository searches, the fee service prerequisite, destination update ordering, payout/job Clock injection and booking-first readiness claims, plus conflict-alert placement in the two existing admin detail pages. No schema migration, new financial workflow, automated transfer, pricing model or debt policy was introduced. Pre-existing work from earlier domains remains intact. DEST scenarios remain NOT RUN; its destination replacement path was exercised only as PAYOUT-004's required dependency.
