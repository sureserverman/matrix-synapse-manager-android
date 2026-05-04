package com.matrix.synapse.feature.moderation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.resources.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.ui.components.DestructiveDialog
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val prettyJson = Json { prettyPrint = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventReportDetailScreen(
    serverId: String,
    serverUrl: String,
    reportId: Long,
    onBack: () -> Unit,
    viewModel: EventReportDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl, reportId) { viewModel.load(serverId, serverUrl, reportId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.report_number, reportId.toString()),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { /* reserved for overflow menu */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = SynapseTheme.colors.textMuted,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.isLoading && state.report == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null && state.report == null -> Text(
                state.error!!,
                color = SynapseTheme.colors.danger,
                style = SynapseText.BodyM,
                modifier = Modifier.padding(innerPadding).padding(Tokens.Space.Xl),
            )

            state.report != null -> {
                val report = state.report!!
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.load(serverId, serverUrl, reportId) },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = Tokens.Space.Md),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Space.Xs),
                    ) {
                        // Header card: reason + target event MXID
                        SynapseCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Tokens.Space.ScreenEdge),
                            padding = Tokens.Space.Lg,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Xs)) {
                                Text(
                                    text = report.reason ?: stringResource(R.string.report_info),
                                    style = SynapseText.Title,
                                    color = SynapseTheme.colors.text,
                                )
                                Text(
                                    text = report.eventId,
                                    style = SynapseText.Mono,
                                    color = SynapseTheme.colors.textMuted,
                                )
                            }
                        }

                        // REPORT section
                        SectionHeader(stringResource(R.string.report_info))
                        SynapseCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Tokens.Space.ScreenEdge),
                            padding = 0.dp,
                        ) {
                            Column {
                                SynapseListItem(
                                    headline = "Reported user",
                                    supporting = report.sender,
                                    supportingMono = true,
                                    showDivider = true,
                                )
                                SynapseListItem(
                                    headline = "Reporter",
                                    supporting = report.userId,
                                    supportingMono = true,
                                    showDivider = true,
                                )
                                val roomDisplay = report.name?.takeIf { it.isNotBlank() }
                                    ?: report.canonicalAlias
                                    ?: report.roomId
                                SynapseListItem(
                                    headline = "Room",
                                    supporting = roomDisplay,
                                    supportingMono = report.name == null && report.canonicalAlias == null,
                                    showDivider = report.reason != null,
                                )
                                report.reason?.let { reason ->
                                    SynapseListItem(
                                        headline = "Reason",
                                        supporting = reason,
                                        supportingMono = false,
                                        showDivider = true,
                                    )
                                }
                                SynapseListItem(
                                    headline = "When",
                                    supporting = formatTs(report.receivedTs),
                                    supportingMono = false,
                                    showDivider = false,
                                )
                            }
                        }

                        // EVENT JSON section
                        report.eventJson?.let { jsonEl ->
                            SectionHeader(stringResource(R.string.event_content))
                            SynapseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge),
                                padding = Tokens.Space.Lg,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    Text(
                                        text = prettyJson.encodeToString(JsonElement.serializer(), jsonEl),
                                        style = SynapseText.Mono,
                                        color = SynapseTheme.colors.textMuted,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        // ACTIONS section
                        SectionHeader(stringResource(R.string.actions))
                        SynapseCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Tokens.Space.ScreenEdge),
                            padding = Tokens.Space.Lg,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Md),
                            ) {
                                SynapseButton(
                                    text = "Dismiss",
                                    onClick = { viewModel.deleteReport(reportId) },
                                    variant = SynapseButtonVariant.Outline,
                                    enabled = !state.isDeleting,
                                    modifier = Modifier.weight(1f),
                                )
                                SynapseButton(
                                    text = "Take action",
                                    onClick = { showDeleteDialog = true },
                                    variant = SynapseButtonVariant.Danger,
                                    enabled = !state.isDeleting,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DestructiveDialog(
            title = stringResource(R.string.delete_report),
            body = stringResource(R.string.delete_report_message),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteReport(reportId)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

private fun formatTs(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
