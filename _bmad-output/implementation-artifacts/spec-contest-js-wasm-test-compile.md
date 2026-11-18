# Contest: `./gradlew test check` compiles on JS and Wasm

Status: review resolutions first. Implement only after O picks one.
Suggested epic: contest-hardening. Do not mark any epic done from this ticket.

## Problem

On 2026-11-18 `./gradlew test check --console=plain` exited 1 in 48s. `:core:compileTestKotlinJs` and `:core:compileTestKotlinWasmJs` failed. Common tests call `runBlocking`, which those targets do not provide (`Unresolved reference 'runBlocking'`, and suspend calls outside a coroutine). `:server:test`, `:core:jvmTest`, and `:app:shared:jvmTest` were already green (183 tests). Android, Desktop, and the server app are not what failed. The web client still served.

## What to do

Review the failure and write two or three concrete resolutions in this file (for example: keep common tests JVM-only, replace `runBlocking` with a multiplatform test helper, or split the blocking tests out of `commonTest`). Stop there and wait for O to choose. After a choice is recorded here, implement that resolution in this same ticket and show `./gradlew test check` green, or name the remaining target that is still red and why.
