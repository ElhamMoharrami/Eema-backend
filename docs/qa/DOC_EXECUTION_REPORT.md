# DOC execution report

**DOC-001–004: 4 LOCAL PASS, 0 FAIL, 0 BLOCKED.** DOC-DEC-001 is resolved by the explicit approved v1 policy and verified Unicode implementation. Earlier blocked results and substitutions remain historical evidence below and in `evidence/doc/unicode/history/`. No reproduced P0/P1 defect or unresolved product decision remains in DOC.

## Inventory

| Scenario | Final status | Scope |
|---|---|---|
| DOC-001 | PASS | Receipt eligibility and ownership |
| DOC-002 | PASS | Receipt contents and immutable issuance |
| DOC-003 | PASS | Payout statement scope and historical ledger |
| DOC-004 | PASS | Private storage failures, Unicode and layout |

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

**DOC-DEC-001 — historical pending decision (resolved below):** The existing Latin-font renderer substitutes unsupported scripts (e.g. Chinese names) with question marks. This is observable behavior, not proof that financial identity presentation is acceptable. Supporting broader scripts/font embedding or approving a restricted identity-rendering policy is not inferred from a test pass. Preserve that boundary while fixing crashes/clipping/truncation. Automatic progression beyond DOC will stop for this unresolved decision under the user's condition.


## Historical initial execution: Final defect disposition

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

## Historical initial execution: Final validation results

- `mvn test`: PASS — 1011 executed, 0 skipped, 1011 reported; zero failures/errors.
- `mvn verify -Ppostgres-integration`: PASS — 825 executed, 9 skipped, 834 reported; zero failures/errors.
- `mvn clean package`: PASS — 1011 executed, 0 skipped, 1011 reported; zero failures/errors.
- Focused units:21 PASS. Focused PostgreSQL/browser launchers:34 PASS (33 domain cases plus explicitly executed browser launcher).
- Browser:8/8 PASS — actual receipt/statement PDF payloads, safe filename, immutable reload SHA-256, role controls, missing-file error without false download, and delayed response after logout blocked.
- Backend/frontend whitespace checks:PASS. Frontend source/build inputs were unchanged from the prior green production build; no fresh typecheck/lint/build is claimed or required for this backend-only change.

All browser launchers skipped by the normal PostgreSQL profile are opt-in; DOC's was explicitly run and passed separately. Real S3 durability, privacy configuration or payment/email provider behavior is not claimed from mocks.

## Historical initial execution: WHAT ELHAM SHOULD TEST PERSONALLY — final

Review document terminology, download discoverability, mobile presentation and the rendered unsupported-name example. No manual money, authorization, race, constraint or rollback checks are requested.

## Historical initial execution: PRODUCT / OPERATIONS DECISIONS — final

DOC-DEC-001 remains unresolved. For an actual unsupported name, the existing font policy loses name characters. Decide whether Eema v1 must preserve original names through Unicode-capable rendering or explicitly accepts the current substitution. No broader font/language policy, transliteration or identity restriction was introduced by assumption. Supporting intentional free bookings remains excluded under the approved PRICE decision.

## Historical initial execution: STAGING ONLY — final

Verify private S3 conditional-write behavior and object access, deployment filesystem hard-link support/private permissions, backup/restore durability and multi-instance operation. Legal/tax wording approval and retention choices are not software test results. No local result substitutes for these checks.

## Historical initial execution: Documentation and continuation

The master plan records three PASS scenarios and DOC-004 BLOCKED on the product decision, while clearly recording its passing local technical checks. Historical failures, independent reproduction, harness corrections, current summaries and reviewed screenshots are retained under `evidence/doc/`. The shared PDF is regenerated with scenario IDs/order intact; visual verification and checksum are recorded in `document-check.json`.

DOC is not cleared for automatic progression while DOC-DEC-001 is pending. **NEXT UNTESTED DOMAIN: INC-001–002** (participant incident reporting and admin incident lifecycle/isolation), not started. The current outstanding DOC requirement is the identity-rendering decision; no further domain is silently assumed green.

Final artifact check:636 PDF pages, all703 scenario IDs in unchanged order; pages1 and88–91 visually reviewed without clipping/overlap. Checksum in `evidence/doc/document-check.json`. Temporary browser server/profile and auth fixture cleaned up. Both repository whitespace checks passed after documentation updates.

## Approved Unicode policy resolution — completed

The user rejected silent substitution and approved controlled embedded Unicode fonts for existing receipt/statement plain-text fields. DOC-DEC-001 was decided before implementation; the completed implementation and evidence are recorded below. Preserve authoritative stored names, fail issuance for unsupported glyphs, and leave existing PDFs immutable. No localization, transliteration, tax or general RTL template redesign.

Pre-change audit: PDFBox3.0.8, Standard14 Helvetica, explicit32..255 substitution. Width wrapping/rollback/private immutability already hardened. Fonts must be controlled classpath resources, loaded per document with no shared PDFont. PDFBox3.0.8 lacks full Arabic GSUB/GPOS support; audit a released supported shaping route before claiming Arabic correctness. Existing10 document units, boundary/storage tests,33 PostgreSQL cases and8 browser checks supply regression. Add requested script/extraction/visual, unsupported-failure, immutable Unicode receipts/statements and packaged/Docker checks. Source frontend unchanged; providers remain mocked.

Implementation audit: embedded Noto Sans Regular/Bold, Noto Naskh Arabic Regular and a renamed static weight400 Noto Sans SC derivative; four controlled fonts total11,919,652 bytes, OFL1.1 notices retained. PDFBox3.0.8 remains unchanged. Its documented AWT/FOP layout modules require unreleased3.0.9 (Maven Central metadata checked); ICU4J78.3 supplies standard shaping/bidi without hand-mapped encoding. This bounded route rejects combining marks and format/control characters because it cannot guarantee GPOS/join-control positioning. It never normalizes stored strings. New template3 applies only to newly issued documents. Original logical strings are retained in PDF ActualText as well as tested extraction where supported.

Setup corrections: initial compilation referenced a nonexistent COSName.SPAN constant (replaced with the standard Span name); initial startup check incorrectly demanded Latin glyphs from the Arabic-only font (changed to an Arabic glyph). The first direct Maven dependency download stalled; the same pinned Central artifact was fetched into the local cache. These are implementation/harness corrections, not additional product defects.

Focused glyph lookup exposed a PDFBox API distinction: PDType0Font.hasGlyph takes PDF character codes, not Unicode code points. Replaced it with FontBox's standard Unicode cmap lookup; no manual mappings introduced. This preserves deterministic fallback and allows unsupported text to reach the controlled internal-error path.

Full-regression harness finding: OwnerTrustIntegrationTest's repository spy reported UnfinishedStubbingException during race setup. Independently reproduced with a deliberately interleaved asynchronous ordinary read consuming Mockito's pending shared stubbing. Replaced only that test's repository spy with a method interceptor scoped to the intended lock methods/IDs and a volatile immutable barrier configuration. Production owner-review logic is unchanged. Preserve first-run failure evidence, rerun both affected repeated race methods and the complete PostgreSQL suite before final acceptance.


## Unicode resolution — final authoritative result

**DOC-004 PASS. DOC-DEC-001 RESOLVED.** Financial documents preserve supported original Unicode names through controlled embedded font rendering; unsupported glyphs fail safely rather than being silently replaced. Receipt/statement eligibility, authorization, saved money and routing remain unchanged. Historical stored files are returned as-is, even if present-day names would now fail rendering. New files use template3. Existing records/files are not migrated, regenerated or overwritten.

FontBox Unicode cmap chooses a deterministic fallback from Noto Sans Regular/Bold, Eema Document CJK (static Noto Sans SC weight400 derivative) and Noto Naskh Arabic Regular. PDType0Font embeds/subsets the chosen fonts. Bundled immutable bytes are cached; fonts are parsed once per used font per document, never per text fragment or shared between documents. Startup validates the required resources. The bounded font bundle is11,919,652 bytes. No host font lookup, user-controlled font path, remote font fetch, font endpoint, image text or hand-written encoding exists. OFL1.1 permits embedding/redistribution with retained notices; derivative renamed, copyright/license retained. ICU4J78.3 Unicode3 license is included. Exact pinned upstream URLs, hashes and derivation are in `src/main/resources/fonts/README.md` and `manifest.json`.

The installed PDFBox3.0.8 has partial GSUB and no GPOS; documented new layout modules were not released in Maven Central. ICU's standard presentation shaping and bidi algorithm provide the bounded Arabic route. Rendering محمد was visually inspected as joined, correctly ordered Arabic; extraction returns the original. No claim of universal shaping: combining-mark sequences (including decomposed Latin and vocalized Arabic), variation selectors, join/bidi controls and unconfigured glyphs fail with a controlled internal error. This is the user-authorized safe failure for unsupported rendering; no normalization, transliteration or substitution is used. Supporting those sequences later requires a proven shaping engine rather than silently accepting an incorrect result.

References: [PDFBox complex-script limitations](https://pdfbox.apache.org/3.0/faq.html), [ICU bidi and shaping](https://unicode-org.github.io/icu/userguide/transforms/bidi.html), [Noto font license](https://github.com/notofonts/noto-fonts/blob/ffebf8c1ee449e544955a7e813c54f9b73848eac/LICENSE), [CJK font license](https://github.com/notofonts/noto-cjk/blob/f8d157532fbfaeda587e826d4cd5b21a49186f7c/Sans/LICENSE).

### Tests and evidence

18 new unit cases cover ASCII, all nine required name examples, long mixed Unicode, literal markup/internal spaces and six unsupported glyph/sequence failures. Earlier clipping, complete-reference, private-object immutability, S3 conditional-write and service authorization/issuance tests remain green. The prior successful substitution characterization is explicitly superseded by safe rejection; its original source and `??` image are retained.

14 added PostgreSQL cases prove nine receipt names and three statement names with exact money, masked destination, no internal fields in receipts, private role isolation and immutable repeat downloads after names change to unsupported text. Two renderer-failure cases prove no document/object/audit/notification/provider effects and successful retry after a supported correction. The existing six repeated barrier races and storage/immediate DB/commit-failure rollback cases pass. Fonts are not available as public resources. Eight actual browser checks pass against the new renderer without frontend changes. The first full PostgreSQL attempt exposed an independently reproduced Mockito setup race in the prior OwnerTrust test; a test-only method interceptor replaces the shared repository spy barrier. Both affected repeated methods and the complete suite were rerun successfully. See `history/first-full-postgres.json` and `harness-focused.json`; the failed attempt is not counted as PASS.

- focused-unit: 39 executed, 0 skipped; zero failures/errors.
- focused-postgres: 48 executed, 0 skipped; zero failures/errors.
- harness-focused: 6 executed, 0 skipped; zero failures/errors.
- unit: 1029 executed, 0 skipped; zero failures/errors.
- postgres: 839 executed, 9 skipped; zero failures/errors.
- package: 1029 executed, 0 skipped; zero failures/errors.

Commands: focused document tests; `mvn test`; `mvn verify -Ppostgres-integration`; `mvn clean package`; `git diff --check`. The normal profile's nine opt-in browser launchers skip by design; DOC's launcher ran separately. The packaged JAR probe rendered/extracted all nine names, checked embedded fonts and exact money, and proved clear missing/corrupt-resource startup failures in disposable copies. Production Docker runtime testing rendered/extracted the same nine names with networking disabled; build/runtime details are in `evidence/doc/unicode/packaging.json`. No host-font dependence is claimed from source inspection alone.

Frontend source unchanged; existing production build used for browser checks. No new npm typecheck/lint/build is claimed. Both repository whitespace checks pass. Real S3/payment/email providers were not exercised.

### WHAT ELHAM SHOULD TEST PERSONALLY

Review document readability, name sizing and download wording/discoverability on mobile. Name integrity, authorization, money, rollback, races and packaging have automated evidence.

### PRODUCT / OPERATIONS DECISIONS

No unresolved DOC policy decision remains. The approved policy is implemented for the tested supported scripts, with explicit safe failure outside the rendering boundary. No localization, tax-invoice semantics, transliteration, language selection or general RTL template was added.

### STAGING ONLY

Real private S3 access/conditional writes/durability, deployed filesystem permissions/hard-link support, backup/restore and multi-instance behavior. Legal/tax wording and retention approval remain distinct from software correctness.

### Continuation

DOC is locally complete. **NEXT UNTESTED DOMAIN: INC-001–002**, participant incident reporting and admin incident lifecycle/isolation. The previous DOC stop was resolved, not silently waived. Master plan and PDF updated with all scenario IDs/order preserved; final visual verification/checksum in `evidence/doc/unicode/document-check.json`.

Final artifact verification:636 PDF pages; all703 scenario IDs extracted in unchanged order. Updated DOC pages1/88–91 and gap-table pages627–629 were visually reviewed without clipping/overlap. G17 records resolution; the stale G18 document-cache sentence now agrees with the verified no-store headers. Checksum and review in `evidence/doc/unicode/document-check.json`. Both repository whitespace checks passed.
