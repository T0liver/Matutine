# Contest: Android "keeps stopping" dialog during the sample session

Status: ready to reproduce on the connected phone, then fix if it is a real crash.
Suggested epic: contest-hardening. Do not mark Epic 1 done from this ticket.

## Problem

On 2026-11-18, after a cleared install and a sample front page, the phone showed the system dialog "A(z) Matutine alkalmazás állandóan leáll" (Matutine keeps stopping), with App info and Close app. O cancelled that dialog. Logcat no longer held a stack trace. A later `am start` stayed on `MainActivity` with no fatal exception. The tap under test was the Settings control at bounds `[79,210][239,272]` on a 1080×2340 screen. A following UI dump was the launcher, not Settings.

## What to do

Recreate the path on the connected phone: install the debug APK, clear app data, open the sample, open Settings. Do not start the emulator. Do not screenshot the phone unless O allows that one shot. Clear logcat first and save the fatal stack if the dialog returns. If O's cancel was the whole event and the app does not crash, write that conclusion here and stop. If there is a crash, find the cause and fix it in this ticket.

## Acceptance criteria

Either a saved log proves the app does not crash on that path, or the crash is fixed and the same path opens Settings without the dialog. Evidence is the log excerpt and the UI text after the tap.
