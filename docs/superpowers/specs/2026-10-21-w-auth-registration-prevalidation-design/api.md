# API contract

## `POST /api/auth/registration-code/validate`

This endpoint is public and requires no Firebase bearer token because it runs before account
creation. It accepts only the role and submitted code:

```json
{
  "role": "HOST|AGENT",
  "registrationCode": "string"
}
```

Unknown fields are rejected. Role parsing is case-sensitive and only `HOST` and `AGENT` are
accepted. A missing body, missing role, unknown role, blank code, overlong code (over 128
characters), malformed JSON, wrong content type, or unknown field returns `400 VALIDATION_ERROR`.
A well-formed HOST/AGENT request with a nonblank code of at most 128 characters that does not
match the configured code returns `403 INVALID_REGISTRATION_CODE`.
A valid configured code returns `204 No Content`; rate exhaustion returns `429
REGISTRATION_RATE_LIMITED`. All responses include `Cache-Control: no-store` and never contain the
configured or submitted code.

The endpoint performs no Firebase, database, profile, wallet, or session mutation. It applies the
existing process-local 10-minute limits with one atomic operation: five invalid attempts for the
same direct peer IP and role, or twenty invalid attempts for the same direct peer IP across roles.
The sixth invalid attempt in a role bucket, or the twenty-first invalid attempt in the global
bucket, returns `429`; the boundary is inclusive until ten minutes after the first attempt. A
valid code resets only that role’s public counter and never resets the other role or global
counter. Invalid JSON, unknown fields, missing fields, invalid roles, blank/overlong codes, wrong
content type, and Guest requests do not consume public counters. The authenticated registration
limiter keeps its existing UID+IP and global-IP counters and reset behavior; the public and
authenticated buckets are separate.

Client IP is always `HttpServletRequest.getRemoteAddr()` for this slice. `Forwarded` and
`X-Forwarded-For` are ignored, including when supplied by a reverse proxy. This avoids trusting
an unspecified proxy boundary; a proxy-aware resolver is a later deployment/security decision.
The resolver and synchronized atomic limiter operation are unit tested. Limiter operations
linearize in monitor order: an invalid operation increments before deciding whether its result is
`403` or `429`; a valid operation resets only its role bucket.

The endpoint is explicitly matched before the authenticated `/api/auth/**` matcher with
`permitAll()` and bypasses Firebase bearer filtering;
`/api/auth/register` and `/api/auth/me` remain bearer-protected. CORS `OPTIONS` handling remains
public and uses the existing exact-origin policy.

The `204`/`403` distinction is an intentional validity oracle required by the current client-side
Firebase signup architecture. It is not treated as a security authorization grant. Codes must be
high-entropy operator secrets; distributed brute-force resistance is outside this slice.

All error responses reuse the established `AuthErrorResponse` shape with `code`, safe `message`,
and `requestId`. A controller exception handler maps unsupported media types and malformed request
bodies to `400 VALIDATION_ERROR`; the endpoint and its error handlers add `Cache-Control: no-store`
to `204`, `400`, `403`, and `429` responses.

## Existing registration endpoint

`POST /api/auth/register` remains bearer-protected and repeats the role-code validation before any
profile lookup or transaction. A caller that bypasses pre-validation therefore cannot register a
Host or Agent profile with an invalid code.
