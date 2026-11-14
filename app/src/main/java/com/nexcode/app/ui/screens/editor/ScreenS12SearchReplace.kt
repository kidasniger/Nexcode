package com.nexcode.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
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

                if (!uiState.hasProject) {
                    NexcodeBanner(
                        text = "Aucun projet ouvert — la recherche parcourt les fichiers réels du projet ouvert.",
                        icon = painterResource(id = R.drawable.ic_info),
                        variant = NexcodeBannerVariant.WARNING,
                        testTag = "s12_banner_no_project"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Champ de recherche avec puce du nombre réel de résultats
                Box(modifier = Modifier.fillMaxWidth()) {
                    NexcodeTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        label = "Rechercher dans les fichiers réels",
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

                Spacer(modifier = Modifier.height(8.dp))

                // Champ de remplacement
                NexcodeTextField(
                    value = uiState.replaceQuery,
                    onValueChange = { viewModel.updateReplaceQuery(it) },
                    label = stringResource(id = R.string.s12_replace_label),
                    isMono = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "s12_input_replace"
                )

                // Puces de filtres défilables horizontalement
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                if (uiState.showReplacePreview) {
                    Spacer(modifier = Modifier.height(10.dp))
                    NexcodeBanner(
                        text = "Aperçu : ${uiState.searchQuery} → ${uiState.replaceQuery} (${uiState.totalMatchesCount} occurrence(s) réelle(s))",
                        icon = painterResource(id = R.drawable.ic_check),
                        variant = NexcodeBannerVariant.SUCCESS
                    )
                }

                // Message d'état réel de la recherche / du remplacement
                val status = uiState.searchStatusMessage
                if (status != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    NexcodeBanner(
                        text = status,
                        icon = painterResource(id = if (uiState.searchStatusIsError) R.drawable.ic_alert else R.drawable.ic_check),
                        variant = if (uiState.searchStatusIsError) NexcodeBannerVariant.ERROR else NexcodeBannerVariant.SUCCESS,
                        testTag = "s12_banner_status"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Titre de section "Résultats"
                Text(
                    text = stringResource(id = R.string.s12_results_header),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (uiState.hasProject && uiState.searchResults.isEmpty() && uiState.searchQuery.trim().length < 2) {
                    Text(
                        text = "Saisissez au moins 2 caractères pour chercher dans le contenu réel des fichiers du projet.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = colors.txt2
                    )
                }
            }

            // Cartes de résultats réels par fichier (chemins et numéros de ligne réels)
            items(uiState.searchResults) { group ->
                SearchResultFileCard(
                    group = group,
                    showPreview = uiState.showReplacePreview,
                    replaceQuery = uiState.replaceQuery
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Barre d'action inférieure de remplacement
        HorizontalDivider(thickness = 0.5.dp, color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
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
                onClick = { viewModel.toggleReplacePreview() },
                testTag = "s12_btn_preview"
            )

            NexcodeButton(
                text = stringResource(id = R.string.s12_btn_replace_format, uiState.totalMatchesCount),
                icon = painterResource(id = R.drawable.ic_check),
                variant = NexcodeButtonVariant.PRIMARY,
                size = NexcodeButtonSize.SMALL,
                enabled = uiState.hasProject && uiState.totalMatchesCount > 0,
                onClick = { viewModel.executeReplaceAll() },
                testTag = "s12_btn_replace_all"
            )
        }
    }
}

@Composable
private fun SearchResultFileCard(
    group: SearchResultFileGroup,
    showPreview: Boolean,
    replaceQuery: String
) {
    val colors = NexcodeTheme.colors

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeRadius12)
                .background(colors.surface)
                .border(1.dp, colors.border, ShapeRadius12)
        ) {
            // En-tête : chemin réel du fichier + nombre réel d'occurrences
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

            // Lignes de correspondances réelles
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
                            color = colors.txt2.copy(alpha = 0.55f),
                            modifier = Modifier.width(24.dp)
                        )

                        val matchString = buildAnnotatedString {
                            append(match.prefixText)
                            withStyle(
                                SpanStyle(
                                    background = if (showPreview) colors.ok.copy(alpha = 0.2f) else colors.accent.copy(alpha = 0.18f),
                                    color = if (showPreview) colors.ok else colors.txt,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(if (showPreview) replaceQuery else match.matchText)
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
}
