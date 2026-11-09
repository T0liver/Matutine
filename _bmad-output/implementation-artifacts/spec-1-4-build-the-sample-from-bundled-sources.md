---
title: '1-4 Build the sample from bundled sources'
type: 'feature'
created: '2026-11-08'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
story_key: '1-4-build-the-sample-from-bundled-sources'
context:
  - _bmad-output/implementation-artifacts/epic-1-context.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** First launch still shows a hand-written sample edition. The store only knows whether a source-list file exists, so an absent list and an empty list are not different, and the paper is not built from bundled feeds and a bundled calendar.

**Approach:** When the source-list document is absent, the core generator builds today's sample from bundled feed and calendar fixtures, keeps the sample line, and does not write those fixtures into a source list. A saved morning edition is returned as stored. An existing list, including an empty one, does not use the bundled sample.

</frozen-after-approval>

## Implementation Notes

- An absent source list is a missing `sources.json`. `readSourceList()` returns null only then. A decoded document, including empty feed and calendar lists, is present and does not use the bundled sample.
- `SampleGenerator` parses three bundled RSS documents and one ICS calendar. With no topics, source order stands: first story is the lead, second is secondary, and the rest are notes.
- The briefing is the item descriptions plus "Summary assembled from headlines." There is no model call.
- Tasks stay empty. This story has no task source, and the screen already shows "No tasks yet."
- The generated sample is saved as today's morning edition. The source list is not written.
- A saved morning edition is returned as stored, and the generator is not called.
- kotlinx-datetime is 0.7.1 and an api dependency of `:core`, matching the version the clients already resolve. `todayLocalDate` uses `kotlin.time.Clock`. The date line uses `LocalDate.day`.
- JavaScript and Wasm stores stay non-persistent placeholders.
- `:core:allTests` and `:app:shared:allTests` did not run. Yarn 1.22.22 could not be downloaded (502, then a timeout). JVM tests, Android host tests, JS and Wasm compilation, desktop compilation, and Android `assembleDebug` succeeded.

## Review Triage Log

- Invalid or blank `sources.json` throws from decode instead of becoming an empty list — false. A file that exists is not absent, so the sample is not built. Decode fails loudly. Nothing in this story writes a blank file, and `{}` is covered.
- JavaScript and Wasm stores still look absent and do not persist — false. Those stubs already did not store a list or an edition. This story keeps that placeholder behavior.
- Sample tasks are gone — low, not fixed. The sample is built from feeds and a calendar. There is no task source in this story, and inventing task lines would put a picture back. The empty copy is already "No tasks yet."
- ICS all-day values can be read as clock times, and sample events are not filtered by day — false for the bundled calendar. The fixture times are `090000` and `123000`, and the sample agenda is the edition's calendar rather than a dated subscription. A general ICS parser is Story 1.7.
- RSS parsing drops items it does not understand — false for the shipped fixtures. The three bundled documents parse to three stories in source order. A failed-feed rule belongs to Story 1.5.
- Briefing text is the decks plus the required suffix — low, not fixed. The suffix is the no-key sentence from the requirements. The extract is the item descriptions, and no length was specified.
- An empty list shows "Today's morning edition is not available yet." — false as a regression. That is the existing result when a source list is present and today's paper is missing. The acceptance criterion is that the sample is not used.
- Disk cold-start, `retrySample`, and the date line are thinly tested — low, not fixed. The repository test returns a saved edition without calling the generator, and the file test checks that generation does not create `sources.json`. `LocalDate.day` is the renamed day-of-month property.
- Spec notes were empty and the frozen intent is short — false as a product defect. Notes are filled here. The oneshot spec keeps the frozen block to the story intent.
- Saved sample id and revision stay `sample-<day>` and `1`, and retry writes that edition again — false as a new bug. Story 1.1 already saved the sample that way. Retry is the explicit reload. Cold start reads the stored edition first.
