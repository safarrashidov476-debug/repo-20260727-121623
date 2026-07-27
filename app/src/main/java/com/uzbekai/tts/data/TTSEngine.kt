package com.uzbekai.tts.data

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.uzbekai.tts.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer

class TTSEngine(private val context: Context, private val modelManager: ModelManager) {

    private var ortEnvironment: OrtEnvironment? = null
    private var session: OrtSession? = null
    private val tokenizer by lazy { Tokenizer() }

    private fun ensureSessionLoaded() {
        if (session != null) return
        if (!modelManager.isModelReady()) {
            throw IllegalStateException("Model hali yuklab olinmagan")
        }
        val env = OrtEnvironment.getEnvironment()
        val options = OrtSession.SessionOptions()
        val newSession = env.createSession(modelManager.modelFile.absolutePath, options)
        ortEnvironment = env
        session = newSession
    }

    /**
     * Synthesizes [text] into a WAV file, saved under the app's files/audio
     * directory, and returns the resulting File.
     *
     * lengthScale (speaking_rate): higher = slower speech (default 1.0)
     * noiseScale (temperature): flow-matching noise variance (default 0.667)
     */
    suspend fun synthesizeToFile(
        text: String,
        lengthScale: Float = Config.DEFAULT_LENGTH_SCALE,
        noiseScale: Float = Config.DEFAULT_NOISE_SCALE
    ): File = withContext(Dispatchers.Default) {
        ensureSessionLoaded()
        val env = ortEnvironment ?: throw IllegalStateException("Model muhiti ishga tushmagan")
        val activeSession = session ?: throw IllegalStateException("Model sessiyasi ishga tushmagan")

        val ids = tokenizer.textToIds(text)
        if (ids.isEmpty()) throw IllegalArgumentException("Matn bo'sh bo'lishi mumkin emas")

        // "x": int64 [1, seq_len]
        val tokenBuffer = LongBuffer.allocate(ids.size)
        ids.forEach { tokenBuffer.put(it.toLong()) }
        tokenBuffer.rewind()
        val tokenTensor = OnnxTensor.createTensor(env, tokenBuffer, longArrayOf(1, ids.size.toLong()))

        // "x_lengths": int64 [1]
        val lengthsBuffer = LongBuffer.allocate(1)
        lengthsBuffer.put(ids.size.toLong())
        lengthsBuffer.rewind()
        val lengthsTensor = OnnxTensor.createTensor(env, lengthsBuffer, longArrayOf(1))

        // "scales": float32 [2] = [noise_scale (temperature), length_scale (speaking_rate)]
        val scalesBuffer = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        scalesBuffer.put(noiseScale)
        scalesBuffer.put(lengthScale)
        scalesBuffer.rewind()
        val scalesTensor = OnnxTensor.createTensor(env, scalesBuffer, longArrayOf(2))

        val inputs = mutableMapOf(
            Config.MODEL_INPUT_TOKENS to tokenTensor,
            Config.MODEL_INPUT_LENGTHS to lengthsTensor,
            Config.MODEL_INPUT_SCALES to scalesTensor
        )

        // Multi-speaker models expose a 4th input ("spks"); single-speaker models expose 3.
        val speakerTensor: OnnxTensor? = if (activeSession.inputNames.size == 4) {
            val spksBuffer = LongBuffer.allocate(1)
            spksBuffer.put(Config.DEFAULT_SPEAKER_ID)
            spksBuffer.rewind()
            OnnxTensor.createTensor(env, spksBuffer, longArrayOf(1)).also {
                inputs[Config.MODEL_INPUT_SPEAKER] = it
            }
        } else null

        val outputNamesOrdered = activeSession.outputNames.toList()

        var rawSamples = FloatArray(0)
        var trimLength = -1

        activeSession.run(inputs).use { results ->
            val wavOnnxValue = outputNamesOrdered.getOrNull(0)?.let { results.get(it).orElse(null) }
            val lengthOnnxValue = outputNamesOrdered.getOrNull(1)?.let { results.get(it).orElse(null) }

            val wavValue = (wavOnnxValue as? OnnxTensor)?.value
            rawSamples = extractFloatSamples(wavValue)

            val lengthValue = (lengthOnnxValue as? OnnxTensor)?.value
            trimLength = extractFirstLength(lengthValue)
        }

        tokenTensor.close()
        lengthsTensor.close()
        scalesTensor.close()
        speakerTensor?.close()

        if (rawSamples.isEmpty()) {
            throw IllegalStateException("Model bo'sh audio qaytardi")
        }

        val finalLength = if (trimLength in 1..rawSamples.size) trimLength else rawSamples.size
        val samples = if (finalLength == rawSamples.size) rawSamples else rawSamples.copyOfRange(0, finalLength)

        val outFile = newAudioFile()
        writeWavFile(outFile, samples, Config.SAMPLE_RATE)
        outFile
    }

    /** Handles both [batch][samples] and flat [samples] tensor shapes for the wav output. */
    private fun extractFloatSamples(value: Any?): FloatArray = when (value) {
        is FloatArray -> value
        is Array<*> -> when (val first = value.firstOrNull()) {
            is FloatArray -> first
            is Array<*> -> (first.firstOrNull() as? FloatArray) ?: FloatArray(0)
            else -> FloatArray(0)
        }
        else -> FloatArray(0)
    }

    /** Handles [batch] or scalar tensor shapes for the wav_lengths output. */
    private fun extractFirstLength(value: Any?): Int = when (value) {
        is LongArray -> value.firstOrNull()?.toInt() ?: -1
        is IntArray -> value.firstOrNull() ?: -1
        is Long -> value.toInt()
        is Int -> value
        is Array<*> -> when (val first = value.firstOrNull()) {
            is Long -> first.toInt()
            is Int -> first
            is LongArray -> first.firstOrNull()?.toInt() ?: -1
            is IntArray -> first.firstOrNull() ?: -1
            else -> -1
        }
        else -> -1
    }

    private fun newAudioFile(): File {
        val dir = File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }
        val name = "tts_${System.currentTimeMillis()}.wav"
        return File(dir, name)
    }

    /** Writes 32-bit float PCM samples as a 16-bit PCM WAV file. */
    private fun writeWavFile(file: File, samples: FloatArray, sampleRate: Int) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = samples.size * 2
        val chunkSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(chunkSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1)
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray())
            header.putInt(dataSize)
            fos.write(header.array())

            val pcmBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in samples) {
                val clamped = sample.coerceIn(-1f, 1f)
                pcmBuffer.putShort((clamped * Short.MAX_VALUE).toInt().toShort())
            }
            fos.write(pcmBuffer.array())
        }
    }

    fun close() {
        session?.close()
        session = null
    }
}
