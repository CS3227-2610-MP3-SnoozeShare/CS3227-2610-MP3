# Branch State — W-DOMAIN

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-DOMAIN (from `development` at `4cc27c3`)
- **Base:** `349d55c` (development tip after W-AUTH merge)
- **Owner:** Nathan
- **Status:** Approved
- **Approved:** Nathan, 2026-10-21
- **Last updated:** 2026-10-21 by Claude Opus 4.6 — closed for review

---

**Scope.** Port the domain model, repositories and core services from the desktop app to the Spring Boot backend. Settles the persistence approach as Spring Data JPA (C74 reversed). Serves the W-DOMAIN workstream.

---

## 1. Orientation

No change.

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md`, no additional steps.

```bash
cd backend; ./gradlew build   # needs Docker (Testcontainers)
```

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|

**Remaining**

- [x] T5: Spring Data JPA repository interfaces (12 repos)
- [x] T6: Port infrastructure (@Transactional, ApplicationEventPublisher, LedgerWriter, Clock bean, AuditServiceImpl)
- [x] T7: Port 15 service implementations (13 interfaces, 14 value objects, 15 impls, 1 utility class, 2 request types)
- [x] T8: DTO layer (16 request DTOs, 21 response DTOs, 3 mappers)
- [x] T9: Tests (unit + integration)
- [ ] **Handover:** Update Developer Guide for W-DOMAIN (entities, repositories, services, JPA conventions)

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W~DOMAIN | Port domain model + core services to Spring Boot | Building | — | [plan](docs/superpowers/plans/2026-10-21-w-domain.md) | T9/9 done; handover next | — |

---

## 4. Architecture

### 4.5 Repository & persistence

**Now (proposed):** Spring Data JPA with Hibernate on PostgreSQL 17. Domain model uses `@Entity` classes (mutable, no-arg constructors) in `domain.model`, mapped to the schema created by Flyway V002 (auth tables from W-AUTH) and V003 (domain ALTERs and remaining 11 tables). All entities use `id` as the PK column name, matching W-AUTH's convention. `WalletTransaction` remains a record (not an entity) — it is a projection from `audit_log` rows read by a future `JdbcTemplate`-based `LedgerRepository`. V004 seeds the SYSTEM user and platform-fee wallet. JPA is configured with `ddl-auto: validate` and `open-in-view: false`.

| Path | Role |
|---|---|
| `backend/src/main/java/com/snoozeshare/domain/model/` | 12 JPA entity classes + `AmenitySetConverter` + `WalletTransaction` record |
| `backend/src/main/java/com/snoozeshare/domain/enums/` | 13 enums (copied from desktop) |
| `backend/src/main/java/com/snoozeshare/domain/statemachine/` | `BookingStateMachine`, `TicketStateMachine` (copied from desktop) |
| `backend/src/main/java/com/snoozeshare/domain/policy/` | `StayDatePolicy`, `DisputeWindowPolicy` (updated for entity getters) |
| `backend/src/main/java/com/snoozeshare/domain/settlement/` | `SettlementCalculator`, `SettlementBreakdown`, `EscrowPolicy` (copied) |
| `backend/src/main/java/com/snoozeshare/domain/validation/` | `DomainValidation` (copied) |
| `backend/src/main/java/com/snoozeshare/service/` | 13 service interfaces, 14 value-object records, 2 request types |
| `backend/src/main/java/com/snoozeshare/service/impl/` | 15 service implementations + `AuthorizationService` utility |
| `backend/src/main/java/com/snoozeshare/api/request/` | 16 request DTOs (records with `@Valid` + `@JsonIgnoreProperties`) |
| `backend/src/main/java/com/snoozeshare/api/response/` | 21 response DTOs (records with `static from()` factory methods) |
| `backend/src/main/java/com/snoozeshare/api/mapper/` | 3 mapper utilities (`ListingMapper`, `TicketMapper`, `SearchMapper`) |
| `backend/src/main/resources/db/migration/V003__domain_tables.sql` | ALTERs W-AUTH's users/wallets + creates 11 domain tables |
| `backend/src/main/resources/db/migration/V004__seed_system_user.sql` | SYSTEM user + platform wallet seed |

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-21 | Use Spring Data JPA for persistence (reverses C74/C5). Entities use `id` as PK column on all tables to match W-AUTH's convention | JPA provides cleaner Spring integration (validation, transactions, repositories) than ported JDBC; column naming follows W-AUTH's established pattern | Plan T1 |
| C~2 | 2026-10-21 | Wallet balance uses `NUMERIC(19,2)` (from W-AUTH's V002), not `NUMERIC(19,4)` from the desktop schema. Other money columns (rates, amounts, adjustments) keep `NUMERIC(19,4)` | W-AUTH created the wallets table first; the domain migration builds on it | V002/V003 reconciliation |

---

## 5. Conventions

- JPA entities use `id` as the PK column name (not entity-specific names like `user_id`, `booking_id`). Getter is `getId()`.
- Domain enums in `domain.enums` are shared between the desktop and backend; copy verbatim, change only the package declaration.
- Checkstyle import order: `java.*` then `org.*` then `com.*` then `jakarta.*`, with blank lines between groups.
- All service impls and JDBC repos use `@ConditionalOnBean(PropertyRepository.class)` to avoid loading in no-DB test contexts (e.g. `ServerPortDefaultTest`).
- Service impls use entity-style getters (`.getField()`) instead of record accessors (`.field()`); mutation uses setters + `repository.save()` instead of creating new records.

---

## 6. Known Gaps

- Replaces "JPA/Hibernate stays out of scope" (C74): JPA is now adopted (C~1). The `SERIALIZABLE` booking overlap-check convention carries over; implementation is TBD in T5/T7.

---

## 7. Needs a Human

No change.

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-21 | Use Spring Data JPA for persistence (reverses C74/C5) | Cleaner Spring integration; operator approved the plan which specifies JPA | Plan T1 |
| C~2 | 2026-10-21 | Wallet balance precision is `NUMERIC(19,2)` per W-AUTH's V002 | W-AUTH created the table first; reconciliation follows existing schema | V002/V003 merge |

---

## 9. Deviations

| ID | Date | What | Consequence |
|---|---|---|---|
| D~1 | 2026-10-21 | W-AUTH's V002 uses `id` as PK for users/wallets (not `user_id`/`wallet_id` from the desktop schema), `status` (not `account_status`), and includes `firebase_uid` + `updated_at`. The plan's original migration had to be restructured as V003 (ALTER + new tables) instead of V002 (CREATE all) | All entity PK fields renamed to `id`; User entity gained `firebaseUid`, `updatedAt`; `accountStatus` renamed to `status`. Future services must use `getId()` not entity-specific getters |
| D~2 | 2026-10-21 | Flyway version conflict: W-AUTH claimed V002, our domain tables had to shift to V003, seed to V004 | Migration numbering diverged from the plan; no functional impact |

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-21 | Add `spring-boot-starter-data-jpa`, JPA config (`ddl-auto: validate`, `open-in-view: false`), Flyway domain migration V003 and seed V004 | W~DOMAIN | `fddc502` |
| 2026-10-21 | Port 13 domain enums from desktop to backend | W~DOMAIN | `a45b743` |
| 2026-10-21 | Port pure logic classes (state machines, policies, settlement, validation) from desktop; update `DisputeWindowPolicy` for entity-style getters | W~DOMAIN | `95ed596` |
| 2026-10-21 | Create 12 JPA entity classes + `AmenitySetConverter` + `WalletTransaction` record | W~DOMAIN | `d96caca` |
| 2026-10-21 | Merge W-AUTH from development: resolve Flyway V002 conflict (restructure to V003/V004), reconcile all entity PK columns to `id`, add `firebaseUid`/`updatedAt` to User, fix wallet balance scale to match V002 | W~DOMAIN | `3eeff49` |
| 2026-10-21 | Port 15 service implementations (T7): 13 interfaces, 14 value objects, 2 request types, 15 impls, 1 utility; adapted desktop record patterns to JPA entity patterns; changed T6 JDBC repos and service impls from `@ConditionalOnBean(JdbcTemplate/WalletRepo/AuditLogRepo)` to `@ConditionalOnBean(PropertyRepository.class)` for reliable no-DB test support | W~DOMAIN | `7ecc9a1` |
| 2026-10-21 | Create DTO layer (T8): 16 request DTOs with Jakarta validation, 21 response DTOs with `from()` factories, 3 mappers; follows auth module pattern (`@JsonIgnoreProperties(ignoreUnknown = false)`, records) | W~DOMAIN | — |
| 2026-10-21 | Add T9 tests: 6 unit test classes (BookingStateMachine, TicketStateMachine, StayDatePolicy, DisputeWindowPolicy, SettlementCalculator, DomainValidation) and 5 repository integration tests (`@DataJpaTest` + Testcontainers: User, Booking, Wallet, Ticket, AuditLog); all 163 tests green | W~DOMAIN | — |
