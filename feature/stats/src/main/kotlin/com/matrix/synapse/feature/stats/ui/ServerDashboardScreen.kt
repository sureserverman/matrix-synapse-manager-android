package com.matrix.synapse.feature.stats.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.matrix.synapse.core.resources.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDashboardScreen(
    serverId: String,
    serverUrl: String,
    onServers: () -> Unit = {},
    onBack: (() -> Unit)? = {},
    onUsersClick: () -> Unit = {},
    onRoomsClick: () -> Unit = {},
    onRoomClick: (String) -> Unit = {},
    onOpenReportsClick: () -> Unit = {},
    onTopMediaUserClick: (userId: String) -> Unit = {},
    viewModel: ServerDashboardViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.loadDashboard(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = state.currentServer?.displayName ?: serverUrl,
                subtitle = serverUrl,
                onTitleClick = onServers,
                onBack = onBack,
                titleCentered = true,
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.serverVersion == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(24.dp),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadDashboard(serverId, serverUrl) },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Version
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.server_version), style = MaterialTheme.typography.titleMedium)
                                Text(state.serverVersion ?: "\u2014", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }

                    // Summary row (tappable → Users / Rooms)
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                label = stringResource(R.string.total_users),
                                value = state.totalUsers.toString(),
                                modifier = Modifier.weight(1f),
                                onClick = onUsersClick,
                            )
                            StatCard(
                                label = stringResource(R.string.total_rooms),
                                value = state.totalRooms.toString(),
                                modifier = Modifier.weight(1f),
                                onClick = onRoomsClick,
                            )
                        }
                    }

                    // Active users
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("DAU (24h)", state.dau.toString(), Modifier.weight(1f))
                            StatCard("MAU (30d)", state.mau.toString(), Modifier.weight(1f))
                        }
                    }

                    // Total media storage
                    item {
                        StatCard(
                            label = stringResource(R.string.total_media_storage),
                            value = state.totalMediaBytes?.let { formatBytes(it) } ?: "\u2014",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Federation
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.federation), style = MaterialTheme.typography.titleMedium)
                                val dest = state.federationDestinations
                                val fail = state.federationFailures
                                val text = when {
                                    dest == null -> "\u2014"
                                    fail != null && fail > 0 -> "$dest destinations ($fail failing)"
                                    else -> "$dest destinations"
                                }
                                Text(text, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    // Background updates
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.background_updates), style = MaterialTheme.typography.titleMedium)
                                val enabled = state.backgroundUpdatesEnabled
                                val job = state.backgroundUpdatesJobName
                                val text = when {
                                    enabled == null -> "\u2014"
                                    job != null && job.isNotBlank() -> "Running: $job"
                                    else -> if (enabled) "Idle" else "Disabled"
                                }
                                Text(text, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    // Open reports (tappable)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (state.openEventReportsCount != null)
                                        Modifier.clickable(onClick = onOpenReportsClick)
                                    else Modifier
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(R.string.open_reports), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    state.openEventReportsCount?.toString() ?: "\u2014",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }

                    // Top media users
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.top_media_users), style = MaterialTheme.typography.titleMedium)
                            if (state.topMediaUsers.isEmpty()) {
                                Text(stringResource(R.string.no_data), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                items(state.topMediaUsers, key = { it.userId }) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable(onClick = { onTopMediaUserClick(user.userId) })
                            .semantics {
                                contentDescription = "${user.displayname ?: user.userId}, ${user.mediaCount} files"
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayname ?: user.userId, style = MaterialTheme.typography.bodySmall)
                            if (user.displayname != null) Text(user.userId, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.files_count, user.mediaCount), style = MaterialTheme.typography.bodySmall)
                            Text(formatBytes(user.mediaLength), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}
