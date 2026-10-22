# Data Model

No new table and no migration. The gateway reuses `audit_log` (created by V003) through `AuditService.record` and adds two `AuditAction` constants. It also keeps process-local runtime state.

## Entities

Persisted: at most one `audit_log` row per call. Layout by column:

| Column | Value | Null | Rule |
|---|---|---|---|
| `actor_user_id` | `call.userId()` | no | the human user, not the SYSTEM user |
| `actor_name`, `subject_name` | filled by `AuditServiceImpl.record` from `users` | yes | the same display names stored for every audit row; accepted (SD20) |
| `action_type` | `AI_CALL` or `AI_CALL_REJECTED` | no | constant name |
| `entity_type` | `AI_CALL` | no | fixed text |
| `entity_id` | the call's `callId` | no | returned in `LlmResult.callId` |
| `before_state` | null | yes | an AI call is not a state change |
| `after_state` | the metadata line below | no | under 400 characters by construction: feature id at most 40, model at most 100 |
| `wallet_adjustment`, `balance_after` | null | yes | an AI call never moves money |
| `reason` | `OK` or the error code of api.md | no | |
| `subject_user_id`, `booking_id`, `ticket_id` | from `AuditContext` | yes | set by the calling feature; foreign keys apply |
| `timestamp` | `Clock.instant()` after the outcome is known | no | |

Metadata line, fixed order, separator `;`, no spaces:
`feature=<id>;model=<model or ->;tokensIn=<n>;tokensOut=<n>;latencyMs=<n>;attempts=<n>;flagged=<true|false>;truncated=<true|false>;promptHash=<64 hex or ->`

`promptHash` is the SHA-256, lower-case hex, of the UTF-8 bytes of the JSON array of the assembled upstream messages, each message an object with keys `role` then `content`, no whitespace, written by one fixed Jackson writer; it is for correlation and need not be stable across library upgrades. It is `-` only for BLOCKED_INPUT; every row written for an outcome at step 8 or later (DAILY_CAP included) carries the hash. The hash of a short, mostly redacted prompt can be guessed offline by anyone who can read `audit_log` (Agents); this is accepted. `model` is `-` and tokens are 0 when no request was sent, and `mock` in mock mode; tokens are 0 when `usage` is missing. No prompt text, response text or upstream error text is ever written (SD2, N6).

Cost per audited call: one count query for the user, one for all users, `AuditServiceImpl` user lookups, and the insert, which Spring Data JPA precedes with a select because the id is assigned. The existing indexes `idx_audit_actor` and `idx_audit_timestamp` serve the counts at the capped volume (at most `global-daily-cap` rows a day).

Runtime state (process-local, lost on restart, C69):

| State | Bound | Eviction |
|---|---|---|
| Scheduler queue and the last 60 s of start times | `max-queue` jobs; at most `max-rpm` timestamps | Timestamps leave the window on each scheduling pass |
| Per-user per-feature call times; per-user in-flight count; per-user and global reserved-call counts, including calls whose audit row failed | one entry per active user and feature | Removed when empty; the failed-audit counts are cleared at 00:00 UTC |
| R-row coalescing times | one per user and code | Removed after 10 minutes |
| Circuit breaker (count, distinct users, open-until, probe flag); budget block (until instant) | constant size | Reset on success or when the instant passes |

## Keys and relationships

| From | To | Cardinality | On delete | Ownership / tenancy scope |
|---|---|---|---|---|
| `audit_log.actor_user_id` | `users.id` | many to one | restrict (existing FK) | the calling user; the per-user cap counts only that user's rows |
| `audit_log.ticket_id`, `booking_id`, `subject_user_id` | tickets, bookings, users | many to one, optional | restrict (existing FKs) | whatever the calling feature passes; the gateway does not verify it |

## Lifecycle

An audit row is written once and never updated. There is no state machine.

## Invariants

- One `run` writes at most one row; ROLE_DENIED, USER_THROTTLED, DISABLED and AUDIT_FAILED write none; an R row may be skipped by the 10-minute coalescing rule.
- The per-user count is that user's `AI_CALL` rows with `timestamp` at or after 00:00 UTC today plus that user's reserved calls; the global count is the same over all users. `AI_CALL_REJECTED` rows never count. A call counts once however many attempts it made.
- No audit row contains prompt, response or upstream error text.
- Reading the reservation counts, counting rows and reserving happen under one lock, and a release happens under the same lock after the row is written, so a cap is never undercounted by a call that is between its insert and its release. A reservation is released on every path out of `run`, except that a sent call whose audit row failed stays counted until 00:00 UTC.

## Migration and rollback

- Version: none. `action_type` is `VARCHAR(50)` with no CHECK constraint and `entity_type` is free text.
- Existing rows: none touched. Regenerate committed data: no; the desktop mock database is not affected.
- If it fails halfway: not applicable. Rollback removes the two enum constants and deletes the rows that carry them (`DELETE FROM audit_log WHERE action_type IN ('AI_CALL','AI_CALL_REJECTED')`).

## Architecture areas changed

- New area, proposed `4.11 AI gateway (backend ai/)`.
- The audit area (desktop `4.8`; the web audit text lands with W-DOMAIN's fold) - two new actions; the Agent audit filter lists enum values, so W-AGENT's page shows them.
- `4.9 Web platform skeleton` - the `snoozeshare.ai.*` keys, the `ProdEnvironmentGuard` additions, and `SNOOZESHARE_AI_ENABLED` in `render.yaml`.
