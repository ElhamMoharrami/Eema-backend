# Phase A staging launch rehearsal

Rehearsal date: 2026-09-07 (Europe/Amsterdam)

Backend revision: `74f426108a2b77702e921cfcd25da4167a956dd6`

Frontend revision: `f48cf0c74bb00b2ccd3b8070b7e304d266d81103`

Overall result: **BLOCKED — no deployed staging environment or staging provider credentials were available.** No external workflow in this document is represented as passed.

## Audit and intended architecture

The frontend has a Netlify build definition. No backend hosting provider, staging deployment manifest, infrastructure-as-code, staging domain, or managed-service binding is present in either repository. Docker Compose is explicitly local-development-only.

The production-like staging topology required for the rehearsal is:

```text
Browser
  -> https://<staging-frontend> (Netlify is configured, site is not provisioned here)
  -> https://<staging-api>/api (backend provider not selected)

Mollie TEST
  -> https://<staging-api>/api/webhooks/mollie/payment

Backend (SPRING_PROFILES_ACTIVE=prod)
  -> empty non-production PostgreSQL
  -> non-production public-media S3 bucket
  -> distinct non-production private-document S3 bucket
  -> Mollie TEST, Resend staging sender, optional Gemini
```

The backend production profile already selects common Flyway migrations only, Hibernate `ddl-auto=validate`, S3 storage, explicit CORS origins, forwarded headers, hidden health details, and HSTS. Production validation rejects unsafe/missing URLs, credentials, or shared S3 buckets.

## Available configuration

The environment did not contain staging values for PostgreSQL, JWT, HTTPS frontend/backend URLs, Mollie, Resend, S3, Gemini, Netlify authentication, or a Netlify site. The backend `.env` is local-only; Mollie, webhook, Resend, and Gemini credentials in it are empty. Secrets were not printed or copied into this document.

Required provisioning inputs:

- HTTPS frontend URL and Netlify site credentials, or an approved replacement frontend host
- approved backend container provider and HTTPS API URL
- empty non-production PostgreSQL plus backup/restore permissions
- two distinct non-production S3-compatible buckets and scoped credentials
- Mollie TEST API/reporting credentials and registered HTTPS webhook/redirect URLs
- Resend staging sender/domain and test recipient accounts
- unique staging JWT and payout-data encryption/fingerprint keys
- ADMIN allowlist and dedicated SCOUT/OWNER/ADMIN test addresses
- optional Gemini staging key if ranking is in scope

## Checks performed

| Check | Result | Evidence / limitation |
|---|---|---|
| Repository and deployment audit | PASS | Netlify configuration exists only for the frontend; no backend/staging provider exists. |
| Backend production configuration audit | PASS | `application-prod.yml` uses common migrations, schema validation, S3, explicit CORS, HTTPS validation, HSTS, and hidden health details. |
| Frontend typecheck | PASS | `NEXT_PUBLIC_API_BASE_URL=https://api-staging.invalid/api npm run typecheck` completed successfully. The `.invalid` host was deliberately non-routable and was used only for build-shape validation. |
| Frontend lint | PASS | No warnings or errors. |
| Frontend production build | PASS | Next.js 14.2.5 built all 29 static pages successfully with the HTTPS staging-shaped API value. |
| Deployed HTTPS frontend/backend | BLOCKED | No staging sites, domains, provider credentials, or backend platform were supplied. |
| PostgreSQL clean migration and health | BLOCKED | No staging database or deployed backend. PostgreSQL 16 migrations are CI-covered, but that is not deployed evidence. |
| Backup and restore drill | BLOCKED | No managed database or backup/restore access. This must use a separate disposable restore database. |
| Public/private object storage | BLOCKED | No staging buckets or credentials. Bucket separation and anonymous-private denial were not exercised. |
| Auth, email verification, password reset | BLOCKED | No HTTPS deployment, Resend key/domain, or test inbox access. |
| Staging policy publication/acceptance | BLOCKED | No deployed ADMIN/user accounts. No policy content was created or published. |
| Owner verification and payout destination | BLOCKED | No deployed environment or test users. No IBAN was submitted or revealed. |
| Venue creation, image upload, moderation | BLOCKED | No deployed system or S3 bucket. |
| Matching | BLOCKED | No deployed system or Gemini staging configuration. |
| Booking request and owner acceptance | BLOCKED | No deployed participants or venue. |
| Mollie TEST checkout/payment/webhook | BLOCKED | No TEST key or public HTTPS callback. Database state was not manually changed. |
| Receipt authorization/private storage | BLOCKED | No confirmed staging payment or private bucket. |
| Incident lifecycle and financial isolation | BLOCKED | No deployed booking. Automated verification is not a staging substitute. |
| Cancellation and real TEST refund | BLOCKED | No Mollie TEST payment/provider access. |
| Late-payment provider safety | SKIPPED | Provider tooling and a deployed short-window staging configuration were unavailable. Automated coverage remains separate evidence. |
| Payout readiness and fee reconciliation | BLOCKED | No Mollie reporting credential or confirmed staging booking. |
| Payout statement | SKIPPED | No legitimately PAID staging payout; no fake transfer was recorded. |
| Destination replacement/snapshot | BLOCKED | No deployed payout workflow. |
| Venue suspension/reactivation | BLOCKED | No active staging venue. |
| HTTPS/security headers/CORS/log review | BLOCKED | No deployed response or durable staging logs. |
| Edge rate limits and monitoring | BLOCKED | No hosting/edge/monitoring provider selected. |
| Controlled external failure | BLOCKED | No isolated staging resources on which to perform it safely. |
| Restart persistence | BLOCKED | No deployed backend. |
| Two-replica check | SKIPPED | Backend platform is not selected. |

## Execution runbook once provisioned

1. Deploy the reviewed backend image with `SPRING_PROFILES_ACTIVE=prod`; deploy the frontend with the real HTTPS API base URL.
2. Confirm public health/readiness, V1 through the latest common migration, no V100+ history or seed accounts, and Hibernate validation.
3. Create representative state, run `pg_dump`, restore into a separate disposable database with `pg_restore`, start the same backend release against it, and verify health plus representative records.
4. Prove public media delivery and anonymous denial of private documents directly at the storage and application boundaries.
5. Execute registration, verification email, refresh, `/auth/me`, password reset, and old-session rejection using dedicated inboxes.
6. Publish only policies labelled `STAGING TEST POLICY — NOT PRODUCTION LEGAL TERMS`; verify server-side gating and explicit acceptance.
7. Execute owner verification, masked/revealed/audited test payout destination verification, venue creation/upload/moderation, and matching.
8. Execute competing REQUESTED bookings, owner acceptance, real Mollie TEST checkout, public webhook confirmation, duplicate webhook, authoritative return page, and receipt authorization.
9. Execute incident lifecycle, a second paid booking cancellation/refund, payout readiness if reporting data permits it, destination replacement, and venue suspension/reactivation.
10. Review security headers, CORS, authorization boundaries, retained logs, secrets redaction, restart persistence, and—if supported—two replicas.

## Known limitations and decision

This run produced useful build/configuration evidence but **did not perform a deployed staging rehearsal**. Eema is therefore **not yet verified ready for PRIVATE ALPHA**. Provisioning and executing every BLOCKED critical-path row is a launch blocker. Legal/business inputs remain independently pending and must not be replaced by staging placeholder text.
