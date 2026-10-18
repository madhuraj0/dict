package madhuraj0.dict.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import madhuraj0.dict.data.model.WordEntry
import madhuraj0.dict.data.model.WordSense
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

class DictionaryDatabaseHelper(private val context: Context) {

    companion object {
        private const val TAG = "DictDbHelper"
        private const val DB_NAME = "dictionary.db"
        private const val ASSET_NAME = "dictionary.db"
        private const val CURRENT_DB_VERSION = 4
        private const val PREF_KEY_DB_VERSION = "db_version"
        private const val PREFS_NAME = "dict_prefs"

        private val IRREGULAR_LEMMAS = mapOf(
            "does" to "do", "goes" to "go", "has" to "have", "had" to "have",
            "went" to "go", "done" to "do", "gone" to "go", "been" to "be",
            "were" to "be", "was" to "be",
            "men" to "man", "women" to "woman", "children" to "child",
            "feet" to "foot", "teeth" to "tooth", "mice" to "mouse",
            "better" to "good", "best" to "good", "worse" to "bad", "worst" to "bad"
        )

        private val BLOCKED_STEMS = setOf(
            "news", "odds"
        )

        fun getStemCandidates(word: String): List<String> {
            val clean = word.trim().lowercase()
            if (clean.length < 4 || clean in BLOCKED_STEMS) return emptyList()

            val irregular = IRREGULAR_LEMMAS[clean]
            if (irregular != null) {
                return listOf(irregular)
            }

            val candidates = mutableListOf<String>()

            // 1. -ies -> -y (parties -> party, flies -> fly)
            if (clean.endsWith("ies") && clean.length >= 5) {
                val fourthFromEnd = clean[clean.length - 4]
                if (fourthFromEnd !in "aeiou") {
                    candidates.add(clean.dropLast(3) + "y")
                }
            }

            // 2. -ied -> -y or -i (carried -> carry, skied -> ski)
            if (clean.endsWith("ied") && clean.length >= 5) {
                candidates.add(clean.dropLast(3) + "i")
                val fourthFromEnd = clean[clean.length - 4]
                if (fourthFromEnd !in "aeiou") {
                    candidates.add(clean.dropLast(3) + "y")
                }
            }

            // 3. -ing (running -> run, walking -> walk, writing -> write)
            if (clean.endsWith("ing") && clean.length >= 5) {
                val base = clean.dropLast(3)
                if (base.length >= 3 && base[base.length - 1] == base[base.length - 2] && base.last() !in "aeiouy") {
                    candidates.add(base.dropLast(1))
                }
                candidates.add(base + "e")
                candidates.add(base)
            }

            // 4. -ed (stopped -> stop, walked -> walk, danced -> dance)
            if (clean.endsWith("ed") && clean.length >= 5) {
                val base = clean.dropLast(2)
                if (base.length >= 3 && base[base.length - 1] == base[base.length - 2] && base.last() !in "aeiouy") {
                    candidates.add(base.dropLast(1))
                }
                candidates.add(clean.dropLast(1))
                candidates.add(base)
            }

            // 5. Plural -es: in English, only after sibilants (s, x, z, ch, sh) or consonant + o
            if (clean.endsWith("es") && clean.length >= 5) {
                val base = clean.dropLast(2)
                if (base.endsWith("ss") || base.endsWith("sh") || base.endsWith("ch") ||
                    base.endsWith("x") || base.endsWith("z") || base.endsWith("s") ||
                    (base.length >= 2 && base.endsWith("o") && base[base.length - 2] !in "aeiou")
                ) {
                    candidates.add(base)
                }
            }

            // 6. Plural / 3rd-person -s (dogs -> dog, books -> book, dances -> dance)
            if (clean.endsWith("s") && !clean.endsWith("ss") && clean.length >= 4) {
                candidates.add(clean.dropLast(1))
            }

            return candidates
        }

        @Volatile
        private var instance: DictionaryDatabaseHelper? = null

        fun getInstance(context: Context): DictionaryDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DictionaryDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dbFile: File get() = File(context.filesDir, DB_NAME)
    private var database: SQLiteDatabase? = null

    @Synchronized
    fun getDatabase(): SQLiteDatabase {
        database?.let { if (it.isOpen) return it }
        ensureDatabaseExtracted()
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        database = db
        return db
    }

    @Synchronized
    fun isDatabaseReady(): Boolean {
        return dbFile.exists() && database?.isOpen == true
    }

    @Synchronized
    fun ensureDatabaseExtracted() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val installedVersion = prefs.getInt(PREF_KEY_DB_VERSION, 0)

        if (dbFile.exists() && installedVersion == CURRENT_DB_VERSION) {
            return
        }

        Log.i(TAG, "Extracting dictionary database from assets...")
        val tempFile = File(context.filesDir, "$DB_NAME.tmp")
        if (tempFile.exists()) tempFile.delete()

        try {
            val rawAssetStream = try {
                context.assets.open(ASSET_NAME)
            } catch (_: Exception) {
                context.assets.open("$ASSET_NAME.gz")
            }

            val pushback = java.io.PushbackInputStream(rawAssetStream, 2)
            val header = ByteArray(2)
            val bytesRead = pushback.read(header)
            if (bytesRead > 0) {
                pushback.unread(header, 0, bytesRead)
            }

            val inputStream = if (bytesRead >= 2 && header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte()) {
                GZIPInputStream(pushback)
            } else {
                pushback
            }

            inputStream.use { inStream ->
                FileOutputStream(tempFile).use { outStream ->
                    inStream.copyTo(outStream, 65536)
                    outStream.flush()
                }
            }

            if (dbFile.exists()) dbFile.delete()
            if (!tempFile.renameTo(dbFile)) {
                throw IllegalStateException("Failed to rename extracted DB from $tempFile to $dbFile")
            }

            prefs.edit().putInt(PREF_KEY_DB_VERSION, CURRENT_DB_VERSION).apply()
            Log.i(TAG, "Dictionary database extracted successfully: ${dbFile.length() / (1024 * 1024)} MB")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting database", e)
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    fun searchExact(query: String): WordEntry? {
        val cleanWord = query.trim()
        if (cleanWord.isEmpty()) return null
        val lower = cleanWord.lowercase()

        val db = getDatabase()
        val senses = mutableListOf<WordSense>()
        var foundWord = cleanWord

        val cursor = db.rawQuery(
            "SELECT word, pos, definition, examples, synonyms FROM entries WHERE word_lower = ? ORDER BY id ASC",
            arrayOf(lower)
        )

        cursor.use { c ->
            val wordIdx = c.getColumnIndexOrThrow("word")
            val posIdx = c.getColumnIndexOrThrow("pos")
            val defIdx = c.getColumnIndexOrThrow("definition")
            val exIdx = c.getColumnIndexOrThrow("examples")
            val synIdx = c.getColumnIndexOrThrow("synonyms")

            while (c.moveToNext()) {
                if (senses.isEmpty()) {
                    foundWord = c.getString(wordIdx)
                }
                val pos = c.getString(posIdx)
                val def = c.getString(defIdx)
                val examplesJson = c.getString(exIdx)
                val synonymsJson = c.getString(synIdx)

                val examplesList = parseJsonStringList(examplesJson)
                val synonymsList = parseJsonStringList(synonymsJson)

                senses.add(WordSense(pos = pos, definition = def, examples = examplesList, synonyms = synonymsList))
            }
        }

        val ipa = try {
            db.rawQuery(
                "SELECT ipa FROM pronunciations WHERE word_lower = ? LIMIT 1",
                arrayOf(lower)
            ).use { pc ->
                if (pc.moveToFirst()) pc.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }

        val translations = getTranslationsForWord(lower)

        return if (senses.isNotEmpty()) WordEntry(word = foundWord, ipa = ipa, translations = translations, senses = senses) else null
    }

    fun getTranslationsForWord(word: String): Map<String, String> {
        val lower = word.trim().lowercase()
        if (lower.isEmpty()) return emptyMap()
        val db = getDatabase()

        // 1. Direct lookup in translations table
        val direct = queryTranslationsDirect(db, lower)
        if (direct.isNotEmpty()) return direct

        // 2. Conservative lemma fallback for verbs, plurals & common irregulars
        val candidates = getStemCandidates(lower)
        for (stem in candidates) {
            val stemMap = queryTranslationsDirect(db, stem)
            if (stemMap.isNotEmpty()) {
                val result = stemMap.toMutableMap()
                result["root"] = stem
                return result
            }
        }
        return emptyMap()
    }

    private fun queryTranslationsDirect(db: SQLiteDatabase, wordLower: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            db.rawQuery(
                "SELECT hi, es, fr FROM translations WHERE word_lower = ? LIMIT 1",
                arrayOf(wordLower)
            ).use { tc ->
                if (tc.moveToFirst()) {
                    val hi = if (!tc.isNull(0)) tc.getString(0) else null
                    val es = if (!tc.isNull(1)) tc.getString(1) else null
                    val fr = if (!tc.isNull(2)) tc.getString(2) else null
                    if (!hi.isNullOrBlank()) map["hi"] = hi
                    if (!es.isNullOrBlank()) map["es"] = es
                    if (!fr.isNullOrBlank()) map["fr"] = fr
                }
            }
        } catch (_: Exception) {
            // translations table optional/fallback
        }
        return map
    }

    fun getIpaForWord(word: String): String? {
        val lower = word.trim().lowercase()
        if (lower.isEmpty()) return null
        val db = getDatabase()
        return try {
            db.rawQuery(
                "SELECT ipa FROM pronunciations WHERE word_lower = ? LIMIT 1",
                arrayOf(lower)
            ).use { pc ->
                if (pc.moveToFirst()) pc.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun searchPrefix(prefix: String, limit: Int = 30): List<String> {
        val clean = prefix.trim().lowercase()
        if (clean.isEmpty()) return emptyList()

        val upper = clean + "\uffff"
        val db = getDatabase()
        val results = mutableListOf<String>()

        val cursor = db.rawQuery(
            "SELECT DISTINCT word FROM entries WHERE word_lower >= ? AND word_lower < ? LIMIT ?",
            arrayOf(clean, upper, limit.toString())
        )

        cursor.use { c ->
            val wordIdx = c.getColumnIndexOrThrow("word")
            while (c.moveToNext()) {
                results.add(c.getString(wordIdx))
            }
        }
        return results
    }

    fun getRandomWord(): String? {
        val db = getDatabase()
        val cursor = db.rawQuery(
            "SELECT word FROM entries WHERE id = (ABS(RANDOM()) % (SELECT COALESCE(MAX(id), 1) FROM entries) + 1) LIMIT 1",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return null
    }

    fun getHistory(limit: Int = 30): List<String> {
        val db = getDatabase()
        val list = mutableListOf<String>()
        val cursor = db.rawQuery(
            "SELECT word FROM history ORDER BY timestamp DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(c.getString(0))
            }
        }
        return list
    }

    fun addHistory(word: String) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        val db = getDatabase()
        val values = ContentValues().apply {
            put("word", clean)
            put("timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict("history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clearHistory() {
        val db = getDatabase()
        db.delete("history", null, null)
    }

    fun getBookmarks(): List<String> {
        val db = getDatabase()
        val list = mutableListOf<String>()
        val cursor = db.rawQuery(
            "SELECT word FROM bookmarks ORDER BY timestamp DESC",
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(c.getString(0))
            }
        }
        return list
    }

    fun toggleBookmark(word: String): Boolean {
        val clean = word.trim()
        if (clean.isEmpty()) return false
        val db = getDatabase()
        val isSaved = isBookmarked(clean)
        return if (isSaved) {
            db.delete("bookmarks", "word = ?", arrayOf(clean))
            false
        } else {
            val values = ContentValues().apply {
                put("word", clean)
                put("timestamp", System.currentTimeMillis())
            }
            db.insertWithOnConflict("bookmarks", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            true
        }
    }

    fun isBookmarked(word: String): Boolean {
        val clean = word.trim()
        if (clean.isEmpty()) return false
        val db = getDatabase()
        val cursor = db.rawQuery(
            "SELECT 1 FROM bookmarks WHERE word = ? LIMIT 1",
            arrayOf(clean)
        )
        cursor.use { return it.moveToFirst() }
    }

    private fun parseJsonStringList(jsonStr: String?): List<String> {
        if (jsonStr.isNullOrEmpty() || jsonStr == "[]" || jsonStr == "null") return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = ArrayList<String>(array.length())
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
