# W-AI-GATEWAY Design

- **Status:** Approved (Xiao-Congchen, 2026-10-22)
- **Amended 2026-10-22:** see branch state C~9 (default staff reserve 5 percent)
- **Risk tier:** High - untrusted user text and personal data cross to an external LLM service, and the gateway carries the shared API key and the shared request budget.
- **Workstream:** W-AI-GATEWAY | **Plan:** [W-AI-GATEWAY plan](../plans/2026-10-22-w-ai-gateway.md)
- **Constraints consulted:** C69 (single backend instance), C73 (DTO layer at the API boundary), C79 (role comes from the server, never the client), C80 (SOC LLM only, outputs are suggestions, key stays on the backend, inputs sanitized, outputs validated, requests rate-limited and capped per user, every AI interaction audited), C67 (High for personal data), C95 and C96 (production fails startup on missing settings; safe correlated failures), Known Gaps "Rate limiting is process-local", "No multi-instance deployment" and "The dev backend sleeps when idle"; Conventions "The `prod` profile has no defaults", "The health body stays exactly `{"status":"UP"}`", "credentials, tokens and registration codes are never logged"; the merged W-DOMAIN branch state (JPA entities, `AuditService`, `AuditLogRepository`, V005 is the last migration). Design source: [Webapp-architecture-design § 5](../../Webapp-architecture-design.md), departed from in three places recorded in the branch state (metadata-only audit, a custom scheduler instead of Bucket4j, package `com.snoozeshare.ai` instead of `service/ai`).
- **Reviewed by lenses:** Implementer, Tester, Maintainer, Attacker (untrusted text to an LLM, secret key, shared budget) - both review rounds done and merged (the two-round cap is used)

## 1. Problem

Three roles will each call the NUS SoCLaaS LLM (C80). Today the backend has no LLM code: no HTTP client for SoCLaaS, no limiter for the shared key, no prompt-injection guard and no AI audit action (`AuditAction` has none). SoCLaaS is an OpenAI-compatible gateway; the key is the unit of limiting (default 90 requests/minute, daily and monthly spend caps) and every caller shares it. If each role builds its own client, the three features exceed the limit together and guard input and output differently.

Desired: one backend module, `com.snoozeshare.ai`, that every role feature calls through one method. It owns the HTTP client, global rate and concurrency control, retries, the circuit breaker, input and output guardrails, per-user and global caps, audit, mock mode and the fallback result. Role workstreams own only their prompt, output type, caps and endpoint. The module adds no REST endpoint, no table and no migration.

The upstream contract comes from the SoC DocHub SoCLaaS guide as summarised in the owner's integration guide of 2026-10-21; it resolves the contract half of Q6. Items the guide marks unconfirmed are edge case E27, verified in T8.

## 2. Goals

Fourteen functional and six non-functional requirements, with acceptance criteria, are in [requirements](2026-10-22-w-ai-gateway-design/requirements.md).

**Functional:** F1 typed call API with a fallback result; F2 role gate; F3 sanitizing in a fixed order; F4 injection flag; F5 prompt layout; F6 global rate and concurrency control; F7 per-user and global caps; F8 upstream request; F9 failure handling; F10 output validation; F11 audit; F12 disabled and mock modes; F13 configuration and startup guard; F14 fixed check order.

**Non-functional:** N1 key and content never logged; N2 bounded call time; N3 request ceiling per process; N4 backend healthy without the LLM; N5 no live calls in CI and no committed key; N6 no prompt or response text in the audit log.

## 3. Non-goals

- Streaming (SSE) responses - no planned feature needs them; the Agent chat is one request per guest message.
- Response caching and in-flight de-duplication - added when W-AI-GUEST needs it; the call interface does not change when it is added.
- `/v1/embeddings`, `/v1/responses`, audio and tool calling - only `/v1/chat/completions` and a one-time `GET /v1/models` are used.
- Reading the SoCLaaS portal budget endpoint (`/api-key/budget`) - its response shape is unconfirmed; budget exhaustion is detected from a 429 body and re-probed every `budget-block-ms`.
- Any role feature: prompts, output types, REST endpoints, frontend, the AI draft field and the `AI` author role (W-AI-GUEST, W-AI-HOST, W-AI-AGENT).
- Shared limiter state across instances (Redis) and surviving a restart - single instance only (C69, Known Gap "Rate limiting is process-local").
- An ops statistics endpoint or page - counters go to logs only; the health body stays exactly `{"status":"UP"}`.
- A "repair" retry when the model returns bad JSON - it costs a request; the fallback is returned instead.
- A content-safety classifier beyond the patterns in [conventions](2026-10-22-w-ai-gateway-design/conventions.md); steered prose that matches no pattern is the role code's concern (outputs stay suggestions, C80).
- Re-checking `users.role` inside the gateway - the role controller already authorized the call; a database read per call is not worth the defence in depth (SD18).

## 4. Components

- [Requirements](2026-10-22-w-ai-gateway-design/requirements.md) - F1-F14 and N1-N6 with acceptance criteria.
- [API](2026-10-22-w-ai-gateway-design/api.md) - the Java call contract, the numbered pipeline, error codes, configuration, feature registration.
- [Data model](2026-10-22-w-ai-gateway-design/data-model.md) - no new table; two audit actions, the audit row layout, runtime state.
- [Conventions](2026-10-22-w-ai-gateway-design/conventions.md) - packages, bean and test rules, prompt layout, sanitizing and output-validation order.
- [Edge cases](2026-10-22-w-ai-gateway-design/edge-cases.md) - E1-E60 including the threat list and "what could corrupt data".
- [Implementation order](2026-10-22-w-ai-gateway-design/implementation-order.md) - T1-T8 with dependencies, waves and test ownership.
- No UI changes: the gateway has no screen.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Generic gateway; each role registers an `AiFeature` bean with its own prompt, output type and caps | Rejected: the gateway owning every feature's prompt (blocks on Host's undecided feature and three owners) | Operator, 2026-10-22 |
| SD2 | Audit stores metadata only (feature, model, tokens, latency, outcome, prompt hash); no prompt or response text | Rejected: truncated sanitized text (copies personal data into `audit_log`; ticket messages already hold the conversation) | Operator, 2026-10-22 |
| SD3 | Risk tier High; lenses Implementer, Tester, Maintainer, Attacker | Personal data and untrusted text to an external service (C67). Rejected: Medium (fewer lenses, thinner security detail) | Operator, 2026-10-22 |
| SD4 | HTTP via `java.net.http.HttpClient` and Jackson; no new dependency | Rejected: a vendor SDK (the API is one POST) | Spec author |
| SD5 | Two audit actions: `AI_CALL` (a request was sent, or mock) and `AI_CALL_REJECTED` (refused before sending). Daily caps count `AI_CALL` rows in the database plus in-flight reservations | Survives restarts and parallel bursts. Rejected: in-memory daily counters (reset on every restart) | Spec author |
| SD6 | Audit is fail-closed for ok results: if the row cannot be written the result is AUDIT_FAILED with the fallback | C80: every AI interaction is audited; the LLM is optional so the cost is a fallback | Spec author |
| SD7 | `run` refuses to run inside an active transaction (`isActualTransactionActive`) | A 45 s external call would hold a connection and a booking `SERIALIZABLE` transaction | Spec author |
| SD8 | Production may run with AI disabled; `SNOOZESHARE_AI_ENABLED` must be set explicitly; when true the base URL and key are required, mock is rejected | Reachability from Render is unverified; a missing setting still fails startup (C95) | Spec author |
| SD9 | Defaults 55 requests/minute, 4 in flight, queue 20 and wait 10 s, 2 in flight per user, 200 calls per user and 3,000 in total per UTC day | Headroom under the shared 90 limit; a small queue bounds blocked servlet threads on a free Render instance | Spec author |
| SD10 | `response_format` and `reasoning_effort` are off by default and enabled by configuration after T8 confirms the models accept them. Supersedes the first draft, which defaulted `reasoning_effort` to `none` | A rejected optional field would degrade every call from day one | Tester review |
| SD11 | A fixed numbered pipeline decides the code; caps and the per-minute window reserve atomically and release on every path | Parallel bursts must not overshoot; outcomes must be testable | Implementer, Tester, Attacker reviews |
| SD12 | `run` blocks the caller thread; the attempt runs on it; a pure `SchedulerCore` plus a blocking wrapper; `Sleeper` and `Jitter` seams; the deadline runs from step 0 and includes queue wait and backoff | Deterministic tests with a fake clock; no worker pool to manage | Implementer, Tester, Maintainer reviews |
| SD13 | A budget 429 blocks calls for `budget-block-ms` (15 minutes, capped at 00:00 UTC), then one probe re-tests it. Rejected: blocking until 00:00 UTC | A forged phrase echoed in an error then costs at most one window, re-armed at most once per window; a monthly cap costs one probe per window | Attacker review |
| SD14 | The breaker needs failures from at least 3 distinct users and half-opens with a single probe | One user cannot open it for everyone. A coordinated group of accounts remains a recorded residual risk, as does a group of accounts exhausting the non-reserved share of the global cap (SD21) | Attacker review |
| SD15 | Model output with links, HTML or Markdown images is rejected unless the feature sets `allowLinks`; role UIs render AI output as plain text | Indirect injection can try to exfiltrate context through a rendered link | Attacker review |
| SD16 | The base URL must be https (loopback http only outside prod) and redirects are never followed | The Bearer key must not leave for another host | Attacker review |
| SD17 | R rows are limited to one per user per code per 10 minutes | A refused flood must not flood `audit_log` | Attacker review |
| SD18 | The gateway trusts the caller for `userId` and `role`; it neither reads the `SecurityContext` nor re-checks `users.role`. Rejected: a role cross-check query per call | The role controller already authorized the call; each role spec tests it (E45) | Spec author |
| SD19 | The package-boundary rule is enforced by a JUnit source-scan test. Rejected: ArchUnit (a new dependency) | Keeps SD4 | Maintainer, Implementer reviews |
| SD20 | Display names written by `AuditService` into `actor_name` and `subject_name` are accepted; they are stored for every audit row | Bypassing `AuditService` would split the audit path | Tester review |
| SD21 | Priority 0 (staff-facing features) keeps a reserved 5 percent (the default; 0 to 50) of the global daily cap, may use every concurrency slot, and displaces the youngest waiting lower-priority job when the queue is full | Guest registration needs no code (C87), so many accounts could otherwise exhaust the pool and starve Host and Agent features. Residual: enough accounts still exhaust the non-reserved share | Attacker review |
| SD22 | Daily caps fail closed: a failed cap count refuses the call (AUDIT_FAILED), and a sent call whose audit row failed still counts until 00:00 UTC | A broken audit store must not lift the spend caps | Maintainer review |
