<a id="family-ops"></a>
### OPS scenarios

<a id="ops-001"></a>
#### OPS-001 — Fresh common migrations and Hibernate validation

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** PASS

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Start the packaged release against an empty disposable PostgreSQL database with only common migrations; compare exact versions and business-table emptiness, then restart and compare all table hashes.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** Common V1–V13 apply once; Hibernate validation succeeds; users, venues, bookings and policy_versions remain empty. Restart changes no persisted rows.


**Expected database/domain state:** Common V1–V13 apply once; Hibernate validation succeeds; users, venues, bookings and policy_versions remain empty. Restart changes no persisted rows.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


<a id="ops-002"></a>
#### OPS-002 — Local dev chain and production exclusion

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** PASS

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Start the packaged local profile on a fresh disposable database; assert common1–13/dev100–103, deterministic development seeds and seed login. Audit prod common-only profile and exercise required production configuration rejection/acceptance.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** Development fixtures belong only to local/dev migration locations. Production validates required configuration; unit validation is not a deployed S3/payment/email check. Neither common nor development migrations supply approved legal policy content.


**Expected database/domain state:** Development fixtures belong only to local/dev migration locations. Production validates required configuration; unit validation is not a deployed S3/payment/email check. Neither common nor development migrations supply approved legal policy content.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


<a id="ops-003"></a>
#### OPS-003 — Constraint and foreign-key behavior

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** PASS

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Reuse the freshly executed critical-path and financial/inventory domain PostgreSQL suites. Add policy/incident parent-delete rollback and actual restrict-FK catalog assertions.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** Exclusion, financial identities, destination/document/policy uniqueness and referenced-parent restrictions remain enforced by PostgreSQL. A rejected deletion preserves parent and referencing rows.


**Expected database/domain state:** Exclusion, financial identities, destination/document/policy uniqueness and referenced-parent restrictions remain enforced by PostgreSQL. A rejected deletion preserves parent and referencing rows.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


<a id="ops-004"></a>
#### OPS-004 — Backup and isolated restore

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** PASS

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Back up synthetic populated PostgreSQL using pg_dump; restore to a separate DB. Copy public/private objects and recover synthetic bank encryption/fingerprint keys. Start the same packaged release, compare all table hashes and constraint definitions, then test documents/media, inquiry, destination reveal, refresh rotation and source isolation.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** Restored rows/IDs and constraints match, allowing equivalent PostgreSQL expression formatting. Receipt/media bytes are identical; authorized access remains enforced; correct keys decrypt and wrong keys fail. Existing restored refresh rotates once; original source stays unchanged. Production backup/secret-escrow/RPO/RTO is staging/operations scope.


**Expected database/domain state:** Restored rows/IDs and constraints match, allowing equivalent PostgreSQL expression formatting. Receipt/media bytes are identical; authorized access remains enforced; correct keys decrypt and wrong keys fail. Existing restored refresh rotates once; original source stays unchanged. Production backup/secret-escrow/RPO/RTO is staging/operations scope.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


<a id="ops-005"></a>
#### OPS-005 — Backend restart and frontend reload

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** PASS

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Restart the actual packaged backend against its existing isolated database/files; compare all table hashes and receipt bytes. Reload scout/owner/admin deep links and fetch the receipt in the browser using the existing session.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** Bookings, durable payment creation claims, sessions, policies, inquiry/document/destination rows and file bytes survive ordinary restart. Browser state agrees with PostgreSQL. Pending process-local notification work is separately characterized under OPS-008.


**Expected database/domain state:** Bookings, durable payment creation claims, sessions, policies, inquiry/document/destination rows and file bytes survive ordinary restart. Browser state agrees with PostgreSQL. Pending process-local notification work is separately characterized under OPS-008.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


<a id="ops-006"></a>
#### OPS-006 — Local reset and reseed rehearsal

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** PASS

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Run scripts/qa/ops-reset-rehearsal.py on a fresh uniquely named local QA Compose project. Assert project volume ownership, reset actual database/public/private volumes, recreate and verify missing markers, exact migrations, deterministic seeds and seed login. Separately verify packaged DB/object-directory reset in the recovery harness.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** Only owned disposable resources are destroyed; shared eema-backend volumes are untouched. Database and file-volume markers disappear;10 venue seeds return; policy_versions stays empty. All owned containers/volumes and temporary keys/files are cleaned.


**Expected database/domain state:** Only owned disposable resources are destroyed; shared eema-backend volumes are untouched. Database and file-volume markers disappear;10 venue seeds return; policy_versions stays empty. All owned containers/volumes and temporary keys/files are cleaned.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


<a id="ops-007"></a>
#### OPS-007 — Fee metric versus exception consistency

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** PASS

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** With fixed Clock, compare fee gauge and the complete exception queue before/after205 qualifying paid-history rows across CONFIRMED/CANCELLED/REFUNDED. Test cutoff minus/equal/plus one microsecond and assert read-only snapshots.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** Gauge and queue include the same positive-paid unreconciled rows strictly older than the shared cutoff. Equality and later are excluded. Results beyond200 are reachable; querying/scanning changes no booking state.


**Expected database/domain state:** Gauge and queue include the same positive-paid unreconciled rows strictly older than the shared cutoff. Equality and later are excluded. Results beyond200 are reachable; querying/scanning changes no booking state.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


<a id="ops-008"></a>
#### OPS-008 — Email asynchronous failure and restart recovery

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** BLOCKED

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Exercise missing booking and one-recipient failure; publish through real PostgreSQL commit/rollback and the actual transactional listener; saturate the configured bounded executor with deterministic latches. In the packaged JVM, pause the listener after business commit, kill the process, restart and prove booking survival plus absent old notification replay while new delivery processes.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** Rollback emits no delivery. Delivery failure cannot undo committed business state; other recipients are attempted where implemented. OPS-DEF-001 binds unqualified Async listeners to the existing bounded pool; saturation applies CallerRuns backpressure. Abrupt death can lose pending notification permanently. No durable ledger, automatic replay or complete missing-email queue exists. OPS-DEC-001 requires explicit v1 launch acceptance; characterization is not approval.


**Expected database/domain state:** Rollback emits no delivery. Delivery failure cannot undo committed business state; other recipients are attempted where implemented. OPS-DEF-001 binds unqualified Async listeners to the existing bounded pool; saturation applies CallerRuns backpressure. Abrupt death can lose pending notification permanently. No durable ledger, automatic replay or complete missing-email queue exists. OPS-DEC-001 requires explicit v1 launch acceptance; characterization is not approval.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


<a id="ops-009"></a>
#### OPS-009 — Reporting pagination and unavailable fee

**Area:** OPS · **Priority:** P0 · **Test level:** OPERATIONAL, INTEGRATION · **Result:** PASS

**Roles involved:** Operator-controlled disposable QA resources; scout/owner/admin only where actual booking/document/session access is exercised. No generic actor matrix for Flyway or maintenance.


**Preconditions:** Local Docker and packaged current release; isolated synthetic fixtures and disabled/mocked providers. Never reset shared local or staging history.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Mock the real reporting HTTP client for missing token, unavailable fee, valid exact EUR, non-EUR, malformed fee/body, multiple pages, foreign origin, loop and provider failure. Exercise enabled/disabled reconciliation and continuation after per-booking failure/no-fee.


**Expected UI behavior:** Where a browser is relevant, role controls and reload reflect persisted booking state and immutable receipt access. Database-only checks have no separate UI contract.


**Expected API/backend behavior:** No fee is invented. Unsafe/invalid reporting is rejected/deferred; the next booking is processed; disabled job makes no provider/query calls. Same-origin pagination retrieves authoritative EUR fees. Real Mollie reporting entitlement/results remain staging-only.


**Expected database/domain state:** No fee is invented. Unsafe/invalid reporting is rejected/deferred; the next booking is processed; disabled job makes no provider/query calls. Same-origin pagination retrieves authoritative EUR fees. Real Mollie reporting entitlement/results remain staging-only.


**Expected email/provider behavior:** Providers disabled or mocked; assert calls appropriate to the tested transition. No real provider delivery or settlement claim.


**Security/authorization expectation:** Preserve existing endpoint authorization and private-document access. Credentials, tokens, dumps and full bank details are excluded from evidence.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [OPS execution report](qa/OPS_EXECUTION_REPORT.md), [local recovery runbook](qa/OPS_LOCAL_RECOVERY_RUNBOOK.md), sanitized qa/evidence/ops; existing AUTH/POL/BK/PAY/REF/DEST/PAYOUT/DOC/ADMIN/EDGE reports.


**Pass/fail criteria:** PASS requires recorded local execution and required regression gates. OPS-008 remains BLOCKED until OPS-DEC-001 launch acceptance is explicit; passing technical characterization alone does not close it. Real providers/deployed recovery remain separate staging requirements.


