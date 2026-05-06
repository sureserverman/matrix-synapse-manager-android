package com.matrix.synapse.feature.auth.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.auth.domain.LoginResult
import com.matrix.synapse.feature.auth.domain.LoginStrategy
import com.matrix.synapse.feature.auth.domain.LoginStrategyResolver
import com.matrix.synapse.feature.auth.domain.LoginUseCase
import com.matrix.synapse.feature.auth.domain.OAuthLoginUseCase
import com.matrix.synapse.feature.auth.domain.TokenLoginUseCase
import com.matrix.synapse.feature.auth.oauth.MasAdminScopeDeniedException
import com.matrix.synapse.feature.auth.oauth.PendingOauth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import javax.inject.Inject

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class AwaitingConsent(val authIntent: Intent) : LoginState
    data class Success(val result: LoginResult) : LoginState
    data class Error(val message: String, val isScopeDenied: Boolean = false) : LoginState
}

@HiltViewModel
open class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val resolver: LoginStrategyResolver,
    private val oauthLoginUseCase: OAuthLoginUseCase,
    private val tokenLoginUseCase: TokenLoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    open val state: StateFlow<LoginState> = _state.asStateFlow()

    private var pendingOauth: PendingOauth? = null

    /** Resolves the login strategy for [serverUrl] and transitions to the appropriate state. */
    open fun startLogin(serverUrl: String, serverId: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            when (val strategy = resolver.resolve(serverUrl)) {
                is LoginStrategy.Oauth -> {
                    val result = runCatching { oauthLoginUseCase.beginOauth(serverId, strategy) }
                    result.fold(
                        onSuccess = { begin ->
                            pendingOauth = begin.pending
                            _state.value = LoginState.AwaitingConsent(begin.authIntent)
                        },
                        onFailure = { ex ->
                            _state.value = LoginState.Error(ex.message ?: "Failed to start OAuth")
                        },
                    )
                }
                is LoginStrategy.Password -> {
                    _state.value = LoginState.Idle
                }
            }
        }
    }

    /** Continues with the password form after strategy resolution returned Password. */
    fun submitPassword(serverUrl: String, serverId: String, username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Username and password are required")
            return
        }
        viewModelScope.launch {
            _state.value = LoginState.Loading
            val result = loginUseCase.login(
                serverUrl = serverUrl,
                serverId = serverId,
                username = username,
                password = password,
            )
            _state.value = result.fold(
                onSuccess = { LoginState.Success(it) },
                onFailure = { LoginState.Error(it.message ?: "Login failed") },
            )
        }
    }

    /** Legacy entry-point kept for call sites that haven't migrated to [startLogin]. */
    fun login(serverUrl: String, serverId: String, username: String, password: String) =
        submitPassword(serverUrl, serverId, username, password)

    /**
     * Alternative login: skip /_matrix/client/v3/login entirely and use a pre-existing
     * access token. Validated via /_matrix/client/v3/account/whoami. Available on
     * every server regardless of strategy — useful when the strategy resolver can't
     * issue an admin-scoped token (e.g. MAS-fronted servers without can_request_admin)
     * but the user already has a working token from another tool.
     */
    fun submitToken(serverUrl: String, serverId: String, token: String) {
        if (token.isBlank()) {
            _state.value = LoginState.Error("Access token must not be empty")
            return
        }
        viewModelScope.launch {
            _state.value = LoginState.Loading
            val result = tokenLoginUseCase.login(serverUrl, serverId, token)
            _state.value = result.fold(
                onSuccess = { LoginState.Success(it) },
                onFailure = { LoginState.Error(it.message ?: "Token login failed") },
            )
        }
    }

    fun completeOauth(serverId: String, response: AuthorizationResponse) {
        val pending = pendingOauth ?: run {
            _state.value = LoginState.Error("No pending OAuth session")
            return
        }
        viewModelScope.launch {
            _state.value = LoginState.Loading
            val result = oauthLoginUseCase.completeOauth(pending, response)
            _state.value = result.fold(
                onSuccess = { LoginState.Success(it) },
                onFailure = { ex ->
                    when (ex) {
                        is MasAdminScopeDeniedException ->
                            LoginState.Error(ex.message ?: "Admin scope denied", isScopeDenied = true)
                        else ->
                            LoginState.Error(ex.message ?: "Login failed")
                    }
                },
            )
        }
    }

    fun onOauthCancelled() {
        pendingOauth = null
        _state.value = LoginState.Idle
    }

    fun onOauthFailed(error: AuthorizationException) {
        pendingOauth = null
        _state.value = LoginState.Error(
            error.errorDescription ?: error.error ?: "Authorization failed"
        )
    }

    fun resetState() {
        _state.value = LoginState.Idle
    }
}
