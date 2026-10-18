# MP2 Feedback Triage — fixes before MP3 features

Source: `MP2_Grade_Report_cleaned.pdf` (TA1, TA2, Peer reviewers 1–3). Items marked **✔ verified**
were spot-checked against the repo on 2026-10-18; the rest are taken from the reviewers' reports and
need reproducing before fixing.

Priority: **P0** wrong behaviour / lost money-or-dispute outcomes · **P1** visible bugs & rubric
losses · **P2** quality & maintainability · **P3** process.

---

## Status (updated 2026-10-19)

Fixed on branch `mp2-bug-fixes` (MP3-0 correctness cluster; awaiting operator review/merge):

| Item | Commit(s) |
|---|---|
| D3 (policies) | `282f1b4` |
| B1, P5 (day-7 boundary + joined test) | `7501a72` |
| B4, B5 (date validation in services) | `a6123c3` |
| B3, D1 (wallet events queued, published after commit) | `7267911`, `efea1dd` |
| D2 (audit atomicity) | `91edc76`, `6806543` |
| D5 (fee defined once) | `36f02e3` |

Fixed on branch `mp2-smaller-bug-fixes` (2026-10-19): B2, B6, B7, D4 (B7 resolved by relabelling to net and correcting the User Guide; the approval dialog deliberately has no message field, C31).

Fixed on branch `mp2-docs-and-process-fixes` (2026-10-19): D6, P2, P3, P4, G1–G5. Details in the Done ledger.

Not doing: **G6** (reflections are rewritten in the MP3 iteration); the `reviewed-plan` skill (never used here).

Still open: **P1** (no CI). Follow-ups from this pass: five User Guide screenshots were not captured (db folder, top-up dialog, host listing detail with Performance card, approve dialog, suspend dialog), and the Jekyll site was not built locally. The manual GUI check of the B1/B3 fixes was not performed.

---

## 1. Bugs to fix (functional)

| # | Pri | Bug | Raised by | Notes / likely cause |
|---|---|---|---|---|
| B1 | P0 | **Auto-complete vs dispute window mismatch.** On day 7 after checkout the booking auto-completes and pays the host, but ticket filing is still allowed; every resolution option then fails because the booking is `COMPLETED`. An accepted complaint has no usable resolution. | TA1 (×3) | Two rules in two layers disagree (completion sweep vs `fileTicket` 7-day window vs `BookingStateMachine`/`DisputeSettlementService`). Decide one boundary (e.g. filing closes *before* completion, or settlement can claw back / settle from a completed booking), put it in one place, add a joined test. Relates to the old "auto-complete must skip open tickets" handoff in `PROJECT_STATE.md` § Workstreams. |
| B2 | P1 | **Creating a listing as Inactive publishes it as Active.** | TA1, Peer 1 | ✔ verified — `ListingServiceImpl.create` hard-codes `ListingStatus.ACTIVE` (line ~104) and ignores `draft.status()`; the existing test locks the wrong behaviour in. Honour `draft.status()` (or remove the field from the form), fix the test. |
| B3 | P1 | **Wallet balance in the top-right header is stale after cancelling a booking** (wallet page refreshes, header doesn't). | Peer 1, Peer 3 | Cancel path doesn't publish `WalletTransactionRecordedEvent`. See D1 — fix structurally, not by adding one more `publish`. |
| B4 | P1 | **Typed past dates create bookings** despite the calendar restriction. | TA1 | Validation lives only in the DatePicker UI. Add the check in `BookingService.submitRequest` (and `AvailabilityService`). |
| B5 | P1 | **Search filter allows check-in later than check-out.** | TA2 | Validate in the filter controller and the search service. |
| B6 | P2 | Confirmed booking does not show as "Booked" in the Host booking calendar. | Peer 3 | Calendar likely keys off availability blocks / only `PENDING`; map `CONFIRMED` bookings to the Booked state. |
| B7 | P2 | Host approval dialog has no optional "Message to guest" field (User Guide says it does), and labels the **gross** amount "earning" while Requests shows the net. | Peer 3 | Either add the field to the dialog or fix the guide; relabel to "Booking total" / show net. (W8 persisted `hostDecisionMessage` — the UI dialog just may not expose it.) |
| — | — | Peer 1 Bug 2 (full refund on cancel < 48 h for a pending request) — reviewer self-resolved as intended. **No fix**, but document it (see G3). | Peer 1 | |

## 2. Design / code-quality issues

| # | Pri | Issue | Raised by | Suggested fix |
|---|---|---|---|---|
| D1 | P1 | **Temporal coupling between wallet writes and UI events.** `LedgerWriter` centralises balance changes, but each service must separately remember to publish `WalletTransactionRecordedEvent`; cancel forgot (root cause of B3). | Peer 1 | Have `LedgerWriter` (or the `TransactionManager`) collect events and publish them automatically after commit. Add a test: every `LedgerWriter` write ⇒ exactly one event post-commit. |
| D2 | P1 | **Audit write not in the same transaction** for listing, review and ticket changes (entity saved, then audit entry written outside the transaction); only settlement is protected. | TA1 | Wrap each in `TransactionManager`, add rollback tests like the settlement ones. |
| D3 | P2 | **Business rules duplicated across layers** (date rules, dispute eligibility) — their disagreement causes B1/B4. | TA1 | One authoritative policy class per rule (e.g. `DisputeWindowPolicy`, `StayDatePolicy`) called from UI, service and sweep. |
| D4 | P2 | **Dead stubs** still throwing `UnsupportedOperationException("Owned by W10")`. | TA1 | ✔ verified — `BookingServiceImpl:357`, `TransactionServiceImpl:113,119`. Remove from interfaces (`forceTransition`, `applyTicketRemedy`, `manualOverride`) or implement; update tests. |
| D5 | P2 | **Fee literal repeated** in payout calculations. | TA1 | ✔ partly — `SettlementCalculator.FEE_RATE` exists; grep the other sites (guide, tests, UI text, seed) and route all through it. |
| D6 | P3 | Public controller/service methods under-documented; composition responsibilities unclear. | TA2 | Javadoc the non-obvious public APIs (`AppContext`, controllers, services). |

## 3. Process / infrastructure

| # | Pri | Item | Raised by | Action |
|---|---|---|---|---|
| P1 | **P0 (rubric)** | **No CI/CD.** | TA1, TA2 (×4 mentions) | ✔ verified — no `.github/workflows`. Add a push/PR workflow: `./gradlew build test checkstyleMain checkstyleTest` on JDK 25, upload test report. This also automates the checks TA1 asked for. |
| P2 | **P0 (explicit MP3 warning)** | **Agentic workflow files not committed** (`superpowers:brainstorming`, `writing-plans`, `reviewed-plan`, etc.). TA2: *"commit all agentic workflows and skills used during development, including those cited in logs or reflections… similar omissions may be penalised in MP3."* | TA1, TA2 | ✔ verified — `.claude/skills/` has only 3 skills. Copy in every skill cited by `maintaining-project-state` and the logs/reflections, or rewrite those skills to be self-contained. |
| P3 | P1 | **Large integration commits** (`7c30139` 96 files, `a945c98` 55, `d1cbf4e` 83); vague messages ("record origin/main as merged"); `54ea419` repaired stale API refs after integration. | TA1, TA2 | MP3 convention: rebase/merge feature branches in small logical commits, run build+tests at every merge (CI enforces), descriptive messages. Add to `PROJECT_STATE.md` § Conventions. |
| P4 | P2 | **"How to Resume" lists paused sessions from completed workstreams.** | TA1 | Prune S1–S14 rows whose work is in § Workstreams/Done ledger (per AGENTS.md § 2). |
| P5 | P2 | Missing test at the **joined completion-and-filing boundary**. | TA1 | Part of B1 (write the failing test first). |

## 4. Documentation fixes

| # | Pri | Item | Raised by | Action |
|---|---|---|---|---|
| G1 | P1 | **User Guide: 8 screenshot placeholders remain** | TA1 | ✔ verified — `> :camera: Screenshot placeholder:` blocks at lines 28, 89, 260, 372, 382, 397, 458, 484, 536. Capture and link them. |
| G2 | P1 | **User Guide formatting/rendering:** tables not rendering as tables, emoji icons not rendering (GitHub Pages / Jekyll). | Peer 1 | Check kramdown config in `docs/_config.yml`, blank line before tables, replace `:camera:` shortcodes with plain text or real emoji. |
| G3 | P2 | User Guide factual errors: cancellation rules omit the **full refund for pending requests**; Host wallet section promises platform-fee rows, but they're recorded against the **System wallet**; approval dialog "Message to guest" (see B7). | TA1, Peer 3 | Correct after the code fixes land. |
| G4 | P1 | **Developer Guide missing the required Acknowledgements section.** | TA1 | ✔ verified — no match in `docs/DeveloperGuide.md`. Add (AI tools used, libraries, references, peers). |
| G5 | P1 | **Developer Guide drift:** default database selection, the budget filter, and Trip tabs differ from the implementation. | TA1 | Re-verify each against code and correct. Add a "compare guide to built app" step at each checkpoint. |
| G6 | P2 | **Reflections:** five-scenario comparison omits executable logic and raw results; "no-rework" claim overstated vs. ledger showing later repairs. | TA1 | Add concise method + results (or link logs) and soften the claim. Note: this is graded content; fix only if reflections are re-assessed in MP3. |

## 5. Suggested order of attack

1. **Foundation (do first, unblocks everything):** P1 (CI) → P2 (commit skills) → P3 convention.
2. **Correctness cluster** (share a root cause, one branch): B1 + D3 + P5, then B4 + B5 (date policy), D1 + B3, D2.
3. **Quick UI bugs:** B2 (+ test), B6, B7.
4. **Cleanup:** D4, D5, D6, P4.
5. **Docs last** (so they describe the fixed app): G1–G5; G6 optional.

Everything above except B1/D1/D2/D3 is small. B1 and D1–D3 touch settlement, events and
transactions, so per AGENTS.md § 3 treat them as **one workstream with a spec and plan**; the rest
are small fixes needing only Done-ledger lines.

## 6. Praise worth preserving (don't regress)

Cohesive role-specific GUI; layered architecture with Checkstyle + architecture tests; transactional
ledger with rollback tests (526 tests / 106 classes passing); strong agentic evidence
(`AGENTS.md`, project-state protocol, logs); feature breadth well beyond MP1.
