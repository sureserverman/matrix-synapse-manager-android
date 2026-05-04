package com.matrix.synapse.feature.servers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.Field
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonSize
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFormScreen(
    serverIdToEdit: String? = null,
    onServerAdded: (serverId: String, serverUrl: String) -> Unit,
    onServerUpdated: () -> Unit = {},
    viewModel: ServerFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverToEdit by viewModel.serverToEdit.collectAsStateWithLifecycle()
    val c = SynapseTheme.colors

    var urlInput by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(serverIdToEdit) {
        serverIdToEdit?.let { viewModel.loadForEdit(it) }
    }
    LaunchedEffect(serverToEdit) {
        serverToEdit?.let { server ->
            urlInput = server.inputUrl
            displayName = server.displayName
        }
    }

    if (state is ServerFormState.Success) {
        val success = state as ServerFormState.Success
        if (serverIdToEdit != null) {
            onServerUpdated()
        } else {
            onServerAdded(success.server.id, success.server.homeserverUrl)
        }
        viewModel.resetState()
    }

    val isEditMode = serverIdToEdit != null

    SynapseScaffold(
        topBar = {
            SynapseTopBar(
                title = if (isEditMode) stringResource(R.string.edit_server)
                else stringResource(R.string.add_server),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Tokens.Space.ScreenEdge)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.Lg),
        ) {
            Spacer(Modifier.height(Tokens.Space.Sm))

            Text(
                text = stringResource(R.string.server_form_description),
                style = SynapseText.BodyM,
                color = c.textMuted,
            )

            Spacer(Modifier.height(Tokens.Space.Sm))

            Field(
                value = urlInput,
                onValueChange = { if (!isEditMode) urlInput = it },
                label = stringResource(R.string.server_url),
                placeholder = stringResource(R.string.server_url_placeholder),
                enabled = !isEditMode,
                keyboardType = KeyboardType.Uri,
                error = (state as? ServerFormState.Error)?.message,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_url_field"),
            )

            Field(
                value = displayName,
                onValueChange = { displayName = it },
                label = stringResource(R.string.display_name_optional),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Tokens.Space.Sm))

            SynapseButton(
                text = if (isEditMode) stringResource(R.string.save)
                else stringResource(R.string.add_server),
                onClick = {
                    if (isEditMode && serverIdToEdit != null) {
                        viewModel.updateServer(serverIdToEdit, displayName)
                    } else {
                        viewModel.addServer(urlInput, displayName)
                    }
                },
                enabled = state !is ServerFormState.Discovering,
                size = SynapseButtonSize.Lg,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (isEditMode) "save_server_button" else "add_server_button"),
            )

            if (state is ServerFormState.Discovering) {
                CircularProgressIndicator(
                    color = c.accent,
                    modifier = Modifier.height(20.dp),
                )
            }
        }
    }
}
