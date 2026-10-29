# AUTH-001 — Register SCOUT: local execution guide

The authoritative scenario is [AUTH-001 in the master plan](../EEMA_COMPLETE_TEST_MASTER_PLAN.md#auth-001). This guide supplies commands, evidence and the focused audit. The focused hardening changes backend auth request binding/normalization and the role allowlist. No schema, auth architecture, business flow or frontend source/framework is changed.

## Prerequisites and startup

Use Bash, Python 3, Java 21/Maven, Docker with Compose, and the existing frontend Node/npm toolchain. Docker must be local. Testcontainers uses its own disposable PostgreSQL 16 container; do not point its tests at the Compose database. Existing seed users/venues are baseline fixtures, not disposable registration users.

```bash
cd ~/workspace/Eema-backend
# First setup only: do not overwrite an existing .env.
if [ ! -f .env ]; then cp .env.example .env; fi
```

In your local `.env`, ensure `EMAIL_NOTIFICATIONS_ENABLED=false`, `MOLLIE_FEE_RECONCILIATION_ENABLED=false`, local storage, and the sample local DB/JWT/encryption configuration. Do not print `.env`, effective Compose config or real keys into evidence. No Mollie, Gemini, Resend or S3 call is needed for this scenario. Do not run a match brief or click resend verification as part of the local email-disabled check.

```bash
scripts/reset-local-qa.sh --dry-run
# This performs guards only. Startup preserves existing volume data:
docker compose up -d --build postgres backend
docker compose ps
docker compose exec -T postgres pg_isready -U eema -d eema
curl --fail --silent --show-error http://localhost:8080/api/actuator/health
```

Wait for both containers to be healthy. Backend uses `/api` servlet context, `local` profile, common migrations V1–V11 plus dev V100–V102, and Hibernate validation. Start frontend in another terminal:

```bash
cd ~/workspace/Eema-frontend
# First setup only; preserve any existing .env.local:
if [ ! -f .env.local ]; then cp .env.example .env.local; fi
npm install
npm run dev
```

Ensure `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api`; open `http://localhost:3000`. If dependencies already exist, skip `npm install`. Run `npm run build` with the dev server stopped, or restart the dev server afterward: both use `.next`, and mixed generated assets can prevent hydration. No application source change is required to recover.

## Optional full-suite reset — destructive

This is optional; AUTH-001 does not need row deletion. The following second command deletes **all local project PostgreSQL data, public uploads and private documents**, then builds/starts the stack, applies local seeds and waits up to 240 seconds for health:

```bash
cd ~/workspace/Eema-backend
scripts/reset-local-qa.sh --dry-run
# Run ONLY when deliberately discarding the entire local QA dataset:
scripts/reset-local-qa.sh --confirm-destroy-local-qa
```

Guards require a local Unix Docker endpoint, explicit `eema-backend` project/Compose file, local datasource/profile, disabled email/reporting and the expected project-owned nonexternal volumes. Custom configurations are refused; do not weaken guards to operate on staging. A timeout reports failure, not a successful reset. Never delete an owner to clear registration data, disable FKs, or use broad SQL CASCADE manually. `venues.owner_id ON DELETE RESTRICT` protects venue ownership/history; token-child CASCADE FKs are appropriate for token lifetime. No FK inconsistency was found in this focused audit.

## API run with safe response handling

Run these commands in one Bash shell. Every invocation generates a new date-plus-random RUN_ID, so parallel/quick reruns do not collide. The password is fictional. The temporary response contains live local access/refresh secrets: permissions are restricted and cleanup removes it. Do not enable shell tracing, use `curl -v`, export a HAR or attach the raw response.

```bash
cd ~/workspace/Eema-backend
export RUN_ID=$(scripts/qa-run-id.sh)
export QA_EMAIL="scout-a+$RUN_ID@example.test"
umask 077
export QA_TMP=$(mktemp -d /tmp/eema-auth-001.XXXXXX)
trap 'rm -f -- "$QA_TMP/request.json" "$QA_TMP/response.json"; rmdir -- "$QA_TMP"' EXIT
python3 - <<'PY'
import json, os
from pathlib import Path
payload = dict(email='  ' + os.environ['QA_EMAIL'].upper() + '  ', fullName='  QA Scout A  ',
               password='QaFixture-2026!', role='SCOUT')
Path(os.environ['QA_TMP'], 'request.json').write_text(json.dumps(payload))
PY
HTTP_STATUS=$(curl --silent --show-error \
  --output "$QA_TMP/response.json" --write-out '%{http_code}' \
  --header 'Content-Type: application/json' \
  --data-binary "@$QA_TMP/request.json" \
  http://localhost:8080/api/auth/register)
printf 'HTTP status: %s\n' "$HTTP_STATUS"
test "$HTTP_STATUS" = 201
python3 - <<'PY'
import json, os, urllib.request
from pathlib import Path
r = json.loads(Path(os.environ['QA_TMP'], 'response.json').read_text())
assert set(r) == {'accessToken', 'refreshToken', 'tokenType', 'expiresIn', 'user'}
assert r['tokenType'] == 'Bearer' and r['expiresIn'] == 900
assert r['accessToken'] and r['refreshToken']
u = r['user']
assert set(u) == {'id', 'email', 'role', 'fullName', 'phone', 'active', 'emailVerified'}
assert u['email'] == os.environ['QA_EMAIL'] and u['role'] == 'SCOUT'
assert u['fullName'] == 'QA Scout A' and u['phone'] is None
assert u['active'] is True and u['emailVerified'] is False
req = urllib.request.Request('http://localhost:8080/api/auth/me',
    headers={'Authorization': 'Bearer ' + r['accessToken']})
with urllib.request.urlopen(req) as response:
    assert response.status == 200 and json.load(response) == u
print(json.dumps({'user': u, 'responseFields': sorted(r), 'meAgrees': True}, indent=2))
PY
```

The fixed 900-second assertion is the current default (15 minutes); change the expectation only when intentionally testing a different configured access expiry. Never include `accessToken` or `refreshToken` values in evidence. API calls above do not install a browser session.

## Safe PostgreSQL inspection

Continue in the same shell so `QA_EMAIL` is set. `psql` quotes the supplied variable as a SQL literal. Check count **and** boolean fields; a NULL aggregate from zero rows is not PASS.

```bash
docker compose exec -T postgres psql -X -U eema -d eema \
  -v ON_ERROR_STOP=1 -v qa_email="$QA_EMAIL" <<'SQL'
SELECT id, email, full_name, role, active, email_verified,
       password_hash LIKE '$argon2id$v=19$m=65536,t=3,p=1$%' AS argon2id_default_cost
FROM users WHERE email = :'qa_email';

SELECT count(*) AS total,
       count(*) FILTER (WHERE NOT revoked AND expires_at > now()) AS active,
       bool_and(token_hash ~ '^[0-9a-f]{64}$') AS sha256_shape,
       bool_and(expires_at > created_at) AS positive_lifetime
FROM refresh_tokens
WHERE user_id = (SELECT id FROM users WHERE email = :'qa_email');

SELECT count(*) AS total,
       count(*) FILTER (WHERE used_at IS NULL AND expires_at > now()) AS active,
       bool_and(token_hash ~ '^[0-9a-f]{64}$') AS sha256_shape,
       bool_and(expires_at > created_at) AS positive_lifetime
FROM email_verification_tokens
WHERE user_id = (SELECT id FROM users WHERE email = :'qa_email');
SQL
```

Expected: one SCOUT row, active true, email_verified false; one total/active token of each type, all booleans true. These queries establish shape/count/state, **not** cryptographic correspondence. The integration test calls `PasswordEncoder.matches(raw, hash)` and hashes the returned refresh token/transient verification-event token using the actual codecs. It checks expiry against configured durations. Never compare a salted password hash to a fixed string; never select password_hash/token_hash values into manual evidence. Raw refresh token, raw verification token, JWT and plaintext password must not be DB fields/values. The verification token cannot be recovered from its hash.

## Browser run

Use a **new** `RUN_ID=$(scripts/qa-run-id.sh)` and `scout-a+$RUN_ID@example.test`; the API email above now exists. Use an isolated browser profile and no pending matching brief.

1. Open `/`; wait for **Sign in / register**. Click it, then the **Create account** tab.
2. Enter `QA Scout A`, choose **Scout**, enter the new email and `QaFixture-2026!`. Submit once. The button briefly reads **Please wait…** and is disabled.
3. In DevTools Network, check one `POST http://localhost:8080/api/auth/register`, status 201. Inspect only response schema and safe user fields; do not save request/response secrets or a HAR.
4. The panel closes at `/`. Header contains identity and Scout, **My bookings**, **Inquiries**, **Email not verified**, **Sign out**. CSS may render labels in uppercase. Marketplace loads without verification/policy prerequisites.
5. Reload before token expiry. Identity, Scout links and unverified notice remain. AuthProvider restores localStorage `eema:auth`; do not print its contents. Reload does not automatically validate `/auth/me`.
6. For an explicit authoritative browser check, run this expression in DevTools. It returns only safe status/booleans:

   ```javascript
   (async () => {
     const session = JSON.parse(localStorage.getItem('eema:auth'));
     const response = await fetch('http://localhost:8080/api/auth/me', {
       headers: { Authorization: 'Bearer ' + session.accessToken }
     });
     const user = await response.json();
     return { status: response.status,
       agrees: JSON.stringify(user) === JSON.stringify(session.user),
       unverified: user.emailVerified === false };
   })()
   ```

7. Expect 200, agrees=true, unverified=true. A policy review link appears only if policy-status loading succeeds and completeness is false. Policy acceptance is not part of registration. Email verification and relevant policy gating apply to later mutations; full transaction-gating tests remain separate scenarios.
8. Capture the sanitized header/screen, route, status and RUN_ID. Sign out; `localStorage.getItem('eema:auth') === null` should return true. Close the profile. Leave RUN_ID rows until the next deliberate full local reset. No manual FK cleanup.

## Focused automation and variants

```bash
cd ~/workspace/Eema-backend
mvn test
mvn verify -Ppostgres-integration
# Focused repeat using the SAME existing integration profile:
mvn verify -Ppostgres-integration -Dit.test=RegistrationIntegrationTest
mvn clean package
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s scripts/tests -v
bash -n scripts/reset-local-qa.sh scripts/qa-run-id.sh
git diff --check

cd ~/workspace/Eema-frontend
npm run typecheck
npm run lint
npm run build
git diff --check
```

Docker socket access is required for Testcontainers; inability to start Docker is BLOCKED, not a registration failure. The eleven registration methods combine MockMvc's real security/controller/service stack with real PostgreSQL; they are not five duplicative layers. They generate UUID email suffixes and let Testcontainers discard its database without manual deletion. The existing critical-path class's `TRUNCATE ... CASCADE` remains confined to its disposable database, unrelated to manual QA.

| Assertion | Automation/evidence |
|---|---|
| 201, padded/mixed-case email and trimmed name, exact DTO/no leaked fields, normalized SCOUT and `/me` agreement | `RegistrationIntegrationTest.anonymousScoutPersistsOnlyHashesAndReturnsExactPublicContract` |
| Argon2id match, refresh hash correspondence, verification-event hash correspondence, token count/expiry | Same method; actual persistence, not mock repositories |
| Existing authenticated SCOUT can register a distinct new user | `authenticatedScoutDoesNotOwnOrOverrideNewRegistration` |
| Case-insensitive duplicate 409/no extra tokens/event/email | `duplicateCaseVariantIs409WithoutAdditionalTokensOrEvents` |
| ADMIN/unsupported role, malformed JSON, missing role, bad email/password/name, internal email whitespace | `rejectedRolesValidationAndMalformedJsonHaveNoSideEffects` |
| Direct-call/HTTP normalization agreement, OWNER explicitly allowed, name internal spaces preserved | `directRegistrationAlsoCanonicalizesAndAllowsOwner` |
| Register unknown fields rejected before side effects | `unknownRegistrationFieldsAreRejectedBeforeAnySideEffect` |
| Other six auth POST endpoints reject unknown fields safely | `otherAuthEndpointsRejectUnknownFieldsWithSafeErrors` |
| Login accepts canonical email variants and exact password spaces only | `normalizedLoginPreservesPasswordSpacesExactly` |
| Forgot/resend target canonical account and retain generic responses | `recoveryUsesCanonicalEmailAndKeepsGenericResponses` |
| Strictness limited to six DTOs; passwords/tokens unchanged | `AuthInputContractTest` (2 methods) |
| Existing PostgreSQL LOWER(email) unique index, concurrent case variants | `PostgresCriticalPathIntegrationTest.emailIndexRejectsConcurrentCaseVariedLogicalDuplicates` |
| Atomic rollback on verification persistence failure | `verificationPersistenceFailureRollsBackUserAndTokens` |
| Delivery adapter failure after commit cannot roll back account | `deliveryFailureAfterCommitDoesNotRollbackRegistration` (simulated fault; no real provider) |
| Disabled email never calls provider | `EmailServiceTest.disabledDeliveryNeverCallsProvider` |
| Existing service token issuance and email content | `AuthServiceTest.registerAndLoginIssueTokensWithoutPersistingRawRefreshToken`, `AccountEmailNotificationServiceTest` |
| Visible UI, cached reload, sign-out | MANUAL FRONTEND; one-off local headless Chrome execution recorded below, no committed browser framework |
| Actual email receipt/CTA/sender/expiry copy | STAGING EMAIL ONLY, variant AUTH-001B |

Negative variants are described precisely in the master scenario. Use an API client for invalid values that HTML forms prevent. Invalid role/validation/malformed requests return 400; duplicate email 409. None creates a new account/token/event. The six auth DTOs implement StrictAuthRequest; a shared @JsonAnySetter rejects any unknown field during binding. Global Jackson tolerance is unchanged. Existing safe HTTP400 body says `Malformed JSON request`, without echoing names/values or Jackson internals. Registration explicitly allows only SCOUT and OWNER, rejecting ADMIN, null and future unsupported roles. OWNER success belongs to AUTH-002, not a denial variation of SCOUT registration.

## Email variants

**AUTH-001A, local disabled:** one verification event is published inside the registration transaction, handled asynchronously after commit. `EmailService.send` may be called but skips Resend when disabled. The token still exists and registration returns 201. Local logs may say **Transactional email skipped because notifications are disabled**. One event/invocation is not a durable delivery guarantee; there is no outbox/retry proof here.

**AUTH-001B, staging enabled:** use an authorized controlled mailbox (replace the synthetic address; never put real personal details in the plan). Ensure `EMAIL_NOTIFICATIONS_ENABLED=true`, configured sender/key and staging frontend base URL. Submit once; check one send attempt and actual received message, subject **Verify your Eema email**, correct recipient, **Verify email** CTA to `/verify-email?token=<transient-token>`, current 24-hour expiry wording. Do not attach token-bearing links. Check `emailVerified=false` before following the link; successful verification belongs to the verification scenario. Record delivery PASS/FAIL/BLOCKED separately. No real delivery was attempted during this local task.

## Corrections and focused findings

| Original wording | Correction and reason |
|---|---|
| Scout A/Owner A; repeat denial with Scout B/Owner B/anonymous | Public registration has no resource ownership boundary. Anonymous is the happy-path actor. A valid existing token does not prohibit registration. |
| Actor active, email verified, policies accepted | New account does not yet exist. Only stack health, reachable endpoint and unique email are required. |
| Standard fixtures/fresh aggregate | Automatically generated RUN_ID email; no seed deletion or assumed snapshot harness. |
| Registration, SCOUT | Exact UI is Sign in / register → Create account tab → Scout → Create account submit. |
| Generic authoritative state/no optimistic money claim | Precise identity/navigation/unverified label, idle homepage, cached reload and explicit `/me` comparison. Money claims are irrelevant. |
| API section mixes response, persistence and header | Exact AuthResponse and UserDto specified separately from DB and UI. |
| DB section repeats 201/accessToken/refreshToken/header | Only persisted user and hashed token state belongs in DB assertions. |
| No email unless another matrix says otherwise | Registration publishes verification event; after-commit adapter attempts notification, disabled mode skips provider. |
| Swap another owner/scout UUID and expect denial | No resource UUID authorization parameter. Assert ADMIN prevention, strict unknown-field rejection, no leaked secrets instead. |
| Restore isolated fixture snapshot | No such manual harness. Leave unique fixtures; optional guarded whole local volume reset. |
| SHOULD AUTOMATE | Specific existing/new integration, provider-adapter unit, manual frontend and staging-delivery mappings. |
| PASS all mechanically listed variants | Dated A result and separate B result; irrelevant denial variants removed. |

Additional observed limitations, not silently fixed:

- The former HTTP/service contradiction is fixed: DTO compact constructors call one EmailNormalizer before validation. It uses String.trim() and Locale.ROOT lowercase, preserving internal whitespace and plus/punctuation; internal whitespace still fails @Email. FullName trims outer whitespace only. Passwords and opaque security tokens are untouched.
- Reload trusts cached localStorage until later API activity. An explicit `/me` comparison is necessary to prove server agreement; this does not claim expired-token handling is covered.
- Password DTO retains Java character length 8–72, not complexity rules. Argon2id processes the complete UTF-8 secret without trimming or Unicode normalization. AUTH-004/AUTH-014 cover distinct accepted Unicode passwords sharing the first 72 bytes. Historical BCrypt failures are preserved in the execution report.
- Email delivery failures after commit cannot roll back registration and are logged; actual delivery/retry guarantees require separate operational coverage.
- Other generated AUTH scenarios still contain generic language; this task only certifies AUTH-001. Do not extrapolate its corrected status to the whole master plan.
- No production referential-integrity defect was established; protective ownership/history FKs remain intact.

## Initial execution record (before input hardening)

Date: **2026-09-10**. Source baseline: backend `b1c33b09ecb7b405a9113021586e03b996c45f1c`, frontend `8eb69a824772caf794945bdb3ac036a6654fb90a`, plus the test/documentation/scripts in this change. This initial run predates the backend input-hardening changes. All data below is fictional; the new contract is verified separately below.

| Check | Result | Evidence |
|---|---|---|
| `mvn test` | PASS | 223 tests, zero failures/errors/skips |
| `mvn verify -Ppostgres-integration` | PASS | 20 tests: existing 13 plus 7 registration; zero failures/errors/skips. Initial sandbox Docker denial recovered by rerunning with socket access. |
| `mvn clean package` | PASS | Clean compilation/package; unit suite passed |
| Frontend typecheck/lint/build | PASS | All three commands succeeded; lint had no warnings/errors |
| Reset guard tests | PASS | Nine fake-Docker tests; valid sequence/health, no-confirmation, remote context/database, production profile, configuration import, external volume, enabled email and dry-run |
| Actual reset dry run | PASS | Local guards passed; no services stopped/volumes removed |
| Actual destructive reset | NOT RUN | Deliberately unnecessary for unique registration fixtures; command sequencing tested with fake Docker |
| Live API + DB | PASS | Email `scout-a+20260910-161346-b06ba24c@example.test`; HTTP201, exact response fields, `/me`200 agreement, one SCOUT active/unverified row, BCrypt shape, one active matching refresh hash, one active verification hash |
| Browser register/reload/sign-out | PASS | Email `scout-a+ui-1789057365876@example.test`; one POST201, identity/Scout links/unverified notice at `/`, same user after reload, explicit `/me`200 agreement, local session removed on sign-out. One-off headless Chrome; no browser framework added. |
| AUTH-001A overall | PASS | Local API, real-Postgres integration, browser registration/reload and session cleanup proved |
| AUTH-001B actual email delivery | NOT RUN | Local notifications disabled; no Resend call or real personal mailbox |
| Both repositories `git diff --check` | PASS | No whitespace errors; frontend source remains unchanged |

Evidence files in the working session are sanitized `/tmp/eema-auth-live-result.json`, `/tmp/eema-auth-browser-result.json`, and `/tmp/eema-auth-ui.png`; Maven/Next output is in `/tmp/eema-auth-{mvn-test,postgres,package,frontend-build}.log`. Temporary paths are ephemeral, so this table records the durable result; do not rely on those files surviving another machine/session. No raw security tokens belong in the final report. Browser session is cleared at the end; RUN_ID rows remain until a deliberate full local reset.

Durable sanitized evidence: [execution JSON](evidence/AUTH-001_LOCAL_EXECUTION.json) and [registered SCOUT screenshot](evidence/AUTH-001_LOCAL_SCOUT.png). Initial browser harness checks were corrected for CSS uppercase labels and CORS OPTIONS preflight counting; only POST responses count as registration submissions. The successful final run used a fresh email. Earlier exploratory RUN_ID rows were left in place, with the isolated session cleared.

## Input-hardening audit and compatibility

Previous contract: padded emails failed HTTP validation; service methods independently trimmed/lowercased; fullName retained surrounding spaces; unknown JSON fields were ignored; ADMIN alone was excluded. Final boundary: RegisterRequest, LoginRequest and shared EmailAddressRequest canonicalize in compact constructors before Jakarta validation. Recovery string entry points and current-user email lookup use the same null-safe EmailNormalizer. No @Email validation is bypassed and no token/password normalization is added.

StrictAuthRequest is implemented by RegisterRequest, LoginRequest, RefreshRequest, VerifyEmailRequest, EmailAddressRequest (both resend/forgot), and ResetPasswordRequest. Its annotated unknown-property method throws a fixed exception with no field/value in the message. GlobalExceptionHandler returns the existing safe HTTP400 structure. Jackson's application-wide unknown-field setting is unchanged; a configuration test proves a non-auth record still accepts extras.

Public roles are explicitly SCOUT/OWNER. Frontend source audit covered `types/auth.ts`, `lib/api/auth.ts`, `lib/api/client.ts`, AuthPanel, VerificationNotice and recovery pages: requests contain only supported keys. No frontend source adjustment is needed. Registration/login send raw email; forgot-password already trims, which is harmless but not authoritative. Passwords are never trimmed. Existing clients sending extra auth properties now receive 400 intentionally. Trimmed registration fullName is a small intentional observable change. Non-ASCII password byte limits and Unicode whitespace beyond String.trim() are not new normalization rules.

Database decision: V1 already creates unique `users_email_idx ON users (LOWER(email))`. A PostgreSQL race proves case-varied logical duplicates cannot both persist. Existing local users had **0 noncanonical rows and 0 canonical collisions** at audit; development seeds are lowercase/trimmed. No migration, data rewrite or repository refactor was needed. The index does not itself enforce trimming or lowercase storage; canonical persistence is guaranteed by these application input paths, not by arbitrary SQL. Direct writes with surrounding whitespace remain outside that invariant; no stronger storage CHECK is claimed.

### Corrected contract execution record

Results below supersede the initial AUTH-001A evidence for padded-email and unknown-field behavior. Staging delivery remains NOT RUN. Use the same safe startup/reset/SQL procedure above; all fixtures remain unique.

| Check | Result | Evidence |
|---|---|---|
| Backend unit suite | PASS | 225 tests, zero failures/errors/skips |
| PostgreSQL integration suite | PASS | 25 tests: 14 critical-path including email uniqueness race, 11 registration/auth; zero failures/errors/skips |
| Clean package | PASS | 225 unit tests and clean package succeeded; local Docker backend rebuilt with this contract |
| Frontend typecheck/lint/build | PASS | All commands succeeded; no source changes required |
| Corrected live AUTH-001 | PASS | `scout-a+20260910-164232-6ca8b350@example.test`: padded/mixed-case email and padded name →201/canonical values; normalized duplicate →409; extra emailVerified →400/no user; one active hashed token of each type; `/me` agrees |
| Browser register/reload/sign-out | PASS | `scout-a+ui-1789058601455@example.test`: one POST201, canonical identity, SCOUT links/unverified notice, reload preserved state, `/me`200 agreed, sign-out cleared session |
| Both diff checks | PASS | No whitespace errors in either repository |

The API command above now deliberately pads/mixes case in email and pads fullName. For an unknown-field manual check, add `emailVerified: true` to a new unique valid registration payload: expect HTTP400 `Malformed JSON request`, no new user or tokens, and no email event. Run the automated negative matrix for all seven forbidden keys; never put actual passwords/hashes/security tokens in error evidence.

Corrected-contract evidence: [sanitized JSON](evidence/AUTH_INPUT_HARDENING.json) and [browser screenshot](evidence/AUTH-001_HARDENED_SCOUT.png). **AUTH-001A PASS** against the hardened local backend; **AUTH-001B NOT RUN**. Unit/integration tests also prove normalized login, exact password spaces, canonical forgot/resend lookup, generic recovery responses, opaque-token preservation and all strict auth DTOs. No real provider call was made.

## Complete AUTH execution follow-up

See [AUTH_EXECUTION_REPORT.md](AUTH_EXECUTION_REPORT.md) for the subsequent complete 15-scenario run. The initial run exposed AUTH-D01 in registration and reset; the subsequent pre-launch Argon2id fix resolves both Unicode regressions. All 15 AUTH scenarios now pass locally, including complete-password matching. Earlier execution tables remain historical. The policy acceptance UUID projection was narrowly repaired, with PostgreSQL evidence. Current suite counts and open defects are recorded there.

Current password algorithm, costs, seed migration and disposable-account guidance: [AUTH_LAUNCH_FIXES.md](AUTH_LAUNCH_FIXES.md). Current results: 226 unit tests, 41 PostgreSQL tests, 13 client checks repeated 20 times, and 15 browser check groups pass. Real email delivery remains staging-only.
