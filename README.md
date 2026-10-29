<div align="center">

# Just Einbürgerungstest

**Prepare for the German citizenship test – free, ad-free and fully offline.**

The open-source trainer for the *Einbürgerungstest* and the *Leben in Deutschland* test.<br>
No account, no ads, no tracking, not even an internet permission.

[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](LICENSE)
[![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#building)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](#tech-stack)
[![Offline](https://img.shields.io/badge/internet-none-success?style=flat-square)](PRIVACY.md)

[Features](#features) · [Privacy](#privacy) · [Building](#building) · [Question catalog](#question-catalog) · [Contributing](#contributing)

</div>

---

## Why another trainer?

Most citizenship test apps show video ads after a few questions, track you, or put basic features
behind a paywall. **Just Einbürgerungstest** does none of that. It ships the complete official catalog
on your device and teaches it with spaced repetition, in short daily sessions.

## Features

|  |  |
| --- | --- |
| 📚 **Complete catalog** | All 300 general questions plus the 10 questions for your federal state – 310 in total, chosen once on first launch. |
| ⏱️ **Daily 10** | Ten questions in about three minutes: due reviews and new questions mixed, with a retry round for mistakes. |
| 🗃️ **Leitner boxes** | Three boxes repeat mistakes right away and known questions less often. Practise any box straight from the home screen. |
| 🎯 **Targeted practice** | By topic (politics, history, society, your state), by mistakes or by bookmarks. |
| 📝 **Exam simulation** | 33 questions in 60 minutes, like the real test. Change answers and flag questions until you submit; past results are kept. |
| 🔥 **Progress** | Daily streak, answers over the last seven days and your next goal. |
| 🌍 **Translations** | Optional English and Ukrainian for questions and answers. The exam simulation stays German, like the real test. |
| 🎨 **Material You** | Material 3 with dynamic colour, dark mode and edge-to-edge layout. |

> [!NOTE]
> The app is in German, the language of the test. Only the German text is authoritative; the
> translations are learning aids.

## Privacy

> [!IMPORTANT]
> The app has **no internet permission** – the manifest even strips it if a library tries to add it.
> It contains no ads, no analytics and no crash reporting. Your progress never leaves your device.

Details: [PRIVACY.md](PRIVACY.md)

## Tech stack

- **Kotlin** and **Jetpack Compose** with **Material 3**
- **Clean Architecture + MVVM**, manual dependency injection, no Hilt/Koin
- **Room** for questions and progress, **DataStore** for settings
- **Navigation Compose** with type-safe routes
- Domain logic (Leitner scheduling, question selection, exam scoring) is plain Kotlin, unit-tested on the JVM

<details>
<summary><b>Project structure</b></summary>

```text
app/src/main/java/io/justpilot/justeinbuergerung/
├── data/      Room database, catalog import, DataStore settings, repositories
├── domain/    Models, repository interfaces, learning and exam logic (no Android types)
└── ui/        Compose screens and ViewModels: home, catalog, session, exam, settings, onboarding
tools/catalog/ Python pipeline that builds the question catalog from the BAMF portal
docs/          Product concept (German)
fastlane/      Store listings for F-Droid and Google Play
```

</details>

## Building

**Requirements:** a JDK 17+ to launch Gradle (the build JVM is provisioned automatically) and the
Android SDK. Android Studio works out of the box.

```bash
./gradlew assembleDebug          # debug APK
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (device or emulator)
./gradlew lint                   # Android lint
./gradlew assembleRelease        # release APK, shrunk with R8
```

<details>
<summary><b>Signing release builds</b></summary>

`assembleRelease` produces an unsigned APK, which is what F-Droid expects. To sign it yourself, create a
git-ignored `keystore.properties` next to `settings.gradle.kts`:

```properties
storeFile=/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

</details>

## Question catalog

The questions, answers and pictures come from the official catalog of test questions – Annex 1 of the
*Einbürgerungstestverordnung* – published by the Federal Office for Migration and Refugees (BAMF) on its
[practice portal](https://oet.bamf.de/ords/oetut/f?p=534:1::::::).

`app/src/main/assets/questions.json` is **generated** by the pipeline in [`tools/catalog/`](tools/catalog/)
(scraping, OCR, manual fixes, translations). Never edit it by hand – see the
[pipeline README](tools/catalog/README.md).

> [!CAUTION]
> This app is not affiliated with or endorsed by the BAMF or any other government agency.

## Contributing

Found a typo, an OCR mistake or a clumsy translation? Issues and pull requests are welcome.

1. Branch from `main` (`fix/…`, `feat/…`, `docs/…`) and use [Conventional Commits](https://www.conventionalcommits.org/).
2. Add or update tests with every change – `main` always stays green.
3. Fix catalog data in `tools/catalog/` (overrides, OCR fixes, translations) and regenerate the asset.

## License

The source code is licensed under the [Apache License 2.0](LICENSE). See [NOTICE](NOTICE) for the
question catalog and third-party software.
