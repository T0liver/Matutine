---
title: 'Keep today''s paper on the next morning run'
type: 'feature'
created: '2026-11-09'
status: 'done'
route: 'dispatch'
story_key: '1-9-keep-today-s-paper-on-the-next-morning-run'
baseline_commit: 'b172d7e158b1343c13287476399644f78594763a'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A later automatic run must not wipe today's paper. On a new day the morning document is missing, the screen is an error, and Generate does nothing because it only runs from a ready edition.

**Approach:** A local morning run inserts today's morning edition only when it is absent and no server is configured. Android schedules that run approximately. Generate still replaces today's paper while the app is open. A missing paper shows an empty front page, not a list.

## Boundaries & Constraints

**Always:**
- The generator assigns the day key as `YYYY-MM-DD` from the device zone.
- No server configured and today's morning document absent: one insert. A later automatic run does not write, and does not read feeds, calendars, or CalDAV. A saved sample counts as that document.
- A failed source is omitted and noted. The insert still completes when another input exists. If nothing can be generated, do not write.
- Android still generates on demand from the empty front page. The periodic run may be late and has no clock time.
- Desktop generates only while open, through the Story 1.5 Generate action.
- A missing morning document shows an empty front page: masthead Matutine and Settings, no story, briefing, calendar, or task list. During that generate, the masthead stays and the body says "Setting today's edition."

**Never:**
- Do not let the morning run replace an existing morning edition, and do not stop explicit Generate from replacing. Regenerate is Story 2.6.
- Do not post or schedule a notification, add the system tray, or use an exact alarm.
- Do not read `VTODO`, add Todoist, or send the CalDAV password to a Matutine server.
- Do not redo Stories 1.7 or 1.8, or show yesterday's paper as today.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Absent, no server | No morning file for today's key; a saved feed parses | One morning edition; generator day key `YYYY-MM-DD` | N/A |
| Already stored | Today's morning file exists, including a sample | No write and no source read | N/A |
| Second run | The edition just inserted | No second write | N/A |
| Server configured | Server flag on; morning absent | No insert and no source read | N/A |
| Nothing to generate | Morning absent; feed missing or unreadable | Store unchanged | Failure stays off the paper |
| Explicit Generate | Today's paper exists | `generateToday` still replaces it | Story 1.5 failure copy unchanged |
| Missing on screen | Source list exists; today's file does not | Empty front page; Settings can generate | No list and no error screen |

</frozen-after-approval>

## Code Map

- `core/.../edition/EditionRepository.kt` — `generateToday` always saves. Load errors when a source list exists and today's file is absent. Add `runMorning` there. Leave CalDAV, ICS, and the snapshot rule alone.
- `core/.../edition/EditionGenerator.kt` and `DayKey.kt` — Generator already assigns `YYYY-MM-DD`. Do not change ranking or format the key in the UI.
- `core/.../edition/LocalEditionRepositoryTest.kt` — `FakeStore.saved` changes only on save. Add the matrix. Keep the explicit-Generate revision test.
- `app/shared/.../edition/EditionViewModel.kt` — `generate()` returns unless state is `Ready`. Allow it from the empty state.
- `app/shared/.../edition/EditionScreen.kt` — Missing paper is `OtherError`. Do not reuse `StoryBlock` for the empty page.
- `app/shared/.../MatutineApp.kt` — Settings already calls `generate()` on both clients. That stays the only desktop generation.
- `app/androidApp` `MainActivity.kt` and `AndroidManifest.xml` — No worker or notification permission. Schedule from an `Application`. A background run has no activity, so pass the application context into the existing readers.
- `gradle/libs.versions.toml` and `app/androidApp/build.gradle.kts` — WorkManager is not a dependency yet.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- Add `runMorning` for insert-if-absent.
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` -- Cover the matrix.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt` -- Empty state, and Generate from it.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- Masthead and Settings only. No list.
- [x] `app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt` -- Generate from the empty state.
- [x] `app/androidApp` -- Periodic WorkManager calls `runMorning` only.
- [x] `gradle/libs.versions.toml` -- Add WorkManager for `androidApp`.

**Acceptance Criteria:**
- Given today's morning edition is absent and no server is configured, when a morning run executes, then it inserts that edition and the generator assigns `YYYY-MM-DD` from the device zone.
- Given today's morning edition exists, when another automatic run fires, then it does not write.
- Given Desktop is open, when a paper is generated, then that is the Generate action, and nothing runs while the process is closed.
- Given Android, when the user generates on demand, then today's edition is written, and the periodic run may be late.
- Given today's morning edition is missing, when the front page is shown, then it is empty and a list is not shown in its place.

## Implementation Notes

## Spec Change Log

## Review Triage Log

- BH1 — false. `MorningEditionWorker` returns success after `runMorning`, including a failed insert. A bad or missing feed leaves the store unchanged (`runMorning_missingOrUnreadableFeed_leavesTheStoreUnchanged`). The spec's later automatic run is the next 24-hour period, and `KEEP` avoids resetting that window. `Result.retry()` would repeat a permanent failure inside the period.
- BH2 — false. `writeGeneratedToday` now rechecks today's file after the feed read and again immediately before save, and returns the stored paper without writing. `runMorning_paperThatAppearsDuringTheRead_isNotReplaced` passed.
- BH3 — false. `runMorning` captures the day once. `runMorning_keepsTheDayCapturedAtTheStart` calls `today()` once and writes `2026-11-07` even when a later call would be the next day. The device read uses that same date.
- BH4 — defer. A corrupt snapshot still throws from `readCalendarSnapshot`, and a failed ICS read is still omitted without a source note. Both behaviors belong to stories 1.7 and 1.8; this story's code map says to leave the ICS and snapshot rules alone. Feed and CalDAV failures are still noted.
- BH5 — defer. The CalDAV REPORT still has no time range. That request is story 1.8, and this story does not change the CalDAV reader.
- BH6 — maybe-false, defer. Android still sets `REPORT` by reflecting into `HttpURLConnection`. Whether API 37 blocks that field was not run on a device. A device REPORT that arrives as `REPORT` would settle it.
- BH7 — defer. An `http` URL already stored in `caldav.json` can still be fetched in cleartext, and backup is still allowed. Saving a new account still rejects `http`. This is story 1.8 account IO, not the morning insert.
- BH8 — defer. A cleared password field still cannot change the URL or username, and both failures use the CalDAV sentence. That is the story 1.8 save rule.
- BH9 — low, rejected. An already open empty page does not reload when the worker later writes the file. The page is empty because the file was absent when it was shown, which is the spec. Refreshing it needs a new lifecycle observer, and the morning case is the next open, which loads the file.
- BH10 — false. Explicit Generate still uses "That feed didn't load." on failure, which the spec keeps as the story 1.5 sentence. The missing Compose assertion is V3, not a wrong failure sentence.
- E1 — false. Same overwrite-during-fetch claim as BH2, and the same recheck now returns the paper that appeared.
- E2 — false. The day is captured before the absence check, so midnight during the later read cannot switch the key. Covered by `runMorning_keepsTheDayCapturedAtTheStart`.
- E3 — false. Generation now passes the captured day into `recordDeviceCalendar`. The public no-arg method still reads "now" for its own callers.
- E4 — low, rejected. `timeZone` is fixed when the repository is constructed, while `today()` reads the current zone. A zone change during one morning run no longer matters because `today()` is called once at the start. A change across a long-lived open app is the pre-existing constructor and is not an everyday morning run.
- E5 — defer. Same corrupt-snapshot abort as BH4. The read still throws, and it predates this story.
- E6 — false. Same as BH1. A failed `runMorning` is not a WorkManager retry, and the next period is the later run.
- E7 — defer. CalDAV fields wire only `onDone`, so IME Next does not move focus. `SettingsScreen` is story 1.8 and was not part of this insert.
- E8 — maybe-false, defer. Status parsing matches `HTTP/1.0` and `HTTP/1.1` only. Whether a live CalDAV body uses `HTTP/2` was not observed. A captured HTTP/2 multistatus would settle it.
- E9 — false. Same in-flight replace claim as E1, closed by the pre-save recheck.
- V1 — defer. `morningWorkSchedule` is now tested for a 24-hour period, equal flex, `KEEP`, and `MorningEditionWorker`. `doWork` and `MatutineApplication.onCreate` still are not executed, so swapping the body for `generateToday` or dropping the `onCreate` call would stay green. Running `doWork` needs an Android context this suite does not build.
- V2 — defer. Desktop and Android `readCalDavCalendar` are still not called by tests. Repository tests inject the response body. That client is story 1.8.
- V3 — defer. `MissingMorning` is asserted on the view model, and no test composes `EmptyFrontPage`. This repo does not run Compose tests.
- V4 — defer. The calendar permission row is still only a string constant. Compose is not run, and the row is story 1.7.
- V5 — defer. `DeviceCalendar.android.kt` is still only compiled. JVM tests inject `DeviceCalendarRead`. An instrumented `CalendarContract` test is outside this suite, and the reader is story 1.7.

## Design Notes

`runMorning` is not a UI-port method. Android uses a 24-hour period and a full-period flex, so the OS may run it at any time. The server flag defaults off; this epic has no server URL field.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: success
- `./gradlew :core:compileKotlinJs :core:compileKotlinWasmJs :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` -- expected: success, or a reported Yarn download failure
