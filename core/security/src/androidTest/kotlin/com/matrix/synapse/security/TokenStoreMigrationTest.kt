package com.matrix.synapse.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test for the legacy EncryptedSharedPreferences -> KeystoreCrypto migration in
 * [TokenStoreImpl]. Runs on a device/emulator (Keystore + EncryptedSharedPreferences).
 */
@RunWith(AndroidJUnit4::class)
class TokenStoreMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val serverId = "11111111-2222-3333-4444-555555555555"

    @Before
    fun clean() {
        // deleteSharedPreferences does NOT reset an already-loaded in-process instance, so an
        // earlier test's migrated flag would leak in. clear().commit() empties the live cache.
        context.getSharedPreferences("secure_token_store_v2", Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteSharedPreferences("secure_token_store_v2")
        context.deleteSharedPreferences("secure_token_store")
    }

    @After
    fun tearDown() = clean()

    @Test
    fun migrates_legacy_tokens_then_deletes_old_file() = runBlocking {
        // Seed the legacy EncryptedSharedPreferences exactly as the old TokenStoreImpl wrote it.
        val legacy = openLegacy()
        legacy.edit()
            .putString("access_$serverId", "syt_access")
            .putString("refresh_$serverId", "syt_refresh")
            .putString("user_$serverId", "@admin:hs.example")
            .putString("oauth_client_$serverId", "client_abc")
            .putLong("issued_at_$serverId", 1_700_000_000L)
            .commit()
        assertFileExists("secure_token_store")

        val store = TokenStoreImpl(context, KeystoreCrypto())

        // Values survive the migration, decrypted through the new Keystore-backed path.
        assertEquals("syt_access", store.accessTokenFlow(serverId).first())
        assertEquals("syt_refresh", store.refreshTokenFlow(serverId).first())
        assertEquals("@admin:hs.example", store.currentUserIdFlow(serverId).first())
        assertEquals("client_abc", store.oauthClientIdFlow(serverId).first())
        assertEquals(1_700_000_000L, store.tokenIssuedAtFlow(serverId).first())

        // Legacy file is gone after a successful migration.
        assertFileDoesNotExist("secure_token_store")
    }

    @Test
    fun fresh_install_with_no_legacy_file_is_empty() = runBlocking {
        val store = TokenStoreImpl(context, KeystoreCrypto())
        assertNull(store.accessTokenFlow(serverId).first())
        // A fresh install must not create the legacy EncryptedSharedPreferences file.
        assertFileDoesNotExist("secure_token_store")
    }

    private fun openLegacy() = EncryptedSharedPreferences.create(
        context,
        "secure_token_store",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun prefsFile(name: String) =
        File(File(context.applicationInfo.dataDir, "shared_prefs"), "$name.xml")

    private fun assertFileExists(name: String) =
        org.junit.Assert.assertTrue("$name.xml should exist", prefsFile(name).exists())

    private fun assertFileDoesNotExist(name: String) =
        assertFalse("$name.xml should be deleted", prefsFile(name).exists())
}
