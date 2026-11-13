package com.nexcode.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape
import com.nexcode.app.ui.theme.ShapeRadius12

enum class NexcodeButtonVariant {
    PRIMARY,    // .btn-p (accent)
    SECONDARY,  // .btn-s (surface2 + bordure)
    GHOST,      // .btn-g (transparent + accent)
    DANGER      // .btn-d (rouge atténué + bordure err)
}

enum class NexcodeButtonSize {
    DEFAULT,    // height: 48dp, padding: 0 20dp, font: 14sp, icon: 18dp
    SMALL       // height: 36dp, padding: 0 14dp, font: 12.5sp, icon: 15dp
}

@Composable
fun NexcodeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NexcodeButtonVariant = NexcodeButtonVariant.PRIMARY,
    size: NexcodeButtonSize = NexcodeButtonSize.DEFAULT,
    icon: Painter? = null,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    testTag: String = "nexcode_button"
) {
    val colors = NexcodeTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (bg, contentColor, border) = when (variant) {
        NexcodeButtonVariant.PRIMARY -> Triple(
            colors.accent,
            colors.onAccent,
            null
        )
        NexcodeButtonVariant.SECONDARY -> Triple(
            colors.surface2,
            colors.txt,
            BorderStroke(1.dp, colors.border)
        )
        NexcodeButtonVariant.GHOST -> Triple(
            Color.Transparent,
            colors.accent,
            null
        )
        NexcodeButtonVariant.DANGER -> Triple(
            colors.err.copy(alpha = 0.12f),
            colors.err,
            BorderStroke(1.dp, colors.err.copy(alpha = 0.35f))
        )
    }

    val height = if (size == NexcodeButtonSize.DEFAULT) 48.dp else 36.dp
    val horizontalPadding = if (size == NexcodeButtonSize.DEFAULT) 20.dp else 14.dp
    val fontSize = if (size == NexcodeButtonSize.DEFAULT) 14.sp else 12.5.sp
    val iconSize = if (size == NexcodeButtonSize.DEFAULT) 18.dp else 15.dp
    val gap = if (size == NexcodeButtonSize.DEFAULT) 8.dp else 6.dp

    val alphaModifier = when {
        !enabled -> 0.4f
        isPressed -> 0.85f
        else -> 1f
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .clip(PillShape),
        shape = PillShape,
        color = bg.copy(alpha = if (variant == NexcodeButtonVariant.GHOST) 0f else bg.alpha * alphaModifier),
        contentColor = contentColor,
        border = border,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = contentColor
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.size(gap)
                )
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.01.sp,
                color = contentColor
            )
        }
    }
}

// Bouton icône (.icb : 40x40 dp, rayon 12 dp)
@Composable
fun NexcodeIconButton(
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    isAccent: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "nexcode_icon_button"
) {
    val colors = NexcodeTheme.colors
    val tint = if (isAccent) colors.accent else colors.txt

    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(ShapeRadius12)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(21.dp),
            tint = if (enabled) tint else colors.txt2.copy(alpha = 0.4f)
        )
    }
}

// Bouton flottant (.fab : hauteur 52 dp, padding 18 dp, ombre)
@Composable
fun NexcodeFab(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "nexcode_fab"
) {
    val colors = NexcodeTheme.colors

    Surface(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .height(52.dp)
            .shadow(elevation = 12.dp, shape = PillShape, ambientColor = Color.Black, spotColor = Color.Black),
        shape = PillShape,
        color = colors.accent,
        contentColor = colors.onAccent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = colors.onAccent
            )
            Text(
                text = text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onAccent
            )
        }
    }
}
