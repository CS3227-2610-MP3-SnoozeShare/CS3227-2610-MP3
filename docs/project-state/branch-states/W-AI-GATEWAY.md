# Branch State — W-AI-GATEWAY

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AI-GATEWAY (from `development` at `4257d4b`)
- **Base:** `4257d4b` (development tip when the branch was cut)
- **Owner:** CongChen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-22 by Claude Sonnet 5.5 — plan drafted and reviewed

---

**Scope.** The shared LLM gateway that every role's AI feature (W-AI-GUEST, W-AI-HOST, W-AI-AGENT) calls: the NUS SoCLaaS client, global rate and concurrency control, guardrails, audit hook, mock mode and fallbacks. It contains no role feature. Serves the new W-AI-GATEWAY workstream, which must merge before the three role AI workstreams.

---

## 1. Orientation

No change.

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md`, no additional steps. The development SoCLaaS key is held by the owner and is never committed; it goes in the gitignored `application-local.yml` or an environment variable.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| S1 | 2026-10-22 | Claude Sonnet 5.5 | W~1 | Active | Executing inline, T1 done | T2 step 1 of `docs/superpowers/plans/2026-10-22-w-ai-gateway.md` (T2 to T5 may run concurrently) | 2026-10-22 |

**Remaining**

- [x] Gate 2 (plan approval) - done 2026-10-22
- [ ] Execute T1 to T7 (`executing-plans-lite`); T8 is Post-merge
- [ ] **Handover:** `GATE:` before the merge, confirm the Render dev service has `SNOOZESHARE_AI_ENABLED=false` (the prod guard fails startup without it)
- [ ] **Handover:** the W-AI-AGENT scoping decisions of 2026-10-22 belong in that branch's state: the report goes in a separate AI draft field; the assistant posts as the SYSTEM user with a new `AI` author role in the GUEST channel only; Guest-raised tickets only; it asks at submission, then on each guest reply up to a fixed round cap; it depends on this gateway
- [ ] **Handover:** `GATE:` T8 of the spec - set `SNOOZESHARE_AI_ENABLED` and the SoCLaaS variables in Render and verify reachability and the unconfirmed upstream behaviours (E27) before any W-AI-* spec starts

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W~1 | W-AI-GATEWAY: shared SoCLaaS LLM gateway | Building | [spec](docs/superpowers/specs/2026-10-22-w-ai-gateway-design.md) | [plan](docs/superpowers/plans/2026-10-22-w-ai-gateway.md) | Task 1/8 done; T2 to T5 next (wave 2) | — |

---

## 4. Architecture

### 4.11 AI gateway (backend `ai/`)

**Now (proposed):** the backend has one LLM entry point, `LlmGateway.run(AiFeature, LlmCall)` in `com.snoozeshare.ai`. It runs a fixed pipeline: shape and transaction checks, DISABLED, role gate, per-user rolling-minute and in-flight limits, sanitizing (NFKC, control and format characters, PII, delimiter tags) and prompt assembly with untrusted text wrapped in `<user_input>` and `<untrusted_data>`, per-user and global daily caps counted from `audit_log` plus in-flight reservations, a budget block and a circuit breaker, then up to `max-retries + 1` attempts through one global scheduler (`max-rpm`, `max-concurrent`, priority queue) against SoCLaaS `POST /chat/completions` over a shared `HttpClient` (https, no redirects), then output validation (stop reason, refusal marker, key and link checks, schema or length) and one audit row (`AI_CALL` or `AI_CALL_REJECTED`, metadata only). Every failure returns the feature's fallback with a code; `run` blocks the caller thread for at most `deadline-ms` and refuses to run inside a transaction. Role workstreams register `AiFeature` beans and own their prompts, output types and endpoints. Limiter, breaker and budget state are process-local (C69). With `snoozeshare.ai.enabled` false, or with the upstream unreachable, the backend and its health endpoint are unaffected.

| Path | Role |
|---|---|
| `backend/src/main/java/com/snoozeshare/ai/` | `LlmGateway`, `AiFeature`, `OutputSpec`, `LlmCall`, `LlmResult`, `LlmErrorCode` |
| `backend/src/main/java/com/snoozeshare/ai/{config,guard,limit,client,audit}/` | properties and startup check; sanitizing and validation; scheduler, throttle, breaker, budget block; the SoCLaaS client; the audit writer |
| `backend/src/main/java/com/snoozeshare/domain/enums/AuditAction.java` | gains `AI_CALL` and `AI_CALL_REJECTED` |
| `application*.yml`, `ProdEnvironmentGuard`, `render.yaml` | the `snoozeshare.ai.*` keys and the production guard rules |

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~3 | 2026-10-22 | Generic gateway; each role feature registers its own prompt, output type and caps | Operator chose it over a gateway that owns every feature's prompt, which would block on Host's undecided feature | spec SD1 |
| C~4 | 2026-10-22 | Audit stores metadata only, not prompt or response text. Departs from `Webapp-architecture-design § 5.3` | Operator chose it over truncated sanitized text, to avoid copying personal data into `audit_log` | spec SD2 |
| C~5 | 2026-10-22 | Three departures from `Webapp-architecture-design §§ 3, 5.3` (with C~4): a custom scheduler and breaker replace Bucket4j, and the module is `com.snoozeshare.ai` instead of `service/ai` | The shared-key budget needs a global queue with priorities, which a per-user rate limiter does not give; no new dependency | spec SD4, SD11, SD12 |
| C~6 | 2026-10-22 | Daily caps count `AI_CALL` audit rows plus in-flight reservations; fail-closed audit for ok results; no gateway call inside a transaction | Caps survive restarts and bursts; unaudited output must not reach a user; a 45 s call must not hold a connection | spec SD5, SD6, SD7, SD11 |
| C~7 | 2026-10-22 | Output with links, HTML or Markdown images (also Unicode-escaped, after NFKC) is rejected by default; priority 0 keeps a reserved share of the global daily cap; base URL must be https and redirects are never followed; a budget 429 blocks for 15 minutes then re-probes | Reviewer threats: exfiltration through rendered links, key sent to another host, a forged "quota" phrase causing a day-long outage | spec SD13, SD15, SD16 |

---

## 5. Conventions

Entries added (new in this branch):

- Role code calls `LlmGateway.run` outside any transaction, supplies `userId` and `role` from its authorized principal, builds ASSISTANT turns only from server-stored history, puts only server-built text in `trustedContext`, acts on `flagged`, and renders AI output as plain text through a response DTO.
- Nothing outside `com.snoozeshare.ai` imports `ai.config`, `ai.guard`, `ai.limit`, `ai.client` or `ai.audit`, and nothing outside `ai/client` calls SoCLaaS; a JUnit source-scan test enforces it.
- Daily boundaries for AI caps use UTC; waiting and randomness in `ai.limit` go through `Sleeper` and `Jitter` so tests need no real sleep.
- Logs for AI failures carry feature id, user id, code, status and latency only, and exceptions are logged by class name; the JDK HTTP client debug logger is never enabled.
- Gateway beans that need the database carry `@ConditionalOnBean(PropertyRepository.class)`, like the W-DOMAIN services, so datasource-free contexts start without them.
- `application-prod.yml` has no default for `snoozeshare.ai.enabled`; `render.yaml` sets `SNOOZESHARE_AI_ENABLED` to `"false"` until AI is switched on.

---

## 6. Known Gaps

Entries added:

- **AI limiter, breaker and budget-block state is process-local and lost on restart** (C69); a Render redeploy briefly runs two instances on one shared key. "Hard ceiling" means per process.
- **A coordinated group of accounts can still open the AI circuit breaker** (it needs failures from 3 distinct users); GUEST registration needs no code (C89). Bounded by the per-user and global daily caps.
- **Steered prose that matches no leak or link pattern passes output validation**; role features keep AI output a suggestion the user confirms (C80).
- **A daily cap counts calls, not upstream requests**: worst case is `global-daily-cap x (max-retries + 1)` requests a day.
- **Many accounts can still exhaust the non-reserved 80 percent of the global AI cap and the queue** (guest registration needs no code, C87); priority-0 features keep a reserved share and displace lower-priority jobs.
- **While audit writes fail, sent calls are counted in memory only until 00:00 UTC**, so a restart loses that floor.
- **Bare-domain links (no scheme) in model output are not detected**; role UIs render AI output as plain text.
- **Until T8 records the live results, `response_format` and `reasoning_effort` stay off and the SoCLaaS reachability from Render is unverified.**

---

## 7. Needs a Human

No change. Resolves, on fold, the SoCLaaS-contract half of `PROJECT_STATE.md` Q6 (the contract is the SoC DocHub guide as summarised in the owner's integration guide, with the unconfirmed items checked in T8). The Host AI feature is still undecided; the Agent feature is decided (W-AI-AGENT).

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-22 | The LLM gateway is its own workstream, W-AI-GATEWAY, merged before W-AI-GUEST, W-AI-HOST and W-AI-AGENT | Operator: lets the three role AI features run concurrently on a shared base. Rejected: the first role feature builds the gateway; every role builds its own client | Operator conversation |
| C~2 | 2026-10-22 | Development and production use separate SoCLaaS keys, each supplied by the deployment environment and never committed. The owner already holds the development key | Operator | Operator conversation |
| C~3 to C~7 | 2026-10-22 | See § 4 (4.11 AI gateway) | Operator and review rounds | spec |
| C~8 | 2026-10-22 | Spec risk tier is High with Implementer, Tester, Maintainer and Attacker lenses | Operator chose High over Medium | spec SD3 |

---

## 9. Deviations

No change.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-22 | Branch cut and branch state created | W~1 | — |
| 2026-10-22 | Spec drafted (index plus six components, High tier); both review rounds merged | W~1 | — |
| 2026-10-22 | Spec approved by Xiao-Congchen (gate 1) | W~1 | — |
| 2026-10-22 | Plan written (8 tasks plus close-out) and reviewed; test infrastructure fixes merged | W~1 | — |
| 2026-10-22 | Plan approved by Xiao-Congchen (gate 2); workstream Planned | W~1 | — |
| 2026-10-22 | T1 done: `ai` core types, validated `LlmProperties`, `AiFeatureRegistry`, prod guard rules, `SNOOZESHARE_AI_ENABLED` in yml and `render.yaml` | W~1 | — |
