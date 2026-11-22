#!/usr/bin/env python3
"""Build a small, attributed OS1N7 cyber-intelligence snapshot from official sources.

No API keys or third-party news scrapers are required. Each source is isolated:
a temporary outage retains the last known-good news or KEV statistics.
"""
from __future__ import annotations

import hashlib
import html
import json
import re
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from collections import Counter
from datetime import datetime, timedelta, timezone
from email.utils import parsedate_to_datetime
from html.parser import HTMLParser
from pathlib import Path

OUTPUT = Path(__file__).resolve().parents[1] / "content" / "cyber-intel.json"
FEEDS = [
    ("CERT-UA", "https://cert.gov.ua/api/articles/rss"),
    ("CISA", "https://www.cisa.gov/cybersecurity-advisories/all.xml"),
    ("NCSC UK", "https://www.ncsc.gov.uk/api/1/services/v1/report-rss-feed.xml"),
]
OFFICIAL_DOMAINS = {
    "CERT-UA": {"cert.gov.ua"},
    "CISA": {"cisa.gov", "www.cisa.gov"},
    "NCSC UK": {"ncsc.gov.uk", "www.ncsc.gov.uk"},
}
KEV_MIRROR = "https://raw.githubusercontent.com/cisagov/kev-data/develop/known_exploited_vulnerabilities.json"
KEV_SOURCE = "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
MAX_DOWNLOAD = 8 * 1024 * 1024
ATOM = "{http://www.w3.org/2005/Atom}"

# Manually verified against FBI IC3 annual reports; not a live count of hacks.
IC3_COMPLAINTS = {
    2020: 791790,
    2021: 847376,
    2022: 800944,
    2023: 880418,
    2024: 859532,
    2025: 1008597,
}
IC3_REPORT = "https://www.ic3.gov/AnnualReport/Reports/2025_IC3Report.pdf"


class PlainText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.words = []

    def handle_data(self, data):
        self.words.append(data)


def clean(text, limit=320):
    parser = PlainText()
    parser.feed(str(text or ""))
    result = html.unescape(" ".join(parser.words))
    return " ".join(result.split())[:limit]


def fetch(url):
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "OS1N7-cyber-intel/1.0 (public official feeds)",
            "Accept": "application/rss+xml, application/atom+xml, application/xml, application/json, */*",
        },
    )
    with urllib.request.urlopen(request, timeout=22) as response:
        if response.status != 200:
            raise ValueError(f"HTTP {response.status}")
        data = response.read(MAX_DOWNLOAD + 1)
        if len(data) > MAX_DOWNLOAD:
            raise ValueError("Response too large")
        return data


def published_day(raw):
    value = str(raw or "").strip()
    if not value:
        return ""
    try:
        date = parsedate_to_datetime(value)
    except (TypeError, ValueError, IndexError):
        try:
            date = datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError:
            return ""
    if date.tzinfo is None:
        date = date.replace(tzinfo=timezone.utc)
    date = date.astimezone(timezone.utc)
    if date > datetime.now(timezone.utc) + timedelta(days=1):
        return ""
    return date.strftime("%Y-%m-%d")


def official_link(url, source):
    parsed = urllib.parse.urlparse(str(url or "").strip())
    host = (parsed.hostname or "").lower()
    if parsed.scheme != "https" or host not in OFFICIAL_DOMAINS[source]:
        return ""
    if parsed.username or parsed.password:
        return ""
    return urllib.parse.urlunparse((parsed.scheme, parsed.netloc, parsed.path, "", parsed.query, ""))[:1200]


def parse_feed(data, source):
    root = ET.fromstring(data)
    entries = root.findall(".//item")
    is_atom = not entries
    if is_atom:
        entries = root.findall(".//" + ATOM + "entry")
    articles = []
    seen = set()
    for item in entries[:70]:
        if is_atom:
            title = item.findtext(ATOM + "title", default="")
            summary = item.findtext(ATOM + "summary", default="") or item.findtext(ATOM + "content", default="")
            day = published_day(item.findtext(ATOM + "updated") or item.findtext(ATOM + "published"))
            link = ""
            for el in item.findall(ATOM + "link"):
                if el.get("rel", "alternate") == "alternate":
                    link = el.get("href", "")
                    break
        else:
            title = item.findtext("title", default="")
            summary = item.findtext("description", default="")
            day = published_day(item.findtext("pubDate") or item.findtext("{http://purl.org/dc/elements/1.1/}date"))
            link = item.findtext("link", default="")
        url = official_link(link, source)
        title = clean(title, 190)
        if not url or not title or not day or url in seen:
            continue
        seen.add(url)
        articles.append({
            "id": hashlib.sha256(url.encode("utf-8")).hexdigest()[:20],
            "source": source,
            "title": title,
            "summary": clean(summary, 310),
            "published": day,
            "url": url,
        })
    return articles[:18]


def kev_series(data):
    payload = json.loads(data)
    rows = payload.get("vulnerabilities")
    if not isinstance(rows, list) or not rows:
        raise ValueError("CISA KEV catalog is empty or invalid")
    years = Counter()
    for item in rows:
        day = str(item.get("dateAdded", ""))
        if re.fullmatch(r"20\d{2}-\d{2}-\d{2}", day):
            years[int(day[:4])] += 1
    if not years:
        raise ValueError("CISA KEV has no dated entries")
    return {
        "id": "cisa-kev",
        "title": "CISA KEV: підтверджено експлуатовані вразливості",
        "unit": "додано в KEV",
        "description": "Кількість CVE, внесених до каталогу CISA KEV у відповідному році. Це НЕ кількість зламів і не дата фактичної атаки.",
        "source": "CISA (офіційне дзеркало GitHub)",
        "sourceUrl": KEV_SOURCE,
        "sourceUpdated": str(payload.get("dateReleased") or payload.get("catalogVersion") or ""),
        "years": [{"year": year, "count": years[year]} for year in sorted(years)],
    }


def ic3_series():
    return {
        "id": "fbi-ic3",
        "title": "FBI IC3: заяви про інтернет-злочини, США",
        "unit": "звернень",
        "description": "Офіційні річні звернення до FBI IC3, зокрема про шахрайство й інші кіберзлочини. Це НЕ кількість підтверджених успішних зламів. Дані 2020–2025 звірено зі щорічними звітами; нові роки додаються після перевірки звіту.",
        "source": "FBI Internet Crime Complaint Center (IC3)",
        "sourceUrl": IC3_REPORT,
        "sourceUpdated": "2026-04-06",
        "years": [{"year": year, "count": count} for year, count in sorted(IC3_COMPLAINTS.items())],
    }


def load_previous():
    try:
        data = json.loads(OUTPUT.read_text(encoding="utf-8"))
        return data if data.get("schemaVersion") == 1 else {}
    except (OSError, ValueError):
        return {}


def generate(previous=None):
    previous = previous if previous is not None else load_previous()
    old_news = previous.get("news", [])
    if not isinstance(old_news, list):
        old_news = []
    news = []
    states = []
    for source, feed in FEEDS:
        try:
            items = parse_feed(fetch(feed), source)
            if not items:
                raise ValueError("feed has no dated, official article links")
            news.extend(items)
            states.append({"name": source, "ok": True, "url": feed})
            print(f"{source}: {len(items)} official articles")
        except (OSError, ValueError, ET.ParseError, urllib.error.URLError) as exc:
            # Keep last good data ONLY for this source.
            news.extend(item for item in old_news if isinstance(item, dict) and item.get("source") == source)
            states.append({"name": source, "ok": False, "url": feed})
            print(f"WARNING {source}: {exc}")
    news = sorted({item["id"]: item for item in news if item.get("id")}.values(),
                  key=lambda item: (item["published"], item["id"]), reverse=True)[:42]
    old_statistics = previous.get("statistics", [])
    old_kev = next((item for item in old_statistics if isinstance(item, dict) and item.get("id") == "cisa-kev"), None)
    try:
        kev = kev_series(fetch(KEV_MIRROR))
        states.append({"name": "CISA KEV", "ok": True, "url": KEV_SOURCE})
        print(f"CISA KEV: {sum(x['count'] for x in kev['years'])} catalog entries")
    except (OSError, ValueError, urllib.error.URLError) as exc:
        kev = old_kev
        states.append({"name": "CISA KEV", "ok": False, "url": KEV_SOURCE})
        print(f"WARNING CISA KEV: {exc}")
    statistics = [ic3_series()]
    if kev:
        statistics.insert(0, kev)
    new_data = {
        "schemaVersion": 1,
        "updatedAt": previous.get("updatedAt", ""),
        "news": news,
        "statistics": statistics,
        "sources": states,
    }
    # Keep updatedAt stable unless real content changes; avoid noisy Git commits.
    stable = lambda obj: {k: v for k, v in obj.items() if k not in ("updatedAt", "sources")}
    if stable(new_data) != stable(previous):
        new_data["updatedAt"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    return new_data


def main():
    previous = load_previous()
    payload = generate(previous)
    serialized = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != serialized:
        OUTPUT.write_text(serialized, encoding="utf-8")
        print(f"Updated {OUTPUT}")
    else:
        print("No changes in the official source snapshot")


if __name__ == "__main__":
    main()
