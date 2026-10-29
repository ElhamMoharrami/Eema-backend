<a id="family-doc"></a>
### DOC scenarios

<a id="doc-001"></a>
#### DOC-001 — Receipt eligibility and ownership

**Area:** DOC · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Download /bookings/{id}/receipt for confirmed+paid, cancelled-after-paid, refunded-after-paid; try REQUESTED, AWAITING_PAYMENT, unpaid EXPIRED and late-paid-refunded. Try scout A/B, owner and admin.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Eligible owner-of-booking SCOUT/admin gets PDF attachment; OWNER denied by role; other scout denied; ineligible 409. First generation requires paidAt, confirmedAt and positive totalPaid with no latePaymentDetectedAt.


**Expected database/domain state:** Eligible owner-of-booking SCOUT/admin gets PDF attachment; OWNER denied by role; other scout denied; ineligible 409. First generation requires paidAt, confirmedAt and positive totalPaid with no latePaymentDetectedAt.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** FinancialDocumentService, FinancialDocumentPdfRenderer, LocalPrivateDocumentStorage, S3PrivateDocumentStorage


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="doc-002"></a>
#### DOC-002 — Receipt content and immutability

**Area:** DOC · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Issue receipt, extract PDF text, then change live venue/name/rate and cancel booking; download again and compare SHA256.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Includes RCT number, booking ID, paid date/status at issuance, venue/neighbourhood/shoot/timezone, scout snapshot name/email, hourly/minimum/duration/billable/cap snapshots, venue price and total paid. Excludes commission/provider fee/owner payout/liabilities/IBAN/provider IDs; payment record, not tax invoice. Repeated bytes unchanged.


**Expected database/domain state:** Includes RCT number, booking ID, paid date/status at issuance, venue/neighbourhood/shoot/timezone, scout snapshot name/email, hourly/minimum/duration/billable/cap snapshots, venue price and total paid. Excludes commission/provider fee/owner payout/liabilities/IBAN/provider IDs; payment record, not tax invoice. Repeated bytes unchanged.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** FinancialDocumentService, FinancialDocumentPdfRenderer, LocalPrivateDocumentStorage, S3PrivateDocumentStorage


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="doc-003"></a>
#### DOC-003 — Payout statement content and scope

**Area:** DOC · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Get statement as owner/admin for PAID, then each nonpaid state; compare PDF ledger with payout; replace bank destination and re-download.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Only PAID with paidAt/reference first-issue eligible. Includes PST/booking/payout IDs, external transfer reference, historical masked bank, paidAt, owner/name/email, venue/time and gross/commission/provider/owner amounts. No unrelated liability or full IBAN; immutable repeat, not tax invoice.


**Expected database/domain state:** Only PAID with paidAt/reference first-issue eligible. Includes PST/booking/payout IDs, external transfer reference, historical masked bank, paidAt, owner/name/email, venue/time and gross/commission/provider/owner amounts. No unrelated liability or full IBAN; immutable repeat, not tax invoice.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** FinancialDocumentService, FinancialDocumentPdfRenderer, LocalPrivateDocumentStorage, S3PrivateDocumentStorage


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="doc-004"></a>
#### DOC-004 — Private storage failure, Unicode and layout

**Area:** DOC · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Cause private store failure, DB issue failure after store, then missing stored object after successful issue in isolated fixture. Download with long names, emoji, Unicode and maximal financial values.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** No success PDF on failed issue; cleanup attempted when DB save fails. Existing missing file errors instead of silent document rewrite. Renderer substitutes unsupported codepoints and truncates at 180; manually assess layout/cropped or changed name as product gap.


**Expected database/domain state:** No success PDF on failed issue; cleanup attempted when DB save fails. Existing missing file errors instead of silent document rewrite. Renderer substitutes unsupported codepoints and truncates at 180; manually assess layout/cropped or changed name as product gap.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** FinancialDocumentService, FinancialDocumentPdfRenderer, LocalPrivateDocumentStorage, S3PrivateDocumentStorage


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


