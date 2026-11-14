package com.nexcode.app

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.entity.BuildEntity
import com.nexcode.app.ui.screens.projects.ProjectBuildStatus
import com.nexcode.app.ui.screens.projects.ProjectFilter
import com.nexcode.app.ui.screens.projects.ProjectTemplateType
import com.nexcode.app.ui.screens.projects.ProjectsViewMode
import com.nexcode.app.ui.screens.projects.ProjectsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectsTest {



    private lateinit var context: Context
    private lateinit var viewModel: ProjectsViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = ProjectsViewModel(context, GitHubRepositoryService(context, api = FakeGitHubApiService()))
    }

    private suspend fun awaitProjectsCondition(
        timeoutMs: Long = 15_000L,
        predicate: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (predicate() || System.currentTimeMillis() >= deadline) {
                break
            }
            delay(25)
        }
    }

    @Test
    fun testInitialProjectsStateStartsEmpty(): Unit = runBlocking {
        awaitProjectsCondition { true }
        val state = viewModel.uiState.value
        assertEquals(0, state.projects.size)
        assertEquals(ProjectsViewMode.EMPTY, state.viewMode)
        assertEquals(ProjectFilter.ALL, state.selectedFilter)
    }

    @Test
    fun testCreateSearchFilterAndDeleteProject(): Unit = runBlocking {
        viewModel.selectTemplate(ProjectTemplateType.COMPOSE)
        viewModel.updateNewAppName("Mon Super App")
        assertEquals("com.monsuperapp.app", viewModel.uiState.value.newPackageName)
        viewModel.selectMinSdk(29)
        val createdId = viewModel.createProjectSync()
        assertTrue(createdId > 0)

        viewModel.selectTemplate(ProjectTemplateType.JAVA)
        viewModel.updateNewAppName("NotesJava")
        viewModel.createProjectSync()

        awaitProjectsCondition { viewModel.uiState.value.projects.size == 2 }
        var state = viewModel.uiState.value
        assertEquals(2, state.projects.size)
        assertEquals(ProjectsViewMode.DEFAULT, state.viewMode)

        viewModel.onSearchQueryChange("Super")
        assertEquals(1, viewModel.uiState.value.filteredProjects.size)
        assertEquals("Mon Super App", viewModel.uiState.value.filteredProjects.first().name)
        viewModel.onSearchQueryChange("")

        viewModel.onFilterSelect(ProjectFilter.JAVA)
        assertEquals(1, viewModel.uiState.value.filteredProjects.size)
        assertEquals("NotesJava", viewModel.uiState.value.filteredProjects.first().name)
        viewModel.onFilterSelect(ProjectFilter.ALL)

        val target = viewModel.uiState.value.projects.first { it.id == createdId }
        assertTrue(File(target.localPath).exists())

        viewModel.openDeleteDialog(target)
        assertTrue(viewModel.uiState.value.showDeleteDialog)
        viewModel.confirmDeleteProjectSync()

        awaitProjectsCondition { !viewModel.uiState.value.projects.any { it.id == createdId } }
        state = viewModel.uiState.value
        assertFalse(state.showDeleteDialog)
        assertFalse(state.projects.any { it.id == createdId })
        assertFalse(File(target.localPath).exists())
    }

    @Test
    fun testProjectMetadataIsRealAndBuildsFeedDetailsScreen(): Unit = runBlocking {
        viewModel.selectTemplate(ProjectTemplateType.COMPOSE)
        viewModel.updateNewAppName("Vrai Données")
        val projectId = viewModel.createProjectSync()
        awaitProjectsCondition { viewModel.uiState.value.projects.any { it.id == projectId } }

        val item = viewModel.uiState.value.projects.first { it.id == projectId }
        // Aucune URL GitHub inventée tant qu'aucun remote réel n'existe
        assertTrue(item.gitRepo.isBlank())
        // Aucune version fictive tant qu'aucun build n'existe
        assertNull(item.version)
        // Date réelle issue du disque (jamais « modifié récemment »)
        assertTrue(item.modifiedText.startsWith("Modifié le "))
        assertFalse(item.modifiedText.contains("récemment"))
        // Statut honnête sans build enregistré
        assertEquals(ProjectBuildStatus.DRAFT, item.buildStatus)
        assertEquals("Aucun build enregistré", item.buildStatusLabel)
        assertEquals(0, item.buildCount)

        // Un vrai build enregistré en base (BuildDao) alimente S08 et la carte projet
        val buildDao = NexcodeDatabase.getInstance(context).buildDao()
        buildDao.insertBuild(
            BuildEntity(
                projectId = projectId,
                buildType = "debug",
                status = "FAILED",
                startTime = System.currentTimeMillis(),
                durationMs = 62_000L,
                logOutput = "> Task :app:compileDebugKotlin FAILED",
                errorCount = 2,
                runNumber = 7,
                versionName = "1.2.0",
                errorSummary = "2 erreurs de compilation"
            )
        )
        awaitProjectsCondition {
            val current = viewModel.uiState.value
            current.recentBuilds.isNotEmpty() &&
                (current.projects.firstOrNull { it.id == projectId }?.buildCount ?: 0) == 1
        }

        val state = viewModel.uiState.value
        assertEquals(1, state.recentBuilds.size)
        val buildItem = state.recentBuilds.first()
        assertFalse(buildItem.isSuccess)
        assertTrue(buildItem.hasLogsChip)
        assertEquals("2 erreurs de compilation", buildItem.errorText)
        assertTrue(buildItem.versionText.contains("Build #7"))
        assertTrue(buildItem.versionText.contains("1.2.0"))
        // Durée réelle formatée en français : 62 s -> « 1 min 2 s »
        assertTrue(buildItem.dateAndSizeText.contains("1 min 2 s"))

        val updatedItem = state.projects.first { it.id == projectId }
        assertEquals(ProjectBuildStatus.FAILED, updatedItem.buildStatus)
        assertTrue(updatedItem.buildStatusLabel.contains("échec"))
        assertEquals("1.2.0", updatedItem.version)
        assertEquals(1, updatedItem.buildCount)
    }

    @Test
    fun testImportRealZipAndLocalBareGitClone(): Unit = runBlocking {
        // 1. Test real .zip extraction
        val tempDir = Files.createTempDirectory("nexcode_zip_test").toFile()
        val zipFile = File(tempDir, "archive-demo.zip")
        val zipBytes = FakeGitHubApiService.createZipBytes(
            "archive-demo/app/src/main/java/Main.kt" to "fun main() {}".toByteArray(),
            "archive-demo/build.gradle.kts" to "plugins {}".toByteArray()
        )
        zipFile.writeBytes(zipBytes)

        val zipImported = viewModel.importZipFileSync(zipFile)
        assertNotNull(zipImported)
        awaitProjectsCondition { viewModel.uiState.value.projects.any { it.name == "archive-demo" } }
        assertTrue(viewModel.uiState.value.projects.any { it.name == "archive-demo" })

        // 2. Test real JGit clone from local bare repo
        val bareDir = File(tempDir, "remote-app.git")
        Git.init().setBare(true).setInitialBranch("main").setDirectory(bareDir).call().close()
        val workDir = File(tempDir, "seed")
        Git.init().setInitialBranch("main").setDirectory(workDir).call().use { git ->
            File(workDir, "README.md").writeText("# Remote App")
            git.add().addFilepattern(".").call()
            git.commit().setMessage("init").setAuthor("Test", "t@e.com").call()
            git.remoteAdd().setName("origin").setUri(URIish(bareDir.toURI().toString())).call()
            git.push().setRemote("origin").call()
        }

        viewModel.setImportTab(1)
        viewModel.updateImportGitUrl(bareDir.toURI().toString())
        val currentBranch = Git.open(workDir).use { it.repository.branch }
        viewModel.updateImportGitBranch(currentBranch)

        val clonedItem = viewModel.importGitSync()
        assertNotNull(clonedItem)
        awaitProjectsCondition { viewModel.uiState.value.projects.any { it.name.contains("remote-app") } }
        val clonedProject = viewModel.uiState.value.projects.firstOrNull { it.name.contains("remote-app") }
        assertNotNull(clonedProject)
        assertTrue(File(clonedProject!!.localPath, ".git").exists())

        tempDir.deleteRecursively()
    }
}
