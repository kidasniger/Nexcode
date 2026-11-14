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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12

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

    Column(
        modifier = modifier
            .testTag("screen_s11")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // TopBar S11 (hauteur >= 54 dp, boutons icônes >= 48 dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .background(colors.surface)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_back),
                contentDescription = "Retour",
                onClick = onNavigateBack,
                testTag = "s11_btn_back"
            )

            Spacer(modifier = Modifier.width(4.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = uiState.activeTab.substringAfterLast('/').ifBlank { "Éditeur de code" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt,
                    maxLines = 1
                )
                if (uiState.activeTab in uiState.unsavedPaths) {
                    // Puce réelle : modifications non enregistrées sur le disque
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.warn)
                    )
                }
            }

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_search),
                contentDescription = "Rechercher",
                onClick = onNavigateSearch,
                testTag = "s11_btn_search"
            )

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_sliders),
                contentDescription = "Ressources / Réglages",
                onClick = onNavigateSettings,
                testTag = "s11_btn_sliders"
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)

        // Messages réels (enregistrement disque, erreurs de lecture...)
        val fileMessage = uiState.fileMessage
        val fileError = uiState.fileError
        if (fileMessage != null || fileError != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                NexcodeBanner(
                    text = fileError ?: fileMessage.orEmpty(),
                    icon = painterResource(id = if (fileError != null) R.drawable.ic_alert else R.drawable.ic_check),
                    variant = if (fileError != null) NexcodeBannerVariant.ERROR else NexcodeBannerVariant.SUCCESS,
                    modifier = Modifier.weight(1f),
                    testTag = "s11_banner_file_status"
                )
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Fermer le message",
                    onClick = { viewModel.dismissFileMessage() }
                )
            }
        }

        // Barre d'onglets (cible >= 48 dp) : onglets = fichiers réellement ouverts
        if (uiState.openTabs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.openTabs.forEach { tabPath ->
                    val isActive = tabPath == uiState.activeTab
                    EditorTabItem(
                        title = tabPath.substringAfterLast('/'),
                        isActive = isActive,
                        hasDot = tabPath in uiState.unsavedPaths,
                        onClick = { viewModel.openTab(tabPath) },
                        onClose = { viewModel.closeTab(tabPath) }
                    )
                }
            }
        } else {
            Text(
                text = "Aucun fichier ouvert — sélectionnez un fichier dans l'explorateur.",
                fontSize = 11.sp,
                color = colors.txt2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        // Zone de contenu défilable (Éditeur réel, éditable ligne par ligne)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            if (uiState.codeLines.isEmpty()) {
                // État vide honnête : aucun fichier chargé
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 24.dp)
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius12)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_code),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = colors.txt2
                    )
                    Text(
                        text = "Aucun fichier ouvert",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "Le contenu affiché ici provient du fichier réel sélectionné dans l'explorateur. Aucun fichier fictif n'est chargé.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = colors.txt2,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                // Carte de code (forcée LTR en RTL)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(ShapeRadius12)
                            .background(colors.codeBg)
                            .border(1.dp, colors.border, ShapeRadius12)
                    ) {
                        // Header de la boîte de code : 3 petits points + nom du fichier réel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surface2)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(colors.err))
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(colors.warn))
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(colors.ok))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = uiState.codeHeaderTitle,
                                fontSize = 11.sp,
                                color = colors.txt2,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                        // Lignes de code réelles, éditables : chaque modification est écrite
                        // sur le disque quand l'utilisateur enregistre
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            uiState.codeLines.forEach { line ->
                                EditableCodeLineRow(
                                    line = line,
                                    onContentChange = { viewModel.updateCodeLine(line.number, it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Barre de symboles (cibles >= 48 dp en hauteur, largeur min 44 dp)
        HorizontalDivider(thickness = 0.5.dp, color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val symbols = listOf("⇥", "{ }", "( )", "< >", "\"", "$", "=", ":", ";", "_")
            symbols.forEach { sym ->
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 44.dp, minHeight = 48.dp)
                        .clickable { viewModel.insertSymbol(sym) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
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
        }

        // Barre d'outils inférieure de l'éditeur (undo, redo, save, gradle, build, ia)
        HorizontalDivider(thickness = 0.5.dp, color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(colors.surface)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_undo),
                contentDescription = "Annuler",
                onClick = { viewModel.undo() },
                enabled = uiState.canUndo,
                testTag = "s11_btn_undo"
            )

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_redo),
                contentDescription = "Rétablir",
                onClick = { viewModel.redo() },
                enabled = uiState.canRedo,
                testTag = "s11_btn_redo"
            )

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_save),
                contentDescription = "Enregistrer sur le disque",
                onClick = { viewModel.saveActiveFile() },
                enabled = uiState.activeTab.isNotBlank(),
                testTag = "s11_btn_save"
            )

            Spacer(modifier = Modifier.weight(1f))

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_box),
                contentDescription = "Dépendances Gradle",
                onClick = onNavigateDependencies,
                testTag = "s11_btn_gradle"
            )

            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_play),
                contentDescription = "Compiler & Exécuter",
                onClick = onNavigateBuild,
                testTag = "s11_btn_run"
            )

            // Bouton IA accentué (cible tactile 48 dp)
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onNavigateAiChat),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent.copy(alpha = 0.15f)),
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
}

@Composable
private fun EditorTabItem(
    title: String,
    isActive: Boolean,
    hasDot: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val colors = NexcodeTheme.colors
    val bg = if (isActive) colors.surface else Color.Transparent
    val textColor = if (isActive) colors.txt else colors.txt2
    val border = if (isActive) colors.border else Color.Transparent

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(8.dp))
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
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Fermer l'onglet $title",
                modifier = Modifier
                    .size(12.dp)
                    .clickable(onClick = onClose),
                tint = colors.txt2
            )
        }
    }
}

@Composable
private fun EditableCodeLineRow(
    line: CodeLine,
    onContentChange: (String) -> Unit
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Numéro de ligne
        Text(
            text = line.number.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = colors.txt2.copy(alpha = 0.55f),
            modifier = Modifier.width(24.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Contenu réel du fichier, éditable
        BasicTextField(
            value = line.content,
            onValueChange = onContentChange,
            textStyle = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.5.sp,
                color = colors.txt
            ),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.weight(1f)
        )
    }
}
