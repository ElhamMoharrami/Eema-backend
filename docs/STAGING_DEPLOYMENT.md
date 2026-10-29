# Eema staging deployment

Status: **READY FOR PROVISIONING**, not staging-rehearsal verified.

The frontend target is Netlify. The backend provider has not yet been selected—the input still says `<INSERT CHOSEN PROVIDER>`—so this repository intentionally does not contain a Render, Railway, Fly.io, or other vendor manifest. The backend is delivered as its existing portable OCI/Docker image.

## Architecture and URL contract

Choose two real DNS names:

```text
STAGING_FRONTEND_URL=https://<staging-frontend-host>
STAGING_API_URL=https://<staging-api-host>
```

These deployment-planning names are not application variables. Set the application configuration derived from them exactly as follows:

```text
APP_FRONTEND_BASE_URL=${STAGING_FRONTEND_URL}
CORS_ALLOWED_ORIGINS=${STAGING_FRONTEND_URL}
MOLLIE_REDIRECT_URL=${STAGING_FRONTEND_URL}/booked
MOLLIE_WEBHOOK_URL=${STAGING_API_URL}/api/webhooks/mollie/payment
NEXT_PUBLIC_API_BASE_URL=${STAGING_API_URL}/api
```

The browser calls Spring for media and private documents. Spring reads/writes S3; the browser does not upload to or fetch from S3 directly. Bucket CORS is therefore unnecessary. Keep the private bucket entirely private.

## Build and run

Build the tested, non-root backend image from the repository root:

```bash
docker build --tag eema-backend:<git-sha> .
```

The build stage runs `mvn clean package` including tests. The runtime image runs as the `eema` user and uses container-aware JVM percentage limits. Configure the provider to pass `SPRING_PROFILES_ACTIVE=prod` and its assigned `PORT`; the application binds `${PORT:8080}`.

The equivalent artifact checks are:

```bash
mvn test
mvn verify -Ppostgres-integration
mvn clean package
```

Configure startup/readiness checks at:

```text
GET /api/actuator/health/readiness
```

The endpoint is public with details hidden. `/api/actuator/metrics/**` requires ADMIN. The platform proxy must terminate valid TLS, redirect HTTP to HTTPS, and forward standard `Forwarded` or `X-Forwarded-*` headers. Spring uses framework forwarded-header handling and emits HSTS in the production profile.

## Environment inventory

Real values belong only in the provider/Netlify secret stores. [.env.staging.example](../.env.staging.example) is a name-and-shape template.

### Public configuration

| Variable | Target | Required | Example form | Purpose |
|---|---|---:|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend | Yes | `prod` | Enables production validation/configuration |
| `PORT` | Backend | Provider-dependent | `8080` | Container listen port |
| `APP_FRONTEND_BASE_URL` | Backend | Yes | `https://staging.example` | Verification/reset links and frontend origin |
| `CORS_ALLOWED_ORIGINS` | Backend | Yes | `https://staging.example` | Explicit browser origin; no wildcard |
| `MOLLIE_REDIRECT_URL` | Backend | Yes | `https://staging.example/booked` | Browser return route |
| `MOLLIE_WEBHOOK_URL` | Backend | Yes | `https://api-staging.example/api/webhooks/mollie/payment` | Public Mollie callback |
| `EMAIL_NOTIFICATIONS_ENABLED` | Backend | Yes | `true` | Enables staging delivery rehearsal |
| `RESEND_FROM_EMAIL` | Backend | Yes when email enabled | `Eema Staging <staging@verified.example>` | Verified staging sender |
| `S3_ENDPOINT` | Backend | Provider-dependent | `https://s3.example` | Empty only for native AWS S3 |
| `S3_REGION` | Backend | Yes | `eu-west-1` | Storage region |
| `S3_BUCKET_PUBLIC_MEDIA` | Backend | Yes | `eema-staging-media` | Venue media only |
| `S3_BUCKET_PRIVATE_DOCUMENTS` | Backend | Yes | `eema-staging-documents` | Receipts/statements only |
| `S3_PATH_STYLE_ACCESS` | Backend | Provider-dependent | `false` | S3 addressing mode |
| `FILE_URL_PREFIX` | Backend | Optional | `/api/upload/files/` | Application-mediated public media route |
| `MOLLIE_FEE_RECONCILIATION_ENABLED` | Backend | Yes | `false` or `true` after reporting setup | Fee job switch |
| `PAYOUT_DESTINATIONS_ENABLED` | Backend | Yes | `true` | Enables staging payout-destination workflow |
| `ADMIN_EMAIL_ALLOWLIST` | Backend | Operational | `admin-staging@example` | Existing configuration metadata; it does **not** bootstrap/promote ADMIN |
| `NEXT_PUBLIC_API_BASE_URL` | Netlify | Yes | `https://api-staging.example/api` | Only browser-visible backend setting |
| `CALA_API_KEY` | Netlify server runtime | Optional | server-only value | Optional `/api/verify-booker` integration; never prefix with `NEXT_PUBLIC_` |

JWT access expiry (15 minutes) and refresh expiry (7 days) are currently fixed application properties; there are no existing environment-variable aliases for them. All optional pool, scheduler, upload, recovery, and workflow tuning names are listed in `.env.staging.example` and `.env.example`.

### Secrets

| Variable | Target | Required | Example form | Purpose |
|---|---|---:|---|---|
| `SPRING_DATASOURCE_URL` | Backend | Yes | `jdbc:postgresql://host:5432/db?sslmode=verify-full` | Managed PostgreSQL connection and TLS mode |
| `SPRING_DATASOURCE_USERNAME` | Backend | Yes | provider-issued user | Database authentication |
| `SPRING_DATASOURCE_PASSWORD` | Backend | Yes | provider secret | Database authentication |
| `JWT_SECRET` | Backend | Yes | Base64 of 32+ random bytes | JWT signing |
| `MOLLIE_API_KEY` | Backend | Yes | `test_…` | Payments, payment retrieval, and refunds |
| `MOLLIE_REPORTING_ACCESS_TOKEN` | Backend | If fee reconciliation enabled | OAuth access token | Balance-transaction fee lookup |
| `RESEND_API_KEY` | Backend | If email enabled | `re_…` | Transactional email delivery |
| `GEMINI_API_KEY` | Backend | Required for matching rehearsal | provider key | Existing matching parser/ranker |
| `S3_ACCESS_KEY` | Backend | Yes | scoped staging credential | Object storage authentication |
| `S3_SECRET_KEY` | Backend | Yes | scoped staging credential | Object storage authentication |
| `PAYOUT_DATA_ENCRYPTION_KEY` | Backend | Yes | Base64 of 32 random bytes | AES-256-GCM payout data protection |
| `PAYOUT_DATA_FINGERPRINT_KEY` | Backend | Yes | different Base64 value | HMAC payout destination fingerprinting |
| `CALA_API_KEY` | Netlify server runtime | Optional | provider key | Server-side company verification only |

Generate independent key material without committing or printing it into logs:

```bash
openssl rand -base64 32
openssl rand -base64 32
openssl rand -base64 32
```

Assign the three outputs separately to JWT, payout encryption, and payout fingerprint secrets. Never reuse them.

## PostgreSQL

Provision an empty, non-production PostgreSQL database. Prefer a provider URL that validates TLS certificates (`sslmode=verify-full`) using its documented CA setup. Do not use `sslmode=disable`; use `require` only when the provider explicitly terminates and authenticates the private connection and documents why hostname verification is unavailable.

On first production-profile startup, Flyway applies common migrations V1–V11 and Hibernate validates the result. Production locations exclude V100–V102. Do not enable baseline-on-migrate, Flyway repair, or automatic rollback.

Before accepting staging data, record:

- automated backup/PITR status and schedule;
- retention duration;
- manual snapshot or `pg_dump` access;
- the procedure and permissions for restoring into a separate disposable database.

The rehearsal remains incomplete until a representative backup is actually restored and the same image starts against it.

## Object storage

Create two non-production buckets with one least-privilege backend credential:

- public media bucket, default prefix `venue-media/`: JPEG, PNG, and WebP venue media only;
- private documents bucket, default prefix `financial-documents/`: receipts and payout statements only.

Production validation refuses identical bucket names. Disable anonymous/public access on the private bucket. The public bucket also need not be anonymously readable because media is currently served by `GET /api/upload/files/{filename}` through Spring. No browser bucket CORS policy or presigned-upload permission is required.

## Mollie TEST and reporting

Set `MOLLIE_API_KEY` to a Mollie TEST key. The same Payments API credential is used for payment creation, authoritative webhook retrieval, and refund creation. The callback must be publicly reachable over HTTPS at exactly:

```text
https://<staging-api-host>/api/webhooks/mollie/payment
```

The return URL is exactly:

```text
https://<staging-frontend-host>/booked
```

Fee reconciliation is different: the application calls `GET /v2/balances/primary/transactions` using `MOLLIE_REPORTING_ACCESS_TOKEN`. This requires a Mollie OAuth access token with permission to read balance transactions; a normal TEST API key is not treated as that credential. Confirm that the staging Mollie organization exposes useful TEST-mode balance transactions before enabling the job. If it does not, keep `MOLLIE_FEE_RECONCILIATION_ENABLED=false` and record payout readiness/provider-fee verification as a staging limitation.

Do not edge-rate-limit the webhook like an interactive endpoint; Mollie retries must remain possible.

## Resend and Gemini

Verify a staging sender domain in Resend, set `RESEND_FROM_EMAIL` to an address on it, and use dedicated non-personal recipient mailboxes. With `EMAIL_NOTIFICATIONS_ENABLED=true`, production startup requires the key and sender. Verification and reset links derive from `APP_FRONTEND_BASE_URL`; it must never be localhost. Application logs must not collect email bodies or raw verification/reset tokens.

Set the existing `GEMINI_API_KEY` for matching rehearsal. No model or prompt change is part of deployment. An absent key produces the existing explicit unavailable/error behavior rather than fabricated matches.

## First staging ADMIN and test data

Public registration rejects the ADMIN role. There is no production ADMIN seed or promotion endpoint. Bootstrap through a controlled, audited database session:

1. Register the dedicated staging admin address through `POST /api/auth/register` as `SCOUT` and complete email verification.
2. Stop using any tokens issued before promotion.
3. In a restricted database session, verify exactly one target row and promote it:

```sql
BEGIN;
SELECT id, email, role, email_verified, active
FROM users
WHERE lower(email) = lower('<dedicated-staging-admin-email>')
FOR UPDATE;

UPDATE users
SET role = 'ADMIN'::user_role,
    updated_at = now()
WHERE lower(email) = lower('<dedicated-staging-admin-email>')
  AND role = 'SCOUT'::user_role
  AND email_verified = true
  AND active = true;

DELETE FROM refresh_tokens
WHERE user_id = (
  SELECT id FROM users WHERE lower(email) = lower('<dedicated-staging-admin-email>')
);
COMMIT;
```

Require an update count of one, then log in again and verify `/api/auth/me` reports ADMIN. Record operator, timestamp, target ID, and reason in the deployment change record. `ADMIN_EMAIL_ALLOWLIST` currently does not perform promotion or authorization and is not a substitute for this procedure.

Create SCOUT, OWNER, venue, availability, and bookings through real APIs. Do not enable dev migrations. Create staging policies manually through ADMIN UI/API only, with title/content visibly containing `STAGING TEST POLICY — NOT PRODUCTION LEGAL TERMS`; never seed or auto-publish them.

## Netlify

The existing `netlify.toml` runs `npm run build` with `@netlify/plugin-nextjs`. In the staging site environment set only:

```text
NEXT_PUBLIC_API_BASE_URL=https://<staging-api-host>/api
```

Set optional `CALA_API_KEY` as a server-only Netlify value. Never put backend, Mollie, Resend, Gemini, database, JWT, payout, or S3 credentials in a `NEXT_PUBLIC_*` variable. Deploy a reviewed commit after running `npm install`, `npm run typecheck`, `npm run lint`, and `npm run build` with the same API base URL.

The 2026-09-07 preparation run built successfully but `npm install` reported 10 dependency advisories (9 high and 1 critical). Review their affected dependency paths and runtime relevance, then apply deliberate compatible upgrades and rerun the frontend checks before private alpha. Do not use `npm audit fix --force` as an unreviewed deployment step.

## Edge, logs, and monitoring

Because the backend provider is unselected, its WAF, log retention, and alert syntax cannot yet be configured. Before private alpha:

- define conservative distributed edge limits for login, registration, forgotten password, resend verification, matching, and uploads;
- exempt or separately protect Mollie webhook retries;
- record provider log retention and searchable access by `X-Request-ID`;
- retain logs outside an individual container;
- monitor readiness and repeated 5xx responses;
- securely scrape ADMIN-only workflow metrics and alert on stuck refunds, overdue payouts, and unreconciled Mollie fees.

If the selected host lacks edge limiting or durable logs, place an approved reverse proxy/WAF and log destination in front of/alongside it. Do not add process-local rate limiting for a multi-replica deployment.

## Deployment order

1. Provision PostgreSQL and record backup/restore capabilities.
2. Create and secure distinct public-media and private-document buckets.
3. Configure backend public settings and encrypted secrets.
4. Build and deploy the backend image with the production profile.
5. Confirm Flyway V1–V11, absence of V100+, and readiness UP.
6. Set Netlify `NEXT_PUBLIC_API_BASE_URL`.
7. Deploy the frontend and check browser-to-backend CORS/TLS.
8. Configure Mollie TEST redirect/webhook and reporting capability.
9. Verify the Resend domain/sender and enable email.
10. Bootstrap the dedicated staging ADMIN through the controlled procedure.
11. Manually create clearly labelled staging policies.
12. Execute [STAGING_LAUNCH_REHEARSAL.md](STAGING_LAUNCH_REHEARSAL.md) and update [PHASE_A_LAUNCH_BLOCKERS.md](PHASE_A_LAUNCH_BLOCKERS.md) from evidence.

## Rollback

Rollback application code by redeploying the previously known-good immutable image/Netlify deploy. Never blindly downgrade Flyway or run automatic repair. If a schema change is not backward-compatible, stop and make an explicit operational decision using the verified pre-deployment backup; restore only into a controlled target and account for data created after that backup. Destructive database rollback is never an automatic deployment hook.

## Provider-selection gate

Before adding a provider manifest, select one backend provider and confirm it supports an OCI image, `${PORT}`, HTTPS/proxy forwarding, readiness probes, encrypted environment variables, durable/searchable logs, and access to managed PostgreSQL and the chosen S3 endpoint. Then add only that provider's minimum deployment file.
