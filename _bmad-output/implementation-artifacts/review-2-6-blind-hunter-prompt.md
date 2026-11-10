Conduct a review of CONTENT.
Look for what's missing, not only what's wrong.
Compute your finding floor N from the diff file's size: N = min(floor(sqrt(kB) + 1), 10), where kB is the file's size in kilobytes. State the arithmetic in one line, then find at least N issues to fix or improve.
Output a Markdown list of findings only — no severity, priority, or ranking.
If the content is empty, stop and say so.
If you have zero findings, re-check and keep thinking; do not stop with an empty list.

CONTENT: the unified diff at the following contents. Read that file — it is the content under review.

diff --git a/_bmad-output/implementation-artifacts/sprint-status.yaml b/_bmad-output/implementation-artifacts/sprint-status.yaml
index ceed8f0..08ea20d 100644
--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
+++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
@@ -29,7 +29,7 @@
 # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
 # - Retrospective appends its action items to action_items; the status view surfaces open ones
 generated: 11-07-2026 19:42
-last_updated: 11-10-2026 08:37
+last_updated: 11-10-2026 18:11
 project: Matutine
 project_key: NOKEY
 tracking_system: file-system
@@ -55,7 +55,7 @@ development_status:
   2-3-customize-the-android-paper: done
   2-4-use-the-desktop-tray: done
   2-5-rewrite-the-edition-on-the-typewriter: done
-  2-6-regenerate-without-losing-the-edits: backlog
+  2-6-regenerate-without-losing-the-edits: in-progress
   2-7-write-the-briefing-with-a-gemini-key: backlog
   epic-2-retrospective: optional
 
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
index 7c6b5dc..7895a75 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
@@ -167,6 +167,10 @@ fun MatutineApp(
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
index acbabcc..fa54a3b 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
@@ -17,6 +17,7 @@ sealed interface EditionUiState {
         val generating: Boolean = false,
         val notice: String? = null,
         val taskSaved: Boolean = false,
+        val sameDayExtras: List<Edition> = emptyList(),
     ) : EditionUiState
 
     data class SampleLoadFailed(val message: String = "The sample edition didn't load.") : EditionUiState
@@ -279,7 +280,11 @@ class EditionViewModel(
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
@@ -291,11 +296,50 @@ class EditionViewModel(
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
@@ -338,7 +382,7 @@ class EditionViewModel(
         }
         val generation = ++typewriterGeneration
         scope.launch {
-            repository.updateMorningEdition(updated)
+            saveEdition(updated)
                 .onSuccess { saved ->
                     if (generation != typewriterGeneration) {
                         onResult(false)
@@ -378,7 +422,7 @@ class EditionViewModel(
         state = previous.copy(edition = updated)
         val rollback = previous
         scope.launch {
-            repository.updateMorningEdition(updated)
+            saveEdition(updated)
                 .onSuccess { saved ->
                     val current = state as? EditionUiState.Ready
                     if (current != null && current.edition.tasks == updated.tasks) {
@@ -394,6 +438,16 @@ class EditionViewModel(
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
     private fun calDavFieldsDirty(url: String, username: String, password: String): Boolean =
         password.isNotEmpty() || url.trim() != calDavUrlBaseline || username.trim() != calDavUsernameBaseline
 
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
index ee97379..58c6618 100644
--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
@@ -382,6 +382,49 @@ class EditionGenerateStateTest {
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
     private fun viewModel(repository: EditionRepository) =
         EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
 }
@@ -485,5 +528,25 @@ private class FakeEditionRepository(
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
index 1bc52ab..74b6c4a 100644
--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTaskStateTest.kt
@@ -122,6 +122,10 @@ class EditionTaskStateTest {
 
         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
 
+        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
+
+        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
+
         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
             updateCalls += 1
             val next = edition.copy(revision = morning.revision + 1)
@@ -129,5 +133,7 @@ class EditionTaskStateTest {
             lastSaved = next
             return Result.success(next)
         }
+
+        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
     }
 }
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
index e241ccb..257690c 100644
--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionTypewriterStateTest.kt
@@ -172,6 +172,10 @@ class EditionTypewriterStateTest {
 
         override suspend fun generateToday(): Result<Edition> = Result.success(morning)
 
+        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)
+
+        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
+
         override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
             gate?.await()
             if (fail) return Result.failure(IllegalStateException("save failed"))
@@ -180,5 +184,7 @@ class EditionTypewriterStateTest {
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
index d62d029..ccef4fb 100644
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
@@ -16,6 +16,8 @@ interface EditionStore {
 
     fun loadMorningEdition(dayKey: String): Edition?
     fun saveMorningEdition(edition: Edition)
+    fun loadExtras(dayKey: String): List<Edition>
+    fun saveExtra(edition: Edition)
 
     /** Null when the snapshot document is absent. */
     fun readCalendarSnapshot(): CalendarSnapshot?
@@ -261,12 +263,85 @@ class LocalEditionRepository(
         } else {
             edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
         }
+        val next = if (replaceExisting && existing != null && !existing.isSample) {
+            keepMorningEdits(existing, noted)
+        } else {
+            noted
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
+        val extra = noted.copy(
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
@@ -353,5 +428,18 @@ interface EditionRepository {
     fun savedEditionLayout(): String
     fun saveEditionLayout(layoutId: String): Result<Unit>
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
diff --git a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
index 711cd09..682a20d 100644
--- a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+++ b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
@@ -43,6 +43,21 @@ private class FakeStore(
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
@@ -428,6 +443,148 @@ class LocalEditionRepositoryTest {
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
diff --git a/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
new file mode 100644
index 0000000..7085344
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-2-6-regenerate-without-losing-the-edits.md
@@ -0,0 +1,82 @@
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

Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.
