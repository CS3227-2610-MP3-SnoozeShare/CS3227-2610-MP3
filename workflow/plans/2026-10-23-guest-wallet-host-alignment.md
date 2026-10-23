# Guest Wallet / Host Wallet Alignment Implementation Plan

Spec: [Guest Wallet / Host Wallet Alignment](../specs/2026-10-23-guest-wallet-host-alignment-design.md)

- **Status:** Approved (Trevor, 2026-10-23)

> **For agents:** tick `- [ ]` -> `- [x]` as each step finishes.

## Scope

**Goal:** Make the Guest wallet page and header wallet balance follow the established Host wallet patterns without changing Host behavior or API contracts.

**In scope:** Guest shell wallet loading; header balance label; Guest wallet loading/error/empty/layout styling; regression tests.

**Non-goals:** Host changes; shared wallet extraction; backend changes; wallet calculations; screenshots; user-guide changes.

**Risk tier:** Low - frontend-only presentation and existing API wiring.

## Task order

| Task | Depends on | Can run concurrently with | Wave | Status |
|---|---|---|---|---|
| T1 Add failing Guest header and wallet alignment tests | - | - | 1 | Done |
| T2 Load Guest balance in the role shell | T1 | - | 2 | Done |
| T3 Align Guest wallet page presentation | T1 | - | 2 | Done |
| T4 Run focused and full frontend verification | T2, T3 | - | 3 | Done |

Shared-file hazards: T2 edits Guest shell wiring and T3 edits the Guest wallet page; tests in T1 must be completed before either implementation task.

## Traceability

| Requirement | Task(s) | Verified by |
|---|---|---|
| Header shows Guest balance in Host format | T1, T2, T4 | Guest shell tests; frontend test suite |
| Header loading and failure states are explicit | T1, T2, T4 | Guest shell tests |
| Guest wallet matches Host layout conventions | T1, T3, T4 | Guest wallet tests; frontend test suite |
| Guest API behavior and labels remain unchanged | T1, T3, T4 | Existing Guest wallet tests plus focused mutation tests |
| Host behavior remains unchanged | T4 | Host wallet and Host route tests |

## Verification gates

- T1 must show the new tests failing for the missing behavior before production edits.
- T2/T3 run their focused frontend tests after implementation.
- T4 runs `npm test`, `npm run lint`, and `npm run build` from `frontend/` with the repository's documented placeholder public configuration if required.

### T1: Add failing Guest header and wallet alignment tests

**Delivers:** RED tests for the requested behavior. **Depends on:** none.

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/app/guest/wallet/page.test.tsx` | Add assertions for Host-aligned balance/action/statement structure and Guest mutation behavior that must remain intact. |
| Create | `frontend/src/app/guest/GuestRoutes.test.tsx` | Test the Guest shell loads the Guest wallet balance, formats it as the Host header does, and exposes loading/failure labels. |

**Checklist**

- [x] 1. Add the Guest route-shell test with a mocked Guest wallet response and assert `Wallet: $250.50`.
- [x] 2. Add loading and failed-load assertions for `—` and `unavailable`.
- [x] 3. Add Guest wallet page assertions for the balance/action row and statement structure that distinguish the Host-aligned presentation.
- [x] 4. Run the focused tests and confirm they fail because Guest shell/page behavior is not yet implemented.

### T2: Load Guest balance in the role shell

**Delivers:** Guest header wallet label with loading and failure states. **Depends on:** T1.

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `frontend/src/components/guest/GuestRoleShell.tsx` | Load `getWallet()` once for the Guest shell and pass a Host-format `walletLabel` to `RoleShell`; preserve auth and active-path behavior. |
| Modify | `frontend/src/app/guest/layout.tsx` | Wrap the Guest route in `GuestRoleShell` instead of passing children directly to the generic shell. |
| Modify | `frontend/src/components/role/RoleShell.tsx` | Apply Host-style viewport containment to the Guest wallet route so its statement scrolls within the workspace. |

**Checklist**

- [x] 1. Add the minimal Guest shell state and `getWallet()` loading path.
- [x] 2. Format the balance with two decimal places and preserve the Guest currency/API response.
- [x] 3. Render `—` during loading and `unavailable` on failure.
- [x] 4. Run the T1 Guest shell tests and confirm they pass.

### T3: Align Guest wallet page presentation

**Delivers:** Guest wallet UI aligned with Host’s balance/actions row, statement container, spacing, and state treatment. **Depends on:** T1.

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/app/guest/wallet/page.tsx` | Match Host wallet layout conventions while retaining Guest API calls, action labels, transaction mapping, and validation. |
| Modify | `frontend/src/app/guest/wallet/page.test.tsx` | Cover aligned structure, existing empty state, and top-up/withdraw behavior. |

**Checklist**

- [x] 1. Make the loading, error, balance/actions row, transaction statement, and empty state follow Host classes and spacing.
- [x] 2. Preserve Guest transaction labels and four-column Guest statement semantics.
- [x] 3. Preserve Guest modal validation, presets, full-balance withdrawal, and API error handling.
- [x] 4. Run focused Guest wallet tests and confirm they pass.
- [x] 5. Map canonical backend `ESCROW_HOLD` to the same warning color as Host while retaining the legacy `BOOKING_HOLD` compatibility case.

### T4: Run focused and full frontend verification

**Delivers:** Verified Guest/Host wallet alignment with no regressions. **Depends on:** T2, T3.

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `workflow/project-state/branch-states/W-GUEST-UI-ALIGN.md` | Record progress, tests, changes, and final handoff state. |

**Checklist**

- [x] 1. Run focused Guest wallet and Guest route tests.
- [x] 2. Run the full `frontend` test suite.
- [x] 3. Run `npm run lint` from `frontend/`.
- [x] 4. Run `npm run build` from `frontend/` with documented placeholder public configuration if required.
- [x] 5. Inspect the diff for Host behavior changes and unintended shared-shell effects.
- [x] 6. Update the branch state with test results and deviations.
- [x] 7. GATE: Ask Trevor to review the branch; do not commit, push, or open a PR.

### Close-out

- [x] All tasks are marked Done and the branch state has no live session row.
- [x] The branch state is set to `In review` and includes the verification results.
- [x] GATE: Ask Trevor for final branch approval; do not commit, push, or open a PR.
