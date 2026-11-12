package com.example

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.ui.screens.editor.DependencyStatusVariant
import com.nexcode.app.ui.screens.editor.EditorViewModel
import com.nexcode.app.ui.screens.editor.FileTreeItem
import com.nexcode.app.ui.screens.editor.ResourceTab
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
class EditorTest {

    private lateinit var viewModel: EditorViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        viewModel = EditorViewModel(context)
    }

    @Test
    fun testInitialEditorState() {
        val state = viewModel.uiState.value
        assertEquals("Nimbus Météo", state.projectName)
        assertEquals("MainActivity.kt", state.activeTab)
        assertEquals(1, state.errorCount)
        assertEquals(27, state.codeLines.size)
        assertEquals(14, state.treeItems.size)
        assertEquals(4, state.totalMatchesCount)
        assertEquals(6, state.stringResources.size)
        assertEquals(4, state.colorResources.size)
        assertEquals(3, state.dimensionResources.size)
        assertEquals(6, state.gradleDependencies.size)
    }

    @Test
    fun testSelectFileAndOpenTab() {
        val themeFile = FileTreeItem("Theme.kt", "app/src/main/java/com/nimbus/meteo/Theme.kt", isDirectory = false, indentLevel = 3)
        viewModel.selectFile(themeFile)

        val state = viewModel.uiState.value
        assertEquals("Theme.kt", state.activeTab)
        assertTrue(state.openTabs.contains("Theme.kt"))

        // Fermeture d'onglet
        viewModel.closeTab("Theme.kt")
        assertFalse(viewModel.uiState.value.openTabs.contains("Theme.kt"))
    }

    @Test
    fun testSearchAndReplaceLogic() {
        viewModel.updateSearchQuery("R.string.title_")
        viewModel.updateReplaceQuery("R.string.title_details")
        viewModel.toggleMatchCase()
        assertFalse(viewModel.uiState.value.matchCase)

        // Exécution du remplacement complet
        viewModel.executeReplaceAll()

        val updatedState = viewModel.uiState.value
        assertEquals(0, updatedState.errorCount)
        assertEquals(0, updatedState.totalMatchesCount)
        val fixedLine = updatedState.codeLines.find { it.number == 23 }
        assertNotNull(fixedLine)
        assertTrue(fixedLine!!.content.contains("R.string.title_details"))
        assertFalse(fixedLine.hasError)
    }

    @Test
    fun testResourceTabsSwitching() {
        viewModel.selectResourceTab(ResourceTab.COLORS)
        assertEquals(ResourceTab.COLORS, viewModel.uiState.value.activeResourceTab)

        viewModel.selectResourceTab(ResourceTab.DIMENSIONS)
        assertEquals(ResourceTab.DIMENSIONS, viewModel.uiState.value.activeResourceTab)

        viewModel.selectResourceTab(ResourceTab.THEMES)
        assertEquals(ResourceTab.THEMES, viewModel.uiState.value.activeResourceTab)

        viewModel.selectResourceTab(ResourceTab.STRINGS)
        assertEquals(ResourceTab.STRINGS, viewModel.uiState.value.activeResourceTab)
    }

    @Test
    fun testGradleDependenciesState() {
        val state = viewModel.uiState.value
        val conflictDep = state.gradleDependencies.find { it.name.contains("kotlin-stdlib") }
        assertNotNull(conflictDep)
        assertEquals(DependencyStatusVariant.CONFLICT, conflictDep!!.statusVariant)

        viewModel.updateDependencyInput("androidx.camera:camera-camera2:1.3.0")
        assertEquals("androidx.camera:camera-camera2:1.3.0", viewModel.uiState.value.newDependencyInput)
    }

    @Test
    fun testContextMenuOpenAndClose() {
        val file = viewModel.uiState.value.treeItems.first()
        viewModel.showContextMenu(file)
        assertTrue(viewModel.uiState.value.showFileContextMenu)
        assertEquals(file.name, viewModel.uiState.value.selectedFileForMenu?.name)

        viewModel.hideContextMenu()
        assertFalse(viewModel.uiState.value.showFileContextMenu)
    }
}
