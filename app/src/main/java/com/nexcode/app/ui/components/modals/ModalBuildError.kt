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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius18

@Composable
fun ModalBuildError(
    onDismiss: () -> Unit,
    onNavigateLogs: () -> Unit,
    onNavigateAiChat: () -> Unit,
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
                .testTag("modal_m_builderr")
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
                // .dic rouge erreur (.12 alpha) avec icône alert 22x22
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.err.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_alert),
                        contentDescription = null,
                        tint = colors.err,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Titre h3
                Text(
                    text = "La compilation a échoué",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.01).sp,
                    color = colors.txt,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Paragraphe p avec monochips
                val annotatedText = buildAnnotatedString {
                    append("2 erreurs dans ")
                    withStyle(
                        style = SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            color = colors.txt
                        )
                    ) {
                        append("MainActivity.kt")
                    }
                    append(" — référence introuvable : ")
                    withStyle(
                        style = SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            color = colors.err
                        )
                    ) {
                        append("R.string.title_detail")
                    }
                    append(". La ressource a été renommée en ")
                    withStyle(
                        style = SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            color = colors.ok
                        )
                    ) {
                        append("title_details")
                    }
                    append(".")
                }

                Text(
                    text = annotatedText,
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
                        text = "Voir les logs",
                        onClick = {
                            onDismiss()
                            onNavigateLogs()
                        },
                        variant = NexcodeButtonVariant.SECONDARY,
                        size = NexcodeButtonSize.SMALL,
                        testTag = "m_builderr_btn_logs"
                    )
                    NexcodeButton(
                        text = "Corriger avec l'IA",
                        onClick = {
                            onDismiss()
                            onNavigateAiChat()
                        },
                        variant = NexcodeButtonVariant.PRIMARY,
                        size = NexcodeButtonSize.SMALL,
                        icon = painterResource(id = R.drawable.ic_spark),
                        testTag = "m_builderr_btn_ai"
                    )
                }
            }
        }
    }
}
