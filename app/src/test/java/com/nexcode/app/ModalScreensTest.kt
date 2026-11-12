package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.ui.screens.chat.ChatViewModel
import com.nexcode.app.ui.screens.editor.EditorViewModel
import com.nexcode.app.ui.screens.projects.ProjectsViewModel
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
class ModalScreensTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testModalDeleteProjectStateInProjectsViewModel() {
        val viewModel = ProjectsViewModel(context)
        assertFalse(viewModel.uiState.value.showDeleteDialog)

        viewModel.openDeleteDialog(viewModel.uiState.value.projects.first())
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
    fun testModalFileMenuStateInEditorViewModel() {
        val viewModel = EditorViewModel(context)
        assertFalse(viewModel.uiState.value.showFileContextMenu)
        assertNull(viewModel.uiState.value.selectedFileForMenu)

        val firstItem = viewModel.uiState.value.treeItems.first()
        viewModel.showContextMenu(firstItem)
        assertTrue(viewModel.uiState.value.showFileContextMenu)
        assertEquals(firstItem, viewModel.uiState.value.selectedFileForMenu)

        viewModel.hideContextMenu()
        assertFalse(viewModel.uiState.value.showFileContextMenu)
        assertNull(viewModel.uiState.value.selectedFileForMenu)
    }

    @Test
    fun testModalSignOutStateInSettingsViewModel() {
        val viewModel = SettingsViewModel()
        assertFalse(viewModel.uiState.value.showSignOutDialog)

        viewModel.setSignOutDialogVisible(true)
        assertTrue(viewModel.uiState.value.showSignOutDialog)

        viewModel.setSignOutDialogVisible(false)
        assertFalse(viewModel.uiState.value.showSignOutDialog)

        viewModel.setSignOutDialogVisible(true)
        viewModel.confirmSignOut()
        assertFalse(viewModel.uiState.value.showSignOutDialog)
        assertTrue(viewModel.uiState.value.isSignedOut)
        assertEquals("Non connecté", viewModel.uiState.value.accountUsername)
    }
}
