<a id="family-edge"></a>
### EDGE scenarios

<a id="edge-001"></a>
#### EDGE-001 — Sub-minute and midnight boundary

**Area:** EDGE · **Priority:** P1 · **Test level:** focused unit, real PostgreSQL HTTP, prior browser evidence · **Result:** PASS

**Source / corrected assumptions:** Source corrections: AVL already fixed midnight date carry and overlap lead clipping; PRICE-DEF-004 already rejects durations below one whole minute. The approved price formula is unchanged.

**Executed contract and outcome:** Fixed Clock threshold 23:59:30 yields no interval on that date and permits 00:00 on the following date. HTTP sub-minute input ending at 59.999999999 seconds fails 400 with no row/notification/provider interaction. Positive intervals of one whole minute or more preserve Duration.toMinutes truncation; residual seconds do not introduce a new billing rule. PostgreSQL TIME supports microseconds, but the current Hibernate/JDBC binding persists milliseconds; sub-millisecond round-trip fidelity is not claimed. Frontend selects HH:mm. No arbitrary second/nanosecond input ban was introduced.

**Evidence:** `docs/qa/EDGE_EXECUTION_REPORT.md`, `docs/qa/evidence/edge/`, `EdgeIntegrationTest`; reused domain reports and fresh SEC full regression. Disposable PostgreSQL with V1–V13/Hibernate validation, fixed Clock, mocked provider transport. No real provider delivery or deployed scheduling claim.

**Acceptance:** PASS only for the corrected local contract; documented unresolved defect/decision remains FAIL/BLOCKED even where characterization tests pass. No generic actor matrix, fabricated reset or automatic progression past a decision.

<a id="edge-002"></a>
#### EDGE-002 — DST missing/repeated local times

**Area:** EDGE · **Priority:** P0 · **Test level:** focused unit, real PostgreSQL HTTP, prior browser evidence · **Result:** FAIL

**Source / corrected assumptions:** EDGE-DEF-001 and EDGE-DEC-001 are unresolved. A green characterization test is not launch approval. Earlier AVL/PRICE/CANCEL/PAYOUT tests characterized Java timezone resolution separately; EDGE proves the combined persisted reservation failure.

**Executed contract and outcome:** Amsterdam 2027-03-28:02:00–02:30 and 03:00–03:30 both reach AWAITING_PAYMENT for different scouts at one venue, yet both resolve to 01:00–01:30Z. The PostgreSQL exclusion constraint uses distinct local ranges and permits both. Separately, 02:30–03:00 reaches CONFIRMED with 30 wall minutes, while its resolved end 01:00Z precedes start 01:30Z. Cancellation deadlines and payout eligible_at preserve those independently resolved instants. Autumn 2026-10-25 02:30–03:30 uses the earlier start offset, 60 wall minutes and 120 elapsed minutes. Choose explicit gap/overlap acceptance and presentation policy before changing lifecycle validation; do not change the approved pricing formula or invent an occurrence selector.

**Evidence:** `docs/qa/EDGE_EXECUTION_REPORT.md`, `docs/qa/evidence/edge/`, `EdgeIntegrationTest`; reused domain reports and fresh SEC full regression. Disposable PostgreSQL with V1–V13/Hibernate validation, fixed Clock, mocked provider transport. No real provider delivery or deployed scheduling claim.

**Acceptance:** PASS only for the corrected local contract; documented unresolved defect/decision remains FAIL/BLOCKED even where characterization tests pass. No generic actor matrix, fabricated reset or automatic progression past a decision.

<a id="edge-003"></a>
#### EDGE-003 — Matching size, neighbourhood, tags and candidate cap

**Area:** EDGE · **Priority:** P1 · **Test level:** focused unit, real PostgreSQL HTTP, prior browser evidence · **Result:** BLOCKED

**Source / corrected assumptions:** EDGE-DEC-002: decide whether the current recall/performance limitation is acceptable for v 1. Existing hard-filter contract remains verified by MATCH138 and fresh full regression.

**Executed contract and outcome:** Real PostgreSQL fixture has 31 hard-eligible studios, with case-insensitive neighbourhood and inclusive minimum-size/tag filters. The first 30 by hourly-price ordering have no availability; the 31st passes exact availability independently. Current matching returns zero because the 30-candidate cap precedes availability. Only parser transport runs; no ranker or booking side effect. No unsupported promise of exhaustive search or deterministic AI preference order is made. Changing candidate retrieval/availability order requires explicit scope; no optimization infrastructure was added.

**Evidence:** `docs/qa/EDGE_EXECUTION_REPORT.md`, `docs/qa/evidence/edge/`, `EdgeIntegrationTest`; reused domain reports and fresh SEC full regression. Disposable PostgreSQL with V1–V13/Hibernate validation, fixed Clock, mocked provider transport. No real provider delivery or deployed scheduling claim.

**Acceptance:** PASS only for the corrected local contract; documented unresolved defect/decision remains FAIL/BLOCKED even where characterization tests pass. No generic actor matrix, fabricated reset or automatic progression past a decision.

<a id="edge-004"></a>
#### EDGE-004 — Suspended request acceptance characterization

**Area:** EDGE · **Priority:** P1 · **Test level:** focused unit, real PostgreSQL HTTP, prior browser evidence · **Result:** PASS

**Source / corrected assumptions:** Historical gap G01 was fixed by VEN-D02; no new production change in EDGE.

**Executed contract and outcome:** Create REQUESTED while ACTIVE, suspend through ADMIN moderation, then accept as owning OWNER:409, identical booking row/audit/notification counts and no provider interaction. Reuse VEN suspension-versus-acceptance race evidence, BK actors and AVL inventory tests, freshly rerun in SEC full regression. Existing AWAITING_PAYMENT remains payable and CONFIRMED history is preserved.

**Evidence:** `docs/qa/EDGE_EXECUTION_REPORT.md`, `docs/qa/evidence/edge/`, `EdgeIntegrationTest`; reused domain reports and fresh SEC full regression. Disposable PostgreSQL with V1–V13/Hibernate validation, fixed Clock, mocked provider transport. No real provider delivery or deployed scheduling claim.

**Acceptance:** PASS only for the corrected local contract; documented unresolved defect/decision remains FAIL/BLOCKED even where characterization tests pass. No generic actor matrix, fabricated reset or automatic progression past a decision.

<a id="edge-005"></a>
#### EDGE-005 — Owner decline after response deadline

**Area:** EDGE · **Priority:** P1 · **Test level:** focused unit, real PostgreSQL HTTP, prior browser evidence · **Result:** BLOCKED

**Source / corrected assumptions:** EDGE-DEC-003: choose terminal status/event semantics for a still-REQUESTED booking at or after its response deadline. BK characterized current decline behavior without approving a new expiry policy.

**Executed contract and outcome:** Fixed Clock at deadline minus 1 microsecond, equality and plus 1 microsecond: decline 200/DECLINED, one REQUEST_DECLINED event and matching lifecycle event. Later worker/repeated decline cannot change or duplicate it. Worker-first at equality:EXPIRED with REQUEST_EXPIRED; later decline 409. Locking is coherent, but scheduler order decides decline versus expiry semantics. Decide whether overdue decline remains permitted or must commit expiry; no deadline cap, reminder, status or automatic cancellation was added.

**Evidence:** `docs/qa/EDGE_EXECUTION_REPORT.md`, `docs/qa/evidence/edge/`, `EdgeIntegrationTest`; reused domain reports and fresh SEC full regression. Disposable PostgreSQL with V1–V13/Hibernate validation, fixed Clock, mocked provider transport. No real provider delivery or deployed scheduling claim.

**Acceptance:** PASS only for the corrected local contract; documented unresolved defect/decision remains FAIL/BLOCKED even where characterization tests pass. No generic actor matrix, fabricated reset or automatic progression past a decision.

<a id="edge-006"></a>
#### EDGE-006 — Unsupported compatibility service paths

**Area:** EDGE · **Priority:** P1 · **Test level:** focused unit, real PostgreSQL HTTP, prior browser evidence · **Result:** PASS

**Source / corrected assumptions:** Source audit corrects generated assumptions: HeroFeature is used by the homepage; BookerVerification has no importing production component. Service presence alone does not create an API.

**Executed contract and outcome:** VenueService.getAllVenues/deleteVenue have no production controller callers; public GET delegates to getAllActiveVenues. OWNER and ADMIN DELETE /venues/{id} return 405 without deletion. Invented force-confirm/status routes return 404 without booking/event/provider effects. The existing role/domain-guarded refund endpoint is real and remains covered by REF. The independently reachable public Next Cala BFF is recorded, not certified safe or treated as owner verification; CALA-001 provider/privacy work remains unexecuted. No new UI/API was added.

**Evidence:** `docs/qa/EDGE_EXECUTION_REPORT.md`, `docs/qa/evidence/edge/`, `EdgeIntegrationTest`; reused domain reports and fresh SEC full regression. Disposable PostgreSQL with V1–V13/Hibernate validation, fixed Clock, mocked provider transport. No real provider delivery or deployed scheduling claim.

**Acceptance:** PASS only for the corrected local contract; documented unresolved defect/decision remains FAIL/BLOCKED even where characterization tests pass. No generic actor matrix, fabricated reset or automatic progression past a decision.

**EDGE gate:** 3 PASS / 1 FAIL / 2 BLOCKED. Stop automatic continuation for EDGE-DEF-001 (P0 reservation aliasing) and EDGE-DEC-001/002/003. OPS-001–009 has not started. Historical generated wording and initial harness outcomes remain in `evidence/edge/history/`.

