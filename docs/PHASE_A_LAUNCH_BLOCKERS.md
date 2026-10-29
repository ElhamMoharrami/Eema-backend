# Phase A launch-blocker matrix

Status meanings:

- **IMPLEMENTED** — product support exists in code.
- **VERIFIED** — exercised across the relevant deployed staging boundaries.
- **BUSINESS/LEGAL PENDING** — an approved policy or business decision is required.
- **OPERATIONAL PENDING** — deployment, credentials, provider setup, monitoring, or an operational drill is required.

| Area | Implementation | Staging verification | Remaining classification |
|---|---|---|---|
| Terms of Service versioning/acceptance | IMPLEMENTED | Not deployed | BUSINESS/LEGAL PENDING: approved text; OPERATIONAL PENDING: staging publish/accept flow |
| Privacy notice | IMPLEMENTED | Not deployed | BUSINESS/LEGAL PENDING: approved notice and acknowledgement semantics |
| Acceptable Use Policy | IMPLEMENTED | Not deployed | BUSINESS/LEGAL PENDING: approved text |
| Production Policy | IMPLEMENTED | Not deployed | BUSINESS/LEGAL PENDING: approved production/content rules |
| Insurance Requirements | IMPLEMENTED | Not deployed | BUSINESS/LEGAL PENDING: approved requirements |
| Policy transaction gates and booking snapshots | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: deployed stale-version and historical-booking verification |
| Incident intake/admin lifecycle | IMPLEMENTED | Not deployed | BUSINESS/LEGAL PENDING: retention and participant communication policy; OPERATIONAL PENDING: deployed workflow |
| PostgreSQL migrations and constraints | IMPLEMENTED | CI VERIFIED against PostgreSQL 16; not staging-verified | OPERATIONAL PENDING: managed staging migration and health evidence |
| PostgreSQL backup/restore | Runbook documented | Not verified | OPERATIONAL PENDING: provision backup access and complete isolated restore drill |
| Authentication and refresh rotation | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: HTTPS registration/login/refresh test |
| Verification and recovery email | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: Resend domain/key, test inboxes, delivery and link checks |
| Owner verification | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: end-to-end admin review and lifecycle email |
| Payout destination encryption/review | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: staging keys and fictional/test IBAN workflow |
| Venue creation/moderation | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: user workflow and S3-backed image verification |
| Matching | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: Gemini staging decision/key or documented disabled behavior |
| Booking request/acceptance | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: competing request and acceptance rehearsal |
| Mollie checkout/payment/webhook | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: TEST credentials and public HTTPS callback |
| Cancellation/refund | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: second TEST payment and real Mollie TEST refund |
| Late-payment safety | IMPLEMENTED and automated-test covered | Not provider-verified | OPERATIONAL PENDING: supported Mollie TEST rehearsal if feasible |
| Payout readiness/fee reconciliation | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: Mollie reporting capability in TEST mode must be confirmed |
| Receipts and payout statements | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: private-bucket and authorization checks; statement requires legitimate PAID staging convention |
| Public/private S3 storage | IMPLEMENTED | Not deployed | OPERATIONAL PENDING: distinct staging buckets, scoped credentials, access-policy verification |
| HTTPS/CORS/security headers | IMPLEMENTED at application layer | Not deployed | OPERATIONAL PENDING: TLS/edge response verification |
| Rate limiting/WAF | Application intentionally delegates to edge | Not configured | OPERATIONAL PENDING: select/configure edge controls without impairing Mollie retries |
| Durable logs/alerts/metrics | Application emits safe logs and secured metrics | Provider not selected | OPERATIONAL PENDING: retention, health monitoring, alerts, and observability selection |
| Restart persistence | Persistence architecture implemented | Not deployed | OPERATIONAL PENDING: restart rehearsal with representative records |
| Multi-instance operation | Database locks and shared-storage design implemented | Not deployed | OPERATIONAL PENDING: select platform and perform two-replica smoke check where supported |
| Frontend production build | IMPLEMENTED | VERIFIED locally with HTTPS staging-shaped API URL | OPERATIONAL PENDING: real frontend/backend deployment and browser rehearsal |
| Frontend dependency audit | Build succeeds; `npm install` reports 10 advisories | Not remediated in this operational task | OPERATIONAL PENDING: review 9 high and 1 critical advisory, determine runtime exposure, and apply/test deliberate upgrades before private alpha |

## Private alpha decision

**NOT READY / NO-GO until the operational and business/legal blockers above are resolved.** Code completion and CI database tests do not substitute for the deployed provider-boundary rehearsal required for Phase A.
