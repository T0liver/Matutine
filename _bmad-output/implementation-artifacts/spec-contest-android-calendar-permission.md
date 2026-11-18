# Contest: sample calendar and the Android permission line

Status: ready to investigate on the connected phone, then implement.
Suggested epic: contest-hardening (Stories 1.1 and 1.7). Do not mark those stories done from this ticket.

## Problem

On a cleared install of the debug APK (phone SM-S916B, 2026-11-18), the sample front page showed bundled events "Team standup" 9:00 and "Lunch with Alex" 12:30, and directly above them the line "Calendar permission is off." A judge can read the sample calendar as broken. The JVM suite does not render this row; `deferred-work.md` already notes that the permission button is not composed in a test and the device calendar reader is not executed.

## What to do

Investigate on the real phone. USB debugging is available; do not start the emulator. Do not take a screenshot of the phone unless O allows that one shot. Use UI hierarchy text and logcat. Report what the line means on the sample edition, whether the bundled events are independent of the device calendar, and what a judge should see. Propose a fix, then implement it in this ticket.

## Acceptance criteria

The sample path no longer looks like the calendar failed. Device-calendar permission behavior for a real edition stays intact: when permission is off, the phone does not pretend to have read the user's calendar. Evidence is the phone UI text before and after the change.
