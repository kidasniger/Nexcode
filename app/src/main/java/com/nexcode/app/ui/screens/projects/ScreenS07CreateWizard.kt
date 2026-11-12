package com.nexcode.app.ui.screens.projects

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeLogoIcon
import com.nexcode.app.ui.components.NexcodeLogoTheme
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS07CreateWizard(
    viewModel: ProjectsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()

    val availableSdks = listOf(24, 26, 29, 33)

    Box(
        modifier = modifier
            .testTag("screen_s07")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar avec bouton retour, titre, sous-titre et puce d'étape 1/2
            NexcodeTopBar(
                title = stringResource(id = R.string.s07_title),
                subtitle = uiState.selectedTemplate.title,
                navigationIcon = painterResource(id = R.drawable.ic_back),
                onNavigationClick = onNavigateBack,
                actions = {
                    NexcodeChip(
                        text = stringResource(id = R.string.s07_step_chip),
                        variant = NexcodeChipVariant.OUTLINE
                    )
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Barre de progression 50% (.bar avec .bar-fill a 50%)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(PillShape)
                        .background(colors.surface2)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(4.dp)
                            .clip(PillShape)
                            .background(colors.accent)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Champ 1 : Nom de l'application
                NexcodeTextField(
                    value = uiState.newAppName,
                    onValueChange = { viewModel.updateNewAppName(it) },
                    label = stringResource(id = R.string.s07_name_label),
                    testTag = "s07_app_name_input"
                )
                Text(
                    text = stringResource(id = R.string.s07_name_hint),
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Champ 2 : Nom du package (mono)
                NexcodeTextField(
                    value = uiState.newPackageName,
                    onValueChange = { viewModel.updateNewPackageName(it) },
                    label = stringResource(id = R.string.s07_package_label),
                    isMono = true,
                    testTag = "s07_package_name_input"
                )
                Row(
                    modifier = Modifier.padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = colors.txt2
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.s07_package_hint),
                        fontFamily = InterFontFamily,
                        fontSize = 11.5.sp,
                        color = colors.txt2
                    )
                }

                // Champ 3 : SDK minimum (.seg avec 24, 26, 29, 33)
                Text(
                    text = stringResource(id = R.string.s07_minsdk_label),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    availableSdks.forEach { sdk ->
                        val isSelected = sdk == uiState.selectedMinSdk
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.surface else androidx.compose.ui.graphics.Color.Transparent)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                    } else Modifier
                                )
                                .clickable { viewModel.selectMinSdk(sdk) }
                                .testTag("s07_sdk_$sdk"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sdk.toString(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.accent else colors.txt2
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(id = R.string.s07_minsdk_hint),
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                )

                // Champ 4 : Carte Icône
                Text(
                    text = stringResource(id = R.string.s07_icon_label),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Aperçu de l'icône générée 58x58 dp
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(androidx.compose.ui.graphics.Color(0xFF0B0F17))
                                .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            NexcodeLogoIcon(
                                size = 30.dp,
                                logoTheme = NexcodeLogoTheme.DARK
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.s07_icon_title),
                                fontFamily = InterFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt
                            )
                            Text(
                                text = stringResource(id = R.string.s07_icon_desc),
                                fontFamily = InterFontFamily,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        NexcodeButton(
                            text = stringResource(id = R.string.s07_change),
                            onClick = { /* Déclenche le changement d'icône */ },
                            variant = NexcodeButtonVariant.SECONDARY,
                            size = NexcodeButtonSize.SMALL,
                            testTag = "s07_change_icon_btn"
                        )
                    }
                }

                // Option 5 : Carte Pré-remplir avec l'IA (switch)
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.s07_ai_scaffold_title),
                                fontFamily = InterFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt
                            )
                            Text(
                                text = stringResource(id = R.string.s07_ai_scaffold_desc),
                                fontFamily = InterFontFamily,
                                fontSize = 11.5.sp,
                                color = colors.txt2,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Switch(
                            checked = uiState.isAiScaffoldEnabled,
                            onCheckedChange = { viewModel.toggleAiScaffold(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onAccent,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.txt2,
                                uncheckedTrackColor = colors.surface2
                            ),
                            modifier = Modifier.testTag("s07_ai_scaffold_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Bouton inférieur fixe Créer le projet avec icone flash
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                NexcodeButton(
                    text = stringResource(id = R.string.s07_create_btn),
                    onClick = {
                        viewModel.createProject {
                            onNavigateNext()
                        }
                    },
                    variant = NexcodeButtonVariant.PRIMARY,
                    icon = painterResource(id = R.drawable.ic_flash),
                    fullWidth = true,
                    testTag = "s07_create_project_btn"
                )
            }
        }
    }
}
