---
title: 'Walk a judge from clone to a front page'
type: 'feature'
created: '2026-11-09'
status: 'done'
route: 'oneshot'
story_key: '1-10-walk-a-judge-from-clone-to-a-front-page'
baseline_commit: '3700c127a6f3629f562aaee6910940817f03770e'
review_loop_iteration: 0
context:
  - _bmad-output/implementation-artifacts/epic-1-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The README is still the Kotlin Multiplatform starter template. Sam cannot follow it from a fresh clone to a newspaper front page on Android or Desktop.

**Approach:** Replace the top of `README.md` with a short judge path: prerequisites, Desktop run, Android run, and what a correct first screen looks like. Leave contributor-oriented notes below. Do not document Docker, the browser client, or iOS.

</frozen-after-approval>

## Implementation Notes

- Replaced the KMP starter README with a judge path (Desktop `run`, Android Studio or `installDebug`), sample-edition expectations, failure copy, and explicit non-goals (no iOS, no Docker/browser judge path).
- Pointed release artifacts at Story 1.11 so this path stays local-debug only.
- Skipped Blind Hunter review: session disallowed child agents.

## Verification

**Commands:**
- `./gradlew :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` — expected: BUILD SUCCESSFUL

**Manual checks (if no CLI):**
- Android UI launch and sample front page were not exercised in this session (emulator unavailable).

## Review Triage Log

- Review layers skipped — session instructions forbade spawning child agents; README steps were checked against epic-1 context, EXPERIENCE Flow 1, and successful Gradle commands above.

