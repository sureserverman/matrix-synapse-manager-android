package com.matrix.synapse.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.core.resources.R
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.core.ui.components.SectionHeader
import com.matrix.synapse.core.ui.components.SynapseCard
import com.matrix.synapse.core.ui.components.SynapseListItem
import com.matrix.synapse.core.ui.components.SynapseScaffold
import com.matrix.synapse.core.ui.components.SynapseToggle
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme
import com.matrix.synapse.core.ui.theme.ThemeMode
import com.matrix.synapse.core.ui.theme.Tokens
import com.matrix.synapse.feature.settings.appearance.AppearanceSettingsViewModel
import com.matrix.synapse.feature.settings.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockSettingsViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
) : ViewModel() {

    val isLockEnabled: StateFlow<Boolean> = appLockManager.isLockEnabled

    private val _showCreatePin = MutableStateFlow(false)
    val showCreatePin: StateFlow<Boolean> = _showCreatePin.asStateFlow()

    private val _showChangePin = MutableStateFlow(false)
    val showChangePin: StateFlow<Boolean> = _showChangePin.asStateFlow()

    fun pinExists(): Boolean = appLockManager.pinExists()

    fun requestEnableLock() {
        viewModelScope.launch {
            if (appLockManager.pinExists()) {
                appLockManager.setEnabled(true)
            } else {
                _showCreatePin.value = true
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { appLockManager.setEnabled(enabled) }
    }

    fun onPinCreated(pin: String) {
        viewModelScope.launch {
            appLockManager.setPin(pin)
            appLockManager.setEnabled(true)
            _showCreatePin.value = false
        }
    }

    fun onCreatePinCancel() {
        _showCreatePin.value = false
    }

    fun verifyPin(pin: String): Boolean = appLockManager.verifyPin(pin)

    fun onPinChanged(newPin: String) {
        viewModelScope.launch {
            appLockManager.setPin(newPin)
            _showChangePin.value = false
        }
    }

    fun onChangePinCancel() {
        _showChangePin.value = false
    }

    fun showChangePinFlow() {
        _showChangePin.value = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsScreen(
    onRearrangeTabs: (() -> Unit)? = null,
    appVersion: String? = null,
    viewModel: AppLockSettingsViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceSettingsViewModel = hiltViewModel(),
) {
    val isEnabled by viewModel.isLockEnabled.collectAsStateWithLifecycle()
    val showCreatePin by viewModel.showCreatePin.collectAsStateWithLifecycle()
    val showChangePin by viewModel.showChangePin.collectAsStateWithLifecycle()
    val pinExists = viewModel.pinExists()
    val themeMode by appearanceViewModel.themeMode.collectAsStateWithLifecycle()

    when {
        showCreatePin -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = SynapseTheme.colors.bg,
            ) {
                CreatePinContent(
                    onPinCreated = viewModel::onPinCreated,
                    onCancel = viewModel::onCreatePinCancel,
                )
            }
        }
        showChangePin -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = SynapseTheme.colors.bg,
            ) {
                ChangePinContent(
                    verifyCurrentPin = viewModel::verifyPin,
                    onPinChanged = viewModel::onPinChanged,
                    onCancel = viewModel::onChangePinCancel,
                )
            }
        }
        else -> SynapseScaffold(
            topBar = { SynapseTopBar(title = stringResource(R.string.settings)) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── APP LOCK ─────────────────────────────────────────
                SectionHeader(stringResource(R.string.app_lock))
                SynapseCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.Space.ScreenEdge),
                    padding = 0.dp,
                ) {
                    Column {
                        SynapseListItem(
                            headline = stringResource(R.string.app_lock),
                            supporting = "Require app PIN when the app is resumed",
                            leading = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = SynapseTheme.colors.textMuted,
                                )
                            },
                            trailing = {
                                SynapseToggle(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) viewModel.requestEnableLock()
                                        else viewModel.setEnabled(false)
                                    },
                                    modifier = Modifier.testTag("app_lock_toggle"),
                                )
                            },
                            showDivider = pinExists,
                        )
                        if (pinExists) {
                            SynapseListItem(
                                headline = stringResource(R.string.change_pin),
                                leading = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = SynapseTheme.colors.textMuted,
                                    )
                                },
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = SynapseTheme.colors.textMuted,
                                    )
                                },
                                onClick = { viewModel.showChangePinFlow() },
                                showDivider = false,
                            )
                        }
                    }
                }

                // ── APPEARANCE ───────────────────────────────────────
                SectionHeader("Appearance")
                SynapseCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.Space.ScreenEdge),
                    padding = 0.dp,
                ) {
                    Column {
                        ThemeModeRow(
                            label = stringResource(R.string.theme_system),
                            supporting = stringResource(R.string.theme_summary_system),
                            selected = themeMode == ThemeMode.System,
                            onSelect = { appearanceViewModel.setThemeMode(ThemeMode.System) },
                        )
                        ThemeModeRow(
                            label = stringResource(R.string.theme_light),
                            selected = themeMode == ThemeMode.Light,
                            onSelect = { appearanceViewModel.setThemeMode(ThemeMode.Light) },
                        )
                        ThemeModeRow(
                            label = stringResource(R.string.theme_dark),
                            selected = themeMode == ThemeMode.Dark,
                            onSelect = { appearanceViewModel.setThemeMode(ThemeMode.Dark) },
                            showDivider = onRearrangeTabs != null,
                        )
                        onRearrangeTabs?.let { onNavigate ->
                            SynapseListItem(
                                headline = stringResource(R.string.rearrange_tabs),
                                supporting = "Reorder the bottom navigation tabs",
                                leading = {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = null,
                                        tint = SynapseTheme.colors.textMuted,
                                    )
                                },
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = SynapseTheme.colors.textMuted,
                                    )
                                },
                                onClick = onNavigate,
                                showDivider = false,
                            )
                        }
                    }
                }

                // ── ABOUT ────────────────────────────────────────────
                if (appVersion != null) {
                    SectionHeader("About")
                    SynapseCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Tokens.Space.ScreenEdge),
                        padding = 0.dp,
                    ) {
                        SynapseListItem(
                            headline = "Version",
                            leading = {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = SynapseTheme.colors.textMuted,
                                )
                            },
                            trailing = {
                                Text(
                                    text = appVersion,
                                    style = SynapseText.Mono,
                                    color = SynapseTheme.colors.textMuted,
                                )
                            },
                            showDivider = false,
                        )
                    }
                }

                Box(modifier = Modifier.size(Tokens.Space.Xxl))
            }
        }
    }
}

@Composable
private fun ThemeModeRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    supporting: String? = null,
    showDivider: Boolean = true,
) {
    SynapseListItem(
        headline = label,
        supporting = supporting,
        trailing = {
            RadioButton(
                selected = selected,
                onClick = onSelect,
            )
        },
        onClick = onSelect,
        showDivider = showDivider,
    )
}
