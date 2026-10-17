package com.madhuraj.dict.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.madhuraj.dict.R

object AppSettings {

    private const val PREFS_NAME = "dict_settings"
    private const val KEY_THEME = "theme_mode" // "system", "light", "dark", "amoled"
    private const val KEY_AUDIO_MODE = "audio_mode" // "human", "tts"
    private const val KEY_ACCENT = "accent_color" // "dynamic", "monochrome", "amber", "blue", "emerald", "crimson"

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
            "dark", "amoled" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
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

    fun isDark(context: Context): Boolean {
        return when (getThemeMode(context)) {
            "dark", "amoled" -> true
            "light" -> false
            else -> {
                val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun applyAppTheme(activity: Activity, isTranslucent: Boolean = false) {
        val themeMode = getThemeMode(activity)
        val accent = getAccent(activity)

        // 1. Base night mode
        applyTheme(themeMode)

        // 2. Base Theme (AMOLED vs Standard vs Translucent)
        if (themeMode == "amoled") {
            if (isTranslucent) {
                activity.setTheme(R.style.Theme_Dict_Translucent_Amoled)
            } else {
                activity.setTheme(R.style.Theme_Dict_Amoled)
            }
        } else if (isTranslucent) {
            activity.setTheme(R.style.Theme_Dict_Translucent)
        }

        // 3. Accent Theme Overlay
        when (accent) {
            "monochrome" -> activity.theme.applyStyle(R.style.ThemeOverlay_Dict_Monochrome, true)
            "blue" -> activity.theme.applyStyle(R.style.ThemeOverlay_Dict_Blue, true)
            "emerald" -> activity.theme.applyStyle(R.style.ThemeOverlay_Dict_Emerald, true)
            "crimson" -> activity.theme.applyStyle(R.style.ThemeOverlay_Dict_Crimson, true)
            "dynamic" -> DynamicColors.applyToActivityIfAvailable(activity)
            else -> activity.theme.applyStyle(R.style.ThemeOverlay_Dict_Amber, true)
        }
    }
}
