# DOC validation evidence

- `history/`: original generated scenarios, baseline/reproduction failures, harness-only corrections and images proving clipping before the fix.
- `focused-unit.json`: 21 passing storage/rendering/service cases.
- `focused-postgres.json`: 33 passing DOC PostgreSQL cases plus the explicitly executed browser launcher.
- `browser.json`: eight passing real browser checks. PDF responses traverse the real local service and private filesystem; CDP captures safe download metadata, not auth credentials.
- `wide-receipt.png`, `long-statement.png`, `unicode-receipt.png`: reviewed synthetic generated documents. The final image makes the unresolved unsupported-script identity substitution visible.
- `receipt-control.png`, `statement-control.png`, `missing-document-error.png`: reviewed representative UI states using fictional QA accounts.
- Full command totals and completion metadata are written only after successful runs. Real S3, real payment/email providers, deployed storage durability and legal/tax approval are not claimed.

The initial contact setter compile failure and incomplete REPLACED-destination fixture were harness defects. Four invalid-delete S3 mock failures arose because Mockito did not execute an SDK default Consumer method; production S3 validation was unchanged. The original glyph-bound probe also included a valid footer in its count; the bound was corrected, and independent visual evidence proves the actual right-edge clipping. Historical results are retained, not relabeled as final successes.
