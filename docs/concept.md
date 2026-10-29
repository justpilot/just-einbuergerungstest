# Projektplanung: Just Einbürgerungstest

**App-Typ:** Native Android App
**Lizenz:** Open Source (Apache 2.0)
**Monetarisierung:** Komplett kostenlos, 100 % werbefrei, kein Tracking
**Vertrieb:** F-Droid & Google Play Store
**Zielgruppe:** Personen in Vorbereitung auf den Einbürgerungstest bzw. den Test „Leben in Deutschland“ (LiD)

---

## 1. Executive Summary & Vision

Aktuelle Prüfungstrainer im Google Play Store sind überwiegend von aggressiver Werbung (Video-Ads nach wenigen Fragen), Tracking-Diensten und Paywalls für grundlegende Funktionen geprägt.

**Just Einbürgerungstest** verfolgt einen radikal nutzerzentrierten Ansatz:
* **Fokus & Ruhe:** Modernes, reduziertes Material-3-Design ohne visuellen Lärm.
* **Privatsphäre & Offline-First:** Der gesamte Fragenkatalog (300 bundesweite Fragen + je 10 landesspezifische Fragen für alle 16 Bundesländer) liegt vollständig lokal auf dem Gerät. Null Internet-Berechtigungen (`android.permission.INTERNET` wird im Manifest gar nicht angefordert).
* **Didaktische Unterstützung:** Optionale, zuschaltbare Übersetzungen (Englisch, Ukrainisch, Türkisch) direkt für Fragen und Antworten, um Sprachbarrieren bei behördlichen Formulierungen abzubauen.
* **Intelligente Wiederholung:** Spaced-Repetition-System (3 Kästen), das Fehler automatisch priorisiert und beherrschte Inhalte seltener abfragt.

---

## 2. Technische Rahmenbedingungen & Spezifikationen

| Parameter | Wert / Spezifikation | Begründung |
| :--- | :--- | :--- |
| **Plattform** | Android (Native) | Volle Kontrolle über Performance und UX |
| **Programmiersprache** | Kotlin | Moderner Android-Standard |
| **UI-Framework** | Jetpack Compose + Material 3 | Deklarative UI, native Dynamic-Color-Unterstützung |
| **Min SDK** | **31 (Android 12)** | Schneidet Legacy-Altlasten ab (~90 % Marktabdeckung); sauberes Dynamic Theming, moderne Gesten und Haptic Feedback |
| **Target SDK** | **37** | Über den aktuellen Play-Store-Vorgaben, vollflächiges Edge-to-Edge Design |
| **Lokale Persistenz** | Room SQLite + DataStore | SQLite für Fragen & Lernfortschritt; DataStore für Benutzereinstellungen |
| **Architektur** | Clean Architecture + MVVM | Klare Trennung von Datenmodell, Domain-Logik und UI |

---

## 3. Funktionsumfang (Feature-Spezifikation)

### Phase 1: MVP (Minimum Viable Product)

#### A. Konfiguration & Lokalisierung
* **Bundesland-Filter:**
  * Einmalige Auswahl beim Erststart (z. B. Rheinland-Pfalz `RP`, Bayern `BY` etc.).
  * Der aktive Fragenpool wird präzise auf 310 Fragen beschränkt (300 allgemeine + 10 Landesfragen).
  * Das Bundesland kann jederzeit in den Einstellungen geändert werden.
* **Mehrsprachigkeit (AI-Assist):**
  * Standardsprache: Deutsch (gemäß den Prüfungsanforderungen).
  * Dezent platziertes Icon `[🌐]` auf dem Frage-Screen.
  * Beim Antippen klappen simultan die Übersetzungen für die Frage sowie für alle 4 Antwortoptionen aus.
  * Unterstützte Sprachen im Release 1: **Englisch (`en`)**, **Ukrainisch (`uk`)** und **Türkisch (`tr`)**.

#### B. Lernmodi & Didaktik
* **Tages-Session („Daily 10“):**
  * 10 Fragen pro Durchgang (fokussierte Mischung aus 6 fälligen Wiederholungen und 4 neuen Fragen).
  * Dauer: ca. 3 Minuten (ideal für unterwegs).
  * **Sofort-Feedback:** Unmittelbare optische (Grün/Rot) und sanfte haptische Rückmeldung nach Klick auf eine Antwort.
  * **Session-Fehlerkorrektur (End-of-Session Retry):** Falsch beantwortete Fragen aus dem aktuellen 10er-Block werden am Ende nochmals abgefragt, bis alle Fehler positiv aufgelöst sind.
* **Spaced Repetition (3-Kasten-System nach Leitner):**
  * Kasten 1: Neu oder falsch beantwortet (höchste Abfragefrequenz, sofort fällig).
  * Kasten 2: Wiederholungsstufe (fällig nach 2 Tagen).
  * Kasten 3: Gemeistert (Wiederholung alle 7 Tage).
  * Drei Kästen genügen, weil der Fragenpool fest und öffentlich ist und der Lernhorizont bis zum Prüfungstermin reicht: Abdeckung aller 310 Fragen zählt mehr als jahrelanges Behalten.
  * Falsche Antworten stufen eine Frage sofort zurück in Kasten 1.
* **Echte Prüfungssimulation:**
  * Exakt 33 Fragen (30 allgemeine + 3 Landesfragen).
  * 60-Minuten-Countdown-Timer.
  * Echte Prüfungsatmosphäre: Keine Übersetzungen während des Tests, keine Sofort-Auflösung, Fragen können zur Wiedervorlage markiert werden.
  * Detaillierte Auswertung nach Abgabe:
    * Bestanden für „Leben in Deutschland“ (LiD): ab 15 Punkten.
    * Bestanden für den Einbürgerungstest: ab 17 Punkten.
    * Liste aller falsch beantworteten Fragen zur gezielten Nacharbeit.

#### C. Fortschritts- & Motivations-Tracking
* **Aktivitäts-Statistik (Letzte 7 Tage):**
  * Minimalistisches Balkendiagramm auf dem Dashboard: die letzten 7 Tage bis heute, beschriftet mit Wochentagen.
  * Zeigt beantwortete Fragen pro Tag an.
* **Streak-Zähler:**
  * Zählt aufeinanderfolgende Tage mit mindestens einer beendeten Tages-Session (`🔥 Streak`).
* **Fehlerkasten & Merkliste:**
  * Direkter Zugriff auf alle aktuell in Kasten 1 verbleibenden Fragen.
  * Manuelle Lesezeichen-Funktion (`[🔖]`) für individuelle Wiederholungen.

---

## 4. UI- & UX-Architektur

Die Benutzeroberfläche verzichtet auf laute Gamification-Elemente und setzt auf Ruhe und Konzentration:

### Screen-Aufteilung (Bottom Navigation)
1. **Home / Dashboard:**
   * Tages-Session CTA (`[ ▶ 10 Fragen starten ]`).
   * Streak-Status und 7-Tage-Aktivitätsdiagramm.
   * Direkter Einstieg in Prüfungssimulation oder Problemfälle.
2. **Katalog & Themen:**
   * Gliederung nach den drei offiziellen BAMF-Themenfeldern plus Landesfragen:
     1. *Politik in der Demokratie*
     2. *Geschichte und Verantwortung*
     3. *Mensch und Gesellschaft*
     4. *Mein Bundesland* (die 10 Landesfragen)
3. **Prüfung:**
   * Historie bisheriger Simulationsergebnisse (z. B. Punktestand-Entwicklung).
   * Startknopf für eine neue 60-minütige Simulation.
4. **Einstellungen:**
   * Bundesland festlegen.
   * Übersetzungssprache auswählen (`en` / `uk` / `tr`).
   * Haptisches Feedback aktivieren/deaktivieren.
   * Darstellung: Hell/Dunkel/System sowie Material You (Dynamic Color) oder eigene App-Farben.
   * Daten-Reset (Lernfortschritt zurücksetzen).

---

## 5. Datenmodell (JSON-Asset-Spezifikation)

Quelle ist der offizielle BAMF-Fragenkatalog (<https://oet.bamf.de/ords/oetut/f?p=534:1::::::>). Er wird mit der Pipeline in `tools/catalog/` abgerufen und nach `app/src/main/assets/questions.json` geschrieben; Bilder liegen unter `app/src/main/assets/images/`.

Das Asset enthält **alle 460 Fragen** (300 allgemeine + 16 × 10 Landesfragen); die App filtert nach dem gewählten Bundesland. Fragen-IDs sind Strings: `"1"`–`"300"` für allgemeine Fragen, `"<Land>-<n>"` (z. B. `"BY-1"`–`"BY-10"`) für Landesfragen.

```json
{
  "catalogVersion": "<content hash>",
  "questions": [
    {
      "id": "1",
      "number": 1,
      "state": null,
      "category": "politics",
      "image": null,
      "question": {
        "de": "In Deutschland dürfen Menschen offen etwas gegen die Regierung sagen, weil …",
        "en": "In Germany, people are allowed to speak out openly against the government because …",
        "uk": "У Німеччині люди можуть відкрито висловлюватися проти уряду, тому що …"
      },
      "options": [
        { "id": "A", "text": { "de": "hier Religionsfreiheit gilt.", "en": "…", "uk": "…" } },
        { "id": "B", "text": { "de": "die Menschen Steuern zahlen.", "en": "…", "uk": "…" } },
        { "id": "C", "text": { "de": "die Menschen das Wahlrecht haben.", "en": "…", "uk": "…" } },
        { "id": "D", "text": { "de": "hier Meinungsfreiheit gilt.", "en": "…", "uk": "…" } }
      ],
      "correct_option": "D"
    }
  ]
}
```

`category` ist einer von `politics`, `history`, `society`, `state`. `image` ist ein Pfad relativ zu `assets/` (z. B. `"images/21.png"`) oder `null`.

---

## 6. Meilensteine & Roadmap

| Phase | Ziel / Meilenstein | Deliverables |
| :--- | :--- | :--- |
| **M1: Datenaufbereitung** | JSON-Katalog fertigstellen | Wiederholbare Pipeline (Abruf vom BAMF-Portal, OCR der Fragetexte, Bilder); alle 300 allgemeinen + 160 Landesfragen; KI-gestützte Übersetzung ins Englische und Ukrainische; Validierung der Lösungsbuchstaben. |
| **M2: Core & Database** | Datenbank & Repositories | Room-Entities (`Question`, `Option`, `Progress`, `AnswerLog`), Import aus dem JSON-Asset (versioniert, Fortschritt bleibt erhalten), DataStore für Settings, Leitner-Logik. |
| **M3: UI & Lernfluss** | Question Screen & Daily 10 | Material 3 Theme, Zweisprachige Antwortkarten mit Accordion-Toggle, Haptik, Session-Retry-Logik. |
| **M4: Simulation & Stats** | Prüfung & Dashboard | 60-Minuten-Countdown-Timer, 33-Fragen-Zufallsgenerator (30+3), 7-Tage-Balkendiagramm, Kasten-Verwaltung. |
| **M5: Polish & Release** | Open-Source-Vorbereitung | F-Droid Metadata, Lizenzdateien, Readme, Google Play Store Assets; Klärung der Nutzungsrechte an BAMF-Texten und -Bildern. |
