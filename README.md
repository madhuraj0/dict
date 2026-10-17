<p align="center">
  <img src="docs/icon.png" width="128" height="128" alt="Dict Icon">
</p>

<h1 align="center">Dict</h1>

<p align="center">
  Fast, offline English collegiate dictionary for Android with system search and text-selection lookup.
</p>

<p align="center">
  <a href="https://github.com/madhuraj0/dict/releases/latest">
    <img src="docs/get-it-on-github.png" width="190" alt="Get it on GitHub">
  </a>
</p>

---

## Features

- **Offline**: 380,000+ words, definitions, etymologies, examples, and synonyms (indexed SQLite, <1ms lookups).
- **System "Look up"**: Select text anywhere on Android & tap **Look up** for a floating sheet.
- **Search Provider**: Integrates with Android global search and browser query intents.
- **Pronunciation**: Human audio streaming with automatic offline TTS fallback.
- **Hindi Badge**: Collegiate Hindi translations matching the active theme accent.
- **Themes**: System, Light, Dark, and true `#000000` AMOLED Black.
- **Accents**: Amber Gold, Oxford Blue, Emerald, Crimson, and Monochrome (B&W).
- **Bookmarks & History**: Fast local SQLite storage. Zero trackers, zero ads.

---

## Build

```bash
git clone https://github.com/madhuraj0/dict.git
cd dict
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## License

- Code: [MIT](LICENSE)
- Dictionary data: [Wiktionary](https://en.wiktionary.org/) ([CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/))
