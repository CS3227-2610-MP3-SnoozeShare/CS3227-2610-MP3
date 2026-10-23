# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Add batched owner-scoped listing summary metrics | Collection response carries booking count and average rating without per-listing queries | F1, N1, N2, E1, E2, E9 |
| T2 | Consume metrics and update listing status in place | Listing cards display real metrics; status changes use the server mutation response | F1, F2, E1, E2, E3 |
| T3 | Correct listing detail time and occupancy formatting | Time-only values stay exact; percentages have fixed precision | F3, F4, E4, E5 |
| T4 | Keep calendar mounted during month loads and clear initial range | Month loading is grid-local and date range starts empty | F5, F6, E6, E7, E8 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Add batched owner-scoped listing summary metrics | - | T3, T4 | 1 |
| T2 Consume metrics and update listing status in place | T1 | T4 | 2 |
| T3 Correct listing detail time and occupancy formatting | - | T1, T4 | 1 |
| T4 Keep calendar mounted during month loads and clear initial range | - | T1, T2, T3 | 1 |

Shared-file hazards: T2 and T3 both modify `frontend/src/components/host/hostTypes.ts`; serialize them even though the dependency table permits other work to overlap. T1 backend files and T3/T4 frontend files are disjoint.
