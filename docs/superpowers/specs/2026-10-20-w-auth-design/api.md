# API contract

All protected requests use `Authorization: Bearer <Firebase ID token>`.

## `POST /api/auth/register`

Request:

```json
{
  "role": "GUEST|HOST|AGENT",
  "registrationCode": "required for HOST and AGENT; max 128 characters",
  "displayName": "optional; max 80 Unicode characters"
}
```

The Firebase UID and email come only from the verified token. The request must not accept a client-supplied UID. The token's email-verification state is not used as an access gate.

Role is parsed case-sensitively. Unknown JSON fields are rejected. `displayName` is trimmed, must not contain control characters, and is serialized as `null` when absent; an explicitly blank value is treated as `null`. A Guest request containing `registrationCode` is rejected with `400 VALIDATION_ERROR`. A same-role retry must still provide the valid role code for HOST/AGENT, returns `200 OK`, and does not change profile snapshots. A new profile returns `201 Created`. Validation occurs before profile lookup, then registration-code validation, then profile lookup, then the transaction.

Response:

```json
{
  "id": "backend-user-id",
  "email": "user@example.com",
  "role": "GUEST|HOST|AGENT",
  "displayName": "string or null",
  "walletId": "string for GUEST/HOST, null for AGENT"
}
```

Failures: `400 VALIDATION_ERROR`, `401 INVALID_TOKEN`, `403 PROFILE_INACTIVE` or `INVALID_REGISTRATION_CODE`, `409 ROLE_ALREADY_ASSIGNED`, `429 REGISTRATION_RATE_LIMITED`, and `503 AUTH_PROVIDER_UNAVAILABLE`.

## `GET /api/auth/me`

Success: `200 OK` with the same profile response. A valid Firebase identity, whether or not its email is verified, without a backend profile returns `404` with stable error code `PROFILE_NOT_REGISTERED`. A non-`ACTIVE` profile returns `403 PROFILE_INACTIVE`.

## Protected role routes

`GET /actuator/health` and CORS `OPTIONS` requests are public. `/api/auth/register` and `/api/auth/me` require a verified token; registration may run without an existing profile. `/api/guest/**`, `/api/host/**`, and `/api/agent/**` require the matching active database role. Unknown `/api/**` paths return `404` after the filter chain; missing/invalid credentials on a known protected path return `401`. URL authorization is enforced by Spring Security and service methods receive the authenticated backend user ID and role; services must derive actor/resource ownership from that principal rather than request-supplied actor IDs. Method-level checks are required for every role-sensitive service operation, using the same principal role.

## Error shape

```json
{
  "code": "INVALID_TOKEN|PROFILE_NOT_REGISTERED|PROFILE_INACTIVE|ROLE_MISMATCH|INVALID_REGISTRATION_CODE|ROLE_ALREADY_ASSIGNED|REGISTRATION_RATE_LIMITED|AUTH_PROVIDER_UNAVAILABLE|VALIDATION_ERROR|INTERNAL_ERROR",
  "message": "safe user-facing message",
  "requestId": "non-secret correlation id"
}
```

`requestId` is generated for every backend error. Messages are display-safe and are not used for frontend branching; clients branch on `code`.

## Operation security and transactions

| Operation | Principal and authorization | Transaction/events | Logging and tenancy |
|---|---|---|---|
| `POST /api/auth/register` | Verified Firebase UID; role from request only for first registration; no existing profile required | One database transaction for profile and optional wallet; emits no external event in W-AUTH | Log request ID, outcome code and backend user ID only; writes only the caller’s profile |
| `GET /api/auth/me` | Verified Firebase UID; profile lookup by principal UID | Read-only | Log request ID and outcome only; returns only caller’s profile |
| Role routes | Verified Firebase UID plus active database role; URL and service checks | Owned by later feature service | Resource access is scoped to principal-derived ownership; never trust actor IDs from requests |
