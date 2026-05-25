package com.matrix.synapse.feature.settings.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.matrix.synapse.security.KeystoreCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val PIN_LENGTH = 4

// verifyPin() runs on the UI thread, so iterations are bounded to keep unlock responsive
// on low-end devices (~10x the old cost). For a 4-digit PIN the primary brute-force defence
// is the attempt lockout below, not the KDF cost. Hashes written before this change are
// verified with their original iteration count (read from KEY_ITERATIONS, defaulting to
// LEGACY_PBKDF2_ITERATIONS) so existing PINs keep working; a PIN re-set upgrades it.
private const val PBKDF2_ITERATIONS = 100_000
private const val LEGACY_PBKDF2_ITERATIONS = 10_000

// After MAX_FAILED_ATTEMPTS consecutive wrong PINs, reject all input (even a correct PIN)
// for LOCKOUT_MS to throttle online guessing of the 10_000-value PIN space.
private const val MAX_FAILED_ATTEMPTS = 5
private const val LOCKOUT_MS = 30_000L

private const val KEY_SALT = "pin_salt"
private const val KEY_HASH = "pin_hash"
private const val KEY_ITERATIONS = "pin_iterations"
private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
private const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"

/**
 * App-lock PIN store.
 *
 * Every value is encrypted with [KeystoreCrypto] (AES-256-GCM via the Android Keystore) and held
 * as ciphertext in a plain [SharedPreferences] file — replacing the deprecated
 * androidx.security:security-crypto. Encrypting authenticated values (not just the PIN hash) keeps
 * `lock_enabled` tamper-resistant: an attacker with filesystem access cannot forge it to false to
 * bypass the lock. On upgrade, [migrateIfNeeded] copies the legacy EncryptedSharedPreferences file
 * into this store once and deletes it.
 */
@Singleton
class DefaultAppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crypto: KeystoreCrypto,
) : AppLockManager {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { migrateIfNeeded(it) }
    }

    private val _isLockEnabled = MutableStateFlow(getBoolDec(KEY_ENABLED, false))
    override val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _isLocked = MutableStateFlow(_isLockEnabled.value)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    override fun pinExists(): Boolean =
        prefs.contains(KEY_HASH)

    override suspend fun setPin(pin: String) {
        require(pin.length == PIN_LENGTH) { "PIN must be $PIN_LENGTH digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt, PBKDF2_ITERATIONS)
        prefs.edit()
            .putString(KEY_SALT, crypto.encrypt(Base64.getEncoder().encodeToString(salt)))
            .putString(KEY_HASH, crypto.encrypt(Base64.getEncoder().encodeToString(hash)))
            .putString(KEY_ITERATIONS, crypto.encrypt(PBKDF2_ITERATIONS.toString()))
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    override fun verifyPin(pin: String): Boolean {
        // Refuse all input (even a correct PIN) while locked out after too many failures.
        if (System.currentTimeMillis() < getLongDec(KEY_LOCKOUT_UNTIL, 0L)) return false
        if (pin.length != PIN_LENGTH) return false
        val saltB64 = getDec(KEY_SALT) ?: return false
        val storedHashB64 = getDec(KEY_HASH) ?: return false
        val iterations = getIntDec(KEY_ITERATIONS, LEGACY_PBKDF2_ITERATIONS)
        val salt = Base64.getDecoder().decode(saltB64)
        val storedHash = Base64.getDecoder().decode(storedHashB64)
        val computedHash = hashPin(pin, salt, iterations)
        val matches = constantTimeEquals(storedHash, computedHash)
        if (matches) recordSuccess() else recordFailure()
        return matches
    }

    override suspend fun clearPin() {
        prefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .remove(KEY_ITERATIONS)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    override suspend fun setEnabled(enabled: Boolean) {
        prefs.edit().putString(KEY_ENABLED, crypto.encrypt(enabled.toString())).apply()
        _isLockEnabled.value = enabled
        if (!enabled) _isLocked.value = false
    }

    override fun lock() {
        if (_isLockEnabled.value) _isLocked.value = true
    }

    override fun unlock() {
        _isLocked.value = false
    }

    private fun recordSuccess() {
        prefs.edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    private fun recordFailure() {
        val attempts = getIntDec(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit()
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Start a lockout window and reset the counter so the next window is a fresh 5 tries.
            editor.putString(KEY_FAILED_ATTEMPTS, crypto.encrypt("0"))
                .putString(KEY_LOCKOUT_UNTIL, crypto.encrypt((System.currentTimeMillis() + LOCKOUT_MS).toString()))
        } else {
            editor.putString(KEY_FAILED_ATTEMPTS, crypto.encrypt(attempts.toString()))
        }
        editor.apply()
    }

    private fun hashPin(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            iterations,
            256,
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec)
        return key.encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private fun getDec(key: String): String? = prefs.getString(key, null)?.let { crypto.decrypt(it) }
    private fun getBoolDec(key: String, default: Boolean): Boolean = getDec(key)?.toBooleanStrictOrNull() ?: default
    private fun getIntDec(key: String, default: Int): Int = getDec(key)?.toIntOrNull() ?: default
    private fun getLongDec(key: String, default: Long): Long = getDec(key)?.toLongOrNull() ?: default

    /**
     * One-time copy of the legacy EncryptedSharedPreferences app-lock data into this store. Each
     * value is re-encrypted through [KeystoreCrypto] as a string. Resilient: any failure is
     * swallowed (worst case: the user re-sets their PIN) and the legacy file dropped.
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
                val asString = when (value) {
                    is String -> value
                    is Boolean -> value.toString()
                    is Int -> value.toString()
                    is Long -> value.toString()
                    else -> null
                }
                if (asString != null) editor.putString(key, crypto.encrypt(asString))
            }
            editor.putBoolean(KEY_MIGRATED, true).apply()
        } catch (e: Exception) {
            android.util.Log.w("AppLockMigration", "legacy app-lock migration failed: ${e.javaClass.simpleName}", e)
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

    companion object {
        private const val PREFS_NAME = "app_lock_prefs_v2"
        private const val LEGACY_PREFS_NAME = "app_lock_prefs"
        private const val KEY_ENABLED = "lock_enabled"
        private const val KEY_MIGRATED = "_migrated_from_esp"
    }
}
