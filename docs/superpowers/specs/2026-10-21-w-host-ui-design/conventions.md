# Conventions

Cite existing PROJECT_STATE § Conventions by ID; list only rules new or applied here.

| Rule | Applies to | Source |
|---|---|---|
| Use CSS variables for the artifact tokens and consume them through shared classes/components; do not duplicate hex values in page components | `frontend/src/app`, `frontend/src/components`, `frontend/src/app/globals.css` | New in this spec; exact values are in N1 |
| Keep all Host routes under `frontend/src/app/host` and protect the layout with the existing `RouteGuard role="HOST"`; `/host` redirects to `/host/listings` | Host route tree | C79, SD4, and existing W-AUTH implementation |
| The authenticated Host shell receives its wallet badge from the Host fixture provider; `RoleNavbar` accepts an optional wallet label and preserves the current default for Guest and Agent | Shared role shell | New in this spec; avoids page-local wallet coupling |
| Keep shared shell, status, card, form, table, modal, and calendar primitives in shared frontend component locations; Host-specific composition belongs under `frontend/src/components/host` | Frontend component structure | C93 and new in this spec |
| Fixture data is read through one Host fixture adapter and typed presentation records; page components do not define duplicate listing, request, message, or wallet records. The adapter defaults to populated and tests replace its state provider | Host UI data | SD2, SD5; N5 |
| Presentation-only controls use the exact local-only outcomes in F9 and never call an invented endpoint | Host mutations | SD3; C74 |
| Bundled fixture records are synthetic, unowned, and never imply authorization or tenant ownership | Host fixture security | SD5; C79 and Known Gaps |
| Use visible text labels alongside status color for active/inactive, available/booked/blocked, pending/confirmed/rejected, and resolved/open states | Host statuses and calendar | N3; artifact Design System |
| Frontend tests use Vitest and Testing Library; visual parity uses 1680×945 and responsive checks use 1280×800 and 768×1024 | Host UI verification | Existing frontend test convention; SD8, N4 |
