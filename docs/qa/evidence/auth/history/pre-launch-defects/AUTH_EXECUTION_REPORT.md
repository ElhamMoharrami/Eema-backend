# AUTH execution report

Scope: all **15** AUTH master scenarios, AUTH-001 through AUTH-015. Initial inventory and source audit completed before repository changes for this execution pass. Source is authoritative; prior PASS is historical until rerun here. No real provider calls are needed for local checks.

## Inventory before corrections/execution

| ID / title | Priority | Initial status | Environment | Existing automation at discovery | Human requirement |
|---|---|---|---|---|---|
| AUTH-001 — Register SCOUT | P0 | Prior LOCAL PASS; staging NOT RUN | LOCAL REAL POSTGRES + browser where applicable | RegistrationIntegrationTest | UX judgment; delivery staging only where applicable |
| AUTH-002 — Register OWNER | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | directRegistrationAlsoCanonicalizesAndAllowsOwner (partial) | UX judgment; delivery staging only where applicable |
| AUTH-003 — Duplicate and normalized email | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | duplicateCaseVariantIs409WithoutAdditionalTokensOrEvents | UX judgment; delivery staging only where applicable |
| AUTH-004 — Registration validation | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | RegistrationIntegrationTest / AuthInputContractTest (partial boundaries) | UX judgment; delivery staging only where applicable |
| AUTH-005 — Login credentials and inactive account | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | AuthServiceTest / normalizedLoginPreservesPasswordSpacesExactly (partial) | UX judgment; delivery staging only where applicable |
| AUTH-006 — JWT and database role authority | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | JwtServiceTest (issuer only; missing filter matrix) | UX judgment; delivery staging only where applicable |
| AUTH-007 — Reload, logout and cross-tab state | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | Prior AUTH-001 browser evidence (SCOUT only) | UX judgment; delivery staging only where applicable |
| AUTH-008 — Refresh valid, expired, reused and revoked | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | AuthServiceTest.refreshRotatesAndRejectsReuse (mock); SQL lock race | UX judgment; delivery staging only where applicable |
| AUTH-009 — Frontend refresh failure and one retry | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | Source only | UX judgment; delivery staging only where applicable |
| AUTH-010 — Same-tab versus cross-tab refresh race | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | SQL lock race only; no browser coordination proof | UX judgment; delivery staging only where applicable |
| AUTH-011 — Recovery enumeration resistance | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | AccountRecoveryServiceTest / recoveryUsesCanonicalEmailAndKeepsGenericResponses (partial) | UX judgment; delivery staging only where applicable |
| AUTH-012 — Verification token lifecycle | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | AccountRecoveryServiceTest (mock lifecycle) | UX judgment; delivery staging only where applicable |
| AUTH-013 — Verification UI and mutation enforcement | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | VerifiedEmailEnforcementSurfaceTest (four annotation checks only) | UX judgment; delivery staging only where applicable |
| AUTH-014 — Password reset end to end | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | AccountRecoveryServiceTest (mock reset) | UX judgment; delivery staging only where applicable |
| AUTH-015 — Issued access token after password reset | P0 | NOT RUN | LOCAL REAL POSTGRES + browser where applicable | Source only | UX judgment; delivery staging only where applicable |

## Source audit and substantive corrections

- All seven auth POST endpoints are public; `/api/auth/me` requires authentication. Removed ownership-denial variants from public flows and prior verification/policy prerequisites from registration/login/recovery.
- Kept AUTH-001 registration/hash mechanics as shared coverage; OWNER-specific checks assert no owner-verification row, venue, payout destination or policy acceptance. GET `/owner/verification` synthesizes UNVERIFIED with null ID until submission.
- Email normalization and strict JSON follow the already-hardened six DTOs. Names trim only on registration; secrets stay exact. Password boundaries distinguish Java character validation from BCrypt's byte limit rather than inventing complexity rules.
- Database assertions now contain persisted state only. Successful login/refresh create token rows, not emails. Registration/resend/forgot publish eligible notification events even when provider delivery is disabled.
- Logout has no server endpoint: clearing browser storage does not revoke captured refresh tokens. Multiple logins are independent sessions.
- Reset deletes all refresh-token rows; it does not set revoked on retained rows. Previously issued access JWTs remain valid until expiry while the current DB account stays active.
- JWT filter uses current DB role/active state; UserDetailsServiceImpl/UserPrincipal are not the principal construction path used by JwtAuthenticationFilter. JWT role claims cannot override DB authority.
- Reload uses cached localStorage. Same-tab refresh shares a promise; cross-tab refresh has no lock and can clear shared state after a losing refresh. Second 401 after successful refresh does not clear session automatically. These are characterized architectural/UX observations, not silently redesigned.
- Removed nonexistent snapshot restores; use unique RUN_ID fixtures and disposable Testcontainers DBs. Local browser recovery links use fresh random tokens whose hashes are inserted only for generated QA users; raw tokens stay in memory and out of screenshots/evidence.

All corrected scenarios started NOT RUN for this pass; executed results are below. The inventory above preserves discovery state.

## Final results

**15 scenarios discovered and re-audited: 13 LOCAL PASS, 2 FAIL, 0 BLOCKED.** All 15 have locally executable checks and were executed; none is exclusively staging-only. Five separate staging check groups below remain NOT RUN. Human UX review is separate from deterministic status. AUTH-001 through AUTH-015 were corrected/clarified; all 14 generic scenarios were rewritten with explicit responsibilities. No non-AUTH scenario or acceptance gate was automatically passed.

At discovery, 12 scenarios had some existing automated tests (usually partial), AUTH-007 had prior one-off browser evidence, and AUTH-009/AUTH-015 had source-only support. Added 15 PostgreSQL tests in two focused classes, one exact-expiry unit test, a shared PostgreSQL support class, 9 client transport checks, and a repeatable 15-check browser runner using existing Chrome/ws. Reused AUTH-001's 11 registration tests rather than duplicating them for OWNER.

| Scenario | Result | Executed evidence / test mapping | What was proved / limitation |
|---|---|---|---|
| AUTH-001 | LOCAL PASS | RegistrationIntegrationTest (11 methods); browser SCOUT registration + /me + reload | One canonical active/unverified SCOUT; BCrypt/refresh/verification hash correspondence and atomicity proved. |
| AUTH-002 | LOCAL PASS | AuthSessionIntegrationTest.ownerRegistrationCreatesNoTrustOrMarketplaceAggregates; browser OWNER journey | OWNER active/unverified; synthetic UNVERIFIED DTO with null id; zero trust, venue, payout-destination and acceptance rows. |
| AUTH-003 | LOCAL PASS | RegistrationIntegrationTest duplicate test; AuthSessionIntegrationTest.inactiveDuplicateStillConflicts; PostgreSQL email index race | Canonical duplicates, including inactive original, conflict; no extra token/event. |
| AUTH-004 | FAIL | AuthSessionIntegrationTest.registrationBoundaryMatrix; AuthInputContractTest; live defects.json AUTH-D01 | ASCII/email/name/role/strict JSON boundaries pass; accepted Unicode password has a colliding different password. |
| AUTH-005 | LOCAL PASS | AuthSessionIntegrationTest.loginMatrixAndAnonymousMe; normalizedLoginPreservesPasswordSpacesExactly; browser SCOUT/OWNER login | SCOUT/OWNER/isolated ADMIN login; wrong/unknown/inactive/blank/malformed; canonical email and exact ASCII spaces. |
| AUTH-006 | LOCAL PASS | AuthSessionIntegrationTest.jwtValidationAndDatabaseAuthority; browser expired-access check | Bad JWT variants401; role claim cannot override DB; promotion /me ADMIN, protected missing detail404; demotion403/inactive401. Separate admin list defect recorded below. |
| AUTH-007 | LOCAL PASS | Browser SCOUT/OWNER registration/login/reload/logout/cross-tab checks | Reload + /me agreement; sign-out clears both tabs; captured refresh still works because logout is local only. |
| AUTH-008 | LOCAL PASS | AuthSessionIntegrationTest.independentSessionsAndRotation / refreshFailureMatrix / concurrentHttpRefreshHasOneWinner; existing SQL race | Independent sessions, one token rotation winner, revoked/expired/reused/unknown/inactive failures; real rows inspected. |
| AUTH-009 | LOCAL PASS | client.json seven refresh outcome variants; browser actual expired-access refresh | One refresh/one retry; failed/malformed refresh clears session; second401 retains new session as current behavior. |
| AUTH-010 | LOCAL PASS — characterization | client.json same-tab five requests / two contexts; browser actual two-tab paused refresh | One refresh per tab; real race200/401 and shared session cleared by loser. Known gap, not an approval of cross-tab UX. |
| AUTH-011 | LOCAL PASS | AuthRecoveryIntegrationTest.enumerationEligibilityMatrix; normalized recovery test; browser forgot and owner resend | Generic known/unknown/inactive/verified responses; only eligible tokens/events. Hash correspondence inspected. |
| AUTH-012 | LOCAL PASS | AuthRecoveryIntegrationTest.verificationLifecycle; AccountRecoveryServiceTest.recoveryTokensRejectExactExpiryEquality; browser valid/missing/invalid/expired | Verification persists; replacement/inactive/expiry failures; used verified token idempotent, including after expiry. |
| AUTH-013 | LOCAL PASS | AuthRecoveryIntegrationTest.verifiedMutationBoundary / policyAcceptanceProjectionReturnsOnlyTheUsersVersionIds; browser verification | Unverified writes403; verified owner submits; verified scout without policies403 after the projection fix. UI /me refresh agrees. |
| AUTH-014 | FAIL | AuthRecoveryIntegrationTest.resetLifecycleAndBoundaries / resetRejectsExpiredInactiveReplacedAndUnknownTokensWithoutMutation / resetMustNotMakeDistinctUnicodePasswordsEquivalent; browser reset matrix | ASCII lifecycle/UI/expiry/revocation pass; Unicode reset hits same AUTH-D01 collision. |
| AUTH-015 | LOCAL PASS — characterization | resetLifecycleAndBoundaries; jwtValidationAndDatabaseAuthority; browser retained-JWT check | Reset deletes refresh rows; previously issued unexpired access JWT200, expired/inactive401. No immediate access revocation claim. |

The two failed scenarios share one security defect; they are not two unrelated password bugs. LOCAL PASS for a characterization means the documented current behavior was exercised, not that a security/product decision has been approved. AUTH-002 specifically passed both real PostgreSQL and browser execution; owner registration grants no trust, venue, bank or policy state.

## Defects and security observations

### AUTH-D01 — accepted Unicode passwords become equivalent (P0, OPEN)

- **Expected:** two different accepted passwords must not authenticate as the same credential.
- **Actual:** registration accepts a 72-character / 144-byte UTF-8 password (201). A different 72-character / 108-byte password sharing its first 72 bytes logs in successfully (200, expected401). Password reset also accepts the first password and generates a hash that matches the different password.
- **Cause:** DTO character-length validation accepts values longer than BCrypt's effective 72-byte input. The current encoder silently ignores the suffix. This is not password-space trimming; the ASCII space-preservation tests pass.
- **Affected:** AUTH-004 registration and AUTH-014 reset; login verifies the colliding values. The defect does not create an arbitrary-password bypass: knowledge of the effective prefix is still needed. It does violate the exact-password contract and makes suffix entropy ineffective.
- **Reproduction:** `node scripts/qa/auth-defect-probes.cjs --local-qa` and the two intentionally failing PostgreSQL regressions named in the result matrix. `defects.json` records only character/byte counts, safe HTTP statuses and fictional fixture email, never passwords or tokens.
- **Disposition:** stopped these failing branches and continued independent scenarios. No password contract/algorithm change was made. Deciding how to reject or support over-limit new passwords while preserving/recovering potentially affected existing accounts requires an explicit compatibility decision. Both regression failures remain visible; they are not skipped or rewritten as passing characterizations.

### AUTH-D02 — admin booking search with absent filters (P1, OPEN, outside AUTH capability)

- **Expected:** authenticated ADMIN GET `/api/admin/bookings` with no filters returns200/page.
- **Actual:**500 with safe public message `An unexpected error occurred`; PostgreSQL cannot determine the type of null query parameter `$1` in BookingRepository.adminSearch.
- **Impact:** the admin booking operations list is unusable for this normal query. No unauthorized data disclosure was observed.
- **Reproduction:** the guarded live defect probe creates a unique SCOUT, temporarily promotes only that fixture to ADMIN, requests the list, then demotes/deactivates it in `finally`. Sanitized HTTP evidence is in `defects.json`.
- **Disposition:** admin-list branch stopped and documented; no financial/admin query redesign in this AUTH execution task. AUTH-006 uses a missing booking detail:403 as SCOUT,404 once authorized as DB ADMIN,403 after demotion, plus `/auth/me` role agreement. A 404 here proves authorization reached normal domain lookup; it is not treated as a successful booking read.

### AUTH-D03 — accepted-policy UUID projection (P0 integration blocker, FIXED)

- **Expected:** querying accepted policy-version IDs returns UUIDs; `/policies/status` returns200 and a verified scout without current policies receives403 at the transaction gate.
- **Actual before fix:**500 `QueryTypeMismatchException`: declared UUID result vs selected PolicyAcceptance entity, including an empty result set.
- **Narrow fix:** explicit JPQL `select a.policyVersionId ... where a.userId = :userId` on the existing repository method. No policy types, acceptance rules, lifecycle, schema or auth flow changed.
- **Regression:** real PostgreSQL user-isolated UUID projection test (including a fixture row), status200 checks and verified-scout403 gate. The policy row in this low-level projection test is inserted directly into the disposable fixture; it does not claim that the public API accepts draft policies.
- **Validation:** final PostgreSQL gate/projection tests pass; rebuilt local backend and reran all browser checks. This is the only production-code change made during this execution pass.

Additional observed architecture/UX limitations, not silently changed:

1. **Logout is client-only.** Captured refresh tokens remain usable until expiry/revocation/reset.
2. **Existing access JWTs survive password reset** until expiry while DB account stays active. Reset removes every refresh row but has no JWT version/revocation check.
3. **Cross-tab refresh can sign the user out.** Same-tab five requests share one refresh; two real tabs produced200/401 and the loser cleared the winner's shared localStorage. Browser screenshots/client evidence record this; no cross-tab coordination was added.
4. **Second401 after a successful refresh retains the new cached session.** One-retry behavior is bounded, but it is not automatic sign-out in this case.
5. **Recovery generic responses do not prove timing equalization.** Inactive login with the correct password intentionally returns403; wrong/unknown remains401 with the same message.
6. **Resend location:** owner verification status page shows the unverified header, but the actionable resend notice is on `/owner/venues/new` (and other guarded transaction components). The browser runner follows that actual path rather than inventing a global resend button.
7. **Recovery URL logging:** browser runs use `next start`, not the dev server's request logger. Tokens remain transient URL values and are absent from localStorage/sessionStorage evidence. Runtime scans cover configured local logging and the generated secrets, not every possible debug/edge logging configuration.

No JWT architecture, rotation policy, roles, password rules, recovery flow, marketplace authorization or database FKs were redesigned. Frontend source remains unchanged. Three concrete defects were found, one narrow defect fixed, two remain open. The client-only logout/JWT lifetime/cross-tab observations are additional known behavior, not counted as newly fixed defects.

## Verification commands and outcomes

| Command / execution | Result |
|---|---|
| `mvn test` | PASS — 226 tests, no failures/errors/skips |
| `mvn verify -Ppostgres-integration` | FAIL — 40 tests, 38 pass, two AUTH-D01 regression failures, zero errors/skips |
| `mvn clean package` | PASS — clean compilation, 226 unit tests and package; does not override PostgreSQL failures |
| Frontend `npm run typecheck`, `npm run lint`, `npm run build` | PASS; no frontend source change |
| `node scripts/qa/auth-client-check.cjs` | PASS — 9 controlled-transport checks of actual client/token source |
| `node scripts/qa/auth-browser.cjs --local-qa` | PASS — 15 browser check groups on real local frontend/API/PostgreSQL |
| `node scripts/qa/auth-defect-probes.cjs --local-qa` | FAIL as intended — reproduces AUTH-D01 and AUTH-D02; temporary admin demoted/deactivated |
| Both repositories `git diff --check` | PASS after document/test changes |

The build's default unit-test phase excludes `*IntegrationTest`; therefore a successful package does **not** override the failing PostgreSQL release gate. Testcontainers used PostgreSQL16 with common migrations and Hibernate validation. No H2, real provider request, volume reset or seed deletion was used.

## Evidence and repeatability

- [Automated test results](evidence/auth/automated.json): sanitized test names/status/counts from Surefire/Failsafe; raw XML/log bodies deliberately omitted.
- [Frontend client checks](evidence/auth/client.json): exact source modules, controlled failure/retry counts and cross-context observation.
- [Browser checks](evidence/auth/browser.json): fictional account emails, safe state/status/counts and runtime secret-scan result.
- [Live defects](evidence/auth/defects.json): registration vs different-password login status, admin500 and cleanup state.
- Screenshots: [SCOUT](evidence/auth/scout-registered.png), [OWNER](evidence/auth/owner-registered.png), [owner trust not submitted](evidence/auth/owner-verification-unsubmitted.png), [forgot generic](evidence/auth/forgot-generic.png), [verification success](evidence/auth/verification-success.png), [invalid verification](evidence/auth/verification-invalid.png), [reset success](evidence/auth/reset-success.png), [cross-tab session cleared](evidence/auth/cross-tab-session-cleared.png).

Use the startup/unique-fixture rules in [AUTH-001 guide](AUTH-001_REGISTER_SCOUT.md). Backend must be healthy, local profile/Compose PostgreSQL, email disabled. Build frontend with the dev server stopped, then run `npm run start > /tmp/eema-complete-auth-frontend-start.log 2>&1` at localhost:3000. Launch an isolated local Chrome CDP session:

```bash
google-chrome --headless --no-sandbox --disable-gpu --disable-dev-shm-usage   --disable-background-networking --remote-debugging-port=9223   --user-data-dir=/tmp/eema-complete-auth-browser about:blank
```

Then execute the Node runners above from the backend checkout; they reuse installed `ws` and frontend TypeScript, not a new browser framework. The browser runner reads local effective configuration only in memory, restricts itself to localhost/Compose local data, and creates random token hashes only for generated QA users. It does not recover raw tokens from DB or contact email providers. It stores only sanitized evidence. CDP should remain local and the isolated profile should be removed after Chrome exits.

The harness corrected three execution issues without product changes: waiting for the actual navigation destination/retrying a Chrome startup abort; locating resend on its implemented page; and restricting runtime log collection to the run window rather than overflowing Node's buffer. The policy-gate fixture was also corrected to supply an ACTIVE venue before expecting the downstream policy403; a nonexistent venue legitimately returns404 first.

Cleanup: Testcontainers databases discarded; browser sign-out/shared storage clearing proved; isolated browser profile removed; unique local fixture rows retained. The temporary admin probe was demoted and deactivated. No manual FK cleanup is required. Local raw secrets were held in process memory only; screenshot captures exclude the browser address bar, and token-bearing storage/log data was not committed.

## WHAT ELI SHOULD TEST PERSONALLY

- Registration and login labels/instructions are clear.
- Email-verification notice and resend location make sense.
- Password-reset form and success/error wording are understandable.
- Navigation after signing in/out is unsurprising.
- Mobile forms, keyboard focus and validation feedback feel usable.

These are human UX judgments. No need to repeat SQL/hash/JWT/refresh-concurrency checks manually.

## STAGING AUTH TESTS STILL REQUIRED

| Group | Required proof | Status |
|---|---|---|
| ST-AUTH-01 | Real Resend verification email: correct sender/recipient/content/expiry and CTA | STAGING ONLY — NOT RUN |
| ST-AUTH-02 | Real password-reset email and recovery journey from a delivered link | STAGING ONLY — NOT RUN |
| ST-AUTH-03 | Real HTTPS links, production-domain/base-URL redirects, browser/edge security headers | STAGING ONLY — NOT RUN |
| ST-AUTH-04 | Deliverability, spam/bounce behavior and operational handling of provider failure | STAGING ONLY — NOT RUN |
| ST-AUTH-05 | Deployed rate limiting/abuse controls, CORS and edge logging redaction under actual configuration | STAGING ONLY — NOT RUN |

These are not local failures and are not assigned to Eli as manual cryptography/database work.

## Private-alpha staging readiness

**NOT READY for a clean auth acceptance gate:** resolve AUTH-D01 and make both PostgreSQL security regressions pass. AUTH-D02 also blocks normal admin booking operations and needs a separate focused fix. Explicitly review the documented logout/JWT lifetime/cross-tab behavior before accepting it for alpha. Human UX review and five real staging/provider groups remain outstanding; none was marked PASS automatically.
