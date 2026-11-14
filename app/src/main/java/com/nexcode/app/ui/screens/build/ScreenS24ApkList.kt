package com.nexcode.app.ui.screens.build

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTile
import com.nexcode.app.ui.components.NexcodeTileVariant
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun ScreenS24ApkList(
    viewModel: BuildViewModel,
    onNavigateBack: () -> Unit,
    onNavigateGit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .testTag("screen_s24")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    testTag = "s24_btn_back"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.s24_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )
                    Text(
                        text = stringResource(id = R.string.s24_sub),
                        fontSize = 12.sp,
                        color = colors.txt2
                    )
                }
            }

            // Contenu défilable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Notice Play Protect / sources inconnues
                uiState.workflowNotice?.let { notice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NexcodeBanner(
                            text = notice,
                            icon = painterResource(id = R.drawable.ic_alert),
                            variant = NexcodeBannerVariant.WARNING,
                            modifier = Modifier.weight(1f),
                            testTag = "s24_banner_warn"
                        )
                        NexcodeIconButton(
                            icon = painterResource(id = R.drawable.ic_close),
                            onClick = { viewModel.dismissNotice() },
                            contentDescription = null,
                            testTag = "s24_btn_dismiss_notice"
                        )
                    }
                }

                // Liste des APK
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    uiState.apkList.forEachIndexed { index, apk ->
                        ApkRow(
                            apk = apk,
                            accentIndex = index
                        )
                        if (index < uiState.apkList.lastIndex) {
                            HorizontalDivider(color = colors.border, thickness = 1.dp)
                        }
                    }
                }

                NexcodeButton(
                    text = stringResource(id = R.string.s24_btn_github_releases),
                    onClick = onNavigateGit,
                    variant = NexcodeButtonVariant.PRIMARY,
                    size = NexcodeButtonSize.DEFAULT,
                    icon = painterResource(id = R.drawable.ic_up),
                    fullWidth = true,
                    testTag = "s24_btn_github_releases"
                )

                Spacer(
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ApkRow(
    apk: ApkItem,
    accentIndex: Int
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexcodeTile(
            icon = painterResource(id = R.drawable.ic_box),
            variant = if (apk.isAccent) NexcodeTileVariant.ACCENT else NexcodeTileVariant.SURFACE,
            isSmall = true,
            testTag = "s24_tile_apk_$accentIndex"
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = apk.nameRes),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.txt
            )
            Text(
                text = stringResource(id = apk.descriptionRes),
                fontSize = 11.sp,
                color = colors.txt2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        NexcodeButton(
            text = stringResource(id = R.string.s24_btn_install),
            onClick = { },
            variant = if (apk.isAccent) NexcodeButtonVariant.PRIMARY else NexcodeButtonVariant.SECONDARY,
            size = NexcodeButtonSize.SMALL,
            testTag = "s24_btn_install_$accentIndex"
        )
    }
}
