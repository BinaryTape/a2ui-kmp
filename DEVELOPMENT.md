# Development

## Repository layout

```
source/                         the AndroidX source snapshot
  UPSTREAM                      the AndroidX commit they come from
  a2ui/                         a2ui-model, a2ui-engine, compose/compose-runtime, compose-ui, compose-ui-testing
  material3-a2ui/
target/                         the Kotlin Multiplatform builds, and all our code
  a2ui-kmp/                     the build of source/a2ui, in the same layout
    a2ui-model/  a2ui-engine/  compose/compose-runtime/  compose/compose-ui/  compose/compose-ui-testing/
    a2ui-compat/                our equivalents of the JDK and Android APIs the sources use
  material3-a2ui-kmp/           the build of source/material3-a2ui, which needs newer Kotlin and
                                Compose; it uses the other packages as a2ui-kmp publishes them
  build-logic/                  the build logic of both builds, an included build (the a2ui.publish
                                plugin and the module helpers); each build applies its own Kotlin
gradle/libs.versions.toml       every version both builds and tests/integration use
tests/integration/              a separate build that uses only the published artifacts, like an app
tests/android-test-stand-ins/   Android test APIs, so upstream's instrumented tests run on desktop
samples/                        a desktop and iOS app that uses the published artifacts
scripts/                        sync, overlay generation, checks, verification
```

In each module of `target/`:

```
overlay-generated/   copies of upstream files with a mechanical change, written by scripts/generate-overlays.py
overlay-manual/      hand-made replacements of upstream files; overlay-manual.json records which
src/                 additional platform code
```

The repository root is a composite build (`settings.gradle.kts`) of `target/build-logic`, both
builds and `samples`: open it in the IDE to work on all of them, each with its own Kotlin version.
There the sample app uses the packages built from source, so you can debug into them.
`tests/integration` is not part of it and is opened on its own: it exists to test what users
receive, the published artifacts (see its `settings.gradle.kts`).

## How the sources are built

Most upstream files compile as they are. A file that uses an API missing outside Android, such as
`java.text.SimpleDateFormat` or `android.util.JsonReader`, gets a generated overlay that imports our
equivalent from `a2ui-compat` instead. The rewrite rules are fixed and listed in
`scripts/generate-overlays.py`; each module's `overlay-generated.json` records which rules changed
which file, and the hash of the upstream file. material3-a2ui's string resources are generated from
upstream's `res/` into `target/material3-a2ui-kmp/resources-generated`, and chosen per locale the
way Android chooses resource directories. The changelog counts, for each package, the files used
as they are, through generated overlays, and through hand-made overlays.

The reference for behaviour is the official artifacts on Android. `tests/integration` runs on desktop,
the iOS simulator and an Android device, and compares each platform with results recorded on the
device; every known difference is pinned to its exact result there and listed in the changelog.

## Building and verifying

Every build in the repository runs with its one Gradle wrapper, `./gradlew -p <build>` (the sample
app has its own, as a separate project):

```sh
./gradlew -p target/a2ui-kmp publishAllPublicationsToLocalRepository
./gradlew -p target/material3-a2ui-kmp publishAllPublicationsToLocalRepository   # needs the first
scripts/verify.sh                                                                  # everything below
```

It publishes `target/a2ui-kmp` and then `target/material3-a2ui-kmp` to `build/repo`, checks that the
artifacts require what README's "Requirements" table says (`scripts/check-requirements.py`), runs
upstream's tests on desktop and ours on the iOS simulator, runs `tests/integration` twice
(`minimum`: the five packages with the Kotlin they require; `all`: every package) on desktop, the
Android host, the iOS simulator and, if one is connected, an Android device or emulator, and builds
and starts the sample app on desktop and the iOS simulator (`scripts/verify-samples.sh`). Each phase
also runs on its own, after `publish`: `scripts/verify.sh integration`, for example.

To use a local build from another project, add `maven { url = uri("<a2ui-kmp>/build/repo") }`
to its repositories, before the others. The sample app takes it as `-Pa2uiRepo=<repository>`.

## Updating to a new upstream version

```sh
scripts/sync-upstream.py <androidx commit> --release "androidx.a2ui <version> (<date>)" --version <version>
scripts/verify.sh
```

The sync script replaces `source/a2ui/` and `source/material3-a2ui/`, regenerates the overlays, sets
the versions in `gradle/libs.versions.toml` (`a2uiKmp`, `a2uiUpstream`) and in the sample app's version
catalog, and starts
a changelog entry.

## CI and releases

CI (`.github/workflows/ci.yml`) runs on every push and pull request. First the quick checks:
`scripts/check-upstream.py` compares `source/` with the commit in `source/UPSTREAM` file by file, the
generated overlays must be current, and the changelog entry for the version being built must be
complete. Then one macOS job publishes both builds to `build/repo` (`scripts/verify.sh publish
requirements`), and four jobs test those artifacts in parallel: upstream's tests, the integration
tests and the samples on macOS (`scripts/verify.sh upstream-tests`, `integration`, `samples`), and
the integration tests' Android device part on an emulator, in both passes. A change to Markdown files
or screenshots only runs the quick checks, unless it changes README's "Requirements" table. Each
Gradle build publishes a Build Scan, linked from the job summary.

To release, set `a2uiKmp` in `gradle/libs.versions.toml`, finish its `CHANGELOG.md`
entry, and push tag `v<version>`. `.github/workflows/release.yml` checks that the tag, the version
and the changelog entry agree, runs CI on the tagged commit, and only then uploads the signed
artifacts to Maven Central, from `target/a2ui-kmp` and then from `target/material3-a2ui-kmp`, and creates a
draft GitHub release with the changelog entry as notes. A maintainer then releases both uploads in
the [Central Portal](https://central.sonatype.com/publishing/deployments) and publishes the draft.
