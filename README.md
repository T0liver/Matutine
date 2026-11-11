# Matutine

Matutine is a morning newspaper for Android, Desktop, and the browser (Kotlin Multiplatform, Compose). **This README's judge path** gets you from a fresh clone to a visible front page in a few minutes — on a phone, on Desktop, and in a browser on your LAN. You do not need an account, an AI key, or to answer any interest questions.

Release builds from GitHub Actions are Story 1.11; the steps below use a local debug build.

## Prerequisites

- Git
- **JDK 17 or newer** (the build uses the JDK that runs Gradle)
- A terminal at the repo root

**Android only:** either [Android Studio](https://developer.android.com/studio) with the Android SDK, or the SDK command-line tools with `ANDROID_HOME` pointing at that SDK (for example `~/Android/Sdk`). An emulator or a USB-connected phone with USB debugging enabled.

**Desktop only:** JDK 17+ is enough on Linux, Windows, or macOS.

**Server and browser (Milestone B):** [Docker](https://docs.docker.com/engine/install/) with the `docker` CLI on the machine that runs the server.

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

## Judge path — Server and browser

Use a home or office LAN. **Do not** expose the server to the public internet — no reverse proxy, port forwarding, or tunnel for the contest judge path.

1. Clone the repository and `cd` into it (skip if you already did for Android or Desktop).
2. From the repo root, build the server image (the first build runs Gradle inside Docker and can take several minutes):

   ```bash
   docker build -f server/Dockerfile -t matutine-server .
   ```

3. Start the server on port 8080 and keep papers in a Docker volume:

   ```bash
   docker run --rm -p 8080:8080 -v matutine-data:/data matutine-server
   ```

   With no `MATUTINE_TOKEN` set, the shared token is the literal `matutine-demo`.

4. In a browser on the same machine, open `http://127.0.0.1:8080/`. From another device on the LAN, use `http://<this-host's-LAN-IP>:8080/` (plain `http` on the LAN is expected).

5. The token gate is the first screen: enter `matutine-demo` and submit. After a valid token you should see today's **sample edition** — masthead **Matutine**, **SAMPLE EDITION**, briefing, ranked stories, calendar events, and tasks — the same content shape as on Android and Desktop.

A wrong token shows **That token doesn't match.** and keeps what you typed. No login, AI key, or Admin setup is required for this path.

## If the sample does not load

The page shows **The sample edition didn't load.** with a retry control. Use **Retry** once. **Settings** is available but not required for the judge demo.

## What this README does not cover

- **iOS** is not a target for this entry.
- **VPN, reverse-proxy, and public-internet exposure** for the server — keep it on your LAN for the judge path.
- **Pulling a pre-published image from a registry** is release-workflow work; this path builds the image locally from `server/Dockerfile`.

---

## Project layout (contributors)

This is a Kotlin Multiplatform project with Android, Desktop (JVM), and server modules. Shared UI lives under [`app/shared`](./app/shared/src); domain logic under [`core`](./core/src).

### Other run targets

- Android APK only: `./gradlew :app:androidApp:assembleDebug`
- Server JVM dev (not the judge path; judges use Docker above): `./gradlew :server:run`
- Web app (server-hosted later): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun` or `./gradlew :app:webApp:jsBrowserDevelopmentRun`

### Tests

- `./gradlew :core:jvmTest :app:shared:jvmTest`
- Android instrumented tests: `./gradlew :app:shared:testAndroidHostTest`

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/).
