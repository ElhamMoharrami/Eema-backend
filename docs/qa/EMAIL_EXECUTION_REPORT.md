# EMAIL execution report

**Current result: EMAIL-001–031 — 31 PASS / 0 FAIL / 0 BLOCKED locally. No unresolved defect or decision.**

Initial source audit before EMAIL changes. EMAIL-001–031 are NOT RUN in the master plan and have no dedicated execution report. CALA is locally complete; PROVIDER-001–005 remain BLOCKED — STAGING/EXTERNAL with their original report preserved. This domain concerns implemented message composition and transactional notification safety, not inbox delivery.

## Inventory and authoritative contract

001–002: account verification/reset. 003–015: booking requests, payment, cancellation and refund. 016–019: inquiry lifecycle. 020–022: owner verification. 023–027: venue moderation. 028–030: payout destination review. 031: explicitly unsupported notification events.

Read AccountEmailNotificationService, BookingNotificationService, AvailabilityInquiryNotificationService, TrustNotificationService, lifecycle listeners, NotificationOutboxWriter and their existing tests. Current OPS-DEC-001 supersedes old AFTER_COMMIT asynchronous delivery: templates are captured before commit, immutable intents are persisted atomically, and the bounded dispatcher later uses mocked transport locally. Recipient slots and stable event identity fence replay; independently repeated legitimate events remain distinct. Auth outbox records contain a token placeholder, with eligibility checked before reconstruction and delivery.

Booking recipients use contact snapshots. Inquiry/trust/auth preparation uses the current associated account, captured immutably at the event. Owner cancellation notification exists only for Scout-initiated cancellation. An owner/admin cancellation still notifies the Scout. A cancelled refund-completed message requires REFUNDED; late-payment mail says initiated, not settled. Templates must not claim an owner payout transfer or expose destination IBAN/encryption material.

Audit correction: EMAIL-010 production and master plan both use “Your Eema booking is confirmed”. An existing failure-injection test still stubbed the obsolete “Payment received for your Eema booking” subject, so it did not exercise Scout delivery failure. Correct the test, not production. The preliminary subject mismatch inference was wrong. EMAIL-007 expiry includes response deadline or minimum lead time, not necessarily owner silence. EMAIL-014 points to authoritative payout details without claiming transfer or an exact final net payout. Verification cannot require an already verified account; prerequisites and denied mutations belong to their real domain. EMAIL-031 is an existing coverage limitation, not permission to introduce new communications.

## Dependencies, strategy and missing coverage

Dependencies: AUTH, BK, INQ, PAY, CANCEL, REF, TRUST, VEN, DEST, OPS and UXACT reports. CALA's just-completed full regression executed 1,014 PostgreSQL cases with zero failures, including 19 OperationsNotificationIntegrationTest cases for real commit/rollback, duplicate events, immutable replay, leasing and worker claims. All prior domain authorization and denied-side-effect cases ran again. These unchanged production/PG paths will be referenced, not copied into new template tests.

Existing unit tests cover most subjects, deadlines and booking snapshot recipients. Add focused missing coverage for all destination templates, unsupported trust events, actor-specific cancellation routing, Unicode/markup escaping across templates, and truthful money-state wording. Inspect actual CTA route reachability and reuse prior authenticated deep-link/browser evidence where it proves the same route. No actual Resend calls, new event types, migration or email UX feature is authorized by this test inventory. No production defect has yet been established.

## WHAT ELHAM SHOULD TEST PERSONALLY

Assess subject clarity, cancellation versus refund-completion wording, timezone/deadline readability and whether the management links make sense on mobile. Real delivered inbox presentation belongs to staging.

## PRODUCT / OPERATIONS DECISIONS

None currently required to validate existing behavior. Additional notification types are product backlog choices and will not be invented here.

## STAGING ONLY

Real Resend sender/domain configuration, inbox/spam delivery, deployed HTTPS links and mailbox rendering remain PROVIDER-002. Local capture proves content and routing only. Real screen-reader acceptance remains deferred.

## Initial focused execution / harness corrections

Added 35 deterministic template invocations in EmailTemplateContractTest. The first 71-case focused run had 17 assertion failures and 7 environment errors: Spring's valid HTML entity encoding of accented characters differed from a literal-byte assertion, and loopback HTTP listeners were denied by the un-escalated sandbox. The tests now require decoded Unicode text preservation while independently forbidding raw `<img` markup; transport tests run with the authorized local socket permission. No production code was changed to satisfy either issue. Sanitized case history is retained under `evidence/email/history/initial-focused.json`.

The existing Scout-delivery-failure test was corrected to stub the actual current subject and now explicitly verifies that Scout send attempt as well as the subsequent Owner attempt. This fixes a vacuous test; the existing service already contains the intended independent-recipient behavior.

Browser harness corrections are also preserved in `history/browser-before.json` and `history/browser-second.json`. Initial readiness probes expected literal title casing or a venue name in body text even though it lives in an input. Token mocks omitted the required MessageResponse.message field, correctly triggering existing client response validation. The last venue timeout was independently inspected in Chromium: the complete editor and enabled Save timezone button were present, while CSS exposed uppercase text through innerText. Readiness now uses the actual DOM text content. These were harness assumptions, not production defects.

## Scenario-to-executed-test mapping

| Scenarios | Actual template evidence | Transaction / lifecycle evidence reused |
|---|---|---|
| EMAIL-001–002 | accountLinksEncodeTokensAndUseCurrentRecipient (2 cases); existing account templates | AUTH registration/recovery; OPS auth token eligibility, placeholder and expiry |
| EMAIL-003–009 | requestTemplatesPreserveSnapshotRecipientsEscapeUnicodeAndUseExistingBookingRoute (6 types, submitted has 2 recipients); existing lifecycle/deadline cases | BK, EDGE and JOB HTTP/PG transitions; OPS duplicate/rollback and recipient slots |
| EMAIL-010–012,015 | confirmedAndLatePaymentAndCompletedRefundHaveDifferentMoneyClaims; existing confirmation, late-payment and refund templates | PAY, CANCEL and REF authoritative payment/refund transitions |
| EMAIL-013–014 | cancellationRoutesByActorAndDoesNotClaimRefundCompletion (3 actors), nonRefundableCancellationNeverClaimsMoneyIsBeingReturned | CANCEL actor/policy/rollback/replay matrices |
| EMAIL-016–019 | inquiryMessagesUseCurrentRecipientAndEscapeAllUntrustedContent (4 types); existing deadline tests | INQ committed offer/decline/expiry; OPS event identity/atomicity |
| EMAIL-020–030 | trustTemplatesUseManagementRouteAndNeverIncludeBankFields (11 types); existing trust reason/route checks | TRUST, VEN, DEST legitimate lifecycle, denied mutation and audit rollback |
| EMAIL-031 | unsupportedTrustEventsHaveNoDedicatedEmail (7 types); cancelled-refund state guard and actor-specific routing | INC transitions, DEST replace/reveal/snapshot, PAYOUT paid recording and REF origin branches; source audit confirms no dedicated notification publication |

EMAIL-031 does not assert that new emails are required. IncidentService and OwnerPayoutService write audit events without publishing mail lifecycle events; destination replacement/reveal/snapshot have audit-only event types. RefundCoordinator publishes CancellationRefundCompletedEvent only for cancellation-origin refunds, and late-payment completion has its separate audit event. Current domain transition tests ran in the fresh PostgreSQL regression; local template tests independently prove unsupported trust types have no composer. Adding those communications remains separate product work.

## Completion and regression

All 71 focused cases pass, including 35 new template invocations. All nine actual-browser CTA variants pass: Scout/Owner booking detail; Owner/Scout inquiries; owner verification; venue editor; payout destination; encoded verification and reset links. Protected routes retain their exact path through inline sign-in and reload. Token links forward the decoded synthetic token correctly. These are real frontend/browser checks with intercepted API fixtures, not a claim of real identity changes, delivered mail or backend persistence. The isolated server and browser were stopped.

Fresh `mvn test` and `mvn clean package` each passed 1,113 cases with no skips/failures/errors. CALA’s immediately preceding complete PostgreSQL regression (1,027 reported, 1,014 executed, 13 opt-in browser skips), plus its focused 14-case context-cleanup rerun, is explicitly reused: all production and integration source hashes were verified unchanged. Frontend typecheck/lint/build are likewise reused from unchanged CALA production sources. No second full PostgreSQL run or new frontend build is claimed. Both diff checks pass. No application code or database migration changed in EMAIL.

Template assertions supplement rather than duplicate the real PostgreSQL domain/outbox transitions. Historical harness failures and the obsolete failure-injection subject are documented above. The master plan now records current expiry and payout-copy semantics and the approved transactional-outbox contract; PDF regeneration preserves all scenario IDs.

**Locally clear to proceed: YES. Next unfinished local domain: JOURNEY-001–009.** PROVIDER remains externally blocked; real screen readers remain deferred. Human review, product backlog boundaries and staging requirements remain as stated above.
