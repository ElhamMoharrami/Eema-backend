# MEDIA QA reproduction

Use `MEDIA_EXECUTION_REPORT.md` for the audited contract, limitations and results. No shared local/staging media or history may be deleted to rerun these checks.

## Automated checks

From the backend:

```sh
mvn test
mvn verify -Ppostgres-integration
mvn clean package
git diff --check
node scripts/qa/media-client-check.cjs
```

Focused binary/storage: `mvn test -Dtest=MediaStorageContractTest,S3StorageTest,VenueServiceTest`. Focused real PostgreSQL: `mvn verify -Ppostgres-integration '-Dit.test=VenueDomainIntegrationTest#media*'`. The upload/last-slot and full-PUT races each use confirmed two-party barriers and repeat three times. No H2 or timing sleeps select race order.

Fixtures are real small JPEG/PNG/WebP images in `src/test/resources/media`. Tests generate exact size-boundary padding in memory. `qa-metadata.jpg` has fictional software/time metadata only. No real personal data, passport/evidence file, S3 credential or huge binary belongs in evidence.

## Local browser and restart durability

Reserve 15443 (dedicated PostgreSQL), 8080 (backend), 3000 (frontend) and 9223 (Chrome). Identify task-owned processes before stopping anything. Build/start the isolated stack using the real production Dockerfile:

```sh
docker compose -p eema-media-qa -f scripts/qa/media-compose.yml up -d --build
```

This uses a disposable tmpfs database and dedicated named media/document volumes. The corrected Dockerfile initializes non-root directory ownership for fresh volumes. Existing deployment volumes do not receive an automatic ownership migration.

In the frontend run `npm run typecheck`, `npm run lint`, `npm run build`, `git diff --check`, then start that build with `npm run start`. It must point to `http://localhost:8080/api`.

```sh
google-chrome --headless --no-sandbox --disable-gpu --disable-dev-shm-usage \
  --disable-background-networking --remote-debugging-port=9223 \
  --user-data-dir=/tmp/eema-media-browser about:blank
node scripts/qa/media-browser.cjs --local-qa
```

The lightweight runner reuses existing Chrome CDP + `ws`. It checks the exact dedicated database identity before creating fictional RUN_ID accounts and changing QA roles/email verification. It performs real UI login, real multipart uploads, loading-state observation by temporarily pausing/releasing a real request, valid previews, reorder/remove, corrupt/oversize errors, public primary rendering, moderation, maximum count and container restart. Browser waits observe UI/network/health state; they do not manufacture backend results. It uses actual moderation to suspend earlier MEDIA-only ACTIVE fixtures when the home page's featured listing needs isolation; all history remains until the entire QA environment is discarded.

Only explicitly marked `QA TEST CONTENT — NOT LEGAL TERMS` policy records are created in the disposable database to establish a real ACTIVE venue. Ordinary upload is tested before those policy/owner-review prerequisites. The runner closes Chrome and clears browser auth on success. `--probe` is historical size-error reproduction mode, not a final validation result.

After completion, stop the identified frontend process, then dispose the complete task environment:

```sh
docker compose -p eema-media-qa -f scripts/qa/media-compose.yml down -v
```

Remove only `/tmp/eema-media-browser`, `/tmp/eema-media-fake.jpg` and `/tmp/eema-media-large.jpg` after task processes stop. Never perform this cleanup against a shared compose project or bucket.

## Master-plan artifact

Regenerate the existing PDF with `/usr/bin/python3 scripts/qa/render-master-plan.py`; inspect the MEDIA pages and preserve sanitized test-count JSON/screenshots in `docs/qa/evidence/media/`. Raw Maven/container/browser logs may contain local tokens/payloads and should not be committed.
