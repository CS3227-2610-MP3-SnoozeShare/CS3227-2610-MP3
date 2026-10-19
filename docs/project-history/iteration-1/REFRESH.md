# Refresh — iteration 1 closed

- **Closed:** 2026-10-19 by Claude Sonnet 5.5 on `main` · last commit `dbd16d8`
- **Approved by:** project owner, 2026-10-19 (revised table: W16 moved to Guide after the owner's review; MP3-0 carried)
- **Developer guide at closure:** last full update 2026-10-19 (the MP3-0 checkpoint), commit `dbd16d8`
- **ID high-water:** W16, C59, D38, Q3 (no S IDs; session rows live in branch states)
- **Size:** PROJECT_STATE.md 917 lines → 269; ledger 174 entries archived
- **Open workstreams carried:** MP3-0

Note: C11, C28, C29, C30, C31, C37, C39, C40 and C41 each appear twice in the old file (separate meanings, from two
branches). They keep their IDs; the rows below name which meaning each is. IDs are never reused.

## Disposition (166 entries: 76 Carry, 59 Archive, 31 Guide)

| Source | Entry | Disposition | Where / why |
|---|---|---|---|
| § Workstreams | W1 F0 — Auth, Registration & Wallet Provisioning | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W2 F1 — Listing Search & Property Discovery | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W3 F2 — Booking Execution & Trip Hub (incl. escrow) | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W4 F3 — Guest Feedback, Disputes & Reviews | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W5 F4 — Guest Wallet Management (top-up/withdraw) | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W6 F5 — Host Listing Management & Publishing | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W7 F6 — Host Calendar & Date Overrides | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W8 F7 — Host Request Queue, Earnings & Disputes | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W9 F8 — Host Wallet Management | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W10 F9 — Agent Dispute Resolution (F9.2.1 force action… | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W11 F10 — Agent Account Governance (F10.1.1 suspend, F… | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W12 F11 — Platform Audit Trail (Analytics half of the … | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W13 F12 — Messaging (persistent ticket chat; MessageSe… | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W14 Unified ledger — fold wallet_transactions into aud… | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W15 Host Listings Dashboard & Copy Refinement | Guide | Done; Guide `Documented` (see guide § 4 and § 5) |
| § Workstreams | W16 Guest UI mockup alignment (Messages sizing + ticke… | Guide | Done; operator reviewed and accepted 2026-10-19; Guide `Documented` (guide § 4.8, traps D38) |
| § Workstreams | MP3-0 MP2 feedback fixes before new MP3 features (bugs B… | Carry | Open: merged, CI and Pages verified; only required-check branch protection remains (Upcoming) |
| § Workstreams | Historical W10→W3 handoffs (5 items) | Archive | Reconciled at W3 closure; the live rules are C17, C23, D9 and the Role.AGENT permission, carried or in the guide |
| § Decisions | C1 Package-based modularity (com.snoozeshare.{domain,service,repository,i… | Carry | Standing: shapes § Architecture (package layering, no JPMS) |
| § Decisions | C2 WalletPanelController lives in ui.common, not duplicated per role | Carry | Standing: shapes § Architecture 4.2 (wallet panel lives in ui.common) |
| § Decisions | C11 UI design proceeded mockup-first: a Claude Design canvas built and ite… | Archive | Process: UI design method / execution mode for W1 |
| § Decisions | C3 Two separate financial interfaces: WalletService (dumb primitive — bal… | Carry | Standing: shapes 4.3; narrowed by C39/C40(W14); both interfaces kept |
| § Decisions | C4 Use Java 25 records directly as the domain model passed to JavaFX view… | Carry | Standing: shapes 4.4 (records are the model; rejects a DTO layer, an out-of-scope item) |
| § Decisions | C7 No guest-side service fee. bookings.totalAmount = nightlyRateSnapshot … | Carry | Standing: no guest-side fee; a Known-Gap-style prohibition (3% host fee only) |
| § Decisions | C8 REJECTED and CANCELLED_BY_HOST bookings both trigger a 100% ESCROW_REF… | Carry | Standing: rejected/host-cancelled bookings refund 100% |
| § Decisions | C9 *(Superseded for agent ticket resolutions by C20, 2026-09-25 — those a… | Guide | Superseded for tickets by C20; the rest is in guide 4.16 |
| § Decisions | C10 wallets.currency is "SGD" for real, not just the doc's illustrative ex… | Guide | wallets.currency = SGD; in code and guide |
| § Decisions | C6 properties columns are exactly: propertyId, hostId, status (ListingSta… | Carry | Standing: the authoritative `properties` column list |
| § Decisions | C5 Embedded DB is SQLite (org.xerial:sqlite-jdbc), accessed via hand-roll… | Carry | Standing: shapes 4.5 (SQLite via plain JDBC; rejects JPA) |
| § Decisions | C37 Recorded in full in § Decisions | Archive | Pointer row to the entry below |
| § Decisions | C28–C32 Audit structure, filters, System user, platform fee as reason text onl… | Archive | Pointer row; the full entries are listed individually |
| § Decisions | C39–C42 W14 executed: System user becomes role SYSTEM with a real wallet, wall… | Archive | Pointer row; the full entries are listed individually |
| § Decisions | C11 Use one-workstream-at-a-time development with TDD-first vertical slice… | Archive | Process: UI design method / execution mode for W1 |
| § Decisions | C12 Host and Agent registration codes are mock constants | Archive | Mock registration codes; covered by the Auth Known Gap |
| § Decisions | C13 W1 owns the complete shared foundation and all cross-cutting logic req… | Archive | W1 ownership boundary; process |
| § Decisions | C14 Use a combined Login/Register entry screen; display Support Agent as t… | Guide | Combined login screen; in guide 3.1 |
| § Decisions | C16 W10 is built concurrently with W3/W4 against the service interfaces (n… | Archive | W10 concurrency plan; process |
| § Decisions | C17 Escrow is held through the 7-day dispute window (checkout + 7d). A tic… | Carry | Standing: escrow held through the dispute window; § Conventions depend on it |
| § Decisions | C18 On an agent Full Payout or Partial split, the 3% platform fee applies … | Guide | Fee on host share; in guide 4.5 |
| § Decisions | C19 Agent Force Cancel = 100% escrow refund to guest (per C8); Force Compl… | Archive | Reversed by C22 |
| § Decisions | C20 Every ticket resolution and manual adjustment settles the full held es… | Carry | Standing: every resolution settles the full escrow; supersedes C9 and C18/C19 for tickets |
| § Decisions | C21 Ticket chat threads (guest↔agent and host↔agent, as shown in the desig… | Archive | Superseded by C37/C38 |
| § Decisions | C22 Reverses C19 and drops backlog F9.2.1 (Force Cancel / Force Complete) … | Carry | Standing: rejects force actions; likely to be re-proposed |
| § Decisions | C23 Ticket resolution moves the booking CONFIRMED → COMPLETED (funds settl… | Carry | Standing: Role.AGENT on CONFIRMED→COMPLETED |
| § Decisions | C24 Reverses D11 for the agent screens. After the manual visual check the … | Guide | Agent theme; in guide 4.13 |
| § Decisions | C25 After the second visual check the operator changed three behaviours: (… | Guide | Agent notes/unassign; in guide 4.6 and 4.13 |
| § Decisions | C26 Third visual round, operator: dropdown text and options right-aligned;… | Guide | Modal scrim and category delete; in guide 4.13 |
| § Decisions | C27 Ticket status UNDER_REVIEW is renamed IN_REVIEW everywhere (enum, tick… | Guide | IN_REVIEW rename; in guide glossary |
| § Decisions | C28 Audit log structure (W12). One row = one change. beforeState/afterStat… | Carry | Standing: audit log structure (one row per change); the other C28 (Host copy waiver) is archived |
| § Decisions | C29 Audit Log screen filters are ONE search box + action-type filter (a si… | Archive | Audit filters, superseded by C33 and the guide 4.14 |
| § Decisions | C30 Platform fee stays reason-text only in W12; the § Known Gaps entry "no… | Archive | Fee as reason text; superseded by C31/C40(W14) |
| § Decisions | C31 Reversal, executed by W14, 2026-09-27 (originally recorded as planned/… | Carry | Standing: ledger fold executed by W14; the W8 `C31` is archived |
| § Decisions | C32 W12 spec approved with its § 10 defaults: REF column added to the Audi… | Archive | W12 defaults; System user role reversed by C40(W14) |
| § Decisions | C33 Operator UI amendments to the W12 Audit Log (supersede C29's single ac… | Guide | Audit Log UI amendments; in guide 4.13 and 4.14 |
| § Decisions | C34 W11 scope: (a) Reactivate is built although F10.1.1 says only suspend … | Carry | Standing: W11 scope (Reactivate, cascade, reason column); reversing needs operator |
| § Decisions | C36 W11 cascade scope widened: suspension force-cancels PENDING and not-ye… | Carry | Standing: cascade covers PENDING and not-yet-started CONFIRMED; amends C34 |
| § Decisions | C35 W11 UI deltas from the canvas: first column "Display Name" plus a new … | Guide | W11 UI deltas; in guide 4.15 |
| § Decisions | C37 W13 scope: replace InMemoryMessageService with a persistent MessageSer… | Carry | Standing: persistent MessageService scope; supersedes W13 in-memory C21 |
| § Decisions | C38 Reverses the W13 spec's exclusion of chat outside tickets. Add a booki… | Carry | Standing: booking chat, agents may not read it; reversal needs operator |
| § Decisions | C40 Reverse the former Host Messages ticket-filing decision: Hosts cannot … | Carry | Standing: Hosts cannot file tickets (reversal needs operator) |
| § Decisions | C41 Refine the existing Host Listings, New/Edit Listing, and Booking Calen… | Archive | Host page refinement; in the code and guide 4.9 |
| § Decisions | C15 Execute W1 natively in the existing w1 checkout rather than creating a… | Archive | Where W1 was executed; process |
| § Decisions | C28 Proceed with Host Listings Dashboard & Copy Refinement without a new d… | Archive | Historical |
| § Decisions | C29 Defer F7.2.2 structured host dispute response notes/evidence from W8 t… | Archive | Deferred item, done by W13 |
| § Decisions | C30 Host booking approve/reject actions require confirmation modals matchi… | Archive | Host modals; in guide 4.11 |
| § Decisions | C31 W8 persists the optional host rejection message on the booking and exp… | Archive | Historical |
| § Decisions | C39 W14 specified. Follows C31, with two refinements: WalletService/Transa… | Carry | Standing: WalletService/TransactionService kept (narrows C3) |
| § Decisions | C40 W14 refinements: (a) the wallets table stays, so no users.balance (rev… | Carry | Standing: wallets table stays; SYSTEM role (reverses part of C31/C32) |
| § Decisions | C41 W14 spec § 10 defaults accepted as written: keep WalletService and Tra… | Archive | W14 split/defaults, executed |
| § Decisions | C42 W14 Task 8 review found $0/sub-cent listing rates crashed booking sett… | Carry | Standing: forbids $0 / sub-cent listing rates |
| § Decisions | C43 Renumbered from a colliding C32 on the w9/main side while merging unif… | Guide | W9 wallet statement; in guide 4.12 |
| § Decisions | C44 Renumbered from a colliding C33 on the w9/main side, same merge as C43… | Guide | W9 common wallet; in guide 4.12 |
| § Decisions | C45 W14 (Unified Ledger) accepted as Done. Also landed on main since the o… | Archive | Acceptance record for W14 |
| § Decisions | C46 The W14 Developer Guide checkpoint (C45) is confirmed: docs/DeveloperG… | Archive | Guide checkpoint record |
| § Decisions | C47 Organize the Guest and Host parts of § 4 like the Agent documentation:… | Archive | Guide structure choice |
| § Decisions | C48 MP3-0 B1: dispute filing stays open through endDate+7; auto-completion… | Carry | Standing: dispute filing through endDate+7, completion from endDate+8 |
| § Decisions | C49 MP3-0 D1: wallet events publish automatically after commit via a dedic… | Carry | Standing: PostCommitEvents separate from TransactionManager |
| § Decisions | C50 Cloud deployment is a recorded future direction only; the single-proce… | Carry | Standing: cloud is future-only; single-process Known Gap stands |
| § Decisions | C51 B7 resolved by keeping C31: the host message exists only when declinin… | Guide | B7/D4 resolution; in the code and guide 4.11 |
| § Decisions | C52 MP3-0 docs and process pass. G6 not done: reflections are rewritten in… | Archive | Docs pass record; Pages part superseded by C56 |
| § Decisions | C53 MP3-0 date rule widened beyond B4/B5: no date picker accepts a past da… | Carry | Standing: no date picker accepts a past date (Audit Log excepted) |
| § Decisions | C54 Adopt the branch-state workflow. PROJECT_STATE.md describes main only … | Carry | Standing: branch-state workflow; binds every agent |
| § Decisions | C55 Adopt refresh-project-state as the way to close an iteration: guide fi… | Carry | Standing: refresh-project-state is run by the owner, guide first |
| § Decisions | C56 The documentation site is published by a GitHub Actions workflow (.git… | Carry | Standing: Pages deploys from the Actions workflow; supersedes the folder part of C52 |
| § Decisions | C57 W16 scope and defaults: replicate the canvas Search, Trips (collapsibl… | Guide | W16 scope; in guide 4.8 and D38 |
| § Decisions | C58 W16 follow-ups requested by the operator: (1) the canvas *Cancel booki… | Guide | W16 follow-ups; in guide 4.8 |
| § Decisions | C59 CI policy: Checkstyle on every push to any branch; the full build and … | Carry | Standing: CI policy (Checkstyle on every push, build on PRs and main) |
| § Conventions | ui.* must never import repository.* or infra.db.* directly — only serv… | Carry | Binding rule |
| § Conventions | Only repository.jdbc.* may import java.sql.*. | Carry | Binding rule |
| § Conventions | BookingService/TicketService are the only callers of TransactionServic… | Carry | Binding rule |
| § Conventions | TransactionService never touches WalletRepository directly — it goes t… | Carry | Binding rule |
| § Conventions | All state transitions (booking, ticket) go through the shared *StateMa… | Carry | Binding rule |
| § Conventions | One audit row per change: a state change and its money movements are s… | Carry | Binding rule |
| § Conventions | wallets.balance is a denormalized cache of the owner's newest money ro… | Carry | Binding rule |
| § Conventions | Only LedgerWriter changes a wallet balance; it never opens a transacti… | Carry | Binding rule |
| § Conventions | Wallet events are queued by LedgerWriter; never publish WalletTransact… | Carry | Binding rule |
| § Conventions | Dispute-window and stay-date rules live only in domain.policy; never r… | Carry | Binding rule |
| § Conventions | Integration merges stay small and run ./gradlew build each time (MP2 f… | Carry | Binding rule |
| § Conventions | ID namespaces: W = workstream, C = decision, D = deviation, Q = needs … | Carry | Binding rule |
| § Conventions | Iterations. When the owner declares an iteration finished and the Deve… | Carry | Binding rule |
| § Conventions | Branches never edit this file. Each branch writes docs/project-state/b… | Carry | Binding rule |
| § Conventions | Agent interaction logs are append-only, one file per session under log… | Carry | Binding rule |
| § Known Gaps | No multi-instance / distributed deployment support — single-process de… | Carry | Decision, not history (AGENTS.md § 4) |
| § Known Gaps | Ticket categories/remedy types are a status enum + a simple ticket_cat… | Carry | Decision, not history (AGENTS.md § 4) |
| § Known Gaps | Booking overlap-check + escrow hold use a SERIALIZABLE DB transaction,… | Carry | Decision, not history (AGENTS.md § 4) |
| § Known Gaps | Top-up/withdraw are fully mocked — WalletService just credits/debits b… | Carry | Decision, not history (AGENTS.md § 4) |
| § Known Gaps | Auth is fully mocked — no real credential validation anywhere; role se… | Carry | Decision, not history (AGENTS.md § 4) |
| § Known Gaps | Audit timestamps are Instant.toString() strings (W12). Variable-length… | Carry | Decision, not history (AGENTS.md § 4) |
| § Known Gaps | Audit Log UI polish left open (W12): the REF column truncates when a r… | Carry | Decision, not history (AGENTS.md § 4) |
| § Known Gaps | Explicitly out of scope: microservices, message brokers, horizontal sc… | Carry | Decision, not history (AGENTS.md § 4) |
| § Needs a Human | No outstanding questions (Q3 resolved as C36) | Archive | Nothing open; Q16-style IDs: highest Q used is Q3 |
| § Deviations | MP3-0 deviations (11 bullets) | Carry | Live: the two PostCommitEvents modes, UserServiceImpl own TransactionManager, settlement event order, SourceText line endings; the rest archived |
| § Deviations | D38 — W16 Guest UI mockup alignment: deviations and traps (2026-09-29) | Carry | Live: W16 traps (wallet card specificity, EmptyTableRow, pill size, 32px inset) |
| § Deviations | W14 deviations (V007/V008 split, C42, rounding fix) | Archive | Done; in guide 4.16 and C42 |
| § Deviations | D20 — W11 deviations and accepted limitations (2026-09-26) | Carry | Live: suspension refund is not checked against an ESCROW_HOLD row |
| § Deviations | D2 — Two schema gaps found while grounding the UI mockups against db/s… | Archive | Resolved by W12 (C28/C32) |
| § Deviations | D22 — W13 notes (2026-09-27) | Archive | W13 signature change; in the code and guide 4.4 |
| § Deviations | D23 — Host Messages verification baseline (2026-09-27) | Archive | Verification baseline, superseded |
| § Deviations | D24 — Host Messages list layout refinement (2026-09-27) | Archive | Host Messages layout refinement; in the code and guide 4.9 |
| § Deviations | D25 — Host Messages horizontal overflow refinement (2026-09-27) | Archive | Host Messages overflow refinement |
| § Deviations | D26 — Host ticket creation reversed (2026-09-27) | Archive | Host ticket creation reversed (C40) |
| § Deviations | D27 — Wallet Related To mapping corrected (2026-09-27) | Archive | Wallet Related-To mapping fixed |
| § Deviations | D28 — Host listing breadcrumb navigation (2026-09-27) | Archive | Reversed by D36 |
| § Deviations | D29 — Host breadcrumb styling ownership (2026-09-27) | Archive | Host crumb styling ownership; enforced by HostThemeOwnershipTest |
| § Deviations | D30 — Host Edit breadcrumb separator (2026-09-27) | Archive | Edit breadcrumb separator |
| § Deviations | D31 — Host Messages typography hierarchy (2026-09-27) | Archive | Typography hierarchy |
| § Deviations | D32 — Host Messages row hierarchy (2026-09-27) | Archive | Row hierarchy |
| § Deviations | D33 — Host Messages badge clipping correction (2026-09-27) | Archive | Badge clipping fix |
| § Deviations | D34 — Consolidated Host stylesheet (2026-09-27) | Archive | host-theme.css ownership; enforced by HostThemeOwnershipTest |
| § Deviations | D35 — Host Messages typography specificity correction (2026-09-27) | Archive | Typography specificity fix |
| § Deviations | D36 — Reverse D28: restore listing form titles (2026-09-27) | Archive | Listing form titles restored |
| § Deviations | D37 — Compact listing form actions (2026-09-27) | Archive | Compact listing form actions |
| § Deviations | D21 — Merge of origin/main into the W11 branch (2026-09-27) | Archive | Merge record for the W11 branch |
| § Deviations | D19 — UserService.suspend replaced by AccountGovernanceService (W11, R… | Archive | Resolved: UserService.suspend removed |
| § Deviations | D1 — Architecture proposal said SQLite, repo briefly ran H2 — now reso… | Archive | Resolved: SQLite chosen (C5) |
| § Deviations | D2 — Feature-specific persistence and transaction policy remain with o… | Archive | Resolved by W12 (C28/C32) |
| § Deviations | D4 — SQLite REAL columns lose BigDecimal scale (W2, RESOLVED 2026-09-2… | Carry | Live: SQLite REAL loses BigDecimal scale; assert with compareTo |
| § Deviations | D3 — SQLite Java 25 native-access warning is environmental | Carry | Live: SQLite native-access warning is environmental |
| § Deviations | D5 — Manual host blocks use inclusive end dates (W7, RESOLVED 2026-09-… | Carry | Live: manual host blocks take inclusive end dates, stored half-open |
| § Deviations | D5 — W10 adds DisputeSettlementService instead of TransactionService.a… | Archive | Resolved: D4 deleted the unbuilt stubs |
| § Deviations | D6 (RESOLVED 2026-09-25) — The mock DB held rows predating C17/C20. Pe… | Archive | Resolved 2026-09-25; the mock DB conventions are carried in the repo map and guide § 7 |
| § Deviations | D7 — Design artifact differs from decisions: Force actions (C22), newe… | Archive | Design artifact differences; recorded in the W10 spec |
| § Deviations | D8 — tickets.category is label text, not an FK; renames don't propagat… | Carry | Live: tickets.category is label text, renames do not propagate |
| § Deviations | D9 — BookingStateMachine gains AGENT on CONFIRMED → COMPLETED (C23); W… | Archive | Covered by C23 |
| § Deviations | D10 — MigrationRunner adopts a pre-provisioned DB: the mock DB has tab… | Carry | Live: MigrationRunner adopts a pre-provisioned DB |
| § Deviations | D11 (SUPERSEDED by C24, 2026-09-25) — The admin shell is sidebar-based… | Archive | Superseded by C24 |
| § Deviations | D12 — Accepted minor findings from the W10 code review (not fixed; rev… | Carry | Live: accepted minor findings (also in guide Known Limitations) |
| § Deviations | D13 (W8, resolved 2026-09-27) — The committed reference/mock database … | Carry | Live: committed mock DB predates V002; null hostDecisionMessage is tolerated |
| § Deviations | D13.5 — Wallet rows are dual-written until W14. Every wallet movement … | Archive | Resolved: dual-write ended by W14 |
| § Deviations | D14 — RESOLVED 2026-09-26 (operator verified the real-app run). Origin… | Archive | Resolved 2026-09-26: operator verified the real-app run |
| § Deviations | D15 — Mock DB now ships migrated. MockDbFixture no longer migrates its… | Carry | Live: mock DB ships migrated; rebuild from schema + seed |
| § Deviations | D16 — Seed availability_blocks ids remapped to valid hex prefixes (60…… | Archive | Resolved: seed ids remapped |
| § Deviations | D17 — API changes from the plan: AuditService.query(...) replaced by s… | Archive | API changes from the W12 plan; in the code |
| § Deviations | D18 — C33 styling limits (accepted). The JavaFX calendar cannot be res… | Carry | Live: JavaFX date-picker popup cannot be restyled to the board |
| § Deviations | Found during W10 execution (a)–(c) and backlog edits 2026-09-25 | Archive | All fixed; seed id/timestamp rules are carried in the repo map |
| § Architecture | 4.1 App shell & bootstrap (Now + paths) | Carry | Tightened; progress narration dropped |
| § Architecture | 4.2 UI (role-isolated) (Now + paths) | Carry | Tightened; progress narration dropped |
| § Architecture | 4.3 Service (application/business logic) (Now + paths) | Carry | Tightened; progress narration dropped |
| § Architecture | 4.4 Domain (Now + paths) | Carry | Tightened; progress narration dropped |
| § Architecture | 4.5 Repository & persistence (Now + paths) | Carry | Tightened; progress narration dropped |
| § Architecture | 4.6 Events (in-process bus) (Now + paths) | Carry | Tightened; progress narration dropped |
| § Architecture | 4.6a Messaging (W13) (Now + paths) | Carry | Tightened; progress narration dropped |
| § Architecture | 4.7 Session (Now + paths) | Carry | Tightened; progress narration dropped |
| § Architecture | 4.8 Audit trail (Now + paths) | Carry | Tightened; progress narration dropped |
| Header, § Orientation, § How to Resume | Header, purpose, repo map, commands, Upcoming rows | Carry | Rewritten for iteration 2 |
| § Record | Done ledger pointer | Archive | Ledger moves to the archive; a new empty ledger replaces it |
