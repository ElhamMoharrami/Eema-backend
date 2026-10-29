# EDGE local evidence

Characterization tests execute production HTTP/services and disposable PostgreSQL. Their successful assertions can reproduce an unresolved product defect: consult completion.json and EDGE_EXECUTION_REPORT.md for scenario status. EDGE-002 is FAIL, not PASS.

Synthetic fixtures only. No raw authentication responses, provider credentials, real bank data or live payments are stored here. history preserves generated scenarios and initial harness findings. The amount-format fixture was corrected; direct SQL confirmed millisecond precision in the ORM binding. No production behavior was changed to make tests pass.

observed-outcomes.json summarizes values asserted by the suite. focused-unit.json records141 reused timing units. Focused PostgreSQL evidence records the final characterization run separately from initial failures. SEC's complete regression is reused for unchanged production and is not falsely labelled a new full run after EDGE additions.

All temporary databases are disposable; no shared staging history or existing development rows are deleted. Stop at unresolved P0 EDGE-DEF-001 and required decisions EDGE-DEC-001/002/003. OPS has not started.
