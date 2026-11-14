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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
        // TopBar S14 : Dépendances Gradle
        NexcodeTopBar(
            title = "Dépendances Gradle",
            subtitle = "Parse réel du build.gradle(.kts) du projet",
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
                // État honnête : aucun projet ouvert
                if (!uiState.hasProject) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeRadius12)
                            .background(colors.surface)
                            .border(1.dp, colors.border, ShapeRadius12)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Aucun projet ouvert — les dépendances listées ici sont lues dans le fichier build.gradle(.kts) réel du projet ouvert.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = colors.txt2
                        )
                    }
                } else {
                    // Message d'état réel (succès ou erreur de parse)
                    if (uiState.gradleSyncError != null) {
                        NexcodeBanner(
                            text = uiState.gradleSyncError.orEmpty(),
                            icon = painterResource(id = R.drawable.ic_alert),
                            variant = NexcodeBannerVariant.ERROR,
                            testTag = "s14_banner_error"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else if (uiState.gradleSyncSuccess) {
                        NexcodeBanner(
                            text = uiState.gradleSyncMessage,
                            icon = painterResource(id = R.drawable.ic_check),
                            variant = NexcodeBannerVariant.SUCCESS,
                            testTag = "s14_banner_status"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Carte des dépendances RÉELLES (aucune mention de versions disponibles ou de conflit inventée)
                    if (uiState.gradleDependencies.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeRadius12)
                                .background(colors.surface)
                                .border(1.dp, colors.border, ShapeRadius12)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = uiState.gradleSyncMessage.ifBlank {
                                    "Aucune dépendance trouvée — touchez « Recharger » pour lire le build.gradle(.kts) réel."
                                },
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = colors.txt2
                            )
                        }
                    } else {
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

                                    NexcodeChip(
                                        text = dep.configuration,
                                        variant = NexcodeChipVariant.OUTLINED,
                                        testTag = "chip_dep_config"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Champ Ajouter une dépendance : écriture réelle dans build.gradle(.kts)
                NexcodeTextField(
                    value = uiState.newDependencyInput,
                    onValueChange = { viewModel.updateDependencyInput(it) },
                    label = "Ajouter une dépendance (groupe:artefact:version)",
                    placeholder = "com.squareup.okhttp3:okhttp:4.12.0",
                    isMono = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "s14_input_dep"
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NexcodeButton(
                        text = "Ajouter",
                        icon = painterResource(id = R.drawable.ic_plus),
                        variant = NexcodeButtonVariant.SECONDARY,
                        size = NexcodeButtonSize.DEFAULT,
                        onClick = { viewModel.addGradleDependency() },
                        enabled = uiState.hasProject,
                        modifier = Modifier.weight(1f),
                        testTag = "s14_btn_add_dep"
                    )

                    NexcodeButton(
                        text = "Recharger",
                        icon = painterResource(id = R.drawable.ic_refresh),
                        variant = NexcodeButtonVariant.PRIMARY,
                        size = NexcodeButtonSize.DEFAULT,
                        onClick = { viewModel.syncGradleDependencies() },
                        enabled = uiState.hasProject,
                        modifier = Modifier.weight(1f),
                        testTag = "s14_btn_sync"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
