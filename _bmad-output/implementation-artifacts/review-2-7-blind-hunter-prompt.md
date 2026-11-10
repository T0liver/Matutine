Conduct a review of CONTENT.
Look for what's missing, not only what's wrong.
Compute your finding floor N from the diff file's size: N = min(floor(sqrt(kB) + 1), 10), where kB is the file's size in kilobytes. State the arithmetic in one line, then find at least N issues to fix or improve.
Output a Markdown list of findings only — no severity, priority, or ranking.
If the content is empty, stop and say so.
If you have zero findings, re-check and keep thinking; do not stop with an empty list.

CONTENT: the unified diff at `diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
index ceed8f0..8a87e8f 100644
--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
+++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
@@ -29,7 +29,7 @@
 # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
 # - Retrospective appends its action items to action_items; the status view surfaces open ones
 generated: 11-07-2026 19:42
-last_updated: 11-10-2026 08:37
+last_updated: 11-10-2026 18:40
 project: Matutine
 project_key: NOKEY
 tracking_system: file-system
@@ -55,8 +55,8 @@ development_status:
   2-3-customize-the-android-paper: done
   2-4-use-the-desktop-tray: done
   2-5-rewrite-the-edition-on-the-typewriter: done
-  2-6-regenerate-without-losing-the-edits: backlog
-  2-7-write-the-briefing-with-a-gemini-key: backlog
+  2-6-regenerate-without-losing-the-edits: in-progress
+  2-7-write-the-briefing-with-a-gemini-key: in-progress
   epic-2-retrospective: optional
 
   epic-3: backlog
diff --git a/app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt b/app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt
index 18d2027..fe5ccef 100644
--- a/app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt
+++ b/app/androidApp/src/main/kotlin/hu/toliver/matutine/MorningEditionWorker.kt
@@ -11,6 +11,7 @@ import hu.toliver.matutine.edition.LocalEditionRepository
 import hu.toliver.matutine.edition.readCalDavCalendar
 import hu.toliver.matutine.edition.readDeviceCalendar
 import hu.toliver.matutine.edition.readFeedDocument
+import hu.toliver.matutine.edition.summarizeWithGemini
 import hu.toliver.matutine.edition.writesDeviceCalendarSnapshot
 import hu.toliver.matutine.store.FileEditionStore
 import hu.toliver.matutine.store.androidDataRoot
@@ -65,4 +66,5 @@ private fun morningRepository(context: Context) = LocalEditionRepository(
     writesDeviceCalendarSnapshot = writesDeviceCalendarSnapshot(),
     readDeviceCalendar = { day -> readDeviceCalendar(context, day) },
     readCalDav = ::readCalDavCalendar,
+    summarize = ::summarizeWithGemini,
 )
diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
index 3f7b6cd..2fcca9e 100644
--- a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
+++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
@@ -62,6 +62,25 @@ actual class FileEditionStore actual constructor(
         file.writeText(encodeEdition(edition))
     }
 
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
+        val dir = File(root, "editions")
+        dir.mkdirs()
+        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
+    }
+
     override fun readCalendarSnapshot(): CalendarSnapshot? {
         val file = File(root, "calendar-snapshot.json")
         if (!file.exists()) return null
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
index 7c6b5dc..c2ef741 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
@@ -24,6 +24,7 @@ import hu.toliver.matutine.edition.openCalendarPermissionSettings
 import hu.toliver.matutine.edition.readCalDavCalendar
 import hu.toliver.matutine.edition.readDeviceCalendar
 import hu.toliver.matutine.edition.readFeedDocument
+import hu.toliver.matutine.edition.summarizeWithGemini
 import hu.toliver.matutine.edition.showsNoTopicLine
 import hu.toliver.matutine.edition.writesDeviceCalendarSnapshot
 import hu.toliver.matutine.settings.InterestsScreen
@@ -61,6 +62,7 @@ fun MatutineApp(
             writesDeviceCalendarSnapshot = writesDeviceCalendarSnapshot(),
             readDeviceCalendar = ::readDeviceCalendar,
             readCalDav = ::readCalDavCalendar,
+            summarize = ::summarizeWithGemini,
         )
         EditionViewModel(repository)
     }
@@ -117,6 +119,9 @@ fun MatutineApp(
                 onCalDavPasswordChange = viewModel::updateCalDavPassword,
                 calDavError = viewModel.calDavFieldError,
                 onSaveCalDav = viewModel::saveCalDav,
+                geminiKey = viewModel.geminiKey,
+                onGeminiKeyChange = viewModel::updateGeminiKey,
+                onSaveGeminiKey = viewModel::saveGeminiKey,
                 onOpenInterests = { screen = MatutineScreen.Interests(returnToSettings = true) },
                 onGenerate = {
                     viewModel.generate()
@@ -167,6 +172,10 @@ fun MatutineApp(
                 calendarPermissionOff = calendarPermissionOff,
                 onOpenCalendarPermission = ::openCalendarPermissionSettings,
                 editionLayoutId = viewModel.editionLayout,
+                sameDayExtras = ready?.sameDayExtras.orEmpty(),
+                onNewEditionNow = viewModel::newEditionNow,
+                onOpenExtra = viewModel::openExtra,
+                onOpenMorning = viewModel::openMorning,
                 onOpenTypewriter = if (showsTypewriter() && ready != null) {
                     {
                         typewriterDraft = EditionTranscript.render(ready.edition)
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
index d4c7314..ac42e6f 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
@@ -13,3 +13,6 @@ internal object EditionCopy {
 
 internal fun showsNoTopicLine(isSample: Boolean, topics: List<String>): Boolean =
     !isSample && topics.none { it.isNotBlank() }
+
+internal fun sameDayExtraLabel(edition: Edition): String =
+    edition.stories.firstOrNull()?.headline?.takeIf { it.isNotBlank() } ?: "Extra"
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
index 0d36849..c06cc9b 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
@@ -68,6 +68,10 @@ fun EditionScreen(
     onOpenCalendarPermission: () -> Unit = {},
     editionLayoutId: String = DEFAULT_EDITION_LAYOUT,
     onOpenTypewriter: (() -> Unit)? = null,
+    sameDayExtras: List<Edition> = emptyList(),
+    onNewEditionNow: () -> Unit = {},
+    onOpenExtra: (String) -> Unit = {},
+    onOpenMorning: () -> Unit = {},
     modifier: Modifier = Modifier,
 ) {
     val typography = LocalMatutineEditionTypography.current
@@ -96,6 +100,10 @@ fun EditionScreen(
             onOpenCalendarPermission = onOpenCalendarPermission,
             editionLayoutId = editionLayoutId,
             onOpenTypewriter = onOpenTypewriter,
+            sameDayExtras = sameDayExtras,
+            onNewEditionNow = onNewEditionNow,
+            onOpenExtra = onOpenExtra,
+            onOpenMorning = onOpenMorning,
             typography = typography,
             modifier = modifier,
         )
@@ -217,6 +225,10 @@ private fun EditionContent(
     onOpenCalendarPermission: () -> Unit,
     editionLayoutId: String,
     onOpenTypewriter: (() -> Unit)?,
+    sameDayExtras: List<Edition>,
+    onNewEditionNow: () -> Unit,
+    onOpenExtra: (String) -> Unit,
+    onOpenMorning: () -> Unit,
     typography: MatutineEditionTypography,
     modifier: Modifier = Modifier,
 ) {
@@ -254,6 +266,34 @@ private fun EditionContent(
                         Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
                     }
                 }
+                TextButton(
+                    onClick = onNewEditionNow,
+                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                ) {
+                    Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                }
+            }
+            if (edition.role == EditionRole.extra) {
+                TextButton(
+                    onClick = onOpenMorning,
+                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                ) {
+                    Text(
+                        text = "Today's morning edition",
+                        style = typography.body.copy(color = EditionThemeColors.current.ink),
+                    )
+                }
+            }
+            sameDayExtras.forEach { extra ->
+                TextButton(
+                    onClick = { onOpenExtra(extra.id) },
+                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                ) {
+                    Text(
+                        text = sameDayExtraLabel(extra),
+                        style = typography.body.copy(color = EditionThemeColors.current.ink),
+                    )
+                }
             }
             Masthead(edition.mastheadName, edition.dateLine, typography)
             if (generating) {
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
index acbabcc..a75ee92 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
@@ -17,6 +17,7 @@ sealed interface EditionUiState {
         val generating: Boolean = false,
         val notice: String? = null,
         val taskSaved: Boolean = false,
+        val sameDayExtras: List<Edition> = emptyList(),
     ) : EditionUiState
 
     data class SampleLoadFailed(val message: String = "The sample edition didn't load.") : EditionUiState
@@ -78,6 +79,9 @@ class EditionViewModel(
     var editionLayout by mutableStateOf(DEFAULT_EDITION_LAYOUT)
         private set
 
+    var geminiKey by mutableStateOf("")
+        private set
+
     var typewriterSaved by mutableStateOf(false)
         private set
 
@@ -92,6 +96,7 @@ class EditionViewModel(
     private var calDavUsernameBaseline: String = ""
     private var notificationTimeBaseline: String = DEFAULT_NOTIFICATION_TIME
     private var notificationBodyBaseline: String = EDITION_READY_NOTIFICATION_BODY
+    private var geminiKeyBaseline: String = ""
 
     init {
         feedBaseline = runCatching { repository.savedFeedUrls().singleOrNull().orEmpty() }.getOrDefault("")
@@ -110,6 +115,8 @@ class EditionViewModel(
         notificationBody = notificationBodyBaseline
         editionTheme = runCatching { repository.savedEditionTheme() }.getOrDefault(DEFAULT_EDITION_THEME)
         editionLayout = runCatching { repository.savedEditionLayout() }.getOrDefault(DEFAULT_EDITION_LAYOUT)
+        geminiKeyBaseline = runCatching { repository.savedGeminiKey() }.getOrDefault("")
+        geminiKey = geminiKeyBaseline
         load()
     }
 
@@ -150,6 +157,14 @@ class EditionViewModel(
             .onSuccess { editionTheme = repository.savedEditionTheme() }
     }
 
+    fun updateGeminiKey(value: String) {
+        geminiKey = value
+    }
+
+    fun saveGeminiKey() {
+        persistGeminiKey(geminiKey)
+    }
+
     fun saveEditionLayout(layoutId: String) {
         repository.saveEditionLayout(layoutId)
             .onSuccess { editionLayout = repository.savedEditionLayout() }
@@ -276,10 +291,16 @@ class EditionViewModel(
                 true
             }
             val notice = if (!calDavSaved && calendarNotice == null) CALDAV_DIDNT_LOAD else calendarNotice
+            val typedKey = geminiKey
+            if (typedKey.trim() != geminiKeyBaseline) persistGeminiKey(typedKey)
             repository.generateToday()
                 .onSuccess { edition ->
                     feedFieldError = null
-                    state = EditionUiState.Ready(edition, notice = notice)
+                    state = EditionUiState.Ready(
+                        edition,
+                        notice = notice,
+                        sameDayExtras = loadExtras(),
+                    )
                 }
                 .onFailure {
                     state = restore(FEED_DIDNT_LOAD)
@@ -291,11 +312,50 @@ class EditionViewModel(
         state = EditionUiState.Loading
         scope.launch {
             repository.loadCurrentMorningEdition()
-                .onSuccess { edition -> state = EditionUiState.Ready(edition) }
+                .onSuccess { edition ->
+                    state = EditionUiState.Ready(edition, sameDayExtras = loadExtras())
+                }
                 .onFailure { error -> state = mapFailure(error) }
         }
     }
 
+    fun newEditionNow() {
+        val current = state as? EditionUiState.Ready ?: return
+        if (current.generating) return
+        state = current.copy(generating = true, notice = null)
+        scope.launch {
+            repository.newEditionNow()
+                .onSuccess {
+                    val morning = repository.loadCurrentMorningEdition().getOrThrow()
+                    state = EditionUiState.Ready(morning, sameDayExtras = loadExtras())
+                }
+                .onFailure {
+                    val shown = state as? EditionUiState.Ready ?: current
+                    state = shown.copy(generating = false, notice = FEED_DIDNT_LOAD)
+                }
+        }
+    }
+
+    fun openExtra(id: String) {
+        val current = state as? EditionUiState.Ready ?: return
+        val extra = current.sameDayExtras.singleOrNull { it.id == id } ?: return
+        state = current.copy(edition = extra, taskSaved = false)
+    }
+
+    fun openMorning() {
+        val current = state as? EditionUiState.Ready ?: return
+        scope.launch {
+            repository.loadCurrentMorningEdition()
+                .onSuccess { morning ->
+                    state = current.copy(
+                        edition = morning,
+                        sameDayExtras = loadExtras(),
+                        taskSaved = false,
+                    )
+                }
+        }
+    }
+
     fun retrySample() {
         state = EditionUiState.Loading
         scope.launch {
@@ -338,7 +398,7 @@ class EditionViewModel(
         }
         val generation = ++typewriterGeneration
         scope.launch {
-            repository.updateMorningEdition(updated)
+            saveEdition(updated)
                 .onSuccess { saved ->
                     if (generation != typewriterGeneration) {
                         onResult(false)
@@ -378,7 +438,7 @@ class EditionViewModel(
         state = previous.copy(edition = updated)
         val rollback = previous
         scope.launch {
-            repository.updateMorningEdition(updated)
+            saveEdition(updated)
                 .onSuccess { saved ->
                     val current = state as? EditionUiState.Ready
                     if (current != null && current.edition.tasks == updated.tasks) {
@@ -394,6 +454,27 @@ class EditionViewModel(
         }
     }
 
+    private suspend fun loadExtras(): List<Edition> =
+        runCatching { repository.loadSameDayExtras() }.getOrDefault(emptyList())
+
+    private suspend fun saveEdition(edition: Edition): Result<Edition> =
+        if (edition.role == EditionRole.extra) {
+            repository.updateExtra(edition)
+        } else {
+            repository.updateMorningEdition(edition)
+        }
+
+    private fun persistGeminiKey(typed: String): Boolean {
+        val saved = repository.saveGeminiKey(typed)
+        if (saved.isSuccess) {
+            geminiKeyBaseline = typed.trim()
+            if (geminiKey == typed) geminiKey = geminiKeyBaseline
+            return true
+        }
+        if (geminiKey == typed) geminiKey = geminiKeyBaseline
+        return false
+    }
+
     private fun calDavFieldsDirty(url: String, username: String, password: String): Boolean =
         password.isNotEmpty() || url.trim() != calDavUrlBaseline || username.trim() != calDavUsernameBaseline
 
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt
index 6a56880..75939ae 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt
@@ -56,6 +56,9 @@ fun SettingsScreen(
     onCalDavPasswordChange: (String) -> Unit,
     calDavError: String?,
     onSaveCalDav: () -> Unit,
+    geminiKey: String,
+    onGeminiKeyChange: (String) -> Unit,
+    onSaveGeminiKey: () -> Unit,
     onOpenInterests: () -> Unit,
     onGenerate: () -> Unit,
     onBack: () -> Unit,
@@ -208,6 +211,23 @@ fun SettingsScreen(
                 )
             }
         }
+        Column(
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = controlHeight),
+            verticalArrangement = Arrangement.spacedBy(8.dp),
+        ) {
+            Text(text = "Gemini key", style = typography.body.copy(color = NewsprintColors.ink))
+            newsprintField(
+                value = geminiKey,
+                onValueChange = onGeminiKeyChange,
+                isError = false,
+                keyboardType = KeyboardType.Password,
+                imeAction = ImeAction.Done,
+                onDone = onSaveGeminiKey,
+                visualTransformation = PasswordVisualTransformation(),
+            )
+        }
         if (notificationTime != null && onNotificationTimeChange != null && onSaveNotificationTime != null) {
             Column(
                 modifier = Modifier
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
index ee97379..c1897d5 100644
--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
@@ -382,6 +382,77 @@ class EditionGenerateStateTest {
         assertEquals(1, repository.generateCalls)
     }
 
+    @Test
+    fun newEditionNowLeavesTheMorningOnTheFrontPageAndTheListOpensTheExtra() {
+        val repository = FakeEditionRepository(finished)
+        val viewModel = viewModel(repository)
+
+        viewModel.newEditionNow()
+
+        val front = viewModel.state as EditionUiState.Ready
+        assertEquals(finished.id, front.edition.id)
+        assertEquals(EditionRole.morning, front.edition.role)
+        assertEquals(finished.briefing, front.edition.briefing)
+        val extra = front.sameDayExtras.single()
+        assertEquals(EditionRole.extra, extra.role)
+        assertEquals(extra.dayKey, finished.dayKey)
+        assertEquals(sameDayExtraLabel(extra), extra.stories.first().headline)
+
+        viewModel.openExtra(extra.id)
+        val opened = viewModel.state as EditionUiState.Ready
+        assertEquals(extra.id, opened.edition.id)
+        assertEquals(listOf(extra), opened.sameDayExtras)
+
+        viewModel.openMorning()
+        val back = viewModel.state as EditionUiState.Ready
+        assertEquals(finished.id, back.edition.id)
+        assertEquals(EditionRole.morning, back.edition.role)
+        assertTrue(back.sameDayExtras.all { it.dayKey == finished.dayKey })
+    }
+
+    @Test
+    fun failedNewEditionNowKeepsTheVisiblePaper() {
+        val repository = FakeEditionRepository(finished)
+        repository.failNewEdition = true
+        val viewModel = viewModel(repository)
+
+        viewModel.newEditionNow()
+
+        val ready = viewModel.state as EditionUiState.Ready
+        assertFalse(ready.generating)
+        assertEquals(finished.id, ready.edition.id)
+        assertEquals(FEED_DIDNT_LOAD, ready.notice)
+        assertTrue(ready.sameDayExtras.isEmpty())
+    }
+
+    @Test
+    fun generatePersistsADirtyGeminiKeyBeforeGenerating() {
+        val repository = FakeEditionRepository(sample, generate = { finished })
+        val viewModel = viewModel(repository)
+        viewModel.updateGeminiKey("  configured  ")
+
+        viewModel.generate()
+
+        assertEquals("configured", viewModel.geminiKey)
+        assertEquals("configured", repository.savedGeminiKey())
+        assertEquals(1, repository.generateCalls)
+    }
+
+    @Test
+    fun failedGeminiKeySaveKeepsThePreviousKeyAndStillGenerates() {
+        val repository = FakeEditionRepository(sample, generate = { finished })
+        repository.geminiKey = "configured"
+        val viewModel = viewModel(repository)
+        viewModel.updateGeminiKey("next")
+        repository.failGeminiSave = true
+
+        viewModel.generate()
+
+        assertEquals("configured", viewModel.geminiKey)
+        assertEquals("configured", repository.savedGeminiKey())
+        assertEquals(1, repository.generateCalls)
+    }
+
     private fun viewModel(repository: EditionRepository) =
         EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
 }
@@ -452,6 +523,17 @@ private class FakeEditionRepository(
 
     override fun saveEditionLayout(layoutId: String): Result<Unit> = Result.success(Unit)
 
+    var geminiKey: String = ""
+    var failGeminiSave: Boolean = false
+
+    override fun savedGeminiKey(): String = geminiKey
+
+    override fun saveGeminiKey(key: String): Result<Unit> {
+        if (failGeminiSave) return Result.failure(IllegalStateException("settings_unavailable"))
+        geminiKey = key.trim()
+        return Result.success(Unit)
+    }
+
     override suspend fun saveFeedUrl(url: String): Result<Unit> {
         saveCalls += 1
         blockSave?.await()
@@ -485,5 +567,25 @@ private class FakeEditionRepository(
         return runCatching { generate() }
     }
 
+    private val extras = mutableListOf<Edition>()
+    var failNewEdition: Boolean = false
+
+    override suspend fun newEditionNow(): Result<Edition> {
+        if (failNewEdition) return Result.failure(IllegalStateException(FEED_DIDNT_LOAD))
+        val extra = edition.copy(
+            id = "extra-${edition.dayKey}-1",
+            revision = 1,
+            role = EditionRole.extra,
+            briefing = "Fresh extra",
+        )
+        extras += extra
+        return Result.success(extra)
+    }
+
+    override suspend fun loadSameDayExtras(): List<Edition> = extras.toList()
+
     override suspend fun updateMorningEdition(edition: Edition): Result<Edition> = Result.success(edition)
+
+    override suspend fun updateExtra(edition: Edition): Result<Edition> =
+        Result.success(edition.copy(revision = edition.revision + 1))
 }
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
index 1bc52ab..cbae170 100644
--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
@@ -120,8 +120,16 @@ class EditionTaskStateTest {
 
         override fun saveEditionLayout(layoutId: String): Result<Unit> = Result.success(Unit)
 
+        override fun savedGeminiKey(): String = ""
+
+        override fun saveGeminiKey(key: String): Result<Unit> = Result.success(Unit)
+
         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
 
+        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
+
+        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
+
         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
             updateCalls += 1
             val next = edition.copy(revision = morning.revision + 1)
@@ -129,5 +137,7 @@ class EditionTaskStateTest {
             lastSaved = next
             return Result.success(next)
         }
+
+        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
     }
 }
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
index e241ccb..5f80185 100644
--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
@@ -170,8 +170,16 @@ class EditionTypewriterStateTest {
 
         override fun saveEditionLayout(layoutId: String): Result<Unit> = Result.success(Unit)
 
+        override fun savedGeminiKey(): String = ""
+
+        override fun saveGeminiKey(key: String): Result<Unit> = Result.success(Unit)
+
         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
 
+        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
+
+        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
+
         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
             gate?.await()
             if (fail) return Result.failure(IllegalStateException("save failed"))
@@ -180,5 +188,7 @@ class EditionTypewriterStateTest {
             morning = next
             return Result.success(next)
         }
+
+        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
     }
 }
diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
index 6a39885..ff3af60 100644
--- a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
+++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
@@ -21,6 +21,10 @@ actual class FileEditionStore actual constructor(
 
     override fun saveMorningEdition(edition: Edition) = Unit
 
+    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
+
+    override fun saveExtra(edition: Edition) = Unit
+
     override fun readCalendarSnapshot(): CalendarSnapshot? = null
 
     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = Unit
diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
index f5f6de0..66e56d1 100644
--- a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
+++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
@@ -61,6 +61,25 @@ actual class FileEditionStore actual constructor(
         file.writeText(encodeEdition(edition))
     }
 
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
+        val dir = File(root, "editions")
+        dir.mkdirs()
+        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
+    }
+
     override fun readCalendarSnapshot(): CalendarSnapshot? {
         val file = File(root, "calendar-snapshot.json")
         if (!file.exists()) return null
diff --git a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
index 2e3f14b..d14b31e 100644
--- a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
+++ b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
@@ -3,11 +3,14 @@ package hu.toliver.matutine.store
 import hu.toliver.matutine.edition.CalDavAccount
 import hu.toliver.matutine.edition.CalendarOccurrence
 import hu.toliver.matutine.edition.CalendarSnapshot
+import hu.toliver.matutine.edition.EditionRole
 import hu.toliver.matutine.edition.LocalEditionRepository
 import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SampleGenerator
 import hu.toliver.matutine.edition.SourceList
 import hu.toliver.matutine.edition.StoryRank
 import kotlinx.coroutines.runBlocking
+import kotlinx.datetime.LocalDate
 import java.io.File
 import java.nio.file.Files
 import kotlin.test.Test
@@ -149,4 +152,33 @@ class FileEditionStoreTest {
         assertTrue(File(dir, "calendar-snapshot.json").exists())
         assertNull(store.readSourceList())
     }
+
+    @Test
+    fun saveExtra_roundTripsWithoutRewritingTheMorningFile() {
+        val dir = Files.createTempDirectory("matutine-extra").toFile()
+        val store = FileEditionStore(dir.absolutePath)
+        val morning = SampleGenerator.forDay(LocalDate(2026, 11, 7)).copy(
+            id = "morning-2026-11-07",
+            isSample = false,
+            briefing = "Morning stays",
+        )
+        store.saveMorningEdition(morning)
+        val morningFile = File(dir, "editions/2026-11-07-morning.json")
+        val before = morningFile.readText()
+        val extra = morning.copy(
+            id = "extra-2026-11-07-1",
+            revision = 1,
+            role = EditionRole.extra,
+            briefing = "Extra",
+        )
+        val otherDay = extra.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06")
+
+        store.saveExtra(extra)
+        store.saveExtra(otherDay)
+
+        assertEquals(before, morningFile.readText())
+        assertEquals(listOf(extra), store.loadExtras("2026-11-07"))
+        assertEquals(listOf(otherDay), store.loadExtras("2026-11-06"))
+        assertEquals(morning, store.loadMorningEdition("2026-11-07"))
+    }
 }
diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
index 6a39885..ff3af60 100644
--- a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
+++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
@@ -21,6 +21,10 @@ actual class FileEditionStore actual constructor(
 
     override fun saveMorningEdition(edition: Edition) = Unit
 
+    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
+
+    override fun saveExtra(edition: Edition) = Unit
+
     override fun readCalendarSnapshot(): CalendarSnapshot? = null
 
     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = Unit
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
index d62d029..5b6273a 100644
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
@@ -16,6 +16,8 @@ interface EditionStore {
 
     fun loadMorningEdition(dayKey: String): Edition?
     fun saveMorningEdition(edition: Edition)
+    fun loadExtras(dayKey: String): List<Edition>
+    fun saveExtra(edition: Edition)
 
     /** Null when the snapshot document is absent. */
     fun readCalendarSnapshot(): CalendarSnapshot?
@@ -39,6 +41,7 @@ class LocalEditionRepository(
     private val readDeviceCalendar: suspend (LocalDate) -> DeviceCalendarRead = { DeviceCalendarRead.Unavailable },
     private val readCalDav: suspend (CalDavAccount) -> String = { error(CALDAV_DIDNT_LOAD) },
     private val serverConfigured: Boolean = false,
+    private val summarize: suspend (List<EditionStory>, String) -> String = { _, _ -> error("summarizer_unavailable") },
 ) : EditionRepository {
     override suspend fun loadCurrentMorningEdition(): Result<Edition> = runCatching {
         val dayKey = today().toDayKey()
@@ -123,6 +126,15 @@ class LocalEditionRepository(
         store.writeSettings(current.copy(editionTheme = normalized))
     }
 
+    override fun savedGeminiKey(): String = store.readSettings()?.geminiKey?.trim().orEmpty()
+
+    override fun saveGeminiKey(key: String): Result<Unit> = runCatching {
+        val normalized = key.trim()
+        val current = store.readSettings() ?: ReaderSettings()
+        if (current.geminiKey == normalized) return@runCatching
+        store.writeSettings(current.copy(geminiKey = normalized))
+    }
+
     override fun saveEditionLayout(layoutId: String): Result<Unit> = runCatching {
         val normalized = normalizeEditionLayoutId(layoutId) ?: error("invalid_edition_layout")
         val current = store.readSettings() ?: ReaderSettings()
@@ -261,12 +273,85 @@ class LocalEditionRepository(
         } else {
             edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
         }
+        val next = if (replaceExisting && existing != null && !existing.isSample) {
+            keepMorningEdits(existing, noted)
+        } else {
+            withModelBriefing(noted)
+        }
         if (!replaceExisting) {
             val current = store.loadMorningEdition(date.toDayKey())
             if (current != null) return current
         }
-        store.saveMorningEdition(noted)
-        return noted
+        store.saveMorningEdition(next)
+        return next
+    }
+
+    override suspend fun newEditionNow(): Result<Edition> = runCatching {
+        val date = today()
+        val dayKey = date.toDayKey()
+        if (store.loadMorningEdition(dayKey) == null) error(MORNING_EDITION_NOT_READY)
+        val list = store.readSourceList()
+        if (list == null || list.feeds.isEmpty()) error(FEED_DIDNT_LOAD)
+        val parsed = list.feeds.map { url ->
+            runCatching { parseFeedDocument(readDocument(url)) }
+        }
+        if (parsed.none { it.isSuccess }) error(FEED_DIDNT_LOAD)
+        recordDeviceCalendar(date)
+        val calDav = readCalDavOccurrences()
+        val events = editionEventsFrom(
+            sources = listOf(
+                readIcsOccurrences(list),
+                store.readCalendarSnapshot()?.occurrences.orEmpty(),
+                calDav.occurrences,
+            ),
+            day = date,
+            zone = timeZone,
+        )
+        val edition = EditionGenerator.fromFeedResults(
+            date = date,
+            parsed = parsed,
+            existing = null,
+            topics = store.readSettings()?.topics.orEmpty(),
+            events = events,
+        )
+        val noted = if (calDav.notes.isEmpty()) {
+            edition
+        } else {
+            edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
+        }
+        val extra = withModelBriefing(noted).copy(
+            id = nextExtraId(dayKey),
+            revision = 1,
+            role = EditionRole.extra,
+        )
+        store.saveExtra(extra)
+        extra
+    }
+
+    override suspend fun loadSameDayExtras(): List<Edition> =
+        store.loadExtras(today().toDayKey())
+
+    override suspend fun updateExtra(edition: Edition): Result<Edition> = runCatching {
+        val dayKey = today().toDayKey()
+        require(edition.dayKey == dayKey && edition.role == EditionRole.extra)
+        val stored = store.loadExtras(dayKey).singleOrNull { it.id == edition.id }
+            ?: error(MORNING_EDITION_NOT_READY)
+        val next = edition.copy(
+            id = stored.id,
+            revision = stored.revision + 1,
+            dayKey = stored.dayKey,
+            role = EditionRole.extra,
+        )
+        store.saveExtra(next)
+        next
+    }
+
+    private fun nextExtraId(dayKey: String): String {
+        val prefix = "extra-$dayKey-"
+        val used = store.loadExtras(dayKey).mapNotNull { extra ->
+            extra.id.removePrefix(prefix).toIntOrNull()
+        }
+        return prefix + ((used.maxOrNull() ?: 0) + 1)
     }
 
     override fun savedCalDavAccount(): CalDavAccount? =
@@ -322,6 +407,19 @@ class LocalEditionRepository(
             rethrowCancellation(error)
             error(FEED_DIDNT_LOAD)
         }
+
+    private suspend fun withModelBriefing(edition: Edition): Edition {
+        val key = savedGeminiKey()
+        if (key.isEmpty() || edition.stories.isEmpty()) return edition
+        val text = try {
+            summarize(edition.stories, key).trim()
+        } catch (error: Throwable) {
+            rethrowCancellation(error)
+            return edition
+        }
+        if (text.isEmpty()) return edition
+        return edition.copy(briefing = text)
+    }
 }
 
 private data class CalDavRead(
@@ -352,6 +450,21 @@ interface EditionRepository {
     fun saveEditionTheme(themeId: String): Result<Unit>
     fun savedEditionLayout(): String
     fun saveEditionLayout(layoutId: String): Result<Unit>
+    fun savedGeminiKey(): String
+    fun saveGeminiKey(key: String): Result<Unit>
     suspend fun generateToday(): Result<Edition>
+    suspend fun newEditionNow(): Result<Edition>
+    suspend fun loadSameDayExtras(): List<Edition>
     suspend fun updateMorningEdition(edition: Edition): Result<Edition>
+    suspend fun updateExtra(edition: Edition): Result<Edition>
+}
+
+internal fun keepMorningEdits(existing: Edition, generated: Edition): Edition {
+    val freshKeys = generated.events.map { it.title to it.timeLabel }.toSet()
+    val keptLines = existing.events.filter { (it.title to it.timeLabel) !in freshKeys }
+    return generated.copy(
+        briefing = existing.briefing,
+        events = generated.events + keptLines,
+        tasks = existing.tasks,
+    )
 }
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt
index f3311a8..7939988 100644
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt
@@ -26,6 +26,7 @@ data class ReaderSettings(
     val notificationBody: String = EDITION_READY_NOTIFICATION_BODY,
     val editionTheme: String = DEFAULT_EDITION_THEME,
     val editionLayout: String = DEFAULT_EDITION_LAYOUT,
+    val geminiKey: String = "",
 )
 
 fun ReaderSettings.resolvedNotificationTime(): String =
diff --git a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
index 711cd09..ac3ab39 100644
--- a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+++ b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
@@ -22,6 +22,7 @@ private class FakeStore(
 ) : EditionStore {
     var saved: Edition? = null
     var snapshotWrites: Int = 0
+    var failSettingsWrite: Boolean = false
 
     override fun readSourceList(): SourceList? = sourceList
 
@@ -32,6 +33,7 @@ private class FakeStore(
     override fun readSettings(): ReaderSettings? = settings
 
     override fun writeSettings(settings: ReaderSettings) {
+        if (failSettingsWrite) error("settings_unavailable")
         this.settings = settings
     }
 
@@ -43,6 +45,21 @@ private class FakeStore(
         saved = edition
     }
 
+    private val extras = mutableListOf<Edition>()
+
+    override fun loadExtras(dayKey: String): List<Edition> =
+        extras.filter { it.dayKey == dayKey && it.role == EditionRole.extra }
+
+    override fun saveExtra(edition: Edition) {
+        require(edition.role == EditionRole.extra)
+        val index = extras.indexOfFirst { it.id == edition.id }
+        if (index >= 0) extras[index] = edition else extras += edition
+    }
+
+    fun seedExtra(edition: Edition) {
+        extras += edition
+    }
+
     override fun readCalendarSnapshot(): CalendarSnapshot? = snapshot
 
     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
@@ -428,6 +445,148 @@ class LocalEditionRepositoryTest {
         assertFalse(edition.isSample)
     }
 
+    @Test
+    fun generate_again_keepsBriefingRewrittenLineAndTasks() = runBlocking {
+        val existing = SampleGenerator.forDay(sampleDay).copy(
+            id = "morning-${sampleDay.toDayKey()}",
+            revision = 3,
+            isSample = false,
+            briefing = "Kept by hand",
+            events = listOf(
+                EditionEvent("Planning", "9:30"),
+                EditionEvent("Team standup", "10:00"),
+            ),
+            tasks = listOf(
+                EditionTask("Buy milk", completed = true),
+                EditionTask("File notes"),
+            ),
+        )
+        val store = FakeStore(
+            morning = existing,
+            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+            snapshot = CalendarSnapshot(
+                occurrences = listOf(
+                    CalendarOccurrence(title = "Planning", startEpochSecond = utc(9, 30), endEpochSecond = utc(10, 0)),
+                ),
+            ),
+        )
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            timeZone = TimeZone.UTC,
+            readFeed = { PERSONAL_RSS },
+        )
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals(existing.id, edition.id)
+        assertEquals(4, edition.revision)
+        assertEquals("Kept by hand", edition.briefing)
+        assertEquals(existing.tasks, edition.tasks)
+        assertTrue(edition.events.contains(EditionEvent("Planning", "9:30")))
+        assertTrue(edition.events.contains(EditionEvent("Team standup", "10:00")))
+        assertEquals(1, edition.events.count { it == EditionEvent("Planning", "9:30") })
+        assertEquals(
+            listOf("Markets open higher", "Port reopens"),
+            edition.stories.map { it.headline },
+        )
+        assertEquals(StoryRank.lead, edition.stories.first().rank)
+    }
+
+    @Test
+    fun newEditionNow_insertsAnExtraAndLeavesOtherEditions() = runBlocking {
+        val morning = SampleGenerator.forDay(sampleDay).copy(
+            id = "morning-${sampleDay.toDayKey()}",
+            revision = 2,
+            isSample = false,
+            briefing = "Morning stays",
+        )
+        val older = morning.copy(
+            id = "extra-${sampleDay.toDayKey()}-1",
+            revision = 1,
+            role = EditionRole.extra,
+            briefing = "Older extra",
+        )
+        val store = FakeStore(
+            morning = morning,
+            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+        )
+        store.seedExtra(older)
+        store.seedExtra(older.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06", briefing = "Yesterday"))
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })
+
+        val extra = repo.newEditionNow().getOrThrow()
+
+        assertEquals(EditionRole.extra, extra.role)
+        assertEquals("extra-${sampleDay.toDayKey()}-2", extra.id)
+        assertEquals(1, extra.revision)
+        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
+        assertEquals(older, store.loadExtras(sampleDay.toDayKey()).first())
+        assertEquals(morning, repo.loadCurrentMorningEdition().getOrThrow())
+        assertEquals(listOf(older, extra), repo.loadSameDayExtras())
+        assertTrue(repo.loadSameDayExtras().none { it.dayKey != sampleDay.toDayKey() })
+    }
+
+    @Test
+    fun lateSnapshot_isOnRegenerateOrExtraAndNotOnThePaperBefore() = runBlocking {
+        val morning = SampleGenerator.forDay(sampleDay).copy(
+            id = "morning-${sampleDay.toDayKey()}",
+            revision = 1,
+            isSample = false,
+            events = listOf(EditionEvent("Kept", "8:00")),
+        )
+        val later = CalendarOccurrence(title = "Later", startEpochSecond = utc(15, 0), endEpochSecond = utc(16, 0))
+        val store = FakeStore(
+            morning = morning,
+            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+            snapshot = CalendarSnapshot(occurrences = listOf(later)),
+        )
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            timeZone = TimeZone.UTC,
+            readFeed = { PERSONAL_RSS },
+        )
+
+        assertEquals(listOf(EditionEvent("Kept", "8:00")), repo.loadCurrentMorningEdition().getOrThrow().events)
+
+        val regenerated = repo.generateToday().getOrThrow()
+        assertTrue(regenerated.events.contains(EditionEvent("Later", "15:00")))
+        assertEquals(morning.id, regenerated.id)
+
+        val untouched = regenerated.copy(events = listOf(EditionEvent("Kept", "8:00")), revision = regenerated.revision)
+        store.saveMorningEdition(untouched)
+        val extra = repo.newEditionNow().getOrThrow()
+        assertEquals(untouched, store.loadMorningEdition(sampleDay.toDayKey()))
+        assertTrue(extra.events.contains(EditionEvent("Later", "15:00")))
+        assertFalse(extra.events.contains(EditionEvent("Kept", "8:00")))
+    }
+
+    @Test
+    fun failedGenerateAndExtra_leaveStoredEditions() = runBlocking {
+        val morning = SampleGenerator.forDay(sampleDay).copy(
+            id = "morning-${sampleDay.toDayKey()}",
+            revision = 2,
+            isSample = false,
+            briefing = "Still here",
+        )
+        val store = FakeStore(
+            morning = morning,
+            sourceList = SourceList(feeds = listOf("https://example.com/bad.xml")),
+        )
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { NOT_A_FEED })
+
+        val generated = repo.generateToday()
+        val extra = repo.newEditionNow()
+
+        assertTrue(generated.isFailure)
+        assertEquals(FEED_DIDNT_LOAD, generated.exceptionOrNull()?.message)
+        assertTrue(extra.isFailure)
+        assertEquals(FEED_DIDNT_LOAD, extra.exceptionOrNull()?.message)
+        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
+        assertTrue(store.loadExtras(sampleDay.toDayKey()).isEmpty())
+    }
+
     @Test
     fun saveTopics_storesChoicesLinesAndLeavesTheSourceList() = runBlocking {
         val list = SourceList(
@@ -1532,6 +1691,216 @@ class EditionTaskPersistenceTest {
         val otherId = morning.copy(id = "other")
         assertTrue(repo.updateMorningEdition(otherId).isFailure)
     }
+
+    @Test
+    fun generate_withKey_storesOneModelBriefingOnANewMorning() = runBlocking {
+        var calls = 0
+        val store = FakeStore(
+            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+            settings = ReaderSettings(geminiKey = "configured"),
+        )
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            readFeed = { PERSONAL_RSS },
+            summarize = { stories, key ->
+                calls += 1
+                assertEquals("configured", key)
+                assertEquals("Markets open higher", stories.first().headline)
+                "The harbor is quiet."
+            },
+        )
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals("The harbor is quiet.", edition.briefing)
+        assertFalse(edition.briefing.contains("Summary assembled from headlines."))
+        assertFalse(edition.briefing.contains("gemini", ignoreCase = true))
+        assertEquals(1, calls)
+        assertFalse(edition.isSample)
+    }
+
+    @Test
+    fun generate_withKey_replacesASampleWithOneModelBriefing() = runBlocking {
+        var calls = 0
+        val store = FakeStore()
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            readFeed = { PERSONAL_RSS },
+            summarize = { _, _ ->
+                calls += 1
+                "A written opening."
+            },
+        )
+        val sample = repo.loadCurrentMorningEdition().getOrThrow()
+        assertTrue(sample.isSample)
+        assertEquals(0, calls)
+        store.writeSettings(ReaderSettings(geminiKey = "configured"))
+        repo.saveFeedUrl("https://example.com/rss.xml").getOrThrow()
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals("A written opening.", edition.briefing)
+        assertFalse(edition.isSample)
+        assertEquals(1, calls)
+    }
+
+    @Test
+    fun generate_withoutKey_keepsTheExtractiveBriefingAndDoesNotCall() = runBlocking {
+        var calls = 0
+        listOf<ReaderSettings?>(null, ReaderSettings(geminiKey = "  ")).forEach { settings ->
+            val store = FakeStore(
+                sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+                settings = settings,
+            )
+            val repo = LocalEditionRepository(
+                store,
+                today = { sampleDay },
+                readFeed = { PERSONAL_RSS },
+                summarize = { _, _ ->
+                    calls += 1
+                    "Should not be stored."
+                },
+            )
+
+            val edition = repo.generateToday().getOrThrow()
+
+            assertTrue(edition.briefing.endsWith("Summary assembled from headlines."))
+            assertEquals(edition, store.saved)
+        }
+        assertEquals(0, calls)
+    }
+
+    @Test
+    fun generate_whenTheCallFailsOrIsBlank_keepsTheExtractiveBriefing() = runBlocking {
+        listOf<() -> String>({ error("summarizer_unavailable") }, { "  " }).forEach { result ->
+            var calls = 0
+            val store = FakeStore(
+                sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+                settings = ReaderSettings(geminiKey = "configured"),
+            )
+            val repo = LocalEditionRepository(
+                store,
+                today = { sampleDay },
+                readFeed = { PERSONAL_RSS },
+                summarize = { _, _ ->
+                    calls += 1
+                    result()
+                },
+            )
+
+            val edition = repo.generateToday().getOrThrow()
+
+            assertTrue(edition.briefing.endsWith("Summary assembled from headlines."))
+            assertEquals(1, calls)
+            assertEquals(edition, store.saved)
+        }
+    }
+
+    @Test
+    fun generate_again_withKey_keepsTheBriefingAndDoesNotCall() = runBlocking {
+        var calls = 0
+        val existing = SampleGenerator.forDay(sampleDay).copy(
+            id = "morning-${sampleDay.toDayKey()}",
+            revision = 3,
+            isSample = false,
+            briefing = "Kept by hand",
+        )
+        val store = FakeStore(
+            morning = existing,
+            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+            settings = ReaderSettings(geminiKey = "configured"),
+        )
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            readFeed = { PERSONAL_RSS },
+            summarize = { _, _ ->
+                calls += 1
+                "A new opening."
+            },
+        )
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals("Kept by hand", edition.briefing)
+        assertEquals(0, calls)
+    }
+
+    @Test
+    fun newEditionNow_withKey_storesOneModelBriefingOnTheExtra() = runBlocking {
+        var calls = 0
+        val morning = SampleGenerator.forDay(sampleDay).copy(
+            id = "morning-${sampleDay.toDayKey()}",
+            revision = 2,
+            isSample = false,
+            briefing = "Morning stays",
+        )
+        val store = FakeStore(
+            morning = morning,
+            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+            settings = ReaderSettings(geminiKey = "configured"),
+        )
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            readFeed = { PERSONAL_RSS },
+            summarize = { _, _ ->
+                calls += 1
+                "Extra opening."
+            },
+        )
+
+        val extra = repo.newEditionNow().getOrThrow()
+
+        assertEquals("Extra opening.", extra.briefing)
+        assertEquals(EditionRole.extra, extra.role)
+        assertEquals(1, calls)
+        assertEquals("Morning stays", repo.loadCurrentMorningEdition().getOrThrow().briefing)
+    }
+
+    @Test
+    fun sampleRetry_withKey_staysExtractive() = runBlocking {
+        var calls = 0
+        val store = FakeStore(settings = ReaderSettings(geminiKey = "configured"))
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            summarize = { _, _ ->
+                calls += 1
+                "Should not be stored."
+            },
+        )
+
+        val retried = repo.retrySample().getOrThrow()
+
+        assertTrue(retried.isSample)
+        assertTrue(retried.briefing.endsWith("Summary assembled from headlines."))
+        assertEquals(0, calls)
+    }
+
+    @Test
+    fun saveGeminiKey_replacesASavedKeyAndBlankClearsIt() {
+        val store = FakeStore(settings = ReaderSettings(geminiKey = "configured"))
+        val repo = LocalEditionRepository(store)
+
+        assertEquals("configured", repo.savedGeminiKey())
+        assertTrue(repo.saveGeminiKey("  next  ").isSuccess)
+        assertEquals("next", repo.savedGeminiKey())
+        assertTrue(repo.saveGeminiKey("   ").isSuccess)
+        assertEquals("", repo.savedGeminiKey())
+    }
+
+    @Test
+    fun saveGeminiKey_failedWriteKeepsThePreviousKey() {
+        val store = FakeStore(settings = ReaderSettings(geminiKey = "configured"))
+        store.failSettingsWrite = true
+        val repo = LocalEditionRepository(store)
+
+        assertTrue(repo.saveGeminiKey("next").isFailure)
+        assertEquals("configured", store.readSettings()?.geminiKey)
+    }
 }
 
 class SampleGeneratorTest {
diff --git a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/ReaderSettingsTest.kt b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/ReaderSettingsTest.kt
index 8f0663f..a499186 100644
--- a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/ReaderSettingsTest.kt
+++ b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/ReaderSettingsTest.kt
@@ -32,6 +32,7 @@ class ReaderSettingsTest {
         assertEquals(EDITION_READY_NOTIFICATION_BODY, settings.resolvedNotificationBody())
         assertEquals(DEFAULT_EDITION_THEME, settings.resolvedEditionTheme())
         assertEquals(DEFAULT_EDITION_LAYOUT, settings.resolvedEditionLayout())
+        assertEquals("", settings.geminiKey)
     }
 
     @Test
diff --git a/_bmad-output/implementation-artifacts/review-2-6-blind-hunter-prompt.md b/_bmad-output/implementation-artifacts/review-2-6-blind-hunter-prompt.md
new file mode 100644
index 0000000..bd86b67
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/review-2-6-blind-hunter-prompt.md
@@ -0,0 +1,895 @@
+Conduct a review of CONTENT.
+Look for what's missing, not only what's wrong.
+Compute your finding floor N from the diff file's size: N = min(floor(sqrt(kB) + 1), 10), where kB is the file's size in kilobytes. State the arithmetic in one line, then find at least N issues to fix or improve.
+Output a Markdown list of findings only — no severity, priority, or ranking.
+If the content is empty, stop and say so.
+If you have zero findings, re-check and keep thinking; do not stop with an empty list.
+
+CONTENT: the unified diff at the following contents. Read that file — it is the content under review.
+
+diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
+index ceed8f0..08ea20d 100644
+--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
++++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
+@@ -29,7 +29,7 @@
+ # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
+ # - Retrospective appends its action items to action_items; the status view surfaces open ones
+ generated: 11-07-2026 19:42
+-last_updated: 11-10-2026 08:37
++last_updated: 11-10-2026 18:11
+ project: Matutine
+ project_key: NOKEY
+ tracking_system: file-system
+@@ -55,7 +55,7 @@ development_status:
+   2-3-customize-the-android-paper: done
+   2-4-use-the-desktop-tray: done
+   2-5-rewrite-the-edition-on-the-typewriter: done
+-  2-6-regenerate-without-losing-the-edits: backlog
++  2-6-regenerate-without-losing-the-edits: in-progress
+   2-7-write-the-briefing-with-a-gemini-key: backlog
+   epic-2-retrospective: optional
+ 
+diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
+index 3f7b6cd..2fcca9e 100644
+--- a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
++++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
+@@ -62,6 +62,25 @@ actual class FileEditionStore actual constructor(
+         file.writeText(encodeEdition(edition))
+     }
+ 
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
++        val dir = File(root, "editions")
++        dir.mkdirs()
++        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? {
+         val file = File(root, "calendar-snapshot.json")
+         if (!file.exists()) return null
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+index 7c6b5dc..7895a75 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+@@ -167,6 +167,10 @@ fun MatutineApp(
+                 calendarPermissionOff = calendarPermissionOff,
+                 onOpenCalendarPermission = ::openCalendarPermissionSettings,
+                 editionLayoutId = viewModel.editionLayout,
++                sameDayExtras = ready?.sameDayExtras.orEmpty(),
++                onNewEditionNow = viewModel::newEditionNow,
++                onOpenExtra = viewModel::openExtra,
++                onOpenMorning = viewModel::openMorning,
+                 onOpenTypewriter = if (showsTypewriter() && ready != null) {
+                     {
+                         typewriterDraft = EditionTranscript.render(ready.edition)
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
+index d4c7314..ac42e6f 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
+@@ -13,3 +13,6 @@ internal object EditionCopy {
+ 
+ internal fun showsNoTopicLine(isSample: Boolean, topics: List<String>): Boolean =
+     !isSample && topics.none { it.isNotBlank() }
++
++internal fun sameDayExtraLabel(edition: Edition): String =
++    edition.stories.firstOrNull()?.headline?.takeIf { it.isNotBlank() } ?: "Extra"
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+index 0d36849..c06cc9b 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+@@ -68,6 +68,10 @@ fun EditionScreen(
+     onOpenCalendarPermission: () -> Unit = {},
+     editionLayoutId: String = DEFAULT_EDITION_LAYOUT,
+     onOpenTypewriter: (() -> Unit)? = null,
++    sameDayExtras: List<Edition> = emptyList(),
++    onNewEditionNow: () -> Unit = {},
++    onOpenExtra: (String) -> Unit = {},
++    onOpenMorning: () -> Unit = {},
+     modifier: Modifier = Modifier,
+ ) {
+     val typography = LocalMatutineEditionTypography.current
+@@ -96,6 +100,10 @@ fun EditionScreen(
+             onOpenCalendarPermission = onOpenCalendarPermission,
+             editionLayoutId = editionLayoutId,
+             onOpenTypewriter = onOpenTypewriter,
++            sameDayExtras = sameDayExtras,
++            onNewEditionNow = onNewEditionNow,
++            onOpenExtra = onOpenExtra,
++            onOpenMorning = onOpenMorning,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -217,6 +225,10 @@ private fun EditionContent(
+     onOpenCalendarPermission: () -> Unit,
+     editionLayoutId: String,
+     onOpenTypewriter: (() -> Unit)?,
++    sameDayExtras: List<Edition>,
++    onNewEditionNow: () -> Unit,
++    onOpenExtra: (String) -> Unit,
++    onOpenMorning: () -> Unit,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -254,6 +266,34 @@ private fun EditionContent(
+                         Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                     }
+                 }
++                TextButton(
++                    onClick = onNewEditionNow,
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                }
++            }
++            if (edition.role == EditionRole.extra) {
++                TextButton(
++                    onClick = onOpenMorning,
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(
++                        text = "Today's morning edition",
++                        style = typography.body.copy(color = EditionThemeColors.current.ink),
++                    )
++                }
++            }
++            sameDayExtras.forEach { extra ->
++                TextButton(
++                    onClick = { onOpenExtra(extra.id) },
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(
++                        text = sameDayExtraLabel(extra),
++                        style = typography.body.copy(color = EditionThemeColors.current.ink),
++                    )
++                }
+             }
+             Masthead(edition.mastheadName, edition.dateLine, typography)
+             if (generating) {
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+index acbabcc..fa54a3b 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+@@ -17,6 +17,7 @@ sealed interface EditionUiState {
+         val generating: Boolean = false,
+         val notice: String? = null,
+         val taskSaved: Boolean = false,
++        val sameDayExtras: List<Edition> = emptyList(),
+     ) : EditionUiState
+ 
+     data class SampleLoadFailed(val message: String = "The sample edition didn't load.") : EditionUiState
+@@ -279,7 +280,11 @@ class EditionViewModel(
+             repository.generateToday()
+                 .onSuccess { edition ->
+                     feedFieldError = null
+-                    state = EditionUiState.Ready(edition, notice = notice)
++                    state = EditionUiState.Ready(
++                        edition,
++                        notice = notice,
++                        sameDayExtras = loadExtras(),
++                    )
+                 }
+                 .onFailure {
+                     state = restore(FEED_DIDNT_LOAD)
+@@ -291,11 +296,50 @@ class EditionViewModel(
+         state = EditionUiState.Loading
+         scope.launch {
+             repository.loadCurrentMorningEdition()
+-                .onSuccess { edition -> state = EditionUiState.Ready(edition) }
++                .onSuccess { edition ->
++                    state = EditionUiState.Ready(edition, sameDayExtras = loadExtras())
++                }
+                 .onFailure { error -> state = mapFailure(error) }
+         }
+     }
+ 
++    fun newEditionNow() {
++        val current = state as? EditionUiState.Ready ?: return
++        if (current.generating) return
++        state = current.copy(generating = true, notice = null)
++        scope.launch {
++            repository.newEditionNow()
++                .onSuccess {
++                    val morning = repository.loadCurrentMorningEdition().getOrThrow()
++                    state = EditionUiState.Ready(morning, sameDayExtras = loadExtras())
++                }
++                .onFailure {
++                    val shown = state as? EditionUiState.Ready ?: current
++                    state = shown.copy(generating = false, notice = FEED_DIDNT_LOAD)
++                }
++        }
++    }
++
++    fun openExtra(id: String) {
++        val current = state as? EditionUiState.Ready ?: return
++        val extra = current.sameDayExtras.singleOrNull { it.id == id } ?: return
++        state = current.copy(edition = extra, taskSaved = false)
++    }
++
++    fun openMorning() {
++        val current = state as? EditionUiState.Ready ?: return
++        scope.launch {
++            repository.loadCurrentMorningEdition()
++                .onSuccess { morning ->
++                    state = current.copy(
++                        edition = morning,
++                        sameDayExtras = loadExtras(),
++                        taskSaved = false,
++                    )
++                }
++        }
++    }
++
+     fun retrySample() {
+         state = EditionUiState.Loading
+         scope.launch {
+@@ -338,7 +382,7 @@ class EditionViewModel(
+         }
+         val generation = ++typewriterGeneration
+         scope.launch {
+-            repository.updateMorningEdition(updated)
++            saveEdition(updated)
+                 .onSuccess { saved ->
+                     if (generation != typewriterGeneration) {
+                         onResult(false)
+@@ -378,7 +422,7 @@ class EditionViewModel(
+         state = previous.copy(edition = updated)
+         val rollback = previous
+         scope.launch {
+-            repository.updateMorningEdition(updated)
++            saveEdition(updated)
+                 .onSuccess { saved ->
+                     val current = state as? EditionUiState.Ready
+                     if (current != null && current.edition.tasks == updated.tasks) {
+@@ -394,6 +438,16 @@ class EditionViewModel(
+         }
+     }
+ 
++    private suspend fun loadExtras(): List<Edition> =
++        runCatching { repository.loadSameDayExtras() }.getOrDefault(emptyList())
++
++    private suspend fun saveEdition(edition: Edition): Result<Edition> =
++        if (edition.role == EditionRole.extra) {
++            repository.updateExtra(edition)
++        } else {
++            repository.updateMorningEdition(edition)
++        }
++
+     private fun calDavFieldsDirty(url: String, username: String, password: String): Boolean =
+         password.isNotEmpty() || url.trim() != calDavUrlBaseline || username.trim() != calDavUsernameBaseline
+ 
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+index ee97379..58c6618 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+@@ -382,6 +382,49 @@ class EditionGenerateStateTest {
+         assertEquals(1, repository.generateCalls)
+     }
+ 
++    @Test
++    fun newEditionNowLeavesTheMorningOnTheFrontPageAndTheListOpensTheExtra() {
++        val repository = FakeEditionRepository(finished)
++        val viewModel = viewModel(repository)
++
++        viewModel.newEditionNow()
++
++        val front = viewModel.state as EditionUiState.Ready
++        assertEquals(finished.id, front.edition.id)
++        assertEquals(EditionRole.morning, front.edition.role)
++        assertEquals(finished.briefing, front.edition.briefing)
++        val extra = front.sameDayExtras.single()
++        assertEquals(EditionRole.extra, extra.role)
++        assertEquals(extra.dayKey, finished.dayKey)
++        assertEquals(sameDayExtraLabel(extra), extra.stories.first().headline)
++
++        viewModel.openExtra(extra.id)
++        val opened = viewModel.state as EditionUiState.Ready
++        assertEquals(extra.id, opened.edition.id)
++        assertEquals(listOf(extra), opened.sameDayExtras)
++
++        viewModel.openMorning()
++        val back = viewModel.state as EditionUiState.Ready
++        assertEquals(finished.id, back.edition.id)
++        assertEquals(EditionRole.morning, back.edition.role)
++        assertTrue(back.sameDayExtras.all { it.dayKey == finished.dayKey })
++    }
++
++    @Test
++    fun failedNewEditionNowKeepsTheVisiblePaper() {
++        val repository = FakeEditionRepository(finished)
++        repository.failNewEdition = true
++        val viewModel = viewModel(repository)
++
++        viewModel.newEditionNow()
++
++        val ready = viewModel.state as EditionUiState.Ready
++        assertFalse(ready.generating)
++        assertEquals(finished.id, ready.edition.id)
++        assertEquals(FEED_DIDNT_LOAD, ready.notice)
++        assertTrue(ready.sameDayExtras.isEmpty())
++    }
++
+     private fun viewModel(repository: EditionRepository) =
+         EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
+ }
+@@ -485,5 +528,25 @@ private class FakeEditionRepository(
+         return runCatching { generate() }
+     }
+ 
++    private val extras = mutableListOf<Edition>()
++    var failNewEdition: Boolean = false
++
++    override suspend fun newEditionNow(): Result<Edition> {
++        if (failNewEdition) return Result.failure(IllegalStateException(FEED_DIDNT_LOAD))
++        val extra = edition.copy(
++            id = "extra-${edition.dayKey}-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Fresh extra",
++        )
++        extras += extra
++        return Result.success(extra)
++    }
++
++    override suspend fun loadSameDayExtras(): List<Edition> = extras.toList()
++
+     override suspend fun updateMorningEdition(edition: Edition): Result<Edition> = Result.success(edition)
++
++    override suspend fun updateExtra(edition: Edition): Result<Edition> =
++        Result.success(edition.copy(revision = edition.revision + 1))
+ }
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
+index 1bc52ab..74b6c4a 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
+@@ -122,6 +122,10 @@ class EditionTaskStateTest {
+ 
+         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
+ 
++        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
++
++        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
++
+         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
+             updateCalls += 1
+             val next = edition.copy(revision = morning.revision + 1)
+@@ -129,5 +133,7 @@ class EditionTaskStateTest {
+             lastSaved = next
+             return Result.success(next)
+         }
++
++        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
+     }
+ }
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
+index e241ccb..257690c 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
+@@ -172,6 +172,10 @@ class EditionTypewriterStateTest {
+ 
+         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
+ 
++        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
++
++        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
++
+         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
+             gate?.await()
+             if (fail) return Result.failure(IllegalStateException("save failed"))
+@@ -180,5 +184,7 @@ class EditionTypewriterStateTest {
+             morning = next
+             return Result.success(next)
+         }
++
++        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
+     }
+ }
+diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
+index 6a39885..ff3af60 100644
+--- a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
++++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
+@@ -21,6 +21,10 @@ actual class FileEditionStore actual constructor(
+ 
+     override fun saveMorningEdition(edition: Edition) = Unit
+ 
++    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
++
++    override fun saveExtra(edition: Edition) = Unit
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = null
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = Unit
+diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
+index f5f6de0..66e56d1 100644
+--- a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
++++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
+@@ -61,6 +61,25 @@ actual class FileEditionStore actual constructor(
+         file.writeText(encodeEdition(edition))
+     }
+ 
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
++        val dir = File(root, "editions")
++        dir.mkdirs()
++        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? {
+         val file = File(root, "calendar-snapshot.json")
+         if (!file.exists()) return null
+diff --git a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
+index 2e3f14b..d14b31e 100644
+--- a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
++++ b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
+@@ -3,11 +3,14 @@ package hu.toliver.matutine.store
+ import hu.toliver.matutine.edition.CalDavAccount
+ import hu.toliver.matutine.edition.CalendarOccurrence
+ import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.EditionRole
+ import hu.toliver.matutine.edition.LocalEditionRepository
+ import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SampleGenerator
+ import hu.toliver.matutine.edition.SourceList
+ import hu.toliver.matutine.edition.StoryRank
+ import kotlinx.coroutines.runBlocking
++import kotlinx.datetime.LocalDate
+ import java.io.File
+ import java.nio.file.Files
+ import kotlin.test.Test
+@@ -149,4 +152,33 @@ class FileEditionStoreTest {
+         assertTrue(File(dir, "calendar-snapshot.json").exists())
+         assertNull(store.readSourceList())
+     }
++
++    @Test
++    fun saveExtra_roundTripsWithoutRewritingTheMorningFile() {
++        val dir = Files.createTempDirectory("matutine-extra").toFile()
++        val store = FileEditionStore(dir.absolutePath)
++        val morning = SampleGenerator.forDay(LocalDate(2026, 11, 7)).copy(
++            id = "morning-2026-11-07",
++            isSample = false,
++            briefing = "Morning stays",
++        )
++        store.saveMorningEdition(morning)
++        val morningFile = File(dir, "editions/2026-11-07-morning.json")
++        val before = morningFile.readText()
++        val extra = morning.copy(
++            id = "extra-2026-11-07-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Extra",
++        )
++        val otherDay = extra.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06")
++
++        store.saveExtra(extra)
++        store.saveExtra(otherDay)
++
++        assertEquals(before, morningFile.readText())
++        assertEquals(listOf(extra), store.loadExtras("2026-11-07"))
++        assertEquals(listOf(otherDay), store.loadExtras("2026-11-06"))
++        assertEquals(morning, store.loadMorningEdition("2026-11-07"))
++    }
+ }
+diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
+index 6a39885..ff3af60 100644
+--- a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
++++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
+@@ -21,6 +21,10 @@ actual class FileEditionStore actual constructor(
+ 
+     override fun saveMorningEdition(edition: Edition) = Unit
+ 
++    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
++
++    override fun saveExtra(edition: Edition) = Unit
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = null
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = Unit
+diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+index d62d029..ccef4fb 100644
+--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
++++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+@@ -16,6 +16,8 @@ interface EditionStore {
+ 
+     fun loadMorningEdition(dayKey: String): Edition?
+     fun saveMorningEdition(edition: Edition)
++    fun loadExtras(dayKey: String): List<Edition>
++    fun saveExtra(edition: Edition)
+ 
+     /** Null when the snapshot document is absent. */
+     fun readCalendarSnapshot(): CalendarSnapshot?
+@@ -261,12 +263,85 @@ class LocalEditionRepository(
+         } else {
+             edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
+         }
++        val next = if (replaceExisting && existing != null && !existing.isSample) {
++            keepMorningEdits(existing, noted)
++        } else {
++            noted
++        }
+         if (!replaceExisting) {
+             val current = store.loadMorningEdition(date.toDayKey())
+             if (current != null) return current
+         }
+-        store.saveMorningEdition(noted)
+-        return noted
++        store.saveMorningEdition(next)
++        return next
++    }
++
++    override suspend fun newEditionNow(): Result<Edition> = runCatching {
++        val date = today()
++        val dayKey = date.toDayKey()
++        if (store.loadMorningEdition(dayKey) == null) error(MORNING_EDITION_NOT_READY)
++        val list = store.readSourceList()
++        if (list == null || list.feeds.isEmpty()) error(FEED_DIDNT_LOAD)
++        val parsed = list.feeds.map { url ->
++            runCatching { parseFeedDocument(readDocument(url)) }
++        }
++        if (parsed.none { it.isSuccess }) error(FEED_DIDNT_LOAD)
++        recordDeviceCalendar(date)
++        val calDav = readCalDavOccurrences()
++        val events = editionEventsFrom(
++            sources = listOf(
++                readIcsOccurrences(list),
++                store.readCalendarSnapshot()?.occurrences.orEmpty(),
++                calDav.occurrences,
++            ),
++            day = date,
++            zone = timeZone,
++        )
++        val edition = EditionGenerator.fromFeedResults(
++            date = date,
++            parsed = parsed,
++            existing = null,
++            topics = store.readSettings()?.topics.orEmpty(),
++            events = events,
++        )
++        val noted = if (calDav.notes.isEmpty()) {
++            edition
++        } else {
++            edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
++        }
++        val extra = noted.copy(
++            id = nextExtraId(dayKey),
++            revision = 1,
++            role = EditionRole.extra,
++        )
++        store.saveExtra(extra)
++        extra
++    }
++
++    override suspend fun loadSameDayExtras(): List<Edition> =
++        store.loadExtras(today().toDayKey())
++
++    override suspend fun updateExtra(edition: Edition): Result<Edition> = runCatching {
++        val dayKey = today().toDayKey()
++        require(edition.dayKey == dayKey && edition.role == EditionRole.extra)
++        val stored = store.loadExtras(dayKey).singleOrNull { it.id == edition.id }
++            ?: error(MORNING_EDITION_NOT_READY)
++        val next = edition.copy(
++            id = stored.id,
++            revision = stored.revision + 1,
++            dayKey = stored.dayKey,
++            role = EditionRole.extra,
++        )
++        store.saveExtra(next)
++        next
++    }
++
++    private fun nextExtraId(dayKey: String): String {
++        val prefix = "extra-$dayKey-"
++        val used = store.loadExtras(dayKey).mapNotNull { extra ->
++            extra.id.removePrefix(prefix).toIntOrNull()
++        }
++        return prefix + ((used.maxOrNull() ?: 0) + 1)
+     }
+ 
+     override fun savedCalDavAccount(): CalDavAccount? =
+@@ -353,5 +428,18 @@ interface EditionRepository {
+     fun savedEditionLayout(): String
+     fun saveEditionLayout(layoutId: String): Result<Unit>
+     suspend fun generateToday(): Result<Edition>
++    suspend fun newEditionNow(): Result<Edition>
++    suspend fun loadSameDayExtras(): List<Edition>
+     suspend fun updateMorningEdition(edition: Edition): Result<Edition>
++    suspend fun updateExtra(edition: Edition): Result<Edition>
++}
++
++internal fun keepMorningEdits(existing: Edition, generated: Edition): Edition {
++    val freshKeys = generated.events.map { it.title to it.timeLabel }.toSet()
++    val keptLines = existing.events.filter { (it.title to it.timeLabel) !in freshKeys }
++    return generated.copy(
++        briefing = existing.briefing,
++        events = generated.events + keptLines,
++        tasks = existing.tasks,
++    )
+ }
+diff --git a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+index 711cd09..682a20d 100644
+--- a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
++++ b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+@@ -43,6 +43,21 @@ private class FakeStore(
+         saved = edition
+     }
+ 
++    private val extras = mutableListOf<Edition>()
++
++    override fun loadExtras(dayKey: String): List<Edition> =
++        extras.filter { it.dayKey == dayKey && it.role == EditionRole.extra }
++
++    override fun saveExtra(edition: Edition) {
++        require(edition.role == EditionRole.extra)
++        val index = extras.indexOfFirst { it.id == edition.id }
++        if (index >= 0) extras[index] = edition else extras += edition
++    }
++
++    fun seedExtra(edition: Edition) {
++        extras += edition
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = snapshot
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
+@@ -428,6 +443,148 @@ class LocalEditionRepositoryTest {
+         assertFalse(edition.isSample)
+     }
+ 
++    @Test
++    fun generate_again_keepsBriefingRewrittenLineAndTasks() = runBlocking {
++        val existing = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 3,
++            isSample = false,
++            briefing = "Kept by hand",
++            events = listOf(
++                EditionEvent("Planning", "9:30"),
++                EditionEvent("Team standup", "10:00"),
++            ),
++            tasks = listOf(
++                EditionTask("Buy milk", completed = true),
++                EditionTask("File notes"),
++            ),
++        )
++        val store = FakeStore(
++            morning = existing,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++            snapshot = CalendarSnapshot(
++                occurrences = listOf(
++                    CalendarOccurrence(title = "Planning", startEpochSecond = utc(9, 30), endEpochSecond = utc(10, 0)),
++                ),
++            ),
++        )
++        val repo = LocalEditionRepository(
++            store,
++            today = { sampleDay },
++            timeZone = TimeZone.UTC,
++            readFeed = { PERSONAL_RSS },
++        )
++
++        val edition = repo.generateToday().getOrThrow()
++
++        assertEquals(existing.id, edition.id)
++        assertEquals(4, edition.revision)
++        assertEquals("Kept by hand", edition.briefing)
++        assertEquals(existing.tasks, edition.tasks)
++        assertTrue(edition.events.contains(EditionEvent("Planning", "9:30")))
++        assertTrue(edition.events.contains(EditionEvent("Team standup", "10:00")))
++        assertEquals(1, edition.events.count { it == EditionEvent("Planning", "9:30") })
++        assertEquals(
++            listOf("Markets open higher", "Port reopens"),
++            edition.stories.map { it.headline },
++        )
++        assertEquals(StoryRank.lead, edition.stories.first().rank)
++    }
++
++    @Test
++    fun newEditionNow_insertsAnExtraAndLeavesOtherEditions() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 2,
++            isSample = false,
++            briefing = "Morning stays",
++        )
++        val older = morning.copy(
++            id = "extra-${sampleDay.toDayKey()}-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Older extra",
++        )
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++        )
++        store.seedExtra(older)
++        store.seedExtra(older.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06", briefing = "Yesterday"))
++        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })
++
++        val extra = repo.newEditionNow().getOrThrow()
++
++        assertEquals(EditionRole.extra, extra.role)
++        assertEquals("extra-${sampleDay.toDayKey()}-2", extra.id)
++        assertEquals(1, extra.revision)
++        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertEquals(older, store.loadExtras(sampleDay.toDayKey()).first())
++        assertEquals(morning, repo.loadCurrentMorningEdition().getOrThrow())
++        assertEquals(listOf(older, extra), repo.loadSameDayExtras())
++        assertTrue(repo.loadSameDayExtras().none { it.dayKey != sampleDay.toDayKey() })
++    }
++
++    @Test
++    fun lateSnapshot_isOnRegenerateOrExtraAndNotOnThePaperBefore() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 1,
++            isSample = false,
++            events = listOf(EditionEvent("Kept", "8:00")),
++        )
++        val later = CalendarOccurrence(title = "Later", startEpochSecond = utc(15, 0), endEpochSecond = utc(16, 0))
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++            snapshot = CalendarSnapshot(occurrences = listOf(later)),
++        )
++        val repo = LocalEditionRepository(
++            store,
++            today = { sampleDay },
++            timeZone = TimeZone.UTC,
++            readFeed = { PERSONAL_RSS },
++        )
++
++        assertEquals(listOf(EditionEvent("Kept", "8:00")), repo.loadCurrentMorningEdition().getOrThrow().events)
++
++        val regenerated = repo.generateToday().getOrThrow()
++        assertTrue(regenerated.events.contains(EditionEvent("Later", "15:00")))
++        assertEquals(morning.id, regenerated.id)
++
++        val untouched = regenerated.copy(events = listOf(EditionEvent("Kept", "8:00")), revision = regenerated.revision)
++        store.saveMorningEdition(untouched)
++        val extra = repo.newEditionNow().getOrThrow()
++        assertEquals(untouched, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertTrue(extra.events.contains(EditionEvent("Later", "15:00")))
++        assertFalse(extra.events.contains(EditionEvent("Kept", "8:00")))
++    }
++
++    @Test
++    fun failedGenerateAndExtra_leaveStoredEditions() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 2,
++            isSample = false,
++            briefing = "Still here",
++        )
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/bad.xml")),
++        )
++        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { NOT_A_FEED })
++
++        val generated = repo.generateToday()
++        val extra = repo.newEditionNow()
++
++        assertTrue(generated.isFailure)
++        assertEquals(FEED_DIDNT_LOAD, generated.exceptionOrNull()?.message)
++        assertTrue(extra.isFailure)
++        assertEquals(FEED_DIDNT_LOAD, extra.exceptionOrNull()?.message)
++        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertTrue(store.loadExtras(sampleDay.toDayKey()).isEmpty())
++    }
++
+     @Test
+     fun saveTopics_storesChoicesLinesAndLeavesTheSourceList() = runBlocking {
+         val list = SourceList(
+diff --git a/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
+new file mode 100644
+index 0000000..7085344
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
+@@ -0,0 +1,82 @@
++---
++title: '2-6 Regenerate without losing the edits'
++type: 'feature'
++created: '2026-11-10'
++status: 'in-review'
++route: 'dispatch'
++review_loop_iteration: 0
++story_key: '2-6-regenerate-without-losing-the-edits'
++baseline_commit: 'fd8aa8ffad6e0a644fdeeee090c499ae251bbb5f'
++context:
++  - _bmad-output/implementation-artifacts/epic-2-context.md
++---
++
++<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
++
++## Intent
++
++**Problem:** Generating today's edition again replaces the morning body, so a rewritten briefing, a rewritten calendar line, and a checked task disappear. There is no same-day extra. A device-calendar snapshot saved after the paper exists never reaches a later paper.
++
++**Approach:** Regenerate keeps the morning id and those three edits, and may refresh stories, rank, and unedited events. New edition now inserts one extra and leaves every other edition untouched. The front page stays today's morning edition. Today's extras open from a same-day list. The late snapshot is included only on that regenerate or extra.
++
++## Boundaries & Constraints
++
++**Always:** Keep the morning id, the briefing already on that document, and every existing task (completed or added). Keep a calendar line whose title and time are not both on the fresh event list, and include every fresh event. Stories and rank come from the new sources. The extra is a new `extra` document with a new id and revision 1. Morning and earlier extras stay unchanged. The list is today's extras only. No revision number is drawn. Tray items, Settings' one primary Generate, and morning insert-if-absent stay. A failed command leaves stored editions unchanged.
++
++**Never:** A merge for a disappeared source item. A merge screen. A past-day archive. A tray item. A server upload. An API key.
++
++## I/O & Edge-Case Matrix
++
++| Scenario | Input / State | Expected Output / Behavior | Error Handling |
++|----------|--------------|---------------------------|----------------|
++| Regenerate with edits | Personal morning: rewritten briefing, a calendar line that differs in title or time, a checked task, an added task | Same id, revision + 1, briefing and tasks unchanged, rewritten line present, fresh stories, ranks, and events included | N/A |
++| New edition now | Morning exists, maybe one extra | New same-day extra; morning and older extra unchanged; current load is still the morning | N/A |
++| Late snapshot | Snapshot saved after the morning paper; that paper does not yet include it | Those events are on the regenerated morning edition or the new extra, not written at snapshot time | N/A |
++| Failed command | Feed read fails | Stored editions unchanged | "That feed didn't load." |
++| Same-day list | Two extras today and an older day | List offers the two extras only; opening one shows it; Today's morning edition returns to the front page | N/A |
++
++</frozen-after-approval>
++
++## Code Map
++
++- `core/.../edition/EditionGenerator.kt` — fresh stories, rank, events, empty tasks. Reuse. Do not teach it extras.
++- `core/.../edition/EditionRepository.kt` — after `fromFeedResults`, overlay the keeps when the stored morning edition is personal. Add `newEditionNow`, `loadSameDayExtras`, and an extra save that does not touch other ids. `recordDeviceCalendar` still leaves the paper alone. `runMorning` stays insert-if-absent.
++- `EditionStore` — add `loadExtras(dayKey)` and `saveExtra`. Morning files stay `editions/{dayKey}-morning.json`.
++- `LocalEditionRepositoryTest.kt` `FakeStore` — in-memory extras. `by inner` delegates the new methods.
++- `FileEditionStore.jvm.kt` and `FileEditionStore.android.kt` — extras at `editions/extra-{dayKey}-{n}.json`. JS and wasm stubs compile.
++- `EditionViewModel.kt` — `generate()` still regenerates and shows the morning result. `newEditionNow`, `openExtra`, and `openMorning`. Task and typewriter saves follow the document on screen.
++- `EditionScreen.kt` and `MatutineApp.kt` — "New edition now" beside Settings, then the same-day list. `main.kt` tray stays three items.
++
++## Tasks & Acceptance
++
++**Execution:**
++- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` — keeps on regenerate; insert and list extras
++- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` — I/O rows for keeps, isolation, late snapshot, and failure
++- [x] `app/shared/src/jvmMain/.../FileEditionStore.jvm.kt` and `androidMain/.../FileEditionStore.android.kt` — persist extras; JS and wasm stubs
++- [x] `app/shared/src/jvmTest/.../FileEditionStoreTest.kt` — extra round-trip does not rewrite the morning file
++- [x] `EditionViewModel.kt`, `EditionScreen.kt`, `MatutineApp.kt` — button, list, open extra, return to morning
++- [x] `app/shared/src/commonTest/.../EditionGenerateStateTest.kt` — insert leaves morning on the front page; the list opens the extra
++
++**Acceptance Criteria:**
++- Given a rewritten briefing, a rewritten calendar line, and a checked task, when O generates today's morning edition again, then the id, briefing, line, and checked task remain, and stories and rank may change.
++- Given O asks for a new edition now, when it finishes, then a same-day extra exists, other editions are untouched, the front page is still today's morning edition, and the extra opens from a same-day list with no past days, no revision number, and no merge screen.
++- Given a device-calendar snapshot saved after today's morning edition exists, when O regenerates or asks for a new edition now, then those events are on that result and were not on the paper before the command.
++
++## Implementation Notes
++
++- Regenerate of a personal morning edition keeps its briefing and tasks, and appends calendar lines whose title and time are not both on the fresh event list. Stories and rank come from the new sources. A sample edition still becomes the personal paper.
++- New edition now writes `editions/extra-{dayKey}-{n}.json` and does not save the morning file. The edition screen lists today's extras by first headline, or Extra, and returns with Today's morning edition. The tray menu is unchanged.
++- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` — BUILD SUCCESSFUL. No emulator and no commit.
++
++## Spec Change Log
++
++## Review Triage Log
++
++## Design Notes
++
++Fresh events stay in source order. A morning calendar line is appended only when its title and time are not both on that list. Tasks are copied as they stand. The extra does not receive those edits. A list row is the extra's first headline, or Extra. "Today's morning edition" is the return row while an extra is open.
++
++## Verification
++
++**Commands:**
++- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
+
+Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
diff --git a/_bmad-output/implementation-artifacts/review-2-6-edge-case-hunter-prompt.md b/_bmad-output/implementation-artifacts/review-2-6-edge-case-hunter-prompt.md
new file mode 100644
index 0000000..9c2e0b6
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/review-2-6-edge-case-hunter-prompt.md
@@ -0,0 +1,1088 @@
+Read the following completely and follow it as your review instructions.
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
+claims_file (leave unread until your instructions call for it):
+
+---
+title: '2-6 Regenerate without losing the edits'
+type: 'feature'
+created: '2026-11-10'
+status: 'in-review'
+route: 'dispatch'
+review_loop_iteration: 0
+story_key: '2-6-regenerate-without-losing-the-edits'
+baseline_commit: 'fd8aa8ffad6e0a644fdeeee090c499ae251bbb5f'
+context:
+  - _bmad-output/implementation-artifacts/epic-2-context.md
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** Generating today's edition again replaces the morning body, so a rewritten briefing, a rewritten calendar line, and a checked task disappear. There is no same-day extra. A device-calendar snapshot saved after the paper exists never reaches a later paper.
+
+**Approach:** Regenerate keeps the morning id and those three edits, and may refresh stories, rank, and unedited events. New edition now inserts one extra and leaves every other edition untouched. The front page stays today's morning edition. Today's extras open from a same-day list. The late snapshot is included only on that regenerate or extra.
+
+## Boundaries & Constraints
+
+**Always:** Keep the morning id, the briefing already on that document, and every existing task (completed or added). Keep a calendar line whose title and time are not both on the fresh event list, and include every fresh event. Stories and rank come from the new sources. The extra is a new `extra` document with a new id and revision 1. Morning and earlier extras stay unchanged. The list is today's extras only. No revision number is drawn. Tray items, Settings' one primary Generate, and morning insert-if-absent stay. A failed command leaves stored editions unchanged.
+
+**Never:** A merge for a disappeared source item. A merge screen. A past-day archive. A tray item. A server upload. An API key.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Regenerate with edits | Personal morning: rewritten briefing, a calendar line that differs in title or time, a checked task, an added task | Same id, revision + 1, briefing and tasks unchanged, rewritten line present, fresh stories, ranks, and events included | N/A |
+| New edition now | Morning exists, maybe one extra | New same-day extra; morning and older extra unchanged; current load is still the morning | N/A |
+| Late snapshot | Snapshot saved after the morning paper; that paper does not yet include it | Those events are on the regenerated morning edition or the new extra, not written at snapshot time | N/A |
+| Failed command | Feed read fails | Stored editions unchanged | "That feed didn't load." |
+| Same-day list | Two extras today and an older day | List offers the two extras only; opening one shows it; Today's morning edition returns to the front page | N/A |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `core/.../edition/EditionGenerator.kt` — fresh stories, rank, events, empty tasks. Reuse. Do not teach it extras.
+- `core/.../edition/EditionRepository.kt` — after `fromFeedResults`, overlay the keeps when the stored morning edition is personal. Add `newEditionNow`, `loadSameDayExtras`, and an extra save that does not touch other ids. `recordDeviceCalendar` still leaves the paper alone. `runMorning` stays insert-if-absent.
+- `EditionStore` — add `loadExtras(dayKey)` and `saveExtra`. Morning files stay `editions/{dayKey}-morning.json`.
+- `LocalEditionRepositoryTest.kt` `FakeStore` — in-memory extras. `by inner` delegates the new methods.
+- `FileEditionStore.jvm.kt` and `FileEditionStore.android.kt` — extras at `editions/extra-{dayKey}-{n}.json`. JS and wasm stubs compile.
+- `EditionViewModel.kt` — `generate()` still regenerates and shows the morning result. `newEditionNow`, `openExtra`, and `openMorning`. Task and typewriter saves follow the document on screen.
+- `EditionScreen.kt` and `MatutineApp.kt` — "New edition now" beside Settings, then the same-day list. `main.kt` tray stays three items.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` — keeps on regenerate; insert and list extras
+- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` — I/O rows for keeps, isolation, late snapshot, and failure
+- [x] `app/shared/src/jvmMain/.../FileEditionStore.jvm.kt` and `androidMain/.../FileEditionStore.android.kt` — persist extras; JS and wasm stubs
+- [x] `app/shared/src/jvmTest/.../FileEditionStoreTest.kt` — extra round-trip does not rewrite the morning file
+- [x] `EditionViewModel.kt`, `EditionScreen.kt`, `MatutineApp.kt` — button, list, open extra, return to morning
+- [x] `app/shared/src/commonTest/.../EditionGenerateStateTest.kt` — insert leaves morning on the front page; the list opens the extra
+
+**Acceptance Criteria:**
+- Given a rewritten briefing, a rewritten calendar line, and a checked task, when O generates today's morning edition again, then the id, briefing, line, and checked task remain, and stories and rank may change.
+- Given O asks for a new edition now, when it finishes, then a same-day extra exists, other editions are untouched, the front page is still today's morning edition, and the extra opens from a same-day list with no past days, no revision number, and no merge screen.
+- Given a device-calendar snapshot saved after today's morning edition exists, when O regenerates or asks for a new edition now, then those events are on that result and were not on the paper before the command.
+
+## Implementation Notes
+
+- Regenerate of a personal morning edition keeps its briefing and tasks, and appends calendar lines whose title and time are not both on the fresh event list. Stories and rank come from the new sources. A sample edition still becomes the personal paper.
+- New edition now writes `editions/extra-{dayKey}-{n}.json` and does not save the morning file. The edition screen lists today's extras by first headline, or Extra, and returns with Today's morning edition. The tray menu is unchanged.
+- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` — BUILD SUCCESSFUL. No emulator and no commit.
+
+## Spec Change Log
+
+## Review Triage Log
+
+## Design Notes
+
+Fresh events stay in source order. A morning calendar line is appended only when its title and time are not both on that list. Tasks are copied as they stand. The extra does not receive those edits. A list row is the extra's first headline, or Extra. "Today's morning edition" is the return row while an extra is open.
+
+## Verification
+
+**Commands:**
+- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
+
+Review content: the unified diff at the following contents. Read that file — it is the content under review.
+
+diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
+index ceed8f0..08ea20d 100644
+--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
++++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
+@@ -29,7 +29,7 @@
+ # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
+ # - Retrospective appends its action items to action_items; the status view surfaces open ones
+ generated: 11-07-2026 19:42
+-last_updated: 11-10-2026 08:37
++last_updated: 11-10-2026 18:11
+ project: Matutine
+ project_key: NOKEY
+ tracking_system: file-system
+@@ -55,7 +55,7 @@ development_status:
+   2-3-customize-the-android-paper: done
+   2-4-use-the-desktop-tray: done
+   2-5-rewrite-the-edition-on-the-typewriter: done
+-  2-6-regenerate-without-losing-the-edits: backlog
++  2-6-regenerate-without-losing-the-edits: in-progress
+   2-7-write-the-briefing-with-a-gemini-key: backlog
+   epic-2-retrospective: optional
+ 
+diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
+index 3f7b6cd..2fcca9e 100644
+--- a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
++++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
+@@ -62,6 +62,25 @@ actual class FileEditionStore actual constructor(
+         file.writeText(encodeEdition(edition))
+     }
+ 
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
++        val dir = File(root, "editions")
++        dir.mkdirs()
++        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? {
+         val file = File(root, "calendar-snapshot.json")
+         if (!file.exists()) return null
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+index 7c6b5dc..7895a75 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+@@ -167,6 +167,10 @@ fun MatutineApp(
+                 calendarPermissionOff = calendarPermissionOff,
+                 onOpenCalendarPermission = ::openCalendarPermissionSettings,
+                 editionLayoutId = viewModel.editionLayout,
++                sameDayExtras = ready?.sameDayExtras.orEmpty(),
++                onNewEditionNow = viewModel::newEditionNow,
++                onOpenExtra = viewModel::openExtra,
++                onOpenMorning = viewModel::openMorning,
+                 onOpenTypewriter = if (showsTypewriter() && ready != null) {
+                     {
+                         typewriterDraft = EditionTranscript.render(ready.edition)
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
+index d4c7314..ac42e6f 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
+@@ -13,3 +13,6 @@ internal object EditionCopy {
+ 
+ internal fun showsNoTopicLine(isSample: Boolean, topics: List<String>): Boolean =
+     !isSample && topics.none { it.isNotBlank() }
++
++internal fun sameDayExtraLabel(edition: Edition): String =
++    edition.stories.firstOrNull()?.headline?.takeIf { it.isNotBlank() } ?: "Extra"
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+index 0d36849..c06cc9b 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+@@ -68,6 +68,10 @@ fun EditionScreen(
+     onOpenCalendarPermission: () -> Unit = {},
+     editionLayoutId: String = DEFAULT_EDITION_LAYOUT,
+     onOpenTypewriter: (() -> Unit)? = null,
++    sameDayExtras: List<Edition> = emptyList(),
++    onNewEditionNow: () -> Unit = {},
++    onOpenExtra: (String) -> Unit = {},
++    onOpenMorning: () -> Unit = {},
+     modifier: Modifier = Modifier,
+ ) {
+     val typography = LocalMatutineEditionTypography.current
+@@ -96,6 +100,10 @@ fun EditionScreen(
+             onOpenCalendarPermission = onOpenCalendarPermission,
+             editionLayoutId = editionLayoutId,
+             onOpenTypewriter = onOpenTypewriter,
++            sameDayExtras = sameDayExtras,
++            onNewEditionNow = onNewEditionNow,
++            onOpenExtra = onOpenExtra,
++            onOpenMorning = onOpenMorning,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -217,6 +225,10 @@ private fun EditionContent(
+     onOpenCalendarPermission: () -> Unit,
+     editionLayoutId: String,
+     onOpenTypewriter: (() -> Unit)?,
++    sameDayExtras: List<Edition>,
++    onNewEditionNow: () -> Unit,
++    onOpenExtra: (String) -> Unit,
++    onOpenMorning: () -> Unit,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -254,6 +266,34 @@ private fun EditionContent(
+                         Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                     }
+                 }
++                TextButton(
++                    onClick = onNewEditionNow,
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                }
++            }
++            if (edition.role == EditionRole.extra) {
++                TextButton(
++                    onClick = onOpenMorning,
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(
++                        text = "Today's morning edition",
++                        style = typography.body.copy(color = EditionThemeColors.current.ink),
++                    )
++                }
++            }
++            sameDayExtras.forEach { extra ->
++                TextButton(
++                    onClick = { onOpenExtra(extra.id) },
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(
++                        text = sameDayExtraLabel(extra),
++                        style = typography.body.copy(color = EditionThemeColors.current.ink),
++                    )
++                }
+             }
+             Masthead(edition.mastheadName, edition.dateLine, typography)
+             if (generating) {
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+index acbabcc..fa54a3b 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+@@ -17,6 +17,7 @@ sealed interface EditionUiState {
+         val generating: Boolean = false,
+         val notice: String? = null,
+         val taskSaved: Boolean = false,
++        val sameDayExtras: List<Edition> = emptyList(),
+     ) : EditionUiState
+ 
+     data class SampleLoadFailed(val message: String = "The sample edition didn't load.") : EditionUiState
+@@ -279,7 +280,11 @@ class EditionViewModel(
+             repository.generateToday()
+                 .onSuccess { edition ->
+                     feedFieldError = null
+-                    state = EditionUiState.Ready(edition, notice = notice)
++                    state = EditionUiState.Ready(
++                        edition,
++                        notice = notice,
++                        sameDayExtras = loadExtras(),
++                    )
+                 }
+                 .onFailure {
+                     state = restore(FEED_DIDNT_LOAD)
+@@ -291,11 +296,50 @@ class EditionViewModel(
+         state = EditionUiState.Loading
+         scope.launch {
+             repository.loadCurrentMorningEdition()
+-                .onSuccess { edition -> state = EditionUiState.Ready(edition) }
++                .onSuccess { edition ->
++                    state = EditionUiState.Ready(edition, sameDayExtras = loadExtras())
++                }
+                 .onFailure { error -> state = mapFailure(error) }
+         }
+     }
+ 
++    fun newEditionNow() {
++        val current = state as? EditionUiState.Ready ?: return
++        if (current.generating) return
++        state = current.copy(generating = true, notice = null)
++        scope.launch {
++            repository.newEditionNow()
++                .onSuccess {
++                    val morning = repository.loadCurrentMorningEdition().getOrThrow()
++                    state = EditionUiState.Ready(morning, sameDayExtras = loadExtras())
++                }
++                .onFailure {
++                    val shown = state as? EditionUiState.Ready ?: current
++                    state = shown.copy(generating = false, notice = FEED_DIDNT_LOAD)
++                }
++        }
++    }
++
++    fun openExtra(id: String) {
++        val current = state as? EditionUiState.Ready ?: return
++        val extra = current.sameDayExtras.singleOrNull { it.id == id } ?: return
++        state = current.copy(edition = extra, taskSaved = false)
++    }
++
++    fun openMorning() {
++        val current = state as? EditionUiState.Ready ?: return
++        scope.launch {
++            repository.loadCurrentMorningEdition()
++                .onSuccess { morning ->
++                    state = current.copy(
++                        edition = morning,
++                        sameDayExtras = loadExtras(),
++                        taskSaved = false,
++                    )
++                }
++        }
++    }
++
+     fun retrySample() {
+         state = EditionUiState.Loading
+         scope.launch {
+@@ -338,7 +382,7 @@ class EditionViewModel(
+         }
+         val generation = ++typewriterGeneration
+         scope.launch {
+-            repository.updateMorningEdition(updated)
++            saveEdition(updated)
+                 .onSuccess { saved ->
+                     if (generation != typewriterGeneration) {
+                         onResult(false)
+@@ -378,7 +422,7 @@ class EditionViewModel(
+         state = previous.copy(edition = updated)
+         val rollback = previous
+         scope.launch {
+-            repository.updateMorningEdition(updated)
++            saveEdition(updated)
+                 .onSuccess { saved ->
+                     val current = state as? EditionUiState.Ready
+                     if (current != null && current.edition.tasks == updated.tasks) {
+@@ -394,6 +438,16 @@ class EditionViewModel(
+         }
+     }
+ 
++    private suspend fun loadExtras(): List<Edition> =
++        runCatching { repository.loadSameDayExtras() }.getOrDefault(emptyList())
++
++    private suspend fun saveEdition(edition: Edition): Result<Edition> =
++        if (edition.role == EditionRole.extra) {
++            repository.updateExtra(edition)
++        } else {
++            repository.updateMorningEdition(edition)
++        }
++
+     private fun calDavFieldsDirty(url: String, username: String, password: String): Boolean =
+         password.isNotEmpty() || url.trim() != calDavUrlBaseline || username.trim() != calDavUsernameBaseline
+ 
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+index ee97379..58c6618 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+@@ -382,6 +382,49 @@ class EditionGenerateStateTest {
+         assertEquals(1, repository.generateCalls)
+     }
+ 
++    @Test
++    fun newEditionNowLeavesTheMorningOnTheFrontPageAndTheListOpensTheExtra() {
++        val repository = FakeEditionRepository(finished)
++        val viewModel = viewModel(repository)
++
++        viewModel.newEditionNow()
++
++        val front = viewModel.state as EditionUiState.Ready
++        assertEquals(finished.id, front.edition.id)
++        assertEquals(EditionRole.morning, front.edition.role)
++        assertEquals(finished.briefing, front.edition.briefing)
++        val extra = front.sameDayExtras.single()
++        assertEquals(EditionRole.extra, extra.role)
++        assertEquals(extra.dayKey, finished.dayKey)
++        assertEquals(sameDayExtraLabel(extra), extra.stories.first().headline)
++
++        viewModel.openExtra(extra.id)
++        val opened = viewModel.state as EditionUiState.Ready
++        assertEquals(extra.id, opened.edition.id)
++        assertEquals(listOf(extra), opened.sameDayExtras)
++
++        viewModel.openMorning()
++        val back = viewModel.state as EditionUiState.Ready
++        assertEquals(finished.id, back.edition.id)
++        assertEquals(EditionRole.morning, back.edition.role)
++        assertTrue(back.sameDayExtras.all { it.dayKey == finished.dayKey })
++    }
++
++    @Test
++    fun failedNewEditionNowKeepsTheVisiblePaper() {
++        val repository = FakeEditionRepository(finished)
++        repository.failNewEdition = true
++        val viewModel = viewModel(repository)
++
++        viewModel.newEditionNow()
++
++        val ready = viewModel.state as EditionUiState.Ready
++        assertFalse(ready.generating)
++        assertEquals(finished.id, ready.edition.id)
++        assertEquals(FEED_DIDNT_LOAD, ready.notice)
++        assertTrue(ready.sameDayExtras.isEmpty())
++    }
++
+     private fun viewModel(repository: EditionRepository) =
+         EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
+ }
+@@ -485,5 +528,25 @@ private class FakeEditionRepository(
+         return runCatching { generate() }
+     }
+ 
++    private val extras = mutableListOf<Edition>()
++    var failNewEdition: Boolean = false
++
++    override suspend fun newEditionNow(): Result<Edition> {
++        if (failNewEdition) return Result.failure(IllegalStateException(FEED_DIDNT_LOAD))
++        val extra = edition.copy(
++            id = "extra-${edition.dayKey}-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Fresh extra",
++        )
++        extras += extra
++        return Result.success(extra)
++    }
++
++    override suspend fun loadSameDayExtras(): List<Edition> = extras.toList()
++
+     override suspend fun updateMorningEdition(edition: Edition): Result<Edition> = Result.success(edition)
++
++    override suspend fun updateExtra(edition: Edition): Result<Edition> =
++        Result.success(edition.copy(revision = edition.revision + 1))
+ }
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
+index 1bc52ab..74b6c4a 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
+@@ -122,6 +122,10 @@ class EditionTaskStateTest {
+ 
+         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
+ 
++        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
++
++        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
++
+         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
+             updateCalls += 1
+             val next = edition.copy(revision = morning.revision + 1)
+@@ -129,5 +133,7 @@ class EditionTaskStateTest {
+             lastSaved = next
+             return Result.success(next)
+         }
++
++        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
+     }
+ }
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
+index e241ccb..257690c 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
+@@ -172,6 +172,10 @@ class EditionTypewriterStateTest {
+ 
+         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
+ 
++        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
++
++        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
++
+         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
+             gate?.await()
+             if (fail) return Result.failure(IllegalStateException("save failed"))
+@@ -180,5 +184,7 @@ class EditionTypewriterStateTest {
+             morning = next
+             return Result.success(next)
+         }
++
++        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
+     }
+ }
+diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
+index 6a39885..ff3af60 100644
+--- a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
++++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
+@@ -21,6 +21,10 @@ actual class FileEditionStore actual constructor(
+ 
+     override fun saveMorningEdition(edition: Edition) = Unit
+ 
++    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
++
++    override fun saveExtra(edition: Edition) = Unit
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = null
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = Unit
+diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
+index f5f6de0..66e56d1 100644
+--- a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
++++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
+@@ -61,6 +61,25 @@ actual class FileEditionStore actual constructor(
+         file.writeText(encodeEdition(edition))
+     }
+ 
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
++        val dir = File(root, "editions")
++        dir.mkdirs()
++        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? {
+         val file = File(root, "calendar-snapshot.json")
+         if (!file.exists()) return null
+diff --git a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
+index 2e3f14b..d14b31e 100644
+--- a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
++++ b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
+@@ -3,11 +3,14 @@ package hu.toliver.matutine.store
+ import hu.toliver.matutine.edition.CalDavAccount
+ import hu.toliver.matutine.edition.CalendarOccurrence
+ import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.EditionRole
+ import hu.toliver.matutine.edition.LocalEditionRepository
+ import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SampleGenerator
+ import hu.toliver.matutine.edition.SourceList
+ import hu.toliver.matutine.edition.StoryRank
+ import kotlinx.coroutines.runBlocking
++import kotlinx.datetime.LocalDate
+ import java.io.File
+ import java.nio.file.Files
+ import kotlin.test.Test
+@@ -149,4 +152,33 @@ class FileEditionStoreTest {
+         assertTrue(File(dir, "calendar-snapshot.json").exists())
+         assertNull(store.readSourceList())
+     }
++
++    @Test
++    fun saveExtra_roundTripsWithoutRewritingTheMorningFile() {
++        val dir = Files.createTempDirectory("matutine-extra").toFile()
++        val store = FileEditionStore(dir.absolutePath)
++        val morning = SampleGenerator.forDay(LocalDate(2026, 11, 7)).copy(
++            id = "morning-2026-11-07",
++            isSample = false,
++            briefing = "Morning stays",
++        )
++        store.saveMorningEdition(morning)
++        val morningFile = File(dir, "editions/2026-11-07-morning.json")
++        val before = morningFile.readText()
++        val extra = morning.copy(
++            id = "extra-2026-11-07-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Extra",
++        )
++        val otherDay = extra.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06")
++
++        store.saveExtra(extra)
++        store.saveExtra(otherDay)
++
++        assertEquals(before, morningFile.readText())
++        assertEquals(listOf(extra), store.loadExtras("2026-11-07"))
++        assertEquals(listOf(otherDay), store.loadExtras("2026-11-06"))
++        assertEquals(morning, store.loadMorningEdition("2026-11-07"))
++    }
+ }
+diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
+index 6a39885..ff3af60 100644
+--- a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
++++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
+@@ -21,6 +21,10 @@ actual class FileEditionStore actual constructor(
+ 
+     override fun saveMorningEdition(edition: Edition) = Unit
+ 
++    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
++
++    override fun saveExtra(edition: Edition) = Unit
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = null
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = Unit
+diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+index d62d029..ccef4fb 100644
+--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
++++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+@@ -16,6 +16,8 @@ interface EditionStore {
+ 
+     fun loadMorningEdition(dayKey: String): Edition?
+     fun saveMorningEdition(edition: Edition)
++    fun loadExtras(dayKey: String): List<Edition>
++    fun saveExtra(edition: Edition)
+ 
+     /** Null when the snapshot document is absent. */
+     fun readCalendarSnapshot(): CalendarSnapshot?
+@@ -261,12 +263,85 @@ class LocalEditionRepository(
+         } else {
+             edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
+         }
++        val next = if (replaceExisting && existing != null && !existing.isSample) {
++            keepMorningEdits(existing, noted)
++        } else {
++            noted
++        }
+         if (!replaceExisting) {
+             val current = store.loadMorningEdition(date.toDayKey())
+             if (current != null) return current
+         }
+-        store.saveMorningEdition(noted)
+-        return noted
++        store.saveMorningEdition(next)
++        return next
++    }
++
++    override suspend fun newEditionNow(): Result<Edition> = runCatching {
++        val date = today()
++        val dayKey = date.toDayKey()
++        if (store.loadMorningEdition(dayKey) == null) error(MORNING_EDITION_NOT_READY)
++        val list = store.readSourceList()
++        if (list == null || list.feeds.isEmpty()) error(FEED_DIDNT_LOAD)
++        val parsed = list.feeds.map { url ->
++            runCatching { parseFeedDocument(readDocument(url)) }
++        }
++        if (parsed.none { it.isSuccess }) error(FEED_DIDNT_LOAD)
++        recordDeviceCalendar(date)
++        val calDav = readCalDavOccurrences()
++        val events = editionEventsFrom(
++            sources = listOf(
++                readIcsOccurrences(list),
++                store.readCalendarSnapshot()?.occurrences.orEmpty(),
++                calDav.occurrences,
++            ),
++            day = date,
++            zone = timeZone,
++        )
++        val edition = EditionGenerator.fromFeedResults(
++            date = date,
++            parsed = parsed,
++            existing = null,
++            topics = store.readSettings()?.topics.orEmpty(),
++            events = events,
++        )
++        val noted = if (calDav.notes.isEmpty()) {
++            edition
++        } else {
++            edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
++        }
++        val extra = noted.copy(
++            id = nextExtraId(dayKey),
++            revision = 1,
++            role = EditionRole.extra,
++        )
++        store.saveExtra(extra)
++        extra
++    }
++
++    override suspend fun loadSameDayExtras(): List<Edition> =
++        store.loadExtras(today().toDayKey())
++
++    override suspend fun updateExtra(edition: Edition): Result<Edition> = runCatching {
++        val dayKey = today().toDayKey()
++        require(edition.dayKey == dayKey && edition.role == EditionRole.extra)
++        val stored = store.loadExtras(dayKey).singleOrNull { it.id == edition.id }
++            ?: error(MORNING_EDITION_NOT_READY)
++        val next = edition.copy(
++            id = stored.id,
++            revision = stored.revision + 1,
++            dayKey = stored.dayKey,
++            role = EditionRole.extra,
++        )
++        store.saveExtra(next)
++        next
++    }
++
++    private fun nextExtraId(dayKey: String): String {
++        val prefix = "extra-$dayKey-"
++        val used = store.loadExtras(dayKey).mapNotNull { extra ->
++            extra.id.removePrefix(prefix).toIntOrNull()
++        }
++        return prefix + ((used.maxOrNull() ?: 0) + 1)
+     }
+ 
+     override fun savedCalDavAccount(): CalDavAccount? =
+@@ -353,5 +428,18 @@ interface EditionRepository {
+     fun savedEditionLayout(): String
+     fun saveEditionLayout(layoutId: String): Result<Unit>
+     suspend fun generateToday(): Result<Edition>
++    suspend fun newEditionNow(): Result<Edition>
++    suspend fun loadSameDayExtras(): List<Edition>
+     suspend fun updateMorningEdition(edition: Edition): Result<Edition>
++    suspend fun updateExtra(edition: Edition): Result<Edition>
++}
++
++internal fun keepMorningEdits(existing: Edition, generated: Edition): Edition {
++    val freshKeys = generated.events.map { it.title to it.timeLabel }.toSet()
++    val keptLines = existing.events.filter { (it.title to it.timeLabel) !in freshKeys }
++    return generated.copy(
++        briefing = existing.briefing,
++        events = generated.events + keptLines,
++        tasks = existing.tasks,
++    )
+ }
+diff --git a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+index 711cd09..682a20d 100644
+--- a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
++++ b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+@@ -43,6 +43,21 @@ private class FakeStore(
+         saved = edition
+     }
+ 
++    private val extras = mutableListOf<Edition>()
++
++    override fun loadExtras(dayKey: String): List<Edition> =
++        extras.filter { it.dayKey == dayKey && it.role == EditionRole.extra }
++
++    override fun saveExtra(edition: Edition) {
++        require(edition.role == EditionRole.extra)
++        val index = extras.indexOfFirst { it.id == edition.id }
++        if (index >= 0) extras[index] = edition else extras += edition
++    }
++
++    fun seedExtra(edition: Edition) {
++        extras += edition
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = snapshot
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
+@@ -428,6 +443,148 @@ class LocalEditionRepositoryTest {
+         assertFalse(edition.isSample)
+     }
+ 
++    @Test
++    fun generate_again_keepsBriefingRewrittenLineAndTasks() = runBlocking {
++        val existing = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 3,
++            isSample = false,
++            briefing = "Kept by hand",
++            events = listOf(
++                EditionEvent("Planning", "9:30"),
++                EditionEvent("Team standup", "10:00"),
++            ),
++            tasks = listOf(
++                EditionTask("Buy milk", completed = true),
++                EditionTask("File notes"),
++            ),
++        )
++        val store = FakeStore(
++            morning = existing,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++            snapshot = CalendarSnapshot(
++                occurrences = listOf(
++                    CalendarOccurrence(title = "Planning", startEpochSecond = utc(9, 30), endEpochSecond = utc(10, 0)),
++                ),
++            ),
++        )
++        val repo = LocalEditionRepository(
++            store,
++            today = { sampleDay },
++            timeZone = TimeZone.UTC,
++            readFeed = { PERSONAL_RSS },
++        )
++
++        val edition = repo.generateToday().getOrThrow()
++
++        assertEquals(existing.id, edition.id)
++        assertEquals(4, edition.revision)
++        assertEquals("Kept by hand", edition.briefing)
++        assertEquals(existing.tasks, edition.tasks)
++        assertTrue(edition.events.contains(EditionEvent("Planning", "9:30")))
++        assertTrue(edition.events.contains(EditionEvent("Team standup", "10:00")))
++        assertEquals(1, edition.events.count { it == EditionEvent("Planning", "9:30") })
++        assertEquals(
++            listOf("Markets open higher", "Port reopens"),
++            edition.stories.map { it.headline },
++        )
++        assertEquals(StoryRank.lead, edition.stories.first().rank)
++    }
++
++    @Test
++    fun newEditionNow_insertsAnExtraAndLeavesOtherEditions() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 2,
++            isSample = false,
++            briefing = "Morning stays",
++        )
++        val older = morning.copy(
++            id = "extra-${sampleDay.toDayKey()}-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Older extra",
++        )
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++        )
++        store.seedExtra(older)
++        store.seedExtra(older.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06", briefing = "Yesterday"))
++        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })
++
++        val extra = repo.newEditionNow().getOrThrow()
++
++        assertEquals(EditionRole.extra, extra.role)
++        assertEquals("extra-${sampleDay.toDayKey()}-2", extra.id)
++        assertEquals(1, extra.revision)
++        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertEquals(older, store.loadExtras(sampleDay.toDayKey()).first())
++        assertEquals(morning, repo.loadCurrentMorningEdition().getOrThrow())
++        assertEquals(listOf(older, extra), repo.loadSameDayExtras())
++        assertTrue(repo.loadSameDayExtras().none { it.dayKey != sampleDay.toDayKey() })
++    }
++
++    @Test
++    fun lateSnapshot_isOnRegenerateOrExtraAndNotOnThePaperBefore() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 1,
++            isSample = false,
++            events = listOf(EditionEvent("Kept", "8:00")),
++        )
++        val later = CalendarOccurrence(title = "Later", startEpochSecond = utc(15, 0), endEpochSecond = utc(16, 0))
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++            snapshot = CalendarSnapshot(occurrences = listOf(later)),
++        )
++        val repo = LocalEditionRepository(
++            store,
++            today = { sampleDay },
++            timeZone = TimeZone.UTC,
++            readFeed = { PERSONAL_RSS },
++        )
++
++        assertEquals(listOf(EditionEvent("Kept", "8:00")), repo.loadCurrentMorningEdition().getOrThrow().events)
++
++        val regenerated = repo.generateToday().getOrThrow()
++        assertTrue(regenerated.events.contains(EditionEvent("Later", "15:00")))
++        assertEquals(morning.id, regenerated.id)
++
++        val untouched = regenerated.copy(events = listOf(EditionEvent("Kept", "8:00")), revision = regenerated.revision)
++        store.saveMorningEdition(untouched)
++        val extra = repo.newEditionNow().getOrThrow()
++        assertEquals(untouched, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertTrue(extra.events.contains(EditionEvent("Later", "15:00")))
++        assertFalse(extra.events.contains(EditionEvent("Kept", "8:00")))
++    }
++
++    @Test
++    fun failedGenerateAndExtra_leaveStoredEditions() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 2,
++            isSample = false,
++            briefing = "Still here",
++        )
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/bad.xml")),
++        )
++        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { NOT_A_FEED })
++
++        val generated = repo.generateToday()
++        val extra = repo.newEditionNow()
++
++        assertTrue(generated.isFailure)
++        assertEquals(FEED_DIDNT_LOAD, generated.exceptionOrNull()?.message)
++        assertTrue(extra.isFailure)
++        assertEquals(FEED_DIDNT_LOAD, extra.exceptionOrNull()?.message)
++        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertTrue(store.loadExtras(sampleDay.toDayKey()).isEmpty())
++    }
++
+     @Test
+     fun saveTopics_storesChoicesLinesAndLeavesTheSourceList() = runBlocking {
+         val list = SourceList(
+diff --git a/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
+new file mode 100644
+index 0000000..7085344
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
+@@ -0,0 +1,82 @@
++---
++title: '2-6 Regenerate without losing the edits'
++type: 'feature'
++created: '2026-11-10'
++status: 'in-review'
++route: 'dispatch'
++review_loop_iteration: 0
++story_key: '2-6-regenerate-without-losing-the-edits'
++baseline_commit: 'fd8aa8ffad6e0a644fdeeee090c499ae251bbb5f'
++context:
++  - _bmad-output/implementation-artifacts/epic-2-context.md
++---
++
++<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
++
++## Intent
++
++**Problem:** Generating today's edition again replaces the morning body, so a rewritten briefing, a rewritten calendar line, and a checked task disappear. There is no same-day extra. A device-calendar snapshot saved after the paper exists never reaches a later paper.
++
++**Approach:** Regenerate keeps the morning id and those three edits, and may refresh stories, rank, and unedited events. New edition now inserts one extra and leaves every other edition untouched. The front page stays today's morning edition. Today's extras open from a same-day list. The late snapshot is included only on that regenerate or extra.
++
++## Boundaries & Constraints
++
++**Always:** Keep the morning id, the briefing already on that document, and every existing task (completed or added). Keep a calendar line whose title and time are not both on the fresh event list, and include every fresh event. Stories and rank come from the new sources. The extra is a new `extra` document with a new id and revision 1. Morning and earlier extras stay unchanged. The list is today's extras only. No revision number is drawn. Tray items, Settings' one primary Generate, and morning insert-if-absent stay. A failed command leaves stored editions unchanged.
++
++**Never:** A merge for a disappeared source item. A merge screen. A past-day archive. A tray item. A server upload. An API key.
++
++## I/O & Edge-Case Matrix
++
++| Scenario | Input / State | Expected Output / Behavior | Error Handling |
++|----------|--------------|---------------------------|----------------|
++| Regenerate with edits | Personal morning: rewritten briefing, a calendar line that differs in title or time, a checked task, an added task | Same id, revision + 1, briefing and tasks unchanged, rewritten line present, fresh stories, ranks, and events included | N/A |
++| New edition now | Morning exists, maybe one extra | New same-day extra; morning and older extra unchanged; current load is still the morning | N/A |
++| Late snapshot | Snapshot saved after the morning paper; that paper does not yet include it | Those events are on the regenerated morning edition or the new extra, not written at snapshot time | N/A |
++| Failed command | Feed read fails | Stored editions unchanged | "That feed didn't load." |
++| Same-day list | Two extras today and an older day | List offers the two extras only; opening one shows it; Today's morning edition returns to the front page | N/A |
++
++</frozen-after-approval>
++
++## Code Map
++
++- `core/.../edition/EditionGenerator.kt` — fresh stories, rank, events, empty tasks. Reuse. Do not teach it extras.
++- `core/.../edition/EditionRepository.kt` — after `fromFeedResults`, overlay the keeps when the stored morning edition is personal. Add `newEditionNow`, `loadSameDayExtras`, and an extra save that does not touch other ids. `recordDeviceCalendar` still leaves the paper alone. `runMorning` stays insert-if-absent.
++- `EditionStore` — add `loadExtras(dayKey)` and `saveExtra`. Morning files stay `editions/{dayKey}-morning.json`.
++- `LocalEditionRepositoryTest.kt` `FakeStore` — in-memory extras. `by inner` delegates the new methods.
++- `FileEditionStore.jvm.kt` and `FileEditionStore.android.kt` — extras at `editions/extra-{dayKey}-{n}.json`. JS and wasm stubs compile.
++- `EditionViewModel.kt` — `generate()` still regenerates and shows the morning result. `newEditionNow`, `openExtra`, and `openMorning`. Task and typewriter saves follow the document on screen.
++- `EditionScreen.kt` and `MatutineApp.kt` — "New edition now" beside Settings, then the same-day list. `main.kt` tray stays three items.
++
++## Tasks & Acceptance
++
++**Execution:**
++- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` — keeps on regenerate; insert and list extras
++- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` — I/O rows for keeps, isolation, late snapshot, and failure
++- [x] `app/shared/src/jvmMain/.../FileEditionStore.jvm.kt` and `androidMain/.../FileEditionStore.android.kt` — persist extras; JS and wasm stubs
++- [x] `app/shared/src/jvmTest/.../FileEditionStoreTest.kt` — extra round-trip does not rewrite the morning file
++- [x] `EditionViewModel.kt`, `EditionScreen.kt`, `MatutineApp.kt` — button, list, open extra, return to morning
++- [x] `app/shared/src/commonTest/.../EditionGenerateStateTest.kt` — insert leaves morning on the front page; the list opens the extra
++
++**Acceptance Criteria:**
++- Given a rewritten briefing, a rewritten calendar line, and a checked task, when O generates today's morning edition again, then the id, briefing, line, and checked task remain, and stories and rank may change.
++- Given O asks for a new edition now, when it finishes, then a same-day extra exists, other editions are untouched, the front page is still today's morning edition, and the extra opens from a same-day list with no past days, no revision number, and no merge screen.
++- Given a device-calendar snapshot saved after today's morning edition exists, when O regenerates or asks for a new edition now, then those events are on that result and were not on the paper before the command.
++
++## Implementation Notes
++
++- Regenerate of a personal morning edition keeps its briefing and tasks, and appends calendar lines whose title and time are not both on the fresh event list. Stories and rank come from the new sources. A sample edition still becomes the personal paper.
++- New edition now writes `editions/extra-{dayKey}-{n}.json` and does not save the morning file. The edition screen lists today's extras by first headline, or Extra, and returns with Today's morning edition. The tray menu is unchanged.
++- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` — BUILD SUCCESSFUL. No emulator and no commit.
++
++## Spec Change Log
++
++## Review Triage Log
++
++## Design Notes
++
++Fresh events stay in source order. A morning calendar line is appended only when its title and time are not both on that list. Tasks are copied as they stand. The extra does not receive those edits. A list row is the extra's first headline, or Extra. "Today's morning edition" is the return row while an extra is open.
++
++## Verification
++
++**Commands:**
++- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
+
+Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. If the instruction file is unreadable, report that exact failure and stop. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
diff --git a/_bmad-output/implementation-artifacts/review-2-6-verification-gap-prompt.md b/_bmad-output/implementation-artifacts/review-2-6-verification-gap-prompt.md
new file mode 100644
index 0000000..bf4ab2c
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/review-2-6-verification-gap-prompt.md
@@ -0,0 +1,1004 @@
+Read the following completely and follow it as your review instructions.
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
+Review content: the unified diff at the following contents. Read that file — it is the content under review.
+
+diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
+index ceed8f0..08ea20d 100644
+--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
++++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
+@@ -29,7 +29,7 @@
+ # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
+ # - Retrospective appends its action items to action_items; the status view surfaces open ones
+ generated: 11-07-2026 19:42
+-last_updated: 11-10-2026 08:37
++last_updated: 11-10-2026 18:11
+ project: Matutine
+ project_key: NOKEY
+ tracking_system: file-system
+@@ -55,7 +55,7 @@ development_status:
+   2-3-customize-the-android-paper: done
+   2-4-use-the-desktop-tray: done
+   2-5-rewrite-the-edition-on-the-typewriter: done
+-  2-6-regenerate-without-losing-the-edits: backlog
++  2-6-regenerate-without-losing-the-edits: in-progress
+   2-7-write-the-briefing-with-a-gemini-key: backlog
+   epic-2-retrospective: optional
+ 
+diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
+index 3f7b6cd..2fcca9e 100644
+--- a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
++++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
+@@ -62,6 +62,25 @@ actual class FileEditionStore actual constructor(
+         file.writeText(encodeEdition(edition))
+     }
+ 
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
++        val dir = File(root, "editions")
++        dir.mkdirs()
++        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? {
+         val file = File(root, "calendar-snapshot.json")
+         if (!file.exists()) return null
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+index 7c6b5dc..7895a75 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+@@ -167,6 +167,10 @@ fun MatutineApp(
+                 calendarPermissionOff = calendarPermissionOff,
+                 onOpenCalendarPermission = ::openCalendarPermissionSettings,
+                 editionLayoutId = viewModel.editionLayout,
++                sameDayExtras = ready?.sameDayExtras.orEmpty(),
++                onNewEditionNow = viewModel::newEditionNow,
++                onOpenExtra = viewModel::openExtra,
++                onOpenMorning = viewModel::openMorning,
+                 onOpenTypewriter = if (showsTypewriter() && ready != null) {
+                     {
+                         typewriterDraft = EditionTranscript.render(ready.edition)
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
+index d4c7314..ac42e6f 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
+@@ -13,3 +13,6 @@ internal object EditionCopy {
+ 
+ internal fun showsNoTopicLine(isSample: Boolean, topics: List<String>): Boolean =
+     !isSample && topics.none { it.isNotBlank() }
++
++internal fun sameDayExtraLabel(edition: Edition): String =
++    edition.stories.firstOrNull()?.headline?.takeIf { it.isNotBlank() } ?: "Extra"
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+index 0d36849..c06cc9b 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+@@ -68,6 +68,10 @@ fun EditionScreen(
+     onOpenCalendarPermission: () -> Unit = {},
+     editionLayoutId: String = DEFAULT_EDITION_LAYOUT,
+     onOpenTypewriter: (() -> Unit)? = null,
++    sameDayExtras: List<Edition> = emptyList(),
++    onNewEditionNow: () -> Unit = {},
++    onOpenExtra: (String) -> Unit = {},
++    onOpenMorning: () -> Unit = {},
+     modifier: Modifier = Modifier,
+ ) {
+     val typography = LocalMatutineEditionTypography.current
+@@ -96,6 +100,10 @@ fun EditionScreen(
+             onOpenCalendarPermission = onOpenCalendarPermission,
+             editionLayoutId = editionLayoutId,
+             onOpenTypewriter = onOpenTypewriter,
++            sameDayExtras = sameDayExtras,
++            onNewEditionNow = onNewEditionNow,
++            onOpenExtra = onOpenExtra,
++            onOpenMorning = onOpenMorning,
+             typography = typography,
+             modifier = modifier,
+         )
+@@ -217,6 +225,10 @@ private fun EditionContent(
+     onOpenCalendarPermission: () -> Unit,
+     editionLayoutId: String,
+     onOpenTypewriter: (() -> Unit)?,
++    sameDayExtras: List<Edition>,
++    onNewEditionNow: () -> Unit,
++    onOpenExtra: (String) -> Unit,
++    onOpenMorning: () -> Unit,
+     typography: MatutineEditionTypography,
+     modifier: Modifier = Modifier,
+ ) {
+@@ -254,6 +266,34 @@ private fun EditionContent(
+                         Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
+                     }
+                 }
++                TextButton(
++                    onClick = onNewEditionNow,
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
++                }
++            }
++            if (edition.role == EditionRole.extra) {
++                TextButton(
++                    onClick = onOpenMorning,
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(
++                        text = "Today's morning edition",
++                        style = typography.body.copy(color = EditionThemeColors.current.ink),
++                    )
++                }
++            }
++            sameDayExtras.forEach { extra ->
++                TextButton(
++                    onClick = { onOpenExtra(extra.id) },
++                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
++                ) {
++                    Text(
++                        text = sameDayExtraLabel(extra),
++                        style = typography.body.copy(color = EditionThemeColors.current.ink),
++                    )
++                }
+             }
+             Masthead(edition.mastheadName, edition.dateLine, typography)
+             if (generating) {
+diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+index acbabcc..fa54a3b 100644
+--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
++++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+@@ -17,6 +17,7 @@ sealed interface EditionUiState {
+         val generating: Boolean = false,
+         val notice: String? = null,
+         val taskSaved: Boolean = false,
++        val sameDayExtras: List<Edition> = emptyList(),
+     ) : EditionUiState
+ 
+     data class SampleLoadFailed(val message: String = "The sample edition didn't load.") : EditionUiState
+@@ -279,7 +280,11 @@ class EditionViewModel(
+             repository.generateToday()
+                 .onSuccess { edition ->
+                     feedFieldError = null
+-                    state = EditionUiState.Ready(edition, notice = notice)
++                    state = EditionUiState.Ready(
++                        edition,
++                        notice = notice,
++                        sameDayExtras = loadExtras(),
++                    )
+                 }
+                 .onFailure {
+                     state = restore(FEED_DIDNT_LOAD)
+@@ -291,11 +296,50 @@ class EditionViewModel(
+         state = EditionUiState.Loading
+         scope.launch {
+             repository.loadCurrentMorningEdition()
+-                .onSuccess { edition -> state = EditionUiState.Ready(edition) }
++                .onSuccess { edition ->
++                    state = EditionUiState.Ready(edition, sameDayExtras = loadExtras())
++                }
+                 .onFailure { error -> state = mapFailure(error) }
+         }
+     }
+ 
++    fun newEditionNow() {
++        val current = state as? EditionUiState.Ready ?: return
++        if (current.generating) return
++        state = current.copy(generating = true, notice = null)
++        scope.launch {
++            repository.newEditionNow()
++                .onSuccess {
++                    val morning = repository.loadCurrentMorningEdition().getOrThrow()
++                    state = EditionUiState.Ready(morning, sameDayExtras = loadExtras())
++                }
++                .onFailure {
++                    val shown = state as? EditionUiState.Ready ?: current
++                    state = shown.copy(generating = false, notice = FEED_DIDNT_LOAD)
++                }
++        }
++    }
++
++    fun openExtra(id: String) {
++        val current = state as? EditionUiState.Ready ?: return
++        val extra = current.sameDayExtras.singleOrNull { it.id == id } ?: return
++        state = current.copy(edition = extra, taskSaved = false)
++    }
++
++    fun openMorning() {
++        val current = state as? EditionUiState.Ready ?: return
++        scope.launch {
++            repository.loadCurrentMorningEdition()
++                .onSuccess { morning ->
++                    state = current.copy(
++                        edition = morning,
++                        sameDayExtras = loadExtras(),
++                        taskSaved = false,
++                    )
++                }
++        }
++    }
++
+     fun retrySample() {
+         state = EditionUiState.Loading
+         scope.launch {
+@@ -338,7 +382,7 @@ class EditionViewModel(
+         }
+         val generation = ++typewriterGeneration
+         scope.launch {
+-            repository.updateMorningEdition(updated)
++            saveEdition(updated)
+                 .onSuccess { saved ->
+                     if (generation != typewriterGeneration) {
+                         onResult(false)
+@@ -378,7 +422,7 @@ class EditionViewModel(
+         state = previous.copy(edition = updated)
+         val rollback = previous
+         scope.launch {
+-            repository.updateMorningEdition(updated)
++            saveEdition(updated)
+                 .onSuccess { saved ->
+                     val current = state as? EditionUiState.Ready
+                     if (current != null && current.edition.tasks == updated.tasks) {
+@@ -394,6 +438,16 @@ class EditionViewModel(
+         }
+     }
+ 
++    private suspend fun loadExtras(): List<Edition> =
++        runCatching { repository.loadSameDayExtras() }.getOrDefault(emptyList())
++
++    private suspend fun saveEdition(edition: Edition): Result<Edition> =
++        if (edition.role == EditionRole.extra) {
++            repository.updateExtra(edition)
++        } else {
++            repository.updateMorningEdition(edition)
++        }
++
+     private fun calDavFieldsDirty(url: String, username: String, password: String): Boolean =
+         password.isNotEmpty() || url.trim() != calDavUrlBaseline || username.trim() != calDavUsernameBaseline
+ 
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+index ee97379..58c6618 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+@@ -382,6 +382,49 @@ class EditionGenerateStateTest {
+         assertEquals(1, repository.generateCalls)
+     }
+ 
++    @Test
++    fun newEditionNowLeavesTheMorningOnTheFrontPageAndTheListOpensTheExtra() {
++        val repository = FakeEditionRepository(finished)
++        val viewModel = viewModel(repository)
++
++        viewModel.newEditionNow()
++
++        val front = viewModel.state as EditionUiState.Ready
++        assertEquals(finished.id, front.edition.id)
++        assertEquals(EditionRole.morning, front.edition.role)
++        assertEquals(finished.briefing, front.edition.briefing)
++        val extra = front.sameDayExtras.single()
++        assertEquals(EditionRole.extra, extra.role)
++        assertEquals(extra.dayKey, finished.dayKey)
++        assertEquals(sameDayExtraLabel(extra), extra.stories.first().headline)
++
++        viewModel.openExtra(extra.id)
++        val opened = viewModel.state as EditionUiState.Ready
++        assertEquals(extra.id, opened.edition.id)
++        assertEquals(listOf(extra), opened.sameDayExtras)
++
++        viewModel.openMorning()
++        val back = viewModel.state as EditionUiState.Ready
++        assertEquals(finished.id, back.edition.id)
++        assertEquals(EditionRole.morning, back.edition.role)
++        assertTrue(back.sameDayExtras.all { it.dayKey == finished.dayKey })
++    }
++
++    @Test
++    fun failedNewEditionNowKeepsTheVisiblePaper() {
++        val repository = FakeEditionRepository(finished)
++        repository.failNewEdition = true
++        val viewModel = viewModel(repository)
++
++        viewModel.newEditionNow()
++
++        val ready = viewModel.state as EditionUiState.Ready
++        assertFalse(ready.generating)
++        assertEquals(finished.id, ready.edition.id)
++        assertEquals(FEED_DIDNT_LOAD, ready.notice)
++        assertTrue(ready.sameDayExtras.isEmpty())
++    }
++
+     private fun viewModel(repository: EditionRepository) =
+         EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
+ }
+@@ -485,5 +528,25 @@ private class FakeEditionRepository(
+         return runCatching { generate() }
+     }
+ 
++    private val extras = mutableListOf<Edition>()
++    var failNewEdition: Boolean = false
++
++    override suspend fun newEditionNow(): Result<Edition> {
++        if (failNewEdition) return Result.failure(IllegalStateException(FEED_DIDNT_LOAD))
++        val extra = edition.copy(
++            id = "extra-${edition.dayKey}-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Fresh extra",
++        )
++        extras += extra
++        return Result.success(extra)
++    }
++
++    override suspend fun loadSameDayExtras(): List<Edition> = extras.toList()
++
+     override suspend fun updateMorningEdition(edition: Edition): Result<Edition> = Result.success(edition)
++
++    override suspend fun updateExtra(edition: Edition): Result<Edition> =
++        Result.success(edition.copy(revision = edition.revision + 1))
+ }
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
+index 1bc52ab..74b6c4a 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
+@@ -122,6 +122,10 @@ class EditionTaskStateTest {
+ 
+         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
+ 
++        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
++
++        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
++
+         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
+             updateCalls += 1
+             val next = edition.copy(revision = morning.revision + 1)
+@@ -129,5 +133,7 @@ class EditionTaskStateTest {
+             lastSaved = next
+             return Result.success(next)
+         }
++
++        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
+     }
+ }
+diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
+index e241ccb..257690c 100644
+--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
++++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
+@@ -172,6 +172,10 @@ class EditionTypewriterStateTest {
+ 
+         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
+ 
++        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
++
++        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
++
+         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
+             gate?.await()
+             if (fail) return Result.failure(IllegalStateException("save failed"))
+@@ -180,5 +184,7 @@ class EditionTypewriterStateTest {
+             morning = next
+             return Result.success(next)
+         }
++
++        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
+     }
+ }
+diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
+index 6a39885..ff3af60 100644
+--- a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
++++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
+@@ -21,6 +21,10 @@ actual class FileEditionStore actual constructor(
+ 
+     override fun saveMorningEdition(edition: Edition) = Unit
+ 
++    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
++
++    override fun saveExtra(edition: Edition) = Unit
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = null
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = Unit
+diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
+index f5f6de0..66e56d1 100644
+--- a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
++++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
+@@ -61,6 +61,25 @@ actual class FileEditionStore actual constructor(
+         file.writeText(encodeEdition(edition))
+     }
+ 
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
++        val dir = File(root, "editions")
++        dir.mkdirs()
++        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? {
+         val file = File(root, "calendar-snapshot.json")
+         if (!file.exists()) return null
+diff --git a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
+index 2e3f14b..d14b31e 100644
+--- a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
++++ b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
+@@ -3,11 +3,14 @@ package hu.toliver.matutine.store
+ import hu.toliver.matutine.edition.CalDavAccount
+ import hu.toliver.matutine.edition.CalendarOccurrence
+ import hu.toliver.matutine.edition.CalendarSnapshot
++import hu.toliver.matutine.edition.EditionRole
+ import hu.toliver.matutine.edition.LocalEditionRepository
+ import hu.toliver.matutine.edition.ReaderSettings
++import hu.toliver.matutine.edition.SampleGenerator
+ import hu.toliver.matutine.edition.SourceList
+ import hu.toliver.matutine.edition.StoryRank
+ import kotlinx.coroutines.runBlocking
++import kotlinx.datetime.LocalDate
+ import java.io.File
+ import java.nio.file.Files
+ import kotlin.test.Test
+@@ -149,4 +152,33 @@ class FileEditionStoreTest {
+         assertTrue(File(dir, "calendar-snapshot.json").exists())
+         assertNull(store.readSourceList())
+     }
++
++    @Test
++    fun saveExtra_roundTripsWithoutRewritingTheMorningFile() {
++        val dir = Files.createTempDirectory("matutine-extra").toFile()
++        val store = FileEditionStore(dir.absolutePath)
++        val morning = SampleGenerator.forDay(LocalDate(2026, 11, 7)).copy(
++            id = "morning-2026-11-07",
++            isSample = false,
++            briefing = "Morning stays",
++        )
++        store.saveMorningEdition(morning)
++        val morningFile = File(dir, "editions/2026-11-07-morning.json")
++        val before = morningFile.readText()
++        val extra = morning.copy(
++            id = "extra-2026-11-07-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Extra",
++        )
++        val otherDay = extra.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06")
++
++        store.saveExtra(extra)
++        store.saveExtra(otherDay)
++
++        assertEquals(before, morningFile.readText())
++        assertEquals(listOf(extra), store.loadExtras("2026-11-07"))
++        assertEquals(listOf(otherDay), store.loadExtras("2026-11-06"))
++        assertEquals(morning, store.loadMorningEdition("2026-11-07"))
++    }
+ }
+diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
+index 6a39885..ff3af60 100644
+--- a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
++++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
+@@ -21,6 +21,10 @@ actual class FileEditionStore actual constructor(
+ 
+     override fun saveMorningEdition(edition: Edition) = Unit
+ 
++    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
++
++    override fun saveExtra(edition: Edition) = Unit
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = null
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = Unit
+diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+index d62d029..ccef4fb 100644
+--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
++++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+@@ -16,6 +16,8 @@ interface EditionStore {
+ 
+     fun loadMorningEdition(dayKey: String): Edition?
+     fun saveMorningEdition(edition: Edition)
++    fun loadExtras(dayKey: String): List<Edition>
++    fun saveExtra(edition: Edition)
+ 
+     /** Null when the snapshot document is absent. */
+     fun readCalendarSnapshot(): CalendarSnapshot?
+@@ -261,12 +263,85 @@ class LocalEditionRepository(
+         } else {
+             edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
+         }
++        val next = if (replaceExisting && existing != null && !existing.isSample) {
++            keepMorningEdits(existing, noted)
++        } else {
++            noted
++        }
+         if (!replaceExisting) {
+             val current = store.loadMorningEdition(date.toDayKey())
+             if (current != null) return current
+         }
+-        store.saveMorningEdition(noted)
+-        return noted
++        store.saveMorningEdition(next)
++        return next
++    }
++
++    override suspend fun newEditionNow(): Result<Edition> = runCatching {
++        val date = today()
++        val dayKey = date.toDayKey()
++        if (store.loadMorningEdition(dayKey) == null) error(MORNING_EDITION_NOT_READY)
++        val list = store.readSourceList()
++        if (list == null || list.feeds.isEmpty()) error(FEED_DIDNT_LOAD)
++        val parsed = list.feeds.map { url ->
++            runCatching { parseFeedDocument(readDocument(url)) }
++        }
++        if (parsed.none { it.isSuccess }) error(FEED_DIDNT_LOAD)
++        recordDeviceCalendar(date)
++        val calDav = readCalDavOccurrences()
++        val events = editionEventsFrom(
++            sources = listOf(
++                readIcsOccurrences(list),
++                store.readCalendarSnapshot()?.occurrences.orEmpty(),
++                calDav.occurrences,
++            ),
++            day = date,
++            zone = timeZone,
++        )
++        val edition = EditionGenerator.fromFeedResults(
++            date = date,
++            parsed = parsed,
++            existing = null,
++            topics = store.readSettings()?.topics.orEmpty(),
++            events = events,
++        )
++        val noted = if (calDav.notes.isEmpty()) {
++            edition
++        } else {
++            edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
++        }
++        val extra = noted.copy(
++            id = nextExtraId(dayKey),
++            revision = 1,
++            role = EditionRole.extra,
++        )
++        store.saveExtra(extra)
++        extra
++    }
++
++    override suspend fun loadSameDayExtras(): List<Edition> =
++        store.loadExtras(today().toDayKey())
++
++    override suspend fun updateExtra(edition: Edition): Result<Edition> = runCatching {
++        val dayKey = today().toDayKey()
++        require(edition.dayKey == dayKey && edition.role == EditionRole.extra)
++        val stored = store.loadExtras(dayKey).singleOrNull { it.id == edition.id }
++            ?: error(MORNING_EDITION_NOT_READY)
++        val next = edition.copy(
++            id = stored.id,
++            revision = stored.revision + 1,
++            dayKey = stored.dayKey,
++            role = EditionRole.extra,
++        )
++        store.saveExtra(next)
++        next
++    }
++
++    private fun nextExtraId(dayKey: String): String {
++        val prefix = "extra-$dayKey-"
++        val used = store.loadExtras(dayKey).mapNotNull { extra ->
++            extra.id.removePrefix(prefix).toIntOrNull()
++        }
++        return prefix + ((used.maxOrNull() ?: 0) + 1)
+     }
+ 
+     override fun savedCalDavAccount(): CalDavAccount? =
+@@ -353,5 +428,18 @@ interface EditionRepository {
+     fun savedEditionLayout(): String
+     fun saveEditionLayout(layoutId: String): Result<Unit>
+     suspend fun generateToday(): Result<Edition>
++    suspend fun newEditionNow(): Result<Edition>
++    suspend fun loadSameDayExtras(): List<Edition>
+     suspend fun updateMorningEdition(edition: Edition): Result<Edition>
++    suspend fun updateExtra(edition: Edition): Result<Edition>
++}
++
++internal fun keepMorningEdits(existing: Edition, generated: Edition): Edition {
++    val freshKeys = generated.events.map { it.title to it.timeLabel }.toSet()
++    val keptLines = existing.events.filter { (it.title to it.timeLabel) !in freshKeys }
++    return generated.copy(
++        briefing = existing.briefing,
++        events = generated.events + keptLines,
++        tasks = existing.tasks,
++    )
+ }
+diff --git a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+index 711cd09..682a20d 100644
+--- a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
++++ b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+@@ -43,6 +43,21 @@ private class FakeStore(
+         saved = edition
+     }
+ 
++    private val extras = mutableListOf<Edition>()
++
++    override fun loadExtras(dayKey: String): List<Edition> =
++        extras.filter { it.dayKey == dayKey && it.role == EditionRole.extra }
++
++    override fun saveExtra(edition: Edition) {
++        require(edition.role == EditionRole.extra)
++        val index = extras.indexOfFirst { it.id == edition.id }
++        if (index >= 0) extras[index] = edition else extras += edition
++    }
++
++    fun seedExtra(edition: Edition) {
++        extras += edition
++    }
++
+     override fun readCalendarSnapshot(): CalendarSnapshot? = snapshot
+ 
+     override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
+@@ -428,6 +443,148 @@ class LocalEditionRepositoryTest {
+         assertFalse(edition.isSample)
+     }
+ 
++    @Test
++    fun generate_again_keepsBriefingRewrittenLineAndTasks() = runBlocking {
++        val existing = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 3,
++            isSample = false,
++            briefing = "Kept by hand",
++            events = listOf(
++                EditionEvent("Planning", "9:30"),
++                EditionEvent("Team standup", "10:00"),
++            ),
++            tasks = listOf(
++                EditionTask("Buy milk", completed = true),
++                EditionTask("File notes"),
++            ),
++        )
++        val store = FakeStore(
++            morning = existing,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++            snapshot = CalendarSnapshot(
++                occurrences = listOf(
++                    CalendarOccurrence(title = "Planning", startEpochSecond = utc(9, 30), endEpochSecond = utc(10, 0)),
++                ),
++            ),
++        )
++        val repo = LocalEditionRepository(
++            store,
++            today = { sampleDay },
++            timeZone = TimeZone.UTC,
++            readFeed = { PERSONAL_RSS },
++        )
++
++        val edition = repo.generateToday().getOrThrow()
++
++        assertEquals(existing.id, edition.id)
++        assertEquals(4, edition.revision)
++        assertEquals("Kept by hand", edition.briefing)
++        assertEquals(existing.tasks, edition.tasks)
++        assertTrue(edition.events.contains(EditionEvent("Planning", "9:30")))
++        assertTrue(edition.events.contains(EditionEvent("Team standup", "10:00")))
++        assertEquals(1, edition.events.count { it == EditionEvent("Planning", "9:30") })
++        assertEquals(
++            listOf("Markets open higher", "Port reopens"),
++            edition.stories.map { it.headline },
++        )
++        assertEquals(StoryRank.lead, edition.stories.first().rank)
++    }
++
++    @Test
++    fun newEditionNow_insertsAnExtraAndLeavesOtherEditions() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 2,
++            isSample = false,
++            briefing = "Morning stays",
++        )
++        val older = morning.copy(
++            id = "extra-${sampleDay.toDayKey()}-1",
++            revision = 1,
++            role = EditionRole.extra,
++            briefing = "Older extra",
++        )
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++        )
++        store.seedExtra(older)
++        store.seedExtra(older.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06", briefing = "Yesterday"))
++        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })
++
++        val extra = repo.newEditionNow().getOrThrow()
++
++        assertEquals(EditionRole.extra, extra.role)
++        assertEquals("extra-${sampleDay.toDayKey()}-2", extra.id)
++        assertEquals(1, extra.revision)
++        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertEquals(older, store.loadExtras(sampleDay.toDayKey()).first())
++        assertEquals(morning, repo.loadCurrentMorningEdition().getOrThrow())
++        assertEquals(listOf(older, extra), repo.loadSameDayExtras())
++        assertTrue(repo.loadSameDayExtras().none { it.dayKey != sampleDay.toDayKey() })
++    }
++
++    @Test
++    fun lateSnapshot_isOnRegenerateOrExtraAndNotOnThePaperBefore() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 1,
++            isSample = false,
++            events = listOf(EditionEvent("Kept", "8:00")),
++        )
++        val later = CalendarOccurrence(title = "Later", startEpochSecond = utc(15, 0), endEpochSecond = utc(16, 0))
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
++            snapshot = CalendarSnapshot(occurrences = listOf(later)),
++        )
++        val repo = LocalEditionRepository(
++            store,
++            today = { sampleDay },
++            timeZone = TimeZone.UTC,
++            readFeed = { PERSONAL_RSS },
++        )
++
++        assertEquals(listOf(EditionEvent("Kept", "8:00")), repo.loadCurrentMorningEdition().getOrThrow().events)
++
++        val regenerated = repo.generateToday().getOrThrow()
++        assertTrue(regenerated.events.contains(EditionEvent("Later", "15:00")))
++        assertEquals(morning.id, regenerated.id)
++
++        val untouched = regenerated.copy(events = listOf(EditionEvent("Kept", "8:00")), revision = regenerated.revision)
++        store.saveMorningEdition(untouched)
++        val extra = repo.newEditionNow().getOrThrow()
++        assertEquals(untouched, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertTrue(extra.events.contains(EditionEvent("Later", "15:00")))
++        assertFalse(extra.events.contains(EditionEvent("Kept", "8:00")))
++    }
++
++    @Test
++    fun failedGenerateAndExtra_leaveStoredEditions() = runBlocking {
++        val morning = SampleGenerator.forDay(sampleDay).copy(
++            id = "morning-${sampleDay.toDayKey()}",
++            revision = 2,
++            isSample = false,
++            briefing = "Still here",
++        )
++        val store = FakeStore(
++            morning = morning,
++            sourceList = SourceList(feeds = listOf("https://example.com/bad.xml")),
++        )
++        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { NOT_A_FEED })
++
++        val generated = repo.generateToday()
++        val extra = repo.newEditionNow()
++
++        assertTrue(generated.isFailure)
++        assertEquals(FEED_DIDNT_LOAD, generated.exceptionOrNull()?.message)
++        assertTrue(extra.isFailure)
++        assertEquals(FEED_DIDNT_LOAD, extra.exceptionOrNull()?.message)
++        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
++        assertTrue(store.loadExtras(sampleDay.toDayKey()).isEmpty())
++    }
++
+     @Test
+     fun saveTopics_storesChoicesLinesAndLeavesTheSourceList() = runBlocking {
+         val list = SourceList(
+diff --git a/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
+new file mode 100644
+index 0000000..7085344
+--- /dev/null
++++ b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
+@@ -0,0 +1,82 @@
++---
++title: '2-6 Regenerate without losing the edits'
++type: 'feature'
++created: '2026-11-10'
++status: 'in-review'
++route: 'dispatch'
++review_loop_iteration: 0
++story_key: '2-6-regenerate-without-losing-the-edits'
++baseline_commit: 'fd8aa8ffad6e0a644fdeeee090c499ae251bbb5f'
++context:
++  - _bmad-output/implementation-artifacts/epic-2-context.md
++---
++
++<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
++
++## Intent
++
++**Problem:** Generating today's edition again replaces the morning body, so a rewritten briefing, a rewritten calendar line, and a checked task disappear. There is no same-day extra. A device-calendar snapshot saved after the paper exists never reaches a later paper.
++
++**Approach:** Regenerate keeps the morning id and those three edits, and may refresh stories, rank, and unedited events. New edition now inserts one extra and leaves every other edition untouched. The front page stays today's morning edition. Today's extras open from a same-day list. The late snapshot is included only on that regenerate or extra.
++
++## Boundaries & Constraints
++
++**Always:** Keep the morning id, the briefing already on that document, and every existing task (completed or added). Keep a calendar line whose title and time are not both on the fresh event list, and include every fresh event. Stories and rank come from the new sources. The extra is a new `extra` document with a new id and revision 1. Morning and earlier extras stay unchanged. The list is today's extras only. No revision number is drawn. Tray items, Settings' one primary Generate, and morning insert-if-absent stay. A failed command leaves stored editions unchanged.
++
++**Never:** A merge for a disappeared source item. A merge screen. A past-day archive. A tray item. A server upload. An API key.
++
++## I/O & Edge-Case Matrix
++
++| Scenario | Input / State | Expected Output / Behavior | Error Handling |
++|----------|--------------|---------------------------|----------------|
++| Regenerate with edits | Personal morning: rewritten briefing, a calendar line that differs in title or time, a checked task, an added task | Same id, revision + 1, briefing and tasks unchanged, rewritten line present, fresh stories, ranks, and events included | N/A |
++| New edition now | Morning exists, maybe one extra | New same-day extra; morning and older extra unchanged; current load is still the morning | N/A |
++| Late snapshot | Snapshot saved after the morning paper; that paper does not yet include it | Those events are on the regenerated morning edition or the new extra, not written at snapshot time | N/A |
++| Failed command | Feed read fails | Stored editions unchanged | "That feed didn't load." |
++| Same-day list | Two extras today and an older day | List offers the two extras only; opening one shows it; Today's morning edition returns to the front page | N/A |
++
++</frozen-after-approval>
++
++## Code Map
++
++- `core/.../edition/EditionGenerator.kt` — fresh stories, rank, events, empty tasks. Reuse. Do not teach it extras.
++- `core/.../edition/EditionRepository.kt` — after `fromFeedResults`, overlay the keeps when the stored morning edition is personal. Add `newEditionNow`, `loadSameDayExtras`, and an extra save that does not touch other ids. `recordDeviceCalendar` still leaves the paper alone. `runMorning` stays insert-if-absent.
++- `EditionStore` — add `loadExtras(dayKey)` and `saveExtra`. Morning files stay `editions/{dayKey}-morning.json`.
++- `LocalEditionRepositoryTest.kt` `FakeStore` — in-memory extras. `by inner` delegates the new methods.
++- `FileEditionStore.jvm.kt` and `FileEditionStore.android.kt` — extras at `editions/extra-{dayKey}-{n}.json`. JS and wasm stubs compile.
++- `EditionViewModel.kt` — `generate()` still regenerates and shows the morning result. `newEditionNow`, `openExtra`, and `openMorning`. Task and typewriter saves follow the document on screen.
++- `EditionScreen.kt` and `MatutineApp.kt` — "New edition now" beside Settings, then the same-day list. `main.kt` tray stays three items.
++
++## Tasks & Acceptance
++
++**Execution:**
++- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` — keeps on regenerate; insert and list extras
++- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` — I/O rows for keeps, isolation, late snapshot, and failure
++- [x] `app/shared/src/jvmMain/.../FileEditionStore.jvm.kt` and `androidMain/.../FileEditionStore.android.kt` — persist extras; JS and wasm stubs
++- [x] `app/shared/src/jvmTest/.../FileEditionStoreTest.kt` — extra round-trip does not rewrite the morning file
++- [x] `EditionViewModel.kt`, `EditionScreen.kt`, `MatutineApp.kt` — button, list, open extra, return to morning
++- [x] `app/shared/src/commonTest/.../EditionGenerateStateTest.kt` — insert leaves morning on the front page; the list opens the extra
++
++**Acceptance Criteria:**
++- Given a rewritten briefing, a rewritten calendar line, and a checked task, when O generates today's morning edition again, then the id, briefing, line, and checked task remain, and stories and rank may change.
++- Given O asks for a new edition now, when it finishes, then a same-day extra exists, other editions are untouched, the front page is still today's morning edition, and the extra opens from a same-day list with no past days, no revision number, and no merge screen.
++- Given a device-calendar snapshot saved after today's morning edition exists, when O regenerates or asks for a new edition now, then those events are on that result and were not on the paper before the command.
++
++## Implementation Notes
++
++- Regenerate of a personal morning edition keeps its briefing and tasks, and appends calendar lines whose title and time are not both on the fresh event list. Stories and rank come from the new sources. A sample edition still becomes the personal paper.
++- New edition now writes `editions/extra-{dayKey}-{n}.json` and does not save the morning file. The edition screen lists today's extras by first headline, or Extra, and returns with Today's morning edition. The tray menu is unchanged.
++- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` — BUILD SUCCESSFUL. No emulator and no commit.
++
++## Spec Change Log
++
++## Review Triage Log
++
++## Design Notes
++
++Fresh events stay in source order. A morning calendar line is appended only when its title and time are not both on that list. Tasks are copied as they stand. The extra does not receive those edits. A list row is the extra's first headline, or Extra. "Today's morning edition" is the return row while an extra is open.
++
++## Verification
++
++**Commands:**
++- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
+
+Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. If the instruction file is unreadable, report that exact failure and stop. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
diff --git a/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
new file mode 100644
index 0000000..c3a64fb
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
@@ -0,0 +1,83 @@
+---
+title: '2-6 Regenerate without losing the edits'
+type: 'feature'
+created: '2026-11-10'
+status: 'in-review'
+route: 'dispatch'
+review_loop_iteration: 0
+story_key: '2-6-regenerate-without-losing-the-edits'
+baseline_commit: 'fd8aa8ffad6e0a644fdeeee090c499ae251bbb5f'
+context:
+  - _bmad-output/implementation-artifacts/epic-2-context.md
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** Generating today's edition again replaces the morning body, so a rewritten briefing, a rewritten calendar line, and a checked task disappear. There is no same-day extra. A device-calendar snapshot saved after the paper exists never reaches a later paper.
+
+**Approach:** Regenerate keeps the morning id and those three edits, and may refresh stories, rank, and unedited events. New edition now inserts one extra and leaves every other edition untouched. The front page stays today's morning edition. Today's extras open from a same-day list. The late snapshot is included only on that regenerate or extra.
+
+## Boundaries & Constraints
+
+**Always:** Keep the morning id, the briefing already on that document, and every existing task (completed or added). Keep a calendar line whose title and time are not both on the fresh event list, and include every fresh event. Stories and rank come from the new sources. The extra is a new `extra` document with a new id and revision 1. Morning and earlier extras stay unchanged. The list is today's extras only. No revision number is drawn. Tray items, Settings' one primary Generate, and morning insert-if-absent stay. A failed command leaves stored editions unchanged.
+
+**Never:** A merge for a disappeared source item. A merge screen. A past-day archive. A tray item. A server upload. An API key.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Regenerate with edits | Personal morning: rewritten briefing, a calendar line that differs in title or time, a checked task, an added task | Same id, revision + 1, briefing and tasks unchanged, rewritten line present, fresh stories, ranks, and events included | N/A |
+| New edition now | Morning exists, maybe one extra | New same-day extra; morning and older extra unchanged; current load is still the morning | N/A |
+| Late snapshot | Snapshot saved after the morning paper; that paper does not yet include it | Those events are on the regenerated morning edition or the new extra, not written at snapshot time | N/A |
+| Failed command | Feed read fails | Stored editions unchanged | "That feed didn't load." |
+| Same-day list | Two extras today and an older day | List offers the two extras only; opening one shows it; Today's morning edition returns to the front page | N/A |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `core/.../edition/EditionGenerator.kt` — fresh stories, rank, events, empty tasks. Reuse. Do not teach it extras.
+- `core/.../edition/EditionRepository.kt` — after `fromFeedResults`, overlay the keeps when the stored morning edition is personal. Add `newEditionNow`, `loadSameDayExtras`, and an extra save that does not touch other ids. `recordDeviceCalendar` still leaves the paper alone. `runMorning` stays insert-if-absent.
+- `EditionStore` — add `loadExtras(dayKey)` and `saveExtra`. Morning files stay `editions/{dayKey}-morning.json`.
+- `LocalEditionRepositoryTest.kt` `FakeStore` — in-memory extras. `by inner` delegates the new methods.
+- `FileEditionStore.jvm.kt` and `FileEditionStore.android.kt` — extras at `editions/extra-{dayKey}-{n}.json`. JS and wasm stubs compile.
+- `EditionViewModel.kt` — `generate()` still regenerates and shows the morning result. `newEditionNow`, `openExtra`, and `openMorning`. Task and typewriter saves follow the document on screen.
+- `EditionScreen.kt` and `MatutineApp.kt` — "New edition now" beside Settings, then the same-day list. `main.kt` tray stays three items.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` — keeps on regenerate; insert and list extras
+- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` — I/O rows for keeps, isolation, late snapshot, and failure
+- [x] `app/shared/src/jvmMain/.../FileEditionStore.jvm.kt` and `androidMain/.../FileEditionStore.android.kt` — persist extras; JS and wasm stubs
+- [x] `app/shared/src/jvmTest/.../FileEditionStoreTest.kt` — extra round-trip does not rewrite the morning file
+- [x] `EditionViewModel.kt`, `EditionScreen.kt`, `MatutineApp.kt` — button, list, open extra, return to morning
+- [x] `app/shared/src/commonTest/.../EditionGenerateStateTest.kt` — insert leaves morning on the front page; the list opens the extra
+
+**Acceptance Criteria:**
+- Given a rewritten briefing, a rewritten calendar line, and a checked task, when O generates today's morning edition again, then the id, briefing, line, and checked task remain, and stories and rank may change.
+- Given O asks for a new edition now, when it finishes, then a same-day extra exists, other editions are untouched, the front page is still today's morning edition, and the extra opens from a same-day list with no past days, no revision number, and no merge screen.
+- Given a device-calendar snapshot saved after today's morning edition exists, when O regenerates or asks for a new edition now, then those events are on that result and were not on the paper before the command.
+
+## Implementation Notes
+
+- Regenerate of a personal morning edition keeps its briefing and tasks, and appends calendar lines whose title and time are not both on the fresh event list. Stories and rank come from the new sources. A sample edition still becomes the personal paper.
+- New edition now writes `editions/extra-{dayKey}-{n}.json` and does not save the morning file. The edition screen lists today's extras by first headline, or Extra, and returns with Today's morning edition. The tray menu is unchanged.
+- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` — BUILD SUCCESSFUL. No emulator and no commit.
+- Review layers were not launched: this session cannot spawn a child agent. Standalone prompts are `review-2-6-blind-hunter-prompt.md`, `review-2-6-edge-case-hunter-prompt.md`, and `review-2-6-verification-gap-prompt.md`.
+
+## Spec Change Log
+
+## Review Triage Log
+
+## Design Notes
+
+Fresh events stay in source order. A morning calendar line is appended only when its title and time are not both on that list. Tasks are copied as they stand. The extra does not receive those edits. A list row is the extra's first headline, or Extra. "Today's morning edition" is the return row while an extra is open.
+
+## Verification
+
+**Commands:**
+- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
diff --git a/_bmad-output/implementation-artifacts/spec-2-7-write-the-briefing-with-a-gemini-key.md b/_bmad-output/implementation-artifacts/spec-2-7-write-the-briefing-with-a-gemini-key.md
new file mode 100644
index 0000000..33066bc
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-2-7-write-the-briefing-with-a-gemini-key.md
@@ -0,0 +1,86 @@
+---
+title: '2-7 Write the briefing with a Gemini key'
+type: 'feature'
+created: '2026-11-10'
+status: 'in-review'
+baseline_commit: 'fd8aa8ffad6e0a644fdeeee090c499ae251bbb5f'
+route: 'dispatch'
+review_loop_iteration: 0
+story_key: '2-7-write-the-briefing-with-a-gemini-key'
+context:
+  - _bmad-output/implementation-artifacts/epic-2-context.md
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** A saved free-tier Gemini key never writes the opening. Every generated briefing stays the extractive summary plus "Summary assembled from headlines."
+
+**Approach:** Settings stores an optional Gemini key in local config. A new morning paper and a new extra make one summarization call and store that text as the briefing. A missing key or a failed call leaves the extractive briefing and still saves the edition.
+
+## Boundaries & Constraints
+
+**Always:** The key lives only in local settings, never in the git tree, and is not sent to a Matutine server. At most one summarization call for each new briefing that is stored. Model text has no model name and no sparkle, and does not add the extractive sentence. Personal regenerate keeps the briefing already on that morning document and does not call. Sample load and sample retry stay on the bundled extractive briefing. Story 2.6's kept briefing, calendar lines, and tasks stay as they are. A disappeared source item gets no new merge rule.
+
+**Never:** OpenRouter, Claude, OpenAI, xAI, a local model, a provider picker, a paid-credit model, a chat box, a follow-up field, a second call after failure, or a key in source, tests, or fixtures.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Key writes a new paper | Local key set; first personal morning, including replacing a sample, or a new extra | Briefing is the model text; the call count for that document is one | N/A |
+| No key | Key blank or settings absent | Extractive summary plus "Summary assembled from headlines."; no call | Edition completes |
+| Call fails | Key set; call throws or returns blank | Same extractive briefing; no second call | Edition completes |
+| Regenerate | Personal morning already stored, key set | Existing briefing stays; no call | N/A |
+| Save key | Typed key, or a blank field | Non-blank key replaces the saved key; blank clears it | A failed write keeps the previous key |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `core/.../edition/ReaderSettings.kt` — add `geminiKey`, default `""`. Decode of older settings must keep working.
+- `core/.../edition/EditionRepository.kt` — `saveGeminiKey`. `LocalEditionRepository` takes a `summarize` lambda, same style as `readFeed`. Call it only when storing a new briefing and the trimmed key is non-blank. `keepMorningEdits` still owns personal regenerate. Sample paths do not call.
+- `core/.../edition/SampleGenerator.kt` — `extractiveBriefing` stays the fallback text. Do not call from `SampleGenerator.forDay`.
+- `core/.../edition/EditionModels.kt` — briefing remains one string. Do not add a model field.
+- `app/shared/.../edition/GeminiBriefing.kt` — prompt and response parse. No HTTP here.
+- `app/shared/.../edition/GeminiBriefingClient.kt` — expect/actual POST to the Gemini free-tier generateContent endpoint. Key in a header, not the prompt and not a Matutine server. JVM and Android implement it. JS and wasm fail so the fallback still completes.
+- `app/shared/.../MatutineApp.kt` and `app/androidApp/.../MorningEditionWorker.kt` — pass the client into the repository. Do not add a second generator.
+- `app/shared/.../edition/EditionViewModel.kt` and `settings/SettingsScreen.kt` — one masked "Gemini key" row. Save does not call the network. Generate persists a dirty key before it generates, and a failed save keeps the previous key.
+- `app/shared/.../edition/EditionScreen.kt` — briefing is already one read-only block. Do not add a model name, a sparkle, or a chat field.
+- Do not change `keepMorningEdits`, the typewriter, or the disappeared-source behavior.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `core/.../ReaderSettings.kt` and `EditionRepository.kt` — key field, save, and one optional summarization when a new briefing is stored
+- [x] `core/src/commonTest/.../LocalEditionRepositoryTest.kt` — matrix rows for key, missing key, failure, regenerate, extra, and sample
+- [x] `app/shared/.../GeminiBriefing.kt` plus JVM/Android/JS/wasm client — free-tier Gemini adapter and response parse test
+- [x] `EditionViewModel.kt`, `SettingsScreen.kt`, `MatutineApp.kt`, `MorningEditionWorker.kt` — masked key row wired into the same repository
+
+**Acceptance Criteria:**
+- Given a Gemini key in local config and not in the git tree, when a new edition is generated, then one summarization is stored as the briefing, with no model name and no sparkle.
+- Given the key is missing or the call fails, when generation finishes, then the briefing is the extractive summary plus "Summary assembled from headlines." and the edition completes.
+- Given a personal morning already has a briefing, when O generates again, then that briefing stays and no summarization runs.
+- Given Settings, when O saves a key, then no provider that needs paid credits can be selected, and there is no chat box.
+
+## Implementation Notes
+
+- The Gemini key is `ReaderSettings.geminiKey` in local `settings.json`. Save trims it. A blank value clears it. A thrown write leaves the previous document.
+- `withModelBriefing` runs only for a new morning briefing and a new extra. Personal regenerate still goes through `keepMorningEdits` and does not call. Sample load and retry do not call.
+- The adapter posts to the `gemini-2.5-flash` generateContent URL with `x-goog-api-key`. JVM and Android implement HTTP. JS and wasm fail the call so generation still finishes with the extractive briefing. No key string was added to the tree.
+- Settings shows one masked Gemini key row. Generate saves a dirty key first. A failed save restores the previous key and still generates.
+- Verified with `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` (BUILD SUCCESSFUL). No emulator.
+
+## Spec Change Log
+
+## Review Triage Log
+
+## Design Notes
+
+The port is a lambda, not a vendor type in `:core`. The adapter names `gemini-2.5-flash` and posts to `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent` with header `x-goog-api-key`. The prompt is the edition's headline and deck lines and asks for a short plain opening. The stored briefing is the joined candidate text. Personal regenerate skips the lambda because story 2.6 already keeps that field.
+
+## Verification
+
+**Commands:**
+- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.android.kt
new file mode 100644
index 0000000..ba8bd89
--- /dev/null
+++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.android.kt
@@ -0,0 +1,26 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.withContext
+import java.net.HttpURLConnection
+import java.net.URI
+
+actual suspend fun postGeminiGenerateContent(apiKey: String, body: String): String = withContext(Dispatchers.IO) {
+    val connection = (URI.create(GEMINI_GENERATE_URL).toURL().openConnection() as HttpURLConnection).apply {
+        instanceFollowRedirects = false
+        connectTimeout = 20_000
+        readTimeout = 20_000
+        requestMethod = "POST"
+        doOutput = true
+        setRequestProperty("Content-Type", "application/json; charset=utf-8")
+        setRequestProperty("Accept", "application/json")
+        setRequestProperty("x-goog-api-key", apiKey)
+    }
+    try {
+        connection.outputStream.use { it.write(body.encodeToByteArray()) }
+        if (connection.responseCode !in 200..299) error("summarizer_unavailable")
+        connection.inputStream.bufferedReader().use { it.readText() }
+    } finally {
+        connection.disconnect()
+    }
+}
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefing.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefing.kt
new file mode 100644
index 0000000..3f3a17c
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefing.kt
@@ -0,0 +1,83 @@
+package hu.toliver.matutine.edition
+
+const val GEMINI_GENERATE_URL =
+    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
+
+fun geminiBriefingPrompt(stories: List<EditionStory>): String {
+    val lines = stories.joinToString("\n") { story ->
+        val headline = story.headline.trim()
+        val deck = story.deck.trim()
+        if (deck.isEmpty()) headline else "$headline\n$deck"
+    }
+    return "Write a short newspaper briefing in plain prose from these headlines and decks. " +
+        "No title, no model name, and no list.\n$lines"
+}
+
+fun geminiGenerateBody(prompt: String): String =
+    """{"contents":[{"parts":[{"text":${jsonString(prompt)}}]}]}"""
+
+fun briefingTextFromGeminiResponse(body: String): String {
+    val text = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
+        .findAll(body)
+        .map { unescapeJsonString(it.groupValues[1]) }
+        .joinToString("")
+        .trim()
+    if (text.isEmpty()) error("empty_briefing")
+    return text
+}
+
+suspend fun summarizeWithGemini(stories: List<EditionStory>, apiKey: String): String {
+    val response = postGeminiGenerateContent(apiKey, geminiGenerateBody(geminiBriefingPrompt(stories)))
+    return briefingTextFromGeminiResponse(response)
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
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.kt
new file mode 100644
index 0000000..c04a68f
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.kt
@@ -0,0 +1,3 @@
+package hu.toliver.matutine.edition
+
+expect suspend fun postGeminiGenerateContent(apiKey: String, body: String): String
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/GeminiBriefingTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/GeminiBriefingTest.kt
new file mode 100644
index 0000000..8e69f18
--- /dev/null
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/GeminiBriefingTest.kt
@@ -0,0 +1,40 @@
+package hu.toliver.matutine.edition
+
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertTrue
+
+class GeminiBriefingTest {
+    private val stories = listOf(
+        EditionStory(
+            headline = "Markets open higher",
+            sourceName = "Personal Wire",
+            deck = "Traders watched the open.",
+            sourceUrl = "https://example.com/lead",
+            rank = StoryRank.lead,
+        ),
+    )
+
+    @Test
+    fun promptAndBodyStayOnTheFreeTierAndLeaveTheKeyOut() {
+        val prompt = geminiBriefingPrompt(stories)
+        val body = geminiGenerateBody(prompt)
+
+        assertTrue(prompt.contains("Markets open higher"))
+        assertTrue(prompt.contains("Traders watched the open."))
+        assertFalse(prompt.contains("Summary assembled from headlines."))
+        assertFalse(body.contains("configured"))
+        assertFalse(GEMINI_GENERATE_URL.contains("key="))
+        assertTrue(GEMINI_GENERATE_URL.contains("gemini-2.5-flash"))
+        assertFalse(GEMINI_GENERATE_URL.contains("pro"))
+    }
+
+    @Test
+    fun responseTextJoinsPartsAndUnescapes() {
+        val body = """{"candidates":[{"content":{"parts":[{"text":"The harbor is quiet."},{"text":" Ships\nmove."}]}}]}"""
+
+        assertEquals("The harbor is quiet. Ships\nmove.", briefingTextFromGeminiResponse(body))
+        assertTrue(runCatching { briefingTextFromGeminiResponse("""{"candidates":[]}""") }.isFailure)
+    }
+}
diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.js.kt
new file mode 100644
index 0000000..48e735b
--- /dev/null
+++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.js.kt
@@ -0,0 +1,6 @@
+package hu.toliver.matutine.edition
+
+actual suspend fun postGeminiGenerateContent(
+    @Suppress("UNUSED_PARAMETER") apiKey: String,
+    @Suppress("UNUSED_PARAMETER") body: String,
+): String = error("summarizer_unavailable")
diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.jvm.kt
new file mode 100644
index 0000000..ba8bd89
--- /dev/null
+++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.jvm.kt
@@ -0,0 +1,26 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.withContext
+import java.net.HttpURLConnection
+import java.net.URI
+
+actual suspend fun postGeminiGenerateContent(apiKey: String, body: String): String = withContext(Dispatchers.IO) {
+    val connection = (URI.create(GEMINI_GENERATE_URL).toURL().openConnection() as HttpURLConnection).apply {
+        instanceFollowRedirects = false
+        connectTimeout = 20_000
+        readTimeout = 20_000
+        requestMethod = "POST"
+        doOutput = true
+        setRequestProperty("Content-Type", "application/json; charset=utf-8")
+        setRequestProperty("Accept", "application/json")
+        setRequestProperty("x-goog-api-key", apiKey)
+    }
+    try {
+        connection.outputStream.use { it.write(body.encodeToByteArray()) }
+        if (connection.responseCode !in 200..299) error("summarizer_unavailable")
+        connection.inputStream.bufferedReader().use { it.readText() }
+    } finally {
+        connection.disconnect()
+    }
+}
diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.wasmJs.kt
new file mode 100644
index 0000000..48e735b
--- /dev/null
+++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/GeminiBriefingClient.wasmJs.kt
@@ -0,0 +1,6 @@
+package hu.toliver.matutine.edition
+
+actual suspend fun postGeminiGenerateContent(
+    @Suppress("UNUSED_PARAMETER") apiKey: String,
+    @Suppress("UNUSED_PARAMETER") body: String,
+): String = error("summarizer_unavailable")
`. Read that file — it is the content under review.

Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
