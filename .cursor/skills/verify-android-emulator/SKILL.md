---
name: verify-android-emulator
description: >-
  Use when an Android UI change needs a real-device check, when launching or
  screenshotting the Android app, or when the user mentions the emulator,
  Medium_Phone, or adb. This machine has the Medium_Phone AVD on ~/Android/Sdk.
---

# Verify Android UI on the emulator

Internal skill for this computer. The Android SDK is `~/Android/Sdk`. The virtual device is `Medium_Phone`, the medium phone IntelliJ uses.

Check the real screen after an Android UI change. Leave the emulator running when you are done.

## In IntelliJ

When this session is running inside IntelliJ IDEA, the IDE's MCP server is how you reach the emulator. Use it before the shell steps below.

1. Call `get_run_configurations` and choose the Android app configuration.
2. Call `execute_run_configuration` with that name. IntelliJ starts `Medium_Phone`, builds, installs, and launches Matutine.
3. To debug, call `xdebug_start_debugger_session` with the same configuration name. Set a breakpoint first.
4. If those tools are not listed, the IDE is in router mode. Call them through `execute_tool`.

Do not start a second emulator from the shell while that run is active. After the app is in front, a screenshot can still use the `screencap` step below when the IDE tools do not return an image.

## Setup

Use this SDK's `adb` and `emulator`. A Debian `adb` may be earlier on `PATH`; do not use that one.

```bash
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
"$SDK/emulator/emulator" -list-avds
"$SDK/platform-tools/adb" devices -l
```

`list-avds` includes `Medium_Phone`. Export `ANDROID_HOME="$SDK"` for Gradle. Do not write `local.properties`.

## Run

1. If `adb devices` already shows an `emulator-*` serial, and `adb -s <serial> emu avd name` prints `Medium_Phone`, reuse that serial. A phone on the cable is not the target.
2. If `Medium_Phone` is down, start it in the background with `"$SDK/emulator/emulator" -avd Medium_Phone`. When `DISPLAY` is empty, add `-no-window`. Wait until `adb -s <serial> shell getprop sys.boot_completed` prints `1`.
3. Install only on that serial:

```bash
ANDROID_SERIAL=<emulator-serial> ./gradlew :app:androidApp:installDebug
```

4. Open the app:

```bash
"$SDK/platform-tools/adb" -s <emulator-serial> shell am start -n hu.toliver.matutine/.MainActivity
```

5. Wait until `dumpsys window` shows `hu.toliver.matutine` focused. Save a screenshot under `app/androidApp/build/verify/` so git ignores it:

```bash
mkdir -p app/androidApp/build/verify
"$SDK/platform-tools/adb" -s <emulator-serial> exec-out screencap -p > app/androidApp/build/verify/screen.png
```

Look at the PNG before deciding the UI is right. Another look at the same session is another `screencap`. Taps go through the same SDK `adb`.

Maestro flows and screenshot baselines are optional when a task needs scripted steps or a pixel check.

## Paths

In committed files, commits, and review comments, write `~/Android/Sdk`. Do not write an absolute home-directory path. If a tool prints one, rewrite it to a `~` path before you quote it.

## Before you finish

Re-read this skill and check the work:

- The screenshot file exists, and its first bytes are a PNG.
- `Medium_Phone` is still connected.
- Nothing you are about to commit contains an absolute home-directory path, a PNG, or `emulator.log`.
