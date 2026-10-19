---
name: writing-specs-lite
description: Use when a feature-level change needs a design spec before any plan or code (AGENTS.md § 3 "Big"), or when the operator says write/draft a spec. A lightweight alternative to superpowers brainstorming - produces a small set of focused spec files (problem, data model, APIs, conventions, edge cases, implementation order with a dependency table), reviewed from several perspectives and sized to the change's risk.
---

# Writing Specs (lite)

A spec says **what the system must be and how it must behave**, never **how to build it step by step** (that is the plan). It is written so that an implementer has no decision left to make by guessing.

**Announce at start:** "I'm using writing-specs-lite to write the spec."

## Hard rules

1. **Not a mega-document.** One short index file plus one file per component (below). No component file should need scrolling to understand.
2. **No implementation detail.** No code bodies, no step lists, no "first do X then Y" outside § Implementation order. Signatures, table columns, JSON shapes and error codes are contract, not implementation, and belong here.
3. **No discretion left to the agent.** Every choice the implementer would otherwise make (a limit, a default, a status code, an ordering, an error message, a library) is either decided in the spec or listed as an open `Q` in the index and resolved with the operator before the spec is approved. Banned phrasing: "appropriate", "as needed", "reasonable", "etc.", "and so on", "should handle errors", "TBD".
4. **Not one perspective.** Run the review pass (below) before presenting it as done.
5. **Risk-sized.** State the risk tier first; it sets how deep Security and Edge cases must go.
6. **Respect recorded decisions.** See step 1.

## Process

1. **Read the constraints.** Read `PROJECT_STATE.md` in full (and the branch state): § Known Gaps, "Explicitly out of scope", § Conventions, § Architecture (area *Now* + decision rows) and § Decisions. Anything the request collides with is handled by `AGENTS.md` § 4: stop, name the entry, ask **keep** or **reverse**. Seed the spec's non-goals from those entries and cite them by ID.
2. **Ground it in the code.** Read the code the change touches (service, repository, schema, UI entry points). Specify against what exists; name real classes, tables and migrations. Never reconstruct project history from source (`AGENTS.md` § 1).
3. **Clarify, one question at a time**, only about things the code and state files cannot answer: purpose, who is affected, acceptance, risk. Prefer multiple choice. Stop asking once every `Q` is closed.
4. **Settle the risk tier** (see Risk tiers) with the operator in one sentence.
5. **Write the files** (see Layout and Components). Present the index and each component to the operator in turn; fix as they react.
6. **Review pass** (see Perspectives). Fix findings in the files, not in chat.
7. **Self-check** against the checklist at the bottom.
8. **Operator approves** the spec. Then record it (Recording) and hand off to `writing-plans-lite`.

## Layout

Spec location follows `AGENTS.md` § 3; the folder holds the components:

```
docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md      <- index (always)
docs/superpowers/specs/YYYY-MM-DD-<topic>-design/
    requirements.md      <- only when functional + non-functional requirements exceed 6
    data-model.md        <- when data or state changes
    api.md               <- when any interface (service method, endpoint, event, CLI) is added or changed
    conventions.md       <- when the change sets or applies naming/layout/error/format rules
    edge-cases.md        <- always
    implementation-order.md  <- always
```

Omit a file only if it truly has nothing to say, and say so in the index ("No API changes: <why>"). Never create an empty file.

## The index (`...-design.md`)

Short. It must stand alone as the summary a reviewer reads first.

```markdown
# <Topic> Design

- **Status:** Draft | In review | Approved (operator name, date)
- **Risk tier:** Low | Medium | High - <one sentence why>
- **Workstream:** <ID> | **Plan:** <link once written>
- **Constraints consulted:** <PROJECT_STATE entries cited by ID: Known Gaps, Conventions, decisions>

## 1. Problem
<What is wrong or missing, who is affected, evidence. 1-2 paragraphs.>

## 2. Goals
Ordered logically (a requirement that others depend on comes first). Each is testable.
**Functional**
- F1 ...
**Non-functional** (performance, security, reliability, usability, maintainability; with numbers)
- N1 ...
(If there are more than 6 combined, move this section to `requirements.md` and keep only a one-line summary and link here.)

## 3. Non-goals
- <thing deliberately not done> - <why; cite PROJECT_STATE ID if recorded>

## 4. Components
Links to each file in the design folder with a one-line summary, or "none: <why>".

## 5. Open questions
| ID | Question | Blocks | Owner |
Must be empty before status is Approved.

## 6. Decisions made in this spec
| ID | Decision | Why / rejected alternative | Asked by |
```

Spec-local IDs use provisional form (`F1`, `N1`, `Q1`, `SD1`); they are not permanent project IDs and must not be cited outside the spec folder and its branch state.

## Components

**requirements.md** - the F/N lists from the index, each with an acceptance criterion ("Given/When/Then" or a measurable threshold). Order so dependencies come first.

**data-model.md** - *logical* model, not DDL: entities, attributes with type/nullability/default/range, keys, relationships and cardinality, ownership, lifecycle/state machine (states, legal transitions, who may trigger each), invariants ("sum of X equals Y"), retention, migration/backfill impact on existing rows, and which `PROJECT_STATE` § Architecture areas change. A small ER or state diagram (Mermaid) is welcome when it removes prose.

**api.md** - for each operation: name/signature, caller and required role, inputs with validation rules and limits, outputs, errors (condition -> error type/message), idempotency, transaction boundary, events emitted, and compatibility with existing callers. Include **security** per operation: authorization rule, what must never be logged or returned, input trust. Table form preferred.

**conventions.md** - the rules this change follows or introduces: naming, package/layer placement (check against `LayerDependencyTest`/`UiDependencyTest`), error handling and message style, money/date/time formats, audit-row requirements, test conventions. Cite existing § Conventions entries rather than restating; list only new or applied-here rules.

**edge-cases.md** - table, grouped by theme (input, state, concurrency, failure, permission, time/clock, data volume, migration). Columns: *Scenario*, *Expected behaviour (exact)*, *Why it matters*, *Test layer* (unit / service / UI). Must cover the non-happy path of every F requirement and every API error. Scale to risk tier.

**implementation-order.md** - the logical sequence of tasks that the plan will mirror. Each task: ID (`T1`...), one-line outcome, what it delivers that others rely on, requirements it satisfies. Then the dependency table:

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Schema + migration | - | - | 1 |
| T2 Repository | T1 | T3 | 2 |
| T3 Domain policy | - | T1, T2 | 1 |

Rules: a task depends on another only if it needs its *output or interface*; tasks in the same wave share no files; mark the wave a task can start in, and note shared-file hazards ("T4 and T5 both edit `AppContext`: serialize"). No step-level instructions here.

## Risk tiers

Decide from the change, not the feature name. Record the tier and why in the index.

| Tier | Triggers | Depth required |
|---|---|---|
| Low | Cosmetic/UI-only, no data, no money, no permissions | Edge cases: input + state basics. Security: one line stating nothing changes |
| Medium | New or changed data, state transitions, new service API, new dependency | Full api.md security per operation; edge cases across all themes; migration notes |
| High | Money/ledger/escrow, role or permission rules, account governance, data migrations touching existing rows, anything irreversible | Everything in Medium plus: threat list (who could abuse this, how), failure/rollback behaviour for every multi-step mutation, audit trail requirements, a concurrency analysis, and an explicit "what could corrupt data" section in edge-cases.md |

SnoozeShare specifics to check: auth is mocked, so role separation lives in service code - every operation states which `Role` may call it and where it is enforced; any wallet change goes through `LedgerWriter` in a `TransactionManager` transaction with its audit rows.

## Perspectives (review pass)

A spec written from one point of view hides that point of view's blind spots. Before asking for approval, review the drafted files from each lens below. Prefer dispatching one fresh subagent per lens with the spec folder and `PROJECT_STATE.md` constraints (they review, they do not edit); if subagents are unavailable, do each lens as a separate, explicitly labelled pass. Skip Security-as-attacker only for Low tier.

| Lens | Asks |
|---|---|
| **Implementer** | Could I build this with zero questions? Any unstated default, ambiguous term, missing error, contradiction between files? |
| **Tester / QA** | Is every requirement observable and testable? Which edge case is missing? Which expected behaviour is not exact? |
| **Attacker / abuser** | Who can call this with the wrong role, bad input, or in the wrong state? What can be replayed, raced or forged? |
| **Maintainer / future reader** | Does it respect Architecture and Conventions? Does it quietly reverse a Known Gap or decision? What will surprise someone in six months? |
| **User / operator** | Does it solve the stated problem? Is anything in scope the operator did not ask for? |

Triage findings: fix blocking ones in the files; turn judgement calls into `Q`s for the operator; record rejected findings with a reason under § Decisions made in this spec. Note in the index which lenses ran.

## Recording (AGENTS.md § 2-3)

On the feature branch, in the same commit as the spec or the next:
- Add/update the workstream row in the **branch state** (`Spec'd`, spec link, Guide `-`), and a Changes line.
- Record the operator's scoping decisions and rejected options as provisional decisions (`C~n`) with who asked and why.
- Commit the spec folder on its own (`docs: spec for <topic>`), separate from code.

## Self-check before handoff

- [ ] Risk tier stated; depth matches it.
- [ ] Every `PROJECT_STATE` Known Gap / out-of-scope / decision relevant to the topic is cited, and none is contradicted (or the operator chose "reverse" and it is recorded).
- [ ] Goals are ordered, testable, and each has an acceptance criterion; >6 moved to `requirements.md`.
- [ ] Non-goals listed.
- [ ] No implementation detail outside `implementation-order.md`; no code bodies.
- [ ] Banned vague words absent; no open `Q`.
- [ ] Every API error and every F requirement has an edge-case row.
- [ ] Dependency table present; waves have no shared files.
- [ ] Review pass ran; lenses noted; findings resolved or recorded.
- [ ] Files are separate and each is short.
