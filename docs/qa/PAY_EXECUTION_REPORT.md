# PAY execution report

Executed 2026-09-15. PAY-001–008: **8 LOCAL PASS, 0 FAIL, 0 BLOCKED**. Live Mollie checks and the existing PRICE zero-charge decision remain separate launch gates. The source audit below was recorded before implementation changes.

## Authoritative architecture found

BookingStatus is REQUESTED, AWAITING_PAYMENT, CONFIRMED, DECLINED, EXPIRED, UNAVAILABLE, CANCELLED, REFUNDED. Owner acceptance creates AWAITING_PAYMENT and its payment deadline. Checkout is not confirmation. A paid provider retrieval confirms AWAITING_PAYMENT only before Eema's deadline. An expired/cancelled or overdue payment is detected as late and follows refund handling without resurrecting inventory.

PaymentCreationStatus is NOT_STARTED → CREATING → CREATED. PaymentCreationCoordinator's separate transactional claim commits key/startedAt before PaymentService's external call. A second CREATING caller can make a second transport request with the SAME key; source guarantees one logical identity, not one HTTP invocation. CREATED returns stored payment ID/URL without another create. Payment replay is hardcoded one hour, with equality currently allowed (`startedAt +1h < now` rejects), not a configurable replay window. Old claims require reconciliation and surface as PAYMENT_CREATION_STUCK.

RefundStatus is NONE, CREATING, QUEUED, PENDING, PROCESSING, REFUNDED, FAILED, CANCELED. Late-refund claim and provider completion are separate transactions. Late expired bookings remain EXPIRED when refund completes; normal refund state and booking state are distinct.

Checkout POST /api/bookings/{id}/checkout has no body/amount DTO; verified current user, current centralized policies and booking.scoutId equality are required. Owner/admin have no delegated checkout privilege. Snapshot price feeds server bookingId metadata, EUR amount and configured redirect/webhook URLs. Blank webhook URL is omitted locally; production validates HTTPS redirect/webhook. Returned checkout response contains bookingId, checkoutUrl, molliePaymentId, totalAmount, expiresIn.

Public POST /api/webhooks/mollie/payment consumes form-urlencoded `id`; extra caller fields are not payment evidence. Server fetches payment from Mollie and paid processing locks the booking, checks stored ID, metadata and amount/currency. Non-paid statuses do not mirror provider failure into BookingStatus. Callback errors intentionally remain non-200 where processing should be retried; do not blindly acknowledge failures.

/booked reads only sessionStorage `eema:bookingId`, then authenticated GET booking. Query `paid`, `success`, `status` or bookingId does not establish payment or booking identity. Missing session yields a neutral state; delayed callback remains AWAITING_PAYMENT with manual recheck. No browser code writes confirmation.

## Risks to reproduce before fixes

- Checkout checks wall-clock deadline but does not persist EXPIRED when it detects expiry; Clock is not consistently injected in payment/refund paths.
- Provider paid amount uses `money()` HALF_UP before comparison, potentially accepting a fractional-cent mismatch.
- Webhook request ID is not compared to fetched payment ID; prefix-only input permits malformed path suffixes.
- Every late callback reapplies financials, potentially resetting reconciled fee/owner economics after a completed refund.
- Late refund CREATING retry lacks the one-hour replay guard used by admin refunds/payment creation.
- Webhook exception logging includes full provider exception bodies; safe client 500 does not imply safe logs.
- Payment create completion after expiry rejects association; probe crash/slow-provider retry consequences.
- Pay action conflicts do not refetch while owner accept/decline conflicts do.

## Existing coverage inventory

Reuse PRICE's 92-case money parity, snapshot/checkout amount authority and BK/INQ prerequisites. Existing PaymentCreationCoordinatorTest covers key reuse/created retry/basic expiry; WebhookServiceTest covers late refund dispatch; BookingServiceTest covers paid outcomes; RefundCoordinatorLatePaymentTest/RefundServiceTest cover refund dispatch; HttpMollieProviderClientTest covers payloads; BookingApprovalExpirationJobTest covers scan delegation. These are mostly mocked; add one focused PostgreSQL payment suite with parameterized matrices and deterministic barriers, not eight duplicated suites.

Cleanup uses fictional unique fixtures, disposable PostgreSQL, fixed/injected clocks, mocked Mollie, isolated browser profile and temporary auth fixture. No shared history deletion or live Mollie/Resend claim.

Provider references (checked 2026-09-15): [Mollie idempotency](https://docs.mollie.com/reference/api-idempotency) documents same-key caching within one hour. [Payments API webhooks](https://docs.mollie.com/reference/payments-api-webhooks) describes fetching payment status after notification. Local transport mocks prove software containment, not actual provider delivery/replay.

## Final audited contract and corrections

The original audit above is retained as pre-fix evidence. The following describes the corrected implementation. One parameterized PostgreSQL suite maps to all eight IDs; pure transport/validation tests and one representative browser journey supplement it. Generated actor boilerplate, fictitious reset instructions, repeated DB/API prose and generic email assertions were replaced in the master plan.

### Three independent lifecycles

| Aggregate | Current contract |
|---|---|
| Booking | REQUESTED is non-blocking. Owner acceptance creates AWAITING_PAYMENT and reserves inventory. A valid paid callback processed strictly before paymentExpiresAt directly creates CONFIRMED. At equality or afterward it expires. EXPIRED remains EXPIRED after late payment/refund; no inventory resurrection or second owner confirmation |
| Payment creation | NOT_STARTED → CREATING → CREATED. CREATING has durable UUID idempotency key and original start timestamp; transport timeout, malformed response or provider error does not discard the identity. CREATED holds unique molliePaymentId and checkout URL |
| Refund | NONE → CREATING → provider QUEUED/PENDING/PROCESSING → REFUNDED, with FAILED/CANCELED alternatives. Ambiguous late-refund creation retains CREATING/key; completed REFUNDED cannot regress when an older response arrives |

The provider-paid timestamp is **not present in the current MolliePaymentResponse model**. Eligibility uses Eema's callback-processing Clock, not evidence of when a customer clicked Pay or when Mollie settled. A callback processed after the deadline takes the late path even if the provider collected earlier. This is the audited current rule, not an invented timestamp policy.

### Checkout, authorization and provider payload

| Case | HTTP / effect |
|---|---|
| Own AWAITING_PAYMENT, active verified account, current required policies, future deadline | 201; snapshot amount, server booking metadata, configured redirect/webhook |
| REQUESTED/CONFIRMED/DECLINED/EXPIRED/UNAVAILABLE/CANCELLED/REFUNDED | 400; no provider creation |
| Other scout, venue owner, other owner, unrelated ADMIN | 403; no provider creation |
| Anonymous | 401; no provider creation |
| Same token after authoritative database email becomes unverified | 403; no provider creation; verification restored permits checkout |
| Missing current policy acceptance / policy replaced since prior acceptance | 403 before claim/provider; accepting current versions restores eligibility |
| Just before deadline / equal / just after | 201 / 409 / 409; equality/after persist EXPIRED and one APPROVAL_EXPIRED event before returning conflict |
| Inside minimum lead-time window but payment deadline open | Allowed; previously accepted commitment is not revalidated as a new request |
| Injected JSON/query amount/currency/venuePriceEur/total/bookingId | Ignored by no-body checkout contract; provider receives server booking ID, EUR and stored price |

Checkout policy gating requires all five current types, including PRIVACY_POLICY; the historical booking snapshot still contains only the four transactional policy references. Checkout does not replace those historical references.

Current authorization is exact `booking.scoutId == currentUser.id`, not delegated role permission. VerifiedUserPolicy permits active ADMIN without email verification and centralized policies exempt ADMIN, but those exemptions do not bypass booking identity. Normal ADMIN fixtures cannot checkout another scout's booking. No new actor policy was introduced.

PRICE's stored amount remains authoritative. The actual HTTP adapter's mock transport captures `Idempotency-Key`, metadata `bookingId`, EUR string `20.32`, configured redirect and conditional webhook omission. PRICE adapter tests already cover `100.00`, `0.01`, `500.00`; no duplicate pricing formula was added. Create responses require payment ID, exact amount/currency/association and an HTTPS checkout URL with host, no userinfo, within existing database length1000. No provider hostname allowlist was invented. Status is required for webhook retrieval; checkout attachment does not claim confirmation from a create-response status.

### Durable claim, retries and expiry

`PaymentService` does not wrap the remote call in a database transaction. Transactional `claim` locks the booking, commits CREATING/key/start, and returns the snapshot. External creation follows. Transactional `complete` locks again, validates, stores association, and returns the URL. Two callers in CREATING can both invoke provider transport using the same key: **one logical payment identity is the invariant, not one HTTP call**. CREATED retries do not call create again.

Three barrier-controlled repetitions observe committed CREATING/key through JDBC from inside the mocked remote call and prove both concurrent responses use the same identity. A fresh coordinator instance inside a new transaction reads the persisted ambiguous claim and later the stored result, proving no in-memory identity dependence. This is a service-instance restart boundary, not an actual JVM kill or real Mollie crash recovery test.

Payment replay is a one-hour constant. Local tests prove 59 minutes allowed, exact 60 allowed, 61 denied with409 and no new provider call/key. The exact source comparison permits equality. Missing claim timestamp also requires reconciliation. Existing PAYMENT_CREATION_STUCK queue and workflow gauges expose stale claims; no new operations model was added. If provider accepted but no payment ID was ever recovered and safe replay/deadline has passed, manual reconciliation remains necessary; unknown webhook association is not trusted to invent one.

A valid in-flight response arriving after expiry now retains its payment ID/URL association while leaving the booking EXPIRED and returning409. This enables validation/refund of later paid callbacks. The typed deadline exception commits expiry/association; generic errors still roll back incomplete local mutation. The ordinary expiry worker uses injected Clock, bounded SKIP LOCKED selection and per-booking transitions; three repeated tests run it twice and assert one expiry event. Direct checkout does not depend on scheduler timeliness.

### Webhook validation matrix

`POST /api/webhooks/mollie/payment` is public and form-urlencoded. Only `id` is consumed; forged paid/amount/currency/bookingId values do not establish payment facts. Server retrieval precedes any booking money/state mutation.

| Input / provider result | Result and side effects |
|---|---|
| Missing/blank/malformed callback ID, path/query/newline suffix | 400 before provider retrieval; no financial transition |
| Fetched ID differs from callback; stored ID differs from fetched payment | 500; no booking mutation |
| Missing metadata, missing/invalid/other booking UUID | 500; no booking mutation |
| Paid amount under/over, fractional125.504, short125.5, extra-scale125.500, NaN, missing amount/value | 500; no confirmation; comparison never rounds away a mismatch |
| Missing/wrong EUR currency | 500; no confirmation |
| Missing provider ID/status; null response; malformed JSON/timeout/5xx | Safe error; no financial transition; no raw provider body returned |
| open/pending/authorized/failed/canceled/expired | 200; no confirmation, ordinary AWAITING_PAYMENT remains until Eema deadline |
| paid, valid facts before deadline | 200; one CONFIRMED transition and financial effect |
| Duplicate valid paid | 200; no repeated booking snapshot/event/payout/notification event |
| paid at/after deadline | 200 when refund processing succeeds; EXPIRED + full-refund workflow |
| Ambiguous refund/provider processing failure | 500 to retain callback retry signal; already committed late state/claim remains coherent |

Malformed caller requests400 are different from provider/integrity processing500. The latter deliberately do not acknowledge financial processing as complete. Known duplicates200 stop unnecessary retries. Provider retry frequency/retention is a deployed/provider check. A completed CONFIRMED/REFUNDED booking, or a CANCELLED booking with historical confirmedAt, returns duplicate after association validation; it does not rewrite economics based on a later amount. No unsupported booking PAYMENT_FAILED/PAID state exists.

`WebhookController` now logs only safe payment ID and exception class, without throwable/raw response. A Logback capture containing a private-provider sentinel proves it never appears in that log. HttpMollieProviderClient keeps credentials in server Authorization transport; no key enters CheckoutResponse, browser state, screenshot or evidence. Existing request-correlation/error mapping is retained; no new high-cardinality telemetry was introduced.

### Confirmation, fees, events and notifications

The locked paid path uses stored `venuePriceEur` and commission percentage. The fixture 125.50 at 20% yields 25.10 commission. It stores totalPaidEur, paidAt, confirmedAt and existing cancellation/financial snapshots. There is no recalculation from current Venue pricing. A zero provisional fee is explicitly unreconciled; one owner payout ledger record is created under existing eligibility rules and is **not READY** while provider cost remains unresolved.

Three repeated simultaneous paid callbacks produce one PAYMENT_SUCCEEDED, one BOOKING_CONFIRMED, one BookingPaidEvent after commit and one payout row. A subsequent duplicate leaves the entire authoritative booking row unchanged. Existing BookingNotificationService/BookingLifecycleEventListener tests cover recipient/content and failure containment; the PostgreSQL event listener counts committed events. They prove application event deduplication, not exactly-once delivery by an external email provider or a durable notification outbox.

| Action | Existing lifecycle notification |
|---|---|
| Checkout / same-key replay | No confirmation email; payment creation is not payment success |
| Ordinary payment deadline expiry | Scout payment-window-expired lifecycle notification |
| On-time paid confirmation | Scout payment received/booking confirmed and owner booking confirmed |
| Late payment/full-refund initiation | One scout message: booking not confirmed, payment too late, full refund initiated |
| Duplicate callback / invalid financial facts | No new confirmation or late-refund notification event |

Local EmailService has delivery disabled; source listeners are exercised with provider skipped. Actual email delivery, retry and formatting are staging-only. Existing observability includes late-payment and automatic-refund counters, stuck payment/refund gauges and exception queues. No per-booking metric labels were added.

### Late payment and independent refund state

The real database tests cover both worker-first expiry and callback-discovered expiry. Both persist EXPIRED, latePaymentDetectedAt and one late event, then claim a full125.50 refund with durable key. No cancellation percentage is applied. The booking stays EXPIRED after refund reaches REFUNDED. Normal receipt API409 and absence of financial-document rows prove late payments cannot become normal receipts. No payable owner entitlement is created; the existing payout service may leave **no row**, rather than always inserting a canceled row.

A separate regression confirms a booking, cancels through the actual coordinator inside the non-refundable window, then repeats the original paid callback. The entire saved cancellation row remains unchanged and no refund is created. An already-processed payment does not become newly late simply because cancellation happened later. This fixes the callback integration without changing cancellation percentages or rules.

A replacement booking can reserve the released slot before the old late payment arrives. The old booking stays EXPIRED/refunded and the new AWAITING_PAYMENT remains valid. This exercises the real exclusion constraint, not an availability mock. Late duplicate callbacks no longer overwrite saved paid/fee/owner economics. A stale queued refund response after completed refund leaves REFUNDED unchanged.

Network timeout and provider5xx after refund creation are ambiguous: CREATING and original refund identity survive, with replay using that key. Beyond one hour, the late-refund path now stops for reconciliation, matching the established safe-window pattern. No generic cancellation/refund redesign was performed. Administrative and cancellation refund-create error classification was outside this narrow late-payment fix and remains a required REF-domain audit; shared terminal REFUNDED protection applies consistently.

## Scenario traceability and automation

| ID | Primary assertions / test mapping | Local result |
|---|---|---|
| PAY-001 | Snapshot/association injection, HTTP transport request, malformed create response matrix; PRICE references | PASS |
| PAY-002 | Eight-state/actor/policy/email matrix, fixed-clock checkout and paid deadline boundaries, lead-time exemption | PASS |
| PAY-003 | Durable committed claim, three concurrent checkout repetitions, stored retry and fresh coordinator instance | PASS |
| PAY-004 | Timeout59/60/61-minute replay and reconciliation refusal | PASS |
| PAY-005 | Invalid caller/provider matrices, actual transport JSON rejection, log redaction | PASS |
| PAY-006 | Six non-paid statuses, valid paid, three concurrent duplicate runs, financial/payout/events | PASS |
| PAY-007 | Late callback with/without worker, repeated expiry, same-key refund ambiguity, terminal/refund economics, replacement reservation, no receipt | PASS |
| PAY-008 | Seven real Chrome UI/API journey checks with controlled provider navigation | PASS |

`PaymentIntegrationTest` is the single parameterized real-PostgreSQL suite; `PaymentTransportContractTest` has 7 pure/transport cases. Existing PaymentCreationCoordinatorTest, BookingServiceTest, WebhookServiceTest, RefundCoordinatorLatePaymentTest, RefundServiceTest, HttpMollieProviderClientTest, BookingApprovalExpirationJobTest and notification/payout suites are retained and run in full validation. Canonical eight scenario counts are not added to individual parameter counts as though they were independent tests.

## Defects reproduced and corrected

| ID | Severity | Before / reproduction / impact | Correction |
|---|---|---|---|
| PAY-DEF-001 | P1 | Checkout after deadline returned409 but kept AWAITING_PAYMENT, leaving expired inventory blocked until worker | Durable expiry before conflict; fixed Clock; worker idempotency |
| PAY-DEF-002 | P0 | Provider125.504 rounded to125.50 and confirmed a125.50 booking | Strict exact EUR cents before comparison; no fractional mismatch tolerance |
| PAY-DEF-003 | P0 | Callback tr_other could apply a different fetched payment ID; prefix-only ID validation also accepted unsafe suffixes | Match incoming/fetched/stored association; constrained ID syntax |
| PAY-DEF-004 | P1 | In-flight create response after deadline rejected without storing provider ID, preventing later refund association | Persist validated association, expire and return409 without resurrection |
| PAY-DEF-005 | P0 | Repeated late callback reset refunded owner economics from0 to100.40 | Apply initial paid snapshot only once for late payment |
| PAY-DEF-006 | P0 | Ambiguous late-refund claim older than provider replay window was replayed, risking another refund | Refuse old claim; retain key and require reconciliation |
| PAY-DEF-007 | P1 | Webhook catch logged complete provider exception/body | Safe ID/error class only; sentinel regression |
| PAY-DEF-008 | P1 | Provider checkout URL was attached without scheme validation | Require HTTPS host/no userinfo/existing length bound; reject malformed response before attachment |
| PAY-DEF-009 | P2 | Browser Pay now409 retained stale pay action and did not refetch; refresh could erase useful feedback | Refetch all409 actions; preserve mounted feedback during /booked recheck |
| PAY-DEF-010 | P0 | Late-refund provider5xx marked FAILED; next callback generated a new key despite ambiguous remote result | Preserve CREATING and same identity for5xx |
| PAY-DEF-011 | P1 | Stale refund response changed completed REFUNDED back toQUEUED | Completed refund is terminal; validate association then ignore stale status |
| PAY-DEF-012 | P0 | Duplicate paid callback after real CONFIRMED → non-refundable CANCELLED changed refund0% to100%, retained125.50 to0 and queued full refund | Historical confirmedAt distinguishes already-paid cancellation from genuinely new late payment; duplicate preserves whole row and makes no refund call |

Initial probe failures are retained as sanitized reproductions, not final failures. Intermediate test-harness errors involved a real-clock overdue fixture, a nonexistent late payout-row assumption and Mockito barrier restubbing; those were corrected to authoritative fixture semantics before the passing run. A final screenshot rerun also exposed a harness-only navigation race reading document.body before creation; the wait now tolerates that loading state. No upload/pricing/booking business formula or new marketplace policy was introduced in this pass.

## Browser evidence and limits

`PaymentBrowserIntegrationTest` runs the built Next frontend, real Spring HTTP endpoints and disposable PostgreSQL. Chrome uses an isolated profile and America/Los_Angeles browser timezone; venue fixture is Europe/Amsterdam. Seven checks cover expired action conflict/refetch, fabricated query parameters, unrelated and malformed stored IDs, server checkout navigation, delayed callback then authoritative confirmation, and late-refund return state. Evidence contains method/path/status only and fictional account data, never credentials. Temporary login config/profile are deleted.

The hosted-checkout page itself is a controlled CDP response, and Mollie transport is mocked in Spring. This proves Eema's navigation and state authority, **not actual Mollie payment collection, SCA, bank settlement, provider replay or refund delivery**. Screenshots: [expired checkout](evidence/payment/stale-checkout.png), [delayed webhook](evidence/payment/delayed-webhook.png), [confirmed](evidence/payment/confirmed.png), [late refund](evidence/payment/late-refund.png). [Browser result](evidence/payment/browser.json) and [before-fix reproduction](evidence/payment/browser-before.json).

## WHAT ELI SHOULD TEST PERSONALLY

- Is “waiting for payment confirmation” understandable after returning from checkout?
- Are deadline, expiry and late-refund messages clear, without suggesting the venue is confirmed?
- Do Pay now and Check payment status feel distinct and useful?
- Does a checkout conflict leave enough guidance to recover?

Do not manually reproduce payment races, forged callbacks or decimal arithmetic.

## PRODUCT / OPERATIONS DECISIONS

- **Existing PRICE zero-charge decision remains open:** valid0.01/hour×1minute can snapshot0.00. Decide a minimum transaction policy or intentional free-booking workflow before paid launch; no arbitrary charge floor added here.
- Confirm the current server-processing-time late-payment rule and acceptable webhook-delay behavior. Using provider paidAt later would require a coordinated financial/availability policy, especially after another booking reserves released inventory.
- Define the manual reconciliation runbook for old ambiguous creation/refund claims and missing provider IDs; never issue fresh keys blindly.
- `/booked` currently needs same-session booking ID. Cross-device/lost-session recovery uses booking history; a different redirect-association UX is a product choice.

## Deferred REF-domain source finding

Administrative and cancellation refund creation still classify HttpStatusCodeException as rejection. This is a technical follow-up for REF, not a product-policy decision: the same ambiguity distinction should be audited there before those workflows are cleared. PAY's automatic late-payment path is fixed and tested; this report does not clear the complete refund domain.

## LIVE MOLLIE / STAGING TESTS STILL REQUIRED

- Real test-mode credentials and hosted checkout, EUR amount acceptance (including provider minimum), redirect and public HTTPS webhook delivery.
- Real idempotency replay guarantees near one hour, delayed/duplicate callback retries, provider timeout/5xx and reconciliation behavior. Local equality semantics are not a live guarantee.
- Refund creation/status/settlement and no duplicate financial effects against real provider records; provider fees and payout holds until reconciliation.
- Deployed migrations/unique payment association/exclusion constraints, application restarts and multiple instances running expiry/callbacks concurrently.
- Real scout/owner/late-refund emails, logs/correlation/metrics, operational queue visibility and deployed browser/session behavior.

Provider references: [Mollie API idempotency](https://docs.mollie.com/reference/api-idempotency) documents same-key replay within one hour; [Payments API webhooks](https://docs.mollie.com/reference/payments-api-webhooks) describes authoritative retrieval after notification. Neither reference substitutes for Eema staging execution.

## Validation results

| Verification | Final result |
|---|---|
| `mvn test` | PASS:803 unit tests,0 failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS:621 discovered,616 executed,5 opt-in browser skips;0 failures/errors. Includes90 payment cases |
| Payment concurrency / expiry repeats | Three checkout races, three paid-callback races and three twice-run expiry-worker cases per matrix; all passed in final full run |
| `PaymentBrowserIntegrationTest` opt-in | PASS:1 test with7 real Chrome/UI/API checks; final repeat passed with stable screenshots |
| `mvn clean package` | PASS:803 unit tests and repackaged runnable jar; clean command exit0 on final run |
| Frontend `npm run typecheck` | PASS |
| Frontend `npm run lint` | PASS: no warnings/errors |
| Frontend `npm run build` | PASS: production build |
| `git diff --check`, both repositories | PASS |
| Master plan + PDF | Corrected all8 PAY IDs; regenerated647-page PDF, extracted PAY text and visually checked page74; no PDF JavaScript |
| Real Mollie / real email delivery | NOT RUN locally; explicitly staging-only |

The first package invocation printed Maven BUILD SUCCESS but the sandbox wrapper exited143. It was rerun outside that sandbox and returned0; the final package result above is the clean rerun. The5 full-profile skips are deliberately opt-in browser suites; PAY's browser suite was executed separately rather than counted as an unexecuted PASS.

Evidence: [validation and command hashes](evidence/payment/validation.json), [source provenance](evidence/payment/source-provenance.json), [sanitized defect reproductions](evidence/payment/reproductions.json), [browser checks](evidence/payment/browser.json).

**Final local assessment:** PAY-001–008: **8 PASS,0 FAIL,0 BLOCKED**. Twelve narrow defects were corrected. Deterministic PAY is locally clear to proceed to staging; this is not an unconditional paid-launch approval. Real Mollie delivery/replay/refund checks, the existing PRICE zero-charge decision, and the explicitly separate REF follow-up remain outstanding. Eli's personal work is UX review, not manual financial arithmetic or race reproduction.

**Subsequent PRICE resolution (2026-09-19):** The zero-price continuation decision referenced above is now resolved under the user-approved Eema v1 policy. Final totals below EUR 0.01 are rejected before booking persistence/payment creation, with defensive stored-price checkout validation and V13 enforcement. See the PRICE execution report for fresh regression evidence; the prior stop remains historical.
