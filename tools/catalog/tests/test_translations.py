import unittest

import build
import translations
from tests.test_build import question


def entry(text="What is that?"):
    return {"question": text, "options": ["a", "b", "c", "d"]}


class TranslationsTest(unittest.TestCase):
    def setUp(self):
        self.catalog = {"1": question("1"), "2": question("2")}

    def test_todo_lists_missing_and_stale(self):
        current = translations.merge(self.catalog, {}, {"1": entry()})
        self.assertEqual([t["id"] for t in translations.todo(self.catalog, current)], ["2"])

        self.catalog["1"]["question"]["de"] = "Geänderte Frage?"
        self.assertEqual([t["id"] for t in translations.todo(self.catalog, current)], ["1", "2"])

    def test_merge_stamps_source_hash_and_keeps_existing(self):
        first = translations.merge(self.catalog, {}, {"1": entry()})
        merged = translations.merge(self.catalog, first, {"2": entry("Other?")})

        self.assertEqual(merged["1"], first["1"])
        self.assertEqual(merged["2"]["source"], build.source_hash(self.catalog["2"]))

    def test_merge_rejects_unknown_ids_and_bad_entries(self):
        with self.assertRaises(ValueError):
            translations.merge(self.catalog, {}, {"999": entry()})
        with self.assertRaises(ValueError):
            translations.merge(self.catalog, {}, {"1": {"question": "Q?", "options": ["a", "b", "c"]}})
        with self.assertRaises(ValueError):
            translations.merge(self.catalog, {}, {"1": {"question": " ", "options": ["a", "b", "c", "d"]}})


if __name__ == "__main__":
    unittest.main()
