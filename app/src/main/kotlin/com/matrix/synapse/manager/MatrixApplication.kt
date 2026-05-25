package com.matrix.synapse.manager

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.matrix.synapse.feature.settings.security.AppLockManager
import com.matrix.synapse.security.SecureTokenStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MatrixApplication : Application() {

    @Inject
    lateinit var tokenStore: SecureTokenStore

    @Inject
    lateinit var appLockManager: AppLockManager

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Use a single shared ImageLoader so memory + disk cache are shared across the app
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .crossfade(true)
                .build()
        }

        // Run the one-time secure-store migrations off the main thread at startup, so an existing
        // user's tokens and PIN are re-encrypted under the Android Keystore before first use,
        // rather than lazily on first access (which for tokens could land on the UI thread).
        startupScope.launch {
            tokenStore.warmUp()
            appLockManager.pinExists() // forces app-lock store init + its one-time migration
        }
    }
}
