package com.nexcode.app.ui.screens.editor

data class FileTreeItem(
    val name: String,
    val path: String, // chemin relatif au dossier racine du projet (données réelles du disque)
    val isDirectory: Boolean,
    val indentLevel: Int, // 0: racine, 1: enfant, 2: petit-enfant...
    val countOrDesc: String? = null,
    val isModified: Boolean = false, // tampon modifié non encore enregistré sur le disque
    val isSelected: Boolean = false
)

data class CodeLine(
    val number: Int,
    val content: String
)

data class SearchResultMatch(
    val lineNumber: Int,
    val prefixText: String,
    val matchText: String,
    val suffixText: String
)

data class SearchResultFileGroup(
    val fileName: String, // chemin relatif réel du fichier
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
    val name: String, // groupe:artefact
    val version: String,
    val configuration: String // implementation, ksp, testImplementation... (config réelle du fichier)
)

enum class ResourceTab {
    STRINGS,
    COLORS,
    DIMENSIONS,
    THEMES
}

data class EditorUiState(
    // Projet réellement ouvert (bindProject) — aucun projet fictif
    val hasProject: Boolean = false,
    val projectName: String = "",
    val projectRootPath: String = "",

    // Arborescence réelle du disque
    val treeItems: List<FileTreeItem> = emptyList(),

    // Onglets ouverts : chemins relatifs réels
    val openTabs: List<String> = emptyList(),
    val activeTab: String = "",
    val codeHeaderTitle: String = "",
    val codeLines: List<CodeLine> = emptyList(),
    val unsavedPaths: Set<String> = emptySet(),

    // Messages réels issus des opérations disque
    val fileMessage: String? = null,
    val fileError: String? = null,

    // S11 : État undo/redo & feedback symbole
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    // S12 : Recherche et remplacement (requêtes vides par défaut)
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val matchCase: Boolean = true,
    val regex: Boolean = false,
    val wholeProject: Boolean = true,
    val showReplacePreview: Boolean = false,
    val searchResults: List<SearchResultFileGroup> = emptyList(),
    val totalMatchesCount: Int = 0,
    val searchStatusMessage: String? = null,
    val searchStatusIsError: Boolean = false,

    // S13 : Ressources (parsées depuis les vrais fichiers res/values)
    val activeResourceTab: ResourceTab = ResourceTab.STRINGS,
    val stringResources: List<ResourceStringItem> = emptyList(),
    val colorResources: List<ResourceColorItem> = emptyList(),
    val dimensionResources: List<ResourceDimensionItem> = emptyList(),
    val themesFileContent: String? = null, // contenu réel de res/values/themes.xml (null si absent)
    val showThemesPreviewDialog: Boolean = false,
    val editingStringResource: ResourceStringItem? = null,
    val showAddResourceDialog: Boolean = false,

    // S14 : Dépendances Gradle (parsées depuis le vrai build.gradle(.kts))
    val gradleDependencies: List<GradleDependencyItem> = emptyList(),
    val gradleSyncSuccess: Boolean = false,
    val gradleSyncMessage: String = "",
    val gradleSyncError: String? = null,
    val newDependencyInput: String = "",

    // Bottom sheets / modales S10
    val showFileContextMenu: Boolean = false,
    val selectedFileForMenu: FileTreeItem? = null,
    val showNewEntryDialog: Boolean = false,
    val newEntryIsDirectory: Boolean = false,
    val showRenameDialog: Boolean = false
)
