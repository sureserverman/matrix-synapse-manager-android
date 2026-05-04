package com.matrix.synapse.feature.auth.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matrix.synapse.feature.auth.domain.LoginResult
import com.matrix.synapse.feature.auth.domain.LoginStrategy
import com.matrix.synapse.feature.auth.domain.LoginStrategyResolver
import com.matrix.synapse.feature.auth.domain.LoginUseCase
import com.matrix.synapse.feature.auth.domain.OAuthLoginUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A minimal fake ViewModel backed by a mutable StateFlow so tests can inject
 * any desired state without Hilt overhead. The screen takes a default
 * [LoginViewModel] parameter which tests override.
 */
private class FakeLoginViewModel(
    initialState: LoginState = LoginState.Idle,
    loginUseCase: LoginUseCase = mockk(relaxed = true),
    resolver: LoginStrategyResolver = mockk(relaxed = true),
    oauthLoginUseCase: OAuthLoginUseCase = mockk(relaxed = true),
) : LoginViewModel(loginUseCase, resolver, oauthLoginUseCase) {

    private val _stateOverride = MutableStateFlow<LoginState>(initialState)

    // Shadow the parent's state with our injectable one.
    override val state: StateFlow<LoginState> get() = _stateOverride.asStateFlow()

    fun setState(s: LoginState) { _stateOverride.value = s }

    // Override startLogin to a no-op so the LaunchedEffect on the screen does not
    // invoke the production strategy resolver (whose mockk-relaxed return value
    // is not a valid LoginStrategy and crashes the exhaustive `when`).
    override fun startLogin(serverUrl: String, serverId: String) { /* no-op */ }
}

@RunWith(AndroidJUnit4::class)
class LoginScreenMasBranchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun password_form_visible_when_state_is_Idle() {
        val fakeVm = FakeLoginViewModel(initialState = LoginState.Idle)
        composeTestRule.setContent {
            LoginScreen(
                serverUrl = "https://example.com",
                serverId = "server1",
                onLoginSuccess = {},
                viewModel = fakeVm,
            )
        }
        composeTestRule.onNodeWithTag("login_username").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_password").assertIsDisplayed()
    }

    @Test
    fun progress_visible_when_state_is_AwaitingConsent() {
        val fakeIntent = mockk<Intent>(relaxed = true)
        val fakeVm = FakeLoginViewModel(initialState = LoginState.AwaitingConsent(fakeIntent))
        composeTestRule.setContent {
            LoginScreen(
                serverUrl = "https://mas.example.com",
                serverId = "server1",
                onLoginSuccess = {},
                viewModel = fakeVm,
            )
        }
        // Either the progress indicator or the "Opening browser" label should be visible.
        composeTestRule.onNodeWithTag("login_oauth_opening_browser").assertIsDisplayed()
    }

    @Test
    fun retry_button_visible_when_scope_denied() {
        val fakeVm = FakeLoginViewModel(
            initialState = LoginState.Error(
                message = "Admin scope denied",
                isScopeDenied = true,
            )
        )
        composeTestRule.setContent {
            LoginScreen(
                serverUrl = "https://mas.example.com",
                serverId = "server1",
                onLoginSuccess = {},
                viewModel = fakeVm,
            )
        }
        composeTestRule.onNodeWithTag("login_retry_button").assertIsDisplayed()
    }
}
