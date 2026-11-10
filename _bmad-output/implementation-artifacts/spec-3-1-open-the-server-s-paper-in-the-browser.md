---
title: 'Open the server''s paper in the browser'
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

**Problem:** The LAN paper is not visible. `:server` is a hello-world Ktor process, and `:app:webApp` opens the on-device app, which generates a paper in the browser.

**Approach:** Ship a Dockerized Ktor process that stores documents under `/data`, requires one Bearer token, and serves a browser client whose first screen is a token gate and whose next screen is the sample newspaper generated on the server.

## Boundaries & Constraints

**Always:**
- Unset or blank `MATUTINE_TOKEN` is the literal `matutine-demo`. One shared token. Every edition API call uses `Authorization: Bearer <token>`.
- Documents live in the host directory mounted at `/data` (override `MATUTINE_DATA` only for tests). Same JSON codec and file names as the existing store: `sources.json`, `settings.json`, `calendar-snapshot.json`, `editions/<dayKey>-morning.json`.
- The only edition route in this story is `GET /editions/current`. A missing or wrong bearer token is rejected before the store is read and before any edition body is written.
- An absent source list and an absent morning document: the server builds the bundled sample through `LocalEditionRepository.loadCurrentMorningEdition` and returns it, with the sample line. An existing morning document is returned unchanged.
- The web client decodes `Edition` with `decodeEdition`. It does not declare an edition model and does not call the generator.
- The token gate is chrome: one field, one primary button labeled Continue. No paper and no Admin until the current-edition call succeeds. A rejected token shows "That token doesn't match." and keeps the typed value.

**Never:**
- No scheduler, no `runMorning`, no Admin, no light edit, no client sync, no Settings screen, no service worker, no public-port instructions, no README change, no timezone identifier format.
- Do not add the other AD-16 routes. Do not replace an existing morning edition. Do not depend on `:app:shared` from `:server`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Default token | Env token unset or blank | Accepted bearer is `Bearer matutine-demo` | N/A |
| Set token | `MATUTINE_TOKEN=lan-secret` | Only `Bearer lan-secret` is accepted | Other values rejected |
| No or wrong bearer | Missing header, non-Bearer scheme, or wrong token | 401, body is not an edition document, store is not read | N/A |
| Absent source list | Valid bearer, no `sources.json`, no morning file | 200 `Edition` JSON, `isSample` true, sample line text present in the decoded edition, morning file written, `sources.json` still absent | N/A |
| Existing morning | Valid bearer, morning file already present | 200 returns that document unchanged; revision and id stay | N/A |
| Wrong gate token | Browser submits a value the server rejects | Gate stays, field keeps the value, message "That token doesn't match.", Edition and Admin are not composed | N/A |
| Right gate token | Browser submits the server token and the source list is absent | Edition screen shows the returned paper, including SAMPLE EDITION | A non-401 failure stays on the gate and does not show a paper |

</frozen-after-approval>

## Code Map

- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- replace the hello route with config, auth, `GET /editions/current`, and static web files.
- `server/build.gradle.kts` -- already depends on `:core`. Keep that. Wire the Wasm distribution into the server resources.
- `core/.../EditionRepository.kt` -- `LocalEditionRepository.loadCurrentMorningEdition` already saves the sample when the source list is absent and returns an existing morning first. Reuse it. Do not call `runMorning`.
- `core/.../EditionJson.kt` -- `editionJson`, `encodeEdition`, `decodeEdition`. The only codec.
- `app/shared/.../FileEditionStore.jvm.kt` -- file names to match. Do not depend on this module from the server; it pulls Compose.
- `app/shared/.../EditionScreen.kt` -- newspaper drawing. Add a defaulted read-only switch so web can hide Settings, New edition now, and task writes. Android and Desktop call sites stay as they are.
- `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- today calls `App()`, which runs `MatutineApp` and the local generator. Replace that entry.
- `app/shared/.../App.kt` -- leave the device preview entry unchanged.
- `gradle/libs.versions.toml` -- Ktor 3.6.0 is the catalog pin. Do not bump it.

## Tasks & Acceptance

**Execution:**
- [x] `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` -- `EditionStore` on the data directory using core codecs and the existing file names.
- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- token resolution, bearer gate, current-edition route, static files from the built web app.
- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- cover the I/O rows that the server owns, including a store probe that fails if a rejected call touches documents.
- [x] `server/Dockerfile` -- image runs the fat jar, store path `/data`, token and other secrets only from the environment.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- defaulted read-only chrome so existing device screens keep their controls.
- [x] `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- token gate, then read-only `EditionScreen` from `GET /editions/current` decoded with `decodeEdition`.
- [x] `server/build.gradle.kts` -- depend on the Wasm browser distribution and package those files for the server to serve.

**Acceptance Criteria:**
- Given the Docker image and a host directory mounted at `/data`, when the container starts with no token in the environment, then the accepted token is `matutine-demo` and edition, settings, source list, and calendar snapshot files are under `/data`.
- Given the wrong token, when it is submitted, then the page says "That token doesn't match.", the field keeps the typed value, and neither the newspaper nor Admin is shown.
- Given the right token and an absent source list, when the gate opens, then the web Edition shows the server-generated sample paper and the sample line.
- Given any current-edition call, when the bearer token is missing or wrong, then the server rejects it before returning an edition.
- The web bundle does not invoke the generator, and it is not an offline app.

## Implementation Notes

- Token environment name is `MATUTINE_TOKEN`. Blank and unset both resolve to `matutine-demo`. Data directory is `MATUTINE_DATA`, default `/data`.
- The server store copies the desktop file names and uses the core codecs. It does not depend on `:app:shared`.
- `GET /editions/current` is the only API route. A rejected bearer returns plain text `unauthorized` and does not call the store.
- Sample generation is `LocalEditionRepository.loadCurrentMorningEdition`. `runMorning` is not called.
- The gate button label is Continue. The field label is Token. The web entry no longer calls `App()`.
- Wasm fetch uses the browser `window.fetch` Promise. JS uses the same call. Both send `Authorization: Bearer` and decode with `decodeEdition`.
- `./gradlew :server:test` passed 7 tests. `:app:shared:jvmTest --tests hu.toliver.matutine.edition.WebPaperGateTest` passed 4 tests. `:server:buildFatJar` produced `server/build/libs/server-all.jar`.
- The browser showed the chrome token gate at `http://127.0.0.1:8080/` with one field and Continue, and no newspaper. Compose on Wasm did not accept the automated click, so the mismatch sentence and the sample paper were not exercised in that browser session. Curl against the running jar confirmed 401 for a missing or wrong bearer and a sample edition for `Bearer matutine-demo`, with only `editions/<day>-morning.json` written.
- The first Docker build failed because the Wasm Node binary needs `libatomic.so.1`, which the Temurin JDK image does not ship. The build stage installs `libatomic1` before Gradle. The jar's bytecode is class version 65, so the runtime image is Temurin 21.

## Spec Change Log

## Review Triage Log

## Design Notes

The server implements `EditionStore` itself so the process can use `LocalEditionRepository` without taking a dependency on `:app:shared`. `GET /editions/current` is the one AD-16 route this story needs; the other routes stay absent until their stories. The gate compares nothing locally: only a successful current-edition response opens the paper. The primary button label is Continue. The field label is Token.

## Verification

**Commands:**
- `./gradlew :server:test` -- expected: bearer, sample, and existing-morning tests pass.
- `./gradlew :server:buildFatJar :app:webApp:wasmJsBrowserDistribution` -- expected: fat jar and Wasm distribution build.
- `docker build -t matutine -f server/Dockerfile .` -- expected: image builds when Docker is available.
