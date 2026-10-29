# PRICE evidence

All identities are fictional QA fixtures. No access/refresh tokens, provider credentials, real personal data or live-provider payloads are retained.

- `client-before.json`: original frontend arithmetic probe (92 cases; raw discrepancies distinguished from displayed cents).
- `client.json`: final shared-fixture exact parity result.
- `browser.json`: nine browser assertions and sanitized booking HTTP method/path/status observations.
- `qa-price-*-preview.png` / `qa-price-*-saved.png`: actual indicative preview, saved Spring price and request lifecycle.
- `owner-invalid-price.png`, `owner-pricing-reloaded.png`: owner form validation and persisted decimal/optional-cap controls.
- `verification.json`: command outcomes and suite totals, captured after final regression.
- `reproductions.json`: sanitized defect reproductions; failures here describe the original implementation, not the final regression result.

Shared arithmetic data: `src/test/resources/pricing-cases.json`. Java runner: `PriceContractTest`. Frontend runner: `node scripts/qa/price-client-check.cjs` (uses sibling frontend's installed TypeScript). PostgreSQL runner: `mvn verify -Ppostgres-integration -Dit.test=PriceIntegrationTest`.

Browser: build/start the sibling frontend at port3000 against API port8080, then `mvn verify -Ppostgres-integration -Dit.test=PriceBrowserIntegrationTest -Deema.price.browser=true`. The existing Chrome CDP harness creates an isolated profile and a temporary mode0600 auth fixture, deletes both afterward, and uses the actual Spring HTTP endpoints with disposable PostgreSQL. A single featured ACTIVE venue is reconfigured for each representative price because the home page's direct-quote entry displays the first catalogue venue. Requests remain non-blocking, and no live Mollie call occurs.

The normal profile intentionally skips opt-in browser launchers. The PRICE browser launcher was separately executed successfully. Full raw logs remain temporary; only sanitized summaries are versionable.
