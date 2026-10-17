package com.madhuraj.dict.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.madhuraj.dict.R
import com.madhuraj.dict.data.DictionaryRepository
import com.madhuraj.dict.data.model.WordEntry
import com.madhuraj.dict.databinding.ActivityMainBinding
import com.madhuraj.dict.ui.adapter.SenseAdapter
import com.madhuraj.dict.ui.adapter.SuggestionAdapter
import com.madhuraj.dict.util.TtsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: DictionaryRepository
    private lateinit var ttsHelper: TtsHelper

    private lateinit var suggestionAdapter: SuggestionAdapter
    private lateinit var homeAdapter: SuggestionAdapter
    private lateinit var senseAdapter: SenseAdapter

    private var currentWordEntry: WordEntry? = null
    private var searchJob: Job? = null
    private var isDbInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = DictionaryRepository(this)
        ttsHelper = TtsHelper(this)

        setupRecyclerViews()
        setupListeners()
        setupBackPress()
        initializeDatabase(savedInstanceState == null)
    }

    private fun setupRecyclerViews() {
        suggestionAdapter = SuggestionAdapter(
            onWordClick = { word -> loadWord(word) },
            onInsertClick = { word ->
                binding.editSearch.setText(word)
                binding.editSearch.setSelection(word.length)
            }
        )
        binding.recyclerSuggestions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = suggestionAdapter
        }

        homeAdapter = SuggestionAdapter(
            onWordClick = { word -> loadWord(word) },
            onInsertClick = { word ->
                binding.editSearch.setText(word)
                binding.editSearch.setSelection(word.length)
            }
        )
        binding.recyclerHome.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = homeAdapter
        }

        senseAdapter = SenseAdapter(
            onSynonymClick = { syn -> loadWord(syn) }
        )
        binding.recyclerSenses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = senseAdapter
        }
    }

    private var isProgrammaticTextChange = false

    private fun setupListeners() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticTextChange) return
                val query = s?.toString()?.trim() ?: ""
                binding.btnClear.isVisible = query.isNotEmpty()

                if (query.isEmpty()) {
                    showHomeView()
                } else {
                    triggerSearch(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    loadWord(query)
                }
                true
            } else {
                false
            }
        }

        binding.btnClear.setOnClickListener {
            binding.editSearch.text?.clear()
            showHomeView()
        }

        binding.btnRandom.setOnClickListener {
            lifecycleScope.launch {
                val randomWord = repository.getRandomWord()
                if (!randomWord.isNullOrEmpty()) {
                    loadWord(randomWord)
                }
            }
        }

        binding.chipGroupHome.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(R.id.chipBookmarks)) {
                loadBookmarks()
            } else {
                loadHistory()
            }
        }

        binding.btnSpeak.setOnClickListener {
            currentWordEntry?.word?.let { ttsHelper.speak(it) }
        }

        binding.btnBookmark.setOnClickListener {
            val entry = currentWordEntry ?: return@setOnClickListener
            lifecycleScope.launch {
                val isSaved = repository.toggleBookmark(entry.word)
                updateBookmarkIcon(isSaved)
                val msg = if (isSaved) "Saved to bookmarks" else "Removed from bookmarks"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                if (binding.chipBookmarks.isChecked) {
                    loadBookmarks()
                }
            }
        }

        binding.btnCopy.setOnClickListener {
            val entry = currentWordEntry ?: return@setOnClickListener
            val textToCopy = buildString {
                append(entry.word)
                append("\n\n")
                entry.senses.forEachIndexed { i, sense ->
                    append("${i + 1}. ")
                    if (!sense.pos.isNullOrBlank()) append("(${sense.pos}) ")
                    append(sense.definition)
                    if (sense.synonyms.isNotEmpty()) {
                        append("\nSynonyms: ")
                        append(sense.synonyms.joinToString(", "))
                    }
                    append("\n\n")
                }
            }.trim()

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Dictionary Definition", textToCopy))
            Toast.makeText(this, "Copied definition to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            val entry = currentWordEntry ?: return@setOnClickListener
            val shareText = "${entry.word}\n\n" + entry.senses.joinToString("\n") { s ->
                val posStr = if (!s.pos.isNullOrBlank()) "(${s.pos}) " else ""
                "$posStr${s.definition}"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "Share Definition"))
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewWordDetail.isVisible) {
                    binding.editSearch.text?.clear()
                    showHomeView()
                } else if (binding.recyclerSuggestions.isVisible) {
                    binding.editSearch.text?.clear()
                    showHomeView()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun initializeDatabase(handleIntent: Boolean) {
        binding.progressLoading.isVisible = true
        lifecycleScope.launch {
            try {
                repository.initialize()
                isDbInitialized = true
                binding.progressLoading.isVisible = false

                if (handleIntent && !handleIncomingIntent(intent)) {
                    loadHistory()
                }
            } catch (e: Exception) {
                binding.progressLoading.isVisible = false
                Toast.makeText(this@MainActivity, "Failed to load database: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(intent: Intent): Boolean {
        val query = intent.getStringExtra("query")
            ?: intent.getStringExtra(android.app.SearchManager.QUERY)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            ?: intent.data?.getQueryParameter("q")

        if (!query.isNullOrBlank()) {
            loadWord(query.trim())
            return true
        }
        return false
    }

    private fun triggerSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(40) // Sub-50ms debounce
            val suggestions = repository.searchPrefix(query, 30)
            suggestionAdapter.submitList(suggestions)
            binding.recyclerSuggestions.isVisible = true
            binding.viewHome.isVisible = false
            binding.viewWordDetail.isVisible = false
        }
    }

    fun loadWord(word: String) {
        hideKeyboard()
        lifecycleScope.launch {
            val entry = repository.searchExact(word)
            if (entry != null) {
                currentWordEntry = entry
                repository.addHistory(entry.word)

                binding.textHeadword.text = entry.word
                val isSaved = repository.isBookmarked(entry.word)
                updateBookmarkIcon(isSaved)

                senseAdapter.submitList(entry.senses)
                binding.viewWordDetail.isVisible = true
                binding.recyclerSuggestions.isVisible = false
                binding.viewHome.isVisible = false

                // Update search field text without re-triggering search
                searchJob?.cancel()
                if (binding.editSearch.text.toString() != entry.word) {
                    isProgrammaticTextChange = true
                    binding.editSearch.setText(entry.word)
                    binding.editSearch.setSelection(entry.word.length)
                    isProgrammaticTextChange = false
                }
            } else {
                Toast.makeText(this@MainActivity, "Word not found in offline dictionary", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showHomeView() {
        binding.viewHome.isVisible = true
        binding.recyclerSuggestions.isVisible = false
        binding.viewWordDetail.isVisible = false
        currentWordEntry = null

        if (binding.chipBookmarks.isChecked) {
            loadBookmarks()
        } else {
            loadHistory()
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val history = repository.getHistory(40)
            homeAdapter.submitList(history)
            binding.textHomeEmpty.isVisible = history.isEmpty()
            binding.textHomeEmpty.text = "No recent searches.\nType a word above to search 380,000+ definitions."
        }
    }

    private fun loadBookmarks() {
        lifecycleScope.launch {
            val bookmarks = repository.getBookmarks()
            homeAdapter.submitList(bookmarks)
            binding.textHomeEmpty.isVisible = bookmarks.isEmpty()
            binding.textHomeEmpty.text = "No saved bookmarks yet.\nTap the star icon on any word to save it here."
        }
    }

    private fun updateBookmarkIcon(isSaved: Boolean) {
        val iconRes = if (isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
        binding.btnBookmark.setImageResource(iconRes)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.editSearch.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper.shutdown()
    }
}
