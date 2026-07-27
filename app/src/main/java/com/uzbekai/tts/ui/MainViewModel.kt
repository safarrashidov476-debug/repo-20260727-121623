package com.uzbekai.tts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uzbekai.tts.UzbekTTSApp
import com.uzbekai.tts.data.AppSettings
import com.uzbekai.tts.data.DownloadState
import com.uzbekai.tts.data.db.HistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class SynthesisState {
    data object Idle : SynthesisState()
    data object Synthesizing : SynthesisState()
    data class Success(val file: File) : SynthesisState()
    data class Error(val message: String) : SynthesisState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<UzbekTTSApp>()

    private val _downloadState = MutableStateFlow<DownloadState>(
        if (app.modelManager.isModelReady()) DownloadState.Completed else DownloadState.NotStarted
    )
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _synthesisState = MutableStateFlow<SynthesisState>(SynthesisState.Idle)
    val synthesisState: StateFlow<SynthesisState> = _synthesisState

    val history: StateFlow<List<HistoryEntity>> = app.database.historyDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = app.settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun startModelDownload() {
        if (app.modelManager.isModelReady()) {
            _downloadState.value = DownloadState.Completed
            return
        }
        viewModelScope.launch {
            app.modelManager.downloadModel().collect { state ->
                _downloadState.value = state
            }
        }
    }

    fun synthesize(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _synthesisState.value = SynthesisState.Synthesizing
            try {
                val currentSettings = settings.value
                val file = app.ttsEngine.synthesizeToFile(
                    text = text,
                    lengthScale = currentSettings.speechRate,
                    noiseScale = currentSettings.voiceVariation
                )
                app.database.historyDao().insert(
                    HistoryEntity(
                        text = text,
                        audioFilePath = file.absolutePath,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
                _synthesisState.value = SynthesisState.Success(file)
            } catch (e: Exception) {
                _synthesisState.value = SynthesisState.Error(e.message ?: "Ovoz yaratishda xatolik yuz berdi")
            }
        }
    }

    fun deleteHistoryItem(item: HistoryEntity) {
        viewModelScope.launch {
            app.database.historyDao().delete(item)
            File(item.audioFilePath).let { if (it.exists()) it.delete() }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            history.value.forEach { File(it.audioFilePath).let { f -> if (f.exists()) f.delete() } }
            app.database.historyDao().clearAll()
        }
    }

    fun updateSpeechRate(value: Float) {
        viewModelScope.launch { app.settingsRepository.setSpeechRate(value) }
    }

    fun updateVoiceVariation(value: Float) {
        viewModelScope.launch { app.settingsRepository.setVoiceVariation(value) }
    }

    fun updateThemeMode(value: String) {
        viewModelScope.launch { app.settingsRepository.setThemeMode(value) }
    }

    fun resetSynthesisState() {
        _synthesisState.value = SynthesisState.Idle
    }
}
