Read the review instructions below completely and follow them.

claims_file: the spec is inlined at the end of this prompt. Leave that block unread until your instructions call for it.

Review content: the unified diff below. It is the content under review.

Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. If the instruction file is unreadable, report that exact failure and stop. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.

## Review instructions

# Edge Case Hunter Review

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


## Unified diff

```diff
diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
index 860caa7..30de6a2 100644
--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
+++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
@@ -29,7 +29,7 @@
 # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
 # - Retrospective appends its action items to action_items; the status view surfaces open ones
 generated: 11-07-2026 19:42
-last_updated: 11-10-2026 18:48
+last_updated: 11-10-2026 18:55
 project: Matutine
 project_key: NOKEY
 tracking_system: file-system
@@ -59,8 +59,8 @@ development_status:
   2-7-write-the-briefing-with-a-gemini-key: done
   epic-2-retrospective: optional
 
-  epic-3: backlog
-  3-1-open-the-server-s-paper-in-the-browser: backlog
+  epic-3: in-progress
+  3-1-open-the-server-s-paper-in-the-browser: in-progress
   3-2-generate-the-morning-edition-on-the-server-clock: backlog
   3-3-configure-the-server-in-admin: backlog
   3-4-light-edit-a-block-in-the-browser: backlog
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
index bbd6ccd..1462455 100644
--- a/server/build.gradle.kts
+++ b/server/build.gradle.kts
@@ -16,4 +16,15 @@ dependencies {
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
index 25b8388..c2cafe8 100644
--- a/server/src/main/kotlin/hu/toliver/matutine/Application.kt
+++ b/server/src/main/kotlin/hu/toliver/matutine/Application.kt
@@ -1,10 +1,33 @@
 package hu.toliver.matutine
 
-import io.ktor.server.application.*
-import io.ktor.server.engine.*
-import io.ktor.server.netty.*
-import io.ktor.server.response.*
-import io.ktor.server.routing.*
+import hu.toliver.matutine.edition.EditionStore
+import hu.toliver.matutine.edition.LocalEditionRepository
+import hu.toliver.matutine.edition.encodeEdition
+import io.ktor.http.ContentType
+import io.ktor.http.HttpStatusCode
+import io.ktor.server.application.Application
+import io.ktor.server.application.call
+import io.ktor.server.engine.embeddedServer
+import io.ktor.server.netty.Netty
+import io.ktor.server.response.respondText
+import io.ktor.server.routing.get
+import io.ktor.server.routing.routing
+import io.ktor.server.http.content.staticResources
+import io.ktor.util.AttributeKey
+
+private val SharedTokenKey = AttributeKey<String>("matutine.sharedToken")
+private val DocumentStoreKey = AttributeKey<EditionStore>("matutine.documents")
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
@@ -12,9 +35,34 @@ fun main() {
 }
 
 fun Application.module() {
+    val token = attributes.getOrNull(SharedTokenKey)
+        ?: resolveSharedToken(System.getenv("MATUTINE_TOKEN"))
+    val documents = attributes.getOrNull(DocumentStoreKey)
+        ?: DataDirectoryStore(resolveDataRoot(System.getenv("MATUTINE_DATA")))
+    val repository = LocalEditionRepository(documents)
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
diff --git a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
index 23dc320..fc711d4 100644
--- a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+++ b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
@@ -1,20 +1,194 @@
 package hu.toliver.matutine
 
-import io.ktor.client.request.*
-import io.ktor.client.statement.*
-import io.ktor.http.*
-import io.ktor.server.testing.*
-import kotlin.test.*
+import hu.toliver.matutine.edition.CalendarSnapshot
+import hu.toliver.matutine.edition.Edition
+import hu.toliver.matutine.edition.EditionRole
+import hu.toliver.matutine.edition.EditionStore
+import hu.toliver.matutine.edition.EditionTranscript
+import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SourceList
+import hu.toliver.matutine.edition.decodeEdition
+import hu.toliver.matutine.edition.todayLocalDate
+import hu.toliver.matutine.edition.toDayKey
+import io.ktor.client.request.get
+import io.ktor.client.request.header
+import io.ktor.client.statement.bodyAsText
+import io.ktor.http.HttpStatusCode
+import io.ktor.server.testing.testApplication
+import java.nio.file.Files
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertNull
+import kotlin.test.assertTrue
 
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
index 0000000..c100704
--- /dev/null
+++ b/.dockerignore
@@ -0,0 +1,9 @@
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
index 0000000..1b24060
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/epic-3-context.md
@@ -0,0 +1,56 @@
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
diff --git a/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
new file mode 100644
index 0000000..afd8a10
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-3-1-open-the-server-s-paper-in-the-browser.md
@@ -0,0 +1,105 @@
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
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
new file mode 100644
index 0000000..2dbdf09
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
@@ -0,0 +1,23 @@
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
index 0000000..9d09d01
--- /dev/null
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/WebPaperGateTest.kt
@@ -0,0 +1,43 @@
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
index 0000000..75b86ea
--- /dev/null
+++ b/app/webApp/src/jsMain/kotlin/hu/toliver/matutine/EditionFetch.js.kt
@@ -0,0 +1,16 @@
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
index 0000000..a20cff6
--- /dev/null
+++ b/app/webApp/src/wasmJsMain/kotlin/hu/toliver/matutine/EditionFetch.wasmJs.kt
@@ -0,0 +1,23 @@
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
index 0000000..076a567
--- /dev/null
+++ b/app/webApp/src/webMain/kotlin/hu/toliver/matutine/EditionFetch.kt
@@ -0,0 +1,8 @@
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
index 0000000..8f444b4
--- /dev/null
+++ b/server/Dockerfile
@@ -0,0 +1,15 @@
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
index 0000000..4ab6443
--- /dev/null
+++ b/server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt
@@ -0,0 +1,96 @@
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

```

## Claims file (do not read until your instructions reach the claims step)

```markdown
---
title: 'Open the server''s paper in the browser'
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

**Problem:** The LAN paper is not visible. `:server` is a hello-world Ktor process, and `:app:webApp` opens the on-device app, which generates a paper in the browser.

**Approach:** Ship a Dockerized Ktor process that stores documents under `/data`, requires one Bearer token, and serves a browser client whose first screen is a token gate and whose next screen is the sample newspaper generated on the server.

## Boundaries & Constraints

**Always:**
- Unset or blank `MATUTINE_TOKEN` is the literal `matutine-demo`. One shared token. Every edition API call uses `Authorization: Bearer <token>`.
- Documents live in the host directory mounted at `/data` (override `MATUTINE_DATA` only for tests). Same JSON codec and file names as the existing store: `sources.json`, `settings.json`, `calendar-snapshot.json`, `editions/<dayKey>-morning.json`.
- The only edition route in this story is `GET /editions/current`. A missing or wrong bearer token is rejected before the store is read and before any edition body is written.
- An absent source list and an absent morning document: the server builds the bundled sample through `LocalEditionRepository.loadCurrentMorningEdition` and returns it, with the sample line. An existing morning document is returned unchanged.
- The web client decodes `Edition` with `decodeEdition`. It does not declare an edition model and does not call the generator.
- The token gate is chrome: one field, one primary button labeled Continue. No paper and no Admin until the current-edition call succeeds. A rejected token shows "That token doesn't match." and keeps the typed value.

**Never:**
- No scheduler, no `runMorning`, no Admin, no light edit, no client sync, no Settings screen, no service worker, no public-port instructions, no README change, no timezone identifier format.
- Do not add the other AD-16 routes. Do not replace an existing morning edition. Do not depend on `:app:shared` from `:server`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Default token | Env token unset or blank | Accepted bearer is `Bearer matutine-demo` | N/A |
| Set token | `MATUTINE_TOKEN=lan-secret` | Only `Bearer lan-secret` is accepted | Other values rejected |
| No or wrong bearer | Missing header, non-Bearer scheme, or wrong token | 401, body is not an edition document, store is not read | N/A |
| Absent source list | Valid bearer, no `sources.json`, no morning file | 200 `Edition` JSON, `isSample` true, sample line text present in the decoded edition, morning file written, `sources.json` still absent | N/A |
| Existing morning | Valid bearer, morning file already present | 200 returns that document unchanged; revision and id stay | N/A |
| Wrong gate token | Browser submits a value the server rejects | Gate stays, field keeps the value, message "That token doesn't match.", Edition and Admin are not composed | N/A |
| Right gate token | Browser submits the server token and the source list is absent | Edition screen shows the returned paper, including SAMPLE EDITION | A non-401 failure stays on the gate and does not show a paper |

</frozen-after-approval>

## Code Map

- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- replace the hello route with config, auth, `GET /editions/current`, and static web files.
- `server/build.gradle.kts` -- already depends on `:core`. Keep that. Wire the Wasm distribution into the server resources.
- `core/.../EditionRepository.kt` -- `LocalEditionRepository.loadCurrentMorningEdition` already saves the sample when the source list is absent and returns an existing morning first. Reuse it. Do not call `runMorning`.
- `core/.../EditionJson.kt` -- `editionJson`, `encodeEdition`, `decodeEdition`. The only codec.
- `app/shared/.../FileEditionStore.jvm.kt` -- file names to match. Do not depend on this module from the server; it pulls Compose.
- `app/shared/.../EditionScreen.kt` -- newspaper drawing. Add a defaulted read-only switch so web can hide Settings, New edition now, and task writes. Android and Desktop call sites stay as they are.
- `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- today calls `App()`, which runs `MatutineApp` and the local generator. Replace that entry.
- `app/shared/.../App.kt` -- leave the device preview entry unchanged.
- `gradle/libs.versions.toml` -- Ktor 3.6.0 is the catalog pin. Do not bump it.

## Tasks & Acceptance

**Execution:**
- [x] `server/src/main/kotlin/hu/toliver/matutine/DataDirectoryStore.kt` -- `EditionStore` on the data directory using core codecs and the existing file names.
- [x] `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- token resolution, bearer gate, current-edition route, static files from the built web app.
- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- cover the I/O rows that the server owns, including a store probe that fails if a rejected call touches documents.
- [x] `server/Dockerfile` -- image runs the fat jar, store path `/data`, token and other secrets only from the environment.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- defaulted read-only chrome so existing device screens keep their controls.
- [x] `app/webApp/src/webMain/kotlin/hu/toliver/matutine/main.kt` -- token gate, then read-only `EditionScreen` from `GET /editions/current` decoded with `decodeEdition`.
- [x] `server/build.gradle.kts` -- depend on the Wasm browser distribution and package those files for the server to serve.

**Acceptance Criteria:**
- Given the Docker image and a host directory mounted at `/data`, when the container starts with no token in the environment, then the accepted token is `matutine-demo` and edition, settings, source list, and calendar snapshot files are under `/data`.
- Given the wrong token, when it is submitted, then the page says "That token doesn't match.", the field keeps the typed value, and neither the newspaper nor Admin is shown.
- Given the right token and an absent source list, when the gate opens, then the web Edition shows the server-generated sample paper and the sample line.
- Given any current-edition call, when the bearer token is missing or wrong, then the server rejects it before returning an edition.
- The web bundle does not invoke the generator, and it is not an offline app.

## Implementation Notes

- Token environment name is `MATUTINE_TOKEN`. Blank and unset both resolve to `matutine-demo`. Data directory is `MATUTINE_DATA`, default `/data`.
- The server store copies the desktop file names and uses the core codecs. It does not depend on `:app:shared`.
- `GET /editions/current` is the only API route. A rejected bearer returns plain text `unauthorized` and does not call the store.
- Sample generation is `LocalEditionRepository.loadCurrentMorningEdition`. `runMorning` is not called.
- The gate button label is Continue. The field label is Token. The web entry no longer calls `App()`.
- Wasm fetch uses the browser `window.fetch` Promise. JS uses the same call. Both send `Authorization: Bearer` and decode with `decodeEdition`.
- `./gradlew :server:test` passed 7 tests. `:app:shared:jvmTest --tests hu.toliver.matutine.edition.WebPaperGateTest` passed 4 tests. `:server:buildFatJar` produced `server/build/libs/server-all.jar`.
- The browser showed the chrome token gate at `http://127.0.0.1:8080/` with one field and Continue, and no newspaper. Compose on Wasm did not accept the automated click, so the mismatch sentence and the sample paper were not exercised in that browser session. Curl against the running jar confirmed 401 for a missing or wrong bearer and a sample edition for `Bearer matutine-demo`, with only `editions/<day>-morning.json` written.
- The first Docker build failed because the Wasm Node binary needs `libatomic.so.1`, which the Temurin JDK image does not ship. The build stage installs `libatomic1` before Gradle. The jar's bytecode is class version 65, so the runtime image is Temurin 21.

## Spec Change Log

## Review Triage Log

## Design Notes

The server implements `EditionStore` itself so the process can use `LocalEditionRepository` without taking a dependency on `:app:shared`. `GET /editions/current` is the one AD-16 route this story needs; the other routes stay absent until their stories. The gate compares nothing locally: only a successful current-edition response opens the paper. The primary button label is Continue. The field label is Token.

## Verification

**Commands:**
- `./gradlew :server:test` -- expected: bearer, sample, and existing-morning tests pass.
- `./gradlew :server:buildFatJar :app:webApp:wasmJsBrowserDistribution` -- expected: fat jar and Wasm distribution build.
- `docker build -t matutine -f server/Dockerfile .` -- expected: image builds when Docker is available.

```
