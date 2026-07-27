package com.uzbekai.tts

/**
 * Text preprocessing matched against Ovozify-Labs/text-to-speech-ui
 * (onnx_infer.py, infer_utils.py, symbols.py, cleaners.py).
 * Model weights: OvozifyLabs/matcha-tts-uz-v1 on Hugging Face (model.onnx, ~130 MB).
 */
object Config {

    // Confirmed: OvozifyLabs/matcha-tts-uz-v1 on Hugging Face (model.onnx, ~130 MB).
    const val MODEL_DOWNLOAD_URL = "https://huggingface.co/OvozifyLabs/matcha-tts-uz-v1/resolve/main/model.onnx"

    const val MODEL_FILE_NAME = "matcha_tts_uzbek_male.onnx"

    // ~130 MB per the Hugging Face file listing, used only for a sanity check after download.
    const val MODEL_EXPECTED_SIZE_BYTES = 130L * 1024 * 1024

    // Confirmed from onnx_infer.py: inputs are exactly "x", "x_lengths", "scales",
    // plus "spks" only when the model is multi-speaker (4 total inputs).
    const val MODEL_INPUT_TOKENS = "x"
    const val MODEL_INPUT_LENGTHS = "x_lengths"
    const val MODEL_INPUT_SCALES = "scales"
    const val MODEL_INPUT_SPEAKER = "spks"
    const val DEFAULT_SPEAKER_ID = 0L

    // Confirmed from onnx_infer.py: `wavs, wav_lengths = model.run(None, inputs)` —
    // exactly 2 outputs, read positionally (their exact names weren't given in the
    // source, so TTSEngine reads session.outputNames in order rather than by name).

    const val SAMPLE_RATE = 22050

    // scales = [noise_scale (temperature), length_scale (speaking_rate)]
    // Confirmed defaults from onnx_infer.py argparse.
    const val DEFAULT_NOISE_SCALE = 0.667f
    const val DEFAULT_LENGTH_SCALE = 1.0f
}

