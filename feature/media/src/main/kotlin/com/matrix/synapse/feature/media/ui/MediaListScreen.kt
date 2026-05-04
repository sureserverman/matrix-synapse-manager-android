package com.matrix.synapse.feature.media.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.matrix.synapse.core.ui.components.SynapseSelectionTopAppBar
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.users.data.UserSummary

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

    val selectionActive = state.selectedKeys.isNotEmpty()

    SynapseScaffold(
        topBar = {
            if (selectionActive) {
                SynapseSelectionTopAppBar(
                    selectedCount = state.selectedKeys.size,
                    onClose = { viewModel.clearSelection() },
                    actions = {
                        IconButton(
                            onClick = { viewModel.deleteSelectedMedia() },
                            modifier = Modifier.testTag("media_delete_selected"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.media_cd_delete_selected),
                            )
                        }
                    },
                )
            } else {
                SynapseTopBar(
                    title = "Media",
                    subtitle = serverUrl,
                    onTitleClick = onServers,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                            )
                        }
                    },
                )
            }
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
                SynapseEmptyState(
                    title = state.error!!,
                    icon = Icons.Filled.Info,
                    errorTone = true,
                    modifier = Modifier
                        .padding(padding)
                        .testTag("media_list_error"),
                )
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
                        contentPadding = PaddingValues(bottom = Tokens.Space.Xl),
                    ) {
                        // Constraint callout
                        item {
                            MediaConstraintCallout(
                                modifier = Modifier
                                    .padding(horizontal = Tokens.Space.ScreenEdge)
                                    .padding(top = Tokens.Space.Md),
                            )
                        }

                        // Scope filter card
                        item {
                            SectionHeader(text = stringResource(R.string.media_section_scope))
                            SynapseCard(
                                modifier = Modifier
                                    .padding(horizontal = Tokens.Space.ScreenEdge)
                                    .fillMaxWidth(),
                                padding = Tokens.Space.Lg,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Md)) {
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

                        // User date filter
                        if (state.selectedUserId != null) {
                            item {
                                SectionHeader(text = stringResource(R.string.media_section_time_filter))
                                SynapseCard(
                                    modifier = Modifier
                                        .padding(horizontal = Tokens.Space.ScreenEdge)
                                        .fillMaxWidth(),
                                    padding = Tokens.Space.Lg,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                                        Text(
                                            text = stringResource(R.string.media_user_created_filter_hint),
                                            style = SynapseText.BodyS,
                                            color = SynapseTheme.colors.textMuted,
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
                                        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                                            SynapseButton(
                                                text = stringResource(R.string.media_apply_date_filter),
                                                onClick = {
                                                    viewModel.setUserMediaDateRange(
                                                        userFromTsText.toLongOrNull(),
                                                        userUntilTsText.toLongOrNull(),
                                                    )
                                                },
                                                variant = SynapseButtonVariant.Primary,
                                                size = com.matrix.synapse.core.ui.components.SynapseButtonSize.Sm,
                                            )
                                            SynapseButton(
                                                text = stringResource(R.string.media_clear_date_filter),
                                                onClick = {
                                                    userFromTsText = ""
                                                    userUntilTsText = ""
                                                    viewModel.setUserMediaDateRange(null, null)
                                                },
                                                variant = SynapseButtonVariant.Ghost,
                                                size = com.matrix.synapse.core.ui.components.SynapseButtonSize.Sm,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bulk-action toolbar (visible when a scope is selected)
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
                                    modifier = Modifier.padding(
                                        horizontal = Tokens.Space.ScreenEdge,
                                        vertical = Tokens.Space.Xs,
                                    ),
                                )
                            }
                        }

                        // Media list header
                        if (state.mediaItems.isNotEmpty()) {
                            item {
                                SectionHeader(text = stringResource(R.string.media_section_list))
                                SynapseCard(
                                    modifier = Modifier
                                        .padding(horizontal = Tokens.Space.ScreenEdge)
                                        .fillMaxWidth(),
                                    padding = 0.dp,
                                ) {
                                    Column {
                                        state.mediaItems.forEachIndexed { index, mediaItem ->
                                            val selected = mediaItem.stableKey() in state.selectedKeys
                                            val mimeIcon = mimeTypeIcon(null) // origin only, no mime in model
                                            SynapseListItem(
                                                headline = mediaItem.mediaId,
                                                supporting = "mxc://${mediaItem.origin}/${mediaItem.mediaId}",
                                                supportingMono = true,
                                                leading = {
                                                    Icon(
                                                        imageVector = if (mediaItem.isLocal) Icons.Filled.Star else Icons.Filled.Email,
                                                        contentDescription = null,
                                                        tint = SynapseTheme.colors.textMuted,
                                                    )
                                                },
                                                trailing = {
                                                    StatusChip(
                                                        text = if (mediaItem.isLocal) {
                                                            stringResource(R.string.media_local)
                                                        } else {
                                                            stringResource(R.string.media_remote)
                                                        },
                                                        tone = if (mediaItem.isLocal) StatusTone.Accent else StatusTone.Neutral,
                                                    )
                                                },
                                                onClick = { viewModel.toggleSelection(mediaItem) },
                                                onLongClick = { onMediaClick(mediaItem.origin, mediaItem.mediaId) },
                                                selected = selected,
                                                showDivider = index < state.mediaItems.lastIndex,
                                                modifier = Modifier.testTag("media_row_${mediaItem.mediaId}"),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Empty state
                        if (state.mediaItems.isEmpty()) {
                            item {
                                val emptyMessage = when {
                                    filterRoomId != null || filterUserId != null -> stringResource(R.string.no_media_found)
                                    state.selectedRoomId == null && state.selectedUserId == null ->
                                        stringResource(R.string.select_room_or_user_to_list_media)
                                    else -> stringResource(R.string.no_media_found)
                                }
                                SynapseEmptyState(
                                    title = emptyMessage,
                                    icon = Icons.Filled.Info,
                                    modifier = Modifier.testTag("media_list_empty"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUserScopedDeleteDialog) {
        DestructiveDialog(
            title = stringResource(R.string.media_delete_user_confirm_title),
            body = stringResource(R.string.media_delete_user_confirm_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                showUserScopedDeleteDialog = false
                viewModel.bulkDeleteUserScopedMedia()
            },
            onDismiss = { showUserScopedDeleteDialog = false },
        )
    }

    if (showRoomScopedDeleteDialog) {
        DestructiveDialog(
            title = stringResource(R.string.media_delete_room_confirm_title),
            body = stringResource(R.string.media_delete_room_confirm_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                showRoomScopedDeleteDialog = false
                viewModel.bulkDeleteRoomScopedMedia()
            },
            onDismiss = { showRoomScopedDeleteDialog = false },
        )
    }
}

@Composable
private fun MediaConstraintCallout(modifier: Modifier = Modifier) {
    val c = SynapseTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.Md))
            .background(c.surface2)
            .padding(Tokens.Space.Lg),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = c.textMuted,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Unencrypted media · this room",
                style = SynapseText.BodyM,
                color = c.text,
            )
            Text(
                text = "Synapse only lists media from unencrypted events.",
                style = SynapseText.Caption,
                color = c.textMuted,
            )
        }
        SynapseButton(
            text = "Select",
            onClick = {},
            variant = SynapseButtonVariant.Outline,
            size = com.matrix.synapse.core.ui.components.SynapseButtonSize.Sm,
        )
    }
}

/** Returns an icon appropriate for a given MIME type prefix. */
private fun mimeTypeIcon(mimeType: String?): ImageVector = when {
    mimeType == null -> Icons.Filled.Email
    mimeType.startsWith("image/") -> Icons.Filled.Star
    mimeType.startsWith("video/") -> Icons.Filled.PlayArrow
    mimeType.startsWith("audio/") -> Icons.Filled.Build
    else -> Icons.Filled.Email
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
    val c = SynapseTheme.colors
    val errorColors = IconButtonDefaults.iconButtonColors(
        contentColor = c.danger,
        disabledContentColor = c.textDim,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.media_section_list),
            style = SynapseText.TitleS,
            color = c.textMuted,
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
                        tint = c.text,
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
                        imageVector = Icons.Filled.Delete,
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
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.media_cd_delete_room_media),
                    )
                }
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
