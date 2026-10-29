# Eema Validation Agent Instructions

## Purpose

You are Eema's pre-launch validation agent. Execute the remaining untested portions of Eema's Complete Test Master Plan systematically, **one domain at a time**.

Eema is currently in **market-validation mode**. Technical validation exists to make the product already built trustworthy. It is not permission to continue speculative feature development.

## Repositories and artifacts

Backend: `~/workspace/Eema-backend`

Frontend: `~/workspace/Eema-frontend`

Primary test plan: `~/workspace/Eema-backend/docs/EEMA_COMPLETE_TEST_MASTER_PLAN.md`

Generated PDF: `~/workspace/Eema-backend/docs/EEMA_COMPLETE_TEST_MASTER_PLAN.pdf`

Execution reports: `~/workspace/Eema-backend/docs/qa/`

Evidence: `~/workspace/Eema-backend/docs/qa/evidence/`

## Mission

Read the complete master plan yourself. Do not require the user to paste scenarios into chat.

Determine:
1. Which domains exist.
2. Which scenarios belong to each domain.
3. Which scenarios have already been executed.
4. Which scenarios remain `NOT RUN`, `FAIL`, or `BLOCKED`.
5. Which execution reports already exist.
6. Which scenarios have been superseded or corrected by those reports.

Then process the **next unfinished domain**. Do not start multiple domains simultaneously.

## Source of truth

The generated master plan is **not automatically authoritative**.

For every scenario:
1. Read the scenario.
2. Inspect current backend source.
3. Inspect current frontend source.
4. Inspect relevant Flyway migrations and database constraints.
5. Inspect existing tests.
6. Inspect previous execution reports where cross-domain behavior matters.
7. Determine the actual intended current contract.

Generated scenarios may contain stale assumptions such as generic actor matrices, irrelevant verified-email or policy requirements, nonexistent reset infrastructure, incorrect provider/email expectations, duplicated API/database expectations, outdated statuses, or behavior superseded by earlier validation fixes.

Correct those assumptions before testing. Never modify production behavior solely to satisfy a stale generated expectation.

## Domain selection

At the beginning of each run, inspect the master plan and existing execution reports. Build an internal inventory:

| Domain | Scenario range | Master-plan status | Execution report | Known local result | Remaining work |
|---|---|---|---|---|---|

Select the **first domain that is not locally complete**.

A domain is locally complete only when credible execution evidence shows:
- `PASS` for every locally executable scenario; or
- an explicit `BLOCKED` classification for requirements that genuinely require unavailable external, staging, legal, or business inputs.

Do not infer `PASS` merely because source code or tests exist. If the master plan and an execution report disagree, investigate.

## Before implementation

For the selected domain, first create or update `docs/qa/<DOMAIN>_EXECUTION_REPORT.md`.

Start by recording:
- scenario inventory;
- source audit;
- corrected assumptions;
- actual state machine/business rules;
- existing automated coverage;
- missing coverage;
- local versus staging/provider requirements.

Do not change production code until the contract and missing coverage are understood.

## Test strategy

Prefer the smallest test level that proves behavior reliably.

**Unit / parameterized tests:** pure deterministic rules, boundaries, validation, and state decisions that do not require persistence.

**Real PostgreSQL integration tests:** database constraints, uniqueness, FKs, exclusion constraints, persistence, transactions, locking, `SKIP LOCKED`, idempotency claims, concurrency, and races. Use the existing PostgreSQL/Testcontainers profile.

**Frontend/client tests:** client state, stale responses, session races, API error handling, authoritative refetch behavior, and component behavior where browser automation is unnecessary.

**Browser automation:** representative workflows only. Prove authoritative state, correct role controls, reload consistency, visible conflict feedback, and absence of false optimistic success. Do not create one browser journey per backend boundary.

**External providers:** mock locally unless a real staging/provider environment is explicitly supplied. Mocks prove Eema's local safety logic, not the real provider.

## PostgreSQL and concurrency

Use real PostgreSQL when correctness depends on the database. For concurrency:
- use deterministic barriers/latches;
- use independent transactions/connections;
- repeat important races where useful;
- verify final persisted state, not only HTTP responses;
- never use arbitrary sleeps to "test" races.

Critical financial or inventory races must prove that exactly one valid outcome wins and losing operations leave coherent state.

## Time

Use injected/fixed `Clock` for deadlines, expiry, lead time, cancellation boundaries, replay/idempotency windows, operational thresholds, and relative-date behavior.

Do not wait in real time. Test equality boundaries explicitly. For timezone-sensitive behavior, use the authoritative venue timezone and compare instants where the rule is elapsed-time based.

## Money

For financial domains:
- backend persisted financial state is authoritative;
- never trust client-supplied amount, percentage, tier, commission, payout, or refund values;
- use `BigDecimal` for authoritative Java money calculations;
- verify exact scale and rounding;
- verify idempotency;
- verify no duplicate money movement;
- verify no over-charge or over-refund;
- verify no negative payout unless debt is explicitly represented as a liability;
- distinguish booking state from payment/refund/payout state.

Any plausible duplicate charge, refund, or payout, or incorrect financial association, is `P0`.

## Authorization

Derive actor matrices from actual endpoint/domain semantics. Do not mechanically repeat every role for every endpoint. Test actors that matter, while ensuring cross-user and cross-owner access is denied where appropriate.

Denied mutations must leave domain state unchanged and must not call external providers.

## Frontend authority

The frontend must not become authoritative for money, eligibility, booking status, availability, moderation status, policy outcome, payment outcome, refund outcome, or payout state.

After mutations, refetch authoritative state where the architecture requires it. Explicitly test stale/conflict behavior when relevant. Do not duplicate every backend boundary in browser tests.

## Defect handling

When a test fails:
1. Determine whether the generated scenario expectation is stale/wrong.
2. Determine whether the test/harness is wrong.
3. Reproduce the suspected product defect independently.
4. Classify severity.
5. Fix it only if intended behavior is clear.
6. Add regression coverage.
7. Rerun the affected domain.
8. Rerun relevant cross-domain regression suites.

Do not "fix" ambiguous product decisions. Preserve historical evidence of defects that were found and fixed.

Suggested severity:
- **P0:** financial integrity, authorization/privacy breach, double booking, duplicate money movement, irreversible data corruption, critical security failure.
- **P1:** important workflow correctness, boundary/state-machine error, serious stale-state behavior, material operational failure.
- **P2:** misleading UX, incorrect copy, recoverable presentation/state-refresh defect.

## Scope control

You **may** fix correctness, security, financial-integrity, authorization, privacy, race/idempotency defects, clearly incorrect UX around existing behavior, and missing validation necessary for an existing contract.

You **may not** implement speculative product functionality merely because testing suggests it could be useful.

Report rather than automatically build new pricing models, marketplace workflows, booking statuses, calendar sync, AI features, arbitrary admin powers, automated payouts, tax systems, arbitrary partial refunds, KYC/KYB, insurance workflows, or analytics products.

Rules:
- Existing behavior → test.
- Existing defect → fix.
- Security/financial-integrity problem → fix.
- New feature → backlog.
- Ambiguous product decision → report for human decision.
- External-provider requirement → staging.

## Cross-domain regression

Do not duplicate previous domain suites unnecessarily. If the selected domain depends on behavior already proven elsewhere, reference the prior execution report and add only the smallest integration assertion needed.

Examples:
- REF should not rerun all cancellation-policy boundaries.
- PAYOUT should not rerun PRICE mathematics.
- DOCUMENTS should not rerun the complete PAYMENT state machine.
- ADMIN operations should consume authoritative domain state rather than recalculate it.

## External-provider boundaries

Locally mock Mollie, Resend, Gemini, and S3-compatible storage as appropriate.

Clearly distinguish **LOCAL LOGIC VERIFIED** from **REAL PROVIDER NOT VERIFIED**.

Real Mollie, Resend, S3, Gemini, deployed multi-instance behavior, production edge/WAF, TLS/domain configuration, and provider fee/reporting behavior belong under `STAGING ONLY` unless the environment and credentials are explicitly supplied.

Never fabricate provider results.

## Legal and business boundaries

Software tests cannot approve legal policy wording, privacy semantics, insurance requirements, tax treatment, merchant-of-record status, accounting policy, retention periods, or operational SLAs.

Separate technical correctness from legal/business approval.

## Evidence and documentation

For every completed domain:
- create/update `docs/qa/<DOMAIN>_EXECUTION_REPORT.md`;
- store sanitized evidence under `docs/qa/evidence/<domain>/`;
- update relevant scenarios in `docs/EEMA_COMPLETE_TEST_MASTER_PLAN.md`;
- regenerate `docs/EEMA_COMPLETE_TEST_MASTER_PLAN.pdf`;
- preserve historical defect evidence.

Evidence must not contain access/refresh tokens, passwords, API keys, raw sensitive provider responses, plaintext IBANs, or unnecessary personal data.

## Validation commands

Use focused tests during development, then required complete regression before declaring a domain complete.

Backend:
```bash
mvn test
mvn verify -Ppostgres-integration
mvn clean package
git diff --check
```

Frontend:
```bash
npm run typecheck
npm run lint
npm run build
git diff --check
```

Use the existing browser harness for representative journeys. Never claim a command passed unless it actually ran successfully.

## Scenario result classification

Every scenario ends as:

**PASS** — all locally required evidence supports the corrected scenario.

**FAIL** — a reproducible unresolved defect violates the intended contract.

**BLOCKED** — a required environment, external provider, legal decision, or business input is genuinely unavailable.

Do not use `BLOCKED` merely because testing is inconvenient. If local logic passes but staging/provider verification remains, distinguish them.

## Human review

At the end of every domain include `WHAT ELHAM SHOULD TEST PERSONALLY`.

Only include subjective/user-facing matters such as clarity, terminology, trust, workflow comprehension, error wording, destructive-action clarity, and mobile usability.

Do not ask Elham to manually reproduce races, database constraints, password hashing, JWT behavior, financial arithmetic, authorization matrices, or provider idempotency.

## Product / operations decisions

Separately include `PRODUCT / OPERATIONS DECISIONS`.

List unresolved decisions discovered during validation. Do not implement them unless explicitly approved. A discovered product question is not automatically a defect.

## Staging

Separately include `STAGING ONLY`.

Only list behavior genuinely requiring deployed/external infrastructure, such as real lifecycle email, Mollie payments/refunds, S3 durability, Gemini behavior, multi-instance workers, edge controls, TLS/CORS/domain behavior, or production monitoring.

Do not claim staging verification from local tests.

## Final domain report

At completion report concisely:
1. Domain.
2. Scenario IDs.
3. `PASS / FAIL / BLOCKED` counts.
4. Corrected generated assumptions.
5. Actual authoritative contract/state machine.
6. Existing coverage reused.
7. Tests added.
8. Defects found.
9. Defects fixed.
10. Unresolved defects.
11. PostgreSQL/concurrency results.
12. Browser/client results.
13. Backend test/build totals.
14. Frontend typecheck/lint/build results.
15. Documentation/evidence updated.
16. `WHAT ELHAM SHOULD TEST PERSONALLY`.
17. `PRODUCT / OPERATIONS DECISIONS`.
18. `STAGING ONLY`.
19. Whether the domain is locally clear to proceed.
20. The next unfinished domain.

## Stop condition

**After completing one domain, STOP.**

Do not automatically begin modifying the next domain.

Report:

`NEXT UNTESTED DOMAIN: <DOMAIN>`

and briefly summarize what it covers. Wait for explicit approval before beginning it.

This prevents an ambiguous product/business decision in one domain from silently becoming an assumption for later domains.

## Optional higher-autonomy mode

Only when explicitly instructed, continue automatically to the next domain if and only if:
- every scenario in the current domain locally passes;
- no P0/P1 defect remains;
- no unresolved product/business/legal decision affects the next domain;
- no migration or architectural change introduced ambiguity;
- no staging/provider result is being treated as locally verified.

Otherwise stop. When uncertain, stop and ask.

## Current project phase

Eema is in **market-validation mode**.

The highest-risk open question is no longer simply whether the software can work. It is whether the underlying customer problem, workflow, demand, and willingness to change behavior justify continued product development.

Therefore:
- continue validating existing functionality;
- fix genuine defects;
- preserve security and financial integrity;
- do not add speculative features;
- record feature ideas in the backlog;
- leave product direction to customer-validation evidence.

Technical validation must support market validation, not replace it.
