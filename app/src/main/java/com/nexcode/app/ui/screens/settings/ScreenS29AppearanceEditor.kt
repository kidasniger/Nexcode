package com.nexcode.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeSwitch
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenS29AppearanceEditor(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .testTag("screen_s29")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("s29_btn_back")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Retour",
                    tint = colors.txt,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Apparence et éditeur",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            // Thème de l'interface
            Text(
                text = "Thème de l'interface",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
            )
            // Segmented control Thème interface
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface2)
                    .padding(3.dp)
                    .testTag("s29_seg_ui_theme"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val themes = listOf(
                    UiThemePreference.SYSTEM to "Système",
                    UiThemePreference.DARK to "Sombre",
                    UiThemePreference.LIGHT to "Clair"
                )
                themes.forEach { (theme, label) ->
                    val isSelected = state.uiTheme == theme
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accent else colors.surface2)
                            .clickable { viewModel.setUiTheme(theme) }
                            .padding(vertical = 8.dp)
                            .testTag("s29_theme_btn_${theme.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) colors.onAccent else colors.txt2
                        )
                    }
                }
            }

            // Éditeur de code
            Text(
                text = "Éditeur de code",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt2,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("s29_card_font_size"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Taille de police",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.decreaseFontSize() }
                                    .semantics { contentDescription = "Diminuer la taille de police" }
                                    .testTag("s29_btn_font_decrease"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .border(1.dp, colors.border, RoundedCornerShape(9.dp))
                                        .clip(RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A-",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.txt
                                    )
                                }
                            }

                            Text(
                                text = "${state.editorFontSizeSp} sp",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = colors.txt,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .testTag("s29_text_font_size")
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.increaseFontSize() }
                                    .semantics { contentDescription = "Augmenter la taille de police" }
                                    .testTag("s29_btn_font_increase"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .border(1.dp, colors.border, RoundedCornerShape(9.dp))
                                        .clip(RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A+",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.txt
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barre de pourcentage taille police (14 sp -> 47% de [10..22])
                    val fontRatio = ((state.editorFontSizeSp - 10f) / 12f).coerceIn(0.1f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.surface2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fontRatio)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.accent)
                        )
                    }
                }
            }

            // Thème de code
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Thème de code",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val codeThemes = listOf(
                        CodeThemePreference.NEX_DARK to "Nex Dark",
                        CodeThemePreference.NEX_LIGHT to "Nex Light",
                        CodeThemePreference.SOLARIZED to "Solarisé",
                        CodeThemePreference.HIGH_CONTRAST to "Haut contraste"
                    )
                    codeThemes.forEach { (ct, label) ->
                        val isSelected = state.codeTheme == ct
                        NexcodeChip(
                            text = label,
                            variant = if (isSelected) NexcodeChipVariant.ACCENT else NexcodeChipVariant.OUTLINE,
                            onClick = { viewModel.setCodeTheme(ct) },
                            modifier = Modifier.testTag("s29_chip_${ct.name.lowercase()}")
                        )
                    }
                }
            }

            // Indentation
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Indentation",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2)
                        .padding(3.dp)
                        .testTag("s29_seg_indentation"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val indents = listOf(
                        IndentationPreference.SPACES_2 to "2 espaces",
                        IndentationPreference.SPACES_4 to "4 espaces",
                        IndentationPreference.TABS to "Tabulations"
                    )
                    indents.forEach { (ind, label) ->
                        val isSelected = state.indentation == ind
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent else colors.surface2)
                                .clickable { viewModel.setIndentation(ind) }
                                .padding(vertical = 8.dp)
                                .testTag("s29_indent_btn_${ind.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) colors.onAccent else colors.txt2
                            )
                        }
                    }
                }
            }

            // Options de l'éditeur
            NexcodeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("s29_card_editor_options"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column {
                    // Option 1 : Retour à la ligne auto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { viewModel.toggleWordWrap(!state.wordWrap) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Retour à la ligne auto",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = "Repli dans les longues lignes",
                                fontSize = 11.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        NexcodeSwitch(
                            checked = state.wordWrap,
                            onCheckedChange = { viewModel.toggleWordWrap(it) },
                            testTag = "s29_switch_word_wrap"
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // Option 2 : Numéros de ligne
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { viewModel.toggleLineNumbers(!state.lineNumbers) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Numéros de ligne",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt,
                            modifier = Modifier.weight(1f)
                        )
                        NexcodeSwitch(
                            checked = state.lineNumbers,
                            onCheckedChange = { viewModel.toggleLineNumbers(it) },
                            testTag = "s29_switch_line_numbers"
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // Option 3 : Fermeture auto des parenthèses
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { viewModel.toggleAutoCloseBrackets(!state.autoCloseBrackets) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fermeture auto des parenthèses",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt,
                            modifier = Modifier.weight(1f)
                        )
                        NexcodeSwitch(
                            checked = state.autoCloseBrackets,
                            onCheckedChange = { viewModel.toggleAutoCloseBrackets(it) },
                            testTag = "s29_switch_auto_brackets"
                        )
                    }
                }
            }
        }
    }
}
