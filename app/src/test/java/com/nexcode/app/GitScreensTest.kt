package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.ui.screens.git.ConflictChoice
import com.nexcode.app.ui.screens.git.GitViewModel
import com.nexcode.app.ui.screens.git.GitViewMode
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GitScreensTest {

    private lateinit var context: Context
    private lateinit var tempRoot: File
    private lateinit var bareRepoDir: File
    private lateinit var localRepoDir: File
    private lateinit var peerRepoDir: File
    private lateinit var gitService: GitHubRepositoryService
    private lateinit var viewModel: GitViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tempRoot = Files.createTempDirectory("nexcode_git_test").toFile()
        bareRepoDir = File(tempRoot, "origin.git")
        localRepoDir = File(tempRoot, "local")
        peerRepoDir = File(tempRoot, "peer")

        Git.init().setBare(true).setInitialBranch("main").setDirectory(bareRepoDir).call().close()

        // Seed initial commit via peer
        Git.cloneRepository().setURI(bareRepoDir.toURI().toString()).setDirectory(peerRepoDir).call().use { git ->
            File(peerRepoDir, "MainActivity.kt").writeText("line1\nval title = \"v1\"\nline3\n")
            git.add().addFilepattern(".").call()
            git.commit().setMessage("chore: initial commit").setAuthor("Seed", "seed@test.com").call()
            git.push().call()
        }

        // Clone into localRepoDir
        Git.cloneRepository().setURI(bareRepoDir.toURI().toString()).setDirectory(localRepoDir).call().close()

        gitService = GitHubRepositoryService(context, api = FakeGitHubApiService())
        viewModel = GitViewModel(context, gitService)
    }

    @After
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun testRealGitStatusDiffCommitAndPush() = runBlocking {
        viewModel.bindProjectAndRefreshSync("meteo-android", "octo-dev/meteo-android", localRepoDir.absolutePath)
        var state = viewModel.uiState.value
        assertEquals("octo-dev / meteo-android", state.repoFullName)
        assertEquals("main", state.currentBranch)
        assertTrue(state.hasGitRepo)
        assertEquals(1, state.commits.size)
        assertTrue(state.modifiedFiles.isEmpty())

        // Modify a file locally -> check diff stats
        File(localRepoDir, "MainActivity.kt").writeText("line1\nval title = \"v2\"\nval extra = 1\nline3\n")
        viewModel.refreshGitStatusSync()
        state = viewModel.uiState.value
        assertEquals(1, state.modifiedFiles.size)
        assertEquals("MainActivity.kt", state.modifiedFiles.first().name)
        assertTrue(state.modifiedFiles.first().added > 0)

        // Commit and push to bare repo
        viewModel.updateCommitMessage("feat: update title")
        viewModel.commitAndPushSync()
        state = viewModel.uiState.value
        assertEquals(GitViewMode.DEFAULT, state.viewMode)
        assertTrue(state.modifiedFiles.isEmpty())
        assertEquals(2, state.commits.size)
        assertTrue(state.commits.first().message.contains("feat: update title"))
        assertNotNull(state.userNotice)
    }

    @Test
    fun testRealMergeConflictDetectionResolutionAndAbort() = runBlocking {
        // 1. Peer modifies and pushes MainActivity.kt
        Git.open(peerRepoDir).use { peer ->
            File(peerRepoDir, "MainActivity.kt").writeText("line1\nval title = \"remote\"\nline3\n")
            peer.add().addFilepattern(".").call()
            peer.commit().setMessage("remote edit").setAuthor("Peer", "p@t.com").call()
            peer.push().call()
        }

        // 2. Local commits a conflicting edit without pushing
        Git.open(localRepoDir).use { local ->
            File(localRepoDir, "MainActivity.kt").writeText("line1\nval title = \"local\"\nline3\n")
            local.add().addFilepattern(".").call()
            local.commit().setMessage("local edit").setAuthor("Local", "l@t.com").call()
        }

        viewModel.bindProjectAndRefreshSync("meteo-android", "octo-dev/meteo-android", localRepoDir.absolutePath)

        // 3. Pull triggers real merge conflict!
        var conflictFound = false
        viewModel.pullRepositorySync { conflictFound = true }
        assertTrue(conflictFound)

        val conflicts = viewModel.uiState.value.conflicts
        assertEquals(1, conflicts.size)
        assertTrue(conflicts.first().localLines.any { it.contains("local") })
        assertTrue(conflicts.first().remoteLines.any { it.contains("remote") })

        // 4. Test cancel merge abort
        viewModel.cancelMergeSync()
        assertTrue(viewModel.uiState.value.conflicts.isEmpty())

        // 5. Re-trigger conflict, choose REMOTE, and resolve cleanly
        viewModel.pullRepositorySync {}
        assertEquals(1, viewModel.uiState.value.conflicts.size)
        val cid = viewModel.uiState.value.conflicts.first().id
        viewModel.chooseConflictResolution(cid, ConflictChoice.REMOTE)
        assertTrue(viewModel.applyConflictResolutionsSync())

        val resolvedText = File(localRepoDir, "MainActivity.kt").readText()
        assertTrue(resolvedText.contains("val title = \"remote\""))
        assertFalse(resolvedText.contains("<<<<<<<"))

        viewModel.dismissNotice()
        assertNull(viewModel.uiState.value.userNotice)
    }
}
