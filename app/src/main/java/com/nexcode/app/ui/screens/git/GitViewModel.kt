package com.nexcode.app.ui.screens.git

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.git.GitOperationResult
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.preferences.NexcodePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class GitViewModel(
    context: Context? = null,
    overrideGitService: GitHubRepositoryService? = null
) : ViewModel() {

    private val gitService = overrideGitService ?: context?.let { GitHubRepositoryService(it) }
    private val preferences = context?.let { NexcodePreferences(it) }

    private var activeProjectDir: File? = null

    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()

    fun bindProject(projectName: String, gitRepo: String, localPath: String) {
        val dir = if (localPath.isNotBlank()) File(localPath) else null
        val sameDir = activeProjectDir?.absolutePath == dir?.absolutePath &&
            _uiState.value.repoFullName == formatRepoDisplay(projectName, gitRepo)
        activeProjectDir = dir

        _uiState.update {
            it.copy(
                repoFullName = formatRepoDisplay(projectName, gitRepo),
                hasGitRepo = dir != null && File(dir, ".git").exists()
            )
        }
        if (!sameDir) {
            viewModelScope.launch {
                refreshGitStatusSync(fetchRemote = false)
            }
        }
    }

    suspend fun bindProjectAndRefreshSync(
        projectName: String,
        gitRepo: String,
        localPath: String,
        fetchRemote: Boolean = false
    ) {
        activeProjectDir = if (localPath.isNotBlank()) File(localPath) else null
        _uiState.update {
            it.copy(repoFullName = formatRepoDisplay(projectName, gitRepo))
        }
        refreshGitStatusSync(fetchRemote = fetchRemote)
    }

    fun refreshGitStatus(fetchRemote: Boolean = false) {
        viewModelScope.launch {
            refreshGitStatusSync(fetchRemote)
        }
    }

    suspend fun refreshGitStatusSync(fetchRemote: Boolean = false) {
        val projectDir = activeProjectDir
        val service = gitService
        if (service == null || projectDir == null || !File(projectDir, ".git").exists()) {
            _uiState.update {
                it.copy(
                    hasGitRepo = false,
                    branchStatusText = "Aucun dépôt Git initialisé dans ce projet",
                    modifiedFiles = emptyList(),
                    unpushedCommits = emptyList(),
                    commits = emptyList(),
                    totalCommitsCount = 0,
                    conflicts = emptyList(),
                    aheadBy = 0,
                    behindBy = 0
                )
            }
            return
        }

        val branchRes = service.getBranchStatus(projectDir, fetchRemote = fetchRemote)
        val currentBranch: String
        val branches: List<String>
        val ahead: Int
        val behind: Int
        val hasConflicts: Boolean
        when (branchRes) {
            is GitOperationResult.Success -> {
                currentBranch = branchRes.data.currentBranch
                branches = branchRes.data.branches
                ahead = branchRes.data.aheadCount
                behind = branchRes.data.behindCount
                hasConflicts = branchRes.data.isMergingWithConflicts
            }
            is GitOperationResult.Error, is GitOperationResult.Conflict -> {
                currentBranch = _uiState.value.currentBranch
                branches = listOf(currentBranch)
                ahead = 0
                behind = 0
                hasConflicts = false
            }
        }

        val modifiedFiles = when (val diffRes = service.getModifiedFilesWithDiff(projectDir)) {
            is GitOperationResult.Success -> diffRes.data.map {
                ModifiedFile(it.path, it.addedLines, it.removedLines)
            }
            else -> emptyList()
        }

        val unpushed = when (val unpushedRes = service.getUnpushedCommits(projectDir)) {
            is GitOperationResult.Success -> unpushedRes.data.map {
                UnpushedCommit(hash = it.shortHash, scope = it.scope)
            }
            else -> emptyList()
        }
        val unpushedShortHashes = unpushed.map { it.hash }.toSet()

        val limit = _uiState.value.commitLoadLimit
        val allBranches = _uiState.value.selectedFilterBranch == "all"
        val commits = when (val histRes = service.getCommitHistory(projectDir, count = limit, allBranches = allBranches)) {
            is GitOperationResult.Success -> histRes.data.mapIndexed { index, commit ->
                GitCommit(
                    id = "commit_${commit.hash}",
                    hash = commit.shortHash,
                    message = commit.message,
                    author = commit.author,
                    date = commit.timeAgo,
                    isAccentIcon = index == 0,
                    isPushed = commit.shortHash !in unpushedShortHashes
                )
            }
            else -> emptyList()
        }

        val parsedConflicts = if (hasConflicts) {
            when (val confRes = service.parseMergeConflicts(projectDir)) {
                is GitOperationResult.Success -> confRes.data.map { hunk ->
                    ConflictItem(
                        id = hunk.id,
                        fileAndLine = "${hunk.fileName} · ligne ${hunk.startLine}",
                        localLines = hunk.localLines,
                        remoteLines = hunk.remoteLines,
                        selectedChoice = ConflictChoice.LOCAL
                    )
                }
                else -> emptyList()
            }
        } else {
            emptyList()
        }

        val statusSummary = when {
            hasConflicts -> "Conflits de fusion détectés sur $currentBranch"
            ahead > 0 && behind > 0 -> "$ahead en avance · $behind en retard sur origin/$currentBranch"
            ahead > 0 -> "$ahead commit(s) local(aux) prêt(s) à pousser sur origin/$currentBranch"
            behind > 0 -> "$behind commit(s) en attente depuis origin/$currentBranch"
            else -> "À jour avec origin/$currentBranch"
        }

        _uiState.update { current ->
            current.copy(
                hasGitRepo = true,
                currentBranch = currentBranch,
                availableBranches = branches,
                historyBranch = currentBranch,
                selectedFilterBranch = if (current.selectedFilterBranch == "all") "all" else currentBranch,
                branchStatusText = statusSummary,
                aheadBy = ahead,
                behindBy = behind,
                modifiedFiles = modifiedFiles,
                unpushedCommits = unpushed,
                commits = commits,
                totalCommitsCount = commits.size,
                conflicts = parsedConflicts
            )
        }
    }

    fun updateCommitMessage(message: String) {
        _uiState.update { it.copy(commitMessage = message) }
    }

    fun setViewMode(mode: GitViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun toggleViewMode() {
        _uiState.update {
            val next = if (it.viewMode == GitViewMode.DEFAULT) GitViewMode.ERROR else GitViewMode.DEFAULT
            it.copy(viewMode = next)
        }
    }

    fun switchBranch(branch: String) {
        viewModelScope.launch {
            switchBranchSync(branch)
        }
    }

    suspend fun switchBranchSync(branch: String) {
        val projectDir = activeProjectDir ?: return
        val service = gitService ?: return
        _uiState.update { it.copy(isBusy = true) }
        when (val res = service.checkoutBranch(projectDir, branch)) {
            is GitOperationResult.Success -> {
                _uiState.update { it.copy(isBusy = false, userNotice = "Branche active : $branch") }
                refreshGitStatusSync(fetchRemote = false)
            }
            is GitOperationResult.Error -> {
                _uiState.update { it.copy(isBusy = false, userNotice = res.message) }
            }
            is GitOperationResult.Conflict -> {
                _uiState.update { it.copy(isBusy = false, userNotice = "Conflits empêchant le changement de branche.") }
            }
        }
    }

    fun commitAndPush() {
        viewModelScope.launch {
            commitAndPushSync()
        }
    }

    suspend fun commitAndPushSync() {
        val projectDir = activeProjectDir
        val service = gitService
        if (service == null || projectDir == null || !File(projectDir, ".git").exists()) {
            _uiState.update {
                it.copy(
                    viewMode = GitViewMode.ERROR,
                    errorTitle = "Dépôt Git introuvable.",
                    errorDescription = "Importez un dépôt GitHub ou initialisez Git dans ce projet avant de pousser.",
                    userNotice = "Aucun dépôt Git initialisé."
                )
            }
            return
        }

        val msg = _uiState.value.commitMessage.trim().ifBlank { "chore: mise à jour NEXCODE" }
        val authorName = preferences?.gitUserNameFlow?.first().orEmpty().ifBlank { "NEXCODE Developer" }
        val authorEmail = preferences?.gitUserEmailFlow?.first().orEmpty().ifBlank { "dev@nexcode.local" }

        _uiState.update { it.copy(isBusy = true) }
        when (val res = service.commitAndPush(projectDir, msg, authorName, authorEmail)) {
            is GitOperationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        viewMode = GitViewMode.DEFAULT,
                        commitMessage = "",
                        userNotice = "Commit créé et poussé sur origin/${it.currentBranch}."
                    )
                }
                refreshGitStatusSync(fetchRemote = false)
            }
            is GitOperationResult.Error -> {
                refreshGitStatusSync(fetchRemote = false)
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
            is GitOperationResult.Conflict -> {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        viewMode = GitViewMode.ERROR,
                        errorTitle = "Conflit détecté.",
                        errorDescription = "Tirez (pull) les modifications distantes avant de pousser."
                    )
                }
            }
        }
    }

    fun pullRepository(onConflictFound: () -> Unit) {
        viewModelScope.launch {
            pullRepositorySync(onConflictFound)
        }
    }

    suspend fun pullRepositorySync(onConflictFound: () -> Unit) {
        val projectDir = activeProjectDir
        val service = gitService
        if (service == null || projectDir == null || !File(projectDir, ".git").exists()) {
            _uiState.update {
                it.copy(
                    viewMode = GitViewMode.ERROR,
                    errorTitle = "Dépôt Git introuvable.",
                    errorDescription = "Impossible de tirer : aucun dépôt Git n'est lié à ce projet.",
                    userNotice = "Aucun dépôt Git initialisé."
                )
            }
            return
        }

        _uiState.update { it.copy(isBusy = true, userNotice = "Synchronisation pull en cours…") }
        when (val res = service.pull(projectDir)) {
            is GitOperationResult.Success -> {
                refreshGitStatusSync(fetchRemote = false)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        viewMode = GitViewMode.DEFAULT,
                        userNotice = "Dépôt mis à jour (${res.data})."
                    )
                }
            }
            is GitOperationResult.Conflict -> {
                refreshGitStatusSync(fetchRemote = false)
                _uiState.update {
                    it.copy(
                        isBusy = false,
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
                        errorTitle = "Échec du pull.",
                        errorDescription = res.message,
                        userNotice = "Erreur réseau ou git : ${res.message}"
                    )
                }
            }
        }
    }

    fun retryPush() {
        viewModelScope.launch {
            retryPushSync()
        }
    }

    suspend fun retryPushSync() {
        val projectDir = activeProjectDir
        val service = gitService
        if (service == null || projectDir == null || !File(projectDir, ".git").exists()) {
            _uiState.update {
                it.copy(
                    viewMode = GitViewMode.ERROR,
                    errorTitle = "Dépôt Git introuvable.",
                    errorDescription = "Aucun dépôt Git disponible pour relancer le push."
                )
            }
            return
        }

        _uiState.update { it.copy(isBusy = true) }
        val result = if (_uiState.value.modifiedFiles.isNotEmpty() && _uiState.value.commitMessage.isNotBlank()) {
            val authorName = preferences?.gitUserNameFlow?.first().orEmpty().ifBlank { "NEXCODE Developer" }
            val authorEmail = preferences?.gitUserEmailFlow?.first().orEmpty().ifBlank { "dev@nexcode.local" }
            service.commitAndPush(projectDir, _uiState.value.commitMessage, authorName, authorEmail)
        } else {
            service.pushOnly(projectDir)
        }

        when (result) {
            is GitOperationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        viewMode = GitViewMode.DEFAULT,
                        userNotice = "Push réussi après reconnexion."
                    )
                }
                refreshGitStatusSync(fetchRemote = false)
            }
            is GitOperationResult.Error -> {
                refreshGitStatusSync(fetchRemote = false)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        viewMode = GitViewMode.ERROR,
                        errorTitle = "Échec du push.",
                        errorDescription = result.message,
                        userNotice = result.message
                    )
                }
            }
            is GitOperationResult.Conflict -> {
                _uiState.update { it.copy(isBusy = false, viewMode = GitViewMode.ERROR) }
            }
        }
    }

    fun selectFilterBranch(branch: String) {
        _uiState.update { it.copy(selectedFilterBranch = branch) }
        viewModelScope.launch {
            refreshGitStatusSync(fetchRemote = false)
        }
    }

    fun loadMoreCommits() {
        _uiState.update { it.copy(commitLoadLimit = it.commitLoadLimit + 25) }
        viewModelScope.launch {
            refreshGitStatusSync(fetchRemote = false)
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
        val count = _uiState.value.conflicts.size
        _uiState.update {
            it.copy(
                userNotice = if (count > 0) {
                    "$count conflit(s) prêt(s) à être appliqués selon vos sélections."
                } else {
                    "Aucun conflit actif à prévisualiser."
                }
            )
        }
    }

    fun applyConflictResolutions(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val ok = applyConflictResolutionsSync()
            if (ok) onSuccess()
        }
    }

    suspend fun applyConflictResolutionsSync(): Boolean {
        val projectDir = activeProjectDir ?: return false
        val service = gitService ?: return false
        val keepLocalMap = _uiState.value.conflicts.associate { item ->
            item.id to (item.selectedChoice == ConflictChoice.LOCAL)
        }
        val authorName = preferences?.gitUserNameFlow?.first().orEmpty().ifBlank { "NEXCODE Developer" }
        val authorEmail = preferences?.gitUserEmailFlow?.first().orEmpty().ifBlank { "dev@nexcode.local" }

        _uiState.update { it.copy(isBusy = true) }
        return when (val res = service.resolveConflictsAndCommit(projectDir, keepLocalMap, authorName, authorEmail)) {
            is GitOperationResult.Success -> {
                refreshGitStatusSync(fetchRemote = false)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        viewMode = GitViewMode.DEFAULT,
                        userNotice = "Conflits résolus et merge validé avec succès."
                    )
                }
                true
            }
            is GitOperationResult.Error -> {
                _uiState.update { it.copy(isBusy = false, userNotice = res.message) }
                false
            }
            is GitOperationResult.Conflict -> {
                _uiState.update { it.copy(isBusy = false) }
                false
            }
        }
    }

    fun cancelMerge(onDone: () -> Unit) {
        viewModelScope.launch {
            cancelMergeSync()
            onDone()
        }
    }

    suspend fun cancelMergeSync() {
        val projectDir = activeProjectDir
        val service = gitService
        if (projectDir != null && service != null) {
            service.abortMerge(projectDir)
        }
        _uiState.update { it.copy(conflicts = emptyList(), userNotice = "Merge annulé.") }
        refreshGitStatusSync(fetchRemote = false)
    }

    fun dismissNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    private fun formatRepoDisplay(projectName: String, gitRepo: String): String {
        val cleaned = gitRepo.trim()
        if (cleaned.isNotBlank()) {
            val parsed = GitHubRepositoryService.parseOwnerAndRepo(cleaned)
            return if (parsed != null) "${parsed.first} / ${parsed.second}" else cleaned
        }
        return if (projectName.isNotBlank()) "$projectName (local)" else "Aucun dépôt sélectionné"
    }
}
