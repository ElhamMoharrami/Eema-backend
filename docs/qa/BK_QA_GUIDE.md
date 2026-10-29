# Core booking QA

Read [BK_EXECUTION_REPORT.md](BK_EXECUTION_REPORT.md) for the audited contract and evidence.

Run from backend:

```sh
mvn test
mvn verify -Ppostgres-integration
mvn verify -Ppostgres-integration -Dit.test=BookingCoreIntegrationTest
```

The shared Testcontainers PostgreSQL harness uses fictional accounts and unique venues; no shared data reset. BookingCoreSupport seeds approved ACTIVE catalogue fixtures directly because moderation is already covered by VEN. All booking operations use real Spring endpoints/transactions. Resulting non-request statuses used for decline-denial fixtures are explicitly seeded; they do not claim successful payment/refund delivery.

For browser checks, first build/start the existing frontend on port3000, with its normal API base pointing to localhost8080. Ensure port8080 is free for the test backend, then:

```sh
mvn verify -Ppostgres-integration -Dit.test=BookingBrowserIntegrationTest -Deema.booking.browser=true
```

This starts a real Spring HTTP server and disposable PostgreSQL, launches the existing Chrome/CDP Node harness and writes sanitized evidence under `docs/qa/evidence/booking/`. Auth tokens exist only in a temporary0600 configuration and isolated browser profile, removed afterward. Gemini and Mollie are not called. Email delivery is disabled. The browser uses Los Angeles timezone while venue schedule is Amsterdam; deadlines are displayed as local instants, schedule as venue-local wall time.

`-Deema.booking.probe=true` records the conflict probe only. The committed `browser-before.json` is historical reproduction evidence from before the UI fix; do not overwrite it when validating the final implementation. Database races live in VenueDomainIntegrationTest (AVL acceptance and VEN suspension) and PostgresCriticalPathIntegrationTest; the full PostgreSQL command reruns them. No sleeps control race outcomes; latches/barriers and fixed Clock do.

Only Eli's short UX checklist and the deployment/email checks in the report remain manual. Do not manually count rows, recalculate hashes/prices, reproduce races or invoke real payment-provider settlement for this domain.
