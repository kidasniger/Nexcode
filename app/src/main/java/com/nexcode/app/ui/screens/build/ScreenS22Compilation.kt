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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape

@Composable
fun ScreenS22Compilation(
    viewModel: BuildViewModel,
    onNavigateBack: () -> Unit,
    onNavigateLogs: () -> Unit,
    onNavigateApk: () -> Unit,
    onNavigateAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    val variantLabel = stringResource(
        id = if (uiState.selectedVariant == BuildVariant.RELEASE) {
            R.string.s21_variant_release
        } else {
            R.string.s21_variant_debug
        }
    )

    Box(
        modifier = modifier
            .testTag("screen_s22")
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
                    testTag = "s22_btn_back"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.s22_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )
                    Text(
                        text = buildString {
                            if (uiState.projectName.isNotBlank()) append(uiState.projectName)
                            if (uiState.versionName.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append("v${uiState.versionName}")
                                if (uiState.buildNumber.isNotBlank()) append(" (${uiState.buildNumber})")
                            }
                            if (isNotEmpty()) append(" · ")
                            append(variantLabel)
                        },
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
                // Carte de progression
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${uiState.progressPercent} %",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            modifier = Modifier.testTag("s22_progress_percent")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (uiState.compilationStatus) {
                                    CompilationStatus.RUNNING -> {
                                        val runningStep = uiState.steps.firstOrNull {
                                            it.status == BuildStepStatus.RUNNING
                                        }
                                        runningStep?.let { "Run GitHub Actions · ${it.label}" }
                                            ?: "Run GitHub Actions en cours"
                                    }
                                    CompilationStatus.IDLE -> "Aucun run en cours"
                                    CompilationStatus.CANCELLED -> "Run annulé"
                                    CompilationStatus.FAILED -> "Run terminé en échec"
                                    CompilationStatus.SUCCESS -> "Run terminé avec succès"
                                },
                                fontSize = 12.sp,
                                color = colors.txt2
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(8.dp)
                            .clip(PillShape)
                            .background(colors.surface2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = uiState.progressPercent / 100f)
                                .height(8.dp)
                                .clip(PillShape)
                                .background(colors.accent)
                        )
                    }

                    when (uiState.compilationStatus) {
                        CompilationStatus.SUCCESS -> {
                            NexcodeBanner(
                                text = "Compilation réussie — APK téléchargé depuis les artefacts.",
                                icon = painterResource(id = R.drawable.ic_check_c),
                                variant = NexcodeBannerVariant.SUCCESS,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                testTag = "s22_banner_success"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NexcodeButton(
                                    text = stringResource(id = R.string.s22_btn_view_apk),
                                    onClick = onNavigateApk,
                                    variant = NexcodeButtonVariant.PRIMARY,
                                    size = NexcodeButtonSize.DEFAULT,
                                    icon = painterResource(id = R.drawable.ic_box),
                                    modifier = Modifier.weight(1f),
                                    testTag = "s22_btn_view_apk"
                                )
                                NexcodeButton(
                                    text = stringResource(id = R.string.s22_btn_logs),
                                    onClick = onNavigateLogs,
                                    variant = NexcodeButtonVariant.SECONDARY,
                                    size = NexcodeButtonSize.DEFAULT,
                                    icon = painterResource(id = R.drawable.ic_terminal),
                                    modifier = Modifier.weight(1f),
                                    testTag = "s22_btn_logs"
                                )
                            }
                        }
                        CompilationStatus.CANCELLED -> {
                            Text(
                                text = "Run GitHub Actions annulé. Aucun APK n'a été produit pour ce build.",
                                fontSize = 12.sp,
                                color = colors.warn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            )
                        }
                        CompilationStatus.FAILED -> {
                            NexcodeBanner(
                                text = "Compilation échouée — les logs réels du run sont disponibles.",
                                icon = painterResource(id = R.drawable.ic_alert),
                                variant = NexcodeBannerVariant.ERROR,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                testTag = "s22_banner_failed"
                            )
                        }
                        else -> Unit
                    }
                }

                // Carte des étapes
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    uiState.steps.forEachIndexed { index, step ->
                        BuildStepRow(step = step)
                        if (index < uiState.steps.lastIndex) {
                            HorizontalDivider(color = colors.border, thickness = 1.dp)
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NexcodeButton(
                        text = stringResource(id = R.string.s22_btn_cancel),
                        onClick = { viewModel.cancelCompilation() },
                        variant = NexcodeButtonVariant.DANGER,
                        size = NexcodeButtonSize.DEFAULT,
                        icon = painterResource(id = R.drawable.ic_stop),
                        enabled = uiState.compilationStatus == CompilationStatus.RUNNING,
                        modifier = Modifier.weight(1f),
                        testTag = "s22_btn_cancel"
                    )
                    NexcodeButton(
                        text = stringResource(id = R.string.s22_btn_live_logs),
                        onClick = onNavigateLogs,
                        variant = NexcodeButtonVariant.SECONDARY,
                        size = NexcodeButtonSize.DEFAULT,
                        icon = painterResource(id = R.drawable.ic_terminal),
                        modifier = Modifier.weight(1f),
                        testTag = "s22_btn_live_logs"
                    )
                }

                Text(
                    text = "Le run GitHub Actions continue côté serveur même si vous quittez cet écran.",
                    fontSize = 11.sp,
                    color = colors.txt2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )

                // Accès à l'IA depuis la compilation (erreurs de compilation)
                if (uiState.compilationStatus == CompilationStatus.FAILED) {
                    NexcodeButton(
                        text = stringResource(id = R.string.s23_btn_fix_ai),
                        onClick = onNavigateAiChat,
                        variant = NexcodeButtonVariant.PRIMARY,
                        size = NexcodeButtonSize.DEFAULT,
                        icon = painterResource(id = R.drawable.ic_spark),
                        fullWidth = true,
                        testTag = "s22_btn_fix_ai"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BuildStepRow(step: BuildStep) {
    val colors = NexcodeTheme.colors

    val (iconRes, iconTint) = when (step.status) {
        BuildStepStatus.SUCCESS -> R.drawable.ic_check to colors.ok
        BuildStepStatus.RUNNING -> R.drawable.ic_refresh to colors.accent
        BuildStepStatus.PENDING -> R.drawable.ic_clock to colors.txt2
        BuildStepStatus.FAILED -> R.drawable.ic_alert to colors.err
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when (step.status) {
                        BuildStepStatus.SUCCESS -> colors.ok.copy(alpha = 0.12f)
                        BuildStepStatus.FAILED -> colors.err.copy(alpha = 0.12f)
                        else -> colors.surface2
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = iconTint
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (step.status == BuildStepStatus.PENDING) colors.txt2 else colors.txt
            )
            Text(
                text = step.detail ?: "en attente",
                fontSize = 11.sp,
                color = when (step.status) {
                    BuildStepStatus.RUNNING -> colors.accent
                    BuildStepStatus.FAILED -> colors.err
                    else -> colors.txt2
                },
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (step.status == BuildStepStatus.SUCCESS) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.ok
            )
        }
    }
}
