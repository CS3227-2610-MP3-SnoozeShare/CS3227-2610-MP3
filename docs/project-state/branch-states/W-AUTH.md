# Branch State — W-AUTH

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`; read it for context and write here instead of editing it.

- **Branch:** W-AUTH (from `development` at `a785e75`)
- **Base:** `0bfd04d`
- **Owner:** project owner
- **Status:** Approved
- **Approved:** Xiao-Congchen, 2026-10-21 (recorded by Claude Sonnet 5.5 at the owner's instruction, after the PR had already merged)
- **Last updated:** 2026-10-21 by Claude Sonnet 5.5 — review fixes: client IP from `CF-Connecting-IP`, `returnTo` parsing, register-failure state

---

**Scope.** This branch implements W-AUTH: Firebase-backed identity, backend token verification and role authorization, registration with role codes, and frontend auth state and route protection. It does not implement role feature pages or domain services beyond registration-time wallet provisioning.

---

## 1. Orientation

Rows added or changed in `PROJECT_STATE.md` § Orientation: W-AUTH specification and implementation files are under `docs/superpowers/`, `backend/src/main/java/com/snoozeshare/auth`, `backend/src/main/java/com/snoozeshare/config`, `backend/src/main/java/com/snoozeshare/ProdEnvironmentGuard.java`, and `frontend/src`.

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

- [ ] **Handover:** Owner reviews the W-AUTH remediation and decides whether to approve and merge it into `development`.

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AUTH | Firebase authentication and role authorization | In review | [spec](docs/superpowers/specs/2026-10-20-w-auth-design.md); [registration pre-validation spec](docs/superpowers/specs/2026-10-21-w-auth-registration-prevalidation-design.md) | [plan](docs/superpowers/plans/2026-10-21-w-auth-registration-prevalidation.md) | Trusted proxy IP resolution added; full backend/frontend verification green; owner review remains | — |

## 4. Architecture

### 4.9 Web platform skeleton (backend, frontend, hosting)

**Now:** W-AUTH initializes Firebase Admin when `FIREBASE_ENABLED=true`, requires security, proxy, and Firebase settings in the prod profile, verifies revocable Firebase ID tokens, establishes a stateless Spring Security principal, resolves the active role from the PostgreSQL `users` row keyed by Firebase UID, applies role-specific URL authorization, and provides transactional profile/wallet registration plus `/api/auth/me`. Registration abuse limits use the direct peer locally and, only when explicitly enabled for the production proxy, the `CF-Connecting-IP` address that Cloudflare overwrites on every request; `X-Forwarded-For` is never read. Email verification status is informational only and does not gate authentication or registration. The frontend validates public Firebase configuration in production, lazily initializes Firebase browser auth, attaches ID tokens to backend requests, maps stable backend errors, logs safe correlated auth failures at the backend boundary, exposes auth screens, and protects role routes with client-side guards that ignore stale profile responses.

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

Authentication failures return a request ID that is logged with status, safe error code and path; credentials, tokens and registration codes are never logged. Production requires explicit security/Firebase enablement and all Firebase/registration/Admin credential settings. Public Firebase browser configuration is deployment metadata, not a secret.

## 6. Known Gaps

- W-DOMAIN still owns domain persistence and wallet operations beyond registration-time provisioning.
- Public code pre-validation remains a validity oracle by design; rate limits and secret strength are the compensating controls.
- Rate limiting is process-local. Forwarded-header trust is off locally and on in production, where the client address comes from `CF-Connecting-IP`; that is only sound while all traffic reaches Render through Cloudflare, so re-check it if the hosting or proxy changes (including W-DEPLOY).
- Host and Agent codes remain reusable configuration values; rotation requires replacing deployment configuration.
- Protected frontend routes use client-side guards; backend role authorization remains authoritative.
- Firebase token revocation is checked on every backend request, coupling request latency and availability to Firebase.

## 7. Needs a Human

| ID | Question | Why it blocks | Owner |
|---|---|---|---|
| — | No open questions | — | — |

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
| C~8 | Implement the approved role navbar and empty landing shells directly without a standalone spec or plan | Project owner explicitly approved the inline implementation to keep this UI-only change moving; feature documents are intentionally skipped | Project owner, 2026-10-21 |
| C~9 | Validate HOST/AGENT registration codes before the website calls Firebase signup, while retaining backend final validation and rate limiting | Project owner observed that an invalid code currently creates a Firebase identity; the website flow must avoid that side effect, while direct or modified clients remain protected by the backend check | Project owner, 2026-10-21 |
| C~10 | Production must fail startup unless security, Firebase, registration-code and Admin credential settings are explicitly configured; frontend browser Firebase values are deployment metadata and CI uses non-secret repository variables or safe fallbacks | Review found that an unset security toggle would otherwise expose a permit-all production chain and that CI secrets were unnecessary for public browser configuration | Review remediation, 2026-10-21 |
| C~11 | Auth error responses carry a generated request ID that is logged with status, safe code and path; unexpected failures use `INTERNAL_ERROR` | Review found that returned request IDs had no correlation and database failures were mislabeled as validation errors | Review remediation, 2026-10-21 |
| C~12 | Use the proxy-set `CF-Connecting-IP` address only when `SNOOZESHARE_TRUST_FORWARD_HEADERS=true`; never read `X-Forwarded-For`; local development ignores proxy headers and falls back to `getRemoteAddr()`. Amends the first version of this decision, which took the first `X-Forwarded-For` entry | The operator requested per-client Render rate limiting while preserving safe local testing. Review found the leftmost `X-Forwarded-For` entry is client-forgeable (Render's proxy appends to it), which let an attacker rotate fake IPs past every limiter; Render's guidance is to use `CF-Connecting-IP` | Project owner, 2026-10-21; review remediation |

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
| 2026-10-21 | Fixed login routing to wait for the resolved backend profile, route registered users by role, and send unregistered users to registration; added regression coverage | W-AUTH | login redirect fix |
| 2026-10-21 | Added outlined account-return action, artifact-aligned role dropdown styling, and specific duplicate-email registration feedback; frontend tests, lint and production build pass | W-AUTH | register-page UX |
| 2026-10-21 | Replaced the native role select with a reusable artifact-style custom dropdown using the shared `bg-background` field surface; interaction coverage added and all frontend checks pass | W-AUTH | custom dropdown |
| 2026-10-21 | Applied the artifact login header treatment to login, register, and forgot-password pages through a shared auth-header component; 31 frontend tests, lint and production build pass | W-AUTH | auth header styling |
| 2026-10-21 | Added shared role navbar without avatars, Host and Support Agent workspace pills, protected empty Guest/Host/Agent landing shells, and role-shell tests; 35 frontend tests, lint and production build pass | W-AUTH | role navbar and landing shells |
| 2026-10-21 | Added and refined the artifact-aligned role navbar: two-row hierarchy, larger tighter-spaced branding, full-width content, full-bleed separator, smaller wider tabs, workspace/wallet pills, and bottom-aligned active underline; 36 frontend tests, lint and production build pass | W-AUTH | navbar artifact alignment |
| 2026-10-21 | Added compact outlined logout control wired to Firebase sign-out and redirected authenticated users away from login, registration, and password-reset pages; 41 frontend tests, lint and production build pass | W-AUTH | logout and auth-page redirects |
| 2026-10-21 | Restyled the logout control as compact text-only action without outline or background; 41 frontend tests, lint and production build pass | W-AUTH | logout visual refinement |
| 2026-10-21 | Increased spacing between the wallet pill and logout action and slightly increased navbar typography while preserving compact row height; 41 frontend tests, lint and production build pass | W-AUTH | navbar typography spacing |
| 2026-10-21 | Started registration-code pre-validation design so invalid Host/Agent codes do not create Firebase accounts in the official website flow; backend final validation remains authoritative | W-AUTH | C~9 |
| 2026-10-21 | Drafted and linted the high-risk registration pre-validation spec; read-only implementer, tester, attacker and maintainer reviews completed and findings incorporated | W-AUTH | [pre-validation spec](../../superpowers/specs/2026-10-21-w-auth-registration-prevalidation-design.md) |
| 2026-10-21 | Wrote and linted the implementation plan; read-only plan review completed and findings incorporated | W-AUTH | [pre-validation plan](../../superpowers/plans/2026-10-21-w-auth-registration-prevalidation.md) |
| 2026-10-21 | Added public registration-code pre-validation endpoint, strict request contract, direct-peer IP limiter, security permit, and no-store responses; backend build/checkstyle/tests pass | W-AUTH | T1 |
| 2026-10-21 | Added unauthenticated frontend preflight, stable error mapping, signup ordering, duplicate-submit lock, and existing-session retry behavior; 46 frontend tests and lint pass | W-AUTH | T2 |
| 2026-10-21 | Added final-check bypass, stale-code, concurrent limiter, no-store/media, and unavailable-backend regression coverage; backend checkstyle/tests and 47 frontend tests/lint pass | W-AUTH | T3 |
| 2026-10-21 | Full backend build and frontend production build pass with dummy public Firebase/API values; diff clean and branch pushed through T3 | W-AUTH | T4 |
| 2026-10-21 | Completed registration pre-validation workstream slice; all plan tasks done, branch state is In review, and no merge/approval was performed | W-AUTH | T5 |
| 2026-10-21 | Added descriptive placeholders to login, registration, and forgot-password form inputs; frontend presentation-only refinement | W-AUTH | auth form UX |
| 2026-10-21 | Closed review findings for production fail-closed configuration, limiter ordering/reset scope, auth error correlation, stale frontend auth responses, safe return paths, development Firebase absence, CI public config and deployment documentation | W-AUTH | review remediation |
| 2026-10-21 | Added opt-in trusted-proxy client IP resolution with local fallback, production configuration and unit coverage | W-AUTH | C~12 |
| 2026-10-21 | Replaced first-entry `X-Forwarded-For` client IP with `CF-Connecting-IP` (forwarded-for ignored; direct-peer fallback) so rate limits cannot be bypassed by a forged header; resolver test covers forged `X-Forwarded-For`, malformed header and local mode | W-AUTH | C~12 amended |
| 2026-10-21 | `returnTo` is parsed as a same-origin URL (closes the tab/newline protocol-relative bypass); a failed `register()` now leaves the state at `registering`; added tests that fail on the old code; merged `development` and moved `Base` | W-AUTH | review remediation |
