# W-AUTH — Firebase authentication and role authorization

- **Status:** Approved (project owner, 2026-10-20)
- **Risk tier:** High — authentication, authorization, registration and personal data
- **Workstream:** W-AUTH | **Plan:** —
- **Constraints consulted:** C70, C79, C82, C83, C85, and S1–S5 in `docs/Webapp-architecture-design.md`; W-AUTH exception to W-INFRA migration ownership recorded in branch state
- **Reviewed by lenses:** Implementer, Tester/QA, Attacker, Maintainer, User/operator (rounds 1–2; findings incorporated)

## 1. Problem

The current Spring Boot web skeleton has no Firebase initialization, security filter, user profile lookup, role assignment or registration endpoint. The current Next.js skeleton has no Firebase client, auth state provider, login/registration flow or protected role routes. A Firebase identity cannot yet become an authorized SnoozeShare user.

W-AUTH supplies that missing boundary while preserving the project decision that Firebase owns credentials and the backend owns application roles. Firebase Authentication owns credentials and token lifecycle; the backend verifies every bearer token and authorizes role-specific API namespaces using the database role.

## 2. Goals

Detailed acceptance criteria are in [requirements](2026-10-20-w-auth-design/requirements.md).

**Functional**

- F1 — Establish Firebase identity and password flows in the frontend.
- F2 — Authenticate protected backend requests from verified Firebase ID tokens.
- F3 — Register one backend profile and role per Firebase UID.
- F4 — Require server-side codes for HOST and AGENT registration.
- F5 — Provision required wallets transactionally during registration.
- F6 — Enforce role-specific backend namespaces.
- F7 — Expose current-user profile data.
- F8 — Provide frontend auth state and protected route behavior.

**Non-functional**

- N1 — Keep Admin credentials and passwords out of source control and logs.
- N2 — Make auth behavior testable without real Firebase credentials.
- N3 — Make registration retry-safe under duplicate and concurrent requests.

## 3. Non-goals

- OAuth providers, MFA, phone authentication and anonymous accounts.
- Firebase custom claims as the role source of truth; roles remain database-owned under S3.
- Role feature pages, domain-service porting, AI, payments or production secret provisioning.
- Password handling in the backend; Firebase handles password storage and reset.

## 4. Components

- [requirements](2026-10-20-w-auth-design/requirements.md) — functional and non-functional acceptance criteria.
- [data-model](2026-10-20-w-auth-design/data-model.md) — profile, registration-code and wallet invariants.
- [api](2026-10-20-w-auth-design/api.md) — registration, current-user and namespace contracts.
- [ui](2026-10-20-w-auth-design/ui.md) — auth state, screens, route states and recovery behavior.
- [conventions](2026-10-20-w-auth-design/conventions.md) — security, package, logging and testing rules.
- [edge-cases](2026-10-20-w-auth-design/edge-cases.md) — abuse, failure, concurrency and corruption cases.
- [implementation-order](2026-10-20-w-auth-design/implementation-order.md) — dependency-ordered work items for the later plan.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Use Firebase Email/Password only for the initial auth surface | The project has enabled this provider; OAuth and MFA add scope without being required by W-AUTH | Project owner |
| SD2 | Resolve roles from PostgreSQL rather than Firebase custom claims | Avoids claim synchronization and keeps backend authorization authoritative | Project architecture |
| SD3 | Use Spring Security with a testable Firebase verifier abstraction | Preserves a standard security boundary and avoids real Firebase credentials in tests | Project architecture |
| SD4 | Use reusable server-side configuration secrets for HOST and AGENT registration codes; rotate by changing configuration | Keeps the first milestone operationally simple; database-backed expiry/rotation is deferred | Project owner |
| SD5 | Require Firebase email verification before backend registration and protected API access | Prevents unverified identities from becoming application users | Project owner |
| SD6 | Use client-side protected layouts driven by `AuthProvider`; do not add Firebase session cookies or Next middleware auth in W-AUTH | Browser Firebase state is not available to ordinary middleware; a session-cookie system would expand scope | Project architecture |
| SD7 | Keep the Firebase session after backend registration failure and offer retry/sign-out | Makes a temporary backend failure recoverable without recreating the Firebase account | Project owner |
| SD8 | W-AUTH owns minimal `V002__auth_users_wallets.sql`; W-DOMAIN owns later domain migrations and preserves the auth table contract | Allows registration and role authorization to be independently tested while keeping the later domain schema decision open | Project owner |
| SD9 | This spec supersedes the older architecture diagram’s client-supplied `firebaseUid` and Next middleware auth; W-AUTH derives UID from the verified bearer token and guards routes in client layouts | Resolves the concrete security and browser-state conflict without adding session-cookie scope | W-AUTH review |
