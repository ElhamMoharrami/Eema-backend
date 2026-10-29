# Availability QA reproduction

Read `AVL_EXECUTION_REPORT.md` for source-derived algebra, exact statuses, evidence and limits.

## Automated

```sh
mvn test
mvn verify -Ppostgres-integration
mvn clean package
git diff --check
node scripts/qa/availability-client-check.cjs
```

Focused pure interval/lead checks: `mvn test -Dtest=AvailabilityAlgebraTest`. Focused real PostgreSQL: `mvn verify -Ppostgres-integration '-Dit.test=VenueDomainIntegrationTest#avl*'`. The status matrix is parameterized over the enum. Acceptance races repeat three times using the existing real-repository two-party barrier; no sleeps choose transaction order. PostgreSQL is disposable Testcontainers, never H2.

Run `npm run typecheck`, `npm run lint`, `npm run build` and `git diff --check` in the frontend. Calendar helper checks use the existing TypeScript compiler; they format dates, not inventory.

## Real browser

Reserve 15444 (dedicated PostgreSQL),8080 (Spring),3000 (Next),9223 (Chrome). Do not stop unrelated/shared processes.

```sh
docker compose -p eema-avl-qa -f scripts/qa/availability-compose.yml up -d
cp target/backend-0.0.1-SNAPSHOT.jar /tmp/eema-avl-browser.jar
env SPRING_PROFILES_ACTIVE=local \
  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15444/eema_avl_qa \
  SPRING_FLYWAY_LOCATIONS=classpath:db/migration/common \
  EMAIL_NOTIFICATIONS_ENABLED=false UPLOAD_DIR=/tmp/eema-avl-media \
  DOCUMENT_STORAGE_DIR=/tmp/eema-avl-private SERVER_PORT=8080 \
  java -jar /tmp/eema-avl-browser.jar
```

The local profile supplies disposable database credentials. Start the validated frontend using `npm run start` against localhost8080/api. Then:

```sh
google-chrome --headless --no-sandbox --disable-gpu --disable-dev-shm-usage \
  --disable-background-networking --remote-debugging-port=9223 \
  --user-data-dir=/tmp/eema-avl-browser about:blank
node scripts/qa/availability-browser.cjs --local-qa
```

The runner guards the exact database identity, creates fictional RUN_ID accounts/venues, exercises actual owner weekly/override UI and scout calendar, and uses the real API for the competing reservation. It sets browser timezone to Los Angeles while venue timezone is Amsterdam. Network failure probes abort requests, never synthesize a successful Spring response. The explicit representative DST browser date is25 October2026; for a future campaign after that date, update it to the intended future Amsterdam transition and retain backend fixed-clock cases. Month navigation is checked across December–January. `--probe` records the historical stale-calendar defect and is not final PASS execution.

Only marked `QA TEST CONTENT — NOT LEGAL TERMS` policy rows are created in the disposable DB to establish booking prerequisites. Ordinary schedule editing is exercised before owner review/policy acceptance. Earlier ACTIVE fixtures in this dedicated QA database may be suspended via actual moderation so the home page features the current fixture; history is retained until disposing the whole database.

Sanitized evidence is under `docs/qa/evidence/availability/`. Tokens remain only in memory and the temporary browser profile. The runner clears auth and closes Chrome on success. Never commit raw server/test logs, tokens or provider credentials.

After stopping only the task-owned frontend/backend processes:

```sh
docker compose -p eema-avl-qa -f scripts/qa/availability-compose.yml down
```

Remove only this QA profile, copied jar and `/tmp/eema-avl-media`/`/tmp/eema-avl-private` directories. Never delete shared schedules, overrides, bookings or audit history. Regenerate the master-plan PDF with `/usr/bin/python3 scripts/qa/render-master-plan.py`.
