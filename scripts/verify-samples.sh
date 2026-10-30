#!/usr/bin/env bash
# Builds and checks the sample app in samples/ the way a customer's app would use a2ui-kmp: from a
# Maven repository (by default build/repo, where verify.sh publishes), never from our sources.
#   desktop  compile, and the sample's UI test (it clicks through the screen)
#   iOS      link the framework, build the Xcode app for the simulator, install and launch it, and
#            check that it is still running after a few seconds (screenshot in
#            samples/build/ios-launch.png); then its UI test presses Book on the Form tab
#            (screenshot in samples/build/ios-form.png)
#
#   scripts/verify-samples.sh [maven repository]
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
repo="$(cd "${1:-$root/build/repo}" && pwd)"
# The samples' settings and their Xcode build phase read the repository from this property.
export ORG_GRADLE_PROJECT_a2uiRepo="$repo"

# The sample names the a2ui-kmp version a customer would use; it must be the version being built,
# so the sample (and the README that quotes it) never falls behind.
version="$(sed -n 's/^a2uiKmp = "\(.*\)"$/\1/p' "$root/gradle/libs.versions.toml")"
for catalog in "$root/samples/gradle/libs.versions.toml"; do
    sample_version="$(sed -n 's/^a2ui = "\(.*\)"$/\1/p' "$catalog")"
    if [ "$sample_version" != "$version" ]; then
        echo "${catalog#"$root"/}: a2ui = \"$sample_version\", but gradle/libs.versions.toml builds $version (a2uiKmp); set a2ui = \"$version\"" >&2
        exit 1
    fi
done

# A standard iPhone simulator (e.g. "iPhone 17 Pro") on the newest iOS runtime the selected Xcode
# can build for (not newer than its iOS Simulator SDK: a machine can have runtimes of other Xcode
# versions, which xcodebuild does not offer as destinations), or the simulator A2UI_SIMULATOR
# names (UDID).
sdk="$(xcrun --sdk iphonesimulator --show-sdk-version)"
echo "$(xcodebuild -version | head -1), iOS Simulator SDK $sdk"
device="${A2UI_SIMULATOR:-$(xcrun simctl list devices available -j | python3 -c '
import json, re, sys
sdk = [int(p) for p in sys.argv[1].split(".")]
runtimes = json.load(sys.stdin)["devices"]
version = lambda r: [int(p) for p in r.rsplit("iOS-", 1)[1].split("-")]
ios = sorted((r for r in runtimes if "iOS" in r), key=version, reverse=True)
for runtime in (r for r in ios if version(r) <= sdk):
    phones = [d for d in runtimes[runtime] if re.fullmatch(r"iPhone \d+( Pro( Max)?| Plus)?", d["name"])]
    if phones:
        phone = phones[0]
        print("Simulator:", phone["name"], runtime.rsplit(".", 1)[1], file=sys.stderr)
        print(phone["udid"])
        break
else:
    sys.exit("No iPhone simulator on an iOS runtime up to " + sys.argv[1] + " (the SDK of the selected Xcode); "
             "available runtimes: " + (", ".join(r.rsplit(".", 1)[1] for r in ios) or "none"))
' "$sdk")}"
booted_here=false
if ! xcrun simctl list devices booted | grep -q "$device"; then
    xcrun simctl boot "$device"
    booted_here=true
fi
trap '$booted_here && xcrun simctl shutdown "$device" >/dev/null 2>&1 || true' EXIT
xcrun simctl bootstatus "$device" -b >/dev/null

samples=()
for sample in "$root/samples"; do
    name="$(basename "$sample")"
    samples+=("$name")
    echo "Sample $name: desktop and iOS framework"
    (cd "$sample" && ./gradlew --quiet --refresh-dependencies \
        :composeApp:compileKotlinDesktop :composeApp:desktopTest :composeApp:linkDebugFrameworkIosSimulatorArm64)

    echo "Sample $name: Xcode app on the simulator"
    derived="$sample/build/xcode"
    xcodebuild -quiet -project "$sample/iosApp/iosApp.xcodeproj" -scheme iosApp -configuration Debug \
        -destination "id=$device" -derivedDataPath "$derived" CODE_SIGNING_ALLOWED=NO build-for-testing
    app="$derived/Build/Products/Debug-iphonesimulator/iosApp.app"
    bundle="$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$app/Info.plist")"
    xcrun simctl install "$device" "$app"
    pid="$(xcrun simctl launch --terminate-running-process "$device" "$bundle" | awk '{print $NF}')"
    # Simulator apps are host processes: a crash on start shows up as a missing process.
    for _ in 1 2 3 4 5 6 7 8; do
        sleep 1
        ps -p "$pid" >/dev/null || { echo "Sample $name: the iOS app exited after launch" >&2; exit 1; }
    done
    xcrun simctl io "$device" screenshot "$sample/build/ios-launch.png" >/dev/null 2>&1
    xcrun simctl terminate "$device" "$bundle"
    # The app must have drawn its screen: a blank (one-colour) screenshot fails.
    sips -s format bmp "$sample/build/ios-launch.png" --out "$sample/build/ios-launch.bmp" >/dev/null
    colours="$(python3 - "$sample/build/ios-launch.bmp" <<'PY'
import struct, sys
data = open(sys.argv[1], "rb").read()
offset, bits = struct.unpack_from("<I", data, 10)[0], struct.unpack_from("<H", data, 28)[0]
size = bits // 8
print(len({data[i:i + size] for i in range(offset, len(data) - size, size * 97)}))
PY
)"
    [ "$colours" -gt 8 ] || { echo "Sample $name: the iOS screen is blank ($colours colours)" >&2; exit 1; }

    echo "Sample $name: iOS UI test (presses Book on the Form tab)"
    TEST_RUNNER_A2UI_SCREENSHOT_DIR="$sample/build" xcodebuild -quiet -project "$sample/iosApp/iosApp.xcodeproj" \
        -scheme iosApp -configuration Debug -destination "id=$device" -derivedDataPath "$derived" \
        CODE_SIGNING_ALLOWED=NO test-without-building
done
echo "All samples passed on desktop and iOS: ${samples[*]}"
