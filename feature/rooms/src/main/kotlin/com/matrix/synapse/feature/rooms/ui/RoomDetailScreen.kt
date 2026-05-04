package com.matrix.synapse.feature.rooms.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.DestructiveDialog
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import com.matrix.synapse.feature.rooms.data.DeleteRoomRequest
import com.matrix.synapse.feature.rooms.data.mxcToDownloadUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    serverUrl: String,
    serverId: String,
    roomId: String,
    onBack: () -> Unit = {},
    onMedia: () -> Unit = {},
    viewModel: RoomDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(roomId) { viewModel.loadRoom(serverUrl, serverId, roomId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var joinUserId by remember { mutableStateOf("") }
    var membersExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleteComplete) {
        if (state.deleteComplete) onBack()
    }

    if (showDeleteDialog) {
        DeleteRoomDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { purge, block, message ->
                showDeleteDialog = false
                viewModel.deleteRoom(serverUrl, serverId, roomId, DeleteRoomRequest(purge = purge, block = block, message = message))
            },
        )
    }

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = state.room?.name ?: stringResource(R.string.room_detail),
                subtitle = roomId,
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.room == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.room == null -> Text(
                text = state.error!!,
                color = SynapseTheme.colors.danger,
                style = SynapseText.BodyM,
                modifier = Modifier.padding(padding).padding(Tokens.Space.Xl),
            )

            state.room != null -> {
                val room = state.room!!
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.loadRoom(serverUrl, serverId, roomId) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        val roomAvatarUrl = mxcToDownloadUrl(serverUrl, room.avatar)

                        // ── Full-width avatar banner ─────────────────────────
                        if (roomAvatarUrl != null) {
                            item {
                                AsyncImage(
                                    model = roomAvatarUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(SynapseTheme.colors.surface3),
                                )
                            }
                        }

                        // ── Header card ───────────────────────────────────────────
                        item {
                            Spacer(Modifier.height(Tokens.Space.Lg))
                            SynapseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge),
                                padding = Tokens.Space.Lg,
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm),
                                ) {
                                    val c = SynapseTheme.colors
                                    if (roomAvatarUrl == null) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(c.surface3),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "#",
                                                style = SynapseText.Display,
                                                color = c.textMuted,
                                            )
                                        }
                                    }
                                    Text(
                                        text = room.name ?: stringResource(R.string.unnamed_room),
                                        style = SynapseText.Title,
                                        color = SynapseTheme.colors.text,
                                    )
                                    Text(
                                        text = room.roomId,
                                        style = SynapseText.Mono,
                                        color = SynapseTheme.colors.textMuted,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        StatusChip(
                                            text = "${room.joinedMembers}",
                                            tone = StatusTone.Neutral,
                                        )
                                        val (chipLabel, chipTone) = joinRuleChipDetail(room.joinRules)
                                        StatusChip(text = chipLabel, tone = chipTone)
                                    }
                                }
                            }
                        }

                        // ── Topic / Alias section ─────────────────────────────────
                        if (room.topic != null || room.canonicalAlias != null) {
                            item {
                                SectionHeader(text = stringResource(R.string.room_info))
                                SynapseCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Tokens.Space.ScreenEdge),
                                    padding = 0.dp,
                                ) {
                                    Column {
                                        if (room.topic != null) {
                                            SynapseListItem(
                                                headline = stringResource(R.string.room_info),
                                                supporting = room.topic,
                                                showDivider = room.canonicalAlias != null,
                                            )
                                        }
                                        if (room.canonicalAlias != null) {
                                            SynapseListItem(
                                                headline = room.canonicalAlias,
                                                supportingMono = true,
                                                showDivider = false,
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(room.canonicalAlias))
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── State section ─────────────────────────────────────────
                        item {
                            SectionHeader(text = stringResource(R.string.actions))
                            SynapseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge),
                                padding = 0.dp,
                            ) {
                                Column {
                                    if (room.isPublic) {
                                        SynapseListItem(
                                            headline = "Visibility",
                                            trailing = {
                                                StatusChip(text = "Public", tone = StatusTone.Success)
                                            },
                                            showDivider = true,
                                        )
                                    } else {
                                        SynapseListItem(
                                            headline = "Visibility",
                                            trailing = {
                                                StatusChip(text = "Private", tone = StatusTone.Neutral)
                                            },
                                            showDivider = true,
                                        )
                                    }
                                    if (room.joinRules != null) {
                                        val (label, tone) = joinRuleChipDetail(room.joinRules)
                                        SynapseListItem(
                                            headline = "Join Rule",
                                            trailing = { StatusChip(text = label, tone = tone) },
                                            showDivider = true,
                                        )
                                    }
                                    if (room.historyVisibility != null) {
                                        SynapseListItem(
                                            headline = "History Visibility",
                                            supporting = room.historyVisibility,
                                            showDivider = true,
                                        )
                                    }
                                    if (room.guestAccess != null) {
                                        SynapseListItem(
                                            headline = "Guest Access",
                                            supporting = room.guestAccess,
                                            showDivider = false,
                                        )
                                    }
                                }
                            }
                        }

                        // ── Members section ───────────────────────────────────────
                        item {
                            SectionHeader(text = stringResource(
                                if (membersExpanded) R.string.hide_members_count
                                else R.string.show_members_count,
                                state.members.size,
                            ))
                            SynapseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge),
                                padding = 0.dp,
                            ) {
                                SynapseListItem(
                                    headline = if (membersExpanded)
                                        stringResource(R.string.hide_members_count, state.members.size)
                                    else
                                        stringResource(R.string.show_members_count, state.members.size),
                                    onClick = { membersExpanded = !membersExpanded },
                                    showDivider = false,
                                )
                            }
                        }
                        if (membersExpanded) {
                            items(state.members) { member ->
                                SynapseListItem(
                                    headline = member,
                                    supportingMono = true,
                                    modifier = Modifier.padding(start = Tokens.Space.Lg),
                                )
                            }
                        }

                        // ── Status messages ───────────────────────────────────────
                        item {
                            if (state.error != null) {
                                Text(
                                    text = state.error!!,
                                    color = SynapseTheme.colors.danger,
                                    style = SynapseText.BodyM,
                                    modifier = Modifier
                                        .padding(horizontal = Tokens.Space.ScreenEdge, vertical = Tokens.Space.Sm)
                                        .testTag("room_detail_error"),
                                )
                            }
                            if (state.actionMessage != null) {
                                Text(
                                    text = state.actionMessage!!,
                                    color = SynapseTheme.colors.accent,
                                    style = SynapseText.BodyM,
                                    modifier = Modifier.padding(horizontal = Tokens.Space.ScreenEdge, vertical = Tokens.Space.Sm),
                                )
                            }
                        }

                        // ── Actions section ───────────────────────────────────────
                        item {
                            SectionHeader(text = stringResource(R.string.actions))
                            SynapseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge),
                                padding = Tokens.Space.Lg,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                                    SynapseButton(
                                        text = if (state.isBlocked) stringResource(R.string.unblock_room) else stringResource(R.string.block_room),
                                        onClick = { viewModel.blockRoom(serverUrl, serverId, roomId, !state.isBlocked) },
                                        variant = SynapseButtonVariant.Outline,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    SynapseButton(
                                        text = stringResource(R.string.make_me_room_admin),
                                        onClick = { viewModel.makeRoomAdmin(serverUrl, serverId, roomId, null) },
                                        variant = SynapseButtonVariant.Outline,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    SynapseButton(
                                        text = stringResource(R.string.room_media),
                                        onClick = onMedia,
                                        variant = SynapseButtonVariant.Outline,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Sm),
                                    ) {
                                        OutlinedTextField(
                                            value = joinUserId,
                                            onValueChange = { joinUserId = it },
                                            label = { Text(stringResource(R.string.user_id)) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                        )
                                        SynapseButton(
                                            text = stringResource(R.string.join),
                                            onClick = {
                                                viewModel.joinUserToRoom(serverUrl, serverId, roomId, joinUserId)
                                                joinUserId = ""
                                            },
                                            enabled = joinUserId.isNotBlank(),
                                        )
                                    }
                                }
                            }
                        }

                        // ── Destructive section ───────────────────────────────────
                        item {
                            SectionHeader(text = "Destructive")
                            SynapseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge),
                                padding = Tokens.Space.Lg,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                                    SynapseButton(
                                        text = if (state.isDeleting) stringResource(R.string.deleting) else stringResource(R.string.delete_room),
                                        onClick = { showDeleteDialog = true },
                                        variant = SynapseButtonVariant.Danger,
                                        enabled = !state.isDeleting,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            Spacer(Modifier.height(Tokens.Space.Xl))
                        }
                    }
                }
            }
        }
    }
}

private fun joinRuleChipDetail(joinRules: String?): Pair<String, StatusTone> = when (joinRules?.lowercase()) {
    "public"           -> "Public"     to StatusTone.Success
    "invite"           -> "Invite"     to StatusTone.Info
    "knock"            -> "Knock"      to StatusTone.Warn
    "restricted"       -> "Restricted" to StatusTone.Accent
    "knock_restricted" -> "Restricted" to StatusTone.Accent
    null               -> "Unknown"    to StatusTone.Neutral
    else               -> joinRules    to StatusTone.Neutral
}

@Composable
private fun DeleteRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (purge: Boolean, block: Boolean, message: String?) -> Unit,
) {
    var purge by remember { mutableStateOf(true) }
    var block by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    DestructiveDialog(
        title = stringResource(R.string.delete_room),
        body = stringResource(R.string.delete_room_message),
        confirmLabel = stringResource(R.string.delete),
        dismissLabel = stringResource(R.string.cancel),
        onConfirm = { onConfirm(purge, block, message.takeIf { it.isNotBlank() }) },
        onDismiss = onDismiss,
    )
}
