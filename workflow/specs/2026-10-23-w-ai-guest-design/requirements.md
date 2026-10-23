# Requirements

## Functional

- **F1** Register a `guest.search` `AiFeature<AiSearchFilters>` bean (FAST tier, GUEST role, priority 2, JSON output) that extracts structured search filters from a natural language query
  - *Acceptance:* Application starts with AI enabled; `AiFeatureRegistry` contains `guest.search`; calling `LlmGateway.run` with a sample query returns an `AiSearchFilters` record
- **F2** A new `POST /api/guest/ai/search` endpoint accepts a text query (1 to 500 characters), calls the gateway, runs the existing search with the extracted filters, and returns the search results plus the AI explanation
  - *Acceptance:* Given a GUEST user sends a POST with `{"query": "apartment in Orchard for 2"}`, the response contains `results` (listing array), `explanation` (string), `filters` (extracted values), and `aiAvailable: true`
- **F3** The LLM system prompt instructs the model to return a JSON object with fields matching the existing `SearchCriteria` (city, startDate, endDate, guests, maxNightlyRate) plus an `explanation` field
  - *Acceptance:* The `AiSearchFilters` record has exactly these six fields; the system prompt specifies JSON-only output; the gateway `OutputSpec.json` validates the record
- **F4** When the gateway returns a degraded result (not ok), the endpoint returns a 200 response with `aiAvailable: false`, the `LlmErrorCode` name, an empty results array, and a user-friendly explanation
  - *Acceptance:* Given the gateway is disabled, a POST returns `{"aiAvailable": false, "errorCode": "DISABLED", "results": [], "explanation": "AI search is temporarily unavailable. Please use the manual filters."}`
- **F5** The frontend Guest search page adds an AI search text input with a submit action that calls the new endpoint and displays results in the same listing grid
  - *Acceptance:* The search page at `/guest/search` shows an AI text input above the manual filters; submitting it calls `POST /api/guest/ai/search` and renders the results in the existing grid layout
- **F6** The frontend shows the AI explanation above the results when an AI search succeeds
  - *Acceptance:* After a successful AI search, a card above the results grid displays the explanation text and the extracted filter values as read-only chips
- **F7** The frontend preserves the existing manual filter search — AI search is an alternative, not a replacement
  - *Acceptance:* The five manual filter fields and "Search" button remain functional; using manual search clears AI results; using AI search does not hide the manual filters

## Non-functional

- **N1** The AI search endpoint responds within the gateway deadline (45 s) plus the existing search query time; no additional external call
  - *Threshold:* Total response time is at most `deadlineMs` (default 45,000 ms) + search query time. Only one LLM call is made per request
- **N2** The system prompt is server-side only and never returned in the response (C80)
  - *Threshold:* No endpoint returns the system prompt text; it is embedded in the `AiFeature` bean definition, not in any DTO or configuration endpoint
- **N3** AI search inherits all gateway protections: sanitizing, injection detection, per-user throttle, daily caps, circuit breaker, audit (C80, C103)
  - *Threshold:* The endpoint calls `LlmGateway.run` and does not bypass any gateway step; per-user throttle is 5 calls/min; all other limits are the gateway defaults
