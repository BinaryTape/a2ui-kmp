# Changelog

## 1.0.0-alpha02

Upstream: androidx.a2ui 1.0.0-alpha02 (2026-10-21), commit `e0a272dcae2b`. Synced 2026-10-29.

### Upstream changes

- Initial sync of `source/a2ui/` and `source/material3-a2ui/`.

### Port changes

- First build of every upstream module for desktop JVM, iosArm64 and iosSimulatorArm64:
  `a2ui-model`, `a2ui-engine`, `compose-runtime`, `compose-ui`, `compose-ui-testing` and
  `material3-a2ui`.
- Requirements (README "Requirements"): `a2ui-model`, `a2ui-engine`, `compose-runtime`, `compose-ui`
  and `compose-ui-testing` are built with Kotlin 2.3, Compose runtime 1.12.1 and Compose
  Multiplatform 1.12.2, a verified baseline: they compile and pass upstream's tests with these, and
  earlier versions were not tried (upstream builds with Kotlin 2.2 and Compose 1.13.0-alpha03). `material3-a2ui` needs Compose Multiplatform
  1.13.0-beta01 and so Kotlin 2.4, and is built separately (`target/material3-a2ui-kmp`).
- compose-ui uses JetBrains Compose Multiplatform for Compose UI on desktop and iOS.
  Its pluralize formatting (ICU MessageFormat on Android) uses our MessageFormat with CLDR 45
  plural rules; on iOS, numbers use CLDR's default numbering system, as ICU does.
- material3-a2ui uses JetBrains Compose Multiplatform's material3 1.13.0-beta01 (Google's material3
  1.5.0-beta01 on Android; upstream builds with 1.5.0-alpha28). Its `DatePickerDialog` is
  experimental in that build for desktop and iOS, so the module opts in to `ExperimentalMaterial3Api`.
- material3-a2ui's string resources (11 strings in 85 locales) are generated from upstream's
  `res/values*/strings.xml`. The locale is chosen as Android chooses resource directories: a port of
  AOSP's resource locale matching with its likely-script, region-parent and representative-locale
  tables (`scripts/generate-android-locale-data.py`, Android 15).
- `java.text.DateFormat` and `java.util.Calendar` for DateTimeInput: the MEDIUM date and SHORT time
  patterns of every locale are Android 15's, recorded on an emulator
  (`scripts/record-android-date-patterns.py`); `android.text.format.DateFormat.is24HourFormat` uses
  the user's setting on iOS and the default locale on desktop.
- One hand-made overlay: `ShimmerIndicator.kt`. Android changes a shader's local matrix each frame;
  Skia shaders cannot change, so it draws a gradient made for the frame.
  `scripts/generate-overlays.py` records the upstream file's hash and fails when it changes.
- `A2uiBasicCatalogV1.Functions(context)`: on desktop and iOS the Context is ours and opens URLs
  with the platform; on Android it is android.content.Context, so common code cannot call it there.
- Android variants forward to Google's artifacts. `a2ui-compat` supplies `Locale`
  for common code.

### How each upstream file is built
| Package | Kotlin files | As-is | Generated overlay | Hand-made overlay | Not built yet | Built for KMP |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| a2ui-model | 66 | 19 | 47 | 0 | 0 | 66/66 (100.0%) |
| a2ui-engine | 20 | 12 | 8 | 0 | 0 | 20/20 (100.0%) |
| compose-runtime | 14 | 9 | 5 | 0 | 0 | 14/14 (100.0%) |
| compose-ui | 6 | 3 | 3 | 0 | 0 | 6/6 (100.0%) |
| compose-ui-testing | 4 | 3 | 1 | 0 | 0 | 4/4 (100.0%) |
| material3-a2ui | 83 | 78 | 4 | 1 | 0 | 83/83 (100.0%) |
| **Total** | **193** | **124** | **68** | **1** | **0** | **193/193 (100.0%)** |

Generated overlays by rule (a file can have several):

- `class-java-simple-name`: 1
- `compose-imports`: 4
- `jvm-default-imports`: 8
- `kotlin-jvm-imports`: 50
- `locale-constants`: 5
- `platform-imports`: 20
- `platform-locale`: 1

Our adapter code (`a2ui-compat`, in `target/a2ui-kmp`), which the overlays import, maintained by us (and data our
scripts generate or record, not edited by hand):

- commonMain: 27 files, 2192 lines (+ 4 data files, 2272 lines)
- desktopMain: 13 files, 245 lines
- iosMain: 14 files, 658 lines
- total: 54 files, 3095 lines (+ 4 data files, 2272 lines)

In the modules, besides overlays:

- material3-a2ui: 1 files, 27 lines of ours; 1 files, 989 lines generated from upstream resources

### Verified

- Upstream unit tests on desktop (JDK 21), unchanged: a2ui-model 499, a2ui-engine 266,
  compose-runtime 258, compose-ui 381, all passing. compose-runtime's two Robolectric classes run under plain JUnit.
- Compiles for iosArm64 and iosSimulatorArm64. The upstream tests are JVM-only and do not run on iOS.
- `tests/integration`, which uses only the published artifacts, on desktop, Android local JVM, iOS
  simulator (iOS 27.1) and an Android 15 emulator (API 35, official artifacts):
  - `Locale` passes through the API in both directions from common code.
  - Number, currency and date formatting, with Android's results as the reference: 260 date pattern
    cases (52 letters, widths 1 to 5), quoting, invalid patterns, JPY and KWD.
  - 30 JSON messages, valid and malformed, including a leading byte order mark, must give Android's
    result exactly, error messages included.
  - On the Android emulator, our Unicode identifier classification (used by `formatString`) equals
    Android's `Character` for all 65,536 UTF-16 chars; desktop and iOS use that table.
  - The engine creates, updates and deletes a surface.
  - Plural messages: 836 cases (20 locales, numbers beyond Long, quoting, `=N`, offset, number
    styles, `selectordinal`, invalid patterns) must give android.icu.text.MessageFormat's result.
  - Numbers in 50 locales and dates in 19 locales (calendar, digits, AM/PM) must give Android's
    java.text results; desktop and iOS use digit and AM/PM tables recorded from Android.
  - compose-ui: a custom component's catalog gives Android's JSON schema exactly, and its message
    processor creates a surface from parsed JSON messages.
- Upstream's instrumented tests run unchanged on desktop with Compose Multiplatform's
  `runComposeUiTest` (`tests/android-test-stand-ins` gives the Android test runner and annotations
  the same names): 527 of material3-a2ui's and all 57 of compose-ui-testing's pass, as do their 36
  and 6 unit tests. Four material3-a2ui tests that press Android's back key are skipped; four that
  use `StateRestorationTester`, which Compose Multiplatform does not implement on desktop, are
  excluded by name.
- `tests/integration` also checks material3-a2ui from the published artifacts on every platform:
  - The resource directory chosen for 174 locales (every resource locale, English, Spanish and
    Portuguese regions, Chinese and Serbian scripts, legacy codes, locales without a directory,
    Unicode extensions and private use) equals what Android chooses on the emulator.
  - MEDIUM dates and SHORT times in 29 locales (4 with extensions or private use), and 11 `Calendar` cases (lenient fields, `clear`,
    dates before 1970 and 1582), against Android's results. On the emulator, compat formats all 853 available
    locales as Android does.
  - `materialA2uiBasicCatalogV1(...)` gives the official catalog's JSON schema.
- `a2ui-compat` tests on desktop and iOS simulator: the lock upstream creates with fair = true serves
  waiting threads in arrival order.
- Known differences from Android, each pinned to its exact result in the tests:
  - Desktop uses the JDK's date formatter: `S`, five-letter `E`/`L`/`M`, `ZZZZ`/`ZZZZZ` differ, and
    `b`, `B` and `c` throw.
  - iOS names UTC "Greenwich Mean Time" for `zzzz`; Android says "Coordinated Universal Time".
  - Number separators: desktop for pa-PK and az-IR, iOS for sd-PK and uz-Latn-AF; and the yen sign
    in ja-JP on iOS ("¥" for Android's "￥").
  - On iOS, JSON written from upstream HashMaps (catalog schemas, data model) has the same content
    as on Android with keys in another order (Kotlin/Native HashMaps keep insertion order).
  - Date patterns were compared in UTC and at one instant; other time zones were not.
- Reference device: Android 15 emulator (API 35) with ICU 75.1, CLDR 45, Unicode 15.1.
- Android apps using the official 1.0.0-alpha02 artifacts need compileSdk 37.1.
