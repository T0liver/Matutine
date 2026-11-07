# Adversarial review — Architecture spine

**Verdict: revise**

**Target:** `ARCHITECTURE-SPINE.md` (feature altitude, hexagonal ports-and-adapters)
**Attack:** for each finding, two units one level down (epics) can each cite the adopted rules and still ship incompatible shared data, two owners, or two mutation paths. Style, naming, and new product behavior are out of scope. Intentional last-write-wins races (AD-4) are not findings.

---

## 1. Critical — Edition id is minted on every write, and regenerate keeps it

**Units:** the owning-store epic (server store and each local store) and the morning-command epic (regenerate / edit through the repository port).

**Legal behavior A:** Every write assigns a new id and a new monotonic revision. AD-4: "The owning store assigns the id and the monotonic revision on every write." A regenerate and a desktop or web edit are writes, so each one leaves a new document id.

**Legal behavior B:** Regenerate replaces the morning body and keeps the id. AD-3: "Regenerate replaces that morning edition's body and keeps its id." The editor epic treats "a write replaces the whole document" as a replacement of that same document, so the id the UI holds still resolves.

**Clash:** After one regenerate or one edit, one side's id is the document and the other side's id is an orphan. "At most one morning for that day" can still be true on the day key while every cached id 404s. AD-3 only stabilizes the id for regenerate; an edit has no id rule at all, so the store epic and the editor epic can both claim compliance.

**Close:** Tighten AD-4 so the store assigns the id only when it inserts, assigns the revision on every write, and never changes the id. State that edit, regenerate, and the settings and source-list replaces keep the id.

---

## 2. Critical — Android both edits editions and does not

**Units:** the Android client epic and the edition-editing epic (desktop, web, and the shared repository port).

**Legal behavior A:** Android edits edition bodies. AD-2's remote-mode command list, which binds clients, includes "edits editions." The consistency table says Android may "list, read, edit, regenerate the morning edition, and create an extra edition."

**Legal behavior B:** Android never writes an edition body. AD-4: "Desktop and web may edit an edition body. Android does not."

**Clash:** One epic adds an edition-body write path on the phone. The other omits it and treats the phone as read-only plus regenerate / extra. The server cannot tell the clients apart (AD-13 is one token), so the port either accepts a phone edit or the Android epic is built against a command the port does not offer. Both are direct readings of adopted text.

**Close:** Pick one mutation right and make AD-2, AD-4, and the generation-commands row say that same thing.

---

## 3. High — Two editions can both be the front page

**Units:** the Android morning-notification epic and the desktop (and web) front-page epic.

**Legal behavior A:** The primary surface, and the edition the remote tick fetches, is the `morning` document whose day key equals today in the device's local calendar. AD-11 tells that tick to fetch "the server's current morning edition" and does not define current. AD-5 tells every client to show one front page.

**Legal behavior B:** The front page is the morning edition for today in the server's configured zone, or the morning with the greatest day key, or the newest document of either role. "New edition now" inserts an `extra` and AD-5 never says the front page stays the morning. Clients "do not invent the day key" (AD-3), which forbids them from formatting today themselves, and no server-owned selector is named.

**Clash:** The same store yields two current papers. A phone notification can open a different document than the desktop front page, and an extra can displace the morning on one client only. The day-key form is also unstated (`YYYY-MM-DD` versus a zoned instant), so two generator epics can insert two `morning` documents for one civil day and still each believe they inserted at most one.

**Close:** Add a selector AD: which document is the front page and the remote "current morning," the comparison timezone, and the day-key representation. State whether an `extra` can be that document.

---

## 4. High — One calendar snapshot, two legal writers

**Units:** the Android device-calendar epic and the desktop remote-repository epic.

**Legal behavior A:** Android pushes a device-calendar snapshot. Generation reads the latest stored snapshot on the next regenerate or extra (conventions table; AD-9). A later push replaces that snapshot.

**Legal behavior B:** Desktop remote mode also pushes a snapshot. AD-2 lists "pushes the device-calendar snapshot" as remote-mode behavior for clients, and desktop is a client. AD-12 requires a platform without a device calendar to supply an empty `actual`. The empty list is a legal snapshot. A whole-document replace (the only write rule the spine gives this kind of input) stores the empty list.

**Clash:** A desktop launch after a phone push is a legal write that removes every device event at the next generate. ICS events remain. The two clients obey the port and show different calendars on the next paper. The snapshot's identity (one document versus a per-upload blob that is not stored) is also unstated outside the conventions table, so a server epic can keep "latest" in memory for one process while the Android epic treats the push as durable.

**Close:** Name the snapshot as one stored document, name Android as its only writer, and define an empty or failed read as "leave the previous snapshot" or "replace it" — one of those, not both.

---

## 5. High — Feed and ICS URLs have two owners

**Units:** the server-config epic (Docker env / mounted files) and the source-list epic (client editor plus generator).

**Legal behavior A:** Feed URLs and ICS URLs come from the process environment or mounted files (AD-14). The generator uses that config. Restarting the container with a new env changes what the next edition fetches.

**Legal behavior B:** Those URLs live only in the source-list document on the active store. Clients write that document with a whole-document replace. "The server environment may only seed it" (AD-18). After a client write, the document is the list the generator reads, including a list the user emptied.

**Clash:** One epic re-applies env on every process start and overwrites the client document. The other treats env as a one-time seed and then ignores it, so a judge who edits the mounted file sees no change. Both satisfy "the generator reads the list" and "clients do not keep a second subscription list." AD-15's bundled samples add a third source when the list is empty, with no rule for whether those samples are written back into the document the client just cleared.

**Close:** State a single owner for the runtime URL set. State whether seed runs only when the document is absent, and whether an empty list still means bundled samples.

---

## 6. High — An edit replaces "the body" and can delete generator fields

**Units:** the desktop/web editor epic and the shared generator epic.

**Legal behavior A:** An edit replaces the whole edition document (AD-4) and "replaces the body" (AD-3 and the conventions table). The editor's body is the prose the typewriter changed. Events, tasks, rank, layout model, and summary are outside that body, so the payload omits them. The store has no field merge, so the omitted fields are gone.

**Legal behavior B:** Events, tasks, rank, layout model, and summary are fields of that same edition document (AD-5, AD-6, AD-9, AD-10). They change only when the generator runs. An edit leaves them in place until regenerate.

**Clash:** Both sides use one kotlinx.serialization document and a whole-document write. A legal edit from the editor epic drops the calendar, the tasks, and the rank the front page is required to draw. Web is not bound by AD-16 (Kotlin clients and the server are), and web does not depend on `shared` (AD-1), so the web editor can also use a different story object than the generator and still be "the edition HTTP API" (AD-7).

**Close:** Define the edit command as a full-document round-trip of the shared edition type, or name the fields an edit is allowed to change and require the rest to be preserved. Pin the JSON object the web client must send.

---

## 7. High — Tasks are edition content and a live client query

**Units:** the shared generator epic and the Android (or desktop) front-page epic.

**Legal behavior A:** The task adapter runs inside generation. Tasks are stored on the edition. The screen draws that list, the same way AD-9 forbids a second calendar beside the edition's event list. Web can show those tasks through the edition API (AD-7).

**Legal behavior B:** The task port is called when the front page is drawn. AD-10 says tasks enter through that one adapter and never says only the generator may call it. The "screens only draw" sentence in AD-5 is about rank and layout, not tasks. The client epic renders the adapter's current result.

**Clash:** Android shows live tasks; web and a generator-faithful desktop show whatever was stored at the last generate, or nothing if the generator epic did not copy tasks onto the document. One adapter, two lists, and AD-9's dedupe rule was not repeated for tasks, so the same task can also appear in both places on one screen.

**Close:** State that the generator is the only caller of the task port and that every UI renders `edition.tasks` only — or the opposite, and then web has a defined task path.

---

## 8. High — Local "generate" both rewrites the morning paper and must not

**Units:** the Android WorkManager epic (and the desktop process-lifetime epic) and the morning-insert epic in the shared generator.

**Legal behavior A:** In local mode each WorkManager tick generates and notifies (AD-11). Desktop generates while its process is open (AD-11), so a later tick or a second launch runs the generator again and writes today's morning edition.

**Legal behavior B:** The morning path inserts at most one `morning` for that day and does not write it again (AD-3). Only an explicit regenerate replaces the body. A second tick is not that command, so it must not write.

**Clash:** One implementation refreshes the paper on every tick or every launch and wipes a same-day edit, which is the outcome AD-3 exists to prevent. The other no-ops after the first insert, so the desktop epic that "generates while open" produces a paper once and then never refreshes until the user finds a regenerate command the desktop clock rule does not mention. Remote mode is unaffected: the client schedulers do not generate there.

**Close:** State that a local tick or a desktop launch inserts today's morning only when it is absent, and that any later automatic fire does not write. Regenerate remains the only replace.

---

## 9. Medium — Calendar instants do not survive the dedupe key

**Units:** the ICS adapter epic and the Android snapshot epic, meeting in the shared generator.

**Legal behavior A:** ICS events keep the DTSTART/DTEND text the feed used. AD-9 compares trimmed start, end, and title.

**Legal behavior B:** The device snapshot uses epoch milliseconds or a zoned ISO instant. The generator still compares trimmed strings and does not normalize.

**Clash:** The same event is two events. Both adapters obeyed the key. Source calendar is metadata, so it cannot rescue the match.

**Close:** Pin one instant representation for start and end on the event input the generator dedupes, and require both adapters to emit that form.

---

## 10. Medium — Settings are one document with two field owners

**Units:** the Android settings epic (notification time) and the desktop settings epic (layout prefs).

**Legal behavior A:** Android writes the settings document with the notification time. Layout prefs are unchanged, so this payload does not carry them. Sync mode whole-replaces the server document (AD-8, AD-4).

**Legal behavior B:** Desktop writes layout prefs the same way and does not carry the notification time.

**Clash:** Each save deletes the other device's half of the one document the spine created so those prefs would sync. This is not the accepted concurrent-edit race; each epic is the sole writer of "its" fields and still follows whole-document replace. The field names and types are not specified, so the two epics can also encode time and layout under different keys inside one kotlinx.serialization codec (AD-16).

**Close:** Require every settings write to round-trip the full shared settings type, and name that type's fields.

---

## 11. Medium — The edition HTTP API is not a contract

**Units:** the Ktor API epic and the webApp epic. The same gap sits between the server and each Kotlin remote-repository epic.

**Legal behavior A:** The server exposes regenerate, extra, list, read, and edit on resource paths it chooses, with the token in a header, and encodes with a `Json` configuration it chooses (snake_case, omit defaults).

**Legal behavior B:** webApp calls a different path set, sends the token in a query string, and reads property names that match the Kotlin defaults. It uses only HTTP and one base URL (AD-7, AD-13). AD-16 does not bind webApp.

**Clash:** Both sides are the edition HTTP API. They do not interoperate. Kotlin clients share `shared` types (AD-1) but not the `Json` configuration or the routes, so they can diverge from the server without a second model.

**Close:** Pin routes (or one route table owned by the server module), the token placement, and one kotlinx.serialization `Json` configuration for every document AD-16 names.

---

## 12. Medium — The demo token has no value

**Units:** the server auth epic and any remote client epic (Android, desktop, web).

**Legal behavior A:** An unset token is the documented demo token `demo` (or whatever literal that epic writes down). AD-13.

**Legal behavior B:** The other epic documents a different literal. Nothing in the spine is that document.

**Clash:** A judge with unset configuration on both sides fails auth. Both epics implemented "an unset token means the documented demo token."

**Close:** Put the demo-token literal in the spine, or name the single file whose value both sides must use.

---

## 13. Medium — The AI pass is a second write the morning insert forbids

**Units:** the morning-schedule epic and the AI-port epic.

**Legal behavior A:** The schedule inserts the morning edition once and does not write it again (AD-3). The document stored at insert is the paper.

**Legal behavior B:** One summarization call per generation stores its result on that edition (AD-6). The port does not say the call finishes inside the insert. A late result writes the summary onto the morning document afterward. That write is not a regenerate.

**Clash:** The schedule epic drops or refuses the summary write, so the edition stays on the no-key fallback after a successful call. The AI epic performs the write and breaks the once-only insert. Both followed the rule that names their own step.

**Close:** State that the summary is part of the same insert or regenerate write as the rest of the edition, and that no later AI write hits a morning document.

---

## Not holes

- Whole-document last-write-wins with no 409, including a later edit beating an earlier one, is AD-4's chosen race.
- Local stores staying local when a server is later configured is AD-2.
- Server morning time and the Android notification time differing is AD-11.
- One task vendor left unnamed, one AI vendor left unnamed, and web UI toolkit left open are single-port deferrals. They become holes only if a second port appears; AD-10 and AD-6 already forbid that.
- Rank's exact type is safe while one shared generator writes it and every UI orders by that value. A headline-size step function is not fixed; that is a presentation gap, not two mutation paths, and it is not listed above.
