package com.matrix.synapse.feature.settings.appearance

import android.content.SharedPreferences
import com.matrix.synapse.core.ui.theme.ThemeMode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultThemeModeRepositoryTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        prefs = mockk()
        editor = mockk()
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just runs
    }

    @Test
    fun defaults_to_System_on_fresh_install() = runTest {
        every { prefs.getString(any(), any()) } returns null

        val repo = DefaultThemeModeRepository(prefs)

        assertEquals(ThemeMode.System, repo.themeMode.value)
    }

    @Test
    fun returns_System_when_stored_value_is_unparseable() = runTest {
        every { prefs.getString(any(), any()) } returns "Eclipse"

        val repo = DefaultThemeModeRepository(prefs)

        assertEquals(ThemeMode.System, repo.themeMode.value)
    }

    @Test
    fun loads_previously_saved_mode() = runTest {
        every { prefs.getString(any(), any()) } returns "Dark"

        val repo = DefaultThemeModeRepository(prefs)

        assertEquals(ThemeMode.Dark, repo.themeMode.value)
    }

    @Test
    fun setThemeMode_writes_to_prefs_and_updates_flow() = runTest {
        every { prefs.getString(any(), any()) } returns null
        val repo = DefaultThemeModeRepository(prefs)

        repo.setThemeMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, repo.themeMode.value)

        repo.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repo.themeMode.value)

        verify { editor.putString(any(), "Light") }
        verify { editor.putString(any(), "Dark") }
    }
}
