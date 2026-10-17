package com.madhuraj.dict.ui

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.madhuraj.dict.data.AppSettings
import com.madhuraj.dict.data.DictionaryRepository
import com.madhuraj.dict.data.model.WordEntry
import com.madhuraj.dict.databinding.ActivitySearchHandlerBinding
import com.madhuraj.dict.ui.adapter.SenseAdapter
import com.madhuraj.dict.util.AudioPlayerManager
import com.madhuraj.dict.util.DictionaryFormatter
import kotlinx.coroutines.launch

class SearchHandlerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchHandlerBinding
    private lateinit var repository: DictionaryRepository
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var senseAdapter: SenseAdapter
    private var currentWord: String = ""
    private var currentEntry: WordEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyTheme(AppSettings.getThemeMode(this))
        super.onCreate(savedInstanceState)
        binding = ActivitySearchHandlerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = DictionaryRepository(this)
        audioPlayerManager = AudioPlayerManager(this, lifecycleScope)

        setupUI()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun setupUI() {
        senseAdapter = SenseAdapter(
            onSynonymClick = { syn -> loadWord(syn) }
        )
        binding.recyclerSenses.apply {
            layoutManager = LinearLayoutManager(this@SearchHandlerActivity)
            adapter = senseAdapter
        }

        com.madhuraj.dict.util.DictionaryFormatter.setupWebView(binding.webWordDetail) { clickedWord ->
            loadWord(clickedWord)
        }

        binding.dialogBackdrop.setOnClickListener {
            finish()
        }

        binding.bottomSheetCard.setOnClickListener {
            // Prevent clicks on card from bubbling to backdrop
        }

        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnSpeak.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                audioPlayerManager.playWord(currentWord, currentEntry?.audioUrl)
            }
        }

        binding.btnOpenFull.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                val fullIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra("query", currentWord)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(fullIntent)
                finish()
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        val query = extractQuery(intent)
        if (!query.isNullOrBlank()) {
            loadWord(query.trim())
        } else {
            // No query provided, open main app directly
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun extractQuery(intent: Intent): String? {
        // 1. Process Text (when highlighting a word in another app)
        val textSelection = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (!textSelection.isNullOrBlank()) return textSelection

        // 2. Standard SearchManager Query (WEB_SEARCH or SEARCH)
        val searchManagerQuery = intent.getStringExtra(SearchManager.QUERY)
        if (!searchManagerQuery.isNullOrBlank()) return searchManagerQuery

        // 3. User query extra
        val userQuery = intent.getStringExtra("query")
        if (!userQuery.isNullOrBlank()) return userQuery

        // 4. URI query parameter (q or query)
        val uriQuery = intent.data?.getQueryParameter("q") ?: intent.data?.getQueryParameter("query")
        if (!uriQuery.isNullOrBlank()) return uriQuery

        return null
    }

    private fun loadWord(word: String) {
        currentWord = word
        binding.textWordTitle.text = word

        lifecycleScope.launch {
            repository.initialize()
            val entry = repository.searchExact(word)
                ?: findFirstPrefixMatch(word)

            if (entry != null) {
                currentWord = entry.word
                currentEntry = entry
                binding.textWordTitle.text = entry.word

                repository.addHistory(entry.word)

                val isDark = DictionaryFormatter.isDarkTheme(this@SearchHandlerActivity)
                if (!entry.htmlFormatted.isNullOrBlank()) {
                    val pageHtml = DictionaryFormatter.buildDictionaryPage(this@SearchHandlerActivity, entry.htmlFormatted, isDark)
                    binding.webWordDetail.loadDataWithBaseURL("https://en.wiktionary.org/", pageHtml, "text/html", "utf-8", null)
                    binding.webWordDetail.isVisible = true
                    binding.recyclerSenses.isVisible = false
                } else {
                    val pageHtml = DictionaryFormatter.formatSensesToHtml(this@SearchHandlerActivity, entry.word, entry.ipa, entry.senses, entry.translations, isDark)
                    binding.webWordDetail.loadDataWithBaseURL("https://en.wiktionary.org/", pageHtml, "text/html", "utf-8", null)
                    binding.webWordDetail.isVisible = true
                    binding.recyclerSenses.isVisible = false
                }

                binding.textNotFound.isVisible = false
                binding.scrollSenses.isVisible = true
                binding.btnSpeak.isVisible = true
                binding.btnOpenFull.isVisible = true
                binding.scrollSenses.post {
                    binding.scrollSenses.scrollTo(0, 0)
                }
            } else {
                currentEntry = null
                binding.textNotFound.isVisible = true
                binding.scrollSenses.isVisible = false
                binding.btnSpeak.isVisible = false
                binding.btnOpenFull.isVisible = false
                binding.textNotFound.text = "No offline definition found for “$word”."
            }
        }
    }

    private suspend fun findFirstPrefixMatch(word: String): WordEntry? {
        val suggestions = repository.searchPrefix(word, 1)
        return if (suggestions.isNotEmpty()) {
            repository.searchExact(suggestions[0])
        } else {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
