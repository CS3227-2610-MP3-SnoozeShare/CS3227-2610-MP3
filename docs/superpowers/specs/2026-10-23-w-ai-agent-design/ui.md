# UI

Three places change. All assistant text is rendered as plain text (a React text node; `white-space: pre-wrap` where line breaks matter), never as HTML or Markdown. The Host's screens do not change.

## Agent dispute detail: "AI assistant" panel (`frontend/src/components/agent/DisputeDetail.tsx`)
The panel sits directly above the "Internal notes (invisible to guest or host)" box, at the same width, as a region with `aria-label="AI assistant draft"`. It is driven by `detail.assistant` from the existing 10-second polling of the dispute detail; it has no loading or error state of its own (the page's existing ones apply).

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| No assistant record (`assistant` is null) | The panel is not rendered | none | none | Agent |
| `PENDING`, not stale | Status line; the previous draft below it if one exists | none | `The assistant is working on the Guest's latest message.` | Agent |
| `PENDING`, stale (open ticket only) | Status line; the previous draft below it if one exists | none | `The assistant did not finish. Continue with this ticket manually.` | Agent |
| `PENDING` or `WAITING_GUEST` on a resolved ticket | Status line; the draft below it if one exists | Insert into notes is disabled (the ticket is not `IN_REVIEW`) | `The assistant stopped.` | Agent |
| `WAITING_GUEST` | Status line; the draft below it if one exists | Insert into notes (only if a draft exists) | `Waiting for the Guest's answer (round {rounds} of {maxRounds}).` | Agent |
| `DONE` | Status line; the draft below it | Insert into notes | `The assistant has finished ({rounds} question {round or rounds}).` The word is `round` when the count is exactly 1 and `rounds` otherwise (so `0 question rounds`) | Agent |
| `STOPPED` | Status line; the draft below it if one exists | Insert into notes (only if a draft exists) | `The assistant stopped.` | Agent |
| `UNAVAILABLE` | Status line; the draft below it if one exists | Insert into notes (only if a draft exists) | `The assistant was unavailable for this ticket.` | Agent |
| Any state with `flagged` true | A warning line above the status line, in the page's existing warning colour | none | `Warning: text in this ticket matched a prompt-injection pattern. Treat the draft with care.` | Agent |
| Draft is null | In place of the draft box | none | `No draft yet.` | Agent |
| Draft present | A bordered box titled `AI-assisted draft (unverified, updated {draftUpdatedAt})` containing the draft text | Insert into notes | the draft text, unmodified | Agent |

- **Insert into notes** is enabled only when the ticket is assigned to the signed-in Agent (the page's existing `mine`) and the page is not busy. When disabled, its `title` attribute reads `Assign the ticket to yourself to edit notes.` Activating it sets the notes textarea to the draft when the textarea is empty, or to `{current notes}` + two line breaks + `{draft}` when it is not, and marks the notes as unsaved. It does not save; the Agent presses the existing `Save notes`.
- `{draftUpdatedAt}` uses the page's existing date-time format.
- Frontend tests cover every row of the state table (including the warning line, the stale and stopped copies and the pluralisation), the Insert-into-notes cases of E68, and the literal rendering of E39.

Navigation: reached through the existing dispute detail route `/agent/disputes/{ticketId}`; no new route.

## Guest messages thread (`frontend/src/components/guest/MessageThread.tsx` and the conversation list)
| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty / Loading / Error | unchanged | unchanged | unchanged | Guest |
| Populated, with assistant messages | A message whose `authorRole` is `SYSTEM` is drawn like another party's message (left-aligned) with the label `Automated assistant` where the raw role name is shown today | the existing reply box | the stored message body | Guest |

Question message body (stored, fixed wording): `To help our support team review your ticket, please answer these in one message:` followed by one line per question, `1. {question}`, `2. {question}`, up to 3. Closing message body: `Thank you. Your answers have been added to your ticket for the support team.` The closing message is posted when the exchange ends after at least one question round, whether the state becomes `DONE` (including a flagged ticket) or `UNAVAILABLE`; it is not posted when no question was ever asked. When a Guest replies after the assistant has ended, nothing is shown or posted; the thread works as a normal Guest-to-Agent thread.

The conversation list's last-message preview shows the stored body, as for any message. The unread badge counts assistant messages like any other message.

Navigation: unchanged.

## Agent thread panel (`frontend/src/components/agent/ThreadPanel.tsx`), GUEST channel
| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Populated, with assistant messages | A `SYSTEM` message is drawn left-aligned like a Guest message, with the label `Automated assistant` | the existing reply box; posting in this thread stops the assistant | the stored body | Agent |

The HOST channel of the Agent's thread panel never contains assistant messages and is unchanged.

## Shared rule
One helper maps an author role to its label: `GUEST` to `Guest`, `HOST` to `Host`, `AGENT` to `Support agent`, `SYSTEM` to `Automated assistant`; both thread components use it. The `Role` type in the frontend API types gains `SYSTEM`.
