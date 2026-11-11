---
title: 'Document the server judge path'
type: 'feature'
created: '2026-11-11'
status: 'done'
route: 'oneshot'
story_key: '3-7-document-the-server-judge-path'
baseline_commit: 'b9228cd850211a44c32180b6f19e985f9d3cea94'
review_loop_iteration: 0
context:
  - _bmad-output/implementation-artifacts/epic-3-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The README still tells judges that Docker, the server, and the browser are documented elsewhere. Milestone B needs Sam to continue from the Android and Desktop judge path to a LAN Docker server and the same sample newspaper in the browser.

**Approach:** Extend `README.md` after the existing mobile and Desktop sections with Docker build/run, LAN browser URL, and the `matutine-demo` token gate. Keep the path free of accounts, AI keys, `java -jar`, and public-internet exposure. Update the intro and non-goals list to match.

</frozen-after-approval>

## Implementation Notes

- Extended the judge path with Docker build/run, LAN browser URL, default token `matutine-demo`, and sample-edition expectations aligned with Story 1.10.
- Clarified non-goals: no public exposure, no registry pull, contributor `./gradlew :server:run` is not the judge path.
- Skipped Blind Hunter review: session instructions forbade child agents.

## Verification

**Commands:**
- `docker --version` — Docker 29.9.3 available on the host.
- `./gradlew :server:test --tests 'hu.toliver.matutine.ApplicationTest.absentSourceList_returnsTheServerSampleAndDoesNotWriteTheList'` — exit 0 (confirms `matutine-demo` and sample edition on first `GET /editions/current`).

**Manual checks (not run in this session):**
- Full `docker build -f server/Dockerfile` and browser smoke on `http://127.0.0.1:8080/` (first Docker build is slow; steps match `server/Dockerfile` and epic-3 context).

## Review Triage Log

- Review layers skipped — session disallowed spawning child agents; README steps checked against epic-3 context, `server/Dockerfile`, and passing `ApplicationTest`.
