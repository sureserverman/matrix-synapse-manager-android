package com.matrix.synapse.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Synapse Console design system tokens.
 *
 * Single source of truth for color, type, radius, spacing.
 * Mirrors `tokens.jsx` from the design handoff bundle.
 *
 * Naming follows the design canvas; do not rename without updating the spec.
 */
object Tokens {

    object Color {
        // ── Dark (default) ──────────────────────────────────────────────
        object Dark {
            val Bg          = Color(0xFF0B1220)
            val Surface     = Color(0xFF111A2E)
            val Surface2    = Color(0xFF18233D)
            val Surface3    = Color(0xFF223150)
            val Line        = Color(0xFF2A3A5C)

            val Text        = Color(0xFFE6EDF7)
            val TextMuted   = Color(0xFF8FA0BD)
            val TextDim     = Color(0xFF6A7A99)

            val Accent      = Color(0xFF5EE3D0) // Synapse teal
            val AccentSoft  = Color(0xFF7FB8FF)
            val AccentBg    = Color(0x335EE3D0) // 20% accent

            val Success     = Color(0xFF5EE3A0)
            val Warn        = Color(0xFFF5C26B)
            val Danger      = Color(0xFFFF7A8A)
            val Info        = Color(0xFF7FB8FF)

            val SuccessBg   = Color(0x335EE3A0)
            val WarnBg      = Color(0x33F5C26B)
            val DangerBg    = Color(0x33FF7A8A)
            val InfoBg      = Color(0x337FB8FF)
        }

        // ── Light ───────────────────────────────────────────────────────
        object Light {
            val Bg          = Color(0xFFF6F8FC)
            val Surface     = Color(0xFFFFFFFF)
            val Surface2    = Color(0xFFF0F3F9)
            val Surface3    = Color(0xFFE4EAF2)
            val Line        = Color(0xFFD8DFEA)

            val Text        = Color(0xFF0E1626)
            val TextMuted   = Color(0xFF5A6B85)
            val TextDim     = Color(0xFF8895AB)

            val Accent      = Color(0xFF0E9F8A)
            val AccentSoft  = Color(0xFF2A7FE0)
            val AccentBg    = Color(0x330E9F8A)

            val Success     = Color(0xFF1AA86A)
            val Warn        = Color(0xFFB8741F)
            val Danger      = Color(0xFFD23F4F)
            val Info        = Color(0xFF2A7FE0)

            val SuccessBg   = Color(0x331AA86A)
            val WarnBg      = Color(0x33B8741F)
            val DangerBg    = Color(0x33D23F4F)
            val InfoBg      = Color(0x332A7FE0)
        }
    }

    object Radius {
        val Sm   = 8.dp
        val Md   = 12.dp
        val Lg   = 16.dp
        val Pill = 999.dp
    }

    object Space {
        val Xs  = 4.dp
        val Sm  = 8.dp
        val Md  = 12.dp
        val Lg  = 16.dp
        val Xl  = 24.dp
        val Xxl = 32.dp

        /** Screen edge padding — denser than M3's 24dp default (sysadmin density). */
        val ScreenEdge = 20.dp
    }

    /** Type scale. Use Inter for prose; Mono for identifiers (MXID, MXC, server URLs, job names, versions). */
    object Type {
        val Display  = TypeSpec(weight = FontWeight.W700, size = 28.sp, lineHeight = 34.sp)
        val Title    = TypeSpec(weight = FontWeight.W600, size = 20.sp, lineHeight = 26.sp)
        val TitleS   = TypeSpec(weight = FontWeight.W600, size = 16.sp, lineHeight = 22.sp)
        val BodyM    = TypeSpec(weight = FontWeight.W400, size = 14.sp, lineHeight = 20.sp)
        val BodyS    = TypeSpec(weight = FontWeight.W400, size = 13.sp, lineHeight = 18.sp)
        val Caption  = TypeSpec(weight = FontWeight.W400, size = 12.sp, lineHeight = 16.sp)
        val Label    = TypeSpec(weight = FontWeight.W600, size = 11.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp)
        val Mono     = TypeSpec(weight = FontWeight.W400, size = 12.sp, lineHeight = 16.sp, mono = true)
    }
}

data class TypeSpec(
    val weight: FontWeight,
    val size: TextUnit,
    val lineHeight: TextUnit,
    val letterSpacing: TextUnit = 0.sp,
    val mono: Boolean = false,
)
