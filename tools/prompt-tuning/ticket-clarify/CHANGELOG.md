# Prompt changelog: `ticket.clarify`

One row per run. `run.mjs --changelog "what I changed"` appends the row and fills every number; you fill in the **Decision** and then write one or two lines of notes under "Findings". Keep one change per run so a row tells you what caused the difference.

The committed prompt (the text of `ClarifyPrompt` in the backend) must have a row here that says `KEEP` before the W-AI-AGENT branch merges (spec conventions R15).

## Findings and decisions

Newest first. Write what you learned, not what you did.

- (nothing yet)

## Runs

Columns: date, prompt file (sha8 of its text), model, the one change made, conversations run, replies that passed the contract, tickets with no issue, first-call `enough` accuracy, questions the model wrote that the screens would drop (promise or data request, before the screen), questions dropped by the screen in total, `mustNotAsk` violations, calls per conversation, average latency per call, decision (`KEEP`, `REVERT`, `TODO`), summary file.

| Date | Prompt (sha8) | Model | Change | Conv. | Schema ok | No issue | Enough acc. | Pre-screen hits | Dropped | mustNotAsk | Calls/conv. | Latency | Decision | Summary file |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
