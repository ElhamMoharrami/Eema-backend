# Eema production deployment

Eema expects an HTTPS frontend, an HTTPS backend API, and an HTTPS Mollie webhook. A reverse proxy or load balancer should terminate TLS and forward standard `Forwarded`/`X-Forwarded-*` headers. The backend uses Spring's forwarded-header support.

Docker Compose in this repository is for local development, not production. Deploy the immutable backend image on a managed container platform with at least two replicas only after PostgreSQL and S3-compatible storage are shared external services.

## Required production environment

Run with `SPRING_PROFILES_ACTIVE=prod` and provide:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`: Base64 for at least 32 cryptographically random bytes
- `CORS_ALLOWED_ORIGINS`: comma-separated explicit HTTPS frontend origins; no wildcards
- `APP_FRONTEND_BASE_URL`: non-local HTTPS URL
- `MOLLIE_API_KEY`, `MOLLIE_REDIRECT_URL`, `MOLLIE_WEBHOOK_URL`
- `S3_REGION`, `S3_BUCKET_PUBLIC_MEDIA`, `S3_BUCKET_PRIVATE_DOCUMENTS`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`
- `S3_ENDPOINT` for R2, B2, Spaces, MinIO, or another compatible service; it may be empty for AWS S3
- `S3_PATH_STYLE_ACCESS=true` where the compatible provider requires path-style addressing
- when `EMAIL_NOTIFICATIONS_ENABLED=true`: `RESEND_API_KEY` and `RESEND_FROM_EMAIL` from a verified Resend domain

Optional integrations: `GEMINI_API_KEY` (matching returns an explicit unavailable/error outcome when absent; no fake AI result), Mollie reporting credentials, and the documented pool/scan tuning variables in `.env.example`.

Production startup fails on missing database settings, weak JWT material, unsafe frontend/CORS URLs, incomplete S3 settings, shared public/private buckets, missing Mollie configuration, or incomplete enabled email configuration. Logs disclose only whether Mollie is configured and its inferred `test`, `live`, or `unknown` mode—not the key.

## Database and migrations

Hibernate is `validate`; Flyway is enabled with only `classpath:db/migration/common`. `baseline-on-migrate` is disabled by default, migration errors stop startup, and the application never runs Flyway repair. The local profile additionally loads `db/migration/dev`; production must never do so.

Use managed PostgreSQL backups where possible: daily backups, at least 30 days retention, point-in-time recovery when supported, and a restore drill at least quarterly. A restore drill should create an isolated database, restore the selected backup, start this exact application release with Flyway validation, and run health plus representative read-only checks. Never restore over production as a test.

## Object storage

Public venue media and private financial documents require distinct buckets and credentials scoped to only the necessary operations. The application generates media keys and document references; clients cannot select buckets or arbitrary keys. Private documents have no public ACL or public URL and are returned only through authorized receipt/statement endpoints.

Enable provider durability/redundancy and versioning where appropriate. Do not configure lifecycle deletion for financial documents without an approved legal retention policy. Venue-media orphan cleanup is deferred until references can be reconciled safely. A manual MinIO smoke test can use two buckets, path-style access, an upload, an authorized PDF read, and an unauthenticated request proving the private bucket/object is inaccessible.

## Health, traffic, and monitoring

- Public: `/api/actuator/health`, `/api/actuator/health/liveness`, `/api/actuator/health/readiness`; details remain hidden.
- Metrics: `/api/actuator/metrics/**`, ADMIN-authenticated and preferably network-restricted by the platform.
- Every response carries `X-Request-ID`; safe caller IDs are propagated and otherwise a UUID is generated and put in MDC.
- Send container logs to managed logging/OpenTelemetry or Sentry. Alert on 5xx rates, restart loops, database readiness, failed jobs, and existing workflow gauges. Never collect Authorization headers, tokens, request bodies, or email bodies.

Apply public endpoint and login/register/refresh/match/availability/upload limits at the edge using an IP-aware distributed proxy/WAF. An in-memory limiter was intentionally not added because it would be inconsistent across replicas. Do not throttle Mollie webhooks in a way that defeats legitimate retries. Configure an edge request-body limit no higher than the backend's 6 MB limit.

## Release and rollback

Build the Docker image from a reviewed commit; the Docker build runs tests and the runtime is non-root. Deploy migrations before or with compatible application code. Migrations must be backward-compatible for rolling deploys. Roll back by redeploying the prior image; do not automatically undo or repair Flyway migrations. Restore the database only for an actual data-loss incident under an approved runbook.

## First-deployment checklist

- [ ] Provision PostgreSQL
- [ ] Verify backups
- [ ] Configure object storage
- [ ] Configure JWT secret
- [ ] Configure frontend origin
- [ ] Configure Mollie live/test credentials
- [ ] Configure webhook
- [ ] Configure Resend domain/key
- [ ] Configure frontend URL
- [ ] Run Flyway on clean/target database
- [ ] Health UP
- [ ] Create test owner/scout
- [ ] Upload venue image
- [ ] Configure availability
- [ ] Run match
- [ ] Create booking request
- [ ] Accept
- [ ] Mollie sandbox payment
- [ ] Webhook confirms
- [ ] Receipt download
- [ ] Cancellation/refund sandbox check
- [ ] Payout readiness check
- [ ] Admin operations access
- [ ] Private document access authorization
- [ ] Backup restore test

None of these boxes are asserted complete by this document.

## Known security and operations tradeoffs

The frontend currently stores bearer/refresh tokens in `localStorage`; a future HttpOnly-cookie/BFF design would reduce XSS token exposure. Provider-side rate limiting, WAF rules, centralized error monitoring, object-storage retention decisions, media orphan cleanup, and an automated backup restore pipeline remain deployment/platform work.
