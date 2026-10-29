<a id="family-doc"></a>
### DOC scenarios

Executed against the current source contract on 2026-09-19: **3 LOCAL PASS, 0 FAIL, 1 BLOCKED (product decision)**. All implemented local regression checks pass. Four reproduced P1 correctness defects were fixed; DOC-DEC-001 remains a genuine identity-rendering decision. See [DOC execution report](qa/DOC_EXECUTION_REPORT.md) and [sanitized evidence](qa/evidence/doc/). No real provider or legal/tax approval is claimed.

<a id="doc-001"></a>
#### DOC-001 — Receipt eligibility and ownership

**Area:** DOC · **Priority:** P0 · **Test level:** POSTGRESQL, BROWSER · **Result:** PASS (local)

**Corrected contract:** Authenticated booking SCOUT or ADMIN may download. OWNER and unrelated scouts are denied. Historical reads do not require newly verified email, current policy acceptance or owner trust. First issuance requires CONFIRMED/CANCELLED/REFUNDED plus paidAt, confirmedAt, positive totalPaid and no latePaymentDetectedAt. Previously issued bytes remain downloadable by authorized actors without recalculating current economics.

**Executed:** All three eligible and all five ineligible booking states; missing paid/confirmed timestamps, zero paid total and late-paid history; role/object/anonymous denial both before and after issuance; unverified/stale-policy historical reads. No denied issuance creates a document/object/audit or provider effect. Successful first issue creates one audit; repeated reads return identical bytes. Three barrier-coordinated concurrent request pairs each produce one persisted document, object and audit. HTTP response is an attachment, application/pdf, exact content length and no-store. The approved PRICE-DEF-005 minimum remains enforced upstream; no free-receipt lifecycle was introduced.

<a id="doc-002"></a>
#### DOC-002 — Receipt content and immutability

**Area:** DOC · **Priority:** P0 · **Test level:** UNIT, POSTGRESQL, PDF, BROWSER · **Result:** PASS (local)

**Authoritative content:** RCT number, booking ID, paid date/status at issuance, venue metadata at first issue, saved scout contact and monetary/duration/rate/minimum/cap snapshots. Internal commission, provider cost, owner payout, liability, provider IDs and full bank values are excluded. It is a payment record, not a claim of tax-invoice compliance.

**Executed:** Extract actual generated PDF text and compare exact saved values. Change current venue name/rate/neighbourhood and cancel booking; subsequent file bytes stay identical. Browser receipt download and reload have identical SHA-256 and safe RCT filenames. No fresh lifecycle email or provider payment is issued by download. Initial repeated request identity is protected by PostgreSQL locks/uniqueness, not by a client cache.

<a id="doc-003"></a>
#### DOC-003 — Payout statement content and scope

**Area:** DOC · **Priority:** P0 · **Test level:** UNIT, POSTGRESQL, PDF, BROWSER · **Result:** PASS (local)

**Corrected contract:** Same payout OWNER or ADMIN only. First issuance requires PAID, paidAt and nonblank external transfer reference. CALCULATED/HELD/READY/CANCELLED and legacy missing-reference rows conflict. New statement reads the immutable paid ledger and historical destination last4, not the owner's latest bank details. Owner/venue labels are captured at first issue and stored bytes are authoritative thereafter.

**Executed:** Exact PST/booking/payout references, complete transfer reference, masked historical REPLACED destination, gross/commission/provider cost/net payout and disclaimer. Unrelated owner/scout/anonymous denial; no internal liability or encrypted/full bank value. Owner/venue edits do not rewrite issued bytes. Three simultaneous first-download pairs each issue once. Browser statement download/reload preserves bytes and role controls. PAYOUT/DEST replacement suites supply the existing immutable-routing regression.

<a id="doc-004"></a>
#### DOC-004 — Private storage failure, Unicode and layout

**Area:** DOC · **Priority:** P0 · **Test level:** UNIT, POSTGRESQL, PDF, BROWSER · **Result:** BLOCKED (DOC-DEC-001 product decision; local implementation checks PASS)

**Technical results:** Storage failure, actual DB constraint failure after store, and transaction-commit failure return safe errors without issued metadata/audit; rollback attempts deletion of the new private object. Missing already-issued bytes fail without silent regeneration. Local opaque storage is inaccessible through public media routes. S3 request mocks prove private bucket/key, conditional writes and rejection of invalid references; real S3 remains staging-only.

**Defects fixed:** DOC-D1, local ATOMIC_MOVE could overwrite an existing object; completed bytes are now published atomically using a non-replacing hard link. DOC-D2, fixed-character wrapping clipped wide text and the 180-character limit truncated persisted references; layout now measures glyph widths, preserves full values and paginates before the footer. DOC-D3, an unsupported control character caused rendering failure; font-encoding checks apply a safe fallback. DOC-D4, late transaction rollback did not clean the file; cleanup now follows transaction completion. All four are P1 and have passing regressions. New documents use template version 2; existing issued files are unchanged.

**Verified visuals:** Wide names, complete 200-character transfer reference, maximal stored amounts, control/Unicode input, receipt/statement controls and missing-file error. No clipping/overlap remains in reviewed outputs. Browser delayed response after logout cannot trigger a download. Local publication requires filesystem hard-link support; deployment must verify that capability, private permissions and backups.

**Unresolved decision:** The existing Latin-font contract still changes unsupported names such as 李雷 to `??`. Tests prove the substitution but cannot approve financial identity loss or select a broader supported-script/font policy. DOC-DEC-001 was presented to the user with a rendered sample. No approval is inferred from silence, and no new Unicode/font feature is implemented by assumption.

**Continuation gate:** Stop at DOC-DEC-001. NEXT UNTESTED DOMAIN: INC-001–002, covering participant incident reports and admin incident handling; not started while the DOC decision remains unresolved.

