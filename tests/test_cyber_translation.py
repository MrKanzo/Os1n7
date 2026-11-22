"""Unit tests for Ukrainian cyber headlines without downloading translation models."""
import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "cyber_translation", ROOT / "scripts" / "translate_cyber_intel.py"
)
translation = importlib.util.module_from_spec(spec)
spec.loader.exec_module(translation)


def fake_uk(text):
    return "Український опис: " + text


class UkrainianCyberNewsTests(unittest.TestCase):
    def sample(self):
        return {
            "schemaVersion": 1,
            "news": [{
                "id": "cisa-demo",
                "source": "CISA",
                "title": "CISA alerts about CVE-2026-12345",
                "summary": "An exploited vulnerability was reported.",
                "published": "2026-11-20",
                "url": "https://www.cisa.gov/example"
            }, {
                "id": "cert-demo",
                "source": "CERT-UA",
                "title": "Виявлено атаку на Україну",
                "summary": "Офіційна інформація CERT-UA.",
                "published": "2026-11-21",
                "url": "https://cert.gov.ua/article/example"
            }]
        }

    def test_translates_english_but_preserves_official_fields(self):
        sample = self.sample()
        parsed, translated = translation.localize_snapshot(sample, fake_uk)
        self.assertEqual(translated, 2)
        self.assertEqual(parsed["news"][0]["originalTitle"], "CISA alerts about CVE-2026-12345")
        self.assertIn("CVE-2026-12345", parsed["news"][0]["title"])
        self.assertTrue(translation.has_ukrainian(parsed["news"][0]["title"]))
        self.assertEqual(parsed["news"][0]["published"], "2026-11-20")
        self.assertEqual(parsed["news"][0]["url"], "https://www.cisa.gov/example")
        self.assertEqual(parsed["news"][1]["title"], "Виявлено атаку на Україну")

    def test_preserves_ukrainian_snapshot_without_retranslating(self):
        initial = self.sample()
        parsed, _ = translation.localize_snapshot(initial, fake_uk)
        def fail_if_called(_):
            raise AssertionError("Should reuse cached Ukrainian translation")
        parsed, count = translation.localize_snapshot(parsed, fail_if_called)
        self.assertEqual(count, 0)

    def test_english_only_translation_is_rejected(self):
        with self.assertRaises(ValueError):
            translation.localize_snapshot(self.sample(), lambda value: value)

    def test_does_not_erase_cve_identifiers(self):
        converted = translation.translated_text(
            "Vulnerability CVE-2026-12345", lambda _: "Повідомлено про вразливість"
        )
        self.assertIn("CVE-2026-12345", converted)

    def test_title_is_original_and_link_not_edited(self):
        entry = self.sample()["news"][0]
        title = entry["title"]
        link = entry["url"]
        translation.localize_snapshot({"schemaVersion": 1, "news": [entry]}, fake_uk)
        self.assertEqual(entry["originalTitle"], title)
        self.assertEqual(entry["url"], link)


if __name__ == "__main__":
    unittest.main()
