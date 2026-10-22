# API

## POST `/api/host/ai/listing-description`

| Field | Contract |
|---|---|
| Caller | Authenticated Firebase principal with active `HOST` role; `HostIdentityResolver` supplies the Host id. |
| Authorization | Controller is enabled only with `snoozeshare.security.enabled=true`; a non-null `listingId` must belong to the resolved Host, otherwise return `404 NOT_FOUND` and do not call the gateway. A null `listingId` is allowed for a new-listing draft. |
| Transaction | No active database transaction may surround `LlmGateway.run`; the endpoint performs no listing mutation. Gateway audit is handled by the gateway's own transaction. |
| Request | JSON body below; unknown fields are rejected. |
| Response | JSON body below; HTTP 200 for accepted gateway results and safe gateway-degraded failures. Validation/auth/resource errors use their stated HTTP status. |
| Logging | Safe request id, status, stable code, feature id, Host id, and latency only. Never log prompt, request body, response body, API key, or host instructions. |
| Idempotency | None. Evaluate and Generate each create a new gateway call and may consume a quota slot. The UI does not retry automatically. |

### Request body

```json
{
  "listingId": "null or UUID",
  "mode": "EVALUATE | GENERATE",
  "writingStyle": "FRIENDLY | PROFESSIONAL | PERSUASIVE",
  "targetAudience": "FAMILIES | COUPLES | BUSINESS_TRAVELLERS | ALL_TRAVELLERS",
  "length": "SHORT | MEDIUM | LONG",
  "currentDescription": "required non-null string, 0 to 5000 characters",
  "listing": {
    "title": "required non-null string, 0 to 120 characters",
    "propertyType": "required enum: HOUSE | APARTMENT | CONDO | PRIVATE_ROOM",
    "city": "required non-null string, 0 to 120 characters",
    "region": "required non-null string, 0 to 120 characters",
    "maxGuests": "null or JSON integer 1 to 100",
    "bedrooms": "null or JSON integer 0 to 100",
    "bathrooms": "null or JSON integer 0 to 100",
    "amenities": "required non-null array of known AmenityType values, at most 30; duplicates normalize to one value",
    "checkInTime": "null or string HH:mm, 24-hour, no seconds",
    "checkOutTime": "null or string HH:mm, 24-hour, no seconds"
  }
}
```

`listingId` is optional only for a new draft. `listing` is always required because the New form
has no persisted row and the Edit form may contain unsaved values. The server validates the draft
shape, treats every field as untrusted context, and uses persisted ownership only to authorize a
non-null id. Street address and postal code are deliberately not accepted.

The frontend snapshots the form at click time. Controlled values supply title, description,
property type, city, region, capacity, rooms, and times; a form reference supplies checked
amenities. Display labels are converted to API enums (`Private room` → `PRIVATE_ROOM`, amenity
labels to uppercase underscore names), blank numeric/time fields become JSON null, and HTML time
values remain `HH:mm`. No separate free-text instruction field is sent.

`currentDescription` may be empty for `GENERATE`. It must contain at least one non-whitespace
character for `EVALUATE`; otherwise return `422 DESCRIPTION_REQUIRED_FOR_EVALUATE`. Invalid UUIDs,
numeric coercions, unknown fields, and invalid times return
`400 VALIDATION_ERROR`, following the existing Host error contract. Duplicate-property handling
follows the existing backend JSON parser; this feature does not introduce a new global parser
policy. Duplicate amenities normalize according to the existing Host DTO convention rather than
being treated as a separate error.

### Response body

```json
{
  "outcome": "SUCCESS | DEGRADED",
  "mode": "EVALUATE | GENERATE",
  "currentAnalysis": {
    "clarity": { "status": "PASS | IMPROVE", "feedback": "1 to 240 characters" },
    "completeness": { "status": "PASS | IMPROVE", "feedback": "1 to 240 characters" },
    "accuracy": { "status": "PASS | IMPROVE", "feedback": "1 to 240 characters" }
  },
  "generatedDescription": "null for EVALUATE or DEGRADED; selected word band and gateway-accepted JSON output for successful GENERATE",
  "generatedAnalysis": "null for EVALUATE or DEGRADED; same three analysis rows for successful GENERATE",
  "gatewayCode": "null on SUCCESS; stable gateway code on DEGRADED",
  "requestId": "standard HTTP request id"
}
```

For `outcome: SUCCESS`, `currentAnalysis` is null only for Generate with an empty current
description; it is populated for Evaluate and Generate with a non-empty current description.
`generatedDescription` and `generatedAnalysis` are null for Evaluate. For `outcome: DEGRADED`,
all three content fields are null regardless of mode. The response never contains the prompt,
upstream body, key, raw model output after validation failure, or hidden listing data.

### Errors

| Condition | Status/code | Exact behavior |
|---|---|---|
| Missing/invalid auth or wrong role | Existing auth response / `403` | No gateway call; do not reveal listing existence. |
| Unknown or non-owned `listingId` | `404 NOT_FOUND` | Existing safe Host error response; no gateway call. |
| Malformed body, unknown enum, invalid draft field or over-limit text | `400 VALIDATION_ERROR` | Existing safe Host error response; no gateway call. |
| Evaluate with blank description | `422 DESCRIPTION_REQUIRED_FOR_EVALUATE` | Existing safe Host error response; no gateway call. |
| AI disabled, role denied, user throttled, daily cap, queue, breaker, budget, timeout, network, auth, forbidden, invalid output, refusal, or audit failure | `200` with `outcome: DEGRADED` and `gatewayCode` | All content fields are null; use the feature fallback and safe UI message. Never retry in the controller. |
| Unexpected feature exception | `500 INTERNAL_ERROR` | Existing request-id error response; log class/status/path only. |

### Gateway registration

The role registers `host.listingDescription` as an `AiFeature<ListingDescriptionResponse>` with
allowed role `HOST`, tier `STANDARD`, priority `1`, JSON output bounded by the gateway's existing
token limit, `maxTokens=1800`, `maxInputChars=8000`, `maxTurns=1`, no links, and a feature fallback that returns
`outcome: DEGRADED` and null content fields. The controller maps `LlmResult.code()` to
`gatewayCode`; the fallback function itself receives only the call. The prompt is server-side and
treats all listing fields and current description as untrusted data. The gateway receives `userId`
and `HOST` from the authenticated request, not the body.

Evaluate uses one call with the current description analysis. Generate uses one call that evaluates
the current text when non-empty, generates the new text, and evaluates the generated text. The
feature must not make a second repair call when JSON validation fails. The HTTP request's
`requestId` uses the normal Host request-id convention for every HTTP response. The gateway
`callId` remains internal to the gateway contract and is not returned or promised as a
cross-log correlation id by this feature.

The AI controller and its gateway-backed feature registration follow the existing
`@ConditionalOnDatabase` convention so datasource-free backend contexts remain usable. The
endpoint itself still requires authenticated Host authorization when enabled.

## Frontend adapter result and safe messages

The typed Host adapter returns a discriminated result: `outcome: SUCCESS` includes the content
fields and enables Apply only for Generate; `outcome: DEGRADED` includes null content fields and
the code, which the modal maps to one of these exact messages:

| Code(s) | Message |
|---|---|
| `DISABLED` | AI assistance is currently unavailable. |
| `ROLE_DENIED` | AI assistance is unavailable for this account. |
| `USER_THROTTLED`, `DAILY_CAP`, `QUEUE_FULL`, `QUEUE_TIMEOUT` | AI assistance is busy. Try again shortly. |
| `BUDGET_EXHAUSTED` | AI assistance is temporarily unavailable because its shared budget is exhausted. |
| `CIRCUIT_OPEN`, `RATE_LIMITED`, `SERVER`, `TIMEOUT`, `NETWORK`, `AUTH`, `FORBIDDEN`, `BAD_REQUEST` | The AI service could not complete the request. Try again later. |
| `BLOCKED_INPUT`, `INVALID_OUTPUT`, `REFUSED` | The AI could not safely produce a result. Review the description and try again. |
| `AUDIT_FAILED` | The AI result could not be recorded. No suggestion was applied. |
