# Matutine

Matutine is a morning newspaper for Android and Desktop (Kotlin Multiplatform, Compose). **This README's judge path** gets you from a fresh clone to a visible front page in a few minutes. You do not need an account, an AI key, or to answer any interest questions.

Release builds from GitHub Actions are Story 1.11; the steps below use a local debug build.

## Prerequisites

- Git
- **JDK 17 or newer** (the build uses the JDK that runs Gradle)
- A terminal at the repo root

**Android only:** either [Android Studio](https://developer.android.com/studio) with the Android SDK, or the SDK command-line tools with `ANDROID_HOME` pointing at that SDK (for example `~/Android/Sdk`). An emulator or a USB-connected phone with USB debugging enabled.

**Desktop only:** JDK 17+ is enough on Linux, Windows, or macOS.

## Judge path — Desktop

1. Clone the repository and `cd` into it.
2. Run:

   ```bash
   ./gradlew :app:desktopApp:run
   ```

3. On first launch the window opens on **today's sample edition**: masthead **Matutine**, a **SAMPLE EDITION** line, briefing, ranked stories, calendar events, and tasks. Tapping a headline opens its source in the system browser.
4. Optional: `./gradlew :app:desktopApp:hotRun --auto` starts the app with Compose hot reload for development.

No login, API key, or Interests screen appears on this path.

## Judge path — Android

### Option A — Android Studio (recommended)

1. Clone the repository.
2. Open the project folder in Android Studio and wait for Gradle sync.
3. Start an emulator or connect a phone with USB debugging.
4. Run the **androidApp** configuration (green run button).
5. On first launch you should see the same **sample edition** as on Desktop: **SAMPLE EDITION**, filled briefing, stories, calendar, and tasks.

### Option B — Command line

1. Clone the repository and `cd` into it.
2. Set `ANDROID_HOME` to your SDK (for example `export ANDROID_HOME=~/Android/Sdk`).
3. Start an emulator from Android Studio, or connect a device; confirm it appears in `adb devices`.
4. Build and install:

   ```bash
   ./gradlew :app:androidApp:installDebug
   ```

5. Open **Matutine** from the launcher. First launch shows the **sample edition** described above.

No login, API key, or Interests screen appears on this path.

## If the sample does not load

The page shows **The sample edition didn't load.** with a retry control. Use **Retry** once. **Settings** is available but not required for the judge demo.

## What this README does not cover

- **iOS** is not a target for this entry.
- **Docker, the server, and the browser client** are documented separately when that milestone ships; they are not part of this judge path.
- **Web** targets in this repo are for server-hosted use later, not an offline contest client.

---

## Project layout (contributors)

This is a Kotlin Multiplatform project with Android, Desktop (JVM), and server modules. Shared UI lives under [`app/shared`](./app/shared/src); domain logic under [`core`](./core/src).

### Other run targets

- Android APK only: `./gradlew :app:androidApp:assembleDebug`
- Server (not needed for the judge path): `./gradlew :server:run`
- Web app (server-hosted later): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun` or `./gradlew :app:webApp:jsBrowserDevelopmentRun`

### Tests

- `./gradlew :core:jvmTest :app:shared:jvmTest`
- Android instrumented tests: `./gradlew :app:shared:testAndroidHostTest`

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/).
