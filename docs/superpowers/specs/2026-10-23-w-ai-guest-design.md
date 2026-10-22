# W-AI-GUEST Design — AI Search Assistant

- **Status:** Approved (Nathan, 2026-10-23)
- **Risk tier:** Medium - untrusted user text is sent to an LLM via the existing gateway; no money, no permissions, no schema change. Attacker lens added: untrusted input crosses to the LLM.
- **Workstream:** W-AI-GUEST | **Plan:** —
- **Constraints consulted:** C80 (SOC LLM only, outputs are suggestions, key stays on backend, inputs sanitized, outputs validated, rate-limited, capped, audited), C101 (gateway is its own workstream, merged), C103 (generic gateway; role registers AiFeature), C104 (audit metadata only), C111 (reasoning-effort defaults to none), Known Gap "Steered prose that matches no leak or link pattern passes output validation", Known Gap "Reasoning cannot be chosen per feature or tier", Known Gap "429 behaviour is unobserved", § Conventions "Role code calls LlmGateway.run outside any transaction", § Conventions "Nothing outside com.snoozeshare.ai imports ai.config/guard/limit/client/audit", § Conventions "@ConditionalOnDatabase on beans that need the database"
- **Reviewed by lenses:** Implementer, Tester, Attacker (untrusted input to LLM) - round 1 done and merged

## 1. Problem

Guests currently search listings through five explicit filter fields: city, check-in date, check-out date, guest count, and max nightly rate (`GuestSearchController.searchListings`). A guest who knows what they want in natural language — "a cozy apartment in Orchard for 2 under $80 next weekend" — must manually decompose that into the five fields.

**Current behaviour (verified in code):** `GET /api/guest/listings` accepts `city`, `startDate`, `endDate`, `guests`, `maxNightlyRate` as optional query parameters. `PropertySearchRepositoryImpl` builds a JPQL query filtering ACTIVE listings. No AI integration exists on the Guest side.

**Desired behaviour:** A new endpoint accepts a free-text natural language query, sends it to the LLM (FAST tier via `LlmGateway`), receives structured JSON matching the existing search parameters, executes the existing search with those parameters, and returns the results with a short AI-generated explanation of what was searched for. The frontend adds an AI search bar alongside the existing manual filters on the Guest search page.

## 2. Goals

Seven functional and three non-functional requirements with acceptance criteria are in [requirements](2026-10-23-w-ai-guest-design/requirements.md).

**Functional:** F1 AiFeature registration; F2 POST endpoint; F3 system prompt and JSON output; F4 degraded-result handling; F5 frontend AI search bar; F6 explanation display; F7 manual filters preserved.

**Non-functional:** N1 response within gateway deadline; N2 system prompt server-side only; N3 inherits all gateway protections.

## 3. Non-goals

- Ranking or re-ranking search results using the LLM — results use the existing search ordering (C80: outputs are suggestions; the filters are the suggestion, not a custom ranking)
- Passing listing descriptions as LLM context — avoids indirect injection from host-authored content; the LLM only sees the user's query
- Multi-turn conversation for search refinement — single query per call; the guest can submit another query
- Amenity filtering — the existing `SearchCriteria` and `PropertySearchRepositoryImpl` do not support amenity filters; adding them is a separate change
- Streaming (SSE) responses — per W-AI-GATEWAY non-goal
- Caching or deduplication of AI search calls — per W-AI-GATEWAY non-goal
- Any schema or migration change — no new tables or columns

## 4. Components

- [Requirements](2026-10-23-w-ai-guest-design/requirements.md) - F1-F7 and N1-N3 with acceptance criteria
- [API](2026-10-23-w-ai-guest-design/api.md) - the new endpoint, the AiFeature registration, the system prompt contract, error responses
- [UI](2026-10-23-w-ai-guest-design/ui.md) - the AI search bar on the Guest search page, states, and interaction
- [Conventions](2026-10-23-w-ai-guest-design/conventions.md) - naming, package placement, test rules
- [Edge cases](2026-10-23-w-ai-guest-design/edge-cases.md) - E1-E36 covering input, LLM output, partial output, failure, permission, UI state, and concurrency
- [Implementation order](2026-10-23-w-ai-guest-design/implementation-order.md) - T1-T5 with dependencies and waves
- No data model changes: no new tables, columns, or migrations

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Single-phase design: the LLM extracts filters only, no second call to rank or explain individual results | Avoids putting listing descriptions into the LLM context (indirect injection surface), halves token cost, one LLM call per search. Rejected: two-phase search+rank (doubles cost, introduces indirect injection via host descriptions) | Operator chose Option A |
| SD2 | FAST model tier for the search feature | The operator confirmed FAST; the model name is changeable via `SNOOZESHARE_AI_MODEL_FAST` without code changes | Operator |
| SD3 | Risk tier Medium with Implementer, Tester, and Attacker lenses | Untrusted user input crosses to the LLM, but no money, permissions, or schema changes; the gateway handles key, limits, and audit. Rejected: High (no money or data-model risk beyond what the gateway already covers) | Spec author |
| SD4 | The AI search endpoint is POST (not GET) because it carries a request body and triggers an LLM side-effect (audit row, rate-limit consumption) | GET with a query body is non-standard and often stripped by proxies. Rejected: GET with query parameter (side-effects make GET misleading) | Spec author |
| SD5 | The LLM output is a JSON record (`AiSearchFilters`) with nullable fields matching `SearchCriteria` plus a required `explanation` string | Reuses the existing search infrastructure with no mapping layer. Rejected: a free-form text response parsed by the backend (fragile, no validation) | Spec author |
| SD6 | The frontend AI search bar is a separate input above the existing filter row, not a replacement | The operator specified AI search alongside manual filters (F7). Rejected: replacing the filter bar (breaks existing UX) | Operator |
| SD7 | The AI search controller catches `IllegalArgumentException` from the search layer (invalid dates extracted by the LLM) and returns a degraded 200 with `errorCode: "INVALID_SEARCH_DATES"`, not a 400 | The AI made the date mistake, not the user; a 400 would mislead. Rejected: letting the 400 propagate (penalizes the user for LLM errors) | Tester review |
| SD8 | `OutputValidator` uses `FAIL_ON_UNKNOWN_PROPERTIES`; extra JSON fields from the LLM trigger the fallback, not silent extraction | Verified in code. E12 corrected from the original draft which incorrectly assumed unknown properties were ignored | Attacker review |
| SD9 | The `flagged` status is not exposed to the client; WARN-logging (user ID, feature ID) is the full extent of "acts on flagged" for this feature | No oracle for injection probing. Rejected: adding a `flagged` field to the response (helps attackers test bypass patterns) | Attacker review |
| SD10 | Error codes from `LlmErrorCode` are exposed in the degraded response | Minor information disclosure (system health state) accepted because the codes help debugging and the feature is optional. Rejected: collapsing all codes to a generic string (loses observability) | Attacker review |
| SD11 | The service injects the `Clock` bean from `AppConfig` for building the `trustedContext` date, enabling deterministic testing of relative-date resolution | Rejected: `LocalDate.now()` (untestable) | Implementer review |
