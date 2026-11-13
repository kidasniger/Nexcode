package com.nexcode.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun ScreenS16History(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag("screen_s16")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // En-tete : TopBar Historique / 18 conversations
        NexcodeTopBar(
            title = stringResource(id = R.string.s16_title),
            subtitle = stringResource(id = R.string.s16_conversations_count_format, uiState.totalConversationsCount),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack
        )

        // Contenu defilable (.pad avec padding-top: 2px, padding horizontal 16px)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))

                // Champ de recherche (.inwrap avec input height 44px, padding-left 42px)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(ShapeRadius12)
                        .background(colors.surface2)
                        .border(1.dp, colors.border, ShapeRadius12)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = colors.txt2
                        )

                        Spacer(modifier = Modifier.width(9.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.s16_search_placeholder),
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.5.sp,
                                    color = colors.txt2
                                )
                            }

                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                textStyle = TextStyle(
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.5.sp,
                                    color = colors.txt
                                ),
                                cursorBrush = SolidColor(colors.accent),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("s16_input_search")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Carte contenant la liste des conversations (.card margin-bottom: 10px)
                val filteredConversations = if (uiState.searchQuery.isBlank()) {
                    uiState.conversations
                } else {
                    uiState.conversations.filter {
                        it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.subtitle.contains(uiState.searchQuery, ignoreCase = true)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeRadius12)
                        .background(colors.surface)
                        .border(1.dp, colors.border, ShapeRadius12)
                ) {
                    if (filteredConversations.isEmpty()) {
                        Text(
                            text = if (uiState.conversations.isEmpty()) {
                                "Aucune conversation enregistrée pour ce projet."
                            } else {
                                "Aucune conversation ne correspond à « ${uiState.searchQuery} »."
                            },
                            fontFamily = InterFontFamily,
                            fontSize = 12.5.sp,
                            color = colors.txt2,
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 16.dp)
                                .testTag("s16_empty_list")
                        )
                    } else {
                        filteredConversations.forEachIndexed { index, convo ->
                            if (index > 0) {
                                HorizontalDivider(thickness = 0.5.dp, color = colors.border)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        viewModel.selectConversationFromHistory(convo.id)
                                        onNavigateChat()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 13.dp)
                                    .testTag("s16_item_${convo.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icone li-ic (acc si conversation active)
                                val isAccent = convo.chipVariant == ConversationChipVariant.ACCENT
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isAccent) colors.accent.copy(alpha = 0.15f) else colors.surface2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_spark),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isAccent) colors.accent else colors.txt2
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Titre et sous-titre
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = convo.title,
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.txt
                                    )
                                    Text(
                                        text = convo.subtitle,
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.5.sp,
                                        color = colors.txt2,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Puce de statut
                                val chipVariant = when (convo.chipVariant) {
                                    ConversationChipVariant.ACCENT -> NexcodeChipVariant.ACCENT
                                    ConversationChipVariant.SUCCESS -> NexcodeChipVariant.SUCCESS
                                    ConversationChipVariant.OUTLINED -> NexcodeChipVariant.OUTLINED
                                }

                                NexcodeChip(
                                    text = convo.chipText,
                                    variant = chipVariant,
                                    testTag = "s16_chip_${convo.id}"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bouton Nouvelle conversation (btn btn-p btn-block)
                NexcodeButton(
                    text = stringResource(id = R.string.s16_new_conversation),
                    icon = painterResource(id = R.drawable.ic_plus),
                    variant = NexcodeButtonVariant.PRIMARY,
                    size = NexcodeButtonSize.DEFAULT,
                    onClick = {
                        viewModel.startNewConversation()
                        onNavigateChat()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "s16_btn_new_conversation"
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
