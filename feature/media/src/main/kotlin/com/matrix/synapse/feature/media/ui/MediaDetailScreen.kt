package com.matrix.synapse.feature.media.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.matrix.synapse.core.resources.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.media_detail),
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading && state.media == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null && state.media == null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(24.dp),
            )

            state.media != null -> {
                val media = state.media!!
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.loadMedia(serverUrl, serverId, serverName, mediaId) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.media_info), style = MaterialTheme.typography.titleMedium)
                            InfoRow(stringResource(R.string.media_detail_label_media_id), media.mediaId)
                            InfoRow(
                                stringResource(R.string.media_detail_label_type),
                                media.mediaType ?: stringResource(R.string.media_detail_unknown_type),
                            )
                            InfoRow(stringResource(R.string.media_detail_label_size), formatBytes(media.mediaLength))
                            InfoRow(stringResource(R.string.media_detail_label_upload_name), media.uploadName ?: "\u2014")
                            InfoRow(stringResource(R.string.media_detail_label_created), formatTimestamp(media.createdTs))
                            InfoRow(stringResource(R.string.media_detail_label_last_accessed), formatTimestamp(media.lastAccessTs))
                            InfoRow(
                                stringResource(R.string.media_detail_label_quarantined_by),
                                media.quarantinedBy ?: stringResource(R.string.common_no),
                            )
                            InfoRow(
                                stringResource(R.string.media_detail_label_protected),
                                if (media.safeFromQuarantine) {
                                    stringResource(R.string.common_yes)
                                } else {
                                    stringResource(R.string.common_no)
                                },
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.actions), style = MaterialTheme.typography.titleMedium)

                            if (media.quarantinedBy != null) {
                                Button(
                                    onClick = { viewModel.unquarantine(serverName, mediaId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                ) { Text(stringResource(R.string.remove_from_quarantine)) }
                            } else {
                                Button(
                                    onClick = { viewModel.quarantine(serverName, mediaId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                ) { Text(stringResource(R.string.quarantine)) }
                            }

                            if (media.safeFromQuarantine) {
                                OutlinedButton(
                                    onClick = { viewModel.unprotect(mediaId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                ) { Text(stringResource(R.string.remove_protection)) }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.protect(mediaId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                ) { Text(stringResource(R.string.protect_from_quarantine)) }
                            }

                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                enabled = !state.isActioning,
                            ) { Text(stringResource(R.string.delete_media)) }

                            if (state.isActioning) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("media_detail_progress"))
                            }
                        }
                    }
                }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_media)) },
            text = { Text(stringResource(R.string.delete_media_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(mediaId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "\u2014"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
