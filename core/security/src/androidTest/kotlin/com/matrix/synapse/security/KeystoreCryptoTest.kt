package com.matrix.synapse.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [KeystoreCrypto] — exercises the real Android Keystore, so this must
 * run on a device/emulator (the CI `instrumented-tests` job covers it).
 */
@RunWith(AndroidJUnit4::class)
class KeystoreCryptoTest {

    private val crypto = KeystoreCrypto()

    @Test
    fun round_trips_plaintext() {
        val secret = "syt_admin_access_token_abc123"
        assertEquals(secret, crypto.decrypt(crypto.encrypt(secret)))
    }

    @Test
    fun round_trips_empty_and_unicode() {
        assertEquals("", crypto.decrypt(crypto.encrypt("")))
        val unicode = "tøken-🔐-日本語"
        assertEquals(unicode, crypto.decrypt(crypto.encrypt(unicode)))
    }

    @Test
    fun same_plaintext_yields_different_ciphertext() {
        // Random per-call IV means ciphertexts must differ even for identical input.
        assertNotEquals(crypto.encrypt("same"), crypto.encrypt("same"))
    }

    @Test
    fun tampered_ciphertext_returns_null() {
        val encrypted = crypto.encrypt("sensitive")
        val tampered = encrypted.dropLast(4) + if (encrypted.endsWith("A")) "B==" else "A=="
        assertNull(crypto.decrypt(tampered))
    }

    @Test
    fun garbage_input_returns_null() {
        assertNull(crypto.decrypt("not-base64-!!!"))
        assertNull(crypto.decrypt(""))
    }

    @Test
    fun key_persists_across_instances() {
        // A new instance must resolve the same Keystore key and decrypt prior ciphertext.
        val encrypted = crypto.encrypt("persisted")
        assertEquals("persisted", KeystoreCrypto().decrypt(encrypted))
    }
}
