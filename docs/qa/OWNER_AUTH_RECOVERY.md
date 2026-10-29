# Owner stale-auth recovery — execution report

**RESOLVED / LOCAL PASS, 2026-09-10.** Frontend-only application changes. Backend source, migrations and pom remain byte-for-byte unchanged. QA documents/evidence were updated as requested; local fixtures used the existing guarded harness and provider-disabled backend.

## Root cause and proof

An expired access token could produce403 on an OWNER method behind a publicly matched URL while a parallel authenticated request produced401. Only401 entered client refresh recovery. The403 was committed to the page's error state, and loaders did not rerun merely because shared authentication subsequently recovered. The generic API mapper incorrectly rendered every403 as an ADMIN requirement. The owner payout-detail page also reused an ADMIN-only forbidden component.

Before editing, the focused test executed the actual client with controlled transport: a403 arriving during refresh rejected instead of resolving with the new generation. Source audit identified the403-before-refresh-start ordering too; that now has a deterministic regression. Browser assertion problems encountered during validation were harness issues: CSS uppercase affected `innerText`, and an intercepted403 affected CORS preflight. Final checks use underlying rendered text and a real other-owner venue denial, respectively. These were not hidden product failures or reasons to relax the acceptance criterion.

## Final behavior and limits

- Each successful session save creates a non-secret UUID generation. Existing cached sessions get a stable timestamp-based transitional identity. Refresh, stale-failure and stale-success comparisons use generation IDs, not raw tokens. Password/token contents and backend contracts are untouched.
- Required GET/HEAD403 joins an active refresh. If none has started and the original generation remains current, concurrent reads share a `/auth/me` check. Only a401 from that authenticated check can initiate normal refresh. A valid200 check leaves the original403 forbidden.
- The original resource is retried **at most once**, only after a successful generation change for the same user. The existing401 retry and this403 recovery share the same retry flag. A retry returning401/403 is surfaced; it cannot start another recovery chain. `/auth/me` is excluded from403 probing, preventing recursion. Concurrent probes coalesce per generation.
- There is no403-based replay for POST/PUT/DELETE or other mutations. A genuine owner ownership denial remains403, including after a retry. Existing role guards remain authoritative for UI access; backend authorization remains authoritative for requests.
- Required responses verify the current account before returning results, including after reading response bodies. Logout or account switching rejects a pending old success rather than supplying data to an authenticated page. RoleGuard removes protected content on logout. No page-level retry subscription or form-remount behavior was added.
- OWNER page errors use owner-resource wording; the default mapper is neutral. Explicit ADMIN guards/load states retain ADMIN wording. The shared payout-detail load-state component now accepts OWNER context. Verification/policy-specific messages retain their existing precedence.

A genuinely forbidden read can incur one additional authenticated session check. That check is read-only and does not turn authorization denial into success. If session validation/recovery cannot establish a newer session, the original resource is not replayed. No independent page retry layer was added.

## Owner route review

| Route | Loader / behavior reviewed | Coverage |
|---|---|---|
| `/owner/venues` | Required `getMyVenues`; owner mapper | Real single-/two-tab expired-session recovery without reload, three repetitions |
| `/owner/venues/[id]` | `getVenue` explicitly required on this page; required availability settings; existing owner forbidden/not-found UI | Real other-owner venue settings403 and owner denial page |
| `/owner/bookings` | Required bookings + venue names in `Promise.allSettled`; owner mapper | Source review and shared-client parallel-read assertions |
| `/owner/inquiries` | Required inquiry loader; owner mapper for load/actions | Source review and shared-client recovery/no-write-replay assertions |
| `/owner/payouts` | Required payout page + venue names; existing effect cleanup and retry | Source review and shared-client recovery assertions |
| `/owner/payouts/[id]` | Required payout detail + venue names; reused `useAdminPage` | Source review; actual shared load-state branch executed with OWNER and ADMIN contexts |
| `/owner/payout-destination` | Required destinations + owner verification in `Promise.all`; owner mapper | Source review and shared-client parallel-read assertions |
| `/owner/verification` | Required verification loader; owner mapper | Existing full browser owner verification/resend journey rerun |
| `/owner/venues/new` (additional) | Existing role/verification gates; owner action-error mapper | Existing full browser resend path rerun; no creation workflow redesign |

This is not a claim that every financial or inquiry workflow was browser-tested. Shared recovery semantics, all requested route integrations and owner/admin error branches were reviewed; the browser exercised the stated routes and auth journeys.

## Executed checks

| Check | Result |
|---|---|
| Focused actual-client/component tests | **11 checks ×20 runs PASS** |
| Existing actual-client auth regression | **13 checks ×20 runs PASS** |
| Browser harness | **22 groups PASS**, including three single-/two-tab owner-recovery repetitions |
| Real role/ownership denial | SCOUT→OWNER guard, OWNER→ADMIN guard, real OWNER→another owner's venue403 all PASS |
| Typecheck / lint / build | PASS; no lint warnings/errors |
| Frontend diff check | PASS |
| Backend source integrity | PASS; source/resource/pom hashes unchanged |

The focused suite covers403 before/during/after refresh, coalesced probes, unchanged-session forbidden, repeated forbidden/unauthorized results, no mutation replay, pending logout/account switch, contextual copy and actual RoleGuard/AdminLoadState branches. The existing suite retains refresh failure, malformed/network outcomes, cross-tab winner/loser, subsequent genuine failure and stale-success regressions.

Evidence: [focused tests](evidence/auth/owner-recovery/client.json), [existing regression](evidence/auth/owner-recovery/legacy-client.json), [browser](evidence/auth/owner-recovery/browser.json), [build/source verification](evidence/auth/owner-recovery/verification.json), [recovered owner page](evidence/auth/owner-recovery/owner-recovered-3.png), [real forbidden owner page](evidence/auth/owner-recovery/owner-genuine-forbidden.png). Configured local runtime logs passed the existing generated-secret scan. No raw credentials were written to evidence.

## Repeat execution

From `Eema-frontend`:

```sh
node scripts/qa/owner-auth-recovery.cjs
node scripts/qa/auth-client-regression.cjs
npm run typecheck
npm run lint
npm run build
git diff --check
```

For browser execution, follow the local startup/Chrome instructions in [AUTH execution report](AUTH_EXECUTION_REPORT.md), then run `node scripts/qa/owner-auth-browser.cjs --local-qa` from the frontend. It reuses the backend-owned browser harness without editing it, runs recovery three times, and writes to `docs/qa/evidence/auth/owner-recovery/`. Keep the existing seed venue available for real cross-owner denial. No seed deletion or full reset is required.

The legacy client adapter supplies Web Crypto to the old VM harness; its assertions remain unchanged. Use this frontend entry point with the new generation implementation rather than the old standalone backend harness without a Web Crypto adapter.

**Local AUTH is green with no remaining known local launch defect.** Actual email delivery, deployment/edge behavior, staging load validation and human UX judgment remain separate outstanding checks. Existing client-only logout and access-JWT lifetime contracts are unchanged, not newly marked defects.
