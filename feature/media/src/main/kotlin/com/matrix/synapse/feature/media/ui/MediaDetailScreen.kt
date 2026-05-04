package com.matrix.synapse.feature.media.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    serverUrl: String,
    serverId: String,
    serverName: String,
    mediaId: String,
    onBack: () -> Unit = {},
    viewModel: MediaDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(mediaId) { viewModel.loadMedia(serverUrl, serverId, serverName, mediaId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { msg ->
            val text = if (msg.formatArgs.isEmpty()) {
                context.getString(msg.resId)
            } else {
                context.getString(msg.resId, *msg.formatArgs.toTypedArray())
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearActionMessage()
        }
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.media_detail),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = state.media != null && !state.isActioning,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_media),
                            tint = SynapseTheme.colors.danger,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading && state.media == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null && state.media == null -> {
                SynapseEmptyState(
                    title = state.error!!,
                    icon = Icons.Filled.Info,
                    errorTone = true,
                    modifier = Modifier.padding(padding),
                )
            }

            state.media != null -> {
                val media = state.media!!
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.loadMedia(serverUrl, serverId, serverName, mediaId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Tokens.Space.ScreenEdge),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Space.Md),
                    ) {
                        // Info card
                        SectionHeader(text = stringResource(R.string.media_info))
                        SynapseCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = 0.dp,
                        ) {
                            Column {
                                SynapseListItem(
                                    headline = stringResource(R.string.media_detail_label_media_id),
                                    supporting = media.mediaId,
                                    supportingMono = true,
                                    showDivider = true,
                                )
                                SynapseListItem(
                                    headline = stringResource(R.string.media_detail_label_type),
                                    supporting = media.mediaType
                                        ?: stringResource(R.string.media_detail_unknown_type),
                                    supportingMono = true,
                                    showDivider = true,
                                )
                                SynapseListItem(
                                    headline = stringResource(R.string.media_detail_label_size),
                                    supporting = formatBytes(media.mediaLength),
                                    showDivider = true,
                                )
                                if (media.uploadName != null) {
                                    SynapseListItem(
                                        headline = stringResource(R.string.media_detail_label_upload_name),
                                        supporting = media.uploadName,
                                        showDivider = true,
                                    )
                                }
                                SynapseListItem(
                                    headline = stringResource(R.string.media_detail_label_created),
                                    supporting = formatTimestamp(media.createdTs),
                                    showDivider = true,
                                )
                                SynapseListItem(
                                    headline = stringResource(R.string.media_detail_label_last_accessed),
                                    supporting = formatTimestamp(media.lastAccessTs),
                                    showDivider = true,
                                )
                                SynapseListItem(
                                    headline = stringResource(R.string.media_detail_label_quarantined_by),
                                    supporting = media.quarantinedBy
                                        ?: stringResource(R.string.common_no),
                                    showDivider = true,
                                )
                                SynapseListItem(
                                    headline = stringResource(R.string.media_detail_label_protected),
                                    supporting = if (media.safeFromQuarantine) {
                                        stringResource(R.string.common_yes)
                                    } else {
                                        stringResource(R.string.common_no)
                                    },
                                    showDivider = false,
                                )
                            }
                        }

                        // Actions card
                        SectionHeader(text = stringResource(R.string.actions))
                        SynapseCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = Tokens.Space.Lg,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Sm)) {
                                if (media.quarantinedBy != null) {
                                    SynapseButton(
                                        text = stringResource(R.string.remove_from_quarantine),
                                        onClick = { viewModel.unquarantine(serverName, mediaId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !state.isActioning,
                                        variant = SynapseButtonVariant.Primary,
                                    )
                                } else {
                                    SynapseButton(
                                        text = stringResource(R.string.quarantine),
                                        onClick = { viewModel.quarantine(serverName, mediaId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !state.isActioning,
                                        variant = SynapseButtonVariant.Primary,
                                    )
                                }

                                if (media.safeFromQuarantine) {
                                    SynapseButton(
                                        text = stringResource(R.string.remove_protection),
                                        onClick = { viewModel.unprotect(mediaId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !state.isActioning,
                                        variant = SynapseButtonVariant.Outline,
                                    )
                                } else {
                                    SynapseButton(
                                        text = stringResource(R.string.protect_from_quarantine),
                                        onClick = { viewModel.protect(mediaId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !state.isActioning,
                                        variant = SynapseButtonVariant.Outline,
                                    )
                                }

                                SynapseButton(
                                    text = stringResource(R.string.delete_media),
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                    variant = SynapseButtonVariant.Danger,
                                )

                                if (state.isActioning) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("media_detail_progress"),
                                        color = SynapseTheme.colors.accent,
                                        trackColor = SynapseTheme.colors.surface2,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(Tokens.Space.Xl))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DestructiveDialog(
            title = stringResource(R.string.delete_media),
            body = stringResource(R.string.delete_media_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete(mediaId)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
