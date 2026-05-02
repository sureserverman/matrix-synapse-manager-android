package com.matrix.synapse.feature.media.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.matrix.synapse.core.ui.Spacing
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.users.data.UserSummary
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.matrix.synapse.core.resources.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaListScreen(
    serverUrl: String,
    serverId: String,
    filterUserId: String? = null,
    filterRoomId: String? = null,
    onMediaClick: (serverName: String, mediaId: String) -> Unit,
    onServers: () -> Unit = {},
    onBack: (() -> Unit)? = {},
    viewModel: MediaListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverUrl) { viewModel.init(serverUrl, serverId, filterUserId, filterRoomId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var roomDropdownExpanded by remember { mutableStateOf(false) }
    var userDropdownExpanded by remember { mutableStateOf(false) }
    var showUserScopedDeleteDialog by remember { mutableStateOf(false) }
    var showRoomScopedDeleteDialog by remember { mutableStateOf(false) }
    var userFromTsText by remember { mutableStateOf("") }
    var userUntilTsText by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun resolveStringResMessage(msg: StringResMessage): String =
        if (msg.formatArgs.isEmpty()) {
            context.getString(msg.resId)
        } else {
            context.getString(msg.resId, *msg.formatArgs.toTypedArray())
        }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(resolveStringResMessage(it))
            viewModel.clearActionMessage()
        }
    }

    LaunchedEffect(state.userError) {
        state.userError?.let {
            snackbarHostState.showSnackbar(resolveStringResMessage(it))
            viewModel.clearUserError()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading && state.mediaItems.isEmpty() && state.error == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("media_list_loading"))
                }
            }

            state.error != null && state.mediaItems.isEmpty() -> {
                Card(
                    modifier = Modifier
                        .padding(padding)
                        .padding(Spacing.ScreenPadding)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = state.error!!,
                        modifier = Modifier.padding(Spacing.FieldSpacing).testTag("media_list_error"),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("media_list"),
                        contentPadding = PaddingValues(
                            horizontal = Spacing.ScreenPadding,
                            vertical = Spacing.TightSpacing,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.FieldSpacing),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.media_actions_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(Spacing.FieldSpacing),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.FieldSpacing),
                                ) {
                                    Text(
                                        text = stringResource(R.string.media_section_scope),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    RoomDropdown(
                                        rooms = state.rooms,
                                        roomsLoading = state.roomsLoading,
                                        selectedRoomId = state.selectedRoomId,
                                        expanded = roomDropdownExpanded,
                                        onExpandedChange = {
                                            roomDropdownExpanded = it
                                            if (it) userDropdownExpanded = false
                                        },
                                        onRoomSelected = { viewModel.selectRoom(it) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    UserDropdown(
                                        users = state.users,
                                        usersLoading = state.usersLoading,
                                        selectedUserId = state.selectedUserId,
                                        expanded = userDropdownExpanded,
                                        onExpandedChange = {
                                            userDropdownExpanded = it
                                            if (it) roomDropdownExpanded = false
                                        },
                                        onUserSelected = { viewModel.selectUser(it) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        if (state.selectedUserId != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(Spacing.FieldSpacing),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.TightSpacing),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.media_section_time_filter),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = stringResource(R.string.media_user_created_filter_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        OutlinedTextField(
                                            value = userFromTsText,
                                            onValueChange = { userFromTsText = it },
                                            label = { Text(stringResource(R.string.media_from_ts)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        OutlinedTextField(
                                            value = userUntilTsText,
                                            onValueChange = { userUntilTsText = it },
                                            label = { Text(stringResource(R.string.media_until_ts)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.TightSpacing),
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.setUserMediaDateRange(
                                                        userFromTsText.toLongOrNull(),
                                                        userUntilTsText.toLongOrNull(),
                                                    )
                                                },
                                            ) { Text(stringResource(R.string.media_apply_date_filter)) }
                                            TextButton(onClick = {
                                                userFromTsText = ""
                                                userUntilTsText = ""
                                                viewModel.setUserMediaDateRange(null, null)
                                            }) { Text(stringResource(R.string.media_clear_date_filter)) }
                                        }
                                    }
                                }
                            }
                        }

                        if (state.selectedRoomId != null || state.selectedUserId != null) {
                            item {
                                MediaListToolbar(
                                    selectedKeysNonEmpty = state.selectedKeys.isNotEmpty(),
                                    showDeleteUser = state.selectedUserId != null,
                                    showDeleteRoom = state.selectedRoomId != null,
                                    roomDeleteEnabled = !state.isLoading && state.mediaItems.isNotEmpty(),
                                    userDeleteEnabled = !state.isLoading,
                                    onClearSelection = { viewModel.clearSelection() },
                                    onDeleteSelected = { viewModel.deleteSelectedMedia() },
                                    onDeleteUserMedia = { showUserScopedDeleteDialog = true },
                                    onDeleteRoomMedia = { showRoomScopedDeleteDialog = true },
                                    modifier = Modifier.padding(top = Spacing.TightSpacing),
                                )
                            }
                        }

                        items(
                            items = state.mediaItems,
                            key = { m -> "${m.origin}\u0000${m.mediaId}" },
                        ) { mediaItem ->
                            val selected = mediaItem.stableKey() in state.selectedKeys
                            MediaListItemCard(
                                item = mediaItem,
                                selected = selected,
                                onClick = { viewModel.toggleSelection(mediaItem) },
                                onLongClick = { onMediaClick(mediaItem.origin, mediaItem.mediaId) },
                            )
                        }

                        if (state.mediaItems.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                                ) {
                                    val emptyMessage = when {
                                        filterRoomId != null || filterUserId != null -> stringResource(R.string.no_media_found)
                                        state.selectedRoomId == null && state.selectedUserId == null ->
                                            stringResource(R.string.select_room_or_user_to_list_media)
                                        else -> stringResource(R.string.no_media_found)
                                    }
                                    Text(
                                        emptyMessage,
                                        modifier = Modifier
                                            .padding(Spacing.FieldSpacing)
                                            .testTag("media_list_empty"),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUserScopedDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showUserScopedDeleteDialog = false },
            title = { Text(stringResource(R.string.media_delete_user_confirm_title)) },
            text = { Text(stringResource(R.string.media_delete_user_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showUserScopedDeleteDialog = false
                        viewModel.bulkDeleteUserScopedMedia()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showUserScopedDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showRoomScopedDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showRoomScopedDeleteDialog = false },
            title = { Text(stringResource(R.string.media_delete_room_confirm_title)) },
            text = { Text(stringResource(R.string.media_delete_room_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRoomScopedDeleteDialog = false
                        viewModel.bulkDeleteRoomScopedMedia()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showRoomScopedDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun MediaListToolbar(
    selectedKeysNonEmpty: Boolean,
    showDeleteUser: Boolean,
    showDeleteRoom: Boolean,
    roomDeleteEnabled: Boolean,
    userDeleteEnabled: Boolean,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteUserMedia: () -> Unit,
    onDeleteRoomMedia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.error,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.media_section_list),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedKeysNonEmpty) {
                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier.testTag("media_clear_selection"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.media_cd_clear_selection),
                    )
                }
            }
            IconButton(
                onClick = onDeleteSelected,
                enabled = selectedKeysNonEmpty,
                modifier = Modifier.testTag("media_delete_selected"),
                colors = errorColors,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.media_cd_delete_selected),
                )
            }
            if (showDeleteUser) {
                IconButton(
                    onClick = onDeleteUserMedia,
                    enabled = userDeleteEnabled,
                    modifier = Modifier.testTag("media_delete_user_scoped"),
                    colors = errorColors,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = stringResource(R.string.media_cd_delete_user_media),
                    )
                }
            }
            if (showDeleteRoom) {
                IconButton(
                    onClick = onDeleteRoomMedia,
                    enabled = roomDeleteEnabled,
                    modifier = Modifier.testTag("media_delete_room_scoped"),
                    colors = errorColors,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MeetingRoom,
                        contentDescription = stringResource(R.string.media_cd_delete_room_media),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaListItemCard(
    item: MediaListItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .testTag("media_row_${item.mediaId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.FieldSpacing, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.mediaId,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.TightSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pillColor = if (item.isLocal) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
                val pillOnColor = if (item.isLocal) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pillColor,
                ) {
                    Text(
                        text = if (item.isLocal) {
                            stringResource(R.string.media_local)
                        } else {
                            stringResource(R.string.media_remote)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = pillOnColor,
                    )
                }
                Text(
                    text = item.origin,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomDropdown(
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
        selectedRoomId == null -> stringResource(R.string.select_room)
        selectedRoom != null -> (selectedRoom.name?.takeIf { it.isNotBlank() } ?: selectedRoom.roomId)
        else -> selectedRoomId
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.testTag("media_room_dropdown"),
    ) {
        OutlinedTextField(
            value = if (roomsLoading && rooms.isEmpty()) stringResource(R.string.loading) else label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.room)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.select_room)) },
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
private fun UserDropdown(
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
        selectedUserId == null -> stringResource(R.string.select_user)
        selectedUser != null -> (selectedUser.displayName?.takeIf { it.isNotBlank() } ?: selectedUser.userId)
        else -> selectedUserId
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.testTag("media_user_dropdown"),
    ) {
        OutlinedTextField(
            value = if (usersLoading && users.isEmpty()) stringResource(R.string.loading) else label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.user)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.select_user)) },
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
