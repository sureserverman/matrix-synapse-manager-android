package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.ui.theme.SynapseShapes
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

/**
 * Surface card with 1dp Line stroke, no shadow. Default radius is Md.
 *
 * Pass `padding = 0.dp` for list-item containers (the inner items handle
 * their own spacing). Pass [Tokens.Space.Lg] for prose cards.
 */
@Composable
fun SynapseCard(
    modifier: Modifier = Modifier,
    padding: Dp = Tokens.Space.Lg,
    raised: Boolean = false,
    content: @Composable () -> Unit,
) {
    val c = SynapseTheme.colors
    Card(
        modifier = modifier,
        shape = SynapseShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (raised) c.surface2 else c.surface,
            contentColor = c.text,
        ),
        border = BorderStroke(1.dp, c.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        if (padding > 0.dp) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(padding),
            ) { content() }
        } else {
            content()
        }
    }
}
