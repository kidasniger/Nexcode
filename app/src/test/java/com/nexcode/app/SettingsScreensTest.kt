package com.nexcode.app

import com.nexcode.app.ui.screens.settings.CodeThemePreference
import com.nexcode.app.ui.screens.settings.IndentationPreference
import com.nexcode.app.ui.screens.settings.SettingsViewModel
import com.nexcode.app.ui.screens.settings.UiThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsScreensTest {

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        viewModel = SettingsViewModel()
    }

    @Test
    fun testInitialSettingsState() {
        val state = viewModel.uiState.value
        assertEquals("nimbus-dev", state.accountUsername)
        assertEquals("github.com/nimbus-dev · Plan gratuit", state.accountPlan)
        assertFalse(state.showSignOutDialog)
        assertFalse(state.isSignedOut)

        assertEquals("Français (France)", state.language)
        assertEquals("claude-sonnet-4.5 · Anthropic", state.aiModelSummary)

        assertEquals("1,8 Go utilisés", state.storageUsedText)
        assertEquals("sur 4 Go", state.storageTotalText)
        assertEquals("Caches Gradle · 480 Mo", state.storageGradleText)
        assertFalse(state.isCacheCleared)

        assertTrue(state.notifyBuildCompleted)
        assertTrue(state.notifyBuildErrors)
        assertFalse(state.notifyDependencyUpdates)

        assertEquals(UiThemePreference.DARK, state.uiTheme)
        assertEquals(14, state.editorFontSizeSp)
        assertEquals(CodeThemePreference.NEX_DARK, state.codeTheme)
        assertEquals(IndentationPreference.SPACES_4, state.indentation)
        assertTrue(state.wordWrap)
        assertTrue(state.lineNumbers)
        assertTrue(state.autoCloseBrackets)

        assertEquals("1.4.2 (build 42)", state.appVersion)
        assertEquals("À jour · vérifié le 10 nov.", state.updateStatus)
        assertTrue(state.autoBackupEnabled)
        assertNull(state.userNotice)
    }

    @Test
    fun testSignOutDialogAndConfirmation() {
        viewModel.setSignOutDialogVisible(true)
        assertTrue(viewModel.uiState.value.showSignOutDialog)

        viewModel.confirmSignOut()
        val state = viewModel.uiState.value
        assertFalse(state.showSignOutDialog)
        assertTrue(state.isSignedOut)
        assertEquals("Non connecté", state.accountUsername)
        assertNotNull(state.userNotice)
    }

    @Test
    fun testClearCaches() {
        viewModel.clearCaches()
        val state = viewModel.uiState.value
        assertTrue(state.isCacheCleared)
        assertEquals("Caches Gradle · 0 Mo", state.storageGradleText)
        assertEquals("1,3 Go utilisés", state.storageUsedText)
        assertNotNull(state.userNotice)
    }

    @Test
    fun testNotificationToggles() {
        viewModel.toggleNotifyBuildCompleted(false)
        viewModel.toggleNotifyBuildErrors(false)
        viewModel.toggleNotifyDependencyUpdates(true)

        val state = viewModel.uiState.value
        assertFalse(state.notifyBuildCompleted)
        assertFalse(state.notifyBuildErrors)
        assertTrue(state.notifyDependencyUpdates)
    }

    @Test
    fun testAppearanceAndEditorSettings() {
        viewModel.setUiTheme(UiThemePreference.LIGHT)
        assertEquals(UiThemePreference.LIGHT, viewModel.uiState.value.uiTheme)

        viewModel.increaseFontSize()
        assertEquals(15, viewModel.uiState.value.editorFontSizeSp)

        viewModel.decreaseFontSize()
        viewModel.decreaseFontSize()
        assertEquals(13, viewModel.uiState.value.editorFontSizeSp)

        viewModel.setCodeTheme(CodeThemePreference.SOLARIZED)
        assertEquals(CodeThemePreference.SOLARIZED, viewModel.uiState.value.codeTheme)

        viewModel.setIndentation(IndentationPreference.SPACES_2)
        assertEquals(IndentationPreference.SPACES_2, viewModel.uiState.value.indentation)

        viewModel.toggleWordWrap(false)
        viewModel.toggleLineNumbers(false)
        viewModel.toggleAutoCloseBrackets(false)

        val state = viewModel.uiState.value
        assertFalse(state.wordWrap)
        assertFalse(state.lineNumbers)
        assertFalse(state.autoCloseBrackets)
    }

    @Test
    fun testBackupAndExportFlow() {
        viewModel.toggleAutoBackup(false)
        assertFalse(viewModel.uiState.value.autoBackupEnabled)

        viewModel.exportProjectsNow()
        assertNotNull(viewModel.uiState.value.userNotice)

        viewModel.dismissNotice()
        assertNull(viewModel.uiState.value.userNotice)
    }
}
