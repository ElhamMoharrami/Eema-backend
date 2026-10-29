> **Current canonical result — 2026-09-19: CANCEL-001–CANCEL-014, 14 LOCAL PASS / 0 FAIL / 0 BLOCKED. Required regression and clean package passed. Locally clear; next unfinished domain is PAYOUT, not started.** The September 15 checklist below is historical; the canonical completion record follows it.

# CANCEL execution report — requirements 163–227

Date: 2026-09-15. Scope: the supplied checklist through the incomplete text of item 227. Existing PAY changes in this checkout were preserved.

## Findings and changes

- Cancellation refunds retain their durable key on timeout, HTTP 5xx, 409, and 429. A known rejection does not silently authorize another cancellation refund operation. A new claim must match the committed cancellation obligation. Resumption reads the persisted key and amount; an amount change fails.
- Automatic normal-cancellation creation replay stops at one hour, including the exact boundary. Older ambiguous claims require manual reconciliation. Keeping the same key indefinitely would **not** prevent duplicate partial refunds after Mollie's documented cache expiry.
- Recording a provider response validates booking/payment association, claim key and amount, existing refund ID, exact two-decimal EUR amount, and supported provider status. No rounding of a provider mismatch. Missing/wrong currency and malformed responses cannot complete an obligation.
- Refund transitions explicitly move forward; REFUNDED, FAILED and CANCELED do not reopen through polling. Completion time is set only on the first accepted REFUNDED transition. Cancellation time and cancellation economics remain unchanged.
- A real concurrent PostgreSQL reconciliation test reproduced two completion events. The cause was loading the Booking entity before acquiring its lock. Reconciliation now resolves only its ID before loading under the booking lock. The common JPA configuration disables Open Session in View, matching production/test configuration so local HTTP requests also cannot retain pre-lock entities across service transactions.
- Payout mark-paid and readiness evaluation now acquire the booking lock first, matching cancellation/recalculation. Tests force both orderings of mark-paid versus cancellation. PAID transfers remain historical; recovery is a separate liability. Owner provider-cost liability is now also recorded when the payout was already PAID. Payout amounts are clamped nonnegative at ledger calculation, in addition to existing cancellation calculation/database protections.
- Admin exception queue and workflow metrics share OperationalThresholds and its injected Clock. Existing semantics are **strictly older than** the threshold: just before and exactly at 24 hours are not stuck; just after is stuck. PostgreSQL testing found an invalid Hibernate enum-literal cast in the unreconciled-fee queue. The query now uses the actual database schema; fee metrics and queue include the same paid CONFIRMED/CANCELLED population.
- CONFIRMED cancellation by Scout, Owner or Admin requires a preview proof. Unpaid withdrawal remains proof-free. A payment discovered paid during withdrawal first becomes authoritative CONFIRMED; missing financial consent then returns 409.
- Frontend preview failure clears stale terms, allows explicit retry, and never uses local financial calculations. Submission requires a proof, prevents duplicate clicks, and refetches the booking. Session/booking generation changes invalidate pending work. Cancellation opts out of the API client's automatic 401 replay. Ambiguous network errors no longer assert that the booking was unchanged.
- Cancellation emails use persisted amounts and participant-visible reasons. They avoid raw policy enum/provider-fee debugging language and do not claim a refund is still pending merely because email delivery was delayed. Email delivery remains AFTER_COMMIT and asynchronous.

## Preview design/security review (215–226)

| Candidate | Assessment |
| --- | --- |
| Server-stored version | A booking version alone misses time-bound tier changes; needs stored evaluated result and expiry. |
| Opaque random ID | Straightforward, but requires durable shared storage, expiry cleanup and multi-instance coordination. In-memory storage would fail across replicas/restarts. |
| Expected-result fingerprint | Easy to compare; an unsigned client-created fingerprint does not establish server-issued preview consent. |
| Signed stateless proof — chosen | Existing JJWT/JCA primitives, no schema or cache. Recompute under booking lock and compare the signed result fingerprint. |

The proof lasts ten minutes. HMAC-SHA256 derives a purpose-specific signing key from the existing validated JWT secret using a fixed cancellation domain string. This key differs from the access-token key; issuer is also cancellation-specific. Invalid root configuration fails startup. No new secret or migration is required. Secret rotation invalidates previews safely.

The proof binds actor ID, authenticated role, booking ID, payment ID, saved policy version/deadlines/percentage/commission and the complete evaluated decision. The token supplies no authoritative amounts. Cancellation evaluates using Clock **after acquiring the booking lock**. Changed result, wrong actor/booking, tampering, missing proof, or expiry produce HTTP 409 with `code: CANCELLATION_TERMS_CHANGED`. This is a targeted response; no general error-framework migration was introduced. Item 227 was truncated in the request.

Proofs are reusable until expiry/state change. Repeated cancellation submission returns the existing cancellation and can only resume the persisted refund identity within the provider cache window. A proof alone is not the refund idempotency mechanism. Request DTO/body is bounded; proofs are neither URL parameters nor event payload fields. Token parser exceptions are replaced with a constant conflict message, preventing token-bearing parser errors from reaching logs. The existing Admin event allowlist excludes token/fingerprint fields. There is no old paid-cancellation API fallback.

## Requirement/evidence map

“Tested” refers to local tests, not real provider verification. “Audit” means source inspection; it is not claimed as a separate browser or integration assertion.

| Items | Evidence / disposition |
| --- | --- |
| 163 | `RefundTransportContractTest` tests real client HTTP paths, method, key, body, decoding and transport errors independently of domain tests. `CancellationServiceTest` covers provider payment canceled/failed/expired, cancellation success, rejected/timeout/malformed/still-open response. Stateful PostgreSQL fake simulates accepted refund with lost response. PostgreSQL also covers unexpected paid payment after local withdrawal. |
| 164–168 | `RefundCancellationContractTest` and `CancellationIntegrationTest`: persisted identity/amount, independent connection observes commit before acceptance, reconstructed service, amount/currency/malformed ID/claim rejection, exact amount and existing refund-ID lookup. See provider association limitation below. |
| 169–175 | 48 current-state/provider-state combinations, immutable completion time and economics. PostgreSQL duplicate polling, snapshot comparison, delayed fee reconciliation, profile role/name changes and participant-visible historical reason. Late-paid unpaid withdrawal has its existing explicit accounting treatment; it is distinct from changing confirmed cancellation terms. |
| 176–179 | Inject event persistence failure before refund and after simulated provider acceptance; rollback leaves no claim/provider call in the former and recoverable CREATING in the latter. Reconstructed service resumes from DB. Concurrent provider reads lead to one completion event. This is service-fixture reconstruction, not an OS-process crash test. |
| 180 | Fixed Clock, PostgreSQL queue and metric comparison at -1/0/+1 second around the shared 24-hour cutoff. |
| 181–188 | Fee reconciliation rejects a changed authoritative fee. Settlement leaves booking/refund/payout unchanged. No waiver route exists. Unknown fee does not gate scout refund; existing readiness holds apply. Nonnegative payout calculation. Real PostgreSQL barriers force both mark-paid/cancel orderings. Audit: Admin payout action uses server conflict handling/refetch. |
| 189–191 | PostgreSQL compares original receipt/statement bytes before and after refund/recovery. Original paid amount remains unchanged. No refund document endpoint or “download refund receipt” action is introduced. A future refund document needs its own backend/document contract. |
| 192–194 | `CancellationDetails` renders stored reason as React text; owner field and request DTO are bounded to 1000 characters, coordinator also enforces bound. Email HTML escapes reason. **The single admin cancellationReason is participant-visible**: operators must write for participants. Incidents/internal investigation notes remain separate; no attachments or automated moderation added. |
| 195–200 | PostgreSQL incident report leaves booking unchanged; cancellation leaves incident OPEN. Venue suspension/pricing changes do not invalidate saved financial rights. Source audit: cancellation authorization does not require owner re-verification; destination readiness is in the payout ledger, not cancellation policy. Profile changes do not rewrite snapshots. PostgreSQL rejects deletion of venue/scout with booking history. Existing FK RESTRICT preserves booking/payout/liability/document history; no migrations/cascades added. |
| 201–208 | Frontend confirmation has explicit CTA/back/loading, no prechecked consent, duplicate submission guard, explicit zero-refund label, safe preview retry, session invalidation, no automatic 401 mutation replay, and authoritative post-submit refetch. Display values come from CancellationPreview or Booking cancellation fields; deadlines are presentation only. Browser evidence is recorded separately. |
| 209–214 | Cancellation event precedes REFUND_REQUESTED in the transaction; provider lifecycle occurs afterward. Equal transaction timestamps are allowed; no artificial millisecond ordering asserted. Email failure cannot roll back committed refund or call Mollie. Emails use saved economics; token is excluded from logs and allowlisted event data. |
| 215–226 | Proof tests: TTL before/at/after expiry, actor ID/role, booking, policy/result change, tampering/missing value, new service validation, cannot authenticate as access token. PostgreSQL tests stale financial terms, replay and unpaid-to-paid race. API/front end updated together; no migration. |
| 227 | Targeted machine-readable 409 implemented. The remainder of this checklist item was not supplied. |

## Provider semantics and limits

The HTTP client exposes refund ID, payment ID, amount/currency and status. It does **not** expose refund metadata, a provider-echoed idempotency key, a refund listing/search API, or provider completion time. We can validate the submitted local claim and exact returned payment/amount; when no provider refund ID is yet stored, we cannot independently distinguish an unrelated refund on the **same payment with the same amount** using this abstraction. Once attached, all subsequent responses must match that exact refund ID. Local completion timestamp is first observed authoritative completion, not a fabricated provider settlement time.

An ambiguous claim older than the provider replay window must not create again automatically. Current operations must inspect Mollie and the persisted booking/payment/amount/key and resolve the provider reference through a controlled reconciliation procedure. No automated provider search/import or manual override endpoint was invented. There is no standalone normal-refund scheduled poller in the current application: provider callbacks trigger authoritative lookup, and repeated cancellation resumes a CREATING claim within the bounded window. Tests exercise concurrent reconciliation calls directly.

Existing OWNER_SETTLEMENT calculation offsets fees against owner settlement and floors payout at zero. The current model creates PROVIDER_COST liability for OWNER responsibility; it does not define an additional OWNER_SETTLEMENT excess-fee debt. Whether that excess is recoverable from an owner needs an explicit accounting policy, not an invented liability rule. Existing OPEN/SETTLED/WAIVED liability amounts are never silently rewritten by these changes.

Reference contract checked against primary documentation:

- [Mollie API idempotency](https://docs.mollie.com/reference/api-idempotency): one-hour cached POST identity; reuse after cache expiry is a new request.
- [Mollie refunds](https://docs.mollie.com/docs/refunds): independent refund statuses and partial refunds.
- [Cancel payment](https://docs.mollie.com/reference/cancel-payment): DELETE returns the canceled payment object; un-cancelable payments may return 422.
- [Cancel payment refund](https://docs.mollie.com/reference/cancel-refund): queued/pending refunds can be canceled.

**These mocked transport, domain, PostgreSQL and browser tests do not prove real Mollie behavior, bank settlement, actual fees, live webhook timing, or live email delivery.**

## Validation

**Passed:** 900 unit tests; 17 PostgreSQL cancellation tests; one browser journey with six checks; frontend production build/type checks/lint; whitespace checks. Browser screenshots were visually inspected.

Final command outcomes and browser evidence are recorded in `evidence/cancellation/validation.json` and `evidence/cancellation/browser.json`. Screenshots contain fictional QA data. Raw application logs (which can include unrelated test authentication activity) are kept in `/tmp`, not copied into repository evidence.

## Canonical CANCEL domain completion — 2026-09-19 (complete)

The requirements 163–227 record above is retained as historical evidence. This pass reconciles the canonical master-plan scenarios, not a new refund or payout domain.

### Inventory and missing evidence

| Scenarios | Contract / existing coverage | Work in this pass |
|---|---|---|
| CANCEL-001 | Scout unpaid withdrawal; CancellationServiceTest, BookingCancellationCoordinatorTest; payment discovery and late-payment PostgreSQL regressions | Verify role/state denial, persistence and reservation release |
| CANCEL-002 | Owner/admin confirmed full refund, required reason, OWNER/EEMA cost responsibility | Close HTTP reason boundary and participant-denial evidence |
| CANCEL-003 | Server preview and signed proof; CancellationPreviewProofTest, staleTermsConflictAndReplayCannotChangeEconomics, browser harness | Re-execute authoritative stale/session/error workflow |
| CANCEL-004 | Zero-refund path, durable duplicate claim, refund state independence | Reuse service/refund suites and verify persisted zero path |
| CANCEL-005–014 | Ten grace/7-day/48-hour boundary rows; BookingCancellationPolicyTest has partial coverage | Add explicit scenario matrix for ordinary time and both Amsterdam DST transitions, exact cents and preview/mutation agreement |

### Source audit and authoritative rules

Audited BookingCancellationPolicy, BookingCancellationCoordinator, CancellationService, existing policy/coordinator tests and cancellation PostgreSQL/browser fixtures. The coordinator locks the booking, reads injected Clock, checks participant/state/reason, recomputes server economics and validates proof before committing CANCELLED and one refund claim. SCOUT may withdraw REQUESTED/AWAITING_PAYMENT or cancel CONFIRMED; OWNER/ADMIN cancel CONFIRMED only. Grace is inclusive at 24 hours only while shoot is strictly more than 48 elapsed hours away. Seven days is exclusive for full refund; 48 hours is inclusive for partial refund. Snapshots use venue timezone and elapsed instants. Money uses persisted paid amount and commission percentage with two-decimal HALF_UP rounding; payout is clamped at zero. Cancellation and refund statuses are separate.

Schema authority: common V1 reservation exclusion/refund uniqueness, V3 cancellation snapshots and nonnegative bounds, V4 unique payout/liability authorities and nonnegative balances, and V12 immutable refund identity/amount association. Frontend BookingCancellation and bookings API consume server previews and refetch state; the existing browser harness tests stale preview/session and failure feedback.

### Corrected generated assumptions

- Confirmed cancellation now requires a signed actor/booking/economics-bound preview token; stale or missing proof returns 409 CANCELLATION_TERMS_CHANGED. The old no-token/silent-tier-change statement is obsolete.
- Current policy acceptance and fresh owner verification are not prerequisites for exercising historical cancellation rights. Use actual endpoint verification/ownership guards.
- Admin cancellation is API-only. No invented admin UI, reset service, provider refund search or live email guarantee.
- Boundary permutations belong in deterministic tests; representative PostgreSQL and browser paths establish transport/persistence/UI behavior without duplicating every permutation in a browser.
- Local provider stubs establish Eema safety, not Mollie settlement or Resend delivery.

Dependencies: AUTH_EXECUTION_REPORT, OWNER_AUTH_RECOVERY, POL_EXECUTION_REPORT, AVL_EXECUTION_REPORT, BK_EXECUTION_REPORT, PRICE_EXECUTION_REPORT, PAY_EXECUTION_REPORT, the historical CANCEL record above and REF_EXECUTION_REPORT. Earlier domains through PAY have dated local execution results; REF already has its own complete four-scenario execution. PAYOUT is not started.

Local work: missing boundary/HTTP evidence, representative browser rerun, complete required regression, scenario mapping and regenerated PDF. Staging-only: actual Mollie payment/refund behavior, email delivery and deployed multi-instance operation. Existing excess-fee accounting and ambiguous-provider reconciliation decisions remain outside implementation scope.

### Coverage added and reused

- `CancellationScenarioBoundaryTest`: 30 named executions (CANCEL-005–014 × ordinary/spring/fall dates). Actual policy, signed proof and fixed-Clock coordinator; explicit expected tier, percentage, exact two-decimal refund/retained/commission/gross/net, deadline and equivalent-instant assertions. These are unit tests, not database concurrency proof.
- `CancellationIntegrationTest`: six additional PostgreSQL executions: three unpaid withdrawal/provider variants, two owner/admin reason/money-authority variants, and unrelated/anonymous denial plus durable zero refund. The focused class now executes 23 cases with no skips. Replacement acceptance proves reservation release through the real exclusion constraint.
- Existing `BookingCancellationPolicyTest`, `BookingCancellationCoordinatorTest`, `CancellationServiceTest`, `CancellationPreviewProofTest`, `RefundCancellationContractTest`, refund transport/claim suites and notification tests are retained. Existing PostgreSQL cancellation rollback/recovery, stale proof, concurrent reconciliation, both payout/cancellation lock orderings, fee/liability isolation and historical document/incident checks are reused.
- Existing Chrome/CDP harness extended with a controlled HTTP409 followed by real preview refetch and renewed user confirmation, plus reload consistency. The actual tier-crossing HTTP409 comes from PostgreSQL integration; the browser transport fault proves client handling, not a second server boundary test.

### Findings and execution caveats

No new production defect has been reproduced in this canonical completion pass. Production source, migrations and frontend source have not been changed by this pass. Prior cancellation/refund fixes above remain historical findings, not newly discovered fixes.

The first concurrent Maven invocations collided in the shared `target/surefire` directory and lost a temporary booter JAR before any PostgreSQL test ran. Maven runs were serialized. A subsequent sandbox Docker denial required the normal escalated test invocation; it was not classified as an application failure. The successful focused rerun executed all 23 PostgreSQL cases. Neither failed setup attempt is counted as PASS. The first broad regression was terminated with SIGTERM before completion; an ordinary background attempt also did not survive its command session. The required full command was restarted in its own process session, with a recorded exit status. Interrupted runs are not counted as passing regression evidence.

### WHAT ELHAM SHOULD TEST PERSONALLY

Review whether the confirmation makes full/partial/no-refund consequences clear, whether the owner reason is understandable as participant-visible text, and whether changed-terms and uncertain-result messages inspire the right next action. Check the confirmation and “Keep booking” action for comfortable mobile use. No manual arithmetic, race, authorization or idempotency reproduction is requested.

### PRODUCT / OPERATIONS DECISIONS

Existing decisions remain: treatment of provider fees exceeding OWNER_SETTLEMENT entitlement; operator ownership and procedure for ambiguous provider refunds outside the replay window; cancellation/legal wording and communication expectations. Current pending/queued/refunded states do not establish bank settlement. This pass adds no new financial policy or automated recovery capability.

### STAGING ONLY

Real Mollie payment cancellation, refunds, replay behavior and settlement/fee reporting; real lifecycle email delivery; deployed multi-instance locking and restart/operational recovery. Local provider responses are simulated, and no staging outcome is claimed.

### Canonical scenario results

| Scenario | Local result | Decisive evidence |
|---|---|---|
| CANCEL-001 | PASS | Three PostgreSQL withdrawal variants, replacement acceptance, discovered-paid preview conflict, late-paid refund; service provider-failure checks |
| CANCEL-002 | PASS | OWNER/ADMIN HTTP reason limits and authoritative full amount; unrelated actor denials, zero entitlement and separate cost responsibility |
| CANCEL-003 | PASS | Real PostgreSQL tier-crossing conflict, proof binding/expiry tests; eight browser checks including controlled409/refetch/renewed consent and reload |
| CANCEL-004 | PASS | PostgreSQL zero refund/no claim/one event and duplicate replay; existing ambiguous positive-refund claim/transport matrix |
| CANCEL-005 | PASS | Confirmation age23h59m, shoot+8d: GRACE_PERIOD100%; three dates |
| CANCEL-006 | PASS | Confirmation age24h exactly, shoot+3d: GRACE_PERIOD100%; three dates |
| CANCEL-007 | PASS | Confirmation age24h01m, shoot+3d: PARTIAL_REFUND50%; three dates |
| CANCEL-008 | PASS | Confirmation age48h, shoot+7d1m: FLEXIBLE_FULL_REFUND100%; three dates |
| CANCEL-009 | PASS | Confirmation age48h, shoot+7d exactly: PARTIAL_REFUND50%; three dates |
| CANCEL-010 | PASS | Confirmation age48h, shoot+3d: PARTIAL_REFUND50%; three dates |
| CANCEL-011 | PASS | Confirmation age48h, shoot+48h exactly: PARTIAL_REFUND50%; three dates |
| CANCEL-012 | PASS | Confirmation age48h, shoot+47h59m: NON_REFUNDABLE0%; three dates |
| CANCEL-013 | PASS | Confirmation age1h, shoot+48h exactly: PARTIAL_REFUND50%; three dates |
| CANCEL-014 | PASS | Confirmation age1h, shoot+47h: NON_REFUNDABLE0%; three dates |

Boundary dates are ordinary summer time, the day before Amsterdam spring-forward, and the day before fall-back. All compare elapsed instants and signed-preview/mutation agreement. Exact amounts are independently specified for EUR100.01, including HALF_UP EUR50.01 partial refund and EUR50.00 retained amount. No live provider outcome is included in these 14 local results.

### Final validation and clearance — 2026-09-19

| Command / evidence | Result |
|---|---|
| `mvn test` | PASS: 945 tests, zero failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS: 698 reported, 692 executed, six intentionally disabled opt-in browser tests, zero failures/errors |
| `mvn clean package` | PASS: clean compile/package and 945 unit tests, zero failures/errors/skips |
| Focused cancellation PostgreSQL suite | PASS: 23 tests; the same class also passed in full regression |
| Explicit `CancellationBrowserIntegrationTest` | PASS: one journey, eight checks; no skipped test |
| Frontend `npm run typecheck`, `npm run lint`, `npm run build` | All PASS; no frontend source changes in this pass |
| Both repositories `git diff --check` | PASS; additional whitespace check includes the new/untracked test and report files |
| Master plan / PDF | 14 scenario results updated; all 703 unique IDs retained. PDF regenerated (641 pages); changed section and adjacent transitions, pages76–83, rendered and visually checked |

Six full-profile skips are MatchingBrowserIntegrationTest, PriceBrowserIntegrationTest, BookingBrowserIntegrationTest, InquiryBrowserIntegrationTest, PaymentBrowserIntegrationTest and CancellationBrowserIntegrationTest. Cancellation was explicitly enabled and passed separately. Other browser domains are not newly claimed executed by this pass.

**14 PASS / 0 FAIL / 0 BLOCKED for locally executable CANCEL scenarios.** No new production defect found or fixed; no unresolved local CANCEL defect identified. This pass added test coverage and corrected documentation while preserving existing application changes and historical evidence. Locked PostgreSQL cancellation/refund races and both payout/cancellation orderings passed; this reuses prior financial coverage without beginning PAYOUT.

Durable sanitized evidence: [completion](evidence/cancellation/completion-2026-09-19.json), [30 boundary cases](evidence/cancellation/boundaries-2026-09-19.json), [focused PostgreSQL](evidence/cancellation/focused-postgres-2026-09-19.json), [browser](evidence/cancellation/browser.json), [unit](evidence/cancellation/unit-2026-09-19.json), [full PostgreSQL](evidence/cancellation/postgres-2026-09-19.json), [clean package](evidence/cancellation/package-2026-09-19.json), [document verification](evidence/cancellation/document-check-2026-09-19.json). See [evidence README](evidence/cancellation/README.md) for repeat commands and historical records.

**CANCEL is locally clear to proceed. STOP: next unfinished domain is PAYOUT-001–PAYOUT-006. PAYOUT was not started.** Real provider, human wording/usability and product/operations decisions remain separately listed above; local completion is not a whole-application release approval.
