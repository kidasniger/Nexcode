package com.nexcode.app.ui.screens.editor

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.theme.DarkCodeColors
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS11CodeEditor(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateDependencies: () -> Unit,
    onNavigateBuild: () -> Unit,
    onNavigateAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val codeColors = colors.codeColors

    Column(
        modifier = modifier
            .testTag("screen_s11")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // TopBar S11 personnalisée (height: 50dp, border-bottom: 1px solid var(--border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(colors.surface)
                .border(width = 0.5.dp, color = colors.border)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_back),
                contentDescription = "Retour",
                onClick = onNavigateBack,
                testTag = "s11_btn_back",
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = uiState.activeTab,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.warn)
                )
            }

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_search),
                contentDescription = "Rechercher",
                onClick = onNavigateSearch,
                testTag = "s11_btn_search",
                modifier = Modifier.size(34.dp)
            )

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_sliders),
                contentDescription = "Ressources / Réglages",
                onClick = onNavigateSettings,
                testTag = "s11_btn_sliders",
                modifier = Modifier.size(34.dp)
            )
        }

        // Barre d'onglets (tabbar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(colors.bg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            uiState.openTabs.forEach { tabName ->
                val isActive = tabName == uiState.activeTab
                EditorTabItem(
                    title = tabName,
                    isActive = isActive,
                    hasDot = tabName == "MainActivity.kt",
                    onClick = { viewModel.openTab(tabName) }
                )
            }

            // Bouton + d'onglet
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* Nouvel onglet */ }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = colors.txt2
                )
            }
        }

        // Zone de contenu défilable (Éditeur + Carte d'erreur)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            // Carte de code (Sora Editor / Jetpack Compose representation)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(ShapeRadius12)
                    .background(colors.codeBg)
                    .border(1.dp, colors.border, ShapeRadius12)
            ) {
                // Header de la boîte de code : 3 petits points + nom du fichier + puce d'erreur
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface2)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF5C7A)))
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFFB020)))
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF2ECC71)))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = uiState.codeHeaderTitle,
                        fontSize = 11.sp,
                        color = colors.txt2,
                        modifier = Modifier.weight(1f)
                    )

                    if (uiState.errorCount > 0) {
                        NexcodeChip(
                            text = stringResource(id = R.string.s11_error_count_format, uiState.errorCount),
                            variant = NexcodeChipVariant.ERROR,
                            testTag = "s11_chip_error"
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                // Lignes de code avec coloration syntaxique complète
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    uiState.codeLines.forEach { line ->
                        CodeLineRow(line = line, codeColors = codeColors)
                    }
                }
            }

            // Carte d'erreur en bas du code (si erreur active)
            if (uiState.errorCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.err.copy(alpha = 0.35f), ShapeRadius12)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_alert),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        tint = colors.err
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.errorTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.err
                        )
                        Text(
                            text = uiState.errorDesc,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 3.dp)
                        )

                        Row(
                            modifier = Modifier.padding(top = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NexcodeButton(
                                text = stringResource(id = R.string.s11_btn_correct_occurrences),
                                variant = NexcodeButtonVariant.SECONDARY,
                                size = NexcodeButtonSize.SMALL,
                                onClick = onNavigateSearch,
                                testTag = "s11_btn_correct"
                            )

                            NexcodeButton(
                                text = stringResource(id = R.string.s11_btn_ai),
                                icon = painterResource(id = R.drawable.ic_spark),
                                variant = NexcodeButtonVariant.GHOST,
                                size = NexcodeButtonSize.SMALL,
                                onClick = onNavigateAiChat,
                                testTag = "s11_btn_ai_correct"
                            )
                        }
                    }
                }
            }
        }

        // Barre de symboles (tab, { }, ( ), < >, ", $, =, :, ;, _)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(width = 0.5.dp, color = colors.border)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val symbols = listOf("⇥", "{ }", "( )", "< >", "\"", "$", "=", ":", ";", "_")
            symbols.forEach { sym ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                        .clickable { viewModel.insertSymbol(sym) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sym,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.txt
                    )
                }
            }
        }

        // Barre d'outils inférieure de l'éditeur (undo, redo, box/gradle, play/build, spark/ia)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(colors.surface)
                .border(width = 0.5.dp, color = colors.border)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_undo),
                contentDescription = "Annuler",
                onClick = { /* Undo */ },
                testTag = "s11_btn_undo",
                modifier = Modifier.size(38.dp)
            )

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_redo),
                contentDescription = "Rétablir",
                onClick = { /* Redo */ },
                testTag = "s11_btn_redo",
                modifier = Modifier.size(38.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_box),
                contentDescription = "Dépendances Gradle",
                onClick = onNavigateDependencies,
                testTag = "s11_btn_gradle",
                modifier = Modifier.size(38.dp)
            )

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_play),
                contentDescription = "Compiler & Exécuter",
                onClick = onNavigateBuild,
                testTag = "s11_btn_run",
                modifier = Modifier.size(38.dp)
            )

            // Bouton IA accentué
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accent.copy(alpha = 0.15f))
                    .clickable(onClick = onNavigateAiChat),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_spark),
                    contentDescription = "Chat IA",
                    modifier = Modifier.size(18.dp),
                    tint = colors.accent
                )
            }
        }
    }
}

@Composable
private fun EditorTabItem(
    title: String,
    isActive: Boolean,
    hasDot: Boolean,
    onClick: () -> Unit
) {
    val colors = NexcodeTheme.colors
    val bg = if (isActive) colors.surface else Color.Transparent
    val textColor = if (isActive) colors.txt else colors.txt2
    val border = if (isActive) colors.border else Color.Transparent

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (hasDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.warn)
            )
        }
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun CodeLineRow(
    line: CodeLine,
    codeColors: com.nexcode.app.ui.theme.CodeColors
) {
    val colors = NexcodeTheme.colors
    val lineBg = if (line.hasError) colors.err.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(lineBg)
            .padding(horizontal = 8.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Numéro de ligne
        Text(
            text = line.number.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = colors.txt2.copy(alpha = 0.5f),
            modifier = Modifier.width(24.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Texte avec coloration syntaxique
        val annotatedText = buildAnnotatedCodeString(line, codeColors)
        Text(
            text = annotatedText,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )
    }
}

private fun buildAnnotatedCodeString(
    line: CodeLine,
    codeColors: com.nexcode.app.ui.theme.CodeColors
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val text = line.content
        when {
            text.startsWith("package") || text.startsWith("import") -> {
                val kw = if (text.startsWith("package")) "package" else "import"
                withStyle(SpanStyle(color = codeColors.keyword, fontWeight = FontWeight.SemiBold)) {
                    append(kw)
                }
                append(text.removePrefix(kw))
            }
            text.contains("//") -> {
                val parts = text.split("//", limit = 2)
                append(parts[0])
                withStyle(SpanStyle(color = codeColors.comment)) {
                    append("//" + parts[1])
                }
            }
            text.startsWith("@Composable") -> {
                withStyle(SpanStyle(color = codeColors.annotation)) {
                    append("@Composable")
                }
            }
            text.contains("class MainActivity") -> {
                val kw = "class "
                withStyle(SpanStyle(color = codeColors.keyword)) { append(kw) }
                withStyle(SpanStyle(color = codeColors.type, fontWeight = FontWeight.Bold)) { append("MainActivity") }
                append(" : ")
                withStyle(SpanStyle(color = codeColors.type)) { append("ComponentActivity") }
                append("() {")
            }
            text.contains("override fun") -> {
                withStyle(SpanStyle(color = codeColors.keyword)) { append("    override fun ") }
                withStyle(SpanStyle(color = codeColors.function)) { append("onCreate") }
                append("(savedInstanceState: ")
                withStyle(SpanStyle(color = codeColors.type)) { append("Bundle") }
                append("?) {")
            }
            text.contains("fun CityScreen") -> {
                withStyle(SpanStyle(color = codeColors.keyword)) { append("fun ") }
                withStyle(SpanStyle(color = codeColors.function)) { append("CityScreen") }
                append("(city: ")
                withStyle(SpanStyle(color = codeColors.type)) { append("String") }
                append(") {")
            }
            line.hasError && line.errorSpanText != null -> {
                // Surligner l'erreur avec un soulignement rouge
                val parts = text.split(line.errorSpanText)
                append(parts[0])
                withStyle(
                    SpanStyle(
                        color = Color(0xFFFF5C7A),
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(line.errorSpanText)
                }
                if (parts.size > 1) {
                    append(parts[1])
                }
            }
            else -> {
                append(text)
            }
        }
    }
}
