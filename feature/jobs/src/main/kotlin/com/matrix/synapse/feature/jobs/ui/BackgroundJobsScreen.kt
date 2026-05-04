package com.matrix.synapse.feature.jobs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.components.SynapseToggle
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

private val JOB_NAMES = listOf(
    "regenerate_directory" to R.string.job_regenerate_directory,
    "populate_stats_process_rooms" to R.string.job_populate_stats,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundJobsScreen(
    serverId: String,
    serverUrl: String,
    onBack: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.load(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.background_jobs),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.load(serverId, serverUrl) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("jobs_refresh_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.retry),
                            tint = SynapseTheme.colors.textMuted,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading && state.enabled == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("jobs_loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SynapseTheme.colors.accent)
                }
            }

            state.error != null && state.enabled == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    SynapseEmptyState(
                        title = state.error!!,
                        icon = Icons.Filled.Warning,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.load(serverId, serverUrl) },
                        errorTone = true,
                        modifier = Modifier.testTag("jobs_error"),
                    )
                }
            }

            else -> PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.load(serverId, serverUrl) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("jobs_content"),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // ── Background updates toggle ────────────────────────
                    state.enabled?.let { enabled ->
                        item(key = "toggle_header") {
                            SectionHeader(
                                text = stringResource(R.string.background_updates),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item(key = "toggle_card") {
                            SynapseCard(
                                padding = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge)
                                    .testTag("jobs_toggle_card"),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = Tokens.Space.Lg,
                                            vertical = Tokens.Space.Md,
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.background_updates),
                                        style = com.matrix.synapse.core.ui.theme.SynapseText.BodyM,
                                        color = SynapseTheme.colors.text,
                                    )
                                    SynapseToggle(
                                        checked = enabled,
                                        onCheckedChange = { viewModel.setEnabled(it) },
                                        enabled = !state.isToggling,
                                        modifier = Modifier.testTag("jobs_enabled_toggle"),
                                    )
                                }
                            }
                        }
                    }

                    // ── Current updates (running jobs) ────────────────────
                    if (state.currentUpdates.isNotEmpty()) {
                        item(key = "current_header") {
                            SectionHeader(
                                text = stringResource(R.string.current_updates),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item(key = "current_card") {
                            SynapseCard(
                                padding = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge)
                                    .testTag("jobs_current_updates_card"),
                            ) {
                                Column {
                                    state.currentUpdates.entries.toList()
                                        .forEachIndexed { index, (dbName, info) ->
                                            SynapseListItem(
                                                headline = info.name,
                                                supporting = dbName,
                                                supportingMono = true,
                                                leading = {
                                                    Icon(
                                                        imageVector = Icons.Filled.Refresh,
                                                        contentDescription = null,
                                                        tint = SynapseTheme.colors.accent,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                },
                                                trailing = {
                                                    StatusChip(
                                                        text = "Running",
                                                        tone = StatusTone.Accent,
                                                        showDot = true,
                                                    )
                                                },
                                                showDivider = index < state.currentUpdates.size - 1,
                                                modifier = Modifier.testTag("jobs_current_item_$dbName"),
                                            )
                                        }
                                }
                            }
                        }
                    }

                    // ── Triggerable jobs ──────────────────────────────────
                    item(key = "run_job_header") {
                        SectionHeader(
                            text = stringResource(R.string.run_job),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (JOB_NAMES.isEmpty()) {
                        item(key = "jobs_empty") {
                            SynapseEmptyState(
                                title = "No background jobs",
                                icon = Icons.Filled.Notifications,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("jobs_empty_state"),
                            )
                        }
                    } else {
                        item(key = "jobs_card") {
                            SynapseCard(
                                padding = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Tokens.Space.ScreenEdge)
                                    .testTag("jobs_run_card"),
                            ) {
                                Column {
                                    JOB_NAMES.forEachIndexed { index, (jobName, labelResId) ->
                                        JobRow(
                                            jobName = jobName,
                                            label = stringResource(labelResId),
                                            isRunning = state.currentUpdates.values.any {
                                                it.name == jobName
                                            },
                                            isPaused = state.enabled == false,
                                            isStarting = state.isStartingJob,
                                            onStart = { viewModel.startJob(jobName) },
                                            showDivider = index < JOB_NAMES.size - 1,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "jobs_bottom_spacer") {
                        Box(modifier = Modifier.padding(bottom = Tokens.Space.Xl))
                    }
                }
            }
        }
    }
}

@Composable
private fun JobRow(
    jobName: String,
    label: String,
    isRunning: Boolean,
    isPaused: Boolean,
    isStarting: Boolean,
    onStart: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = SynapseTheme.colors
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    val (tone, chipLabel, icon, iconTint) = when {
        isRunning -> JobVisual(
            tone = StatusTone.Accent,
            chipLabel = "Running",
            icon = Icons.Filled.Refresh,
            iconTint = c.accent,
        )
        isPaused  -> JobVisual(
            tone = StatusTone.Warn,
            chipLabel = "Paused",
            icon = Icons.Filled.Lock,
            iconTint = c.warn,
        )
        else      -> JobVisual(
            tone = StatusTone.Neutral,
            chipLabel = "Idle",
            icon = Icons.Filled.Notifications,
            iconTint = c.textMuted,
        )
    }

    SynapseListItem(
        headline = label,
        // TODO: render headline in mono if design requires — keep current Inter behavior for now
        supporting = jobName,
        supportingMono = true,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Xs),
            ) {
                StatusChip(text = chipLabel, tone = tone)
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("jobs_overflow_$jobName"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = c.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Start",
                                    style = com.matrix.synapse.core.ui.theme.SynapseText.BodyM,
                                    color = c.text,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onStart()
                            },
                            enabled = !isStarting,
                            modifier = Modifier.testTag("jobs_start_$jobName"),
                        )
                    }
                }
            }
        },
        showDivider = showDivider,
        modifier = modifier.testTag("jobs_row_$jobName"),
    )
}

private data class JobVisual(
    val tone: StatusTone,
    val chipLabel: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: androidx.compose.ui.graphics.Color,
)
