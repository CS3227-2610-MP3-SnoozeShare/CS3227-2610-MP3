---
name: executing-plans-lite
description: Use when implementing a plan written with writing-plans-lite - task by task in dependency order, ticking the plan's checklist, keeping the branch state current, running the lint script, and closing out for owner review. The execution counterpart of writing-plans-lite; use it instead of superpowers executing-plans or subagent-driven-development.
---

# Executing Plans (lite)

Implements a plan exactly as written, keeps the record current as it goes, and stops when reality disagrees with the plan.

**Announce at start:** "I'm using executing-plans-lite to implement the plan."

## Preconditions (stop if any fails)

- Spec status is `Approved`; the plan has been reviewed by the operator; you are on a feature branch (never `development` or `main`).
- Read `PROJECT_STATE.md` and the branch state in full. Your session has a row in the branch state's Sessions table.
- Lint passes: `node .claude/skills/writing-specs-lite/scripts/lint.mjs plan <plan.md>` shows no errors.
- The working tree is clean and `<BUILD>` is green before the first task (record the baseline; a red start is not yours to hide).

## Loop (per task, in the order and waves of the Task order table)

1. **Pick the next task**: the first task whose Status is not `Done` and whose dependencies are all `Done`. Set its Status to `In progress`; set your session row's Doing/Next action (task ID + step number); commit nothing yet.
2. **Do the checklist in order.** Tests first, as the steps say. Use the snippets in the plan as the intended shape, not as text to paste blindly; adapt to the real code, and when you do, note why in the commit message.
3. **Tick each box** (`- [ ]` to `- [x]`) as the step finishes, in the plan file.
4. **Edge cases**: every `E` ID assigned to the task has a test, and the test name or a comment cites the ID.
5. **Verify**: run the task's tests, then `<BUILD>`. Do not continue on red. If a failure is not caused by your change, record it as a Deviation and ask; do not skip or weaken a test.
6. **Record**: set Status `Done`, update the branch state's Progress (position, about 15 words) and add a Changes line.
7. **Commit** the task: code, tests, the ticked plan and the branch-state update together (`AGENTS.md` § 6; a handful of files; no `git add -A`). Message `type: what and why (workstream)`; no provisional IDs.
8. After each wave: run `lint.mjs plan <plan.md>` and `<BUILD>` again.

## Parallel waves

Dispatch concurrent tasks (same wave, marked concurrent, sharing no files) to implementer subagents, one per task, each in its own worktree or isolated checkout where possible. Each subagent gets: the task's section, the spec files it cites, `PROJECT_STATE.md` constraints, and the rule that it ticks only its own boxes, commits only its own files and adds its own session row. The lead then integrates in task order, resolves conflicts at the merge (build first), and runs the wave check. Never dispatch a task whose dependencies are not `Done`.

## When reality diverges

Stop at the first sign of: a path or symbol that does not exist, a spec rule that cannot be met, a test the plan did not anticipate, an edge case not in the spec, a decision the operator has not made. Then:
1. Write a Deviation (`D~n`) in the branch state: what, the consequence, what you did.
2. Plan-only fix (extra step, reorder within a task): make it, and record it as a Changes line.
3. Anything that changes behaviour, a task boundary, dependencies or a requirement: amend the spec first (`writing-specs-lite` change control; a Decision `C~n` in the branch state), then the plan, then re-run the lint script, then continue. If the operator must decide, set the session `Blocked - needs human` with the question as `Q~n`, and stop.
Never silently change scope, weaken a test, or tick a box for work not done.

## Close-out (the plan's final task)

1. `node .claude/skills/writing-specs-lite/scripts/lint.mjs plan <plan.md> --closeout` has no errors (all boxes ticked, all tasks Done).
2. `<BUILD>` is green; each F/N has a passing test or a recorded manual check (state plainly which checks you could not run).
3. Run a code review of the branch diff (the `code-review` skill) and fix confirmed findings, or record them as Deviations.
4. Branch state: workstream `In review`, session rows deleted, Changes lines complete, new Known Gaps, Conventions and Deviations written, Handover items marked, `guide out of date: <what>` if the guide is affected.
5. Tell the project owner the branch is ready. Never set `Approved`, never merge.

## Resuming a half-done plan

The resume point is the first unticked box of the first task whose Status is not `Done`; your session row's Next action should name it. Re-read the branch state and `git log` first. If the plan or spec changed since, re-lint and re-read the amended tasks before continuing.
