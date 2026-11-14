package com.nexcode.app

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.github.repository.GitHubResult
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import com.nexcode.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
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
        // PAS de clearGitSession ici : une écriture DataStore dans un @Before
        // figeait le JVM de test de façon non déterministe (le DataStore est un
        // singleton process partagé entre les applications Robolectric successives).
        // Le coffre NexcodeSecureVault est lui isolé par test (fichiers par app),
        // donc l'état initial (non connecté) reste garanti sans écriture DataStore.
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

        viewModel.refreshSessionFromStorageSync()
        assertTrue(viewModel.uiState.value.isGitHubConnected)
        assertEquals("octo-dev", viewModel.uiState.value.gitHubUsername)

        // disconnectGitHubSync (et non disconnectGitHub) : l'écriture DataStore de
        // clearGitSession se termine AVANT la fin du test. L'ancienne version
        // fire-and-forget laissait l'écriture en vol à la destruction de
        // l'environnement Robolectric (le test ne faisait que vérifier le flag UI,
        // posé avant la fin de l'écriture), tuant l'actor DataStore du JVM —
        // c'est ce qui figeait testAiProviderAndModelSelection de façon
        // non déterministe (24 min de gel, 2 échecs CI consécutifs).
        withTimeout(20_000) { viewModel.disconnectGitHubSync() }
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

        // Toutes les interactions DataStore sont bornées : si l'actor DataStore du JVM
        // de test se fige (blocage non déterministe connu, cf. setUp), le test échoue
        // en ~20 s en nommant l'étape coupable au lieu de geler la tâche Gradle 24 min.
        withTimeout(20_000) { viewModel.selectModelSync("gpt-4o") }
        assertEquals("gpt-4o", viewModel.uiState.value.selectedModel)

        viewModel.updateApiKey("sk-proj-test12345")
        assertEquals("sk-proj-test12345", viewModel.uiState.value.apiKey)

        withTimeout(20_000) { viewModel.completeOnboardingSync() }

        val vault = NexcodeSecureVault(context)
        assertEquals("sk-proj-test12345", vault.getApiKey())

        val prefs = NexcodePreferences(context)
        assertTrue(withTimeout(20_000) { prefs.onboardingDoneFlow.first() })
        assertEquals("gpt-4o", withTimeout(20_000) { prefs.aiModelFlow.first() })
        assertNotNull(viewModel.uiState.value)
    }

    private suspend fun awaitAiTestDone(timeoutMs: Long = 15_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (!viewModel.uiState.value.isTestingAi || System.currentTimeMillis() >= deadline) {
                break
            }
            delay(25)
        }
    }

    private suspend fun awaitCondition(timeoutMs: Long = 15_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (predicate() || System.currentTimeMillis() >= deadline) {
                break
            }
            delay(25)
        }
    }

    @Test
    fun testAiConnectionWithoutKeyReturnsRealValidationError(): Unit = runBlocking {
        viewModel.selectAiProvider("Anthropic")
        viewModel.updateApiKey("")

        viewModel.testAiConnection()
        awaitAiTestDone()

        val state = viewModel.uiState.value
        assertFalse(state.isTestingAi)
        assertTrue(state.aiTestSuccess == false)
        assertNotNull(state.aiTestMessage)
        // Message réel renvoyé par le service IA (validation locale du endpoint)
        assertTrue(state.aiTestMessage!!.contains("clé API"))
    }

    @Test
    fun testAiConnectionWithInvalidCustomUrlReturnsRealError(): Unit = runBlocking {
        viewModel.selectAiProvider("Serveur personnalisé")
        viewModel.updateCustomBaseUrl("serveur-invalide-sans-protocole")

        viewModel.testAiConnection()
        awaitAiTestDone()

        val state = viewModel.uiState.value
        assertFalse(state.isTestingAi)
        assertTrue(state.aiTestSuccess == false)
        assertNotNull(state.aiTestMessage)
        assertTrue(state.aiTestMessage!!.contains("URL du serveur personnalisé"))
    }
}
