#!/usr/bin/env python3
"""Lists the releases this repository should follow and does not yet, one JSON object per line
({"title": ..., "body": ...}); .github/workflows/release-watch.yml opens an issue for each.

  - a newer androidx.a2ui or material3-a2ui release on Google's Maven repository than a2uiUpstream
    (synced with scripts/sync-upstream.py);
  - a stable Compose Multiplatform release for material3-a2ui, while composeMultiplatformMaterial3
    is a pre-release (README, "Requirements": it moves to the stable release once there is one).
    Read from the project's GitHub releases: Maven Central answers GitHub-hosted runners with 404 or
    429 now and then, and the GitHub API does not.

The titles name the version, so each release gets one issue.
"""
import http.client, json, os, re, sys, time, urllib.error, urllib.request

import layout

GOOGLE = "https://dl.google.com/android/maven2"
# The official artifacts the two builds follow; every package of target/a2ui-kmp has a2ui-model's
# version.
UPSTREAM = {
    "androidx.a2ui": f"{GOOGLE}/androidx/a2ui/a2ui-model",
    "material3-a2ui": f"{GOOGLE}/androidx/compose/material3/material3-a2ui",
}
# Newest first; pre-releases are marked as such.
COMPOSE_MULTIPLATFORM_RELEASES = (
    "https://api.github.com/repos/JetBrains/compose-multiplatform/releases?per_page=100"
)
ATTEMPTS = 4


def fetch(url: str, headers: dict[str, str] | None = None) -> bytes:
    """Tries again, after 15, 30 and 60 seconds, when the server is busy (429, 5xx), does not
    answer or breaks off; any other error, such as a 404 for an artifact that moved, fails at
    once."""
    for attempt in range(1, ATTEMPTS + 1):
        try:
            request = urllib.request.Request(url, headers=headers or {})
            with urllib.request.urlopen(request, timeout=30) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            if (error.code != 429 and error.code < 500) or attempt == ATTEMPTS:
                raise
            reason = f"HTTP {error.code}"
        # No answer, or the connection broke while the answer was being read.
        except (OSError, http.client.HTTPException) as error:
            if attempt == ATTEMPTS:
                raise
            reason = str(error)
        delay = 15 * 2 ** (attempt - 1)
        print(f"{url}: {reason}; trying again in {delay} s", file=sys.stderr)
        time.sleep(delay)
    raise AssertionError("unreachable")


def versions(artifact: str) -> list[str]:
    """In the order they were published, as maven-metadata.xml lists them."""
    metadata = fetch(f"{artifact}/maven-metadata.xml").decode()
    return re.findall(r"<version>([^<]+)</version>", metadata)


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
    headers = {"Accept": "application/vnd.github+json"}
    if token := os.environ.get("GH_TOKEN"):
        headers["Authorization"] = f"Bearer {token}"
    releases = json.loads(fetch(COMPOSE_MULTIPLATFORM_RELEASES, headers))
    released = [r["tag_name"].removeprefix("v") for r in releases if not r["prerelease"]]
    stable = [v for v in released if (s := stable_release(v)) and s[:2] >= line]
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
