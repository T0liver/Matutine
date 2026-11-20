# Contest: Docker starts when the token env var is unset

Status: done (2026-11-20). Docker boot without `MATUTINE_TOKEN` verified; server test `unsetMatutineToken_envPathBootsAndAcceptsDemoBearer`.
Suggested epic: contest-hardening. Do not mark Epic 3 done from this ticket.

## Problem

A judge who follows `README.md` runs:

```bash
docker run --rm -p 8080:8080 -v matutine-data:/data matutine-server
```

with no `MATUTINE_TOKEN`. On 2026-11-18 that process exited 1. `NullPointerException` at `server/src/main/kotlin/hu/toliver/matutine/Application.kt:75`. `ServerRuntime.envToken` is a non-null `String`, and `System.getenv("MATUTINE_TOKEN")` is null. `resolveSharedToken` already maps a missing token to `matutine-demo`, but that function never runs. The same image stayed up when started with `-e MATUTINE_TOKEN=matutine-demo`.

## Acceptance criteria

The README command, with no `MATUTINE_TOKEN`, keeps the server up on port 8080. `http://127.0.0.1:8080/` loads, and bearer token `matutine-demo` opens today's edition. A non-blank `MATUTINE_TOKEN` still replaces that default. No secret is baked into the image. Add a server test that boots the module the way the Docker process does, with the token variable unset.
