package com.nexcode.app.ui.screens.projects

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.git.GitOperationResult
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.github.repository.GitHubResult
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.entity.BuildEntity
import com.nexcode.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

class ProjectsViewModel(
    private val context: Context,
    overrideGitService: GitHubRepositoryService? = null
) : ViewModel() {

    private val database = NexcodeDatabase.getInstance(context)
    private val projectDao = database.projectDao()
    private val buildDao = database.buildDao()
    private val gitService = overrideGitService ?: GitHubRepositoryService(context)
    private val templateMetadataById = ConcurrentHashMap<Long, Pair<String, String>>()

    // Données réelles issues de BuildDao, indexées par projet
    private val buildsByProjectId = ConcurrentHashMap<Long, List<BuildEntity>>()
    private val entitiesById = ConcurrentHashMap<Long, ProjectEntity>()
    private val buildObserverJobs = ConcurrentHashMap<Long, Job>()

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        loadProjectsFromDb()
        loadUserRepositories()
    }

    private fun loadUserRepositories() {
        viewModelScope.launch {
            if (!gitService.hasSavedToken()) return@launch
            _uiState.update { it.copy(isLoadingUserRepos = true) }
            when (val res = gitService.getUserRepositories()) {
                is GitHubResult.Success -> {
                    _uiState.update {
                        it.copy(
                            userRepositories = res.data,
                            isLoadingUserRepos = false
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingUserRepos = false) }
                }
            }
        }
    }

    private fun loadProjectsFromDb() {
        viewModelScope.launch {
            projectDao.getAllProjects().collect { entities ->
                entities.forEach { entity -> entitiesById[entity.id] = entity }
                entitiesById.keys.retainAll(entities.map { it.id }.toSet())
                val mapped = entities.map { entity -> mapEntityToItem(entity) }
                val totalBytes = entities.sumOf { entity -> directoryBytes(File(entity.path)) }
                _uiState.update { current ->
                    val filtered = filterProjectList(mapped, current.searchQuery, current.selectedFilter)
                    val selectedId = current.selectedProject?.id
                        ?: mapped.firstOrNull()?.id
                    current.copy(
                        projects = mapped,
                        filteredProjects = filtered,
                        totalProjectCount = mapped.size,
                        totalSizeText = formatBytes(totalBytes),
                        selectedProject = mapped.firstOrNull { it.id == selectedId },
                        recentBuilds = recentBuildsFor(selectedId),
                        viewMode = if (mapped.isEmpty()) ProjectsViewMode.EMPTY else ProjectsViewMode.DEFAULT
                    )
                }
                syncBuildObservers(entities.map { it.id }.toSet())
            }
        }
    }

    private fun mapEntityToItem(entity: ProjectEntity): ProjectItem {
        val meta = templateMetadataById[entity.id]
        val projectDir = File(entity.path)
        val inferredLanguage = meta?.first ?: detectLanguageFromDir(projectDir)
        val inferredFramework = meta?.second ?: detectFrameworkFromDir(projectDir, inferredLanguage)
        val initials = computeInitials(entity.name)
        val hasGit = File(projectDir, ".git").exists()
        val fileCount = countProjectFiles(projectDir)

        // Statut réel issu des builds enregistrés en base (BuildDao)
        val builds = buildsByProjectId[entity.id].orEmpty()
        val latestBuild = builds.firstOrNull()
        val buildStatus: ProjectBuildStatus
        val buildStatusLabel: String
        when (latestBuild?.status?.uppercase(Locale.ROOT)) {
            "SUCCESS" -> {
                buildStatus = ProjectBuildStatus.SUCCESS
                buildStatusLabel = if (latestBuild.runNumber > 0) {
                    "Build #${latestBuild.runNumber} réussi"
                } else {
                    "Dernier build réussi"
                }
            }
            "FAILED" -> {
                buildStatus = ProjectBuildStatus.FAILED
                buildStatusLabel = if (latestBuild.runNumber > 0) {
                    "Build #${latestBuild.runNumber} en échec"
                } else {
                    "Dernier build en échec"
                }
            }
            "RUNNING" -> {
                buildStatus = ProjectBuildStatus.RUNNING
                buildStatusLabel = "Build en cours…"
            }
            else -> {
                buildStatus = ProjectBuildStatus.DRAFT
                buildStatusLabel = "Aucun build enregistré"
            }
        }

        // Date réelle : dernière modification du dossier projet, sinon updatedAt de l'entité
        val lastModifiedMs = if (projectDir.exists()) projectDir.lastModified() else entity.updatedAt
        val modifiedStamp = formatTimestamp(lastModifiedMs, "dd/MM 'à' HH:mm")
        val modifiedText = if (modifiedStamp.isBlank()) {
            "date de modification inconnue"
        } else {
            "Modifié le $modifiedStamp"
        }

        return ProjectItem(
            id = entity.id,
            name = entity.name,
            packageName = entity.packageName,
            initials = initials,
            language = inferredLanguage,
            uiFramework = inferredFramework,
            modifiedText = modifiedText,
            buildStatus = buildStatus,
            buildStatusLabel = buildStatusLabel,
            version = latestBuild?.versionName?.takeIf { it.isNotBlank() },
            onGitHub = hasGit,
            fileCount = fileCount,
            sizeText = formatDirSize(projectDir),
            buildCount = builds.size,
            gitRepo = entity.gitRepo,
            gitBranch = entity.gitBranch,
            gitSyncText = if (hasGit) {
                "Dépôt Git local · branche ${entity.gitBranch.ifBlank { "main" }}"
            } else {
                "Aucun dépôt Git initialisé"
            },
            minSdk = entity.minSdk,
            localPath = entity.path
        )
    }

    private fun computeInitials(name: String): String {
        return name.split(" ", "-", "_")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { name.take(2).uppercase().ifEmpty { "NX" } }
    }

    private fun detectLanguageFromDir(dir: File): String {
        if (!dir.exists()) return "Kotlin"
        var hasJava = false
        var hasDart = false
        dir.walkTopDown().maxDepth(5).forEach { f ->
            if (f.isFile) {
                when (f.extension.lowercase()) {
                    "java" -> hasJava = true
                    "dart" -> hasDart = true
                }
            }
        }
        return when {
            hasDart -> "Flutter"
            hasJava -> "Java"
            else -> "Kotlin"
        }
    }

    private fun detectFrameworkFromDir(dir: File, language: String): String {
        return when (language) {
            "Flutter" -> "Dart"
            "Java" -> "XML"
            else -> "Compose"
        }
    }

    private fun countProjectFiles(dir: File): Int {
        if (!dir.exists()) return 0
        return dir.walkTopDown().count { it.isFile && !it.path.contains(".git") }
    }

    private fun directoryBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 Mo"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb < 1.0) "1 Mo" else String.format(Locale.FRENCH, "%.1f Mo", mb)
    }

    private fun formatDirSize(dir: File): String = formatBytes(directoryBytes(dir))

    private fun formatTimestamp(epochMs: Long, pattern: String): String {
        if (epochMs <= 0L) return ""
        return SimpleDateFormat(pattern, Locale.FRENCH).format(Date(epochMs))
    }

    private fun formatDurationFr(durationMs: Long): String? {
        if (durationMs <= 0L) return null
        val totalSeconds = durationMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0L) "$minutes min $seconds s" else "$seconds s"
    }

    private fun mapBuildEntityToItem(build: BuildEntity): ProjectBuildItem {
        val isSuccess = build.status.equals("SUCCESS", ignoreCase = true)
        val isFailed = build.status.equals("FAILED", ignoreCase = true)
        val title = buildString {
            if (build.runNumber > 0) {
                append("Build #${build.runNumber}")
                append(" · ")
            }
            append(build.versionName)
            append(" · ")
            append(build.buildType)
        }
        val parts = mutableListOf<String>()
        val dateText = formatTimestamp(build.startTime, "dd/MM HH:mm")
        parts.add(dateText.ifBlank { "date inconnue" })
        formatDurationFr(build.durationMs)?.let { parts.add(it) }
        if (build.apkSize > 0L) {
            parts.add(formatBytes(build.apkSize))
        }
        return ProjectBuildItem(
            id = build.id,
            versionText = title,
            dateAndSizeText = parts.joinToString(" · "),
            isSuccess = isSuccess,
            hasLogsChip = isFailed && (!build.logOutput.isNullOrBlank() || build.errorCount > 0),
            errorText = build.errorSummary
        )
    }

    private fun recentBuildsFor(projectId: Long?): List<ProjectBuildItem> {
        if (projectId == null) return emptyList()
        return buildsByProjectId[projectId].orEmpty()
            .take(RECENT_BUILDS_LIMIT)
            .map { mapBuildEntityToItem(it) }
    }

    private fun syncBuildObservers(activeProjectIds: Set<Long>) {
        buildObserverJobs.keys.filterNot { it in activeProjectIds }.forEach { projectId ->
            buildObserverJobs.remove(projectId)?.cancel()
            buildsByProjectId.remove(projectId)
        }
        activeProjectIds.forEach { projectId ->
            if (!buildObserverJobs.containsKey(projectId)) {
                buildObserverJobs[projectId] = viewModelScope.launch {
                    buildDao.getBuildsByProject(projectId).collect { builds ->
                        buildsByProjectId[projectId] = builds
                        onBuildsUpdated(projectId)
                    }
                }
            }
        }
    }

    private fun onBuildsUpdated(projectId: Long) {
        val entity = entitiesById[projectId] ?: return
        val refreshedItem = mapEntityToItem(entity)
        _uiState.update { current ->
            val updated = current.projects.map { if (it.id == projectId) refreshedItem else it }
            val selectedId = current.selectedProject?.id
            current.copy(
                projects = updated,
                filteredProjects = filterProjectList(updated, current.searchQuery, current.selectedFilter),
                selectedProject = current.selectedProject?.let { sel ->
                    updated.firstOrNull { it.id == sel.id } ?: sel
                },
                recentBuilds = if (selectedId == projectId) recentBuildsFor(projectId) else current.recentBuilds
            )
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

    fun setViewMode(mode: ProjectsViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectProject(project: ProjectItem) {
        _uiState.update {
            it.copy(
                selectedProject = project,
                recentBuilds = recentBuildsFor(project.id)
            )
        }
    }

    fun selectProjectById(id: Long) {
        val found = _uiState.value.projects.firstOrNull { it.id == id }
        if (found != null) {
            _uiState.update {
                it.copy(
                    selectedProject = found,
                    recentBuilds = recentBuildsFor(id)
                )
            }
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
        templateMetadataById.remove(target.id)
        withContext(Dispatchers.IO) {
            if (target.localPath.isNotBlank()) {
                File(target.localPath).deleteRecursively()
            }
        }
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
                viewMode = if (updatedList.isEmpty()) ProjectsViewMode.EMPTY else ProjectsViewMode.DEFAULT
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
        val clean = name.lowercase().replace(" ", "").replace(Regex("[^a-z0-9]"), "")
        val autoPackage = "com.${clean.ifBlank { "nexcode" }}.app"
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
        val safeName = state.newAppName.trim().ifBlank { "NouveauProjet" }
        val safePkg = state.newPackageName.trim().ifBlank { "com.nexcode.app" }
        val projectDir = File(context.filesDir, "projects/$safeName")

        withContext(Dispatchers.IO) {
            projectDir.mkdirs()
            val srcExt = when (state.selectedTemplate) {
                ProjectTemplateType.JAVA -> "java"
                ProjectTemplateType.FLUTTER -> "dart"
                else -> "kt"
            }
            val mainDir = File(projectDir, "app/src/main/java/${safePkg.replace('.', '/')}")
            mainDir.mkdirs()
            File(mainDir, "MainActivity.$srcExt").writeText("// $safeName ($safePkg)\n")
            File(projectDir, "build.gradle.kts").writeText("// Gradle config for $safeName\n")
        }

        val entity = ProjectEntity(
            name = safeName,
            packageName = safePkg,
            path = projectDir.absolutePath,
            minSdk = state.selectedMinSdk,
            targetSdk = 35,
            gitBranch = "main"
        )
        val newId = projectDao.insertProject(entity)

        val language = when (state.selectedTemplate) {
            ProjectTemplateType.COMPOSE, ProjectTemplateType.XML, ProjectTemplateType.EMPTY -> "Kotlin"
            ProjectTemplateType.JAVA -> "Java"
            ProjectTemplateType.FLUTTER -> "Flutter"
        }
        val uiFramework = when (state.selectedTemplate) {
            ProjectTemplateType.COMPOSE -> "Compose"
            ProjectTemplateType.XML, ProjectTemplateType.JAVA -> "XML"
            ProjectTemplateType.FLUTTER -> "Dart"
            ProjectTemplateType.EMPTY -> "Gradle"
        }
        templateMetadataById[newId] = language to uiFramework

        // Vraie date de création du dossier projet (le libellé « modifié à l'instant »
        // contredisait la date réelle affichée ensuite par l'émission Room).
        val createdStamp = formatTimestamp(projectDir.lastModified(), "dd/MM 'à' HH:mm")

        val newProjectItem = ProjectItem(
            id = newId,
            name = safeName,
            packageName = safePkg,
            initials = computeInitials(safeName),
            language = language,
            uiFramework = uiFramework,
            modifiedText = if (createdStamp.isBlank()) {
                "date de modification inconnue"
            } else {
                "Modifié le $createdStamp"
            },
            buildStatus = ProjectBuildStatus.DRAFT,
            buildStatusLabel = "Aucun build enregistré",
            fileCount = countProjectFiles(projectDir),
            sizeText = formatDirSize(projectDir),
            buildCount = 0,
            gitBranch = "main",
            gitSyncText = "Aucun dépôt Git initialisé",
            minSdk = state.selectedMinSdk,
            localPath = projectDir.absolutePath
        )

        _uiState.update { current ->
            val updated = listOf(newProjectItem) + current.projects.filter { it.id != newId }
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
        _uiState.update { it.copy(importTabIndex = tabIndex, importError = null) }
    }

    fun updateImportGitUrl(url: String) {
        _uiState.update { it.copy(importGitUrl = url, importError = null) }
    }

    fun updateImportGitBranch(branch: String) {
        _uiState.update { it.copy(importGitBranch = branch) }
    }

    fun selectUserRepository(fullName: String, defaultBranch: String) {
        _uiState.update {
            it.copy(
                importGitUrl = "https://github.com/$fullName.git",
                importGitBranch = defaultBranch.ifBlank { "main" },
                importError = null
            )
        }
    }

    suspend fun importZipFileSync(zipFile: File): ProjectItem? {
        _uiState.update { it.copy(isImporting = true, importError = null) }
        return try {
            val projectName = zipFile.nameWithoutExtension.ifBlank { "projet-zip" }
            val targetDir = File(context.filesDir, "projects/$projectName")
            var fileCount = 0
            withContext(Dispatchers.IO) {
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()
                FileInputStream(zipFile).use { fis ->
                    ZipInputStream(fis).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val outFile = File(targetDir, entry.name)
                            if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                                throw SecurityException("Entrée ZIP invalide: ${entry.name}")
                            }
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                fileCount++
                            }
                            entry = zis.nextEntry
                        }
                    }
                }
            }
            val entity = ProjectEntity(
                name = projectName,
                packageName = "com.import.${projectName.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "app" }}",
                path = targetDir.absolutePath,
                minSdk = 29,
                targetSdk = 35,
                gitBranch = "main"
            )
            val id = projectDao.insertProject(entity)
            val imported = mapEntityToItem(entity.copy(id = id)).copy(
                fileCount = fileCount.coerceAtLeast(1),
                modifiedText = "importé à l'instant",
                buildStatusLabel = "Importé · prêt à compiler"
            )
            _uiState.update { current ->
                val updated = listOf(imported) + current.projects.filter { it.id != id }
                val filtered = filterProjectList(updated, current.searchQuery, current.selectedFilter)
                current.copy(
                    projects = updated,
                    filteredProjects = filtered,
                    totalProjectCount = updated.size,
                    selectedProject = imported,
                    viewMode = ProjectsViewMode.DEFAULT,
                    isImporting = false,
                    recentZipImportName = zipFile.name,
                    recentZipImportDesc = "$fileCount fichiers extraits"
                )
            }
            imported
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isImporting = false,
                    importError = e.message ?: "Erreur lors de l'extraction ZIP."
                )
            }
            null
        }
    }

    fun importZipFromUri(uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importError = null) }
            try {
                var displayName = "projet_importe.zip"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                }
                val tempZip = File(context.cacheDir, displayName)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempZip).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Impossible d'ouvrir le fichier ZIP sélectionné.")
                }
                val item = importZipFileSync(tempZip)
                tempZip.delete()
                if (item != null) onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importError = e.message ?: "Erreur lors de l'extraction de l'archive ZIP."
                    )
                }
            }
        }
    }

    suspend fun importGitSync(): ProjectItem? {
        val state = _uiState.value
        val repoUrl = state.importGitUrl.trim()
        val branch = state.importGitBranch.trim().ifEmpty { "main" }
        val repoName = repoUrl.trimEnd('/')
            .substringAfterLast("/")
            .removeSuffix(".git")
            .ifEmpty { "repo-git" }
        val targetDir = File(context.filesDir, "projects/$repoName")

        _uiState.update { it.copy(isImporting = true, importError = null) }
        return when (val res = gitService.cloneRepository(repoUrl, targetDir, branch)) {
            is GitOperationResult.Success -> {
                val entity = ProjectEntity(
                    name = repoName,
                    packageName = "com.git.${repoName.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "app" }}",
                    path = targetDir.absolutePath,
                    minSdk = 29,
                    targetSdk = 35,
                    gitBranch = branch
                )
                val id = projectDao.insertProject(entity)
                val imported = mapEntityToItem(entity.copy(id = id)).copy(
                    onGitHub = true,
                    gitRepo = repoUrl,
                    gitBranch = branch,
                    gitSyncText = "synchronisé à l'instant",
                    modifiedText = "cloné à l'instant",
                    buildStatus = ProjectBuildStatus.DRAFT,
                    buildStatusLabel = "Cloné · prêt à compiler"
                )
                _uiState.update { current ->
                    val updated = listOf(imported) + current.projects.filter { it.id != id }
                    val filtered = filterProjectList(updated, current.searchQuery, current.selectedFilter)
                    current.copy(
                        projects = updated,
                        filteredProjects = filtered,
                        totalProjectCount = updated.size,
                        selectedProject = imported,
                        viewMode = ProjectsViewMode.DEFAULT,
                        isImporting = false,
                        importError = null
                    )
                }
                imported
            }
            is GitOperationResult.Error -> {
                _uiState.update {
                    it.copy(isImporting = false, importError = res.message)
                }
                null
            }
            is GitOperationResult.Conflict -> {
                _uiState.update {
                    it.copy(isImporting = false, importError = "Conflit détecté pendant le clonage.")
                }
                null
            }
        }
    }

    fun importGit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (importGitSync() != null) {
                onSuccess()
            }
        }
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

    private companion object {
        const val RECENT_BUILDS_LIMIT = 5
    }
}
