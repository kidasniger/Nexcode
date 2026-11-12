package com.nexcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun NexcodeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: Painter? = null,
    onNavigationClick: (() -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    testTag: String = "nexcode_top_bar"
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .height(54.dp)
            .background(colors.topBarBg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (navigationIcon != null && onNavigationClick != null) {
            NexcodeIconButton(
                icon = navigationIcon,
                onClick = onNavigationClick,
                contentDescription = "Retour",
                testTag = "top_bar_nav_back"
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (navigationIcon == null) 8.dp else 0.dp)
        ) {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.01).sp,
                    color = colors.txt,
                    maxLines = 1
                )
            }
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = colors.txt2,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = actions
        )
    }
}
