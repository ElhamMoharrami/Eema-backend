# MATCH execution report

Pre-change audit / inventory, 2026-09-14. **Complete:32 local scenarios PASS,0 FAIL,0 locally BLOCKED. Six defects fixed.** Final regression/package verification is recorded below. Earlier domain changes are preserved.

## Authoritative pipeline (audited before changes)

Authenticated POST /api/match (all roles, no email/policy/owner-review prerequisite); brief required 10–1000 characters even for direct venueId. Anonymous denied by authenticated catch-all. Gemini parser receives explicit Europe/Amsterdam reference date. ParsedBrief then drives ACTIVE SQL predicates (size, neighbourhood/options, unstructured tags, production facts), Java production evaluator, first 30 ordered candidates, exact whole-interval availability delegation, and Gemini ranker (at most 30 input, five output). Ordering is hourly rate ascending, size descending, name ascending; ties unspecified. Budget is context, not a price predicate. No availability check for incomplete timing. No matches means no ranker call; parser has already run.

Direct venueId takes precedence over brief parsing, uses VenueService detail access, score1, current hourly/minimum/full-day pricing labels, no Gemini, no reservation or total quote. Own OWNER/ADMIN may access non-ACTIVE detail; unrelated users may not. Booking has no matching bypass.

Hard production fields: activity, crew, eight Boolean capabilities, parking spaces, ceiling height. Explicit prohibition wins; permitted null/empty fails an activity requirement, prohibited null does not override known permission. Parser null/false means no positive requirement; venue null fails a true hard requirement. Minimum parking spaces including zero requires parkingAvailable=true. Five preferences (natural light, blackout, loading, parking, lift) never eliminate. Other aesthetic/lighting/amenity/indoorOutdoor fields are ranking context, not guaranteed deterministic requirements. Structured feature tags are ignored by SQL rather than treated as capability evidence.

Parser prompt requires exact timing only for complete unambiguous date/start/end; constructor allows partial timing but availability intentionally does not run. Unknown scalar defaults null, lists empty. Existing mapper may coerce scalar types: reproduce. Ranker clamps numeric scores, coerces malformed values, sorts/limits before candidate membership lookup, and does not deduplicate: reproduce. Unknown IDs cannot create inventory. Prompt facts are a restricted catalogue DTO with no owner/contact/financial fields. Free-text brief and owner description/rules/power text are sent to Gemini. Prompt constraints cannot prove live semantic truth of explanations.

No parser/ranker fallback is implemented: failures propagate AiServiceException to generic500. Gemini transport retries429/5xx/network three times; JSON fences stripped at transport boundary. Key is currently in URL, exceptions retain causes, and parser logs raw JSON at debug/error: audit redaction. No search persistence or email. Candidate result is bounded but SQL currently materializes all predicate matches before Java limit; performance risk to assess.

## Pre-change inventory

| ID | Generated title | Priority | Existing automation | Environment / human limit |
|---|---|---|---|---|
| MATCH-001 | Parser extraction: productionActivity | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-002 | Parser extraction: numPeople | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-003 | Parser extraction: requiresNaturalLight | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-004 | Parser extraction: requiresBlackout | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-005 | Parser extraction: requiresLoadingAccess | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-006 | Parser extraction: requiresParking | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-007 | Parser extraction: minimumParkingSpaces | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-008 | Parser extraction: requiresWheelchairAccessibility | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-009 | Parser extraction: requiresLift | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-010 | Parser extraction: requiresBathrooms | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-011 | Parser extraction: requiresSoundRecordingSuitable | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-012 | Parser extraction: minimumCeilingHeightM | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-013 | Parser extraction: requestedDate | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-014 | Parser extraction: requestedStartTime/requestedEndTime | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-015 | Parser extraction: relative timing | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-016 | Parser extraction: ambiguous timing | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-017 | Parser extraction: budget context | P1 | GeminiBriefParserServiceTest (4 examples) | Parameterized mock-provider UNIT; live extraction quality separate |
| MATCH-018 | Hard requirement activity | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-019 | Hard requirement maxCrewSize | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-020 | Hard requirement naturalLight | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-021 | Hard requirement blackoutPossible | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-022 | Hard requirement loadingAccess | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-023 | Hard requirement parkingAvailable | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-024 | Hard requirement parkingSpaces | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-025 | Hard requirement wheelchairAccessible | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-026 | Hard requirement liftAvailable | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-027 | Hard requirement bathroomsAvailable | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-028 | Hard requirement soundRecordingSuitable | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-029 | Hard requirement ceilingHeightM | P0 | VenueHardRequirementEvaluatorTest + VEN PostgreSQL | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-030 | Preference ranking and zero candidates | P1 | GeminiAiServiceTest / GeminiVenueRankerTest / VenueMatchAvailabilityFilterTest | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-031 | Availability-aware match and direct quote | P1 | GeminiAiServiceTest / GeminiVenueRankerTest / VenueMatchAvailabilityFilterTest | UNIT + real PostgreSQL; no manual predicate checks |
| MATCH-032 | Ranker malicious/malformed response | P1 | GeminiAiServiceTest / GeminiVenueRankerTest / VenueMatchAvailabilityFilterTest | UNIT + real PostgreSQL; no manual predicate checks |

## Generated corrections

Parser fixtures have no actors, ownership, email or policies. API matching requires authentication, not marketplace commitment prerequisites. Direct quote visibility differs from ACTIVE-only natural-language candidates. Remove repeated UI/DB prose and fictional snapshot restore. Isolate RUN_ID venues, disposable PostgreSQL, mock Gemini transport and browser state. Mock output mapping does not demonstrate natural-language understanding. No email exists, but Gemini calls do; provider errors are not an implemented fallback. Individual32 IDs remain mapped to shared parameterized matrices.

## Parser field set and authority boundaries

ParsedBrief contains35 fields: aesthetic, lighting; sizeM2Min/Max; budgetMin/MaxEur; neighbourhood/options; indoorOutdoor; requiredAmenities; mustHaveTags/niceToHaveTags; productionActivity; numPeople; requires/prefersNaturalLight; requires/prefersBlackout; minimumCeilingHeightM; requires/prefersLoadingAccess; requires/prefersParking; minimumParkingSpaces; requiresWheelchairAccessibility; requires/prefersLift; requiresBathrooms; requiresSoundRecordingSuitable; requestedDate/start/end; timingDatePreferences; briefSummary; confidenceScore. Scalar absence stays null, four list fields normalize to empty. Only five dedicated preference Booleans exist; no negative-capability or wheelchair/bathroom/sound preference Boolean was invented.

SQL additionally interprets size bounds, neighbourhood/options (case-insensitive equality) and unstructured must-have tags. requiredAmenities, indoorOutdoor, aesthetic, lighting and niceToHaveTags are ranking context. This is a limitation of the current typed model: an amenity stored only as free text has no deterministic guarantee. Structured tags are ignored, not promoted to requirements. A model that misclassifies natural language can therefore still produce a wrong structured intent; local containment starts from ParsedBrief and cannot certify model comprehension.

Provider data-type validation is independent of prompt wording. The strict provider-type boundary now rejects string-to-number/Boolean, fractional integer and non-string list-item coercion. Dates/times retain Jackson's strict local formats. Unknown fields remain ignored under the application mapper; they cannot assign venue/user state. Partial timing is tolerated as context but does not invoke availability. No local English parser, approximate window or new numeric product cap was added. Java Integer representability bounds people/parking; positive metres use BigDecimal. Confidence is numeric0–1 and finite at the provider boundary.

## Provider and privacy findings

Gemini gets the brief, structured intent, and up to30 catalogue candidate DTOs: identity/name/neighbourhood/size, current pricing metadata, production fields/rules/power/tags/description. It does not receive user accounts, owner contacts, payment/bank data, booking contacts or private files. Users can enter personal data in the brief and owners can enter it in catalogue text: privacy disclosure/operations guidance is still required. Prompts label both as untrusted data and JSON-encode the original brief; this does not prove universal prompt-injection resistance.

API keys now use the documented `x-goog-api-key` header rather than a URL query: [Google Gemini API reference](https://ai.google.dev/api). The configured model identifier was not changed or certified live. Transport connect/read limits remain10s/60s, up to three attempts for429/5xx/network with bounded exponential backoff. Matching has no implemented fallback: parser or ranker failure returns safe502 with retry wording. No raw provider cause/body is logged by the matching exception handler; parser raw JSON and parsed-brief debug logging were removed. No credentials were used for local tests.

Free-form explanation semantic truth remains a live-model limitation: Java validates candidate IDs, scores and data types, but cannot prove that arbitrary prose makes no unsupported promise. React renders explanation as text, not HTML. Authoritative prices and capabilities in cards come from the persisted venue, never model-generated quote fields. A match does not reserve; AVL's real concurrent acceptance/stale request tests remain the integration proof for subsequent booking authority.

## Defects and fixes

| ID | Severity | Reproduction / actual | Expected / narrow correction |
|---|---|---|---|
| MATCH-D01 | P1 | Controlled provider fractional people, numeric/Boolean strings and numeric tag entries silently coerced into requirements | Reject wrong JSON types before binding; no fabricated typed constraints |
| MATCH-D02 | P2 | Same eligible ID returned twice; five unknown IDs consumed the result limit and hid a valid candidate | Membership filter before result limit; deduplicate after score sorting, highest score retained |
| MATCH-D03 | P1 | Nonnumeric/null/string/overflow scores silently coerced or accepted; malformed rows inconsistently skipped | Require numeric finite scores, valid row/UUID shape; preserve existing finite numeric clamping0–1; safe502 for malformed output |
| MATCH-D04 | P1 | Parser error logged raw JSON; generic handler logged provider cause (which may contain key URL/raw body), and provider failure became generic500 | Remove raw payload logging, use API-key header, dedicated safe502 handler without logging provider cause. No fallback invented |
| MATCH-D05 | P1 | Real PostgreSQL eligible result failed with LazyInitializationException because ranker unnecessarily mapped owner-containing VenueDto | Use existing catalogue-only VenueMapper.toListDto; no owner lazy load, no database transaction held across provider calls |

The initial108-case provider suite recorded17 failures before fixes. The first PostgreSQL execution reproduced D05. Contradictory-activity DB fixture failures are harness corrections, not defects. No hard requirement, availability, price or booking rule was relaxed. The ranking prompt's contradictory blanket availability sentence was aligned with its already-existing exact-interval context; no new availability claim is permitted beyond that check.

## Database, availability and performance scope

The shared122-case matrix covers all eight hard Booleans crossed with requirement null/false/true and venue null/false/true, inclusive numeric boundaries, parking minimum zero and seven activities with allowed/unknown/empty/prohibited variants. Seven contradictory lists are impossible in PostgreSQL and assert the disjoint-list constraint; the remaining rows compare Java eligibility with actual SQL candidate results. Typed PostgreSQL enum arrays are used, not H2.

Real matching availability cases use persisted weekly intervals, AVAILABLE/BLOCKED overrides and booking statuses. REQUESTED does not block; AWAITING_PAYMENT/CONFIRMED do. Full containment succeeds; partial/empty/blocked/lead-clipped fails. Date-only/start-only/missing-end contexts retain eligibility and skip exact filtering. Zero eligible results skip the second provider call. Existing AVL proves exclusion constraints, lead/DST algebra and stale-request rejection after another scout reserves; matching adds no locks or independent scheduling algebra.

Candidate order is current hourly price ascending, size descending, name ascending; no tie-breaker guarantee. The test seeds33 matching candidates and proves only30 reach the next layer. SQL materializes predicate matches before Java's limit, so database materialization is not bounded30. Availability calls are bounded30 candidates × one requested date (roughly four repository reads per candidate, up to about120 availability reads plus candidate retrieval; source-derived count, not a load benchmark); ordinary successful search makes two Gemini calls, no-candidate search one, direct quote zero. No query optimization was introduced without evidence of an alpha-blocking performance issue. Growth/load testing and edge cost controls remain deployed operations work.

## WHAT ELI SHOULD TEST PERSONALLY

- Does natural-language search and preference ordering feel useful, with believable explanations?
- Are empty/error results helpful without implying that requirements were silently relaxed?
- Is it clear that a match is not a reservation or a guarantee?

## LIVE GEMINI / STAGING TESTS STILL REQUIRED

- Real credentials/model availability, latency, outage/retry behavior, rate limits and cost controls at the deployed edge. No in-memory limiter added.
- Curated explicit/ambiguous activity, crew, hard-versus-preferred, parking-minimum, relative-date and approximate-timing briefs; unusual phrasing and injection attempts through brief and venue text.
- Check output stability, unsupported capability/price/availability claims, logs/redaction and privacy disclosure for text sent to Gemini.
- Measure field extraction accuracy, hard/preference classification, timing accuracy and invented-constraint rate on a small fixed dataset. Record model/prompt version and reference date; choose acceptance thresholds before alpha. Mocks do not count as successful model evaluations.

## PRODUCT / AI DECISIONS

Supported languages; negative requirements; deterministic coverage for generic amenities/indoorOutdoor; aesthetic taxonomy; duration-aware budget ranking; approximate timing policy; explanation tone and acceptable unsupported-claim rate; whether to introduce a ranker-only fallback; and live evaluation thresholds. The current behavior returns a safe error rather than guessing a parser fallback. Catalogue growth/query materialization and privacy guidance for user/owner text need operations ownership.

## Execution evidence and fixture corrections

The focused real PostgreSQL suite passed138 invocations:122 shared SQL/Java/constraint cases,12 availability cases and four pipeline/auth/bound/error groups. Initial harness corrections included booking snapshot/contact/deadline NOT NULL fields, removing nonexistent bookings.updated_at, comparing PostgreSQL array-containing rows through row_to_json text, numeric price comparison instead of JSON decimal scale, and retaining the established static Testcontainers lifecycle. These corrections did not alter production rules. Test-only internal booking rows are read-model fixtures; actual reservation transitions and races are reused from AVL rather than reimplemented here.

Thirty candidates are selected before exact availability checks. Thus an available31st candidate may not be searched after the first30 fail availability; this is the existing recall/performance tradeoff, not silent hard-filter relaxation. Parser and ranker prompt schema assertions are deterministic; the live evaluation matrix remains NOT_RUN_LIVE.

### Browser-discovered frontend boundary defect

MATCH-D06 (P2): the shared API client rejected every authenticated non-GET request for an unverified-email user before HTTP. Browser evidence showed “Verify your email before continuing” on search even though PostgreSQL/API tests proved Spring permits authenticated discovery. The fix exempts only exact POST /match from that client commitment guard. Authentication, actor consistency, refresh handling and all other mutation guards remain. Seven runtime client checks prove all three authenticated roles can search, three unrelated mutations remain guarded, and anonymous matching remains denied. `before-unverified-discovery.png` records the reproduction. This was not resolved by artificially verifying the browser scout.

## Browser result and local validation

Six real browser groups passed: loading/results; full venue detail; zero hard candidates; exact unavailable time; safe provider failure; network failure. The scout remained unverified and without policy acceptance, proving discovery prerequisites after D06. Script text appeared literally and did not execute. Cards displayed persisted125EUR hourly pricing. Browser setup used isolated PostgreSQL and test-only catalogue rows; no live Gemini or production legal content. Results screenshot was visually inspected.

| Verification | Result |
|---|---|
| `mvn test` | PASS:618, no failures/errors/skips |
| Focused matching PostgreSQL | PASS:138 |
| Full `mvn verify -Ppostgres-integration` | PASS:377 executed, zero failures/errors; one opt-in browser test skipped here and passed separately |
| Repeated provider/transport matrix | PASS:121 |
| Opt-in real Spring/PostgreSQL browser | PASS:1 test, six journey groups |
| Runtime client guard checks | PASS:7 |
| Frontend typecheck / lint / build | PASS after the client fix |
| `mvn clean package` | PASS:618 tests and packaged JAR |
| Backend/frontend `git diff --check` | PASS |
| Master-plan PDF | PASS:659 pages; MATCH pages57–64; page64 visually inspected |

**Scenario accounting:**32 PASS /0 FAIL /0 locally BLOCKED. Six defects fixed (four P1, two P2); no P0 hard-filter/inventory bypass was found. Live model quality is **NOT RUN LIVE**, not a software PASS. The full PostgreSQL regression, provider repeat, clean package, PDF and whitespace outcomes are saved in `evidence/match/final-validation.json`. Counts overlap across commands and must not be summed.

**Local readiness:** matching is clear to proceed to the next software domain under the documented contract with all required local checks green. Live-provider quality, explanation truthfulness, privacy disclosure and deployed cost/rate controls still require the separate staging/product review; local mocks do not approve an AI launch.

Cleanup completed: task-owned frontend server and Chrome stopped, isolated profiles/session files removed, disposable test databases closed; shared history untouched. Sanitized JSON was parsed and checked for credential/token patterns. See [MATCH_QA_GUIDE.md](MATCH_QA_GUIDE.md) for reproducible commands and [final validation evidence](evidence/match/final-validation.json).
