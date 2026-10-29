### PAYOUT scenarios

<a id="payout-001"></a>
#### PAYOUT-001 — Payout creation economics

**Area:** PAYOUT · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** On CONFIRMED payment calculate retained gross, commission and actual provider fee; repeat recalculation. Also test zero entitlement, cancellation, full refund, late payment and no existing payout.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** At most one payout per booking; zero entitlement CANCELLED; no normal late-payment payout; fees must be authoritative and monetary values nonnegative. Snapshot commission differs by configured profile, never hardcode 20% for local.


**Expected database/domain state:** At most one payout per booking; zero entitlement CANCELLED; no normal late-payment payout; fees must be authoritative and monetary values nonnegative. Snapshot commission differs by configured profile, never hardcode 20% for local.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutService, OwnerFinancialLiabilityService, BookingFinancials


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="payout-002"></a>
#### PAYOUT-002 — Payout readiness precedence

**Area:** PAYOUT · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Use positive entitlement and independently omit: completed shoot, reconciled fee, completed refund, verified destination. Then supply all. Combine missing prerequisites to test precedence.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Zero→CANCELLED first; active refund→HELD/REFUND_IN_PROGRESS; missing fee→HELD/WAITING_FOR_PROVIDER_COST; before end→CALCULATED/WAITING_FOR_BOOKING_END; missing destination→HELD/WAITING_FOR_PAYOUT_DESTINATION; else READY.


**Expected database/domain state:** Zero→CANCELLED first; active refund→HELD/REFUND_IN_PROGRESS; missing fee→HELD/WAITING_FOR_PROVIDER_COST; before end→CALCULATED/WAITING_FOR_BOOKING_END; missing destination→HELD/WAITING_FOR_PAYOUT_DESTINATION; else READY.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutService, OwnerFinancialLiabilityService, BookingFinancials


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="payout-003"></a>
#### PAYOUT-003 — Fee reconciliation idempotency and responsibility

**Area:** PAYOUT · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Invoke BookingService.reconcileMollieFee on paid booking with fee 1.23; repeat 1.23 then 1.24; negative fee and unpaid booking; repeat owner/admin/scout cancellation fixtures.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Same fee idempotent; conflicting second fee 409; negative/unpaid rejected; owner cancellation creates positive PROVIDER_COST liability, admin responsibility EEMA does not charge owner; payout never negative.


**Expected database/domain state:** Same fee idempotent; conflicting second fee 409; negative/unpaid rejected; owner cancellation creates positive PROVIDER_COST liability, admin responsibility EEMA does not charge owner; payout never negative.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutService, OwnerFinancialLiabilityService, BookingFinancials


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="payout-004"></a>
#### PAYOUT-004 — Destination snapshot on READY

**Area:** PAYOUT · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Make payout READY using verified destination A; submit B pending then verify B; recalculate old payout and create future eligible payout.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Old payout keeps destination A even when REPLACED; future payout uses B. Single snapshot trust event on first READY; replacement never redirects READY/PAID history.


**Expected database/domain state:** Old payout keeps destination A even when REPLACED; future payout uses B. Single snapshot trust event on first READY; replacement never redirects READY/PAID history.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutService, OwnerFinancialLiabilityService, BookingFinancials


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="payout-005"></a>
#### PAYOUT-005 — Mark paid records external transfer

**Area:** PAYOUT · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** ADMIN opens READY payout and enters QA-TRANSFER-RUN; confirm; repeat same trimmed reference, different reference, empty and >200. Attempt all other payout states and wrong roles.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** READY→PAID; paidAt/reference recorded; same reference idempotent, different 409; validation 400; no provider transfer call. PAID immutable; historical destination must be VERIFIED or REPLACED.


**Expected database/domain state:** READY→PAID; paidAt/reference recorded; same reference idempotent, different 409; validation 400; no provider transfer call. PAID immutable; historical destination must be VERIFIED or REPLACED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutService, OwnerFinancialLiabilityService, BookingFinancials


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="payout-006"></a>
#### PAYOUT-006 — Liability settlement and WAIVED

**Area:** PAYOUT · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Create PROVIDER_COST and OWNER_PAYOUT_RECOVERY OPEN fixtures; ADMIN settles twice; attempt owner/scout and seeded WAIVED; include arbitrary amount field.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Stored amount unchanged; OPEN→SETTLED audited once; same settlement idempotent; WAIVED cannot settle. Enum exists but no waive or amount-edit endpoint.


**Expected database/domain state:** Stored amount unchanged; OPEN→SETTLED audited once; same settlement idempotent; WAIVED cannot settle. Enum exists but no waive or amount-edit endpoint.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutService, OwnerFinancialLiabilityService, BookingFinancials


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="family-dest"></a>
