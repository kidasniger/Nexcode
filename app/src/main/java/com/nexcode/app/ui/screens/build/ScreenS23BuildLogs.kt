package com.nexcode.app.ui.screens.build

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.modals.ModalBuildError
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun ScreenS23BuildLogs(
    viewModel: BuildViewModel,
    onNavigateBack: () -> Unit,
    onNavigateAiChat: () -> Unit,
    onNavigateBuildConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    var showBuildErrorModal by remember { mutableStateOf(false) }

    val filterAll = stringResource(id = R.string.s23_chip_all)
    val filterErrors = stringResource(id = R.string.s23_chip_errors)
    val filterWarnings = stringResource(id = R.string.s23_chip_warnings)
    val filterGradle = "Gradle"

    val visibleLogs = uiState.buildLogs.filter { entry ->
        when {
            uiState.selectedLogFilter == filterAll -> true
            uiState.selectedLogFilter.lowercase().contains("erreur") -> entry.level == BuildLogLevel.ERROR
            uiState.selectedLogFilter.lowercase().startsWith("avertissement") -> entry.level == BuildLogLevel.WARNING
            uiState.selectedLogFilter == filterGradle -> entry.tag.contains("Gradle", ignoreCase = true) ||
                entry.message.contains("Gradle", ignoreCase = true)
            else -> true
        }
    }

    Box(
        modifier = modifier
            .testTag("screen_s23")
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
                    testTag = "s23_btn_back"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.s23_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )
                    Text(
                        text = when (uiState.compilationStatus) {
                            CompilationStatus.RUNNING -> "Logs en direct · run en cours"
                            CompilationStatus.FAILED -> "Logs du run échoué"
                            CompilationStatus.SUCCESS -> "Logs du run réussi"
                            CompilationStatus.CANCELLED -> "Logs du run annulé"
                            CompilationStatus.IDLE -> "Aucun run — lancez un build"
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
                // Filtres de logs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NexcodeChip(
                        text = filterAll,
                        variant = if (uiState.selectedLogFilter == filterAll) {
                            NexcodeChipVariant.ACCENT
                        } else {
                            NexcodeChipVariant.DEFAULT
                        },
                        onClick = { viewModel.setLogFilter(filterAll) },
                        testTag = "s23_chip_all"
                    )
                    NexcodeChip(
                        text = filterErrors,
                        variant = if (uiState.selectedLogFilter == filterErrors) {
                            NexcodeChipVariant.ACCENT
                        } else {
                            NexcodeChipVariant.DEFAULT
                        },
                        onClick = { viewModel.setLogFilter(filterErrors) },
                        testTag = "s23_chip_errors"
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NexcodeChip(
                        text = filterWarnings,
                        variant = if (uiState.selectedLogFilter == filterWarnings) {
                            NexcodeChipVariant.ACCENT
                        } else {
                            NexcodeChipVariant.DEFAULT
                        },
                        onClick = { viewModel.setLogFilter(filterWarnings) },
                        testTag = "s23_chip_warnings"
                    )
                    NexcodeChip(
                        text = filterGradle,
                        variant = if (uiState.selectedLogFilter == filterGradle) {
                            NexcodeChipVariant.ACCENT
                        } else {
                            NexcodeChipVariant.DEFAULT
                        },
                        onClick = { viewModel.setLogFilter(filterGradle) },
                        testTag = "s23_chip_kotlin"
                    )
                }

                // Console de sortie Gradle
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface2)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Logs du run GitHub Actions",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = colors.txt2,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${uiState.buildLogs.size} lignes",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = colors.txt2
                        )
                    }
                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    if (uiState.buildLogs.isEmpty()) {
                        Text(
                            text = "Aucun log pour le moment. Lancez un build depuis la configuration : les logs réels du run GitHub Actions apparaîtront ici.",
                            fontSize = 12.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (visibleLogs.isEmpty()) {
                        Text(
                            text = "Aucune entrée pour ce filtre.",
                            fontSize = 12.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        visibleLogs.forEachIndexed { index, entry ->
                            BuildLogLine(
                                entry = entry,
                                onClick = {
                                    if (entry.level == BuildLogLevel.ERROR) {
                                        showBuildErrorModal = true
                                    }
                                }
                            )
                            if (index < visibleLogs.lastIndex) {
                                HorizontalDivider(color = colors.border, thickness = 1.dp)
                            }
                        }
                    }
                }

                // Résumé réel selon l'état du run
                if (uiState.compilationStatus == CompilationStatus.FAILED) {
                    val errorCount = uiState.buildLogs.count { it.level == BuildLogLevel.ERROR }
                    val firstError = uiState.buildLogs.firstOrNull { it.level == BuildLogLevel.ERROR }?.message
                    NexcodeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = "Run échoué",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.err
                        )
                        Text(
                            text = firstError ?: "$errorCount ligne(s) d'erreur détectée(s) dans les logs du run.",
                            fontSize = 11.5.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NexcodeButton(
                                text = stringResource(id = R.string.s23_btn_fix_ai),
                                onClick = onNavigateAiChat,
                                variant = NexcodeButtonVariant.PRIMARY,
                                size = NexcodeButtonSize.DEFAULT,
                                icon = painterResource(id = R.drawable.ic_spark),
                                modifier = Modifier.weight(1f),
                                testTag = "s23_btn_fix_ai"
                            )
                            NexcodeButton(
                                text = stringResource(id = R.string.s23_btn_retry),
                                onClick = onNavigateBuildConfig,
                                variant = NexcodeButtonVariant.SECONDARY,
                                size = NexcodeButtonSize.DEFAULT,
                                icon = painterResource(id = R.drawable.ic_refresh),
                                modifier = Modifier.weight(1f),
                                testTag = "s23_btn_retry"
                            )
                        }
                    }
                } else if (uiState.compilationStatus == CompilationStatus.SUCCESS) {
                    NexcodeBanner(
                        text = "Run GitHub Actions terminé avec succès — APK disponible.",
                        icon = painterResource(id = R.drawable.ic_check_c),
                        variant = NexcodeBannerVariant.SUCCESS,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        testTag = "s23_banner_success"
                    )
                }
            }
        }

        if (showBuildErrorModal) {
            ModalBuildError(
                title = "Erreur de compilation",
                errorDetail = uiState.buildLogs.firstOrNull { it.level == BuildLogLevel.ERROR }?.message,
                onDismiss = { showBuildErrorModal = false },
                onNavigateLogs = { showBuildErrorModal = false },
                onNavigateAiChat = {
                    showBuildErrorModal = false
                    onNavigateAiChat()
                }
            )
        }
    }
}

@Composable
private fun BuildLogLine(
    entry: BuildLogEntry,
    onClick: () -> Unit
) {
    val colors = NexcodeTheme.colors
    val lineColor = when (entry.level) {
        BuildLogLevel.ERROR -> colors.err
        BuildLogLevel.WARNING -> colors.warn
        BuildLogLevel.INFO -> colors.txt2
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = entry.time,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.5.sp,
            color = colors.txt2,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = entry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = if (entry.level == BuildLogLevel.INFO) colors.txt else lineColor,
            modifier = Modifier.weight(1f)
        )
    }
}
