# Implementation Order

| Task | Outcome | Delivers | Requirements |
|---|---|---|---|
| T1 | Host AI contract types and prompt/output schema are defined | Shared request/response, focus enum, fixed length guidance, analysis validation, feature id and exact fallback contract | F3–F5, F8, N1–N2, N5 |
| T2 | Backend Host AI operation is authorized and gateway-backed | `POST /api/host/ai/listing-description`, Host ownership check, draft-context validation, feature registration, safe error mapping, and backend tests | F3–F5, F7–F8, N1–N2, N5–N6 |
| T3 | Typed frontend Host AI adapter and modal state model exist | Authenticated request wrapper and isolated modal state for Evaluate/Generate/loading/error/Apply, focus dropdown, and audience order | F2–F6, F8, N3 |
| T4 | New/Edit forms include the compact assistant trigger and modal | Existing form integration, responsive two-column/stacked layout, icon, analysis rows, and Apply-to-draft behavior | F1–F6, N3–N4 |
| T5 | Full verification and close-out are complete | Backend/frontend tests, Checkstyle, lint, production build, browser viewport checks, code review, and branch-state handoff | F1–F8, N1–N6 |

## Dependency table

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 | — | — | 1 |
| T2 | T1 | T3 | 2 |
| T3 | T1 | T2 | 2 |
| T4 | T3 | — | 3 |
| T5 | T2, T4 | — | 4 |

T2 and T3 may be developed concurrently after T1 because they share only the contract, but T4
waits for the frontend adapter and modal state. T5 is serialized after the integrated flow exists.
