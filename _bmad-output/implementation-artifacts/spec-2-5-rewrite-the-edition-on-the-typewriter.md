---
title: '2-5 Rewrite the edition on the typewriter'
type: 'feature'
created: '2026-11-10'
status: 'done'
baseline_commit: '6d28e0fe87aa998421cdfc046153f9b2dbdf51b2'
route: 'dispatch'
review_loop_iteration: 0
story_key: '2-5-rewrite-the-edition-on-the-typewriter'
context:
  - _bmad-output/implementation-artifacts/epic-2-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Desktop can show today's edition but cannot rewrite the masthead, briefing, stories, calendar, or tasks as one column.

**Approach:** A visible Desktop control opens one borderless mono region named "Edition" in the same window. Pausing or closing saves through the edition repository. The front page then shows that document with ranks applied by story order. Android and web do not gain this editor.

## Boundaries & Constraints

**Always:** Keep `dayKey`, `id`, and `role` when the printed date changes. Preserve `sourceNotes`. Reapply `lead`, then `secondary`, then `note` from story order. Save failures keep the typed text and say "Couldn't save. Your text is still here." Leave "Saved." in place until the next successful save. No modal during a save. Discard is the only throw-away action and restores the last saved edition. Escape asks for a save and does not close the window or the process. The tray menu stays Open, Generate today's edition, and Quit.

**Never:** Web light edit, a paper-styled tray, a Desktop morning notification, importing a file, regenerate/merge (Story 2.6), rank edited by hand, or a typewriter toolbar.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Round trip | Current edition | Masthead, date, sample line when sampled, Briefing, stories, Calendar, Tasks | N/A |
| Printed date | Date line changed | `dateLine` changes; `dayKey` does not | N/A |
| Reorder | Story blocks swapped | That order is stored; ranks follow position | N/A |
| Add a line | New task or story block | It is on the saved edition | N/A |
| Save fails | Repository returns failure | Typed text remains; failure sentence shows; prior "Saved." stays | Stay in the typewriter |
| Discard | Unsaved or failed text | Editor text becomes the last saved edition | Clear the failure sentence |
| Escape | Save succeeds | Typewriter closes; front page shows the save | N/A |
| Escape | Save fails | Typewriter stays; text and failure sentence stay | Do not close the window |
| Android | Edition open | No typewriter control | N/A |

</frozen-after-approval>

## Code Map

- `core/.../edition/EditionModels.kt` — document fields to preserve and rewrite. Do not add label columns.
- `core/.../edition/EditionRepository.kt` — `updateMorningEdition` already bumps revision and rejects a `dayKey` change. Reuse it.
- `core/.../edition/SampleGenerator.kt` — `rankForSourceOrder` is the rank reapply.
- `app/shared/.../edition/EditionViewModel.kt` — task saves already use `updateMorningEdition` and "Saved." Do not overload `taskSaved` for the typewriter.
- `app/shared/.../edition/EditionScreen.kt` — Desktop entry sits with Settings. Not hover-only. Absent when the opener is null.
- `app/shared/.../MatutineApp.kt` — screen switch. Tray Open still returns to the edition and must save first if the typewriter is open.
- `app/desktopApp/.../main.kt` — tray items stay. Window hide flushes an open typewriter. Do not quit on Escape.
- `app/shared/.../theme/MatutineEditionTypography.kt` — `typewriter` is IBM Plex Mono 16/1.55. Override ink with the current theme.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionTranscript.kt` — render and apply the one-column text -- so edits round-trip without a second model
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTranscriptTest.kt` -- cover the I/O rows for date, order, ranks, and preserve
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt` -- save, discard, and failure copy -- repository write without a modal flag
- [x] `app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt` -- success, failure, Saved. stickiness, discard
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/TypewriterAccess.kt` -- `showsTypewriter()` expect -- Android and web false, desktop true
- [x] `app/shared/.../EditionScreen.kt` and `MatutineApp.kt` -- visible Typewriter control and screen swap on Desktop only
- [x] `app/desktopApp/.../TypewriterScreen.kt` and `main.kt` -- borderless mono region, pause save, Discard, Escape, window flush

**Acceptance Criteria:**
- Given today's edition on Desktop, when the typewriter opens, then it replaces the front page as one mono region named "Edition" with no toolbar and no rank styling.
- Given a pause or a successful close, when the front page returns, then the edits are there and ranks follow story order.
- Given a failed save, when the text is still on screen, then it says "Couldn't save. Your text is still here." and "Saved." does not disappear early.
- Given Android, when the edition is open, then there is no typewriter.

## Implementation Notes

- `EditionTranscript` renders and applies masthead, date, sample line, Briefing, stories, Calendar, and Tasks. `dayKey`, `id`, `role`, `revision`, and `sourceNotes` stay on apply. Ranks follow story order.
- Desktop shows a visible Typewriter control beside Settings. The editor is a borderless IBM Plex Mono field named Edition on the theme paper, with Discard under the text. Pause is one second. Escape saves and returns to the front page only when that save succeeds.
- Android, JS, and wasm report `showsTypewriter()` false, so they draw no control.
- Tray items stay Open, Generate today's edition, and Quit. Hiding the window flushes an open typewriter. No emulator and no commit.
- Review subagents were not run: this session was told not to spawn a child agent. Nothing was added to deferred work.

## Spec Change Log

## Review Triage Log

## Design Notes

The visible opener is the word Typewriter, always shown beside Settings on the Desktop edition. The editor has no toolbar. Discard is the secondary button under the text.

Section markers are the lines `Briefing`, `Calendar`, and `Tasks`. `SAMPLE EDITION` is its own line only while `isSample` is true. Story blocks are blank-line separated, in headline, source, deck, then an `http` URL when one exists. Tasks use `[ ]` and `[x]`. Calendar events are title then time. A blank line keeps the current section, so events and tasks can be split while stories stay between Briefing and the next marker. Front-page section order stays masthead, briefing, stories, calendar, tasks. Item order follows the text. Pause is one second. Escape closes the typewriter only after the save succeeds.

```
Matutine
Tuesday, November 10, 2026
SAMPLE EDITION

Briefing
The briefing.

Lead headline
City Desk
Deck.
https://example.com/lead

Calendar
Standup
09:00

Tasks
[ ] Milk
[x] File notes
```

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
