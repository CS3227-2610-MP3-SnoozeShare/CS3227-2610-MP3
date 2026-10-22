# UI

## Trigger and placement

The New and Edit listing forms retain the existing `Description` textarea, card, order, spacing,
validation, and save controls. A compact button labelled **AI Description Assistant** sits beside
the Description label and uses the existing `lucide-react` icon family with a two- or three-star
`Sparkles` icon. It must not add a second persistent editor or push the current form sections into
a different order.

## Modal layout

The button opens a modal without submitting the listing form. The modal has a title, a close
control, and two columns at desktop/tablet widths.

### Left column

- Top row: Writing style select (`Friendly`, `Professional`, `Persuasive`) and Target audience
  select (`Families`, `Couples`, `Business travellers`, `All travellers`).
- Second row: Description length radios (`Short`, `Medium`, `Long`) with visible ranges `50–74`,
  `75–124`, and `125–200` words.
- Existing description textarea, initialized from the form's current controlled value each time the
  modal opens. Edits here do not change the form until Apply.
- `Evaluate` and `Generate` buttons. Evaluate requires non-blank text; Generate permits blank text.

### Right column

- Analysis panel at the top. Each criterion is a row: Clarity, Completeness, Accuracy. The panel
  has distinct Current and Generated sections; Generated is hidden or empty for Evaluate.
- Read-only generated-description textarea below the analysis panel.
- `Apply` button is disabled until a validated generated description exists. Apply copies the text to
  the form description, closes the modal, and does not save the listing.

## States and interaction

| State | UI behavior |
|---|---|
| Initial | Current text is copied; defaults are Friendly, All travellers, Medium; analysis and generated text are empty; Evaluate/Generate are enabled according to validation. |
| Waiting | All selects, radios, textareas, Evaluate, Generate, Apply, and modal close are disabled; a visible spinner and `Evaluating…` or `Generating…` label are shown. |
| Evaluate success | Current analysis is populated; generated text and Generated analysis remain empty; Evaluate and Generate are available again. |
| Generate success | Current analysis is populated when current text was non-empty; generated text and Generated analysis are populated; Apply becomes enabled. |
| Gateway failure | No generated text or analysis is displayed from a degraded result; a safe inline alert uses the exact gateway-code mapping in [api.md](api.md); controls recover and the modal remains open. |
| Close/cancel | No form value changes; the modal closes. Browser Escape and the close control follow the same rule when not waiting. |
| Apply | Generated text replaces the form's description value, the modal closes, and the form's existing validation/save flow remains unchanged. |

At widths below 768px, the two columns stack in left-then-right order inside the modal to avoid
horizontal overflow. The modal is scrollable within the viewport; the page behind it is inert.
