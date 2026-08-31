package com.callassistant.data.repository

import android.content.Context
import com.callassistant.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
    fun isSetupComplete(): Boolean
    fun setSetupComplete(complete: Boolean)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    override fun isSetupComplete(): Boolean =
        prefs.getBoolean(KEY_SETUP_COMPLETE, false)

    override fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    private fun loadThemeMode(): ThemeMode = try {
        ThemeMode.valueOf(
            prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        )
    } catch (_: IllegalArgumentException) {
        ThemeMode.SYSTEM
    }

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SETUP_COMPLETE = "setup_complete"
    }
}
