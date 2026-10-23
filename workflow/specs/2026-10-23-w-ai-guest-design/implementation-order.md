# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | AiFeature bean and LLM output record | The `guest.search` feature registration, `AiSearchFilters` record, `GuestAiConfig` configuration class, and a unit test proving the feature validates and registers at startup | F1, F3 |
| T2 | Backend endpoint and service | `POST /api/guest/ai/search` controller, request/response DTOs, the service method that calls the gateway and then the existing search (with `IllegalArgumentException` catch for LLM-extracted invalid dates), integration tests covering happy path, degraded, validation, permission, and date-rejection edge cases | F2, F4, N1, N2, N3 |
| T3 | Backend edge-case tests | Additional tests for LLM output validation (E7-E15), injection flagging (E6), BLOCKED_INPUT (E19), REFUSED (E20), AUTH/FORBIDDEN (E21), partial dates (E16-E17), Unicode input (E18), and concurrency (E36) | F1, F3, F4 |
| T4 | Frontend AI search bar | The AI search input, submit action, loading state (with in-flight guard per E35), explanation card, filter chips, "Clear AI search" action, and degraded/error state display on the Guest search page | F5, F6, F7 |
| T5 | Frontend tests and close-out | Vitest tests for the AI search component, the API wrapper, and the interaction states (E31-E35); Checkstyle, backend build, frontend build all green | F5, F6, F7, N1 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 AiFeature bean | - | - | 1 |
| T2 Backend endpoint | T1 | - | 2 |
| T3 Backend edge-case tests | T2 | T4 | 3 |
| T4 Frontend AI search bar | T2 | T3 | 3 |
| T5 Frontend tests and close-out | T4 | - | 4 |

Shared-file hazards: T2 and T3 both edit test files in the guest controller test package — serialize them (T2 before T3). T4 and T5 both edit `frontend/src/app/guest/search/page.tsx` — serialize them (T4 before T5).
