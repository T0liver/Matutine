---
title: '2-3 Customize the Android paper'
type: 'feature'
created: '2026-11-10'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
story_key: '2-3-customize-the-android-paper'
context:
  - _bmad-output/implementation-artifacts/epic-2-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Android Settings cannot change the notification sentence, theme, or layout, so the phone paper and morning notice stay fixed at defaults.

**Approach:** Persist notification body, theme id, and layout id on `ReaderSettings` (defaults: current body sentence, Newsprint theme, Newsprint columns layout). Android-only Settings rows save those values. The edition renders with resolved theme colors and layout preference; the notification worker posts the saved body. Only Newsprint palette and column behavior ship; unnamed future ids fall back safely. Desktop gains no new rows.

</frozen-after-approval>

## Implementation Notes

- Extended `ReaderSettings` with `notificationBody`, `editionTheme`, and `editionLayout`; defaults preserve Story 2.2 behavior and Newsprint.
- `EditionRepository` saves body, theme, and layout; `EditionNotificationWorker` posts the resolved body.
- Android-only Settings rows (`showsAndroidPaperCustomization`): notification text, theme, and layout pickers; Desktop unchanged.
- `MatutineEditionColors` + `LocalMatutineEditionColors`; `MatutineTheme` and `EditionScreen` use edition tokens (chrome stays Newsprint).
- Theme/layout option lists ship with Newsprint only; unknown ids fall back to defaults until palettes or layouts are designed.
- Skipped review subagents (parent: no child agents). No emulator. No commit.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:androidApp:assembleDebug` — BUILD SUCCESSFUL

