package com.uzbekai.tts

import android.app.Application
import com.uzbekai.tts.data.ModelManager
import com.uzbekai.tts.data.SettingsRepository
import com.uzbekai.tts.data.TTSEngine
import com.uzbekai.tts.data.db.AppDatabase

class UzbekTTSApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var modelManager: ModelManager
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var ttsEngine: TTSEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        modelManager = ModelManager(this)
        settingsRepository = SettingsRepository(this)
        ttsEngine = TTSEngine(this, modelManager)
    }
}
