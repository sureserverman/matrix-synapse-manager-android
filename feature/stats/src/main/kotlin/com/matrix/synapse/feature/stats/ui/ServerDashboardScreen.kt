package com.matrix.synapse.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.KpiTile
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

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

    val serverDisplayName = state.currentServer?.displayName ?: serverUrl

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = serverDisplayName,
                subtitle = serverUrl,
                onTitleClick = onServers,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard(serverId, serverUrl) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = SynapseTheme.colors.textMuted,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = SynapseTheme.colors.textMuted,
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.serverVersion == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SynapseTheme.colors.accent)
            }

            state.error != null -> SynapseEmptyState(
                title = state.error!!,
                icon = Icons.Filled.Warning,
                errorTone = true,
                actionLabel = stringResource(R.string.loading),
                onAction = { viewModel.loadDashboard(serverId, serverUrl) },
                modifier = Modifier.padding(padding),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadDashboard(serverId, serverUrl) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = Tokens.Space.Xl),
                ) {
                    // ── OVERVIEW ──────────────────────────────────────────────────────
                    SectionHeader(text = "OVERVIEW")
                    Column(
                        modifier = Modifier.padding(horizontal = Tokens.Space.ScreenEdge),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                            KpiTile(
                                label = stringResource(R.string.total_users),
                                value = state.totalUsers.toString(),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onUsersClick),
                            )
                            KpiTile(
                                label = stringResource(R.string.total_rooms),
                                value = state.totalRooms.toString(),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onRoomsClick),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                            // Federation: show failing count if available, else "—"
                            val federationFailures = state.federationFailures
                            val federationValue = when {
                                federationFailures != null && federationFailures > 0 ->
                                    federationFailures.toString()
                                else -> "—"
                            }
                            KpiTile(
                                label = stringResource(R.string.federation),
                                value = federationValue,
                                modifier = Modifier.weight(1f),
                            )
                            // Storage: totalMediaBytes if available, else "—"
                            KpiTile(
                                label = stringResource(R.string.total_media_storage),
                                value = state.totalMediaBytes?.let { formatBytes(it) } ?: "—",
                                valueMono = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // ── SYSTEM ────────────────────────────────────────────────────────
                    SectionHeader(text = "SYSTEM")
                    SynapseCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Tokens.Space.ScreenEdge),
                        padding = 0.dp,
                    ) {
                        Column {
                            // Version row
                            SynapseListItem(
                                headline = stringResource(R.string.server_version),
                                trailing = {
                                    Text(
                                        text = state.serverVersion ?: "—",
                                        style = SynapseText.Mono,
                                        color = SynapseTheme.colors.textMuted,
                                    )
                                },
                                showDivider = true,
                            )
                            // DAU / MAU rows — exposed by the dashboard state
                            SynapseListItem(
                                headline = "DAU (24h)",
                                trailing = {
                                    Text(
                                        text = state.dau.toString(),
                                        style = SynapseText.Mono,
                                        color = SynapseTheme.colors.textMuted,
                                    )
                                },
                                showDivider = true,
                            )
                            SynapseListItem(
                                headline = "MAU (30d)",
                                trailing = {
                                    Text(
                                        text = state.mau.toString(),
                                        style = SynapseText.Mono,
                                        color = SynapseTheme.colors.textMuted,
                                    )
                                },
                                // No open reports row below — showDivider false on last item
                                showDivider = false,
                            )
                        }
                    }

                    // ── TOP MEDIA USERS ───────────────────────────────────────────────
                    SectionHeader(text = "TOP MEDIA USERS")
                    SynapseCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Tokens.Space.ScreenEdge),
                        padding = 0.dp,
                    ) {
                        Column {
                            if (state.topMediaUsers.isEmpty()) {
                                SynapseListItem(
                                    headline = stringResource(R.string.no_data),
                                    showDivider = false,
                                )
                            } else {
                                val displayUsers = state.topMediaUsers.take(3)
                                displayUsers.forEachIndexed { index, user ->
                                    val initial = (user.displayname ?: user.userId)
                                        .trimStart('@')
                                        .firstOrNull()
                                        ?.uppercaseChar()
                                        ?.toString()
                                        ?: "?"
                                    SynapseListItem(
                                        headline = user.displayname ?: user.userId,
                                        supporting = if (user.displayname != null) user.userId else null,
                                        supportingMono = true,
                                        leading = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(SynapseTheme.colors.surface3),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = initial,
                                                    style = SynapseText.Caption,
                                                    color = SynapseTheme.colors.textMuted,
                                                )
                                            }
                                        },
                                        trailing = {
                                            StatusChip(
                                                text = formatBytes(user.mediaLength),
                                                tone = StatusTone.Neutral,
                                            )
                                        },
                                        onClick = { onTopMediaUserClick(user.userId) },
                                        showDivider = index < displayUsers.lastIndex,
                                        modifier = Modifier.semantics {
                                            contentDescription =
                                                "${user.displayname ?: user.userId}, ${user.mediaCount} files"
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // ── BACKGROUND JOBS ───────────────────────────────────────────────
                    // Only shown when backgroundUpdatesEnabled is not null
                    if (state.backgroundUpdatesEnabled != null) {
                        SectionHeader(text = "BACKGROUND JOBS")
                        SynapseCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Tokens.Space.ScreenEdge),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Sm),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val enabled = state.backgroundUpdatesEnabled
                                val jobName = state.backgroundUpdatesJobName
                                when {
                                    jobName != null && jobName.isNotBlank() -> {
                                        StatusChip(
                                            text = "Running: $jobName",
                                            tone = StatusTone.Success,
                                            showDot = true,
                                        )
                                    }
                                    enabled == true -> {
                                        StatusChip(
                                            text = "Idle",
                                            tone = StatusTone.Neutral,
                                        )
                                    }
                                    else -> {
                                        StatusChip(
                                            text = "Disabled",
                                            tone = StatusTone.Warn,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}
