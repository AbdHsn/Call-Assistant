package com.callassistant.ui

import androidx.lifecycle.ViewModel
import com.callassistant.data.repository.SettingsRepository
import com.callassistant.ui.navigation.AppRoute
import com.callassistant.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode

    private val _selectedRoute = MutableStateFlow<AppRoute>(AppRoute.DialPad)
    val selectedRoute: StateFlow<AppRoute> = _selectedRoute.asStateFlow()

    fun selectRoute(route: AppRoute) {
        _selectedRoute.value = route
    }

    fun selectThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
