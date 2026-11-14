package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.github.repository.GitHubResult
import com.nexcode.app.data.preferences.NexcodeSecureVault
import kotlinx.coroutines.runBlocking
import com.nexcode.app.ui.screens.settings.SettingsViewModel
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsAccountTest {

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
    fun testAccountSyncAndRealSignOut() = runBlocking {
        // Starts disconnected
        viewModel.refreshAccountAndStorageSync()
        assertTrue(viewModel.uiState.value.isSignedOut)
        assertEquals("Non connecté", viewModel.uiState.value.accountUsername)
        assertEquals("?", viewModel.uiState.value.accountInitials)

        // Verify real session
        val res = gitService.verifyAndSaveToken("ghp_settings_token")
        assertTrue(res is GitHubResult.Success)
        viewModel.refreshAccountAndStorageSync()

        var state = viewModel.uiState.value
        assertFalse(state.isSignedOut)
        assertEquals("octo-dev", state.accountUsername)
        assertEquals("OD", state.accountInitials)
        assertEquals("github.com/octo-dev · Plan Pro", state.accountPlan)

        // Confirm sign-out wipes token and resets state
        viewModel.setSignOutDialogVisible(true)
        assertTrue(viewModel.uiState.value.showSignOutDialog)

        viewModel.confirmSignOutSync()
        state = viewModel.uiState.value
        assertFalse(state.showSignOutDialog)
        assertTrue(state.isSignedOut)
        assertEquals("Non connecté", state.accountUsername)
        assertNull(NexcodeSecureVault(context).getGitToken())
    }
}
