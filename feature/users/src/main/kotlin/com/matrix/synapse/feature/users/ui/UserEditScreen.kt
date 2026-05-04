package com.matrix.synapse.feature.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.matrix.synapse.core.resources.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.ui.components.Field
import com.matrix.synapse.core.ui.components.SynapseButton
import com.matrix.synapse.core.ui.components.SynapseButtonSize
import com.matrix.synapse.core.ui.components.SynapseButtonVariant
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.components.SynapseToggle
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.Tokens

/**
 * Screen for creating a new Synapse user.
 *
 * For editing an existing user, pass [existingUserId]; the password field is then hidden
 * since passwords are not required for updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    serverUrl: String,
    existingUserId: String? = null,
    onSaved: (userId: String) -> Unit,
    viewModel: UserEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedUserId) {
        state.savedUserId?.let { onSaved(it) }
    }

    LaunchedEffect(serverUrl) {
        if (existingUserId == null) viewModel.loadServerName(serverUrl)
    }

    val title = if (existingUserId == null) stringResource(R.string.create_user) else stringResource(R.string.edit_user)
    val serverName = state.serverName ?: remember(serverUrl) { serverNameFromUrl(serverUrl) }
    val c = SynapseTheme.colors

    SynapseScaffold(
        topBar = {
            SynapseTopBar(title = title)
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
            // Top spacing
            androidx.compose.foundation.layout.Spacer(
                Modifier.padding(top = Tokens.Space.Sm),
            )

            if (existingUserId == null) {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.Xs)) {
                    Field(
                        value = username,
                        onValueChange = { username = it.filter { c -> c != '@' && c != ':' } },
                        label = stringResource(R.string.username),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_user_id"),
                    )
                    val preview = username.trim()
                    Text(
                        text = if (preview.isNotBlank()) "@$preview:$serverName" else " ",
                        style = SynapseText.BodyS,
                        color = c.textMuted,
                        modifier = Modifier.padding(start = Tokens.Space.Sm),
                    )
                }
            } else {
                Field(
                    value = existingUserId,
                    onValueChange = { },
                    label = stringResource(R.string.user_id),
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_user_id"),
                )
            }

            if (existingUserId == null) {
                Field(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password),
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_password"),
                )
            }

            Field(
                value = displayName,
                onValueChange = { displayName = it },
                label = stringResource(R.string.display_name_optional),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_display_name"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.server_admin),
                    style = SynapseText.BodyM,
                    color = c.text,
                )
                SynapseToggle(
                    checked = isAdmin,
                    onCheckedChange = { isAdmin = it },
                    modifier = Modifier.testTag("edit_admin_checkbox"),
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = SynapseText.BodyS,
                    color = c.danger,
                    modifier = Modifier.testTag("edit_error"),
                )
            }

            SynapseButton(
                text = if (state.isSaving) "" else {
                    if (existingUserId == null) stringResource(R.string.create) else stringResource(R.string.save)
                },
                onClick = {
                    if (existingUserId == null) {
                        val fullUserId = "@${username.trim()}:$serverName"
                        viewModel.createUser(
                            serverUrl = serverUrl,
                            userId = fullUserId,
                            password = password,
                            displayName = displayName.ifBlank { null },
                            admin = isAdmin,
                        )
                    } else {
                        viewModel.updateUser(
                            serverUrl = serverUrl,
                            userId = existingUserId,
                            displayName = displayName.ifBlank { null },
                            admin = if (isAdmin) true else null,
                        )
                    }
                },
                enabled = !state.isSaving && (existingUserId != null || username.isNotBlank()),
                variant = SynapseButtonVariant.Primary,
                size = SynapseButtonSize.Lg,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_save_button"),
            )

            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            // Bottom spacing
            androidx.compose.foundation.layout.Spacer(
                Modifier.padding(bottom = Tokens.Space.Xl),
            )
        }
    }
}

private fun serverNameFromUrl(serverUrl: String): String {
    return try {
        val uri = java.net.URI(serverUrl)
        var host = uri.host ?: return serverUrl
        // Use domain for Matrix ID (e.g. matrix.myserver.com -> myserver.com)
        if (host.startsWith("matrix.")) host = host.removePrefix("matrix.")
        val port = uri.port
        if (port > 0 && port != 80 && port != 443) "$host:$port" else host
    } catch (_: Exception) {
        val fallback = serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (fallback.startsWith("matrix.")) fallback.removePrefix("matrix.") else fallback
    }
}
