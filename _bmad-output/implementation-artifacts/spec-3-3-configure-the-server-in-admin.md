---
title: 'Configure the server in Admin'
type: 'feature'
created: '2026-11-10'
status: 'in-progress'
baseline_commit: '4950eb881e9c47277090d91900df761f7cb18889'
route: 'dispatch'
review_loop_iteration: 0
story_key: '3-3-configure-the-server-in-admin'
context:
  - _bmad-output/implementation-artifacts/epic-3-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The LAN server has no browser configuration. Sources, clock, topics, and the AI key live only in per-device settings or environment variables.

**Approach:** Add Admin on the web newspaper (no Settings screen) and server HTTP routes so one bearer-authenticated screen can save sources, process config, interests, and Gemini key, then regenerate today's morning edition on the server.

## Boundaries & Constraints

**Always:** Valid token opens the newspaper, then Admin via a chrome link. Admin sets feed, ICS, CalDAV, generation time, timezone, interests (Story 1.6 UI), Gemini provider label and key, and shared token. Bad feed or calendar shows the existing device sentences; prior stored values stay until save succeeds. Source list is created only on first successful save when absent. Generate from Admin calls `POST /editions/morning/regenerate` with empty AI key → personal edition, no sample line, topic-ranked lead, extractive briefing.

**Never:** No Todoist, no extra AI providers, no theme/layout/notification on Admin, no web generator, no emulator verification this story.

</frozen-after-approval>

## Tasks & Acceptance

**Execution:**
- [x] `core/.../ServerProcessConfig.kt` and `EditionStore` process document
- [x] `server/.../ServerRoutes.kt`, `ServerRuntime.kt`, `ServerCalDav.kt`
- [x] `app/shared/.../admin/AdminScreen.kt`, `EditionScreen.kt` Admin link
- [x] `app/webApp/...` ServerApi, ServerAdminModel, `main.kt` navigation
- [x] `server/.../ApplicationTest.kt` admin route rows

**Acceptance Criteria:** Epic 3.3 AC (Admin fields, error copy, generate outcome).

## Verification

- `./gradlew :server:test` — 21 tests pass
- `./gradlew :core:jvmTest` — pass
- `./gradlew :app:webApp:compileKotlinWasmJs` — pass
