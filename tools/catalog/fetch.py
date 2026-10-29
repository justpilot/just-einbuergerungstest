#!/usr/bin/env python3
"""Download the official BAMF "Leben in Deutschland" question catalog.

Walks the public practice portal (an Oracle APEX app) and stores every question
in raw/<id>.json plus its question image (if any) in raw/<id>.png. Many questions
publish their text only inside the image; build.py OCRs those.

Questions 1-300 are the same for every federal state and are fetched once;
301-310 are fetched for each of the 16 states and stored as "<STATE>-<n>".

Usage:
    python fetch.py                   # everything
    python fetch.py --state BY        # only the state questions of Bavaria
    python fetch.py --only 21 55 BY-3 # selected questions
"""

import argparse
import html
import http.cookiejar
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://oet.bamf.de/ords/oetut/"
START = BASE + "f?p=534:1::::::"
USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:140.0) Gecko/20100101 Firefox/140.0"
RAW = Path(__file__).parent / "raw"
GENERAL_COUNT = 300
STATE_COUNT = 10
DELAY = 0.5

# Portal state name -> code used in question ids.
STATES = {
    "Baden-Württemberg": "BW",
    "Bayern": "BY",
    "Berlin": "BE",
    "Brandenburg": "BB",
    "Bremen": "HB",
    "Hamburg": "HH",
    "Hessen": "HE",
    "Mecklenburg-Vorpommern": "MV",
    "Niedersachsen": "NI",
    "Nordrhein-Westfalen": "NW",
    "Rheinland-Pfalz": "RP",
    "Saarland": "SL",
    "Sachsen": "SN",
    "Sachsen-Anhalt": "ST",
    "Schleswig-Holstein": "SH",
    "Thüringen": "TH",
}


class Portal:
    """One APEX session; every navigation is a form submit of the current page."""

    def __init__(self):
        jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
        self.opener.addheaders = [("User-Agent", USER_AGENT), ("Accept-Language", "de-DE,de;q=0.9")]
        self.page = None
        self.page_id = None

    def _open(self, url, data=None):
        for attempt in range(4):
            try:
                with self.opener.open(url, data, timeout=60) as response:
                    return response.read()
            except OSError as error:
                if attempt == 3:
                    raise
                print(f"  retry after {error}", file=sys.stderr)
                time.sleep(2 ** attempt)

    def _hidden(self, name):
        return re.search(rf'name="{name}" value="([^"]*)"', self.page).group(1)

    def _submit(self, items, request):
        protected = html.unescape(re.search(r'id="pPageItemsProtected" value="([^"]*)"', self.page).group(1))
        salt = self._hidden("p_page_submission_id")
        instance = self._hidden("p_instance")
        form = {
            "p_flow_id": "534",
            "p_flow_step_id": str(self.page_id),
            "p_instance": instance,
            "p_page_submission_id": salt,
            "p_request": request,
            "p_reload_on_submit": "A",
            "p_json": json.dumps({
                "pageItems": {
                    "itemsToSubmit": [{"n": name, "v": value} for name, value in items.items()],
                    "protected": protected,
                    "rowVersion": "",
                    "formRegionChecksums": [],
                },
                "salt": salt,
            }),
        }
        url = f"{BASE}wwv_flow.accept?p_context=534:{self.page_id}:{instance}"
        self.page = self._open(url, urllib.parse.urlencode(form).encode()).decode()
        self.page_id = int(re.search(r'name="p_flow_step_id" value="(\d+)"', self.page).group(1))
        time.sleep(DELAY)

    def start(self, state_name):
        self.page = self._open(START).decode()
        self.page_id = 1
        options = dict((name.strip(), value) for value, name in
                       re.findall(r'<option value="(\d+)"[^>]*>([^<]+)', self.page))
        self._submit({"P1_BUL_ID": options[state_name]}, "SUBMIT")
        if self.page_id != 30:
            raise RuntimeError(f"expected question page, got page {self.page_id}")

    def catalog(self):
        return parse_catalog(self.page)

    def question(self, portal_id):
        self._submit({"P30_ROWNUM": portal_id}, "P30_ROWNUM")
        data, image_src = parse_question(self.page, portal_id)
        image = None
        if image_src:
            image = self._open(BASE + image_src)
            time.sleep(DELAY)
        return data, image


def parse_catalog(page):
    """Question number -> portal question id, from the "go to question" select."""
    select = re.search(r'id="P30_ROWNUM".*?</select>', page, re.S).group(0)
    return {int(number): value for value, number in re.findall(r'<option value="(\d+)"[^>]*>(\d+)', select)}


def parse_question(page, portal_id):
    """Extract one question from page 30. Returns (data, image URL relative to BASE or None)."""
    header = re.search(r"Aufgabe (\d+) von (\d+)", page)
    text = re.search(r'id="P30_AUFGABENSTELLUNG"[^>]*>(.*?)</span>', page, re.S)
    image_src = re.search(r'<img\s+src="(f\?p=534:30::APPLICATION_PROCESS[^"]*)"', page)
    if not header:
        raise RuntimeError(f"question {portal_id}: not a question page")
    if not text and not image_src:
        raise RuntimeError(f"question {portal_id} has neither text nor image")
    if image_src:
        image_src = html.unescape(image_src.group(1))
        if not image_src.endswith("," + portal_id):
            raise RuntimeError(f"image {image_src} does not belong to question {portal_id}")
    rows = re.findall(r'<tr>\s*<td headers="RICHTIGE_ANTWORT"(.*?)</tr>', page, re.S)
    answers = [_text(re.search(r'headers="ANTWORT"[^>]*>(.*?)</td>', row, re.S).group(1)) for row in rows]
    correct = [i for i, row in enumerate(rows) if 'id="FARBE"' in row]
    code = re.search(r'id="P30_BESCHREIBUNG"[^>]*>(.*?)</span>', page, re.S)
    return {
        "number": int(header.group(1)),
        "portalId": portal_id,
        "code": _text(code.group(1)) if code else None,
        "text": _text(text.group(1)) if text else None,
        "answers": answers,
        "correct": correct[0] if len(correct) == 1 else correct,
    }, image_src or None


def _text(fragment):
    fragment = re.sub(r"<br\s*/?>", " ", fragment)
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", fragment))).strip()


def save(question_id, state, data, image):
    data = {"id": question_id, "state": state, **data}
    if len(data["answers"]) != 4 or not isinstance(data["correct"], int):
        raise RuntimeError(f"unexpected answer layout for {question_id}: {data}")
    png = RAW / f"{question_id}.png"
    if image:
        png.write_bytes(image)
    else:
        png.unlink(missing_ok=True)
    (RAW / f"{question_id}.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    print(f"{question_id:>6}  {data['code']}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--state", choices=sorted(STATES.values()), help="only fetch this state's questions")
    parser.add_argument("--only", nargs="+", metavar="ID", help="only fetch these question ids")
    args = parser.parse_args()
    RAW.mkdir(exist_ok=True)

    only = set(args.only or [])
    names = {code: name for name, code in STATES.items()}
    general_ids = None

    for code, name in names.items():
        if args.state and code != args.state:
            continue
        wanted_general = general_ids is None and not args.state and (not only or any(i.isdigit() for i in only))
        wanted_state = [n for n in range(1, STATE_COUNT + 1) if not only or f"{code}-{n}" in only]
        if not wanted_general and not wanted_state:
            continue

        portal = Portal()
        portal.start(name)
        catalog = portal.catalog()
        if len(catalog) != GENERAL_COUNT + STATE_COUNT:
            raise RuntimeError(f"{code}: expected {GENERAL_COUNT + STATE_COUNT} questions, portal lists {len(catalog)}")
        ids = [catalog[n] for n in range(1, GENERAL_COUNT + 1)]
        if general_ids is not None and ids != general_ids:
            raise RuntimeError(f"{code}: general questions differ from the other states")

        if wanted_general:
            general_ids = ids
            for number in range(1, GENERAL_COUNT + 1):
                if not only or str(number) in only:
                    data, image = portal.question(catalog[number])
                    save(str(number), None, data, image)
        general_ids = general_ids or ids

        for n in wanted_state:
            data, image = portal.question(catalog[GENERAL_COUNT + n])
            save(f"{code}-{n}", code, data, image)


if __name__ == "__main__":
    main()
