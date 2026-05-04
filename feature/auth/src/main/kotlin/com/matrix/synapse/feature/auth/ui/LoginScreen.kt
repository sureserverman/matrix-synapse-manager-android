package com.matrix.synapse.feature.auth.ui

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.matrix.synapse.feature.auth.oauth.OAuthLoginContract
import com.matrix.synapse.feature.auth.oauth.OAuthLoginResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    serverUrl: String,
    serverId: String,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = SynapseTheme.colors

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(serverUrl) {
        viewModel.startLogin(serverUrl, serverId)
    }

    val oauthLauncher = rememberLauncherForActivityResult(OAuthLoginContract()) { result ->
        when (result) {
            is OAuthLoginResult.Success -> viewModel.completeOauth(serverId, result.response)
            is OAuthLoginResult.Failure -> viewModel.onOauthFailed(result.error)
            OAuthLoginResult.Cancelled -> viewModel.onOauthCancelled()
        }
    }

    val currentState = state
    LaunchedEffect(currentState) {
        if (currentState is LoginState.AwaitingConsent) {
            try {
                oauthLauncher.launch(currentState.authIntent)
            } catch (e: android.content.ActivityNotFoundException) {
                viewModel.onOauthFailed(
                    net.openid.appauth.AuthorizationException(
                        net.openid.appauth.AuthorizationException.TYPE_GENERAL_ERROR,
                        net.openid.appauth.AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW.code,
                        "no_browser",
                        e.message ?: "No browser available to handle OAuth consent",
                        null,
                        e,
                    ),
                )
            }
        }
    }

    if (currentState is LoginState.Success) {
        LaunchedEffect(Unit) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    SynapseScaffold(
        topBar = { SynapseTopBar(title = stringResource(R.string.admin_login)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Tokens.Space.Xl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.Lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Tokens.Space.Xl))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(Tokens.Radius.Md))
                    .background(c.surface3),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(c.accent),
                )
            }

            Text(
                text = "Sign in to Synapse",
                style = SynapseText.Display,
                color = c.text,
            )
            Text(
                text = serverUrl,
                style = SynapseText.Mono,
                color = c.textMuted,
                modifier = Modifier.testTag("login_server_url"),
            )

            Spacer(Modifier.height(Tokens.Space.Sm))

            when (currentState) {
                is LoginState.Loading, is LoginState.AwaitingConsent -> {
                    CircularProgressIndicator(
                        color = c.accent,
                        modifier = Modifier.testTag("login_oauth_progress"),
                    )
                    if (currentState is LoginState.AwaitingConsent) {
                        Text(
                            text = "Opening browser…",
                            style = SynapseText.BodyM,
                            color = c.textMuted,
                            modifier = Modifier.testTag("login_oauth_opening_browser"),
                        )
                    }
                }

                LoginState.Idle -> {
                    Field(
                        value = username,
                        onValueChange = { username = it },
                        label = stringResource(R.string.username),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username"),
                    )
                    Field(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.password),
                        isPassword = true,
                        keyboardType = KeyboardType.Password,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password"),
                    )

                    Spacer(Modifier.height(Tokens.Space.Sm))

                    SynapseButton(
                        text = stringResource(R.string.sign_in),
                        onClick = { viewModel.submitPassword(serverUrl, serverId, username, password) },
                        size = SynapseButtonSize.Lg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_button"),
                    )

                    SecurityCallout()
                }

                is LoginState.Error -> {
                    val errorMessage = if (currentState.isScopeDenied) {
                        stringResource(R.string.login_error_admin_scope_denied)
                    } else {
                        currentState.message
                    }
                    Text(
                        text = errorMessage,
                        style = SynapseText.BodyS,
                        color = c.danger,
                        modifier = Modifier.testTag("login_error"),
                    )
                    if (currentState.isScopeDenied) {
                        SynapseButton(
                            text = stringResource(R.string.retry),
                            onClick = { viewModel.startLogin(serverUrl, serverId) },
                            modifier = Modifier.testTag("login_retry_button"),
                        )
                    }
                }

                is LoginState.Success -> Unit
            }
        }
    }
}

@Composable
private fun SecurityCallout() {
    val c = SynapseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.Md))
            .background(c.surface2)
            .padding(Tokens.Space.Lg),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.Md),
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Tokens are stored in the Android Keystore. Passwords are never stored.",
            style = SynapseText.BodyS,
            color = c.textMuted,
        )
    }
}
