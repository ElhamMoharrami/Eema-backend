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
- Email normalization and strict JSON follow the already-hardened six DTOs. Names trim only on registration; secrets stay exact. Password boundaries retain Java character validation; Argon2id now removes the discovered BCrypt byte-prefix equivalence without adding complexity rules.
- Database assertions now contain persisted state only. Successful login/refresh create token rows, not emails. Registration/resend/forgot publish eligible notification events even when provider delivery is disabled.
- Logout has no server endpoint: clearing browser storage does not revoke captured refresh tokens. Multiple logins are independent sessions.
- Reset deletes all refresh-token rows; it does not set revoked on retained rows. Previously issued access JWTs remain valid until expiry while the current DB account stays active.
- JWT filter uses current DB role/active state; UserDetailsServiceImpl/UserPrincipal are not the principal construction path used by JwtAuthenticationFilter. JWT role claims cannot override DB authority.
- Reload uses cached localStorage. Same-tab refresh shares a promise; cross-tab refresh has no lock, but stale failures now preserve and adopt newer sessions. Second 401 after successful refresh does not clear session automatically. These are characterized architectural/UX observations, not silently redesigned.
- Removed nonexistent snapshot restores; use unique RUN_ID fixtures and disposable Testcontainers DBs. Local browser recovery links use fresh random tokens whose hashes are inserted only for generated QA users; raw tokens stay in memory and out of screenshots/evidence.

All corrected scenarios started NOT RUN for this pass; executed results are below. The inventory above preserves discovery state.

## Final results after launch-defect fixes — 2026-09-10

**15 LOCAL PASS, 0 FAIL, 0 BLOCKED.** All 15 AUTH scenarios were rerun, including real PostgreSQL and actual browser journeys. Real provider delivery remains STAGING ONLY / NOT RUN; human judgment is separate. This is clearance for private-alpha **staging validation**, not production launch or completed staging acceptance.

Historical execution was **13 LOCAL PASS / 2 FAIL**. Both failing assertions remained enabled. See [original report and evidence](evidence/auth/history/pre-launch-defects/AUTH_EXECUTION_REPORT.md). All three requested launch defects are resolved; the previously fixed policy projection remains covered and passing.

| Scenario | Result | Executed evidence / test mapping | What was proved / limitation |
|---|---|---|---|
| AUTH-001 | LOCAL PASS | RegistrationIntegrationTest (11 methods); browser SCOUT registration + /me + reload | One canonical active/unverified SCOUT; Argon2id/refresh/verification hash correspondence and atomicity proved. |
| AUTH-002 | LOCAL PASS | AuthSessionIntegrationTest.ownerRegistrationCreatesNoTrustOrMarketplaceAggregates; browser OWNER journey | OWNER active/unverified; synthetic UNVERIFIED DTO with null id; zero trust, venue, payout-destination and acceptance rows. |
| AUTH-003 | LOCAL PASS | RegistrationIntegrationTest duplicate test; AuthSessionIntegrationTest.inactiveDuplicateStillConflicts; PostgreSQL email index race | Canonical duplicates, including inactive original, conflict; no extra token/event. |
| AUTH-004 | LOCAL PASS | AuthSessionIntegrationTest.registrationBoundaryMatrix; AuthInputContractTest; live defects.json AUTH-D01 | Registration boundaries pass; two accepted Unicode passwords sharing the first 72 UTF-8 bytes authenticate only their exact value in both directions. |
| AUTH-005 | LOCAL PASS | AuthSessionIntegrationTest.loginMatrixAndAnonymousMe; normalizedLoginPreservesPasswordSpacesExactly; browser SCOUT/OWNER login | SCOUT/OWNER/isolated ADMIN login; wrong/unknown/inactive/blank/malformed; canonical email and exact ASCII spaces. |
| AUTH-006 | LOCAL PASS | AuthSessionIntegrationTest.jwtValidationAndDatabaseAuthority; browser expired-access check | Bad JWT variants401; role claim cannot override DB; promotion /me ADMIN, protected missing detail404; demotion403/inactive401. Admin list regression separately passes real PostgreSQL and local HTTP. |
| AUTH-007 | LOCAL PASS | Browser SCOUT/OWNER registration/login/reload/logout/cross-tab checks | Reload + /me agreement; sign-out clears both tabs; captured refresh still works because logout is local only. |
| AUTH-008 | LOCAL PASS | AuthSessionIntegrationTest.independentSessionsAndRotation / refreshFailureMatrix / concurrentHttpRefreshHasOneWinner; existing SQL race | Independent sessions, one token rotation winner, revoked/expired/reused/unknown/inactive failures; real rows inspected. |
| AUTH-009 | LOCAL PASS | client.json seven refresh outcome variants; browser actual expired-access refresh | One refresh/one retry; failed current generation clears; superseding session preserved. Second401 still retains new session as existing behavior. |
| AUTH-010 | LOCAL PASS | client.json same-tab five requests / two contexts; browser actual two-tab paused refresh | Controlled real two-tab refresh200/401 preserves winner session; loser remains authenticated and retries its original401 request to200. Subsequent genuine failure clears its current generation; each request retries at most once. |
| AUTH-011 | LOCAL PASS | AuthRecoveryIntegrationTest.enumerationEligibilityMatrix; normalized recovery test; browser forgot and owner resend | Generic known/unknown/inactive/verified responses; only eligible tokens/events. Hash correspondence inspected. |
| AUTH-012 | LOCAL PASS | AuthRecoveryIntegrationTest.verificationLifecycle; AccountRecoveryServiceTest.recoveryTokensRejectExactExpiryEquality; browser valid/missing/invalid/expired | Verification persists; replacement/inactive/expiry failures; used verified token idempotent, including after expiry. |
| AUTH-013 | LOCAL PASS | AuthRecoveryIntegrationTest.verifiedMutationBoundary / policyAcceptanceProjectionReturnsOnlyTheUsersVersionIds; browser verification | Unverified writes403; verified owner submits; verified scout without policies403 after the projection fix. UI /me refresh agrees. |
| AUTH-014 | LOCAL PASS | AuthRecoveryIntegrationTest.resetLifecycleAndBoundaries / resetRejectsExpiredInactiveReplacedAndUnknownTokensWithoutMutation / resetMustNotMakeDistinctUnicodePasswordsEquivalent; browser reset matrix | Reset persists Argon2id; exact new Unicode password succeeds, colliding-prefix alternate and old password fail. Expiry, replay, spaces and refresh revocation pass. |
| AUTH-015 | LOCAL PASS | resetLifecycleAndBoundaries; jwtValidationAndDatabaseAuthority; browser retained-JWT check | Reset deletes refresh rows; previously issued unexpired access JWT200, expired/inactive401. No immediate access revocation claim. This is characterization of the unchanged JWT architecture. |

## Resolved defects and implementation

- **AUTH-D01, P0 — FIXED:** BCrypt accepted distinct Unicode passwords with the same first 72 UTF-8 bytes. Reproduced before editing through local registration/login and preserved the original reset regression. Registration, login and reset now use Spring Argon2id. PostgreSQL tests prove both exact Unicode values remain distinct; reset rejects the old and alternate password. AUTH-004/AUTH-014 are now LOCAL PASS.
- **AUTH-D02, P1 — FIXED:** Unfiltered admin booking search produced PostgreSQL “could not determine data type of parameter $1” and HTTP500. Reproduced locally before editing. Conditional Criteria predicates eliminate ambiguous null bindings. All seven filters, combinations, pagination, descending order and role boundaries pass a new real PostgreSQL HTTP test; live no-filter request returns200.
- **AUTH-D03 — previously FIXED, still passing:** Policy acceptance repository explicitly projects UUIDs. No further policy changes.
- **AUTH-D04, cross-tab refresh — FIXED:** The old losing request unconditionally cleared shared storage after the winner saved a rotated session. Generation comparison now preserves/synchronizes the newer session and retries once. Genuine current-generation failure clears; stale success cannot overwrite another login or resurrect logout. No server rotation or JWT architecture change.

[Password contract and design decisions](AUTH_LAUNCH_FIXES.md) documents Argon2id, costs, benchmark, policy, dependency and development data strategy. Defaults: 64 MiB, three iterations, parallelism one; 16-byte salt/32-byte hash. Five local samples averaged approximately 930 ms encode/915 ms match. Password policy remains 8–72 Java string characters, unchanged exact secret handling. `password_hash` is TEXT. V103 is development-only, updating only the two seed identities; both seed logins and migration were executed successfully. No volume reset, common schema change, legacy BCrypt fallback or FK change.

## Verification and sanitized evidence

| Check | Executed result |
|---|---|
| `mvn test` | PASS —226 tests, zero failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS —226 unit +41 real PostgreSQL tests, zero failures/errors/skips |
| `mvn clean package` | PASS |
| Frontend `npm run typecheck` | PASS |
| Frontend `npm run lint` | PASS, no warnings/errors |
| Frontend `npm run build` | PASS |
| Deterministic client suite | PASS —13 checks, repeated 20 runs; deferred barriers, no timing sleeps |
| Actual Chrome browser suite | PASS —15 check groups, including SCOUT/OWNER registration/login/reload/logout, verification/recovery forms and two-tab refresh |
| Live defect probes | PASS —alternate Unicode password401; unfiltered admin search200 |
| Development seeds | PASS —V103 applied; both seed logins200; two Argon2id rows |
| Backend/frontend `git diff --check` | PASS |

New coverage in this fix: one PostgreSQL admin matrix, stronger registration/reset exact Unicode login assertions, four additional client checks (13 total), and corrected browser winner/loser assertions. The earlier15 new AUTH PostgreSQL methods and shared harness were rerun, not replaced by mocks. All 41 PostgreSQL tests include existing constraint/lock concurrency coverage.

Evidence: [JUnit names/counts](evidence/auth/automated.json), [scenario mapping](evidence/auth/scenarios.json), [client repeated execution](evidence/auth/client.json), [browser/screenshot evidence](evidence/auth/browser.json), [live probes](evidence/auth/defects.json), [benchmark](evidence/auth/argon2-benchmark.json), [seed migration/logins](evidence/auth/seeds.json). Evidence contains statuses, counts and fictional identifiers; no raw passwords/tokens. Runtime scans found none of the generated secrets in configured local backend/frontend logs. Raw provider delivery is not claimed.

## Remaining observations

1. **Stale owner-page auth UX — RESOLVED (frontend-only follow-up):** Original403s bypassed401 recovery and were mapped to ADMIN copy. Required reads now capture a non-secret generation, join existing refresh, or coalesce a current-user check if403 arrives first. Only a successful generation change permits one resource retry. Real same-generation ownership403 remains forbidden. The complete browser harness passed22 groups, including three repetitions of single-/two-tab owner recovery without reload, both role guards and real other-owner venue denial. See [owner recovery evidence](evidence/auth/owner-recovery/browser.json) and [focused report](OWNER_AUTH_RECOVERY.md).
2. Logout clears browser credentials only; captured refresh credentials remain usable until expiry/revocation/reset. Separate logins remain independent sessions.
3. Password reset removes all refresh rows; existing access JWTs remain valid until expiry while the DB account stays active. No immediate JWT revocation claim.
4. A second401 after successful refresh retains the newer cached session; retry remains bounded.
5. Generic recovery responses do not prove timing equalization. Correct-password inactive login deliberately returns403.
6. Real deployment Argon2 latency, concurrency memory budget and edge abuse controls need staging validation. Other disposable BCrypt QA accounts require fresh registration or normal password reset; only immutable dev seeds were migrated.
7. Recovery URL logs must be checked at staging proxies/edge. The local browser suite uses `next start` and an isolated disposable Chrome profile, not token-logging dev navigation.

## Reproduce locally

Use [AUTH-001 startup and fixture guide](AUTH-001_REGISTER_SCOUT.md). Keep email disabled and use unique RUN_ID fixtures. Do not delete seeded owners. Start the built frontend with `npm run start > /tmp/eema-complete-auth-frontend-start.log 2>&1`. Launch isolated Chrome:

```sh
google-chrome --headless --no-sandbox --disable-gpu --disable-dev-shm-usage --disable-background-networking --remote-debugging-port=9223 --user-data-dir=/tmp/eema-complete-auth-browser about:blank
node scripts/qa/auth-client-check.cjs
node scripts/qa/auth-defect-probes.cjs --local-qa
node scripts/qa/auth-browser.cjs --local-qa
mvn verify -Ppostgres-integration
```

The browser runner closes Chrome on success. Discard its temporary profile afterward. Testcontainers owns its disposable databases. Local RUN_ID rows can remain until an intentional guarded full-suite reset.

## WHAT ELI SHOULD TEST PERSONALLY

- Registration/login form clarity and navigation after authentication.
- Verification notice and next-step wording.
- Password-reset form usability and confirmation copy.
- Mobile form layout, focus and keyboard usability.
- Error wording, including owner forbidden-state wording.

No manual hash/SQL/JWT/concurrency repetition is needed.

## STAGING AUTH TESTS STILL REQUIRED

1. Real Resend verification email: sender, recipient, CTA and successful verification.
2. Real password-reset delivery and link behavior.
3. Real HTTPS/domain URLs, redirects and headers.
4. Deliverability/spam and provider-failure operational handling.
5. Edge rate limiting, abuse controls, CORS/log redaction and Argon2 latency/memory under expected concurrency.

All five groups remain NOT RUN / STAGING ONLY. Human review is not automatically passed; the stale-list defect is now deterministically resolved. **The three requested defects and local AUTH gate are clear for private-alpha staging validation; production/alpha release acceptance still depends on those outstanding checks.**


## Owner auth-recovery follow-up — completed 2026-09-10

The remaining owner-page stale-auth defect is resolved. Frontend typecheck, lint, build and diff check passed;11 focused client checks and13 existing auth checks each passed20 runs. The existing auth harness was rerun with22 browser groups, including three owner-recovery repetitions. Backend application sources, migrations and pom were hash-compared unchanged; backend tests were not rerun during this frontend-only pass. Prior backend results above remain dated evidence, not a claim of new execution.

Use the frontend QA entry points: `node scripts/qa/owner-auth-recovery.cjs`, `node scripts/qa/auth-client-regression.cjs`, and `node scripts/qa/owner-auth-browser.cjs --local-qa`. The regression adapter supplies Web Crypto to the existing backend-owned VM harness without editing that read-only script. Details, route review and evidence: [OWNER_AUTH_RECOVERY.md](OWNER_AUTH_RECOVERY.md). All15 defined AUTH scenarios remain LOCAL PASS. No known local AUTH launch defect remains; staging/provider checks and human judgment remain separate.
