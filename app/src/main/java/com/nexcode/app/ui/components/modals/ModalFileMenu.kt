package com.nexcode.app.ui.components.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun ModalFileMenu(
    onDismiss: () -> Unit,
    onNewFile: () -> Unit = onDismiss,
    onRename: () -> Unit = onDismiss,
    onDuplicate: () -> Unit = onDismiss,
    onDelete: () -> Unit = onDismiss,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .testTag("modal_m_filemenu")
                .width(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = colors.surface,
            contentColor = colors.txt,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                // Item 1 : Nouveau fichier
                PopMenuItem(
                    icon = painterResource(id = R.drawable.ic_plus),
                    title = "Nouveau fichier",
                    onClick = {
                        onDismiss()
                        onNewFile()
                    },
                    testTag = "m_filemenu_new_file"
                )

                // Item 2 : Renommer
                PopMenuItem(
                    icon = painterResource(id = R.drawable.ic_edit),
                    title = "Renommer",
                    onClick = {
                        onDismiss()
                        onRename()
                    },
                    testTag = "m_filemenu_rename"
                )

                // Item 3 : Dupliquer
                PopMenuItem(
                    icon = painterResource(id = R.drawable.ic_copy),
                    title = "Dupliquer",
                    onClick = {
                        onDismiss()
                        onDuplicate()
                    },
                    testTag = "m_filemenu_duplicate"
                )

                HorizontalDivider(
                    color = colors.border,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                )

                // Item 4 : Supprimer (.danger)
                PopMenuItem(
                    icon = painterResource(id = R.drawable.ic_trash),
                    title = "Supprimer",
                    isDanger = true,
                    onClick = {
                        onDismiss()
                        onDelete()
                    },
                    testTag = "m_filemenu_delete"
                )
            }
        }
    }
}

@Composable
private fun PopMenuItem(
    icon: Painter,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    testTag: String = "popmenu_item"
) {
    val colors = NexcodeTheme.colors
    val itemColor = if (isDanger) colors.err else colors.txt
    val iconColor = if (isDanger) colors.err else colors.txt2

    Row(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = itemColor
        )
    }
}
