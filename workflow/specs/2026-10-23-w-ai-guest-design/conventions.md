# Conventions

Cite existing PROJECT_STATE § Conventions by ID; list only rules new or applied here.

| Rule | Applies to | Source |
|---|---|---|
| Role code calls `LlmGateway.run` outside any transaction, supplies `userId` and `role` from its authorized principal, builds ASSISTANT turns only from server-stored history, puts only server-built text in `trustedContext`, acts on `flagged`, and renders AI output as plain text through a response DTO | `GuestAiSearchController` and the service that calls the gateway | § Conventions |
| Nothing outside `com.snoozeshare.ai` imports `ai.config`, `ai.guard`, `ai.limit`, `ai.client` or `ai.audit` | The new controller and service import only `LlmGateway`, `AiFeature`, `LlmCall`, `LlmResult`, `LlmErrorCode`, `Turn`, `TurnRole`, `OutputSpec`, `ModelTier`, `AiFeatureRegistry` (all top-level `ai` package) | § Conventions |
| Beans that need the database carry `@ConditionalOnDatabase` | The new controller and any service bean that injects a repository | § Conventions |
| Guest controllers use `ResolvedUser` for authenticated identity | `GuestAiSearchController` uses `ResolvedUser` to get the user ID for `LlmCall` | § Conventions (Guest controllers) |
| The `AiFeature` bean is registered in a `@Configuration` class under `com.snoozeshare.controller.guest` (co-located with the Guest AI controller) | `GuestAiConfig` | New in this spec |
| The AI search request and response DTOs live in `com.snoozeshare.api.request` and `com.snoozeshare.api.response` respectively, following the existing DTO placement | `AiSearchRequest`, `AiSearchResponse`, `AiSearchFiltersResponse` | § Conventions (C73, DTO layer at API boundary) |
| The `AiSearchFilters` record (the LLM output type) lives in `com.snoozeshare.controller.guest` alongside the feature config, since it is the feature's output contract, not a shared DTO | `AiSearchFilters` | New in this spec |
| The `trustedContext` passed to `LlmCall` contains only server-generated text (today's date); `untrustedContext` is null (no listing data in context) | The service method that builds the `LlmCall` | § Conventions + SD1 |
| Frontend renders the AI explanation as plain text (not HTML or Markdown) | The AI explanation card in the search page | § Conventions (AI output as plain text) + Known Gap "Steered prose that matches no leak or link pattern passes output validation" |
| Test naming: integration tests for the new endpoint extend `PostgresIntegrationTest` with `FakeTokenVerifier` for auth bypass, matching the existing Guest controller test pattern | `GuestAiSearchControllerTest` | § Conventions (Guest controller tests) |
