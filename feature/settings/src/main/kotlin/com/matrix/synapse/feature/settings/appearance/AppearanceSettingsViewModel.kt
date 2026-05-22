package com.matrix.synapse.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.core.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val repository: ThemeModeRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repository.themeMode

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }
}
