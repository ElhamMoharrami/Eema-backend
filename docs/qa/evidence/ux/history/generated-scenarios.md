#### UX-001 — Page design, navigation and state QA: /admin/bookings/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/bookings/[id] directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states OPEN, error, loading, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/bookings/[id]/page.tsx](../../Eema-frontend/app/admin/bookings/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-002"></a>
#### UX-002 — Page design, navigation and state QA: /admin/bookings

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/bookings directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/bookings/page.tsx](../../Eema-frontend/app/admin/bookings/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-003"></a>
#### UX-003 — Page design, navigation and state QA: /admin/exceptions

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/exceptions directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/exceptions/page.tsx](../../Eema-frontend/app/admin/exceptions/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-004"></a>
#### UX-004 — Page design, navigation and state QA: /admin/incidents/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/incidents/[id] directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states OPEN, RESOLVED, UNDER_REVIEW, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/incidents/[id]/page.tsx](../../Eema-frontend/app/admin/incidents/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-005"></a>
#### UX-005 — Page design, navigation and state QA: /admin/incidents

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/incidents directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states OPEN, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/incidents/page.tsx](../../Eema-frontend/app/admin/incidents/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-006"></a>
#### UX-006 — Page design, navigation and state QA: /admin/liabilities/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/liabilities/[id] directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states OPEN, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/liabilities/[id]/page.tsx](../../Eema-frontend/app/admin/liabilities/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-007"></a>
#### UX-007 — Page design, navigation and state QA: /admin/liabilities

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/liabilities directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states OPEN, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/liabilities/page.tsx](../../Eema-frontend/app/admin/liabilities/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-008"></a>
#### UX-008 — Page design, navigation and state QA: /admin/owner-verifications/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/owner-verifications/[id] directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states PENDING, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/owner-verifications/[id]/page.tsx](../../Eema-frontend/app/admin/owner-verifications/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-009"></a>
#### UX-009 — Page design, navigation and state QA: /admin/owner-verifications

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/owner-verifications directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states PENDING, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/owner-verifications/page.tsx](../../Eema-frontend/app/admin/owner-verifications/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-010"></a>
#### UX-010 — Page design, navigation and state QA: /admin

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states auth ready and role-gated content. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/page.tsx](../../Eema-frontend/app/admin/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-011"></a>
#### UX-011 — Page design, navigation and state QA: /admin/payout-destinations/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/payout-destinations/[id] directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states PENDING_VERIFICATION, REJECTED, REPLACED, VERIFIED, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/payout-destinations/[id]/page.tsx](../../Eema-frontend/app/admin/payout-destinations/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-012"></a>
#### UX-012 — Page design, navigation and state QA: /admin/payout-destinations

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/payout-destinations directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states PENDING_VERIFICATION, REJECTED, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/payout-destinations/page.tsx](../../Eema-frontend/app/admin/payout-destinations/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-013"></a>
#### UX-013 — Page design, navigation and state QA: /admin/payouts/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/payouts/[id] directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states HELD, READY, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/payouts/[id]/page.tsx](../../Eema-frontend/app/admin/payouts/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-014"></a>
#### UX-014 — Page design, navigation and state QA: /admin/payouts

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/payouts directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states HELD, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/payouts/page.tsx](../../Eema-frontend/app/admin/payouts/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-015"></a>
#### UX-015 — Page design, navigation and state QA: /admin/policies

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/policies directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states DRAFT, PUBLISHED, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/policies/page.tsx](../../Eema-frontend/app/admin/policies/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-016"></a>
#### UX-016 — Page design, navigation and state QA: /admin/venues/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/venues/[id] directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states ACTIVE, PENDING, SUSPENDED, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/venues/[id]/page.tsx](../../Eema-frontend/app/admin/venues/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-017"></a>
#### UX-017 — Page design, navigation and state QA: /admin/venues

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** ADMIN


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /admin/venues directly and from every header/dashboard link as ADMIN; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states PENDING, SUSPENDED, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/admin/venues/page.tsx](../../Eema-frontend/app/admin/venues/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-018"></a>
#### UX-018 — Page design, navigation and state QA: /booked

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** anonymous, SCOUT, OWNER, ADMIN as applicable


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /booked directly and from every header/dashboard link as anonymous, SCOUT, OWNER, ADMIN as applicable; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states AWAITING_PAYMENT, auth, error, loading, missing, not-found, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/booked/page.tsx](../../Eema-frontend/app/booked/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-019"></a>
#### UX-019 — Page design, navigation and state QA: /bookings/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** anonymous, SCOUT, OWNER, ADMIN as applicable


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /bookings/[id] directly and from every header/dashboard link as anonymous, SCOUT, OWNER, ADMIN as applicable; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states AWAITING_PAYMENT, CANCELLED, CONFIRMED, DECLINED, EXPIRED, REFUNDED, REQUESTED, UNAVAILABLE, error, expired, forbidden, idle, loading, not-found, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/bookings/[id]/page.tsx](../../Eema-frontend/app/bookings/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-020"></a>
#### UX-020 — Page design, navigation and state QA: /forgot-password

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** anonymous, SCOUT, OWNER, ADMIN as applicable


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /forgot-password directly and from every header/dashboard link as anonymous, SCOUT, OWNER, ADMIN as applicable; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states auth ready and role-gated content. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/forgot-password/page.tsx](../../Eema-frontend/app/forgot-password/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-021"></a>
#### UX-021 — Page design, navigation and state QA: /owner/bookings

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/bookings directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states REQUESTED, error, fulfilled, loading, rejected. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/bookings/page.tsx](../../Eema-frontend/app/owner/bookings/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-022"></a>
#### UX-022 — Page design, navigation and state QA: /owner/inquiries

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/inquiries directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states OPEN, error, loading. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/inquiries/page.tsx](../../Eema-frontend/app/owner/inquiries/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-023"></a>
#### UX-023 — Page design, navigation and state QA: /owner/payout-destination

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/payout-destination directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states PENDING_VERIFICATION, REJECTED, REPLACED, VERIFIED, error, loading. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/payout-destination/page.tsx](../../Eema-frontend/app/owner/payout-destination/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-024"></a>
#### UX-024 — Page design, navigation and state QA: /owner/payouts/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/payouts/[id] directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states PAID, fulfilled, ready, rejected. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/payouts/[id]/page.tsx](../../Eema-frontend/app/owner/payouts/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-025"></a>
#### UX-025 — Page design, navigation and state QA: /owner/payouts

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/payouts directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states error, fulfilled, loading, rejected. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/payouts/page.tsx](../../Eema-frontend/app/owner/payouts/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-026"></a>
#### UX-026 — Page design, navigation and state QA: /owner/venues/[id]

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/venues/[id] directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states ACTIVE, PENDING, error, forbidden, loading, not-found. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/venues/[id]/page.tsx](../../Eema-frontend/app/owner/venues/[id]/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-027"></a>
#### UX-027 — Page design, navigation and state QA: /owner/venues/new

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/venues/new directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states auth ready and role-gated content. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/venues/new/page.tsx](../../Eema-frontend/app/owner/venues/new/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-028"></a>
#### UX-028 — Page design, navigation and state QA: /owner/venues

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/venues directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states error, loading. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/venues/page.tsx](../../Eema-frontend/app/owner/venues/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-029"></a>
#### UX-029 — Page design, navigation and state QA: /owner/verification

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** OWNER


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /owner/verification directly and from every header/dashboard link as OWNER; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states REJECTED, UNVERIFIED, error, loading. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/owner/verification/page.tsx](../../Eema-frontend/app/owner/verification/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-030"></a>
#### UX-030 — Page design, navigation and state QA: /

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** anonymous, SCOUT, OWNER, ADMIN as applicable


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open / directly and from every header/dashboard link as anonymous, SCOUT, OWNER, ADMIN as applicable; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states auth ready and role-gated content. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/page.tsx](../../Eema-frontend/app/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-031"></a>
#### UX-031 — Page design, navigation and state QA: /policies

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** anonymous, SCOUT, OWNER, ADMIN as applicable


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /policies directly and from every header/dashboard link as anonymous, SCOUT, OWNER, ADMIN as applicable; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states error, loading, ready. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/policies/page.tsx](../../Eema-frontend/app/policies/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-032"></a>
#### UX-032 — Page design, navigation and state QA: /reset-password

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** anonymous, SCOUT, OWNER, ADMIN as applicable


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /reset-password directly and from every header/dashboard link as anonymous, SCOUT, OWNER, ADMIN as applicable; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states invalid, success. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/reset-password/page.tsx](../../Eema-frontend/app/reset-password/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-033"></a>
#### UX-033 — Page design, navigation and state QA: /scout/bookings

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** SCOUT


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /scout/bookings directly and from every header/dashboard link as SCOUT; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states error, loading. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/scout/bookings/page.tsx](../../Eema-frontend/app/scout/bookings/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-034"></a>
#### UX-034 — Page design, navigation and state QA: /scout/inquiries

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** SCOUT


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /scout/inquiries directly and from every header/dashboard link as SCOUT; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states OFFERED, error, loading. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/scout/inquiries/page.tsx](../../Eema-frontend/app/scout/inquiries/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="ux-035"></a>
#### UX-035 — Page design, navigation and state QA: /verify-email

**Area:** UX · **Priority:** P2 · **Test level:** MANUAL UX, LOCAL E2E, SECURITY · **Result:** NOT RUN

**Roles involved:** anonymous, SCOUT, OWNER, ADMIN as applicable


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** 1. Open /verify-email directly and from every header/dashboard link as anonymous, SCOUT, OWNER, ADMIN as applicable; substitute owned fixture UUID for [id]. 2. Repeat anonymous and wrong-role access. 3. At 375px, 768px and 1440px inspect all cards/forms/tables/gallery/dialogs/financial breakdowns. 4. Keyboard-tab entire page, activate primary/secondary actions, close dialogs, check returned focus; use screen reader for labels/errors/loading/table headings. 5. Delay API 3 seconds; return empty collection or missing detail; then exercise current states invalid, success, verifying. 6. Inject 400/401/403/404/409/500, offline, invalid JSON and valid JSON wrong shape into each page loader; restore and retry. 7. Use long UUID, max venue name/reason, Unicode and large money values; reload and navigate back. 8. Review each design item below and record screenshot/finding.


**Expected UI behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. Visible focus, associated labels/errors, semantic headings/tables, meaningful button names, focus containment/return for dialogs, status not color-only, live loading/status announcement and contrast must be checked.


**Expected API/backend behavior:** Primary action obvious; role/context obvious; status understandable; next step clear; no unnecessary information; consistent copy; precise money labels; destructive actions distinct; mobile usable; polished loading/empty/error; consistent Eema typography, paper/ink/ochre palette and marketplace layout. No private data flash before role resolution; errors contain no stack/provider output; independent failed panels do not hide valid sibling data. State/action visibility agrees with backend. Fail any uncovered case; static /admin home only loads auth/policy state, not its own result list.


**Expected database/domain state:** Read-only variants do not mutate; any invoked workflow must satisfy its domain scenario and endpoint matrix.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** MANUAL ONLY


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** [frontend/app/verify-email/page.tsx](../../Eema-frontend/app/verify-email/page.tsx)


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.

