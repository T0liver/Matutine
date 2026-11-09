---
title: '1-5 Generate today''s edition from a feed'
type: 'feature'
created: '2026-11-08'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
story_key: '1-5-generate-today-s-edition-from-a-feed'
baseline_commit: '5b7cf66aa147856946ea8b84267839b7ac885baa'
context:
  - _bmad-output/implementation-artifacts/epic-1-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The front page can only become a paper from bundled fixtures. O cannot point it at a personal RSS or Atom feed, so the lead and the briefing stay the sample.

**Approach:** Settings stores one RSS or Atom URL on the existing source list after that document parses. "Generate today's edition" builds today's morning edition from the saved feeds in the device zone, keeps the current paper readable until the new one is ready, and hides the sample line on the finished paper. With no topics, source order stands and the first story is the lead. With no AI key, the briefing is the extractive summary plus "Summary assembled from headlines."

## Boundaries & Constraints

**Always:** An absent source list still builds the bundled sample and does not write those fixtures into the list. An empty list stays empty. Rank is source order until topics exist. A new feed URL is written only after its document parses as RSS or Atom. A failed feed is omitted. When another saved feed still parses, the edition completes and carries the note "That feed didn't load." When nothing parses, the saved list and the current paper stay, and that same sentence is shown. The generating body copy is "Setting today's edition." Masthead and date stay. One primary button, "Generate today's edition". One settings row for the feed. Desktop and web chrome controls added here are at least 40px; Android rows are at least 48dp. The previous saved URL stays until the new URL succeeds, and the field keeps the typed URL.

**Never:** A model call, CalDAV, a server, a second source list, Interests, or topic ranking. Do not change the bundled sample fixtures. Do not put Ktor or Compose in `:core`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Save one feed | Sample on screen, URL returns RSS or Atom | Source list gains that URL and no bundled feed. Sample line still shows until generate. | N/A |
| Generate | Saved feed parses, no topics, no key | Today's morning edition, device day key, sample line hidden, source-order lead, extractive briefing ending in the required sentence. | N/A |
| Generating | Generate in flight, sample already shown | Masthead and date stay. Body says "Setting today's edition." Sample stays readable until the new edition replaces it. | N/A |
| Bad URL | Document is not RSS or Atom, or the read fails | List unchanged. Field keeps the URL. Current paper stays. | "That feed didn't load." |
| Mixed feeds | Stored list has one good feed and one bad feed | Edition completes from the good feed. Bad feed omitted. | Note on the edition: "That feed didn't load." |
| Empty list | `sources.json` exists with no feeds | Generate does not build the bundled sample. | No sample fallback |

</frozen-after-approval>

## Code Map

- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt` — RSS parser. Extend for Atom. Reuse `FeedItem`. Do not change bundled fixture text.
- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/SampleGenerator.kt` — `rankForSourceOrder` and `extractiveBriefing`. Reuse. Do not call the sample path for a present list.
- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` — Absent list builds the sample. Empty list does not. Add save and generate beside that.
- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionModels.kt` — Edition has no failure note yet. Add a defaulted note list so older documents still decode.
- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/SourceList.kt` — One document, `feeds` plus `calendars`. Do not add a second list.
- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/DayKey.kt` — Device zone via `todayLocalDate`. Reuse for the generated day key and date line.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt` — Load and retry only. Add generate state without dropping the visible edition.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` — Draws the paper. Add the generating sentence and the failure note. Do not re-rank.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt` — Composition root. Bind one repository and the feed reader here.
- `app/shared/src/*/kotlin/hu/toliver/matutine/store/FileEditionStore.*.kt` — `sources.json` absent is null. Add a whole-document write. Leave JS and Wasm non-persistent.
- `app/androidApp/src/main/AndroidManifest.xml` — No INTERNET permission yet. Add it. Do not add a calendar permission.
- `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` — Cover the matrix. Keep the absent-list and empty-list tests passing.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt` -- Parse RSS and Atom into `FeedItem`, and fail with "That feed didn't load." when the document is neither.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionModels.kt` -- Add defaulted `sourceNotes` on `Edition`.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt` -- Build today's morning edition from feed documents: source-order rank, extractive briefing, empty events and tasks, `isSample` false. Omit a failed feed and note it when another feed succeeds.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- Save one parsed URL onto the existing source list, and generate from that list without using the bundled sample.
- [x] `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt` -- Write `sources.json` as one document. Mirror the write on the Android store. JS and Wasm accept the method and still store nothing.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` -- One feed row, field error under the label, one primary generate button, secondary return to the edition. Heights: 40px desktop and web, 48dp Android rows.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- Settings entry, generating copy over the readable paper, sample line only when `isSample`, source notes visible.
- [x] `app/androidApp/src/main/AndroidManifest.xml` -- Allow the feed read.
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` -- Unit-test the matrix, including Atom, a bad document, and mixed feeds.

**Acceptance Criteria:**
- Given the sample is on screen, when O saves one RSS or Atom URL and activates "Generate today's edition", then the masthead and date stay, the body says "Setting today's edition.", and the sample stays readable until the finished morning edition hides `SAMPLE EDITION`.
- Given no AI key, when generation finishes, then the briefing ends with "Summary assembled from headlines." and the page is a finished edition.
- Given a feed does not parse, when generation runs, then the page says "That feed didn't load.", the field keeps the URL, the prior list stays, and the sample stays.
- Given another saved feed parses, when one feed fails, then the failed feed is omitted, the edition notes "That feed didn't load.", and the edition completes.

## Implementation Notes

- Feed documents are read through `readFeedDocument` in `:app:shared`. JVM and Android use `HttpURLConnection`. JS and Wasm fail with "That feed didn't load." and still do not persist a store.
- Saving a URL replaces `feeds` with that one URL and keeps `calendars`. Generation reads every URL already on the list, so a stored mix of good and bad feeds still completes.
- `:app:desktopApp:compileKotlinJvm` is not a task. `:app:desktopApp:compileKotlin` succeeded.
- `:core:jvmTest` and `:app:shared:jvmTest` passed, including the matrix tests. JS and Wasm compilation succeeded. `:app:androidApp:assembleDebug` succeeded.
- The new screens were not opened on the emulator.
- Review patches: a corrupt `sources.json` no longer crashes the feed field; a finished save or generate does not overwrite a field the person changed; Android allows cleartext HTTP. Tests cover the kept calendar URL, the view-model save and generate paths, HTTP 200 and non-2xx, and a corrupt source list.

## Spec Change Log

## Review Triage Log

- low — Settings is missing on the sample-failure and other-error screens. Rejected. This story starts from the sample paper, and the specified return path is the Settings button on that paper.
- false — Closing Settings when Generate is pressed hides the generating paper. The page is supposed to show "Setting today's edition." while the sample stays readable.
- low — Android system back from Settings leaves the activity. Rejected. The specified control is "Return to the edition"; a back handler is extra platform code.
- false — A second `generate()` while one is running returns immediately. That guard stops a second write.
- medium — `saveFeed` and `generate` assign the captured URL back onto `feedUrl` when they finish, so keystrokes during the request disappear. The completion must leave a newer field value alone.
- low — Feed XML is parsed on the main dispatcher. Rejected. A normal feed is small, and moving the parser adds a dispatcher boundary.
- low — A successful generate that skipped save can leave `feedFieldError` set. Clear it when generation succeeds.
- false — A failed generate of an already saved URL does not set `feedFieldError`. The edition notice is the page sentence, and the field still holds the URL.
- low — `singleOrNull()` leaves the field blank when the stored list has any count other than one. Rejected. This story saves one URL.
- false — `saveFeedUrl` replaces `feeds` with that one URL. That is the specified save.
- false — A recognized RSS or Atom document with no items still produces an edition. The spec fails a document only when it is neither RSS nor Atom. "No stories yet." already covers an empty paper.
- low — `<rss` anywhere is preferred over a later `<feed`, so an Atom document that mentions that text is parsed as RSS. Rejected. Everyday Atom documents do not embed that tag, and a second classifier is extra branching.
- false — `CancellationException` inside `runCatching` becomes a failed result. Nothing in this UI cancels that coroutine.
- medium — Android `targetSdk` 37 blocks cleartext, so an `http://` feed fails with "That feed didn't load." while the same reader works on the desktop. Allow cleartext on the application.
- low — `readFeedDocument` buffers the whole body. Rejected. A size cap is a new guard for a response this story does not demonstrate.
- low — Numeric character references and HTML in descriptions are left in the text. Rejected. A sanitizer is a new parser, and the spec takes the description text as the extract.
- false — Sprint status and the superpowers notes in the baseline diff are not defects in this story. Story 1.4's review status and those untracked files were already in the tree.
- false — Generate that saves a newly parsed URL, then fails to rebuild, leaves that new URL on the list. The URL is written because it parsed. The current paper stays.
- medium — `EditionViewModel` reads `savedFeedUrls()` during `init`, outside `load()`'s catch. A `sources.json` that does not decode crashes composition instead of showing the existing error state.
- low — `writeSourceList` is not atomic. Rejected. A torn write needs a crash mid-write, and a temp-file rename is extra machinery.
- low — `updateFeedUrl` leaves `feedFieldError` showing after the text changes. Clear the error when the text changes.
- low — A successful `saveFeedUrl` is not tested against a list that already has a calendar. The code copies `calendars` through. Add the assertion.
- medium — `EditionViewModel.generate` never runs its successful save-then-generate path under test. The fake save always fails. Add that case.
- low — A failed `generateToday` for an already saved URL is not applied to `EditionUiState`. Add the case.
- medium — The failed-save view-model test still passes if `generateToday` runs afterward, because the fake always throws. Assert that a failed new URL does not generate.
- low — `saveFeed` and the initial field value are never called. Add those cases.
- medium — `readFeedDocument` is never executed. A JVM test must accept a 200 body and reject a non-2xx response.
- low — A corrupt `sources.json` is not distinguished from a missing file under test. Add the case that load fails and does not write a sample.
- low — The paper's generating line, notice, and source notes are not composed in a test. Deferred. The view-model flags and `sourceNotes` are already asserted, and common tests do not run Compose.
- low — The Android `INTERNET` permission is not executed on a device. Deferred. Unit tests cannot perform that fetch.

## Design Notes

The feed reader is a function passed into `LocalEditionRepository`. `:core` parses XML it is given. JVM and Android read HTTP in `:app:shared`; JS and Wasm do not gain a second store.

A personal morning edition uses id `morning-<dayKey>` and revision 1 when it replaces the sample. A later on-demand generate of an existing personal morning keeps that id and increments revision. Automatic insert-if-absent stays Story 1.9.

`sourceNotes` defaults to empty. Each omitted feed adds the sentence "That feed didn't load." Sample JSON without the field still loads.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlinJvm :app:androidApp:assembleDebug` -- expected: success
- `./gradlew :core:compileKotlinJs :core:compileKotlinWasmJs :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` -- expected: success, or a reported Yarn download failure
