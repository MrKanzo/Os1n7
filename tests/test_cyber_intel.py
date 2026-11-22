"""Offline fixture tests for official-source OS1N7 news ingestion."""
import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("cyber_intel", ROOT / "scripts" / "sync_cyber_intel.py")
intel = importlib.util.module_from_spec(spec)
spec.loader.exec_module(intel)


class CyberIntelTests(unittest.TestCase):
    def test_official_cert_rss(self):
        xml = b"""<rss version="2.0"><channel><item>
        <title>New advisory</title><link>https://cert.gov.ua/article/100</link>
        <description>Security &amp; updates</description>
        <pubDate>Sun, 22 Nov 2026 12:00:00 GMT</pubDate>
        </item></channel></rss>"""
        entries = intel.parse_feed(xml, "CERT-UA")
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0]["source"], "CERT-UA")
        self.assertEqual(entries[0]["published"], "2026-11-22")

    def test_reject_unofficial_links(self):
        xml = b"""<rss version="2.0"><channel><item>
        <title>Fake official advisory</title><link>https://cert.gov.ua.bad.example/article/5</link>
        <pubDate>Sun, 22 Nov 2026 12:00:00 GMT</pubDate>
        </item></channel></rss>"""
        self.assertEqual(intel.parse_feed(xml, "CERT-UA"), [])
        self.assertFalse(intel.official_link("http://cert.gov.ua/article/4", "CERT-UA"))

    def test_cisa_kev_years_are_catalog_additions(self):
        payload = b'{"vulnerabilities":[{"dateAdded":"2022-11-03"},{"dateAdded":"2023-01-11"},{"dateAdded":"2023-01-11"}],"catalogVersion":"test"}'
        series = intel.kev_series(payload)
        self.assertEqual(series["years"], [{"year": 2022, "count": 1}, {"year": 2023, "count": 2}])
        self.assertIn("НЕ", series["description"])

    def test_invalid_date_not_reinterpreted_as_today(self):
        self.assertEqual(intel.published_day("bogus"), "")

    def test_manual_fbi_series_not_mislabeled_as_hacks(self):
        series = intel.ic3_series()
        self.assertEqual(series["years"][-1], {"year": 2025, "count": 1008597})
        self.assertIn("НЕ", series["description"])


if __name__ == "__main__":
    unittest.main()
