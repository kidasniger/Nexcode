package com.nexcode.app.ui.screens.projects

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS08ProjectDetails(
    viewModel: ProjectsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateEditor: () -> Unit,
    onNavigateBuild: () -> Unit,
    onNavigateAiChat: () -> Unit,
    onNavigateDesigner: () -> Unit,
    onNavigateGitBranch: () -> Unit,
    onNavigateBuildReport: () -> Unit,
    onNavigateBuildLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()

    val project = uiState.selectedProject ?: uiState.projects.firstOrNull() ?: ProjectItem(
        id = 1L,
        name = "Nimbus Météo",
        packageName = "com.nimbus.meteo",
        initials = "NM",
        language = "Kotlin",
        uiFramework = "Compose",
        modifiedText = "modifié hier à 17:24",
        buildStatus = ProjectBuildStatus.SUCCESS,
        buildStatusLabel = "Build réussi"
    )

    var showBuildErrorModal by remember { mutableStateOf(false) }

    // Dialogue modal de suppression (m-del)
    if (uiState.showDeleteDialog) {
        com.nexcode.app.ui.components.modals.ModalDeleteProject(
            projectName = project.name,
            githubRepo = "nimbus-dev/meteo-android",
            onDismiss = { viewModel.dismissDeleteDialog() },
            onConfirm = {
                viewModel.confirmDeleteProject()
                onNavigateBack()
            }
        )
    }

    // Dialogue modal d'erreur de build (m-builderr)
    if (showBuildErrorModal) {
        com.nexcode.app.ui.components.modals.ModalBuildError(
            onDismiss = { showBuildErrorModal = false },
            onNavigateLogs = onNavigateBuildLogs,
            onNavigateAiChat = onNavigateAiChat
        )
    }

    Box(
        modifier = modifier
            .testTag("screen_s08")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar avec bouton retour vers s05, icone suppression m-del et icone more
            NexcodeTopBar(
                title = "",
                navigationIcon = painterResource(id = R.drawable.ic_back),
                onNavigationClick = onNavigateBack,
                actions = {
                    NexcodeIconButton(
                        icon = painterResource(id = R.drawable.ic_trash),
                        onClick = { viewModel.openDeleteDialog(project) },
                        contentDescription = "Supprimer le projet",
                        testTag = "s08_trash_btn"
                    )
                    NexcodeIconButton(
                        icon = painterResource(id = R.drawable.ic_more),
                        onClick = { /* Actions secondaires */ },
                        contentDescription = "Options supplémentaires",
                        testTag = "s08_more_btn"
                    )
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // En-tête projet avec tuile 62x62 dp (.tile avec NM)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(ShapeRadius14)
                            .background(colors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = project.initials,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onAccent
                        )
                    }

                    Column {
                        Text(
                            text = project.name,
                            fontFamily = InterFontFamily,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.02).sp,
                            color = colors.txt
                        )

                        Text(
                            text = project.packageName,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Row(
                            modifier = Modifier.padding(top = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NexcodeChip(
                                text = project.language,
                                variant = NexcodeChipVariant.ACCENT
                            )
                            NexcodeChip(
                                text = project.uiFramework,
                                variant = NexcodeChipVariant.OUTLINE
                            )
                            NexcodeChip(
                                text = "minSdk ${project.minSdk}",
                                variant = NexcodeChipVariant.OUTLINE
                            )
                        }
                    }
                }

                // Carte synchronisation GitHub (onclick -> go('s25'))
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable(onClick = onNavigateGitBranch)
                        .testTag("s08_github_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(ShapeRadius12)
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_branch),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = colors.accent
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.gitRepo,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = project.gitSyncText,
                                fontFamily = InterFontFamily,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        NexcodeChip(
                            text = stringResource(id = R.string.s08_up_to_date),
                            variant = NexcodeChipVariant.SUCCESS
                        )
                    }
                }

                // Statistiques sur 3 colonnes (.col3 avec 34 Fichiers, 8,2 Mo Taille, 12 Builds)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        value = "${project.fileCount}",
                        label = stringResource(id = R.string.s08_files_label),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = project.sizeText,
                        label = stringResource(id = R.string.s08_size_label),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${project.buildCount}",
                        label = stringResource(id = R.string.s08_builds_label),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section Actions rapides (.sect-t et grille 2x2 .col2)
                Text(
                    text = stringResource(id = R.string.s08_quick_actions),
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt2,
                    letterSpacing = 0.08.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NexcodeButton(
                            text = stringResource(id = R.string.s08_action_edit),
                            onClick = onNavigateEditor,
                            variant = NexcodeButtonVariant.SECONDARY,
                            icon = painterResource(id = R.drawable.ic_code),
                            modifier = Modifier.weight(1f),
                            testTag = "s08_action_edit"
                        )
                        NexcodeButton(
                            text = stringResource(id = R.string.s08_action_compile),
                            onClick = onNavigateBuild,
                            variant = NexcodeButtonVariant.PRIMARY,
                            icon = painterResource(id = R.drawable.ic_play),
                            modifier = Modifier.weight(1f),
                            testTag = "s08_action_compile"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NexcodeButton(
                            text = stringResource(id = R.string.s08_action_ai_chat),
                            onClick = onNavigateAiChat,
                            variant = NexcodeButtonVariant.SECONDARY,
                            icon = painterResource(id = R.drawable.ic_spark),
                            modifier = Modifier.weight(1f),
                            testTag = "s08_action_ai_chat"
                        )
                        NexcodeButton(
                            text = stringResource(id = R.string.s08_action_designer),
                            onClick = onNavigateDesigner,
                            variant = NexcodeButtonVariant.SECONDARY,
                            icon = painterResource(id = R.drawable.ic_layout),
                            modifier = Modifier.weight(1f),
                            testTag = "s08_action_designer"
                        )
                    }
                }

                // Section Derniers builds (.sect-t)
                Text(
                    text = stringResource(id = R.string.s08_last_builds),
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt2,
                    letterSpacing = 0.08.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column {
                        // Build 1 : v1.2.0 release (ok)
                        BuildRowItem(
                            isSuccess = true,
                            title = "v1.2.0 (12) · release",
                            subtitle = "Il y a 2 h · 8,1 Mo · signé",
                            onClick = onNavigateBuildLogs,
                            trailingContent = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_fwd),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.txt2
                                )
                            }
                        )

                        HorizontalDivider(thickness = 1.dp, color = colors.border)

                        // Build 2 : v1.1.9 debug (ok)
                        BuildRowItem(
                            isSuccess = true,
                            title = "v1.1.9 (11) · debug",
                            subtitle = "Hier · 12,4 Mo",
                            onClick = onNavigateBuildLogs,
                            trailingContent = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_fwd),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.txt2
                                )
                            }
                        )

                        HorizontalDivider(thickness = 1.dp, color = colors.border)

                        // Build 3 : v1.1.8 release (err, 2 erreurs)
                        BuildRowItem(
                            isSuccess = false,
                            title = "v1.1.8 (10) · release",
                            subtitle = "Le 3 nov. · 2 erreurs",
                            onClick = { showBuildErrorModal = true },
                            trailingContent = {
                                NexcodeChip(
                                    text = stringResource(id = R.string.s08_logs_chip),
                                    variant = NexcodeChipVariant.ERROR
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors

    NexcodeCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = InterFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.txt,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontSize = 10.5.sp,
                color = colors.txt2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun BuildRowItem(
    isSuccess: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (isSuccess) colors.ok.copy(alpha = 0.12f) else colors.err.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = if (isSuccess) R.drawable.ic_check_c else R.drawable.ic_x_c),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSuccess) colors.ok else colors.err
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.txt
            )
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontSize = 11.5.sp,
                color = colors.txt2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        trailingContent()
    }
}
