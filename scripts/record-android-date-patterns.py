#!/usr/bin/env python3
"""Records Android's MEDIUM date and SHORT time patterns as Kotlin, for compat's DateFormat.

  scripts/record-android-date-patterns.py

Run tests/integration's device tests on an Android emulator first (scripts/verify.sh does);
AndroidDatePatternsTest prints the patterns of every available locale to the log, which this reads
with `adb logcat -d`. Writes a2ui-compat/.../java/text/AndroidDatePatterns.kt. Re-record when the
Android version the tests compare with changes; the device test fails while the table is stale.
"""
import pathlib, subprocess, sys

import layout

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = layout.COMPAT / "src/commonMain/kotlin/ai/littlepress/a2ui/compat/java/text/AndroidDatePatterns.kt"


def kotlin_string(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$").replace("\t", "\\t").replace("\n", "\\n") + '"'


def main() -> None:
    log = subprocess.run(["adb", "logcat", "-d", "-v", "raw"], check=True, capture_output=True, text=True).stdout
    sdk = subprocess.run(["adb", "shell", "getprop", "ro.build.version.sdk"], check=True, capture_output=True, text=True).stdout.strip()
    patterns = {}
    for line in log.splitlines():
        if line.startswith("A2UI-DATE-PATTERN\t"):
            _, tag, maximized, date, time = line.split("\t")
            patterns.setdefault(tag, (date, time))
            patterns.setdefault(maximized, (date, time))
    if not patterns:
        sys.exit("No A2UI-DATE-PATTERN lines in the log: run the integration device tests first")
    records = [f"{tag}\t{date}\t{time}" for tag, (date, time) in sorted(patterns.items())]
    chunks = ["\n".join(records[i:i + 400]) for i in range(0, len(records), 400)]
    lines = [
        "package ai.littlepress.a2ui.compat.java.text",
        "",
        "// Recorded by scripts/record-android-date-patterns.py on an Android emulator (API "
        f"{sdk}) from",
        "// java.text.DateFormat: for each available locale (as tagged and with likely subtags added), its",
        "// MEDIUM date and SHORT time pattern, tab-separated, one locale per line. Do not edit.",
        "",
        "internal val ANDROID_DATE_PATTERNS: List<String> = listOf(",
    ]
    for chunk in chunks:
        lines.append("    " + " +\n        ".join(kotlin_string(r + "\n") for r in chunk.split("\n")) + ",")
    lines += [")", ""]
    OUT.write_text("\n".join(lines))
    print(f"{OUT.relative_to(ROOT)}: {len(records)} locales")


if __name__ == "__main__":
    main()
