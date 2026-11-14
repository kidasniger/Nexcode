package com.nexcode.app.ui.screens.git

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.git.GitOperationResult
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.preferences.NexcodeSecureVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class GitViewModel(
    context: Context? = null,
    overrideGitService: GitHubRepositoryService? = null
) : ViewModel() {

    private val gitService = overrideGitService ?: context?.let { GitHubRepositoryService(it) }
    private val vault = context?.let { NexcodeSecureVault(it) }

    // Chemin réel du projet actif : alimenté par bindProjectAndRefreshSync (aucun chemin codé en dur)
    private var activeProjectDir: File? = null

    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshGitStatusSync(fetchRemote = false)
        }
    }

    suspend fun bindProjectAndRefreshSync(
        projectName: String,
        fullName: String,
        localPath: String
    ) {
        activeProjectDir = localPath.takeIf { it.isNotBlank() }?.let { File(it) }
        val displayRepo = fullName.replace("/", " / ")
        _uiState.update {
            it.copy(
                repoFullName = displayRepo.ifBlank { projectName },
                viewMode = GitViewMode.DEFAULT
            )
        }
        refreshGitStatusSync(fetchRemote = false)
    }

    fun updateCommitMessage(message: String) {
        _uiState.update { it.copy(commitMessage = message) }
    }

    fun setViewMode(mode: GitViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    suspend fun commitAndPushSync() {
        val projectDir = activeProjectDir
        val msg = _uiState.value.commitMessage.ifBlank { "Mise à jour NEXCODE" }

        if (gitService != null && projectDir != null && projectDir.exists() && File(projectDir, ".git").exists()) {
            _uiState.update { it.copy(isBusy = true, userNotice = "Commit et push en cours avec JGit…") }
            when (val res = gitService.commitAndPush(projectDir, msg)) {
                is GitOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            viewMode = GitViewMode.DEFAULT,
                            commitMessage = "",
                            branchStatusText = "À jour avec origin/${it.currentBranch} · à l'instant",
                            userNotice = "Commit créé et poussé sur origin/${it.currentBranch} avec succès."
                        )
                    }
                    refreshGitStatusSync(fetchRemote = false)
                }
                is GitOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            viewMode = GitViewMode.ERROR,
                            errorTitle = "Échec du push.",
                            errorDescription = res.message,
                            userNotice = "Échec du push : ${res.message}"
                        )
                    }
                }
                else -> _uiState.update { it.copy(isBusy = false) }
            }
        } else {
            val token = vault?.getGitToken()
            _uiState.update {
                if (token.isNullOrBlank() && vault != null) {
                    it.copy(
                        viewMode = GitViewMode.ERROR,
                        errorTitle = "Échec du push.",
                        errorDescription = "Jeton GitHub non configuré. Connectez-vous dans les Réglages.",
                        userNotice = "Jeton GitHub non configuré. Veuillez vous connecter dans les Réglages."
                    )
                } else {
                    it.copy(
                        userNotice = "Aucun dépôt Git initialisé sur le disque."
                    )
                }
            }
        }
    }

    fun commitAndPush() {
        viewModelScope.launch {
            commitAndPushSync()
        }
    }

    suspend fun pullRepositorySync(onConflictFound: () -> Unit) {
        val projectDir = activeProjectDir
        if (gitService != null && projectDir != null && projectDir.exists() && File(projectDir, ".git").exists()) {
            _uiState.update { it.copy(isBusy = true, userNotice = "Synchronisation pull en cours…") }
            when (val res = gitService.pull(projectDir)) {
                is GitOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            viewMode = GitViewMode.DEFAULT,
                            branchStatusText = "À jour avec origin/${it.currentBranch} · à l'instant",
                            userNotice = "Dépôt mis à jour avec succès (${res.data})."
                        )
                    }
                    refreshGitStatusSync(fetchRemote = false)
                }
                is GitOperationResult.Conflict -> {
                    val parsed = gitService.parseMergeConflicts(projectDir)
                    val conflictItems = if (parsed is GitOperationResult.Success) {
                        parsed.data.map { hunk ->
                            ConflictItem(
                                id = hunk.id,
                                fileAndLine = "${hunk.fileName}:${hunk.startLine}",
                                localLines = hunk.localLines,
                                remoteLines = hunk.remoteLines,
                                selectedChoice = ConflictChoice.LOCAL
                            )
                        }
                    } else {
                        emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            conflicts = conflictItems,
                            userNotice = "${res.conflictedFiles.size} conflit(s) détecté(s)."
                        )
                    }
                    onConflictFound()
                }
                is GitOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            viewMode = GitViewMode.ERROR,
                            userNotice = "Erreur réseau ou git : ${res.message}"
                        )
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(userNotice = "Aucun dépôt Git local")
            }
        }
    }

    fun pullRepository(onConflictFound: () -> Unit) {
        viewModelScope.launch {
            pullRepositorySync(onConflictFound)
        }
    }

    fun retryPush() {
        viewModelScope.launch {
            commitAndPushSync()
        }
    }

    fun selectFilterBranch(branch: String) {
        _uiState.update { it.copy(selectedFilterBranch = branch) }
        viewModelScope.launch {
            refreshCommitHistory(_uiState.value.commitLoadLimit)
        }
    }

    fun loadMoreCommits() {
        viewModelScope.launch {
            val nextLimit = _uiState.value.commitLoadLimit + 20
            _uiState.update { it.copy(commitLoadLimit = nextLimit) }
            refreshCommitHistory(limit = nextLimit)
        }
    }

    fun chooseConflictResolution(conflictId: String, choice: ConflictChoice) {
        _uiState.update { current ->
            val updated = current.conflicts.map {
                if (it.id == conflictId) it.copy(selectedChoice = choice) else it
            }
            current.copy(conflicts = updated)
        }
    }

    fun previewSmartMerge() {
        // Aucun calcul de fusion intelligente n'existe : notice honnête au lieu d'un faux aperçu
        _uiState.update {
            it.copy(userNotice = "Fusion intelligente non disponible : résolvez les conflits manuellement.")
        }
    }

    suspend fun applyConflictResolutionsSync(): Boolean {
        val service = gitService ?: return false
        val projectDir = activeProjectDir
        if (projectDir == null) {
            _uiState.update {
                it.copy(userNotice = "Aucun dépôt Git local")
            }
            return false
        }
        val map = _uiState.value.conflicts.associate { item ->
            item.id to (item.selectedChoice == ConflictChoice.LOCAL)
        }
        return when (val res = service.resolveConflictsAndCommit(projectDir, map)) {
            is GitOperationResult.Success -> {
                _uiState.update {
                    it.copy(
                        viewMode = GitViewMode.DEFAULT,
                        conflicts = emptyList(),
                        userNotice = "Conflits résolus et merge validé avec succès."
                    )
                }
                refreshGitStatusSync(fetchRemote = false)
                true
            }
            is GitOperationResult.Error -> {
                _uiState.update {
                    it.copy(userNotice = res.message)
                }
                false
            }
            else -> false
        }
    }

    fun applyConflictResolutions(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (applyConflictResolutionsSync()) {
                onSuccess()
            }
        }
    }

    suspend fun refreshGitStatusSync(fetchRemote: Boolean = false) {
        val projectDir = activeProjectDir
        val service = gitService ?: return
        if (projectDir == null || !projectDir.exists() || !File(projectDir, ".git").exists()) {
            _uiState.update { it.copy(hasGitRepo = false) }
            return
        }

        _uiState.update { it.copy(isBusy = true, hasGitRepo = true) }

        when (val statusRes = service.getBranchStatus(projectDir, fetchRemote)) {
            is GitOperationResult.Success -> {
                val status = statusRes.data
                _uiState.update {
                    it.copy(
                        hasGitRepo = true,
                        currentBranch = status.currentBranch,
                        historyBranch = status.currentBranch,
                        selectedFilterBranch = status.currentBranch,
                        availableBranches = status.branches,
                        aheadBy = status.aheadCount,
                        behindBy = status.behindCount,
                        branchStatusText = if (status.aheadCount == 0 && status.behindCount == 0) {
                            "À jour avec origin/${status.currentBranch}"
                        } else {
                            "${status.aheadCount} en avance · ${status.behindCount} en retard"
                        }
                    )
                }
            }
            is GitOperationResult.Error -> {
                if (fetchRemote) {
                    _uiState.update {
                        it.copy(
                            viewMode = GitViewMode.ERROR,
                            errorTitle = "Échec de la synchronisation.",
                            errorDescription = statusRes.message,
                            userNotice = statusRes.message
                        )
                    }
                }
            }
            else -> Unit
        }

        when (val modRes = service.getModifiedFilesWithDiff(projectDir)) {
            is GitOperationResult.Success -> {
                val mapped = modRes.data.map { file ->
                    ModifiedFile(
                        name = file.fileName,
                        added = file.addedLines,
                        removed = file.removedLines
                    )
                }
                _uiState.update { it.copy(modifiedFiles = mapped) }
            }
            else -> Unit
        }

        when (val unpushedRes = service.getUnpushedCommits(projectDir)) {
            is GitOperationResult.Success -> {
                val mapped = unpushedRes.data.map { commit ->
                    UnpushedCommit(
                        hash = commit.shortHash,
                        scope = commit.message
                    )
                }
                _uiState.update { it.copy(unpushedCommits = mapped) }
            }
            else -> Unit
        }

        refreshCommitHistory(_uiState.value.commitLoadLimit)
        _uiState.update { it.copy(isBusy = false) }
    }

    private suspend fun refreshCommitHistory(limit: Int = 20) {
        val service = gitService ?: return
        val projectDir = activeProjectDir ?: return
        if (!projectDir.exists() || !File(projectDir, ".git").exists()) return

        // Filtre réel : branche courante uniquement, ou toutes les branches du dépôt
        val allBranches = _uiState.value.selectedFilterBranch == "all"
        when (val result = service.getCommitHistory(projectDir, limit, allBranches)) {
            is GitOperationResult.Success -> {
                val realCommits = result.data.mapIndexed { index, commit ->
                    GitCommit(
                        id = "commit_${commit.hash}",
                        hash = commit.shortHash,
                        message = commit.message,
                        author = commit.author,
                        date = commit.timeAgo,
                        isAccentIcon = index == 0
                    )
                }
                _uiState.update {
                    it.copy(
                        commits = realCommits,
                        totalCommitsCount = realCommits.size
                    )
                }
            }
            else -> Unit
        }
    }

    fun refreshGitStatus(fetchRemote: Boolean = false) {
        viewModelScope.launch {
            refreshGitStatusSync(fetchRemote)
        }
    }

    fun switchBranch(branch: String) {
        viewModelScope.launch {
            val projectDir = activeProjectDir
            if (gitService != null && projectDir != null && projectDir.exists() && File(projectDir, ".git").exists()) {
                _uiState.update { it.copy(isBusy = true) }
                when (val res = gitService.checkoutBranch(projectDir, branch)) {
                    is GitOperationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                currentBranch = branch,
                                isBusy = false,
                                userNotice = "Branche changée vers $branch"
                            )
                        }
                        refreshGitStatusSync(fetchRemote = false)
                    }
                    is GitOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                userNotice = "Impossible de changer de branche : ${res.message}"
                            )
                        }
                    }
                    else -> _uiState.update { it.copy(isBusy = false) }
                }
            } else {
                // Aucun dépôt réel : échec honnête, pas de succès fictif
                _uiState.update {
                    it.copy(userNotice = "Aucun dépôt Git local")
                }
            }
        }
    }

    suspend fun cancelMergeSync() {
        val projectDir = activeProjectDir
        if (gitService != null && projectDir != null && projectDir.exists() && File(projectDir, ".git").exists()) {
            gitService.abortMerge(projectDir)
        }
        _uiState.update {
            it.copy(
                viewMode = GitViewMode.DEFAULT,
                conflicts = emptyList(),
                userNotice = "Fusion annulée."
            )
        }
    }

    fun cancelMerge(onDone: () -> Unit) {
        viewModelScope.launch {
            cancelMergeSync()
            onDone()
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }
}
