# Policy QA guide

The deterministic policy checks are automated. Use the execution report for results and remaining human/legal work. Do not publish QA text into production or shared staging. No policy history is manually deleted.

## Backend and client checks

```bash
mvn test
mvn verify -Ppostgres-integration
mvn clean package
node scripts/qa/policy-client-check.cjs
```

The PostgreSQL profile uses disposable Testcontainers PostgreSQL 16 and common Flyway migrations, including V11. `PolicyIntegrationTest` tests the production HTTP/security/service/repository stack. Mockito repository spies only introduce transaction barriers; queries and writes still use PostgreSQL. The paid cancellation test mocks only the external refund provider. No H2 and no provider calls.

## Browser isolation

The runner reuses AUTH's Chrome DevTools Protocol and installed `ws` module; it adds no browser framework. It creates fresh SCOUT, OWNER and ADMIN fixtures, marks the owner's independent verification prerequisite in the disposable DB, and uses only `QA TEST CONTENT — NOT LEGAL TERMS`. Tokens remain in process memory/browser localStorage and are not saved in evidence. No credentials are logged.

Ports 8080, 3000, 9223 and 15439 must be available. Stop an existing local stack deliberately before running; do not point this script at staging. From the backend repository:

```bash
docker compose -p eema-policy-qa -f scripts/qa/policy-compose.yml up -d
SPRING_PROFILES_ACTIVE=local \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15439/eema_policy_qa \
SPRING_FLYWAY_LOCATIONS=classpath:db/migration/common \
EMAIL_NOTIFICATIONS_ENABLED=false SERVER_PORT=8080 \
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Use a separate terminal in the frontend repository:

```bash
npm run typecheck
npm run lint
npm run build
npm run start
```

The frontend build must use `http://localhost:8080/api`. Do not supply provider credentials to the local backend. The local profile uses development-only payout protection keys; no real bank account is used.

Start Chrome in a separate terminal:

```bash
google-chrome --headless --no-sandbox --disable-gpu --disable-dev-shm-usage \
  --disable-background-networking --remote-debugging-port=9223 \
  --user-data-dir=/tmp/eema-policy-browser about:blank
```

Then, from the backend repository:

```bash
node scripts/qa/policy-browser.cjs --local-qa
```

The suite retires current **QA** versions only in `eema_policy_qa` so it can repeat the empty-set journey. It retains every historical version and acceptance throughout the run. It tests absent/partial/full sets; subset/all acceptance; literal HTML as text; neutral Privacy acknowledgement; a stale OWNER payout form that receives 403 and triggers status refresh; successful submission after review; ADMIN draft creation, content review, confirmation, publication, replacement, retirement and reload. It clears the QA auth session and closes Chrome on success. Screenshots and JSON summaries are in `docs/qa/evidence/policy/`.

Stop the backend/frontend processes started for this suite. Dispose the entire dedicated QA database:

```bash
docker compose -p eema-policy-qa -f scripts/qa/policy-compose.yml down
```

Its data directory is tmpfs, so this removes only the intentionally disposable test environment. Shared local/staging databases are untouched. The browser profile is disposable and must never be committed. `--probe` is retained only to reproduce the original missing-type UI defect against an old build; it is not the final pass command.

## Evidence interpretation

`client.json` uses controlled transport with actual frontend client/auth/error modules. `browser.json` uses real Chrome, HTTP and PostgreSQL. PostgreSQL result summaries contain test names and counts only; raw Maven logs remain temporary because unrelated auth tests can log test token-related data. SHA-256 checks are software integrity checks, not signatures or legal approval.

## Master-plan PDF

Run `/usr/bin/python3 scripts/qa/render-master-plan.py` to regenerate the existing landscape PDF using installed system MarkdownIt and Chrome. Raw HTML attack examples render as text; only safe section anchors are restored. The regenerated policy pages and the final document content were checked, including visual inspection of page 33.
