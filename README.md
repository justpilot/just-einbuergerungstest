# Just Einbürgerungstest

A free, ad-free and fully offline Android trainer for the German citizenship test
(*Einbürgerungstest*) and the *Leben in Deutschland* test.

- All 300 general questions plus the 10 questions for your federal state, stored on the device
- No internet permission, no tracking, no ads
- Optional English and Ukrainian translations of questions and answers
- Spaced repetition (Leitner system), daily 10-question sessions and an exam simulation

See [docs/concept.md](docs/concept.md) for the full concept (German).

## Building

```bash
./gradlew assembleDebug
```

## Question catalog

The questions come from the official BAMF catalog and are generated with the pipeline in
[`tools/catalog/`](tools/catalog/).

## License

[Apache License 2.0](LICENSE)
