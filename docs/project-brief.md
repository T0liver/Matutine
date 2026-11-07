# Project Brief: Matutine

A Kotlin Multiplatform app that assembles a personal "front page" every morning from RSS feeds, a calendar and a task source, with light AI summarization. An optional self-hosted Ktor server adds scheduled generation and sync between devices.

> **Note for the agent:** Read this whole file first. Items marked **[UNVERIFIED]** are assumptions that should be checked before relying on them. Section 14 has no remaining product decisions.

---

## 1. Context: the contest

- **Contest:** Kotlin Multiplatform Contest, run by the Kotlin Foundation, tied to KotlinConf. It targets students and recent graduates. Entrants must be 18+.
- **Round:** KotlinConf 2027 (Kraków, April 22-23, 2027).
- **Deadline:** 23:59 CET on December 7, 2026. Winners are announced December 21, 2026. Confirmed on the contest page and the rules page on 7 November 2026. The author wants implementation finished about five days earlier so one day remains for the demo video. That earlier date is a soft aim, not a scope cutoff. https://kotlinconf.com/contest/
- **Prize:** Trip to KotlinConf for the top three; souvenirs for other eligible entrants.
- **Rules that matter:**
  - The project must use Kotlin Multiplatform and run on **at least two platforms**.
  - It must be made specifically for the contest (not a library or a template).
  - Submission is a GitHub link. The **README must explain how to run the project on every target and how to try the key features.**
  - One project per entrant.
- **Judging:** creativity of the idea and how well KMP supports it.
- **AI:** encouraged ("give your project an edge") but **optional**.
- **Possible resource:** the Kotlin Foundation published a KMP Contest Starter Kit on GitHub. **[UNVERIFIED]** whether it is useful for this project; check the contest page.
- **Open rule question [UNVERIFIED]:** whether a Ktor server counts as one of the "platforms". Do not assume it does. Android and Desktop must be the two guaranteed targets.

## 2. Author constraints

- **No Mac, so no iOS build.** Targets are Android, Desktop (JVM), and Web, plus a JVM server. Do not claim iOS support. Optionally, a GitHub Actions macOS runner could verify that the iOS target compiles, but that is out of scope and untestable by the author.
- **AI must be cheap or free.** No budget for paid credits. The MVP default provider is the Google Gemini free tier. The app still runs with no key. The provider interface must also accept other providers later, including OpenRouter's free models, other OpenRouter models, and further providers. OpenRouter's current free-tier limits are **[UNVERIFIED]**.
- **Time:** about six weeks to the deadline. The author hopes to build much of it faster with coding agents.

## 3. The idea

A cross-platform app that delivers a **daily newspaper**: a one-page "front page" generated each morning, giving a briefing on what the day looks like.

**Inputs**
- RSS news sources.
- Calendar events.
- A task source.
- Email was considered and **rejected as too much scope** (OAuth and verification overhead).

**Output**
- A newspaper-style front page, with headline size reflecting importance, a short AI-written briefing, today's calendar, and tasks.
- Viewable on mobile and desktop, and in a browser through the server's web interface.

**Differentiators (the hook)**
- The newspaper metaphor (front-page layout, headline hierarchy) instead of a generic list-style briefing.
- A **typewriter-style editor on desktop** for editing the edition (better keyboard and screen).
- **Customization options on Android.**
- A self-hosted server that makes the morning edition arrive on time.

**Printing and paper:** the author originally considered auto-printing (Raspberry Pi or printer) but is concerned about paper waste. **Decision: demote printing.** At most provide "export as print-ready PDF" as a normal feature. Better twists to consider if time remains: an **audio edition** (text-to-speech) or an e-ink-style one-page layout for a spare tablet or screen.

## 4. Architecture

### Modules
- **`shared`** (Kotlin Multiplatform): data models, RSS/feed parsing, ranking, the **edition generator**, the AI client, and repository interfaces. Used by both clients and server.
- **`server`** (Ktor, JVM): scheduled daily generation, API, storage, sync, serves the web client as static files, holds the AI key so clients don't need one.
- **`androidApp`**, **`desktopApp`**, **`webApp`**: UI clients.

### Key design: one repository interface, two implementations
- **Local repository:** generates the edition on the device itself.
- **Remote repository:** fetches (and syncs edits of) the edition from the Ktor server.
- The UI does not know which one it uses. Result: **the app works standalone, and gets better (scheduled, synced) when a server is present.**

### Server responsibilities
- Daily generation job (a coroutine-based scheduler is enough).
- REST API for editions and edits.
- Sync using one JSON document per edition with a revision number, **last-write-wins** (single user; no real-time collaboration).
- Hosting the web interface.
- Simple auth: shared token.

### Client behavior without the server
- Clients can generate an edition on demand or periodically but with **no guaranteed timing**.
- Android: WorkManager periodic fetch (approximate timing is fine, e.g. "around 7am").
- Desktop: only generates while the app is open (document this clearly).
- Timer-precise generation without a server is out of scope for the first version.

## 5. Platform-specific features (to show real cross-platform design via `expect/actual`)

- **Android:** read the device calendar (local calendar provider, no OAuth), morning local notification, optional home-screen widget, customization settings.
- **Desktop:** typewriter-style editor. The document written there is kept by the app and included in the edition. System tray icon. PDF export is stretch. Importing a file chosen from disk is out.
- **Web:** Compose Multiplatform for Web, served by the server. The project already uses Compose Multiplatform 1.12.2. The web target is Kotlin/Wasm and is Beta.
- Optional later: text-to-speech audio edition.

## 6. Data sources: avoid OAuth where possible

- **Calendar:** ICS URL import (all platforms) plus Android's local calendar provider.
- **Tasks:** any of these may be configured, including more than one: a Todoist personal API token, a task list kept in Matutine, and a CalDAV to-do collection. The author will test CalDAV with Baikal. The generator includes every configured source.
- **News:** RSS/Atom feeds.
- **Email:** out of scope.

## 7. AI approach (light and free)

- **One batched summarization call per edition**, cached locally or on the server. No chatbot.
- **Provider:** The MVP default is the Google Gemini free tier. The summary interface is a port: later adapters may target OpenRouter (free models or a model the user picks) and other providers. A local model via an Ollama-style server stays a later adapter on that same port. Gemini free-tier limits change and vary by country **[UNVERIFIED: check AI Studio before relying on a number]**.
- **Rules:**
  - **Bring your own key.** Never commit keys to the public repo.
  - **AI is optional.** With no key, fall back to plain headlines or a simple extractive summary so judges can run the app without setup.
  - On the server, the key lives in server configuration so clients don't need one.

## 8. Judge experience (critical)

Judges must be able to run it quickly. Therefore:
- Android and Desktop are the two guaranteed targets.
- Server is trivially runnable from a **Docker image**. A `java -jar` launch is later, only if time remains, and must use the same process and config.
- **Demo mode** with bundled sample feeds and sample calendar data, requiring no keys and no accounts.
- README is written like a demo script: exact steps from clone to a visible edition on each target, in a few minutes.
- Consider a hosted or easily runnable web demo.
- Include a short demo video.

## 9. Networking notes (MVP)

- Server access is **LAN-only** with a shared token. Remote access (VPN or reverse proxy) is a documented future step.
- Android blocks cleartext HTTP by default, so allow it for local addresses or use a certificate.
- No push notifications in the MVP (avoids Firebase setup). Use a scheduled local notification from background fetch.

## 10. Scope

### In scope (MVP)
- Shared generator: RSS fetch, rank, layout model.
- Desktop UI (works standalone).
- Android client with local calendar and morning notification.
- Ktor server with scheduled job, API, sync, auth token.
- Web client served by the server.
- Typewriter-style editor on desktop.
- Optional AI summary with non-AI fallback.
- Docker image, demo mode, README, demo video. A `java -jar` launch only if time remains.

### Stretch (only if the MVP is in good shape during implementation; no time is reserved)
- Print-ready PDF export.
- Audio edition (text-to-speech).
- Android home-screen widget.
- Ollama/local-model provider, on the same summary port as Gemini.
- `java -jar` server packaging.

### Out of scope
- iOS.
- Email ingestion.
- Real-time collaboration or complex conflict resolution.
- Push notifications.
- Remote/internet access beyond documentation.
- Auto-printing hardware integration (Pi/printer).
- Importing an existing file from disk into the edition.

## 11. Suggested plan (about 6 weeks; compress if using agents)

1. **Week 1-2:** shared generator (RSS, ranking, layout model) and Desktop UI, working standalone.
2. **Week 3:** Android client with local calendar, notification, same generator.
3. **Week 4:** Ktor server with scheduled job, API, sync; clients switch to the remote repository.
4. **Week 5:** web client served by the server; typewriter editor polish.
5. **Week 6:** AI summaries with fallback, README, Docker, demo video, buffer.

**Suggested first step:** sketch the shared module's data model and the repository interface, since everything else depends on them.

## 12. Risks and unknowns

- Server may not count as a "platform"; keep Android + Desktop as the guaranteed pair.
- Compose Multiplatform for Web is Beta (Kotlin/Wasm). The web client uses it anyway. Stability can still change between releases.
- Free AI limits and availability may change.
- Daily-briefing apps already exist; the newspaper metaphor and typewriter editor are the differentiators, so prioritize polish there.
- Server setup could hurt the judge experience if it is awkward to run.
- Android background timing is approximate.

## 13. Other ideas considered (backup options, all light-AI)

1. **Read-later triage:** Android share sheet and desktop clipboard capture links; AI gives a one-line summary and tags; weekly digest.
2. **Receipt scanner:** Android camera plus one vision call per photo; desktop dashboard.
3. **Lecture/meeting notes cleaner:** raw notes or recording turned into structured notes and action items in one call.
4. **Private journal with weekly reflection:** local entries, one AI summary per week.
5. **Fridge-photo meal planner:** camera input, one vision call, shared recipe and shopping-list logic.

The newspaper idea was chosen as the primary direction for feasibility, clear daily value, light AI use, an easy demo, and strong platform-specific hooks.

## 14. Open questions for the author

None. The web client is Compose Multiplatform for Web. Stretch features have no reserved time; they are pulled in only if the MVP is in good shape during implementation.
