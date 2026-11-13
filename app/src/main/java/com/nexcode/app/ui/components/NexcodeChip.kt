package com.nexcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape

enum class NexcodeChipVariant {
    DEFAULT,   // .chip (surface2)
    ACCENT,    // .chip-acc (accent 0.1, bordure 0.3)
    SUCCESS,   // .chip-ok (ok 0.1, bordure 0.3)
    WARNING,   // .chip-warn (warn 0.1, bordure 0.3)
    ERROR,     // .chip-err (err 0.1, bordure 0.3)
    OUTLINED,  // .chip-out (transparent, bordure border)
    OUTLINE    // alias pour OUTLINED
}

@Composable
fun NexcodeChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: NexcodeChipVariant = NexcodeChipVariant.DEFAULT,
    icon: Painter? = null,
    showDot: Boolean = false,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    onClick: (() -> Unit)? = null,
    testTag: String = "nexcode_chip"
) {
    val colors = NexcodeTheme.colors

    val (bg, txtColor, borderColor) = when (variant) {
        NexcodeChipVariant.DEFAULT -> Triple(
            colors.surface2,
            colors.txt2,
            Color.Transparent
        )
        NexcodeChipVariant.ACCENT -> Triple(
            colors.accent.copy(alpha = 0.1f),
            colors.accent,
            colors.accent.copy(alpha = 0.3f)
        )
        NexcodeChipVariant.SUCCESS -> Triple(
            colors.ok.copy(alpha = 0.1f),
            colors.ok,
            colors.ok.copy(alpha = 0.3f)
        )
        NexcodeChipVariant.WARNING -> Triple(
            colors.warn.copy(alpha = 0.1f),
            colors.warn,
            colors.warn.copy(alpha = 0.3f)
        )
        NexcodeChipVariant.ERROR -> Triple(
            colors.err.copy(alpha = 0.1f),
            colors.err,
            colors.err.copy(alpha = 0.3f)
        )
        NexcodeChipVariant.OUTLINED, NexcodeChipVariant.OUTLINE -> Triple(
            Color.Transparent,
            colors.txt2,
            colors.border
        )
    }

    val pillModifier = Modifier
        .height(26.dp)
        .clip(PillShape)
        .background(bg)
        .then(
            if (borderColor != Color.Transparent) {
                Modifier.border(1.dp, borderColor, PillShape)
            } else Modifier
        )
        .padding(horizontal = 10.dp)

    if (onClick != null) {
        Box(
            modifier = modifier
                .testTag(testTag)
                .minimumInteractiveComponentSize()
                .clip(PillShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = pillModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (showDot) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(txtColor)
                    )
                }
                if (icon != null) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = txtColor
                    )
                }
                Text(
                    text = text,
                    fontFamily = fontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = txtColor,
                    maxLines = 1
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .testTag(testTag)
                .then(pillModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(txtColor)
                )
            }
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = txtColor
                )
            }
            Text(
                text = text,
                fontFamily = fontFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = txtColor,
                maxLines = 1
            )
        }
    }
}
