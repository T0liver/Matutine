---
title: Matutine
status: final
created: 2026-11-07
updated: 2026-11-07
---

# PRD: Matutine

## 0. Document Purpose

This PRD is the requirements contract for Matutine: what a person can do, what the contest entry must prove, and what is out. It is for the product owner, for epic and story breakdown, and for implementation. It builds on the initiative in `docs/project-brief.md`. Where that brief and later documents disagree, the shared decisions win: the UX spines (`EXPERIENCE.md`, `DESIGN.md`) decide what a person sees and does, and the architecture spine decides storage, generation, and sync. Pixel tokens, type ramps, and module layout stay in those documents and are not repeated here. Assumptions inferred while reconciling the sources are tagged `[ASSUMPTION]` and indexed in §9. Mechanism detail and the conflict log live in `addendum.md`.

## 1. Vision

Matutine is a personal morning newspaper. One person opens today's front page and sees the day: a short briefing, stories whose headline size follows what they said they care about, today's events, and their tasks. It is a newspaper, not a feed of equal cards and not a chat.

The product is also a Kotlin Multiplatform contest entry for KotlinConf 2027. Android and Desktop are the contest pair and work with no server. A judge on a fresh clone reaches a visible edition in a few minutes, with no account and no AI key. A committed second milestone adds a LAN server that generates the paper on a clock, then a web client that reads and lightly edits that same edition.

The bet is that a finished front page and a desktop typewriter are enough to make the idea legible. Sources, ranking, and the generator stay shared. The page still finishes when a feed fails or no model key exists.

## 2. Target User

### 2.1 Jobs To Be Done

- Open one front page and know the shape of today before leaving the page: what matters in the news, what is on the calendar, what tasks are waiting.
- Have headline size follow topics the reader named, with no second, hidden ranking the reader did not set.
- Read that page on the phone in the morning, and edit it on the desktop like a page rather than a form.
- Get a paper with no account. A model key improves the briefing; it is not required for the page to exist.
- Once a server is running, read and lightly edit the same morning edition from the phone, the desktop, and the browser.

### 2.2 Non-Users (v1)

- Not a second person. There is no sharing, no multi-user editing, and no accounts.
- Not an iOS user. The contest pair is Android and Desktop. Do not claim iOS.
- Not a team, a newsroom, or a public-internet audience. Server reach is the LAN.
- Not a library or a starter template. The submission is this product.

Two named roles carry the journeys. **Sam** is a contest judge with a few minutes and no prior context. **O** is the person who uses the paper in the morning and edits it. Neither is a researched market persona.

### 2.3 Key User Journeys

Journey IDs match the UX flows. Detail of layout and copy tone stays in `EXPERIENCE.md`.

**UJ-1. Sam sees a newspaper before any setup.**
- **Persona + context:** Sam is judging the entry from the README, on Desktop, with no key file and no account.
- **Entry state:** First launch. No source list has been saved.
- **Path:** The Edition opens on the sample. The line reads `SAMPLE EDITION`. A masthead, a briefing, a lead, further stories, calendar events, and tasks are filled from fixtures. Activating the lead opens its source URL.
- **Climax:** Sam is looking at a newspaper of the day without creating an account or pasting a key.
- **Resolution:** Settings stays reachable. The demo does not force Interests.
- **Edge case:** Fixtures fail to load. The page says "The sample edition didn't load." A primary action tries again.

**UJ-2. O reads the morning paper on Android.**
- **Persona + context:** O set a notification time. The edition already exists on the device.
- **Entry state:** A local notification says "Today's edition is ready."
- **Path:** O opens it. The Edition is today's date in a single column. The briefing is model text if a key is configured, otherwise an extractive summary followed by "Summary assembled from headlines." O reads an event and checks one task.
- **Climax:** The task shows completed on the page, and that completion is part of today's edition, so a later regeneration still shows it done.
- **Edge case:** Notification permission is off, so no notification is posted, and opening the app still shows the edition. Calendar permission is off, so the page says "Calendar permission is off." and the stories still read.

**UJ-3. O leaves the sample and gets their own edition.**
- **Persona + context:** The sample is on screen. O is ready to point the paper at real sources.
- **Entry state:** Desktop, sample edition.
- **Path:** Settings. One RSS URL. Interests: Technology and Video games. "Generate today's edition." The page says "Setting today's edition." The sample stays until the new edition is ready.
- **Climax:** The sample line is gone. The lead is the story that best fits those topics. The page is O's edition for the local date.
- **Edge case:** The feed does not parse. The page says "That feed didn't load." The sample stays. The field keeps the URL.

**UJ-4. O rewrites the page on the typewriter.**
- **Persona + context:** O wants the morning page to say what they mean, including a calendar line, not only the briefing.
- **Entry state:** Today's edition on Desktop, from the window or the tray **Open**.
- **Path:** The typewriter opens the whole edition in one mono column. O rewrites a briefing sentence and a calendar line. After a pause it saves. O closes the typewriter. Ranks return on the front page.
- **Climax:** Both changes are on the front page. O generates again. Both changes are still there.
- **Edge case:** Save fails. The page says "Couldn't save. Your text is still here." Escape does not discard the sentence. **Discard** returns the last saved text.

**UJ-5. O points the phone at the server's paper.**
- **Persona + context:** A LAN server is running. O configures it in the browser, then reads it on Android.
- **Entry state:** Web client. The token gate is first.
- **Path:** O submits the shared token, sets a feed, a calendar source, a generation time, a timezone, and interest questions in Admin, and leaves the AI key empty. The server writes today's edition. The web Edition shows an extractive briefing. Android Settings points at the same server.
- **Climax:** The phone shows that server edition, including the extractive briefing, with no AI key stored on the phone.
- **Edge case:** A wrong token says "That token doesn't match." and Admin does not open. If the phone later cannot reach the server, it shows the last stored edition plus "Couldn't reach the server. Showing the copy on this device."

**UJ-6. O changes one line in the browser.**
- **Persona + context:** The web Edition is open with a valid token.
- **Entry state:** Authenticated only by the shared token.
- **Path:** O activates a calendar line, changes a word, and leaves the field. The edition saves. Last write wins.
- **Climax:** A reload shows the new line. The phone, pointed at the server, shows that same line on its next successful load.
- **Edge case:** Save fails with "Couldn't save. Your text is still here." The field keeps the edit until save or **Discard**.

**UJ-7. O makes the Android paper theirs.**
- **Persona + context:** O wants the phone paper, the notification, and the calendar to match the morning they actually have.
- **Entry state:** Android Settings, opened from the Edition.
- **Path:** O sets the notification time, rewrites the notification text, chooses a theme and a layout, and chooses the on-device calendar. The Edition uses that theme and layout. The calendar block lists that phone's events for today.
- **Climax:** The next local notification uses O's sentence and O's time, and the edition it opens uses that theme, that layout, and that calendar.
- **Edge case:** An ICS URL that does not open says "That calendar link didn't open." The on-device calendar remains the source.

## 3. Glossary

Use these terms exactly. Do not substitute "feed", "issue", "digest", or "article" for the nouns below.

- **Edition** — One stored document for a civil date, with a role of morning or extra. It holds the briefing, stories, the calendar lines for that generation, tasks, rank, and any user edit. `[ASSUMPTION]` Same-day extras exist because the architecture spine requires them. The front page never becomes a multi-day archive.
- **Day key** — The civil date of an edition, `YYYY-MM-DD`, assigned by the generator. The device zone applies when the device generates. The server's configured zone applies when the server generates. Clients do not invent the day key. Editing the printed date changes the printed line and does not refile the edition.
- **Morning edition** — The edition with role morning for today's day key. The front page shows only this edition. If it is missing, the front page is empty. A list is not a substitute front page.
- **Extra** — Another edition for the same day key, opened from a same-day list. It is not the front page. `[ASSUMPTION]` The control that creates one is **New edition now**.
- **Front page** — The newspaper layout of the morning edition: masthead, optional sample line, briefing, stories in rank order, calendar, tasks. One edition fills the view. The person scrolls. There is no second page and no swipe between days.
- **Briefing** — The short written opening of an edition. Model text when a key exists; otherwise an extractive summary.
- **Story** — One RSS or Atom item placed on the front page. Events are not stories.
- **Rank** — Lead, secondary, or note. How importance is drawn. Lead is at most one story. The generator stores rank. Clients do not re-rank. Layout preferences do not change rank.
- **Importance** — The set of topics the person named. The lead is the story that best fits those topics, then secondary, then note. Matutine does not keep a second, hidden idea of what matters.
- **Topic** — One answer from Interests: a selected premade choice or a non-empty line. Stories that fit a topic outrank stories that do not.
- **Interests** — The two questions whose answers are the topics.
- **Source list** — The single list of news feeds and ICS URLs on the active store. The generator reads this list and no other. An absent list is "not configured." An empty list is a real configuration.
- **Sample edition** — The bundled fixture page shown when the source list is absent. It is not written into the source list.
- **Typewriter** — The Desktop editor. It replaces the front page in the same window and can change any part of the current edition.
- **Light edit** — In-place edit of any block on the web Edition. Not the typewriter, and not available on Android.
- **Regenerate** — An explicit request to generate again into the current morning edition. User edits that are part of the document remain. See FR-14. `[ASSUMPTION]`
- **Morning run** — An automatic insert of today's morning edition only when that edition is absent. It never replaces an existing morning edition.
- **Task** — An item on the edition from a configured task source. Checking or adding a task writes the edition and survives regeneration.
- **Repository** — The only store the screens talk to. Local: the device generates. Remote: the server owns the morning edition and clients read, edit, regenerate, or add an extra over HTTP.
- **Shared token** — The single secret for the LAN server. No accounts.
- **Chrome** — Settings, Admin, Interests, the token gate, buttons, and fields. Not the paper.
- **Milestone A** — Contest MVP. Android and Desktop. No server required.
- **Milestone B** — Committed. Ktor server, then the web client, and a Docker image.

## 4. Features

### 4.1 The front page

**Description:** The product is the morning edition drawn as a newspaper. Headline size is importance. The page is columns, a masthead, and ranks. It is not a stack of equal cards. Realizes UJ-1, UJ-2, UJ-3.

**Functional Requirements:**

#### FR-1: Open today's front page

O or Sam can open the morning edition as one newspaper page. Realizes UJ-1, UJ-2.

**Consequences (testable):**
- The reading order is masthead, the sample line if present, briefing, stories in rank order, calendar, then tasks.
- The primary view is not a flat list of equal items.
- If today's morning edition is missing, the front page is empty. A list of other editions does not replace it.
- The edition screen does not show whether the data is local or from the server.
- Activating a story opens its source URL in the platform browser.
- Section labels are "Briefing", "Calendar", and "Tasks".
- Wide layout (at or above 960px) may put calendar and tasks in a rail. Below 960px, and the Android default, the page is one column in reading order. The rail is not read before the stories.

#### FR-2: Draw rank from the reader's topics

The generator ranks stories from the person's topics, and every client draws headline size from that stored rank. Realizes UJ-3.

**Consequences (testable):**
- Ranks are lead, secondary, and note. There is at most one lead.
- With topics set, the lead is the story that best fits those topics.
- With no topics yet, the edition still generates, rank follows source order, the first story is the lead, and a line opens Interests with "What do you want to read about?" `[ASSUMPTION: source-order fallback]`
- Clients do not re-rank. A layout change rearranges blocks and does not change rank.
- The page does not animate between ranks. Rank is type size plus source order, not color.
- No model name, sparkle, or chat box appears on the edition.

#### FR-3: Show a briefing that still finishes with no key

The morning edition includes one briefing block. Realizes UJ-2, UJ-5.

**Consequences (testable):**
- With a configured key, the briefing is model text from one summarization stored on that edition.
- With no key, the briefing is an extractive summary followed by the line "Summary assembled from headlines."
- The page is a finished edition in both cases. There is no follow-up box.
- The briefing is read-only on the Android and Desktop front pages. It is editable in the typewriter and by light edit.

#### FR-4: Keep the edition a newspaper

The default edition reads as a broadsheet. Chrome does not dress itself as the paper. Realizes UJ-1.

**Consequences (testable):**
- The default theme is Newsprint: warm paper, black ink, one dark-red rule under the name, serif text.
- The sample line, when shown, is the text `SAMPLE EDITION`, not color alone.
- The edition is flat: no story shadows, no equal-card feed, no logo mark, no thumbnail, no avatars, no badge counts.
- Voice on the page is the date, the facts, and the day's list. No cheer, no streaks, no chat.
- Empty copy is exact: "No stories yet." (calendar and tasks still show), "No events today.", "No tasks yet." (the add line remains).
- A theme replaces paper, ink, and rule together and keeps the contrast floor. Which families and hex values stay in `DESIGN.md`.

**Feature-specific NFRs:**
- Text and controls meet WCAG 2.2 AA on all three clients, including every alternate theme. `[ASSUMPTION: the UX spine marks the level as assumed]`
- English only. `[ASSUMPTION]`
- Android touch targets on the edition and in Settings are at least 48dp. Desktop and web chrome controls are at least 40px.
- No information is carried by motion. Celebratory motion is banned.

### 4.2 Demo, sources, and the first real edition

**Description:** First launch proves the newspaper. After that, O points the paper at feeds, a calendar, and tasks. Realizes UJ-1, UJ-3, UJ-7.

**Functional Requirements:**

#### FR-5: Show the sample when nothing is configured

Sam can see an edition with no config. Realizes UJ-1.

**Consequences (testable):**
- When the source list document is absent, the generator builds an edition from bundled sample feeds and sample calendar data and does not write those samples into the list.
- The sample line is visible.
- No account wall and no key prompt.
- The judge path does not require answering Interests.
- A cold load that already has a saved edition shows that edition immediately and does not flash the sample.

#### FR-6: Treat an empty source list as empty

O can clear sources without falling back to the demo.

**Consequences (testable):**
- An empty source list does not use bundled samples.
- The sample line is hidden on any edition generated from the person's sources.

#### FR-7: Take news from RSS and Atom only

O can add XML syndication feeds. Realizes UJ-3.

**Consequences (testable):**
- The only news inputs are RSS and Atom URLs on the source list.
- A feed that does not parse shows "That feed didn't load." The prior list stays. The field keeps the URL.
- A failed feed is omitted from that generation, the failure is noted on the edition, and the edition still completes.

#### FR-8: Take the calendar from the device, an ICS URL, and CalDAV

O can put today's events on the page. Realizes UJ-2, UJ-7.

**Consequences (testable):**
- Milestone A calendar sources are the Android on-device calendar, an ICS URL, and CalDAV reading `VEVENT` (a URL, username, and password). Baikal is the reference server the author tests, not a runtime dependency. `[ASSUMPTION: the decision freeze puts CalDAV events in Milestone A; the architecture spine describes ICS plus a device snapshot and does not restate CalDAV events. This FR follows the freeze.]`
- The generator unions those sources. Two events with the same start, end, and trimmed title appear once. Which calendar they came from is metadata, not part of that identity.
- Screens draw `edition.events`. They do not query a calendar themselves.
- Android is the only writer of the device-calendar snapshot. An empty or failed read does not overwrite the stored snapshot.
- Calendar permission off shows "Calendar permission is off." and the rest of the edition stays.
- A bad ICS URL shows "That calendar link didn't open." The prior calendar source stays.
- A snapshot that arrives after the paper exists is not written into that paper. It appears on the next regenerate or the next extra.
- Email is never a source.

#### FR-9: Keep a local task list in Milestone A

O can see, check, and add tasks on the edition. Realizes UJ-2.

**Consequences (testable):**
- Milestone A stores one local task list behind the repository. That list is the only task source in Milestone A. `[ASSUMPTION: the decision freeze cuts Todoist and CalDAV `VTODO` to a later milestone. The architecture spine names all three on the task port. The port may exist; the later sources do not ship in A.]`
- Checking a task or confirming the add line writes the edition.
- That completion or addition is still present after a later regeneration. Realizes the climax of UJ-2.
- Incomplete tasks remain and appear on the next edition. `[ASSUMPTION]`
- A similar title in another source does not drop a task, once later sources exist.
- The generator is the only caller of the task port. The front page checkbox and add line are the person's writes.

#### FR-10: Set importance by answering two questions

O can name topics. Realizes UJ-3.

**Consequences (testable):**
- The questions are "What do you want to read about?" and "What should the page notice about your day?"
- The first question offers premade choices plus a free line. The second is an open line, with a control to add another.
- Each selected choice and each non-empty line is a topic.
- Choices named as product copy: Video games, Technology, Biology, Finance. Further choices Science, Sports, World news, Culture, Health, and Local are `[ASSUMPTION]`.
- Many topics may be on at once.
- With no server, Interests is reachable from Settings, and from the first generation of the person's own edition.
- With a server, the interest questions live in Admin with the feeds.

### 4.3 Generation on the device

**Description:** Without a server, the phone or the desktop owns the paper. Timing is honest: only a server promises a clock. Realizes UJ-2, UJ-3, UJ-4.

**Functional Requirements:**

#### FR-11: Generate today's morning edition if it is absent

A morning run inserts today's morning edition only when that document is absent. Realizes UJ-2.

**Consequences (testable):**
- A later automatic run does not write over an existing morning edition. User edits therefore survive the morning run.
- One generation, including its AI pass, produces one edition even if a feed, calendar, or task call fails.
- While generating, the masthead and date stay, the body says "Setting today's edition.", and an existing edition stays readable until the new one is ready.
- Desktop generates only while the app is open, on demand, and from the tray. It runs no morning notification.
- Android generates on demand and on a periodic local run. Timing may be approximate. That is acceptable in Milestone A.
- The day key uses the device zone when the device generates.

#### FR-12: Notify locally on Android

Android posts a local notification for the current edition at the time in Settings. Realizes UJ-2, UJ-7.

**Consequences (testable):**
- The default body is "Today's edition is ready." O can replace that sentence.
- The default time is 07:00 in the device zone. `[ASSUMPTION]`
- The title is Matutine. There is no custom notification layout and no push.
- Activating the notification opens today's morning edition.
- If notification permission is off, nothing is posted, Settings still holds the time, and opening the app still shows the edition.
- `[NOTE FOR PM]` The architecture spine does not say whether Android notifies when today's morning edition is missing and the tick does not insert one. Until that is closed, do not post a notification that opens an empty front page.

#### FR-13: Reach the desktop app from the tray

While the desktop process is running, the system tray can open the edition, generate, and quit. Realizes UJ-4.

**Consequences (testable):**
- The tray menu is the operating system's menu.
- If the process is closed, no generation runs. The next open shows the last saved edition.

#### FR-14: Offer an explicit regenerate and a same-day extra

O can refresh today's morning edition or add another edition the same day.

**Consequences (testable):**
- **Regenerate** targets the current morning edition, keeps its id, and refreshes generated stories, rank, events, and tasks from sources.
- User edits that are part of the document remain after regenerate, including a rewritten briefing and a rewritten calendar line (UJ-4) and a completed or added task (UJ-2). `[ASSUMPTION: the UX spines require edits to survive regenerate. The architecture spine says regenerate replaces the morning paper's body and that an edit may change only story text and briefing text. This FR follows the UX spines. The spine needs a revision; see addendum.]`
- **New edition now** inserts an extra and leaves every other edition untouched. The front page remains today's morning edition.
- There is no archive of past days and no revision number on the page.

### 4.4 Editing

**Description:** Desktop is where the page is rewritten. The browser can change the same edition in place. Android reads, and writes tasks. Realizes UJ-4, UJ-6.

**Functional Requirements:**

#### FR-15: Edit the whole edition on the typewriter

On Desktop, O can change any part of the current edition. Realizes UJ-4.

**Consequences (testable):**
- The typewriter is one mono column of the whole edition in reading order, including calendar lines and tasks. Any part can be changed. Lines can be added. Blocks can be reordered.
- It does not scale headline ranks. Close returns to the front page with ranks applied.
- It saves when typing pauses and again on close.
- Escape closes the typewriter after a save attempt and closes nothing else.
- A failed save says "Couldn't save. Your text is still here." **Discard** restores the last saved edition. The word is Discard, not Cancel.
- Changing the printed date does not change the day key.
- Android has no typewriter and no light edit of story text or briefing. `[ASSUMPTION on the boundary: task checkbox and add line on the Android front page are still writes, per UJ-2.]`

#### FR-16: Light-edit any block on the web

On the web Edition, O can change any block in place. Realizes UJ-6. Milestone B.

**Consequences (testable):**
- Activate a block, edit in place, leave the field to save.
- The active string keeps its display type. Light edit uses an underline, not a mono face and not a toolbar.
- Last write wins. There is no merge screen.
- A failed save keeps the edited line until save or Discard.

### 4.5 Settings on the device

**Description:** Chrome points the app at sources, topics, and, on Android, the look of the paper and the morning notice. Realizes UJ-3, UJ-7.

**Functional Requirements:**

#### FR-17: Keep device settings local until a server is chosen

O can configure the device without a server. Realizes UJ-7.

**Consequences (testable):**
- Android Settings holds local sources, topics, notification time, notification text, theme, layout, an optional key, and an optional server.
- Desktop Settings holds local sources, topics, an optional key, and an optional server. Theme and layout are Android customization.
- One row, one setting. The previous saved value stays until a new value succeeds.
- Newsprint is the default theme and the default column layout.
- Keys live in config and are never committed. A missing key still yields an edition (FR-3).
- At most one summarization call per generation. No chatbot. The MVP provider is the Google Gemini free tier, or no key. A provider that needs paid credits is out.

#### FR-18: Switch to the server without uploading the old paper

O can point a client at a server. Realizes UJ-5. The server itself is Milestone B; remembering a server address may exist on the clients in A so the judge path can continue, and it does nothing until a server answers. `[ASSUMPTION: client server-address fields ship with the client that must complete UJ-5, which is Milestone B for the working path.]`

**Consequences (testable):**
- Saving a server base URL and token does not upload local editions, the local source list, or the local calendar snapshot.
- Remote clients do not mint the morning edition. They read, edit, regenerate, or request an extra.
- If the server cannot be reached, the client shows the last stored edition and "Couldn't reach the server. Showing the copy on this device." Generate stays available and writes locally.
- Android accepts a cleartext `http` LAN URL.
- Notification time, notification text, theme, and layout stay on the Android device when a server is configured. Feeds, calendar, and interest questions move to Admin.

### 4.6 The LAN server and the web client

**Description:** Milestone B is committed. The server owns the clock and the paper. The web client is the newspaper plus light edit, and Admin is the only web-only surface. Realizes UJ-5, UJ-6.

**Functional Requirements:**

#### FR-19: Generate on a clock when the server is running

The server inserts today's morning edition at the configured time when that edition is absent. Realizes UJ-5.

**Consequences (testable):**
- A clock-precise morning paper exists only while the server is running.
- The server's configured timezone supplies the day key. The concrete timezone string is chosen in Admin and supplied to the process. `[NOTE FOR PM: the spine still defers the timezone identifier format.]`
- The server holds the AI key. Clients in remote mode do not.
- The morning run does not replace an existing morning edition (FR-11).

#### FR-20: Serve the paper on the LAN with one token

A person on the LAN can open the web client and the API with the shared token. Realizes UJ-5.

**Consequences (testable):**
- Reach is the LAN. No public ingress, no accounts, no push channel.
- The token is required before the newspaper or Admin renders. A mismatch says "That token doesn't match." and keeps the typed value.
- If the token is unset, server and remote clients use the literal `matutine-demo`. `[ASSUMPTION from the architecture spine]`
- The same token covers the newspaper and Admin.
- The required way to run the server is the Docker image. Papers and settings live in a host directory mounted at `/data`. A `java -jar` launch is not required.

#### FR-21: Configure the server from Admin

O can set the server's sources and clock in the browser. Realizes UJ-5.

**Consequences (testable):**
- Admin can set feed URLs, an ICS URL, CalDAV URL with username and password, generation time, timezone, the interest questions, the AI provider and key, and the shared token.
- Web has no Settings screen. Configuration is Admin.
- The web client does not run the generator and is not an offline app.
- About 80–90% of the newspaper UI is the shared Edition. Admin is the web-only piece.

### 4.7 The judge can finish the path

**Description:** The contest entry is not done if a stranger cannot see a newspaper. Realizes UJ-1 and the second half of UJ-5.

**Functional Requirements:**

#### FR-22: Document a path from clone to a visible edition

Sam can follow the README to a front page. Realizes UJ-1.

**Consequences (testable):**
- From a fresh clone, Sam reaches a newspaper front page on Android and on Desktop in a few minutes, with no account and no AI key.
- Milestone B adds: Sam starts the Docker server and sees that morning paper in the browser.
- A short demo video shows that path. The video is part of the submission, not part of the app.
- Milestone A release artifacts, produced by GitHub Actions on the release branch or a merged version PR, are an Android `.apk` and `.aab` and Desktop installers for Windows, Linux, and macOS. The macOS build is unsigned and not notarized. Local builds are for debug and testing.
- Milestone B also produces the Docker image.
- A phase is not done if demo mode cannot show an edition.

## 5. Non-Goals (Explicit)

- iOS, and any claim of iOS support.
- Email as a source. Push notifications. Real-time collaboration or a conflict UI. Public-internet access. Accounts. A chatbot. Printer hardware and a print button. Importing a file from disk. An archive of past days. Badge counts. Celebratory motion. A second modal.
- More than one user, and sharing an edition with another person.
- Paid-credit AI providers in the contest scope.
- The contest essay and the submission form. Those are the author's, outside the build.
- Read-later triage, receipt scanning, meeting notes, a private journal, a meal planner.
- Pixel specs, type ramps, and spacing scales. Those live in `DESIGN.md`.

## 6. MVP Scope

The scope kind is an experience MVP plus a committed platform increment. Milestone A must feel like a newspaper on two clients. Milestone B must make that newspaper a shared morning edition. Later ideas do not enter A or B to make the demo more impressive.

### 6.1 In Scope

**Milestone A**
- Front page, rank from topics, briefing with extractive fallback, sample edition (FR-1 through FR-6, FR-10).
- RSS and Atom, device calendar, ICS, CalDAV `VEVENT`, local tasks (FR-7, FR-8, FR-9).
- On-device generation, Android local notification, desktop tray, typewriter, regenerate, same-day extra (FR-11 through FR-15).
- Device settings and the judge path on Android and Desktop (FR-17, FR-22 except the Docker and browser clauses).

**Milestone B**
- Server clock, LAN token, Docker, `/data`, web Edition, light edit, Admin (FR-16, FR-18, FR-19, FR-20, FR-21, and the Docker clause of FR-22).

### 6.2 Out of Scope for MVP

- Print-ready PDF, audio edition, Android home-screen widget, e-ink layout. Later milestones. No button in this UI.
- Todoist and CalDAV `VTODO`. Later. The task port should not need a redesign to add them. `[ASSUMPTION]`
- Further AI providers (OpenRouter, Claude, OpenAI, xAI, a local model). Later. An MVP provider must be free-tier or need no key.
- `java -jar` as a supported server launch.
- Multi-user editing. Post-contest.
- A hosted public demo. Not decided. `[NOTE FOR PM]`

## 7. Success Metrics

The thesis is that a stranger recognizes a newspaper and can reach one without an account. Metrics check that, not engagement.

**Primary**
- **SM-1**: Judge path. A person who is not the author follows the README from a fresh clone and, within a few minutes, sees a front page on Android and on Desktop with no account and no AI key. Validates FR-5, FR-22.
- **SM-2**: Own paper. After one feed and two topics, the sample line is gone and the lead matches those topics. Validates FR-2, FR-6, FR-10.
- **SM-3**: Edits stick. A typewriter change to the briefing and to a calendar line, and a checked task, are still present after regenerate. Validates FR-9, FR-14, FR-15.

**Secondary**
- **SM-4**: Server path, Milestone B. The Docker server produces the morning edition, and the phone shows that edition with no AI key on the phone. Validates FR-19, FR-20, UJ-5.
- **SM-5**: Deadline. The implementation is complete in time to submit by 23:59 CET on 7 December 2026. The working aim is about five days earlier so one day remains for the video. That earlier date does not cut scope.

**Counter-metrics (do not optimize)**
- **SM-C1**: Setup burden. Do not add accounts, key prompts, or interest questions to the judge's first screen. Counterbalances SM-1.
- **SM-C2**: Feed density. Do not add stories, badges, or a second page to make the paper look fuller. A short page with a real lead is the point. Counterbalances SM-2.
- **SM-C3**: Scope creep against the video date. Do not drop Milestone B, and do not pull PDF, audio, or the widget forward, to protect the earlier recording day. Counterbalances SM-5.

## 8. Open Questions

1. How regenerate reapplies user edits onto freshly fetched stories, events, and tasks when the source item has disappeared. FR-14 requires the UJ-4 and UJ-2 climaxes. The field-level merge is not specified. `[NOTE FOR PM]` Block stories that invent a merge algorithm until this is decided. Preserving briefing text, story text the user changed, task completion, and calendar lines the user changed is the minimum bar.
2. Whether Android posts a notification when today's morning edition is missing.
3. The server timezone identifier format.
4. Whether a hosted or easily runnable web demo is in scope. Default: no.
5. Confirm or strike the assumptions in §9 before stories that depend on them are implemented. The load-bearing ones are the edit-survives-regenerate rule, CalDAV events in Milestone A, and local-only tasks in Milestone A.

## 9. Assumptions Index

- §3, FR-14 — Same-day extras exist, opened from a list, and **New edition now** is the control. The UX spines say one document per date and no archive. The architecture spine says morning plus extra. The front page stays today's morning edition only.
- §3, FR-14, FR-15 — User edits to any block survive regenerate, overriding the spine's rule that regenerate replaces the body and that only story text and briefing text are editable.
- FR-2 — With no topics, rank follows source order and the first story is the lead.
- FR-8 — CalDAV `VEVENT` ships in Milestone A, following the decision freeze where the spine is silent.
- FR-9 — Milestone A tasks are the in-app list only. Todoist and CalDAV `VTODO` wait, following the freeze where the spine names all three.
- FR-9 — Incomplete tasks carry onto the next edition.
- FR-10 — Premade topics beyond Video games, Technology, Biology, and Finance are Science, Sports, World news, Culture, Health, and Local.
- FR-12 — Default notification time is 07:00 in the device zone.
- FR-4 — WCAG 2.2 AA and English-only.
- FR-20 — Unset token means `matutine-demo`.
- §2.2 — Sam names the judge role. O names the author and daily user.

## 10. Constraints and Guardrails

**Privacy.** Single user. Keys, the Todoist token, and CalDAV passwords are configuration and are never committed. In remote mode the server holds the AI key. No cloud account.

**Safety of the demo.** A phase may not add a platform, a data source, an AI provider, or a later-milestone feature. Demo mode must be able to show an edition.

**Cost.** One batched summarization per edition. Free tier or no key. No paid credits in the contest scope.

**Timing honesty.** Do not promise a clock-precise paper on Android or Desktop without the server. Android's periodic run is approximate. Desktop generates only while open.
