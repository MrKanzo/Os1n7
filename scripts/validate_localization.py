#!/usr/bin/env python3
"""Offline static guard for the app's selected-language localization architecture."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
KOTLIN = ROOT / "app/src/main/java/com/mrkanzo/os1n7"
LOCALES = [
    "values", "values-en", "values-pl", "values-es",
    "values-pt-rBR", "values-de", "values-fr",
    "values-in", "values-tr", "values-ar",
]
ADDED = {
    "settings_translation_title", "settings_translation_detail",
    "settings_translation_ready", "settings_translation_loading",
    "settings_translation_failed", "settings_translation_retry",
    "settings_translation_credit",
}


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def parse_names(folder):
    root = ET.parse(RES / folder / "strings.xml").getroot()
    values = [item.attrib["name"] for item in root if item.tag == "string"]
    check(len(values) == len(set(values)), "Duplicate string key in " + folder)
    check(all((item.text or "").strip() for item in root if item.tag == "string"),
          "Empty localized text in " + folder)
    return set(values)


def main():
    reference = parse_names("values")
    check(ADDED <= reference, "Missing translation-model status resources")
    for folder in LOCALES[1:]:
        other = parse_names(folder)
        check(other == reference, f"{folder}: missing {reference-other}; extra {other-reference}")
    check(not (RES / "values-ru").exists(), "Russian localization must not be added")
    check(len(LOCALES) == 10, "Expected ten supported locales")
    base = (KOTLIN / "Os1n7Activity.kt").read_text(encoding="utf-8")
    check("Os1n7Translations.Session(this)" in base,
          "Global translation session is not bound to Activity lifecycle")
    check("translationSession?.stop()" in base,
          "Translation listeners must be released on pause")
    translator = (KOTLIN / "Os1n7Translations.kt").read_text(encoding="utf-8")
    check("requireWifi()" in translator,
          "Translation models must not silently download over cellular data")
    check("TranslateLanguage.UKRAINIAN" in translator,
          "Source language must be Ukrainian")
    check("if (view is EditText)" in translator,
          "User's input and research notes must remain unmodified")
    check("translateInto(view, raw)" in translator,
          "Dynamic hard-coded content not being translated")
    for file in KOTLIN.glob("*Activity.kt"):
        if file.name == "Os1n7Activity.kt":
            continue
        code = file.read_text(encoding="utf-8")
        check(re.search(r"class\s+\w+Activity\s*:\s*Os1n7Activity\(\)", code) is not None,
              "Screen bypasses app locale: " + file.name)
    gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    check('implementation("com.google.mlkit:translate:17.0.3")' in gradle,
          "Missing ML Kit translation library")
    print("OS1N7 localization QA: PASS")
    print(f"{len(LOCALES)} locales, {len(reference)} string keys each, all Activities localized")
    print("Wi-Fi-only model downloads, original user inputs preserved")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, ValueError, OSError, ET.ParseError) as exc:
        print("OS1N7 localization QA: FAIL:", exc, file=sys.stderr)
        sys.exit(1)
