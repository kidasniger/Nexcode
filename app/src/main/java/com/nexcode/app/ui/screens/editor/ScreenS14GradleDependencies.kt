package com.nexcode.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun ScreenS14GradleDependencies(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag("screen_s14")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // TopBar S14 : Dépendances Gradle / build.gradle.kts (Module :app)
        NexcodeTopBar(
            title = stringResource(id = R.string.s14_title),
            subtitle = stringResource(id = R.string.s14_sub),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack
        )

        // Contenu défilable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                // Carte des dépendances Gradle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius12)
                ) {
                    uiState.gradleDependencies.forEachIndexed { index, dep ->
                        if (index > 0) {
                            HorizontalDivider(thickness = 0.5.dp, color = colors.border)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dep.name,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    color = colors.txt
                                )
                                Text(
                                    text = dep.version,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.txt2,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            val (chipVariant, testTag) = when (dep.statusVariant) {
                                DependencyStatusVariant.UP_TO_DATE -> Pair(NexcodeChipVariant.SUCCESS, "chip_dep_ok")
                                DependencyStatusVariant.WARN_UPDATE -> Pair(NexcodeChipVariant.WARNING, "chip_dep_warn")
                                DependencyStatusVariant.CONFLICT -> Pair(NexcodeChipVariant.ERROR, "chip_dep_err")
                            }

                            NexcodeChip(
                                text = dep.statusText,
                                variant = chipVariant,
                                testTag = testTag
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.gradleSyncSuccess) {
                    NexcodeBanner(
                        text = "Gradle synchronisé — versions alignées dans build.gradle.kts.",
                        icon = painterResource(id = R.drawable.ic_check),
                        variant = NexcodeBannerVariant.SUCCESS
                    )
                } else {
                    // Bannière avertissement conflit kotlin-stdlib
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeRadius12)
                            .background(colors.warn.copy(alpha = 0.1f))
                            .border(1.dp, colors.warn.copy(alpha = 0.25f), ShapeRadius12)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_alert),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).padding(top = 1.dp),
                            tint = colors.warn
                        )

                        Text(
                            text = stringResource(id = R.string.s14_warn_kotlin_conflict),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = colors.warn
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Champ Ajouter une dépendance
                NexcodeTextField(
                    value = uiState.newDependencyInput,
                    onValueChange = { viewModel.updateDependencyInput(it) },
                    label = stringResource(id = R.string.s14_add_dep_label),
                    placeholder = stringResource(id = R.string.s14_add_dep_placeholder),
                    hint = stringResource(id = R.string.s14_add_dep_hint),
                    isMono = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "s14_input_dep"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton Synchroniser Gradle
                NexcodeButton(
                    text = stringResource(id = R.string.s14_btn_sync_gradle),
                    icon = painterResource(id = R.drawable.ic_refresh),
                    variant = NexcodeButtonVariant.PRIMARY,
                    size = NexcodeButtonSize.DEFAULT,
                    onClick = { viewModel.syncGradleDependencies() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "s14_btn_sync"
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
