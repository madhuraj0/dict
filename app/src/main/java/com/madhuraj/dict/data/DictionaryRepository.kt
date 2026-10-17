package com.madhuraj.dict.data

import android.content.Context
import com.madhuraj.dict.data.model.WordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DictionaryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dbHelper = DictionaryDatabaseHelper.getInstance(appContext)

    suspend fun initialize() = withContext(Dispatchers.IO) {
        dbHelper.ensureDatabaseExtracted()
        dbHelper.getDatabase()
    }

    suspend fun searchExact(word: String): WordEntry? = withContext(Dispatchers.IO) {
        val livioEntry = queryLivioProvider(word)
        if (livioEntry != null) {
            return@withContext livioEntry
        }
        dbHelper.searchExact(word)
    }

    private fun queryLivioProvider(word: String): WordEntry? {
        val cleanWord = word.trim()
        if (cleanWord.isEmpty()) return null
        return try {
            val uri = android.net.Uri.parse("content://livio.pack.lang.en_US.DictionaryProvider/dictionary")
            val cursor = appContext.contentResolver.query(uri, null, null, arrayOf(cleanWord), null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val wordCol = c.getColumnIndex("suggest_text_1")
                    val textCol = c.getColumnIndex("suggest_text_2")
                    val foundWord = if (wordCol != -1) c.getString(wordCol) else cleanWord
                    val html = if (textCol != -1) c.getString(textCol) else null
                    android.util.Log.i("DictionaryRepository", "Livio provider query for '$cleanWord' returned html length=${html?.length}")
                    html?.chunked(800)?.take(3)?.forEachIndexed { idx, chunk ->
                        android.util.Log.i("DictionaryRepository", "HTML[$idx]: $chunk")
                    }
                    if (!html.isNullOrBlank()) {
                        val ipa = extractIpa(html) ?: dbHelper.getIpaForWord(cleanWord)
                        val offlineTrans = dbHelper.getTranslationsForWord(cleanWord)

                        var enrichedHtml = html
                        val hindi = offlineTrans["hi"]
                        if (!hindi.isNullOrBlank() && !enrichedHtml.contains("class=\"hindi-card\"")) {
                            val hindiCard = "<div class=\"hindi-card\"><div class=\"hindi-tag\">Hindi • हिन्दी</div><div class=\"hindi-word\">$hindi</div></div>"
                            enrichedHtml = when {
                                enrichedHtml.contains("</silence>") -> {
                                    enrichedHtml.replaceFirst("</silence>", "</silence>$hindiCard")
                                }
                                enrichedHtml.contains("<hr>") -> {
                                    enrichedHtml.replaceFirst("<hr>", "<hr>$hindiCard")
                                }
                                else -> {
                                    hindiCard + enrichedHtml
                                }
                            }
                        }

                        WordEntry(
                            word = foundWord,
                            ipa = ipa,
                            translations = offlineTrans,
                            htmlFormatted = enrichedHtml
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            android.util.Log.d("DictionaryRepository", "Livio provider not accessible: ${e.message}")
            null
        }
    }

    private fun extractIpa(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val match = Regex("""IPA</a></small>:\s*([^<]+)""").find(html)
            ?: Regex("""/[^/\n<]+/(?:,\s*/[^/\n<]+/)*""").find(html)
        return match?.groupValues?.getOrNull(1)?.trim() ?: match?.value?.trim()
    }

    suspend fun searchPrefix(prefix: String, limit: Int = 30): List<String> = withContext(Dispatchers.IO) {
        dbHelper.searchPrefix(prefix, limit)
    }

    suspend fun getRandomWord(): String? = withContext(Dispatchers.IO) {
        dbHelper.getRandomWord()
    }

    suspend fun getHistory(limit: Int = 30): List<String> = withContext(Dispatchers.IO) {
        dbHelper.getHistory(limit)
    }

    suspend fun addHistory(word: String) = withContext(Dispatchers.IO) {
        dbHelper.addHistory(word)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dbHelper.clearHistory()
    }

    suspend fun getBookmarks(): List<String> = withContext(Dispatchers.IO) {
        dbHelper.getBookmarks()
    }

    suspend fun toggleBookmark(word: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.toggleBookmark(word)
    }

    suspend fun isBookmarked(word: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.isBookmarked(word)
    }
}
