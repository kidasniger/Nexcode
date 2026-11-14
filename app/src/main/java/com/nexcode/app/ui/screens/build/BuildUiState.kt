package com.nexcode.app.ui.screens.build

/**
 * État réel des écrans de build (21 à 24).
 *
 * Aucune donnée n'est pré-remplie : tout ce qui est affiché provient soit du
 * projet réellement ouvert (fichiers sur disque), soit de GitHub Actions
 * (workflow déclenché, étapes, logs, artefacts téléchargés).
 */
enum class BuildVariant {
    DEBUG,
    RELEASE
}

enum class CompilationStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    CANCELLED,
    FAILED
}

enum class BuildStepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}

// Étape de build réelle (Écran 22) — provient des jobs GitHub Actions
data class BuildStep(
    val id: String,
    val label: String,
    val detail: String? = null,
    val status: BuildStepStatus = BuildStepStatus.PENDING
)

// Entrée de console de build réelle (Écran 23) — provient des logs du run
enum class BuildLogLevel {
    INFO,
    WARNING,
    ERROR
}

data class BuildLogEntry(
    val level: BuildLogLevel,
    val tag: String,
    val message: String,
    val time: String
)

// APK réel téléchargé depuis les artefacts du run (Écran 24)
data class ApkItem(
    val name: String,
    val description: String,
    val path: String,
    val sizeBytes: Long = 0
)

data class BuildUiState(
    // Écran 21 : configuration réelle (projet lié sur disque)
    val projectName: String = "",
    val versionName: String = "",
    val buildNumber: String = "",
    val githubRepo: String = "",
    val isGitHubReady: Boolean = false,
    val keystoreInfo: String = "",
    val selectedVariant: BuildVariant = BuildVariant.DEBUG,
    val canStartBuild: Boolean = false,
    val configNotice: String? = null,

    // Écran 22 : compilation réelle (run GitHub Actions)
    val compilationStatus: CompilationStatus = CompilationStatus.IDLE,
    val progressPercent: Int = 0,
    val steps: List<BuildStep> = emptyList(),
    val runUrl: String? = null,

    // Écran 23 : logs réels du run
    val selectedLogFilter: String = "Tout",
    val buildLogs: List<BuildLogEntry> = emptyList(),

    // Écran 24 : APK réels téléchargés depuis les artefacts
    val apkList: List<ApkItem> = emptyList(),
    val workflowNotice: String? = null
)
