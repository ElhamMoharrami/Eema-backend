# TRUST execution report

Inventory and source audit recorded before TRUST code changes, 2026-09-12. Execution completed; results below. Prior AUTH and POLICY changes in the working tree are preserved. Scope is owner review only; other domains retain their behavior.

## Discovery inventory

| ID / title | Priority | Generated errors | Actual actors / prerequisites | Existing automation | Environment / human limit |
|---|---|---|---|---|---|
| TRUST-001 — submit, duplicate, reject, resubmit | P0 | Generic scout/owner ownership variants; policy prerequisite; copied API/DB claims; provider silence on rejection; fake snapshot restoration | Active OWNER; verified email for submit, not GET. ADMIN reviews. No policy acceptance prerequisite for either actor | OwnerVerificationServiceTest (two mock tests); TrustNotificationServiceTest approval/rejection email templates | Disposable PostgreSQL + browser; operational review checklist and real email delivery remain separate |
| TRUST-002 — actor and state restrictions | P0 | Generic roles, required admin acceptance/verification, irrelevant ownership UUIDs, repeated assertions | Owner endpoint has no selectable owner UUID; SCOUT/ADMIN denied. ADMIN review endpoints have controller and global /admin security. Only PENDING reviewable | Mock verified-owner conflict; security annotation tests; AUTH current-DB identity/verification coverage | PostgreSQL/HTTP + browser; no manual DB work |
| TRUST-003 — methods and note boundaries | P0 | Generic prerequisites/provider/cleanup wording; methods could be mistaken for automated identity evidence | ADMIN approves independent pending records using required enum; optional submit/approve notes ≤1000, required rejection reason ≤1000 | VIDEO_CALL mock approval; partial trust notification tests | Parameterized HTTP/DB tests; meaning of manual evidence methods requires operations input |

## Authoritative model before changes

- Logical UNVERIFIED (GET synthesizes `id=null`; no row) → submit → PENDING → approve → VERIFIED, or reject → REJECTED → resubmit → PENDING. Existing persisted UNVERIFIED is schema-valid but not created by normal GET. VERIFIED has no revoke/downgrade endpoint.
- Duplicate PENDING submission returns the existing result without changing note/timestamp or emitting audit/notification events. VERIFIED submission conflicts. Resubmission reuses ID, replaces owner note/submission time, clears review note/method/reviewer/review time. `@Version` tracks entity updates; existing review queries take pessimistic row locks.
- Owner DTO: id, ownerId, status, submittedAt, reviewedAt, reviewNote, verificationMethod. It intentionally omits ownerNote, reviewer ID, created/updated timestamps and persistence version. Review notes are owner-visible, including approval notes in the API; the current owner UI displays the note only for REJECTED. There is no internal-secret note field.
- Admin DTO additionally contains ownerNote, reviewedBy UUID, createdAt and updatedAt; no user email/password/token or evidence files. Queue filters status/ownerId; page size capped at 100, sort discarded by AdminPagePolicy.
- Submission: OWNER role and authoritative current active/email-verified user. GET allows unverified OWNER. ADMIN is denied the owner endpoint even though VerifiedUserPolicy exempts admins from email verification elsewhere. Admin reviews need neither email verification nor policies. JWT filter loads current DB user state; token claims do not override it.
- Submit note and approval note: optional, null/blank normalized to null, maximum 1000 Java characters. Rejection reason: required nonblank, maximum 1000; trimmed. Method required: MANUAL_REVIEW, VIDEO_CALL, BUSINESS_DOCUMENT, VENUE_CONTROL_EVIDENCE. Record DTOs safely ignore unknown fields; no entity mass assignment.
- Audit records SUBMITTED, REJECTED, SUBMITTED, APPROVED for the full lifecycle, with OWNER/ADMIN actor and verification subject. Only rejection carries a reason in the audit/event; earlier owner/approval notes are not separately copied into audit history.
- Each legitimate transition publishes TrustNotificationEvent; an asynchronous AFTER_COMMIT listener calls TrustNotificationService, which uses EmailService. Disabled email skips provider delivery; listener catches runtime failures after commit. Duplicate/invalid/denied actions must not publish lifecycle notifications. Real Resend delivery is staging-only.
- V9: one row per owner; owner/reviewer FKs; enum status, method check, status/review timestamp+reviewer consistency check; version; queue index. The database does not enforce every semantic method/reason combination; service transitions do.
- Storage contains notes/method/timestamps only. No passport, face, government ID, business-document upload, bank statement, KYC/KYB provider, or identity/bank ownership record is part of this workflow. Approval does not call venue or payout mutation services. UI describes manual Eema marketplace review, explicitly distinguishes it from automated KYC, and discourages sensitive documents.
- Frontend owner success uses Spring's returned DTO (not optimistic state invention); admin success and conflicts refetch GET. No requirement to add a redundant owner GET solely to replace an authoritative POST response. Potential UI defect to exercise: review-conflict error lives inside the PENDING-only form and may disappear after refetch changes status.

## Isolation

Unique RUN_ID fictional owners; Testcontainers PostgreSQL for HTTP/persistence/concurrency; dedicated disposable local PostgreSQL and Chrome profile for browser flows. No shared trust-history deletion or imaginary snapshot restore. No evidence file uploads. Sanitized evidence belongs in `docs/qa/evidence/trust/`.

## Initial audit suspicions (resolved below)

- Concurrent first submission may race through absent verification-row locks and return 500 on unique owner violation.
- Admin stale-decision message may disappear when the authoritative refetch removes the pending-only action form.

## Execution outcome — 2026-09-12

TRUST-001, TRUST-002 and TRUST-003 are locally PASS after two narrow defect fixes. Software scenarios: **3 PASS / 0 FAIL / 0 BLOCKED**. External delivery/deployment and operations approval are separate release follow-ups, not software passes. No unrelated booking, pricing, payment, payout, policy, venue or moderation behavior was changed by TRUST work.

### Defects reproduced and fixed

| ID | Severity | Expected / actual / reproduction | Impact and fix |
|---|---|---|---|
| TRUST-D01 | P1 | Two first submissions must return the same PENDING record. A barrier forced both transactions to read no verification row; one returned HTTP 500 on unique owner constraint violation in all three original runs. | An ordinary duplicate submission could fail despite the idempotent contract. Lock the existing owner row before verification lookup/creation. Admin review now locks only the verification, avoiding joined owner-lock inversion. An intermediate fix exposed a reject/resubmit deadlock, caught by the new test and corrected before completion. No exception swallowing or weakened constraint. |
| TRUST-D02 | P2 | A stale admin review must show an actionable conflict and refetch. Browser: open approval, concurrently reject by API, confirm stale approval; 409 refetched REJECTED but its error message vanished with the PENDING form. | Reviewer could not tell why their decision failed. Move the alert outside the PENDING-only form; status still comes from GET. Repeated real stale decisions now preserve the alert. |

Before evidence: [PostgreSQL reproduction](evidence/trust/concurrency-before.json), [UI reproduction](evidence/trust/ui-defect-before.json), [before screenshot](evidence/trust/before-stale-review.png). After evidence: [PostgreSQL](evidence/trust/postgres.json), [browser checks](evidence/trust/browser.json), [stale review screenshot](evidence/trust/admin-stale-review.png).

Harness corrections were not product defects: JDBC returns Timestamp and SQL array wrappers require canonical comparison; browser waits must allow React rendering/document navigation; controlled HTTP failures must not replace CORS OPTIONS preflight. The backend browser jar is copied outside target so Maven packaging cannot replace a running jar.

### Authorization matrix

| Request | OWNER verified email | OWNER unverified email | SCOUT | ADMIN | Anonymous |
|---|---:|---:|---:|---:|---:|
| GET /api/owner/verification | 200 | 200 | 403 | 403 | 401 |
| POST /api/owner/verification/submit | 200 in allowed state | 403 | 403 | 403 | 401 |
| GET admin queue/detail | 403 | 403 | 403 | 200 (404 missing detail) | 401 |
| POST admin approve/reject | 403 | 403 | 403 | 200 PENDING; 409 non-PENDING; 404 missing | 401 |

Inactive account returns 401. Current DB email changes were tested in both directions using the same previously issued JWT. Admin fixtures intentionally had unverified email and no policy acceptance. A verified owner cannot self-downgrade: submit returns 409, preserving method/time/version and effects.

### Lifecycle, methods and validation results

- Initial GET is synthetic UNVERIFIED, null ID, zero records. Submission creates one PENDING record with current submittedAt; duplicate preserves complete row and emits nothing additional.
- Rejection persists trimmed owner-visible reason, admin UUID/time and null method. Resubmission reuses the ID, updates owner context/time and clears review fields. Approval persists VERIFIED, method, admin/time and optional owner-visible note. Full sequence increments persisted version 0 → 1 → 2 → 3.
- Parameterized independent owners proved all four VerificationMethod values. Missing/null/unknown method is 400.
- Submit note: null, empty, blank, one accented character and 1000 accented characters accepted/normalized; 1001 rejected. Approval note: null/blank/1000 accepted, 1001 rejected. Missing/null/empty/whitespace/1001 rejection reason rejected; valid and 1000 accepted. Validation occurs before service mutation; invalid pending reviews preserve fields, audit count and notification count.
- Unknown submit status/ownerId and admin status/ownerId/reviewedAt fields cannot assign entity state. Record DTO binding ignores them. No broad strict-JSON change was introduced.
- Owner/admin DTO field sets and admin queue filters/page cap were verified over HTTP. Unsupported logical no-row review is addressed only by nonexistent UUID (404); a schema-valid persisted UNVERIFIED fixture proves 409 without inventing a normal endpoint for creating it.

### PostgreSQL invariants and concurrency

PostgreSQL 16 Testcontainers, real Spring HTTP/security/service transactions and Flyway migrations; no H2. The complete integration run passed 85 cases. TRUST contributed 30 invocations and passed a second focused 30-case run with explicit cross-thread lifecycle-event capture.

Each race group repeats three times per run; competing review covers both approve/approve and approve/reject in each repetition. Barriers coordinate transaction boundaries; no timing sleeps determine race outcomes.

| Race | Authoritative outcome |
|---|---|
| First submit / first submit | 200 + 200; same ID, one PENDING row, one submission audit and notification event |
| Approve / approve | 200 + 409; one review, one review event, version 1 |
| Approve / reject | 200 + 409; one consistent VERIFIED or REJECTED result; no mixed method/reason/reviewer/time |
| Reject / submit | Both 200: duplicate submit precedes rejection (final REJECTED), or rejection precedes legitimate resubmission (final PENDING with cleared review fields). Audit sequence agrees with serialization. |

Unique owner, owner/reviewer FKs, enum status/method and status/review-field checks reject invalid SQL without disabling constraints. Queue index and optimistic version are present. The service enforces semantic method/reason combinations beyond the schema's checks. Existing DRAFT/PENDING venue rows remain byte-equivalent as JSON after owner approval; no owner payout destination or payout is created. There is no KYC/bank-evidence entity/provider to call in this workflow.

### Audit and notification results

Full lifecycle audit is SUBMITTED, REJECTED, SUBMITTED, APPROVED in chronological order, correct OWNER/ADMIN actors and OWNER_VERIFICATION subject. Duplicate, unauthorized and invalid review commands leave counts unchanged. Only rejection reason is copied into trust audit and lifecycle event; owner/approval notes remain current entity fields rather than a newly invented evidence archive.

A real asynchronous AFTER_COMMIT listener observed a committed record, called the disabled EmailService, and a deliberately failing approval notification left VERIFIED and audit committed. Unit coverage proves disabled EmailService never calls Resend. Template tests cover submission/approval/rejection, owner-management link and HTML escaping of rejection text. These are software delivery-path checks, not evidence of actual inbox delivery.

### Browser and build results

Existing Chrome/CDP + ws harness, dedicated tmpfs PostgreSQL, fictional example.test accounts and isolated profile; no new browser framework.

15 browser check groups pass: real owner login; synthetic UNVERIFIED/submit/PENDING/reload; invalid blank rejection 400; admin rejection and owner-visible reason/resubmission; approval confirmation and VERIFIED/reload; wrong-role guards; three stale-review repetitions; real missing-record 404; controlled 401, 403, 500 and network failure rendering; unverified-email instruction with no row created. Failure injection is explicitly limited to transport/error rendering; lifecycle and stale decisions use real Spring and PostgreSQL. Screenshots contain fictional notes/IDs only.

| Check | Result |
|---|---|
| mvn test | PASS, 227 tests, zero failures/errors/skips |
| mvn verify -Ppostgres-integration | PASS, 85 tests, zero failures/errors/skips |
| Focused repeated TRUST PostgreSQL suite | PASS, 30 invocations including three repetitions per race group |
| mvn clean package | PASS, 227 unit tests and packaged jar |
| npm run typecheck | PASS |
| npm run lint | PASS, no warnings/errors |
| npm run build | PASS |
| git diff --check, both repositories | PASS |
| Browser | PASS, 15 groups including three stale decisions |
| Master-plan Markdown/PDF | Corrected TRUST contract; regenerated using existing renderer |

Raw logs and tokens are not committed. [Checks summary](evidence/trust/checks.json) and [rerun guide](TRUST_QA_GUIDE.md) accompany sanitized browser/PostgreSQL evidence.

## WHAT ELI SHOULD TEST PERSONALLY

- Is email verification clearly different from manual owner review?
- Does PENDING explain what happens next, and is rejection/resubmission respectful and actionable?
- Does VERIFIED reassure without implying regulated identity/KYC/bank verification?
- Are admin decisions deliberate, and is the stale-review explanation understandable?

## STAGING TRUST TESTS STILL REQUIRED

- Real submission/approval/rejection emails: recipient, sender, copy and HTTPS owner-management links.
- Deployed owner/admin authorization and routing/CORS with actual staging accounts.
- Production-like V9 migration/constraints and deployment logs that exclude sensitive review data.
- If deployed with multiple instances, run the same competing decisions against separate instances sharing PostgreSQL.

No external KYC test applies: this workflow has no KYC provider. Local disabled-email PASS does not close delivery checks.

## PRODUCT / OPERATIONS DECISIONS STILL REQUIRED

- Operational meaning/checklist for MANUAL_REVIEW, VIDEO_CALL, BUSINESS_DOCUMENT and VENUE_CONTROL_EVIDENCE.
- Review SLA, escalation and what owners should expect while PENDING.
- Note/evidence retention requirements; current product stores notes only and discourages sensitive documents.
- Whether approval notes should also be displayed in owner UI; API currently makes reviewNote owner-visible, and admin forms label it accordingly.
- Whether verified owners can ever be revoked/downgraded; no such operation currently exists.

No misleading regulated KYC, legal identity, bank verification or automatic venue activation claim was found in the audited workflow. Software tests do not approve an operational identity policy. **TRUST is locally clear to proceed to the next domain**, with the above staging and product/UX follow-ups retained.
