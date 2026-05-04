package com.matrix.synapse.core.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.matrix.synapse.core.ui.theme.SynapseTheme

/**
 * Themed switch. Track Surface3 (off) / Accent (on); thumb white, no inner icon.
 */
@Composable
fun SynapseToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = SynapseTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = c.accent,
            checkedBorderColor = c.accent,
            checkedIconColor = c.accent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = c.surface3,
            uncheckedBorderColor = c.line,
            uncheckedIconColor = c.surface3,
            disabledCheckedThumbColor = Color.White,
            disabledCheckedTrackColor = c.surface3,
            disabledUncheckedThumbColor = Color.White,
            disabledUncheckedTrackColor = c.surface3,
        ),
        thumbContent = null,
    )
}
