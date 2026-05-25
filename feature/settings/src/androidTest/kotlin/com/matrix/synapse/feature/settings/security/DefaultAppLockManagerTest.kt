package com.matrix.synapse.feature.settings.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matrix.synapse.security.KeystoreCrypto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Instrumented tests for [DefaultAppLockManager] — exercises the real Keystore + the legacy
 * EncryptedSharedPreferences migration, so it runs on a device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class DefaultAppLockManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clean() {
        // clear() resets the in-process cached instance (deleteSharedPreferences alone does not).
        context.getSharedPreferences("app_lock_prefs_v2", Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteSharedPreferences("app_lock_prefs_v2")
        context.deleteSharedPreferences("app_lock_prefs")
    }

    @After
    fun tearDown() = clean()

    @Test
    fun migrates_legacy_pin_and_enabled_flag() {
        // Seed a legacy install: PIN "1234" hashed at the old 10k iterations, lock enabled, and
        // NO pin_iterations key (as a pre-upgrade install would look).
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2("1234", salt, 10_000)
        openLegacy().edit()
            .putString("pin_salt", Base64.getEncoder().encodeToString(salt))
            .putString("pin_hash", Base64.getEncoder().encodeToString(hash))
            .putBoolean("lock_enabled", true)
            .commit()
        assertTrue(prefsFile("app_lock_prefs").exists())

        val manager = DefaultAppLockManager(context, KeystoreCrypto())

        assertTrue("lock_enabled should survive migration", manager.isLockEnabled.value)
        assertTrue(manager.pinExists())
        assertTrue("legacy PIN must verify via default 10k iterations", manager.verifyPin("1234"))
        assertFalse(manager.verifyPin("9999"))
        assertFalse("legacy file deleted after migration", prefsFile("app_lock_prefs").exists())
    }

    @Test
    fun set_verify_then_lockout_after_five_failures() = runBlocking {
        val manager = DefaultAppLockManager(context, KeystoreCrypto())
        manager.setPin("4321")
        assertTrue(manager.verifyPin("4321"))

        // Five consecutive wrong PINs trip the lockout window.
        repeat(5) { assertFalse(manager.verifyPin("0000")) }

        // Even the correct PIN is rejected while locked out.
        assertFalse("correct PIN must be rejected during lockout", manager.verifyPin("4321"))
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, iterations, 256))
            .encoded

    private fun openLegacy() = EncryptedSharedPreferences.create(
        context,
        "app_lock_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun prefsFile(name: String) =
        File(File(context.applicationInfo.dataDir, "shared_prefs"), "$name.xml")
}
