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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun ScreenS18VisualDesigner(
    viewModel: DesignerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateProperties: () -> Unit,
    onNavigatePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag("screen_s18")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // En-tete : TopBar avec titre Concepteur visuel et icône œil vers S20
        NexcodeTopBar(
            title = stringResource(id = R.string.s18_title),
            subtitle = "Plan de travail local",
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack,
            actions = {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_eye),
                    contentDescription = "Aperçu",
                    onClick = onNavigatePreview,
                    testTag = "s18_btn_preview"
                )
            }
        )

        // Bannière honnête : le plan de travail est local et n'est pas enregistré
        NexcodeBanner(
            text = "Plan de travail local — non enregistré dans le projet.",
            icon = painterResource(id = R.drawable.ic_info),
            variant = NexcodeBannerVariant.WARNING,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            testTag = "s18_banner_local_workspace"
        )

        // Zone centrale : Colonne d'outils (.palcol 88dp) + Canvas interactif (.canvasw)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Palette d'outils (.palcol : width 88px, border-right 1px solid border)
                Column(
                    modifier = Modifier
                        .width(88.dp)
                        .fillMaxSize()
                        .background(colors.surface)
                        .border(width = 0.5.dp, color = colors.border)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val paletteTools = listOf(
                        PaletteToolType.TEXT to R.string.s18_pal_text,
                        PaletteToolType.IMAGE to R.string.s18_pal_image,
                        PaletteToolType.BUTTON to R.string.s18_pal_button,
                        PaletteToolType.FIELD to R.string.s18_pal_field,
                        PaletteToolType.CARD to R.string.s18_pal_card,
                        PaletteToolType.LIST to R.string.s18_pal_list,
                        PaletteToolType.COLUMN to R.string.s18_pal_column,
                        PaletteToolType.ROW to R.string.s18_pal_row,
                        PaletteToolType.TOGGLE to R.string.s18_pal_toggle,
                        PaletteToolType.GRID to R.string.s18_pal_grid
                    )

                    paletteTools.forEach { (tool, labelRes) ->
                        val isSelected = uiState.selectedTool == tool
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.surface2 else colors.surface)
                                .clickable { viewModel.selectTool(tool) }
                                .padding(vertical = 9.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = tool.iconRes),
                                contentDescription = stringResource(id = labelRes),
                                modifier = Modifier.size(20.dp),
                                tint = colors.accent
                            )
                            Text(
                                text = stringResource(id = labelRes),
                                fontFamily = InterFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }

                // Zone du Canvas (.canvasw avec quadrillage subtil) : rendu RÉEL
                // des composants créés par l'utilisateur (aucune donnée en dur)
                val gridColor = colors.txt.copy(alpha = 0.045f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .drawBehind {
                            val stepPx = 18.dp.toPx()
                            var x = 0f
                            while (x <= size.width) {
                                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                                x += stepPx
                            }
                            var y = 0f
                            while (y <= size.height) {
                                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                                y += stepPx
                            }
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Maquette smartphone miniature (.mphone : width 236px, radius 22px)
                    Column(
                        modifier = Modifier
                            .width(236.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.bg)
                            .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                    ) {
                        // Encoche / haut-parleur (.mh : height 22px)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(colors.border)
                            )
                        }

                        // Contenu du plan de travail réel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            if (uiState.components.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Aucun composant",
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.txt
                                    )
                                    Text(
                                        text = "Choisissez un outil dans la palette puis touchez « Ajouter » pour créer un composant.",
                                        fontFamily = InterFontFamily,
                                        fontSize = 10.5.sp,
                                        lineHeight = 15.sp,
                                        color = colors.txt2,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            } else {
                                uiState.components.forEach { component ->
                                    val isSelected = component.id == uiState.selectedComponentId
                                    WorkspaceComponentItem(
                                        component = component,
                                        isSelected = isSelected,
                                        onClick = {
                                            viewModel.selectComponent(component.id)
                                            if (isSelected) onNavigateProperties()
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Pied de page du concepteur (.dsgfoot : fixe au-dessus de la nav bar)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(start = 88.dp)
                    .background(colors.surface)
                    .border(width = 0.5.dp, color = colors.border)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.accent
                )

                Text(
                    text = "Touchez un outil puis « Ajouter » · touchez un composant pour le sélectionner",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = colors.txt2,
                    modifier = Modifier.weight(1f)
                )

                NexcodeButton(
                    text = "Ajouter",
                    variant = NexcodeButtonVariant.SECONDARY,
                    size = NexcodeButtonSize.SMALL,
                    onClick = { viewModel.addComponentFromTool() },
                    enabled = uiState.selectedTool != null,
                    testTag = "s18_btn_add_component"
                )

                NexcodeButton(
                    text = stringResource(id = R.string.s18_btn_properties),
                    variant = NexcodeButtonVariant.PRIMARY,
                    size = NexcodeButtonSize.SMALL,
                    onClick = onNavigateProperties,
                    enabled = uiState.selectedComponent != null,
                    testTag = "s18_btn_properties"
                )
            }
        }
    }
}

/**
 * Rendu réel d'un composant du plan de travail : les propriétés affichées
 * (texte, couleur de fond, rayon, hauteur, largeur, état activé) viennent
 * du modèle éditable par l'utilisateur.
 */
@Composable
private fun WorkspaceComponentItem(
    component: DesignerComponent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    val componentBg = runCatching {
        Color(android.graphics.Color.parseColor(component.bgColorHex))
    }.getOrDefault(colors.surface)

    val widthModifier = when (component.widthMode) {
        "160 dp" -> Modifier.width(160.dp)
        "240 dp" -> Modifier.width(240.dp)
        else -> Modifier.fillMaxWidth() // "Remplir" / "Ajuster"
    }

    Box(
        modifier = Modifier
            .padding(top = if (isSelected) 10.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .then(widthModifier)
                .heightIn(min = if (component.type == PaletteToolType.TEXT) 24.dp else component.heightDp.dp)
                .clip(RoundedCornerShape(component.cornerRadius.dp))
                .alpha(if (component.isEnabled) 1f else 0.55f)
                .background(componentBg)
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) colors.accent else colors.border,
                    shape = RoundedCornerShape(component.cornerRadius.dp)
                )
                .clickable(onClick = onClick)
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when (component.type) {
                PaletteToolType.TEXT -> {
                    Text(
                        text = component.text.ifBlank { "(texte vide)" },
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.txt
                    )
                }
                PaletteToolType.BUTTON -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = component.text.ifBlank { component.type.label },
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onAccent
                        )
                    }
                }
                PaletteToolType.FIELD -> {
                    Text(
                        text = component.text.ifBlank { "(champ vide)" },
                        fontFamily = InterFontFamily,
                        fontSize = 11.5.sp,
                        color = colors.txt2
                    )
                }
                PaletteToolType.IMAGE -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_image),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = colors.txt2
                        )
                    }
                }
                else -> {
                    Text(
                        text = component.name,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = colors.txt2
                    )
                }
            }
        }

        // Étiquette réelle : nom du composant sélectionné
        if (isSelected) {
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = (-8).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.accent)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = component.name,
                    fontFamily = InterFontFamily,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = colors.onAccent
                )
            }

            // Poignée coin supérieur gauche
            Box(
                modifier = Modifier
                    .offset(x = (-3).dp, y = (-3).dp)
                    .size(7.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent)
                    .border(1.dp, colors.bg, RoundedCornerShape(2.dp))
            )

            // Poignée coin inférieur droit
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(7.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent)
                    .border(1.dp, colors.bg, RoundedCornerShape(2.dp))
            )
        }
    }
}
