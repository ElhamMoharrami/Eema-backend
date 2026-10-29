### INC scenarios

<a id="inc-001"></a>
#### INC-001 — Incident participants and plain text

**Area:** INC · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** For every IncidentCategory report own booking as Scout A, owning Owner A and ADMIN; try unrelated users; description empty, markup, 4000/4001, newline, Unicode.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Valid reports OPEN (201); unrelated denied; blank/markup/>4000 400; plain text retained. Admin permitted by service; UI location does not define additional booking-status eligibility.


**Expected database/domain state:** Valid reports OPEN (201); unrelated denied; blank/markup/>4000 400; plain text retained. Admin permitted by service; UI location does not define additional booking-status eligibility.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** IncidentService, IncidentReport, CreateIncidentRequest, ResolveIncidentRequest


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="inc-002"></a>
#### INC-002 — Admin incident lifecycle and isolation

**Area:** INC · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** ADMIN start-review OPEN, resolve with note, close; try every skipped/repeated/reverse transition. Compare participant list and financial/venue rows before/after.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** OPEN→UNDER_REVIEW→RESOLVED→CLOSED only; invalid 409; required plain-text resolution ≤2000. Participant DTO hides admin note; no automatic booking/refund/payout/liability/venue changes, no incident email implemented.


**Expected database/domain state:** OPEN→UNDER_REVIEW→RESOLVED→CLOSED only; invalid 409; required plain-text resolution ≤2000. Participant DTO hides admin note; no automatic booking/refund/payout/liability/venue changes, no incident email implemented.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** IncidentService, IncidentReport, CreateIncidentRequest, ResolveIncidentRequest


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


