package com.matrix.synapse.manager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.matrix.synapse.core.ui.theme.SynapseAppTheme
import com.matrix.synapse.core.ui.theme.ThemeMode

/**
 * App-level theme. Delegates colours, typography, and shapes to the
 * Synapse Console design system (see [SynapseAppTheme] in `:core:ui`).
 *
 * Kept here so existing call sites (`MatrixSynapseManagerTheme { ... }`)
 * continue to work unchanged.
 */
@Composable
fun MatrixSynapseManagerTheme(
    themeMode: ThemeMode = ThemeMode.System,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    },
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    SynapseAppTheme(darkTheme = darkTheme, content = content)
}
