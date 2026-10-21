# W-HOST Backend Integration Design

- **Status:** Draft
- **Risk tier:** High - this crosses authenticated role authorization, host tenancy, booking state, messaging, and wallet operations.
- **Workstream:** W-HOST-API | **Plan:** —
- **Constraints consulted:** C67, C70, C73, C75, C78, C79, C89, C92; § Known Gaps (mocked top-up/withdraw, backend authorization authoritative); § Conventions (audit, money, service authorization, Testcontainers)
- **Reviewed by lenses:** none yet

## 1. Problem

The completed W-HOST UI is fixture-backed. `HostFixtureProvider`, `hostFixtures.ts`, and the Host panels provide visual states and local presentation interactions, but no Host page calls the backend. The backend already contains W-DOMAIN entities, repositories, services, request DTOs, and response DTOs; only `AuthController` currently exposes REST endpoints.

The desired result is an authenticated `/api/host/**` boundary that derives the Host identity from the verified Firebase principal, exposes the existing W-DOMAIN capabilities, and lets the existing Host screens replace fixtures without changing their visual contract or claiming unsupported financial success.

## 2. Goals

The complete requirement set is in [requirements](2026-10-22-w-host-backend-integration-design/requirements.md).

## 3. Non-goals

- New persistence tables or migrations - W-DOMAIN already owns the domain schema (C73, C74 decision resolved by W-DOMAIN).
- Payment-provider integration - top-up and withdrawal remain mocked by project decision (C75 and § Known Gaps).
- Host or Agent AI features - unresolved Q6 blocks those workstreams.
- Polling, WebSockets, or background synchronization - the first integration is request/response; polling remains the later platform convention (C78).
- Visual redesign of the completed Host UI - preserve the approved Fall Light contract.

## 4. Components

- [Requirements](2026-10-22-w-host-backend-integration-design/requirements.md) - observable backend and frontend outcomes.
- [API](2026-10-22-w-host-backend-integration-design/api.md) - Host endpoint contracts, security, errors, and transaction boundaries.
- [UI](2026-10-22-w-host-backend-integration-design/ui.md) - fixture-to-API state and mutation behavior for each Host screen.
- [Conventions](2026-10-22-w-host-backend-integration-design/conventions.md) - applied security, identity, money, error, and testing rules.
- [Edge cases](2026-10-22-w-host-backend-integration-design/edge-cases.md) - security, state, concurrency, and failure cases.
- [Implementation order](2026-10-22-w-host-backend-integration-design/implementation-order.md) - dependency-ordered work.
- No data-model component: this phase adds no tables, columns, migrations, or seed changes.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|
| — | None; endpoint grouping and response/error conventions are selected below. | — | — |

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Use `/api/host/**` controllers over the existing W-DOMAIN services and DTOs; do not add a second Host service layer. | Keeps authorization at the REST boundary and preserves the service/domain ownership already implemented; a frontend-only direct database or fixture continuation is rejected. | Project owner request to continue W-HOST after W-DOMAIN |
| SD2 | Resolve the domain `User` UUID from the verified Firebase UID at the controller boundary; clients never send an acting host ID. | Prevents identity spoofing and follows C79/C92; accepting a client host ID is rejected. | Project owner request; existing W-AUTH contract |
| SD3 | Keep the current fixture adapter as a test adapter, while production Host providers use the API client. | Preserves deterministic empty/loading/error and visual tests while removing fixture data from authenticated production routes. | Project owner request; current W-HOST design |
| SD4 | Use one safe error envelope, `code`, `message`, `requestId`, matching `AuthErrorResponse`. | Gives frontend stable codes and correlation without leaking credentials, tokens, SQL, or entity internals; ad-hoc strings are rejected. | Existing C96 convention |
