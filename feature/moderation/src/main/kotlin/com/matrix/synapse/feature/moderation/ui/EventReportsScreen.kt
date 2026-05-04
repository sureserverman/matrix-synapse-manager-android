package com.matrix.synapse.feature.moderation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.matrix.synapse.core.ui.SynapseTopBar
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
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import com.matrix.synapse.feature.moderation.data.EventReportSummary
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.users.data.UserSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventReportsScreen(
    serverId: String,
    serverUrl: String,
    onReportClick: (reportId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EventReportsViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.load(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var roomDropdownExpanded by remember { mutableStateOf(false) }
    var userDropdownExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (last >= state.reports.size - 3 && state.hasMore && !state.isLoadingMore) {
            viewModel.loadNextPage()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.event_reports_title, state.total),
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter card
            SynapseCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Tokens.Space.ScreenEdge,
                        vertical = Tokens.Space.Sm,
                    ),
                padding = Tokens.Space.Md,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Md),
                    ) {
                        RoomFilterDropdown(
                            rooms = state.rooms,
                            roomsLoading = state.roomsLoading,
                            selectedRoomId = state.filterRoomId.takeIf { it.isNotBlank() },
                            expanded = roomDropdownExpanded,
                            onExpandedChange = { roomDropdownExpanded = it; if (it) userDropdownExpanded = false },
                            onRoomSelected = { viewModel.setFilters(it ?: "", state.filterUserId) },
                            modifier = Modifier.weight(1f),
                        )
                        UserFilterDropdown(
                            users = state.users,
                            usersLoading = state.usersLoading,
                            selectedUserId = state.filterUserId.takeIf { it.isNotBlank() },
                            expanded = userDropdownExpanded,
                            onExpandedChange = { userDropdownExpanded = it; if (it) roomDropdownExpanded = false },
                            onUserSelected = { viewModel.setFilters(state.filterRoomId, it ?: "") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    SynapseButton(
                        text = if (state.sortNewestFirst) {
                            stringResource(R.string.newest_first)
                        } else {
                            stringResource(R.string.oldest_first)
                        },
                        onClick = { viewModel.setSortNewestFirst(!state.sortNewestFirst) },
                        variant = SynapseButtonVariant.Ghost,
                    )
                }
            }

            when {
                state.isLoading && state.reports.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("reports_loading")) }

                state.reports.isEmpty() && !state.isLoading -> SynapseEmptyState(
                    title = "No reports",
                    icon = Icons.Filled.CheckCircle,
                    body = "Nothing flagged.",
                )

                else -> PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.load(serverId, serverUrl) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("reports_list"),
                    ) {
                        items(state.reports, key = { it.id }) { report ->
                            EventReportRow(
                                report = report,
                                onClick = { onReportClick(report.id) },
                            )
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(Tokens.Space.Lg),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator(modifier = Modifier.padding(Tokens.Space.Sm)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventReportRow(
    report: EventReportSummary,
    onClick: () -> Unit,
) {
    val c = SynapseTheme.colors
    val roomDisplay = report.name?.takeIf { it.isNotBlank() } ?: report.canonicalAlias ?: report.roomId
    val supporting = buildString {
        append(report.sender)
        append(" · ")
        append(roomDisplay)
        append(" · ")
        append(formatTs(report.receivedTs))
    }

    SynapseListItem(
        headline = report.reason ?: report.eventId,
        supporting = supporting,
        supportingMono = false,
        leading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Tokens.Radius.Sm))
                    .background(c.dangerBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = c.danger,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailing = if (report.score != null && report.score > 1) {
            { StatusChip(text = "${report.score}", tone = StatusTone.Danger) }
        } else {
            null
        },
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_row_${report.id}"),
    )
}

private fun formatTs(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomFilterDropdown(
    rooms: List<RoomSummary>,
    roomsLoading: Boolean,
    selectedRoomId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRoomSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRoom = rooms.find { it.roomId == selectedRoomId }
    val label = when {
        selectedRoomId == null -> stringResource(R.string.all_rooms)
        selectedRoom != null -> (selectedRoom.name?.takeIf { it.isNotBlank() } ?: selectedRoom.roomId)
        else -> selectedRoomId
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.testTag("filter_room"),
    ) {
        OutlinedTextField(
            value = if (roomsLoading && rooms.isEmpty()) stringResource(R.string.loading) else label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.room_filter)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_rooms)) },
                onClick = { onRoomSelected(null); onExpandedChange(false) },
            )
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = { Text(room.name?.takeIf { it.isNotBlank() } ?: room.roomId, maxLines = 1) },
                    onClick = { onRoomSelected(room.roomId); onExpandedChange(false) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFilterDropdown(
    users: List<UserSummary>,
    usersLoading: Boolean,
    selectedUserId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onUserSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedUser = users.find { it.userId == selectedUserId }
    val label = when {
        selectedUserId == null -> stringResource(R.string.all_reporters)
        selectedUser != null -> (selectedUser.displayName?.takeIf { it.isNotBlank() } ?: selectedUser.userId)
        else -> selectedUserId
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.testTag("filter_user"),
    ) {
        OutlinedTextField(
            value = if (usersLoading && users.isEmpty()) stringResource(R.string.loading) else label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.reporter)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_reporters)) },
                onClick = { onUserSelected(null); onExpandedChange(false) },
            )
            users.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.displayName?.takeIf { it.isNotBlank() } ?: user.userId, maxLines = 1) },
                    onClick = { onUserSelected(user.userId); onExpandedChange(false) },
                )
            }
        }
    }
}
