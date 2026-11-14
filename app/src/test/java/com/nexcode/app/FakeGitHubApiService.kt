package com.nexcode.app

import com.nexcode.app.data.github.api.GitHubApiService
import com.nexcode.app.data.github.model.ArtifactsResponse
import com.nexcode.app.data.github.model.GitHubArtifact
import com.nexcode.app.data.github.model.GitHubBranch
import com.nexcode.app.data.github.model.GitHubPlan
import com.nexcode.app.data.github.model.GitHubRepository
import com.nexcode.app.data.github.model.GitHubUser
import com.nexcode.app.data.github.model.GitHubWorkflow
import com.nexcode.app.data.github.model.GitHubWorkflowsResponse
import com.nexcode.app.data.github.model.WorkflowDispatchPayload
import com.nexcode.app.data.github.model.WorkflowJob
import com.nexcode.app.data.github.model.WorkflowJobStep
import com.nexcode.app.data.github.model.WorkflowJobsResponse
import com.nexcode.app.data.github.model.WorkflowRun
import com.nexcode.app.data.github.model.WorkflowRunsResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FakeGitHubApiService : GitHubApiService {

    var throwNetworkError: Boolean = false
    var authFailureCode: Int? = null
    var dispatchFailureCode: Int? = null

    // Cycle de vie réaliste d'un run déclenché : in_progress puis completed
    // après deux lectures. Les conclusions peuvent être forcées pour tester
    // les échecs réels.
    var simulateRunLifecycle: Boolean = false
    var forcedRunConclusion: String? = null
    private var dispatched = false
    private var pollsAfterDispatch = 0
    private var dynamicRuns: MutableList<WorkflowRun> = mutableListOf()

    var user: GitHubUser = GitHubUser(
        login = "octo-dev",
        id = 42L,
        name = "Octo Dev",
        email = "octo@example.com",
        publicRepos = 3,
        totalPrivateRepos = 1,
        plan = GitHubPlan("pro")
    )

    var repositories: List<GitHubRepository> = listOf(
        GitHubRepository(
            id = 101L,
            name = "sample-android",
            fullName = "octo-dev/sample-android",
            private = false,
            htmlUrl = "https://github.com/octo-dev/sample-android",
            description = "Sample app",
            defaultBranch = "main",
            cloneUrl = "https://github.com/octo-dev/sample-android.git"
        )
    )

    var workflows: List<GitHubWorkflow> = listOf(
        GitHubWorkflow(
            id = 1L,
            name = "Android CI Build",
            path = ".github/workflows/build.yml",
            state = "active"
        )
    )

    var triggeredWorkflowIds = mutableListOf<String>()
    var cancelledRunIds = mutableListOf<Long>()

    var runs: List<WorkflowRun> = listOf(
        WorkflowRun(
            id = 9001L,
            name = "Android CI Build",
            headBranch = "main",
            headSha = "abc1234",
            status = "completed",
            conclusion = "success",
            htmlUrl = "https://github.com/octo-dev/sample-android/actions/runs/9001",
            createdAt = "2026-11-13T10:00:00Z",
            updatedAt = "2026-11-13T10:01:15Z",
            runNumber = 17
        )
    )

    var jobs: List<WorkflowJob> = listOf(
        WorkflowJob(
            id = 501L,
            name = "build",
            status = "completed",
            conclusion = "success",
            startedAt = "2026-11-13T10:00:02Z",
            completedAt = "2026-11-13T10:01:12Z",
            steps = listOf(
                WorkflowJobStep("Checkout", "completed", "success", 1, "2026-11-13T10:00:02Z", "2026-11-13T10:00:05Z"),
                WorkflowJobStep("Assemble APK", "completed", "success", 2, "2026-11-13T10:00:06Z", "2026-11-13T10:01:12Z")
            )
        )
    )

    var rawLogContent: String = """
        2026-11-13T10:00:06Z > Task :app:compileDebugKotlin
        2026-11-13T10:00:40Z w: MainActivity.kt:12:9 Variable unused
        2026-11-13T10:01:10Z BUILD SUCCESSFUL in 1m 4s
    """.trimIndent()

    var artifacts: List<GitHubArtifact> = listOf(
        GitHubArtifact(
            id = 777L,
            name = "app-debug",
            sizeInBytes = 2048L,
            archiveDownloadUrl = "https://api.github.com/repos/octo-dev/sample-android/actions/artifacts/777/zip",
            expired = false,
            createdAt = "2026-11-13T10:01:14Z"
        )
    )

    var apkPayloadBytes: ByteArray = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4)

    private fun checkNetwork() {
        if (throwNetworkError) throw IOException("Network unreachable")
    }

    override suspend fun getAuthenticatedUser(authHeader: String): Response<GitHubUser> {
        checkNetwork()
        authFailureCode?.let { code ->
            return Response.error(code, """{"message":"Bad credentials"}""".toResponseBody("application/json".toMediaType()))
        }
        return Response.success(user)
    }

    override suspend fun getUserRepositories(
        authHeader: String,
        sort: String,
        perPage: Int
    ): Response<List<GitHubRepository>> {
        checkNetwork()
        authFailureCode?.let { code ->
            return Response.error(code, "{}".toResponseBody("application/json".toMediaType()))
        }
        return Response.success(repositories)
    }

    override suspend fun getRepository(authHeader: String, owner: String, repo: String): Response<GitHubRepository> {
        checkNetwork()
        return Response.success(repositories.first())
    }

    override suspend fun getBranches(authHeader: String, owner: String, repo: String): Response<List<GitHubBranch>> {
        checkNetwork()
        return Response.success(emptyList())
    }

    override suspend fun getWorkflows(authHeader: String, owner: String, repo: String): Response<GitHubWorkflowsResponse> {
        checkNetwork()
        return Response.success(GitHubWorkflowsResponse(workflows.size, workflows))
    }

    override suspend fun triggerWorkflowDispatch(
        authHeader: String,
        owner: String,
        repo: String,
        workflowId: String,
        payload: WorkflowDispatchPayload
    ): Response<Unit> {
        checkNetwork()
        dispatchFailureCode?.let { code ->
            return Response.error(code, "{}".toResponseBody("application/json".toMediaType()))
        }
        triggeredWorkflowIds.add(workflowId)
        if (simulateRunLifecycle) {
            if (dynamicRuns.isEmpty()) dynamicRuns.addAll(runs)
            val nextNumber = (dynamicRuns.maxOfOrNull { it.runNumber } ?: 0) + 1
            dynamicRuns.add(
                0,
                WorkflowRun(
                    id = 9900L + nextNumber,
                    name = "Build NEXCODE APK",
                    headBranch = payload.ref,
                    headSha = "dispatch" + nextNumber,
                    status = "in_progress",
                    conclusion = null,
                    htmlUrl = "https://github.com/$owner/$repo/actions/run/9900$nextNumber",
                    createdAt = "2026-11-14T09:00:00Z",
                    updatedAt = "2026-11-14T09:00:00Z",
                    runNumber = nextNumber
                )
            )
            dispatched = true
            pollsAfterDispatch = 0
        }
        return Response.success(204, Unit)
    }

    override suspend fun getWorkflowRuns(
        authHeader: String,
        owner: String,
        repo: String,
        perPage: Int
    ): Response<WorkflowRunsResponse> {
        checkNetwork()
        if (simulateRunLifecycle && dispatched) {
            pollsAfterDispatch++
            if (pollsAfterDispatch >= 2) {
                dynamicRuns = dynamicRuns.map { run ->
                    if (run.status != "completed") {
                        run.copy(status = "completed", conclusion = forcedRunConclusion ?: "success")
                    } else {
                        run
                    }
                }.toMutableList()
            }
            return Response.success(WorkflowRunsResponse(dynamicRuns.size, dynamicRuns.toList()))
        }
        return Response.success(WorkflowRunsResponse(runs.size, runs))
    }

    override suspend fun getWorkflowRunJobs(
        authHeader: String,
        owner: String,
        repo: String,
        runId: Long
    ): Response<WorkflowJobsResponse> {
        checkNetwork()
        return Response.success(WorkflowJobsResponse(jobs.size, jobs))
    }

    override suspend fun cancelWorkflowRun(
        authHeader: String,
        owner: String,
        repo: String,
        runId: Long
    ): Response<Unit> {
        checkNetwork()
        cancelledRunIds.add(runId)
        return Response.success(202, Unit)
    }

    override suspend fun getWorkflowRunLogs(
        authHeader: String,
        owner: String,
        repo: String,
        runId: Long
    ): Response<ResponseBody> {
        checkNetwork()
        val zipBytes = createZipBytes("build/1_Assemble.txt" to rawLogContent.toByteArray())
        return Response.success(zipBytes.toResponseBody("application/zip".toMediaType()))
    }

    override suspend fun getWorkflowRunArtifacts(
        authHeader: String,
        owner: String,
        repo: String,
        runId: Long
    ): Response<ArtifactsResponse> {
        checkNetwork()
        return Response.success(ArtifactsResponse(artifacts.size, artifacts))
    }

    override suspend fun downloadArtifactZip(
        authHeader: String,
        owner: String,
        repo: String,
        artifactId: Long
    ): Response<ResponseBody> {
        checkNetwork()
        val zipBytes = createZipBytes("app-debug.apk" to apkPayloadBytes)
        return Response.success(zipBytes.toResponseBody("application/zip".toMediaType()))
    }

    companion object {
        fun createZipBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
            val baos = ByteArrayOutputStream()
            ZipOutputStream(baos).use { zos ->
                entries.forEach { (name, data) ->
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(data)
                    zos.closeEntry()
                }
            }
            return baos.toByteArray()
        }
    }
}
