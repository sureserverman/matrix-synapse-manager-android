package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrix.synapse.core.ui.theme.SynapseShapes
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

/**
 * KPI tile for the dashboard. Surface2 background, 1dp Line border, 12dp padding.
 * Value renders at 24sp / 700; pass [valueMono] = true for counts/durations.
 */
@Composable
fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMono: Boolean = true,
    delta: String? = null,
    deltaPositive: Boolean = true,
) {
    val c = SynapseTheme.colors
    Column(
        modifier = modifier
            .clip(SynapseShapes.medium)
            .background(c.surface2)
            .border(BorderStroke(1.dp, c.line), SynapseShapes.medium)
            .padding(Tokens.Space.Md),
    ) {
        Text(
            text = label.uppercase(),
            style = SynapseText.Label,
            color = c.textMuted,
        )
        Spacer(Modifier.height(Tokens.Space.Xs))
        Text(
            text = value,
            style = if (valueMono) SynapseText.Mono.copy(fontSize = 24.sp, fontWeight = FontWeight.W700, lineHeight = 28.sp)
            else SynapseText.Display.copy(fontSize = 24.sp, lineHeight = 28.sp),
            color = c.text,
        )
        if (delta != null) {
            Text(
                text = delta,
                style = SynapseText.BodyS,
                color = if (deltaPositive) c.success else c.danger,
            )
        }
    }
}
