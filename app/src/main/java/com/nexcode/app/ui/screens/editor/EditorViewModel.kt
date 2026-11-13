package com.nexcode.app.ui.screens.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.entity.FileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    context: Context? = null
) : ViewModel() {

    private val database = context?.let { NexcodeDatabase.getInstance(it) }
    private val fileDao = database?.fileDao()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        initializeDefaultState()
    }

    private fun initializeDefaultState() {
        val initialTree = listOf(
            FileTreeItem("app", "app", isDirectory = true, indentLevel = 0, countOrDesc = "3 sous-dossiers"),
            FileTreeItem("src", "app/src", isDirectory = true, indentLevel = 1, countOrDesc = "2"),
            FileTreeItem("main", "app/src/main", isDirectory = true, indentLevel = 2, countOrDesc = "4"),
            FileTreeItem("MainActivity.kt", "app/src/main/java/com/nimbus/meteo/MainActivity.kt", isDirectory = false, indentLevel = 3, isModified = true, isSelected = true),
            FileTreeItem("CityRepository.kt", "app/src/main/java/com/nimbus/meteo/CityRepository.kt", isDirectory = false, indentLevel = 3),
            FileTreeItem("Theme.kt", "app/src/main/java/com/nimbus/meteo/Theme.kt", isDirectory = false, indentLevel = 3, isModified = true),
            FileTreeItem("City.kt", "app/src/main/java/com/nimbus/meteo/City.kt", isDirectory = false, indentLevel = 3),
            FileTreeItem("res", "app/src/main/res", isDirectory = true, indentLevel = 2, countOrDesc = "6"),
            FileTreeItem("values · strings.xml", "app/src/main/res/values/strings.xml", isDirectory = false, indentLevel = 3, isModified = true),
            FileTreeItem("values · colors.xml", "app/src/main/res/values/colors.xml", isDirectory = false, indentLevel = 3),
            FileTreeItem("mipmap · ic_launcher.xml", "app/src/main/res/mipmap/ic_launcher.xml", isDirectory = false, indentLevel = 3),
            FileTreeItem("AndroidManifest.xml", "app/src/main/AndroidManifest.xml", isDirectory = false, indentLevel = 2),
            FileTreeItem("build.gradle.kts", "app/build.gradle.kts", isDirectory = false, indentLevel = 1),
            FileTreeItem("settings.gradle.kts", "settings.gradle.kts", isDirectory = false, indentLevel = 1)
        )

        val lines = listOf(
            CodeLine(1, "package com.nimbus.meteo"),
            CodeLine(2, ""),
            CodeLine(3, "import android.os.Bundle"),
            CodeLine(4, "import androidx.activity.ComponentActivity"),
            CodeLine(5, "import androidx.activity.compose.setContent"),
            CodeLine(6, "import androidx.compose.material3.MaterialTheme"),
            CodeLine(7, "import androidx.compose.runtime.Composable"),
            CodeLine(8, ""),
            CodeLine(9, "class MainActivity : ComponentActivity() {"),
            CodeLine(10, "    override fun onCreate(savedInstanceState: Bundle?) {"),
            CodeLine(11, "        super.onCreate(savedInstanceState)"),
            CodeLine(12, "        setContent {"),
            CodeLine(13, "            MaterialTheme(colorScheme = AppTheme.dark) {"),
            CodeLine(14, "                CityScreen(city = \"Lyon\")"),
            CodeLine(15, "            }"),
            CodeLine(16, "        }"),
            CodeLine(17, "    }"),
            CodeLine(18, "}"),
            CodeLine(19, ""),
            CodeLine(20, "@Composable"),
            CodeLine(21, "fun CityScreen(city: String) {"),
            CodeLine(22, "    Column(Modifier.padding(16.dp)) {"),
            CodeLine(23, "        Text(text = stringResource(R.string.title_detail),", hasError = true, errorSpanText = "title_detail"),
            CodeLine(24, "            style = MaterialTheme.typography.headlineSmall)"),
            CodeLine(25, "    }"),
            CodeLine(26, "}"),
            CodeLine(27, "// TODO : extraire la ville des préférences")
        )

        val results = listOf(
            SearchResultFileGroup(
                fileName = "MainActivity.kt",
                matchCount = 2,
                matches = listOf(
                    SearchResultMatch(23, "Text(text = ", "R.string.title_", "detail)"),
                    SearchResultMatch(41, "Text(text = ", "R.string.title_", "home)")
                )
            ),
            SearchResultFileGroup(
                fileName = "CityRepository.kt",
                matchCount = 1,
                matches = listOf(
                    SearchResultMatch(58, "name = ", "R.string.title_", "settings")
                )
            ),
            SearchResultFileGroup(
                fileName = "strings.xml",
                matchCount = 1,
                matches = listOf(
                    SearchResultMatch(12, "<string name=\"", "title_", "detail\">")
                )
            )
        )

        val strings = listOf(
            ResourceStringItem("app_name", "Nimbus Météo"),
            ResourceStringItem("title_details", "Détails météo"),
            ResourceStringItem("title_home", "Mes villes"),
            ResourceStringItem("err_network", "Connexion perdue — réessayez"),
            ResourceStringItem("unit_wind", "Vent"),
            ResourceStringItem("unit_humidity", "Humidité")
        )

        val colors = listOf(
            ResourceColorItem("accent_primary", "#3DDCFF"),
            ResourceColorItem("accent_secondary", "#7C5CFF"),
            ResourceColorItem("background_dark", "#0B0F17"),
            ResourceColorItem("error_red", "#FF5C7A")
        )

        val dimensions = listOf(
            ResourceDimensionItem("spacing_md", "16 dp"),
            ResourceDimensionItem("radius_card", "12 dp"),
            ResourceDimensionItem("text_body", "14 sp")
        )

        val deps = listOf(
            GradleDependencyItem("androidx.compose:bom", "2024.09.00", "À jour", DependencyStatusVariant.UP_TO_DATE),
            GradleDependencyItem("androidx.compose.material3", "1.3.0", "À jour", DependencyStatusVariant.UP_TO_DATE),
            GradleDependencyItem("com.squareup.retrofit2:retrofit", "2.11.0", "2.12.0 dispo", DependencyStatusVariant.WARN_UPDATE),
            GradleDependencyItem("io.coil-kt:coil-compose", "2.7.0", "À jour", DependencyStatusVariant.UP_TO_DATE),
            GradleDependencyItem("org.jetbrains.kotlinx:serialization", "1.7.3", "À jour", DependencyStatusVariant.UP_TO_DATE),
            GradleDependencyItem("org.jetbrains.kotlin:kotlin-stdlib", "2.0.20", "Conflit 2.0.21", DependencyStatusVariant.CONFLICT)
        )

        _uiState.update {
            it.copy(
                treeItems = initialTree,
                codeLines = lines,
                searchResults = results,
                totalMatchesCount = 4,
                stringResources = strings,
                colorResources = colors,
                dimensionResources = dimensions,
                gradleDependencies = deps
            )
        }
    }

    private val undoStack = ArrayDeque<List<CodeLine>>()
    private val redoStack = ArrayDeque<List<CodeLine>>()

    fun selectFile(item: FileTreeItem) {
        _uiState.update { state ->
            val updatedTree = state.treeItems.map {
                it.copy(isSelected = it.path == item.path)
            }
            val openTabs = if (item.name !in state.openTabs && !item.isDirectory) {
                state.openTabs + item.name
            } else {
                state.openTabs
            }
            val nextActive = if (!item.isDirectory) item.name else state.activeTab
            state.copy(
                treeItems = updatedTree,
                openTabs = openTabs,
                activeTab = nextActive,
                codeHeaderTitle = if (!item.isDirectory) "${item.name} · Kotlin" else state.codeHeaderTitle
            )
        }
    }

    fun openTab(tabName: String) {
        _uiState.update { state ->
            state.copy(
                activeTab = tabName,
                codeHeaderTitle = "$tabName · ${if (tabName.endsWith(".xml")) "XML" else "Kotlin"}"
            )
        }
    }

    fun addScratchTab() {
        _uiState.update { state ->
            val candidate = if ("City.kt" !in state.openTabs) {
                "City.kt"
            } else {
                var idx = 1
                while ("Helper$idx.kt" in state.openTabs) idx++
                "Helper$idx.kt"
            }
            state.copy(
                openTabs = state.openTabs + candidate,
                activeTab = candidate,
                codeHeaderTitle = "$candidate · Kotlin"
            )
        }
    }

    fun closeTab(tabName: String) {
        _uiState.update { state ->
            val updated = state.openTabs.filter { it != tabName }
            val nextActive = if (state.activeTab == tabName) {
                updated.lastOrNull() ?: ""
            } else {
                state.activeTab
            }
            state.copy(openTabs = updated, activeTab = nextActive)
        }
    }

    fun showContextMenu(file: FileTreeItem) {
        _uiState.update { it.copy(showFileContextMenu = true, selectedFileForMenu = file) }
    }

    fun hideContextMenu() {
        _uiState.update { it.copy(showFileContextMenu = false) }
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
        val cleanName = rawName.trim()
        if (cleanName.isEmpty()) {
            dismissNewEntryDialog()
            return
        }
        _uiState.update { state ->
            val newItem = FileTreeItem(
                name = cleanName,
                path = if (isDirectory) "app/src/main/$cleanName" else "app/src/main/java/com/nimbus/meteo/$cleanName",
                isDirectory = isDirectory,
                indentLevel = if (isDirectory) 2 else 3,
                countOrDesc = if (isDirectory) "0" else null,
                isModified = !isDirectory,
                isSelected = true
            )
            val updated = state.treeItems.map { it.copy(isSelected = false) } + newItem
            val tabs = if (!isDirectory && cleanName !in state.openTabs) state.openTabs + cleanName else state.openTabs
            state.copy(
                treeItems = updated,
                openTabs = tabs,
                activeTab = if (!isDirectory) cleanName else state.activeTab,
                showNewEntryDialog = false
            )
        }
    }

    fun openRenameDialog() {
        _uiState.update { it.copy(showFileContextMenu = false, showRenameDialog = true) }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = false, selectedFileForMenu = null) }
    }

    fun renameSelectedFile(newName: String) {
        val clean = newName.trim()
        val target = _uiState.value.selectedFileForMenu ?: return
        if (clean.isEmpty()) {
            dismissRenameDialog()
            return
        }
        _uiState.update { state ->
            val updatedTree = state.treeItems.map {
                if (it.path == target.path) it.copy(name = clean, isModified = true) else it
            }
            val updatedTabs = state.openTabs.map { if (it == target.name) clean else it }
            val nextActive = if (state.activeTab == target.name) clean else state.activeTab
            state.copy(
                treeItems = updatedTree,
                openTabs = updatedTabs,
                activeTab = nextActive,
                showRenameDialog = false,
                selectedFileForMenu = null
            )
        }
    }

    fun duplicateSelectedFile() {
        val target = _uiState.value.selectedFileForMenu ?: return
        _uiState.update { state ->
            val copyName = if (target.name.contains(".")) {
                val base = target.name.substringBeforeLast(".")
                val ext = target.name.substringAfterLast(".")
                "${base}Copy.$ext"
            } else {
                "${target.name}_copy"
            }
            val copyItem = target.copy(
                name = copyName,
                path = "${target.path}_copy",
                isModified = true,
                isSelected = true
            )
            val updatedTree = state.treeItems.map { it.copy(isSelected = false) } + copyItem
            state.copy(
                treeItems = updatedTree,
                showFileContextMenu = false,
                selectedFileForMenu = null
            )
        }
    }

    fun deleteSelectedFile() {
        val target = _uiState.value.selectedFileForMenu ?: return
        _uiState.update { state ->
            val updatedTree = state.treeItems.filterNot { it.path == target.path }
            val updatedTabs = state.openTabs.filterNot { it == target.name }
            val nextActive = if (state.activeTab == target.name) updatedTabs.firstOrNull() ?: "" else state.activeTab
            state.copy(
                treeItems = updatedTree,
                openTabs = updatedTabs,
                activeTab = nextActive,
                showFileContextMenu = false,
                selectedFileForMenu = null
            )
        }
    }

    fun insertSymbol(symbol: String) {
        val inserted = when (symbol) {
            "⇥" -> "    "
            else -> symbol
        }
        _uiState.update { state ->
            if (state.codeLines.isEmpty()) return@update state
            undoStack.addLast(state.codeLines)
            redoStack.clear()
            val targetIdx = state.codeLines.indexOfLast { it.content.isNotBlank() }.coerceAtLeast(0)
            val updated = state.codeLines.mapIndexed { idx, line ->
                if (idx == targetIdx) line.copy(content = line.content + inserted) else line
            }
            state.copy(
                codeLines = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        _uiState.update { state ->
            redoStack.addLast(state.codeLines)
            state.copy(
                codeLines = previous,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        _uiState.update { state ->
            undoStack.addLast(state.codeLines)
            state.copy(
                codeLines = next,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun toggleMatchCase() {
        _uiState.update { it.copy(matchCase = !it.matchCase) }
    }

    fun toggleRegex() {
        _uiState.update { it.copy(regex = !it.regex) }
    }

    fun toggleWholeProject() {
        _uiState.update { it.copy(wholeProject = !it.wholeProject) }
    }

    fun toggleReplacePreview() {
        _uiState.update { it.copy(showReplacePreview = !it.showReplacePreview) }
    }

    fun executeReplaceAll() {
        _uiState.update { state ->
            undoStack.addLast(state.codeLines)
            redoStack.clear()
            val updatedLines = state.codeLines.map { line ->
                if (line.number == 23) {
                    line.copy(
                        content = "        Text(text = stringResource(R.string.title_details),",
                        hasError = false,
                        errorSpanText = null
                    )
                } else line
            }
            state.copy(
                codeLines = updatedLines,
                errorCount = 0,
                searchResults = emptyList(),
                totalMatchesCount = 0,
                showReplacePreview = false,
                canUndo = true
            )
        }
    }

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
        val k = key.trim()
        val v = value.trim()
        if (k.isEmpty() || v.isEmpty()) {
            dismissAddResourceDialog()
            return
        }
        _uiState.update { state ->
            state.copy(
                stringResources = state.stringResources + ResourceStringItem(k, v),
                showAddResourceDialog = false
            )
        }
    }

    fun addColorResource(key: String, hex: String) {
        val k = key.trim()
        val h = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
        if (k.isEmpty() || h.length < 4) {
            dismissAddResourceDialog()
            return
        }
        _uiState.update { state ->
            state.copy(
                colorResources = state.colorResources + ResourceColorItem(k, h),
                showAddResourceDialog = false
            )
        }
    }

    fun startEditingString(item: ResourceStringItem) {
        _uiState.update { it.copy(editingStringResource = item) }
    }

    fun dismissEditingString() {
        _uiState.update { it.copy(editingStringResource = null) }
    }

    fun saveEditedString(key: String, newValue: String) {
        val v = newValue.trim()
        if (v.isEmpty()) {
            dismissEditingString()
            return
        }
        _uiState.update { state ->
            state.copy(
                stringResources = state.stringResources.map { if (it.key == key) it.copy(value = v) else it },
                editingStringResource = null
            )
        }
    }

    fun completeTranslations() {
        _uiState.update { it.copy(showTranslationsSyncedBanner = true) }
    }

    fun toggleThemesPreviewDialog(show: Boolean) {
        _uiState.update { it.copy(showThemesPreviewDialog = show) }
    }

    fun updateDependencyInput(input: String) {
        _uiState.update { it.copy(newDependencyInput = input, gradleSyncSuccess = false) }
    }

    fun syncGradleDependencies() {
        _uiState.update { state ->
            val raw = state.newDependencyInput.trim()
            val withNew = if (raw.isNotEmpty() && raw.contains(":")) {
                val lastColon = raw.lastIndexOf(':')
                val artifact = raw.substring(0, lastColon)
                val version = raw.substring(lastColon + 1).ifBlank { "1.0.0" }
                state.gradleDependencies + GradleDependencyItem(
                    name = artifact,
                    version = version,
                    statusText = "Ajoutée",
                    statusVariant = DependencyStatusVariant.UP_TO_DATE
                )
            } else {
                state.gradleDependencies
            }
            val resolved = withNew.map { dep ->
                if (dep.name == "org.jetbrains.kotlin:kotlin-stdlib") {
                    dep.copy(
                        version = "2.0.21",
                        statusText = "Alignée",
                        statusVariant = DependencyStatusVariant.UP_TO_DATE
                    )
                } else dep
            }
            state.copy(
                gradleDependencies = resolved,
                newDependencyInput = "",
                gradleSyncSuccess = true
            )
        }
    }
}
