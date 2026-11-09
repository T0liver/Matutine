---
title: 'Show today''s events from ICS and the phone calendar'
type: 'feature'
created: '2026-11-09'
status: 'done'
route: 'dispatch'
baseline_commit: 'b172d7e158b1343c13287476399644f78594763a'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Today's paper lists news and an empty calendar. O needs today's events from an ICS URL and, on Android, the phone calendar.

**Approach:** Save one ICS URL on the source list. Android alone writes a device-calendar snapshot. Generation unions that snapshot with ICS, dedupes, and stores the events. The front page draws `edition.events` and does not query a calendar.

## Boundaries & Constraints

**Always:**
- The generator unions ICS, fetched during generation, with the stored device snapshot.
- Identity is start, end, and trimmed title, as UTC instants at second precision. Source calendar is not part of identity.
- The paper shows events in time order, with title and start time.
- A URL that does not open shows "That calendar link didn't open." and leaves the prior calendar source.
- Android with calendar permission off shows "Calendar permission is off." and a row that opens the system permission screen. Briefing, stories, and tasks still show.
- An empty or failed device read does not overwrite the snapshot. A snapshot written after the paper exists does not change that paper.
- Only Android writes the snapshot.

**Never:**
- Do not read VTODO, add Todoist or CalDAV, or send a password to a server.
- Do not build the event list by querying a calendar from the screen.
- Do not re-rank stories or change the sample path.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| ICS generate | Stored ICS URL opens and has today's VEVENTs | Events show title and start time, sorted by start | N/A |
| Dedupe | Two events share start, end, and trimmed title | One event | N/A |
| Bad save | ICS URL does not open | "That calendar link didn't open." Prior calendars stay | Do not write the new URL |
| Permission off | Android READ_CALENDAR denied | Calendar block says "Calendar permission is off." Row opens the system permission screen. Briefing, stories, and tasks stay | Device read is a failed read |
| Device events | Android permission on, device has today's events | Those events are in the generated edition | N/A |
| Empty or failed read | Snapshot already stored; read is empty or fails | Snapshot document unchanged | Keep the previous snapshot |
| Late snapshot | Snapshot written after today's paper exists | Stored edition events unchanged | Do not save the edition from that write |

</frozen-after-approval>

## Code Map

- `core/.../edition/FeedDocuments.kt` — Keep `parseIcs` for the sample. Add VEVENT occurrences. Do not read VTODO.
- `core/.../edition/EditionGenerator.kt` — Keep topic ranking. Take the unioned event list. Do not read a device calendar.
- `core/.../edition/EditionRepository.kt` — `saveFeedUrl` keeps `calendars`. Add `saveCalendarUrl`. Write the snapshot only for a non-empty Android read. Leave the absent-list sample path.
- `core/.../edition/SourceList.kt` — One ICS URL replaces `calendars` and keeps `feeds`.
- `app/shared/.../store/FileEditionStore.*.kt` — `calendar-snapshot.json` on JVM and Android. JS and Wasm store nothing.
- `app/shared/.../edition/EditionScreen.kt` — Draw `edition.events`. Permission copy is display state, not a calendar query.
- `app/androidApp/src/main/AndroidManifest.xml` — `READ_CALENDAR` only.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/CalendarSnapshot.kt` -- Snapshot document and occurrence type.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt` -- Parse VEVENT start, end, and title.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt` -- Store the unioned events. Keep topic ranking.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- Save one ICS URL. Union ICS with the snapshot. Write the snapshot only when the Android flag is set and the read is non-empty.
- [x] `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt` -- Persist `calendar-snapshot.json`. Mirror on Android. JS and Wasm no-op.
- [x] `app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/DeviceCalendar.android.kt` -- Read today's instances. Other targets return unavailable and do not write.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` -- Calendar URL field and the bad-link sentence.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- Permission sentence and row. Keep drawing `edition.events`.
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` -- Test the matrix.

**Acceptance Criteria:**
- Given Desktop has an ICS URL, when today's edition is generated, then events appear in time order with title and start time, and the screen draws the edition's event list.
- Given two events share the same start, end, and trimmed title, when the edition is generated, then they appear once.
- Given the ICS URL does not open, when O saves it, then the page says "That calendar link didn't open." and the prior calendar source stays.
- Given Android calendar permission is off, when the edition is shown, then the calendar block says "Calendar permission is off." and offers a row that opens the system permission screen, and stories, briefing, and tasks still show.
- Given Android has a device calendar and permission is on, when generation runs, then today's device events are included, an empty or failed device read does not overwrite a stored snapshot, and a snapshot written after the paper exists does not change that paper.

## Implementation Notes

- ICS URLs and the device snapshot meet in `editionEventsFrom`. VEVENT only. A URL with userinfo is refused and is not fetched.
- `calendar-snapshot.json` is written only when `writesDeviceCalendarSnapshot()` is true and the read is a non-empty `Events` list. `recordDeviceCalendar` does not save the edition.
- JVM tests for the matrix passed, including order, dedupe, a closed link, device events, an empty or failed read, and a late snapshot. Shared JVM tests, desktop compile, Android debug assemble, and JS/Wasm compiles succeeded.
- The permission row uses "Calendar permission is off." and opens the application details screen. Compose is not in the unit suite.

## Spec Change Log

## Review Triage Log

- false — Recurring ICS events whose original start is not today stay out. Today is the start's civil date, so a later RRULE day is not a today's event in this story.
- false — An event that started yesterday is omitted, including after the device query. `startsOn` keeps a start that falls today.
- medium — `VALUE=DATE-TIME` matches `contains("VALUE=DATE")`, so a timed event is stored as all-day at 0:00. Patch: treat `VALUE=DATE` as its own parameter.
- medium — An impossible date or second 60 throws out of the event loop, and the fetch catch drops the whole ICS. Patch: skip that event and keep the rest.
- false — A missing `DTEND` uses the start as the end. `DURATION` is not the end field this story reads.
- low — A stored ICS URL that fails during generation leaves the paper and does not add a notice. Rejected: the bad-link sentence is for save, and the edition still completes.
- low — A failed generate after a failed calendar save shows only the calendar sentence, while the previous paper stays. Patch: show "That feed didn't load." and keep the calendar field error.
- low — A blank calendar field does not clear the URL, and `webcal` is not rewritten. Rejected: a blank feed does not clear either, and this story saves an http(s) link.
- false — A query token is the saved calendar address. Userinfo is refused so a username and password are not sent.
- false — The permission row opens the application details screen. The story asks for that screen, and the row text is the required sentence.
- low — An empty event list still says "No events today." under the permission sentence. Rejected: that is the empty copy, and the permission sentence is added above it.
- low — A null activity context reports permission on and a failed device read. Rejected: the activity sets the context before the page is shown.
- low — `STATUS:CANCELLED` is kept. Rejected: this story does not add a cancelled-event branch.
- false — An unrecognized timezone id is read in the system zone, which is the recorded rule.
- low — A corrupt `calendar-snapshot.json` fails generation. Rejected: a torn snapshot is not an everyday file.
- low — Saving a calendar before any feed leaves the sample path, and generate still requires a feed. Rejected: the paper is still generated from a feed.
- false — An all-day event is labeled `0:00`. Visible time is local `H:mm`, and the repository test locks that label.
- low — Overlapping source-list writes have no lock. Rejected: one person edits one field, and a lock is new machinery.
- false — Sprint status stays `in-progress` while this review sets the spec to `in-review`. The status sync is a later step.
- low — `openCalendarPermissionSettings` does not catch a missing settings activity. Rejected: application details is a platform screen, and a catch is an extra guard.
- low — `saveCalendar` success and failure are not called by a test, and the fake calendar save always fails. Patch: cover both, and a later generate that does not save again.
- low — `generateToday` is not run against a stored ICS URL that throws. Patch: the edition still succeeds and keeps snapshot events.
- low — `generateToday` is not run against a stored userinfo calendar URL. Patch: that URL is not fetched.
- defer — The Android `CalendarContract` reader and `writesDeviceCalendarSnapshot()` are not executed by the JVM suite. There is no instrumented test source to add.
- defer — The permission row is not composed. The copy constant is asserted. This repo does not run Compose tests.

## Design Notes

`writesDeviceCalendarSnapshot()` is true only on Android. A non-empty `Events` read writes `calendar-snapshot.json`. `Empty`, `Failed`, and `Unavailable` do not. Desktop, JS, and Wasm return `Unavailable`. Today is the start's civil date in the system zone, or the DATE value for an all-day event. A missing end equals the start. Visible time is local `H:mm`. The permission row says "Calendar permission is off." and opens the application details screen. Stored events still draw. If a dirty calendar field fails to save, generation uses the stored URL and the edition notice is the bad-link sentence.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: success
- `./gradlew :core:compileKotlinJs :core:compileKotlinWasmJs :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` -- expected: success, or a reported Yarn download failure
