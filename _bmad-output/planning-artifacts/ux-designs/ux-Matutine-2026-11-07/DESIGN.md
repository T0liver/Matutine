---
name: Matutine
description: Newsprint front page for a morning edition, with quiet Material chrome around it.
status: final
sources:
  - docs/project-brief.md
  - _bmad-output/planning-artifacts/decision-freeze.md
updated: 2026-11-07
colors:
  paper: '#F6F1E6'
  ink: '#1A1612'
  ink-secondary: '#4E4942'
  rule: '#C9C1B4'
  accent: '#7A2424'
  on-accent: '#FFFFFF'
  error: '#8B1E1E'
  chrome: '#EFEBE3'
  field: '#FFFCF6'
typography:
  masthead:
    fontFamily: Newsreader
    fontSize: 40px
    fontWeight: '500'
    lineHeight: '1.05'
    letterSpacing: -0.02em
  masthead-compact:
    fontFamily: Newsreader
    fontSize: 32px
    fontWeight: '500'
    lineHeight: '1.05'
    letterSpacing: -0.02em
  lead:
    fontFamily: Newsreader
    fontSize: 34px
    fontWeight: '500'
    lineHeight: '1.15'
    letterSpacing: -0.015em
  lead-compact:
    fontFamily: Newsreader
    fontSize: 28px
    fontWeight: '500'
    lineHeight: '1.15'
  story:
    fontFamily: Newsreader
    fontSize: 22px
    fontWeight: '500'
    lineHeight: '1.25'
  note:
    fontFamily: Newsreader
    fontSize: 18px
    fontWeight: '500'
    lineHeight: '1.3'
  briefing:
    fontFamily: Source Serif 4
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.45'
  body:
    fontFamily: Source Serif 4
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.45'
  meta:
    fontFamily: Source Serif 4
    fontSize: 13px
    fontWeight: '400'
    lineHeight: '1.35'
  kicker:
    fontFamily: Source Serif 4
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: 0.08em
  typewriter:
    fontFamily: IBM Plex Mono
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.55'
  chrome-body:
    fontFamily: Source Serif 4
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.4'
rounded:
  none: 0px
  sm: 4px
  md: 8px
  full: 9999px
spacing:
  '1': 4px
  '2': 8px
  '3': 12px
  '4': 16px
  '5': 24px
  '6': 40px
  margin-compact: 16px
  margin-wide: 48px
  column-gap: 24px
components:
  masthead:
    background: '{colors.paper}'
    color: '{colors.ink}'
    font: '{typography.masthead}'
    rule: '{colors.accent}'
    radius: '{rounded.none}'
  sample-line:
    color: '{colors.accent}'
    font: '{typography.kicker}'
  briefing:
    color: '{colors.ink}'
    font: '{typography.briefing}'
  story:
    color: '{colors.ink}'
    meta: '{colors.ink-secondary}'
    lead-font: '{typography.lead}'
    story-font: '{typography.story}'
    note-font: '{typography.note}'
    body-font: '{typography.body}'
  calendar-block:
    color: '{colors.ink}'
    meta: '{colors.ink-secondary}'
    rule: '{colors.rule}'
    title-font: '{typography.story}'
    body-font: '{typography.body}'
  task-block:
    color: '{colors.ink}'
    meta: '{colors.ink-secondary}'
    rule: '{colors.rule}'
    title-font: '{typography.story}'
    body-font: '{typography.body}'
  typewriter:
    background: '{colors.paper}'
    color: '{colors.ink}'
    font: '{typography.typewriter}'
    radius: '{rounded.none}'
  button:
    background: '{colors.ink}'
    color: '{colors.paper}'
    radius: '{rounded.sm}'
    font: '{typography.chrome-body}'
  text-field:
    background: '{colors.field}'
    color: '{colors.ink}'
    border: '{colors.rule}'
    radius: '{rounded.sm}'
    font: '{typography.chrome-body}'
  settings-row:
    background: '{colors.chrome}'
    color: '{colors.ink}'
    font: '{typography.chrome-body}'
  light-edit:
    background: '{colors.paper}'
    color: '{colors.ink}'
    caret: '{colors.ink}'
    underline: '{colors.ink}'
  token-gate:
    background: '{colors.chrome}'
    field: '{colors.field}'
    font: '{typography.chrome-body}'
  interests:
    background: '{colors.chrome}'
    color: '{colors.ink}'
    font: '{typography.chrome-body}'
  choice:
    background: '{colors.field}'
    color: '{colors.ink}'
    border: '{colors.ink}'
    selected-background: '{colors.ink}'
    selected-color: '{colors.paper}'
    radius: '{rounded.sm}'
    font: '{typography.chrome-body}'
---

## Brand & Style

Matutine is a morning newspaper that happens to be an app. The product name is Matutine. The edition is the product: a paper page, a masthead, a date line, a short briefing, and headlines whose size is their importance. Importance is the reader's. Topics they name are what the page treats as important. Chrome exists so a person can point the app at feeds, a calendar, tasks, and those topics. Chrome does not dress itself up as the paper.

The default aesthetic is a broadsheet. Warm paper, black ink, one dark-red rule under the name, serif text, and rules between columns. The person can change the edition, and on Android they can change the theme and the layout. The tokens below are the default theme, Newsprint.

[ASSUMPTION] Compose Multiplatform with Material 3 is the chrome system, because the repo already themes with Material and the shared UI is Compose. The edition does not use Material cards, Material color roles, or the template purple container.

Voice on the page is a desk editor: the date, the facts, the day's list. No cheer, no streaks, no chat.

## Colors

Contrast pairs that carry text meet WCAG 2.2 AA. `{colors.ink}` on `{colors.paper}` is about 16:1. `{colors.ink-secondary}` on `{colors.paper}` is about 7.9:1. `{colors.accent}` on `{colors.paper}` is about 8.9:1. `{colors.on-accent}` on `{colors.accent}` is about 10:1.

- **`{colors.paper}`** is the Newsprint edition canvas on every platform, including the typewriter.
- **`{colors.ink}`** is text, the primary button fill, and the focus ring. Headlines, briefing, and task titles use it.
- **`{colors.ink-secondary}`** is the date line, source names, event times, and helper lines under fields.
- **`{colors.rule}`** is a hairline between stories and columns. It is not text. It fails as text contrast on purpose; never set copy in it.
- **`{colors.accent}`** is the masthead rule and the sample-edition kicker. Errors use `{colors.error}` instead. Links inside stories stay `{colors.ink}` with an underline.
- **`{colors.error}`** is only for failed saves, bad URLs, and rejected tokens. `{colors.error}` on `{colors.paper}` is about 8.1:1.
- **`{colors.chrome}`** is the settings and admin ground, so those screens read as tools beside the paper.
- **`{colors.field}`** is the fill of text fields on chrome screens.
- **`{colors.on-accent}`** is reserved for text on an accent fill. The edition does not use accent-filled buttons.

Newsprint is the default theme. Android settings can select another theme. A theme replaces `{colors.paper}`, `{colors.ink}`, `{colors.ink-secondary}`, `{colors.rule}`, and `{colors.accent}` together. Every theme keeps text at the contrast floor above. [ASSUMPTION] This spine draws only Newsprint. The other themes are chosen by name in settings and are not palettes yet.

## Typography

[ASSUMPTION] Three families, all open licenses, embedded so Android, Desktop, and web match.

- **Newsreader** is the newspaper voice: `{typography.masthead}`, `{typography.lead}`, `{typography.story}`, `{typography.note}`.
- **Source Serif 4** is reading text and chrome: `{typography.briefing}`, `{typography.body}`, `{typography.meta}`, `{typography.kicker}`, `{typography.chrome-body}`.
- **IBM Plex Mono** is `{typography.typewriter}` only.

Ranks are how importance is drawn. Importance is the set of topics the person named. The lead is the story that best fits those topics, then secondary, then note:

| Rank | Type | Where |
|---|---|---|
| Masthead | `{typography.masthead}` wide, `{typography.masthead-compact}` when the page is a single column | Name only |
| Lead | `{typography.lead}` / `{typography.lead-compact}` | At most one story |
| Secondary | `{typography.story}` | The rest of the front page |
| Note | `{typography.note}` | The shortest items, still a headline, still above `{typography.body}` |

`{typography.kicker}` is uppercase tracking for "SAMPLE EDITION" and section labels (Briefing, Calendar, Tasks). `{typography.meta}` is source and time. Body decks use `{typography.body}`.

The typewriter does not scale headline ranks. It is one mono size so writing stays writing, and it holds the whole edition. The front page applies ranks again when the person leaves the typewriter.

## Layout & Spacing

The scale is 4, 8, 12, 16, 24, 40. `{spacing.margin-compact}` (16px) frames the single-column page. `{spacing.margin-wide}` (48px) frames the wide page. `{spacing.column-gap}` (24px) separates columns. `{spacing.4}` separates a headline from its deck. `{spacing.6}` separates the masthead, the briefing, and the blocks.

The default layout is Newsprint columns. [ASSUMPTION] It is wide when the edition width is at least 960px (desktop window, and the web client at that width):

- Masthead and date span the page.
- Briefing spans the story columns, not the rail.
- Stories sit in two columns.
- Calendar block and task block stack in a right rail about 280px wide.
- A `{colors.ink}` 1px rule splits the rail from the stories.

The default below 960px, and the default on Android, is one column: masthead, sample line, briefing, stories in rank order, calendar block, task block. Android settings can choose another layout. Another layout may rearrange these blocks. Rank stays type size. [ASSUMPTION] The extra layouts are not drawn in this spine.

One edition fills the view. The person scrolls the page. There is no second page and no archive grid.

## Elevation & Depth

The edition is flat. No shadow, no scrim, no raised story. Sections separate by type size and by `{colors.rule}`.

Chrome screens use `{colors.chrome}` against `{colors.field}` for fields. Material 3 elevation is allowed on a dialog that confirms leaving an unsaved chrome form, and nowhere on the edition or the typewriter.

## Shapes

The edition, the masthead, stories, and the typewriter use `{rounded.none}`. Paper corners are the window's corners.

`{rounded.sm}` is for buttons and text fields on settings, admin, and the token gate. `{rounded.md}` is unused by the edition. `{rounded.full}` is unused. No pills, no circular story art, no avatars.

## Components

- **Masthead** — `{colors.paper}` ground, the name in `{typography.masthead}` (or compact), then a 2px `{colors.accent}` rule the width of the name, then the date in `{typography.meta}` and `{colors.ink-secondary}`, then a 1px `{colors.ink}` rule across the page. The default name is Matutine. No logo mark. The typewriter can change the name and the date line on that edition.
- **Sample line** — `{typography.kicker}` in `{colors.accent}`, directly under the masthead rule: `SAMPLE EDITION`. Absent once the edition is the person's own.
- **Briefing** — Section label in `{typography.kicker}`, then `{typography.briefing}` in `{colors.ink}`. When the text is the extractive fallback, a `{typography.meta}` line follows: "Summary assembled from headlines." No model name, no sparkle icon.
- **Story** — Headline in the rank's type, then source in `{typography.meta}` / `{colors.ink-secondary}`, then a deck in `{typography.body}`. Stories stack with a `{colors.rule}` hairline between them. The lead has no hairline above it. No thumbnail.
- **Calendar block** — Label "Calendar" in `{typography.kicker}`, title size `{typography.story}`. Each event is `{typography.body}` title and `{typography.meta}` time. Hairline between events.
- **Task block** — Label "Tasks" in `{typography.kicker}`. Each task is `{typography.body}` with a 16px square checkbox stroked in `{colors.ink}`. A completed task stays in ink, with a single strike. The add line is `{typography.body}` on `{colors.paper}`.
- **Typewriter** — Full edition area, `{colors.paper}`, `{typography.typewriter}`, `{colors.ink}` caret. No toolbar and no rank styling. The column contains every part of the edition: masthead, briefing, stories, calendar, tasks, and the section labels. The person can change, add, and reorder that text.
- **Button** — Ink fill, paper label, `{rounded.sm}`, horizontal padding `{spacing.4}`, height 40px on desktop and web, 48px on Android. The secondary button is paper fill, ink 1px border, ink label. Error text sits under the field in `{colors.error}`, not inside the button.
- **Text field** — `{colors.field}` fill, 1px `{colors.rule}` border, `{rounded.sm}`, label in `{typography.meta}` above. Focus: 2px `{colors.ink}` outline, 2px offset. On the typewriter the field is the page, borderless.
- **Settings row** — Full width on `{colors.chrome}`, label in `{typography.chrome-body}`, value in `{colors.ink-secondary}`. Rows separate with a `{colors.rule}` hairline. Android rows are at least 48px tall.
- **Light edit** — Any block on the page can be edited in place. The active string keeps its display type. Ground stays `{colors.paper}`. A 1px `{colors.ink}` underline marks the active string. No toolbar and no mono face; mono is the typewriter only.
- **Token gate** — `{colors.chrome}` screen, one `{components.text-field}`, one `{components.button}`. No masthead and no edition preview behind it.
- **Tray menu** — The operating system's menu. No custom panel, no paper styling.
- **Notification** — The Android system notification. No custom layout. The title is Matutine. The body is the person's notification text. The default body is the sentence in `EXPERIENCE.md`.
- **Interests** — `{colors.chrome}` screen. The first question offers `{components.choice}` chips and a line for a topic of the person's own. The second question is a `{components.text-field}`. Further lines use the same field. Primary button saves. Selected chips and non-empty lines are the topics headline size follows.
- **Choice** — Chrome only, on Interests. Resting state is `{colors.field}` with an ink label and a 1px ink border. Selected state is ink fill and paper label. `{rounded.sm}`. Height 40px on desktop and web, 48px on Android. The default edition does not use choices.

## Do's and Don'ts

- Do use Matutine as the product name on the default masthead, the notification title, and chrome.
- Do print the default edition in ink on `{colors.paper}`, with rank carried by Newsreader size.
- Do let the typewriter change any part of the edition.
- Do keep settings, admin, interests, and the token gate on `{colors.chrome}`.
- Do use `{colors.accent}` for the masthead rule and the sample kicker in Newsprint.
- Do keep the typewriter in `{typography.typewriter}` on the same paper as the current theme.
- Do let a chosen theme replace paper, ink, and accent together, with the contrast floor intact.
- Don't replace the default front page with a feed of equal cards. A chosen layout rearranges the same edition.
- Don't put a chat box, a model badge, or a gradient on the default edition.
- Don't use `{colors.rule}` for text.
- Don't round the default edition or add shadow to a story.
