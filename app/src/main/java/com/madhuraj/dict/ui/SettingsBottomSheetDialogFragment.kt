package com.madhuraj.dict.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madhuraj.dict.R
import com.madhuraj.dict.data.AppSettings
import com.madhuraj.dict.databinding.DialogSettingsBinding

class SettingsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    var onSettingsChangedListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        // 1. Theme Setup
        when (AppSettings.getThemeMode(context)) {
            "light" -> binding.chipThemeLight.isChecked = true
            "dark" -> binding.chipThemeDark.isChecked = true
            else -> binding.chipThemeSystem.isChecked = true
        }

        binding.chipGroupTheme.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when {
                checkedIds.contains(R.id.chipThemeLight) -> "light"
                checkedIds.contains(R.id.chipThemeDark) -> "dark"
                else -> "system"
            }
            AppSettings.setThemeMode(context, mode)
            onSettingsChangedListener?.invoke()
        }

        // 2. Accent Color Setup
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            binding.chipAccentDynamic.visibility = View.GONE
        }

        when (AppSettings.getAccent(context)) {
            "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) binding.chipAccentDynamic.isChecked = true else binding.chipAccentAmber.isChecked = true
            "blue" -> binding.chipAccentBlue.isChecked = true
            "emerald" -> binding.chipAccentEmerald.isChecked = true
            "crimson" -> binding.chipAccentCrimson.isChecked = true
            else -> binding.chipAccentAmber.isChecked = true
        }

        binding.chipGroupAccent.setOnCheckedStateChangeListener { _, checkedIds ->
            val accent = when {
                checkedIds.contains(R.id.chipAccentDynamic) -> "dynamic"
                checkedIds.contains(R.id.chipAccentBlue) -> "blue"
                checkedIds.contains(R.id.chipAccentEmerald) -> "emerald"
                checkedIds.contains(R.id.chipAccentCrimson) -> "crimson"
                else -> "amber"
            }
            AppSettings.setAccent(context, accent)
            onSettingsChangedListener?.invoke()
        }

        // 3. Audio Mode Setup
        when (AppSettings.getAudioMode(context)) {
            "tts" -> binding.chipAudioTts.isChecked = true
            else -> binding.chipAudioHuman.isChecked = true
        }

        binding.chipGroupAudio.setOnCheckedStateChangeListener { _, checkedIds ->
            val audioMode = if (checkedIds.contains(R.id.chipAudioTts)) "tts" else "human"
            AppSettings.setAudioMode(context, audioMode)
            onSettingsChangedListener?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingsBottomSheet"
        fun newInstance() = SettingsBottomSheetDialogFragment()
    }
}
