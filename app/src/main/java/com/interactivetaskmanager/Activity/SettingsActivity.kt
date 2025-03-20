package com.interactivetaskmanager.Activity

import android.content.SharedPreferences
import android.os.Bundle
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.interactivetaskmanager.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        // Detect system default mode
        val defaultNightMode = AppCompatDelegate.getDefaultNightMode()
        val isDarkMode = when (defaultNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> isSystemDarkMode() // If set to "Follow System"
        }

        // Load saved preference, if available
        val savedDarkMode = sharedPreferences.getBoolean("DarkMode", isDarkMode)
        binding.switchDarkMode.isChecked = savedDarkMode

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun toggleDarkMode(isDark: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("DarkMode", isDark)
        editor.apply()

        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun isSystemDarkMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}
