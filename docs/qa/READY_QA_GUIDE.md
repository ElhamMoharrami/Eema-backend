# Readiness QA rerun

See [READY execution report](READY_EXECUTION_REPORT.md) for the twelve keys, DTO/database distinctions and remaining human/operations checks.

## Automated checks

```sh
mvn test -Dtest=VenueReadinessContractTest
mvn verify -Ppostgres-integration '-Dit.test=VenueDomainIntegrationTest#readinessMissingFieldBlocksSubmitApproveAndReactivateUntilRestored'
node scripts/qa/ready-client-check.cjs
mvn test
mvn verify -Ppostgres-integration
mvn clean package
git diff --check
```

Use Java 21, Maven and Docker. PostgreSQL 16 Testcontainers handles persisted fixtures; no H2 or manual database edits are needed. The pure/service suite covers all twelve requirements; persisted tests cover ten legally reachable absence variants across submission, approval and reactivation. NOT NULL fields stay in internal service fixtures. Existing VEN tests cover the approval/edit race and matching/availability distinctions.

In the frontend run `npm run typecheck`, `npm run lint`, `npm run build`, and `git diff --check`. The lightweight client check uses the already-installed TypeScript compiler and shared labels; it does not implement readiness locally.

## Representative real browser journey

Reserve ports 15442 (dedicated PostgreSQL), 8080 (Spring), 3000 (Next) and 9223 (Chrome). Identify and stop only task-owned processes if needed; never reset shared data.

```sh
docker compose -p eema-ready-qa -f scripts/qa/ready-compose.yml up -d
cp target/backend-0.0.1-SNAPSHOT.jar /tmp/eema-ready-browser.jar
env SPRING_PROFILES_ACTIVE=local \
  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15442/eema_ready_qa \
  SPRING_FLYWAY_LOCATIONS=classpath:db/migration/common \
  EMAIL_NOTIFICATIONS_ENABLED=false UPLOAD_DIR=/tmp/eema-ready-uploads \
  SERVER_PORT=8080 java -jar /tmp/eema-ready-browser.jar
```

The local profile supplies disposable eema/eema database credentials. Do not use production credentials. In a separate terminal start the frontend production build with `npm run start`, pointing to Spring on port 8080.

```sh
google-chrome --headless --no-sandbox --disable-gpu --disable-dev-shm-usage \
  --disable-background-networking --remote-debugging-port=9223 \
  --user-data-dir=/tmp/eema-ready-browser about:blank
node scripts/qa/ready-browser.cjs --local-qa
```

The Chrome flag is limited to this isolated local QA process. The runner creates unique fictional identities, verifies the dedicated database identity before fixture role/email changes, publishes only clearly marked QA policy content, and uses real browser/API transitions. `--probe` is historical defect reproduction mode, not final PASS validation. Evidence is sanitized JSON/screenshots under `docs/qa/evidence/readiness/`; tokens remain in memory/isolated browser storage.

After the runner closes Chrome, stop the identified QA frontend/backend and remove the whole dedicated tmpfs database:

```sh
docker compose -p eema-ready-qa -f scripts/qa/ready-compose.yml down
```

Remove only its QA profile, uploaded image files and copied jar after processes stop. Do not delete individual shared policies, venues, bookings or trust events.

Regenerate the master-plan PDF with `/usr/bin/python3 scripts/qa/render-master-plan.py` after recording results. Review a representative READY page. Legal policy approval, actual email delivery and deployed storage are not implied by a local readiness PASS.
