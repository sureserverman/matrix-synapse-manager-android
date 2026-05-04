package com.matrix.synapse.manager

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OAuthRedirectIntentFilterTest {

    @Test
    fun oauth_redirect_uri_resolves_to_appauth_receiver() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("com.matrix.synapse.manager://oauth/redirect?code=abc&state=xyz"),
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(context.packageName)
        }
        val resolved = context.packageManager.queryIntentActivities(intent, 0)
        assertEquals(
            "Expected exactly one activity to handle the OAuth redirect, found " +
                resolved.map { it.activityInfo.name },
            1,
            resolved.size,
        )
        assertTrue(
            "Expected RedirectUriReceiverActivity, got " + resolved[0].activityInfo.name,
            resolved[0].activityInfo.name.endsWith("RedirectUriReceiverActivity"),
        )
    }
}
