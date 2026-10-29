<a id="family-sec"></a>
### SEC scenarios

<a id="sec-001"></a>
#### SEC-001 — Object ownership tampering

**Area:** SEC · **Priority:** P0 · **Test level:** SECURITY, API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** For bookings, venue editor/availability/moderation, inquiries, payouts, payout destinations, receipts/statements and incidents replay legitimate request after replacing UUID with another persona’s existing UUID, then nonexistent UUID.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Forbidden ownership requests never expose data or mutate; use endpoint matrix exact 403/404 distinction. ADMIN privilege applies only where service permits (e.g. not owner-only payout history).


**Expected database/domain state:** Forbidden ownership requests never expose data or mutate; use endpoint matrix exact 403/404 distinction. ADMIN privilege applies only where service permits (e.g. not owner-only payout history).


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** SecurityConfig, JwtAuthenticationFilter, GlobalExceptionHandler, frontend API/auth modules


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="sec-002"></a>
#### SEC-002 — Input injection matrix

**Area:** SEC · **Priority:** P0 · **Test level:** SECURITY, API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Independently use <script>alert(1)</script>, HTML, SQL-like quotes, 10k text, Unicode, emoji, CR/LF, quotes and backslashes in name, brief, contact, reason, policy, incident, query filter and filename.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Plain text escaped in React/email; no execution or SQL injection; DTO bounds respected. Incident <> is rejected. Logs must not permit attacker-forged structured entries; record actual sanitation gaps.


**Expected database/domain state:** Plain text escaped in React/email; no execution or SQL injection; DTO bounds respected. Incident <> is rejected. Logs must not permit attacker-forged structured entries; record actual sanitation gaps.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** SecurityConfig, JwtAuthenticationFilter, GlobalExceptionHandler, frontend API/auth modules


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="sec-003"></a>
#### SEC-003 — Token and credential leak audit

**Area:** SEC · **Priority:** P0 · **Test level:** SECURITY, API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** After local full flow, scan captured network/logs/storage for Bearer, JWT-like three-part tokens, verification/reset raw token, password, Mollie/Resend/Gemini/S3 key and full fixture IBAN. Review only counts/redacted snippets.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** No secrets in logs or normal DTOs. Auth token response/storage is intentional current design; verification/reset token only in link/request, never persisted client-side. Refresh token isolated to auth module but accessible to same-origin JS via localStorage.


**Expected database/domain state:** No secrets in logs or normal DTOs. Auth token response/storage is intentional current design; verification/reset token only in link/request, never persisted client-side. Refresh token isolated to auth module but accessible to same-origin JS via localStorage.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** SecurityConfig, JwtAuthenticationFilter, GlobalExceptionHandler, frontend API/auth modules


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="sec-004"></a>
#### SEC-004 — CORS, security headers and request ID

**Area:** SEC · **Priority:** P0 · **Test level:** SECURITY, API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Send OPTIONS from allowed localhost origin then unlisted origin, wildcard/null origin; GET API with safe/unsafe/oversized X-Request-ID. Check nosniff, DENY, no-referrer, CSP and HSTS with local HTTP and staging HTTPS.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Configured origins only; allowed Authorization/Content-Type/X-Requested-With/X-Request-ID; expose request ID and Content-Disposition. API CSP default-src none/frame-ancestors none; HSTS only enabled profile/HTTPS. Safe correlation ID propagated, unsafe replaced.


**Expected database/domain state:** Configured origins only; allowed Authorization/Content-Type/X-Requested-With/X-Request-ID; expose request ID and Content-Disposition. API CSP default-src none/frame-ancestors none; HSTS only enabled profile/HTTPS. Safe correlation ID propagated, unsafe replaced.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** SecurityConfig, JwtAuthenticationFilter, GlobalExceptionHandler, frontend API/auth modules


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="sec-005"></a>
#### SEC-005 — Private document URL and cache isolation

**Area:** SEC · **Priority:** P0 · **Test level:** SECURITY, API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Download authorized PDF, inspect links/header/response; attempt guessed private path via /upload/files and anonymous/unrelated PDF endpoint; use second account after browser back navigation.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** No public private-document URL or storage reference; backend permission enforced for each download. Inspect actual cache headers: document controller does not explicitly set no-store (unlike IBAN reveal); absence is a security review gap.


**Expected database/domain state:** No public private-document URL or storage reference; backend permission enforced for each download. Inspect actual cache headers: document controller does not explicitly set no-store (unlike IBAN reveal); absence is a security review gap.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** SecurityConfig, JwtAuthenticationFilter, GlobalExceptionHandler, frontend API/auth modules


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="sec-006"></a>
#### SEC-006 — Anonymous framework and actuator exposure

**Area:** SEC · **Priority:** P0 · **Test level:** SECURITY, API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Call /actuator/health, /health/readiness, /health/liveness, /actuator/metrics and an individual metric as each role; attempt Swagger/api-docs and unsupported admin method.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Health public with no details; metrics ADMIN only; other unallowlisted routes require auth. Check full URLs include /api; do not assume Swagger publicly available.


**Expected database/domain state:** Health public with no details; metrics ADMIN only; other unallowlisted routes require auth. Check full URLs include /api; do not assume Swagger publicly available.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** SecurityConfig, JwtAuthenticationFilter, GlobalExceptionHandler, frontend API/auth modules


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="sec-007"></a>
#### SEC-007 — Frontend malformed sessions and unverified bypass

**Area:** SEC · **Priority:** P0 · **Test level:** SECURITY, API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Corrupt eema:auth JSON, remove role/token fields, use invalid role, set emailVerified=true locally, tamper booking navigation. Reload and call protected API.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Frontend should recover safely without protected data disclosure; backend checks database role/email independently. getAuthSession performs partial shape validation; malformed stored user may expose UI robustness gap.


**Expected database/domain state:** Frontend should recover safely without protected data disclosure; backend checks database role/email independently. getAuthSession performs partial shape validation; malformed stored user may expose UI robustness gap.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** SecurityConfig, JwtAuthenticationFilter, GlobalExceptionHandler, frontend API/auth modules


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


