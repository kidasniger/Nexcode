package com.nexcode.app.data.github.repository

import android.content.Context
import com.nexcode.app.data.git.GitOperationResult
import com.nexcode.app.data.git.JGitManager
import com.nexcode.app.data.git.LocalGitBranchStatus
import com.nexcode.app.data.git.LocalGitCommitInfo
import com.nexcode.app.data.git.LocalModifiedFile
import com.nexcode.app.data.git.LocalUnpushedCommit
import com.nexcode.app.data.git.ParsedConflictHunk
import com.nexcode.app.data.github.api.GitHubApiClient
import com.nexcode.app.data.github.api.GitHubApiService
import com.nexcode.app.data.github.model.GitHubArtifact
import com.nexcode.app.data.github.model.GitHubRepository
import com.nexcode.app.data.github.model.GitHubUser
import com.nexcode.app.data.github.model.GitHubWorkflow
import com.nexcode.app.data.github.model.WorkflowDispatchPayload
import com.nexcode.app.data.github.model.WorkflowJob
import com.nexcode.app.data.github.model.WorkflowRun
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.MergeResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

sealed class GitHubResult<out T> {
    data class Success<out T>(val data: T) : GitHubResult<T>()
    data class Error(val message: String, val code: Int? = null) : GitHubResult<Nothing>()
    data object NetworkError : GitHubResult<Nothing>()
}

data class VerifiedGitHubSession(
    val user: GitHubUser,
    val repoCount: Int,
    val planName: String
)

class GitHubRepositoryService(
    private val context: Context,
    private val vault: NexcodeSecureVault = NexcodeSecureVault(context),
    private val preferences: NexcodePreferences = NexcodePreferences(context),
    private val jgitManager: JGitManager = JGitManager(),
    private val api: GitHubApiService = GitHubApiClient.apiService
) {

    private fun getAuthHeader(): String {
        val token = vault.getGitToken().orEmpty()
        return if (token.isBlank()) "" else "Bearer $token"
    }

    fun hasSavedToken(): Boolean = !vault.getGitToken().isNullOrBlank()

    fun getSavedToken(): String = vault.getGitToken().orEmpty()

    suspend fun verifyAndSaveToken(token: String): GitHubResult<VerifiedGitHubSession> = withContext(Dispatchers.IO) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) {
            return@withContext GitHubResult.Error("Veuillez saisir un jeton d'accès personnel.", code = 400)
        }
        try {
            val authHeader = "Bearer $trimmed"
            val userResponse = api.getAuthenticatedUser(authHeader)
            if (userResponse.isSuccessful && userResponse.body() != null) {
                val user = userResponse.body()!!
                val reposResp = runCatching { api.getUserRepositories(authHeader, perPage = 100) }.getOrNull()
                val repoCount = if (reposResp != null && reposResp.isSuccessful && reposResp.body() != null) {
                    reposResp.body()!!.size
                } else {
                    user.publicRepos + user.totalPrivateRepos
                }
                val rawPlan = user.plan?.name.orEmpty()
                val displayPlan = when (rawPlan.lowercase()) {
                    "pro" -> "Plan Pro"
                    "team" -> "Plan Team"
                    "enterprise" -> "Plan Enterprise"
                    else -> "Plan gratuit"
                }

                vault.saveGitToken(trimmed)
                preferences.setGitAccount(
                    login = user.login,
                    name = user.name ?: user.login,
                    email = user.email ?: "${user.login}@users.noreply.github.com",
                    repoCount = repoCount,
                    plan = displayPlan
                )

                GitHubResult.Success(
                    VerifiedGitHubSession(
                        user = user,
                        repoCount = repoCount,
                        planName = displayPlan
                    )
                )
            } else {
                val code = userResponse.code()
                val msg = when (code) {
                    401 -> "Jeton d'accès GitHub invalide ou expiré (401)."
                    403 -> "Accès GitHub refusé ou quota dépassé (403)."
                    else -> "Jeton d'accès GitHub invalide ($code)."
                }
                GitHubResult.Error(message = msg, code = code)
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors de la vérification GitHub")
        }
    }

    suspend fun clearGitSession() {
        vault.clearGitToken()
        preferences.clearGitAccount()
    }

    suspend fun getAuthenticatedUser(): GitHubResult<VerifiedGitHubSession> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) {
                return@withContext GitHubResult.Error("Aucun jeton GitHub enregistré", code = 401)
            }
            val response = api.getAuthenticatedUser(header)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                val reposResp = runCatching { api.getUserRepositories(header, perPage = 100) }.getOrNull()
                val repoCount = if (reposResp != null && reposResp.isSuccessful && reposResp.body() != null) {
                    reposResp.body()!!.size
                } else {
                    user.publicRepos + user.totalPrivateRepos
                }
                val rawPlan = user.plan?.name.orEmpty()
                val displayPlan = when (rawPlan.lowercase()) {
                    "pro" -> "Plan Pro"
                    "team" -> "Plan Team"
                    "enterprise" -> "Plan Enterprise"
                    else -> "Plan gratuit"
                }
                preferences.setGitAccount(
                    login = user.login,
                    name = user.name ?: user.login,
                    email = user.email ?: "${user.login}@users.noreply.github.com",
                    repoCount = repoCount,
                    plan = displayPlan
                )
                GitHubResult.Success(VerifiedGitHubSession(user, repoCount, displayPlan))
            } else {
                GitHubResult.Error("Erreur d'authentification GitHub (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur inattendue")
        }
    }

    suspend fun getUserRepositories(): GitHubResult<List<GitHubRepository>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) {
                return@withContext GitHubResult.Error("Aucun jeton GitHub enregistré", code = 401)
            }
            val response = api.getUserRepositories(header)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!)
            } else {
                GitHubResult.Error("Impossible de charger les dépôts (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors du chargement des dépôts")
        }
    }

    suspend fun cloneRepository(
        cloneUrl: String,
        targetDir: File,
        branch: String = "main"
    ): GitOperationResult<File> {
        val token = vault.getGitToken()
        return jgitManager.cloneRepository(
            remoteUrl = cloneUrl,
            destinationDir = targetDir,
            token = token,
            branch = branch
        )
    }

    suspend fun commitAndPush(
        projectDir: File,
        message: String,
        authorName: String = "NEXCODE Developer",
        authorEmail: String = "dev@nexcode.local"
    ): GitOperationResult<String> {
        val token = vault.getGitToken()
        return jgitManager.commitAndPush(
            projectDir = projectDir,
            commitMessage = message,
            authorName = authorName,
            authorEmail = authorEmail,
            token = token
        )
    }

    suspend fun pushOnly(projectDir: File): GitOperationResult<Unit> {
        return jgitManager.pushOnly(projectDir, vault.getGitToken())
    }

    suspend fun pull(projectDir: File): GitOperationResult<MergeResult.MergeStatus> {
        return jgitManager.pull(projectDir, vault.getGitToken())
    }

    suspend fun getBranchStatus(
        projectDir: File,
        fetchRemote: Boolean = false
    ): GitOperationResult<LocalGitBranchStatus> {
        return jgitManager.getBranchStatus(projectDir, vault.getGitToken(), fetchRemote)
    }

    suspend fun checkoutBranch(projectDir: File, branchName: String): GitOperationResult<Unit> {
        return jgitManager.checkoutBranch(projectDir, branchName)
    }

    suspend fun getModifiedFilesWithDiff(projectDir: File): GitOperationResult<List<LocalModifiedFile>> {
        return jgitManager.getModifiedFilesWithDiff(projectDir)
    }

    suspend fun getUnpushedCommits(projectDir: File): GitOperationResult<List<LocalUnpushedCommit>> {
        return jgitManager.getUnpushedCommits(projectDir)
    }

    suspend fun getCommitHistory(
        projectDir: File,
        count: Int = 50,
        allBranches: Boolean = false
    ): GitOperationResult<List<LocalGitCommitInfo>> {
        return jgitManager.getCommitHistory(projectDir, count, allBranches)
    }

    suspend fun parseMergeConflicts(projectDir: File): GitOperationResult<List<ParsedConflictHunk>> {
        return jgitManager.parseMergeConflicts(projectDir)
    }

    suspend fun resolveConflictsAndCommit(
        projectDir: File,
        keepLocalByHunkId: Map<String, Boolean>,
        authorName: String = "NEXCODE Developer",
        authorEmail: String = "dev@nexcode.local"
    ): GitOperationResult<String> {
        return jgitManager.resolveConflictsAndCommit(projectDir, keepLocalByHunkId, authorName, authorEmail)
    }

    suspend fun abortMerge(projectDir: File): GitOperationResult<Unit> {
        return jgitManager.abortMerge(projectDir)
    }

    suspend fun getWorkflows(owner: String, repo: String): GitHubResult<List<GitHubWorkflow>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) return@withContext GitHubResult.Error("Jeton GitHub manquant", code = 401)
            val response = api.getWorkflows(header, owner, repo)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!.workflows)
            } else {
                GitHubResult.Error("Impossible de lister les workflows (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur réseau")
        }
    }

    suspend fun triggerBuildWorkflow(
        owner: String,
        repo: String,
        workflowFileName: String = "build.yml",
        branch: String = "main"
    ): GitHubResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) return@withContext GitHubResult.Error("Jeton GitHub manquant", code = 401)
            val payload = WorkflowDispatchPayload(ref = branch, inputs = null)
            val response = api.triggerWorkflowDispatch(header, owner, repo, workflowFileName, payload)
            if (response.isSuccessful || response.code() == 204) {
                GitHubResult.Success(Unit)
            } else {
                val msg = when (response.code()) {
                    401 -> "Jeton GitHub invalide ou expiré (401)."
                    403 -> "Portée 'workflow' requise sur votre jeton GitHub (403)."
                    404 -> "Workflow $workflowFileName introuvable sur $owner/$repo (404)."
                    422 -> "Branche '$branch' ou workflow_dispatch invalide (422)."
                    else -> "Échec du déclenchement du workflow (${response.code()})"
                }
                GitHubResult.Error(msg, code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors du déclenchement du workflow")
        }
    }

    suspend fun getWorkflowRuns(
        owner: String,
        repo: String
    ): GitHubResult<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) return@withContext GitHubResult.Error("Jeton GitHub manquant", code = 401)
            val response = api.getWorkflowRuns(header, owner, repo)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!.workflowRuns)
            } else {
                GitHubResult.Error("Impossible de récupérer l'état des builds (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors de la lecture des workflows")
        }
    }

    suspend fun getWorkflowRunJobs(
        owner: String,
        repo: String,
        runId: Long
    ): GitHubResult<List<WorkflowJob>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) return@withContext GitHubResult.Error("Jeton GitHub manquant", code = 401)
            val response = api.getWorkflowRunJobs(header, owner, repo, runId)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!.jobs)
            } else {
                GitHubResult.Error("Impossible de récupérer les étapes (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors de la lecture des jobs")
        }
    }

    suspend fun cancelWorkflowRun(
        owner: String,
        repo: String,
        runId: Long
    ): GitHubResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) return@withContext GitHubResult.Error("Jeton GitHub manquant", code = 401)
            val response = api.cancelWorkflowRun(header, owner, repo, runId)
            if (response.isSuccessful || response.code() == 202) {
                GitHubResult.Success(Unit)
            } else {
                GitHubResult.Error("Impossible d'annuler le build (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors de l'annulation")
        }
    }

    suspend fun getWorkflowRunLogs(
        owner: String,
        repo: String,
        runId: Long
    ): GitHubResult<String> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) return@withContext GitHubResult.Error("Jeton GitHub manquant", code = 401)
            val response = api.getWorkflowRunLogs(header, owner, repo, runId)
            if (response.isSuccessful && response.body() != null) {
                val byteStream = response.body()!!.byteStream()
                val logBuilder = StringBuilder()
                ZipInputStream(byteStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".txt")) {
                            val content = zis.bufferedReader().readText()
                            logBuilder.append("=== ${entry.name} ===\n")
                            logBuilder.append(content)
                            logBuilder.append("\n")
                        }
                        entry = zis.nextEntry
                    }
                }
                val resultText = if (logBuilder.isNotEmpty()) {
                    logBuilder.toString()
                } else {
                    "Aucun log textuel trouvé dans l'archive."
                }
                GitHubResult.Success(resultText)
            } else {
                GitHubResult.Error("Impossible de télécharger les logs (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors de la lecture des logs")
        }
    }

    suspend fun getArtifacts(
        owner: String,
        repo: String,
        runId: Long
    ): GitHubResult<List<GitHubArtifact>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) return@withContext GitHubResult.Error("Jeton GitHub manquant", code = 401)
            val response = api.getWorkflowRunArtifacts(header, owner, repo, runId)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!.artifacts)
            } else {
                GitHubResult.Error("Impossible de lister les artefacts (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors du listing des artefacts")
        }
    }

    suspend fun downloadArtifactApks(
        owner: String,
        repo: String,
        artifactId: Long,
        destinationDir: File
    ): GitHubResult<List<File>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) return@withContext GitHubResult.Error("Jeton GitHub manquant", code = 401)
            val response = api.downloadArtifactZip(header, owner, repo, artifactId)
            if (response.isSuccessful && response.body() != null) {
                destinationDir.mkdirs()
                val extractedFiles = mutableListOf<File>()
                val byteStream = response.body()!!.byteStream()
                ZipInputStream(byteStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                            val safeName = File(entry.name).name
                            val outFile = File(destinationDir, safeName)
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            extractedFiles.add(outFile)
                        }
                        entry = zis.nextEntry
                    }
                }

                if (extractedFiles.isNotEmpty()) {
                    GitHubResult.Success(extractedFiles)
                } else {
                    GitHubResult.Error("Aucun fichier .apk trouvé dans l'artefact.")
                }
            } else {
                GitHubResult.Error("Échec du téléchargement de l'artefact (${response.code()})", code = response.code())
            }
        } catch (_: IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur lors du téléchargement de l'artefact")
        }
    }

    companion object {
        fun parseOwnerAndRepo(gitRepo: String): Pair<String, String>? {
            val cleaned = gitRepo
                .trim()
                .replace(" ", "")
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("github.com/")
                .removeSuffix(".git")
                .trim('/')
            val parts = cleaned.split("/").filter { it.isNotBlank() }
            if (parts.size < 2) return null
            return parts[0] to parts[1]
        }
    }
}
