package com.nexcode.app.ui.screens.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.data.ai.ParsedCodeBlock
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeBottomSheet
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.components.modals.ModalModelSelection
import com.nexcode.app.ui.theme.CodeColors
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12
import com.nexcode.app.ui.theme.ShapeRadius14

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScreenS15AiChat(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateReview: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateAiConfig: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val lastMsgLength = uiState.messages.lastOrNull()?.let { it.text.length + (it.codeContent?.length ?: 0) } ?: 0
    LaunchedEffect(uiState.messages.size, lastMsgLength, uiState.chatState) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem((uiState.messages.size - 1).coerceAtLeast(0))
        }
    }

    if (uiState.showModelSelector) {
        ModalModelSelection(
            selectedModel = uiState.modelName,
            onModelSelected = { viewModel.selectModel(it) },
            onDismiss = { viewModel.closeModelSelector() }
        )
    }

    if (uiState.showFilePickerSheet) {
        ProjectContextPickerSheet(
            availableFiles = uiState.availableProjectFiles,
            selectedFiles = uiState.contextFiles,
            onToggleFile = { viewModel.toggleContextFile(it) },
            onDismiss = { viewModel.closeFilePicker() }
        )
    }

    Column(
        modifier = modifier
            .testTag("screen_s15")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        NexcodeTopBar(
            title = stringResource(id = R.string.s15_title),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack,
            titleContent = {
                Text(
                    text = stringResource(id = R.string.s15_title),
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.01).sp,
                    color = colors.txt,
                    maxLines = 1
                )
                Text(
                    text = "${uiState.modelName} · ${uiState.providerName}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.5.sp,
                    color = colors.txt2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            },
            actions = {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_sliders),
                    contentDescription = "Changer de modèle",
                    onClick = { viewModel.openModelSelector() },
                    testTag = "s15_btn_model"
                )
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_clock),
                    contentDescription = "Historique",
                    onClick = onNavigateHistory,
                    testTag = "s15_btn_history"
                )
            }
        )

        // Carte Contexte de la conversation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp)
                .clip(ShapeRadius14)
                .background(colors.surface)
                .border(1.dp, colors.border, ShapeRadius14)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.s15_context_label),
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp,
                    color = colors.txt2,
                    modifier = Modifier.weight(1f)
                )

                NexcodeButton(
                    text = stringResource(id = R.string.s15_btn_add_files),
                    icon = painterResource(id = R.drawable.ic_plus),
                    variant = NexcodeButtonVariant.GHOST,
                    size = NexcodeButtonSize.SMALL,
                    onClick = { viewModel.openFilePicker() },
                    testTag = "s15_btn_add_files"
                )
            }

            val chipsScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipsScroll)
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.contextFiles.isEmpty()) {
                    Text(
                        text = if (uiState.projectLocalPath.isBlank()) {
                            "Aucun projet actif sélectionné"
                        } else {
                            "Aucun fichier attaché · touchez + Fichiers"
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = colors.txt2
                    )
                } else {
                    uiState.contextFiles.forEach { file ->
                        NexcodeChip(
                            text = file.substringAfterLast('/'),
                            variant = NexcodeChipVariant.ACCENT,
                            fontFamily = JetBrainsMonoFontFamily,
                            onClick = { viewModel.toggleContextFile(file) },
                            testTag = "s15_chip_context_$file"
                        )
                    }
                }
            }
        }

        // Zone de défilement des messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }

            uiState.userNotice?.let { notice ->
                item {
                    NexcodeBanner(
                        text = notice,
                        icon = painterResource(id = R.drawable.ic_check_c),
                        variant = NexcodeBannerVariant.SUCCESS,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { viewModel.dismissNotice() }
                            .testTag("s15_notice_banner")
                    )
                }
            }

            uiState.chatError?.let { errorMsg ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("s15_error_container"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NexcodeBanner(
                            text = errorMsg,
                            icon = painterResource(id = R.drawable.ic_alert),
                            variant = NexcodeBannerVariant.ERROR,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("s15_error_banner")
                        )
                        if (uiState.isMissingApiKey) {
                            NexcodeButton(
                                text = "Configurer la clé IA",
                                icon = painterResource(id = R.drawable.ic_sliders),
                                variant = NexcodeButtonVariant.SECONDARY,
                                size = NexcodeButtonSize.SMALL,
                                onClick = onNavigateAiConfig,
                                testTag = "s15_btn_configure_ai"
                            )
                        }
                    }
                }
            }

            if (uiState.messages.isEmpty() && uiState.chatState == ChatScreenState.DEFAULT) {
                item {
                    EmptyChatIntroCard(
                        projectName = uiState.projectName,
                        providerName = uiState.providerName,
                        modelName = uiState.modelName,
                        hasConfiguredAi = uiState.hasConfiguredAi,
                        diffCount = uiState.diffItems.size,
                        onNavigateAiConfig = onNavigateAiConfig,
                        onNavigateReview = onNavigateReview
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { msg ->
                    if (msg.author == MessageAuthor.USER) {
                        UserMessageBubble(
                            who = "${msg.authorLabel} · ${msg.timeString}",
                            message = msg.text
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    } else {
                        val hasContent = msg.text.isNotBlank() || msg.codeBlocks.isNotEmpty()
                        if (hasContent) {
                            AssistantMessageWithCode(
                                who = "${msg.authorLabel} · ${msg.timeString}",
                                message = msg,
                                onApplyClick = {
                                    viewModel.prepareReviewFromMessage(msg)
                                    onNavigateReview()
                                },
                                onCopyClick = {
                                    val toCopy = if (msg.codeBlocks.isNotEmpty()) {
                                        msg.codeBlocks.joinToString("\n\n") { it.content }
                                    } else {
                                        msg.text
                                    }
                                    clipboardManager.setText(AnnotatedString(toCopy))
                                    viewModel.notifyCopied()
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (!msg.adviceText.isNullOrBlank() && !msg.isStreaming) {
                                AssistantAdviceBubble(
                                    adviceText = msg.adviceText,
                                    onReviewClick = {
                                        viewModel.prepareReviewFromMessage(msg)
                                        onNavigateReview()
                                    }
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            if (uiState.chatState == ChatScreenState.ANALYZING) {
                item {
                    AssistantTypingBubble(
                        fileName = uiState.analyzingFile.ifBlank { "projet" },
                        linesCount = uiState.analyzingLinesCount
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    WaitingProgressCard(
                        onStopClick = { viewModel.stopAnalysis() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Barre inférieure de saisie
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(width = 0.5.dp, color = colors.border)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.openFilePicker() }
                    .testTag("s15_btn_attach"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clip),
                    contentDescription = "Pièce jointe",
                    modifier = Modifier.size(20.dp),
                    tint = colors.txt2
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (uiState.inputMessage.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.s15_input_placeholder),
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.txt2
                    )
                }

                BasicTextField(
                    value = uiState.inputMessage,
                    onValueChange = { viewModel.updateInputMessage(it) },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.txt
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("s15_input_message")
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.inputMessage.isNotBlank() && uiState.chatState == ChatScreenState.DEFAULT) {
                            colors.accent
                        } else {
                            colors.accent.copy(alpha = 0.5f)
                        }
                    )
                    .clickable(enabled = uiState.inputMessage.isNotBlank()) { viewModel.sendMessage() }
                    .testTag("s15_btn_send"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = "Envoyer",
                    modifier = Modifier.size(19.dp),
                    tint = colors.onAccent
                )
            }
        }
    }
}

@Composable
private fun EmptyChatIntroCard(
    projectName: String,
    providerName: String,
    modelName: String,
    hasConfiguredAi: Boolean,
    diffCount: Int,
    onNavigateAiConfig: () -> Unit,
    onNavigateReview: () -> Unit
) {
    val colors = NexcodeTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeRadius14)
            .background(colors.surface)
            .border(1.dp, colors.border, ShapeRadius14)
            .padding(14.dp)
            .testTag("s15_empty_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accent2.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_spark),
                    contentDescription = null,
                    tint = colors.accent2,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (projectName.isNotBlank()) "Assistant · $projectName" else "Assistant IA NEXCODE",
                    fontFamily = InterFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt
                )
                Text(
                    text = "$providerName · $modelName",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = colors.txt2
                )
            }
        }

        Text(
            text = "Décrivez une fonctionnalité ou une correction. Les blocs de code générés sont transformés en diff et ne modifient jamais vos fichiers sans revue préalable.",
            fontFamily = InterFontFamily,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = colors.txt2
        )

        if (!hasConfiguredAi) {
            NexcodeBanner(
                text = "Aucune clé API détectée pour $providerName. Configurez votre clé avant d'envoyer un message.",
                icon = painterResource(id = R.drawable.ic_alert),
                variant = NexcodeBannerVariant.WARNING,
                modifier = Modifier.fillMaxWidth()
            )
            NexcodeButton(
                text = "Configurer l'IA",
                icon = painterResource(id = R.drawable.ic_sliders),
                variant = NexcodeButtonVariant.PRIMARY,
                size = NexcodeButtonSize.SMALL,
                onClick = onNavigateAiConfig,
                testTag = "s15_btn_setup_ai"
            )
        }

        if (diffCount > 0) {
            NexcodeButton(
                text = "Revoir $diffCount modification(s) en attente",
                icon = painterResource(id = R.drawable.ic_check),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.SMALL,
                onClick = onNavigateReview,
                testTag = "s15_btn_open_pending_review"
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProjectContextPickerSheet(
    availableFiles: List<String>,
    selectedFiles: List<String>,
    onToggleFile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NexcodeTheme.colors

    NexcodeBottomSheet(
        title = "Fichiers du projet",
        subtitle = "Sélectionnez jusqu'à 8 fichiers à inclure dans le prompt",
        onDismissRequest = onDismiss,
        testTag = "s15_sheet_file_picker"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            if (availableFiles.isEmpty()) {
                Text(
                    text = "Aucun fichier source détecté dans le projet actif.",
                    fontFamily = InterFontFamily,
                    fontSize = 12.5.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableFiles.forEach { relPath ->
                        val isSelected = relPath in selectedFiles
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(ShapeRadius12)
                                .background(
                                    if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.surface2
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colors.accent else colors.border,
                                    shape = ShapeRadius12
                                )
                                .clickable { onToggleFile(relPath) }
                                .padding(horizontal = 12.dp, vertical = 11.dp)
                                .testTag("s15_picker_file_$relPath"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = relPath,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                color = if (isSelected) colors.accent else colors.txt,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "Sélectionné",
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NexcodeButton(
                text = "Terminer (${selectedFiles.size})",
                variant = NexcodeButtonVariant.PRIMARY,
                size = NexcodeButtonSize.DEFAULT,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "s15_picker_btn_done"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UserMessageBubble(
    who: String,
    message: String
) {
    val colors = NexcodeTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = who,
            fontFamily = InterFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = colors.txt2,
            modifier = Modifier.padding(bottom = 4.dp, end = 2.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 6.dp))
                .background(colors.accent)
                .padding(horizontal = 13.dp, vertical = 11.dp)
        ) {
            Text(
                text = message,
                fontFamily = InterFontFamily,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = colors.onAccent
            )
        }
    }
}

@Composable
private fun AssistantMessageWithCode(
    who: String,
    message: ChatMessage,
    onApplyClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val colors = NexcodeTheme.colors
    val codeColors = colors.codeColors

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = who,
            fontFamily = InterFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = colors.txt2,
            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp))
                .padding(13.dp)
        ) {
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    fontFamily = InterFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = colors.txt
                )
            }

            message.codeBlocks.forEachIndexed { index, block ->
                CodeBlockCard(
                    block = block,
                    codeColors = codeColors,
                    modifier = Modifier.padding(top = if (index == 0 && message.text.isBlank()) 0.dp else 10.dp)
                )
            }

            if (message.showApplyActions && !message.isStreaming) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NexcodeButton(
                        text = stringResource(id = R.string.s15_btn_apply_project),
                        icon = painterResource(id = R.drawable.ic_check),
                        variant = NexcodeButtonVariant.PRIMARY,
                        size = NexcodeButtonSize.SMALL,
                        onClick = onApplyClick,
                        testTag = "s15_btn_apply"
                    )

                    NexcodeButton(
                        text = stringResource(id = R.string.s15_btn_copy),
                        icon = painterResource(id = R.drawable.ic_copy),
                        variant = NexcodeButtonVariant.SECONDARY,
                        size = NexcodeButtonSize.SMALL,
                        onClick = onCopyClick,
                        testTag = "s15_btn_copy"
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    block: ParsedCodeBlock,
    codeColors: CodeColors,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(ShapeRadius12)
                .background(colors.codeBg)
                .border(1.dp, colors.border, ShapeRadius12)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface2)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.headerLabel,
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.txt2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = colors.border)

            val scrollState = rememberScrollState()
            val lines = block.content.lines()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 10.dp)
            ) {
                lines.forEachIndexed { idx, rawLine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            color = colors.txt2.copy(alpha = 0.55f),
                            modifier = Modifier
                                .width(38.dp)
                                .padding(end = 12.dp),
                            textAlign = TextAlign.End
                        )

                        Text(
                            text = highlightCodeLine(rawLine, codeColors),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            lineHeight = 18.sp,
                            color = colors.txt
                        )
                    }
                }
            }
        }
    }
}

private val KOTLIN_KEYWORDS = setOf(
    "package", "import", "class", "interface", "object", "enum", "sealed", "data",
    "fun", "val", "var", "if", "else", "when", "for", "while", "return", "true", "false",
    "null", "private", "public", "internal", "override", "suspend", "inline", "companion"
)

private val TOKEN_REGEX = Regex(
    "(//.*$)|(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")|(@[A-Za-z_][A-Za-z0-9_]*)|(\\b\\d+(?:\\.\\d+)?[fFL]?\\b)|(\\b[A-Za-z_][A-Za-z0-9_]*\\b)"
)

private fun highlightCodeLine(line: String, codeColors: CodeColors): AnnotatedString {
    if (line.isBlank()) return AnnotatedString("")
    return buildAnnotatedString {
        var cursor = 0
        TOKEN_REGEX.findAll(line).forEach { match ->
            if (match.range.first > cursor) {
                append(line.substring(cursor, match.range.first))
            }
            val token = match.value
            val color: Color? = when {
                match.groups[1] != null -> codeColors.comment
                match.groups[2] != null -> codeColors.string
                match.groups[3] != null -> codeColors.annotation
                match.groups[4] != null -> codeColors.number
                token in KOTLIN_KEYWORDS -> codeColors.keyword
                match.range.last + 1 < line.length && line[match.range.last + 1] == '(' -> codeColors.function
                token.firstOrNull()?.isUpperCase() == true -> codeColors.type
                else -> null
            }
            if (color != null) {
                withStyle(SpanStyle(color = color)) {
                    append(token)
                }
            } else {
                append(token)
            }
            cursor = match.range.last + 1
        }
        if (cursor < line.length) {
            append(line.substring(cursor))
        }
    }
}

@Composable
private fun AssistantAdviceBubble(
    adviceText: String,
    onReviewClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp))
            .clickable(onClick = onReviewClick)
            .padding(13.dp)
            .testTag("s15_advice_bubble")
    ) {
        Text(
            text = adviceText,
            fontFamily = InterFontFamily,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = colors.txt
        )
    }
}

@Composable
private fun AssistantTypingBubble(
    fileName: String,
    linesCount: Int
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TypingIndicatorDots()

        Text(
            text = stringResource(id = R.string.s15_analyzing_format, fileName, linesCount),
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = colors.txt2
        )
    }
}

@Composable
private fun WaitingProgressCard(
    onStopClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeRadius14)
            .background(colors.surface)
            .border(1.dp, colors.border, ShapeRadius14)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 1.5.dp,
            color = colors.accent
        )

        Text(
            text = stringResource(id = R.string.s15_wait_card_hint),
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = colors.txt2,
            modifier = Modifier.weight(1f)
        )

        NexcodeButton(
            text = stringResource(id = R.string.s15_btn_stop),
            icon = painterResource(id = R.drawable.ic_stop),
            variant = NexcodeButtonVariant.DANGER,
            size = NexcodeButtonSize.SMALL,
            onClick = onStopClick,
            testTag = "s15_btn_stop"
        )
    }
}

@Composable
private fun TypingIndicatorDots() {
    val colors = NexcodeTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 180, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.txt2.copy(alpha = dot1Alpha))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.txt2.copy(alpha = dot2Alpha))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.txt2.copy(alpha = dot3Alpha))
        )
    }
}
