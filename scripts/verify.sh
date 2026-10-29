#!/usr/bin/env bash
# Builds target/a2ui-kmp (five packages) and then target/material3-a2ui-kmp (material3-a2ui, built
# against the first five as published), runs upstream's tests on desktop and ours on the iOS
# simulator, and publishes both to build/repo. Checks that the published artifacts require what
# README's "Requirements" table says. Then runs tests/integration against the published artifacts
# twice, "minimum" (the five packages with the Kotlin they require) and "all" (every package), on
# desktop, Android host, iOS simulator, and on an Android device or emulator if one is connected
# (adb). Finally builds and checks the samples against the same artifacts on desktop and the iOS
# simulator (scripts/verify-samples.sh).
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
repo="$root/build/repo"

# The version being built (gradle/libs.versions.toml). Every build here runs with the repository's
# one Gradle wrapper (./gradlew -p <build>).
version="$(sed -n 's/^a2uiKmp = "\(.*\)"$/\1/p' "$root/gradle/libs.versions.toml")"
[ -n "$version" ] || { echo "a2uiKmp missing from gradle/libs.versions.toml" >&2; exit 1; }
gradlew="$root/gradlew"
echo "Verifying a2ui-kmp $version"

# tests/integration has an Android target: use ANDROID_HOME, or the default SDK location on macOS.
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
[ -d "$ANDROID_HOME" ] || { echo "Android SDK not found: set ANDROID_HOME" >&2; exit 1; }

python3 "$root/scripts/generate-overlays.py"
rm -rf "$repo"
for build in a2ui-kmp material3-a2ui-kmp; do
    "$gradlew" --quiet -p "$root/target/$build" desktopTest iosSimulatorArm64Test publishAllPublicationsToLocalRepository
done
python3 "$root/scripts/check-requirements.py"

tasks=(desktopTest testAndroidHostTest iosSimulatorArm64Test)
if adb get-state >/dev/null 2>&1; then
    tasks+=(connectedAndroidDeviceTest)
else
    echo "No Android device connected: skipping the device tests."
fi
for run in minimum all; do
    echo "Integration run: $run"
    "$gradlew" --quiet -p "$root/tests/integration" --refresh-dependencies -Pa2uiIntegration="$run" "${tasks[@]}"
done
"$root/scripts/verify-samples.sh" "$repo"
echo "All checks passed for $version: ${tasks[*]} (integration runs: minimum, all), samples"
