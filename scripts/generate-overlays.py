#!/usr/bin/env python3
"""Generates the automatic overlay files for the KMP build.

For every upstream source file that imports a JDK class the compat module provides, writes a copy to
<module>/overlay-generated/<same relative path> with only these mechanical changes (modules: see
layout.py):
  1. `import java.<pkg>.<Class>` -> `import ai.littlepress.a2ui.compat.java.<pkg>.<Class>`
  2. `Locale.US` / `Locale.getDefault()` -> `Locales.US` / `Locales.getDefault()` (static members of a
     class that is java.util.Locale itself on the JVM; see a2ui-compat/.../Locale.kt), plus its import.
  3. `import kotlin.jvm.<Annotation>` added for @JvmName / @JvmField / @JvmOverloads / @JvmStatic used
     without an import: kotlin.jvm.* is imported by default on the JVM only.
  4. Android-only Compose names (`stringResource`, `LocalContext`) imported from our stand-ins, and
     `LocalLocale.current.platformLocale` (JVM only) replaced by the same locale as a compat Locale.
The build compiles a generated copy instead of its upstream file. Files under <module>/overlay-manual/
are hand-written and take precedence; <module>/overlay-manual.json records the upstream file each
one replaces, and this script fails when that upstream file has changed since (review the change,
update the hand-written file, then run with --record-manual).

For a module with Android string resources it also writes <module>/resources-generated: R.string
and every string's value per resource locale, read from the upstream res/values*/strings.xml.

Re-run after every upstream sync; the output is committed so each change is reviewable. Exits
non-zero if an upstream file imports a JDK class the compat module lacks.
"""
import hashlib, html, json, pathlib, re, shutil, sys
import xml.etree.ElementTree as ElementTree

import layout

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODULES = {  # KMP module -> upstream main source root
    "a2ui-model": "source/a2ui/a2ui-model/src/main/kotlin",
    "a2ui-engine": "source/a2ui/a2ui-engine/src/main/kotlin",
    "compose-runtime": "source/a2ui/compose/compose-runtime/src/main/kotlin",
    "compose-ui": "source/a2ui/compose/compose-ui/src/main/kotlin",
    "compose-ui-testing": "source/a2ui/compose/compose-ui-testing/src/main/kotlin",
    "material3-a2ui": "source/material3-a2ui/src/main/kotlin",
}
# KMP module -> (upstream res directory, package of its R class).
RESOURCES = {
    "material3-a2ui": ("source/material3-a2ui/src/main/res", "androidx.compose.material3.a2ui"),
}
# Imported classes and functions that do not exist on every platform -> our equivalent in
# a2ui-compat, at ai.littlepress.a2ui.compat.<same package>.<same name>.
COMPAT = {
    "java.util": {"Locale", "Date", "TimeZone", "Currency", "Calendar"},
    "java.util.concurrent": {"ConcurrentHashMap", "CopyOnWriteArrayList"},
    "java.util.concurrent.atomic": {"AtomicInteger"},
    "java.util.concurrent.locks": {"ReentrantLock"},
    "java.text": {"SimpleDateFormat", "NumberFormat", "ParseException", "DateFormat"},
    "java.io": {"StringReader"},
    "android.util": {"JsonReader", "JsonToken"},
    "android.content": {"Context", "Intent"},
    "android.text.format.DateFormat": {"is24HourFormat"},
    "android.net": {"Uri"},
    "android.icu.text": {"MessageFormat", "MessagePattern"},
    "androidx.core.util": {"PatternsCompat"},
    "kotlin.concurrent": {"withLock"},
}
# Compose names that only Android has -> our stand-in: in a2ui-compat, or, for stringResource (which
# reads the module's resources-generated strings), in the module itself.
COMPOSE = {
    "androidx.compose.ui.platform.LocalContext": "ai.littlepress.a2ui.compat.androidx.compose.ui.platform.LocalContext",
    "androidx.compose.ui.res.stringResource": "ai.littlepress.a2ui.compat.androidx.compose.ui.res.stringResource",
}
COMPOSE_IMPORT = re.compile(r"^import (" + "|".join(re.escape(name) for name in COMPOSE) + r")$", re.M)
PLATFORM_LOCALE = re.compile(r"\bLocalLocale\.current\.platformLocale\b")
# JDK classes the Kotlin standard library has in common code (on the JVM, as type aliases to them).
STDLIB = {"java.lang.StringBuilder": "kotlin.text.StringBuilder"}
IMPORT = re.compile(r"^import ((?:java|android|androidx\.core|kotlin\.concurrent)(?:\.\w+)*)\.(\w+)$", re.M)
# Names the JVM has without an import (java.lang.*, kotlin.jvm.*) or as a JDK member. The overlay
# imports our equivalent explicitly, which takes precedence over default imports.
IMPLICIT = [
    (re.compile(r"\bjavaClass\b"), "ai.littlepress.a2ui.compat.kotlin.jvm.javaClass"),
    (re.compile(r"\bjavaClass\.simpleName\b"), "ai.littlepress.a2ui.compat.kotlin.jvm.simpleName"),
    (re.compile(r"\bSystem\."), "ai.littlepress.a2ui.compat.java.lang.System"),
    (re.compile(r"\bCharacter\."), "ai.littlepress.a2ui.compat.java.lang.Character"),
    (re.compile(r"\.putIfAbsent\("), "ai.littlepress.a2ui.compat.kotlin.collections.putIfAbsent"),
]
CLASS_SIMPLE_NAME = re.compile(r"::class\.java\.simpleName\b")
JVM_ANNOTATION = re.compile(r"@(?:file:|get:|set:)?(JvmName|JvmField|JvmOverloads|JvmStatic)\b")


def needs_jvm_imports(text: str) -> set:
    used = set(JVM_ANNOTATION.findall(text))
    return {a for a in used if not re.search(rf"^import kotlin\.jvm\.{a}$", text, re.M)}


def add_jvm_imports(text: str) -> str:
    missing = needs_jvm_imports(text)
    if not missing:
        return text
    lines = "".join(f"import kotlin.jvm.{a}\n" for a in sorted(missing))
    # After the package line, before any other import.
    return re.sub(r"^(package .*\n)\n?", lambda m: m.group(1) + "\n" + lines, text, count=1, flags=re.M)


def add_locales_import(text: str) -> str:
    locale = "import ai.littlepress.a2ui.compat.java.util.Locale\n"
    locales = "import ai.littlepress.a2ui.compat.java.util.Locales\n"
    if locales in text:
        return text
    if locale in text:
        return text.replace(locale, locale + locales, 1)
    return re.sub(r"^(package .*\n)", lambda m: m.group(1) + "\n" + locales, text, count=1, flags=re.M)


def rewrite(text: str, path: pathlib.Path) -> tuple[str, list[str]]:
    """Returns the overlay text and the names of the rules that changed something."""
    missing = []
    rules = []

    def remap(m: re.Match) -> str:
        pkg, cls = m.group(1), m.group(2)
        if f"{pkg}.{cls}" in STDLIB:
            return f"import {STDLIB[pkg + '.' + cls]}"
        if cls not in COMPAT.get(pkg, set()):
            missing.append(f"{pkg}.{cls}")
            return m.group(0)
        return f"import ai.littlepress.a2ui.compat.{pkg}.{cls}"

    out = IMPORT.sub(remap, text)
    if missing:
        sys.exit(f"{path}: no compat class for {', '.join(missing)}")
    if out != text:
        rules.append("platform-imports")
    composed = COMPOSE_IMPORT.sub(lambda m: f"import {COMPOSE[m.group(1)]}", out)
    if composed != out:
        rules.append("compose-imports")
        out = composed
    if re.search(r"\bLocale\.(US|getDefault\(\))", out):
        rules.append("locale-constants")
        out = add_locales_import(re.sub(r"\bLocale\.(US|getDefault\(\))", r"Locales.\1", out))
    if PLATFORM_LOCALE.search(out):
        # Only the JVM has Locale.platformLocale. The compat Locale for the same language tag is the
        # same java.util.Locale on desktop.
        rules.append("platform-locale")
        out = add_locales_import(PLATFORM_LOCALE.sub("Locales.forLanguageTag(LocalLocale.current.toLanguageTag())", out))
    if CLASS_SIMPLE_NAME.search(out):
        # Only used in error messages; KClass.simpleName gives the same name.
        rules.append("class-java-simple-name")
        out = CLASS_SIMPLE_NAME.sub("::class.simpleName", out)
    implicit = [imp for pattern, imp in IMPLICIT if pattern.search(out) and f"import {imp}\n" not in out]
    if implicit:
        rules.append("jvm-default-imports")
        lines = "".join(f"import {imp}\n" for imp in implicit)
        out = re.sub(r"^(package .*\n)\n?", lambda m: m.group(1) + "\n" + lines, out, count=1, flags=re.M)
    if needs_jvm_imports(out):
        rules.append("kotlin-jvm-imports")
        out = add_jvm_imports(out)
    return out, rules


def check_manual(module: str, src: pathlib.Path, manual: pathlib.Path, record: bool) -> list:
    """Compares the upstream files that hand-written overlays replace with the recorded hashes."""
    path = layout.MODULES[module] / "overlay-manual.json"
    recorded = json.loads(path.read_text()) if path.exists() else {}
    current = {
        f.relative_to(manual).as_posix(): hashlib.sha256((src / f.relative_to(manual)).read_bytes()).hexdigest()
        for f in sorted(manual.rglob("*.kt")) if (src / f.relative_to(manual)).exists()
    }
    if record:
        if current:
            path.write_text(json.dumps({k: {"upstreamSha256": v} for k, v in current.items()}, indent=2) + "\n")
        elif path.exists():
            path.unlink()
        return []
    problems = [f"{module}/{rel}: upstream file changed since its hand-written overlay was reviewed"
                for rel, sha in current.items() if recorded.get(rel, {}).get("upstreamSha256") != sha]
    problems += [f"{module}/{rel}: recorded, but no hand-written overlay or upstream file" for rel in recorded if rel not in current]
    return problems


def android_string(element: ElementTree.Element) -> str:
    """A <string>'s value as aapt2 compiles it: escapes resolved, double quotes removed and keeping the
    whitespace between them, other whitespace runs as one space, none at the start or end."""
    raw = "".join(element.itertext())
    out, quoted, space, i = [], False, False, 0

    def append(text: str) -> None:
        nonlocal space
        if space and out:
            out.append(" ")
        space = False
        out.append(text)

    while i < len(raw):
        c = raw[i]
        if c == "\\" and i + 1 < len(raw):
            nxt = raw[i + 1]
            if nxt == "u" and re.fullmatch(r"[0-9a-fA-F]{4}", raw[i + 2:i + 6]):
                append(chr(int(raw[i + 2:i + 6], 16)))
                i += 6
                continue
            append({"n": "\n", "t": "\t"}.get(nxt, nxt))
            i += 2
            continue
        if c == '"':
            quoted = not quoted
        elif c.isspace() and not quoted:
            space = True
        else:
            append(c)
        i += 1
    return "".join(out)


def qualifier_tag(directory: str) -> str:
    """values-zh-rTW -> zh-TW, values-b+sr+Latn -> sr-Latn, values -> "" (the default resources)."""
    qualifier = directory.removeprefix("values").removeprefix("-")
    if qualifier.startswith("b+"):
        return "-".join(qualifier[2:].split("+"))
    return qualifier.replace("-r", "-")


def kotlin_string(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$").replace("\n", "\\n") + '"'


def generate_resources(module: str, res: pathlib.Path, package: str) -> None:
    strings = {}  # name -> {tag: value}
    for directory in sorted(res.iterdir(), key=lambda d: d.name):
        xml = directory / "strings.xml"
        if not directory.name.startswith("values") or not xml.exists():
            continue
        tag = qualifier_tag(directory.name)
        for element in ElementTree.parse(xml).getroot().iter("string"):
            strings.setdefault(element.get("name"), {})[tag] = android_string(element)
    out_root = layout.MODULES[module] / "resources-generated"
    shutil.rmtree(out_root, ignore_errors=True)
    names = sorted(strings)
    lines = [
        f"package {package}",
        "",
        f"// Generated by scripts/generate-overlays.py from {res.relative_to(ROOT)}/values*/strings.xml.",
        "// Do not edit.",
        "",
        "/**",
        " * The string resources for desktop and iOS. Ids start at 1, as 0 means \"no resource\" on Android;",
        " * id n is STRING_RESOURCES[n - 1].",
        " */",
        "@Suppress(\"ClassName\", \"ObjectPropertyName\")",
        "internal object R {",
        "    object string {",
    ]
    lines += [f"        const val {name}: Int = {i}" for i, name in enumerate(names, start=1)]
    lines += [
        "    }",
        "}",
        "",
        "/**",
        " * For each string in [R.string], its value in each resource locale that has it, by language tag",
        " * (\"\" for values/, \"zh-TW\" for values-zh-rTW, \"sr-Latn\" for values-b+sr+Latn).",
        " */",
        "internal val STRING_RESOURCES: List<Map<String, String>> = listOf(",
    ]
    for name in names:
        lines.append(f"    mapOf( // {name}")
        lines += [f"        {kotlin_string(tag)} to {kotlin_string(value)}," for tag, value in strings[name].items()]
        lines.append("    ),")
    lines += [")", ""]
    target = out_root / pathlib.Path(*package.split(".")) / "R.kt"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(lines))
    print(f"generated {module} resources: {len(names)} strings, {len({t for v in strings.values() for t in v})} locales")


def generate_integration_resource_locales(res: pathlib.Path) -> None:
    """tests/integration gets one string per resource directory of [res], its locale, so that on an
    Android device it can ask Android which directory serves a locale (ResourceLocaleTest)."""
    out_res = layout.INTEGRATION / "src/androidMain/res"
    shutil.rmtree(out_res, ignore_errors=True)
    tags = []
    for directory in sorted(d for d in res.iterdir() if d.name.startswith("values") and (d / "strings.xml").exists()):
        tag = qualifier_tag(directory.name)
        tags.append(tag)
        (out_res / directory.name).mkdir(parents=True)
        (out_res / directory.name / "a2ui_resource_locale.xml").write_text(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!-- Generated by scripts/generate-overlays.py. -->\n"
            f"<resources>\n    <string name=\"a2ui_resource_locale\">{tag or '(default)'}</string>\n</resources>\n")
    (layout.INTEGRATION / "src/commonTest/kotlin/ai/littlepress/a2ui/integration/Material3ResourceLocales.kt").write_text("\n".join([
        "package ai.littlepress.a2ui.integration",
        "",
        f"// Generated by scripts/generate-overlays.py from {res.relative_to(ROOT)}. Do not edit.",
        "",
        "/** The locales of material3-a2ui's resource directories as language tags; \"\" is values/. */",
        "internal val RESOURCE_LOCALES: List<String> = listOf(",
        *[f"    {kotlin_string(tag)}," for tag in tags],
        ")",
        "",
    ]))


def main() -> None:
    record = "--record-manual" in sys.argv[1:]
    problems = []
    for module, (res, package) in RESOURCES.items():
        generate_resources(module, ROOT / res, package)
    generate_integration_resource_locales(ROOT / RESOURCES["material3-a2ui"][0])
    for module, upstream in MODULES.items():
        src = ROOT / upstream
        manual = layout.MODULES[module] / "overlay-manual"
        problems += check_manual(module, src, manual, record)
        out_root = layout.MODULES[module] / "overlay-generated"
        shutil.rmtree(out_root, ignore_errors=True)
        manifest = {}
        for f in sorted(src.rglob("*.kt")):
            rel = f.relative_to(src)
            text = f.read_text()
            if (manual / rel).exists():
                continue
            out, rules = rewrite(text, f)
            if not rules:
                continue
            target = out_root / rel
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(out)
            manifest[rel.as_posix()] = {
                "upstreamSha256": hashlib.sha256(text.encode()).hexdigest(),
                "rules": rules,
            }
            print(f"generated {module}/{rel}: {', '.join(rules)}")
        # Record the rewrite rules and source hashes for each generated overlay.
        (layout.MODULES[module] / "overlay-generated.json").write_text(json.dumps(manifest, indent=2) + "\n")
    if problems:
        sys.exit("\n".join(problems))


if __name__ == "__main__":
    main()
