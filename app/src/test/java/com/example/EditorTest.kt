package com.example

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.ui.screens.editor.EditorViewModel
import com.nexcode.app.ui.screens.projects.ProjectBuildStatus
import com.nexcode.app.ui.screens.projects.ProjectItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/**
 * Tests du ViewModel éditeur avec un VRAI dossier de projet temporaire :
 * aucune donnée « Nimbus Météo », tous les scénarios manipulent le disque réel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorTest {



    private lateinit var viewModel: EditorViewModel
    private lateinit var projectDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        viewModel = EditorViewModel(context)

        // Arborescence réelle créée dans un dossier temporaire
        projectDir = Files.createTempDirectory("nexcode_editor_test").toFile()
        File(projectDir, "app/src/main/java/com/demo").mkdirs()
        File(projectDir, "res/values").mkdirs()
        File(projectDir, ".git/objects").mkdirs()
        File(projectDir, "build/outputs").mkdirs()

        File(projectDir, "app/src/main/java/com/demo/MainActivity.kt").writeText(
            "package com.demo\n\nfun greeting(): String {\n    return \"Bonjour\"\n}\n"
        )
        File(projectDir, "app/src/main/java/com/demo/Theme.kt").writeText("package com.demo\n")
        File(projectDir, "res/values/strings.xml").writeText(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <string name=\"app_name\">Démo</string>\n" +
                "    <string name=\"title_home\">Accueil</string>\n" +
                "</resources>\n"
        )
        File(projectDir, "app/build.gradle.kts").writeText(
            "plugins {\n    id(\"com.android.application\")\n}\n\n" +
                "dependencies {\n" +
                "    implementation(\"com.squareup.retrofit2:retrofit:2.11.0\")\n" +
                "    implementation(\"androidx.core:core-ktx:1.13.1\")\n" +
                "}\n"
        )
        File(projectDir, ".git/config").writeText("junk")
        File(projectDir, "build/outputs/out.txt").writeText("junk")
    }

    private fun fakeProject(localPath: String = projectDir.absolutePath) = ProjectItem(
        id = 1L,
        name = "Démo",
        packageName = "com.demo",
        initials = "D",
        language = "Kotlin",
        uiFramework = "Compose",
        modifiedText = "",
        buildStatus = ProjectBuildStatus.DRAFT,
        buildStatusLabel = "",
        localPath = localPath
    )

    @Test
    fun testBindProjectNullShowsHonestEmptyState() = runBlocking {
        viewModel.bindProjectSync(null)
        val state = viewModel.uiState.value
        assertFalse(state.hasProject)
        assertEquals("", state.projectName)
        assertTrue(state.treeItems.isEmpty())
        assertTrue(state.codeLines.isEmpty())
        assertTrue(state.searchResults.isEmpty())
        assertTrue(state.gradleDependencies.isEmpty())
        assertTrue(state.stringResources.isEmpty())
    }

    @Test
    fun testBindProjectMissingFolderShowsHonestError() = runBlocking {
        viewModel.bindProjectSync(fakeProject(localPath = "/chemin/inexistant/nexcode"))
        val state = viewModel.uiState.value
        assertFalse(state.hasProject)
        assertNotNull(state.fileError)
    }

    @Test
    fun testBindProjectLoadsRealTreeIgnoringGitAndBuild() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        val state = viewModel.uiState.value
        assertTrue(state.hasProject)
        assertEquals("Démo", state.projectName)

        val paths = state.treeItems.map { it.path }
        assertTrue(paths.contains("app/src/main/java/com/demo/MainActivity.kt"))
        assertTrue(paths.contains("app/src/main/java/com/demo/Theme.kt"))
        assertTrue(paths.contains("app/build.gradle.kts"))
        assertTrue(paths.contains("res/values/strings.xml"))

        // Dossiers réellement ignorés : .git et build
        assertFalse(paths.any { it.startsWith(".git") })
        assertFalse(paths.any { it.startsWith("build") })

        // Les dossiers sont marqués comme répertoires avec un compte réel d'entrées
        val appDir = state.treeItems.first { it.path == "app" }
        assertTrue(appDir.isDirectory)
        assertEquals("2", appDir.countOrDesc)
    }

    @Test
    fun testOpenRealFileReadsDiskContent() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        viewModel.openFileSync("app/src/main/java/com/demo/MainActivity.kt")

        val state = viewModel.uiState.value
        assertEquals("app/src/main/java/com/demo/MainActivity.kt", state.activeTab)
        assertEquals("package com.demo", state.codeLines.first().content)
        assertEquals(1, state.codeLines.first().number)
        assertTrue(state.codeLines.any { it.content.contains("Bonjour") })
        assertTrue(state.openTabs.contains("app/src/main/java/com/demo/MainActivity.kt"))
    }

    @Test
    fun testEditLineAndSaveWritesToDisk() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        viewModel.openFileSync("app/src/main/java/com/demo/MainActivity.kt")

        val target = viewModel.uiState.value.codeLines.first { it.content.contains("Bonjour") }
        viewModel.updateCodeLine(target.number, "    return \"Salut\"")

        // Tampon marqué non enregistré
        assertTrue(viewModel.uiState.value.unsavedPaths.contains("app/src/main/java/com/demo/MainActivity.kt"))

        assertTrue(viewModel.saveActiveFileSync())

        // Le disque contient réellement la modification
        val diskText = File(projectDir, "app/src/main/java/com/demo/MainActivity.kt").readText()
        assertTrue(diskText.contains("Salut"))
        assertFalse(diskText.contains("Bonjour"))

        // Tampon réinitialisé après enregistrement
        assertFalse(viewModel.uiState.value.unsavedPaths.contains("app/src/main/java/com/demo/MainActivity.kt"))
    }

    @Test
    fun testSearchFindsRealOccurrencesWithRealLineNumbers() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        viewModel.updateSearchQuery("Bonjour")
        viewModel.runSearchSync()

        val state = viewModel.uiState.value
        assertEquals(1, state.searchResults.size)
        val group = state.searchResults.first()
        assertEquals("app/src/main/java/com/demo/MainActivity.kt", group.fileName)
        assertEquals(1, group.matchCount)
        assertEquals(4, group.matches.first().lineNumber)
        assertEquals(1, state.totalMatchesCount)
    }

    @Test
    fun testSearchCaseInsensitiveToggle() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        viewModel.updateSearchQuery("BONJOUR")
        viewModel.runSearchSync()
        assertEquals(0, viewModel.uiState.value.totalMatchesCount)

        viewModel.toggleMatchCase()
        viewModel.runSearchSync()
        assertEquals(1, viewModel.uiState.value.totalMatchesCount)
    }

    @Test
    fun testReplaceAllWritesReplacedContentToDisk() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        viewModel.openFileSync("app/src/main/java/com/demo/MainActivity.kt")
        viewModel.updateSearchQuery("Bonjour")
        viewModel.updateReplaceQuery("Salut")
        viewModel.runSearchSync()

        assertTrue(viewModel.executeReplaceAllSync())

        // Le disque est réellement modifié
        val diskText = File(projectDir, "app/src/main/java/com/demo/MainActivity.kt").readText()
        assertTrue(diskText.contains("Salut"))
        assertFalse(diskText.contains("Bonjour"))

        // La recherche relancée ne trouve plus d'occurrence
        assertEquals(0, viewModel.uiState.value.totalMatchesCount)
    }

    @Test
    fun testCreateRenameDuplicateDeleteAreRealDiskOperations() = runBlocking {
        viewModel.bindProjectSync(fakeProject())

        // Création réelle
        assertTrue(viewModel.createNewEntrySync("Notes.md", isDirectory = false))
        assertTrue(File(projectDir, "Notes.md").exists())
        assertEquals("Notes.md", viewModel.uiState.value.activeTab.substringAfterLast('/'))

        // Renommage réel
        val notesItem = viewModel.uiState.value.treeItems.first { it.path == "Notes.md" }
        viewModel.showContextMenu(notesItem)
        assertTrue(viewModel.renameSelectedFileSync("Aide.md"))
        assertFalse(File(projectDir, "Notes.md").exists())
        assertTrue(File(projectDir, "Aide.md").exists())

        // Duplication réelle
        val aideItem = viewModel.uiState.value.treeItems.first { it.path == "Aide.md" }
        viewModel.showContextMenu(aideItem)
        assertTrue(viewModel.duplicateSelectedFileSync())
        assertTrue(File(projectDir, "AideCopy.md").exists())

        // Suppression réelle
        val copyItem = viewModel.uiState.value.treeItems.first { it.path == "AideCopy.md" }
        viewModel.showContextMenu(copyItem)
        assertTrue(viewModel.deleteSelectedFileSync())
        assertFalse(File(projectDir, "AideCopy.md").exists())
    }

    @Test
    fun testCreateDuplicateNameFailsHonest() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        File(projectDir, "Existant.md").writeText("contenu")

        assertFalse(viewModel.createNewEntrySync("Existant.md", isDirectory = false))
        assertNotNull(viewModel.uiState.value.fileError)
    }

    @Test
    fun testDirectoryCreationIsReal() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        assertTrue(viewModel.createNewEntrySync("docs", isDirectory = true))
        assertTrue(File(projectDir, "docs").isDirectory)
    }

    @Test
    fun testGradleDependenciesParsedFromRealBuildFile() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        val state = viewModel.uiState.value
        assertTrue(state.gradleSyncSuccess)
        assertNull(state.gradleSyncError)
        assertEquals(2, state.gradleDependencies.size)
        assertTrue(state.gradleDependencies.any {
            it.name == "com.squareup.retrofit2:retrofit" && it.version == "2.11.0"
        })
        assertTrue(state.gradleDependencies.any {
            it.name == "androidx.core:core-ktx" && it.version == "1.13.1"
        })
    }

    @Test
    fun testAddGradleDependencyWritesRealFile() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        viewModel.updateDependencyInput("com.squareup.okhttp3:okhttp:4.12.0")
        assertTrue(viewModel.addGradleDependencySync())

        val diskText = File(projectDir, "app/build.gradle.kts").readText()
        assertTrue(diskText.contains("implementation(\"com.squareup.okhttp3:okhttp:4.12.0\")"))
        assertEquals(3, viewModel.uiState.value.gradleDependencies.size)
    }

    @Test
    fun testStringResourcesParsedFromRealStringsXml() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        val strings = viewModel.uiState.value.stringResources
        assertTrue(strings.any { it.key == "app_name" && it.value == "Démo" })
        assertTrue(strings.any { it.key == "title_home" && it.value == "Accueil" })
    }

    @Test
    fun testAddStringResourceWritesRealFile() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        assertTrue(viewModel.addStringResourceSync("unit_test", "Valeur réelle"))
        assertTrue(File(projectDir, "res/values/strings.xml").readText().contains("unit_test"))

        // Rechargement réel : la ressource apparaît dans l'état
        viewModel.bindProjectSync(fakeProject())
        assertTrue(viewModel.uiState.value.stringResources.any { it.key == "unit_test" && it.value == "Valeur réelle" })
    }

    @Test
    fun testEditStringWritesRealFile() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        assertTrue(viewModel.saveEditedStringSync("title_home", "Ma page"))
        val diskText = File(projectDir, "res/values/strings.xml").readText()
        assertTrue(diskText.contains("Ma page"))
    }

    @Test
    fun testInvalidHexColorIsRejected() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        assertFalse(viewModel.addColorResourceSync("bad_color", "zzz"))
        assertNotNull(viewModel.uiState.value.fileError)
        assertTrue(viewModel.uiState.value.colorResources.none { it.key == "bad_color" })
    }

    @Test
    fun testResourceTabsSwitching() {
        viewModel.selectResourceTab(com.nexcode.app.ui.screens.editor.ResourceTab.COLORS)
        assertEquals(com.nexcode.app.ui.screens.editor.ResourceTab.COLORS, viewModel.uiState.value.activeResourceTab)

        viewModel.selectResourceTab(com.nexcode.app.ui.screens.editor.ResourceTab.THEMES)
        assertEquals(com.nexcode.app.ui.screens.editor.ResourceTab.THEMES, viewModel.uiState.value.activeResourceTab)
    }

    @Test
    fun testContextMenuOpenAndClose() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        val file = viewModel.uiState.value.treeItems.first { !it.isDirectory }
        viewModel.showContextMenu(file)
        assertTrue(viewModel.uiState.value.showFileContextMenu)
        assertEquals(file, viewModel.uiState.value.selectedFileForMenu)

        viewModel.hideContextMenu()
        assertFalse(viewModel.uiState.value.showFileContextMenu)
        assertNull(viewModel.uiState.value.selectedFileForMenu)
    }

    @Test
    fun testCloseTabDiscardsUnsavedBuffer() = runBlocking {
        viewModel.bindProjectSync(fakeProject())
        viewModel.openFileSync("app/src/main/java/com/demo/MainActivity.kt")
        viewModel.openFileSync("app/src/main/java/com/demo/Theme.kt")
        viewModel.updateCodeLine(1, "package com.demo // modifié")
        assertTrue(viewModel.uiState.value.unsavedPaths.isNotEmpty())

        viewModel.closeTab("app/src/main/java/com/demo/Theme.kt")
        assertEquals("app/src/main/java/com/demo/MainActivity.kt", viewModel.uiState.value.activeTab)

        viewModel.closeTab("app/src/main/java/com/demo/MainActivity.kt")
        assertEquals("", viewModel.uiState.value.activeTab)
        assertTrue(viewModel.uiState.value.codeLines.isEmpty())
    }
}
