# Re-running owner trust QA

Use the backend repository root. Read TRUST_EXECUTION_REPORT.md for the audited contract and limitations. Fixtures are fictional; do not enter personal evidence or legal identity documents.

## Automated software checks

```sh
mvn test
mvn verify -Ppostgres-integration
mvn verify -Ppostgres-integration -Dit.test=OwnerTrustIntegrationTest
mvn clean package
git diff --check
```

Docker must be available for disposable PostgreSQL 16 Testcontainers. The TRUST suite repeats each race three times using barriers, not sleeps. It runs HTTP/security, audit and notification checks. No H2 or real email provider is used.

In the frontend repository:

```sh
npm run typecheck
npm run lint
npm run build
git diff --check
```

## Local browser harness

Ports 8080, 3000, 15440 and 9223 must be available. Do not stop an unrelated process just to free them. Start the dedicated database:

```sh
docker compose -p eema-trust-qa -f scripts/qa/trust-compose.yml up -d
cp target/backend-0.0.1-SNAPSHOT.jar /tmp/eema-trust-qa.jar
env SPRING_PROFILES_ACTIVE=local SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15440/eema_trust_qa SPRING_FLYWAY_LOCATIONS=classpath:db/migration/common EMAIL_NOTIFICATIONS_ENABLED=false SERVER_PORT=8080 java -jar /tmp/eema-trust-qa.jar
```

The disposable database uses the existing local eema/eema development credentials. Copy the jar before starting it: Maven must not replace a running jar. Wait for backend startup/health. In another terminal run the already built frontend with `npm run start`, then isolated Chrome:

```sh
google-chrome --headless --no-sandbox --disable-gpu --disable-dev-shm-usage --disable-background-networking --remote-debugging-port=9223 --user-data-dir=/tmp/eema-trust-browser about:blank
node scripts/qa/trust-browser.cjs --local-qa
```

The existing system Chrome/ws harness creates unique RUN_ID accounts. It verifies that API-created IDs exist in the dedicated DB before fixture updates. OWNER starts through the real login form; subsequent actor changes use isolated local session setup. It exercises real API lifecycle and stale reviews; 401/403/500/network rendering uses explicit CDP fault injection with OPTIONS preflight passed through. No provider delivery occurs.

Evidence is overwritten by the next successful run under `docs/qa/evidence/trust/`; preserve a run separately if comparing versions. `--probe` additionally reproduces the pre-fix hidden-conflict behavior and is for before/after diagnosis only. Normal final execution must use the full runner.

The runner clears session storage and closes Chrome on success. If interrupted, close the isolated Chrome process/profile before reuse; never commit profile data, raw tokens or backend logs. Stop only the backend/frontend processes started for this run, then dispose the entire dedicated tmpfs DB:

```sh
docker compose -p eema-trust-qa -f scripts/qa/trust-compose.yml down
```

Do not delete individual trust-history rows on shared local/staging databases. No snapshot-restoration harness is assumed.

## Documentation

After scenario updates:

```sh
/usr/bin/python3 scripts/qa/render-master-plan.py
```

The existing renderer uses MarkdownIt and Chrome; raw master-plan HTML examples are escaped. Verify the PDF includes all three corrected TRUST sections. Staging delivery and operations/UX decisions remain listed separately in the execution report.
