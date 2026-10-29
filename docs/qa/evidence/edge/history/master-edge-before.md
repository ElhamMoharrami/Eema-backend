### EDGE scenarios

<a id="edge-001"></a>
#### EDGE-001 — Sub-minute and midnight boundary

**Area:** EDGE · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Set threshold to 23:59:30 local, with current-day 22:00–23:59:59 availability; also request times with seconds/nanos; compare price minutes and next-day clipping.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Lead clip rounds up to minute; current LocalTime.plusMinutes can wrap midnight. Require no past slot offered as valid; record wraparound discrepancy. Price Duration.toMinutes truncates seconds; no invented minimum-second validation.


**Expected database/domain state:** Lead clip rounds up to minute; current LocalTime.plusMinutes can wrap midnight. Require no past slot offered as valid; record wraparound discrepancy. Price Duration.toMinutes truncates seconds; no invented minimum-second validation.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Source-specific edge paths; see gaps register


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="edge-002"></a>
#### EDGE-002 — DST missing/repeated local times

**Area:** EDGE · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Use next Europe/Amsterdam spring transition 02:30 (gap) and fall 02:30 (overlap); run request, availability, confirmation snapshot, cancellation and payout eligibility.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Java atZone normalization/earlier-offset behavior is current authority; UI must state timezone and avoid falsely precise duration claims. Compare elapsed cancellation/lead durations versus LocalTime booking duration; flag ambiguity.


**Expected database/domain state:** Java atZone normalization/earlier-offset behavior is current authority; UI must state timezone and avoid falsely precise duration claims. Compare elapsed cancellation/lead durations versus LocalTime booking duration; flag ambiguity.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Source-specific edge paths; see gaps register


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="edge-003"></a>
#### EDGE-003 — Matching size, neighbourhood, tags and candidate cap

**Area:** EDGE · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed 31 hard-eligible venues ordered by hourly asc, size desc, name asc; filter minimum size, neighbourhood casing and must-have tags; use structured feature tags contradicted by profile. Make first 30 unavailable but 31st available.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** DB filters and evaluator agree, structured tags cannot bypass facts; max 30 candidates limited before availability filter, so later available venue can be missed. Record candidate truncation product gap.


**Expected database/domain state:** DB filters and evaluator agree, structured tags cannot bypass facts; max 30 candidates limited before availability filter, so later available venue can be missed. Record candidate truncation product gap.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Source-specific edge paths; see gaps register


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="edge-004"></a>
#### EDGE-004 — Suspended request acceptance characterization

**Area:** EDGE · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Create REQUESTED while ACTIVE; suspend as ADMIN; accept existing request as owner before deadlines.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** VEN-D02 reproduced and fixed this gap: acceptance now checks ACTIVE under the venue row lock and returns 409 for a suspended REQUESTED booking. Existing AWAITING_PAYMENT remains payable; CONFIRMED history is preserved. New requests remain blocked. See VEN-010 and docs/qa/VEN_EXECUTION_REPORT.md for PostgreSQL race evidence.


**Expected database/domain state:** VEN-D02 reproduced and fixed this gap: acceptance now checks ACTIVE under the venue row lock and returns 409 for a suspended REQUESTED booking. Existing AWAITING_PAYMENT remains payable; CONFIRMED history is preserved. New requests remain blocked. See VEN-010 and docs/qa/VEN_EXECUTION_REPORT.md for PostgreSQL race evidence.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Source-specific edge paths; see gaps register


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="edge-005"></a>
#### EDGE-005 — Owner decline after response deadline

**Area:** EDGE · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Create REQUESTED; make ownerResponseExpiresAt past without running scheduler; decline as owner.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Current declineRequest only checks REQUESTED, not response deadline; can produce DECLINED before scheduler. Compare expiry email/status semantics and document inconsistent deadline enforcement.


**Expected database/domain state:** Current declineRequest only checks REQUESTED, not response deadline; can produce DECLINED before scheduler. Compare expiry email/status semantics and document inconsistent deadline enforcement.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Source-specific edge paths; see gaps register


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="edge-006"></a>
#### EDGE-006 — Unsupported compatibility service paths

**Area:** EDGE · **Priority:** P1 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Inspect VenueService getAllVenues/deleteVenue call sites, old BookerVerification/HeroFeature imports, unused refund/generic endpoints. Try DELETE /venues/{id} as authorized owner.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Service methods without controller routes are not public features; unsupported delete must not mutate. Record compatibility code and missing tests without adding new UI/API.


**Expected database/domain state:** Service methods without controller routes are not public features; unsupported delete must not mutate. Record compatibility code and missing tests without adding new UI/API.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Source-specific edge paths; see gaps register


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


