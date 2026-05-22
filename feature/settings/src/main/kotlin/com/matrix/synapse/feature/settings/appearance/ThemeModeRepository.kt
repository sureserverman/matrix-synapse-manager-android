package com.matrix.synapse.feature.settings.appearance

import com.matrix.synapse.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** Persists the user's theme-mode override. Defaults to [ThemeMode.System] when unset. */
interface ThemeModeRepository {
    val themeMode: StateFlow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
