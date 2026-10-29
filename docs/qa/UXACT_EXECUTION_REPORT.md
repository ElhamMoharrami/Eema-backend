# UXACT execution report

Executed 2026-09-21: **UXACT-001–019: 19 PASS / 0 FAIL / 0 BLOCKED locally.** UX is now locally complete under the explicit private-alpha scope decision; actual AT remains deferred and not executed.

## Inventory and source audit

UXACT-001–014 cover repeated booking/inquiry creation, owner accept/decline, checkout/cancel/incident, policy acceptance, venue/owner verification submissions, payout destination, admin destination decisions, manual payout recording and liability settlement. UXACT-015–019 cover session expiry in discovery, owner editing, checkout, admin operations and private-document download.

Frontend source uses busy/disabled state and authoritative reload in existing action components. Auth transport coalesces refresh, retries an unauthorized operation once, and rejects responses after an actor switch; cancellation also fences request generations. Audit each actual control rather than assigning every action to Scout/Owner. Admin-only operations require ADMIN; support intake has its documented participant exceptions. Exact endpoint/state contracts and prior PostgreSQL suites remain authoritative.

## Corrected assumptions / test strategy

Use explicitly held/released browser requests, not a fixed sleep as proof of concurrency. Separate repeated input while pending from a deliberate retry after an unknown network outcome. Creation endpoints do not advertise universal request idempotency; do not invent one or equate an additional REQUESTED record with duplicate money movement. Any genuine ambiguity or unacceptable duplicate outcome must be classified explicitly. Existing row locks, uniqueness and payment/provider fences are proved by domain integration results, not simulated browser responses.

Dependencies: AUTH, BK, INQ, PAY, CANCEL, INC, POL, TRUST, READY, VEN, DEST, PAYOUT, ADMIN, DOC, SEC, RACE and UX execution reports. Their matching database/authorization/concurrency cases were reused; focused pending-input, response-loss and session browser/client coverage was added. No live providers, new financial workflow, schema change or accessibility removal were introduced.


## Reproduced defect UXACT-DEF-001

P0 session/financial-action association risk: after the original 401 is observed, an in-flight refresh can be superseded by a different account login. The original client then retries under the new account. Actual TypeScript client/token execution reproduces one cross-actor request in each of five paths, including checkout/admin mark-paid and private receipt. Backend authorization remains authoritative, but a valid new actor (especially another Admin) could execute an action they did not initiate. No real provider or money was used in reproduction. Historical client-before.json preserves the failures.

Fix: assert the original actor immediately after awaited refresh, before any replay. Same-user rotation remains supported; different-user login/logout rejects the original operation without changing the new session. Focused client and AUTH regressions pass after correction; independent rebuilt-browser confirmation and all required gates now pass. **RESOLVED**: one original request, zero cross-actor replay, new account preserved. Historical failing browser and client results remain under `evidence/uxact/history/`.

## Scenario mapping and corrected contract

| ID | Action / actor | Persistent safety evidence |
|---|---|---|
| UXACT-001 | Booking request / Scout | New `UxActionReplayIntegrationTest.bookingCreationReplayCreatesRequestsButOnlyOneCanReserve`: explicit replay creates two REQUESTED records and their own notices, no reservation until one acceptance; competitor becomes UNAVAILABLE. |
| UXACT-002 | Inquiry / Scout | New `inquiryCreationReplayCreatesOpenRecordsWithoutReservations`: two OPEN records, one lifecycle event each, no booking/calendar mutation. |
| UXACT-003 | Accept / owning Owner | BK `actorBoundaryAndExplicitAdminDelegation`, `halfOpenCompetitorsOnlyBecomeUnavailable`; INQ serialized acceptance evidence. |
| UXACT-004 | Decline / owning Owner | BK `declineRejectsEveryNonRequestStateWithoutEffects`: terminal replay denied, no second transition. |
| UXACT-005 | Checkout / booking Scout | PAY `createdRetryReusesSavedCheckout`, `ambiguousCreationReplayBoundary`, `duplicateConcurrentPaidCallbacksHaveOneEconomicEffect`. |
| UXACT-006 | Cancel / authorized participant | CANCEL `staleTermsConflictAndReplayCannotChangeEconomics`, `acceptedRefundResponseLossOrPersistenceFailureResumesFromDatabase`; browser representative is unpaid withdrawal, existing CANCEL covers paid cancellation. |
| UXACT-007 | Incident / authorized participant | New `incidentCreationReplayCreatesSupportRecordsWithoutFinancialEffects`; INC participant/denial and audit atomicity coverage. Separate support submissions remain possible. |
| UXACT-008 | Current policy acceptance / authenticated user | POL `duplicateConcurrentAcceptanceCommitsBothRequestsWithoutPartialSet`: one acceptance per user/version. |
| UXACT-009 | Venue review submission / owning Owner | New VENUE `replayedSubmissionHasNoSecondLifecycle`: DRAFT→PENDING, retry409, exactly one audit/event. |
| UXACT-010 | Owner verification submission / Owner | TRUST `simultaneousFirstSubmissionIsOneCommittedLifecycle`; repeated pending submission returns the existing lifecycle. |
| UXACT-011 | Payout destination / Owner | DEST `concurrentSameOwnerSubmissionIsConflictNot500`: one pending destination, conflicting replay. |
| UXACT-012 | Destination verify AND reject / Admin | DEST `sameRecordVerifyRejectHasOneWinner`, `concurrentDistinctVerificationSerializesOwnerRouting`; both browser controls exercised. |
| UXACT-013 | Record external payout / Admin | PAYOUT `concurrentPaidRecordingIsIdempotentOrConflicts`, `paidRecordingRollsBackWithItsAudit`; no bank transfer API exists. |
| UXACT-014 | Settle liability / Admin | PAYOUT `liabilitySettlementUsesStoredAmountAndSerializes`: stored amount, serialized transition. |
| UXACT-015 | Marketplace expiry / authenticated Scout | Actual common client successful/failed refresh/account-switch cases; browser expired search, safe reauthentication, reload. Public catalogue itself does not require authentication. |
| UXACT-016 | Owner editing expiry / owning Owner | Same client controls; browser edits timezone to UTC, rejects expired save, then reloads authoritative original timezone after login. Unsaved edit is discarded, never presented as saved. |
| UXACT-017 | Checkout expiry / booking Scout | Same client controls; browser sign-in guidance, explicit action restored after reload; PAY covers persisted checkout claims. |
| UXACT-018 | Admin operation expiry / Admin | Same client controls plus independent cross-account browser reproduction/fix; after expiration stale confirmation is unmounted. |
| UXACT-019 | Private receipt expiry / booking Scout | Actual binary client successful refresh and account-switch rejection, failed refresh browser, no unauthorized file download. DOC covers private-document authorization/issuance uniqueness. |

AUTH `refreshFailureMatrix` and `concurrentHttpRefreshHasOneWinner` supply the real token/database boundaries for 015–019. Browser expiration uses synthetic 401 responses; it is not a deployed auth or provider test. Existing endpoint authorization matrices and rejected-mutation side-effect assertions are reused, not mechanically duplicated with irrelevant actors.

Creation replay is a known explicit master-plan limitation: booking requests, inquiries and incident intake have no universal idempotency key. Ordinary rapid input is suppressed; after a lost response a deliberate retry can create a distinct request/support record and its corresponding legitimate lifecycle notices. These records do not themselves charge money, reserve inventory, settle liabilities or execute payouts. No global exactly-once submission claim or new idempotency architecture is introduced.

## Browser/client evidence and harness corrections

`browser-actions.json`: 75 passing checks, 15 action variants across 001–014. Each holds the actual request, dispatches physical double-click and Enter input, proves only one request, drops its response, proves no automatic retry, explicitly retries once and checks authoritative completion. Reload smoke checks are separate from the real database assertions above. Synthetic responses prove UI behavior, not provider/database results.

`expiry-browser.json`: 10 passing checks across 015–019. `session-browser.json`: original operation rejected, zero cross-account mutation calls, replacement account preserved. `client.json`: 30 passing cases, including five same-account refreshes, five account-switch rejections, fifteen network-failure no-replay checks, five failed-refresh sign-outs. Receipt success/switch cases use the binary adapter. Existing AUTH client regression: 13 passing cases; SEC client regression: 26; UX client regression: 56. Prior full UX accessibility/browser evidence is reused for the approved scope reclassification; no real screen-reader run is claimed.

Historical harness failures are retained: first payout fixture used booking payout enum PAID_MANUALLY instead of payout DTO PAID; retry needed reference re-entry because the form reloads; first receipt fixture omitted paid/confirmed metadata. The initial focused SQL assertion also treated recipient_slot as a role instead of an integer ordinal; it now checks both intended notification rows per request. Correcting these fixture/interaction/query assumptions required no production changes. Historical P0 evidence is separately preserved and must not be confused with harness failures.

## Database, migrations and providers

Real PostgreSQL integration uses isolated Testcontainers, fresh common migrations and Hibernate validation. Existing races use barriers/independent transactions; newly added tests target sequential explicit replay, not timing races. The full regression refreshed the OPS recovery and ADMIN backlog query-plan artifacts; their prior committed versions are preserved under `evidence/uxact/history/`. No schema or migration change. Mollie remains mocked; no live provider, email inbox, bank transfer or staging environment was exercised.

## Required regression results

| Command | Result |
|---|---|
| mvn test | PASS: 1078 reported, 1078 executed, 0 skipped, zero failures/errors |
| mvn verify -Ppostgres-integration | PASS: 1023 reported, 1010 executed, 13 skipped, zero failures/errors |
| mvn clean package | PASS: 1078 reported, 1078 executed, 0 skipped, zero failures/errors |
| focused PostgreSQL replay tests | PASS: 4 reported, 4 executed, 0 skipped, zero failures/errors |
| npm run typecheck | PASS |
| npm run lint | PASS |
| npm run build | PASS |
| `git diff --check` in both repositories | PASS |

Thirteen opt-in backend browser cases remain skipped in the standard PostgreSQL profile; the independently executed browser checks above are reported separately. No skipped test is counted as executed. The four additional PostgreSQL replay cases ran in the focused profile after the full suite.

## WHAT ELHAM SHOULD TEST PERSONALLY

Check whether sign-in recovery, loss of unsaved edits, external-transfer reference re-entry and unknown-outcome error wording are understandable on mobile. Do not manually reproduce the database races.

## PRODUCT / OPERATIONS DECISIONS

No unresolved decision or P0/P1 defect was found after the fix. Existing creation-replay limitations are explicitly characterized above; this is not a new universal idempotency promise. Real screen-reader acceptance remains deferred under the approved UX launch-scope decision.

## STAGING ONLY

Actual Mollie, Resend, Gemini, S3, deployed session behavior and provider fee/reporting access remain unverified by local mocks.

## Continuation

**Locally clear to proceed: YES.** Next unfinished domain: **PROVIDER-001–005**. Continue only with an explicitly supplied isolated staging/provider environment; local mock results cannot close that external requirement.
