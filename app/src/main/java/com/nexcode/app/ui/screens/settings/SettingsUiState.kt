package com.nexcode.app.ui.screens.settings

enum class UiThemePreference {
    SYSTEM,
    DARK,
    LIGHT
}

enum class CodeThemePreference {
    NEX_DARK,
    NEX_LIGHT,
    SOLARIZED,
    HIGH_CONTRAST
}

enum class IndentationPreference {
    SPACES_2,
    SPACES_4,
    TABS
}

data class SettingsUiState(
    // Compte
    val accountInitials: String = "?",
    val accountUsername: String = "Non connecté",
    val accountPlan: String = "Aucun compte associé",
    val showSignOutDialog: Boolean = false,
    val isSignedOut: Boolean = true,

    // Préférences S28
    val language: String = "Français (France)",
    val aiModelSummary: String = "claude-sonnet-4.5 · Anthropic",

    // Stockage S28
    val storageUsedText: String = "0 Mo utilisés",
    val storageTotalText: String = "sur 4 Go",
    val storageBarFillRatio: Float = 0f,
    val storageProjectsText: String = "Projets · 0 Mo",
    val storageGradleText: String = "Caches · 0 Mo",
    val storageApkText: String = "APK · 0 Mo",
    val clearCacheButtonLabel: String = "Vider les caches (0 Mo)",
    val isCacheCleared: Boolean = false,

    // Notifications S28
    val notifyBuildCompleted: Boolean = true,
    val notifyBuildErrors: Boolean = true,
    val notifyDependencyUpdates: Boolean = false,

    // Apparence & Éditeur S29
    val uiTheme: UiThemePreference = UiThemePreference.DARK,
    val editorFontSizeSp: Int = 14,
    val codeTheme: CodeThemePreference = CodeThemePreference.NEX_DARK,
    val indentation: IndentationPreference = IndentationPreference.SPACES_4,
    val wordWrap: Boolean = true,
    val lineNumbers: Boolean = true,
    val autoCloseBrackets: Boolean = true,

    // Sauvegarde & À propos S30
    val lastExportText: String = "Export local des projets au format .zip",
    val autoBackupEnabled: Boolean = true,
    val appVersion: String = "1.4.2 (build 42)",
    val updateStatus: String = "À jour",
    val userNotice: String? = null
)
