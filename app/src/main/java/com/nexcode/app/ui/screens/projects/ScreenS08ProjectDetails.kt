package com.nexcode.app.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
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

    val project = uiState.selectedProject ?: uiState.projects.firstOrNull()

    var showBuildErrorModal by remember { mutableStateOf(false) }

    if (uiState.showDeleteDialog && project != null) {
        com.nexcode.app.ui.components.modals.ModalDeleteProject(
            projectName = project.name,
            githubRepo = project.gitRepo.ifBlank { "dépôt local" },
            onDismiss = { viewModel.dismissDeleteDialog() },
            onConfirm = {
                viewModel.confirmDeleteProject()
                onNavigateBack()
            }
        )
    }

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
            NexcodeTopBar(
                title = "",
                navigationIcon = painterResource(id = R.drawable.ic_back),
                onNavigationClick = onNavigateBack,
                actions = {
                    if (project != null) {
                        NexcodeIconButton(
                            icon = painterResource(id = R.drawable.ic_trash),
                            onClick = { viewModel.openDeleteDialog(project) },
                            contentDescription = "Supprimer le projet",
                            testTag = "s08_trash_btn"
                        )
                    }
                    NexcodeIconButton(
                        icon = painterResource(id = R.drawable.ic_more),
                        onClick = onNavigateGitBranch,
                        contentDescription = "Options du dépôt",
                        testTag = "s08_more_btn"
                    )
                }
            )

            if (project == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.s05_empty_title),
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.txt2
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                ) {
                    // En-tête projet avec tuile 62x62 dp
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

                    // Carte synchronisation GitHub (onclick -> s25)
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
                                    text = project.gitRepo.ifBlank { "Dépôt local (non lié à GitHub)" },
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
                                text = if (project.onGitHub) stringResource(id = R.string.s08_up_to_date) else "Local",
                                variant = if (project.onGitHub) NexcodeChipVariant.SUCCESS else NexcodeChipVariant.OUTLINE
                            )
                        }
                    }

                    // Statistiques sur 3 colonnes
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

                    // Actions rapides
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

                    // Derniers builds réels
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
                            if (uiState.recentBuilds.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onNavigateBuild)
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(colors.surface2),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_play),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = colors.accent
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Aucun build enregistré",
                                            fontFamily = InterFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.txt
                                        )
                                        Text(
                                            text = "Lancez un workflow GitHub Actions depuis l'onglet Build",
                                            fontFamily = InterFontFamily,
                                            fontSize = 11.5.sp,
                                            color = colors.txt2,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_fwd),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = colors.txt2
                                    )
                                }
                            } else {
                                uiState.recentBuilds.forEachIndexed { index, buildItem ->
                                    if (index > 0) {
                                        HorizontalDivider(thickness = 1.dp, color = colors.border)
                                    }
                                    BuildRowItem(
                                        isSuccess = buildItem.isSuccess,
                                        title = buildItem.versionText,
                                        subtitle = buildItem.dateAndSizeText,
                                        onClick = {
                                            if (buildItem.isSuccess) {
                                                onNavigateBuildReport()
                                            } else {
                                                showBuildErrorModal = true
                                            }
                                        },
                                        trailingContent = {
                                            if (buildItem.hasLogsChip) {
                                                NexcodeChip(
                                                    text = stringResource(id = R.string.s08_logs_chip),
                                                    variant = NexcodeChipVariant.ERROR
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_fwd),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = colors.txt2
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
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
