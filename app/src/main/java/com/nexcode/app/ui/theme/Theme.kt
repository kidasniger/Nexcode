package com.nexcode.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// Choix de thème : Système, Clair, Sombre
enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkSurface2,
    onPrimaryContainer = DarkTxt,
    secondary = DarkAccent2,
    onSecondary = DarkTxt,
    secondaryContainer = DarkSurface2,
    onSecondaryContainer = DarkTxt,
    background = DarkBg,
    onBackground = DarkTxt,
    surface = DarkSurface,
    onSurface = DarkTxt,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkTxt2,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkErr,
    onError = DarkBg
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightSurface2,
    onPrimaryContainer = LightTxt,
    secondary = LightAccent2,
    onSecondary = LightTxt,
    secondaryContainer = LightSurface2,
    onSecondaryContainer = LightTxt,
    background = LightBg,
    onBackground = LightTxt,
    surface = LightSurface,
    onSurface = LightTxt,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightTxt2,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = LightErr,
    onError = LightOnAccent
)

val LocalNexcodeColors = staticCompositionLocalOf { DarkNexcodeColors }
val LocalCodeColors = staticCompositionLocalOf { DarkCodeColors }
val LocalThemeMode = staticCompositionLocalOf { AppThemeMode.SYSTEM }

object NexcodeTheme {
    val colors: NexcodeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNexcodeColors.current

    val codeColors: CodeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCodeColors.current

    val spacing: NexcodeSpacing
        get() = NexcodeSpacing

    val themeMode: AppThemeMode
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeMode.current
}

@Composable
fun NexcodeTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val nexcodeColors = if (isDark) DarkNexcodeColors else LightNexcodeColors
    val codeColors = if (isDark) DarkCodeColors else LightCodeColors

    CompositionLocalProvider(
        LocalNexcodeColors provides nexcodeColors,
        LocalCodeColors provides codeColors,
        LocalThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
