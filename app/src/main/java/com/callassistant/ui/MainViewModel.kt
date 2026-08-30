package com.callassistant.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.callassistant.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _selectedRoute = MutableStateFlow("dial_pad")
    val selectedRoute: StateFlow<String> = _selectedRoute

    fun selectRoute(route: String) {
        _selectedRoute.value = route
    }

    private fun loadThemeMode(): ThemeMode = try {
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    } catch (_: IllegalArgumentException) {
        ThemeMode.SYSTEM
    }

    fun selectThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

}
