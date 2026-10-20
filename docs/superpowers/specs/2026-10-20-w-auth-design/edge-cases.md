# Edge cases and abuse cases

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | F1, F2, `INVALID_TOKEN` | Missing, malformed, expired, revoked or wrong-project token | Return `401 INVALID_TOKEN`; do not call a protected controller or use client UID input | Prevents forged identity | Backend unit/integration |
| E2 | F3, F7, `PROFILE_NOT_REGISTERED` | Valid Firebase identity has no profile | `/api/auth/me` returns `404 PROFILE_NOT_REGISTERED`; only registration remains available | Supports first registration | Backend integration/UI |
| E3 | F3, F5, N3 | Same-role registration repeats | Return existing profile with `200`; create no second profile or wallet | Makes retries safe | Backend integration |
| E4 | F3, F6, N3, `ROLE_ALREADY_ASSIGNED`, `ROLE_MISMATCH` | User attempts a different role or namespace | Registration returns `409 ROLE_ALREADY_ASSIGNED`; mismatched namespace returns `403 ROLE_MISMATCH` | Prevents privilege changes | Backend integration |
| E5 | F4, N1, `INVALID_REGISTRATION_CODE` | HOST/AGENT code is blank, wrong or cross-role | Return `403 INVALID_REGISTRATION_CODE`; create no profile or wallet and log no code | Prevents privileged self-registration | Backend unit/integration |
| E6 | F3 | Firebase token has `email_verified=false` | Continue authentication and registration; do not use email verification as an access gate | Owner reversed the earlier verification prerequisite for the current milestone | Backend integration |
| E7 | F3, `VALIDATION_ERROR` | Invalid role, display name or request shape | Return `400 VALIDATION_ERROR`; perform no write | Prevents malformed profile data | Backend controller |
| E8 | F3, F5, N3 | Two first-registration requests race | Uniqueness and transaction handling leave exactly one profile and one required wallet | Prevents duplicate data | Backend integration |
| E9 | F7 | Verified token email differs from stored snapshot | Return the stored snapshot; do not update it or change role during `/me` | Prevents identity confusion | Backend service |
| E10 | F1, F8 | Firebase signup succeeds but backend registration fails | Keep the Firebase session, show retry, and offer sign-out to `/login`; never store or replay a password | Prevents stranded users and secret leakage | UI |
| E11 | F2, N1, `AUTH_PROVIDER_UNAVAILABLE` | Firebase Admin runtime provider is unavailable | Return `503 AUTH_PROVIDER_UNAVAILABLE`; no unverified fallback | Preserves security boundary | Backend integration |
| E12 | F6, `ROLE_MISMATCH` | Authenticated user accesses another role namespace | Return `403 ROLE_MISMATCH` with no cross-role data | Prevents authorization bypass | Backend integration |
| E13 | F8 | User signs out while a request is in flight | Backend evaluates only the presented token; frontend clears auth/profile state and returns to `/login` after sign-out | Defines race behavior | UI/integration |
| E14 | N1, N2 | Logging, error serialization or fixtures handle secrets | No bearer token, password, code, private key or raw Firebase exception appears in output or fixtures | Prevents secret disclosure | Static/unit |
| E15 | F2, N1 | Cross-origin request uses an unapproved origin or protected preflight | `OPTIONS` is answered by exact-origin CORS policy without token verification; disallowed origins are rejected and actual requests remain protected | Prevents browser-origin abuse | Backend integration |
| E16 | F4, N1, `REGISTRATION_RATE_LIMITED` | Repeated invalid privileged codes are submitted | Return `429 REGISTRATION_RATE_LIMITED` after 5 failures per UID+IP or 20 failures per IP within 10 minutes; reset counters after 10 minutes or successful registration | Limits brute-force risk for reusable secrets | Backend integration |
| E17 | F3, F6, `PROFILE_INACTIVE` | Profile status is not `ACTIVE` | `/api/auth/me`, registration and role routes return `403 PROFILE_INACTIVE`; no wallet or profile mutation occurs | Prevents suspended-user access | Backend integration/UI |
| E18 | F8 | `returnTo` is external or protocol-relative | Ignore it and redirect to `/` after login; only a single-slash internal path is accepted | Prevents open redirect | UI |

## Threat list

Threat actors include an unauthenticated caller, a valid Guest attempting Host/Agent access, a user replaying registration, and an attacker who obtains a Firebase client token but not the Admin credential. Controls are server-side token verification, database-owned roles, role-specific URL authorization, uniqueness constraints, transactional provisioning, code secrecy and fail-closed Admin SDK errors.

## What could corrupt data

- A non-transactional registration could create a profile without its wallet or duplicate a wallet; profile and wallet writes must share one transaction with unique constraints.
- A race between duplicate registrations could create two profiles; the Firebase UID unique constraint and idempotent conflict handling must be tested.
- Accepting a client-supplied UID or role could attach a profile to another identity; both values must come from the verified token or validated server policy.
- A failed migration must leave the previous schema usable; Flyway must stop startup and preserve migration transaction behavior rather than applying a partial auth schema.
