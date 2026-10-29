# READY execution report

Audit/inventory recorded before READY code or test changes, 2026-09-12. Final execution results are recorded below. Existing AUTH/POL/TRUST/VEN work is preserved.

## Pre-change inventory

All twelve generated scenarios are P1. Actual mutation actors are the owning verified-email OWNER for submission and ADMIN for approval/reactivation. Submission fixture already has active account, verified owner review and current policies; admin actions do not require admin policy acceptance. No repeated unrelated Scout/ownership matrix is needed: VEN covers it.

A means absence can occur through supported persisted editing; B means public DTO forbids omission but internal/legacy fixtures can represent it; C means database invariants prohibit that absent persisted state. C still allows an in-memory defensive validator/service test, never an invalid database row.

| Scenario | Authoritative key | Classification | Existing automation / missing coverage |
|---|---|---|---|
| READY-001 | `maxCrewSize` | A: nullable persisted field | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-002 | `permittedActivities` | A: null or empty persisted array | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-003 | `productionRules` | A: null, empty or blank persisted text | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-004 | `naturalLight` | A: nullable persisted Boolean; false is complete | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-005 | `loadingAccess` | A: nullable persisted Boolean; false is complete | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-006 | `bathroomsAvailable` | A: nullable persisted Boolean; false is complete | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-007 | `soundRecordingSuitable` | A: nullable persisted Boolean; false is complete | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-008 | `hourlyRateEur` | C: DB NOT NULL/positive; internal fixture only | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-009 | `minimumBookingMinutes` | C: DB NOT NULL/positive; internal fixture only | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-010 | `minimumLeadTimeMinutes` | B: current DTO required; V7 deliberately permits legacy NULL | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-011 | `timezone` | C: DB NOT NULL plus btrim(timezone) <> empty string; internal fixture only | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |
| READY-012 | `photoPaths` | A: empty persisted array; null is C (DB NOT NULL) | Existing validator smoke tests; VEN validation/tri-state and selected moderation revalidation. Missing exact parameterized single-issue/restore and workflow coverage. |

## Source-derived contract before testing

`VenueProductionProfileValidator.readinessIssues` returns an immutable ordered list in the key order above. It accumulates all issues. Numeric fields are checked for null, not bounds; timezone for null/blank, not ZoneId validity. DTO/entity/DB/media validation supplies those separate invariants. ZoneId accepts UTC/fixed offsets as well as IANA region names, so the generated IANA-only wording is too strict.

Required: maxCrewSize; nonempty permittedActivities; nonblank productionRules; known naturalLight/loadingAccess/bathroomsAvailable/soundRecordingSuitable; hourlyRateEur; minimumBookingMinutes; minimumLeadTimeMinutes; nonblank timezone; at least one photo reference. Readiness does not fetch storage. Optional facts, fullDayRateEur and availability are not requirements. ACTIVE means moderation-approved, not necessarily available for a requested slot.

Submission, approval and reactivation invoke the same validator under the existing venue row lock before status/metadata/audit/notification changes. Allowed PENDING/SUSPENDED owner edits can clear the nullable critical fields without changing those statuses, giving real API revalidation paths. VEN already proves critical edit versus approval atomicity and matching distinctions.

Owner UI maps all twelve keys to readable labels and renders Spring's list, without local completeness logic. Admin UI currently renders raw keys. Owner submit catches 409 then reloads through a function that clears its error; investigate in the representative browser run before deciding whether a fix is needed.

## Corrections and isolation

Replace generic prerequisites, duplicated API/DB expectations, unrelated actor variants, “LOCAL MOCKED” and fictional snapshot reset. Use one canonical ready fixture, parameterized validator/service coverage, real PostgreSQL for reachable transitions and representative browser states. No invalid persisted C rows, disabled constraints, new required facts or provider calls from readiness. Use unique fictional RUN_ID fixtures in disposable Testcontainers/dedicated tmpfs browser database; dispose the whole QA environment, never shared history. Only actual successful transitions emit lifecycle notifications; delivery remains staging-only.

## Authoritative field details and tested boundaries

| Field / issue key | Readiness evaluation | Separate public validation |
|---|---|---|
| maxCrewSize | null is missing | Optional draft value; if provided 1–10,000 (DB positive) |
| permittedActivities | null and [] both incomplete, retain distinct values | Seven supported enum values; duplicates/overlap invalid; VEN tests reused |
| productionRules | null, empty, Java isBlank whitespace incomplete | Max 5,000; no invented content-quality rule |
| naturalLight | null missing; false/true complete | Nullable Boolean |
| loadingAccess | null missing; false/true complete | Nullable Boolean |
| bathroomsAvailable | null missing; false/true complete | Nullable Boolean; bathrooms are not mandated |
| soundRecordingSuitable | null missing; false/true complete | Nullable Boolean; suitability is not mandated |
| hourlyRateEur | null missing | Required; 0.01–99,999,999.99; DB NOT NULL/positive |
| minimumBookingMinutes | null missing | Required; 1–10,080; DB NOT NULL/positive |
| minimumLeadTimeMinutes | null missing | Required current DTO; 60–43,200; DB allows legacy NULL |
| timezone | null/blank missing | Required update; ZoneId validity through media/settings service; DB NOT NULL plus btrim(timezone) <> empty string |
| photoPaths | null/empty missing | DTO requires nonnull; DB NOT NULL; valid Eema stored references, max 12 |

Readiness is deliberately a completeness check. An invalid non-null number or nonblank invalid ZoneId in an impossible internal fixture is not made valid by an empty readiness list. Public DTO, media policy, entity and database validations run at their own boundaries. Numeric tests explicitly distinguish presence from null/negative/zero/minimum/maximum DTO results. Existing VEN tests cover invalid ZoneId HTTP 400 with unchanged rows and valid Amsterdam/New York/UTC/fixed-offset persistence. No forced invalid persisted rows were created.

Optional: blackoutPossible, parkingAvailable, parkingSpaces, liftAvailable, wheelchairAccessible, ceilingHeightM, floorNumber, powerDetails, prohibitedActivities, fullDayRateEur, clipUrl, description and tags are not additional readiness keys. Other base listing fields may still be required by DTO/schema. Weekly hours/available dates are not readiness requirements. The canonical fixture leaves optional production facts/full-day pricing unset and all four required Booleans false, yet is complete.

Photo readiness checks reference-list presence, not remote object existence or content. Validator fixtures use Eema-shaped UUID references; PostgreSQL reuses the existing media-policy-valid fixture; the browser uploads a real fictional QA image. No readiness call accesses storage. Storage reachability remains deployment verification.

## Automation and results

`ReadyVenueFixture.complete()` is the central readiness baseline. `VenueReadinessContractTest` adds 43 test invocations: 18 single-issue variants, 18 parameterized service variants, four Boolean cases, canonical optional/null regression, full-key/purity contract, and numeric validation boundary matrix. Each service variant exercises submission, approval and reactivation, so 54 rejected/restored service transition cases are covered without pretending impossible C fixtures are valid persisted venues.

Each single-issue test starts complete, changes one value, asserts exactly one issue, snapshots every entity field before/after evaluation, restores the original and expects empty. The combined-missing test checks all twelve keys, source order, accumulation and immutable output. This locks the current stable presentation order; consumers should still use issue identifiers, not positional meaning. The validator has no repositories, event publishers or provider dependencies.

`VenueDomainIntegrationTest.readinessMissingFieldBlocksSubmitApproveAndReactivateUntilRestored` adds 10 PostgreSQL invocations: seven nullable required fields, empty activities, blank rules and empty photos. Every invocation performs the full reject/restore sequence for DRAFT submission, PENDING approval and SUSPENDED reactivation. These are 30 persisted reject/restore pairs, using supported owner PUT operations, real HTTP/controller/service/database paths, and common verified owner/current-policy fixtures. Both owner/admin DTOs return the same exact single issue. Rejected transitions return 409 and preserve the complete venue row (including timestamps/reviewer/status), audit-event count and notification count. Restoration removes the issue and each successful transition adds exactly one expected lifecycle effect.

C fields use defensive service fixtures only; B lead time uses the service fixture and current DTO boundary checks. No database constraint was disabled or invalid row forced. Full VEN tests still cover current-email/policy/owner-review gates, pricing/media consistency, matching, empty availability and atomic critical edit versus admin approval. READY adds no duplicate race test because the VEN PostgreSQL barrier-controlled transaction test already proves that shared locking path.

### Events and email

Failed readiness occurs before status/metadata writes, audit append or event publication. Service spies and real PostgreSQL event/row assertions prove no submitted/approved/reactivated notification on refusal. Successful transitions retain the established AFTER_COMMIT notification pipeline. The full regression suite reuses TRUST listener/disabled-email coverage; the dedicated browser backend runs with notifications disabled and skips provider delivery. No real Resend delivery is claimed.

### Frontend mapping and representative browser

`lib/venue-readiness.ts` contains presentation labels only. Both owner and admin render `readinessIssues` supplied by Spring. `ready-client-check.cjs` executes the TypeScript mapping with the already-installed compiler and compares its exact twelve keys to the backend readiness fixture; no new framework or client-side readiness calculator was added. Unknown future keys retain the existing fallback; the exact-set contract catches an uncoordinated new backend requirement.

Browser runner `scripts/qa/ready-browser.cjs` reuses the established Chrome CDP/ws harness against real Spring and an isolated PostgreSQL database. It covers a draft with eight simultaneously missing reachable requirements, exact readable owner list, safe 409 submission with DRAFT retained, actual editor corrections and photo upload, all four required Booleans No, checklist clearing after Spring refresh, successful PENDING submission, a competing owner edit while admin approval is open, 409 with authoritative incomplete PENDING refresh, and restored field followed by successful ACTIVE approval. Protected pricing/timezone absence is covered through service tests, not twelve artificial browser variants.

### Defects

| ID | Severity | Expected / actual before fix | Reproduction / impact | Fix |
|---|---|---|---|---|
| READY-D01 | P2 | Failed submission explains conflict; refresh cleared the error | Click submit on incomplete draft. Backend 409 and checklist correct, but errorVisible=false after reload. Owner could not tell why the click failed. | Set the action error after the awaited authoritative refresh; retain Spring checklist/state |
| READY-D02 | P2 | Readiness issues use readable labels; admin displayed productionRules | Open approval, owner clears rules, confirm: 409 refresh showed raw backend identifier. | Extract existing twelve owner labels and reuse in admin; no duplicated completeness logic |

Both were reproduced before editing production code; [ui-before.json](evidence/readiness/ui-before.json) and the before screenshots record the results. No validator, moderation, trust, policy, matching, availability, booking, pricing or storage business rule was changed.

## WHAT ELI SHOULD TEST PERSONALLY

- Is the checklist understandable and does it explain what to fix before review?
- Is No clearly different from Not specified?
- Does the blocked submission explain the next step without feeling frustrating?
- Does fixing a field visibly remove its issue?

No manual twelve-field clearing, database inspection or concurrency reproduction is needed.

## PRODUCT / OPERATIONS QUESTIONS

The existing VEN question remains open: ADMIN direct critical edits bypass owner re-review and can leave an ACTIVE venue incomplete. Admin approve/reactivate do **not** bypass readiness; those paths are now explicitly proven. Decide whether direct operational edits should retain that override or require a separate audited workflow. READY does not silently change it.

Availability and currently optional production facts are not required today. Category-specific or future expanded readiness would require an explicit product decision; there is no new blocker merely because these possibilities exist. No new availability/capability requirement was invented.

## STAGING ONLY

- Uploaded photo objects/public URLs using the deployed storage configuration.
- Real moderation lifecycle email delivery and owner links.
- Deployed authorization/CORS and production-like migration/configuration validation.

The calculation, issue mapping and transaction gating are locally provable; staging is not needed to repeat the twelve readiness omissions. No external KYC, legal approval or AI provider is part of readiness.

### Regression harness repair

The first full regression run exposed READY-H01 (P2 test reliability), a pre-existing VEN test-harness race: Mockito failed while installing a shared repository stub (`InvocationContainerImpl.setMethodForStubbing`, with suppressed `UnfinishedStubbingException`). The ten READY PostgreSQL invocations passed in that run. Asynchronous notification reads can overlap mutable spy configuration; this was a harness failure before the competing production transactions started, not a readiness/locking failure.

The VEN race harness now uses a test-only repository interceptor with an atomically armed barrier, preserving the real Spring Data repository and database locks. The final assertion requires both transactions to cross the barrier. No production scheduler, transaction, notification or locking behavior was disabled. The focused nine race repetitions passed after replacement, followed by the full regression rerun recorded below. [Harness reproduction](evidence/readiness/harness-before.json).

## Final verification and disposition

| Check | Result |
|---|---|
| READY-001–012 | **12 PASS / 0 FAIL / 0 locally BLOCKED** for corrected deterministic contracts |
| `mvn test` | PASS — 271 tests, including 43 READY invocations; 0 failures/errors/skips |
| `mvn verify -Ppostgres-integration` final rerun | PASS — 201 tests; 0 failures/errors/skips |
| READY PostgreSQL subset | 10 invocations covering 30 rejected/restored persisted transition pairs |
| Reused concurrency after harness repair | Focused 9 repetitions passed, then full regression reran them with both barrier arrivals asserted |
| `mvn clean package` | PASS — 271 tests rerun and executable jar packaged |
| Frontend typecheck / lint / production build | PASS; lint has no warnings/errors |
| Shared frontend issue mapping | PASS — exact 12 keys, both views use the shared mapping |
| Real representative browser journey | PASS — 7 assertion groups after fixes |
| Both repositories `git diff --check` | PASS |
| Master plan/PDF | Twelve scenarios corrected; 685-page PDF regenerated and READY page visually inspected |
| Evidence/cleanup | Sanitized fictional JSON/screenshots; dedicated QA database/processes/profile/media removed |

See [validation.json](evidence/readiness/validation.json), [PostgreSQL totals](evidence/readiness/postgres-full.json), [browser assertions](evidence/readiness/browser.json) and [label contract](evidence/readiness/client.json). The integration profile intentionally skips units; they ran separately and again during clean package. Test invocations, internal parameter loops and browser groups are stated separately to avoid inflating counts.

**READY is locally clear to proceed.** All twelve omissions are automated at their valid boundaries. The two UI defects and one test-harness defect were fixed; no readiness requirement or marketplace business rule changed. The human/operations/staging items above remain separate from software PASS. Existing AUTH/POL/TRUST/VEN working changes were preserved.
