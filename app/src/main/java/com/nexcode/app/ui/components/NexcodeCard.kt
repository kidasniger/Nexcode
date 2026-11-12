package com.nexcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.nexcode.app.ui.theme.ShapeRadius12
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun NexcodeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String = "nexcode_card",
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = NexcodeTheme.colors

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .clip(ShapeRadius14)
            .background(colors.surface)
            .border(1.dp, colors.border, ShapeRadius14)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content
    )
}

// Tuile d'initiale de projet ou d'icône (.tile : 52x52 dp, rayon 14 dp)
enum class NexcodeTileVariant {
    ACCENT2,  // violet IA/Projet (accent2)
    ACCENT,   // cyan (accent)
    SURFACE   // surface2 + bordure
}

@Composable
fun NexcodeTile(
    text: String? = null,
    icon: Painter? = null,
    modifier: Modifier = Modifier,
    variant: NexcodeTileVariant = NexcodeTileVariant.ACCENT2,
    isSmall: Boolean = false,
    testTag: String = "nexcode_tile"
) {
    val colors = NexcodeTheme.colors

    val (bg, contentColor, border) = when (variant) {
        NexcodeTileVariant.ACCENT2 -> Triple(colors.accent2, colors.onAccent, null)
        NexcodeTileVariant.ACCENT -> Triple(colors.accent, colors.onAccent, null)
        NexcodeTileVariant.SURFACE -> Triple(colors.surface2, colors.accent, colors.border)
    }

    val size = if (isSmall) 40.dp else 52.dp
    val shape = if (isSmall) RoundedCornerShape(11.dp) else ShapeRadius14
    val fontSize = if (isSmall) 15.sp else 19.sp
    val iconSize = if (isSmall) 18.dp else 24.dp

    Box(
        modifier = modifier
            .testTag(testTag)
            .size(size)
            .clip(shape)
            .background(bg)
            .then(
                if (border != null) Modifier.border(1.dp, border, shape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
        } else if (text != null) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.03).sp,
                color = contentColor
            )
        }
    }
}

// Ligne de liste (.li : icône 38x38, titre 14sp, sous-titre 12sp)
@Composable
fun NexcodeListItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    testTag: String = "nexcode_list_item"
) {
    val colors = NexcodeTheme.colors
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = colors.txt2
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).sp,
                color = colors.txt
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.txt2,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        }
    }
}

// Bannière de notification / statut (.banner: ok, warn, err)
enum class NexcodeBannerVariant {
    SUCCESS,
    WARNING,
    ERROR
}

@Composable
fun NexcodeBanner(
    text: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    variant: NexcodeBannerVariant = NexcodeBannerVariant.SUCCESS,
    testTag: String = "nexcode_banner"
) {
    val colors = NexcodeTheme.colors

    val (bg, contentColor, borderColor) = when (variant) {
        NexcodeBannerVariant.SUCCESS -> Triple(
            colors.ok.copy(alpha = 0.1f),
            colors.ok,
            colors.ok.copy(alpha = 0.25f)
        )
        NexcodeBannerVariant.WARNING -> Triple(
            colors.warn.copy(alpha = 0.1f),
            colors.warn,
            colors.warn.copy(alpha = 0.25f)
        )
        NexcodeBannerVariant.ERROR -> Triple(
            colors.err.copy(alpha = 0.1f),
            colors.err,
            colors.err.copy(alpha = 0.25f)
        )
    }

    Row(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .clip(ShapeRadius12)
            .background(bg)
            .border(1.dp, borderColor, ShapeRadius12)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 1.dp),
            tint = contentColor
        )
        Text(
            text = text,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = contentColor
        )
    }
}
