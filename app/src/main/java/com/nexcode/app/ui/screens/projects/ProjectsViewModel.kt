package com.nexcode.app.ui.screens.projects

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.entity.BuildEntity
import com.nexcode.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProjectsViewModel(
    context: Context
) : ViewModel() {

    private val database = NexcodeDatabase.getInstance(context)
    private val projectDao = database.projectDao()
    private val buildDao = database.buildDao()

    private val initialSampleProjects = listOf(
        ProjectItem(
            id = 1L,
            name = "Nimbus Météo",
            packageName = "com.nimbus.meteo",
            initials = "NM",
            language = "Kotlin",
            uiFramework = "Compose",
            modifiedText = "modifié hier à 17:24",
            buildStatus = ProjectBuildStatus.SUCCESS,
            buildStatusLabel = "Build réussi",
            version = "v1.2.0",
            onGitHub = false,
            tileBgHex = null,
            tileTextHex = null,
            fileCount = 34,
            sizeText = "8,2 Mo",
            buildCount = 12,
            gitRepo = "github.com/nimbus-dev/meteo-android",
            gitBranch = "main",
            gitSyncText = "synchronisé il y a 12 min",
            minSdk = 29
        ),
        ProjectItem(
            id = 2L,
            name = "TicketFlash",
            packageName = "com.ticketflash.app",
            initials = "TF",
            language = "Kotlin",
            uiFramework = "XML",
            modifiedText = "modifié hier à 09:02",
            buildStatus = ProjectBuildStatus.FAILED,
            buildStatusLabel = "Build échoué",
            version = "v0.9.2",
            onGitHub = false,
            tileBgHex = "#FF5C7A",
            tileTextHex = "#2B0A12",
            fileCount = 28,
            sizeText = "14,5 Mo",
            buildCount = 9,
            gitRepo = "github.com/nimbus-dev/ticketflash-android",
            gitBranch = "main",
            gitSyncText = "synchronisé hier",
            minSdk = 26
        ),
        ProjectItem(
            id = 3L,
            name = "SuiviFitness",
            packageName = "com.suivifitness.app",
            initials = "SF",
            language = "Flutter",
            uiFramework = "Dart",
            modifiedText = "modifié hier à 08:15",
            buildStatus = ProjectBuildStatus.RUNNING,
            buildStatusLabel = "Compilation en cours",
            version = null,
            onGitHub = false,
            tileBgHex = "#FFB84D",
            tileTextHex = "#2B1D06",
            fileCount = 42,
            sizeText = "22,1 Mo",
            buildCount = 15,
            gitRepo = "github.com/nimbus-dev/suivifitness",
            gitBranch = "main",
            gitSyncText = "synchronisé hier",
            minSdk = 24
        ),
        ProjectItem(
            id = 4L,
            name = "NotesSimples",
            packageName = "com.notessimples.app",
            initials = "NS",
            language = "Java",
            uiFramework = "XML",
            modifiedText = "modifié le 3 nov.",
            buildStatus = ProjectBuildStatus.SUCCESS,
            buildStatusLabel = "Build réussi",
            version = "v2.3.1",
            onGitHub = false,
            tileBgHex = "#2E7D32",
            tileTextHex = "#E7F5E8",
            fileCount = 19,
            sizeText = "5,4 Mo",
            buildCount = 8,
            gitRepo = "github.com/nimbus-dev/notessimples",
            gitBranch = "main",
            gitSyncText = "synchronisé le 3 nov.",
            minSdk = 26
        ),
        ProjectItem(
            id = 5L,
            name = "PortailClient",
            packageName = "com.portailclient.app",
            initials = "PC",
            language = "Kotlin",
            uiFramework = "Compose",
            modifiedText = "modifié le 28 oct.",
            buildStatus = ProjectBuildStatus.DRAFT,
            buildStatusLabel = "Brouillon · jamais compilé",
            version = null,
            onGitHub = false,
            tileBgHex = null,
            tileTextHex = null,
            fileCount = 12,
            sizeText = "3,1 Mo",
            buildCount = 0,
            gitRepo = "github.com/nimbus-dev/portailclient",
            gitBranch = "main",
            gitSyncText = "non synchronisé",
            minSdk = 29
        ),
        ProjectItem(
            id = 6L,
            name = "MarquesPages",
            packageName = "com.marquespages.app",
            initials = "MP",
            language = "Kotlin",
            uiFramework = "XML",
            modifiedText = "modifié le 21 oct.",
            buildStatus = ProjectBuildStatus.SUCCESS,
            buildStatusLabel = "Build réussi",
            version = null,
            onGitHub = true,
            tileBgHex = null,
            tileTextHex = null,
            fileCount = 22,
            sizeText = "6,8 Mo",
            buildCount = 6,
            gitRepo = "github.com/nimbus-dev/marquespages",
            gitBranch = "main",
            gitSyncText = "synchronisé le 21 oct.",
            minSdk = 26
        )
    )

    private val sampleBuilds = listOf(
        ProjectBuildItem(
            id = 1L,
            versionText = "v1.2.0 (12) · release",
            dateAndSizeText = "Il y a 2 h · 8,1 Mo · signé",
            isSuccess = true
        ),
        ProjectBuildItem(
            id = 2L,
            versionText = "v1.1.9 (11) · debug",
            dateAndSizeText = "Hier · 12,4 Mo",
            isSuccess = true
        ),
        ProjectBuildItem(
            id = 3L,
            versionText = "v1.1.8 (10) · release",
            dateAndSizeText = "Le 3 nov. · 2 erreurs",
            isSuccess = false,
            hasLogsChip = true,
            errorText = "2 erreurs"
        )
    )

    private val _uiState = MutableStateFlow(
        ProjectsUiState(
            projects = initialSampleProjects,
            filteredProjects = initialSampleProjects,
            selectedProject = initialSampleProjects.first(),
            recentBuilds = sampleBuilds
        )
    )
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        loadProjectsFromDb()
    }

    private fun loadProjectsFromDb() {
        viewModelScope.launch {
            // Initialisation en base si vide
            val existing = projectDao.getProjectById(1L)
            if (existing == null) {
                initialSampleProjects.forEach { sample ->
                    projectDao.insertProject(
                        ProjectEntity(
                            id = sample.id,
                            name = sample.name,
                            packageName = sample.packageName,
                            path = "/storage/emulated/0/NEXCODE/${sample.name}",
                            minSdk = sample.minSdk,
                            targetSdk = 35,
                            gitBranch = sample.gitBranch
                        )
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            val filtered = filterProjectList(current.projects, query, current.selectedFilter)
            current.copy(searchQuery = query, filteredProjects = filtered)
        }
    }

    fun onFilterSelect(filter: ProjectFilter) {
        _uiState.update { current ->
            val filtered = filterProjectList(current.projects, current.searchQuery, filter)
            current.copy(selectedFilter = filter, filteredProjects = filtered)
        }
    }

    fun toggleViewMode() {
        _uiState.update { current ->
            val newMode = if (current.viewMode == ProjectsViewMode.DEFAULT) {
                ProjectsViewMode.EMPTY
            } else {
                ProjectsViewMode.DEFAULT
            }
            current.copy(viewMode = newMode)
        }
    }

    fun setViewMode(mode: ProjectsViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectProject(project: ProjectItem) {
        _uiState.update { it.copy(selectedProject = project) }
    }

    fun selectProjectById(id: Long) {
        val found = _uiState.value.projects.firstOrNull { it.id == id }
        if (found != null) {
            _uiState.update { it.copy(selectedProject = found) }
        }
    }

    fun openDeleteDialog(project: ProjectItem) {
        _uiState.update { it.copy(projectToDelete = project, showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(projectToDelete = null, showDeleteDialog = false) }
    }

    suspend fun confirmDeleteProjectSync() {
        val target = _uiState.value.projectToDelete ?: return
        projectDao.deleteProjectById(target.id)
        _uiState.update { current ->
            val updatedList = current.projects.filter { it.id != target.id }
            val filtered = filterProjectList(updatedList, current.searchQuery, current.selectedFilter)
            val newSelected = updatedList.firstOrNull()
            current.copy(
                projects = updatedList,
                filteredProjects = filtered,
                totalProjectCount = updatedList.size,
                selectedProject = newSelected,
                projectToDelete = null,
                showDeleteDialog = false,
                viewMode = if (updatedList.isEmpty()) ProjectsViewMode.EMPTY else current.viewMode
            )
        }
    }

    fun confirmDeleteProject() {
        viewModelScope.launch {
            confirmDeleteProjectSync()
        }
    }

    // Écran 6 : Choix du modèle
    fun selectTemplate(template: ProjectTemplateType) {
        _uiState.update { it.copy(selectedTemplate = template) }
    }

    // Écran 7 : Assistant de création
    fun updateNewAppName(name: String) {
        val autoPackage = "com." + name.lowercase().replace(" ", "").replace("[^a-z0-9]".toRegex(), "") + ".app"
        _uiState.update { it.copy(newAppName = name, newPackageName = autoPackage) }
    }

    fun updateNewPackageName(packageName: String) {
        _uiState.update { it.copy(newPackageName = packageName) }
    }

    fun selectMinSdk(minSdk: Int) {
        _uiState.update { it.copy(selectedMinSdk = minSdk) }
    }

    fun toggleAiScaffold(enabled: Boolean) {
        _uiState.update { it.copy(isAiScaffoldEnabled = enabled) }
    }

    suspend fun createProjectSync(): Long {
        val state = _uiState.value
        val entity = ProjectEntity(
            name = state.newAppName,
            packageName = state.newPackageName,
            path = "/storage/emulated/0/NEXCODE/${state.newAppName}",
            minSdk = state.selectedMinSdk,
            targetSdk = 35,
            gitBranch = "main"
        )
        val newId = projectDao.insertProject(entity)

        val initials = state.newAppName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "NX" }

        val newProjectItem = ProjectItem(
            id = newId,
            name = state.newAppName,
            packageName = state.newPackageName,
            initials = initials,
            language = when (state.selectedTemplate) {
                ProjectTemplateType.COMPOSE, ProjectTemplateType.XML -> "Kotlin"
                ProjectTemplateType.JAVA -> "Java"
                ProjectTemplateType.FLUTTER -> "Flutter"
                ProjectTemplateType.EMPTY -> "Kotlin"
            },
            uiFramework = when (state.selectedTemplate) {
                ProjectTemplateType.COMPOSE -> "Compose"
                ProjectTemplateType.XML, ProjectTemplateType.JAVA -> "XML"
                ProjectTemplateType.FLUTTER -> "Dart"
                ProjectTemplateType.EMPTY -> "Gradle"
            },
            modifiedText = "modifié à l'instant",
            buildStatus = ProjectBuildStatus.DRAFT,
            buildStatusLabel = "Brouillon · jamais compilé",
            version = "v0.1.0",
            fileCount = 14,
            sizeText = "1,8 Mo",
            buildCount = 0,
            gitRepo = "github.com/nimbus-dev/${state.newAppName.lowercase().replace(' ', '-')}",
            gitBranch = "main",
            gitSyncText = "non synchronisé",
            minSdk = state.selectedMinSdk
        )

        _uiState.update { current ->
            val updated = listOf(newProjectItem) + current.projects
            val filtered = filterProjectList(updated, current.searchQuery, current.selectedFilter)
            current.copy(
                projects = updated,
                filteredProjects = filtered,
                totalProjectCount = updated.size,
                selectedProject = newProjectItem,
                viewMode = ProjectsViewMode.DEFAULT
            )
        }
        return newId
    }

    fun createProject(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = createProjectSync()
            onSuccess(newId)
        }
    }

    // Écran 9 : Importer
    fun setImportTab(tabIndex: Int) {
        _uiState.update { it.copy(importTabIndex = tabIndex) }
    }

    fun updateImportGitUrl(url: String) {
        _uiState.update { it.copy(importGitUrl = url) }
    }

    fun updateImportGitBranch(branch: String) {
        _uiState.update { it.copy(importGitBranch = branch) }
    }

    fun importZip(onSuccess: () -> Unit) {
        // Ajout d'un projet importé
        val imported = ProjectItem(
            id = System.currentTimeMillis(),
            name = "Météo Android",
            packageName = "com.nimbus.meteo",
            initials = "MA",
            language = "Kotlin",
            uiFramework = "Compose",
            modifiedText = "modifié il y a 5 min",
            buildStatus = ProjectBuildStatus.SUCCESS,
            buildStatusLabel = "Build réussi",
            version = "v1.0.0",
            fileCount = 38,
            sizeText = "46,3 Mo",
            buildCount = 1,
            minSdk = 29
        )
        _uiState.update { current ->
            val updated = listOf(imported) + current.projects
            val filtered = filterProjectList(updated, current.searchQuery, current.selectedFilter)
            current.copy(
                projects = updated,
                filteredProjects = filtered,
                totalProjectCount = updated.size,
                selectedProject = imported,
                viewMode = ProjectsViewMode.DEFAULT
            )
        }
        onSuccess()
    }

    fun importGit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val repoName = state.importGitUrl.substringAfterLast("/").ifEmpty { "Repo-Git" }
        val initials = repoName.take(2).uppercase()
        val imported = ProjectItem(
            id = System.currentTimeMillis(),
            name = repoName,
            packageName = "com.git.${repoName.lowercase().replace("-", "")}",
            initials = initials,
            language = "Kotlin",
            uiFramework = "Compose",
            modifiedText = "modifié à l'instant",
            buildStatus = ProjectBuildStatus.SUCCESS,
            buildStatusLabel = "Build réussi",
            version = "v1.0.0",
            onGitHub = true,
            fileCount = 34,
            sizeText = "8,2 Mo",
            buildCount = 2,
            gitRepo = state.importGitUrl,
            gitBranch = state.importGitBranch,
            gitSyncText = "synchronisé à l'instant",
            minSdk = 29
        )
        _uiState.update { current ->
            val updated = listOf(imported) + current.projects
            val filtered = filterProjectList(updated, current.searchQuery, current.selectedFilter)
            current.copy(
                projects = updated,
                filteredProjects = filtered,
                totalProjectCount = updated.size,
                selectedProject = imported,
                viewMode = ProjectsViewMode.DEFAULT
            )
        }
        onSuccess()
    }

    private fun filterProjectList(
        list: List<ProjectItem>,
        query: String,
        filter: ProjectFilter
    ): List<ProjectItem> {
        return list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                ProjectFilter.ALL -> true
                ProjectFilter.KOTLIN -> item.language.equals("Kotlin", ignoreCase = true)
                ProjectFilter.JAVA -> item.language.equals("Java", ignoreCase = true)
                ProjectFilter.FLUTTER -> item.language.equals("Flutter", ignoreCase = true)
                ProjectFilter.EXPORTED -> item.onGitHub
            }
            matchesQuery && matchesFilter
        }
    }
}
