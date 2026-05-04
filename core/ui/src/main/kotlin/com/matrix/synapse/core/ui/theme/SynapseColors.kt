package com.matrix.synapse.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended palette beyond Material 3's ColorScheme — exposes the design
 * tokens (Surface2/Surface3, status tones, mono-borderable Line) that have
 * no direct M3 equivalent.
 *
 * Access via [SynapseTheme.colors] or [LocalSynapseColors].
 */
@Immutable
data class SynapseColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val line: Color,

    val text: Color,
    val textMuted: Color,
    val textDim: Color,

    val accent: Color,
    val accentSoft: Color,
    val accentBg: Color,

    val success: Color,
    val warn: Color,
    val danger: Color,
    val info: Color,

    val successBg: Color,
    val warnBg: Color,
    val dangerBg: Color,
    val infoBg: Color,

    val isLight: Boolean,
)

internal val DarkSynapseColors = SynapseColors(
    bg         = Tokens.Color.Dark.Bg,
    surface    = Tokens.Color.Dark.Surface,
    surface2   = Tokens.Color.Dark.Surface2,
    surface3   = Tokens.Color.Dark.Surface3,
    line       = Tokens.Color.Dark.Line,
    text       = Tokens.Color.Dark.Text,
    textMuted  = Tokens.Color.Dark.TextMuted,
    textDim    = Tokens.Color.Dark.TextDim,
    accent     = Tokens.Color.Dark.Accent,
    accentSoft = Tokens.Color.Dark.AccentSoft,
    accentBg   = Tokens.Color.Dark.AccentBg,
    success    = Tokens.Color.Dark.Success,
    warn       = Tokens.Color.Dark.Warn,
    danger     = Tokens.Color.Dark.Danger,
    info       = Tokens.Color.Dark.Info,
    successBg  = Tokens.Color.Dark.SuccessBg,
    warnBg     = Tokens.Color.Dark.WarnBg,
    dangerBg   = Tokens.Color.Dark.DangerBg,
    infoBg     = Tokens.Color.Dark.InfoBg,
    isLight    = false,
)

internal val LightSynapseColors = SynapseColors(
    bg         = Tokens.Color.Light.Bg,
    surface    = Tokens.Color.Light.Surface,
    surface2   = Tokens.Color.Light.Surface2,
    surface3   = Tokens.Color.Light.Surface3,
    line       = Tokens.Color.Light.Line,
    text       = Tokens.Color.Light.Text,
    textMuted  = Tokens.Color.Light.TextMuted,
    textDim    = Tokens.Color.Light.TextDim,
    accent     = Tokens.Color.Light.Accent,
    accentSoft = Tokens.Color.Light.AccentSoft,
    accentBg   = Tokens.Color.Light.AccentBg,
    success    = Tokens.Color.Light.Success,
    warn       = Tokens.Color.Light.Warn,
    danger     = Tokens.Color.Light.Danger,
    info       = Tokens.Color.Light.Info,
    successBg  = Tokens.Color.Light.SuccessBg,
    warnBg     = Tokens.Color.Light.WarnBg,
    dangerBg   = Tokens.Color.Light.DangerBg,
    infoBg     = Tokens.Color.Light.InfoBg,
    isLight    = true,
)

val LocalSynapseColors = staticCompositionLocalOf { DarkSynapseColors }

object SynapseTheme {
    val colors: SynapseColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSynapseColors.current
}
