# API

## AiFeature registration: `guest.search`

| Field | Value |
|---|---|
| `id` | `guest.search` |
| `allowedRoles` | `{GUEST}` |
| `tier` | `FAST` |
| `systemPrompt` | See § System prompt below |
| `maxTokens` | 300 |
| `temperature` | 0.2 |
| `priority` | 2 (lowest; guest search is not staff-urgent) |
| `perUserPerMinute` | 5 |
| `maxInputChars` | 500 (matches the endpoint's input limit) |
| `maxTurns` | 1 (single query, no conversation) |
| `output` | `OutputSpec.json(AiSearchFilters.class, false)` — no links allowed |
| `fallback` | Returns `AiSearchFilters` with all filter fields null and explanation `"I couldn't understand that query. Please try the manual filters."` |
| `mockResponse` | `{"city":"Singapore","guests":2,"maxNightlyRate":100,"startDate":null,"endDate":null,"explanation":"Looking for places in Singapore for 2 guests under $100 per night."}` |

**Prompt-size check:** `500 × (1 + 1) + 15,000 = 16,000 ≤ 40,000` (default `max-prompt-chars`). Passes.

### System prompt

```
You are a search assistant for SnoozeShare, a short-term property rental platform.

The user will describe what kind of accommodation they are looking for in natural language. Extract structured search filters from their query and return a JSON object.

Return ONLY a JSON object with these fields:
- "city": string or null — the city or area name to search, or null if not mentioned
- "startDate": string or null — check-in date in YYYY-MM-DD format, or null if not mentioned
- "endDate": string or null — check-out date in YYYY-MM-DD format, or null if not mentioned
- "guests": integer or null — number of guests, or null if not mentioned
- "maxNightlyRate": number or null — maximum price per night in dollars, or null if not mentioned
- "explanation": string (required) — a one-sentence summary of what you understood from the query

Rules:
- Only extract information explicitly stated or clearly implied in the query
- Do not invent or assume values not present in the query
- If a relative date is mentioned (e.g. "next weekend", "tomorrow"), calculate it relative to today
- The explanation must be concise and describe the search in plain language
- Return valid JSON only, no markdown fences, no extra text
```

The system prompt is 918 characters, within the 6,000-character limit. Today's date is appended by the calling service as a `trustedContext` line: `Today is YYYY-MM-DD, DayOfWeek.` so the LLM can resolve relative dates. The service injects the existing `Clock` bean (from `AppConfig`) for testability.

---

## POST /api/guest/ai/search

| Field | Definition |
|---|---|
| Caller and principal | Authenticated GUEST user via `ResolvedUser` |
| Authorization rule | `RoleAuthorizationFilter` gates `/api/guest/**`; controller uses `ResolvedUser` for the user ID passed to `LlmCall` |
| Tenancy scope | The search itself returns only ACTIVE listings (existing `PropertySearchRepositoryImpl`); AI output contains no user-specific data |
| Inputs | `query`: string, 1 to 500 characters, trimmed, required. Sent in the request body as JSON: `{"query": "..."}` |
| Outputs | `AiSearchResponse`: `{ results: SearchResultResponse[], explanation: string, filters: AiSearchFiltersResponse, aiAvailable: true }` on success. See shapes below |
| Errors | See error table below |
| Idempotency | Not idempotent: each call consumes a gateway rate-limit slot and may write an audit row |
| Transaction boundary | No database transaction wraps the call. The gateway call is outside any transaction (convention). The subsequent search query runs in its own read-only default transaction |
| Events / audit | The gateway writes one `AI_CALL` or `AI_CALL_REJECTED` audit row per call (inherited from W-AI-GATEWAY) |
| Logging | Logged: feature id, user id, error code, latency. Never logged: query text, LLM response, API key. A WARN is logged when `LlmResult.flagged()` is true (user ID and feature ID only) |
| Compatibility | New endpoint; no existing callers affected. The existing `GET /api/guest/listings` is unchanged |

### Request body

```java
public record AiSearchRequest(
    @NotBlank @Size(min = 1, max = 500) String query
) {}
```

### LLM output record (validated by the gateway's JSON output spec)

```java
public record AiSearchFilters(
    @Nullable String city,
    @Nullable LocalDate startDate,
    @Nullable LocalDate endDate,
    @Nullable @Min(1) Integer guests,
    @Nullable @DecimalMin("0.01") BigDecimal maxNightlyRate,
    @NotBlank @Size(max = 200) String explanation
) {}
```

The gateway's `OutputSpec.json` validates this record using Jakarta Bean Validation after JSON deserialization. `OutputValidator` uses `FAIL_ON_UNKNOWN_PROPERTIES`, so extra fields in the LLM response trigger the fallback. The `@Size(max = 200)` on `explanation` is the hard validation limit; the system prompt asks for concise output but does not name a character count, so anything up to 200 passes. A record that fails validation triggers the fallback.

### Response shapes

**Success (200):**
```json
{
  "results": [ { "property": { ... }, "available": true } ],
  "explanation": "Looking for a 2-guest apartment in Orchard under $80.",
  "filters": {
    "city": "Orchard",
    "startDate": null,
    "endDate": null,
    "guests": 2,
    "maxNightlyRate": 80
  },
  "aiAvailable": true
}
```

`filters` lets the frontend show which parameters the AI extracted, so the user can verify and adjust. `results` uses the existing `SearchResultResponse` shape. `aiAvailable` is always `true` when the response comes from this endpoint. The response shape is different from the `{code, message, requestId}` shape used by `GuestExceptionHandler` for 400/500 errors.

**AI unavailable (200 with degraded flag):**

When the gateway returns a non-ok result (any `LlmErrorCode`: `DISABLED`, `ROLE_DENIED`, `USER_THROTTLED`, `DAILY_CAP`, `BUDGET_EXHAUSTED`, `CIRCUIT_OPEN`, `RATE_LIMITED`, `SERVER`, `TIMEOUT`, `NETWORK`, `AUTH`, `FORBIDDEN`, `BAD_REQUEST`, `BLOCKED_INPUT`, `INVALID_OUTPUT`, `REFUSED`, `AUDIT_FAILED`, `QUEUE_TIMEOUT`, `QUEUE_FULL`):
```json
{
  "results": [],
  "explanation": "AI search is temporarily unavailable. Please use the manual filters.",
  "filters": null,
  "aiAvailable": false,
  "errorCode": "USER_THROTTLED"
}
```

The endpoint returns 200 (not 503) because the client can still use manual search; `aiAvailable: false` tells the frontend to show the fallback message. The `errorCode` string is the `LlmErrorCode` enum name.

**Search-layer date rejection (200 with degraded flag):**

When the LLM extracts dates that `StayDatePolicy.requireValidSearchRange()` rejects (past start date, or end date not after start date), the controller catches the `IllegalArgumentException` and returns:
```json
{
  "results": [],
  "explanation": "The AI extracted dates that could not be searched. Please try again or use the manual filters.",
  "filters": null,
  "aiAvailable": false,
  "errorCode": "INVALID_SEARCH_DATES"
}
```

`INVALID_SEARCH_DATES` is a controller-level code (not an `LlmErrorCode`); it is used only for this catch path.

**Validation error (400):**

Uses the existing `GuestExceptionHandler.handleMethodArgumentNotValid`, which formats messages as `"field: default-message"` (Spring's field error format). No custom message attributes on the annotations.

**Authentication / authorization errors:** 401 and 403, handled by the existing filter chain — no change.

### Error table

| Condition | HTTP status | Response | Why |
|---|---|---|---|
| Missing or blank query | 400 | Validation error via `GuestExceptionHandler` (`"query: must not be blank"`) | Jakarta validation on `@NotBlank` |
| Query over 500 characters | 400 | Validation error via `GuestExceptionHandler` (`"query: size must be between 1 and 500"`) | Jakarta validation on `@Size` |
| Unauthenticated | 401 | Existing auth filter response | No bearer token or invalid token |
| Not GUEST role | 403 | Existing role filter response | RoleAuthorizationFilter |
| Gateway returns non-ok (any `LlmErrorCode`) | 200 | Degraded response with `aiAvailable: false` and `errorCode` set to the `LlmErrorCode` name | The feature is optional; the guest can still search manually |
| Gateway input flagged as injection (`LlmResult.flagged()` true, `LlmResult.ok()` true) | 200 | Normal success response; the search runs with the extracted filters. A WARN is logged (user ID, feature ID). The `flagged` status is not exposed to the client (no oracle for injection probing) | Flagging is advisory and informational; the gateway already sanitized the input. WARN-logging is the full extent of "acts on flagged" for this feature |
| Gateway input blocked after sanitizing (`BLOCKED_INPUT`) | 200 | Degraded response with `aiAvailable: false`, `errorCode: "BLOCKED_INPUT"` | Distinct from flagging: the input was empty after sanitization |
| LLM returns invalid JSON, fails validation, or contains unknown fields | 200 | Degraded response (the gateway returns the fallback via `INVALID_OUTPUT`). `OutputValidator` uses `FAIL_ON_UNKNOWN_PROPERTIES` so extra fields also trigger the fallback | Strict JSON validation prevents unexpected LLM output |
| LLM-extracted dates rejected by `StayDatePolicy` | 200 | Degraded response with `errorCode: "INVALID_SEARCH_DATES"` | The AI made the mistake, not the user; a 400 would be misleading |
| Database error during the search query | 500 | Spring's default exception handler; the controller catches only `IllegalArgumentException` from the search layer, not `DataAccessException` | Unexpected database failures are not masked |

### Security

| Threat | Mitigation |
|---|---|
| Direct prompt injection via the query field | The gateway's `InputSanitizer` strips known injection patterns, wraps user text in `<user_input>` tags, and the system prompt instructs JSON-only output. `OutputSpec.json` with `FAIL_ON_UNKNOWN_PROPERTIES` rejects non-conforming responses. The `flagged` flag is logged but not exposed to the client |
| Indirect injection via listing descriptions | Not applicable: listing descriptions are never passed to the LLM (SD1) |
| Query exfiltration through AI explanation | The explanation comes from the LLM, which only sees the user's own query and the system prompt. Output validation rejects links and HTML. The explanation is rendered as plain text in the frontend. The `PromptBuilder` SECURITY_SUFFIX instructs the model never to reveal or paraphrase its instructions. Residual: per the Known Gap, steered prose matching no pattern passes validation |
| System prompt extraction via crafted query | The `InjectionDetector` patterns are a small fixed set; rephrased probes bypass them. The `OutputSpec.json` + `FAIL_ON_UNKNOWN_PROPERTIES` defence means the model must return the prompt inside one of the six defined JSON fields (most likely `explanation`, max 200 chars). The `SECURITY_SUFFIX` mitigates this. Residual risk accepted per Known Gap |
| Denial of service via repeated AI searches | Gateway per-user throttle (5/min), per-user daily cap (200), global daily cap (3,000), circuit breaker. Frontend disables the button during loading (E35). The endpoint adds no bypass |
| Enumeration of filter values | The LLM extracts filters from the user's own query; no database data is exposed to or through the LLM |
| Error code information disclosure | The `errorCode` field exposes the `LlmErrorCode` name (SD10), allowing a client to distinguish specific gateway states. Minor information disclosure accepted because the codes help debugging and the feature is optional |
