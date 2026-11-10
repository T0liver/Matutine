---
title: '2-2 Open the paper from a local notification'
type: 'feature'
created: '2026-11-10'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
story_key: '2-2-open-the-paper-from-a-local-notification'
context:
  - _bmad-output/implementation-artifacts/epic-2-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Android has no local notification to open today's morning edition at the time O sets.

**Approach:** Store notification time on the settings document (default 07:00 device zone). Schedule approximate daily WorkManager work at that time. When today's morning edition exists and posting is allowed, show a system notification (title Matutine, body "Today's edition is ready."). Tapping opens the app on the edition. Skip posting when permission is off or the morning file is absent.

</frozen-after-approval>

## Implementation Notes

- `ReaderSettings.notificationTime` defaults to `07:00`; parsing helpers live in `:core`.
- Android-only Settings row saves time and reschedules via `NotificationScheduleCallbacks`.
- `EditionNotificationWorker` uses one-shot WorkManager with `REPLACE`, posts only when permission allows and today's morning file exists, then reschedules the next day.
- `MorningEditionWorker` unchanged (insert-if-absent only; no notification).
- Skipped blind-hunter and other review subagents (parent session: no child agents). No emulator run. No commit per user.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:androidApp:assembleDebug` — success
