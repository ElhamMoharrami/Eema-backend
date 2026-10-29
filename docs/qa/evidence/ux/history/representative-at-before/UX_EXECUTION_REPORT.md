# UX execution report

**UX-001–035: 0 PASS / 0 FAIL / 35 BLOCKED overall.** All executed local browser/client checks pass after five P2 fixes. Every generated UX scenario also explicitly requires screen-reader execution; that requirement remains unverified, so no complete scenario is marked PASS. **UX-ENV-001** is an execution-environment limitation, not an approved exemption or a product decision. Automatic continuation stops here; UXACT was not started.

## Audit, inventory and corrected assumptions

Read the complete plan and previous execution reports earlier in this continuing validation, then read the 35 UX routes, their common requirements and every role/state substitution. Original scenarios and the pre-implementation/source audit are preserved in `evidence/ux/history/`. Inspected frontend pages, shared role/auth/policy gates, response transport and adapters, status/error/loading components, financial and trust details, owner editors, recovery forms and navigation. Backend response DTOs and current service mappings were checked before adding structural validation. Existing PostgreSQL schema/authorization/state guarantees remain those verified in prior reports; UX adds no migration or backend authority.

Public recovery/verification does not require login, verified email or current-policy acceptance. Policy content currently requires login. Admin operations use ADMIN authority without a blanket marketplace-policy prerequisite. Static admin home/new-listing form have no invented result loaders. Promise `fulfilled/rejected` values are implementation details, not user-facing domain states. Inline confirmations are not automatically modal dialogs. Recovery endpoints return MessageResponse JSON; explicit empty mutations retain their existing empty-response contract.

The generated MANUAL ONLY label is corrected: deterministic layout, response, labels, keyboard and role-state checks are automated; subjective clarity remains human review. Actual screen-reader execution is retained as a separate required check. Simulated responses do not prove provider or database behavior.

## Scenario results

| Scenario | Route | Executed browser/client portion | Overall |
|---|---|---|---|
| UX-001 | `/admin/bookings/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-002 | `/admin/bookings` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-003 | `/admin/exceptions` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-004 | `/admin/incidents/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-005 | `/admin/incidents` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-006 | `/admin/liabilities/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-007 | `/admin/liabilities` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-008 | `/admin/owner-verifications/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-009 | `/admin/owner-verifications` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-010 | `/admin` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-011 | `/admin/payout-destinations/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-012 | `/admin/payout-destinations` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-013 | `/admin/payouts/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-014 | `/admin/payouts` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-015 | `/admin/policies` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-016 | `/admin/venues/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-017 | `/admin/venues` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-018 | `/booked` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-019 | `/bookings/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-020 | `/forgot-password` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-021 | `/owner/bookings` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-022 | `/owner/inquiries` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-023 | `/owner/payout-destination` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-024 | `/owner/payouts/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-025 | `/owner/payouts` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-026 | `/owner/venues/[id]` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-027 | `/owner/venues/new` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-028 | `/owner/venues` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-029 | `/owner/verification` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-030 | `/` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-031 | `/policies` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-032 | `/reset-password` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-033 | `/scout/bookings` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-034 | `/scout/inquiries` | PASS; see route checks | BLOCKED: UX-ENV-001 |
| UX-035 | `/verify-email` | PASS; see route checks | BLOCKED: UX-ENV-001 |

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

Chromium AX-tree and keyboard results do not certify real screen-reader announcements, browse mode, errors, headings or dialog interaction. All 35 scenarios retain this required outstanding execution. A usable browser/screen-reader test environment is needed to close it; do not substitute source inspection or waive it implicitly. The master plan/PDF record BLOCKED rather than a fabricated PASS.

## WHAT ELHAM SHOULD TEST PERSONALLY

Review terminology, confidence in booking/payment/payout wording, destructive-action clarity, the denser mobile admin navigation and reading comfort after contrast/wrapping changes. No manual reproduction of financial arithmetic, authorization matrices, locks, tokens or provider idempotency is requested.

## PRODUCT / OPERATIONS DECISIONS

No new product/business/legal decision was inferred. UX-ENV-001 is an unmet execution requirement. Existing legal text and launch/provider prerequisites remain separate. No new workflow, payment path, notification template or administrative power was added.

## STAGING ONLY

Real provider delivery/payments/storage, deployed edge/session behavior and production browser/device integration remain unverified locally. Real assistive-technology execution needs an appropriate environment but does not inherently require production or live providers.

## Documentation and continuation

Updated this report, scenario inventory, master-plan UX wording/statuses and PDF; retained all 703 scenario IDs/order and historical defect evidence. PDF extraction and changed-page visual checks are recorded separately.

**Locally clear to proceed automatically: NO. NEXT UNFINISHED DOMAIN: UX (UX-ENV-001).** The following unstarted domain is UXACT-001–019; it has not been executed or modified by this pass.
