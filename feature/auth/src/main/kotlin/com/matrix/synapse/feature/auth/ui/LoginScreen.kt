package com.matrix.synapse.feature.auth.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.feature.auth.oauth.OAuthLoginContract
import com.matrix.synapse.feature.auth.oauth.OAuthLoginResult

private val ScreenPadding = 24.dp
private val FieldSpacing = 16.dp
private val SectionSpacing = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    serverUrl: String,
    serverId: String,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Kick off strategy resolution once per serverUrl.
    LaunchedEffect(serverUrl) {
        viewModel.startLogin(serverUrl, serverId)
    }

    // Launch the OAuth consent intent exactly once per AwaitingConsent state.
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
            // Defensive: a device with no browser/Custom Tabs has no activity to
            // resolve the intent and would otherwise crash the app. Surface as a
            // recoverable error instead.
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
                    )
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

    Scaffold(
        topBar = {
            SynapseTopBar(title = stringResource(R.string.admin_login))
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FieldSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = serverUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(SectionSpacing))

            when (currentState) {
                is LoginState.Loading, is LoginState.AwaitingConsent -> {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("login_oauth_progress"),
                    )
                    if (currentState is LoginState.AwaitingConsent) {
                        Text(
                            text = "Opening browser…",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("login_oauth_opening_browser"),
                        )
                    }
                }

                LoginState.Idle -> {
                    // Password form for non-MAS servers (or after cancellation).
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("login_username"),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("login_password"),
                    )

                    Spacer(Modifier.height(SectionSpacing))

                    Button(
                        onClick = { viewModel.submitPassword(serverUrl, serverId, username, password) },
                        modifier = Modifier.fillMaxWidth().testTag("login_button"),
                    ) {
                        Text(stringResource(R.string.sign_in))
                    }
                }

                is LoginState.Error -> {
                    val errorMessage = if (currentState.isScopeDenied) {
                        stringResource(R.string.login_error_admin_scope_denied)
                    } else {
                        currentState.message
                    }
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("login_error"),
                    )

                    if (currentState.isScopeDenied) {
                        Button(
                            onClick = { viewModel.startLogin(serverUrl, serverId) },
                            modifier = Modifier.testTag("login_retry_button"),
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                is LoginState.Success -> {
                    // Handled above via LaunchedEffect; show nothing during navigation.
                }
            }
        }
    }
}
