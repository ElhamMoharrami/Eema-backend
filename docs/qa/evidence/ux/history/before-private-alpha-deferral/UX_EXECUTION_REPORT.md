# UX execution report

**UX-001–035: 24 PASS / 0 FAIL / 11 BLOCKED under the approved representative acceptance model. Domain: BLOCKED.** Automated coverage of all 35 routes remains PASS (918 executed checks). The 11 selected route scenarios retain distinct actual screen-reader acceptance work; the other 24 no longer require duplicate manual execution of shared patterns. Their PASS does not certify actual AT or close the shared domain acceptance gate. **UX-ENV-001 remains open** until all 11 representatives pass. This corrects test scope, not product behavior or an accessibility waiver. UXACT has not started.

## Audit, inventory and corrected assumptions

Read the complete plan and previous execution reports earlier in this continuing validation, then read the 35 UX routes, their common requirements and every role/state substitution. Original scenarios and the pre-implementation/source audit are preserved in `evidence/ux/history/`. Inspected frontend pages, shared role/auth/policy gates, response transport and adapters, status/error/loading components, financial and trust details, owner editors, recovery forms and navigation. Backend response DTOs and current service mappings were checked before adding structural validation. Existing PostgreSQL schema/authorization/state guarantees remain those verified in prior reports; UX adds no migration or backend authority.

Public recovery/verification does not require login, verified email or current-policy acceptance. Policy content currently requires login. Admin operations use ADMIN authority without a blanket marketplace-policy prerequisite. Static admin home/new-listing form have no invented result loaders. Promise `fulfilled/rejected` values are implementation details, not user-facing domain states. Inline confirmations are not automatically modal dialogs. Recovery endpoints return MessageResponse JSON; explicit empty mutations retain their existing empty-response contract.

The generated MANUAL ONLY label is corrected: deterministic layout, response, labels, keyboard and role-state checks are automated; subjective clarity remains human review. Actual screen-reader execution is retained as a shared domain gate, using 11 distinct representatives rather than every route/state permutation. Simulated responses do not prove provider or database behavior.

## Scenario results

| Scenario | Route | Executed browser/client portion | Overall |
|---|---|---|---|
| UX-001 | `/admin/bookings/[id]` | PASS; see route checks | BLOCKED: AT-R09 actual AT pending |
| UX-002 | `/admin/bookings` | PASS; see route checks | BLOCKED: AT-R08 actual AT pending |
| UX-003 | `/admin/exceptions` | PASS; see route checks | BLOCKED: AT-R11 actual AT pending |
| UX-004 | `/admin/incidents/[id]` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-005 | `/admin/incidents` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-006 | `/admin/liabilities/[id]` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-007 | `/admin/liabilities` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-008 | `/admin/owner-verifications/[id]` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-009 | `/admin/owner-verifications` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-010 | `/admin` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-011 | `/admin/payout-destinations/[id]` | PASS; see route checks | BLOCKED: AT-R10 actual AT pending |
| UX-012 | `/admin/payout-destinations` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-013 | `/admin/payouts/[id]` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-014 | `/admin/payouts` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-015 | `/admin/policies` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-016 | `/admin/venues/[id]` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-017 | `/admin/venues` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-018 | `/booked` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-019 | `/bookings/[id]` | PASS; see route checks | BLOCKED: AT-R03 actual AT pending |
| UX-020 | `/forgot-password` | PASS; see route checks | BLOCKED: AT-R01 actual AT pending |
| UX-021 | `/owner/bookings` | PASS; see route checks | BLOCKED: AT-R05 actual AT pending |
| UX-022 | `/owner/inquiries` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-023 | `/owner/payout-destination` | PASS; see route checks | BLOCKED: AT-R07 actual AT pending |
| UX-024 | `/owner/payouts/[id]` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-025 | `/owner/payouts` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-026 | `/owner/venues/[id]` | PASS; see route checks | BLOCKED: AT-R06 actual AT pending |
| UX-027 | `/owner/venues/new` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-028 | `/owner/venues` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-029 | `/owner/verification` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-030 | `/` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-031 | `/policies` | PASS; see route checks | BLOCKED: AT-R02 actual AT pending |
| UX-032 | `/reset-password` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-033 | `/scout/bookings` | PASS; see route checks | BLOCKED: AT-R04 actual AT pending |
| UX-034 | `/scout/inquiries` | PASS; see route checks | PASS: route scope; shared AT gate pending |
| UX-035 | `/verify-email` | PASS; see route checks | PASS: route scope; shared AT gate pending |

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

## UX-ENV-001: required screen-reader execution

The initial Orca attempt could not connect to AT-SPI. A private accessibility bus subsequently started, but neither Wayland nor X11 graphical Chrome initialized a usable debugging endpoint in that isolated setup. Xvfb, Xephyr, Weston and Cage are unavailable. Temporary private settings/profile attempts were cleaned up; the user's desktop preferences were not changed. `screen-reader-environment.json` records the available/failed capabilities.

Chromium AX-tree and keyboard results do not certify real screen-reader announcements, browse mode, errors, headings or dialog interaction. The original 35 repeated requirements are superseded by the user-approved representative matrix. Eleven selected scenarios remain BLOCKED by pending real AT acceptance; the remaining 24 have passing route-level evidence. UX as a domain remains BLOCKED until all representatives pass. The current tool inventory provides browser automation, not control and observation of Orca on the normal desktop. No further isolated graphical environment was attempted. Elham can execute the supplied checklist in the normal desktop browser/Orca session; actual results remain unrecorded.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review terminology, confidence in booking/payment/payout wording, destructive-action clarity, the denser mobile admin navigation and reading comfort after contrast/wrapping changes. No manual reproduction of financial arithmetic, authorization matrices, locks, tokens or provider idempotency is requested.

## PRODUCT / OPERATIONS DECISIONS

No new product/business/legal decision was inferred. UX-ENV-001 is an unmet execution requirement. Existing legal text and launch/provider prerequisites remain separate. No new workflow, payment path, notification template or administrative power was added.

## STAGING ONLY

Real provider delivery/payments/storage, deployed edge/session behavior and production browser/device integration remain unverified locally. Real assistive-technology execution needs an appropriate environment but does not inherently require production or live providers. Broader compatibility follow-up should include a representative Windows/NVDA browser combination; every browser/reader permutation is not a local launch blocker.

## Documentation and continuation

Updated this report, scenario inventory, master-plan UX wording/statuses and PDF; retained all 703 scenario IDs/order and historical defect evidence. PDF extraction and changed-page visual checks are recorded separately.

**Locally clear to proceed automatically: NO. NEXT UNFINISHED DOMAIN: UX (UX-ENV-001).** The following unstarted domain is UXACT-001–019; it has not been executed or modified by this pass.

## Approved representative AT acceptance correction (2026-09-21)

Before implementation: selected UX-001–035, dependent on existing UX automation plus AUTH/POL/BK/VEN/DEST/ADMIN workflow evidence. No production behavior, schema or pricing/state authority changes are required for this correction. Historical 35-BLOCKED classification is retained in `evidence/ux/history/representative-at-before/`.

See [the 11-flow manual checklist](UX_ASSISTIVE_TECHNOLOGY_CHECKLIST.md), [blank evidence template](evidence/ux/representative-at-results.csv), and `evidence/ux/representative-at-matrix.json`. Each flow names its unique component family and concrete actions. Normal Linux desktop plus Orca and Firefox/Chrome is the immediate target. This session cannot control and observe the actual desktop reader; no announcement is fabricated. The checklist is the explicitly authorized fallback, not a request for a product decision.

All 11 actual-AT results are pending (BLOCKED); this is one shared acceptance gate, not 35 duplicated manual journeys. For each representative, check common navigation/name/focus/feedback criteria plus its distinct actions. N/A is only for an absent control/pattern, with a reason; unavailable fixtures or unexecuted flows remain BLOCKED. The reader must be active and its actual announcements observed. AX-tree results cannot fill these fields.

Validation for this documentation-only correction: verify 35 unique route mappings, 11 distinct representatives, evidence/result counts, unchanged 703 master scenario IDs/order, regenerate/extract/visually inspect PDF, and both repositories' diff checks. No new application tests or builds are claimed; prior validated code and the 918-check evidence are unchanged.

On an observed defect, record severity and a concrete reproduction, fix clear existing-behavior defects, rerun affected automated checks and that real-AT representative, then frontend typecheck/lint/build/diff check. Preserve failed evidence. After all 11 actual passes and any fixes are validated, mark UX locally complete and continue to UXACT-001–019 under the existing automatic-continuation authorization. Until then, STOP.
