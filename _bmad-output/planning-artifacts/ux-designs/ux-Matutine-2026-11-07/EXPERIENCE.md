---
name: Matutine
status: final
sources:
  - docs/project-brief.md
  - _bmad-output/planning-artifacts/decision-freeze.md
updated: 2026-11-07
---

# Matutine — Experience Spine

Visual identity is `DESIGN.md`. These two files are the contract. No mockups were produced. Where a mock is added later, the spines win on conflict.

## Foundation

Multi-surface. Android and Desktop are the contest pair and work with no server. The web client is committed with the server: the shared newspaper, light editing, and a web-only admin panel. Compose Multiplatform is the UI toolkit. [ASSUMPTION] Material 3 is the chrome toolkit only; the edition is the custom paper in `DESIGN.md`.

The edition screen does not show whether data is local or from the server. Settings and admin are where that choice is made.

Glossary:

| Term | Meaning |
|---|---|
| Edition | One document for one calendar date. Holds the briefing, stories, calendar snapshot, tasks, and any user edit. |
| Front page | The newspaper layout of an edition. |
| Briefing | The short written opening. Model text when a key exists; otherwise an extractive summary. |
| Story | One RSS or Atom item placed on the front page. |
| Rank | Lead, secondary, or note. Lead is at most one story. Size is how rank is drawn. Rank follows the person's topics. |
| Topic | Something the person named on Interests. Stories that fit a topic outrank stories that do not. |
| Typewriter | The desktop editor for the whole edition. Any part of the page can be changed there. |
| Light edit | Web in-place edit of any block on the edition. |
| Sample edition | The bundled fixture page shown when nothing has been configured. |
| Newsprint | The default theme and the default column layout in `DESIGN.md`. |
| Chrome | Settings, admin, interests, token gate, buttons, and fields. Not the paper. |

[ASSUMPTION] The edition date is the generator's timezone: the device zone when the device generates, the admin timezone when the server generates.

Later milestones (print-ready PDF, audio edition, Android home-screen widget, further task sources, further AI providers) have no surfaces here.

Out of the product, and so out of the UI: iOS, email, push notifications, real-time collaboration, auto-printing, remote access beyond the LAN.

## Information Architecture

| Surface | Reached from | Purpose |
|---|---|---|
| Edition | App open; Android notification; desktop tray **Open** | Today's front page |
| Typewriter | Edition, desktop only | Change any part of today's edition |
| Interests | Settings; Admin; the first generation of the person's own edition | Questions whose answers are the topics importance uses |
| Settings | Edition, Android and Desktop | Sources, topics, notification, theme and layout on Android, optional server, optional key, generate |
| Token gate | Web, first visit or rejected token | Enter the shared token |
| Admin | Web, after the token, link from the newspaper | Server configuration, including the interest questions |
| Tray | Desktop system tray | Open, generate, quit |
| Notification | Android system | Open today's edition |

The web newspaper is the Edition surface, with light edit turned on. It is not a separate destination.

[ASSUMPTION] There is no archive. The open edition is today. Settings do not list past dates.

Modal depth is one. Settings and admin are full screens, not dialogs stacked on dialogs.

## Voice and Tone

Microcopy. Brand posture lives in `DESIGN.md`.

| Do | Don't |
|---|---|
| "Sample edition." | "Welcome to your personalized briefing!" |
| "Today's edition is ready." | "Your morning digest has arrived ☀️" |
| "What do you want to read about?" | A personality quiz or a suggested-topic carousel |
| "No events today." | "Your calendar is clear — enjoy!" |
| "No tasks yet." | "Add a task to crush your day." |
| "No stories yet." | "We're fetching the latest news..." |
| "Summary assembled from headlines." | "AI summary" or a model name |
| "Saved." | "Successfully saved." |
| "Couldn't save. Your text is still here." | "Error 500." |
| "Couldn't reach the server. Showing the copy on this device." | "Network error. Try again later." |
| "That token doesn't match." | "Unauthorized." |
| "That feed didn't load." | "Parse exception." |
| "That calendar link didn't open." | "Invalid ICS." |
| "Calendar permission is off." | "Permission denied." |
| "The sample edition didn't load." | A blank screen |
| "Setting today's edition." | A spinner with no words |
| "Discard" | "Cancel" for throwing away unsaved edition text |

Section labels on the page are "Briefing", "Calendar", and "Tasks". The sample kicker is "SAMPLE EDITION" as specified in `DESIGN.md`.

## Component Patterns

Behavioral rules. Visual specs live in `DESIGN.md` Components.

| Component | Use | Behavioral rules |
|---|---|---|
| Masthead | Edition | Default name is Matutine, then the edition date. Both lines are part of the edition, so the typewriter and web light edit can change them. The notification title and other chrome stay Matutine. Editing the date line changes the printed line and leaves the edition filed on its calendar date. |
| Sample line | Edition, only for fixtures | Visible while the current edition is the bundled sample. Hidden on any edition generated from the person's sources. |
| Briefing | Edition | One block on the default layout. Read-only on the Android front page and on the desktop front page. Editable in the typewriter and by web light edit. If the text was produced without a model, the fallback line in Voice and Tone is shown. |
| Story | Edition | Ordered by fit to the person's topics: lead, then secondary, then note. Activating a story opens its source URL in the platform browser. The story text is editable in the typewriter and by web light edit. Android reads it. |
| Calendar block | Edition | Today's events in time order: title and start time. On the front page the block is a view of the calendar. In the typewriter, and by web light edit, the lines on this edition can be changed. Events are not stories. |
| Task block | Edition | Local task list for the first milestone. Check completes; activate the add line, type, confirm to add. [ASSUMPTION] Incomplete tasks remain on the list and appear on the next edition. Completing or adding writes the repository and survives regeneration. |
| Typewriter | Desktop edition | Replaces the front page in the same window. One mono column of the whole edition, in reading order: masthead, briefing, stories, calendar, tasks, labels. The person can change any of it, add lines, and reorder blocks. Saves when typing pauses and again on close. Close returns to the front page with ranks applied to stories. |
| Light edit | Web edition | Activate any block and edit that text in place. Leaving the field saves. |
| Interests | Settings, Admin, first own edition | Two questions start the screen: "What do you want to read about?" and "What should the page notice about your day?" The first question shows premade choices the person can tap, plus a line for their own topic. The second question is an open line. A control adds another line. Each selected choice and each non-empty line is a topic. Save stores them. The judge demo does not open this screen; the sample edition comes up first, and Interests stays available from Settings afterward. |
| Button | Chrome | Primary: generate, save a settings form, submit the token. Secondary: cancel back to the edition. One primary button per chrome screen. |
| Choice | Interests | Tap turns a premade topic on or off. Many can be on. The starter choices are Video games, Technology, Biology, Finance, Science, Sports, World news, Culture, Health, and Local. [ASSUMPTION] Science, Sports, World news, Culture, Health, and Local complete the set; Video games, Technology, Biology, and Finance were named outright. |
| Text field | Settings, admin, token gate, task add line | Label above, error sentence below in `{colors.error}` when the last attempt failed. The previous saved value stays until a new value succeeds. |
| Settings row | Settings | One row, one setting. Activating a row opens its field on the same screen. |
| Token gate | Web | One field, one primary button. The newspaper and admin do not render behind a rejected token. |
| Tray menu | Desktop tray | Three items: Open, Generate today's edition, Quit. |
| Notification | Android | Title is Matutine. Body is the notification text from Settings. The default body is "Today's edition is ready." Activating it opens the Edition. No push. |

## State Patterns

| State | Surface | Treatment |
|---|---|---|
| First launch | Edition | Sample edition, sample line visible. No key prompt, no account wall. |
| Sample fixtures missing | Edition | "The sample edition didn't load." Primary button runs the load again. |
| Generating | Edition | Masthead and date stay. Body copy is "Setting today's edition." Existing edition, if any, stays readable underneath and is replaced only when generation finishes. |
| Own edition | Edition | Sample line hidden. Stories, briefing, calendar, and tasks from the person's sources. Story order follows their topics when they have any. |
| No topics yet | Edition, after leaving the sample | The edition still generates. A line on the page opens Interests: "What do you want to read about?" Rank falls back to source order, and the first story is the lead. |
| No stories | Edition | "No stories yet." Calendar and tasks still show. |
| Extractive briefing | Briefing | Fallback line under the text. The page is still a finished edition. |
| No events | Calendar block | "No events today." |
| Calendar permission off | Calendar block, Android | "Calendar permission is off." A settings row action opens the system permission screen. The rest of the edition stays. |
| No tasks | Task block | "No tasks yet." The add line is still there. |
| Cold load with a saved edition | Edition | Show the saved edition immediately. Refresh in the background. Do not flash the sample. |
| Save in flight | Typewriter, light edit, task block | No modal. The last confirmed line remains "Saved." until the next success. |
| Save failed | Typewriter, light edit | "Couldn't save. Your text is still here." The editor stays open. Closing is allowed; the text stays in the editor until a save succeeds or the person discards with the secondary button "Discard". Discard returns the last saved edition. |
| Server unreachable | Edition, when a server is configured | Keep the last edition on the device. "Couldn't reach the server. Showing the copy on this device." Generate stays available and writes locally. |
| Token rejected | Token gate | "That token doesn't match." Field keeps the typed value. |
| Bad feed URL | Settings, admin | "That feed didn't load." The prior feed list stays. |
| Bad calendar URL | Settings, admin | "That calendar link didn't open." The prior calendar source stays. |
| Notification permission off | Android | No notification is posted. Opening the app still shows the edition. Settings still holds the time. |
| Desktop app closed | Desktop | No generation. The tray is the only path while the process is running. The next open shows the last saved edition. |
| Stale edit vs regeneration | Edition | A user edit is part of the document and remains after regeneration. Last write wins. No merge screen and no revision number on the page. |

## Interaction Primitives

**Android.** Tap. The system back control leaves Settings or Interests for the Edition. Notification tap opens the Edition. Story tap opens the source URL. No swipe between days. On the front page, the task checkbox and the add line are the writes. Theme, layout, notification text, and topics are Settings.

**Desktop.** Pointer on the front page. The typewriter takes the keyboard. Escape closes the typewriter after a save attempt. The tray menu works with the window closed. Generate from the tray uses the same action as the settings primary button.

**Web.** Token gate first. Then the edition. Activate any block to light-edit; leave the field to save. Admin is a link on the edition, available only after the token. The same token covers the newspaper and admin. Interests are inside Admin.

**Banned.** A chat transcript, email connect, a print button, an archive, a second modal, celebratory motion, badge counts, and any control that exists only as a hover. Reordering the edition belongs to the typewriter.

## Accessibility Floor

Behavioral. Contrast lives in `DESIGN.md` Colors.

[ASSUMPTION] WCAG 2.2 AA for text and for controls on all three clients.

- Reading order on the edition is masthead, sample line if present, briefing, stories in rank order, calendar, tasks. Wide layout does not change that order for assistive tech; the rail is not read before the stories.
- The typewriter is one text region named "Edition". The whole edition is inside it, including the date line.
- Android touch targets on the edition and in settings are at least 48dp. Desktop and web chrome controls are at least 40px, with the focus ring from `DESIGN.md`.
- Focus order follows reading order on the edition and field order on chrome screens. Escape closes the typewriter and nothing else.
- The sample line is text, not color alone. Rank is type size plus source order, not color.
- Story links expose the source name and the headline.
- The notification's accessible name is its current body. The default body is "Today's edition is ready."
- No information is carried by motion. The page does not animate between ranks.

## Responsive & Platform

| Surface | Android | Desktop | Web |
|---|---|---|---|
| Edition | Default is one column. Text read-only except tasks. Settings can choose another layout and a theme. | Default wide layout at ≥ 960px window width; single column below. | Same width rule as desktop. Light edit on. |
| Typewriter | Absent. | Present. Edits the whole edition. | Absent. |
| Interests | In Settings when no server is configured. | In Settings when no server is configured. | In Admin. |
| Settings | Local sources, topics, notification time, notification text, theme, layout, optional key, optional server. The screen can gain further customization controls. | Local sources, topics, optional key, optional server. | Absent. Configuration is Admin. |
| Admin | Absent. | Absent. | Feed URLs, ICS URL, CalDAV URL with username and password, generation time, timezone, interest questions, AI provider and key, shared token. |
| Tray | Absent. | Present while the process runs. | Absent. |
| Notification | Local, at the time set in Settings, with the body set in Settings. [ASSUMPTION] Default time is 07:00 in the device zone. Default body is "Today's edition is ready." | Absent. | Absent. |
| Calendar read | On-device provider, plus ICS or CalDAV if configured. | ICS or CalDAV. | Whatever the server was given. |
| Generate | On demand, and a periodic local run with approximate timing. | On demand, and only while the app is open. | The server's scheduled job. The page can also ask for a new edition. |

[ASSUMPTION] Below 960px the web client uses the single-column order. There is no separate tablet layout.

[ASSUMPTION] When a server URL and token are saved, Android and Desktop settings show the server address, the token, a control to forget the server, and on Android the notification time, notification text, theme, and layout. Feed, calendar, and interest questions are hidden there and live on Admin. With no server, those lists are on the device settings screen. The edition looks the same either way, apart from the Android theme and layout.

Android allows cleartext HTTP for the LAN server address entered in settings. The field accepts an `http` URL.

## Inspiration & Anti-patterns

- **Kept from the brief:** a front page whose headline size reflects importance; a desktop typewriter that can change the edition; Android customization of theme, layout, and notification text; a sample edition a judge can read with no accounts.
- **Importance:** the page uses the reader's topics. Matutine does not keep a second, hidden idea of what matters.
- **Rejected — generic list briefing:** the edition is columns, a masthead, and ranks, not a stack of equal cards.
- **Rejected — email as a source:** no connect-mail control.
- **Rejected — auto-printing:** no print action on the edition. PDF export is a later milestone and has no button yet.
- **Rejected — chatbot:** one briefing block per edition. No follow-up box.
- **Rejected — push:** the Android notice is a local notification after the edition exists on the device.
- **Rejected — conflict UI:** last write wins, so there is no "keep mine / keep theirs" screen.

## Key Flows

### Flow 1 — First look (Sam, contest judge, desktop, first launch)

Sam is a label for the judge role in the brief. [ASSUMPTION] The name is not a researched persona.

1. Sam starts the desktop app with the README steps. No key file and no account exist.
2. The Edition opens on the sample edition. The sample line reads "SAMPLE EDITION".
3. Masthead, briefing, a lead story, further stories, calendar events, and tasks are all filled from fixtures.
4. Sam activates the lead story and the source URL opens.
5. **Climax:** Sam is looking at a newspaper of the day — a lead headline, a briefing, a calendar, and tasks — without creating an account or pasting a key.

Failure: fixtures do not load. The page says "The sample edition didn't load." The primary button tries again. Settings is still reachable.

The sample path matches the judge demo: a visible edition with no key, no account, and no interest questions. Settings can open Interests after that edition is on screen. Tapping a premade choice there is enough to set a topic.

### Flow 2 — Morning (O, Android)

1. At the time set in Settings, a local notification says "Today's edition is ready."
2. O activates it. The Edition opens on today's date in a single column.
3. The briefing is the model text if a key is configured, otherwise the extractive text plus "Summary assembled from headlines."
4. O reads an event in the calendar block and checks one task.
5. **Climax:** The task shows completed on the page, and that completion is part of today's edition, so a later regeneration still shows it done.

Failure: notification permission is off. No notification appears. O opens the app from the launcher and the edition is there. If calendar permission is off, the calendar block says "Calendar permission is off." and the stories still read.

### Flow 3 — Leaving the sample (O, desktop settings)

1. The sample edition is on screen.
2. O opens Settings, adds one RSS URL, and on Interests taps Technology and Video games.
3. O activates "Generate today's edition". The page says "Setting today's edition." The sample stays up until the new edition is ready.
4. **Climax:** The sample line is gone. The lead headline is the story that best fits Technology or Video games. The page is O's edition for the local date.

Failure: the feed does not parse. "That feed didn't load." The sample edition stays. The field keeps the URL so O can fix it.

### Flow 4 — Typewriter (O, desktop, same day)

1. O opens today's edition, from the window or from the tray item Open.
2. O opens the typewriter. The whole edition is in one mono column, including the calendar lines and the tasks.
3. O rewrites a sentence of the briefing and a calendar line. After a pause the edition saves.
4. O closes the typewriter.
5. **Climax:** The front page shows both changes, the briefing in `{typography.briefing}` and the calendar line in the calendar block, with ranks back on the headlines. O generates again. Both changes are still there.

Failure: the save fails. "Couldn't save. Your text is still here." Escape does not throw the sentence away. "Discard" is the explicit way back to the last saved text.

### Flow 5 — Server edition (O, web admin, then Android)

1. O opens the web client on the LAN. The token gate is the first screen.
2. O submits the shared token and opens Admin.
3. O sets a feed URL, a calendar source, a generation time, a timezone, and the interest questions. The AI key is left empty.
4. The server writes today's edition. O opens the web Edition and reads an extractive briefing.
5. O points the Android app at the same server in Settings.
6. **Climax:** The phone shows that server edition, including the extractive briefing, with no AI key stored on the phone.

Failure: the token is wrong. "That token doesn't match." Admin does not open. If the phone cannot reach the server later, it shows the last edition it stored and "Couldn't reach the server. Showing the copy on this device."

### Flow 6 — Light edit (O, web)

1. O is on the web Edition with a valid token.
2. O activates a calendar line, changes a word, and leaves the field.
3. The edition saves. Last write wins.
4. **Climax:** A reload shows the new calendar line. The phone, pointed at the server, shows that same line on its next successful load.

Failure: the save fails. "Couldn't save. Your text is still here." The field keeps the edited line until save or Discard.

### Flow 7 — Android customization (O)

1. O opens Settings from the Edition.
2. O sets the notification time, rewrites the notification text, chooses a theme and a layout, and chooses the on-device calendar.
3. O returns to the Edition. The page uses that theme and layout. The calendar block lists that phone's events for today.
4. **Climax:** The next local notification uses O's sentence and O's time, and the edition it opens uses that theme, that layout, and that calendar.

Failure: O enters an ICS URL that does not open. "That calendar link didn't open." The on-device calendar remains the source.

## Assumptions

Decided, and no longer assumptions: the product name is Matutine; importance is the person's topics; the opening questions are "What do you want to read about?" and "What should the page notice about your day?"; the first question includes premade choices and a free line; the typewriter can change any part of the edition; editing the printed date does not refile the edition; Android customization includes theme, layout, and notification text; alternate themes and layouts are settings and are not drawn in this spine; the judge demo shows the sample edition before any interest question.

Still assumed:

1. The 2026-11-05 decision freeze overrides the brief where they disagree (local tasks, CalDAV, Compose web, admin panel, sample-on-first-launch).
2. Newsprint's three type sizes, typefaces, and 960px column break are the default.
3. The premade choices beyond Video games, Technology, Biology, and Finance are Science, Sports, World news, Culture, Health, and Local. With no topics, rank follows source order.
4. Incomplete tasks carry to the next edition.
5. Edition date follows the generator's timezone.
6. Default notification time is 07:00 in the device zone.
7. English only. WCAG 2.2 AA, including every alternate theme.
8. Sam is a name for the judge role. O is the author.
9. Material 3 is chrome only.
10. No archive, no revision number on the page, no unsaved-discard except the labeled Discard control.
11. With a server configured, topics move to Admin with the feeds. Theme, layout, and notification text stay on the Android device.
