---
id: SPEC-daily-newspaper
companions:
  - ../../planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md
sources:
  - docs/project-brief.md
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability — consult them only if you need narrative rationale or prose color this contract intentionally omits.

# Matutine

## Why

Matutine is a personal morning paper: one front page for the day, from news, a calendar, and tasks, with a short briefing. It is also a Kotlin Multiplatform contest entry for KotlinConf 2027, scored on the idea and on how well the multiplatform split carries it. Judges have to reach a visible edition quickly. The front page and the desktop typewriter editor are the parts that have to feel finished.

## Capabilities

- **CAP-1**
  - **intent:** The user can open today's paper as one front page, with headline size reflecting importance, a short briefing, today's events, and tasks.
  - **success:** On Android and on Desktop, sample data produces that page, headline sizes differ with importance, and the primary view is not a flat list.
- **CAP-2**
  - **intent:** The user can read today's paper on a device with no server, and, once a server is configured, on the phone, the desktop, and in the browser as the paper that server generated.
  - **success:** With no server address, a device shows only editions it generated. With one, the front page matches the server's current morning edition, and earlier local editions are not uploaded.
- **CAP-3**
  - **intent:** The user can replace the morning paper, or add another edition the same day without replacing the morning one.
  - **success:** An automatic later run leaves an edit in place. Regenerate replaces the morning edition and keeps it the front page. A new edition shows up in the list, and the front page stays the morning edition.
- **CAP-4**
  - **intent:** The user can add news feeds and a calendar link. Tasks can come from Todoist, a list kept in Matutine, and a CalDAV to-do collection. On Android the device calendar is included. Each event is shown once on every client of that edition.
  - **success:** Two events with the same start, end, and title appear once. A phone calendar event shows on desktop and in the browser after the next regenerate or added edition. Tasks from each configured source appear on the edition. Email never appears.
- **CAP-5**
  - **intent:** On desktop the user can edit the edition in a typewriter-style editor. In the browser the user can view it and make a light edit. Android does not edit the edition.
  - **success:** A desktop change to story text or the briefing is what the browser shows for that edition. Rank, events, and tasks are unchanged.
- **CAP-6**
  - **intent:** The user gets a local morning notification on Android, and can keep notification time and layout preferences on the device or follow the server copy.
  - **success:** The notification opens the current morning edition. Device-only settings do not change the server copy. Choosing the server copy shows those settings and does not upload the previous device copy.
- **CAP-7**
  - **intent:** The briefing can be an AI summary, and the paper still completes when no key is configured.
  - **success:** Sample mode produces an edition with no AI key. A generation makes at most one summary call, stored on that edition. The MVP default provider is the Gemini free tier, through one interface that can later target another provider. A provider that needs paid credits is not used.
- **CAP-8**
  - **intent:** A judge can reach a visible edition on Android, Desktop, and the server-hosted page without an account or a paid key.
  - **success:** The README's steps go from clone to that edition on Android and Desktop in a few minutes, and the Docker image serves the same morning paper on the local network.
- **CAP-9**
  - **intent:** While the desktop app is open, the user can reach it from the system tray and write a document that is included in the edition.
  - **success:** The tray icon is visible. The document the user writes is kept by the app and appears in the edition. A file chosen from the disk is not imported.

## Constraints

- Kotlin Multiplatform contest entry, built as this product and not as a library or a template. Android and Desktop are the two required platforms. The server does not count as one of them. No iOS target and no claim of iOS support.
- One GitHub project under the MIT license already in `LICENSE`. The README is the demo script for every target and the key features. A short demo video is part of the submission. The hard submission deadline is 23:59 CET on 7 December 2026. The working aim is to finish implementation about five days earlier so one day remains for the video. That earlier date does not cut scope.
- A visible edition must be possible with no account and no AI key, from bundled sample feeds and a sample calendar.
- AI is optional, at most one summary per edition, and the paper completes with no key. The MVP default provider is the Google Gemini free tier. That same interface must accept later providers, including OpenRouter free models, other OpenRouter models, and further providers. Paid credits are out.
- Tasks may come from any of three sources, including more than one at the same time: a Todoist account via a personal API token, a task list kept in Matutine, and a CalDAV to-do collection. Baikal is the CalDAV server the author will test against. Email is out.
- One person, one shared token, server on the local network only. No accounts, no push notifications, no reach from the public internet.
- The Android morning notification is local and may be approximate. A clock-precise morning paper exists only while the server is running.
- Rank, events, and tasks change only when an edition is generated, not when it is edited.
- The web page is Compose Multiplatform for Web, the same 1.12.2 already in the project, built for the browser and served by the server. It does not run the generator. Compose Multiplatform for Web is Beta and uses Kotlin/Wasm.
- Modules, routes, sync, and the stack are in the architecture spine named in `companions:`. An implementation that contradicts an AD there is wrong even where this kernel is silent.

## Non-goals

- iOS.
- Email ingestion.
- Real-time collaboration or merging concurrent edits.
- Push notifications.
- Use from outside the local network.
- Printer hardware and automatic printing.
- For the MVP: print-ready PDF, an audio edition, a home-screen widget, an e-ink layout, a local-model provider, and a `java -jar` server launch. No time is reserved for these. One is built only if the MVP is in good shape while it is being implemented.
- The other product ideas in the brief: read-later triage, receipt scanning, meeting notes, a private journal, and a meal planner.
- Importing an existing file from disk into the edition.
- The 300-word contest essay and the contest submission form. The author writes and submits those.

## Success signal

A judge follows the README from a fresh clone and, within a few minutes, sees a newspaper front page on Android and on Desktop with no account and no AI key, then starts the Docker server and sees that morning paper in the browser. A short demo video shows that path.

## Assumptions

- When no token is set, the server and every remote client use the literal `matutine-demo`.
- An edition edit may change story text and the briefing text, and nothing else on that edition.
- The front page is only today's morning edition. An extra edition from the same day stays in the list.
- Bundled samples are used only when no source list exists. An empty source list stays empty.
- The server's stored papers live in a host directory mounted at `/data`.
