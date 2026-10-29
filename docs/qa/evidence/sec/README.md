# SEC local evidence

Synthetic users, disposable PostgreSQL and controlled failures. Evidence summaries omit credentials, auth responses and raw request bodies. History retains pre-fix session/client/browser results and logging failures using synthetic markers only.

The two logging tests independently execute production handlers/JJWT and the HTTP filter chain. A test-only endpoint injects an unexpected exception cause; no real provider leak is claimed. The session client harness executes the actual TypeScript parser. The browser uses the rebuilt production frontend and real local APIs, with a transport bridge to the disposable test server.

Existing ownership, private-document, DTO, notification-escaping and financial authorization tests remain authoritative cross-domain evidence. Full regression re-executes them; no universal verified-email prerequisite or generic actor matrix is invented.

CORS/headers are verified through the local Spring filter chain; a secure mock request tests enabled HSTS, not real TLS deployment. Public health may list probe groups but not components/details. Swagger remains authenticated. Unknown actuator endpoints are unavailable. Bad CR/LF headers remain rejected by the firewall, now with safe400 handling.

`frontend.json` records actual typecheck/lint/build success. Client summaries separately record26 session cases,13 AUTH,7 INC and6 policy regressions. Final command totals and document-check metadata are added only after completion. Staging remains unverified.
