# Implementation order

| Task | Outcome | Requirements |
|---|---|---|
| T1 | Auth schema contract and migration | F3, F5, N3 |
| T2 | Firebase Admin configuration and token-verifier abstraction | F2, N1, N2 |
| T3 | Spring Security dependency, stateless filter chain and role authorization | F2, F6, N1 |
| T4 | Registration-code policy, registration service and auth endpoints | F3, F4, F5, F7, N3 |
| T5 | Backend auth and registration tests | F2–F7, N1–N3 |
| T6 | Firebase JS initialization, synchronized lockfile and token-aware API client | F1, F2, F7, N1, N2 |
| T7 | AuthProvider, auth screens and protected role layouts | F1, F8, N1 |
| T8 | Frontend auth and guard tests | F1, F8, N2 |
| T9 | Full verification and branch-state evidence | F1–F8, N1–N3 |

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 | - | T2, T6 | 1 |
| T2 | - | T1, T6 | 1 |
| T3 | T1, T2 | T6 | 2 |
| T4 | T1, T2, T3 | T6 | 3 |
| T5 | T1, T2, T3, T4 | T8 | 4 |
| T6 | - | T1, T2 | 1 |
| T7 | T6, T4 | T5 | 4 |
| T8 | T6, T7 | T5 | 5 |
| T9 | T5, T8 | - | 6 |
