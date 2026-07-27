package com.uzbekai.tts.data

import android.content.Context
import com.uzbekai.tts.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

sealed class DownloadState {
    data object NotStarted : DownloadState()
    data class InProgress(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data object Completed : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

/**
 * Downloads and caches the ONNX model file in app-internal storage so it
 * only needs to be fetched once, then works fully offline afterwards.
 */
class ModelManager(private val context: Context) {

    private val client = OkHttpClient.Builder().build()

    private val modelDir: File
        get() = File(context.filesDir, "model").apply { if (!exists()) mkdirs() }

    val modelFile: File
        get() = File(modelDir, Config.MODEL_FILE_NAME)

    private val partFile: File
        get() = File(modelDir, "${Config.MODEL_FILE_NAME}.part")

    fun isModelReady(): Boolean = modelFile.exists() && modelFile.length() > 0

    fun deleteModel() {
        if (modelFile.exists()) modelFile.delete()
        if (partFile.exists()) partFile.delete()
    }

    /**
     * Downloads the model with resume support: if a previous attempt was
     * interrupted, this continues from where it left off using an HTTP
     * Range request.
     */
    fun downloadModel(): Flow<DownloadState> = flow {
        if (isModelReady()) {
            emit(DownloadState.Completed)
            return@flow
        }

        emit(DownloadState.InProgress(0, 0, 0))

        try {
            val existingBytes = if (partFile.exists()) partFile.length() else 0L

            val requestBuilder = Request.Builder().url(Config.MODEL_DOWNLOAD_URL)
            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Failed("Server javobi: HTTP ${response.code}"))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(DownloadState.Failed("Bo'sh javob serverdan"))
                    return@flow
                }

                val isResumed = response.code == 206
                val startOffset = if (isResumed) existingBytes else 0L
                val contentLength = body.contentLength()
                val totalBytes = if (contentLength > 0) startOffset + contentLength else Config.MODEL_EXPECTED_SIZE_BYTES

                if (!isResumed && partFile.exists()) partFile.delete()

                RandomAccessFile(partFile, "rw").use { raf ->
                    raf.seek(startOffset)
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = startOffset
                        var lastEmitPercent = -1

                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            raf.write(buffer, 0, read)
                            downloaded += read

                            val percent = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                            if (percent != lastEmitPercent) {
                                lastEmitPercent = percent
                                emit(DownloadState.InProgress(percent, downloaded, totalBytes))
                            }
                        }
                    }
                }
            }

            if (!partFile.renameTo(modelFile)) {
                emit(DownloadState.Failed("Faylni saqlab bo'lmadi"))
                return@flow
            }

            emit(DownloadState.Completed)
        } catch (e: Exception) {
            emit(DownloadState.Failed(e.message ?: "Noma'lum xatolik yuz berdi"))
        }
    }.flowOn(Dispatchers.IO)
}
