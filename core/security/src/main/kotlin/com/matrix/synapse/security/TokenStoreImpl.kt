package com.matrix.synapse.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [SecureTokenStore].
 *
 * Values are encrypted with [KeystoreCrypto] (AES-256-GCM via the Android Keystore) and stored
 * as ciphertext in a plain [SharedPreferences] file. This replaces the deprecated
 * androidx.security:security-crypto. Pref-key names embed the random-UUID serverId only, which
 * is not sensitive (it is not the Matrix server_name), so keys are not encrypted.
 *
 * Passwords are never stored here — access/refresh tokens, user id, and OAuth client id only.
 *
 * On first run after upgrade, [migrateIfNeeded] copies any data from the legacy
 * EncryptedSharedPreferences file into this store and deletes the old file. The legacy read path
 * is the only remaining use of security-crypto and is slated for removal once installs have
 * migrated.
 */
@Singleton
class TokenStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crypto: KeystoreCrypto,
) : SecureTokenStore {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { migrateIfNeeded(it) }
    }

    // StateFlow to allow reactive observation even though SharedPreferences is synchronous.
    private val _state = MutableStateFlow(Unit)

    private fun readString(key: String): String? =
        prefs.getString(key, null)?.let { crypto.decrypt(it) }

    private fun writeString(key: String, value: String) {
        prefs.edit().putString(key, crypto.encrypt(value)).apply()
        _state.value = Unit
    }

    override fun accessTokenFlow(serverId: String): Flow<String?> =
        _state.map { readString(accessKey(serverId)) }

    override suspend fun saveToken(serverId: String, accessToken: String) =
        writeString(accessKey(serverId), accessToken)

    override suspend fun saveUserId(serverId: String, userId: String) =
        writeString(userIdKey(serverId), userId)

    override fun currentUserIdFlow(serverId: String): Flow<String?> =
        _state.map { readString(userIdKey(serverId)) }

    override suspend fun clearTokens(serverId: String) {
        prefs.edit()
            .remove(accessKey(serverId))
            .remove(userIdKey(serverId))
            .remove(refreshKey(serverId))
            .remove(oauthClientKey(serverId))
            .remove(issuedAtKey(serverId))
            .apply()
        _state.value = Unit
    }

    override fun refreshTokenFlow(serverId: String): Flow<String?> =
        _state.map { readString(refreshKey(serverId)) }

    override suspend fun saveRefreshToken(serverId: String, token: String) =
        writeString(refreshKey(serverId), token)

    override fun oauthClientIdFlow(serverId: String): Flow<String?> =
        _state.map { readString(oauthClientKey(serverId)) }

    override suspend fun saveOAuthClientId(serverId: String, clientId: String) =
        writeString(oauthClientKey(serverId), clientId)

    override fun tokenIssuedAtFlow(serverId: String): Flow<Long?> =
        _state.map { readString(issuedAtKey(serverId))?.toLongOrNull() }

    override suspend fun saveTokenIssuedAt(serverId: String, epochSeconds: Long) =
        writeString(issuedAtKey(serverId), epochSeconds.toString())

    /**
     * One-time copy of legacy EncryptedSharedPreferences data into this store. Resilient by
     * design: any failure (the documented EncryptedSharedPreferences keyset-corruption case
     * included) is swallowed and the legacy file dropped, so the worst case is a re-login rather
     * than a crash. Marks completion with [KEY_MIGRATED] so it runs at most once.
     */
    private fun migrateIfNeeded(newPrefs: SharedPreferences) {
        if (newPrefs.getBoolean(KEY_MIGRATED, false)) return
        if (!legacyPrefsFileExists()) {
            newPrefs.edit().putBoolean(KEY_MIGRATED, true).apply()
            return
        }
        try {
            val legacy = openLegacyPrefs()
            val editor = newPrefs.edit()
            for ((key, value) in legacy.all) {
                when (value) {
                    is String -> editor.putString(key, crypto.encrypt(value))
                    is Long -> editor.putString(key, crypto.encrypt(value.toString()))
                    // Other types were never written by this store; skip defensively.
                }
            }
            editor.putBoolean(KEY_MIGRATED, true).apply()
        } catch (e: Exception) {
            // Unreadable legacy keyset — give up gracefully; user re-authenticates.
            android.util.Log.w("TokenStoreMigration", "legacy token migration failed: ${e.javaClass.simpleName}", e)
            newPrefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        } finally {
            context.deleteSharedPreferences(LEGACY_PREFS_NAME)
        }
    }

    private fun legacyPrefsFileExists(): Boolean =
        File(File(context.applicationInfo.dataDir, "shared_prefs"), "$LEGACY_PREFS_NAME.xml").exists()

    private fun openLegacyPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            LEGACY_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun accessKey(serverId: String) = "access_$serverId"
    private fun userIdKey(serverId: String) = "user_$serverId"
    private fun refreshKey(serverId: String) = "refresh_$serverId"
    private fun oauthClientKey(serverId: String) = "oauth_client_$serverId"
    private fun issuedAtKey(serverId: String) = "issued_at_$serverId"

    private companion object {
        const val PREFS_NAME = "secure_token_store_v2"
        const val LEGACY_PREFS_NAME = "secure_token_store"
        const val KEY_MIGRATED = "_migrated_from_esp"
    }
}
