# Contest: verify the browser token gate

Status: ready to verify in the browser, then fix whatever fails.
Suggested epic: contest-hardening (Stories 3.1 and 3.7). Do not mark those stories done from this ticket.

## Problem

On 2026-11-18 the page at `http://127.0.0.1:8080/` drew a newsprint screen with the label "Token" and a "Continue" button. The demo token and the sentence "That token doesn't match." are in the README and in `webPaperGate`, not on that first screen. A programmatic click on Continue did not produce a `GET /editions/current` in the server log, so the button was not proven. The API itself is fine: bearer `wrong-token` returns 401 `unauthorized`, bearer `matutine-demo` returns the sample edition. This gate uses the shared token, not a Gemini key.

## What to do

Start the server the way the README will work after the unset-token fix, or pass `-e MATUTINE_TOKEN=matutine-demo` until that fix lands. In a browser, enter a wrong token and confirm the field keeps the text and shows "That token doesn't match." Then enter `matutine-demo` and confirm today's edition. Note any further issues (empty field, Continue doing nothing, missing copy) and fix them in this ticket. A Gemini key is not required.

## Acceptance criteria

A person can finish the README browser steps: wrong token stays on the gate with that sentence, `matutine-demo` opens the sample edition. Evidence is the browser text before and after each submit, plus the matching server log lines.
