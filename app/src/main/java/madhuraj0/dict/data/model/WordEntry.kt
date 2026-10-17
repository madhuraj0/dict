package madhuraj0.dict.data.model

data class WordSense(
    val pos: String?,
    val definition: String,
    val examples: List<String> = emptyList(),
    val synonyms: List<String> = emptyList()
)

data class WordEntry(
    val word: String,
    val ipa: String? = null,
    val audioUrl: String? = null,
    val etymology: String? = null,
    val translations: Map<String, String> = emptyMap(),
    val senses: List<WordSense> = emptyList(),
    val htmlFormatted: String? = null
)
