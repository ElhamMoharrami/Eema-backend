# Representative real screen-reader acceptance — Elham

**Status: not executed. UX-ENV-001 stays open.** This is 11 distinct flows, not 35 route repetitions. Existing automation covers all routes/states; actual speech/braille output is still required here. No recordings required.

## Setup once

Use your normal Linux graphical desktop, Orca, and either Firefox or Chrome/Chromium. Start Orca through your desktop accessibility settings; use its normal reading, heading/landmark, form and table commands alongside Tab/Shift+Tab and Enter/Space. Do not create an isolated graphical session. Record the browser and reader versions actually used, date, and tested build/revision.

Use the existing local QA application with synthetic Scout, Owner and Admin accounts. Record the local base URL and substitute fixture IDs for `[id]`; never use live customer records. Provider email/payment/storage must remain mocked or disabled. Needed fixtures: unaccepted current policies; editable owner venue; two requested bookings; a Scout booking with amounts; verified owner and synthetic payout destination; admin lists/events/exception queue large enough to paginate. Credentials belong outside evidence. If a fixture/state is unavailable, leave that flow BLOCKED rather than improvising on production. The checklist does not claim these temporary fixtures/servers are currently running.

For loading/error states use the existing QA controlled-response setup, or have the test operator prepare a delayed response and one recoverable failure. You do not need to repeat every HTTP code, authorization case or amount calculation. A successful static read alone does not cover mutation feedback.

## Common checks — once in each representative

- Hear useful page context/title, heading hierarchy, landmarks and navigation.
- Hear meaningful names, associated form labels/instructions, and required/error state.
- Follow logical keyboard focus; check its position after navigation, errors and mutations.
- Hear necessary dynamic loading and success/error feedback without hunting for it.
- Understand inline confirmations/reveals and consequences of destructive actions where present.
- Navigate lists/tables and their pagination where present; associate values with their labels.
- Understand financial amounts and textual statuses without color; hidden/decorative content must not create confusing announcements.

Mark the common checks PASS/FAIL individually in notes (context, structure, names/labels, required/errors, focus, loading, feedback, confirmation/consequence, lists/pagination, money/status, decorative content). Use N/A with a reason only where the pattern is absent. Record a concrete observed announcement, not an expected script. Subjective perfection is not required; inability to find/understand/operate a control is a concrete failure.

## Eleven flows

### AT-R01 — UX-020: `/forgot-password`

**Fixture/role:** anonymous. **Representative because:** Public required-field form, validation, asynchronous submission and recovery feedback.

Submit empty/invalid email; correct to a synthetic test address; submit with a mocked successful response and one controlled failure. Hear labels, required/error feedback, progress and result; inspect focus after each.

### AT-R02 — UX-031: `/policies`

**Fixture/role:** signed-in Scout, unaccepted current policies. **Representative because:** Policy reading regions, individual checkboxes, disabled/enabled acceptance and refreshed status.

Read each policy heading and checkbox in context; distinguish privacy acknowledgement from acceptance. Select the required policies, submit, hear the recorded result; exercise one controlled error and retry.

### AT-R03 — UX-019: `/bookings/[id]`

**Fixture/role:** owning Scout. **Representative because:** Financial definition lists, booking state and consequential action confirmation.

Read a synthetic booking with money and payment status. Open an available cancellation/withdrawal confirmation, hear its consequence, then cancel the confirmation. Use a separate disposable fixture for a completed action and its feedback; never start real checkout.

### AT-R04 — UX-033: `/scout/bookings`

**Fixture/role:** Scout with multiple own bookings. **Representative because:** Marketplace card/list navigation and navigation back from detail.

Navigate by headings/links between two bookings and detail; return to the list. Hear venue, date, state and amounts without relying on color; verify context and focus after navigation.

### AT-R05 — UX-021: `/owner/bookings`

**Fixture/role:** owning verified Owner with REQUESTED bookings. **Representative because:** Owner decision buttons, busy state, authoritative refresh and conflict feedback.

On disposable requests, accept one and decline another using existing controls. Hear action names/consequences, loading and final state; exercise one controlled conflict/error and inspect focus. No payment is made.

### AT-R06 — UX-026: `/owner/venues/[id]`

**Fixture/role:** Owner of editable synthetic venue. **Representative because:** Long grouped editor, activity selects, instructions, validation and save result.

Navigate headings/fieldsets, activity selectors and form labels. Submit a missing required field, correct it, save a harmless synthetic text change and hear feedback. Check focus/error recovery and decorative photo announcements.

### AT-R07 — UX-023: `/owner/payout-destination`

**Fixture/role:** verified Owner; current policies accepted; synthetic verified destination. **Representative because:** Sensitive-data form, inline cancellation and pending/replacement explanation.

Open Change payout details, check field labels/instructions and required errors, then cancel. Using ONLY a synthetic test fixture, submit a replacement; hear under-review state and the explanation that the existing verified destination stays active. Do not record the full test IBAN.

### AT-R08 — UX-002: `/admin/bookings`

**Fixture/role:** Admin; more than 20 synthetic bookings. **Representative because:** Shared admin shell, filter controls, table headers/cells and pagination.

Navigate table rows/cells with the reader, associate headers with values, filter, use Next/Previous and open a booking. Hear page context, loading and an empty result; recover from one controlled list error.

### AT-R09 — UX-001: `/admin/bookings/[id]`

**Fixture/role:** Admin; synthetic booking with events and monetary fields. **Representative because:** Complex section hierarchy, financial facts and independently loading pageable timeline.

Navigate Booking/Payment/Refund/Payout sections and event list. Hear amount/status labels including unavailable values. Exercise timeline error/retry while booking facts remain; paginate events and inspect focus.

### AT-R10 — UX-011: `/admin/payout-destinations/[id]`

**Fixture/role:** Admin; synthetic destination. **Representative because:** Sensitive inline reveal confirmation and focus return, without false modal semantics.

Open the existing reveal confirmation, hear its purpose/consequence, cancel and verify focus returns to its opener. Reopen and confirm using ONLY synthetic bank data; check reveal/error feedback. Record only a redacted description of announcements.

### AT-R11 — UX-003: `/admin/exceptions`

**Fixture/role:** Admin; synthetic queue with severity/types and multiple pages. **Representative because:** Operational cards, textual severity, linked records and queue pagination.

Read exception severity, type, reason and related-record links. Navigate Next/Previous and a linked record; hear loading/empty/error feedback. Confirm no meaning depends on border color and no decorative content obscures the reason.

## Evidence and completion

Fill [representative-at-results.csv](evidence/ux/representative-at-results.csv), one row per flow. Include browser/version, reader/version, scenario, PASS/FAIL (or BLOCKED if not executable), actual observed announcement, problem, optional sanitized screenshot path and notes. Keep credentials, reset URLs/tokens, full IBANs and personal financial data out of text/screenshots; describe sensitive announcements with placeholders. No recording is required.

PASS means the applicable common checks and flow actions were actually exercised with the reader. If a defect occurs, give action + observed speech/focus + expected usable behavior; retain the original FAIL when retesting. Return the filled file or paste the 11 results so they can be reviewed and the gate updated. Until all 11 pass and reproduced defects are fixed/retested, UX remains incomplete and UXACT must not start. Broader Windows/NVDA compatibility is a later representative follow-up, not a requirement to repeat every browser/reader combination locally.
