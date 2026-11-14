package com.nexcode.app.ui.screens.designer

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeSwitch
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS19Properties(
    viewModel: DesignerViewModel,
    onNavigateBack: () -> Unit,
    onNavigatePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val selected = uiState.selectedComponent

    Column(
        modifier = modifier
            .testTag("screen_s19")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // En-tete : TopBar Propriétés avec bouton retour vers S18 et œil vers S20
        NexcodeTopBar(
            title = stringResource(id = R.string.s19_title),
            subtitle = selected?.name ?: "Aucun composant sélectionné",
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack,
            actions = {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_eye),
                    contentDescription = "Aperçu",
                    onClick = onNavigatePreview,
                    testTag = "s19_btn_preview"
                )
            }
        )

        if (selected == null) {
            // État honnête : aucune sélection, aucune propriété fictive affichée
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sliders),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = colors.txt2
                )
                Text(
                    text = "Aucun composant sélectionné",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = "Revenez au concepteur visuel, créez ou touchez un composant pour éditer ses propriétés réelles.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        } else {
            // Contenu défilable (.pad : padding horizontal 16px)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))

                    // Carte composant sélectionné : propriétés RÉELLES du modèle éditable
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeRadius14)
                            .background(colors.surface)
                            .border(1.dp, colors.border, ShapeRadius14)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tuile icône (.tile .p .sm : 38x38, radius 11px)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(colors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = selected.type.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colors.accent
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selected.name,
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt
                            )
                            Text(
                                text = "Type : ${selected.type.label} · X ${selected.posX} · Y ${selected.posY}",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        NexcodeSwitch(
                            checked = selected.isEnabled,
                            onCheckedChange = { viewModel.toggleComponentEnabled() },
                            testTag = "s19_switch_enabled"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 1 : Position et taille (valeurs réelles du composant)
                    SectionHeader(title = stringResource(id = R.string.s19_section_pos_size))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PropertyInputField(
                            label = stringResource(id = R.string.s19_lab_x),
                            value = selected.posX.toString(),
                            onValueChange = { viewModel.updatePosX(it) },
                            modifier = Modifier.weight(1f)
                        )
                        PropertyInputField(
                            label = stringResource(id = R.string.s19_lab_y),
                            value = selected.posY.toString(),
                            onValueChange = { viewModel.updatePosY(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.s19_lab_width),
                                fontFamily = InterFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt2,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(ShapeRadius12)
                                    .background(colors.surface2)
                                    .border(1.dp, colors.border, ShapeRadius12)
                                    .clickable { viewModel.cycleWidthMode() }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selected.widthMode,
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.txt
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_down),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.txt2
                                )
                            }
                        }

                        PropertyInputField(
                            label = stringResource(id = R.string.s19_lab_height),
                            value = selected.heightDp.toString(),
                            onValueChange = { viewModel.updateHeight(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Section 2 : Contenu texte réel (composants à texte)
                    if (selected.type.acceptsText) {
                        SectionHeader(title = "Contenu du texte")

                        PropertyInputField(
                            label = "Texte affiché dans le composant",
                            value = selected.text,
                            onValueChange = { viewModel.updateComponentText(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Section 3 : Apparence réelle
                    SectionHeader(title = stringResource(id = R.string.s19_section_appearance))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeRadius12)
                            .background(colors.surface)
                            .border(1.dp, colors.border, ShapeRadius12)
                    ) {
                        // Rayon des coins avec jauge de progression (réel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.s19_corner_radius),
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                color = colors.txt2
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface2)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((selected.cornerRadius / 25f).coerceIn(0f, 1f))
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(colors.accent)
                                    )
                                }
                                Text(
                                    text = "${selected.cornerRadius} dp",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.txt
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                        // Couleur de fond réelle, éditable
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.s19_bg_color),
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                color = colors.txt2
                            )

                            val parsedBg = runCatching {
                                Color(android.graphics.Color.parseColor(selected.bgColorHex))
                            }.getOrDefault(colors.surface)

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(parsedBg)
                                    .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(ShapeRadius12)
                                    .background(colors.surface2)
                                    .border(1.dp, colors.border, ShapeRadius12)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = selected.bgColorHex,
                                    onValueChange = { viewModel.updateBgColor(it) },
                                    textStyle = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.txt
                                    ),
                                    cursorBrush = SolidColor(colors.accent),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Boutons d'action : Dupliquer et Supprimer (opérations réelles sur la liste)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NexcodeButton(
                            text = stringResource(id = R.string.s19_btn_duplicate),
                            icon = painterResource(id = R.drawable.ic_copy),
                            variant = NexcodeButtonVariant.SECONDARY,
                            onClick = { viewModel.duplicateComponent() },
                            modifier = Modifier.weight(1f),
                            testTag = "s19_btn_duplicate"
                        )

                        NexcodeButton(
                            text = stringResource(id = R.string.s19_btn_delete),
                            icon = painterResource(id = R.drawable.ic_trash),
                            variant = NexcodeButtonVariant.DANGER,
                            onClick = {
                                viewModel.deleteComponent()
                                onNavigateBack()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "s19_btn_delete"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = NexcodeTheme.colors
    Text(
        text = title,
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = colors.txt2,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun PropertyInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    Column(modifier = modifier) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.txt2,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(ShapeRadius12)
                .background(colors.surface2)
                .border(1.dp, colors.border, ShapeRadius12)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.txt
                ),
                cursorBrush = SolidColor(colors.accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
