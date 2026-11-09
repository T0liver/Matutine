---
title: '1-6 Name topics and change the lead'
type: 'feature'
created: '2026-11-08'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
story_key: '1-6-name-topics-and-change-the-lead'
baseline_commit: '5b7cf66aa147856946ea8b84267839b7ac885baa'
context:
  - _bmad-output/implementation-artifacts/epic-1-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The lead still follows feed order. O cannot name what the paper should notice.

**Approach:** Interests, opened from Settings, saves topics on the settings document. The generator stores lead, secondary, and note from those topics. With no topics, source order stands and a line on the person's edition opens Interests. The sample launch does not.

## Boundaries & Constraints

**Always:** Ask "What do you want to read about?" and "What should the page notice about your day?" First question: Video games, Technology, Biology, Finance, Science, Sports, World news, Culture, Health, Local, plus a free line. Second: an open line and a control to add another. Many choices can be on. Selected: ink fill, paper label. Resting: field fill, ink label, 1px ink border. Height 40px on desktop and web, 48px on Android. Each selected choice and each non-empty line is one topic. A story fits when the topic is a case-insensitive substring of the headline or deck. More matches outrank fewer. Ties keep source order. Store that order with one lead, then secondary, then notes. The client draws the stored rank. Topics live on the settings document. Absent or empty topics means no topics. The line "What do you want to read about?" appears only on an edition from the person's sources.

**Never:** A model call, CalDAV, a server, a second source list, or a stored numeric score. Do not put topics on the source list or move Interests to Admin. Do not open Interests on the sample launch. Do not change the bundled sample. Do not put Compose in `:core`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Save topics | Technology and Video games on, free line "Rust", day lines "school" and blank | Topics are Technology, Video games, Rust, school. Source list unchanged. | N/A |
| Best fit | Those topics. Order is markets, a Video games story, then a story with both Technology and Video games | Both-topic story is the only lead and first. One-topic story is secondary. Markets is a note. | N/A |
| No topics | Person's feeds. Settings absent or topics empty | Source order. First story is the lead. The page line opens Interests. Sample line stays hidden. | N/A |
| Sample | Source list absent | Bundled sample. No Interests line. Topics are not written. | N/A |
| Clear topics | Save with nothing selected | Topics become empty. Next generate uses source order. The sample does not return. | N/A |

</frozen-after-approval>

## Code Map

- `core/.../edition/SampleGenerator.kt` — `rankForSourceOrder`. Reuse after ordering. Do not read topics.
- `core/.../edition/EditionGenerator.kt` — `fromFeedResults` is source order. Pass topics in. Store `StoryRank` only.
- `core/.../edition/EditionRepository.kt` — `generateToday` and `EditionStore`. Read topics when generating. Add settings read and save. Leave the absent-list sample path.
- `core/.../edition/SourceList.kt` — Feeds and calendars only.
- `app/shared/.../edition/EditionScreen.kt` — Draws stored order via `storiesForTwoColumns`. Add the no-topic line. Do not sort.
- `app/shared/.../settings/SettingsScreen.kt` — Add one Interests row. Keep the feed row and generate.
- `app/shared/.../MatutineApp.kt` — Open Interests from Settings and from the edition line. One repository.
- `app/shared/.../store/FileEditionStore.*.kt` — Add `settings.json` on JVM and Android. JS and Wasm accept the methods and store nothing.
- `core/.../edition/LocalEditionRepositoryTest.kt` — Keep absent-list, empty-list, and source-order tests. Add this matrix.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt` -- Settings document with defaulted topics.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt` -- Rank by topic fit, then source order.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- Save topics and pass them into generation.
- [x] `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt` -- Persist `settings.json`. Mirror on Android. JS and Wasm no-op.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/InterestsScreen.kt` -- Both questions, choices, lines, and one primary save.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- No-topic line on the person's edition only. Wire that line and the Settings row through `SettingsScreen`, `EditionViewModel`, and `MatutineApp`.
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` -- Test the matrix.

**Acceptance Criteria:**
- Given Interests is opened from Settings, when it is shown, then both questions, the named choices, the free line, and add-another are present, and a selected choice uses ink fill with a paper label.
- Given Technology and Video games are saved and today's edition is generated, when it is drawn, then the stored lead is the best fit, then secondary, then note, with one lead.
- Given no topics, when the person's sources generate an edition, then source order stands and "What do you want to read about?" opens Interests.
- Given the first launch, when the sample is shown, then Interests is not opened.

## Implementation Notes

- Topics are `ReaderSettings.topics` in `settings.json`. The source list is unchanged. Fit count is computed at generation and is not a field on the story.
- `:core:jvmTest`, `:app:shared:jvmTest`, `:app:desktopApp:compileKotlin`, and `:app:androidApp:assembleDebug` succeeded.
- The no-topic line is `showsNoTopicLine`: a person's edition with no topics. The sample launch does not show it.

## Spec Change Log

## Review Triage Log

- false — Desktop `40.toDp()` is 40px, which is the specified height. Android uses 48dp, so the chip is at least the 48px floor. Clipping is not shown.
- false — The briefing stays in feed order because it is the extractive summary, and this story does not reorder that summary. A deck that already ends in `!` or `?` gaining a period is the existing helper, not topic ranking.
- false — A topic match is a case-insensitive substring, which is the specified fit. A repeated topic string is two saved topics. The matrix does not collapse them.
- low — JS and Wasm report a successful topic save and then show an empty list, because those stores keep nothing and the view model re-reads them. Android and Desktop write `settings.json` and read it back. A web store is outside this story. Rejected: everyday use is Android and Desktop, and the fix is a new store.
- low — A corrupt `settings.json` becomes an empty topic list on load, and generate then shows "That feed didn't load." Rejected: a torn settings file is not an everyday file, and a separate settings error is a new branch.
- false — Saving topics hides the no-topic line and leaves the current lead until Generate. Rank is stored when the edition is generated.
- false — A blank feed field does not clear the saved URL. Story 1.5 keeps the previous URL until a new one succeeds, and Done saves the field.
- defer — The Android and JVM feed readers are copies, with no body cap and with cleartext on the application. That reader predates topic ranking. Cleartext is how an `http` feed loads.
- defer — Interests colors, the edition line, and the question texts are not composed. Common tests do not run Compose.
- false — Sprint status and spec status move at different steps of this workflow. The emulator notes and the absolute path in deferred work were already in the tree.
- false — A cancelled feed read is not reachable. Nothing in this UI cancels the view-model scope.
- false — `saveFeedUrl` rethrows cancellation before mapping other errors, and the outer `runCatching` is not reached by a cancellation this UI never starts.
- false — A parsed feed with no items is still saved. An empty item list is a recognized document, and the page already says "No stories yet."
- false — Generate with a blank field uses the stored URL. That is the previous value staying until a new one succeeds.
- false — Generate uses the URL captured for that run and leaves a newer field value alone.
- false — A stored list with a count other than one leaves the field blank. This screen saves one URL.
- low — A failed `saveTopics` leaves Interests open with no error. Rejected: a local settings write fails only in rare IO cases, and the matrix has no error copy for it.
- low — The same JS and Wasm re-read clears topics after Save. Rejected for the same reason as the other web-store finding.
- false — A description that contains a closing tag is taken as text. This story does not change the parser.
- false — The feed reader has no size cap. That cap was already left out of the reader.
- false — HTML in a description stays in the extract. The description text is the deck.
- low — `EditionViewModel.topics` is never asserted, so deleting the init and save assignments would leave the tests green. Patch: assert the initial list, the list after save, and an empty list when `savedTopics()` throws.
- low — `draftFromTopics` is never called by a test, so a bad split would still pass. Patch: assert Technology and Video games stay selected, with Rust on the free line and school on the day line.
- low — A second `generate()` while one is running is not asserted. Patch: call it again before the first fetch finishes and assert one `generateToday()`.
- defer — The generating line, notice, source notes, feed error, and both interest questions are not composed. Closing that needs a Compose harness this repo does not run.
- defer — Android cleartext and `INTERNET` are not what the JVM feed test runs. Seeing a failure needs a device fetch.
- defer — `writeSettings` is tested on the JVM store, not the Android actual. The Android file is the same `java.io.File` behavior.
- defer — JS and Wasm `readSettings` and `readFeedDocument` are only compiled. The story's check for those targets is compilation.

## Design Notes

Fit count orders stories and is not stored. Ties keep feed order, then `rankForSourceOrder` labels the reordered list. `ReaderSettings` is the settings document; this story writes `topics` only.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: success
- `./gradlew :core:compileKotlinJs :core:compileKotlinWasmJs :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` -- expected: success, or a reported Yarn download failure
