The review instructions follow in full. Follow them as your review instructions.

# Verification Gap Review

**Goal:** Find changed behavior that could break without reliable verification catching it. Ask one question — "if the behavior this change is supposed to produce broke where it's actually used, would verification fail?" Do not hunt for correctness bugs, but report genuine problems you notice while tracing verification.

The main verification gap shapes are:

1. **Regression gap:** the changed code regresses where it's used, and no test covering that use would fail.
2. **Missing-adoption gap:** a place that should now use the new behavior doesn't; it handles the same case its own way, or not at all, and no test would flag the omission.
3. **Broken-verification gap:** a test appears to cover the changed behavior, but would not actually protect it because it is skipped, flaky, not run in the normal verification path, or too weak to observe the regression.

## Evidence Rules

- Read a test before claiming what it covers, runs, asserts, or misses.
- Before claiming no test exists, search the whole repo by the symbol under test and by import references; expected file locations are not enough.
- Never assert what you did not verify. If a finding cannot be grounded, drop it.
- In a finding, say what you actually checked — "none of the tests I read cover this" — and show how far you looked. Say a test doesn't exist anywhere only when the symbol/import-reference search actually shows that.
- Do not assign severity, confidence, priority, or ranking.

## Review Sequence

### Step 1: Screen for behavioral change

Screen each part of the change separately. If a part is non-behavioral, skip it. Call a part non-behavioral only when the changed code does not alter return values, thrown errors, caller-visible side effects, or observable state (including iteration order and emitted messages). Once a part meets that test, move on; do not inspect callers or tests for extra confirmation.

Common non-behavioral examples: formatting, comments, whitespace; pure renames; trivial getters/setters and pass-throughs; type-only or compiler-enforced changes with no runtime effect; etc.

Only outcomes produced by deterministic code are worth automatically testing; tests are useless on static source text and brittle on LLM output. Skip those parts.

If every part is skipped, output the clean result (see Output Format).

### Step 2: Find the behavior that changed

Identify what behavior changed compared to the previous version: output, side effect, branch, error path, schema/event shape, config default, validation/authorization rule, external contract, etc. If the change affects more than one behavior, handle each separately.

Treat broad-impact changes as behavioral even when no single changed line looks important: dependency, toolchain, build/config, data-file, etc.

### Step 3: Trace where that behavior is used

Trace the changed behavior to the places that observe it. Start with direct callers and registered entry points (routes, commands, DI), contract consumers (schemas, events, APIs, database readers), and reverse-dependency info if already available.

Follow a path only while the changed behavior is reachable and unverified. Stop when a test at that boundary would fail, the consumer does not observe the changed behavior, or the next hop is guesswork (dynamic dispatch, reflection, outside-repo consumers, etc.). Prefer the nearest observable boundary, often one to three hops away, especially across contract, integration, or service edges. If there are more than five similar consumers, group obvious repeats and check representative paths; expand only when a consumer observes the behavior differently.

### Step 4: Qualify the consumer, then check its test

For each consumer, name the smallest realistic regression this consumer would observe: invert the branch, drop the default, omit the field, return the old error code, skip the integration call, etc. This is the Demonstration. If no such regression exists, drop the path; untested downstream code is not a finding.

A `Missing-adoption gap` qualifies not by the adoption failure alone but by a supersession signal: the change gives clear evidence the new behavior is meant to replace the local one — PR intent, naming or docs, a replaced sibling site, deleted duplicate logic, or a test defining the new rule — and the local site shares the same observable contract. Without a supersession signal and a shared observable contract, it is a refactor suggestion, not a verification-gap finding. Once both hold, check whether any test for that site would flag the non-adoption; missing coverage of the non-adoption is the gap itself, not a disqualifier.

Find and read the relevant test. Ask whether the Demonstration would make an assertion fail.

- If yes, the behavior is verified. No finding.
- For a regression-style Demonstration: if no test runs the path, the test is skipped/flaky/not run normally, or the test runs the code without checking the changed result, report a `Regression gap` or `Broken-verification gap`.
- For a qualifying Missing-adoption case: if none of the site tests you found assert it adopts the new behavior, report a `Missing-adoption gap`.

A test counts only if it runs normally and an assertion observes the changed output, branch, or contract. These do not count: no execution; source-text assertions that match a file's wording instead of running it; success/no-throw/snapshot-only checks; mock/log-call checks; human-only checks; tests that mock away the integration; e2e tests that pass through without checking the changed output; stale assertions or fixtures.

For example, `expect(x ?? DEFAULT).toBe(DEFAULT)` passes when `x` is missing.

Common patterns:

- **Caller-path gap** — helper test covers the branch, but caller values skip it.
- **Contract drift** — payload/schema/event changes must be verified at the consumer.
- **Migration compatibility** — tests only create new-format rows or fresh schemas.
- **Phantom exception** — handled partial-failure path has no test.
- **Missing-adoption gap** — sibling site should use the new rule/helper and does not.
- **Removed verification** — deleted test or weakened assertion leaves behavior unpinned; removing a source-text assertion is not this, since it never counted.

### Step 5: Confirm each finding is real

Before writing a finding, re-open the specific tests or search results the finding relies on. Verify the Demonstration would not make any test you checked fail, or that the absence claim is backed by the symbol/import-reference search. Do not claim more than you verified; drop any finding you cannot ground.

Explain why the test misses the bug using what the test sets up and checks.

Do not report: compiler/type-checker-enforced cases; behavior already verified by an integration, contract, or e2e test; implementation-detail or mock-only tests; low coverage or a missing test file by itself; legacy untested code the change did not affect.

Report genuine problems you noticed while tracing verification, even if they are not verification gaps. Put them under `Other findings` in the output. This permits reporting what you already reached, not extra hunting. A claim that code misbehaves is a defect, not a gap — it goes under `Other findings` for standard triage, however you found it.

## OUTPUT FORMAT

Emit each verification-gap finding as one block. No general advice, no severity or confidence. Triage trusts a gap finding as filed and does not re-verify it, so each block must stand on its own evidence.

```markdown
### <one-line title naming the gap>

- **Changed surface:** the exact behavior or contract that changed — `file:line`.
- **Impacted consumer or site:** named concretely with `file:line` (e.g. "the `createInvoice` mutation used by the billing dashboard at `billing/dashboard.ts:88`," not "callers of this function").
- **Existing test evidence:**
  - `Regression gap`: what the relevant test actually asserts, with `file:line`; or, if none, the symbol/import-reference searches run and their result.
  - `Missing-adoption gap`: tests for the impacted site, and whether any assert it adopts the new behavior.
  - `Broken-verification gap`: the apparent test or verification path, and why it does not count.
- **Missing verification:** the precise assertion or check that's absent.
- **Demonstration:**
  - `Regression gap` / `Broken-verification gap`: the concrete regression that would ship undetected, and why the tests you checked would not fail.
  - `Missing-adoption gap`: the case the site mishandles by not adopting the new behavior, and that none of the tests you read assert adoption.
- **Consequence:** the concrete thing that ships wrong — a regression the checked evidence would not catch, or a site that should use the new behavior and doesn't.
- **Disposition:** `patch` — name the test to add, fit to the repo's own way of verifying (don't impose a generic test pyramid) — or `defer` when the gap is real but not worth closing as part of this change, with one sentence of why.
```

If you noticed genuine non-gap problems while tracing verification, append:

```markdown
## Other findings

- <description only; no severity, confidence, priority, or ranking>
```

When you find no verification gaps and no other findings, output exactly this single line, not an empty response:

`No verification gaps found.`

## CONTENT SOURCE

"Review content:" in the message that launched you gives the content itself or a path to read it from. Read the file when it is a path; either way that is the content under review, and this instruction file never is. If no content is supplied, or the file it points to is missing, empty, or unreadable, say exactly that and stop — never report a clean review for content you could not read.


Review content: the unified diff follows in full.

diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
index 33047da..373fc3f 100644
--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
+++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
@@ -29,7 +29,7 @@
 # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
 # - Retrospective appends its action items to action_items; the status view surfaces open ones
 generated: 11-07-2026 19:42
-last_updated: 11-10-2026 19:55
+last_updated: 11-10-2026 21:08
 project: Matutine
 project_key: NOKEY
 tracking_system: file-system
@@ -64,7 +64,7 @@ development_status:
   3-2-generate-the-morning-edition-on-the-server-clock: done
   3-3-configure-the-server-in-admin: done
   3-4-light-edit-a-block-in-the-browser: done
-  3-5-read-the-server-edition-on-android: backlog
+  3-5-read-the-server-edition-on-android: in-progress
   3-6-read-the-server-edition-on-desktop: backlog
   3-7-document-the-server-judge-path: backlog
   3-8-publish-the-docker-image: backlog
diff --git a/app/androidApp/src/main/kotlin/hu/toliver/matutine/EditionNotificationWorker.kt b/app/androidApp/src/main/kotlin/hu/toliver/matutine/EditionNotificationWorker.kt
index 7753385..7ded8fd 100644
--- a/app/androidApp/src/main/kotlin/hu/toliver/matutine/EditionNotificationWorker.kt
+++ b/app/androidApp/src/main/kotlin/hu/toliver/matutine/EditionNotificationWorker.kt
@@ -5,9 +5,12 @@ import androidx.work.CoroutineWorker
 import androidx.work.ListenableWorker
 import androidx.work.WorkerParameters
 import hu.toliver.matutine.edition.EDITION_READY_NOTIFICATION_BODY
+import hu.toliver.matutine.edition.openRemoteTransport
+import hu.toliver.matutine.edition.presentedBearerToken
 import hu.toliver.matutine.edition.resolvedNotificationBody
 import hu.toliver.matutine.edition.todayLocalDate
 import hu.toliver.matutine.edition.toDayKey
+import hu.toliver.matutine.settings.readDeviceServerLink
 import hu.toliver.matutine.store.FileEditionStore
 import hu.toliver.matutine.store.androidDataRoot
 
@@ -17,15 +20,31 @@ class EditionNotificationWorker(
 ) : CoroutineWorker(context, params) {
     override suspend fun doWork(): ListenableWorker.Result {
         val context = applicationContext
-        if (notificationsMayPost(context)) {
-            val store = FileEditionStore(androidDataRoot(context))
-            val dayKey = todayLocalDate().toDayKey()
-            if (store.loadMorningEdition(dayKey) != null) {
-                val body = store.readSettings()?.resolvedNotificationBody() ?: EDITION_READY_NOTIFICATION_BODY
-                postEditionReadyNotification(context, body)
-            }
+        val root = androidDataRoot(context)
+        val store = FileEditionStore(root)
+        val link = readDeviceServerLink(root)
+        val morningReturned = if (link == null) {
+            store.loadMorningEdition(todayLocalDate().toDayKey()) != null
+        } else {
+            val status = runCatching {
+                openRemoteTransport(link.baseUrl).exchange(
+                    method = "GET",
+                    path = "/editions/current",
+                    token = presentedBearerToken(link.token),
+                ).status
+            }.getOrNull()
+            morningEditionWasReturned(status)
+        }
+        if (shouldPostEditionNotification(notificationsMayPost(context), morningReturned)) {
+            val body = store.readSettings()?.resolvedNotificationBody() ?: EDITION_READY_NOTIFICATION_BODY
+            postEditionReadyNotification(context, body)
         }
         scheduleEditionNotification(context)
         return ListenableWorker.Result.success()
     }
 }
+
+internal fun morningEditionWasReturned(httpStatus: Int?): Boolean = httpStatus == 200
+
+internal fun shouldPostEditionNotification(notificationsAllowed: Boolean, morningReturned: Boolean): Boolean =
+    notificationsAllowed && morningReturned
diff --git a/app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt b/app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt
index fe5ccef..f57368e 100644
--- a/app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt
+++ b/app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt
@@ -13,6 +13,7 @@ import hu.toliver.matutine.edition.readDeviceCalendar
 import hu.toliver.matutine.edition.readFeedDocument
 import hu.toliver.matutine.edition.summarizeWithGemini
 import hu.toliver.matutine.edition.writesDeviceCalendarSnapshot
+import hu.toliver.matutine.settings.readDeviceServerLink
 import hu.toliver.matutine.store.FileEditionStore
 import hu.toliver.matutine.store.androidDataRoot
 import java.util.concurrent.TimeUnit
@@ -28,11 +29,16 @@ class MorningEditionWorker(
     params: WorkerParameters,
 ) : CoroutineWorker(context, params) {
     override suspend fun doWork(): ListenableWorker.Result {
-        morningRepository(applicationContext).runMorning()
+        val root = androidDataRoot(applicationContext)
+        if (morningTickInserts(readDeviceServerLink(root) != null)) {
+            morningRepository(applicationContext).runMorning()
+        }
         return ListenableWorker.Result.success()
     }
 }
 
+internal fun morningTickInserts(serverConfigured: Boolean): Boolean = !serverConfigured
+
 internal data class MorningWorkSchedule(
     val uniqueName: String,
     val policy: ExistingPeriodicWorkPolicy,
diff --git a/app/androidApp/src/test/kotlin/hu/toliver/matutine/EditionNotificationScheduleTest.kt b/app/androidApp/src/test/kotlin/hu/toliver/matutine/EditionNotificationScheduleTest.kt
index 4b33ef1..73ec4d9 100644
--- a/app/androidApp/src/test/kotlin/hu/toliver/matutine/EditionNotificationScheduleTest.kt
+++ b/app/androidApp/src/test/kotlin/hu/toliver/matutine/EditionNotificationScheduleTest.kt
@@ -6,6 +6,8 @@ import java.time.ZonedDateTime
 import java.util.concurrent.TimeUnit
 import kotlin.test.Test
 import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertTrue
 
 class EditionNotificationScheduleTest {
     @Test
@@ -34,4 +36,15 @@ class EditionNotificationScheduleTest {
             schedule.request.workSpec.workerClassName,
         )
     }
+
+    @Test
+    fun missingOrUnreachableMorningDoesNotPostANotification() {
+        assertFalse(morningEditionWasReturned(null))
+        assertFalse(morningEditionWasReturned(404))
+        assertFalse(morningEditionWasReturned(401))
+        assertTrue(morningEditionWasReturned(200))
+        assertFalse(shouldPostEditionNotification(notificationsAllowed = true, morningReturned = false))
+        assertFalse(shouldPostEditionNotification(notificationsAllowed = false, morningReturned = true))
+        assertTrue(shouldPostEditionNotification(notificationsAllowed = true, morningReturned = true))
+    }
 }
diff --git a/app/androidApp/src/test/kotlin/hu/toliver/matutine/MorningEditionWorkerTest.kt b/app/androidApp/src/test/kotlin/hu/toliver/matutine/MorningEditionWorkerTest.kt
index 24a04d7..8fbcc3d 100644
--- a/app/androidApp/src/test/kotlin/hu/toliver/matutine/MorningEditionWorkerTest.kt
+++ b/app/androidApp/src/test/kotlin/hu/toliver/matutine/MorningEditionWorkerTest.kt
@@ -18,4 +18,10 @@ class MorningEditionWorkerTest {
             schedule.request.workSpec.workerClassName,
         )
     }
+
+    @Test
+    fun remoteMorningTickDoesNotInsert() {
+        assertEquals(false, morningTickInserts(serverConfigured = true))
+        assertEquals(true, morningTickInserts(serverConfigured = false))
+    }
 }
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
index c2ef741..5e5beca 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
@@ -19,6 +19,11 @@ import hu.toliver.matutine.edition.EditionViewModel
 import hu.toliver.matutine.edition.TypewriterPane
 import hu.toliver.matutine.edition.showsTypewriter
 import hu.toliver.matutine.edition.LocalEditionRepository
+import hu.toliver.matutine.edition.RemoteEditionRepository
+import hu.toliver.matutine.edition.SERVER_ADDRESS_NEEDS_HTTP
+import hu.toliver.matutine.edition.ServerLink
+import hu.toliver.matutine.edition.normalizeServerBaseUrl
+import hu.toliver.matutine.edition.openRemoteTransport
 import hu.toliver.matutine.edition.calendarPermissionIsOff
 import hu.toliver.matutine.edition.openCalendarPermissionSettings
 import hu.toliver.matutine.edition.readCalDavCalendar
@@ -29,8 +34,13 @@ import hu.toliver.matutine.edition.showsNoTopicLine
 import hu.toliver.matutine.edition.writesDeviceCalendarSnapshot
 import hu.toliver.matutine.settings.InterestsScreen
 import hu.toliver.matutine.settings.SettingsScreen
+import hu.toliver.matutine.settings.clearDeviceServerLink
+import hu.toliver.matutine.settings.phoneSettingsVisibility
+import hu.toliver.matutine.settings.readDeviceServerLink
 import hu.toliver.matutine.settings.showsAndroidPaperCustomization
 import hu.toliver.matutine.settings.showsNotificationTimeRow
+import hu.toliver.matutine.settings.showsServerLinkSettings
+import hu.toliver.matutine.settings.writeDeviceServerLink
 import hu.toliver.matutine.store.FileEditionStore
 import hu.toliver.matutine.theme.MatutineTheme
 import hu.toliver.matutine.theme.resolveEditionTheme
@@ -54,9 +64,16 @@ fun MatutineApp(
     windowFlush: WindowFlush = WindowFlush(),
     typewriter: @Composable (TypewriterPane) -> Unit = {},
 ) {
-    val viewModel = remember(dataRoot) {
+    var serverRevision by remember { mutableStateOf(0) }
+    var serverBaseUrl by remember { mutableStateOf("") }
+    var serverToken by remember { mutableStateOf("") }
+    var serverAddressError by remember { mutableStateOf<String?>(null) }
+    val serverLink = remember(dataRoot, serverRevision) {
+        if (showsServerLinkSettings()) readDeviceServerLink(dataRoot) else null
+    }
+    val viewModel = remember(dataRoot, serverRevision) {
         val store = FileEditionStore(dataRoot)
-        val repository = LocalEditionRepository(
+        val local = LocalEditionRepository(
             store,
             readFeed = ::readFeedDocument,
             writesDeviceCalendarSnapshot = writesDeviceCalendarSnapshot(),
@@ -64,8 +81,23 @@ fun MatutineApp(
             readCalDav = ::readCalDavCalendar,
             summarize = ::summarizeWithGemini,
         )
+        val link = if (showsServerLinkSettings()) readDeviceServerLink(dataRoot) else null
+        val repository = if (link == null) {
+            local
+        } else {
+            RemoteEditionRepository(
+                local = local,
+                store = store,
+                link = link,
+                transport = openRemoteTransport(link.baseUrl),
+            )
+        }
         EditionViewModel(repository)
     }
+    LaunchedEffect(serverLink) {
+        serverBaseUrl = serverLink?.baseUrl.orEmpty()
+        serverToken = serverLink?.token.orEmpty()
+    }
     var screen by remember { mutableStateOf<MatutineScreen>(MatutineScreen.Edition) }
     var typewriterDraft by remember { mutableStateOf("") }
     LaunchedEffect(trayOpenRequest) {
@@ -100,6 +132,7 @@ fun MatutineApp(
     val calendarPermissionOff = rememberCalendarPermissionOff()
     val ready = viewModel.state as? EditionUiState.Ready
     val paperSettings = showsAndroidPaperCustomization()
+    val settingsVisibility = phoneSettingsVisibility(serverLink != null)
     MatutineTheme(editionColors = resolveEditionTheme(viewModel.editionTheme)) {
         when (val current = screen) {
             MatutineScreen.Settings -> SettingsScreen(
@@ -139,6 +172,37 @@ fun MatutineApp(
                 onEditionThemeSelect = if (paperSettings) viewModel::saveEditionTheme else null,
                 editionLayoutId = if (paperSettings) viewModel.editionLayout else null,
                 onEditionLayoutSelect = if (paperSettings) viewModel::saveEditionLayout else null,
+                showServerLink = showsServerLinkSettings(),
+                serverBaseUrl = serverBaseUrl,
+                onServerBaseUrlChange = {
+                    serverAddressError = null
+                    serverBaseUrl = it
+                },
+                serverToken = serverToken,
+                onServerTokenChange = { serverToken = it },
+                serverAddressError = serverAddressError,
+                onSaveServer = {
+                    val normalized = normalizeServerBaseUrl(serverBaseUrl)
+                    if (normalized == null) {
+                        serverAddressError = SERVER_ADDRESS_NEEDS_HTTP
+                    } else {
+                        writeDeviceServerLink(dataRoot, ServerLink(baseUrl = normalized, token = serverToken))
+                        serverAddressError = null
+                        serverRevision += 1
+                    }
+                },
+                onForgetServer = if (settingsVisibility.offerForgetServer) {
+                    {
+                        clearDeviceServerLink(dataRoot)
+                        serverAddressError = null
+                        serverRevision += 1
+                    }
+                } else {
+                    null
+                },
+                showSources = settingsVisibility.showSources,
+                showGeminiKey = settingsVisibility.showGeminiKey,
+                showInterests = settingsVisibility.showInterests,
             )
             is MatutineScreen.Interests -> InterestsScreen(
                 topics = viewModel.topics,
@@ -163,7 +227,7 @@ fun MatutineApp(
                 state = viewModel.state,
                 onRetrySample = viewModel::retrySample,
                 onOpenSettings = { screen = MatutineScreen.Settings },
-                showNoTopicLine = ready?.let {
+                showNoTopicLine = serverLink == null && ready?.let {
                     showsNoTopicLine(it.edition.isSample, viewModel.topics)
                 } == true,
                 onOpenInterests = { screen = MatutineScreen.Interests(returnToSettings = false) },
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
index a75ee92..10dbaf3 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
@@ -302,8 +302,12 @@ class EditionViewModel(
                         sameDayExtras = loadExtras(),
                     )
                 }
-                .onFailure {
-                    state = restore(FEED_DIDNT_LOAD)
+                .onFailure { error ->
+                    val shown = when (val message = error.message) {
+                        TOKEN_DOES_NOT_MATCH, SERVER_COPY_ON_DEVICE -> message
+                        else -> FEED_DIDNT_LOAD
+                    }
+                    state = restore(shown)
                 }
         }
     }
@@ -313,9 +317,16 @@ class EditionViewModel(
         scope.launch {
             repository.loadCurrentMorningEdition()
                 .onSuccess { edition ->
-                    state = EditionUiState.Ready(edition, sameDayExtras = loadExtras())
+                    state = EditionUiState.Ready(
+                        edition,
+                        notice = repository.consumeLoadNotice(),
+                        sameDayExtras = loadExtras(),
+                    )
+                }
+                .onFailure { error ->
+                    repository.consumeLoadNotice()
+                    state = mapFailure(error)
                 }
-                .onFailure { error -> state = mapFailure(error) }
         }
     }
 
@@ -500,6 +511,9 @@ class EditionViewModel(
         if (error.message == "sample_load_failed") {
             return EditionUiState.SampleLoadFailed()
         }
+        if (error.message == SERVER_COPY_ON_DEVICE || error.message == TOKEN_DOES_NOT_MATCH) {
+            return EditionUiState.MissingMorning(notice = error.message)
+        }
         if (error.message == MORNING_EDITION_NOT_READY) {
             return EditionUiState.MissingMorning()
         }
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
index 2dbdf09..877842e 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt
@@ -1,7 +1,5 @@
 package hu.toliver.matutine.edition
 
-const val TOKEN_DOES_NOT_MATCH = "That token doesn't match."
-
 data class WebPaperGate(
     val typedToken: String,
     val showEdition: Boolean,
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt
index 75939ae..12c04c8 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt
@@ -73,6 +73,17 @@ fun SettingsScreen(
     onEditionThemeSelect: ((String) -> Unit)? = null,
     editionLayoutId: String? = null,
     onEditionLayoutSelect: ((String) -> Unit)? = null,
+    showServerLink: Boolean = false,
+    serverBaseUrl: String = "",
+    onServerBaseUrlChange: (String) -> Unit = {},
+    serverToken: String = "",
+    onServerTokenChange: (String) -> Unit = {},
+    serverAddressError: String? = null,
+    onSaveServer: () -> Unit = {},
+    onForgetServer: (() -> Unit)? = null,
+    showSources: Boolean = true,
+    showGeminiKey: Boolean = true,
+    showInterests: Boolean = true,
     modifier: Modifier = Modifier,
 ) {
     val typography = LocalMatutineEditionTypography.current
@@ -87,6 +98,70 @@ fun SettingsScreen(
         verticalArrangement = Arrangement.spacedBy(16.dp),
     ) {
         Text(text = "Settings", style = typography.story.copy(color = NewsprintColors.ink))
+        if (showServerLink) {
+            Column(
+                modifier = Modifier
+                    .fillMaxWidth()
+                    .heightIn(min = controlHeight),
+                verticalArrangement = Arrangement.spacedBy(8.dp),
+            ) {
+                Text(text = "Server", style = typography.body.copy(color = NewsprintColors.ink))
+                newsprintField(
+                    value = serverBaseUrl,
+                    onValueChange = onServerBaseUrlChange,
+                    isError = serverAddressError != null,
+                    keyboardType = KeyboardType.Uri,
+                    imeAction = ImeAction.Next,
+                )
+                if (serverAddressError != null) {
+                    Text(
+                        text = serverAddressError,
+                        style = typography.body.copy(color = NewsprintColors.error),
+                    )
+                }
+                Text(text = "Token", style = typography.body.copy(color = NewsprintColors.ink))
+                newsprintField(
+                    value = serverToken,
+                    onValueChange = onServerTokenChange,
+                    isError = false,
+                    keyboardType = KeyboardType.Password,
+                    imeAction = ImeAction.Done,
+                    onDone = onSaveServer,
+                    visualTransformation = PasswordVisualTransformation(),
+                )
+                OutlinedButton(
+                    onClick = onSaveServer,
+                    modifier = Modifier
+                        .fillMaxWidth()
+                        .heightIn(min = controlHeight),
+                    border = BorderStroke(1.dp, NewsprintColors.ink),
+                    colors = ButtonDefaults.outlinedButtonColors(
+                        containerColor = NewsprintColors.paper,
+                        contentColor = NewsprintColors.ink,
+                    ),
+                    shape = RoundedCornerShape(4.dp),
+                ) {
+                    Text(text = "Save server", style = typography.body.copy(color = NewsprintColors.ink))
+                }
+                if (onForgetServer != null) {
+                    OutlinedButton(
+                        onClick = onForgetServer,
+                        modifier = Modifier
+                            .fillMaxWidth()
+                            .heightIn(min = controlHeight),
+                        border = BorderStroke(1.dp, NewsprintColors.ink),
+                        colors = ButtonDefaults.outlinedButtonColors(
+                            containerColor = NewsprintColors.paper,
+                            contentColor = NewsprintColors.ink,
+                        ),
+                        shape = RoundedCornerShape(4.dp),
+                    ) {
+                        Text(text = "Forget server", style = typography.body.copy(color = NewsprintColors.ink))
+                    }
+                }
+            }
+        }
+        if (showSources) {
         Column(
             modifier = Modifier
                 .fillMaxWidth()
@@ -211,6 +286,8 @@ fun SettingsScreen(
                 )
             }
         }
+        }
+        if (showGeminiKey) {
         Column(
             modifier = Modifier
                 .fillMaxWidth()
@@ -228,6 +305,7 @@ fun SettingsScreen(
                 visualTransformation = PasswordVisualTransformation(),
             )
         }
+        }
         if (notificationTime != null && onNotificationTimeChange != null && onSaveNotificationTime != null) {
             Column(
                 modifier = Modifier
@@ -325,14 +403,16 @@ fun SettingsScreen(
                 controlHeight = controlHeight,
             )
         }
-        Row(
-            modifier = Modifier
-                .fillMaxWidth()
-                .heightIn(min = controlHeight)
-                .clickable(role = Role.Button, onClick = onOpenInterests),
-            verticalAlignment = Alignment.CenterVertically,
-        ) {
-            Text(text = "Interests", style = typography.body.copy(color = NewsprintColors.ink))
+        if (showInterests) {
+            Row(
+                modifier = Modifier
+                    .fillMaxWidth()
+                    .heightIn(min = controlHeight)
+                    .clickable(role = Role.Button, onClick = onOpenInterests),
+                verticalAlignment = Alignment.CenterVertically,
+            ) {
+                Text(text = "Interests", style = typography.body.copy(color = NewsprintColors.ink))
+            }
         }
         HorizontalDivider(thickness = 1.dp, color = NewsprintColors.rule)
         Button(
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
index c1897d5..4ca77dc 100644
--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
@@ -453,6 +453,25 @@ class EditionGenerateStateTest {
         assertEquals(1, repository.generateCalls)
     }
 
+    @Test
+    fun unreachableLoadShowsTheStoredEditionAndTheExactSentence() {
+        val repository = FakeEditionRepository(sample, loadNotice = SERVER_COPY_ON_DEVICE)
+        val viewModel = viewModel(repository)
+
+        val ready = viewModel.state as EditionUiState.Ready
+        assertEquals(sample, ready.edition)
+        assertEquals("Couldn't reach the server. Showing the copy on this device.", ready.notice)
+    }
+
+    @Test
+    fun unreachableLoadWithNoStoredEditionKeepsTheSentenceOnTheEmptyPage() {
+        val repository = FakeEditionRepository(sample, loadError = SERVER_COPY_ON_DEVICE)
+        val viewModel = viewModel(repository)
+
+        val empty = viewModel.state as EditionUiState.MissingMorning
+        assertEquals("Couldn't reach the server. Showing the copy on this device.", empty.notice)
+    }
+
     private fun viewModel(repository: EditionRepository) =
         EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
 }
@@ -468,6 +487,7 @@ private class FakeEditionRepository(
     private val failSavedTopics: Boolean = false,
     private val missingMorning: Boolean = false,
     private val loadError: String? = null,
+    private val loadNotice: String? = null,
     var blockSave: CompletableDeferred<Unit>? = null,
     private val generate: suspend () -> Edition = { edition },
 ) : EditionRepository {
@@ -490,6 +510,8 @@ private class FakeEditionRepository(
         else -> Result.success(edition)
     }
 
+    override fun consumeLoadNotice(): String? = loadNotice
+
     override suspend fun retrySample(): Result<Edition> = Result.success(edition)
 
     override fun savedFeedUrls(): List<String> {
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionJson.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionJson.kt
index 260ad52..a70fc95 100644
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionJson.kt
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionJson.kt
@@ -1,5 +1,6 @@
 package hu.toliver.matutine.edition
 
+import kotlinx.serialization.builtins.ListSerializer
 import kotlinx.serialization.json.Json
 
 val editionJson: Json = Json {
@@ -11,3 +12,9 @@ val editionJson: Json = Json {
 fun decodeEdition(json: String): Edition = editionJson.decodeFromString(Edition.serializer(), json)
 
 fun encodeEdition(edition: Edition): String = editionJson.encodeToString(Edition.serializer(), edition)
+
+fun encodeEditionList(editions: List<Edition>): String =
+    editionJson.encodeToString(ListSerializer(Edition.serializer()), editions)
+
+fun decodeEditionList(json: String): List<Edition> =
+    editionJson.decodeFromString(ListSerializer(Edition.serializer()), json)
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
index 3ca117b..865b5e2 100644
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
@@ -462,6 +462,9 @@ interface EditionRepository {
     suspend fun loadSameDayExtras(): List<Edition>
     suspend fun updateMorningEdition(edition: Edition): Result<Edition>
     suspend fun updateExtra(edition: Edition): Result<Edition>
+
+    /** Set by a remote load that had to show the copy already on the device. */
+    fun consumeLoadNotice(): String? = null
 }
 
 internal fun keepMorningEdits(existing: Edition, generated: Edition): Edition {
diff --git a/server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt b/server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt
index c1cf9d5..1837651 100644
--- a/server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt
+++ b/server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt
@@ -13,6 +13,7 @@ import hu.toliver.matutine.edition.decodeServerProcessConfig
 import hu.toliver.matutine.edition.decodeSourceList
 import hu.toliver.matutine.edition.encodeCalDavAccount
 import hu.toliver.matutine.edition.encodeEdition
+import hu.toliver.matutine.edition.encodeEditionList
 import hu.toliver.matutine.edition.encodeReaderSettings
 import hu.toliver.matutine.edition.encodeServerProcessConfig
 import hu.toliver.matutine.edition.encodeSourceList
@@ -137,6 +138,22 @@ internal fun Route.configureMatutineApi(runtime: ServerRuntime) {
         }
         call.respondText(encodeEdition(saved.getOrThrow()), ContentType.Application.Json)
     }
+    get("/editions") {
+        if (!authorize(call, runtime)) return@get
+        call.respondText(encodeEditionList(runtime.listTodayEditions()), ContentType.Application.Json)
+    }
+    post("/editions") {
+        if (!authorize(call, runtime)) return@post
+        val generated = runtime.repository().newEditionNow()
+        if (generated.isFailure) {
+            call.respondText(
+                generated.exceptionOrNull()?.message ?: FEED_DIDNT_LOAD,
+                status = HttpStatusCode.BadRequest,
+            )
+            return@post
+        }
+        call.respondText(encodeEdition(generated.getOrThrow()), ContentType.Application.Json)
+    }
     post("/editions/morning/regenerate") {
         if (!authorize(call, runtime)) return@post
         val generated = runtime.repository().generateToday()
diff --git a/server/src/main/kotlin/hu/toliver/matutine/ServerRuntime.kt b/server/src/main/kotlin/hu/toliver/matutine/ServerRuntime.kt
index 97c3d81..e00305b 100644
--- a/server/src/main/kotlin/hu/toliver/matutine/ServerRuntime.kt
+++ b/server/src/main/kotlin/hu/toliver/matutine/ServerRuntime.kt
@@ -1,9 +1,11 @@
 package hu.toliver.matutine
 
+import hu.toliver.matutine.edition.Edition
 import hu.toliver.matutine.edition.EditionStore
 import hu.toliver.matutine.edition.EditionStory
 import hu.toliver.matutine.edition.LocalEditionRepository
 import hu.toliver.matutine.edition.ServerProcessConfig
+import hu.toliver.matutine.edition.toDayKey
 import io.ktor.server.application.Application
 import io.ktor.server.application.ApplicationStopped
 import io.ktor.util.AttributeKey
@@ -11,6 +13,7 @@ import kotlin.time.Instant
 import kotlinx.coroutines.Job
 import kotlinx.coroutines.launch
 import kotlinx.datetime.TimeZone
+import kotlinx.datetime.toLocalDateTime
 
 internal val ServerRuntimeKey = AttributeKey<ServerRuntime>("matutine.runtime")
 
@@ -43,6 +46,12 @@ internal class ServerRuntime(
 
     fun repository(): LocalEditionRepository = repository
 
+    suspend fun listTodayEditions(): List<Edition> {
+        val dayKey = clock().toLocalDateTime(zone).date.toDayKey()
+        val morning = documents.loadMorningEdition(dayKey)
+        return listOfNotNull(morning) + repository.loadSameDayExtras()
+    }
+
     fun reloadRepository() {
         repository = buildRepository()
     }
diff --git a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
index 01a8a74..d09cd14 100644
--- a/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
+++ b/server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt
@@ -12,6 +12,7 @@ import hu.toliver.matutine.edition.ReaderSettings
 import hu.toliver.matutine.edition.SourceList
 import hu.toliver.matutine.edition.StoryRank
 import hu.toliver.matutine.edition.decodeEdition
+import hu.toliver.matutine.edition.decodeEditionList
 import hu.toliver.matutine.edition.encodeEdition
 import hu.toliver.matutine.edition.decodeReaderSettings
 import hu.toliver.matutine.edition.parseNotificationTime
@@ -444,6 +445,50 @@ class ApplicationTest {
         assertEquals(StoryRank.lead, edition.stories.first().rank)
     }
 
+    @Test
+    fun postEditions_insertsAnExtraAndLeavesTheMorning() = testApplication {
+        val root = Files.createTempDirectory("matutine-extra").toFile()
+        val store = DataDirectoryStore(root.absolutePath)
+        store.writeSourceList(SourceList(feeds = listOf("https://example.com/rss.xml")))
+        application {
+            installTestDocuments("matutine-demo", store)
+            installServerClock(
+                zone = TimeZone.UTC,
+                generationTime = "07:00",
+                clock = { DUE },
+                readFeed = { PERSONAL_RSS },
+            )
+            module()
+        }
+        val morningResponse = client.post("/editions/morning/regenerate") {
+            header("Authorization", "Bearer matutine-demo")
+        }
+        assertEquals(HttpStatusCode.OK, morningResponse.status)
+        val morning = decodeEdition(morningResponse.bodyAsText())
+        val sourcesBefore = root.resolve("sources.json").readText()
+
+        val extraResponse = client.post("/editions") {
+            header("Authorization", "Bearer matutine-demo")
+        }
+        assertEquals(HttpStatusCode.OK, extraResponse.status)
+        val extra = decodeEdition(extraResponse.bodyAsText())
+        assertEquals(EditionRole.extra, extra.role)
+        assertEquals(morning.dayKey, extra.dayKey)
+
+        val current = client.get("/editions/current") {
+            header("Authorization", "Bearer matutine-demo")
+        }
+        assertEquals(morning, decodeEdition(current.bodyAsText()))
+        val listed = decodeEditionList(
+            client.get("/editions") {
+                header("Authorization", "Bearer matutine-demo")
+            }.bodyAsText(),
+        )
+        assertEquals(listOf(morning.id, extra.id), listed.map { it.id })
+        assertEquals(sourcesBefore, root.resolve("sources.json").readText())
+        assertFalse(root.resolve("calendar-snapshot.json").exists())
+    }
+
     @Test
     fun processConfig_roundTripsThroughAdminApi() = testApplication {
         val root = Files.createTempDirectory("matutine-process").toFile()
diff --git a/_bmad-output/implementation-artifacts/spec-3-5-read-the-server-edition-on-android.md b/_bmad-output/implementation-artifacts/spec-3-5-read-the-server-edition-on-android.md
new file mode 100644
index 0000000..26054e6
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-3-5-read-the-server-edition-on-android.md
@@ -0,0 +1,98 @@
+---
+title: 'Read the server edition on Android'
+type: 'feature'
+created: '2026-11-10'
+status: 'in-progress'
+baseline_commit: 'b2628ff179903b75937c57e2a626840b980010c3'
+route: 'dispatch'
+review_loop_iteration: 0
+story_key: '3-5-read-the-server-edition-on-android'
+context:
+  - _bmad-output/implementation-artifacts/epic-3-context.md
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** The phone always reads and mints a local morning paper, so it cannot show the LAN server edition without uploading the papers already on the device.
+
+**Approach:** Saving a server base URL binds an Android remote repository that reads, regenerates, and requests an extra through the server HTTP API. Local editions, sources, and the calendar snapshot stay on the device. An unreachable server shows that local copy and the required sentence. Generate then writes locally. Forget-server binds the local repository again.
+
+## Boundaries & Constraints
+
+**Always:** Persist the base URL and token only in a device-local `server-link.json`. A blank token is `Bearer matutine-demo` on the wire and is stored blank. Accept `http` and `https`; reject anything else with "Use an http or https address." and do not save. `GET /editions/current` shows the server edition with no device/server label. Transport failure, and any status other than 200, 401, or 404, shows the last local morning edition (or the empty front page) and "Couldn't reach the server. Showing the copy on this device." 401 uses "That token doesn't match." and keeps the saved link. 404 stays "today's morning edition is not available yet." Reachable Generate calls `POST /editions/morning/regenerate`. Reachable new-edition calls `POST /editions`, which runs the existing `newEditionNow` and does not merge fields. Transport failure on Generate writes through the local repository. The morning tick does not insert while a link is saved. The notification tick posts the existing notification only when a morning edition is actually returned. While a link is saved, phone Settings hides feed, calendar, CalDAV, interests, and the Gemini key; notification time, notification text, theme, and layout still save on the device. Forget-server deletes the link and shows the unchanged local source fields.
+
+**Never:** Upload local editions, the source list, the calendar snapshot, or settings. Send or show the Gemini key in remote mode. Edit a server edition from Android, including task writes. Mint a morning edition from the remote client or the morning tick. Invent a missing-edition notification. Change `:app:desktopApp` repository selection or desktop Settings. Label the Edition as device or server. Start the emulator.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Save link | `http://192.168.0.5:8080` and a token | Only `server-link.json` changes | Invalid scheme is not saved |
+| Read | Server returns today's edition | That edition is shown and not written into the local store | 401 keeps the link and uses the token sentence |
+| Offline open | Transport throws; a local morning exists | That local edition plus the unreachable sentence | No local morning: empty front page plus the same sentence |
+| Generate | Link saved and server reachable | `POST /editions/morning/regenerate` | Transport failure writes locally and does not POST sources |
+| Extra | Link saved and server reachable | `POST /editions` inserts an extra on the server | Transport failure does not insert a local morning |
+| Forget | Link saved | Source fields return with the pre-link local values | No download of server sources |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- local repository and port. Remote delegates device settings here. `loadCurrentMorningEdition` still mints a sample; the unreachable path must read `EditionStore.loadMorningEdition` instead.
+- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt` -- existing error sentences. Add the unreachable sentence beside them.
+- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/WebPaperGate.kt` -- `TOKEN_DOES_NOT_MATCH`. Reuse it.
+- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt` -- composition root. Keep constructing `LocalEditionRepository` for every platform; wrap it only when `readDeviceServerLink` returns a link. Desktop actuals return null.
+- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` -- optional server fields. Defaults keep desktop sources, key, and interests visible.
+- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt` -- `load` applies `consumeLoadNotice()`. Map the unreachable and token sentences onto `MissingMorning.notice` when no edition is returned. Do not add a source label.
+- `server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt` -- regenerate exists. `POST /editions` and `GET /editions` do not. Add them without calling `loadCurrentMorningEdition` from the list route.
+- `server/src/main/kotlin/hu/toliver/matutine/Application.kt` -- `GET /editions/current` and bearer check. Do not change desktop or web clients.
+- `app/androidApp/src/main/AndroidManifest.xml` -- `usesCleartextTraffic` is already true. Leave it.
+- `app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt` -- skip `runMorning` when a link exists.
+- `app/androidApp/src/main/kotlin/hu/toliver/matutine/EditionNotificationWorker.kt` -- remote tick posts only when a morning edition is returned. No new notification type.
+- `app/desktopApp` -- do not change.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/ServerLink.kt` -- URL normalization, bearer token, link codec.
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteEditionRepository.kt` -- remote port over a transport, local fallback only for Generate and for the stored copy.
+- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/RemoteEditionRepositoryTest.kt` -- matrix rows for save-less transport, read, offline, generate, extra.
+- [x] `server/src/main/kotlin/hu/toliver/matutine/ServerRoutes.kt` -- `POST /editions` and `GET /editions`.
+- [x] `server/src/test/kotlin/hu/toliver/matutine/ApplicationTest.kt` -- extra insert and list do not replace the morning edition.
+- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt` -- surface the two exact sentences.
+- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` and `MatutineApp.kt` -- Android server fields, hidden sources, forget.
+- [x] `app/shared/src/androidMain` -- file link and HTTP transport. Other targets read no link.
+- [x] `app/androidApp` morning and notification workers -- no remote morning insert; no missing-edition notification.
+- [x] `app/shared/src/commonTest` -- view model shows the unreachable sentence on the stored edition and on the empty page.
+
+**Acceptance Criteria:**
+- Given a saved server URL, when the phone loads or generates, then no request writes local editions, sources, or the calendar snapshot.
+- Given the server has today's edition, when the phone loads, then that edition is shown, including a later server edit, and the page has no device/server label.
+- Given the server cannot be reached, when the Edition opens, then the last local edition and "Couldn't reach the server. Showing the copy on this device." are shown, and Generate still writes locally.
+- Given the server is reachable, when O regenerates or asks for a new edition, then those calls run on the server and the phone does not insert a morning edition.
+- Given a server is configured, when Settings is open, then feeds, calendar, and interests are hidden, notification time, notification text, theme, and layout remain, and Forget server brings the source fields back.
+
+## Implementation Notes
+
+- Device link is `server-link.json` beside the local documents. Saving it does not call the server.
+- `POST /editions` calls `newEditionNow`. `GET /editions` lists today's stored morning plus extras and does not mint a sample.
+- Unreachable and non-200/401/404 loads read `EditionStore.loadMorningEdition` and do not write it. Generate is the only local write on a transport failure.
+- Desktop `readDeviceServerLink` returns null, so the shared composition root still binds `LocalEditionRepository` there. `:app:desktopApp` was not edited.
+- Verified with `./gradlew :core:jvmTest :app:shared:jvmTest :server:test :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` and `:app:shared:compileKotlinJs`. No emulator.
+
+## Spec Change Log
+
+## Review Triage Log
+
+## Design Notes
+
+`POST /editions` is the missing HTTP face of `LocalEditionRepository.newEditionNow`. The route returns that edition document and does not merge fields. `GET /editions` returns today's stored morning, if any, plus today's extras, and must not mint a sample. The remote repository never calls `PUT /editions/by-id`, `PUT /sources`, `PUT /settings`, or `PUT /calendar-snapshot`. Android task edits fail and the existing view-model rollback keeps the server copy.
+
+Desktop `readDeviceServerLink` stays null, so `MatutineApp` keeps the local repository there. Cleartext stays on the existing manifest flag.
+
+## Verification
+
+**Commands:**
+- `./gradlew :core:jvmTest :app:shared:jvmTest :server:test :app:androidApp:testDebugUnitTest` -- expected: all tests pass, no emulator.
diff --git a/app/androidApp/src/test/kotlin/hu/toliver/matutine/ServerLinkStoreTest.kt b/app/androidApp/src/test/kotlin/hu/toliver/matutine/ServerLinkStoreTest.kt
new file mode 100644
index 0000000..da7a569
--- /dev/null
+++ b/app/androidApp/src/test/kotlin/hu/toliver/matutine/ServerLinkStoreTest.kt
@@ -0,0 +1,49 @@
+package hu.toliver.matutine
+
+import hu.toliver.matutine.edition.ServerLink
+import hu.toliver.matutine.settings.clearDeviceServerLink
+import hu.toliver.matutine.settings.readDeviceServerLink
+import hu.toliver.matutine.settings.writeDeviceServerLink
+import java.io.File
+import java.nio.file.Files
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertNull
+import kotlin.test.assertTrue
+
+class ServerLinkStoreTest {
+    @Test
+    fun savingAServerLinkDoesNotTouchLocalDocuments() {
+        val root = Files.createTempDirectory("matutine-link").toFile()
+        File(root, "sources.json").writeText("local-sources")
+        File(root, "settings.json").writeText("local-settings")
+        File(root, "calendar-snapshot.json").writeText("local-snapshot")
+        File(root, "editions").mkdirs()
+        File(root, "editions/2026-11-10-morning.json").writeText("local-edition")
+
+        writeDeviceServerLink(root.absolutePath, ServerLink("http://192.168.0.5:8080", "lan-secret"))
+
+        assertEquals("local-sources", File(root, "sources.json").readText())
+        assertEquals("local-settings", File(root, "settings.json").readText())
+        assertEquals("local-snapshot", File(root, "calendar-snapshot.json").readText())
+        assertEquals("local-edition", File(root, "editions/2026-11-10-morning.json").readText())
+        assertEquals(
+            ServerLink("http://192.168.0.5:8080", "lan-secret"),
+            readDeviceServerLink(root.absolutePath),
+        )
+
+        clearDeviceServerLink(root.absolutePath)
+
+        assertNull(readDeviceServerLink(root.absolutePath))
+        assertEquals("local-sources", File(root, "sources.json").readText())
+        assertTrue(File(root, "editions/2026-11-10-morning.json").exists())
+    }
+
+    @Test
+    fun manifestAllowsCleartextHttp() {
+        val manifest = File("src/main/AndroidManifest.xml").readText()
+        assertTrue(manifest.contains("android:usesCleartextTraffic=\"true\""))
+        assertFalse(manifest.contains("networkSecurityConfig"))
+    }
+}
diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.android.kt
new file mode 100644
index 0000000..65c3d7e
--- /dev/null
+++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.android.kt
@@ -0,0 +1,44 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.withContext
+import java.net.HttpURLConnection
+import java.net.URI
+
+actual fun openRemoteTransport(baseUrl: String): RemoteEditionTransport =
+    AndroidRemoteTransport(baseUrl)
+
+private class AndroidRemoteTransport(
+    private val baseUrl: String,
+) : RemoteEditionTransport {
+    override suspend fun exchange(
+        method: String,
+        path: String,
+        token: String,
+        body: String?,
+    ): RemoteHttpResult = withContext(Dispatchers.IO) {
+        val connection = (URI.create(baseUrl.trimEnd('/') + path).toURL().openConnection() as HttpURLConnection).apply {
+            instanceFollowRedirects = false
+            connectTimeout = 15_000
+            readTimeout = 20_000
+            requestMethod = method
+            setRequestProperty("Authorization", "Bearer $token")
+            setRequestProperty("Accept", "application/json")
+            if (body != null) {
+                doOutput = true
+                setRequestProperty("Content-Type", "application/json; charset=utf-8")
+            }
+        }
+        try {
+            if (body != null) {
+                connection.outputStream.use { stream -> stream.write(body.encodeToByteArray()) }
+            }
+            val code = connection.responseCode
+            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
+            val text = stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
+            RemoteHttpResult(code, text)
+        } finally {
+            connection.disconnect()
+        }
+    }
+}
diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.android.kt
new file mode 100644
index 0000000..f04631c
--- /dev/null
+++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.android.kt
@@ -0,0 +1,39 @@
+package hu.toliver.matutine.settings
+
+import hu.toliver.matutine.edition.ServerLink
+import hu.toliver.matutine.edition.decodeServerLink
+import hu.toliver.matutine.edition.encodeServerLink
+import java.io.File
+
+actual fun showsServerLinkSettings(): Boolean = true
+
+actual fun readDeviceServerLink(dataRoot: String): ServerLink? =
+    FileServerLinkStore(dataRoot).read()
+
+actual fun writeDeviceServerLink(dataRoot: String, link: ServerLink) {
+    FileServerLinkStore(dataRoot).write(link)
+}
+
+actual fun clearDeviceServerLink(dataRoot: String) {
+    FileServerLinkStore(dataRoot).clear()
+}
+
+internal class FileServerLinkStore(
+    rootPath: String,
+) {
+    private val file = File(rootPath, "server-link.json")
+
+    fun read(): ServerLink? {
+        if (!file.exists()) return null
+        return runCatching { decodeServerLink(file.readText()) }.getOrNull()
+    }
+
+    fun write(link: ServerLink) {
+        file.parentFile?.mkdirs()
+        file.writeText(encodeServerLink(link))
+    }
+
+    fun clear() {
+        if (file.exists()) file.delete()
+    }
+}
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.kt
new file mode 100644
index 0000000..5918f62
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.kt
@@ -0,0 +1,3 @@
+package hu.toliver.matutine.edition
+
+expect fun openRemoteTransport(baseUrl: String): RemoteEditionTransport
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.kt
new file mode 100644
index 0000000..10d935c
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.kt
@@ -0,0 +1,25 @@
+package hu.toliver.matutine.settings
+
+import hu.toliver.matutine.edition.ServerLink
+
+data class PhoneSettingsVisibility(
+    val showSources: Boolean,
+    val showGeminiKey: Boolean,
+    val showInterests: Boolean,
+    val offerForgetServer: Boolean,
+)
+
+fun phoneSettingsVisibility(serverConfigured: Boolean) = PhoneSettingsVisibility(
+    showSources = !serverConfigured,
+    showGeminiKey = !serverConfigured,
+    showInterests = !serverConfigured,
+    offerForgetServer = serverConfigured,
+)
+
+expect fun showsServerLinkSettings(): Boolean
+
+expect fun readDeviceServerLink(dataRoot: String): ServerLink?
+
+expect fun writeDeviceServerLink(dataRoot: String, link: ServerLink)
+
+expect fun clearDeviceServerLink(dataRoot: String)
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/settings/PhoneSettingsVisibilityTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/settings/PhoneSettingsVisibilityTest.kt
new file mode 100644
index 0000000..feae733
--- /dev/null
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/settings/PhoneSettingsVisibilityTest.kt
@@ -0,0 +1,22 @@
+package hu.toliver.matutine.settings
+
+import kotlin.test.Test
+import kotlin.test.assertFalse
+import kotlin.test.assertTrue
+
+class PhoneSettingsVisibilityTest {
+    @Test
+    fun configuredServerHidesSourcesAndForgetBringsThemBack() {
+        val remote = phoneSettingsVisibility(serverConfigured = true)
+        assertFalse(remote.showSources)
+        assertFalse(remote.showGeminiKey)
+        assertFalse(remote.showInterests)
+        assertTrue(remote.offerForgetServer)
+
+        val local = phoneSettingsVisibility(serverConfigured = false)
+        assertTrue(local.showSources)
+        assertTrue(local.showGeminiKey)
+        assertTrue(local.showInterests)
+        assertFalse(local.offerForgetServer)
+    }
+}
diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.js.kt
new file mode 100644
index 0000000..e2d6475
--- /dev/null
+++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.js.kt
@@ -0,0 +1,5 @@
+package hu.toliver.matutine.edition
+
+actual fun openRemoteTransport(baseUrl: String): RemoteEditionTransport {
+    error("The browser does not open a device remote repository: $baseUrl")
+}
diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.js.kt
new file mode 100644
index 0000000..46ad7c5
--- /dev/null
+++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.js.kt
@@ -0,0 +1,15 @@
+package hu.toliver.matutine.settings
+
+import hu.toliver.matutine.edition.ServerLink
+
+actual fun showsServerLinkSettings(): Boolean = false
+
+actual fun readDeviceServerLink(dataRoot: String): ServerLink? = dataRoot.let { null }
+
+actual fun writeDeviceServerLink(dataRoot: String, link: ServerLink) {
+    if (dataRoot.isEmpty() && link.baseUrl.isEmpty()) return
+}
+
+actual fun clearDeviceServerLink(dataRoot: String) {
+    if (dataRoot.isEmpty()) return
+}
diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.jvm.kt
new file mode 100644
index 0000000..7aa0c4c
--- /dev/null
+++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.jvm.kt
@@ -0,0 +1,5 @@
+package hu.toliver.matutine.edition
+
+actual fun openRemoteTransport(baseUrl: String): RemoteEditionTransport {
+    error("Desktop does not open a remote repository: $baseUrl")
+}
diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.jvm.kt
new file mode 100644
index 0000000..096c16b
--- /dev/null
+++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.jvm.kt
@@ -0,0 +1,14 @@
+package hu.toliver.matutine.settings
+
+import hu.toliver.matutine.edition.ServerLink
+
+actual fun showsServerLinkSettings(): Boolean = false
+
+@Suppress("UNUSED_PARAMETER")
+actual fun readDeviceServerLink(dataRoot: String): ServerLink? = null
+
+@Suppress("UNUSED_PARAMETER")
+actual fun writeDeviceServerLink(dataRoot: String, link: ServerLink) = Unit
+
+@Suppress("UNUSED_PARAMETER")
+actual fun clearDeviceServerLink(dataRoot: String) = Unit
diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.wasmJs.kt
new file mode 100644
index 0000000..e2d6475
--- /dev/null
+++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/RemoteTransport.wasmJs.kt
@@ -0,0 +1,5 @@
+package hu.toliver.matutine.edition
+
+actual fun openRemoteTransport(baseUrl: String): RemoteEditionTransport {
+    error("The browser does not open a device remote repository: $baseUrl")
+}
diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.wasmJs.kt
new file mode 100644
index 0000000..46ad7c5
--- /dev/null
+++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/settings/ServerLinkSettings.wasmJs.kt
@@ -0,0 +1,15 @@
+package hu.toliver.matutine.settings
+
+import hu.toliver.matutine.edition.ServerLink
+
+actual fun showsServerLinkSettings(): Boolean = false
+
+actual fun readDeviceServerLink(dataRoot: String): ServerLink? = dataRoot.let { null }
+
+actual fun writeDeviceServerLink(dataRoot: String, link: ServerLink) {
+    if (dataRoot.isEmpty() && link.baseUrl.isEmpty()) return
+}
+
+actual fun clearDeviceServerLink(dataRoot: String) {
+    if (dataRoot.isEmpty()) return
+}
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteEditionRepository.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteEditionRepository.kt
new file mode 100644
index 0000000..0932f69
--- /dev/null
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/RemoteEditionRepository.kt
@@ -0,0 +1,159 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.datetime.LocalDate
+
+data class RemoteHttpResult(
+    val status: Int,
+    val body: String,
+)
+
+interface RemoteEditionTransport {
+    suspend fun exchange(method: String, path: String, token: String, body: String? = null): RemoteHttpResult
+}
+
+/**
+ * Reads and asks the server to regenerate or insert an extra.
+ * Device settings stay on [local]. A failed connection shows the stored morning edition
+ * and does not copy the server paper into the device store.
+ */
+class RemoteEditionRepository(
+    private val local: EditionRepository,
+    private val store: EditionStore,
+    private val link: ServerLink,
+    private val transport: RemoteEditionTransport,
+    private val today: () -> LocalDate = { todayLocalDate() },
+) : EditionRepository {
+    private var pendingNotice: String? = null
+    private var showingStoredCopy = false
+
+    override fun consumeLoadNotice(): String? {
+        val notice = pendingNotice
+        pendingNotice = null
+        return notice
+    }
+
+    override suspend fun loadCurrentMorningEdition(): Result<Edition> {
+        val response = call("GET", "/editions/current")
+        if (response.isFailure) return storedCopy(SERVER_COPY_ON_DEVICE)
+        val http = response.getOrThrow()
+        return when (http.status) {
+            200 -> {
+                showingStoredCopy = false
+                pendingNotice = null
+                Result.success(decodeEdition(http.body))
+            }
+            401 -> storedCopy(TOKEN_DOES_NOT_MATCH)
+            404 -> {
+                showingStoredCopy = false
+                pendingNotice = null
+                Result.failure(IllegalStateException(MORNING_EDITION_NOT_READY))
+            }
+            else -> storedCopy(SERVER_COPY_ON_DEVICE)
+        }
+    }
+
+    override suspend fun retrySample(): Result<Edition> =
+        Result.failure(IllegalStateException(MORNING_EDITION_NOT_READY))
+
+    override fun savedFeedUrls(): List<String> = local.savedFeedUrls()
+
+    override suspend fun saveFeedUrl(url: String): Result<Unit> = local.saveFeedUrl(url)
+
+    override fun savedCalendarUrls(): List<String> = local.savedCalendarUrls()
+
+    override suspend fun saveCalendarUrl(url: String): Result<Unit> = local.saveCalendarUrl(url)
+
+    override fun savedCalDavAccount(): CalDavAccount? = local.savedCalDavAccount()
+
+    override suspend fun saveCalDavAccount(url: String, username: String, password: String): Result<Unit> =
+        local.saveCalDavAccount(url, username, password)
+
+    override fun savedTopics(): List<String> = local.savedTopics()
+
+    override fun saveTopics(topics: List<String>) {
+        local.saveTopics(topics)
+    }
+
+    override fun savedNotificationTime(): String = local.savedNotificationTime()
+
+    override fun saveNotificationTime(time: String): Result<Unit> = local.saveNotificationTime(time)
+
+    override fun savedNotificationBody(): String = local.savedNotificationBody()
+
+    override fun saveNotificationBody(body: String): Result<Unit> = local.saveNotificationBody(body)
+
+    override fun savedEditionTheme(): String = local.savedEditionTheme()
+
+    override fun saveEditionTheme(themeId: String): Result<Unit> = local.saveEditionTheme(themeId)
+
+    override fun savedEditionLayout(): String = local.savedEditionLayout()
+
+    override fun saveEditionLayout(layoutId: String): Result<Unit> = local.saveEditionLayout(layoutId)
+
+    override fun savedGeminiKey(): String = ""
+
+    override fun saveGeminiKey(key: String): Result<Unit> = Result.success(Unit)
+
+    override suspend fun generateToday(): Result<Edition> {
+        val response = call("POST", "/editions/morning/regenerate")
+        if (response.isFailure) return local.generateToday()
+        val http = response.getOrThrow()
+        return when (http.status) {
+            200 -> {
+                showingStoredCopy = false
+                Result.success(decodeEdition(http.body))
+            }
+            401 -> Result.failure(IllegalStateException(TOKEN_DOES_NOT_MATCH))
+            else -> Result.failure(IllegalStateException(http.body.ifBlank { FEED_DIDNT_LOAD }))
+        }
+    }
+
+    override suspend fun newEditionNow(): Result<Edition> {
+        val response = call("POST", "/editions")
+        if (response.isFailure) return Result.failure(IllegalStateException(SERVER_COPY_ON_DEVICE))
+        val http = response.getOrThrow()
+        return when (http.status) {
+            200 -> Result.success(decodeEdition(http.body))
+            401 -> Result.failure(IllegalStateException(TOKEN_DOES_NOT_MATCH))
+            else -> Result.failure(IllegalStateException(http.body.ifBlank { FEED_DIDNT_LOAD }))
+        }
+    }
+
+    override suspend fun loadSameDayExtras(): List<Edition> {
+        if (showingStoredCopy) return local.loadSameDayExtras()
+        val response = call("GET", "/editions")
+        val http = response.getOrNull() ?: return emptyList()
+        if (http.status != 200) return emptyList()
+        return decodeEditionList(http.body).filter { edition ->
+            edition.role == EditionRole.extra && edition.dayKey == today().toDayKey()
+        }
+    }
+
+    override suspend fun updateMorningEdition(edition: Edition): Result<Edition> = refusedEdit()
+
+    override suspend fun updateExtra(edition: Edition): Result<Edition> = refusedEdit()
+
+    private fun refusedEdit(): Result<Edition> =
+        Result.failure(IllegalStateException("Android does not edit a server edition"))
+
+    private fun storedCopy(message: String): Result<Edition> {
+        pendingNotice = message
+        val stored = store.loadMorningEdition(today().toDayKey())
+        showingStoredCopy = stored != null
+        return if (stored != null) {
+            Result.success(stored)
+        } else {
+            Result.failure(IllegalStateException(message))
+        }
+    }
+
+    private suspend fun call(method: String, path: String, body: String? = null): Result<RemoteHttpResult> =
+        try {
+            Result.success(
+                transport.exchange(method, path, presentedBearerToken(link.token), body),
+            )
+        } catch (error: Throwable) {
+            if (error is kotlin.coroutines.cancellation.CancellationException) throw error
+            Result.failure(error)
+        }
+}
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ServerLink.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ServerLink.kt
new file mode 100644
index 0000000..526cfc9
--- /dev/null
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ServerLink.kt
@@ -0,0 +1,34 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.serialization.Serializable
+
+const val TOKEN_DOES_NOT_MATCH = "That token doesn't match."
+const val SERVER_COPY_ON_DEVICE = "Couldn't reach the server. Showing the copy on this device."
+const val SERVER_ADDRESS_NEEDS_HTTP = "Use an http or https address."
+const val UNSET_BEARER_TOKEN = "matutine-demo"
+
+@Serializable
+data class ServerLink(
+    val baseUrl: String = "",
+    val token: String = "",
+)
+
+fun presentedBearerToken(raw: String): String = raw.trim().ifEmpty { UNSET_BEARER_TOKEN }
+
+fun normalizeServerBaseUrl(raw: String): String? {
+    val trimmed = raw.trim().trimEnd('/')
+    val scheme = when {
+        trimmed.startsWith("https://") -> "https://"
+        trimmed.startsWith("http://") -> "http://"
+        else -> return null
+    }
+    val rest = trimmed.removePrefix(scheme)
+    if (rest.isEmpty() || rest.any { it.isWhitespace() }) return null
+    return trimmed
+}
+
+fun encodeServerLink(link: ServerLink): String =
+    editionJson.encodeToString(ServerLink.serializer(), link)
+
+fun decodeServerLink(json: String): ServerLink =
+    editionJson.decodeFromString(ServerLink.serializer(), json)
diff --git a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/RemoteEditionRepositoryTest.kt b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/RemoteEditionRepositoryTest.kt
new file mode 100644
index 0000000..9f61d96
--- /dev/null
+++ b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/RemoteEditionRepositoryTest.kt
@@ -0,0 +1,298 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.coroutines.runBlocking
+import kotlinx.datetime.LocalDate
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertNull
+import kotlin.test.assertTrue
+
+class RemoteEditionRepositoryTest {
+    private val day = LocalDate(2026, 11, 10)
+    private val serverEdition = SampleGenerator.forDay(day).copy(
+        id = "morning-2026-11-10",
+        revision = 4,
+        isSample = false,
+        briefing = "Extractive briefing.",
+    )
+    private val extra = serverEdition.copy(
+        id = "extra-2026-11-10-1",
+        revision = 1,
+        role = EditionRole.extra,
+        briefing = "An extra from the server.",
+    )
+
+    @Test
+    fun httpLanUrlIsAcceptedAndOtherSchemesAreNot() {
+        assertEquals("http://192.168.0.5:8080", normalizeServerBaseUrl("  http://192.168.0.5:8080/ "))
+        assertEquals("https://paper.local", normalizeServerBaseUrl("https://paper.local"))
+        assertNull(normalizeServerBaseUrl("ftp://192.168.0.5"))
+        assertNull(normalizeServerBaseUrl("192.168.0.5:8080"))
+        assertNull(normalizeServerBaseUrl(""))
+        assertEquals("matutine-demo", presentedBearerToken("  "))
+        assertEquals("lan-secret", presentedBearerToken(" lan-secret "))
+    }
+
+    @Test
+    fun readShowsTheServerEditionAndDoesNotWriteTheLocalStore() = runBlocking {
+        val store = MemoryStore()
+        val local = LocalFake()
+        val transport = ScriptedTransport { method, path ->
+            assertEquals("GET", method)
+            assertEquals("/editions/current", path)
+            RemoteHttpResult(200, encodeEdition(serverEdition))
+        }
+        val repo = repository(store, local, transport, token = "")
+
+        val loaded = repo.loadCurrentMorningEdition().getOrThrow()
+
+        assertEquals(serverEdition, loaded)
+        assertNull(repo.consumeLoadNotice())
+        assertEquals(0, store.morningWrites)
+        assertEquals(0, store.sourceWrites)
+        assertEquals(0, store.snapshotWrites)
+        assertEquals(listOf("GET /editions/current matutine-demo"), transport.calls)
+        assertFalse(loaded.sourceNotes.any { it.contains("server") || it.contains("device") })
+    }
+
+    @Test
+    fun wrongTokenKeepsTheStoredEditionAndTheTokenSentence() = runBlocking {
+        val stored = serverEdition.copy(briefing = "Already on the phone.")
+        val store = MemoryStore(morning = stored)
+        val transport = ScriptedTransport { _, _ -> RemoteHttpResult(401, "unauthorized") }
+        val repo = repository(store, LocalFake(), transport, token = "typed")
+
+        val loaded = repo.loadCurrentMorningEdition().getOrThrow()
+
+        assertEquals(stored, loaded)
+        assertEquals(TOKEN_DOES_NOT_MATCH, repo.consumeLoadNotice())
+        assertEquals(0, store.morningWrites)
+        assertEquals(listOf("GET /editions/current typed"), transport.calls)
+    }
+
+    @Test
+    fun offlineOpenShowsTheStoredEditionAndDoesNotUpload() = runBlocking {
+        val stored = serverEdition.copy(briefing = "Copy on the phone.")
+        val store = MemoryStore(morning = stored)
+        val local = LocalFake()
+        val transport = ScriptedTransport { _, _ -> error("offline") }
+        val repo = repository(store, local, transport)
+
+        val loaded = repo.loadCurrentMorningEdition().getOrThrow()
+
+        assertEquals(stored, loaded)
+        assertEquals(SERVER_COPY_ON_DEVICE, repo.consumeLoadNotice())
+        assertEquals(0, store.morningWrites)
+        assertEquals(0, store.sourceWrites)
+        assertEquals(0, store.snapshotWrites)
+        assertEquals(0, local.generateCalls)
+        assertEquals(emptyList(), transport.calls.filter { it.startsWith("PUT") || it.startsWith("POST") })
+    }
+
+    @Test
+    fun offlineOpenWithNoStoredEditionFailsWithTheSameSentence() = runBlocking {
+        val transport = ScriptedTransport { _, _ -> error("offline") }
+        val repo = repository(MemoryStore(), LocalFake(), transport)
+
+        val loaded = repo.loadCurrentMorningEdition()
+
+        assertEquals(SERVER_COPY_ON_DEVICE, loaded.exceptionOrNull()?.message)
+        assertEquals(SERVER_COPY_ON_DEVICE, repo.consumeLoadNotice())
+    }
+
+    @Test
+    fun reachableGenerateAndExtraRunOnTheServer() = runBlocking {
+        val store = MemoryStore()
+        val local = LocalFake()
+        val transport = ScriptedTransport { method, path ->
+            when (path) {
+                "/editions/morning/regenerate" -> RemoteHttpResult(200, encodeEdition(serverEdition))
+                "/editions" -> if (method == "POST") {
+                    RemoteHttpResult(200, encodeEdition(extra))
+                } else {
+                    RemoteHttpResult(200, encodeEditionList(listOf(serverEdition, extra)))
+                }
+                else -> error(path)
+            }
+        }
+        val repo = repository(store, local, transport)
+
+        val regenerated = repo.generateToday().getOrThrow()
+        val created = repo.newEditionNow().getOrThrow()
+        val extras = repo.loadSameDayExtras()
+
+        assertEquals(serverEdition.id, regenerated.id)
+        assertEquals(EditionRole.extra, created.role)
+        assertEquals(listOf(extra.id), extras.map { it.id })
+        assertEquals(0, local.generateCalls)
+        assertEquals(0, local.extraCalls)
+        assertEquals(0, store.morningWrites)
+        assertEquals(0, store.sourceWrites)
+        assertEquals(0, store.snapshotWrites)
+        assertEquals(
+            listOf(
+                "POST /editions/morning/regenerate secret",
+                "POST /editions secret",
+                "GET /editions secret",
+            ),
+            transport.calls,
+        )
+    }
+
+    @Test
+    fun unreachableGenerateWritesLocallyAndDoesNotPostSources() = runBlocking {
+        val local = LocalFake()
+        val transport = ScriptedTransport { _, _ -> error("offline") }
+        val repo = repository(MemoryStore(), local, transport)
+
+        val generated = repo.generateToday().getOrThrow()
+        val extraResult = repo.newEditionNow()
+
+        assertEquals("generated-local", generated.briefing)
+        assertEquals(1, local.generateCalls)
+        assertTrue(extraResult.isFailure)
+        assertEquals(0, local.extraCalls)
+        assertEquals(0, local.feedWrites)
+        assertTrue(transport.calls.all { it.startsWith("POST /editions") })
+        assertTrue(transport.calls.none { it.contains("/sources") || it.contains("calendar-snapshot") })
+    }
+
+    @Test
+    fun remoteSettingsStayOnTheDeviceAndTheKeyIsNotSent() = runBlocking {
+        val local = LocalFake()
+        val transport = ScriptedTransport { _, _ -> error("unused") }
+        val repo = repository(MemoryStore(), local, transport)
+
+        assertEquals(Result.success(Unit), repo.saveNotificationTime("08:15"))
+        assertEquals(Result.success(Unit), repo.saveNotificationBody("Ready when you are."))
+        assertEquals(Result.success(Unit), repo.saveEditionTheme("newsprint"))
+        assertEquals(Result.success(Unit), repo.saveEditionLayout("newsprint"))
+        repo.saveTopics(listOf("Markets"))
+        val savedFeed = repo.saveFeedUrl("https://example.com/rss.xml")
+        assertEquals("", repo.savedGeminiKey())
+        assertEquals(Result.success(Unit), repo.saveGeminiKey("should-not-land"))
+        assertEquals(0, local.geminiWrites)
+        assertEquals(1, local.notificationTimeWrites)
+        assertEquals(1, local.topicWrites)
+        assertEquals(1, local.feedWrites)
+        assertEquals(Result.success(Unit), savedFeed)
+        assertEquals(emptyList(), transport.calls)
+        assertTrue(repo.updateMorningEdition(serverEdition).isFailure)
+        assertEquals(emptyList(), transport.calls)
+    }
+
+    private fun repository(
+        store: MemoryStore,
+        local: LocalFake,
+        transport: ScriptedTransport,
+        token: String = "secret",
+    ) = RemoteEditionRepository(
+        local = local,
+        store = store,
+        link = ServerLink(baseUrl = "http://192.168.0.5:8080", token = token),
+        transport = transport,
+        today = { day },
+    )
+}
+
+private class ScriptedTransport(
+    private val handler: (method: String, path: String) -> RemoteHttpResult,
+) : RemoteEditionTransport {
+    val calls = mutableListOf<String>()
+
+    override suspend fun exchange(method: String, path: String, token: String, body: String?): RemoteHttpResult {
+        calls += "$method $path $token"
+        return handler(method, path)
+    }
+}
+
+private class MemoryStore(
+    private val morning: Edition? = null,
+) : EditionStore {
+    var morningWrites = 0
+    var sourceWrites = 0
+    var snapshotWrites = 0
+
+    override fun readSourceList(): SourceList? = null
+    override fun writeSourceList(sourceList: SourceList) {
+        sourceWrites += 1
+    }
+    override fun readSettings(): ReaderSettings? = null
+    override fun writeSettings(settings: ReaderSettings) = Unit
+    override fun loadMorningEdition(dayKey: String): Edition? = morning?.takeIf { it.dayKey == dayKey }
+    override fun saveMorningEdition(edition: Edition) {
+        morningWrites += 1
+    }
+    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
+    override fun saveExtra(edition: Edition) = Unit
+    override fun readCalendarSnapshot(): CalendarSnapshot? = null
+    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
+        snapshotWrites += 1
+    }
+    override fun readCalDavAccount(): CalDavAccount? = null
+    override fun writeCalDavAccount(account: CalDavAccount) = Unit
+    override fun readProcessConfig(): ServerProcessConfig? = null
+    override fun writeProcessConfig(config: ServerProcessConfig) = Unit
+}
+
+private class LocalFake : EditionRepository {
+    var generateCalls = 0
+    var extraCalls = 0
+    var feedWrites = 0
+    var notificationTimeWrites = 0
+    var topicWrites = 0
+    var geminiWrites = 0
+
+    override suspend fun loadCurrentMorningEdition(): Result<Edition> =
+        Result.failure(IllegalStateException("local load is not used"))
+
+    override suspend fun retrySample(): Result<Edition> =
+        Result.failure(IllegalStateException("no sample"))
+
+    override fun savedFeedUrls(): List<String> = emptyList()
+    override suspend fun saveFeedUrl(url: String): Result<Unit> {
+        feedWrites += 1
+        return Result.success(Unit)
+    }
+    override fun savedCalendarUrls(): List<String> = emptyList()
+    override suspend fun saveCalendarUrl(url: String): Result<Unit> = Result.success(Unit)
+    override fun savedCalDavAccount(): CalDavAccount? = null
+    override suspend fun saveCalDavAccount(url: String, username: String, password: String): Result<Unit> =
+        Result.success(Unit)
+    override fun savedTopics(): List<String> = emptyList()
+    override fun saveTopics(topics: List<String>) {
+        topicWrites += 1
+    }
+    override fun savedNotificationTime(): String = DEFAULT_NOTIFICATION_TIME
+    override fun saveNotificationTime(time: String): Result<Unit> {
+        notificationTimeWrites += 1
+        return Result.success(Unit)
+    }
+    override fun savedNotificationBody(): String = EDITION_READY_NOTIFICATION_BODY
+    override fun saveNotificationBody(body: String): Result<Unit> = Result.success(Unit)
+    override fun savedEditionTheme(): String = DEFAULT_EDITION_THEME
+    override fun saveEditionTheme(themeId: String): Result<Unit> = Result.success(Unit)
+    override fun savedEditionLayout(): String = DEFAULT_EDITION_LAYOUT
+    override fun saveEditionLayout(layoutId: String): Result<Unit> = Result.success(Unit)
+    override fun savedGeminiKey(): String = "local-key"
+    override fun saveGeminiKey(key: String): Result<Unit> {
+        geminiWrites += 1
+        return Result.success(Unit)
+    }
+    override suspend fun generateToday(): Result<Edition> {
+        generateCalls += 1
+        return Result.success(
+            SampleGenerator.forDay(LocalDate(2026, 11, 10)).copy(briefing = "generated-local", isSample = false),
+        )
+    }
+    override suspend fun newEditionNow(): Result<Edition> {
+        extraCalls += 1
+        return Result.failure(IllegalStateException("local extra"))
+    }
+    override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
+    override suspend fun updateMorningEdition(edition: Edition): Result<Edition> =
+        Result.failure(IllegalStateException("local edit"))
+    override suspend fun updateExtra(edition: Edition): Result<Edition> =
+        Result.failure(IllegalStateException("local edit"))
+}

Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. If the instruction file is unreadable, report that exact failure and stop. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
