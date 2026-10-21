# W-HOST Fall Light UI Design

- **Status:** Approved (project owner, 2026-10-21)
- **Risk tier:** Medium - role-gated screens display synthetic wallet and booking information, but this slice adds no API, persistence, or domain mutation.
- **Workstream:** W-HOST | **Plan:** —
- **Constraints consulted:** C73 (web DTO boundary), C79 (role namespaces), C93 (existing role navbar and empty shells), C~1 in the W-HOST branch state (Claude artifact is the visual source of truth); W-DOMAIN ownership of domain persistence and wallet operations; client-side route guards are not the backend authorization boundary.
- **Reviewed by lenses:** Implementer, Tester, Attacker (two rounds; findings resolved)

## 1. Problem

The current Host web area contains only an empty protected landing page. `frontend/src/app/host/layout.tsx` provides the `HOST` route guard and `RoleShell`, while `frontend/src/components/role/RoleNavbar.tsx` renders Host labels but sends every tab to `/host`. `frontend/src/app/globals.css` already contains some Fall Light variables, but the Host screen composition, route wiring, fixture states, and artifact-specific component treatment are not implemented.

The desired result is a fixture-backed Host UI that matches the Claude artifact's Fall Light design system and screen composition exactly enough for side-by-side visual review. The slice covers Host navigation, listings, listing detail, listing editing, booking calendar, booking requests, messages, and wallet presentation without assuming W-DOMAIN's persistence choice or API contracts.

## 2. Goals

The full functional and non-functional requirements are in [requirements.md](2026-10-21-w-host-ui-design/requirements.md).

## 3. Non-goals

- Backend Host controllers, services, repositories, migrations, and API DTOs - W-DOMAIN owns the domain and persistence contracts, and C74 leaves the persistence approach open.
- Real listing, calendar, booking-request, wallet, or messaging mutations - this slice has no domain API to call.
- Replacing Firebase authentication or backend role authorization - W-AUTH is complete; this slice consumes its existing `RouteGuard` and `AuthProvider`.
- New image hosting, payment integration, polling, or AI features - outside this UI-only slice and the W-HOST scope.
- Changes to Guest or Agent page composition - the artifact's Host screens are the requested target.

## 4. Components

- [Requirements](2026-10-21-w-host-ui-design/requirements.md) - observable visual, routing, fixture, accessibility, and isolation requirements.
- [UI](2026-10-21-w-host-ui-design/ui.md) - exact Host shell, page layouts, controls, copy, and screen states.
- [Conventions](2026-10-21-w-host-ui-design/conventions.md) - applied Fall Light tokens, component, route, and fixture rules.
- [Edge cases](2026-10-21-w-host-ui-design/edge-cases.md) - empty, loading, error, permission, viewport, and no-domain-behaviour cases.
- [Implementation order](2026-10-21-w-host-ui-design/implementation-order.md) - dependency-ordered implementation tasks.
- No data model changes: fixture types are presentation-only and are not persisted.
- No API changes: the UI uses an in-memory fixture adapter until W-DOMAIN publishes its contracts.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

None. Domain API shape remains a later W-DOMAIN decision, not an open question for this UI slice.

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Use the supplied Claude artifact as the visual authority and reproduce its Fall Light tokens and Host page composition in the existing Next.js/Tailwind/Shadcn stack | The user explicitly required exact artifact styling; replacing the frontend stack would add unnecessary coupling and would not improve visual parity | Project owner |
| SD2 | Build Host pages against an in-memory fixture adapter with no final API types | Allows visual work to proceed independently of W-DOMAIN; hard-coding page-local records or inventing REST endpoints would make later integration harder | Project owner, via approved design |
| SD3 | Render domain-action controls as presentation-only controls until W-DOMAIN is available; navigation and local preview state are allowed, but no control reports server success | Preserves the artifact's visible interaction affordances without inventing domain behaviour | Project owner, via approved design |
| SD4 | `/host` redirects to `/host/listings`; the shared role navbar gains explicit per-role route maps while Guest and Agent links retain their current landing paths | The artifact's first Host tab is Listings and the existing navbar otherwise sends every tab to its landing path | Project owner, via approved design |
| SD5 | The fixture adapter defaults to `populated`; tests replace its state provider with `empty`, `loading`, or `error`; all fixture records are synthetic and may be seen by any authenticated Host | Makes state selection deterministic without query-string production behaviour and avoids treating bundled data as tenant data | Project owner, via approved design |
| SD6 | Known fixture route IDs are `lakeside-cabin`, `downtown-loft`, `garden-studio`, and `hilltop-house`; unknown or malformed IDs render the exact not-found state | Gives dynamic routes deterministic fixture lookup and avoids inventing identity semantics | Project owner, via approved design |
| SD7 | Form validation is local and exact: required text fields reject blank values, numeric fields use the ranges in `ui.md`, and invalid calendar ranges render the messages listed there | Allows visual forms to behave predictably without importing W-DOMAIN validation | Project owner, via approved design |
| SD8 | Visual parity is checked at 1680×945; responsive usability is checked at 1280×800 and 768×1024 | Separates artifact screenshot comparison from the project's desktop and tablet layout requirements | Project owner, via approved design |
