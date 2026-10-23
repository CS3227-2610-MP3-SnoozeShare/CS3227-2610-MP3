# W-AUTH — registration-code pre-validation

- **Status:** Approved (project owner, 2026-10-21)
- **Risk tier:** High — changes the authentication and privileged-role registration boundary
- **Workstream:** W-AUTH | **Plan:** [implementation plan](../plans/2026-10-21-w-auth-registration-prevalidation.md)
- **Extends:** [W-AUTH Firebase authentication and role authorization](2026-10-20-w-auth-design.md)
- **Reviewed by lenses:** Implementer, Tester/QA, Attacker, Maintainer (rounds 1–3; findings incorporated)

## 1. Problem

The current registration page creates a Firebase user before the backend checks a Host or
Support Agent registration code. A wrong code therefore leaves an unwanted Firebase identity,
even though no backend profile is created. This makes retries confusing and violates the desired
registration invariant for the official website flow.

## 2. Goals

For Host and Agent registration, the website shall obtain a successful backend code
validation before invoking Firebase Email/Password signup. A wrong or malformed privileged code
shall create neither a Firebase identity nor a backend profile through the website flow.

The backend shall continue to validate the code during authenticated profile registration. The
pre-validation endpoint is an early user-experience guard, not an authorization grant.

## 3. Non-goals and limitation

- Guest registration remains unchanged and does not require a pre-validation request.
- Registration-code storage, rotation, and role ownership remain unchanged from the approved
  W-AUTH spec.
- No Firebase Admin deletion or compensating cleanup is added.
- A modified client can still call Firebase directly and create an identity before contacting the
  backend; preventing that requires moving signup behind a trusted server or Firebase blocking
  functions, which is outside this slice. The official website flow is the protected scope. The
  public preflight necessarily reveals whether a submitted code is valid; rate limiting and secret
  strength are the compensating controls for this accepted limitation.

## 4. Components

- [requirements](2026-10-21-w-auth-registration-prevalidation-design/requirements.md) — acceptance criteria.
- [api](2026-10-21-w-auth-registration-prevalidation-design/api.md) — pre-validation contract.
- [ui](2026-10-21-w-auth-registration-prevalidation-design/ui.md) — registration ordering and errors.
- [edge-cases](2026-10-21-w-auth-registration-prevalidation-design/edge-cases.md) — abuse and failure cases.
- [implementation-order](2026-10-21-w-auth-registration-prevalidation-design/implementation-order.md) — plan seed.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Add a public `POST /api/auth/registration-code/validate` endpoint for HOST and AGENT pre-validation | Firebase signup currently precedes code validation; a public preflight is required before a Firebase token exists. Keeping final authenticated validation rejects bypassing clients | Project owner |
| SD2 | Return `204` on valid code, `403 INVALID_REGISTRATION_CODE` on a well-formed wrong code, and `429 REGISTRATION_RATE_LIMITED` after five invalid attempts per IP+role or twenty invalid attempts per IP in ten minutes | The frontend needs a stable success/failure signal; separate buckets preserve the existing per-identity and global-IP intent without making one threshold unreachable | Project owner |
| SD3 | Do not persist pre-validation state or issue a token | The code itself is the reusable authorization secret and the authenticated registration endpoint must recheck it | Project owner |
| SD4 | Use `HttpServletRequest.getRemoteAddr()` only for public pre-validation; do not consume forwarded headers in this slice | Avoids attacker-controlled IP rotation and leaves proxy-aware client identity for a later deployment/security decision | Project owner |
| SD5 | A valid pre-validation resets only the selected role’s public counter; invalid attempts are atomically counted before returning `403` or `429` | A known Host code must not reset the Agent brute-force bucket, and concurrent requests must observe one linearized limiter operation | Project owner |
| SD6 | This extension supersedes the original approved spec’s email-verification prerequisite for the current branch, consistent with branch decision C~4 | The branch already removed email verification as an access gate; the extension must not reintroduce SD5 from the original document | Project owner |
