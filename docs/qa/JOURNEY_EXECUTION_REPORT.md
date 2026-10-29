# JOURNEY execution report

**Final local result: JOURNEY-001–009 — 9 PASS / 0 FAIL / 0 BLOCKED. No new unresolved defect or decision.**

## Initial audit — 2026-09-21

JOURNEY-001–009 are NOT RUN pending cross-domain execution. PROVIDER-001–005 remain BLOCKED — STAGING/EXTERNAL; their report is preserved. Real screen-reader execution remains deferred.

| Scenario | Workflow | Missing evidence |
|---|---|---|
| JOURNEY-001 | Scout registration through confirmed booking and private receipt | Coherent aggregate across domain boundaries |
| JOURNEY-002 | Owner onboarding through venue activation and recorded payout | Coherent aggregate across domain boundaries |
| JOURNEY-003 | Admin review, incidents, liabilities and policy publication | Scoped operation sequence and durable audit |
| JOURNEY-004 | Five cancellation outcomes | Paid aggregate through preview, cancellation and refund completion |
| JOURNEY-005 | Inquiry through payment | Accepted inquiry linkage and unchanged recurring availability |
| JOURNEY-006 | Competing requests | Winner/loser history and denied stale checkout |
| JOURNEY-007 | Suspension | Preserved history and rejected new commitments |
| JOURNEY-008 | Recovery | Revoked refresh tokens, bounded existing JWT lifetime and preserved resumable booking |
| JOURNEY-009 | Policy replacement | Immutable history and resumed gated transaction |

Source audit starts from existing AuthPostgresSupport, BookingCoreSupport, InquirySupport and current domain integration tests, then their actual service/controller contracts, frontend routes and migrations. Dependencies: AUTH, POL, TRUST, VEN, READY, MEDIA, AVL, BK, INQ, PRICE, PAY, CANCEL, REF, PAYOUT, DEST, DOC, INC, ADMIN, OPS, UX and UXACT execution reports. Fresh CALA PostgreSQL regression and EMAIL unit/browser evidence provide the unchanged baseline; source existence alone is not a pass.

Corrected assumptions: JOURNEY-007's old-request acceptance gap was already fixed in VEN/EDGE. JOURNEY-008 retains the established AUTH contract: reset deletes refresh tokens, while existing access JWTs remain valid until expiry for active accounts. The initial inference that this had been fixed was incorrect and was corrected against source, AUTH report and executed tests before implementation. Generic five-role matrices do not apply to every step. Registration starts unverified; payment confirmation comes only from fetched mocked provider truth. READY is not PAID. Local synthetic policy publication is not legal approval. Do not infer real provider delivery from local adapters.

Strategy: real Spring HTTP/security/transactions and disposable PostgreSQL for coherent workflows; fixed time for financial boundaries; mock external providers; reuse executed domain boundary and representative browser suites, adding only missing integration assertions. Capture persisted state, snapshots, audit, private documents, idempotent transitions and denied side effects. No production change is proposed before evidence identifies a clear defect. Local workflows can proceed independently of PROVIDER.

## WHAT ELHAM SHOULD TEST PERSONALLY

Workflow clarity, distinction between request/approval/payment/confirmation, refund copy, and READY versus externally paid payout terminology.

## PRODUCT / OPERATIONS DECISIONS

No new decision identified in the initial audit. Existing approved decisions remain authoritative.

## STAGING ONLY

Actual provider checkout/refund/delivery/storage and deployed infrastructure remain under PROVIDER and existing staging requirements.

## Contract audit refinement

Inspected actual BookingService commitment and paid-payment transitions, PolicyAcceptanceService, BookingCancellationPolicy/Coordinator, OwnerPayoutService, VenueModerationService and VenueProductionProfileValidator; controllers and request/response DTOs; existing common migration constraints; frontend inquiry acceptance navigation and admin payout controls. Booking snapshots contain four transaction policies, excluding PRIVACY_POLICY, although users accept five current policies. Inquiry acceptance returns BookingDto, while subsequent inquiry reads expose acceptedBookingId. Provider-cost responsibility is OWNER_SETTLEMENT for scout cancellation, OWNER for owner cancellation and EEMA for admin cancellation; this internal field is asserted from PostgreSQL, not invented in the participant DTO.

The added test uses actual registration and verification for the onboarding/recovery actors, actual venue creation/update/image upload/weekly availability/review, and actual checkout plus fetched synthetic paid callbacks. Existing admin provisioning and initial policies are isolated fixture setup. Inquiry and cancellation cases reuse already-established active-venue fixtures rather than retesting onboarding for every variant. Admin incident operations retain financial isolation; liability settlement uses an explicitly seeded existing OPEN liability, not an invented incident adjustment. Private document files use a dedicated temporary root deleted at suite completion.

## Executed result and scenario mapping

The 12 new PostgreSQL invocations passed. A further focused five-case execution passed after adding direct cancellation ledger amount assertions. All 14 common Flyway migrations and Hibernate validation succeeded on fresh disposable PostgreSQL. No migration or production change was introduced.

| Scenario | Executed local proof |
|---|---|
| JOURNEY-001 | Fresh scout registers, verifies, accepts policies, browses venue/availability, requests 09:15–13:45, owner accepts, checkout uses €564.75, duplicate fetched paid callback confirms once, private receipt is immutable and another scout is denied |
| JOURNEY-002 | Fresh owner verifies email, accepts policies, obtains manual owner approval, creates DRAFT, completes profile, uploads two real local PNGs, saves weekly availability, obtains venue approval, submits encrypted synthetic destination and admin verification; same booking becomes READY only after end/fee reconciliation, then admin records a simulated external transfer and owner downloads statement |
| JOURNEY-003 | All admin operations resources load; booking detail/timeline, participant incident OPEN→UNDER_REVIEW→RESOLVED→CLOSED with four audit entries, internal note stays private and booking unchanged; existing OPEN liability settlement rejects owner and accepts admin; review/payment-recording/publication steps additionally covered by coherent onboarding/policy tests |
| JOURNEY-004 | Five independently paid bookings: scout full/half/zero and owner/admin full refund. Actual preview token, cancellation and mocked completed refund; booking stays CANCELLED with separate refund state. Exact retained amount, commission and owner payout asserted from persisted ledger; cost responsibility differs; zero-refund case makes no refund call; both participants can read history and scout gets receipt |
| JOURNEY-005 | Real inquiry open/offer/accept returns one linked AWAITING_PAYMENT booking, paid callback confirms it; weekly/override calendar remains byte-equivalent |
| JOURNEY-006 | Overlapping requests coexist until acceptance; winner pays/confirms; loser remains UNAVAILABLE, stale acceptance/checkout denied, no loser provider creation or duplicate lifecycle notice |
| JOURNEY-007 | Real approved venue and paid history survive suspension; anonymous detail denied, new request denied, old request acceptance conflicts; receipt survives and reactivation permits eligible pending request |
| JOURNEY-008 | Fresh verified scout's payable booking survives password reset unchanged; old refresh fails, old unexpired access still works under established AUTH semantics, old password fails, new login completes payment |
| JOURNEY-009 | Confirmed booking's policy references and already-issued receipt remain immutable after isolated replacement publication; new commitment blocked until exact new policy acceptance, then succeeds through payment |

### Corrections and defects

The initial run was 11/12 passing: the new harness expected404 on anonymous suspended venue detail, but VenueService's established contract is403. Corrected the harness, not production; sanitized history retained in `evidence/journey/history/initial.json`. A first ledger follow-up reused an older compiled class because the extra assertions were edited during the preceding compile; it is retained only as historical evidence. A forced recompile and bytecode check precede the credited ledger run. No product defect was found. The initial audit incorrectly inferred immediate JWT revocation; AUTH/source disproved that inference before implementation. Existing bounded JWT lifetime is preserved, not silently redesigned. The generated suspension gap is resolved, not reopened.

### Regression and browser evidence

- New PostgreSQL workflow suite:12 passed; additional strengthened ledger suite:5 passed. No skipped new workflow tests.
- Fresh frontend client regression:30 checks passed, including same-actor refresh, different-actor replay denial and no automatic replay after unknown transport outcome.
- Full backend unit/package baseline from EMAIL:1113 passed. Full PostgreSQL baseline from CALA:1027 reported,1014 executed passing,13 opt-in browser cases skipped, plus14 passing critical-path context-cleanup checks. These commands actually ran earlier in this autonomous run; they are explicitly reused, not represented as rerun for JOURNEY.
- Frontend typecheck/lint/build passed in the unchanged CALA baseline. Compared452 production/build/frontend files against saved hashes: unchanged. Backend/frontend diff checks pass.
- Previously executed actual browser evidence in AUTH, BK, INQ, PAY, CANCEL, PAYOUT, DEST, DOC, ADMIN, EDGE and UXACT covers the corresponding role controls, navigation, reload and conflict UI. EMAIL's nine new link checks cover CTA routing. `reused-browser.json` identifies evidence and hashes. This is layered evidence: no claim that a new browser traversed every step or that synthetic providers verify external services.
- Existing financial, authorization, timing and deterministic concurrency matrices are reused rather than duplicated. New workflows inspect persisted associations and outcomes; no arbitrary race sleeps or live money.

Evidence: `evidence/journey/{inventory,postgres,ledger,client,dependencies,reused-regression,reused-browser,work-status}.json`. Raw token/provider logs are not copied. Testcontainers owns database cleanup; dedicated private-document/media directory is deleted. No shared staging data changed.

## Final human / decision / staging review

**WHAT ELHAM SHOULD TEST PERSONALLY:** clarity of request versus approved-but-unpaid versus confirmed; inquiry-to-booking navigation; cancellation preview/refund progress; owner READY versus PAID wording; mobile navigation between histories, receipts and operations. No manual SQL, races or financial calculations requested.

**PRODUCT / OPERATIONS DECISIONS:** no new unresolved decision; existing AUTH session lifetime and approved pricing/accounting/document/incident policies remain unchanged. Isolated test policies are not legal approval.

**STAGING ONLY:** real provider checkout/refund, email delivery, external transfer procedure and deployed controls remain unverified. PROVIDER report preserved unchanged; blocked external scenarios do not block independent local work. Real screen-reader execution remains deferred.

**Locally clear to proceed. Next unfinished local domain: INPUT-001–028.**
