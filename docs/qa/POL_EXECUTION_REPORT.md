# POLICY execution report

Local deterministic execution completed, 2026-09-12. Inventory below was recorded before code modifications.
Code is authoritative. Software results do not approve legal content.

## Discovery inventory

| ID / title | Priority | Generated assumptions requiring correction | Endpoint ownership / prerequisites | Existing automation at discovery | Environment / human limitation |
|---|---|---|---|---|---|
| POL-001 — draft, publish, replacement, retire | P0 | Generic scout/owner roles, acceptance prerequisite, duplicated DB/API assertions, imaginary snapshot restore | ADMIN list/create/publish/retire; active authenticated account, no email verification or acceptance prerequisite | PostgreSQL critical-path SQL publication race only | Disposable PostgreSQL + admin browser; approved production content remains legal work |
| POL-002 — immutability / unsupported edit | P0 | Entity update-method test despite no update methods; generic roles and cleanup | ADMIN API boundary; draft/published/retired fixtures | SQL snapshot history check (partial) | PostgreSQL + API; no legal decision needed for immutable records |
| POL-003 — exact IDs / duplicates | P0 | Already-accepted prerequisite contradicts test; duplicate JSON IDs not distinguished from distinct IDs | Authenticated SCOUT/OWNER/ADMIN; no verification prerequisite; Set request accepts nonempty subsets, deduplicates IDs | PolicyAcceptanceServiceTest: mocked repeat, replacement, four snapshot writes | PostgreSQL + HTTP; all-five role semantics need business approval |
| POL-004 — replacement gating / history | P0 | Generic provider silence includes unrelated booking actions; ownership variants misapplied to policies | SCOUT/OWNER own booking/inquiry/venue/destination with each action's independent eligibility; ADMIN publishes replacement and bypasses commitment policy gate | Mock replacement status; SQL historical snapshot; existing booking/inquiry/payment suites | PostgreSQL + browser; rights tested independently from new commitments |
| POL-005 — privacy / missing UI | P0 | Generic role denial and acceptance prerequisite; software PASS conflated with legal review | Authenticated policy centre; empty/partial/full/subset/accepted/replaced sets; ADMIN prepares isolated data | Source only | Chrome + real local backend; LEGAL REVIEW REQUIRED |
| POL-006 — publication / acceptance race | P0 | Generic roles, fixture restore, repeated API/DB assertions; SQL uniqueness not service transaction proof | ADMIN publisher; authenticated accepting user; deterministic transaction barriers | SQL publication race; mocked sequential duplicate only | Real PostgreSQL required; no human row-count/race work |

## Audited architecture

- `/api/admin/policies` GET/POST and `/{id}/publish`, `/{id}/retire` POST are ADMIN-only. Other authenticated roles receive 403; anonymous receives 401. Policy current/type/status reads and acceptance require authentication, including ADMIN support. No ownership UUID applies to global policy versions.
- Required set: TERMS_OF_SERVICE, PRIVACY_POLICY, ACCEPTABLE_USE_POLICY, PRODUCTION_POLICY, INSURANCE_REQUIREMENTS. Booking references exclude PRIVACY_POLICY. ADMIN bypasses `requireCurrentPolicies`, not status calculation.
- DRAFT → PUBLISHED → RETIRED. Publication immediately retires the previous current version, hashes exact UTF-8 content with SHA-256, and records publication time. `effectiveAt` is metadata even in the future. Hashes are integrity metadata, not digital signatures. No edit or deletion endpoint, including drafts; immutability is an application contract, not a database trigger preventing privileged SQL updates.
- Acceptance body is `{policyVersionIds: [...]}` mapped to a nonempty Set of at most five distinct non-null UUIDs. Subsets are allowed. Duplicate JSON IDs collapse; stale/draft/retired/nonexistent IDs reject. Unknown-field behavior and exact unsupported-method statuses are to be exercised.
- V11 has unique type/version, partial unique current type, unique user/version acceptance, and RESTRICT historical foreign keys. No policy text seeded. Policy lifecycle/acceptance services publish no events and invoke no email/provider services.
- Policy gates found in booking create/create-from-inquiry/owner accept/decline; inquiry create/owner offer/owner decline/scout accept; checkout; payout-destination submit; venue submit. Login, browsing, historical reads, cancellation/refund, financial documents, payouts and incident services do not call policy enforcement.
- Frontend renders content as React text with whitespace preserved; Privacy says “I acknowledge that I have reviewed this privacy notice.” No legal approval inferred. UI requires selecting all currently unaccepted cards although API permits subsets. 403 policy errors dispatch `eema:policy-stale`; provider refreshes status. No publication push/polling.

## Isolation and evidence

Use unique `QA-<RUN_ID>-v1/v2` and `QA TEST CONTENT — NOT LEGAL TERMS`. Integration tests use disposable Testcontainers PostgreSQL; never H2. Browser fixtures must use an isolated local database or retain unique local QA history until an intentional guarded full reset. No manual deletion of published shared policies. Evidence goes in `docs/qa/evidence/policy/`, without tokens, passwords, cookies or provider credentials.

## Audit suspicions at discovery (resolved below)

- P1: concurrent acceptance catches `DataIntegrityViolationException` inside the transaction, potentially leaving it unusable/rollback-only.
- P2: policy centre computes completion from returned cards alone; a missing required type with other cards accepted can show a false completion message.

Final verification results and scenario disposition are recorded below.

## Corrections and defects

The generated six scenarios repeated unrelated participant-ownership roles, accepted-policy prerequisites (even for accepting/publishing), an imaginary snapshot restore, and API assertions copied into DB sections. These are replaced with per-endpoint roles, prerequisites, persisted-state expectations, isolated cleanup, exact automation mappings and explicit legal limits. No internal entity update methods, scheduling, digital signatures or generic provider expectations were invented. Unknown request fields remain ignored by the existing policy DTO contract; no new validation rules were introduced.

Three production defects were reproduced and narrowly fixed. See [sanitized defect evidence](evidence/policy/defects.json):

| Defect | Severity | Expected / actual / impact | Resolution |
|---|---|---|---|
| POL-D01 concurrent acknowledgement | P1 | Both identical requests should succeed idempotently; one returned HTTP 500 after uniqueness violation and subsequent aborted-transaction SQL. Users could receive an error despite idempotent intent. | Native PostgreSQL `ON CONFLICT DO NOTHING`, pre-check retained, stable insert order. No swallowed uniqueness exception in an unusable transaction. Status now evaluates missing versions and required-type completeness from one current-set query. |
| POL-D02 unsupported edit status | P2 | Unsupported methods should be client errors; generic handler converted Spring's exception to 500. No mutation occurred. | Preserve 405 plus Allow header for unsupported methods and 404 for unmapped paths. PUT/PATCH collection: 405; ID resource without mapping: 404. |
| POL-D03 missing-type success message | P2 | Four available policies accepted while a fifth is absent must remain incomplete; UI claimed all required policies reviewed while backend returned false. | Policy centre checks backend completeness and all five displayed types, and shows a missing-set notice. Privacy wording unchanged. |

The initial repository-spy barrier, owner-verification browser setup, and paid cancellation/refund mock needed harness corrections. They are not classified as production defects. The latter uses the actual value/currency DTO order and the existing confirmation cancellation snapshot. No cancellation or refund production code changed.

## Gated mutation matrix

All listed rows were exercised through authenticated HTTP with a policy-specific 403 after replacement. Email verification was independently satisfied. Inquiry response checks use an unknown inquiry ID deliberately because policy enforcement precedes lookup; they prove the policy gate, not resource ownership or full inquiry success. Existing inquiry tests retain those separate contracts.

| Action | Endpoint | Actor / implementation |
|---|---|---|
| New booking request | POST /api/bookings | SCOUT; BookingService.createBooking; existing active venue |
| Inquiry creation | POST /api/availability-inquiries | SCOUT; AvailabilityInquiryService.create |
| Inquiry offer | POST /api/availability-inquiries/{id}/offer | OWNER; AvailabilityInquiryService.offer |
| Inquiry decline | POST /api/availability-inquiries/{id}/decline | OWNER; AvailabilityInquiryService.decline |
| Inquiry acceptance / exception booking | POST /api/availability-inquiries/{id}/accept | SCOUT; AvailabilityInquiryService.accept then BookingService.createApprovedExceptionBooking |
| Booking acceptance | POST /api/bookings/{id}/accept | OWNER; BookingService.acceptRequest |
| Booking decline | POST /api/bookings/{id}/decline | OWNER; BookingService.declineRequest |
| Checkout | POST /api/bookings/{id}/checkout | SCOUT; PaymentService.createPayment |
| Payout-destination submission | POST /api/owner/payout-destinations | OWNER; OwnerPayoutDestinationService.submit |
| Venue review submission | POST /api/venues/{id}/submit-for-review | OWNER; VenueModerationService.submit |

`requireCurrentPolicies` bypasses ADMIN. The all-five status calculation itself does not bypass ADMIN. No new gate was added, removed or weakened.

## Historical rights and exact-version integrity

The PostgreSQL test creates a booking through the real booking API after v1 acceptance. It checks the actual four stored references, excludes Privacy, replaces Privacy to invalidate acceptance, and later replaces/retires Terms to prove an original booking-relevant reference stays unchanged. Historic content and hashes remain unchanged through lifecycle tests. The V11 foreign keys retain original policy records.

Authenticated SCOUT/OWNER booking list/detail and incident-history reads remain reachable. Login and anonymous venue browsing work. Populated owner payout list/detail and populated incident history remain accessible after replacement; destination-history reads also remain accessible. Unpaid cancellation and cancellation preview remain available. An eligible paid fixture downloads a real PDF receipt, then exercises paid cancellation and queued refund-status access using a mocked refund provider. This tests local policy compatibility with financial rights, not provider integration. Direct full refund initiation remains ADMIN-only under its existing rule. No policy checks are wired into financial-document, payout-history, incident, cancellation, refund or authentication services.

After reviewing the replacement, a new booking request and owner decline succeed through the API. The real browser additionally proves an OWNER payout submission succeeds after review. Policy lifecycle and acceptance publish no domain notification events; the integration test compares the event stream before/after. Successful actions in other domains retain their normal email event behavior, with external delivery disabled/mocked locally.

## Concurrency evidence boundaries

- Three duplicate-acceptance repetitions force both real HTTP transactions past the same absent-row pre-check. Both return 200; PostgreSQL contains exactly five user/version rows. Repeating acknowledgement sequentially preserves row IDs, timestamps and request metadata. The unique database constraint is the authority.
- Three acceptance/publication repetitions pause after the actual current-set query. ADMIN commits a replacement before acceptance resumes. Five original acceptances may commit historically; the next gate rejects. Accepting the replacement yields six historical rows and permits the gate. No stale rows are deleted.
- Three service/HTTP competing-publication repetitions use two drafts replacing an existing current version, with a barrier after the current-version read. Exactly one request returns 200 and the other 409; one current remains and the losing draft remains draft. The existing SQL-only critical-path race separately checks first publication without a previous current version.
- No timing sleeps are used to arrange database races. Spy barriers delegate to real Spring Data repository queries. This does not claim that publication is serialized with every entire downstream booking/payment transaction; the tested contract is re-evaluation at the next gate.

## Browser and client evidence

[Browser evidence](evidence/policy/browser.json) and screenshots use real Chrome, the built Next app, HTTP and a dedicated PostgreSQL database. The final browser suite has eight check groups. A prior complete seven-group run also passed; the final repeat explicitly re-exercises the empty state. Local synthetic content is shown exactly as plain text, including literal `<b>` markup; no HTML element is created. Privacy uses explicit unchecked review/acknowledgement text. No production legal text was created.

The UI intentionally requires selecting every remaining unaccepted card before submitting; API subsets are supported and displayed correctly. No-policy, missing-type, full-set, subset, all-accepted and replaced-version states were exercised. A stale OWNER payout form receives one real 403; the client refreshes global status and renders the review link. After review, submission succeeds. ADMIN uses the real create form and native confirmation dialogs to publish, replace and retire; historical rows remain read-only after reload.

[Client evidence](evidence/policy/client.json) runs the actual TypeScript client/auth/error modules with controlled transport: three repetitions each for policy 403 versus ordinary ownership 403. Policy errors emit one `eema:policy-stale`; ownership errors do not. Neither triggers token refresh or mutation replay. This complements, rather than substitutes for, the browser evidence.

## WHAT ELI SHOULD TEST PERSONALLY

- Is the policy centre understandable, including missing or replaced versions?
- Are explicit review controls clear and non-coercive?
- Does the stale-policy notice clearly explain the next step?
- Does admin publication/retirement feel deliberate and safe?

No manual hash checks, row counting, uniqueness inspection or concurrency reproduction is needed.

## LEGAL/BUSINESS INPUT STILL REQUIRED

- Final policy content and approval for publication; Privacy review/acknowledgement semantics.
- Effective dates and version/title conventions. Software publication is immediate; effectiveAt is metadata.
- Retention requirements for immutable policy, acceptance and booking history.
- Whether all five are required for every marketplace role, or role-specific sets should differ.

**LEGAL REVIEW REQUIRED.** Software tests cannot approve policy content or Privacy semantics. No production policy publication was performed.

## STAGING ONLY

- Verify deployed HTTPS policy navigation and deployment-specific authorization/CORS.
- Verify the migration/unique indexes on the production-like PostgreSQL deployment.
- Check deployed cache behavior and admin account access; approved policy publication belongs to the controlled deployment/legal process.

Policy flows have no external provider dependency. No policy-specific email-provider staging test is invented.

## Scenario disposition and verification

| Scenario | Deterministic result | Scope closed | Remaining sign-off |
|---|---|---|---|
| POL-001 | PASS | All-five lifecycle, exact content/hash/time, immediate future-date publication, authorization, admin UI | Approved production content/publication process |
| POL-002 | PASS | No edit routes, supported client-error responses, illegal transitions, duplicate type/version, immutable history | Version/title conventions |
| POL-003 | PASS | Exact IDs, subset/duplicate/invalid requests, sequential and concurrent idempotency, all-five completeness | Business choice of role-specific required sets |
| POL-004 | PASS | Ten gated HTTP mutation surfaces; historical reads, populated incidents/payout history, cancellation/refund/receipt; four immutable booking references; review/retry succeeds | Product review of gate explanation |
| POL-005 | TECHNICAL PASS / LEGAL BLOCKED | Six policy-centre states, plain text, unchecked explicit controls, neutral Privacy acknowledgement, admin UI | Final content and Privacy semantics; LEGAL REVIEW REQUIRED |
| POL-006 | PASS | Real PostgreSQL acceptance/publication, duplicate acceptance and competing replacement publication; three repetitions each | None for deterministic concurrency |

Counts: **6 technical PASS, 0 technical FAIL, 0 technical BLOCKED**. If a scenario requires all human/legal sign-off to count as PASS, the roll-up is **5 PASS, 0 FAIL, 1 BLOCKED (POL-005 legal)**. Three defects fixed: **0 P0, 1 P1, 2 P2**. No unresolved POLICY software defect remains.

| Verification | Result |
|---|---|
| `mvn test` | PASS — 226 tests, no failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS — 55 tests, including 14 policy executions; no failures/errors/skips |
| Final focused policy rerun | PASS — 14 executions, including populated incident history and nine barrier race executions |
| Final historical-rights follow-up | PASS — adds populated payout list/detail assertions; all gate, snapshot, cancellation/refund, incident and receipt checks pass |
| Actual Chrome browser | PASS — 8 final groups; prior complete 7-group run also passed; SCOUT, OWNER and ADMIN |
| Actual TypeScript client with controlled transport | PASS — 6 checks (policy/ownership distinction repeated three times) |
| Frontend `npm run typecheck`, `npm run lint`, `npm run build` | PASS — no lint/type errors; production build completed |
| Both repositories `git diff --check` | PASS |
| `mvn clean package` | PASS — packaged executable JAR; 226 unit tests rerun, no failures/errors/skips |
| Master-plan Markdown/PDF | POL-001–006 corrected; PDF regenerated (693 pages); all six corrected headings/legal state and final document content checked; policy page visually inspected |

Counts describe their own test layers; they should not be added together as unique business scenarios. Sanitized per-suite evidence is in [automated.json](evidence/policy/automated.json). Raw test logs were not copied into the repository. The dedicated tmpfs browser PostgreSQL environment was disposed of after evidence capture, and only the backend/frontend QA processes started for this execution were stopped. The dedicated QA browser profile was also removed. Shared history and production policies were untouched.

**POLICY is locally clear to move to the next domain.** This is a software execution decision, not legal approval or production launch clearance. Eli does not need to rerun deterministic policy tests manually.
