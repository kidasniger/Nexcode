package com.nexcode.app.data.ai

enum class AiProviderType(val displayName: String, val shortAuthorLabel: String) {
    ANTHROPIC("Anthropic", "CLAUDE"),
    OPENAI("OpenAI", "OPENAI"),
    CUSTOM("Serveur personnalisé", "IA");

    companion object {
        fun fromDisplayName(name: String): AiProviderType {
            return when {
                name.equals("OpenAI", ignoreCase = true) -> OPENAI
                name.equals("Serveur personnalisé", ignoreCase = true) ||
                    name.contains("ollama", ignoreCase = true) ||
                    name.contains("custom", ignoreCase = true) -> CUSTOM
                else -> ANTHROPIC
            }
        }
    }
}

data class AiEndpointConfig(
    val provider: AiProviderType,
    val model: String,
    val apiKey: String,
    val customBaseUrl: String = ""
)

data class AiChatRequestMessage(
    val role: String,
    val content: String
)

sealed class AiStreamEvent {
    data class Delta(val chunk: String) : AiStreamEvent()
    data class Completed(val fullText: String) : AiStreamEvent()
    data class Error(
        val message: String,
        val code: Int? = null,
        val isNetworkError: Boolean = false
    ) : AiStreamEvent()
}

data class AiConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long = 0L,
    val message: String
)

data class ParsedCodeBlock(
    val language: String,
    val filePath: String?,
    val headerLabel: String,
    val content: String
)

data class ParsedAiAssistantResponse(
    val proseText: String,
    val codeBlocks: List<ParsedCodeBlock>,
    val fileProposals: List<ProposedFileChange>
)

data class ProposedFileChange(
    val filePath: String,
    val content: String
)
