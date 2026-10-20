# Branch State — W-AUTH

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`; read it for context and write here instead of editing it.

- **Branch:** W-AUTH (from `development` at `a785e75`)
- **Base:** `a785e75`
- **Owner:** project owner
- **Status:** In review
- **Approved:** —
- **Last updated:** 2026-10-20 by Codex — second review findings incorporated

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
| S1 | 2026-10-20 | Codex (lead) | W-AUTH | Active | Plan in review | Project owner reviews the implementation plan; after approval, execute T1 in the plan | 2026-10-20 |

**Remaining**

- [ ] Project owner approves the W-AUTH implementation plan.
- [ ] **Handover:** After spec approval, write the implementation plan and execute it with Firebase emulator/test doubles for automated tests.

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AUTH | Firebase authentication and role authorization | Planned | [spec](docs/superpowers/specs/2026-10-20-w-auth-design.md) | [plan](docs/superpowers/plans/2026-10-20-w-auth.md) | Plan complete; awaiting project owner approval | — |

## 4. Architecture

### 4.9 Web platform skeleton (backend, frontend, hosting)

**Now (proposed):** W-AUTH adds Firebase Admin initialization to the Spring Boot backend, verifies a Firebase ID token at the HTTP boundary, resolves the role from the PostgreSQL `users` row keyed by Firebase UID, and applies role-specific URL authorization. The Next.js frontend initializes the Firebase JS SDK from `NEXT_PUBLIC_FIREBASE_*` values, exposes auth state through `AuthProvider`, sends bearer tokens through the API wrapper, and protects role routes. Registration creates the Firebase identity client-side, then creates the backend profile and wallet transactionally after the backend verifies the bearer token.

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
Resolved: Firebase email verification is required before backend registration and protected API access.
Resolved: Keep the Firebase session after backend registration failure; allow retry or explicit sign-out.
Firebase project, Email/Password provider, and local Admin SDK credentials are configured. Review rounds are complete.

## 8. Decisions

| ID | Decision | Why |
|---|---|---|
| C~1 | Roles are stored in the backend database, not Firebase custom claims | Keeps authorization controlled by the backend and avoids claim refresh/synchronization problems |
| C~2 | HOST and AGENT registration requires distinct server-side registration codes | Prevents users from choosing privileged roles without operator authorization |
| C~3 | HOST and AGENT codes are reusable configuration secrets named `SNOOZESHARE_HOST_REGISTRATION_CODE` and `SNOOZESHARE_AGENT_REGISTRATION_CODE`; rotation replaces the configured value | Project owner selected configuration-only codes for the initial milestone; database rotation is deferred | Project owner, 2026-10-20 |
| C~4 | Firebase email verification is required before backend registration and protected API access | Project owner selected verified email as a prerequisite for application identity | Project owner, 2026-10-20 |
| C~5 | Keep the Firebase session after backend registration failure and expose retry/sign-out actions | Prevents a recoverable backend outage from forcing a new account/password flow | Project owner, 2026-10-20 |
| C~6 | W-AUTH may add the minimal V002 migration for auth users and wallets; W-DOMAIN owns later domain migrations and must preserve the auth contract | Project owner approved the exception so W-AUTH can be independently implemented and tested | Project owner, 2026-10-20 |
| C~7 | W-AUTH supersedes the older architecture flow that sends `firebaseUid` from the client and relies on Next middleware auth; the W-AUTH API derives UID from the verified bearer token and uses client-side guards | The older design predates the concrete Firebase browser flow; changing it avoids trusting client identity and avoids an unspecified session-cookie system | W-AUTH spec, 2026-10-20 |

## 9. Deviations

No change.

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-20 | Started W-AUTH branch state and recorded Firebase credential setup | W-AUTH | working tree |
