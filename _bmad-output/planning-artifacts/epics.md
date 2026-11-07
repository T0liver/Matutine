---
status: final
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - docs/project-brief.md
  - _bmad-output/planning-artifacts/decision-freeze.md
  - _bmad-output/specs/spec-daily-newspaper/SPEC.md
  - _bmad-output/planning-artifacts/prds/prd-Matutine-2026-11-07/prd.md
  - _bmad-output/planning-artifacts/prds/prd-Matutine-2026-11-07/addendum.md
  - _bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Matutine-2026-11-07/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Matutine-2026-11-07/EXPERIENCE.md
---

# Matutine - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Matutine, decomposing the requirements from the PRD, UX Design if it exists, and Architecture requirements into implementable stories.

The PRD is `status: final`. O accepted the assumptions index on 2026-11-07: user edits survive explicit regenerate, CalDAV `VEVENT` is in Milestone A, and Milestone A tasks are the in-app list only.

## Requirements Inventory

### Functional Requirements

FR1: O or Sam can open today's morning edition as one newspaper page. Reading order is masthead, sample line if present, briefing, stories in rank order, calendar, then tasks. The primary view is not a flat list. If today's morning edition is missing, the front page is empty. A list does not replace it. The screen does not show whether data is local or from the server. Activating a story opens its source URL. Section labels are Briefing, Calendar, and Tasks. At or above 960px, calendar and tasks may sit in a rail; below 960px, and on Android by default, the page is one column. The rail is not read before the stories.

FR2: The generator ranks stories from the person's topics, and every client draws headline size from that stored rank. Ranks are lead, secondary, and note. There is at most one lead. With topics set, the lead is the story that best fits those topics. With no topics, the edition still generates, rank follows source order, the first story is the lead, and a line opens Interests. Clients do not re-rank. Layout does not change rank. Rank is type size plus source order, not color, and the page does not animate between ranks. No model name, sparkle, or chat box appears on the edition.

FR3: The morning edition includes one briefing block. With a key, it is model text from one summarization stored on that edition. With no key, it is an extractive summary followed by "Summary assembled from headlines." The page is a finished edition in both cases. There is no follow-up box. The briefing is read-only on the Android and Desktop front pages and editable in the typewriter and by light edit.

FR4: The default edition reads as a broadsheet (Newsprint: warm paper, black ink, one dark-red rule under the name, serif text). The sample line is the text SAMPLE EDITION. The edition is flat: no story shadows, no equal-card feed, no logo, no thumbnail, no avatars, no badge counts. Voice is the date, the facts, and the day's list. Empty copy is "No stories yet.", "No events today.", and "No tasks yet." A theme replaces paper, ink, and rule together and keeps the contrast floor.

FR5: When the source list document is absent, the generator builds an edition from bundled sample feeds and sample calendar data and does not write those samples into the list. The sample line is visible. There is no account wall and no key prompt. The judge path does not require Interests. A cold load that already has a saved edition shows that edition immediately and does not flash the sample.

FR6: An empty source list does not use bundled samples. The sample line is hidden on any edition generated from the person's sources.

FR7: The only news inputs are RSS and Atom URLs on the single source list. A feed that does not parse shows "That feed didn't load." The prior list stays. The field keeps the URL. A failed feed is omitted, the failure is noted on the edition, and the edition still completes.

FR8: Milestone A calendar sources are the Android on-device calendar, an ICS URL, and CalDAV reading VEVENT (URL, username, and password). The generator unions them. Two events with the same start, end, and trimmed title appear once. Screens draw edition.events and do not query a calendar themselves. Only Android writes the device snapshot, and an empty or failed read does not overwrite it. Calendar permission off shows "Calendar permission is off." and the rest of the edition stays. A bad ICS URL shows "That calendar link didn't open." A snapshot that arrives after the paper exists appears on the next regenerate or extra. Email is never a source.

FR9: Milestone A stores one local task list behind the repository, and that list is the only task source in A. Checking a task or confirming the add line writes the edition. Completion or addition is still present after a later regeneration. Incomplete tasks remain and appear on the next edition. The generator is the only caller of the task port. The front-page checkbox and add line are the person's writes. A similar title in another source does not drop a task once later sources exist.

FR10: Interests asks "What do you want to read about?" and "What should the page notice about your day?" The first question offers premade choices plus a free line. The second is an open line with a control to add another. Each selected choice and each non-empty line is a topic. Named choices: Video games, Technology, Biology, Finance, plus the assumed set Science, Sports, World news, Culture, Health, and Local. Many topics may be on. With no server, Interests is reachable from Settings and from the first generation of the person's own edition. With a server, the questions live in Admin.

FR11: A morning run inserts today's morning edition only when that document is absent. A later automatic run does not overwrite it. One generation produces one edition even if a feed, calendar, or task call fails. While generating, the masthead and date stay, the body says "Setting today's edition.", and an existing edition stays readable until the new one is ready. Desktop generates only while the app is open. Android generates on demand and on an approximate periodic run. The day key uses the device zone when the device generates.

FR12: Android posts a local notification for the current edition at the time in Settings. The default body is "Today's edition is ready." O can replace that sentence. The default time is 07:00 in the device zone. The title is Matutine. There is no custom layout and no push. Activating it opens today's morning edition. If notification permission is off, nothing is posted and opening the app still shows the edition. Do not post a notification that opens an empty front page until the missing-edition case is decided.

FR13: While the desktop process is running, the system tray can Open, Generate today's edition, and Quit. The menu is the operating system's menu. If the process is closed, no generation runs. The next open shows the last saved edition.

FR14: Regenerate targets the current morning edition, keeps its id, and refreshes generated stories, rank, events, and tasks from sources. User edits that are part of the document remain, including a rewritten briefing, a rewritten calendar line, and a completed or added task. New edition now inserts an extra and leaves every other edition untouched. The front page remains today's morning edition. There is no archive of past days and no revision number on the page. The field-level merge when a source item disappears is Open Question 1 and must not be invented inside a story.

FR15: On Desktop, the typewriter can change any part of the current edition, including calendar lines, tasks, the masthead name, and the printed date. It is one mono column in reading order, with no toolbar and no rank styling. It saves on typing pause and on close. Close returns to the front page with ranks applied. Escape closes the typewriter after a save attempt and nothing else. A failed save says "Couldn't save. Your text is still here." Discard restores the last saved edition. Changing the printed date does not change the day key. Android has no typewriter.

FR16: On the web Edition, O can activate any block, edit it in place, and save by leaving the field. The active string keeps its display type. Light edit uses an underline, not a mono face and not a toolbar. Last write wins. There is no merge screen. A failed save keeps the edited line until save or Discard. Milestone B.

FR17: Android Settings holds local sources, topics, notification time, notification text, theme, layout, an optional key, and an optional server. Desktop Settings holds local sources, topics, an optional key, and an optional server. One row, one setting. The previous saved value stays until a new value succeeds. Newsprint is the default theme and the default column layout. Keys are never committed. At most one summarization call per generation. No chatbot. The MVP provider is the Google Gemini free tier, or no key. A provider that needs paid credits is out.

FR18: Saving a server base URL and token does not upload local editions, the local source list, or the local calendar snapshot. Remote clients do not mint the morning edition. They read, edit, regenerate, or request an extra. If the server cannot be reached, the client shows the last stored edition and "Couldn't reach the server. Showing the copy on this device." Generate stays available and writes locally. Android accepts a cleartext http LAN URL. Notification time, notification text, theme, and layout stay on the Android device when a server is configured. Feeds, calendar, and interest questions move to Admin.

FR19: The server inserts today's morning edition at the configured time when that edition is absent. A clock-precise morning paper exists only while the server is running. The server's configured timezone supplies the day key. The server holds the AI key. Clients in remote mode do not. The morning run does not replace an existing morning edition. Milestone B.

FR20: A person on the LAN can open the web client and the API with one shared token. No public ingress, no accounts, no push. The token is required before the newspaper or Admin renders. A mismatch says "That token doesn't match." and keeps the typed value. If the token is unset, server and remote clients use the literal matutine-demo. The same token covers the newspaper and Admin. The required server launch is the Docker image. Papers and settings live in a host directory mounted at /data. Milestone B.

FR21: Admin can set feed URLs, an ICS URL, CalDAV URL with username and password, generation time, timezone, the interest questions, the AI provider and key, and the shared token. Web has no Settings screen. The web client does not run the generator and is not an offline app. Admin is the web-only piece. Milestone B.

FR22: From a fresh clone, Sam reaches a newspaper front page on Android and on Desktop in a few minutes, with no account and no AI key. Milestone B adds the Docker server and that morning paper in the browser. A short demo video shows that path and is part of the submission, not the app. Milestone A release artifacts from GitHub Actions are an Android apk and aab and Desktop installers for Windows, Linux, and macOS. The macOS build is unsigned and not notarized. Milestone B also produces the Docker image. A phase is not done if demo mode cannot show an edition.

### NonFunctional Requirements

NFR1: Text and controls meet WCAG 2.2 AA on Android, Desktop, and web, including every alternate theme. English only. Assumption, from the UX spine.

NFR2: Android touch targets on the edition and in Settings are at least 48dp. Desktop and web chrome controls are at least 40px. Focus order follows reading order on the edition and field order on chrome screens.

NFR3: No information is carried by motion. Celebratory motion is banned. The page does not animate between ranks.

NFR4: One generation still completes when a feed, calendar, or task call fails, or when no AI key exists. The failed source is omitted and noted on the edition.

NFR5: The morning job writes at most once per generator-day. An empty or failed Android calendar read does not destroy the stored snapshot.

NFR6: Single user. AI keys, the Todoist token, and CalDAV passwords are configuration and are never committed. In remote mode the server holds the AI key. The token is sent as Authorization Bearer. No cloud account.

NFR7: Contest server reach is the LAN. One configured base URL. http and https are the same API with a different scheme. Android must allow cleartext when the scheme is http.

NFR8: At most one summarization call per edition. MVP AI is free-tier or no key. No paid credits.

NFR9: Android does not promise a clock time. Desktop generates only while the process is open. Only the server promises creation at a configured time, and only if today's morning edition is absent.

NFR10: Edition, settings, source list, and calendar snapshot are each one JSON document. The store assigns the id on insert. The id never changes. Every write gets a new monotonic revision. The latest whole document wins. Server copies of those documents live in the host directory at /data, not in the image. Token, AI key, and timezone come from the process environment.

NFR11: A phase may not add a platform, a data source, an AI provider, or a later-milestone feature. Demo mode must be able to show an edition.

NFR12: Three embedded open-license type families (Newsreader, Source Serif 4, IBM Plex Mono) so Android, Desktop, and web match. Assumption from the UX spine.

### Additional Requirements

- The repository already exists. There is no greenfield starter template. Epic 1 Story 1 is not "initialize from a template." Package `hu.toliver.matutine`. Kotlin 2.4.21, Compose Multiplatform 1.12.2, Ktor 3.6.1, AGP 9.3.3, Gradle 9.5.0, JDK 17, kotlinx-serialization 1.12.0. Kotlin JVM target 17 for shared, server, and desktopApp.
- Structural seed: `shared/` ports, models, generator, parsers; `server/` scheduler, HTTP API, stores, Docker, served web files; `androidApp/` UI, WorkManager, device calendar, notifications; `desktopApp/` UI, typewriter, tray; `webApp/` Compose for Web, HTTP only. The freeze names `:core` with no Compose and `:app:shared` for Compose UI. Reconcile that split inside the existing modules. Do not add a module as a product feature.
- AD-1: Models, RSS/Atom parsing, ranking, the edition generator, the AI provider interface, and repository interfaces stay shared. No Compose in that core.
- AD-2: Clients talk only to a repository. Only the active generator writes a new paper. With a server base URL, clients do not mint the morning edition.
- AD-3: Many editions. Roles `morning` and `extra`. The front page is only today's morning. A schedule tick inserts today's morning only when absent. Regenerate is the only replace and keeps the id. New edition now inserts an extra.
- AD-4: Whole-document last-write-wins. No field merge and no rejected stale write. The product rule that user-owned text survives regenerate has to be implemented inside that write. Do not add a second sync protocol.
- AD-5: Only the generator sets story rank and the layout model. Screens draw them.
- AD-6: One optional AI pass per generation, stored on that edition, which is the only cache. No chatbot.
- AD-7: Android and Desktop are the guaranteed products. Web is a server-hosted page, not a contest platform, and not an offline app. No iOS target.
- AD-8: Settings are a separate document. The sync-versus-local choice stays on the device and is not part of the document. Turning sync on does not upload local settings. Turning local on copies the current server settings onto the device, then stops writing the server. Switching settings does not move editions.
- AD-9: The generator unions ICS with the Android calendar snapshot. Identity is start, end, and trimmed title. Which calendar they came from is metadata.
- AD-10: One task port. No email. The generator is the only caller. Milestone A ships the in-app list. The port must be able to gain Todoist and CalDAV VTODO later without a redesign.
- AD-11: Clocks differ on purpose. Server: configured time, insert-if-absent. Android: WorkManager, approximate, local notification. Desktop: only while open, no morning notification.
- AD-12: Platform APIs (device calendar, notifications, tray, cleartext) stay in the client.
- AD-13: LAN, one shared token, one base URL. Unset token is the literal `matutine-demo`.
- AD-14: Docker is the server a person runs. Host directory mounted at `/data`. `java -jar` is deferred and, if added, must be the same process, config, and store.
- AD-15: A judge can see a paper with no accounts and no AI key, on a client and on the server image.
- AD-16: One JSON codec. Routes live in the spine. Stories that add an endpoint use that codec and those routes.
- AD-18: One source-list document for RSS/Atom and ICS URLs. The generator reads that list and no other. The server may seed it only when the document is absent.
- GitHub Actions is the release source. Milestone A artifacts: Android apk and aab, Desktop installers for Windows, Linux, and macOS, native installers built on the matching runner, macOS unsigned. Milestone B adds the Docker image. Local builds are for debug and testing.
- Deferred, and stories must not build them: server timezone identifier format beyond "Admin sets it and the process receives it"; visual details inside layout preferences; `java -jar`; PDF, audio, widget, e-ink.

### UX Design Requirements

UX-DR1: Implement the Masthead. Default name Matutine, then the edition date. Paper ground, name, a 2px accent rule the width of the name, date in meta ink-secondary, then a 1px ink rule across the page. No logo. Typewriter and light edit can change the name and the printed date. Editing the date line does not refile the edition. Notification title and other chrome stay Matutine.

UX-DR2: Implement the Sample line. Kicker in accent, text SAMPLE EDITION, directly under the masthead rule. Visible only while the current edition is the bundled sample. Hidden on any edition generated from the person's sources. The line is text, not color alone.

UX-DR3: Implement the Briefing block. Section label in kicker, then briefing type in ink. One block on the default layout. Extractive fallback adds a meta line: "Summary assembled from headlines." No model name and no sparkle icon. Read-only on Android and Desktop front pages. Editable in the typewriter and by light edit.

UX-DR4: Implement the Story block. Headline in the rank's type (lead, secondary, note), then source in meta ink-secondary, then a deck in body. Hairline between stories. The lead has no hairline above it. No thumbnail. Activating opens the source URL. The accessible name exposes the source name and the headline. Story text is editable in the typewriter and by light edit. Android reads it.

UX-DR5: Implement the Calendar block. Label "Calendar". Events in time order, title and start time, hairline between events. On the front page it is a view. In the typewriter and by light edit, the lines on this edition can be changed. Events are not stories. Empty copy: "No events today." Android permission off: "Calendar permission is off." with a row that opens the system permission screen. The rest of the edition stays.

UX-DR6: Implement the Task block. Label "Tasks". Each task is body type with a 16px square checkbox stroked in ink. A completed task stays in ink with a single strike. The add line is body type on paper. Check completes. Activate the add line, type, confirm to add. Empty copy: "No tasks yet." and the add line remains. Completing or adding writes the repository.

UX-DR7: Implement the Desktop typewriter. It replaces the front page in the same window. One mono column of the whole edition in reading order, including labels, on the current theme's paper, borderless, no toolbar, no rank styling. The person can change, add, and reorder. Saves when typing pauses and again on close. Close returns to the front page with ranks applied. The typewriter is one text region named "Edition". Escape closes it after a save attempt and closes nothing else. The keyboard belongs to the typewriter. The front page is pointer.

UX-DR8: Implement web light edit. Activate any block and edit that text in place. The active string keeps its display type. A 1px ink underline marks it. No toolbar and no mono face. Leaving the field saves. Last write wins. No merge screen and no revision number on the page.

UX-DR9: Implement Interests. Chrome screen. First question: choice chips plus a free line. Second question: an open line and a control to add another. Selected chips and non-empty lines are topics. Primary button saves. Starter choices are Video games, Technology, Biology, Finance, Science, Sports, World news, Culture, Health, and Local. Many can be on. The judge demo does not open this screen. With no topics, a line on the edition opens Interests and rank falls back to source order.

UX-DR10: Implement Choice. Chrome only, on Interests. Resting: field fill, ink label, 1px ink border. Selected: ink fill, paper label. Height 40px on desktop and web, 48px on Android. The default edition does not use choices.

UX-DR11: Implement Button and Text field for chrome. One primary button per chrome screen (generate, save a settings form, or submit the token). Secondary cancels back to the edition, except Discard, which throws away unsaved edition text and returns the last saved edition. The word is Discard, not Cancel. Text field: label above, error sentence below in the error color. The previous saved value stays until a new value succeeds. Focus: 2px ink outline, 2px offset. Heights: 40px desktop and web, 48px Android. Material elevation is allowed only on a dialog that confirms leaving an unsaved chrome form, nowhere on the edition or the typewriter.

UX-DR12: Implement the Settings row. One row, one setting, on chrome. Activating a row opens its field on the same screen. Android rows are at least 48px tall. Rows separate with a hairline.

UX-DR13: Implement the Token gate. Chrome screen, one text field, one primary button. No masthead and no edition preview behind a rejected token. Copy: "That token doesn't match." The field keeps the typed value.

UX-DR14: Implement the tray as the operating system's menu with exactly three items: Open, Generate today's edition, Quit. No custom panel and no paper styling.

UX-DR15: Implement the Android notification as the system notification. No custom layout. Title Matutine. Body is the Settings sentence, default "Today's edition is ready." The accessible name is the current body. Activating opens the Edition. No push.

UX-DR16: Implement the state copy and treatments exactly: "The sample edition didn't load." with a retry; "Setting today's edition." while generating, existing edition stays readable underneath; "Couldn't save. Your text is still here."; "Couldn't reach the server. Showing the copy on this device."; "That feed didn't load."; "That calendar link didn't open."; "No stories yet." with calendar and tasks still shown. Save in flight shows no modal. The last confirmed line remains "Saved." until the next success.

UX-DR17: Implement the responsive rule. Wide layout at or above 960px: masthead and date span the page; briefing spans the story columns, not the rail; stories in two columns; calendar and tasks stack in a right rail; a rule splits the rail from the stories. Below 960px, and the Android default, one column in reading order. No separate tablet layout. Wide layout does not change assistive-tech reading order.

UX-DR18: Implement theme and layout as Android settings. Newsprint is the default. A theme replaces paper, ink, ink-secondary, rule, and accent together and keeps the contrast floor. A layout rearranges the same edition and does not change rank. Other themes may be names before palettes exist. Mono is the typewriter only.

UX-DR19: Enforce the banned set. No chat transcript, email connect, print button, archive, second modal, celebratory motion, badge counts, hover-only control, equal-card feed, story shadow, logo, thumbnail, avatar, gradient, or model badge on the default edition. Rule color is never text. Paper corners are the window's corners.

UX-DR20: Embed Newsreader, Source Serif 4, and IBM Plex Mono so the three clients match. Do not copy the type ramp, spacing scale, or hex table into stories as behavior. Those values stay in DESIGN.md and are the visual acceptance reference for UX-DR1 through UX-DR18.

### FR Coverage Map

FR1: Epic 1 — Open today's front page
FR2: Epic 1 — Rank stories from topics and draw headline size
FR3: Epic 1 — Briefing, including the no-key extractive fallback
FR4: Epic 1 — Newspaper presentation and empty copy
FR5: Epic 1 — Sample edition on first launch
FR6: Epic 1 — Empty source list stays empty
FR7: Epic 1 — RSS and Atom feeds
FR8: Epic 1 — Device calendar, ICS, and CalDAV events
FR9: Epic 2 — Local tasks, check, and add
FR10: Epic 1 — Interests and topics
FR11: Epic 1 — Morning run inserts today's edition only when absent
FR12: Epic 2 — Android local notification
FR13: Epic 2 — Desktop tray
FR14: Epic 2 — Regenerate keeps user edits; same-day extra
FR15: Epic 2 — Desktop typewriter
FR16: Epic 3 — Web light edit
FR17: Epic 2 — Device settings, theme, layout, key
FR18: Epic 3 — Point a client at the server without uploading the old paper
FR19: Epic 3 — Server generates on a clock
FR20: Epic 3 — LAN token and Docker
FR21: Epic 3 — Admin
FR22: Epic 1 (Android and Desktop judge path) and Epic 3 (Docker and browser)

## Epic List

### Epic 1: See today's newspaper
Sam or O opens Android or Desktop and reads a front page. First launch is the sample edition, with no account and no key. After that, O adds feeds, a calendar, and topics, and generates their own edition. No server.
**FRs covered:** FR1, FR2, FR3, FR4, FR5, FR6, FR7, FR8, FR10, FR11, FR22 (Android and Desktop)

### Epic 2: Keep and change today's paper
O checks and adds tasks, gets the Android morning notification, sets the phone's theme, layout, and notification text, and rewrites the page on the desktop typewriter. Those edits survive regenerate. A same-day extra is not the front page.
**FRs covered:** FR9, FR12, FR13, FR14, FR15, FR17

### Epic 3: Read the server's morning edition
O runs the Docker server on the LAN, configures it in Admin, and reads and lightly edits that same edition in the browser and on the phone. Local papers are not uploaded when the server is chosen.
**FRs covered:** FR16, FR18, FR19, FR20, FR21, FR22 (Docker and browser)

## Epic 1: See today's newspaper

Sam or O opens Android or Desktop and reads a front page. First launch is the sample edition, with no account and no key. After that, O adds feeds, a calendar, and topics, and generates their own edition. No server.

Stories are ordered. A story may use only earlier stories in this epic, plus the existing Gradle modules. Each story is one coding-agent task. Create only the edition fields and settings rows that story displays.

### Story 1.1: Open the sample front page

As Sam,
I want the first launch to show a newspaper of the day,
So that I can judge the entry without an account or a key.

**Acceptance Criteria:**

**Given** no source list has been saved and no account or key exists
**When** Sam opens the Desktop app or the Android app
**Then** the Edition shows a bundled sample edition for today's day key
**And** the page shows the text `SAMPLE EDITION`, a masthead whose default name is Matutine, a date line, one briefing, at least one lead and one smaller story, calendar lines, and task lines
**And** there is no account wall and no key prompt
**And** section labels read "Briefing", "Calendar", and "Tasks"
**And** the page is one column in reading order: masthead, sample line, briefing, stories, calendar, tasks
**And** activating a story opens its source URL in the platform browser
**And** the Edition does not say whether the paper was generated on this device or on a server
**And** if the sample cannot be loaded, the page says "The sample edition didn't load." and a primary button tries again

**Agent notes:**

- Covers: FR1, FR5 (display half), UX-DR1, UX-DR2, UX-DR4 (open URL). Starts UX-DR3 and UX-DR5 and UX-DR6 as read-only fixture blocks.
- May touch: `:app:shared` Edition screen, `:app:androidApp`, `:app:desktopApp`, a fixture edition document.
- Do not: add a server, a feed form, Interests, the typewriter, or a second page. Do not query a live calendar. Tasks are not checkable yet.

### Story 1.2: Draw the page as a newspaper

As Sam,
I want headline size, type, and ink to read as a broadsheet,
So that the page is a newspaper rather than a list of equal cards.

**Acceptance Criteria:**

**Given** the sample edition from Story 1.1
**When** the Edition is shown
**Then** the lead headline is larger than a secondary story, and a note is smaller than a secondary story
**And** rank is type size plus source order, not color, and the page does not animate between ranks
**And** Newsreader, Source Serif 4, and IBM Plex Mono are embedded and used for their roles in `DESIGN.md`
**And** the default theme is Newsprint: warm paper, black ink, one dark-red rule under the name, serif text
**And** stories have no thumbnail, shadow, avatar, or equal-card chrome, and there is no logo, chat box, sparkle, model name, badge count, or gradient
**And** a story link's accessible name includes the source name and the headline
**And** an edition with no stories says "No stories yet." and still shows calendar and tasks
**And** an edition with no events says "No events today."
**And** an edition with no tasks says "No tasks yet."
**And** rule color is not used as text

**Agent notes:**

- Covers: FR2 (draw stored rank), FR4, NFR3, NFR12, UX-DR19, UX-DR20, UX-DR4 visual.
- May touch: `:app:shared` theme and edition layout.
- Do not: copy the hex table or type ramp into new documents. `DESIGN.md` is the visual reference. Do not add a theme picker yet.

### Story 1.3: Reflow the front page at 960px

As O,
I want a wide window to put the day's events and tasks beside the stories,
So that the desktop page has a newspaper rail without changing what I read first.

**Acceptance Criteria:**

**Given** an edition is open on Desktop
**When** the window is at least 960px wide
**Then** the masthead and date span the page, stories sit in two columns, and calendar and tasks stack in a right rail separated by a rule
**And** the briefing spans the story columns and not the rail
**When** the window is below 960px, or the client is Android
**Then** the edition is one column in reading order
**And** assistive-tech reading order stays masthead, sample line, briefing, stories, calendar, tasks, including when the rail is visible
**And** there is no separate tablet layout
**And** Android touch targets on the edition are at least 48dp

**Agent notes:**

- Covers: FR1 (layout), NFR2 (edition targets), UX-DR17.
- May touch: `:app:shared` edition layout.
- Do not: change rank when the layout changes. Do not add an archive or swipe between days.

### Story 1.4: Build the sample from bundled sources

As Sam,
I want the sample paper to come from bundled feeds and a bundled calendar,
So that demo mode is a generated edition and not a picture of one.

**Acceptance Criteria:**

**Given** the source-list document is absent
**When** the app generates the edition
**Then** it uses bundled sample feeds and bundled sample calendar data
**And** it does not write those samples into a source list
**And** the sample line is visible
**Given** a saved edition already exists
**When** the app cold-starts
**Then** that edition appears immediately and the sample does not flash
**Given** the source list exists and is empty
**When** the app generates
**Then** it does not fall back to bundled samples

**Agent notes:**

- Covers: FR5, FR6 (empty-list rule), AD-15, AD-18 (absent versus empty).
- May touch: `:core` generator and fixture loader, repository interface used by the two apps.
- Do not: add RSS URL settings yet. Do not call a network calendar. Do not put Compose in `:core`.

### Story 1.5: Generate today's edition from a feed

As O,
I want to replace the sample with a paper from my own RSS or Atom feed,
So that the lead and the briefing are about my sources.

**Acceptance Criteria:**

**Given** the sample edition is on screen
**When** O saves one RSS or Atom URL and activates "Generate today's edition"
**Then** the masthead and date stay, the body says "Setting today's edition.", and the sample stays readable until generation finishes
**And** the finished edition hides `SAMPLE EDITION` and is today's morning edition for the device zone
**And** the only news inputs accepted are RSS and Atom
**Given** no AI key is configured
**When** generation finishes
**Then** the briefing is an extractive summary followed by "Summary assembled from headlines."
**And** the page is a finished edition
**Given** the feed does not parse
**When** generation runs
**Then** the page says "That feed didn't load.", the field keeps the URL, the prior feed list stays, and the sample stays
**And** a failed feed is omitted, the failure is noted on the edition, and the edition still completes when other inputs exist
**And** Settings uses one row per setting, a previous value stays until the new value succeeds, and there is one primary button for generate
**And** Desktop and web chrome controls added here are at least 40px; Android rows are at least 48dp

**Agent notes:**

- Covers: FR3 (no-key), FR6 (sample line hides), FR7, FR11 (on-demand generate and the generating state), UX-DR11, UX-DR12, UX-DR16 (generating, bad feed).
- May touch: `:core` RSS/Atom parser and generator, `:app:shared` Settings, both clients.
- Do not: add a model call, CalDAV, the server, or a second source list. One summarization is out of scope until Story 2.7.

### Story 1.6: Name topics and change the lead

As O,
I want the largest headline to be the story that fits what I said I care about,
So that the paper is not using a hidden ranking.

**Acceptance Criteria:**

**Given** O opens Interests from Settings
**When** the screen is shown
**Then** it asks "What do you want to read about?" and "What should the page notice about your day?"
**And** the first question has choices for Video games, Technology, Biology, Finance, Science, Sports, World news, Culture, Health, and Local, plus a free line
**And** the second question is an open line with a control to add another
**And** many choices can be on, and a selected choice uses ink fill and a paper label
**When** O saves Technology and Video games and generates today's edition
**Then** each selected choice and each non-empty line is a topic
**And** the lead is the story that best fits those topics, then secondary, then note, with at most one lead
**And** the generator stores the rank and the client does not re-rank
**Given** no topics are saved
**When** an edition is generated from the person's sources
**Then** the edition still generates, rank follows source order, the first story is the lead, and a line on the page opens Interests with "What do you want to read about?"
**And** the judge's first launch still does not force Interests

**Agent notes:**

- Covers: FR2, FR10, UX-DR9, UX-DR10.
- May touch: `:core` ranking, Interests UI in `:app:shared`, Settings entry on both clients.
- Do not: add a second hidden score. Do not move Interests to Admin. That happens in Epic 3.

### Story 1.7: Show today's events from ICS and the phone calendar

As O,
I want today's events on the front page,
So that the paper includes the day, not only the news.

**Acceptance Criteria:**

**Given** Desktop has an ICS URL
**When** today's edition is generated
**Then** events from that calendar appear in time order with title and start time
**And** the screen draws the edition's event list and does not query a calendar itself
**Given** two events share the same start, end, and trimmed title
**When** the edition is generated
**Then** they appear once
**Given** the ICS URL does not open
**When** O saves it
**Then** the page says "That calendar link didn't open." and the prior calendar source stays
**Given** Android calendar permission is off
**When** the edition is shown
**Then** the calendar block says "Calendar permission is off." and offers a row that opens the system permission screen
**And** stories, briefing, and tasks still show
**Given** Android has a device calendar and permission is on
**When** generation runs
**Then** today's device events are included
**And** an empty or failed device read does not overwrite a previously stored snapshot
**And** a snapshot written after the paper exists does not change that paper

**Agent notes:**

- Covers: FR8 except CalDAV, AD-9, AD-12, UX-DR5 (front-page view), UX-DR16 (bad calendar link, permission).
- May touch: `:core` calendar union, `:app:androidApp` calendar provider, Settings rows on both clients.
- Do not: add email. Do not let Desktop or a future web client write the device snapshot. CalDAV is Story 1.8.

### Story 1.8: Show today's events from CalDAV

As O,
I want events from my CalDAV calendar on the paper,
So that a Baikal calendar is a first-class source in Milestone A.

**Acceptance Criteria:**

**Given** O saves a CalDAV URL, username, and password that can read `VEVENT`
**When** today's edition is generated
**Then** those events join the ICS and device events under the same dedupe rule (same start, end, and trimmed title appear once)
**And** which calendar an event came from is not part of that identity
**And** the password is stored as local configuration and is not committed
**Given** the CalDAV call fails
**When** generation runs
**Then** that source is omitted, the failure is noted on the edition, and the edition still completes
**And** Baikal is not required for the app to start

**Agent notes:**

- Covers: FR8 (CalDAV `VEVENT`), NFR6 (credential not committed).
- May touch: `:core` CalDAV event reader, Settings fields on Android and Desktop.
- Do not: read `VTODO`. Do not add Todoist. Do not send the password to a server. There is no server in this epic.

### Story 1.9: Keep today's paper on the next morning run

As O,
I want a later automatic run to leave today's paper alone,
So that the morning job does not wipe an edition I already have.

**Acceptance Criteria:**

**Given** today's morning edition is absent
**When** a morning run executes on a device that has no server configured
**Then** it inserts today's morning edition using the device timezone for the day key
**Given** today's morning edition already exists
**When** another automatic run fires
**Then** it does not write
**And** Desktop generation runs only while the app is open, using the Generate action from Story 1.5
**And** Android can generate on demand, and a periodic local run may be late
**And** the day key is `YYYY-MM-DD` assigned by the generator
**And** if today's morning edition is missing, the front page is empty and a list is not shown in its place

**Agent notes:**

- Covers: FR11, NFR5, NFR9 (device clocks), AD-3 (insert-if-absent), AD-11.
- May touch: `:core` repository insert-if-absent, `:app:androidApp` periodic work, `:app:desktopApp` open-app generation.
- Do not: promise a clock time on Android or Desktop. Do not add the system tray menu. That is Story 2.4. Do not replace an existing morning edition. Regenerate is Story 2.6.

### Story 1.10: Walk a judge from clone to a front page

As Sam,
I want the README to get me to a visible edition on Android and on Desktop,
So that I can see the newspaper in a few minutes with no account and no key.

**Acceptance Criteria:**

**Given** a fresh clone and the README
**When** Sam follows the Android steps and the Desktop steps
**Then** each client shows a front page in a few minutes
**And** no step asks for an account, an AI key, or Interests
**And** the steps do not claim iOS
**And** a phase that cannot show this sample edition is not done

**Agent notes:**

- Covers: FR22 (Android and Desktop), NFR11, AD-7, AD-15.
- May touch: `README.md` only.
- Do not: document the Docker server or the browser path. That is Story 3.7. Do not add a hosted public demo.

### Story 1.11: Publish the Android and Desktop release artifacts

As a contest entrant,
I want GitHub Actions to produce the installable clients,
So that a judge is not asked to assemble a local release build.

**Acceptance Criteria:**

**Given** a push of the release branch, or a merged version PR
**When** the release workflow finishes
**Then** it produces an Android `.apk`, an Android `.aab`, and Desktop installers for Windows, Linux, and macOS
**And** each desktop installer is built on the matching runner
**And** the macOS artifact is unsigned and not notarized
**And** local builds remain available for debug and testing
**And** the workflow does not publish a Docker image

**Agent notes:**

- Covers: FR22 (Milestone A artifacts).
- May touch: `.github/workflows`.
- Do not: add iOS. Do not add the Docker image. That is Story 3.8.

## Epic 2: Keep and change today's paper

O checks and adds tasks, gets the Android morning notification, sets the phone's theme, layout, and notification text, and rewrites the page on the desktop typewriter. Those edits survive regenerate. A same-day extra is not the front page.

Depends on Epic 1. Stories inside this epic depend only on earlier stories in this epic.

### Story 2.1: Check and add a task on the front page

As O,
I want to complete a task and add another on today's paper,
So that the task list is part of the edition, not a separate app.

**Acceptance Criteria:**

**Given** today's edition is open on Android or Desktop
**When** O checks a task
**Then** the task stays in ink with a single strike and the edition is saved
**When** O activates the add line, types a task, and confirms
**Then** the task appears on the edition
**Given** no tasks exist
**When** the block is shown
**Then** it says "No tasks yet." and the add line remains
**Given** a task was checked or added
**When** O closes and reopens the app
**Then** that completion or addition is still on the edition
**And** an incomplete task remains on the list
**And** Milestone A has no task source other than this in-app list
**And** the checkbox and add line write the edition; they do not call a second task service
**And** while a save is in flight there is no modal, and the last confirmed line remains "Saved." until the next success

**Agent notes:**

- Covers: FR9 (write and reload), UX-DR6, UX-DR16 (Saved. on the task block). Survival across regenerate is tested in Story 2.6.
- May touch: `:core` task list on the edition, `:app:shared` task block.
- Do not: add Todoist or CalDAV `VTODO`. Do not dedupe tasks by title. Do not implement regenerate here.

### Story 2.2: Open the paper from a local notification

As O,
I want a local notification at the time I set,
So that the morning paper opens without a push service.

**Acceptance Criteria:**

**Given** a notification time is in Android Settings and today's morning edition exists
**When** that time is reached
**Then** Android posts a system notification whose title is Matutine and whose body is "Today's edition is ready."
**And** the accessible name is that body
**And** there is no custom notification layout and no push
**When** O activates the notification
**Then** today's morning edition opens
**Given** notification permission is off
**When** the time is reached
**Then** nothing is posted, Settings still shows the time, and opening the app still shows the edition
**Given** today's morning edition is missing
**When** the tick runs
**Then** no notification is posted that would open an empty front page
**And** the default time, when none was saved, is 07:00 in the device zone

**Agent notes:**

- Covers: FR12, UX-DR15, NFR9 (Android clock).
- May touch: `:app:androidApp` notification scheduling, the notification-time settings row.
- Do not: add push, a custom notification layout, or a Desktop notification.

### Story 2.3: Customize the Android paper

As O,
I want to change the notification sentence, the theme, and the layout,
So that the phone paper matches the morning I actually have.

**Acceptance Criteria:**

**Given** Android Settings
**When** O replaces the notification body, picks a theme, and picks a layout
**Then** the next notification uses that sentence and that time
**And** the edition it opens uses that theme and that layout
**And** Newsprint stays the default theme and the default column layout
**And** a theme replaces paper, ink, ink-secondary, rule, and accent together and keeps WCAG 2.2 AA contrast for text
**And** a layout rearranges the same edition and does not change rank
**And** Desktop Settings does not gain theme or layout
**And** English remains the only language

**Agent notes:**

- Covers: FR17 (theme, layout, notification text), NFR1, UX-DR18.
- May touch: `:app:androidApp` settings, shared theme tokens.
- Do not: require a finished palette for every named theme. A name may ship before a second palette. Do not put these controls on Desktop.

### Story 2.4: Use the desktop tray

As O,
I want the running desktop app in the system tray,
So that I can open the paper, generate, or quit without hunting for the window.

**Acceptance Criteria:**

**Given** the Desktop process is running
**When** O opens the tray menu
**Then** it is the operating system's menu with exactly Open, Generate today's edition, and Quit
**And** Open shows today's edition
**And** Generate today's edition runs the same generate action as Settings
**And** Quit leaves the process
**Given** the process is closed
**When** the clock reaches morning
**Then** no generation runs
**And** the next open shows the last saved edition

**Agent notes:**

- Covers: FR13, UX-DR14.
- May touch: `:app:desktopApp` tray.
- Do not: draw a custom paper-styled menu. Do not add a morning notification on Desktop.

### Story 2.5: Rewrite the edition on the typewriter

As O,
I want to edit the whole morning page in one column,
So that I can change the briefing and a calendar line without a form.

**Acceptance Criteria:**

**Given** today's edition is open on Desktop
**When** O opens the typewriter
**Then** it replaces the front page in the same window
**And** it is one mono text region named "Edition", containing the masthead, briefing, stories, calendar, tasks, and labels, in reading order
**And** there is no toolbar and no rank styling
**And** O can change any part, add a line, and reorder blocks
**When** O pauses typing, or closes the typewriter
**Then** the edition saves
**And** the front page shows the changes with ranks applied again
**And** editing the printed date does not change the day key
**When** save fails
**Then** the page says "Couldn't save. Your text is still here."
**And** Escape attempts a save and closes the typewriter and nothing else
**And** Discard restores the last saved edition
**And** the secondary action that throws away edition text is labeled Discard, not Cancel
**And** while a save is in flight there is no modal, and the last confirmed line remains "Saved." until the next success
**And** Android has no typewriter

**Agent notes:**

- Covers: FR15, UX-DR7, UX-DR11 (Discard), UX-DR16 (save failed and Saved.).
- May touch: `:app:desktopApp` typewriter, edition edit on the repository.
- Do not: build web light edit. Do not change rank by hand. Do not import a file from disk.

### Story 2.6: Regenerate without losing the edits

As O,
I want generate-again to refresh the news and still keep what I changed,
So that a new lead does not erase the briefing, the calendar line, or a checked task.

**Acceptance Criteria:**

**Given** O has rewritten the briefing and a calendar line on the typewriter, and has checked a task
**When** O generates today's morning edition again
**Then** the same edition id is kept
**And** the rewritten briefing is still there
**And** the rewritten calendar line is still there
**And** the checked task is still checked
**And** stories, rank, and unedited events and tasks may refresh from sources
**Given** O asks for a new edition now
**When** the command finishes
**Then** a same-day extra exists, every other edition is untouched, and the front page is still today's morning edition
**And** the extra opens from a same-day list
**And** there is no archive of past days and no revision number on the page
**And** there is no merge screen
**And** this story does not define what happens when the source item behind an edit has disappeared, beyond the three keeps above
**Given** a device-calendar snapshot is saved after today's morning edition already exists
**When** O regenerates that morning edition, or asks for a new edition now
**Then** the events from that snapshot are on the regenerated edition or the extra
**And** they were not written into the paper that already existed before that command

**Agent notes:**

- Covers: FR14, FR8 (a late snapshot appears on the next regenerate or extra), AD-3 (regenerate keeps id; extra is a new document). The product rule overrides a spine diagram that says regenerate replaces the whole body.
- May touch: `:core` regenerate and extra insert, a same-day list on the clients.
- Do not: invent a general field-merge algorithm. Do not add conflict UI. Do not upload editions to a server.

### Story 2.7: Write the briefing with a Gemini key

As O,
I want a model briefing when I have supplied a free-tier key,
So that the opening is written, and the paper still exists when I have not.

**Acceptance Criteria:**

**Given** a Gemini API key is in local config and is not in the git tree
**When** an edition is generated
**Then** exactly one summarization call is stored on that edition
**And** the briefing is model text and does not show a model name or a sparkle
**Given** the key is missing or the call fails
**When** generation finishes
**Then** the briefing stays the extractive summary plus "Summary assembled from headlines."
**And** the edition still completes
**And** a provider that needs paid credits cannot be selected
**And** there is no chat box and no follow-up field

**Agent notes:**

- Covers: FR3 (with key), FR17 (key), NFR4, NFR8, AD-6.
- May touch: `:core` AI provider port and the Gemini adapter, a key field in Settings.
- Do not: add OpenRouter, Claude, OpenAI, xAI, or a local model. Do not put the key in the repository. Do not send the key to a server. There is no server in this epic.

## Epic 3: Read the server's morning edition

O runs the Docker server on the LAN, configures it in Admin, and reads and lightly edits that same edition in the browser and on the phone. Local papers are not uploaded when the server is chosen.

Depends on Epic 1 and Epic 2. The edit-survives-regenerate rule from Story 2.6 still applies to editions the server owns.

### Story 3.1: Open the server's paper in the browser

As O,
I want to start the Docker server and see a newspaper in the browser,
So that the LAN edition is visible before I configure anything.

**Acceptance Criteria:**

**Given** O runs the Docker image with a host directory mounted at `/data`
**When** the container starts with no token in the environment
**Then** the token is the literal `matutine-demo`
**And** edition, settings, source list, and calendar snapshot are stored under `/data`, not in the image layer
**When** O opens the web client
**Then** a token gate is the first screen: one field, one primary button, chrome rather than paper, and no edition behind it
**Given** the wrong token
**When** O submits it
**Then** the page says "That token doesn't match.", the field keeps the typed value, and neither the newspaper nor Admin renders
**Given** the right token and an absent source list
**When** the gate opens
**Then** the web Edition shows the sample paper generated on the server, with the sample line visible
**And** the web client does not run the generator inside the browser
**And** the server is not an offline app
**And** every API call sends the shared token as `Authorization: Bearer`
**And** a missing or wrong bearer token is rejected before any edition is returned

**Agent notes:**

- Covers: FR20, FR5 on the server, NFR6 (Bearer token), UX-DR13, AD-13, AD-14, AD-16.
- May touch: `:server`, Docker, `:app:webApp` token gate and read-only Edition.
- Do not: build Admin, light edit, or client sync in this story. Do not open a public port in the docs. Reach is the LAN.

### Story 3.2: Generate the morning edition on the server clock

As O,
I want the server to create today's paper at a configured time,
So that the morning edition exists even when my phone and desktop are closed.

**Acceptance Criteria:**

**Given** the server is running and today's morning edition is absent
**When** the configured generation time is reached in the server timezone
**Then** the server inserts today's morning edition
**And** the day key uses that timezone and is assigned by the generator
**Given** today's morning edition already exists
**When** the schedule fires again
**Then** it does not write
**And** a clock-precise paper is claimed only while this server is running
**And** the AI key, if any, is read from the server environment and is not stored on clients
**And** the timezone value is whatever the process was given; this story does not invent an identifier format beyond accepting that string

**Agent notes:**

- Covers: FR19, NFR9 (server clock), NFR10, AD-11.
- May touch: `:server` scheduler and environment config.
- Do not: build the Admin form yet. A test may set the time and timezone through the environment. Do not replace an existing morning edition.

### Story 3.3: Configure the server in Admin

As O,
I want one browser screen for the server's sources, clock, topics, and key,
So that I do not configure the LAN paper on each device.

**Acceptance Criteria:**

**Given** a valid token
**When** O opens Admin from the newspaper
**Then** Admin can set feed URLs, an ICS URL, a CalDAV URL with username and password, generation time, timezone, the two interest questions, the AI provider and key, and the shared token
**And** the web client has no Settings screen
**And** saving interests uses the same two questions and the same choices as Story 1.6
**And** a bad feed shows "That feed didn't load." and a bad calendar link shows "That calendar link didn't open."
**And** the previous saved value stays until the new value succeeds
**And** the server may create the source list only when that document is absent
**When** O generates from Admin after setting one feed and two topics and leaving the AI key empty
**Then** the web Edition shows O's edition, the sample line is gone, the lead fits those topics, and the briefing is the extractive fallback

**Agent notes:**

- Covers: FR21, FR10 (server home for interests), UX-DR9 on web.
- May touch: `:server` admin API, `:app:webApp` Admin.
- Do not: add Todoist or extra AI providers. Do not put theme, layout, or notification text in Admin.

### Story 3.4: Light-edit a block in the browser

As O,
I want to change one line on the web paper,
So that a small correction is on the server edition without the typewriter.

**Acceptance Criteria:**

**Given** the web Edition is open with a valid token
**When** O activates a calendar line, changes a word, and leaves the field
**Then** the edition saves
**And** the active string keeps its display type, with a 1px ink underline and no toolbar and no mono face
**When** O reloads
**Then** the new line is shown
**And** last write wins
**And** there is no merge screen and no revision number on the page
**Given** the save fails
**When** the error is shown
**Then** it says "Couldn't save. Your text is still here." and Discard restores the last saved line
**And** O can light-edit any block, including briefing, story text, and the printed date
**And** changing the printed date does not change the day key
**And** while a save is in flight there is no modal, and the last confirmed line remains "Saved." until the next success

**Agent notes:**

- Covers: FR16, UX-DR8, UX-DR16 (save failed and Saved.).
- May touch: `:app:webApp` edition edit, `:server` edition write.
- Do not: add the desktop typewriter to web. Do not add a conflict UI.

### Story 3.5: Read the server edition on Android

As O,
I want the phone to show the server's morning paper,
So that I read one edition, and my old on-device papers stay where they were.

**Acceptance Criteria:**

**Given** Android Settings has a server base URL and a token
**When** the save succeeds
**Then** local editions, the local source list, and the local calendar snapshot are not uploaded
**And** the phone does not create the morning edition itself
**When** the server has today's edition, including an extractive briefing and no key on the phone
**Then** the phone shows that edition
**And** a line saved by Story 3.4 appears on the phone's next successful load
**Given** the phone cannot reach the server
**When** the Edition opens
**Then** it shows the last stored edition and "Couldn't reach the server. Showing the copy on this device."
**And** Generate stays available and writes locally
**And** an `http` LAN URL is accepted
**And** feeds, calendar, and interest questions are not editable in phone Settings while the server is configured
**And** notification time, notification text, theme, and layout stay on the phone
**And** a control forgets the server and returns those source fields to the device
**Given** the phone can reach the server and today's morning edition already exists there
**When** O regenerates or asks for a new edition now
**Then** regenerate and the extra run on the server
**And** the phone still does not insert a morning edition on its own
**And** the Edition still does not say whether the paper came from the device or the server

**Agent notes:**

- Covers: FR18 (Android, including remote regenerate and extra), FR1 (no local/server label), UJ-5, UX-DR16 (server unreachable), AD-2, AD-8, NFR7.
- May touch: `:app:androidApp` repository selection, remote repository client.
- Do not: sync old local editions up. Do not put the AI key on the phone in remote mode. Desktop is Story 3.6.

### Story 3.6: Read the server edition on Desktop

As O,
I want the desktop app to show the same server paper,
So that the typewriter edits the LAN edition rather than a private copy.

**Acceptance Criteria:**

**Given** Desktop Settings has the same server base URL and token
**When** the save succeeds
**Then** local editions and the local source list are not uploaded
**And** Desktop does not create the morning edition while the server is configured
**When** the server edition loads
**Then** the front page matches the server's current morning edition
**And** a typewriter save from Story 2.5 writes that server edition
**And** a later web reload shows that edit
**Given** the server cannot be reached
**When** the Edition opens
**Then** the last stored edition is shown with "Couldn't reach the server. Showing the copy on this device."
**And** Generate writes locally
**And** Desktop Settings while remote shows the server address, the token, and forget-server, and does not show theme, layout, or notification text
**Given** Desktop can reach the server and today's morning edition already exists there
**When** O regenerates or asks for a new edition now
**Then** regenerate and the extra run on the server
**And** Desktop still does not insert a morning edition while the server is configured
**And** the Edition still does not say whether the paper came from the device or the server

**Agent notes:**

- Covers: FR18 (Desktop, including remote regenerate and extra), FR1 (no local/server label), FR15 against a remote edition.
- May touch: `:app:desktopApp` repository selection.
- Do not: upload the pre-server editions. Do not add a Desktop morning notification.

### Story 3.7: Document the server judge path

As Sam,
I want the README to continue from the two apps to the Docker server and the browser,
So that I can see the same morning paper on the LAN.

**Acceptance Criteria:**

**Given** the Android and Desktop steps from Story 1.10
**When** Sam follows the server steps
**Then** the Docker server starts on the local network and the browser shows that morning paper after the token
**And** no step requires an AI key or an account
**And** the README does not tell Sam to expose the server to the public internet
**And** `java -jar` is not documented as a required launch

**Agent notes:**

- Covers: FR22 (Docker and browser half), AD-14.
- May touch: `README.md`.
- Do not: add a hosted public demo. Do not document VPN or reverse-proxy setup as part of the contest path.

### Story 3.8: Publish the Docker image

As a contest entrant,
I want the release workflow to publish the server image,
So that Milestone B has the same release source as the clients.

**Acceptance Criteria:**

**Given** the workflow from Story 1.11
**When** a release for Milestone B is built
**Then** it also produces the Docker image
**And** the image reads token, AI key, and timezone from the environment
**And** it does not bake those secrets into the image
**And** a `java -jar` artifact is not required

**Agent notes:**

- Covers: FR20 (image as the shipped server), FR22 (Milestone B artifact), AD-14.
- May touch: `:server` Dockerfile, `.github/workflows`.
- Do not: add a second server process. Do not add platforms or data sources.
