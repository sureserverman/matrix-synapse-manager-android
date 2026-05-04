package com.matrix.synapse.feature.users.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.matrix.synapse.core.ui.components.Field
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.components.SynapseSelectionTopAppBar
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import com.matrix.synapse.feature.users.data.UserSummary
import com.matrix.synapse.feature.users.data.mxcToDownloadUrl
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    serverId: String,
    serverUrl: String,
    onUserClick: (userId: String) -> Unit,
    onAddUser: () -> Unit = {},
    onSettings: () -> Unit = {},
    onServers: () -> Unit = {},
    onRooms: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onMedia: () -> Unit = {},
    onFederation: () -> Unit = {},
    viewModel: UserListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.init(serverId, serverUrl) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showDeactivateUsersDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    if (showDeactivateUsersDialog) {
        DestructiveDialog(
            title = stringResource(R.string.deactivate_users_title),
            body = stringResource(R.string.deactivate_users_message, state.selectedUserIds.size),
            confirmLabel = stringResource(R.string.deactivate),
            onConfirm = {
                viewModel.deleteSelectedUsers(erase = false)
                showDeactivateUsersDialog = false
            },
            onDismiss = { showDeactivateUsersDialog = false },
        )
    }

    val sortedUsers = remember(state.users, state.sortOrder) {
        if (state.sortOrder == "name_desc") {
            state.users.sortedByDescending { (it.displayName ?: it.userId).lowercase() }
        } else {
            state.users.sortedBy { (it.displayName ?: it.userId).lowercase() }
        }
    }

    val c = SynapseTheme.colors

    SynapseScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.selectionMode) {
                SynapseSelectionTopAppBar(
                    selectedCount = state.selectedUserIds.size,
                    onClose = { viewModel.exitSelectionMode() },
                    actions = {
                        IconButton(
                            onClick = { showDeactivateUsersDialog = true },
                            enabled = !state.isDeleting,
                            modifier = Modifier.testTag("user_selection_deactivate"),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.deactivate_selected_users),
                            )
                        }
                    },
                )
            } else {
                SynapseTopBar(
                    title = state.currentServer?.displayName ?: serverUrl,
                    subtitle = if (state.totalUsers > 0L) {
                        stringResource(R.string.users_count, serverUrl, state.totalUsers)
                    } else {
                        serverUrl
                    },
                    onTitleClick = onServers,
                    titleCentered = true,
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = c.textMuted)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.Menu, contentDescription = null, tint = c.textMuted)
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (!state.selectionMode) {
                FloatingActionButton(
                    onClick = onAddUser,
                    containerColor = c.accent,
                    contentColor = if (c.isLight) Tokens.Color.Light.Bg else Tokens.Color.Dark.Bg,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_user))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Field(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    viewModel.search(q)
                },
                label = stringResource(R.string.search_users),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.Space.ScreenEdge, vertical = Tokens.Space.Md)
                    .testTag("user_search"),
            )

            when {
                state.isLoading && state.users.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("user_list_loading")) }

                state.error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    SynapseEmptyState(
                        title = state.error!!,
                        icon = Icons.Filled.Person,
                        errorTone = true,
                        modifier = Modifier.testTag("user_list_error"),
                    )
                }

                sortedUsers.isEmpty() && !state.isLoading -> SynapseEmptyState(
                    title = stringResource(R.string.no_users),
                    body = stringResource(R.string.no_users_body),
                    icon = Icons.Filled.Person,
                    actionLabel = stringResource(R.string.add_user),
                    onAction = onAddUser,
                )

                else -> PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    UserList(
                        users = sortedUsers,
                        serverUrl = serverUrl,
                        hasMore = state.hasMore,
                        isLoadingMore = state.isLoadingMore,
                        selectionMode = state.selectionMode,
                        selectedUserIds = state.selectedUserIds,
                        onUserClick = onUserClick,
                        onLoadMore = { viewModel.loadNextPage() },
                        onUserLongPress = { viewModel.enterSelectionMode(it) },
                        onToggleUserSelection = { viewModel.toggleUserSelection(it) },
                        onSelectAll = { viewModel.selectAllUsers() },
                        onClearSelection = { viewModel.clearUserSelection() },
                        currentUserId = state.currentUserId,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserList(
    users: List<UserSummary>,
    serverUrl: String,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    selectionMode: Boolean,
    selectedUserIds: Set<String>,
    onUserClick: (userId: String) -> Unit,
    onLoadMore: () -> Unit,
    onUserLongPress: (userId: String) -> Unit,
    onToggleUserSelection: (userId: String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    currentUserId: String? = null,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { if (it) onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_list"),
    ) {
        if (selectionMode) {
            item(key = "select_all") {
                val selectableUsers = currentUserId?.let { id -> users.filter { it.userId != id } } ?: users
                val allSelectableSelected =
                    selectableUsers.isNotEmpty() && selectableUsers.all { it.userId in selectedUserIds }
                SynapseListItem(
                    headline = stringResource(R.string.select_all),
                    leading = {
                        Checkbox(
                            checked = allSelectableSelected,
                            onCheckedChange = { if (it) onSelectAll() else onClearSelection() },
                            modifier = Modifier.testTag("user_select_all"),
                        )
                    },
                    onClick = { if (allSelectableSelected) onClearSelection() else onSelectAll() },
                )
            }
        }
        items(users, key = { it.userId }) { user ->
            val isCurrentUser = user.userId == currentUserId
            UserRow(
                user = user,
                serverUrl = serverUrl,
                selectionMode = selectionMode,
                selected = user.userId in selectedUserIds,
                isCurrentUser = isCurrentUser,
                onClick = { onUserClick(user.userId) },
                onLongPress = { if (!isCurrentUser) onUserLongPress(user.userId) },
                onToggleSelection = { if (!isCurrentUser) onToggleUserSelection(user.userId) },
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Space.Md),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: UserSummary,
    serverUrl: String,
    selectionMode: Boolean,
    selected: Boolean,
    isCurrentUser: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    val c = SynapseTheme.colors
    val headline = user.displayName ?: user.userId.substringAfter("@").substringBefore(":")

    val statusTone: StatusTone? = when {
        user.locked -> StatusTone.Warn
        else -> null
    }
    val statusLabel: String? = when {
        user.locked -> "Locked"
        else -> null
    }

    val trailingContent: (@Composable () -> Unit)? = when {
        selectionMode -> {
            {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection() },
                    enabled = !isCurrentUser,
                    modifier = Modifier.testTag("user_checkbox_${user.userId}"),
                )
            }
        }
        statusTone != null && statusLabel != null -> {
            { StatusChip(text = statusLabel, tone = statusTone) }
        }
        else -> null
    }

    val avatarUrl = mxcToDownloadUrl(serverUrl, user.avatarUrl)
    SynapseListItem(
        headline = headline,
        supporting = user.userId,
        supportingMono = true,
        leadingShape = CircleShape,
        leading = {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                val initial = headline.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                Text(
                    text = initial,
                    style = SynapseText.TitleS,
                    color = c.text,
                )
            }
        },
        trailing = trailingContent,
        onClick = {
            if (selectionMode && !isCurrentUser) onToggleSelection()
            else if (!selectionMode) onClick()
        },
        onLongClick = { if (!isCurrentUser) onLongPress() },
        selected = selected && selectionMode,
        modifier = Modifier.testTag("user_row_${user.userId}"),
    )
}
