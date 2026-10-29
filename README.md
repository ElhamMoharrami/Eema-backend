# Eema backend

## Local development

The local stack runs PostgreSQL 16 and Spring Boot in Docker. The frontend stays on the host at `http://localhost:3000`. A local PostgreSQL installation or manual database setup is not required.

### Prerequisites

- Docker with Docker Compose
- A Mollie **test** API key when testing checkout, webhooks, or refunds
- A Gemini API key when testing AI matching

Use one Docker environment consistently (for example, Docker Desktop or Docker Engine). Avoid mixing Docker contexts or using `sudo docker` with a per-user Docker Desktop context.

### Start

```bash
cp .env.example .env
# Fill in only the test/development credentials you need.
docker compose up --build
```

Compose starts `postgres:16-alpine`, waits for PostgreSQL to become healthy, then starts the backend with the `local` Spring profile. The API is available at `http://localhost:8080/api`; the backend is ready when:

```bash
curl http://localhost:8080/api/actuator/health
```

returns `{"status":"UP"}`. CORS permits the frontend origin `http://localhost:3000` by default.

Local-only database credentials are `eema` / `eema`, database `eema`. Local seed users are:

- `owner@eema-dev.local` / `password123`
- `scout@eema-dev.local` / `password123`

These credentials and the JWT secret in `.env.example` are deliberately development-only.

### Flyway and data

The local profile applies both `db/migration/common` and `db/migration/dev`. The dev migrations add test users and venues. Production applies `common` only. Hibernate runs with `ddl-auto=validate`; it does not create or repair the schema.

PostgreSQL data is stored in the Docker named volume `eema-postgres-data`. Uploaded files are stored in `eema-uploads`, mounted at `/app/uploads` in the backend container.

A normal restart preserves both volumes:

```bash
docker compose down
docker compose up --build
```

To inspect PostgreSQL without installing `psql` locally:

```bash
docker compose exec postgres psql -U eema -d eema
```

Useful commands inside `psql`:

```sql
\dt
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
SELECT * FROM bookings;
SELECT * FROM booking_events ORDER BY created_at DESC;
```

To fully reset local database and upload data:

```bash
docker compose down -v
docker compose up --build
```

**Warning:** `docker compose down -v` permanently deletes the LOCAL PostgreSQL and upload volumes. Do not use it for a normal restart.

### Mollie test payments and webhooks

Set `MOLLIE_API_KEY` to a Mollie test key. The browser never supplies the authoritative amount; checkout derives it from the booking in PostgreSQL.

Manual webhook mode needs no public callback URL. Leave `MOLLIE_WEBHOOK_URL` blank, complete the test checkout, obtain the `tr_...` payment ID from the checkout response/browser network view or Mollie dashboard, then run:

```bash
curl -X POST http://localhost:8080/api/webhooks/mollie/payment \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'id=tr_...'
```

The backend retrieves the authoritative payment from Mollie before applying any state change.

For a true end-to-end callback, expose port 8080 through a trusted tunnel service and set:

```dotenv
MOLLIE_WEBHOOK_URL=https://<public-tunnel>/api/webhooks/mollie/payment
```

Restart the backend before creating the payment. No tunnel provider or tunnel software is required by this repository. `MOLLIE_REDIRECT_URL` normally remains `http://localhost:3000/booked`.

Fee reconciliation additionally requires `MOLLIE_REPORTING_ACCESS_TOKEN` and `MOLLIE_FEE_RECONCILIATION_ENABLED=true`.

### Transactional booking email

Payment-received email is triggered only after the authoritative Mollie webhook commits `AWAITING_PAYMENT` to `CONFIRMED`. It is disabled locally by default. To test with Resend, set:

```dotenv
EMAIL_NOTIFICATIONS_ENABLED=true
RESEND_API_KEY=re_your_development_key
RESEND_FROM_EMAIL=Eema Bookings <bookings@your-verified-domain.example>
APP_FRONTEND_BASE_URL=http://localhost:3000
```

Restart the backend, create and pay a booking with a Resend-approved scout and owner snapshot address, then deliver the Mollie webhook as described above. Never commit the key. Production enables notifications by default and fails startup clearly when the Resend key or sender is absent. Set `EMAIL_NOTIFICATIONS_ENABLED=false` only for an intentional operational disable.

Email links use `{APP_FRONTEND_BASE_URL}/bookings/{bookingId}`. The frontend must provide that booking-detail route; this backend task does not add it.

### Request-to-book workflow

Scouts submit `POST /api/bookings` with `venueId`, `shootDate`, `startTime`, `endTime`, `rawBrief`, and `scoutContact`. Owners then use `POST /api/bookings/{id}/accept` or `/decline`. After acceptance, the requesting scout starts Mollie checkout with `POST /api/bookings/{id}/checkout` (no request body).

Accepted requests default to a 24-hour payment window. Configure it with `BOOKING_PAYMENT_WINDOW_HOURS`; `BOOKING_EXPIRATION_SCAN_DELAY_MS` controls how often expired approvals are released.

### Workflow operations

Actuator health remains public, while `/api/actuator/metrics` and individual metric endpoints require authentication. Workflow metrics use aggregate counters/gauges without booking IDs or user data as labels. Relevant names begin with `eema.workflow` and cover approval/inquiry expiration, late payments, automatic refunds, stuck payment/refund creation, and old unreconciled Mollie fees.

The read-only stuck-state scan emits an aggregate warning when work exceeds the configured thresholds. Tune it with `WORKFLOW_OBSERVABILITY_SCAN_DELAY_MS`, `PAYMENT_CREATING_STUCK_MINUTES`, `REFUND_STUCK_MINUTES`, and `FEE_UNRECONCILED_MINUTES`. The scan never changes booking or provider state.

### Logs, stop, and common checks

```bash
docker compose logs -f backend
docker compose logs -f postgres
docker compose down
```

Configuration avoids HTTP wire logging and must never log JWT secrets, provider API keys, Authorization headers, or refresh tokens.
