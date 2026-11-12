package com.nexcode.app.ui.components.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius18

@Composable
fun ModalSignOut(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
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
                .testTag("modal_m_signout")
                .fillMaxWidth()
                .clip(ShapeRadius18)
                .border(1.dp, colors.border, ShapeRadius18),
            shape = ShapeRadius18,
            color = colors.surface,
            contentColor = colors.txt
        ) {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 14.dp)
            ) {
                // .dic orange avertissement (.12 alpha) avec icône logout 22x22
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.warn.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logout),
                        contentDescription = null,
                        tint = colors.warn,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Titre h3
                Text(
                    text = "Se déconnecter de GitHub ?",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.01).sp,
                    color = colors.txt,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Paragraphe descriptif
                Text(
                    text = "Vous ne pourrez plus pousser, tirer ni importer de dépôt avant de vous reconnecter. Vos projets locaux restent accessibles.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                // .acts boutons d'actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NexcodeButton(
                        text = "Annuler",
                        onClick = onDismiss,
                        variant = NexcodeButtonVariant.SECONDARY,
                        size = NexcodeButtonSize.SMALL,
                        testTag = "m_signout_btn_cancel"
                    )
                    NexcodeButton(
                        text = "Se déconnecter",
                        onClick = onConfirm,
                        variant = NexcodeButtonVariant.DANGER,
                        size = NexcodeButtonSize.SMALL,
                        testTag = "m_signout_btn_confirm"
                    )
                }
            }
        }
    }
}
