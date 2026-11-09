---
title: 'Show today''s events from CalDAV'
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

**Problem:** Today's paper can show ICS and Android device events, but not a CalDAV calendar such as Baikal.

**Approach:** Save one CalDAV URL, username, and password as local configuration. Generation reads `VEVENT` from that calendar and joins those events to the ICS and device lists under the existing dedupe rule. A failed call omits that source, notes the failure on the edition, and still completes the paper.

## Boundaries & Constraints

**Always:**
- Identity stays start, end, and trimmed title. Which calendar an event came from is not part of it.
- The password is local configuration in the app data directory and is never committed, logged, or placed in a URL.
- The note for a failed CalDAV read is "That CalDAV calendar didn't load." on `edition.sourceNotes`.
- Saving the account does not contact a calendar server. The app starts with no Baikal process.
- ICS save, device snapshot rules, topic ranking, and the sample path stay as they are.

**Never:**
- Do not read `VTODO` or add Todoist.
- Do not send the password to the Matutine server, follow a redirect with it, or put it in `sources.json`.
- Do not query a calendar from the screen.
- Do not require Baikal for startup.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| CalDAV generate | Saved URL, username, and password; response has today's VEVENTs | Those events join ICS and device events, time-ordered, with title and start time | N/A |
| Dedupe | Same start, end, and trimmed title on CalDAV and another source | One event | N/A |
| Failed call | Saved account; the read throws or is not a calendar | Edition completes; CalDAV events omitted; source note is the failure sentence | Keep ICS and device events |
| VTODO body | Response also contains a VTODO | That to-do is not an event | Ignore VTODO |
| Startup | No generation | No CalDAV call | App loads the stored paper or sample |
| Password file | Account saved | Password is in `caldav.json` only | Absent from `sources.json` and the repo |

</frozen-after-approval>

## Code Map

- `core/.../edition/CalendarSnapshot.kt` — Reuse `editionEventsFrom`. Do not change identity.
- `core/.../edition/FeedDocuments.kt` — Reuse `parseIcsOccurrences` (VEVENT only). Add the failure sentence.
- `core/.../edition/EditionRepository.kt` — Add one account beside ICS and the snapshot. Do not fetch on load. Do not change ICS refusal of URL userinfo.
- `core/.../edition/EditionGenerator.kt` — Keep topic ranking. Append the CalDAV note after generation.
- `app/shared/.../store/FileEditionStore.*.kt` — `caldav.json` on JVM and Android. JS and Wasm store nothing.
- `app/shared/.../edition/CalDavReader.*.kt` — REPORT `VEVENT` on JVM and Android. No redirect. JS and Wasm fail the read.
- `app/shared/.../settings/SettingsScreen.kt` — URL, username, and password fields. Password is masked.
- `app/shared/.../edition/EditionScreen.kt` — Keep drawing `edition.events` and `sourceNotes`. Do not query CalDAV.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/CalDavAccount.kt` -- Account document and VEVENT response parser.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- Save the account with no network call. Union events. Note a failed read.
- [x] `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt` -- Persist `caldav.json`. Mirror on Android. JS and Wasm no-op.
- [x] `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/CalDavReader.jvm.kt` -- REPORT to the saved URL with Basic auth. Mirror on Android. JS and Wasm do not connect.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` -- Three fields. Wire them through the view model.
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` -- Union, dedupe, failure, VTODO ignored, no call on load.

**Acceptance Criteria:**
- Given O saves a CalDAV URL, username, and password that can read VEVENT, when today's edition is generated, then those events join the ICS and device events under the same dedupe rule, and the password is local configuration and is not committed.
- Given the CalDAV call fails, when generation runs, then that source is omitted, the failure is noted on the edition, and the edition still completes.
- Given the app starts, when no generation runs, then Baikal is not required.

## Implementation Notes

- The account is `caldav.json` in the data root. `savedCalDavAccount` returns a blank password. Save does not call the reader.
- JVM uses `java.net.http` with redirects off. Android uses `HttpURLConnection` with redirects off. Both send `Authorization` only on that request. JS and Wasm do not connect.
- A failed read appends "That CalDAV calendar didn't load." to `sourceNotes` and still saves the edition. ICS and device events stay on the same `editionEventsFrom` path.
- JVM tests for union, dedupe, a failed read, VTODO ignored, no call on load, and the password file passed. Desktop compile, Android debug assemble, and JS/Wasm compiles succeeded. No test opened a socket.
- Review patches: Android sets the request method field to REPORT, the desktop client is not closed, save accepts only https, an error multistatus is noted, and a failed CalDAV save shows the same sentence on the edition.

## Spec Change Log

## Review Triage Log

- high — Android `HttpURLConnection.setRequestMethod("REPORT")` throws `ProtocolException` because the platform allowlist excludes REPORT. Patched: set the inherited `method` field to REPORT, including the HTTPS delegate.
- medium — `HttpClient.close()` exists from JDK 21, while the module targets 11. Patched: one client for the process, never closed.
- false — Repeating events whose `DTSTART` is not today stay out. `parseIcsOccurrences` is the shared parser, and a later RRULE day is already outside today's start date.
- false — Redirects stay off. The frozen constraint forbids following a redirect with the password, and a 3xx is a failed read.
- medium — A multistatus whose statuses are all 4xx or 5xx, with no calendar body, was stored as an empty day. Patched: that body notes the failure. An empty multistatus with no error status stays an empty day.
- medium — Generate left Settings before the CalDAV field error could be seen. Patched: a failed CalDAV save sets the edition notice to "That CalDAV calendar didn't load." when the ICS notice is absent.
- medium — `http://` sent Basic auth in cleartext. Patched: save accepts only `https://`.
- low — Plaintext `caldav.json` and `allowBackup` are the local configuration store this story asks for. A backup rule is extra machinery a person does not meet in ordinary use. Rejected.
- low — IME Next does not move focus. A person can still tap the next field. Rejected.
- false — The CalDAV fields have URL, Username, and Password captions. Missing autofill content types are cosmetic and are not met as a broken save.
- false — A blank field does not delete the account, matching the ICS URL. A dead account is omitted and noted on the next generate.
- medium — A blank password with a changed username was untested. The code already refuses the write. Patched with a test, plus the same-URL keep.
- false — Sprint status stays `in-progress` while this step sets the spec to `in-review`. Status sync is the next step.
- defer — A corrupt `calendar-snapshot.json` fails generation. That read predates this story, and a torn snapshot is not an everyday file.
- defer — `recordDeviceCalendar` calls `today()` again after the edition date is captured. That midnight split predates this story.
- low — A second save can overlap generation. One person edits one account, and a lock is new machinery. Rejected.
- medium — The JVM and Android readers are not executed by a socket test. A local server would send the password. Patched the Android method and asserted the REPORT body and Basic value without opening a socket. Redirects remain off in the client.
- low — Settings hydration of the calendar URL and CalDAV login was untested. The init path already copies them and leaves the password blank. Patched with a test.
- low — A non-calendar body was untested. The parser already throws. Patched with a test.
- defer — The permission row is not composed. The copy constant is asserted. This repo does not run Compose tests. That row is Story 1.7.
- defer — The Android `CalendarContract` reader is not executed by the JVM suite. There is no instrumented test source. That reader is Story 1.7.

## Design Notes

The saved URL is the calendar collection. Generation sends one `REPORT` with a `VEVENT` comp-filter and `Authorization: Basic` only to that URL, with redirects off. The password is not shown again after save; a blank password keeps the stored one when the URL and username are unchanged. A changed URL or username with a blank password does not replace the account. `parseIcsOccurrences` remains the only event parser.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: success
- `./gradlew :core:compileKotlinJs :core:compileKotlinWasmJs :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` -- expected: success, or a reported Yarn download failure
