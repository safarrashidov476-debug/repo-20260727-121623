package com.uzbekai.tts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uzbekai.tts.data.DownloadState

@Composable
fun ModelSetupScreen(
    downloadState: DownloadState,
    onStartDownload: () -> Unit,
    onRetry: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (downloadState is DownloadState.NotStarted) onStartDownload()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "O'zbekcha ovoz modeli tayyorlanmoqda",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        when (downloadState) {
            is DownloadState.NotStarted -> {
                CircularProgressIndicator()
            }
            is DownloadState.InProgress -> {
                Text(
                    text = "Yuklanmoqda: ${downloadState.progressPercent}%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { downloadState.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (downloadState.totalBytes > 0) {
                    Text(
                        text = "${downloadState.downloadedBytes / (1024 * 1024)} MB / ${downloadState.totalBytes / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            is DownloadState.Completed -> {
                Text("Model tayyor!", style = MaterialTheme.typography.bodyLarge)
            }
            is DownloadState.Failed -> {
                Text(
                    text = "Xatolik: ${downloadState.message}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("Qayta urinish")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Model bir marta yuklanadi va shundan so'ng ilova to'liq oflayn ishlaydi.",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
