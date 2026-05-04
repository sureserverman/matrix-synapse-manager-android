package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme

/**
 * Default top app bar — Surface background, 1dp Line bottom border, no shadow.
 *
 * Pass [subtitle] for the small mono/muted line under the title (e.g. server URL).
 * Use `subtitleMono = true` if the subtitle is an identifier (server URL, MXID).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapseTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleMono: Boolean = false,
    onBack: (() -> Unit)? = null,
    titleCentered: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = SynapseTheme.colors
    val titleContent = @Composable {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (titleCentered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                text = title,
                style = SynapseText.Title,
                color = c.text,
                maxLines = 1,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = if (subtitleMono) SynapseText.Mono else SynapseText.BodyS,
                    color = c.textMuted,
                    maxLines = 1,
                )
            }
        }
    }
    val nav = @Composable {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = c.text,
                )
            }
        }
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = c.surface,
        titleContentColor = c.text,
        navigationIconContentColor = c.text,
        actionIconContentColor = c.textMuted,
    )
    Column(modifier = modifier) {
        if (titleCentered) {
            CenterAlignedTopAppBar(
                title = titleContent,
                navigationIcon = nav,
                actions = actions,
                colors = colors,
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        } else {
            TopAppBar(
                title = titleContent,
                navigationIcon = nav,
                actions = actions,
                colors = colors,
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        }
        HorizontalDivider(color = c.line, thickness = 1.dp)
    }
}

/**
 * Selection-mode top app bar. Background tints to AccentBg, leading is close,
 * title shows the selection count.
 *
 * Per spec: never three text actions stacked — show 1–2 icon actions plus a
 * `more_horiz` overflow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapseSelectionTopAppBar(
    selectedCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = SynapseTheme.colors
    Column(modifier = modifier) {
        TopAppBar(
            title = {
                Text(
                    text = "$selectedCount selected",
                    style = SynapseText.Title,
                    color = c.text,
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear selection",
                        tint = c.text,
                    )
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = c.accentBg,
                titleContentColor = c.text,
                navigationIconContentColor = c.text,
                actionIconContentColor = c.text,
            ),
            windowInsets = TopAppBarDefaults.windowInsets,
        )
        HorizontalDivider(color = c.line, thickness = 1.dp)
    }
}
