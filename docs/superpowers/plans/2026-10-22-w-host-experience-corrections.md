# W-HOST Experience Corrections Implementation Plan

- **Status:** Approved (Trevor, 2026-10-22)

> **For agents:** tick `- [ ]` -> `- [x]` as each step is completed, in the same commit as the work. Keep the branch state's Progress current. Spec: [W-HOST experience corrections](../specs/2026-10-22-w-host-experience-corrections-design.md).

## Scope

**Goal:** Show accurate Host listing metrics and preserve stable listing, detail, and calendar UI state.
**In scope:** F1–F6, N1–N2, E1–E9.
**Non-goals:** Authentication/ownership changes; pagination or collection caps; calendar API/write changes; listing form time-entry or `LocalTime` changes; fixture removal; generic error-contract changes.
**Risk tier:** Medium — authenticated owner-scoped listing data and Host UI state. Verify the backend owner boundary and query count, frontend regressions, lint, and production builds.

## Task order

| Task | Depends on | Can run concurrently with | Wave | Status |
|---|---|---|---|---|
| T1 Add batched owner-scoped listing summary metrics | - | T3, T4 | 1 | Done |
| T2 Consume metrics and update listing status in place | T1 | T4 | 2 | Done |
| T3 Correct listing detail time and occupancy formatting | - | T1, T4 | 1 | Done |
| T4 Keep calendar mounted during month loads and clear initial range | - | T1, T2, T3 | 1 | Done |

Tasks T2 and T3 both edit `frontend/src/components/host/hostTypes.ts`; perform them serially. The backend task and calendar task otherwise have disjoint files. The Wave column and task metadata mirror the spec; this note records the shared-file serialization needed during execution.

## Traceability

| Requirement | Task(s) | Verified by |
|---|---|---|
| F1 | T1, T2 | `HostListingsControllerTest`, `HostListingsPage` tests |
| F2 | T2 | `frontend/src/app/host/listings/page.test.tsx` |
| F3 | T3 | `frontend/src/lib/hostApi.test.ts`, listing detail and edit page tests |
| F4 | T3 | `frontend/src/app/host/listings/[listingId]/page.test.tsx` |
| F5 | T4 | `frontend/src/app/host/listings/[listingId]/calendar/page.test.tsx` |
| F6 | T4 | `HostCalendarRequests.test.tsx`, calendar page test |
| N1 | T1, T2 | `ListingMetricsServiceImplTest`, `HostListingsControllerTest`, listings page test |
| N2 | T1 | `HostListingsControllerTest` |
| E1 | T1 | `HostListingsControllerTest` |
| E2 | T1, T2 | `ListingMetricsServiceImplTest`, listings page test |
| E3 | T2 | `frontend/src/app/host/listings/page.test.tsx` |
| E4 | T3 | `frontend/src/lib/hostApi.test.ts`, listing detail page test under UTC+8 and UTC-8 |
| E5 | T3 | listing detail page test |
| E6 | T4 | calendar page test |
| E7 | T4 | calendar page test |
| E8 | T4 | `HostCalendarRequests.test.tsx` and calendar page test |
| E9 | T1 | `HostListingsControllerTest` |

## Verification gates

- Backend changed: run `./gradlew checkstyleMain checkstyleTest` and `./gradlew build` from `backend/` (separately to limit memory); targeted tests use `./gradlew test --tests <class>`.
- Frontend changed: run `npm test`, `npm run lint`, and `npm run build` from `frontend/`; build requires `NEXT_PUBLIC_API_BASE_URL`.
- Before finishing, both component gates are green; every F/N has an automated test or documented manual check; `node .claude/skills/writing-specs-lite/scripts/lint.mjs plan docs/superpowers/plans/2026-10-22-w-host-experience-corrections.md --closeout` passes.

---

### T1: Add batched owner-scoped listing summary metrics

**Delivers:** The complete Host listing response includes booking counts and average ratings from at most two host-scoped aggregate queries. | **Satisfies:** F1, N1, N2, E1, E2, E9 | **Depends on:** - | **Wave:** 1

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/service/ListingSummaryMetrics.java` | Immutable count/average value for one listing; spec `api.md` response metrics. |
| Create | `backend/src/main/java/com/snoozeshare/api/response/HostListingSummaryResponse.java` | Collection-row DTO with every existing `PropertyResponse` field plus `bookingCount` and `averageRating`; spec `api.md` compatibility. |
| Modify | `backend/src/main/java/com/snoozeshare/service/ListingMetricsService.java` (`ListingMetricsService`) | Add one host-scoped batch summary method returning metrics keyed by listing UUID; do not alter detail metrics. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/ListingMetricsServiceImpl.java` (`ListingMetricsServiceImpl`) | Use two grouped `JdbcTemplate` aggregate queries: bookings by listing and reviews joined through bookings, each joined to `properties` and filtered by resolved `hostId`. Keep zero-review averages at 0.0; do no per-listing queries. |
| Modify | `backend/src/main/java/com/snoozeshare/api/controller/HostListingController.java` (`list`) | Resolve owner as today; return complete ordered summary rows, skip summary queries for an empty property list, and retain existing status/detail/create/update behavior. |
| Create | `backend/src/test/java/com/snoozeshare/service/impl/ListingMetricsServiceImplTest.java` | Mock `JdbcTemplate`; verify exactly two aggregate reads for a non-empty host batch at 1 and 101 listings, no per-listing calls, and correct zero/positive mapping. |
| Modify | `backend/src/test/java/com/snoozeshare/api/host/HostListingsControllerTest.java` (`listingCollectionReturnsAllOwnedPropertiesAndIgnoresLegacyPagination`) | Assert complete rows include metrics; seed owned bookings/reviews plus foreign-host data; assert counts/average and no foreign values. Add an empty-owner case and verify the summary method is skipped. |

**Checklist**

- [x] 1. Add failing service tests for E2: host batches of 1 and 101 listings each perform exactly two grouped query calls and no per-listing query. Run `./gradlew test --tests com.snoozeshare.service.impl.ListingMetricsServiceImplTest` in `backend/`; it failed at test compilation because the batch method and summary record are not yet implemented.
- [x] 2. Add the summary value and batch method; make the two aggregate queries use `properties.host_id = ?`, with booking counts and review averages grouped by listing. Return 0.0 where no review group exists.
- [x] 3. Add failing authenticated controller integration coverage for E1/E9: owned metrics, no-review `0.0`, foreign-host exclusion, all existing properties/order, legacy parameters unchanged, and empty collection; verify the controller skips the summary service for an empty owner collection. The contract test failed on missing collection metrics before the response update.
- [x] 4. Implement the summary response DTO and update only `HostListingController.list`; skip the summary call for an empty list and map missing metric groups to zero values.
- [x] 5. Run targeted service and controller tests; expect PASS. Run `./gradlew checkstyleMain checkstyleTest` and `./gradlew build` from `backend/`.
- [x] 6. Update branch state Progress and Changes; commit `feat: return batched Host listing metrics`.

### T2: Consume metrics and update listing status in place

**Delivers:** Listing cards display server metrics and a status toggle patches only the matching row without reloading the collection. | **Satisfies:** F1, F2, N1, E1, E2, E3 | **Depends on:** T1 | **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/components/host/hostTypes.ts` (`HostListingDto`, collection summary type, `mapHostListing`) | Keep `HostListingDto` as the existing property shape used by detail/create/update/status; add a distinct collection-summary DTO with required `bookingCount`/`averageRating`; map collection rows only from that type. |
| Modify | `frontend/src/lib/hostApi.ts` (`fetchHostListings`) | Type only the collection GET as the summary DTO array; preserve the detail endpoint's `HostListingDetailDto` response shape. |
| Modify | `frontend/src/app/host/listings/page.tsx` (`toggle`) | Read the returned `PropertyResponse`, patch only its `status` on the existing row in the `host:listings` resource, preserve metrics and other rows, and avoid a second collection GET. Keep the row-level pending state and safe inline failure. |
| Modify | `frontend/src/components/host/HostApiProvider.tsx` (`HostApiContextValue`, resource state) | Add a narrow resource update operation if needed so the page can patch the ready/empty listings array without reloading it; do not replace unrelated resources. |
| Modify | `frontend/src/app/host/listings/page.test.tsx` | Cover populated server metrics, status success preserving other cards/metrics with exactly one collection GET, and failure preserving old status with safe inline error. |
| Modify | `frontend/src/lib/hostApi.test.ts` | Update collection DTO mapping expectation for booking count and average rating; keep detail DTO fixtures free of collection-only fields. |

**Checklist**

- [x] 1. Add failing UI tests for E3 and metric mapping: successful toggle returns a `PropertyResponse`, updates only its card, preserves counts/ratings, and makes no second collection GET; failure leaves status unchanged and shows the generic inline error. Run `npm test -- src/app/host/listings/page.test.tsx src/lib/hostApi.test.ts` in `frontend/`; observed expected failures for zero metrics and stale status after refetch.
- [x] 2. Add a collection-only summary DTO and type `fetchHostListings` with it; leave the property DTO used by the detail endpoint unchanged. Map server metrics from collection rows.
- [x] 3. Add the smallest typed provider resource-patch operation needed to update the listing array. The status mutation response has no metrics: copy only its `status` field into the matching existing DTO row; never remap it as a complete summary row.
- [x] 4. Run focused tests; expect PASS. Run `npm test`, `npm run lint`, and `npm run build` from `frontend/`.
- [x] 5. Update branch state Progress and Changes; commit `fix: update Host listing status in place`.

### T3: Correct listing detail time and occupancy formatting

**Delivers:** Listing detail shows exact saved `HH:mm` time and a percentage with exactly two fractional digits. | **Satisfies:** F3, F4, E4, E5 | **Depends on:** - | **Wave:** 1

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/components/host/hostTypes.ts` (`formatTime`) | Extract the leading `HH:mm` directly from the time-only API string; do not construct a `Date` or apply timezone formatting. |
| Modify | `frontend/src/components/host/HostListingDetail.tsx` (occupancy row) | Render `listing.occupancy30d.toFixed(2)` followed by `%`. |
| Modify | `frontend/src/lib/hostApi.test.ts` | Assert `07:00:00` and `03:00:00` map exactly to `07:00` and `03:00` under simulated UTC+8 and UTC-8. |
| Modify | `frontend/src/app/host/listings/[listingId]/page.test.tsx` | Assert occupancy renders as `0.00%`, `12.50%`, and `100.00%` across detail fixtures/API cases. |
| Modify | `frontend/src/app/host/listings/[listingId]/edit/page.test.tsx` | Assert API-provided `07:00` and `03:00` populate the edit time inputs unchanged. |
| Modify | `frontend/src/components/host/HostListings.test.tsx` | Update the existing fixture-backed detail assertion to expect two-decimal occupancy. |

**Checklist**

- [x] 1. Add failing E4/E5 regression tests for both timezone settings (UTC+8 and UTC-8), edit-form round-trip input values, and zero/fractional/100 percent output. Focused tests exposed the time shift and precision behavior; the full run identified the existing fixture assertion now listed above.
- [x] 2. Replace date/timezone conversion with direct time-string formatting and fix occupancy precision.
- [x] 3. Run focused tests; expect PASS. Run `npm test`, `npm run lint`, and `npm run build` from `frontend/`.
- [x] 4. Update branch state Progress and Changes; commit `fix: preserve Host listing time and occupancy display`.

### T4: Keep calendar mounted during month loads and clear initial range

**Delivers:** Calendar navigation keeps the page and block form mounted, shows grid-local loading, and starts with an empty block-date range in API and fixture modes. | **Satisfies:** F5, F6, E6, E7, E8 | **Depends on:** - | **Wave:** 1

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/app/host/listings/[listingId]/calendar/page.tsx` (`calendarResource`, loading/error selection, `HostCalendarPanel` props) | Keep the route-level skeleton only for initial listing/month loads; on later month changes retain the frame and panel, pass requested month and loading state, and preserve generic load error behavior. |
| Modify | `frontend/src/components/host/HostCalendarPanel.tsx` (`range` state, calendar effect, month heading/grid) | Initialize `from`, `to`, `draftFrom`, and `draftTo` empty; open picker at visible month; update month label immediately; render loading only in grid while preserving applied range/reason; refresh blocked dates from the successful month response; retain outside-click draft discard. |
| Modify | `frontend/src/app/host/listings/[listingId]/calendar/page.test.tsx` | Cover pending next/previous-month load, requested label, mounted panel and preserved applied range/reason, month-specific blocked-date refresh, 404/error generic return link, and no stale grid. |
| Modify | `frontend/src/components/host/HostCalendarRequests.test.tsx` | Assert fixture mode starts with “Select dates,” has no pressed days, and opens picker at visible fixture month. |
| Modify | `frontend/src/components/host/HostPresentationInteractions.test.tsx` | Select dates explicitly in the local-only invalid-range regression after the initial range becomes empty. |

**Checklist**

- [x] 1. Add failing E6/E7/E8 tests for both API and fixture modes. In the pending-month case assert the panel remains mounted, the requested month label changes immediately, only the grid indicates loading, applied dates/reason stay unchanged, and the new blocked list appears only after success. Also open the picker, change its draft, then navigate months from outside it and assert the picker closes, the draft is discarded, and the applied range remains. Expect FAIL.
- [x] 2. Keep the last successful calendar data available while a new month resource is loading; pass an explicit requested month/loading state to the panel so stale month cells are replaced by grid loading without removing the form.
- [x] 3. Remove generated sample range initialization; use empty applied/draft values and synchronize the date picker month with the visible calendar month. Preserve the existing outside-click close/discard behavior.
- [x] 4. Verify a failed month load routes to the existing generic error with Back to Listings, without displaying the prior month as the requested month.
- [x] 5. Run focused calendar tests; expect PASS. Run `npm test`, `npm run lint`, and `npm run build` from `frontend/`.
- [x] 6. Update branch state Progress and Changes; commit `fix: keep Host calendar state during month changes`.

### Close-out

- [ ] Every Traceability row verified; backend Checkstyle/build and frontend tests/lint/build are green.
- [ ] Spec status and plan Task order match actual work; record any plan/spec deviation in the branch state before continuing.
- [ ] Branch state: W-HOST workstream In review; clear session rows only when all work is reflected in Workstreams and Changes; update Conventions/Deviations and note `guide out of date: Host metrics, time display, and calendar behavior`.
- [ ] GATE: Ask Trevor for Gate 3 review; do not set branch `Approved`, push, open a PR or merge before the owner's yes.
