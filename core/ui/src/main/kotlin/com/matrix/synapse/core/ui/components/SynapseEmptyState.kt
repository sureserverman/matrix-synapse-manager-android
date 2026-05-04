package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

/**
 * Synapse empty-state block. 48dp icon in a Surface2 circle, TitleS heading,
 * BodyS muted description, optional primary action button.
 *
 * `errorTone = true` tints the icon and circle Danger / DangerBg — used as the
 * error-state alternative.
 */
@Composable
fun SynapseEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    errorTone: Boolean = false,
) {
    val c = SynapseTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Tokens.Space.Xl, vertical = Tokens.Space.Xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (errorTone) c.dangerBg else c.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (errorTone) c.danger else c.textMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(Tokens.Space.Lg))
        }
        Text(text = title, style = SynapseText.TitleS, color = c.text)
        if (body != null) {
            Spacer(Modifier.height(Tokens.Space.Xs))
            Text(
                text = body,
                style = SynapseText.BodyS,
                color = c.textMuted,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Tokens.Space.Xl))
            SynapseButton(
                text = actionLabel,
                onClick = onAction,
                variant = SynapseButtonVariant.Primary,
            )
        }
    }
}
