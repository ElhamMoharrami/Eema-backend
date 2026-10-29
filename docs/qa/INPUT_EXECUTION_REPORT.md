# INPUT execution report

**Current result: INPUT — 28 PASS /0 FAIL /0 BLOCKED locally. Initial pending notes below are historical; final evidence follows.**

## Initial audit — 2026-09-21

INPUT-001–028 remain NOT RUN until boundary execution and existing-evidence reconciliation finish. All 28 complete scenario declarations and current request DTOs were read. Dependencies: AUTH/POL/TRUST/VEN/READY/MEDIA/AVL/BK/INQ/MATCH/PRICE/PAY/CANCEL/PAYOUT/DEST/INC/SEC and JOURNEY reports, their executed unit/PostgreSQL/browser evidence. External provider behavior is not needed to validate local inputs.

Inventory follows the master plan:001 policies;002 cancellation;003 inquiry creation;004 availability override;005 booking;006 incident;007 payout destination;008 policy draft;009 venue creation;010 recovery email;011 login;012 payout-paid reference;013 match;014 inquiry offer;015 optional note;016 owner verification approval;017 verification submission;018 reason;019 refresh;020 registration;021 reset;022 incident resolution;023 scout contact;024 timezone settings;025 venue update;026 weekly list;027 verify-email;028 weekly item.

Source audit: current DTO constraints and StrictAuthRequest, JacksonConfig, nested validation, controllers and corresponding domain validation. Common V1 contact columns are name100/email255/phone50. Weekly service dereferences each interval. Existing domain suites cover persisted atomicity, role/ownership, service validation and financial/provider side effects; smallest added coverage will address actual missing boundaries rather than repeat every browser/form/role permutation.

Corrected assumptions: cancellation also has previewToken(max2048); six auth DTOs reject unknown fields, ordinary DTOs ignore them. Jackson's existing coercions are not automatically a requirement for a new global strictness architecture. Optional null/empty values remain optional; service contracts may reject structurally valid DTOs (invalid zone, interval, IBAN, state, policy identity). Generic current-policy/verified-email prerequisites do not apply to all endpoints. Nested scout contact and weekly items are covered via parents; they are not standalone HTTP endpoints. Replace imaginary fixture restores with disposable resources.

Audit candidates, not confirmed defects: weekly list permits null elements in Bean Validation while the service dereferences them; optional scout phone has no DTO maximum although PostgreSQL permits50; owner account fullName permits120 while booking snapshot name column permits100. Reproduce through actual HTTP/PostgreSQL before any production change. Preserve names and reject malformed inputs safely; do not truncate, invent phone formatting or change optionality. If expansion is required by the established registered-name contract, use the next common migration and fresh Flyway/Hibernate validation.

Strategy: explicit boundary tables independent of annotation introspection, actual configured Jackson plus Jakarta Validator for deterministic DTO binding/validation; existing real HTTP/PostgreSQL tests for domain semantics and rejected side effects; add focused persisted reproductions/regressions for identified gaps. Reuse existing representative browser error rendering and audit frontend constraints against backend. No new feature or ambiguous product policy is proposed.

## WHAT ELHAM SHOULD TEST PERSONALLY

Field-label/error clarity and whether validation feedback helps correct a form without losing useful input.

## PRODUCT / OPERATIONS DECISIONS

None identified yet. Existing approved decisions remain authoritative.

## STAGING ONLY

Deployed request/edge limits and real provider validation remain separate. PROVIDER unchanged; real screen-reader execution deferred.

## Confirmed defects and authorized fixes

Before changes, real HTTP/PostgreSQL reproduced four failures: null weekly item500 (NullPointerException); scout phone51 and email256 produced SQLState22001 and misleading availability409; registered owner name120 produced the same409 against name100. The independent835-case DTO matrix passed832 and reproduced exactly the three missing validators. Sanitized method outcomes are retained under `evidence/input/history/`.

- INPUT-DEF-001 P2: null weekly item now rejected by nested @NotNull validation before service mutation.
- INPUT-DEF-002 P2: scout email/phone overflow now rejected by @Size255/@Size50, matching existing database limits, preserving optional phone and no formatting rules.
- INPUT-DEF-003 P1: supported account name cannot be copied into a booking. V15 widens both snapshot name columns to255, accommodating registered names120 and the existing supported email fallback; no truncation, data rewrite, new name policy or changed direct scout-contact name100 limit. Inquiry-derived scout snapshots use the same columns and receive explicit coverage.

Migration audit: no PostgreSQL Docker container was running when existing local instances were inspected; no shared/production history was accessed or claimed inspected. Existing schema limits prove widening is lossless for all existing snapshot values. A dedicated upgrade test applies V1–14, audits eight populated synthetic historical rows spanning every BookingStatus at the old100-character limit, applies V15, compares complete row JSON byte-for-byte, checks both new limits and migration replay. Fresh Spring PostgreSQL integration will also apply all migrations and Hibernate validation. Applied migrations remain untouched.

Regression audit found the packaged recovery harness explicitly expects V1–V14. Updated only its expected common/dev migration inventory to include V15. The currently running full suite compiled the old expectation and is historical until the corrected recovery check and final full gates are executed; do not count a failed command as PASS. INPUT focused checks are green; final disposition awaits regression reconciliation.

The first full PostgreSQL run reported1046 cases:1031 passed,13 opt-in browser skips and2 failures, both stale migration-harness expectations. Besides packaged inventory14→15, the existing outbox-specific V13 upgrade test expected exactly one migration while targeting latest. It now explicitly targets V14, the migration that test is designed to verify; the new V14→V15 history test and full fresh schema cover V15. No production failure was observed in that run. Final complete regression is rerunning these corrected harnesses.

## Final executed result

**INPUT-001–028:28 PASS /0 FAIL /0 BLOCKED. INPUT-DEF-001/002/003 resolved; no unresolved P0/P1 or decision.**

All28 DTOs are represented in the independent835-case RequestBoundaryContractTest table. Fields cover null/omitted/blank, lengths and collection counts, numeric limits/overflow, nested elements, enum values/unknown strings, UUID/date/time binding, incompatible JSON shapes and strict-auth versus ordinary unknown fields. Together with AuthInputContractTest, focused837 cases pass. Structurally valid timezone/IBAN/date/state/media references still require authoritative service validation.

Seven focused PostgreSQL cases pass: six HTTP boundaries and one populated V14→V15 upgrade. Invalid phone/email leaves no booking/outbox/provider effect; null weekly item preserves calendar. Valid phone50 persists; supported120-character owner/scout names survive normal/inquiry-derived snapshots; historical254-character email fallback survives. Eight all-status historical rows preserve complete JSON; both snapshot columns become255; repeat migration applies nothing. Full regression verifies fresh common migrations, Hibernate validation, packaged migration inventory and the separately targeted V13→V14 outbox upgrade.

Current AUTH/POL/TRUST/VEN/READY/MEDIA/AVL/BK/INQ/MATCH/PRICE/PAY/CANCEL/PAYOUT/DEST/INC/SEC/JOURNEY suites supply service normalization, role/ownership, state/policy gates, numeric representability, atomicity and rejected side effects. No new global coercion/phone/pricing policy was introduced. Existing form/error handling is reused; the later security upgrade reruns actual browser feedback. No browser-per-DTO or real-provider claim.

Historical3 DTO failures,4 HTTP reproductions and2 stale migration-harness failures remain preserved under `evidence/input/history`. V15 is the only common migration introduced in this continuation; no applied migration was edited. No shared/production history was accessed. Widening is lossless by schema and populated-history evidence.

**Locally complete; next API was executed under the same final gates.**

### Final complete regression

- `mvn test`: PASS — 1981 passed, 0 opt-in browser skips; zero failures/errors (1981 reported).
- `mvn verify -Ppostgres-integration`: PASS — 1071 passed, 13 opt-in browser skips; zero failures/errors (1084 reported).
- `mvn clean package`: PASS — 1981 passed, 0 opt-in browser skips; zero failures/errors (1981 reported).
- Frontend typecheck, ESLint and production build: PASS on Next16.3.5/React19.3.0. ESLint reports19 warnings and zero errors; two newly introduced compiler diagnostics remain warnings, with the reason recorded in the dependency-security report. Full/production npm audits: zero vulnerabilities.
- Backend/frontend whitespace checks pass. Sanitized per-method outcomes and log hashes are in `evidence/final`; security-upgrade browser/client evidence is in `evidence/security-dependencies`. No raw logs with synthetic credentials are published.
- Default PostgreSQL profile skips13 opt-in browser cases. Those skips are not counted as passes; independently executed browser evidence is cited by the relevant domain and dependency-security reports. Local Chromium and mocked transport never count as real-provider verification.
