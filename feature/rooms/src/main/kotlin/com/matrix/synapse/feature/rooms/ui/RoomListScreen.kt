package com.matrix.synapse.feature.rooms.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.Spacing
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import com.matrix.synapse.feature.rooms.data.RoomSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    serverUrl: String,
    serverId: String,
    onRoomClick: (roomId: String) -> Unit,
    onServers: () -> Unit = {},
    onBack: (() -> Unit)? = {},
    viewModel: RoomListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.init(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteRoomsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    if (showDeleteRoomsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRoomsDialog = false },
            title = { Text(stringResource(R.string.delete_rooms_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Md)) {
                    Text(
                        stringResource(R.string.delete_rooms_message, state.selectedRoomIds.size),
                        style = SynapseText.BodyM,
                    )
                    SynapseButton(
                        text = stringResource(R.string.delete),
                        onClick = {
                            viewModel.deleteSelectedRooms(purge = false)
                            showDeleteRoomsDialog = false
                        },
                        variant = SynapseButtonVariant.Outline,
                        enabled = !state.isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SynapseButton(
                        text = stringResource(R.string.delete_with_media),
                        onClick = {
                            viewModel.deleteSelectedRooms(purge = true)
                            showDeleteRoomsDialog = false
                        },
                        variant = SynapseButtonVariant.Danger,
                        enabled = !state.isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SynapseButton(
                        text = stringResource(R.string.cancel),
                        onClick = { showDeleteRoomsDialog = false },
                        variant = SynapseButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = { },
            dismissButton = { },
        )
    }

    SynapseScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SynapseTopBar(
                title = when {
                    state.selectionMode -> stringResource(R.string.selected_count, state.selectedRoomIds.size)
                    else -> state.currentServer?.displayName ?: serverUrl
                },
                subtitle = when {
                    state.selectionMode -> null
                    state.totalRooms > 0 -> stringResource(R.string.rooms_count, serverUrl, state.totalRooms)
                    else -> serverUrl
                },
                onTitleClick = if (state.selectionMode) null else onServers,
                titleCentered = true,
                onBack = when {
                    state.selectionMode -> { { viewModel.exitSelectionMode() } }
                    else -> onBack
                },
                actions = {
                    if (state.selectionMode) {
                        IconButton(
                            onClick = { showDeleteRoomsDialog = true },
                            enabled = !state.isDeleting,
                            modifier = Modifier.testTag("room_selection_delete"),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_selected_rooms))
                        }
                        TextButton(onClick = { viewModel.exitSelectionMode() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    } else {
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q -> searchQuery = q; viewModel.search(q) },
                label = { Text(stringResource(R.string.search_rooms)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.Space.ScreenEdge, vertical = Tokens.Space.Md)
                    .testTag("room_search"),
            )
            RoomSortDropdown(
                sortBy = state.sortBy,
                sortDir = state.sortDir,
                onSortChange = { orderBy, dir -> viewModel.setSort(orderBy, dir) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.Space.ScreenEdge, vertical = Tokens.Space.Xs)
                    .testTag("room_sort"),
            )
            when {
                state.isLoading && state.rooms.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("room_list_loading")) }

                state.error != null -> Text(
                    text = state.error!!,
                    color = SynapseTheme.colors.danger,
                    style = SynapseText.BodyM,
                    modifier = Modifier
                        .padding(Spacing.ScreenPadding)
                        .testTag("room_list_error"),
                )

                state.rooms.isEmpty() -> PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    SynapseEmptyState(
                        title = stringResource(R.string.no_rooms),
                        body = stringResource(R.string.no_rooms_body),
                        icon = Icons.Filled.Home,
                        modifier = Modifier.testTag("room_list_empty"),
                    )
                }

                else -> PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    RoomList(
                        rooms = state.rooms,
                        roomAvatarUrls = state.roomAvatarUrls,
                        hasMore = state.hasMore,
                        isLoadingMore = state.isLoadingMore,
                        selectionMode = state.selectionMode,
                        selectedRoomIds = state.selectedRoomIds,
                        onRoomClick = onRoomClick,
                        onLoadMore = { viewModel.loadNextPage() },
                        onRoomLongPress = { viewModel.enterSelectionMode(it) },
                        onToggleRoomSelection = { viewModel.toggleRoomSelection(it) },
                        onSelectAll = { viewModel.selectAllRooms() },
                        onClearSelection = { viewModel.clearRoomSelection() },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomSortDropdown(
    sortBy: String,
    sortDir: String,
    onSortChange: (orderBy: String, dir: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SynapseTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val labelResId = remember(sortBy, sortDir) {
        when (sortBy) {
            "name" -> if (sortDir == "f") R.string.name_az else R.string.name_za
            "joined_members" -> if (sortDir == "f") R.string.members_most_first else R.string.members_least_first
            "state_events" -> if (sortDir == "b") R.string.state_events_most_first else R.string.state_events_least_first
            else -> R.string.sort
        }
    }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.FieldSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.sort_by),
                    style = SynapseText.Caption,
                    color = c.textMuted,
                )
                Text(
                    stringResource(labelResId),
                    style = SynapseText.BodyM,
                    color = c.text,
                )
            }
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = c.textMuted,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                Triple("name", "f", R.string.name_az),
                Triple("name", "b", R.string.name_za),
                Triple("joined_members", "f", R.string.members_most_first),
                Triple("joined_members", "b", R.string.members_least_first),
                Triple("state_events", "b", R.string.state_events_most_first),
                Triple("state_events", "f", R.string.state_events_least_first),
            ).forEach { (orderBy, dir, resId) ->
                DropdownMenuItem(
                    text = { Text(stringResource(resId)) },
                    onClick = { onSortChange(orderBy, dir); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun RoomList(
    rooms: List<RoomSummary>,
    roomAvatarUrls: Map<String, String>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    selectionMode: Boolean,
    selectedRoomIds: Set<String>,
    onRoomClick: (roomId: String) -> Unit,
    onLoadMore: () -> Unit,
    onRoomLongPress: (roomId: String) -> Unit,
    onToggleRoomSelection: (roomId: String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
) {
    val listState = rememberLazyListState()
    val allLoadedSelected = rooms.isNotEmpty() && rooms.all { it.roomId in selectedRoomIds }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (lastVisible >= rooms.size - 5 && hasMore && !isLoadingMore) onLoadMore()
    }

    LazyColumn(state = listState, modifier = Modifier.testTag("room_list")) {
        if (selectionMode) {
            item(key = "select_all") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (allLoadedSelected) onClearSelection() else onSelectAll()
                        }
                        .padding(horizontal = Spacing.ScreenPadding, vertical = Tokens.Space.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.TightSpacing),
                ) {
                    Checkbox(
                        checked = allLoadedSelected,
                        onCheckedChange = { if (it) onSelectAll() else onClearSelection() },
                        modifier = Modifier.testTag("room_select_all"),
                    )
                    Text(stringResource(R.string.select_all), style = SynapseText.BodyM)
                }
                HorizontalDivider(color = SynapseTheme.colors.line, thickness = 1.dp)
            }
        }
        items(rooms, key = { it.roomId }) { room ->
            RoomRow(
                room = room,
                avatarUrl = roomAvatarUrls[room.roomId],
                selectionMode = selectionMode,
                selected = room.roomId in selectedRoomIds,
                onClick = { onRoomClick(room.roomId) },
                onLongPress = { onRoomLongPress(room.roomId) },
                onToggleSelection = { onToggleRoomSelection(room.roomId) },
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.FieldSpacing),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun RoomRow(
    room: RoomSummary,
    avatarUrl: String?,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    val c = SynapseTheme.colors
    val isBlocked = room.joinRules?.lowercase() == "knock_restricted" || room.joinRules?.lowercase() == "private"
    val isInviteOnly = room.joinRules?.lowercase() == "invite"

    SynapseListItem(
        headline = room.name ?: room.roomId,
        supporting = "${room.roomId} · ${room.joinedMembers}",
        supportingMono = true,
        selected = selected,
        leadingShape = CircleShape,
        leading = {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.testTag("room_checkbox_${room.roomId}"),
                )
            } else if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                when {
                    isBlocked -> Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = c.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    isInviteOnly -> Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = c.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    else -> Text(
                        text = "#",
                        style = SynapseText.Title,
                        color = c.textMuted,
                    )
                }
            }
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Xs),
            ) {
                if (room.encryption != null) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = c.textMuted,
                        modifier = Modifier.size(12.dp),
                    )
                }
                val (chipLabel, chipTone) = joinRuleChip(room.joinRules)
                StatusChip(text = chipLabel, tone = chipTone)
            }
        },
        onClick = if (selectionMode) onToggleSelection else onClick,
        onLongClick = onLongPress,
        modifier = Modifier
            .pointerInput(selectionMode, room.roomId) {
                detectTapGestures(
                    onTap = { if (selectionMode) onToggleSelection() else onClick() },
                    onLongPress = { onLongPress() },
                )
            }
            .testTag("room_row_${room.roomId}"),
    )
}

private fun joinRuleChip(joinRules: String?): Pair<String, StatusTone> = when (joinRules?.lowercase()) {
    "public"       -> "Public"    to StatusTone.Success
    "invite"       -> "Invite"    to StatusTone.Info
    "knock"        -> "Knock"     to StatusTone.Warn
    "restricted"   -> "Restricted" to StatusTone.Accent
    "knock_restricted" -> "Restricted" to StatusTone.Accent
    null           -> "Unknown"   to StatusTone.Neutral
    else           -> joinRules   to StatusTone.Neutral
}
