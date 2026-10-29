# Phase A policy and incident contracts

This infrastructure stores and audits externally approved policy content. It does not supply legal terms. Production has no automatically published policies: approved content for every required policy type must be created and explicitly published by an administrator before marketplace launch.

## Policy UX and API

The future frontend should provide `/policies` (or an equivalent blocking modal) and show “Review and accept updated Eema policies” before a transactional action when `GET /api/policies/status` returns `allRequiredAccepted: false`.

- `GET /api/policies/current` returns the current published required policy documents and an `accepted` flag.
- `GET /api/policies/{type}/current` returns one current document.
- `GET /api/policies/status` is authoritative for the user's current acceptance state.
- `POST /api/policies/accept` accepts `{ "policyVersionIds": ["uuid"] }`. IDs must be exact current published versions; time is server supplied and duplicates are idempotent.
- `GET /api/admin/policies`, `POST /api/admin/policies`, `POST /api/admin/policies/{id}/publish`, and `POST /api/admin/policies/{id}/retire` support the minimal admin workflow.

Creating a draft accepts `policyType`, `version`, `title`, `effectiveAt`, and exact `content`. Publishing computes the SHA-256 hash and retires the prior current version of that type. There is no edit or delete endpoint. The frontend must treat content as plain text/approved Markdown and must sanitize Markdown rendering; the backend never accepts rendered HTML as policy source.

All five types are technically configured as required. Whether `PRIVACY_POLICY` is presented as explicit acceptance or notice/acknowledgement is intentionally not a legal determination; counsel must decide the final wording and UX semantics. The stored record supports either presentation.

## Incident UX and API

Booking detail can offer “Report an issue”. Admin operations can provide `/admin/incidents`.

- `POST /api/bookings/{bookingId}/incidents` accepts only `{ "category": "...", "description": "..." }`.
- `GET /api/bookings/{bookingId}/incidents` returns participant-safe history and omits the admin resolution note.
- `GET /api/admin/incidents?status=&category=&bookingId=&page=&size=` is pageable; `GET /api/admin/incidents/{id}` includes the resolution note.
- `POST /api/admin/incidents/{id}/start-review`, `POST /api/admin/incidents/{id}/resolve` with `{ "resolutionNote": "..." }`, and `POST /api/admin/incidents/{id}/close` are the only state transitions.

Incident creation and resolution do not call cancellation, refund, payout, liability, or venue-moderation services. Evidence upload is explicitly deferred; evidence must not be placed in public venue-media storage. Operational email is also deferred because the current configuration has no dedicated admin incident recipient/channel. Internal notes are not emailed.

## Decisions still required

Externally approved text, titles, version identifiers, effective dates, the required-policy set by role/action, privacy notice-versus-acceptance UX, whether safe resolution summaries should be shared with participants, an incident operations email/channel, retention rules, and any later private evidence workflow remain business/legal/operational decisions.
