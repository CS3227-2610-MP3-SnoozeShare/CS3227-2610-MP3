# Implementation order

| Task | Outcome | Requirements |
|---|---|---|
| T1 | Public pre-validation request/response contract, security-chain exception, trusted client-IP resolver, and atomic IP rate-limit support | F9–F12, F14 |
| T2 | Frontend public API helper, registration ordering, and stable error mapping | F9–F11, F13–F14 |
| T3 | Backend and frontend regression tests for route exposure, validation, ordering, races, boundaries, and bypass resistance | F9–F14, N4 |
| T4 | Full verification and branch-state evidence | F9–F14, N4 |
| T5 | Close-out and handoff evidence | F9–F14, N4 |

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 | — | — | 1 |
| T2 | T1 | — | 2 |
| T3 | T1, T2 | — | 3 |
| T4 | T3 | — | 4 |
| T5 | T4 | — | 5 |
