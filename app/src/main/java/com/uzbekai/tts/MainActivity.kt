package com.uzbekai.tts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.uzbekai.tts.data.DownloadState
import com.uzbekai.tts.ui.MainViewModel
import com.uzbekai.tts.ui.navigation.AppNavHost
import com.uzbekai.tts.ui.screens.ModelSetupScreen
import com.uzbekai.tts.ui.theme.UzbekTTSTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UzbekTTSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val downloadState by viewModel.downloadState.collectAsState()

                    if (downloadState is DownloadState.Completed) {
                        AppNavHost(viewModel = viewModel)
                    } else {
                        ModelSetupScreen(
                            downloadState = downloadState,
                            onStartDownload = { viewModel.startModelDownload() },
                            onRetry = { viewModel.startModelDownload() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as UzbekTTSApp).ttsEngine.close()
    }
}
