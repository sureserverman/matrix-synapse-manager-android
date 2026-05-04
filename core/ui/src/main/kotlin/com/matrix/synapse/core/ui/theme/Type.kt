package com.matrix.synapse.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis

/**
 * Inter is the prose family. We declare it as the system sans-serif fallback
 * (`FontFamily.SansSerif`) — Inter ships on Android 13+ and falls back cleanly
 * on older devices to Roboto without bundling a custom font asset. If the app
 * later adopts downloadable fonts or a bundled Inter variable font, swap the
 * family here without touching call sites.
 */
val InterFamily: FontFamily = FontFamily.SansSerif

/**
 * JetBrains Mono is the identifier family. We declare it as the system
 * monospace fallback (`FontFamily.Monospace`); Android 13+ bundles JetBrains
 * Mono. Older devices fall back to the system mono. Use [TypeSpec.mono] = true
 * to opt in (and prefer [SynapseText.Mono] in call sites).
 */
val MonoFamily: FontFamily = FontFamily.Monospace

private fun TypeSpec.toTextStyle(): TextStyle = TextStyle(
    fontFamily = if (mono) MonoFamily else InterFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    fontStyle = FontStyle.Normal,
    fontSynthesis = FontSynthesis.None,
)

/**
 * The full Synapse type scale, also exposed as Material 3 [Typography] for
 * components that read from `MaterialTheme.typography`.
 *
 * Mapping rationale:
 * - displayLarge ← Display
 * - headlineLarge / titleLarge ← Title
 * - titleMedium ← TitleS
 * - bodyLarge / bodyMedium ← BodyM
 * - bodySmall ← BodyS
 * - labelLarge / labelMedium ← Caption
 * - labelSmall ← Label (UPPERCASE section headers — applied at call site)
 */
val SynapseTypography: Typography = run {
    val display = Tokens.Type.Display.toTextStyle()
    val title   = Tokens.Type.Title.toTextStyle()
    val titleS  = Tokens.Type.TitleS.toTextStyle()
    val bodyM   = Tokens.Type.BodyM.toTextStyle()
    val bodyS   = Tokens.Type.BodyS.toTextStyle()
    val caption = Tokens.Type.Caption.toTextStyle()
    val label   = Tokens.Type.Label.toTextStyle()
    Typography(
        displayLarge   = display,
        displayMedium  = display,
        displaySmall   = display,
        headlineLarge  = title,
        headlineMedium = title,
        headlineSmall  = title,
        titleLarge     = title,
        titleMedium    = titleS,
        titleSmall     = titleS,
        bodyLarge      = bodyM,
        bodyMedium     = bodyM,
        bodySmall      = bodyS,
        labelLarge     = caption,
        labelMedium    = caption,
        labelSmall     = label,
    )
}

object SynapseText {
    val Display: TextStyle = Tokens.Type.Display.toTextStyle()
    val Title:   TextStyle = Tokens.Type.Title.toTextStyle()
    val TitleS:  TextStyle = Tokens.Type.TitleS.toTextStyle()
    val BodyM:   TextStyle = Tokens.Type.BodyM.toTextStyle()
    val BodyS:   TextStyle = Tokens.Type.BodyS.toTextStyle()
    val Caption: TextStyle = Tokens.Type.Caption.toTextStyle()
    val Label:   TextStyle = Tokens.Type.Label.toTextStyle()
    val Mono:    TextStyle = Tokens.Type.Mono.toTextStyle()
}
