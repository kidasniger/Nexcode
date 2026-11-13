package com.nexcode.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.github.model.WorkflowJob
import com.nexcode.app.data.github.model.WorkflowJobStep
import com.nexcode.app.data.github.model.WorkflowRun
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.ui.screens.build.BuildVariant
import com.nexcode.app.ui.screens.build.BuildViewModel
import com.nexcode.app.ui.screens.build.CompilationStatus
import com.nexcode.app.ui.screens.build.LogFilter
import com.nexcode.app.ui.screens.build.StepState
import com.nexcode.app.ui.screens.projects.ProjectBuildStatus
import com.nexcode.app.ui.screens.projects.ProjectItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BuildScreensTest {

    private lateinit var context: Context
    private lateinit var fakeApi: FakeGitHubApiService
    private lateinit var gitService: GitHubRepositoryService
    private lateinit var viewModel: BuildViewModel

    private lateinit var sampleProject: ProjectItem

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeApi = FakeGitHubApiService()
        gitService = GitHubRepositoryService(context, api = fakeApi)
        val insertedId = runBlocking {
            gitService.clearGitSession()
            gitService.verifyAndSaveToken("ghp_build_test_token")
            val db = com.nexcode.app.data.local.NexcodeDatabase.getInstance(context)
            db.projectDao().insertProject(
                com.nexcode.app.data.local.entity.ProjectEntity(
                    name = "SampleAndroid",
                    packageName = "com.octo.sample",
                    path = File(context.filesDir, "projects/SampleAndroid").absolutePath,
                    gitRepo = "https://github.com/octo-dev/sample-android",
                    gitBranch = "main"
                )
            )
        }
        sampleProject = ProjectItem(
            id = insertedId,
            name = "SampleAndroid",
            packageName = "com.octo.sample",
            initials = "SA",
            language = "Kotlin",
            uiFramework = "Compose",
            modifiedText = "Aujourd'hui",
            buildStatus = ProjectBuildStatus.DRAFT,
            buildStatusLabel = "Brouillon",
            version = "1.4.0",
            onGitHub = true,
            gitRepo = "https://github.com/octo-dev/sample-android",
            gitBranch = "main",
            buildCount = 4
        )
        viewModel = BuildViewModel(context, gitService)
    }

    @Test
    fun testBuildConfigAndMissingRepoValidation() = runBlocking {
        viewModel.updateVersion("2.0.0")
        viewModel.updateBuildNumber("15")
        viewModel.selectVariant(BuildVariant.DEBUG)
        viewModel.toggleR8(false)
        viewModel.toggleDebuggable(true)

        val state = viewModel.uiState.value
        assertEquals("2.0.0", state.versionName)
        assertEquals("15", state.buildNumber)
        assertEquals(BuildVariant.DEBUG, state.selectedVariant)
        assertFalse(state.r8Minification)
        assertTrue(state.debuggable)

        // Cannot trigger build if no owner/repo linked
        val started = viewModel.triggerBuildSync()
        assertFalse(started)
        assertNotNull(viewModel.uiState.value.buildErrorBanner)
    }

    @Test
    fun testTriggerGitHubActionsPollStepsLogsAndDownloadApk(): Unit = runBlocking {
        viewModel.bindProject(sampleProject)
        assertEquals("octo-dev", viewModel.uiState.value.owner)
        assertEquals("sample-android", viewModel.uiState.value.repo)

        // 1. Trigger build dispatch
        fakeApi.runs = listOf(
            WorkflowRun(
                id = 9001L,
                name = "Android CI Build",
                headBranch = "main",
                headSha = "deadbeef",
                status = "in_progress",
                conclusion = null,
                htmlUrl = "https://github.com/octo-dev/sample-android/actions/runs/9001",
                createdAt = "2026-11-13T10:00:00Z",
                updatedAt = "2026-11-13T10:00:25Z",
                runNumber = 17
            )
        )
        fakeApi.jobs = listOf(
            WorkflowJob(
                id = 501L,
                name = "build",
                status = "in_progress",
                conclusion = null,
                steps = listOf(
                    WorkflowJobStep("Checkout", "completed", "success", 1),
                    WorkflowJobStep("Compile Gradle", "in_progress", null, 2),
                    WorkflowJobStep("Upload APK", "queued", null, 3)
                )
            )
        )

        assertTrue(viewModel.triggerBuildSync())
        assertEquals(listOf("build.yml"), fakeApi.triggeredWorkflowIds)
        assertEquals(CompilationStatus.RUNNING, viewModel.uiState.value.compilationStatus)

        // 2. Poll while running
        val finishedFirstPoll = viewModel.syncWorkflowRunOnce()
        assertFalse(finishedFirstPoll)
        assertEquals(3, viewModel.uiState.value.steps.size)
        assertEquals(StepState.DONE, viewModel.uiState.value.steps[0].state)
        assertEquals(StepState.RUNNING, viewModel.uiState.value.steps[1].state)
        assertEquals(StepState.PENDING, viewModel.uiState.value.steps[2].state)

        // 3. Complete run -> fetch logs ZIP + artifacts
        fakeApi.runs = listOf(fakeApi.runs.first().copy(status = "completed", conclusion = "success"))
        fakeApi.jobs = listOf(
            fakeApi.jobs.first().copy(
                status = "completed",
                conclusion = "success",
                steps = listOf(
                    WorkflowJobStep("Checkout", "completed", "success", 1),
                    WorkflowJobStep("Compile Gradle", "completed", "success", 2),
                    WorkflowJobStep("Upload APK", "completed", "success", 3)
                )
            )
        )

        val finishedSecondPoll = viewModel.syncWorkflowRunOnce()
        assertTrue(finishedSecondPoll)

        val state = viewModel.uiState.value
        assertEquals(CompilationStatus.SUCCESS, state.compilationStatus)
        assertEquals(100, state.progressPercent)
        assertEquals(17, state.activeRunNumber)
        assertTrue(state.logs.isNotEmpty())
        assertEquals(0, state.errorCount)
        assertEquals(1, state.warningCount)
        assertEquals(1, state.apks.size)

        // 4. Download real APK from artifact ZIP into filesDir/apks/
        val apkId = state.apks.first().id
        val downloaded = viewModel.downloadApkSync(apkId)
        assertNotNull(downloaded)
        assertTrue(downloaded!!.exists())
        assertEquals("app-debug.apk", downloaded.name)
        assertTrue(downloaded.length() > 0)

        // Clean up APK file
        File(context.filesDir, "apks").deleteRecursively()
    }

    @Test
    fun testLogParserExtractsErrorsWarningsAndLocations() {
        val raw = """
            2026-11-13T10:00:01Z e: file:///src/MainActivity.kt:23:31 Unresolved reference: title_detail
            2026-11-13T10:00:02Z w: Deprecated API in Helper.kt:12:4
            2026-11-13T10:00:03Z BUILD FAILED in 14s
        """.trimIndent()
        val parsed = BuildViewModel.parseBuildLogs(raw)
        assertEquals(2, parsed.errorCount)
        assertEquals(1, parsed.warningCount)
        assertEquals(listOf("MainActivity.kt:23:31"), parsed.locations)
        viewModel.selectLogFilter(LogFilter.ERRORS)
        assertEquals(LogFilter.ERRORS, viewModel.uiState.value.activeLogFilter)
        viewModel.dismissNotice()
        assertNull(viewModel.uiState.value.userNotice)
    }
}
