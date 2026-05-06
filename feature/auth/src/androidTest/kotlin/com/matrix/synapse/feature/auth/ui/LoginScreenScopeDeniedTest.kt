package com.matrix.synapse.feature.auth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.feature.auth.domain.LoginStrategyResolver
import com.matrix.synapse.feature.auth.domain.LoginUseCase
import com.matrix.synapse.feature.auth.domain.OAuthLoginUseCase
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scope-denial specific UI tests. Uses the same [FakeLoginViewModelForScope] pattern
 * as [LoginScreenMasBranchTest] but additionally verifies mock interactions.
 */
private class FakeLoginViewModelForScope(
    initialState: LoginState,
    loginUseCase: LoginUseCase = mockk(relaxed = true),
    resolver: LoginStrategyResolver = mockk(relaxed = true),
    oauthLoginUseCase: OAuthLoginUseCase = mockk(relaxed = true),
    tokenLoginUseCase: com.matrix.synapse.feature.auth.domain.TokenLoginUseCase = mockk(relaxed = true),
) : LoginViewModel(loginUseCase, resolver, oauthLoginUseCase, tokenLoginUseCase) {

    private val _stateOverride = MutableStateFlow<LoginState>(initialState)
    override val state: StateFlow<LoginState> get() = _stateOverride.asStateFlow()

    // Track calls to startLogin so tests can verify them.
    var startLoginCallCount = 0

    override fun startLogin(serverUrl: String, serverId: String) {
        startLoginCallCount++
    }
}

@RunWith(AndroidJUnit4::class)
class LoginScreenScopeDeniedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val scopeDeniedState = LoginState.Error(
        message = "scope denied",
        isScopeDenied = true,
    )

    @Test
    fun displays_operator_command_in_message() {
        val fakeVm = FakeLoginViewModelForScope(initialState = scopeDeniedState)
        composeTestRule.setContent {
            LoginScreen(
                serverUrl = "https://mas.example.com",
                serverId = "server1",
                onLoginSuccess = {},
                viewModel = fakeVm,
            )
        }

        val expectedText = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.login_error_admin_scope_denied)

        // The string resource contains "mas-cli manage set-can-request-admin".
        composeTestRule.onNodeWithText(expectedText, substring = false).assertIsDisplayed()
    }

    @Test
    fun retry_button_re_invokes_startLogin() {
        val fakeVm = FakeLoginViewModelForScope(initialState = scopeDeniedState)
        composeTestRule.setContent {
            LoginScreen(
                serverUrl = "https://mas.example.com",
                serverId = "server1",
                onLoginSuccess = {},
                viewModel = fakeVm,
            )
        }

        composeTestRule.onNodeWithTag("login_retry_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_retry_button").performClick()

        // The LaunchedEffect on serverUrl also calls startLogin once on composition.
        // After button click, startLoginCallCount should be >= 2 (init + retry).
        assert(fakeVm.startLoginCallCount >= 2) {
            "Expected startLogin to be called at least twice (initial + retry), got ${fakeVm.startLoginCallCount}"
        }
    }
}
