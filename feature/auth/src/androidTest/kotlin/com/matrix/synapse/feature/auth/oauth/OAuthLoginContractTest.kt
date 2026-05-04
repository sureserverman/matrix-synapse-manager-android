package com.matrix.synapse.feature.auth.oauth

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.openid.appauth.AuthorizationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OAuthLoginContractTest {

    private val contract = OAuthLoginContract()

    @Test
    fun parseResult_returns_Cancelled_for_RESULT_CANCELED() {
        val result = contract.parseResult(Activity.RESULT_CANCELED, null)
        assertEquals(OAuthLoginResult.Cancelled, result)
    }

    @Test
    fun parseResult_returns_Failure_when_intent_carries_authorization_exception() {
        val accessDenied = AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED
        val intent = accessDenied.toIntent()

        val result = contract.parseResult(Activity.RESULT_OK, intent)

        assertTrue("Expected Failure result, got: $result", result is OAuthLoginResult.Failure)
        val failure = result as OAuthLoginResult.Failure
        assertEquals(
            "Expected ACCESS_DENIED code",
            accessDenied.code,
            failure.error.code,
        )
    }
}
