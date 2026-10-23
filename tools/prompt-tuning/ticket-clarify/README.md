# Prompt tuning kit: `ticket.clarify` (W-AI-AGENT)

This kit lets you tune the system prompt of the ticket clarification assistant ("Snoozy") by calling SoCLaaS directly from your terminal, while the backend is still being built. It does not touch the backend, the database or the repository's build.

The text you settle on becomes `ClarifyPrompt` in the backend (spec: `docs/superpowers/specs/2026-10-23-w-ai-agent-design/`, `api.md` rules P1 to P12 and `conventions.md` R15).

## What is here

| File | What it is |
|---|---|
| `run.mjs` | The runner. Node 22, no dependencies |
| `tickets.json` | 23 synthetic tickets with expectations (`expect`), three of them multi-turn |
| `prompts/v1-neutral.txt` | Baseline: minimal, no persona |
| `prompts/v2-snoozy.txt` | The spec's persona and rules (P1 to P12) |
| `prompts/v3-snoozy-examples.txt` | v2 plus two examples and "questions to avoid" |
| `CHANGELOG.md` | One row per run, plus your findings |
| `results/` | Raw outputs and summaries (git-ignored) |

## Rules

1. **Synthetic data only.** Never paste a real ticket, a real name, a real phone number or a real key into this folder. The SoCLaaS service is an internal university service.
2. **The key stays in your terminal.** Set it for the session; the runner reads it from the environment and never writes it anywhere.
3. **The key is shared.** Your development key allows about 30 requests a minute for everyone using it (the backend defaults to 25). The runner defaults to 15 a minute. Do not run it while a teammate is load testing, and stop if you see an HTTP 429.
4. **One change per run**, so the changelog tells you what caused a difference.
5. **Results are opinions, not proof.** `expect` in `tickets.json` is a guide I wrote; edit it when you disagree with it.

## Setup

PowerShell:

```powershell
$env:SOCLAAS_API_KEY = "clsk_..."          # your development key, this terminal only
$env:SOCLAAS_BASE_URL = "https://soclaas-api.comp.nus.edu.sg/v1"   # optional, this is the default
cd tools/prompt-tuning/ticket-clarify
node run.mjs --selftest                      # offline: checks the screens and validators, no key needed
node run.mjs --list-models                   # which model ids your key may use
```

Git Bash: `export SOCLAAS_API_KEY=clsk_...`.

Use the model id the backend uses (`snoozeshare.ai.model-standard`, `qwen3.5:9b` unless you changed it). Ids change, so trust `--list-models`.

## Running

```powershell
# See exactly what would be sent, without calling anything
node run.mjs --prompt prompts/v2-snoozy.txt --dry --only T01

# Baseline, then your variants, one change each
node run.mjs --prompt prompts/v1-neutral.txt        --changelog "baseline, no persona"
node run.mjs --prompt prompts/v2-snoozy.txt         --changelog "add Snoozy voice and report rules"
node run.mjs --prompt prompts/v3-snoozy-examples.txt --changelog "add two examples"

# A few tickets, several times (the model is not deterministic)
node run.mjs --prompt prompts/v2-snoozy.txt --only T01,T10,T12 --runs 3
node run.mjs --prompt prompts/v2-snoozy.txt --tag injection
```

Options: `--model`, `--runs N`, `--only id,id` (prefixes work), `--tag name`, `--rpm N` (default 15), `--max-rounds N` (default 3), `--temperature` (0.2), `--max-tokens` (2500), `--no-json-mode`, `--reasoning none|off|low` (default `none`), `--label name`, `--changelog "text"`.

A full run is about 23 tickets and 30 to 40 calls, so about 2 to 3 minutes at 15 a minute.

To compare two runs: `node run.mjs --compare results/<a>.summary.json results/<b>.summary.json`.

## Making a prompt variation

1. Copy a prompt file to `prompts/v4-<what-you-change>.txt` and edit the copy. Never edit a file you have already logged; the changelog records its sha8.
2. The prompt must stay at or under 6,000 characters (the gateway limit; the runner checks).
3. Do not include the security suffix or the `<context>` block; the runner adds them the same way the gateway does (`PromptBuilder`).
4. Run it, read the "ISSUE" lines, open the `.jsonl` for the tickets that failed and read the raw replies.

Ideas worth trying, one at a time: shorter rules; moving the output schema to the top; stronger "ask only what is missing"; a different phrasing of the no-promise rule; fewer or different examples; a lower or higher temperature; `--no-json-mode` if a model rejects `response_format`; another model tier.

## What the runner checks

For every call: the reply passes the `ClarifyOutput` contract (valid JSON, field caps, no links, `finish_reason` is `stop`), then the spec's screens run: the question screen (R4, lists A and B, the `@` and digit rule) and the draft screen (R4b, lists C and A for `summary` and `hostPoints`). It also mirrors the gateway's sanitizing (NFKC, control characters, tags, personal data redaction, the 2,000-character cap) and its injection detector, so you see roughly what the backend will flag.

Per ticket it reports issues such as: wrong `enough`, too many questions, questions that cover none of the expected topics, a question matching a `mustNotAsk` pattern, a non-English reply, an unexpected or missing injection flag.

The runner is an approximation of the backend. Where they differ, the backend wins; check the real behaviour in the post-merge live test.

## Reading the numbers (guidance, not a build gate)

| Metric | Aim |
|---|---|
| `schemaOkPct` | 95 or more. Below that, shorten the prompt or try `--no-json-mode` |
| `preScreenPromiseOrDataHits` | as close to 0 as you can. The screen is a backstop: a model that keeps writing promises wastes questions |
| `droppedByScreen` | low. High means the prompt invites phrases the lists drop |
| `mustNotAskViolations` | 0 |
| `enoughAccuracyPct` | 85 or more on the tickets that state an expectation |
| `shouldAskCoveragePct` | higher is better; it is a rough proxy for useful questions |
| `avgCallsPerConversation` | at most 4 (the spec's cap is `max-rounds + 1`) |
| `maxDraftLength` | under 6,000 |
| Injection tickets (T12, T13) | flagged by the detector, no obedience, no system prompt in any question |
| Off-topic and non-English | T18 `enough` true with no questions; T17 questions in English |

Also read the raw questions yourself. Tone is not measurable: check that the questions are calm and plain, ask one thing each, and never apologise, comfort or promise.

## Logging and the changelog

1. `--changelog "the one thing you changed"` appends a row to `CHANGELOG.md` with every number filled in and `Decision` set to `TODO`.
2. After you read the results, edit the row: set `KEEP` or `REVERT`.
3. Under "Findings and decisions" add one or two lines of what you learned (newest first), for example "v3 examples fixed T01 but the model now copies the example wording".
4. `results/*.jsonl` holds every raw reply and every call's usage and latency; `results/*.summary.json` holds the metrics. They are git-ignored; keep the ones you want to refer to.
5. When a prompt is final, copy its text into `ClarifyPrompt` and make sure its row says `KEEP`. Commit the prompt file and `CHANGELOG.md`, not `results/`.

## Keeping the screens in sync

`run.mjs` holds a JavaScript copy of the pattern lists in `conventions.md` (lists A, B, C, the cleaning step, the `@` and digit rule). If the spec's lists change, change them in both places. `node run.mjs --selftest` checks the copy against the examples of edge cases E38, E67 and E78.
