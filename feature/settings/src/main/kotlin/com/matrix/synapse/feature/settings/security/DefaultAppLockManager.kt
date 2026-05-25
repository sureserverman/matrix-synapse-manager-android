package com.matrix.synapse.feature.settings.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@Singleton
class DefaultAppLockManager @Inject constructor(
    @ApplicationContext context: Context,
) : AppLockManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _isLockEnabled = MutableStateFlow(encryptedPrefs.getBoolean(KEY_ENABLED, false))
    override val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _isLocked = MutableStateFlow(_isLockEnabled.value)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    override fun pinExists(): Boolean =
        encryptedPrefs.contains(KEY_HASH)

    override suspend fun setPin(pin: String) {
        require(pin.length == PIN_LENGTH) { "PIN must be $PIN_LENGTH digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt, PBKDF2_ITERATIONS)
        encryptedPrefs.edit()
            .putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_HASH, Base64.getEncoder().encodeToString(hash))
            .putInt(KEY_ITERATIONS, PBKDF2_ITERATIONS)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    override fun verifyPin(pin: String): Boolean {
        // Refuse all input (even a correct PIN) while locked out after too many failures.
        if (System.currentTimeMillis() < encryptedPrefs.getLong(KEY_LOCKOUT_UNTIL, 0L)) return false
        if (pin.length != PIN_LENGTH) return false
        val saltB64 = encryptedPrefs.getString(KEY_SALT, null) ?: return false
        val storedHashB64 = encryptedPrefs.getString(KEY_HASH, null) ?: return false
        val iterations = encryptedPrefs.getInt(KEY_ITERATIONS, LEGACY_PBKDF2_ITERATIONS)
        val salt = Base64.getDecoder().decode(saltB64)
        val storedHash = Base64.getDecoder().decode(storedHashB64)
        val computedHash = hashPin(pin, salt, iterations)
        val matches = constantTimeEquals(storedHash, computedHash)
        if (matches) recordSuccess() else recordFailure()
        return matches
    }

    override suspend fun clearPin() {
        encryptedPrefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .remove(KEY_ITERATIONS)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    override suspend fun setEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
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
        encryptedPrefs.edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    private fun recordFailure() {
        val attempts = encryptedPrefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = encryptedPrefs.edit()
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Start a lockout window and reset the counter so the next window is a fresh 5 tries.
            editor.putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_MS)
        } else {
            editor.putInt(KEY_FAILED_ATTEMPTS, attempts)
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

    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_ENABLED = "lock_enabled"
    }
}
