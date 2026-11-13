package com.nexcode.app.ui.screens.onboarding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeButton
import com.nexcode.app.ui.components.NexcodeButtonVariant
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenS04AiConfig(
    viewModel: OnboardingViewModel,
    onNavigateBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()

    // Feuille modale basse pour le choix du modèle (.sheet m-model)
    if (uiState.showModelSheet) {
        com.nexcode.app.ui.components.modals.ModalModelSelection(
            selectedModel = uiState.selectedModel,
            onModelSelected = { viewModel.selectModel(it) },
            onDismiss = { viewModel.showModelBottomSheet(false) }
        )
    }

    Box(
        modifier = modifier
            .testTag("screen_s04")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar avec titre et sous-titre
            NexcodeTopBar(
                title = stringResource(id = R.string.s04_title),
                subtitle = stringResource(id = R.string.s04_subtitle),
                navigationIcon = painterResource(id = R.drawable.ic_back),
                onNavigationClick = onNavigateBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Section Fournisseur
                Text(
                    text = stringResource(id = R.string.s04_provider_label),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.txt2,
                    letterSpacing = 0.02.sp,
                    modifier = Modifier.padding(bottom = 7.dp)
                )

                // Carte avec 3 fournisseurs
                NexcodeCard(
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    ProviderRow(
                        title = stringResource(id = R.string.s04_provider_anthropic),
                        subtitle = stringResource(id = R.string.s04_provider_anthropic_sub),
                        iconRes = R.drawable.ic_spark,
                        isSelected = uiState.selectedProvider == "Anthropic",
                        isAccentIcon = true,
                        onClick = { viewModel.selectAiProvider("Anthropic") }
                    )

                    HorizontalDivider(thickness = 1.dp, color = colors.border)

                    ProviderRow(
                        title = stringResource(id = R.string.s04_provider_openai),
                        subtitle = stringResource(id = R.string.s04_provider_openai_sub),
                        iconRes = R.drawable.ic_spark,
                        isSelected = uiState.selectedProvider == "OpenAI",
                        isAccentIcon = false,
                        onClick = { viewModel.selectAiProvider("OpenAI") }
                    )

                    HorizontalDivider(thickness = 1.dp, color = colors.border)

                    ProviderRow(
                        title = stringResource(id = R.string.s04_provider_custom),
                        subtitle = stringResource(id = R.string.s04_provider_custom_sub),
                        iconRes = R.drawable.ic_db,
                        isSelected = uiState.selectedProvider == "Serveur personnalisé",
                        isAccentIcon = false,
                        onClick = { viewModel.selectAiProvider("Serveur personnalisé") }
                    )
                }

                if (uiState.selectedProvider == "Serveur personnalisé") {
                    NexcodeTextField(
                        value = uiState.customBaseUrl,
                        onValueChange = { viewModel.updateCustomBaseUrl(it) },
                        label = "URL de l'endpoint (OpenAI / Ollama)",
                        placeholder = "http://localhost:11434/v1",
                        hint = "Adresse de base compatible /v1/chat/completions.",
                        isMono = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        testTag = "s04_custom_url_input"
                    )
                }

                // Champ Clé API
                NexcodeTextField(
                    value = uiState.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = stringResource(id = R.string.s04_key_label),
                    hint = stringResource(id = R.string.s04_key_hint),
                    isMono = true,
                    visualTransformation = if (uiState.isApiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingContent = {
                        NexcodeIconButton(
                            icon = painterResource(id = R.drawable.ic_eye),
                            onClick = { viewModel.toggleApiKeyVisibility() },
                            contentDescription = "Afficher/Masquer la clé"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    testTag = "s04_api_key_input"
                )

                // Champ Modèle
                Text(
                    text = stringResource(id = R.string.s04_model_label),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.txt2,
                    letterSpacing = 0.02.sp,
                    modifier = Modifier.padding(bottom = 7.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(PillShape)
                        .background(colors.surface2)
                        .border(1.dp, colors.border, PillShape)
                        .clickable { viewModel.showModelBottomSheet(true) }
                        .padding(horizontal = 16.dp)
                        .testTag("s04_select_model_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.selectedModel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.txt
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_down),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.txt2
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bouton Tester la connexion
                NexcodeButton(
                    text = stringResource(id = R.string.s04_test_btn),
                    onClick = { viewModel.testAiConnection() },
                    variant = NexcodeButtonVariant.SECONDARY,
                    icon = painterResource(id = R.drawable.ic_flash),
                    fullWidth = true,
                    testTag = "s04_test_ai_btn"
                )

                // Bannière résultat test IA
                if (uiState.isTestingAi) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                    }
                } else if (uiState.aiTestSuccess == true) {
                    Box(modifier = Modifier.padding(top = 12.dp)) {
                        NexcodeBanner(
                            text = uiState.aiTestMessage ?: stringResource(id = R.string.s04_test_success),
                            icon = painterResource(id = R.drawable.ic_check_c),
                            variant = NexcodeBannerVariant.SUCCESS,
                            testTag = "s04_test_success_banner"
                        )
                    }
                } else if (uiState.aiTestSuccess == false) {
                    Box(modifier = Modifier.padding(top = 12.dp)) {
                        NexcodeBanner(
                            text = uiState.aiTestMessage ?: stringResource(id = R.string.s04_test_error),
                            icon = painterResource(id = R.drawable.ic_alert),
                            variant = NexcodeBannerVariant.ERROR,
                            testTag = "s04_test_error_banner"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bouton fixe Terminer la configuration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                NexcodeButton(
                    text = stringResource(id = R.string.s04_finish),
                    onClick = {
                        viewModel.completeOnboarding()
                        onFinish()
                    },
                    variant = NexcodeButtonVariant.PRIMARY,
                    fullWidth = true,
                    testTag = "s04_finish_btn"
                )
            }
        }
    }
}

@Composable
private fun ProviderRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    isSelected: Boolean,
    isAccentIcon: Boolean,
    onClick: () -> Unit
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (isAccentIcon) colors.accent.copy(alpha = 0.12f) else colors.surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = if (isAccentIcon) colors.accent else colors.txt2
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).sp,
                color = colors.txt
            )
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = colors.txt2,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Sélecteur radio personnalisé
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) colors.accent else colors.border,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }
    }
}
