package com.matrix.synapse.feature.federation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.DestructiveDialog
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import com.matrix.synapse.feature.federation.data.FederationDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederationDetailScreen(
    serverUrl: String,
    serverId: String,
    destination: String,
    onRoomClick: ((roomId: String) -> Unit)? = null,
    onBack: () -> Unit = {},
    viewModel: FederationDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(destination) { viewModel.loadDestination(serverUrl, serverId, destination) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = destination,
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.reset_connection),
                                        style = SynapseText.BodyM,
                                        color = SynapseTheme.colors.danger,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showResetDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading && state.destination == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.destination == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                SynapseEmptyState(
                    title = state.error!!,
                    errorTone = true,
                )
            }

            state.destination != null -> {
                val dest = state.destination!!

                val (statusLabel, statusTone) = when {
                    dest.failureTs == null -> stringResource(R.string.healthy) to StatusTone.Success
                    dest.retryInterval > 0 -> stringResource(R.string.retrying) to StatusTone.Warn
                    else -> stringResource(R.string.failing) to StatusTone.Danger
                }

                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.loadDestination(serverUrl, serverId, destination) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        // Header card: icon, host, last-seen, status chip
                        item {
                            Box(modifier = Modifier.padding(Tokens.Space.ScreenEdge)) {
                                SynapseCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(SynapseTheme.colors.surface2),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Place,
                                                contentDescription = null,
                                                tint = SynapseTheme.colors.textMuted,
                                                modifier = Modifier.size(32.dp),
                                            )
                                        }
                                        Text(
                                            text = dest.destination,
                                            style = SynapseText.Title,
                                            color = SynapseTheme.colors.text,
                                        )
                                        if (dest.retryLastTs > 0) {
                                            Text(
                                                text = stringResource(R.string.last_seen, formatTimestamp(dest.retryLastTs)),
                                                style = SynapseText.BodyS,
                                                color = SynapseTheme.colors.textMuted,
                                            )
                                        }
                                        StatusChip(
                                            text = statusLabel,
                                            tone = statusTone,
                                            showDot = true,
                                        )
                                    }
                                }
                            }
                        }

                        // Statistics section
                        item {
                            SectionHeader(text = stringResource(R.string.connection_info))
                        }
                        item {
                            Box(modifier = Modifier.padding(horizontal = Tokens.Space.ScreenEdge)) {
                                SynapseCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    padding = 0.dp,
                                ) {
                                    Column {
                                        SynapseListItem(
                                            headline = "First Failure",
                                            trailing = {
                                                Text(
                                                    text = if (dest.failureTs != null) formatTimestamp(dest.failureTs) else "—",
                                                    style = SynapseText.Mono,
                                                    color = SynapseTheme.colors.textMuted,
                                                )
                                            },
                                        )
                                        SynapseListItem(
                                            headline = "Last Retry",
                                            trailing = {
                                                Text(
                                                    text = if (dest.retryLastTs > 0) formatTimestamp(dest.retryLastTs) else "—",
                                                    style = SynapseText.Mono,
                                                    color = SynapseTheme.colors.textMuted,
                                                )
                                            },
                                        )
                                        SynapseListItem(
                                            headline = "Retry Interval",
                                            trailing = {
                                                Text(
                                                    text = formatInterval(dest.retryInterval),
                                                    style = SynapseText.Mono,
                                                    color = SynapseTheme.colors.textMuted,
                                                )
                                            },
                                            showDivider = false,
                                        )
                                    }
                                }
                            }
                        }

                        // Shared rooms
                        item {
                            SectionHeader(
                                text = stringResource(R.string.shared_rooms_count, state.totalRooms),
                            )
                        }
                        item {
                            Box(modifier = Modifier.padding(horizontal = Tokens.Space.ScreenEdge)) {
                                SynapseCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    padding = 0.dp,
                                ) {
                                    Column {
                                        state.rooms.forEachIndexed { index, room ->
                                            SynapseListItem(
                                                headline = room.roomId,
                                                supportingMono = true,
                                                showDivider = index < state.rooms.lastIndex || state.hasMoreRooms,
                                                onClick = if (onRoomClick != null) {
                                                    { onRoomClick(room.roomId) }
                                                } else {
                                                    null
                                                },
                                                modifier = Modifier.testTag("federation_room_${room.roomId}"),
                                            )
                                        }
                                        if (state.hasMoreRooms) {
                                            SynapseListItem(
                                                headline = stringResource(R.string.load_more_rooms),
                                                showDivider = false,
                                                onClick = { viewModel.loadMoreRooms(destination) },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Destructive section
                        item {
                            SectionHeader(text = "Destructive")
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = Tokens.Space.ScreenEdge)
                                    .padding(bottom = Tokens.Space.Xl),
                            ) {
                                SynapseCard(modifier = Modifier.fillMaxWidth()) {
                                    SynapseButton(
                                        text = stringResource(R.string.reset_connection),
                                        onClick = { showResetDialog = true },
                                        variant = SynapseButtonVariant.Danger,
                                        enabled = !state.isResetting,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        DestructiveDialog(
            title = stringResource(R.string.reset_connection),
            body = stringResource(R.string.reset_connection_message, destination),
            confirmLabel = stringResource(R.string.reset),
            onConfirm = {
                showResetDialog = false
                viewModel.resetConnection(destination)
            },
            onDismiss = { showResetDialog = false },
            dismissLabel = stringResource(R.string.cancel),
        )
    }
}

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}

private fun formatInterval(ms: Long): String = when {
    ms == 0L -> "none"
    ms < 60_000 -> "${ms / 1000}s"
    ms < 3_600_000 -> "${ms / 60_000}m"
    ms < 86_400_000 -> "${ms / 3_600_000}h"
    else -> "${ms / 86_400_000}d"
}
