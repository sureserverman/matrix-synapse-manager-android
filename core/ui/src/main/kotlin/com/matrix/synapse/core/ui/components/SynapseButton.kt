package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.ui.theme.SynapseShapes
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

enum class SynapseButtonVariant { Primary, Outline, Ghost, Danger }
enum class SynapseButtonSize(val height: Dp) {
    Sm(32.dp),
    Md(40.dp),
    Lg(48.dp),
}

/**
 * Themed button. Variants:
 *  - Primary: Accent fill, dark foreground.
 *  - Outline: transparent, 1dp Line border.
 *  - Ghost:   transparent, no border, muted foreground.
 *  - Danger:  Danger fill, white foreground.
 *
 * Min hit target is 44dp regardless of visual height.
 */
@Composable
fun SynapseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SynapseButtonVariant = SynapseButtonVariant.Primary,
    size: SynapseButtonSize = SynapseButtonSize.Md,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = SynapseTheme.colors
    val padding = PaddingValues(horizontal = 16.dp)
    val content: @Composable () -> Unit = {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null)
            Spacer(Modifier.width(Tokens.Space.Sm))
        }
        Text(text = text, style = SynapseText.BodyM.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.W600))
    }
    val rowMod = Modifier
        .heightIn(min = 44.dp)
        .defaultMinSize(minHeight = 44.dp)
    when (variant) {
        SynapseButtonVariant.Primary -> Button(
            onClick = onClick,
            enabled = enabled,
            shape = SynapseShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = c.accent,
                contentColor = if (c.isLight) Color.White else Tokens.Color.Dark.Bg,
                disabledContainerColor = c.surface3,
                disabledContentColor = c.textDim,
            ),
            contentPadding = padding,
            modifier = modifier.then(rowMod),
        ) { content() }

        SynapseButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = SynapseShapes.medium,
            border = BorderStroke(1.dp, c.line),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = c.text,
                disabledContentColor = c.textDim,
            ),
            contentPadding = padding,
            modifier = modifier.then(rowMod),
        ) { content() }

        SynapseButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            enabled = enabled,
            shape = SynapseShapes.medium,
            colors = ButtonDefaults.textButtonColors(
                contentColor = c.textMuted,
                disabledContentColor = c.textDim,
            ),
            contentPadding = padding,
            modifier = modifier.then(rowMod),
        ) { content() }

        SynapseButtonVariant.Danger -> Button(
            onClick = onClick,
            enabled = enabled,
            shape = SynapseShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = c.danger,
                contentColor = Color.White,
                disabledContainerColor = c.surface3,
                disabledContentColor = c.textDim,
            ),
            contentPadding = padding,
            modifier = modifier.then(rowMod),
        ) { content() }
    }
}
