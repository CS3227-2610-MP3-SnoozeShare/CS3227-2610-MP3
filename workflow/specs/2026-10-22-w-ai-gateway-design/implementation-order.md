# Implementation Order

Each task ships with its own tests; the last column names the edge cases it must cover. A task keeps the build green (`AGENTS.md` § 6).

| ID | Task | Delivers | Satisfies | Edge cases tested |
|---|---|---|---|---|
| T1 | Core types and configuration | The `ai` types, validated `LlmProperties`, the `snoozeshare.ai.*` keys in `application*.yml`, the `ProdEnvironmentGuard` additions, `SNOOZESHARE_AI_ENABLED` in `render.yaml`, and `--SNOOZESHARE_AI_ENABLED=false` in `ProdProfileTest`'s argument lists so the existing prod tests stay green | F13, N1 | E10, E11, E34, E35 (properties and guard parts) |
| T2 | Audit actions and writer | `AuditAction.AI_CALL` and `AI_CALL_REJECTED`; `AiAuditWriter` with the row layout, the per-user and global UTC count queries, reservations and R-row coalescing | F7, F11, N6 | E13 (counts, reservations, parallel burst), E30 (writer failures), E31 (R-row coalescing), E47, E56, E59 |
| T3 | Guardrails | `InputSanitizer`, `PiiRedactor`, `InjectionDetector`, `PromptBuilder`, `OutputValidator` as pure classes | F3, F4, F5, F10 | E1 to E8, E24 (validator rows), E25, E26, E42, E48, E52, E58 |
| T4 | Limits and resilience | `SchedulerCore`, `LlmScheduler`, `UserThrottle`, `CircuitBreaker`, `BudgetBlock`, `Backoff`, `Sleeper`, `Jitter` | F6, F7, F9, N3 | E12, E14 to E17 (codes only), E19 and E20 (state logic), E53 (throttle logic), E55, E57 (state logic) |
| T5 | SoCLaaS client | `SoclaasClient`: one attempt, request body, response parsing and size cap, status classification, per-attempt timeout, no redirects | F8, F9, N1 | E21, E23, E37, E46, E60 and the single-attempt parts of E22 |
| T6 | Gateway facade | `LlmGateway.run` composing T2 to T5 in the api.md pipeline: transaction check, role gate, caps, retry loop, deadline, mock and disabled modes, feature registry, startup model check, bean conditions | F1, F2, F8, F9, F10, F11, F12, F14, N2, N4 | E9, E18, E19, E20, E22, E28, E29, E30, E31, E32, E33, E36, E38, E41, E43, E44, E49 to E51, E53, E54, E57; the R-row assertions of E15, E16 and E55 |
| T7 | Integration tests, boundary test and docs | End-to-end tests against an in-process stub server, the datasource-free and health tests, the package-boundary source-scan test, the secret-scan test, the runbook section for the new variables | N1 to N6 | E39, E40, E45 (role specs), E35 end to end |
| T8 | Live verification (`GATE:` Post-merge) | The owner sets `SNOOZESHARE_AI_ENABLED` and the key variables in the Render dashboard, runs the checks in E27 from the dev machine and from the Render dev service, and records the observed values; any default that changes is a follow-up commit pinned by the T1 configuration test | E27 | E27 |

W-AI-GUEST, W-AI-HOST and W-AI-AGENT specs do not start until T8's observed values are recorded in the branch state or the runbook.

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Core types and configuration | - | - | 1 |
| T2 Audit actions and writer | T1 | T3, T4, T5 | 2 |
| T3 Guardrails | T1 | T2, T4, T5 | 2 |
| T4 Limits and resilience | T1 | T2, T3, T5 | 2 |
| T5 SoCLaaS client | T1 | T2, T3, T4 | 2 |
| T6 Gateway facade | T2, T3, T4, T5 | - | 3 |
| T7 Integration tests, boundary test and docs | T6 | - | 4 |
| T8 Live verification | T7 | - | 5 |

Shared-file hazards: none between T2 to T5, each of which owns a package (T2 also edits `AuditAction.java` and `AuditLogRepository.java`, which no other task touches). T1 edits `ProdEnvironmentGuard.java`, `ProdProfileTest.java`, `render.yaml` and the three `application*.yml` files; T7 edits new tests and `docs/runbooks/dev-environment.md`. Merge `development` before T2 if W-AGENT or a W-DOMAIN follow-up has edited `AuditAction.java` or `AuditLogRepository.java`.
