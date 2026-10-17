package com.madhuraj.dict.data.model

data class WordSense(
    val pos: String?,
    val definition: String,
    val examples: List<String> = emptyList(),
    val synonyms: List<String> = emptyList()
)

data class WordEntry(
    val word: String,
    val senses: List<WordSense>
)
