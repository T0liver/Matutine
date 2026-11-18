# Contest: publish a 0.x.x release the judge can download

Status: ready for an agent to implement.
Suggested epic: contest-hardening (Stories 1.11 and 3.8). Do not mark those stories done from this ticket.

## Problem

`.github/workflows/release.yml` uploads an APK, an AAB, Linux deb, Windows msi, macOS dmg, and a Docker image tarball. It runs only on a push to branch `release`, or a merged pull request into `main` labeled `version`. This clone has only `main`. `gh release list` is empty. The README judge path is a local Gradle build and a local `docker build`. Artifact names are Actions upload names (`matutine-android-apk` and similar), not versioned files.

## What to do

Create the `release` branch, finish the workflow so a run publishes a GitHub Release, and run that workflow once for a `0.x.x` version (start at `0.1.0` unless the repo already has a higher version). Each desktop installer stays on its matching runner. The macOS build stays unsigned and not notarized. The token and the Gemini key stay out of the image.

Published file names:

`matutine-[platform id]-vX.Y.Z.[extension]`

Use these platform ids: `android` (apk), `android-bundle` (aab), `linux` (deb), `windows` (msi), `macos` (dmg), `server` (the Docker image `.tar.gz`). Example: `matutine-android-v0.1.0.apk`.

Point the README judge path at the published release as well as the local debug steps.

## Acceptance criteria

The GitHub Release exists, the six files are attached under those names, and a judge can download them without building. The workflow run is linked from the ticket notes.
