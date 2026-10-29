# Core booking execution report

Audit recorded before BK code/test changes, 2026-09-14. Local software scenarios: **8 PASS / 0 FAIL / 0 BLOCKED** after two P2 fixes. Human/operations decisions and staging delivery are listed separately. All required local build checks are complete; see the execution ledger.

## Source-authoritative audit and inventory

| ID | Actual contract / correction | Existing coverage | Missing focused execution |
|---|---|---|---|
| BK-001 | SCOUT creates REQUESTED, 201. Selected duration differs from billable minimum. Request contact is supplied by scout; owner contact comes from owner profile. Four historical policy references, excluding Privacy. | BookingServiceTest; BookingPriceCalculatorTest; PolicyIntegrationTest; VEN snapshots | PostgreSQL 270-minute/minimum snapshots, profile/pricing replacement then acceptance |
| BK-002 | Verified email and current policies; ACTIVE venue; ordered interval, effective availability and lead time. ADMIN cannot create. Non-ACTIVE 404, malformed 400, unavailable/lead 409, role/trust 403, anonymous 401. Past dates encounter lead validation first (409). | BookingAvailabilityValidatorTest; VerifiedUserPolicyTest; POL/VEN/AVL integration | Parameterized HTTP failure atomicity |
| BK-003 | Multiple REQUESTED overlap without reservation. | AVL PostgreSQL status/exclusion matrix | Exact/partial/contained request coexistence and events |
| BK-004 | Response window defaults 24h, not capped to shoot start. Equality expires. Acceptance commits EXPIRED before 409. | BookingServiceTest; PostgresCriticalPathIntegrationTest SKIP LOCKED workers | Fixed-clock boundary and persisted repeated expiry |
| BK-005 | Owning OWNER **or ADMIN** can accept. Payment deadline defaults 24h after acceptance, not capped. Half-open overlapping REQUESTED become UNAVAILABLE. No provider payment yet. | AVL repeated concurrent acceptance; BookingServiceTest | Full competitor matrix, exact payment deadline, snapshot retention |
| BK-006 | ACTIVE, lead time, availability rechecked on accept. Suspension/blocked availability leave REQUESTED; lead failure commits EXPIRED. | VEN suspension races; AVL lead expiry and checkout exemption | Traceable shared coverage plus actor checks |
| BK-007 | Owning OWNER or ADMIN declines only REQUESTED; no deadline/venue/lead revalidation in decline. Duplicate conflicts. | BookingServiceTest | All invalid states and denied actor persistence |
| BK-008 | SCOUT list own; OWNER list venue bookings; ADMIN ordinary list all. Participants/admin detail allowed, unrelated 403, missing 404, malformed 400, anonymous 401. | BookingServiceTest; AdminBookingSearchIntegrationTest | Ordinary HTTP list/detail privacy matrix |

Unit fixtures require no actors/providers. HTTP workflow uses fictional unique accounts, verified email and current policies for SCOUT/OWNER; ADMIN is exempt from email/policy gate. Owner verification is not directly checked by booking endpoints. Fixtures supply ACTIVE venues; moderation prerequisite coverage belongs to VEN. No fake snapshot-reset harness: disposable Testcontainers PostgreSQL, unique RUN_ID fixtures, isolated Chrome profile; no shared booking history deletion.

## State and authority

REQUESTED → AWAITING_PAYMENT / DECLINED / EXPIRED / UNAVAILABLE / CANCELLED.
AWAITING_PAYMENT → CONFIRMED / EXPIRED / CANCELLED.
CONFIRMED → CANCELLED / REFUNDED. Other states are terminal.

Only AWAITING_PAYMENT and CONFIRMED reserve inventory, consistently with Java reservation policy, availability subtraction and PostgreSQL half-open exclusion. Acceptance locks venue then reloads/locks booking, flushes reservation before resolving competitors. No owner post-payment confirmation. Cancellation/payment transitions are referenced, not exercised as provider lifecycles in BK.

Price, selected/billable duration, commission, participant contacts and four policy references are historical. Acceptance does not recalculate price. Matching is not a booking prerequisite. Reads do not require current policy acceptance. Booking DTO is shared between participants and ADMIN, including financial snapshot fields; it does not serialize User or bank credentials.

Notifications use after-commit asynchronous listener with contained failures. REQUEST_SUBMITTED targets scout and owner; acceptance, decline, request expiry and unavailable target scout. Stale notifications are suppressed if authoritative status has already changed. Local provider delivery is disabled; real delivery remains staging-only. No owner reminder/escalation exists.

## Audit observations to reproduce

- BookingActions refetches acceptance conflicts but not decline conflicts.
- Request-expiry UI and email blame owner response deadline even when the lead-time rule expired the request before that deadline.
- BookingService currently reads wall clock directly; fixed-clock core workflow seam is needed for deterministic equality tests.

## Corrections and narrow changes

Generated duplicate API/DB paragraphs and generic actor language were replaced for all eight IDs. Current ADMIN response authority is explicit; no ADMIN creation. Past dates are 409 through lead-time validation, not automatically400. Price tests distinguish selected270 from billed270/360. Booking creation already creates a request record, so the former expiry email “No booking was created” was also inaccurate. No cleanup deletes shared history.

| Defect | Severity | Expected / actual reproduction | Fix / impact |
|---|---|---|---|
| BK-D01 | P2 | Real browser loads REQUESTED; separate owner action declines; stale UI decline409 did not refetch and retained action buttons. Acceptance refetched but hid its error when controls disappeared. `evidence/booking/browser-before.json` and screenshots reproduce. | Both review409 paths refetch, retain a safe message, and handle refetch failure. No locally forced status. |
| BK-D02 | P2 | Lead-time expiration emits REQUEST_EXPIRED before the response deadline (existing AVL and focused PostgreSQL reproduce); UI/email wrongly asserted the owner missed the response deadline. | Neutral request-expiry copy covers both causes, distinct from declined/unavailable/payment-expired. Email says no reservation/payment, rather than no booking record. No new expiry policy. |

BookingService now uses the existing injected application Clock for request creation, acceptance and transition timestamps, enabling exact Instant boundary checks. This is a testability change with the same elapsed-time/deadline rules. Existing VEN suspension enforcement is preserved, not claimed as a new BK fix. Provider payment processing/pricing/cancellation rules are unchanged.

## Snapshot and read privacy findings

- Price source is BookingPriceCalculator at request creation. `durationMinutes=270`, billable270 with minimum60 and billable360 with minimum360. Hourly/minimum/full-day, venuePriceEur, commission percentage/amount and contacts survive later venue edits and acceptance. Checkout coordinator claim uses stored venuePriceEur; no live Mollie request is made in this check.
- Commission is immutable application configuration during this process, not a runtime editable setting. Test compares creation snapshot with configured percentage and verifies acceptance retains it; no fabricated hot-reload behavior claimed.
- Scout name/email/optional phone come from supplied `scoutContact`; owner name/email/phone from live owner when request is created. Live user name/phone/email changes do not rewrite historical snapshot fields. Contact delivery uses those historical recipients.
- Four policy reference rows: TERMS_OF_SERVICE, ACCEPTABLE_USE_POLICY, PRODUCTION_POLICY, INSURANCE_REQUIREMENTS. All five account acceptances are required; PRIVACY_POLICY is excluded from booking references. Replacement makes new mutations stale but historical detail and references remain available.
- Ordinary DTO includes both participant contact snapshots and financial snapshot fields, including commission/provider-state fields. Participants/admin receive the same shape. No User entity/password/token/IBAN/encrypted payout data is serialized. Immediate participant contact visibility and retention remain product/privacy review matters; no unrelated data minimization policy was invented.

## Authorization and mutation outcomes

| Actor | Create request | Accept/decline | List/detail |
|---|---|---|---|
| Verified, policy-current SCOUT | 201 if domain conditions hold |403 | Own history200; unrelated403 |
| Verified, policy-current current venue OWNER |403 | Own venue200 only REQUESTED; unrelated403 | Own venue histories200; unrelated403 |
| Active ADMIN |403 | Explicitly permitted200, no policy/email prerequisite | All ordinary histories/detail200 |
| Unverified or policy-stale marketplace actor |403 mutation |403 mutation | Historical reads remain actor-scoped |
| Anonymous |401 |401 |401 |

Malformed input/UUID400, missing venue/booking404, non-ACTIVE creation404, well-formed unavailable/too-soon409. ACTIVE and effective availability are rechecked on acceptance. Blocked override or suspended venue leaves REQUESTED on409; minimum lead time commits EXPIRED with reason MINIMUM_LEAD_TIME_NOT_MET before409. Another successful acceptance makes true overlapping REQUESTED unavailable. Decline has no independent deadline/lead/venue check; scheduler lag may leave a REQUESTED row explicitly declinable.

## Transaction, notification and observability evidence

Venue-level acceptance serialization and PostgreSQL exclusion are final reservation defenses. Existing AVL concurrent HTTP acceptance runs three times with a two-transaction barrier, producing one200 and one409, one AWAITING_PAYMENT and one UNAVAILABLE. Existing VEN suspension-versus-acceptance races prove the same lock boundary. PostgreSQL tests cover both blocking statuses and all six non-blocking statuses, plus half-open adjacency.

The new competitor-failure test throws a controlled conflict after winner saveAndFlush and before competitor resolution; winner row, competitors, availability and audit roll back. The after-commit notification fault test uses a latch, not a sleep: listener failure leaves AWAITING_PAYMENT and REQUEST_ACCEPTED committed. Disabled EmailService skips provider calls. Notification recipient/copy/stale suppression and delivery-failure containment are covered by BookingNotificationServiceTest; published event counts are separate from real provider delivery.

The response expiry worker takes bounded SKIP LOCKED batches; repeated expiry is idempotent. Existing PostgresCriticalPathIntegrationTest proves disjoint worker batches. Direct acceptance handles deadline even without scheduler. Existing WorkflowObservability counts request expiration/unavailable and approval expiration without booking-ID labels; no new metrics/reminder job added.

## WHAT ELI SHOULD TEST PERSONALLY

- Is it clear that requesting does not reserve, and accepting does?
- Are response/payment deadlines and Declined / Request expired / Time unavailable understandable?
- Does the owner queue prioritize responses usefully, and do conflict messages make the next step clear?

## PRODUCT / OPERATIONS QUESTIONS

- Confirm continued ADMIN authority to accept/decline on behalf of an owner (API currently supports it; ordinary owner UI does not expose it to ADMIN).
- Decide reminders/escalation, response-window variation, and treatment of still-REQUESTED rows after suspension; no automatic cancellation was added.
- Confirm participant contact visibility/retention, and whether deadline displays should explicitly label browser-local time (calendar uses venue-local time).

## STAGING ONLY

Actual lifecycle email delivery/recipients/sender/links; deployed multi-instance acceptance and expiry scheduling; production exclusion constraint/migrations; deployed logs/metrics and deadline/timezone rendering. No payment-provider success/refund/payout checks are claimed in BK. Provider delivery and operations decisions are not local software PASS.

## Execution ledger

| Check | Evidence / current result |
|---|---|
| Backend unit suite | 680 passed; `evidence/booking/unit-results.json` |
| New PostgreSQL booking matrix | 37 passed again with final actor/timestamp assertions; `booking-core-cases.json` |
| Full PostgreSQL regressions | 414 executed PASS, 0 failures/errors; two opt-in browser classes skipped. Includes AVL acceptance3x, VEN suspension3x; `postgres-results.json` |
| Browser before fix | Real409 reproduction, `browser-before.json`, before screenshots |
| Browser final | PASS: 7 representative checks through real Chrome/Spring/PostgreSQL; `browser.json` and screenshots |
| Frontend typecheck / lint / build | PASS after final UI edits |
| Backend clean package | PASS; clean compile/package and all680 unit tests passed again |
| Master-plan PDF | PASS; regenerated656-page PDF, BK pages64–67; visual and text checks pass |

Fixture corrections during execution: a nonexistent policy-current URL was removed from the snapshot test; current required IDs are read from the real database and accepted through the actual endpoint. A failed assertion had left the isolated class's shared actors policy-stale; correcting that fixture restored all37 tests. This was not a production booking defect. Browser's intermediate failed run exposed the parent loading/unmount behavior and resulted in the complete BK-D01 fix.

## Browser and scenario closeout

The final browser uses real Spring/PostgreSQL and the production frontend build, with an isolated Los Angeles browser timezone and Amsterdam venue. It creates a270-minute request from the calendar, verifies REQUESTED/non-reservation copy and reload, checks the displayed deadline against the authoritative timestamp, verifies queue priority, accepts a winner, observes overlapping UNAVAILABLE and adjacent REQUESTED, then declines the adjacent request. Scout sees the payment deadline and Pay now without invoking checkout. Both stale review409 paths refetch and preserve safe feedback; actual lead-time expiry before response deadline displays accurate wording. Seven representative checks pass. Screenshots contain fictional QA data only.

| Scenario | Local result | Principal automated evidence |
|---|---|---|
| BK-001 | PASS | quote/minimum matrix, historic pricing/contact/policy replacement, stored-price checkout claim; browser270-minute request |
| BK-002 | PASS | 12 invalid HTTP cases and shared validator/trust/availability suites; no request/provider effects |
| BK-003 | PASS | two scouts' exact/partial/contained/adjacent REQUESTED coexist; availability unchanged |
| BK-004 | PASS | before/equal/after fixed deadline; committed409 expiry; worker idempotency3x; PostgreSQL bounded workers |
| BK-005 | PASS | winner/competitors/adjacency/different-day/terminal matrix; deadline snapshot; rollback fault; async failure; repeated AVL acceptance races |
| BK-006 | PASS | changed override/lead/suspension; existing reservation/stale request, VEN suspension race and AVL checkout exemption |
| BK-007 | PASS | both role matrices, seven invalid states, duplicate conflicts, exhaustive64 transitions, browser decline/refetch |
| BK-008 | PASS | two scouts/two owners/admin list/detail matrix; UUID/error/privacy checks; historical reads and snapshots |

No P0/P1 booking defect was reproduced. BK-D01 and BK-D02 are fixed and regression-tested. Core BOOKING is locally clear to proceed to the next domain; this is not approval of provider-payment behavior or deployed email delivery.

Evidence checks: JSON syntax and token/credential scan PASS; backend/frontend `git diff --check` PASS. Temporary browser credentials/profiles removed and this task’s frontend server stopped. Historical reproduction/harness failures are retained separately from final PASS results in `validation.json`.
