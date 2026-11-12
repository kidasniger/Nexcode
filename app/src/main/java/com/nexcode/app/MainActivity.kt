package com.nexcode.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nexcode.app.ui.navigation.NexcodeNavHost
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
            NexcodeTheme {
                NexcodeNavHost()
            }
        }
    }
}
