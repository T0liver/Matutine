---
title: '2-6 Regenerate without losing the edits'
type: 'feature'
created: '2026-11-10'
status: 'in-review'
route: 'dispatch'
review_loop_iteration: 0
story_key: '2-6-regenerate-without-losing-the-edits'
baseline_commit: 'fd8aa8ffad6e0a644fdeeee090c499ae251bbb5f'
context:
  - _bmad-output/implementation-artifacts/epic-2-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Generating today's edition again replaces the morning body, so a rewritten briefing, a rewritten calendar line, and a checked task disappear. There is no same-day extra. A device-calendar snapshot saved after the paper exists never reaches a later paper.

**Approach:** Regenerate keeps the morning id and those three edits, and may refresh stories, rank, and unedited events. New edition now inserts one extra and leaves every other edition untouched. The front page stays today's morning edition. Today's extras open from a same-day list. The late snapshot is included only on that regenerate or extra.

## Boundaries & Constraints

**Always:** Keep the morning id, the briefing already on that document, and every existing task (completed or added). Keep a calendar line whose title and time are not both on the fresh event list, and include every fresh event. Stories and rank come from the new sources. The extra is a new `extra` document with a new id and revision 1. Morning and earlier extras stay unchanged. The list is today's extras only. No revision number is drawn. Tray items, Settings' one primary Generate, and morning insert-if-absent stay. A failed command leaves stored editions unchanged.

**Never:** A merge for a disappeared source item. A merge screen. A past-day archive. A tray item. A server upload. An API key.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Regenerate with edits | Personal morning: rewritten briefing, a calendar line that differs in title or time, a checked task, an added task | Same id, revision + 1, briefing and tasks unchanged, rewritten line present, fresh stories, ranks, and events included | N/A |
| New edition now | Morning exists, maybe one extra | New same-day extra; morning and older extra unchanged; current load is still the morning | N/A |
| Late snapshot | Snapshot saved after the morning paper; that paper does not yet include it | Those events are on the regenerated morning edition or the new extra, not written at snapshot time | N/A |
| Failed command | Feed read fails | Stored editions unchanged | "That feed didn't load." |
| Same-day list | Two extras today and an older day | List offers the two extras only; opening one shows it; Today's morning edition returns to the front page | N/A |

</frozen-after-approval>

## Code Map

- `core/.../edition/EditionGenerator.kt` — fresh stories, rank, events, empty tasks. Reuse. Do not teach it extras.
- `core/.../edition/EditionRepository.kt` — after `fromFeedResults`, overlay the keeps when the stored morning edition is personal. Add `newEditionNow`, `loadSameDayExtras`, and an extra save that does not touch other ids. `recordDeviceCalendar` still leaves the paper alone. `runMorning` stays insert-if-absent.
- `EditionStore` — add `loadExtras(dayKey)` and `saveExtra`. Morning files stay `editions/{dayKey}-morning.json`.
- `LocalEditionRepositoryTest.kt` `FakeStore` — in-memory extras. `by inner` delegates the new methods.
- `FileEditionStore.jvm.kt` and `FileEditionStore.android.kt` — extras at `editions/extra-{dayKey}-{n}.json`. JS and wasm stubs compile.
- `EditionViewModel.kt` — `generate()` still regenerates and shows the morning result. `newEditionNow`, `openExtra`, and `openMorning`. Task and typewriter saves follow the document on screen.
- `EditionScreen.kt` and `MatutineApp.kt` — "New edition now" beside Settings, then the same-day list. `main.kt` tray stays three items.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` — keeps on regenerate; insert and list extras
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` — I/O rows for keeps, isolation, late snapshot, and failure
- [x] `app/shared/src/jvmMain/.../FileEditionStore.jvm.kt` and `androidMain/.../FileEditionStore.android.kt` — persist extras; JS and wasm stubs
- [x] `app/shared/src/jvmTest/.../FileEditionStoreTest.kt` — extra round-trip does not rewrite the morning file
- [x] `EditionViewModel.kt`, `EditionScreen.kt`, `MatutineApp.kt` — button, list, open extra, return to morning
- [x] `app/shared/src/commonTest/.../EditionGenerateStateTest.kt` — insert leaves morning on the front page; the list opens the extra

**Acceptance Criteria:**
- Given a rewritten briefing, a rewritten calendar line, and a checked task, when O generates today's morning edition again, then the id, briefing, line, and checked task remain, and stories and rank may change.
- Given O asks for a new edition now, when it finishes, then a same-day extra exists, other editions are untouched, the front page is still today's morning edition, and the extra opens from a same-day list with no past days, no revision number, and no merge screen.
- Given a device-calendar snapshot saved after today's morning edition exists, when O regenerates or asks for a new edition now, then those events are on that result and were not on the paper before the command.

## Implementation Notes

- Regenerate of a personal morning edition keeps its briefing and tasks, and appends calendar lines whose title and time are not both on the fresh event list. Stories and rank come from the new sources. A sample edition still becomes the personal paper.
- New edition now writes `editions/extra-{dayKey}-{n}.json` and does not save the morning file. The edition screen lists today's extras by first headline, or Extra, and returns with Today's morning edition. The tray menu is unchanged.
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` — BUILD SUCCESSFUL. No emulator and no commit.
- Review layers were not launched: this session cannot spawn a child agent. Standalone prompts are `review-2-6-blind-hunter-prompt.md`, `review-2-6-edge-case-hunter-prompt.md`, and `review-2-6-verification-gap-prompt.md`.

## Spec Change Log

## Review Triage Log

## Design Notes

Fresh events stay in source order. A morning calendar line is appended only when its title and time are not both on that list. Tasks are copied as they stand. The extra does not receive those edits. A list row is the extra's first headline, or Extra. "Today's morning edition" is the return row while an extra is open.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
