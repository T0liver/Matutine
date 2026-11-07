# Version reality check — Daily newspaper spine

Lens: every committed technology pin was checked against live public sources on 2026-11-07. Training-data memory was not treated as evidence. The spine’s Stack section says “Verified 2026-11-07” and does not cite URLs; this review supplies the sources.

Verdict: **Pass with findings.** Every named version exists, the stated dates match the publishers, and the decision to stay off AGP 9.5.1 matches Kotlin’s published compatibility ceiling. The check is incomplete on the AGP 9.3 patch line.

## Claim ledger

| Claim | Result | What the live source says |
| --- | --- | --- |
| Kotlin 2.4.21, released 2026-10-08, from kotlinlang.org | Confirmed, and it is the current stable | [Kotlin FAQ](https://kotlinlang.org/docs/faq.html) says the released version is 2.4.21, published 8 October 2026. [Release history](https://kotlinlang.org/docs/releases.html) lists “2.4.21 Released: October 8, 2026” and the 2.4 line’s latest as 2.4.21. [GitHub v2.4.21](https://github.com/jetbrains/kotlin/releases) is dated 08 Oct 2026. |
| Ktor 3.6.1, released 2026-10-30 | Confirmed, and it is the current stable | [Ktor releases](https://ktor.io/docs/releases.html) lists 3.6.1 on October 30, 2026 as the top row. [ktor.io](https://ktor.io/) says “Latest release: 3.6.1”. [Changelog 3.6](https://ktor.io/changelog/3.6/) says “released 30th October 2026”. [GitHub 3.6.1](https://github.com/ktorio/ktor/releases) is 30 Oct 2026. The Gradle plugin portal page for `io.ktor.plugin` 3.6.1 is marked latest, created 30 October 2026. |
| Kotlin 2.4.21 fully supports AGP through 9.3.1; AGP 9.5.1 exists and is not the pin | Confirmed, with a gap on 9.3.2 and 9.3.3 | [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html): KGP 2.4.20–2.4.21 fully supports Gradle 7.6.3–9.7.0 and AGP 8.5.2–9.3.1. The same page says newer Gradle and AGP can be used, with possible deprecation warnings or new features that do not work. [AGP API reference](https://developer.android.com/reference/tools/gradle-api) lists current release 9.5.1, preview 9.6.0-alpha03, and 9.3.3 among past releases. Google Maven `com.android.tools.build:gradle` metadata lists 9.3.1, 9.3.2, 9.3.3, 9.5.0, and 9.5.1. |
| Gradle 9.5.0 is the AGP 9.3 default | Confirmed | [AGP 9.3.0 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes) compatibility table: Gradle minimum 9.5.0, default 9.5.0. [About AGP](https://developer.android.com/build/releases/about-agp) lists plugin 9.3 as requiring Gradle 9.5.0. 9.5.0 is inside Kotlin’s fully supported Gradle range, which ends at 9.7.0. |
| JDK 17 is the AGP 9.3 default | Confirmed for the build JDK | The same AGP 9.3 compatibility table lists JDK minimum 17 and default 17. This is the JDK that runs the Android Gradle Plugin, not the Kotlin `jvmTarget` written in current samples. |
| kotlinx-serialization 1.12.0, released 2026-10-19 | Confirmed, and it is the current stable | [CHANGELOG.md](https://github.com/Kotlin/kotlinx.serialization/blob/HEAD/CHANGELOG.md) opens with `1.12.0 / 2026-10-19`. [GitHub v1.12.0](https://github.com/Kotlin/kotlinx.serialization/releases) is 19 Oct 2026. The next older stable in that changelog is 1.11.0 / 2026-04-10. Maven Central has `kotlinx-serialization-json` 1.12.0. |
| `com.android.kotlin.multiplatform.library` on `shared` | Confirmed, and it fits AGP 9 | [Android’s KMP library plugin page](https://developer.android.com/kotlin/multiplatform/plugin) calls this the supported plugin for an Android target on a KMP library. `com.android.library` on a KMP module depends on APIs that are deprecated and opt-in from AGP 9.0 and scheduled for removal in AGP 10. [JetBrains AGP 9 migration](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html) says the KMP plugin is no longer compatible with `com.android.application` or `com.android.library` in the same module, and the shared module should use `com.android.kotlin.multiplatform.library`. That matches AD-17: the library plugin on `shared`, `com.android.application` only in `androidApp`. |

## Findings

### 1. AGP 9.3.1 is the Kotlin ceiling, and it is not the newest 9.3 patch

The spine is right that Kotlin’s fully supported AGP maximum for 2.4.21 is 9.3.1, and that 9.5.1 is a newer line that should not be the pin if the project stays inside that ceiling. AGP 9.5 also changes the Gradle default to 9.6.0, which is still inside Kotlin’s Gradle range (through 9.7.0) but is a different wrapper than the one the spine pins.

The spine does not mention the patches Google has already published on the 9.3 line. Google Maven lists 9.3.2 and 9.3.3 after 9.3.1. The [AGP 9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes), last updated 2026-10-28, record this under 9.3.2:

- Lint issue #522845800: “AGP 9.3 lint crashes on JDK 17 — NoSuchMethodError: java.util.List.removeLast() in bundled intellij-core JavaDocParser.”

9.3.1’s own notes say no public issues were marked fixed in that patch. Pinning 9.3.1 together with the AGP 9.3 JDK default of 17 therefore selects the patch before the documented JDK 17 lint crash fix. Kotlin’s table still stops at 9.3.1, so 9.3.2 and 9.3.3 are past the published “fully supported” maximum even though they are the same minor line. That is a real compatibility tradeoff. The spine frames the only alternative as 9.5.1, which hides it.

### 2. “JDK 17” is the plugin runtime default, not the sample bytecode target

AGP 9.3’s compatibility table does list JDK 17 as both minimum and default. [Java versions in Android builds](https://developer.android.com/build/jdks) treats that as the JDK required to run AGP. Current setup samples do not default Kotlin bytecode to 17:

- The JetBrains AGP 9 migration snippet sets `jvmTarget` to `JvmTarget.JVM_11`.
- Google’s KMP library plugin page still shows an older sample with `JvmTarget.JVM_1_8` and `compileSdk = 33`. That sample is stale relative to AGP 9.3, whose notes say the maximum supported API level is 37.

The stack row is accurate if it means the JDK that runs Gradle. It would be wrong if an implementer reads it as the live `jvmTarget` default of the Android or KMP starter samples.

### 3. No starter template is pinned, so wizard defaults were not part of this decision

The spine does not name the KMP wizard, an Android Studio template, or a sample repository. AD-17 matches the current Google and JetBrains plugin guidance, which is the right check for a hand-specified module layout. It is not evidence that a generated project’s version catalog was opened.

The [May 2026 JetBrains post](https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/) says the new default structure is live in the KMP wizard at kmp.jetbrains.com and in the IDE plugin. This review did not download a generated project, so wizard pins for Kotlin, AGP, Gradle, and JDK were not compared with the spine. Google’s own plugin page still documents two configuration shapes: `android {}` inside `kotlin {}` from AGP 8.12, with `androidLibrary {}` deprecated since AGP 9.1.0-alpha09, while the JetBrains migration page still shows `androidLibrary {}`. The spine avoids that split by naming only the plugin id.

### 4. The other pins are current stables, with one library/compiler skew that is normal

- Kotlin 2.4.21 is the latest stable on kotlinlang.org. A 2.5 beta has been discussed elsewhere; it is not the released version on the FAQ or the release table.
- Ktor 3.6.1 is the latest on ktor.io, eight days before this review. 3.6.0 (17 September 2026) added default client engines for Kotlin Multiplatform, so 3.6.1 is a fit for a KMP shared module plus a JVM server.
- kotlinx-serialization 1.12.0 is the latest stable. Its changelog says the release is based on Kotlin 2.4.10. That is older than the pinned compiler 2.4.21 and is the usual library-versus-compiler lag, not a missing newer serialization release.
- Gradle 9.5.0 is both the minimum and the default written for AGP 9.3, and it sits inside Kotlin 2.4.21’s fully supported Gradle range.

## Sources opened

- https://kotlinlang.org/docs/faq.html
- https://kotlinlang.org/docs/releases.html
- https://github.com/jetbrains/kotlin/releases
- https://kotlinlang.org/docs/gradle-configure-project.html
- https://ktor.io/docs/releases.html
- https://ktor.io/changelog/3.6/
- https://ktor.io/
- https://github.com/ktorio/ktor/releases
- https://plugins.gradle.org/plugin/io.ktor.plugin/3.6.1
- https://developer.android.com/build/releases/agp-9-3-0-release-notes
- https://developer.android.com/build/releases/agp-9-5-0-release-notes
- https://developer.android.com/build/releases/about-agp
- https://developer.android.com/reference/tools/gradle-api
- https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml
- https://developer.android.com/build/jdks
- https://developer.android.com/kotlin/multiplatform/plugin
- https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html
- https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/
- https://github.com/Kotlin/kotlinx.serialization/blob/HEAD/CHANGELOG.md
- https://github.com/Kotlin/kotlinx.serialization/releases
