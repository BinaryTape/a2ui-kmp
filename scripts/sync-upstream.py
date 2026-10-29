#!/usr/bin/env python3
"""Syncs source/a2ui/ and source/material3-a2ui/ to an AndroidX commit and adds a CHANGELOG.md entry.

  scripts/sync-upstream.py <commit> --release "androidx.a2ui 1.0.0-alpha03 (2026-11-04)" --version 1.0.0-alpha03

1. Fetches only the A2UI paths of the commit from the AndroidX GitHub mirror (cached in build/).
2. Replaces source/a2ui/ and source/material3-a2ui/ with them, unchanged, and records the commit in source/UPSTREAM.
3. Regenerates the overlays (scripts/generate-overlays.py) and sets the versions in gradle/libs.versions.toml and in the sample app's catalog.
4. Adds a CHANGELOG.md entry: upstream files changed since the previous sync, the per-package file
   statistics (scripts/kmp_stats.py), and our commits since the previous sync.

Then build and test (scripts/verify.sh), fix what the new upstream breaks, and fill in the entry's
"Verified" section with what was run.
"""
import argparse, datetime, pathlib, re, shutil, subprocess, sys

import kmp_stats

import layout

ROOT = pathlib.Path(__file__).resolve().parent.parent
CACHE = ROOT / "build" / "androidx-upstream"


def git(*args, cwd=ROOT) -> str:
    return subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True, text=True).stdout


def read_upstream() -> dict:
    fields = {}
    for line in layout.UPSTREAM.read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            fields[key] = value
    return fields


def write_upstream(fields: dict, commit: str, release: str) -> None:
    text = layout.UPSTREAM.read_text()
    text = re.sub(r"^commit=.*$", f"commit={commit}", text, flags=re.M)
    text = re.sub(r"^release=.*$", f"release={release}", text, flags=re.M)
    layout.UPSTREAM.write_text(text)


def fetch(mirror: str, commit: str, paths: list) -> None:
    if not (CACHE / ".git").exists():
        CACHE.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["git", "clone", "--filter=blob:none", "--no-checkout", "--sparse", mirror, str(CACHE)], check=True)
    git("sparse-checkout", "set", *paths, cwd=CACHE)
    git("fetch", "--filter=blob:none", "origin", commit, cwd=CACHE)
    git("checkout", "--quiet", commit, cwd=CACHE)


def upstream_changes(old: str, new: str, mappings: list) -> list:
    """Upstream files added, modified or removed between two commits, by our directory."""
    lines = []
    for src, dst in mappings:
        diff = git("diff", "--name-status", old, new, "--", src, cwd=CACHE).splitlines()
        if not diff:
            continue
        lines.append(f"- `{dst}/`: {len(diff)} files")
        for entry in diff:
            status, *names = entry.split("\t")
            shown = " -> ".join(n.replace(src, dst, 1) for n in names)
            lines.append(f"  - {status[0]} `{shown}`")
    return lines or ["- None."]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("commit")
    parser.add_argument("--release", required=True, help='e.g. "androidx.a2ui 1.0.0-alpha03 (2026-11-04)"')
    parser.add_argument("--version", required=True, help="our artifact version, e.g. 1.0.0-alpha03")
    args = parser.parse_args()

    fields = read_upstream()
    mappings = [tuple(p.split(":")) for p in fields["paths"].split()]
    previous = fields["commit"]
    fetch(fields["mirror"], args.commit, [src for src, _ in mappings])
    commit = git("rev-parse", "HEAD", cwd=CACHE).strip()

    for src, dst in mappings:
        shutil.rmtree(ROOT / dst, ignore_errors=True)
        shutil.copytree(CACHE / src, ROOT / dst)
    write_upstream(fields, commit, args.release)
    subprocess.run([sys.executable, str(ROOT / "scripts/generate-overlays.py")], check=True, stdout=subprocess.DEVNULL)
    layout.set_catalog_version("a2uiUpstream", args.release.split()[1])
    layout.set_catalog_version("a2uiKmp", args.version)
    # The sample app uses the version being released, as an app would.
    layout.set_catalog_version("a2ui", args.version, ROOT / "samples" / "gradle" / "libs.versions.toml")

    # Collect port changes since the previous upstream sync.
    last_sync = git("log", "-1", "--format=%H", "-G", "^commit=", "--", "source/UPSTREAM", "UPSTREAM").strip()
    ours = git("log", "--reverse", "--format=- %s", f"{last_sync}..HEAD").strip() if last_sync else ""

    entry = "\n".join([
        f"## {args.version}",
        "",
        f"Upstream: {args.release}, commit `{commit[:12]}`. Synced {datetime.date.today().isoformat()}.",
        "",
        f"### Upstream changes since `{previous[:12]}`",
        "",
        *upstream_changes(previous, commit, mappings),
        "",
        "### Port changes",
        "",
        ours or "- None.",
        "",
        "### How each upstream file is built",
        "",
        kmp_stats.markdown(),
        "",
        "### Verified",
        "",
        "- TODO: fill in after scripts/verify.sh (targets, test counts, devices).",
        "",
    ])
    changelog = ROOT / "CHANGELOG.md"
    text = changelog.read_text() if changelog.exists() else "# Changelog\n\n"
    head, _, rest = text.partition("\n## ")
    changelog.write_text(head.rstrip() + "\n\n" + entry + ("\n## " + rest if rest else ""))
    print(f"Synced {commit[:12]}; review the diff, run scripts/verify.sh and complete CHANGELOG.md.")


if __name__ == "__main__":
    main()
