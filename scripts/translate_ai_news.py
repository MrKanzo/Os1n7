#!/usr/bin/env python3
"""Translate original-source AI headlines locally; preserve links and original text."""
import json
from pathlib import Path
from translate_cyber_intel import get_translator, localize_snapshot

FILE = Path(__file__).resolve().parents[1] / "content" / "ai-news.json"


def main():
    payload = json.loads(FILE.read_text(encoding="utf-8"))
    if not payload.get("news"):
        print("No AI articles from publishers yet; leaving feed empty.")
        return
    translator = get_translator()
    payload, count = localize_snapshot(payload, translator)
    result = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if FILE.read_text(encoding="utf-8") != result:
        FILE.write_text(result, encoding="utf-8")
    print(f"AI news translated into Ukrainian: {len(payload['news'])} items, {count} new.")


if __name__ == "__main__":
    main()
