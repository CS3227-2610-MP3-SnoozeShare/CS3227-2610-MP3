# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Freeze Host API contract and controller support | Endpoint mappings, principal-to-domain-user resolver, error envelope, and shared controller test fixtures | F1-F5, N1-N4 |
| T2 | Implement listing and calendar controllers | Authenticated listing/detail/form/status/calendar/block endpoints | F1,F2,N1-N3 |
| T3 | Implement requests, messages, and wallet controllers | Authenticated booking decisions, conversations, messages, wallet reads/mocked mutations | F3-F5,N1-N4 |
| T4 | Build the frontend authenticated Host API adapter | Token-attaching client, typed responses, stable error mapping, and provider state interface | F6,F7,N3-N5 |
| T5 | Integrate Host screens incrementally | Listings, calendar, requests, messages, and wallet use API state/mutations while retaining fixture test adapters | F6,F7,N3,N5 |
| T6 | Verify end-to-end Host behavior and close the phase | Controller/security/integration/frontend tests, build, visual regression, and branch-state handover | F1-F7,N1-N5 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Freeze Host API contract and controller support | - | - | 1 |
| T2 Implement listing and calendar controllers | T1 | T3 | 2 |
| T3 Implement requests, messages, and wallet controllers | T1 | T2 | 2 |
| T4 Build the frontend authenticated Host API adapter | T1 | T2,T3 | 2 |
| T5 Integrate Host screens incrementally | T2,T3,T4 | - | 3 |
| T6 Verify end-to-end Host behavior and close the phase | T5 | - | 4 |

Shared-file hazards: T2 and T3 use separate controller/test packages; T4 and T5 both touch `HostFixtureProvider` and must be serialized after T4's adapter contract is stable. T6 alone edits close-out state.
