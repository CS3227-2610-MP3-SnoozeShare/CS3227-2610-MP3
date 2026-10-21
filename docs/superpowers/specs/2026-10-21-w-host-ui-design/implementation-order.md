# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Fall Light token and shared primitive contract | Shared colors, typography, spacing, borders, cards, buttons, fields, badges, tables, modal, and calendar presentation primitives | F2, N1, N3, N4 |
| T2 | Host fixture adapter and presentation types | Deterministic populated, empty, loading, and error data for listings, detail, calendar, requests, messages, and wallet; default populated state and test-replaceable state provider | F10, N5 |
| T3 | Host shell and route navigation | Correct Host layout, active tabs, explicit Host route map, `/host` redirect, and guarded route tree without changing Guest/Agent destinations | F1, F2, N2 |
| T4 | Listings, detail, and edit/new listing screens | Artifact-matching listing rows, detail metrics, and form cards | F3, F4, F5 |
| T5 | Calendar and requests screens | Artifact-matching calendar/block panel and pending/history request tables | F6, F7 |
| T6 | Messages and wallet screens | Shared Host inbox/thread and wallet/statement presentation | F8 |
| T7 | Local presentation interactions and non-domain states | Navigation, modal open/close, fixture-only control behaviour, error/empty states, and no-network guarantees | F9, E1–E11, E16–E17 |
| T8 | Visual and accessibility verification | Screenshot comparison at 1680×945 plus responsive checks at 1280×800 and 768×1024; visual pass allows only anti-aliasing differences up to 2px at edges | N1, N3, N4, N5, E12–E15 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Fall Light primitives | - | T2 | 1 |
| T2 Fixture adapter | - | T1 | 1 |
| T3 Host shell and routes | T1 | - | 2 |
| T4 Listings and listing forms | T1, T2, T3 | T5, T6 | 3 |
| T5 Calendar and requests | T1, T2, T3 | T4, T6 | 3 |
| T6 Messages and wallet | T1, T2, T3 | T4, T5 | 3 |
| T7 Local presentation interactions | T4, T5, T6 | - | 4 |
| T8 Visual and accessibility verification | T7 | - | 5 |

Shared-file hazards: T1 and T3 both edit the shared shell styling; serialize them. T4, T5, and T6 may run concurrently only after T3 and must keep page-specific styling in their own component files; any shared primitive changes return to T1 and are serialized.
