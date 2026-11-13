package com.nexcode.app.ui.screens.settings

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeSwitch
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun ScreenS30BackupAbout(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()
    var activeInfoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    activeInfoDialog?.let { (dialogTitle, dialogBody) ->
        AlertDialog(
            onDismissRequest = { activeInfoDialog = null },
            containerColor = colors.surface,
            titleContentColor = colors.txt,
            textContentColor = colors.txt2,
            title = {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = dialogBody,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { activeInfoDialog = null }) {
                    Text(
                        text = "Fermer",
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
            }
        )
    }

    Column(
        modifier = modifier
            .testTag("screen_s30")
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
                    .testTag("s30_btn_back")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Retour",
                    tint = colors.txt,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Sauvegarde et à propos",
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

            // Section 1 : Sauvegarde
            Text(
                text = "Sauvegarde",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
            )

            // Carte d'export
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("s30_card_export"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_db),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exporter tous les projets (.zip)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt
                            )
                            Text(
                                text = state.lastExportText,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    NexcodeButton(
                        text = "Exporter maintenant",
                        onClick = { viewModel.exportProjectsNow() },
                        variant = NexcodeButtonVariant.PRIMARY,
                        icon = painterResource(id = R.drawable.ic_download),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("s30_btn_export_now")
                    )
                }
            }

            // Carte sauvegarde automatique
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("s30_card_auto_backup"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable { viewModel.toggleAutoBackup(!state.autoBackupEnabled) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sauvegarde automatique",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt
                        )
                        Text(
                            text = "Chaque nuit à 03:00 · garde 3 versions",
                            fontSize = 11.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    NexcodeSwitch(
                        checked = state.autoBackupEnabled,
                        onCheckedChange = { viewModel.toggleAutoBackup(it) },
                        testTag = "s30_switch_auto_backup"
                    )
                }
            }

            // Section 2 : À propos
            Text(
                text = "À propos",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .testTag("s30_card_about"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column {
                    // Version
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Version",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.txt
                        )
                        Text(
                            text = state.appVersion,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = colors.txt2
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // Mises à jour
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mises à jour",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.txt
                        )
                        Text(
                            text = state.updateStatus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ok
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // Licences open source
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                activeInfoDialog = "Licences open source" to
                                    "• Jetpack Compose & Material 3 (Apache 2.0)\n• Kotlin & Coroutines (Apache 2.0)\n• Eclipse JGit (EDL 1.0)\n• Retrofit & OkHttp (Apache 2.0)\n• Sora Code Editor (LGPL 2.1)\n• Inter & JetBrains Mono (SIL OFL 1.1)"
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("s30_row_licenses"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Licences open source",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = "Gradle, Kotlin, Retrofit, Coil, Inter, JetBrains Mono…",
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

                    // Aide et assistance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                activeInfoDialog = "Aide et assistance" to
                                    "Raccourcis éditeur : glissez la barre de symboles au-dessus du clavier, touchez l'icône éclair pour corriger une erreur avec l'IA.\n\nSynchronisation Git : configurez votre jeton PAT dans Connexion GitHub pour cloner, pousser et lancer GitHub Actions."
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("s30_row_help"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aide et assistance",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = "Documentation, raccourcis, contact",
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

                    // Politique de confidentialité
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                activeInfoDialog = "Politique de confidentialité" to
                                    "Vos fichiers de projet, historiques et bases locales restent stockés sur votre appareil.\n\nVos clés API IA et jetons GitHub sont chiffrés via EncryptedSharedPreferences et Android Keystore, et ne sont transmis qu'aux fournisseurs que vous configurez."
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("s30_row_privacy"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Politique de confidentialité",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = "Aucune donnée ne quitte l'appareil sans votre accord",
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

            // Footer branding NEXCODE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = colors.txt2, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)) {
                            append("NEX")
                        }
                        withStyle(style = SpanStyle(color = colors.accent, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)) {
                            append("CODE")
                        }
                    },
                    fontSize = 12.sp
                )
                Text(
                    text = "Codez vraiment, depuis votre poche.",
                    fontSize = 10.5.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}
