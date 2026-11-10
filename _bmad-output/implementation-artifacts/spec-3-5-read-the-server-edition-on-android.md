---
title: 'Read the server edition on Android'
type: 'feature'
created: '2026-11-10'
status: 'in-review'
baseline_commit: 'b2628ff179903b75937c57e2a626840b980010c3'
route: 'dispatch'
review_loop_iteration: 0
story_key: '3-5-read-the-server-edition-on-android'
context:
  - _bmad-output/implementation-artifacts/epic-3-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The phone always reads and mints a local morning paper, so it cannot show the LAN server edition without uploading the papers already on the device.

**Approach:** Saving a server base URL binds an Android remote repository that reads, regenerates, and requests an extra through the server HTTP API. Local editions, sources, and the calendar snapshot stay on the device. An unreachable server shows that local copy and the required sentence. Generate then writes locally. Forget-server binds the local repository again.

## Boundaries & Constraints

**Always:** Persist the base URL and token only in a device-local `server-link.json`. A blank token is `Bearer matutine-demo` on the wire and is stored blank. Accept `http` and `https`; reject anything else with "Use an http or https address." and do not save. `GET /editions/current` shows the server edition with no device/server label. Transport failure, and any status other than 200, 401, or 404, shows the last local morning edition (or the empty front page) and "Couldn't reach the server. Showing the copy on this device." 401 uses "That token doesn't match." and keeps the saved link. 404 stays "today's morning edition is not available yet." Reachable Generate calls `POST /editions/morning/regenerate`. Reachable new-edition calls `POST /editions`, which runs the existing `newEditionNow` and does not merge fields. Transport failure on Generate writes through the local repository. The morning tick does not insert while a link is saved. The notification tick posts the existing notification only when a morning edition is actually returned. While a link is saved, phone Settings hides feed, calendar, CalDAV, interests, and the Gemini key; notification time, notification text, theme, and layout still save on the device. Forget-server deletes the link and shows the unchanged local source fields.

**Never:** Upload local editions, the source list, the calendar snapshot, or settings. Send or show the Gemini key in remote mode. Edit a server edition from Android, including task writes. Mint a morning edition from the remote client or the morning tick. Invent a missing-edition notification. Change `:app:desktopApp` repository selection or desktop Settings. Label the Edition as device or server. Start the emulator.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Save link | `http://192.168.0.5:8080` and a token | Only `server-link.json` changes | Invalid scheme is not saved |
| Read | Server returns today's edition | That edition is shown and not written into the local store | 401 keeps the link and uses the token sentence |
| Offline open | Transport throws; a local morning exists | That local edition plus the unreachable sentence | No local morning: empty front page plus the same sentence |
| Generate | Link saved and server reachable | `POST /editions/morning/regenerate` | Transport failure writes locally and does not POST sources |
| Extra | Link saved and server reachable | `POST /editions` inserts an extra on the server | Transport failure does not insert a local morning |
| Forget | Link saved | Source fields return with the pre-link local values | No download of server sources |

</frozen-after-approval>

## Code Map

- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- local repository and port. Remote delegates device settings here. `loadCurrentMorningEdition` still mints a sample; the unreachable path must read `EditionStore.loadMorningEdition` instead.
- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt` -- existing error sentences. Add the unreachable sentence beside them.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt` -- `TOKEN_DOES_NOT_MATCH`. Reuse it.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt` -- composition root. Keep constructing `LocalEditionRepository` for every platform; wrap it only when `readDeviceServerLink` returns a link. Desktop actuals return null.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` -- optional server fields. Defaults keep desktop sources, key, and interests visible.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt` -- `load` applies `consumeLoadNotice()`. Map the unreachable and token sentences onto `MissingMorning.notice` when no edition is returned. Do not add a source label.
- `server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt` -- regenerate exists. `POST /editions` and `GET /editions` do not. Add them without calling `loadCurrentMorningEdition` from the list route.
- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- `GET /editions/current` and bearer check. Do not change desktop or web clients.
- `app/androidApp/src/main/AndroidManifest.xml` -- `usesCleartextTraffic` is already true. Leave it.
- `app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt` -- skip `runMorning` when a link exists.
- `app/androidApp/src/main/kotlin/hu/toliver/matutine/EditionNotificationWorker.kt` -- remote tick posts only when a morning edition is returned. No new notification type.
- `app/desktopApp` -- do not change.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/ServerLink.kt` -- URL normalization, bearer token, link codec.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteEditionRepository.kt` -- remote port over a transport, local fallback only for Generate and for the stored copy.
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/RemoteEditionRepositoryTest.kt` -- matrix rows for save-less transport, read, offline, generate, extra.
- [x] `server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt` -- `POST /editions` and `GET /editions`.
- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- extra insert and list do not replace the morning edition.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt` -- surface the two exact sentences.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` and `MatutineApp.kt` -- Android server fields, hidden sources, forget.
- [x] `app/shared/src/androidMain` -- file link and HTTP transport. Other targets read no link.
- [x] `app/androidApp` morning and notification workers -- no remote morning insert; no missing-edition notification.
- [x] `app/shared/src/commonTest` -- view model shows the unreachable sentence on the stored edition and on the empty page.

**Acceptance Criteria:**
- Given a saved server URL, when the phone loads or generates, then no request writes local editions, sources, or the calendar snapshot.
- Given the server has today's edition, when the phone loads, then that edition is shown, including a later server edit, and the page has no device/server label.
- Given the server cannot be reached, when the Edition opens, then the last local edition and "Couldn't reach the server. Showing the copy on this device." are shown, and Generate still writes locally.
- Given the server is reachable, when O regenerates or asks for a new edition, then those calls run on the server and the phone does not insert a morning edition.
- Given a server is configured, when Settings is open, then feeds, calendar, and interests are hidden, notification time, notification text, theme, and layout remain, and Forget server brings the source fields back.

## Implementation Notes

- Device link is `server-link.json` beside the local documents. Saving it does not call the server.
- `POST /editions` calls `newEditionNow`. `GET /editions` lists today's stored morning plus extras and does not mint a sample.
- Unreachable and non-200/401/404 loads read `EditionStore.loadMorningEdition` and do not write it. Generate is the only local write on a transport failure.
- Desktop `readDeviceServerLink` returns null, so the shared composition root still binds `LocalEditionRepository` there. `:app:desktopApp` was not edited.
- Verified with `./gradlew :core:jvmTest :app:shared:jvmTest :server:test :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` and `:app:shared:compileKotlinJs`. No emulator.

## Spec Change Log

## Review Triage Log

## Design Notes

`POST /editions` is the missing HTTP face of `LocalEditionRepository.newEditionNow`. The route returns that edition document and does not merge fields. `GET /editions` returns today's stored morning, if any, plus today's extras, and must not mint a sample. The remote repository never calls `PUT /editions/by-id`, `PUT /sources`, `PUT /settings`, or `PUT /calendar-snapshot`. Android task edits fail and the existing view-model rollback keeps the server copy.

Desktop `readDeviceServerLink` stays null, so `MatutineApp` keeps the local repository there. Cleartext stays on the existing manifest flag.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :server:test :app:androidApp:testDebugUnitTest` -- expected: all tests pass, no emulator.
