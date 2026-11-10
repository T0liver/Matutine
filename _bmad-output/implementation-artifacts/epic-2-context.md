# Epic 2 Context: Keep and change today's paper

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Epic 2 makes the morning edition something O can change on the device: tasks on the front page, Android notification and paper customization, desktop tray and typewriter, regenerate without losing edits, and optional Gemini briefing. The edition document remains the single source of truth for user-owned content.

## Stories

- Story 2.1: Check and add a task on the front page
- Story 2.2: Open the paper from a local notification
- Story 2.3: Customize the Android paper
- Story 2.4: Use the desktop tray
- Story 2.5: Rewrite the edition on the typewriter
- Story 2.6: Regenerate without losing the edits
- Story 2.7: Write the briefing with a Gemini key

## Requirements & Constraints

Milestone A tasks live on the edition JSON only. Checking or adding writes the edition through the repository, not a separate task service. Empty tasks copy is "No tasks yet." with the add line still shown. Save in flight uses no modal; the task block may show "Saved." after a confirmed write.

## Technical Decisions

Whole-document last-write-wins on the edition store. `:core` holds models and repository; `:app:shared` draws the task block. Generator clears tasks on generate until Story 2.6 merges user tasks.

## UX & Interaction Patterns

Task block: kicker label "Tasks", 16px ink-stroked checkbox, completed tasks in ink with one strike, add line in body on paper, confirm to add. "Saved." on the task block after a successful save.

## Cross-Story Dependencies

Epic 1 delivers the read-only task block and edition load path. Story 2.6 owns task survival across regenerate.
