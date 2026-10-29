# Approved complete exception backlog — local evidence

The user's approved Eema v1 decision supersedes the capped-preview ambiguity. History retains the previous blocked report, 200-of-201 characterization and prior browser result. Current fixtures are synthetic; no production provider or user data is used.

- `focused-unit.json`: only the five service/boundary tests executed by the focused command.
- `focused-postgres.json`: 47 real PostgreSQL cases, including cardinalities 0/1/20/21/200/201/321 and repeated mixed-category traversal.
- `focused-browser.json` and `browser.json`: one enabled browser launcher, 13 checks; the real UI traverses 202 exceptions over 11 pages using size20 requests.
- `exceptions-final-page.png`: final page with one refund and one payment exception, Previous enabled and Next disabled.
- `query-plan.json`: EXPLAIN ANALYZE/BUFFERS of the actual production SQL on the 321-refund fixture. The full run refreshes this evidence; timings are local observations, not a production performance guarantee.
- `unit.json`, `postgres.json`, `package.json`: generated only after successful corresponding full commands.

Database work for exact counts and deep OFFSET grows with the qualifying backlog. Database-side sorting/materialization is permitted; Java receives at most one bounded page and no sensitive domain entities. No exception table or migration is introduced. Worker batches and metric scans were not changed. The API has no existing type/severity filters, so those are not invented. Pagination is deterministic for stable data; separate requests are not a snapshot transaction.

Local evidence does not verify real providers, production infrastructure, operational SLAs or legal policy.
