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
import com.nexcode.app.data.preferences.NexcodePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModel(
    private val context: Context,
    private val gitService: GitHubRepositoryService = GitHubRepositoryService(context)
) : ViewModel() {

    private val database = NexcodeDatabase.getInstance(context)
    private val projectDao = database.projectDao()
    private val buildDao = database.buildDao()
    private val preferences = NexcodePreferences(context)

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        observeProjectsFromDb()
        observeSelectedProjectBuilds()
        loadUserRepositories()
    }

    private fun projectsRootDir(): File {
        return File(context.filesDir, "projects").apply { mkdirs() }
    }

    private fun observeProjectsFromDb() {
        viewModelScope.launch {
            projectDao.getAllProjects().collectLatest { entities ->
                val savedActiveId = runCatching { preferences.activeProjectIdFlow.first() }.getOrNull()
                var totalBytes = 0L
                val mappedItems = entities.map { entity ->
                    val projectDir = File(entity.path)
                    val stats = withContext(Dispatchers.IO) { inspectDirectoryStats(projectDir) }
                    totalBytes += stats.totalBytes
                    val builds = runCatching { buildDao.getBuildsByProject(entity.id).first() }.getOrDefault(emptyList())
                    mapEntityToItem(entity, stats, builds)
                }

                _uiState.update { current ->
                    val currentSelectedId = current.selectedProject?.id ?: savedActiveId
                    val newSelected = mappedItems.firstOrNull { it.id == currentSelectedId }
                        ?: mappedItems.firstOrNull()
                    val filtered = filterProjectList(mappedItems, current.searchQuery, current.selectedFilter)
                    current.copy(
                        projects = mappedItems,
                        filteredProjects = filtered,
                        totalProjectCount = mappedItems.size,
                        totalSizeText = formatFrenchSize(totalBytes),
                        selectedProject = newSelected,
                        viewMode = if (mappedItems.isEmpty()) ProjectsViewMode.EMPTY else ProjectsViewMode.DEFAULT
                    )
                }
            }
        }
    }

    private fun observeSelectedProjectBuilds() {
        viewModelScope.launch {
            _uiState
                .map { it.selectedProject?.id }
                .distinctUntilChanged()
                .flatMapLatest { projectId ->
                    if (projectId == null) flowOf(emptyList())
                    else buildDao.getBuildsByProject(projectId)
                }
                .collect { buildEntities ->
                    val mappedBuilds = buildEntities.take(10).map { entity ->
                        val isSuccess = entity.status == "SUCCESS"
                        val isRunning = entity.status == "RUNNING"
                        val runPart = if (entity.runNumber > 0) " (${entity.runNumber})" else ""
                        val sizePart = if (entity.apkSize > 0) " · ${formatFrenchSize(entity.apkSize)}" else ""
                        val statusPart = when {
                            isRunning -> " · en cours"
                            !isSuccess && entity.errorCount > 0 -> " · ${entity.errorCount} erreur(s)"
                            !isSuccess -> " · échec"
                            else -> sizePart
                        }
                        ProjectBuildItem(
                            id = entity.id,
                            versionText = "v${entity.versionName}$runPart · ${entity.buildType}",
                            dateAndSizeText = "${formatRelativeTime(entity.startTime)}$statusPart",
                            isSuccess = isSuccess,
                            hasLogsChip = !isSuccess && !isRunning,
                            errorText = entity.errorSummary
                        )
                    }
                    _uiState.update { it.copy(recentBuilds = mappedBuilds) }
                }
        }
    }

    fun loadUserRepositories() {
        if (!gitService.hasSavedToken()) {
            _uiState.update { it.copy(userRepositories = emptyList(), isLoadingUserRepos = false) }
            return
        }
        viewModelScope.launch {
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
        viewModelScope.launch {
            preferences.setActiveProjectId(project.id)
        }
    }

    fun selectProjectById(id: Long) {
        val found = _uiState.value.projects.firstOrNull { it.id == id }
        if (found != null) {
            selectProject(found)
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
        val entity = projectDao.getProjectById(target.id)
        projectDao.deleteProjectById(target.id)
        withContext(Dispatchers.IO) {
            val localPath = entity?.path ?: target.localPath
            if (localPath.isNotBlank()) {
                val dir = File(localPath)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
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
        val slug = name.lowercase().replace(" ", "").replace("[^a-z0-9]".toRegex(), "")
        val autoPackage = if (slug.isNotBlank()) "com.$slug.app" else ""
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
        val appName = state.newAppName.trim().ifEmpty { "NouveauProjet" }
        val slug = appName.lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifEmpty { "projet-${System.currentTimeMillis()}" }
        val pkgName = state.newPackageName.trim().ifEmpty {
            "com.${slug.replace("-", "")}.app"
        }
        val projectDir = uniqueTargetDir(slug)

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

        val stats = withContext(Dispatchers.IO) {
            writeStarterFiles(
                projectDir = projectDir,
                appName = appName,
                packageName = pkgName,
                minSdk = state.selectedMinSdk,
                template = state.selectedTemplate
            )
            inspectDirectoryStats(projectDir)
        }

        val entity = ProjectEntity(
            name = appName,
            packageName = pkgName,
            path = projectDir.absolutePath,
            minSdk = state.selectedMinSdk,
            targetSdk = 35,
            gitBranch = "main",
            gitRepo = "",
            language = language,
            uiFramework = uiFramework,
            onGitHub = false
        )
        val newId = projectDao.insertProject(entity)
        preferences.setActiveProjectId(newId)

        val newProjectItem = mapEntityToItem(entity.copy(id = newId), stats, emptyList())
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
        if (tabIndex == 1 && _uiState.value.userRepositories.isEmpty() && gitService.hasSavedToken()) {
            loadUserRepositories()
        }
    }

    fun updateImportGitUrl(url: String) {
        _uiState.update { it.copy(importGitUrl = url, importError = null) }
    }

    fun updateImportGitBranch(branch: String) {
        _uiState.update { it.copy(importGitBranch = branch, importError = null) }
    }

    fun selectUserRepository(fullName: String, defaultBranch: String) {
        _uiState.update {
            it.copy(
                importGitUrl = "github.com/$fullName",
                importGitBranch = defaultBranch.ifBlank { "main" },
                importError = null
            )
        }
    }

    fun importZipFromUri(uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importError = null) }
            val displayName = resolveUriFileName(uri) ?: "archive.zip"
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Impossible d'ouvrir le fichier sélectionné.")
                    stream.use { importZipStream(it, displayName) }
                }
            }
            result.fold(
                onSuccess = { imported ->
                    _uiState.update { current ->
                        val updated = listOf(imported) + current.projects.filter { it.id != imported.id }
                        val filtered = filterProjectList(updated, current.searchQuery, current.selectedFilter)
                        current.copy(
                            projects = updated,
                            filteredProjects = filtered,
                            totalProjectCount = updated.size,
                            selectedProject = imported,
                            viewMode = ProjectsViewMode.DEFAULT,
                            isImporting = false,
                            importError = null,
                            recentZipImportName = displayName,
                            recentZipImportDesc = "${imported.sizeText} · importé à l'instant"
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importError = error.message ?: "Échec de l'extraction de l'archive .zip."
                        )
                    }
                }
            )
        }
    }

    suspend fun importZipFileSync(zipFile: File): ProjectItem {
        val imported = withContext(Dispatchers.IO) {
            FileInputStream(zipFile).use { fis ->
                importZipStream(fis, zipFile.name)
            }
        }
        _uiState.update { current ->
            val updated = listOf(imported) + current.projects.filter { it.id != imported.id }
            val filtered = filterProjectList(updated, current.searchQuery, current.selectedFilter)
            current.copy(
                projects = updated,
                filteredProjects = filtered,
                totalProjectCount = updated.size,
                selectedProject = imported,
                viewMode = ProjectsViewMode.DEFAULT
            )
        }
        return imported
    }

    private suspend fun importZipStream(inputStream: InputStream, archiveName: String): ProjectItem {
        val baseName = archiveName.removeSuffix(".zip").ifBlank { "Projet-Importe" }
        val targetDir = uniqueTargetDir(baseName)
        val canonicalTarget = targetDir.canonicalPath
        var extractedBytes = 0L
        val maxBytes = 200L * 1024L * 1024L

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (!outFile.canonicalPath.startsWith(canonicalTarget + File.separator) &&
                    outFile.canonicalPath != canonicalTarget
                ) {
                    throw SecurityException("Chemin d'archive non sécurisé : ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len = zis.read(buffer)
                        while (len > 0) {
                            extractedBytes += len
                            if (extractedBytes > maxBytes) {
                                throw IllegalStateException("L'archive dépasse la limite de 200 Mo.")
                            }
                            fos.write(buffer, 0, len)
                            len = zis.read(buffer)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }

        // Si l'archive contient un seul dossier racine, descendre dedans pour inspecter
        val rootChildren = targetDir.listFiles()?.toList().orEmpty()
        val effectiveDir = if (rootChildren.size == 1 && rootChildren.first().isDirectory) {
            rootChildren.first()
        } else {
            targetDir
        }

        val detected = detectProjectMetadata(effectiveDir, baseName)
        val stats = inspectDirectoryStats(effectiveDir)
        val entity = ProjectEntity(
            name = detected.name,
            packageName = detected.packageName,
            path = effectiveDir.absolutePath,
            minSdk = detected.minSdk,
            targetSdk = 35,
            gitBranch = "main",
            gitRepo = "",
            language = detected.language,
            uiFramework = detected.uiFramework,
            onGitHub = false
        )
        val newId = projectDao.insertProject(entity)
        preferences.setActiveProjectId(newId)
        return mapEntityToItem(entity.copy(id = newId), stats, emptyList())
    }

    suspend fun importGitSync(): ProjectItem? {
        val state = _uiState.value
        val repoUrl = state.importGitUrl.trim()
        val branch = state.importGitBranch.trim().ifEmpty { "main" }

        if (repoUrl.isBlank()) {
            _uiState.update { it.copy(importError = "Veuillez saisir l'URL ou le nom du dépôt (ex: utilisateur/depot).") }
            return null
        }

        _uiState.update { it.copy(isImporting = true, importError = null) }

        val repoName = repoUrl.trimEnd('/').substringAfterLast("/").removeSuffix(".git").ifEmpty { "Repo-Git" }
        val targetDir = uniqueTargetDir(repoName)

        return when (val cloneRes = gitService.cloneRepository(repoUrl, targetDir, branch)) {
            is GitOperationResult.Success -> {
                val detected = withContext(Dispatchers.IO) { detectProjectMetadata(targetDir, repoName) }
                val stats = withContext(Dispatchers.IO) { inspectDirectoryStats(targetDir) }
                val branchStatus = gitService.getBranchStatus(targetDir, fetchRemote = false)
                val actualBranch = (branchStatus as? GitOperationResult.Success)?.data?.currentBranch ?: branch
                val parsedPair = GitHubRepositoryService.parseOwnerAndRepo(repoUrl)
                val normalizedGitRepo = parsedPair?.let { "${it.first}/${it.second}" } ?: repoUrl

                val entity = ProjectEntity(
                    name = detected.name,
                    packageName = detected.packageName,
                    path = targetDir.absolutePath,
                    minSdk = detected.minSdk,
                    targetSdk = 35,
                    gitBranch = actualBranch,
                    gitRepo = normalizedGitRepo,
                    language = detected.language,
                    uiFramework = detected.uiFramework,
                    onGitHub = true
                )
                val newId = projectDao.insertProject(entity)
                preferences.setActiveProjectId(newId)

                val importedItem = mapEntityToItem(entity.copy(id = newId), stats, emptyList())
                _uiState.update { current ->
                    val updated = listOf(importedItem) + current.projects.filter { it.id != newId }
                    val filtered = filterProjectList(updated, current.searchQuery, current.selectedFilter)
                    current.copy(
                        projects = updated,
                        filteredProjects = filtered,
                        totalProjectCount = updated.size,
                        selectedProject = importedItem,
                        viewMode = ProjectsViewMode.DEFAULT,
                        isImporting = false,
                        importError = null
                    )
                }
                importedItem
            }
            is GitOperationResult.Error -> {
                withContext(Dispatchers.IO) {
                    if (targetDir.exists()) targetDir.deleteRecursively()
                }
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importError = cloneRes.message
                    )
                }
                null
            }
            is GitOperationResult.Conflict -> {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importError = "Conflit détecté lors du clonage."
                    )
                }
                null
            }
        }
    }

    fun importGit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val imported = importGitSync()
            if (imported != null) {
                onSuccess()
            }
        }
    }

    private fun resolveUriFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun uniqueTargetDir(baseSlug: String): File {
        val safe = baseSlug.lowercase().replace("[^a-z0-9_-]".toRegex(), "-").trim('-').ifEmpty { "project" }
        var candidate = File(projectsRootDir(), safe)
        var counter = 2
        while (candidate.exists()) {
            candidate = File(projectsRootDir(), "${safe}_$counter")
            counter++
        }
        return candidate
    }

    private data class DirStats(val fileCount: Int, val totalBytes: Long)

    private data class DetectedMetadata(
        val name: String,
        val packageName: String,
        val language: String,
        val uiFramework: String,
        val minSdk: Int
    )

    private fun inspectDirectoryStats(dir: File): DirStats {
        if (!dir.exists()) return DirStats(0, 0L)
        var count = 0
        var bytes = 0L
        dir.walkTopDown()
            .onEnter { it.name != ".git" && it.name != "build" && it.name != ".gradle" }
            .forEach { file ->
                if (file.isFile) {
                    count++
                    bytes += file.length()
                }
            }
        return DirStats(count, bytes)
    }

    private fun detectProjectMetadata(dir: File, fallbackName: String): DetectedMetadata {
        var detectedPackage = "com.${fallbackName.lowercase().replace("[^a-z0-9]".toRegex(), "").ifEmpty { "nexcode" }}.app"
        var detectedMinSdk = 26
        var hasFlutter = File(dir, "pubspec.yaml").exists()
        var hasCompose = false
        var hasJava = false
        var hasKotlin = false

        val appGradleKts = File(dir, "app/build.gradle.kts")
        val appGradleGroovy = File(dir, "app/build.gradle")
        val gradleText = when {
            appGradleKts.isFile -> runCatching { appGradleKts.readText() }.getOrDefault("")
            appGradleGroovy.isFile -> runCatching { appGradleGroovy.readText() }.getOrDefault("")
            else -> ""
        }
        if (gradleText.isNotBlank()) {
            Regex("""applicationId\s*=?\s*["']([^"']+)["']""").find(gradleText)?.let {
                detectedPackage = it.groupValues[1]
            }
            Regex("""minSdk\s*=?\s*(\d+)""").find(gradleText)?.let {
                detectedMinSdk = it.groupValues[1].toIntOrNull() ?: 26
            }
            if (gradleText.contains("compose", ignoreCase = true)) {
                hasCompose = true
            }
        }

        val manifest = File(dir, "app/src/main/AndroidManifest.xml")
        if (manifest.isFile) {
            val manifestText = runCatching { manifest.readText() }.getOrDefault("")
            Regex("""package=["']([^"']+)["']""").find(manifestText)?.let {
                if (detectedPackage.startsWith("com.") && gradleText.isBlank()) {
                    detectedPackage = it.groupValues[1]
                }
            }
        }

        dir.walkTopDown()
            .onEnter { it.name != ".git" && it.name != "build" }
            .take(200)
            .forEach { file ->
                when (file.extension.lowercase()) {
                    "kt", "kts" -> hasKotlin = true
                    "java" -> hasJava = true
                    "dart" -> hasFlutter = true
                }
            }

        val language = when {
            hasFlutter -> "Flutter"
            hasKotlin -> "Kotlin"
            hasJava -> "Java"
            else -> "Kotlin"
        }
        val uiFramework = when {
            hasFlutter -> "Dart"
            hasCompose -> "Compose"
            else -> "XML"
        }

        return DetectedMetadata(
            name = fallbackName,
            packageName = detectedPackage,
            language = language,
            uiFramework = uiFramework,
            minSdk = detectedMinSdk
        )
    }

    private fun writeStarterFiles(
        projectDir: File,
        appName: String,
        packageName: String,
        minSdk: Int,
        template: ProjectTemplateType
    ) {
        projectDir.mkdirs()
        val packagePath = packageName.replace('.', '/')
        val srcDir = File(projectDir, "app/src/main/java/$packagePath").apply { mkdirs() }
        val resValuesDir = File(projectDir, "app/src/main/res/values").apply { mkdirs() }

        File(projectDir, "settings.gradle.kts").writeText(
            "rootProject.name = \"$appName\"\ninclude(\":app\")\n"
        )
        File(projectDir, "app/build.gradle.kts").writeText(
            """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            }

            android {
                namespace = "$packageName"
                compileSdk = 35
                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = $minSdk
                    targetSdk = 35
                    versionCode = 1
                    versionName = "0.1.0"
                }
            }
            """.trimIndent()
        )
        File(projectDir, "app/src/main/AndroidManifest.xml").writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:label="$appName" />
            </manifest>
            """.trimIndent()
        )
        File(resValuesDir, "strings.xml").writeText(
            "<resources>\n    <string name=\"app_name\">$appName</string>\n</resources>\n"
        )
        if (template == ProjectTemplateType.JAVA) {
            File(srcDir, "MainActivity.java").writeText(
                "package $packageName;\n\nimport android.app.Activity;\n\npublic class MainActivity extends Activity {}\n"
            )
        } else {
            File(srcDir, "MainActivity.kt").writeText(
                "package $packageName\n\nimport android.app.Activity\n\nclass MainActivity : Activity()\n"
            )
        }
    }

    private fun mapEntityToItem(
        entity: ProjectEntity,
        stats: DirStats,
        builds: List<BuildEntity>
    ): ProjectItem {
        val initials = entity.name.split(" ", "-", "_")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "NX" }

        val latestBuild = builds.maxByOrNull { it.startTime }
        val (buildStatus, buildStatusLabel) = when (latestBuild?.status) {
            "SUCCESS" -> ProjectBuildStatus.SUCCESS to "Build réussi"
            "FAILED" -> ProjectBuildStatus.FAILED to "Build échoué"
            "RUNNING" -> ProjectBuildStatus.RUNNING to "Compilation en cours"
            else -> ProjectBuildStatus.DRAFT to "Brouillon · jamais compilé"
        }

        val syncText = if (entity.onGitHub || entity.gitRepo.isNotBlank()) {
            "branche ${entity.gitBranch} · ${formatRelativeTime(entity.updatedAt)}"
        } else {
            "non synchronisé"
        }

        return ProjectItem(
            id = entity.id,
            name = entity.name,
            packageName = entity.packageName,
            initials = initials,
            language = entity.language,
            uiFramework = entity.uiFramework,
            modifiedText = "modifié ${formatRelativeTime(entity.updatedAt)}",
            buildStatus = buildStatus,
            buildStatusLabel = buildStatusLabel,
            version = latestBuild?.versionName?.let { "v$it" },
            onGitHub = entity.onGitHub || entity.gitRepo.isNotBlank(),
            fileCount = stats.fileCount,
            sizeText = formatFrenchSize(stats.totalBytes),
            buildCount = builds.size,
            gitRepo = entity.gitRepo,
            gitBranch = entity.gitBranch,
            gitSyncText = syncText,
            minSdk = entity.minSdk,
            localPath = entity.path
        )
    }

    private fun formatRelativeTime(timestampMs: Long): String {
        val diffSec = ((System.currentTimeMillis() - timestampMs) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 60L -> "à l'instant"
            diffSec < 3600L -> "il y a ${diffSec / 60L} min"
            diffSec < 86400L -> "il y a ${diffSec / 3600L} h"
            diffSec < 172800L -> "hier"
            else -> "il y a ${diffSec / 86400L} j"
        }
    }

    private fun formatFrenchSize(bytes: Long): String {
        if (bytes <= 0L) return "0 Mo"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.FRANCE, "%.1f Go", gb)
            mb >= 0.1 -> String.format(Locale.FRANCE, "%.1f Mo", mb)
            else -> String.format(Locale.FRANCE, "%.0f Ko", kb.coerceAtLeast(1.0))
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
}
