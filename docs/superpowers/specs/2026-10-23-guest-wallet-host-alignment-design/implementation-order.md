# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Add failing Guest header and wallet alignment tests | RED tests for header balance, loading/failure states, and Host-aligned wallet structure | Header behavior; page structure; preserved Guest behavior |
| T2 | Load Guest balance in the role shell | Guest header wallet label with Host formatting and explicit loading/failure states | Header balance and loading/failure states |
| T3 | Align Guest wallet page presentation | Host-aligned wallet UI while preserving Guest API and transaction semantics | Host-aligned wallet UI and preserved Guest semantics |
| T4 | Run focused and full frontend verification | Regression evidence and branch handoff | Regression safety and branch handoff |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 | - | - | 1 |
| T2 | T1 | T3 | 2 |
| T3 | T1 | T2 | 2 |
| T4 | T2, T3 | - | 3 |

Shared-file hazards: T2 and T3 have disjoint production write sets, but both depend on T1 tests; serialize if test fixtures overlap.
