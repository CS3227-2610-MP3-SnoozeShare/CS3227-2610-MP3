# W-AI-HOST Design

- **Status:** Approved (Xiao-Congchen, 2026-10-23)
- **Amended 2026-10-23:** Host guidance now uses fixed 50–150-word generation, adds description focus, and places All travellers first; see SD10.
- **Risk tier:** High — Host-authored text and listing data cross the backend-to-LLM boundary, and the feature must prevent cross-Host access and fabricated property claims.
- **Workstream:** W-AI-HOST | **Plan:** —
- **Constraints consulted:** C80, C86, C101–C111, C131; § Known Gaps (single backend instance, process-local AI limits, plain-text AI rendering); § Conventions (server-owned prompts, Host identity resolution, safe errors, gateway boundary). This workstream does not amend W-AI-GATEWAY.
- **Reviewed by lenses:** Implementer, Tester, Maintainer, Attacker (all High-tier lenses; first round findings being resolved)

## 1. Problem

Host New/Edit listing forms currently provide only a free-text description field. Hosts need help
evaluating an existing description and drafting a clearer, more complete, accurate description
without leaving the form or changing the existing layout. The shared W-AI-GATEWAY is available,
but no Host feature, endpoint, prompt contract, or frontend flow consumes it.

The desired feature is an inline **AI Description Assistant** trigger beside the existing
description field. It opens a modal where a Host chooses style, audience, and description focus,
evaluates the current text, or generates and evaluates a new suggestion of 50–150 words. Applying a
suggestion changes only the unsaved form value; the ordinary listing Save action remains the only
persistence path.

## 2. Goals

The functional and non-functional requirements are in [requirements](2026-10-22-w-ai-host-design/requirements.md).

## 3. Non-goals

- No automatic listing save or mutation from an AI response (C80 human oversight).
- No new database table, migration, AI draft column, or audit schema change; the gateway owns the
  existing AI audit rows (C103–C106).
- No direct browser-to-SoCLaaS call, client API key, user-configurable prompt, or second LLM client
  (C80, C101, C105).
- No listing creation/update API change; the assistant consumes a validated draft context and the
  existing Save action persists it.
- No streaming, conversation history, response caching, or multi-instance limiter state (gateway
  scope and C69).

## 4. Components

- [Requirements](2026-10-22-w-ai-host-design/requirements.md) — measurable behavior and acceptance criteria.
- [API](2026-10-22-w-ai-host-design/api.md) — Host endpoint, request/response contracts, authorization, errors, and gateway call.
- [UI](2026-10-22-w-ai-host-design/ui.md) — inline trigger, modal layout, controls, loading/error states, and Apply behavior.
- [Conventions](2026-10-22-w-ai-host-design/conventions.md) — feature identity, prompt/output rules, options, and test conventions.
- [Edge cases](2026-10-22-w-ai-host-design/edge-cases.md) — exact input, security, concurrency, failure, and viewport behavior.
- [Implementation order](2026-10-22-w-ai-host-design/implementation-order.md) — dependency-ordered tasks and waves.
- No data model file: this feature has no persisted-data or schema change.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

None.

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Use one `POST /api/host/ai/listing-description` operation with `EVALUATE` and `GENERATE` modes, accepting an optional persisted listing id and a validated draft context | One contract supports both New and Edit forms; two endpoints would duplicate authorization and response mapping | Operator |
| SD2 | Use fixed options: styles FRIENDLY, PROFESSIONAL, PERSUASIVE; audiences ALL_TRAVELLERS, FAMILIES, COUPLES, BUSINESS_TRAVELLERS; no Host-selected length | Matches the supplied prompt examples and makes the general audience the first option; rejected arbitrary style/audience text and length customization | Superseded by SD10; operator requirements |
| SD3 | Put the trigger inline beside the existing Description label and put all interaction in a modal; Apply updates only the controlled form draft | Preserves the current New/Edit layout and keeps persistence under the existing Save action; rejected an always-visible second editor or auto-save | Operator requirements |
| SD4 | Send only non-address listing context to the model: title, property type, city, region, capacity, rooms, amenities, check-in/out, and current description; street address and postal code are excluded | The feature does not need precise address data to write a description; reduces unnecessary personal/location data exposure | Spec author |
| SD5 | Use one structured JSON output for both modes with three criteria (`clarity`, `completeness`, `accuracy`), each `PASS` or `IMPROVE` plus one concise feedback sentence | Makes the two-column analysis stable and validates generated text against the actual listing context; rejected free-form prose | Operator requirements |
| SD6 | Treat model accuracy analysis and factual grounding as advisory; reject only gateway-invalid, unsafe, malformed, or clearly over-limit output | The existing gateway contract does not provide a complete claim-grounding engine. A stronger allowlist would expand this workstream into a new validation subsystem; Hosts review the suggestion before applying it | Operator decision after review |
| SD7 | Do not add an additional free-text instruction control in this iteration; style, audience, focus, fixed generation guidance, listing context, and current description are the complete Host guidance surface | The operator's detailed modal requirements specify the complete input surface; a free-text instruction box would change the requested layout and expand the prompt-injection surface | Operator requirements and Maintainer review |
| SD8 | A gateway-degraded result is HTTP 200 with `outcome: DEGRADED`, all analysis/generated fields null, and a stable `gatewayCode`; only `outcome: SUCCESS` can populate or apply a generated description | Makes fallback behavior unambiguous and prevents degraded content from entering the form | Implementer and Tester review |
| SD9 | Stay within the existing gateway contract: reject serialized context above 8,000 characters, use existing validation/error conventions, use token-bounded JSON validation, and treat gateway flags and grounding feedback as advisory unless the gateway already rejects the result | Avoids changing the completed shared gateway; larger context support, global moderation, stronger correlation, and new output-size semantics are out of scope for W-AI-HOST | Operator decision |
| SD10 | Remove Host-selected description length customization; every Generate request asks for 50–150 words as prompt guidance, without degrading solely for a length mismatch. Add fixed description focus options: BALANCED means highlight main selling points equally; COMFORT means emphasise living space, comfort, and available facilities; PRACTICALITY means emphasise functional features and suitability for everyday needs. BALANCED is the default. Order target audiences with ALL_TRAVELLERS first. | The operator requested a single generation range, a new focus control with exact meanings, and a general audience default; retaining the length control was rejected as unnecessary customization | Operator, 2026-10-23 |
