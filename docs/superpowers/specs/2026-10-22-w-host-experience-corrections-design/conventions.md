# Conventions

Cite existing `PROJECT_STATE.md` § Conventions by ID; list only rules applied here.

| Rule | Applies to | Source |
|---|---|---|
| Keep the Host listing collection complete and deterministically ordered; do not add paging or in-memory slicing. | Listing API and frontend | W-HOST branch state § Decisions (complete collections) |
| Use the authenticated principal and existing server-side Host ownership checks; aggregate by the resolved Host ID. | Listing summary metrics | W-HOST branch state § Architecture (Host API) |
| Treat API `LocalTime` values as time-only strings; format `HH:mm` without a `Date`, timezone, or UTC conversion. | Listing detail and edit round-trip | New in this spec (SD3) |
| Preserve existing generic route/load errors and safe inline mutation errors. | Host listing and calendar flows | W-HOST branch state § Decisions (route/load failures) |
| Use regression tests for behavior first; run frontend tests/lint/build and backend Checkstyle/build for changed components. | Verification | `PROJECT_STATE.md` § Conventions; `AGENTS.md` §§ 3, 6 |
