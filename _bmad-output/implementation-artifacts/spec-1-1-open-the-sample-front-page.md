---
title: '1-1 Open the sample front page'
type: 'feature'
created: '2026-11-07'
status: 'review'
route: 'oneshot'
review_loop_iteration: 0
baseline_commit: '886128c1413f57867c1942a71feca6c2deeed27f'
story_key: '1-1-open-the-sample-front-page'
context:
  - _bmad-output/implementation-artifacts/epic-1-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Android and Desktop still show the KMP template screen; first launch must show today's bundled sample newspaper with no account or key.

**Approach:** Add edition models and sample fixture in `:core`, a local repository that serves the sample when no source list exists, and a one-column Edition screen in `:app:shared` wired from both clients with platform URL opening.

## Implementation Notes

- Edition models, JSON codec, `SampleEdition` fixture, and `LocalEditionRepository` live in `:core`.
- `:app:shared` adds `EditionScreen`, `EditionViewModel`, `FileEditionStore`, and `openUrl` platform hooks; `MatutineApp` replaces the template demo.
- `androidApp` and `desktopApp` pass each platform data root into `MatutineApp`.
- Sample load failure UI is wired; injectable `forceSampleLoadFailure` on the repository exists for tests but is not exposed in production UI paths yet.

</frozen-after-approval>

## Verification

**Commands:**
- `./gradlew :core:allTests :app:shared:allTests :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: BUILD SUCCESSFUL
