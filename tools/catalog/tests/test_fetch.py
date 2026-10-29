import unittest
from pathlib import Path

import fetch

FIXTURES = Path(__file__).parent / "fixtures"


def page(name):
    return (FIXTURES / name).read_text()


class ParseQuestionTest(unittest.TestCase):
    def test_question_with_text_in_image(self):
        data, image_src = fetch.parse_question(page("question_image_text.html"), "4619")

        self.assertEqual(data["number"], 1)
        self.assertEqual(data["code"], "LD5024a_Meinungsfreiheit")
        self.assertIsNone(data["text"])
        self.assertEqual(data["answers"], [
            "hier Religionsfreiheit gilt.",
            "die Menschen Steuern zahlen.",
            "die Menschen das Wahlrecht haben.",
            "hier Meinungsfreiheit gilt.",
        ])
        self.assertEqual(data["correct"], 3)
        self.assertTrue(image_src.endswith(",4619"))
        self.assertNotIn("&amp;", image_src)

    def test_question_with_html_text(self):
        data, image_src = fetch.parse_question(page("question_html_text.html"), "4622")

        self.assertEqual(data["number"], 14)
        self.assertEqual(data["text"], "Meinungsfreiheit in Deutschland heißt zum Beispiel, dass ich")
        self.assertEqual(len(data["answers"]), 4)
        self.assertIsInstance(data["correct"], int)
        self.assertIsNone(image_src)

    def test_rejects_image_of_another_question(self):
        with self.assertRaisesRegex(RuntimeError, "does not belong"):
            fetch.parse_question(page("question_image_text.html"), "9999")

    def test_rejects_non_question_page(self):
        with self.assertRaisesRegex(RuntimeError, "not a question page"):
            fetch.parse_question("<html></html>", "1")


class ParseCatalogTest(unittest.TestCase):
    def test_lists_all_310_questions(self):
        catalog = fetch.parse_catalog(page("question_image_text.html"))

        self.assertEqual(sorted(catalog), list(range(1, 311)))
        self.assertEqual(catalog[1], "4619")
        self.assertEqual(len(set(catalog.values())), 310)


class TextTest(unittest.TestCase):
    def test_strips_tags_entities_and_whitespace(self):
        self.assertEqual(fetch._text(" <b>a&nbsp;&amp;</b><br/>\n  b "), "a & b")


if __name__ == "__main__":
    unittest.main()
