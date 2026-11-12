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

    Column(
        modifier = modifier
            .testTag("screen_s19")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // En-tete : TopBar Proprietes avec bouton retour vers S18 et œil vers S20
        NexcodeTopBar(
            title = stringResource(id = R.string.s19_title),
            subtitle = stringResource(id = R.string.s19_sub),
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

        // Contenu defilable (.pad : padding horizontal 16px)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))

                // Carte composant racine (.card .card-p : padding 14px 16px, margin-bottom 12px)
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
                    // Tuile icone (.tile .p .sm : 38x38, radius 11px)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_layout),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colors.accent
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.componentName,
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.txt
                        )
                        Text(
                            text = uiState.componentSubtitle,
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Interrupteur toggle (.switch .on)
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 26.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isComponentEnabled) colors.accent else colors.border)
                            .clickable { viewModel.toggleComponentEnabled() }
                            .padding(3.dp),
                        contentAlignment = if (uiState.isComponentEnabled) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isComponentEnabled) colors.onAccent else Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 1 : Position et taille
                SectionHeader(title = stringResource(id = R.string.s19_section_pos_size))

                // Grille 2 colonnes : X et Y
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PropertyInputField(
                        label = stringResource(id = R.string.s19_lab_x),
                        value = uiState.posX,
                        onValueChange = { viewModel.updatePosX(it) },
                        modifier = Modifier.weight(1f)
                    )
                    PropertyInputField(
                        label = stringResource(id = R.string.s19_lab_y),
                        value = uiState.posY,
                        onValueChange = { viewModel.updatePosY(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Grille 2 colonnes : Largeur (bouton Remplir v) et Hauteur
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
                                .height(44.dp)
                                .clip(ShapeRadius12)
                                .background(colors.surface2)
                                .border(1.dp, colors.border, ShapeRadius12)
                                .clickable { /* Menu Largeur */ }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uiState.widthMode,
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
                        value = uiState.heightVal,
                        onValueChange = { viewModel.updateHeight(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section 2 : Apparence
                SectionHeader(title = stringResource(id = R.string.s19_section_appearance))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius12)
                ) {
                    // Rayon des coins avec jauge de progression
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
                            // Barre de progression (.bar : width 90px, height 6px)
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface2)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(uiState.cornerRadiusProgress)
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(colors.accent)
                                )
                            }
                            Text(
                                text = "${uiState.cornerRadius} dp",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                    // Couleur de fond avec pastille
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.s19_bg_color),
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = colors.txt2
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                            )
                            Text(
                                text = uiState.bgColorHex,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                    // Bordure
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.s19_border),
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = colors.txt2
                        )
                        Text(
                            text = uiState.borderVal,
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.txt2
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                    // Elevation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.s19_elevation),
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = colors.txt2
                        )
                        Text(
                            text = uiState.elevationVal,
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3 : Espacements (.col3)
                SectionHeader(title = stringResource(id = R.string.s19_section_spacing))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpacingBox(value = uiState.paddingDp, label = stringResource(id = R.string.s19_padding_dp), modifier = Modifier.weight(1f))
                    SpacingBox(value = uiState.marginDp, label = stringResource(id = R.string.s19_margin_dp), modifier = Modifier.weight(1f))
                    SpacingBox(value = uiState.internalDp, label = stringResource(id = R.string.s19_internal_dp), modifier = Modifier.weight(1f))
                }

                // Section 4 : Alignement (.seg avec 3 boutons)
                SectionHeader(title = stringResource(id = R.string.s19_section_alignment))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(ShapeRadius12)
                        .background(colors.surface2)
                        .border(1.dp, colors.border, ShapeRadius12)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val icons = listOf(R.drawable.ic_lr, R.drawable.ic_list, R.drawable.ic_move)
                    icons.forEachIndexed { index, iconRes ->
                        val isSelected = uiState.alignmentIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) colors.surface else Color.Transparent)
                                .clickable { viewModel.setAlignment(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) colors.txt else colors.txt2
                            )
                        }
                    }
                }

                // Boutons d'action : Dupliquer et Supprimer
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

                Spacer(modifier = Modifier.height(10.dp))

                // Bouton generer Compose
                NexcodeButton(
                    text = stringResource(id = R.string.s19_btn_generate_compose),
                    icon = painterResource(id = R.drawable.ic_code),
                    variant = NexcodeButtonVariant.PRIMARY,
                    size = NexcodeButtonSize.DEFAULT,
                    onClick = { viewModel.generateComposeCode() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "s19_btn_generate"
                )

                Spacer(modifier = Modifier.height(24.dp))
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
                .height(44.dp)
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

@Composable
private fun SpacingBox(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    Column(
        modifier = modifier
            .clip(ShapeRadius12)
            .background(colors.surface)
            .border(1.dp, colors.border, ShapeRadius12)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.txt
        )
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 9.5.sp,
            color = colors.txt2,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
