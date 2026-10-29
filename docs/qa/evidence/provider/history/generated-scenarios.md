### PROVIDER scenarios

<a id="provider-001"></a>
#### PROVIDER-001 — Real staging Mollie

**Area:** PROVIDER · **Priority:** P0 · **Test level:** STAGING E2E, OPERATIONAL · **Result:** NOT RUN

**Roles involved:** QA sandbox personas, Admin/operator


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** After isolated staging is provisioned with test credentials by operations, repeat variants: payment create/open/paid; duplicate callback; full and partial refund; late payment; invalid amount/ID injected only in mock. Record sandbox provider IDs, sanitized request/response, timestamps and UI/domain evidence. Do not perform these calls during documentation work.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected database/domain state:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected email/provider behavior:** Mollie: collect sandbox provider-side confirmation and safe failure evidence; no production calls.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** STAGING ONLY


**Local/staging requirement:** STAGING


**Source / existing evidence:** MOLLIE


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="provider-002"></a>
#### PROVIDER-002 — Real staging Resend

**Area:** PROVIDER · **Priority:** P0 · **Test level:** STAGING E2E, OPERATIONAL · **Result:** NOT RUN

**Roles involved:** QA sandbox personas, Admin/operator


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** After isolated staging is provisioned with test credentials by operations, repeat variants: delivery of every implemented EMAIL scenario; sender/domain, correct HTTPS deep link, inbox/spam and eventual delivery evidence. Record sandbox provider IDs, sanitized request/response, timestamps and UI/domain evidence. Do not perform these calls during documentation work.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected database/domain state:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected email/provider behavior:** Resend: collect sandbox provider-side confirmation and safe failure evidence; no production calls.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** STAGING ONLY


**Local/staging requirement:** STAGING


**Source / existing evidence:** RESEND


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="provider-003"></a>
#### PROVIDER-003 — Real staging Gemini

**Area:** PROVIDER · **Priority:** P0 · **Test level:** STAGING E2E, OPERATIONAL · **Result:** NOT RUN

**Roles involved:** QA sandbox personas, Admin/operator


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** After isolated staging is provisioned with test credentials by operations, repeat variants: representative explicit/relative/ambiguous briefs; provider timeout/unavailable/malformed output; deterministic hard filters and zero-candidate ranker skip. Record sandbox provider IDs, sanitized request/response, timestamps and UI/domain evidence. Do not perform these calls during documentation work.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected database/domain state:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected email/provider behavior:** Gemini: collect sandbox provider-side confirmation and safe failure evidence; no production calls.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** STAGING ONLY


**Local/staging requirement:** STAGING


**Source / existing evidence:** GEMINI


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="provider-004"></a>
#### PROVIDER-004 — Real staging S3

**Area:** PROVIDER · **Priority:** P0 · **Test level:** STAGING E2E, OPERATIONAL · **Result:** NOT RUN

**Roles involved:** QA sandbox personas, Admin/operator


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** After isolated staging is provisioned with test credentials by operations, repeat variants: public media retrieval, private PDF authorization, correct bucket separation, upload/issue failure and restart durability. Record sandbox provider IDs, sanitized request/response, timestamps and UI/domain evidence. Do not perform these calls during documentation work.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected database/domain state:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected email/provider behavior:** S3: collect sandbox provider-side confirmation and safe failure evidence; no production calls.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** STAGING ONLY


**Local/staging requirement:** STAGING


**Source / existing evidence:** S3


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="provider-005"></a>
#### PROVIDER-005 — Real staging Mollie reporting

**Area:** PROVIDER · **Priority:** P0 · **Test level:** STAGING E2E, OPERATIONAL · **Result:** NOT RUN

**Roles involved:** QA sandbox personas, Admin/operator


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** After isolated staging is provisioned with test credentials by operations, repeat variants: actual fee visibility and reporting access with test payments; unavailable fee remains held and manual reconciliation limitation documented. Record sandbox provider IDs, sanitized request/response, timestamps and UI/domain evidence. Do not perform these calls during documentation work.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected database/domain state:** Provider-specific behavior agrees with local contract and lifecycle matrix; no live charge or transfer. If test mode cannot supply authoritative reporting data mark BLOCKED; mock success is not delivery/reporting/hosting proof.


**Expected email/provider behavior:** Mollie reporting: collect sandbox provider-side confirmation and safe failure evidence; no production calls.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** STAGING ONLY


**Local/staging requirement:** STAGING


**Source / existing evidence:** MOLLIE


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="family-cala"></a>
