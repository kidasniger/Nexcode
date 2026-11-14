package com.nexcode.app.ui.screens.build

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeChip
import com.nexcode.app.ui.components.NexcodeChipVariant
import com.nexcode.app.ui.components.NexcodeIconButton
import com.nexcode.app.ui.components.NexcodeSwitch
import com.nexcode.app.ui.components.NexcodeTextField
import com.nexcode.app.ui.components.NexcodeTile
import com.nexcode.app.ui.components.NexcodeTileVariant
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun ScreenS21BuildConfig(
    viewModel: BuildViewModel,
    onNavigateBack: () -> Unit,
    onNavigateCompilation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .testTag("screen_s21")
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Topbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(colors.bg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NexcodeIconButton(
                    icon = painterResource(id = R.drawable.ic_back),
                    onClick = onNavigateBack,
                    contentDescription = stringResource(id = R.string.back),
                    testTag = "s21_btn_back"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.s21_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.txt
                    )
                    Text(
                        text = uiState.projectName,
                        fontSize = 12.sp,
                        color = colors.txt2
                    )
                }
            }

            // Contenu défilable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Carte version / variante
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    BuildConfigRow(
                        label = stringResource(id = R.string.s21_lab_version),
                        value = uiState.versionName,
                        testTag = "s21_value_version"
                    )
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    BuildConfigRow(
                        label = stringResource(id = R.string.s21_lab_build_number),
                        value = uiState.buildNumber,
                        testTag = "s21_value_build_number"
                    )
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 0.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.s21_lab_variant),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.txt,
                            modifier = Modifier.weight(1f)
                        )
                        NexcodeChip(
                            text = stringResource(id = R.string.s21_variant_debug),
                            variant = if (uiState.selectedVariant == BuildVariant.DEBUG) {
                                NexcodeChipVariant.ACCENT
                            } else {
                                NexcodeChipVariant.DEFAULT
                            },
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            onClick = { viewModel.setVariant(BuildVariant.DEBUG) },
                            testTag = "s21_chip_debug"
                        )
                        NexcodeChip(
                            text = stringResource(id = R.string.s21_variant_release),
                            variant = if (uiState.selectedVariant == BuildVariant.RELEASE) {
                                NexcodeChipVariant.ACCENT
                            } else {
                                NexcodeChipVariant.DEFAULT
                            },
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            onClick = { viewModel.setVariant(BuildVariant.RELEASE) },
                            testTag = "s21_chip_release"
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.s21_hint_variant),
                    fontSize = 11.5.sp,
                    color = colors.txt2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )

                // Section signature
                Text(
                    text = stringResource(id = R.string.s21_sect_signature),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt2,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NexcodeTile(
                            icon = painterResource(id = R.drawable.ic_key),
                            variant = NexcodeTileVariant.ACCENT,
                            isSmall = true,
                            testTag = "s21_tile_keystore"
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.keystoreName,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.txt
                            )
                            Text(
                                text = stringResource(id = R.string.s21_keystore_desc),
                                fontSize = 11.sp,
                                color = colors.txt2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        NexcodeChip(
                            text = stringResource(id = R.string.s21_chip_active),
                            variant = NexcodeChipVariant.SUCCESS,
                            testTag = "s21_chip_keystore_active"
                        )
                    }

                    NexcodeTextField(
                        value = uiState.keystorePassword,
                        onValueChange = { },
                        label = stringResource(id = R.string.s21_lab_keystore_password),
                        isMono = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        testTag = "s21_field_keystore_password"
                    )

                    NexcodeButton(
                        text = stringResource(id = R.string.s21_btn_generate_keystore),
                        onClick = { },
                        variant = NexcodeButtonVariant.SECONDARY,
                        size = NexcodeButtonSize.DEFAULT,
                        icon = painterResource(id = R.drawable.ic_refresh),
                        fullWidth = true,
                        modifier = Modifier.padding(top = 12.dp),
                        testTag = "s21_btn_generate_keystore"
                    )
                }

                // Section options
                Text(
                    text = stringResource(id = R.string.s21_sect_options),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt2,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                NexcodeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    BuildOptionRow(
                        title = stringResource(id = R.string.s21_opt_r8_title),
                        description = stringResource(id = R.string.s21_opt_r8_desc),
                        checked = uiState.isR8Enabled,
                        onToggle = { viewModel.toggleR8() },
                        testTag = "s21_switch_r8"
                    )
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    BuildOptionRow(
                        title = stringResource(id = R.string.s21_opt_mapping_title),
                        description = stringResource(id = R.string.s21_opt_mapping_desc),
                        checked = uiState.isExportMappingEnabled,
                        onToggle = { viewModel.toggleExportMapping() },
                        testTag = "s21_switch_mapping"
                    )
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    BuildOptionRow(
                        title = stringResource(id = R.string.s21_opt_debuggable_title),
                        description = stringResource(id = R.string.s21_opt_debuggable_desc),
                        checked = uiState.isDebuggableEnabled,
                        onToggle = { viewModel.toggleDebuggable() },
                        testTag = "s21_switch_debuggable"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                NexcodeButton(
                    text = stringResource(id = R.string.s21_btn_start_compilation),
                    onClick = {
                        viewModel.startCompilation(onNavigateCompilation)
                    },
                    variant = NexcodeButtonVariant.PRIMARY,
                    size = NexcodeButtonSize.DEFAULT,
                    icon = painterResource(id = R.drawable.ic_play),
                    fullWidth = true,
                    testTag = "s21_btn_start_compilation"
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BuildConfigRow(
    label: String,
    value: String,
    testTag: String
) {
    val colors = NexcodeTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.txt,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.txt2,
            modifier = Modifier
                .testTag(testTag)
                .defaultMinSize(minWidth = 48.dp)
        )
    }
}

@Composable
private fun BuildOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    val colors = NexcodeTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.txt
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = colors.txt2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        NexcodeSwitch(
            checked = checked,
            onCheckedChange = { onToggle() },
            testTag = testTag
        )
    }
}
