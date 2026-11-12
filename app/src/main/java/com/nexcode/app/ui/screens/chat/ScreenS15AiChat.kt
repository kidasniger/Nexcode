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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBottomSheet
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTopBar
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val codeColors = colors.codeColors

    // Feuille modale basse pour le choix du modèle (.sheet m-model)
    if (uiState.showModelSelector) {
        com.nexcode.app.ui.components.modals.ModalModelSelection(
            selectedModel = uiState.modelName,
            onModelSelected = { viewModel.selectModel(it) },
            onDismiss = { viewModel.closeModelSelector() }
        )
    }

    Column(
        modifier = modifier
            .testTag("screen_s15")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // En-tete : TopBar avec Chat IA et sous-titre mono claude-sonnet-4.5
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
                    text = uiState.modelName,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.5.sp,
                    color = colors.txt2,
                    maxLines = 1,
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

        // Carte Contexte de la conversation (margin: 0 16px 4px)
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
                    onClick = { /* Ajouter des fichiers */ },
                    modifier = Modifier.height(28.dp),
                    testTag = "s15_btn_add_files"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uiState.contextFiles.forEach { file ->
                    NexcodeChip(
                        text = file,
                        variant = NexcodeChipVariant.ACCENT,
                        fontFamily = JetBrainsMonoFontFamily,
                        testTag = "s15_chip_context_$file"
                    )
                }
            }
        }

        // Zone de defilement des messages (pad : padding-top: 14px)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Etat par defaut : stv="def"
            if (uiState.chatState == ChatScreenState.DEFAULT) {
                item {
                    // Message Utilisateur : VOUS · 14:02
                    UserMessageBubble(
                        who = stringResource(id = R.string.s15_user_header_format, "14:02"),
                        message = "Ajoute un écran de détails météo qui affiche l'humidité et le vent de la ville sélectionnée."
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Message Assistant : CLAUDE · 14:02 avec code WeatherDetails
                    AssistantMessageWithCode(
                        who = stringResource(id = R.string.s15_ai_header_format, "14:02"),
                        onApplyClick = onNavigateReview
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Message Assistant : Avis de revue des 3 fichiers concernes
                    AssistantAdviceBubble(
                        onReviewClick = onNavigateReview
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // Etat en cours : stv="cours"
                item {
                    // Message Utilisateur : VOUS · 14:05
                    UserMessageBubble(
                        who = stringResource(id = R.string.s15_user_header_format, "14:05"),
                        message = uiState.analyzingUserPrompt
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Message Assistant avec indicateur de frappe
                    AssistantTypingBubble(
                        fileName = uiState.analyzingFile,
                        linesCount = uiState.analyzingLinesCount
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Carte d'attente / interruption avec spinner et bouton Stop
                    WaitingProgressCard(
                        onStopClick = { viewModel.stopAnalysis() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Barre inferieure de saisie (position: absolute bottom 82px dans la maquette)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(width = 0.5.dp, color = colors.border)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bouton trompette / trombone (ic_clip 38x38)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { /* Piece jointe */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clip),
                    contentDescription = "Pièce jointe",
                    modifier = Modifier.size(20.dp),
                    tint = colors.txt2
                )
            }

            // Champ de saisie (height: 42px, font-size: 13px)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
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
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("s15_input_message")
                )
            }

            // Bouton Envoyer rond accent (#3DDCFF, taille 42x42)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable { viewModel.sendMessage() }
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

    // Feuille modale du choix du modele IA (m-model)
    if (uiState.showModelSelector) {
        NexcodeBottomSheet(
            title = stringResource(id = R.string.s04_choose_model_title),
            subtitle = stringResource(id = R.string.s04_choose_model_sub),
            onDismissRequest = { viewModel.closeModelSelector() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val models = listOf(
                    "claude-sonnet-4.5" to "Anthropic · Recommandé pour le code Kotlin",
                    "claude-opus-4.5" to "Anthropic · Raisonnement approfondi",
                    "gpt-4o" to "OpenAI · Multimodal",
                    "ollama / custom" to "Serveur personnalisé local"
                )

                models.forEach { (model, desc) ->
                    val isSelected = uiState.modelName == model
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeRadius12)
                            .background(if (isSelected) colors.accent.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { viewModel.selectModel(model) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.accent else colors.txt
                            )
                            Text(
                                text = desc,
                                fontFamily = InterFontFamily,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
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
    onApplyClick: () -> Unit
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
            Text(
                text = buildAnnotatedString {
                    append("Voici un composable ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("WeatherDetails")
                    }
                    append(" qui réutilise votre thème et vos chaînes existantes :")
                },
                fontFamily = InterFontFamily,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = colors.txt
            )

            // Bloc de code (.code)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(ShapeRadius12)
                    .background(colors.codeBg)
                    .border(1.dp, colors.border, ShapeRadius12)
            ) {
                // En-tete du code (.code-h)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface2)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WeatherDetails.kt · nouveau fichier",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.txt2,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                // Lignes de code avec numeros et coloration syntaxique
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(vertical = 10.dp)
                ) {
                    val codeLines = listOf(
                        1 to buildAnnotatedString {
                            withStyle(SpanStyle(color = codeColors.annotation)) { append("@Composable") }
                        },
                        2 to buildAnnotatedString {
                            withStyle(SpanStyle(color = codeColors.keyword)) { append("fun") }
                            append(" ")
                            withStyle(SpanStyle(color = codeColors.function)) { append("WeatherDetails") }
                            append("(city: ")
                            withStyle(SpanStyle(color = codeColors.type)) { append("City") }
                            append(") {")
                        },
                        3 to buildAnnotatedString {
                            append("    ")
                            withStyle(SpanStyle(color = codeColors.type)) { append("Column") }
                            append(" {")
                        },
                        4 to buildAnnotatedString {
                            append("        ")
                            withStyle(SpanStyle(color = codeColors.function)) { append("DetailRow") }
                            append("(")
                        },
                        5 to buildAnnotatedString {
                            append("            label = ")
                            withStyle(SpanStyle(color = codeColors.function)) { append("stringResource") }
                            append("(")
                            withStyle(SpanStyle(color = codeColors.type)) { append("R") }
                            append(".string.unit_humidity),")
                        },
                        6 to buildAnnotatedString {
                            append("            value = ")
                            withStyle(SpanStyle(color = codeColors.string)) { append("\"${'$'}{city.humidity} %\"") }
                            append(")")
                        },
                        7 to buildAnnotatedString {
                            append("        )")
                        },
                        8 to buildAnnotatedString {
                            append("        ")
                            withStyle(SpanStyle(color = codeColors.function)) { append("DetailRow") }
                            append("(")
                        },
                        9 to buildAnnotatedString {
                            append("            label = ")
                            withStyle(SpanStyle(color = codeColors.function)) { append("stringResource") }
                            append("(")
                            withStyle(SpanStyle(color = codeColors.type)) { append("R") }
                            append(".string.unit_wind),")
                        },
                        10 to buildAnnotatedString {
                            append("            value = ")
                            withStyle(SpanStyle(color = codeColors.string)) { append("\"${'$'}{city.windKmh} km/h\"") }
                            append(")")
                        },
                        11 to buildAnnotatedString {
                            append("        )")
                        },
                        12 to buildAnnotatedString {
                            append("    }")
                        },
                        13 to buildAnnotatedString {
                            append("}")
                        }
                    )

                    codeLines.forEach { (lineNum, annotatedLine) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$lineNum",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                color = colors.txt2.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .width(38.dp)
                                    .padding(end = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Right
                            )

                            Text(
                                text = annotatedLine,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                lineHeight = 18.sp,
                                color = colors.txt
                            )
                        }
                    }
                }
            }

            // Boutons d'action : Appliquer au projet / Copier
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
                    onClick = { /* Copier */ },
                    testTag = "s15_btn_copy"
                )
            }
        }
    }
}

@Composable
private fun AssistantAdviceBubble(
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
    ) {
        val annotated = buildAnnotatedString {
            append("3 fichiers concernés : ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("WeatherDetails.kt") }
            append(" (nouveau), ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("strings.xml") }
            append(" (+2 chaînes), ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("City.kt") }
            append(" (+1 champ). Passez en revue avant application.")
        }

        Text(
            text = annotated,
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
            modifier = Modifier.height(30.dp),
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
