package com.matrix.synapse.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Builds an M3 [ColorScheme] that flows from Synapse design tokens, so any
 * component that reads `MaterialTheme.colorScheme.*` automatically picks up
 * the Synapse palette without going through [SynapseTheme.colors].
 */
private fun ColorScheme.applySynapse(c: SynapseColors): ColorScheme = copy(
    primary            = c.accent,
    onPrimary          = if (c.isLight) c.surface else c.bg,
    primaryContainer   = c.accentBg,
    onPrimaryContainer = c.text,
    secondary          = c.accentSoft,
    onSecondary        = if (c.isLight) c.surface else c.bg,
    secondaryContainer = c.infoBg,
    onSecondaryContainer = c.text,
    tertiary           = c.accentSoft,
    onTertiary         = if (c.isLight) c.surface else c.bg,
    tertiaryContainer  = c.infoBg,
    onTertiaryContainer = c.text,
    error              = c.danger,
    onError            = if (c.isLight) c.surface else c.bg,
    errorContainer     = c.dangerBg,
    onErrorContainer   = c.text,
    background         = c.bg,
    onBackground       = c.text,
    surface            = c.surface,
    onSurface          = c.text,
    surfaceVariant     = c.surface2,
    onSurfaceVariant   = c.textMuted,
    surfaceTint        = c.accent,
    inverseSurface     = c.text,
    inverseOnSurface   = c.bg,
    inversePrimary     = c.accent,
    outline            = c.line,
    outlineVariant     = c.line,
    scrim              = c.bg,
    surfaceContainerLowest = c.bg,
    surfaceContainerLow    = c.surface,
    surfaceContainer       = c.surface,
    surfaceContainerHigh   = c.surface2,
    surfaceContainerHighest = c.surface3,
)

@Composable
fun SynapseAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val synapseColors = if (darkTheme) DarkSynapseColors else LightSynapseColors
    val baseScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    val colorScheme = baseScheme.applySynapse(synapseColors)

    CompositionLocalProvider(LocalSynapseColors provides synapseColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SynapseTypography,
            shapes = SynapseShapes,
            content = content,
        )
    }
}
