package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.github.repository.GitHubResult
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import com.nexcode.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingTest {

    private lateinit var context: Context
    private lateinit var fakeApi: FakeGitHubApiService
    private lateinit var gitService: GitHubRepositoryService
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeApi = FakeGitHubApiService()
        gitService = GitHubRepositoryService(context, api = fakeApi)
        runBlocking { gitService.clearGitSession() }
        viewModel = OnboardingViewModel(context, gitService)
    }

    @Test
    fun testInitialOnboardingStateIsCleanWithoutFakeTokens() {
        val state = viewModel.uiState.value
        assertEquals("", state.gitHubToken)
        assertFalse(state.isTokenVisible)
        assertFalse(state.isGitHubConnected)
        assertEquals("", state.gitHubUsername)
        assertFalse(state.isGitHubError)
        assertEquals("Anthropic", state.selectedProvider)
        assertEquals("", state.apiKey)
        assertEquals("claude-sonnet-4.5", state.selectedModel)
    }

    @Test
    fun testVerifyAndSaveRealTokenSuccessAndDisconnect() = runBlocking {
        val res = gitService.verifyAndSaveToken("ghp_realTestToken123")
        assertTrue(res is GitHubResult.Success)
        val session = (res as GitHubResult.Success).data
        assertEquals("octo-dev", session.user.login)
        assertEquals(1, session.repoCount)
        assertEquals("Plan Pro", session.planName)

        val vault = NexcodeSecureVault(context)
        assertEquals("ghp_realTestToken123", vault.getGitToken())

        val prefs = NexcodePreferences(context)
        assertEquals("octo-dev", prefs.gitLoginFlow.first())
        assertEquals("Plan Pro", prefs.gitPlanFlow.first())

        viewModel.refreshSessionFromStorage()
        kotlinx.coroutines.delay(50)
        assertTrue(viewModel.uiState.value.isGitHubConnected)
        assertEquals("octo-dev", viewModel.uiState.value.gitHubUsername)

        viewModel.disconnectGitHub()
        kotlinx.coroutines.delay(50)
        assertFalse(viewModel.uiState.value.isGitHubConnected)
        assertNull(vault.getGitToken())
    }

    @Test
    fun testInvalidTokenReturns401AndNetworkErrorHandled() = runBlocking {
        fakeApi.authFailureCode = 401
        val err = gitService.verifyAndSaveToken("ghp_bad")
        assertTrue(err is GitHubResult.Error)
        assertEquals(401, (err as GitHubResult.Error).code)

        fakeApi.authFailureCode = null
        fakeApi.throwNetworkError = true
        val netErr = gitService.verifyAndSaveToken("ghp_offline")
        assertTrue(netErr is GitHubResult.NetworkError)
    }

    @Test
    fun testAiProviderAndModelSelection() = runBlocking {
        viewModel.selectAiProvider("OpenAI")
        assertEquals("OpenAI", viewModel.uiState.value.selectedProvider)

        viewModel.selectModelSync("gpt-4o")
        assertEquals("gpt-4o", viewModel.uiState.value.selectedModel)

        viewModel.updateApiKey("sk-proj-test12345")
        assertEquals("sk-proj-test12345", viewModel.uiState.value.apiKey)

        viewModel.completeOnboardingSync()

        val vault = NexcodeSecureVault(context)
        assertEquals("sk-proj-test12345", vault.getApiKey())

        val prefs = NexcodePreferences(context)
        assertTrue(prefs.onboardingDoneFlow.first())
        assertEquals("gpt-4o", prefs.aiModelFlow.first())
        assertNotNull(viewModel.uiState.value)
    }
}
