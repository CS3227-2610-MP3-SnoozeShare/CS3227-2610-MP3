# Data Model

No domain data is added. The only database object is Flyway's own history table.

## Entities
| Entity | Attribute | Type | Null | Default | Range / rule |
|---|---|---|---|---|---|
| `flyway_schema_history` (Flyway-managed) | all columns | Flyway standard | per Flyway | - | written only by Flyway; never edited by hand |

## Keys and relationships
None added. Tenancy scope: not applicable (no user data).

## Lifecycle
None.

## Invariants
- `backend/src/main/resources/db/migration/` holds `V001__baseline.sql`, whose only content is a SQL comment; it exists so Flyway runs, creates its history table and proves the migration path.
- Migration numbering continues the desktop's three-digit style (`V002__...`); W-DOMAIN owns every later migration. The desktop's `V001-V008` stay SQLite-only under `desktop/src/main/resources/db/migration/` and are not reused.
- The backend never connects to the desktop SQLite file, and the desktop never connects to Postgres.

## Migration and rollback
- Version: 001 | Existing rows: none | Regenerate committed data: no - the committed mock DB `db/snoozeshare-mock.db` moves unchanged to `desktop/db/` (no rebuild; only its path changes) | If it fails halfway: Flyway runs `V001` in one transaction, so a failure leaves no history row and the backend refuses to start; fix and restart.
- Desktop move rollback: revert the move commit; because it is a pure `git mv`, `git revert` restores every path.

## Architecture areas changed
- § Architecture intro and § 4.1-4.8 - these remain accurate for `desktop/`; the branch state adds a mono-repo layout block above them.
- § Orientation path table - every `src/`, `db/`, `build.gradle` path gains the `desktop/` prefix; `backend/`, `frontend/`, `docker-compose.yml` are added.
- § How to Resume - the `.\gradlew` commands gain `desktop/` and the web commands arrive.
- § Needs a Human - Q5 is closed by the fold.
- § Conventions - new rules from [conventions.md](conventions.md).
