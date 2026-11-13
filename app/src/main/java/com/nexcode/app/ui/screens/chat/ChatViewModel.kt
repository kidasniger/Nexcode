package com.nexcode.app.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.ai.AiChatRequestMessage
import com.nexcode.app.data.ai.AiProviderType
import com.nexcode.app.data.ai.AiRepositoryService
import com.nexcode.app.data.ai.AiResponseParser
import com.nexcode.app.data.ai.AiStreamEvent
import com.nexcode.app.data.ai.ParsedCodeBlock
import com.nexcode.app.data.ai.ProposedFileChange
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.entity.ConversationEntity
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.ui.screens.projects.ProjectItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(
    context: Context? = null,
    overrideAiService: AiRepositoryService? = null
) : ViewModel() {

    private val aiService = overrideAiService ?: context?.let { AiRepositoryService(it) }
    private val preferences = context?.let { NexcodePreferences(it) }
    private val database = context?.let { NexcodeDatabase.getInstance(it) }
    private val conversationDao = database?.conversationDao()

    private var streamingJob: Job? = null
    private var historyObserverJob: Job? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeAiPreferences()
    }

    private fun observeAiPreferences() {
        val prefs = preferences ?: return
        viewModelScope.launch {
            combine(prefs.aiProviderFlow, prefs.aiModelFlow) { provider, model ->
                provider to model
            }.collect { (provider, model) ->
                val providerType = AiProviderType.fromDisplayName(provider)
                val cfg = aiService?.loadConfig()
                val configured = cfg != null && (
                    cfg.provider == AiProviderType.CUSTOM && cfg.customBaseUrl.isNotBlank() ||
                        cfg.apiKey.isNotBlank()
                    )
                _uiState.update { current ->
                    current.copy(
                        providerName = provider,
                        providerAuthorLabel = providerType.shortAuthorLabel,
                        modelName = model.ifBlank { current.modelName },
                        hasConfiguredAi = configured
                    )
                }
            }
        }
    }

    suspend fun refreshAiConfigStateSync() {
        val cfg = aiService?.loadConfig() ?: return
        val configured = (cfg.provider == AiProviderType.CUSTOM && cfg.customBaseUrl.isNotBlank()) ||
            cfg.apiKey.isNotBlank()
        _uiState.update {
            it.copy(
                providerName = cfg.provider.displayName,
                providerAuthorLabel = cfg.provider.shortAuthorLabel,
                modelName = cfg.model,
                hasConfiguredAi = configured
            )
        }
    }

    fun bindProject(project: ProjectItem?) {
        if (project == null) {
            historyObserverJob?.cancel()
            _uiState.update {
                it.copy(
                    projectId = null,
                    projectName = "",
                    projectLocalPath = "",
                    availableProjectFiles = emptyList(),
                    contextFiles = emptyList(),
                    messages = emptyList(),
                    conversations = emptyList(),
                    totalConversationsCount = 0,
                    diffItems = emptyList(),
                    totalAdditions = 0,
                    totalDeletions = 0,
                    totalModificationsCount = 0,
                    totalFilesCount = 0
                )
            }
            return
        }

        val sameProject = _uiState.value.projectId == project.id &&
            _uiState.value.projectLocalPath == project.localPath
        val projectDir = project.localPath.takeIf { it.isNotBlank() }?.let(::File)
        val discoveredFiles = aiService?.listProjectRelativeFiles(projectDir).orEmpty()
        val defaultContext = if (sameProject && _uiState.value.contextFiles.isNotEmpty()) {
            _uiState.value.contextFiles.filter { it in discoveredFiles || File(projectDir, it).exists() }
        } else {
            discoveredFiles.take(3)
        }

        _uiState.update {
            it.copy(
                projectId = project.id,
                projectName = project.name,
                projectLocalPath = project.localPath,
                availableProjectFiles = discoveredFiles,
                contextFiles = defaultContext
            )
        }

        if (!sameProject) {
            observeProjectConversations(project.id, project.name, projectDir)
        }
    }

    private fun observeProjectConversations(projectId: Long, projectName: String, projectDir: File?) {
        val dao = conversationDao ?: return
        historyObserverJob?.cancel()
        historyObserverJob = viewModelScope.launch {
            dao.getMessagesByProject(projectId).collect { entities ->
                val conversations = buildConversationItems(entities, projectName)
                _uiState.update { current ->
                    // Charger les messages persistés seulement si la session courante est vide
                    val shouldHydrate = current.messages.isEmpty() && entities.isNotEmpty()
                    val hydratedMessages = if (shouldHydrate) {
                        mapEntitiesToChatMessages(entities, current.providerAuthorLabel)
                    } else {
                        current.messages
                    }
                    val hydratedDiffs = if (shouldHydrate && current.diffItems.isEmpty()) {
                        val lastAiWithProposals = entities.lastOrNull { it.role == "assistant" }
                        if (lastAiWithProposals != null) {
                            val parsed = AiResponseParser.parseMarkdownResponse(lastAiWithProposals.content)
                            AiResponseParser.generateDiffItems(parsed.fileProposals, projectDir)
                        } else emptyList()
                    } else {
                        current.diffItems
                    }

                    current.copy(
                        messages = hydratedMessages,
                        conversations = conversations,
                        totalConversationsCount = conversations.size,
                        diffItems = hydratedDiffs,
                        totalAdditions = hydratedDiffs.sumOf { d -> d.additionsCount },
                        totalDeletions = hydratedDiffs.sumOf { d -> d.deletionsCount },
                        totalModificationsCount = hydratedDiffs.size,
                        totalFilesCount = hydratedDiffs.map { d -> d.relativePath }.distinct().size
                    )
                }
            }
        }
    }

    fun setChatScreenState(state: ChatScreenState) {
        _uiState.update { it.copy(chatState = state) }
    }

    fun updateInputMessage(message: String) {
        _uiState.update { it.copy(inputMessage = message, chatError = null) }
    }

    fun openFilePicker() {
        val projectDir = _uiState.value.projectLocalPath.takeIf { it.isNotBlank() }?.let(::File)
        val files = aiService?.listProjectRelativeFiles(projectDir).orEmpty()
        _uiState.update {
            it.copy(
                availableProjectFiles = files,
                showFilePickerSheet = true
            )
        }
    }

    fun closeFilePicker() {
        _uiState.update { it.copy(showFilePickerSheet = false) }
    }

    fun toggleContextFile(relativePath: String) {
        _uiState.update { current ->
            val updated = if (relativePath in current.contextFiles) {
                current.contextFiles - relativePath
            } else {
                (current.contextFiles + relativePath).take(8)
            }
            current.copy(contextFiles = updated)
        }
    }

    fun sendMessage() {
        val currentInput = _uiState.value.inputMessage.trim()
        if (currentInput.isEmpty()) return
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            sendPromptInternal(currentInput)
        }
    }

    suspend fun sendMessageSync(): Boolean {
        val currentInput = _uiState.value.inputMessage.trim()
        if (currentInput.isEmpty()) return false
        streamingJob?.cancel()
        return sendPromptInternal(currentInput)
    }

    fun sendErrorFixRequest(errorPrompt: String, errorFileHints: List<String> = emptyList()) {
        val cleanPrompt = errorPrompt.trim()
        if (cleanPrompt.isEmpty()) return

        // Ajouter automatiquement les fichiers cités dans l'erreur au contexte si présents dans le projet
        if (errorFileHints.isNotEmpty()) {
            _uiState.update { current ->
                val matched = errorFileHints.mapNotNull { hint ->
                    val nameOnly = hint.substringBefore(':').substringAfterLast('/')
                    current.availableProjectFiles.firstOrNull {
                        it == hint || it.endsWith("/$nameOnly") || it == nameOnly
                    }
                }
                val mergedContext = (current.contextFiles + matched).distinct().take(8)
                current.copy(contextFiles = mergedContext)
            }
        }

        _uiState.update { it.copy(inputMessage = cleanPrompt, chatError = null) }
        sendMessage()
    }

    private suspend fun sendPromptInternal(prompt: String): Boolean {
        val service = aiService ?: return false
        refreshAiConfigStateSync()

        val snapshot = _uiState.value
        val nowLabel = formatCurrentTime()
        val userMsgId = "user_${System.currentTimeMillis()}"
        val aiMsgId = "ai_${System.currentTimeMillis() + 1}"

        val userMsg = ChatMessage(
            id = userMsgId,
            author = MessageAuthor.USER,
            authorLabel = "VOUS",
            timeString = nowLabel,
            text = prompt
        )
        val initialAiMsg = ChatMessage(
            id = aiMsgId,
            author = MessageAuthor.AI,
            authorLabel = snapshot.providerAuthorLabel,
            timeString = nowLabel,
            text = "",
            isStreaming = true
        )

        val historyForApi = (snapshot.messages + userMsg)
            .filter { it.text.isNotBlank() || it.codeBlocks.isNotEmpty() }
            .takeLast(12)
            .map { msg ->
                val fullContent = buildString {
                    if (msg.text.isNotBlank()) append(msg.text)
                    msg.codeBlocks.forEach { block ->
                        append("\n```").append(block.language)
                        if (!block.filePath.isNullOrBlank()) {
                            append(" file=").append(block.filePath)
                        }
                        append("\n").append(block.content).append("\n```")
                    }
                }.trim()
                AiChatRequestMessage(
                    role = if (msg.author == MessageAuthor.USER) "user" else "assistant",
                    content = fullContent
                )
            }

        val firstCtxFile = snapshot.contextFiles.firstOrNull()?.substringAfterLast('/')
            ?: snapshot.projectName.ifBlank { "projet" }
        val projectDir = snapshot.projectLocalPath.takeIf { it.isNotBlank() }?.let(::File)
        val firstCtxLineCount = snapshot.contextFiles.firstOrNull()?.let { rel ->
            val resolved = AiResponseParser.resolveProjectFile(projectDir, rel).second
            if (resolved != null && resolved.isFile) runCatching { resolved.readLines().size }.getOrDefault(0) else 0
        } ?: 0

        _uiState.update { state ->
            state.copy(
                inputMessage = "",
                chatState = ChatScreenState.ANALYZING,
                analyzingUserPrompt = prompt,
                analyzingFile = firstCtxFile,
                analyzingLinesCount = firstCtxLineCount,
                chatError = null,
                isMissingApiKey = false,
                messages = state.messages + userMsg + initialAiMsg
            )
        }

        persistMessageSafe(
            projectId = snapshot.projectId,
            role = "user",
            content = prompt,
            attachedFiles = snapshot.contextFiles.joinToString(","),
            modelName = snapshot.modelName,
            hasDiffReview = false
        )

        val rawBuffer = StringBuilder()
        var succeeded = false

        service.streamAssistantResponse(
            history = historyForApi,
            projectName = snapshot.projectName,
            projectDir = projectDir,
            contextFiles = snapshot.contextFiles,
            overrideModel = snapshot.modelName
        ).collect { event ->
            when (event) {
                is AiStreamEvent.Delta -> {
                    rawBuffer.append(event.chunk)
                    val parsedPartial = AiResponseParser.parseMarkdownResponse(
                        markdown = rawBuffer.toString(),
                        isStreaming = true
                    )
                    updateStreamingMessage(aiMsgId, parsedPartial.proseText, parsedPartial.codeBlocks, isStreaming = true)
                }
                is AiStreamEvent.Completed -> {
                    val finalRaw = event.fullText.ifBlank { rawBuffer.toString() }
                    val parsed = AiResponseParser.parseMarkdownResponse(finalRaw, isStreaming = false)
                    val diffs = AiResponseParser.generateDiffItems(parsed.fileProposals, projectDir)
                    val advice = buildAdviceSummary(diffs)

                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == aiMsgId) {
                                msg.copy(
                                    text = parsed.proseText.ifBlank {
                                        if (parsed.codeBlocks.isNotEmpty()) "Voici les propositions générées :" else finalRaw
                                    },
                                    codeBlocks = parsed.codeBlocks,
                                    codeHeader = parsed.codeBlocks.firstOrNull()?.headerLabel,
                                    codeContent = parsed.codeBlocks.firstOrNull()?.content,
                                    isStreaming = false,
                                    showApplyActions = parsed.codeBlocks.isNotEmpty() || diffs.isNotEmpty(),
                                    adviceText = advice
                                )
                            } else msg
                        }

                        val activeDiffs = if (diffs.isNotEmpty()) diffs else state.diffItems
                        state.copy(
                            chatState = ChatScreenState.DEFAULT,
                            messages = updatedMessages,
                            diffItems = activeDiffs,
                            totalAdditions = activeDiffs.sumOf { it.additionsCount },
                            totalDeletions = activeDiffs.sumOf { it.deletionsCount },
                            totalModificationsCount = activeDiffs.size,
                            totalFilesCount = activeDiffs.map { it.relativePath }.distinct().size,
                            reviewNotice = null
                        )
                    }

                    persistMessageSafe(
                        projectId = snapshot.projectId,
                        role = "assistant",
                        content = finalRaw,
                        attachedFiles = snapshot.contextFiles.joinToString(","),
                        modelName = snapshot.modelName,
                        hasDiffReview = diffs.isNotEmpty()
                    )
                    succeeded = true
                }
                is AiStreamEvent.Error -> {
                    val partialRaw = rawBuffer.toString()
                    val parsedPartial = if (partialRaw.isNotBlank()) {
                        AiResponseParser.parseMarkdownResponse(partialRaw, isStreaming = false)
                    } else null
                    val cfg = service.loadConfig()
                    val missingKey = cfg.provider != AiProviderType.CUSTOM && cfg.apiKey.isBlank()

                    _uiState.update { state ->
                        val updatedMessages = if (partialRaw.isNotBlank() && parsedPartial != null) {
                            state.messages.map { msg ->
                                if (msg.id == aiMsgId) {
                                    msg.copy(
                                        text = parsedPartial.proseText.ifBlank { partialRaw },
                                        codeBlocks = parsedPartial.codeBlocks,
                                        codeHeader = parsedPartial.codeBlocks.firstOrNull()?.headerLabel,
                                        codeContent = parsedPartial.codeBlocks.firstOrNull()?.content,
                                        isStreaming = false,
                                        showApplyActions = parsedPartial.codeBlocks.isNotEmpty()
                                    )
                                } else msg
                            }
                        } else {
                            // Supprimer le placeholder vide si rien n'a été reçu
                            state.messages.filterNot { it.id == aiMsgId }
                        }
                        state.copy(
                            chatState = ChatScreenState.DEFAULT,
                            messages = updatedMessages,
                            chatError = event.message,
                            isMissingApiKey = missingKey || event.code == 401
                        )
                    }
                    succeeded = false
                }
            }
        }
        return succeeded
    }

    private fun updateStreamingMessage(
        aiMsgId: String,
        proseText: String,
        codeBlocks: List<ParsedCodeBlock>,
        isStreaming: Boolean
    ) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == aiMsgId) {
                    msg.copy(
                        text = proseText,
                        codeBlocks = codeBlocks,
                        codeHeader = codeBlocks.firstOrNull()?.headerLabel,
                        codeContent = codeBlocks.firstOrNull()?.content,
                        isStreaming = isStreaming
                    )
                } else msg
            }
            state.copy(messages = updated)
        }
    }

    fun stopAnalysis() {
        streamingJob?.cancel()
        streamingJob = null

        val projectDir = _uiState.value.projectLocalPath.takeIf { it.isNotBlank() }?.let(::File)
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.isStreaming) {
                    val hasAny = msg.text.isNotBlank() || msg.codeBlocks.isNotEmpty()
                    if (hasAny) {
                        msg.copy(
                            isStreaming = false,
                            showApplyActions = msg.codeBlocks.isNotEmpty()
                        )
                    } else {
                        msg.copy(
                            text = "Réponse interrompue avant réception du flux.",
                            isStreaming = false
                        )
                    }
                } else msg
            }
            val lastCodeProposals = updatedMessages.lastOrNull { it.author == MessageAuthor.AI }
                ?.codeBlocks
                ?.mapNotNull { block ->
                    block.filePath?.let { path -> ProposedFileChange(path, block.content) }
                }
                .orEmpty()
            val generatedDiffs = if (lastCodeProposals.isNotEmpty() && state.diffItems.isEmpty()) {
                AiResponseParser.generateDiffItems(lastCodeProposals, projectDir)
            } else state.diffItems

            state.copy(
                chatState = ChatScreenState.DEFAULT,
                messages = updatedMessages,
                diffItems = generatedDiffs,
                totalAdditions = generatedDiffs.sumOf { it.additionsCount },
                totalDeletions = generatedDiffs.sumOf { it.deletionsCount },
                totalModificationsCount = generatedDiffs.size,
                totalFilesCount = generatedDiffs.map { it.relativePath }.distinct().size
            )
        }
    }

    fun prepareReviewFromMessage(message: ChatMessage) {
        val current = _uiState.value
        val projectDir = current.projectLocalPath.takeIf { it.isNotBlank() }?.let(::File)
        val explicitProposals = message.codeBlocks.mapNotNull { block ->
            val targetPath = block.filePath
                ?: current.contextFiles.firstOrNull()
                ?: block.headerLabel.substringBefore(" · ").takeIf { it.contains('.') }
            targetPath?.let { ProposedFileChange(it, block.content) }
        }
        if (explicitProposals.isNotEmpty()) {
            val diffs = AiResponseParser.generateDiffItems(explicitProposals, projectDir)
            if (diffs.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(
                        diffItems = diffs,
                        totalAdditions = diffs.sumOf { it.additionsCount },
                        totalDeletions = diffs.sumOf { it.deletionsCount },
                        totalModificationsCount = diffs.size,
                        totalFilesCount = diffs.map { it.relativePath }.distinct().size,
                        reviewNotice = null
                    )
                }
            }
        }
    }

    fun startNewConversation() {
        streamingJob?.cancel()
        _uiState.update {
            it.copy(
                chatState = ChatScreenState.DEFAULT,
                messages = emptyList(),
                diffItems = emptyList(),
                totalAdditions = 0,
                totalDeletions = 0,
                totalModificationsCount = 0,
                totalFilesCount = 0,
                chatError = null,
                reviewNotice = null
            )
        }
    }

    fun selectConversationFromHistory(conversationId: String) {
        val dao = conversationDao ?: return
        val projectId = _uiState.value.projectId ?: return
        val projectDir = _uiState.value.projectLocalPath.takeIf { it.isNotBlank() }?.let(::File)
        viewModelScope.launch {
            val entities = runCatching { dao.getMessagesByProject(projectId).first() }.getOrDefault(emptyList())
            val messages = mapEntitiesToChatMessages(entities, _uiState.value.providerAuthorLabel)
            val lastAssistant = entities.lastOrNull { it.role == "assistant" }
            val diffs = if (lastAssistant != null) {
                val parsed = AiResponseParser.parseMarkdownResponse(lastAssistant.content)
                AiResponseParser.generateDiffItems(parsed.fileProposals, projectDir)
            } else emptyList()

            _uiState.update {
                it.copy(
                    messages = messages,
                    diffItems = diffs,
                    totalAdditions = diffs.sumOf { d -> d.additionsCount },
                    totalDeletions = diffs.sumOf { d -> d.deletionsCount },
                    totalModificationsCount = diffs.size,
                    totalFilesCount = diffs.map { d -> d.relativePath }.distinct().size,
                    chatError = null
                )
            }
        }
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
        preferences?.let { prefs ->
            viewModelScope.launch {
                prefs.setAiModel(model)
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(chatError = null) }
    }

    fun notifyCopied() {
        _uiState.update { it.copy(userNotice = "Code copié dans le presse-papiers.") }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(userNotice = null, reviewNotice = null) }
    }

    fun acceptAllDiffs() {
        viewModelScope.launch {
            acceptAllDiffsSync()
        }
    }

    suspend fun acceptAllDiffsSync(): Int {
        val state = _uiState.value
        val projectDir = state.projectLocalPath.takeIf { it.isNotBlank() }?.let(::File)
        val service = aiService
        var appliedCount = 0

        val updated = state.diffItems.map { item ->
            if (item.statusText == "Refusé") {
                item
            } else if (projectDir != null && service != null && item.proposedContent.isNotEmpty()) {
                val res = service.writeReviewedDiffToDisk(projectDir, item)
                if (res.isSuccess) {
                    appliedCount++
                    item.copy(statusText = "Appliqué", statusVariant = DiffStatusVariant.SUCCESS)
                } else {
                    item
                }
            } else {
                appliedCount++
                item.copy(statusText = "Appliqué", statusVariant = DiffStatusVariant.SUCCESS)
            }
        }

        val notice = if (projectDir == null) {
            "Aucun projet local actif pour écrire les fichiers."
        } else {
            "$appliedCount fichier(s) appliqué(s) au projet après revue."
        }

        _uiState.update { it.copy(diffItems = updated, reviewNotice = notice) }
        return appliedCount
    }

    fun rejectAllDiffs() {
        _uiState.update { state ->
            val updated = state.diffItems.map { it.copy(statusText = "Refusé", statusVariant = DiffStatusVariant.WARNING) }
            state.copy(
                diffItems = updated,
                reviewNotice = "Toutes les modifications proposées ont été refusées (aucun fichier modifié)."
            )
        }
    }

    fun acceptDiffItem(fileName: String) {
        viewModelScope.launch {
            acceptDiffItemSync(fileName)
        }
    }

    suspend fun acceptDiffItemSync(fileName: String): Boolean {
        val state = _uiState.value
        val target = state.diffItems.firstOrNull { it.fileName == fileName || it.relativePath == fileName } ?: return false
        val projectDir = state.projectLocalPath.takeIf { it.isNotBlank() }?.let(::File)
        val service = aiService

        if (projectDir != null && service != null && target.proposedContent.isNotEmpty()) {
            val writeRes = service.writeReviewedDiffToDisk(projectDir, target)
            if (writeRes.isFailure) {
                _uiState.update {
                    it.copy(reviewNotice = writeRes.exceptionOrNull()?.message ?: "Échec d'écriture de $fileName")
                }
                return false
            }
        }

        _uiState.update { current ->
            val updated = current.diffItems.map {
                if (it.fileName == fileName || it.relativePath == fileName) {
                    it.copy(statusText = "Appliqué", statusVariant = DiffStatusVariant.SUCCESS)
                } else it
            }
            current.copy(
                diffItems = updated,
                reviewNotice = "${target.fileName} appliqué au projet."
            )
        }
        return true
    }

    fun rejectDiffItem(fileName: String) {
        _uiState.update { state ->
            val updated = state.diffItems.map {
                if (it.fileName == fileName || it.relativePath == fileName) {
                    it.copy(statusText = "Refusé", statusVariant = DiffStatusVariant.WARNING)
                } else it
            }
            state.copy(
                diffItems = updated,
                reviewNotice = "$fileName refusé (aucune écriture sur disque)."
            )
        }
    }

    private suspend fun persistMessageSafe(
        projectId: Long?,
        role: String,
        content: String,
        attachedFiles: String,
        modelName: String,
        hasDiffReview: Boolean
    ) {
        val dao = conversationDao ?: return
        if (projectId == null || projectId <= 0L) return
        runCatching {
            dao.insertMessage(
                ConversationEntity(
                    projectId = projectId,
                    role = role,
                    content = content,
                    attachedFiles = attachedFiles,
                    modelName = modelName,
                    hasDiffReview = hasDiffReview
                )
            )
        }
    }

    private fun mapEntitiesToChatMessages(
        entities: List<ConversationEntity>,
        providerAuthorLabel: String
    ): List<ChatMessage> {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.FRANCE)
        return entities.map { entity ->
            val timeStr = timeFmt.format(Date(entity.timestamp))
            if (entity.role == "user") {
                ChatMessage(
                    id = "db_${entity.id}",
                    author = MessageAuthor.USER,
                    authorLabel = "VOUS",
                    timeString = timeStr,
                    text = entity.content
                )
            } else {
                val parsed = AiResponseParser.parseMarkdownResponse(entity.content)
                val advice = if (parsed.fileProposals.isNotEmpty()) {
                    "${parsed.fileProposals.size} fichier(s) concerné(s) : ${parsed.fileProposals.joinToString(", ") { it.filePath.substringAfterLast('/') }}. Passez en revue avant application."
                } else null
                ChatMessage(
                    id = "db_${entity.id}",
                    author = MessageAuthor.AI,
                    authorLabel = providerAuthorLabel,
                    timeString = timeStr,
                    text = parsed.proseText.ifBlank { entity.content },
                    codeBlocks = parsed.codeBlocks,
                    codeHeader = parsed.codeBlocks.firstOrNull()?.headerLabel,
                    codeContent = parsed.codeBlocks.firstOrNull()?.content,
                    showApplyActions = parsed.codeBlocks.isNotEmpty() || parsed.fileProposals.isNotEmpty(),
                    adviceText = advice
                )
            }
        }
    }

    private fun buildConversationItems(
        entities: List<ConversationEntity>,
        projectName: String
    ): List<ConversationItem> {
        if (entities.isEmpty()) return emptyList()
        val timeFmt = SimpleDateFormat("dd MMM · HH:mm", Locale.FRANCE)
        val firstUser = entities.firstOrNull { it.role == "user" }
        val lastTimestamp = entities.last().timestamp
        val diffCount = entities.count { it.hasDiffReview }
        val title = firstUser?.content?.lines()?.firstOrNull()?.take(48)?.ifBlank { "Conversation IA" }
            ?: "Conversation IA"
        val subtitle = "${timeFmt.format(Date(lastTimestamp))} · ${entities.size} messages" +
            if (projectName.isNotBlank()) " · $projectName" else ""

        return listOf(
            ConversationItem(
                id = "project_thread_${entities.first().projectId}",
                title = title,
                subtitle = subtitle,
                chipText = if (diffCount > 0) "$diffCount modifs" else "Actif",
                chipVariant = if (diffCount > 0) ConversationChipVariant.ACCENT else ConversationChipVariant.SUCCESS
            )
        )
    }

    private fun buildAdviceSummary(diffs: List<DiffItem>): String? {
        if (diffs.isEmpty()) return null
        val names = diffs.joinToString(", ") { diff ->
            val tag = if (diff.isNewFile) "nouveau" else "+${diff.additionsCount} −${diff.deletionsCount}"
            "${diff.fileName} ($tag)"
        }
        return "${diffs.size} fichier(s) concerné(s) : $names. Passez en revue avant application."
    }

    private fun formatCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date())
    }
}
