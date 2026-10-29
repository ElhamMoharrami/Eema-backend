# DOC execution report

**Executed 2026-09-19: DOC-001–004 — 3 LOCAL PASS, 0 FAIL, 1 BLOCKED (DOC-DEC-001 product decision).** All implemented local technical checks and required full backend regressions pass. Four reproduced P1 defects are fixed. Unicode identity policy remains undecided; no automatic continuation is authorized while that decision is pending.

## Inventory

| Scenario | Final status | Scope |
|---|---|---|
| DOC-001 | PASS | Receipt eligibility and ownership |
| DOC-002 | PASS | Receipt contents and immutable issuance |
| DOC-003 | PASS | Payout statement scope and historical ledger |
| DOC-004 | BLOCKED — product decision; local technical checks PASS | Private storage failures, Unicode and layout |

## Dependencies, source audit and corrected assumptions

At the initial audit, all four master scenarios were NOT RUN and no DOC execution report existed. The following audit records the pre-fix source contract. Dependencies: AUTH (actor/session), PRICE (stored positive original-price contract and unchanged canonical arithmetic), PAY/CANCEL/REF (paid/confirmed/late-payment history), PAYOUT (immutable PAID ledger/reference/routing), DEST (historical masked destination and privacy), MEDIA (public/private storage separation).

Correct current contract:
- Receipt GET: SCOUT or ADMIN role; same booking scout unless admin. First issuance requires CONFIRMED/CANCELLED/REFUNDED, paidAt, confirmedAt, positive totalPaid, no latePaymentDetectedAt. Existing authorized issued bytes are reused before re-evaluating current eligibility. No email/current-policy/trust gate on historical read.
- Statement GET: OWNER or ADMIN; same payout owner unless admin. First issuance PAID + paidAt + nonblank reference. Reads exact PAID ledger and historical destination last4. Live owner/venue labels are captured on first issue; only stored file is authoritative afterward.
- Booking/payout locks serialize issuance; unique constraints and sequences enforce document identity. No provider/payment/lifecycle call on download. One issuance audit only.
- Store writes before DB flush; catch attempts deletion on immediate DB failure. Probe commit-time rollback separately, not just mocked save. Existing missing object must fail safely and not regenerate.
- Local storage resolves opaque relative references outside public uploads; S3 uses private bucket/prefix and If-None-Match. Real S3 durability/privacy are staging-only. Probe local overwrite semantics and storage paths.
- Renderer uses Helvetica, replaces non-Latin-1 characters, truncates to180 and wraps by62characters; source alone cannot prove bounds. Probe long wide valid names/reference, control/Unicode and numeric maxima; inspect rendered pages. Text loss or clipping should be classified from concrete results, not assumed acceptable.
- UI existing receipt/statement controls and binary client; role visibility, reload, 409/error, delayed cross-session blob must be checked in representative browser/client flow.

Plan: real PostgreSQL eligibility/role/immutability/fault/concurrent-issuance cases; unit storage/renderer bounds; actual PDF extraction and visual review; representative browser receipt and statement download/reload, errors and role scope. Report-first before any DOC production edits. No legal/tax-policy change or new document type.


## Existing and missing coverage

Reuse FinancialDocumentServiceTest (6), FinancialDocumentPdfRendererTest (2), LocalPrivateDocumentStorageTest (2), PRICE downstream rendering snapshots and existing PAY/PAYOUT business-state regression. Missing locally executed DOC evidence: database issuance uniqueness/transactions, complete role/eligibility boundaries, real stored immutable bytes, storage/commit faults, representative browser downloads and measured/visual PDF layout. No business or tax policy is inferred from renderer text.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review document wording, download discoverability and mobile layout; assess whether identity rendering and the payment-record/not-tax-invoice wording are understandable. Automated access, money, concurrency and rollback checks remain the agent's responsibility.

## PRODUCT / OPERATIONS DECISIONS

No new document types, tax semantics, retention policy or provider integration is authorized. Any concrete name-rendering or document-policy ambiguity will be recorded before continuation.

## STAGING ONLY

Real private S3 access/durability, deployment filesystem permissions/backups, multi-instance storage and legal/tax wording approval. Local mocks do not verify these.


## Historical baseline and independent reproduction

Five boundary unit checks exposed four failed assertions and one renderer error: sequential/concurrent local store overwrites, out-of-page text (118 measured glyphs), lost transfer-reference tail, and unsupported control character failure. Visual rendering independently confirms wide names and transfer references run off the right page edge. The first PostgreSQL run executed32 cases:23 pass, one real rollback-cleanup failure, eight payout fixture errors. The latter omitted required verified/replaced metadata and are harness errors; they were corrected before independent rerun. The earlier immutable-contact compilation mistake executed no product checks.

Provisional defects: DOC-D1 P1 local private-object overwrite; DOC-D2 P1 clipped/truncated document content; DOC-D3 P1 unsupported control input crashes rendering; DOC-D4 P1 commit-time rollback leaves a private orphan without attempting cleanup. Existing unsupported-script substitution is separately observed behavior, not an automatic Unicode-support feature decision. No production fix has been made yet.


Independent PostgreSQL rerun:32 cases,31 PASS and exactly the commit-time cleanup failure; all payout fixture corrections now pass. Unit rerun independently repeats overwrite, clipping, reference truncation and control-character failure. Four S3 invalid-delete test failures were mock-only: Mockito did not execute the SDK Consumer overload, unlike the real SDK. Correct the mock to call that request-builder default method and verify no concrete DeleteObjectRequest reaches transport. No S3 production defect is claimed. Correct the measured body bound to allow the existing footer's valid48-point position; visual right-edge clipping remains independently demonstrated.

Fixes now authorized by the existing contract: atomically publish completed local bytes with createLink (no replacement); register transaction rollback cleanup after successful storage, preserving existing immediate cleanup outside transactions; retain complete text, wrap by actual font width and paginate before footer; check supported font encoding so controls fall back safely. New files use template version2; issued version1 files remain unchanged. Local filesystem hard-link capability must be checked in deployment; unsupported publication fails rather than weakening immutability.

**DOC-DEC-001 — product decision pending:** The existing Latin-font renderer substitutes unsupported scripts (e.g. Chinese names) with question marks. This is observable behavior, not proof that financial identity presentation is acceptable. Supporting broader scripts/font embedding or approving a restricted identity-rendering policy is not inferred from a test pass. Preserve that boundary while fixing crashes/clipping/truncation. Automatic progression beyond DOC will stop for this unresolved decision under the user's condition.


## Final defect disposition

| ID | Severity | Reproduced defect | Final result |
|---|---|---|---|
| DOC-D1 | P1 | Sequential and simultaneous local writes overwrite an existing immutable reference | Non-replacing atomic publication; sequential retry rejected, one concurrent winner; fixed |
| DOC-D2 | P1 | Wide names/reference text clips and 200-character transfer reference loses its tail | Font-width wrapping, full persisted text and footer-safe pagination; visual and extraction/bounds checks pass; fixed |
| DOC-D3 | P1 | U+0085 in accepted text causes PDF rendering exception | Font-encoding fallback handles unsupported controls safely; fixed |
| DOC-D4 | P1 | Transaction commit failure leaves a private orphan without cleanup attempt | Rollback synchronization deletes new object; real PostgreSQL commit-failure check passes; fixed |

No P0/P1 implementation defect remains reproduced. DOC-DEC-001 is separately a product decision, not silently counted as a successful financial-identity policy. The user was asked to choose preserved Unicode names versus acceptance of the existing substitution; no response is inferred from elapsed time.

## Executed authoritative contracts and coverage

Receipt first issue requires the booking scout/admin, one of the three paid-history states, paid/confirmed timestamps, positive total paid and no late-payment detection. Five other states and four invalid-history variants reject without issuance. Existing files remain authorized and immutable. Tests remove current consent and verified-email state to prove historical access is not a new-mutation gate. The upstream positive original-price invariant stays in effect; zero refund/payout amounts are not reclassified.

Statements require same owner/admin and PAID with paidAt/reference. All four nonpaid states and a legacy missing-reference row reject. Stored ledger money, complete transfer reference and historical REPLACED-destination last4 appear, without encrypted/full bank values or unrelated liability. Owner/venue edits do not rewrite bytes. Existing PAYOUT/DEST tests prove actual destination replacement does not redirect historical routing.

33 new PostgreSQL cases cover actual controllers/transactions, role and object isolation, persisted identity/constraints, missing private object, immediate store/DB failure and late commit rollback. Receipt and statement first-issuance races each repeat three times with barriers and independent requests, producing one stored object/document/audit per pair. No race sleeps are used. Controlled paid-history fixtures isolate document semantics; they do not claim real payment or bank transfer.

11 new unit cases exercise renderer boundaries, private-object immutability/concurrency and mocked S3 private-key/conditional-write contracts, alongside 10 reused document unit cases. Rendered synthetic receipts/statements and UI screenshots were visually reviewed. Wide content and full transfer reference fit; unsupported-script name substitution remains visible in `unicode-receipt.png` for the product decision. New files use template version2, while existing issued files are never rendered again.

## Final validation results

- `mvn test`: PASS — 1011 executed, 0 skipped, 1011 reported; zero failures/errors.
- `mvn verify -Ppostgres-integration`: PASS — 825 executed, 9 skipped, 834 reported; zero failures/errors.
- `mvn clean package`: PASS — 1011 executed, 0 skipped, 1011 reported; zero failures/errors.
- Focused units:21 PASS. Focused PostgreSQL/browser launchers:34 PASS (33 domain cases plus explicitly executed browser launcher).
- Browser:8/8 PASS — actual receipt/statement PDF payloads, safe filename, immutable reload SHA-256, role controls, missing-file error without false download, and delayed response after logout blocked.
- Backend/frontend whitespace checks:PASS. Frontend source/build inputs were unchanged from the prior green production build; no fresh typecheck/lint/build is claimed or required for this backend-only change.

All browser launchers skipped by the normal PostgreSQL profile are opt-in; DOC's was explicitly run and passed separately. Real S3 durability, privacy configuration or payment/email provider behavior is not claimed from mocks.

## WHAT ELHAM SHOULD TEST PERSONALLY — final

Review document terminology, download discoverability, mobile presentation and the rendered unsupported-name example. No manual money, authorization, race, constraint or rollback checks are requested.

## PRODUCT / OPERATIONS DECISIONS — final

DOC-DEC-001 remains unresolved. For an actual unsupported name, the existing font policy loses name characters. Decide whether Eema v1 must preserve original names through Unicode-capable rendering or explicitly accepts the current substitution. No broader font/language policy, transliteration or identity restriction was introduced by assumption. Supporting intentional free bookings remains excluded under the approved PRICE decision.

## STAGING ONLY — final

Verify private S3 conditional-write behavior and object access, deployment filesystem hard-link support/private permissions, backup/restore durability and multi-instance operation. Legal/tax wording approval and retention choices are not software test results. No local result substitutes for these checks.

## Documentation and continuation

The master plan records three PASS scenarios and DOC-004 BLOCKED on the product decision, while clearly recording its passing local technical checks. Historical failures, independent reproduction, harness corrections, current summaries and reviewed screenshots are retained under `evidence/doc/`. The shared PDF is regenerated with scenario IDs/order intact; visual verification and checksum are recorded in `document-check.json`.

DOC is not cleared for automatic progression while DOC-DEC-001 is pending. **NEXT UNTESTED DOMAIN: INC-001–002** (participant incident reporting and admin incident lifecycle/isolation), not started. The current outstanding DOC requirement is the identity-rendering decision; no further domain is silently assumed green.

Final artifact check:636 PDF pages, all703 scenario IDs in unchanged order; pages1 and88–91 visually reviewed without clipping/overlap. Checksum in `evidence/doc/document-check.json`. Temporary browser server/profile and auth fixture cleaned up. Both repository whitespace checks passed after documentation updates.
