---
name: logging-agent-interactions
description: Use when explicitly invoked to record or retrieve material interactions for a specific chat and agent session.
---

# Logging Agent Interactions

## Purpose

Maintain a concise, append-only record of material user/agent interactions for this repository.
`PROJECT_STATE.md` remains the source of truth for current decisions, workstream status, and
handoff state; this log preserves interaction evidence and context.

## Chat identity and transcript access

This skill distinguishes a persisted **chat** from an agent **session**. A chat may be resumed
after several days, may be compacted, or may contain multiple agent sessions. The model's active
context is not necessarily the complete persisted chat transcript.

Every log must identify the chat it belongs to. Prefer the host's stable conversation/thread ID.
If the host does not expose one, use an explicit user-supplied chat URL or recognizable chat
anchor and mark the identity as uncertain; never guess from the newest file, branch, or workstream.

Use the host's transcript or thread-retrieval capability before reconstructing a log. In Codex,
use the thread retrieval tool with the exact thread ID when it is available, following pagination
or cursors until the requested history has been read. A visible chat in the UI is not by itself
evidence that the full transcript is in the model context.

When the user asks to retrieve logs from a specific chat:

1. Resolve the exact chat key or thread ID.
2. Fetch the persisted transcript, rather than relying on current context, memory, or compaction.
3. Find every session file with that chat key and follow its continuation links.
4. Read the complete linked chain before summarizing or extracting interactions.
5. If the chat identity or transcript cannot be fetched, say what is missing and do not silently
   substitute the latest branch log or a similarly named chat.

If the retrieved transcript differs from the active context, the retrieved transcript is the
source for historical user-input wording. Preserve the distinction between fetched history and
information remembered from the current context.

## When to run

Invoke this skill manually once at the end of an agent session (for example, with
`$logging-agent-interactions` or the host's equivalent explicit skill invocation). Do not invoke it
automatically on every turn. At invocation time, review the session transcript and create an entry
only when a user input materially affects the project, including when it:

- requests or changes work, requirements, scope, priorities, or constraints;
- makes, approves, rejects, or reverses a design or process decision;
- approves a specification, plan, implementation step, branch strategy, or handoff;
- reports a bug, failure, blocker, or unexpected behavior;
- requests status, review, verification, or handoff information; or
- resolves a question recorded in `PROJECT_STATE.md`.

Do not log greetings, thanks, repeated non-material acknowledgements, casual discussion with no
project consequence, or routine tool output. A short response such as “yes” is material when it
approves a plan or decision. If the session contains no material interactions, do not create an
empty log file.

## Session file

Use one file for the whole agent session. Create it directly under `logs/` using the established
timestamped filename convention:

```text
logs/YYYY-MM-DD_HH-mm-ss_<branch>.md
```

The filename timestamp format is exactly `YYYY-MM-DD_HH-mm-ss`. Replace `/`, spaces, and other unsafe
branch characters with `-`; preserve the branch name in a recognizable slug. The filename stem is
the canonical session key, so do not use a copied numeric `S2`-style label as the log identity.
Numeric `S` labels in `PROJECT_STATE.md` are human-readable handoff labels only. If two sessions
must run on the same branch at the same second, append a short unique nonce while retaining the
required timestamp and branch portions. If session metadata is uncertain, use the safest unique
filename and state the uncertainty in the entry instead of blocking project work.

Do not use or recreate `logs/LLM_interactions.md`. There is intentionally no consolidated log yet.
Parallel sessions get separate files because their session-start timestamps and/or branch slugs
must make the targets distinct.

## Session-file and entry format

Start the file with this session-level header, then append exactly one entry for each material user
input. Entries must be kept in chronological order and must not contain timestamp metadata. Never
overwrite existing entries.

```markdown
# Agent Session Interaction Log

- Session key: `2026-09-23_14-44-51_w1`
- Chat key: `chat:<stable-conversation-id>`
- Transcript source: `host thread retrieval` or `user-provided transcript`
- Continuation of: `none` or a prior session key
- Branch: `w1`
- Workstream: W1

## 2026-09-23_14-44-51_w1__01 — request

### User input

Complete user input, verbatim except redactions.

### Agent response summary

- Factual action taken.
- Files changed and tests run.
- Decisions, blockers, or follow-up, when relevant.

### Outcomes

- Files changed: `path/to/file`
- Tests: `not run` or exact command/result
- Blockers: `none` or the blocking question
- Follow-up: next concrete action
```

The interaction ID must be unique within the repository and include the canonical session key plus
an entry sequence, for example `2026-09-23_14-44-51_w1__01`. Use a concise type such as `request`,
`decision`, `approval`, `blocker`, `failure`, `verification`, `status`, or `handoff`.

## Redaction

Preserve the user's wording except for high-confidence secrets and clearly private personal
information. Redact passwords, API keys, access tokens, private keys, and similar credentials with
explicit markers such as `[REDACTED: API key]`. Do not repeat a redacted value in the agent
summary. Do not guess that ordinary project names, requirements, or source code are confidential.

## Manual logging procedure

When explicitly invoked to record a session:

1. Identify the chat key, branch, session key, and workstream.
2. Fetch the persisted transcript for the exact chat key when the host provides retrieval. Do not
   assume the active context contains the complete history.
3. Review the complete retrieved transcript and select material user inputs.
4. Redact only high-confidence confidential values.
5. Create the single session file, if at least one material input exists.
6. Append exactly one entry for each selected material input in chronological order, without
   timestamp metadata.
7. Summarize each response factually; do not claim work that did not happen.
8. Verify that every entry exists in the session file and record any transcript gap in the header
   or Outcomes section.

If writing fails, report the logging failure to the user, continue the requested project work, and
do not claim successful recording.

If transcript retrieval fails, report that the log is incomplete and identify the unavailable chat
or cursor. Do not fill missing user inputs from memory or invent a reconstructed transcript.

## Examples

Material:

```text
User: Store this as part of W1 and use per-session SGT files.
Result: Append one entry describing the approved scope and resulting files.
```

Not material:

```text
User: Thanks.
Result: Do not create an entry.
```

Redaction:

```text
User: Test with API key sk-example-secret.
Recorded: Test with API key [REDACTED: API key].
```

Session target:

```text
Session on branch w1, with entries ordered chronologically
→ logs/2026-09-23_14-44-51_w1.md
```
