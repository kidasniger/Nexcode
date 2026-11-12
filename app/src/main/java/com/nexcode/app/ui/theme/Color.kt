package com.nexcode.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Jetons exacts - Thème sombre
val DarkBackground = Color(0xFF0B0F17)
val DarkSurface = Color(0xFF121826)
val DarkSurface2 = Color(0xFF1A2233)
val DarkBorder = Color(0xFF263047)
val DarkText = Color(0xFFE8EDF7)
val DarkText2 = Color(0xFF9AA7BF)
val DarkAccent = Color(0xFF3DDCFF)
val DarkAccent2 = Color(0xFF7C5CFF)
val DarkOnAccent = Color(0xFF04242E)
val DarkSuccess = Color(0xFF3DDC97)
val DarkWarning = Color(0xFFFFB84D)
val DarkError = Color(0xFFFF5C7A)
val DarkCodeBackground = Color(0xFF0E1420)

// Jetons exacts - Thème clair
val LightBackground = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFEDF1F7)
val LightBorder = Color(0xFFDDE4EF)
val LightText = Color(0xFF0B0F17)
val LightText2 = Color(0xFF5A6579)
val LightAccent = Color(0xFF0E7C9E)
val LightAccent2 = Color(0xFF6A48E8)
val LightOnAccent = Color(0xFFFFFFFF)
val LightSuccess = Color(0xFF0E8A5F)
val LightWarning = Color(0xFF9A6A1B)
val LightError = Color(0xFFC9284D)
val LightCodeBackground = Color(0xFFF0F3F9)

// Coloration syntaxique - Sombre
val DarkCodeKeyword = Color(0xFFA78BFA)
val DarkCodeString = Color(0xFF3DDC97)
val DarkCodeFunction = Color(0xFF3DDCFF)
val DarkCodeNumber = Color(0xFFFFB84D)
val DarkCodeComment = Color(0xFF5E6B85)
val DarkCodeType = Color(0xFF7CD7F0)
val DarkCodeAnnotation = Color(0xFFFFB84D)

// Coloration syntaxique - Clair
val LightCodeKeyword = Color(0xFF6D28D9)
val LightCodeString = Color(0xFF0E8A5F)
val LightCodeFunction = Color(0xFF0E7C9E)
val LightCodeNumber = Color(0xFF9A6A1B)
val LightCodeComment = Color(0xFF7B8699)
val LightCodeType = Color(0xFF155E8A)
val LightCodeAnnotation = Color(0xFF9A6A1B)

@Immutable
data class NexcodeCustomColors(
    val surface2: Color,
    val border: Color,
    val text2: Color,
    val accent2: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val codeBackground: Color,
    val codeKeyword: Color,
    val codeString: Color,
    val codeFunction: Color,
    val codeNumber: Color,
    val codeComment: Color,
    val codeType: Color,
    val codeAnnotation: Color
)

val DarkNexcodeCustomColors = NexcodeCustomColors(
    surface2 = DarkSurface2,
    border = DarkBorder,
    text2 = DarkText2,
    accent2 = DarkAccent2,
    onAccent = DarkOnAccent,
    success = DarkSuccess,
    warning = DarkWarning,
    codeBackground = DarkCodeBackground,
    codeKeyword = DarkCodeKeyword,
    codeString = DarkCodeString,
    codeFunction = DarkCodeFunction,
    codeNumber = DarkCodeNumber,
    codeComment = DarkCodeComment,
    codeType = DarkCodeType,
    codeAnnotation = DarkCodeAnnotation
)

val LightNexcodeCustomColors = NexcodeCustomColors(
    surface2 = LightSurface2,
    border = LightBorder,
    text2 = LightText2,
    accent2 = LightAccent2,
    onAccent = LightOnAccent,
    success = LightSuccess,
    warning = LightWarning,
    codeBackground = LightCodeBackground,
    codeKeyword = LightCodeKeyword,
    codeString = LightCodeString,
    codeFunction = LightCodeFunction,
    codeNumber = LightCodeNumber,
    codeComment = LightCodeComment,
    codeType = LightCodeType,
    codeAnnotation = LightCodeAnnotation
)
