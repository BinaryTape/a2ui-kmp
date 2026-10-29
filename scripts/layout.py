"""Where things are in this repository, for the scripts."""
import pathlib, re

ROOT = pathlib.Path(__file__).resolve().parent.parent
# AndroidX's sources, exactly as synced, and the commit they come from.
SOURCE = ROOT / "source"
UPSTREAM = SOURCE / "UPSTREAM"
# Each published package's Gradle module (its build file, overlay-generated/, overlay-manual/, src/).
TARGET = ROOT / "target"
MODULES = {
    "a2ui-compat": TARGET / "a2ui-kmp" / "a2ui-compat",
    "a2ui-model": TARGET / "a2ui-kmp" / "a2ui-model",
    "a2ui-engine": TARGET / "a2ui-kmp" / "a2ui-engine",
    "compose-runtime": TARGET / "a2ui-kmp" / "compose" / "compose-runtime",
    "compose-ui": TARGET / "a2ui-kmp" / "compose" / "compose-ui",
    "compose-ui-testing": TARGET / "a2ui-kmp" / "compose" / "compose-ui-testing",
    "material3-a2ui": TARGET / "material3-a2ui-kmp",
}
COMPAT = MODULES["a2ui-compat"]
# The one version catalog of both builds, build-logic and tests/integration; it also has this
# release's versions (a2uiKmp, a2uiUpstream).
CATALOG = ROOT / "gradle" / "libs.versions.toml"


def catalog_version(key: str, catalog: pathlib.Path = CATALOG) -> str:
    match = re.search(rf'^{re.escape(key)} = "(.*)"$', catalog.read_text(), flags=re.M)
    if not match:
        raise SystemExit(f"{catalog.relative_to(ROOT)} has no version {key}")
    return match.group(1)


def set_catalog_version(key: str, value: str, catalog: pathlib.Path = CATALOG) -> None:
    text = catalog.read_text()
    new = re.sub(rf'^{re.escape(key)} = ".*"$', f'{key} = "{value}"', text, count=1, flags=re.M)
    if new == text and f'{key} = "{value}"' not in text:
        raise SystemExit(f"{catalog.relative_to(ROOT)} has no version {key}")
    catalog.write_text(new)
# Where both builds publish for local use.
LOCAL_REPO = ROOT / "build" / "repo"
INTEGRATION = ROOT / "tests" / "integration"
