package com.nexcode.app.ui.screens.git

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS27ConflictResolution(
    viewModel: GitViewModel,
    onNavigateBack: () -> Unit,
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
            .testTag("screen_s27")
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
                    testTag = "s27_btn_back"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.s27_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )
                    Text(
                        text = "Pull depuis origin/${uiState.currentBranch}",
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
                if (uiState.conflicts.isEmpty()) {
                    NexcodeBanner(
                        text = "Aucun conflit de fusion actif dans le dépôt.",
                        icon = painterResource(id = R.drawable.ic_check_c),
                        variant = NexcodeBannerVariant.SUCCESS,
                        modifier = Modifier.padding(bottom = 12.dp),
                        testTag = "s27_banner_no_conflicts"
                    )
                } else {
                    NexcodeBanner(
                        text = "${uiState.conflicts.size} conflit(s) après le pull de origin/${uiState.currentBranch}. Choisissez une version pour chaque fichier.",
                        icon = painterResource(id = R.drawable.ic_merge),
                        variant = NexcodeBannerVariant.WARNING,
                        modifier = Modifier.padding(bottom = 12.dp),
                        testTag = "s27_banner_conflicts"
                    )

                    uiState.conflicts.forEach { conflict ->
                        ConflictCard(
                            conflict = conflict,
                            onSelectChoice = { choice ->
                                viewModel.chooseConflictResolution(conflict.id, choice)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Carte Fusion intelligente
                    NexcodeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
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
                                    .background(colors.accent2.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_spark),
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp),
                                    tint = colors.accent2
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.s27_card_smart_merge_title),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.txt
                                )
                                Text(
                                    text = stringResource(id = R.string.s27_card_smart_merge_desc),
                                    fontSize = 11.5.sp,
                                    color = colors.txt2,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            NexcodeButton(
                                text = stringResource(id = R.string.s27_btn_preview),
                                onClick = { viewModel.previewSmartMerge() },
                                variant = NexcodeButtonVariant.SECONDARY,
                                size = NexcodeButtonSize.SMALL,
                                testTag = "s27_btn_preview_merge"
                            )
                        }
                    }

                    // Boutons d'action : Annuler le merge + Appliquer les choix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NexcodeButton(
                            text = stringResource(id = R.string.s27_btn_cancel_merge),
                            onClick = { viewModel.cancelMerge(onDone = onNavigateBack) },
                            variant = NexcodeButtonVariant.SECONDARY,
                            size = NexcodeButtonSize.DEFAULT,
                            enabled = !uiState.isBusy,
                            modifier = Modifier.weight(1f),
                            testTag = "s27_btn_cancel_merge"
                        )

                        NexcodeButton(
                            text = stringResource(id = R.string.s27_btn_apply_choices),
                            onClick = {
                                viewModel.applyConflictResolutions(onSuccess = onNavigateBack)
                            },
                            variant = NexcodeButtonVariant.PRIMARY,
                            size = NexcodeButtonSize.DEFAULT,
                            icon = painterResource(id = R.drawable.ic_check),
                            enabled = !uiState.isBusy,
                            modifier = Modifier.weight(1.3f),
                            testTag = "s27_btn_apply_choices"
                        )
                    }
                }

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
private fun ConflictCard(
    conflict: ConflictItem,
    onSelectChoice: (ConflictChoice) -> Unit
) {
    val colors = NexcodeTheme.colors

    Box(
        modifier = Modifier
            .testTag("s27_conflict_${conflict.id}")
            .fillMaxWidth()
            .clip(ShapeRadius14)
            .background(colors.surface)
            .border(1.dp, colors.border, ShapeRadius14)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = conflict.fileAndLine,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt
                )
            }

            HorizontalDivider(color = colors.border, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                val isLocalSelected = conflict.selectedChoice == ConflictChoice.LOCAL
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(BorderStroke(0.5.dp, colors.border))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.accent.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                        )
                        Text(
                            text = stringResource(id = R.string.s27_tag_your_version),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        conflict.localLines.forEach { line ->
                            Text(
                                text = line,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.5.sp,
                                color = colors.txt,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            onClick = { onSelectChoice(ConflictChoice.LOCAL) },
                            modifier = Modifier
                                .testTag("s27_btn_use_local_${conflict.id}")
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(PillShape),
                            shape = PillShape,
                            color = if (isLocalSelected) colors.accent else colors.surface2,
                            contentColor = if (isLocalSelected) colors.onAccent else colors.txt,
                            border = if (isLocalSelected) null else BorderStroke(1.dp, colors.border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(id = R.string.s27_btn_use),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                val isRemoteSelected = conflict.selectedChoice == ConflictChoice.REMOTE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(BorderStroke(0.5.dp, colors.border))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.accent2.copy(alpha = 0.09f))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.accent2)
                        )
                        Text(
                            text = stringResource(id = R.string.s27_tag_remote),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent2
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        conflict.remoteLines.forEach { line ->
                            Text(
                                text = line,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.5.sp,
                                color = colors.txt2,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            onClick = { onSelectChoice(ConflictChoice.REMOTE) },
                            modifier = Modifier
                                .testTag("s27_btn_use_remote_${conflict.id}")
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(PillShape),
                            shape = PillShape,
                            color = if (isRemoteSelected) colors.accent else colors.surface2,
                            contentColor = if (isRemoteSelected) colors.onAccent else colors.txt,
                            border = if (isRemoteSelected) null else BorderStroke(1.dp, colors.border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(id = R.string.s27_btn_use),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
