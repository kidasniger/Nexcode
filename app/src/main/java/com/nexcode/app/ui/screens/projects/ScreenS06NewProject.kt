package com.nexcode.app.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.ShapeRadius14

@Composable
fun ScreenS06NewProject(
    viewModel: ProjectsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .testTag("screen_s06")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar avec bouton retour, titre et sous-titre
            NexcodeTopBar(
                title = stringResource(id = R.string.s06_title),
                subtitle = stringResource(id = R.string.s06_subtitle),
                navigationIcon = painterResource(id = R.drawable.ic_back),
                onNavigationClick = onNavigateBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Modèle 1 : Kotlin · Compose (Recommandé)
                TemplateCard(
                    template = ProjectTemplateType.COMPOSE,
                    iconRes = R.drawable.ic_code,
                    isSelected = uiState.selectedTemplate == ProjectTemplateType.COMPOSE,
                    isAccentTile = true,
                    onClick = { viewModel.selectTemplate(ProjectTemplateType.COMPOSE) }
                )

                // Modèle 2 : Kotlin · XML
                TemplateCard(
                    template = ProjectTemplateType.XML,
                    iconRes = R.drawable.ic_layout,
                    isSelected = uiState.selectedTemplate == ProjectTemplateType.XML,
                    onClick = { viewModel.selectTemplate(ProjectTemplateType.XML) }
                )

                // Modèle 3 : Java
                TemplateCard(
                    template = ProjectTemplateType.JAVA,
                    iconRes = R.drawable.ic_file,
                    isSelected = uiState.selectedTemplate == ProjectTemplateType.JAVA,
                    onClick = { viewModel.selectTemplate(ProjectTemplateType.JAVA) }
                )

                // Modèle 4 : Flutter
                TemplateCard(
                    template = ProjectTemplateType.FLUTTER,
                    iconRes = R.drawable.ic_flash,
                    isSelected = uiState.selectedTemplate == ProjectTemplateType.FLUTTER,
                    onClick = { viewModel.selectTemplate(ProjectTemplateType.FLUTTER) }
                )

                // Modèle 5 : Projet vide
                TemplateCard(
                    template = ProjectTemplateType.EMPTY,
                    iconRes = R.drawable.ic_box,
                    isSelected = uiState.selectedTemplate == ProjectTemplateType.EMPTY,
                    onClick = { viewModel.selectTemplate(ProjectTemplateType.EMPTY) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bouton inférieur fixe Continuer avec [Modèle sélectionné]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                NexcodeButton(
                    text = stringResource(id = R.string.s06_continue_format, uiState.selectedTemplate.title),
                    onClick = onNavigateNext,
                    variant = NexcodeButtonVariant.PRIMARY,
                    fullWidth = true,
                    testTag = "s06_continue_btn"
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: ProjectTemplateType,
    iconRes: Int,
    isSelected: Boolean,
    isAccentTile: Boolean = false,
    onClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    NexcodeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, colors.accent, ShapeRadius14)
                } else Modifier
            )
            .testTag("s06_template_${template.name.lowercase()}"),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tuile .tile.c.sm ou .tile.p.sm (rayon 11dp, icone 20x20dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isAccentTile) colors.accent else colors.surface2)
                    .then(
                        if (!isAccentTile) {
                            Modifier.border(1.dp, colors.border, RoundedCornerShape(11.dp))
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isAccentTile) colors.onAccent else colors.accent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = template.title,
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )

                    if (template.isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        NexcodeChip(
                            text = stringResource(id = R.string.s06_recommended),
                            variant = NexcodeChipVariant.ACCENT
                        )
                    }
                }

                Text(
                    text = template.description,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = colors.txt2,
                    lineHeight = 16.5.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_c),
                    contentDescription = "Sélectionné",
                    modifier = Modifier.size(18.dp),
                    tint = colors.accent
                )
            }
        }
    }
}
