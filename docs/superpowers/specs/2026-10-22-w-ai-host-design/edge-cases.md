# Edge Cases

| ID | Requirement | Scenario | Expected behavior (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | F1, N4 | Button is rendered beside the Description label on New and Edit forms | Existing textarea/card order and normal form layout remain unchanged; button opens only the modal | Prevents UI regression | UI |
| E2 | F2, N4 | Modal opens with existing, empty, or 5,000-character description | Textarea copies exactly the controlled form value; no value is written back before Apply | Draft edits must remain reversible | UI |
| E3 | F2 | Style/audience/focus is omitted or unknown | API returns `400 VALIDATION_ERROR`; no LLM request is sent | Prevents ambiguous prompts | Service |
| E4 | F2, N5 | Description, title, or context field exceeds its limit | API returns `400 VALIDATION_ERROR`; no truncation silently changes Host intent | Bounds prompt and request size | Unit |
| E5 | F3 | Evaluate receives blank or whitespace-only current description | API returns `422 DESCRIPTION_REQUIRED_FOR_EVALUATE`; UI keeps modal open and shows the safe validation message | Evaluation has no subject | API/UI |
| E6 | F4 | Generate receives an empty current description | Gateway receives an omitted/empty current-description data block; a valid generated description may still be returned | New listings need generation | Integration |
| E7 | F4, F5 | Generated response has currentAnalysis null for a non-empty current description or generatedAnalysis null | Output validation rejects it as `INVALID_OUTPUT`; no generated text reaches the UI | Ensures both analyses are real | Unit |
| E8 | F5, N2 | Model returns an extra criterion, invalid status, HTML, link, or feedback above 240 characters | Gateway/feature rejects the response and returns a safe degraded result; existing gateway Markdown-fence handling remains in force | Prevents unsafe/unrenderable output | Unit |
| E9 | F4, N5 | Model returns fewer or more than 50–150 words, using the defined Unicode word tokenizer | The response follows normal successful output validation; length alone does not produce `DEGRADED` or disable Apply, because the 50–150 range is generation guidance | Preserves the operator's decision not to degrade usable output for length mismatch | Unit |
| E10 | F7 | Host sends another Host's listing id, a nonexistent id, or an id after ownership changes | API returns `404 NOT_FOUND`; no prompt or gateway audit call is made | Prevents cross-tenant data exposure | Integration |
| E11 | F7, N1 | Request includes JSON fields for street address, postal code, or unknown properties | Existing request validation rejects unknown/address fields; no LLM request is made | Minimizes sensitive context and prevents contract drift | API |
| E12 | F8, N3 | Gateway is disabled, throttled, queue-full, budget-blocked, circuit-open, times out, or returns invalid/refused output | UI shows safe error, leaves existing form text unchanged, re-enables controls, and keeps modal open | Failure must not destroy drafts | Integration/UI |
| E13 | F8, N2 | Gateway returns a validated fallback without generated text | Response is `outcome: DEGRADED` with null current/generated analysis and description; UI shows only the safe error state | Prevents applying degraded content | API/UI |
| E14 | N3 | User clicks Evaluate/Generate/Apply/close repeatedly while waiting | Only the first request is sent; all controls remain disabled until it completes | Prevents duplicate quota use and state races | UI |
| E15 | N3 | Request resolves after modal close is requested or component unmounts | No state update occurs on the unmounted modal; the form value remains unchanged | Prevents stale async writes | UI |
| E16 | F6 | User clicks Apply, then edits another field and saves | Only the final ordinary listing Save request persists the generated description and other edits | Preserves existing persistence semantics | UI/API |
| E17 | N1, N2 | Prompt injection appears in title, amenities, or description | It remains delimited untrusted data; the gateway's advisory flag is preserved, output follows existing validation, and Host review remains required before Apply; no prompt text is logged | Direct and indirect injection are expected threats | Integration |
| E18 | N6 | Endpoint is invoked inside an active transaction | Gateway returns/propagates the existing transaction-boundary failure; no external call is made | Prevents holding a DB connection during LLM latency | Service |
| E19 | N4 | Modal is opened at 390px width | Columns stack, modal scrolls within viewport, no horizontal overflow occurs, and all controls remain reachable | Matches existing mobile Host acceptance | UI |
| E20 | N5 | Model returns a description containing a bare domain without a scheme | Gateway's known limitation remains; UI renders accepted output as plain text and never turns it into a link | Records the inherited gap without adding unsafe rendering | Unit |
| E21 | N1, N7 | Current description contains configured PII or a generated response contains unsupported personal data | Gateway applies its existing sanitizer/validator behavior; the feature does not promise detection beyond that contract and does not include address/postal fields in context | Records the inherited gateway limitation without silently expanding scope | Integration |
| E22 | F4, N5 | Generated description is outside the 50–150-word guidance range | The successful response remains usable and is not degraded solely for the mismatch; the UI may display and Apply the gateway-accepted plain text | Preserves the operator's explicit no-degradation decision | Unit/UI |

## What could corrupt data

- A generated description must never be written directly to the database. Only Apply changes the
  controlled client draft, and only the existing validated Save action persists it.
- A forged `listingId` must not select another Host's property context. Ownership is resolved from
  the verified principal before any gateway call.
- A model response must not replace the draft unless its JSON shape, analysis fields, and output
  safety checks pass. A 50–150-word mismatch alone does not block Apply; invalid output leaves the
  current description unchanged.
- Factual analysis is advisory. The Host reviews the generated suggestion before Apply; this
  workstream does not claim complete deterministic detection of unsupported factual claims.
- Duplicate clicks must not issue duplicate requests or race a later result into the form; the
  modal's pending state owns the single in-flight operation.
