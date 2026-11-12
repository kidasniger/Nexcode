package com.nexcode.app.data.github.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val name: String? = null,
    val email: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    val plan: GitHubPlan? = null
)

@JsonClass(generateAdapter = true)
data class GitHubPlan(
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepository(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val private: Boolean = false,
    @Json(name = "html_url") val htmlUrl: String,
    val description: String? = null,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "clone_url") val cloneUrl: String
)

@JsonClass(generateAdapter = true)
data class GitHubBranch(
    val name: String,
    val commit: GitHubBranchCommit
)

@JsonClass(generateAdapter = true)
data class GitHubBranchCommit(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchPayload(
    val ref: String,
    val inputs: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRun>
)

@JsonClass(generateAdapter = true)
data class WorkflowRun(
    val id: Long,
    val name: String? = null,
    @Json(name = "head_branch") val headBranch: String? = null,
    @Json(name = "head_sha") val headSha: String? = null,
    val status: String, // "queued", "in_progress", "completed"
    val conclusion: String? = null, // "success", "failure", "cancelled", null
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "run_number") val runNumber: Int
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponse(
    @Json(name = "total_count") val totalCount: Int,
    val artifacts: List<GitHubArtifact>
)

@JsonClass(generateAdapter = true)
data class GitHubArtifact(
    val id: Long,
    val name: String,
    @Json(name = "size_in_bytes") val sizeInBytes: Long,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean = false,
    @Json(name = "created_at") val createdAt: String
)
