# Payment evidence

All data is fictional. PostgreSQL is disposable Testcontainers PostgreSQL16; Mollie transport is mocked. Browser interaction uses the real built Next frontend and Spring API, with a controlled hosted-checkout navigation page. No live payment, refund or email delivery is claimed.

- `reproductions.json`: sanitized before-fix observations and hashes of local diagnostic logs; excludes raw HTTP bodies/tokens.
- `browser-before.json`, `before-stale-checkout.png`: stale Pay now reproduction.
- `browser.json`: final browser checks and API method/path/status trace.
- `stale-checkout.png`, `delayed-webhook.png`, `confirmed.png`, `late-refund.png`: representative UI evidence.
- `validation.json`: command results, exact test counts and artifact checks.
- `source-provenance.json`: base revisions and tested source hashes across both repositories.

Reproduce backend coverage with `mvn test`, `mvn verify -Ppostgres-integration`, and `mvn clean package`. For the browser, run the production frontend on localhost3000 configured for localhost8080/api, then `mvn verify -Ppostgres-integration -Dit.test=PaymentBrowserIntegrationTest -Deema.payment.browser=true`. The harness owns disposable database fixtures and removes its temporary browser profile/auth config. It never resets shared data.

Frontend checks: `npm run typecheck`, `npm run lint`, `npm run build`, `git diff --check`. Backend diff check is separate. The master-plan PDF is rendered using `scripts/qa/render-master-plan.py`; its PAY sections were text-checked and visually reviewed.
