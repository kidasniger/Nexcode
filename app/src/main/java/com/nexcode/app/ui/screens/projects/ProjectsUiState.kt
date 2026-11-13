package com.nexcode.app.ui.screens.projects

import com.nexcode.app.data.github.model.GitHubRepository

enum class ProjectFilter(val label: String) {
    ALL("Tous"),
    KOTLIN("Kotlin"),
    JAVA("Java"),
    FLUTTER("Flutter"),
    EXPORTED("Exportés")
}

enum class ProjectsViewMode {
    DEFAULT,
    EMPTY
}

enum class ProjectBuildStatus {
    SUCCESS,
    FAILED,
    RUNNING,
    DRAFT
}

data class ProjectItem(
    val id: Long,
    val name: String,
    val packageName: String,
    val initials: String,
    val language: String,
    val uiFramework: String,
    val modifiedText: String,
    val buildStatus: ProjectBuildStatus,
    val buildStatusLabel: String,
    val version: String? = null,
    val onGitHub: Boolean = false,
    val tileBgHex: String? = null,
    val tileTextHex: String? = null,
    val fileCount: Int = 0,
    val sizeText: String = "0 Mo",
    val buildCount: Int = 0,
    val gitRepo: String = "",
    val gitBranch: String = "main",
    val gitSyncText: String = "non synchronisé",
    val minSdk: Int = 26,
    val localPath: String = ""
)

enum class ProjectTemplateType(
    val title: String,
    val description: String,
    val isRecommended: Boolean = false
) {
    COMPOSE("Kotlin · Compose", "UI déclarative, thème Material 3, preuve de concept rapide.", true),
    XML("Kotlin · XML", "Vues classiques, plus léger sur les vieux appareils."),
    JAVA("Java", "Projet Java traditionnel avec AppCompat."),
    FLUTTER("Flutter", "Dart + SDK Flutter, compilation via Flutter CLI."),
    EMPTY("Projet vide", "Structure minimale Gradle, à composer vous-même.")
}

data class ProjectBuildItem(
    val id: Long,
    val versionText: String,
    val dateAndSizeText: String,
    val isSuccess: Boolean,
    val hasLogsChip: Boolean = false,
    val errorText: String? = null
)

data class ProjectsUiState(
    val projects: List<ProjectItem> = emptyList(),
    val filteredProjects: List<ProjectItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: ProjectFilter = ProjectFilter.ALL,
    val viewMode: ProjectsViewMode = ProjectsViewMode.EMPTY,
    val totalProjectCount: Int = 0,
    val totalSizeText: String = "0 Mo",
    val selectedProject: ProjectItem? = null,
    val projectToDelete: ProjectItem? = null,
    val showDeleteDialog: Boolean = false,

    // Écran 6 : Nouveau projet
    val selectedTemplate: ProjectTemplateType = ProjectTemplateType.COMPOSE,

    // Écran 7 : Assistant de création
    val newAppName: String = "",
    val newPackageName: String = "",
    val selectedMinSdk: Int = 29,
    val isAiScaffoldEnabled: Boolean = true,

    // Écran 8 : Derniers builds
    val recentBuilds: List<ProjectBuildItem> = emptyList(),

    // Écran 9 : Importer
    val importTabIndex: Int = 0, // 0 = Zip, 1 = Git
    val importGitUrl: String = "",
    val importGitBranch: String = "main",
    val isImporting: Boolean = false,
    val importError: String? = null,
    val recentZipImportName: String? = null,
    val recentZipImportDesc: String? = null,
    val userRepositories: List<GitHubRepository> = emptyList(),
    val isLoadingUserRepos: Boolean = false
)
