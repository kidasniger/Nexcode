package com.nexcode.app.ui.screens.git

enum class GitViewMode {
    DEFAULT,
    ERROR
}

data class ModifiedFile(
    val name: String,
    val added: Int,
    val removed: Int
)

data class UnpushedCommit(
    val hash: String,
    val scope: String
)

data class GitCommit(
    val id: String,
    val hash: String,
    val message: String,
    val author: String,
    val date: String,
    val isAccentIcon: Boolean = false,
    val isPushed: Boolean = true
)

enum class ConflictChoice {
    LOCAL,
    REMOTE
}

data class ConflictItem(
    val id: String,
    val fileAndLine: String,
    val localLines: List<String>,
    val remoteLines: List<String>,
    val selectedChoice: ConflictChoice
)

data class GitUiState(
    // Écran 25 : Dépôt
    val repoFullName: String = "Aucun dépôt sélectionné",
    val currentBranch: String = "main",
    val availableBranches: List<String> = emptyList(),
    val branchStatusText: String = "Aucun dépôt Git initialisé",
    val aheadBy: Int = 0,
    val behindBy: Int = 0,
    val hasGitRepo: Boolean = false,
    val isBusy: Boolean = false,
    val viewMode: GitViewMode = GitViewMode.DEFAULT,
    val errorTitle: String = "Échec du push.",
    val errorDescription: String = "Connexion à github.com indisponible. Les commits locaux sont en sécurité — réessayez plus tard.",
    val modifiedFiles: List<ModifiedFile> = emptyList(),
    val commitMessage: String = "",
    val unpushedCommits: List<UnpushedCommit> = emptyList(),

    // Écran 26 : Historique des commits
    val historyBranch: String = "main",
    val totalCommitsCount: Int = 0,
    val commitLoadLimit: Int = 20,
    val selectedFilterBranch: String = "main",
    val commits: List<GitCommit> = emptyList(),

    // Écran 27 : Résolution de conflits
    val conflicts: List<ConflictItem> = emptyList(),

    val userNotice: String? = null
)
