<a id="family-race"></a>
### RACE scenarios

<a id="race-001"></a>
#### RACE-001 — Booking acceptance

**Area:** RACE · **Priority:** P0 · **Test level:** CONCURRENCY, INTEGRATION, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest#overlappingRequestedRowsCoexistButConcurrentAcceptanceCannotDoubleReserve. Inspect fixture/barrier and assertions in referenced test. Then for service-level race, launch two independent authenticated requests with a barrier and separate DB connections; collect both outcomes and final row/event/provider counts.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected database/domain state:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** PostgresCriticalPathIntegrationTest#overlappingRequestedRowsCoexistButConcurrentAcceptanceCannotDoubleReserve


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="race-002"></a>
#### RACE-002 — Blocking insert

**Area:** RACE · **Priority:** P0 · **Test level:** CONCURRENCY, INTEGRATION, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest#exclusionRejectsOneOverlappingBlockingInsert. Inspect fixture/barrier and assertions in referenced test. Then for service-level race, launch two independent authenticated requests with a barrier and separate DB connections; collect both outcomes and final row/event/provider counts.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected database/domain state:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** PostgresCriticalPathIntegrationTest#exclusionRejectsOneOverlappingBlockingInsert


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="race-003"></a>
#### RACE-003 — Refresh rotation

**Area:** RACE · **Priority:** P0 · **Test level:** CONCURRENCY, INTEGRATION, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest#refreshTokenRotationLockAllowsOnlyOneWinner. Inspect fixture/barrier and assertions in referenced test. Then for service-level race, launch two independent authenticated requests with a barrier and separate DB connections; collect both outcomes and final row/event/provider counts.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected database/domain state:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** PostgresCriticalPathIntegrationTest#refreshTokenRotationLockAllowsOnlyOneWinner


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="race-004"></a>
#### RACE-004 — Payout uniqueness

**Area:** RACE · **Priority:** P0 · **Test level:** CONCURRENCY, INTEGRATION, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest#payoutAndDocumentUniqueConstraintsHaveSingleAuthorities. Inspect fixture/barrier and assertions in referenced test. Then for service-level race, launch two independent authenticated requests with a barrier and separate DB connections; collect both outcomes and final row/event/provider counts.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected database/domain state:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** PostgresCriticalPathIntegrationTest#payoutAndDocumentUniqueConstraintsHaveSingleAuthorities


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="race-005"></a>
#### RACE-005 — Verified destination

**Area:** RACE · **Priority:** P0 · **Test level:** CONCURRENCY, INTEGRATION, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest#verifiedDestinationPartialIndexAndReplacementSemanticsHold. Inspect fixture/barrier and assertions in referenced test. Then for service-level race, launch two independent authenticated requests with a barrier and separate DB connections; collect both outcomes and final row/event/provider counts.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected database/domain state:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** PostgresCriticalPathIntegrationTest#verifiedDestinationPartialIndexAndReplacementSemanticsHold


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="race-006"></a>
#### RACE-006 — Policy publication

**Area:** RACE · **Priority:** P0 · **Test level:** CONCURRENCY, INTEGRATION, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest#policyPublishRaceLeavesExactlyOneCurrentVersion. Inspect fixture/barrier and assertions in referenced test. Then for service-level race, launch two independent authenticated requests with a barrier and separate DB connections; collect both outcomes and final row/event/provider counts.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected database/domain state:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** PostgresCriticalPathIntegrationTest#policyPublishRaceLeavesExactlyOneCurrentVersion


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="race-007"></a>
#### RACE-007 — Document issuance

**Area:** RACE · **Priority:** P0 · **Test level:** CONCURRENCY, INTEGRATION, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest#payoutAndDocumentUniqueConstraintsHaveSingleAuthorities. Inspect fixture/barrier and assertions in referenced test. Then for service-level race, launch two independent authenticated requests with a barrier and separate DB connections; collect both outcomes and final row/event/provider counts.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected database/domain state:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** PostgresCriticalPathIntegrationTest#payoutAndDocumentUniqueConstraintsHaveSingleAuthorities


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="race-008"></a>
#### RACE-008 — SKIP LOCKED workers

**Area:** RACE · **Priority:** P0 · **Test level:** CONCURRENCY, INTEGRATION, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest#twoSkipLockedWorkersReceiveDisjointBoundedBookingBatches. Inspect fixture/barrier and assertions in referenced test. Then for service-level race, launch two independent authenticated requests with a barrier and separate DB connections; collect both outcomes and final row/event/provider counts.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected database/domain state:** Single authoritative winner or disjoint worker batches; no duplicate reservation/financial document/payout/current destination/current policy or refresh rotation. A raw SQL race proves constraint/lock behavior only; service transaction retries and HTTP status mapping need separate reproduction.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** PostgresCriticalPathIntegrationTest#twoSkipLockedWorkersReceiveDisjointBoundedBookingBatches


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


