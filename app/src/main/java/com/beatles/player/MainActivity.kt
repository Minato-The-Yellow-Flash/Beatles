package com.beatles.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatles.player.ui.BeatlesApp
import com.beatles.player.ui.theme.BeatlesTheme
import com.beatles.player.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<PlayerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure edge-to-edge full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Force highest display refresh rate (120Hz / 90Hz) on supported devices
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val modes = display?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.refreshRate >= 90f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            } catch (_: Exception) {}
        }

        // Request media permissions for offline downloaded songs playback
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.READ_MEDIA_AUDIO,
                        android.Manifest.permission.READ_MEDIA_VIDEO,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ),
                    1001
                )
            } else {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1001
                )
            }
        } catch (_: Throwable) {}
        
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            BeatlesTheme(themeMode = state.themeMode) {
                BeatlesApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.syncPlaybackWithService()
        } catch (_: Exception) {}
    }
}
