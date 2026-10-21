# W-HOST Backend Integration Design

- **Status:** Draft
- **Risk tier:** High - this crosses authenticated role authorization, host tenancy, booking state, messaging, and wallet operations.
- **Workstream:** W-HOST-API | **Plan:** —
- **Constraints consulted:** C67, C70, C73, C75, C78, C79, C89, C92; § Known Gaps (mocked top-up/withdraw, backend authorization authoritative); § Conventions (audit, money, service authorization, Testcontainers)
- **Reviewed by lenses:** Implementer, Tester / QA, Maintainer, and Attacker; second-round contract findings addressed (idempotency scope and bounded paging)

## 1. Problem

The completed W-HOST UI is fixture-backed. `HostFixtureProvider`, `hostFixtures.ts`, and the Host panels provide visual states and local presentation interactions, but no Host page calls the backend. The backend already contains W-DOMAIN entities, repositories, services, request DTOs, and response DTOs; only `AuthController` currently exposes REST endpoints.

The desired result is an authenticated `/api/host/**` boundary that derives the Host identity from the verified Firebase principal, exposes the existing W-DOMAIN capabilities, and lets the existing Host screens replace fixtures without changing their visual contract or claiming unsupported financial success.

## 2. Goals

The complete requirement set is in [requirements](2026-10-22-w-host-backend-integration-design/requirements.md).

## 3. Non-goals

- Changes to existing domain tables - W-DOMAIN owns those tables; one dedicated wallet idempotency table is added by this phase.
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
- [Data model](2026-10-22-w-host-backend-integration-design/data-model.md) - durable wallet idempotency table and exact response value shapes.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|
| — | None; endpoint paths, limits, error mappings, identity resolution, and mutation guarantees are selected below. | — | — |

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Use `/api/host/**` controllers over the existing W-DOMAIN services and DTOs; do not add a second Host service layer. | Keeps authorization at the REST boundary and preserves the service/domain ownership already implemented; a frontend-only direct database or fixture continuation is rejected. | Project owner request to continue W-HOST after W-DOMAIN |
| SD2 | Resolve the domain `User` UUID from the verified Firebase UID at the controller boundary; clients never send an acting host ID. | Prevents identity spoofing and follows C79/C92; accepting a client host ID is rejected. | Project owner request; existing W-AUTH contract |
| SD3 | Keep the current fixture adapter as a test adapter, while production Host providers use the API client. | Preserves deterministic empty/loading/error and visual tests while removing fixture data from authenticated production routes. | Project owner request; current W-HOST design |
| SD4 | Use one safe error envelope, `code`, `message`, `requestId`, matching `AuthErrorResponse`. | Gives frontend stable codes and correlation without leaking credentials, tokens, SQL, or entity internals; ad-hoc strings are rejected. | Existing C96 convention |
| SD5 | Use separate `/messages/bookings/**` and `/messages/tickets/**` routes and DTOs; return 404 for absent and non-owned resources. | Their services take different identifiers/channels; a discriminator route can call the wrong authorization path. Concealing other tenants' resource existence follows least disclosure. | Project owner approved recommended defaults |
| SD6 | Use month-based inclusive calendar JSON, page size default 50/max 100, and the exact stable error matrix in `api.md`. | Makes backend/client contract deterministic and bounds response sizes; unbounded arrays and alternate status outcomes are rejected. | Project owner approved recommended defaults |
| SD7 | Wallet mutations require persistent idempotency keys; booking decisions lock the booking row; availability mutations use SERIALIZABLE transactions; message events publish after transaction commit. | Prevents duplicate ledger entries, competing decisions, availability races, and events for rolled-back writes. UI-only retry protection is insufficient. | Project owner approved recommended defaults |
| SD8 | Resolve the Firebase UID through `AuthUserRepository`, then verify the same ID/UID/role/status in the domain `User`; return 409 on inconsistent rows. | Auth and domain models share the users table but separate repositories; checking both detects drift without trusting client identity. | Project owner approved recommended defaults |
| SD9 | Keep established route style from this spec, with `PUT .../status` and `/wallet/top-up`; this Host API spec supersedes earlier illustrative route spellings in the architecture design. | A single canonical path set avoids client ambiguity; the architecture document is an outline and its legacy spelling is superseded for this workstream. | Project owner approved recommended defaults |
| SD10 | Scope wallet idempotency keys per host across all wallet operations; hash operation and canonical amount together. | Cross-operation reuse must conflict, not create a second ledger entry; per-operation uniqueness is rejected. | Project owner approved defaults; second-round review exposed the schema mismatch |
| SD11 | Every collection, message-history, and wallet-statement page uses owner-scoped database pagination with deterministic ordering and at most `size + 1` rows fetched. | The existing unpaged service lists cannot safely meet a bounded-page contract by slicing in memory; unbounded reads are rejected. | Project owner approved bounded paging default; second-round review identified the missing query contract |
