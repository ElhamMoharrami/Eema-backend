# AVL execution report

Source audit and inventory recorded before test/production changes, 2026-09-13. Local execution complete: **15 scenarios PASS, 0 FAIL, 0 locally BLOCKED; four reproduced defects fixed.** Product and deployed checks below remain separate. Existing AUTH/POL/TRUST/VEN/READY/MEDIA changes are preserved.

## Authoritative algebra before changes

For each venue-local date, normalize(weekly UNION AVAILABLE overrides), subtract normalized(BLOCKED overrides UNION AWAITING_PAYMENT/CONFIRMED bookings), clip by lead time, then normalize again. Intervals use LocalTime and same-local-day half-open [start,end). Overlap/adjacency in the union merges; touching subtraction endpoints do not overlap. All-day means [00:00,23:59:59.999999999), not a next-day midnight endpoint. No overnight interval is supported.

`VenueBookingLeadTimePolicy` adds elapsed Duration minutes to Clock.instant(), then resolves requested local date/time with Java ZoneId rules (gap shifts forward, overlap chooses earlier offset). Display clips to the threshold in venue timezone and rounds seconds upward. The pre-change audit identified date carry at 23:59:xx and the second occurrence of the autumn overlap as candidates; both were reproduced and fixed below.

Public effective GET uses VenueService visibility: ACTIVE public; non-ACTIVE own OWNER/ADMIN only. Settings GET allows owning OWNER/ADMIN; mutations additionally require verified email (ADMIN exemption). No policy acceptance or owner review prerequisite for ordinary schedule changes. Weekly/settings mutations lock venue row; override mutation does too. Weekly PUT fully replaces, max50 intervals, ordered same-day, no overlaps but adjacency allowed. Duplicate/opposite overrides are legal and follow algebra; route-scoped delete hides cross-venue override with404. Range max93 inclusive dates. Settings lists sort persisted day strings/start time; do not assume Monday-first repository order (UI groups by weekday).

No availability cache or ordinary schedule/override lifecycle email. Per-day calculation queries venue, weekly, overrides and blocking bookings: bounded by93 dates, not an unbounded calendar fetch. Settings lists all overrides without pagination; record operational growth risk. ACTIVE can have an empty week/no slots. Timezone mutation is allowed without a booking guard; weekly/override wall-clock values stay unchanged. Booking rows do not snapshot timezone; changing venue timezone can reinterpret lifecycle timing, an operations policy question rather than permission to invent a new rule.

## Pre-change inventory

| ID | Generated title / priority | Actual existing coverage | Missing meaningful proof / environment |
|---|---|---|---|
| AVL-001 | Weekly schedule replacement / P1 | VenueAvailabilityServiceTest; VenueBookingLeadTimePolicyTest; BookingAvailabilityValidatorTest; VEN integration | Focused API/PG boundaries, exact clock and representative browser cases |
| AVL-002 | Weekly validation and adjacency / P1 | VenueAvailabilityServiceTest; VenueBookingLeadTimePolicyTest; BookingAvailabilityValidatorTest; VEN integration | Focused API/PG boundaries, exact clock and representative browser cases |
| AVL-003 | Available and blocked override algebra / P1 | VenueAvailabilityServiceTest; VenueBookingLeadTimePolicyTest; BookingAvailabilityValidatorTest; VEN integration | Focused API/PG boundaries, exact clock and representative browser cases |
| AVL-004 | Override validation and conflicts / P1 | VenueAvailabilityServiceTest; VenueBookingLeadTimePolicyTest; BookingAvailabilityValidatorTest; VEN integration | Focused API/PG boundaries, exact clock and representative browser cases |
| AVL-005 | Availability query boundary / P1 | VenueAvailabilityServiceTest; VenueBookingLeadTimePolicyTest; BookingAvailabilityValidatorTest; VEN integration | Focused API/PG boundaries, exact clock and representative browser cases |
| AVL-006 | Lead-time exact boundary and clipping / P1 | VenueAvailabilityServiceTest; VenueBookingLeadTimePolicyTest; BookingAvailabilityValidatorTest; VEN integration | Focused API/PG boundaries, exact clock and representative browser cases |
| AVL-007 | Lead-time expires between stages / P1 | VenueAvailabilityServiceTest; VenueBookingLeadTimePolicyTest; BookingAvailabilityValidatorTest; VEN integration | Focused API/PG boundaries, exact clock and representative browser cases |
| AVL-008 | Booking subtraction REQUESTED / P0 | VenueAvailabilityServiceTest; BookingReservationPolicy; PostgresCriticalPathIntegrationTest | One eight-status PostgreSQL subtraction/constraint matrix; interval shape parameterization |
| AVL-009 | Booking subtraction AWAITING_PAYMENT / P0 | VenueAvailabilityServiceTest; BookingReservationPolicy; PostgresCriticalPathIntegrationTest | One eight-status PostgreSQL subtraction/constraint matrix; interval shape parameterization |
| AVL-010 | Booking subtraction CONFIRMED / P0 | VenueAvailabilityServiceTest; BookingReservationPolicy; PostgresCriticalPathIntegrationTest | One eight-status PostgreSQL subtraction/constraint matrix; interval shape parameterization |
| AVL-011 | Booking subtraction DECLINED / P0 | VenueAvailabilityServiceTest; BookingReservationPolicy; PostgresCriticalPathIntegrationTest | One eight-status PostgreSQL subtraction/constraint matrix; interval shape parameterization |
| AVL-012 | Booking subtraction EXPIRED / P0 | VenueAvailabilityServiceTest; BookingReservationPolicy; PostgresCriticalPathIntegrationTest | One eight-status PostgreSQL subtraction/constraint matrix; interval shape parameterization |
| AVL-013 | Booking subtraction UNAVAILABLE / P0 | VenueAvailabilityServiceTest; BookingReservationPolicy; PostgresCriticalPathIntegrationTest | One eight-status PostgreSQL subtraction/constraint matrix; interval shape parameterization |
| AVL-014 | Booking subtraction CANCELLED / P0 | VenueAvailabilityServiceTest; BookingReservationPolicy; PostgresCriticalPathIntegrationTest | One eight-status PostgreSQL subtraction/constraint matrix; interval shape parameterization |
| AVL-015 | Booking subtraction REFUNDED / P0 | VenueAvailabilityServiceTest; BookingReservationPolicy; PostgresCriticalPathIntegrationTest | One eight-status PostgreSQL subtraction/constraint matrix; interval shape parameterization |

## Corrections and reuse

Remove generic Scout A/B ownership language where no owned override is addressed, blanket accepted-policy/owner-review prerequisites, duplicate API/DB assertions, generic no-email claims and fake snapshot reset. All deterministic cases use unique RUN_ID venues, Testcontainers PostgreSQL, fixed/mutable test Clock and isolated browser fixtures. No shared history is deleted.

Reuse existing PaymentServiceLeadTimeExemptionTest for checkout, BookingService and AvailabilityInquiryService tests for lead expiry, PostgreSQL exclusion/half-open/concurrent acceptance coverage and VEN suspension/commitment preservation. Extend boundaries where persistence or end-to-end authoritative refetch is missing. AVL-008–015 retain IDs but map to one status matrix.

Frontend audit: owner settings page has full weekly replacement and override create/delete; Spring responses drive saved state. InstantQuote fetches a month, renders Spring local-time strings and timezone label, offers inquiry for future unavailable dates. Browser-local `todayIso` and missing conflict refetch were reproduced as defects and fixed below. No local inventory calculation is added.

## Exact endpoint and validation contract

| Endpoint | Success | Authorization / validation |
|---|---:|---|
| GET `/api/venues/{id}/availability?from=…&to=…` | 200 | ACTIVE public. DRAFT/PENDING/SUSPENDED: own OWNER/ADMIN200, anonymous/SCOUT/other OWNER403. Missing venue404; malformed UUID400 |
| GET `/api/venues/{id}/availability/settings` | 200 | Own OWNER/ADMIN; other OWNER/SCOUT/anonymous403. Email verification not required for settings read |
| PUT `…/settings` | 200 settings DTO | Own verified-email OWNER or ADMIN; timezone must resolve through ZoneId |
| PUT `…/weekly` | 200 settings DTO | Full replacement; own verified-email OWNER or ADMIN; maximum50 intervals |
| POST `…/overrides` | 201 override DTO | Same mutation actors; date/type required; allDay boolean defaults false if omitted |
| DELETE `…/overrides/{overrideId}` | 204 | Same mutation actors; missing/cross-venue override404 |

Mutating another owner's venue or acting as SCOUT returns403; anonymous mutation returns401. ADMIN's verified-user guard is exempt by current contract. Mutations need neither accepted policies nor owner review. Policy/current-email prerequisites are established only for the separate booking/inquiry commitment fixtures. No source authorization was weakened to make tests pass.

### Weekly replacement and validation

`avlWeeklyReplacementAllWeekdays` parameterizes seven weekdays: empty →09:00–17:00 →09:00–12:00/13:00–17:00 →adjacent09:00–12:00/12:00–15:00 →empty. Exact persisted counts, effective output, no moderation events and no trust notification are checked. Unsorted same-day input returns sorted times. Whole replacement deletes previous rows transactionally under the venue lock.

`avlWeeklyValidationAndAuthorizationAreAtomic` rejects reversed/equal/overnight intervals, missing day/start/end, overlaps, missing intervals and51 entries, leaving prior settings unchanged. Exactly50 valid intervals succeed. The actor matrix covers settings read and each mutation route; unverified OWNER may read settings but cannot mutate. Owner editor failures preserve persisted values; unsaved form edits remain editable and reload restores Spring state. That is not optimistic persistence.

### Override algebra and all-day semantics

Base09:00–17:00 + AVAILABLE08:00–10:00/16:00–18:00 − BLOCKED11:00–12:00/16:00–19:00 gives08:00–11:00 and12:00–16:00. Duplicate BLOCKED and opposite-type identical ranges leave the same result. Deleting one duplicate does not remove the other's effect; deleting the last blocker immediately reopens the interval. No override uniqueness ban was added.

All-day AVAILABLE adds `[00:00,23:59:59.999999999)` with empty or populated base; BLOCKED all-day removes all of it. The representation does not support next-day midnight or overnight intervals. The frontend's minute controls display the time strings to minute precision, including23:59 for the all-day end. No fictitious24:00 support is asserted.

All-day with times, timed overrides missing an endpoint, equal/reversed endpoints return400 with no row addition. Cross-venue delete through an owned route returns404 and retains the foreign row. Public effective results are recomputed after mutations; there is no cache to invalidate.

### Query boundaries and query cost

Same-day and93 inclusive days succeed (`to = from +92`); reversed, missing or malformed dates and94 days fail400. Each day includes its interval list, including empty dates; there is no missing-date compression. Effective GET respects all four visibility states and five actor categories.

Source query audit: one outer venue/visibility lookup followed by per-day venue/weekly/override/booking reads, roughly three list queries per date plus venue reads resolved partly by the persistence context. Maximum93 days bounds calls, not total historical row count for a very busy date. Blocking booking query is restricted to venue/date/two statuses; overrides are venue/date-scoped. Settings GET returns all venue overrides without pagination, a long-lived venue operations concern. No cache, batching framework or high-cardinality metrics were added.

## Lead time and DST evidence

`AvailabilityAlgebraTest` uses fixed Clock values, never wall-clock sleeps to select boundaries. Required lead is added to an Instant as Duration. Tests prove exact120 minutes accepted,119 rejected, earlier intervals removed, and seconds rounded up. Every displayed minute in the targeted edge cases is checked against the same authoritative `VenueBookingLeadTimePolicy`.

| Fixed now / venue Amsterdam | Lead | Expected display / result |
|---|---:|---|
| 2026-09-10T08:00:00Z (10:00 local) |120m|12:00 onward;11:59 fails |
| 2026-09-10T08:00:30Z |120m|12:01 onward; do not expose12:00 |
| 2026-09-10T19:59:30Z |120m|Threshold23:59:30 rounds to next local midnight; prior date empty, next date00:00 allowed |
| 2026-10-24T22:30:00Z |120m|First02:30 occurrence remains selectable |
| 2026-10-24T23:30:00Z |120m|Second02:30 occurrence cannot be represented by earlier-offset request semantics; first selectable local time03:00 |
| 2026-03-29T00:30:00Z |120m|04:30 local after spring transition |
| 2026-03-28T09:00:00Z |1440m|Next-day11:00 local is exactly24 elapsed hours;10:59 too soon |
| 2026-10-24T08:00:00Z |1440m|Next-day09:00 local is exactly24 elapsed hours;08:59 too soon |

Java's existing local-time resolution is explicitly characterized: Amsterdam2026-03-29 02:30 shifts to03:30 (01:30Z);2026-10-25 02:30 chooses the earlier offset (00:30Z). No offset selector, overnight support or pricing-duration rewrite was introduced. The nominal wall-time schedule model cannot separately express both autumn occurrences. How to reject/explain nonexistent spring local times and whether to expose occurrence selection are product scheduling decisions; these tests do not certify arbitrary DST-gap bookings as a new supported product behavior.

### Between-stage lead expiry and accepted commitments

The real PostgreSQL fixture injects a primary mutable test Clock for availability/lead policy, initially fixed and advanced without sleeping. Other existing lifecycle deadline code still uses its normal system clock; tests explicitly keep owner/offer/payment deadlines future, including asserting the advanced lead Clock precedes paymentExpiresAt. This isolates lead expiry from response/payment deadline expiry without changing production time wiring.

A valid REQUESTED booking becomes EXPIRED on too-late owner acceptance, returns409, commits REQUEST_EXPIRED and `payload.data.reason=MINIMUM_LEAD_TIME_NOT_MET`, and emits exactly one matching lifecycle event. `noRollbackFor` preserves this terminal transition.

An inquiry begins outside ordinary availability (a normally available interval returns400 and should use booking request). The owner offers it while valid; advancing the Clock inside lead time makes acceptance409, persists EXPIRED/expiredAt, emits OFFER_EXPIRED and creates no booking. The inquiry model has no separate booking-style reason audit column; none was invented.

A second request accepted while valid becomes AWAITING_PAYMENT. Advancing inside lead time while payment deadline remains future still permits checkout201. Provider transport is mocked; no real payment or new payment rule is involved. Existing `PaymentServiceLeadTimeExemptionTest` and VEN suspension/accepted-commitment coverage remain in the full regression.

## Booking-status matrix and PostgreSQL consistency

| Scenario | BookingStatus | Reserves / partial subtraction / overlapping DB row |
|---|---|---|
| AVL-008 |REQUESTED|No;09:00–17:00; allowed |
| AVL-009 |AWAITING_PAYMENT|Yes;09:00–11:00 and13:00–17:00; rejected |
| AVL-010 |CONFIRMED|Yes;09:00–11:00 and13:00–17:00; rejected |
| AVL-011 |DECLINED|No;09:00–17:00; allowed |
| AVL-012 |EXPIRED|No;09:00–17:00; allowed |
| AVL-013 |UNAVAILABLE|No;09:00–17:00; allowed |
| AVL-014 |CANCELLED|No;09:00–17:00; allowed |
| AVL-015 |REFUNDED|No;09:00–17:00; allowed |

One eight-status PostgreSQL test creates real request rows, seeds the target persisted status as a read/constraint fixture, verifies partial and full-cover subtraction, and attempts an overlapping row in that same status. Separate autocommit operations preserve the database after expected exclusion failures. It also inspects the actual `pg_get_constraintdef`: precisely AWAITING_PAYMENT/CONFIRMED and `[)`; no other enum in the predicate. This checks the live migrated schema, not only migration text or mocked Java policy.

The shared56-case unit matrix crosses all eight statuses with contained, covering, start/end-overlapping and both touching-boundary shapes. `combinedAlgebraAndMultipleBookingsNormalizeWithoutMutation` composes weekly + AVAILABLE − BLOCKED − two reserving bookings + a non-reserving request − lead clipping; final result08:30–10:00,10:30–11:00,12:00–13:00,14:00–16:00. It verifies read evaluation does not save weekly/override/booking entities. No property-testing framework or duplicated status classes were added.

### Request-to-reservation, terminal transitions and races

`avlConcurrentAcceptanceReservesOnceAndExpiryReopens`, repeated three times per run, creates two overlapping REQUESTED rows through the API. The final three repetitions explicitly use two distinct verified-email, policy-current SCOUT accounts and assert distinct persisted scout IDs. Before acceptance availability remains whole. Two real HTTP acceptance transactions cross the existing verified two-party venue-lock barrier. Results are one200 and one409; persisted statuses are AWAITING_PAYMENT and UNAVAILABLE. Availability shrinks, and a stale request for the now-reserved interval returns409. Expiring the winner through `BookingService.expireApproval` reopens availability. No timing sleeps choose race order.

`avlConfirmationKeepsReservationAndCancellationOrRefundReleasesIt` creates request, accepts, checks out with mocked Mollie transport, then invokes the production paid-payment processing boundary. CONFIRMED remains blocking. Actual scout cancellation or admin full refund with a mocked refunded provider response persists CANCELLED/REFUNDED and reopens availability. These tests cover inventory effects only; they do not claim real provider delivery or re-test financial policy.

Existing `PostgresCriticalPathIntegrationTest` exclusion races, back-to-back `[09:00,12:00)`/`[12:00,15:00)` reservations and SKIP LOCKED worker behavior are rerun. Existing VEN proves suspension blocks new request acceptance but preserves AWAITING_PAYMENT/CONFIRMED and payment eligibility. Availability must not release a reservation merely because the venue is suspended.

## Frontend and browser findings

Owner weekly and override editor use server responses for saved state. Browser covers unavailable day, single/multiple ranges, add/remove/save/reload, overlap400, network failure, four timed/all-day override combinations and deletion. No schedule is claimed saved on failure. Settings display Amsterdam even with browser timezone America/Los_Angeles.

SCOUT displays Spring intervals without a second timezone conversion. A real competing booking is accepted between selection and submission; backend409 remains authoritative. Before AVL-D04 the UI retained both stale time inputs and made no availability GET. The fix awaits a fresh availability response, clears selection and retains the safe conflict explanation. Future unavailable dates offer the existing inquiry flow rather than fabricated local inventory.

Calendar date formatting now derives today's date from the backend venue timezone, initializes the venue-local month and ignores superseded fetch responses. This helper only formats dates/months; it does not calculate availability.20 focused client checks cover three browser timezones, twelve months including leap February, and December/January arithmetic. Fixed-date reproduction demonstrates the original browser-ahead date error. Browser adds a real Amsterdam DST date displayed09:00–17:00 in Los Angeles, availability failure/retry and exact December/January query bounds/day counts.

## Defects

| ID | Severity | Expected / actual before fix | Impact / fix |
|---|---|---|---|
| AVL-D01 |P1|Rounded23:59:xx threshold should advance date; LocalTime wrapped to00:00 and exposed earlier hours | Invalid selectable past/too-soon inventory. Round Instant before local-date comparison |
| AVL-D02 |P1|Displayed autumn-overlap starts must satisfy existing earlier-offset booking rule; second-occurrence02:30 displayed though rejected | Calendar/booking contract disagreement. Clip beyond unrepresentable second occurrence, preserving exact elapsed lead enforcement |
| AVL-D03 |P1|Calendar day/month context must use venue timezone; browser-ahead timezone disabled a valid venue-local date | Cross-timezone customer cannot select current venue inventory. Derive date/month from returned venue timezone |
| AVL-D04 |P2|409 stale slot should refetch; browser kept two stale time inputs and no GET | Repeated avoidable failed submissions. Refetch/clear selection on409, retain error |

Reproduction JSON is in `evidence/availability/before.json`, `client-before.json` and `browser-before.json`. Harness corrections are not defects: anonymous settings GET403 vs mutation401, inquiry creation requiring unavailable time, nested booking event `payload.data`, exact UI labels, and filtering GET responses rather than OPTIONS in browser network assertions.

## Scheduling, timezone, email and operations boundaries

Timezone changes are currently allowed for venues with bookings. PostgreSQL proves weekly/booking wall-clock fields and immutable existing booking row data remain unchanged when timezone changes, but derived future Instants can change because booking timezone is not snapshotted. Confirmed cancellation deadlines already snapshotted as timestamps remain their recorded values. A timezone-change policy is an operations decision; no new restriction or historical-row rewrite was invented.

Ordinary schedule/settings/override operations send no lifecycle events or email. Booking request expiry and inquiry offer expiry emit their actual lifecycle events; existing after-commit notification listeners and email-disabled service behavior are reused. Provider delivery is not part of availability algebra. Scheduled request/payment scans and inquiry offer scans run at configurable delays (default60s), use bounded batches/row locks, and are covered by existing critical-path worker tests. They do not reserve REQUESTED inventory. No availability-specific metric was necessary for these fixes; existing logs/events and scheduler operations should be checked deployed.

## WHAT ELI SHOULD TEST PERSONALLY

- Are weekly multiple ranges and one-off available/blocked overrides understandable?
- Does the calendar distinguish available time from asking the owner, and clearly identify venue timezone?
- Are lead-time clipping, failed save and stale-slot messages actionable?

## PRODUCT / OPERATIONS QUESTIONS

- Timezone changes with existing bookings; interpretation of nonexistent spring times and occurrence selection during autumn overlap.
- Overnight intervals, seasonal recurrence, calendar sync, setup/teardown buffers and maximum duration: not introduced here.
- Whether availability should ever become required for activation (currently it is not).
- Long-lived override list management and the existing same-day/minute UI end-of-day representation.

## STAGING ONLY

- Deployed JVM/timezone configuration and browser timezone behavior against the deployed API.
- Live PostgreSQL exclusion constraint and migration verification; multi-instance acceptance/scheduler locking.
- Scheduled expiry operation, logs/monitoring and any existing lifecycle notification delivery.

There is no external calendar provider to invent or test. No manual DST arithmetic, row counting or exclusion-constraint reproduction is assigned to Eli.

## Final local validation

| Layer | Result | Evidence / scope |
|---|---|---|
| `mvn test` | PASS: 375 tests | Includes 67 new parameterized algebra/lead/DST invocations |
| `mvn verify -Ppostgres-integration` | PASS: 239 tests | Real disposable PostgreSQL; 154 venue integration invocations, including 26 AVL cases. This profile skips unit execution; units ran separately |
| Final distinct-scout acceptance race | PASS: 3 repetitions | Strengthened two-account fixture rerun after the full suite; exactly one reservation each time |
| Client checks | PASS: 20 | Date/timezone/month helper checks |
| Actual browser journey | PASS: 7 grouped checks | Real owner settings and scout calendar against isolated Spring/PostgreSQL; sanitized JSON/screenshots |
| Frontend typecheck / lint / build | PASS | All three required commands completed |
| `mvn clean package` | PASS: 375 tests, packaged JAR | Final clean build after all code/test changes |
| Both repositories `git diff --check` | PASS | Final whitespace checks |
| Master-plan PDF | PASS | Regenerated 677 pages; AVL pages 53–56; page 54 visually inspected |

These counts overlap by layer and are not summed into a fictitious test total. Clean package and final artifact verification are recorded in `evidence/availability/final-validation.json`. Reusable commands and fixture limitations are in [AVL_QA_GUIDE.md](AVL_QA_GUIDE.md). Task-owned browser, frontend/backend servers, tmpfs QA database and temporary storage were removed; shared history was untouched.

**AVL is locally clear to proceed to the next domain under the current contract.** This does not approve new DST occurrence-selection, timezone-change, overnight or deployed scheduler policy.
