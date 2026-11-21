# Contest: `./gradlew test check` compiles on JS and Wasm

Status: done (2026-11-21). Resolution A plus follow-up fixes. `./gradlew test check` is green.
Suggested epic: contest-hardening. Do not mark any epic done from this ticket.

## Problem

On 2026-11-18 `./gradlew test check --console=plain` exited 1 in 48s. `:core:compileTestKotlinJs` and `:core:compileTestKotlinWasmJs` failed. Common tests call `runBlocking`, which those targets do not provide (`Unresolved reference 'runBlocking'`, and suspend calls outside a coroutine). `:server:test`, `:core:jvmTest`, and `:app:shared:jvmTest` were already green (183 tests). Android, Desktop, and the server app are not what failed. The web client still served.

## What to do

Review the failure and write two or three concrete resolutions in this file (for example: keep common tests JVM-only, replace `runBlocking` with a multiplatform test helper, or split the blocking tests out of `commonTest`). Stop there and wait for O to choose. After a choice is recorded here, implement that resolution in this same ticket and show `./gradlew test check` green, or name the remaining target that is still red and why.

## Review (2026-11-21)

Reproduced with `./gradlew :core:compileTestKotlinJs :core:compileTestKotlinWasmJs --continue --console=plain`. Both tasks failed (`BUILD FAILED in 3s`) with the same errors: `Unresolved reference 'runBlocking'` at the import and at each test body, then `Suspend function ... can only be called from a coroutine` for calls such as `loadCurrentMorningEdition()` and `saveFeedUrl(url)`.

What the review found:

- Only two files use `runBlocking`: `core/src/commonTest/.../edition/LocalEditionRepositoryTest.kt` and `RemoteEditionRepositoryTest.kt`. `EditionTranscriptTest.kt` and `ReaderSettingsTest.kt` are already plain common tests.
- `core` has only `commonMain` and `commonTest`. There is no `jvmTest` source set. `commonTest` depends on `kotlin-test` and `kotlinx-coroutines-core`. `kotlinx-coroutines-test` is not in `gradle/libs.versions.toml`.
- The tests contain no `launch`, `async`, `delay`, `Dispatchers`, channels, or mutexes. Each test is a straight sequence of suspend calls, so a virtual-time runner would not change their behavior.
- The repositories ship on the web. `app/shared/src/commonMain/.../MatutineApp.kt` uses them, and `app/shared` has `jsMain` and `wasmJsMain`. Coverage that runs only on the JVM would miss the code the browser client actually runs.
- Whichever option makes JS and Wasm compile, `check` will then reach `:core:jsBrowserTest` and `:core:wasmJsBrowserTest` for the first time. Those tasks need a headless Chrome. This machine has Chrome at the default install path. CI has not been checked.

### Resolution A: switch to `runTest` from `kotlinx-coroutines-test` (recommended)

- `gradle/libs.versions.toml`: add `kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }` (1.11.0, the same version as core).
- `core/build.gradle.kts`: add `implementation(libs.kotlinx.coroutines.test)` to `commonTest.dependencies`.
- The two test files: replace `import kotlinx.coroutines.runBlocking` with `import kotlinx.coroutines.test.runTest`, and change `= runBlocking {` to `= runTest {`. The tests keep their expression bodies, which `runTest` needs so that JS can return its `Promise`.
- Gain: the same tests run on JVM, Android host, JS, and Wasm, which covers the repository code the web client uses.
- Risk: `runTest` uses virtual time and has a 60-second default timeout per test. Neither matters here because the tests have no `delay` or concurrency. The browser test tasks now run inside `check` (see above).

### Resolution B: move the two blocking test files into a new `jvmTest` source set

- Move `LocalEditionRepositoryTest.kt` and `RemoteEditionRepositoryTest.kt` from `core/src/commonTest/kotlin/hu/toliver/matutine/edition/` to `core/src/jvmTest/kotlin/hu/toliver/matutine/edition/`, keeping `runBlocking` unchanged.
- `core/build.gradle.kts`: nothing has to change, because `kotlinx-coroutines-core` already reaches `jvmTest` through `commonTest`. Optionally move that dependency to `jvmTest.dependencies` so that no common test can use `runBlocking` again.
- Gain: no new dependency and no edits inside the tests. JS and Wasm compile and run only the two plain common tests.
- Loss: the repository tests stop running on JS, Wasm, and the Android host test, even though the repository code ships on all of them. Any shared fakes the two files use from `commonTest` remain visible, because `jvmTest` depends on `commonTest`.

### Resolution C: turn off JS and Wasm tests for `core` (not recommended)

- `core/build.gradle.kts`: disable `compileTestKotlinJs`, `compileTestKotlinWasmJs`, `jsBrowserTest`, and `wasmJsBrowserTest` for `core`, for example with `tasks.matching { it.name in setOf(...) }.configureEach { enabled = false }`.
- Gain: only one file changes and the build turns green fastest. No test code changes and no browser is needed.
- Loss: no common test in `core` runs on the web, including the two that would already pass. Any future break in a common test on JS or Wasm also stays hidden. This is a workaround, not a fix.

Rejected without a write-up: a hand-written `expect`/`actual` helper (`runBlocking` on the JVM, a `Promise` on JS and Wasm). It would reimplement `runTest` with less coverage of edge cases.

## Choice

2026-11-21: O chose **A** (switch to `runTest` from `kotlinx-coroutines-test`).

## Outcome (2026-11-21)

Changed `gradle/libs.versions.toml`, `core/build.gradle.kts`, and the two test files as described in Resolution A. The tests contained 65 `= runBlocking {` bodies and 2 imports, and all were replaced.

`./gradlew test check --continue --console=plain` (with `ANDROID_HOME` pointing at the local SDK) exited 1 after 11m 36s. The JS and Wasm test compiles now succeed, and none of the 65 converted tests failed on any target. `check` is still red in three tasks that the compile failure used to hide:

- `:core:allTests`: 4 of 90 failed in `:core:jsBrowserTest`, all in `EditionTranscriptTest`. Cause: in `core/src/commonMain/.../edition/EditionTranscript.kt:12`, `Regex("""^\[(x|X| )] ?(.*)$""")` has an unescaped `]`. Kotlin/JS compiles it to `/.../gu`, and Chrome rejects that with `Invalid regular expression ... Lone quantifier brackets`. The first test gets the `SyntaxError`, and the others then get `TypeError ... matchEntire` on the undefined regex. This is a production bug that affects the JS web client, not a test problem. `:core:wasmJsTest` passed.
- `:app:shared:allTests`: 6 of 56 failed in `:app:shared:jsBrowserTest`, all in `EditionTypewriterStateTest`, with the same `SyntaxError` followed by `TypeError`. These tests go through the same `EditionTranscript` regex.
- `:app:androidApp:lintDebug`: lint found 13 errors and 13 warnings. The first is `MissingPermission` at `EditionNotifications.kt:49` (`NotificationManagerCompat.notify` without a permission check). This is not related to this ticket. It was probably never reached before, because the build stopped at the first failure without `--continue`.

None of these was fixed in that first pass, because each one needs production code changes outside the scope of Resolution A.

## Follow-up fixes in this ticket (2026-11-21)

O chose to fix all three in this ticket.

- `core/src/commonMain/.../edition/EditionTranscript.kt:12`: escaped the bracket, giving `Regex("""^\[(x|X| )\] ?(.*)$""")`. The JVM matches it the same way, and JS now accepts it. All other `Regex(...)` patterns in `commonMain` were checked and are valid under the JS `u` flag.
- Lint `NewApi` (12 errors): `EditionNotificationSchedule.kt` uses `java.time`, which needs API 26, while minSdk is 24. `MatutineApplication.onCreate` calls `scheduleEditionNotification`, so on API 24 and 25 the app would crash at startup. The fix enables core library desugaring: `isCoreLibraryDesugaringEnabled = true` plus `coreLibraryDesugaring(libs.desugar.jdk.libs)` in `app/androidApp/build.gradle.kts`, and `desugar-jdk-libs = "2.1.5"` (the latest stable release) in `gradle/libs.versions.toml`. Raising minSdk to 26 would also work, but that is a product decision.
- Lint `MissingPermission` (1 error): `EditionNotificationWorker` already checks `notificationsMayPost` before posting. The permission can still be revoked between that check and `notify`, so `EditionNotifications.kt` now catches `SecurityException` around `notify`.

Lint now reports 0 errors. The 13 warnings remain (newer dependency versions, the `drawable-v24` folder, a monochrome launcher icon). Warnings do not fail `check`, and they are left as they are.

`./gradlew test check --console=plain` exited 0 with `BUILD SUCCESSFUL in 4m 43s`. Test results:

| Task | Tests | Failed |
| --- | --- | --- |
| `:core` jvm, androidHost, jsBrowser, wasmJsBrowser | 90 each | 0 |
| `:app:shared` jvm | 69 | 0 |
| `:app:shared` androidHost, jsBrowser, wasmJsBrowser | 56 each | 0 |
| `:app:androidApp:testDebugUnitTest` | 9 | 0 |
| `:server:test` | 26 | 0 |

Not checked on a device: whether the desugared `java.time` path works on a real API 24 device. `Medium_Phone` was not used in this ticket.

Environment note: on this Windows machine the SDK is in the per-user `AppData\Local\Android\Sdk` folder, not `~/Android/Sdk` as `AGENTS.md` and the verify-android-emulator skill say. `test check` fails with `SDK location not found` unless `ANDROID_HOME` is set.
