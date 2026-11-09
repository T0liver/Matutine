- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md`
  summary: The edition screen's generating line, feed notice, and source notes are not composed in a test.
  evidence: View-model flags and sourceNotes are asserted, and the common test source set does not run Compose. Deleting the three text blocks in EditionContent would still leave those tests green.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md`
  summary: The Android INTERNET permission is not exercised by a device fetch.
  evidence: Removing the permission leaves the JVM and shared unit tests green. A device fetch would be needed to show that a missing permission becomes "That feed didn't load."

- source_spec: `_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md`
  summary: The Android and JVM feed readers are duplicated, and the Android cleartext flag is not fetched on a device.
  evidence: The copies and the manifest flag come from the feed reader, not from topic ranking. A device HTTP fetch would show whether cleartext still loads.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md`
  summary: Interests colors, the no-topic line, and the two questions are not composed in a test.
  evidence: Common tests assert the copy and `showsNoTopicLine` only. The test source set does not run Compose, so deleting those text blocks would stay green.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md`
  summary: The Android settings file is not the store the JVM test writes.
  evidence: `FileEditionStoreTest` runs the JVM actual. An Android host test would be needed to show the Android `settings.json` round trip.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md`
  summary: JS and Wasm settings and feed stubs are only compiled.
  evidence: Those actuals return null and fail the feed read. A JS or Wasm test is outside the story's compile check.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-7-show-today-s-events-from-ics-and-the-phone-calendar.md`
  summary: The Android calendar reader is not executed by the JVM suite.
  evidence: Repository tests inject a device read. `DeviceCalendar.android.kt` is only compiled into the debug APK, and this repo has no instrumented test source that can query `CalendarContract`.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-7-show-today-s-events-from-ics-and-the-phone-calendar.md`
  summary: The calendar permission row is not composed in a test.
  evidence: The sentence is asserted as copy. Deleting the permission button in `CalendarBlock` would leave the unit suite green, because Compose is not run here.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-8-show-today-s-events-from-caldav.md`
  summary: A corrupt calendar snapshot fails generation.
  evidence: `readCalendarSnapshot` still throws on bad JSON, and that read predates CalDAV. A torn snapshot is not an everyday file.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-8-show-today-s-events-from-caldav.md`
  summary: A device calendar read can use the next day if midnight passes during generation.
  evidence: `recordDeviceCalendar` calls `today()` again after the edition date is captured. That split predates CalDAV.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-8-show-today-s-events-from-caldav.md`
  summary: The calendar permission row is not composed in a test.
  evidence: The sentence is asserted as copy. Deleting the permission button would leave the unit suite green, because this repo does not run Compose tests. The row is Story 1.7.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-8-show-today-s-events-from-caldav.md`
  summary: The Android calendar reader is not executed by the JVM suite.
  evidence: Repository tests inject a device read. There is no instrumented test source that can query CalendarContract. The reader is Story 1.7.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: A corrupt calendar snapshot still aborts generation, and a failed ICS read is omitted without a note.
  evidence: `readCalendarSnapshot` throws on bad JSON, and `readIcsOccurrences` returns an empty list. Both rules are stories 1.7 and 1.8; this story leaves them in place. Feed and CalDAV failures are noted.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: The CalDAV REPORT still downloads the collection without a time range, and Android still sets that method by reflection.
  evidence: The request body and `useReportMethod` are story 1.8. Whether API 37 blocks the hidden method field needs a device REPORT.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: A stored http CalDAV URL can be sent in cleartext, and the password file is included in backup.
  evidence: New saves reject http. An already stored http URL and `allowBackup` come from story 1.8 and the existing manifest.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: Clearing the password blocks a URL or username change, and IME Next does not move between CalDAV fields.
  evidence: `saveCalDavAccount` rejects an empty password when the identity changes, and the field only handles `onDone`. Both are story 1.8.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: CalDAV status lines that use HTTP/2 are not classified.
  evidence: The parser matches `HTTP/1.0` and `HTTP/1.1` only. A captured HTTP/2 multistatus would show whether an error is treated as an empty calendar.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: The worker body and application startup are not executed, so a wrong insert or a dropped schedule would stay green.
  evidence: The unit test locks the 24-hour period, full flex, KEEP, and worker class. `doWork` needs an Android context this suite does not build.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: The desktop and Android CalDAV HTTP clients are not executed.
  evidence: Tests inject `readCalDav`. No test observes REPORT, Basic auth, or status handling on the real client. That client is story 1.8.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: The empty front page is not composed in a test.
  evidence: View-model tests assert `MissingMorning`. Deleting the masthead or adding a story list in `EmptyFrontPage` would stay green, because Compose is not run.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-keep-today-s-paper-on-the-next-morning-run.md`
  summary: The calendar permission row and the Android calendar query are not executed.
  evidence: The permission sentence is a constant, and device events are injected. Both are story 1.7, and this suite has no Compose or instrumented test.
