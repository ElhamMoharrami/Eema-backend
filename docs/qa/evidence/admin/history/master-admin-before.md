### ADMIN scenarios

<a id="admin-001"></a>
#### ADMIN-001 — Every admin queue pagination and filters

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Admin; anonymous, Scout A and Owner A denial probes


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** For each /admin list route seed 25 records with mixed states; use filter fields independently and combined; page forward/back, reload URL and clear filters; test size 1/20/100/101, page negative, malformed UUID/enum and unsupported sort.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Controlled filters scope results; bounded page size at most 100; empty page not a crash; details match list IDs. Service-defined ordering takes precedence where query fixes it. No generic status or amount edit controls.


**Expected database/domain state:** Controlled filters scope results; bounded page size at most 100; empty page not a crash; details match list IDs. Service-defined ordering takes precedence where query fixes it. No generic status or amount edit controls.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Admin*Controller, AdminOperationsService, AdminPagePolicy, useAdminPage


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-002"></a>
#### ADMIN-002 — Booking search and detail composition

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Admin; anonymous, Scout A and Owner A denial probes


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Search bookingId, venueId, scoutId, each BookingStatus, Mollie payment ID (including whitespace), each RefundStatus and PaymentCreationStatus, singly and combined; open detail.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Only matching records returned; booking detail composes stored commission, fee, refund, cancellation, payout and liabilities, without calculating replacement money in UI.


**Expected database/domain state:** Only matching records returned; booking detail composes stored commission, fee, refund, cancellation, payout and liabilities, without calculating replacement money in UI.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Admin*Controller, AdminOperationsService, AdminPagePolicy, useAdminPage


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-003"></a>
#### ADMIN-003 — Booking event timeline payload allowlist

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Admin; anonymous, Scout A and Owner A denial probes


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Create >20 ordered events with permitted payload fields then password/token/iban/unknown keys; request all pages and inspect frontend rendering; put nested sensitive content under an allowlisted key.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Chronological pagination; unallowlisted top-level keys removed. Allowlist is shallow, so nested-value safety must be evaluated separately. Missing booking 404; nonadmin denied.


**Expected database/domain state:** Chronological pagination; unallowlisted top-level keys removed. Allowlist is shallow, so nested-value safety must be evaluated separately. Missing booking 404; nonadmin denied.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Admin*Controller, AdminOperationsService, AdminPagePolicy, useAdminPage


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-004"></a>
#### ADMIN-004 — Review mutation stale-state recovery

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Admin; anonymous, Scout A and Owner A denial probes


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** For owner verification, venue moderation, payout destination, incident and policy actions load detail in two tabs; execute competing review; submit stale action in first.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Invalid second transition 409 or current endpoint validation code; frontend should refetch authoritative state and clear sensitive input. Record routes that only show generic error and require manual refresh.


**Expected database/domain state:** Invalid second transition 409 or current endpoint validation code; frontend should refetch authoritative state and clear sensitive input. Record routes that only show generic error and require manual refresh.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Admin*Controller, AdminOperationsService, AdminPagePolicy, useAdminPage


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-005"></a>
#### ADMIN-005 — Exception PAYOUT_READY_OVERDUE

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed READY older than 72h; open /admin/exceptions and GET /admin/operations/exceptions. Test exact cutoff and one second either side where age applies; follow every booking/payout/liability link.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** PAYOUT_READY_OVERDUE severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected database/domain state:** PAYOUT_READY_OVERDUE severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AdminOperationsService#exceptions, OperationalThresholds


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-006"></a>
#### ADMIN-006 — Exception PAYOUT_HELD

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed HELD with each hold reason; open /admin/exceptions and GET /admin/operations/exceptions. Test exact cutoff and one second either side where age applies; follow every booking/payout/liability link.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** PAYOUT_HELD severity INFO; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected database/domain state:** PAYOUT_HELD severity INFO; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AdminOperationsService#exceptions, OperationalThresholds


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-007"></a>
#### ADMIN-007 — Exception OWNER_LIABILITY_OPEN

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed OPEN liability; open /admin/exceptions and GET /admin/operations/exceptions. Test exact cutoff and one second either side where age applies; follow every booking/payout/liability link.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** OWNER_LIABILITY_OPEN severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected database/domain state:** OWNER_LIABILITY_OPEN severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AdminOperationsService#exceptions, OperationalThresholds


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-008"></a>
#### ADMIN-008 — Exception REFUND_STUCK

**Area:** ADMIN · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed active refund older than 1440 minutes; open /admin/exceptions and GET /admin/operations/exceptions. Test exact cutoff and one second either side where age applies; follow every booking/payout/liability link.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** REFUND_STUCK severity CRITICAL; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected database/domain state:** REFUND_STUCK severity CRITICAL; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AdminOperationsService#exceptions, OperationalThresholds


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-009"></a>
#### ADMIN-009 — Exception PAYMENT_CREATION_STUCK

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed CREATING older than 60 minutes; open /admin/exceptions and GET /admin/operations/exceptions. Test exact cutoff and one second either side where age applies; follow every booking/payout/liability link.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** PAYMENT_CREATION_STUCK severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected database/domain state:** PAYMENT_CREATION_STUCK severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AdminOperationsService#exceptions, OperationalThresholds


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-010"></a>
#### ADMIN-010 — Exception MOLLIE_FEE_UNRECONCILED

**Area:** ADMIN · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed paid CONFIRMED/CANCELLED, fee missing, paidAt older than 1440 minutes; open /admin/exceptions and GET /admin/operations/exceptions. Test exact cutoff and one second either side where age applies; follow every booking/payout/liability link.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** MOLLIE_FEE_UNRECONCILED severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected database/domain state:** MOLLIE_FEE_UNRECONCILED severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AdminOperationsService#exceptions, OperationalThresholds


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="admin-011"></a>
#### ADMIN-011 — Exception LATE_PAYMENT_REFUND_IN_PROGRESS

**Area:** ADMIN · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed latePaymentDetectedAt set and active refund; open /admin/exceptions and GET /admin/operations/exceptions. Test exact cutoff and one second either side where age applies; follow every booking/payout/liability link.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** LATE_PAYMENT_REFUND_IN_PROGRESS severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected database/domain state:** LATE_PAYMENT_REFUND_IN_PROGRESS severity WARNING; correct linked IDs and reason; sort CRITICAL then WARNING then INFO, oldest within severity. Read-only queue performs no remediation. Age checks use strict before cutoff.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AdminOperationsService#exceptions, OperationalThresholds


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


