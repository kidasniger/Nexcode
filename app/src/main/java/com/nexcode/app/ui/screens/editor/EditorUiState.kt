package com.nexcode.app.ui.screens.editor

data class FileTreeItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val indentLevel: Int, // 0: root, 1: i1, 2: i2, 3: i3
    val countOrDesc: String? = null,
    val isModified: Boolean = false,
    val isSelected: Boolean = false
)

data class CodeLine(
    val number: Int,
    val content: String,
    val hasError: Boolean = false,
    val errorSpanText: String? = null
)

data class SearchResultMatch(
    val lineNumber: Int,
    val prefixText: String,
    val matchText: String,
    val suffixText: String
)

data class SearchResultFileGroup(
    val fileName: String,
    val matchCount: Int,
    val matches: List<SearchResultMatch>
)

data class ResourceStringItem(
    val key: String,
    val value: String
)

data class ResourceColorItem(
    val key: String,
    val hexColor: String
)

data class ResourceDimensionItem(
    val key: String,
    val value: String
)

data class GradleDependencyItem(
    val name: String,
    val version: String,
    val statusText: String,
    val statusVariant: DependencyStatusVariant
)

enum class DependencyStatusVariant {
    UP_TO_DATE,
    WARN_UPDATE,
    CONFLICT
}

enum class ResourceTab {
    STRINGS,
    COLORS,
    DIMENSIONS,
    THEMES
}

data class EditorUiState(
    val projectName: String = "Nimbus Météo",
    val packageSubPath: String = "app · src · main · java · com · nimbus · meteo",
    val treeItems: List<FileTreeItem> = emptyList(),
    val openTabs: List<String> = listOf("MainActivity.kt", "Theme.kt", "CityRepository.kt"),
    val activeTab: String = "MainActivity.kt",
    val codeHeaderTitle: String = "MainActivity.kt · Kotlin",
    val errorCount: Int = 1,
    val codeLines: List<CodeLine> = emptyList(),
    val errorLineNumber: Int = 23,
    val errorTitle: String = "Ligne 23 · référence introuvable",
    val errorDesc: String = "R.string.title_detail n'existe pas — la ressource s'appelle title_details.",
    // S11 : État undo/redo & feedback symbole
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    // S12 : Recherche et remplacement
    val searchQuery: String = "R.string.title_",
    val replaceQuery: String = "R.string.title_details",
    val matchCase: Boolean = true,
    val regex: Boolean = false,
    val wholeProject: Boolean = true,
    val showReplacePreview: Boolean = false,
    val searchResults: List<SearchResultFileGroup> = emptyList(),
    val totalMatchesCount: Int = 4,
    // S13 : Ressources
    val activeResourceTab: ResourceTab = ResourceTab.STRINGS,
    val stringResources: List<ResourceStringItem> = emptyList(),
    val colorResources: List<ResourceColorItem> = emptyList(),
    val dimensionResources: List<ResourceDimensionItem> = emptyList(),
    val showTranslationsSyncedBanner: Boolean = false,
    val showThemesPreviewDialog: Boolean = false,
    val editingStringResource: ResourceStringItem? = null,
    val showAddResourceDialog: Boolean = false,
    // S14 : Dépendances Gradle
    val gradleDependencies: List<GradleDependencyItem> = emptyList(),
    val newDependencyInput: String = "",
    val gradleSyncSuccess: Boolean = false,
    // Bottom sheets / modales S10
    val showFileContextMenu: Boolean = false,
    val selectedFileForMenu: FileTreeItem? = null,
    val showNewEntryDialog: Boolean = false,
    val newEntryIsDirectory: Boolean = false,
    val showRenameDialog: Boolean = false
)
