package com.nexcode.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS02Presentation(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors

    Box(
        modifier = modifier
            .testTag("screen_s02")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar avec bouton Passer à droite
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                NexcodeButton(
                    text = stringResource(id = R.string.s01_skip),
                    onClick = onNavigateNext,
                    variant = NexcodeButtonVariant.GHOST,
                    size = NexcodeButtonSize.SMALL,
                    testTag = "s02_skip_btn"
                )
            }

            // Contenu centré verticalement au-dessus du bouton fixe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 26.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                // Tuile cyan .tile.c (72x72 dp, rayon 22 dp, icone 32x32)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = colors.onAccent
                    )
                }

                // Titre
                Text(
                    text = stringResource(id = R.string.s02_title),
                    fontFamily = InterFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).sp,
                    lineHeight = 27.5.sp, // 1.25 * 22
                    color = colors.txt,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )

                // Description
                Text(
                    text = stringResource(id = R.string.s02_desc),
                    fontFamily = InterFontFamily,
                    fontSize = 13.5.sp,
                    color = colors.txt2,
                    lineHeight = 22.sp, // 1.65 * 13.5
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .widthIn(max = 290.dp)
                )

                // Carte .card.card-p
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp)
                        .clip(ShapeRadius14)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius14)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icône .li-ic.acc
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(colors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_flash),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = colors.accent
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(id = R.string.s02_card_title),
                            fontFamily = InterFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.txt
                        )
                        Text(
                            text = stringResource(id = R.string.s02_card_desc),
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }

                // Indicateurs de pagination (dots)
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 6.dp)
                            .clip(PillShape)
                            .background(colors.accent)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 6.dp)
                            .clip(PillShape)
                            .background(colors.border)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 6.dp)
                            .clip(PillShape)
                            .background(colors.border)
                    )
                }
            }
            }

            // Bas de page : Bouton Suivant et libellé d'étape
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NexcodeButton(
                    text = stringResource(id = R.string.s02_next),
                    onClick = onNavigateNext,
                    variant = NexcodeButtonVariant.PRIMARY,
                    fullWidth = true,
                    testTag = "s02_next_btn"
                )
                Text(
                    text = stringResource(id = R.string.s02_step_label),
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(top = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
