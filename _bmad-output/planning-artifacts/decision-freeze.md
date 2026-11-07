# Matutine decision freeze

Locked 2026-11-05 with O, before the brainstorm. Do not re-open these in the brainstorm. The brainstorm exists to grill the product idea that this freeze deliberately left open.

Source brief: `docs/project-brief.md`. The repo already scaffolds the name `Matutine`, package `hu.toliver.matutine`, Kotlin 2.4.21, Compose Multiplatform 1.12.2, Ktor 3.6.0, and modules `:core`, `:app:shared`, `:app:androidApp`, `:app:desktopApp`, `:app:webApp` (JS and Wasm), `:server`.

## Milestones

**Milestone A — contest MVP.** Android and Desktop. The app works with no server. CalDAV calendars are in this milestone so they can be tested against real Baikal data.

**Milestone B — committed, not optional.** Ktor server, then the Compose web client. Docker image arrives with this milestone.

**Later milestones — in the plan, outside A.** Pull one forward only if Milestone A and B are in good shape and time remains. PDF export, audio edition, Android home-screen widget. More task sources (Todoist, CalDAV `VTODO`). More AI providers (OpenRouter, Claude, OpenAI, xAI, a local model). Multi-user editing and sharing an edition with other people are post-contest.

## Frozen decisions

1. **Platforms.** Milestone A must run on Android and Desktop. Those two are the contest pair. Milestone B adds Ktor, then web. No iOS.
2. **Modules.** `:core` holds models, RSS/Atom parsing, ranking, the edition generator, the AI provider interface, and repository interfaces. No Compose in `:core`. `:app:shared` holds shared Compose UI. `:server` holds the schedule, API, storage, shared token, and the web files. Clients talk only to a repository.
3. **Edition.** One JSON document per local calendar date. Integer revision. Last write wins. Single user. A user edit is part of the document and survives regeneration.
4. **News.** RSS and Atom only. Both are XML syndication formats.
5. **Calendar.** Three sources in Milestone A: the Android on-device calendar, an ICS URL, and CalDAV (a URL plus username and password, reading `VEVENT`). Baikal is the reference server. The client speaks generic CalDAV.
6. **Tasks.** Milestone A stores a local task list behind the repository interface. A later milestone adds Todoist and CalDAV tasks (`VTODO`) through that same interface.
7. **AI.** A provider interface exists in Milestone A. Gemini is the first implementation. With no key, an extractive fallback still produces the page, so the judge demo needs no account. Keys live in config and are never committed. Later providers plug into the same interface.
8. **Web.** Compose for Web, served by the server. About 80–90% of the UI is shared: the newspaper and light editing. The web-only piece is an admin panel that configures the Ktor server: feed URLs, ICS and CalDAV calendars, generation time and timezone, AI provider and key, and the shared access token. Todoist and CalDAV task settings appear with that later milestone. Standalone Android and Desktop keep their own local settings when no server is running.
9. **Reach.** LAN only, shared token, for the contest.
10. **Judge path.** First launch with no config shows a sample edition from bundled fixtures. After that, a person fills in config and gets their own newspaper.
11. **Later product features.** PDF export, an audio edition, and the Android home-screen widget are planned milestones after B. They are not part of A. The local model belongs with the later AI providers.
12. **Packages.** GitHub Actions is the release source. A push of the right branch, or a merged version PR, produces the release. Local builds are for debug and testing only. Milestone A artifacts: Android `.apk` and `.aab`, plus Desktop installers for Windows, Linux, and macOS. Milestone B also produces a Docker image. Native desktop installers are built on the matching GitHub Actions runner. The contest macOS build is unsigned, with no Apple notarization.

## Agent contract

An orchestrator may own a whole phase and hand stages to sub-agents. A phase may choose file layout inside a module and how a pinned behavior is coded. A phase may not add a platform, a data source, an AI provider, or a later-milestone feature, and it may not call itself done if demo mode cannot show an edition.

## Left open for the brainstorm

Grill these until they are strong enough to build on:

- What "importance" means, and how that becomes headline size.
- What the morning briefing is for, in one sentence.
- How the Desktop typewriter editor should feel.
- Which visual cues make the page a newspaper rather than a feed.
