package com.matrix.synapse.feature.auth.oauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

/**
 * ActivityResultContract that launches an AppAuth authorization Intent and parses
 * the redirect on return. Survives process death: AppAuth's
 * RedirectUriReceiverActivity is the entry-point Activity, declared in the
 * feature/auth manifest; the system delivers the redirect to a fresh process
 * instance and the response is recovered via AuthorizationResponse.fromIntent.
 */
class OAuthLoginContract : ActivityResultContract<Intent, OAuthLoginResult>() {

    override fun createIntent(context: Context, input: Intent): Intent = input

    override fun parseResult(resultCode: Int, intent: Intent?): OAuthLoginResult {
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return OAuthLoginResult.Cancelled
        }
        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        return when {
            response != null -> OAuthLoginResult.Success(response)
            ex != null -> OAuthLoginResult.Failure(ex)
            else -> OAuthLoginResult.Cancelled
        }
    }
}

sealed interface OAuthLoginResult {
    data class Success(val response: AuthorizationResponse) : OAuthLoginResult
    data class Failure(val error: AuthorizationException) : OAuthLoginResult
    data object Cancelled : OAuthLoginResult
}
