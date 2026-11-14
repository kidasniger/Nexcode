package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.github.repository.GitHubResult
import com.nexcode.app.data.preferences.NexcodeSecureVault
import com.nexcode.app.ui.screens.settings.CodeThemePreference
import com.nexcode.app.ui.screens.settings.IndentationPreference
import com.nexcode.app.ui.screens.settings.SettingsViewModel
import com.nexcode.app.ui.screens.settings.UiThemePreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreensTest {

    private lateinit var context: Context
    private lateinit var fakeApi: FakeGitHubApiService
    private lateinit var gitService: GitHubRepositoryService
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeApi = FakeGitHubApiService()
        gitService = GitHubRepositoryService(context, api = fakeApi)
        // PAS de clearGitSession ici (écriture DataStore figeant le JVM de test,
        // voir OnboardingTest) : le vault par test garantit l'état non connecté.
        viewModel = SettingsViewModel(context, gitService)
    }

    @Test
    fun testRealStorageMeasurementAndClearCaches() = runBlocking {
        val dummyCache = File(context.cacheDir, "gradle_dummy_cache.bin")
        dummyCache.writeBytes(ByteArray(4096))

        viewModel.refreshAccountAndStorageSync()
        assertTrue(dummyCache.exists())
        assertTrue(viewModel.uiState.value.storageGradleText.contains("Ko"))

        viewModel.clearCachesSync()
        assertFalse(dummyCache.exists())
        assertTrue(viewModel.uiState.value.isCacheCleared)
        assertEquals("Caches · 0 Mo", viewModel.uiState.value.storageGradleText)
        assertNotNull(viewModel.uiState.value.userNotice)
    }

    @Test
    fun testNotificationAndAppearanceSettings() {
        viewModel.toggleNotifyBuildCompleted(false)
        viewModel.toggleNotifyBuildErrors(false)
        viewModel.toggleNotifyDependencyUpdates(true)

        var state = viewModel.uiState.value
        assertFalse(state.notifyBuildCompleted)
        assertFalse(state.notifyBuildErrors)
        assertTrue(state.notifyDependencyUpdates)

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

        state = viewModel.uiState.value
        assertFalse(state.wordWrap)
        assertFalse(state.lineNumbers)
        assertFalse(state.autoCloseBrackets)
    }
}
