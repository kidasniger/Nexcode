package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.ui.screens.chat.ChatViewModel
import com.nexcode.app.ui.screens.editor.EditorViewModel
import com.nexcode.app.ui.screens.projects.ProjectBuildStatus
import com.nexcode.app.ui.screens.projects.ProjectItem
import com.nexcode.app.ui.screens.projects.ProjectsViewModel
import com.nexcode.app.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModalScreensTest {



    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testModalDeleteProjectStateInProjectsViewModel() = runBlocking {
        val viewModel = ProjectsViewModel(context, GitHubRepositoryService(context, api = FakeGitHubApiService()))
        viewModel.updateNewAppName("ModalTestApp")
        viewModel.createProjectSync()
        delay(80)

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        val project = viewModel.uiState.value.projects.first()
        viewModel.openDeleteDialog(project)
        assertTrue(viewModel.uiState.value.showDeleteDialog)
        assertNotNull(viewModel.uiState.value.projectToDelete)

        viewModel.dismissDeleteDialog()
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun testModalModelSelectionStateInChatViewModel() {
        val viewModel = ChatViewModel(context)
        assertEquals("claude-sonnet-4.5", viewModel.uiState.value.modelName)
        assertFalse(viewModel.uiState.value.showModelSelector)

        viewModel.openModelSelector()
        assertTrue(viewModel.uiState.value.showModelSelector)

        viewModel.selectModel("claude-opus-4.1")
        assertEquals("claude-opus-4.1", viewModel.uiState.value.modelName)
        assertFalse(viewModel.uiState.value.showModelSelector)

        viewModel.openModelSelector()
        viewModel.closeModelSelector()
        assertFalse(viewModel.uiState.value.showModelSelector)
    }

    @Test
    fun testModalFileMenuStateInEditorViewModel() = runBlocking {
        val viewModel = EditorViewModel(context)
        assertFalse(viewModel.uiState.value.showFileContextMenu)
        assertNull(viewModel.uiState.value.selectedFileForMenu)

        // Un vrai projet avec un vrai fichier : l'arborescence doit être réelle
        val projectDir = java.nio.file.Files.createTempDirectory("nexcode_modal_test").toFile()
        val sourceFile = java.io.File(projectDir, "app/src/main/java/com/demo/MainActivity.kt")
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText("package com.demo\n\nfun greeting(): String {\n    return \"Bonjour\"\n}\n")

        viewModel.bindProjectSync(
            ProjectItem(
                id = 1L,
                name = "Démo",
                packageName = "com.demo",
                initials = "D",
                language = "Kotlin",
                uiFramework = "Compose",
                modifiedText = "",
                buildStatus = ProjectBuildStatus.DRAFT,
                buildStatusLabel = "",
                localPath = projectDir.absolutePath
            )
        )

        val fileItems = viewModel.uiState.value.treeItems.filter { !it.isDirectory }
        assertTrue(fileItems.isNotEmpty())
        val firstItem = fileItems.first()
        viewModel.showContextMenu(firstItem)
        assertTrue(viewModel.uiState.value.showFileContextMenu)
        assertEquals(firstItem, viewModel.uiState.value.selectedFileForMenu)

        viewModel.hideContextMenu()
        assertFalse(viewModel.uiState.value.showFileContextMenu)
        assertNull(viewModel.uiState.value.selectedFileForMenu)
        Unit
    }

    @Test
    fun testModalSignOutStateInSettingsViewModel() = runBlocking {
        val viewModel = SettingsViewModel(context, GitHubRepositoryService(context, api = FakeGitHubApiService()))
        assertFalse(viewModel.uiState.value.showSignOutDialog)

        viewModel.setSignOutDialogVisible(true)
        assertTrue(viewModel.uiState.value.showSignOutDialog)

        viewModel.setSignOutDialogVisible(false)
        assertFalse(viewModel.uiState.value.showSignOutDialog)

        viewModel.setSignOutDialogVisible(true)
        viewModel.confirmSignOutSync()
        assertFalse(viewModel.uiState.value.showSignOutDialog)
        assertTrue(viewModel.uiState.value.isSignedOut)
        assertEquals("Non connecté", viewModel.uiState.value.accountUsername)
    }
}
