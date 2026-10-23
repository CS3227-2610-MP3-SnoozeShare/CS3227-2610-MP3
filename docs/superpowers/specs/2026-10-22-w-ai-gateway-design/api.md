# API

The gateway has no HTTP endpoint. Its callers are backend services in `com.snoozeshare`, which are called by role controllers under `/api/guest/**`, `/api/host/**` and `/api/agent/**`. All types live in `com.snoozeshare.ai` (layout in [conventions](conventions.md)). `Role` is `com.snoozeshare.domain.enums.Role`.

## LlmGateway.run(AiFeature<T> feature, LlmCall call) returns LlmResult<T>

| Field | Definition |
|---|---|
| Caller and principal | A service method running for an authenticated user. `call.userId()` is the `users.id` of that user and `call.role()` their role, both taken from the same source the role controller used to authorize the request (`RoleAuthorizationService`), never from a request body (C79). The gateway does not read the `SecurityContext` and does not re-check `users.role` (SD18) |
| Authorization rule | `call.role()` must be in `feature.allowedRoles()` (pipeline step 4) |
| Tenancy scope | The gateway reads no tenant data and sends only what the caller put in `LlmCall`; the caller scopes that context to what the user may see. Its only database reads are counts of `AI_CALL` rows |
| Inputs | `LlmCall(UUID userId, Role role, List<Turn> turns, String trustedContext, String untrustedContext, AuditContext audit)`. `Turn(TurnRole role, String text)`, `TurnRole` is USER or ASSISTANT. `turns` non-empty, last turn USER, no null element or text. ASSISTANT turns must come from server-stored history, never from client input. `trustedContext` holds only server-built text, never user-authored text. `AuditContext(UUID ticketId, UUID bookingId, UUID subjectUserId)` fields may be null. Oversized text is truncated, not rejected (step 7) |
| Outputs | `LlmResult<T>(boolean ok, T value, LlmErrorCode code, boolean flagged, boolean truncated, UUID callId)`. Ok: validated output, `code` null. Otherwise `value` is `feature.fallback(call)` and `code` is set. `callId` is a fresh random UUID on every call, also when no row is written. Never returned: key, upstream body, prompt text. Role controllers map `value` to a response DTO (C73) and the UI renders it as plain text |
| Errors | Expected failures return `ok=false` with a code. `IllegalArgumentException` for a malformed `LlmCall` (step 1); `IllegalStateException` when a database transaction is active (step 2). A `fallback` that throws propagates (feature bug) |
| Idempotency | None. Each call is a new `callId` and at most one audit row; callers must not loop on a degraded result |
| Transaction boundary | Runs outside any transaction, detected by `TransactionSynchronizationManager.isActualTransactionActive()`. Tests of `run` are not `@Transactional`. The audit write is its own transaction |
| Threading | `run` blocks the calling thread for at most `deadline-ms` plus 1,000 ms plus the time of its database calls (steps 8 and 14, each bounded by the datasource connection timeout of 5 s). The HTTP attempt runs on the calling thread through one shared `HttpClient`; no worker pool. At most `max-concurrent + max-queue` threads (24 by default) wait on the scheduler, against Tomcat's default pool of 200 |
| Events / audit | At most one audit row (data-model.md); no application event |
| Logging | One WARN line per non-ok outcome: feature id, user id, code, HTTP status, latency. One ERROR line for AUTH. For an exception log only its class name, never its message or stack (Jackson and HTTP messages quote content). Never the key, headers, prompt, response or upstream body (N1) |
| Compatibility | New module, no existing caller. `AuditAction` gains two constants |

## Pipeline (the total order of checks; the first that fails decides the code)

| Step | Check or action | Failure |
|---|---|---|
| 0 | Create `callId`; record the entry instant, the origin of `deadline-ms` | - |
| 1 | Shape: null `feature`, `userId`, `role`, `turns`, a null element or text, empty `turns`, last turn not USER | `IllegalArgumentException` |
| 2 | A transaction is active | `IllegalStateException` |
| 3 | `enabled` is false | DISABLED |
| 4 | `role` not in `allowedRoles` | ROLE_DENIED |
| 5 | Per-user per-feature minute window: when under the limit reserve a slot, which stays used even if a later step refuses; a call refused here consumes none | USER_THROTTLED |
| 6 | Per-user in-flight count below `per-user-max-in-flight`; take a slot, release it when `run` returns | USER_THROTTLED |
| 7 | Sanitize and assemble the prompt (conventions.md), set `flagged`, `truncated`, `promptHash`. Blank ASSISTANT turns and blank earlier USER turns are dropped; after keeping the last `maxTurns` turns a leading ASSISTANT turn is dropped; a blank `untrustedContext` is omitted. The last USER turn blank after sanitizing | BLOCKED_INPUT (R) |
| 8 | Daily caps, as one step under a single per-process lock held across the reads and the reservation: read the reserved counts first, then count rows; (user's `AI_CALL` rows since 00:00 UTC + user's reserved calls) must be below `daily-cap-per-user`, and (all users' rows + all reserved) below `global-daily-cap` for priority 0 or below `global-daily-cap` x (100 - `priority-zero-reserve-percent`) / 100 for other priorities; on pass reserve one. If a count query fails, no request is sent and the code is AUDIT_FAILED | DAILY_CAP (R) |
| 9 | `mock` is true: skip steps 10 to 12 and use `mockResponse` as the model text with 0 tokens and `attempts` 0 | - |
| 10 | Budget block active (steps 10 and 11 are checked again at the start of every attempt, retries included) | BUDGET_EXHAUSTED (R) |
| 11 | Circuit open (a half-open breaker lets exactly one probe call through) | CIRCUIT_OPEN (R) |
| 12 | Attempt loop below | per table |
| 13 | Validate the model text (conventions.md) | INVALID_OUTPUT, REFUSED |
| 14 | Write the audit row, then release the reservation under the step 8 lock; if the row of a sent call cannot be written the reservation is not released but kept as an in-memory count until 00:00 UTC | AUDIT_FAILED |

Attempt loop. Up to `max-retries + 1` attempts. Each attempt takes a scheduler permit (priority from the feature, arrival time of the first attempt kept for retries), waits at most the smaller of `queue-wait-ms` and the time left to the deadline, sends with a timeout of the smaller of `request-timeout-ms` and the time left, then classifies the response. After a retryable failure the call sleeps `min(8,000, 500 x 2^n)` ms times a jitter factor in [0.5, 1.0] (n is 0 before the first retry; the `Jitter` seam is clamped to that range, so a test may fix it at 1.0), or the `Retry-After` value in whole seconds when present (an HTTP-date is ignored), capped at 10 seconds; if the time left is shorter than the sleep plus 1,000 ms the loop ends. Once any request has been sent, a later refusal (QUEUE_FULL, QUEUE_TIMEOUT, CIRCUIT_OPEN, deadline) ends the loop and the result code is the last attempt's failure code; before any request was sent, deadline expiry while queued is QUEUE_TIMEOUT. A thread interrupt ends the loop the same way and restores the interrupt flag. A call that carries the breaker probe or the budget probe gets one attempt and no retries; while a probe is in flight other calls get CIRCUIT_OPEN or BUDGET_EXHAUSTED, and one call may carry both probes. Scheduler queue: jobs of priority 0 run first; when the queue is full and the new job has priority 0, the youngest waiting job of priority 1 or 2 is removed (its call gets QUEUE_FULL, R) and the new job queues. Priority 1 and 2 behave alike: they may use at most `max-concurrent - 1` slots, priority 0 all of them. Priority 0 is for staff-facing features; a role workstream that sets it for a public feature removes the protection. Audit row: `AI_CALL` when at least one request was sent or in mock mode, otherwise `AI_CALL_REJECTED`.

## Error codes

Audit column: A = `AI_CALL`, R = `AI_CALL_REJECTED`, L = logged only, no row.

| Code | When | Retried | Audit |
|---|---|---|---|
| DISABLED | step 3 | no | L |
| ROLE_DENIED | step 4 | no | L |
| USER_THROTTLED | step 5 or 6 | no | L |
| BLOCKED_INPUT | step 7 | no | R |
| DAILY_CAP | step 8 | no | R |
| BUDGET_EXHAUSTED | step 10 (R), or the attempt whose 429 matched the budget rule (A) | no | R / A |
| CIRCUIT_OPEN | step 11 | no | R |
| QUEUE_FULL | `max-queue` jobs waiting | no | R |
| QUEUE_TIMEOUT | waited longer than `queue-wait-ms`, or deadline hit while queued with nothing sent | no | R |
| RATE_LIMITED | HTTP 429 not matching the budget rule | yes | A |
| SERVER | HTTP 500 to 599 | yes | A |
| TIMEOUT | an attempt exceeded its timeout, or the deadline ended an in-flight attempt | yes | A |
| NETWORK | connection error | yes | A |
| BAD_REQUEST | HTTP 400, 413, any other 4xx except 401, 403 and 429, any 3xx (redirects are never followed) | no | A |
| AUTH | HTTP 401 | no | A |
| FORBIDDEN | HTTP 403 | no | A |
| INVALID_OUTPUT | output validation failed, a 2xx status other than 200, an unusable 200 body, or a body over 262,144 bytes | no | A |
| REFUSED | the model returned the refusal marker | no | A |
| AUDIT_FAILED | an `ok` result whose audit row could not be written (SD6), or a failed cap count at step 8. If an R row cannot be written the code is unchanged and one WARN line is logged | no | L |

R rows are limited to one per user per code per 10 minutes (further ones are counted and logged only). Budget rule for an upstream 429: parse the body as JSON; take `error` if it is a string, or `error.message` if it is an object; use only its first 200 characters (for classification only the first 4,096 bytes of a 429 body are read). Budget if it matches `\b(quota|budget|credits?)\b` (case-insensitive) and does not match `\brate[ -]?limit`; any other body, including non-JSON or empty, is RATE_LIMITED. On a budget 429 all calls get BUDGET_EXHAUSTED for `budget-block-ms` (capped at the next 00:00 UTC); after that window the next call is sent as a probe and a repeated budget 429 extends the block again. Breaker: a failed attempt is one with code SERVER, TIMEOUT or NETWORK; any attempt that receives an HTTP response below 500 resets the counter. The breaker keeps one streak: the failed-attempt count and the set of user ids that caused them; the streak resets on any response below 500. It opens for `breaker-open-ms` when the count reaches `breaker-failures` and the set holds at least 3 users, so 5 failures from 2 users keep counting and it opens at once when a third user fails; then one probe call runs: success closes it, failure re-opens it for another window.

## Configuration (Spring properties; environment variable in brackets)

| Key | Default | Rule |
|---|---|---|
| `snoozeshare.ai.enabled` [`SNOOZESHARE_AI_ENABLED`] | false in dev; no default in prod | boolean |
| `snoozeshare.ai.mock` [`SNOOZESHARE_AI_MOCK`] | false | the `prod` profile rejects `true` whatever `enabled` is; elsewhere ignored when `enabled` is false |
| `snoozeshare.ai.base-url` [`SOCLAAS_BASE_URL`] | empty | when enabled and not mock: `https://` and ending `/v1` with no trailing slash; `http://` allowed only for `localhost`, `127.0.0.1` or `[::1]` outside prod |
| `snoozeshare.ai.api-key` [`SOCLAAS_API_KEY`] | empty | non-blank when enabled and not mock; a value that still contains `${` counts as blank; never in `toString` |
| `snoozeshare.ai.model-fast` / `-standard` / `-strong` [`SNOOZESHARE_AI_MODEL_FAST` / `_STANDARD` / `_STRONG`] | `llama3.1:8b` / `qwen3.5:9b` / `gemma4:26b` | 1 to 100 characters |
| `snoozeshare.ai.max-rpm` / `max-concurrent` / `max-queue` | 25 / 4 / 20 | 1 to 90 / 2 to 16 / 1 to 200 |
| `snoozeshare.ai.queue-wait-ms` / `request-timeout-ms` / `deadline-ms` | 10,000 / 30,000 / 45,000 | 100 to 60,000 / 1,000 to 120,000 / at least `request-timeout-ms` |
| `snoozeshare.ai.max-retries` | 2 | 0 to 5 |
| `snoozeshare.ai.breaker-failures` / `breaker-open-ms` | 5 / 30,000 | 3 to 50 / 1,000 to 600,000 |
| `snoozeshare.ai.budget-block-ms` | 900,000 | 60,000 to 86,400,000 |
| `snoozeshare.ai.per-user-max-in-flight` | 2 | 1 to 8 |
| `snoozeshare.ai.daily-cap-per-user` / `global-daily-cap` | 200 / 3,000 | 1 to 10,000 / 1 to 100,000 |
| `snoozeshare.ai.priority-zero-reserve-percent` | 5 | 0 to 50; the share of `global-daily-cap` only priority 0 calls may use |
| `snoozeshare.ai.max-prompt-chars` | 40,000 | 2,000 to 200,000 |
| `snoozeshare.ai.response-format-json` | false | boolean; sends `response_format` `json_object` for JSON features when true |
| `snoozeshare.ai.reasoning-effort` | `none` | empty (field omitted), `none`, `low`, `medium` or `high`; T8 found `none` accepted by all three models and `low` rejected by `llama3.1:8b` |

`response-format-json` stays off; `reasoning-effort` defaults to `none` after T8 (SD10, amended). `max-rpm` defaults to 25 because the development key allows 30 requests a minute. Development and production use separate SoCLaaS keys, each supplied only by the environment. An invalid value fails startup in every profile, naming the key. The `prod` profile has no placeholder default for `enabled`; `base-url` and `api-key` default to empty in `application-prod.yml` because they are required only when enabled, and `ProdEnvironmentGuard` fails startup naming `SNOOZESHARE_AI_ENABLED` when it is unset, naming `snoozeshare.ai.base-url` or `snoozeshare.ai.api-key` when enabled, not mock and blank, and rejects `mock=true` in every case.

## AiFeature<T> (a Spring bean per feature, registered by the role workstream)

| Field | Definition |
|---|---|
| `id` | `^[a-z]+\.[a-zA-Z]+$`, at most 40 characters, unique across beans |
| `allowedRoles` | Non-empty set of GUEST, HOST, AGENT |
| `tier` | `FAST`, `STANDARD` or `STRONG`, resolved to a configured model |
| `systemPrompt` | Trusted text, 1 to 6,000 characters |
| `maxTokens` / `temperature` / `priority` | 1 to 4,000 / 0.0 to 1.0 / 0 (most urgent) to 2 |
| `perUserPerMinute` | 1 to 60 per user and feature, rolling window of 60 s that includes a start at t and excludes t minus 60 s |
| `maxInputChars` / `maxTurns` | 1 to 8,000, default 2,000 / 1 to 30, default 10; `maxInputChars x (maxTurns + 1) + 15,000` must not exceed `max-prompt-chars` |
| `output` | `OutputSpec.text(maxOutputChars, allowLinks)` with 1 to 4,000 characters, or `OutputSpec.json(Class<T>, allowLinks)` where the class is a record validated with Jakarta Bean Validation; `allowLinks` defaults to false |
| `fallback` | `Function<LlmCall, T>`; by convention it never calls the gateway or the database |
| `mockResponse` | Raw model text used in mock mode and validated like a real reply |

A bean that breaks a rule, or a duplicate id, fails startup naming the feature id and the field.

## Startup model check (internal operation)

Runs once on `ApplicationReadyEvent` when enabled and not mock, on an injectable `Executor`. `GET {base-url}/models` takes a scheduler permit at priority 2, reads `data[].id`, and logs one WARN naming each configured model that is absent, or one WARN with the status on failure. It never fails or delays startup.
