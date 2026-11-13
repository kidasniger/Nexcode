package com.nexcode.app.data.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.Edit
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.util.io.DisabledOutputStream
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

data class LocalModifiedFile(
    val path: String,
    val fileName: String,
    val addedLines: Int,
    val removedLines: Int
)

data class LocalUnpushedCommit(
    val hash: String,
    val shortHash: String,
    val message: String,
    val scope: String
)

data class LocalGitBranchStatus(
    val currentBranch: String,
    val branches: List<String>,
    val aheadCount: Int,
    val behindCount: Int,
    val isMergingWithConflicts: Boolean
)

data class ParsedConflictHunk(
    val id: String,
    val filePath: String,
    val fileName: String,
    val startLine: Int,
    val localLines: List<String>,
    val remoteLines: List<String>
)

class JGitManager {

    fun normalizeRemoteUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        val withScheme = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:/") -> trimmed
            trimmed.startsWith("github.com/") -> "https://$trimmed"
            trimmed.contains("/") && !trimmed.contains("://") && !trimmed.startsWith("/") -> "https://github.com/$trimmed"
            else -> trimmed
        }
        return if ((withScheme.startsWith("https://") || withScheme.startsWith("http://")) && !withScheme.endsWith(".git")) {
            "$withScheme.git"
        } else {
            withScheme
        }
    }

    suspend fun cloneRepository(
        remoteUrl: String,
        destinationDir: File,
        token: String?,
        branch: String = "main"
    ): GitOperationResult<File> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeRemoteUrl(remoteUrl)
            if (normalizedUrl.isBlank()) {
                return@withContext GitOperationResult.Error("L'URL du dépôt est vide.")
            }

            if (destinationDir.exists()) {
                destinationDir.deleteRecursively()
            }
            destinationDir.mkdirs()

            val cloneCommand = Git.cloneRepository()
                .setURI(normalizedUrl)
                .setDirectory(destinationDir)
                .setTimeout(30)

            if (branch.isNotBlank()) {
                cloneCommand.setBranch(branch)
            }

            if (!token.isNullOrBlank()) {
                cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
            }

            cloneCommand.call().use {
                GitOperationResult.Success(destinationDir)
            }
        } catch (e: Exception) {
            if (destinationDir.exists() && !File(destinationDir, ".git").exists()) {
                destinationDir.deleteRecursively()
            }
            GitOperationResult.Error(
                message = formatGitException(e, "Échec du clone Git"),
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
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Error("Aucun dépôt Git initialisé dans ce projet.")
            }

            Git.open(projectDir).use { git ->
                val status = git.status().call()
                var commitHash = git.repository.resolve(Constants.HEAD)?.name ?: ""

                if (!status.isClean) {
                    git.add().addFilepattern(".").call()
                    git.add().setUpdate(true).addFilepattern(".").call()

                    val author = PersonIdent(
                        authorName.ifBlank { "NEXCODE Developer" },
                        authorEmail.ifBlank { "dev@nexcode.local" }
                    )
                    val commit = git.commit()
                        .setMessage(commitMessage.ifBlank { "chore: mise à jour" })
                        .setAuthor(author)
                        .setCommitter(author)
                        .call()
                    commitHash = commit.name
                }

                val pushCommand = git.push().setTimeout(30)
                if (!token.isNullOrBlank()) {
                    pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
                }

                val pushResults = pushCommand.call()
                for (pushResult in pushResults) {
                    for (update in pushResult.remoteUpdates) {
                        if (update.status != RemoteRefUpdate.Status.OK &&
                            update.status != RemoteRefUpdate.Status.UP_TO_DATE
                        ) {
                            return@withContext GitOperationResult.Error(
                                "Échec du push : ${update.message ?: update.status.name}"
                            )
                        }
                    }
                }

                GitOperationResult.Success(commitHash)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Erreur lors du push vers GitHub"),
                throwable = e
            )
        }
    }

    suspend fun pushOnly(
        projectDir: File,
        token: String?
    ): GitOperationResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Error("Aucun dépôt Git initialisé dans ce projet.")
            }
            Git.open(projectDir).use { git ->
                val pushCommand = git.push().setTimeout(30)
                if (!token.isNullOrBlank()) {
                    pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
                }
                val pushResults = pushCommand.call()
                for (pushResult in pushResults) {
                    for (update in pushResult.remoteUpdates) {
                        if (update.status != RemoteRefUpdate.Status.OK &&
                            update.status != RemoteRefUpdate.Status.UP_TO_DATE
                        ) {
                            return@withContext GitOperationResult.Error(
                                "Échec du push : ${update.message ?: update.status.name}"
                            )
                        }
                    }
                }
                GitOperationResult.Success(Unit)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Connexion à github.com indisponible"),
                throwable = e
            )
        }
    }

    suspend fun pull(
        projectDir: File,
        token: String?
    ): GitOperationResult<MergeResult.MergeStatus> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Error("Aucun dépôt Git initialisé dans ce projet.")
            }
            Git.open(projectDir).use { git ->
                val pullCommand = git.pull().setTimeout(30)
                if (!token.isNullOrBlank()) {
                    pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
                }

                val pullResult = pullCommand.call()
                val mergeResult = pullResult.mergeResult

                if (mergeResult != null && !mergeResult.mergeStatus.isSuccessful) {
                    if (!mergeResult.conflicts.isNullOrEmpty()) {
                        return@withContext GitOperationResult.Conflict(mergeResult.conflicts.keys.toList())
                    }
                    return@withContext GitOperationResult.Error("Fusion impossible : ${mergeResult.mergeStatus}")
                }

                GitOperationResult.Success(mergeResult?.mergeStatus ?: MergeResult.MergeStatus.ALREADY_UP_TO_DATE)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Erreur de synchronisation pull"),
                throwable = e
            )
        }
    }

    suspend fun getBranchStatus(
        projectDir: File,
        token: String? = null,
        fetchRemote: Boolean = false
    ): GitOperationResult<LocalGitBranchStatus> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Error("Aucun dépôt Git local.")
            }
            Git.open(projectDir).use { git ->
                if (fetchRemote) {
                    try {
                        val fetchCmd = git.fetch().setTimeout(15)
                        if (!token.isNullOrBlank()) {
                            fetchCmd.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
                        }
                        fetchCmd.call()
                    } catch (_: Exception) {
                        // Conserve les infos locales si hors ligne
                    }
                }

                val repo = git.repository
                val currentBranch = repo.branch ?: "main"
                val localBranches = git.branchList()
                    .call()
                    .map { it.name.removePrefix("refs/heads/") }
                val remoteBranches = git.branchList()
                    .setListMode(ListBranchCommand.ListMode.REMOTE)
                    .call()
                    .map { it.name.removePrefix("refs/remotes/origin/") }
                    .filter { it != "HEAD" }
                val allBranches = (localBranches + remoteBranches).distinct()

                val tracking = BranchTrackingStatus.of(repo, currentBranch)
                val ahead = tracking?.aheadCount ?: 0
                val behind = tracking?.behindCount ?: 0

                val state = repo.repositoryState
                val hasConflicts = state == RepositoryState.MERGING ||
                    state == RepositoryState.MERGING_RESOLVED ||
                    git.status().call().conflicting.isNotEmpty()

                GitOperationResult.Success(
                    LocalGitBranchStatus(
                        currentBranch = currentBranch,
                        branches = allBranches.ifEmpty { listOf(currentBranch) },
                        aheadCount = ahead,
                        behindCount = behind,
                        isMergingWithConflicts = hasConflicts
                    )
                )
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Impossible de lire les branches"),
                throwable = e
            )
        }
    }

    suspend fun checkoutBranch(
        projectDir: File,
        branchName: String
    ): GitOperationResult<Unit> = withContext(Dispatchers.IO) {
        try {
            Git.open(projectDir).use { git ->
                val localExists = git.branchList().call().any {
                    it.name == "refs/heads/$branchName"
                }
                if (localExists) {
                    git.checkout().setName(branchName).call()
                } else {
                    git.checkout()
                        .setCreateBranch(true)
                        .setName(branchName)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setStartPoint("origin/$branchName")
                        .call()
                }
                GitOperationResult.Success(Unit)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Impossible de changer de branche"),
                throwable = e
            )
        }
    }

    suspend fun getModifiedFilesWithDiff(
        projectDir: File
    ): GitOperationResult<List<LocalModifiedFile>> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Success(emptyList())
            }
            Git.open(projectDir).use { git ->
                val repo = git.repository
                val status = git.status().call()
                val diffStats = mutableMapOf<String, Pair<Int, Int>>()

                val headId = repo.resolve(Constants.HEAD)
                if (headId != null) {
                    repo.newObjectReader().use { reader ->
                        val commitTree = repo.parseCommit(headId).tree
                        val oldTreeParser = CanonicalTreeParser().apply {
                            reset(reader, commitTree)
                        }
                        val workingTreeIt = FileTreeIterator(repo)
                        DiffFormatter(DisabledOutputStream.INSTANCE).use { formatter ->
                            formatter.setRepository(repo)
                            formatter.setDiffComparator(RawTextComparator.DEFAULT)
                            formatter.isDetectRenames = true
                            val entries = formatter.scan(oldTreeParser, workingTreeIt)
                            for (entry in entries) {
                                val path = entry.newPath.takeIf { it != "/dev/null" } ?: entry.oldPath
                                var added = 0
                                var removed = 0
                                val fileHeader = formatter.toFileHeader(entry)
                                for (hunk in fileHeader.hunks) {
                                    for (edit: Edit in hunk.toEditList()) {
                                        added += (edit.endB - edit.beginB)
                                        removed += (edit.endA - edit.beginA)
                                    }
                                }
                                diffStats[path] = added to removed
                            }
                        }
                    }
                }

                val changedPaths = (status.modified + status.changed + status.added +
                    status.removed + status.missing + status.untracked + status.conflicting).toSortedSet()

                val files = changedPaths.map { relPath ->
                    val stats = diffStats[relPath]
                    val (added, removed) = if (stats != null) {
                        stats
                    } else if (relPath in status.untracked) {
                        val file = File(projectDir, relPath)
                        val lineCount = if (file.isFile) runCatching { file.readLines().size }.getOrDefault(0) else 0
                        lineCount to 0
                    } else {
                        0 to 0
                    }
                    LocalModifiedFile(
                        path = relPath,
                        fileName = relPath.substringAfterLast('/'),
                        addedLines = added,
                        removedLines = removed
                    )
                }

                GitOperationResult.Success(files)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Erreur de lecture du statut git"),
                throwable = e
            )
        }
    }

    suspend fun getUnpushedCommits(
        projectDir: File
    ): GitOperationResult<List<LocalUnpushedCommit>> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Success(emptyList())
            }
            Git.open(projectDir).use { git ->
                val repo = git.repository
                val headId = repo.resolve(Constants.HEAD) ?: return@withContext GitOperationResult.Success(emptyList())
                val currentBranch = repo.branch ?: "main"
                val remoteRefId = repo.resolve("refs/remotes/origin/$currentBranch")

                val logCommand = git.log()
                if (remoteRefId != null) {
                    logCommand.addRange(remoteRefId, headId)
                } else {
                    logCommand.add(headId).setMaxCount(10)
                }

                val commits = logCommand.call().map { rev ->
                    LocalUnpushedCommit(
                        hash = rev.name,
                        shortHash = rev.name.take(7),
                        message = rev.shortMessage,
                        scope = "local"
                    )
                }
                GitOperationResult.Success(commits)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Erreur lecture des commits non poussés"),
                throwable = e
            )
        }
    }

    suspend fun getCommitHistory(
        projectDir: File,
        maxCount: Int = 50,
        allBranches: Boolean = false
    ): GitOperationResult<List<LocalGitCommitInfo>> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Success(emptyList())
            }
            Git.open(projectDir).use { git ->
                val headId = git.repository.resolve(Constants.HEAD)
                    ?: return@withContext GitOperationResult.Success(emptyList())

                val logCmd = git.log().setMaxCount(maxCount)
                if (allBranches) {
                    logCmd.all()
                } else {
                    logCmd.add(headId)
                }

                val commits = logCmd.call()
                val resultList = mutableListOf<LocalGitCommitInfo>()
                val nowSeconds = System.currentTimeMillis() / 1000
                for (rev in commits) {
                    val diffSeconds = (nowSeconds - rev.commitTime).coerceAtLeast(0)
                    val timeText = when {
                        diffSeconds < 60 -> "à l'instant"
                        diffSeconds < 3600 -> "il y a ${diffSeconds / 60} min"
                        diffSeconds < 86400 -> "il y a ${diffSeconds / 3600} h"
                        diffSeconds < 172800 -> "hier"
                        else -> "il y a ${diffSeconds / 86400} j"
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
                message = formatGitException(e, "Impossible de lire l'historique"),
                throwable = e
            )
        }
    }

    suspend fun parseMergeConflicts(
        projectDir: File
    ): GitOperationResult<List<ParsedConflictHunk>> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Success(emptyList())
            }
            Git.open(projectDir).use { git ->
                val conflictingFiles = git.status().call().conflicting.toSortedSet()
                val hunks = mutableListOf<ParsedConflictHunk>()

                for (relPath in conflictingFiles) {
                    val file = File(projectDir, relPath)
                    if (!file.isFile) continue
                    val lines = file.readLines()
                    var i = 0
                    var hunkIndex = 0
                    while (i < lines.size) {
                        if (lines[i].startsWith("<<<<<<<")) {
                            val startLine = i + 1
                            val localLines = mutableListOf<String>()
                            val remoteLines = mutableListOf<String>()
                            var inRemote = false
                            i++
                            while (i < lines.size && !lines[i].startsWith(">>>>>>>")) {
                                if (lines[i].startsWith("=======")) {
                                    inRemote = true
                                } else if (inRemote) {
                                    remoteLines.add(lines[i])
                                } else {
                                    localLines.add(lines[i])
                                }
                                i++
                            }
                            hunkIndex++
                            hunks.add(
                                ParsedConflictHunk(
                                    id = "${relPath}#$hunkIndex",
                                    filePath = relPath,
                                    fileName = relPath.substringAfterLast('/'),
                                    startLine = startLine,
                                    localLines = localLines,
                                    remoteLines = remoteLines
                                )
                            )
                        }
                        i++
                    }
                }
                GitOperationResult.Success(hunks)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Impossible d'analyser les conflits"),
                throwable = e
            )
        }
    }

    /**
     * Resolves conflict hunks in files using a map from hunkId ("relPath#index") -> keepLocal (true=LOCAL, false=REMOTE),
     * then stages all resolved files and commits the merge.
     */
    suspend fun resolveConflictsAndCommit(
        projectDir: File,
        keepLocalByHunkId: Map<String, Boolean>,
        authorName: String,
        authorEmail: String
    ): GitOperationResult<String> = withContext(Dispatchers.IO) {
        try {
            Git.open(projectDir).use { git ->
                val conflictingFiles = git.status().call().conflicting.toSortedSet()
                for (relPath in conflictingFiles) {
                    val file = File(projectDir, relPath)
                    if (!file.isFile) continue
                    val lines = file.readLines()
                    val resolved = mutableListOf<String>()
                    var i = 0
                    var hunkIndex = 0
                    while (i < lines.size) {
                        if (lines[i].startsWith("<<<<<<<")) {
                            hunkIndex++
                            val hunkId = "${relPath}#$hunkIndex"
                            val keepLocal = keepLocalByHunkId[hunkId] ?: true
                            val localLines = mutableListOf<String>()
                            val remoteLines = mutableListOf<String>()
                            var inRemote = false
                            i++
                            while (i < lines.size && !lines[i].startsWith(">>>>>>>")) {
                                if (lines[i].startsWith("=======")) {
                                    inRemote = true
                                } else if (inRemote) {
                                    remoteLines.add(lines[i])
                                } else {
                                    localLines.add(lines[i])
                                }
                                i++
                            }
                            resolved.addAll(if (keepLocal) localLines else remoteLines)
                        } else {
                            resolved.add(lines[i])
                        }
                        i++
                    }
                    file.writeText(resolved.joinToString("\n") + if (resolved.isNotEmpty()) "\n" else "")
                    git.add().addFilepattern(relPath).call()
                }

                val author = PersonIdent(
                    authorName.ifBlank { "NEXCODE Developer" },
                    authorEmail.ifBlank { "dev@nexcode.local" }
                )
                val commit = git.commit()
                    .setMessage("merge: résolution des conflits")
                    .setAuthor(author)
                    .setCommitter(author)
                    .call()

                GitOperationResult.Success(commit.name)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Erreur lors de la résolution des conflits"),
                throwable = e
            )
        }
    }

    suspend fun abortMerge(projectDir: File): GitOperationResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, ".git").exists()) {
                return@withContext GitOperationResult.Success(Unit)
            }
            Git.open(projectDir).use { git ->
                git.repository.writeMergeHeads(null)
                git.repository.writeMergeCommitMsg(null)
                git.reset().setMode(ResetCommand.ResetType.HARD).call()
                GitOperationResult.Success(Unit)
            }
        } catch (e: Exception) {
            GitOperationResult.Error(
                message = formatGitException(e, "Impossible d'annuler le merge"),
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
                message = formatGitException(e, "Erreur de lecture du statut git"),
                throwable = e
            )
        }
    }

    private fun formatGitException(e: Exception, fallback: String): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("not authorized", ignoreCase = true) || raw.contains("401") || raw.contains("403") ->
                "Authentification GitHub refusée. Vérifiez votre jeton d'accès personnel."
            raw.contains("UnknownHostException", ignoreCase = true) ||
                raw.contains("ConnectException", ignoreCase = true) ||
                raw.contains("SocketTimeoutException", ignoreCase = true) ||
                raw.contains("cannot open git-upload-pack", ignoreCase = true) ||
                raw.contains("cannot open git-receive-pack", ignoreCase = true) ->
                "Connexion à github.com indisponible."
            raw.isNotBlank() -> raw
            else -> fallback
        }
    }
}
