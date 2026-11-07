---
title: '1-3 Reflow the front page at 960px'
type: 'feature'
created: '2026-11-07'
status: 'review'
route: 'oneshot'
review_loop_iteration: 0
story_key: '1-3-reflow-the-front-page-at-960px'
context:
  - _bmad-output/implementation-artifacts/epic-1-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 1.1–1.2 render the edition as a single column on every client, so desktop wide windows do not show a story grid plus calendar/tasks rail at the 960px breakpoint.

**Approach:** In `:app:shared` `EditionScreen`, use `BoxWithConstraints` at 960dp on non-Android clients for a full-width masthead, briefing over story columns only, two-column stories, and a right rail with a vertical rule; keep one-column reading order on narrow/desktop-below-960 and on Android; bump interactive targets to 48dp on Android.

## Implementation Notes

- `EditionLayout.kt`: 960dp breakpoint, 280dp rail, `usesWideEditionLayout`, `storiesForTwoColumns` (left column first, then right).
- `EditionPlatform.kt` (+ platform actuals): Android forces single-column layout regardless of width.
- `EditionScreen.kt`: `BoxWithConstraints` wide body — masthead full width; briefing + two-column stories in main area; calendar/tasks in right rail with `VerticalDivider`; narrow/Android unchanged single column; Android story rows use `minimumInteractiveComponentSize()`.
- `EditionLayoutTest.kt`: unit tests for breakpoint and column split.

</frozen-after-approval>

## Verification

**Commands:**
- `./gradlew :core:allTests :app:shared:allTests :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: BUILD SUCCESSFUL
