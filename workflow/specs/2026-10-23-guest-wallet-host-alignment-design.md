# Guest Wallet / Host Wallet Alignment Design

- **Status:** Approved (Trevor, 2026-10-23)
- **Approach:** Guest-specific shell loader plus Host-aligned wallet presentation

## Intent

The Guest wallet currently has its own page layout and the Guest header shows only a static
`Wallet` pill. The Host workspace loads a balance into the header and presents the wallet through
`HostWalletPanel`. Align the Guest experience with that established Host pattern while preserving
the Guest API and Guest-specific transaction labels.

## Goals

- Load the authenticated Guest wallet balance for the header and show it using the same display
  format as Host (`Wallet: $X.XX`).
- Keep the header wallet pill display-only, matching current Host behavior; the Wallet tab remains
  the navigation control.
- Make the Guest wallet page use the Host wallet's balance/actions row, statement container,
  spacing, colors, buttons, modal treatment, and empty/loading/error states.
- Preserve Guest behavior: Guest API calls, Guest transaction action labels, Guest currency data,
  and existing top-up/withdraw validation and error messages.
- Add regression tests before production changes and run the focused plus full frontend suites.

## Non-goals

- Extracting a shared wallet panel or changing Host wallet behavior.
- Changing backend contracts, wallet calculations, or authentication.
- Replacing screenshots or updating the user guide.

## Chosen approach

Add a small Guest shell balance loader that calls the existing Guest wallet endpoint and passes a
formatted `walletLabel` to `RoleNavbar`. Update only the Guest wallet page and its tests to match
the Host presentation patterns. A shared wallet component is deferred because the Guest and Host
statement DTOs have different action/related-item semantics, and the request is limited to
alignment rather than a cross-role refactor.

## Acceptance checks

- Guest role routes render a header wallet label with the loaded balance, `—` while loading, and
  `unavailable` after a failed balance load.
- The Guest wallet page has the Host-aligned balance/actions row and scrollable statement layout.
- Existing Guest top-up, withdrawal, empty state, and API error tests remain green.
- No Host files require behavior changes.

## Implementation order

| ID | Task | Satisfies |
|---|---|---|
| T1 | Add failing Guest header and wallet alignment tests | Header behavior; page structure; preserved Guest behavior |
| T2 | Load Guest balance in the role shell | Header balance and loading/failure states |
| T3 | Align Guest wallet page presentation | Host-aligned wallet UI and preserved Guest semantics |
| T4 | Run focused and full frontend verification | Regression safety and branch handoff |
