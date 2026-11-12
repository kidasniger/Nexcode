package com.nexcode.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun ScreenS17ModificationReview(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag("screen_s17")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // En-tete : TopBar Revue des modifications / Proposees il y a 2 min · claude-sonnet-4.5
        NexcodeTopBar(
            title = stringResource(id = R.string.s17_title),
            subtitle = stringResource(id = R.string.s17_sub_format, uiState.modelName),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack
        )

        // Contenu defilable (.pad : padding horizontal 16px)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))

                // Carte recapitulative (.card .card-p : padding 14px 16px, margin-bottom: 10px)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius12)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_spark),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colors.accent
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.s17_summary_title),
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp,
                            color = colors.txt
                        )
                        Text(
                            text = stringResource(id = R.string.s17_summary_sub),
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Boutons d'action globale : Tout accepter / Tout refuser (gap: 8px, margin-bottom: 14px)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NexcodeButton(
                        text = stringResource(id = R.string.s17_btn_accept_all),
                        icon = painterResource(id = R.drawable.ic_check),
                        variant = NexcodeButtonVariant.PRIMARY,
                        size = NexcodeButtonSize.SMALL,
                        onClick = { viewModel.acceptAllDiffs() },
                        modifier = Modifier.weight(1f),
                        testTag = "s17_btn_accept_all"
                    )

                    NexcodeButton(
                        text = stringResource(id = R.string.s17_btn_reject_all),
                        icon = painterResource(id = R.drawable.ic_close),
                        variant = NexcodeButtonVariant.DANGER,
                        size = NexcodeButtonSize.SMALL,
                        onClick = { viewModel.rejectAllDiffs() },
                        modifier = Modifier.weight(1f),
                        testTag = "s17_btn_reject_all"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Cartes de diff par fichier (.card style="margin-bottom:10px;overflow:hidden")
            items(uiState.diffItems) { diffItem ->
                DiffFileCard(
                    diffItem = diffItem,
                    onAccept = { viewModel.acceptDiffItem(diffItem.fileName) },
                    onReject = { viewModel.rejectDiffItem(diffItem.fileName) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DiffFileCard(
    diffItem: DiffItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val colors = NexcodeTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeRadius12)
            .background(colors.surface)
            .border(1.dp, colors.border, ShapeRadius12)
    ) {
        // En-tete : Nom du fichier mono 12px gras + stats (+X -Y) et puce de statut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = diffItem.fileName,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "+${diffItem.additionsCount}",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.ok
                    )
                    Text(
                        text = "−${diffItem.deletionsCount}",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.err
                    )
                }
            }

            val chipVariant = when (diffItem.statusVariant) {
                DiffStatusVariant.SUCCESS -> NexcodeChipVariant.SUCCESS
                DiffStatusVariant.WARNING -> NexcodeChipVariant.WARNING
            }

            NexcodeChip(
                text = diffItem.statusText,
                variant = chipVariant,
                testTag = "s17_chip_${diffItem.fileName}"
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

        // Conteneur de diff (.diff font-family: JetBrains Mono, font-size: 11px, line-height: 1.6, background: var(--codebg))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.codeBg)
        ) {
            diffItem.lines.forEach { line ->
                val (lineBg, lineTextColor, sign, signColor) = when (line.type) {
                    DiffLineType.CONTEXT -> Quadruple(
                        Color.Transparent,
                        colors.txt2,
                        " ",
                        colors.txt2.copy(alpha = 0.7f)
                    )
                    DiffLineType.ADDITION -> Quadruple(
                        colors.ok.copy(alpha = 0.1f),
                        colors.ok,
                        "+",
                        colors.ok
                    )
                    DiffLineType.DELETION -> Quadruple(
                        colors.err.copy(alpha = 0.1f),
                        colors.err,
                        "−",
                        colors.err
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(lineBg)
                        .padding(horizontal = 10.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = sign,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = signColor,
                        modifier = Modifier.width(16.dp)
                    )

                    Text(
                        text = line.content,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 17.5.sp,
                        color = lineTextColor
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

        // Boutons d'action par fichier (.btn .btn-s .btn-sm Accepter / Refuser avec padding: 10px 12px)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NexcodeButton(
                text = stringResource(id = R.string.s17_btn_accept),
                icon = painterResource(id = R.drawable.ic_check),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.SMALL,
                onClick = onAccept,
                modifier = Modifier.weight(1f),
                testTag = "s17_btn_accept_${diffItem.fileName}"
            )

            NexcodeButton(
                text = stringResource(id = R.string.s17_btn_reject),
                icon = painterResource(id = R.drawable.ic_close),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.SMALL,
                onClick = onReject,
                modifier = Modifier.weight(1f),
                testTag = "s17_btn_reject_${diffItem.fileName}"
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
