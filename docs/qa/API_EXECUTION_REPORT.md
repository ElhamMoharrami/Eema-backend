# API execution report

**Current result: API — 90 PASS /0 FAIL /0 BLOCKED locally. Initial pending notes below are historical; final evidence follows.**

## Initial audit — 2026-09-21

API-001–090 are the next locally executable inventory. INPUT focused boundary/migration checks are green; shared full regression is still running and includes a known stale V14 recovery-harness expectation, now corrected in source. Final domain results are not inferred from source existence or the in-progress run. User explicitly permits continuation without requiring the preceding domain to be completely green; implementation is handled sequentially and final gates will cover the combined state.

Read all90 endpoint scenarios, complete role table, controller/security mappings and relevant previously executed domain tests/reports. Dependencies: AUTH, POL, TRUST, VEN/READY/MEDIA/AVL, MATCH, BK/INQ, PRICE/PAY/CANCEL/REF/PAYOUT/DEST/DOC, INC, ADMIN, SEC, INPUT and JOURNEY. Existing suites provide actual HTTP, persisted-state, ownership, current-state, atomicity, provider-denial and malformed-input evidence. Framework health/metrics/Swagger remain SEC scope; Next Cala remains CALA scope.

Corrected generated assumptions: auth POST routes are public and operate on supplied credentials rather than the signed-in actor. Policy reads require authentication but not already accepted policies. Incident POST201/GET200 requires participant/admin access without current policy or verified email; frontend exemption is already fixed in INC. Booking/inquiry/payout list endpoints return current-user-scoped collections, not an invented other-owner selector or403 for another user's collection. Public venue GET matching yields403 for anonymous protected subresources; other protected anonymous routes401. Webhook is public and validates fetched provider facts; provider retrieval/integrity failure can deliberately500 for retry and must not count as success. Unknown ordinary DTO fields and Jackson coercions retain INPUT's established contract.

Strategy: map each scenario to actually executed method evidence; supplement the admin collection/detail/mutation surface with an explicit HTTP filter-chain denial matrix for all35 admin routes, asserting unchanged domain/audit/outbox counts and no Mollie call. Reuse existing successful lifecycle/ownership/error matrices rather than invent90 duplicate browser journeys. Check registered controller inventory against the explicit90-route contract. No production behavior change is planned; real providers remain blocked externally, not required here.

## WHAT ELHAM SHOULD TEST PERSONALLY

Clarity of access-denied, expired-session and stale-action feedback; useful navigation from errors. No manual role matrix or SQL work requested.

## PRODUCT / OPERATIONS DECISIONS

None newly identified. Existing approved domain rules remain authoritative.

## STAGING ONLY

Deployed proxy/CORS/authentication, provider callback transport and edge controls. Real screen-reader execution remains deferred; PROVIDER report preserved.

Focused supplement:36 PostgreSQL cases PASS, including105 actual admin-denial HTTP requests with unchanged row counts/no Mollie call and exact90-route registration equality. Executed from /tmp/eema-api-check with copied source verified equal; isolated target avoided interference with the ongoing full regression. No production/frontend/schema change. Final90-scenario disposition awaits mapped full-suite evidence and complete regression; source existence alone is not credited.

## Final executed result

**API-001–090:90 PASS /0 FAIL /0 BLOCKED. No new production defect, migration or decision.**

Every endpoint maps to completed non-skipped test methods in `evidence/api/inventory.json`, matched against the final unit/PostgreSQL XML-derived outcomes. Existing domain HTTP tests supply success, object ownership, input/current-state conflicts, persisted side effects, privacy and provider guards. New ApiSurfaceIntegrationTest contributes36 passing cases: exact90 registered method/path equality and35 admin-route denial cases, each exercising anonymous401/scout403/owner403 (105 requests), unchanged11-table state counts and no Mollie call. Dynamic placeholders use synthetic IDs; no fabricated provider evidence.

Actor/error corrections from the initial audit are retained in the master plan. Scoped lists remain scoped200 responses, incidentPOST201/read200, public credential routes and webhook are not forced into generic actor assumptions. Webhook provider/integrity failures that require retry are not relabeled successful validation responses. Framework operational endpoints remain SEC scope, and Cala remains its separate Next contract.

No extra browser-per-endpoint matrix is needed: existing authoritative API/domain suites plus current actual frontend route/error checks prove the relevant layers. Fresh PG validates all current schema/transaction/concurrency dependencies.

**Locally complete; next STATE was executed under the same final gates.**

### Final complete regression

- `mvn test`: PASS — 1981 passed, 0 opt-in browser skips; zero failures/errors (1981 reported).
- `mvn verify -Ppostgres-integration`: PASS — 1071 passed, 13 opt-in browser skips; zero failures/errors (1084 reported).
- `mvn clean package`: PASS — 1981 passed, 0 opt-in browser skips; zero failures/errors (1981 reported).
- Frontend typecheck, ESLint and production build: PASS on Next16.3.5/React19.3.0. ESLint reports19 warnings and zero errors; two newly introduced compiler diagnostics remain warnings, with the reason recorded in the dependency-security report. Full/production npm audits: zero vulnerabilities.
- Backend/frontend whitespace checks pass. Sanitized per-method outcomes and log hashes are in `evidence/final`; security-upgrade browser/client evidence is in `evidence/security-dependencies`. No raw logs with synthetic credentials are published.
- Default PostgreSQL profile skips13 opt-in browser cases. Those skips are not counted as passes; independently executed browser evidence is cited by the relevant domain and dependency-security reports. Local Chromium and mocked transport never count as real-provider verification.
