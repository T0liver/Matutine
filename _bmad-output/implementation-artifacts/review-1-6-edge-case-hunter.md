Follow the review instructions below completely.

claims_file contents (leave unread until your instructions call for it — they are included here because this session has no shared filesystem):

```markdown
---
title: '1-6 Name topics and change the lead'
type: 'feature'
created: '2026-11-08'
status: 'in-review'
route: 'dispatch'
review_loop_iteration: 0
story_key: '1-6-name-topics-and-change-the-lead'
baseline_commit: '5b7cf66aa147856946ea8b84267839b7ac885baa'
context:
  - _bmad-output/implementation-artifacts/epic-1-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The lead still follows feed order. O cannot name what the paper should notice.

**Approach:** Interests, opened from Settings, saves topics on the settings document. The generator stores lead, secondary, and note from those topics. With no topics, source order stands and a line on the person's edition opens Interests. The sample launch does not.

## Boundaries & Constraints

**Always:** Ask "What do you want to read about?" and "What should the page notice about your day?" First question: Video games, Technology, Biology, Finance, Science, Sports, World news, Culture, Health, Local, plus a free line. Second: an open line and a control to add another. Many choices can be on. Selected: ink fill, paper label. Resting: field fill, ink label, 1px ink border. Height 40px on desktop and web, 48px on Android. Each selected choice and each non-empty line is one topic. A story fits when the topic is a case-insensitive substring of the headline or deck. More matches outrank fewer. Ties keep source order. Store that order with one lead, then secondary, then notes. The client draws the stored rank. Topics live on the settings document. Absent or empty topics means no topics. The line "What do you want to read about?" appears only on an edition from the person's sources.

**Never:** A model call, CalDAV, a server, a second source list, or a stored numeric score. Do not put topics on the source list or move Interests to Admin. Do not open Interests on the sample launch. Do not change the bundled sample. Do not put Compose in `:core`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Save topics | Technology and Video games on, free line "Rust", day lines "school" and blank | Topics are Technology, Video games, Rust, school. Source list unchanged. | N/A |
| Best fit | Those topics. Order is markets, a Video games story, then a story with both Technology and Video games | Both-topic story is the only lead and first. One-topic story is secondary. Markets is a note. | N/A |
| No topics | Person's feeds. Settings absent or topics empty | Source order. First story is the lead. The page line opens Interests. Sample line stays hidden. | N/A |
| Sample | Source list absent | Bundled sample. No Interests line. Topics are not written. | N/A |
| Clear topics | Save with nothing selected | Topics become empty. Next generate uses source order. The sample does not return. | N/A |

</frozen-after-approval>

## Code Map

- `core/.../edition/SampleGenerator.kt` — `rankForSourceOrder`. Reuse after ordering. Do not read topics.
- `core/.../edition/EditionGenerator.kt` — `fromFeedResults` is source order. Pass topics in. Store `StoryRank` only.
- `core/.../edition/EditionRepository.kt` — `generateToday` and `EditionStore`. Read topics when generating. Add settings read and save. Leave the absent-list sample path.
- `core/.../edition/SourceList.kt` — Feeds and calendars only.
- `app/shared/.../edition/EditionScreen.kt` — Draws stored order via `storiesForTwoColumns`. Add the no-topic line. Do not sort.
- `app/shared/.../settings/SettingsScreen.kt` — Add one Interests row. Keep the feed row and generate.
- `app/shared/.../MatutineApp.kt` — Open Interests from Settings and from the edition line. One repository.
- `app/shared/.../store/FileEditionStore.*.kt` — Add `settings.json` on JVM and Android. JS and Wasm accept the methods and store nothing.
- `core/.../edition/LocalEditionRepositoryTest.kt` — Keep absent-list, empty-list, and source-order tests. Add this matrix.

## Tasks & Acceptance

**Execution:**
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt` -- Settings document with defaulted topics.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt` -- Rank by topic fit, then source order.
- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- Save topics and pass them into generation.
- [x] `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt` -- Persist `settings.json`. Mirror on Android. JS and Wasm no-op.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/InterestsScreen.kt` -- Both questions, choices, lines, and one primary save.
- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- No-topic line on the person's edition only. Wire that line and the Settings row through `SettingsScreen`, `EditionViewModel`, and `MatutineApp`.
- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` -- Test the matrix.

**Acceptance Criteria:**
- Given Interests is opened from Settings, when it is shown, then both questions, the named choices, the free line, and add-another are present, and a selected choice uses ink fill with a paper label.
- Given Technology and Video games are saved and today's edition is generated, when it is drawn, then the stored lead is the best fit, then secondary, then note, with one lead.
- Given no topics, when the person's sources generate an edition, then source order stands and "What do you want to read about?" opens Interests.
- Given the first launch, when the sample is shown, then Interests is not opened.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Design Notes

Fit count orders stories and is not stored. Ties keep feed order, then `rankForSourceOrder` labels the reordered list. `ReaderSettings` is the settings document; this story writes `topics` only.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: success
- `./gradlew :core:compileKotlinJs :core:compileKotlinWasmJs :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` -- expected: success, or a reported Yarn download failure

```

Review content: the unified diff below.

Do not invoke any skill, and do not spawn subagents of your own — you are the reviewer. Return your findings as text in your final message; do not route them through any findings-reporting tool the host may offer.

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
index ab18f52..73c8302 100644
--- a/_bmad-output/implementation-artifacts/sprint-status.yaml
+++ b/_bmad-output/implementation-artifacts/sprint-status.yaml
@@ -29,7 +29,7 @@
 # - Dev moves story to 'review', then runs code-review (fresh context, different LLM recommended)
 # - Retrospective appends its action items to action_items; the status view surfaces open ones
 generated: 11-07-2026 19:42
-last_updated: 11-07-2026 21:10
+last_updated: 11-08-2026 13:10
 project: Matutine
 project_key: NOKEY
 tracking_system: file-system
@@ -39,9 +39,9 @@ development_status:
   1-1-open-the-sample-front-page: done
   1-2-draw-the-page-as-a-newspaper: done
   1-3-reflow-the-front-page-at-960px: done
-  1-4-build-the-sample-from-bundled-sources: backlog
-  1-5-generate-today-s-edition-from-a-feed: backlog
-  1-6-name-topics-and-change-the-lead: backlog
+  1-4-build-the-sample-from-bundled-sources: review
+  1-5-generate-today-s-edition-from-a-feed: review
+  1-6-name-topics-and-change-the-lead: in-progress
   1-7-show-today-s-events-from-ics-and-the-phone-calendar: backlog
   1-8-show-today-s-events-from-caldav: backlog
   1-9-keep-today-s-paper-on-the-next-morning-run: backlog
diff --git a/app/androidApp/src/main/AndroidManifest.xml b/app/androidApp/src/main/AndroidManifest.xml
index cdba621..f847667 100644
--- a/app/androidApp/src/main/AndroidManifest.xml
+++ b/app/androidApp/src/main/AndroidManifest.xml
@@ -1,13 +1,16 @@
 <?xml version="1.0" encoding="utf-8"?>
 <manifest xmlns:android="http://schemas.android.com/apk/res/android">
 
+    <uses-permission android:name="android.permission.INTERNET" />
+
     <application
             android:allowBackup="true"
             android:icon="@mipmap/ic_launcher"
             android:label="@string/app_name"
             android:roundIcon="@mipmap/ic_launcher_round"
             android:supportsRtl="true"
-            android:theme="@android:style/Theme.Material.Light.NoActionBar">
+            android:theme="@android:style/Theme.Material.Light.NoActionBar"
+            android:usesCleartextTraffic="true">
         <activity
                 android:exported="true"
                 android:name=".MainActivity">
diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
index 12ce25a..59de051 100644
--- a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
+++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/store/FileEditionStore.android.kt
@@ -3,8 +3,14 @@ package hu.toliver.matutine.store
 import android.content.Context
 import hu.toliver.matutine.edition.Edition
 import hu.toliver.matutine.edition.EditionRole
+import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SourceList
 import hu.toliver.matutine.edition.decodeEdition
+import hu.toliver.matutine.edition.decodeReaderSettings
+import hu.toliver.matutine.edition.decodeSourceList
 import hu.toliver.matutine.edition.encodeEdition
+import hu.toliver.matutine.edition.encodeReaderSettings
+import hu.toliver.matutine.edition.encodeSourceList
 import java.io.File
 
 actual class FileEditionStore actual constructor(
@@ -16,7 +22,25 @@ actual class FileEditionStore actual constructor(
         root.mkdirs()
     }
 
-    override fun hasSourceList(): Boolean = File(root, "sources.json").exists()
+    override fun readSourceList(): SourceList? {
+        val file = File(root, "sources.json")
+        if (!file.exists()) return null
+        return decodeSourceList(file.readText())
+    }
+
+    override fun writeSourceList(sourceList: SourceList) {
+        File(root, "sources.json").writeText(encodeSourceList(sourceList))
+    }
+
+    override fun readSettings(): ReaderSettings? {
+        val file = File(root, "settings.json")
+        if (!file.exists()) return null
+        return decodeReaderSettings(file.readText())
+    }
+
+    override fun writeSettings(settings: ReaderSettings) {
+        File(root, "settings.json").writeText(encodeReaderSettings(settings))
+    }
 
     override fun loadMorningEdition(dayKey: String): Edition? {
         val file = File(root, "editions/$dayKey-morning.json")
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
index d74cea9..661ab4a 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt
@@ -1,26 +1,80 @@
 package hu.toliver.matutine
 
 import androidx.compose.runtime.Composable
+import androidx.compose.runtime.getValue
+import androidx.compose.runtime.mutableStateOf
 import androidx.compose.runtime.remember
+import androidx.compose.runtime.setValue
 import androidx.compose.ui.tooling.preview.Preview
 import hu.toliver.matutine.edition.EditionScreen
+import hu.toliver.matutine.edition.EditionUiState
 import hu.toliver.matutine.edition.EditionViewModel
 import hu.toliver.matutine.edition.LocalEditionRepository
+import hu.toliver.matutine.edition.readFeedDocument
+import hu.toliver.matutine.edition.showsNoTopicLine
+import hu.toliver.matutine.settings.InterestsScreen
+import hu.toliver.matutine.settings.SettingsScreen
 import hu.toliver.matutine.store.FileEditionStore
 import hu.toliver.matutine.theme.MatutineTheme
 
+private sealed interface MatutineScreen {
+    data object Edition : MatutineScreen
+    data object Settings : MatutineScreen
+    data class Interests(val returnToSettings: Boolean) : MatutineScreen
+}
+
 @Composable
 fun MatutineApp(dataRoot: String) {
     val viewModel = remember(dataRoot) {
         val store = FileEditionStore(dataRoot)
-        val repository = LocalEditionRepository(store)
+        val repository = LocalEditionRepository(store, readFeed = ::readFeedDocument)
         EditionViewModel(repository)
     }
+    var screen by remember { mutableStateOf<MatutineScreen>(MatutineScreen.Edition) }
+    val ready = viewModel.state as? EditionUiState.Ready
     MatutineTheme {
-        EditionScreen(
-            state = viewModel.state,
-            onRetrySample = viewModel::retrySample,
-        )
+        when (val current = screen) {
+            MatutineScreen.Settings -> SettingsScreen(
+                feedUrl = viewModel.feedUrl,
+                onFeedUrlChange = viewModel::updateFeedUrl,
+                feedError = viewModel.feedFieldError,
+                onSaveFeed = viewModel::saveFeed,
+                onOpenInterests = { screen = MatutineScreen.Interests(returnToSettings = true) },
+                onGenerate = {
+                    viewModel.generate()
+                    screen = MatutineScreen.Edition
+                },
+                onBack = { screen = MatutineScreen.Edition },
+            )
+            is MatutineScreen.Interests -> InterestsScreen(
+                topics = viewModel.topics,
+                onSave = { topics ->
+                    if (viewModel.saveTopics(topics)) {
+                        screen = if (current.returnToSettings) {
+                            MatutineScreen.Settings
+                        } else {
+                            MatutineScreen.Edition
+                        }
+                    }
+                },
+                onBack = {
+                    screen = if (current.returnToSettings) {
+                        MatutineScreen.Settings
+                    } else {
+                        MatutineScreen.Edition
+                    }
+                },
+            )
+            MatutineScreen.Edition -> EditionScreen(
+                state = viewModel.state,
+                onRetrySample = viewModel::retrySample,
+                onOpenSettings = { screen = MatutineScreen.Settings },
+                showNoTopicLine = ready?.let {
+                    showsNoTopicLine(it.edition.isSample, viewModel.topics)
+                } == true,
+                onOpenInterests = { screen = MatutineScreen.Interests(returnToSettings = false) },
+            )
+        }
     }
 }
 
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
index 1bd7af0..08f7501 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionCopy.kt
@@ -4,4 +4,10 @@ internal object EditionCopy {
     const val NO_STORIES = "No stories yet."
     const val NO_EVENTS = "No events today."
     const val NO_TASKS = "No tasks yet."
+    const val SETTING_TODAYS_EDITION = "Setting today's edition."
+    const val WHAT_DO_YOU_WANT_TO_READ_ABOUT = "What do you want to read about?"
+    const val WHAT_SHOULD_THE_PAGE_NOTICE = "What should the page notice about your day?"
 }
+
+internal fun showsNoTopicLine(isSample: Boolean, topics: List<String>): Boolean =
+    !isSample && topics.none { it.isNotBlank() }
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
index d81b466..aa6284f 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt
@@ -13,6 +13,7 @@ import androidx.compose.foundation.layout.fillMaxHeight
 import androidx.compose.foundation.layout.fillMaxSize
 import androidx.compose.foundation.layout.fillMaxWidth
 import androidx.compose.foundation.layout.height
+import androidx.compose.foundation.layout.heightIn
 import androidx.compose.foundation.layout.padding
 import androidx.compose.foundation.layout.safeContentPadding
 import androidx.compose.foundation.layout.size
@@ -25,6 +26,7 @@ import androidx.compose.material3.ButtonDefaults
 import androidx.compose.material3.CircularProgressIndicator
 import androidx.compose.material3.HorizontalDivider
 import androidx.compose.material3.Text
+import androidx.compose.material3.TextButton
 import androidx.compose.material3.VerticalDivider
 import androidx.compose.material3.minimumInteractiveComponentSize
 import androidx.compose.runtime.Composable
@@ -46,6 +48,9 @@ import hu.toliver.matutine.theme.NewsprintColors
 fun EditionScreen(
     state: EditionUiState,
     onRetrySample: () -> Unit,
+    onOpenSettings: () -> Unit,
+    showNoTopicLine: Boolean,
+    onOpenInterests: () -> Unit,
     modifier: Modifier = Modifier,
 ) {
     val typography = LocalMatutineEditionTypography.current
@@ -53,7 +58,16 @@ fun EditionScreen(
         EditionUiState.Loading -> LoadingEdition(modifier)
         is EditionUiState.SampleLoadFailed -> SampleLoadError(onRetrySample, typography, modifier)
         is EditionUiState.OtherError -> OtherError(state.message, typography, modifier)
-        is EditionUiState.Ready -> EditionContent(state.edition, typography, modifier)
+        is EditionUiState.Ready -> EditionContent(
+            edition = state.edition,
+            generating = state.generating,
+            notice = state.notice,
+            showNoTopicLine = showNoTopicLine,
+            onOpenInterests = onOpenInterests,
+            onOpenSettings = onOpenSettings,
+            typography = typography,
+            modifier = modifier,
+        )
     }
 }
 
@@ -120,6 +134,11 @@ private fun OtherError(message: String, typography: MatutineEditionTypography, m
 @Composable
 private fun EditionContent(
     edition: Edition,
+    generating: Boolean,
+    notice: String?,
+    showNoTopicLine: Boolean,
+    onOpenInterests: () -> Unit,
+    onOpenSettings: () -> Unit,
     typography: MatutineEditionTypography,
     modifier: Modifier = Modifier,
 ) {
@@ -139,12 +158,47 @@ private fun EditionContent(
                 .padding(horizontal = 16.dp, vertical = 16.dp),
             verticalArrangement = Arrangement.spacedBy(16.dp),
         ) {
+            TextButton(
+                onClick = onOpenSettings,
+                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+            ) {
+                Text(text = "Settings", style = typography.body.copy(color = NewsprintColors.ink))
+            }
             Masthead(edition.mastheadName, edition.dateLine, typography)
+            if (generating) {
+                Text(
+                    text = EditionCopy.SETTING_TODAYS_EDITION,
+                    style = typography.body,
+                )
+            }
+            if (!notice.isNullOrBlank()) {
+                Text(
+                    text = notice,
+                    style = typography.body.copy(color = NewsprintColors.error),
+                )
+            }
+            edition.sourceNotes.forEach { note ->
+                Text(
+                    text = note,
+                    style = typography.body.copy(color = NewsprintColors.error),
+                )
+            }
             if (edition.isSample) {
                 Text(
                     text = "SAMPLE EDITION",
                     style = typography.kicker.copy(color = NewsprintColors.accent),
                 )
+            } else if (showNoTopicLine) {
+                TextButton(
+                    onClick = onOpenInterests,
+                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
+                    colors = ButtonDefaults.textButtonColors(contentColor = NewsprintColors.ink),
+                ) {
+                    Text(
+                        text = EditionCopy.WHAT_DO_YOU_WANT_TO_READ_ABOUT,
+                        style = typography.body.copy(color = NewsprintColors.ink),
+                    )
+                }
             }
             if (wide) {
                 WideEditionBody(edition, typography)
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
index dfb0800..10813fb 100644
--- a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt
@@ -11,7 +11,11 @@ import kotlinx.coroutines.launch
 sealed interface EditionUiState {
     data object Loading : EditionUiState
 
-    data class Ready(val edition: Edition) : EditionUiState
+    data class Ready(
+        val edition: Edition,
+        val generating: Boolean = false,
+        val notice: String? = null,
+    ) : EditionUiState
 
     data class SampleLoadFailed(val message: String = "The sample edition didn't load.") : EditionUiState
 
@@ -25,10 +29,78 @@ class EditionViewModel(
     var state: EditionUiState by mutableStateOf(EditionUiState.Loading)
         private set
 
+    var feedUrl by mutableStateOf("")
+        private set
+
+    var feedFieldError by mutableStateOf<String?>(null)
+        private set
+
+    var topics by mutableStateOf<List<String>>(emptyList())
+        private set
+
+    private var feedBaseline: String = ""
+
     init {
+        feedBaseline = runCatching { repository.savedFeedUrls().singleOrNull().orEmpty() }.getOrDefault("")
+        feedUrl = feedBaseline
+        topics = runCatching { repository.savedTopics() }.getOrDefault(emptyList())
         load()
     }
 
+    fun updateFeedUrl(value: String) {
+        if (value != feedUrl) feedFieldError = null
+        feedUrl = value
+    }
+
+    fun saveFeed() {
+        val typed = feedUrl
+        scope.launch {
+            repository.saveFeedUrl(typed)
+                .onSuccess {
+                    feedBaseline = typed.trim()
+                    if (feedUrl == typed) feedUrl = feedBaseline
+                    feedFieldError = null
+                }
+                .onFailure {
+                    if (feedUrl == typed) feedFieldError = FEED_DIDNT_LOAD
+                }
+        }
+    }
+
+    fun saveTopics(values: List<String>): Boolean =
+        runCatching {
+            repository.saveTopics(values)
+            topics = repository.savedTopics()
+        }.isSuccess
+
+    fun generate() {
+        val current = state as? EditionUiState.Ready ?: return
+        if (current.generating) return
+        val typed = feedUrl
+        state = current.copy(generating = true, notice = null)
+        scope.launch {
+            if (typed.isNotBlank() && typed.trim() != feedBaseline) {
+                val saved = repository.saveFeedUrl(typed)
+                if (saved.isFailure) {
+                    if (feedUrl == typed) feedFieldError = FEED_DIDNT_LOAD
+                    state = current.copy(generating = false, notice = FEED_DIDNT_LOAD)
+                    return@launch
+                }
+                feedBaseline = typed.trim()
+                if (feedUrl == typed) feedUrl = feedBaseline
+                feedFieldError = null
+            }
+            repository.generateToday()
+                .onSuccess { edition ->
+                    feedFieldError = null
+                    state = EditionUiState.Ready(edition)
+                }
+                .onFailure {
+                    state = current.copy(generating = false, notice = FEED_DIDNT_LOAD)
+                }
+        }
+    }
+
     fun load() {
         state = EditionUiState.Loading
         scope.launch {
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionCopyTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionCopyTest.kt
index 5f902f2..eba684e 100644
--- a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionCopyTest.kt
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionCopyTest.kt
@@ -1,7 +1,12 @@
 package hu.toliver.matutine.edition
 
+import hu.toliver.matutine.settings.ADD_ANOTHER
+import hu.toliver.matutine.settings.InterestChoices
+import hu.toliver.matutine.settings.SAVE_TOPICS
 import kotlin.test.Test
 import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertTrue
 
 class EditionCopyTest {
 
@@ -10,5 +15,34 @@ class EditionCopyTest {
         assertEquals("No stories yet.", EditionCopy.NO_STORIES)
         assertEquals("No events today.", EditionCopy.NO_EVENTS)
         assertEquals("No tasks yet.", EditionCopy.NO_TASKS)
+        assertEquals("Setting today's edition.", EditionCopy.SETTING_TODAYS_EDITION)
+        assertEquals("What do you want to read about?", EditionCopy.WHAT_DO_YOU_WANT_TO_READ_ABOUT)
+        assertEquals("What should the page notice about your day?", EditionCopy.WHAT_SHOULD_THE_PAGE_NOTICE)
+        assertEquals("Add another", ADD_ANOTHER)
+        assertEquals("Save", SAVE_TOPICS)
+        assertEquals(
+            listOf(
+                "Video games",
+                "Technology",
+                "Biology",
+                "Finance",
+                "Science",
+                "Sports",
+                "World news",
+                "Culture",
+                "Health",
+                "Local",
+            ),
+            InterestChoices,
+        )
+    }
+
+    @Test
+    fun noTopicLineOpensInterestsOnlyOnThePersonsEdition() {
+        assertTrue(showsNoTopicLine(isSample = false, topics = emptyList()))
+        assertTrue(showsNoTopicLine(isSample = false, topics = listOf("  ")))
+        assertFalse(showsNoTopicLine(isSample = true, topics = emptyList()))
+        assertFalse(showsNoTopicLine(isSample = true, topics = listOf("Technology")))
+        assertFalse(showsNoTopicLine(isSample = false, topics = listOf("Technology", "Video games")))
     }
 }
diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
index a0ca95f..48ac197 100644
--- a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
+++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.js.kt
@@ -1,11 +1,19 @@
 package hu.toliver.matutine.store
 
 import hu.toliver.matutine.edition.Edition
+import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SourceList
 
 actual class FileEditionStore actual constructor(
     @Suppress("UNUSED_PARAMETER") rootPath: String,
 ) : hu.toliver.matutine.edition.EditionStore {
-    override fun hasSourceList(): Boolean = false
+    override fun readSourceList(): SourceList? = null
+
+    override fun writeSourceList(sourceList: SourceList) = Unit
+
+    override fun readSettings(): ReaderSettings? = null
+
+    override fun writeSettings(settings: ReaderSettings) = Unit
 
     override fun loadMorningEdition(dayKey: String): Edition? = null
 
diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
index 8df2f01..1a4301a 100644
--- a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
+++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt
@@ -2,8 +2,14 @@ package hu.toliver.matutine.store
 
 import hu.toliver.matutine.edition.Edition
 import hu.toliver.matutine.edition.EditionRole
+import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SourceList
 import hu.toliver.matutine.edition.decodeEdition
+import hu.toliver.matutine.edition.decodeReaderSettings
+import hu.toliver.matutine.edition.decodeSourceList
 import hu.toliver.matutine.edition.encodeEdition
+import hu.toliver.matutine.edition.encodeReaderSettings
+import hu.toliver.matutine.edition.encodeSourceList
 import java.io.File
 
 actual class FileEditionStore actual constructor(
@@ -15,7 +21,25 @@ actual class FileEditionStore actual constructor(
         root.mkdirs()
     }
 
-    override fun hasSourceList(): Boolean = File(root, "sources.json").exists()
+    override fun readSourceList(): SourceList? {
+        val file = File(root, "sources.json")
+        if (!file.exists()) return null
+        return decodeSourceList(file.readText())
+    }
+
+    override fun writeSourceList(sourceList: SourceList) {
+        File(root, "sources.json").writeText(encodeSourceList(sourceList))
+    }
+
+    override fun readSettings(): ReaderSettings? {
+        val file = File(root, "settings.json")
+        if (!file.exists()) return null
+        return decodeReaderSettings(file.readText())
+    }
+
+    override fun writeSettings(settings: ReaderSettings) {
+        File(root, "settings.json").writeText(encodeReaderSettings(settings))
+    }
 
     override fun loadMorningEdition(dayKey: String): Edition? {
         val file = File(root, "editions/$dayKey-morning.json")
diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
index a0ca95f..48ac197 100644
--- a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
+++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/store/FileEditionStore.wasmJs.kt
@@ -1,11 +1,19 @@
 package hu.toliver.matutine.store
 
 import hu.toliver.matutine.edition.Edition
+import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SourceList
 
 actual class FileEditionStore actual constructor(
     @Suppress("UNUSED_PARAMETER") rootPath: String,
 ) : hu.toliver.matutine.edition.EditionStore {
-    override fun hasSourceList(): Boolean = false
+    override fun readSourceList(): SourceList? = null
+
+    override fun writeSourceList(sourceList: SourceList) = Unit
+
+    override fun readSettings(): ReaderSettings? = null
+
+    override fun writeSettings(settings: ReaderSettings) = Unit
 
     override fun loadMorningEdition(dayKey: String): Edition? = null
 
diff --git a/core/build.gradle.kts b/core/build.gradle.kts
index a1c6903..6d6a6ac 100644
--- a/core/build.gradle.kts
+++ b/core/build.gradle.kts
@@ -33,7 +33,7 @@ kotlin {
     sourceSets {
         commonMain.dependencies {
             implementation(libs.kotlinx.serialization.json)
-            implementation(libs.kotlinx.datetime)
+            api(libs.kotlinx.datetime)
         }
         commonTest.dependencies {
             implementation(libs.kotlin.test)
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/DayKey.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/DayKey.kt
index 9e6e7fb..34bc44f 100644
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/DayKey.kt
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/DayKey.kt
@@ -1,6 +1,6 @@
 package hu.toliver.matutine.edition
 
-import kotlinx.datetime.Clock
+import kotlin.time.Clock
 import kotlinx.datetime.LocalDate
 import kotlinx.datetime.TimeZone
 import kotlinx.datetime.todayIn
@@ -13,5 +13,5 @@ fun LocalDate.toDayKey(): String = toString()
 fun LocalDate.toDateLine(): String {
     val weekday = dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase)
     val month = month.name.lowercase().replaceFirstChar(Char::uppercase)
-    return "$weekday, $month $dayOfMonth, $year"
+    return "$weekday, $month $day, $year"
 }
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionModels.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionModels.kt
index fedfa36..f9bbea9 100644
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionModels.kt
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionModels.kt
@@ -49,4 +49,5 @@ data class Edition(
     val stories: List<EditionStory>,
     val events: List<EditionEvent>,
     val tasks: List<EditionTask>,
+    val sourceNotes: List<String> = emptyList(),
 )
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
index fd87e1d..509a86a 100644
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt
@@ -3,7 +3,16 @@ package hu.toliver.matutine.edition
 import kotlinx.datetime.LocalDate
 
 interface EditionStore {
-    fun hasSourceList(): Boolean
+    /** Null when the source-list document is absent. An empty document is present. */
+    fun readSourceList(): SourceList?
+
+    fun writeSourceList(sourceList: SourceList)
+
+    /** Null when the settings document is absent. An empty topic list is present. */
+    fun readSettings(): ReaderSettings?
+
+    fun writeSettings(settings: ReaderSettings)
+
     fun loadMorningEdition(dayKey: String): Edition?
     fun saveMorningEdition(edition: Edition)
 }
@@ -12,12 +21,14 @@ class LocalEditionRepository(
     private val store: EditionStore,
     private val today: () -> LocalDate = { todayLocalDate() },
     private val forceSampleLoadFailure: () -> Boolean = { false },
+    private val generateSample: (LocalDate) -> Edition = SampleGenerator::forDay,
+    private val readFeed: suspend (String) -> String = { error(FEED_DIDNT_LOAD) },
 ) : EditionRepository {
     override suspend fun loadCurrentMorningEdition(): Result<Edition> = runCatching {
         val dayKey = today().toDayKey()
         store.loadMorningEdition(dayKey)?.let { return@runCatching it }
 
-        if (store.hasSourceList()) {
+        if (store.readSourceList() != null) {
             error("Today's morning edition is not available yet.")
         }
 
@@ -25,23 +36,92 @@ class LocalEditionRepository(
             error("sample_load_failed")
         }
 
-        SampleEdition.forDay(today()).also { sample ->
+        generateSample(today()).also { sample ->
             store.saveMorningEdition(sample)
         }
     }
 
     override suspend fun retrySample(): Result<Edition> = runCatching {
-        if (store.hasSourceList()) {
+        if (store.readSourceList() != null) {
             error("Sample is not used when a source list exists.")
         }
         if (forceSampleLoadFailure()) {
             error("sample_load_failed")
         }
-        SampleEdition.forDay(today()).also { store.saveMorningEdition(it) }
+        generateSample(today()).also { store.saveMorningEdition(it) }
+    }
+
+    override fun savedFeedUrls(): List<String> = store.readSourceList()?.feeds.orEmpty()
+
+    override fun savedTopics(): List<String> = store.readSettings()?.topics.orEmpty()
+
+    override fun saveTopics(topics: List<String>) {
+        store.writeSettings(
+            ReaderSettings(
+                topics = topics.map { it.trim() }.filter { it.isNotEmpty() },
+            ),
+        )
     }
+
+    override suspend fun saveFeedUrl(url: String): Result<Unit> = runCatching {
+        val trimmed = url.trim()
+        if (trimmed.isEmpty()) error(FEED_DIDNT_LOAD)
+        val document = readDocument(trimmed)
+        try {
+            parseFeedDocument(document)
+        } catch (error: Throwable) {
+            rethrowCancellation(error)
+            error(FEED_DIDNT_LOAD)
+        }
+        val current = store.readSourceList() ?: SourceList()
+        if (current.feeds == listOf(trimmed)) return@runCatching
+        store.writeSourceList(
+            current.copy(
+                id = current.id.ifBlank { "sources" },
+                revision = current.revision + 1,
+                feeds = listOf(trimmed),
+            ),
+        )
+    }
+
+    override suspend fun generateToday(): Result<Edition> = runCatching {
+        val list = store.readSourceList()
+        if (list == null || list.feeds.isEmpty()) {
+            error(FEED_DIDNT_LOAD)
+        }
+        val date = today()
+        val parsed = list.feeds.map { url ->
+            runCatching { parseFeedDocument(readDocument(url)) }
+        }
+        val edition = EditionGenerator.fromFeedResults(
+            date = date,
+            parsed = parsed,
+            existing = store.loadMorningEdition(date.toDayKey()),
+            topics = store.readSettings()?.topics.orEmpty(),
+        )
+        store.saveMorningEdition(edition)
+        edition
+    }
+
+    private suspend fun readDocument(url: String): String =
+        try {
+            readFeed(url)
+        } catch (error: Throwable) {
+            rethrowCancellation(error)
+            error(FEED_DIDNT_LOAD)
+        }
+}
+
+private fun rethrowCancellation(error: Throwable) {
+    if (error is kotlin.coroutines.cancellation.CancellationException) throw error
 }
 
 interface EditionRepository {
     suspend fun loadCurrentMorningEdition(): Result<Edition>
     suspend fun retrySample(): Result<Edition>
+    fun savedFeedUrls(): List<String>
+    suspend fun saveFeedUrl(url: String): Result<Unit>
+    fun savedTopics(): List<String>
+    fun saveTopics(topics: List<String>)
+    suspend fun generateToday(): Result<Edition>
 }
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SampleEdition.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SampleEdition.kt
deleted file mode 100644
index 3833344..0000000
--- a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SampleEdition.kt
+++ /dev/null
@@ -1,53 +0,0 @@
-package hu.toliver.matutine.edition
-
-import kotlinx.datetime.LocalDate
-
-object SampleEdition {
-    fun forDay(date: LocalDate): Edition {
-        val dayKey = date.toDayKey()
-        return Edition(
-            id = "sample-$dayKey",
-            revision = 1,
-            dayKey = dayKey,
-            role = EditionRole.morning,
-            mastheadName = "Matutine",
-            dateLine = date.toDateLine(),
-            isSample = true,
-            briefing =
-                "Overnight wires bring a mixed picture: labor markets hold steady while " +
-                    "policy makers signal patience on rates. Markets open attentively on " +
-                    "earnings and a light data calendar.",
-            stories = listOf(
-                EditionStory(
-                    headline = "Central banks signal a longer pause on rates",
-                    sourceName = "Sample Wire",
-                    deck = "Officials emphasize data dependence as inflation cools unevenly across regions.",
-                    sourceUrl = "https://example.com/sample/lead",
-                    rank = StoryRank.lead,
-                ),
-                EditionStory(
-                    headline = "Tech firms outline cautious hiring plans",
-                    sourceName = "Sample Herald",
-                    deck = "Quarterly updates point to selective growth rather than broad expansion.",
-                    sourceUrl = "https://example.com/sample/secondary",
-                    rank = StoryRank.secondary,
-                ),
-                EditionStory(
-                    headline = "Brief: Transit upgrades set for weekend",
-                    sourceName = "City Desk",
-                    deck = "A single track closure may add fifteen minutes to cross-town trips.",
-                    sourceUrl = "https://example.com/sample/note",
-                    rank = StoryRank.note,
-                ),
-            ),
-            events = listOf(
-                EditionEvent(title = "Team standup", timeLabel = "9:00"),
-                EditionEvent(title = "Lunch with Alex", timeLabel = "12:30"),
-            ),
-            tasks = listOf(
-                EditionTask(title = "Skim the overnight wires"),
-                EditionTask(title = "Confirm dentist appointment"),
-            ),
-        )
-    }
-}
diff --git a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
index 1e026bd..5e39333 100644
--- a/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
+++ b/core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt
@@ -5,42 +5,578 @@ import kotlinx.datetime.LocalDate
 import kotlin.test.Test
 import kotlin.test.assertEquals
 import kotlin.test.assertFalse
+import kotlin.test.assertNull
 import kotlin.test.assertTrue
 
 private class FakeStore(
     private var morning: Edition? = null,
-    private var sourceList: Boolean = false,
+    private var sourceList: SourceList? = null,
+    private var settings: ReaderSettings? = null,
 ) : EditionStore {
-    override fun hasSourceList(): Boolean = sourceList
+    var saved: Edition? = null
+
+    override fun readSourceList(): SourceList? = sourceList
+
+    override fun writeSourceList(sourceList: SourceList) {
+        this.sourceList = sourceList
+    }
+
+    override fun readSettings(): ReaderSettings? = settings
+
+    override fun writeSettings(settings: ReaderSettings) {
+        this.settings = settings
+    }
 
     override fun loadMorningEdition(dayKey: String): Edition? =
         morning?.takeIf { it.dayKey == dayKey && it.role == EditionRole.morning }
 
     override fun saveMorningEdition(edition: Edition) {
         morning = edition
+        saved = edition
     }
 }
 
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
+    <item>
+      <title>Port reopens</title>
+      <link>https://example.com/second</link>
+      <description>Cargo is moving again</description>
+    </item>
+  </channel>
+</rss>
+"""
+
+private const val PERSONAL_ATOM = """
+<?xml version="1.0"?>
+<feed xmlns="http://www.w3.org/2005/Atom">
+  <title>Atom Desk</title>
+  <entry>
+    <title>Atom lead</title>
+    <link rel="alternate" href="https://example.com/atom"/>
+    <summary>Atom deck</summary>
+  </entry>
+</feed>
+"""
+
+private const val NOT_A_FEED = "<html><body>nope</body></html>"
+
+private const val TOPIC_RSS = """
+<?xml version="1.0"?>
+<rss version="2.0">
+  <channel>
+    <title>Desk</title>
+    <item>
+      <title>Markets open higher</title>
+      <link>https://example.com/markets</link>
+      <description>Traders watched the open.</description>
+    </item>
+    <item>
+      <title>A studio ships video games</title>
+      <link>https://example.com/games</link>
+      <description>Players lined up.</description>
+    </item>
+    <item>
+      <title>TECHNOLOGY firms buy studios</title>
+      <link>https://example.com/both</link>
+      <description>A week of Video Games.</description>
+    </item>
+  </channel>
+</rss>
+"""
+
+private const val TIE_RSS = """
+<?xml version="1.0"?>
+<rss version="2.0">
+  <channel>
+    <title>Desk</title>
+    <item>
+      <title>Studio ships a sequel</title>
+      <link>https://example.com/games</link>
+      <description>video games players lined up.</description>
+    </item>
+    <item>
+      <title>Another studio follows</title>
+      <link>https://example.com/games-two</link>
+      <description>More video games news.</description>
+    </item>
+    <item>
+      <title>Markets open higher</title>
+      <link>https://example.com/markets</link>
+      <description>Traders watched the open.</description>
+    </item>
+  </channel>
+</rss>
+"""
+
 class LocalEditionRepositoryTest {
     private val sampleDay = LocalDate(2026, 11, 7)
 
     @Test
-    fun firstLaunchWithoutSourceList_servesSample() = runBlocking {
-        val repo = LocalEditionRepository(FakeStore(), today = { sampleDay })
+    fun absentSourceList_buildsBundledSampleAndDoesNotWriteList() = runBlocking {
+        val store = FakeStore()
+        val repo = LocalEditionRepository(store, today = { sampleDay })
         val edition = repo.loadCurrentMorningEdition().getOrThrow()
+        val built = SampleGenerator.forDay(sampleDay)
+
         assertTrue(edition.isSample)
-        assertEquals("Matutine", edition.mastheadName)
-        assertEquals("2026-11-07", edition.dayKey)
-        assertTrue(edition.stories.any { it.rank == StoryRank.lead })
-        assertTrue(edition.stories.any { it.rank == StoryRank.secondary })
+        assertEquals(built.stories.map { it.headline }, edition.stories.map { it.headline })
+        assertEquals(built.events, edition.events)
+        assertEquals(StoryRank.lead, edition.stories.first().rank)
+        assertEquals(1, edition.stories.count { it.rank == StoryRank.lead })
+        assertTrue(edition.briefing.endsWith("Summary assembled from headlines."))
+        assertNull(store.readSourceList())
+        assertNull(store.readSettings())
+        assertEquals(edition.id, store.saved?.id)
     }
 
     @Test
-    fun savedMorningEdition_preferredOverSample() = runBlocking {
-        val saved = SampleEdition.forDay(sampleDay).copy(id = "saved", isSample = false)
-        val repo = LocalEditionRepository(FakeStore(morning = saved), today = { sampleDay })
+    fun savedMorningEdition_isReturnedWithoutBuildingSample() = runBlocking {
+        var generated = false
+        val saved = SampleGenerator.forDay(sampleDay).copy(id = "saved", isSample = false, briefing = "Kept")
+        val store = FakeStore(morning = saved)
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            generateSample = {
+                generated = true
+                error("sample")
+            },
+        )
         val edition = repo.loadCurrentMorningEdition().getOrThrow()
         assertEquals("saved", edition.id)
+        assertEquals("Kept", edition.briefing)
+        assertFalse(edition.isSample)
+        assertFalse(generated)
+        assertNull(store.saved)
+    }
+
+    @Test
+    fun emptyOrPresentSourceList_doesNotUseBundledSample() = runBlocking {
+        listOf(
+            SourceList(),
+            SourceList(feeds = listOf("https://example.com/feed.xml")),
+        ).forEach { list ->
+            var generated = false
+            val repo = LocalEditionRepository(
+                FakeStore(sourceList = list),
+                today = { sampleDay },
+                generateSample = {
+                    generated = true
+                    error("sample")
+                },
+            )
+            assertTrue(repo.loadCurrentMorningEdition().isFailure)
+            assertTrue(repo.retrySample().isFailure)
+            assertFalse(generated)
+        }
+    }
+
+    @Test
+    fun saveRssOrAtom_addsThatUrlAndLeavesTheSampleUntilGenerate() = runBlocking {
+        val store = FakeStore()
+        val documents = mapOf(
+            "https://example.com/rss.xml" to PERSONAL_RSS,
+            "https://example.com/atom.xml" to PERSONAL_ATOM,
+        )
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { url -> documents.getValue(url) })
+        val sample = repo.loadCurrentMorningEdition().getOrThrow()
+
+        repo.saveFeedUrl("https://example.com/rss.xml").getOrThrow()
+        val rssList = requireNotNull(store.readSourceList())
+        assertEquals(listOf("https://example.com/rss.xml"), rssList.feeds)
+        assertTrue(rssList.calendars.isEmpty())
+        assertFalse(encodeSourceList(rssList).contains("Central banks"))
+        assertTrue(repo.loadCurrentMorningEdition().getOrThrow().isSample)
+        assertEquals(sample.id, repo.loadCurrentMorningEdition().getOrThrow().id)
+
+        repo.saveFeedUrl("  https://example.com/atom.xml  ").getOrThrow()
+        assertEquals(listOf("https://example.com/atom.xml"), store.readSourceList()?.feeds)
+        assertTrue(repo.loadCurrentMorningEdition().getOrThrow().isSample)
+    }
+
+    @Test
+    fun saveFeedUrl_replacesFeedsAndKeepsTheCalendar() = runBlocking {
+        val store = FakeStore(
+            sourceList = SourceList(
+                id = "sources",
+                revision = 4,
+                feeds = listOf("https://example.com/old.xml"),
+                calendars = listOf("https://example.com/cal.ics"),
+            ),
+        )
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })
+
+        repo.saveFeedUrl("https://example.com/rss.xml").getOrThrow()
+
+        val list = requireNotNull(store.readSourceList())
+        assertEquals(listOf("https://example.com/rss.xml"), list.feeds)
+        assertEquals(listOf("https://example.com/cal.ics"), list.calendars)
+    }
+
+    @Test
+    fun badDocumentOrFailedRead_keepsThePreviousUrlAndThePaper() = runBlocking {
+        val morning = SampleGenerator.forDay(sampleDay)
+        val store = FakeStore(
+            morning = morning,
+            sourceList = SourceList(id = "sources", revision = 2, feeds = listOf("https://example.com/old.xml"), calendars = listOf("https://example.com/cal.ics")),
+        )
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            readFeed = { url ->
+                if (url.contains("new")) NOT_A_FEED else error("offline")
+            },
+        )
+
+        val badDocument = repo.saveFeedUrl("https://example.com/new.xml")
+        assertTrue(badDocument.isFailure)
+        assertEquals(FEED_DIDNT_LOAD, badDocument.exceptionOrNull()?.message)
+        assertEquals(listOf("https://example.com/old.xml"), store.readSourceList()?.feeds)
+        assertEquals(listOf("https://example.com/cal.ics"), store.readSourceList()?.calendars)
+        assertEquals(morning.id, repo.loadCurrentMorningEdition().getOrThrow().id)
+
+        val failedRead = repo.saveFeedUrl("https://example.com/also-new.xml")
+        assertTrue(failedRead.isFailure)
+        assertEquals(listOf("https://example.com/old.xml"), store.readSourceList()?.feeds)
+        assertEquals(morning.id, repo.loadCurrentMorningEdition().getOrThrow().id)
+    }
+
+    @Test
+    fun generate_savedFeed_buildsTodaysMorningEditionInSourceOrder() = runBlocking {
+        val store = FakeStore()
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            readFeed = { PERSONAL_RSS },
+        )
+        val sample = repo.loadCurrentMorningEdition().getOrThrow()
+        repo.saveFeedUrl("https://example.com/rss.xml").getOrThrow()
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals("morning-${sampleDay.toDayKey()}", edition.id)
+        assertEquals(1, edition.revision)
+        assertEquals(sampleDay.toDayKey(), edition.dayKey)
+        assertEquals(sampleDay.toDateLine(), edition.dateLine)
+        assertEquals(EditionRole.morning, edition.role)
+        assertEquals("Matutine", edition.mastheadName)
+        assertFalse(edition.isSample)
+        assertTrue(edition.events.isEmpty())
+        assertTrue(edition.tasks.isEmpty())
+        assertTrue(edition.sourceNotes.isEmpty())
+        assertEquals(
+            listOf("Markets open higher", "Port reopens"),
+            edition.stories.map { it.headline },
+        )
+        assertEquals(listOf(StoryRank.lead, StoryRank.secondary), edition.stories.map { it.rank })
+        assertEquals(1, edition.stories.count { it.rank == StoryRank.lead })
+        assertEquals("Personal Wire", edition.stories.first().sourceName)
+        assertEquals(
+            "Traders watched the open. Cargo is moving again. Summary assembled from headlines.",
+            edition.briefing,
+        )
+        assertTrue(edition.briefing.endsWith("Summary assembled from headlines."))
+        assertEquals(listOf("https://example.com/rss.xml"), store.readSourceList()?.feeds)
+        assertFalse(sample.isSample && edition.id == sample.id)
+        assertEquals(edition.id, repo.loadCurrentMorningEdition().getOrThrow().id)
+    }
+
+    @Test
+    fun generate_atomFeed_usesTheEntryAsTheLead() = runBlocking {
+        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/atom.xml")))
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_ATOM })
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals("Atom lead", edition.stories.single().headline)
+        assertEquals("Atom Desk", edition.stories.single().sourceName)
+        assertEquals("https://example.com/atom", edition.stories.single().sourceUrl)
+        assertEquals("Atom deck.", edition.briefing.removeSuffix(" Summary assembled from headlines."))
+        assertEquals(StoryRank.lead, edition.stories.single().rank)
+        assertFalse(edition.isSample)
+    }
+
+    @Test
+    fun generate_mixedFeeds_omitsTheFailureAndNotesIt() = runBlocking {
+        val store = FakeStore(
+            sourceList = SourceList(
+                feeds = listOf(
+                    "https://example.com/bad-first.xml",
+                    "https://example.com/rss.xml",
+                    "https://example.com/bad-second.xml",
+                ),
+            ),
+        )
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            readFeed = { url -> if (url.contains("bad")) NOT_A_FEED else PERSONAL_RSS },
+        )
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
+        assertEquals(StoryRank.lead, edition.stories.first().rank)
+        assertEquals(listOf(FEED_DIDNT_LOAD, FEED_DIDNT_LOAD), edition.sourceNotes)
+        assertFalse(edition.isSample)
+        assertEquals(
+            listOf(
+                "https://example.com/bad-first.xml",
+                "https://example.com/rss.xml",
+                "https://example.com/bad-second.xml",
+            ),
+            store.readSourceList()?.feeds,
+        )
+    }
+
+    @Test
+    fun generate_whenNothingParses_keepsTheListAndTheCurrentPaper() = runBlocking {
+        val morning = SampleGenerator.forDay(sampleDay)
+        val list = SourceList(feeds = listOf("https://example.com/bad.xml"))
+        val store = FakeStore(morning = morning, sourceList = list)
+        var generatedSample = false
+        val repo = LocalEditionRepository(
+            store,
+            today = { sampleDay },
+            generateSample = {
+                generatedSample = true
+                error("sample")
+            },
+            readFeed = { NOT_A_FEED },
+        )
+
+        val result = repo.generateToday()
+
+        assertTrue(result.isFailure)
+        assertEquals(FEED_DIDNT_LOAD, result.exceptionOrNull()?.message)
+        assertFalse(generatedSample)
+        assertEquals(list, store.readSourceList())
+        assertEquals(morning.id, repo.loadCurrentMorningEdition().getOrThrow().id)
+        assertTrue(repo.loadCurrentMorningEdition().getOrThrow().isSample)
+        assertNull(store.saved)
+    }
+
+    @Test
+    fun generate_emptyOrAbsentList_doesNotBuildTheBundledSample() = runBlocking {
+        listOf<SourceList?>(null, SourceList()).forEach { list ->
+            var generatedSample = false
+            val store = FakeStore(sourceList = list)
+            val repo = LocalEditionRepository(
+                store,
+                today = { sampleDay },
+                generateSample = {
+                    generatedSample = true
+                    error("sample")
+                },
+            )
+            assertTrue(repo.generateToday().isFailure)
+            assertFalse(generatedSample)
+            assertNull(store.saved)
+            assertEquals(list, store.readSourceList())
+        }
+    }
+
+    @Test
+    fun generate_again_keepsThePersonalIdAndIncrementsRevision() = runBlocking {
+        val existing = SampleGenerator.forDay(sampleDay).copy(
+            id = "morning-${sampleDay.toDayKey()}",
+            revision = 3,
+            isSample = false,
+        )
+        val store = FakeStore(
+            morning = existing,
+            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+        )
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals(existing.id, edition.id)
+        assertEquals(4, edition.revision)
         assertFalse(edition.isSample)
     }
+
+    @Test
+    fun saveTopics_storesChoicesLinesAndLeavesTheSourceList() = runBlocking {
+        val list = SourceList(
+            id = "sources",
+            revision = 2,
+            feeds = listOf("https://example.com/rss.xml"),
+            calendars = listOf("https://example.com/cal.ics"),
+        )
+        val store = FakeStore(sourceList = list)
+        val repo = LocalEditionRepository(store, today = { sampleDay })
+        val topics = topicsFromInterests(
+            selectedChoices = listOf("Technology", "Video games"),
+            freeLine = "Rust",
+            dayLines = listOf("school", " "),
+        )
+
+        repo.saveTopics(topics)
+
+        assertEquals(listOf("Technology", "Video games", "Rust", "school"), topics)
+        assertEquals(topics, store.readSettings()?.topics)
+        assertEquals(list, store.readSourceList())
+    }
+
+    @Test
+    fun generate_withTopics_putsTheBestFitFirstAsTheOnlyLead() = runBlocking {
+        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { TOPIC_RSS })
+        repo.saveTopics(
+            topicsFromInterests(
+                selectedChoices = listOf("Technology", "Video games"),
+                freeLine = "Rust",
+                dayLines = listOf("school", ""),
+            ),
+        )
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals(
+            listOf("TECHNOLOGY firms buy studios", "A studio ships video games", "Markets open higher"),
+            edition.stories.map { it.headline },
+        )
+        assertEquals(
+            listOf(StoryRank.lead, StoryRank.secondary, StoryRank.note),
+            edition.stories.map { it.rank },
+        )
+        assertEquals(1, edition.stories.count { it.rank == StoryRank.lead })
+        assertTrue(edition.briefing.startsWith("Traders watched the open."))
+        assertFalse(encodeEdition(edition).contains("\"fit\""))
+    }
+
+    @Test
+    fun generate_equalTopicFits_keepSourceOrder() = runBlocking {
+        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { TIE_RSS })
+        repo.saveTopics(listOf("Video games"))
+
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals(
+            listOf("Studio ships a sequel", "Another studio follows", "Markets open higher"),
+            edition.stories.map { it.headline },
+        )
+        assertEquals(StoryRank.lead, edition.stories.first().rank)
+        assertEquals(1, edition.stories.count { it.rank == StoryRank.lead })
+    }
+
+    @Test
+    fun generate_absentOrEmptyTopics_keepsSourceOrderOnThePersonsEdition() = runBlocking {
+        listOf<ReaderSettings?>(null, ReaderSettings(topics = emptyList())).forEach { settings ->
+            val store = FakeStore(
+                sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+                settings = settings,
+            )
+            val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })
+
+            val edition = repo.generateToday().getOrThrow()
+
+            assertEquals(
+                listOf("Markets open higher", "Port reopens"),
+                edition.stories.map { it.headline },
+            )
+            assertEquals(StoryRank.lead, edition.stories.first().rank)
+            assertFalse(edition.isSample)
+            assertEquals(settings, store.readSettings())
+        }
+    }
+
+    @Test
+    fun clearTopics_nextGenerateUsesSourceOrderAndDoesNotReturnTheSample() = runBlocking {
+        val store = FakeStore(
+            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
+            settings = ReaderSettings(topics = listOf("Technology", "Video games")),
+        )
+        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })
+
+        repo.saveTopics(
+            topicsFromInterests(
+                selectedChoices = emptyList(),
+                freeLine = "  ",
+                dayLines = listOf("", "   "),
+            ),
+        )
+        val edition = repo.generateToday().getOrThrow()
+
+        assertEquals(emptyList(), store.readSettings()?.topics)
+        assertEquals(
+            listOf("Markets open higher", "Port reopens"),
+            edition.stories.map { it.headline },
+        )
+        assertEquals(listOf(StoryRank.lead, StoryRank.secondary), edition.stories.map { it.rank })
+        assertFalse(edition.isSample)
+        assertFalse(edition.id.startsWith("sample-"))
+    }
+
+    @Test
+    fun editionWithoutSourceNotes_decodesAsEmpty() {
+        val json = """
+            {"id":"sample-2026-11-07","revision":1,"dayKey":"2026-11-07","role":"morning","mastheadName":"Matutine","dateLine":"Saturday, November 7, 2026","isSample":true,"briefing":"Kept","stories":[],"events":[],"tasks":[]}
+        """.trimIndent()
+        val edition = decodeEdition(json)
+        assertTrue(edition.sourceNotes.isEmpty())
+        assertTrue(encodeEdition(edition).contains("\"sourceNotes\":[]"))
+    }
+
+    @Test
+    fun documentThatIsNeitherRssNorAtom_failsWithTheFeedSentence() {
+        val failure = runCatching { parseFeedDocument(NOT_A_FEED) }
+        assertTrue(failure.isFailure)
+        assertEquals(FEED_DIDNT_LOAD, failure.exceptionOrNull()?.message)
+        assertEquals("Atom lead", parseFeedDocument(PERSONAL_ATOM).single().headline)
+    }
+}
+
+class SampleGeneratorTest {
+    @Test
+    fun bundledFeeds_keepSourceOrderAndCalendarTimes() {
+        val edition = SampleGenerator.forDay(LocalDate(2026, 11, 8))
+        assertEquals(
+            listOf(
+                "Central banks signal a longer pause on rates",
+                "Tech firms outline cautious hiring plans",
+                "Brief: Transit upgrades set for weekend",
+            ),
+            edition.stories.map { it.headline },
+        )
+        assertEquals(
+            listOf(StoryRank.lead, StoryRank.secondary, StoryRank.note),
+            edition.stories.map { it.rank },
+        )
+        assertEquals(listOf("Sample Wire", "Sample Herald", "City Desk"), edition.stories.map { it.sourceName })
+        assertEquals(
+            listOf(
+                EditionEvent("Team standup", "9:00"),
+                EditionEvent("Lunch with Alex", "12:30"),
+            ),
+            edition.events,
+        )
+        assertTrue(edition.tasks.isEmpty())
+        assertEquals("2026-11-08", edition.dayKey)
+    }
+
+    @Test
+    fun emptySourceListDocument_decodesAsPresentAndEmpty() {
+        val list = decodeSourceList("{}")
+        assertTrue(list.feeds.isEmpty())
+        assertTrue(list.calendars.isEmpty())
+    }
+
+    @Test
+    fun emptySettingsDocument_defaultsTopicsToEmpty() {
+        val settings = decodeReaderSettings("{}")
+        assertTrue(settings.topics.isEmpty())
+        assertTrue(encodeReaderSettings(ReaderSettings()).contains("\"topics\":[]"))
+    }
 }
diff --git a/gradle/libs.versions.toml b/gradle/libs.versions.toml
index fafc4e2..b149fa0 100644
--- a/gradle/libs.versions.toml
+++ b/gradle/libs.versions.toml
@@ -14,7 +14,7 @@ composeMultiplatform = "1.12.2"
 junit = "4.13.2"
 kotlin = "2.4.21"
 kotlinx-serialization = "1.8.1"
-kotlinx-datetime = "0.6.2"
+kotlinx-datetime = "0.7.1"
 kotlin-wrappers = "2026.10.4"
 kotlinx-coroutines = "1.11.0"
 ktor = "3.6.0"
diff --git a/skill-observations/log.md b/skill-observations/log.md
index 4d0f0ea..a6c4376 100644
--- a/skill-observations/log.md
+++ b/skill-observations/log.md
@@ -24,3 +24,33 @@ resolved statuses always carry their resolution date
 **Suggested improvement:** In On Activation, if the caller has already named the deliverable and marked the input's decisions as final, skip the mode offer and the deliverable question. Ask only about gaps that would let two downstream units diverge.
 
 **Principle:** When the caller has already fixed the output and the settled decisions, an activation menu that reopens those choices is friction, not elicitation.
+
+### Observation 2: Committed text must not contain the home directory
+
+**Status:** OPEN
+**Date:** 2026-11-07
+**Session context:** User asked to wire a local Android SDK and emulator into the repo, then corrected how paths should be written.
+**Skill:** brainstorming
+**Type:** open-source
+**Phase/Area:** Design docs, scripts, and logs that can be committed
+
+**Issue:** The SDK lives under the user's home directory. Writing that absolute path into a design doc, script log, or agent instruction would publish the home folder path on GitHub. The user asked for `~/Android/Sdk` instead.
+
+**Suggested improvement:** Any artifact that can be committed (docs, scripts' printed logs, agent instructions, commit messages) should print portable paths (`~/Android/Sdk`, repo-relative paths). Absolute home paths stay only in gitignored local config that tools require.
+
+**Principle:** Do not write an absolute home-directory path into anything that can be committed. Use a tilde path or a path relative to the repo.
+
+### Observation 3: A notice to agents is not a verification product
+
+**Status:** OPEN
+**Date:** 2026-11-08
+**Session context:** User asked to let agents use an already installed Android emulator. After approving a growing design, they stopped at the execution choice and said they only wanted a notice, not a large implementation.
+**Skill:** brainstorming
+**Type:** open-source
+**Phase/Area:** Scope, before writing a spec and plan
+
+**Issue:** The workflow turned "tell agents this emulator exists" into a script, a fake-SDK test suite, a spec, a plan, and a choice between execution modes. The user had to halt that and restate the original size.
+
+**Suggested improvement:** When the request is an agent instruction about a tool already installed on the machine, write that instruction and stop. Offer a script or test harness only if the user asks for one.
+
+**Principle:** Match the artifact to the request. A notice stays a notice.
diff --git a/_bmad-output/implementation-artifacts/deferred-work.md b/_bmad-output/implementation-artifacts/deferred-work.md
new file mode 100644
index 0000000..17fff08
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/deferred-work.md
@@ -0,0 +1,7 @@
+- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md`
+  summary: The edition screen's generating line, feed notice, and source notes are not composed in a test.
+  evidence: View-model flags and sourceNotes are asserted, and the common test source set does not run Compose. Deleting the three text blocks in EditionContent would still leave those tests green.
+
+- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md`
+  summary: The Android INTERNET permission is not exercised by a device fetch.
+  evidence: Removing the permission leaves the JVM and shared unit tests green. A device fetch would be needed to show that a missing permission becomes "That feed didn't load."
diff --git a/_bmad-output/implementation-artifacts/spec-1-4-build-the-sample-from-bundled-sources.md b/_bmad-output/implementation-artifacts/spec-1-4-build-the-sample-from-bundled-sources.md
new file mode 100644
index 0000000..678a223
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-1-4-build-the-sample-from-bundled-sources.md
@@ -0,0 +1,46 @@
+---
+title: '1-4 Build the sample from bundled sources'
+type: 'feature'
+created: '2026-11-08'
+status: 'done'
+route: 'oneshot'
+review_loop_iteration: 0
+story_key: '1-4-build-the-sample-from-bundled-sources'
+context:
+  - _bmad-output/implementation-artifacts/epic-1-context.md
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** First launch still shows a hand-written sample edition. The store only knows whether a source-list file exists, so an absent list and an empty list are not different, and the paper is not built from bundled feeds and a bundled calendar.
+
+**Approach:** When the source-list document is absent, the core generator builds today's sample from bundled feed and calendar fixtures, keeps the sample line, and does not write those fixtures into a source list. A saved morning edition is returned as stored. An existing list, including an empty one, does not use the bundled sample.
+
+</frozen-after-approval>
+
+## Implementation Notes
+
+- An absent source list is a missing `sources.json`. `readSourceList()` returns null only then. A decoded document, including empty feed and calendar lists, is present and does not use the bundled sample.
+- `SampleGenerator` parses three bundled RSS documents and one ICS calendar. With no topics, source order stands: first story is the lead, second is secondary, and the rest are notes.
+- The briefing is the item descriptions plus "Summary assembled from headlines." There is no model call.
+- Tasks stay empty. This story has no task source, and the screen already shows "No tasks yet."
+- The generated sample is saved as today's morning edition. The source list is not written.
+- A saved morning edition is returned as stored, and the generator is not called.
+- kotlinx-datetime is 0.7.1 and an api dependency of `:core`, matching the version the clients already resolve. `todayLocalDate` uses `kotlin.time.Clock`. The date line uses `LocalDate.day`.
+- JavaScript and Wasm stores stay non-persistent placeholders.
+- `:core:allTests` and `:app:shared:allTests` did not run. Yarn 1.22.22 could not be downloaded (502, then a timeout). JVM tests, Android host tests, JS and Wasm compilation, desktop compilation, and Android `assembleDebug` succeeded.
+
+## Review Triage Log
+
+- Invalid or blank `sources.json` throws from decode instead of becoming an empty list — false. A file that exists is not absent, so the sample is not built. Decode fails loudly. Nothing in this story writes a blank file, and `{}` is covered.
+- JavaScript and Wasm stores still look absent and do not persist — false. Those stubs already did not store a list or an edition. This story keeps that placeholder behavior.
+- Sample tasks are gone — low, not fixed. The sample is built from feeds and a calendar. There is no task source in this story, and inventing task lines would put a picture back. The empty copy is already "No tasks yet."
+- ICS all-day values can be read as clock times, and sample events are not filtered by day — false for the bundled calendar. The fixture times are `090000` and `123000`, and the sample agenda is the edition's calendar rather than a dated subscription. A general ICS parser is Story 1.7.
+- RSS parsing drops items it does not understand — false for the shipped fixtures. The three bundled documents parse to three stories in source order. A failed-feed rule belongs to Story 1.5.
+- Briefing text is the decks plus the required suffix — low, not fixed. The suffix is the no-key sentence from the requirements. The extract is the item descriptions, and no length was specified.
+- An empty list shows "Today's morning edition is not available yet." — false as a regression. That is the existing result when a source list is present and today's paper is missing. The acceptance criterion is that the sample is not used.
+- Disk cold-start, `retrySample`, and the date line are thinly tested — low, not fixed. The repository test returns a saved edition without calling the generator, and the file test checks that generation does not create `sources.json`. `LocalDate.day` is the renamed day-of-month property.
+- Spec notes were empty and the frozen intent is short — false as a product defect. Notes are filled here. The oneshot spec keeps the frozen block to the story intent.
+- Saved sample id and revision stay `sample-<day>` and `1`, and retry writes that edition again — false as a new bug. Story 1.1 already saved the sample that way. Retry is the explicit reload. Cold start reads the stored edition first.
diff --git a/_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md b/_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md
new file mode 100644
index 0000000..d94f2e1
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md
@@ -0,0 +1,131 @@
+---
+title: '1-5 Generate today''s edition from a feed'
+type: 'feature'
+created: '2026-11-08'
+status: 'done'
+route: 'dispatch'
+review_loop_iteration: 0
+story_key: '1-5-generate-today-s-edition-from-a-feed'
+baseline_commit: '5b7cf66aa147856946ea8b84267839b7ac885baa'
+context:
+  - _bmad-output/implementation-artifacts/epic-1-context.md
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** The front page can only become a paper from bundled fixtures. O cannot point it at a personal RSS or Atom feed, so the lead and the briefing stay the sample.
+
+**Approach:** Settings stores one RSS or Atom URL on the existing source list after that document parses. "Generate today's edition" builds today's morning edition from the saved feeds in the device zone, keeps the current paper readable until the new one is ready, and hides the sample line on the finished paper. With no topics, source order stands and the first story is the lead. With no AI key, the briefing is the extractive summary plus "Summary assembled from headlines."
+
+## Boundaries & Constraints
+
+**Always:** An absent source list still builds the bundled sample and does not write those fixtures into the list. An empty list stays empty. Rank is source order until topics exist. A new feed URL is written only after its document parses as RSS or Atom. A failed feed is omitted. When another saved feed still parses, the edition completes and carries the note "That feed didn't load." When nothing parses, the saved list and the current paper stay, and that same sentence is shown. The generating body copy is "Setting today's edition." Masthead and date stay. One primary button, "Generate today's edition". One settings row for the feed. Desktop and web chrome controls added here are at least 40px; Android rows are at least 48dp. The previous saved URL stays until the new URL succeeds, and the field keeps the typed URL.
+
+**Never:** A model call, CalDAV, a server, a second source list, Interests, or topic ranking. Do not change the bundled sample fixtures. Do not put Ktor or Compose in `:core`.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Save one feed | Sample on screen, URL returns RSS or Atom | Source list gains that URL and no bundled feed. Sample line still shows until generate. | N/A |
+| Generate | Saved feed parses, no topics, no key | Today's morning edition, device day key, sample line hidden, source-order lead, extractive briefing ending in the required sentence. | N/A |
+| Generating | Generate in flight, sample already shown | Masthead and date stay. Body says "Setting today's edition." Sample stays readable until the new edition replaces it. | N/A |
+| Bad URL | Document is not RSS or Atom, or the read fails | List unchanged. Field keeps the URL. Current paper stays. | "That feed didn't load." |
+| Mixed feeds | Stored list has one good feed and one bad feed | Edition completes from the good feed. Bad feed omitted. | Note on the edition: "That feed didn't load." |
+| Empty list | `sources.json` exists with no feeds | Generate does not build the bundled sample. | No sample fallback |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt` — RSS parser. Extend for Atom. Reuse `FeedItem`. Do not change bundled fixture text.
+- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/SampleGenerator.kt` — `rankForSourceOrder` and `extractiveBriefing`. Reuse. Do not call the sample path for a present list.
+- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` — Absent list builds the sample. Empty list does not. Add save and generate beside that.
+- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionModels.kt` — Edition has no failure note yet. Add a defaulted note list so older documents still decode.
+- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/SourceList.kt` — One document, `feeds` plus `calendars`. Do not add a second list.
+- `core/src/commonMain/kotlin/hu/toliver/matutine/edition/DayKey.kt` — Device zone via `todayLocalDate`. Reuse for the generated day key and date line.
+- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionViewModel.kt` — Load and retry only. Add generate state without dropping the visible edition.
+- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` — Draws the paper. Add the generating sentence and the failure note. Do not re-rank.
+- `app/shared/src/commonMain/kotlin/hu/toliver/matutine/MatutineApp.kt` — Composition root. Bind one repository and the feed reader here.
+- `app/shared/src/*/kotlin/hu/toliver/matutine/store/FileEditionStore.*.kt` — `sources.json` absent is null. Add a whole-document write. Leave JS and Wasm non-persistent.
+- `app/androidApp/src/main/AndroidManifest.xml` — No INTERNET permission yet. Add it. Do not add a calendar permission.
+- `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` — Cover the matrix. Keep the absent-list and empty-list tests passing.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt` -- Parse RSS and Atom into `FeedItem`, and fail with "That feed didn't load." when the document is neither.
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionModels.kt` -- Add defaulted `sourceNotes` on `Edition`.
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt` -- Build today's morning edition from feed documents: source-order rank, extractive briefing, empty events and tasks, `isSample` false. Omit a failed feed and note it when another feed succeeds.
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- Save one parsed URL onto the existing source list, and generate from that list without using the bundled sample.
+- [x] `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt` -- Write `sources.json` as one document. Mirror the write on the Android store. JS and Wasm accept the method and still store nothing.
+- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt` -- One feed row, field error under the label, one primary generate button, secondary return to the edition. Heights: 40px desktop and web, 48dp Android rows.
+- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- Settings entry, generating copy over the readable paper, sample line only when `isSample`, source notes visible.
+- [x] `app/androidApp/src/main/AndroidManifest.xml` -- Allow the feed read.
+- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` -- Unit-test the matrix, including Atom, a bad document, and mixed feeds.
+
+**Acceptance Criteria:**
+- Given the sample is on screen, when O saves one RSS or Atom URL and activates "Generate today's edition", then the masthead and date stay, the body says "Setting today's edition.", and the sample stays readable until the finished morning edition hides `SAMPLE EDITION`.
+- Given no AI key, when generation finishes, then the briefing ends with "Summary assembled from headlines." and the page is a finished edition.
+- Given a feed does not parse, when generation runs, then the page says "That feed didn't load.", the field keeps the URL, the prior list stays, and the sample stays.
+- Given another saved feed parses, when one feed fails, then the failed feed is omitted, the edition notes "That feed didn't load.", and the edition completes.
+
+## Implementation Notes
+
+- Feed documents are read through `readFeedDocument` in `:app:shared`. JVM and Android use `HttpURLConnection`. JS and Wasm fail with "That feed didn't load." and still do not persist a store.
+- Saving a URL replaces `feeds` with that one URL and keeps `calendars`. Generation reads every URL already on the list, so a stored mix of good and bad feeds still completes.
+- `:app:desktopApp:compileKotlinJvm` is not a task. `:app:desktopApp:compileKotlin` succeeded.
+- `:core:jvmTest` and `:app:shared:jvmTest` passed, including the matrix tests. JS and Wasm compilation succeeded. `:app:androidApp:assembleDebug` succeeded.
+- The new screens were not opened on the emulator.
+- Review patches: a corrupt `sources.json` no longer crashes the feed field; a finished save or generate does not overwrite a field the person changed; Android allows cleartext HTTP. Tests cover the kept calendar URL, the view-model save and generate paths, HTTP 200 and non-2xx, and a corrupt source list.
+
+## Spec Change Log
+
+## Review Triage Log
+
+- low — Settings is missing on the sample-failure and other-error screens. Rejected. This story starts from the sample paper, and the specified return path is the Settings button on that paper.
+- false — Closing Settings when Generate is pressed hides the generating paper. The page is supposed to show "Setting today's edition." while the sample stays readable.
+- low — Android system back from Settings leaves the activity. Rejected. The specified control is "Return to the edition"; a back handler is extra platform code.
+- false — A second `generate()` while one is running returns immediately. That guard stops a second write.
+- medium — `saveFeed` and `generate` assign the captured URL back onto `feedUrl` when they finish, so keystrokes during the request disappear. The completion must leave a newer field value alone.
+- low — Feed XML is parsed on the main dispatcher. Rejected. A normal feed is small, and moving the parser adds a dispatcher boundary.
+- low — A successful generate that skipped save can leave `feedFieldError` set. Clear it when generation succeeds.
+- false — A failed generate of an already saved URL does not set `feedFieldError`. The edition notice is the page sentence, and the field still holds the URL.
+- low — `singleOrNull()` leaves the field blank when the stored list has any count other than one. Rejected. This story saves one URL.
+- false — `saveFeedUrl` replaces `feeds` with that one URL. That is the specified save.
+- false — A recognized RSS or Atom document with no items still produces an edition. The spec fails a document only when it is neither RSS nor Atom. "No stories yet." already covers an empty paper.
+- low — `<rss` anywhere is preferred over a later `<feed`, so an Atom document that mentions that text is parsed as RSS. Rejected. Everyday Atom documents do not embed that tag, and a second classifier is extra branching.
+- false — `CancellationException` inside `runCatching` becomes a failed result. Nothing in this UI cancels that coroutine.
+- medium — Android `targetSdk` 37 blocks cleartext, so an `http://` feed fails with "That feed didn't load." while the same reader works on the desktop. Allow cleartext on the application.
+- low — `readFeedDocument` buffers the whole body. Rejected. A size cap is a new guard for a response this story does not demonstrate.
+- low — Numeric character references and HTML in descriptions are left in the text. Rejected. A sanitizer is a new parser, and the spec takes the description text as the extract.
+- false — Sprint status and the superpowers notes in the baseline diff are not defects in this story. Story 1.4's review status and those untracked files were already in the tree.
+- false — Generate that saves a newly parsed URL, then fails to rebuild, leaves that new URL on the list. The URL is written because it parsed. The current paper stays.
+- medium — `EditionViewModel` reads `savedFeedUrls()` during `init`, outside `load()`'s catch. A `sources.json` that does not decode crashes composition instead of showing the existing error state.
+- low — `writeSourceList` is not atomic. Rejected. A torn write needs a crash mid-write, and a temp-file rename is extra machinery.
+- low — `updateFeedUrl` leaves `feedFieldError` showing after the text changes. Clear the error when the text changes.
+- low — A successful `saveFeedUrl` is not tested against a list that already has a calendar. The code copies `calendars` through. Add the assertion.
+- medium — `EditionViewModel.generate` never runs its successful save-then-generate path under test. The fake save always fails. Add that case.
+- low — A failed `generateToday` for an already saved URL is not applied to `EditionUiState`. Add the case.
+- medium — The failed-save view-model test still passes if `generateToday` runs afterward, because the fake always throws. Assert that a failed new URL does not generate.
+- low — `saveFeed` and the initial field value are never called. Add those cases.
+- medium — `readFeedDocument` is never executed. A JVM test must accept a 200 body and reject a non-2xx response.
+- low — A corrupt `sources.json` is not distinguished from a missing file under test. Add the case that load fails and does not write a sample.
+- low — The paper's generating line, notice, and source notes are not composed in a test. Deferred. The view-model flags and `sourceNotes` are already asserted, and common tests do not run Compose.
+- low — The Android `INTERNET` permission is not executed on a device. Deferred. Unit tests cannot perform that fetch.
+
+## Design Notes
+
+The feed reader is a function passed into `LocalEditionRepository`. `:core` parses XML it is given. JVM and Android read HTTP in `:app:shared`; JS and Wasm do not gain a second store.
+
+A personal morning edition uses id `morning-<dayKey>` and revision 1 when it replaces the sample. A later on-demand generate of an existing personal morning keeps that id and increments revision. Automatic insert-if-absent stays Story 1.9.
+
+`sourceNotes` defaults to empty. Each omitted feed adds the sentence "That feed didn't load." Sample JSON without the field still loads.
+
+## Verification
+
+**Commands:**
+- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlinJvm :app:androidApp:assembleDebug` -- expected: success
+- `./gradlew :core:compileKotlinJs :core:compileKotlinWasmJs :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` -- expected: success, or a reported Yarn download failure
diff --git a/_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md b/_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md
new file mode 100644
index 0000000..89a2d73
--- /dev/null
+++ b/_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md
@@ -0,0 +1,83 @@
+---
+title: '1-6 Name topics and change the lead'
+type: 'feature'
+created: '2026-11-08'
+status: 'in-review'
+route: 'dispatch'
+review_loop_iteration: 0
+story_key: '1-6-name-topics-and-change-the-lead'
+baseline_commit: '5b7cf66aa147856946ea8b84267839b7ac885baa'
+context:
+  - _bmad-output/implementation-artifacts/epic-1-context.md
+---
+
+<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">
+
+## Intent
+
+**Problem:** The lead still follows feed order. O cannot name what the paper should notice.
+
+**Approach:** Interests, opened from Settings, saves topics on the settings document. The generator stores lead, secondary, and note from those topics. With no topics, source order stands and a line on the person's edition opens Interests. The sample launch does not.
+
+## Boundaries & Constraints
+
+**Always:** Ask "What do you want to read about?" and "What should the page notice about your day?" First question: Video games, Technology, Biology, Finance, Science, Sports, World news, Culture, Health, Local, plus a free line. Second: an open line and a control to add another. Many choices can be on. Selected: ink fill, paper label. Resting: field fill, ink label, 1px ink border. Height 40px on desktop and web, 48px on Android. Each selected choice and each non-empty line is one topic. A story fits when the topic is a case-insensitive substring of the headline or deck. More matches outrank fewer. Ties keep source order. Store that order with one lead, then secondary, then notes. The client draws the stored rank. Topics live on the settings document. Absent or empty topics means no topics. The line "What do you want to read about?" appears only on an edition from the person's sources.
+
+**Never:** A model call, CalDAV, a server, a second source list, or a stored numeric score. Do not put topics on the source list or move Interests to Admin. Do not open Interests on the sample launch. Do not change the bundled sample. Do not put Compose in `:core`.
+
+## I/O & Edge-Case Matrix
+
+| Scenario | Input / State | Expected Output / Behavior | Error Handling |
+|----------|--------------|---------------------------|----------------|
+| Save topics | Technology and Video games on, free line "Rust", day lines "school" and blank | Topics are Technology, Video games, Rust, school. Source list unchanged. | N/A |
+| Best fit | Those topics. Order is markets, a Video games story, then a story with both Technology and Video games | Both-topic story is the only lead and first. One-topic story is secondary. Markets is a note. | N/A |
+| No topics | Person's feeds. Settings absent or topics empty | Source order. First story is the lead. The page line opens Interests. Sample line stays hidden. | N/A |
+| Sample | Source list absent | Bundled sample. No Interests line. Topics are not written. | N/A |
+| Clear topics | Save with nothing selected | Topics become empty. Next generate uses source order. The sample does not return. | N/A |
+
+</frozen-after-approval>
+
+## Code Map
+
+- `core/.../edition/SampleGenerator.kt` — `rankForSourceOrder`. Reuse after ordering. Do not read topics.
+- `core/.../edition/EditionGenerator.kt` — `fromFeedResults` is source order. Pass topics in. Store `StoryRank` only.
+- `core/.../edition/EditionRepository.kt` — `generateToday` and `EditionStore`. Read topics when generating. Add settings read and save. Leave the absent-list sample path.
+- `core/.../edition/SourceList.kt` — Feeds and calendars only.
+- `app/shared/.../edition/EditionScreen.kt` — Draws stored order via `storiesForTwoColumns`. Add the no-topic line. Do not sort.
+- `app/shared/.../settings/SettingsScreen.kt` — Add one Interests row. Keep the feed row and generate.
+- `app/shared/.../MatutineApp.kt` — Open Interests from Settings and from the edition line. One repository.
+- `app/shared/.../store/FileEditionStore.*.kt` — Add `settings.json` on JVM and Android. JS and Wasm accept the methods and store nothing.
+- `core/.../edition/LocalEditionRepositoryTest.kt` — Keep absent-list, empty-list, and source-order tests. Add this matrix.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt` -- Settings document with defaulted topics.
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt` -- Rank by topic fit, then source order.
+- [x] `core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionRepository.kt` -- Save topics and pass them into generation.
+- [x] `app/shared/src/jvmMain/kotlin/hu/toliver/matutine/store/FileEditionStore.jvm.kt` -- Persist `settings.json`. Mirror on Android. JS and Wasm no-op.
+- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/InterestsScreen.kt` -- Both questions, choices, lines, and one primary save.
+- [x] `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionScreen.kt` -- No-topic line on the person's edition only. Wire that line and the Settings row through `SettingsScreen`, `EditionViewModel`, and `MatutineApp`.
+- [x] `core/src/commonTest/kotlin/hu/toliver/matutine/edition/LocalEditionRepositoryTest.kt` -- Test the matrix.
+
+**Acceptance Criteria:**
+- Given Interests is opened from Settings, when it is shown, then both questions, the named choices, the free line, and add-another are present, and a selected choice uses ink fill with a paper label.
+- Given Technology and Video games are saved and today's edition is generated, when it is drawn, then the stored lead is the best fit, then secondary, then note, with one lead.
+- Given no topics, when the person's sources generate an edition, then source order stands and "What do you want to read about?" opens Interests.
+- Given the first launch, when the sample is shown, then Interests is not opened.
+
+## Implementation Notes
+
+## Spec Change Log
+
+## Review Triage Log
+
+## Design Notes
+
+Fit count orders stories and is not stored. Ties keep feed order, then `rankForSourceOrder` labels the reordered list. `ReaderSettings` is the settings document; this story writes `topics` only.
+
+## Verification
+
+**Commands:**
+- `./gradlew :core:jvmTest :app:shared:jvmTest :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: success
+- `./gradlew :core:compileKotlinJs :core:compileKotlinWasmJs :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` -- expected: success, or a reported Yarn download failure
diff --git a/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/FeedReader.android.kt b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/FeedReader.android.kt
new file mode 100644
index 0000000..0c0faa8
--- /dev/null
+++ b/app/shared/src/androidMain/kotlin/hu/toliver/matutine/edition/FeedReader.android.kt
@@ -0,0 +1,35 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.withContext
+import java.net.HttpURLConnection
+import java.net.URI
+import java.nio.charset.Charset
+
+actual suspend fun readFeedDocument(url: String): String = withContext(Dispatchers.IO) {
+    val connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
+        instanceFollowRedirects = true
+        connectTimeout = 20_000
+        readTimeout = 20_000
+        requestMethod = "GET"
+        setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
+        setRequestProperty("User-Agent", "Matutine")
+    }
+    try {
+        val code = connection.responseCode
+        if (code !in 200..299) error(FEED_DIDNT_LOAD)
+        val charset = charsetFrom(connection.contentType)
+        connection.inputStream.bufferedReader(charset).use { it.readText() }
+    } finally {
+        connection.disconnect()
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
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/ChromeControls.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/ChromeControls.kt
new file mode 100644
index 0000000..556d72a
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/ChromeControls.kt
@@ -0,0 +1,12 @@
+package hu.toliver.matutine.edition
+
+import androidx.compose.runtime.Composable
+import androidx.compose.ui.platform.LocalDensity
+import androidx.compose.ui.unit.Dp
+import androidx.compose.ui.unit.dp
+
+@Composable
+internal fun chromeControlMinHeight(): Dp {
+    if (editionForceSingleColumn()) return 48.dp
+    return with(LocalDensity.current) { 40.toDp() }
+}
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedReader.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedReader.kt
new file mode 100644
index 0000000..c9bf362
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedReader.kt
@@ -0,0 +1,3 @@
+package hu.toliver.matutine.edition
+
+expect suspend fun readFeedDocument(url: String): String
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/InterestsScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/InterestsScreen.kt
new file mode 100644
index 0000000..01c6efb
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/InterestsScreen.kt
@@ -0,0 +1,252 @@
+package hu.toliver.matutine.settings
+
+import androidx.compose.foundation.BorderStroke
+import androidx.compose.foundation.background
+import androidx.compose.foundation.border
+import androidx.compose.foundation.layout.Arrangement
+import androidx.compose.foundation.layout.Box
+import androidx.compose.foundation.layout.Column
+import androidx.compose.foundation.layout.FlowRow
+import androidx.compose.foundation.layout.fillMaxSize
+import androidx.compose.foundation.layout.fillMaxWidth
+import androidx.compose.foundation.layout.height
+import androidx.compose.foundation.layout.heightIn
+import androidx.compose.foundation.layout.padding
+import androidx.compose.foundation.layout.safeContentPadding
+import androidx.compose.foundation.selection.toggleable
+import androidx.compose.foundation.rememberScrollState
+import androidx.compose.foundation.shape.RoundedCornerShape
+import androidx.compose.foundation.verticalScroll
+import androidx.compose.material3.Button
+import androidx.compose.material3.ButtonDefaults
+import androidx.compose.material3.OutlinedButton
+import androidx.compose.material3.OutlinedTextField
+import androidx.compose.material3.OutlinedTextFieldDefaults
+import androidx.compose.material3.Text
+import androidx.compose.runtime.Composable
+import androidx.compose.runtime.getValue
+import androidx.compose.runtime.mutableStateOf
+import androidx.compose.runtime.remember
+import androidx.compose.runtime.setValue
+import androidx.compose.ui.Alignment
+import androidx.compose.ui.Modifier
+import androidx.compose.ui.draw.clip
+import androidx.compose.ui.semantics.Role
+import androidx.compose.ui.unit.Dp
+import androidx.compose.ui.unit.dp
+import hu.toliver.matutine.edition.EditionCopy
+import hu.toliver.matutine.edition.chromeControlMinHeight
+import hu.toliver.matutine.edition.topicsFromInterests
+import hu.toliver.matutine.theme.LocalMatutineEditionTypography
+import hu.toliver.matutine.theme.NewsprintColors
+
+internal val InterestChoices: List<String> = listOf(
+    "Video games",
+    "Technology",
+    "Biology",
+    "Finance",
+    "Science",
+    "Sports",
+    "World news",
+    "Culture",
+    "Health",
+    "Local",
+)
+
+internal const val ADD_ANOTHER = "Add another"
+internal const val SAVE_TOPICS = "Save"
+
+internal data class InterestDraft(
+    val selected: List<String>,
+    val freeLine: String,
+    val dayLines: List<String>,
+)
+
+internal fun draftFromTopics(topics: List<String>): InterestDraft {
+    val known = InterestChoices.toSet()
+    val selected = mutableListOf<String>()
+    val custom = mutableListOf<String>()
+    topics.forEach { topic ->
+        if (topic in known && topic !in selected) selected += topic
+        else custom += topic
+    }
+    val dayLines = custom.drop(1)
+    return InterestDraft(
+        selected = selected,
+        freeLine = custom.firstOrNull().orEmpty(),
+        dayLines = dayLines.ifEmpty { listOf("") },
+    )
+}
+
+@Composable
+fun InterestsScreen(
+    topics: List<String>,
+    onSave: (List<String>) -> Unit,
+    onBack: () -> Unit,
+    modifier: Modifier = Modifier,
+) {
+    val typography = LocalMatutineEditionTypography.current
+    val controlHeight = chromeControlMinHeight()
+    val draft = remember(topics) { draftFromTopics(topics) }
+    var selected by remember(topics) { mutableStateOf(draft.selected) }
+    var freeLine by remember(topics) { mutableStateOf(draft.freeLine) }
+    var dayLines by remember(topics) { mutableStateOf(draft.dayLines) }
+    Column(
+        modifier = modifier
+            .fillMaxSize()
+            .background(NewsprintColors.chrome)
+            .safeContentPadding()
+            .verticalScroll(rememberScrollState())
+            .padding(24.dp),
+        verticalArrangement = Arrangement.spacedBy(16.dp),
+    ) {
+        Text(text = "Interests", style = typography.story.copy(color = NewsprintColors.ink))
+        Text(
+            text = EditionCopy.WHAT_DO_YOU_WANT_TO_READ_ABOUT,
+            style = typography.body.copy(color = NewsprintColors.ink),
+        )
+        FlowRow(
+            modifier = Modifier.fillMaxWidth(),
+            horizontalArrangement = Arrangement.spacedBy(8.dp),
+            verticalArrangement = Arrangement.spacedBy(8.dp),
+        ) {
+            InterestChoices.forEach { choice ->
+                val on = choice in selected
+                val shape = RoundedCornerShape(4.dp)
+                Box(
+                    modifier = Modifier
+                        .height(controlHeight)
+                        .clip(shape)
+                        .background(if (on) NewsprintColors.ink else NewsprintColors.field)
+                        .then(
+                            if (on) {
+                                Modifier
+                            } else {
+                                Modifier.border(1.dp, NewsprintColors.ink, shape)
+                            },
+                        )
+                        .toggleable(
+                            value = on,
+                            role = Role.Checkbox,
+                            onValueChange = { checked ->
+                                selected = if (checked) selected + choice else selected - choice
+                            },
+                        )
+                        .padding(horizontal = 16.dp),
+                    contentAlignment = Alignment.Center,
+                ) {
+                    Text(
+                        text = choice,
+                        style = typography.body.copy(
+                            color = if (on) NewsprintColors.paper else NewsprintColors.ink,
+                        ),
+                    )
+                }
+            }
+        }
+        TopicLine(
+            value = freeLine,
+            onValueChange = { freeLine = it },
+            controlHeight = controlHeight,
+        )
+        Text(
+            text = EditionCopy.WHAT_SHOULD_THE_PAGE_NOTICE,
+            style = typography.body.copy(color = NewsprintColors.ink),
+        )
+        dayLines.forEachIndexed { index, line ->
+            TopicLine(
+                value = line,
+                onValueChange = { updated ->
+                    dayLines = dayLines.toMutableList().also { it[index] = updated }
+                },
+                controlHeight = controlHeight,
+            )
+        }
+        OutlinedButton(
+            onClick = { dayLines = dayLines + "" },
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = controlHeight),
+            border = BorderStroke(1.dp, NewsprintColors.ink),
+            colors = ButtonDefaults.outlinedButtonColors(
+                containerColor = NewsprintColors.paper,
+                contentColor = NewsprintColors.ink,
+            ),
+            shape = RoundedCornerShape(4.dp),
+        ) {
+            Text(
+                text = ADD_ANOTHER,
+                style = typography.body.copy(color = NewsprintColors.ink),
+            )
+        }
+        Button(
+            onClick = {
+                onSave(
+                    topicsFromInterests(
+                        selectedChoices = selected,
+                        freeLine = freeLine,
+                        dayLines = dayLines,
+                    ),
+                )
+            },
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = controlHeight),
+            colors = ButtonDefaults.buttonColors(
+                containerColor = NewsprintColors.ink,
+                contentColor = NewsprintColors.paper,
+            ),
+            shape = RoundedCornerShape(4.dp),
+        ) {
+            Text(
+                text = SAVE_TOPICS,
+                style = typography.body.copy(color = NewsprintColors.paper),
+            )
+        }
+        OutlinedButton(
+            onClick = onBack,
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = controlHeight),
+            border = BorderStroke(1.dp, NewsprintColors.ink),
+            colors = ButtonDefaults.outlinedButtonColors(
+                containerColor = NewsprintColors.paper,
+                contentColor = NewsprintColors.ink,
+            ),
+            shape = RoundedCornerShape(4.dp),
+        ) {
+            Text(
+                text = "Back",
+                style = typography.body.copy(color = NewsprintColors.ink),
+            )
+        }
+    }
+}
+
+@Composable
+private fun TopicLine(
+    value: String,
+    onValueChange: (String) -> Unit,
+    controlHeight: Dp,
+) {
+    val typography = LocalMatutineEditionTypography.current
+    OutlinedTextField(
+        value = value,
+        onValueChange = onValueChange,
+        modifier = Modifier
+            .fillMaxWidth()
+            .heightIn(min = controlHeight),
+        singleLine = true,
+        textStyle = typography.body,
+        shape = RoundedCornerShape(4.dp),
+        colors = OutlinedTextFieldDefaults.colors(
+            focusedBorderColor = NewsprintColors.ink,
+            unfocusedBorderColor = NewsprintColors.ink,
+            focusedContainerColor = NewsprintColors.field,
+            unfocusedContainerColor = NewsprintColors.field,
+            cursorColor = NewsprintColors.ink,
+            focusedTextColor = NewsprintColors.ink,
+            unfocusedTextColor = NewsprintColors.ink,
+        ),
+    )
+}
diff --git a/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt
new file mode 100644
index 0000000..ebec38d
--- /dev/null
+++ b/app/shared/src/commonMain/kotlin/hu/toliver/matutine/settings/SettingsScreen.kt
@@ -0,0 +1,146 @@
+package hu.toliver.matutine.settings
+
+import androidx.compose.foundation.BorderStroke
+import androidx.compose.foundation.background
+import androidx.compose.foundation.clickable
+import androidx.compose.foundation.layout.Arrangement
+import androidx.compose.foundation.layout.Column
+import androidx.compose.foundation.layout.Row
+import androidx.compose.foundation.layout.fillMaxSize
+import androidx.compose.foundation.layout.fillMaxWidth
+import androidx.compose.foundation.layout.heightIn
+import androidx.compose.foundation.layout.padding
+import androidx.compose.foundation.layout.safeContentPadding
+import androidx.compose.foundation.rememberScrollState
+import androidx.compose.foundation.shape.RoundedCornerShape
+import androidx.compose.foundation.text.KeyboardActions
+import androidx.compose.foundation.text.KeyboardOptions
+import androidx.compose.foundation.verticalScroll
+import androidx.compose.material3.Button
+import androidx.compose.material3.ButtonDefaults
+import androidx.compose.material3.HorizontalDivider
+import androidx.compose.material3.OutlinedButton
+import androidx.compose.material3.OutlinedTextField
+import androidx.compose.material3.OutlinedTextFieldDefaults
+import androidx.compose.material3.Text
+import androidx.compose.runtime.Composable
+import androidx.compose.ui.Alignment
+import androidx.compose.ui.Modifier
+import androidx.compose.ui.semantics.Role
+import androidx.compose.ui.text.input.ImeAction
+import androidx.compose.ui.text.input.KeyboardType
+import androidx.compose.ui.unit.dp
+import hu.toliver.matutine.edition.chromeControlMinHeight
+import hu.toliver.matutine.theme.LocalMatutineEditionTypography
+import hu.toliver.matutine.theme.NewsprintColors
+
+@Composable
+fun SettingsScreen(
+    feedUrl: String,
+    onFeedUrlChange: (String) -> Unit,
+    feedError: String?,
+    onSaveFeed: () -> Unit,
+    onOpenInterests: () -> Unit,
+    onGenerate: () -> Unit,
+    onBack: () -> Unit,
+    modifier: Modifier = Modifier,
+) {
+    val typography = LocalMatutineEditionTypography.current
+    val controlHeight = chromeControlMinHeight()
+    Column(
+        modifier = modifier
+            .fillMaxSize()
+            .background(NewsprintColors.chrome)
+            .safeContentPadding()
+            .verticalScroll(rememberScrollState())
+            .padding(24.dp),
+        verticalArrangement = Arrangement.spacedBy(16.dp),
+    ) {
+        Text(text = "Settings", style = typography.story.copy(color = NewsprintColors.ink))
+        Column(
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = controlHeight),
+            verticalArrangement = Arrangement.spacedBy(8.dp),
+        ) {
+            Text(text = "Feed", style = typography.body.copy(color = NewsprintColors.ink))
+            OutlinedTextField(
+                value = feedUrl,
+                onValueChange = onFeedUrlChange,
+                modifier = Modifier
+                    .fillMaxWidth()
+                    .heightIn(min = controlHeight),
+                singleLine = true,
+                isError = feedError != null,
+                textStyle = typography.body,
+                keyboardOptions = KeyboardOptions(
+                    keyboardType = KeyboardType.Uri,
+                    imeAction = ImeAction.Done,
+                ),
+                keyboardActions = KeyboardActions(onDone = { onSaveFeed() }),
+                shape = RoundedCornerShape(4.dp),
+                colors = OutlinedTextFieldDefaults.colors(
+                    focusedBorderColor = NewsprintColors.ink,
+                    unfocusedBorderColor = NewsprintColors.ink,
+                    focusedContainerColor = NewsprintColors.field,
+                    unfocusedContainerColor = NewsprintColors.field,
+                    cursorColor = NewsprintColors.ink,
+                    focusedTextColor = NewsprintColors.ink,
+                    unfocusedTextColor = NewsprintColors.ink,
+                    errorBorderColor = NewsprintColors.error,
+                    errorCursorColor = NewsprintColors.error,
+                    errorTextColor = NewsprintColors.ink,
+                ),
+            )
+            if (feedError != null) {
+                Text(
+                    text = feedError,
+                    style = typography.body.copy(color = NewsprintColors.error),
+                )
+            }
+        }
+        Row(
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = controlHeight)
+                .clickable(role = Role.Button, onClick = onOpenInterests),
+            verticalAlignment = Alignment.CenterVertically,
+        ) {
+            Text(text = "Interests", style = typography.body.copy(color = NewsprintColors.ink))
+        }
+        HorizontalDivider(thickness = 1.dp, color = NewsprintColors.rule)
+        Button(
+            onClick = onGenerate,
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = controlHeight),
+            colors = ButtonDefaults.buttonColors(
+                containerColor = NewsprintColors.ink,
+                contentColor = NewsprintColors.paper,
+            ),
+            shape = RoundedCornerShape(4.dp),
+        ) {
+            Text(
+                text = "Generate today's edition",
+                style = typography.body.copy(color = NewsprintColors.paper),
+            )
+        }
+        OutlinedButton(
+            onClick = onBack,
+            modifier = Modifier
+                .fillMaxWidth()
+                .heightIn(min = controlHeight),
+            border = BorderStroke(1.dp, NewsprintColors.ink),
+            colors = ButtonDefaults.outlinedButtonColors(
+                containerColor = NewsprintColors.paper,
+                contentColor = NewsprintColors.ink,
+            ),
+            shape = RoundedCornerShape(4.dp),
+        ) {
+            Text(
+                text = "Return to the edition",
+                style = typography.body.copy(color = NewsprintColors.ink),
+            )
+        }
+    }
+}
diff --git a/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
new file mode 100644
index 0000000..ee1bde3
--- /dev/null
+++ b/app/shared/src/commonTest/kotlin/hu/toliver/matutine/edition/EditionGenerateStateTest.kt
@@ -0,0 +1,211 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.coroutines.CompletableDeferred
+import kotlinx.coroutines.CoroutineScope
+import kotlinx.coroutines.Dispatchers
+import kotlinx.datetime.LocalDate
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertTrue
+
+class EditionGenerateStateTest {
+    private val day = LocalDate(2026, 11, 8)
+    private val sample = SampleGenerator.forDay(day)
+    private val finished = sample.copy(id = "morning-2026-11-08", revision = 1, isSample = false)
+
+    @Test
+    fun generateKeepsTheVisibleEditionUntilTheNewOneIsReady() {
+        val gate = CompletableDeferred<Edition>()
+        val repository = FakeEditionRepository(sample) { gate.await() }
+        val viewModel = viewModel(repository)
+
+        val before = viewModel.state as EditionUiState.Ready
+        assertTrue(before.edition.isSample)
+        viewModel.generate()
+
+        val generating = viewModel.state as EditionUiState.Ready
+        assertTrue(generating.generating)
+        assertEquals(sample.mastheadName, generating.edition.mastheadName)
+        assertEquals(sample.dateLine, generating.edition.dateLine)
+        assertTrue(generating.edition.isSample)
+        assertEquals(null, generating.notice)
+
+        gate.complete(finished)
+
+        val ready = viewModel.state as EditionUiState.Ready
+        assertFalse(ready.generating)
+        assertFalse(ready.edition.isSample)
+        assertEquals(finished.id, ready.edition.id)
+        assertEquals(null, ready.notice)
+        assertEquals(null, viewModel.feedFieldError)
+    }
+
+    @Test
+    fun generateSuccessSavesTheNewUrlAndClearsTheFieldError() {
+        val repository = FakeEditionRepository(sample, generate = { finished })
+        val viewModel = viewModel(repository)
+        viewModel.updateFeedUrl("  https://example.com/rss.xml  ")
+        viewModel.saveFeed()
+        assertEquals(FEED_DIDNT_LOAD, viewModel.feedFieldError)
+        repository.saveSucceeds = true
+
+        viewModel.generate()
+
+        val ready = viewModel.state as EditionUiState.Ready
+        assertFalse(ready.generating)
+        assertEquals(finished.id, ready.edition.id)
+        assertFalse(ready.edition.isSample)
+        assertEquals(null, viewModel.feedFieldError)
+        assertEquals("https://example.com/rss.xml", viewModel.feedUrl)
+        assertEquals(listOf("https://example.com/rss.xml"), repository.savedFeedUrls())
+        assertEquals(1, repository.generateCalls)
+    }
+
+    @Test
+    fun failedGenerateOfSavedUrlKeepsTheSample() {
+        val repository = FakeEditionRepository(
+            sample,
+            initialFeeds = listOf("https://example.com/saved.xml"),
+            generate = { error(FEED_DIDNT_LOAD) },
+        )
+        val viewModel = viewModel(repository)
+
+        viewModel.generate()
+
+        val ready = viewModel.state as EditionUiState.Ready
+        assertFalse(ready.generating)
+        assertTrue(ready.edition.isSample)
+        assertEquals(FEED_DIDNT_LOAD, ready.notice)
+        assertEquals("https://example.com/saved.xml", viewModel.feedUrl)
+        assertEquals(1, repository.generateCalls)
+        assertEquals(0, repository.saveCalls)
+    }
+
+    @Test
+    fun failedNewUrlDoesNotGenerate() {
+        val repository = FakeEditionRepository(sample, generate = { finished })
+        val viewModel = viewModel(repository)
+        viewModel.updateFeedUrl("https://example.com/broken.xml")
+
+        viewModel.generate()
+
+        val ready = viewModel.state as EditionUiState.Ready
+        assertFalse(ready.generating)
+        assertTrue(ready.edition.isSample)
+        assertEquals(FEED_DIDNT_LOAD, ready.notice)
+        assertEquals("https://example.com/broken.xml", viewModel.feedUrl)
+        assertEquals(FEED_DIDNT_LOAD, viewModel.feedFieldError)
+        assertEquals(0, repository.generateCalls)
+        assertEquals(emptyList(), repository.savedFeedUrls())
+    }
+
+    @Test
+    fun initialFieldIsTheSavedUrl() {
+        val repository = FakeEditionRepository(sample, initialFeeds = listOf("https://example.com/saved.xml"))
+        val viewModel = viewModel(repository)
+
+        assertEquals("https://example.com/saved.xml", viewModel.feedUrl)
+        assertEquals(null, viewModel.feedFieldError)
+        assertTrue(viewModel.state is EditionUiState.Ready)
+    }
+
+    @Test
+    fun corruptSourceListLeavesTheFieldEmptyAndStillLoads() {
+        val repository = FakeEditionRepository(sample, failSavedFeedUrls = true)
+        val viewModel = viewModel(repository)
+
+        assertEquals("", viewModel.feedUrl)
+        assertTrue(viewModel.state is EditionUiState.Ready)
+    }
+
+    @Test
+    fun saveFeedSuccessStoresTheTrimmedUrl() {
+        val repository = FakeEditionRepository(sample, initialSaveSucceeds = true)
+        val viewModel = viewModel(repository)
+        viewModel.updateFeedUrl("  https://example.com/rss.xml  ")
+
+        viewModel.saveFeed()
+
+        assertEquals("https://example.com/rss.xml", viewModel.feedUrl)
+        assertEquals(null, viewModel.feedFieldError)
+        assertEquals(listOf("https://example.com/rss.xml"), repository.savedFeedUrls())
+    }
+
+    @Test
+    fun saveFeedFailureKeepsTheTypedUrl() {
+        val repository = FakeEditionRepository(sample)
+        val viewModel = viewModel(repository)
+        viewModel.updateFeedUrl("https://example.com/broken.xml")
+
+        viewModel.saveFeed()
+
+        assertEquals("https://example.com/broken.xml", viewModel.feedUrl)
+        assertEquals(FEED_DIDNT_LOAD, viewModel.feedFieldError)
+        assertEquals(emptyList(), repository.savedFeedUrls())
+
+        viewModel.updateFeedUrl("https://example.com/next.xml")
+        assertEquals("https://example.com/next.xml", viewModel.feedUrl)
+        assertEquals(null, viewModel.feedFieldError)
+    }
+
+    @Test
+    fun saveFeedDoesNotReplaceTextTypedWhileSaving() {
+        val gate = CompletableDeferred<Unit>()
+        val repository = FakeEditionRepository(sample, initialSaveSucceeds = true, blockSave = gate)
+        val viewModel = viewModel(repository)
+        viewModel.updateFeedUrl("https://example.com/first.xml")
+        viewModel.saveFeed()
+        viewModel.updateFeedUrl("https://example.com/second.xml")
+
+        gate.complete(Unit)
+
+        assertEquals("https://example.com/second.xml", viewModel.feedUrl)
+        assertEquals(null, viewModel.feedFieldError)
+        assertEquals(listOf("https://example.com/first.xml"), repository.savedFeedUrls())
+    }
+
+    private fun viewModel(repository: EditionRepository) =
+        EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
+}
+
+private class FakeEditionRepository(
+    private val edition: Edition,
+    initialFeeds: List<String> = emptyList(),
+    initialSaveSucceeds: Boolean = false,
+    private val failSavedFeedUrls: Boolean = false,
+    var blockSave: CompletableDeferred<Unit>? = null,
+    private val generate: suspend () -> Edition = { edition },
+) : EditionRepository {
+    private val feeds = initialFeeds.toMutableList()
+    var saveSucceeds: Boolean = initialSaveSucceeds
+    var generateCalls: Int = 0
+    var saveCalls: Int = 0
+
+    override suspend fun loadCurrentMorningEdition(): Result<Edition> = Result.success(edition)
+
+    override suspend fun retrySample(): Result<Edition> = Result.success(edition)
+
+    override fun savedFeedUrls(): List<String> {
+        if (failSavedFeedUrls) error("corrupt sources")
+        return feeds.toList()
+    }
+
+    override fun savedTopics(): List<String> = emptyList()
+
+    override fun saveTopics(topics: List<String>) = Unit
+
+    override suspend fun saveFeedUrl(url: String): Result<Unit> {
+        saveCalls += 1
+        blockSave?.await()
+        if (!saveSucceeds) return Result.failure(IllegalStateException(FEED_DIDNT_LOAD))
+        feeds.clear()
+        feeds += url.trim()
+        return Result.success(Unit)
+    }
+
+    override suspend fun generateToday(): Result<Edition> {
+        generateCalls += 1
+        return runCatching { generate() }
+    }
+}
diff --git a/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/FeedReader.js.kt b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/FeedReader.js.kt
new file mode 100644
index 0000000..066b425
--- /dev/null
+++ b/app/shared/src/jsMain/kotlin/hu/toliver/matutine/edition/FeedReader.js.kt
@@ -0,0 +1,5 @@
+package hu.toliver.matutine.edition
+
+actual suspend fun readFeedDocument(@Suppress("UNUSED_PARAMETER") url: String): String {
+    error(FEED_DIDNT_LOAD)
+}
diff --git a/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/FeedReader.jvm.kt b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/FeedReader.jvm.kt
new file mode 100644
index 0000000..0c0faa8
--- /dev/null
+++ b/app/shared/src/jvmMain/kotlin/hu/toliver/matutine/edition/FeedReader.jvm.kt
@@ -0,0 +1,35 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.withContext
+import java.net.HttpURLConnection
+import java.net.URI
+import java.nio.charset.Charset
+
+actual suspend fun readFeedDocument(url: String): String = withContext(Dispatchers.IO) {
+    val connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
+        instanceFollowRedirects = true
+        connectTimeout = 20_000
+        readTimeout = 20_000
+        requestMethod = "GET"
+        setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
+        setRequestProperty("User-Agent", "Matutine")
+    }
+    try {
+        val code = connection.responseCode
+        if (code !in 200..299) error(FEED_DIDNT_LOAD)
+        val charset = charsetFrom(connection.contentType)
+        connection.inputStream.bufferedReader(charset).use { it.readText() }
+    } finally {
+        connection.disconnect()
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
diff --git a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/edition/FeedReaderJvmTest.kt b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/edition/FeedReaderJvmTest.kt
new file mode 100644
index 0000000..e4efb72
--- /dev/null
+++ b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/edition/FeedReaderJvmTest.kt
@@ -0,0 +1,35 @@
+package hu.toliver.matutine.edition
+
+import com.sun.net.httpserver.HttpServer
+import kotlinx.coroutines.runBlocking
+import java.net.InetSocketAddress
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertTrue
+
+class FeedReaderJvmTest {
+    @Test
+    fun http200ReturnsTheBodyAndNon2xxFails() = runBlocking {
+        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
+        server.createContext("/ok") { exchange ->
+            val body = "<rss>ok</rss>".toByteArray()
+            exchange.sendResponseHeaders(200, body.size.toLong())
+            exchange.responseBody.use { it.write(body) }
+            exchange.close()
+        }
+        server.createContext("/missing") { exchange ->
+            exchange.sendResponseHeaders(404, -1)
+            exchange.close()
+        }
+        server.start()
+        try {
+            val port = server.address.port
+            assertEquals("<rss>ok</rss>", readFeedDocument("http://127.0.0.1:$port/ok"))
+            val failure = runCatching { readFeedDocument("http://127.0.0.1:$port/missing") }
+            assertTrue(failure.isFailure)
+            assertEquals(FEED_DIDNT_LOAD, failure.exceptionOrNull()?.message)
+        } finally {
+            server.stop(0)
+        }
+    }
+}
diff --git a/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
new file mode 100644
index 0000000..30b4c70
--- /dev/null
+++ b/app/shared/src/jvmTest/kotlin/hu/toliver/matutine/store/FileEditionStoreTest.kt
@@ -0,0 +1,104 @@
+package hu.toliver.matutine.store
+
+import hu.toliver.matutine.edition.LocalEditionRepository
+import hu.toliver.matutine.edition.ReaderSettings
+import hu.toliver.matutine.edition.SourceList
+import hu.toliver.matutine.edition.StoryRank
+import kotlinx.coroutines.runBlocking
+import java.io.File
+import java.nio.file.Files
+import kotlin.test.Test
+import kotlin.test.assertEquals
+import kotlin.test.assertFalse
+import kotlin.test.assertNull
+import kotlin.test.assertTrue
+
+class FileEditionStoreTest {
+    @Test
+    fun absentList_buildsSampleWithoutCreatingSourceList() = runBlocking {
+        val dir = Files.createTempDirectory("matutine-absent").toFile()
+        val store = FileEditionStore(dir.absolutePath)
+        val edition = LocalEditionRepository(store)
+            .loadCurrentMorningEdition()
+            .getOrThrow()
+
+        assertTrue(edition.isSample)
+        assertEquals(StoryRank.lead, edition.stories.first().rank)
+        assertNull(store.readSourceList())
+        assertNull(store.readSettings())
+        assertFalse(File(dir, "sources.json").exists())
+        assertFalse(File(dir, "settings.json").exists())
+    }
+
+    @Test
+    fun emptySourceList_doesNotBuildSample() = runBlocking {
+        val dir = Files.createTempDirectory("matutine-empty").toFile()
+        File(dir, "sources.json").writeText("""{"feeds":[],"calendars":[]}""")
+        val store = FileEditionStore(dir.absolutePath)
+        val result = LocalEditionRepository(store).loadCurrentMorningEdition()
+
+        assertTrue(result.isFailure)
+        assertTrue(store.readSourceList()?.feeds?.isEmpty() == true)
+        assertFalse(File(dir, "editions").exists())
+    }
+
+    @Test
+    fun writeSourceList_storesOneDocument() {
+        val dir = Files.createTempDirectory("matutine-write-sources").toFile()
+        val store = FileEditionStore(dir.absolutePath)
+        store.writeSourceList(
+            SourceList(
+                id = "sources",
+                revision = 1,
+                feeds = listOf("https://example.com/rss.xml"),
+                calendars = listOf("https://example.com/cal.ics"),
+            ),
+        )
+
+        val file = File(dir, "sources.json")
+        assertTrue(file.exists())
+        assertFalse(file.readText().contains("Central banks"))
+        assertEquals(listOf("https://example.com/rss.xml"), store.readSourceList()?.feeds)
+        assertEquals(listOf("https://example.com/cal.ics"), store.readSourceList()?.calendars)
+        assertEquals(1, store.readSourceList()?.revision)
+    }
+
+    @Test
+    fun writeSettings_storesTopicsWithoutTouchingTheSourceList() {
+        val dir = Files.createTempDirectory("matutine-settings").toFile()
+        val store = FileEditionStore(dir.absolutePath)
+        store.writeSourceList(
+            SourceList(
+                id = "sources",
+                revision = 1,
+                feeds = listOf("https://example.com/rss.xml"),
+                calendars = listOf("https://example.com/cal.ics"),
+            ),
+        )
+
+        store.writeSettings(ReaderSettings(topics = listOf("Technology", "Video games", "Rust", "school")))
+
+        val settings = File(dir, "settings.json")
+        assertTrue(settings.exists())
+        assertEquals(
+            listOf("Technology", "Video games", "Rust", "school"),
+            store.readSettings()?.topics,
+        )
+        assertEquals(listOf("https://example.com/rss.xml"), store.readSourceList()?.feeds)
+        assertEquals(listOf("https://example.com/cal.ics"), store.readSourceList()?.calendars)
+        assertFalse(settings.readText().contains("https://example.com"))
+    }
+
+    @Test
+    fun corruptSourceList_failsLoadAndDoesNotWriteSample() = runBlocking {
+        val dir = Files.createTempDirectory("matutine-corrupt").toFile()
+        File(dir, "sources.json").writeText("not json")
+        val store = FileEditionStore(dir.absolutePath)
+
+        val result = LocalEditionRepository(store).loadCurrentMorningEdition()
+
+        assertTrue(result.isFailure)
+        assertFalse(File(dir, "editions").exists())
+        assertEquals("not json", File(dir, "sources.json").readText())
+    }
+}
diff --git a/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/FeedReader.wasmJs.kt b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/FeedReader.wasmJs.kt
new file mode 100644
index 0000000..066b425
--- /dev/null
+++ b/app/shared/src/wasmJsMain/kotlin/hu/toliver/matutine/edition/FeedReader.wasmJs.kt
@@ -0,0 +1,5 @@
+package hu.toliver.matutine.edition
+
+actual suspend fun readFeedDocument(@Suppress("UNUSED_PARAMETER") url: String): String {
+    error(FEED_DIDNT_LOAD)
+}
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/BundledSources.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/BundledSources.kt
new file mode 100644
index 0000000..26e7a51
--- /dev/null
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/BundledSources.kt
@@ -0,0 +1,63 @@
+package hu.toliver.matutine.edition
+
+internal object BundledSources {
+    val feeds: List<String> = listOf(
+        """
+        <?xml version="1.0"?>
+        <rss version="2.0">
+          <channel>
+            <title>Sample Wire</title>
+            <item>
+              <title>Central banks signal a longer pause on rates</title>
+              <link>https://example.com/sample/lead</link>
+              <description>Officials emphasize data dependence as inflation cools unevenly across regions.</description>
+            </item>
+          </channel>
+        </rss>
+        """.trimIndent(),
+        """
+        <?xml version="1.0"?>
+        <rss version="2.0">
+          <channel>
+            <title>Sample Herald</title>
+            <item>
+              <title>Tech firms outline cautious hiring plans</title>
+              <link>https://example.com/sample/secondary</link>
+              <description>Quarterly updates point to selective growth rather than broad expansion.</description>
+            </item>
+          </channel>
+        </rss>
+        """.trimIndent(),
+        """
+        <?xml version="1.0"?>
+        <rss version="2.0">
+          <channel>
+            <title>City Desk</title>
+            <item>
+              <title>Brief: Transit upgrades set for weekend</title>
+              <link>https://example.com/sample/note</link>
+              <description>A single track closure may add fifteen minutes to cross-town trips.</description>
+            </item>
+          </channel>
+        </rss>
+        """.trimIndent(),
+    )
+
+    val calendar: String =
+        """
+        BEGIN:VCALENDAR
+        BEGIN:VEVENT
+        SUMMARY:Team standup
+        DTSTART:090000
+        END:VEVENT
+        BEGIN:VEVENT
+        SUMMARY:Lunch with Alex
+        DTSTART:123000
+        END:VEVENT
+        END:VCALENDAR
+        """.trimIndent()
+
+    fun feedItems(): List<FeedItem> = feeds.flatMap(::parseRss)
+
+    fun events(): List<EditionEvent> = parseIcs(calendar)
+}
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt
new file mode 100644
index 0000000..8b480cb
--- /dev/null
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/EditionGenerator.kt
@@ -0,0 +1,58 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.datetime.LocalDate
+
+object EditionGenerator {
+    fun fromFeedResults(
+        date: LocalDate,
+        parsed: List<Result<List<FeedItem>>>,
+        existing: Edition?,
+        topics: List<String> = emptyList(),
+    ): Edition {
+        if (parsed.none { it.isSuccess }) {
+            error(FEED_DIDNT_LOAD)
+        }
+        val items = parsed.mapNotNull { it.getOrNull() }.flatten()
+        val activeTopics = topics.map { it.trim() }.filter { it.isNotEmpty() }
+        val stories = items.withIndex()
+            .sortedWith(
+                compareByDescending<IndexedValue<FeedItem>> { topicFit(it.value, activeTopics) }
+                    .thenBy { it.index },
+            )
+            .mapIndexed { index, indexed ->
+                val item = indexed.value
+                EditionStory(
+                    headline = item.headline,
+                    sourceName = item.sourceName,
+                    deck = item.deck,
+                    sourceUrl = item.sourceUrl,
+                    rank = rankForSourceOrder(index),
+                )
+            }
+        val personal = existing != null && !existing.isSample
+        val failures = parsed.count { it.isFailure }
+        return Edition(
+            id = if (personal) existing.id else "morning-${date.toDayKey()}",
+            revision = if (personal) existing.revision + 1 else 1,
+            dayKey = date.toDayKey(),
+            role = EditionRole.morning,
+            mastheadName = "Matutine",
+            dateLine = date.toDateLine(),
+            isSample = false,
+            briefing = extractiveBriefing(items),
+            stories = stories,
+            events = emptyList(),
+            tasks = emptyList(),
+            sourceNotes = List(failures) { FEED_DIDNT_LOAD },
+        )
+    }
+}
+
+internal fun topicFit(item: FeedItem, topics: List<String>): Int {
+    val headline = item.headline.lowercase()
+    val deck = item.deck.lowercase()
+    return topics.count { topic ->
+        val needle = topic.lowercase()
+        headline.contains(needle) || deck.contains(needle)
+    }
+}
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt
new file mode 100644
index 0000000..070790a
--- /dev/null
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/FeedDocuments.kt
@@ -0,0 +1,111 @@
+package hu.toliver.matutine.edition
+
+const val FEED_DIDNT_LOAD = "That feed didn't load."
+
+data class FeedItem(
+    val sourceName: String,
+    val headline: String,
+    val deck: String,
+    val sourceUrl: String,
+)
+
+fun parseFeedDocument(document: String): List<FeedItem> {
+    if (Regex("<rss\\b", RegexOption.IGNORE_CASE).containsMatchIn(document)) {
+        return parseRss(document)
+    }
+    if (Regex("<feed\\b", RegexOption.IGNORE_CASE).containsMatchIn(document)) {
+        return parseAtom(document)
+    }
+    error(FEED_DIDNT_LOAD)
+}
+
+internal fun parseRss(document: String): List<FeedItem> {
+    val itemPattern = Regex("<item\\b[^>]*>([\\s\\S]*?)</item>", RegexOption.IGNORE_CASE)
+    val items = itemPattern.findAll(document).toList()
+    val head = if (items.isEmpty()) document else document.substring(0, items.first().range.first)
+    val sourceName = tagText(head, "title").ifBlank { "Sample" }
+    return items.map { match ->
+        val body = match.groupValues[1]
+        FeedItem(
+            sourceName = sourceName,
+            headline = tagText(body, "title"),
+            deck = tagText(body, "description"),
+            sourceUrl = tagText(body, "link"),
+        )
+    }.filter { it.headline.isNotBlank() }
+}
+
+internal fun parseAtom(document: String): List<FeedItem> {
+    val entryPattern = Regex("<entry\\b[^>]*>([\\s\\S]*?)</entry>", RegexOption.IGNORE_CASE)
+    val entries = entryPattern.findAll(document).toList()
+    val head = if (entries.isEmpty()) document else document.substring(0, entries.first().range.first)
+    val sourceName = tagText(head, "title").ifBlank { "Feed" }
+    return entries.map { match ->
+        val body = match.groupValues[1]
+        FeedItem(
+            sourceName = sourceName,
+            headline = tagText(body, "title"),
+            deck = tagText(body, "summary").ifBlank { tagText(body, "content") },
+            sourceUrl = atomLink(body),
+        )
+    }.filter { it.headline.isNotBlank() }
+}
+
+internal fun parseIcs(calendar: String): List<EditionEvent> {
+    val eventPattern = Regex("BEGIN:VEVENT([\\s\\S]*?)END:VEVENT", RegexOption.IGNORE_CASE)
+    return eventPattern.findAll(calendar).mapNotNull { match ->
+        val body = match.groupValues[1]
+        val title = lineValue(body, "SUMMARY")?.trim().orEmpty()
+        if (title.isEmpty()) return@mapNotNull null
+        EditionEvent(title = title, timeLabel = formatIcsTime(lineValue(body, "DTSTART").orEmpty()))
+    }.toList()
+}
+
+internal fun formatIcsTime(raw: String): String {
+    val digits = raw.substringAfter('T', raw).takeWhile { it.isDigit() }
+    if (digits.length < 4) return ""
+    val hour = digits.substring(0, 2).toIntOrNull() ?: return ""
+    val minute = digits.substring(2, 4)
+    return "$hour:$minute"
+}
+
+private fun tagText(xml: String, tag: String): String {
+    val match = Regex("<$tag\\b[^>]*>([\\s\\S]*?)</$tag>", RegexOption.IGNORE_CASE).find(xml) ?: return ""
+    return unescapeXml(unwrapCdata(match.groupValues[1].trim()))
+}
+
+private fun atomLink(xml: String): String {
+    val tags = Regex("<(?:[\\w.-]+:)?link\\b([^>]*)/?>", RegexOption.IGNORE_CASE)
+        .findAll(xml)
+        .map { it.groupValues[1] }
+        .toList()
+    val alternate = tags.firstOrNull { attributes ->
+        Regex("rel\\s*=\\s*['\"]alternate['\"]", RegexOption.IGNORE_CASE).containsMatchIn(attributes)
+    }
+    val chosen = alternate ?: tags.firstOrNull()
+    if (chosen != null) {
+        val href = Regex("href\\s*=\\s*['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE).find(chosen)
+        if (href != null) return unescapeXml(href.groupValues[1].trim())
+    }
+    return tagText(xml, "link")
+}
+
+private fun unwrapCdata(text: String): String {
+    val match = Regex("^<!\\[CDATA\\[([\\s\\S]*?)\\]\\]>$").find(text) ?: return text
+    return match.groupValues[1].trim()
+}
+
+private fun lineValue(block: String, name: String): String? =
+    block.lineSequence()
+        .map { it.trim() }
+        .firstOrNull { line ->
+            line.startsWith("$name:", ignoreCase = true) || line.startsWith("$name;", ignoreCase = true)
+        }
+        ?.substringAfter(':')
+
+private fun unescapeXml(text: String): String =
+    text.replace("&lt;", "<")
+        .replace("&gt;", ">")
+        .replace("&quot;", "\"")
+        .replace("&apos;", "'")
+        .replace("&amp;", "&")
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt
new file mode 100644
index 0000000..af61c75
--- /dev/null
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/ReaderSettings.kt
@@ -0,0 +1,22 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.serialization.Serializable
+
+@Serializable
+data class ReaderSettings(
+    val topics: List<String> = emptyList(),
+)
+
+fun decodeReaderSettings(json: String): ReaderSettings =
+    editionJson.decodeFromString(ReaderSettings.serializer(), json)
+
+fun encodeReaderSettings(settings: ReaderSettings): String =
+    editionJson.encodeToString(ReaderSettings.serializer(), settings)
+
+/** Selected choices, then the free line, then day lines. Blank lines are not topics. */
+fun topicsFromInterests(
+    selectedChoices: List<String>,
+    freeLine: String,
+    dayLines: List<String>,
+): List<String> =
+    (selectedChoices + listOf(freeLine) + dayLines).map { it.trim() }.filter { it.isNotEmpty() }
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SampleGenerator.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SampleGenerator.kt
new file mode 100644
index 0000000..19b6393
--- /dev/null
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SampleGenerator.kt
@@ -0,0 +1,46 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.datetime.LocalDate
+
+object SampleGenerator {
+    fun forDay(date: LocalDate): Edition {
+        val items = BundledSources.feedItems()
+        if (items.isEmpty()) error("sample_load_failed")
+        val stories = items.mapIndexed { index, item ->
+            EditionStory(
+                headline = item.headline,
+                sourceName = item.sourceName,
+                deck = item.deck,
+                sourceUrl = item.sourceUrl,
+                rank = rankForSourceOrder(index),
+            )
+        }
+        return Edition(
+            id = "sample-${date.toDayKey()}",
+            revision = 1,
+            dayKey = date.toDayKey(),
+            role = EditionRole.morning,
+            mastheadName = "Matutine",
+            dateLine = date.toDateLine(),
+            isSample = true,
+            briefing = extractiveBriefing(items),
+            stories = stories,
+            events = BundledSources.events(),
+            tasks = emptyList(),
+        )
+    }
+}
+
+internal fun rankForSourceOrder(index: Int): StoryRank = when (index) {
+    0 -> StoryRank.lead
+    1 -> StoryRank.secondary
+    else -> StoryRank.note
+}
+
+internal fun extractiveBriefing(items: List<FeedItem>): String {
+    val summary = items.joinToString(" ") { item ->
+        val text = item.deck.ifBlank { item.headline }.trim()
+        if (text.endsWith(".")) text else "$text."
+    }
+    return "$summary Summary assembled from headlines."
+}
diff --git a/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SourceList.kt b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SourceList.kt
new file mode 100644
index 0000000..e25bf90
--- /dev/null
+++ b/core/src/commonMain/kotlin/hu/toliver/matutine/edition/SourceList.kt
@@ -0,0 +1,17 @@
+package hu.toliver.matutine.edition
+
+import kotlinx.serialization.Serializable
+
+@Serializable
+data class SourceList(
+    val id: String = "",
+    val revision: Long = 0,
+    val feeds: List<String> = emptyList(),
+    val calendars: List<String> = emptyList(),
+)
+
+fun decodeSourceList(json: String): SourceList =
+    editionJson.decodeFromString(SourceList.serializer(), json)
+
+fun encodeSourceList(sourceList: SourceList): String =
+    editionJson.encodeToString(SourceList.serializer(), sourceList)
diff --git a/docs/superpowers/plans/2026-11-08-android-emulator-verify.md b/docs/superpowers/plans/2026-11-08-android-emulator-verify.md
new file mode 100644
index 0000000..9bee5d2
--- /dev/null
+++ b/docs/superpowers/plans/2026-11-08-android-emulator-verify.md
@@ -0,0 +1,758 @@
+# Android Emulator Verification Implementation Plan
+
+> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
+
+**Goal:** Give agents one command that installs the debug app on the local Medium_Phone emulator and prints a screenshot path.
+
+**Architecture:** `scripts/android-emulator.sh` resolves the SDK from `ANDROID_HOME` or `~/Android/Sdk`, reuses a running Medium_Phone, and otherwise boots it. `verify` installs and launches the app, then saves a PNG under `app/androidApp/build/verify/`. `AGENTS.md` tells the agent when to run it. Printed lines rewrite an absolute home path to a `~` path.
+
+**Tech Stack:** bash, the Android SDK `adb` and `emulator` already installed on this machine, Gradle task `:app:androidApp:installDebug`.
+
+## Global Constraints
+
+- Script path is `scripts/android-emulator.sh`. Commands are `verify`, `shot`, and `adb`.
+- Chosen AVD is `ANDROID_AVD`, or `Medium_Phone` when that is unset.
+- SDK is `ANDROID_HOME` when set, otherwise `~/Android/Sdk`. The script puts that SDK's `adb` and `emulator` first on `PATH`.
+- The script does not write `local.properties`.
+- Screenshots and emulator stdout go under `app/androidApp/build/verify/`. `**/build/` is already gitignored.
+- A visible window when `DISPLAY` is set to a non-empty value, and `-no-window` when `DISPLAY` is empty or unset.
+- Leave the emulator running, including a session started from IntelliJ.
+- A running emulator matches the chosen AVD when `adb -s <serial> emu avd name` prints that name. Other connected devices are ignored.
+- When `ANDROID_SERIAL` is unset, export the chosen emulator's serial so Gradle installs only there.
+- Package is `hu.toliver.matutine`. Activity is `hu.toliver.matutine/.MainActivity`.
+- Install command is `./gradlew :app:androidApp:installDebug`.
+- Boot wait is 180 seconds for `sys.boot_completed`. Focus wait is 30 seconds until `dumpsys window` shows `hu.toliver.matutine`.
+- Screenshot name is `app/androidApp/build/verify/YYYYMMDD-HHMMSS.png` in local time. If that file exists, use `-2`, then `-3`, and so on.
+- Success is exit code 0 and the repo-relative PNG path. Every failure is exit code 1, one reason, and the emulator stays up.
+- `shot` does not build or relaunch. If the chosen AVD is down, the message names `verify`.
+- `adb` runs the SDK `adb` and does not boot the emulator.
+- Every printed line rewrites an absolute home path to a `~` path. Committed files say `~/Android/Sdk`.
+- `MATUTINE_GRADLE`, when set, is a test-only executable used instead of `./gradlew :app:androidApp:installDebug`. Unset, the Gradle command above is what runs.
+
+## File structure
+
+- `scripts/android-emulator.sh` — the only user-facing command. Boots, installs, launches, and captures.
+- `scripts/android-emulator_test.sh` — fake SDK tests. They must not touch the real emulator.
+- `AGENTS.md` — the Android verification instruction. This design adds no other rules.
+
+---
+
+### Task 1: Emulator script and fake-SDK tests
+
+**Files:**
+- Create: `scripts/android-emulator_test.sh`
+- Create: `scripts/android-emulator.sh`
+- Test: `scripts/android-emulator_test.sh`
+
+**Interfaces:**
+- Consumes: nothing from earlier tasks.
+- Produces: `scripts/android-emulator.sh verify|shot|adb`. Functions used only inside that file: `rewrite_stream`, `say`, `die`, `run_gradle`, `resolve_sdk`, `avd_installed`, `find_serial`, `start_emulator`, `wait_until_booted`, `run_gradle_filtered`, `wait_until_focused`, `capture_screenshot`, `cmd_verify`, `cmd_shot`, `cmd_adb`, `main`.
+
+- [ ] **Step 1: Write the failing test**
+
+Create `scripts/android-emulator_test.sh` with this exact content:
+
+```bash
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
+SCRIPT="${ROOT}/scripts/android-emulator.sh"
+PASS=0
+FAIL=0
+
+pass() {
+  PASS=$((PASS + 1))
+  printf 'PASS %s\n' "$1"
+}
+
+fail() {
+  FAIL=$((FAIL + 1))
+  printf 'FAIL %s\n' "$1"
+  printf '%s\n' "$2"
+}
+
+run_expect() {
+  local name="$1" expected="$2"
+  shift 2
+  local status
+  set +e
+  LAST_OUTPUT=$("$@" 2>&1)
+  status=$?
+  set -e
+  if [[ "$status" != "$expected" ]]; then
+    fail "$name" "status ${status}, want ${expected}: ${LAST_OUTPUT}"
+    return
+  fi
+  pass "$name"
+}
+
+assert_contains() {
+  local name="$1" haystack="$2" needle="$3"
+  if [[ "$haystack" == *"$needle"* ]]; then
+    pass "$name"
+  else
+    fail "$name" "missing [${needle}] in [${haystack}]"
+  fi
+}
+
+assert_not_contains() {
+  local name="$1" haystack="$2" needle="$3"
+  if [[ "$haystack" == *"$needle"* ]]; then
+    fail "$name" "found [${needle}] in [${haystack}]"
+  else
+    pass "$name"
+  fi
+}
+
+new_fake() {
+  FAKE="$(mktemp -d)"
+  mkdir -p "${FAKE}/home" "${FAKE}/sdk/platform-tools" "${FAKE}/sdk/emulator" "${FAKE}/bin"
+  cat > "${FAKE}/sdk/platform-tools/adb" << EOF
+#!/usr/bin/env bash
+set -euo pipefail
+if [[ "\${1:-}" == "devices" ]]; then
+  echo "List of devices attached"
+  echo "phone-123 device"
+  if [[ -f "${FAKE}/booted" ]]; then
+    echo "emulator-5554 device"
+  fi
+  exit 0
+fi
+if [[ "\${1:-}" == "-s" && "\${3:-}" == "get-state" ]]; then
+  if [[ -f "${FAKE}/booted" ]]; then
+    echo device
+  else
+    echo unknown
+    exit 1
+  fi
+  exit 0
+fi
+if [[ "\${1:-}" == "-s" && "\${3:-}" == "emu" ]]; then
+  echo Medium_Phone
+  echo OK
+  exit 0
+fi
+if [[ "\${1:-}" == "-s" && "\${3:-}" == "shell" && "\${4:-}" == "getprop" ]]; then
+  echo 1
+  exit 0
+fi
+if [[ "\${1:-}" == "-s" && "\${3:-}" == "shell" && "\${4:-}" == "dumpsys" ]]; then
+  echo "  mCurrentFocus=Window{abc u0 hu.toliver.matutine/hu.toliver.matutine.MainActivity}"
+  exit 0
+fi
+if [[ "\${1:-}" == "-s" && "\${3:-}" == "shell" && "\${4:-}" == "am" ]]; then
+  exit 0
+fi
+if [[ "\${1:-}" == "-s" && "\${3:-}" == "exec-out" ]]; then
+  printf '\x89PNG\r\n\x1a\n'
+  exit 0
+fi
+echo "unexpected adb: \$*" >&2
+exit 1
+EOF
+  cat > "${FAKE}/sdk/emulator/emulator" << EOF
+#!/usr/bin/env bash
+printf '%s\n' "\$*" >> "${FAKE}/emulator-args"
+if [[ "\${1:-}" == "-list-avds" ]]; then
+  if [[ -f "${FAKE}/no-avd" ]]; then
+    exit 0
+  fi
+  echo Medium_Phone
+  exit 0
+fi
+if [[ -f "${FAKE}/emulator-exits" ]]; then
+  echo "ERROR locked \${HOME}/Android/Sdk" >&2
+  exit 1
+fi
+touch "${FAKE}/booted"
+sleep 60
+EOF
+  cat > "${FAKE}/bin/gradle" << EOF
+#!/usr/bin/env bash
+printf '%s\n' "\${ANDROID_SERIAL:-}" > "${FAKE}/serial"
+printf '%s\n' "\${ANDROID_HOME:-}" > "${FAKE}/sdk-used"
+if [[ -f "${FAKE}/gradle-fails" ]]; then
+  echo "failure \${HOME}/Android/Sdk"
+  exit 1
+fi
+echo "BUILD SUCCESSFUL"
+echo "\${HOME}/Android/Sdk"
+exit 0
+EOF
+  chmod +x "${FAKE}/sdk/platform-tools/adb" "${FAKE}/sdk/emulator/emulator" "${FAKE}/bin/gradle"
+}
+
+cleanup_pids() {
+  if [[ -n "${FAKE:-}" && -d "$FAKE" ]]; then
+    pkill -f "${FAKE}/sdk/emulator/emulator" >/dev/null 2>&1 || true
+  fi
+}
+
+# --- usage ---
+run_expect "usage exits 1" 1 "$SCRIPT"
+assert_contains "usage names verify" "$LAST_OUTPUT" "verify"
+assert_contains "usage names shot" "$LAST_OUTPUT" "shot"
+assert_contains "usage names adb" "$LAST_OUTPUT" "adb"
+
+run_expect "unknown command exits 1" 1 "$SCRIPT" frob
+assert_contains "unknown names verify" "$LAST_OUTPUT" "verify"
+
+# --- missing sdk ---
+missing="$(mktemp -d)"
+mkdir -p "${missing}/home/Android/Sdk"
+saved_home="$HOME"
+saved_android="${ANDROID_HOME:-}"
+export HOME="${missing}/home"
+export ANDROID_HOME="${HOME}/Android/Sdk"
+run_expect "missing sdk exits 1" 1 "$SCRIPT" verify
+assert_contains "missing sdk mentions tilde" "$LAST_OUTPUT" "~/Android/Sdk"
+assert_not_contains "missing sdk hides home" "$LAST_OUTPUT" "$HOME"
+export HOME="$saved_home"
+if [[ -n "$saved_android" ]]; then
+  export ANDROID_HOME="$saved_android"
+else
+  unset ANDROID_HOME
+fi
+
+# --- shot while down ---
+new_fake
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" \
+  run_expect "shot while down exits 1" 1 "$SCRIPT" shot
+assert_contains "shot while down names verify" "$LAST_OUTPUT" "verify"
+if [[ -f "${FAKE}/emulator-args" ]] && grep -q -- '-avd' "${FAKE}/emulator-args"; then
+  fail "shot while down does not boot" "$(cat "${FAKE}/emulator-args")"
+else
+  pass "shot while down does not boot"
+fi
+cleanup_pids
+
+# --- adb passthrough does not boot ---
+new_fake
+touch "${FAKE}/booted"
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" \
+  run_expect "adb passthrough exits 0" 0 "$SCRIPT" adb devices
+assert_contains "adb lists the emulator" "$LAST_OUTPUT" "emulator-5554"
+if [[ -f "${FAKE}/emulator-args" ]]; then
+  fail "adb passthrough does not start emulator" "$(cat "${FAKE}/emulator-args")"
+else
+  pass "adb passthrough does not start emulator"
+fi
+cleanup_pids
+
+# --- shot of a running emulator ---
+new_fake
+touch "${FAKE}/booted"
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" \
+  run_expect "shot exits 0" 0 "$SCRIPT" shot
+assert_contains "shot prints png" "$LAST_OUTPUT" "app/androidApp/build/verify/"
+assert_contains "shot prints suffix" "$LAST_OUTPUT" ".png"
+assert_not_contains "shot skips gradle" "$LAST_OUTPUT" "BUILD SUCCESSFUL"
+if [[ -f "${FAKE}/emulator-args" ]]; then
+  fail "shot does not boot" "$(cat "${FAKE}/emulator-args")"
+else
+  pass "shot does not boot"
+fi
+png=$(printf '%s\n' "$LAST_OUTPUT" | awk '/\.png$/ { line=$0 } END { print line }')
+magic=$(od -An -t x1 -N 8 -- "${ROOT}/${png}" | tr -d ' \n')
+if [[ "$magic" == "89504e470d0a1a0a" ]]; then
+  pass "shot file is png"
+else
+  fail "shot file is png" "$magic"
+fi
+rm -f "${ROOT}/${png}"
+cleanup_pids
+
+# --- verify happy path, no window ---
+new_fake
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" MATUTINE_GRADLE="${FAKE}/bin/gradle" DISPLAY= \
+  run_expect "verify exits 0" 0 "$SCRIPT" verify
+assert_contains "verify rewrites home" "$LAST_OUTPUT" "~/Android/Sdk"
+assert_not_contains "verify hides home" "$LAST_OUTPUT" "${FAKE}/home"
+assert_contains "verify prints png" "$LAST_OUTPUT" ".png"
+if [[ "$(cat "${FAKE}/serial")" == "emulator-5554" ]]; then
+  pass "verify sets serial"
+else
+  fail "verify sets serial" "$(cat "${FAKE}/serial")"
+fi
+if grep -q -- '-no-window' "${FAKE}/emulator-args"; then
+  pass "verify uses -no-window"
+else
+  fail "verify uses -no-window" "$(cat "${FAKE}/emulator-args")"
+fi
+png=$(printf '%s\n' "$LAST_OUTPUT" | awk '/\.png$/ { line=$0 } END { print line }')
+rm -f "${ROOT}/${png}" "${ROOT}/app/androidApp/build/verify/emulator.log"
+cleanup_pids
+
+# --- verify shows a window when DISPLAY is set ---
+new_fake
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" MATUTINE_GRADLE="${FAKE}/bin/gradle" DISPLAY=:99 \
+  run_expect "verify with display exits 0" 0 "$SCRIPT" verify
+if grep -q -- '-no-window' "${FAKE}/emulator-args"; then
+  fail "verify with display omits -no-window" "$(cat "${FAKE}/emulator-args")"
+else
+  pass "verify with display omits -no-window"
+fi
+png=$(printf '%s\n' "$LAST_OUTPUT" | awk '/\.png$/ { line=$0 } END { print line }')
+rm -f "${ROOT}/${png}" "${ROOT}/app/androidApp/build/verify/emulator.log"
+cleanup_pids
+
+# --- missing avd ---
+new_fake
+touch "${FAKE}/no-avd"
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" \
+  run_expect "missing avd exits 1" 1 "$SCRIPT" verify
+assert_contains "missing avd names avd" "$LAST_OUTPUT" "Medium_Phone"
+if [[ -f "${FAKE}/emulator-args" ]] && grep -w -q -- '-avd' "${FAKE}/emulator-args"; then
+  fail "missing avd does not boot" "$(cat "${FAKE}/emulator-args")"
+else
+  pass "missing avd does not boot"
+fi
+cleanup_pids
+
+# --- emulator exits immediately ---
+new_fake
+touch "${FAKE}/emulator-exits"
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" \
+  run_expect "emulator exit is 1" 1 "$SCRIPT" verify
+assert_contains "emulator exit mentions locked" "$LAST_OUTPUT" "locked"
+assert_contains "emulator exit rewrites home" "$LAST_OUTPUT" "~/Android/Sdk"
+assert_not_contains "emulator exit hides home" "$LAST_OUTPUT" "${FAKE}/home"
+rm -f "${ROOT}/app/androidApp/build/verify/emulator.log"
+cleanup_pids
+
+# --- gradle failure ---
+new_fake
+touch "${FAKE}/booted" "${FAKE}/gradle-fails"
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" MATUTINE_GRADLE="${FAKE}/bin/gradle" \
+  run_expect "gradle failure exits 1" 1 "$SCRIPT" verify
+assert_contains "gradle failure reason" "$LAST_OUTPUT" "installDebug failed"
+assert_contains "gradle failure rewrites home" "$LAST_OUTPUT" "~/Android/Sdk"
+assert_not_contains "gradle failure hides home" "$LAST_OUTPUT" "${FAKE}/home"
+if [[ -f "${FAKE}/emulator-args" ]] && grep -w -q -- '-avd' "${FAKE}/emulator-args"; then
+  fail "gradle failure reuses device" "$(cat "${FAKE}/emulator-args")"
+else
+  pass "gradle failure reuses device"
+fi
+cleanup_pids
+
+# --- second screenshot in the same second gets -2 ---
+new_fake
+touch "${FAKE}/booted"
+cat > "${FAKE}/bin/date" << 'EOF'
+#!/usr/bin/env bash
+echo 20260101-000000
+EOF
+chmod +x "${FAKE}/bin/date"
+mkdir -p "${ROOT}/app/androidApp/build/verify"
+: > "${ROOT}/app/androidApp/build/verify/20260101-000000.png"
+HOME="${FAKE}/home" ANDROID_HOME="${FAKE}/sdk" PATH="${FAKE}/bin:${PATH}" \
+  run_expect "shot collision exits 0" 0 "$SCRIPT" shot
+assert_contains "shot collision suffix" "$LAST_OUTPUT" "20260101-000000-2.png"
+rm -f "${ROOT}/app/androidApp/build/verify/20260101-000000.png" \
+  "${ROOT}/app/androidApp/build/verify/20260101-000000-2.png"
+cleanup_pids
+
+printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
+[[ "$FAIL" -eq 0 ]]
+```
+
+- [ ] **Step 2: Run the test to verify it fails**
+
+Run: `bash scripts/android-emulator_test.sh`
+
+Expected: the run fails immediately because `scripts/android-emulator.sh` does not exist (a "No such file" error from the first `$SCRIPT` invocation).
+
+- [ ] **Step 3: Write the script**
+
+Create `scripts/android-emulator.sh` with this exact content:
+
+```bash
+#!/usr/bin/env bash
+# Boot Medium_Phone, install the debug app, and capture a screenshot.
+# SDK resolution: ANDROID_HOME, otherwise ~/Android/Sdk.
+set -euo pipefail
+
+ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
+PACKAGE="hu.toliver.matutine"
+ACTIVITY="${PACKAGE}/.MainActivity"
+VERIFY_DIR="app/androidApp/build/verify"
+
+rewrite_stream() {
+  local line
+  while IFS= read -r line || [[ -n "$line" ]]; do
+    if [[ -n "${HOME:-}" ]]; then
+      line=${line//"$HOME"/"~"}
+    fi
+    printf '%s\n' "$line"
+  done
+}
+
+say() {
+  printf '%s\n' "$*" | rewrite_stream >&2
+}
+
+die() {
+  say "$*"
+  exit 1
+}
+
+# MATUTINE_GRADLE is a test hook. Unset, this runs the debug install.
+run_gradle() {
+  if [[ -n "${MATUTINE_GRADLE:-}" ]]; then
+    "$MATUTINE_GRADLE"
+  else
+    ./gradlew :app:androidApp:installDebug
+  fi
+}
+
+resolve_sdk() {
+  if [[ -n "${ANDROID_HOME:-}" ]]; then
+    SDK=$ANDROID_HOME
+  else
+    SDK="${HOME}/Android/Sdk"
+  fi
+  ADB="${SDK}/platform-tools/adb"
+  EMULATOR="${SDK}/emulator/emulator"
+  AVD="${ANDROID_AVD:-Medium_Phone}"
+  if [[ ! -x "$ADB" || ! -x "$EMULATOR" ]]; then
+    die "SDK adb or emulator is missing under ${SDK}. Install the SDK at ~/Android/Sdk or set ANDROID_HOME."
+  fi
+}
+
+avd_installed() {
+  "$EMULATOR" -list-avds | tr -d '\r' | grep -qx "$AVD"
+}
+
+find_serial() {
+  local serial state name
+  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
+    state=$("$ADB" -s "$ANDROID_SERIAL" get-state 2>/dev/null | tr -d '\r' || true)
+    if [[ "$state" == "device" ]]; then
+      printf '%s\n' "$ANDROID_SERIAL"
+      return 0
+    fi
+    return 1
+  fi
+  while read -r serial state; do
+    [[ "$state" == "device" ]] || continue
+    [[ "$serial" == emulator-* ]] || continue
+    name=$("$ADB" -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | awk 'NF && $0 != "OK" { print; exit }')
+    if [[ "$name" == "$AVD" ]]; then
+      printf '%s\n' "$serial"
+      return 0
+    fi
+  done < <("$ADB" devices | awk 'NR>1 && NF>=2 { print $1, $2 }')
+  return 1
+}
+
+start_emulator() {
+  mkdir -p "$VERIFY_DIR"
+  local log="${VERIFY_DIR}/emulator.log"
+  if [[ -n "${DISPLAY:-}" ]]; then
+    nohup "$EMULATOR" -avd "$AVD" >"$log" 2>&1 &
+  else
+    nohup "$EMULATOR" -avd "$AVD" -no-window >"$log" 2>&1 &
+  fi
+  EMU_PID=$!
+}
+
+wait_until_booted() {
+  local deadline=$((SECONDS + 180)) serial value
+  while (( SECONDS < deadline )); do
+    if serial=$(find_serial); then
+      value=$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
+      if [[ "$value" == "1" ]]; then
+        SERIAL=$serial
+        return 0
+      fi
+    fi
+    if [[ -n "${EMU_PID:-}" ]] && ! kill -0 "$EMU_PID" 2>/dev/null; then
+      say "AVD ${AVD} is locked or the emulator exited before adb could see it."
+      if [[ -f "${VERIFY_DIR}/emulator.log" ]]; then
+        rewrite_stream < "${VERIFY_DIR}/emulator.log" >&2 || true
+      fi
+      exit 1
+    fi
+    sleep 1
+  done
+  die "AVD ${AVD} did not finish booting within 180 seconds."
+}
+
+run_gradle_filtered() {
+  local output status
+  set +e
+  output=$(run_gradle 2>&1)
+  status=$?
+  set -e
+  if [[ "$status" -ne 0 ]]; then
+    say "installDebug failed"
+    printf '%s\n' "$output" | rewrite_stream >&2
+    return 1
+  fi
+  printf '%s\n' "$output" | rewrite_stream
+  return 0
+}
+
+wait_until_focused() {
+  local deadline=$((SECONDS + 30))
+  while (( SECONDS < deadline )); do
+    if "$ADB" -s "$SERIAL" shell dumpsys window 2>/dev/null | tr -d '\r' | grep -E 'mCurrentFocus|mFocusedApp' | grep -q "$PACKAGE"; then
+      return 0
+    fi
+    sleep 1
+  done
+  return 1
+}
+
+capture_screenshot() {
+  local base rel n magic
+  mkdir -p "$VERIFY_DIR"
+  base=$(date +%Y%m%d-%H%M%S)
+  rel="${VERIFY_DIR}/${base}.png"
+  n=2
+  while [[ -e "$rel" ]]; do
+    rel="${VERIFY_DIR}/${base}-${n}.png"
+    n=$((n + 1))
+  done
+  if ! "$ADB" -s "$SERIAL" exec-out screencap -p >"$rel"; then
+    die "Screenshot is not a PNG."
+  fi
+  magic=$(od -An -t x1 -N 8 -- "$rel" 2>/dev/null | tr -d ' \n' || true)
+  if [[ "$magic" != "89504e470d0a1a0a" ]]; then
+    die "Screenshot is not a PNG."
+  fi
+  printf '%s\n' "$rel"
+}
+
+cmd_verify() {
+  resolve_sdk
+  export ANDROID_HOME="$SDK"
+  export PATH="${SDK}/platform-tools:${SDK}/emulator:${PATH}"
+  if ! avd_installed; then
+    die "AVD ${AVD} was not found by emulator -list-avds."
+  fi
+  if ! find_serial >/dev/null; then
+    start_emulator
+  fi
+  wait_until_booted
+  export ANDROID_SERIAL="$SERIAL"
+  run_gradle_filtered
+  if ! "$ADB" -s "$SERIAL" shell am start -n "$ACTIVITY" >/dev/null; then
+    die "Could not start ${ACTIVITY}."
+  fi
+  if ! wait_until_focused; then
+    die "${PACKAGE} was not in front within 30 seconds of launch."
+  fi
+  capture_screenshot
+}
+
+cmd_shot() {
+  resolve_sdk
+  export ANDROID_HOME="$SDK"
+  if ! serial=$(find_serial); then
+    die "AVD ${AVD} is not running. Run verify."
+  fi
+  SERIAL=$serial
+  export ANDROID_SERIAL="$SERIAL"
+  capture_screenshot
+}
+
+cmd_adb() {
+  resolve_sdk
+  if [[ -z "${ANDROID_SERIAL:-}" ]]; then
+    if serial=$(find_serial); then
+      export ANDROID_SERIAL="$serial"
+    fi
+  fi
+  exec "$ADB" "$@"
+}
+
+main() {
+  local cmd="${1:-}"
+  case "$cmd" in
+    verify|shot|adb) ;;
+    *)
+      die "Usage: scripts/android-emulator.sh <verify|shot|adb> [args...]"
+      ;;
+  esac
+  shift
+  cd "$ROOT"
+  case "$cmd" in
+    verify) cmd_verify ;;
+    shot) cmd_shot ;;
+    adb) cmd_adb "$@" ;;
+  esac
+}
+
+if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
+  main "$@"
+fi
+```
+
+Then mark it executable:
+
+```bash
+chmod +x scripts/android-emulator.sh scripts/android-emulator_test.sh
+```
+
+- [ ] **Step 4: Run the test to verify it passes**
+
+Run: `bash scripts/android-emulator_test.sh`
+
+Expected: `43 passed, 0 failed` and exit code 0.
+
+- [ ] **Step 5: Commit**
+
+```bash
+git add scripts/android-emulator.sh scripts/android-emulator_test.sh
+git commit -m "$(cat <<'EOF'
+Add a script that verifies the Android app on the emulator.
+
+Agents need one command that installs the debug build and saves a screenshot without printing an absolute home path.
+EOF
+)"
+```
+
+---
+
+### Task 2: Agent instruction
+
+**Files:**
+- Create: `AGENTS.md`
+- Test: `AGENTS.md` (grep checks below)
+
+**Interfaces:**
+- Consumes: `scripts/android-emulator.sh verify`, `shot`, and `adb` from Task 1.
+- Produces: the `## Android verification` section agents read before an Android UI change.
+
+- [ ] **Step 1: Write the failing check**
+
+Run:
+
+```bash
+test -f AGENTS.md && grep -F 'scripts/android-emulator.sh verify' AGENTS.md
+```
+
+Expected: exit code 1 because `AGENTS.md` is absent.
+
+- [ ] **Step 2: Write `AGENTS.md`**
+
+Create `AGENTS.md` with this exact content:
+
+```markdown
+## Android verification
+
+After a change to Android UI, run `scripts/android-emulator.sh verify` and look at the screenshot path it prints. Use `scripts/android-emulator.sh shot` for another capture of the current screen. Send taps through `scripts/android-emulator.sh adb`. Leave the emulator running.
+
+Maestro flows and screenshot baselines are optional when a task needs scripted steps or a pixel check.
+
+In committed files, commits, and review comments, write `~/Android/Sdk`. Do not write an absolute home-directory path.
+```
+
+- [ ] **Step 3: Run the checks**
+
+Run:
+
+```bash
+grep -F 'scripts/android-emulator.sh verify' AGENTS.md
+grep -F '~/Android/Sdk' AGENTS.md scripts/android-emulator.sh
+! grep -n '/home/' AGENTS.md scripts/android-emulator.sh
+```
+
+Expected: the two `grep -F` commands print matches. The third command exits 0 because `AGENTS.md` and the script contain no `/home/` path. The test file is not part of that check; its fixture directory is named `home` under a temp folder.
+
+- [ ] **Step 4: Commit**
+
+```bash
+git add AGENTS.md
+git commit -m "$(cat <<'EOF'
+Tell agents to verify Android UI on the emulator.
+
+The instruction points at the script and keeps home-directory paths out of committed text.
+EOF
+)"
+```
+
+---
+
+### Task 3: Real Medium_Phone checklist
+
+**Files:**
+- Modify: none unless a step fails and the script needs a fix.
+- Test: the checklist below, against the SDK at `~/Android/Sdk` and the `Medium_Phone` AVD.
+
+**Interfaces:**
+- Consumes: `scripts/android-emulator.sh` and `AGENTS.md`.
+- Produces: evidence that `verify` and `shot` work on the real emulator. Do not commit PNGs or `emulator.log`.
+
+Unset `MATUTINE_GRADLE` and `ANDROID_SERIAL` for every command in this task. Leave `ANDROID_HOME` unset unless it already points at the SDK that contains `Medium_Phone`.
+
+- [ ] **Step 1: Usage**
+
+Run: `scripts/android-emulator.sh`
+
+Expected: exit code 1, and the text contains `verify`, `shot`, and `adb`.
+
+- [ ] **Step 2: `shot` while the AVD is down**
+
+If `scripts/android-emulator.sh adb devices` already shows an emulator, skip this step and record "skipped: Medium_Phone already running".
+
+Otherwise run: `scripts/android-emulator.sh shot`
+
+Expected: exit code 1, and the text contains `verify`.
+
+- [ ] **Step 3: `verify`**
+
+Run: `scripts/android-emulator.sh verify`
+
+Expected: exit code 0. The output contains a path matching `app/androidApp/build/verify/` and ending in `.png`. That file's first 8 bytes are the PNG magic `89 50 4e 47 0d 0a 1a 0a`. The output does not contain the expanded home directory (`grep -F "$HOME"` on the saved output finds nothing).
+
+Confirm the app is in front:
+
+```bash
+scripts/android-emulator.sh adb shell dumpsys window | tr -d '\r' | grep -E 'mCurrentFocus|mFocusedApp'
+```
+
+Expected: a line containing `hu.toliver.matutine`.
+
+Confirm the emulator is still connected:
+
+```bash
+scripts/android-emulator.sh adb devices
+scripts/android-emulator.sh adb emu avd name
+```
+
+Expected: a serial in the `device` state, and `Medium_Phone` from `emu avd name`.
+
+- [ ] **Step 4: `shot` does not rebuild**
+
+Run: `scripts/android-emulator.sh shot`
+
+Expected: exit code 0, a second path ending in `.png`, and the output does not contain `installDebug` or `BUILD`.
+
+- [ ] **Step 5: Nothing sensitive is tracked**
+
+Run:
+
+```bash
+git status --short -- app/androidApp/build/verify AGENTS.md scripts/android-emulator.sh scripts/android-emulator_test.sh
+```
+
+Expected: the status does not list a `.png` or `emulator.log`. `AGENTS.md` and the two scripts are committed already, so they are absent from this short status unless a checklist failure edited them.
+
+- [ ] **Step 6: Commit a fix only if the checklist changed the script**
+
+If steps 1–5 passed with no edits, do not create a commit.
+
+If the script had to change, re-run `bash scripts/android-emulator_test.sh` and expect `43 passed, 0 failed`, then commit only the script and its test:
+
+```bash
+git add scripts/android-emulator.sh scripts/android-emulator_test.sh
+git commit -m "$(cat <<'EOF'
+Fix emulator verification against the running Medium_Phone AVD.
+
+The real device checklist disagreed with the fake SDK tests.
+EOF
+)"
+```
diff --git a/docs/superpowers/specs/2026-11-08-android-emulator-verify-design.md b/docs/superpowers/specs/2026-11-08-android-emulator-verify-design.md
new file mode 100644
index 0000000..b67f033
--- /dev/null
+++ b/docs/superpowers/specs/2026-11-08-android-emulator-verify-design.md
@@ -0,0 +1,90 @@
+# Android emulator verification
+
+Date: 2026-11-08
+
+Agents that change Android UI in this repo must be able to install a debug build on the local `Medium_Phone` emulator, open Matutine, and read a screenshot of the real screen. The emulator stays installed on this computer. The repo holds a script and a short agent instruction.
+
+Maestro flows and screenshot baselines are out of scope. An agent may add them later for a task that needs scripted steps or a pixel diff.
+
+## Architecture
+
+Two committed pieces:
+
+- `scripts/android-emulator.sh` is the only command an agent or a person runs for this check.
+- `AGENTS.md` at the repo root tells the agent when to run it.
+
+The script resolves the SDK from `ANDROID_HOME` when that is set, otherwise from `~/Android/Sdk`. It puts that SDK's `adb` and `emulator` first on `PATH`. Gradle sees the SDK through `ANDROID_HOME` in the script's process. The script does not write `local.properties`. That file stays gitignored.
+
+Screenshots and the emulator's own stdout go under `app/androidApp/build/verify/`. `**/build/` is already gitignored.
+
+The script reuses a running `Medium_Phone`, including a session started from IntelliJ. If that AVD is down, `verify` boots it: a visible window when `DISPLAY` is set to a non-empty value, and `-no-window` when `DISPLAY` is empty or unset. The script leaves the emulator running. A running emulator matches the chosen AVD when `adb -s <serial> emu avd name` prints that name.
+
+Every line the script prints has an absolute home path rewritten to a `~` path. Committed files say `~/Android/Sdk`.
+
+## Agent instruction
+
+`AGENTS.md` contains this section and no other rules from this design:
+
+```markdown
+## Android verification
+
+After a change to Android UI, run `scripts/android-emulator.sh verify` and look at the screenshot path it prints. Use `scripts/android-emulator.sh shot` for another capture of the current screen. Send taps through `scripts/android-emulator.sh adb`. Leave the emulator running.
+
+Maestro flows and screenshot baselines are optional when a task needs scripted steps or a pixel check.
+
+In committed files, commits, and review comments, write `~/Android/Sdk`. Do not write an absolute home-directory path.
+```
+
+## Command contract
+
+`scripts/android-emulator.sh` is bash. `ANDROID_AVD` overrides the AVD name; the default is `Medium_Phone`. `ANDROID_SERIAL` overrides the device. When `ANDROID_SERIAL` is unset, the script selects the running emulator whose AVD name is the chosen AVD and exports that serial so Gradle installs only there. Other connected devices are ignored.
+
+### `verify`
+
+1. Resolve the SDK. Exit 1 if `adb` or `emulator` is missing there.
+2. Reuse a running chosen AVD. If it is down, start it in the background and wait up to 180 seconds for `sys.boot_completed`. Emulator stdout goes to `app/androidApp/build/verify/emulator.log`.
+3. Export `ANDROID_HOME` and `ANDROID_SERIAL`, then run `./gradlew :app:androidApp:installDebug`.
+4. Start `hu.toliver.matutine/.MainActivity` and wait up to 30 seconds until `dumpsys window` shows package `hu.toliver.matutine` focused.
+5. Save `app/androidApp/build/verify/YYYYMMDD-HHMMSS.png` using local time. If that file already exists, use `YYYYMMDD-HHMMSS-2.png`, then `-3`, and so on. Check that the file is a PNG, and print that repo-relative path.
+
+Success is exit code 0 and that path. The emulator stays up.
+
+### `shot`
+
+Capture the current screen to a new PNG under `app/androidApp/build/verify/` and print the repo-relative path. Do not build, install, or relaunch. If the chosen AVD is not already running, exit 1 and tell the caller to run `verify`.
+
+### `adb`
+
+Run the SDK `adb` with the remaining arguments. When the chosen AVD is already up and `ANDROID_SERIAL` is unset, target that serial. Do not boot the emulator.
+
+### Usage
+
+With no arguments, or with an unknown command, exit 1 and print the three command names: `verify`, `shot`, and `adb`.
+
+## Error handling
+
+Every failure exits with code 1, prints one reason, and leaves the emulator running. A failed `verify` does not kill a session started from IntelliJ, and it does not kill an emulator the script just booted. Stdout and stderr have absolute home paths rewritten to `~` paths before they are shown.
+
+Exit 1 in these cases:
+
+- The SDK `adb` or `emulator` binary is missing under `ANDROID_HOME` or `~/Android/Sdk`.
+- The chosen AVD (`ANDROID_AVD`, or `Medium_Phone` when that is unset) is not in `emulator -list-avds`.
+- `shot` runs while the chosen AVD is down. The message names `verify`.
+- The AVD lock is already held and `adb` cannot see that emulator.
+- Boot does not finish within 180 seconds.
+- `./gradlew :app:androidApp:installDebug` fails. The reason line is followed by the Gradle output, with home paths rewritten.
+- `hu.toliver.matutine` is not in front within 30 seconds of launch.
+- The screenshot is missing or is not a PNG.
+
+`adb` does not boot a device. If none is up, the SDK `adb` reports that itself.
+
+## Testing
+
+No CI job. `Medium_Phone` is on this computer. During implementation, run this checklist in order:
+
+1. `scripts/android-emulator.sh` with no arguments exits 1 and prints `verify`, `shot`, and `adb`.
+2. With `Medium_Phone` not running, `shot` exits 1 and the message names `verify`. If the AVD is already up, skip this step and say so.
+3. `verify` exits 0, prints a path under `app/androidApp/build/verify/` ending in `.png`, and that file is a PNG. The app is in front, and `Medium_Phone` is still connected afterward.
+4. `shot` prints a second PNG path and does not run Gradle.
+5. The output of those runs contains no absolute home path. Compare against the home directory at runtime.
+6. The script and `AGENTS.md` mention `~/Android/Sdk` and contain no absolute home path. `git status` does not list the screenshots or `emulator.log`.

```
