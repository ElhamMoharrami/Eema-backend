# Pre-launch AUTH defect resolution

The original execution found BCrypt password equivalence through registration and reset (AUTH-004/AUTH-014), a stale-tab refresh failure deleting a newer session, and a PostgreSQL error on unfiltered admin booking search. The original evidence is retained in [history](evidence/auth/history/pre-launch-defects/). Current execution results are in [AUTH execution report](AUTH_EXECUTION_REPORT.md).

## Password contract

Registration and reset share Spring Security's `Argon2PasswordEncoder`; login verifies through the same `PasswordEncoder` bean. It produces `$argon2id$v=19$...` PHC strings containing algorithm, version, cost, random salt and hash. A `DelegatingPasswordEncoder` prefix adds no necessary information here: there is one supported algorithm and no legacy fallback. No custom cryptography was introduced. Spring's implementation requires Bouncy Castle; the only new dependency is `bcprov-jdk18on:1.84`. See [Spring password storage](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html) and [Bouncy Castle release](https://www.bouncycastle.org/resources/new-releases-bouncy-castle-java-1-84-and-bouncy-castle-java-lts-2-73-11/).

The existing Jakarta `@Size` policy remains 8–72 Java string characters (UTF-16 code units), with `@NotBlank`. This is not a 72-byte restriction. Passwords are not trimmed, case-folded or Unicode-normalized. Supplementary characters count as two UTF-16 units; this existing boundary has not been redesigned. The browser retains its existing limits.

BCrypt's key schedule used the first 72 UTF-8 bytes even when validation accepted a longer byte sequence. Two accepted 72-character secrets with a shared 36-character `é` prefix consequently authenticated equivalently. Regression tests now require distinct authentication in both directions and after reset. ASCII surrounding spaces remain significant.

Default parameters: 65,536 KiB memory, 3 iterations, parallelism 1; random 16-byte salt and 32-byte hash. Override with `app.password.argon2.memory-kib`, `.iterations`, `.parallelism` (Spring environment equivalents supported). Startup rejects memory below 19,456 KiB, iterations below 2 or parallelism below 1. Benchmark five encode/match samples after one warmup using the same Spring constructor: approximately 930/915 ms on this local machine; see [measured values](evidence/auth/argon2-benchmark.json). This is a local observation, not a production performance guarantee. Budget memory per concurrent operation and validate staging latency under expected load.

`users.password_hash` is TEXT: no common schema migration. Development-only `V103__argon2_seed_passwords.sql` updates only the two immutable seed IDs with matching seed emails. Existing V100 checksums and referential integrity remain intact. Seed password remains the documented development credential. No staging database or volume was reset. Other disposable local BCrypt accounts need a fresh registration or the existing password-reset flow; their old passwords are intentionally unsupported. Do not deploy to a persistent environment expecting old accounts to continue working without planning their reset.

## Refresh generation semantics

A non-secret UUID stored with each successful login/refresh now identifies its generation. Recovery compares that metadata; raw tokens are used only as credentials. Older cached sessions use their stored timestamp as a transitional generation until the next successful save. Before refreshing, a request notices an already superseding session. On failure it re-reads storage: a different generation is preserved and notified to this tab's subscribers; the failed current generation is cleared. The request retries at most once. A success arriving after another login/rotation/logout is also prevented from overwriting that newer state.

There is still no cross-tab refresh mutex: the server correctly permits exactly one rotation of a shared token. Separate logins remain independent sessions. Client-only logout and existing access-JWT lifetime after reset remain unchanged.

The deterministic client suite uses deferred transport barriers, not timing sleeps. It covers genuine failures, winner-before-loser, subscriber synchronization, subsequent genuine failure of the newer generation, bounded retry, same-tab coalescing, and stale success after logout/login. The actual two-tab browser runner pauses refresh dispatch and releases the winner first.

## Admin search

The old JPQL used nullable `:parameter IS NULL OR ...` expressions; PostgreSQL could not type an absent UUID parameter. `BookingRepository.adminSearch` now uses Spring Data specifications and adds equality predicates only for supplied values. With no filters it naturally selects all admin-visible bookings. Existing bounded pagination and descending `createdAt` ordering remain. No booking lifecycle or financial logic changed.

`AdminBookingSearchIntegrationTest` exercises the HTTP ADMIN boundary with real PostgreSQL: absent filters, all seven individual filters, all combined, incompatible filters, two pages, order, and anonymous/SCOUT/OWNER denial.

The subsequent [owner-page auth recovery fix](OWNER_AUTH_RECOVERY.md) also handles stale403 reads without reload and separates owner/admin forbidden copy. Backend application code was unchanged in that follow-up.
