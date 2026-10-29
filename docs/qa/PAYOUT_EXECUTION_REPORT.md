# PAYOUT execution report

**Completed 2026-09-19: PAYOUT-001–006 — 6 LOCAL PASS, 0 FAIL, 0 BLOCKED.** The approved Eema v1 allocation policy and subsequent paid-history correction are implemented and fully regressed. PAYOUT-V1-D1 is fixed; no unresolved PAYOUT P0/P1 or allocation decision remains. Earlier defect evidence and superseded command results are retained below. DEST was also completed during the authorized continuation; the next domain is DOC, where the separate inherited zero-price decision stops automatic progression.

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

The previously inherited CANCEL/REF excess-fee decision is resolved by the user's Eema v1 policy recorded below. OWNER_SETTLEMENT is limited recourse to unpaid entitlement; OWNER alone permits explicit excess provider-cost debt. Automated debt collection and liability netting are not implemented. Review this policy if Eema adopts Mollie Connect, delayed routing, automated payouts or a different merchant-of-record structure. The separate PRICE zero-price checkout question is unchanged and does not govern destination submission/review.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review clarity of held/readiness labels, the distinction between recording and sending a transfer, liability settlement wording, the owner-facing financial terminology and mobile comprehension. Do not manually retest money arithmetic, races, SQL constraints or authorization.

## Reproduced defects — all fixed

- PAYOUT-D1 (P1): both admin payout and liability list endpoints returned HTTP 500 with absent UUID filters. PostgreSQL rejected untyped nullable parameters. Reproduced in two separate database runs. Replaced nullable JPQL predicates with predicates only for supplied filters, retaining queue priority and pagination.
- PAYOUT-D2 (P1): CANCELLED without prior payment accepted fee reconciliation and acquired recalculated financial state. Reproduced twice; REQUESTED correctly rejected. Added the same paid-at/positive-total prerequisite used by the fee worker. The current automatic worker already excludes unpaid rows; this closes the service-level gap rather than claiming a reproduced external transfer.
- PAYOUT-D3 (P1): verifying destination B when A is VERIFIED violated the one-verified-destination partial unique index. Independently reproduced twice. Hibernate queued both updates in the wrong order. Flush A's REPLACED update before setting B VERIFIED; all changes remain in one transaction.
- PAYOUT-D4 (P1): readiness scan held payout locks and waited for a busy booking, opposite to the booking-first financial mutation order. Independent transaction/latch tests reproduced a five-second bounded completion failure twice. The regression does not use sleeps and does not claim a reproduced deadlock. Worker now selects/locks booking IDs with `FOR UPDATE OF b SKIP LOCKED`, then reads/recalculates payout state after acquiring the booking lock.

Clock injection in payout calculation/recording and the readiness job is a deterministic test seam using the existing production UTC Clock bean; it does not introduce a new payout policy.

- PAYOUT-D5 (P2): admin payout/liability conflict alerts were nested inside READY/OPEN action forms. When authoritative refetch changed the state, the message disappeared. Two independently changed payout records returned real HTTP 409 and reproduced the hidden alert; a controlled liability 409/WAIVED client response reproduced the same rendering failure (no public waive endpoint was invented). Moved alerts outside the conditional forms. Successful mutations still refetch authoritative state. Historical browser evidence records seven passing checks and three failing conflict checks before this fix. A first harness attempt failed on a cancelled Chrome interception during navigation; that harness failure is not classified as a product defect.


## Original execution and regression (before policy approval)

Added 28 parameterized unit cases in `OwnerPayoutServiceTest` (13 blocker combinations, 9 end-time/DST boundaries, 6 ineligible booking states), 24 PostgreSQL cases in `PayoutIntegrationTest`, and one browser launcher with ten workflow/client checks. The focused unit run passed all 40 payout/liability tests; the first green PostgreSQL run passed 23 cases. The final full regression passed all 24 payout PostgreSQL cases, including the subsequently added worker equality case and destination-B assertion.

Full `mvn test`: **973 passed, 0 failed, 0 skipped**. Frontend `npm run typecheck`, `npm run lint`, `npm run build`, and its `git diff --check`: **PASS**. Final browser rerun: **10/10 checks PASS** against the rebuilt production frontend. Full `mvn verify -Ppostgres-integration`: **716 executed PASS, 7 opt-in browser launchers skipped (723 reported), 0 failures/errors**. The payout browser launcher was explicitly executed separately. `mvn clean package`: **PASS, 973 unit tests, 0 failures/errors/skips**. Both repositories' final `git diff --check` passed.

Browser screenshots were visually reviewed: manual-transfer confirmation, persistent conflict warning with authoritative PAID state, and owner-only masked financial history. The liability stale-state case is controlled transport evidence, while actual settlement, payout conflicts, owner scope and reload checks use real local API/persistence.

## STAGING ONLY

Real Mollie fee/report reconciliation and bank settlement; verification that a recorded manual transfer matches the actual external bank transfer; actual delivery of lifecycle/trust emails; deployed multi-instance worker operation and operational monitoring. Local Testcontainers and mocked provider tests do not verify these integrations.

## Original completion gate (superseded by approved policy below)

The domain is locally technically complete and all required regression/build commands passed. The master-plan Markdown and 639-page PDF are updated; all 703 scenario IDs are preserved in order, with pages 83–87 visually reviewed. This was the original completion gate before the user resolved the policy; see the policy update below for the current gate. No waiver or automated payout is introduced. NEXT UNTESTED DOMAIN: DEST-001–004, covering destination submission/validation, verification/replacement, rejection and audited reveal. DEST has not been started.


## Documentation, evidence and scope

Updated this report, `docs/EEMA_COMPLETE_TEST_MASTER_PLAN.md` and its PDF. Sanitized [execution evidence](evidence/payout/README.md) includes baseline/reproduction failures, focused/full totals, browser before/after results, screenshots, [completion metadata](evidence/payout/completion.json) and [document checks](evidence/payout/document-check.json). Evidence excludes credentials, tokens, plaintext bank details and provider bodies. Temporary QA frontend/browser processes and browser profiles were cleaned up.

Original pre-policy production changes were limited to the two financial repository searches, the fee service prerequisite, destination update ordering, payout/job Clock injection and booking-first readiness claims, plus conflict-alert placement in the two existing admin detail pages. No schema migration, new financial workflow, automated transfer, pricing model or debt policy was introduced. Pre-existing work from earlier domains remains intact. DEST scenarios remain NOT RUN; its destination replacement path was exercised only as PAYOUT-004's required dependency.

## Authorized Eema v1 allocation update — COMPLETE

The user has resolved the excess-provider-cost decision. This addendum completes the policy update for PAYOUT-001/003/005/006; the original evidence above remains historical and the fresh results below govern current completion.

Before implementation, audited BookingFinancials, BookingService fee reconciliation, BookingCancellationPolicy/Coordinator, OwnerPayoutService, booking/payout/liability entities and V3/V4/V10 constraints, existing unit/PostgreSQL tests, and owner/admin financial rendering. Actual cancellation assignments match the requested policy: scout OWNER_SETTLEMENT, owner OWNER, admin EEMA, unpaid withdrawal NONE. Non-cancelled bookings have NULL responsibility, not NONE; preserve their existing settlement-based fee allocation, now capped at entitlement.

Policy: NONE/EEMA never deduct owner proceeds or create provider-cost owner debt. OWNER_SETTLEMENT means limited recourse to unpaid settlement only; excess is Eema's expense. OWNER means explicit responsibility; only authoritative excess beyond unpaid entitlement creates PROVIDER_COST liability. No fee reduces the scout refund. Historical PAID rows and existing recovery semantics remain protected; provider-cost changes alone must not manufacture OWNER_SETTLEMENT debt via the recovery type.

Representation audit: booking actual mollieFeeEur/reconciled flag, cancellation ownerCancellationSettlementEur (pre-fee entitlement), responsibility, ownerPayoutEur, immutable payout snapshots and explicit liabilities provide authoritative inputs. Derive applied cost/platform excess through the same production allocation logic rather than storing redundant mutable totals. Normal pre-fee entitlement is venuePriceEur minus stored commissionEur. No general ledger, liability netting, automated collection, bank debit or arbitrary adjustment is in scope.

Missing coverage: all four responsibilities × fee below/equal/above entitlement/zero entitlement; positive OWNER/EEMA entitlement (existing cancellation paths happen to have zero); actual excess provider liability; fee-before/after-cancellation convergence; concurrent reconciliation; paid history and no fee-only limited-recourse recovery. Reuse prior financial races and full regression. Any frontend work is limited to correcting misleading existing presentation if the audit proves it necessary; no new debt UI.

This policy needs review if Eema adopts Mollie Connect, delayed routing, automated payouts or changes merchant-of-record structure. Real provider/bank behavior remains staging-only. After this policy passes and introduces no unresolved P0/P1 or accounting ambiguity, proceed to the next domain under the user's continuation instruction.

Existing owner presentation audit: the raw authoritative provider fee was labeled “Less: Payment processing cost,” which incorrectly implies that the whole fee is deducted even when allocation is capped or platform-borne. Correct only the label to “Provider processing cost”; keep the backend payout authoritative. This is an existing presentation correction, with no new accounting fields, scout exposure or frontend arithmetic. Frontend regression is therefore required for this update.

### Eema v1 authoritative representation and regression

For unpaid payouts, `BookingFinancials.allocateProviderCost` returns payout, applied settlement cost, Eema-borne cost and explicit owner liability from the persisted pre-fee entitlement, actual reconciled provider cost and responsibility. Normal entitlement uses stored venue price minus stored commission; cancellations use stored pre-fee owner cancellation settlement. Refund/late-payment reversal has zero entitlement. Raw `mollieFeeEur` / payout `providerCostEur` always means the authoritative provider cost, **not** an assertion that the whole amount was deducted from the owner.

For a PAID row, the recorded payout and routing remain authoritative and immutable. The historical deduction can be derived from its retained gross minus commission minus paid amount, bounded at zero; its recorded cost and applied deduction distinguish historical platform expense. Later booking fee knowledge must be evaluated against that immutable history, not reinterpreted as a new deduction from already-transferred funds. Recovery continues to represent reversal of entitlement; calculation retains the fee actually deducted at transfer so a fee-only update cannot manufacture limited-recourse recovery debt. Actual owner cancellation has zero retained entitlement and uses the existing explicit OWNER provider-cost liability and, when applicable, true paid-entitlement recovery.

| Responsibility | Entitlement | Actual fee | Applied to unpaid settlement | Payout | Platform cost | PROVIDER_COST liability |
|---|---:|---:|---:|---:|---:|---:|
| NONE | 16.00 | 25.00 | 0.00 | 16.00 | 25.00 | 0.00 |
| EEMA | 16.00 | 25.00 | 0.00 | 16.00 | 25.00 | 0.00 |
| OWNER_SETTLEMENT | 16.00 | 25.00 | 16.00 | 0.00 | 9.00 | 0.00 |
| OWNER | 16.00 | 25.00 | 16.00 | 0.00 | 0.00 | 9.00 |

The 16-case matrix additionally covers fees 5.00 and 16.00, and zero entitlement with fee 25.00, for each responsibility. Unit assertions use the production helper and exact two-decimal results. PostgreSQL assertions call the real reconciliation/payout services in independent transactions with a barrier, inspect persisted payout/liability/refund/audit state, and repeat reconciliation without duplicate debt. Controlled pre-fee fixtures cover positive OWNER/EEMA entitlement; these are generic allocation cases, not claims that current full-refund cancellation policy creates positive OWNER/EEMA entitlement.

Actual cancellation tests cover SCOUT / OWNER / ADMIN with fees before / after / concurrent with cancellation. For the EUR 125.50 paid fixture and EUR 75 actual fee, SCOUT retains EUR 50.20 pre-fee owner entitlement, pays zero, refunds EUR 62.75 and leaves EUR 24.80 platform cost without debt. OWNER refunds EUR 125.50 and records EUR 75 provider-cost liability; ADMIN refunds EUR 125.50 with EUR 75 platform cost and no debt. Additional historical PAID tests cover NONE, EEMA, OWNER_SETTLEMENT and existing normal NULL responsibility; a normal NULL-responsibility excess-fee case also proves zero payout and no debt.

Focused policy/unit prerequisites passed. Initial focused PostgreSQL run: **50 passed** (24 prior payout cases + 26 new policy cases). Full regression includes four additional normal/PAID responsibility cases. All required full commands passed; sanitized results are under `evidence/payout/v1-policy/`. Earlier evidence remains preserved, including the pre-policy report under `evidence/payout/history/pre-v1-policy/`.


### Initial policy completion and command results (superseded by paid-history correction)

- Focused unit prerequisites: 89 PASS. Focused PostgreSQL: 50 PASS.
- `mvn test`: 989 PASS, no failures/errors/skips.
- `mvn verify -Ppostgres-integration`: 746 executed PASS, 7 opt-in browser launchers skipped, 753 reported; no failures/errors. This includes all 30 policy PostgreSQL cases and the 24 prior payout cases, plus full CANCEL/REF regression.
- `mvn clean package`: PASS, 989 unit tests, no failures/errors/skips.
- Frontend typecheck, lint, build and both repositories' whitespace checks: PASS. Prior ten-check PAYOUT browser evidence is reused for unchanged interactions; the policy update changes only one existing owner fee label.
- Master plan and PDF updated; all 703 IDs preserved in order, 639 pages, pages 84–88 visually reviewed.

No schema migration, automated payout, collection, netting, bank debit, arbitrary balance edit or new debt-management UI was introduced. Historical PAID rows remain immutable. The approved allocation decision is resolved; no new accounting ambiguity or unresolved P0/P1 defect was found. PAYOUT is locally clear to proceed under the user's automatic-continuation instruction.

NEXT UNTESTED DOMAIN: DEST-001–004 — submission/validation, encrypted masked access, review/replacement/rejection, and audited ephemeral reveal. Destination validation depends on AUTH, POL, TRUST and PAYOUT evidence; those reports do not by themselves execute the four DEST scenarios.


### Additional paid-history finding during DEST dependency regression — FIXED

A final financial audit and standalone probe against the actual OwnerPayoutService reproduced a gap in the new generic positive-OWNER allocation: an unchanged EUR 16 entitlement / EUR 5 already-applied fee / EUR 11 PAID payout created a new EUR 5 provider-cost liability on recalculation. The current owner-cancellation policy normally has zero entitlement, but the explicitly requested generic OWNER matrix must remain correct after payment as well. Classify this as PAYOUT-V1-D1, P0 financial integrity; do not leave the newly approved rule capable of double-charging. The full DEST regression was stopped before completion rather than reported green.

A related historical-basis check distinguishes provider cost from the amount actually deducted at transfer: a NONE/EEMA fee in a paid snapshot must not later be treated as an old owner deduction when protecting limited-recourse history. Add service and PostgreSQL regressions before correction. Preserve genuine owner-cancellation reversal: once entitlement is reversed to zero, the explicit OWNER provider cost remains owed alongside the established net-payout recovery.

The independent payout service run reproduced all three new historical-basis assertions (41 cases: 38 pass, 3 fail). The corrected service/allocation run passes **57 cases**. Production now derives the original owner deduction as nonnegative retained gross minus commission minus paid amount, capped by the historical actual provider cost. For a PAID OWNER record, recognize that deduction only up to surviving pre-fee entitlement before calculating any provider-cost liability. Recovery uses the amount actually deducted at transfer, not raw provider cost that may have been platform-borne.

This preserves both sides of the approved policy: an unchanged EUR 16 / fee 5 / PAID 11 OWNER record creates no debt; if its entitlement is genuinely reversed to zero, existing recovery remains EUR 11 plus explicit OWNER provider-cost liability EUR 5. Original NONE/EEMA-paid fees cannot later become OWNER_SETTLEMENT recovery debt. PAID rows remain immutable, and no old liability is netted against another booking. Three PostgreSQL cases exercise these persisted histories as well; all focused and final complete regressions passed.


### Final policy result after paid-history correction

The standalone service probe and three failing service cases independently established PAYOUT-V1-D1 before correction. The corrected payout service/allocation unit suite passed 57 cases. The focused PostgreSQL run passed 68 cases: 33 provider-cost policy histories and 35 destination cases. PAID monetary/routing/reference rows remain unchanged, unchanged OWNER fees are not charged twice, NONE/EEMA historical provider costs cannot manufacture limited-recourse recovery, and true OWNER reversal still records net recovery plus explicit provider cost.

- `mvn test`: PASS — 997 executed, 0 opt-in skips, 997 reported; no failures/errors.
- `mvn verify -Ppostgres-integration`: PASS — 784 executed, 8 opt-in skips, 792 reported; no failures/errors.
- `mvn clean package`: PASS — 997 executed, 0 opt-in skips, 997 reported; no failures/errors.

The frontend typecheck/lint/build checks remain green. The final correction is backend-only with no DTO or interaction change; prior PAYOUT browser evidence and the 12-check rebuilt DEST browser run remain applicable. No schema, collection, netting or automatic money movement was added. Sanitized current totals are in `evidence/payout/v1-policy/`; the initial policy totals are retained under its `history/pre-paid-history-fix/` directory.

PAYOUT is locally complete and DEST-001–004 is complete; see the DEST report for its separate four fixed defects and final evidence. **NEXT UNTESTED DOMAIN: DOC-001–004.** Automatic continuation stops because PRICE-DEF-005 still lacks the zero-price checkout decision that affects receipt eligibility. That decision was not resolved by the user's provider-cost policy.

Final shared-document verification: regenerated 637-page PDF; all 703 scenario IDs/order preserved. Visually reviewed pages 1 and 83–90, including the corrected policy/history, DEST scenarios and adjacent DOC transition, with no clipping or overlap. SHA-256 is recorded in `evidence/payout/document-check.json`. Temporary DEST frontend and Chrome profiles were stopped/removed. Backend and frontend final `git diff --check` passed.

**Subsequent PRICE resolution (2026-09-19):** The zero-price continuation decision referenced above is now resolved under the user-approved Eema v1 policy. Final totals below EUR 0.01 are rejected before booking persistence/payment creation, with defensive stored-price checkout validation and V13 enforcement. See the PRICE execution report for fresh regression evidence; the prior stop remains historical.
