#!/usr/bin/env python3
"""Original-source AI news feed for OS1N7; never fabricate items or scrape user data."""
from __future__ import annotations

import hashlib
import html
import json
import re
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone
from email.utils import parsedate_to_datetime
from html.parser import HTMLParser
from pathlib import Path

OUTPUT = Path(__file__).resolve().parents[1] / "content/ai-news.json"
FEEDS = [
    ("OpenAI", "https://openai.com/news/rss.xml"),
    ("Google DeepMind", "https://deepmind.google/blog/rss.xml"),
    ("Hugging Face", "https://huggingface.co/blog/feed.xml"),
]
DOMAINS = {
    "OpenAI": {"openai.com", "www.openai.com"},
    "Google DeepMind": {"deepmind.google"},
    "Hugging Face": {"huggingface.co", "www.huggingface.co"},
}
ATOM = "{http://www.w3.org/2005/Atom}"
MAX_BYTES = 2 * 1024 * 1024


class PlainText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []

    def handle_data(self, value):
        self.parts.append(value)


def clean(raw, limit=320):
    parser = PlainText()
    parser.feed(str(raw or ""))
    return " ".join(html.unescape(" ".join(parser.parts)).split())[:limit]


def fetch(url):
    request = urllib.request.Request(
        url, headers={"User-Agent": "OS1N7-AI-News/1.0 (RSS; sources credited)",
                      "Accept": "application/rss+xml, application/atom+xml, text/xml, application/xml"}
    )
    with urllib.request.urlopen(request, timeout=22) as response:
        if response.status != 200:
            raise ValueError(f"HTTP {response.status}")
        data = response.read(MAX_BYTES + 1)
        if len(data) > MAX_BYTES:
            raise ValueError("Oversize feed")
        return data


def published_day(raw):
    if not raw:
        return ""
    try:
        value = parsedate_to_datetime(str(raw))
    except (ValueError, TypeError, IndexError):
        try:
            value = datetime.fromisoformat(str(raw).replace("Z", "+00:00"))
        except ValueError:
            return ""
    if value.tzinfo is None:
        value = value.replace(tzinfo=timezone.utc)
    value = value.astimezone(timezone.utc)
    if value > datetime.now(timezone.utc) + timedelta(days=1):
        return ""
    return value.date().isoformat()


def canonical_url(raw, source):
    try:
        parsed = urllib.parse.urlparse(str(raw or "").strip())
        if (parsed.scheme != "https" or parsed.hostname not in DOMAINS[source]
                or parsed.username or parsed.password or parsed.port not in (None, 443)):
            return ""
    except ValueError:
        return ""
    return urllib.parse.urlunparse(("https", parsed.hostname, parsed.path, "", parsed.query, ""))[:1200]


def parse_feed(payload, source):
    root = ET.fromstring(payload)
    items = root.findall(".//item")
    atom = len(items) == 0
    if atom:
        items = root.findall(".//" + ATOM + "entry")
    earliest = (datetime.now(timezone.utc) - timedelta(days=240)).date().isoformat()
    results, seen = [], set()
    for item in items[:90]:
        if atom:
            title = item.findtext(ATOM + "title", default="")
            summary = item.findtext(ATOM + "summary", default="") or item.findtext(ATOM + "content", default="")
            date = published_day(item.findtext(ATOM + "published") or item.findtext(ATOM + "updated"))
            link = ""
            for anchor in item.findall(ATOM + "link"):
                if anchor.get("rel", "alternate") == "alternate":
                    link = anchor.get("href", "")
                    break
        else:
            title = item.findtext("title", default="")
            summary = item.findtext("description", default="")
            date = published_day(item.findtext("pubDate") or item.findtext("{http://purl.org/dc/elements/1.1/}date"))
            link = item.findtext("link", default="")
        url = canonical_url(link, source)
        title = clean(title, 190)
        if not url or not title or not date or date < earliest or url in seen:
            continue
        seen.add(url)
        results.append({
            "id": hashlib.sha256(url.encode("utf-8")).hexdigest()[:20],
            "source": source,
            "title": title,
            "summary": clean(summary, 320),
            "published": date,
            "url": url,
        })
        if len(results) >= 15:
            break
    return results


def load_previous():
    try:
        existing = json.loads(OUTPUT.read_text(encoding="utf-8"))
        if existing.get("schemaVersion") == 1 and isinstance(existing.get("news"), list):
            return existing
    except (ValueError, OSError):
        pass
    return {"schemaVersion": 1, "language": "uk", "updatedAt": "", "news": [], "sources": []}


def generate(previous=None, downloader=fetch):
    previous = previous if previous is not None else load_previous()
    old_news = previous.get("news", [])
    news, sources = [], []
    for source, feed in FEEDS:
        try:
            items = parse_feed(downloader(feed), source)
            if not items:
                raise ValueError("Feed contained no dated original-source entries")
            news.extend(items)
            sources.append({"name": source, "ok": True, "url": feed})
            print(f"{source}: {len(items)} articles")
        except (OSError, ValueError, ET.ParseError, urllib.error.URLError) as exc:
            news.extend(row for row in old_news if isinstance(row, dict)
                        and row.get("source") == source
                        and canonical_url(row.get("url"), source))
            sources.append({"name": source, "ok": False, "url": feed})
            print(f"WARNING: {source}: {exc}")
    old_by_id = {row.get("id"): row for row in old_news if isinstance(row, dict)}
    for item in news:
        old = old_by_id.get(item["id"])
        if (old and old.get("translation") == "uk-v1" and
                old.get("originalTitle") == item.get("title") and
                old.get("originalSummary") == item.get("summary")):
            item.update({key: old[key] for key in
                         ("title", "summary", "originalTitle", "originalSummary", "translation")})
    news = sorted({row["id"]: row for row in news}.values(),
                  key=lambda x: (x["published"], x["id"]), reverse=True)[:40]
    snapshot = {
        "schemaVersion": 1, "language": "uk",
        "updatedAt": previous.get("updatedAt", ""),
        "news": news, "sources": sources,
    }
    old_stable = {k: v for k, v in previous.items() if k not in ("updatedAt", "sources")}
    new_stable = {k: v for k, v in snapshot.items() if k not in ("updatedAt", "sources")}
    if old_stable != new_stable:
        snapshot["updatedAt"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    return snapshot


def main():
    snapshot = generate()
    text = json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n"
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != text:
        OUTPUT.write_text(text, encoding="utf-8")
        print(f"Updated {OUTPUT}")
    else:
        print("AI news content unchanged")


if __name__ == "__main__":
    main()
