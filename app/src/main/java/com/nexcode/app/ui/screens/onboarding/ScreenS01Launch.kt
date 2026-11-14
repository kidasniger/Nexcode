package com.nexcode.app.ui.screens.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeLogoIcon
import com.nexcode.app.ui.components.NexcodeLogoTheme
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import kotlinx.coroutines.delay

@Composable
fun ScreenS01Launch(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors

    // Animation de pulsation du logo (conforme a .launch-anim de la maquette: 2.4s)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_logo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Progression automatique vers s02 apres 2.8 secondes
    LaunchedEffect(Unit) {
        delay(2800)
        onNavigateNext()
    }

    Box(
        modifier = modifier
            .testTag("screen_s01")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // Logo anime 104x104 dp
            Box(
                modifier = Modifier
                    .padding(bottom = 26.dp)
                    .size(104.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                NexcodeLogoIcon(
                    size = 104.dp,
                    logoTheme = NexcodeLogoTheme.AUTO
                )
            }

            // Mot-symbole NEXCODE
            Text(
                text = "NEXCODE",
                fontFamily = InterFontFamily,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.32.sp, // .16em à 27px = 4.32px
                color = colors.txt,
                textAlign = TextAlign.Center
            )

            // Slogan
            Text(
                text = stringResource(id = R.string.s01_tagline),
                fontFamily = InterFontFamily,
                fontSize = 13.5.sp,
                color = colors.txt2,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Bouton Passer en bas (.btn.btn-g.btn-sm)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
        ) {
            NexcodeButton(
                text = stringResource(id = R.string.s01_skip),
                onClick = onNavigateNext,
                variant = NexcodeButtonVariant.GHOST,
                size = NexcodeButtonSize.SMALL,
                testTag = "s01_skip_btn"
            )
        }
    }
}
