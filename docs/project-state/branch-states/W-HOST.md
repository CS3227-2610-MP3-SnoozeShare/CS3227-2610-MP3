# Branch State — W-HOST

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `INT`;
read it for context and write here instead of editing it. `sync-project-state` folds this file
in after the branch merges.

- **Branch:** W-HOST (from `development` at `8702e88`)
- **Base:** `e185dc0`
- **Owner:** project owner
- **Status:** In review
- **Last updated:** 2026-10-22 by GPT-5 — Trevor approved the W-HOST experience-corrections spec; plan drafting started

---

**Scope.** Implement the Host feature set, including the authenticated backend API and Next.js
pages, on top of the completed W-DOMAIN model, persistence, and service contracts.

---

## 1. Orientation

No change.

## 2. How to Resume

**Setup**, only if it differs from `PROJECT_STATE.md` § How to Resume:

No change.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| GPT-5 | 2026-10-22 | — | W-HOST follow-up | Active | T4 complete; 187 frontend tests, lint, and production build pass | T2 step 1: add listing metric and in-place status-toggle regression tests | 2026-10-22 |

**Remaining**

- [ ] Owner review and approval remain for W-HOST and W-HOST-API.
- [x] T4 booking decisions: owner-scoped database paging, authenticated API, row-lock decision and concurrency tests are complete.
- [x] T5 Host booking/ticket message APIs: distinct authenticated routes, database-bounded summaries/history, after-commit events and rollback coverage complete.
- [x] T6 Host wallet API: authenticated balance, paged statement and mocked mutation routes; five PostgreSQL integration tests pass.
- [x] T7 authenticated frontend Host API adapter and provider; 154 tests, lint and production build pass.
- [x] T8 wire listing, calendar, and request screens to the API provider; 166 frontend tests, lint and production build pass.
- [x] T9 wire Host messages and wallet screens to the API provider; 172 frontend tests, lint, and production build pass.
- [x] T10 backend/frontend acceptance, authenticated request captures, regression fixes, code review, and fresh build/test/lint gates complete; owner review remains (D~23/D~25/D~26/D~27).
- [x] T12 return complete booking/ticket conversation arrays; 101-row collections, 1,001-message history, legacy parameter equivalence, privacy, Checkstyle, and backend build pass.
- [x] T13 return the complete newest-first wallet statement; 101-row stable-order/owner-scope test, legacy parameter equivalence, Checkstyle, and backend build pass.
- [x] T14 replace paged frontend API/provider types with complete arrays; 177 tests, lint, and production build pass; wallet response is shared between navbar and Wallet screen.
- [x] T15 remove listing/request pagination controls and state; tests render 101 listings, pending rows, and history rows, all 177 frontend tests, lint, and production build pass.
- [x] T16 remove message-history and wallet load-more controls/state; 1,001-message and 101-row tests, 178 frontend tests, lint, and production build pass.
- [x] T17 final verification: fresh backend full rerun, 178 frontend tests/lint/build, integration coverage, and authenticated read-only request capture complete; owner review remains.
- [x] Merged `development` through `e185dc0`, adopted `@ConditionalOnDatabase`, and verified backend build plus frontend tests/lint/production build; W-HOST remains awaiting owner review.

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-HOST | Host features — listings, calendar, requests, wallet, messages, Next.js pages | Building | [experience corrections spec](docs/superpowers/specs/2026-10-22-w-host-experience-corrections-design.md); [UI spec](docs/superpowers/specs/2026-10-21-w-host-ui-design.md) | [UI plan](docs/superpowers/plans/2026-10-21-w-host-ui.md); [experience corrections plan](docs/superpowers/plans/2026-10-22-w-host-experience-corrections.md) | T1/T3/T4 done; 187 frontend tests/lint/build green; T2 status update remains | guide out of date: Host metrics, time display, and calendar behavior |
| W-HOST-API | Replace Host fixtures with authenticated backend integration | In review | [spec](docs/superpowers/specs/2026-10-22-w-host-backend-integration-design.md) | [plan](docs/superpowers/plans/2026-10-22-w-host-backend-integration.md) | T17 complete: 101/1,001-row tests, full gates, and authenticated read-only captures; owner review pending | — |

## 4. Architecture

**Persistence — Now:** W-DOMAIN's JPA model and repositories are merged; database-dependent service implementations and JDBC adapters use the shared `@ConditionalOnDatabase` condition, which matches unless datasource auto-configuration is excluded.

| Source | Standing choice |
|---|---|
| C~17 | *(superseded by C~20)* Persistence-backed services and adapters use the explicit property gate. |
| C~20 | Database-dependent services and adapters use `@ConditionalOnDatabase`, matching the shared backend convention on `development`. |

**Host API — Now:** Protected Host controllers register only when `snoozeshare.security.enabled=true`, matching `HostIdentityResolver`; listing, calendar, booking decision, conversation, and wallet routes resolve host identity server-side and call services rather than repositories. The authenticated frontend adapter routes requests through `authenticatedFetch`; all Host listing, calendar, request, message, and wallet screens use API-backed state with fixture mode retained for presentation tests. Collection and history GETs return every matching owner-scoped row in deterministic order, with no page/size contract; the listings collection carries booking count and average review rating from two grouped queries filtered by the resolved Host ID; calendar remains month-bounded and its grid contains at most 42 cells. This accepts unbounded collection payloads and server/client memory use (C~19). Wallet mutations retain the existing ledger transaction and wallet row lock, with events after commit; ambiguous failures refresh balance and statement without retrying.

| Source | Standing choice |
|---|---|
| C~15 | Use stable ownership errors, bounded projections, and approved endpoint contracts. |
| C~19 | Collection/history GETs return complete owner-scoped result lists; no pagination or collection-size cap. |

**Availability — Now:** Host month reads query only overlapping blocks and bookings; mutations use fresh PostgreSQL `SERIALIZABLE` transactions, audit in the transaction, retry SQLSTATE `40001` once, then publish the availability event after commit. API ranges are inclusive; persisted ranges remain half-open.

| Source | Standing choice |
|---|---|
| C~15 | Calendar reads return a month projection; block writes/removals are atomic and auditable. |

## 5. Conventions

Host route content uses a full-width page frame; input value text is normal-weight, while field labels remain emphasized. Listings, requests, messages, and wallet keep the navbar fixed to the viewport and scroll long content internally; requests cap pending rows at four and let past rows use remaining height, while the wallet statement uses remaining height. Scrollable Host tables keep their header visible, including on horizontal overflow. Per C~20, database-dependent service implementations and JDBC adapters use `@ConditionalOnDatabase`, matching the shared backend convention; datasource-free contexts must exclude datasource auto-configuration. The Host listing API controller is separately gated by `snoozeshare.security.enabled=true` so auth-disabled dev contexts do not register a route without its identity resolver.

## 6. Known Gaps

W-DOMAIN is complete and merged; its accepted gaps remain as recorded in `PROJECT_STATE.md` § Known Gaps. D~6's missing service-bean prerequisite is resolved by C~17; C~20 adopts `development`'s shared activation mechanism.

- C~19 accepts unbounded response size and memory use for complete Host collections and histories, without adding response-size, per-request rate, or concurrency limits; a future pagination/response-bound decision requires owner approval.

## 7. Needs a Human

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-21 | W-HOST UI must match the supplied Claude artifact exactly, including its Fall Light tokens, shell layout, page composition and interaction presentation | User explicitly set the artifact as the visual source of truth; generic existing Tailwind styling is insufficient | [Claude artifact](https://claude.ai/artifact/PWBCxbfv9e9FGVvY6RKUwd) |
| C~2 | 2026-10-21 | Host content pages use the full available width; edit values use normal weight and selected status uses title case; host messages do not show a New Ticket action | Project owner, after authenticated comparison, directed these UI corrections and explicitly rejected adding New Ticket to Host messaging | Operator conversation |
| C~3 | 2026-10-21 | Listings page places its create action beside the title; cards omit the status pill and use five centered, width-stable right-side groups, with outlined calendar/edit actions | Project owner requested the final Listings refinements after reviewing the pushed W-HOST implementation | Operator conversation |
| C~4 | 2026-10-21 | Within the Availability group, the switch sits to the right of its ACTIVE/INACTIVE label; reduce horizontal gaps among all five card groups while retaining centered content and stable widths | Project owner requested a follow-up after reviewing the first T9 layout | Operator conversation |
| C~5 | 2026-10-21 | New/Edit listing forms retain full-width page use, with their four form cards arranged to fill the available width rather than constraining the form to the artifact's narrower artboard | Project owner clarified that the forms should add up to use the full width after the visual comparison surfaced the apparent width difference | Operator conversation |
| C~6 | 2026-10-21 | Booking calendar shows as many complete week rows as its month requires; blocking uses one artifact-style date-range picker and a required reason; current blocked dates list bold reasons above ranges with aligned remove actions, using Maintenance as the fixture reason | Project owner specified calendar coverage and the date-blocking presentation; separate start/end fields, optional reasons, and unlabeled override ranges are rejected | Operator conversation |
| C~7 | 2026-10-21 | Booking-calendar month navigation uses fixed arrow columns with the month/year centered in a fixed-width track, so varying title length does not move the arrows | Project owner requested stable arrow placement and a centered month/year label when navigating across month names of different lengths | Operator conversation |
| C~8 | 2026-10-21 | Reopened date picker must calculate its Sunday-first grid from the first of the selected month, not the selected date itself | Project owner reported selected dates shifted to incorrect weekdays after closing and reopening the picker | Operator conversation |
| C~9 | 2026-10-21 | The booking calendar always renders six week rows; the date-range picker stages edits until Apply, Clear resets the draft without closing, outside click closes without applying, and picker/current-blocked ranges include the end year (both years across year boundaries) | Project owner specified the calendar layout, artifact-style picker controls, dismissal behavior, and range-year presentation | Operator conversation |
| C~10 | 2026-10-21 | The current page name in every Host breadcrumb is bolded | Project owner requested consistent visual emphasis for the current page | Operator conversation |
| C~11 | 2026-10-21 | Host wallet presentation follows the artifact’s inset balance panel, stacked controls, currency label, and bordered transaction table; fixture rows represent relevant booking and wallet events | Project owner approved matching the Wallet page to the artifact and requested sample data based on booking or other relevant examples | Operator conversation |
| C~12 | 2026-10-22 | Vertically center the stacked Top up and Withdraw actions against the available-balance panel without changing their width or stack order | Project owner requested and approved the Wallet action alignment adjustment | Operator conversation |
| C~13 | 2026-10-22 | Keep Host listings, booking requests, and wallet within the viewport below the navbar; scroll listing results, cap pending requests at four rows, and let past requests and wallet statement use the remaining height | Project owner requested the three page-specific scrolling behaviors to avoid long content pushing pages beyond the screen | Operator conversation |
| C~14 | 2026-10-22 | Populate enough sample data to exercise each scroll region (12 listings, 8 pending requests, 10 past requests, 31 wallet transactions) and keep table headers visible while scrolling | Project owner requested overflow-ready sample data and sticky headers after reviewing the initial scroll behavior | Operator conversation |
| C~15 | 2026-10-22 | W-HOST-API uses the operator-approved composite responses, inclusive month calendar, distinct message routes, stable errors, Firebase/domain identity check, booking row locking, transactional availability/message behavior, and bounded database pagination; it does not add wallet request idempotency after C~16 | Project owner approved the recommended defaults to proceed; second-round review required exact bounded query semantics. In-memory slicing was rejected. Wallet idempotency was later explicitly reversed | Operator conversation and second-round spec review |
| C~16 | 2026-10-22 | Remove wallet request idempotency from W-HOST-API: no key header, durable key table, hash, replay response, or request deduplication; each accepted mocked wallet mutation is a separate operation, with no automatic client retry and wallet refresh after ambiguous failure | Project owner said it was unnecessary, then explicitly directed removal after the existing decision C~15 / spec SD7 and SD10 was surfaced. Existing wallet transaction, row lock, ledger, and audit behavior remain | Operator conversation |
| C~17 | 2026-10-22 | Amend W-HOST-API to include deterministic activation of existing W-DOMAIN services and JDBC adapters through `snoozeshare.persistence.enabled` (`true` by default; `false` in DB-free contexts), without changing service/domain behavior | Project owner approved amending and continuing after T2 exposed that `@ConditionalOnBean(PropertyRepository.class)` runs before JPA repository registration. Deferring the prerequisite to separate work was rejected because all Host API endpoints depend on those services | Operator conversation |
| C~18 | 2026-10-22 | All Host listing route/load failures use one generic error page with a link to `/host/listings`; form validation and mutation failures remain inline | Project owner clarified scope as route/load failures only and approved the shared error-page design; the listings index additionally offers retry on its own load failure | Operator conversation |
| C~19 | 2026-10-22 | Reverse bounded Host collection/history pagination: listing, request, conversation-summary, message-history, and wallet-statement GETs return every matching row in one response; remove `page`, `size`, `items`, `hasMore`, and pagination controls. Preserve owner scoping, authorization, deterministic ordering, DTOs, and the calendar's one-month bound | Project owner explicitly chose to remove pagination and return whole collections after the bounded database-pagination decision was surfaced; rejected retaining pagination or silently loading full rows and slicing them in memory. This knowingly accepts unbounded response size and memory use, with no added response-size, per-request rate, or concurrency limits | Operator conversation |
| C~20 | 2026-10-22 | Replace W-HOST-API's explicit `snoozeshare.persistence.enabled` bean gate (C~17) with `@ConditionalOnDatabase`, which matches unless datasource auto-configuration is excluded | Project owner approved adopting `development`'s convention after learning the former gate would be replaced by the shared backend condition; this aligns W-HOST-API with the integrated service/repository activation model | Operator conversation |
| C~21 | 2026-10-22 | Correct Host listing metrics, avoid full-content reloads for listing status and calendar month changes, preserve listing times exactly across detail/edit views, show occupancy to two decimals, and leave the calendar block date range empty initially | Project owner approved this follow-up after review found collection responses omit metrics, status/month loads replace the page with a skeleton, and local-time parsing followed by UTC formatting shifts times (e.g. 07:00 to 23:00 prior day); rejecting timezone conversion and API N+1 detail reads | Operator conversation |
| C~22 | 2026-10-22 | Approve the W-HOST experience-corrections spec and proceed to the plan gate | Trevor approved the reviewed Medium-risk spec after two rounds of Implementer, Tester, and Attacker review; code remains gated on a separate plan approval | Operator, 2026-10-22 |

## 9. Deviations

| ID | Date | Deviation | Consequence / action |
|---|---|---|---|
| D~1 | 2026-10-21 | Initial T8 comparison was blocked by missing Host authentication; operator subsequently logged in and enabled a live route comparison | Resolved. Authenticated artifact comparison completed; all eight Host content routes were checked at 1680×945, 1280×800, and 768×1024 with no horizontal overflow |
| D~2 | 2026-10-21 | Next.js production build intermittently failed during page-data collection with `Invariant: no direct app page entry found for /_not-found` after the breadcrumb implementation | Build passed from an isolated frontend copy while the local dev server remained active; shared `.next` state may explain the earlier failure, but cause is not proven. Recheck from a clean isolated build if it recurs |
| D~3 | 2026-10-22 | Initial frontend dependency restore failed in the restricted environment: `ENOTFOUND` for `registry.npmjs.org`; the default npm cache also rejected log writes | Resolved by restoring lockfile-pinned dependencies with approved network access and a `/private/tmp` npm cache. Baselines pass: backend build; frontend 143 tests, lint, and production build using local placeholder public config |
| D~4 | 2026-10-22 | The plan's `./gradlew checkstyle` shorthand is ambiguous because Gradle exposes `checkstyleMain` and `checkstyleTest`; the first invocation stopped before checks | Resolved: plan invokes both tasks explicitly; T1 Checkstyle and the full backend build pass |
| D~5 | 2026-10-22 | T2's file list omitted `CreateListingRequest` and `UpdateListingRequest`, which need the approved title/description length constraints | Plan-only correction added both DTOs to T2; the 120-character title and 5,000-character description limits are unchanged |
| D~6 | 2026-10-22 | `HostListingsControllerTest` context fails because `ListingService` is not registered; Spring's condition report says `PropertyRepository` is unavailable to service `@ConditionalOnBean` checks, although JPA later discovers 11 repository interfaces | Resolved by operator approval C~17; T0 now activates 15 service contracts and three JDBC adapters via explicit property |
| D~7 | 2026-10-22 | The partial `ListingServiceImpl.findByHostId` used `PageRequest.of(page, size + 1)`, whose offset is `page * (size + 1)` and skips rows on later pages | Resolved in T2 with `OffsetLimitPageable` and repository plus controller page-continuity regression tests |
| D~8 | 2026-10-22 | The partial `HostListingController` was unconditional while `HostIdentityResolver` is gated by `snoozeshare.security.enabled`, making the default auth-disabled profile fail startup | T2 now gates the controller on the same security property; DB-backed controller integration enables security, while dev and DB-free contexts omit the protected Host route |
| D~9 | 2026-10-22 | T3's approved file list omitted the create-block request DTO and audit-action enum; E18 described two different Hosts writing the same listing, which ownership rules make impossible | Plan now includes the missing files; the concurrency regression will issue two simultaneous requests as the listing's single authorized Host, preserving both E3 ownership and the overlap invariant |
| D~10 | 2026-10-22 | The first complete T4 backend check hit a Gradle test-worker `EOFException` despite the focused new tests passing | Re-running the same full backend Checkstyle/build completed successfully; no code or test changes were needed to clear it |
| D~11 | 2026-10-22 | T5's original file list omitted `BookingRepository` and `TicketRepository`, although existing summary services load and sort unbounded parent lists | Added both repositories to T5 so the required summary pages are bounded in the database, not only the message histories |
| D~12 | 2026-10-22 | Host conversation summaries reached existing JDBC unread-count queries that compared `sent_at` to `last_read_message_id` (UUID), causing PostgreSQL type errors | Corrected both booking and ticket read adapters to compare against the joined last-read message's `sent_at`; added the two adapters to T5 |
| D~13 | 2026-10-22 | T5's checklist said every edge case applied identically to both channels, although E8 is ticket-specific | Corrected the checklist coverage wording; E20 rollback behavior is verified for both booking and ticket messages |
| D~14 | 2026-10-22 | T6's concurrent-withdrawal regression showed both withdrawals succeeding because `WalletServiceImpl.move` loaded the wallet before `LedgerWriter` acquired its pessimistic row lock | Resolved with ID-only wallet lookup before ledger posting; the lock now hydrates the current row, and concurrency test confirms one success/one insufficient-balance response |
| D~15 | 2026-10-22 | T6's first bounded statement read exposed `JdbcLedgerRepository` using `ResultSet.getObject(..., Instant.class)`, unsupported by the PostgreSQL JDBC driver for `timestamptz` | Map via `getTimestamp(...).toInstant()` and exercise real PostgreSQL statement reads in the Host wallet integration test |
| D~16 | 2026-10-22 | T6's event-contract review found `LedgerWriter` publishes `WalletTransactionRecordedEvent` before transaction commit, contrary to the approved API spec | Register wallet event dispatch with transaction synchronization and test that rolled-back operations publish no event |
| D~17 | 2026-10-22 | A direct frontend `npx tsc --noEmit` check includes existing test files and reports unrelated typing errors in `HostListings.test.tsx` and `api.test.ts` | The required Next.js production build type-checks successfully; errors remain confined to untouched test lines and were not changed or suppressed |
| D~18 | 2026-10-22 | The fixture calendar's fixed September 15–17 default range would be in the past once the API calendar followed the current month | API mode now defaults to a valid upcoming range in the selected month; fixture presentation defaults remain unchanged |
| D~19 | 2026-10-22 | T8 touched more than 15 files, exceeding the repository's preferred commit review size | Split the task into listing/forms and calendar/requests commits; both pass the same full frontend verification |
| D~20 | 2026-10-22 | Initial T10 browser attempts showed Host routes loading, and normal shell HTTP probes could not reach the host listener | Resolved for browser verification: an approved local health probe confirmed the backend is healthy; authenticated Host routes loaded with API-backed empty states. A transient browser navigation block cleared after the Next build completed; all eight routes were subsequently checked at desktop, tablet, and mobile widths |
| D~21 | 2026-10-22 | Live calendar route with a valid but nonexistent listing ID returned 404 for its listing/calendar requests, but the page rendered only breadcrumbs with no unavailable message | Regression test now confirms the safe unavailable message is rendered; calendar route and test updated |
| D~22 | 2026-10-22 | Live 390px viewport showed the shared navbar top row extended the document by 15px on New/Edit listing routes | Allow the navbar top row to wrap; targeted regression passes, and all eight routes now render within a 390px document width |
| D~23 | 2026-10-22 | The initial live browser route sweep verified rendered route states and viewport bounds but did not capture exact Host HTTP request URLs, query parameters, or request counts required by N5 | Resolved in T10: authenticated Network captures recorded all eight route request sets, status codes, and actual GET counts in the implementation plan; browser OPTIONS preflights were excluded |
| D~24 | 2026-10-22 | On a later T10 resume, the owner had signed in; a fresh read-only capture showed `/api/auth/me` returning `200` and the Host listing route rendering API-backed state | Authenticated acceptance can continue without credentials being handled by the agent; no mutations were performed |
| D~25 | 2026-10-22 | A clean authenticated development reload duplicated the initial listings and shared wallet reads; React Strict Mode replayed mount effects and the provider did not coalesce same-key in-flight reads | Added provider-level in-flight request sharing for keyed page resources and wallet summary reads; the Strict Mode regressions pass, all 176 frontend tests/lint/build and backend build pass, and a fresh `/host/listings` capture showed one request each for `/api/auth/me`, `/api/host/listings?page=0&size=50`, and `/api/host/wallet?page=0&size=1`; the remaining route requests were later captured during T10 (D~23) |
| D~26 | 2026-10-22 | A clean Wallet route capture duplicated `GET /api/host/wallet?page=0&size=50` under Strict Mode even though the summary and keyed page resources were already coalesced | Added separate in-flight sharing for wallet statement pages and a Strict Mode regression test; the regression failed with two page reads before the fix, passed after it, and a fresh authenticated Wallet capture now shows one statement request and one wallet summary request |
| D~27 | 2026-10-22 | Read-only T10 review found stale gate wording and a handoff that still said route captures remained | Reconciled the evidence and next-action records; review found no code findings, and the final frontend/backend gates and plan closeout lint now pass |
| D~28 | 2026-10-22 | T11's approved file list omitted `PropertyRepositoryTest`, which directly asserted the listing page-query API that T11 removes | Resolved: add the repository test to T11 and replace paging assertions with complete stable-order and owner-scope coverage; no API scope change |
| D~29 | 2026-10-22 | T14's original file list omitted the four API-mode Host pages, which still consumed page-envelope types and separate wallet summary/statement calls | Resolved: include those route adapters and their route tests in T14 so each commit remains type-safe and consumes the complete response; T15/T16 still remove the now-inert pagination controls and handlers |
| D~30 | 2026-10-22 | T3's full frontend run found `HostListings.test.tsx` still asserted the prior `62%` presentation after occupancy changed to fixed two-decimal formatting | Resolved: added the existing fixture-backed test to T3's file list and updated its assertion to `62.00%`; no scope or behavior change |
| D~31 | 2026-10-22 | T4's empty-range change also invalidated the existing local-only calendar interaction test, which was not listed in T4's file inventory | Resolved: added `HostPresentationInteractions.test.tsx` to T4 and changed it to choose a partial range explicitly; full frontend tests pass |

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-21 | Specified the exact Claude-artifact Fall Light Host UI, synthetic fixture contract, local-only presentation behavior, and visual verification criteria | W-HOST | `c608be1` |
| 2026-10-21 | Added shared Fall Light tokens and UI primitives with accessible focus and status presentation | W-HOST | T1 |
| 2026-10-21 | Added deterministic synthetic Host fixture adapter, typed records, and state-replaceable provider | W-HOST | T2 |
| 2026-10-21 | Added Host-only fixture shell, explicit route map, active tabs, wallet badge, and `/host` redirect | W-HOST | T3 |
| 2026-10-21 | Added fixture-backed listing cards, detail metrics, and local-only new/edit listing forms | W-HOST | T4 |
| 2026-10-21 | Added calendar precedence, booking block presentation, and pending/history request screens | W-HOST | T5 |
| 2026-10-21 | Added Host messages, resolved-thread behavior, wallet statement, and mocked wallet controls | W-HOST | T6 |
| 2026-10-21 | Added cross-screen local-only interaction tests and no-network guarantees | W-HOST | T7 |
| 2026-10-21 | Corrected full-width Host framing, listing/detail/calendar/request parity, and edit-form typography; no Host New Ticket action; fixed inactive switch token and modal keyboard focus; all 97 frontend tests, lint, build, and three-viewport route checks pass | W-HOST | T8 |
| 2026-10-21 | Aligned New listing with the page title; removed status pills; centered five fixed-width card groups and added outlined calendar/edit actions; all 99 frontend tests, lint, build, and desktop/tablet browser checks pass | W-HOST | T9 |
| 2026-10-21 | Kept the availability switch to the right of its label and compacted the five desktop card groups; all 99 frontend tests, lint, production build, and desktop browser check pass | W-HOST | T9 follow-up |
| 2026-10-21 | Matched New/Edit listing forms to artifact card order, paired fields, six amenity rows, section styling, breadcrumbs, currency/time display, and action order while retaining full-width cards; 104 tests, lint, build, and browser checks pass | W-HOST | T4 follow-up |
| 2026-10-21 | Added visible Property type/Status labels; independently sized cards; tightened form gaps; positioned select menus directly below fields; increased breadcrumb spacing; 105 tests, lint, build, and browser check pass | W-HOST | T4 follow-up |
| 2026-10-21 | Split New/Edit listing cards into top-aligned desktop stacks, kept the narrow-screen card sequence, tightened vertical field/card gaps, and normalized select trigger weight; 105 tests and lint pass | W-HOST | T4 follow-up |
| 2026-10-21 | Applied shared chevron breadcrumbs to New, Edit, listing detail, and booking calendar routes; expanded route regression coverage; 106 tests and lint pass | W-HOST | T4 follow-up |
| 2026-10-21 | Added entry-example placeholders to New/Edit title, description, address, location, capacity, and rate inputs; 107 tests, lint, and production build pass | W-HOST | T4 follow-up |
| 2026-10-21 | Added pointer cursors for enabled buttons and hover feedback for outlined buttons and links | W-HOST | T1 follow-up |
| 2026-10-21 | Refined listing action link colors and listing-detail actions; restored page-title tracking style | W-HOST | Operator follow-up |
| 2026-10-21 | Made each listing card surface navigate to its detail page while preserving independent availability, calendar, and edit controls; 109 tests, lint, and build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched listing detail to the artifact with a gradient hero, six metrics, compact inline reviews/performance, and breadcrumb-row actions; 111 tests, lint, and isolated production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Increased detail/address typography; added review spacing; aligned Performance beside title/metrics at ~40% content width; top-aligned breadcrumb with shared Host placement; 111 tests, lint, isolated production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Simplified outlined listing-card action-link styles while retaining the shared hover background; 111 tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-21 | Added an intermediate three-column card-info layout to prevent the Edit action being stranded at narrow viewport widths; 111 tests, lint, production build, and 628px browser check pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched the calendar split panel, month/legend row, day colors and sizing, sample bookings, block form, and removable override; 114 tests, lint, production build, and browser check pass | W-HOST | Operator follow-up |
| 2026-10-21 | Added month-sized calendar week rows; artifact-style single range picker; required reason; and reason-first blocked-date entries with Maintenance fixture. 117 tests, lint, production build, and browser checks pass | W-HOST | Operator follow-up |
| 2026-10-21 | Fixed calendar month navigation to fixed arrow columns around the centered label; verified May and September alignment, 118 tests, lint, and production build | W-HOST | Operator follow-up |
| 2026-10-21 | Fixed date-picker weekday drift by anchoring reopened grids to the selected month’s first day; Sep 16 stays Wednesday after reopening. 119 tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Booking calendar now always renders six weeks; date picker stages edits for Apply, Clear leaves it open, outside click dismisses without applying, and picker/blocked-date ranges share year-aware formatting. 124 tests, lint, production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Bolded the current page in shared Host breadcrumbs, marked it with aria-current="page", and added a regression test; 125 frontend tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched booking requests hierarchy, card tables, emphasis, statuses, action buttons, and contextual approval/rejection dialogs; preserved header and full-width layout; 127 frontend tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched approval/rejection dialogs to artifact sizing, summary colors, copy order, input and action styling; outside clicks now dismiss modals without dismissing on inside clicks; 128 frontend tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Guarded backdrop dismissal by pointer origin so dragging the rejection textarea resize handle outside the modal no longer closes it; verified 128 tests, lint, production build | W-HOST | Operator follow-up |
| 2026-10-21 | Grouped the booking-requests pending pill directly beside its title while preserving the New listing title/action row; 128 tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-21 | Built direct-message threads from the three current pending booking requests, added listing/date/guest context and sender-aligned bubbles, retained the existing read-only support ticket without a New Ticket action; 130 tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Messages route now fills the viewport below the navbar; conversation and transcript columns stay within it and scroll internally; 132 tests, lint, isolated production build, and live viewport overflow check pass | W-HOST | Operator follow-up |
| 2026-10-21 | Message history scrolls to the newest message when a message is sent, and Enter sends the composer draft; 134 tests, lint, and isolated production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Refined Messages to use the artifact’s wider compact thread list, divided warm chat surface, smaller message bubbles, and bordered composer footer; 135 tests, lint, and isolated production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Padded the resolved/read-only notice inside the composer footer; updated the fixture title expectation to match the current in-progress Messages fixture shape; 135 tests, lint, isolated production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched the wallet balance panel, stacked actions, SGD and ledger table styling; expanded the sample statement to seven reconciled booking/wallet transactions; 138 tests, lint, production build pass | W-HOST | Operator follow-up |
| 2026-10-22 | Vertically centered the Top up/Withdraw stack against the available-balance panel; 138 tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-22 | Added viewport-contained scroll regions for 12 listings, four-visible pending requests, past requests, and 31 wallet transactions; table headers stay visible; 143 tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-22 | Prevented pending request cells from wrapping and adjusted the scroll cap so four complete rows fit before scrolling; 143 tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-22 | Inspected the live Requests page; reduced pending-cell padding and set a 224px cap so four complete single-line rows fit before scrolling; 143 tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-22 | Confirmed the fourth row clipped at 224px in the live screenshot; raised the pending-table cap to 244px and verified all four rows fully show before scrolling; requests tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-22 | Merged current `development` into W-HOST with the `ort` strategy; no conflicts or conflict markers; branch state base advanced to `4257d4b` | W-HOST | `6709cc8` |
| 2026-10-22 | Merged local `development`'s W-DOMAIN fold into W-HOST with `ort`; preview showed no conflicts; branch base advanced from `4257d4b` to `6b242b2` | W-HOST | `af32e3f` |
| 2026-10-22 | Marked the fixture-backed W-HOST UI plan complete as scoped and opened W-HOST-API for authenticated backend integration | W-HOST / W-HOST-API | `2026-10-22` |
| 2026-10-22 | Drafted W-HOST-API spec; lint passed, but Implementer/Tester/Maintainer/Attacker review found unresolved High-risk contract blockers recorded under Q~1 | W-HOST-API | `2026-10-22` |
| 2026-10-22 | Resolved user-approved W-HOST-API defaults; fixed second-round findings by aligning idempotency uniqueness/hash scope and defining bounded database paging for summaries, histories, and wallet statements | W-HOST-API | `2026-10-22` |
| 2026-10-22 | Removed wallet idempotency table, key/replay API, and deduplication requirements per C~16; retained atomic wallet ledger/audit behavior and specified refresh instead of automatic retry | W-HOST-API | `2026-10-22` |
| 2026-10-22 | Recorded operator approval of the amended W-HOST-API spec and drafted its ten-task implementation plan; plan review and Gate 2 approval remain pending | W-HOST-API | `2026-10-22` |
| 2026-10-22 | Recorded owner approval of the reviewed 10-task, six-wave W-HOST-API plan; corrected frontend test commands to match the `vitest run` package script | W-HOST-API | `2026-10-22` |
| 2026-10-22 | Restored frontend dependencies after initial registry DNS failures; backend build and frontend 143 tests, lint, and production build pass; started T1 | W-HOST-API | D~3 |
| 2026-10-22 | Corrected backend Checkstyle command to `checkstyleMain checkstyleTest` after Gradle reported `checkstyle` ambiguous; T1 checks rerun pending | W-HOST-API | D~4 |
| 2026-10-22 | Added verified Host identity cross-checking, controller-scoped safe error responses, and security/profile-drift tests; focused tests, Checkstyle, and full backend build pass | W-HOST-API | T1 |
| 2026-10-22 | Expanded T2's planned file list to include title/description DTO validation already required by the approved API contract | W-HOST-API | D~5 |
| 2026-10-22 | T2 exposed unavailable `ListingService` bean activation in Spring; paused for owner direction on the prerequisite cross-cutting wiring | W-HOST-API | D~6 |
| 2026-10-22 | Amended the W-HOST-API contract and plan with T0: property-controlled activation for W-DOMAIN services/adapters and enabled/disabled context coverage | W-HOST-API | C~17 |
| 2026-10-22 | Clarified T2's zero-based page offset and bounded `size + 1` query mechanism after review exposed a skip on later listing pages | W-HOST-API | D~7 |
| 2026-10-22 | Activated W-DOMAIN persistence beans with the approved property gate; T0 context tests, Checkstyle, and backend build pass | W-HOST-API | C~17 |
| 2026-10-22 | Implemented Host listing collection/detail/create/update/status routes and metrics projection; fixed status-omitting update validation and verified page continuity | W-HOST-API | T2 |
| 2026-10-22 | Committed T0 deterministic service activation as `9ba4ce2` and T2 Host listing API as `b57b096`; post-commit backend Checkstyle and build pass | W-HOST-API | T0/T2 |
| 2026-10-22 | Added the authenticated Host calendar API, bounded month projection, inclusive range responses, audited block mutations, and serializable retry; focused tests and full backend checks pass | W-HOST-API | T3 |
| 2026-10-22 | Committed the atomic Host calendar API as `fb4188a`; starting T4 booking-request paging and decision serialization | W-HOST-API | T3/T4 |
| 2026-10-22 | Added authenticated pending/history booking pages, 500-code-point decision validation, row-locked one-shot decisions, transactional decision audit and after-commit events; PostgreSQL tests and backend Checkstyle/build pass | W-HOST-API | T4 |
| 2026-10-22 | Committed serialized Host booking decisions as `740e1bc`; starting T5 booking/ticket messaging APIs | W-HOST-API | T4/T5 |
| 2026-10-22 | Added distinct authenticated booking/ticket message routes, database-bounded summary/history paging, transactional read/post operations, after-commit events and rollback tests; fixed unread-count timestamp comparison; Checkstyle/build pass | W-HOST-API | T5 |
| 2026-10-22 | Committed Host booking and ticket messaging APIs as `0b1f0ef`; starting T6 wallet API | W-HOST-API | T5/T6 |
| 2026-10-22 | Added authenticated wallet balance, bounded newest-first statement and separate mocked top-up/withdraw operations; fixed stale-row locking, PostgreSQL timestamp mapping and after-commit event dispatch; five wallet integration tests, Checkstyle and backend build pass | W-HOST-API | T6 |
| 2026-10-22 | Committed T6 wallet API as `ba67ad4`; beginning T7 authenticated frontend API adapter | W-HOST-API | T6/T7 |
| 2026-10-22 | Added authenticated typed Host API route wrappers, bounded page validation, DTO mapping, shared async resource states, safe errors and token-rejection sign-out; cached wallet summary, lazy statement paging and no-retry ambiguous-failure refresh; 154 frontend tests, lint and production build pass | W-HOST-API | T7 |
| 2026-10-22 | Committed T7 authenticated Host API adapter as `1d0adda`; starting T8 listing, calendar and request route tests | W-HOST-API | T7/T8 |
| 2026-10-22 | Connected Host listing collection/detail and create/edit/status mutations to server data; API-mode route/form tests pass while fixture-backed visual tests remain supported | W-HOST-API | `2d8190f` |
| 2026-10-22 | Connected calendar month reads, audited block create/remove, bounded pending/history pages and server-authoritative booking decisions; full frontend suite 166 tests, lint and production build pass | W-HOST-API | `2d8190f`, `80f3322` |
| 2026-10-22 | Connected separate booking/ticket messages and paged history, plus wallet statement and mocked mutations with ambiguous-failure recovery lock; 172 frontend tests, lint and production build pass | W-HOST-API | T9 |
| 2026-10-22 | T10 fresh backend Checkstyle/build and frontend 172-test/lint/production-build gates pass; authenticated browser/network acceptance remains open per D~20 | W-HOST-API | T10 |
| 2026-10-22 | Added calendar 404 unavailable-state regression/fix and responsive navbar wrapping; all eight routes fit at desktop/tablet/mobile, 173 frontend tests and lint pass, production build passes on rerun; N5 live network capture remains unverified | W-HOST-API | T10 |
| 2026-10-22 | Coalesced same-key in-flight page and wallet-summary reads to prevent duplicate Host API calls during React Strict Mode effect replay; 175 frontend tests, lint, production build, and backend build pass | W-HOST-API | T10 / D~25 |
| 2026-10-22 | Coalesced in-flight wallet statement page reads after live Wallet acceptance exposed a Strict Mode duplicate; the regression failed before the fix and the clean Wallet recapture shows one statement fetch | W-HOST-API | T10 / D~26 |
| 2026-10-22 | Closed T10 with eight-route authenticated request capture, 176 frontend tests, lint/build, backend build, read-only review, and plan closeout lint | W-HOST-API | T10 |
| 2026-10-22 | Unified listing collection/detail/edit/calendar route-load failures behind a generic error page with a return link; collection errors retain retry. Validation and mutation errors remain inline; 179 frontend tests, lint, and production build pass | W-HOST | C~18 |
| 2026-10-22 | Reversed bounded Host collection/history pagination; corrected T12 dependency after source inspection showed a shared BookingRepository edit; approved T11-T17 plan (seven tasks, waves 7–11) and began T11 | W-HOST-API | C~19 |
| 2026-10-22 | Replaced paged Host listing and pending/history request reads with complete ordered owner-scoped arrays; legacy page/size params return identical results. 101-row integration coverage, repository ordering/scope tests, Checkstyle, and backend build pass | W-HOST-API | T11 |
| 2026-10-22 | Replaced paged booking/ticket conversation summary and history APIs with complete stable-order arrays; retained host/channel scoping, read/post transactions, and after-commit events. Tests cover 101 summaries/messages, a 1,001-message thread, legacy parameter equivalence, privacy; Checkstyle and backend build pass | W-HOST-API | T12 |
| 2026-10-22 | Replaced the paged wallet statement with a complete newest-first owner-scoped array and stable timestamp/ID ordering; 101-row plus foreign-owner integration coverage, legacy parameter equivalence, Checkstyle, and backend build pass | W-HOST-API | T13 |
| 2026-10-22 | Replaced frontend page envelopes with complete arrays, removed page/size URL generation, and shared the complete wallet response across navbar and Wallet; 177 frontend tests, lint, and production build pass | W-HOST-API | T14 |
| 2026-10-22 | Removed listing and request page state, handlers, and controls; 101-row listing/pending/history coverage passes with 177 frontend tests, lint, and production build | W-HOST-API | T15 |
| 2026-10-22 | Removed message-history and wallet load-more state/controls; 1,001 messages and 101 statement rows render with 178 frontend tests, lint, production build pass; full wallet recovery behavior retained | W-HOST-API | T16 |
| 2026-10-22 | Verified complete Host collections: backend full rerun, 178 frontend tests/lint/production build, 101/1,001-row integration evidence, and authenticated GET-only browser captures with no page/size parameters; no N1/N3/N4 findings | W-HOST-API | T17 |
| 2026-10-22 | Merged `development` through `e185dc0`; adopted `@ConditionalOnDatabase`, retaining W-HOST behavior and both branches' audit actions; backend build and frontend tests/lint/production build pass | W-HOST | C~20 |
| 2026-10-22 | Added and approved the Host experience-corrections spec; approved the reviewed four-task plan and committed it separately | W-HOST | C~21, C~22, `a9494d3` |
| 2026-10-22 | Added complete Host listing rows with booking counts and average ratings using two owner-scoped aggregate queries; empty collections skip metrics; integration tests, Checkstyle, and backend build pass | W-HOST | T1 |
| 2026-10-22 | Corrected exact HH:mm detail/edit values and two-decimal occupancy; updated the stale fixture assertion; 184 frontend tests, lint, and production build pass | W-HOST | T3, D~30 |
| 2026-10-22 | Kept the calendar frame and block form mounted during month reads, showed loading only in the grid, synchronized the picker to the visible month, and cleared the initial range in API/fixture modes; 187 tests, lint, and production build pass | W-HOST | T4, D~31 |
