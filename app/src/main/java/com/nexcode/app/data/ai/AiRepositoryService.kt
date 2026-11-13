package com.nexcode.app.data.ai

import android.content.Context
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import com.nexcode.app.ui.screens.chat.DiffItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class AiRepositoryService(
    private val context: Context,
    private val vault: NexcodeSecureVault = NexcodeSecureVault(context),
    private val preferences: NexcodePreferences = NexcodePreferences(context),
    private val client: AiStreamingClient = OkHttpAiStreamingClient()
) {

    suspend fun loadConfig(): AiEndpointConfig {
        val providerName = runCatching { preferences.aiProviderFlow.first() }.getOrDefault("Anthropic")
        val model = runCatching { preferences.aiModelFlow.first() }.getOrDefault("claude-sonnet-4.5")
        val customUrl = runCatching { preferences.aiCustomUrlFlow.first() }.getOrDefault("")
        val apiKey = vault.getApiKey().orEmpty()
        return AiEndpointConfig(
            provider = AiProviderType.fromDisplayName(providerName),
            model = model.ifBlank { "claude-sonnet-4.5" },
            apiKey = apiKey,
            customBaseUrl = customUrl
        )
    }

    suspend fun saveConfig(
        providerName: String,
        model: String,
        apiKey: String? = null,
        customBaseUrl: String? = null
    ) {
        preferences.setAiProvider(providerName)
        if (model.isNotBlank()) {
            preferences.setAiModel(model.trim())
        }
        if (customBaseUrl != null) {
            preferences.setAiCustomUrl(customBaseUrl.trim())
        }
        if (apiKey != null) {
            if (apiKey.isBlank()) {
                vault.clearApiKey()
            } else {
                vault.saveApiKey(apiKey.trim())
            }
        }
    }

    suspend fun testConnection(overrideConfig: AiEndpointConfig? = null): AiConnectionTestResult {
        val cfg = overrideConfig ?: loadConfig()
        return client.testConnection(cfg)
    }

    suspend fun streamAssistantResponse(
        history: List<AiChatRequestMessage>,
        projectName: String,
        projectDir: File?,
        contextFiles: List<String>,
        overrideModel: String? = null
    ): Flow<AiStreamEvent> {
        val baseCfg = loadConfig()
        val effectiveConfig = if (!overrideModel.isNullOrBlank()) {
            baseCfg.copy(model = overrideModel.trim())
        } else {
            baseCfg
        }
        val systemPrompt = buildSystemPrompt(projectName, projectDir, contextFiles)
        return client.streamChat(
            config = effectiveConfig,
            systemPrompt = systemPrompt,
            messages = history
        )
    }

    suspend fun buildContextSnippet(projectDir: File?, contextFiles: List<String>): String =
        withContext(Dispatchers.IO) {
            if (projectDir == null || !projectDir.exists() || contextFiles.isEmpty()) {
                return@withContext ""
            }
            val sb = StringBuilder()
            contextFiles.take(8).forEach { requested ->
                val (relPath, diskFile) = AiResponseParser.resolveProjectFile(projectDir, requested)
                if (diskFile != null && diskFile.isFile) {
                    val content = runCatching { diskFile.readText() }.getOrDefault("")
                    val bounded = if (content.length > 12_000) {
                        content.take(12_000) + "\n// ... (tronqué)"
                    } else {
                        content
                    }
                    sb.append("\n### Fichier : ").append(relPath).append("\n```\n")
                    sb.append(bounded).append("\n```\n")
                }
            }
            sb.toString()
        }

    private suspend fun buildSystemPrompt(
        projectName: String,
        projectDir: File?,
        contextFiles: List<String>
    ): String {
        val contextCode = buildContextSnippet(projectDir, contextFiles)
        val projectHeader = if (projectName.isNotBlank()) {
            "Projet Android actif : $projectName."
        } else {
            "Aucun projet local lié actuellement."
        }
        return buildString {
            appendLine("Tu es un assistant expert Android (Kotlin, Jetpack Compose, Gradle) intégré dans NEXCODE.")
            appendLine(projectHeader)
            appendLine("Réponds de façon concise et précise en français.")
            appendLine("Règles pour proposer des modifications de fichiers :")
            appendLine("1. Ne prétends jamais avoir déjà écrit sur le disque : toutes tes propositions passent par l'écran de revue de l'utilisateur.")
            appendLine("2. Pour chaque fichier créé ou modifié, fournis un bloc de code Markdown en précisant le chemin relatif dans l'en-tête du bloc, par exemple :")
            appendLine("```kotlin file=app/src/main/java/MainActivity.kt")
            appendLine("// contenu complet du fichier mis à jour")
            appendLine("```")
            if (contextCode.isNotBlank()) {
                appendLine("\nFichiers de contexte actuels du projet :")
                append(contextCode)
            }
        }
    }

    suspend fun writeReviewedDiffToDisk(projectDir: File, diffItem: DiffItem): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetRelative = diffItem.relativePath.ifBlank { diffItem.fileName }
                val (_, resolvedFile) = AiResponseParser.resolveProjectFile(projectDir, targetRelative)
                val targetFile = resolvedFile ?: File(projectDir, targetRelative)
                val canonicalRoot = projectDir.canonicalPath
                val canonicalTarget = targetFile.canonicalPath
                if (!canonicalTarget.startsWith("$canonicalRoot${File.separator}")) {
                    throw SecurityException("Chemin de fichier hors du répertoire du projet : $targetRelative")
                }
                targetFile.parentFile?.mkdirs()
                targetFile.writeText(diffItem.proposedContent)
                targetFile
            }
        }

    fun listProjectRelativeFiles(projectDir: File?, maxCount: Int = 80): List<String> {
        if (projectDir == null || !projectDir.exists()) return emptyList()
        val extensions = setOf("kt", "kts", "java", "xml", "gradle", "json", "md", "properties", "yml", "yaml", "dart")
        return projectDir.walkTopDown()
            .onEnter { it.name != ".git" && it.name != "build" && it.name != ".gradle" && it.name != ".idea" }
            .filter { it.isFile && it.extension.lowercase() in extensions }
            .map { it.relativeTo(projectDir).path.replace(File.separatorChar, '/') }
            .sortedWith(
                compareBy<String>(
                    { !it.endsWith("MainActivity.kt") && !it.endsWith("MainActivity.java") },
                    { !it.endsWith("build.gradle.kts") && !it.endsWith("build.gradle") },
                    { !it.endsWith("strings.xml") },
                    { it }
                )
            )
            .take(maxCount)
            .toList()
    }
}
