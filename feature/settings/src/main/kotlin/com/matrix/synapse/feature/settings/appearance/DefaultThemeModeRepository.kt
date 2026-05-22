package com.matrix.synapse.feature.settings.appearance

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.matrix.synapse.core.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "theme_mode_prefs"
private const val KEY_MODE = "mode"

@Singleton
class DefaultThemeModeRepository @VisibleForTesting internal constructor(
    private val prefs: SharedPreferences,
) : ThemeModeRepository {

    @Inject constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    private val _themeMode = MutableStateFlow(load())

    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun load(): ThemeMode {
        val raw = prefs.getString(KEY_MODE, null) ?: return ThemeMode.System
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.System
    }
}
