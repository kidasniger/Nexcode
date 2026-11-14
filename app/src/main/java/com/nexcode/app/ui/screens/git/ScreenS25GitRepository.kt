package com.nexcode.app.ui.screens.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun ScreenS25GitRepository(
    viewModel: GitViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateConflict: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBranchPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshGitStatus(fetchRemote = false)
    }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.dismissNotice()
        }
    }

    if (showBranchPicker) {
        val branches = uiState.availableBranches.ifEmpty { listOf(uiState.currentBranch) }
        AlertDialog(
            onDismissRequest = { showBranchPicker = false },
            containerColor = colors.surface,
            titleContentColor = colors.txt,
            textContentColor = colors.txt2,
            title = {
                Text(
                    text = "Changer de branche",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    branches.forEach { branch ->
                        val isCurrent = branch == uiState.currentBranch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(ShapeRadius12)
                                .background(if (isCurrent) colors.accent.copy(alpha = 0.12f) else colors.surface2)
                                .clickable {
                                    showBranchPicker = false
                                    if (!isCurrent) viewModel.switchBranch(branch)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = branch,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) colors.accent else colors.txt
                            )
                            if (isCurrent) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBranchPicker = false }) {
                    Text(text = stringResource(id = R.string.modal_cancel), color = colors.txt2)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .testTag("screen_s25")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Topbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_back),
                    onClick = onNavigateBack,
                    contentDescription = stringResource(id = R.string.back),
                    testTag = "s25_btn_back"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.s25_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )
                    Text(
                        text = uiState.repoFullName,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.5.sp,
                        color = colors.txt2
                    )
                }

                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_clock),
                    onClick = onNavigateHistory,
                    contentDescription = stringResource(id = R.string.s26_title),
                    testTag = "s25_btn_history"
                )
            }

            // Pad content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Carte Branche
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_branch),
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = colors.txt2
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${stringResource(id = R.string.s25_branch_label)} ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.txt
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.surface2)
                                        .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = uiState.currentBranch,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.txt
                                    )
                                }
                            }
                            Text(
                                text = uiState.branchStatusText,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        NexcodeButton(
                            text = stringResource(id = R.string.s25_btn_change_branch),
                            onClick = { showBranchPicker = true },
                            variant = NexcodeButtonVariant.SECONDARY,
                            size = NexcodeButtonSize.SMALL,
                            icon = painterResource(id = R.drawable.ic_down),
                            enabled = uiState.hasGitRepo && !uiState.isBusy,
                            testTag = "s25_btn_change_branch"
                        )
                    }
                }

                if (uiState.viewMode == GitViewMode.DEFAULT) {
                    if (uiState.conflicts.isNotEmpty()) {
                        NexcodeBanner(
                            text = "${uiState.conflicts.size} conflit(s) non résolu(s). Touchez pour résoudre.",
                            icon = painterResource(id = R.drawable.ic_merge),
                            variant = NexcodeBannerVariant.WARNING,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clickable { onNavigateConflict() },
                            testTag = "s25_banner_conflicts"
                        )
                    } else if (uiState.behindBy > 0) {
                        NexcodeBanner(
                            text = "Le dépôt distant a ${uiState.behindBy} commit(s) d'avance — un pull est disponible.",
                            icon = painterResource(id = R.drawable.ic_alert),
                            variant = NexcodeBannerVariant.WARNING,
                            modifier = Modifier.padding(bottom = 10.dp),
                            testTag = "s25_banner_ahead"
                        )
                    }

                    val modifiedCount = uiState.modifiedFiles.size
                    Text(
                        text = if (modifiedCount == 1) "1 fichier modifié" else "$modifiedCount fichiers modifiés",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt2,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    NexcodeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (uiState.modifiedFiles.isEmpty()) {
                            Text(
                                text = if (uiState.hasGitRepo) {
                                    "Aucune modification locale dans la copie de travail."
                                } else {
                                    "Importez un dépôt GitHub pour activer le suivi des modifications."
                                },
                                fontSize = 12.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        } else {
                            uiState.modifiedFiles.forEachIndexed { index, file ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = colors.border,
                                        thickness = 1.dp
                                    )
                                }
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = file.name,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 12.sp,
                                            color = colors.txt,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "+${file.added}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.ok
                                            )
                                            Text(
                                                text = "−${file.removed}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.err
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Champ message de commit
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.s25_commit_msg_label),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt2,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(ShapeRadius12)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, ShapeRadius12)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (uiState.commitMessage.isEmpty()) {
                                    Text(
                                        text = "feat: décrivez vos modifications…",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 13.5.sp,
                                        color = colors.txt2.copy(alpha = 0.7f)
                                    )
                                }
                                BasicTextField(
                                    value = uiState.commitMessage,
                                    onValueChange = { viewModel.updateCommitMessage(it) },
                                    modifier = Modifier
                                        .testTag("s25_input_commit_msg")
                                        .fillMaxWidth(),
                                    textStyle = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 13.5.sp,
                                        color = colors.txt
                                    ),
                                    cursorBrush = SolidColor(colors.accent),
                                    singleLine = true
                                )
                            }
                        }
                        val convMatch = Regex("^(feat|fix|chore|docs|style|refactor|test)(\\([^)]+\\))?:").find(uiState.commitMessage.trim())
                        if (convMatch != null) {
                            val type = convMatch.groupValues[1]
                            Text(
                                text = "Format conventionnel détecté : type « $type »",
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Boutons d'action : Tirer (pull) + Commit & pousser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NexcodeButton(
                            text = stringResource(id = R.string.s25_btn_pull),
                            onClick = {
                                viewModel.pullRepository(onConflictFound = onNavigateConflict)
                            },
                            variant = NexcodeButtonVariant.SECONDARY,
                            size = NexcodeButtonSize.DEFAULT,
                            icon = painterResource(id = R.drawable.ic_pull),
                            enabled = !uiState.isBusy,
                            modifier = Modifier.weight(1f),
                            testTag = "s25_btn_pull"
                        )

                        NexcodeButton(
                            text = if (uiState.isBusy) "Synchronisation…" else stringResource(id = R.string.s25_btn_commit_push),
                            onClick = { viewModel.commitAndPush() },
                            variant = NexcodeButtonVariant.PRIMARY,
                            size = NexcodeButtonSize.DEFAULT,
                            icon = painterResource(id = R.drawable.ic_push),
                            enabled = !uiState.isBusy,
                            modifier = Modifier.weight(1.4f),
                            testTag = "s25_btn_commit_push"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // ÉTAT ERREUR (data-v="err")
                    NexcodeBanner(
                        text = "${uiState.errorTitle}\n${uiState.errorDescription}",
                        icon = painterResource(id = R.drawable.ic_wifi),
                        variant = NexcodeBannerVariant.ERROR,
                        modifier = Modifier.padding(bottom = 12.dp),
                        testTag = "s25_banner_error"
                    )

                    NexcodeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (uiState.unpushedCommits.isEmpty()) {
                            Text(
                                text = "Aucun commit local en attente de push.",
                                fontSize = 12.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        } else {
                            uiState.unpushedCommits.forEachIndexed { index, commit ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = colors.border,
                                        thickness = 1.dp
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${commit.hash} · ${commit.scope}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.txt
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(colors.warn.copy(alpha = 0.12f))
                                            .border(1.dp, colors.warn.copy(alpha = 0.35f), PillShape)
                                            .padding(horizontal = 9.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.s25_chip_unpushed),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.warn
                                        )
                                    }
                                }
                            }
                        }
                    }

                    NexcodeButton(
                        text = if (uiState.isBusy) "Nouvelle tentative…" else stringResource(id = R.string.s25_btn_retry_push),
                        onClick = { viewModel.retryPush() },
                        variant = NexcodeButtonVariant.PRIMARY,
                        size = NexcodeButtonSize.DEFAULT,
                        icon = painterResource(id = R.drawable.ic_refresh),
                        enabled = !uiState.isBusy,
                        fullWidth = true,
                        testTag = "s25_btn_retry_push"
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
