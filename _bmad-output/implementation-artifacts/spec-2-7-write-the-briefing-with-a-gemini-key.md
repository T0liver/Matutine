---
title: '2-7 Write the briefing with a Gemini key'
type: 'feature'
created: '2026-11-10'
status: 'in-review'
baseline_commit: 'fd8aa8ffad6e0a644fdeeee090c499ae251bbb5f'
route: 'dispatch'
review_loop_iteration: 0
story_key: '2-7-write-the-briefing-with-a-gemini-key'
context:
  - _bmad-output/implementation-artifacts/epic-2-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A saved free-tier Gemini key never writes the opening. Every generated briefing stays the extractive summary plus "Summary assembled from headlines."

**Approach:** Settings stores an optional Gemini key in local config. A new morning paper and a new extra make one summarization call and store that text as the briefing. A missing key or a failed call leaves the extractive briefing and still saves the edition.

## Boundaries & Constraints

**Always:** The key lives only in local settings, never in the git tree, and is not sent to a Matutine server. At most one summarization call for each new briefing that is stored. Model text has no model name and no sparkle, and does not add the extractive sentence. Personal regenerate keeps the briefing already on that morning document and does not call. Sample load and sample retry stay on the bundled extractive briefing. Story 2.6's kept briefing, calendar lines, and tasks stay as they are. A disappeared source item gets no new merge rule.

**Never:** OpenRouter, Claude, OpenAI, xAI, a local model, a provider picker, a paid-credit model, a chat box, a follow-up field, a second call after failure, or a key in source, tests, or fixtures.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Key writes a new paper | Local key set; first personal morning, including replacing a sample, or a new extra | Briefing is the model text; the call count for that document is one | N/A |
| No key | Key blank or settings absent | Extractive summary plus "Summary assembled from headlines."; no call | Edition completes |
| Call fails | Key set; call throws or returns blank | Same extractive briefing; no second call | Edition completes |
| Regenerate | Personal morning already stored, key set | Existing briefing stays; no call | N/A |
| Save key | Typed key, or a blank field | Non-blank key replaces the saved key; blank clears it | A failed write keeps the previous key |

</frozen-after-approval>

## Code Map

- `core/.../edition/ReaderSettings.kt` — add `geminiKey`, default `""`. Decode of older settings must keep working.
- `core/.../edition/EditionRepository.kt` — `saveGeminiKey`. `LocalEditionRepository` takes a `summarize` lambda, same style as `readFeed`. Call it only when storing a new briefing and the trimmed key is non-blank. `keepMorningEdits` still owns personal regenerate. Sample paths do not call.
- `core/.../edition/SampleGenerator.kt` — `extractiveBriefing` stays the fallback text. Do not call from `SampleGenerator.forDay`.
- `core/.../edition/EditionModels.kt` — briefing remains one string. Do not add a model field.
- `app/shared/.../edition/GeminiBriefing.kt` — prompt and response parse. No HTTP here.
- `app/shared/.../edition/GeminiBriefingClient.kt` — expect/actual POST to the Gemini free-tier generateContent endpoint. Key in a header, not the prompt and not a Matutine server. JVM and Android implement it. JS and wasm fail so the fallback still completes.
- `app/shared/.../MatutineApp.kt` and `app/androidApp/.../MorningEditionWorker.kt` — pass the client into the repository. Do not add a second generator.
- `app/shared/.../edition/EditionViewModel.kt` and `settings/SettingsScreen.kt` — one masked "Gemini key" row. Save does not call the network. Generate persists a dirty key before it generates, and a failed save keeps the previous key.
- `app/shared/.../edition/EditionScreen.kt` — briefing is already one read-only block. Do not add a model name, a sparkle, or a chat field.
- Do not change `keepMorningEdits`, the typewriter, or the disappeared-source behavior.

## Tasks & Acceptance

**Execution:**
- [x] `core/.../ReaderSettings.kt` and `EditionRepository.kt` — key field, save, and one optional summarization when a new briefing is stored
- [x] `core/src/commonTest/.../LocalEditionRepositoryTest.kt` — matrix rows for key, missing key, failure, regenerate, extra, and sample
- [x] `app/shared/.../GeminiBriefing.kt` plus JVM/Android/JS/wasm client — free-tier Gemini adapter and response parse test
- [x] `EditionViewModel.kt`, `SettingsScreen.kt`, `MatutineApp.kt`, `MorningEditionWorker.kt` — masked key row wired into the same repository

**Acceptance Criteria:**
- Given a Gemini key in local config and not in the git tree, when a new edition is generated, then one summarization is stored as the briefing, with no model name and no sparkle.
- Given the key is missing or the call fails, when generation finishes, then the briefing is the extractive summary plus "Summary assembled from headlines." and the edition completes.
- Given a personal morning already has a briefing, when O generates again, then that briefing stays and no summarization runs.
- Given Settings, when O saves a key, then no provider that needs paid credits can be selected, and there is no chat box.

## Implementation Notes

- The Gemini key is `ReaderSettings.geminiKey` in local `settings.json`. Save trims it. A blank value clears it. A thrown write leaves the previous document.
- `withModelBriefing` runs only for a new morning briefing and a new extra. Personal regenerate still goes through `keepMorningEdits` and does not call. Sample load and retry do not call.
- The adapter posts to the `gemini-2.5-flash` generateContent URL with `x-goog-api-key`. JVM and Android implement HTTP. JS and wasm fail the call so generation still finishes with the extractive briefing. No key string was added to the tree.
- Settings shows one masked Gemini key row. Generate saves a dirty key first. A failed save restores the previous key and still generates.
- Verified with `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` (BUILD SUCCESSFUL). No emulator.

## Spec Change Log

## Review Triage Log

## Design Notes

The port is a lambda, not a vendor type in `:core`. The adapter names `gemini-2.5-flash` and posts to `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent` with header `x-goog-api-key`. The prompt is the edition's headline and deck lines and asks for a short plain opening. The stored briefing is the joined candidate text. Personal regenerate skips the lambda because story 2.6 already keeps that field.

## Verification

**Commands:**
- `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin` -- expected: BUILD SUCCESSFUL
