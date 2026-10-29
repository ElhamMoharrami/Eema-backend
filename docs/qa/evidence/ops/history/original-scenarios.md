# Historical generated OPS scenarios before execution

<a id="ops-001"></a>
#### OPS-001 — Fresh common migrations and Hibernate validation

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run mvn verify -Ppostgres-integration with Docker. Separately start same release against disposable empty PG16 with common-only Flyway location; query flyway_schema_history and health.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** V1–V11 common apply once and Hibernate validate succeeds; no development users/policies/venues in common-only DB; rerun produces no extra migration or data.


**Expected database/domain state:** V1–V11 common apply once and Hibernate validate succeeds; no development users/policies/venues in common-only DB; rerun produces no extra migration or data.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ops-002"></a>
#### OPS-002 — Local dev chain and production exclusion

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Start local profile against disposable empty DB; inspect V100–V102 plus V1–V11. Start prod-config validation fixture excluding all dev locations.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Local common+dev chain includes seed production profiles/lead times; prod common only. Prod fails missing/weak secrets, local storage, wildcard CORS, invalid/shared buckets as validator specifies; do not provision infrastructure here.


**Expected database/domain state:** Local common+dev chain includes seed production profiles/lead times; prod common only. Prod fails missing/weak secrets, local storage, wildcard CORS, invalid/shared buckets as validator specifies; do not provision infrastructure here.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ops-003"></a>
#### OPS-003 — Constraint and foreign-key behavior

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** In separate transactions violate blocking overlap, payout booking uniqueness, VERIFIED-per-owner, document aggregate uniqueness, current policy per type and policy/incident referenced-parent delete; rollback each.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Constraint rejects invalid write, previous authoritative row intact; half-open adjacency and overlapping REQUESTED allowed. ON DELETE RESTRICT preserves financial/legal history; use actual migration FK action for every entity.


**Expected database/domain state:** Constraint rejects invalid write, previous authoritative row intact; half-open adjacency and overlapping REQUESTED allowed. ON DELETE RESTRICT preserves financial/legal history; use actual migration FK action for every entity.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ops-004"></a>
#### OPS-004 — Backup and isolated restore

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** On disposable local DB with paid/cancelled/inquiry/policy/destination/document fixtures run backup commands below; restore into separate eema_qa_restore DB; start same jar against restore on separate port with external delivery disabled; inspect representative records.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Counts/IDs/snapshots/constraints and health agree. Restore private/public object copies and payout encryption/fingerprint keys too; SQL backup alone cannot recover files/encrypted bank access. Never overwrite source DB.


**Expected database/domain state:** Counts/IDs/snapshots/constraints and health agree. Restore private/public object copies and payout encryption/fingerprint keys too; SQL backup alone cannot recover files/encrypted bank access. Never overwrite source DB.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ops-005"></a>
#### OPS-005 — Backend restart and frontend reload

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Create representative states; restart backend without volume deletion, reload frontend, revisit owner/scout/admin deep links and PDFs.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Postgres/volume data, idempotency claims, sessions and document bytes survive ordinary restart. In-memory notification work can be lost; no durable mail outbox exists.


**Expected database/domain state:** Postgres/volume data, idempotency claims, sessions and document bytes survive ordinary restart. In-memory notification work can be lost; no durable mail outbox exists.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ops-006"></a>
#### OPS-006 — Local reset and reseed rehearsal

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** On disposable test project only follow reset options below; after reset re-register personas/reseed local chain and rebuild needed trust/policy fixtures.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Full reset deletes all database/upload-volume data; targeted reset respects FKs; common migrations contain no legal policy content. No automatic PASS carried across reset.


**Expected database/domain state:** Full reset deletes all database/upload-volume data; targeted reset respects FKs; common migrations contain no legal policy content. No automatic PASS carried across reset.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ops-007"></a>
#### OPS-007 — Fee metric versus exception consistency

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Seed old unreconciled paid CONFIRMED and CANCELLED rows; run metrics scan and exception query with same thresholds; create >200 exceptions in one category.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Thresholds share OperationalThresholds, but fee gauge currently counts only CONFIRMED whereas queue includes paid CANCELLED; queue caps 200 candidates/category while gauges count full set. Record mismatch, not false equality.


**Expected database/domain state:** Thresholds share OperationalThresholds, but fee gauge currently counts only CONFIRMED whereas queue includes paid CANCELLED; queue caps 200 candidates/category while gauges count full set. Record mismatch, not false equality.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ops-008"></a>
#### OPS-008 — Email asynchronous failure and restart recovery

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Mock one recipient send failure, missing entity, event rollback, queue saturation and process restart after commit before delivery. Inspect lifecycle response and emails.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** After-commit listeners avoid rolled-back messages; delivery failure must not roll back business success, other recipients attempted where implemented. No persistent retry/outbox guarantee; missing delivery becomes operations follow-up.


**Expected database/domain state:** After-commit listeners avoid rolled-back messages; delivery failure must not roll back business success, other recipients attempted where implemented. No persistent retry/outbox guarantee; missing delivery becomes operations follow-up.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ops-009"></a>
#### OPS-009 — Reporting pagination and unavailable fee

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Mock reporting no fee, EUR fee, non-EUR, malformed body, pagination outside Mollie origin, loop and provider failure; run reconciliation job enabled then disabled.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** No fee invented; invalid/reporting failure deferred and next booking processed; disabled job no call. Ordinary test payment API does not prove real reporting/fee availability.


**Expected database/domain state:** No fee invented; invalid/reporting failure deferred and next booking processed; disabled job no call. Ordinary test payment API does not prove real reporting/fee availability.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** Flyway common/dev, ProductionConfigurationValidator, repositories, deployment docs


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="family-job"></a>
### JOB scenarios

