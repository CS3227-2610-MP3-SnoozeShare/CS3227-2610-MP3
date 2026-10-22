# Branch State — ai-gateway-t8-defaults

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** ai-gateway-t8-defaults (from `development` at `7eef36a`)
- **Base:** `7eef36a` (development tip when the branch was cut)
- **Owner:** CongChen
- **Status:** Folded
- **Approved:** Xiao-Congchen 2026-10-22 (recorded after the merge, PR #18)
- **Last updated:** 2026-10-22 by Claude Sonnet 5.5 - approval, Render check and key rotation recorded

---

**Scope.** Follow-up to the merged W-AI-GATEWAY branch (`docs/project-state/branch-states/W-AI-GATEWAY.md`, still to be folded). It records the T8 live-check results against the real SoCLaaS service and changes three things they showed: two configuration defaults and one startup log line. Small change (`AGENTS.md` § 3), with the spec amended in place.

---

## 1. Orientation

No change.

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md`, no additional steps.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|

**Remaining**

- [x] Done 2026-10-22, reported by the owner: Render is reachable. GATE: after this merges and Render redeploys, set `SNOOZESHARE_AI_ENABLED=true` with `SOCLAAS_BASE_URL` and `SOCLAAS_API_KEY` on the dev service and confirm the log line `SoCLaaS reachable: N models listed, all configured models present`. That closes the one T8 item still open (reachability from Render); then set the variable back to `false` or leave it on.
- [x] Done 2026-10-22, reported by the owner: the development key was rotated. GATE: rotate the development SoCLaaS key in the portal; it was printed in a terminal during T8 and read into an agent session.
- [ ] **Handover:** before a production key is configured, read its requests-a-minute limit from the portal budget endpoint and set `SNOOZESHARE_AI_MAX_RPM` to a value below it (the development key allows 30; the default is now 25).
- [ ] **Handover:** role AI specs (W-AI-GUEST, W-AI-HOST, W-AI-AGENT) can start once the Render check above is recorded. They must set `maxTokens` with room to spare if a feature ever turns reasoning back on; with the default `none` the models answer in a few tokens.

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W~1 | W-AI-GATEWAY: shared SoCLaaS LLM gateway (follow-up to the merged branch) | Done | [spec](docs/superpowers/specs/2026-10-22-w-ai-gateway-design.md) | [plan](docs/superpowers/plans/2026-10-22-w-ai-gateway.md) | T8 checks recorded; Render reachable (owner, 2026-10-22) | — |

---

## 4. Architecture

No change: the structure of the AI gateway is unchanged. Only defaults move (§ Decisions); the area's *Now* text in the W-AI-GATEWAY branch state stays correct.

---

## 5. Conventions

No change.

---

## 6. Known Gaps

Entries added:

- **Reasoning cannot be chosen per feature or tier.** `reasoning-effort` is one global setting (default `none`). `llama3.1:8b` rejects `low` with HTTP 400, and `low` does not stop `qwen3.5:9b` or `gemma4:26b` from reasoning, so a feature that needs reasoning would need a gateway amendment (a per-feature option and `maxTokens` headroom).
- **429 behaviour is unobserved.** 100 requests in a burst returned no 429 although the development key's policy says 30 requests a minute; whether a 429 carries `Retry-After` and what a budget 429 says are still unknown, so those paths rely on the spec's assumptions.

---

## 7. Needs a Human

No change. The SoCLaaS-contract half of Q6 is now answered by `docs/runbooks/dev-environment.md` § Evidence (AI gateway live checks), apart from the Render reachability item in § How to Resume.

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-22 | `snoozeshare.ai.max-rpm` defaults to 25 (was 55) | Operator: the development key allows 30 requests a minute (portal budget endpoint). Rejected: leaving 55 and setting an environment variable on Render, because a forgotten variable means 429s | spec SD9 |
| C~2 | 2026-10-22 | `snoozeshare.ai.reasoning-effort` defaults to `none` (was empty, field omitted) | Operator: `qwen3.5:9b` and `gemma4:26b` spend `max_tokens` on reasoning and returned empty content at 20 and 300 tokens; all three models accept `none`. Rejected: switching to other chat models (most larger ones also reason and cost 4 to 20 times as much per output token), and keeping reasoning on with large `maxTokens` | spec SD10 |
| C~3 | 2026-10-22 | The startup model check logs one INFO line when it succeeds | Operator: silence on success is indistinguishable from "AI is off" in Render logs | `ModelCheck` |

---

## 9. Deviations

No change.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-22 | Branch cut from `development` after PR #17 merged | W~1 | — |
| 2026-10-22 | T8 live-check results recorded in the runbook Evidence block | W~1 | — |
| 2026-10-22 | Defaults: `max-rpm` 25, `reasoning-effort` `none`; spec SD9, SD10, api.md, F8, E27, E37 amended; tests updated | W~1 | C~1, C~2 |
| 2026-10-22 | `ModelCheck` logs an INFO line on success | W~1 | C~3 |
| 2026-10-22 | Pull request merged (PR #18) before the approval was recorded; the owner confirmed the approval afterwards in chat and it was written then | W~1 | — |
| 2026-10-22 | Owner reported that Render is reachable with AI enabled, and that the development key was rotated; the runbook Evidence row was updated | W~1 | — |

---

## Fold record
Folded 2026-10-22 by Claude Sonnet 5.5 on development · merge commit `be1d8694c08edbc1008471e01e3f17af869b8246` · base `7eef36a`

| Provisional | Permanent |
|---|---|
| C~1 | C110 |
| C~2 | C111 |
| C~3 | C112 |

Workstream W~1 is the existing W-AI-GATEWAY row (no W number allocated).

Sessions discarded at fold: none
