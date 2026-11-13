package com.nexcode.app.ui.screens.chat

import com.nexcode.app.data.ai.ParsedCodeBlock

enum class ChatScreenState {
    DEFAULT,
    ANALYZING
}

enum class MessageAuthor {
    USER,
    AI
}

data class ChatMessage(
    val id: String,
    val author: MessageAuthor,
    val authorLabel: String = if (author == MessageAuthor.USER) "VOUS" else "IA",
    val timeString: String,
    val text: String,
    val codeBlocks: List<ParsedCodeBlock> = emptyList(),
    val codeHeader: String? = codeBlocks.firstOrNull()?.headerLabel,
    val codeContent: String? = codeBlocks.firstOrNull()?.content,
    val isStreaming: Boolean = false,
    val showApplyActions: Boolean = false,
    val adviceText: String? = null
)

data class ConversationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val chipText: String,
    val chipVariant: ConversationChipVariant
)

enum class ConversationChipVariant {
    ACCENT,
    SUCCESS,
    OUTLINED
}

data class DiffItem(
    val fileName: String,
    val relativePath: String = fileName,
    val additionsCount: Int,
    val deletionsCount: Int,
    val statusText: String,
    val statusVariant: DiffStatusVariant,
    val lines: List<DiffLine>,
    val proposedContent: String = "",
    val isNewFile: Boolean = false
)

data class DiffLine(
    val type: DiffLineType,
    val content: String
)

enum class DiffLineType {
    CONTEXT,
    ADDITION,
    DELETION
}

enum class DiffStatusVariant {
    SUCCESS,
    WARNING
}

data class ChatUiState(
    val projectId: Long? = null,
    val projectName: String = "",
    val projectLocalPath: String = "",
    val providerName: String = "Anthropic",
    val providerAuthorLabel: String = "CLAUDE",
    val modelName: String = "claude-sonnet-4.5",
    val hasConfiguredAi: Boolean = false,
    val contextFiles: List<String> = emptyList(),
    val availableProjectFiles: List<String> = emptyList(),
    val showFilePickerSheet: Boolean = false,
    val chatState: ChatScreenState = ChatScreenState.DEFAULT,
    val inputMessage: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val analyzingUserPrompt: String = "",
    val analyzingFile: String = "",
    val analyzingLinesCount: Int = 0,
    val chatError: String? = null,
    val isMissingApiKey: Boolean = false,
    val userNotice: String? = null,
    // S16 : Historique
    val searchQuery: String = "",
    val conversations: List<ConversationItem> = emptyList(),
    val totalConversationsCount: Int = 0,
    // S17 : Revue des modifications
    val diffItems: List<DiffItem> = emptyList(),
    val totalAdditions: Int = 0,
    val totalDeletions: Int = 0,
    val totalModificationsCount: Int = 0,
    val totalFilesCount: Int = 0,
    val reviewNotice: String? = null,
    // Modales
    val showModelSelector: Boolean = false
)
