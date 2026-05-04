package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

/**
 * UPPERCASE section header. Spec: Label / +0.8 sp tracking / muted.
 * Sits above a [SynapseCard] or list group with screen-edge horizontal padding.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = SynapseText.Label,
        color = SynapseTheme.colors.textMuted,
        modifier = modifier.padding(
            start = Tokens.Space.ScreenEdge,
            end = Tokens.Space.ScreenEdge,
            top = Tokens.Space.Lg,
            bottom = Tokens.Space.Sm,
        ),
    )
}
