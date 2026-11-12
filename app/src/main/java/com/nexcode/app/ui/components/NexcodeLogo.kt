package com.nexcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.theme.DarkBg
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme

enum class NexcodeLogoTheme {
    DARK,        // Cyan #3DDCFF et Violet #7C5CFF
    LIGHT,       // Cyan #0E7C9E et Violet #6A48E8
    MONOCHROME,  // Tout en blanc #FFFFFF
    AUTO         // Basé sur le thème actif
}

@Composable
fun NexcodeLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    logoTheme: NexcodeLogoTheme = NexcodeLogoTheme.AUTO,
    testTag: String = "nexcode_logo_icon"
) {
    val isDark = when (logoTheme) {
        NexcodeLogoTheme.DARK -> true
        NexcodeLogoTheme.LIGHT -> false
        NexcodeLogoTheme.MONOCHROME -> null
        NexcodeLogoTheme.AUTO -> NexcodeTheme.colors.bg == DarkBg
    }

    val iconRes = when (isDark) {
        true -> R.drawable.ic_logo
        false -> R.drawable.ic_logo_light
        null -> R.drawable.ic_logo_monochrome
    }

    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = "Logo NEXCODE",
        modifier = modifier
            .testTag(testTag)
            .size(size),
        tint = Color.Unspecified
    )
}

// Mot-symbole : NEXCODE en majuscules, espacement large (letter-spacing: .14em)
@Composable
fun NexcodeWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    color: Color = NexcodeTheme.colors.txt,
    accentColor: Color = NexcodeTheme.colors.accent,
    testTag: String = "nexcode_wordmark"
) {
    val styledText = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = color,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.14.em
            )
        ) {
            append("NEX")
        }
        withStyle(
            SpanStyle(
                color = accentColor,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.14.em
            )
        ) {
            append("CODE")
        }
    }

    Text(
        text = styledText,
        fontSize = fontSize,
        modifier = modifier.testTag(testTag)
    )
}

// Composant de marque complet (logo + mot-symbole)
@Composable
fun NexcodeBrand(
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    textSize: TextUnit = 14.sp,
    logoTheme: NexcodeLogoTheme = NexcodeLogoTheme.AUTO,
    testTag: String = "nexcode_brand"
) {
    Row(
        modifier = modifier.testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NexcodeLogoIcon(size = iconSize, logoTheme = logoTheme)
        NexcodeWordmark(fontSize = textSize)
    }
}

// Écran de démarrage système / splash screen Compose
@Composable
fun NexcodeSplashScreen(
    modifier: Modifier = Modifier,
    testTag: String = "nexcode_splash_screen"
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NexcodeLogoIcon(
                size = 72.dp,
                logoTheme = NexcodeLogoTheme.DARK
            )
            NexcodeWordmark(
                fontSize = 20.sp,
                color = Color(0xFFE8EDF7),
                accentColor = Color(0xFF3DDCFF)
            )
        }
    }
}
