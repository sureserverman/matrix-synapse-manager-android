package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

enum class StatusTone { Success, Warn, Danger, Info, Neutral, Accent }

/** Pill-shaped status chip. 22dp tall, 8/4 padding, optional 6dp leading dot. */
@Composable
fun StatusChip(
    text: String,
    tone: StatusTone = StatusTone.Neutral,
    showDot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val c = SynapseTheme.colors
    val (fg, bg) = when (tone) {
        StatusTone.Success -> c.success to c.successBg
        StatusTone.Warn    -> c.warn    to c.warnBg
        StatusTone.Danger  -> c.danger  to c.dangerBg
        StatusTone.Info    -> c.info    to c.infoBg
        StatusTone.Accent  -> c.accent  to c.accentBg
        StatusTone.Neutral -> c.textMuted to c.surface2
    }
    Row(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Tokens.Radius.Pill))
            .background(bg)
            .padding(horizontal = Tokens.Space.Sm, vertical = Tokens.Space.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
            Spacer(Modifier.width(Tokens.Space.Xs))
        }
        Text(
            text = text,
            style = SynapseText.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.W600),
            color = fg,
        )
    }
}
