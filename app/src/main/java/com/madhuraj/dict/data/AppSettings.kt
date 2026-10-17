package com.madhuraj.dict.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object AppSettings {

    private const val PREFS_NAME = "dict_settings"
    private const val KEY_THEME = "theme_mode" // "system", "light", "dark"
    private const val KEY_AUDIO_MODE = "audio_mode" // "human", "tts"
    private const val KEY_TRANSLATIONS = "selected_languages"
    private const val KEY_ACCENT = "accent_color" // "dynamic", "amber", "blue", "emerald", "crimson"

    val DEFAULT_LANGUAGES = setOf("hi", "es", "fr", "de", "ru")

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): String {
        return prefs(context).getString(KEY_THEME, "system") ?: "system"
    }

    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_THEME, mode).apply()
        applyTheme(mode)
    }

    fun applyTheme(mode: String) {
        when (mode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun getAudioMode(context: Context): String {
        return prefs(context).getString(KEY_AUDIO_MODE, "human") ?: "human"
    }

    fun setAudioMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_AUDIO_MODE, mode).apply()
    }

    fun getAccent(context: Context): String {
        return prefs(context).getString(KEY_ACCENT, "amber") ?: "amber"
    }

    fun setAccent(context: Context, accent: String) {
        prefs(context).edit().putString(KEY_ACCENT, accent).apply()
    }

    fun getSelectedLanguages(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_TRANSLATIONS, DEFAULT_LANGUAGES) ?: DEFAULT_LANGUAGES
    }

    fun setSelectedLanguages(context: Context, langs: Set<String>) {
        prefs(context).edit().putStringSet(KEY_TRANSLATIONS, langs).apply()
    }

    fun isLanguageEnabled(context: Context, langCode: String): Boolean {
        return getSelectedLanguages(context).contains(langCode.lowercase())
    }
}
