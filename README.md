# Dict — Offline English Dictionary & Search Engine

A simple, minimal, blazing fast offline English dictionary and thesaurus for Android that doubles as a native system-wide search engine.

---

## Highlights

- **100% Offline**: Zero internet permissions requested (`android.permission.INTERNET` is omitted entirely).
- **Deep Android Integration**:
  - **System Web Search (`WEB_SEARCH` / `SEARCH`)**: Appears alongside Google, Chrome, and Firefox when any app or widget initiates a search.
  - **Instant Context Menu (`PROCESS_TEXT`)**: Highlight text anywhere on Android (browser, messaging app, PDF reader) and tap "Offline English Dictionary" for an instant floating definition sheet without leaving your app.
  - **Digital Assistant (`ASSIST`)**: Can be chosen as default Assistant in Android Settings.
- **388,000+ Definitions & Thesaurus**:
  - Combines 1913 Webster's Unabridged Dictionary with Open English WordNet (2025+).
  - Integrated thesaurus: word senses link to interactive synonym chips that query related words with a single tap.
- **Ultra-Fast Performance**:
  - Indexed B-tree prefix lookup in under **0.06 ms**.
  - Built with native Views and `RecyclerView` for a locked 120Hz refresh rate.
  - Initial cold launch and database extraction complete in **< 300 ms**.
- **Offline Audio Pronunciation**: Built-in `TextToSpeech` engine for offline pronunciation.
- **History & Bookmarks**: Full SQLite-backed search history and starred words.

---

## Screenshots

| Floating Search Overlay | Autocomplete Search | Definition & Thesaurus |
| :---: | :---: | :---: |
| Floating sheet over host apps | Instant live suggestions as you type | Senses, examples, & clickable synonym chips |

---

## Architecture

- **Language**: Kotlin 1.9
- **Target SDK**: Android 15 (API 35), Min SDK 26 (Android 8.0+)
- **UI Framework**: Native Views + Material Components 3 (ViewBinding, zero Compose cold-start overhead)
- **Database**: Android's native `android.database.sqlite.SQLiteDatabase` with B-tree index on `word_lower`.
- **Dataset Compiler**: `scripts/build_dictionary.py` processes raw JSONL/synsets into the compressed asset database.

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/madhuraj0/dict.git
cd dict

# Build debug APK
./gradlew assembleDebug

# Install on connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## License

Dictionary data derived from [Open English WordNet](https://github.com/globalwordnet/english-wordnet) (CC BY 4.0) and Project Gutenberg (Public Domain). Code released under the MIT License.
