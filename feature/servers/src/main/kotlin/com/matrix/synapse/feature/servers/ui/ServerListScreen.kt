package com.matrix.synapse.feature.servers.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.StatusChip
import com.matrix.synapse.core.ui.components.StatusTone
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseEmptyState
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens
import androidx.compose.material3.FloatingActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    onAddServer: () -> Unit,
    onEditServer: (serverId: String) -> Unit,
    onOpenLogin: (serverId: String, serverUrl: String) -> Unit,
    onOpenUserList: (serverId: String, serverUrl: String) -> Unit,
    viewModel: ServerListViewModel = hiltViewModel(),
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val deletingId by viewModel.deletingId.collectAsStateWithLifecycle()
    val c = SynapseTheme.colors

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ServerListNavEvent.OpenLogin -> onOpenLogin(event.serverId, event.serverUrl)
                is ServerListNavEvent.OpenUserList -> onOpenUserList(event.serverId, event.serverUrl)
            }
        }
    }

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.servers),
                actions = {
                    IconButton(onClick = onAddServer) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_server),
                            tint = c.text,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (servers.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onAddServer,
                    containerColor = c.accent,
                    contentColor = if (c.isLight) androidx.compose.ui.graphics.Color.White
                                   else Tokens.Color.Dark.Bg,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_server))
                }
            }
        },
    ) { padding ->
        if (servers.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                SynapseEmptyState(
                    title = stringResource(R.string.no_servers_yet),
                    body = stringResource(R.string.no_servers_body),
                    icon = Icons.Filled.Add,
                    actionLabel = stringResource(R.string.add_server),
                    onAction = onAddServer,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(top = Tokens.Space.Md, bottom = Tokens.Space.Xxl),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.Md),
            ) {
                items(servers, key = { it.id }) { server ->
                    val isDeleting = deletingId == server.id
                    SynapseCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Tokens.Space.ScreenEdge),
                        padding = 0.dp,
                    ) {
                        SynapseListItem(
                            headline = server.displayName,
                            supporting = server.homeserverUrl,
                            supportingMono = true,
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(c.accent),
                                )
                            },
                            trailing = {
                                if (isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = c.accent,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Xs),
                                    ) {
                                        StatusChip(
                                            text = "Online",
                                            tone = StatusTone.Success,
                                            showDot = true,
                                        )
                                        IconButton(onClick = { onEditServer(server.id) }) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = stringResource(R.string.edit_server),
                                                tint = c.textMuted,
                                            )
                                        }
                                        IconButton(onClick = { viewModel.removeServer(server.id) }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = stringResource(R.string.remove_server),
                                                tint = c.danger,
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = if (!isDeleting) { { viewModel.onServerClick(server) } } else null,
                            showDivider = false,
                        )
                    }
                }
            }
        }
    }
}
