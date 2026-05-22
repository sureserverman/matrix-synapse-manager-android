package com.matrix.synapse.manager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.feature.settings.appearance.ThemeModeRepository
import com.matrix.synapse.feature.settings.security.AppLockManager
import com.matrix.synapse.feature.settings.ui.PinEntryContent
import com.matrix.synapse.manager.ui.theme.MatrixSynapseManagerTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.manager.tabs.TabOrderRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var tabOrderRepository: TabOrderRepository

    @Inject
    lateinit var themeModeRepository: ThemeModeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeModeRepository.themeMode.collectAsStateWithLifecycle()
            MatrixSynapseManagerTheme(themeMode = themeMode) {
                val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()
                if (isLocked) {
                    LockScreen(
                        verifyPin = { appLockManager.verifyPin(it) },
                        onUnlock = { appLockManager.unlock() },
                    )
                } else {
                    AppNavHost(
                        modifier = Modifier.fillMaxSize(),
                        tabOrderRepository = tabOrderRepository,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appLockManager.lock()
    }
}

@Composable
private fun LockScreen(
    verifyPin: (String) -> Boolean,
    onUnlock: () -> Unit,
) {
    var wrongPin by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        PinEntryContent(
            title = stringResource(R.string.enter_pin_to_unlock),
            wrongPin = wrongPin,
            onComplete = { pin ->
                if (verifyPin(pin)) {
                    onUnlock()
                } else {
                    wrongPin = true
                }
            },
        )
    }
}
