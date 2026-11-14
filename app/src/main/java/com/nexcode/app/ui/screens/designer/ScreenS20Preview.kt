package com.nexcode.app.ui.screens.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.nexcode.app.ui.theme.DarkAccent
import com.nexcode.app.ui.theme.DarkBg
import com.nexcode.app.ui.theme.DarkBorder
import com.nexcode.app.ui.theme.DarkOnAccent
import com.nexcode.app.ui.theme.DarkSurface
import com.nexcode.app.ui.theme.DarkSurface2
import com.nexcode.app.ui.theme.DarkTxt
import com.nexcode.app.ui.theme.DarkTxt2
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.LightAccent
import com.nexcode.app.ui.theme.LightBg
import com.nexcode.app.ui.theme.LightBorder
import com.nexcode.app.ui.theme.LightOnAccent
import com.nexcode.app.ui.theme.LightSurface
import com.nexcode.app.ui.theme.LightSurface2
import com.nexcode.app.ui.theme.LightTxt
import com.nexcode.app.ui.theme.LightTxt2
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun ScreenS20Preview(
    viewModel: DesignerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateDesigner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    // Couleurs locales pour la preview miniature (supportant l'interrupteur clair/sombre)
    val phoneBg = if (uiState.isDarkPreview) DarkBg else LightBg
    val phoneSurface = if (uiState.isDarkPreview) DarkSurface else LightSurface
    val phoneSurface2 = if (uiState.isDarkPreview) DarkSurface2 else LightSurface2
    val phoneBorder = if (uiState.isDarkPreview) DarkBorder else LightBorder
    val phoneTxt = if (uiState.isDarkPreview) DarkTxt else LightTxt
    val phoneTxt2 = if (uiState.isDarkPreview) DarkTxt2 else LightTxt2
    val phoneAccent = if (uiState.isDarkPreview) DarkAccent else LightAccent
    val phoneOnAccent = if (uiState.isDarkPreview) DarkOnAccent else LightOnAccent

    Column(
        modifier = modifier
            .testTag("screen_s20")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // En-tete : TopBar avec icone retour vers s18/precedent et icone editer vers s18
        NexcodeTopBar(
            title = stringResource(id = R.string.s20_title),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack,
            actions = {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_edit),
                    contentDescription = "Éditer dans le concepteur",
                    onClick = onNavigateDesigner,
                    testTag = "s20_btn_edit"
                )
            }
        )

        // Contenu défilable centré (.pad avec padding-top: 8px)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Bannière HONNÊTE : le plan de travail est local, aucune compilation n'existe ici
                NexcodeBanner(
                    text = "Plan de travail local — non enregistré dans le projet. Aperçu des composants tels que définis dans le concepteur.",
                    icon = painterResource(id = R.drawable.ic_info),
                    variant = NexcodeBannerVariant.WARNING,
                    testTag = "s20_banner_status"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selecteur segmenté Sombre / Clair (.seg : cible tactile >= 48 dp)
                Row(
                    modifier = Modifier
                        .width(220.dp)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(ShapeRadius12)
                        .background(colors.surface2)
                        .border(1.dp, colors.border, ShapeRadius12)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton theme sombre (lune)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (uiState.isDarkPreview) colors.surface else Color.Transparent)
                            .clickable { viewModel.togglePreviewTheme(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_moon),
                            contentDescription = "Thème sombre",
                            modifier = Modifier.size(16.dp),
                            tint = if (uiState.isDarkPreview) colors.txt else colors.txt2
                        )
                    }

                    // Bouton theme clair (soleil)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!uiState.isDarkPreview) colors.surface else Color.Transparent)
                            .clickable { viewModel.togglePreviewTheme(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sun),
                            contentDescription = "Thème clair",
                            modifier = Modifier.size(16.dp),
                            tint = if (!uiState.isDarkPreview) colors.txt else colors.txt2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Boitier smartphone : dimensions RÉELLES liées à l'appareil et à l'orientation
                val phoneWidth = uiState.phoneWidthDp.dp
                val phoneHeight = uiState.phoneHeightDp.dp
                Box(
                    modifier = Modifier
                        .width(phoneWidth)
                        .height(phoneHeight)
                        .clip(RoundedCornerShape(26.dp))
                        .background(phoneBg)
                        .border(1.dp, phoneBorder, RoundedCornerShape(26.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Encoche / haut-parleur (.mh : height 26px)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(phoneBorder)
                            )
                        }

                        // Contenu de la maquette : composants RÉELS créés par l'utilisateur
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp)
                        ) {
                            if (uiState.components.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Aucun composant à prévisualiser",
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = phoneTxt
                                    )
                                    Text(
                                        text = "Créez des composants dans le concepteur visuel : cet aperçu affiche uniquement ce que vous avez défini.",
                                        fontFamily = InterFontFamily,
                                        fontSize = 10.5.sp,
                                        lineHeight = 15.sp,
                                        color = phoneTxt2,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            } else {
                                uiState.components.forEach { component ->
                                    PreviewComponentItem(
                                        component = component,
                                        phoneTxt = phoneTxt,
                                        phoneTxt2 = phoneTxt2,
                                        phoneAccent = phoneAccent,
                                        phoneOnAccent = phoneOnAccent,
                                        phoneSurface = phoneSurface,
                                        phoneBorder = phoneBorder
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(52.dp))
                        }
                    }

                    // Barre de navigation basse miniature du telephone (height: 44px)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(phoneSurface)
                            .border(width = 0.5.dp, color = phoneBorder),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_folder), contentDescription = null, modifier = Modifier.size(18.dp), tint = phoneAccent)
                        Icon(painter = painterResource(id = R.drawable.ic_code), contentDescription = null, modifier = Modifier.size(18.dp), tint = phoneTxt2)
                        Icon(painter = painterResource(id = R.drawable.ic_spark), contentDescription = null, modifier = Modifier.size(18.dp), tint = phoneTxt2)
                        Icon(painter = painterResource(id = R.drawable.ic_wrench), contentDescription = null, modifier = Modifier.size(18.dp), tint = phoneTxt2)
                    }
                }

                // Ligne de boutons : Pivoter et appareil (dimensions réellement changées)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NexcodeButton(
                        text = stringResource(id = R.string.s20_btn_rotate),
                        icon = painterResource(id = R.drawable.ic_rotate),
                        variant = NexcodeButtonVariant.SECONDARY,
                        onClick = { viewModel.toggleOrientation() },
                        modifier = Modifier.weight(1f),
                        testTag = "s20_btn_rotate"
                    )

                    NexcodeButton(
                        text = uiState.deviceName,
                        icon = painterResource(id = R.drawable.ic_phone),
                        variant = NexcodeButtonVariant.SECONDARY,
                        onClick = { viewModel.cycleDevice() },
                        modifier = Modifier.weight(1f),
                        testTag = "s20_btn_device"
                    )
                }

                // Bouton Ouvrir dans le concepteur
                NexcodeButton(
                    text = stringResource(id = R.string.s20_btn_open_designer),
                    variant = NexcodeButtonVariant.PRIMARY,
                    size = NexcodeButtonSize.DEFAULT,
                    onClick = onNavigateDesigner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    testTag = "s20_btn_open_designer"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Rendu d'aperçu d'un composant réel du plan de travail : texte, couleur de
 * fond, rayon, hauteur, largeur et état activé proviennent tous du modèle
 * édité par l'utilisateur (aucune donnée codée en dur).
 */
@Composable
private fun PreviewComponentItem(
    component: DesignerComponent,
    phoneTxt: Color,
    phoneTxt2: Color,
    phoneAccent: Color,
    phoneOnAccent: Color,
    phoneSurface: Color,
    phoneBorder: Color
) {
    val componentBg = runCatching {
        Color(android.graphics.Color.parseColor(component.bgColorHex))
    }.getOrDefault(phoneSurface)

    val widthModifier = when (component.widthMode) {
        "160 dp" -> Modifier.width(160.dp)
        "240 dp" -> Modifier.width(240.dp)
        else -> Modifier.fillMaxWidth()
    }

    Box(
        modifier = Modifier
            .then(widthModifier)
            .heightIn(min = if (component.type == PaletteToolType.TEXT) 24.dp else component.heightDp.dp)
            .clip(RoundedCornerShape(component.cornerRadius.dp))
            .alpha(if (component.isEnabled) 1f else 0.55f)
            .background(componentBg)
            .border(1.dp, phoneBorder, RoundedCornerShape(component.cornerRadius.dp))
            .padding(10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        when (component.type) {
            PaletteToolType.TEXT -> {
                Text(
                    text = component.text.ifBlank { "(texte vide)" },
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = phoneTxt
                )
            }
            PaletteToolType.BUTTON -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(phoneAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = component.text.ifBlank { component.type.label },
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = phoneOnAccent
                    )
                }
            }
            PaletteToolType.FIELD -> {
                Text(
                    text = component.text.ifBlank { "(champ vide)" },
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = phoneTxt2
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
                        modifier = Modifier.size(24.dp),
                        tint = phoneTxt2
                    )
                }
            }
            else -> {
                Text(
                    text = component.name,
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = phoneTxt2
                )
            }
        }
    }
}
