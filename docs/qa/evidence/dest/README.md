# DEST local validation evidence — 2026-09-19

Disposable PostgreSQL and synthetic bank fixtures; no actual account verification, bank transfer or provider email delivery is claimed.

- `baseline.json`: 27 cases, six reproduced race failures plus two corrected harness expectations.
- `reproduction.json`: independent 27-case run with exactly the six race failures.
- `focused-postgres.json`: 34 green cases after owner-lock fixes. Full regression additionally executes the audit-failure/POST-only reveal case.
- `history/browser-baseline.json` and `history/browser-reproduction.json`: two independent reproductions of cross-owner cached-history disclosure and retained revealed value on admin account switch; harness cleanup/disabled-Cancel errors are explicitly distinguished.
- `browser.json`: final 12 green checks on the rebuilt frontend, including held responses released after account switch/logout.
- `owner-pending.png`, `admin-masked.png`, `owner-replacement.png`: visually reviewed masked-state screenshots; never capture full revealed bank values.
- `focused-unit.json`, `unit.json`, `postgres.json`, `package.json`: command totals emitted only after successful execution.
- `frontend.json`: fresh typecheck/lint/build and visual-review evidence.

Historical generated scenarios are retained with the synthetic full IBAN redacted. Credentials, tokens, full bank values and raw provider bodies are not stored here. Raw test logs remain outside the repository in `/tmp`.
