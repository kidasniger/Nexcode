package com.nexcode.app.ui.screens.build

import com.example.R

enum class BuildVariant {
    DEBUG,
    RELEASE
}

enum class CompilationStatus {
    RUNNING,
    SUCCESS,
    CANCELLED,
    FAILED
}

enum class BuildStepStatus {
    PENDING,
    RUNNING,
    SUCCESS
}

// Étape de compilation (Écran 22)
data class BuildStep(
    val id: String,
    val labelRes: Int,
    val detailRunningRes: Int? = null,
    val detailSuccessRes: Int? = null,
    val status: BuildStepStatus = BuildStepStatus.PENDING
)

// Entrée de console de build (Écran 23)
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

// APK généré (Écran 24)
data class ApkItem(
    val nameRes: Int,
    val descriptionRes: Int,
    val isAccent: Boolean = false
)

data class BuildUiState(
    // Écran 21 : Configuration du build
    val projectName: String = "Nimbus Météo",
    val versionName: String = "1.2.0",
    val buildNumber: String = "13",
    val selectedVariant: BuildVariant = BuildVariant.RELEASE,
    val keystoreName: String = "nexcode-release.jks",
    val keystorePassword: String = "uX9#kP2m!vR7",
    val isR8Enabled: Boolean = true,
    val isExportMappingEnabled: Boolean = true,
    val isDebuggableEnabled: Boolean = false,

    // Écran 22 : Compilation en cours
    val compilationStatus: CompilationStatus = CompilationStatus.RUNNING,
    val progressPercent: Int = 68,
    val steps: List<BuildStep> = defaultBuildSteps(),

    // Écran 23 : Logs et erreurs
    val logVariantSuccess: Boolean = false,
    val selectedLogFilter: String = "Tout",
    val buildLogs: List<BuildLogEntry> = defaultBuildLogs(),

    // Écran 24 : APK générés
    val apkList: List<ApkItem> = defaultApkList(),
    val workflowNotice: String? = "L'installation hors Play Store demande l'autorisation « sources inconnues ». Un avertissement Play Protect peut apparaître — il est normal pour un APK signé par vous."
)

// Étapes de compilation par défaut (maquette : Kotlin en cours, signature en attente)
internal fun defaultBuildSteps(): List<BuildStep> = listOf(
    BuildStep(
        id = "clean",
        labelRes = R.string.s22_step_clean,
        detailSuccessRes = R.string.s22_step_clean_du,
        status = BuildStepStatus.SUCCESS
    ),
    BuildStep(
        id = "gradle",
        labelRes = R.string.s22_step_gradle,
        detailRunningRes = R.string.s22_step_gradle_du_cours,
        detailSuccessRes = R.string.s22_step_gradle_du_succes,
        status = BuildStepStatus.SUCCESS
    ),
    BuildStep(
        id = "kotlin",
        labelRes = R.string.s22_step_kotlin,
        detailRunningRes = R.string.s22_step_kotlin_du_cours,
        detailSuccessRes = R.string.s22_step_kotlin_du_succes,
        status = BuildStepStatus.RUNNING
    ),
    BuildStep(
        id = "res",
        labelRes = R.string.s22_step_res,
        detailSuccessRes = R.string.s22_step_res_du_succes,
        status = BuildStepStatus.PENDING
    ),
    BuildStep(
        id = "dex",
        labelRes = R.string.s22_step_dex,
        detailSuccessRes = R.string.s22_step_dex_du_succes,
        status = BuildStepStatus.PENDING
    ),
    BuildStep(
        id = "apk",
        labelRes = R.string.s22_step_apk,
        detailSuccessRes = R.string.s22_step_apk_du_succes,
        status = BuildStepStatus.PENDING
    ),
    BuildStep(
        id = "sign",
        labelRes = R.string.s22_step_sign,
        detailSuccessRes = R.string.s22_step_sign_du_succes,
        status = BuildStepStatus.PENDING
    )
)

// Console du build #12 (interrompu : 2 erreurs Kotlin, 5 avertissements)
internal fun defaultBuildLogs(): List<BuildLogEntry> = listOf(
    BuildLogEntry(
        level = BuildLogLevel.INFO,
        tag = "Gradle",
        message = "Démarrage du build #12 (variante release)",
        time = "00:00"
    ),
    BuildLogEntry(
        level = BuildLogLevel.INFO,
        tag = "Gradle",
        message = "> Task :app:clean — cache local réutilisé (0,8 s)",
        time = "00:02"
    ),
    BuildLogEntry(
        level = BuildLogLevel.INFO,
        tag = "Gradle",
        message = "Résolution des dépendances Gradle · 34 artefacts récupérés",
        time = "00:06"
    ),
    BuildLogEntry(
        level = BuildLogLevel.WARNING,
        tag = "Ressources",
        message = "Ressource inutilisée : R.string.title_detail (strings.xml)",
        time = "00:14"
    ),
    BuildLogEntry(
        level = BuildLogLevel.WARNING,
        tag = "Ressources",
        message = "Ressource inutilisée : R.drawable.ic_qr (drawables)",
        time = "00:15"
    ),
    BuildLogEntry(
        level = BuildLogLevel.ERROR,
        tag = "Kotlin",
        message = "e: ScreenS23BuildLogs.kt:37 Unresolved reference 'titleDetail'",
        time = "00:38"
    ),
    BuildLogEntry(
        level = BuildLogLevel.ERROR,
        tag = "Kotlin",
        message = "e: Compilation Kotlin échouée — 2 erreurs sur 12 fichiers analysés",
        time = "02:29"
    ),
    BuildLogEntry(
        level = BuildLogLevel.INFO,
        tag = "Gradle",
        message = "FAILURE : build terminé en 02 min 41 s — 2 erreurs, 5 avertissements",
        time = "02:41"
    )
)

// APK générés (maquette : 3 fichiers, le plus récent mis en avant)
internal fun defaultApkList(): List<ApkItem> = listOf(
    ApkItem(
        nameRes = R.string.s24_apk1_name,
        descriptionRes = R.string.s24_apk1_desc,
        isAccent = true
    ),
    ApkItem(
        nameRes = R.string.s24_apk2_name,
        descriptionRes = R.string.s24_apk2_desc,
        isAccent = false
    ),
    ApkItem(
        nameRes = R.string.s24_apk3_name,
        descriptionRes = R.string.s24_apk3_desc,
        isAccent = false
    )
)
