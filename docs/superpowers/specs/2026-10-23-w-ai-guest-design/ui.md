# UI

## Guest search page (`/guest/search`)

The AI search bar is added above the existing manual filter row. Both remain usable independently.

### AI search bar

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | A text input with placeholder text, a submit button | Text input (full width of the filter area), "Search with AI" button (disabled when input is empty) | Placeholder: "Describe what you're looking for..." | GUEST |
| Loading | The text input is disabled, the button shows a spinner, a subtle "Searching..." label | None interactive | "Searching..." | GUEST |
| Error (AI unavailable) | An inline alert below the AI bar with the fallback message; the manual filters remain usable | Dismiss the alert, or use manual filters | "AI search is temporarily unavailable. Please use the manual filters." | GUEST |
| Populated (AI results) | The AI explanation displayed in a card above the results grid; the extracted filters shown as tags/chips below the explanation; the listing grid shows the results | "Clear AI search" link to return to manual mode; each filter chip is read-only (not editable) | Explanation text from the AI response; filter chips like "City: Orchard", "Guests: 2", "Max $80/night" | GUEST |

### Interaction flow

1. Guest types a natural language query into the AI search bar (e.g. "apartment near Clementi for 3 people")
2. Guest clicks "Search with AI" or presses Enter
3. The frontend sends `POST /api/guest/ai/search` with `{ "query": "..." }`
4. While loading: input disabled, spinner on button, existing results cleared
5. On success (`aiAvailable: true`): display the explanation card, filter chips from `filters`, and the results grid
6. On degraded (`aiAvailable: false`): show the inline alert with the explanation; results are empty
7. "Clear AI search" returns to the manual filter view with no results preloaded

### Manual filter row (existing, unchanged)

The existing five filter fields (city, check-in, check-out, guests, max rate) and "Search" button remain below the AI search bar. Using the manual search clears any AI results and explanation. Using AI search does not populate the manual filter fields (the extracted filters are shown as read-only chips, not editable filter values).

### Layout

```
┌──────────────────────────────────────────────────────────┐
│  "Describe what you're looking for..."    [Search with AI] │
├──────────────────────────────────────────────────────────┤
│  City [____]  Check-in [__]  Check-out [__]  Guests [_]  │
│  Max rate [____]                              [Search]    │
├──────────────────────────────────────────────────────────┤
│  ┌─ AI Explanation Card (shown after AI search) ──────┐  │
│  │  "Looking for a 3-guest apartment near Clementi."  │  │
│  │  [City: Clementi] [Guests: 3]       [Clear AI search] │
│  └────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────┤
│  [Listing Card]  [Listing Card]  [Listing Card]          │
│  [Listing Card]  [Listing Card]  ...                     │
└──────────────────────────────────────────────────────────┘
```

Navigation: the search page is the existing `/guest/search` route; no new routes are added. The listing detail sheet (right-side drawer) is unchanged.
