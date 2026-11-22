#!/usr/bin/env python3
"""Translate the public OS1N7 cyber news snapshot into Ukrainian.

Original titles, descriptions, official links and dates are preserved. Uses
Argos Translate with an English -> Ukrainian local model, without API keys.
If the translator cannot run, exit before publishing an English-only update.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

DATA_PATH = Path(__file__).resolve().parents[1] / "content" / "cyber-intel.json"
UK_LETTERS = re.compile(r"[А-Яа-яІіЇїЄєҐґ]")
CVE = re.compile(r"\bCVE-\d{4}-\d{4,}\b", re.IGNORECASE)


def has_ukrainian(text: str) -> bool:
    return bool(UK_LETTERS.search(text or ""))


def translated_text(text: str, translator) -> str:
    source = str(text or "").strip()
    if not source or has_ukrainian(source):
        return source
    translated = " ".join(str(translator(source)).strip().split())
    if not has_ukrainian(translated):
        raise ValueError("Machine translation returned no Ukrainian text")
    # Vulnerability identifiers are technical facts and cannot be discarded.
    missing = [code for code in CVE.findall(source) if code.lower() not in translated.lower()]
    if missing:
        translated += " (" + ", ".join(missing) + ")"
    return translated


def localize_snapshot(payload: dict, translator) -> tuple[dict, int]:
    if payload.get("schemaVersion") != 1 or not isinstance(payload.get("news"), list):
        raise ValueError("Unexpected OS1N7 news schema")
    translated_count = 0
    for article in payload["news"]:
        raw_title = article.get("originalTitle", article.get("title", ""))
        raw_summary = article.get("originalSummary", article.get("summary", ""))
        if not str(raw_title).strip():
            raise ValueError("News item missing original title")
        if (article.get("translation") == "uk-v1"
                and has_ukrainian(article.get("title", ""))
                and (not raw_summary or has_ukrainian(article.get("summary", "")))):
            continue
        ukrainian_title = translated_text(raw_title, translator)
        ukrainian_summary = translated_text(raw_summary, translator)
        if not has_ukrainian(ukrainian_title):
            raise ValueError("No Ukrainian headline for " + article.get("id", "unknown"))
        article["originalTitle"] = raw_title
        article["originalSummary"] = raw_summary
        article["title"] = ukrainian_title
        article["summary"] = ukrainian_summary
        article["translation"] = "uk-v1"
        translated_count += 1
    return payload, translated_count


def get_translator():
    # Imported only for the production CLI, so unit tests do not need a model.
    import argostranslate.package
    import argostranslate.translate

    languages = argostranslate.translate.get_installed_languages()
    installed_en = next((language for language in languages if language.code == "en"), None)
    installed_uk = next((language for language in languages if language.code == "uk"), None)
    if installed_en is None or installed_uk is None or not installed_en.get_translation(installed_uk):
        argostranslate.package.update_package_index()
        matches = [model for model in argostranslate.package.get_available_packages()
                   if model.from_code == "en" and model.to_code == "uk"]
        if not matches:
            raise RuntimeError("Argos English-to-Ukrainian model unavailable")
        package_path = matches[0].download()
        argostranslate.package.install_from_path(package_path)
    def translate(text: str) -> str:
        return argostranslate.translate.translate(text, "en", "uk")
    return translate


def main():
    payload = json.loads(DATA_PATH.read_text(encoding="utf-8"))
    translate = get_translator()
    payload, count = localize_snapshot(payload, translate)
    if any(not has_ukrainian(item.get("title", "")) for item in payload["news"]):
        raise RuntimeError("Refusing to publish untranslated English headlines")
    new_content = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if DATA_PATH.read_text(encoding="utf-8") != new_content:
        DATA_PATH.write_text(new_content, encoding="utf-8")
    print(f"Ukrainian cybernews ready: {len(payload['news'])} headlines, {count} translated now.")


if __name__ == "__main__":
    main()
