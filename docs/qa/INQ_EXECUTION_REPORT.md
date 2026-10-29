# Availability inquiry execution report

**Local result: INQ-001–004 all PASS (4/0/0). Five defects reproduced and fixed. Real provider delivery and product/UX decisions remain separate.**

Source audit and inventory recorded before modifications on 2026-09-14. The first sections preserve that audit; final corrections, execution results and limitations follow.

## Authoritative model before changes

OPEN → DECLINED or OFFERED. OFFERED → ACCEPTED or EXPIRED. No other statuses/transitions; owner cannot revise/withdraw OFFERED, scout has no explicit decline action. Decline stores declinedAt, no reason field. Expiry worker takes 100 due offers with FOR UPDATE SKIP LOCKED, defaults 60-second scan. Offer duration defaults 48h; expiry=min(now+configured hours, offered local slot start converted to Instant).

SCOUT creates/reads mine/accepts own. OWNER reads owned venues/offers/declines own. ADMIN is denied every inquiry endpoint, unlike BK owner-response delegation. Mutations require active verified-email users and current policies; list reads do not. No owner-verification/payout prerequisite. Anonymous 401, wrong role/owner 403, absent inquiry 404, state/conflict 409, structural 400. Past dates currently reach lead validation and return 409; the misleadingly named validateFutureInterval validates order/timezone, not a separate future-date 400.

Creation requires ACTIVE and a slot not wholly contained in effective availability. REQUESTED bookings remain non-blocking so do not make inquiry eligible. Blocking reservations may make creation eligible, but offer/accept must reject overlapping bookings. Neither OPEN nor OFFERED changes availability or reserves. One-off offer may be outside weekly hours or through a BLOCKED override; conversion deliberately bypasses weekly/override availability, while retaining lead/status/blocking rules.

Acceptance returns BookingDto 200 directly as AWAITING_PAYMENT, stores acceptedBookingId, sets standard 24h payment deadline and no owner response deadline. Price/commission/current live participant contacts and four policy references are snapshotted at conversion, not inquiry creation/offer. No custom price exists. Exception booking checks ACTIVE and blocking overlap but currently reads venue without a row lock, unlike normal acceptance/moderation; assess race.

Deadline equality/after: accept 409, remains OFFERED until expiry worker. Lead failure: EXPIRED committed before 409. Occupied slot:409 remains OFFERED; suspended venue:404 remains OFFERED. These distinctions must not be overwritten by generated assumptions.

## Inventory before code/test changes

| ID | Generated scenario / priority | Corrections and existing automation | Missing meaningful execution |
|---|---|---|---|
| INQ-001 | Unavailable-slot creation / P1 | AvailabilityInquiryServiceTest creation/lead; AVL existing REQUESTED/nonblocking and effective algebra. DTO message NotBlank/max 1000, service trims. Frontend fallback: “I'd like to ask about this time.” | Parameterized HTTP message/interval/trust/status matrix; normal booking versus inquiry comparison; no schedule mutation |
| INQ-002 | Same/different owner offer / P1 | Service tests offer conflict/one-off. Owner only, no ADMIN. Offer does not reserve or price-lock. Source currently omits ACTIVE check here. | Fixed-clock expiry cap, statuses, actor boundaries, blocking-status/adjacency matrix, suspension reproduction |
| INQ-003 | Offer acceptance/late conflict / P0 | Service tests conversion/ownership/lead; AVL persisted lead expiry; BK snapshots/DB exclusion. Booking REQUEST_ACCEPTED notification only, no separate inquiry ACCEPTED event. | PostgreSQL double acceptance, two offers and normal reservation races; full atomicity and snapshots; suspension serialization |
| INQ-004 | Decline/expiry visibility / P1 | Service decline/expiry, notification unit tests, shared PostgreSQL SKIP LOCKED infrastructure. Deadline 409 does not itself expire; lead 409 does. | Parameterized action/state matrix, repeated durable worker expiry, scoped lists and real browser lifecycle/conflicts |

Cleanup/environment: unique fictional RUN_ID accounts/venues, fixed Clock, disposable Testcontainers PostgreSQL, isolated Chrome profile and 0600 temporary auth config. No fake snapshot restore or deletion of shared inquiry/history. UNIT for pure/service contracts, HTTP+PostgreSQL for persistence/races, representative browser. No real Mollie/Resend calls.

## Storage, privacy and notification audit

Current baseline is common V1, not a separate live V19. Inquiry table stores requested/offered local dates/times and lifecycle timestamps, but **no timezone snapshot**: DTO reads current venue timezone. Status VARCHAR check has five values, venue/scout/accepted-booking FKs, unique accepted booking reference, ordered request interval and conditional offer-fields check. DB does not independently require ACCEPTED to have a booking reference; transaction tests must prove service atomicity. No inquiry event-history table; CREATED/OFFERED/DECLINED/OFFER_EXPIRED are application lifecycle events, not persisted MarketplaceTrustEvents. Conversion persists booking REQUEST_ACCEPTED with source=availability_inquiry and OWNER attribution; no duplicate inquiry-accepted email.

Inquiry DTO exposes scoutId/message/time/status/booking link, no contact/bank/payment data. **Owner creation email does include live scout name/email/optional phone**, unlike the dashboard DTO. Notification templates escape text/HTML and use after-commit listener, contain failures and suppress stale-state messages. Inquiry notification recipient is live user, not a contact snapshot until booking conversion. Lists are unpaginated and map venue metadata per row; retain as alpha volume/operations observation. Existing metric: eema.workflow.inquiry.offer.expired; no high-cardinality additions needed.

Frontend owner actions currently do not refetch conflicts; owner load unmounts cards. Scout conflict error is page-level and survives reload, but offer expiry is formatted in browser timezone beneath a venue-timezone label. Requested/offered times themselves render unconverted wall times as intended. Messages render as React text. These are targeted browser probes, not assumed PASS.

## Corrections and final contract

The inventory above was written before tests/fixes. Generic ownership boilerplate, fake snapshot cleanup, duplicated API/DB prose and generic provider wording have been replaced for all four master-plan IDs. Current baseline is V1, not historical V19. Past valid intervals fail lead policy 409 rather than an invented date 400. Inactive accounts are unauthenticated 401; verified-email/policy failures 403. ADMIN is not an inquiry participant and has no action delegation. No owner-verification or payout prerequisite exists.

| Operation | Allowed actor and prerequisites | Success | Rejected outcomes |
|---|---|---|---|
| POST /availability-inquiries | Active verified SCOUT, current policies, ACTIVE venue, valid lead, not normally available | 201 OPEN | Available interval 400; malformed/message 400; too-soon/past 409; non-ACTIVE 404; role/policy/email 403; anonymous/inactive 401 |
| GET /availability-inquiries/mine | Active SCOUT | 200 own rows | OWNER/ADMIN 403, unauthenticated 401 |
| GET /availability-inquiries/owned-venues | Active OWNER | 200 owned-venue rows | SCOUT/ADMIN 403, unauthenticated 401 |
| POST /{id}/offer | Own verified/current-policy OWNER, OPEN, ACTIVE, valid lead and no blocking overlap | 200 OFFERED | Invalid payload 400; wrong state/lead/overlap/non-ACTIVE 409; unrelated actor 403 |
| POST /{id}/decline | Own verified/current-policy OWNER, OPEN | 200 DECLINED | Non-OPEN 409; no reason field or ACTIVE requirement |
| POST /{id}/accept | Originating verified/current-policy SCOUT, OFFERED, unexpired, ACTIVE, valid lead/no blocking overlap | 200 BookingDto AWAITING_PAYMENT | Duplicate/state/deadline/lead/overlap 409; non-ACTIVE 404; unrelated actor 403 |

All ID mutations return 404 for a nonexistent UUID and 400 for malformed UUID where the actor can reach that command. Unknown command fields bind to neither entity state nor ownership. No inquiry detail GET exists. Lists are not policy-gated.

| Layer | Proven behavior |
|---|---|
| Message | Null/empty/whitespace/1001 fail 400; one Unicode character/1000 pass; validation precedes trim. Browser blank fallback is tested independently. |
| Routing | Normal 09–11 booking request succeeds, inquiry 400. Outside 09–17 normal request 409, inquiry 201. Existing REQUESTED remains nonblocking. Blocking booking may permit inquiry creation but owner cannot offer its occupied interval. |
| Offers | Same/different requested date/time allowed. Weekly/override restrictions deliberately bypassed only for one-off conversion. No price negotiation or durable calendar override. |
| Blocking set | Exactly AWAITING_PAYMENT and CONFIRMED; other six BookingStatus values do not block offers. Adjacent half-open intervals allowed, partial overlap blocked. |
| Expiry | Default 48h capped to offered local slot start; exact equality rejects. Deadline rejection remains OFFERED until worker; lead rejection durably EXPIRED. OPEN never automatically expires. |
| Booking conversion | One AWAITING_PAYMENT booking/link; acceptedAt and standard configured 24h payment deadline; ownerResponseExpiresAt null. No second owner acceptance. |
| Snapshots | At acceptance: current venue pricing/minimum/full-day cap, selected/billable minutes, current commission, current live participant contacts, four current booking policy references. Terms/Acceptable Use/Production/Insurance; privacy excluded. Later venue price edits do not reprice the booking. BK covers the shared immutable contact/policy history model. |
| Price example | Offered 09:15–13:45 selects 270 minutes; fixture minimum 360 makes 360 billable. Expected money comes from BookingPriceCalculator. Mocked shared checkout sends stored amount, creates no real Mollie request. |
| Policies/account | Replacement after offer prevents offer/decline/accept until current versions accepted; conversion snapshots replacement. Existing token cannot bypass current DB email/inactive state. |
| Calendar | Exact persisted operating-hour/override JSON unchanged across create/offer/accept/decline/expiry and failures. Inquiry alone does not shrink effective availability; converted booking does. |
| Privacy | Inquiry DTO contains IDs, venue metadata, original/offered time, message, state, timestamps and booking link, not contacts/tokens/password/payment/bank data. React text rendering prevents script interpretation. |

## Automation and evidence

`InquiryIntegrationTest` uses `InquirySupport` over BK's disposable PostgreSQL fixture/real Spring HTTP stack. Repetition is parameterized: message, creation, offer input, all eight booking statuses, exact lead, expiry boundary, actor/account and five inquiry statuses × four actions. No twelve/five copied test classes or H2. Complete unit suite includes existing `AvailabilityInquiryServiceTest`, `AvailabilityInquiryNotificationServiceTest`, lead-time and shared checkout tests.

Concurrency tests use barriers before real repository row-lock acquisition, independent transactions and database assertions, never sleep-based race guesses:

- Same OFFERED accepted twice: one 200/one 409, one booking, one booking audit, same accepted link; repeated 3.
- Two overlapping offers: one 200/one 409, exactly one blocking booking, no ACCEPTED orphan; repeated 3.
- Offer acceptance versus ordinary REQUESTED acceptance: exactly one reservation; repeated 3.
- Acceptance versus admin suspension: coherent serialization. If acceptance commits first, existing commitment survives suspension; if suspension wins, acceptance 404 and inquiry remains OFFERED; repeated 3.
- Due-offer worker twice: one terminal transition/event, no booking/calendar write; repeated 3. Existing `PostgresCriticalPathIntegrationTest.inquiryAndPayoutSkipLockedQueriesAreActuallyDisjoint` proves disjoint inquiry worker claims.

A stricter test-only CHECK on one unique disposable inquiry rejects its ACCEPTED update. The resulting generic 500 is intentional injected persistence failure, not a successful race outcome: inquiry remains OFFERED, booking/policy/audit transaction rolls back, and no after-commit booking notification occurs. No production constraint is disabled. Normal races return safe 200/409 or status 404, never 500.

DB baseline enforces inquiry FKs/status/request ordering/offer-field combinations/unique booking reference and booking exclusion for the two blocking statuses. Service locking/atomic conversion establishes the stronger ACCEPTED↔booking invariant; no invented DB guarantee is claimed. Testcontainers applies real migrations.

Sanitized evidence lives in `evidence/inquiry/`: suspension pre-fix JSON; before/after browser network summaries and screenshots; verification summary. Only fictional identities are used. Temporary tokens stay in mode 0600 config files removed by the harness; screenshots/network summaries omit credentials. Raw application logs are not committed because injected persistence failures can include row payloads.

## Notifications and events

| Transition | Application event / persistence | Recipient |
|---|---|---|
| Create OPEN | Inquiry CREATED, no persisted inquiry audit row | Owner: new availability inquiry |
| OPEN→OFFERED | Inquiry OFFERED | Scout: owner offered time/deadline, not reserved |
| OPEN→DECLINED | Inquiry DECLINED | Scout: inquiry declined |
| OFFERED→EXPIRED | Inquiry OFFER_EXPIRED, expiry metric | Scout: offer expired |
| OFFERED→ACCEPTED | Booking REQUEST_ACCEPTED audit, source availability_inquiry, actor OWNER; booking lifecycle REQUEST_ACCEPTED only | Scout: accepted booking / pay now |

Listeners are after-commit and contain notification failures. Existing notification tests verify recipients, safe template semantics and provider-failure containment. Integration tests capture committed events and prove no duplicate lifecycle effect for denied/duplicate actions. Local EmailService explicitly skips provider calls when disabled. Mocks and local events do not prove actual Resend delivery. No inquiry-accepted duplicate notification is added. Successful matching/payment/refund/payout are not claimed.

## Defects reproduced and fixed

| ID | Severity | Expected / actual / reproduction | Narrow fix and verification |
|---|---|---|---|
| INQ-D01 | P1 | Suspended venue must not receive new offers; OPEN offer succeeded. Unit regression failed before fix. | Own venue locked and ACTIVE checked before offer. Real admin-suspend→offer now 409 with unchanged OPEN/event count. |
| INQ-D02 | P1 | Suspension must serialize with new commitments. Pausing before booking insert allowed real admin suspension to commit, then acceptance 200 created blocking booking on SUSPENDED. | Inquiry acceptance locks venue before status/booking checks, same boundary as normal acceptance/moderation. PostgreSQL suspension/normal/two-offer races and expiry-after-lock-wait regression verify the boundary. Existing accepted bookings are preserved. |
| INQ-D03 | P2 | Stale owner action must refetch authoritative state and retain feedback. Browser confirmed both offer and decline 409 left stale OPEN controls with no GET. | Refetch 409; refresh preserves card instance/error; busy released in finally. Real before/after browser assertions. |
| INQ-D04 | P2 | Deadline beneath venue-zone label must use that timezone. Browser Los Angeles displayed local deadline under Europe/Amsterdam label. | Existing formatTimestamp receives inquiry.timezone. Browser checks expected Amsterdam text. |
| INQ-D05 | P2 | Inquiry-converted booking screenshots labelled saved prices “at request,” but snapshots are captured at acceptance. | Neutral “Saved hourly rate / minimum booking / full-day rate” labels in shared cards and detail. No amount or timing logic changed; browser checks detail wording. |

Clock injection uses existing application Clock for inquiry timestamps and exception-booking payment deadline; no window formula or offer policy changed. No new marketplace policy, price locking, reservation for offers, expiry state or provider integration was introduced.

## WHAT ELI SHOULD TEST PERSONALLY

- Is the choice between booking request and inquiry understandable?
- Are requested versus offered times, non-reservation and expiry clear?
- Does accepting an offer clearly lead to payment, and do conflicts explain the next step?

Do not manually test binary API validation, database fields, arithmetic or races.

## PRODUCT / OPERATIONS QUESTIONS

- Confirm current acceptance-time pricing: offers do not lock price or reserve inventory. UI does not promise a locked price, but should pricing be shown/acknowledged before acceptance?
- Owner emails currently disclose scout contact details before a booking; confirm intended disclosure/retention. Inquiry DTO/dashboard omit these contacts.
- Requested/offered wall times use live venue timezone, not a snapshot; coordinate future timezone-change policy with AVL/BK.
- Offer revision/withdrawal, explicit scout decline, OPEN response SLA, retention and eventual list pagination remain decisions/backlog. Current lists are unbounded with per-row venue mapping; no alpha-volume evidence justifies broad pagination changes here.
- Suspended venues cannot offer/convert; existing OFFERED rows remain visible until expiry. Decide whether an explicit withdrawal workflow is desirable later.

These are not silently implemented or counted as software failures/legal approval.

## STAGING ONLY

- Real lifecycle email delivery, recipient/content/link verification and sender configuration.
- Deployed expiry scheduling, multi-instance acceptance/suspension behavior and production exclusion constraints/migrations.
- Deployed authorization, browser timezone/deadline display, redacted logs and expiry metrics.

There is no external inquiry provider. Real Mollie payment/refund/payout belong to their own domains.

## Final execution results

INQ-001, INQ-002, INQ-003 and INQ-004: **4 PASS / 0 FAIL / 0 BLOCKED locally**. Five reproduced defects fixed (2 P1, 3 P2). Human/product/deployment checks above are not software PASS claims.

| Check | Result |
|---|---|
| `mvn test` | PASS: 681 tests, zero failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS: 509 discovered, 506 executed, 3 opt-in browser skips; zero failures/errors |
| Final focused PostgreSQL + real browser after deadline-lock hardening | PASS: 95 tests: 94 inquiry PostgreSQL cases plus 1 browser harness; zero failures/errors/skips |
| Final browser rerun after saved-price copy fix | PASS: 1 opt-in harness / 12 browser checks; no skipped cases |
| Browser assertions within that harness | PASS: 12 representative checks, real Chrome → Next → Spring → disposable PostgreSQL |
| `mvn clean package` on final source | PASS: 681 tests; executable JAR built |
| Frontend typecheck / lint / build | PASS; no lint warnings/errors |
| Backend/frontend `git diff --check` | PASS |
| Master-plan PDF | Regenerated via existing renderer; 656 pages, inquiry section pages 68–69 visually/text checked |

The full profile ran before the final lock-wait expiry recheck and two additional focused regressions. Final 94-case inquiry execution reran all inquiry races on the final implementation. Counts are reported per command, not added together as distinct tests. Browser tests are opt-in; the inquiry browser harness was explicitly executed, not inferred from the full-profile skip.

Harness setup corrections (override column name, waiting for actual DECLINED rather than temporary busy text, and tolerating the scout refetch loading state) were fixed and rerun; these were test-harness issues, not extra product defects. Final browser evidence uses the strengthened state assertions.

Browser evidence covers owner offer/decline409 refetch and retained feedback; scout stale duplicate and occupied acceptance; worker-expired display; Los Angeles browser/Amsterdam deadline; actual unavailable-day mode switch and neutral message fallback; owner different-date offer preserving original request; scout booking redirect to AWAITING_PAYMENT; user message text escaping. No real payment was attempted. Screenshots and sanitized network outcomes are linked by filename in [browser evidence](evidence/inquiry/browser.json).

**Locally clear to proceed to the next domain**, subject to completing the separately listed human/product/staging checks before release. The deterministic inquiry lifecycle, authority boundaries, snapshots, no-calendar-write invariant and reservation atomicity are locally proven.
