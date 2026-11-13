package com.nexcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape
import com.nexcode.app.ui.theme.ShapeRadius12

enum class NexcodeNavTab(
    val title: String,
    val iconResId: Int,
    val testTag: String
) {
    PROJECTS("Projets", R.drawable.ic_folder, "nav_tab_projects"),
    EDITOR("Éditeur", R.drawable.ic_code, "nav_tab_editor"),
    CHAT("Chat IA", R.drawable.ic_spark, "nav_tab_chat"),
    BUILD("Build", R.drawable.ic_wrench, "nav_tab_build")
}

@Composable
fun NexcodeBottomBar(
    selectedTab: NexcodeNavTab,
    onTabSelected: (NexcodeNavTab) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "nexcode_bottom_bar"
) {
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .background(colors.bNavBg)
            .navigationBarsPadding()
            .height(74.dp)
    ) {
        // Bordure supérieure fine
        HorizontalDivider(thickness = 1.dp, color = colors.border)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Éléments de navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NexcodeNavTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val contentColor = if (isSelected) colors.accent else colors.txt2

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(ShapeRadius12)
                            .clickable { onTabSelected(tab) }
                            .testTag(tab.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconResId),
                            contentDescription = tab.title,
                            modifier = Modifier.size(22.dp),
                            tint = contentColor
                        )
                        Text(
                            text = tab.title,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.01.sp,
                            color = contentColor,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }

            // Indicateur de geste tactile (.gest: 104x4 dp, rayon 99, fond txt2 à 50% d'opacité)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 7.dp)
                    .size(width = 104.dp, height = 4.dp)
                    .clip(PillShape)
                    .background(colors.txt2.copy(alpha = 0.5f))
            )
        }
    }
}
