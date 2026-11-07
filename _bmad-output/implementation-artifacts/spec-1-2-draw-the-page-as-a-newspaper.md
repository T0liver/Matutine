---
title: '1-2 Draw the page as a newspaper'
type: 'feature'
created: '2026-11-07'
status: 'review'
route: 'oneshot'
review_loop_iteration: 0
story_key: '1-2-draw-the-page-as-a-newspaper'
context:
  - _bmad-output/implementation-artifacts/epic-1-context.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Matutine-2026-11-07/DESIGN.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 1.1 renders the edition with default Material typography and colors, so ranks do not read as a broadsheet and Newsprint tokens from DESIGN.md are missing.

**Approach:** Embed Newsreader, Source Serif 4, and IBM Plex Mono in `:app:shared`, add a Newsprint theme and type ramp, and restyle `EditionScreen` (masthead, ranks, kickers, hairlines, empty copy) without card chrome or rank animation.

## Implementation Notes

- Embedded `newsreader_medium`, `source_serif4_regular`, and `ibm_plex_mono_regular` under `app/shared/.../composeResources/font` with `publicResClass = true`.
- Added `NewsprintColors`, `MatutineEditionTypography`, and `MatutineTheme`; `MatutineApp` wraps the edition in Newsprint instead of default Material.
- Restyled `EditionScreen`: masthead accent rule, ink page rule, rank headline sizes, rule hairlines, empty-section copy, stroked task checkbox, story semantics unchanged.
- Tests: `EditionCopyTest`, `StoryRankSizesTest`.

</frozen-after-approval>

## Verification

**Commands:**
- `./gradlew :core:allTests :app:shared:allTests :app:desktopApp:compileKotlin :app:androidApp:assembleDebug` -- expected: BUILD SUCCESSFUL
