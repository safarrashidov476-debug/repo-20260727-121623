package com.uzbekai.tts.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.uzbekai.tts.ui.SynthesisState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    synthesisState: SynthesisState,
    onSynthesize: (String) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("O'zbek TTS") })
        }
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                label = { Text("Matnni kiriting") },
                placeholder = { Text("Masalan: Assalomu alaykum, qandaysiz?") }
            )

            Button(
                onClick = { onSynthesize(text) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank() && synthesisState !is SynthesisState.Synthesizing
            ) {
                if (synthesisState is SynthesisState.Synthesizing) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(0.dp))
                } else {
                    Text("Ovozga aylantirish")
                }
            }

            when (synthesisState) {
                is SynthesisState.Error -> {
                    Text(
                        text = synthesisState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is SynthesisState.Success -> {
                    ResultControls(file = synthesisState.file)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ResultControls(file: File) {
    val context = LocalContext.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(onClick = {
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.prepare()
            player.start()
            player.setOnCompletionListener { it.release() }
        }) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text(" Tinglash")
        }

        OutlinedButton(onClick = {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Audio faylni ulashish"))
        }) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text(" Ulashish")
        }
    }

    Text(
        text = "Fayl saqlandi: ${file.name}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
