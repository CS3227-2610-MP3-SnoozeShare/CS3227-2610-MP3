# UI

## Trigger and placement

The New and Edit listing forms retain the existing `Description` textarea, card, order, spacing,
validation, and save controls. A compact button labelled **AI Description Assistant** sits beside
the Description label and uses the existing `lucide-react` icon family with a two- or three-star
`Sparkles` icon. It must not add a second persistent editor or push the current form sections into
a different order.

The trigger is disabled until the form has a non-whitespace title, a selected property type,
positive max guests, bedrooms, bathrooms, check-in time, and check-out time. The disabled trigger
has a compact, website-styled helper popover anchored to the button. It appears on pointer hover or
keyboard focus, uses an info icon and muted popover styling, and explains that all seven listing
details are required before AI assistance can be used. The rule applies to both New and Edit forms
and is recalculated as the draft changes.

## Modal layout

The button opens a modal without submitting the listing form. The modal has a title, a close
control, and two columns at desktop/tablet widths.

### Left column

- Top row: Writing style select (`Friendly`, `Professional`, `Persuasive`), Target audience select
  (`All travellers`, `Families`, `Couples`, `Business travellers`), and Description focus select
  (`Balanced`, `Comfort`, `Practicality`).
- Generation uses a fixed 50–150-word target; no description-length control is shown.
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
| Initial | Current text is copied; defaults are Friendly, All travellers, Balanced; analysis and generated text are empty; Evaluate/Generate are enabled according to validation. |
| Waiting | All selects, radios, textareas, Evaluate, Generate, Apply, and modal close are disabled; a visible spinner and `Evaluating…` or `Generating…` label are shown. |
| Evaluate success | Current analysis is populated; generated text and Generated analysis remain empty; Evaluate and Generate are available again. |
| Generate success | Current analysis is populated when current text was non-empty; generated text and Generated analysis are populated; Apply becomes enabled. |
| Gateway failure | No generated text or analysis is displayed from a degraded result; a safe inline alert uses the exact gateway-code mapping in [api.md](api.md); controls recover and the modal remains open. |
| Close/cancel | No form value changes; the modal closes. Browser Escape and the close control follow the same rule when not waiting. |
| Apply | Generated text replaces the form's description value, the modal closes, and the form's existing validation/save flow remains unchanged. |

At widths below 768px, the two columns stack in left-then-right order inside the modal to avoid
horizontal overflow. The modal is scrollable within the viewport; the page behind it is inert.
