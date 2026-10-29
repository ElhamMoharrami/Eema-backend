# Dependency-security execution report — G23

## Initial audit — 2026-09-21

The complete remaining-plan audit identifies G23 as locally actionable after the indexed INPUT/API/STATE/AUTO work. Final combined backend regression remains pending; there is no new product decision. Existing frontend README records a required patched-major migration, but its September7 “no critical” audit is now historical. Fresh `npm audit --omit=dev` reports next critical and its nested postcss high. Installed Next14.2.35 is outside the supported release lines. This is package/advisory evidence, not a claim that every listed exploit is reachable in Eema.

Source audit: App Router; no Pages Router, custom rewrites, middleware/proxy, Server Actions, CSP nonce configuration or WebSocket upgrade handler. Images remain unoptimized. Accordingly the Windows-only and configured AVIF image-optimization RCE conditions are not demonstrated in this Linux app. RSC availability/cache advisories are relevant enough that leaving the unsupported framework is not an acceptable local security closeout. No exploit is run against external infrastructure.

Authoritative sources: [support policy](https://nextjs.org/support-policy), [August2026 security release](https://nextjs.org/blog/august-2026-security-release), [Next15 migration](https://nextjs.org/docs/app/guides/upgrading/version-15), [Next16 migration](https://nextjs.org/docs/app/guides/upgrading/version-16), and the advisory URLs retained in the sanitized npm report. Npm registry metadata supplies exact release and peer/runtime requirements. Existing Node22.22.1 meets the current runtime floor.

Implementation strategy: move to supported active-LTS Next16.3.5 and compatible React/types/lint dependencies; retain Webpack for this security migration. Adapt async route params and ESLint CLI/flat configuration as required by that version. Retain current marketplace, payment, auth and pricing authority. Audit the resulting dependency tree and resolve applicable transitive findings without `npm audit fix --force`. Run typecheck/lint/build, actual client harnesses and representative existing browser route/status/auth/error checks. Recheck relevant CALA contract and no raw output. No deployment or live provider call.

## WHAT ELHAM SHOULD TEST PERSONALLY

Mobile navigation and whether loading/error transitions still feel clear; no manual dependency/exploit tests required.

## PRODUCT / OPERATIONS DECISIONS

No new product behavior is being selected. Deployed rollout and supported device acceptance remain launch work.

## STAGING ONLY

Actual hosting runtime, proxy/CDN/cache behavior and supported Safari/Firefox/mobile acceptance remain unverified by local Chromium and builds. Provider blockers and deferred real AT remain unchanged.

## Compatibility and initial checks

Installed Next16.3.5, React/react-dom19.3.0, matching React types19.3.0 and eslint-config-next16.3.5. Npm installation completed with zero audit findings, including development dependencies. Node22.22.1 is used. ESLint9.39.5 is the newest version allowed by eslint-plugin-react's declared peer range; npm marks that lint-only major unsupported. ESLint10 is not forced past that peer contract. Track compatible tooling support updates separately; no known npm vulnerability is present in the resolved tree.

Ten client detail pages now unwrap Promise params using React.use. Webpack remains the bundler; `next lint` is replaced by ESLint CLI/flat config, including the hooks directory. Initial TypeScript check passed. The new React plugin initially reported17 errors for pre-existing synchronous effect-loading/declaration patterns under two newly introduced compiler diagnostics. React Compiler is not enabled. Those two diagnostics remain visible as warnings during incremental adoption; existing rules-of-hooks/exhaustive-deps, accessibility and other checks remain enforced. No workflow was rewritten solely for compiler optimization. This is an explicit lint-policy compatibility choice, not a claim those diagnostics vanished. See [React plugin guidance](https://react.dev/reference/eslint-plugin-react-hooks).

Browser harnesses gain optional evidence-output paths so the security-upgrade rerun does not overwrite prior UX/EMAIL evidence. No browser behavior or assertion is weakened.

### Browser harness isolation finding

Initial full sweep:1006 checks,1005 PASS and1 FAIL. The only failure was a missing targeted moderation GET in the owner-venue injected403 subcase; no crash, overflow, label failure, raw marker or unexpected endpoint. The harness directly overwrote localStorage with the same legacy session generation for every simulated login, unlike the real saveAuthSession contract that assigns a fresh generation. A previous injected401 could therefore clear the following synthetic login before its loader fired. Preserve that result under history. Updated only harness login metadata to use a new generation/storedAt per login; no application auth behavior changed and no assertion removed. Full rerun and independent session/action checks will determine final disposition.

A separate expiry-harness run completed all10 assertions but exited1 during intentional Browser.close. Its shared asynchronous error handler referenced undefined `checks`; after correcting that diagnostic, an independent rerun exposed the underlying `CDP connection closed` rejection. The shared helper now marks intentional close before Browser.close and ignores only that exact connection-close rejection during teardown. Active interception failures still fail with their real error and no undefined variable. Both failed commands are retained; expiry and cross-actor checks are rerun after this test-only cleanup correction.

## Final result

**G23 resolved locally.** No remaining applicable dependency-audit finding in the resolved npm tree; no new product/financial behavior or backend migration. Unsupported lint-tool major and19 visible warnings remain documented maintenance items, not hidden failures or a claim of universal tooling support.

- Production build and post-build TypeScript pass. ESLint passes with0 errors/19 warnings. Full and production npm audits both report0 vulnerabilities; installed top-level dependency tree is valid.
- Corrected full browser sweep:1006 PASS,0 failures,0 unexpected endpoints across35 routes,375/768/1440 layouts, malformed/error/offline/loading/empty/role states. The initial1005 PASS/1 missing-request failure remains in history; only synthetic login generation isolation changed.
- `browser/accessibility.json`: 112 checks PASS.
- `actions/browser-actions.json`: 75 checks PASS.
- `expiry-browser.json`: 10 checks PASS.
- `session-browser.json`: 1 checks PASS.
- `email-browser.json`: 9 checks PASS.
- `cala-http.json`: 4 checks PASS.
- Nine actual client harnesses pass: Cala, auth, security, action replay, state representations, UX response schemas, policy, incident and admin stale-load handling. Detailed outcomes and hashes are stored in `client/`.
- Real AT is still deferred. Keyboard/AX browser results do not prove spoken screen-reader behavior. All browser APIs are synthetic local fixtures except the explicit invalid-input Next HTTP checks; no real provider or transfer occurs.
- Mobile owner listing and admin verification screenshots were inspected for layout; automated layout checks cover all35 routes.

The current frontend README removes historical Next14 audit claims and stale listing/upload/admin descriptions. No generic deletion workflow or financial admin action was added.

**Locally clear.** Full backend gates and master-plan/PDF closeout are recorded in the final validation closeout; PROVIDER-001–005 remain externally blocked without report modification.
