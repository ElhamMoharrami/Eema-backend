<a id="family-cancel"></a>
### CANCEL scenarios

<a id="cancel-001"></a>
#### CANCEL-001 — Withdraw REQUESTED and unpaid AWAITING_PAYMENT

**Area:** CANCEL · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** As Scout A cancel each fixture; for pending provider payment mock open, canceled and paid responses separately; GET after operation.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** REQUESTED/unpaid approval→CANCELLED no refund; blocking slot released; open payment cancellation reconciled before withdrawal. If provider already paid, first apply payment then confirmed cancellation policy.


**Expected database/domain state:** REQUESTED/unpaid approval→CANCELLED no refund; blocking slot released; open payment cancellation reconciled before withdrawal. If provider already paid, first apply payment then confirmed cancellation policy.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** CancellationService, BookingCancellationCoordinator, BookingCancellationPolicy, BookingCancellation.tsx


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-002"></a>
#### CANCEL-002 — Owner and admin cancellation

**Area:** CANCEL · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** On independent CONFIRMED fixtures cancel as owning owner then ADMIN; submit absent/blank/1000/1001 reason and unrelated owner.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Reason required (current coordinator uses 409 for missing owner/admin reason); owner/admin 100% refund, zero commission/owner entitlement; responsibility OWNER versus EEMA; no negative payout.


**Expected database/domain state:** Reason required (current coordinator uses 409 for missing owner/admin reason); owner/admin 100% refund, zero commission/owner entitlement; responsibility OWNER versus EEMA; no negative payout.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** CancellationService, BookingCancellationCoordinator, BookingCancellationPolicy, BookingCancellation.tsx


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-003"></a>
#### CANCEL-003 — Cancellation preview and stale UI

**Area:** CANCEL · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** As scout and owner GET preview then open frontend confirmation; change booking state before confirming and inject 409. Repeat at money-tier boundary without state change.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** canCancel/tier/percentage/refund/retained/deadline from backend. 409 refetches preview and booking; no forced local cancel. Request has no preview version token; crossing a time tier alone can yield different cancellation economics without 409—record UX gap.


**Expected database/domain state:** canCancel/tier/percentage/refund/retained/deadline from backend. 409 refetches preview and booking; no forced local cancel. Request has no preview version token; crossing a time tier alone can yield different cancellation economics without 409—record UX gap.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** CancellationService, BookingCancellationCoordinator, BookingCancellationPolicy, BookingCancellation.tsx


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-004"></a>
#### CANCEL-004 — No refund and duplicate cancellation recovery

**Area:** CANCEL · **Priority:** P0 · **Test level:** API, LOCAL E2E · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Cancel inside nonrefundable window; repeat; cancel refundable booking with remote ambiguity then repeat cancellation.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Zero refund never calls provider. Duplicate completed mutation does not add financial effects; ambiguous persisted refund may resume using same claim. CANCELLED remains separate from RefundStatus.


**Expected database/domain state:** Zero refund never calls provider. Duplicate completed mutation does not add financial effects; ambiguous persisted refund may resume using same claim. CANCELLED remains separate from RefundStatus.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL REAL POSTGRES


**Source / existing evidence:** CancellationService, BookingCancellationCoordinator, BookingCancellationPolicy, BookingCancellation.tsx


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-005"></a>
#### CANCEL-005 — grace before

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−23h59m, shootStart=T0+8d; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 100% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 100% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-006"></a>
#### CANCEL-006 — grace equality

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−24h, shootStart=T0+3d; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 100% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 100% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-007"></a>
#### CANCEL-007 — grace after

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−24h1m, shootStart=T0+3d; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-008"></a>
#### CANCEL-008 — over seven days

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−48h, shootStart=T0+7d1m; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 100% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 100% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-009"></a>
#### CANCEL-009 — exact seven days

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−48h, shootStart=T0+7d; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-010"></a>
#### CANCEL-010 — between seven and two days

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−48h, shootStart=T0+3d; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-011"></a>
#### CANCEL-011 — exact 48 hours outside grace

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−48h, shootStart=T0+48h; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-012"></a>
#### CANCEL-012 — less than 48 hours

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−48h, shootStart=T0+47h59m; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 0% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 0% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-013"></a>
#### CANCEL-013 — grace cannot override 48h equality

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−1h, shootStart=T0+48h; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 50% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


<a id="cancel-014"></a>
#### CANCEL-014 — grace cannot override under 48h

**Area:** CANCEL · **Priority:** P0 · **Test level:** UNIT, API · **Result:** NOT RUN

**Roles involved:** Scout A, Owner A; repeat denial with Scout B, Owner B and anonymous


**Preconditions:** Local stack healthy; relevant actor active, email verified and current policies accepted unless varied.


**Test data:** Standard fixtures below; use a fresh aggregate for each independent variant.


**Exact steps:** Freeze cancellation T0; set confirmedAt=T0−1h, shootStart=T0+47h; snapshot EEMA_V1 in venue timezone. Preview then cancel as scout; repeat equivalent instants across spring/fall DST.


**Expected UI behavior:** Show the authoritative returned state or a safe actionable error; refresh/reload must agree. No optimistic money or status claim.


**Expected API/backend behavior:** Refund 0% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected database/domain state:** Refund 0% of paid snapshot with HALF_UP cents; retained=paid−refund; commission recomputed on retained using snapshot percentage; provider cost accounted separately; booking CANCELLED.


**Expected email/provider behavior:** No email/provider operation unless the lifecycle email matrix explicitly names this transition; capture calls and verify none on rejection.


**Security/authorization expectation:** Use the endpoint role matrix and a different owner/scout UUID; denial must leave domain state unchanged.


**Cleanup/reset steps:** Restore the isolated fixture snapshot; clear browser session and discard captured tokens. Never delete shared staging history.


**Automation recommendation:** SHOULD AUTOMATE


**Local/staging requirement:** LOCAL MOCKED


**Source / existing evidence:** BookingCancellationPolicy#evaluate


**Pass/fail criteria:** PASS only when every listed variant meets all expectations and cleanup/evidence are recorded; FAIL for any mismatch or forbidden side effect; BLOCKED when required harness/provider/fixture is unavailable. Existing source-level characterization does not close a separate launch-acceptance gap.


