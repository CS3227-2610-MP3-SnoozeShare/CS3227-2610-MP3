# Branch State — W-GUEST-UI-ALIGN

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `INT`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-GUEST-UI-ALIGN (from `development` at `73cbebe`)
- **Base:** 73cbebe
- **Owner:** Nathan
- **Status:** Approved
- **Approved:** Trevor 2026-10-23
- **Last updated:** 2026-10-23 by Codex — Guest wallet alignment approved

---

**Scope.** Visual alignment of all Guest frontend pages and components to match Host design
patterns: skeleton loading, card-wrapped states, design tokens, shared Modal/Table components,
Host-style message panel. No functional changes.

---

## 1. Orientation

No change.

---

## 2. How to Resume

**Setup** — no change from `PROJECT_STATE.md`.

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|

**Remaining** — none; awaiting branch review.

---

## 3. Workstreams

No new workstream. This is a small visual refactor of the existing W-GUEST pages.

---

## 4. Architecture

Guest wallet behavior will use the existing Guest API through a Guest-specific shell loader; the Host wallet implementation remains the presentation reference and is not extracted into a new shared component.

---

## 5. Conventions

No change: follows existing Host design patterns already established.

---

## 6. Known Gaps

No change: no new gaps introduced.

---

## 7. Needs a Human

No change.

---

## 8. Decisions

No change.

---

## 9. Deviations

| ID | What diverged | Why | Action |
|---|---|---|---|
| D~1 | The backend uses `ESCROW_HOLD`, while the earlier Guest UI mapping only recognized `BOOKING_HOLD`. | The Guest page fell through to the green default for real escrow rows. | Treat both names as warning-colored; regression test covers the canonical backend value. |

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-23 | Align Guest UI to Host design: skeleton loading states, card-wrapped error/empty states, design token colors (text-destructive, var(--success), var(--surface-inset)), rounded-lg buttons, shared Modal for CancelDialog/ReviewDialog/TicketFilingDialog, shared Table for wallet transactions, Host-style bordered message panel and conversation list. 12 files, all 330 tests pass. | W-GUEST | `cb634c6` |
| 2026-10-23 | Started follow-up to make the Guest wallet page match the Host wallet presentation and load the Guest balance into the header wallet pill. | W-GUEST-UI-ALIGN | resolved |
| 2026-10-23 | Added Guest header wallet loading (`$X.XX`, loading dash, unavailable fallback), Host-style Guest wallet viewport/layout, and regression tests. Frontend verification: 334 tests, lint, and production build pass with placeholder public environment values. | W-GUEST-UI-ALIGN | uncommitted working-tree changes |
| 2026-10-23 | Corrected Guest transaction badge coloring for canonical backend `ESCROW_HOLD`; added a regression test. Verification now passes with 335 frontend tests, lint, and production build. | W-GUEST-UI-ALIGN | uncommitted working-tree changes |
