package com.nexcode.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonSize
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun ScreenS12SearchReplace(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors

    Column(
        modifier = modifier
            .testTag("screen_s12")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // TopBar S12 : Rechercher et remplacer
        NexcodeTopBar(
            title = stringResource(id = R.string.s12_title),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            onNavigationClick = onNavigateBack
        )

        // Contenu défilable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Champ de recherche avec puce "4 résultats" à droite
                Box(modifier = Modifier.fillMaxWidth()) {
                    NexcodeTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        isMono = true,
                        trailingContent = {
                            NexcodeChip(
                                text = stringResource(id = R.string.s12_results_count_format, uiState.totalMatchesCount),
                                variant = NexcodeChipVariant.OUTLINED,
                                testTag = "s12_chip_count"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "s12_input_search"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Champ de remplacement
                NexcodeTextField(
                    value = uiState.replaceQuery,
                    onValueChange = { viewModel.updateReplaceQuery(it) },
                    label = stringResource(id = R.string.s12_replace_label),
                    isMono = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "s12_input_replace"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Puces de filtres : Respecter la casse, Regex, Projet entier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NexcodeChip(
                        text = stringResource(id = R.string.s12_chip_match_case),
                        variant = if (uiState.matchCase) NexcodeChipVariant.ACCENT else NexcodeChipVariant.OUTLINED,
                        onClick = { viewModel.toggleMatchCase() },
                        testTag = "s12_chip_match_case"
                    )

                    NexcodeChip(
                        text = stringResource(id = R.string.s12_chip_regex),
                        variant = if (uiState.regex) NexcodeChipVariant.ACCENT else NexcodeChipVariant.OUTLINED,
                        onClick = { viewModel.toggleRegex() },
                        testTag = "s12_chip_regex"
                    )

                    NexcodeChip(
                        text = stringResource(id = R.string.s12_chip_whole_project),
                        variant = if (uiState.wholeProject) NexcodeChipVariant.ACCENT else NexcodeChipVariant.OUTLINED,
                        onClick = { viewModel.toggleWholeProject() },
                        testTag = "s12_chip_whole_project"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Titre de section "Résultats"
                Text(
                    text = stringResource(id = R.string.s12_results_header),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Cartes de résultats par fichier
            items(uiState.searchResults) { group ->
                SearchResultFileCard(group = group)
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Barre d'action inférieure de remplacement
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(width = 0.5.dp, color = colors.border)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.s12_replace_all_question),
                fontSize = 12.sp,
                color = colors.txt2,
                modifier = Modifier.weight(1f)
            )

            NexcodeButton(
                text = stringResource(id = R.string.s12_btn_preview),
                variant = NexcodeButtonVariant.SECONDARY,
                size = NexcodeButtonSize.SMALL,
                onClick = { /* Prévisualisation */ },
                testTag = "s12_btn_preview"
            )

            NexcodeButton(
                text = stringResource(id = R.string.s12_btn_replace_format, uiState.totalMatchesCount),
                icon = painterResource(id = R.drawable.ic_check),
                variant = NexcodeButtonVariant.PRIMARY,
                size = NexcodeButtonSize.SMALL,
                onClick = {
                    viewModel.executeReplaceAll()
                    onNavigateBack()
                },
                testTag = "s12_btn_replace_all"
            )
        }
    }
}

@Composable
private fun SearchResultFileCard(group: SearchResultFileGroup) {
    val colors = NexcodeTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeRadius12)
            .background(colors.surface)
            .border(1.dp, colors.border, ShapeRadius12)
    ) {
        // En-tête : Nom du fichier + Nombre d'occurrences
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.fileName,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.txt,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = group.matchCount.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.txt2
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

        // Lignes de correspondances
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            group.matches.forEach { match ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${match.lineNumber} ",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = colors.txt2.copy(alpha = 0.5f),
                        modifier = Modifier.width(22.dp)
                    )

                    val matchString = buildAnnotatedString {
                        append(match.prefixText)
                        withStyle(
                            SpanStyle(
                                background = colors.accent.copy(alpha = 0.18f),
                                color = colors.txt,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(match.matchText)
                        }
                        append(match.suffixText)
                    }

                    Text(
                        text = matchString,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colors.txt2
                    )
                }
            }
        }
    }
}
