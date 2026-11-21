# Contest: a Gemini key produces a model briefing

Status: done (2026-11-21). Shared `GEMINI_GENERATE_URL` moved to `core/.../GeminiBriefing.kt` on `gemini-flash-lite-latest`; verified live with a throwaway key (HTTP 200 resolving to `gemini-3.5-flash-lite`) and fallback on missing/invalid keys, with no key stored in git or settings.
Suggested epic: contest-hardening (Story 2.7). Do not mark Epic 2 done from this ticket.

## Problem

`GEMINI_GENERATE_URL` is hardcoded to `v1beta/models/gemini-2.5-flash:generateContent` in `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefing.kt` and again in `server/src/main/kotlin/hu/toliver/matutine/ServerGeneration.kt`. On 2026-11-18 a current key, sent as the server does (`POST`, header `x-goog-api-key`, key not in the URL), returned HTTP 404: `models/gemini-2.5-flash` is no longer available to new users. The error named `models/gemini-3.8-flash`. `withModelBriefing` catches the failure and keeps the extractive briefing, so the paper still completes, and Story 2.7's with-key outcome does not happen. The key is not in git.

## What to do

Read the current official Gemini documentation before changing the call. Try more than the one suggested model. Prefer an official alias or auto route that follows the cheapest currently supported model, if such a parameter exists, over pinning another id that will expire the same way. Change the shared call (Android, Desktop, and server must stay on one definition). Ask O for a throwaway key if one is not already in the environment. Do not write the key into the repo, the image, or this ticket.

## Acceptance criteria

One generation with a valid key stores model prose as the briefing: no model name, no sparkle, one summarization. The validation evidence is a real Gemini response body with generated text, plus the edition text that came from it. A missing key or a failed call still finishes the edition on the extractive briefing ending in "Summary assembled from headlines." The unit test that currently asserts the URL contains `gemini-2.5-flash` is updated to the chosen contract.

## Verification

- Probed candidate `v1beta/models/*:generateContent` routes on 2026-11-21 with a throwaway key (`x-goog-api-key`, key not saved): `gemini-2.5-flash` returned `404`; `gemini-flash-latest` and `gemini-4-hydrogen` returned `503`; `gemini-3.8-flash` timed out at 20s; `gemini-3.5-flash-lite` and the rolling alias `gemini-flash-lite-latest` returned `200` (`modelVersion: "gemini-3.5-flash-lite"`).
- Moved `GEMINI_GENERATE_URL`, `geminiBriefingPrompt`, `geminiGenerateBody`, and `briefingTextFromGeminiResponse` into `core/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefing.kt`, shared by Android, Desktop (`:app:shared`), and `:server`.
- Verified `./gradlew :core:jvmTest :app:shared:jvmTest :app:androidApp:testDebugUnitTest :app:desktopApp:compileKotlin :server:test` and `:app:shared:compileKotlinJs :app:shared:compileTestKotlinJs :server:installDist` green.
- Verified live `POST /v1beta/models/gemini-flash-lite-latest:generateContent` returned model prose (captured response `"responseId": "-1oBa_T9JKiBxs0Ph8ChyQQ"`, `"modelVersion": "gemini-3.5-flash-lite"`), rendered into an edition briefing without model name or sparkle; verified live server `POST /editions/morning/regenerate` produced model prose with a valid key (without writing `settings.json`) and kept `"Summary assembled from headlines."` on missing or invalid keys.
