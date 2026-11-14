package com.nexcode.app.ui.screens.editor

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun ScreenS13Resources(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateCorrectInSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag("screen_s13")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // TopBar S13 : Ressources / res · values + Bouton +
        NexcodeTopBar(
            title = stringResource(id = R.string.s13_title),
            subtitle = stringResource(id = R.string.s13_sub),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack,
            actions = {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Ajouter une ressource",
                    onClick = { viewModel.openAddResourceDialog() },
                    enabled = uiState.hasProject,
                    testTag = "s13_btn_add"
                )
            }
        )

        // Messages réels issus des opérations disque
        val fileMessage = uiState.fileMessage
        val fileError = uiState.fileError
        if (fileMessage != null || fileError != null) {
            NexcodeBanner(
                text = fileError ?: fileMessage.orEmpty(),
                icon = painterResource(id = if (fileError != null) R.drawable.ic_alert else R.drawable.ic_check),
                variant = if (fileError != null) NexcodeBannerVariant.ERROR else NexcodeBannerVariant.SUCCESS,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Barre d'onglets .tabbar (Chaînes, Couleurs, Dimensions, Thèmes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ResourceTabButton(
                title = stringResource(id = R.string.s13_tab_strings),
                isSelected = uiState.activeResourceTab == ResourceTab.STRINGS,
                onClick = { viewModel.selectResourceTab(ResourceTab.STRINGS) },
                modifier = Modifier.weight(1f)
            )

            ResourceTabButton(
                title = stringResource(id = R.string.s13_tab_colors),
                isSelected = uiState.activeResourceTab == ResourceTab.COLORS,
                onClick = { viewModel.selectResourceTab(ResourceTab.COLORS) },
                modifier = Modifier.weight(1f)
            )

            ResourceTabButton(
                title = stringResource(id = R.string.s13_tab_dimensions),
                isSelected = uiState.activeResourceTab == ResourceTab.DIMENSIONS,
                onClick = { viewModel.selectResourceTab(ResourceTab.DIMENSIONS) },
                modifier = Modifier.weight(1f)
            )

            ResourceTabButton(
                title = stringResource(id = R.string.s13_tab_themes),
                isSelected = uiState.activeResourceTab == ResourceTab.THEMES,
                onClick = { viewModel.selectResourceTab(ResourceTab.THEMES) },
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)

        // Volet actif selon l'onglet
        when (uiState.activeResourceTab) {
            ResourceTab.STRINGS -> StringsResourcePane(
                uiState = uiState,
                onEditString = { viewModel.startEditingString(it) }
            )
            ResourceTab.COLORS -> ColorsResourcePane(
                uiState = uiState,
                onAddColor = { viewModel.openAddResourceDialog() }
            )
            ResourceTab.DIMENSIONS -> DimensionsResourcePane(uiState = uiState)
            ResourceTab.THEMES -> ThemesResourcePane(
                onOpenThemesFile = { viewModel.toggleThemesPreviewDialog(true) }
            )
        }
    }

    if (uiState.showAddResourceDialog) {
        val isColorMode = uiState.activeResourceTab == ResourceTab.COLORS
        var keyInput by remember(isColorMode) { mutableStateOf("") }
        var valueInput by remember(isColorMode) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddResourceDialog() },
            containerColor = colors.surface,
            titleContentColor = colors.txt,
            textContentColor = colors.txt2,
            title = {
                Text(
                    text = if (isColorMode) "Nouvelle couleur" else "Nouvelle chaîne",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    NexcodeTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = "Clé XML",
                        isMono = true
                    )
                    NexcodeTextField(
                        value = valueInput,
                        onValueChange = { valueInput = it },
                        label = if (isColorMode) "Code hexadécimal" else "Valeur",
                        isMono = isColorMode
                    )
                    Text(
                        text = "La ressource est réellement écrite dans le fichier res/values correspondant du projet.",
                        fontSize = 11.sp,
                        color = colors.txt2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isColorMode) viewModel.addColorResource(keyInput, valueInput)
                        else viewModel.addStringResource(keyInput, valueInput)
                    }
                ) {
                    Text("Ajouter", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAddResourceDialog() }) {
                    Text("Annuler", color = colors.txt2)
                }
            }
        )
    }

    val editing = uiState.editingStringResource
    if (editing != null) {
        var updatedValue by remember(editing.key) { mutableStateOf(editing.value) }
        AlertDialog(
            onDismissRequest = { viewModel.dismissEditingString() },
            containerColor = colors.surface,
            titleContentColor = colors.txt,
            textContentColor = colors.txt2,
            title = { Text(editing.key, fontFamily = JetBrainsMonoFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                NexcodeTextField(
                    value = updatedValue,
                    onValueChange = { updatedValue = it },
                    label = "Nouvelle valeur"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveEditedString(editing.key, updatedValue) }) {
                    Text("Enregistrer", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEditingString() }) {
                    Text("Annuler", color = colors.txt2)
                }
            }
        )
    }

    if (uiState.showThemesPreviewDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleThemesPreviewDialog(false) },
            containerColor = colors.surface,
            titleContentColor = colors.txt,
            textContentColor = colors.txt2,
            title = { Text("themes.xml", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    // Contenu RÉEL du fichier res/values/themes.xml du projet,
                    // ou notice honnête si le fichier n'existe pas
                    text = uiState.themesFileContent
                        ?: "themes.xml est introuvable dans ce projet (res/values/themes.xml absent).",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = colors.txt
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleThemesPreviewDialog(false) }) {
                    Text("Fermer", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ResourceTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    val textColor = if (isSelected) colors.accent else colors.txt2

    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(2.dp)
                .background(if (isSelected) colors.accent else Color.Transparent)
        )
    }
}

@Composable
private fun StringsResourcePane(
    uiState: EditorUiState,
    onEditString: (ResourceStringItem) -> Unit
) {
    val colors = NexcodeTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (!uiState.hasProject) {
            item {
                EmptyResourceCard(text = "Aucun projet ouvert — les ressources listées ici proviennent des fichiers res/values réels du projet.")
            }
        } else {
            // Carte listant les clés/valeurs RÉELLES des fichiers res/values*/strings.xml
            item {
                if (uiState.stringResources.isEmpty()) {
                    EmptyResourceCard(
                        text = "Aucune chaîne trouvée — aucun fichier strings.xml dans les dossiers res/values du projet."
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeRadius12)
                            .background(colors.surface)
                            .border(1.dp, colors.border, ShapeRadius12)
                    ) {
                        uiState.stringResources.forEachIndexed { index, strItem ->
                            if (index > 0) {
                                HorizontalDivider(thickness = 0.5.dp, color = colors.border)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strItem.key,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = colors.accent
                                    )
                                    Text(
                                        text = strItem.value,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.txt,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                NexcodeIconButton(
                                    icon = painterResource(id = R.drawable.ic_edit),
                                    contentDescription = "Modifier ${strItem.key}",
                                    onClick = { onEditString(strItem) }
                                )
                            }
                        }
                    }
                }
            }

            // Carte Traductions : la fonctionnalité n'existe pas — notice honnête
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius12)
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.s13_translations_title),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )
                    Text(
                        text = "Traduction automatique non disponible dans cette version de NEXCODE.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = colors.txt2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyResourceCard(text: String) {
    val colors = NexcodeTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeRadius12)
            .background(colors.surface)
            .border(1.dp, colors.border, ShapeRadius12)
            .padding(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = colors.txt2
        )
    }
}

@Composable
private fun ColorsResourcePane(
    uiState: EditorUiState,
    onAddColor: () -> Unit
) {
    val colors = NexcodeTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            if (!uiState.hasProject) {
                EmptyResourceCard(text = "Aucun projet ouvert — les couleurs listées ici proviennent des fichiers res/values réels du projet.")
            } else if (uiState.colorResources.isEmpty()) {
                EmptyResourceCard(text = "Aucune couleur trouvée — aucun fichier colors.xml dans les dossiers res/values du projet.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius12)
                ) {
                    uiState.colorResources.forEachIndexed { index, colItem ->
                        if (index > 0) {
                            HorizontalDivider(thickness = 0.5.dp, color = colors.border)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pastille de couleur réelle
                            val parsedColor = runCatching {
                                Color(android.graphics.Color.parseColor(colItem.hexColor))
                            }.getOrDefault(colors.accent)

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(parsedColor)
                                    .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = colItem.key,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = colors.accent,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = colItem.hexColor,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                color = colors.txt2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NexcodeButton(
                text = stringResource(id = R.string.s13_btn_new_color),
                icon = painterResource(id = R.drawable.ic_plus),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.DEFAULT,
                onClick = onAddColor,
                enabled = uiState.hasProject,
                modifier = Modifier.fillMaxWidth(),
                testTag = "s13_btn_new_color"
            )
        }
    }
}

@Composable
private fun DimensionsResourcePane(uiState: EditorUiState) {
    val colors = NexcodeTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            if (!uiState.hasProject) {
                EmptyResourceCard(text = "Aucun projet ouvert — les dimensions listées ici proviennent des fichiers res/values réels du projet.")
            } else if (uiState.dimensionResources.isEmpty()) {
                EmptyResourceCard(text = "Aucune dimension trouvée — aucun fichier dimens.xml dans les dossiers res/values du projet.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius12)
                ) {
                    uiState.dimensionResources.forEachIndexed { index, dimItem ->
                        if (index > 0) {
                            HorizontalDivider(thickness = 0.5.dp, color = colors.border)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dimItem.key,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = colors.accent,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = dimItem.value,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.txt
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemesResourcePane(
    onOpenThemesFile: () -> Unit
) {
    val colors = NexcodeTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeRadius12)
                .background(colors.surface)
                .border(1.dp, colors.border, ShapeRadius12)
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(id = R.string.s13_themes_title),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt
            )
            Text(
                text = "Contenu réel du fichier res/values/themes.xml du projet ouvert.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = colors.txt2,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            NexcodeButton(
                text = stringResource(id = R.string.s13_btn_open_file),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.SMALL,
                onClick = onOpenThemesFile,
                testTag = "s13_btn_open_themes"
            )
        }
    }
}
