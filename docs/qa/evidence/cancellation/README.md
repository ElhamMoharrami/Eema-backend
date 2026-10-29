# Cancellation evidence

Canonical CANCEL-001 through CANCEL-014 completion: 2026-09-19. The September 15 requirements-checklist evidence is preserved under `history/2026-09-15/`; the original `validation.json` remains dated historical evidence. See `../../CANCEL_EXECUTION_REPORT.md` for scenario mapping, findings and final clearance.

- `boundaries-2026-09-19.json`: 30 named deterministic boundary cases, including both DST changes, exact cents and signed preview/mutation agreement.
- `focused-postgres-2026-09-19.json`: 23 actual PostgreSQL/Testcontainers executions, including six added cases. No H2 or mock database.
- `browser.json`, `confirmation.png`, `zero-refund.png`: current Chrome/CDP journey and eight assertions. Fixtures are fictional. A controlled409 proves client behavior; actual tier-crossing conflict is tested through PostgreSQL.
- `unit-2026-09-19.json`, `postgres-2026-09-19.json`, `package-2026-09-19.json`: sanitized per-suite counts from required full regression commands. No raw XML system properties, tokens, request bodies or provider response logs are copied.
- `document-check-2026-09-19.json`: Markdown/PDF fingerprints, ID count and changed-page visual review.
- `completion-2026-09-19.json`: final command and scenario outcomes.

Run backend Maven commands sequentially because they share `target/`:

```sh
mvn test -Dtest=CancellationScenarioBoundaryTest
mvn verify -Ppostgres-integration -Dit.test=CancellationIntegrationTest
mvn verify -Ppostgres-integration -Dit.test=CancellationBrowserIntegrationTest -Deema.cancellation.browser=true
mvn test
mvn verify -Ppostgres-integration
mvn clean package
git diff --check
```

Browser execution uses the existing frontend production build served on port3016, disposable backend port18086, headless Chrome and mocked Mollie. Frontend validation: `npm run typecheck`, `npm run lint`, `npm run build`, `git diff --check`. The integration profile intentionally skips unit tests and disables opt-in browser classes; the unit command and explicit cancellation browser invocation provide that separate evidence.

No real Mollie payment/refund, Resend delivery, bank settlement or deployed multi-instance operation is claimed. The fixed-clock tests do not sleep to cross business deadlines. Existing financial races use independent transactions and barriers/latches.
