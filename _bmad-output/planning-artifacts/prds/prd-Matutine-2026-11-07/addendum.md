# Addendum: Matutine PRD

Not requirements. This file keeps the mechanism and the conflicts that the PRD refused to smooth over. Sources: `docs/project-brief.md`, `_bmad-output/planning-artifacts/decision-freeze.md`, `_bmad-output/specs/spec-daily-newspaper/SPEC.md`, `architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md`, `ux-designs/ux-Matutine-2026-11-07/EXPERIENCE.md`, `ux-designs/ux-Matutine-2026-11-07/DESIGN.md`.

## Precedence used while writing

O said the brief is the initiative intent and the architecture and UX docs are the shared decisions. Applied in that order when they disagree:

1. UX spines win on what a person sees and does.
2. The architecture spine wins on storage, identity, sync, and who is allowed to generate.
3. The decision freeze wins on milestone cuts and on CalDAV events, where the spine is silent or names a later source on the same port.
4. The brief and the spec inform intent. They do not override the three above.

## Conflict log

| Topic | What the PRD requires | What lost | Why |
|---|---|---|---|
| Editions per day | Morning edition is the front page. Same-day extras exist. No past-day archive. | UX "one document per date." Freeze "one JSON document per local calendar date." | The spine closed this: roles `morning` and `extra`, front page is only today's morning. |
| Edits vs regenerate | User edits, including a calendar line and task completion, remain after explicit regenerate. Automatic morning run only inserts when absent. | Spine: regenerate replaces the body; an edit may change only story text and briefing text. | UJ-4 and UJ-2 state the climax in the final UX spine. Open Question 1 still blocks a merge algorithm. |
| Tasks in Milestone A | In-app list only. | Spine names Todoist, in-app list, and CalDAV `VTODO` together. Brief and spec agree with the spine. | The freeze's milestone cut is explicit. The port should accept the later sources without a redesign. |
| CalDAV events | In Milestone A, with the device calendar and ICS. | Spine describes calendar as ICS plus a device snapshot and uses CalDAV for to-dos. | The freeze lists three calendar sources in A and names Baikal as the reference server. |
| Importance | Topics the person named, stored as rank, drawn as type size. | Freeze left importance undefined. | The UX spine closed it. The spine's "clients do not re-rank" still holds. |
| No-key briefing | Extractive summary plus the sentence "Summary assembled from headlines." | Brief also allowed plain headlines with no extractive sentence. | UX specifies the sentence. The page still completes. |
| Web stack | Not a requirement. | Freeze says the web module is JS and Wasm. Brief and spec say Kotlin/Wasm. | Implementation detail. Compose for Web, served by the server, is the product fact. |

## Mechanism the PRD deliberately omitted

- Package `hu.toliver.matutine`. Modules: `:core` (models, RSS/Atom, ranking, generator, AI port, repository interfaces, no Compose), `:app:shared` (Compose UI), `:app:androidApp`, `:app:desktopApp`, `:app:webApp`, `:server`.
- Clients talk only to a repository. Remote mode is HTTP. The token is `Authorization: Bearer`. One configured base URL. `http` and `https` are the same API with a different scheme.
- Documents are whole-document writes. The store assigns the id on insert. The id never changes. Every write gets a new monotonic revision. Latest whole document wins. No field merge and no rejected stale write. The product rule that some fields are user-owned (Open Question 1) has to be implemented inside that whole-document write, not as a second sync protocol.
- Settings (notification time and layout preferences) are their own document. The sync-versus-local choice stays on the device. Turning sync on does not upload local settings. Turning local on copies the current server settings onto the device, then stops writing the server. Switching settings does not move editions.
- Server token, AI key, and timezone come from the process environment, not from the image. Edition, settings, source list, and calendar snapshot live in the host directory at `/data`.
- The server may seed the source list only when that document is absent.
- Desktop has a reserved local-file capability on the client. No user-facing file import. A file chosen from disk is not imported.
- GitHub Actions produces release artifacts. Native desktop installers are built on the matching runner.

## Rejected product ideas (already in the brief)

Read-later triage, receipt scanning, meeting notes, a private journal, a meal planner, email ingestion, auto-printing. Printing survives only as a possible later PDF export, with no button in this scope.
