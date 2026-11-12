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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12
import com.nexcode.app.ui.theme.ShapeRadius14

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
        // En-tete : TopBar (height: 50dp) avec titre Concepteur visuel et icone œil vers S20
        NexcodeTopBar(
            title = stringResource(id = R.string.s18_title),
            subtitle = stringResource(id = R.string.s18_sub),
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

                // Zone du Canvas (.canvasw : flex: 1, centre avec mphone)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
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

                        // Contenu de l'application miniature (.mini-c : padding 12px)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Barre de titre miniature (.mtb)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.s18_canvas_cities),
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.txt
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_plus),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.txt2
                                )
                            }

                            // Salutation (.mini-txt : 15px, gras, margin-bottom 8px)
                            Text(
                                text = stringResource(id = R.string.s18_canvas_greeting),
                                fontFamily = InterFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.txt,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Placeholder d'image (.mini-img : height 64px, radius 10px, border dashed)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface2)
                                    .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_image),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.txt2
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bouton miniature (.mini-btn : height 36px, pilule, background accent)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.s18_canvas_refresh),
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Carte selectionnee : CARTE #3 Lyon (.selbox avec etiquette tag et poignees)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ShapeRadius12)
                                        .background(colors.surface)
                                        .border(1.5.dp, colors.accent, ShapeRadius12)
                                        .clickable { onNavigateProperties() }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = stringResource(id = R.string.s18_canvas_lyon),
                                                fontFamily = InterFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.txt
                                            )
                                            Text(
                                                text = stringResource(id = R.string.s18_canvas_lyon_sub),
                                                fontFamily = InterFontFamily,
                                                fontSize = 9.5.sp,
                                                color = colors.txt2,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = stringResource(id = R.string.s18_canvas_lyon_temp),
                                            fontFamily = InterFontFamily,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.txt
                                        )
                                    }
                                }

                                // Etiquette .tag : CARTE #3 (top: -17px)
                                Box(
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = (-8).dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.accent)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.s18_canvas_card3_tag),
                                        fontFamily = InterFontFamily,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp,
                                        color = colors.onAccent
                                    )
                                }

                                // Poignee coin superieur gauche (.hnd.tl)
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-3).dp, y = (-3).dp)
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(colors.accent)
                                        .border(1.dp, colors.bg, RoundedCornerShape(2.dp))
                                )

                                // Poignee coin inferieur droit (.hnd.br)
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Carte 2 : Paris (opacite 0.75)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(0.75f)
                                    .clip(ShapeRadius12)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, ShapeRadius12)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.s18_canvas_paris),
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.txt
                                        )
                                        Text(
                                            text = stringResource(id = R.string.s18_canvas_paris_sub),
                                            fontFamily = InterFontFamily,
                                            fontSize = 9.5.sp,
                                            color = colors.txt2,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = stringResource(id = R.string.s18_canvas_paris_temp),
                                        fontFamily = InterFontFamily,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.txt
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Carte 3 : Brest (opacite 0.75)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(0.75f)
                                    .clip(ShapeRadius12)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, ShapeRadius12)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.s18_canvas_brest),
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.txt
                                        )
                                        Text(
                                            text = stringResource(id = R.string.s18_canvas_brest_sub),
                                            fontFamily = InterFontFamily,
                                            fontSize = 9.5.sp,
                                            color = colors.txt2,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = stringResource(id = R.string.s18_canvas_brest_temp),
                                        fontFamily = InterFontFamily,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.txt
                                    )
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
                    painter = painterResource(id = R.drawable.ic_move),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.accent
                )

                Text(
                    text = stringResource(id = R.string.s18_footer_hint),
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = colors.txt2,
                    modifier = Modifier.weight(1f)
                )

                NexcodeButton(
                    text = stringResource(id = R.string.s18_btn_properties),
                    variant = NexcodeButtonVariant.PRIMARY,
                    size = NexcodeButtonSize.SMALL,
                    onClick = onNavigateProperties,
                    testTag = "s18_btn_properties"
                )
            }
        }
    }
}
