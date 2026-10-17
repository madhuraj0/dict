package com.madhuraj.dict.data

import android.content.Context
import com.madhuraj.dict.data.model.WordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DictionaryRepository(context: Context) {

    private val dbHelper = DictionaryDatabaseHelper.getInstance(context)

    suspend fun initialize() = withContext(Dispatchers.IO) {
        dbHelper.ensureDatabaseExtracted()
        dbHelper.getDatabase()
    }

    suspend fun searchExact(word: String): WordEntry? = withContext(Dispatchers.IO) {
        dbHelper.searchExact(word)
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
