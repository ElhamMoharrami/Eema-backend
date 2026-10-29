# Venue QA rerun

See [execution report](VEN_EXECUTION_REPORT.md) for the audited contract, defects and remaining decisions. Fixtures are fictional and policy text is explicitly `QA TEST CONTENT — NOT LEGAL TERMS`.

## Deterministic backend

From the backend repository with Java 21, Maven and Docker available:

```sh
mvn test
mvn verify -Ppostgres-integration
mvn verify -Ppostgres-integration -Dit.test=VenueDomainIntegrationTest
mvn clean package
git diff --check
```

The profile uses disposable PostgreSQL 16 Testcontainers. `VenueDomainIntegrationTest` includes three repetitions each of admin review, critical-edit/approval and suspension/acceptance races. Synchronization uses barriers before the existing database row lock; there are no race timing sleeps. Media files use a unique temporary directory removed by the suite. Do not replace PostgreSQL with H2.

## Isolated browser environment

Use the existing Chrome CDP runner, not a shared local/staging database. Reserve ports 15441 (QA PostgreSQL), 8080 (Spring), 3000 (production Next) and 9223 (Chrome). Stop only identified task-owned processes if these ports are occupied; do not reset shared data.

```sh
docker compose -p eema-venue-qa -f scripts/qa/venue-compose.yml up -d
```

Start a copy of the packaged backend jar with these QA-only settings:

```sh
env SPRING_PROFILES_ACTIVE=local \
  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15441/eema_venue_qa \
  SPRING_FLYWAY_LOCATIONS=classpath:db/migration/common \
  EMAIL_NOTIFICATIONS_ENABLED=false \
  UPLOAD_DIR=/tmp/eema-venue-browser-uploads SERVER_PORT=8080 \
  java -jar /tmp/eema-venue-browser.jar
```

Copy `target/backend-0.0.1-SNAPSHOT.jar` to that temporary jar first. The repository local profile supplies disposable eema/eema database credentials. Never use production credentials. The dedicated database contains only common Flyway schema and runner-created QA fixtures.

In the frontend repository run `npm run typecheck`, `npm run lint`, `npm run build`, then `npm run start`. Its API configuration must point to the local Spring API at port 8080.

Start an isolated Chrome profile:

```sh
google-chrome --headless --no-sandbox --disable-gpu --disable-dev-shm-usage \
  --disable-background-networking --remote-debugging-port=9223 \
  --user-data-dir=/tmp/eema-venue-browser about:blank
node scripts/qa/venue-browser.cjs --local-qa
```

Chrome's `--no-sandbox` is for this disposable local test process only. Run the node command from the backend repository. The runner checks the dedicated database identity and registration rows before changing fictional email/role fixtures, creates unique RUN_ID actors/listings, and exercises real browser/API flows. It publishes only explicitly marked QA policy content to this isolated database. It may suspend older QA catalogue fixtures through the actual admin API to isolate the featured venue; history is retained until the whole disposable database is removed.

Evidence is written to `docs/qa/evidence/venue/`; the runner stores no tokens or passwords there and closes Chrome on success. `--probe` is a defect-reproduction mode and must not be treated as final validation.

After success stop the task-owned frontend/backend processes and remove the **whole dedicated** database:

```sh
docker compose -p eema-venue-qa -f scripts/qa/venue-compose.yml down
```

The database uses tmpfs. Remove only the identified QA browser profile, uploads and copied jar after stopping their processes. Do not delete individual shared venue, booking, policy or trust-event rows.

## Master-plan PDF

After editing the Markdown and recording final results:

```sh
/usr/bin/python3 scripts/qa/render-master-plan.py
```

The existing system MarkdownIt/Chrome renderer escapes raw HTML in attack examples. Inspect the regenerated VEN pages and run both repositories' `git diff --check`.
