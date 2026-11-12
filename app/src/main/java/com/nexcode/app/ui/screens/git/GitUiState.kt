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
    val isAccentIcon: Boolean = false
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
    val repoFullName: String = "nimbus-dev / meteo-android",
    val currentBranch: String = "main",
    val branchStatusText: String = "À jour avec origin/main · il y a 12 min",
    val viewMode: GitViewMode = GitViewMode.DEFAULT,
    val modifiedFiles: List<ModifiedFile> = listOf(
        ModifiedFile("MainActivity.kt", 18, 4),
        ModifiedFile("strings.xml", 6, 0),
        ModifiedFile("Theme.kt", 3, 1)
    ),
    val commitMessage: String = "feat: écran de détails météo",
    val unpushedCommits: List<UnpushedCommit> = listOf(
        UnpushedCommit("a3f92c1", "local"),
        UnpushedCommit("b71e0d4", "local")
    ),

    // Écran 26 : Historique des commits
    val historyBranch: String = "main",
    val totalCommitsCount: Int = 214,
    val selectedFilterBranch: String = "main",
    val commits: List<GitCommit> = listOf(
        GitCommit("c1", "a3f92c1", "feat : écran de détails météo", "nimbus-dev", "il y a 2 h", isAccentIcon = true),
        GitCommit("c2", "b71e0d4", "fix : gestion du cache hors ligne", "nimbus-dev", "hier", isAccentIcon = false),
        GitCommit("c3", "c9a1b77", "chore : mise à jour des dépendances Gradle", "nimbus-dev", "le 3 nov.", isAccentIcon = false),
        GitCommit("c4", "d44f0e2", "feat : recherche dans les projets", "nimbus-dev", "le 1 nov.", isAccentIcon = false),
        GitCommit("c5", "e12c9a4", "style : thème Material 3 sombre", "nimbus-dev", "le 28 oct.", isAccentIcon = false)
    ),

    // Écran 27 : Résolution de conflits
    val conflicts: List<ConflictItem> = listOf(
        ConflictItem(
            id = "conf_1",
            fileAndLine = "MainActivity.kt · ligne 14",
            localLines = listOf("CityScreen(city =", "  prefs.defaultCity)"),
            remoteLines = listOf("CityScreen(city =", "  repo.firstCity())"),
            selectedChoice = ConflictChoice.LOCAL
        ),
        ConflictItem(
            id = "conf_2",
            fileAndLine = "strings.xml · ligne 3",
            localLines = listOf("<string name=", "\"title_details\">"),
            remoteLines = listOf("<string name=", "\"title_detail\">"),
            selectedChoice = ConflictChoice.REMOTE
        )
    ),

    val userNotice: String? = null
)
