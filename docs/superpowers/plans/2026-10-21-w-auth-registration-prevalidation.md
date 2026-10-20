# W-AUTH registration-code pre-validation implementation plan

> **For agents:** tick each checkbox as it is completed. Spec: [registration pre-validation design](../specs/2026-10-21-w-auth-registration-prevalidation-design.md).

## Scope

**Goal:** Validate Host/Agent registration codes before the website creates a Firebase identity while retaining backend final validation.

**In scope:** Public code-validation API, public security exception, atomic direct-peer-IP rate limiting, frontend validation-before-signup ordering, retry-safe Firebase session handling, and regression tests.

**Non-goals:** Guest pre-validation; Firebase Admin deletion; server-side signup; code rotation/storage changes; forwarded-header parsing; domain features; email verification (superseded by C~4).

**Risk tier:** High — authentication boundary changes require backend contract, security-chain, abuse, ordering, and full-build verification.

## Task order

| Task | Depends on | Can run concurrently with | Wave | Status |
|---|---|---|---|---|
| T1 | — | — | 1 | Done |
| T2 | T1 | — | 2 | Done |
| T3 | T1, T2 | — | 3 | Not started |
| T4 | T3 | — | 4 | Not started |
| T5 | T4 | — | 5 | Not started |

## Traceability

| Requirement | Task(s) | Verified by |
|---|---|---|
| F9 | T1, T2, T3 | `AuthContractTest`, `page.test.tsx` ordering test |
| F10 | T1, T2, T3 | `RegistrationCodeValidationTest`, `page.test.tsx` invalid-code test |
| F11 | T1, T2, T3 | `RegistrationCodeValidationTest`, `api.test.ts`, `page.test.tsx` |
| F12 | T1, T3 | existing `AuthRegistrationIntegrationTest` plus bypass regression |
| F13 | T2, T3 | `page.test.tsx` Guest flow test |
| F14 | T1, T2, T3 | API error-contract and frontend mapping tests |
| N4 | T3 | backend concurrency/boundary tests and frontend flow tests |
| E1 | T2, T3 | invalid-code request and no-signup assertions |
| E2 | T2, T3 | Guest submit assertion |
| E3 | T2, T3 | Firebase signup failure test |
| E4 | T1, T3 | final authenticated invalid-code regression |
| E5 | T1, T3 | sixth/twenty-first attempt boundary tests |
| E6 | T1, T3 | concurrent limiter test |
| E7 | T2, T3 | unavailable backend mapping test |
| E8 | T1, T3 | code-rotation/final-check test |
| E9 | T1, T3 | clock expiry and role-only reset test |
| E10 | T1, T3 | forwarded-header ignored test |
| E11 | T1, T3 | unsupported media/malformed-body contract test |
| E12 | T2, T3 | duplicate-submit and existing-session retry test |

## Verification gates

- Per backend task: `cd backend && ./gradlew test` and `cd backend && ./gradlew checkstyleMain`.
- Per frontend task: `cd frontend && npm test -- --run` and `cd frontend && npm run lint`.
- Before close-out: `cd backend && ./gradlew build`; `cd frontend && npm run build` with the documented dummy public Firebase/API environment values; `git diff --check`.
- No live Firebase, database, deployment, credential rotation, push, or PR action is part of this plan.

---

### T1: Public backend pre-validation contract and security boundary

**Delivers:** Public, no-token code validation with exact request/error semantics, direct-peer-IP atomic rate limiting, no-store responses, and protected neighboring auth routes. **Satisfies:** F10, F11, F12, F14, E4, E5, E6, E8, E9, E10, E11. **Depends on:** — **Wave:** 1

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/auth/api/RegistrationCodeValidationRequest.java` | Strict JSON DTO with `HOST`/`AGENT` role input and a nonblank code capped at 128 characters; unknown fields remain rejected. |
| Create | `backend/src/main/java/com/snoozeshare/auth/service/PublicRegistrationRateLimiter.java` | Synchronized process-local ten-minute counters keyed by direct peer IP and role plus global IP; invalid attempts increment before returning 403/429, valid checks reset only the selected role. Keep it separate from the existing authenticated UID+IP limiter and inject a test clock. |
| Create | `backend/src/main/java/com/snoozeshare/auth/service/RegistrationCodeValidationService.java` | Constant-time comparison against the existing Host/Agent configuration, Guest rejection, request-shape policy, and limiter calls; no Firebase/database writes. |
| Modify | `backend/src/main/java/com/snoozeshare/auth/controller/AuthController.java` (`/api/auth/**` handlers) | Add `POST /registration-code/validate`, return 204 on success, handle `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, and `HttpMediaTypeNotSupportedException` as 400 `AuthErrorResponse`, and set `Cache-Control: no-store` on endpoint outcomes. |
| Modify | `backend/src/main/java/com/snoozeshare/auth/config/SecurityConfig.java` (`authorizeHttpRequests`) | Permit `/api/auth/registration-code/validate` before the authenticated `/api/auth/**` matcher while leaving register/me protected. |
| Verify | `backend/src/main/java/com/snoozeshare/auth/security/FirebaseAuthFilter.java` (`isProtectedPath`) | Confirm the new path remains outside bearer filtering while preserving register/me and role namespace protection; no code change is expected. |
| Test | `backend/src/test/java/com/snoozeshare/AuthContractTest.java` | Assert no-token 204/400/403/429 behavior, stable error shape, no-store header, and neighboring auth protection. |
| Create | `backend/src/test/java/com/snoozeshare/RegistrationCodeValidationTest.java` | Assert constant-time role-code outcomes, direct peer IP selection, ignored forwarded headers, atomic counters, reset/expiry boundaries, and no persistence calls. |

**Checklist**

- [x] 1. Create failing `RegistrationCodeValidationTest` and extend `AuthContractTest` for public access, exact 400/403/429 responses, no-store headers, forwarded-header ignoring, atomic counter boundaries, and protected neighboring routes; run the focused tests and observe failure.
- [x] 2. Implement the strict request DTO, service, and atomic public limiter using the existing configured secrets and `MessageDigest.isEqual`.
- [x] 3. Add the controller route and exception/header handling; preserve `/api/auth/register` and `/api/auth/me` bearer requirements.
- [x] 4. Add the explicit Spring Security matcher and confirm Firebase filter path behavior.
- [x] 5. Run `cd backend && ./gradlew test` and `cd backend && ./gradlew checkstyleMain`; fix all failures.
- [x] 6. Update branch-state Progress and Changes for T1.
- [x] 7. Commit: `feat: add pre-registration code validation endpoint`.

### T2: Frontend pre-validation and retry-safe registration ordering

**Delivers:** Public API helper and registration UI flow that never calls Firebase signup after a failed privileged-code preflight and never repeats signup for an existing Firebase session. **Satisfies:** F9, F10, F11, F13, F14, E1, E2, E3, E7, E12. **Depends on:** T1 **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/lib/api.ts` (`BackendApiError`, shared error parsing) | Add unauthenticated `validateRegistrationCode(role, registrationCode)`; handle 204 success and stable 400/403/429/network errors without requiring `auth.currentUser`. |
| Modify | `frontend/src/app/register/page.tsx` (`submit`, `auth.currentUser`) | Lock submission, preflight only HOST/AGENT, show exact mapped messages, call Firebase signup only after 204, and skip signup on retry when `auth.currentUser` already exists before calling backend registration. |
| Test | `frontend/src/lib/api.test.ts` | Test no-token public preflight, 204, stable error parsing, 429, and network/5xx fallback. |
| Test | `frontend/src/app/register/page.test.tsx` | Test invalid-code no-signup, valid-code ordering, Guest bypass, duplicate-submit lock, Firebase failure, and existing-session retry. |

**Checklist**

- [x] 1. Add failing API/page tests proving invalid Host/Agent code prevents `registerWithEmail`, Guest skips preflight, and existing sessions skip duplicate signup.
- [x] 2. Implement the public API helper with JSON request headers and established `BackendApiError` mapping.
- [x] 3. Update the register submit state machine and exact user-facing messages from the approved UI contract.
- [x] 4. Run `cd frontend && npm test -- --run` and `cd frontend && npm run lint`; fix all failures.
- [x] 5. Update branch-state Progress and Changes for T2.
- [x] 6. Commit: `feat: validate registration codes before Firebase signup`.

### T3: Cross-layer regression and abuse coverage

**Delivers:** Deterministic backend/frontend coverage for all pre-validation edge cases and bypass resistance. **Satisfies:** F9–F14, N4, E1–E12. **Depends on:** T1, T2 **Wave:** 3

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `backend/src/test/java/com/snoozeshare/AuthContractTest.java` | Add route precedence, CORS/OPTIONS, malformed/media, no-store, and neighboring protected-route assertions. |
| Modify | `backend/src/test/java/com/snoozeshare/AuthRegistrationIntegrationTest.java` | Add final-check bypass and stale-code cases proving no profile/wallet write after invalid authenticated registration. |
| Create | `backend/src/test/java/com/snoozeshare/PublicRegistrationRateLimiterTest.java` | Exercise concurrent invalid operations, sixth/twenty-first boundaries, expiry, role-local reset, and forwarded-header-independent keys. |
| Modify | `frontend/src/app/register/page.test.tsx` | Complete page-level ordering, retry, and duplicate-submit acceptance coverage. |
| Modify | `frontend/src/lib/api.test.ts` | Complete helper-level response and failure coverage. |

**Checklist**

- [ ] 1. Add failing tests for E4–E11 and the F/N assertions not already covered by T1/T2: final-check bypass, stale-code rejection using two service instances with different configured codes, concurrent limiter boundaries, expiry/reset, no-store/error contracts, CORS/OPTIONS, and protected neighboring routes.
- [ ] 2. Make no production test-bypass changes; only add deterministic test clocks or verifier/controller fixtures if required by the named assertions.
- [ ] 3. Run focused backend and frontend suites, then the complete component suites.
- [ ] 4. Run `cd backend && ./gradlew checkstyleMain` and `cd frontend && npm run lint`.
- [ ] 5. Update branch-state Progress and Changes for T3.
- [ ] 6. Commit: `test: cover registration pre-validation boundaries`.

### T4: Full verification and state evidence

**Delivers:** Green component builds and verified evidence for the approved spec. **Satisfies:** F9–F14, N4. **Depends on:** T3 **Wave:** 4

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `docs/superpowers/plans/2026-10-21-w-auth-registration-prevalidation.md` | Tick completed T1–T3 checklists and mark task statuses after evidence is collected. |
| Modify | `docs/project-state/branch-states/W-AUTH.md` | Record verification commands, Changes, Deviations, and the next handoff action. |

**Checklist**

- [ ] 1. Run `cd backend && ./gradlew build`.
- [ ] 2. Run `cd frontend && npm run build` with `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`, `NEXT_PUBLIC_FIREBASE_API_KEY=key`, `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=project.firebaseapp.com`, `NEXT_PUBLIC_FIREBASE_PROJECT_ID=project`, `NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=project.firebasestorage.app`, `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=123`, and `NEXT_PUBLIC_FIREBASE_APP_ID=app-id`.
- [ ] 3. Run `git diff --check` and inspect the complete diff for credentials, tokens, or unrelated files.
- [ ] 4. Record all command evidence and any deviations in the branch state.
- [ ] 5. Commit: `test: verify registration pre-validation integration`.

### T5: Close-out and handoff

**Delivers:** Review-ready W-AUTH branch state with no approval claim. **Satisfies:** F9–F14, N4. **Depends on:** T4 **Wave:** 5

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `docs/superpowers/plans/2026-10-21-w-auth-registration-prevalidation.md` | Mark all tasks done and complete the close-out checklist. |
| Modify | `docs/project-state/branch-states/W-AUTH.md` | Empty the session row, set workstream/branch status to `In review`, and record the handoff. |

**Checklist**

- [ ] 1. Confirm every Traceability row has a passing test or documented manual check.
- [ ] 2. Confirm the spec, plan, branch state, and commits agree; record any deviation instead of silently changing scope.
- [ ] 3. Set branch state to `In review`; never set it to `Approved`.
- [ ] 4. Tell the project owner the branch is ready for review.
