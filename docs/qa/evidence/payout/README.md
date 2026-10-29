# PAYOUT local validation evidence — 2026-09-19

This evidence records local application behavior on disposable PostgreSQL and a local production frontend build. It does not verify real Mollie, bank transfers, fee reports or deployed workers.

- `baseline.json`: initial four PostgreSQL checks; two financial list failures and unpaid cancelled fee acceptance.
- `reproduction.json`: independent expanded run; three assertion failures plus destination constraint failure and bounded readiness-worker completion failure.
- `lock-destination-reproduction.json`: separate confirmation of the destination and worker defects.
- `focused-unit.json`: 40 payout/liability unit cases, including 28 new fixed-clock/precedence/ineligible-state cases.
- `focused-postgres.json`: first green run of 23 payout PostgreSQL cases. The later full regression also includes a worker equality case and the additional destination-B assertion.
- `browser.json`: final ten-check browser/client result. Real payout conflicts use the local API; the liability WAIVED conflict is a controlled client response, because no public waive endpoint exists.
- `record-payment.png`, `conflict-refetch.png`, `owner-paid.png`: representative screenshots, visually reviewed for content and clipping.
- `history/`: generated scenario baseline and pre-fix browser evidence. Historical FAIL is deliberately retained.
- `unit.json`, `postgres.json`, `package.json`: required full regression totals, generated only after each command succeeds.
- `completion.json` and `document-check.json`: final command/status and master-plan/PDF integrity checks, written at completion.

Fixtures use fictional QA actors. Tokens, passwords, provider bodies and plaintext account numbers are excluded. Raw Maven output remains outside the repository under `/tmp`.

The user-approved Eema v1 policy update has separate current evidence in [`v1-policy/`](v1-policy/README.md); original baseline/fixed-defect results above remain historical. The pre-policy report is retained in `history/pre-v1-policy/`.
