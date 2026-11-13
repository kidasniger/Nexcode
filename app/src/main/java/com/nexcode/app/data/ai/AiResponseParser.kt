package com.nexcode.app.data.ai

import com.nexcode.app.ui.screens.chat.DiffItem
import com.nexcode.app.ui.screens.chat.DiffLine
import com.nexcode.app.ui.screens.chat.DiffLineType
import com.nexcode.app.ui.screens.chat.DiffStatusVariant
import java.io.File

object AiResponseParser {

    private val FILE_FENCE_REGEX = Regex(
        pattern = "```([A-Za-z0-9_+.-]*)(?:\\s+(?:file|path)=([^\\s`]+))?\\s*\\n([\\s\\S]*?)```",
        options = setOf(RegexOption.MULTILINE)
    )

    private val PRECEDING_FILENAME_REGEX = Regex(
        pattern = "(?:\\*\\*|`|###?\\s+)?([A-Za-z0-9_./-]+\\.(?:kt|java|xml|kts|gradle|json|md|txt|properties|yml|yaml|dart))(?:\\*\\*|`|:)?\\s*$"
    )

    private val FIRST_LINE_PATH_COMMENT_REGEX = Regex(
        pattern = "^(?://|/\\*|#|<!--)\\s*(?:file|fichier|path)\\s*:\\s*([A-Za-z0-9_./-]+\\.[A-Za-z0-9]+)"
    )

    fun parseMarkdownResponse(markdown: String, isStreaming: Boolean = false): ParsedAiAssistantResponse {
        val codeBlocks = mutableListOf<ParsedCodeBlock>()
        val fileProposals = mutableListOf<ProposedFileChange>()
        val proseBuilder = StringBuilder()

        var lastIndex = 0
        FILE_FENCE_REGEX.findAll(markdown).forEach { match ->
            val beforeText = markdown.substring(lastIndex, match.range.first)
            if (beforeText.isNotBlank()) {
                proseBuilder.append(beforeText.trim()).append("\n\n")
            }

            val language = match.groupValues[1].trim().ifEmpty { "kotlin" }
            val fenceFile = match.groupValues[2].trim().ifBlank { null }
            val rawCode = match.groupValues[3].trimEnd('\n', '\r')

            val inferredFromProse = fenceFile ?: extractFilenameFromPrecedingProse(beforeText)
            val inferredFromCode = inferredFromProse ?: extractFilenameFromFirstLine(rawCode)
            val cleanCode = stripFirstLineFileCommentIfPresent(rawCode, inferredFromCode)

            val header = when {
                inferredFromCode != null && !language.equals("diff", ignoreCase = true) ->
                    "${inferredFromCode.substringAfterLast('/')} · $language"
                inferredFromCode != null -> inferredFromCode.substringAfterLast('/')
                else -> language
            }

            codeBlocks.add(
                ParsedCodeBlock(
                    language = language,
                    filePath = inferredFromCode,
                    headerLabel = header,
                    content = cleanCode
                )
            )

            if (inferredFromCode != null && !language.equals("diff", ignoreCase = true) && cleanCode.isNotBlank()) {
                fileProposals.add(ProposedFileChange(filePath = inferredFromCode, content = cleanCode))
            }

            lastIndex = match.range.last + 1
        }

        val tail = markdown.substring(lastIndex)
        if (isStreaming && tail.contains("```")) {
            // Bloc de code en cours de réception (balise fermante pas encore reçue)
            val openIdx = tail.indexOf("```")
            val beforeOpen = tail.substring(0, openIdx)
            if (beforeOpen.isNotBlank()) {
                proseBuilder.append(beforeOpen.trim())
            }
            val afterOpen = tail.substring(openIdx + 3)
            val firstNewline = afterOpen.indexOf('\n')
            if (firstNewline >= 0) {
                val info = afterOpen.substring(0, firstNewline).trim()
                val partialCode = afterOpen.substring(firstNewline + 1)
                val lang = info.substringBefore(' ').ifBlank { "kotlin" }
                val fileMatch = Regex("(?:file|path)=([^\\s`]+)").find(info)?.groupValues?.get(1)
                    ?: extractFilenameFromPrecedingProse(beforeOpen)
                codeBlocks.add(
                    ParsedCodeBlock(
                        language = lang,
                        filePath = fileMatch,
                        headerLabel = fileMatch?.substringAfterLast('/') ?: lang,
                        content = partialCode
                    )
                )
            }
        } else if (tail.isNotBlank()) {
            proseBuilder.append(tail.trim())
        }

        // Dédupliquer les propositions de fichiers (garder la plus récente pour un même chemin)
        val dedupProposals = linkedMapOf<String, ProposedFileChange>()
        fileProposals.forEach { prop ->
            dedupProposals[prop.filePath] = prop
        }

        return ParsedAiAssistantResponse(
            proseText = proseBuilder.toString().trim(),
            codeBlocks = codeBlocks,
            fileProposals = dedupProposals.values.toList()
        )
    }

    fun generateDiffItems(
        proposals: List<ProposedFileChange>,
        projectDir: File?
    ): List<DiffItem> {
        return proposals.mapNotNull { proposal ->
            val resolved = resolveProjectFile(projectDir, proposal.filePath)
            val relativePath = resolved.first
            val diskFile = resolved.second
            val isNewFile = diskFile == null || !diskFile.exists()
            val oldText = if (diskFile != null && diskFile.isFile) {
                runCatching { diskFile.readText() }.getOrDefault("")
            } else {
                ""
            }
            buildDiffItem(
                displayName = relativePath.substringAfterLast('/'),
                relativePath = relativePath,
                oldContent = oldText,
                newContent = proposal.content,
                isNewFile = isNewFile
            )
        }
    }

    fun buildDiffItem(
        displayName: String,
        relativePath: String,
        oldContent: String,
        newContent: String,
        isNewFile: Boolean
    ): DiffItem {
        val oldLines = if (oldContent.isEmpty()) emptyList() else oldContent.replace("\r\n", "\n").lines()
        val newLines = if (newContent.isEmpty()) emptyList() else newContent.replace("\r\n", "\n").lines()

        val diffLines = if (isNewFile) {
            newLines.map { DiffLine(DiffLineType.ADDITION, it) }
        } else {
            computeLineDiff(oldLines, newLines)
        }

        val additions = diffLines.count { it.type == DiffLineType.ADDITION }
        val deletions = diffLines.count { it.type == DiffLineType.DELETION }

        return DiffItem(
            fileName = displayName,
            relativePath = relativePath,
            additionsCount = additions,
            deletionsCount = deletions,
            statusText = if (isNewFile) "Nouveau" else "En attente",
            statusVariant = if (isNewFile) DiffStatusVariant.SUCCESS else DiffStatusVariant.WARNING,
            lines = diffLines,
            proposedContent = newContent,
            isNewFile = isNewFile
        )
    }

    /**
     * Calcule un diff ligne à ligne via LCS (Longest Common Subsequence) avec contexte compact.
     */
    fun computeLineDiff(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        val n = oldLines.size
        val m = newLines.size
        if (n * m > 250_000) {
            // Repli sûr pour très grands fichiers
            val out = mutableListOf<DiffLine>()
            oldLines.forEach { out.add(DiffLine(DiffLineType.DELETION, it)) }
            newLines.forEach { out.add(DiffLine(DiffLineType.ADDITION, it)) }
            return out
        }

        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (oldLines[i] == newLines[j]) {
                    dp[i + 1][j + 1] + 1
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val rawOps = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                oldLines[i] == newLines[j] -> {
                    rawOps.add(DiffLine(DiffLineType.CONTEXT, oldLines[i]))
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> {
                    rawOps.add(DiffLine(DiffLineType.DELETION, oldLines[i]))
                    i++
                }
                else -> {
                    rawOps.add(DiffLine(DiffLineType.ADDITION, newLines[j]))
                    j++
                }
            }
        }
        while (i < n) {
            rawOps.add(DiffLine(DiffLineType.DELETION, oldLines[i]))
            i++
        }
        while (j < m) {
            rawOps.add(DiffLine(DiffLineType.ADDITION, newLines[j]))
            j++
        }

        // Ne conserver que les lignes modifiées + 2 lignes de contexte autour pour une revue lisible
        val keep = BooleanArray(rawOps.size)
        for (idx in rawOps.indices) {
            if (rawOps[idx].type != DiffLineType.CONTEXT) {
                val from = (idx - 2).coerceAtLeast(0)
                val to = (idx + 2).coerceAtMost(rawOps.lastIndex)
                for (k in from..to) {
                    keep[k] = true
                }
            }
        }

        val compacted = rawOps.filterIndexed { idx, _ -> keep[idx] }
        return if (compacted.isNotEmpty()) compacted else rawOps.take(12)
    }

    fun resolveProjectFile(projectDir: File?, requestedPath: String): Pair<String, File?> {
        val sanitized = requestedPath.trim().trimStart('/').replace("\\", "/")
        if (projectDir == null || !projectDir.exists()) {
            return sanitized to null
        }

        val canonicalRoot = runCatching { projectDir.canonicalPath }.getOrNull()
            ?: return sanitized to null

        val directFile = File(projectDir, sanitized)
        val directCanonical = runCatching { directFile.canonicalPath }.getOrNull()
        if (directCanonical != null &&
            (directCanonical.startsWith("$canonicalRoot${File.separator}") || directCanonical == canonicalRoot) &&
            directFile.isFile
        ) {
            return sanitized to directFile
        }

        // Recherche d'un fichier existant portant le même nom (ex: MainActivity.kt -> app/src/main/.../MainActivity.kt)
        val targetName = sanitized.substringAfterLast('/')
        val found = projectDir.walkTopDown()
            .onEnter { it.name != ".git" && it.name != "build" && it.name != ".gradle" }
            .firstOrNull { it.isFile && it.name == targetName }

        if (found != null) {
            val rel = found.relativeTo(projectDir).path.replace(File.separatorChar, '/')
            return rel to found
        }

        // Sinon, vérifier que le chemin cible pour un nouveau fichier reste dans projectDir
        val newTarget = File(projectDir, sanitized)
        val newCanonical = runCatching { newTarget.canonicalPath }.getOrNull()
        val safeTarget = if (newCanonical != null &&
            newCanonical.startsWith("$canonicalRoot${File.separator}")
        ) {
            newTarget
        } else {
            File(projectDir, targetName)
        }
        val safeRel = safeTarget.relativeTo(projectDir).path.replace(File.separatorChar, '/')
        return safeRel to safeTarget
    }

    private fun extractFilenameFromPrecedingProse(prose: String): String? {
        val lastLines = prose.trimEnd().lines().takeLast(2)
        for (line in lastLines.reversed()) {
            val match = PRECEDING_FILENAME_REGEX.find(line.trim())
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractFilenameFromFirstLine(code: String): String? {
        val firstLine = code.lineSequence().firstOrNull()?.trim() ?: return null
        val match = FIRST_LINE_PATH_COMMENT_REGEX.find(firstLine)
        return match?.groupValues?.get(1)
    }

    private fun stripFirstLineFileCommentIfPresent(code: String, detectedFile: String?): String {
        if (detectedFile == null) return code
        val lines = code.lines()
        val first = lines.firstOrNull()?.trim() ?: return code
        return if (FIRST_LINE_PATH_COMMENT_REGEX.containsMatchIn(first)) {
            lines.drop(1).joinToString("\n").trimStart('\n')
        } else {
            code
        }
    }
}
