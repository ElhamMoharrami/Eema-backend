# CALA execution report

**Current result: CALA-001 — 1 PASS / 0 FAIL / 0 BLOCKED locally. All four CALA defects resolved; no unresolved P0/P1 or decision.**

## Historical initial audit

CALA-001 — execution in progress; no PASS claim. PROVIDER-001–005 remain BLOCKED — STAGING/EXTERNAL; their report is preserved. The user's latest autonomy instruction permits independent local work past those blockers.

## Inventory, source audit and corrected assumptions

The Next POST `/api/verify-booker` route imports `lib/cala.ts`; the compatibility `BookerVerification` component has no production importer (EDGE-006). This endpoint does not call Spring, persist verification state, or authorize booking/payment/owner trust. It has no session check, so a generic Scout/Owner denial matrix is incorrect. Missing/invalid input convention is HTTP200 with verified=false. Existing search behavior treats the first Company entity with an ID as verified, optionally enriches it, and returns raw search/detail data. Source alone does not establish whether that exposure and meaning are appropriate for v1.

Dependencies: EDGE, AUTH, SEC, TRUST and UXACT reports. No dedicated CALA execution evidence existed. No database/migration involvement; no real-provider credentials or calls are needed for local contract tests.

## Strategy and missing coverage

Execute the actual TypeScript modules and actual NextResponse with controlled fetch/environment/timers. Cover malformed JSON, {}, blank/missing key, quoted keys, search success/no match, detail failure, 500, timeout, JSON shape errors, public reachability, raw output and log contents. Independently reproduce material findings through a running Next route where feasible. Separate technical failures from any unresolved decision about the optional public integration; do not connect it to trust/payment or invent identity assurance.

## Executed contract and findings

The existing public first-Company search contract is informational discovery only; it does not verify that a caller represents a company. No new trust/checkout feature or authentication requirement was introduced. The compatibility component remains unreferenced. Returning arbitrary provider properties and logging company/provider bodies are unnecessary for the existing summary contract and conflict with the established SEC data-minimization convention.

- CALA-DEF-001 (P2): JSON `null` and a non-string company with an unusable `toString` produced HTTP500. Independently reproduced through the pre-fix production HTTP route. Fixed by validating the JSON object/string shape, returning HTTP200 verified=false with zero transport calls.
- CALA-DEF-002 (P1): arbitrary search/detail fields escaped in `raw`. A synthetic marker independently reproduced the exposure over HTTP. Removed raw output; retained the defined company summary. No real customer exposure is asserted.
- CALA-DEF-003 (P1): provider error bodies and submitted company names reached diagnostic logs. Deterministic regression reproduced this before the fix; static diagnostic events now omit them.
- CALA-DEF-004 (P2): a quoted whitespace-only API key was treated as configured. Normalization now trims after quote removal; zero provider calls.

`scripts/qa/cala-contract-check.cjs` executes actual TypeScript modules with NextRequest/NextResponse and deterministic transport/timers. `evidence/cala/contract-before.json` preserves failures. All 18 final cases in `contract.json` pass, including bounded retry, optional detail fallback, timeout abort, key normalization, malformed provider shape and public informational success. `http-before.json` and `http-after.json` independently demonstrate the two original HTTP500s becoming safe HTTP200 false and removal of a synthetic extra-data marker from successful output. The isolated server used synthetic credentials and a fail-closed mock fetch; it was stopped after verification.

No persistence, migration, concurrency, payment or lifecycle notification applies to this pure Next BFF. Backend code is unchanged; required complete backend regression is nevertheless running. Frontend `npm run typecheck`, `npm run lint`, `npm run build`, and `git diff --check` all ran successfully. No real Cala verification is claimed.

## WHAT ELHAM SHOULD TEST PERSONALLY

No new user-facing flow is introduced. If the unused compatibility component is deliberately integrated later, review whether its informational wording is understandable and does not imply identity/KYC approval.

## PRODUCT / OPERATIONS DECISIONS

None required to close the existing local contract. Connecting this optional lookup to trust, booking, or an identity claim would be separate product work, not part of these fixes.

## STAGING ONLY

Actual Cala schema, key provisioning, provider availability and deployed edge controls remain unverified. PROVIDER-001–005 retain their external blockers and unchanged report. Real screen-reader execution remains deferred.

## Completion gate

Focused and rebuilt HTTP checks PASS; full backend regression and PDF update pending. Next locally executable domain: EMAIL-001–031.

Independent HTTP reproduction is repeatable with the already built frontend on isolated loopback port 3027, `CALA_API_KEY=synthetic-qa-only` and `NODE_OPTIONS=--require=/home/elham/workspace/Eema-backend/scripts/qa/cala-http-preload.cjs`. POST the three bodies `{ "company": "Synthetic Company" }`, `null`, and `{ "company": { "toString": null } }` to `/api/verify-booker`. Expected post-fix statuses are 200/200/200, verified values true/false/false, and no synthetic raw marker in any output. The checked-in shim is equivalent to the temporary shim used for the recorded before/after runs and forbids every outbound fetch instead of requiring real credentials.

A read-only replay against `HEAD` source (`node scripts/qa/cala-contract-check.cjs --baseline`) also preserves the strengthened 18-case pre-fix regression as `contract-baseline.json`: 6 PASS / 12 FAIL, with intentional nonzero exit. Several variants fail the same log-minimization assertion; this is not 12 distinct defects. Numeric company input and a quoted blank key each attempted two mocked provider calls before the fix; both now make zero. The working-source rerun is 18 PASS. An initial sandbox attempt could not spawn read-only Git; rerunning with the authorized local execution permission succeeded. No production file was reverted.

## Regression harness cleanup

The full regression exposed a cached Spring context in `PostgresCriticalPathIntegrationTest` continuing scheduled work after its Testcontainer was destroyed (HikariPool-17 connection-refused traces). Its 14 assertions passed; these traces were not recovery-test failures. The other PostgreSQL suites already inherit `@DirtiesContext(AFTER_CLASS)` from AuthPostgresSupport. Applied that same cleanup convention to this standalone class so schedulers/pool are closed at class completion. No application production behavior changes. A focused rerun of the changed class is required after the active full suite; the active run compiled the previous annotation set.

## Final regression and completion

- `mvn test`: PASS — 1078 reported, 1078 executed, 0 skipped, zero failures/errors.
- `mvn verify -Ppostgres-integration`: PASS — 1027 reported, 1014 executed, 13 skipped, zero failures/errors.
- `mvn clean package`: PASS — 1078 reported, 1078 executed, 0 skipped, zero failures/errors.
- `mvn verify -Ppostgres-integration -Dit.test=PostgresCriticalPathIntegrationTest`: PASS — 14 reported, 14 executed, 0 skipped, zero failures/errors.
- `npm run typecheck`: PASS.
- `npm run lint`: PASS.
- `npm run build`: PASS.

The 13 opt-in browser launcher skips in the full PostgreSQL profile are not counted as executed. Existing UX/UXACT/SEC browser evidence is reused; CALA adds real local HTTP checks because its compatibility UI has no production caller. Fresh common migrations and Hibernate validation passed, as did packaged recovery and concurrency suites. No migration or backend production change was introduced. The critical-path test context cleanup passed its focused rerun after the full suite. Full-run recovery and query-plan artifacts were refreshed, with previous versions preserved under `evidence/cala/history/`.

Both repositories pass `git diff --check`. Evidence includes historical failures, the current 18 passing checks, HTTP before/after outcomes, sanitized executed-method results, source hashes, regression metadata and master-plan PDF verification. No live provider result, legal approval or real screen-reader acceptance is claimed.

**Locally clear to proceed: YES. Next locally executable domain: EMAIL-001–031.** PROVIDER-001–005 remain BLOCKED — STAGING/EXTERNAL and their report is unchanged. Earlier in-progress gate statements above are preserved as execution history.
