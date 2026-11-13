package com.nexcode.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.nexcode.app.ui.theme.DarkBg
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexcode.app.ui.theme.InterFontFamily
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape
import com.nexcode.app.ui.theme.ShapeRadius12

@Composable
fun NexcodeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    hint: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    isMono: Boolean = false,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = "nexcode_text_field"
) {
    val colors = NexcodeTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isError -> colors.err
        isFocused -> colors.accent
        else -> colors.border
    }

    val textStyle = TextStyle(
        fontFamily = if (isMono) JetBrainsMonoFontFamily else InterFontFamily,
        fontSize = if (isMono) 13.sp else 14.5.sp,
        fontWeight = FontWeight.Normal,
        color = colors.txt
    )

    Column(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.txt2,
                letterSpacing = 0.02.sp,
                modifier = Modifier.padding(bottom = 7.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(ShapeRadius12)
                .background(colors.surface2)
                .border(1.dp, borderColor, ShapeRadius12)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingContent != null) {
                    Box(modifier = Modifier.padding(end = 10.dp)) {
                        leadingContent()
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = colors.txt2.copy(alpha = 0.7f))
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = textStyle,
                        singleLine = singleLine,
                        visualTransformation = visualTransformation,
                        cursorBrush = SolidColor(colors.accent),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions
                    )
                }

                if (trailingContent != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        trailingContent()
                    }
                }
            }
        }

        val displayHint = if (isError && !errorMessage.isNullOrEmpty()) errorMessage else hint
        if (!displayHint.isNullOrEmpty()) {
            Text(
                text = displayHint,
                fontSize = 11.5.sp,
                color = if (isError) colors.err else colors.txt2,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// Contrôle segmenté (.seg : rayon 12 dp, cible tactile >= 48 dp)
@Composable
fun NexcodeSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "nexcode_segmented_control"
) {
    val colors = NexcodeTheme.colors

    Row(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(ShapeRadius12)
            .background(colors.surface2)
            .border(1.dp, colors.border, ShapeRadius12)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            val bg = if (isSelected) colors.accent else androidx.compose.ui.graphics.Color.Transparent
            val txtColor = if (isSelected) colors.onAccent else colors.txt2

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 42.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .clickable { onOptionSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = txtColor
                )
            }
        }
    }
}

// Interrupteur à bascule (.switch : 44x26 dp visuel, zone tactile >= 48 dp)
@Composable
fun NexcodeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "nexcode_switch"
) {
    val colors = NexcodeTheme.colors
    val isDark = colors.bg == DarkBg
    val uncheckedTrack = if (isDark) colors.border else Color(0xFF8C9BB0)
    val bgColor by animateColorAsState(
        targetValue = if (checked) colors.accent else uncheckedTrack,
        label = "switch_bg"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        label = "switch_thumb"
    )

    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(PillShape)
                .background(if (enabled) bgColor else bgColor.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset, y = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = if (checked) colors.accent else colors.txt2.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
            )
        }
    }
}
