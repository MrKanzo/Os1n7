"""Tests for original-source AI RSS ingestion; no network is used."""
import importlib.util
import pathlib
import unittest
from datetime import datetime, timezone
from email.utils import format_datetime

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("sync_ai_news", ROOT / "scripts/sync_ai_news.py")
ai = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ai)


def rss(link="https://openai.com/index/demo", date=None):
    date = date or format_datetime(datetime.now(timezone.utc))
    return f"""<rss version="2.0"><channel><item>
        <title>Demo AI research publication</title>
        <link>{link}</link>
        <description>Short original description</description>
        <pubDate>{date}</pubDate>
    </item></channel></rss>""".encode("utf-8")


class AiNewsFeedTests(unittest.TestCase):
    def test_reject_external_link(self):
        self.assertEqual(ai.parse_feed(rss("https://openai.com.evil.example/news"), "OpenAI"), [])
        self.assertEqual(ai.canonical_url("http://openai.com/news", "OpenAI"), "")
        self.assertFalse(ai.canonical_url("https://openai.com@bad.example/news", "OpenAI"))

    def test_source_attribution_and_dates(self):
        parsed = ai.parse_feed(rss(), "OpenAI")
        self.assertEqual(len(parsed), 1)
        self.assertEqual(parsed[0]["source"], "OpenAI")
        self.assertEqual(parsed[0]["published"], datetime.now(timezone.utc).date().isoformat())
        self.assertEqual(parsed[0]["url"], "https://openai.com/index/demo")

    def test_source_isolation_and_fallback(self):
        old = {"schemaVersion": 1, "language": "uk", "updatedAt": "old",
               "news": [{"id": "cached", "source": "Google DeepMind",
                         "url": "https://deepmind.google/blog/example",
                         "published": "2026-11-22", "title": "Збережена стаття",
                         "summary": "Попередній запис"}], "sources": []}
        def downloader(url):
            if "openai.com" in url:
                return rss()
            raise OSError("Temporary source outage")
        snapshot = ai.generate(old, downloader)
        self.assertEqual(len(snapshot["news"]), 2)
        self.assertEqual(snapshot["sources"][0]["ok"], True)
        self.assertEqual(snapshot["sources"][1]["ok"], False)
        self.assertEqual(snapshot["sources"][2]["ok"], False)

    def test_reuse_translation_without_changing_original(self):
        old = {"schemaVersion": 1, "language": "uk", "updatedAt": "",
               "news": [{"id": ai.hashlib.sha256("https://openai.com/index/demo".encode()).hexdigest()[:20],
                         "source": "OpenAI", "title": "Переклад",
                         "summary": "Опис", "published": "2026-11-22",
                         "url": "https://openai.com/index/demo",
                         "originalTitle": "Demo AI research publication",
                         "originalSummary": "Short original description",
                         "translation": "uk-v1"}], "sources": []}
        snapshot = ai.generate(old, lambda url: rss() if "openai.com" in url else (_ for _ in ()).throw(OSError()))
        self.assertEqual(next(row for row in snapshot["news"] if row["source"] == "OpenAI")["title"], "Переклад")


if __name__ == "__main__":
    unittest.main()
