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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
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
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
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

    Column(
        modifier = modifier
            .testTag("screen_s15")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // TopBar S15 : Chat IA / claude-sonnet-4.5 (mono 10.5px) + Sliders + Clock
        NexcodeTopBar(
            title = stringResource(id = R.string.s15_title),
            subtitle = uiState.modelName,
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack,
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
                .padding(horizontal = 16.dp, vertical = 2.dp)
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
                    fontSize = 11.5.sp,
                    color = colors.txt2,
                    modifier = Modifier.weight(1f)
                )

                NexcodeButton(
                    text = stringResource(id = R.string.s15_btn_add_files),
                    icon = painterResource(id = R.drawable.ic_plus),
                    variant = NexcodeButtonVariant.GHOST,
                    size = NexcodeButtonSize.SMALL,
                    onClick = { /* Ajouter des fichiers au contexte */ },
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
                        testTag = "s15_chip_context_$file"
                    )
                }
            }
        }

        // Zone de défilement des messages (pad: padding-top: 14px)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // État par défaut (stv: def)
            if (uiState.chatState == ChatScreenState.DEFAULT) {
                item {
                    // Message Utilisateur 1
                    UserChatBubble(
                        header = stringResource(id = R.string.s15_user_header_format, "14:02"),
                        message = "Ajoute un écran de détails météo qui affiche l'humidité et le vent de la ville sélectionnée."
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Message Assistant 1 avec code
                    AssistantCodeBubble(
                        header = stringResource(id = R.string.s15_ai_header_format, "14:02"),
                        introText = "Voici un composable WeatherDetails qui réutilise votre thème et vos chaînes existantes :",
                        codeHeader = "WeatherDetails.kt · nouveau fichier",
                        onApplyClick = onNavigateReview
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Message Assistant 2 (Avis de revue)
                    AssistantAdviceBubble(
                        adviceText = stringResource(id = R.string.s15_review_advice),
                        onReviewClick = onNavigateReview
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // État en cours (stv: cours)
                item {
                    // Message Utilisateur 2
                    UserChatBubble(
                        header = stringResource(id = R.string.s15_user_header_format, "14:05"),
                        message = uiState.analyzingUserPrompt
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bulle d'analyse en cours avec 3 points clignotants
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TypingIndicatorDots()
                        Text(
                            text = stringResource(
                                id = R.string.s15_analyzing_format,
                                uiState.analyzingFile,
                                uiState.analyzingLinesCount
                            ),
                            fontSize = 12.sp,
                            color = colors.txt2
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Carte d'attente / interruption avec spinner et bouton Stop
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeRadius14)
                            .background(colors.surface)
                            .border(1.dp, colors.border, ShapeRadius14)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            fontSize = 12.sp,
                            color = colors.txt2,
                            modifier = Modifier.weight(1f)
                        )

                        NexcodeButton(
                            text = stringResource(id = R.string.s15_btn_stop),
                            icon = painterResource(id = R.drawable.ic_stop),
                            variant = NexcodeButtonVariant.DANGER,
                            size = NexcodeButtonSize.SMALL,
                            onClick = { viewModel.stopAnalysis() },
                            modifier = Modifier.height(30.dp),
                            testTag = "s15_btn_stop"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Barre de saisie inférieure (attachement, input, bouton envoyer rond en accent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(width = 0.5.dp, color = colors.border)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_clip),
                contentDescription = "Attacher un fichier",
                onClick = { /* Joindre fichier */ },
                modifier = Modifier.size(38.dp),
                testTag = "s15_btn_clip"
            )

            NexcodeTextField(
                value = uiState.inputMessage,
                onValueChange = { viewModel.updateInputMessage(it) },
                placeholder = stringResource(id = R.string.s15_input_placeholder),
                singleLine = true,
                modifier = Modifier.weight(1f),
                testTag = "s15_input_message"
            )

            // Bouton d'envoi rond en accent (#3DDCFF, fond accent, couleur onAccent)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable { viewModel.sendMessage() },
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

    // Modal / BottomSheet de choix du modèle IA
    if (uiState.showModelSelector) {
        NexcodeBottomSheet(
            title = "Choisir le modèle IA",
            subtitle = "Sélectionnez le modèle pour le chat et la génération",
            onDismissRequest = { viewModel.closeModelSelector() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                listOf(
                    "claude-sonnet-4.5" to "Anthropic · Recommandé pour Kotlin",
                    "claude-opus-4.5" to "Anthropic · Raisonnement approfondi",
                    "gpt-4o" to "OpenAI · Multimodal",
                    "ollama / custom" to "Serveur personnalisé local"
                ).forEach { (model, desc) ->
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
                            Text(text = model, fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) colors.accent else colors.txt)
                            Text(text = desc, fontSize = 11.5.sp, color = colors.txt2)
                        }
                        if (isSelected) {
                            Icon(painter = painterResource(id = R.drawable.ic_check), contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserChatBubble(header: String, message: String) {
    val colors = NexcodeTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = header,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.txt2,
            modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(colors.accent2.copy(alpha = 0.15f))
                .border(1.dp, colors.accent2.copy(alpha = 0.35f), RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = message,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = colors.txt
            )
        }
    }
}

@Composable
private fun AssistantCodeBubble(
    header: String,
    introText: String,
    codeHeader: String,
    onApplyClick: () -> Unit
) {
    val colors = NexcodeTheme.colors
    val codeColors = colors.codeColors

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = header,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.txt2,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(14.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Voici un composable ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("WeatherDetails")
                    }
                    append(" qui réutilise votre thème et vos chaînes existantes :")
                },
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.txt
            )

            // Conteneur de code inséré
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(ShapeRadius12)
                    .background(colors.codeBg)
                    .border(1.dp, colors.border, ShapeRadius12)
            ) {
                // Header du code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface2)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = codeHeader,
                        fontSize = 11.sp,
                        color = colors.txt2,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                // Lignes de code avec coloration
                val lines = listOf(
                    1 to "@Composable",
                    2 to "fun WeatherDetails(city: City) {",
                    3 to "    Column {",
                    4 to "        DetailRow(",
                    5 to "            label = stringResource(R.string.unit_humidity),",
                    6 to "            value = \"\${city.humidity} %\")",
                    7 to "        )",
                    8 to "        DetailRow(",
                    9 to "            label = stringResource(R.string.unit_wind),",
                    10 to "            value = \"\${city.windKmh} km/h\")",
                    11 to "        )",
                    12 to "    }",
                    13 to "}"
                )

                Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)) {
                    lines.forEach { (num, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$num",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = colors.txt2.copy(alpha = 0.5f),
                                modifier = Modifier.width(22.dp)
                            )
                            Text(
                                text = text,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                color = if (text.startsWith("@")) codeColors.annotation else if (text.contains("fun")) colors.txt else colors.txt2
                            )
                        }
                    }
                }
            }

            // Boutons d'action : Appliquer au projet / Copier
            Row(
                modifier = Modifier.padding(top = 10.dp),
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
private fun AssistantAdviceBubble(adviceText: String, onReviewClick: () -> Unit) {
    val colors = NexcodeTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .clickable(onClick = onReviewClick)
            .padding(14.dp)
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
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = colors.txt
        )
    }
}

@Composable
private fun TypingIndicatorDots() {
    val colors = NexcodeTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.7f)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.4f)))
    }
}
