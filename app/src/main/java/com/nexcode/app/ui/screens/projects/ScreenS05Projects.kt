package com.nexcode.app.ui.screens.projects

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeDialog
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape
import com.nexcode.app.ui.theme.ShapeRadius12
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS05Projects(
    viewModel: ProjectsViewModel,
    onNavigateNewProject: () -> Unit,
    onNavigateImportProject: () -> Unit,
    onNavigateProjectDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val chipScrollState = rememberScrollState()

    // Dialogue modal de suppression (m-del)
    if (uiState.showDeleteDialog && uiState.projectToDelete != null) {
        val target = uiState.projectToDelete!!
        NexcodeDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = stringResource(id = R.string.s05_delete_dialog_title),
            message = stringResource(id = R.string.s05_delete_dialog_msg, target.name),
            confirmButton = {
                NexcodeButton(
                    text = stringResource(id = R.string.s03_confirm),
                    onClick = { viewModel.confirmDeleteProject() },
                    variant = NexcodeButtonVariant.DANGER,
                    size = NexcodeButtonSize.SMALL,
                    testTag = "s05_confirm_delete_btn"
                )
            },
            dismissButton = {
                NexcodeButton(
                    text = stringResource(id = R.string.s03_cancel),
                    onClick = { viewModel.dismissDeleteDialog() },
                    variant = NexcodeButtonVariant.SECONDARY,
                    size = NexcodeButtonSize.SMALL,
                    testTag = "s05_cancel_delete_btn"
                )
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trash),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = colors.err
                )
            }
        )
    }

    Box(
        modifier = modifier
            .testTag("screen_s05")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar spécifique à S05 : Titre, sous-titre et actions Importer & Nouveau
            NexcodeTopBar(
                title = stringResource(id = R.string.s05_title),
                subtitle = stringResource(
                    id = R.string.s05_subtitle_format,
                    if (uiState.viewMode == ProjectsViewMode.EMPTY) 0 else uiState.totalProjectCount,
                    if (uiState.viewMode == ProjectsViewMode.EMPTY) "0 Mo" else uiState.totalSizeText
                ),
                actions = {
                    // Puce permettant de basculer facilement entre état par défaut et état vide (conforme à la consigne de l'étape 7)
                    NexcodeChip(
                        text = if (uiState.viewMode == ProjectsViewMode.DEFAULT) "Défaut" else "Vide",
                        variant = if (uiState.viewMode == ProjectsViewMode.DEFAULT) NexcodeChipVariant.ACCENT else NexcodeChipVariant.DEFAULT,
                        onClick = { viewModel.toggleViewMode() },
                        testTag = "s05_toggle_view_chip"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    NexcodeIconButton(
                        icon = painterResource(id = R.drawable.ic_download),
                        onClick = onNavigateImportProject,
                        contentDescription = "Importer un projet",
                        testTag = "s05_topbar_import_btn"
                    )
                    NexcodeIconButton(
                        icon = painterResource(id = R.drawable.ic_plus),
                        onClick = onNavigateNewProject,
                        contentDescription = "Nouveau projet",
                        testTag = "s05_topbar_new_btn"
                    )
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Champ de recherche (.inwrap avec icone ic_search a gauche)
                NexcodeTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = stringResource(id = R.string.s05_search_placeholder),
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = colors.txt2
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    testTag = "s05_search_input"
                )

                // Ligne de filtres horizontale avec puces (.chip.chip-acc / .chip.chip-out)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipScrollState)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProjectFilter.entries.forEach { filter ->
                        val isSelected = filter == uiState.selectedFilter
                        NexcodeChip(
                            text = filter.label,
                            variant = if (isSelected) NexcodeChipVariant.ACCENT else NexcodeChipVariant.OUTLINE,
                            showDot = isSelected,
                            onClick = { viewModel.onFilterSelect(filter) },
                            testTag = "s05_filter_${filter.name.lowercase()}"
                        )
                    }
                }

                // ============ VUE PAR DÉFAUT (stv data-v="def") ============
                if (uiState.viewMode == ProjectsViewMode.DEFAULT) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(id = R.string.s05_recent_section),
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt2,
                                letterSpacing = 0.08.sp,
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.filteredProjects, key = { it.id }) { project ->
                            ProjectCardItem(
                                project = project,
                                onClick = {
                                    viewModel.selectProject(project)
                                    onNavigateProjectDetails(project.id)
                                },
                                onMoreClick = {
                                    viewModel.openDeleteDialog(project)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // Marge pour le FAB et la barre basse
                        }
                    }
                } else {
                    // ============ VUE VIDE (stv data-v="vide") ============
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(colors.surface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_folder),
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    tint = colors.txt2
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.s05_empty_title),
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt,
                                modifier = Modifier.padding(top = 16.dp)
                            )

                            Text(
                                text = stringResource(id = R.string.s05_empty_desc),
                                fontFamily = InterFontFamily,
                                fontSize = 12.5.sp,
                                color = colors.txt2,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .widthIn(max = 280.dp)
                            )

                            Row(
                                modifier = Modifier.padding(top = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NexcodeButton(
                                    text = stringResource(id = R.string.s05_create),
                                    onClick = onNavigateNewProject,
                                    variant = NexcodeButtonVariant.PRIMARY,
                                    size = NexcodeButtonSize.SMALL,
                                    icon = painterResource(id = R.drawable.ic_plus),
                                    testTag = "s05_empty_create_btn"
                                )
                                NexcodeButton(
                                    text = stringResource(id = R.string.s05_import),
                                    onClick = onNavigateImportProject,
                                    variant = NexcodeButtonVariant.SECONDARY,
                                    size = NexcodeButtonSize.SMALL,
                                    icon = painterResource(id = R.drawable.ic_download),
                                    testTag = "s05_empty_import_btn"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bouton flottant d'action (FAB "+ Nouveau") conforme à .fab de la maquette
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 18.dp)
        ) {
            FloatingActionButton(
                onClick = onNavigateNewProject,
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                shape = PillShape,
                modifier = Modifier
                    .height(44.dp)
                    .testTag("s05_fab_new_project")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.onAccent
                    )
                    Text(
                        text = stringResource(id = R.string.s05_new_fab),
                        fontFamily = InterFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectCardItem(
    project: ProjectItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    val tileBg = when {
        project.tileBgHex != null -> Color(android.graphics.Color.parseColor(project.tileBgHex))
        project.initials == "NM" -> colors.accent
        else -> colors.surface2
    }

    val tileTextColor = when {
        project.tileTextHex != null -> Color(android.graphics.Color.parseColor(project.tileTextHex))
        project.initials == "NM" -> colors.onAccent
        else -> colors.txt2
    }

    NexcodeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("s05_project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tuile d'initiales (.tile) 40x40 dp, rayon 12 dp
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(ShapeRadius12)
                    .background(tileBg)
                    .then(
                        if (project.tileBgHex == null && project.initials != "NM") {
                            Modifier.border(1.dp, colors.border, ShapeRadius12)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = project.initials,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = tileTextColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Textes et statuts
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = project.name,
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${project.language} · ${project.uiFramework} · ${project.modifiedText}",
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp,
                    color = colors.txt2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Puces de statut (.chip-ok, .chip-err, .chip-warn, .chip-out)
                Row(
                    modifier = Modifier.padding(top = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (project.buildStatus) {
                        ProjectBuildStatus.SUCCESS -> {
                            NexcodeChip(
                                text = project.buildStatusLabel,
                                variant = NexcodeChipVariant.SUCCESS,
                                showDot = true
                            )
                        }
                        ProjectBuildStatus.FAILED -> {
                            NexcodeChip(
                                text = project.buildStatusLabel,
                                variant = NexcodeChipVariant.ERROR,
                                showDot = true
                            )
                        }
                        ProjectBuildStatus.RUNNING -> {
                            NexcodeChip(
                                text = project.buildStatusLabel,
                                variant = NexcodeChipVariant.WARNING,
                                showDot = true
                            )
                        }
                        ProjectBuildStatus.DRAFT -> {
                            NexcodeChip(
                                text = project.buildStatusLabel,
                                variant = NexcodeChipVariant.OUTLINE,
                                showDot = true
                            )
                        }
                    }

                    if (project.version != null) {
                        NexcodeChip(
                            text = project.version,
                            variant = NexcodeChipVariant.OUTLINE
                        )
                    }

                    if (project.onGitHub) {
                        NexcodeChip(
                            text = "Sur GitHub",
                            variant = NexcodeChipVariant.ACCENT,
                            icon = painterResource(id = R.drawable.ic_branch)
                        )
                    }
                }
            }

            // Bouton More (.icb avec ic_more)
            NexcodeIconButton(
                icon = painterResource(id = R.drawable.ic_more),
                onClick = onMoreClick,
                contentDescription = "Actions du projet",
                testTag = "s05_project_more_${project.id}"
            )
        }
    }
}
