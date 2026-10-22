# Branch State — W-AI-HOST

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `INT`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AI-HOST (from `development` at `f6cad04`)
- **Base:** `f6cad04`
- **Owner:** Xiao-Congchen
- **Status:** Planned
- **Approved:** Spec and plan approved (Xiao-Congchen, 2026-10-22); implementation in progress
- **Last updated:** 2026-10-22 by GPT-5 — started Host AI feature

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
| S1 | 2026-10-22 21:56 | GPT-5 (lead) | W~1 | Building | T1 complete; beginning T2 backend endpoint | T2 step 1: write failing endpoint tests | 2026-10-23 00:25 |

**Remaining**

- [ ] Finish the constrained W-AI-HOST spec review and ask for gate 1 approval.

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W~1 | W-AI-HOST: Host AI Listing Optimisation Assistant | Building | [design](../../superpowers/specs/2026-10-22-w-ai-host-design.md) | [plan](../../superpowers/plans/2026-10-22-w-ai-host.md) | T1 complete; T2 backend endpoint begins | — |

---

## 4. Architecture

### 4.14 Host AI feature

**Now (proposed):** No Host AI feature is implemented yet. This branch will add a Host-only
assistant that calls `LlmGateway.run` with listing details and host-selected writing guidance, then
returns a generated description as a suggestion for Host review and editing. It must not mutate a
listing or auto-apply model output.

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

No change.

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
