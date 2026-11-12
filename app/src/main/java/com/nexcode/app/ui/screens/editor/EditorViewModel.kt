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
            state.copy(
                treeItems = updatedTree,
                openTabs = openTabs,
                activeTab = if (!item.isDirectory) item.name else state.activeTab
            )
        }
    }

    fun openTab(tabName: String) {
        _uiState.update { state ->
            state.copy(activeTab = tabName)
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
        _uiState.update { it.copy(showFileContextMenu = false, selectedFileForMenu = null) }
    }

    fun insertSymbol(symbol: String) {
        // Ajoute ou modifie le code en simulation
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

    fun executeReplaceAll() {
        _uiState.update { state ->
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
                totalMatchesCount = 0
            )
        }
    }

    fun selectResourceTab(tab: ResourceTab) {
        _uiState.update { it.copy(activeResourceTab = tab) }
    }

    fun updateDependencyInput(input: String) {
        _uiState.update { it.copy(newDependencyInput = input) }
    }
}
