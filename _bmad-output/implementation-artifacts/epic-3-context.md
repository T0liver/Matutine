# Epic 3 Context: Read the server's morning edition

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

O runs one Docker server on the LAN and reads that same morning edition in the browser and on the phone. Admin is where the server is configured. Local papers are not uploaded when a client points at the server. The edit-survives-regenerate rule still applies to editions the server owns.

## Stories

- Story 3.1: Open the server's paper in the browser
- Story 3.2: Generate the morning edition on the server clock
- Story 3.3: Configure the server in Admin
- Story 3.4: Light-edit a block in the browser
- Story 3.5: Read the server edition on Android
- Story 3.6: Read the server edition on Desktop
- Story 3.7: Document the server judge path
- Story 3.8: Publish the Docker image

## Requirements & Constraints

- One shared token, no accounts, no push. Unset token is the literal `matutine-demo` on the server and on every remote client. The token is required before the newspaper or Admin renders. A mismatch says "That token doesn't match." and keeps the typed value. The same token covers the newspaper and Admin. Every remote call sends `Authorization: Bearer`.
- The shipped server is a Docker image running the Ktor process. Token, AI key, and timezone come from the process environment, not the image. Edition, settings, source list, and calendar snapshot live in a host directory mounted at `/data`, not in the image layer. Do not document a public port. `java -jar` is not a required launch.
- When the source-list document is absent, the generator builds an edition from bundled sample feeds and sample calendar data, shows the sample line, and does not write those samples into the list. An empty list is a real configuration and does not fall back to samples. No account and no AI key are required to see a paper.
- The web client does not run the generator and is not an offline app. Web has no Settings screen. Admin is the web-only place for feeds, ICS, CalDAV, generation time, timezone, interest questions, AI provider and key, and the shared token.
- The server inserts today's morning edition at the configured time only when that document is absent. It does not replace an existing morning edition. A clock-precise paper is claimed only while this server is running. The server holds the AI key; remote clients do not. The timezone value is whatever the process was given; do not invent an identifier format.
- Saving a server base URL and token does not upload local editions, the local source list, or the local calendar snapshot. Remote clients read, edit, regenerate, or request an extra. They do not mint the morning edition. If the server cannot be reached, show the last stored edition and "Couldn't reach the server. Showing the copy on this device." Generate stays available and writes locally. Android accepts a cleartext `http` LAN URL. Notification time, notification text, theme, and layout stay on the phone. Feeds, calendar, and interest questions are not editable on the phone while a server is configured.
- Web light edit changes one block in place and saves on leave. Last write wins. There is no merge screen and no revision number on the page. A failed save says "Couldn't save. Your text is still here." Changing the printed date does not change the day key.
- User-owned text on a server-owned edition survives regenerate the same way it does on the device. Whole-document replace stays the write model.
- Milestone B also publishes the Docker image from the existing release workflow. Secrets stay in the environment.

## Technical Decisions

- Modules stay `shared`, `server`, `androidApp`, `desktopApp`, and `webApp`. `server` owns the scheduler, HTTP API, stores, Docker image, and served web files. `webApp` is Compose Multiplatform for Web, HTTP only, served by the server.
- Edition documents, settings, the source list, and calendar snapshots use one kotlinx.serialization `Json` configuration with default property names and no naming strategy. `webApp` sends and reads that JSON and does not declare its own model.
- The server owns these routes, and every remote client uses them: `GET /editions`, `GET /editions/current`, `GET /editions/by-id`, `PUT /editions/by-id`, `POST /editions/morning/regenerate`, `POST /editions`, `GET /settings`, `PUT /settings`, `GET /sources`, `PUT /sources`, `PUT /calendar-snapshot`.
- Each of those documents is one JSON document. The store assigns the id on insert. The id never changes. Every write gets a new monotonic revision. The latest whole document wins.
- One source-list document holds RSS/Atom and ICS URLs. The generator reads that list and no other. The server environment writes it only when it is absent.
- Clients talk only to a repository. With a server base URL, only the server generator writes a new morning paper. Roles stay `morning` and `extra`. The front page is today's morning edition. Regenerate keeps the id. New edition now inserts an extra.
- Kotlin 2.4.21, Ktor 3.6.1, kotlinx-serialization 1.12.0, Compose Multiplatform 1.12.2, JDK 17. JVM target 17 for `shared`, `server`, and `desktopApp`.
- Reach is the LAN. One configured base URL. `http` and `https` are the same API with a different scheme. Android network security must allow that URL, including cleartext when the scheme is `http`.

## UX & Interaction Patterns

- The token gate is the first web screen: chrome, one text field, one primary button. No masthead and no edition behind a rejected token. Error copy is "That token doesn't match." The field keeps the typed value.
- After a valid token, the web Edition is the newspaper. Admin is a separate chrome screen opened from the newspaper, not a Settings screen.
- Light edit keeps the display type, uses a 1px ink underline, and has no toolbar and no mono face. "Saved." is the last confirmed line.
- The Edition does not say whether the paper came from the device or the server.
- Below 960px the web client uses the single-column order. Chrome controls are at least 40px. The edition itself stays the custom paper, not chrome.

## Cross-Story Dependencies

- Depends on Epic 1 and Epic 2. The sample-when-absent rule, the edition document, and the edit-survives-regenerate rule already exist and apply to editions the server owns.
- The browser paper and token come before the server clock, Admin, light edit, and phone or desktop remote mode.
- The server clock owns insert-if-absent for today's morning edition. Later stories must not replace an existing morning edition.
- The README server path and the release image come after the server and browser actually show a morning paper. Neither documents public internet exposure.
