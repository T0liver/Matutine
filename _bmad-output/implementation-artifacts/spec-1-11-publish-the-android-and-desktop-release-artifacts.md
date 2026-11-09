---
title: 'Publish the Android and Desktop release artifacts'
type: 'feature'
created: '2026-11-09'
status: 'done'
route: 'oneshot'
story_key: '1-11-publish-the-android-and-desktop-release-artifacts'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Judges and entrants need installable Android and Desktop clients without assembling a local release build.

**Approach:** Add one GitHub Actions release workflow that builds Android `.apk` and `.aab` on Ubuntu, and unsigned Desktop `.msi`, `.deb`, and `.dmg` installers each on the matching OS runner. Trigger on push to `release` or a merged pull request to `main` labeled `version`. Do not publish Docker images.

## Implementation Notes

- Added `.github/workflows/release.yml` with four parallel jobs: Android (apk+aab on ubuntu), Linux deb, Windows msi, macOS unsigned dmg. Triggers: push to `release`, or merged PR to `main` with label `version`. Skips `notarizeDmg` and any Docker/server build. Review subagents skipped (parent forbade child agents).

## Review Triage Log

- Skipped Blind Hunter — parent session forbade child agents; no automated review loop run.

</frozen-after-approval>
