#### UXACT-001 — Double submission: booking request

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open / with valid fixture. Delay mutation response 3 seconds; rapid double-click booking request, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [/](..//)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-002"></a>
#### UXACT-002 — Double submission: inquiry

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open / with valid fixture. Delay mutation response 3 seconds; rapid double-click inquiry, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [/](..//)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-003"></a>
#### UXACT-003 — Double submission: owner accept

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /owner/bookings with valid fixture. Delay mutation response 3 seconds; rapid double-click owner accept, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /owner/bookings


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-004"></a>
#### UXACT-004 — Double submission: owner decline

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /owner/bookings with valid fixture. Delay mutation response 3 seconds; rapid double-click owner decline, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /owner/bookings


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-005"></a>
#### UXACT-005 — Double submission: checkout

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /bookings/[id] with valid fixture. Delay mutation response 3 seconds; rapid double-click checkout, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /bookings/[id]


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-006"></a>
#### UXACT-006 — Double submission: cancel

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /bookings/[id] with valid fixture. Delay mutation response 3 seconds; rapid double-click cancel, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /bookings/[id]


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-007"></a>
#### UXACT-007 — Double submission: incident

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /bookings/[id] with valid fixture. Delay mutation response 3 seconds; rapid double-click incident, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /bookings/[id]


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-008"></a>
#### UXACT-008 — Double submission: policy acceptance

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /policies with valid fixture. Delay mutation response 3 seconds; rapid double-click policy acceptance, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /policies


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-009"></a>
#### UXACT-009 — Double submission: venue submission

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /owner/venues/[id] with valid fixture. Delay mutation response 3 seconds; rapid double-click venue submission, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /owner/venues/[id]


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-010"></a>
#### UXACT-010 — Double submission: owner verification

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /owner/verification with valid fixture. Delay mutation response 3 seconds; rapid double-click owner verification, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /owner/verification


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-011"></a>
#### UXACT-011 — Double submission: payout destination

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /owner/payout-destination with valid fixture. Delay mutation response 3 seconds; rapid double-click payout destination, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /owner/payout-destination


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-012"></a>
#### UXACT-012 — Double submission: admin verify/reject

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /admin/payout-destinations/[id] with valid fixture. Delay mutation response 3 seconds; rapid double-click admin verify/reject, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /admin/payout-destinations/[id]


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-013"></a>
#### UXACT-013 — Double submission: mark paid

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /admin/payouts/[id] with valid fixture. Delay mutation response 3 seconds; rapid double-click mark paid, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /admin/payouts/[id]


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-014"></a>
#### UXACT-014 — Double submission: settle liability

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, API, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open /admin/liabilities/[id] with valid fixture. Delay mutation response 3 seconds; rapid double-click settle liability, press Enter twice, then click again after a network timeout. Inspect requests and final record/provider/event count.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected database/domain state:** Busy control blocks ordinary repeated clicks; backend transition/idempotency prevents duplicate money/state actions where implemented. Creation endpoints have no universal idempotency key: duplicate distinct REQUESTED bookings/incidents/inquiries remain possible through replay; document failures rather than assuming universal guarantee.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** /admin/liabilities/[id]


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-015"></a>
#### UXACT-015 — Session expiry while browsing marketplace

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, SECURITY, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open flow for browsing marketplace; expire access token then refresh token in isolated fixture; invoke next read/mutation/download; reauthenticate and retry after authoritative reload.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected database/domain state:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AuthProvider, apiRequest, route/components


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-016"></a>
#### UXACT-016 — Session expiry while editing owner venue

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, SECURITY, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open flow for editing owner venue; expire access token then refresh token in isolated fixture; invoke next read/mutation/download; reauthenticate and retry after authoritative reload.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected database/domain state:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AuthProvider, apiRequest, route/components


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-017"></a>
#### UXACT-017 — Session expiry while checkout

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, SECURITY, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open flow for checkout; expire access token then refresh token in isolated fixture; invoke next read/mutation/download; reauthenticate and retry after authoritative reload.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected database/domain state:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AuthProvider, apiRequest, route/components


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-018"></a>
#### UXACT-018 — Session expiry while admin review or mark paid

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, SECURITY, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open flow for admin review or mark paid; expire access token then refresh token in isolated fixture; invoke next read/mutation/download; reauthenticate and retry after authoritative reload.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected database/domain state:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AuthProvider, apiRequest, route/components


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="uxact-019"></a>
#### UXACT-019 — Session expiry while private document download

**Area:** UXACT · **Priority:** P0 · **Test level:** MANUAL UX, SECURITY, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Open flow for private document download; expire access token then refresh token in isolated fixture; invoke next read/mutation/download; reauthenticate and retry after authoritative reload.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected database/domain state:** Successful refresh continues once; failed refresh signs out and safe re-auth guidance shown. Unsaved fields and stale confirmations must be assessed; no duplicate financial operation or cross-account content after relogin.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AuthProvider, apiRequest, route/components


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="family-provider"></a>
