# Starter: Matutine sprint planning

Run this in a fresh session. Do the whole sprint-planning intent in one pass. Do not implement product code.

## Skill

Invoke `bmad-sprint-planning` and follow that skill exactly. Intent is **sprint-planning**, not readiness-only, not status, not validate, not fix.

Skill file: `.agents/skills/bmad-sprint-planning/SKILL.md`

Project root: `/home/oliv/Documents/Programs/Matutine`

User: O. Language: English. Date for the tracking script must be `MM-DD-YYYY HH:MM`.

## What "done" means

1. Resolve customization and config the way the skill requires.
2. Run the readiness gate in `references/readiness-gate.md`.
3. Unless the gate is **FAIL**, generate tracking with `references/generate-tracking.md` and the skill's `sprint_plan.py generate` command.
4. Leave `{implementation_artifacts}/sprint-status.yaml` written and valid.
5. Report the file path, epic count, story count, status breakdown, and the first story a `bmad-build` session should pick up.
6. Run `{workflow.on_complete}` if it is non-empty.

Do not start `bmad-build`. Do not edit stories except to fix a heading the generator cannot parse, and if you do that, say what changed.

## Inputs, in this order

- `_bmad-output/planning-artifacts/epics.md` — **final**. This is the only epic file. 3 epics, 26 stories. Headings are `## Epic N:` and `### Story N.M:`.
- `_bmad-output/planning-artifacts/prds/prd-Matutine-2026-11-07/prd.md` — requirements. Frontmatter still says `draft`.
- `_bmad-output/planning-artifacts/prds/prd-Matutine-2026-11-07/addendum.md` — conflict log. Not requirements.
- `_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md` — **final**.
- `_bmad-output/planning-artifacts/ux-designs/ux-Matutine-2026-11-07/DESIGN.md` and `EXPERIENCE.md` — **final**. Read both.
- `docs/project-brief.md` — initiative intent.
- `_bmad-output/planning-artifacts/decision-freeze.md` — milestone cut. Do not re-open it.

`{planning_artifacts}` is `_bmad-output/planning-artifacts`. `{implementation_artifacts}` is `_bmad-output/implementation-artifacts`.

## Decisions already made

O accepted these. Do not ask again.

- Epic 1 and Epic 2 are Milestone A (Android and Desktop, no server). Epic 3 is Milestone B (LAN server, Docker, Admin, web client).
- UX spines decide what a person sees and does. The architecture spine decides storage, generation, and sync. The decision freeze decides the milestone cut and CalDAV `VEVENT` in Milestone A.
- User edits that must survive regenerate are a rewritten briefing, a rewritten calendar line, and a checked task. Do not invent a merge past that.
- Milestone A tasks are the in-app list only. Todoist and CalDAV `VTODO` are later.
- Same-day extras exist. The front page is today's morning edition only.
- The demo video is not a software story.

## How to treat the gate

The question is whether a developer can implement the epics without inventing decisions nothing records.

- **PASS:** generate tracking immediately.
- **CONCERNS whose only item is `prd.md` still saying `draft`:** treat that as noted, not blocking. O accepted the requirements and the assumptions index on 2026-11-07. Generate tracking. Name the draft PRD as a warning in the final report.
- **Any other CONCERNS:** list them with file paths and stop. Do not generate tracking.
- **FAIL:** follow the readiness gate. Save findings if the skill says to. Do not generate tracking.

## Generate

One epic file:

```bash
uv run .agents/skills/bmad-sprint-planning/scripts/sprint_plan.py generate \
  --epic-file _bmad-output/planning-artifacts/epics.md \
  --status-file _bmad-output/implementation-artifacts/sprint-status.yaml \
  --stories-dir _bmad-output/implementation-artifacts \
  --project "Matutine" \
  --date "<MM-DD-YYYY HH:MM>"
```

Run that from the project root. If the script fails, follow the skill's "If the Script Fails" rule. If it warns that headings were not parsed, fix those headings, say what you changed, and rerun. There is no previous `sprint-status.yaml` to reconcile unless one appears before you start.

## Close

Tell O, in English:

- Gate verdict.
- Path of `sprint-status.yaml`.
- Epic count, story count, status breakdown.
- The first story key.
- The draft-PRD warning, if you kept it.
- Next session: `bmad-build` on that first story, in another fresh context.
