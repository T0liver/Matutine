# Rubric review — Architecture Spine, Daily newspaper

Verdict: **revise**.

The spine fixes most of the feature-altitude seams (one generator, morning versus extra, last-write-wins, clocks, calendar dedup, Docker-as-the-shipped-server). It is not safe to hand to parallel epics yet. The repository switch is described with two overlapping flags, Android edit is both granted and forbidden, an edit can overwrite generator-owned fields, and the static web client is outside the JSON contract that Deferred then leaves open.

Altitude judged: feature, so the units one level down are epics. Input judged: `docs/project-brief.md`. No formal capability ids. No parent spine. No brownfield code.

## Checklist

| Item | Result |
| --- | --- |
| Real divergence points for the epics below are fixed, and none remain that would let two epics build incompatibly | Fail. Generation, rank storage, calendar dedup, clocks, targets, and the Docker distribution are fixed. The repository-mode switch, the edit payload, the remote wire (especially web), and the task read path are not. |
| Every AD Rule is enforceable and actually prevents its stated divergence | Fail. AD-2, AD-4, AD-5, AD-8, AD-11, and AD-16 do not hold together. AD-10 prevents a second vendor, not a second read path. |
| Nothing under Deferred could let two units diverge | Fail. "Field-level contents of an edition…" lets `webApp` and `shared` invent different documents. The other deferrals are safe. |
| Named tech is verified-current | Pass, on the pins as written. Not re-researched. The table and the paragraph agree: Kotlin 2.4.21, AGP 9.3.1 (not 9.5.1), Gradle 9.5.0 inside the stated Kotlin Gradle range, Ktor 3.6.1, JDK 17, kotlinx-serialization 1.12.0. No pin contradicts another. |
| Brownfield ratification | Skipped. No codebase. |
| Spec coverage | Pass under the brief bar. Cross-epic conflicts are the standard, not a restatement of the brief. The brief's load-bearing choices (two guaranteed clients, one repository, LWW, optional AI, LAN token, no iOS/email/push) are present. |
| Parent spine | Pass. None. No inherited AD is weakened because none exists. Empty `binds` matches the lack of spec ids. |
| Every owned dimension is decided, deferred, or an open question | Fail on part of operations. Deployment, environments, and infra/provider strategy are decided or explicitly deferred. Store durability and failed-generation behavior are silent. |

### Dimensions

- **Deployment.** Decided. One Docker image runs the Ktor process (AD-14). Config is environment or mounted files, not baked into the image. A later `java -jar` must be that same process and config.
- **Environments.** Decided, without using the word. Two runtimes: a client alone on device sample data, or clients pointed at one LAN host. No cloud account and no public ingress (AD-13 and the structural seed). A third environment is not sitting unspoken.
- **Infra / provider strategy.** Decided and deferred where that is safe. No cloud provider. RSS/Atom and ICS are user URLs plus bundled samples. AI and the task vendor are single ports; the concrete vendors are deferred. Web toolkit is deferred behind the HTTP boundary.
- **Operations.** Split. Who creates the morning edition, where the token and AI key live, and how config enters the process are decided. What is silent: whether the edition store, settings document, source list, and calendar snapshot survive a container recreate, and what the morning job writes when a fetch fails. Neither is an AD, a Deferred line, or an open question.

## What already holds

These are enforceable and would keep two epics apart:

- AD-1 dependency direction, including `shared` naming no Ktor type and no vendor.
- AD-3's one `morning` per day key, regenerate keeping id, `extra` leaving other editions alone, clients not inventing the day key.
- AD-4's arrival-order whole-document replace with store-assigned id and revision, and no 409.
- AD-6's single optional summary stored on the edition.
- AD-7 targets and "web is static files, HTTP only."
- AD-9 calendar union and dedup key.
- AD-11's split clocks, including remote Android not generating.
- AD-12's `expect`/`actual` port list.
- AD-13 LAN, one token, one base URL, cleartext when the scheme is http.
- AD-14 and AD-15 for the judge path.
- AD-17's Android plugin split.
- AD-18's single source-list document, aside from the seed wording below.
- Deferred product name, task vendor, AI vendor, web toolkit, `java -jar`, the server zone string, stretch adapters, and the out-of-scope list. None of those, by themselves, let two in-scope epics pick incompatible structures.

## Findings

### 1. Critical — Repository mode and settings mode are not one switch

**Disposition:** discuss.

AD-2, AD-8, and AD-11 do not name the same flag.

- AD-2 turns the generator off in "Remote mode, once a server is configured," and only then reads and writes settings "when sync mode is on."
- AD-8 has its own device flag with "sync mode" and "local mode," and local mode never calls the server settings document.
- AD-11 generates on the Android tick in "local mode" and fetches in "remote mode."

A server-and-sync epic can treat "base URL is set" as remote and keep generating forbidden. A settings epic can leave the device in local mode, so AD-11 still mints a morning paper. That is the divergence AD-2 says it prevents.

The transition is only half specified, so the settings epic and the repository epic will fill it in differently:

- Local to remote: do not upload the local settings document (AD-8) and do not merge or upload the old local store (AD-2). Source list is not mentioned.
- Remote to local: copy the server settings document onto the device (AD-8). Editions and the source list are not mentioned.

**Needed:** one sentence that either (a) one device flag selects the repository and the settings document together, or (b) the two flags are independent and each rule cites the flag it actually means. Plus what happens to editions and the source list on both transitions. Do not leave this only in Deferred: the epics cannot build the composition root until it is chosen.

### 2. High — Android is both allowed and forbidden to edit

**Disposition:** autofix.

AD-4: "Desktop and web may edit an edition body. Android does not."

The consistency table: "Android and Desktop may list, read, edit, regenerate the morning edition, and create an extra edition."

AD-2's remote-mode sentence also lists "edits editions" with no Android exception. An Android epic and a desktop/web epic can each cite a different line. The brief puts editing on desktop and light edit on web, and customization (not the edition body) on Android. Make the convention and AD-2 match AD-4: Android may list, read, regenerate, and create an extra; it does not edit an edition.

### 3. High — A whole-document edit does not prevent the rank divergence

**Disposition:** discuss.

AD-4 says a write replaces the whole edition document. AD-3 says regenerate "replaces that morning edition's body." AD-5 says only the generator sets rank and the layout model, which is the divergence it claims to prevent. "Body" is never defined.

The desktop typewriter epic and the web light-edit epic can replace rank, layout model, day key, role, events, tasks, and summary and still claim they followed AD-4. The generator epic will assume those fields survive an edit. AD-5 is then not enforceable against the editors.

**Needed:** a closed list of what an edit may change, and a statement that the store keeps id, revision, role, day key, rank, and layout model across an edit. Whether story text, summary, events, and tasks are editable is a product choice; it has to be written down because desktop and web will otherwise choose differently. "Light edit" in the capability map does not say this.

### 4. High — Deferred edition fields let web and shared diverge

**Disposition:** autofix the binding; do not invent the field schema.

AD-16 binds `shared`, `server`, and Kotlin clients to kotlinx.serialization. `webApp` is the client that cannot use that library, and it is the one left out. Deferred then says field-level contents beyond a name list are not decided. Kotlin epics still share types through AD-1, so they stay aligned. The web epic and the server epic do not. They can ship different property names, different nesting, and different story or event objects, and both will match today's text.

The same hole is the command wire. Remote behavior is a list of verbs (list, read, edit, regenerate, extra, settings, source list, calendar snapshot, "current morning") with no shared shape for how those verbs look on HTTP. Kotlin clients are told not to depend on the `server` module and not to name Ktor inside `shared`, so each client writes its own HTTP. Web has no other contract.

**Needed:** keep the internal field schema deferred only with an explicit rule that `webApp` consumes the same documents `shared` encodes and does not declare a second model. Add a convention for the token placement and the remote verbs, including a server-defined current morning read so clients keep not inventing the day key (AD-3, AD-11). A full schema is not required at this altitude.

This Deferred bullet, as written, fails the "nothing deferred may let two units diverge" test. Narrow it. Do not delete the deferral of vendor-level and layout-widget detail.

### 5. Medium — Tasks can still be read twice

**Disposition:** autofix.

The deferred field list puts tasks on the edition, and AD-10 allows only one task adapter and no email. Nothing says the UI renders that stored list and does not call the task source itself. AD-9 says exactly that for events. An Android or desktop epic can live-fetch tasks while the generator epic embeds them. Mirror the calendar sentence. Leave the vendor deferred.

### 6. Medium — Operations does not say whether a restart keeps the paper

**Disposition:** discuss.

AD-14 mounts files for token, AI key, timezone, and URLs. It never says where edition documents, the settings document, the source list, or the calendar snapshot live. The Docker epic can keep them on the container writable layer. The store epic can assume a host directory. A recreate then disagrees with "the store holds many edition documents."

Also silent, so it should be decided or deferred in one line: if RSS, ICS, or the task source fails during the morning run, does the server write nothing, write a partial edition, or retry? The shared generator is one unit, but the server scheduler epic and the local Android epic can still wrap that failure differently. AI already has a no-key rule; source failure does not.

Deployment itself is not missing. This is the operations residue.

### 7. Medium — "Seed" can wipe the source list on every boot

**Disposition:** autofix.

AD-18: in remote mode the client writes the server list, and "the server environment may only seed it." "Seed" can mean "write if absent" or "apply env URLs whenever the process starts." The second reading replaces the document under AD-4 and throws away a list saved on another client. Say the environment writes that document only when none exists.

### 8. Low — Desktop local mode has no generation trigger

**Disposition:** autofix.

AD-11 says Desktop generates only while open and has no morning notification, and that client schedulers do not create a morning edition in remote mode. It does not say what Desktop does in local mode while open: a tick, generate-on-focus, or only an explicit command. AD-3's commands are enough once the trigger is named. Bind it: while open, Desktop may call the same repository commands; it does not run its own morning clock.

### 9. Low — The demo token is "documented" but not single-sourced

**Disposition:** autofix.

AD-13: an unset token means the documented demo token. Server, Android, Desktop, and web can each document a different default and still satisfy the sentence. One rule: the unset default is a single literal shared by the server and every remote client, published once.

### 10. Low — Local-mode time zone is not in the server-zone deferral

**Disposition:** autofix.

Deferred covers the server zone string and says one configured zone exists. Local generation still needs a zone input to assign the day key (AD-3). State it or defer it in the same bullet. A sound default is: local mode uses the device zone; the server uses its configured zone; clients still do not invent the day key.

## AD enforceability

| Rule | Prevents what it claims? |
| --- | --- |
| AD-1 | Yes. |
| AD-2 | No. See finding 1. Remote "edits" also fights AD-4 (finding 2). |
| AD-3 | Yes for insert, regenerate, and extra. "Body" is undefined (finding 3). |
| AD-4 | The LWW rule is enforceable. The Android exception is contradicted (finding 2). Whole-document replace undoes AD-5 (finding 3). |
| AD-5 | Enforceable for drawing rank. Not enforceable once an editor writes the document (finding 3). |
| AD-6 | Yes. |
| AD-7 | Yes. |
| AD-8 | Settings-versus-edition is enforceable. The flag and the remote-to-local copy are not (finding 1). |
| AD-9 | Yes. |
| AD-10 | Vendor count and the email ban, yes. Second read path, no (finding 5). |
| AD-11 | Yes on its own sentences. "Local" and "remote" are not tied to AD-2 or AD-8 (finding 1). Desktop trigger is open (finding 8). |
| AD-12 | Yes. |
| AD-13 | Yes, except the demo-token literal (finding 9). |
| AD-14 | Yes. The jar deferral does not reopen a second config scheme. |
| AD-15 | Yes. |
| AD-16 | Not for web, which is the serializer that can actually diverge (finding 4). |
| AD-17 | Yes. |
| AD-18 | The single list, yes. "Seed" is ambiguous (finding 7). |

## Deferred audit

| Deferred item | Can two units diverge? |
| --- | --- |
| Product name | No. |
| Task vendor behind the one port | No, once finding 5 is fixed. |
| AI vendor, including a later local-model adapter | No. |
| Web UI toolkit | No. The HTTP boundary is fixed; the toolkit hole is finding 4, which is the document, not the toolkit. |
| `java -jar` after Docker | No. AD-14 already binds the config. |
| Server timezone string | No for the value. The local-mode source is a separate silence (finding 10). |
| Field-level edition contents | Yes. Web versus shared. Finding 4. |
| Stretch PDF, audio, widget, e-ink, with no new store or generation path | No. |
| iOS, email, realtime, push, public ingress, printer hardware | No. |

## Not findings

- Empty `binds` and no open-question section. There are no spec ids to bind. Real gaps above should become rules or explicit deferrals, not a padded question list.
- UI toolkit for Android and Desktop. Screens stay in the client (AD-12), so two clients may use different toolkits without an incompatible contract.
- Store engine (files versus SQLite) on device versus server. They do not share a disk. Durability of the server store is the part that matters (finding 6), not a shared engine.
- Server morning time (process config) versus notification time (settings document). AD-11 and AD-8 already split them on purpose.
- Version freshness against the network. The pins are internally consistent, so they were not re-checked.
