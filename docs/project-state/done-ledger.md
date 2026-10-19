# Done Ledger — SnoozeShare

The full chronological record of every change, newest first. One line each: what changed and
why, with a commit, plan or artifact as the reference. Not a copy of `git log`.

Iteration 2 started; earlier entries: [docs/project-history/iteration-1/done-ledger.md](../project-history/iteration-1/done-ledger.md)

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-19 | Fixed the first CI failure: `GuestUiSnapshotTest.messagesUseSmallerTypeAndAStatusPillOnlyForTickets` compared the fixed 36px status pill with the New Ticket button, whose height follows font metrics (34px on the Linux runner, 36px on Windows). The button now has `minHeight`/`prefHeight` 36 in `guest-messages.fxml`. Correction: my earlier note that CI had run green was wrong — the `build` job had failed on this test (the run I watched ended with the Checkstyle job output) and the Pages job was the one that passed. Local `.\gradlew build` is green | MP3-0 | this commit |
| 2026-10-19 | Moved the MP2-era specs and plans (W1–W16, 41 files) into `mp2 archive/` subfolders of `docs/superpowers/specs/` and `plans/`; the MP3 correctness-cluster spec and plan stay at the top level. Every reference was updated (`%20` in link targets): the iteration 1 state and ledger, `docs/Reflections.md`, and the cross-links inside the moved files. Logs under `logs/` and the `.superpowers/` work log keep the old paths as history | MP3-0 | owner move + this commit |
| 2026-10-19 | Refreshed `PROJECT_STATE.md` into iteration 2: iteration 1 (state, 174-entry ledger, disposition table `REFRESH.md`) archived under `docs/project-history/iteration-1/`; the new file keeps the architecture, conventions, Known Gaps, standing decisions, live deviations and the open MP3-0 row (269 lines in place of 917) | MP3-0 | owner-approved refresh |

**Archive:** [iteration 1](../project-history/iteration-1/done-ledger.md)
