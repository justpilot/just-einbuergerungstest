# Question catalog pipeline

Generates `app/src/main/assets/questions.json` and `app/src/main/assets/images/` from the official
BAMF practice portal (<https://oet.bamf.de/ords/oetut/f?p=534:1::::::>). Re-run it whenever BAMF
updates the catalog.

## Setup (once)

```bash
cd tools/catalog
brew install tesseract
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
mkdir -p .tessdata && curl -L -o .tessdata/deu.traineddata \
  https://github.com/tesseract-ocr/tessdata_best/raw/main/deu.traineddata
```

## Update the catalog

```bash
.venv/bin/python fetch.py          # ~15 min, writes raw/ (git-ignored)
.venv/bin/python build.py --check  # validate only
.venv/bin/python build.py          # write the app asset, prints added/changed/removed ids
```

Review every id listed as added or changed: OCR'd text, the cropped image and the translations.

## Tests

```bash
.venv/bin/python -m unittest
```

The tests use saved portal pages and images from `tests/fixtures/` and never touch the network. The
OCR test is skipped when Tesseract or the German model is missing.

## Files

| File | Purpose |
| --- | --- |
| `fetch.py` | Scrapes the portal: 300 general questions once, then the 10 questions of each of the 16 states. Stores answers, the correct answer, the portal's internal code and the question image. |
| `build.py` | OCRs question texts that only exist as images, crops pictures, applies overrides, categories and translations, validates and writes the asset. |
| `overrides.json` | Manual corrections keyed by question id: `question`, `options` (all four), `category`, `image: false`. Use it for OCR mistakes. |
| `categories.json` | Topic of each general question (`politics`, `history`, `society`). The portal does not publish one, so it is maintained by hand. State questions are always `state`. |
| `translations.py` | `export <lang> todo.json` writes all questions whose translation is missing or stale; translate them and `import <lang> done.json` merges them with the current source hash. |
| `translations/<lang>.json` | `{id: {source, question, options[4]}}` for `en`, `uk` and `tr`. `source` is the hash of the German text the translation was made from; `build.py` warns when the German text changes and a translation becomes stale. |

## How the portal works

It is an Oracle APEX app. Page 1 selects the federal state, page 30 shows one question; navigation
is done by submitting the "Gehe zu Aufgabe Nr." select. Question text is either HTML or, for many
questions, only part of the question image. The correct answer is the row marked `FARBE`.
