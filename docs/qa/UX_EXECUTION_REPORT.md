# UX execution report

**UX-001–035: 35 PASS / 0 FAIL / 0 BLOCKED for the current private-alpha/local scope.** Separately: **11 representative AT flows DEFERRED / NOT EXECUTED** (not 11 additional master-plan scenarios). UX-ENV-001 is closed as **DEFERRED BY EXPLICIT PRODUCT/LAUNCH-SCOPE DECISION**, not technically passed.

For the current private-alpha/local validation gate, real assistive-technology execution is deferred by explicit scope decision. Existing automated accessibility, keyboard, browser, responsive and Chromium accessibility-tree checks remain required and pass. This does not constitute real screen-reader testing or accessibility certification.

## Audit, inventory and corrected assumptions

Read the complete plan and previous execution reports earlier in this continuing validation, then read the 35 UX routes, their common requirements and every role/state substitution. Original scenarios and the pre-implementation/source audit are preserved in `evidence/ux/history/`. Inspected frontend pages, shared role/auth/policy gates, response transport and adapters, status/error/loading components, financial and trust details, owner editors, recovery forms and navigation. Backend response DTOs and current service mappings were checked before adding structural validation. Existing PostgreSQL schema/authorization/state guarantees remain those verified in prior reports; UX adds no migration or backend authority.

Public recovery/verification does not require login, verified email or current-policy acceptance. Policy content currently requires login. Admin operations use ADMIN authority without a blanket marketplace-policy prerequisite. Static admin home/new-listing form have no invented result loaders. Promise `fulfilled/rejected` values are implementation details, not user-facing domain states. Inline confirmations are not automatically modal dialogs. Recovery endpoints return MessageResponse JSON; explicit empty mutations retain their existing empty-response contract.

The generated MANUAL ONLY label is corrected: deterministic layout, response, labels, keyboard and role-state checks are automated; subjective clarity remains human review. Actual screen-reader execution is deferred to future accessibility/public-launch acceptance, using the preserved representative matrix. Simulated responses do not prove provider or database behavior.

## Scenario results

| Scenario | Route | Executed browser/client portion | Overall |
|---|---|---|---|
| UX-001 | `/admin/bookings/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-002 | `/admin/bookings` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-003 | `/admin/exceptions` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-004 | `/admin/incidents/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-005 | `/admin/incidents` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-006 | `/admin/liabilities/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-007 | `/admin/liabilities` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-008 | `/admin/owner-verifications/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-009 | `/admin/owner-verifications` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-010 | `/admin` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-011 | `/admin/payout-destinations/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-012 | `/admin/payout-destinations` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-013 | `/admin/payouts/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-014 | `/admin/payouts` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-015 | `/admin/policies` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-016 | `/admin/venues/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-017 | `/admin/venues` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-018 | `/booked` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-019 | `/bookings/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-020 | `/forgot-password` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-021 | `/owner/bookings` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-022 | `/owner/inquiries` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-023 | `/owner/payout-destination` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-024 | `/owner/payouts/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-025 | `/owner/payouts` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-026 | `/owner/venues/[id]` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-027 | `/owner/venues/new` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-028 | `/owner/venues` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-029 | `/owner/verification` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-030 | `/` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-031 | `/policies` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-032 | `/reset-password` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-033 | `/scout/bookings` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-034 | `/scout/inquiries` | PASS; see route checks | PASS: current private-alpha/local scope |
| UX-035 | `/verify-email` | PASS; see route checks | PASS: current private-alpha/local scope |

## Authoritative contract and coverage reused

Spring remains authoritative for roles/ownership, persisted booking/payment/refund/payout state, money, availability, policy eligibility and transitions. Client guards only verify the structure needed to render a response; they do not recalculate prices, clamp monetary values, invent statuses or infer provider success. Valid payloads are returned unchanged, including €0.01 and nullable pending monetary fields. Unknown/malformed required display data uses the existing MALFORMED_RESPONSE/error/retry path.

Reused AUTH and SEC session/authorization evidence; POL acceptance/publication; VEN/READY/TRUST/MEDIA listing semantics; BK/AVL/INQ inventory and request transitions; PRICE/PAY/CANCEL/REF amounts and payment lifecycle; PAYOUT/DEST/DOC financial and private-document semantics; INC/ADMIN operations; MATCH discovery; EDGE boundaries; OPS outbox durability; JOB/RACE concurrency. Existing corresponding domain browser reports remain evidence for workflow states and mutation outcomes; the new UX tests focus on presentation/transport rather than reimplementing every backend boundary.

## Tests added and browser results

- `ux-client-check.cjs`: **56 PASS**. Actual TypeScript adapters/transport reject primitive, absent and nested malformed bodies, preserve HTTP error classification, recover with valid responses, and leave amounts unchanged. Existing AUTH client regression rerun PASS.
- `ux-browser.cjs`: **619 PASS** across all 35 pages, three viewport widths, actual applicable loaders, HTTP 400/401/403/404/409/500, offline, invalid JSON, wrong-shape JSON, empty collections and anonymous/wrong-role guards. Static pages have no invented loader variants. Valid JSON fixtures are synthetic; authorization/database proof comes from prior integration suites.
- Long-content run: **105 PASS**, 375/768/1440 widths with long Unicode names/reasons and large monetary displays. Document overflow and control-label assertions are explicit; wide admin tables keep their local scroll containers.
- `ux-accessibility.cjs`: **112 PASS**. Chromium accessibility control names, computed active-text contrast, distinct keyboard traversal, inline reveal focus/return and held/released live loading region. This is not a spoken screen-reader test or complete WCAG certification.
- `ux-flow-probes.cjs`: **26 PASS**. Recovery/verification success/failure/malformed behavior, existing admin navigation, independent timeline error/retry with booking facts retained, and no marketplace mutations from read-only probes.
- Existing `AdminBrowserIntegrationTest` was additionally executed against real PostgreSQL and the built frontend: **1 PASS**, no skips. Its real API responses pass the new guards; pagination, 202 exceptions, safe event payloads and timeline recovery remain correct. Provider calls were mocked/absent.

Evidence is under `evidence/ux/`: per-check JSON, synthetic fixtures, mobile screenshots, client/auth results, real-admin summary and historical failures. Chromium automation uses isolated profiles and intercepted local transport. No live Mollie, Resend, S3, Gemini or financial transfer was invoked.

## Defects found and fixed

| Defect | Severity | Reproduction and final correction |
|---|---|---|
| UX-DEF-001 | P2 | `200 {}` crashed admin bookings/policies and scout inquiries. Structural adapter guards now route malformed bodies to existing safe error/retry UI. Nested cases and valid real-API responses are covered. |
| UX-DEF-002 | P2 | Inline reveal falsely claimed modal semantics and left focus outside it. It now uses named inline-group semantics with deliberate action focus and return to the opener on cancel. |
| UX-DEF-003 | P2 | Two production-activity selectors lacked individual accessible names. Added names derived from their existing labels. |
| UX-DEF-004 | P2 | Muted/ochre small text and faded photo placeholders had inadequate contrast; borderless brief focus was not visibly indicated. Darkened the existing palette, removed opacity from active captions/future-date inquiry controls, and provided visible focus. Shared loading/error components now expose status/alert semantics. |
| UX-DEF-005 | P2 | Long unbroken content overflowed mobile and some wider layouts. Added natural emergency wrapping and constrained detail grid items; no content is silently clipped or shortened. |

Historical wrong-shape crashes, label failures, 43 initial long-content failures and the remaining caption/grid failures are retained. Harness-only issues (case-sensitive waits on visually uppercase text, initial VM missing URLSearchParams, CDP close/timeouts, profile cleanup) were corrected without changing product expectations. No known unresolved P0/P1 or reproduced product defect remains in the executed UX checks.

## Required validation and PostgreSQL results

Frontend `npm run typecheck`, `npm run lint`, `npm run build`, and both repositories' `git diff --check`: **PASS** after final source corrections. The focused browser/client evidence above uses that final built state; the additional real-API regression preceded only CSS/label corrections, with response guards unchanged.

Backend source was unchanged in UX. The latest complete JOB/RACE regression is reused: `mvn test` **1,078 PASS**; `mvn verify -Ppostgres-integration` **1,023 reported / 1,010 executed / 13 opt-in browser skips**, zero failures/errors; `mvn clean package` **1,078 PASS**. The additional UX `mvn verify -Ppostgres-integration -Dit.test=AdminBrowserIntegrationTest -Deema.admin.browser=true` completed successfully with the unit suite and one enabled PostgreSQL/browser test. Fresh migrations through common V14 and Hibernate startup validation succeeded there. No concurrency authority changed; prior JOB/RACE/OPS results remain applicable.

## UX-ENV-001 disposition and historical evidence

Explicit user scope decision supersedes the prior representative gate. Historical Orca/private-bus/graphical-browser attempts did not produce a usable AT/browser session. Actual AT execution was never completed. Previous 35-BLOCKED and 24-PASS/11-BLOCKED classifications remain in `evidence/ux/history/`; the immediately preceding state is in `before-private-alpha-deferral/`. No failure evidence was deleted and no screen-reader PASS is claimed.

## Re-audit and validation for scope correction

Reviewed every UX route against stored browser, accessibility and long-content results: all 35 have passing route-specific evidence, including three long-content viewports. All five evidence groups remain PASS, 918 checks total. `evidence/ux/scope-reaudit.json` records per-scenario counts and source-evidence hashes. Form semantics, keyboard traversal, focus, errors/loading, labels, contrast, responsive/Unicode handling and Chromium AX checks stay required. Existing real-API/admin and prior domain workflow evidence remains applicable. No production code changed and no reproduced non-AT requirement was converted from untested to PASS. All five P2 fixes remain intact.

Documentation/status/count/703-ID checks and both repository diff checks run for this correction. Existing backend/frontend gate results above are reused; no new test/build run or actual AT run is claimed for this documentation-only decision.

## WHAT ELHAM SHOULD TEST PERSONALLY

Current subjective review remains terminology, booking/payment/payout clarity, consequential actions, dense mobile navigation and reading comfort. The preserved [real AT checklist](UX_ASSISTIVE_TECHNOLOGY_CHECKLIST.md) and `evidence/ux/representative-at-results.csv` are future validation assets.

## PRODUCT / OPERATIONS DECISIONS

Resolved current scope: actual screen-reader execution is deferred for private alpha/local validation. This is not a technical pass and introduces no product feature or accessibility regression. No unresolved current UX decision or known P0/P1 defect remains.

## POST-ALPHA / PUBLIC-LAUNCH ACCESSIBILITY

**Perform representative real assistive-technology testing before broader/public launch.** All items below are DEFERRED / NOT EXECUTED:

- Representative NVDA/Windows testing.
- Representative VoiceOver/macOS/iOS testing where applicable.
- Orca/Linux where useful.
- Actual spoken announcement validation.
- Browse-mode/navigation validation.
- Accessibility review against the target WCAG standard.

No WCAG certification or real-reader compatibility is claimed. These future requirements are visible launch items, not current local/private-alpha blockers.

## STAGING ONLY

Real providers and deployed edge/device behavior remain separately unverified. Actual AT testing is a later-phase acceptance item, not inherently staging-only.

## Continuation

**Locally clear to proceed: YES under the approved private-alpha/local scope. NEXT UNTESTED DOMAIN: UXACT-001–019.** Continue under existing higher-autonomy rules; stop on real defects/decisions/dependencies, not the deferred AT item.
