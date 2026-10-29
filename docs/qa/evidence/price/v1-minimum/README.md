# Approved Eema v1 minimum booking total

Historical report and former zero-checkout characterization are preserved in `history/`. `reproduction.json` records two independently failing rejection assertions before the fix. `focused-unit.json` and `focused-postgres.json` contain passing focused summaries. `database-audit.json` contains only aggregate data from the existing local development database. `frontend-message.json` proves the existing client formatter retains the safe HTTP 400 message; frontend production code is unchanged.

Full command summaries and completion metadata are emitted only after successful runs. Real payment acceptance/email delivery and deployed historical data are not claimed from local mocks. No credentials, provider bodies or personal records are retained here.
