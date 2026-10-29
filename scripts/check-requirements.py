#!/usr/bin/env python3
"""What the published artifacts require of a consumer, as README's "Requirements" table.

A consumer gets at least the versions our artifacts require: Gradle moves its Kotlin standard
library and Compose up to them without a warning, and a Kotlin compiler cannot read iOS klibs whose
ABI version is newer than its own. This reads, from build/repo (scripts/verify.sh
publishes there first):
  - the Kotlin that built each iOS artifact, and its klib ABI version: the oldest Kotlin compiler
    that can read it (on the JVM, Kotlin reads class files one language version ahead, so the JVM
    limit is not this; tests/integration is what shows a version works)
  - the versions of kotlin-stdlib and Compose that the desktop and iOS variants require: Gradle
    raises a consumer's to these (the stdlib is not the compiler: Gradle does not change that)
  - the Kotlin tests/integration compiles each package with
  - the official artifact the Android variants resolve to
and fails unless the table between the markers in README.md is exactly the one it builds, so a
version that rises is a reviewed change to README rather than a surprise for consumers.

  scripts/check-requirements.py           check
  scripts/check-requirements.py --write   rewrite README's table from the artifacts
"""
import json, pathlib, re, sys, zipfile

import layout

ROOT = pathlib.Path(__file__).resolve().parent.parent
REPO = layout.LOCAL_REPO / "ai" / "littlepress" / "a2ui"
README = ROOT / "README.md"
START, END = "<!-- requirements:start (scripts/check-requirements.py) -->", "<!-- requirements:end -->"
PACKAGES = ["a2ui-model", "a2ui-engine", "compose-runtime", "compose-ui", "compose-ui-testing", "material3-a2ui"]
IOS = ["iosarm64", "iossimulatorarm64"]


def version() -> str:
    return layout.catalog_version("a2uiKmp")


def module(name: str, v: str) -> dict:
    path = REPO / name / v / f"{name}-{v}.module"
    if not path.exists():
        sys.exit(f"{path.relative_to(ROOT)} is missing: run scripts/verify.sh (or publish both builds) first")
    return json.loads(path.read_text())


def requires(name: str, v: str) -> dict:
    """(group, module) -> the highest version any variant of this platform artifact requires."""
    out = {}
    for variant in module(name, v)["variants"]:
        for dep in variant.get("dependencies", []):
            required = dep.get("version", {}).get("requires")
            if required:
                key = (dep["group"], dep["module"])
                out[key] = max(out.get(key, required), required, key=version_key)
    return out


def version_key(v: str):
    # 1.13.0-beta01 < 1.13.0: a pre-release sorts before its release.
    main, _, pre = v.partition("-")
    return [int(x) for x in main.split(".")], (pre == "", pre)


def klib_manifest(name: str, v: str) -> dict:
    directory = REPO / name / v
    klib = next(directory.glob("*.klib"))
    with zipfile.ZipFile(klib) as z:
        manifest = z.read("default/manifest").decode()
    return dict(line.split("=", 1) for line in manifest.splitlines() if "=" in line)


def integration_kotlin(package: str) -> str:
    """The Kotlin tests/integration compiles this package with (settings.gradle.kts: minimum or all)."""
    if f'"ai.littlepress.a2ui:{package}:' not in (layout.INTEGRATION / "build.gradle.kts").read_text():
        return "-"
    key = "kotlinMaterial3" if package == "material3-a2ui" else "kotlin"
    kotlin = layout.catalog_version(key)
    return f"Kotlin {kotlin}: desktop, Android host and device, iOS simulator"


def compose(deps: dict) -> str:
    names = []
    for (group, mod), required in sorted(deps.items()):
        if group.startswith("org.jetbrains.compose."):
            names.append(f"CMP `{mod}` {required}")
        elif group.startswith("androidx.compose."):
            names.append(f"`{group}:{mod}` {required}")
    return ", ".join(names) or "-"


def table() -> str:
    v = version()
    lines = [
        "| Package | Built with Kotlin | iOS: Kotlin that can read it (klib ABI) | kotlin-stdlib required | "
        "Compose required (desktop, iOS) | Integration test (tests/integration) | Android |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for package in PACKAGES:
        manifests = [klib_manifest(f"{package}-{target}", v) for target in IOS]
        abis = {m["abi_version"] for m in manifests}
        compilers = {m["compiler_version"] for m in manifests}
        if len(abis) != 1 or len(compilers) != 1:
            sys.exit(f"{package}: the iOS klibs disagree: ABI {sorted(abis)}, compiler {sorted(compilers)}")
        abi = ".".join(abis.pop().split(".")[:2])
        deps = {}
        for platform in ["desktop", *IOS]:
            for key, required in requires(f"{package}-{platform}", v).items():
                deps[key] = max(deps.get(key, required), required, key=version_key)
        stdlib = deps.get(("org.jetbrains.kotlin", "kotlin-stdlib"), "-")
        android = sorted(
            f"`{dep['group']}:{dep['module']}:{dep['version']['requires']}`"
            for variant in module(package, v)["variants"]
            if variant.get("attributes", {}).get("org.jetbrains.kotlin.platform.type") == "androidJvm"
            and variant["attributes"].get("org.gradle.usage") == "java-runtime"
            for dep in variant.get("dependencies", [])
            if dep["module"] != "a2ui-compat"
        )
        lines.append(
            f"| `{package}` | {compilers.pop()} | {abi} or later | {stdlib} | {compose(deps)} | "
            f"{integration_kotlin(package)} | {', '.join(android) or '-'} |"
        )
    return "\n".join(lines)


def main() -> None:
    current = table()
    text = README.read_text()
    match = re.search(re.escape(START) + r"\n(.*?)" + re.escape(END), text, flags=re.S)
    if not match:
        sys.exit(f"README.md has no {START} ... {END} section")
    if "--write" in sys.argv[1:]:
        README.write_text(text[: match.start(1)] + current + "\n" + text[match.end(1):])
        print(current)
        return
    if match.group(1).strip() != current:
        sys.exit("README.md's requirements table is not what the published artifacts require. If the change "
                 "is intended, run scripts/check-requirements.py --write and review it:\n\n" + current)
    print(current)


if __name__ == "__main__":
    main()
