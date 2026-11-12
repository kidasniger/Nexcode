package com.nexcode.app.data.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

sealed class GitOperationResult<out T> {
    data class Success<out T>(val data: T) : GitOperationResult<T>()
    data class Conflict(val conflictedFiles: List<String>) : GitOperationResult<Nothing>()
    data class Error(val message: String, val throwable: Throwable? = null) : GitOperationResult<Nothing>()
}

data class LocalGitCommitInfo(
    val hash: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val timeAgo: String,
    val timestamp: Long
)

class JGitManager {

    suspend fun cloneRepository(
        remoteUrl: String,
        destinationDir: File,
        token: String?,
        branch: String = "main"
    ): GitOperationResult<File> = withContext(Dispatchers.IO) {
        try {
            if (destinationDir.exists()) {
                destinationDir.deleteRecursively()
            }
            destinationDir.mkdirs()

            val cloneCommand = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(destinationDir)
                .setBranch(branch)

            if (!token.isNullOrBlank()) {
                cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
            }

            cloneCommand.call().use { git ->
                GitOperationResult.Success(destinationDir)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = e.message ?: "Échec du clone Git",
                throwable = e
            )
        }
    }

    suspend fun commitAndPush(
        projectDir: File,
        commitMessage: String,
        authorName: String,
        authorEmail: String,
        token: String?
    ): GitOperationResult<String> = withContext(Dispatchers.IO) {
        try {
            Git.open(projectDir).use { git ->
                // git add -A
                git.add().addFilepattern(".").call()
                git.add().setUpdate(true).addFilepattern(".").call()

                val author = PersonIdent(authorName, authorEmail)
                val commit = git.commit()
                    .setMessage(commitMessage)
                    .setAuthor(author)
                    .setCommitter(author)
                    .call()

                val pushCommand = git.push()
                if (!token.isNullOrBlank()) {
                    pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
                }

                val pushResults = pushCommand.call()
                for (pushResult in pushResults) {
                    for (update in pushResult.remoteUpdates) {
                        if (update.status != org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK &&
                            update.status != org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE) {
                            return@withContext GitOperationResult.Error("Push échoué: ${update.status}")
                        }
                    }
                }

                GitOperationResult.Success(commit.name)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = e.message ?: "Erreur de commit/push",
                throwable = e
            )
        }
    }

    suspend fun pull(
        projectDir: File,
        token: String?
    ): GitOperationResult<MergeResult.MergeStatus> = withContext(Dispatchers.IO) {
        try {
            Git.open(projectDir).use { git ->
                val pullCommand = git.pull()
                if (!token.isNullOrBlank()) {
                    pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
                }

                val pullResult = pullCommand.call()
                val mergeResult = pullResult.mergeResult

                if (mergeResult != null && !mergeResult.mergeStatus.isSuccessful) {
                    if (mergeResult.conflicts != null && mergeResult.conflicts.isNotEmpty()) {
                        return@withContext GitOperationResult.Conflict(mergeResult.conflicts.keys.toList())
                    }
                }

                GitOperationResult.Success(mergeResult?.mergeStatus ?: MergeResult.MergeStatus.ALREADY_UP_TO_DATE)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = e.message ?: "Erreur de synchronisation pull",
                throwable = e
            )
        }
    }

    suspend fun getCommitHistory(
        projectDir: File,
        maxCount: Int = 50
    ): GitOperationResult<List<LocalGitCommitInfo>> = withContext(Dispatchers.IO) {
        try {
            Git.open(projectDir).use { git ->
                val commits = git.log().setMaxCount(maxCount).call()
                val resultList = mutableListOf<LocalGitCommitInfo>()

                val nowSeconds = System.currentTimeMillis() / 1000
                for (rev in commits) {
                    val diffSeconds = nowSeconds - rev.commitTime
                    val timeText = when {
                        diffSeconds < 3600 -> "il y a ${diffSeconds / 60} min"
                        diffSeconds < 86400 -> "il y a ${diffSeconds / 3600} h"
                        diffSeconds < 172800 -> "hier"
                        else -> "le ${(diffSeconds / 86400)} j."
                    }

                    resultList.add(
                        LocalGitCommitInfo(
                            hash = rev.name,
                            shortHash = rev.name.take(7),
                            message = rev.shortMessage,
                            author = rev.authorIdent.name,
                            timeAgo = timeText,
                            timestamp = rev.commitTime * 1000L
                        )
                    )
                }

                GitOperationResult.Success(resultList)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = e.message ?: "Impossible de lire l'historique",
                throwable = e
            )
        }
    }

    suspend fun getStatus(
        projectDir: File
    ): GitOperationResult<org.eclipse.jgit.api.Status> = withContext(Dispatchers.IO) {
        try {
            Git.open(projectDir).use { git ->
                val status = git.status().call()
                GitOperationResult.Success(status)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = e.message ?: "Erreur de lecture du statut git",
                throwable = e
            )
        }
    }
}
