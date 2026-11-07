# Epic 1 Context: See today's newspaper

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Sam or O opens Android or Desktop and reads a front page that feels like a morning newspaper. First launch shows a bundled sample edition with no account, API key, or saved source list. Later stories add real feeds, calendar, topics, and generation—all client-local, no server in this epic.

## Stories

- Story 1.1: Open the sample front page
- Story 1.2: Draw the page as a newspaper
- Story 1.3: Reflow the front page at 960px
- Story 1.4: Build the sample from bundled sources
- Story 1.5: Generate today's edition from a feed
- Story 1.6: Name topics and change the lead
- Story 1.7: Show today's events from ICS and the phone calendar
- Story 1.8: Show today's events from CalDAV
- Story 1.9: Keep today's paper on the next morning run
- Story 1.10: Walk a judge from clone to a front page
- Story 1.11: Publish the Android and Desktop release artifacts

## Requirements & Constraints

- The primary view is one newspaper page: masthead, optional sample line, briefing, ranked stories, calendar, then tasks—not a flat feed list.
- With no saved source list, show the bundled sample for today's day key; no account wall or key prompt.
- Section labels are Briefing, Calendar, and Tasks. Activating a story opens its source URL in the platform browser.
- The screen must not indicate whether data is local or from a server.
- Headline rank and layout come from the edition document; clients draw only, they do not re-rank.
- Whole-document JSON stores; repository port is the only data access from UI.
- Package `hu.toliver.matutine`; Kotlin 2.4 / Compose MP 1.12; hexagonal core in `shared`, UI in client apps.
- Sample load failure copy: "The sample edition didn't load." with a primary retry control.

## Technical Decisions

- Core models, generator, parsers, and repository ports live in `shared` with no Compose; `:app:shared` holds Compose edition UI shared by Android and Desktop.
- Clients bind one repository at composition root; only the active generator writes new papers (local mode for Epic 1).
- Edition documents use civil day keys (`YYYY-MM-DD`), role `morning` or `extra`; front page is today's morning only.
- Platform-specific APIs (calendar, notifications, tray, URL opening) stay in client modules via `expect`/`actual` ports.

## UX & Interaction Patterns

- Masthead default name Matutine, edition date, accent rule; sample kicker text `SAMPLE EDITION` when showing bundled sample.
- Briefing, story blocks (lead/secondary/note by stored rank), calendar lines, task lines (read-only checkboxes in early stories).
- One-column reading order on Android and narrow desktop; wide two-column + rail deferred to Story 1.3.
- Newsprint default theme and typography families defined in DESIGN.md (full visual polish in Story 1.2).

## Cross-Story Dependencies

- Story 1.1 lays down edition screen structure and sample fixture; 1.2 adds newspaper typography/theme; 1.4 replaces static fixture with generator-built sample from bundled feeds; later stories add sources, calendar, scheduling, and release CI.
