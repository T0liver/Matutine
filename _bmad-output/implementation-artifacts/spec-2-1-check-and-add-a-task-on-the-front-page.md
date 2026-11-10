---
title: '2-1 Check and add a task on the front page'
type: 'feature'
created: '2026-11-10'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
story_key: '2-1-check-and-add-a-task-on-the-front-page'
context:
  - _bmad-output/implementation-artifacts/epic-2-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Tasks on the front page are read-only. O cannot check or add tasks on the edition.

**Approach:** Wire the task block to write today's morning edition through the repository. Checkbox toggles completion; the add line confirms a new task. Show "Saved." after a successful write; no modal while saving.

</frozen-after-approval>

## Implementation Notes

- `updateMorningEdition` on `LocalEditionRepository` bumps revision and writes the full morning edition; no separate task store.
- `EditionViewModel` toggles and adds with optimistic UI, rolls back on failure, sets `taskSaved` after success.
- `TaskBlock` is interactive: filled checkbox, strike-through, add `OutlinedTextField` with Ime Done, `"Saved."` meta line.
- Skipped blind-hunter and other review subagents (parent session: no child agents). No commit per user.
