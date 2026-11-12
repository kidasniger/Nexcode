package com.nexcode.app.ui.screens.git

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape

@Composable
fun ScreenS26CommitHistory(
    viewModel: GitViewModel,
    onNavigateBack: () -> Unit,
    onNavigateConflict: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.dismissNotice()
        }
    }

    Box(
        modifier = modifier
            .testTag("screen_s26")
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
                    .height(56.dp)
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_back),
                    onClick = onNavigateBack,
                    contentDescription = stringResource(id = R.string.back),
                    testTag = "s26_btn_back"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.s26_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )
                    Text(
                        text = stringResource(id = R.string.s26_sub),
                        fontSize = 12.sp,
                        color = colors.txt2
                    )
                }
            }

            // Pad content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Chips de filtre des branches (main, Toutes les branches)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isMain = uiState.selectedFilterBranch == "main"

                    // Chip main (chip-acc)
                    Box(
                        modifier = Modifier
                            .testTag("s26_chip_main")
                            .clip(PillShape)
                            .background(if (isMain) colors.accent else colors.surface2)
                            .then(if (!isMain) Modifier.border(1.dp, colors.border, PillShape) else Modifier)
                            .clickable { viewModel.selectFilterBranch("main") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.s26_chip_main),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMain) colors.onAccent else colors.txt
                        )
                    }

                    // Chip Toutes les branches (chip-out)
                    Box(
                        modifier = Modifier
                            .testTag("s26_chip_all_branches")
                            .clip(PillShape)
                            .background(if (!isMain) colors.accent else colors.surface2)
                            .then(if (isMain) Modifier.border(1.dp, colors.border, PillShape) else Modifier)
                            .clickable { viewModel.selectFilterBranch("all") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.s26_chip_all_branches),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isMain) colors.onAccent else colors.txt
                        )
                    }
                }

                // Carte liste des commits (.card)
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    uiState.commits.forEachIndexed { index, commit ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = colors.border,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                        CommitListItem(
                            commit = commit,
                            onClick = {
                                if (index == 0) {
                                    onNavigateConflict()
                                }
                            }
                        )
                    }
                }

                // Bouton Charger plus (209 restants)
                NexcodeButton(
                    text = stringResource(id = R.string.s26_btn_load_more),
                    onClick = { viewModel.loadMoreCommits() },
                    variant = NexcodeButtonVariant.SECONDARY,
                    size = NexcodeButtonSize.DEFAULT,
                    icon = painterResource(id = R.drawable.ic_down),
                    fullWidth = true,
                    testTag = "s26_btn_load_more"
                )

                Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun CommitListItem(
    commit: GitCommit,
    onClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icône de commit (.li-ic ou .li-ic.acc)
        val iconBg = if (commit.isAccentIcon) colors.accent.copy(alpha = 0.15f) else colors.surface2
        val iconTint = if (commit.isAccentIcon) colors.accent else colors.txt2

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_commit),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = iconTint
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = commit.message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.txt
            )
            Text(
                text = "${commit.hash} · ${commit.author} · ${commit.date}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.5.sp,
                color = colors.txt2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Chip Poussé (.chip.chip-ok)
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(colors.ok.copy(alpha = 0.12f))
                .border(1.dp, colors.ok.copy(alpha = 0.35f), PillShape)
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text = stringResource(id = R.string.s26_chip_pushed),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ok
            )
        }
    }
}
