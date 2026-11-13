package com.nexcode.app.ui.screens.projects

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS09ImportProject(
    viewModel: ProjectsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()
    val repoChipScroll = rememberScrollState()

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importZipFromUri(uri) { onNavigateBack() }
        }
    }

    Box(
        modifier = modifier
            .testTag("screen_s09")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NexcodeTopBar(
                title = stringResource(id = R.string.s09_title),
                navigationIcon = painterResource(id = R.drawable.ic_back),
                onNavigationClick = onNavigateBack
            )

            // Barre d'onglets (.tabbar avec indicateur bas accent 2dp)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabItem(
                        title = stringResource(id = R.string.s09_tab_zip),
                        iconRes = R.drawable.ic_box,
                        isSelected = uiState.importTabIndex == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setImportTab(0) }
                    )
                    TabItem(
                        title = stringResource(id = R.string.s09_tab_git),
                        iconRes = R.drawable.ic_branch,
                        isSelected = uiState.importTabIndex == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setImportTab(1) }
                    )
                }
                HorizontalDivider(color = colors.border, thickness = 1.dp)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                uiState.importError?.let { errMsg ->
                    NexcodeBanner(
                        text = errMsg,
                        icon = painterResource(id = R.drawable.ic_alert),
                        variant = NexcodeBannerVariant.ERROR,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        testTag = "s09_error_banner"
                    )
                }

                if (uiState.importTabIndex == 0) {
                    // ============ VOLET 1 : FICHIER .ZIP ============
                    val borderColor = colors.border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeRadius14)
                            .background(colors.codeBg)
                            .drawBehind {
                                drawRoundRect(
                                    color = borderColor,
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                    ),
                                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                                )
                            }
                            .clickable(enabled = !uiState.isImporting) {
                                zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                            }
                            .padding(vertical = 34.dp, horizontal = 20.dp)
                            .testTag("s09_zip_drop_card"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (uiState.isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .padding(bottom = 8.dp),
                                    color = colors.accent,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_download),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .padding(bottom = 8.dp),
                                    tint = colors.accent
                                )
                            }

                            Text(
                                text = if (uiState.isImporting) "Extraction en cours…" else stringResource(id = R.string.s09_choose_zip_title),
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt
                            )

                            Text(
                                text = stringResource(id = R.string.s09_choose_zip_desc),
                                fontFamily = InterFontFamily,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    if (uiState.recentZipImportName != null) {
                        Spacer(modifier = Modifier.height(14.dp))

                        NexcodeCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("s09_recent_zip_card"),
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(ShapeRadius12)
                                        .background(colors.surface2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_file),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colors.accent
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.recentZipImportName!!,
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.txt
                                    )
                                    Text(
                                        text = uiState.recentZipImportDesc.orEmpty(),
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.5.sp,
                                        color = colors.txt2,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                NexcodeChip(
                                    text = stringResource(id = R.string.s09_success_chip),
                                    variant = NexcodeChipVariant.SUCCESS
                                )
                            }
                        }
                    }
                } else {
                    // ============ VOLET 2 : DÉPÔT GITHUB ============
                    if (uiState.userRepositories.isNotEmpty()) {
                        Text(
                            text = "Vos dépôts GitHub",
                            fontFamily = InterFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt2,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(repoChipScroll)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.userRepositories.take(15).forEach { repo ->
                                val isChosen = uiState.importGitUrl.contains(repo.fullName, ignoreCase = true)
                                NexcodeChip(
                                    text = repo.fullName,
                                    variant = if (isChosen) NexcodeChipVariant.ACCENT else NexcodeChipVariant.OUTLINE,
                                    onClick = { viewModel.selectUserRepository(repo.fullName, repo.defaultBranch) },
                                    testTag = "s09_repo_chip_${repo.name}"
                                )
                            }
                        }
                    }

                    NexcodeTextField(
                        value = uiState.importGitUrl,
                        onValueChange = { viewModel.updateImportGitUrl(it) },
                        label = stringResource(id = R.string.s09_repo_url_label),
                        placeholder = "utilisateur/depot ou https://github.com/...",
                        isMono = true,
                        testTag = "s09_git_url_input"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    NexcodeTextField(
                        value = uiState.importGitBranch,
                        onValueChange = { viewModel.updateImportGitBranch(it) },
                        label = stringResource(id = R.string.s09_branch_label),
                        placeholder = "main",
                        isMono = true,
                        testTag = "s09_git_branch_input"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NexcodeButton(
                        text = if (uiState.isImporting) "Clonage en cours…" else stringResource(id = R.string.s09_import_repo_btn),
                        onClick = { viewModel.importGit { onNavigateBack() } },
                        variant = NexcodeButtonVariant.PRIMARY,
                        icon = painterResource(id = R.drawable.ic_pull),
                        enabled = !uiState.isImporting,
                        fullWidth = true,
                        testTag = "s09_import_git_btn"
                    )

                    Text(
                        text = stringResource(id = R.string.s09_git_hint),
                        fontFamily = InterFontFamily,
                        fontSize = 11.5.sp,
                        color = colors.txt2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) colors.accent else colors.txt2
            )
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.accent else colors.txt2
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colors.accent)
            )
        }
    }
}
