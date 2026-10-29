#!/usr/bin/env python3
"""Maintain translations/<lang>.json.

    python translations.py export en todo.json
        Writes the German text of every question whose `en` translation is missing or stale
        (German text changed since it was translated) as [{id, question, options}].

    python translations.py import en done.json
        Merges translated {id: {question, options[4]}} into translations/en.json and stamps each
        entry with the hash of the current German text.

Run build.py afterwards to put the translations into the app asset.
"""

import argparse
import json
import sys

import build

TRANSLATIONS = build.HERE / "translations"


def german_catalog():
    """The current German catalog, keyed by id, exactly as build.py produces it."""
    questions, _, errors, _ = build.build()
    if errors:
        sys.exit("catalog has validation errors, run build.py --check first")
    return {q["id"]: q for q in questions}


def load(lang):
    return build.load_json(TRANSLATIONS / f"{lang}.json", {})


def save(lang, translations):
    TRANSLATIONS.mkdir(exist_ok=True)
    ordered = dict(sorted(translations.items(), key=lambda item: build.sort_key(item[0])))
    (TRANSLATIONS / f"{lang}.json").write_text(json.dumps(ordered, ensure_ascii=False, indent=1) + "\n")


def todo(catalog, translations):
    """Questions without an up-to-date translation."""
    return [
        {"id": q["id"], "question": q["question"]["de"], "options": [o["text"]["de"] for o in q["options"]]}
        for q in catalog.values()
        if translations.get(q["id"], {}).get("source") != build.source_hash(q)
    ]


def merge(catalog, translations, new):
    """Add `new` translations stamped with the current source hash. Returns the merged dict."""
    merged = dict(translations)
    for question_id, entry in new.items():
        if question_id not in catalog:
            raise ValueError(f"unknown question id {question_id}")
        if len(entry["options"]) != 4 or not entry["question"].strip() or not all(o.strip() for o in entry["options"]):
            raise ValueError(f"{question_id}: needs a question and exactly 4 non-empty options")
        merged[question_id] = {
            "source": build.source_hash(catalog[question_id]),
            "question": entry["question"],
            "options": entry["options"],
        }
    return merged


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", choices=["export", "import"])
    parser.add_argument("lang", choices=build.LANGUAGES)
    parser.add_argument("file")
    args = parser.parse_args()

    catalog = german_catalog()
    translations = load(args.lang)
    if args.command == "export":
        items = todo(catalog, translations)
        with open(args.file, "w") as file:
            json.dump(items, file, ensure_ascii=False, indent=1)
        print(f"{len(items)} questions need a {args.lang} translation")
    else:
        with open(args.file) as file:
            new = json.load(file)
        save(args.lang, merge(catalog, translations, new))
        print(f"imported {len(new)} {args.lang} translations")


if __name__ == "__main__":
    main()
