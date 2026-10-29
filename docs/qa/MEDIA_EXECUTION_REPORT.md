# MEDIA execution report

Audit/inventory recorded before MEDIA production/test changes, 2026-09-12; final validation 2026-09-13. Existing AUTH/POL/TRUST/VEN/READY work is preserved.

**MEDIA-001–004: 4 PASS / 0 FAIL / 0 locally BLOCKED. Five reproduced defects fixed.** Backend unit/service and PostgreSQL checks pass; the real browser/HTTP/restart journey passes seven grouped checks. Deployed storage and EXIF/privacy handling are explicitly outside software approval. MEDIA is locally clear to proceed to the next deterministic domain; it is not authorization to accept real photos before the EXIF decision or to claim production S3 readiness.

## Final execution results

| Validation | PASS | FAIL | BLOCKED locally |
|---|---:|---:|---:|
| MEDIA scenarios | 4 | 0 | 0 |
| Unit/service suite (`mvn test`; final verify/clean package) | 308 | 0 | 0 |
| Real PostgreSQL full integration suite | 213 | 0 | 0 |
| MEDIA-prefixed PostgreSQL invocations, included above | 13 | 0 | 0 |
| Real browser/HTTP/restart grouped checks | 7 | 0 | 0 |
| Focused frontend error-contract checks | 3 | 0 | 0 |

Counts are separate layers, not an inflated sum of independent scenarios. The PostgreSQL suite includes 128 VenueDomain invocations, including 12 newly added MEDIA invocations and the pre-existing MEDIA upload integration. Two repeated race methods exercise nine transaction pairs per run (empty append, last-slot append, and full PUT/upload), and passed again in the final full suite. Five reproduced software defects are fixed; staging and product decisions are not labeled software failures or silently marked PASS.

`mvn test`, `mvn verify -Ppostgres-integration`, `mvn clean package`, backend `git diff --check`, frontend `npm run typecheck`, `npm run lint`, `npm run build`, and frontend `git diff --check` all pass. The existing PDF renderer regenerated the 684-page master plan; MEDIA pages 51–52 were visually inspected. Sanitized machine-readable counts are in `evidence/media/validation.json` and `verify.json`; `browser.json`, `client.json`, `dependency.json`, `pdf.json`, reproduction JSON and screenshots supply focused evidence. No raw server/test log or credential is committed. The final focused HTTP check also confirms a non-multipart text/plain upload is 400; a general unsupported-media-type handler must not be mistaken for this endpoint behavior. Task servers, containers, volumes, browser profile and temporary binary fixtures were removed; shared data is untouched.

## Pre-change inventory

| ID | Generated title / priority | Source-authoritative contract | Existing coverage / missing proof |
|---|---|---|---|
| MEDIA-001 | Valid image upload and durable reference / P1 | Two distinct endpoints; local/S3 provider selected by configuration; generated UUID references | S3 mock happy path uses fake bytes; VEN real PNG attach/read; actual JPEG/JPEG/PNG/WebP and restart durability missing |
| MEDIA-002 | Image boundaries and fake media / P1 | MIME/extension allowlists, no decoding, no MIME-extension equality; size 5 MiB default | Fake bytes acceptance is a security defect to reproduce/fix; no comprehensive binary/multipart/public-path matrix |
| MEDIA-003 | Photo list ordering, primary and maximum / P1 | Ordered array, first primary, max12; detach keeps physical bytes; owner ACTIVE changes re-review, ADMIN override retained | VEN/READY limits/reorder/readiness and critical photos covered; add targeted concurrent uploads/reorder and no-orphan checks |
| MEDIA-004 | Upload persistence failure and orphan cleanup / P1 | Storage external to transaction; saveAndFlush failure attempts delete, original exception retained | Mock save failure existing; add cleanup failure/log contract, actual PostgreSQL rollback and S3 compensation |

## Architecture before changes

- `POST /api/upload/venue-photo`, multipart `file`: verified-email OWNER only (ADMIN/SCOUT denied). 200 `{message,url}`; no venue attachment, moderation or lifecycle notification. Frontend editor uses scoped upload instead; standalone endpoint remains supported and can create orphans.
- `POST /api/venues/{id}/photos`, multipart `file`: own verified-email OWNER or ADMIN (email exemption), not another OWNER/SCOUT/anonymous. No policy acceptance or owner-review prerequisite. Venue lock, authorization and max12 guard precede storage. Append validated stored reference, apply owner critical-edit moderation, saveAndFlush, return 200 VenueDto.
- `GET /api/upload/files/{filename}` is public; local storage reads configured filesystem root, S3 adapter proxies bucket bytes behind the same stable API URL. S3 bucket need not itself be public. Production rejects identical public/private document buckets. Local compose mounts separate named upload/document volumes.
- Local/public size defaults 5,242,880 bytes. Base servlet max file 5MB/request6MB; local profile file5MB/request10MB. Servlet and storage validation are distinct layers.
- Actual allowlists: JPEG MIME with .jpg/.jpeg, PNG, WebP; pre-audit code independently accepts any allowed MIME and extension combination, and never decodes bytes. MIME/filename are untrusted declarations, not content proof.
- UUID names discard client path components. Local writes currently use REPLACE_EXISTING; local read accepts arbitrary root-relative filenames and missing files become generic exceptions. Audit strict public-name validation/missing404 separately from ordinary authorized reads.
- Photo array permits only configured Eema-prefix/UUID-shaped references, not arbitrary URLs. Shape does not prove existence or media ownership. No exclusive-ownership/reference table, garbage collector, EXIF stripping, resizing or antivirus scan exists.
- Ordinary draft photo writes emit no lifecycle event. ACTIVE owner add/remove/reorder invokes re-review and its VENUE_SUBMITTED notification. ADMIN media changes retain the existing override operations question. Removal UI says removed from listing, not permanently deleted.
- Database rollback cannot undo filesystem/S3 writes. Existing catch handles immediate persistence failure with delete compensation; cleanup error logs venue ID and exception class without replacing original failure. Audit transaction-completion failure handling as well.

## Isolation and corrections

Replace generic Scout/ownership variants, blanket policy/owner-review prerequisites, duplicated API/DB wording, generic no-email claims and fictional snapshot restore. Use unique RUN_ID fixtures, Testcontainers PostgreSQL, temporary test media directories, a dedicated named local media volume for restart proof, isolated browser profiles and mocked S3. Dispose whole QA environments; never delete shared media or history. Local software PASS does not prove deployed S3 durability/access or metadata privacy.

## Executed contract and validation matrix

The source audit precedes the additions above. MEDIA scenarios are grouped by invariant rather than repeated binary/browser cases. Source paths are relative to the backend unless prefixed `../Eema-frontend`.

| Request / condition | Authoritative result | Durable write / venue / moderation |
|---|---|---|
| JPEG `.jpg` and `.jpeg`, PNG `.png`, WebP `.webp` with matching MIME and decodable content | 200; generated UUID reference | Standalone: object only. Scoped: append and return VenueDto |
| Lossless and alpha WebP | Accepted by shared decoder | Original bytes retained |
| Empty / 1-byte / fake text renamed JPEG / truncated JPEG | 400 | No new object/reference/event |
| GIF / SVG / BMP / text / octet-stream / missing MIME | 400 | No write; SVG remains unsupported |
| Allowed MIME + wrong extension, or actual JPEG declared PNG | 400 | No write |
| Exactly 5,242,880-byte decodable JPEG | 200 | Accepted by both adapters and real Tomcat |
| 5,242,881 bytes | Service/MockMvc 400; real multipart servlet 413 | No durable write / mutation |
| Declared short MultipartFile size but actual stream oversized | 400 via bounded read | No write |
| Missing multipart part / malformed boundary | 400 | No write |
| Non-multipart text/plain upload request | 400 (MultipartException) | No write |
| Width 10,001 or 30-million-pixel compressed image | 400 before full decode | No write |
| `../../x.jpg`, Windows paths, absolute path, encoded traversal, Unicode separators as upload filename | Valid image accepted with unrelated UUID key | Cannot choose storage path or overwrite existing key |
| Public nonexistent UUID, malformed reference, private-document path, root-escaping path, valid-shaped symlink | 404 (security firewall may return 400 for crafted HTTP paths) | No arbitrary/private filesystem read |
| Duplicate / arbitrary external / file / traversal / non-UUID photo reference | 400 | No partial replacement; shape-only accepted references are not ownership proof |
| Thirteenth scoped upload | 400 before storage | Existing 12 unchanged; no orphan |
| Reorder / remove | Full ordered-array replacement | First is primary; removal detaches only, physical object retained |

`MediaStorageContractTest` centralizes binary coverage. Tiny real images are in `src/test/resources/media/`; the exact 5 MiB sample is generated during testing by padding a decodable JPEG, rather than committing a large binary. This deliberately proves the byte limit independently of decoder rejection. Image decoding is **not** a proof that trailing bytes/metadata contain no other data.

### Exact authorization and prerequisites

| Actor | Standalone POST | Own scoped POST | Someone else's scoped POST |
|---|---:|---:|---:|
| OWNER, verified email | 200 | 200 | 403 |
| OWNER, unverified email | 403 | 403 | 403 before storage |
| SCOUT | 403 | 403 | 403 |
| ADMIN | 403 | 200 for any venue | 200 for any venue |
| Anonymous | 401 | 401 | 401 |

Scoped ADMIN is exempt from the verified-email guard. Public GET needs no authentication. Policies and owner-review status do not gate upload, reordering or removal; the browser successfully uploads before accepting policies or obtaining owner review. Those prerequisites are established later only to create a genuinely ACTIVE fixture for the moderation check. Denials, validation failures and the max-limit rejection occur before storage and leave authoritative venue/event state unchanged in PostgreSQL.

### Content validation and resource safety

`ImageUploadValidator` is shared by local and S3 adapters: declared type + extension equality, bounded input read, reader-recognized format equality, pre-decode geometry budget, and actual image decode. JPEG/PNG use JDK ImageIO; WebP uses `com.twelvemonkeys.imageio:imageio-webp:3.15.0`. It disposes readers/images/streams, rejects decoder warnings, and does not re-encode originals.

New documented abuse limits are 10,000 pixels per dimension, 25 million pixels total and two concurrent decoders per JVM. A 25MP ARGB raster is approximately 100MB; compressed buffers, decoder overhead, queued HTTP requests and multiple application instances still require deployment capacity planning. This is bounded decoding, not a guarantee against every decoder vulnerability or CPU attack. Rate limiting and infrastructure capacity are not introduced by this change.

Dependency review: TwelveMonkeys is a maintained pure-Java ImageIO project; the selected release includes WebP fixes and memory-allocation safeguards. Its license is BSD 3-Clause. Only the WebP module and its core/metadata/common dependencies are added; no native executable or SVG decoder is introduced. See the [official release](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.15.0), [license](https://raw.githubusercontent.com/haraldk/TwelveMonkeys/twelvemonkeys-3.15.0/LICENSE.txt), and [module POM](https://raw.githubusercontent.com/haraldk/TwelveMonkeys/twelvemonkeys-3.15.0/imageio/imageio-webp/pom.xml). This is a dependency impact/maintenance review, not a claim of comprehensive vulnerability certification.

The decoder validates the first image/frame; it is not an exhaustive metadata/container lint or animation-frame scan. Stock JPEG/PNG plus lossy/lossless/alpha WebP fixtures pass. No WebP support was removed. Existing stored objects are not retroactively decoded; deployments with pre-fix objects need an inventory/review before relying on the new-upload guarantee.

### Public storage and durability

Both adapters return `/api/upload/files/<uuid>.<ext>` under the configured application prefix. Local filenames are generated without overwrite; temporary files are removed on local write failure. Client filenames cannot choose a key. S3 writes use the configured public-media bucket/prefix, authoritative content type and `If-None-Match: *`; the mock verifies bucket/key/body and conditional-create metadata. Deletion targets the same generated bucket/key.

The backend proxies public bytes; direct public bucket access is not required by this design. Production configuration continues to reject identical public-media/private-document buckets. Strict public filename validation and local symlink refusal keep financial-document paths outside the public route. Mocked S3 missing-key handling returns 404; real permissions, endpoint behavior and conditional-write support remain staging checks.

Real HTTP responses return JPEG/PNG/WebP Content-Type, inline disposition and `X-Content-Type-Options: nosniff`. Current Spring cache policy is `no-cache, no-store, max-age=0, must-revalidate`; this pass does not add CDN/long caching.

The production Dockerfile originally left fresh named volumes root-owned. A non-root backend could start but failed to write a valid uploaded image. It now creates `/app/uploads` and `/app/private-documents` before assigning the application directory to `eema`; a fresh named volume inherits that ownership. This fixes fresh-volume initialization. It does not silently chown existing deployment volumes; operators must check ownership when deploying to an already-created volume.

### Ordering, readiness and moderation

There is no independent primary-photo ID. Ordered `photoPaths[0]` drives `VenueMapper` list `firstPhotoUrl`, owner preview/primary marker, catalogue card and `VenueDetail` through `venuePhoto`/`GalleryFrame`. `/venues/my` returns full VenueDto with `photoPaths`, not VenueList's `firstPhotoUrl`; the initial test assertion was corrected accordingly.

Reorder and detach use complete PUT photo arrays. Neither copies storage objects. Removing a reference does not promise permanent object deletion; UI says “removed from the listing.” First attachment clears READY-012's `photoPaths` issue; removing all restores it. Readiness checks references rather than storage reachability.

Owner ACTIVE add/remove/reorder remains a critical edit: ACTIVE → PENDING, public/matching removal, VENUE_SUBMITTED trust event and existing re-review notification. Existing `VenueDomainIntegrationTest#photoListChangesRequireReview` covers all three with unchanged historical booking and policy snapshots. The MEDIA browser adds real ACTIVE scoped-upload/refetch proof. Ordinary DRAFT upload/reorder/remove has no lifecycle event/email. ADMIN's existing critical-edit override is unchanged and remains an operations decision from VEN.

### PostgreSQL races and compensation

`VenueDomainIntegrationTest#mediaConcurrentUploadsAppendBothAndRespectLastSlot` uses a real repository lock interception barrier with two confirmed arrivals, repeated three times. Empty venue: two successful requests leave both references exactly once. At 11 references: one success and one safe 400 leave exactly 12, with one new object. No sleeps select transaction order; database locking is exercised.

`mediaConcurrentWholePutAndUploadHaveSerializedReplacementSemantics` repeats three times. Both operations complete without a partial/corrupt array. The final state is one of the two legal serialized results. A later whole-array PUT can intentionally or unknowingly detach the new photo; locks do not resolve stale client snapshots. No merge behavior was invented because it would change removal semantics. Revision-aware editing is a backlog/API decision, not a claim that the race is impossible.

`mediaAttachmentFailureAfterRealFlushRollsBackRowsAndDeletesNewObject` arms a test-only repository interceptor after an actual PostgreSQL flush. The request returns 500; database row/status/photo paths/trust events remain unchanged, the newly stored object is deleted, and no AFTER_COMMIT notification is delivered. A raw in-transaction application event may have been published, but a rolled-back event cannot become a lifecycle email.

`VenueServiceTest` proves immediate save failure compensation, delete-failure preservation of the exact original persistence exception, and the real S3 adapter's delete bucket/key. Cleanup error logging contains venue ID and exception class, not cleanup exception text/bytes/credentials. Client 5xx messages remain generic. The application's general unexpected-exception logger still records server stack traces; deployed logging/access controls and provider error redaction must be checked in staging.

Compensation is not a distributed transaction. The existing catch surrounds save/saveAndFlush inside the transactional method; a later transaction-completion failure, process death, timeout with ambiguous object creation, or failed delete can leave an orphan. Standalone uploads never attached, detached photos and abandoned drafts can also leave objects. No ownership/reference table or GC job exists, and none was introduced.

## Defects and corrections

| ID | Severity | Expected / reproduced actual | Impact / narrow fix |
|---|---|---|---|
| MEDIA-D01 | P1 security/integrity | Fake JPEG text must fail; both adapters accepted it and standalone returned 200 | Arbitrary unvalidated bytes under trusted image references. Shared bounded actual decode + MIME/extension/format equality; WebP retained. Before/after tests recorded |
| MEDIA-D02 | P2 | Missing public media 404; local missing UUID returned 500 | Broken expected read contract. Local strict controlled-name/non-symlink reads and 404; S3 NoSuchKey 404 |
| MEDIA-D03 | P2 | Missing/malformed multipart safe 400; missing file returned 500 | Client error treated as server fault. Specific multipart/size mappings, including actual servlet 413 |
| MEDIA-D04 | P1 local deployment | Fresh named media volume writable by non-root backend; actual valid upload failed with AccessDeniedException/500 | Supported container local uploads unusable. Initialize/chown directories in Dockerfile, validate fresh volume and restart |
| MEDIA-D05 | P2 UX | Oversize upload explains corrective action; real 413 showed generic “Something went wrong” | Owner cannot identify size issue. Focused frontend 413 mapping; client and browser regression |

Initial harness failures were also corrected: create DTO requires `sizeM2` and `tags`, lazy previews require rendering/scrolling before inspection, gallery detail uses a caption-based image label, and the home page features the first ACTIVE venue. These were test assumptions, not product defects. Dedicated QA prior ACTIVE fixtures are suspended through the real admin endpoint when needed, preserving their history. No shared data is touched.

## Privacy and operational boundaries

Original bytes and metadata are retained, including possible EXIF GPS/device/time information. `qa-metadata.jpg` contains only synthetic software/time metadata and proves byte preservation; no personal GPS data or evidence file is stored. Content validation does not remove this privacy risk. Decide how to handle metadata **before accepting real owner photos into private alpha**; this is not a software/legal PASS. No re-encoding, resizing, antivirus scanning, cryptographic signing, new retention policy or garbage collection is claimed.

## WHAT ELI SHOULD TEST PERSONALLY

- Is upload progress and the primary-photo/reordering model understandable?
- Do removal and file type/size/corruption messages explain what happened?
- Does the gallery look usable on desktop/mobile, and is re-review after a live photo change clear?

## STAGING MEDIA TESTS STILL REQUIRED

- Real S3-compatible bucket/credentials, API proxy reads, permissions and conditional-create compatibility; enforce separate private-document/public-media buckets.
- Durability across deployment/restart, existing volume ownership if using local storage, real object accessibility, cache/CDN behavior and large-file latency.
- Provider outage/timeout behavior, orphan observability, and logs free of credentials/signed URLs/sensitive provider details.
- Actual re-review lifecycle email delivery for ACTIVE photo changes; ordinary photo edits have no provider email contract.

## PRODUCT / OPERATIONS DECISIONS

- EXIF/GPS stripping or an explicit pre-alpha privacy handling decision; resizing/compression and animation handling if expanded later.
- Orphan retention/GC, media ownership/reference table, legacy object review and storage/database backup consistency.
- Revision-aware full-array editing to avoid stale PUT detachment; existing ADMIN critical-edit/re-review policy.
- CDN/caching and deployment capacity for the documented decode limits. No new marketplace rules were silently implemented.
