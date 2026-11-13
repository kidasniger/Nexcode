package com.nexcode.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun ScreenS03GitHub(
    viewModel: OnboardingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = NexcodeTheme.colors
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.dismissNotice()
        }
    }

    // Dialogue modal de déconnexion (m-signout)
    if (uiState.showDisconnectDialog) {
        com.nexcode.app.ui.components.modals.ModalSignOut(
            onDismiss = { viewModel.showDisconnectModal(false) },
            onConfirm = { viewModel.disconnectGitHub() }
        )
    }

    Box(
        modifier = modifier
            .testTag("screen_s03")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar avec bouton retour
            NexcodeTopBar(
                title = stringResource(id = R.string.s03_title),
                navigationIcon = painterResource(id = R.drawable.ic_back),
                onNavigationClick = onNavigateBack,
                actions = {
                    NexcodeChip(
                        text = if (uiState.isGitHubError) "Erreur" else "Défaut",
                        variant = if (uiState.isGitHubError) NexcodeChipVariant.ERROR else NexcodeChipVariant.DEFAULT,
                        onClick = { viewModel.toggleGitHubErrorState() },
                        testTag = "s03_toggle_error_chip"
                    )
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // En-tête avec tuile .tile.p (68x68 dp, rayon 20 dp, icône 30x30 dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surface2)
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_branch),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = colors.accent
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.s03_subtitle),
                        fontFamily = InterFontFamily,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt,
                        modifier = Modifier.padding(top = 14.dp)
                    )

                    Text(
                        text = stringResource(id = R.string.s03_desc),
                        fontFamily = InterFontFamily,
                        fontSize = 12.5.sp,
                        color = colors.txt2,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .widthIn(max = 280.dp)
                    )
                }

                if (!uiState.isGitHubError) {
                    // ============ VARIANTE PAR DÉFAUT (stv data-v="def") ============
                    NexcodeTextField(
                        value = uiState.gitHubToken,
                        onValueChange = { viewModel.updateGitHubToken(it) },
                        label = stringResource(id = R.string.s03_token_label),
                        placeholder = "ghp_…",
                        isMono = true,
                        visualTransformation = if (uiState.isTokenVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingContent = {
                            NexcodeIconButton(
                                icon = painterResource(id = R.drawable.ic_eye),
                                onClick = { viewModel.toggleTokenVisibility() },
                                contentDescription = "Afficher/Masquer le jeton"
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.connectWithToken() }
                        ),
                        testTag = "s03_token_input"
                    )

                    if (!uiState.isGitHubConnected && uiState.gitHubToken.isNotBlank()) {
                        Box(modifier = Modifier.padding(bottom = 12.dp)) {
                            NexcodeButton(
                                text = if (uiState.isConnectingGitHub) "Connexion…" else "Vérifier et connecter",
                                onClick = { viewModel.connectWithToken() },
                                variant = NexcodeButtonVariant.PRIMARY,
                                size = NexcodeButtonSize.SMALL,
                                enabled = !uiState.isConnectingGitHub,
                                fullWidth = true,
                                testTag = "s03_verify_token_btn"
                            )
                        }
                    }

                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(id = R.string.s03_token_hint))
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 11.5.sp,
                        color = colors.txt2,
                        lineHeight = 16.5.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
                        Text(
                            text = stringResource(id = R.string.s03_or),
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt2,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
                    }

                    NexcodeButton(
                        text = stringResource(id = R.string.s03_device_code_btn),
                        onClick = { viewModel.showDeviceCodeNotice() },
                        variant = NexcodeButtonVariant.SECONDARY,
                        icon = painterResource(id = R.drawable.ic_qr),
                        fullWidth = true,
                        testTag = "s03_device_code_btn"
                    )

                    Text(
                        text = stringResource(id = R.string.s03_device_code_desc),
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = colors.txt2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )

                    // Bannière compte connecté
                    if (uiState.isGitHubConnected) {
                        Box(modifier = Modifier.padding(top = 20.dp)) {
                            NexcodeBanner(
                                text = "Connecté en tant que ${uiState.gitHubUsername} · ${uiState.gitHubRepoCount} dépôts accessibles",
                                icon = painterResource(id = R.drawable.ic_check_c),
                                variant = NexcodeBannerVariant.SUCCESS,
                                testTag = "s03_connected_banner"
                            )
                        }

                        Box(modifier = Modifier.padding(top = 12.dp)) {
                            NexcodeButton(
                                text = stringResource(id = R.string.s03_disconnect_btn),
                                onClick = { viewModel.showDisconnectModal(true) },
                                variant = NexcodeButtonVariant.SECONDARY,
                                size = NexcodeButtonSize.SMALL,
                                icon = painterResource(id = R.drawable.ic_logout),
                                testTag = "s03_disconnect_btn"
                            )
                        }
                    }
                } else {
                    // ============ VARIANTE ERREUR (stv data-v="err") ============
                    NexcodeBanner(
                        text = uiState.gitHubErrorMessage ?: stringResource(id = R.string.s03_error_banner),
                        icon = painterResource(id = R.drawable.ic_wifi),
                        variant = NexcodeBannerVariant.ERROR,
                        testTag = "s03_error_banner"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NexcodeTextField(
                        value = uiState.gitHubToken,
                        onValueChange = { viewModel.updateGitHubToken(it) },
                        label = stringResource(id = R.string.s03_token_label),
                        placeholder = "ghp_…",
                        isMono = true,
                        visualTransformation = if (uiState.isTokenVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingContent = {
                            NexcodeIconButton(
                                icon = painterResource(id = R.drawable.ic_eye),
                                onClick = { viewModel.toggleTokenVisibility() },
                                contentDescription = "Afficher/Masquer le jeton"
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.retryGitHubConnection() }
                        ),
                        testTag = "s03_token_err_input"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NexcodeButton(
                        text = if (uiState.isConnectingGitHub) "Connexion…" else stringResource(id = R.string.s03_retry),
                        onClick = { viewModel.retryGitHubConnection() },
                        variant = NexcodeButtonVariant.PRIMARY,
                        icon = painterResource(id = R.drawable.ic_refresh),
                        enabled = !uiState.isConnectingGitHub,
                        fullWidth = true,
                        testTag = "s03_retry_btn"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bouton fixe Continuer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                NexcodeButton(
                    text = if (uiState.isConnectingGitHub) "Vérification…" else stringResource(id = R.string.s03_continue),
                    onClick = {
                        if (!uiState.isGitHubConnected && uiState.gitHubToken.isNotBlank()) {
                            viewModel.connectWithToken { onNavigateNext() }
                        } else {
                            onNavigateNext()
                        }
                    },
                    variant = NexcodeButtonVariant.PRIMARY,
                    enabled = !uiState.isConnectingGitHub,
                    fullWidth = true,
                    testTag = "s03_continue_btn"
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 78.dp)
        )
    }
}
