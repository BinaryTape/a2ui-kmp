#!/usr/bin/env python3
"""Checks that source/a2ui/ and source/material3-a2ui/ are exactly the AndroidX commit recorded in source/UPSTREAM.

  scripts/check-upstream.py

Fetches only the A2UI paths of that commit from the mirror (shallow, into build/upstream-check) and
compares every file, so CI fails if an upstream file was edited, added or removed by hand. Changes to
the upstream copy only come from scripts/sync-upstream.py.
"""
import pathlib, shutil, subprocess, sys

import layout

ROOT = pathlib.Path(__file__).resolve().parent.parent
WORK = ROOT / "build" / "upstream-check"


def git(*args) -> None:
    subprocess.run(["git", *args], cwd=WORK, check=True, capture_output=True, text=True)


def read_upstream() -> dict:
    fields = {}
    for line in layout.UPSTREAM.read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            fields[key] = value
    return fields


def files(directory: pathlib.Path) -> set:
    return {p.relative_to(directory).as_posix() for p in directory.rglob("*") if p.is_file()}


def main() -> None:
    fields = read_upstream()
    mappings = [entry.split(":") for entry in fields["paths"].split()]
    shutil.rmtree(WORK, ignore_errors=True)
    WORK.mkdir(parents=True)
    git("init", "--quiet")
    git("remote", "add", "origin", fields["mirror"])
    git("sparse-checkout", "set", *(src for src, _ in mappings))
    git("fetch", "--quiet", "--depth=1", "--filter=blob:none", "origin", fields["commit"])
    git("checkout", "--quiet", "FETCH_HEAD")

    problems = []
    for src, dst in mappings:
        theirs, ours = WORK / src, ROOT / dst
        upstream, local = files(theirs), files(ours)
        problems += [f"missing: {dst}/{name}" for name in sorted(upstream - local)]
        problems += [f"not upstream: {dst}/{name}" for name in sorted(local - upstream)]
        problems += [
            f"edited: {dst}/{name}"
            for name in sorted(upstream & local)
            if (theirs / name).read_bytes() != (ours / name).read_bytes()
        ]
        print(f"{dst}/: {len(upstream)} files from {src} at {fields['commit'][:12]}")
    if problems:
        print("\n".join(problems), file=sys.stderr)
        sys.exit(f"{len(problems)} differences from UPSTREAM; use scripts/sync-upstream.py to change the upstream copy")
    print("Upstream copy matches UPSTREAM.")


if __name__ == "__main__":
    main()
