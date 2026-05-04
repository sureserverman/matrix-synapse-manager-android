package com.matrix.synapse.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme

/** Matches list content horizontal padding so top bar title aligns with screen content. */
private val TopBarHorizontalPadding = 20.dp

/**
 * Shared top app bar with Synapse Console styling — Surface background,
 * 1dp Line bottom border, no shadow.
 *
 * Pass server display name as [title], server URL as [subtitle],
 * and [onTitleClick] to open the server list (server switcher).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapseTopBar(
    title: String,
    subtitle: String? = null,
    onTitleClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleCentered: Boolean = false,
) {
    val c = SynapseTheme.colors
    val currentServerTapDesc = stringResource(R.string.current_server_tap)
    val backDesc = stringResource(R.string.back_nav)

    val titleContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TopBarHorizontalPadding),
        ) {
            if (onTitleClick != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTitleClick)
                        .semantics { contentDescription = currentServerTapDesc }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f, fill = true)) {
                        Text(
                            text = title,
                            style = SynapseText.Title,
                            color = c.text,
                            maxLines = 1,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = SynapseText.Mono,
                                color = c.textMuted,
                                maxLines = 1,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = c.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
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
                            style = SynapseText.BodyS,
                            color = c.textMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
    val navigationIcon = @Composable {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backDesc,
                    tint = c.text,
                )
            }
        }
    }
    val actionsContent: @Composable RowScope.() -> Unit = {
        Spacer(Modifier.width(8.dp))
        actions()
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = c.surface,
        titleContentColor = c.text,
        navigationIconContentColor = c.text,
        actionIconContentColor = c.textMuted,
    )
    val windowInsets = TopAppBarDefaults.windowInsets

    Column {
        if (titleCentered) {
            CenterAlignedTopAppBar(
                title = titleContent,
                navigationIcon = navigationIcon,
                actions = actionsContent,
                colors = colors,
                windowInsets = windowInsets,
            )
        } else {
            TopAppBar(
                title = titleContent,
                navigationIcon = navigationIcon,
                actions = actionsContent,
                colors = colors,
                windowInsets = windowInsets,
            )
        }
        HorizontalDivider(color = c.line, thickness = 1.dp)
    }
}
