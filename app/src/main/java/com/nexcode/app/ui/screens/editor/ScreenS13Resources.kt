package com.nexcode.app.ui.screens.editor

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun ScreenS13Resources(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateCorrectInSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag("screen_s13")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // TopBar S13 : Ressources / res · values + Bouton +
        NexcodeTopBar(
            title = stringResource(id = R.string.s13_title),
            subtitle = stringResource(id = R.string.s13_sub),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack,
            actions = {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Ajouter une ressource",
                    onClick = { /* Ajouter */ },
                    testTag = "s13_btn_add"
                )
            }
        )

        // Barre d'onglets (tabbar : Chaînes, Couleurs, Dimensions, Thèmes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ResourceTabButton(
                title = stringResource(id = R.string.s13_tab_strings),
                isSelected = uiState.activeResourceTab == ResourceTab.STRINGS,
                onClick = { viewModel.selectResourceTab(ResourceTab.STRINGS) },
                modifier = Modifier.weight(1f)
            )

            ResourceTabButton(
                title = stringResource(id = R.string.s13_tab_colors),
                isSelected = uiState.activeResourceTab == ResourceTab.COLORS,
                onClick = { viewModel.selectResourceTab(ResourceTab.COLORS) },
                modifier = Modifier.weight(1f)
            )

            ResourceTabButton(
                title = stringResource(id = R.string.s13_tab_dimensions),
                isSelected = uiState.activeResourceTab == ResourceTab.DIMENSIONS,
                onClick = { viewModel.selectResourceTab(ResourceTab.DIMENSIONS) },
                modifier = Modifier.weight(1f)
            )

            ResourceTabButton(
                title = stringResource(id = R.string.s13_tab_themes),
                isSelected = uiState.activeResourceTab == ResourceTab.THEMES,
                onClick = { viewModel.selectResourceTab(ResourceTab.THEMES) },
                modifier = Modifier.weight(1f)
            )
        }

        // Volet actif selon l'onglet
        when (uiState.activeResourceTab) {
            ResourceTab.STRINGS -> StringsResourcePane(
                uiState = uiState,
                onCorrectClick = onNavigateCorrectInSearch
            )
            ResourceTab.COLORS -> ColorsResourcePane(uiState = uiState)
            ResourceTab.DIMENSIONS -> DimensionsResourcePane(uiState = uiState)
            ResourceTab.THEMES -> ThemesResourcePane()
        }
    }
}

@Composable
private fun ResourceTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    val bg = if (isSelected) colors.surface else Color.Transparent
    val border = if (isSelected) colors.border else Color.Transparent
    val textColor = if (isSelected) colors.txt else colors.txt2

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun StringsResourcePane(
    uiState: EditorUiState,
    onCorrectClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Carte listant les clés/valeurs de chaînes
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeRadius12)
                    .background(colors.surface)
                    .border(1.dp, colors.border, ShapeRadius12)
            ) {
                uiState.stringResources.forEachIndexed { index, strItem ->
                    if (index > 0) {
                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strItem.key,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = colors.accent
                            )
                            Text(
                                text = strItem.value,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.txt,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        NexcodeIconButton(
                            icon = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Modifier",
                            onClick = { /* Editer */ },
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bannière d'avertissement : title_detail manquant avec bouton Corriger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeRadius12)
                    .background(colors.warn.copy(alpha = 0.1f))
                    .border(1.dp, colors.warn.copy(alpha = 0.25f), ShapeRadius12)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_alert),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.warn
                )

                Text(
                    text = stringResource(id = R.string.s13_warn_unresolved_string),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.warn,
                    modifier = Modifier.weight(1f)
                )

                NexcodeButton(
                    text = stringResource(id = R.string.s13_btn_correct),
                    variant = NexcodeButtonVariant.GHOST,
                    size = NexcodeButtonSize.SMALL,
                    onClick = onCorrectClick,
                    modifier = Modifier.height(28.dp),
                    testTag = "s13_btn_correct"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carte Traductions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeRadius12)
                    .background(colors.surface)
                    .border(1.dp, colors.border, ShapeRadius12)
                    .padding(14.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.s13_translations_title),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt
                )
                Text(
                    text = stringResource(id = R.string.s13_translations_desc),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                NexcodeButton(
                    text = stringResource(id = R.string.s13_btn_complete_ai),
                    variant = NexcodeButtonVariant.SECONDARY,
                    size = NexcodeButtonSize.SMALL,
                    onClick = { /* IA */ },
                    testTag = "s13_btn_translate_ai"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ColorsResourcePane(uiState: EditorUiState) {
    val colors = NexcodeTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeRadius12)
                    .background(colors.surface)
                    .border(1.dp, colors.border, ShapeRadius12)
            ) {
                uiState.colorResources.forEachIndexed { index, colItem ->
                    if (index > 0) {
                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pastille de couleur
                        val parsedColor = runCatching {
                            Color(android.graphics.Color.parseColor(colItem.hexColor))
                        }.getOrDefault(colors.accent)

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(parsedColor)
                                .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = colItem.key,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = colors.accent,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = colItem.hexColor,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            color = colors.txt2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NexcodeButton(
                text = stringResource(id = R.string.s13_btn_new_color),
                icon = painterResource(id = R.drawable.ic_plus),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.DEFAULT,
                onClick = { /* Nouvelle couleur */ },
                modifier = Modifier.fillMaxWidth(),
                testTag = "s13_btn_new_color"
            )
        }
    }
}

@Composable
private fun DimensionsResourcePane(uiState: EditorUiState) {
    val colors = NexcodeTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeRadius12)
                    .background(colors.surface)
                    .border(1.dp, colors.border, ShapeRadius12)
            ) {
                uiState.dimensionResources.forEachIndexed { index, dimItem ->
                    if (index > 0) {
                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dimItem.key,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = colors.accent,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = dimItem.value,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.txt
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemesResourcePane() {
    val colors = NexcodeTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeRadius12)
                .background(colors.surface)
                .border(1.dp, colors.border, ShapeRadius12)
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(id = R.string.s13_themes_title),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt
            )
            Text(
                text = stringResource(id = R.string.s13_themes_desc),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = colors.txt2,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            NexcodeButton(
                text = stringResource(id = R.string.s13_btn_open_file),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.SMALL,
                onClick = { /* Ouvrir */ },
                testTag = "s13_btn_open_themes"
            )
        }
    }
}
