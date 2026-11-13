package com.nexcode.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.IconButton
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
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeSwitch
import com.nexcode.app.ui.theme.NexcodeTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenS28Settings(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateAppearance: () -> Unit,
    onNavigateAiConfig: () -> Unit,
    onNavigateBackupAbout: () -> Unit,
    onNavigateGitHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()

    if (state.showSignOutDialog) {
        com.nexcode.app.ui.components.modals.ModalSignOut(
            onDismiss = { viewModel.setSignOutDialogVisible(false) },
            onConfirm = { viewModel.confirmSignOut() }
        )
    }

    Column(
        modifier = modifier
            .testTag("screen_s28")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("s28_btn_back")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Retour",
                    tint = colors.txt,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Réglages",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            // Notice utilisateur si présente
            state.userNotice?.let { notice ->
                NexcodeBanner(
                    text = notice,
                    icon = painterResource(id = R.drawable.ic_info),
                    variant = NexcodeBannerVariant.SUCCESS,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            // Section 1 : Compte
            Text(
                text = "Compte",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
            )
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("s28_card_account"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column {
                    // Ligne utilisateur
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.accountInitials,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.accountUsername,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt
                            )
                            Text(
                                text = state.accountPlan,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        NexcodeButton(
                            text = if (state.isSignedOut) "Connecter" else "Gérer",
                            onClick = onNavigateGitHub,
                            variant = NexcodeButtonVariant.SECONDARY,
                            size = NexcodeButtonSize.SMALL,
                            testTag = "s28_btn_manage_account"
                        )
                    }

                    if (!state.isSignedOut) {
                        HorizontalDivider(color = colors.border)

                        // Bouton se déconnecter
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { viewModel.setSignOutDialogVisible(true) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("s28_btn_signout"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.err.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_logout),
                                    contentDescription = null,
                                    tint = colors.err,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Se déconnecter de GitHub",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.err,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section 2 : Préférences
            Text(
                text = "Préférences",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("s28_card_preferences"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column {
                    // Apparence et éditeur -> S29
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onNavigateAppearance() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("s28_btn_go_appearance"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_palette),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Apparence et éditeur",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            val themeLabel = when (state.uiTheme) {
                                UiThemePreference.SYSTEM -> "Thème système"
                                UiThemePreference.DARK -> "Thème sombre"
                                UiThemePreference.LIGHT -> "Thème clair"
                            }
                            Text(
                                text = "$themeLabel · police ${state.editorFontSizeSp} sp",
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.ic_fwd),
                            contentDescription = null,
                            tint = colors.txt2,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // Langue (statique)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_globe),
                                contentDescription = null,
                                tint = colors.txt2,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Langue",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = state.language,
                            fontSize = 12.5.sp,
                            color = colors.txt2
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // IA · clé et modèle -> S04
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onNavigateAiConfig() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("s28_btn_go_ai_config"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_spark),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "IA · clé et modèle",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = state.aiModelSummary,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.ic_fwd),
                            contentDescription = null,
                            tint = colors.txt2,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Section 3 : Stockage
            Text(
                text = "Stockage",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("s28_card_storage"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.storageUsedText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt
                        )
                        Text(
                            text = state.storageTotalText,
                            fontSize = 12.sp,
                            color = colors.txt2
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barre de stockage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.surface2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(state.storageBarFillRatio)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(colors.accent)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chips de stockage
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NexcodeChip(
                            text = state.storageProjectsText,
                            variant = NexcodeChipVariant.OUTLINE
                        )
                        NexcodeChip(
                            text = state.storageGradleText,
                            variant = NexcodeChipVariant.OUTLINE
                        )
                        NexcodeChip(
                            text = state.storageApkText,
                            variant = NexcodeChipVariant.OUTLINE
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    NexcodeButton(
                        text = state.clearCacheButtonLabel,
                        onClick = { viewModel.clearCaches() },
                        variant = NexcodeButtonVariant.SECONDARY,
                        icon = painterResource(id = R.drawable.ic_trash),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("s28_btn_clear_caches")
                    )
                }
            }

            // Section 4 : Notifications
            Text(
                text = "Notifications",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("s28_card_notifications"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column {
                    // Notification 1 : Compilation terminée
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { viewModel.toggleNotifyBuildCompleted(!state.notifyBuildCompleted) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Compilation terminée",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = "Succès comme échec",
                                fontSize = 11.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        NexcodeSwitch(
                            checked = state.notifyBuildCompleted,
                            onCheckedChange = { viewModel.toggleNotifyBuildCompleted(it) },
                            testTag = "s28_switch_build_completed"
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // Notification 2 : Erreurs de build
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { viewModel.toggleNotifyBuildErrors(!state.notifyBuildErrors) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Erreurs de build",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = "Avec suggestion IA automatique",
                                fontSize = 11.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        NexcodeSwitch(
                            checked = state.notifyBuildErrors,
                            onCheckedChange = { viewModel.toggleNotifyBuildErrors(it) },
                            testTag = "s28_switch_build_errors"
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // Notification 3 : Mises à jour de dépendances
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { viewModel.toggleNotifyDependencyUpdates(!state.notifyDependencyUpdates) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mises à jour de dépendances",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = "Une fois par semaine",
                                fontSize = 11.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        NexcodeSwitch(
                            checked = state.notifyDependencyUpdates,
                            onCheckedChange = { viewModel.toggleNotifyDependencyUpdates(it) },
                            testTag = "s28_switch_deps_updates"
                        )
                    }
                }
            }

            // Section 5 : Application -> S30
            Text(
                text = "Application",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("s28_card_application"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable { onNavigateBackupAbout() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("s28_btn_go_backup_about"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_db),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sauvegarde et à propos",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt
                        )
                        Text(
                            text = "Export, version 1.4.2, licences",
                            fontSize = 11.5.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_fwd),
                        contentDescription = null,
                        tint = colors.txt2,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
