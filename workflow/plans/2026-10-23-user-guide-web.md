# Web User Guide Rewrite Implementation Plan

- **Status:** Approved (Trevor, 2026-10-23)

> **For agents:** tick `- [ ]` -> `- [x]` as each step finishes. Spec: [Web User Guide Rewrite Design](../specs/2026-10-23-user-guide-web-design.md).

## Scope

**Goal:** Rewrite `docs/UserGuide.md` from JavaFX/SQLite instructions into an accurate browser-app guide while preserving screenshot placeholders for later replacement.

**In scope:** browser quick start and deployment entry point; Firebase account flows; Guest, Host, and Support Agent workflows; Host and Guest AI assistants; current limitations, FAQ, troubleshooting, glossary, navigation, and screenshot placeholder markers.

**Non-goals:** screenshot capture/replacement; frontend/backend changes; production secrets; Agent AI documentation; desktop removal.

**Risk tier:** Low - documentation-only, verified through source cross-checks and Markdown/link audits.

## Task order

| Task | Depends on | Can run concurrently with | Wave | Status |
|---|---|---|---|---|
| T1 Inventory current web user experience | - | - | 1 | Done |
| T2 Rewrite guide structure and quick start | T1 | - | 2 | Done |
| T3 Rewrite role workflows and feature reference | T1 | - | 2 | Done |
| T4 Validate guide and close out | T2, T3 | - | 3 | In review |

Shared-file hazards: T2 and T3 both edit `docs/UserGuide.md`; execute them serially as listed.

## Source-to-section checklist

| Requirement | Guide section / required claim or route | Source path | Verification |
|---|---|---|---|
| F1 | Quick start: deployed frontend URL | `docs/runbooks/dev-environment.md` (evidence URL) | URL matches the runbook |
| F1 | Quick start: optional local frontend/backend path | `README.md`; `docs/runbooks/dev-environment.md` | Commands and ports match source |
| F1 | Navigation and role landing routes | `frontend/src/app/`; `frontend/src/components/role/RoleNavbar.tsx` | Every listed route exists |
| F2 | Login, registration, password reset | `frontend/src/app/login/`; `frontend/src/app/register/`; `frontend/src/app/forgot-password/`; `backend/src/main/java/com/snoozeshare/auth/` | Flow and labels match implementation |
| F2 | Host/Agent codes are administrator-provided | `docs/runbooks/dev-environment.md`; `PROJECT_STATE.md` C87 | No production code values appear |
| F2 | Suspended account behavior | `frontend/src/`; `backend/src/main/java/com/snoozeshare/auth/` | Guide describes observed error/action |
| F3 | Guest search, booking, trips, messaging, wallet, reviews | `frontend/src/app/guest/`; `frontend/src/components/guest/`; `backend/src/main/java/com/snoozeshare/controller/guest/` | Current routes and actions are covered |
| F3 | Guest AI Search Assistant | `frontend/src/app/guest/search/`; `frontend/src/lib/guest-api.ts`; `PROJECT_STATE.md` W-AI-GUEST / § 4.27 | Feature is described as current and Agent AI is excluded |
| F3 | Host listings, calendar, requests, messaging, wallet | `frontend/src/app/host/`; `frontend/src/components/host/`; `backend/src/main/java/com/snoozeshare/api/controller/` | Current routes and actions are covered |
| F3 | Host AI Listing Optimisation Assistant | `frontend/src/components/host/HostAiDescriptionAssistant.tsx`; `frontend/src/lib/hostAiApi.ts`; `PROJECT_STATE.md` § 4.17 | Eligibility, Evaluate/Generate, Apply, and degraded behavior match |
| F3 | Support Agent disputes, accounts, audit, categories | `frontend/src/app/agent/`; `frontend/src/components/agent/`; `backend/src/main/java/com/snoozeshare/api/controller/agent/` | Current routes and actions are covered |
| F4 | FAQ and troubleshooting | Current guide claims cross-checked against web sources above | No desktop-only recovery steps remain |
| F4 | Known limitations and glossary | `PROJECT_STATE.md` § Known Gaps; current web source paths | Limitations describe web reality |
| F4 | Navigation and internal links | `docs/UserGuide.md`; `docs/_config.yml`; sibling docs pages | Anchors and Jekyll page links resolve |
| F4 | Screenshot placeholders | Existing `docs/images/` references | Each retained image has the exact replacement marker |
| N1 | All factual claims | All source paths in this table | Manual checklist completed |
| N2 | Jekyll Markdown and paths | `docs/UserGuide.md`; `docs/_config.yml`; `docs/images/` | `git diff --check` and link/path audit pass |
| E1 | No JavaFX/SQLite/JAR instructions | `docs/UserGuide.md` | Targeted `rg` review has no user-facing matches |
| E2 | No mocked/email-only auth claims | `docs/UserGuide.md` | Targeted review passes |
| E3 | Planned features explicitly omitted or marked | `PROJECT_STATE.md` W-AI-AGENT | No unqualified Agent AI claim |
| E4 | Anchors and image paths | `docs/UserGuide.md`; `docs/images/` | Audit verifies every local target |
| E5 | Unimplemented features | `PROJECT_STATE.md` § Upcoming | Manual review confirms omission/qualification |
| E6 | Production registration secrets | `docs/runbooks/dev-environment.md` | No secret values appear |
| E7 | Deployment URL | `docs/runbooks/dev-environment.md` | URL matches verified evidence |
| E8 | Retained desktop screenshots | `docs/UserGuide.md`; `docs/images/` | Every image has marker in caption or following prose |

## Verification gates

- Per documentation task: `git diff --check` and a focused source cross-check for the sections changed.
- Before finishing: spec lint with `--approved`, plan lint with `--approved`, targeted stale-term search, Markdown anchor/image/link audit, and manual traceability checklist review.

### T1: Inventory current web user experience

**Delivers:** Completed source-to-section checklist above, with any discovered implementation facts recorded in the plan before guide prose changes. **Satisfies:** N1.

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `workflow/plans/2026-10-23-user-guide-web.md` (Traceability table) | Fill the checklist with exact route, label, limitation, and feature evidence while inspecting the repository. |

**Checklist**

- [x] 1. Inspect frontend route trees, role navigation, API wrappers, and AI components named in the Traceability table.
- [x] 2. Inspect backend auth, Guest, Host, and Agent controllers plus the runbook and state entries named in the table.
- [x] 3. Correct the checklist if any path, route, label, or feature claim is not present; do not invent replacement behavior.
- [x] 4. Run `git diff --check`.

### T2: Rewrite guide structure and quick start

**Delivers:** Browser-first introduction, access/setup, authentication, navigation, and cross-role rules. **Satisfies:** F1, F2, F4, N2.

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `docs/UserGuide.md` | Replace the desktop introduction, Quick start, sign-in/registration, sample-account, shared-rules, and table-of-contents content with current web-app prose. |

**Checklist**

- [x] 1. Replace JavaFX/SQLite/JAR prerequisites with the verified Vercel entry point and optional local developer path.
- [x] 2. Describe Firebase email/password login, registration, password reset, role routing, administrator-provided privileged codes, and suspended accounts.
- [x] 3. Remove or rewrite sample-account and persistence claims that depend on the desktop mock database; retain only verified web test-account guidance if available.
- [x] 4. Update shared wallet, booking, dispute, and messaging rules to match the web implementation.
- [x] 5. Add the exact screenshot placeholder marker to retained images in the changed sections.
- [x] 6. Run `git diff --check` and the focused stale-term search.

### T3: Rewrite role workflows and feature reference

**Delivers:** Current Guest, Host, and Support Agent workflows, AI assistant guidance, FAQ, limitations, troubleshooting, glossary, and link structure. **Satisfies:** F3, F4, N1.

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `docs/UserGuide.md` | Rewrite role sections and closing reference sections from desktop behavior to current web routes and controls. |

**Checklist**

- [x] 1. Document Guest Search, Trips, Messages, Wallet, booking/review/dispute flows, and Guest AI search behavior.
- [x] 2. Document Host Listings, listing detail/edit, calendar, Requests, Messages, Wallet, and Host AI Evaluate/Generate/Apply behavior.
- [x] 3. Document Agent Disputes, Accounts, Audit Log, and Categories without adding Agent AI or unsupported artifact-only controls.
- [x] 4. Update feature summary, FAQ, known limitations, troubleshooting, and glossary for the web app.
- [x] 5. Preserve or correct Jekyll links to sibling documentation pages and retained image references.
- [x] 6. Add screenshot placeholder markers to every retained role-section image.
- [x] 7. Run `git diff --check` and the focused stale-term search.

### T4: Validate guide

**Delivers:** A source-backed web guide ready for later screenshot replacement. **Satisfies:** F1, F2, F3, F4, N1, N2, E1–E8.

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `workflow/plans/2026-10-23-user-guide-web.md` (task status/checklists) | Tick completed plan steps and record verification results. |

**Checklist**

- [x] 1. Run `node .claude/skills/writing-specs-lite/scripts/lint.mjs spec workflow/specs/2026-10-23-user-guide-web-design.md --approved`.
- [x] 2. Run `node .claude/skills/writing-specs-lite/scripts/lint.mjs plan workflow/plans/2026-10-23-user-guide-web.md --approved`.
- [x] 3. Run `git diff --check`.
- [x] 4. Audit every Markdown link, internal anchor, Jekyll page link, and `images/...` reference in `docs/UserGuide.md`.
- [x] 5. Run `rg -n -i 'javafx|sqlite|snoozeshare-mock|gradlew|shadowJar|single-user|mocked authentication|email only' docs/UserGuide.md` and inspect every match.
- [x] 6. Check every retained image's caption or immediately following prose for `Screenshot placeholder — replace with the current web UI screenshot.`.
- [x] 7. Complete the Traceability table review against the current repository.
- [ ] 8. GATE: Update plan task statuses and ask Trevor for Gate 3 review; do not push or open a PR before approval.

## Traceability

| Requirement | Task(s) | Verified by |
|---|---|---|
| F1 | T1, T2, T4 | Source checklist; browser quick-start review; link audit |
| F2 | T1, T2, T4 | Auth source review; registration-secret review; stale-term search |
| F3 | T1, T3, T4 | Role source checklist; workflow review; AI feature review |
| F4 | T2, T3, T4 | Closing-sections review; anchor/link/image audit |
| N1 | T1, T2, T3, T4 | Source-to-section checklist and manual claim review |
| N2 | T2, T3, T4 | `git diff --check`; Markdown/Jekyll link and image audit |
| E1 | T2, T4 | Targeted stale desktop-term search |
| E2 | T2, T4 | Authentication wording review and stale-term search |
| E3 | T3, T4 | `PROJECT_STATE.md` W-AI-AGENT comparison |
| E4 | T2, T3, T4 | Anchor and image-path audit |
| E5 | T3, T4 | Planned-feature review |
| E6 | T2, T4 | Runbook secret comparison |
| E7 | T2, T4 | Deployment URL comparison |
| E8 | T2, T3, T4 | Every retained image caption/adjacent-prose review |

### Close-out

- [ ] Every Traceability row is verified and every task is marked Done.
- [ ] The spec index and plan statuses reflect reality; any deviation is recorded before handoff.
- [ ] Run the final `git diff --check` and report the documentation checks performed.
- [ ] GATE: Ask Trevor to review the completed branch; do not push, open a PR, or merge before approval.
