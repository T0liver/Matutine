---
title: 'Generate the morning edition on the server clock'
type: 'feature'
created: '2026-11-10'
status: 'in-review'
baseline_commit: '4b57ede7cf6ca7d43bbf180250b81d904035a479'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Today's morning edition appears only if a device is open or someone loads the paper. The server process has no clock, so a closed phone and desktop leave no edition.

**Approach:** While the Ktor process is running, a coroutine scheduler inserts today's morning edition at the configured time in the process timezone, and only when that document is absent. The generator assigns the day key. The AI key is read from the server environment and is not written onto a client or into the store.

## Boundaries & Constraints

**Always:**
- AD-11: only this server coroutine scheduler creates the morning edition at a configured time, and only when today's morning document is absent. AD-3: a later automatic fire does not write. The generator (`SampleGenerator.forDay` or `EditionGenerator.fromFeedResults`) assigns the day key. The scheduler does not format it.
- The zone string and the generation-time string come from the process environment. Blank zone uses the process zone. Blank generation time is `07:00`. A generation time `parseNotificationTime` accepts is the clock. The scheduler fires only while the local minute in that zone equals that clock, including when the process is already inside that minute. It does not catch up later in the day.
- An absent source list inserts the bundled sample. A present list, including an empty one, uses `runMorning` and does not fall back to samples. `serverConfigured` stays false on this process. `generateToday` is not the clock.
- An existing morning document, including a saved sample or a hand-edited briefing, is left unread from sources and is not replaced.
- The AI key is `MATUTINE_GEMINI_KEY`. Blank means no key. The repository sees it only in memory. `settings.json` keeps whatever key it already had, or stays absent. The web client is unchanged and does not receive the key.
- The same zone and clock feed `GET /editions/current` and the scheduler. A failed insert leaves stored documents unchanged and leaves the process running.

**Never:**
- No Admin form, light edit, client sync, public port, README change, or timezone-identifier design.
- Do not redo story 3.1's token gate, and do not add the other AD-16 routes.
- Do not start the Android emulator. Do not change device schedulers.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Due, no paper, no source list | Local minute equals the clock, no morning file, no `sources.json` | One sample morning file whose `dayKey` is the generator's date in the given zone | N/A |
| Zone crosses midnight | Instant is still the previous UTC date, zone string is the one the process was given | `dayKey` is the civil date `todayLocalDate` would use in that zone | N/A |
| Already stored | Morning file exists, briefing edited | Tick does not write; id, revision, and briefing stay | N/A |
| Second fire | Same local day after a successful insert | No second write and no source read | N/A |
| Not yet due | Local time is before the clock minute, and the process did not start inside it | No morning file is created | N/A |
| After the minute | Process observes only a later minute the same day | No catch-up write | N/A |
| Empty or bad list | Source list present, feeds missing or unreadable | No morning file; list file unchanged | Insert failure does not stop the process |
| AI key | `MATUTINE_GEMINI_KEY` set, personal morning inserted | Summarizer receives that key; `settings.json` does not gain it | Blank key leaves the extractive briefing |
| Stopped process | Module is not running | No scheduled insert | N/A |

</frozen-after-approval>

## Code Map

- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` — token and `GET /editions/current` already exist. Build one `LocalEditionRepository` with `today = { todayLocalDate(zone) }`, that `timeZone`, a JVM feed reader, and a Gemini `summarize`. Start the clock from `module` and cancel it on application stop. Do not call `generateToday`.
- `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` — documents under the data root. Do not add a second file layout.
- `core/.../EditionRepository.kt` — `runMorning` inserts only when absent and `serverConfigured` is false. `loadCurrentMorningEdition` inserts the sample when the source list is absent and returns an existing morning first. Reuse both. Do not change device `runMorning`.
- `core/.../DayKey.kt` — `todayLocalDate(timeZone)` and `toDayKey`. The repository calls these. The clock does not write a key.
- `core/.../ReaderSettings.kt` — `parseNotificationTime` and `DEFAULT_NOTIFICATION_TIME` (`07:00`). Reuse them. `geminiKey` stays on the settings document only when something else saved it.
- `core/.../EditionGenerator.kt` and `SampleGenerator.kt` — assign `dayKey` from the `LocalDate` they are given.
- `app/shared/.../FeedReader.jvm.kt` and `GeminiBriefingClient.jvm.kt` — the HTTP shape to copy into `:server`. Do not depend on `:app:shared`.
- `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` — story 3.1 rows stay. Add clock rows here. Tests pass zone, generation time, key, and instant in; they do not need Admin.

## Tasks & Acceptance

**Execution:**
- [x] `server/src/main/kotlin/hu/toliver/matutine/ServerMorningClock.kt` — resolve zone and generation time, decide when a tick is due, and insert through the repository only when due.
- [x] `server/src/main/kotlin/hu/toliver/matutine/ServerGeneration.kt` — JVM feed reader, in-memory Gemini key, and the Gemini summarize call. Never write the env key.
- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` — one zoned repository for the current-edition route and the coroutine clock.
- [x] `server/build.gradle.kts` — coroutines for the scheduler.
- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` — cover the I/O rows for due, zone day key, existing edition, second fire, not-due, no catch-up, bad list, and the env key.

**Acceptance Criteria:**
- Given the server is running and today's morning edition is absent, when the configured generation time is reached in the server timezone, then the server inserts today's morning edition and the generator assigns the day key in that timezone.
- Given today's morning edition already exists, when the schedule fires again, then it does not write.
- Given the process is not running, when the generation time passes, then no scheduled insert happens.
- Given an AI key in the server environment, when a morning edition is generated, then that key is used in memory and is not stored for clients.
- Given a timezone string, when the process receives it, then that string is the zone, with no identifier format added by this story.

## Implementation Notes

- The repository date is `clock().toLocalDateTime(zone).date`. With `Clock.System` that is the same civil date as `todayLocalDate(zone)`, and tests can pin the instant.
- A generation time `parseNotificationTime` rejects does not start the clock. Blank stays `07:00`.
- `MATUTINE_TIMEZONE`, `MATUTINE_GENERATION_TIME`, and `MATUTINE_GEMINI_KEY` are the environment names. The zone string is passed to `TimeZone.of`.
- Suspend functions cannot be Ktor attribute types, so tests install feed and summarize lambdas through a `ServerPorts` holder.
- The clock coroutine is a child of the application scope and is also cancelled on `ApplicationStopped`.
- `./gradlew :server:test` passed 17 tests, including the story 3.1 rows. `:core:jvmTest --tests hu.toliver.matutine.edition.LocalEditionRepositoryTest` stayed green.

## Spec Change Log

## Review Triage Log

## Design Notes

The clock is a one-second poll inside the server process. A tick is due when the zone-local hour and minute equal the configured clock. Starting during that minute still inserts once. Starting afterward waits until the next day, which is the clock-precise claim: a missed minute is not reconstructed. Absent `sources.json` uses the sample insert already used by `GET /editions/current`. A present list uses `runMorning`. Environment names are `MATUTINE_TIMEZONE`, `MATUTINE_GENERATION_TIME`, and `MATUTINE_GEMINI_KEY`.

## Verification

**Commands:**
- `./gradlew :server:test` -- expected: story 3.1 bearer and edition tests still pass, and the clock rows pass.
- `./gradlew :core:jvmTest --tests hu.toliver.matutine.edition.LocalEditionRepositoryTest` -- expected: existing `runMorning` tests still pass.
