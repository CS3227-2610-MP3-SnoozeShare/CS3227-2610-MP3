# Prompt changelog: `ticket.clarify`

One row per run. `run.mjs --changelog "what I changed"` appends the row and fills every number; you fill in the **Decision** and then write one or two lines of notes under "Findings". Keep one change per run so a row tells you what caused the difference.

The committed prompt (the text of `ClarifyPrompt` in the backend) must have a row here that says `KEEP` before the W-AI-AGENT branch merges (spec conventions R15).

## Findings and decisions

Newest first. Write what you learned, not what you did.

- The operator selected the v3 examples prompt for initial release because useful information for human Agents is the priority (C~20). The release text retains v3's examples and adds the approved P13 marker/round rules. Exact prompt hash `b30b9d3e`, full 21-ticket run: 84% schema-valid calls, 71.4% tickets without deterministic issues, 100% first-call `enough` accuracy, 60% topic coverage; one pre-screen hit and one `mustNotAsk` hit. Operator decision: KEEP; full conversations remain available in the result JSONL.
- v4 committed backend prompt, full 21-ticket run (one run per ticket): 79.2% schema-valid calls, 66.7% tickets without deterministic issues, 93.3% first-call `enough` accuracy, 59.7% question-topic coverage. Four tickets had schema failures; T14 had one `mustNotAsk` hit. The production-pinned prompt is not a clear winner from this single run; review all transcripts before choosing KEEP.
- Expanded comparison: the remaining 18 tickets, one run each per prompt (combined with the earlier T01/T10/T14 sample, all 21 cases have now been covered). On this batch v2 led contract validity (86.4%) and tickets without deterministic issues (77.8%); v3 led first-call `enough` accuracy (100%) and question-topic coverage (70.0%); v1 scored 68.4%, 33.3%, 80% and 35.4% on those measures. No universal winner: review the bounded transcripts before scoring or choosing KEEP. One run per case is directional evidence only.
- Preliminary comparison: T01/T10/T14, one run each. v1 produced no schema-valid outputs (all omitted `hostPoints`); v2 was 100% schema-valid but had one `mustNotAsk` hit; v3 was 100% schema-valid with no deterministic issues. Suggested manual scores for review: v2 T01 Questions 1/2, summary 2/2; T10 2/2, 2/2; T14 1/2, 2/2. v3 T01 2/2, 2/2; T10 2/2, 1/2 (omits the public-posting threat); T14 1/2, 2/2. No KEEP decision; transcript vetting pending.

## Runs

Columns: date, prompt file (sha8 of its text), model, the one change made, conversations run, replies that passed the contract, tickets with no issue, first-call `enough` accuracy, questions the model wrote that the screens would drop (promise or data request, before the screen), questions dropped by the screen in total, `mustNotAsk` violations, calls per conversation, average latency per call, decision (`KEEP`, `REVERT`, `TODO`), summary file.

Review every printed transcript before recording a decision. The optional Questions (0–2) and summary (0–2) suggestions are manual notes only; you choose the scores and the `KEEP`/`REVERT` decision. The runner does not automatically score summary factuality or completeness.

| Date | Prompt (sha8) | Model | Change | Conv. | Schema ok | No issue | Enough acc. | Pre-screen hits | Dropped | mustNotAsk | Calls/conv. | Latency | Decision | Summary file |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-10-23 | prompts/v1-neutral.txt (4929a812) | qwen3.5:9b | baseline | 3 | 0% | 0% | -% | 0 | 0 | 0 | 1 | 3924 ms | TODO | 2026-10-23T02-32-17_v1-neutral_qwen3.5-9b.summary.json |
| 2026-10-23 | prompts/v2-snoozy.txt (952f116d) | qwen3.5:9b | Snoozy rules | 3 | 100% | 66.7% | 100% | 1 | 1 | 1 | 1 | 4086 ms | TODO | 2026-10-23T02-32-44_v2-snoozy_qwen3.5-9b.summary.json |
| 2026-10-23 | prompts/v3-snoozy-examples.txt (f8508122) | qwen3.5:9b | Snoozy examples | 3 | 100% | 100% | 100% | 0 | 0 | 0 | 1 | 3315 ms | TODO | 2026-10-23T02-33-10_v3-snoozy-examples_qwen3.5-9b.summary.json |
| 2026-10-23 | prompts/v1-neutral.txt (4929a812) | qwen3.5:9b | expanded 18-case sample v1 | 18 | 68.4% | 33.3% | 80% | 0 | 0 | 1 | 1.06 | 4047 ms | TODO | 2026-10-23T02-50-48_v1-neutral_qwen3.5-9b.summary.json |
| 2026-10-23 | prompts/v2-snoozy.txt (952f116d) | qwen3.5:9b | expanded 18-case sample v2 | 18 | 86.4% | 77.8% | 91.7% | 1 | 1 | 0 | 1.22 | 3699 ms | TODO | 2026-10-23T02-52-30_v2-snoozy_qwen3.5-9b.summary.json |
| 2026-10-23 | prompts/v3-snoozy-examples.txt (f8508122) | qwen3.5:9b | expanded 18-case sample v3 | 18 | 72.7% | 66.7% | 100% | 0 | 0 | 0 | 1.22 | 3594 ms | TODO | 2026-10-23T02-54-16_v3-snoozy-examples_qwen3.5-9b.summary.json |
| 2026-10-23 | prompts/v4-snoozy-support-documents.txt (71c53e5a) | qwen3.5:9b | committed backend prompt, exact match pinned by ClarifyFeatureTest | 21 | 79.2% | 66.7% | 93.3% | 1 | 1 | 1 | 1.14 | 3877 ms | TODO | 2026-10-23T03-52-10_v4-snoozy-support-documents_qwen3.5-9b.summary.json |
| 2026-10-23 | prompts/v3-snoozy-examples.txt (b30b9d3e) | qwen3.5:9b | operator-selected v3 for initial release; explicit P13 marker behavior | 21 | 84% | 71.4% | 100% | 1 | 2 | 1 | 1.19 | 3492 ms | KEEP | 2026-10-23T04-13-09_v3-snoozy-examples-p13_qwen3.5-9b.summary.json |
