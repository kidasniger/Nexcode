package com.nexcode.app.ui.screens.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.ui.screens.projects.ProjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Limites raisonnables pour l'exploration réelle du disque
private const val MAX_TREE_FILES = 200
private const val MAX_SEARCH_FILES = 50
private const val MAX_TOTAL_MATCHES = 200
private const val MAX_MATCHES_PER_FILE = 20
private const val MAX_FILE_BYTES = 512L * 1024L

private val IGNORED_DIR_NAMES = setOf(".git", "build", ".gradle", ".idea")

private val TEXT_EXTENSIONS = setOf(
    "kt", "java", "xml", "gradle", "kts", "properties", "md", "txt", "json",
    "yaml", "yml", "toml", "pro", "cfg", "conf", "html", "css", "dart", "py", "sh", "csv", "gitignore"
)

private val GRADLE_CONFIGURATIONS = listOf(
    "implementation", "api", "debugImplementation", "releaseImplementation",
    "testImplementation", "androidTestImplementation", "compileOnly", "runtimeOnly",
    "ksp", "kapt", "coreLibraryDesugaring"
)

private val GRADLE_ENTRY_REGEX = Regex(
    """(${GRADLE_CONFIGURATIONS.joinToString("|")})\s*(?:\(\s*)?["']([^"']+)["']"""
)

class EditorViewModel(
    @Suppress("UNUSED_PARAMETER") context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // État interne d'édition (tampons réels des fichiers ouverts)
    private data class BufferedFile(
        val lines: List<CodeLine>,
        val trailingNewline: Boolean
    )

    private val unsavedBuffers = LinkedHashMap<String, BufferedFile>()
    private val pendingSavePaths = LinkedHashSet<String>()
    private val expandedDirs = LinkedHashSet<String>()
    private val undoStack = ArrayDeque<List<CodeLine>>()
    private val redoStack = ArrayDeque<List<CodeLine>>()
    private var activeFileTrailingNewline = false

    // ------------------------------------------------------------------
    // Ouverture réelle d'un projet
    // ------------------------------------------------------------------

    /**
     * Charge l'arborescence RÉELLE du dossier disque du projet.
     * project == null ou dossier inexistant -> état vide honnête, aucune donnée inventée.
     */
    fun bindProject(project: ProjectItem?) {
        viewModelScope.launch { bindProjectSync(project) }
    }

    suspend fun bindProjectSync(project: ProjectItem?) {
        if (project == null || project.localPath.isBlank()) {
            resetToEmptyState()
            return
        }
        val root = File(project.localPath)
        if (!root.exists() || !root.isDirectory) {
            resetToEmptyState()
            _uiState.update {
                it.copy(fileError = "Le dossier « ${project.localPath} » du projet « ${project.name} » est introuvable sur le disque.")
            }
            return
        }
        unsavedBuffers.clear()
        pendingSavePaths.clear()
        expandedDirs.clear()
        undoStack.clear()
        redoStack.clear()
        _uiState.value = EditorUiState(
            hasProject = true,
            projectName = project.name,
            projectRootPath = root.absolutePath
        )
        // Déploiement réel de l'arborescence : tous les dossiers source sont ouverts
        // dès la liaison (l'affichage reste plafonné par MAX_TREE_FILES).
        expandedDirs.addAll(collectAllDirectories(root))
        refreshTreeSync()
        loadResourcesSync()
        syncGradleDependenciesSync()
    }

    private fun resetToEmptyState() {
        unsavedBuffers.clear()
        pendingSavePaths.clear()
        expandedDirs.clear()
        undoStack.clear()
        redoStack.clear()
        _uiState.value = EditorUiState()
    }

    /**
     * Liste réelle des dossiers du projet (chemins relatifs) pour l'auto-déploiement.
     * Les dossiers ignorés (.git, build, etc.) ne sont jamais ouverts.
     */
    private fun collectAllDirectories(root: File): Set<String> {
        val dirs = mutableSetOf<String>()
        root.walkTopDown()
            .onEnter { dir ->
                // Descendre depuis la racine, mais jamais dans un dossier ignoré.
                dir.isDirectory && (dir == root || dir.name !in IGNORED_DIR_NAMES)
            }
            .forEach { dir ->
                if (dir.isDirectory && dir != root) {
                    dirs += dir.relativeTo(root).path
                }
            }
        return dirs
    }

    // ------------------------------------------------------------------
    // Arborescence réelle
    // ------------------------------------------------------------------

    private suspend fun refreshTreeSync() {
        val state = _uiState.value
        if (!state.hasProject) return
        val root = File(state.projectRootPath)
        val selected = state.activeTab.takeIf { it.isNotBlank() }
        val items = withContext(Dispatchers.IO) {
            buildTreeItems(root, expandedDirs, pendingSavePaths, selected)
        }
        _uiState.update { it.copy(treeItems = items) }
    }

    private fun buildTreeItems(
        root: File,
        expandedDirs: Set<String>,
        modifiedPaths: Set<String>,
        selectedPath: String?
    ): List<FileTreeItem> {
        val items = mutableListOf<FileTreeItem>()
        var truncated = false

        fun walk(dir: File, indent: Int) {
            val children = dir.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: return
            for (child in children) {
                if (items.size >= MAX_TREE_FILES) {
                    truncated = true
                    return
                }
                if (child.isDirectory && child.name in IGNORED_DIR_NAMES) continue
                val rel = child.relativeTo(root).path
                if (child.isDirectory) {
                    val isExpanded = rel in expandedDirs
                    val directCount = child.listFiles()?.size ?: 0
                    items += FileTreeItem(
                        name = child.name,
                        path = rel,
                        isDirectory = true,
                        indentLevel = indent,
                        countOrDesc = directCount.toString()
                    )
                    if (isExpanded) walk(child, indent + 1)
                } else {
                    items += FileTreeItem(
                        name = child.name,
                        path = rel,
                        isDirectory = false,
                        indentLevel = indent,
                        countOrDesc = null,
                        isModified = rel in modifiedPaths,
                        isSelected = rel == selectedPath
                    )
                }
            }
        }

        walk(root, 0)
        if (truncated) {
            items += FileTreeItem(
                name = "… affichage limité à $MAX_TREE_FILES fichiers",
                path = "",
                isDirectory = false,
                indentLevel = 0
            )
        }
        return items
    }

    private fun ensureAncestorsExpanded(relativePath: String) {
        val segments = relativePath.split('/')
        var current = ""
        for (i in 0 until segments.size - 1) {
            current = if (current.isEmpty()) segments[i] else "$current/${segments[i]}"
            expandedDirs.add(current)
        }
    }

    // ------------------------------------------------------------------
    // Sélection / ouverture réelle de fichiers
    // ------------------------------------------------------------------

    fun selectFile(item: FileTreeItem) {
        if (item.isDirectory) {
            if (item.path in expandedDirs) expandedDirs.remove(item.path) else expandedDirs.add(item.path)
            viewModelScope.launch { refreshTreeSync() }
        } else {
            openFile(item.path)
        }
    }

    fun openFile(relativePath: String) {
        viewModelScope.launch { openFileSync(relativePath) }
    }

    suspend fun openFileSync(relativePath: String) {
        val state = _uiState.value
        if (!state.hasProject) return
        val root = File(state.projectRootPath)
        val file = File(root, relativePath)
        if (!file.exists() || !file.isFile) {
            _uiState.update {
                it.copy(fileError = "Fichier introuvable sur le disque : $relativePath")
            }
            return
        }
        val buffered = unsavedBuffers[relativePath]
        val (lines, trailing) = if (buffered != null) {
            buffered.lines to buffered.trailingNewline
        } else {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    if (file.length() <= MAX_FILE_BYTES) file.readText() else null
                }.getOrNull()
            }
            if (content == null) {
                _uiState.update {
                    it.copy(fileError = "Impossible de lire ce fichier (binaire ou trop volumineux) : $relativePath")
                }
                return
            }
            val endsWithNewline = content.endsWith("\n")
            val split = content.split("\n").let {
                if (endsWithNewline && it.size > 1) it.dropLast(1) else it
            }
            split.mapIndexed { index, line -> CodeLine(index + 1, line) } to endsWithNewline
        }
        activeFileTrailingNewline = trailing
        undoStack.clear()
        redoStack.clear()
        val displayName = relativePath.substringAfterLast('/')
        _uiState.update { st ->
            st.copy(
                openTabs = if (relativePath in st.openTabs) st.openTabs else st.openTabs + relativePath,
                activeTab = relativePath,
                codeLines = lines,
                codeHeaderTitle = "$displayName · ${languageLabelFor(displayName)}",
                canUndo = false,
                canRedo = false,
                fileError = null,
                treeItems = st.treeItems.map { it.copy(isSelected = it.path == relativePath) }
            )
        }
    }

    fun openTab(relativePath: String) {
        if (relativePath == _uiState.value.activeTab) return
        viewModelScope.launch { openFileSync(relativePath) }
    }

    fun closeTab(relativePath: String) {
        unsavedBuffers.remove(relativePath)
        pendingSavePaths.remove(relativePath)
        syncUnsavedToState()
        _uiState.update { st ->
            val updated = st.openTabs.filterNot { it == relativePath }
            val nextActive = if (st.activeTab == relativePath) updated.lastOrNull().orEmpty() else st.activeTab
            st.copy(openTabs = updated, activeTab = nextActive)
        }
        val next = _uiState.value.activeTab
        if (next.isNotBlank()) {
            viewModelScope.launch { openFileSync(next) }
        } else {
            _uiState.update { it.copy(codeLines = emptyList(), codeHeaderTitle = "") }
        }
    }

    // ------------------------------------------------------------------
    // Édition réelle du tampon + enregistrement disque
    // ------------------------------------------------------------------

    fun updateCodeLine(lineNumber: Int, newContent: String) {
        val state = _uiState.value
        if (state.activeTab.isBlank()) return
        undoStack.addLast(state.codeLines)
        redoStack.clear()
        val updated = state.codeLines.map {
            if (it.number == lineNumber) it.copy(content = newContent) else it
        }
        unsavedBuffers[state.activeTab] = BufferedFile(updated, activeFileTrailingNewline)
        pendingSavePaths.add(state.activeTab)
        _uiState.update { st ->
            st.copy(
                codeLines = updated,
                canUndo = true,
                canRedo = false,
                treeItems = st.treeItems.map {
                    if (it.path == st.activeTab) it.copy(isModified = true) else it
                }
            )
        }
        syncUnsavedToState()
    }

    fun insertSymbol(symbol: String) {
        val inserted = if (symbol == "⇥") "    " else symbol
        val state = _uiState.value
        if (state.activeTab.isBlank()) return
        val target = state.codeLines.lastOrNull() ?: return
        updateCodeLine(target.number, target.content + inserted)
    }

    fun undo() {
        val state = _uiState.value
        if (state.activeTab.isBlank()) return
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(state.codeLines)
        unsavedBuffers[state.activeTab] = BufferedFile(previous, activeFileTrailingNewline)
        pendingSavePaths.add(state.activeTab)
        _uiState.update {
            it.copy(codeLines = previous, canUndo = undoStack.isNotEmpty(), canRedo = true)
        }
        syncUnsavedToState()
    }

    fun redo() {
        val state = _uiState.value
        if (state.activeTab.isBlank()) return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(state.codeLines)
        unsavedBuffers[state.activeTab] = BufferedFile(next, activeFileTrailingNewline)
        pendingSavePaths.add(state.activeTab)
        _uiState.update {
            it.copy(codeLines = next, canUndo = true, canRedo = redoStack.isNotEmpty())
        }
        syncUnsavedToState()
    }

    fun saveActiveFile() {
        viewModelScope.launch { saveActiveFileSync() }
    }

    suspend fun saveActiveFileSync(): Boolean {
        val state = _uiState.value
        val path = state.activeTab
        if (!state.hasProject || path.isBlank()) {
            _uiState.update { it.copy(fileError = "Aucun fichier ouvert à enregistrer.") }
            return false
        }
        val file = File(state.projectRootPath, path)
        val text = state.codeLines.joinToString("\n") +
            if (activeFileTrailingNewline && state.codeLines.isNotEmpty()) "\n" else ""
        return try {
            withContext(Dispatchers.IO) {
                file.parentFile?.mkdirs()
                file.writeText(text)
            }
            unsavedBuffers.remove(path)
            pendingSavePaths.remove(path)
            syncUnsavedToState()
            _uiState.update {
                it.copy(fileMessage = "Fichier enregistré sur le disque : $path", fileError = null)
            }
            refreshTreeSync()
            true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(fileError = "Échec de l'enregistrement de $path : ${e.message ?: "erreur disque"}")
            }
            false
        }
    }

    fun dismissFileMessage() {
        _uiState.update { it.copy(fileMessage = null, fileError = null) }
    }

    private fun syncUnsavedToState() {
        _uiState.update { it.copy(unsavedPaths = pendingSavePaths.toSet()) }
    }

    // ------------------------------------------------------------------
    // Menu contextuel / opérations disque réelles
    // ------------------------------------------------------------------

    fun showContextMenu(file: FileTreeItem) {
        _uiState.update { it.copy(showFileContextMenu = true, selectedFileForMenu = file) }
    }

    fun hideContextMenu() {
        _uiState.update { it.copy(showFileContextMenu = false, selectedFileForMenu = null) }
    }

    fun openNewEntryDialog(isDirectory: Boolean) {
        _uiState.update {
            it.copy(
                showFileContextMenu = false,
                showNewEntryDialog = true,
                newEntryIsDirectory = isDirectory
            )
        }
    }

    fun dismissNewEntryDialog() {
        _uiState.update { it.copy(showNewEntryDialog = false) }
    }

    fun createNewEntry(rawName: String, isDirectory: Boolean) {
        viewModelScope.launch { createNewEntrySync(rawName, isDirectory) }
    }

    suspend fun createNewEntrySync(rawName: String, isDirectory: Boolean): Boolean {
        val state = _uiState.value
        val cleanName = rawName.trim().replace('/', '_').replace('\\', '_')
        if (!state.hasProject) {
            _uiState.update { it.copy(showNewEntryDialog = false, fileError = "Aucun projet ouvert.") }
            return false
        }
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(showNewEntryDialog = false) }
            return false
        }
        val root = File(state.projectRootPath)
        val parent = parentDirForSelection(root)
        val target = File(parent, cleanName)
        if (target.exists()) {
            _uiState.update {
                it.copy(fileError = "Un élément nommé « $cleanName » existe déjà à cet emplacement.")
            }
            return false
        }
        val created = withContext(Dispatchers.IO) {
            runCatching {
                if (isDirectory) target.mkdirs() else {
                    target.parentFile?.mkdirs()
                    target.createNewFile()
                }
            }.getOrDefault(false)
        }
        if (!created) {
            _uiState.update {
                it.copy(fileError = "Échec de la création de « $cleanName » sur le disque.")
            }
            return false
        }
        val relative = target.relativeTo(root).path
        ensureAncestorsExpanded(relative)
        _uiState.update {
            it.copy(
                showNewEntryDialog = false,
                fileError = null,
                fileMessage = if (isDirectory) "Dossier créé : $relative" else "Fichier créé : $relative"
            )
        }
        refreshTreeSync()
        if (!isDirectory) openFileSync(relative)
        return true
    }

    fun openRenameDialog() {
        _uiState.update { it.copy(showFileContextMenu = false, showRenameDialog = true) }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = false, selectedFileForMenu = null) }
    }

    fun renameSelectedFile(newName: String) {
        viewModelScope.launch { renameSelectedFileSync(newName) }
    }

    suspend fun renameSelectedFileSync(newName: String): Boolean {
        val state = _uiState.value
        val target = state.selectedFileForMenu
        val clean = newName.trim().replace('/', '_').replace('\\', '_')
        if (target == null || !state.hasProject) {
            _uiState.update { it.copy(showRenameDialog = false, selectedFileForMenu = null) }
            return false
        }
        if (clean.isEmpty() || clean == target.name) {
            _uiState.update { it.copy(showRenameDialog = false, selectedFileForMenu = null) }
            return false
        }
        val root = File(state.projectRootPath)
        val source = File(root, target.path)
        val destination = File(source.parentFile, clean)
        if (destination.exists()) {
            _uiState.update {
                it.copy(showRenameDialog = false, fileError = "Un élément nommé « $clean » existe déjà.")
            }
            return false
        }
        val renamed = withContext(Dispatchers.IO) {
            runCatching { source.renameTo(destination) && destination.exists() }.getOrDefault(false)
        }
        if (!renamed) {
            _uiState.update {
                it.copy(
                    showRenameDialog = false,
                    fileError = "Renommage impossible sur le disque pour « ${target.path} »."
                )
            }
            return false
        }
        val newRelative = destination.relativeTo(root).path
        remapTab(target.path, newRelative)
        ensureAncestorsExpanded(newRelative)
        _uiState.update {
            it.copy(
                showRenameDialog = false,
                selectedFileForMenu = null,
                fileError = null,
                fileMessage = "« ${target.name} » renommé en « $clean »."
            )
        }
        refreshTreeSync()
        if (_uiState.value.activeTab == newRelative) openFileSync(newRelative)
        return true
    }

    fun duplicateSelectedFile() {
        viewModelScope.launch { duplicateSelectedFileSync() }
    }

    suspend fun duplicateSelectedFileSync(): Boolean {
        val state = _uiState.value
        val target = state.selectedFileForMenu
        if (target == null || !state.hasProject) {
            _uiState.update { it.copy(showFileContextMenu = false, selectedFileForMenu = null) }
            return false
        }
        val root = File(state.projectRootPath)
        val source = File(root, target.path)
        val copyName = if (target.isDirectory) {
            "${target.name}_copie"
        } else if (target.name.contains('.')) {
            "${target.name.substringBeforeLast('.')}Copy.${target.name.substringAfterLast('.')}"
        } else {
            "${target.name}Copy"
        }
        val destination = File(source.parentFile, copyName)
        if (destination.exists()) {
            _uiState.update {
                it.copy(
                    showFileContextMenu = false,
                    selectedFileForMenu = null,
                    fileError = "Un élément nommé « $copyName » existe déjà."
                )
            }
            return false
        }
        val copied: Boolean = withContext(Dispatchers.IO) {
            try {
                if (source.isDirectory) {
                    source.copyRecursively(destination)
                } else {
                    source.copyTo(destination, overwrite = false)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
        if (!copied) {
            _uiState.update {
                it.copy(
                    showFileContextMenu = false,
                    selectedFileForMenu = null,
                    fileError = "Duplication impossible sur le disque pour « ${target.path} »."
                )
            }
            return false
        }
        val relative = destination.relativeTo(root).path
        ensureAncestorsExpanded(relative)
        _uiState.update {
            it.copy(
                showFileContextMenu = false,
                selectedFileForMenu = null,
                fileError = null,
                fileMessage = "Copie créée : $relative"
            )
        }
        refreshTreeSync()
        return true
    }

    fun deleteSelectedFile() {
        viewModelScope.launch { deleteSelectedFileSync() }
    }

    suspend fun deleteSelectedFileSync(): Boolean {
        val state = _uiState.value
        val target = state.selectedFileForMenu
        if (target == null || !state.hasProject) {
            _uiState.update { it.copy(showFileContextMenu = false, selectedFileForMenu = null) }
            return false
        }
        val root = File(state.projectRootPath)
        val file = File(root, target.path)
        val deleted = withContext(Dispatchers.IO) {
            runCatching {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }.getOrDefault(false) && !file.exists()
        }
        if (!deleted) {
            _uiState.update {
                it.copy(
                    showFileContextMenu = false,
                    selectedFileForMenu = null,
                    fileError = "Suppression impossible sur le disque pour « ${target.path} »."
                )
            }
            return false
        }
        val affectedTabs = state.openTabs.filter { it == target.path || it.startsWith("${target.path}/") }
        affectedTabs.forEach { tab ->
            unsavedBuffers.remove(tab)
            pendingSavePaths.remove(tab)
        }
        syncUnsavedToState()
        _uiState.update { st ->
            val updatedTabs = st.openTabs.filterNot { it in affectedTabs }
            val nextActive = if (st.activeTab in affectedTabs) updatedTabs.lastOrNull().orEmpty() else st.activeTab
            st.copy(
                openTabs = updatedTabs,
                activeTab = nextActive,
                showFileContextMenu = false,
                selectedFileForMenu = null,
                fileError = null,
                fileMessage = "« ${target.name} » supprimé du disque."
            )
        }
        val next = _uiState.value.activeTab
        if (next.isNotBlank()) {
            openFileSync(next)
        } else {
            _uiState.update { it.copy(codeLines = emptyList(), codeHeaderTitle = "") }
        }
        refreshTreeSync()
        return true
    }

    private fun parentDirForSelection(root: File): File {
        val selected = _uiState.value.selectedFileForMenu ?: return root
        val selectedFile = File(root, selected.path)
        return if (selected.isDirectory) selectedFile else selectedFile.parentFile ?: root
    }

    private fun remapTab(oldPath: String, newPath: String) {
        val buffered = unsavedBuffers.remove(oldPath)
        if (buffered != null) unsavedBuffers[newPath] = buffered
        if (pendingSavePaths.remove(oldPath)) pendingSavePaths.add(newPath)
        syncUnsavedToState()
        _uiState.update { st ->
            st.copy(
                openTabs = st.openTabs.map { if (it == oldPath) newPath else it },
                activeTab = if (st.activeTab == oldPath) newPath else st.activeTab,
                codeHeaderTitle = if (st.activeTab == oldPath) {
                    "${newPath.substringAfterLast('/')} · ${languageLabelFor(newPath)}"
                } else st.codeHeaderTitle
            )
        }
    }

    // ------------------------------------------------------------------
    // Recherche et remplacement réels
    // ------------------------------------------------------------------

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, searchStatusMessage = null, searchStatusIsError = false) }
        if (query.trim().length >= 2) {
            viewModelScope.launch { runSearchSync() }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), totalMatchesCount = 0) }
        }
    }

    fun updateReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun toggleMatchCase() {
        _uiState.update { it.copy(matchCase = !it.matchCase) }
        relaunchSearchIfNeeded()
    }

    fun toggleRegex() {
        _uiState.update { it.copy(regex = !it.regex) }
        relaunchSearchIfNeeded()
    }

    fun toggleWholeProject() {
        _uiState.update { it.copy(wholeProject = !it.wholeProject) }
        relaunchSearchIfNeeded()
    }

    fun toggleReplacePreview() {
        _uiState.update { it.copy(showReplacePreview = !it.showReplacePreview) }
    }

    private fun relaunchSearchIfNeeded() {
        if (_uiState.value.searchQuery.trim().length >= 2) {
            viewModelScope.launch { runSearchSync() }
        }
    }

    private fun compileSearchRegex(query: String, matchCase: Boolean, useRegex: Boolean): Regex? {
        return try {
            val pattern = if (useRegex) query else Regex.escape(query)
            if (matchCase) Regex(pattern) else Regex(pattern, setOf(RegexOption.IGNORE_CASE))
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun collectTargetFiles(wholeProject: Boolean): List<File>? {
        val state = _uiState.value
        val root = File(state.projectRootPath)
        return withContext(Dispatchers.IO) {
            if (wholeProject) {
                val files = mutableListOf<File>()
                root.walkTopDown()
                    .onEnter { it.isDirectory && it.name !in IGNORED_DIR_NAMES }
                    .forEach { f ->
                        if (files.size < MAX_SEARCH_FILES &&
                            f.isFile &&
                            f.length() in 1..MAX_FILE_BYTES &&
                            f.extension.lowercase() in TEXT_EXTENSIONS
                        ) {
                            files += f
                        }
                    }
                files
            } else {
                val active = state.activeTab.takeIf { it.isNotBlank() }?.let { File(root, it) }
                if (active != null && active.exists() && active.isFile) listOf(active) else emptyList()
            }
        }
    }

    suspend fun runSearchSync() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        if (!state.hasProject) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    totalMatchesCount = 0,
                    searchStatusMessage = "Aucun projet ouvert — la recherche nécessite un projet.",
                    searchStatusIsError = true
                )
            }
            return
        }
        if (query.length < 2) {
            _uiState.update {
                it.copy(searchResults = emptyList(), totalMatchesCount = 0, searchStatusMessage = null, searchStatusIsError = false)
            }
            return
        }
        val regex = compileSearchRegex(query, state.matchCase, state.regex)
        if (regex == null) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    totalMatchesCount = 0,
                    searchStatusMessage = "Expression régulière invalide : « $query ».",
                    searchStatusIsError = true
                )
            }
            return
        }
        val files = collectTargetFiles(state.wholeProject) ?: emptyList()
        val groups = mutableListOf<SearchResultFileGroup>()
        var totalMatches = 0
        val root = File(state.projectRootPath)
        for (file in files) {
            if (totalMatches >= MAX_TOTAL_MATCHES) break
            val content = withContext(Dispatchers.IO) {
                runCatching { file.readText() }.getOrNull()
            } ?: continue
            val relative = file.relativeTo(root).path
            val matches = mutableListOf<SearchResultMatch>()
            content.split("\n").forEachIndexed { index, line ->
                if (matches.size >= MAX_MATCHES_PER_FILE) return@forEachIndexed
                for (match in regex.findAll(line)) {
                    matches += SearchResultMatch(
                        lineNumber = index + 1,
                        prefixText = line.substring(0, match.range.first),
                        matchText = match.value,
                        suffixText = line.substring(match.range.last + 1)
                    )
                    if (matches.size >= MAX_MATCHES_PER_FILE) break
                }
            }
            if (matches.isNotEmpty()) {
                groups += SearchResultFileGroup(relative, matches.size, matches.toList())
                totalMatches += matches.size
            }
        }
        _uiState.update {
            it.copy(
                searchResults = groups,
                totalMatchesCount = totalMatches,
                searchStatusMessage = if (totalMatches == 0) "Aucun résultat trouvé dans les fichiers du projet." else null,
                searchStatusIsError = false
            )
        }
    }

    fun executeReplaceAll() {
        viewModelScope.launch { executeReplaceAllSync() }
    }

    suspend fun executeReplaceAllSync(): Boolean {
        val state = _uiState.value
        val query = state.searchQuery
        val replacement = state.replaceQuery
        if (!state.hasProject) {
            _uiState.update {
                it.copy(searchStatusMessage = "Aucun projet ouvert — le remplacement nécessite un projet.", searchStatusIsError = true)
            }
            return false
        }
        if (query.isBlank()) {
            _uiState.update {
                it.copy(searchStatusMessage = "Saisissez d'abord un texte à rechercher.", searchStatusIsError = true)
            }
            return false
        }
        if (!state.wholeProject && state.activeTab.isBlank()) {
            _uiState.update {
                it.copy(searchStatusMessage = "Ouvrez un fichier pour remplacer dans le fichier courant.", searchStatusIsError = true)
            }
            return false
        }
        val regex = compileSearchRegex(query, state.matchCase, state.regex)
        if (regex == null) {
            _uiState.update {
                it.copy(searchStatusMessage = "Expression régulière invalide : « $query ».", searchStatusIsError = true)
            }
            return false
        }
        val root = File(state.projectRootPath)
        val files = collectTargetFiles(state.wholeProject) ?: emptyList()
        var totalReplacements = 0
        var filesChanged = 0
        var activeFileChanged = false
        for (file in files) {
            val content = withContext(Dispatchers.IO) {
                runCatching { file.readText() }.getOrNull()
            } ?: continue
            val occurrences = regex.findAll(content).count()
            if (occurrences == 0) continue
            // Remplacement littéral : le texte de remplacement n'est jamais interprété ($1, etc.)
            val newContent = regex.replace(content) { replacement }
            val written = withContext(Dispatchers.IO) {
                runCatching { file.writeText(newContent) }.isSuccess
            }
            if (!written) {
                _uiState.update {
                    it.copy(
                        searchStatusMessage = "Échec d'écriture sur le disque : ${file.relativeTo(root).path}",
                        searchStatusIsError = true
                    )
                }
                return false
            }
            totalReplacements += occurrences
            filesChanged++
            if (file.relativeTo(root).path == state.activeTab) activeFileChanged = true
        }
        if (activeFileChanged) {
            unsavedBuffers.remove(state.activeTab)
            pendingSavePaths.remove(state.activeTab)
            syncUnsavedToState()
            openFileSync(state.activeTab)
        }
        _uiState.update {
            it.copy(
                searchStatusMessage = if (totalReplacements == 0) {
                    "Aucune occurrence trouvée à remplacer."
                } else {
                    "$totalReplacements remplacement(s) effectué(s) dans $filesChanged fichier(s) sur le disque."
                },
                searchStatusIsError = false,
                showReplacePreview = false
            )
        }
        runSearchSync()
        return totalReplacements > 0
    }

    // ------------------------------------------------------------------
    // Ressources réelles (res/values)
    // ------------------------------------------------------------------

    fun selectResourceTab(tab: ResourceTab) {
        _uiState.update { it.copy(activeResourceTab = tab) }
    }

    fun openAddResourceDialog() {
        _uiState.update { it.copy(showAddResourceDialog = true) }
    }

    fun dismissAddResourceDialog() {
        _uiState.update { it.copy(showAddResourceDialog = false) }
    }

    fun addStringResource(key: String, value: String) {
        viewModelScope.launch { addStringResourceSync(key, value) }
    }

    suspend fun addStringResourceSync(key: String, value: String): Boolean {
        val k = key.trim()
        val v = value.trim()
        if (k.isEmpty() || v.isEmpty()) {
            _uiState.update {
                it.copy(showAddResourceDialog = false, fileError = "Clé et valeur requises pour une chaîne.")
            }
            return false
        }
        val ok = writeResourceEntry("strings.xml", "string", k, escapeXml(v))
        if (ok) {
            _uiState.update {
                it.copy(showAddResourceDialog = false, fileError = null, fileMessage = "Chaîne « $k » ajoutée dans res/values/strings.xml.")
            }
            loadResourcesSync()
        }
        return ok
    }

    fun addColorResource(key: String, hex: String) {
        viewModelScope.launch { addColorResourceSync(key, hex) }
    }

    suspend fun addColorResourceSync(key: String, hex: String): Boolean {
        val k = key.trim()
        val normalized = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
        val hexRegex = Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$")
        if (k.isEmpty() || !hexRegex.matches(normalized)) {
            _uiState.update {
                it.copy(
                    showAddResourceDialog = false,
                    fileError = "Code hexadécimal invalide (exemple attendu : #1C2438)."
                )
            }
            return false
        }
        val ok = writeResourceEntry("colors.xml", "color", k, normalized)
        if (ok) {
            _uiState.update {
                it.copy(showAddResourceDialog = false, fileError = null, fileMessage = "Couleur « $k » ajoutée dans res/values/colors.xml.")
            }
            loadResourcesSync()
        }
        return ok
    }

    fun startEditingString(item: ResourceStringItem) {
        _uiState.update { it.copy(editingStringResource = item) }
    }

    fun dismissEditingString() {
        _uiState.update { it.copy(editingStringResource = null) }
    }

    fun saveEditedString(key: String, newValue: String) {
        viewModelScope.launch { saveEditedStringSync(key, newValue) }
    }

    suspend fun saveEditedStringSync(key: String, newValue: String): Boolean {
        val v = newValue.trim()
        if (v.isEmpty()) {
            _uiState.update { it.copy(editingStringResource = null) }
            return false
        }
        val state = _uiState.value
        if (!state.hasProject) {
            _uiState.update { it.copy(editingStringResource = null, fileError = "Aucun projet ouvert.") }
            return false
        }
        val root = File(state.projectRootPath)
        val targetFile = withContext(Dispatchers.IO) {
            findResourceFile(root, "strings.xml", key)
        }
        if (targetFile == null) {
            _uiState.update {
                it.copy(editingStringResource = null, fileError = "Chaîne « $key » introuvable dans les fichiers de ressources du projet.")
            }
            return false
        }
        val escaped = escapeXml(v)
        val success = withContext(Dispatchers.IO) {
            runCatching {
                val text = targetFile.readText()
                val entryRegex = Regex("<string\\s+name=\"$key\"[^>]*>.*?</string>", RegexOption.DOT_MATCHES_ALL)
                val match = entryRegex.find(text) ?: return@runCatching false
                val replacement = "<string name=\"$key\">$escaped</string>"
                targetFile.writeText(
                    text.substring(0, match.range.first) + replacement + text.substring(match.range.last + 1)
                )
                true
            }.getOrDefault(false)
        }
        if (!success) {
            _uiState.update {
                it.copy(editingStringResource = null, fileError = "Échec d'écriture dans ${targetFile.name}.")
            }
            return false
        }
        _uiState.update {
            it.copy(editingStringResource = null, fileError = null, fileMessage = "Chaîne « $key » mise à jour sur le disque.")
        }
        loadResourcesSync()
        return true
    }

    private suspend fun writeResourceEntry(fileName: String, tag: String, key: String, escapedValue: String): Boolean {
        val state = _uiState.value
        if (!state.hasProject) {
            _uiState.update { it.copy(fileError = "Aucun projet ouvert.") }
            return false
        }
        val root = File(state.projectRootPath)
        return try {
            withContext(Dispatchers.IO) {
                val existing = findResourceFile(root, fileName, key)
                if (existing != null) {
                    val text = existing.readText()
                    val entryRegex = Regex("<$tag\\s+name=\"$key\"[^>]*>.*?</$tag>", RegexOption.DOT_MATCHES_ALL)
                    val updated = entryRegex.find(text)?.range?.let { range ->
                        text.substring(0, range.first) + "<$tag name=\"$key\">$escapedValue</$tag>" +
                            text.substring(range.last + 1)
                    } ?: insertBeforeClosingResources(text, tag, key, escapedValue)
                    existing.writeText(updated)
                } else {
                    val dir = findOrCreateValuesDir(root)
                    val file = File(dir, fileName)
                    if (file.exists()) {
                        val text = file.readText()
                        file.writeText(insertBeforeClosingResources(text, tag, key, escapedValue))
                    } else {
                        file.parentFile?.mkdirs()
                        file.writeText(
                            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n    <$tag name=\"$key\">$escapedValue</$tag>\n</resources>\n"
                        )
                    }
                }
                true
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(fileError = "Échec d'écriture de la ressource : ${e.message ?: "erreur disque"}")
            }
            false
        }
    }

    private fun insertBeforeClosingResources(text: String, tag: String, key: String, escapedValue: String): String {
        val closing = "</resources>"
        if (!text.contains(closing)) {
            throw IllegalStateException("Fichier de ressources mal formé (</resources> absent).")
        }
        return text.replaceLast(closing) { "    <$tag name=\"$key\">$escapedValue</$tag>\n$closing" }
    }

    private fun String.replaceLast(literal: String, replacement: () -> String): String {
        val index = lastIndexOf(literal)
        if (index < 0) return this
        return substring(0, index) + replacement() + substring(index + literal.length)
    }

    private suspend fun findResourceFile(root: File, fileName: String, key: String?): File? =
        withContext(Dispatchers.IO) {
            var found: File? = null
            root.walkTopDown()
                .onEnter { it.isDirectory && it.name !in IGNORED_DIR_NAMES }
                .forEach { f ->
                    if (found == null && f.isFile && f.name == fileName &&
                        f.parentFile?.name?.startsWith("values") == true
                    ) {
                        if (key == null) {
                            found = f
                        } else {
                            val text = runCatching { f.readText() }.getOrNull()
                            if (text != null && Regex("name=\"$key\"").containsMatchIn(text)) {
                                found = f
                            }
                        }
                    }
                }
            found
        }

    private suspend fun findOrCreateValuesDir(root: File): File = withContext(Dispatchers.IO) {
        var valuesDir: File? = null
        root.walkTopDown()
            .onEnter { it.isDirectory && it.name !in IGNORED_DIR_NAMES }
            .forEach { d ->
                if (valuesDir == null && d.isDirectory && d.name == "values") valuesDir = d
            }
        valuesDir ?: File(root, "app/src/main/res/values").also { it.mkdirs() }
    }

    private suspend fun loadResourcesSync() {
        val state = _uiState.value
        if (!state.hasProject) return
        val root = File(state.projectRootPath)
        val result = withContext(Dispatchers.IO) {
            val strings = mutableListOf<ResourceStringItem>()
            val colors = mutableListOf<ResourceColorItem>()
            val dims = mutableListOf<ResourceDimensionItem>()
            var themes: String? = null
            val stringRegex = Regex("<string\\s+name=\"([^\"]+)\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
            val colorRegex = Regex("<color\\s+name=\"([^\"]+)\"[^>]*>(.*?)</color>", RegexOption.DOT_MATCHES_ALL)
            val dimenRegex = Regex("<dimen\\s+name=\"([^\"]+)\"[^>]*>(.*?)</dimen>", RegexOption.DOT_MATCHES_ALL)
            root.walkTopDown()
                .onEnter { it.isDirectory && it.name !in IGNORED_DIR_NAMES }
                .forEach { f ->
                    if (!f.isFile) return@forEach
                    val parentName = f.parentFile?.name ?: return@forEach
                    if (!parentName.startsWith("values")) return@forEach
                    val text = runCatching { f.readText() }.getOrNull() ?: return@forEach
                    when (f.name) {
                        "strings.xml" -> stringRegex.findAll(text).forEach { m ->
                            strings += ResourceStringItem(m.groupValues[1], unescapeXml(m.groupValues[2]).trim())
                        }
                        "colors.xml" -> colorRegex.findAll(text).forEach { m ->
                            colors += ResourceColorItem(m.groupValues[1], unescapeXml(m.groupValues[2]).trim())
                        }
                        "dimens.xml" -> dimenRegex.findAll(text).forEach { m ->
                            dims += ResourceDimensionItem(m.groupValues[1], unescapeXml(m.groupValues[2]).trim())
                        }
                        "themes.xml" -> if (themes == null) themes = text
                    }
                }
            Triple(strings.toList(), colors.toList(), dims.toList()) to themes
        }
        val (lists, themes) = result
        _uiState.update {
            it.copy(
                stringResources = lists.first,
                colorResources = lists.second,
                dimensionResources = lists.third,
                themesFileContent = themes
            )
        }
    }

    fun toggleThemesPreviewDialog(show: Boolean) {
        _uiState.update { it.copy(showThemesPreviewDialog = show) }
    }

    // ------------------------------------------------------------------
    // Dépendances Gradle réelles (parse du build.gradle(.kts) du projet)
    // ------------------------------------------------------------------

    fun updateDependencyInput(input: String) {
        _uiState.update { it.copy(newDependencyInput = input, gradleSyncError = null) }
    }

    fun syncGradleDependencies() {
        viewModelScope.launch { syncGradleDependenciesSync() }
    }

    suspend fun syncGradleDependenciesSync() {
        val state = _uiState.value
        if (!state.hasProject) {
            _uiState.update {
                it.copy(gradleSyncSuccess = false, gradleSyncError = "Aucun projet ouvert.")
            }
            return
        }
        val root = File(state.projectRootPath)
        val deps = withContext(Dispatchers.IO) { parseGradleDependencies(root) }
        if (deps == null) {
            _uiState.update {
                it.copy(
                    gradleDependencies = emptyList(),
                    gradleSyncSuccess = false,
                    gradleSyncError = "Aucun fichier build.gradle(.kts) trouvé dans le projet."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                gradleDependencies = deps,
                gradleSyncSuccess = true,
                gradleSyncError = null,
                gradleSyncMessage = if (deps.isEmpty()) {
                    "Dépendances rechargées depuis build.gradle.kts — aucune dépendance déclarée."
                } else {
                    "Dépendances rechargées depuis build.gradle.kts — ${deps.size} dépendance(s) réelle(s)."
                }
            )
        }
    }

    fun addGradleDependency() {
        viewModelScope.launch { addGradleDependencySync() }
    }

    suspend fun addGradleDependencySync(): Boolean {
        val state = _uiState.value
        val raw = state.newDependencyInput.trim()
        if (!state.hasProject) {
            _uiState.update { it.copy(gradleSyncError = "Aucun projet ouvert.") }
            return false
        }
        val parts = raw.split(':')
        if (raw.isEmpty() || parts.size < 3 || parts.any { it.isBlank() }) {
            _uiState.update {
                it.copy(gradleSyncError = "Coordonnée invalide — format attendu : groupe:artefact:version.")
            }
            return false
        }
        val root = File(state.projectRootPath)
        val buildFile = withContext(Dispatchers.IO) { findBuildFile(root) }
        if (buildFile == null) {
            _uiState.update {
                it.copy(gradleSyncError = "Aucun fichier build.gradle(.kts) trouvé dans le projet.")
            }
            return false
        }
        val insertedLine = if (buildFile.name.endsWith(".kts")) {
            "    implementation(\"$raw\")\n"
        } else {
            "    implementation '$raw'\n"
        }
        val success = withContext(Dispatchers.IO) {
            runCatching {
                val text = buildFile.readText()
                val depsBlockRegex = Regex("""dependencies\s*\{""")
                val updated = depsBlockRegex.find(text)?.range?.let { range ->
                    text.substring(0, range.first) + "dependencies {\n$insertedLine" + text.substring(range.last + 1)
                } ?: (text + "\ndependencies {\n$insertedLine}\n")
                buildFile.writeText(updated)
            }.isSuccess
        }
        if (!success) {
            _uiState.update {
                it.copy(gradleSyncError = "Échec d'écriture dans ${buildFile.relativeTo(root).path}.")
            }
            return false
        }
        _uiState.update { it.copy(newDependencyInput = "") }
        syncGradleDependenciesSync()
        _uiState.update {
            it.copy(gradleSyncMessage = "Dépendance « $raw » ajoutée dans ${buildFile.relativeTo(root).path}.")
        }
        return true
    }

    private fun findBuildFile(root: File): File? {
        var found: File? = null
        root.walkTopDown()
            .onEnter { it.isDirectory && it.name !in IGNORED_DIR_NAMES }
            .forEach { f ->
                if (found == null && f.isFile &&
                    (f.name == "build.gradle.kts" || f.name == "build.gradle")
                ) {
                    found = f
                }
            }
        return found
    }

    private fun parseGradleDependencies(root: File): List<GradleDependencyItem>? {
        val buildFiles = mutableListOf<File>()
        root.walkTopDown()
            .onEnter { it.isDirectory && it.name !in IGNORED_DIR_NAMES }
            .forEach { f ->
                if (buildFiles.size < 5 && f.isFile &&
                    (f.name == "build.gradle.kts" || f.name == "build.gradle")
                ) {
                    buildFiles += f
                }
            }
        if (buildFiles.isEmpty()) return null
        val deps = mutableListOf<GradleDependencyItem>()
        for (buildFile in buildFiles) {
            val text = runCatching { buildFile.readText() }.getOrNull() ?: continue
            GRADLE_ENTRY_REGEX.findAll(text).forEach { match ->
                val coords = match.groupValues[2]
                val parts = coords.split(':')
                if (parts.size >= 3 && parts[0].isNotBlank() && parts[1].isNotBlank() && parts[2].isNotBlank()) {
                    deps += GradleDependencyItem(
                        name = "${parts[0]}:${parts[1]}",
                        version = parts[2],
                        configuration = match.groupValues[1]
                    )
                }
            }
        }
        return deps
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun languageLabelFor(fileName: String): String = when (fileName.substringAfterLast('.').lowercase()) {
        "kt" -> "Kotlin"
        "java" -> "Java"
        "xml" -> "XML"
        "gradle", "kts" -> "Gradle"
        "properties" -> "Properties"
        "md" -> "Markdown"
        "json" -> "JSON"
        "yaml", "yml" -> "YAML"
        "txt" -> "Texte"
        else -> "Texte"
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "\\'")
        .replace("\"", "\\\"")

    private fun unescapeXml(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
