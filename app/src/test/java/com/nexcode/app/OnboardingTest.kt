package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import com.nexcode.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = OnboardingViewModel(context)
    }

    @Test
    fun testInitialOnboardingState() {
        val state = viewModel.uiState.value
        assertEquals("ghp_9f2Xk7QzLm4Np8Rt", state.gitHubToken)
        assertFalse(state.isTokenVisible)
        assertTrue(state.isGitHubConnected)
        assertEquals("nimbus-dev", state.gitHubUsername)
        assertFalse(state.isGitHubError)
        assertEquals("Anthropic", state.selectedProvider)
        assertEquals("sk-ant-api03-x9L2mQ7vNp4Tt8Rw", state.apiKey)
        assertEquals("claude-sonnet-4.5", state.selectedModel)
    }

    @Test
    fun testGitHubTokenAndErrorToggle() {
        viewModel.updateGitHubToken("ghp_custom123")
        assertEquals("ghp_custom123", viewModel.uiState.value.gitHubToken)

        viewModel.toggleTokenVisibility()
        assertTrue(viewModel.uiState.value.isTokenVisible)

        // Toggle error state (conforme maquette s03 stv data-v="err")
        viewModel.toggleGitHubErrorState()
        assertTrue(viewModel.uiState.value.isGitHubError)

        viewModel.retryGitHubConnection()
        assertFalse(viewModel.uiState.value.isGitHubError)
        assertTrue(viewModel.uiState.value.isGitHubConnected)
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
    }
}
