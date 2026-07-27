package com.uzbekai.tts.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uzbekai.tts.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSpeechRateChange: (Float) -> Unit,
    onVoiceVariationChange: (Float) -> Unit,
    onThemeModeChange: (String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Sozlamalar") }) }
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text("Gapirish tezligi", style = MaterialTheme.typography.titleLarge)
            Text(
                "Kichikroq qiymat = tezroq nutq, kattaroq = sekinroq",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = settings.speechRate,
                onValueChange = onSpeechRateChange,
                valueRange = 0.5f..1.5f
            )

            Column {
                Text("Ovoz xilma-xilligi", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Sintezdagi tabiiy o'zgaruvchanlik darajasi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = settings.voiceVariation,
                    onValueChange = onVoiceVariationChange,
                    valueRange = 0.1f..1.2f
                )
            }

            Text("Mavzu", style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                listOf("system" to "Tizim", "light" to "Yorug'", "dark" to "Qorong'i").forEach { (value, label) ->
                    FilterChip(
                        selected = settings.themeMode == value,
                        onClick = { onThemeModeChange(value) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}
