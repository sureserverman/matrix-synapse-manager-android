package com.matrix.synapse.feature.settings.appearance

import com.matrix.synapse.core.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppearanceSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposes_repository_flow() {
        val repo = FakeThemeModeRepository(ThemeMode.Dark)
        val vm = AppearanceSettingsViewModel(repo)

        assertEquals(ThemeMode.Dark, vm.themeMode.value)
    }

    @Test
    fun setThemeMode_delegates_to_repository_and_updates_flow() = runTest {
        val repo = FakeThemeModeRepository(ThemeMode.System)
        val vm = AppearanceSettingsViewModel(repo)

        vm.setThemeMode(ThemeMode.Light)
        advanceUntilIdle()

        assertEquals(ThemeMode.Light, repo.themeMode.value)
        assertEquals(ThemeMode.Light, vm.themeMode.value)
    }

    private class FakeThemeModeRepository(initial: ThemeMode) : ThemeModeRepository {
        private val _themeMode = MutableStateFlow(initial)
        override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
        override suspend fun setThemeMode(mode: ThemeMode) {
            _themeMode.value = mode
        }
    }
}
