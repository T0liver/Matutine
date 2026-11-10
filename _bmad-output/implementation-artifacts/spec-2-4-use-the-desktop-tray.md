---
title: '2-4 Use the desktop tray'
type: 'feature'
created: '2026-11-10'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
story_key: '2-4-use-the-desktop-tray'
baseline_commit: '6d28e0fe87aa998421cdfc046153f9b2dbdf51b2'
context:
  - _bmad-output/implementation-artifacts/epic-2-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** With the window closed or minimized, O cannot open today's paper, generate, or quit without hunting for the Matutine process.

**Approach:** Register a Compose Desktop system tray while the process runs. The OS menu has exactly Open, Generate today's edition, and Quit. Closing the window hides it; Quit ends the process. Tray Generate calls the same `viewModel.generate()` path as Settings and returns to the edition screen. No custom menu styling and no desktop morning notification.

</frozen-after-approval>

## Tasks & Acceptance

**Execution:**
- [x] `app/desktopApp/.../main.kt` — Tray with three menu items; hide window on close; Quit exits.
- [x] `app/shared/.../MatutineApp.kt` — Tray open/generate signals match Settings generate behavior.

**Acceptance Criteria:**
- [x] Given the Desktop process is running, when O opens the tray menu, then it is the OS menu with exactly Open, Generate today's edition, and Quit.
- [x] Open shows today's edition; Generate runs the same action as Settings; Quit leaves the process.
- [x] The tray menu works with the main window hidden.
- [x] Given the process is closed, when the clock reaches morning, then no generation runs (unchanged Story 1.9 behavior).

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlin` — BUILD SUCCESSFUL

## Implementation Notes

- `main.kt` registers `Tray` with OS `MenuScope.Item` entries (Open, Generate today's edition, Quit). Window X hides when tray is supported; Quit calls `exitApplication`. Unsupported tray keeps close-to-quit.
- `MatutineApp` accepts monotonic `trayOpenRequest` / `trayGenerateRequest`; generate mirrors Settings (`viewModel.generate()` then edition screen).
- `MatutineTrayIcon.kt` draws a small AWT bitmap for the tray glyph.
- No desktop morning scheduler added (Story 1.9 unchanged). Skipped review subagents (parent: no child agents). No manual tray UI exercise on a live session (headless agent). No commit.

