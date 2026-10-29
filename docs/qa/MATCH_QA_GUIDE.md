# Matching QA reproduction

Run from the backend repository; preserve existing changes. No live Gemini credentials are needed.

```sh
node scripts/qa/match-client-check.cjs
mvn test -Dtest=MatchingProviderContractTest,MatchingTransportContractTest,MatchingEligibilityContractTest
mvn verify -Ppostgres-integration -Dit.test=MatchingIntegrationTest
mvn test
mvn verify -Ppostgres-integration
mvn clean package
git diff --check
```

Frontend: `npm run typecheck`, `npm run lint`, `npm run build`, `git diff --check` in the frontend repository. For the representative browser run, start the existing built frontend with its default local API URL (`http://localhost:8080/api`) using `npm run start`. Ensure ports8080,3000,9223 are free of unrelated processes. Then:

```sh
mvn verify -Ppostgres-integration -Dit.test=MatchingBrowserIntegrationTest -Deema.match.browser=true
```

The opt-in browser test starts Spring on8080 and disposable PostgreSQL, creates fictional catalogue fixtures and an unverified-email scout, mocks only Gemini generation and launches the existing Chrome/CDP harness. Real provider-envelope extraction, parser, SQL/Java filtering, availability, ranking, HTTP security and React render remain in the flow. Model-generated response fixtures intentionally contain a script string to prove text rendering. No actual Gemini quality conclusion is drawn. Test-only ACTIVE rows establish catalogue read fixtures, not production moderation approvals or legal policies.

The private temporary session file is mode0600 and deleted in finally. Chrome/profile is task-owned and removed; stop the frontend process you started. Testcontainers removes its disposable DB. No shared venue/trust/media history needs cleanup. Evidence under `docs/qa/evidence/match/` contains counts, controlled JSON and fictional screenshots, never tokens or provider credentials. Raw Maven logs should remain outside committed evidence.

The full integration profile intentionally skips the browser test unless explicitly enabled; record its separate result. The matrix uses a shared set of fictional accounts per disposable DB to avoid repeatedly exercising unrelated password hashing. PostgreSQL rejects contradictory permitted/prohibited lists; those cases assert the DB constraint rather than persisting impossible rows. Pure Java tests still prove prohibition wins defensively.
