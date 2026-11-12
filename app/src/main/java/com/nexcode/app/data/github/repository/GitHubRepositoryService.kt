package com.nexcode.app.data.github.repository

import android.content.Context
import com.nexcode.app.data.git.GitOperationResult
import com.nexcode.app.data.git.JGitManager
import com.nexcode.app.data.git.LocalGitCommitInfo
import com.nexcode.app.data.github.api.GitHubApiClient
import com.nexcode.app.data.github.model.GitHubArtifact
import com.nexcode.app.data.github.model.GitHubRepository
import com.nexcode.app.data.github.model.GitHubUser
import com.nexcode.app.data.github.model.WorkflowDispatchPayload
import com.nexcode.app.data.github.model.WorkflowRun
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.MergeResult
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

sealed class GitHubResult<out T> {
    data class Success<out T>(val data: T) : GitHubResult<T>()
    data class Error(val message: String, val code: Int? = null) : GitHubResult<Nothing>()
    data object NetworkError : GitHubResult<Nothing>()
}

class GitHubRepositoryService(
    private val context: Context,
    private val vault: NexcodeSecureVault = NexcodeSecureVault(context),
    private val preferences: NexcodePreferences = NexcodePreferences(context),
    private val jgitManager: JGitManager = JGitManager()
) {
    private val api = GitHubApiClient.apiService

    private fun getAuthHeader(): String {
        val token = vault.getGitToken() ?: ""
        return if (token.isBlank()) "" else "Bearer $token"
    }

    suspend fun verifyAndSaveToken(token: String): GitHubResult<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer $token"
            val response = api.getAuthenticatedUser(authHeader)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                vault.saveGitToken(token)
                preferences.setGitUser(user.name ?: user.login, user.email ?: "${user.login}@users.noreply.github.com")
                GitHubResult.Success(user)
            } else {
                GitHubResult.Error(
                    message = "Jeton d'accès GitHub invalide (${response.code()})",
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            GitHubResult.NetworkError
        }
    }

    suspend fun getAuthenticatedUser(): GitHubResult<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            if (header.isBlank()) {
                return@withContext GitHubResult.Error("Aucun jeton GitHub enregistré", code = 401)
            }
            val response = api.getAuthenticatedUser(header)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!)
            } else {
                GitHubResult.Error("Erreur d'authentification GitHub (${response.code()})", code = response.code())
            }
        } catch (e: java.io.IOException) {
            GitHubResult.NetworkError
        } catch (e: Exception) {
            GitHubResult.Error(e.message ?: "Erreur inattendue")
        }
    }

    suspend fun getUserRepositories(): GitHubResult<List<GitHubRepository>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            val response = api.getUserRepositories(header)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!)
            } else {
                GitHubResult.Error("Impossible de charger les dépôts (${response.code()})", code = response.code())
            }
        } catch (e: Exception) {
            GitHubResult.NetworkError
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

    suspend fun pull(projectDir: File): GitOperationResult<MergeResult.MergeStatus> {
        val token = vault.getGitToken()
        return jgitManager.pull(projectDir, token)
    }

    suspend fun getCommitHistory(projectDir: File, count: Int = 50): GitOperationResult<List<LocalGitCommitInfo>> {
        return jgitManager.getCommitHistory(projectDir, count)
    }

    suspend fun triggerBuildWorkflow(
        owner: String,
        repo: String,
        workflowFileName: String = "build.yml",
        branch: String = "main"
    ): GitHubResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            val payload = WorkflowDispatchPayload(ref = branch)
            val response = api.triggerWorkflowDispatch(header, owner, repo, workflowFileName, payload)
            if (response.isSuccessful || response.code() == 204) {
                GitHubResult.Success(Unit)
            } else {
                GitHubResult.Error("Échec du déclenchement du workflow (${response.code()})", code = response.code())
            }
        } catch (e: Exception) {
            GitHubResult.NetworkError
        }
    }

    suspend fun getWorkflowRuns(
        owner: String,
        repo: String
    ): GitHubResult<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            val response = api.getWorkflowRuns(header, owner, repo)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!.workflowRuns)
            } else {
                GitHubResult.Error("Impossible de récupérer l'état des builds (${response.code()})", code = response.code())
            }
        } catch (e: Exception) {
            GitHubResult.NetworkError
        }
    }

    suspend fun getWorkflowRunLogs(
        owner: String,
        repo: String,
        runId: Long
    ): GitHubResult<String> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            val response = api.getWorkflowRunLogs(header, owner, repo, runId)
            if (response.isSuccessful && response.body() != null) {
                val byteStream = response.body()!!.byteStream()
                val logBuilder = StringBuilder()
                // GitHub retourne un fichier zip contenant les fichiers logs
                ZipInputStream(byteStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".txt")) {
                            val content = zis.bufferedReader().readText()
                            logBuilder.append("=== Log: ${entry.name} ===\n")
                            logBuilder.append(content)
                            logBuilder.append("\n\n")
                        }
                        entry = zis.nextEntry
                    }
                }
                val resultText = if (logBuilder.isNotEmpty()) logBuilder.toString() else "Aucun log textuel trouvé dans l'archive."
                GitHubResult.Success(resultText)
            } else {
                GitHubResult.Error("Impossible de télécharger les logs (${response.code()})", code = response.code())
            }
        } catch (e: Exception) {
            GitHubResult.NetworkError
        }
    }

    suspend fun getArtifacts(
        owner: String,
        repo: String,
        runId: Long
    ): GitHubResult<List<GitHubArtifact>> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            val response = api.getWorkflowRunArtifacts(header, owner, repo, runId)
            if (response.isSuccessful && response.body() != null) {
                GitHubResult.Success(response.body()!!.artifacts)
            } else {
                GitHubResult.Error("Impossible de lister les artefacts (${response.code()})", code = response.code())
            }
        } catch (e: Exception) {
            GitHubResult.NetworkError
        }
    }

    suspend fun downloadArtifactApk(
        owner: String,
        repo: String,
        artifactId: Long,
        destinationFile: File
    ): GitHubResult<File> = withContext(Dispatchers.IO) {
        try {
            val header = getAuthHeader()
            val response = api.downloadArtifactZip(header, owner, repo, artifactId)
            if (response.isSuccessful && response.body() != null) {
                val byteStream = response.body()!!.byteStream()
                // L'archive contient le ou les fichiers APK générés
                var apkFound = false
                ZipInputStream(byteStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".apk")) {
                            destinationFile.parentFile?.mkdirs()
                            FileOutputStream(destinationFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            apkFound = true
                            break
                        }
                        entry = zis.nextEntry
                    }
                }

                if (apkFound) {
                    GitHubResult.Success(destinationFile)
                } else {
                    GitHubResult.Error("Aucun fichier .apk dans l'artefact téléchargé.")
                }
            } else {
                GitHubResult.Error("Échec du téléchargement de l'artefact (${response.code()})", code = response.code())
            }
        } catch (e: Exception) {
            GitHubResult.NetworkError
        }
    }
}
