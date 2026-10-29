<a id="family-job"></a>
### JOB scenarios

<a id="job-001"></a>
#### JOB-001 — BookingApprovalExpirationJob.expireUnpaidApprovals

**Area:** JOB · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed 101 eligible rows (REQUESTED and AWAITING_PAYMENT due at/before now) and one ineligible/future/terminal row. Invoke job, inspect changes and batch count; invoke again; run two workers and observe locks, errors and metrics.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** 100 per request/approval query; 60s default. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected database/domain state:** 100 per request/approval query; 60s default. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** BookingApprovalExpirationJob.expireUnpaidApprovals


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="job-002"></a>
#### JOB-002 — AvailabilityInquiryExpirationJob.expireOffers

**Area:** JOB · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed 101 eligible rows (OFFERED due at/before now) and one ineligible/future/terminal row. Invoke job, inspect changes and batch count; invoke again; run two workers and observe locks, errors and metrics.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** 100 per query; 60s default. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected database/domain state:** 100 per query; 60s default. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AvailabilityInquiryExpirationJob.expireOffers


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="job-003"></a>
#### JOB-003 — OwnerPayoutReadinessJob.scan

**Area:** JOB · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed 101 eligible rows (due eligible nonterminal payout rows selected by repository) and one ineligible/future/terminal row. Invoke job, inspect changes and batch count; invoke again; run two workers and observe locks, errors and metrics.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** 100; 300s default; transaction encloses SKIP LOCKED. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected database/domain state:** 100; 300s default; transaction encloses SKIP LOCKED. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** OwnerPayoutReadinessJob.scan


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="job-004"></a>
#### JOB-004 — AuthTokenCleanupJob.cleanup

**Area:** JOB · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed 101 eligible rows (used/expired verification and reset tokens) and one ineligible/future/terminal row. Invoke job, inspect changes and batch count; invoke again; run two workers and observe locks, errors and metrics.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** 100 each; 3600s default; not refresh-token housekeeping. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected database/domain state:** 100 each; 3600s default; not refresh-token housekeeping. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** AuthTokenCleanupJob.cleanup


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="job-005"></a>
#### JOB-005 — MollieFeeReconciliationJob.reconcile

**Area:** JOB · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed 101 eligible rows (paid CONFIRMED/CANCELLED unreconciled provider fee) and one ineligible/future/terminal row. Invoke job, inspect changes and batch count; invoke again; run two workers and observe locks, errors and metrics.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** 100; 3600s default, initial 60s; configuration enabled. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected database/domain state:** 100; 3600s default, initial 60s; configuration enabled. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** MollieFeeReconciliationJob.reconcile


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="job-006"></a>
#### JOB-006 — WorkflowObservability.scanStuckStates

**Area:** JOB · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION, CONCURRENCY · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed 101 eligible rows (old CREATING/active-refund/fee, HELD/READY/overdue and OPEN liabilities) and one ineligible/future/terminal row. Invoke job, inspect changes and batch count; invoke again; run two workers and observe locks, errors and metrics.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** read-only gauges; 300s default. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected database/domain state:** read-only gauges; 300s default. Only eligible rows affected; repeat has no duplicate business event. Do not assume every scheduled job holds a shared transaction: verify actual worker locking scope; counters reflect committed transitions and gauges are read-only.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** WorkflowObservability.scanStuckStates


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


