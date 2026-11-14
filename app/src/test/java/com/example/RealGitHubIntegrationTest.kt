package com.example

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.git.GitOperationResult
import com.nexcode.app.data.git.JGitManager
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.github.repository.GitHubResult
import com.nexcode.app.data.preferences.NexcodeSecureVault
import com.nexcode.app.ui.screens.build.BuildVariant
import com.nexcode.app.ui.screens.build.BuildViewModel
import com.nexcode.app.ui.screens.git.ConflictChoice
import com.nexcode.app.ui.screens.git.GitViewModel
import com.nexcode.app.ui.screens.git.GitViewMode
import com.nexcode.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealGitHubIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: android.content.Context
    private lateinit var vault: NexcodeSecureVault
    private lateinit var gitHubService: GitHubRepositoryService
    private lateinit var jgitManager: JGitManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        vault = NexcodeSecureVault(context)
        gitHubService = GitHubRepositoryService(context)
        jgitManager = JGitManager()
    }

    @Test
    fun testTokenSavingAndRetrievalInVault() {
        val testToken = "ghp_TestToken123456789ABC"
        vault.saveGitToken(testToken)
        assertEquals(testToken, vault.getGitToken())

        vault.clearGitToken()
        assertTrue(vault.getGitToken().isNullOrBlank())
    }

    @Test
    fun testOnboardingGitHubConnectionWithToken() = runBlocking {
        val onboardingViewModel = OnboardingViewModel(context)
        onboardingViewModel.updateGitHubToken("ghp_ValidTokenForTest")
        assertEquals("ghp_ValidTokenForTest", onboardingViewModel.uiState.value.gitHubToken)

        // Déconnexion
        onboardingViewModel.disconnectGitHub()
        assertFalse(onboardingViewModel.uiState.value.isGitHubConnected)
        assertEquals("", onboardingViewModel.uiState.value.gitHubToken)
    }

    @Test
    fun testJGitLocalRepoInitCommitAndHistory() = runBlocking {
        val repoDir = tempFolder.newFolder("local_git_test")
        org.eclipse.jgit.api.Git.init().setDirectory(repoDir).call().use { git ->
            // Création d'un fichier réel
            val testFile = File(repoDir, "MainActivity.kt")
            testFile.writeText("package com.test\nclass MainActivity")

            // Commit via JGitManager
            val commitRes = jgitManager.commitAndPush(
                projectDir = repoDir,
                commitMessage = "Initial commit by NEXCODE",
                authorName = "Nexcode Dev",
                authorEmail = "dev@nexcode.local",
                token = null
            )
            // Dans un dépôt sans remote configuré, le push échoue ou le commit est local
            // Vérifions l'historique
            val historyRes = jgitManager.getCommitHistory(repoDir)
            assertTrue(historyRes is GitOperationResult.Success)
            val commits = (historyRes as GitOperationResult.Success).data
            assertEquals(1, commits.size)
            assertEquals("Initial commit by NEXCODE", commits[0].message)
            assertEquals("Nexcode Dev", commits[0].author)
        }
    }

    @Test
    fun testGitViewModelOperations() = runBlocking {
        val gitViewModel = GitViewModel(context)
        assertEquals(GitViewMode.DEFAULT, gitViewModel.uiState.value.viewMode)

        // Bascule vers l'état d'erreur réseau
        gitViewModel.setViewMode(GitViewMode.ERROR)
        assertEquals(GitViewMode.ERROR, gitViewModel.uiState.value.viewMode)

        // Résolution d'un conflit
        gitViewModel.chooseConflictResolution("conf_1", ConflictChoice.REMOTE)
        val conf1 = gitViewModel.uiState.value.conflicts.find { it.id == "conf_1" }
        assertNotNull(conf1)
        assertEquals(ConflictChoice.REMOTE, conf1!!.selectedChoice)
    }

    @Test
    fun testBuildViewModelStateAndWorkflowTrigger() = runBlocking {
        // Le ViewModel réel démarre sur un état honnête et vide (aucun projet lié).
        val buildViewModel = BuildViewModel(context)
        assertEquals(BuildVariant.DEBUG, buildViewModel.uiState.value.selectedVariant)
        assertEquals("", buildViewModel.uiState.value.versionName)
        assertEquals(
            com.nexcode.app.ui.screens.build.CompilationStatus.IDLE,
            buildViewModel.uiState.value.compilationStatus
        )
        assertFalse(buildViewModel.uiState.value.canStartBuild)

        // Changement de variante
        buildViewModel.setVariant(BuildVariant.RELEASE)
        assertEquals(BuildVariant.RELEASE, buildViewModel.uiState.value.selectedVariant)

        // Sans projet GitHub lié, le lancement est refusé honnêtement (pas de faux démarrage).
        var started = false
        buildViewModel.startCompilation { started = true }
        assertFalse(started)
        assertTrue(buildViewModel.uiState.value.configNotice != null)
    }
}
