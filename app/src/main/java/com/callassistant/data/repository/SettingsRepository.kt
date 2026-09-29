package com.callassistant.data.repository

import android.content.Context
import com.callassistant.ui.theme.ThemeMode
import com.callassistant.ai.model.AiContextScope
import com.callassistant.ai.model.AiModelVariant
import com.callassistant.ai.model.AiModelCatalog
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

    fun isAiAssistantEnabled(): Boolean
    fun setAiAssistantEnabled(enabled: Boolean)
    fun getAiDefaultContextScope(): AiContextScope
    fun setAiDefaultContextScope(scope: AiContextScope)
    fun isAiDownloadOnWifiOnly(): Boolean
    fun setAiDownloadOnWifiOnly(wifiOnly: Boolean)
    fun getAiModelDownloadedAt(): Long
    fun setAiModelDownloadedAt(timestamp: Long)
    val selectedAiModel: StateFlow<AiModelVariant>
    fun getSelectedAiModel(): AiModelVariant
    fun setSelectedAiModel(variant: AiModelVariant)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _selectedAiModel = MutableStateFlow(loadSelectedAiModel())
    override val selectedAiModel: StateFlow<AiModelVariant> = _selectedAiModel.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    override fun isSetupComplete(): Boolean =
        prefs.getBoolean(KEY_SETUP_COMPLETE, false)

    override fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    override fun isAiAssistantEnabled(): Boolean =
        prefs.getBoolean(KEY_AI_ENABLED, true)

    override fun setAiAssistantEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
    }

    override fun getAiDefaultContextScope(): AiContextScope = try {
        AiContextScope.valueOf(
            prefs.getString(KEY_AI_CONTEXT_SCOPE, AiContextScope.DEFAULT.name)
                ?: AiContextScope.DEFAULT.name
        )
    } catch (_: IllegalArgumentException) {
        AiContextScope.DEFAULT
    }

    override fun setAiDefaultContextScope(scope: AiContextScope) {
        prefs.edit().putString(KEY_AI_CONTEXT_SCOPE, scope.name).apply()
    }

    override fun isAiDownloadOnWifiOnly(): Boolean =
        prefs.getBoolean(KEY_AI_WIFI_ONLY, true)

    override fun setAiDownloadOnWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_AI_WIFI_ONLY, wifiOnly).apply()
    }

    override fun getAiModelDownloadedAt(): Long =
        prefs.getLong(KEY_AI_MODEL_DOWNLOADED_AT, 0L)

    override fun setAiModelDownloadedAt(timestamp: Long) {
        prefs.edit().putLong(KEY_AI_MODEL_DOWNLOADED_AT, timestamp).apply()
    }

    override fun getSelectedAiModel(): AiModelVariant = _selectedAiModel.value

    override fun setSelectedAiModel(variant: AiModelVariant) {
        _selectedAiModel.value = variant
        prefs.edit().putString(KEY_AI_SELECTED_MODEL, variant.id).apply()
    }

    private fun loadSelectedAiModel(): AiModelVariant {
        val id = prefs.getString(KEY_AI_SELECTED_MODEL, null)
        return if (id != null) AiModelVariant.fromId(id) else AiModelVariant.DEFAULT
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
        const val KEY_AI_ENABLED = "ai_assistant_enabled"
        const val KEY_AI_CONTEXT_SCOPE = "ai_context_scope"
        const val KEY_AI_WIFI_ONLY = "ai_download_wifi_only"
        const val KEY_AI_MODEL_DOWNLOADED_AT = "ai_model_downloaded_at"
        const val KEY_AI_SELECTED_MODEL = "ai_selected_model"
    }
}
