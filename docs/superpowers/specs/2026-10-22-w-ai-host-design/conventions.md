# Conventions

- Feature id is `host.listingDescription`; it is the only code outside the gateway's public
  `com.snoozeshare.ai` contract that identifies this feature.
- The feature uses `LlmGateway.run` with `Role.HOST`, tier `STANDARD`, priority `1`, JSON output,
  the gateway's existing JSON/token output bounds, `maxInputChars=8000`, `maxTurns=1`, `allowLinks=false`, `maxTokens=1800`, and
  `temperature=0.4`. It uses the gateway's global
  `reasoning-effort=none`; no per-feature reasoning override is added.
- The system prompt is server-side code and contains the task rules and response schema only. A
  single server-built USER turn says `Perform the requested listing-description task using the
  structured data in untrusted context.` Mode, style, audience, length, listing fields, and current
  description are serialized as one JSON object in `LlmCall.untrustedContext`; `trustedContext` is
  empty. They are data, never instructions. The serialized context must be at most 8,000
  characters after NFKC normalization; reject it before `LlmGateway.run` rather than relying on
  gateway truncation.
- The prompt must require exactly the response keys in [api.md](api.md): `outcome`, `mode`, `currentAnalysis`,
  `generatedDescription`, and `generatedAnalysis`; each analysis object has only `clarity`,
  `completeness`, and `accuracy`, with status `PASS` or `IMPROVE` and feedback of 240 characters
  or fewer.
- Generated text is plain text. The gateway applies its existing link, HTML/image, refusal,
  schema, and token/output rules; its existing JSON path may strip Markdown fences. The feature
  does not add a separate Markdown parser. It additionally validates the selected word band and
  feedback shape. A word is a maximal Unicode-letter-or-number run, optionally containing internal
  apostrophe or hyphen joins; surrounding punctuation and whitespace do not count. Accuracy
  analysis and factual grounding are advisory; the feature does not claim to prove every property
  claim and does not add a new moderation or claim-grounding subsystem.
- The feature never logs prompt text, descriptions, host instructions, listing context, raw model
  output, API keys, or upstream bodies. It relies on the gateway's metadata-only AI audit.
- The gateway sanitizer applies its existing configured PII redaction before the external call.
  Street address and postal code fields are excluded from the listing context. Broader address or
  postal-code detection inside Host-authored descriptions is not guaranteed by this workstream.
- Host-specific word-band or shape validation happens after the gateway result and therefore does
  not rewrite the gateway's already-recorded metadata audit; a post-validation failure is returned
  as a safe Host-side `INVALID_OUTPUT` degraded result.
- Host ownership is enforced from the verified Firebase principal and server-side listing lookup;
  client-supplied `listingId` is never treated as proof of ownership.
- The New/Edit trigger snapshots the current form at click time: controlled values provide title,
  description, type, city, region, capacity, rooms, and times; a form ref reads checked amenity
  controls. Display `Private room` becomes `PRIVATE_ROOM`, amenity labels become enum names,
  blank numeric/time fields become null, and times remain `HH:mm`.
- Backend tests use the gateway mock seam and assert the exact feature request/context, including a
  deterministic valid mock response with three PASS analyses and a 50-word generated description.
  Frontend
  tests mock the typed Host AI adapter and cover modal state transitions without making network
  calls from presentation-only fixture tests.
