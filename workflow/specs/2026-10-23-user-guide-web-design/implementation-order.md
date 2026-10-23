# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Inventory current web user experience | A retained source-to-section checklist for the post-approval plan, with `Guide section`, `required claim or route`, `source path`, and `verification` columns, covering every required section and feature in F1–F4 | N1 |
| T2 | Rewrite guide structure and quick start | Browser-first introduction, access/setup, account flows, and current navigation | F1, F2, F4, N2 |
| T3 | Rewrite role workflows and feature reference | Current Guest, Host, and Support Agent journeys, including implemented AI assistants and explicit screenshot placeholders | F3, F4, N1 |
| T4 | Validate guide | No desktop-only claims, source-to-section checklist complete, valid internal anchors and Markdown/image paths, and a source-backed guide ready for screenshot replacement | F1, F2, F3, F4, N1, N2, E1, E2, E3, E4, E5, E6, E7, E8 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Inventory current web user experience | - | - | 1 |
| T2 Rewrite guide structure and quick start | T1 | - | 2 |
| T3 Rewrite role workflows and feature reference | T1 | - | 2 |
| T4 Validate guide and close out | T2, T3 | - | 3 |

Shared-file hazards: T2 and T3 both edit `docs/UserGuide.md`; serialize them in the listed order.

## Validation evidence

- T1 records the source-to-section checklist in the post-approval plan's Traceability section; the checklist is reviewed against `frontend/src/app/`, `frontend/src/components/`, `frontend/src/lib/`, `backend/src/main/java/`, `docs/runbooks/dev-environment.md`, and `PROJECT_STATE.md`.
- T4 runs `git diff --check`, the spec lint, the approved plan lint, and a short link audit that extracts every Markdown link, every `(#...)` anchor, and every `images/...` reference from `docs/UserGuide.md`, then verifies local targets, Jekyll page targets (`/product-backlog/` and `/developer-guide/`), headings, and image files. It also runs `rg -n -i 'javafx|sqlite|snoozeshare-mock|gradlew|shadowJar|single-user|mocked authentication|email only' docs/UserGuide.md` and reviews every match.
- T4 manually checks every retained image and its caption or immediately following prose for the exact screenshot replacement marker, then checks the source-to-section checklist and verified deployment URL against the runbook.
