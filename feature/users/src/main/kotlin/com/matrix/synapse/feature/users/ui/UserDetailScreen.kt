package com.matrix.synapse.feature.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.feature.users.data.mxcToDownloadUrl
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.DestructiveDialog
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.components.SynapseToggle
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    serverUrl: String,
    serverId: String,
    userId: String,
    onEdit: () -> Unit,
    onDevices: () -> Unit,
    onWhois: () -> Unit,
    onMedia: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: UserDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, serverId, userId) {
        viewModel.loadUser(serverUrl, serverId, userId)
    }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onBack?.invoke() }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val c = SynapseTheme.colors

    if (showDeactivateDialog) {
        DestructiveDialog(
            title = stringResource(R.string.deactivate_user_title),
            body = stringResource(R.string.deactivate_user_message),
            confirmLabel = stringResource(R.string.deactivate),
            onConfirm = {
                showDeactivateDialog = false
                viewModel.deactivateUser(serverUrl, serverId, userId, false)
            },
            onDismiss = { showDeactivateDialog = false },
        )
    }

    SynapseScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SynapseTopBar(
                title = userId,
                subtitle = state.user?.displayName,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = c.textMuted)
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading && state.user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@SynapseScaffold
        }

        val user = state.user
        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.user_not_found),
                    style = SynapseText.BodyM,
                    color = c.textMuted,
                )
            }
            return@SynapseScaffold
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadUser(serverUrl, serverId, userId) },
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
                // ── Header card ────────────────────────────────────────────
                SynapseCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Tokens.Space.ScreenEdge,
                            vertical = Tokens.Space.Lg,
                        ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm),
                    ) {
                        val avatarUrl = mxcToDownloadUrl(serverUrl, user.avatarUrl)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(c.surface3),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                )
                            } else {
                                val initial = (user.displayName ?: user.userId)
                                    .firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                Text(text = initial, style = SynapseText.Title, color = c.text)
                            }
                        }
                        if (user.displayName != null) {
                            Text(
                                text = user.displayName,
                                style = SynapseText.Title,
                                color = c.text,
                            )
                        }
                        Text(
                            text = user.userId,
                            style = SynapseText.Mono,
                            color = c.textMuted,
                        )
                        val chips = buildList {
                            if (user.admin) add("Admin" to StatusTone.Accent)
                            if (user.locked) add("Locked" to StatusTone.Warn)
                            if (user.suspended) add("Suspended" to StatusTone.Danger)
                        }
                        if (chips.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                                chips.forEach { (label, tone) ->
                                    StatusChip(text = label, tone = tone)
                                }
                            }
                        }
                    }
                }

                // ── Profile section ────────────────────────────────────────
                SectionHeader(text = "Profile")
                SynapseCard(
                    padding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.Space.ScreenEdge),
                ) {
                    Column {
                        if (user.displayName != null) {
                            SynapseListItem(
                                headline = stringResource(R.string.display_name_optional),
                                trailing = {
                                    Text(
                                        text = user.displayName,
                                        style = SynapseText.BodyM,
                                        color = c.textMuted,
                                    )
                                },
                                showDivider = user.threepids.isNotEmpty() || user.creationTs > 0L,
                            )
                        }
                        if (user.threepids.isNotEmpty()) {
                            val emailCount = user.threepids.count { it.medium == "email" }
                            val phoneCount = user.threepids.count { it.medium == "msisdn" }
                            val summary = buildString {
                                if (emailCount > 0) append("$emailCount email")
                                if (phoneCount > 0) {
                                    if (emailCount > 0) append(" · ")
                                    append("$phoneCount phone")
                                }
                            }
                            SynapseListItem(
                                headline = "3PIDs",
                                supporting = summary,
                                showDivider = user.creationTs > 0L,
                            )
                        }
                        if (user.creationTs > 0L) {
                            val dateStr = remember(user.creationTs) {
                                val millis = if (user.creationTs > 1_000_000_000_000L) {
                                    user.creationTs
                                } else {
                                    user.creationTs * 1000L
                                }
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Date(millis))
                            }
                            SynapseListItem(
                                headline = "Created",
                                trailing = {
                                    Text(
                                        text = dateStr,
                                        style = SynapseText.BodyM,
                                        color = c.textMuted,
                                    )
                                },
                                showDivider = false,
                            )
                        }
                    }
                }

                // ── Access section ─────────────────────────────────────────
                val isCurrentUser = userId == state.currentUserId
                SectionHeader(text = "Access")
                SynapseCard(
                    padding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.Space.ScreenEdge),
                ) {
                    Column {
                        SynapseListItem(
                            headline = stringResource(R.string.locked),
                            trailing = {
                                if (state.isLocking) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    SynapseToggle(
                                        checked = user.locked,
                                        onCheckedChange = { locked ->
                                            viewModel.setLocked(serverUrl, userId, locked)
                                        },
                                        enabled = !isCurrentUser,
                                    )
                                }
                            },
                            showDivider = state.canSuspend,
                        )
                        if (state.canSuspend) {
                            SynapseListItem(
                                headline = stringResource(R.string.suspended),
                                trailing = {
                                    if (state.isSuspending) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        SynapseToggle(
                                            checked = user.suspended,
                                            onCheckedChange = { suspended ->
                                                viewModel.setSuspended(serverUrl, userId, suspended)
                                            },
                                            enabled = !isCurrentUser,
                                        )
                                    }
                                },
                                showDivider = false,
                            )
                        }
                    }
                }

                // ── Devices section ────────────────────────────────────────
                SectionHeader(text = "Devices")
                SynapseCard(
                    padding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.Space.ScreenEdge),
                ) {
                    SynapseListItem(
                        headline = stringResource(R.string.devices),
                        onClick = onDevices,
                        showDivider = false,
                    )
                }

                // ── Destructive section ────────────────────────────────────
                if (!state.isDeactivated && !user.deactivated) {
                    SectionHeader(text = "Destructive")
                    SynapseCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Tokens.Space.ScreenEdge),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Tokens.Space.Md),
                        ) {
                            SynapseButton(
                                text = stringResource(R.string.deactivate_user),
                                onClick = { showDeactivateDialog = true },
                                variant = SynapseButtonVariant.Danger,
                                enabled = !state.isDeactivating && userId != state.currentUserId,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
