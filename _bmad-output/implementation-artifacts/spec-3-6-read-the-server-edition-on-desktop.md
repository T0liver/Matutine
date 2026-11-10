---
title: 'Read the server edition on Desktop'
type: 'feature'
created: '2026-11-10'
status: 'in-review'
route: 'dispatch'
review_loop_iteration: 0
story_key: '3-6-read-the-server-edition-on-desktop'
baseline_commit: 'b2628ff179903b75937c57e2a626840b980010c3'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Desktop still binds the local repository, so it mints and edits a private paper instead of the LAN edition.

**Approach:** Saving a server base URL binds the existing remote repository. Desktop reads, regenerates, and requests an extra through that client, and a typewriter save writes the open server edition. Local editions and the local source list stay on the device.

## Boundaries & Constraints

**Always:** Persist the base URL and token only in `server-link.json`. A blank token is `Bearer matutine-demo` on the wire and is stored blank. Accept `http` and `https`; reject anything else with "Use an http or https address." and do not save. With a link, `GET /editions/current` is the front page and Desktop does not mint a morning edition. Transport failure, and any status other than 200, 401, or 404, shows the last local morning edition (or the empty front page) and "Couldn't reach the server. Showing the copy on this device." 401 uses "That token doesn't match." and keeps the link. 404 stays "today's morning edition is not available yet." Reachable Generate calls `POST /editions/morning/regenerate`. Reachable new-edition calls `POST /editions`. A typewriter save of the open edition calls `PUT /editions/by-id` and does not write the local edition store. An unreachable typewriter save fails with "Couldn't save. Your text is still here." and does not write locally. Transport failure on Generate writes through the local repository. While a link is saved, Desktop Settings shows the server address, the token, and Forget server, and hides feeds, calendar, interests, and the Gemini key. Theme, layout, notification time, and notification text stay absent. Forget-server deletes the link and shows the unchanged local source fields. The Edition has no device or server label. Android still refuses edition writes.

**Never:** Upload local editions, the source list, the calendar snapshot, or settings when the link is saved. Mint a morning edition from Desktop while a link is saved. Invent a field-level merge for regenerate. Add a Desktop morning notification. Label the Edition as device or server. Change Android repository selection or Android Settings.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Save link | `http://192.168.0.5:8080` and a token | Only `server-link.json` changes | Invalid scheme is not saved |
| Read | Server returns today's edition | That edition is shown and not written into the local store | 401 keeps the link and uses the token sentence |
| Offline open | Transport throws; a local morning exists | That local edition plus the unreachable sentence | No local morning: empty front page plus the same sentence |
| Generate | Link saved and server reachable | `POST /editions/morning/regenerate` | Transport failure writes locally and does not POST sources |
| Extra | Link saved and server reachable | `POST /editions` inserts an extra on the server | Transport failure does not insert a local extra |
| Typewriter | Link saved and server reachable | `PUT /editions/by-id` returns the saved edition | Transport failure does not write the local store |
| Forget | Link saved | Source fields return with the pre-link local values | No download of server sources |

</frozen-after-approval>

## Code Map

- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteEditionRepository.kt` -- remote port. Generate, extra, and unreachable read already exist. `updateMorningEdition` and `updateExtra` refuse every caller. Desktop writes must PUT; the default stays a refusal so Android task edits still roll back.
- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/ServerLink.kt` -- URL, bearer, and `server-link.json` codec. Reuse. Do not upload on encode.
- `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.jvm.kt` -- Desktop actuals return no link. This is the desktop file store. Android's `FileServerLinkStore` is the pattern. Do not change the Android actual.
- `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.jvm.kt` -- throws. Replace with the Android `HttpURLConnection` exchange. JVM cleartext HTTP needs no manifest flag.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt` -- binds `RemoteEditionRepository` when `readDeviceServerLink` returns a link. Pass edition writes only where `showsTypewriter()` is true. `phoneSettingsVisibility` already hides sources when a link exists.
- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` -- server, token, and Forget server already render. Theme, layout, and notification rows render only when their arguments are non-null. Desktop actuals already pass null. Do not add those rows.
- `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/settings/PaperCustomizationSettings.jvm.kt` and `NotificationSettings.jvm.kt` -- both false. Leave them false.
- `server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt` -- `PUT /editions/by-id` calls `updateMorningEdition` only. Dispatch `EditionRole.extra` to the existing `updateExtra`. Do not add a merge.
- `app/desktopApp` -- composition stays `MatutineApp(desktopDataRoot())`. No tray notification. Do not add a morning tick.
- `app/androidApp` -- do not change.

## Tasks & Acceptance

**Execution:**
- [x] `core/.../RemoteEditionRepository.kt` -- optional edition writes via `PUT /editions/by-id`; default remains refusal.
- [x] `core/.../RemoteEditionRepositoryTest.kt` -- typewriter PUT does not write the local store; Android-style refusal stays the default.
- [x] `server/.../ServerRoutes.kt` and `ApplicationTest.kt` -- extra role uses `updateExtra`; a later current-edition read shows a morning edit.
- [x] `app/shared/src/jvmMain` -- file link and HTTP transport. `showsServerLinkSettings` is true only on JVM.
- [x] `app/shared/.../MatutineApp.kt` -- desktop remote binding writes editions; Android does not.
- [x] `app/shared/src/jvmTest` -- saving a link does not touch local documents; remote desktop settings expose address, token, and forget, and not theme, layout, or notification text.

**Acceptance Criteria:**
- Given a saved server URL, when Desktop loads or generates, then no request writes local editions or the local source list, and Desktop does not insert a morning edition.
- Given the server has today's edition, when Desktop loads, then that edition is shown and the page has no device or server label.
- Given a typewriter save, when the server is reachable, then `PUT /editions/by-id` stores that edition and a later read of the current morning edition shows a morning edit.
- Given the server cannot be reached, when the Edition opens, then the last local edition and "Couldn't reach the server. Showing the copy on this device." are shown, and Generate still writes locally.
- Given the server is reachable, when O regenerates or asks for a new edition, then those calls run on the server.
- Given a server is configured, when Desktop Settings is open, then it shows the address, the token, and Forget server, and it does not show theme, layout, or notification text.

## Implementation Notes

- Desktop binds `RemoteEditionRepository` only when `server-link.json` exists. `writesEditions` is `showsTypewriter()`, so Android still refuses edition writes.
- A reachable typewriter save PUTs the whole edition. An unreachable save returns the existing save-failed sentence and does not write the local store.
- `PUT /editions/by-id` sends an extra to the existing `updateExtra`. Morning edits still use `updateMorningEdition`.
- Verified with `./gradlew :core:jvmTest :app:shared:jvmTest :server:test :app:desktopApp:compileKotlin`. No emulator.

## Spec Change Log

## Review Triage Log

## Design Notes

`writesEditions` defaults to false so the Android remote repository keeps refusing `updateMorningEdition` and `updateExtra`. Desktop passes true because `showsTypewriter()` is the existing desktop-only edit gate. The PUT body is the whole edition document. The server assigns the next revision. Regenerate and extra stay the existing POST routes.

Desktop Settings reuse `phoneSettingsVisibility(serverLink != null)` for feeds, calendar, interests, the Gemini key, and Forget server. Theme, layout, and notification text stay off because the JVM paper and notification actuals are already false.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :server:test :app:desktopApp:compileKotlin` -- expected: tests pass and Desktop compiles. No emulator.
