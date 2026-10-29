# DEST execution report

**Completed 2026-09-19: DEST-001–004 — 4 LOCAL PASS, 0 FAIL, 0 BLOCKED.** Four reproduced defects fixed (one P0 privacy defect, three P1 concurrency/sensitive-state defects); no unresolved local DEST defect. First unfinished domain after the approved PAYOUT policy update: DEST-001–004. All four master-plan scenarios were NOT RUN and no prior DEST report existed. Full master plan and existing execution reports were read personally; source/test existence is not PASS evidence.

## Inventory and dependencies

| Scenario | Current result | Contract / missing evidence |
|---|---|---|
| DEST-001 | PASS | Owner-only submission, verification/email/policy prerequisites, normalization and request boundaries, duplicate concurrency |
| DEST-002 | PASS | Real encrypted persistence, masked owner/admin responses, isolation, crypto randomness/tamper failure |
| DEST-003 | PASS | Pending/verified/replaced/rejected transitions, concurrent review and one verified destination, historical routing |
| DEST-004 | PASS | Admin-only POST reveal, no-store headers, audit without bank value, ephemeral frontend state across hide/review/navigation/session changes |

Dependencies: AUTH (role/session/object isolation), POL (current submission policy acceptance), TRUST (owner operational verification and audit), PAYOUT (READY routing snapshots and atomic replacement flush). PAYOUT's destination check proves only its routing dependency, not these four scenarios. Existing IbanPolicyTest and PayoutDestinationCryptoTest provide four deterministic unit tests; TrustWorkflowSecuritySurfaceTest is not complete destination endpoint authorization coverage.

## Source audit before production changes

Reviewed OwnerPayoutDestinationService, Owner/AdminPayoutDestinationController, request/response DTOs, entity/repository, PayoutDestinationCrypto, IbanPolicy, V10 migration, relevant security/error handling, owner submission and admin review frontend pages, useAdminPage, and existing payout/verification fixtures and browser harness.

Authoritative rules: owner lists are scoped from current identity. Only submission additionally requires current policies, verified email and VERIFIED owner profile. Client owner/status/currency/type fields are not authoritative. Name is nonblank/max160; raw IBAN is nonblank/max80, then whitespace removed/case normalized and current supported-country length/mod97 checked. The current country allowlist is the software contract; this is not blanket approval of every SEPA country. One owner/fingerprint is unique across all historical statuses, including rejected; another owner may submit the same fixture independently. Type/currency are IBAN/EUR.

Persisted IBAN is versioned AES-256-GCM with random nonce, keyed HMAC fingerprint and last4. Normal DTOs expose masked last4, not encrypted values/fingerprint/full IBAN. Admin reveal is a deliberate POST for pending/verified/replaced records; rejected conflicts; successful reveal is audited with no bank value and no-store/no-cache response headers.

Submit creates PENDING_VERIFICATION. Verify changes pending to VERIFIED, atomically replacing any current VERIFIED destination; pending/rejected replacements leave the old verified destination active. Reject requires a reason and changes only pending to REJECTED. Repeated/stale review conflicts. Existing PAYOUT flush ordering releases the partial unique-index slot before replacement. Manual verification is an operational review, not electronic bank-ownership verification. Notifications are after-commit lifecycle events; real email remains staging-only.

Audit hypotheses to reproduce: same-owner simultaneous submissions may race the fingerprint check and return 500; simultaneous verification of different pending records may race the one-verified index; frontend reveal state is not explicitly bound to destination ID/request generation and delayed responses may restore sensitive information after review or navigation. These are hypotheses, not yet classified defects.

## Test strategy and boundaries

Reuse deterministic normalization/crypto cases, expand relevant malformed/key boundaries only where needed. Use disposable real PostgreSQL for exact persisted state, constraints, role/gate denial, audit, transaction rollback and simultaneous submission/review. Use barriers/latches and independent connections, never sleeps for races. Reuse PAYOUT's immutable routing evidence rather than duplicate its financial suite. Use the existing Chrome/CDP browser harness for representative owner submission, admin deliberate reveal/hide/review, reload, stale/conflict behavior and session/ID races. Provider clients remain mocked; no bank movement or external verification is performed.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review whether manual-verification wording, replacement continuity, rejection guidance, deliberate reveal warning and mobile form/review layout are understandable. No manual arithmetic, races, crypto or authorization retesting is requested.

## PRODUCT / OPERATIONS DECISIONS

No new feature or policy is authorized. Current operational verification and historical duplicate rules are tested as implemented. Real account-ownership review and key custody/rotation remain operational responsibilities; do not invent electronic bank verification, historical restoration, bank transfer, account editing or new KYC flows. Record any newly discovered ambiguity before deciding continuation.

## STAGING ONLY

Production key provisioning/custody, actual bank ownership review, real lifecycle email, deployed multi-instance operation and edge/cache behavior. Local encryption/transaction/browser tests do not verify those external operations.

## Initial execution / defect triage

Initial PostgreSQL run: 27 cases, 19 pass and 8 assertions fail. Six failures independently reproduce three duplicate-submission races and three different-destination verification races: outcomes include HTTP 500 from unique constraints instead of the intended conflict/serialized transitions. Classify these as DEST-D1 and DEST-D2, P1 workflow/concurrency defects; uniqueness still protects persisted routing. Two failures are harness expectations, not product defects: current policy denial is 403 (not 409), and the shared alternate-owner fixture may already have its own legitimate history, so isolation must assert exclusion of the tested owner's record rather than an empty alternate-owner list. These fixture expectations were corrected before the independent rerun. Historical baseline totals are retained.

Independent PostgreSQL rerun: 27 cases, exactly six race failures (the same two product defects repeated three times); both harness expectations now pass. Fix DEST-D1/D2 by locking the owner row before destination submission/review. An absent current destination cannot provide a lock, so the owner is the stable serialization key. Review resolves owner ID without loading a stale entity, then locks owner before destination. Preserve existing unique constraints and atomic replacement flush. No schema change.

Initial browser evidence confirms cross-owner stale history (DEST-D3, P0 privacy) and admin account-switch retention of a revealed value (DEST-D4, P1 sensitive-state handling). The independent browser rerun subsequently confirmed both failures before frontend fixes. The initial browser teardown error is a harness issue and does not erase the recorded privacy assertions.

Two independent browser runs reproduced DEST-D3 and DEST-D4. Fix by keying the owner destination component to current user ID and the admin detail component to current user ID plus destination ID. On a context change React replaces all local state and pending old responses cannot populate the new component. Existing API actor checks remain authoritative for delayed responses. This also clears entered bank details, forms, prior errors and revealed values; no persistent sensitive storage is added. Correct the owner form's implementation-specific “Spring” copy to ordinary user wording.

The original delayed-reveal harness incorrectly attempted Cancel while that button is intentionally disabled during an in-flight reveal. This is not a product defect or a reason to add a cancellation feature. Replace that check with real account-switch/logout transitions and release a deliberately held response; synchronize on delivery and browser frames, not race sleeps. The first teardown ENOTEMPTY error was also harness-only. Both historical browser runs retain their independently observed privacy failures.

## Continuation assessment

NEXT UNTESTED DOMAIN: DOC-001–004 (receipt eligibility/content, paid-owner statements, private storage and rendering). Its receipt contract explicitly requires positive totalPaidEur, paidAt and confirmedAt. PRICE-DEF-005 remains an inherited **P1 unresolved accounting risk**: valid pricing can round to EUR 0.00 and reach mocked checkout; the choice between a minimum charge and an intentional free-booking flow has not been approved. The user's Eema v1 fee-allocation decision resolved PAYOUT excess costs, not this separate decision.

DEST's technical validation can finish because destination submission/review does not depend on booking amount. Automatic progression into financial receipt eligibility stops under the user's “no unresolved P0/P1 or product/business decision” condition. This is an inherited continuation blocker, not a new DEST defect or a claim that DOC tests failed. DOC remains NOT RUN; no free-booking flow, minimum charge or document policy is implemented by assumption.


The first full regression was interrupted after a separate real-service probe exposed PAYOUT-V1-D1 in the recently approved generic positive-OWNER paid-history path (see PAYOUT report). This is a financial dependency correction, not a new domain or feature. Initial 994-unit and 12-browser passes are historical evidence; a fresh complete backend regression is required after the correction. The test matrix must retain the fee already deducted at transfer while preserving true full-refund owner recovery.


## Final defect disposition and authoritative behavior

| Defect | Severity | Reproduced behavior | Fix / result |
|---|---|---|---|
| DEST-D1 | P1 | Concurrent duplicate submission returns 500 after fingerprint race | Owner-row serialization; one pending record/submission audit, loser 409; PASS |
| DEST-D2 | P1 | Different pending verifications race the unique verified index | Owner-before-destination lock order; serialized valid transitions, one VERIFIED; PASS |
| DEST-D3 | P0 | Owner account switch retains another owner's private destination history | Component state bound to user identity; new scoped data fetched; PASS |
| DEST-D4 | P1 | Admin account switch retains an already-revealed bank value without fresh reveal | Component state bound to user and destination identity; PASS |

The existing state machine, encrypted storage, country allowlist and historical duplicate rule are retained. Client owner/status/type/currency cannot override server values. A pending replacement never redirects a historical payout; existing PAYOUT regression remains green. No schema change, automated transfer, bank-verification provider, restoration/editing workflow or new financial calculation was added.

## Executed coverage and command results

Added 35 real PostgreSQL cases (initial 27, then review/request/constraint/audit boundaries), five crypto cases beyond the four existing IBAN/crypto tests, and an opt-in browser launcher with 12 checks. Independent-transaction races use barriers; each submission and distinct-verification race repeats three times. Persisted state, audit, rollback, masking and notification event boundaries are asserted. The full run includes the additional POST-only/audit-failure reveal case, all 33 provider-allocation PostgreSQL cases and all earlier financial regression. PAYOUT-V1-D1 was independently reproduced and corrected during dependency review; its three new unit and three PostgreSQL histories prevent a second charge for fees already deducted at transfer while retaining true full-refund recovery.

- `mvn test`: PASS — 997 executed, 0 skipped, 997 reported; zero failures/errors.
- `mvn verify -Ppostgres-integration`: PASS — 784 executed, 8 skipped, 792 reported; zero failures/errors.
- `mvn clean package`: PASS — 997 executed, 0 skipped, 997 reported; zero failures/errors.
- Focused IBAN/crypto: 9 PASS. Focused PostgreSQL after fixes: 34 PASS; all 35 execute in the full run.
- Frontend typecheck, lint, production build and whitespace check: PASS. Backend whitespace check: PASS.
- Browser: 12/12 PASS against the rebuilt production frontend and disposable PostgreSQL. Three masked-state screenshots visually reviewed. Full revealed values were not captured or persisted in evidence.

Only opt-in browser launchers are skipped by the normal full PostgreSQL profile; DEST's launcher ran explicitly and passed separately. Real-provider behavior is not inferred from these tests.

## Documentation, evidence and local clearance

This report and sanitized `docs/qa/evidence/dest/` preserve initial failures, independent reproduction, harness corrections, final command totals and browser checks/screenshots. The final backend-only historical fee correction does not change DEST's DTOs or browser interactions; the successful 12-check rebuilt-frontend run remains applicable, and fresh PostgreSQL regression covers the corrected financial dependency. The master-plan DEST scenarios now state the corrected contract and local PASS results. PDF regeneration/integrity/visual verification is recorded separately in `document-check.json`.

DEST is locally complete with no unresolved P0/P1 defect. Automatic progression stops at the inherited PRICE-DEF-005 business/accounting gate described above. NEXT UNTESTED DOMAIN: DOC-001–004. No DOC scenario has been marked PASS or modified by assumption.

Final shared-document verification: regenerated 637-page PDF; all 703 scenario IDs/order preserved. Visually reviewed pages 1 and 83–90, including the corrected policy/history, DEST scenarios and adjacent DOC transition, with no clipping or overlap. SHA-256 is recorded in `evidence/dest/document-check.json`. Temporary DEST frontend and Chrome profiles were stopped/removed. Backend and frontend final `git diff --check` passed.

**Subsequent PRICE resolution (2026-09-19):** The zero-price continuation decision referenced above is now resolved under the user-approved Eema v1 policy. Final totals below EUR 0.01 are rejected before booking persistence/payment creation, with defensive stored-price checkout validation and V13 enforcement. See the PRICE execution report for fresh regression evidence; the prior stop remains historical.
