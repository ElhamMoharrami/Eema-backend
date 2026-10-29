# EDGE execution report

**Current result (2026-09-20): 6 PASS / 0 FAIL / 0 BLOCKED. EDGE-DEF-001 and EDGE-DEC-001/002/003 resolved under the approved v1 policies. All required local validation gates passed. Historical failures and decisions remain below.**

Initial audit 2026-09-20, before EDGE test or production changes. SEC completed 7 PASS with all regression gates green. EDGE-001–006 remain NOT RUN and have no prior dedicated report. Prior complete-plan/report reading is reused; this domain reconciles earlier fixes rather than reopening their approved contracts.

| Scenario | Current source / reused evidence | Missing evidence |
|---|---|---|
| EDGE-001 | AVL fixed date carry and overlap lead clipping; PRICE rejects less than one whole minute. LocalTime Duration.toMinutes still truncates residual seconds. | HTTP/PG seconds/nanos and midnight carry; calculator guard regression |
| EDGE-002 | Java atZone shifts spring gaps forward and chooses earlier autumn offset; local wall-clock pricing, elapsed lead/cancellation/payout timing. AVL, PRICE, CANCEL and PAYOUT characterize separately. | Persisted request/confirmation across transition, potential nonpositive resolved interval; explicit product acceptance gap |
| EDGE-003 | VenueFilterBuilder applies hard SQL/evaluator filters, then Java limit 30, then availability. MATCH138 proves predicates and candidate bound; exact 31st-available case remains. | Reproduce 31 eligible/first 30 unavailable, retain product recall decision rather than silently change algorithm |
| EDGE-004 | VEN-D02 already checks ACTIVE under venue lock before acceptance. BK/AVL/VEN races passed in SEC full regression. | Small current HTTP atomicity assertion and link prior races |
| EDGE-005 | decline locks booking and checks REQUESTED only; accept and expiry worker enforce response deadline. BK explicitly characterized scheduler-lag decline but did not choose a new policy. | Before/equal/after deadline with persisted audit/notification distinction and worker-first/decline-first outcomes |
| EDGE-006 | VenueService getAllVenues/deleteVenue have no controller callers; actual public GET maps getAllActiveVenues. HeroFeature is currently imported by homepage; BookerVerification has no importing production component. Optional public Cala BFF exists separately. | Unsupported DELETE/generic mutation HTTP leaves state unchanged; call-site audit, no new API/UI |

Authoritative state: only AWAITING_PAYMENT/CONFIRMED reserve local half-open intervals. Booking rows persist local date/times (PostgreSQL microsecond precision) and monetary/contact/policy snapshots, with no timezone/offset snapshot. Confirmation resolves venue timezone for cancellation deadline snapshots; payout readiness resolves venue end time. Availability requires same-local-day ordered times. No overnight or occurrence selector exists. No universal email/policy prerequisite for public availability; existing booking actors/guards apply to mutations.

Corrections: generated midnight bug is already fixed; sub-minute acceptance assumption was superseded by PRICE-DEF-004; suspended acceptance assumption was superseded by VEN-D02; HeroFeature is not unused. Generated actor matrices and snapshot resets are replaced by endpoint-specific assertions and disposable PostgreSQL. No provider is required to characterize local transitions; payment transport will remain mocked.

Plan: reuse fixed-clock algebra, pricing, cancellation/payout boundary suites and previous browser evidence. Add only focused PostgreSQL cross-cutting state/precision tests and deterministic matching truncation proof. Record policy ambiguity separately from successful characterization; a green test that reproduces a known ambiguity does not make its launch scenario PASS. No production change until a reproduced defect has a clear existing intended rule.

## WHAT ELHAM SHOULD TEST PERSONALLY

Timezone and duration wording around clock changes; clarity of empty matching results and declined versus expired requests.

## PRODUCT / OPERATIONS DECISIONS

Candidates requiring evidence: acceptance of nonexistent/repeated local booking times; recall versus candidate cap; deadline semantics for overdue owner decline. Existing policies must not be guessed.

## STAGING ONLY

Real provider/email delivery and deployed timezone/edge behavior. Local fixtures do not certify these.

## First focused execution / historical evidence

141 reused timing unit cases passed (AvailabilityAlgebraTest, VenueBookingLeadTimePolicyTest, CancellationScenarioBoundaryTest, OwnerPayoutServiceTest). First PostgreSQL run:13 cases,11 pass,1 assertion failure and 1 fixture error. Synthetic provider amount 125.5 was correctly rejected because Mollie's contract requires 125.50; the fixture now formats two decimals. Fractional-time inspection used JdbcTemplate's convenience conversion, which can truncate fractional precision; a direct PostgreSQL text comparison is required to establish actual persisted precision. Neither failure justifies changing provider validation or inventing an input-time rule.

The first run independently established these current outcomes:

- Amsterdam 2027-03-28 02:30–03:00 is created, accepted and confirmed. Local duration is 30 minutes. Java resolves start to 01:30Z and payout end to 01:00Z: elapsed duration is minus 30 minutes. Cancellation deadlines use the later resolved start. This is not a valid launch acceptance proof merely because characterization assertions pass.
-31 hard-eligible venues, sorted by increasing hourly price; first 30 have no availability,31st does. Exact matching returns zero. The excluded venue independently passes the same availability filter. Only the parser transport is called, not the ranker; no booking is created.
-At response-deadline minus 1 microsecond, equality and plus 1 microsecond, owner decline returns 200/DECLINED and REQUEST_DECLINED. If the expiry worker executes first at equality, the booking is EXPIRED and subsequent decline 409. Both are atomic/idempotent; scheduler order decides terminal reason/notification under current code.

These findings require a product scheduling/deadline decision and an explicit acceptance of the matching recall limitation. No production policy has been changed. The final reservation-alias check and precision comparison are pending.

## Historical scenario accounting and stop decision

| ID | Result | Evidence and remaining issue |
|---|---|---|
| EDGE-001 | PASS | Fixed-clock midnight HTTP/DB carry, sub-minute 400 atomicity, seconds/nanos characterization. Selected/billable whole minutes unchanged; exact sub-millisecond persistence is not claimed |
| EDGE-002 | FAIL | EDGE-DEF-001 P0: two accepted local intervals alias to the same instants. Separate confirmed gap interval has negative resolved elapsed duration. EDGE-DEC-001 required |
| EDGE-003 | BLOCKED |31st available candidate is omitted, independently reproduced; EDGE-DEC-002 requires explicit acceptance or change of recall policy |
| EDGE-004 | PASS | Real ADMIN suspension followed by owner 409; row/audit/notices unchanged, no provider; prior lock races reused |
| EDGE-005 | BLOCKED | Exact deadline 3-point matrix and both serialized orders prove scheduler-dependent terminal reason; EDGE-DEC-003 required |
| EDGE-006 | PASS | Unsupported DELETE405 and invented mutation 404 are side-effect free; audited call sites and existing refund route |

**3 PASS / 1 FAIL / 2 BLOCKED. EDGE is not locally clear to proceed.** No EDGE production fix or migration was made. The precise DST acceptance policy remains ambiguous and the instructions prohibit silently resolving product decisions. OPS and subsequent domains have not started.

### Historical EDGE-DEF-001: P0 reservation integrity, unresolved at initial execution

Independent PostgreSQL/API reproduction: one ACTIVE Amsterdam venue with 00:00–06:00 weekly availability, date 2027-03-28, fixed Clock 2027-03-14. Scout A requests 02:00–02:30; Scout B requests 03:00–03:30. Both owner accept calls return 200 and both persisted rows are AWAITING_PAYMENT. The production lead-time resolver maps both ranges to 01:00–01:30Z. The database exclusion constraint uses `tsrange(shoot_date + start_time, shoot_date + end_time,'[)')`, so these disjoint local ranges do not conflict. This is an observed local reservation alias, not a claim that a live customer was double charged or that a real payment occurred.

The separate 02:30–03:00 confirmation reproduction gives a positive 30-minute wall-clock charge while payout end precedes resolved start by 30 minutes. Autumn 02:30–03:30 gives 60 wall-clock minutes and 120 elapsed minutes with the earlier start offset. The approved wall-clock pricing formula must remain unchanged; fixing temporal acceptance is not permission to introduce elapsed-time billing, overnight bookings or an occurrence selector.

The P0 is reproducible without a concurrency race. Existing local-range exclusion and venue-lock races remain effective for their represented ranges; they cannot resolve two different local ranges that alias after timezone normalization. Characterization assertions deliberately preserve the observed state for decision review and must be replaced/extended with approved rejection/normalization expectations when the policy is resolved.

### Fractional-time fixture correction

The second run directly queried PostgreSQL `start_time::text` and confirmed that this Hibernate/JDBC TIME binding persists milliseconds, even though PostgreSQL TIME itself supports microseconds. The initial assertion wrongly assumed the ORM preserves all six input fractional digits. The final characterization explicitly checks the observed millisecond values and unchanged whole-minute duration; no higher-resolution guarantee, minute-only API ban or production truncation rule was added. The frontend uses HH:mm controls. High-resolution API round-trip fidelity is a known representation limitation, not certified by EDGE-001.

## Historical PRODUCT / OPERATIONS DECISIONS — required before initial continuation

1. **EDGE-DEC-001 (P0, required):** choose how v 1 rejects or deliberately represents nonexistent spring times, and approve the repeated-autumn-time convention and wording. Current implicit normalization is not safe for reservations. A decision must cover normal request/inquiry acceptance, availability presentation, historical handling and consistency of lifecycle instants while preserving wall-clock pricing. Do not silently add an offset-selection product or change billing.
2. **EDGE-DEC-002 (P1, required):** is missing the only available 31st candidate acceptable in v 1, or must matching search beyond 30 hard candidates until the intended available set is found? Current empty-result UI does not reveal candidate truncation. This is an existing recall/performance limitation, not hard-filter relaxation; no database/search redesign is approved by this report.
3. **EDGE-DEC-003 (P1, required):** for still-REQUESTED rows at/after ownerResponseExpiresAt, should decline remain DECLINED or atomically become EXPIRED like acceptance/worker expiry? Choose event/notification semantics explicitly. No new status, reminder or deadline cap is needed merely to make that choice.

## WHAT ELHAM SHOULD TEST PERSONALLY

After temporal policy resolution, assess timezone/clock-change wording and the meaning of selected versus elapsed duration. Assess whether empty search feedback communicates its limits, and whether declined versus expired wording matches the selected policy. Do not manually reproduce races, time arithmetic or database exclusions; automated evidence already records those.

## STAGING ONLY

Real lifecycle email delivery, deployed worker/edge behavior and real provider transport remain unverified. The Cala compatibility route is reachable in source independently of its unused component; its provider/privacy assessment is tracked by CALA-001 and was not executed or certified in EDGE. No external API was called for this domain.

## Validation scope and reused complete regression

Immediately before EDGE, the unchanged production tree passed SEC's complete gates: `mvn test`1,032; `mvn verify -Ppostgres-integration`958 reported/946 executed/12 opt-in skips; `mvn clean package`1,032. Frontend typecheck/lint/build and both diff checks passed. All prior domain integration suites, including ownership, financial/inventory races, matching 138, VEN154, BK37, DOC47 and ADMIN47, ran in that regression. These are explicitly reused results, not claimed as a new complete run after adding EdgeIntegrationTest.

EDGE adds only its PostgreSQL characterization class and QA documentation. It reruns 141 targeted timing units and a fresh PostgreSQL focused suite; all 13 common migrations and Hibernate validation passed again. No frontend production change or new browser journey was necessary to establish the blockers. Prior AVL/BK/MATCH browser evidence is reused for minute controls, venue-zone labels, authoritative refetch and empty results; SEC's fresh 6 browser checks and 8 DOC checks remain the latest executed browser regression. No new full regression/build is represented as passing after the EDGE additions, and the domain is not declared complete. Its unresolved P0/decisions require stopping rather than advancing on green characterization counts.

**NEXT UNTESTED DOMAIN: OPS-001–009** (operations, migration/reset/restore and recovery boundaries), not started. The first unfinished domain remains EDGE until its P0 and decisions are resolved.

## Final evidence closeout

Final focused PostgreSQL characterization:14 tests, zero failures/errors/skips;141 focused timing units also passed. These counts prove reproducibility of current behavior, including the unresolved P0, and do not change the scenario FAIL/BLOCKED statuses. Historical initial runs remain in evidence/edge/history. No production or frontend implementation changed during EDGE. The master plan preserves all 703 scenario IDs in order and records the stop gate; final PDF metadata and visual review are in evidence/edge/document-check.json. Both repositories pass whitespace checks. Only synthetic local fixtures were used; no real provider operation or deployment occurred.

## Approved v1 resolution — initial audit before implementation

The user has now resolved EDGE-DEC-001/002/003. Earlier failures, decisions and characterization are historical and retained above and under evidence/edge/v1/history. Implementation and final execution remain pending at this audit point.

Approved contract: reject local gap endpoints rather than normalize; explicitly choose the earlier overlap offset; require a positive resolved interval; use one temporal policy across commitments, lead time, availability, confirmation/cancellation and payout. Pricing remains wall-clock minutes. Availability must exclude gap portions. Matching queries bounded, deterministically ordered batches until 30 eligible available candidates or exhaustion; no semantic total-candidate cap. Both owner decisions expire at equality/after the response deadline, with one committed expiry event/notification and repeat/job idempotency.

Source audit identifies conversion sites in lead-time policy, inquiry offer expiry, confirmation cancellation snapshots and payout eligibility. Normal booking creation uses BookingAvailabilityValidator; inquiry-derived booking bypasses ordinary availability and needs its own interval validation. Inquiry create/offer/accept must share the policy. Availability clipping must use the same resolution rules. Existing booking decline already locks the booking row but omits the acceptance deadline guard/noRollbackFor exception. Matching Criteria query currently materializes every predicate match before Java limit; move limits/order including UUID tie-breaker into SQL, then continue batches in the matching service.

Coverage plan: pure temporal boundaries including normal, gap, overlap, positive duration and wall-clock pricing; real PostgreSQL request/inquiry/snapshot/availability and repeated DST safety; deadline minus/equal/plus microsecond plus deterministic decline/worker serialization races; matching 31st candidate, enough first batch, exhausted all-unavailable, ties/duplicates and provider input membership. Reuse prior authorization/financial suites. Verify existing frontend409 refetch and actionable backend error handling before deciding any UI changes. No migration or new scheduling/search feature is planned. Full required regression gates follow focused evidence; OPS starts only after local completion.

### Implementation and focused execution history

The approved implementation centralizes valid-offset resolution in `BookingTimePolicy`. Direct requests and inquiry create/offer/accept reject gap endpoints, and every booking commitment validates positive resolved elapsed duration. Availability splits weekly/AVAILABLE intervals around nonexistent local portions. Legacy/corrupt gap rows fail acceptance/checkout without reservation/payment effects. Confirmation validates the full interval before financial snapshot mutation; cancellation and payout use the same explicit resolver. Existing wall-clock price calculation is unchanged.

Matching now retrieves at most 30 SQL rows per query and advances by the raw database batch length, preserving all hard predicates and the Java eligibility check. It collects at most 30 distinct available venues before ranking. Ordering is price ascending, size descending, name ascending, UUID ascending; UUID is the unique continuation tie-breaker. No full catalogue materialization or new search infrastructure is introduced.

Decline uses the existing acceptance-style no-rollback deadline exception: before deadline DECLINED; equality/after EXPIRED, commit then409. The locked mutation emits only the applicable terminal event. The expiry scheduler remains cleanup. Frontend changes are explanatory timezone copy only; backend error display, interval eligibility and stale-conflict refetch are reused.

Initial implementation validation found and corrected an over-broad interval check in the start-only cancellation snapshot helper (legacy unit fixtures omit endTime); the full interval check belongs in booking confirmation, while the snapshot helper resolves its start through the central policy. The normal validator retains its existing invalid-zone error mapping. Updated tests replace the previous implicit-gap characterization and correct a new assertion to the existing BadRequestException convention. Final focused units: 174 passed. First PostgreSQL approved-policy run:25 cases,23 passed; both failures exposed the missing UUID ordering tie-breaker, now added. All repeated gap/inquiry/deadline cases passed in that run. Sanitized initial failures remain under `evidence/edge/v1/`; final rerun and full regression are pending.

The second PostgreSQL run passed all 25 cases after the UUID ordering fix. The first browser attempt correctly committed expiry but used a nonexistent label in its assertion; the existing UI says “Response window expired”. The test now asserts that existing wording, with historical failure retained. No product change was made for this harness correction. Frontend typecheck, lint, production build and whitespace check passed after the two explanatory copy edits.

### Verified focused resolution

All 25 EDGE PostgreSQL cases now pass. These include three repetitions each of direct gap rejection/legacy acceptance-checkout protection and inquiry gap rollback, plus three repetitions of the deadline race test, each forcing both transaction orderings (six races). Deadline minus/equal/plus1microsecond proves the exact boundary. Exactly one committed REQUEST_EXPIRED and no REQUEST_DECLINED occurs in each at/after-deadline race. Repeated worker/commands leave the same terminal state and audit count. Provider transport remains untouched.

Autumn confirmation proves wall-clock60 minutes, real elapsed120 minutes, earlier-offset start, cancellation deadlines and payout eligibility; inquiry acceptance proves the same earlier offset for offer expiry. Weekly and all-day AVAILABLE rows exclude gap portions. Before-gap availability ends immediately before the nonexistent boundary; frontend HH:mm displays01:59, followed by03:00. A selection must fit one returned continuous interval. Unit coverage also checks Lord Howe's half-hour transition and Apia's skipped date.

Within the 25-case EDGE suite, matching checks include independent available-candidate31 proof,61 tied candidates across bounded queries with no duplicates, enough available venues in the first batch (no offset30 query), exhaustion of61 unavailable candidates (queries0/30/60), and captured ranker input membership. No unavailable venue is passed to ranking. The legacy first-batch helper remains for callers/tests; production matching uses continuation.

The real frontend/HTTP browser launcher passed with four checks: stale decline409 commits/refetches EXPIRED and retains conflict feedback/removes controls; reload remains EXPIRED; returned availability excludes the gap and manual out-of-interval input disables booking; inquiry submission shows the actionable backend gap message without success. Its Java closeout verifies EXPIRED plus only REQUEST_SUBMITTED/REQUEST_EXPIRED, no inquiry persistence and no payment-provider interaction. All three screenshots were visually reviewed: labels, interval boundaries and error text are legible, with no overlapping controls. Browser evidence contains only synthetic local fixtures. The test clock endpoint exists only in the opt-in test configuration.

### WHAT ELHAM SHOULD TEST PERSONALLY — approved v1 resolution

Assess whether “first occurrence” is understandable on the calendar/inquiry form, whether the clock-change rejection clearly tells a scout to choose another time, and whether “Response window expired” plus retained conflict feedback makes a stale owner decision understandable. Review these on a narrow/mobile screen for wording and flow. Database races and arithmetic are automated and need no manual reproduction.

### PRODUCT / OPERATIONS DECISIONS — approved v1 resolution

EDGE-DEC-001/002/003 are explicitly approved by the user. No new product, business or legal decision was introduced by this implementation. The existing current-venue-timezone model and local-range database constraint are retained; no historical data rewrite or offset-selection feature was added. Final defect closure and automatic continuation await the required complete regression.

### STAGING ONLY — approved v1 resolution

Local tests mock provider transports and verify committed lifecycle event semantics; they do not prove actual email delivery, Gemini quality/latency, deployed multi-instance worker behavior or real payment transport. These remain staging scope. No staging result is counted as a local PASS.

The first complete unit run passed 1,040 tests. Before completing the cross-domain PostgreSQL run, harness review identified that the nested browser clock controller needed explicit `@TestComponent` isolation from normal Spring scans; it remains explicitly imported by the opt-in browser class. The in-progress regression was deliberately stopped for that test-only correction. A subsequent foreground browser process terminated during startup without a test outcome, so it is not counted as validation. The isolated harness and complete backend gates are rerun sequentially in a persistent local process. No production behavior changed for either harness issue.


## Approved v1 final closeout

| Scenario | Result | Final evidence |
|---|---|---|
| EDGE-001 | PASS | Sub-minute rejection, milliseconds characterization and fixed-clock midnight date carry |
| EDGE-002 | PASS | Central gap rejection/earlier overlap, positive resolved duration; three repeated direct/legacy checks and three inquiry rollback checks; unchanged wall-clock pricing and consistent lifecycle instants |
| EDGE-003 | PASS | Available candidate31 included; bounded batches, early stop, exhaustive unavailable fixture, UUID ties/no duplicates and ranker membership |
| EDGE-004 | PASS | Suspended acceptance409 atomicity plus reused full-regression lock races |
| EDGE-005 | PASS | Minus/equal/plus1microsecond deadline, six forced serialized races, one terminal expiry event and browser authoritative refetch/reload |
| EDGE-006 | PASS | Unsupported mutation routes remain unavailable and side-effect free |

**Defects and decisions:** EDGE-DEF-001 P0 is resolved: nonexistent local intervals cannot become blocking reservations by implicit normalization. EDGE-DEC-001 resolves DST rejection and explicit earlier autumn occurrence; EDGE-DEC-002 resolves complete useful recall through bounded queries; EDGE-DEC-003 resolves the shared acceptance/decline deadline and scheduler-independent reason. The UUID tie-breaker defect found during implementation is fixed and its failures retained. No unresolved EDGE P0/P1 defect or product/business/legal decision remains. No migration or new architecture was introduced.

**Validation:** focused units174; focused PostgreSQL25, all passed. Opt-in browser launcher1 passed with4 browser checks, including after explicit test-component isolation. Complete `mvn test`: 1040 tests, zero failures/errors/skips. Complete `mvn verify -Ppostgres-integration`: 984 reported, 971 executed, 13 opt-in skips, zero failures/errors. `mvn clean package`: 1040 tests, zero failures/errors. PostgreSQL16 fresh V1–V13 migrations and Hibernate validation passed. Frontend `npm run typecheck`, `npm run lint`, `npm run build` and both repositories’ `git diff --check` passed. Required complete regression reran the earlier domain suites; focused tests did not replace them. Provider transports remain mocked.

**Documentation/evidence:** this report, approved EDGE scenario wording and G11/G12/G13 in the master plan; original blocked report/master/results in `evidence/edge/v1/history`; focused/full summaries, browser checks/screenshots and frontend/visual records in `evidence/edge/v1`. All703 scenario IDs remain in order. PDF regeneration/visual metadata is recorded in `evidence/edge/v1/document-check.json` after rendering.

**WHAT ELHAM SHOULD TEST PERSONALLY:** clarity of first-occurrence wording, actionable clock-change errors, expired-request feedback and narrow-screen usability. **PRODUCT / OPERATIONS DECISIONS:** none outstanding for EDGE; all three supplied decisions are implemented. **STAGING ONLY:** actual provider/email delivery, Gemini behavior and deployed multi-instance/edge operation remain unverified by local tests.

**Locally clear to proceed: YES. NEXT UNTESTED DOMAIN: OPS-001–009.** Automatic continuation is authorized by the user; OPS must receive its own source audit, scenario correction and report before implementation.

Final EDGE document verification: 625-page PDF regenerated; all703 scenario IDs and order match extraction. Pages97–99 and616 visually reviewed without clipping/overlap. Hash recorded in v1/document-check.json. Packaged application verified to exclude the browser test controller. Both repository whitespace checks passed.
