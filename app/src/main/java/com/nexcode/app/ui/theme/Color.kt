package com.nexcode.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Jetons exacts - Thème sombre (CSS data-theme="dark")
val DarkBg = Color(0xFF0B0F17)
val DarkSurface = Color(0xFF121826)
val DarkSurface2 = Color(0xFF1A2233)
val DarkBorder = Color(0xFF263047)
val DarkTxt = Color(0xFFE8EDF7)
val DarkTxt2 = Color(0xFF9AA7BF)
val DarkAccent = Color(0xFF3DDCFF)
val DarkAccent2 = Color(0xFF7C5CFF)
val DarkOnAccent = Color(0xFF04242E)
val DarkOk = Color(0xFF3DDC97)
val DarkWarn = Color(0xFFFFB84D)
val DarkErr = Color(0xFFFF5C7A)
val DarkCodeBg = Color(0xFF0E1420)

// Coloration syntaxique sombre
val DarkKw = Color(0xFFA78BFA)
val DarkStr = Color(0xFF3DDC97)
val DarkFn = Color(0xFF3DDCFF)
val DarkNum = Color(0xFFFFB84D)
val DarkCmt = Color(0xFF5E6B85)
val DarkType = Color(0xFF7CD7F0)
val DarkAnn = Color(0xFFFFB84D)

// Jetons exacts - Thème clair (CSS data-theme="light")
val LightBg = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFEDF1F7)
val LightBorder = Color(0xFFDDE4EF)
val LightTxt = Color(0xFF0B0F17)
val LightTxt2 = Color(0xFF5A6579)
val LightAccent = Color(0xFF0B6D8C)
val LightAccent2 = Color(0xFF6A48E8)
val LightOnAccent = Color(0xFFFFFFFF)
val LightOk = Color(0xFF096E4B)
val LightWarn = Color(0xFF825711)
val LightErr = Color(0xFFC9284D)
val LightCodeBg = Color(0xFFF0F3F9)

// Coloration syntaxique claire
val LightKw = Color(0xFF6D28D9)
val LightStr = Color(0xFF096E4B)
val LightFn = Color(0xFF0B6D8C)
val LightNum = Color(0xFF825711)
val LightCmt = Color(0xFF5A6579)
val LightType = Color(0xFF155E8A)
val LightAnn = Color(0xFF825711)

// Éléments spécifiques du thème clair
val LightTopBarBg = LightBg
val LightBNavBg = Color(0xFFFFFFFF)

// Éléments spécifiques du thème sombre
val DarkTopBarBg = DarkBg
val DarkBNavBg = DarkSurface

@Immutable
data class CodeColors(
    val background: Color,
    val keyword: Color,
    val string: Color,
    val function: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val annotation: Color
)

val DarkCodeColors = CodeColors(
    background = DarkCodeBg,
    keyword = DarkKw,
    string = DarkStr,
    function = DarkFn,
    number = DarkNum,
    comment = DarkCmt,
    type = DarkType,
    annotation = DarkAnn
)

val LightCodeColors = CodeColors(
    background = LightCodeBg,
    keyword = LightKw,
    string = LightStr,
    function = LightFn,
    number = LightNum,
    comment = LightCmt,
    type = LightType,
    annotation = LightAnn
)

@Immutable
data class NexcodeColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val txt: Color,
    val txt2: Color,
    val accent: Color,
    val accent2: Color,
    val onAccent: Color,
    val ok: Color,
    val warn: Color,
    val err: Color,
    val topBarBg: Color,
    val bNavBg: Color,
    val codeColors: CodeColors
) {
    val codeBg: Color get() = codeColors.background
}

val DarkNexcodeColors = NexcodeColors(
    bg = DarkBg,
    surface = DarkSurface,
    surface2 = DarkSurface2,
    border = DarkBorder,
    txt = DarkTxt,
    txt2 = DarkTxt2,
    accent = DarkAccent,
    accent2 = DarkAccent2,
    onAccent = DarkOnAccent,
    ok = DarkOk,
    warn = DarkWarn,
    err = DarkErr,
    topBarBg = DarkTopBarBg,
    bNavBg = DarkBNavBg,
    codeColors = DarkCodeColors
)

val LightNexcodeColors = NexcodeColors(
    bg = LightBg,
    surface = LightSurface,
    surface2 = LightSurface2,
    border = LightBorder,
    txt = LightTxt,
    txt2 = LightTxt2,
    accent = LightAccent,
    accent2 = LightAccent2,
    onAccent = LightOnAccent,
    ok = LightOk,
    warn = LightWarn,
    err = LightErr,
    topBarBg = LightTopBarBg,
    bNavBg = LightBNavBg,
    codeColors = LightCodeColors
)
