<a id="family-dest"></a>
### DEST scenarios

<a id="dest-001"></a>
#### DEST-001 — Destination submit and validation

**Area:** DEST · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Verified Owner A submits accountHolderName=QA Owner A and synthetic fixture IBAN from IbanPolicyTest (e.g. [synthetic IBAN omitted from evidence], never transfer). Try whitespace/lowercase, bad checksum, unsupported country, name blank/160/161 and IBAN 80/81 chars.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** EUR/IBAN only; normalization and country-length/mod97 checks; valid→PENDING_VERIFICATION; invalid 400; owner verification/email/current policies required.


**Expected database/domain state:** EUR/IBAN only; normalization and country-length/mod97 checks; valid→PENDING_VERIFICATION; invalid 400; owner verification/email/current policies required.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutDestinationService, PayoutDestinationCrypto, IbanPolicy, AdminPayoutDestinationController


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="dest-002"></a>
#### DEST-002 — Encrypted persistence and masked access

**Area:** DEST · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Inspect row ciphertext/fingerprint and normal owner/admin list/detail DTO; encrypt same IBAN twice; tamper ciphertext in isolated unit fixture.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** AES-256-GCM v1 with random nonce, HMAC fingerprint, last4 only outside reveal; different ciphertext but stable fingerprint; tamper fails closed. Full IBAN never present in logs/normal DTO.


**Expected database/domain state:** AES-256-GCM v1 with random nonce, HMAC fingerprint, last4 only outside reveal; different ciphertext but stable fingerprint; tamper fails closed. Full IBAN never present in logs/normal DTO.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutDestinationService, PayoutDestinationCrypto, IbanPolicy, AdminPayoutDestinationController


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="dest-003"></a>
#### DEST-003 — Review replacement and rejection

**Area:** DEST · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Submit first destination, verify; submit alternate supported fixture, leave pending, reject with reason; submit distinct replacement and verify; repeat verify/reject stale ID and duplicate same IBAN.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Original remains VERIFIED while replacement pending/rejected; new verify makes old REPLACED; exactly one VERIFIED. Same owner fingerprint duplicate conflicts, including previously rejected details; stale review 409.


**Expected database/domain state:** Original remains VERIFIED while replacement pending/rejected; new verify makes old REPLACED; exactly one VERIFIED. Same owner fingerprint duplicate conflicts, including previously rejected details; stale review 409.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutDestinationService, PayoutDestinationCrypto, IbanPolicy, AdminPayoutDestinationController


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="dest-004"></a>
#### DEST-004 — Audited reveal and ephemeral frontend value

**Area:** DEST · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** ADMIN detail clicks Reveal and explicitly confirms; inspect response headers; hide, navigate, review record, logout and switch ID. Repeat owner/scout/anonymous and REJECTED destination.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** POST reveal no-store and Pragma no-cache; audit REVEALED, no full IBAN in audit; only pending/verified/replaced allowed. Value cleared from component state; nonadmin denied; rejected state 409.


**Expected database/domain state:** POST reveal no-store and Pragma no-cache; audit REVEALED, no full IBAN in audit; only pending/verified/replaced allowed. Value cleared from component state; nonadmin denied; rejected state 409.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutDestinationService, PayoutDestinationCrypto, IbanPolicy, AdminPayoutDestinationController


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


