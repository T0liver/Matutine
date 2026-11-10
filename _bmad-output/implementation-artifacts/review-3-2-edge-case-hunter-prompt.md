Read `# Edge Case Hunter Review

**Goal:** You are a pure path tracer. Never comment on whether code is good or bad; only list missing handling.
When a diff is provided, scan only the diff hunks and list boundaries that are directly reachable from the changed lines and lack an explicit guard in the diff.
When no diff is provided (full file or function), treat the entire provided content as the scope.
Ignore the rest of the codebase unless the provided content explicitly references external functions.
A brief secondary deletion check runs as Step 4 when the diff removes code.
A claims check runs as Step 5.

**Inputs:**
- **content** — Content to review, or a path to read it from: diff, full file, or function
- **also_consider** (optional) — Areas to keep in mind during review alongside normal edge-case analysis
- **claims_file** — Path to the spec this change was built from. Do NOT read it before Step 5: the path tracing in Steps 2–3 must finish before the claims are seen.

**MANDATORY: Execute steps in the Execution section IN EXACT ORDER. DO NOT skip steps or change the sequence. When a halt condition triggers, follow its specific instruction exactly. Each action within a step is a REQUIRED action to complete that step.**

**Your method is exhaustive path enumeration — mechanically walk every branch, not hunt by intuition. Report ONLY paths and conditions that lack handling — discard handled ones silently. Do NOT editorialize or add filler. Do not assign severity labels, rankings, or priority levels.**


## EXECUTION

### Step 1: Receive Content

- Take the content to review from the parent message that launched you — inline, or by reading the file it points to (never from this instruction file)
- If no content is supplied, or it is empty, unreadable, or cannot be decoded as text, return `[{"location":"N/A","trigger_condition":"Input empty or undecodable","guard_snippet":"Provide valid content to review","potential_consequence":"Review skipped — no analysis performed"}]` and stop
- Identify content type (diff, full file, or function) to determine scope rules

### Step 2: Exhaustive Path Analysis

**Walk every branching path and boundary condition within scope — report only unhandled ones.**

- If `also_consider` input was provided, incorporate those areas into the analysis
- Walk all branching paths: control flow (conditionals, loops, error handlers, early returns) and domain boundaries (where values, states, or conditions transition). Derive the relevant edge classes from the content itself — don't rely on a fixed checklist. Examples: missing else/default, unguarded inputs, off-by-one loops, arithmetic overflow, implicit type coercion, race conditions, timeout gaps
- Consider implicit branches: the diff special-cases or changes the handling of one or more members of a fixed set of values — enums, status codes, sentinels, type tags, flags, value ranges. The rest of the set is implicit branches (e.g. the diff changes the `RED` and `YELLOW` cases of a `RED`/`YELLOW`/`GREEN` enum; `GREEN` is the implicit branch)
- Consider handle lifetime: when the changed code re-checks, re-fetches, or re-validates something it already held — a handle, index, id, pointer — the re-check exists because an intervening call can invalidate it. Identify that call, what it does to the thing held, and what the changed code silently skips when the re-check fails
- For each call site the diff adds or changes — in test files as well as production code — read the callee's declaration and check the call against it: argument count, order, types, and defaults. Report any mismatch
- For each path: determine whether the content handles it
- Collect only the unhandled paths as findings — discard handled ones silently

### Step 3: Validate Completeness

- Revisit every edge class from Step 2 — e.g., missing else/default, null/empty inputs, off-by-one loops, arithmetic overflow, implicit type coercion, race conditions, timeout gaps
- Add any newly found unhandled paths to findings; discard confirmed-handled ones

### Step 4: Deletion Check

If the diff removed or replaced meaningful code (ignore pure renames and whitespace): load `references/deletion-check.md` and follow it.

### Step 5: Claims Check

Load `references/claims-check.md` and follow it.

### Step 6: Present Findings

Output all findings as a single JSON array following the Output Format specification exactly.


## OUTPUT FORMAT

Return ONLY a valid JSON array of objects. Each edge-case finding contains exactly these four fields:

```json
[{
  "location": "file:start-end (or file:line when single line, or file:hunk when exact line unavailable)",
  "trigger_condition": "one-line description (max 15 words)",
  "guard_snippet": "minimal code sketch that closes the gap (single-line escaped string, no raw newlines or unescaped quotes)",
  "potential_consequence": "what could actually go wrong (max 15 words)"
}]
```

No extra text, no explanations, no markdown wrapping. An empty array `[]` is valid when nothing is found. Deletion findings from Step 4 and claim findings from Step 5, if any, go in the same array with the extra fields defined in `references/deletion-check.md` and `references/claims-check.md`.


## HALT CONDITIONS

- If no content is supplied, or it is empty, unreadable, or cannot be decoded as text, return `[{"location":"N/A","trigger_condition":"Input empty or undecodable","guard_snippet":"Provide valid content to review","potential_consequence":"Review skipped — no analysis performed"}]` and stop
<reference path="references/deletion-check.md">
# Deletion Check

Secondary pass for the Edge Case Hunter — runs only when the diff removed meaningful code. Subordinate to the edge-case pass; findings are usually few or none.

For each chunk of removed or replaced code (ignore pure renames and whitespace), ask: did it carry behavior or a contract that the change neither re-established nor intentionally retired? Add a finding for any resulting regression, orphaned reference, or newly-dead code. Skip anything already covered by your edge-case findings.

Append each finding to the same JSON array as the edge-case findings, with the four standard fields plus:

- `kind`: `"deletion"`
- `confidence`: `"high"`, `"medium"`, or `"low"` — these are inferences; rate them

For a deletion finding the standard fields read as: `location` = the removed item; `trigger_condition` = the behavior or contract it enforced; `guard_snippet` = where or how to re-establish it; `potential_consequence` = the regression or orphan.

Add nothing if nothing qualifies.
</reference>
<reference path="references/claims-check.md">
# Claims Check

Final pass for the Edge Case Hunter. Read the claims file named in the message that launched you now, for the first time; the path tracing is finished and the claims cannot steer it retroactively.

It is the spec the change was built from. Read only its `## Intent` and `## Tasks & Acceptance` sections — the claims live there; ignore the rest of the file. The spec is the change's own account of itself: testimony, not evidence — a claim repeated in a code comment is still the same claim, not confirmation. Extract each checkable claim — what the change does, what it preserves, ordering, arithmetic, and parity with existing code ("exactly as X does") — then try to falsify each one against the code you have already traced. Where your trace is not enough to decide, read the code that decides it: the compared-to function, the actual callee, the state the claim assumes.

Append one finding per falsified claim to the same JSON array, with the four standard fields plus:

- `kind`: `"claim"`
- `confidence`: `"high"`, `"medium"`, or `"low"`

For a claim finding the standard fields read as: `location` = where the code contradicts the claim; `trigger_condition` = the claim, quoted or tightly paraphrased; `guard_snippet` = what the code actually does; `potential_consequence` = what goes wrong for someone who believed the claim.

Verified claims produce nothing. Add nothing if nothing is falsified.
</reference>

## CONTENT SOURCE

"Review content:" in the message that launched you gives the content itself or a path to read it from. Read the file when it is a path; either way that is the content under review, and this instruction file never is.
` completely and follow it as your review instructions.

claims_file (leave unread until your instructions call for it): ---
title: 'Generate the morning edition on the server clock'
type: 'feature'
created: '2026-11-10'
status: 'in-review'
baseline_commit: '4b57ede7cf6ca7d43bbf180250b81d904035a479'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Today's morning edition appears only if a device is open or someone loads the paper. The server process has no clock, so a closed phone and desktop leave no edition.

**Approach:** While the Ktor process is running, a coroutine scheduler inserts today's morning edition at the configured time in the process timezone, and only when that document is absent. The generator assigns the day key. The AI key is read from the server environment and is not written onto a client or into the store.

## Boundaries & Constraints

**Always:**
- AD-11: only this server coroutine scheduler creates the morning edition at a configured time, and only when today's morning document is absent. AD-3: a later automatic fire does not write. The generator (`SampleGenerator.forDay` or `EditionGenerator.fromFeedResults`) assigns the day key. The scheduler does not format it.
- The zone string and the generation-time string come from the process environment. Blank zone uses the process zone. Blank generation time is `07:00`. A generation time `parseNotificationTime` accepts is the clock. The scheduler fires only while the local minute in that zone equals that clock, including when the process is already inside that minute. It does not catch up later in the day.
- An absent source list inserts the bundled sample. A present list, including an empty one, uses `runMorning` and does not fall back to samples. `serverConfigured` stays false on this process. `generateToday` is not the clock.
- An existing morning document, including a saved sample or a hand-edited briefing, is left unread from sources and is not replaced.
- The AI key is `MATUTINE_GEMINI_KEY`. Blank means no key. The repository sees it only in memory. `settings.json` keeps whatever key it already had, or stays absent. The web client is unchanged and does not receive the key.
- The same zone and clock feed `GET /editions/current` and the scheduler. A failed insert leaves stored documents unchanged and leaves the process running.

**Never:**
- No Admin form, light edit, client sync, public port, README change, or timezone-identifier design.
- Do not redo story 3.1's token gate, and do not add the other AD-16 routes.
- Do not start the Android emulator. Do not change device schedulers.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Due, no paper, no source list | Local minute equals the clock, no morning file, no `sources.json` | One sample morning file whose `dayKey` is the generator's date in the given zone | N/A |
| Zone crosses midnight | Instant is still the previous UTC date, zone string is the one the process was given | `dayKey` is the civil date `todayLocalDate` would use in that zone | N/A |
| Already stored | Morning file exists, briefing edited | Tick does not write; id, revision, and briefing stay | N/A |
| Second fire | Same local day after a successful insert | No second write and no source read | N/A |
| Not yet due | Local time is before the clock minute, and the process did not start inside it | No morning file is created | N/A |
| After the minute | Process observes only a later minute the same day | No catch-up write | N/A |
| Empty or bad list | Source list present, feeds missing or unreadable | No morning file; list file unchanged | Insert failure does not stop the process |
| AI key | `MATUTINE_GEMINI_KEY` set, personal morning inserted | Summarizer receives that key; `settings.json` does not gain it | Blank key leaves the extractive briefing |
| Stopped process | Module is not running | No scheduled insert | N/A |

</frozen-after-approval>

## Code Map

- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` — token and `GET /editions/current` already exist. Build one `LocalEditionRepository` with `today = { todayLocalDate(zone) }`, that `timeZone`, a JVM feed reader, and a Gemini `summarize`. Start the clock from `module` and cancel it on application stop. Do not call `generateToday`.
- `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` — documents under the data root. Do not add a second file layout.
- `core/.../EditionRepository.kt` — `runMorning` inserts only when absent and `serverConfigured` is false. `loadCurrentMorningEdition` inserts the sample when the source list is absent and returns an existing morning first. Reuse both. Do not change device `runMorning`.
- `core/.../DayKey.kt` — `todayLocalDate(timeZone)` and `toDayKey`. The repository calls these. The clock does not write a key.
- `core/.../ReaderSettings.kt` — `parseNotificationTime` and `DEFAULT_NOTIFICATION_TIME` (`07:00`). Reuse them. `geminiKey` stays on the settings document only when something else saved it.
- `core/.../EditionGenerator.kt` and `SampleGenerator.kt` — assign `dayKey` from the `LocalDate` they are given.
- `app/shared/.../FeedReader.jvm.kt` and `GeminiBriefingClient.jvm.kt` — the HTTP shape to copy into `:server`. Do not depend on `:app:shared`.
- `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` — story 3.1 rows stay. Add clock rows here. Tests pass zone, generation time, key, and instant in; they do not need Admin.

## Tasks & Acceptance

**Execution:**
- [x] `server/src/main/kotlin/hu/toliver/matutine/ServerMorningClock.kt` — resolve zone and generation time, decide when a tick is due, and insert through the repository only when due.
- [x] `server/src/main/kotlin/hu/toliver/matutine/ServerGeneration.kt` — JVM feed reader, in-memory Gemini key, and the Gemini summarize call. Never write the env key.
- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` — one zoned repository for the current-edition route and the coroutine clock.
- [x] `server/build.gradle.kts` — coroutines for the scheduler.
- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` — cover the I/O rows for due, zone day key, existing edition, second fire, not-due, no catch-up, bad list, and the env key.

**Acceptance Criteria:**
- Given the server is running and today's morning edition is absent, when the configured generation time is reached in the server timezone, then the server inserts today's morning edition and the generator assigns the day key in that timezone.
- Given today's morning edition already exists, when the schedule fires again, then it does not write.
- Given the process is not running, when the generation time passes, then no scheduled insert happens.
- Given an AI key in the server environment, when a morning edition is generated, then that key is used in memory and is not stored for clients.
- Given a timezone string, when the process receives it, then that string is the zone, with no identifier format added by this story.

## Implementation Notes

- The repository date is `clock().toLocalDateTime(zone).date`. With `Clock.System` that is the same civil date as `todayLocalDate(zone)`, and tests can pin the instant.
- A generation time `parseNotificationTime` rejects does not start the clock. Blank stays `07:00`.
- `MATUTINE_TIMEZONE`, `MATUTINE_GENERATION_TIME`, and `MATUTINE_GEMINI_KEY` are the environment names. The zone string is passed to `TimeZone.of`.
- Suspend functions cannot be Ktor attribute types, so tests install feed and summarize lambdas through a `ServerPorts` holder.
- The clock coroutine is a child of the application scope and is also cancelled on `ApplicationStopped`.
- `./gradlew :server:test` passed 17 tests, including the story 3.1 rows. `:core:jvmTest --tests hu.toliver.matutine.edition.LocalEditionRepositoryTest` stayed green.

## Spec Change Log

## Review Triage Log

## Design Notes

The clock is a one-second poll inside the server process. A tick is due when the zone-local hour and minute equal the configured clock. Starting during that minute still inserts once. Starting afterward waits until the next day, which is the clock-precise claim: a missed minute is not reconstructed. Absent `sources.json` uses the sample insert already used by `GET /editions/current`. A present list uses `runMorning`. Environment names are `MATUTINE_TIMEZONE`, `MATUTINE_GENERATION_TIME`, and `MATUTINE_GEMINI_KEY`.

## Verification

**Commands:**
- `./gradlew :server:test` -- expected: story 3.1 bearer and edition tests still pass, and the clock rows pass.
- `./gradlew :core:jvmTest --tests hu.toliver.matutine.edition.LocalEditionRepositoryTest` -- expected: existing `runMorning` tests still pass.


Review content: the unified diff at `diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
index 860caa7..26ce30d 100644
--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
+++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
@@ -29,7 +29,7 @@
 # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
 # - Retrospective appends its action items to action_items; the status view surfaces open ones
 generated: 11-07-2026 19:42
-last_updated: 11-10-2026 18:48
+last_updated: 11-10-2026 19:55
 project: Matutine
 project_key: NOKEY
 tracking_system: file-system
@@ -59,9 +59,9 @@ development_status:
   2-7-write-the-briefing-with-a-gemini-key: done
   epic-2-retrospective: optional
 
-  epic-3: backlog
-  3-1-open-the-server-s-paper-in-the-browser: backlog
-  3-2-generate-the-morning-edition-on-the-server-clock: backlog
+  epic-3: in-progress
+  3-1-open-the-server-s-paper-in-the-browser: in-progress
+  3-2-generate-the-morning-edition-on-the-server-clock: in-progress
   3-3-configure-the-server-in-admin: backlog
   3-4-light-edit-a-block-in-the-browser: backlog
   3-5-read-the-server-edition-on-android: backlog
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
index c06cc9b..e418217 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
@@ -72,6 +72,7 @@ fun EditionScreen(
     onNewEditionNow: () -> Unit = {},
     onOpenExtra: (String) -> Unit = {},
     onOpenMorning: () -> Unit = {},
+    readOnly: Boolean = false,
     modifier: Modifier = Modifier,
 ) {
     val typography = LocalMatutineEditionTypography.current
@@ -82,6 +83,7 @@ fun EditionScreen(
             generating = state.generating,
             notice = state.notice,
             onOpenSettings = onOpenSettings,
+            readOnly = readOnly,
             typography = typography,
             modifier = modifier,
         )
@@ -104,6 +106,7 @@ fun EditionScreen(
             onNewEditionNow = onNewEditionNow,
             onOpenExtra = onOpenExtra,
             onOpenMorning = onOpenMorning,
+            readOnly = readOnly,
             typography = typography,
             modifier = modifier,
         )
@@ -175,6 +178,7 @@ private fun EmptyFrontPage(
     generating: Boolean,
     notice: String?,
     onOpenSettings: () -> Unit,
+    readOnly: Boolean,
     typography: MatutineEditionTypography,
     modifier: Modifier = Modifier,
 ) {
@@ -188,11 +192,13 @@ private fun EmptyFrontPage(
             .padding(horizontal = 16.dp, vertical = 16.dp),
         verticalArrangement = Arrangement.spacedBy(16.dp),
     ) {
-        TextButton(
-            onClick = onOpenSettings,
-            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
-        ) {
-            Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
+        if (!readOnly) {
+            TextButton(
+                onClick = onOpenSettings,
+                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+            ) {
+                Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
+            }
         }
         Masthead(name = "Matutine", dateLine = "", typography = typography)
         if (generating) {
@@ -229,6 +235,7 @@ private fun EditionContent(
     onNewEditionNow: () -> Unit,
     onOpenExtra: (String) -> Unit,
     onOpenMorning: () -> Unit,
+    readOnly: Boolean,
     typography: MatutineEditionTypography,
     modifier: Modifier = Modifier,
 ) {
@@ -248,29 +255,31 @@ private fun EditionContent(
                 .padding(horizontal = 16.dp, vertical = 16.dp),
             verticalArrangement = Arrangement.spacedBy(16.dp),
         ) {
-            Row(
-                horizontalArrangement = Arrangement.spacedBy(16.dp),
-                verticalAlignment = Alignment.CenterVertically,
-            ) {
-                TextButton(
-                    onClick = onOpenSettings,
-                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+            if (!readOnly) {
+                Row(
+                    horizontalArrangement = Arrangement.spacedBy(16.dp),
+                    verticalAlignment = Alignment.CenterVertically,
                 ) {
-                    Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
-                }
-                if (onOpenTypewriter != null) {
                     TextButton(
-                        onClick = onOpenTypewriter,
+                        onClick = onOpenSettings,
                         modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                     ) {
-                        Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                        Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                    }
+                    if (onOpenTypewriter != null) {
+                        TextButton(
+                            onClick = onOpenTypewriter,
+                            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                        ) {
+                            Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                        }
+                    }
+                    TextButton(
+                        onClick = onNewEditionNow,
+                        modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                    ) {
+                        Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
                     }
-                }
-                TextButton(
-                    onClick = onNewEditionNow,
-                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
-                ) {
-                    Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
                 }
             }
             if (edition.role == EditionRole.extra) {
@@ -316,10 +325,10 @@ private fun EditionContent(
             }
             if (edition.isSample) {
                 Text(
-                    text = "SAMPLE EDITION",
+                    text = EditionTranscript.SAMPLE,
                     style = typography.kicker.copy(color = EditionThemeColors.current.accent),
                 )
-            } else if (showNoTopicLine) {
+            } else if (showNoTopicLine && !readOnly) {
                 TextButton(
                     onClick = onOpenInterests,
                     modifier = Modifier.heightIn(min = chromeControlMinHeight()),
@@ -339,6 +348,7 @@ private fun EditionContent(
                     onAddTask = onAddTask,
                     calendarPermissionOff = calendarPermissionOff,
                     onOpenCalendarPermission = onOpenCalendarPermission,
+                    readOnly = readOnly,
                     typography = typography,
                 )
             } else {
@@ -351,6 +361,7 @@ private fun EditionContent(
                     taskSaved = taskSaved,
                     onToggleTask = onToggleTask,
                     onAddTask = onAddTask,
+                    readOnly = readOnly,
                     typography = typography,
                 )
             }
@@ -366,6 +377,7 @@ private fun WideEditionBody(
     onAddTask: (String) -> Unit,
     calendarPermissionOff: Boolean,
     onOpenCalendarPermission: () -> Unit,
+    readOnly: Boolean,
     typography: MatutineEditionTypography,
 ) {
     Spacer(Modifier.height(8.dp))
@@ -395,6 +407,7 @@ private fun WideEditionBody(
                 taskSaved = taskSaved,
                 onToggleTask = onToggleTask,
                 onAddTask = onAddTask,
+                readOnly = readOnly,
                 typography = typography,
             )
         }
@@ -562,6 +575,7 @@ private fun TaskBlock(
     taskSaved: Boolean,
     onToggleTask: (Int) -> Unit,
     onAddTask: (String) -> Unit,
+    readOnly: Boolean,
     typography: MatutineEditionTypography,
 ) {
     var draft by remember { mutableStateOf("") }
@@ -571,18 +585,25 @@ private fun TaskBlock(
             Text(text = EditionCopy.NO_TASKS, style = typography.body)
         } else {
             tasks.forEachIndexed { index, task ->
-                TaskRow(task, typography, onToggle = { onToggleTask(index) })
+                TaskRow(
+                    task,
+                    typography,
+                    onToggle = { onToggleTask(index) },
+                    readOnly = readOnly,
+                )
             }
         }
-        TaskAddLine(
-            value = draft,
-            onValueChange = { draft = it },
-            onConfirm = {
-                onAddTask(draft)
-                draft = ""
-            },
-            typography = typography,
-        )
+        if (!readOnly) {
+            TaskAddLine(
+                value = draft,
+                onValueChange = { draft = it },
+                onConfirm = {
+                    onAddTask(draft)
+                    draft = ""
+                },
+                typography = typography,
+            )
+        }
         if (taskSaved) {
             Text(text = EditionCopy.SAVED, style = typography.meta)
         }
@@ -624,6 +645,7 @@ private fun TaskRow(
     task: EditionTask,
     typography: MatutineEditionTypography,
     onToggle: () -> Unit,
+    readOnly: Boolean,
 ) {
     val touchTarget = if (editionForceSingleColumn()) {
         Modifier.minimumInteractiveComponentSize()
@@ -636,10 +658,16 @@ private fun TaskRow(
         modifier = Modifier
             .padding(vertical = 4.dp)
             .then(touchTarget)
-            .clickable(
-                interactionSource = remember { MutableInteractionSource() },
-                indication = null,
-                onClick = onToggle,
+            .then(
+                if (readOnly) {
+                    Modifier
+                } else {
+                    Modifier.clickable(
+                        interactionSource = remember { MutableInteractionSource() },
+                        indication = null,
+                        onClick = onToggle,
+                    )
+                },
             ),
     ) {
         Box(
diff --git a/app/webApp/build.gradle.kts b/app/webApp/build.gradle.kts
index 4d8b955..6bbc79c 100644
--- a/app/webApp/build.gradle.kts
+++ b/app/webApp/build.gradle.kts
@@ -22,7 +22,11 @@ kotlin {
         commonMain.dependencies {
             implementation(project(":app:shared"))
 
+            implementation(libs.compose.runtime)
+            implementation(libs.compose.foundation)
+            implementation(libs.compose.material3)
             implementation(libs.compose.ui)
+            implementation(libs.kotlinx.coroutines.core)
         }
     }
 }
\ No newline at end of file
diff --git a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
index f0b181a..e9da8b9 100644
--- a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
+++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
@@ -1,11 +1,142 @@
 package hu.toliver.matutine
 
+import androidx.compose.foundation.background
+import androidx.compose.foundation.layout.Arrangement
+import androidx.compose.foundation.layout.Column
+import androidx.compose.foundation.layout.fillMaxSize
+import androidx.compose.foundation.layout.fillMaxWidth
+import androidx.compose.foundation.layout.heightIn
+import androidx.compose.foundation.layout.padding
+import androidx.compose.foundation.shape.RoundedCornerShape
+import androidx.compose.material3.Button
+import androidx.compose.material3.ButtonDefaults
+import androidx.compose.material3.OutlinedTextField
+import androidx.compose.material3.Text
+import androidx.compose.runtime.Composable
+import androidx.compose.runtime.getValue
+import androidx.compose.runtime.mutableStateOf
+import androidx.compose.runtime.remember
+import androidx.compose.runtime.rememberCoroutineScope
+import androidx.compose.runtime.setValue
 import androidx.compose.ui.ExperimentalComposeUiApi
+import androidx.compose.ui.Modifier
+import androidx.compose.ui.unit.dp
 import androidx.compose.ui.window.ComposeViewport
+import hu.toliver.matutine.edition.Edition
+import hu.toliver.matutine.edition.EditionScreen
+import hu.toliver.matutine.edition.EditionUiState
+import hu.toliver.matutine.edition.decodeEdition
+import hu.toliver.matutine.edition.webPaperGate
+import hu.toliver.matutine.theme.EditionThemeColors
+import hu.toliver.matutine.theme.LocalMatutineEditionTypography
+import hu.toliver.matutine.theme.MatutineTheme
+import hu.toliver.matutine.theme.NewsprintColors
+import kotlinx.coroutines.launch
 
 @OptIn(ExperimentalComposeUiApi::class)
 fun main() {
     ComposeViewport {
-        App()
+        ServerPaperApp()
     }
-}
\ No newline at end of file
+}
+
+@Composable
+fun ServerPaperApp() {
+    var typedToken by remember { mutableStateOf("") }
+    var message by remember { mutableStateOf<String?>(null) }
+    var edition by remember { mutableStateOf<Edition?>(null) }
+    var submitting by remember { mutableStateOf(false) }
+    val scope = rememberCoroutineScope()
+    MatutineTheme {
+        val paper = edition
+        if (paper == null) {
+            TokenGate(
+                token = typedToken,
+                onTokenChange = { typedToken = it },
+                message = message,
+                submitting = submitting,
+                onSubmit = {
+                    if (submitting) return@TokenGate
+                    submitting = true
+                    scope.launch {
+                        val decision = try {
+                            val response = fetchCurrentEdition(typedToken)
+                            val gate = webPaperGate(typedToken, response.status)
+                            typedToken = gate.typedToken
+                            message = gate.message
+                            if (gate.showEdition) {
+                                decodeEdition(response.body)
+                            } else {
+                                null
+                            }
+                        } catch (_: Throwable) {
+                            message = null
+                            null
+                        }
+                        edition = decision
+                        submitting = false
+                    }
+                },
+            )
+        } else {
+            EditionScreen(
+                state = EditionUiState.Ready(paper),
+                onRetrySample = {},
+                onOpenSettings = {},
+                showNoTopicLine = false,
+                onOpenInterests = {},
+                onToggleTask = {},
+                onAddTask = {},
+                readOnly = true,
+            )
+        }
+    }
+}
+
+@Composable
+private fun TokenGate(
+    token: String,
+    onTokenChange: (String) -> Unit,
+    message: String?,
+    submitting: Boolean,
+    onSubmit: () -> Unit,
+) {
+    val typography = LocalMatutineEditionTypography.current
+    Column(
+        modifier = Modifier
+            .fillMaxSize()
+            .background(NewsprintColors.chrome)
+            .padding(24.dp),
+        verticalArrangement = Arrangement.spacedBy(16.dp),
+    ) {
+        Text(text = "Token", style = typography.body.copy(color = EditionThemeColors.current.ink))
+        OutlinedTextField(
+            value = token,
+            onValueChange = onTokenChange,
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = 40.dp),
+            singleLine = true,
+            textStyle = typography.body.copy(color = EditionThemeColors.current.ink),
+            shape = RoundedCornerShape(2.dp),
+        )
+        if (!message.isNullOrBlank()) {
+            Text(
+                text = message,
+                style = typography.body.copy(color = EditionThemeColors.current.error),
+            )
+        }
+        Button(
+            onClick = onSubmit,
+            enabled = !submitting,
+            modifier = Modifier.heightIn(min = 40.dp),
+            shape = RoundedCornerShape(2.dp),
+            colors = ButtonDefaults.buttonColors(
+                containerColor = EditionThemeColors.current.ink,
+                contentColor = EditionThemeColors.current.paper,
+            ),
+        ) {
+            Text(text = "Continue")
+        }
+    }
+}
diff --git a/server/build.gradle.kts b/server/build.gradle.kts
index bbd6ccd..ca8fa7c 100644
--- a/server/build.gradle.kts
+++ b/server/build.gradle.kts
@@ -11,9 +11,21 @@ application {
 
 dependencies {
     api(project(":core"))
+    implementation(libs.kotlinx.coroutines.core)
     implementation(libs.logback)
     implementation(libs.ktor.serverCore)
     implementation(libs.ktor.serverNetty)
     testImplementation(libs.ktor.serverTestHost)
     testImplementation(libs.kotlin.testJunit)
+}
+
+val webDistribution = project(":app:webApp").layout.buildDirectory.dir(
+    "dist/wasmJs/productionExecutable",
+)
+
+tasks.named<ProcessResources>("processResources") {
+    dependsOn(":app:webApp:wasmJsBrowserDistribution")
+    from(webDistribution) {
+        into("web")
+    }
 }
\ No newline at end of file
diff --git a/server/src/main/kotlin/hu/toliver/matutine/Application.kt b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
index 25b8388..381bdaf 100644
--- a/server/src/main/kotlin/hu/toliver/matutine/Application.kt
+++ b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
@@ -1,10 +1,48 @@
 package hu.toliver.matutine
 
-import io.ktor.server.application.*
-import io.ktor.server.engine.*
-import io.ktor.server.netty.*
-import io.ktor.server.response.*
-import io.ktor.server.routing.*
+import hu.toliver.matutine.edition.EditionStore
+import hu.toliver.matutine.edition.EditionStory
+import hu.toliver.matutine.edition.encodeEdition
+import io.ktor.http.ContentType
+import io.ktor.http.HttpStatusCode
+import io.ktor.server.application.Application
+import io.ktor.server.application.ApplicationStopped
+import io.ktor.server.application.call
+import io.ktor.server.engine.embeddedServer
+import io.ktor.server.netty.Netty
+import io.ktor.server.response.respondText
+import io.ktor.server.routing.get
+import io.ktor.server.routing.routing
+import io.ktor.server.http.content.staticResources
+import io.ktor.util.AttributeKey
+import kotlin.time.Clock
+import kotlin.time.Instant
+import kotlinx.coroutines.launch
+import kotlinx.datetime.TimeZone
+
+private val SharedTokenKey = AttributeKey<String>("matutine.sharedToken")
+private val DocumentStoreKey = AttributeKey<EditionStore>("matutine.documents")
+private val ZoneKey = AttributeKey<TimeZone>("matutine.zone")
+private val GenerationRawKey = AttributeKey<String>("matutine.generationTime")
+private val GeminiKeyKey = AttributeKey<String>("matutine.geminiKey")
+private val ClockKey = AttributeKey<() -> Instant>("matutine.clock")
+private val PortsKey = AttributeKey<ServerPorts>("matutine.ports")
+
+internal class ServerPorts(
+    val readFeed: (suspend (String) -> String)? = null,
+    val summarize: (suspend (List<EditionStory>, String) -> String)? = null,
+)
+
+fun resolveSharedToken(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "matutine-demo"
+
+fun resolveDataRoot(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "/data"
+
+internal fun bearerMatches(authorization: String?, token: String): Boolean {
+    val prefix = "Bearer "
+    if (authorization == null || !authorization.startsWith(prefix)) return false
+    val presented = authorization.substring(prefix.length)
+    return presented.isNotEmpty() && presented == token
+}
 
 fun main() {
     embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
@@ -12,9 +50,84 @@ fun main() {
 }
 
 fun Application.module() {
+    val token = attributes.getOrNull(SharedTokenKey)
+        ?: resolveSharedToken(System.getenv("MATUTINE_TOKEN"))
+    val documents = attributes.getOrNull(DocumentStoreKey)
+        ?: DataDirectoryStore(resolveDataRoot(System.getenv("MATUTINE_DATA")))
+    val zone = attributes.getOrNull(ZoneKey)
+        ?: resolveServerTimeZone(System.getenv("MATUTINE_TIMEZONE"))
+    val generation = resolveGenerationTime(
+        if (attributes.contains(GenerationRawKey)) {
+            attributes[GenerationRawKey]
+        } else {
+            System.getenv("MATUTINE_GENERATION_TIME")
+        },
+    )
+    val geminiKey = attributes.getOrNull(GeminiKeyKey) ?: System.getenv("MATUTINE_GEMINI_KEY").orEmpty()
+    val clock = attributes.getOrNull(ClockKey) ?: { Clock.System.now() }
+    val ports = attributes.getOrNull(PortsKey)
+    val readFeed = ports?.readFeed ?: ::serverReadFeed
+    val summarize = ports?.summarize ?: ::serverSummarize
+    val repository = serverEditionRepository(
+        documents = documents,
+        zone = zone,
+        clock = clock,
+        geminiKey = geminiKey,
+        readFeed = readFeed,
+        summarize = summarize,
+    )
+    if (generation != null) {
+        val (hour, minute) = generation
+        val morning = ServerMorningClock(
+            zone = zone,
+            hour = hour,
+            minute = minute,
+            clock = clock,
+            insert = { insertScheduledMorning(documents, repository) },
+        )
+        val job = launch { morning.run() }
+        monitor.subscribe(ApplicationStopped) { job.cancel() }
+    }
     routing {
-        get("/") {
-            call.respondText(sayHello("Ktor"))
+        get("/editions/current") {
+            val authorization = call.request.headers["Authorization"]
+            if (!bearerMatches(authorization, token)) {
+                call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
+                return@get
+            }
+            val edition = repository.loadCurrentMorningEdition().getOrElse { error ->
+                call.respondText(
+                    error.message ?: "unavailable",
+                    status = HttpStatusCode.NotFound,
+                )
+                return@get
+            }
+            call.respondText(encodeEdition(edition), ContentType.Application.Json)
+        }
+        staticResources("/", "web") {
+            default("index.html")
         }
     }
-}
\ No newline at end of file
+}
+
+fun Application.installTestDocuments(token: String, documents: EditionStore) {
+    attributes.put(SharedTokenKey, token)
+    attributes.put(DocumentStoreKey, documents)
+}
+
+fun Application.installServerClock(
+    zone: TimeZone,
+    generationTime: String,
+    geminiKey: String = "",
+    clock: () -> Instant,
+    readFeed: (suspend (String) -> String)? = null,
+    summarize: (suspend (List<EditionStory>, String) -> String)? = null,
+) {
+    attributes.put(ZoneKey, zone)
+    attributes.put(GenerationRawKey, generationTime)
+    attributes.put(GeminiKeyKey, geminiKey)
+    attributes.put(ClockKey, clock)
+    if (readFeed != null || summarize != null) {
+        attributes.put(PortsKey, ServerPorts(readFeed, summarize))
+    }
+}
diff --git a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
index 23dc320..267f7fe 100644
--- a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+++ b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
@@ -1,20 +1,420 @@
 package hu.toliver.matutine
 
-import io.ktor.client.request.*
-import io.ktor.client.statement.*
-import io.ktor.http.*
-import io.ktor.server.testing.*
-import kotlin.test.*
+import hu.toliver.matutine.edition.CalendarSnapshot
+import hu.toliver.matutine.edition.DEFAULT_NOTIFICATION_TIME
+import hu.toliver.matutine.edition.Edition
+import hu.toliver.matutine.edition.EditionRole
+import hu.toliver.matutine.edition.EditionStore
+import hu.toliver.matutine.edition.EditionStory
+import hu.toliver.matutine.edition.EditionTranscript
+import hu.toliver.matutine.edition.FEED_DIDNT_LOAD
+import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SourceList
+import hu.toliver.matutine.edition.decodeEdition
+import hu.toliver.matutine.edition.decodeReaderSettings
+import hu.toliver.matutine.edition.parseNotificationTime
+import hu.toliver.matutine.edition.todayLocalDate
+import hu.toliver.matutine.edition.toDayKey
+import io.ktor.client.request.get
+import io.ktor.client.request.header
+import io.ktor.client.statement.bodyAsText
+import io.ktor.http.HttpStatusCode
+import io.ktor.server.testing.testApplication
+import java.io.File
+import java.nio.file.Files
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertNull
+import kotlin.test.assertTrue
+import kotlin.time.Instant
+import kotlinx.coroutines.delay
+import kotlinx.coroutines.runBlocking
+import kotlinx.datetime.TimeZone
+import kotlinx.datetime.toLocalDateTime
 
 class ApplicationTest {
 
     @Test
-    fun testRoot() = testApplication {
+    fun unsetToken_acceptsTheDemoBearer() = testApplication {
+        val root = Files.createTempDirectory("matutine-demo").toFile()
         application {
+            installTestDocuments(resolveSharedToken(null), DataDirectoryStore(root.absolutePath))
             module()
         }
-        val response = client.get("/")
+        val response = client.get("/editions/current") {
+            header("Authorization", "Bearer matutine-demo")
+        }
         assertEquals(HttpStatusCode.OK, response.status)
-        assertEquals("Hello, Ktor!", response.bodyAsText())
+        assertTrue(decodeEdition(response.bodyAsText()).isSample)
+    }
+
+    @Test
+    fun blankToken_isTheDemoBearer() {
+        assertEquals("matutine-demo", resolveSharedToken(null))
+        assertEquals("matutine-demo", resolveSharedToken(""))
+        assertEquals("matutine-demo", resolveSharedToken("   "))
+        assertEquals("/data", resolveDataRoot(null))
+        assertEquals("/data", resolveDataRoot(""))
     }
-}
\ No newline at end of file
+
+    @Test
+    fun setToken_rejectsEveryOtherBearerBeforeReadingDocuments() = testApplication {
+        val store = TouchingStore()
+        application {
+            installTestDocuments(resolveSharedToken("lan-secret"), store)
+            module()
+        }
+        val missing = client.get("/editions/current")
+        val wrongScheme = client.get("/editions/current") {
+            header("Authorization", "Token lan-secret")
+        }
+        val wrongToken = client.get("/editions/current") {
+            header("Authorization", "Bearer matutine-demo")
+        }
+        assertEquals(HttpStatusCode.Unauthorized, missing.status)
+        assertEquals(HttpStatusCode.Unauthorized, wrongScheme.status)
+        assertEquals(HttpStatusCode.Unauthorized, wrongToken.status)
+        assertFalse(missing.bodyAsText().contains("isSample"))
+        assertFalse(wrongToken.bodyAsText().contains("mastheadName"))
+        assertEquals(0, store.touches)
+    }
+
+    @Test
+    fun setToken_acceptsThatBearer() = testApplication {
+        val root = Files.createTempDirectory("matutine-set-token").toFile()
+        application {
+            installTestDocuments(resolveSharedToken("lan-secret"), DataDirectoryStore(root.absolutePath))
+            module()
+        }
+        val accepted = client.get("/editions/current") {
+            header("Authorization", "Bearer lan-secret")
+        }
+        assertEquals(HttpStatusCode.OK, accepted.status)
+        assertTrue(decodeEdition(accepted.bodyAsText()).isSample)
+    }
+
+    @Test
+    fun absentSourceList_returnsTheServerSampleAndDoesNotWriteTheList() = testApplication {
+        val root = Files.createTempDirectory("matutine-sample").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        application {
+            installTestDocuments("matutine-demo", store)
+            module()
+        }
+        val response = client.get("/editions/current") {
+            header("Authorization", "Bearer matutine-demo")
+        }
+        val edition = decodeEdition(response.bodyAsText())
+        assertEquals(HttpStatusCode.OK, response.status)
+        assertTrue(edition.isSample)
+        assertTrue(EditionTranscript.render(edition).contains(EditionTranscript.SAMPLE))
+        assertNull(store.readSourceList())
+        assertFalse(root.resolve("sources.json").exists())
+        assertTrue(root.resolve("editions/${edition.dayKey}-morning.json").exists())
+        val again = decodeEdition(
+            client.get("/editions/current") {
+                header("Authorization", "Bearer matutine-demo")
+            }.bodyAsText(),
+        )
+        assertEquals(edition.id, again.id)
+        assertEquals(edition.revision, again.revision)
+    }
+
+    @Test
+    fun existingMorning_isReturnedUnchanged() = testApplication {
+        val root = Files.createTempDirectory("matutine-kept").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        val day = todayLocalDate()
+        val morning = Edition(
+            id = "morning-${day.toDayKey()}",
+            revision = 4,
+            dayKey = day.toDayKey(),
+            role = EditionRole.morning,
+            mastheadName = "Matutine",
+            dateLine = "Kept date",
+            isSample = false,
+            briefing = "Kept by hand",
+            stories = emptyList(),
+            events = emptyList(),
+            tasks = emptyList(),
+        )
+        store.saveMorningEdition(morning)
+        application {
+            installTestDocuments("matutine-demo", store)
+            module()
+        }
+        val response = client.get("/editions/current") {
+            header("Authorization", "Bearer matutine-demo")
+        }
+        val edition = decodeEdition(response.bodyAsText())
+        assertEquals(morning, edition)
+        assertEquals(morning, store.loadMorningEdition(day.toDayKey()))
+    }
+
+    @Test
+    fun documents_areFilesUnderTheDataDirectory() {
+        val root = Files.createTempDirectory("matutine-docs").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        store.writeSettings(ReaderSettings())
+        store.writeSourceList(SourceList(id = "sources", revision = 1))
+        store.writeCalendarSnapshot(CalendarSnapshot())
+        val day = todayLocalDate()
+        store.saveMorningEdition(
+            Edition(
+                id = "morning-${day.toDayKey()}",
+                revision = 1,
+                dayKey = day.toDayKey(),
+                role = EditionRole.morning,
+                mastheadName = "Matutine",
+                dateLine = "Date",
+                isSample = false,
+                briefing = "Brief",
+                stories = emptyList(),
+                events = emptyList(),
+                tasks = emptyList(),
+            ),
+        )
+        assertTrue(root.resolve("settings.json").isFile)
+        assertTrue(root.resolve("sources.json").isFile)
+        assertTrue(root.resolve("calendar-snapshot.json").isFile)
+        assertTrue(root.resolve("editions/${day.toDayKey()}-morning.json").isFile)
+    }
+
+    @Test
+    fun blankGenerationTime_isSevenInTheProcessZone() {
+        assertEquals(parseNotificationTime(DEFAULT_NOTIFICATION_TIME), resolveGenerationTime(null))
+        assertEquals(parseNotificationTime(DEFAULT_NOTIFICATION_TIME), resolveGenerationTime("  "))
+        assertNull(resolveGenerationTime("morning"))
+        assertEquals(TimeZone.of(KIRITIMATI), resolveServerTimeZone(KIRITIMATI))
+        assertEquals(TimeZone.currentSystemDefault(), resolveServerTimeZone("  "))
+    }
+
+    @Test
+    fun dueMinute_absentList_insertsTheGeneratorDayKey() = runBlocking {
+        val root = Files.createTempDirectory("matutine-due").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        val clock = morningClock(store, DUE)
+        clock.tick()
+        val edition = decodeEdition(morningFile(root).readText())
+        val zone = resolveServerTimeZone(KIRITIMATI)
+        assertEquals(DUE.toLocalDateTime(zone).date.toDayKey(), edition.dayKey)
+        assertEquals("2026-11-11", edition.dayKey)
+        assertEquals("sample-2026-11-11", edition.id)
+        assertTrue(edition.isSample)
+        assertNull(store.readSourceList())
+    }
+
+    @Test
+    fun existingMorning_isNotReplacedWhenTheClockFires() = runBlocking {
+        val root = Files.createTempDirectory("matutine-keep-clock").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        val dayKey = DUE.toLocalDateTime(resolveServerTimeZone(KIRITIMATI)).date.toDayKey()
+        store.writeSourceList(SourceList(feeds = listOf("https://example.com/rss.xml")))
+        store.saveMorningEdition(
+            Edition(
+                id = "morning-$dayKey",
+                revision = 4,
+                dayKey = dayKey,
+                role = EditionRole.morning,
+                mastheadName = "Matutine",
+                dateLine = "Kept date",
+                isSample = false,
+                briefing = "Kept by hand",
+                stories = emptyList(),
+                events = emptyList(),
+                tasks = emptyList(),
+            ),
+        )
+        val before = morningFile(root).readText()
+        var reads = 0
+        val clock = morningClock(store, DUE, readFeed = {
+            reads += 1
+            PERSONAL_RSS
+        })
+        clock.tick()
+        assertEquals(0, reads)
+        assertEquals(before, morningFile(root).readText())
+        assertEquals("Kept by hand", store.loadMorningEdition(dayKey)?.briefing)
+    }
+
+    @Test
+    fun secondFire_doesNotWriteOrReadSources() = runBlocking {
+        val root = Files.createTempDirectory("matutine-second").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        store.writeSourceList(SourceList(feeds = listOf("https://example.com/rss.xml")))
+        var reads = 0
+        val clock = morningClock(store, DUE, readFeed = {
+            reads += 1
+            PERSONAL_RSS
+        })
+        clock.tick()
+        val written = morningFile(root).readText()
+        assertEquals(1, reads)
+        clock.tick()
+        assertEquals(1, reads)
+        assertEquals(written, morningFile(root).readText())
+    }
+
+    @Test
+    fun beforeTheMinute_doesNotInsert() = runBlocking {
+        val root = Files.createTempDirectory("matutine-before").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        morningClock(store, BEFORE).tick()
+        assertFalse(morningFile(root).exists())
+    }
+
+    @Test
+    fun afterTheMinute_doesNotCatchUp() = runBlocking {
+        val root = Files.createTempDirectory("matutine-after").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        morningClock(store, AFTER).tick()
+        assertFalse(morningFile(root).exists())
+    }
+
+    @Test
+    fun emptyOrUnreadableList_leavesTheStoreUnchanged() = runBlocking {
+        listOf(SourceList(), SourceList(feeds = listOf("https://example.com/bad.xml"))).forEach { list ->
+            val root = Files.createTempDirectory("matutine-bad-list").toFile()
+            val store = DataDirectoryStore(root.absolutePath)
+            store.writeSourceList(list)
+            val before = root.resolve("sources.json").readText()
+            morningClock(store, DUE, readFeed = { error(FEED_DIDNT_LOAD) }).tick()
+            assertFalse(morningFile(root).exists())
+            assertEquals(before, root.resolve("sources.json").readText())
+        }
+    }
+
+    @Test
+    fun geminiKey_isUsedInMemoryAndNotWritten() = runBlocking {
+        val root = Files.createTempDirectory("matutine-key").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        store.writeSettings(ReaderSettings(topics = listOf("ports")))
+        store.writeSourceList(SourceList(feeds = listOf("https://example.com/rss.xml")))
+        var seen = ""
+        val clock = morningClock(
+            store,
+            DUE,
+            geminiKey = "server-key",
+            readFeed = { PERSONAL_RSS },
+            summarize = { _, key ->
+                seen = key
+                "From the model."
+            },
+        )
+        clock.tick()
+        assertEquals("server-key", seen)
+        assertEquals("From the model.", store.loadMorningEdition("2026-11-11")?.briefing)
+        val settings = decodeReaderSettings(root.resolve("settings.json").readText())
+        assertEquals("", settings.geminiKey)
+        assertEquals(listOf("ports"), settings.topics)
+        assertFalse(root.resolve("settings.json").readText().contains("server-key"))
+    }
+
+    @Test
+    fun stoppedProcess_doesNotInsert() = runBlocking {
+        val root = Files.createTempDirectory("matutine-stopped").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        morningClock(store, DUE)
+        assertFalse(morningFile(root).exists())
+    }
+
+    @Test
+    fun runningServer_insertsOnTheGenerationMinute() = testApplication {
+        val root = Files.createTempDirectory("matutine-running").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        application {
+            installTestDocuments("matutine-demo", store)
+            installServerClock(
+                zone = resolveServerTimeZone(KIRITIMATI),
+                generationTime = "04:00",
+                clock = { DUE },
+            )
+            module()
+        }
+        startApplication()
+        val file = morningFile(root)
+        val deadline = System.nanoTime() + 3_000_000_000L
+        while (!file.exists() && System.nanoTime() < deadline) {
+            delay(20)
+        }
+        assertTrue(file.exists())
+        val edition = decodeEdition(file.readText())
+        assertEquals("2026-11-11", edition.dayKey)
+        val response = client.get("/editions/current") {
+            header("Authorization", "Bearer matutine-demo")
+        }
+        val served = decodeEdition(response.bodyAsText())
+        assertEquals(edition, served)
+        assertEquals(1, served.revision)
+    }
+}
+
+private const val KIRITIMATI = "Pacific/Kiritimati"
+private val DUE: Instant = Instant.parse("2026-11-10T14:00:00Z")
+private val BEFORE: Instant = Instant.parse("2026-11-10T13:59:00Z")
+private val AFTER: Instant = Instant.parse("2026-11-10T14:01:00Z")
+
+private const val PERSONAL_RSS = """
+<?xml version="1.0"?>
+<rss version="2.0">
+  <channel>
+    <title>Personal Wire</title>
+    <item>
+      <title>Markets open higher</title>
+      <link>https://example.com/lead</link>
+      <description>Traders watched the open.</description>
+    </item>
+  </channel>
+</rss>
+"""
+
+private fun morningFile(root: File): File = root.resolve("editions/2026-11-11-morning.json")
+
+private fun morningClock(
+    store: EditionStore,
+    instant: Instant,
+    geminiKey: String = "",
+    readFeed: suspend (String) -> String = { error(FEED_DIDNT_LOAD) },
+    summarize: suspend (List<EditionStory>, String) -> String = { _, _ -> error("summarizer_unavailable") },
+): ServerMorningClock {
+    val zone = resolveServerTimeZone(KIRITIMATI)
+    val (hour, minute) = requireNotNull(resolveGenerationTime("04:00"))
+    val repository = serverEditionRepository(
+        documents = store,
+        zone = zone,
+        clock = { instant },
+        geminiKey = geminiKey,
+        readFeed = readFeed,
+        summarize = summarize,
+    )
+    return ServerMorningClock(
+        zone = zone,
+        hour = hour,
+        minute = minute,
+        clock = { instant },
+        insert = { insertScheduledMorning(store, repository) },
+    )
+}
+
+private class TouchingStore : EditionStore {
+    var touches: Int = 0
+
+    private fun touch(): Nothing {
+        touches += 1
+        error("store touched")
+    }
+
+    override fun readSourceList() = touch()
+    override fun writeSourceList(sourceList: SourceList) = touch()
+    override fun readSettings() = touch()
+    override fun writeSettings(settings: ReaderSettings) = touch()
+    override fun loadMorningEdition(dayKey: String) = touch()
+    override fun saveMorningEdition(edition: Edition) = touch()
+    override fun loadExtras(dayKey: String) = touch()
+    override fun saveExtra(edition: Edition) = touch()
+    override fun readCalendarSnapshot() = touch()
+    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = touch()
+    override fun readCalDavAccount() = touch()
+    override fun writeCalDavAccount(account: hu.toliver.matutine.edition.CalDavAccount) = touch()
+}
diff --git a/.dockerignore b/.dockerignore
new file mode 100644
--- /dev/null
+++ b/.dockerignore
+.git
+.gradle
+**/.gradle
+**/build
+.idea
+_bmad
+_bmad-output
+*.md
+!README.md
diff --git a/_bmad-output/implementation-artifacts/epic-3-context.md b/_bmad-output/implementation-artifacts/epic-3-context.md
new file mode 100644
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/epic-3-context.md
+# Epic 3 Context: Read the server's morning edition
+
+<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->
+
+## Goal
+
+O runs one Docker server on the LAN and reads that same morning edition in the browser and on the phone. Admin is where the server is configured. Local papers are not uploaded when a client points at the server. The edit-survives-regenerate rule still applies to editions the server owns.
+
+## Stories
+
+- Story 3.1: Open the server's paper in the browser
+- Story 3.2: Generate the morning edition on the server clock
+- Story 3.3: Configure the server in Admin
+- Story 3.4: Light-edit a block in the browser
+- Story 3.5: Read the server edition on Android
+- Story 3.6: Read the server edition on Desktop
+- Story 3.7: Document the server judge path
+- Story 3.8: Publish the Docker image
+
+## Requirements & Constraints
+
+- One shared token, no accounts, no push. Unset token is the literal `matutine-demo` on the server and on every remote client. The token is required before the newspaper or Admin renders. A mismatch says "That token doesn't match." and keeps the typed value. The same token covers the newspaper and Admin. Every remote call sends `Authorization: Bearer`.
+- The shipped server is a Docker image running the Ktor process. Token, AI key, and timezone come from the process environment, not the image. Edition, settings, source list, and calendar snapshot live in a host directory mounted at `/data`, not in the image layer. Do not document a public port. `java -jar` is not a required launch.
+- When the source-list document is absent, the generator builds an edition from bundled sample feeds and sample calendar data, shows the sample line, and does not write those samples into the list. An empty list is a real configuration and does not fall back to samples. No account and no AI key are required to see a paper.
+- The web client does not run the generator and is not an offline app. Web has no Settings screen. Admin is the web-only place for feeds, ICS, CalDAV, generation time, timezone, interest questions, AI provider and key, and the shared token.
+- The server inserts today's morning edition at the configured time only when that document is absent. It does not replace an existing morning edition. A clock-precise paper is claimed only while this server is running. The server holds the AI key; remote clients do not. The timezone value is whatever the process was given; do not invent an identifier format.
+- Saving a server base URL and token does not upload local editions, the local source list, or the local calendar snapshot. Remote clients read, edit, regenerate, or request an extra. They do not mint the morning edition. If the server cannot be reached, show the last stored edition and "Couldn't reach the server. Showing the copy on this device." Generate stays available and writes locally. Android accepts a cleartext `http` LAN URL. Notification time, notification text, theme, and layout stay on the phone. Feeds, calendar, and interest questions are not editable on the phone while a server is configured.
+- Web light edit changes one block in place and saves on leave. Last write wins. There is no merge screen and no revision number on the page. A failed save says "Couldn't save. Your text is still here." Changing the printed date does not change the day key.
+- User-owned text on a server-owned edition survives regenerate the same way it does on the device. Whole-document replace stays the write model.
+- Milestone B also publishes the Docker image from the existing release workflow. Secrets stay in the environment.
+
+## Technical Decisions
+
+- Modules stay `shared`, `server`, `androidApp`, `desktopApp`, and `webApp`. `server` owns the scheduler, HTTP API, stores, Docker image, and served web files. `webApp` is Compose Multiplatform for Web, HTTP only, served by the server.
+- Edition documents, settings, the source list, and calendar snapshots use one kotlinx.serialization `Json` configuration with default property names and no naming strategy. `webApp` sends and reads that JSON and does not declare its own model.
+- The server owns these routes, and every remote client uses them: `GET /editions`, `GET /editions/current`, `GET /editions/by-id`, `PUT /editions/by-id`, `POST /editions/morning/regenerate`, `POST /editions`, `GET /settings`, `PUT /settings`, `GET /sources`, `PUT /sources`, `PUT /calendar-snapshot`.
+- Each of those documents is one JSON document. The store assigns the id on insert. The id never changes. Every write gets a new monotonic revision. The latest whole document wins.
+- One source-list document holds RSS/Atom and ICS URLs. The generator reads that list and no other. The server environment writes it only when it is absent.
+- Clients talk only to a repository. With a server base URL, only the server generator writes a new morning paper. Roles stay `morning` and `extra`. The front page is today's morning edition. Regenerate keeps the id. New edition now inserts an extra.
+- Kotlin 2.4.21, Ktor 3.6.1, kotlinx-serialization 1.12.0, Compose Multiplatform 1.12.2, JDK 17. JVM target 17 for `shared`, `server`, and `desktopApp`.
+- Reach is the LAN. One configured base URL. `http` and `https` are the same API with a different scheme. Android network security must allow that URL, including cleartext when the scheme is `http`.
+
+## UX & Interaction Patterns
+
+- The token gate is the first web screen: chrome, one text field, one primary button. No masthead and no edition behind a rejected token. Error copy is "That token doesn't match." The field keeps the typed value.
+- After a valid token, the web Edition is the newspaper. Admin is a separate chrome screen opened from the newspaper, not a Settings screen.
+- Light edit keeps the display type, uses a 1px ink underline, and has no toolbar and no mono face. "Saved." is the last confirmed line.
+- The Edition does not say whether the paper came from the device or the server.
+- Below 960px the web client uses the single-column order. Chrome controls are at least 40px. The edition itself stays the custom paper, not chrome.
+
+## Cross-Story Dependencies
+
+- Depends on Epic 1 and Epic 2. The sample-when-absent rule, the edition document, and the edit-survives-regenerate rule already exist and apply to editions the server owns.
+- The browser paper and token come before the server clock, Admin, light edit, and phone or desktop remote mode.
+- The server clock owns insert-if-absent for today's morning edition. Later stories must not replace an existing morning edition.
+- The README server path and the release image come after the server and browser actually show a morning paper. Neither documents public internet exposure.
diff --git a/_bmad-output/implementation-artifacts/review-3-1-blind-hunter-prompt.md b/_bmad-output/implementation-artifacts/review-3-1-blind-hunter-prompt.md
new file mode 100644
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/review-3-1-blind-hunter-prompt.md
+Conduct a review of CONTENT.
+Look for what's missing, not only what's wrong.
+Compute your finding floor N from the diff file's size: N = min(floor(sqrt(kB) + 1), 10), where kB is the file's size in kilobytes. State the arithmetic in one line, then find at least N issues to fix or improve.
+Output a Markdown list of findings only — no severity, priority, or ranking.
+If the content is empty, stop and say so.
+If you have zero findings, re-check and keep thinking; do not stop with an empty list.
+
+CONTENT: the unified diff below. It is the content under review.
+
+Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
+
+```diff
+diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
+index 860caa7..30de6a2 100644
+--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
++++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
+@@ -29,7 +29,7 @@
+ # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
+ # - Retrospective appends its action items to action_items; the status view surfaces open ones
+ generated: 11-07-2026 19:42
+-last_updated: 11-10-2026 18:48
++last_updated: 11-10-2026 18:55
+ project: Matutine
+ project_key: NOKEY
+ tracking_system: file-system
+@@ -59,8 +59,8 @@ development_status:
+   2-7-write-the-briefing-with-a-gemini-key: done
+   epic-2-retrospective: optional
+ 
+-  epic-3: backlog
+-  3-1-open-the-server-s-paper-in-the-browser: backlog
++  epic-3: in-progress
++  3-1-open-the-server-s-paper-in-the-browser: in-progress
+   3-2-generate-the-morning-edition-on-the-server-clock: backlog
+   3-3-configure-the-server-in-admin: backlog
+   3-4-light-edit-a-block-in-the-browser: backlog
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+index c06cc9b..e418217 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+@@ -72,6 +72,7 @@ fun EditionScreen(
+     onNewEditionNow: () -> Unit = {},
+     onOpenExtra: (String) -> Unit = {},
+     onOpenMorning: () -> Unit = {},
++    readOnly: Boolean = false,
+     modifier: Modifier = Modifier,
+ ) {
+     val typography = LocalMatutineEditionTypography.current
+@@ -82,6 +83,7 @@ fun EditionScreen(
+             generating = state.generating,
+             notice = state.notice,
+             onOpenSettings = onOpenSettings,
++            readOnly = readOnly,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -104,6 +106,7 @@ fun EditionScreen(
+             onNewEditionNow = onNewEditionNow,
+             onOpenExtra = onOpenExtra,
+             onOpenMorning = onOpenMorning,
++            readOnly = readOnly,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -175,6 +178,7 @@ private fun EmptyFrontPage(
+     generating: Boolean,
+     notice: String?,
+     onOpenSettings: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -188,11 +192,13 @@ private fun EmptyFrontPage(
+             .padding(horizontal = 16.dp, vertical = 16.dp),
+         verticalArrangement = Arrangement.spacedBy(16.dp),
+     ) {
+-        TextButton(
+-            onClick = onOpenSettings,
+-            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+-        ) {
+-            Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++        if (!readOnly) {
++            TextButton(
++                onClick = onOpenSettings,
++                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++            ) {
++                Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++            }
+         }
+         Masthead(name = "Matutine", dateLine = "", typography = typography)
+         if (generating) {
+@@ -229,6 +235,7 @@ private fun EditionContent(
+     onNewEditionNow: () -> Unit,
+     onOpenExtra: (String) -> Unit,
+     onOpenMorning: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -248,29 +255,31 @@ private fun EditionContent(
+                 .padding(horizontal = 16.dp, vertical = 16.dp),
+             verticalArrangement = Arrangement.spacedBy(16.dp),
+         ) {
+-            Row(
+-                horizontalArrangement = Arrangement.spacedBy(16.dp),
+-                verticalAlignment = Alignment.CenterVertically,
+-            ) {
+-                TextButton(
+-                    onClick = onOpenSettings,
+-                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++            if (!readOnly) {
++                Row(
++                    horizontalArrangement = Arrangement.spacedBy(16.dp),
++                    verticalAlignment = Alignment.CenterVertically,
+                 ) {
+-                    Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
+-                }
+-                if (onOpenTypewriter != null) {
+                     TextButton(
+-                        onClick = onOpenTypewriter,
++                        onClick = onOpenSettings,
+                         modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                     ) {
+-                        Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                        Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                    }
++                    if (onOpenTypewriter != null) {
++                        TextButton(
++                            onClick = onOpenTypewriter,
++                            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                        ) {
++                            Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                        }
++                    }
++                    TextButton(
++                        onClick = onNewEditionNow,
++                        modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                    ) {
++                        Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                     }
+-                }
+-                TextButton(
+-                    onClick = onNewEditionNow,
+-                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+-                ) {
+-                    Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                 }
+             }
+             if (edition.role == EditionRole.extra) {
+@@ -316,10 +325,10 @@ private fun EditionContent(
+             }
+             if (edition.isSample) {
+                 Text(
+-                    text = "SAMPLE EDITION",
++                    text = EditionTranscript.SAMPLE,
+                     style = typography.kicker.copy(color = EditionThemeColors.current.accent),
+                 )
+-            } else if (showNoTopicLine) {
++            } else if (showNoTopicLine && !readOnly) {
+                 TextButton(
+                     onClick = onOpenInterests,
+                     modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+@@ -339,6 +348,7 @@ private fun EditionContent(
+                     onAddTask = onAddTask,
+                     calendarPermissionOff = calendarPermissionOff,
+                     onOpenCalendarPermission = onOpenCalendarPermission,
++                    readOnly = readOnly,
+                     typography = typography,
+                 )
+             } else {
+@@ -351,6 +361,7 @@ private fun EditionContent(
+                     taskSaved = taskSaved,
+                     onToggleTask = onToggleTask,
+                     onAddTask = onAddTask,
++                    readOnly = readOnly,
+                     typography = typography,
+                 )
+             }
+@@ -366,6 +377,7 @@ private fun WideEditionBody(
+     onAddTask: (String) -> Unit,
+     calendarPermissionOff: Boolean,
+     onOpenCalendarPermission: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+ ) {
+     Spacer(Modifier.height(8.dp))
+@@ -395,6 +407,7 @@ private fun WideEditionBody(
+                 taskSaved = taskSaved,
+                 onToggleTask = onToggleTask,
+                 onAddTask = onAddTask,
++                readOnly = readOnly,
+                 typography = typography,
+             )
+         }
+@@ -562,6 +575,7 @@ private fun TaskBlock(
+     taskSaved: Boolean,
+     onToggleTask: (Int) -> Unit,
+     onAddTask: (String) -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+ ) {
+     var draft by remember { mutableStateOf("") }
+@@ -571,18 +585,25 @@ private fun TaskBlock(
+             Text(text = EditionCopy.NO_TASKS, style = typography.body)
+         } else {
+             tasks.forEachIndexed { index, task ->
+-                TaskRow(task, typography, onToggle = { onToggleTask(index) })
++                TaskRow(
++                    task,
++                    typography,
++                    onToggle = { onToggleTask(index) },
++                    readOnly = readOnly,
++                )
+             }
+         }
+-        TaskAddLine(
+-            value = draft,
+-            onValueChange = { draft = it },
+-            onConfirm = {
+-                onAddTask(draft)
+-                draft = ""
+-            },
+-            typography = typography,
+-        )
++        if (!readOnly) {
++            TaskAddLine(
++                value = draft,
++                onValueChange = { draft = it },
++                onConfirm = {
++                    onAddTask(draft)
++                    draft = ""
++                },
++                typography = typography,
++            )
++        }
+         if (taskSaved) {
+             Text(text = EditionCopy.SAVED, style = typography.meta)
+         }
+@@ -624,6 +645,7 @@ private fun TaskRow(
+     task: EditionTask,
+     typography: MatutineEditionTypography,
+     onToggle: () -> Unit,
++    readOnly: Boolean,
+ ) {
+     val touchTarget = if (editionForceSingleColumn()) {
+         Modifier.minimumInteractiveComponentSize()
+@@ -636,10 +658,16 @@ private fun TaskRow(
+         modifier = Modifier
+             .padding(vertical = 4.dp)
+             .then(touchTarget)
+-            .clickable(
+-                interactionSource = remember { MutableInteractionSource() },
+-                indication = null,
+-                onClick = onToggle,
++            .then(
++                if (readOnly) {
++                    Modifier
++                } else {
++                    Modifier.clickable(
++                        interactionSource = remember { MutableInteractionSource() },
++                        indication = null,
++                        onClick = onToggle,
++                    )
++                },
+             ),
+     ) {
+         Box(
+diff --git a/app/webApp/build.gradle.kts b/app/webApp/build.gradle.kts
+index 4d8b955..6bbc79c 100644
+--- a/app/webApp/build.gradle.kts
++++ b/app/webApp/build.gradle.kts
+@@ -22,7 +22,11 @@ kotlin {
+         commonMain.dependencies {
+             implementation(project(":app:shared"))
+ 
++            implementation(libs.compose.runtime)
++            implementation(libs.compose.foundation)
++            implementation(libs.compose.material3)
+             implementation(libs.compose.ui)
++            implementation(libs.kotlinx.coroutines.core)
+         }
+     }
+ }
+\ No newline at end of file
+diff --git a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
+index f0b181a..e9da8b9 100644
+--- a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
++++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
+@@ -1,11 +1,142 @@
+ package hu.toliver.matutine
+ 
++import androidx.compose.foundation.background
++import androidx.compose.foundation.layout.Arrangement
++import androidx.compose.foundation.layout.Column
++import androidx.compose.foundation.layout.fillMaxSize
++import androidx.compose.foundation.layout.fillMaxWidth
++import androidx.compose.foundation.layout.heightIn
++import androidx.compose.foundation.layout.padding
++import androidx.compose.foundation.shape.RoundedCornerShape
++import androidx.compose.material3.Button
++import androidx.compose.material3.ButtonDefaults
++import androidx.compose.material3.OutlinedTextField
++import androidx.compose.material3.Text
++import androidx.compose.runtime.Composable
++import androidx.compose.runtime.getValue
++import androidx.compose.runtime.mutableStateOf
++import androidx.compose.runtime.remember
++import androidx.compose.runtime.rememberCoroutineScope
++import androidx.compose.runtime.setValue
+ import androidx.compose.ui.ExperimentalComposeUiApi
++import androidx.compose.ui.Modifier
++import androidx.compose.ui.unit.dp
+ import androidx.compose.ui.window.ComposeViewport
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionScreen
++import hu.toliver.matutine.edition.EditionUiState
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.webPaperGate
++import hu.toliver.matutine.theme.EditionThemeColors
++import hu.toliver.matutine.theme.LocalMatutineEditionTypography
++import hu.toliver.matutine.theme.MatutineTheme
++import hu.toliver.matutine.theme.NewsprintColors
++import kotlinx.coroutines.launch
+ 
+ @OptIn(ExperimentalComposeUiApi::class)
+ fun main() {
+     ComposeViewport {
+-        App()
++        ServerPaperApp()
+     }
+-}
+\ No newline at end of file
++}
++
++@Composable
++fun ServerPaperApp() {
++    var typedToken by remember { mutableStateOf("") }
++    var message by remember { mutableStateOf<String?>(null) }
++    var edition by remember { mutableStateOf<Edition?>(null) }
++    var submitting by remember { mutableStateOf(false) }
++    val scope = rememberCoroutineScope()
++    MatutineTheme {
++        val paper = edition
++        if (paper == null) {
++            TokenGate(
++                token = typedToken,
++                onTokenChange = { typedToken = it },
++                message = message,
++                submitting = submitting,
++                onSubmit = {
++                    if (submitting) return@TokenGate
++                    submitting = true
++                    scope.launch {
++                        val decision = try {
++                            val response = fetchCurrentEdition(typedToken)
++                            val gate = webPaperGate(typedToken, response.status)
++                            typedToken = gate.typedToken
++                            message = gate.message
++                            if (gate.showEdition) {
++                                decodeEdition(response.body)
++                            } else {
++                                null
++                            }
++                        } catch (_: Throwable) {
++                            message = null
++                            null
++                        }
++                        edition = decision
++                        submitting = false
++                    }
++                },
++            )
++        } else {
++            EditionScreen(
++                state = EditionUiState.Ready(paper),
++                onRetrySample = {},
++                onOpenSettings = {},
++                showNoTopicLine = false,
++                onOpenInterests = {},
++                onToggleTask = {},
++                onAddTask = {},
++                readOnly = true,
++            )
++        }
++    }
++}
++
++@Composable
++private fun TokenGate(
++    token: String,
++    onTokenChange: (String) -> Unit,
++    message: String?,
++    submitting: Boolean,
++    onSubmit: () -> Unit,
++) {
++    val typography = LocalMatutineEditionTypography.current
++    Column(
++        modifier = Modifier
++            .fillMaxSize()
++            .background(NewsprintColors.chrome)
++            .padding(24.dp),
++        verticalArrangement = Arrangement.spacedBy(16.dp),
++    ) {
++        Text(text = "Token", style = typography.body.copy(color = EditionThemeColors.current.ink))
++        OutlinedTextField(
++            value = token,
++            onValueChange = onTokenChange,
++            modifier = Modifier
++                .fillMaxWidth()
++                .heightIn(min = 40.dp),
++            singleLine = true,
++            textStyle = typography.body.copy(color = EditionThemeColors.current.ink),
++            shape = RoundedCornerShape(2.dp),
++        )
++        if (!message.isNullOrBlank()) {
++            Text(
++                text = message,
++                style = typography.body.copy(color = EditionThemeColors.current.error),
++            )
++        }
++        Button(
++            onClick = onSubmit,
++            enabled = !submitting,
++            modifier = Modifier.heightIn(min = 40.dp),
++            shape = RoundedCornerShape(2.dp),
++            colors = ButtonDefaults.buttonColors(
++                containerColor = EditionThemeColors.current.ink,
++                contentColor = EditionThemeColors.current.paper,
++            ),
++        ) {
++            Text(text = "Continue")
++        }
++    }
++}
+diff --git a/server/build.gradle.kts b/server/build.gradle.kts
+index bbd6ccd..1462455 100644
+--- a/server/build.gradle.kts
++++ b/server/build.gradle.kts
+@@ -16,4 +16,15 @@ dependencies {
+     implementation(libs.ktor.serverNetty)
+     testImplementation(libs.ktor.serverTestHost)
+     testImplementation(libs.kotlin.testJunit)
++}
++
++val webDistribution = project(":app:webApp").layout.buildDirectory.dir(
++    "dist/wasmJs/productionExecutable",
++)
++
++tasks.named<ProcessResources>("processResources") {
++    dependsOn(":app:webApp:wasmJsBrowserDistribution")
++    from(webDistribution) {
++        into("web")
++    }
+ }
+\ No newline at end of file
+diff --git a/server/src/main/kotlin/hu/toliver/matutine/Application.kt b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
+index 25b8388..c2cafe8 100644
+--- a/server/src/main/kotlin/hu/toliver/matutine/Application.kt
++++ b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
+@@ -1,10 +1,33 @@
+ package hu.toliver.matutine
+ 
+-import io.ktor.server.application.*
+-import io.ktor.server.engine.*
+-import io.ktor.server.netty.*
+-import io.ktor.server.response.*
+-import io.ktor.server.routing.*
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.LocalEditionRepository
++import hu.toliver.matutine.edition.encodeEdition
++import io.ktor.http.ContentType
++import io.ktor.http.HttpStatusCode
++import io.ktor.server.application.Application
++import io.ktor.server.application.call
++import io.ktor.server.engine.embeddedServer
++import io.ktor.server.netty.Netty
++import io.ktor.server.response.respondText
++import io.ktor.server.routing.get
++import io.ktor.server.routing.routing
++import io.ktor.server.http.content.staticResources
++import io.ktor.util.AttributeKey
++
++private val SharedTokenKey = AttributeKey<String>("matutine.sharedToken")
++private val DocumentStoreKey = AttributeKey<EditionStore>("matutine.documents")
++
++fun resolveSharedToken(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "matutine-demo"
++
++fun resolveDataRoot(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "/data"
++
++internal fun bearerMatches(authorization: String?, token: String): Boolean {
++    val prefix = "Bearer "
++    if (authorization == null || !authorization.startsWith(prefix)) return false
++    val presented = authorization.substring(prefix.length)
++    return presented.isNotEmpty() && presented == token
++}
+ 
+ fun main() {
+     embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
+@@ -12,9 +35,34 @@ fun main() {
+ }
+ 
+ fun Application.module() {
++    val token = attributes.getOrNull(SharedTokenKey)
++        ?: resolveSharedToken(System.getenv("MATUTINE_TOKEN"))
++    val documents = attributes.getOrNull(DocumentStoreKey)
++        ?: DataDirectoryStore(resolveDataRoot(System.getenv("MATUTINE_DATA")))
++    val repository = LocalEditionRepository(documents)
+     routing {
+-        get("/") {
+-            call.respondText(sayHello("Ktor"))
++        get("/editions/current") {
++            val authorization = call.request.headers["Authorization"]
++            if (!bearerMatches(authorization, token)) {
++                call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
++                return@get
++            }
++            val edition = repository.loadCurrentMorningEdition().getOrElse { error ->
++                call.respondText(
++                    error.message ?: "unavailable",
++                    status = HttpStatusCode.NotFound,
++                )
++                return@get
++            }
++            call.respondText(encodeEdition(edition), ContentType.Application.Json)
++        }
++        staticResources("/", "web") {
++            default("index.html")
+         }
+     }
+-}
+\ No newline at end of file
++}
++
++fun Application.installTestDocuments(token: String, documents: EditionStore) {
++    attributes.put(SharedTokenKey, token)
++    attributes.put(DocumentStoreKey, documents)
++}
+diff --git a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+index 23dc320..fc711d4 100644
+--- a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
++++ b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+@@ -1,20 +1,194 @@
+ package hu.toliver.matutine
+ 
+-import io.ktor.client.request.*
+-import io.ktor.client.statement.*
+-import io.ktor.http.*
+-import io.ktor.server.testing.*
+-import kotlin.test.*
++import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionRole
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.EditionTranscript
++import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SourceList
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.todayLocalDate
++import hu.toliver.matutine.edition.toDayKey
++import io.ktor.client.request.get
++import io.ktor.client.request.header
++import io.ktor.client.statement.bodyAsText
++import io.ktor.http.HttpStatusCode
++import io.ktor.server.testing.testApplication
++import java.nio.file.Files
++import kotlin.test.Test
++import kotlin.test.assertEquals
++import kotlin.test.assertFalse
++import kotlin.test.assertNull
++import kotlin.test.assertTrue
+ 
+ class ApplicationTest {
+ 
+     @Test
+-    fun testRoot() = testApplication {
++    fun unsetToken_acceptsTheDemoBearer() = testApplication {
++        val root = Files.createTempDirectory("matutine-demo").toFile()
+         application {
++            installTestDocuments(resolveSharedToken(null), DataDirectoryStore(root.absolutePath))
+             module()
+         }
+-        val response = client.get("/")
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
+         assertEquals(HttpStatusCode.OK, response.status)
+-        assertEquals("Hello, Ktor!", response.bodyAsText())
++        assertTrue(decodeEdition(response.bodyAsText()).isSample)
++    }
++
++    @Test
++    fun blankToken_isTheDemoBearer() {
++        assertEquals("matutine-demo", resolveSharedToken(null))
++        assertEquals("matutine-demo", resolveSharedToken(""))
++        assertEquals("matutine-demo", resolveSharedToken("   "))
++        assertEquals("/data", resolveDataRoot(null))
++        assertEquals("/data", resolveDataRoot(""))
+     }
+-}
+\ No newline at end of file
++
++    @Test
++    fun setToken_rejectsEveryOtherBearerBeforeReadingDocuments() = testApplication {
++        val store = TouchingStore()
++        application {
++            installTestDocuments(resolveSharedToken("lan-secret"), store)
++            module()
++        }
++        val missing = client.get("/editions/current")
++        val wrongScheme = client.get("/editions/current") {
++            header("Authorization", "Token lan-secret")
++        }
++        val wrongToken = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        assertEquals(HttpStatusCode.Unauthorized, missing.status)
++        assertEquals(HttpStatusCode.Unauthorized, wrongScheme.status)
++        assertEquals(HttpStatusCode.Unauthorized, wrongToken.status)
++        assertFalse(missing.bodyAsText().contains("isSample"))
++        assertFalse(wrongToken.bodyAsText().contains("mastheadName"))
++        assertEquals(0, store.touches)
++    }
++
++    @Test
++    fun setToken_acceptsThatBearer() = testApplication {
++        val root = Files.createTempDirectory("matutine-set-token").toFile()
++        application {
++            installTestDocuments(resolveSharedToken("lan-secret"), DataDirectoryStore(root.absolutePath))
++            module()
++        }
++        val accepted = client.get("/editions/current") {
++            header("Authorization", "Bearer lan-secret")
++        }
++        assertEquals(HttpStatusCode.OK, accepted.status)
++        assertTrue(decodeEdition(accepted.bodyAsText()).isSample)
++    }
++
++    @Test
++    fun absentSourceList_returnsTheServerSampleAndDoesNotWriteTheList() = testApplication {
++        val root = Files.createTempDirectory("matutine-sample").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        application {
++            installTestDocuments("matutine-demo", store)
++            module()
++        }
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        val edition = decodeEdition(response.bodyAsText())
++        assertEquals(HttpStatusCode.OK, response.status)
++        assertTrue(edition.isSample)
++        assertTrue(EditionTranscript.render(edition).contains(EditionTranscript.SAMPLE))
++        assertNull(store.readSourceList())
++        assertFalse(root.resolve("sources.json").exists())
++        assertTrue(root.resolve("editions/${edition.dayKey}-morning.json").exists())
++        val again = decodeEdition(
++            client.get("/editions/current") {
++                header("Authorization", "Bearer matutine-demo")
++            }.bodyAsText(),
++        )
++        assertEquals(edition.id, again.id)
++        assertEquals(edition.revision, again.revision)
++    }
++
++    @Test
++    fun existingMorning_isReturnedUnchanged() = testApplication {
++        val root = Files.createTempDirectory("matutine-kept").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        val day = todayLocalDate()
++        val morning = Edition(
++            id = "morning-${day.toDayKey()}",
++            revision = 4,
++            dayKey = day.toDayKey(),
++            role = EditionRole.morning,
++            mastheadName = "Matutine",
++            dateLine = "Kept date",
++            isSample = false,
++            briefing = "Kept by hand",
++            stories = emptyList(),
++            events = emptyList(),
++            tasks = emptyList(),
++        )
++        store.saveMorningEdition(morning)
++        application {
++            installTestDocuments("matutine-demo", store)
++            module()
++        }
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        val edition = decodeEdition(response.bodyAsText())
++        assertEquals(morning, edition)
++        assertEquals(morning, store.loadMorningEdition(day.toDayKey()))
++    }
++
++    @Test
++    fun documents_areFilesUnderTheDataDirectory() {
++        val root = Files.createTempDirectory("matutine-docs").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        store.writeSettings(ReaderSettings())
++        store.writeSourceList(SourceList(id = "sources", revision = 1))
++        store.writeCalendarSnapshot(CalendarSnapshot())
++        val day = todayLocalDate()
++        store.saveMorningEdition(
++            Edition(
++                id = "morning-${day.toDayKey()}",
++                revision = 1,
++                dayKey = day.toDayKey(),
++                role = EditionRole.morning,
++                mastheadName = "Matutine",
++                dateLine = "Date",
++                isSample = false,
++                briefing = "Brief",
++                stories = emptyList(),
++                events = emptyList(),
++                tasks = emptyList(),
++            ),
++        )
++        assertTrue(root.resolve("settings.json").isFile)
++        assertTrue(root.resolve("sources.json").isFile)
++        assertTrue(root.resolve("calendar-snapshot.json").isFile)
++        assertTrue(root.resolve("editions/${day.toDayKey()}-morning.json").isFile)
++    }
++}
++
++private class TouchingStore : EditionStore {
++    var touches: Int = 0
++
++    private fun touch(): Nothing {
++        touches += 1
++        error("store touched")
++    }
++
++    override fun readSourceList() = touch()
++    override fun writeSourceList(sourceList: SourceList) = touch()
++    override fun readSettings() = touch()
++    override fun writeSettings(settings: ReaderSettings) = touch()
++    override fun loadMorningEdition(dayKey: String) = touch()
++    override fun saveMorningEdition(edition: Edition) = touch()
++    override fun loadExtras(dayKey: String) = touch()
++    override fun saveExtra(edition: Edition) = touch()
++    override fun readCalendarSnapshot() = touch()
++    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = touch()
++    override fun readCalDavAccount() = touch()
++    override fun writeCalDavAccount(account: hu.toliver.matutine.edition.CalDavAccount) = touch()
++}
+diff --git a/.dockerignore b/.dockerignore
+new file mode 100644
+index 0000000..c100704
+--- /dev/null
++++ b/.dockerignore
+@@ -0,0 +1,9 @@
++.git
++.gradle
++**/.gradle
++**/build
++.idea
++_bmad
++_bmad-output
++*.md
++!README.md
+diff --git a/_bmad-output/implementation-artifacts/epic-3-context.md b/_bmad-output/implementation-artifacts/epic-3-context.md
+new file mode 100644
+index 0000000..1b24060
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/epic-3-context.md
+@@ -0,0 +1,56 @@
++# Epic 3 Context: Read the server's morning edition
++
++<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->
++
++## Goal
++
++O runs one Docker server on the LAN and reads that same morning edition in the browser and on the phone. Admin is where the server is configured. Local papers are not uploaded when a client points at the server. The edit-survives-regenerate rule still applies to editions the server owns.
++
++## Stories
++
++- Story 3.1: Open the server's paper in the browser
++- Story 3.2: Generate the morning edition on the server clock
++- Story 3.3: Configure the server in Admin
++- Story 3.4: Light-edit a block in the browser
++- Story 3.5: Read the server edition on Android
++- Story 3.6: Read the server edition on Desktop
++- Story 3.7: Document the server judge path
++- Story 3.8: Publish the Docker image
++
++## Requirements & Constraints
++
++- One shared token, no accounts, no push. Unset token is the literal `matutine-demo` on the server and on every remote client. The token is required before the newspaper or Admin renders. A mismatch says "That token doesn't match." and keeps the typed value. The same token covers the newspaper and Admin. Every remote call sends `Authorization: Bearer`.
++- The shipped server is a Docker image running the Ktor process. Token, AI key, and timezone come from the process environment, not the image. Edition, settings, source list, and calendar snapshot live in a host directory mounted at `/data`, not in the image layer. Do not document a public port. `java -jar` is not a required launch.
++- When the source-list document is absent, the generator builds an edition from bundled sample feeds and sample calendar data, shows the sample line, and does not write those samples into the list. An empty list is a real configuration and does not fall back to samples. No account and no AI key are required to see a paper.
++- The web client does not run the generator and is not an offline app. Web has no Settings screen. Admin is the web-only place for feeds, ICS, CalDAV, generation time, timezone, interest questions, AI provider and key, and the shared token.
++- The server inserts today's morning edition at the configured time only when that document is absent. It does not replace an existing morning edition. A clock-precise paper is claimed only while this server is running. The server holds the AI key; remote clients do not. The timezone value is whatever the process was given; do not invent an identifier format.
++- Saving a server base URL and token does not upload local editions, the local source list, or the local calendar snapshot. Remote clients read, edit, regenerate, or request an extra. They do not mint the morning edition. If the server cannot be reached, show the last stored edition and "Couldn't reach the server. Showing the copy on this device." Generate stays available and writes locally. Android accepts a cleartext `http` LAN URL. Notification time, notification text, theme, and layout stay on the phone. Feeds, calendar, and interest questions are not editable on the phone while a server is configured.
++- Web light edit changes one block in place and saves on leave. Last write wins. There is no merge screen and no revision number on the page. A failed save says "Couldn't save. Your text is still here." Changing the printed date does not change the day key.
++- User-owned text on a server-owned edition survives regenerate the same way it does on the device. Whole-document replace stays the write model.
++- Milestone B also publishes the Docker image from the existing release workflow. Secrets stay in the environment.
++
++## Technical Decisions
++
++- Modules stay `shared`, `server`, `androidApp`, `desktopApp`, and `webApp`. `server` owns the scheduler, HTTP API, stores, Docker image, and served web files. `webApp` is Compose Multiplatform for Web, HTTP only, served by the server.
++- Edition documents, settings, the source list, and calendar snapshots use one kotlinx.serialization `Json` configuration with default property names and no naming strategy. `webApp` sends and reads that JSON and does not declare its own model.
++- The server owns these routes, and every remote client uses them: `GET /editions`, `GET /editions/current`, `GET /editions/by-id`, `PUT /editions/by-id`, `POST /editions/morning/regenerate`, `POST /editions`, `GET /settings`, `PUT /settings`, `GET /sources`, `PUT /sources`, `PUT /calendar-snapshot`.
++- Each of those documents is one JSON document. The store assigns the id on insert. The id never changes. Every write gets a new monotonic revision. The latest whole document wins.
++- One source-list document holds RSS/Atom and ICS URLs. The generator reads that list and no other. The server environment writes it only when it is absent.
++- Clients talk only to a repository. With a server base URL, only the server generator writes a new morning paper. Roles stay `morning` and `extra`. The front page is today's morning edition. Regenerate keeps the id. New edition now inserts an extra.
++- Kotlin 2.4.21, Ktor 3.6.1, kotlinx-serialization 1.12.0, Compose Multiplatform 1.12.2, JDK 17. JVM target 17 for `shared`, `server`, and `desktopApp`.
++- Reach is the LAN. One configured base URL. `http` and `https` are the same API with a different scheme. Android network security must allow that URL, including cleartext when the scheme is `http`.
++
++## UX & Interaction Patterns
++
++- The token gate is the first web screen: chrome, one text field, one primary button. No masthead and no edition behind a rejected token. Error copy is "That token doesn't match." The field keeps the typed value.
++- After a valid token, the web Edition is the newspaper. Admin is a separate chrome screen opened from the newspaper, not a Settings screen.
++- Light edit keeps the display type, uses a 1px ink underline, and has no toolbar and no mono face. "Saved." is the last confirmed line.
++- The Edition does not say whether the paper came from the device or the server.
++- Below 960px the web client uses the single-column order. Chrome controls are at least 40px. The edition itself stays the custom paper, not chrome.
++
++## Cross-Story Dependencies
++
++- Depends on Epic 1 and Epic 2. The sample-when-absent rule, the edition document, and the edit-survives-regenerate rule already exist and apply to editions the server owns.
++- The browser paper and token come before the server clock, Admin, light edit, and phone or desktop remote mode.
++- The server clock owns insert-if-absent for today's morning edition. Later stories must not replace an existing morning edition.
++- The README server path and the release image come after the server and browser actually show a morning paper. Neither documents public internet exposure.
+diff --git a/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
+new file mode 100644
+index 0000000..afd8a10
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
+@@ -0,0 +1,105 @@
++---
++title: 'Open the server''s paper in the browser'
++type: 'feature'
++created: '2026-11-10'
++status: 'in-review'
++baseline_commit: '4b57ede7cf6ca7d43bbf180250b81d904035a479'
++route: 'dispatch'
++review_loop_iteration: 0
++context:
++  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
++  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md'
++---
++
++<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
++
++## Intent
++
++**Problem:** The LAN paper is not visible. `:server` is a hello-world Ktor process, and `:app:webApp` opens the on-device app, which generates a paper in the browser.
++
++**Approach:** Ship a Dockerized Ktor process that stores documents under `/data`, requires one Bearer token, and serves a browser client whose first screen is a token gate and whose next screen is the sample newspaper generated on the server.
++
++## Boundaries & Constraints
++
++**Always:**
++- Unset or blank `MATUTINE_TOKEN` is the literal `matutine-demo`. One shared token. Every edition API call uses `Authorization: Bearer <token>`.
++- Documents live in the host directory mounted at `/data` (override `MATUTINE_DATA` only for tests). Same JSON codec and file names as the existing store: `sources.json`, `settings.json`, `calendar-snapshot.json`, `editions/<dayKey>-morning.json`.
++- The only edition route in this story is `GET /editions/current`. A missing or wrong bearer token is rejected before the store is read and before any edition body is written.
++- An absent source list and an absent morning document: the server builds the bundled sample through `LocalEditionRepository.loadCurrentMorningEdition` and returns it, with the sample line. An existing morning document is returned unchanged.
++- The web client decodes `Edition` with `decodeEdition`. It does not declare an edition model and does not call the generator.
++- The token gate is chrome: one field, one primary button labeled Continue. No paper and no Admin until the current-edition call succeeds. A rejected token shows "That token doesn't match." and keeps the typed value.
++
++**Never:**
++- No scheduler, no `runMorning`, no Admin, no light edit, no client sync, no Settings screen, no service worker, no public-port instructions, no README change, no timezone identifier format.
++- Do not add the other AD-16 routes. Do not replace an existing morning edition. Do not depend on `:app:shared` from `:server`.
++
++## I/O & Edge-Case Matrix
++
++| Scenario | Input / State | Expected Output / Behavior | Error Handling |
++|----------|--------------|---------------------------|----------------|
++| Default token | Env token unset or blank | Accepted bearer is `Bearer matutine-demo` | N/A |
++| Set token | `MATUTINE_TOKEN=lan-secret` | Only `Bearer lan-secret` is accepted | Other values rejected |
++| No or wrong bearer | Missing header, non-Bearer scheme, or wrong token | 401, body is not an edition document, store is not read | N/A |
++| Absent source list | Valid bearer, no `sources.json`, no morning file | 200 `Edition` JSON, `isSample` true, sample line text present in the decoded edition, morning file written, `sources.json` still absent | N/A |
++| Existing morning | Valid bearer, morning file already present | 200 returns that document unchanged; revision and id stay | N/A |
++| Wrong gate token | Browser submits a value the server rejects | Gate stays, field keeps the value, message "That token doesn't match.", Edition and Admin are not composed | N/A |
++| Right gate token | Browser submits the server token and the source list is absent | Edition screen shows the returned paper, including SAMPLE EDITION | A non-401 failure stays on the gate and does not show a paper |
++
++</frozen-after-approval>
++
++## Code Map
++
++- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- replace the hello route with config, auth, `GET /editions/current`, and static web files.
++- `server/build.gradle.kts` -- already depends on `:core`. Keep that. Wire the Wasm distribution into the server resources.
++- `core/.../EditionRepository.kt` -- `LocalEditionRepository.loadCurrentMorningEdition` already saves the sample when the source list is absent and returns an existing morning first. Reuse it. Do not call `runMorning`.
++- `core/.../EditionJson.kt` -- `editionJson`, `encodeEdition`, `decodeEdition`. The only codec.
++- `app/shared/.../FileEditionStore.jvm.kt` -- file names to match. Do not depend on this module from the server; it pulls Compose.
++- `app/shared/.../EditionScreen.kt` -- newspaper drawing. Add a defaulted read-only switch so web can hide Settings, New edition now, and task writes. Android and Desktop call sites stay as they are.
++- `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- today calls `App()`, which runs `MatutineApp` and the local generator. Replace that entry.
++- `app/shared/.../App.kt` -- leave the device preview entry unchanged.
++- `gradle/libs.versions.toml` -- Ktor 3.6.0 is the catalog pin. Do not bump it.
++
++## Tasks & Acceptance
++
++**Execution:**
++- [x] `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` -- `EditionStore` on the data directory using core codecs and the existing file names.
++- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- token resolution, bearer gate, current-edition route, static files from the built web app.
++- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- cover the I/O rows that the server owns, including a store probe that fails if a rejected call touches documents.
++- [x] `server/Dockerfile` -- image runs the fat jar, store path `/data`, token and other secrets only from the environment.
++- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- defaulted read-only chrome so existing device screens keep their controls.
++- [x] `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- token gate, then read-only `EditionScreen` from `GET /editions/current` decoded with `decodeEdition`.
++- [x] `server/build.gradle.kts` -- depend on the Wasm browser distribution and package those files for the server to serve.
++
++**Acceptance Criteria:**
++- Given the Docker image and a host directory mounted at `/data`, when the container starts with no token in the environment, then the accepted token is `matutine-demo` and edition, settings, source list, and calendar snapshot files are under `/data`.
++- Given the wrong token, when it is submitted, then the page says "That token doesn't match.", the field keeps the typed value, and neither the newspaper nor Admin is shown.
++- Given the right token and an absent source list, when the gate opens, then the web Edition shows the server-generated sample paper and the sample line.
++- Given any current-edition call, when the bearer token is missing or wrong, then the server rejects it before returning an edition.
++- The web bundle does not invoke the generator, and it is not an offline app.
++
++## Implementation Notes
++
++- Token environment name is `MATUTINE_TOKEN`. Blank and unset both resolve to `matutine-demo`. Data directory is `MATUTINE_DATA`, default `/data`.
++- The server store copies the desktop file names and uses the core codecs. It does not depend on `:app:shared`.
++- `GET /editions/current` is the only API route. A rejected bearer returns plain text `unauthorized` and does not call the store.
++- Sample generation is `LocalEditionRepository.loadCurrentMorningEdition`. `runMorning` is not called.
++- The gate button label is Continue. The field label is Token. The web entry no longer calls `App()`.
++- Wasm fetch uses the browser `window.fetch` Promise. JS uses the same call. Both send `Authorization: Bearer` and decode with `decodeEdition`.
++- `./gradlew :server:test` passed 7 tests. `:app:shared:jvmTest --tests hu.toliver.matutine.edition.WebPaperGateTest` passed 4 tests. `:server:buildFatJar` produced `server/build/libs/server-all.jar`.
++- The browser showed the chrome token gate at `http://127.0.0.1:8080/` with one field and Continue, and no newspaper. Compose on Wasm did not accept the automated click, so the mismatch sentence and the sample paper were not exercised in that browser session. Curl against the running jar confirmed 401 for a missing or wrong bearer and a sample edition for `Bearer matutine-demo`, with only `editions/<day>-morning.json` written.
++- The first Docker build failed because the Wasm Node binary needs `libatomic.so.1`, which the Temurin JDK image does not ship. The build stage installs `libatomic1` before Gradle. The jar's bytecode is class version 65, so the runtime image is Temurin 21.
++
++## Spec Change Log
++
++## Review Triage Log
++
++## Design Notes
++
++The server implements `EditionStore` itself so the process can use `LocalEditionRepository` without taking a dependency on `:app:shared`. `GET /editions/current` is the one AD-16 route this story needs; the other routes stay absent until their stories. The gate compares nothing locally: only a successful current-edition response opens the paper. The primary button label is Continue. The field label is Token.
++
++## Verification
++
++**Commands:**
++- `./gradlew :server:test` -- expected: bearer, sample, and existing-morning tests pass.
++- `./gradlew :server:buildFatJar :app:webApp:wasmJsBrowserDistribution` -- expected: fat jar and Wasm distribution build.
++- `docker build -t matutine -f server/Dockerfile .` -- expected: image builds when Docker is available.
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
+new file mode 100644
+index 0000000..2dbdf09
+--- /dev/null
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
+@@ -0,0 +1,23 @@
++package hu.toliver.matutine.edition
++
++const val TOKEN_DOES_NOT_MATCH = "That token doesn't match."
++
++data class WebPaperGate(
++    val typedToken: String,
++    val showEdition: Boolean,
++    val message: String?,
++)
++
++/**
++ * The browser gate does not compare the token itself. A 200 from the current-edition
++ * route is the only path that opens the paper. The typed value is returned unchanged.
++ */
++fun webPaperGate(typedToken: String, httpStatus: Int): WebPaperGate = when (httpStatus) {
++    200 -> WebPaperGate(typedToken = typedToken, showEdition = true, message = null)
++    401 -> WebPaperGate(
++        typedToken = typedToken,
++        showEdition = false,
++        message = TOKEN_DOES_NOT_MATCH,
++    )
++    else -> WebPaperGate(typedToken = typedToken, showEdition = false, message = null)
++}
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
+new file mode 100644
+index 0000000..9d09d01
+--- /dev/null
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
+@@ -0,0 +1,43 @@
++package hu.toliver.matutine.edition
++
++import kotlin.test.Test
++import kotlin.test.assertEquals
++import kotlin.test.assertFalse
++import kotlin.test.assertNull
++import kotlin.test.assertTrue
++
++class WebPaperGateTest {
++
++    @Test
++    fun wrongToken_keepsTheTypedValueAndDoesNotOpenThePaper() {
++        val decision = webPaperGate(typedToken = "not-the-token", httpStatus = 401)
++        assertEquals("not-the-token", decision.typedToken)
++        assertEquals(TOKEN_DOES_NOT_MATCH, decision.message)
++        assertFalse(decision.showEdition)
++    }
++
++    @Test
++    fun rightToken_opensTheEditionWithoutAGateMessage() {
++        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 200)
++        assertEquals("matutine-demo", decision.typedToken)
++        assertNull(decision.message)
++        assertTrue(decision.showEdition)
++    }
++
++    @Test
++    fun otherFailure_staysOnTheGateWithoutAPaper() {
++        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 503)
++        assertEquals("matutine-demo", decision.typedToken)
++        assertNull(decision.message)
++        assertFalse(decision.showEdition)
++    }
++
++    @Test
++    fun sampleEditionCarriesTheSampleLine() {
++        val edition = SampleGenerator.forDay(todayLocalDate())
++        val decoded = decodeEdition(encodeEdition(edition))
++        assertTrue(decoded.isSample)
++        assertTrue(EditionTranscript.render(decoded).contains(EditionTranscript.SAMPLE))
++        assertEquals("SAMPLE EDITION", EditionTranscript.SAMPLE)
++    }
++}
+diff --git a/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
+new file mode 100644
+index 0000000..75b86ea
+--- /dev/null
++++ b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
+@@ -0,0 +1,16 @@
++package hu.toliver.matutine
++
++import kotlinx.browser.window
++import kotlinx.coroutines.await
++import org.w3c.fetch.Headers
++import org.w3c.fetch.RequestInit
++
++actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
++    val headers = Headers()
++    headers.append("Authorization", "Bearer $token")
++    val response = window.fetch(
++        "/editions/current",
++        RequestInit(method = "GET", headers = headers),
++    ).await()
++    return EditionFetch(status = response.status.toInt(), body = response.text().await())
++}
+diff --git a/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
+new file mode 100644
+index 0000000..a20cff6
+--- /dev/null
++++ b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
+@@ -0,0 +1,23 @@
++package hu.toliver.matutine
++
++import kotlin.js.ExperimentalWasmJsInterop
++import kotlin.js.JsString
++import kotlin.js.Promise
++import kotlinx.browser.window
++import kotlinx.coroutines.await
++import org.w3c.fetch.Headers
++import org.w3c.fetch.RequestInit
++import org.w3c.fetch.Response
++
++@OptIn(ExperimentalWasmJsInterop::class)
++actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
++    val headers = Headers()
++    headers.append("Authorization", "Bearer $token")
++    val pending: Promise<Response> = window.fetch(
++        "/editions/current",
++        RequestInit(method = "GET", headers = headers),
++    )
++    val response = pending.await()
++    val body = response.text().await<JsString>().toString()
++    return EditionFetch(status = response.status.toInt(), body = body)
++}
+diff --git a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
+new file mode 100644
+index 0000000..076a567
+--- /dev/null
++++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
+@@ -0,0 +1,8 @@
++package hu.toliver.matutine
++
++data class EditionFetch(
++    val status: Int,
++    val body: String,
++)
++
++expect suspend fun fetchCurrentEdition(token: String): EditionFetch
+diff --git a/server/Dockerfile b/server/Dockerfile
+new file mode 100644
+index 0000000..8f444b4
+--- /dev/null
++++ b/server/Dockerfile
+@@ -0,0 +1,15 @@
++FROM eclipse-temurin:17-jdk AS build
++RUN apt-get update \
++    && apt-get install -y --no-install-recommends libatomic1 \
++    && rm -rf /var/lib/apt/lists/*
++WORKDIR /src
++COPY . .
++RUN ./gradlew :server:buildFatJar --no-daemon
++
++FROM eclipse-temurin:21-jre
++WORKDIR /app
++COPY --from=build /src/server/build/libs/*-all.jar /app/server.jar
++ENV MATUTINE_DATA=/data
++EXPOSE 8080
++VOLUME /data
++ENTRYPOINT ["java", "-jar", "/app/server.jar"]
+diff --git a/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
+new file mode 100644
+index 0000000..4ab6443
+--- /dev/null
++++ b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
+@@ -0,0 +1,96 @@
++package hu.toliver.matutine
++
++import hu.toliver.matutine.edition.CalDavAccount
++import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionRole
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SourceList
++import hu.toliver.matutine.edition.decodeCalDavAccount
++import hu.toliver.matutine.edition.decodeCalendarSnapshot
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.decodeReaderSettings
++import hu.toliver.matutine.edition.decodeSourceList
++import hu.toliver.matutine.edition.encodeCalDavAccount
++import hu.toliver.matutine.edition.encodeCalendarSnapshot
++import hu.toliver.matutine.edition.encodeEdition
++import hu.toliver.matutine.edition.encodeReaderSettings
++import hu.toliver.matutine.edition.encodeSourceList
++import java.io.File
++
++/**
++ * Same document names as the desktop file store, rooted at the server data directory.
++ * The server does not depend on `:app:shared` because that module pulls Compose.
++ */
++class DataDirectoryStore(
++    rootPath: String,
++) : EditionStore {
++    private val root = File(rootPath)
++
++    init {
++        root.mkdirs()
++    }
++
++    override fun readSourceList(): SourceList? = read("sources.json", ::decodeSourceList)
++
++    override fun writeSourceList(sourceList: SourceList) {
++        write("sources.json", encodeSourceList(sourceList))
++    }
++
++    override fun readSettings(): ReaderSettings? = read("settings.json", ::decodeReaderSettings)
++
++    override fun writeSettings(settings: ReaderSettings) {
++        write("settings.json", encodeReaderSettings(settings))
++    }
++
++    override fun loadMorningEdition(dayKey: String): Edition? =
++        read("editions/$dayKey-morning.json", ::decodeEdition)
++
++    override fun saveMorningEdition(edition: Edition) {
++        require(edition.role == EditionRole.morning)
++        write("editions/${edition.dayKey}-morning.json", encodeEdition(edition))
++    }
++
++    override fun loadExtras(dayKey: String): List<Edition> {
++        val dir = File(root, "editions")
++        if (!dir.exists()) return emptyList()
++        val prefix = "extra-$dayKey-"
++        return dir.listFiles().orEmpty()
++            .filter { it.isFile && it.name.startsWith(prefix) && it.name.endsWith(".json") }
++            .map { decodeEdition(it.readText()) }
++            .filter { it.role == EditionRole.extra && it.dayKey == dayKey }
++            .sortedBy { it.id.substringAfterLast("-").toIntOrNull() ?: 0 }
++    }
++
++    override fun saveExtra(edition: Edition) {
++        require(edition.role == EditionRole.extra)
++        require(edition.id.startsWith("extra-${edition.dayKey}-"))
++        write("editions/${edition.id}.json", encodeEdition(edition))
++    }
++
++    override fun readCalendarSnapshot(): CalendarSnapshot? =
++        read("calendar-snapshot.json", ::decodeCalendarSnapshot)
++
++    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
++        write("calendar-snapshot.json", encodeCalendarSnapshot(snapshot))
++    }
++
++    override fun readCalDavAccount(): CalDavAccount? = read("caldav.json", ::decodeCalDavAccount)
++
++    override fun writeCalDavAccount(account: CalDavAccount) {
++        write("caldav.json", encodeCalDavAccount(account))
++    }
++
++    private fun <T> read(relativePath: String, decode: (String) -> T): T? {
++        val file = File(root, relativePath)
++        if (!file.exists()) return null
++        return decode(file.readText())
++    }
++
++    private fun write(relativePath: String, json: String) {
++        val file = File(root, relativePath)
++        file.parentFile?.mkdirs()
++        file.writeText(json)
++    }
++}
+
+```
diff --git a/_bmad-output/implementation-artifacts/review-3-1-edge-case-hunter-prompt.md b/_bmad-output/implementation-artifacts/review-3-1-edge-case-hunter-prompt.md
new file mode 100644
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/review-3-1-edge-case-hunter-prompt.md
+Read the review instructions below completely and follow them.
+
+claims_file: the spec is inlined at the end of this prompt. Leave that block unread until your instructions call for it.
+
+Review content: the unified diff below. It is the content under review.
+
+Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. If the instruction file is unreadable, report that exact failure and stop. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
+
+## Review instructions
+
+# Edge Case Hunter Review
+
+**Goal:** You are a pure path tracer. Never comment on whether code is good or bad; only list missing handling.
+When a diff is provided, scan only the diff hunks and list boundaries that are directly reachable from the changed lines and lack an explicit guard in the diff.
+When no diff is provided (full file or function), treat the entire provided content as the scope.
+Ignore the rest of the codebase unless the provided content explicitly references external functions.
+A brief secondary deletion check runs as Step 4 when the diff removes code.
+A claims check runs as Step 5.
+
+**Inputs:**
+- **content** — Content to review, or a path to read it from: diff, full file, or function
+- **also_consider** (optional) — Areas to keep in mind during review alongside normal edge-case analysis
+- **claims_file** — Path to the spec this change was built from. Do NOT read it before Step 5: the path tracing in Steps 2–3 must finish before the claims are seen.
+
+**MANDATORY: Execute steps in the Execution section IN EXACT ORDER. DO NOT skip steps or change the sequence. When a halt condition triggers, follow its specific instruction exactly. Each action within a step is a REQUIRED action to complete that step.**
+
+**Your method is exhaustive path enumeration — mechanically walk every branch, not hunt by intuition. Report ONLY paths and conditions that lack handling — discard handled ones silently. Do NOT editorialize or add filler. Do not assign severity labels, rankings, or priority levels.**
+
+
+## EXECUTION
+
+### Step 1: Receive Content
+
+- Take the content to review from the parent message that launched you — inline, or by reading the file it points to (never from this instruction file)
+- If no content is supplied, or it is empty, unreadable, or cannot be decoded as text, return `[{"location":"N/A","trigger_condition":"Input empty or undecodable","guard_snippet":"Provide valid content to review","potential_consequence":"Review skipped — no analysis performed"}]` and stop
+- Identify content type (diff, full file, or function) to determine scope rules
+
+### Step 2: Exhaustive Path Analysis
+
+**Walk every branching path and boundary condition within scope — report only unhandled ones.**
+
+- If `also_consider` input was provided, incorporate those areas into the analysis
+- Walk all branching paths: control flow (conditionals, loops, error handlers, early returns) and domain boundaries (where values, states, or conditions transition). Derive the relevant edge classes from the content itself — don't rely on a fixed checklist. Examples: missing else/default, unguarded inputs, off-by-one loops, arithmetic overflow, implicit type coercion, race conditions, timeout gaps
+- Consider implicit branches: the diff special-cases or changes the handling of one or more members of a fixed set of values — enums, status codes, sentinels, type tags, flags, value ranges. The rest of the set is implicit branches (e.g. the diff changes the `RED` and `YELLOW` cases of a `RED`/`YELLOW`/`GREEN` enum; `GREEN` is the implicit branch)
+- Consider handle lifetime: when the changed code re-checks, re-fetches, or re-validates something it already held — a handle, index, id, pointer — the re-check exists because an intervening call can invalidate it. Identify that call, what it does to the thing held, and what the changed code silently skips when the re-check fails
+- For each call site the diff adds or changes — in test files as well as production code — read the callee's declaration and check the call against it: argument count, order, types, and defaults. Report any mismatch
+- For each path: determine whether the content handles it
+- Collect only the unhandled paths as findings — discard handled ones silently
+
+### Step 3: Validate Completeness
+
+- Revisit every edge class from Step 2 — e.g., missing else/default, null/empty inputs, off-by-one loops, arithmetic overflow, implicit type coercion, race conditions, timeout gaps
+- Add any newly found unhandled paths to findings; discard confirmed-handled ones
+
+### Step 4: Deletion Check
+
+If the diff removed or replaced meaningful code (ignore pure renames and whitespace): load `references/deletion-check.md` and follow it.
+
+### Step 5: Claims Check
+
+Load `references/claims-check.md` and follow it.
+
+### Step 6: Present Findings
+
+Output all findings as a single JSON array following the Output Format specification exactly.
+
+
+## OUTPUT FORMAT
+
+Return ONLY a valid JSON array of objects. Each edge-case finding contains exactly these four fields:
+
+```json
+[{
+  "location": "file:start-end (or file:line when single line, or file:hunk when exact line unavailable)",
+  "trigger_condition": "one-line description (max 15 words)",
+  "guard_snippet": "minimal code sketch that closes the gap (single-line escaped string, no raw newlines or unescaped quotes)",
+  "potential_consequence": "what could actually go wrong (max 15 words)"
+}]
+```
+
+No extra text, no explanations, no markdown wrapping. An empty array `[]` is valid when nothing is found. Deletion findings from Step 4 and claim findings from Step 5, if any, go in the same array with the extra fields defined in `references/deletion-check.md` and `references/claims-check.md`.
+
+
+## HALT CONDITIONS
+
+- If no content is supplied, or it is empty, unreadable, or cannot be decoded as text, return `[{"location":"N/A","trigger_condition":"Input empty or undecodable","guard_snippet":"Provide valid content to review","potential_consequence":"Review skipped — no analysis performed"}]` and stop
+<reference path="references/deletion-check.md">
+# Deletion Check
+
+Secondary pass for the Edge Case Hunter — runs only when the diff removed meaningful code. Subordinate to the edge-case pass; findings are usually few or none.
+
+For each chunk of removed or replaced code (ignore pure renames and whitespace), ask: did it carry behavior or a contract that the change neither re-established nor intentionally retired? Add a finding for any resulting regression, orphaned reference, or newly-dead code. Skip anything already covered by your edge-case findings.
+
+Append each finding to the same JSON array as the edge-case findings, with the four standard fields plus:
+
+- `kind`: `"deletion"`
+- `confidence`: `"high"`, `"medium"`, or `"low"` — these are inferences; rate them
+
+For a deletion finding the standard fields read as: `location` = the removed item; `trigger_condition` = the behavior or contract it enforced; `guard_snippet` = where or how to re-establish it; `potential_consequence` = the regression or orphan.
+
+Add nothing if nothing qualifies.
+</reference>
+<reference path="references/claims-check.md">
+# Claims Check
+
+Final pass for the Edge Case Hunter. Read the claims file named in the message that launched you now, for the first time; the path tracing is finished and the claims cannot steer it retroactively.
+
+It is the spec the change was built from. Read only its `## Intent` and `## Tasks & Acceptance` sections — the claims live there; ignore the rest of the file. The spec is the change's own account of itself: testimony, not evidence — a claim repeated in a code comment is still the same claim, not confirmation. Extract each checkable claim — what the change does, what it preserves, ordering, arithmetic, and parity with existing code ("exactly as X does") — then try to falsify each one against the code you have already traced. Where your trace is not enough to decide, read the code that decides it: the compared-to function, the actual callee, the state the claim assumes.
+
+Append one finding per falsified claim to the same JSON array, with the four standard fields plus:
+
+- `kind`: `"claim"`
+- `confidence`: `"high"`, `"medium"`, or `"low"`
+
+For a claim finding the standard fields read as: `location` = where the code contradicts the claim; `trigger_condition` = the claim, quoted or tightly paraphrased; `guard_snippet` = what the code actually does; `potential_consequence` = what goes wrong for someone who believed the claim.
+
+Verified claims produce nothing. Add nothing if nothing is falsified.
+</reference>
+
+## CONTENT SOURCE
+
+"Review content:" in the message that launched you gives the content itself or a path to read it from. Read the file when it is a path; either way that is the content under review, and this instruction file never is.
+
+
+## Unified diff
+
+```diff
+diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
+index 860caa7..30de6a2 100644
+--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
++++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
+@@ -29,7 +29,7 @@
+ # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
+ # - Retrospective appends its action items to action_items; the status view surfaces open ones
+ generated: 11-07-2026 19:42
+-last_updated: 11-10-2026 18:48
++last_updated: 11-10-2026 18:55
+ project: Matutine
+ project_key: NOKEY
+ tracking_system: file-system
+@@ -59,8 +59,8 @@ development_status:
+   2-7-write-the-briefing-with-a-gemini-key: done
+   epic-2-retrospective: optional
+ 
+-  epic-3: backlog
+-  3-1-open-the-server-s-paper-in-the-browser: backlog
++  epic-3: in-progress
++  3-1-open-the-server-s-paper-in-the-browser: in-progress
+   3-2-generate-the-morning-edition-on-the-server-clock: backlog
+   3-3-configure-the-server-in-admin: backlog
+   3-4-light-edit-a-block-in-the-browser: backlog
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+index c06cc9b..e418217 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+@@ -72,6 +72,7 @@ fun EditionScreen(
+     onNewEditionNow: () -> Unit = {},
+     onOpenExtra: (String) -> Unit = {},
+     onOpenMorning: () -> Unit = {},
++    readOnly: Boolean = false,
+     modifier: Modifier = Modifier,
+ ) {
+     val typography = LocalMatutineEditionTypography.current
+@@ -82,6 +83,7 @@ fun EditionScreen(
+             generating = state.generating,
+             notice = state.notice,
+             onOpenSettings = onOpenSettings,
++            readOnly = readOnly,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -104,6 +106,7 @@ fun EditionScreen(
+             onNewEditionNow = onNewEditionNow,
+             onOpenExtra = onOpenExtra,
+             onOpenMorning = onOpenMorning,
++            readOnly = readOnly,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -175,6 +178,7 @@ private fun EmptyFrontPage(
+     generating: Boolean,
+     notice: String?,
+     onOpenSettings: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -188,11 +192,13 @@ private fun EmptyFrontPage(
+             .padding(horizontal = 16.dp, vertical = 16.dp),
+         verticalArrangement = Arrangement.spacedBy(16.dp),
+     ) {
+-        TextButton(
+-            onClick = onOpenSettings,
+-            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+-        ) {
+-            Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++        if (!readOnly) {
++            TextButton(
++                onClick = onOpenSettings,
++                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++            ) {
++                Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++            }
+         }
+         Masthead(name = "Matutine", dateLine = "", typography = typography)
+         if (generating) {
+@@ -229,6 +235,7 @@ private fun EditionContent(
+     onNewEditionNow: () -> Unit,
+     onOpenExtra: (String) -> Unit,
+     onOpenMorning: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -248,29 +255,31 @@ private fun EditionContent(
+                 .padding(horizontal = 16.dp, vertical = 16.dp),
+             verticalArrangement = Arrangement.spacedBy(16.dp),
+         ) {
+-            Row(
+-                horizontalArrangement = Arrangement.spacedBy(16.dp),
+-                verticalAlignment = Alignment.CenterVertically,
+-            ) {
+-                TextButton(
+-                    onClick = onOpenSettings,
+-                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++            if (!readOnly) {
++                Row(
++                    horizontalArrangement = Arrangement.spacedBy(16.dp),
++                    verticalAlignment = Alignment.CenterVertically,
+                 ) {
+-                    Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
+-                }
+-                if (onOpenTypewriter != null) {
+                     TextButton(
+-                        onClick = onOpenTypewriter,
++                        onClick = onOpenSettings,
+                         modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                     ) {
+-                        Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                        Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                    }
++                    if (onOpenTypewriter != null) {
++                        TextButton(
++                            onClick = onOpenTypewriter,
++                            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                        ) {
++                            Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                        }
++                    }
++                    TextButton(
++                        onClick = onNewEditionNow,
++                        modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                    ) {
++                        Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                     }
+-                }
+-                TextButton(
+-                    onClick = onNewEditionNow,
+-                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+-                ) {
+-                    Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                 }
+             }
+             if (edition.role == EditionRole.extra) {
+@@ -316,10 +325,10 @@ private fun EditionContent(
+             }
+             if (edition.isSample) {
+                 Text(
+-                    text = "SAMPLE EDITION",
++                    text = EditionTranscript.SAMPLE,
+                     style = typography.kicker.copy(color = EditionThemeColors.current.accent),
+                 )
+-            } else if (showNoTopicLine) {
++            } else if (showNoTopicLine && !readOnly) {
+                 TextButton(
+                     onClick = onOpenInterests,
+                     modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+@@ -339,6 +348,7 @@ private fun EditionContent(
+                     onAddTask = onAddTask,
+                     calendarPermissionOff = calendarPermissionOff,
+                     onOpenCalendarPermission = onOpenCalendarPermission,
++                    readOnly = readOnly,
+                     typography = typography,
+                 )
+             } else {
+@@ -351,6 +361,7 @@ private fun EditionContent(
+                     taskSaved = taskSaved,
+                     onToggleTask = onToggleTask,
+                     onAddTask = onAddTask,
++                    readOnly = readOnly,
+                     typography = typography,
+                 )
+             }
+@@ -366,6 +377,7 @@ private fun WideEditionBody(
+     onAddTask: (String) -> Unit,
+     calendarPermissionOff: Boolean,
+     onOpenCalendarPermission: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+ ) {
+     Spacer(Modifier.height(8.dp))
+@@ -395,6 +407,7 @@ private fun WideEditionBody(
+                 taskSaved = taskSaved,
+                 onToggleTask = onToggleTask,
+                 onAddTask = onAddTask,
++                readOnly = readOnly,
+                 typography = typography,
+             )
+         }
+@@ -562,6 +575,7 @@ private fun TaskBlock(
+     taskSaved: Boolean,
+     onToggleTask: (Int) -> Unit,
+     onAddTask: (String) -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+ ) {
+     var draft by remember { mutableStateOf("") }
+@@ -571,18 +585,25 @@ private fun TaskBlock(
+             Text(text = EditionCopy.NO_TASKS, style = typography.body)
+         } else {
+             tasks.forEachIndexed { index, task ->
+-                TaskRow(task, typography, onToggle = { onToggleTask(index) })
++                TaskRow(
++                    task,
++                    typography,
++                    onToggle = { onToggleTask(index) },
++                    readOnly = readOnly,
++                )
+             }
+         }
+-        TaskAddLine(
+-            value = draft,
+-            onValueChange = { draft = it },
+-            onConfirm = {
+-                onAddTask(draft)
+-                draft = ""
+-            },
+-            typography = typography,
+-        )
++        if (!readOnly) {
++            TaskAddLine(
++                value = draft,
++                onValueChange = { draft = it },
++                onConfirm = {
++                    onAddTask(draft)
++                    draft = ""
++                },
++                typography = typography,
++            )
++        }
+         if (taskSaved) {
+             Text(text = EditionCopy.SAVED, style = typography.meta)
+         }
+@@ -624,6 +645,7 @@ private fun TaskRow(
+     task: EditionTask,
+     typography: MatutineEditionTypography,
+     onToggle: () -> Unit,
++    readOnly: Boolean,
+ ) {
+     val touchTarget = if (editionForceSingleColumn()) {
+         Modifier.minimumInteractiveComponentSize()
+@@ -636,10 +658,16 @@ private fun TaskRow(
+         modifier = Modifier
+             .padding(vertical = 4.dp)
+             .then(touchTarget)
+-            .clickable(
+-                interactionSource = remember { MutableInteractionSource() },
+-                indication = null,
+-                onClick = onToggle,
++            .then(
++                if (readOnly) {
++                    Modifier
++                } else {
++                    Modifier.clickable(
++                        interactionSource = remember { MutableInteractionSource() },
++                        indication = null,
++                        onClick = onToggle,
++                    )
++                },
+             ),
+     ) {
+         Box(
+diff --git a/app/webApp/build.gradle.kts b/app/webApp/build.gradle.kts
+index 4d8b955..6bbc79c 100644
+--- a/app/webApp/build.gradle.kts
++++ b/app/webApp/build.gradle.kts
+@@ -22,7 +22,11 @@ kotlin {
+         commonMain.dependencies {
+             implementation(project(":app:shared"))
+ 
++            implementation(libs.compose.runtime)
++            implementation(libs.compose.foundation)
++            implementation(libs.compose.material3)
+             implementation(libs.compose.ui)
++            implementation(libs.kotlinx.coroutines.core)
+         }
+     }
+ }
+\ No newline at end of file
+diff --git a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
+index f0b181a..e9da8b9 100644
+--- a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
++++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
+@@ -1,11 +1,142 @@
+ package hu.toliver.matutine
+ 
++import androidx.compose.foundation.background
++import androidx.compose.foundation.layout.Arrangement
++import androidx.compose.foundation.layout.Column
++import androidx.compose.foundation.layout.fillMaxSize
++import androidx.compose.foundation.layout.fillMaxWidth
++import androidx.compose.foundation.layout.heightIn
++import androidx.compose.foundation.layout.padding
++import androidx.compose.foundation.shape.RoundedCornerShape
++import androidx.compose.material3.Button
++import androidx.compose.material3.ButtonDefaults
++import androidx.compose.material3.OutlinedTextField
++import androidx.compose.material3.Text
++import androidx.compose.runtime.Composable
++import androidx.compose.runtime.getValue
++import androidx.compose.runtime.mutableStateOf
++import androidx.compose.runtime.remember
++import androidx.compose.runtime.rememberCoroutineScope
++import androidx.compose.runtime.setValue
+ import androidx.compose.ui.ExperimentalComposeUiApi
++import androidx.compose.ui.Modifier
++import androidx.compose.ui.unit.dp
+ import androidx.compose.ui.window.ComposeViewport
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionScreen
++import hu.toliver.matutine.edition.EditionUiState
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.webPaperGate
++import hu.toliver.matutine.theme.EditionThemeColors
++import hu.toliver.matutine.theme.LocalMatutineEditionTypography
++import hu.toliver.matutine.theme.MatutineTheme
++import hu.toliver.matutine.theme.NewsprintColors
++import kotlinx.coroutines.launch
+ 
+ @OptIn(ExperimentalComposeUiApi::class)
+ fun main() {
+     ComposeViewport {
+-        App()
++        ServerPaperApp()
+     }
+-}
+\ No newline at end of file
++}
++
++@Composable
++fun ServerPaperApp() {
++    var typedToken by remember { mutableStateOf("") }
++    var message by remember { mutableStateOf<String?>(null) }
++    var edition by remember { mutableStateOf<Edition?>(null) }
++    var submitting by remember { mutableStateOf(false) }
++    val scope = rememberCoroutineScope()
++    MatutineTheme {
++        val paper = edition
++        if (paper == null) {
++            TokenGate(
++                token = typedToken,
++                onTokenChange = { typedToken = it },
++                message = message,
++                submitting = submitting,
++                onSubmit = {
++                    if (submitting) return@TokenGate
++                    submitting = true
++                    scope.launch {
++                        val decision = try {
++                            val response = fetchCurrentEdition(typedToken)
++                            val gate = webPaperGate(typedToken, response.status)
++                            typedToken = gate.typedToken
++                            message = gate.message
++                            if (gate.showEdition) {
++                                decodeEdition(response.body)
++                            } else {
++                                null
++                            }
++                        } catch (_: Throwable) {
++                            message = null
++                            null
++                        }
++                        edition = decision
++                        submitting = false
++                    }
++                },
++            )
++        } else {
++            EditionScreen(
++                state = EditionUiState.Ready(paper),
++                onRetrySample = {},
++                onOpenSettings = {},
++                showNoTopicLine = false,
++                onOpenInterests = {},
++                onToggleTask = {},
++                onAddTask = {},
++                readOnly = true,
++            )
++        }
++    }
++}
++
++@Composable
++private fun TokenGate(
++    token: String,
++    onTokenChange: (String) -> Unit,
++    message: String?,
++    submitting: Boolean,
++    onSubmit: () -> Unit,
++) {
++    val typography = LocalMatutineEditionTypography.current
++    Column(
++        modifier = Modifier
++            .fillMaxSize()
++            .background(NewsprintColors.chrome)
++            .padding(24.dp),
++        verticalArrangement = Arrangement.spacedBy(16.dp),
++    ) {
++        Text(text = "Token", style = typography.body.copy(color = EditionThemeColors.current.ink))
++        OutlinedTextField(
++            value = token,
++            onValueChange = onTokenChange,
++            modifier = Modifier
++                .fillMaxWidth()
++                .heightIn(min = 40.dp),
++            singleLine = true,
++            textStyle = typography.body.copy(color = EditionThemeColors.current.ink),
++            shape = RoundedCornerShape(2.dp),
++        )
++        if (!message.isNullOrBlank()) {
++            Text(
++                text = message,
++                style = typography.body.copy(color = EditionThemeColors.current.error),
++            )
++        }
++        Button(
++            onClick = onSubmit,
++            enabled = !submitting,
++            modifier = Modifier.heightIn(min = 40.dp),
++            shape = RoundedCornerShape(2.dp),
++            colors = ButtonDefaults.buttonColors(
++                containerColor = EditionThemeColors.current.ink,
++                contentColor = EditionThemeColors.current.paper,
++            ),
++        ) {
++            Text(text = "Continue")
++        }
++    }
++}
+diff --git a/server/build.gradle.kts b/server/build.gradle.kts
+index bbd6ccd..1462455 100644
+--- a/server/build.gradle.kts
++++ b/server/build.gradle.kts
+@@ -16,4 +16,15 @@ dependencies {
+     implementation(libs.ktor.serverNetty)
+     testImplementation(libs.ktor.serverTestHost)
+     testImplementation(libs.kotlin.testJunit)
++}
++
++val webDistribution = project(":app:webApp").layout.buildDirectory.dir(
++    "dist/wasmJs/productionExecutable",
++)
++
++tasks.named<ProcessResources>("processResources") {
++    dependsOn(":app:webApp:wasmJsBrowserDistribution")
++    from(webDistribution) {
++        into("web")
++    }
+ }
+\ No newline at end of file
+diff --git a/server/src/main/kotlin/hu/toliver/matutine/Application.kt b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
+index 25b8388..c2cafe8 100644
+--- a/server/src/main/kotlin/hu/toliver/matutine/Application.kt
++++ b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
+@@ -1,10 +1,33 @@
+ package hu.toliver.matutine
+ 
+-import io.ktor.server.application.*
+-import io.ktor.server.engine.*
+-import io.ktor.server.netty.*
+-import io.ktor.server.response.*
+-import io.ktor.server.routing.*
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.LocalEditionRepository
++import hu.toliver.matutine.edition.encodeEdition
++import io.ktor.http.ContentType
++import io.ktor.http.HttpStatusCode
++import io.ktor.server.application.Application
++import io.ktor.server.application.call
++import io.ktor.server.engine.embeddedServer
++import io.ktor.server.netty.Netty
++import io.ktor.server.response.respondText
++import io.ktor.server.routing.get
++import io.ktor.server.routing.routing
++import io.ktor.server.http.content.staticResources
++import io.ktor.util.AttributeKey
++
++private val SharedTokenKey = AttributeKey<String>("matutine.sharedToken")
++private val DocumentStoreKey = AttributeKey<EditionStore>("matutine.documents")
++
++fun resolveSharedToken(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "matutine-demo"
++
++fun resolveDataRoot(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "/data"
++
++internal fun bearerMatches(authorization: String?, token: String): Boolean {
++    val prefix = "Bearer "
++    if (authorization == null || !authorization.startsWith(prefix)) return false
++    val presented = authorization.substring(prefix.length)
++    return presented.isNotEmpty() && presented == token
++}
+ 
+ fun main() {
+     embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
+@@ -12,9 +35,34 @@ fun main() {
+ }
+ 
+ fun Application.module() {
++    val token = attributes.getOrNull(SharedTokenKey)
++        ?: resolveSharedToken(System.getenv("MATUTINE_TOKEN"))
++    val documents = attributes.getOrNull(DocumentStoreKey)
++        ?: DataDirectoryStore(resolveDataRoot(System.getenv("MATUTINE_DATA")))
++    val repository = LocalEditionRepository(documents)
+     routing {
+-        get("/") {
+-            call.respondText(sayHello("Ktor"))
++        get("/editions/current") {
++            val authorization = call.request.headers["Authorization"]
++            if (!bearerMatches(authorization, token)) {
++                call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
++                return@get
++            }
++            val edition = repository.loadCurrentMorningEdition().getOrElse { error ->
++                call.respondText(
++                    error.message ?: "unavailable",
++                    status = HttpStatusCode.NotFound,
++                )
++                return@get
++            }
++            call.respondText(encodeEdition(edition), ContentType.Application.Json)
++        }
++        staticResources("/", "web") {
++            default("index.html")
+         }
+     }
+-}
+\ No newline at end of file
++}
++
++fun Application.installTestDocuments(token: String, documents: EditionStore) {
++    attributes.put(SharedTokenKey, token)
++    attributes.put(DocumentStoreKey, documents)
++}
+diff --git a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+index 23dc320..fc711d4 100644
+--- a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
++++ b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+@@ -1,20 +1,194 @@
+ package hu.toliver.matutine
+ 
+-import io.ktor.client.request.*
+-import io.ktor.client.statement.*
+-import io.ktor.http.*
+-import io.ktor.server.testing.*
+-import kotlin.test.*
++import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionRole
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.EditionTranscript
++import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SourceList
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.todayLocalDate
++import hu.toliver.matutine.edition.toDayKey
++import io.ktor.client.request.get
++import io.ktor.client.request.header
++import io.ktor.client.statement.bodyAsText
++import io.ktor.http.HttpStatusCode
++import io.ktor.server.testing.testApplication
++import java.nio.file.Files
++import kotlin.test.Test
++import kotlin.test.assertEquals
++import kotlin.test.assertFalse
++import kotlin.test.assertNull
++import kotlin.test.assertTrue
+ 
+ class ApplicationTest {
+ 
+     @Test
+-    fun testRoot() = testApplication {
++    fun unsetToken_acceptsTheDemoBearer() = testApplication {
++        val root = Files.createTempDirectory("matutine-demo").toFile()
+         application {
++            installTestDocuments(resolveSharedToken(null), DataDirectoryStore(root.absolutePath))
+             module()
+         }
+-        val response = client.get("/")
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
+         assertEquals(HttpStatusCode.OK, response.status)
+-        assertEquals("Hello, Ktor!", response.bodyAsText())
++        assertTrue(decodeEdition(response.bodyAsText()).isSample)
++    }
++
++    @Test
++    fun blankToken_isTheDemoBearer() {
++        assertEquals("matutine-demo", resolveSharedToken(null))
++        assertEquals("matutine-demo", resolveSharedToken(""))
++        assertEquals("matutine-demo", resolveSharedToken("   "))
++        assertEquals("/data", resolveDataRoot(null))
++        assertEquals("/data", resolveDataRoot(""))
+     }
+-}
+\ No newline at end of file
++
++    @Test
++    fun setToken_rejectsEveryOtherBearerBeforeReadingDocuments() = testApplication {
++        val store = TouchingStore()
++        application {
++            installTestDocuments(resolveSharedToken("lan-secret"), store)
++            module()
++        }
++        val missing = client.get("/editions/current")
++        val wrongScheme = client.get("/editions/current") {
++            header("Authorization", "Token lan-secret")
++        }
++        val wrongToken = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        assertEquals(HttpStatusCode.Unauthorized, missing.status)
++        assertEquals(HttpStatusCode.Unauthorized, wrongScheme.status)
++        assertEquals(HttpStatusCode.Unauthorized, wrongToken.status)
++        assertFalse(missing.bodyAsText().contains("isSample"))
++        assertFalse(wrongToken.bodyAsText().contains("mastheadName"))
++        assertEquals(0, store.touches)
++    }
++
++    @Test
++    fun setToken_acceptsThatBearer() = testApplication {
++        val root = Files.createTempDirectory("matutine-set-token").toFile()
++        application {
++            installTestDocuments(resolveSharedToken("lan-secret"), DataDirectoryStore(root.absolutePath))
++            module()
++        }
++        val accepted = client.get("/editions/current") {
++            header("Authorization", "Bearer lan-secret")
++        }
++        assertEquals(HttpStatusCode.OK, accepted.status)
++        assertTrue(decodeEdition(accepted.bodyAsText()).isSample)
++    }
++
++    @Test
++    fun absentSourceList_returnsTheServerSampleAndDoesNotWriteTheList() = testApplication {
++        val root = Files.createTempDirectory("matutine-sample").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        application {
++            installTestDocuments("matutine-demo", store)
++            module()
++        }
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        val edition = decodeEdition(response.bodyAsText())
++        assertEquals(HttpStatusCode.OK, response.status)
++        assertTrue(edition.isSample)
++        assertTrue(EditionTranscript.render(edition).contains(EditionTranscript.SAMPLE))
++        assertNull(store.readSourceList())
++        assertFalse(root.resolve("sources.json").exists())
++        assertTrue(root.resolve("editions/${edition.dayKey}-morning.json").exists())
++        val again = decodeEdition(
++            client.get("/editions/current") {
++                header("Authorization", "Bearer matutine-demo")
++            }.bodyAsText(),
++        )
++        assertEquals(edition.id, again.id)
++        assertEquals(edition.revision, again.revision)
++    }
++
++    @Test
++    fun existingMorning_isReturnedUnchanged() = testApplication {
++        val root = Files.createTempDirectory("matutine-kept").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        val day = todayLocalDate()
++        val morning = Edition(
++            id = "morning-${day.toDayKey()}",
++            revision = 4,
++            dayKey = day.toDayKey(),
++            role = EditionRole.morning,
++            mastheadName = "Matutine",
++            dateLine = "Kept date",
++            isSample = false,
++            briefing = "Kept by hand",
++            stories = emptyList(),
++            events = emptyList(),
++            tasks = emptyList(),
++        )
++        store.saveMorningEdition(morning)
++        application {
++            installTestDocuments("matutine-demo", store)
++            module()
++        }
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        val edition = decodeEdition(response.bodyAsText())
++        assertEquals(morning, edition)
++        assertEquals(morning, store.loadMorningEdition(day.toDayKey()))
++    }
++
++    @Test
++    fun documents_areFilesUnderTheDataDirectory() {
++        val root = Files.createTempDirectory("matutine-docs").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        store.writeSettings(ReaderSettings())
++        store.writeSourceList(SourceList(id = "sources", revision = 1))
++        store.writeCalendarSnapshot(CalendarSnapshot())
++        val day = todayLocalDate()
++        store.saveMorningEdition(
++            Edition(
++                id = "morning-${day.toDayKey()}",
++                revision = 1,
++                dayKey = day.toDayKey(),
++                role = EditionRole.morning,
++                mastheadName = "Matutine",
++                dateLine = "Date",
++                isSample = false,
++                briefing = "Brief",
++                stories = emptyList(),
++                events = emptyList(),
++                tasks = emptyList(),
++            ),
++        )
++        assertTrue(root.resolve("settings.json").isFile)
++        assertTrue(root.resolve("sources.json").isFile)
++        assertTrue(root.resolve("calendar-snapshot.json").isFile)
++        assertTrue(root.resolve("editions/${day.toDayKey()}-morning.json").isFile)
++    }
++}
++
++private class TouchingStore : EditionStore {
++    var touches: Int = 0
++
++    private fun touch(): Nothing {
++        touches += 1
++        error("store touched")
++    }
++
++    override fun readSourceList() = touch()
++    override fun writeSourceList(sourceList: SourceList) = touch()
++    override fun readSettings() = touch()
++    override fun writeSettings(settings: ReaderSettings) = touch()
++    override fun loadMorningEdition(dayKey: String) = touch()
++    override fun saveMorningEdition(edition: Edition) = touch()
++    override fun loadExtras(dayKey: String) = touch()
++    override fun saveExtra(edition: Edition) = touch()
++    override fun readCalendarSnapshot() = touch()
++    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = touch()
++    override fun readCalDavAccount() = touch()
++    override fun writeCalDavAccount(account: hu.toliver.matutine.edition.CalDavAccount) = touch()
++}
+diff --git a/.dockerignore b/.dockerignore
+new file mode 100644
+index 0000000..c100704
+--- /dev/null
++++ b/.dockerignore
+@@ -0,0 +1,9 @@
++.git
++.gradle
++**/.gradle
++**/build
++.idea
++_bmad
++_bmad-output
++*.md
++!README.md
+diff --git a/_bmad-output/implementation-artifacts/epic-3-context.md b/_bmad-output/implementation-artifacts/epic-3-context.md
+new file mode 100644
+index 0000000..1b24060
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/epic-3-context.md
+@@ -0,0 +1,56 @@
++# Epic 3 Context: Read the server's morning edition
++
++<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->
++
++## Goal
++
++O runs one Docker server on the LAN and reads that same morning edition in the browser and on the phone. Admin is where the server is configured. Local papers are not uploaded when a client points at the server. The edit-survives-regenerate rule still applies to editions the server owns.
++
++## Stories
++
++- Story 3.1: Open the server's paper in the browser
++- Story 3.2: Generate the morning edition on the server clock
++- Story 3.3: Configure the server in Admin
++- Story 3.4: Light-edit a block in the browser
++- Story 3.5: Read the server edition on Android
++- Story 3.6: Read the server edition on Desktop
++- Story 3.7: Document the server judge path
++- Story 3.8: Publish the Docker image
++
++## Requirements & Constraints
++
++- One shared token, no accounts, no push. Unset token is the literal `matutine-demo` on the server and on every remote client. The token is required before the newspaper or Admin renders. A mismatch says "That token doesn't match." and keeps the typed value. The same token covers the newspaper and Admin. Every remote call sends `Authorization: Bearer`.
++- The shipped server is a Docker image running the Ktor process. Token, AI key, and timezone come from the process environment, not the image. Edition, settings, source list, and calendar snapshot live in a host directory mounted at `/data`, not in the image layer. Do not document a public port. `java -jar` is not a required launch.
++- When the source-list document is absent, the generator builds an edition from bundled sample feeds and sample calendar data, shows the sample line, and does not write those samples into the list. An empty list is a real configuration and does not fall back to samples. No account and no AI key are required to see a paper.
++- The web client does not run the generator and is not an offline app. Web has no Settings screen. Admin is the web-only place for feeds, ICS, CalDAV, generation time, timezone, interest questions, AI provider and key, and the shared token.
++- The server inserts today's morning edition at the configured time only when that document is absent. It does not replace an existing morning edition. A clock-precise paper is claimed only while this server is running. The server holds the AI key; remote clients do not. The timezone value is whatever the process was given; do not invent an identifier format.
++- Saving a server base URL and token does not upload local editions, the local source list, or the local calendar snapshot. Remote clients read, edit, regenerate, or request an extra. They do not mint the morning edition. If the server cannot be reached, show the last stored edition and "Couldn't reach the server. Showing the copy on this device." Generate stays available and writes locally. Android accepts a cleartext `http` LAN URL. Notification time, notification text, theme, and layout stay on the phone. Feeds, calendar, and interest questions are not editable on the phone while a server is configured.
++- Web light edit changes one block in place and saves on leave. Last write wins. There is no merge screen and no revision number on the page. A failed save says "Couldn't save. Your text is still here." Changing the printed date does not change the day key.
++- User-owned text on a server-owned edition survives regenerate the same way it does on the device. Whole-document replace stays the write model.
++- Milestone B also publishes the Docker image from the existing release workflow. Secrets stay in the environment.
++
++## Technical Decisions
++
++- Modules stay `shared`, `server`, `androidApp`, `desktopApp`, and `webApp`. `server` owns the scheduler, HTTP API, stores, Docker image, and served web files. `webApp` is Compose Multiplatform for Web, HTTP only, served by the server.
++- Edition documents, settings, the source list, and calendar snapshots use one kotlinx.serialization `Json` configuration with default property names and no naming strategy. `webApp` sends and reads that JSON and does not declare its own model.
++- The server owns these routes, and every remote client uses them: `GET /editions`, `GET /editions/current`, `GET /editions/by-id`, `PUT /editions/by-id`, `POST /editions/morning/regenerate`, `POST /editions`, `GET /settings`, `PUT /settings`, `GET /sources`, `PUT /sources`, `PUT /calendar-snapshot`.
++- Each of those documents is one JSON document. The store assigns the id on insert. The id never changes. Every write gets a new monotonic revision. The latest whole document wins.
++- One source-list document holds RSS/Atom and ICS URLs. The generator reads that list and no other. The server environment writes it only when it is absent.
++- Clients talk only to a repository. With a server base URL, only the server generator writes a new morning paper. Roles stay `morning` and `extra`. The front page is today's morning edition. Regenerate keeps the id. New edition now inserts an extra.
++- Kotlin 2.4.21, Ktor 3.6.1, kotlinx-serialization 1.12.0, Compose Multiplatform 1.12.2, JDK 17. JVM target 17 for `shared`, `server`, and `desktopApp`.
++- Reach is the LAN. One configured base URL. `http` and `https` are the same API with a different scheme. Android network security must allow that URL, including cleartext when the scheme is `http`.
++
++## UX & Interaction Patterns
++
++- The token gate is the first web screen: chrome, one text field, one primary button. No masthead and no edition behind a rejected token. Error copy is "That token doesn't match." The field keeps the typed value.
++- After a valid token, the web Edition is the newspaper. Admin is a separate chrome screen opened from the newspaper, not a Settings screen.
++- Light edit keeps the display type, uses a 1px ink underline, and has no toolbar and no mono face. "Saved." is the last confirmed line.
++- The Edition does not say whether the paper came from the device or the server.
++- Below 960px the web client uses the single-column order. Chrome controls are at least 40px. The edition itself stays the custom paper, not chrome.
++
++## Cross-Story Dependencies
++
++- Depends on Epic 1 and Epic 2. The sample-when-absent rule, the edition document, and the edit-survives-regenerate rule already exist and apply to editions the server owns.
++- The browser paper and token come before the server clock, Admin, light edit, and phone or desktop remote mode.
++- The server clock owns insert-if-absent for today's morning edition. Later stories must not replace an existing morning edition.
++- The README server path and the release image come after the server and browser actually show a morning paper. Neither documents public internet exposure.
+diff --git a/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
+new file mode 100644
+index 0000000..afd8a10
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
+@@ -0,0 +1,105 @@
++---
++title: 'Open the server''s paper in the browser'
++type: 'feature'
++created: '2026-11-10'
++status: 'in-review'
++baseline_commit: '4b57ede7cf6ca7d43bbf180250b81d904035a479'
++route: 'dispatch'
++review_loop_iteration: 0
++context:
++  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
++  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md'
++---
++
++<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
++
++## Intent
++
++**Problem:** The LAN paper is not visible. `:server` is a hello-world Ktor process, and `:app:webApp` opens the on-device app, which generates a paper in the browser.
++
++**Approach:** Ship a Dockerized Ktor process that stores documents under `/data`, requires one Bearer token, and serves a browser client whose first screen is a token gate and whose next screen is the sample newspaper generated on the server.
++
++## Boundaries & Constraints
++
++**Always:**
++- Unset or blank `MATUTINE_TOKEN` is the literal `matutine-demo`. One shared token. Every edition API call uses `Authorization: Bearer <token>`.
++- Documents live in the host directory mounted at `/data` (override `MATUTINE_DATA` only for tests). Same JSON codec and file names as the existing store: `sources.json`, `settings.json`, `calendar-snapshot.json`, `editions/<dayKey>-morning.json`.
++- The only edition route in this story is `GET /editions/current`. A missing or wrong bearer token is rejected before the store is read and before any edition body is written.
++- An absent source list and an absent morning document: the server builds the bundled sample through `LocalEditionRepository.loadCurrentMorningEdition` and returns it, with the sample line. An existing morning document is returned unchanged.
++- The web client decodes `Edition` with `decodeEdition`. It does not declare an edition model and does not call the generator.
++- The token gate is chrome: one field, one primary button labeled Continue. No paper and no Admin until the current-edition call succeeds. A rejected token shows "That token doesn't match." and keeps the typed value.
++
++**Never:**
++- No scheduler, no `runMorning`, no Admin, no light edit, no client sync, no Settings screen, no service worker, no public-port instructions, no README change, no timezone identifier format.
++- Do not add the other AD-16 routes. Do not replace an existing morning edition. Do not depend on `:app:shared` from `:server`.
++
++## I/O & Edge-Case Matrix
++
++| Scenario | Input / State | Expected Output / Behavior | Error Handling |
++|----------|--------------|---------------------------|----------------|
++| Default token | Env token unset or blank | Accepted bearer is `Bearer matutine-demo` | N/A |
++| Set token | `MATUTINE_TOKEN=lan-secret` | Only `Bearer lan-secret` is accepted | Other values rejected |
++| No or wrong bearer | Missing header, non-Bearer scheme, or wrong token | 401, body is not an edition document, store is not read | N/A |
++| Absent source list | Valid bearer, no `sources.json`, no morning file | 200 `Edition` JSON, `isSample` true, sample line text present in the decoded edition, morning file written, `sources.json` still absent | N/A |
++| Existing morning | Valid bearer, morning file already present | 200 returns that document unchanged; revision and id stay | N/A |
++| Wrong gate token | Browser submits a value the server rejects | Gate stays, field keeps the value, message "That token doesn't match.", Edition and Admin are not composed | N/A |
++| Right gate token | Browser submits the server token and the source list is absent | Edition screen shows the returned paper, including SAMPLE EDITION | A non-401 failure stays on the gate and does not show a paper |
++
++</frozen-after-approval>
++
++## Code Map
++
++- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- replace the hello route with config, auth, `GET /editions/current`, and static web files.
++- `server/build.gradle.kts` -- already depends on `:core`. Keep that. Wire the Wasm distribution into the server resources.
++- `core/.../EditionRepository.kt` -- `LocalEditionRepository.loadCurrentMorningEdition` already saves the sample when the source list is absent and returns an existing morning first. Reuse it. Do not call `runMorning`.
++- `core/.../EditionJson.kt` -- `editionJson`, `encodeEdition`, `decodeEdition`. The only codec.
++- `app/shared/.../FileEditionStore.jvm.kt` -- file names to match. Do not depend on this module from the server; it pulls Compose.
++- `app/shared/.../EditionScreen.kt` -- newspaper drawing. Add a defaulted read-only switch so web can hide Settings, New edition now, and task writes. Android and Desktop call sites stay as they are.
++- `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- today calls `App()`, which runs `MatutineApp` and the local generator. Replace that entry.
++- `app/shared/.../App.kt` -- leave the device preview entry unchanged.
++- `gradle/libs.versions.toml` -- Ktor 3.6.0 is the catalog pin. Do not bump it.
++
++## Tasks & Acceptance
++
++**Execution:**
++- [x] `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` -- `EditionStore` on the data directory using core codecs and the existing file names.
++- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- token resolution, bearer gate, current-edition route, static files from the built web app.
++- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- cover the I/O rows that the server owns, including a store probe that fails if a rejected call touches documents.
++- [x] `server/Dockerfile` -- image runs the fat jar, store path `/data`, token and other secrets only from the environment.
++- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- defaulted read-only chrome so existing device screens keep their controls.
++- [x] `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- token gate, then read-only `EditionScreen` from `GET /editions/current` decoded with `decodeEdition`.
++- [x] `server/build.gradle.kts` -- depend on the Wasm browser distribution and package those files for the server to serve.
++
++**Acceptance Criteria:**
++- Given the Docker image and a host directory mounted at `/data`, when the container starts with no token in the environment, then the accepted token is `matutine-demo` and edition, settings, source list, and calendar snapshot files are under `/data`.
++- Given the wrong token, when it is submitted, then the page says "That token doesn't match.", the field keeps the typed value, and neither the newspaper nor Admin is shown.
++- Given the right token and an absent source list, when the gate opens, then the web Edition shows the server-generated sample paper and the sample line.
++- Given any current-edition call, when the bearer token is missing or wrong, then the server rejects it before returning an edition.
++- The web bundle does not invoke the generator, and it is not an offline app.
++
++## Implementation Notes
++
++- Token environment name is `MATUTINE_TOKEN`. Blank and unset both resolve to `matutine-demo`. Data directory is `MATUTINE_DATA`, default `/data`.
++- The server store copies the desktop file names and uses the core codecs. It does not depend on `:app:shared`.
++- `GET /editions/current` is the only API route. A rejected bearer returns plain text `unauthorized` and does not call the store.
++- Sample generation is `LocalEditionRepository.loadCurrentMorningEdition`. `runMorning` is not called.
++- The gate button label is Continue. The field label is Token. The web entry no longer calls `App()`.
++- Wasm fetch uses the browser `window.fetch` Promise. JS uses the same call. Both send `Authorization: Bearer` and decode with `decodeEdition`.
++- `./gradlew :server:test` passed 7 tests. `:app:shared:jvmTest --tests hu.toliver.matutine.edition.WebPaperGateTest` passed 4 tests. `:server:buildFatJar` produced `server/build/libs/server-all.jar`.
++- The browser showed the chrome token gate at `http://127.0.0.1:8080/` with one field and Continue, and no newspaper. Compose on Wasm did not accept the automated click, so the mismatch sentence and the sample paper were not exercised in that browser session. Curl against the running jar confirmed 401 for a missing or wrong bearer and a sample edition for `Bearer matutine-demo`, with only `editions/<day>-morning.json` written.
++- The first Docker build failed because the Wasm Node binary needs `libatomic.so.1`, which the Temurin JDK image does not ship. The build stage installs `libatomic1` before Gradle. The jar's bytecode is class version 65, so the runtime image is Temurin 21.
++
++## Spec Change Log
++
++## Review Triage Log
++
++## Design Notes
++
++The server implements `EditionStore` itself so the process can use `LocalEditionRepository` without taking a dependency on `:app:shared`. `GET /editions/current` is the one AD-16 route this story needs; the other routes stay absent until their stories. The gate compares nothing locally: only a successful current-edition response opens the paper. The primary button label is Continue. The field label is Token.
++
++## Verification
++
++**Commands:**
++- `./gradlew :server:test` -- expected: bearer, sample, and existing-morning tests pass.
++- `./gradlew :server:buildFatJar :app:webApp:wasmJsBrowserDistribution` -- expected: fat jar and Wasm distribution build.
++- `docker build -t matutine -f server/Dockerfile .` -- expected: image builds when Docker is available.
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
+new file mode 100644
+index 0000000..2dbdf09
+--- /dev/null
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
+@@ -0,0 +1,23 @@
++package hu.toliver.matutine.edition
++
++const val TOKEN_DOES_NOT_MATCH = "That token doesn't match."
++
++data class WebPaperGate(
++    val typedToken: String,
++    val showEdition: Boolean,
++    val message: String?,
++)
++
++/**
++ * The browser gate does not compare the token itself. A 200 from the current-edition
++ * route is the only path that opens the paper. The typed value is returned unchanged.
++ */
++fun webPaperGate(typedToken: String, httpStatus: Int): WebPaperGate = when (httpStatus) {
++    200 -> WebPaperGate(typedToken = typedToken, showEdition = true, message = null)
++    401 -> WebPaperGate(
++        typedToken = typedToken,
++        showEdition = false,
++        message = TOKEN_DOES_NOT_MATCH,
++    )
++    else -> WebPaperGate(typedToken = typedToken, showEdition = false, message = null)
++}
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
+new file mode 100644
+index 0000000..9d09d01
+--- /dev/null
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
+@@ -0,0 +1,43 @@
++package hu.toliver.matutine.edition
++
++import kotlin.test.Test
++import kotlin.test.assertEquals
++import kotlin.test.assertFalse
++import kotlin.test.assertNull
++import kotlin.test.assertTrue
++
++class WebPaperGateTest {
++
++    @Test
++    fun wrongToken_keepsTheTypedValueAndDoesNotOpenThePaper() {
++        val decision = webPaperGate(typedToken = "not-the-token", httpStatus = 401)
++        assertEquals("not-the-token", decision.typedToken)
++        assertEquals(TOKEN_DOES_NOT_MATCH, decision.message)
++        assertFalse(decision.showEdition)
++    }
++
++    @Test
++    fun rightToken_opensTheEditionWithoutAGateMessage() {
++        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 200)
++        assertEquals("matutine-demo", decision.typedToken)
++        assertNull(decision.message)
++        assertTrue(decision.showEdition)
++    }
++
++    @Test
++    fun otherFailure_staysOnTheGateWithoutAPaper() {
++        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 503)
++        assertEquals("matutine-demo", decision.typedToken)
++        assertNull(decision.message)
++        assertFalse(decision.showEdition)
++    }
++
++    @Test
++    fun sampleEditionCarriesTheSampleLine() {
++        val edition = SampleGenerator.forDay(todayLocalDate())
++        val decoded = decodeEdition(encodeEdition(edition))
++        assertTrue(decoded.isSample)
++        assertTrue(EditionTranscript.render(decoded).contains(EditionTranscript.SAMPLE))
++        assertEquals("SAMPLE EDITION", EditionTranscript.SAMPLE)
++    }
++}
+diff --git a/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
+new file mode 100644
+index 0000000..75b86ea
+--- /dev/null
++++ b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
+@@ -0,0 +1,16 @@
++package hu.toliver.matutine
++
++import kotlinx.browser.window
++import kotlinx.coroutines.await
++import org.w3c.fetch.Headers
++import org.w3c.fetch.RequestInit
++
++actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
++    val headers = Headers()
++    headers.append("Authorization", "Bearer $token")
++    val response = window.fetch(
++        "/editions/current",
++        RequestInit(method = "GET", headers = headers),
++    ).await()
++    return EditionFetch(status = response.status.toInt(), body = response.text().await())
++}
+diff --git a/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
+new file mode 100644
+index 0000000..a20cff6
+--- /dev/null
++++ b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
+@@ -0,0 +1,23 @@
++package hu.toliver.matutine
++
++import kotlin.js.ExperimentalWasmJsInterop
++import kotlin.js.JsString
++import kotlin.js.Promise
++import kotlinx.browser.window
++import kotlinx.coroutines.await
++import org.w3c.fetch.Headers
++import org.w3c.fetch.RequestInit
++import org.w3c.fetch.Response
++
++@OptIn(ExperimentalWasmJsInterop::class)
++actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
++    val headers = Headers()
++    headers.append("Authorization", "Bearer $token")
++    val pending: Promise<Response> = window.fetch(
++        "/editions/current",
++        RequestInit(method = "GET", headers = headers),
++    )
++    val response = pending.await()
++    val body = response.text().await<JsString>().toString()
++    return EditionFetch(status = response.status.toInt(), body = body)
++}
+diff --git a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
+new file mode 100644
+index 0000000..076a567
+--- /dev/null
++++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
+@@ -0,0 +1,8 @@
++package hu.toliver.matutine
++
++data class EditionFetch(
++    val status: Int,
++    val body: String,
++)
++
++expect suspend fun fetchCurrentEdition(token: String): EditionFetch
+diff --git a/server/Dockerfile b/server/Dockerfile
+new file mode 100644
+index 0000000..8f444b4
+--- /dev/null
++++ b/server/Dockerfile
+@@ -0,0 +1,15 @@
++FROM eclipse-temurin:17-jdk AS build
++RUN apt-get update \
++    && apt-get install -y --no-install-recommends libatomic1 \
++    && rm -rf /var/lib/apt/lists/*
++WORKDIR /src
++COPY . .
++RUN ./gradlew :server:buildFatJar --no-daemon
++
++FROM eclipse-temurin:21-jre
++WORKDIR /app
++COPY --from=build /src/server/build/libs/*-all.jar /app/server.jar
++ENV MATUTINE_DATA=/data
++EXPOSE 8080
++VOLUME /data
++ENTRYPOINT ["java", "-jar", "/app/server.jar"]
+diff --git a/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
+new file mode 100644
+index 0000000..4ab6443
+--- /dev/null
++++ b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
+@@ -0,0 +1,96 @@
++package hu.toliver.matutine
++
++import hu.toliver.matutine.edition.CalDavAccount
++import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionRole
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SourceList
++import hu.toliver.matutine.edition.decodeCalDavAccount
++import hu.toliver.matutine.edition.decodeCalendarSnapshot
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.decodeReaderSettings
++import hu.toliver.matutine.edition.decodeSourceList
++import hu.toliver.matutine.edition.encodeCalDavAccount
++import hu.toliver.matutine.edition.encodeCalendarSnapshot
++import hu.toliver.matutine.edition.encodeEdition
++import hu.toliver.matutine.edition.encodeReaderSettings
++import hu.toliver.matutine.edition.encodeSourceList
++import java.io.File
++
++/**
++ * Same document names as the desktop file store, rooted at the server data directory.
++ * The server does not depend on `:app:shared` because that module pulls Compose.
++ */
++class DataDirectoryStore(
++    rootPath: String,
++) : EditionStore {
++    private val root = File(rootPath)
++
++    init {
++        root.mkdirs()
++    }
++
++    override fun readSourceList(): SourceList? = read("sources.json", ::decodeSourceList)
++
++    override fun writeSourceList(sourceList: SourceList) {
++        write("sources.json", encodeSourceList(sourceList))
++    }
++
++    override fun readSettings(): ReaderSettings? = read("settings.json", ::decodeReaderSettings)
++
++    override fun writeSettings(settings: ReaderSettings) {
++        write("settings.json", encodeReaderSettings(settings))
++    }
++
++    override fun loadMorningEdition(dayKey: String): Edition? =
++        read("editions/$dayKey-morning.json", ::decodeEdition)
++
++    override fun saveMorningEdition(edition: Edition) {
++        require(edition.role == EditionRole.morning)
++        write("editions/${edition.dayKey}-morning.json", encodeEdition(edition))
++    }
++
++    override fun loadExtras(dayKey: String): List<Edition> {
++        val dir = File(root, "editions")
++        if (!dir.exists()) return emptyList()
++        val prefix = "extra-$dayKey-"
++        return dir.listFiles().orEmpty()
++            .filter { it.isFile && it.name.startsWith(prefix) && it.name.endsWith(".json") }
++            .map { decodeEdition(it.readText()) }
++            .filter { it.role == EditionRole.extra && it.dayKey == dayKey }
++            .sortedBy { it.id.substringAfterLast("-").toIntOrNull() ?: 0 }
++    }
++
++    override fun saveExtra(edition: Edition) {
++        require(edition.role == EditionRole.extra)
++        require(edition.id.startsWith("extra-${edition.dayKey}-"))
++        write("editions/${edition.id}.json", encodeEdition(edition))
++    }
++
++    override fun readCalendarSnapshot(): CalendarSnapshot? =
++        read("calendar-snapshot.json", ::decodeCalendarSnapshot)
++
++    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
++        write("calendar-snapshot.json", encodeCalendarSnapshot(snapshot))
++    }
++
++    override fun readCalDavAccount(): CalDavAccount? = read("caldav.json", ::decodeCalDavAccount)
++
++    override fun writeCalDavAccount(account: CalDavAccount) {
++        write("caldav.json", encodeCalDavAccount(account))
++    }
++
++    private fun <T> read(relativePath: String, decode: (String) -> T): T? {
++        val file = File(root, relativePath)
++        if (!file.exists()) return null
++        return decode(file.readText())
++    }
++
++    private fun write(relativePath: String, json: String) {
++        val file = File(root, relativePath)
++        file.parentFile?.mkdirs()
++        file.writeText(json)
++    }
++}
+
+```
+
+## Claims file (do not read until your instructions reach the claims step)
+
+```markdown
+---
+title: 'Open the server''s paper in the browser'
+type: 'feature'
+created: '2026-11-10'
+status: 'in-review'
+baseline_commit: '4b57ede7cf6ca7d43bbf180250b81d904035a479'
+route: 'dispatch'
+review_loop_iteration: 0
+context:
+  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
+  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md'
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** The LAN paper is not visible. `:server` is a hello-world Ktor process, and `:app:webApp` opens the on-device app, which generates a paper in the browser.
+
+**Approach:** Ship a Dockerized Ktor process that stores documents under `/data`, requires one Bearer token, and serves a browser client whose first screen is a token gate and whose next screen is the sample newspaper generated on the server.
+
+## Boundaries & Constraints
+
+**Always:**
+- Unset or blank `MATUTINE_TOKEN` is the literal `matutine-demo`. One shared token. Every edition API call uses `Authorization: Bearer <token>`.
+- Documents live in the host directory mounted at `/data` (override `MATUTINE_DATA` only for tests). Same JSON codec and file names as the existing store: `sources.json`, `settings.json`, `calendar-snapshot.json`, `editions/<dayKey>-morning.json`.
+- The only edition route in this story is `GET /editions/current`. A missing or wrong bearer token is rejected before the store is read and before any edition body is written.
+- An absent source list and an absent morning document: the server builds the bundled sample through `LocalEditionRepository.loadCurrentMorningEdition` and returns it, with the sample line. An existing morning document is returned unchanged.
+- The web client decodes `Edition` with `decodeEdition`. It does not declare an edition model and does not call the generator.
+- The token gate is chrome: one field, one primary button labeled Continue. No paper and no Admin until the current-edition call succeeds. A rejected token shows "That token doesn't match." and keeps the typed value.
+
+**Never:**
+- No scheduler, no `runMorning`, no Admin, no light edit, no client sync, no Settings screen, no service worker, no public-port instructions, no README change, no timezone identifier format.
+- Do not add the other AD-16 routes. Do not replace an existing morning edition. Do not depend on `:app:shared` from `:server`.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Default token | Env token unset or blank | Accepted bearer is `Bearer matutine-demo` | N/A |
+| Set token | `MATUTINE_TOKEN=lan-secret` | Only `Bearer lan-secret` is accepted | Other values rejected |
+| No or wrong bearer | Missing header, non-Bearer scheme, or wrong token | 401, body is not an edition document, store is not read | N/A |
+| Absent source list | Valid bearer, no `sources.json`, no morning file | 200 `Edition` JSON, `isSample` true, sample line text present in the decoded edition, morning file written, `sources.json` still absent | N/A |
+| Existing morning | Valid bearer, morning file already present | 200 returns that document unchanged; revision and id stay | N/A |
+| Wrong gate token | Browser submits a value the server rejects | Gate stays, field keeps the value, message "That token doesn't match.", Edition and Admin are not composed | N/A |
+| Right gate token | Browser submits the server token and the source list is absent | Edition screen shows the returned paper, including SAMPLE EDITION | A non-401 failure stays on the gate and does not show a paper |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- replace the hello route with config, auth, `GET /editions/current`, and static web files.
+- `server/build.gradle.kts` -- already depends on `:core`. Keep that. Wire the Wasm distribution into the server resources.
+- `core/.../EditionRepository.kt` -- `LocalEditionRepository.loadCurrentMorningEdition` already saves the sample when the source list is absent and returns an existing morning first. Reuse it. Do not call `runMorning`.
+- `core/.../EditionJson.kt` -- `editionJson`, `encodeEdition`, `decodeEdition`. The only codec.
+- `app/shared/.../FileEditionStore.jvm.kt` -- file names to match. Do not depend on this module from the server; it pulls Compose.
+- `app/shared/.../EditionScreen.kt` -- newspaper drawing. Add a defaulted read-only switch so web can hide Settings, New edition now, and task writes. Android and Desktop call sites stay as they are.
+- `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- today calls `App()`, which runs `MatutineApp` and the local generator. Replace that entry.
+- `app/shared/.../App.kt` -- leave the device preview entry unchanged.
+- `gradle/libs.versions.toml` -- Ktor 3.6.0 is the catalog pin. Do not bump it.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` -- `EditionStore` on the data directory using core codecs and the existing file names.
+- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- token resolution, bearer gate, current-edition route, static files from the built web app.
+- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- cover the I/O rows that the server owns, including a store probe that fails if a rejected call touches documents.
+- [x] `server/Dockerfile` -- image runs the fat jar, store path `/data`, token and other secrets only from the environment.
+- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- defaulted read-only chrome so existing device screens keep their controls.
+- [x] `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- token gate, then read-only `EditionScreen` from `GET /editions/current` decoded with `decodeEdition`.
+- [x] `server/build.gradle.kts` -- depend on the Wasm browser distribution and package those files for the server to serve.
+
+**Acceptance Criteria:**
+- Given the Docker image and a host directory mounted at `/data`, when the container starts with no token in the environment, then the accepted token is `matutine-demo` and edition, settings, source list, and calendar snapshot files are under `/data`.
+- Given the wrong token, when it is submitted, then the page says "That token doesn't match.", the field keeps the typed value, and neither the newspaper nor Admin is shown.
+- Given the right token and an absent source list, when the gate opens, then the web Edition shows the server-generated sample paper and the sample line.
+- Given any current-edition call, when the bearer token is missing or wrong, then the server rejects it before returning an edition.
+- The web bundle does not invoke the generator, and it is not an offline app.
+
+## Implementation Notes
+
+- Token environment name is `MATUTINE_TOKEN`. Blank and unset both resolve to `matutine-demo`. Data directory is `MATUTINE_DATA`, default `/data`.
+- The server store copies the desktop file names and uses the core codecs. It does not depend on `:app:shared`.
+- `GET /editions/current` is the only API route. A rejected bearer returns plain text `unauthorized` and does not call the store.
+- Sample generation is `LocalEditionRepository.loadCurrentMorningEdition`. `runMorning` is not called.
+- The gate button label is Continue. The field label is Token. The web entry no longer calls `App()`.
+- Wasm fetch uses the browser `window.fetch` Promise. JS uses the same call. Both send `Authorization: Bearer` and decode with `decodeEdition`.
+- `./gradlew :server:test` passed 7 tests. `:app:shared:jvmTest --tests hu.toliver.matutine.edition.WebPaperGateTest` passed 4 tests. `:server:buildFatJar` produced `server/build/libs/server-all.jar`.
+- The browser showed the chrome token gate at `http://127.0.0.1:8080/` with one field and Continue, and no newspaper. Compose on Wasm did not accept the automated click, so the mismatch sentence and the sample paper were not exercised in that browser session. Curl against the running jar confirmed 401 for a missing or wrong bearer and a sample edition for `Bearer matutine-demo`, with only `editions/<day>-morning.json` written.
+- The first Docker build failed because the Wasm Node binary needs `libatomic.so.1`, which the Temurin JDK image does not ship. The build stage installs `libatomic1` before Gradle. The jar's bytecode is class version 65, so the runtime image is Temurin 21.
+
+## Spec Change Log
+
+## Review Triage Log
+
+## Design Notes
+
+The server implements `EditionStore` itself so the process can use `LocalEditionRepository` without taking a dependency on `:app:shared`. `GET /editions/current` is the one AD-16 route this story needs; the other routes stay absent until their stories. The gate compares nothing locally: only a successful current-edition response opens the paper. The primary button label is Continue. The field label is Token.
+
+## Verification
+
+**Commands:**
+- `./gradlew :server:test` -- expected: bearer, sample, and existing-morning tests pass.
+- `./gradlew :server:buildFatJar :app:webApp:wasmJsBrowserDistribution` -- expected: fat jar and Wasm distribution build.
+- `docker build -t matutine -f server/Dockerfile .` -- expected: image builds when Docker is available.
+
+```
diff --git a/_bmad-output/implementation-artifacts/review-3-1-verification-gap-prompt.md b/_bmad-output/implementation-artifacts/review-3-1-verification-gap-prompt.md
new file mode 100644
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/review-3-1-verification-gap-prompt.md
+Read the review instructions below completely and follow them.
+
+Review content: the unified diff below. It is the content under review.
+
+Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. If the instruction file is unreadable, report that exact failure and stop. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
+
+## Review instructions
+
+# Verification Gap Review
+
+**Goal:** Find changed behavior that could break without reliable verification catching it. Ask one question — "if the behavior this change is supposed to produce broke where it's actually used, would verification fail?" Do not hunt for correctness bugs, but report genuine problems you notice while tracing verification.
+
+The main verification gap shapes are:
+
+1. **Regression gap:** the changed code regresses where it's used, and no test covering that use would fail.
+2. **Missing-adoption gap:** a place that should now use the new behavior doesn't; it handles the same case its own way, or not at all, and no test would flag the omission.
+3. **Broken-verification gap:** a test appears to cover the changed behavior, but would not actually protect it because it is skipped, flaky, not run in the normal verification path, or too weak to observe the regression.
+
+## Evidence Rules
+
+- Read a test before claiming what it covers, runs, asserts, or misses.
+- Before claiming no test exists, search the whole repo by the symbol under test and by import references; expected file locations are not enough.
+- Never assert what you did not verify. If a finding cannot be grounded, drop it.
+- In a finding, say what you actually checked — "none of the tests I read cover this" — and show how far you looked. Say a test doesn't exist anywhere only when the symbol/import-reference search actually shows that.
+- Do not assign severity, confidence, priority, or ranking.
+
+## Review Sequence
+
+### Step 1: Screen for behavioral change
+
+Screen each part of the change separately. If a part is non-behavioral, skip it. Call a part non-behavioral only when the changed code does not alter return values, thrown errors, caller-visible side effects, or observable state (including iteration order and emitted messages). Once a part meets that test, move on; do not inspect callers or tests for extra confirmation.
+
+Common non-behavioral examples: formatting, comments, whitespace; pure renames; trivial getters/setters and pass-throughs; type-only or compiler-enforced changes with no runtime effect; etc.
+
+Only outcomes produced by deterministic code are worth automatically testing; tests are useless on static source text and brittle on LLM output. Skip those parts.
+
+If every part is skipped, output the clean result (see Output Format).
+
+### Step 2: Find the behavior that changed
+
+Identify what behavior changed compared to the previous version: output, side effect, branch, error path, schema/event shape, config default, validation/authorization rule, external contract, etc. If the change affects more than one behavior, handle each separately.
+
+Treat broad-impact changes as behavioral even when no single changed line looks important: dependency, toolchain, build/config, data-file, etc.
+
+### Step 3: Trace where that behavior is used
+
+Trace the changed behavior to the places that observe it. Start with direct callers and registered entry points (routes, commands, DI), contract consumers (schemas, events, APIs, database readers), and reverse-dependency info if already available.
+
+Follow a path only while the changed behavior is reachable and unverified. Stop when a test at that boundary would fail, the consumer does not observe the changed behavior, or the next hop is guesswork (dynamic dispatch, reflection, outside-repo consumers, etc.). Prefer the nearest observable boundary, often one to three hops away, especially across contract, integration, or service edges. If there are more than five similar consumers, group obvious repeats and check representative paths; expand only when a consumer observes the behavior differently.
+
+### Step 4: Qualify the consumer, then check its test
+
+For each consumer, name the smallest realistic regression this consumer would observe: invert the branch, drop the default, omit the field, return the old error code, skip the integration call, etc. This is the Demonstration. If no such regression exists, drop the path; untested downstream code is not a finding.
+
+A `Missing-adoption gap` qualifies not by the adoption failure alone but by a supersession signal: the change gives clear evidence the new behavior is meant to replace the local one — PR intent, naming or docs, a replaced sibling site, deleted duplicate logic, or a test defining the new rule — and the local site shares the same observable contract. Without a supersession signal and a shared observable contract, it is a refactor suggestion, not a verification-gap finding. Once both hold, check whether any test for that site would flag the non-adoption; missing coverage of the non-adoption is the gap itself, not a disqualifier.
+
+Find and read the relevant test. Ask whether the Demonstration would make an assertion fail.
+
+- If yes, the behavior is verified. No finding.
+- For a regression-style Demonstration: if no test runs the path, the test is skipped/flaky/not run normally, or the test runs the code without checking the changed result, report a `Regression gap` or `Broken-verification gap`.
+- For a qualifying Missing-adoption case: if none of the site tests you found assert it adopts the new behavior, report a `Missing-adoption gap`.
+
+A test counts only if it runs normally and an assertion observes the changed output, branch, or contract. These do not count: no execution; source-text assertions that match a file's wording instead of running it; success/no-throw/snapshot-only checks; mock/log-call checks; human-only checks; tests that mock away the integration; e2e tests that pass through without checking the changed output; stale assertions or fixtures.
+
+For example, `expect(x ?? DEFAULT).toBe(DEFAULT)` passes when `x` is missing.
+
+Common patterns:
+
+- **Caller-path gap** — helper test covers the branch, but caller values skip it.
+- **Contract drift** — payload/schema/event changes must be verified at the consumer.
+- **Migration compatibility** — tests only create new-format rows or fresh schemas.
+- **Phantom exception** — handled partial-failure path has no test.
+- **Missing-adoption gap** — sibling site should use the new rule/helper and does not.
+- **Removed verification** — deleted test or weakened assertion leaves behavior unpinned; removing a source-text assertion is not this, since it never counted.
+
+### Step 5: Confirm each finding is real
+
+Before writing a finding, re-open the specific tests or search results the finding relies on. Verify the Demonstration would not make any test you checked fail, or that the absence claim is backed by the symbol/import-reference search. Do not claim more than you verified; drop any finding you cannot ground.
+
+Explain why the test misses the bug using what the test sets up and checks.
+
+Do not report: compiler/type-checker-enforced cases; behavior already verified by an integration, contract, or e2e test; implementation-detail or mock-only tests; low coverage or a missing test file by itself; legacy untested code the change did not affect.
+
+Report genuine problems you noticed while tracing verification, even if they are not verification gaps. Put them under `Other findings` in the output. This permits reporting what you already reached, not extra hunting. A claim that code misbehaves is a defect, not a gap — it goes under `Other findings` for standard triage, however you found it.
+
+## OUTPUT FORMAT
+
+Emit each verification-gap finding as one block. No general advice, no severity or confidence. Triage trusts a gap finding as filed and does not re-verify it, so each block must stand on its own evidence.
+
+```markdown
+### <one-line title naming the gap>
+
+- **Changed surface:** the exact behavior or contract that changed — `file:line`.
+- **Impacted consumer or site:** named concretely with `file:line` (e.g. "the `createInvoice` mutation used by the billing dashboard at `billing/dashboard.ts:88`," not "callers of this function").
+- **Existing test evidence:**
+  - `Regression gap`: what the relevant test actually asserts, with `file:line`; or, if none, the symbol/import-reference searches run and their result.
+  - `Missing-adoption gap`: tests for the impacted site, and whether any assert it adopts the new behavior.
+  - `Broken-verification gap`: the apparent test or verification path, and why it does not count.
+- **Missing verification:** the precise assertion or check that's absent.
+- **Demonstration:**
+  - `Regression gap` / `Broken-verification gap`: the concrete regression that would ship undetected, and why the tests you checked would not fail.
+  - `Missing-adoption gap`: the case the site mishandles by not adopting the new behavior, and that none of the tests you read assert adoption.
+- **Consequence:** the concrete thing that ships wrong — a regression the checked evidence would not catch, or a site that should use the new behavior and doesn't.
+- **Disposition:** `patch` — name the test to add, fit to the repo's own way of verifying (don't impose a generic test pyramid) — or `defer` when the gap is real but not worth closing as part of this change, with one sentence of why.
+```
+
+If you noticed genuine non-gap problems while tracing verification, append:
+
+```markdown
+## Other findings
+
+- <description only; no severity, confidence, priority, or ranking>
+```
+
+When you find no verification gaps and no other findings, output exactly this single line, not an empty response:
+
+`No verification gaps found.`
+
+## CONTENT SOURCE
+
+"Review content:" in the message that launched you gives the content itself or a path to read it from. Read the file when it is a path; either way that is the content under review, and this instruction file never is. If no content is supplied, or the file it points to is missing, empty, or unreadable, say exactly that and stop — never report a clean review for content you could not read.
+
+
+## Unified diff
+
+```diff
+diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
+index 860caa7..30de6a2 100644
+--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
++++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
+@@ -29,7 +29,7 @@
+ # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
+ # - Retrospective appends its action items to action_items; the status view surfaces open ones
+ generated: 11-07-2026 19:42
+-last_updated: 11-10-2026 18:48
++last_updated: 11-10-2026 18:55
+ project: Matutine
+ project_key: NOKEY
+ tracking_system: file-system
+@@ -59,8 +59,8 @@ development_status:
+   2-7-write-the-briefing-with-a-gemini-key: done
+   epic-2-retrospective: optional
+ 
+-  epic-3: backlog
+-  3-1-open-the-server-s-paper-in-the-browser: backlog
++  epic-3: in-progress
++  3-1-open-the-server-s-paper-in-the-browser: in-progress
+   3-2-generate-the-morning-edition-on-the-server-clock: backlog
+   3-3-configure-the-server-in-admin: backlog
+   3-4-light-edit-a-block-in-the-browser: backlog
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+index c06cc9b..e418217 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+@@ -72,6 +72,7 @@ fun EditionScreen(
+     onNewEditionNow: () -> Unit = {},
+     onOpenExtra: (String) -> Unit = {},
+     onOpenMorning: () -> Unit = {},
++    readOnly: Boolean = false,
+     modifier: Modifier = Modifier,
+ ) {
+     val typography = LocalMatutineEditionTypography.current
+@@ -82,6 +83,7 @@ fun EditionScreen(
+             generating = state.generating,
+             notice = state.notice,
+             onOpenSettings = onOpenSettings,
++            readOnly = readOnly,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -104,6 +106,7 @@ fun EditionScreen(
+             onNewEditionNow = onNewEditionNow,
+             onOpenExtra = onOpenExtra,
+             onOpenMorning = onOpenMorning,
++            readOnly = readOnly,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -175,6 +178,7 @@ private fun EmptyFrontPage(
+     generating: Boolean,
+     notice: String?,
+     onOpenSettings: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -188,11 +192,13 @@ private fun EmptyFrontPage(
+             .padding(horizontal = 16.dp, vertical = 16.dp),
+         verticalArrangement = Arrangement.spacedBy(16.dp),
+     ) {
+-        TextButton(
+-            onClick = onOpenSettings,
+-            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+-        ) {
+-            Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++        if (!readOnly) {
++            TextButton(
++                onClick = onOpenSettings,
++                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++            ) {
++                Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++            }
+         }
+         Masthead(name = "Matutine", dateLine = "", typography = typography)
+         if (generating) {
+@@ -229,6 +235,7 @@ private fun EditionContent(
+     onNewEditionNow: () -> Unit,
+     onOpenExtra: (String) -> Unit,
+     onOpenMorning: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -248,29 +255,31 @@ private fun EditionContent(
+                 .padding(horizontal = 16.dp, vertical = 16.dp),
+             verticalArrangement = Arrangement.spacedBy(16.dp),
+         ) {
+-            Row(
+-                horizontalArrangement = Arrangement.spacedBy(16.dp),
+-                verticalAlignment = Alignment.CenterVertically,
+-            ) {
+-                TextButton(
+-                    onClick = onOpenSettings,
+-                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++            if (!readOnly) {
++                Row(
++                    horizontalArrangement = Arrangement.spacedBy(16.dp),
++                    verticalAlignment = Alignment.CenterVertically,
+                 ) {
+-                    Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
+-                }
+-                if (onOpenTypewriter != null) {
+                     TextButton(
+-                        onClick = onOpenTypewriter,
++                        onClick = onOpenSettings,
+                         modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                     ) {
+-                        Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                        Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                    }
++                    if (onOpenTypewriter != null) {
++                        TextButton(
++                            onClick = onOpenTypewriter,
++                            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                        ) {
++                            Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                        }
++                    }
++                    TextButton(
++                        onClick = onNewEditionNow,
++                        modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                    ) {
++                        Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                     }
+-                }
+-                TextButton(
+-                    onClick = onNewEditionNow,
+-                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+-                ) {
+-                    Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                 }
+             }
+             if (edition.role == EditionRole.extra) {
+@@ -316,10 +325,10 @@ private fun EditionContent(
+             }
+             if (edition.isSample) {
+                 Text(
+-                    text = "SAMPLE EDITION",
++                    text = EditionTranscript.SAMPLE,
+                     style = typography.kicker.copy(color = EditionThemeColors.current.accent),
+                 )
+-            } else if (showNoTopicLine) {
++            } else if (showNoTopicLine && !readOnly) {
+                 TextButton(
+                     onClick = onOpenInterests,
+                     modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+@@ -339,6 +348,7 @@ private fun EditionContent(
+                     onAddTask = onAddTask,
+                     calendarPermissionOff = calendarPermissionOff,
+                     onOpenCalendarPermission = onOpenCalendarPermission,
++                    readOnly = readOnly,
+                     typography = typography,
+                 )
+             } else {
+@@ -351,6 +361,7 @@ private fun EditionContent(
+                     taskSaved = taskSaved,
+                     onToggleTask = onToggleTask,
+                     onAddTask = onAddTask,
++                    readOnly = readOnly,
+                     typography = typography,
+                 )
+             }
+@@ -366,6 +377,7 @@ private fun WideEditionBody(
+     onAddTask: (String) -> Unit,
+     calendarPermissionOff: Boolean,
+     onOpenCalendarPermission: () -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+ ) {
+     Spacer(Modifier.height(8.dp))
+@@ -395,6 +407,7 @@ private fun WideEditionBody(
+                 taskSaved = taskSaved,
+                 onToggleTask = onToggleTask,
+                 onAddTask = onAddTask,
++                readOnly = readOnly,
+                 typography = typography,
+             )
+         }
+@@ -562,6 +575,7 @@ private fun TaskBlock(
+     taskSaved: Boolean,
+     onToggleTask: (Int) -> Unit,
+     onAddTask: (String) -> Unit,
++    readOnly: Boolean,
+     typography: MatutineEditionTypography,
+ ) {
+     var draft by remember { mutableStateOf("") }
+@@ -571,18 +585,25 @@ private fun TaskBlock(
+             Text(text = EditionCopy.NO_TASKS, style = typography.body)
+         } else {
+             tasks.forEachIndexed { index, task ->
+-                TaskRow(task, typography, onToggle = { onToggleTask(index) })
++                TaskRow(
++                    task,
++                    typography,
++                    onToggle = { onToggleTask(index) },
++                    readOnly = readOnly,
++                )
+             }
+         }
+-        TaskAddLine(
+-            value = draft,
+-            onValueChange = { draft = it },
+-            onConfirm = {
+-                onAddTask(draft)
+-                draft = ""
+-            },
+-            typography = typography,
+-        )
++        if (!readOnly) {
++            TaskAddLine(
++                value = draft,
++                onValueChange = { draft = it },
++                onConfirm = {
++                    onAddTask(draft)
++                    draft = ""
++                },
++                typography = typography,
++            )
++        }
+         if (taskSaved) {
+             Text(text = EditionCopy.SAVED, style = typography.meta)
+         }
+@@ -624,6 +645,7 @@ private fun TaskRow(
+     task: EditionTask,
+     typography: MatutineEditionTypography,
+     onToggle: () -> Unit,
++    readOnly: Boolean,
+ ) {
+     val touchTarget = if (editionForceSingleColumn()) {
+         Modifier.minimumInteractiveComponentSize()
+@@ -636,10 +658,16 @@ private fun TaskRow(
+         modifier = Modifier
+             .padding(vertical = 4.dp)
+             .then(touchTarget)
+-            .clickable(
+-                interactionSource = remember { MutableInteractionSource() },
+-                indication = null,
+-                onClick = onToggle,
++            .then(
++                if (readOnly) {
++                    Modifier
++                } else {
++                    Modifier.clickable(
++                        interactionSource = remember { MutableInteractionSource() },
++                        indication = null,
++                        onClick = onToggle,
++                    )
++                },
+             ),
+     ) {
+         Box(
+diff --git a/app/webApp/build.gradle.kts b/app/webApp/build.gradle.kts
+index 4d8b955..6bbc79c 100644
+--- a/app/webApp/build.gradle.kts
++++ b/app/webApp/build.gradle.kts
+@@ -22,7 +22,11 @@ kotlin {
+         commonMain.dependencies {
+             implementation(project(":app:shared"))
+ 
++            implementation(libs.compose.runtime)
++            implementation(libs.compose.foundation)
++            implementation(libs.compose.material3)
+             implementation(libs.compose.ui)
++            implementation(libs.kotlinx.coroutines.core)
+         }
+     }
+ }
+\ No newline at end of file
+diff --git a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
+index f0b181a..e9da8b9 100644
+--- a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
++++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt
+@@ -1,11 +1,142 @@
+ package hu.toliver.matutine
+ 
++import androidx.compose.foundation.background
++import androidx.compose.foundation.layout.Arrangement
++import androidx.compose.foundation.layout.Column
++import androidx.compose.foundation.layout.fillMaxSize
++import androidx.compose.foundation.layout.fillMaxWidth
++import androidx.compose.foundation.layout.heightIn
++import androidx.compose.foundation.layout.padding
++import androidx.compose.foundation.shape.RoundedCornerShape
++import androidx.compose.material3.Button
++import androidx.compose.material3.ButtonDefaults
++import androidx.compose.material3.OutlinedTextField
++import androidx.compose.material3.Text
++import androidx.compose.runtime.Composable
++import androidx.compose.runtime.getValue
++import androidx.compose.runtime.mutableStateOf
++import androidx.compose.runtime.remember
++import androidx.compose.runtime.rememberCoroutineScope
++import androidx.compose.runtime.setValue
+ import androidx.compose.ui.ExperimentalComposeUiApi
++import androidx.compose.ui.Modifier
++import androidx.compose.ui.unit.dp
+ import androidx.compose.ui.window.ComposeViewport
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionScreen
++import hu.toliver.matutine.edition.EditionUiState
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.webPaperGate
++import hu.toliver.matutine.theme.EditionThemeColors
++import hu.toliver.matutine.theme.LocalMatutineEditionTypography
++import hu.toliver.matutine.theme.MatutineTheme
++import hu.toliver.matutine.theme.NewsprintColors
++import kotlinx.coroutines.launch
+ 
+ @OptIn(ExperimentalComposeUiApi::class)
+ fun main() {
+     ComposeViewport {
+-        App()
++        ServerPaperApp()
+     }
+-}
+\ No newline at end of file
++}
++
++@Composable
++fun ServerPaperApp() {
++    var typedToken by remember { mutableStateOf("") }
++    var message by remember { mutableStateOf<String?>(null) }
++    var edition by remember { mutableStateOf<Edition?>(null) }
++    var submitting by remember { mutableStateOf(false) }
++    val scope = rememberCoroutineScope()
++    MatutineTheme {
++        val paper = edition
++        if (paper == null) {
++            TokenGate(
++                token = typedToken,
++                onTokenChange = { typedToken = it },
++                message = message,
++                submitting = submitting,
++                onSubmit = {
++                    if (submitting) return@TokenGate
++                    submitting = true
++                    scope.launch {
++                        val decision = try {
++                            val response = fetchCurrentEdition(typedToken)
++                            val gate = webPaperGate(typedToken, response.status)
++                            typedToken = gate.typedToken
++                            message = gate.message
++                            if (gate.showEdition) {
++                                decodeEdition(response.body)
++                            } else {
++                                null
++                            }
++                        } catch (_: Throwable) {
++                            message = null
++                            null
++                        }
++                        edition = decision
++                        submitting = false
++                    }
++                },
++            )
++        } else {
++            EditionScreen(
++                state = EditionUiState.Ready(paper),
++                onRetrySample = {},
++                onOpenSettings = {},
++                showNoTopicLine = false,
++                onOpenInterests = {},
++                onToggleTask = {},
++                onAddTask = {},
++                readOnly = true,
++            )
++        }
++    }
++}
++
++@Composable
++private fun TokenGate(
++    token: String,
++    onTokenChange: (String) -> Unit,
++    message: String?,
++    submitting: Boolean,
++    onSubmit: () -> Unit,
++) {
++    val typography = LocalMatutineEditionTypography.current
++    Column(
++        modifier = Modifier
++            .fillMaxSize()
++            .background(NewsprintColors.chrome)
++            .padding(24.dp),
++        verticalArrangement = Arrangement.spacedBy(16.dp),
++    ) {
++        Text(text = "Token", style = typography.body.copy(color = EditionThemeColors.current.ink))
++        OutlinedTextField(
++            value = token,
++            onValueChange = onTokenChange,
++            modifier = Modifier
++                .fillMaxWidth()
++                .heightIn(min = 40.dp),
++            singleLine = true,
++            textStyle = typography.body.copy(color = EditionThemeColors.current.ink),
++            shape = RoundedCornerShape(2.dp),
++        )
++        if (!message.isNullOrBlank()) {
++            Text(
++                text = message,
++                style = typography.body.copy(color = EditionThemeColors.current.error),
++            )
++        }
++        Button(
++            onClick = onSubmit,
++            enabled = !submitting,
++            modifier = Modifier.heightIn(min = 40.dp),
++            shape = RoundedCornerShape(2.dp),
++            colors = ButtonDefaults.buttonColors(
++                containerColor = EditionThemeColors.current.ink,
++                contentColor = EditionThemeColors.current.paper,
++            ),
++        ) {
++            Text(text = "Continue")
++        }
++    }
++}
+diff --git a/server/build.gradle.kts b/server/build.gradle.kts
+index bbd6ccd..1462455 100644
+--- a/server/build.gradle.kts
++++ b/server/build.gradle.kts
+@@ -16,4 +16,15 @@ dependencies {
+     implementation(libs.ktor.serverNetty)
+     testImplementation(libs.ktor.serverTestHost)
+     testImplementation(libs.kotlin.testJunit)
++}
++
++val webDistribution = project(":app:webApp").layout.buildDirectory.dir(
++    "dist/wasmJs/productionExecutable",
++)
++
++tasks.named<ProcessResources>("processResources") {
++    dependsOn(":app:webApp:wasmJsBrowserDistribution")
++    from(webDistribution) {
++        into("web")
++    }
+ }
+\ No newline at end of file
+diff --git a/server/src/main/kotlin/hu/toliver/matutine/Application.kt b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
+index 25b8388..c2cafe8 100644
+--- a/server/src/main/kotlin/hu/toliver/matutine/Application.kt
++++ b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
+@@ -1,10 +1,33 @@
+ package hu.toliver.matutine
+ 
+-import io.ktor.server.application.*
+-import io.ktor.server.engine.*
+-import io.ktor.server.netty.*
+-import io.ktor.server.response.*
+-import io.ktor.server.routing.*
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.LocalEditionRepository
++import hu.toliver.matutine.edition.encodeEdition
++import io.ktor.http.ContentType
++import io.ktor.http.HttpStatusCode
++import io.ktor.server.application.Application
++import io.ktor.server.application.call
++import io.ktor.server.engine.embeddedServer
++import io.ktor.server.netty.Netty
++import io.ktor.server.response.respondText
++import io.ktor.server.routing.get
++import io.ktor.server.routing.routing
++import io.ktor.server.http.content.staticResources
++import io.ktor.util.AttributeKey
++
++private val SharedTokenKey = AttributeKey<String>("matutine.sharedToken")
++private val DocumentStoreKey = AttributeKey<EditionStore>("matutine.documents")
++
++fun resolveSharedToken(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "matutine-demo"
++
++fun resolveDataRoot(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "/data"
++
++internal fun bearerMatches(authorization: String?, token: String): Boolean {
++    val prefix = "Bearer "
++    if (authorization == null || !authorization.startsWith(prefix)) return false
++    val presented = authorization.substring(prefix.length)
++    return presented.isNotEmpty() && presented == token
++}
+ 
+ fun main() {
+     embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
+@@ -12,9 +35,34 @@ fun main() {
+ }
+ 
+ fun Application.module() {
++    val token = attributes.getOrNull(SharedTokenKey)
++        ?: resolveSharedToken(System.getenv("MATUTINE_TOKEN"))
++    val documents = attributes.getOrNull(DocumentStoreKey)
++        ?: DataDirectoryStore(resolveDataRoot(System.getenv("MATUTINE_DATA")))
++    val repository = LocalEditionRepository(documents)
+     routing {
+-        get("/") {
+-            call.respondText(sayHello("Ktor"))
++        get("/editions/current") {
++            val authorization = call.request.headers["Authorization"]
++            if (!bearerMatches(authorization, token)) {
++                call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
++                return@get
++            }
++            val edition = repository.loadCurrentMorningEdition().getOrElse { error ->
++                call.respondText(
++                    error.message ?: "unavailable",
++                    status = HttpStatusCode.NotFound,
++                )
++                return@get
++            }
++            call.respondText(encodeEdition(edition), ContentType.Application.Json)
++        }
++        staticResources("/", "web") {
++            default("index.html")
+         }
+     }
+-}
+\ No newline at end of file
++}
++
++fun Application.installTestDocuments(token: String, documents: EditionStore) {
++    attributes.put(SharedTokenKey, token)
++    attributes.put(DocumentStoreKey, documents)
++}
+diff --git a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+index 23dc320..fc711d4 100644
+--- a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
++++ b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+@@ -1,20 +1,194 @@
+ package hu.toliver.matutine
+ 
+-import io.ktor.client.request.*
+-import io.ktor.client.statement.*
+-import io.ktor.http.*
+-import io.ktor.server.testing.*
+-import kotlin.test.*
++import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionRole
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.EditionTranscript
++import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SourceList
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.todayLocalDate
++import hu.toliver.matutine.edition.toDayKey
++import io.ktor.client.request.get
++import io.ktor.client.request.header
++import io.ktor.client.statement.bodyAsText
++import io.ktor.http.HttpStatusCode
++import io.ktor.server.testing.testApplication
++import java.nio.file.Files
++import kotlin.test.Test
++import kotlin.test.assertEquals
++import kotlin.test.assertFalse
++import kotlin.test.assertNull
++import kotlin.test.assertTrue
+ 
+ class ApplicationTest {
+ 
+     @Test
+-    fun testRoot() = testApplication {
++    fun unsetToken_acceptsTheDemoBearer() = testApplication {
++        val root = Files.createTempDirectory("matutine-demo").toFile()
+         application {
++            installTestDocuments(resolveSharedToken(null), DataDirectoryStore(root.absolutePath))
+             module()
+         }
+-        val response = client.get("/")
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
+         assertEquals(HttpStatusCode.OK, response.status)
+-        assertEquals("Hello, Ktor!", response.bodyAsText())
++        assertTrue(decodeEdition(response.bodyAsText()).isSample)
++    }
++
++    @Test
++    fun blankToken_isTheDemoBearer() {
++        assertEquals("matutine-demo", resolveSharedToken(null))
++        assertEquals("matutine-demo", resolveSharedToken(""))
++        assertEquals("matutine-demo", resolveSharedToken("   "))
++        assertEquals("/data", resolveDataRoot(null))
++        assertEquals("/data", resolveDataRoot(""))
+     }
+-}
+\ No newline at end of file
++
++    @Test
++    fun setToken_rejectsEveryOtherBearerBeforeReadingDocuments() = testApplication {
++        val store = TouchingStore()
++        application {
++            installTestDocuments(resolveSharedToken("lan-secret"), store)
++            module()
++        }
++        val missing = client.get("/editions/current")
++        val wrongScheme = client.get("/editions/current") {
++            header("Authorization", "Token lan-secret")
++        }
++        val wrongToken = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        assertEquals(HttpStatusCode.Unauthorized, missing.status)
++        assertEquals(HttpStatusCode.Unauthorized, wrongScheme.status)
++        assertEquals(HttpStatusCode.Unauthorized, wrongToken.status)
++        assertFalse(missing.bodyAsText().contains("isSample"))
++        assertFalse(wrongToken.bodyAsText().contains("mastheadName"))
++        assertEquals(0, store.touches)
++    }
++
++    @Test
++    fun setToken_acceptsThatBearer() = testApplication {
++        val root = Files.createTempDirectory("matutine-set-token").toFile()
++        application {
++            installTestDocuments(resolveSharedToken("lan-secret"), DataDirectoryStore(root.absolutePath))
++            module()
++        }
++        val accepted = client.get("/editions/current") {
++            header("Authorization", "Bearer lan-secret")
++        }
++        assertEquals(HttpStatusCode.OK, accepted.status)
++        assertTrue(decodeEdition(accepted.bodyAsText()).isSample)
++    }
++
++    @Test
++    fun absentSourceList_returnsTheServerSampleAndDoesNotWriteTheList() = testApplication {
++        val root = Files.createTempDirectory("matutine-sample").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        application {
++            installTestDocuments("matutine-demo", store)
++            module()
++        }
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        val edition = decodeEdition(response.bodyAsText())
++        assertEquals(HttpStatusCode.OK, response.status)
++        assertTrue(edition.isSample)
++        assertTrue(EditionTranscript.render(edition).contains(EditionTranscript.SAMPLE))
++        assertNull(store.readSourceList())
++        assertFalse(root.resolve("sources.json").exists())
++        assertTrue(root.resolve("editions/${edition.dayKey}-morning.json").exists())
++        val again = decodeEdition(
++            client.get("/editions/current") {
++                header("Authorization", "Bearer matutine-demo")
++            }.bodyAsText(),
++        )
++        assertEquals(edition.id, again.id)
++        assertEquals(edition.revision, again.revision)
++    }
++
++    @Test
++    fun existingMorning_isReturnedUnchanged() = testApplication {
++        val root = Files.createTempDirectory("matutine-kept").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        val day = todayLocalDate()
++        val morning = Edition(
++            id = "morning-${day.toDayKey()}",
++            revision = 4,
++            dayKey = day.toDayKey(),
++            role = EditionRole.morning,
++            mastheadName = "Matutine",
++            dateLine = "Kept date",
++            isSample = false,
++            briefing = "Kept by hand",
++            stories = emptyList(),
++            events = emptyList(),
++            tasks = emptyList(),
++        )
++        store.saveMorningEdition(morning)
++        application {
++            installTestDocuments("matutine-demo", store)
++            module()
++        }
++        val response = client.get("/editions/current") {
++            header("Authorization", "Bearer matutine-demo")
++        }
++        val edition = decodeEdition(response.bodyAsText())
++        assertEquals(morning, edition)
++        assertEquals(morning, store.loadMorningEdition(day.toDayKey()))
++    }
++
++    @Test
++    fun documents_areFilesUnderTheDataDirectory() {
++        val root = Files.createTempDirectory("matutine-docs").toFile()
++        val store = DataDirectoryStore(root.absolutePath)
++        store.writeSettings(ReaderSettings())
++        store.writeSourceList(SourceList(id = "sources", revision = 1))
++        store.writeCalendarSnapshot(CalendarSnapshot())
++        val day = todayLocalDate()
++        store.saveMorningEdition(
++            Edition(
++                id = "morning-${day.toDayKey()}",
++                revision = 1,
++                dayKey = day.toDayKey(),
++                role = EditionRole.morning,
++                mastheadName = "Matutine",
++                dateLine = "Date",
++                isSample = false,
++                briefing = "Brief",
++                stories = emptyList(),
++                events = emptyList(),
++                tasks = emptyList(),
++            ),
++        )
++        assertTrue(root.resolve("settings.json").isFile)
++        assertTrue(root.resolve("sources.json").isFile)
++        assertTrue(root.resolve("calendar-snapshot.json").isFile)
++        assertTrue(root.resolve("editions/${day.toDayKey()}-morning.json").isFile)
++    }
++}
++
++private class TouchingStore : EditionStore {
++    var touches: Int = 0
++
++    private fun touch(): Nothing {
++        touches += 1
++        error("store touched")
++    }
++
++    override fun readSourceList() = touch()
++    override fun writeSourceList(sourceList: SourceList) = touch()
++    override fun readSettings() = touch()
++    override fun writeSettings(settings: ReaderSettings) = touch()
++    override fun loadMorningEdition(dayKey: String) = touch()
++    override fun saveMorningEdition(edition: Edition) = touch()
++    override fun loadExtras(dayKey: String) = touch()
++    override fun saveExtra(edition: Edition) = touch()
++    override fun readCalendarSnapshot() = touch()
++    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = touch()
++    override fun readCalDavAccount() = touch()
++    override fun writeCalDavAccount(account: hu.toliver.matutine.edition.CalDavAccount) = touch()
++}
+diff --git a/.dockerignore b/.dockerignore
+new file mode 100644
+index 0000000..c100704
+--- /dev/null
++++ b/.dockerignore
+@@ -0,0 +1,9 @@
++.git
++.gradle
++**/.gradle
++**/build
++.idea
++_bmad
++_bmad-output
++*.md
++!README.md
+diff --git a/_bmad-output/implementation-artifacts/epic-3-context.md b/_bmad-output/implementation-artifacts/epic-3-context.md
+new file mode 100644
+index 0000000..1b24060
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/epic-3-context.md
+@@ -0,0 +1,56 @@
++# Epic 3 Context: Read the server's morning edition
++
++<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->
++
++## Goal
++
++O runs one Docker server on the LAN and reads that same morning edition in the browser and on the phone. Admin is where the server is configured. Local papers are not uploaded when a client points at the server. The edit-survives-regenerate rule still applies to editions the server owns.
++
++## Stories
++
++- Story 3.1: Open the server's paper in the browser
++- Story 3.2: Generate the morning edition on the server clock
++- Story 3.3: Configure the server in Admin
++- Story 3.4: Light-edit a block in the browser
++- Story 3.5: Read the server edition on Android
++- Story 3.6: Read the server edition on Desktop
++- Story 3.7: Document the server judge path
++- Story 3.8: Publish the Docker image
++
++## Requirements & Constraints
++
++- One shared token, no accounts, no push. Unset token is the literal `matutine-demo` on the server and on every remote client. The token is required before the newspaper or Admin renders. A mismatch says "That token doesn't match." and keeps the typed value. The same token covers the newspaper and Admin. Every remote call sends `Authorization: Bearer`.
++- The shipped server is a Docker image running the Ktor process. Token, AI key, and timezone come from the process environment, not the image. Edition, settings, source list, and calendar snapshot live in a host directory mounted at `/data`, not in the image layer. Do not document a public port. `java -jar` is not a required launch.
++- When the source-list document is absent, the generator builds an edition from bundled sample feeds and sample calendar data, shows the sample line, and does not write those samples into the list. An empty list is a real configuration and does not fall back to samples. No account and no AI key are required to see a paper.
++- The web client does not run the generator and is not an offline app. Web has no Settings screen. Admin is the web-only place for feeds, ICS, CalDAV, generation time, timezone, interest questions, AI provider and key, and the shared token.
++- The server inserts today's morning edition at the configured time only when that document is absent. It does not replace an existing morning edition. A clock-precise paper is claimed only while this server is running. The server holds the AI key; remote clients do not. The timezone value is whatever the process was given; do not invent an identifier format.
++- Saving a server base URL and token does not upload local editions, the local source list, or the local calendar snapshot. Remote clients read, edit, regenerate, or request an extra. They do not mint the morning edition. If the server cannot be reached, show the last stored edition and "Couldn't reach the server. Showing the copy on this device." Generate stays available and writes locally. Android accepts a cleartext `http` LAN URL. Notification time, notification text, theme, and layout stay on the phone. Feeds, calendar, and interest questions are not editable on the phone while a server is configured.
++- Web light edit changes one block in place and saves on leave. Last write wins. There is no merge screen and no revision number on the page. A failed save says "Couldn't save. Your text is still here." Changing the printed date does not change the day key.
++- User-owned text on a server-owned edition survives regenerate the same way it does on the device. Whole-document replace stays the write model.
++- Milestone B also publishes the Docker image from the existing release workflow. Secrets stay in the environment.
++
++## Technical Decisions
++
++- Modules stay `shared`, `server`, `androidApp`, `desktopApp`, and `webApp`. `server` owns the scheduler, HTTP API, stores, Docker image, and served web files. `webApp` is Compose Multiplatform for Web, HTTP only, served by the server.
++- Edition documents, settings, the source list, and calendar snapshots use one kotlinx.serialization `Json` configuration with default property names and no naming strategy. `webApp` sends and reads that JSON and does not declare its own model.
++- The server owns these routes, and every remote client uses them: `GET /editions`, `GET /editions/current`, `GET /editions/by-id`, `PUT /editions/by-id`, `POST /editions/morning/regenerate`, `POST /editions`, `GET /settings`, `PUT /settings`, `GET /sources`, `PUT /sources`, `PUT /calendar-snapshot`.
++- Each of those documents is one JSON document. The store assigns the id on insert. The id never changes. Every write gets a new monotonic revision. The latest whole document wins.
++- One source-list document holds RSS/Atom and ICS URLs. The generator reads that list and no other. The server environment writes it only when it is absent.
++- Clients talk only to a repository. With a server base URL, only the server generator writes a new morning paper. Roles stay `morning` and `extra`. The front page is today's morning edition. Regenerate keeps the id. New edition now inserts an extra.
++- Kotlin 2.4.21, Ktor 3.6.1, kotlinx-serialization 1.12.0, Compose Multiplatform 1.12.2, JDK 17. JVM target 17 for `shared`, `server`, and `desktopApp`.
++- Reach is the LAN. One configured base URL. `http` and `https` are the same API with a different scheme. Android network security must allow that URL, including cleartext when the scheme is `http`.
++
++## UX & Interaction Patterns
++
++- The token gate is the first web screen: chrome, one text field, one primary button. No masthead and no edition behind a rejected token. Error copy is "That token doesn't match." The field keeps the typed value.
++- After a valid token, the web Edition is the newspaper. Admin is a separate chrome screen opened from the newspaper, not a Settings screen.
++- Light edit keeps the display type, uses a 1px ink underline, and has no toolbar and no mono face. "Saved." is the last confirmed line.
++- The Edition does not say whether the paper came from the device or the server.
++- Below 960px the web client uses the single-column order. Chrome controls are at least 40px. The edition itself stays the custom paper, not chrome.
++
++## Cross-Story Dependencies
++
++- Depends on Epic 1 and Epic 2. The sample-when-absent rule, the edition document, and the edit-survives-regenerate rule already exist and apply to editions the server owns.
++- The browser paper and token come before the server clock, Admin, light edit, and phone or desktop remote mode.
++- The server clock owns insert-if-absent for today's morning edition. Later stories must not replace an existing morning edition.
++- The README server path and the release image come after the server and browser actually show a morning paper. Neither documents public internet exposure.
+diff --git a/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
+new file mode 100644
+index 0000000..afd8a10
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
+@@ -0,0 +1,105 @@
++---
++title: 'Open the server''s paper in the browser'
++type: 'feature'
++created: '2026-11-10'
++status: 'in-review'
++baseline_commit: '4b57ede7cf6ca7d43bbf180250b81d904035a479'
++route: 'dispatch'
++review_loop_iteration: 0
++context:
++  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
++  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md'
++---
++
++<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
++
++## Intent
++
++**Problem:** The LAN paper is not visible. `:server` is a hello-world Ktor process, and `:app:webApp` opens the on-device app, which generates a paper in the browser.
++
++**Approach:** Ship a Dockerized Ktor process that stores documents under `/data`, requires one Bearer token, and serves a browser client whose first screen is a token gate and whose next screen is the sample newspaper generated on the server.
++
++## Boundaries & Constraints
++
++**Always:**
++- Unset or blank `MATUTINE_TOKEN` is the literal `matutine-demo`. One shared token. Every edition API call uses `Authorization: Bearer <token>`.
++- Documents live in the host directory mounted at `/data` (override `MATUTINE_DATA` only for tests). Same JSON codec and file names as the existing store: `sources.json`, `settings.json`, `calendar-snapshot.json`, `editions/<dayKey>-morning.json`.
++- The only edition route in this story is `GET /editions/current`. A missing or wrong bearer token is rejected before the store is read and before any edition body is written.
++- An absent source list and an absent morning document: the server builds the bundled sample through `LocalEditionRepository.loadCurrentMorningEdition` and returns it, with the sample line. An existing morning document is returned unchanged.
++- The web client decodes `Edition` with `decodeEdition`. It does not declare an edition model and does not call the generator.
++- The token gate is chrome: one field, one primary button labeled Continue. No paper and no Admin until the current-edition call succeeds. A rejected token shows "That token doesn't match." and keeps the typed value.
++
++**Never:**
++- No scheduler, no `runMorning`, no Admin, no light edit, no client sync, no Settings screen, no service worker, no public-port instructions, no README change, no timezone identifier format.
++- Do not add the other AD-16 routes. Do not replace an existing morning edition. Do not depend on `:app:shared` from `:server`.
++
++## I/O & Edge-Case Matrix
++
++| Scenario | Input / State | Expected Output / Behavior | Error Handling |
++|----------|--------------|---------------------------|----------------|
++| Default token | Env token unset or blank | Accepted bearer is `Bearer matutine-demo` | N/A |
++| Set token | `MATUTINE_TOKEN=lan-secret` | Only `Bearer lan-secret` is accepted | Other values rejected |
++| No or wrong bearer | Missing header, non-Bearer scheme, or wrong token | 401, body is not an edition document, store is not read | N/A |
++| Absent source list | Valid bearer, no `sources.json`, no morning file | 200 `Edition` JSON, `isSample` true, sample line text present in the decoded edition, morning file written, `sources.json` still absent | N/A |
++| Existing morning | Valid bearer, morning file already present | 200 returns that document unchanged; revision and id stay | N/A |
++| Wrong gate token | Browser submits a value the server rejects | Gate stays, field keeps the value, message "That token doesn't match.", Edition and Admin are not composed | N/A |
++| Right gate token | Browser submits the server token and the source list is absent | Edition screen shows the returned paper, including SAMPLE EDITION | A non-401 failure stays on the gate and does not show a paper |
++
++</frozen-after-approval>
++
++## Code Map
++
++- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- replace the hello route with config, auth, `GET /editions/current`, and static web files.
++- `server/build.gradle.kts` -- already depends on `:core`. Keep that. Wire the Wasm distribution into the server resources.
++- `core/.../EditionRepository.kt` -- `LocalEditionRepository.loadCurrentMorningEdition` already saves the sample when the source list is absent and returns an existing morning first. Reuse it. Do not call `runMorning`.
++- `core/.../EditionJson.kt` -- `editionJson`, `encodeEdition`, `decodeEdition`. The only codec.
++- `app/shared/.../FileEditionStore.jvm.kt` -- file names to match. Do not depend on this module from the server; it pulls Compose.
++- `app/shared/.../EditionScreen.kt` -- newspaper drawing. Add a defaulted read-only switch so web can hide Settings, New edition now, and task writes. Android and Desktop call sites stay as they are.
++- `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- today calls `App()`, which runs `MatutineApp` and the local generator. Replace that entry.
++- `app/shared/.../App.kt` -- leave the device preview entry unchanged.
++- `gradle/libs.versions.toml` -- Ktor 3.6.0 is the catalog pin. Do not bump it.
++
++## Tasks & Acceptance
++
++**Execution:**
++- [x] `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` -- `EditionStore` on the data directory using core codecs and the existing file names.
++- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- token resolution, bearer gate, current-edition route, static files from the built web app.
++- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- cover the I/O rows that the server owns, including a store probe that fails if a rejected call touches documents.
++- [x] `server/Dockerfile` -- image runs the fat jar, store path `/data`, token and other secrets only from the environment.
++- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- defaulted read-only chrome so existing device screens keep their controls.
++- [x] `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- token gate, then read-only `EditionScreen` from `GET /editions/current` decoded with `decodeEdition`.
++- [x] `server/build.gradle.kts` -- depend on the Wasm browser distribution and package those files for the server to serve.
++
++**Acceptance Criteria:**
++- Given the Docker image and a host directory mounted at `/data`, when the container starts with no token in the environment, then the accepted token is `matutine-demo` and edition, settings, source list, and calendar snapshot files are under `/data`.
++- Given the wrong token, when it is submitted, then the page says "That token doesn't match.", the field keeps the typed value, and neither the newspaper nor Admin is shown.
++- Given the right token and an absent source list, when the gate opens, then the web Edition shows the server-generated sample paper and the sample line.
++- Given any current-edition call, when the bearer token is missing or wrong, then the server rejects it before returning an edition.
++- The web bundle does not invoke the generator, and it is not an offline app.
++
++## Implementation Notes
++
++- Token environment name is `MATUTINE_TOKEN`. Blank and unset both resolve to `matutine-demo`. Data directory is `MATUTINE_DATA`, default `/data`.
++- The server store copies the desktop file names and uses the core codecs. It does not depend on `:app:shared`.
++- `GET /editions/current` is the only API route. A rejected bearer returns plain text `unauthorized` and does not call the store.
++- Sample generation is `LocalEditionRepository.loadCurrentMorningEdition`. `runMorning` is not called.
++- The gate button label is Continue. The field label is Token. The web entry no longer calls `App()`.
++- Wasm fetch uses the browser `window.fetch` Promise. JS uses the same call. Both send `Authorization: Bearer` and decode with `decodeEdition`.
++- `./gradlew :server:test` passed 7 tests. `:app:shared:jvmTest --tests hu.toliver.matutine.edition.WebPaperGateTest` passed 4 tests. `:server:buildFatJar` produced `server/build/libs/server-all.jar`.
++- The browser showed the chrome token gate at `http://127.0.0.1:8080/` with one field and Continue, and no newspaper. Compose on Wasm did not accept the automated click, so the mismatch sentence and the sample paper were not exercised in that browser session. Curl against the running jar confirmed 401 for a missing or wrong bearer and a sample edition for `Bearer matutine-demo`, with only `editions/<day>-morning.json` written.
++- The first Docker build failed because the Wasm Node binary needs `libatomic.so.1`, which the Temurin JDK image does not ship. The build stage installs `libatomic1` before Gradle. The jar's bytecode is class version 65, so the runtime image is Temurin 21.
++
++## Spec Change Log
++
++## Review Triage Log
++
++## Design Notes
++
++The server implements `EditionStore` itself so the process can use `LocalEditionRepository` without taking a dependency on `:app:shared`. `GET /editions/current` is the one AD-16 route this story needs; the other routes stay absent until their stories. The gate compares nothing locally: only a successful current-edition response opens the paper. The primary button label is Continue. The field label is Token.
++
++## Verification
++
++**Commands:**
++- `./gradlew :server:test` -- expected: bearer, sample, and existing-morning tests pass.
++- `./gradlew :server:buildFatJar :app:webApp:wasmJsBrowserDistribution` -- expected: fat jar and Wasm distribution build.
++- `docker build -t matutine -f server/Dockerfile .` -- expected: image builds when Docker is available.
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
+new file mode 100644
+index 0000000..2dbdf09
+--- /dev/null
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
+@@ -0,0 +1,23 @@
++package hu.toliver.matutine.edition
++
++const val TOKEN_DOES_NOT_MATCH = "That token doesn't match."
++
++data class WebPaperGate(
++    val typedToken: String,
++    val showEdition: Boolean,
++    val message: String?,
++)
++
++/**
++ * The browser gate does not compare the token itself. A 200 from the current-edition
++ * route is the only path that opens the paper. The typed value is returned unchanged.
++ */
++fun webPaperGate(typedToken: String, httpStatus: Int): WebPaperGate = when (httpStatus) {
++    200 -> WebPaperGate(typedToken = typedToken, showEdition = true, message = null)
++    401 -> WebPaperGate(
++        typedToken = typedToken,
++        showEdition = false,
++        message = TOKEN_DOES_NOT_MATCH,
++    )
++    else -> WebPaperGate(typedToken = typedToken, showEdition = false, message = null)
++}
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
+new file mode 100644
+index 0000000..9d09d01
+--- /dev/null
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
+@@ -0,0 +1,43 @@
++package hu.toliver.matutine.edition
++
++import kotlin.test.Test
++import kotlin.test.assertEquals
++import kotlin.test.assertFalse
++import kotlin.test.assertNull
++import kotlin.test.assertTrue
++
++class WebPaperGateTest {
++
++    @Test
++    fun wrongToken_keepsTheTypedValueAndDoesNotOpenThePaper() {
++        val decision = webPaperGate(typedToken = "not-the-token", httpStatus = 401)
++        assertEquals("not-the-token", decision.typedToken)
++        assertEquals(TOKEN_DOES_NOT_MATCH, decision.message)
++        assertFalse(decision.showEdition)
++    }
++
++    @Test
++    fun rightToken_opensTheEditionWithoutAGateMessage() {
++        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 200)
++        assertEquals("matutine-demo", decision.typedToken)
++        assertNull(decision.message)
++        assertTrue(decision.showEdition)
++    }
++
++    @Test
++    fun otherFailure_staysOnTheGateWithoutAPaper() {
++        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 503)
++        assertEquals("matutine-demo", decision.typedToken)
++        assertNull(decision.message)
++        assertFalse(decision.showEdition)
++    }
++
++    @Test
++    fun sampleEditionCarriesTheSampleLine() {
++        val edition = SampleGenerator.forDay(todayLocalDate())
++        val decoded = decodeEdition(encodeEdition(edition))
++        assertTrue(decoded.isSample)
++        assertTrue(EditionTranscript.render(decoded).contains(EditionTranscript.SAMPLE))
++        assertEquals("SAMPLE EDITION", EditionTranscript.SAMPLE)
++    }
++}
+diff --git a/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
+new file mode 100644
+index 0000000..75b86ea
+--- /dev/null
++++ b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
+@@ -0,0 +1,16 @@
++package hu.toliver.matutine
++
++import kotlinx.browser.window
++import kotlinx.coroutines.await
++import org.w3c.fetch.Headers
++import org.w3c.fetch.RequestInit
++
++actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
++    val headers = Headers()
++    headers.append("Authorization", "Bearer $token")
++    val response = window.fetch(
++        "/editions/current",
++        RequestInit(method = "GET", headers = headers),
++    ).await()
++    return EditionFetch(status = response.status.toInt(), body = response.text().await())
++}
+diff --git a/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
+new file mode 100644
+index 0000000..a20cff6
+--- /dev/null
++++ b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
+@@ -0,0 +1,23 @@
++package hu.toliver.matutine
++
++import kotlin.js.ExperimentalWasmJsInterop
++import kotlin.js.JsString
++import kotlin.js.Promise
++import kotlinx.browser.window
++import kotlinx.coroutines.await
++import org.w3c.fetch.Headers
++import org.w3c.fetch.RequestInit
++import org.w3c.fetch.Response
++
++@OptIn(ExperimentalWasmJsInterop::class)
++actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
++    val headers = Headers()
++    headers.append("Authorization", "Bearer $token")
++    val pending: Promise<Response> = window.fetch(
++        "/editions/current",
++        RequestInit(method = "GET", headers = headers),
++    )
++    val response = pending.await()
++    val body = response.text().await<JsString>().toString()
++    return EditionFetch(status = response.status.toInt(), body = body)
++}
+diff --git a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
+new file mode 100644
+index 0000000..076a567
+--- /dev/null
++++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
+@@ -0,0 +1,8 @@
++package hu.toliver.matutine
++
++data class EditionFetch(
++    val status: Int,
++    val body: String,
++)
++
++expect suspend fun fetchCurrentEdition(token: String): EditionFetch
+diff --git a/server/Dockerfile b/server/Dockerfile
+new file mode 100644
+index 0000000..8f444b4
+--- /dev/null
++++ b/server/Dockerfile
+@@ -0,0 +1,15 @@
++FROM eclipse-temurin:17-jdk AS build
++RUN apt-get update \
++    && apt-get install -y --no-install-recommends libatomic1 \
++    && rm -rf /var/lib/apt/lists/*
++WORKDIR /src
++COPY . .
++RUN ./gradlew :server:buildFatJar --no-daemon
++
++FROM eclipse-temurin:21-jre
++WORKDIR /app
++COPY --from=build /src/server/build/libs/*-all.jar /app/server.jar
++ENV MATUTINE_DATA=/data
++EXPOSE 8080
++VOLUME /data
++ENTRYPOINT ["java", "-jar", "/app/server.jar"]
+diff --git a/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
+new file mode 100644
+index 0000000..4ab6443
+--- /dev/null
++++ b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
+@@ -0,0 +1,96 @@
++package hu.toliver.matutine
++
++import hu.toliver.matutine.edition.CalDavAccount
++import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.Edition
++import hu.toliver.matutine.edition.EditionRole
++import hu.toliver.matutine.edition.EditionStore
++import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SourceList
++import hu.toliver.matutine.edition.decodeCalDavAccount
++import hu.toliver.matutine.edition.decodeCalendarSnapshot
++import hu.toliver.matutine.edition.decodeEdition
++import hu.toliver.matutine.edition.decodeReaderSettings
++import hu.toliver.matutine.edition.decodeSourceList
++import hu.toliver.matutine.edition.encodeCalDavAccount
++import hu.toliver.matutine.edition.encodeCalendarSnapshot
++import hu.toliver.matutine.edition.encodeEdition
++import hu.toliver.matutine.edition.encodeReaderSettings
++import hu.toliver.matutine.edition.encodeSourceList
++import java.io.File
++
++/**
++ * Same document names as the desktop file store, rooted at the server data directory.
++ * The server does not depend on `:app:shared` because that module pulls Compose.
++ */
++class DataDirectoryStore(
++    rootPath: String,
++) : EditionStore {
++    private val root = File(rootPath)
++
++    init {
++        root.mkdirs()
++    }
++
++    override fun readSourceList(): SourceList? = read("sources.json", ::decodeSourceList)
++
++    override fun writeSourceList(sourceList: SourceList) {
++        write("sources.json", encodeSourceList(sourceList))
++    }
++
++    override fun readSettings(): ReaderSettings? = read("settings.json", ::decodeReaderSettings)
++
++    override fun writeSettings(settings: ReaderSettings) {
++        write("settings.json", encodeReaderSettings(settings))
++    }
++
++    override fun loadMorningEdition(dayKey: String): Edition? =
++        read("editions/$dayKey-morning.json", ::decodeEdition)
++
++    override fun saveMorningEdition(edition: Edition) {
++        require(edition.role == EditionRole.morning)
++        write("editions/${edition.dayKey}-morning.json", encodeEdition(edition))
++    }
++
++    override fun loadExtras(dayKey: String): List<Edition> {
++        val dir = File(root, "editions")
++        if (!dir.exists()) return emptyList()
++        val prefix = "extra-$dayKey-"
++        return dir.listFiles().orEmpty()
++            .filter { it.isFile && it.name.startsWith(prefix) && it.name.endsWith(".json") }
++            .map { decodeEdition(it.readText()) }
++            .filter { it.role == EditionRole.extra && it.dayKey == dayKey }
++            .sortedBy { it.id.substringAfterLast("-").toIntOrNull() ?: 0 }
++    }
++
++    override fun saveExtra(edition: Edition) {
++        require(edition.role == EditionRole.extra)
++        require(edition.id.startsWith("extra-${edition.dayKey}-"))
++        write("editions/${edition.id}.json", encodeEdition(edition))
++    }
++
++    override fun readCalendarSnapshot(): CalendarSnapshot? =
++        read("calendar-snapshot.json", ::decodeCalendarSnapshot)
++
++    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
++        write("calendar-snapshot.json", encodeCalendarSnapshot(snapshot))
++    }
++
++    override fun readCalDavAccount(): CalDavAccount? = read("caldav.json", ::decodeCalDavAccount)
++
++    override fun writeCalDavAccount(account: CalDavAccount) {
++        write("caldav.json", encodeCalDavAccount(account))
++    }
++
++    private fun <T> read(relativePath: String, decode: (String) -> T): T? {
++        val file = File(root, relativePath)
++        if (!file.exists()) return null
++        return decode(file.readText())
++    }
++
++    private fun write(relativePath: String, json: String) {
++        val file = File(root, relativePath)
++        file.parentFile?.mkdirs()
++        file.writeText(json)
++    }
++}
+
+```
diff --git a/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
new file mode 100644
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
+---
+title: 'Open the server''s paper in the browser'
+type: 'feature'
+created: '2026-11-10'
+status: 'in-review'
+baseline_commit: '4b57ede7cf6ca7d43bbf180250b81d904035a479'
+route: 'dispatch'
+review_loop_iteration: 0
+context:
+  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
+  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md'
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** The LAN paper is not visible. `:server` is a hello-world Ktor process, and `:app:webApp` opens the on-device app, which generates a paper in the browser.
+
+**Approach:** Ship a Dockerized Ktor process that stores documents under `/data`, requires one Bearer token, and serves a browser client whose first screen is a token gate and whose next screen is the sample newspaper generated on the server.
+
+## Boundaries & Constraints
+
+**Always:**
+- Unset or blank `MATUTINE_TOKEN` is the literal `matutine-demo`. One shared token. Every edition API call uses `Authorization: Bearer <token>`.
+- Documents live in the host directory mounted at `/data` (override `MATUTINE_DATA` only for tests). Same JSON codec and file names as the existing store: `sources.json`, `settings.json`, `calendar-snapshot.json`, `editions/<dayKey>-morning.json`.
+- The only edition route in this story is `GET /editions/current`. A missing or wrong bearer token is rejected before the store is read and before any edition body is written.
+- An absent source list and an absent morning document: the server builds the bundled sample through `LocalEditionRepository.loadCurrentMorningEdition` and returns it, with the sample line. An existing morning document is returned unchanged.
+- The web client decodes `Edition` with `decodeEdition`. It does not declare an edition model and does not call the generator.
+- The token gate is chrome: one field, one primary button labeled Continue. No paper and no Admin until the current-edition call succeeds. A rejected token shows "That token doesn't match." and keeps the typed value.
+
+**Never:**
+- No scheduler, no `runMorning`, no Admin, no light edit, no client sync, no Settings screen, no service worker, no public-port instructions, no README change, no timezone identifier format.
+- Do not add the other AD-16 routes. Do not replace an existing morning edition. Do not depend on `:app:shared` from `:server`.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Default token | Env token unset or blank | Accepted bearer is `Bearer matutine-demo` | N/A |
+| Set token | `MATUTINE_TOKEN=lan-secret` | Only `Bearer lan-secret` is accepted | Other values rejected |
+| No or wrong bearer | Missing header, non-Bearer scheme, or wrong token | 401, body is not an edition document, store is not read | N/A |
+| Absent source list | Valid bearer, no `sources.json`, no morning file | 200 `Edition` JSON, `isSample` true, sample line text present in the decoded edition, morning file written, `sources.json` still absent | N/A |
+| Existing morning | Valid bearer, morning file already present | 200 returns that document unchanged; revision and id stay | N/A |
+| Wrong gate token | Browser submits a value the server rejects | Gate stays, field keeps the value, message "That token doesn't match.", Edition and Admin are not composed | N/A |
+| Right gate token | Browser submits the server token and the source list is absent | Edition screen shows the returned paper, including SAMPLE EDITION | A non-401 failure stays on the gate and does not show a paper |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- replace the hello route with config, auth, `GET /editions/current`, and static web files.
+- `server/build.gradle.kts` -- already depends on `:core`. Keep that. Wire the Wasm distribution into the server resources.
+- `core/.../EditionRepository.kt` -- `LocalEditionRepository.loadCurrentMorningEdition` already saves the sample when the source list is absent and returns an existing morning first. Reuse it. Do not call `runMorning`.
+- `core/.../EditionJson.kt` -- `editionJson`, `encodeEdition`, `decodeEdition`. The only codec.
+- `app/shared/.../FileEditionStore.jvm.kt` -- file names to match. Do not depend on this module from the server; it pulls Compose.
+- `app/shared/.../EditionScreen.kt` -- newspaper drawing. Add a defaulted read-only switch so web can hide Settings, New edition now, and task writes. Android and Desktop call sites stay as they are.
+- `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- today calls `App()`, which runs `MatutineApp` and the local generator. Replace that entry.
+- `app/shared/.../App.kt` -- leave the device preview entry unchanged.
+- `gradle/libs.versions.toml` -- Ktor 3.6.0 is the catalog pin. Do not bump it.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` -- `EditionStore` on the data directory using core codecs and the existing file names.
+- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- token resolution, bearer gate, current-edition route, static files from the built web app.
+- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- cover the I/O rows that the server owns, including a store probe that fails if a rejected call touches documents.
+- [x] `server/Dockerfile` -- image runs the fat jar, store path `/data`, token and other secrets only from the environment.
+- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- defaulted read-only chrome so existing device screens keep their controls.
+- [x] `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- token gate, then read-only `EditionScreen` from `GET /editions/current` decoded with `decodeEdition`.
+- [x] `server/build.gradle.kts` -- depend on the Wasm browser distribution and package those files for the server to serve.
+
+**Acceptance Criteria:**
+- Given the Docker image and a host directory mounted at `/data`, when the container starts with no token in the environment, then the accepted token is `matutine-demo` and edition, settings, source list, and calendar snapshot files are under `/data`.
+- Given the wrong token, when it is submitted, then the page says "That token doesn't match.", the field keeps the typed value, and neither the newspaper nor Admin is shown.
+- Given the right token and an absent source list, when the gate opens, then the web Edition shows the server-generated sample paper and the sample line.
+- Given any current-edition call, when the bearer token is missing or wrong, then the server rejects it before returning an edition.
+- The web bundle does not invoke the generator, and it is not an offline app.
+
+## Implementation Notes
+
+- Token environment name is `MATUTINE_TOKEN`. Blank and unset both resolve to `matutine-demo`. Data directory is `MATUTINE_DATA`, default `/data`.
+- The server store copies the desktop file names and uses the core codecs. It does not depend on `:app:shared`.
+- `GET /editions/current` is the only API route. A rejected bearer returns plain text `unauthorized` and does not call the store.
+- Sample generation is `LocalEditionRepository.loadCurrentMorningEdition`. `runMorning` is not called.
+- The gate button label is Continue. The field label is Token. The web entry no longer calls `App()`.
+- Wasm fetch uses the browser `window.fetch` Promise. JS uses the same call. Both send `Authorization: Bearer` and decode with `decodeEdition`.
+- `./gradlew :server:test` passed 7 tests. `:app:shared:jvmTest --tests hu.toliver.matutine.edition.WebPaperGateTest` passed 4 tests. `:server:buildFatJar` produced `server/build/libs/server-all.jar`.
+- The browser showed the chrome token gate at `http://127.0.0.1:8080/` with one field and Continue, and no newspaper. Compose on Wasm did not accept the automated click, so the mismatch sentence and the sample paper were not exercised in that browser session. Curl against the running jar confirmed 401 for a missing or wrong bearer and a sample edition for `Bearer matutine-demo`, with only `editions/<day>-morning.json` written.
+- The first Docker build failed because the Wasm Node binary needs `libatomic.so.1`, which the Temurin JDK image does not ship. The build stage installs `libatomic1` before Gradle. The jar's bytecode is class version 65, so the runtime image is Temurin 21.
+
+## Spec Change Log
+
+## Review Triage Log
+
+## Design Notes
+
+The server implements `EditionStore` itself so the process can use `LocalEditionRepository` without taking a dependency on `:app:shared`. `GET /editions/current` is the one AD-16 route this story needs; the other routes stay absent until their stories. The gate compares nothing locally: only a successful current-edition response opens the paper. The primary button label is Continue. The field label is Token.
+
+## Verification
+
+**Commands:**
+- `./gradlew :server:test` -- expected: bearer, sample, and existing-morning tests pass.
+- `./gradlew :server:buildFatJar :app:webApp:wasmJsBrowserDistribution` -- expected: fat jar and Wasm distribution build.
+- `docker build -t matutine -f server/Dockerfile .` -- expected: image builds when Docker is available.
diff --git a/_bmad-output/implementation-artifacts/spec-3-2-generate-the-morning-edition-on-the-server-clock.md b/_bmad-output/implementation-artifacts/spec-3-2-generate-the-morning-edition-on-the-server-clock.md
new file mode 100644
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-3-2-generate-the-morning-edition-on-the-server-clock.md
+---
+title: 'Generate the morning edition on the server clock'
+type: 'feature'
+created: '2026-11-10'
+status: 'in-review'
+baseline_commit: '4b57ede7cf6ca7d43bbf180250b81d904035a479'
+route: 'dispatch'
+review_loop_iteration: 0
+context:
+  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
+  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Matutine-2026-11-07/ARCHITECTURE-SPINE.md'
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** Today's morning edition appears only if a device is open or someone loads the paper. The server process has no clock, so a closed phone and desktop leave no edition.
+
+**Approach:** While the Ktor process is running, a coroutine scheduler inserts today's morning edition at the configured time in the process timezone, and only when that document is absent. The generator assigns the day key. The AI key is read from the server environment and is not written onto a client or into the store.
+
+## Boundaries & Constraints
+
+**Always:**
+- AD-11: only this server coroutine scheduler creates the morning edition at a configured time, and only when today's morning document is absent. AD-3: a later automatic fire does not write. The generator (`SampleGenerator.forDay` or `EditionGenerator.fromFeedResults`) assigns the day key. The scheduler does not format it.
+- The zone string and the generation-time string come from the process environment. Blank zone uses the process zone. Blank generation time is `07:00`. A generation time `parseNotificationTime` accepts is the clock. The scheduler fires only while the local minute in that zone equals that clock, including when the process is already inside that minute. It does not catch up later in the day.
+- An absent source list inserts the bundled sample. A present list, including an empty one, uses `runMorning` and does not fall back to samples. `serverConfigured` stays false on this process. `generateToday` is not the clock.
+- An existing morning document, including a saved sample or a hand-edited briefing, is left unread from sources and is not replaced.
+- The AI key is `MATUTINE_GEMINI_KEY`. Blank means no key. The repository sees it only in memory. `settings.json` keeps whatever key it already had, or stays absent. The web client is unchanged and does not receive the key.
+- The same zone and clock feed `GET /editions/current` and the scheduler. A failed insert leaves stored documents unchanged and leaves the process running.
+
+**Never:**
+- No Admin form, light edit, client sync, public port, README change, or timezone-identifier design.
+- Do not redo story 3.1's token gate, and do not add the other AD-16 routes.
+- Do not start the Android emulator. Do not change device schedulers.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Due, no paper, no source list | Local minute equals the clock, no morning file, no `sources.json` | One sample morning file whose `dayKey` is the generator's date in the given zone | N/A |
+| Zone crosses midnight | Instant is still the previous UTC date, zone string is the one the process was given | `dayKey` is the civil date `todayLocalDate` would use in that zone | N/A |
+| Already stored | Morning file exists, briefing edited | Tick does not write; id, revision, and briefing stay | N/A |
+| Second fire | Same local day after a successful insert | No second write and no source read | N/A |
+| Not yet due | Local time is before the clock minute, and the process did not start inside it | No morning file is created | N/A |
+| After the minute | Process observes only a later minute the same day | No catch-up write | N/A |
+| Empty or bad list | Source list present, feeds missing or unreadable | No morning file; list file unchanged | Insert failure does not stop the process |
+| AI key | `MATUTINE_GEMINI_KEY` set, personal morning inserted | Summarizer receives that key; `settings.json` does not gain it | Blank key leaves the extractive briefing |
+| Stopped process | Module is not running | No scheduled insert | N/A |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` — token and `GET /editions/current` already exist. Build one `LocalEditionRepository` with `today = { todayLocalDate(zone) }`, that `timeZone`, a JVM feed reader, and a Gemini `summarize`. Start the clock from `module` and cancel it on application stop. Do not call `generateToday`.
+- `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` — documents under the data root. Do not add a second file layout.
+- `core/.../EditionRepository.kt` — `runMorning` inserts only when absent and `serverConfigured` is false. `loadCurrentMorningEdition` inserts the sample when the source list is absent and returns an existing morning first. Reuse both. Do not change device `runMorning`.
+- `core/.../DayKey.kt` — `todayLocalDate(timeZone)` and `toDayKey`. The repository calls these. The clock does not write a key.
+- `core/.../ReaderSettings.kt` — `parseNotificationTime` and `DEFAULT_NOTIFICATION_TIME` (`07:00`). Reuse them. `geminiKey` stays on the settings document only when something else saved it.
+- `core/.../EditionGenerator.kt` and `SampleGenerator.kt` — assign `dayKey` from the `LocalDate` they are given.
+- `app/shared/.../FeedReader.jvm.kt` and `GeminiBriefingClient.jvm.kt` — the HTTP shape to copy into `:server`. Do not depend on `:app:shared`.
+- `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` — story 3.1 rows stay. Add clock rows here. Tests pass zone, generation time, key, and instant in; they do not need Admin.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `server/src/main/kotlin/hu/toliver/matutine/ServerMorningClock.kt` — resolve zone and generation time, decide when a tick is due, and insert through the repository only when due.
+- [x] `server/src/main/kotlin/hu/toliver/matutine/ServerGeneration.kt` — JVM feed reader, in-memory Gemini key, and the Gemini summarize call. Never write the env key.
+- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` — one zoned repository for the current-edition route and the coroutine clock.
+- [x] `server/build.gradle.kts` — coroutines for the scheduler.
+- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` — cover the I/O rows for due, zone day key, existing edition, second fire, not-due, no catch-up, bad list, and the env key.
+
+**Acceptance Criteria:**
+- Given the server is running and today's morning edition is absent, when the configured generation time is reached in the server timezone, then the server inserts today's morning edition and the generator assigns the day key in that timezone.
+- Given today's morning edition already exists, when the schedule fires again, then it does not write.
+- Given the process is not running, when the generation time passes, then no scheduled insert happens.
+- Given an AI key in the server environment, when a morning edition is generated, then that key is used in memory and is not stored for clients.
+- Given a timezone string, when the process receives it, then that string is the zone, with no identifier format added by this story.
+
+## Implementation Notes
+
+- The repository date is `clock().toLocalDateTime(zone).date`. With `Clock.System` that is the same civil date as `todayLocalDate(zone)`, and tests can pin the instant.
+- A generation time `parseNotificationTime` rejects does not start the clock. Blank stays `07:00`.
+- `MATUTINE_TIMEZONE`, `MATUTINE_GENERATION_TIME`, and `MATUTINE_GEMINI_KEY` are the environment names. The zone string is passed to `TimeZone.of`.
+- Suspend functions cannot be Ktor attribute types, so tests install feed and summarize lambdas through a `ServerPorts` holder.
+- The clock coroutine is a child of the application scope and is also cancelled on `ApplicationStopped`.
+- `./gradlew :server:test` passed 17 tests, including the story 3.1 rows. `:core:jvmTest --tests hu.toliver.matutine.edition.LocalEditionRepositoryTest` stayed green.
+
+## Spec Change Log
+
+## Review Triage Log
+
+## Design Notes
+
+The clock is a one-second poll inside the server process. A tick is due when the zone-local hour and minute equal the configured clock. Starting during that minute still inserts once. Starting afterward waits until the next day, which is the clock-precise claim: a missed minute is not reconstructed. Absent `sources.json` uses the sample insert already used by `GET /editions/current`. A present list uses `runMorning`. Environment names are `MATUTINE_TIMEZONE`, `MATUTINE_GENERATION_TIME`, and `MATUTINE_GEMINI_KEY`.
+
+## Verification
+
+**Commands:**
+- `./gradlew :server:test` -- expected: story 3.1 bearer and edition tests still pass, and the clock rows pass.
+- `./gradlew :core:jvmTest --tests hu.toliver.matutine.edition.LocalEditionRepositoryTest` -- expected: existing `runMorning` tests still pass.
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
new file mode 100644
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
+package hu.toliver.matutine.edition
+
+const val TOKEN_DOES_NOT_MATCH = "That token doesn't match."
+
+data class WebPaperGate(
+    val typedToken: String,
+    val showEdition: Boolean,
+    val message: String?,
+)
+
+/**
+ * The browser gate does not compare the token itself. A 200 from the current-edition
+ * route is the only path that opens the paper. The typed value is returned unchanged.
+ */
+fun webPaperGate(typedToken: String, httpStatus: Int): WebPaperGate = when (httpStatus) {
+    200 -> WebPaperGate(typedToken = typedToken, showEdition = true, message = null)
+    401 -> WebPaperGate(
+        typedToken = typedToken,
+        showEdition = false,
+        message = TOKEN_DOES_NOT_MATCH,
+    )
+    else -> WebPaperGate(typedToken = typedToken, showEdition = false, message = null)
+}
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
new file mode 100644
--- /dev/null
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
+package hu.toliver.matutine.edition
+
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertNull
+import kotlin.test.assertTrue
+
+class WebPaperGateTest {
+
+    @Test
+    fun wrongToken_keepsTheTypedValueAndDoesNotOpenThePaper() {
+        val decision = webPaperGate(typedToken = "not-the-token", httpStatus = 401)
+        assertEquals("not-the-token", decision.typedToken)
+        assertEquals(TOKEN_DOES_NOT_MATCH, decision.message)
+        assertFalse(decision.showEdition)
+    }
+
+    @Test
+    fun rightToken_opensTheEditionWithoutAGateMessage() {
+        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 200)
+        assertEquals("matutine-demo", decision.typedToken)
+        assertNull(decision.message)
+        assertTrue(decision.showEdition)
+    }
+
+    @Test
+    fun otherFailure_staysOnTheGateWithoutAPaper() {
+        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 503)
+        assertEquals("matutine-demo", decision.typedToken)
+        assertNull(decision.message)
+        assertFalse(decision.showEdition)
+    }
+
+    @Test
+    fun sampleEditionCarriesTheSampleLine() {
+        val edition = SampleGenerator.forDay(todayLocalDate())
+        val decoded = decodeEdition(encodeEdition(edition))
+        assertTrue(decoded.isSample)
+        assertTrue(EditionTranscript.render(decoded).contains(EditionTranscript.SAMPLE))
+        assertEquals("SAMPLE EDITION", EditionTranscript.SAMPLE)
+    }
+}
diff --git a/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
new file mode 100644
--- /dev/null
+++ b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
+package hu.toliver.matutine
+
+import kotlinx.browser.window
+import kotlinx.coroutines.await
+import org.w3c.fetch.Headers
+import org.w3c.fetch.RequestInit
+
+actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
+    val headers = Headers()
+    headers.append("Authorization", "Bearer $token")
+    val response = window.fetch(
+        "/editions/current",
+        RequestInit(method = "GET", headers = headers),
+    ).await()
+    return EditionFetch(status = response.status.toInt(), body = response.text().await())
+}
diff --git a/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
new file mode 100644
--- /dev/null
+++ b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
+package hu.toliver.matutine
+
+import kotlin.js.ExperimentalWasmJsInterop
+import kotlin.js.JsString
+import kotlin.js.Promise
+import kotlinx.browser.window
+import kotlinx.coroutines.await
+import org.w3c.fetch.Headers
+import org.w3c.fetch.RequestInit
+import org.w3c.fetch.Response
+
+@OptIn(ExperimentalWasmJsInterop::class)
+actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
+    val headers = Headers()
+    headers.append("Authorization", "Bearer $token")
+    val pending: Promise<Response> = window.fetch(
+        "/editions/current",
+        RequestInit(method = "GET", headers = headers),
+    )
+    val response = pending.await()
+    val body = response.text().await<JsString>().toString()
+    return EditionFetch(status = response.status.toInt(), body = body)
+}
diff --git a/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
new file mode 100644
--- /dev/null
+++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
+package hu.toliver.matutine
+
+data class EditionFetch(
+    val status: Int,
+    val body: String,
+)
+
+expect suspend fun fetchCurrentEdition(token: String): EditionFetch
diff --git a/server/Dockerfile b/server/Dockerfile
new file mode 100644
--- /dev/null
+++ b/server/Dockerfile
+FROM eclipse-temurin:17-jdk AS build
+RUN apt-get update \
+    && apt-get install -y --no-install-recommends libatomic1 \
+    && rm -rf /var/lib/apt/lists/*
+WORKDIR /src
+COPY . .
+RUN ./gradlew :server:buildFatJar --no-daemon
+
+FROM eclipse-temurin:21-jre
+WORKDIR /app
+COPY --from=build /src/server/build/libs/*-all.jar /app/server.jar
+ENV MATUTINE_DATA=/data
+EXPOSE 8080
+VOLUME /data
+ENTRYPOINT ["java", "-jar", "/app/server.jar"]
diff --git a/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
new file mode 100644
--- /dev/null
+++ b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
+package hu.toliver.matutine
+
+import hu.toliver.matutine.edition.CalDavAccount
+import hu.toliver.matutine.edition.CalendarSnapshot
+import hu.toliver.matutine.edition.Edition
+import hu.toliver.matutine.edition.EditionRole
+import hu.toliver.matutine.edition.EditionStore
+import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SourceList
+import hu.toliver.matutine.edition.decodeCalDavAccount
+import hu.toliver.matutine.edition.decodeCalendarSnapshot
+import hu.toliver.matutine.edition.decodeEdition
+import hu.toliver.matutine.edition.decodeReaderSettings
+import hu.toliver.matutine.edition.decodeSourceList
+import hu.toliver.matutine.edition.encodeCalDavAccount
+import hu.toliver.matutine.edition.encodeCalendarSnapshot
+import hu.toliver.matutine.edition.encodeEdition
+import hu.toliver.matutine.edition.encodeReaderSettings
+import hu.toliver.matutine.edition.encodeSourceList
+import java.io.File
+
+/**
+ * Same document names as the desktop file store, rooted at the server data directory.
+ * The server does not depend on `:app:shared` because that module pulls Compose.
+ */
+class DataDirectoryStore(
+    rootPath: String,
+) : EditionStore {
+    private val root = File(rootPath)
+
+    init {
+        root.mkdirs()
+    }
+
+    override fun readSourceList(): SourceList? = read("sources.json", ::decodeSourceList)
+
+    override fun writeSourceList(sourceList: SourceList) {
+        write("sources.json", encodeSourceList(sourceList))
+    }
+
+    override fun readSettings(): ReaderSettings? = read("settings.json", ::decodeReaderSettings)
+
+    override fun writeSettings(settings: ReaderSettings) {
+        write("settings.json", encodeReaderSettings(settings))
+    }
+
+    override fun loadMorningEdition(dayKey: String): Edition? =
+        read("editions/$dayKey-morning.json", ::decodeEdition)
+
+    override fun saveMorningEdition(edition: Edition) {
+        require(edition.role == EditionRole.morning)
+        write("editions/${edition.dayKey}-morning.json", encodeEdition(edition))
+    }
+
+    override fun loadExtras(dayKey: String): List<Edition> {
+        val dir = File(root, "editions")
+        if (!dir.exists()) return emptyList()
+        val prefix = "extra-$dayKey-"
+        return dir.listFiles().orEmpty()
+            .filter { it.isFile && it.name.startsWith(prefix) && it.name.endsWith(".json") }
+            .map { decodeEdition(it.readText()) }
+            .filter { it.role == EditionRole.extra && it.dayKey == dayKey }
+            .sortedBy { it.id.substringAfterLast("-").toIntOrNull() ?: 0 }
+    }
+
+    override fun saveExtra(edition: Edition) {
+        require(edition.role == EditionRole.extra)
+        require(edition.id.startsWith("extra-${edition.dayKey}-"))
+        write("editions/${edition.id}.json", encodeEdition(edition))
+    }
+
+    override fun readCalendarSnapshot(): CalendarSnapshot? =
+        read("calendar-snapshot.json", ::decodeCalendarSnapshot)
+
+    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
+        write("calendar-snapshot.json", encodeCalendarSnapshot(snapshot))
+    }
+
+    override fun readCalDavAccount(): CalDavAccount? = read("caldav.json", ::decodeCalDavAccount)
+
+    override fun writeCalDavAccount(account: CalDavAccount) {
+        write("caldav.json", encodeCalDavAccount(account))
+    }
+
+    private fun <T> read(relativePath: String, decode: (String) -> T): T? {
+        val file = File(root, relativePath)
+        if (!file.exists()) return null
+        return decode(file.readText())
+    }
+
+    private fun write(relativePath: String, json: String) {
+        val file = File(root, relativePath)
+        file.parentFile?.mkdirs()
+        file.writeText(json)
+    }
+}
diff --git a/server/src/main/kotlin/hu/toliver/matutine/ServerGeneration.kt b/server/src/main/kotlin/hu/toliver/matutine/ServerGeneration.kt
new file mode 100644
--- /dev/null
+++ b/server/src/main/kotlin/hu/toliver/matutine/ServerGeneration.kt
+package hu.toliver.matutine
+
+import hu.toliver.matutine.edition.EditionStore
+import hu.toliver.matutine.edition.EditionStory
+import hu.toliver.matutine.edition.FEED_DIDNT_LOAD
+import hu.toliver.matutine.edition.LocalEditionRepository
+import hu.toliver.matutine.edition.ReaderSettings
+import java.net.HttpURLConnection
+import java.net.URI
+import java.nio.charset.Charset
+import kotlin.time.Instant
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.withContext
+import kotlinx.datetime.TimeZone
+import kotlinx.datetime.toLocalDateTime
+
+private const val GEMINI_GENERATE_URL =
+    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
+
+internal fun serverEditionRepository(
+    documents: EditionStore,
+    zone: TimeZone,
+    clock: () -> Instant,
+    geminiKey: String,
+    readFeed: suspend (String) -> String,
+    summarize: suspend (List<EditionStory>, String) -> String,
+): LocalEditionRepository {
+    val trimmed = geminiKey.trim()
+    val store = if (trimmed.isEmpty()) documents else GeminiKeyOverlay(documents, trimmed)
+    return LocalEditionRepository(
+        store = store,
+        today = { clock().toLocalDateTime(zone).date },
+        timeZone = zone,
+        readFeed = readFeed,
+        summarize = summarize,
+    )
+}
+
+/**
+ * The env key is visible to the generator and is never written back.
+ * A settings file that was already there keeps its own key.
+ */
+private class GeminiKeyOverlay(
+    private val base: EditionStore,
+    private val key: String,
+) : EditionStore by base {
+    override fun readSettings(): ReaderSettings? {
+        val current = base.readSettings() ?: ReaderSettings()
+        return current.copy(geminiKey = key)
+    }
+
+    override fun writeSettings(settings: ReaderSettings) {
+        val stored = base.readSettings()?.geminiKey.orEmpty()
+        base.writeSettings(settings.copy(geminiKey = stored))
+    }
+}
+
+internal suspend fun serverReadFeed(url: String): String = withContext(Dispatchers.IO) {
+    val connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
+        instanceFollowRedirects = true
+        connectTimeout = 20_000
+        readTimeout = 20_000
+        requestMethod = "GET"
+        setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
+        setRequestProperty("User-Agent", "Matutine")
+    }
+    try {
+        if (connection.responseCode !in 200..299) error(FEED_DIDNT_LOAD)
+        val charset = charsetFrom(connection.contentType)
+        connection.inputStream.bufferedReader(charset).use { it.readText() }
+    } finally {
+        connection.disconnect()
+    }
+}
+
+internal suspend fun serverSummarize(stories: List<EditionStory>, apiKey: String): String =
+    withContext(Dispatchers.IO) {
+        val body = geminiGenerateBody(geminiBriefingPrompt(stories))
+        val connection = (URI.create(GEMINI_GENERATE_URL).toURL().openConnection() as HttpURLConnection).apply {
+            instanceFollowRedirects = false
+            connectTimeout = 20_000
+            readTimeout = 20_000
+            requestMethod = "POST"
+            doOutput = true
+            setRequestProperty("Content-Type", "application/json; charset=utf-8")
+            setRequestProperty("Accept", "application/json")
+            setRequestProperty("x-goog-api-key", apiKey)
+        }
+        try {
+            connection.outputStream.use { it.write(body.encodeToByteArray()) }
+            if (connection.responseCode !in 200..299) error("summarizer_unavailable")
+            briefingTextFromGeminiResponse(connection.inputStream.bufferedReader().use { it.readText() })
+        } finally {
+            connection.disconnect()
+        }
+    }
+
+private fun geminiBriefingPrompt(stories: List<EditionStory>): String {
+    val lines = stories.joinToString("\n") { story ->
+        val headline = story.headline.trim()
+        val deck = story.deck.trim()
+        if (deck.isEmpty()) headline else "$headline\n$deck"
+    }
+    return "Write a short newspaper briefing in plain prose from these headlines and decks. " +
+        "No title, no model name, and no list.\n$lines"
+}
+
+private fun geminiGenerateBody(prompt: String): String =
+    """{"contents":[{"parts":[{"text":${jsonString(prompt)}}]}]}"""
+
+private fun briefingTextFromGeminiResponse(body: String): String {
+    val text = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
+        .findAll(body)
+        .map { unescapeJsonString(it.groupValues[1]) }
+        .joinToString("")
+        .trim()
+    if (text.isEmpty()) error("empty_briefing")
+    return text
+}
+
+private fun jsonString(value: String): String = buildString {
+    append('"')
+    value.forEach { char ->
+        when (char) {
+            '\\' -> append("\\\\")
+            '"' -> append("\\\"")
+            '\n' -> append("\\n")
+            '\r' -> append("\\r")
+            '\t' -> append("\\t")
+            else -> append(char)
+        }
+    }
+    append('"')
+}
+
+private fun unescapeJsonString(value: String): String = buildString {
+    var index = 0
+    while (index < value.length) {
+        val char = value[index]
+        if (char != '\\' || index + 1 >= value.length) {
+            append(char)
+            index += 1
+            continue
+        }
+        when (val escaped = value[index + 1]) {
+            '\\', '"', '/' -> {
+                append(escaped)
+                index += 2
+            }
+            'n' -> {
+                append('\n')
+                index += 2
+            }
+            'r' -> {
+                append('\r')
+                index += 2
+            }
+            't' -> {
+                append('\t')
+                index += 2
+            }
+            'u' -> {
+                if (index + 5 >= value.length) error("empty_briefing")
+                append(value.substring(index + 2, index + 6).toInt(16).toChar())
+                index += 6
+            }
+            else -> error("empty_briefing")
+        }
+    }
+}
+
+private fun charsetFrom(contentType: String?): Charset {
+    val name = contentType
+        ?.let { Regex("charset=([^;]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
+        ?.trim()
+        ?.trim('"')
+    if (name.isNullOrBlank()) return Charsets.UTF_8
+    return runCatching { Charset.forName(name) }.getOrDefault(Charsets.UTF_8)
+}
diff --git a/server/src/main/kotlin/hu/toliver/matutine/ServerMorningClock.kt b/server/src/main/kotlin/hu/toliver/matutine/ServerMorningClock.kt
new file mode 100644
--- /dev/null
+++ b/server/src/main/kotlin/hu/toliver/matutine/ServerMorningClock.kt
+package hu.toliver.matutine
+
+import hu.toliver.matutine.edition.DEFAULT_NOTIFICATION_TIME
+import hu.toliver.matutine.edition.EditionStore
+import hu.toliver.matutine.edition.LocalEditionRepository
+import hu.toliver.matutine.edition.parseNotificationTime
+import kotlin.time.Instant
+import kotlinx.coroutines.CancellationException
+import kotlinx.coroutines.delay
+import kotlinx.datetime.LocalDate
+import kotlinx.datetime.TimeZone
+import kotlinx.datetime.toLocalDateTime
+import kotlin.time.Duration.Companion.seconds
+
+internal fun resolveServerTimeZone(raw: String?): TimeZone {
+    val zone = raw?.takeIf { it.isNotBlank() } ?: return TimeZone.currentSystemDefault()
+    return TimeZone.of(zone)
+}
+
+/** Blank is 07:00. A string [parseNotificationTime] rejects does not arm the clock. */
+internal fun resolveGenerationTime(raw: String?): Pair<Int, Int>? {
+    if (raw.isNullOrBlank()) return parseNotificationTime(DEFAULT_NOTIFICATION_TIME)
+    return parseNotificationTime(raw)
+}
+
+internal data class MorningClockMark(val attempted: LocalDate? = null)
+
+/**
+ * One insert the first time this process observes the generation minute.
+ * A later minute the same day is not a catch-up.
+ */
+internal fun morningClockAdvance(
+    mark: MorningClockMark,
+    now: Instant,
+    zone: TimeZone,
+    hour: Int,
+    minute: Int,
+): Pair<MorningClockMark, Boolean> {
+    val local = now.toLocalDateTime(zone)
+    val due = local.hour == hour && local.minute == minute
+    if (!due || mark.attempted == local.date) return mark to false
+    return mark.copy(attempted = local.date) to true
+}
+
+internal suspend fun insertScheduledMorning(
+    store: EditionStore,
+    repository: LocalEditionRepository,
+) {
+    if (store.readSourceList() == null) {
+        repository.loadCurrentMorningEdition().getOrThrow()
+    } else {
+        repository.runMorning().getOrThrow()
+    }
+}
+
+internal class ServerMorningClock(
+    private val zone: TimeZone,
+    private val hour: Int,
+    private val minute: Int,
+    private val clock: () -> Instant,
+    private val insert: suspend () -> Unit,
+) {
+    private var mark = MorningClockMark()
+
+    suspend fun tick() {
+        val (next, due) = morningClockAdvance(mark, clock(), zone, hour, minute)
+        mark = next
+        if (!due) return
+        try {
+            insert()
+        } catch (error: CancellationException) {
+            throw error
+        } catch (_: Throwable) {
+            // A failed insert leaves the store unchanged and the process stays up.
+        }
+    }
+
+    suspend fun run() {
+        while (true) {
+            tick()
+            delay(1.seconds)
+        }
+    }
+}
`. Read that file — it is the content under review.

Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. If the instruction file is unreadable, report that exact failure and stop. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
