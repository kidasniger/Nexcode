package com.nexcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape
import com.nexcode.app.ui.theme.ShapeRadius18
import com.nexcode.app.ui.theme.ShapeRadius20

// Feuille modale basse (.sheet : rayon 20dp haut, poignée 36x4 dp)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexcodeBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    testTag: String = "nexcode_bottom_sheet",
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = NexcodeTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.testTag(testTag),
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.txt,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            // Poignée de tirage (.grab: 36x4 dp, rayon 99, fond border)
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(PillShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.txt2,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }

            content()
        }
    }
}

// Boîte de dialogue modale (.dialog : rayon 18dp, bordure, titre 16.5sp, texte 13sp)
@Composable
fun NexcodeDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    testTag: String = "nexcode_dialog"
) {
    val colors = NexcodeTheme.colors

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier
                .testTag(testTag)
                .fillMaxWidth()
                .clip(ShapeRadius18)
                .border(1.dp, colors.border, ShapeRadius18),
            shape = ShapeRadius18,
            color = colors.surface,
            contentColor = colors.txt
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }

                Text(
                    text = title,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.01).sp,
                    color = colors.txt,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = message,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}
