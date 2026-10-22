# Branch State — W-AI-HOST

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `INT`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AI-HOST (from `development` at `f6cad04`)
- **Base:** `de137c6`
- **Owner:** Xiao-Congchen
- **Status:** In review
- **Approved:** Spec and plan approved (Xiao-Congchen, 2026-10-22); Gate 3 review requested
- **Last updated:** 2026-10-23 by GPT-5 — aligned Generate word-band boundaries

---

**Scope.** Define and implement the Host AI Listing Optimisation Assistant: a Host-only,
human-reviewed description generator using listing details and host-selected writing guidance,
through the shared W-AI-GATEWAY.

---

## 1. Orientation

No change.

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md` § How to Resume.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| S2 | 2026-10-23 00:20 | GPT-5 (lead) | W~1 | In review | Corrected live 400/time-field, response-lifecycle, prompt-contract, and Generate band issues | Commit Generate band fix, then leave branch ready for Gate 3 review | 2026-10-23 01:08 |

**Remaining**

- [ ] Operator/browser session checks at desktop/tablet/390px.
- [ ] Resolve or separately accept the pre-existing Next.js prerender route-module failure.
- [ ] Gate 3 approval, then separately ask whether to push/open a PR.

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W~1 | W-AI-HOST: Host AI Listing Optimisation Assistant | In review | [design](../../superpowers/specs/2026-10-22-w-ai-host-design.md) | [plan](../../superpowers/plans/2026-10-22-w-ai-host.md) | T1–T4 plus live 400, response-lifecycle, prompt-contract, and Generate band fixes complete; backend checks pass | — |

---

## 4. Architecture

### 4.14 Host AI feature

**Now:** The backend exposes a conditional, Host-authenticated `POST /api/host/ai/listing-description`
operation. It serializes address-free listing facts and fixed Host preferences into one bounded
untrusted context, calls the shared gateway once, validates the returned analysis/description, and
returns a suggestion without mutating or saving a listing. The frontend assistant remains to be built.

| Path | Role |
|---|---|
| `backend/src/main/java/com/snoozeshare/ai/` | Existing shared gateway consumed by the Host feature |
| `backend/src/main/java/com/snoozeshare/api/controller/` | Existing Host API boundary to extend only if the approved spec adds an AI endpoint |
| `frontend/src/app/host/`, `frontend/src/components/host/` | Existing Host UI boundary to extend only if the approved spec adds an AI flow |

---

## 5. Conventions

No change: the spec will apply the existing AI gateway, Host authorization, DTO, audit and
plain-text rendering conventions.

---

## 6. Known Gaps

- The feature inherits W-AI-GATEWAY's 8,000-character input cap, token-bounded JSON output,
  advisory prompt-injection flag, configured-only PII detection, and lack of complete deterministic
  factual grounding or feature-specific rate limiting.
- These are accepted constraints of the narrowed Host workstream; the UI must reject oversize
  context before the gateway and require Host review before Apply.

---

## 7. Needs a Human

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|
| Q~1 | — | — | — |

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-22 | Narrow C142's "style or instructions" for this feature to fixed style, audience, length, listing context, and current description; add no free-text instruction control | The operator's detailed modal requirements specify the complete input surface; a free-text instruction box would change the requested layout and expand the prompt-injection surface | Operator requirements; spec SD7 |
| C~2 | 2026-10-22 | Superseded by C~3: reject gateway-invalid or clearly over-limit results, while factual grounding and broader PII detection remain advisory/inherited | The operator chose the narrower gateway-compatible scope after review; the earlier stronger rejection rule required new shared-gateway or validation behavior | Operator decision |
| C~3 | 2026-10-22 | Do not amend W-AI-GATEWAY in this workstream; constrain Host AI to the existing 8,000-character input cap, gateway token/output validation, current error conventions, advisory flagging, and advisory factual analysis | The operator chose the narrower implementation after reviewing the gateway constraints; broader moderation, correlation, and grounding changes are deferred rather than silently added | Operator conversation |

---

## 9. Deviations

- **D~1 (2026-10-23):** T2 endpoint tests were added alongside the initial service seam rather than
  before the first implementation edit, because the generic gateway response type and existing
  Host service ownership API had to be inspected together. The tests cover the approved behavior;
  no scope or contract was changed.
- **D~2 (2026-10-23):** `frontend/npm run build` compiled and passed type checking but failed while
  prerendering pre-existing `/_not-found` and `/guest/*` route modules that are absent from the
  repository. This is outside the Host AI diff and remains a handover item.
- **D~3 (2026-10-23):** Browser/manual responsive checks were not run in this session; focused and
  full Vitest coverage plus lint passed, and the operator can verify the rendered flow at Gate 3.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-22 | Created the W-AI-HOST branch and began the spec-stage design framing | W~1 | — |
| 2026-10-22 | Pulled and fast-forwarded origin/development, then narrowed the spec to the existing gateway contract | W~1 | SD9, C~3 |
| 2026-10-22 | Reconciled review findings: explicit gateway registration bounds, existing error/request-id conventions, deterministic word counting, conditional activation, and inherited safety limitations | W~1 | Spec review |
| 2026-10-22 | Gate 1 approved by Xiao-Congchen; implementation planning may begin | W~1 | Gate 1 |
| 2026-10-22 | Implementation plan written, linted, and reconciled with the High-tier review | W~1 | Plan review |
| 2026-10-22 | Gate 2 approved by Xiao-Congchen; implementation may begin | W~1 | Gate 2 |
| 2026-10-23 | T1 added an explicit model-response record so gateway JSON remains separate from HTTP metadata | W~1 | Plan refinement |
| 2026-10-23 | T1 contract types, address-free context serialization, Unicode word bands, and validation tests complete | W~1 | T1 |
| 2026-10-23 | T2 added conditional Host endpoint, gateway feature registration, ownership and output validation, safe degraded mapping, and focused backend tests | W~1 | T2 |
| 2026-10-23 | T3 added typed Host AI adapter, safe degraded messages, two-column assistant modal, analysis panels, loading lockout, and Apply callback | W~1 | T3 |
| 2026-10-23 | T4 integrated the compact Sparkles trigger into New/Edit listing forms with an address-free draft snapshot and Apply-only local update | W~1 | T4 |
| 2026-10-23 | T5 verification: backend build, frontend tests (219), lint, diff inspection, and implementation review passed; frontend production prerender and browser checks remain recorded deviations | W~1 | T5 |
| 2026-10-23 | Fixed live AI request 400 by aligning frontend `checkInTime`/`checkOutTime` fields with backend `ListingFacts`, with a regression payload assertion | W~1 | Post-review fix |
| 2026-10-23 | Fixed React Strict Mode mounted-ref handling so degraded and error responses display safely and all controls unlock after completion; added regression coverage | W~1 | Post-review fix |
| 2026-10-23 | Added the missing server-side Host AI mode instructions and exact JSON response contract; added prompt regression coverage | W~1 | Post-review fix |
| 2026-10-23 | Corrected Generate word-band validation to accept the requested inclusive 50–75, 75–125, and 125–200 ranges; added boundary tests | W~1 | Post-review fix |
