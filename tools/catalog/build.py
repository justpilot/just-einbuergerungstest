#!/usr/bin/env python3
"""Build app/src/main/assets/questions.json (+ images/) from the raw/ cache written by fetch.py.

Steps per question:
  1. Take the question text from the portal HTML, or, where the portal only publishes it inside
     the question image, split the image into the text band (top) and an optional picture below
     it and OCR the text band (Tesseract, German model in .tessdata/).
  2. Crop the picture (if any) into assets/images/<id>.png.
  3. Apply manual corrections from overrides.json and the category from categories.json.
  4. Merge translations from translations/<lang>.json.
Then validate the whole catalog, write the asset and print what changed.

Usage:
    python build.py            # build and write the asset
    python build.py --check    # build and validate only, write nothing
"""

import argparse
import hashlib
import io
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageOps

HERE = Path(__file__).parent
RAW = HERE / "raw"
OCR_CACHE = HERE / "raw" / "ocr"
ASSETS = HERE.parent.parent / "app" / "src" / "main" / "assets"
TESSDATA = HERE / ".tessdata"
LANGUAGES = ["en", "uk", "tr"]
CATEGORIES = {"politics", "history", "society", "state"}
STATES = ["BW", "BY", "BE", "BB", "HB", "HH", "HE", "MV", "NI", "NW", "RP", "SL", "SN", "ST", "SH", "TH"]
OPTION_IDS = ["A", "B", "C", "D"]

INK = 160            # grey value below which a pixel counts as ink
TEXT_GAP = 10        # rows of blank space that separate the question text from a picture
PADDING = 8          # blank border kept around a cropped picture
OCR_VERSION = 2      # bump when the OCR preprocessing changes to invalidate the cache


def load_json(path, default):
    return json.loads(path.read_text()) if path.exists() else default


def ink_runs(image):
    """Vertical ranges [start, end) of rows that contain ink."""
    pixels = image.load()
    runs, start = [], None
    for y in range(image.height + 1):
        has_ink = y < image.height and any(pixels[x, y] < INK for x in range(image.width))
        if has_ink and start is None:
            start = y
        elif not has_ink and start is not None:
            runs.append((start, y))
            start = None
    return runs


def split_image(png, has_text):
    """Return (text band image or None, picture image or None).

    has_text: the question text came as HTML, so the whole image is the picture.
    """
    image = Image.open(io.BytesIO(png)).convert("L")
    runs = ink_runs(image)
    text, picture = None, runs
    if not has_text:
        text_end = runs[0][1]
        picture = []
        for start, end in runs[1:]:
            if picture or start - text_end >= TEXT_GAP:
                picture.append((start, end))
            else:
                text_end = end
        text = image.crop((0, 0, image.width, min(image.height, text_end + 4)))
    if not picture:
        return text, None
    top, bottom = picture[0][0], picture[-1][1]
    band = image.crop((0, top, image.width, bottom))
    pixels = band.load()
    columns = [x for x in range(band.width) if any(pixels[x, y] < INK for y in range(band.height))]
    left, right = columns[0], columns[-1] + 1
    box = (max(0, left - PADDING), max(0, top - PADDING), min(image.width, right + PADDING), min(image.height, bottom + PADDING))
    return text, Image.open(io.BytesIO(png)).convert("RGB").crop(box)


def ocr(question_id, png, text_band):
    """Raw Tesseract output for the text band; cached by image hash so rebuilds are fast."""
    digest = hashlib.sha256(png).hexdigest()[:16]
    cache = OCR_CACHE / f"{question_id}.{OCR_VERSION}.{digest}.txt"
    if cache.exists():
        return cache.read_text()
    # Upscaled, binarised and padded text gives Tesseract far fewer misreads of the italic font.
    scaled = text_band.resize((text_band.width * 4, text_band.height * 4), Image.LANCZOS)
    prepared = ImageOps.expand(scaled.point(lambda p: 255 if p > 150 else 0), 40, fill=255)
    with tempfile.NamedTemporaryFile(suffix=".png") as file:
        prepared.save(file.name)
        result = subprocess.run(
            ["tesseract", file.name, "-", "-l", "deu", "--psm", "6"],
            capture_output=True, text=True, check=True,
            env={**os.environ, "TESSDATA_PREFIX": str(TESSDATA)},
        )
    OCR_CACHE.mkdir(parents=True, exist_ok=True)
    for old in OCR_CACHE.glob(f"{question_id}.*.txt"):
        old.unlink()
    cache.write_text(result.stdout)
    return result.stdout


def normalize(text):
    text = re.sub(r"-\n(?=[a-zäöüß])", "", text)          # word split across lines
    text = re.sub(r"-\n(?=[A-ZÄÖÜ])", "-", text)          # compound like Baden-Württemberg
    text = re.sub(r"\s+", " ", text).strip()
    text = re.sub(r"\s*\.\.\.|\s*…", " …", text)
    text = re.sub(r"\s+([?!.,:;])", r"\1", text)
    text = text.replace("„ ", "„").replace(" “", "“").replace("”", "“")
    return text.strip()


# Systematic Tesseract misreads of the portal's italic font. Only applied to OCR'd text.
OCR_RULES = [
    (r"\b([Ww])eil?ch(e|es|er|em|en)\b", r"\1elch\2"),              # "Weiche", "weichem", "Weilchem"
    (r"(\w) ?- ?([A-ZÄÖÜ])", r"\1-\2"),                               # "Baden -Württemberg"
    (r"(\w) -(\w)", r"\1-\2"),                                        # "schwarz -rot-goldene"
    (r"\b(\w+(?:ung|tag)) s(chef|zone|mächt|mitglied|organ|wahl|problem)", r"\1s\2"),  # "Regierung schefin"
    (r"(?<=[a-zäöüß] )In\b", "in"),                                    # "Wahlen In Deutschland"
    (r"(\d{4})2?7$", r"\1?"),                                          # "1945?" read as "19457"
    (r"“2$", "“?"),                                                     # "?" read as "2"
    (r"(\w)/ (\w)", r"\1 / \2"),                                      # "Präsidenten/ zur"
]


def fix_ocr(text, word_fixes):
    for pattern, replacement in OCR_RULES:
        text = re.sub(pattern, replacement, text)
    for wrong, right in word_fixes.items():
        text = re.sub(rf"(?<!\w){re.escape(wrong)}(?!\w)", right, text)
    return text


def source_hash(question):
    german = [question["question"]["de"]] + [option["text"]["de"] for option in question["options"]]
    return hashlib.sha256("\n".join(german).encode()).hexdigest()[:12]


def sort_key(question_id):
    if question_id.isdigit():
        return (0, "", int(question_id))
    state, number = question_id.split("-")
    return (1, state, int(number))


def build():
    overrides = load_json(HERE / "overrides.json", {})
    categories = load_json(HERE / "categories.json", {})
    word_fixes = load_json(HERE / "ocr_fixes.json", {})
    translations = {lang: load_json(HERE / "translations" / f"{lang}.json", {}) for lang in LANGUAGES}
    errors, warnings = [], []
    questions, images = [], {}

    for raw_path in sorted(RAW.glob("*.json"), key=lambda p: sort_key(p.stem)):
        raw = json.loads(raw_path.read_text())
        question_id = raw["id"]
        override = overrides.get(question_id, {})
        png_path = RAW / f"{question_id}.png"
        png = png_path.read_bytes() if png_path.exists() else None
        text_band, picture = split_image(png, raw["text"] is not None) if png else (None, None)
        if override.get("image") is False:
            picture = None
        if raw["text"] is not None:
            text = normalize(raw["text"])
        else:
            text = fix_ocr(normalize(ocr(question_id, png, text_band)), word_fixes)

        question = {
            "id": question_id,
            "number": raw["number"] if raw["state"] is None else raw["number"] - 300,
            "state": raw["state"],
            "category": "state" if raw["state"] else override.get("category", categories.get(question_id)),
            "image": f"images/{question_id}.png" if picture else None,
            "question": {"de": override.get("question", text)},
            "options": [
                {"id": option_id, "text": {"de": text}}
                for option_id, text in zip(OPTION_IDS, override.get("options", raw["answers"]))
            ],
            "correct_option": OPTION_IDS[raw["correct"]],
        }
        if picture:
            images[question_id] = picture

        hash_ = source_hash(question)
        for lang in LANGUAGES:
            translation = translations[lang].get(question_id)
            if translation is None:
                warnings.append(f"{question_id}: missing {lang} translation")
                continue
            if translation["source"] != hash_:
                warnings.append(f"{question_id}: {lang} translation is stale (German text changed)")
            question["question"][lang] = translation["question"]
            for option, text in zip(question["options"], translation["options"]):
                option["text"][lang] = text
        questions.append(question)

    validate(questions, errors)
    return questions, images, errors, warnings


def validate(questions, errors):
    ids = [q["id"] for q in questions]
    expected = [str(n) for n in range(1, 301)] + [f"{s}-{n}" for s in STATES for n in range(1, 11)]
    if sorted(ids, key=sort_key) != sorted(expected, key=sort_key):
        missing, extra = set(expected) - set(ids), set(ids) - set(expected)
        errors.append(f"catalog ids incomplete: missing {sorted(missing, key=sort_key)}, unexpected {sorted(extra)}")
    for q in questions:
        if q["category"] not in CATEGORIES:
            errors.append(f"{q['id']}: no category")
        if len(q["question"]["de"]) < 5:
            errors.append(f"{q['id']}: question text is empty")
        if len(q["options"]) != 4 or any(not o["text"]["de"] for o in q["options"]):
            errors.append(f"{q['id']}: needs exactly 4 non-empty options")
        if q["correct_option"] not in OPTION_IDS:
            errors.append(f"{q['id']}: invalid correct option")
        for option in q["options"]:
            for lang, text in option["text"].items():
                # A gloss on only some options (usually the correct one) gives the answer away.
                if lang != "de" and "(" in text and "(" not in option["text"]["de"]:
                    errors.append(f"{q['id']}: {lang} option {option['id']} adds a parenthetical gloss")
        if all(o["text"]["de"] in "1234" for o in q["options"]) and not q.get("image"):
            errors.append(f"{q['id']}: answers refer to numbered pictures but there is no image")


def write(questions, images):
    content = json.dumps(questions, ensure_ascii=False, sort_keys=True)
    digest = hashlib.sha256(content.encode())
    for question_id in sorted(images):
        buffer = io.BytesIO()
        images[question_id].save(buffer, "PNG", optimize=True)
        digest.update(buffer.getvalue())
    catalog = {"catalogVersion": digest.hexdigest()[:16], "questions": questions}

    target = ASSETS / "questions.json"
    previous = load_json(target, {"catalogVersion": None, "questions": []})
    if previous["catalogVersion"] == catalog["catalogVersion"]:
        print("No changes.")
        return

    image_dir = ASSETS / "images"
    shutil.rmtree(image_dir, ignore_errors=True)
    image_dir.mkdir(parents=True)
    for question_id, image in images.items():
        image.save(image_dir / f"{question_id}.png", "PNG", optimize=True)
    target.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n")

    before = {q["id"]: q for q in previous["questions"]}
    after = {q["id"]: q for q in questions}
    added = sorted(after.keys() - before.keys(), key=sort_key)
    removed = sorted(before.keys() - after.keys(), key=sort_key)
    changed = sorted((i for i in after.keys() & before.keys() if after[i] != before[i]), key=sort_key)
    print(f"Wrote {len(questions)} questions, {len(images)} images, version {catalog['catalogVersion']}.")
    print(f"  added {len(added)}, removed {len(removed)}, changed {len(changed)}")
    for label, ids in (("removed", removed), ("changed", changed)):
        if ids:
            shown = " ".join(ids[:40]) + (f" … (+{len(ids) - 40})" if len(ids) > 40 else "")
            print(f"  {label}: {shown}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="validate only, do not write the asset")
    args = parser.parse_args()

    questions, images, errors, warnings = build()
    if warnings:
        missing = [w for w in warnings if "missing" in w]
        print(f"{len(warnings)} warnings ({len(missing)} missing translations)", file=sys.stderr)
        for warning in (w for w in warnings if "missing" not in w):
            print(f"  warning: {warning}", file=sys.stderr)
    if errors:
        for error in errors:
            print(f"  error: {error}", file=sys.stderr)
        sys.exit(1)
    if not args.check:
        write(questions, images)


if __name__ == "__main__":
    main()
