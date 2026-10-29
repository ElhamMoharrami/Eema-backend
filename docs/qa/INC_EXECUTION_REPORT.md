# INC execution report

**Completed 2026-09-20: INC-001–002 — 2 LOCAL PASS, 0 FAIL, 0 BLOCKED. INC-DEF-001 resolved. No unresolved P0/P1 or product/business/legal decision.**

Initial source audit and inventory recorded 2026-09-20 before implementation changes. INC-001–002 are the first unfinished domain: both master scenarios are NOT RUN and no previous incident execution report exists. Earlier reports and the complete master plan were read in this continuing validation session; DOC is now 4 PASS after its approved Unicode decision.

## Inventory and authoritative contract

| Scenario | Initial result | Required evidence |
|---|---|---|
| INC-001 — participants and plain text | NOT RUN | All seven categories, booking scout/owning owner/admin, unrelated and anonymous denial; descriptions blank/markup/4000/4001/newline/Unicode; persistence and audit; no financial or provider effects |
| INC-002 — lifecycle and isolation | NOT RUN | Admin-only OPEN → UNDER_REVIEW → RESOLVED → CLOSED; all other operations conflict; note validation; participant note privacy; real PostgreSQL concurrent transitions; authoritative browser refetch |

IncidentService, both incident controllers, request/response DTOs, IncidentReport, IncidentReportRepository, SecurityConfig/JwtAuthenticationFilter, MarketplaceTrustAuditService and common V11 establish the contract. Authenticated active users only; the booking's scout, its venue's owning OWNER and ADMIN may create/list. No email verification, owner verification, policy acceptance, booking status or date prerequisite is implemented. No such additional product restriction is inferred from where the UI is placed. Admin reads are protected by global /admin security, mutations also check the role in the service.

Descriptions are required, trimmed at the outer edges, at most 4000 Java characters, and cannot contain angle brackets. Newlines, Unicode and literal non-HTML punctuation are permitted. Resolution notes have analogous validation with 2000 limit. The API validates raw length before trim. Participant DTOs always omit the internal note value, including admin access through the participant endpoint; only admin endpoints expose it. Notes are not copied into trust audit event text.

Every successful action appends one trust event in its transaction. Mutations lock the incident row; only OPEN→UNDER_REVIEW, UNDER_REVIEW→RESOLVED and RESOLVED→CLOSED exist. Invalid/repeated/skipped/reverse operations return 409. There is no edit/delete/reopen operation. Reporting has no deduplication key: separate accepted reports are separate records. No booking/refund/payout/liability/venue mutation or incident notification is implemented. V11 supplies FKs, category/status and description checks, and required resolution fields; note content validation is the API contract, not an additional SQL constraint.

Frontend audit: BookingIncidents reports then refetches its list; admin detail mutates then refetches, and refetches on 409 with explicit conflict feedback. Admin queue has status/category/booking filters. React renders values as text. Booking participant UI exists for SCOUT/OWNER; administrator intake is API-supported without a new UI requirement.

## Dependencies and coverage strategy

AUTH provides current-account and endpoint roles; POL establishes that acceptance is endpoint-specific; BK establishes booking participation and fixture creation. PAY, CANCEL, REF and PAYOUT provide authoritative financial state and side-effect baselines. DOC is the completed preceding domain. Reuse existing IncidentReportTest (two entity tests), shared real PostgreSQL/auth fixtures, security and prior booking/financial suites. Missing incident HTTP/DB boundary, race, atomicity and representative browser coverage will be added. Independent transactions and deterministic barriers will prove a single transition/audit winner. Injected audit failure will prove rollback. No production change is planned absent an independently reproduced defect.

## Local versus staging

Run focused PostgreSQL/browser checks, then required full backend regression and package; run frontend typecheck/lint/build. All fixtures use disposable PostgreSQL and mocked Mollie. Real deployment, provider delivery and multi-instance operation are not inferred from local checks. Incident emails do not exist and are not a staging requirement.

## WHAT ELHAM SHOULD TEST PERSONALLY

Assess whether reporting copy clearly distinguishes support intake from cancellation/refund/responsibility decisions, whether internal-note terminology is clear to operations, and whether the report form is comfortable on mobile.

## PRODUCT / OPERATIONS DECISIONS

No new decision identified in the initial audit. Existing legal/policy launch approvals remain outside this technical domain.

## STAGING ONLY

Deployed authentication/edge behavior and operational monitoring. No external-provider action belongs to incident handling.

## Focused execution

41 PostgreSQL cases passed on a fresh V1–V13 database with Hibernate schema validation. Seven category cases each exercise scout, owning owner and admin (21 accepted reports). Eight booking states accept operational intake without altering booking state. All twelve state/action combinations are covered by the lifecycle and illegal-transition cases. Nine deterministic same-action race pairs (three repetitions of start, resolve and close) each produce exactly one 200 and one 409, with one new audit event. Independent HTTP calls create separate service transactions/connections.

The suite also proves description/note limits and markup rejection, unrelated/anonymous/inactive denial, removal of verification/policy prerequisites, admin endpoint role protection, participant note redaction, bounded filtered listing, missing-record handling, foreign-key/status/resolution-shape constraints, and transaction rollback when trust audit fails. A lifecycle with populated payout and liability rows preserves their full snapshots, booking/refund fields, venue fields, booking audit and policy references. No Mollie call or booking lifecycle event is emitted.

Reused coverage: IncidentReportTest's two state-machine tests and the full existing booking/financial/security suites. Added IncidentIntegrationTest (41 cases), opt-in IncidentBrowserIntegrationTest, and the existing CDP harness pattern in scripts/qa/inc-browser.cjs. No production code, database migration or frontend source was changed for this execution.

Harness history: the initial 40-case run failed because test SQL named the user column is_active instead of active. Its cleanup also failed, leaving the shared test scout unverified and causing later fixture-creation 403s. Corrected only the fixture SQL, added explicit database constraint coverage and populated financial fixtures, and reran on a fresh database. History/focused-first.json preserves the unsuccessful run. No product defect was inferred from those setup failures.

Final browser/full-regression results remain pending; this focused result alone does not mark the domain complete.

## INC-DEF-001 — frontend incorrectly blocks eligible support intake (P1)

The first browser run showed “Verify your email before continuing” and no incident POST. The shared API client treated every authenticated non-GET mutation outside admin/matching as verified-email-only, contradicting the incident service's existing participant contract. Independently reproduced using the actual client/token TypeScript modules and deterministic transport (history/client-before.json). The browser's original fixture had current database verification but stale unverified session metadata, demonstrating the same incorrect client authority; final regression deliberately uses an actually unverified scout with no policy acceptances.

Fixed only the shared client's exact POST /bookings/{id}/incidents predicate. Backend remains authoritative for actor/booking access and input validation. Booking/venue mutations retain their existing verification guard. Seven focused client checks pass, including backend-denial propagation; the existing AUTH client suite also passes. Browser failure/screenshot retained under history. The initial browser's “markup error” check was insufficiently specific: final harness additionally requires an actual 400 incident POST, then a 201 valid POST, so a client gate cannot masquerade as validation coverage.

This supersedes the initial no-production-change statement: the sole product change is the narrow frontend client exception. No backend production or schema change is required. Browser and full rebuild/regression after the fix remain pending.

## Final validation and continuation

| Gate | Result |
|---|---|
| Focused PostgreSQL | 41 PASS; fresh V1–V13 migration and Hibernate validation |
| Concurrency | Nine pairs, each one successful transition and one conflict; one audit event |
| Real browser | Eight PASS; launcher and persisted CLOSED/no-side-effect assertions PASS |
| Focused client | Seven PASS |
| AUTH client regression | Thirteen PASS |
| mvn test | 1029 PASS |
| mvn verify -Ppostgres-integration | 890 reported, 880 executed PASS, 10 opt-in browser skips; zero failure/error |
| mvn clean package | 1029 PASS; packaged JAR produced |
| Frontend typecheck / lint / build | All PASS after the client fix |
| Both repository git diff --check | PASS |
| Master plan / PDF | Updated; 635 pages, 703 scenario IDs retained in identical order; changed pages 90–92 visually checked |

Browser screenshots confirm readable Unicode plain text, visible conflict feedback, correct closed state and note privacy. Temporary browser credentials/profiles were removed by the harness. No real provider was contacted. The frontend exception is the only production change; prior working-tree changes were preserved. Historical failure evidence is retained rather than overwritten.

**Final result: INC-001 PASS; INC-002 PASS.** One product defect found and fixed (INC-DEF-001, P1); zero unresolved defects. The prior pending-result statements are historical checkpoints superseded by this section. Human review, product/operations and staging boundaries above remain applicable. No new decision or migration ambiguity prevents continuation.

**NEXT UNTESTED DOMAIN: ADMIN-001–011.** Covers administrative queues, authoritative booking/financial detail, operational exceptions and admin UI/actions. Proceed automatically under the user's explicit higher-autonomy instruction, with a fresh source audit and report before changes.
