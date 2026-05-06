package com.matrix.synapse.feature.auth.ui

import android.content.Intent
import app.cash.turbine.test
import com.matrix.synapse.feature.auth.domain.LoginResult
import com.matrix.synapse.feature.auth.domain.LoginStrategy
import com.matrix.synapse.feature.auth.domain.LoginStrategyResolver
import com.matrix.synapse.feature.auth.domain.LoginUseCase
import com.matrix.synapse.feature.auth.domain.OAuthLoginUseCase
import com.matrix.synapse.feature.auth.domain.OauthBegin
import com.matrix.synapse.feature.auth.oauth.MasAdminScopeDeniedException
import com.matrix.synapse.feature.auth.oauth.PendingOauth
import com.matrix.synapse.network.auth.AuthMetadata
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.openid.appauth.AuthorizationResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginViewModelOAuthTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockResolver = mockk<LoginStrategyResolver>()
    private val mockOAuthLoginUseCase = mockk<OAuthLoginUseCase>()
    private val mockLoginUseCase = mockk<LoginUseCase>()

    private lateinit var viewModel: LoginViewModel

    private val fakeMetadata = AuthMetadata(
        issuer = "https://auth.example.com",
        authorizationEndpoint = "https://auth.example.com/authorize",
        tokenEndpoint = "https://auth.example.com/token",
    )

    private val fakePending = PendingOauth(
        serverId = "server1",
        homeserverUrl = "https://example.com",
        metadata = fakeMetadata,
        clientId = "client_123",
        deviceId = "DEV1",
        codeVerifier = "verifier_abc",
    )

    private val fakeIntent = mockk<Intent>(relaxed = true)

    private val fakeBegin = OauthBegin(
        authIntent = fakeIntent,
        pending = fakePending,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(
            mockLoginUseCase,
            mockResolver,
            mockOAuthLoginUseCase,
            mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun oauth_path_drives_Loading_to_AwaitingConsent_to_Success() = runTest {
        coEvery { mockResolver.resolve(any()) } returns
            LoginStrategy.Oauth("https://example.com", fakeMetadata)
        coEvery { mockOAuthLoginUseCase.beginOauth(any(), any()) } returns fakeBegin
        val fakeResponse = mockk<AuthorizationResponse>(relaxed = true)
        coEvery { mockOAuthLoginUseCase.completeOauth(any(), fakeResponse) } returns
            Result.success(LoginResult("@user:example.com", "DEV1", setOf("openid", "urn:synapse:admin:*")))

        viewModel.state.test {
            assertEquals(LoginState.Idle, awaitItem())

            viewModel.startLogin("https://example.com", "server1")
            assertEquals(LoginState.Loading, awaitItem())
            assertEquals(LoginState.AwaitingConsent(fakeIntent), awaitItem())

            viewModel.completeOauth("server1", fakeResponse)
            assertEquals(LoginState.Loading, awaitItem())
            val success = awaitItem()
            assertTrue("Expected Success state", success is LoginState.Success)
        }
    }

    @Test
    fun password_path_does_not_emit_AwaitingConsent() = runTest {
        coEvery { mockResolver.resolve(any()) } returns
            LoginStrategy.Password("https://legacy.example.com")

        viewModel.state.test {
            assertEquals(LoginState.Idle, awaitItem())

            viewModel.startLogin("https://legacy.example.com", "server1")
            assertEquals(LoginState.Loading, awaitItem())
            // Password path resolves back to Idle for the UI to show the form
            assertEquals(LoginState.Idle, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun scope_denied_emits_Error_with_isScopeDenied_true() = runTest {
        coEvery { mockResolver.resolve(any()) } returns
            LoginStrategy.Oauth("https://example.com", fakeMetadata)
        coEvery { mockOAuthLoginUseCase.beginOauth(any(), any()) } returns fakeBegin
        val fakeResponse = mockk<AuthorizationResponse>(relaxed = true)
        coEvery { mockOAuthLoginUseCase.completeOauth(any(), fakeResponse) } returns
            Result.failure(MasAdminScopeDeniedException(setOf("openid")))

        viewModel.state.test {
            assertEquals(LoginState.Idle, awaitItem())

            viewModel.startLogin("https://example.com", "server1")
            awaitItem() // Loading
            awaitItem() // AwaitingConsent

            viewModel.completeOauth("server1", fakeResponse)
            awaitItem() // Loading
            val error = awaitItem()
            assertTrue("Expected Error with isScopeDenied", error is LoginState.Error)
            val errorState = error as LoginState.Error
            assertTrue("Expected isScopeDenied=true", errorState.isScopeDenied)
            assertTrue(
                "Expected mas-cli in message: ${errorState.message}",
                errorState.message.contains("mas-cli"),
            )
        }
    }
}
