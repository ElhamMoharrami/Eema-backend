# PRICE execution report

**Updated 2026-09-19: PRICE-001–011 remain 11 PASS, 0 FAIL, 0 BLOCKED. PRICE-DEF-005 is RESOLVED under the user-approved Eema v1 policy: free bookings are unsupported; final booking price below EUR 0.01 is rejected.** The 2026-09-15 execution and then-unresolved decision are preserved as historical evidence below; the final addendum supersedes that decision status.

## Authoritative architecture before changes

BookingPriceCalculator is pure BigDecimal/LocalTime math. Duration is Duration.between(start,end).toMinutes (wall-clock, same day); billing minimum changes billable minutes, not selected duration. Hourly total uses raw input rate × billable /60 rounded HALF_UP to2 decimals. Rate and optional cap are separately normalized HALF_UP to2; final price is min(rounded hourly total, normalized cap). Cap changes money only. Java does not use floating-point money. Frontend lib/pricing.ts currently computes unrounded Number totals and caps before any rounding; Intl formats for display. Audit cent parity before fixing.

One backend venue-price formula exists. BookingService uses it at normal request creation and inquiry acceptance. Downstream PaymentCreationCoordinator claims stored venuePriceEur; PaymentService sends that claim to HttpMollieProviderClient; no checkout amount DTO. BookingFinancials commission rounds stored price × snapshotted percentage /100 HALF_UP. Paid processing uses stored booking economics. Cancellation uses totalPaidEur and snapshotted percentage/retained amounts, not current venue rates. OwnerPayoutService uses stored retained/commission/provider-cost/payout fields. Receipt renderer reads booking monetary snapshots; venue name/neighbourhood/timezone remain live metadata. Ordinary matching/direct quote describes hourly/minimum/optional cap, not a total without duration. No legacy dayRateEur/day_rate_eur formula found (full_day_rate_eur is the supported cap).

Venue/booking money columns are NUMERIC(10,2), max99,999,999.99. Rate/cap positive; booking price permits zero; required snapshots nonnull. DTO rate/cap0.01..99,999,999.99, minimum1..10,080; no Digits constraint. Fractional-cent input is accepted and PostgreSQL rounds on persistence, but immediate entity/DTO may still contain raw precision: reproduce. Positive0.01/hour×1minute rounds0.00; checkout currently has no positive-total guard. No free-booking/product policy is assumed. Rate maximum × long billing minimum may overflow the booking price column: reproduce safe-boundary behavior before fix.

Normal request loads a coherent Venue entity without row lock; owner update locks row and writes a single entity state. Inquiry acceptance holds venue lock (INQ fix). Probe concurrent price edits through actual persistence boundaries; either whole old or whole new configuration is valid, mixed snapshots are not.

## Inventory before implementation changes

All PRICE IDs are P0 calculator mathematics, requiring no actors/auth/policies/email. Existing BookingPriceCalculatorTest has4 tests with broad minimum/cap cases but not this canonical matrix. BK/INQ PostgreSQL suites already cover snapshots and mocked checkout; cancellation, payout and document unit/integration coverage are reused for downstream economics. Missing: shared differential fixture, precision edge cases, exact HTTP snapshot rows, coherent price-edit races and representative browser pricing.

| ID | Selected min | Billing min | EUR/hour | Cap | Billable min | Expected EUR |
|---|---:|---:|---:|---:|---:|---:|
| PRICE-001 |30|60|100|null|60|100.00|
| PRICE-002 |60|60|100|null|60|100.00|
| PRICE-003 |90|60|100|null|90|150.00|
| PRICE-004 |270|60|100|null|270|450.00|
| PRICE-005 |480|60|100|500|480|500.00|
| PRICE-006 |30|240|100|200|240|200.00|
| PRICE-007 |61|1|19.99|null|61|20.32|
| PRICE-008 |1|1|0.30|null|1|0.01|
| PRICE-009 |299|1|100|500|299|498.33|
| PRICE-010 |300|1|100|500|300|500.00|
| PRICE-011 |301|1|100|500|301|500.00|

Expectations independently checked using exact decimal arithmetic, then compared to source formula. Map all11 to one shared-fixture Java suite and one frontend client suite. Representative API/PostgreSQL cases use active verified/current-policy fixture actors only to reach snapshot creation; these are not prerequisites for pure pricing. Cleanup: disposable Testcontainers PostgreSQL, unique fictional QA venues/users, isolated Chrome and temporary auth config; no shared-history deletion/fake snapshot restore. No live Mollie/Resend calls.

## Executed contract and authority

The canonical eleven examples all matched the original Java arithmetic before implementation changes. The business formula was preserved. Rate/cap cents are now normalized at the venue write boundary to match the existing PostgreSQL representation. The frontend uses bounded integer cent × minute arithmetic with positive HALF_UP rounding; the largest permitted product is below JavaScript's exact-integer limit. No decimal dependency was introduced. Its displayed locale uses comma decimals (`€ 20,32`); provider strings use `20.32` without grouping or locale formatting.

| Layer | Authority / validation |
|---|---|
| Owner configuration | Rate/cap 0.01..99,999,999.99; optional cap; minimum 1..10,080; fractional cents accepted then normalized HALF_UP to stored cents |
| Preview | Actual `lib/pricing.ts`, indicative only; exact cent parity for all 92 shared fixtures; invalid/unsupported amount has no preview |
| Normal booking | Calculator called at REQUESTED creation; selected and billable durations remain distinct; no payment creation |
| Inquiry conversion | Current configuration at OFFERED acceptance; direct AWAITING_PAYMENT snapshot; offer itself does not lock a price |
| Existing booking | Rate, minimum, cap, durations, price and commission snapshot survive venue edits and owner acceptance |
| Checkout | No amount parameter. Adversarial body keys `amount`, `price`, `venuePriceEur`, `total` have no influence; adapter receives stored booking amount |
| Mollie transport | Mocked actual HTTP adapter sends EUR values `100.00`, `20.32`, `0.01`, `500.00` using `toPlainString`; no live payment |
| Commission | Stored booking price × stored percentage /100, HALF_UP cents; e.g. 20.32 ×20%=4.06 |
| Cancellation | Saved total paid, cancellation policy/deadlines and commission percentage; 100%/50%/0% refund examples yield 20.32/10.16/0.00; retained economics determine commission |
| Payout | Existing OwnerPayoutService coverage and source use retained/gross stored economics, stored commission and reconciled provider cost; current venue price is never reconstructed |
| Receipt | Real PDF renderer/extracted text asserts saved 19.99 hourly, 1-minute minimum, 61 selected/billable minutes, 500 cap, 20.32 price/paid after current venue price changes |
| Matching | Current hourly/minimum/optional cap only before duration; no authoritative total or reservation claim |

### Parameterization, boundaries and persistence

`src/test/resources/pricing-cases.json` contains the canonical eleven plus 81 rounding/duration variants. Java and the actual transpiled TypeScript module consume the same test-only data. Expectations were independently generated with exact decimal HALF_UP arithmetic, not copied from the implementation. The additional regression .03/hour ×30minutes must round .015 to .02; the previous frontend displayed .01. No one-cent tolerance is used.

`PriceContractTest` covers 107 executions: 92 shared values, ten invalid-input variants, two DST dates, the cap/rounding order, the technical amount limit after capping, and a lightweight monotonicity/billable/cap invariant matrix. Sub-minute intervals now fail defensively. Same-day LocalTime subtraction means spring Amsterdam 01:30–03:30 costs 120 selected minutes although elapsed time is 60; fall costs 120 although elapsed time is 180. The browser selects local HH:mm and uses the same wall-clock interpretation. No overnight/multi-day or elapsed-time billing policy was added.

`PriceIntegrationTest` uses real PostgreSQL and actual controller/service calls. PRICE-001/006/007/010 persist exact price, hourly/minimum/cap snapshots, selected duration and billable duration; venue updates followed by owner acceptance and mocked checkout preserve the original tuple. Inquiry price changes before acceptance affect the newly created booking; changes afterward do not. Required snapshots reject NULL; venue zero values violate actual constraints; optional cap remains nullable. NUMERIC(10,2) precision/scale are inspected. API invalid configuration is rejected separately from defensive calculator validation and database checks.

Repeated races use latches/barriers, not sleeps. Normal creation pauses after reading the venue while an owner update commits: the snapshot remains the complete old configuration. Inquiry acceptance races the owner update under the existing venue lock: one whole configuration wins, never a mixed rate/minimum/cap/price. Each race runs three times per focused/full integration execution. No new production locking or scheduling policy was introduced.

### Side effects and scope

Pure math, adapter formatting mocks and document rendering have no lifecycle/provider effects. Real request/conversion fixtures retain existing BK/INQ after-commit notifications; local EmailService reports notifications disabled, so delivery is not claimed. The suite mocks Mollie and asserts no interaction outside explicitly exercised checkout tests. Pricing does not mutate availability, policy, trust, payment-success, cancellation or payout workflows. Existing downstream suites are reused; this pass does not claim a real payment/refund/payout execution.

## Defects and decisions

| ID | Severity | Expected / actual before fix | Reproduction and impact | Disposition |
|---|---|---|---|---|
| PRICE-DEF-001 | P0 financial preview integrity | Exact preview cents; Number arithmetic displayed a cent too little on valid positive inputs | .03/hour,30min,min1 → .015; old .014999999999999998 displayed .01 instead of .02. 63 raw-value disagreements/92, three displayed cent disagreements | Fixed: bounded integer-cent arithmetic; all 92 exact |
| PRICE-DEF-002 | P1 configuration consistency | Immediate saved DTO agrees with reload; fractional input returned raw decimals then PG rounded them | Create 19.999/hour,50.005 cap; immediate/reload differed and could change a quote | Fixed: normalize rate/cap at create/update before returning DTO, preserving existing storage semantics |
| PRICE-DEF-003 | P1 safe amount boundary | Unsupported numeric amount rejected as pricing error before persistence; DB overflow surfaced as misleading availability conflict | Valid maximum rate with minimum10,080 produced NUMERIC(10,2) overflow; API returned409 time unavailable | Fixed: calculator rejects final price over99,999,999.99 with safe400; cap applied first; no commercial maximum added |
| PRICE-DEF-004 | P1 interval boundary | Persistable positive selected duration; calculator accepted sub-minute interval with duration0 | 09:00–09:00:30 produced zero duration, incompatible with booking constraint | Fixed: reject under one whole minute; no overnight change |
| PRICE-DEF-005 | P1 accounting risk (previously unresolved) | Defined zero-price checkout policy required; valid config can create0.00 and adapter receives0.00 | .01/hour,min1,selected1 minute →0.00; real HTTP/PG creation, acceptance and mocked checkout reproduce | Resolved 2026-09-19: approved v1 rejects final prices below EUR 0.01 before persistence/payment creation. The prior open decision and successful zero-price mock reproduction remain historical evidence |

Initial failed probes were intentional reproductions, not final validation failures: Java sub-minute guard (1/106), PostgreSQL fractional response and overflow (2/2), and frontend cent differences. Sanitized evidence records these separately from final passing tests.

## WHAT ELI SHOULD TEST PERSONALLY

- Is hourly pricing and the billing minimum clear, including selected versus billable duration?
- Is the optional full-day cap understandable?
- Does the preview read as an estimate, and does the saved booking price feel authoritative?

## PRODUCT / ACCOUNTING QUESTIONS

- **Resolve zero-price handling before paid launch.** Current valid settings can round to zero; this pass deliberately does not invent a minimum charge or free checkout.
- Preserve accepted fractional-cent input with cent normalization, or explicitly reject fractional cents in future owner/API configuration?
- Confirm wall-clock billing on DST dates versus real elapsed minutes; changing this requires coordinated pricing/booking/UX policy.
- Inquiry offers do not lock a price. Decide whether later offer-price locking or explicit price-change acknowledgement is needed.
- Existing technical limits prevent numeric overflow; any narrower commercial price/duration limits require a product decision.

## STAGING PRICE TESTS STILL REQUIRED

- Real provider amount acceptance, EUR formatting and minimum-amount behavior using permitted staging/test transactions; local mocks prove only application payload authority.
- Deployed PostgreSQL migrations retain money precision/scale and required snapshot constraints; multi-instance price-update/request consistency.
- Deployed receipt, checkout and locale display against the saved amount; provider fee reconciliation/refund/payout delivery remain their respective domains.
- Existing booking lifecycle email delivery and production logs/metrics remain deployed checks, not pricing arithmetic tests.

## Local conclusion

PRICE-001–011 canonical mathematics and shared frontend parity pass. The existing business formula, billing minimum, cap and historical money authority remain intact. Wider paid-launch clearance is conditional on PRICE-DEF-005's zero-price accounting decision; it is not hidden behind a successful mock provider response. Deterministic tests, PostgreSQL and representative browser work are the assistant's responsibility, not manual follow-up for Eli.

## Coverage inventory and browser evidence

| Reused / added coverage | Responsibility |
|---|---|
| Existing BookingPriceCalculatorTest (4), BookingFinancialsTest (2) | Original minimum/cap and financial arithmetic contracts |
| Existing BookingServiceTest (18), BK/INQ PostgreSQL suites | State transitions, snapshot/security/policy history, payment-stage boundaries |
| Existing BookingCancellationPolicyTest (6), CancellationServiceTest (4), BookingCancellationCoordinatorTest (3) | Cancellation snapshots and retained/refund accounting |
| Existing OwnerPayoutServiceTest (10) | Payout derives stored economics; venue fixture deliberately has no price fields |
| Existing FinancialDocumentPdfRendererTest (2), FinancialDocumentServiceTest (6) | Historical document rendering and access/service behavior |
| Added PriceContractTest (107) | Canonical/differential table, invalid inputs, round order, DST, bounded invariant matrix |
| Added PriceDownstreamContractTest (8) | Actual adapter amount body, receipt after venue edit, three refund-tier money examples |
| Added PriceIntegrationTest (18) | Real HTTP/PG snapshot tuples, client amount containment, zero-price characterization, constraints, price-edit races |
| Added PriceBrowserIntegrationTest (1 opt-in test, 9 recorded assertions) | Actual production frontend and Spring/PostgreSQL pricing journeys |

The browser passed five real request journeys: selected30/min60 →100.00; selected61/rate19.99 →20.32; selected480/cap500 →500.00; selected90/rate100 →150.00; and stale150.00 preview followed by server-side rate110 →saved165.00. Each saved price survived reload. All request payloads omitted monetary fields. The owner form rejected zero hourly rate using native validation, persisted19.99/min1/no cap, and later saved optional cap500. Browser timezone was America/Los_Angeles while venue times remained Europe/Amsterdam. Screenshots were inspected; all identities are fictional.

One initial browser attempt used an incorrect harness assumption that every catalogue venue had a direct card on the home page. The page only exposes the first featured venue in that flow. The fixture was corrected to reconfigure one featured venue between cases; no application change was needed for that harness failure.

## Final validation totals

| Check | Result |
|---|---|
| PRICE-001–011 | **11 PASS / 0 FAIL / 0 BLOCKED** |
| `mvn test` | PASS — 796 tests, zero failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS — 530 discovered, 526 executed, zero failures/errors, four opt-in browser launchers skipped |
| Focused PriceIntegrationTest | PASS — 18; repeated again in the full PostgreSQL suite |
| PriceBrowserIntegrationTest opt-in | PASS — one actual browser test, nine recorded checks, five real booking requests |
| Frontend differential fixtures | PASS — 92 exact monetary cases plus nine technical/invalid-input guard assertions |
| `mvn clean package` | PASS — executable jar built, all 796 unit tests passed again |
| Frontend typecheck / lint / production build | PASS |
| Both repository `git diff --check` | PASS |
| Master plan / PDF | Eleven PRICE anchors preserved, all marked PASS; generated 651-page PDF, PRICE page visually inspected |

Each normal request/edit race and inquiry accept/edit race passed three repetitions in each of two PostgreSQL executions (12 races total). Browser launchers skipped by the normal profile are not counted as executed; the PRICE launcher was explicitly run separately. The first browser harness fixture failure was corrected and rerun successfully. Local software tests do not mark real provider acceptance or the zero-price product decision PASS.

Defect totals: **five findings; four fixed; one open accounting decision**. Backend changes are limited to safe calculator duration/amount bounds and venue rate/cap normalization; frontend change is confined to indicative pricing arithmetic. No dependency, migration, commission formula, cancellation tier, payout rule, booking commitment rule or inquiry price-lock policy was added. Pre-existing changes from earlier domains remain intact.

Final sanitized command totals and source hashes: [verification.json](evidence/price/verification.json). Screenshots and rerun commands: [evidence index](evidence/price/README.md). Master plan: [Markdown](../EEMA_COMPLETE_TEST_MASTER_PLAN.md) / [PDF](../EEMA_COMPLETE_TEST_MASTER_PLAN.pdf).


## Approved Eema v1 zero-price decision — RESOLVED

The user resolved PRICE-DEF-005: **Free bookings are not supported. A calculated booking total below €0.01 is rejected before booking persistence/payment creation.** Canonical PRICE-001–011 expectations and exact-minute/HALF_UP/cap arithmetic are unchanged. Prior unresolved-decision evidence is preserved under `evidence/price/v1-minimum/history/` and in the historical sections above.

Source audit before implementation: both BookingService creation paths obtain final price from BookingPriceCalculator before saving or publishing events. Inquiry acceptance invokes creation within its existing transaction and only then marks ACCEPTED. PaymentCreationCoordinator claims the stored amount, including durable retries, without recomputing venue pricing. Existing IllegalArgumentException/BadRequestException map to the standard HTTP 400 envelope; generic price validation has no dedicated machine code. Frontend normal request and inquiry error handling must display this existing response safely. Current DTO cap minimum 0.01 and cent normalization prevent a valid stored cap becoming zero; the final amount guard also covers defensive raw-cap edge cases.

Database audit: the available local development database has zero booking rows (aggregate-only inspection), and development seeds create no bookings. Original venue price remains the original positive snapshot even for cancellations/refunds; zero total-paid, retained amounts or payouts are separate legitimate fields. Add common V13 with a validated venue_price_eur >=0.01 constraint, leaving applied migrations unchanged. It must fail safely if another environment contains historical zeros; never clamp/delete/rewrite history. Fresh PostgreSQL migrations and Hibernate validation will execute in integration tests.

Strategy: add focused tests of zero rejection, exact one-cent acceptance including PRICE-008, inquiry rollback with unchanged OFFERED row, legacy checkout across NOT_STARTED/CREATING/CREATED, database enforcement across booking states, and no rows/events/notifications/inventory/provider effects. Keep calculator fixtures unchanged. Existing zero-price characterization will be archived and superseded by the approved rejection expectation (it is not a canonical PRICE scenario). Then run all required backend regressions/build. Inspect existing frontend error presentation; change it only if the new safe validation is hidden or misleading. Real Mollie minimum acceptance remains staging-only and is not inferred from local mocks.


### Resolution and verified behavior

Both independently added before-fix PostgreSQL assertions failed with actual success (normal request 201, inquiry acceptance 200). After correction, final amounts are checked immediately after the shared calculator and before save, policy snapshot, audit or notification. Inquiry acceptance rolls back with the entire OFFERED row unchanged. Checkout validates only the stored original amount before a new claim, retry or cached checkout response. Standard HTTP 400 carries the safe minimum-bookable-amount message; the response path identifies the booking for operational diagnosis. No unavailable-time or provider-failure error is substituted.

Eight new PostgreSQL cases pass: no-side-effect zero request (including unchanged availability), one-cent creation at rates 0.30 and 0.60, atomic inquiry rejection and successful one-cent retry, legacy zero checkout for all three creation states, and constraint rejection across every booking state. Three new focused service cases reject zero, negative and sub-cent corrupt snapshots before a claim/provider call. The canonical PRICE-008 arithmetic remains 0.005 HALF_UP →0.01 and creates a valid booking. Required complete regression reruns existing tests, but no canonical PRICE expectation or shared formula fixture was rewritten.

The isolated legacy checkout fixture temporarily removes only the new constraint, restores a positive value, then restores the validated constraint in finally. This simulates pre-migration/corrupt history without weakening production protection. No production/development historical row was edited. The local database used for aggregate audit was returned to its original stopped state. V13 intentionally fails on incompatible rows elsewhere; deployment must inspect such rows rather than auto-clamp/delete financial history.

Frontend production code is unchanged: InstantQuote and inquiry acceptance already render the safe backend validation through apiErrorMessage. A direct execution of the actual transpiled ApiError/error formatter preserves the exact new message. No free checkout or frontend pricing authority was added. Existing frontend build/browser evidence is retained; no fresh frontend build is claimed for this backend-only change.

- `mvn test`: PASS — 1000 executed, 0 skipped, 1000 reported, zero failures/errors.
- `mvn verify -Ppostgres-integration`: PASS — 792 executed, 8 skipped, 800 reported, zero failures/errors.
- `mvn clean package`: PASS — 1000 executed, 0 skipped, 1000 reported, zero failures/errors.
- `git diff --check`: PASS. Focused unit suite: 7 PASS; focused PostgreSQL suite: 8 PASS.

### WHAT ELHAM SHOULD TEST PERSONALLY

Review whether the minimum-bookable-total message is understandable when a tiny-rate preview displays EUR 0.00. No manual financial arithmetic, database or rollback checks are requested.

### PRODUCT / OPERATIONS DECISIONS

PRICE-DEF-005 is resolved exactly as approved. Intentional complimentary bookings remain a separate future product feature. Existing future-policy questions in the historical report do not alter current exact-minute/rounding/offer semantics. No new unresolved P0/P1 or pricing ambiguity was found.

### STAGING ONLY

Real provider amount acceptance and real payment/email delivery remain unverified locally. Inspect deployed historical original prices before applying V13; the migration never rewrites them. Fresh local migrations and Hibernate schema validation passed.

PRICE is locally green and the previous zero-price continuation blocker is removed. NEXT UNTESTED DOMAIN: DOC-001–004, authorized for continuation after this complete regression. Sanitized current evidence is under `evidence/price/v1-minimum/`; historical defect evidence remains intact.

PDF verification: regenerated 637 pages, preserved all 703 scenario IDs in order, visually reviewed pages 1, 73, 74 and 88. No clipping/overlap. Current checksum is in `evidence/price/v1-minimum/document-check.json`.

Subsequent continuation: DOC-001–004 executed after this resolution. Three scenarios pass; DOC-004 is blocked on DOC-DEC-001 (unsupported-name rendering policy). See [DOC execution report](DOC_EXECUTION_REPORT.md). PRICE remains green; INC has not started.

Subsequent DOC policy resolution: DOC-001–004 are now4 LOCAL PASS after the approved embedded Unicode-name implementation and full regression. The earlier DOC decision stop above is historical. See [DOC execution report](DOC_EXECUTION_REPORT.md). Next unfinished domain remains INC-001–002.
