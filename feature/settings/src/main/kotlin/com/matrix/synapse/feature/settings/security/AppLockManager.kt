package com.matrix.synapse.feature.settings.security

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the optional app PIN lock (separate from device PIN).
 *
 * - [isLockEnabled] persists: when true, app shows PIN screen on resume/cold start.
 * - [isLocked] is in-memory: app starts locked when enabled; [unlock] clears it after correct PIN.
 * - PIN is stored as salt + PBKDF2 hash, encrypted at rest via the Android Keystore (never plaintext).
 */
interface AppLockManager {
    val isLockEnabled: StateFlow<Boolean>
    val isLocked: StateFlow<Boolean>

    /** True if a PIN has been set (even when lock is currently disabled). */
    fun pinExists(): Boolean

    /** Set or change the app PIN (hashes and stores; does not enable lock). */
    suspend fun setPin(pin: String)

    /** Verify the given PIN; returns true if it matches the stored PIN. */
    fun verifyPin(pin: String): Boolean

    /** Remove the stored PIN. Call when user explicitly disables lock or removes PIN. */
    suspend fun clearPin()

    suspend fun setEnabled(enabled: Boolean)
    fun lock()
    fun unlock()
}
