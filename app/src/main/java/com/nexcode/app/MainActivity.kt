package com.nexcode.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.ui.navigation.NexcodeNavHost
import com.nexcode.app.ui.theme.AppThemeMode
import com.nexcode.app.ui.theme.NexcodeTheme

class MainActivity : ComponentActivity() {
    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("NEXCODE_FATAL", "Exception non interceptee sur ${thread.name}", throwable)
        }
        enableEdgeToEdge()
        setContent {
            val prefs = remember { NexcodePreferences(applicationContext) }
            val themeString by prefs.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = NexcodePreferences.DEFAULT_THEME_MODE
            )
            val themeMode = when (themeString.uppercase()) {
                "LIGHT" -> AppThemeMode.LIGHT
                "DARK" -> AppThemeMode.DARK
                else -> AppThemeMode.SYSTEM
            }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }
            }

            NexcodeTheme(themeMode = themeMode) {
                NexcodeNavHost()
            }
        }
    }
}
