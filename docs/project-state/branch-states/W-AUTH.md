# Branch State — W-AUTH

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`; read it for context and write here instead of editing it.

- **Branch:** W-AUTH (from `development` at `a785e75`)
- **Base:** `a785e75`
- **Owner:** project owner
- **Status:** In review
- **Approved:** —
- **Last updated:** 2026-10-21 by Codex — email-verification policy reversed; verification complete

---

**Scope.** This branch implements W-AUTH: Firebase-backed identity, backend token verification and role authorization, registration with role codes, and frontend auth state and route protection. It does not implement role feature pages or domain services beyond registration-time wallet provisioning.

---

## 1. Orientation

Rows added or changed in `PROJECT_STATE.md` § Orientation: W-AUTH specification and implementation files will be added under `docs/superpowers/`, `backend/src/main/java/com/snoozeshare/security`, `backend/src/main/java/com/snoozeshare/controller/auth`, and `frontend/src`.

## 2. How to Resume

**Setup**

```bash
cd frontend && npm ci
cd ../backend && ./gradlew test
```

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|

**Remaining**

- [ ] **Handover:** Owner reviews the W-AUTH branch and decides whether to approve and merge it into `development`.
- [ ] **Handover:** After spec approval, write the implementation plan and execute it with Firebase emulator/test doubles for automated tests.

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AUTH | Firebase authentication and role authorization | In review | [spec](docs/superpowers/specs/2026-10-20-w-auth-design.md) | [plan](docs/superpowers/plans/2026-10-20-w-auth.md) | Email verification no longer gates auth; backend/frontend gates pass | — |

## 4. Architecture

### 4.9 Web platform skeleton (backend, frontend, hosting)

**Now:** W-AUTH initializes Firebase Admin when `FIREBASE_ENABLED=true`, verifies revocable Firebase ID tokens, establishes a stateless Spring Security principal, resolves the active role from the PostgreSQL `users` row keyed by Firebase UID, applies role-specific URL authorization, and provides transactional profile/wallet registration plus `/api/auth/me`. Email verification status is informational only and does not gate authentication or registration. The frontend validates public Firebase configuration in production, lazily initializes Firebase browser auth, attaches ID tokens to backend requests, maps stable backend errors, exposes auth screens, and protects role routes with client-side guards.

| Path | Role |
|---|---|
| `backend/src/main/java/com/snoozeshare/auth/config/FirebaseConfig.java` | Firebase Admin application and credential initialization |
| `backend/src/main/java/com/snoozeshare/auth/security/FirebaseAuthFilter.java` | Verify bearer ID tokens and establish the authenticated principal |
| `backend/src/main/java/com/snoozeshare/auth/security/RoleAuthorizationFilter.java` | Resolve and enforce database role for URL namespaces |
| `backend/src/main/java/com/snoozeshare/auth/controller/AuthController.java` | Registration and current-user endpoints |
| `frontend/src/lib/firebase.ts` | Firebase JS SDK initialization |
| `frontend/src/providers/AuthProvider.tsx` | Client auth state and role/profile loading |
| `frontend/src` protected layouts | Client-side route protection and role redirects through `AuthProvider`; no middleware auth in W-AUTH |

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-20 | Keep roles in PostgreSQL keyed by Firebase UID; do not use Firebase custom claims | Existing web architecture requires server-owned role data and avoids claim synchronization | [spec](../superpowers/specs/2026-10-20-w-auth-design.md) |
| C~2 | 2026-10-20 | Require server-side registration codes for HOST and AGENT; GUEST needs no code | Preserves role isolation and prevents self-assigning privileged roles | [spec](../superpowers/specs/2026-10-20-w-auth-design.md) |

## 5. Conventions

No change: this branch adds the authentication boundary and follows existing Spring Boot, Flyway, Next.js, and environment-variable conventions.

## 6. Known Gaps

No change: this branch does not settle domain persistence beyond the authentication user/profile and wallet records needed for registration.

## 7. Needs a Human

| ID | Question | Why it blocks | Owner |
|---|---|---|---|
Resolved: HOST and AGENT use separate reusable server-side configuration secrets; rotation happens by changing the environment/configuration value.
Resolved: Firebase email verification is not required before backend registration or protected API access.
Resolved: Keep the Firebase session after backend registration failure; allow retry or explicit sign-out.
Firebase project, Email/Password provider, and local Admin SDK credentials are configured. Review rounds are complete.

## 8. Decisions

| ID | Decision | Why |
|---|---|---|
| C~1 | Roles are stored in the backend database, not Firebase custom claims | Keeps authorization controlled by the backend and avoids claim refresh/synchronization problems |
| C~2 | HOST and AGENT registration requires distinct server-side registration codes | Prevents users from choosing privileged roles without operator authorization |
| C~3 | HOST and AGENT codes are reusable configuration secrets named `SNOOZESHARE_HOST_REGISTRATION_CODE` and `SNOOZESHARE_AGENT_REGISTRATION_CODE`; rotation replaces the configured value | Project owner selected configuration-only codes for the initial milestone; database rotation is deferred | Project owner, 2026-10-20 |
| C~4 | Firebase email verification is not required before backend registration or protected API access | Project owner reversed the earlier verification prerequisite to allow immediate local and browser testing | Project owner, 2026-10-21 |
| C~5 | Keep the Firebase session after backend registration failure and expose retry/sign-out actions | Prevents a recoverable backend outage from forcing a new account/password flow | Project owner, 2026-10-20 |
| C~6 | W-AUTH may add the minimal V002 migration for auth users and wallets; W-DOMAIN owns later domain migrations and must preserve the auth contract | Project owner approved the exception so W-AUTH can be independently implemented and tested | Project owner, 2026-10-20 |
| C~7 | W-AUTH supersedes the older architecture flow that sends `firebaseUid` from the client and relies on Next middleware auth; the W-AUTH API derives UID from the verified bearer token and uses client-side guards | The older design predates the concrete Firebase browser flow; changing it avoids trusting client identity and avoids an unspecified session-cookie system | W-AUTH spec, 2026-10-20 |

## 9. Deviations

| ID | What happened | Consequence | Action |
|---|---|---|---|
| D~1 | Branch had pre-existing uncommitted Firebase Admin and Firebase JS dependency edits when execution began | The clean-tree precondition cannot be met without touching operator work | Preserve the files, include them in their planned T2/T6 dependency commits, and verify the final diff before close-out |
| D~2 | Initial frontend baseline `npm ci` was blocked by the environment cache/registry state | The gate was temporarily unverified | Resolved on 2026-10-21 with a task-local npm cache; `npm ci`, 27 tests, lint and production build pass |
| D~3 | Firebase Admin beans are gated by `FIREBASE_ENABLED` so existing datasource-free contexts remain usable | Local auth activation requires `FIREBASE_ENABLED=true`; enabled startup still fails fast for missing project, codes or ADC credentials | Document the activation variable in the developer handover at the development checkpoint |

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-20 | Started W-AUTH branch state and recorded Firebase credential setup | W-AUTH | working tree |
| 2026-10-21 | Added V002 auth users/wallets schema, role model, JDBC repository contracts and implementations; focused migration test, checkstyle and backend build pass | W-AUTH | T1 |
| 2026-10-21 | Added Firebase Admin configuration, revocation-aware token verifier, provider error classification and required backend dependencies; focused tests, checkstyle and backend build pass | W-AUTH | T2 |
| 2026-10-21 | Added stateless Spring Security bearer filtering, role namespace authorization, service authorization boundary, CORS/health handling and security integration tests; full backend build passes | W-AUTH | T3 |
| 2026-10-21 | Added registration DTOs, transactional profile/wallet provisioning, role-code validation, rate limiter, idempotent retries and `/api/auth/me`; full backend build passes; edge coverage remains | W-AUTH | T4 core |
| 2026-10-21 | Completed T4 edge coverage for inactive and unverified identities, safe request IDs, immutable identity snapshots, UID/IP limiter reset and expiry, and concurrent same-UID registration; checkstyle and full backend build pass | W-AUTH | T4 edge |
| 2026-10-21 | Added backend auth contract checks for stable error responses, secret-safe messages, unknown routes and actual-response CORS; complete backend build and checkstyle pass | W-AUTH | T5 |
| 2026-10-21 | Added Firebase browser configuration validation, lazy client initialization, email auth actions, authenticated API fetch and mocked client/API tests; frontend tests, lint and production build pass | W-AUTH | T6 |
| 2026-10-21 | Added AuthProvider session/profile state, verification refresh and sign-out handling, artifact-aligned login/register/reset screens, role route guards and provider coverage; frontend tests, lint and production build pass | W-AUTH | T7 |
| 2026-10-21 | Added page-level sign-in, registration, password-reset and route-guard acceptance tests; 27 frontend tests, lint and production build pass | W-AUTH | T8 |
| 2026-10-21 | Completed final backend/frontend verification, diff check and credential scan; branch is review-ready with no tracked service-account JSON, bearer token or password credential | W-AUTH | T9 |
| 2026-10-21 | Reversed email-verification enforcement: unverified Firebase identities may authenticate, register and access protected routes; removed verification-email client behavior and updated contract coverage | W-AUTH | C~4 reversal |
| 2026-10-21 | Removed the verification-email commit from local branch history and verified the replacement policy with backend build plus frontend tests, lint and production build | W-AUTH | C~4 reversal |
