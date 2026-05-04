package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

/**
 * Three-row list item with leading slot, headline, supporting text, and trailing slot.
 *
 * - 56dp single-line, ~72dp two-line.
 * - Leading slot is a 40dp Surface3 square with a Line border.
 * - Use [supportingMono] = true to render the supporting text in JetBrains Mono.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SynapseListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingMono: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    leadingShape: Shape = RoundedCornerShape(Tokens.Radius.Sm),
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    showDivider: Boolean = true,
) {
    val c = SynapseTheme.colors
    val baseClickable = when {
        onClick != null && onLongClick != null -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) c.accentBg else Color.Transparent)
                .then(baseClickable)
                .heightIn(min = if (supporting != null) 72.dp else 56.dp)
                .padding(horizontal = Tokens.Space.Lg, vertical = Tokens.Space.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(leadingShape)
                        .background(c.surface3)
                        .border(BorderStroke(1.dp, c.line), leadingShape),
                    contentAlignment = Alignment.Center,
                ) { leading() }
                Spacer(Modifier.width(Tokens.Space.Md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = SynapseText.BodyM,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = if (supportingMono) SynapseText.Mono else SynapseText.BodyS,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(Tokens.Space.Md))
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = c.line,
                thickness = 1.dp,
                modifier = Modifier.padding(start = Tokens.Space.Lg),
            )
        }
    }
}
