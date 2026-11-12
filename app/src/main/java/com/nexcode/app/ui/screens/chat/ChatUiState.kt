package com.nexcode.app.ui.screens.chat

enum class ChatScreenState {
    DEFAULT,   // stv="def" : message avec code généré et conseil de revue
    ANALYZING  // stv="cours" : indicateur de frappe, spinner et bouton Stop
}

enum class MessageAuthor {
    USER,
    AI
}

data class ChatMessage(
    val id: String,
    val author: MessageAuthor,
    val timeString: String,
    val text: String,
    val codeHeader: String? = null,
    val codeContent: String? = null,
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
    ACCENT,  // .chip-acc (3 modifs)
    SUCCESS, // .chip-ok (Appliqué)
    OUTLINED // .chip-out (Annulée)
}

data class DiffItem(
    val fileName: String,
    val additionsCount: Int,
    val deletionsCount: Int,
    val statusText: String,
    val statusVariant: DiffStatusVariant,
    val lines: List<DiffLine>
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
    SUCCESS, // .chip-ok (Nouveau, Modifié)
    WARNING  // .chip-warn (En attente)
}

data class ChatUiState(
    val modelName: String = "claude-sonnet-4.5",
    val contextFiles: List<String> = listOf("MainActivity.kt", "Theme.kt", "strings.xml"),
    val chatState: ChatScreenState = ChatScreenState.DEFAULT,
    val inputMessage: String = "",
    val defaultMessages: List<ChatMessage> = emptyList(),
    val analyzingUserPrompt: String = "Refactorise CityRepository pour utiliser kotlinx-serialization.",
    val analyzingFile: String = "CityRepository.kt",
    val analyzingLinesCount: Int = 148,
    // S16 : Historique
    val searchQuery: String = "",
    val conversations: List<ConversationItem> = emptyList(),
    val totalConversationsCount: Int = 18,
    // S17 : Revue des modifications
    val diffItems: List<DiffItem> = emptyList(),
    val totalAdditions: Int = 27,
    val totalDeletions: Int = 5,
    val totalModificationsCount: Int = 3,
    val totalFilesCount: Int = 2,
    // Modales
    val showModelSelector: Boolean = false
)
