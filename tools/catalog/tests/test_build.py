import shutil
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import build

FIXTURES = Path(__file__).parent / "fixtures"


def png(name):
    return (FIXTURES / name).read_bytes()


def question(question_id="1", state=None, category="politics"):
    return {
        "id": question_id,
        "state": state,
        "category": category,
        "question": {"de": "Was ist das?"},
        "options": [{"id": i, "text": {"de": f"Antwort {i}"}} for i in build.OPTION_IDS],
        "correct_option": "B",
    }


def full_catalog():
    general = [question(str(n)) for n in range(1, 301)]
    states = [question(f"{s}-{n}", s, "state") for s in build.STATES for n in range(1, 11)]
    return general + states


class NormalizeTest(unittest.TestCase):
    def test_joins_lines_and_hyphenation(self):
        self.assertEqual(build.normalize("Bundes-\nrepublik und\nmehr"), "Bundesrepublik und mehr")

    def test_keeps_hyphen_before_capital(self):
        self.assertEqual(build.normalize("Baden-\nWürttemberg"), "Baden-Württemberg")

    def test_ellipsis_and_punctuation(self):
        self.assertEqual(build.normalize("sagen, weil..."), "sagen, weil …")
        self.assertEqual(build.normalize("Was zeigt dieses Bild ?"), "Was zeigt dieses Bild?")


class FixOcrTest(unittest.TestCase):
    def fix(self, text, word_fixes=None):
        return build.fix_ocr(text, word_fixes or {})

    def test_welche_misread_as_weiche(self):
        self.assertEqual(self.fix("Weiche Farben, weichem Alter, Weilchem Grundsatz"),
                         "Welche Farben, welchem Alter, Welchem Grundsatz")

    def test_spaces_around_compound_hyphen(self):
        self.assertEqual(self.fix("Baden -Württemberg und Nordrhein- Westfalen"), "Baden-Württemberg und Nordrhein-Westfalen")
        self.assertEqual(self.fix("schwarz -rot-goldene"), "schwarz-rot-goldene")
        self.assertEqual(self.fix("Bundestags- und Landtagswahlen"), "Bundestags- und Landtagswahlen")

    def test_split_compounds(self):
        self.assertEqual(self.fix("die Regierung schefin"), "die Regierungschefin")
        self.assertEqual(self.fix("Bundestag swahlen"), "Bundestagswahlen")
        self.assertEqual(self.fix("gegen die Regierung sagen"), "gegen die Regierung sagen")

    def test_capital_in_mid_sentence(self):
        self.assertEqual(self.fix("Wahlen In Deutschland"), "Wahlen in Deutschland")
        self.assertEqual(self.fix("In Deutschland"), "In Deutschland")

    def test_question_mark_misreads(self):
        self.assertEqual(self.fix("Was war am 8. Mai 19457"), "Was war am 8. Mai 1945?")
        self.assertEqual(self.fix("von 1939 bis 194527"), "von 1939 bis 1945?")
        self.assertEqual(self.fix("„Wir sind das Volk“2"), "„Wir sind das Volk“?")

    def test_slash_spacing(self):
        self.assertEqual(self.fix("Präsidenten/ zur Präsidentin"), "Präsidenten / zur Präsidentin")
        self.assertEqual(self.fix("ist er/sie"), "ist er/sie")

    def test_word_table_matches_whole_words_only(self):
        fixes = {"Wesffalen": "Westfalen"}
        self.assertEqual(self.fix("Nordrhein-Wesffalen", fixes), "Nordrhein-Westfalen")
        self.assertEqual(self.fix("Wesffalens", fixes), "Wesffalens")


class SplitImageTest(unittest.TestCase):
    def test_text_only_image_has_no_picture(self):
        text, picture = build.split_image(png("text_only.png"), has_text=False)

        self.assertIsNotNone(text)
        self.assertIsNone(picture)

    def test_picture_is_cropped_below_the_text(self):
        text, picture = build.split_image(png("text_and_picture.png"), has_text=False)

        self.assertLess(text.height, 40)
        self.assertIsNotNone(picture)
        self.assertGreater(picture.height, 250)
        self.assertLess(picture.width, 532)

    def test_whole_image_is_picture_when_text_is_html(self):
        text, picture = build.split_image(png("text_and_picture.png"), has_text=True)

        self.assertIsNone(text)
        self.assertGreater(picture.height, 330)


@unittest.skipUnless(shutil.which("tesseract") and (build.TESSDATA / "deu.traineddata").exists(),
                     "tesseract with German model not installed")
class OcrTest(unittest.TestCase):
    def setUp(self):
        self.cache = Path(tempfile.mkdtemp())
        patcher = mock.patch.object(build, "OCR_CACHE", self.cache)
        patcher.start()
        self.addCleanup(patcher.stop)
        self.addCleanup(shutil.rmtree, self.cache)

    def test_reads_question_text_and_caches_it(self):
        data = png("text_only.png")
        text_band, _ = build.split_image(data, has_text=False)

        text = build.normalize(build.ocr("1", data, text_band))

        self.assertEqual(text, "In Deutschland dürfen Menschen offen etwas gegen die Regierung sagen, weil …")
        self.assertEqual(len(list(self.cache.glob("1.*.txt"))), 1)


class ValidateTest(unittest.TestCase):
    def test_complete_catalog_is_valid(self):
        errors = []
        build.validate(full_catalog(), errors)
        self.assertEqual(errors, [])

    def test_reports_missing_questions(self):
        errors = []
        build.validate(full_catalog()[1:], errors)
        self.assertTrue(any("missing ['1']" in e for e in errors), errors)

    def test_reports_bad_question(self):
        catalog = full_catalog()
        catalog[0]["category"] = None
        catalog[1]["options"] = catalog[1]["options"][:3]
        catalog[2]["question"]["de"] = ""
        errors = []

        build.validate(catalog, errors)

        self.assertEqual(len(errors), 3, errors)

    def test_translated_options_must_not_add_glosses(self):
        catalog = full_catalog()
        catalog[0]["options"][1]["text"]["en"] = "Answer B (Antwort B)"
        catalog[1]["options"][1]["text"]["de"] = "Wahl (Landtag)"
        catalog[1]["options"][1]["text"]["en"] = "Election (state parliament)"
        errors = []

        build.validate(catalog, errors)

        self.assertEqual(errors, ["1: en option B adds a parenthetical gloss"])

    def test_numbered_answers_need_a_picture(self):
        catalog = full_catalog()
        for option, number in zip(catalog[0]["options"], "1234"):
            option["text"]["de"] = number
        errors = []

        build.validate(catalog, errors)

        self.assertEqual(errors, ["1: answers refer to numbered pictures but there is no image"])


class SourceHashTest(unittest.TestCase):
    def test_changes_with_german_text_only(self):
        a, b = question(), question()
        b["question"]["en"] = "What is that?"
        self.assertEqual(build.source_hash(a), build.source_hash(b))
        b["options"][0]["text"]["de"] = "anders"
        self.assertNotEqual(build.source_hash(a), build.source_hash(b))


class SortKeyTest(unittest.TestCase):
    def test_general_numerically_then_states(self):
        ids = ["BY-10", "10", "BY-2", "2", "BE-1"]
        self.assertEqual(sorted(ids, key=build.sort_key), ["2", "10", "BE-1", "BY-2", "BY-10"])


if __name__ == "__main__":
    unittest.main()
