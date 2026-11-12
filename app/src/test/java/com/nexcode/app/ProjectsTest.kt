package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.ui.screens.projects.ProjectFilter
import com.nexcode.app.ui.screens.projects.ProjectTemplateType
import com.nexcode.app.ui.screens.projects.ProjectsViewModel
import com.nexcode.app.ui.screens.projects.ProjectsViewMode
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
class ProjectsTest {

    private lateinit var context: Context
    private lateinit var viewModel: ProjectsViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = ProjectsViewModel(context)
    }

    @Test
    fun testInitialProjectsState() {
        val state = viewModel.uiState.value
        assertEquals(6, state.projects.size)
        assertEquals(6, state.filteredProjects.size)
        assertEquals("Nimbus Météo", state.projects.first().name)
        assertEquals(ProjectFilter.ALL, state.selectedFilter)
        assertEquals(ProjectsViewMode.DEFAULT, state.viewMode)
        assertNotNull(state.selectedProject)
    }

    @Test
    fun testSearchAndFilter() {
        // Recherche
        viewModel.onSearchQueryChange("Ticket")
        assertEquals(1, viewModel.uiState.value.filteredProjects.size)
        assertEquals("TicketFlash", viewModel.uiState.value.filteredProjects.first().name)

        viewModel.onSearchQueryChange("")
        assertEquals(6, viewModel.uiState.value.filteredProjects.size)

        // Filtre Kotlin
        viewModel.onFilterSelect(ProjectFilter.KOTLIN)
        val kotlinProjects = viewModel.uiState.value.filteredProjects
        assertTrue(kotlinProjects.all { it.language == "Kotlin" })

        // Filtre Java
        viewModel.onFilterSelect(ProjectFilter.JAVA)
        val javaProjects = viewModel.uiState.value.filteredProjects
        assertEquals(1, javaProjects.size)
        assertEquals("NotesSimples", javaProjects.first().name)

        // Filtre Flutter
        viewModel.onFilterSelect(ProjectFilter.FLUTTER)
        val flutterProjects = viewModel.uiState.value.filteredProjects
        assertEquals(1, flutterProjects.size)
        assertEquals("SuiviFitness", flutterProjects.first().name)

        // Filtre Exportés
        viewModel.onFilterSelect(ProjectFilter.EXPORTED)
        val exportedProjects = viewModel.uiState.value.filteredProjects
        assertEquals(1, exportedProjects.size)
        assertEquals("MarquesPages", exportedProjects.first().name)
    }

    @Test
    fun testToggleViewMode() {
        assertEquals(ProjectsViewMode.DEFAULT, viewModel.uiState.value.viewMode)
        viewModel.toggleViewMode()
        assertEquals(ProjectsViewMode.EMPTY, viewModel.uiState.value.viewMode)
        viewModel.toggleViewMode()
        assertEquals(ProjectsViewMode.DEFAULT, viewModel.uiState.value.viewMode)
    }

    @Test
    fun testTemplateSelectionAndProjectCreation() = runBlocking {
        viewModel.selectTemplate(ProjectTemplateType.COMPOSE)
        assertEquals(ProjectTemplateType.COMPOSE, viewModel.uiState.value.selectedTemplate)

        viewModel.updateNewAppName("Mon Super App")
        assertEquals("Mon Super App", viewModel.uiState.value.newAppName)
        assertEquals("com.monsuperapp.app", viewModel.uiState.value.newPackageName)

        viewModel.selectMinSdk(29)
        assertEquals(29, viewModel.uiState.value.selectedMinSdk)

        val createdId = viewModel.createProjectSync()

        assertTrue(createdId > 0)
        val state = viewModel.uiState.value
        assertEquals(7, state.projects.size)
        assertEquals("Mon Super App", state.projects.first().name)
        assertEquals("com.monsuperapp.app", state.projects.first().packageName)
    }

    @Test
    fun testDeleteProject() = runBlocking {
        val target = viewModel.uiState.value.projects.first()
        viewModel.openDeleteDialog(target)
        assertTrue(viewModel.uiState.value.showDeleteDialog)
        assertEquals(target, viewModel.uiState.value.projectToDelete)

        viewModel.confirmDeleteProjectSync()
        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertEquals(5, viewModel.uiState.value.projects.size)
        assertFalse(viewModel.uiState.value.projects.any { it.id == target.id })
    }

    @Test
    fun testImportZipAndGit() {
        var zipImported = false
        viewModel.importZip { zipImported = true }
        assertTrue(zipImported)
        assertEquals(7, viewModel.uiState.value.projects.size)
        assertEquals("Météo Android", viewModel.uiState.value.projects.first().name)

        viewModel.setImportTab(1)
        assertEquals(1, viewModel.uiState.value.importTabIndex)

        viewModel.updateImportGitUrl("github.com/nimbus-dev/custom-repo")
        viewModel.updateImportGitBranch("develop")

        var gitImported = false
        viewModel.importGit { gitImported = true }
        assertTrue(gitImported)
        assertEquals(8, viewModel.uiState.value.projects.size)
        assertEquals("custom-repo", viewModel.uiState.value.projects.first().name)
    }
}
