# VEN execution report

Source audit and inventory recorded before VEN code/test changes, 2026-09-12. Final execution results are recorded below. Existing AUTH/POL/TRUST work is preserved.

## Inventory before changes

| ID / generated title | Priority | Actual ownership / prerequisites | Existing coverage | Generated correction / environment |
|---|---|---|---|---|
| VEN-001 — Create minimum valid incomplete draft | P1 | OWNER creates own draft; active + verified email, no owner-review/policy prerequisite | VenueServiceTest pricing/create DTO validation | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-002 — Save, remove optional data and unauthorized update | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueServiceTest update/optional cap/media; BookingServiceTest snapshots | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-003 — Activity list semantics and contradictions | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueProductionProfileValidatorTest consistency/readiness; VenueHardRequirementEvaluatorTest activity | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-004 — Parking contradictions | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueProductionProfileValidatorTest; VenueHardRequirementEvaluatorTest parking | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-005 — Clip URL and timezone | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueServiceTest media; VenueBookingLeadTimePolicyTest; VenueAvailabilityServiceTest | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-006 — Production-critical active edit matrix | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | Partial VenueModerationServiceTest only; full critical matrix missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-007 — Non-critical active edit matrix | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | Partial service tests; independent noncritical matrix missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-008 — Venue public status and tag filtering | P1 | Public catalogue/detail ACTIVE; own OWNER or ADMIN sees non-ACTIVE; /my OWNER only | VenueService rules; HTTP full status/actor matrix missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-009 — Moderation valid sequence | P1 | Own OWNER submits with email, owner VERIFIED, current policies, readiness; ADMIN reviews | VenueModerationServiceTest mock lifecycle; TrustNotificationServiceTest | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-010 — Moderation revalidation and historical reservations | P1 | Own OWNER submits with email, owner VERIFIED, current policies, readiness; ADMIN reviews | VenueModerationServiceTest readiness; BookingServiceTest acceptance; suspension acceptance gap untested | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-011 — naturalLight: unknown/no/yes round trip | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueHardRequirementEvaluatorTest boolean/preference coverage; full HTTP/PostgreSQL/browser round trip missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-012 — blackoutPossible: unknown/no/yes round trip | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueHardRequirementEvaluatorTest boolean/preference coverage; full HTTP/PostgreSQL/browser round trip missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-013 — loadingAccess: unknown/no/yes round trip | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueHardRequirementEvaluatorTest boolean/preference coverage; full HTTP/PostgreSQL/browser round trip missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-014 — parkingAvailable: unknown/no/yes round trip | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueHardRequirementEvaluatorTest boolean/preference coverage; full HTTP/PostgreSQL/browser round trip missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-015 — liftAvailable: unknown/no/yes round trip | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueHardRequirementEvaluatorTest boolean/preference coverage; full HTTP/PostgreSQL/browser round trip missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-016 — wheelchairAccessible: unknown/no/yes round trip | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueHardRequirementEvaluatorTest boolean/preference coverage; full HTTP/PostgreSQL/browser round trip missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-017 — bathroomsAvailable: unknown/no/yes round trip | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueHardRequirementEvaluatorTest boolean/preference coverage; full HTTP/PostgreSQL/browser round trip missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |
| VEN-018 — soundRecordingSuitable: unknown/no/yes round trip | P1 | Own verified-email OWNER edits; ADMIN also edits; SCOUT/other owner denied; no policy/owner-review prerequisite | VenueHardRequirementEvaluatorTest boolean/preference coverage; full HTTP/PostgreSQL/browser round trip missing | Replace generic roles, repeated API/DB claims, provider silence and fake snapshot restore. Real PostgreSQL + isolated browser; unique RUN_ID fixtures |

## Authoritative lifecycle and boundaries

DRAFT → submit → PENDING → request-changes → DRAFT or approve → ACTIVE; ACTIVE owner critical edit → PENDING; ACTIVE → suspend → SUSPENDED → reactivate → ACTIVE. No generic status editor. All relevant writes lock the venue row; entity has no optimistic @Version.

- Draft POST /api/venues: 201, OWNER + current verified email; server assigns owner; no policy or owner-verification gate. No timezone/photo fields in create DTO: Europe/Amsterdam and empty photos are entity defaults; no weekly hours/overrides created.
- PUT /api/venues/{id}: full DTO replacement, nullable optional values clear, null activity arrays remain distinct from empty. Own OWNER or ADMIN + verified-user policy (ADMIN exempt from email prerequisite). Required timezone and photoPaths. Unknown owner/status/timestamp fields cannot bind entity state.
- Submit: policy-current checked, own OWNER, verified email, owner VERIFIED, DRAFT and readiness. Admin approve/reactivate check expected status, owner VERIFIED and readiness, not owner policy/email again. No supported trust revocation exists; do not fabricate one.
- Readiness requires maxCrewSize, nonempty permittedActivities, nonblank productionRules, known naturalLight/loadingAccess/bathroomsAvailable/soundRecordingSuitable, hourlyRate/minimumBooking/minimumLeadTime, timezone and at least one photo. Availability is not required: ACTIVE is moderation approval; no slots is a valid availability state. DTO validation protects required noncritical pricing/timezone/lead-time fields.
- CriticalProfile fields: maxCrewSize, permittedActivities, prohibitedActivities, naturalLight, blackoutPossible, ceilingHeightM, loadingAccess, parkingAvailable, parkingSpaces, floorNumber, liftAvailable, wheelchairAccessible, bathroomsAvailable, soundRecordingSuitable, productionRules, powerDetails, ordered photoPaths. Lists compare in order, null differs from empty. The audit found scale-sensitive BigDecimal.equals; VEN-D03 now normalizes only the comparison so numerically identical saves do not trigger review.
- Noncritical: name, neighbourhood, sizeM2, hourlyRateEur, minimumBookingMinutes, minimumLeadTimeMinutes, fullDayRateEur, tags, description, clipUrl, timezone and availability routes. ADMIN critical edits bypass owner-only re-review; operations decision, not permission to invent policy.
- Public catalogue only ACTIVE. Blank tag is unfiltered; requested tag trimmed/lowercased but stored tags are not normalized. Non-ACTIVE detail returns 403 for anonymous/unrelated actors; owner/ADMIN allowed. /my OWNER only, own records across statuses, unpaged. Missing UUID 404; malformed UUID 400, verified at HTTP.
- ProductionActivity: PHOTO, FILM, VIDEO, INTERVIEW, COMMERCIAL, MUSIC_VIDEO, CONTENT_CREATION. Duplicate or overlapping activity entries are 400. Parking false + positive spaces is 400; true/null/0 allowed, unknown + positive allowed by backend. Hard minimum parking spaces also requires parkingAvailable=true.
- Clip is stored as HTTP(S) text and shown in owner inputs; current public detail does not render a clip link. It is never fetched by backend; no SSRF host blacklist is warranted. ZoneId accepts valid zone IDs (including fixed offsets); retain current contract, no invented scheduler.
- Matching SQL + final evaluator fail closed for explicit hard boolean requirements. Preferences never exclude unknown values. Gemini prompt asks known matches above unknown above mismatch; actual model ordering is provider-dependent, not a deterministic ranking guarantee.
- Moderation writes SUBMITTED/CHANGES_REQUESTED/APPROVED/SUSPENDED/REACTIVATED audit and AFTER_COMMIT notification events. Critical owner edit also emits SUBMITTED. Disabled EmailService skips delivery; invalid requests emit none. Real email/object-store/provider behavior is staging-only.
- The suspension acceptance gap was reproduced and fixed (VEN-D02): the existing venue lock now protects an ACTIVE guard before REQUESTED acceptance. Existing accepted/confirmed commitments and checkout rights remain available.

## Isolation and decisions

Use disposable PostgreSQL Testcontainers for automation, a dedicated local tmpfs DB and isolated Chrome profile for browser journeys; fictional accounts/notes and clearly marked QA policy content only. Dispose whole dedicated environments; never delete shared venue/moderation history or restore a fictional snapshot.

Pending product/operations decisions: admin critical edits without re-review; activity-list ordering semantics if product wants order-insensitive comparison; operational moderation criteria. Provider rank ordering, real lifecycle email delivery and deployed media storage require staging. No new marketplace rule is introduced.

## Results and evidence

Final command totals are in [validation.json](evidence/venue/validation.json); browser assertions and fictional screenshots are in [browser.json](evidence/venue/browser.json). The new `VenueDomainIntegrationTest` runs production controllers/services/repositories against PostgreSQL 16 Testcontainers, including Flyway constraints and native enum arrays. No H2. Existing unit suites cover DTO/readiness, pricing/lead time, availability, hard requirements and shared notifications; they are reused rather than duplicated.

All 18 corrected scenario contracts are covered by the combined suite. Software results and open operations/provider decisions are separate: a passing characterization of admin overrides does not approve their business semantics. VEN-001–018: **18 PASS / 0 FAIL / 0 locally BLOCKED** for the corrected deterministic contracts. Four reproduced defects were fixed (two P1, two P2). Operations decisions and staging checks below remain open and are not counted as software PASS.

### Corrections to generated assumptions

- Venue creation is OWNER-only, email-gated, and does not require owner review or accepted policies. ADMIN can edit but cannot create through the owner create endpoint.
- Submission requires current policies, email verification, owner VERIFIED and readiness. Admin approval/reactivation recheck owner review/readiness, without requiring the admin or owner to accept policies again.
- PUT replaces the DTO fields; unknown ownership/status/timestamp fields are safely ignored and cannot assign entity state. Null and empty lists are distinct. Create has no timezone/photo fields.
- Criticality comes from the exact comparison, including ordered photos and activity lists. Tags and availability are noncritical. Required pricing/lead-time/timezone values cannot be cleared through valid requests; empty availability is allowed and makes slots unavailable without changing approval.
- Non-ACTIVE detail is forbidden to unrelated actors, not universally hidden as 404. `/my` is OWNER-only, including denial of ADMIN. Catalogue and `/my` are unpaged.
- Moderation does send lifecycle notification events. Local disabled delivery is tested; real email delivery is not a local PASS.
- The documented suspension acceptance gap was a defect to fix, not an accepted exception.
- Remove fake snapshot restores and repeated generic API/DB/actor assertions. Each test uses unique disposable fixtures; no shared history is deleted.

### Authorization and transition matrix

| Action | Anonymous | SCOUT | Own OWNER | Other OWNER | ADMIN |
|---|---|---|---|---|---|
| Create draft | 401 | 403 | 201 with verified email | Creates own only | 403 |
| Update/photo/availability write | 401 | 403 | 200 (override create 201/delete 204), verified email | 403 | Allowed |
| Submit DRAFT | 401 | 403 | 200 with all submission prerequisites | 403 | 403 |
| Admin moderation | 401 | 403 | 403 | 403 | 200 valid transition |
| ACTIVE detail/catalogue | 200 | 200 | 200 | 200 | 200 |
| Non-ACTIVE detail | 403 | 403 | 200 | 403 | 200 |
| `/venues/my` | 403 | 403 | 200 own venues | 200 their own only | 403 |

Invalid DTOs return 400, wrong moderation state 409, nonexistent venue 404 and malformed UUID 400. Denial tests compare persisted rows, trust-event counts and notification counts. Flipping authoritative DB email verification after issuing the token blocks owner mutations; reads remain available. Policy replacement blocks submission until exact current versions are accepted.

### Production profile and matching

All seven activities round-trip through actual native `production_activity[]`; null and empty remain different. Duplicates, contradictory permitted/prohibited lists and unknown enums fail safely. Tests run the actual SQL filter and final hard-requirement evaluator. The native containment function preserves the indexed PostgreSQL array contract.

All eight booleans are parameterized over null/false/true through request JSON, DTO, entity, PostgreSQL, response and browser reload. Hard requirements accept true only. All applicable preferences keep unknown values eligible. Dedicated preference fields exist for natural light, blackout, loading, parking and lift; no preference field is invented for bathrooms, wheelchair accessibility or sound recording. Known-match ranking is a Gemini prompt instruction, not a guaranteed deterministic score; live ordering remains provider validation.

Parking matrix: false with positive spaces is rejected; false with null/0, true with null/0/positive, and null with null/positive are allowed. Minimum-space matching also requires parking=true, even for a zero minimum. The owner UI clears parkingSpaces when parking is not Yes; this is distinct from the permissive backend unknown/positive contract.

Clip HTTP/HTTPS absolute URLs persist, including localhost because the server never fetches them. Unsafe schemes, relative/malformed/no-host/oversized values are rejected. Current public detail does not render a clip link. ZoneId validation/persistence is tested through PUT and availability settings, including Amsterdam, New York, UTC and fixed offset; invalid/blank/whitespace/case-invalid values fail. Existing timing suites establish downstream wall-clock and lead-time behavior without duplicating the booking domain.

### Editing, readiness and immutable history

Each critical field is changed independently on an ACTIVE venue; ordered photo addition/removal/reorder are separate cases. Results: PENDING, removed from catalogue and matching, forbidden to unrelated detail readers, still accessible by owner, one submission event. Booking rows and policy-reference rows remain byte-for-byte logically unchanged. Tests include price snapshots, duration/billable minutes and contact snapshots through full booking-row comparison; venue display name is not falsely asserted to be a booking snapshot.

Each noncritical scalar field preserves ACTIVE and emits no moderation event. Weekly hours, overrides and timezone settings do likewise. Clearing weekly availability is valid: the venue remains approved but availability-aware matching finds no requested slot. Invalid required price/lead/timezone values are rejected. Identical saves, numeric scale differences and tag order do not cause review; activity order, null versus empty activity lists and photo order currently do.

Admin critical edits bypass re-review and can leave an ACTIVE listing readiness-incomplete. This is explicitly characterized and left as an operations decision, rather than silently changing moderation policy.

### Suspension and concurrency

| State/action at suspension | Proven result |
|---|---|
| Existing REQUESTED | Preserved; new owner acceptance returns 409 |
| Existing AWAITING_PAYMENT | Preserved and checkout returns 201 before its deadline |
| Existing CONFIRMED | Booking and timestamps remain unchanged |
| New request/inquiry | 404; no new commitment |
| Catalogue/matching | Venue absent |
| Historical participant detail | OWNER and SCOUT still receive 200 |

Checkout uses the real checkout service with only Mollie transport mocked; this is not a claim of live payment delivery. CONFIRMED is seeded as the already-settled historical fixture; payment settlement is outside VEN. No bookings are automatically cancelled.

Barrier-controlled PostgreSQL races use two real transactions and venue row locks, without timing sleeps. Two admins approving, or approving versus requesting changes, produce one 200 and one 409, one coherent winning state and one winning review event/notification. Critical owner edit versus approval legally serializes to ACTIVE (edited before approval) or PENDING (edited after approval). Suspension versus acceptance serializes to preserved AWAITING_PAYMENT if acceptance committed first, or unchanged REQUESTED/409 if suspension won first. The final venue remains SUSPENDED. No 500, mixed state or contradictory duplicate review event is accepted by these assertions.

Database tests exercise owner FK, native enum validity, activity overlap, parking and positive crew constraints without disabling them. Venue has row locking rather than optimistic versioning. Queue filtering/bounds and exact moderation metadata clearing/retention are tested alongside chronological trust events.

### Notifications and browser

The full submit → changes → resubmit → approve → suspend → reactivate journey produces six trust events and six lifecycle notifications. Critical owner edits also submit for review. Shared AFTER_COMMIT listener and disabled-provider behavior are covered by existing TRUST integration tests and the full suite; the VEN browser backend records skipped delivery with notifications disabled. Focused template tests cover submission/reactivation links and escaped owner-visible reasons, alongside existing approval/changes/suspension tests. Invalid transitions and denied writes leave event/notification counts unchanged.

The actual Chrome runner uses the existing CDP/ws harness and production Next build against Spring/PostgreSQL. It covers real owner login, draft creation, all tri-state controls and reloads, real photo upload, readiness/submission, admin queue/details/changes/approval/suspension/reactivation, owner notices, price/timezone/weekly/override editing, photo ordering/removal, SCOUT detail and role guards. Each stale-admin conflict is repeated three times and must retain the actionable error after authoritative DRAFT refetch. No optimistic status assignment or mocked lifecycle API is used. Public unknown required-field fixtures intentionally use the existing ADMIN override behavior; they are not evidence that incomplete drafts pass readiness.

### Defects reproduced and fixed

| ID | Severity | Expected / actual before fix | Reproduction and impact | Narrow fix |
|---|---|---|---|---|
| VEN-D01 | P1 | Activity arrays persist/filter; actual PUT 500 and matching query type failures | Ready profile with FILM; all seven SQL filter cases. Prevented usable production profiles/matching. [Before](evidence/venue/enum-array-before.json) | Explicit enum-name array mapping/native write cast; registered typed native containment function for activity predicates |
| VEN-D02 | P1 | Suspended REQUESTED cannot become a commitment; actual accept 200/AWAITING_PAYMENT | Create request, suspend, accept. Bypassed moderation suspension. [Before](evidence/venue/suspension-and-identical-before.json) | ACTIVE check under existing venue lock in request acceptance; accepted/confirmed rights preserved |
| VEN-D03 | P2 | Numerically identical save stays ACTIVE; actual 3.5 vs stored 3.50 caused PENDING | Reload/save approved profile. Unnecessary delisting/review. [Before](evidence/venue/suspension-and-identical-before.json) | Normalize ceiling scale only for critical comparison |
| VEN-D04 | P2 | Stale review explains 409; actual refetch hid error when DRAFT has no actions | Competing admin requests changes while approval form open. User lost explanation. [Before](evidence/venue/ui-before.json) | Keep error outside action availability and discard stale action form |

No new scheduling, KYC, legal, payment, refund, payout or marketplace eligibility policy was introduced. The booking change is the explicitly requested suspension invariant. Existing AUTH/POL/TRUST changes were preserved.

## WHAT ELI SHOULD TEST PERSONALLY

- Are production “Not specified / No / Yes” controls and readiness messages understandable?
- Does re-review after critical edits explain what changed and what happens next?
- Are changes-requested and suspension reasons useful and respectful?
- Do admin confirmations and stale-review errors make decisions feel deliberate?

No manual database counts, hashes, race reproduction or deterministic role checks are needed.

## PRODUCT / OPERATIONS DECISIONS STILL REQUIRED

- Should ADMIN critical edits remain an override that can leave ACTIVE readiness-incomplete, or require an explicit audited override/re-review workflow?
- Is activity/photo ordering intentionally review-worthy? Current ordered comparison is preserved.
- Final moderation checklist, owner-facing reason conventions and review SLA remain operational choices.

These decisions are not software approval. Empty availability is already supported and does not by itself require a new status.

## STAGING ONLY

- Real submission/approval/changes/suspension/reactivation email delivery, recipient/copy and HTTPS owner links.
- Deployed authorization/CORS, media upload/public URL behavior with configured storage, and logs free of sensitive operational notes.
- Deployment migration/configuration against a production-like database; PostgreSQL 16 Testcontainers proves local schema behavior, not deployment execution.
- Live Gemini preference ordering if ranking is to be accepted operationally; no deterministic ordering claim is made from local provider-free tests.
- Multi-instance deployment smoke check of moderation/booking serialization if deployed across instances; local PostgreSQL race invariants already pass independently of JVM locking.

No invented external KYC, legal approval or payment-provider test is added to VEN.

## Final verification and disposition

| Check | Result |
|---|---|
| `mvn test` | PASS — 228 tests, 0 failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS — 191 tests, including 106 VEN invocations; 0 failures/errors/skips |
| Final PostgreSQL concurrency repeat | PASS — 9 further invocations (3 per race group); 0 failures/errors/skips |
| `mvn clean package` | PASS — 228 tests rerun and executable jar packaged |
| Frontend `npm run typecheck` | PASS |
| Frontend `npm run lint` | PASS — no warnings/errors |
| Frontend `npm run build` | PASS — production build used for browser journey |
| Real browser | PASS — 19 assertion groups, including three stale-admin conflicts |
| Both repositories `git diff --check` | PASS |
| Master-plan PDF | Regenerated, 688 pages; VEN text extracted and representative page visually inspected |
| Evidence | Sanitized fictional screenshots/JSON; no raw credentials, tokens or provider payloads committed |

The integration profile skips unit execution by design; units were separately run and rerun by clean package. The 9 repeated race invocations are additional to the 191 full integration tests, not extra distinct test methods. Browser groups include parameterized tri-state values rather than claiming 19 independent pages.

The dedicated tmpfs browser database/network and task-owned servers/profile/uploads/copied jars were removed after the successful journey. Testcontainers disposed their databases. No shared venue, policy, booking or trust history was deleted. Existing unrelated AUTH/POL/TRUST working changes remain intact.

**VEN is locally clear to move to the next domain for the corrected deterministic contract: 18 PASS, 0 FAIL, 0 locally BLOCKED.** This is not production launch approval: the explicitly documented ADMIN override/readiness decision, human UX review and deployment/provider checks remain open. Eli need not execute the deterministic matrices or PostgreSQL races manually.
