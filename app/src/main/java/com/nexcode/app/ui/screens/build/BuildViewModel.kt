package com.nexcode.app.ui.screens.build

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.github.repository.GitHubResult
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.dao.BuildDao
import com.nexcode.app.data.local.entity.BuildEntity
import com.nexcode.app.data.github.model.WorkflowRun
import com.nexcode.app.ui.screens.projects.ProjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel des écrans de build (21 à 24), branché sur le pipeline réel :
 * le projet est déclenché via GitHub Actions (workflow_dispatch), les étapes
 * proviennent des jobs du run, les logs de l'archive de logs, et les APK des
 * artefacts téléchargés. Aucune progression n'est simulée : tout ce qui est
 * affiché vient de l'API GitHub ou du disque.
 *
 * [context] reste nullable pour les tests JVM ; [pollDelayMs] est injectable
 * pour des tests rapides ; [overrideGitService] permet d'injecter un faux
 * GitHubApiService.
 */
class BuildViewModel(
    context: Context? = null,
    overrideGitService: GitHubRepositoryService? = null,
    private val pollDelayMs: Long = 15_000L
) : ViewModel() {

    private val appContext: Context? = context?.applicationContext
    private val gitService: GitHubRepositoryService? =
        overrideGitService ?: appContext?.let { GitHubRepositoryService(it) }
    private val buildDao: BuildDao? = appContext?.let {
        try { NexcodeDatabase.getInstance(it).buildDao() } catch (_: Exception) { null }
    }

    private val _uiState = MutableStateFlow(BuildUiState())
    val uiState: StateFlow<BuildUiState> = _uiState.asStateFlow()

    // Projet réellement lié (owner/repo + chemin disque)
    private var owner: String = ""
    private var repo: String = ""
    private var branch: String = "main"
    private var projectId: Long? = null
    private var projectDir: File? = null

    private var buildJob: Job? = null
    private var activeRunId: Long? = null
    private var buildStartMs: Long = 0

    // Écran 21 : liaison au projet réel

    fun bindProject(project: ProjectItem?) {
        buildJob?.cancel()
        buildJob = null
        if (project == null) {
            owner = ""; repo = ""; projectId = null; projectDir = null
            _uiState.update {
                BuildUiState(
                    configNotice = "Aucun projet ouvert. Ouvrez un projet pour configurer son build.",
                    keystoreInfo = "",
                    versionName = "",
                    buildNumber = ""
                )
            }
            return
        }
        projectId = project.id
        projectDir = project.localPath.takeIf { it.isNotBlank() }?.let { File(it) }
        val parsed = parseOwnerAndRepo(project.gitRepo)
        branch = project.gitBranch.ifBlank { "main" }
        owner = parsed?.first ?: ""
        repo = parsed?.second ?: ""

        val version = readProjectVersion(projectDir)
        val keystore = readKeystoreInfo(projectDir)
        val githubRepoDisplay = parsed?.let { "$owner/$repo" } ?: ""
        val notice = when {
            parsed == null -> "Ce projet n'est pas connecté à un dépôt GitHub. " +
                "Le build utilise GitHub Actions : importez le projet depuis GitHub ou poussez-le d'abord."
            else -> null
        }
        _uiState.update {
            BuildUiState(
                projectName = project.name,
                versionName = version.first,
                buildNumber = version.second,
                githubRepo = githubRepoDisplay,
                isGitHubReady = parsed != null,
                keystoreInfo = keystore,
                selectedVariant = it.selectedVariant,
                canStartBuild = parsed != null && gitService != null,
                configNotice = notice
            )
        }
    }

    fun setVariant(variant: BuildVariant) {
        _uiState.update { it.copy(selectedVariant = variant) }
    }

    // Écran 22 : build réel via GitHub Actions

    fun startCompilation(onStarted: () -> Unit = {}) {
        val state = _uiState.value
        if (buildJob?.isActive == true) return
        if (!state.canStartBuild || gitService == null || owner.isBlank()) {
            _uiState.update {
                it.copy(configNotice = "Build impossible : " + (it.configNotice ?: "aucun dépôt GitHub lié."))
            }
            return
        }
        buildStartMs = System.currentTimeMillis()
        val variantValue = state.selectedVariant.name.lowercase()
        _uiState.update {
            it.copy(
                compilationStatus = CompilationStatus.RUNNING,
                progressPercent = 0,
                steps = listOf(
                    BuildStep(
                        id = "dispatch",
                        label = "Déclenchement du workflow GitHub Actions",
                        detail = "$owner/$repo · branche $branch",
                        status = BuildStepStatus.RUNNING
                    )
                ),
                buildLogs = emptyList(),
                apkList = emptyList(),
                configNotice = null
            )
        }
        onStarted()
        buildJob = viewModelScope.launch(Dispatchers.IO) {
            runRealBuild(variantValue)
        }
    }

    fun cancelCompilation() {
        buildJob?.cancel()
        buildJob = null
        val runId = activeRunId
        if (runId != null && gitService != null) {
            viewModelScope.launch(Dispatchers.IO) {
                gitService.cancelWorkflowRun(owner, repo, runId)
            }
        }
        viewModelScope.launch(Dispatchers.IO) { persistBuild("CANCELLED") }
        _uiState.update {
            it.copy(
                compilationStatus = CompilationStatus.CANCELLED,
                steps = it.steps.map { step ->
                    if (step.status == BuildStepStatus.RUNNING) step.copy(status = BuildStepStatus.PENDING) else step
                }
            )
        }
    }

    private suspend fun runRealBuild(variantValue: String) {
        val service = gitService ?: return
        // Numéro de run de référence pour identifier le run créé par notre dispatch.
        val baseRunNumber = when (val runs = service.getWorkflowRuns(owner, repo)) {
            is com.nexcode.app.data.github.repository.GitHubResult.Success ->
                runs.data.maxOfOrNull { it.runNumber } ?: 0
            else -> 0
        }
        persistBuild("RUNNING")
        val triggered = service.triggerBuildWorkflow(
            owner = owner,
            repo = repo,
            workflowFileName = "build.yml",
            branch = branch,
            inputs = mapOf("build_variant" to variantValue)
        )
        if (triggered is GitHubResult.Error) {
            failBuild(triggered.message)
            return
        }

        var run: WorkflowRun? = null
        while (true) {
            if (buildJob?.isActive != true) return
            when (val runs = service.getWorkflowRuns(owner, repo)) {
                is GitHubResult.Success -> {
                    val target = runs.data
                        .filter { it.runNumber > baseRunNumber }
                        .minByOrNull { it.runNumber }
                    if (target != null) {
                        run = target
                        activeRunId = target.id
                        when (target.status) {
                            "completed" -> {
                                val conclusion = target.conclusion
                                when (conclusion) {
                                    "success" -> succeedBuild(target, service)
                                    "cancelled" -> {
                                        persistBuild("CANCELLED")
                                        _uiState.update { it.copy(compilationStatus = CompilationStatus.CANCELLED) }
                                    }
                                    else -> failBuildFromRun(target, service)
                                }
                                return
                            }
                            else -> {
                                val refreshedSteps = loadSteps(service, target.id)
                                updateRunningState(target, refreshedSteps)
                            }
                        }
                    }
                }
                is GitHubResult.Error -> {
                    // Erreur transitoire réseau : on retente au prochain cycle.
                    _uiState.update {
                        it.copy(
                            steps = it.steps.map { s ->
                                if (s.id == "dispatch" && s.status == BuildStepStatus.RUNNING)
                                    s.copy(detail = "Reconnexion… (${s.detail ?: ""})") else s
                            }
                        )
                    }
                }
                else -> Unit
            }
            delay(pollDelayMs)
        }
    }

    private suspend fun loadSteps(
        service: GitHubRepositoryService,
        runId: Long
    ): List<BuildStep> = when (val jobs = service.getWorkflowRunJobs(owner, repo, runId)) {
        is GitHubResult.Success -> jobs.data.flatMap { job ->
            job.steps.map { step ->
                val status = when {
                    step.status == "queued" -> BuildStepStatus.PENDING
                    step.status == "in_progress" -> BuildStepStatus.RUNNING
                    else -> when (step.conclusion) {
                        "success" -> BuildStepStatus.SUCCESS
                        "failure" -> BuildStepStatus.FAILED
                        "skipped" -> BuildStepStatus.PENDING
                        else -> BuildStepStatus.PENDING
                    }
                }
                BuildStep(
                    id = "step_${step.number}",
                    label = step.name,
                    detail = formatStepDuration(step.startedAt, step.completedAt),
                    status = status
                )
            }
        }
        else -> emptyList()
    }

    private fun updateRunningState(run: WorkflowRun, steps: List<BuildStep>) {
        _uiState.update { current ->
            val mergedSteps = if (steps.isEmpty()) current.steps else current.steps
                .filter { it.id == "dispatch" } + steps
            val completed = mergedSteps.count { it.status == BuildStepStatus.SUCCESS }
            val percent = if (mergedSteps.isEmpty()) 0
            else (completed * 100) / mergedSteps.size
            current.copy(
                progressPercent = percent.coerceIn(0, 99),
                steps = mergedSteps,
                runUrl = run.htmlUrl
            )
        }
    }

    private suspend fun succeedBuild(run: WorkflowRun, service: GitHubRepositoryService) {
        val artifacts = service.getArtifacts(owner, repo, run.id)
        val apkFiles = mutableListOf<File>()
        var artifactName = ""
        if (artifacts is GitHubResult.Success) {
            val target = artifacts.data.firstOrNull { it.name.contains("apk", ignoreCase = true) }
                ?: artifacts.data.firstOrNull()
            if (target != null && appContext != null) {
                artifactName = target.name
                val destDir = File(appContext.filesDir, "apks")
                when (val downloaded = service.downloadArtifactApks(owner, repo, target.id, destDir)) {
                    is GitHubResult.Success ->
                        apkFiles.addAll(downloaded.data)
                    else -> Unit
                }
            }
        }
        val durationMs = System.currentTimeMillis() - buildStartMs
        val apkItems = apkFiles.map { file ->
            ApkItem(
                name = file.name,
                description = "${artifactName.ifBlank { "artefact" }} · ${formatBytes(file.length())}",
                path = file.absolutePath,
                sizeBytes = file.length()
            )
        }
        _uiState.update {
            it.copy(
                compilationStatus = CompilationStatus.SUCCESS,
                progressPercent = 100,
                runUrl = run.htmlUrl,
                apkList = apkItems,
                workflowNotice = "L'installation hors Play Store demande l'autorisation « sources inconnues ». " +
                    "Un avertissement Play Protect peut apparaître — il est normal pour un APK signé par vous.",
                steps = it.steps.map { s ->
                    if (s.status == BuildStepStatus.RUNNING) s.copy(status = BuildStepStatus.SUCCESS) else s
                }
            )
        }
        persistBuild("SUCCESS", durationMs = durationMs, apkPath = apkFiles.firstOrNull()?.absolutePath)
    }

    private suspend fun failBuildFromRun(run: WorkflowRun, service: GitHubRepositoryService) {
        val logs = when (val result = service.getWorkflowRunLogs(owner, repo, run.id)) {
            is GitHubResult.Success -> result.data
            else -> ""
        }
        val parsed = parseBuildLogs(logs)
        val errorLines = parsed.filter { it.level == BuildLogLevel.ERROR }
        val summary = errorLines.take(5).joinToString("\n") { "• ${it.message}" }
            .ifBlank { "Le run GitHub Actions s'est terminé avec l'état « ${run.conclusion ?: "failure"} »." }
        _uiState.update {
            it.copy(
                compilationStatus = CompilationStatus.FAILED,
                buildLogs = parsed,
                runUrl = run.htmlUrl,
                steps = it.steps.map { s ->
                    if (s.status == BuildStepStatus.RUNNING) s.copy(status = BuildStepStatus.FAILED) else s
                }
            )
        }
        persistBuild("FAILED", errorCount = errorLines.size, errorSummary = summary, logOutput = logs)
    }

    private suspend fun failBuild(message: String) {
        persistBuild(
            "FAILED",
            errorCount = 1,
            errorSummary = message,
            logOutput = message
        )
        _uiState.update {
            it.copy(
                compilationStatus = CompilationStatus.FAILED,
                buildLogs = listOf(
                    BuildLogEntry(BuildLogLevel.ERROR, "Workflow", message, nowTime())
                ),
                steps = it.steps.map { s ->
                    if (s.status == BuildStepStatus.RUNNING) s.copy(status = BuildStepStatus.FAILED) else s
                }
            )
        }
    }

    // Écran 23 : logs réels

    fun setLogFilter(filter: String) {
        _uiState.update { it.copy(selectedLogFilter = filter) }
    }

    // Écran 24 : notices

    fun dismissNotice() {
        _uiState.update { it.copy(workflowNotice = null) }
    }

    // Persistance réelle de l'historique (Room)

    private suspend fun persistBuild(
        status: String,
        durationMs: Long = 0,
        apkPath: String? = null,
        errorCount: Int = 0,
        errorSummary: String? = null,
        logOutput: String? = null
    ) {
        val dao = buildDao ?: return
        val pid = projectId ?: return
        val state = _uiState.value
        try {
            dao.insertBuild(
                BuildEntity(
                    projectId = pid,
                    buildType = state.selectedVariant.name.lowercase(),
                    status = status,
                    startTime = buildStartMs,
                    durationMs = durationMs,
                    apkPath = apkPath,
                    apkSize = state.apkList.firstOrNull()?.sizeBytes ?: 0,
                    logOutput = logOutput?.take(100_000),
                    errorCount = errorCount,
                    githubRunId = activeRunId,
                    runNumber = 0,
                    versionName = state.versionName.ifBlank { "1.0" },
                    errorSummary = errorSummary
                )
            )
        } catch (_: Exception) {
            // La persistance ne doit jamais faire échouer le build lui-même.
        }
    }

    // Utilitaires réels

    private fun parseOwnerAndRepo(gitRepo: String): Pair<String, String>? {
        val cleaned = gitRepo.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("git@github.com:")
            .removePrefix("github.com/")
            .removeSuffix(".git")
        val parts = cleaned.split("/").filter { it.isNotBlank() }
        return if (parts.size >= 2) parts[0] to parts[1] else null
    }

    private fun readProjectVersion(projectDir: File?): Pair<String, String> {
        val gradleFile = projectDir?.let { dir ->
            sequenceOf("app/build.gradle.kts", "app/build.gradle", "build.gradle.kts", "build.gradle")
                .map { File(dir, it) }
                .firstOrNull { it.isFile }
        } ?: return "" to ""
        return try {
            val content = gradleFile.readText()
            val versionName = Regex("versionName\\s*=\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1) ?: ""
            val versionCode = Regex("versionCode\\s*=\\s*(\\d+)").find(content)?.groupValues?.get(1) ?: ""
            versionName to versionCode
        } catch (_: Exception) {
            "" to ""
        }
    }

    private fun readKeystoreInfo(projectDir: File?): String {
        val dir = projectDir ?: return ""
        return when {
            File(dir, "debug.keystore").isFile -> "debug.keystore · présent à la racine du dépôt"
            File(dir, "my-upload-key.jks").isFile -> "my-upload-key.jks · présent à la racine du dépôt"
            else -> "Aucun keystore détecté à la racine du dépôt"
        }
    }

    internal fun parseBuildLogs(raw: String): List<BuildLogEntry> {
        if (raw.isBlank()) return emptyList()
        val entries = mutableListOf<BuildLogEntry>()
        var currentTag = "GitHub Actions"
        raw.lines().forEach { line ->
            val trimmed = line.trimEnd()
            if (trimmed.isBlank()) return@forEach
            if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                currentTag = trimmed.removePrefix("===").removeSuffix("===").trim()
                return@forEach
            }
            val level = when {
                trimmed.contains("##[error]") || trimmed.contains(" e: ") ||
                    trimmed.contains("FAILURE:") || trimmed.contains("error:") ||
                    trimmed.endsWith("FAILED") -> BuildLogLevel.ERROR
                trimmed.contains("##[warning]") || trimmed.contains(" w: ") ||
                    trimmed.contains("warning:") -> BuildLogLevel.WARNING
                else -> BuildLogLevel.INFO
            }
            val time = Regex("(\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?Z?").find(trimmed)?.groupValues?.get(1) ?: ""
            val message = trimmed
                .replace("##[error]", "")
                .replace("##[warning]", "")
                .trim()
            entries.add(BuildLogEntry(level, currentTag, message, time))
        }
        return entries
    }

    internal fun formatStepDuration(startedAt: String?, completedAt: String?): String? {
        if (startedAt.isNullOrBlank() || completedAt.isNullOrBlank()) return null
        return try {
            val start = java.time.OffsetDateTime.parse(startedAt)
            val end = java.time.OffsetDateTime.parse(completedAt)
            val seconds = java.time.Duration.between(start, end).seconds
            formatDuration(seconds * 1000)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> String.format(java.util.Locale.FRENCH, "%.1f Mo", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format(java.util.Locale.FRENCH, "%.0f Ko", bytes / 1024.0)
        else -> "$bytes o"
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) String.format(java.util.Locale.FRENCH, "%d min %02d s", minutes, seconds)
        else String.format(java.util.Locale.FRENCH, "%.1f s", ms / 1000.0)
    }

    private fun nowTime(): String =
        java.time.LocalTime.now().withNano(0).toString()
}
