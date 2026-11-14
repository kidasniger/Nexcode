package com.nexcode.app.ui.screens.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ScreenS10FileExplorer(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateEditor: () -> Unit,
    onNavigateResources: () -> Unit,
    onNavigateDependencies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag("screen_s10")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // TopBar S10 : titre = nom réel du projet ouvert (vide si aucun projet)
        NexcodeTopBar(
            title = uiState.projectName.ifBlank { "Éditeur" },
            subtitle = if (uiState.hasProject) "Projet local" else null,
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack,
            actions = {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_search),
                    contentDescription = stringResource(id = R.string.s12_title),
                    onClick = onNavigateSearch,
                    testTag = "s10_btn_search"
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

        // Arborescence de fichiers réelle dans une Card avec marge 0 16px, border-radius 12dp
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(ShapeRadius12)
                    .background(colors.surface)
                    .border(1.dp, colors.border, ShapeRadius12)
            ) {
                if (!uiState.hasProject) {
                    // État vide honnête : aucun projet ouvert, aucune donnée inventée
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = colors.txt2
                        )
                        Text(
                            text = "Aucun projet ouvert",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.txt,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = "Ouvrez un projet depuis l'écran Projets pour explorer ses fichiers réels.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                } else if (uiState.treeItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Ce dossier de projet est vide.",
                            fontSize = 12.sp,
                            color = colors.txt2
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(uiState.treeItems.filter { it.path.isNotEmpty() }) { item ->
                            TreeRowItem(
                                item = item,
                                onClick = {
                                    viewModel.selectFile(item)
                                    when {
                                        item.isDirectory -> Unit // expansion gérée par le ViewModel
                                        item.path.contains("/res/") && item.path.endsWith(".xml") -> onNavigateResources()
                                        item.path.endsWith(".gradle.kts") || item.path.endsWith(".gradle") -> onNavigateDependencies()
                                        else -> onNavigateEditor()
                                    }
                                },
                                onLongClick = {
                                    viewModel.showContextMenu(item)
                                }
                            )
                        }

                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                thickness = 1.dp,
                                color = colors.border
                            )

                            // Ligne indicative appui long (48 dp cible tactile)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        uiState.treeItems.firstOrNull { it.path.isNotEmpty() }?.let { viewModel.showContextMenu(it) }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.txt2
                                )
                                Text(
                                    text = stringResource(id = R.string.s10_file_menu_hint),
                                    color = colors.txt2,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Barre d'actions inférieure : Dossier, Fichier, Ouvrir
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexcodeButton(
                text = stringResource(id = R.string.s10_btn_folder),
                icon = painterResource(id = R.drawable.ic_folder),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.SMALL,
                enabled = uiState.hasProject,
                onClick = { viewModel.openNewEntryDialog(isDirectory = true) },
                modifier = Modifier.weight(1f),
                testTag = "s10_btn_new_folder"
            )

            NexcodeButton(
                text = stringResource(id = R.string.s10_btn_file),
                icon = painterResource(id = R.drawable.ic_plus),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.SMALL,
                enabled = uiState.hasProject,
                onClick = { viewModel.openNewEntryDialog(isDirectory = false) },
                modifier = Modifier.weight(1f),
                testTag = "s10_btn_new_file"
            )

            NexcodeButton(
                text = stringResource(id = R.string.s10_btn_open),
                icon = painterResource(id = R.drawable.ic_code),
                variant = NexcodeButtonVariant.PRIMARY,
                size = NexcodeButtonSize.SMALL,
                onClick = onNavigateEditor,
                modifier = Modifier.weight(1.2f),
                testTag = "s10_btn_open_editor"
            )
        }
    }

    // Modal Menu Fichier (m-filemenu)
    if (uiState.showFileContextMenu) {
        com.nexcode.app.ui.components.modals.ModalFileMenu(
            onDismiss = { viewModel.hideContextMenu() },
            onNewFile = { viewModel.openNewEntryDialog(isDirectory = false) },
            onRename = { viewModel.openRenameDialog() },
            onDuplicate = { viewModel.duplicateSelectedFile() },
            onDelete = { viewModel.deleteSelectedFile() }
        )
    }

    if (uiState.showNewEntryDialog) {
        var entryName by remember(uiState.newEntryIsDirectory) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissNewEntryDialog() },
            containerColor = colors.surface,
            titleContentColor = colors.txt,
            textContentColor = colors.txt2,
            title = {
                Text(
                    text = if (uiState.newEntryIsDirectory) "Nouveau dossier" else "Nouveau fichier",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    NexcodeTextField(
                        value = entryName,
                        onValueChange = { entryName = it },
                        label = if (uiState.newEntryIsDirectory) "Nom du dossier" else "Nom du fichier",
                        isMono = true
                    )
                    Text(
                        text = "Le fichier est réellement créé dans le dossier sélectionné (ou à la racine du projet).",
                        fontSize = 11.sp,
                        color = colors.txt2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createNewEntry(entryName, uiState.newEntryIsDirectory) }) {
                    Text("Créer", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNewEntryDialog() }) {
                    Text("Annuler", color = colors.txt2)
                }
            }
        )
    }

    if (uiState.showRenameDialog && uiState.selectedFileForMenu != null) {
        var renamed by remember(uiState.selectedFileForMenu) {
            mutableStateOf(uiState.selectedFileForMenu?.name.orEmpty())
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissRenameDialog() },
            containerColor = colors.surface,
            titleContentColor = colors.txt,
            textContentColor = colors.txt2,
            title = { Text("Renommer", fontWeight = FontWeight.Bold) },
            text = {
                NexcodeTextField(
                    value = renamed,
                    onValueChange = { renamed = it },
                    label = "Nouveau nom",
                    isMono = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.renameSelectedFile(renamed) }) {
                    Text("Enregistrer", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRenameDialog() }) {
                    Text("Annuler", color = colors.txt2)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRowItem(
    item: FileTreeItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = NexcodeTheme.colors
    val indentPadding = (item.indentLevel * 14).dp

    val iconRes = if (item.isDirectory) R.drawable.ic_folder else R.drawable.ic_file
    val iconColor = if (item.isSelected) colors.accent else colors.txt2
    val textColor = if (item.isSelected) colors.accent else colors.txt
    val bgColor = if (item.isSelected) colors.accent.copy(alpha = 0.08f) else colors.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 14.dp + indentPadding, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = item.name,
            color = textColor,
            fontSize = 12.5.sp,
            fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = if (item.name.contains(".")) JetBrainsMonoFontFamily else FontFamily.Default,
            modifier = Modifier.weight(1f)
        )

        if (item.countOrDesc != null) {
            Text(
                text = item.countOrDesc,
                color = colors.txt2,
                fontSize = 11.sp
            )
        }

        if (item.isModified) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.warn)
            )
        }
    }
}
