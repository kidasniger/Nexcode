package com.nexcode.app

import com.nexcode.app.ui.screens.git.ConflictChoice
import com.nexcode.app.ui.screens.git.GitViewModel
import com.nexcode.app.ui.screens.git.GitViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitScreensTest {

    private lateinit var viewModel: GitViewModel

    @Before
    fun setUp() {
        viewModel = GitViewModel()
    }

    @Test
    fun testInitialGitState() {
        val state = viewModel.uiState.value
        assertEquals("nimbus-dev / meteo-android", state.repoFullName)
        assertEquals("main", state.currentBranch)
        assertEquals(GitViewMode.DEFAULT, state.viewMode)
        assertEquals(3, state.modifiedFiles.size)
        assertEquals("feat: écran de détails météo", state.commitMessage)
        assertEquals(2, state.unpushedCommits.size)
        assertEquals(5, state.commits.size)
        assertEquals(2, state.conflicts.size)
    }

    @Test
    fun testUpdateCommitMessageAndToggleError() {
        viewModel.updateCommitMessage("fix: correct responsive padding")
        assertEquals("fix: correct responsive padding", viewModel.uiState.value.commitMessage)

        viewModel.toggleViewMode()
        assertEquals(GitViewMode.ERROR, viewModel.uiState.value.viewMode)

        viewModel.retryPush()
        assertEquals(GitViewMode.DEFAULT, viewModel.uiState.value.viewMode)
        assertNotNull(viewModel.uiState.value.userNotice)
        assertTrue(viewModel.uiState.value.userNotice!!.contains("Push réussi"))
    }

    @Test
    fun testCommitAndPullActions() {
        viewModel.commitAndPush()
        assertNotNull(viewModel.uiState.value.userNotice)
        assertTrue(viewModel.uiState.value.userNotice!!.contains("poussé"))

        var conflictNavigated = false
        viewModel.pullRepository {
            conflictNavigated = true
        }
        assertTrue(conflictNavigated)
    }

    @Test
    fun testCommitHistoryFiltering() {
        assertEquals("main", viewModel.uiState.value.selectedFilterBranch)
        viewModel.selectFilterBranch("all")
        assertEquals("all", viewModel.uiState.value.selectedFilterBranch)

        viewModel.loadMoreCommits()
        assertNotNull(viewModel.uiState.value.userNotice)
        assertTrue(viewModel.uiState.value.userNotice!!.contains("Chargement"))
    }

    @Test
    fun testConflictResolutions() {
        val conflicts = viewModel.uiState.value.conflicts
        assertEquals(2, conflicts.size)
        assertEquals(ConflictChoice.LOCAL, conflicts[0].selectedChoice)
        assertEquals(ConflictChoice.REMOTE, conflicts[1].selectedChoice)

        viewModel.chooseConflictResolution("conf_1", ConflictChoice.REMOTE)
        assertEquals(ConflictChoice.REMOTE, viewModel.uiState.value.conflicts[0].selectedChoice)

        viewModel.chooseConflictResolution("conf_2", ConflictChoice.LOCAL)
        assertEquals(ConflictChoice.LOCAL, viewModel.uiState.value.conflicts[1].selectedChoice)

        var successCallbackCalled = false
        viewModel.applyConflictResolutions {
            successCallbackCalled = true
        }
        assertTrue(successCallbackCalled)
        assertNotNull(viewModel.uiState.value.userNotice)
        assertTrue(viewModel.uiState.value.userNotice!!.contains("résolus"))

        viewModel.dismissNotice()
        assertEquals(null, viewModel.uiState.value.userNotice)
    }
}
