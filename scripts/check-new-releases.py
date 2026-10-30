#!/usr/bin/env python3
"""Lists the releases this repository should follow and does not yet, one JSON object per line
({"title": ..., "body": ...}); .github/workflows/release-watch.yml opens an issue for each.

  - a newer androidx.a2ui or material3-a2ui release on Google's Maven repository than a2uiUpstream
    (synced with scripts/sync-upstream.py);
  - a stable Compose Multiplatform release for material3-a2ui, while composeMultiplatformMaterial3
    is a pre-release (README, "Requirements": it moves to the stable release once there is one).

The titles name the version, so each release gets one issue.
"""
import json, re, urllib.request

import layout

GOOGLE = "https://dl.google.com/android/maven2"
CENTRAL = "https://repo1.maven.org/maven2"
# The official artifacts the two builds follow; every package of target/a2ui-kmp has a2ui-model's
# version.
UPSTREAM = {
    "androidx.a2ui": f"{GOOGLE}/androidx/a2ui/a2ui-model",
    "material3-a2ui": f"{GOOGLE}/androidx/compose/material3/material3-a2ui",
}
COMPOSE_MULTIPLATFORM = f"{CENTRAL}/org/jetbrains/compose/ui/ui"


def versions(artifact: str) -> list[str]:
    """In the order they were published, as maven-metadata.xml lists them."""
    with urllib.request.urlopen(f"{artifact}/maven-metadata.xml", timeout=30) as response:
        return re.findall(r"<version>([^<]+)</version>", response.read().decode())


def stable_release(version: str) -> tuple[int, ...] | None:
    return tuple(map(int, version.split("."))) if re.fullmatch(r"\d+(\.\d+)*", version) else None


def upstream_releases() -> list[dict]:
    current = layout.catalog_version("a2uiUpstream")
    newer: dict[str, list[str]] = {}
    for name, artifact in UPSTREAM.items():
        published = versions(artifact)
        if current not in published:
            raise SystemExit(f"{artifact} has no version {current} (a2uiUpstream)")
        for version in published[published.index(current) + 1:]:
            newer.setdefault(version, []).append(name)
    return [{
        "title": f"Sync upstream A2UI {version}",
        "body": (
            f"Google released {' and '.join(names)} {version}; this repository is built from "
            f"{current} (`a2uiUpstream`).\n\n"
            "Release notes, with the AndroidX commit of the release: "
            "https://developer.android.com/jetpack/androidx/releases/a2ui\n\n"
            "```sh\n"
            f'scripts/sync-upstream.py <androidx commit> --release "androidx.a2ui {version} (<date>)" --version {version}\n'
            "scripts/verify.sh\n"
            "```"
        ),
    } for version, names in newer.items()]


def compose_multiplatform_release() -> list[dict]:
    current = layout.catalog_version("composeMultiplatformMaterial3")
    if stable_release(current):
        return []
    line = stable_release(current.split("-")[0])[:2]
    stable = [v for v in versions(COMPOSE_MULTIPLATFORM) if (s := stable_release(v)) and s[:2] >= line]
    if not stable:
        return []
    version = max(stable, key=stable_release)
    return [{
        "title": f"Build material3-a2ui with Compose Multiplatform {version}",
        "body": (
            f"Compose Multiplatform {version} is released; material3-a2ui is built with {current} "
            "(`composeMultiplatformMaterial3` in gradle/libs.versions.toml). Move it to the stable "
            "release, with the Kotlin it requires, and update README's \"Requirements\".\n\n"
            "https://github.com/JetBrains/compose-multiplatform/releases"
        ),
    }]


def main() -> None:
    for release in upstream_releases() + compose_multiplatform_release():
        print(json.dumps(release))


if __name__ == "__main__":
    main()
