# Contest: a Gemini key produces a model briefing

Status: ready for an agent to implement.
Suggested epic: contest-hardening (Story 2.7). Do not mark Epic 2 done from this ticket.

## Problem

`GEMINI_GENERATE_URL` is hardcoded to `v1beta/models/gemini-2.5-flash:generateContent` in `app/shared/src/commonMain/kotlin/hu/toliver/matutine/edition/GeminiBriefing.kt` and again in `server/src/main/kotlin/hu/toliver/matutine/ServerGeneration.kt`. On 2026-11-18 a current key, sent as the server does (`POST`, header `x-goog-api-key`, key not in the URL), returned HTTP 404: `models/gemini-2.5-flash` is no longer available to new users. The error named `models/gemini-3.8-flash`. `withModelBriefing` catches the failure and keeps the extractive briefing, so the paper still completes, and Story 2.7's with-key outcome does not happen. The key is not in git.

## What to do

Read the current official Gemini documentation before changing the call. Try more than the one suggested model. Prefer an official alias or auto route that follows the cheapest currently supported model, if such a parameter exists, over pinning another id that will expire the same way. Change the shared call (Android, Desktop, and server must stay on one definition). Ask O for a throwaway key if one is not already in the environment. Do not write the key into the repo, the image, or this ticket.

## Acceptance criteria

One generation with a valid key stores model prose as the briefing: no model name, no sparkle, one summarization. The validation evidence is a real Gemini response body with generated text, plus the edition text that came from it. A missing key or a failed call still finishes the edition on the extractive briefing ending in "Summary assembled from headlines." The unit test that currently asserts the URL contains `gemini-2.5-flash` is updated to the chosen contract.
