# Local operations recovery rehearsal

This procedure verifies the current packaged release using synthetic fixtures and disposable PostgreSQL/storage. It does not restore, reset or modify the shared `eema-backend` Compose project. Production backup scheduling, retention, key escrow and recovery objectives require a separate deployment procedure and owner approval.

## Database and object recovery

With local Docker running, execute from the backend repository:

```bash
mvn verify -Ppostgres-integration -Dit.test=OperationsRecoveryIntegrationTest
```

The test starts the packaged JAR, proves ordinary restart, pauses a committed notification at a debugger breakpoint and kills the isolated application, then restores a `pg_dump` into a separate database. It copies public/private files and synthetic encryption/fingerprint keys, compares all table hashes and constraints, checks unchanged receipt/media bytes, validates authorized document access and destination decryptability, and rotates an existing restored session. It also starts fresh common-only and local-development databases and verifies reset/reseed isolation. Testcontainers, child processes and temporary files are cleaned by the harness. Ports18091 and18092 must be available; the debugger binds only to loopback.

Never treat database recovery alone as full recovery: immutable document/media objects and the corresponding bank-data encryption/fingerprint keys must also be recoverable. Do not replace lost keys with new keys and assume existing encrypted data remains readable. Do not expose raw dumps, keys, credentials or bank details in QA evidence.

Optional browser restart proof uses the existing built frontend on3018 and the test's local API bridge on8080. The API bridge requires8080 to be free. The harness launches its own Chrome profile;9244 must also be free:

```bash
mvn verify -Ppostgres-integration -Dit.test=OperationsRecoveryIntegrationTest -Deema.ops.browser=true
```

## Disposable volume reset

After building the package, run:

```bash
python3 scripts/qa/ops-reset-rehearsal.py
```

The script requires a local Unix Docker socket, chooses a fresh `eema-ops-reset-*` project, validates ownership of its three named volumes, resets them, verifies marker removal and reseeding, then removes its own resources. Port18093 must be free. Database reseeding uses the packaged app; public/private named-volume deletion is verified independently with file markers. No legal policy content is seeded by common or development migrations.

Do not substitute the shared development project's name or run its destructive reset script as part of this rehearsal.

## Notification recovery limitation

Committed booking state is authoritative even when email fails. The configured bounded executor applies backpressure during normal operation, but pending in-memory events can be lost on abrupt process death. There is no durable delivery ledger, automatic replay, or reliable query listing every missing email. Booking audit events show business transitions; they do not prove provider delivery. Repeating a business mutation to resend email is unsafe and is not a recovery procedure.

OPS-008 / G08 launch acceptance remains a business/operations decision unless explicitly approved. Operators can inspect authoritative booking state and logs for a reported case; comprehensive detection or guaranteed retry must not be claimed from the current design.

## Durable notification dispatch (V14, approved OPS-DEC-001)

The PostgreSQL backup now includes `notification_outbox`. Restore it together with business tables; do not truncate it during recovery. Existing notification templates are composed in the domain transaction, with an immutable recipient/subject/text/HTML artifact and aggregate reference. Trust events use their audit-row UUID; booking/inquiry types use their one-time lifecycle transition and recipient slot. There is no new notification product scope. Historical rows from before V14 cannot be reliably reconstructed: migration does not invent or replay old emails.

`PENDING` messages are due at `next_attempt_at`; a short transaction claims one with `FOR UPDATE SKIP LOCKED`, marks `PROCESSING`, increments attempts and commits a random claim fence and lease. The bounded executor sends outside every database transaction. Completion compares the claim fence; a replaced worker cannot finish another worker's claim. Equality at lease expiry permits recovery. Defaults: batch20, one submitted/running batch per application instance,10-second polling,300-second lease,20-second HTTP request timeout,8 attempts,30-second exponential backoff capped at3600 seconds. A crash consumes an attempt; repeated lease deaths eventually become terminal rather than remaining stuck. `app.notification-outbox.*` configures these bounds. Existing bounded executor aliases and CallerRuns backpressure are preserved.

`DELIVERED` means the provider accepted the request, not inbox delivery. `FAILED` retains a sanitized failure category and appears in the ADMIN-only exception queue as `NOTIFICATION_DELIVERY_FAILED`, with outbox UUID/type/category and booking reference where applicable. No email body or recipient is exposed there. `eema.notifications.outbox{status=...}` reports aggregate counts only. Investigate sender configuration, recipient rejection or outage before any separately authorized remediation; no automatic terminal retry or new admin mutation endpoint was added. Transport/network/429/5xx failures retry; other provider4xx failures are terminal except Resend's concurrent-idempotency conflict. An unrecognized response is retryable. Disabled provider/dispatcher leaves pending messages and attempt counts intact.

The idempotency header is `eema-notification-<outbox UUID>`. Recipient, sender (fixed on first claim if originally unset), subject and content stay stable. Resend documents a24-hour key retention window: https://resend.com/docs/dashboard/emails/idempotency-keys. Provider acceptance followed by lost local completion leaves a recoverable lease. A retry after provider retention, or other provider uncertainty, can send a duplicate. This is at-least-once dispatch; it is not an exactly-once or inbox-delivery guarantee.

Authentication messages use a separate security representation. Each new token contains a32-byte random nonce plus HMAC-SHA256 over a versioned, domain-separated input using the existing JWT root secret. Token tables still contain only SHA256 hashes. The outbox contains the nonsecret nonce, token-hash/user reference and the existing template with a credential placeholder, never the HMAC credential or a credential-bearing URL. Dispatch reconstructs the exact token only in memory, verifies its hash and checks authoritative token expiry/use, active account and unchanged recipient before sending. Used/superseded/inactive-account/changed-recipient messages become `CANCELLED`; expired/missing token references fail safely. Existing opaque tokens remain redeemable through unchanged hash lookup. No expiry is extended.

Back up/recover the existing JWT secret through the existing secret-management process, separately from PostgreSQL. Root-key rotation makes previously pending derivations fail visibly with `AUTH_DERIVATION_KEY_CHANGED`; use the existing resend/reset flow after rotation. Already-issued links continue to be checked by their persisted hash. Do not log auth message content or capture token-bearing provider requests in evidence. The optional `app.resend.api-url` defaults to the official HTTPS endpoint; an HTTP loopback URL is permitted solely for local provider-stub rehearsals. Never configure a live provider in local QA.

Rollout boundary: retire/drain all pre-outbox application instances before paid traffic uses the new guarantee. A mixed deployment containing an old after-commit-only writer can still lose its own notifications; V14 cannot retroactively create trustworthy intent for those commits. New-version instances can share the outbox safely. Confirm deployed version uniformity and real sender/provider configuration in staging; the local tests do not certify deployment topology.

For an exception's outbox UUID, authorized operators can inspect routing and attempt metadata without selecting content or recipient addresses:

```sql
SELECT id, aggregate_type, aggregate_id, notification_type,
       status, attempt_count, next_attempt_at, lease_until,
       created_at, updated_at, delivered_at, last_failure_category
FROM notification_outbox
WHERE id = :notification_id;
```

Use the aggregate reference to review the existing booking, inquiry or trust workflow. This is a read-only diagnostic, not authorization to resend or alter financial/domain state. Keep message bodies and auth derivation data out of routine support exports.
