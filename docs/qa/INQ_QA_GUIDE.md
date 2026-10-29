# Re-running deterministic inquiry QA

Run from Eema-backend. PostgreSQL tests use disposable Testcontainers databases; Docker access is required. No shared reset or real provider credentials are needed.

```sh
mvn test
mvn verify -Ppostgres-integration -Dit.test=InquiryIntegrationTest
mvn verify -Ppostgres-integration
mvn clean package
git diff --check
```

The inquiry suite parameterizes request fields, roles, five-state/four-action behavior and all eight booking statuses. Race groups repeat three times with barriers. A fixed Clock covers exact deadlines and expiry during lock contention. A disposable stricter constraint injects conversion failure; its expected generic500 is a rollback test, not a permitted race result.

For real browser QA, build/start the existing frontend first with API base pointing at localhost:8080/api (the established local configuration). Ensure no other backend uses port8080. The opt-in JVM serves real Spring HTTP on8080 with disposable PostgreSQL and invokes the existing Chrome CDP runner. Chrome and Node `ws` must be available.

```sh
# Eema-frontend
npm run typecheck
npm run lint
npm run build
npm run start
# In another terminal, Eema-backend
mvn verify -Ppostgres-integration -Dit.test=InquiryBrowserIntegrationTest -Deema.inquiry.browser=true
```

Browser fixtures are fictional; temporary auth config is mode0600 and automatically removed. Chrome uses an isolated temporary profile with browser timezone America/Los_Angeles against Europe/Amsterdam venue times. Evidence is saved under `docs/qa/evidence/inquiry/`. Stop only the frontend process you started afterward. Do not run frontend build while that server is serving the old build.

Do not run the optional `eema.inquiry.probe=true` against corrected UI expecting success: it intentionally checks the previously reproduced stale-control/timezone defects. Pre-fix evidence is retained separately.

See [execution report](INQ_EXECUTION_REPORT.md) for exact contracts, defects, counts and the short human/product/staging lists. No live Mollie or Resend success is asserted by this suite.
