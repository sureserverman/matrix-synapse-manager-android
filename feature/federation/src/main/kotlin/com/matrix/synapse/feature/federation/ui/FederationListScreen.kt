package com.matrix.synapse.feature.federation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.Spacing
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.DestructiveDialog
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.feature.federation.data.FederationDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederationListScreen(
    serverUrl: String,
    serverId: String,
    onDestinationClick: (destination: String) -> Unit,
    onServers: () -> Unit = {},
    onBack: (() -> Unit)? = {},
    viewModel: FederationListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.init(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Track which destination's reset dialog is open.
    // TODO: FederationListViewModel does not expose a resetConnection method.
    // Wire this to the VM once a reset action is added to FederationListViewModel.
    var resetTarget by remember { mutableStateOf<String?>(null) }

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.federation),
                subtitle = serverUrl,
                onTitleClick = onServers,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { /* TODO: open search */ }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_users),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.destinations.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag("federation_list_loading"),
                )
            }

            state.error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.ScreenPadding),
                contentAlignment = Alignment.Center,
            ) {
                SynapseEmptyState(
                    title = state.error!!,
                    errorTone = true,
                    modifier = Modifier.testTag("federation_list_error"),
                )
            }

            state.destinations.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                SynapseEmptyState(
                    title = "No federation destinations",
                    icon = Icons.Filled.Place,
                    body = "Federation traffic will appear here.",
                )
            }

            else -> {
                val listState = rememberLazyListState()

                LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
                    val lastVisible =
                        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
                    if (lastVisible >= state.destinations.size - 5 && state.hasMore && !state.isLoadingMore) {
                        viewModel.loadNextPage()
                    }
                }

                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("federation_list"),
                    ) {
                        items(state.destinations, key = { it.destination }) { dest ->
                            DestinationRow(
                                dest = dest,
                                onClick = { onDestinationClick(dest.destination) },
                                onResetRequest = { resetTarget = dest.destination },
                            )
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.ScreenPadding),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val target = resetTarget
    if (target != null) {
        DestructiveDialog(
            title = stringResource(R.string.reset_connection),
            body = stringResource(R.string.reset_connection_message, target),
            confirmLabel = stringResource(R.string.reset),
            onConfirm = {
                resetTarget = null
                // TODO: call viewModel.resetConnection(target) once added to FederationListViewModel
            },
            onDismiss = { resetTarget = null },
            dismissLabel = stringResource(R.string.cancel),
        )
    }
}

@Composable
private fun DestinationRow(
    dest: FederationDestination,
    onClick: () -> Unit,
    onResetRequest: () -> Unit,
) {
    val c = SynapseTheme.colors

    val (statusLabel, statusTone) = when {
        dest.failureTs == null -> stringResource(R.string.healthy) to StatusTone.Success
        dest.retryInterval > 0 -> stringResource(R.string.retrying) to StatusTone.Warn
        else -> stringResource(R.string.failing) to StatusTone.Danger
    }

    val supportingText = if (dest.failureTs != null) {
        stringResource(R.string.retry_interval, formatInterval(dest.retryInterval))
    } else {
        stringResource(R.string.healthy)
    }

    var menuExpanded by remember { mutableStateOf(false) }

    SynapseListItem(
        headline = dest.destination,
        supporting = supportingText,
        leading = {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = c.textMuted,
                modifier = Modifier.size(20.dp),
            )
        },
        trailing = {
            StatusChip(
                text = statusLabel,
                tone = statusTone,
                showDot = true,
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = c.textMuted,
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
                                color = c.danger,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onResetRequest()
                        },
                    )
                }
            }
        },
        onClick = onClick,
        modifier = Modifier.testTag("federation_row_${dest.destination}"),
    )
}

private fun formatInterval(ms: Long): String = when {
    ms == 0L -> "none"
    ms < 60_000 -> "${ms / 1000}s"
    ms < 3_600_000 -> "${ms / 60_000}m"
    ms < 86_400_000 -> "${ms / 3_600_000}h"
    else -> "${ms / 86_400_000}d"
}
