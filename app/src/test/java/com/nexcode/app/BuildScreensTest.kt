package com.nexcode.app

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.preferences.NexcodeSecureVault
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.entity.ProjectEntity
import com.nexcode.app.ui.screens.build.BuildVariant
import com.nexcode.app.ui.screens.build.BuildViewModel
import com.nexcode.app.ui.screens.build.CompilationStatus
import com.nexcode.app.ui.screens.build.BuildLogLevel
import com.nexcode.app.ui.screens.projects.ProjectItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests du pipeline de build RÉEL : état initial honnête, lecture réelle du
 * projet (version + keystore), pipeline GitHub Actions via FakeGitHubApiService
 * (succès et échec), parsing réel des logs et filtres.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BuildScreensTest {



    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: android.content.Context
    private lateinit var fakeApi: FakeGitHubApiService
    private lateinit var service: GitHubRepositoryService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeApi = FakeGitHubApiService().apply { simulateRunLifecycle = true }
        service = GitHubRepositoryService(context, api = fakeApi)
        // Jeton écrit directement dans le coffre (par test, isolé) : verifyAndSaveToken
        // écrivait aussi dans DataStore depuis un @Before et figeait le JVM de test
        // de façon non déterministe. Le pipeline de build lit le jeton du vault.
        NexcodeSecureVault(context).saveGitToken("test-token")
    }

    private fun buildProjectItem(localPath: String, gitRepo: String) = ProjectItem(
        id = 1L,
        name = "Test App",
        packageName = "com.test.app",
        initials = "TA",
        language = "Kotlin",
        uiFramework = "Compose",
        modifiedText = "",
        buildStatus = com.nexcode.app.ui.screens.projects.ProjectBuildStatus.DRAFT,
        buildStatusLabel = "",
        gitRepo = gitRepo,
        gitBranch = "main",
        localPath = localPath
    )

    @Test
    fun testInitialStateIsHonestAndEmpty() {
        val viewModel = BuildViewModel(context = null)
        val state = viewModel.uiState.value
        assertEquals(CompilationStatus.IDLE, state.compilationStatus)
        assertEquals("", state.projectName)
        assertEquals(BuildVariant.DEBUG, state.selectedVariant)
        assertTrue(state.steps.isEmpty())
        assertTrue(state.buildLogs.isEmpty())
        assertTrue(state.apkList.isEmpty())
        assertFalse(state.canStartBuild)
        assertNotNull(state.configNotice)
    }

    @Test
    fun testBindProjectReadsRealProjectFiles() {
        val projectDir = tempFolder.newFolder("project")
        val appDir = File(projectDir, "app").apply { mkdirs() }
        File(appDir, "build.gradle.kts").writeText(
            """
            android {
                defaultConfig {
                    versionCode = 7
                    versionName = "2.1.0"
                }
            }
            """.trimIndent()
        )
        File(projectDir, "debug.keystore").writeBytes(byteArrayOf(1, 2, 3))

        val viewModel = BuildViewModel(context, overrideGitService = service, pollDelayMs = 20)
        viewModel.bindProject(buildProjectItem(projectDir.absolutePath, "github.com/octo-dev/sample-android"))

        val state = viewModel.uiState.value
        assertEquals("Test App", state.projectName)
        assertEquals("2.1.0", state.versionName)
        assertEquals("7", state.buildNumber)
        assertEquals("octo-dev/sample-android", state.githubRepo)
        assertTrue(state.isGitHubReady)
        assertTrue(state.canStartBuild)
        assertTrue(state.keystoreInfo.contains("debug.keystore"))
        assertTrue(state.keystoreInfo.contains("présent"))
    }

    @Test
    fun testLocalProjectShowsHonestNotice() {
        val projectDir = tempFolder.newFolder("local")
        val viewModel = BuildViewModel(context, overrideGitService = service, pollDelayMs = 20)
        viewModel.bindProject(buildProjectItem(projectDir.absolutePath, ""))

        val state = viewModel.uiState.value
        assertFalse(state.isGitHubReady)
        assertFalse(state.canStartBuild)
        assertNotNull(state.configNotice)
        assertTrue(state.configNotice!!.contains("GitHub"))
    }

    @Test
    fun testRealBuildPipelineSucceedsAndDownloadsApk() {
        val projectDir = tempFolder.newFolder("projok")
        val appDir = File(projectDir, "app").apply { mkdirs() }
        File(appDir, "build.gradle.kts").writeText(
            "android { defaultConfig { versionCode = 3\nversionName = \"1.3.0\" } }"
        )
        val viewModel = BuildViewModel(context, overrideGitService = service, pollDelayMs = 20)
        viewModel.bindProject(buildProjectItem(projectDir.absolutePath, "github.com/octo-dev/sample-android"))

        var started = false
        viewModel.startCompilation { started = true }
        assertTrue(started)

        awaitCondition(20_000) {
            viewModel.uiState.value.compilationStatus != CompilationStatus.RUNNING
        }
        val state = viewModel.uiState.value
        assertEquals(CompilationStatus.SUCCESS, state.compilationStatus)
        assertEquals(100, state.progressPercent)
        assertTrue(state.steps.isNotEmpty())
        assertTrue(state.steps.any { it.label == "Checkout" })
        assertEquals(1, state.apkList.size)
        assertEquals("app-debug.apk", state.apkList[0].name)
        assertTrue(File(state.apkList[0].path).exists())
        assertTrue(fakeApi.triggeredWorkflowIds.isNotEmpty())
        assertTrue(state.runUrl != null)
    }

    @Test
    fun testRealBuildPipelineFailureProvidesRealLogs() {
        fakeApi.forcedRunConclusion = "failure"
        fakeApi.rawLogContent = """
            2026-11-14T09:00:10Z > Task :app:compileDebugKotlin FAILED
            2026-11-14T09:00:11Z e: Sample.kt:12:5 Unresolved reference 'missing'
            2026-11-14T09:00:12Z FAILURE: Build failed with an exception.
        """.trimIndent()

        val projectDir = tempFolder.newFolder("projfail")
        val viewModel = BuildViewModel(context, overrideGitService = service, pollDelayMs = 20)
        viewModel.bindProject(buildProjectItem(projectDir.absolutePath, "github.com/octo-dev/sample-android"))

        viewModel.startCompilation { }
        awaitCondition(20_000) {
            viewModel.uiState.value.compilationStatus == CompilationStatus.FAILED
        }
        val state = viewModel.uiState.value
        assertEquals(CompilationStatus.FAILED, state.compilationStatus)
        assertTrue(state.buildLogs.isNotEmpty())
        assertTrue(state.buildLogs.any { it.level == BuildLogLevel.ERROR })
        assertTrue(state.buildLogs.any { it.message.contains("Unresolved reference") })
    }

    @Test
    fun testLogFilterWorksOnRealLogs() {
        fakeApi.forcedRunConclusion = "failure"
        fakeApi.rawLogContent = """
            2026-11-14T09:00:10Z info ligne normale
            2026-11-14T09:00:11Z w: avertissement réel
            2026-11-14T09:00:12Z e: erreur réelle
        """.trimIndent()

        val projectDir = tempFolder.newFolder("projlog")
        val viewModel = BuildViewModel(context, overrideGitService = service, pollDelayMs = 20)
        viewModel.bindProject(buildProjectItem(projectDir.absolutePath, "github.com/octo-dev/sample-android"))
        viewModel.startCompilation { }
        awaitCondition(20_000) {
            viewModel.uiState.value.compilationStatus == CompilationStatus.FAILED
        }

        viewModel.setLogFilter("Erreurs")
        assertEquals("Erreurs", viewModel.uiState.value.selectedLogFilter)
        val errors = viewModel.uiState.value.buildLogs.filter { it.level == BuildLogLevel.ERROR }
        assertEquals(1, errors.size)
    }

    @Test
    fun testParseBuildLogsDetectsRealLevelsAndTags() {
        val viewModel = BuildViewModel(context = null)
        val logs = viewModel.parseBuildLogs(
            """
            === 1_Build with Gradle.txt ===
            2026-11-14T01:05:51.62Z > Task :app:assembleDebug
            2026-11-14T01:05:52.10Z w: unused variable
            2026-11-14T01:05:53.00Z e: unresolved reference
            FAILURE: Build failed
            """.trimIndent()
        )
        assertEquals(4, logs.size)
        assertEquals("1_Build with Gradle.txt", logs[0].tag)
        assertEquals("01:05:51", logs[0].time)
        assertEquals(BuildLogLevel.INFO, logs[0].level)
        assertEquals(BuildLogLevel.WARNING, logs[1].level)
        assertEquals(BuildLogLevel.ERROR, logs[2].level)
        assertEquals(BuildLogLevel.ERROR, logs[3].level)
    }

    @Test
    fun testBuildHistoryIsPersistedInRoom() {
        val projectDir = tempFolder.newFolder("projdb")
        val viewModel = BuildViewModel(context, overrideGitService = service, pollDelayMs = 20)
        viewModel.bindProject(buildProjectItem(projectDir.absolutePath, "github.com/octo-dev/sample-android"))

        // Insère un projet réel dans Room pour satisfaire la clé étrangère,
        // puis lance un build réel et vérifie la persistance.
        val db = NexcodeDatabase.getInstance(context)
        runBlocking {
            db.projectDao().insertProject(
                ProjectEntity(id = 1L, name = "Test App", packageName = "com.test.app", path = projectDir.absolutePath)
            )
        }
        viewModel.startCompilation { }
        awaitCondition(20_000) {
            viewModel.uiState.value.compilationStatus == CompilationStatus.SUCCESS
        }
        runBlocking {
            val list = db.buildDao().getBuildsByProject(1L).first()
            assertTrue(list.isNotEmpty())
            assertEquals("SUCCESS", list[0].status)
            assertEquals("debug", list[0].buildType)
            assertNotNull(list[0].githubRunId)
        }
    }

    private fun awaitCondition(timeoutMs: Long, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("Condition non remplie après ${timeoutMs} ms")
            }
            Thread.sleep(50)
        }
    }
}
