#!/usr/bin/env python3
"""Prints the CHANGELOG.md entry of a version, for its GitHub release.

  scripts/release-notes.py 1.0.0-alpha02 > notes.md

Fails if the version is not the one gradle/libs.versions.toml builds (a2uiKmp), has no entry, or
the entry's file statistics ("How each upstream file is built") are not what scripts/kmp_stats.py
reports for the tree being released, so a release tag can only publish the version it names, with a
changelog that describes it.
"""
import pathlib, re, sys

import kmp_stats

import layout

ROOT = pathlib.Path(__file__).resolve().parent.parent


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    version = sys.argv[1]
    built = layout.catalog_version("a2uiKmp")
    if built != version:
        sys.exit(f"gradle/libs.versions.toml builds {built} (a2uiKmp), not {version}")
    changelog = (ROOT / "CHANGELOG.md").read_text()
    entry = re.search(rf"^## {re.escape(version)}\n(.*?)(?=^## |\Z)", changelog, flags=re.M | re.S)
    if not entry or not entry.group(1).strip():
        sys.exit(f"CHANGELOG.md has no entry for {version}")
    stats = re.search(r"^### How each upstream file is built\n(.*?)(?=^### |\Z)", entry.group(1), flags=re.M | re.S)
    current = kmp_stats.markdown().strip()
    if not stats or stats.group(1).strip() != current:
        sys.exit(f"The {version} entry's \"How each upstream file is built\" section must be the output of "
                 f"scripts/kmp_stats.py for this tree:\n\n{current}")
    print(entry.group(1).strip())


if __name__ == "__main__":
    main()
