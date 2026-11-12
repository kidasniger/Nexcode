package com.nexcode.app.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    context: Context? = null
) : ViewModel() {

    private val database = context?.let { NexcodeDatabase.getInstance(it) }
    private val conversationDao = database?.conversationDao()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        initializeDefaultData()
    }

    private fun initializeDefaultData() {
        val weatherCodeSnippet = """
@Composable
fun WeatherDetails(city: City) {
    Column {
        DetailRow(
            label = stringResource(R.string.unit_humidity),
            value = "${'$'}{city.humidity} %"
        )
        DetailRow(
            label = stringResource(R.string.unit_wind),
            value = "${'$'}{city.windKmh} km/h"
        )
    }
}
        """.trimIndent()

        val defMessages = listOf(
            ChatMessage(
                id = "m1",
                author = MessageAuthor.USER,
                timeString = "14:02",
                text = "Ajoute un écran de détails météo qui affiche l'humidité et le vent de la ville sélectionnée."
            ),
            ChatMessage(
                id = "m2",
                author = MessageAuthor.AI,
                timeString = "14:02",
                text = "Voici un composable WeatherDetails qui réutilise votre thème et vos chaînes existantes :",
                codeHeader = "WeatherDetails.kt · nouveau fichier",
                codeContent = weatherCodeSnippet,
                showApplyActions = true
            ),
            ChatMessage(
                id = "m3",
                author = MessageAuthor.AI,
                timeString = "14:02",
                text = "",
                adviceText = "3 fichiers concernés : WeatherDetails.kt (nouveau), strings.xml (+2 chaînes), City.kt (+1 champ). Passez en revue avant application."
            )
        )

        val convos = listOf(
            ConversationItem(
                id = "c1",
                title = "Écran de détails météo",
                subtitle = "Il y a 5 min · 4 messages · Nimbus Météo",
                chipText = "3 modifs",
                chipVariant = ConversationChipVariant.ACCENT
            ),
            ConversationItem(
                id = "c2",
                title = "Refactoriser le gestionnaire de cache",
                subtitle = "Hier · 12 messages · Nimbus Météo",
                chipText = "Appliqué",
                chipVariant = ConversationChipVariant.SUCCESS
            ),
            ConversationItem(
                id = "c3",
                title = "Corriger l'erreur ProGuard (release)",
                subtitle = "Le 8 nov. · 9 messages · TicketFlash",
                chipText = "Appliqué",
                chipVariant = ConversationChipVariant.SUCCESS
            ),
            ConversationItem(
                id = "c4",
                title = "Traduire les chaînes en anglais",
                subtitle = "Le 2 nov. · 6 messages · NotesSimples",
                chipText = "Annulée",
                chipVariant = ConversationChipVariant.OUTLINED
            )
        )

        val diffs = listOf(
            DiffItem(
                fileName = "MainActivity.kt",
                additionsCount = 18,
                deletionsCount = 4,
                statusText = "Nouveau",
                statusVariant = DiffStatusVariant.SUCCESS,
                lines = listOf(
                    DiffLine(DiffLineType.CONTEXT, "fun CityScreen(city: String) {"),
                    DiffLine(DiffLineType.DELETION, "    Text(text = \"Météo à \$city\")"),
                    DiffLine(DiffLineType.ADDITION, "    Column {"),
                    DiffLine(DiffLineType.ADDITION, "        CityHeader(city = city)"),
                    DiffLine(DiffLineType.ADDITION, "        WeatherDetails(city = repo.find(city))"),
                    DiffLine(DiffLineType.ADDITION, "    }"),
                    DiffLine(DiffLineType.CONTEXT, "}")
                )
            ),
            DiffItem(
                fileName = "strings.xml",
                additionsCount = 6,
                deletionsCount = 0,
                statusText = "Modifié",
                statusVariant = DiffStatusVariant.SUCCESS,
                lines = listOf(
                    DiffLine(DiffLineType.CONTEXT, "<resources>"),
                    DiffLine(DiffLineType.ADDITION, " <string name=\"unit_humidity\">Humidité</string>"),
                    DiffLine(DiffLineType.ADDITION, " <string name=\"unit_wind\">Vent</string>"),
                    DiffLine(DiffLineType.ADDITION, " <string name=\"title_details\">Détails météo</string>"),
                    DiffLine(DiffLineType.CONTEXT, "</resources>")
                )
            ),
            DiffItem(
                fileName = "City.kt",
                additionsCount = 3,
                deletionsCount = 1,
                statusText = "En attente",
                statusVariant = DiffStatusVariant.WARNING,
                lines = listOf(
                    DiffLine(DiffLineType.DELETION, "data class City(val name: String)"),
                    DiffLine(DiffLineType.ADDITION, "data class City("),
                    DiffLine(DiffLineType.ADDITION, "  val name: String,"),
                    DiffLine(DiffLineType.ADDITION, "  val humidity: Int, val windKmh: Int)")
                )
            )
        )

        _uiState.update {
            it.copy(
                defaultMessages = defMessages,
                conversations = convos,
                diffItems = diffs
            )
        }
    }

    fun setChatScreenState(state: ChatScreenState) {
        _uiState.update { it.copy(chatState = state) }
    }

    fun toggleChatState() {
        _uiState.update {
            it.copy(
                chatState = if (it.chatState == ChatScreenState.DEFAULT) {
                    ChatScreenState.ANALYZING
                } else {
                    ChatScreenState.DEFAULT
                }
            )
        }
    }

    fun updateInputMessage(message: String) {
        _uiState.update { it.copy(inputMessage = message) }
    }

    fun sendMessage() {
        val currentInput = _uiState.value.inputMessage.trim()
        if (currentInput.isEmpty()) return

        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            author = MessageAuthor.USER,
            timeString = "14:05",
            text = currentInput
        )

        _uiState.update { state ->
            state.copy(
                inputMessage = "",
                chatState = ChatScreenState.ANALYZING,
                analyzingUserPrompt = currentInput
            )
        }
    }

    fun stopAnalysis() {
        _uiState.update { it.copy(chatState = ChatScreenState.DEFAULT) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openModelSelector() {
        _uiState.update { it.copy(showModelSelector = true) }
    }

    fun closeModelSelector() {
        _uiState.update { it.copy(showModelSelector = false) }
    }

    fun selectModel(model: String) {
        _uiState.update { it.copy(modelName = model, showModelSelector = false) }
    }

    fun acceptAllDiffs() {
        _uiState.update { state ->
            val updated = state.diffItems.map { it.copy(statusText = "Appliqué", statusVariant = DiffStatusVariant.SUCCESS) }
            state.copy(diffItems = updated)
        }
    }

    fun rejectAllDiffs() {
        _uiState.update { state ->
            val updated = state.diffItems.map { it.copy(statusText = "Refusé", statusVariant = DiffStatusVariant.WARNING) }
            state.copy(diffItems = updated)
        }
    }

    fun acceptDiffItem(fileName: String) {
        _uiState.update { state ->
            val updated = state.diffItems.map {
                if (it.fileName == fileName) it.copy(statusText = "Appliqué", statusVariant = DiffStatusVariant.SUCCESS) else it
            }
            state.copy(diffItems = updated)
        }
    }

    fun rejectDiffItem(fileName: String) {
        _uiState.update { state ->
            val updated = state.diffItems.map {
                if (it.fileName == fileName) it.copy(statusText = "Refusé", statusVariant = DiffStatusVariant.WARNING) else it
            }
            state.copy(diffItems = updated)
        }
    }
}
