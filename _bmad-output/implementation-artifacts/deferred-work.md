- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md`
  summary: The edition screen's generating line, feed notice, and source notes are not composed in a test.
  evidence: View-model flags and sourceNotes are asserted, and the common test source set does not run Compose. Deleting the three text blocks in EditionContent would still leave those tests green.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-generate-today-s-edition-from-a-feed.md`
  summary: The Android INTERNET permission is not exercised by a device fetch.
  evidence: Removing the permission leaves the JVM and shared unit tests green. A device fetch would be needed to show that a missing permission becomes "That feed didn't load."

- source_spec: `_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md`
  summary: The Android and JVM feed readers are duplicated, and the Android cleartext flag is not fetched on a device.
  evidence: The copies and the manifest flag come from the feed reader, not from topic ranking. A device HTTP fetch would show whether cleartext still loads.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md`
  summary: Interests colors, the no-topic line, and the two questions are not composed in a test.
  evidence: Common tests assert the copy and `showsNoTopicLine` only. The test source set does not run Compose, so deleting those text blocks would stay green.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md`
  summary: The Android settings file is not the store the JVM test writes.
  evidence: `FileEditionStoreTest` runs the JVM actual. An Android host test would be needed to show the Android `settings.json` round trip.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-6-name-topics-and-change-the-lead.md`
  summary: JS and Wasm settings and feed stubs are only compiled.
  evidence: Those actuals return null and fail the feed read. A JS or Wasm test is outside the story's compile check.
