package com.uzbekai.tts.data

/**
 * Direct Kotlin port of Ovozify-Labs/text-to-speech-ui's symbols.py +
 * cleaners.py (basic_cleaners) + infer_utils.py (text_to_sequence, intersperse).
 *
 * Ported faithfully so the resulting integer id sequence matches exactly
 * what the Python backend feeds into the ONNX model. If the upstream repo's
 * symbols.py / cleaners.py changes, mirror those changes here too.
 */
private object Symbols {
    private const val PAD = '_'

    // From symbols.py: _punctuation = ';:,.!?¡¿—…"«»"" ' + "'"
    private val PUNCTUATION = charArrayOf(
        ';', ':', ',', '.', '!', '?', '¡', '¿', '—', '…', '"', '«', '»', '\u201C', '\u201D', ' ', '\''
    )

    // From symbols.py: _letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
    private val LETTERS = ('A'..'Z') + ('a'..'z')

    // symbols = [_pad] + list(_punctuation) + list(_letters) + list(_letters_ipa)
    // IPA letters are intentionally omitted here: basic_cleaners() never produces
    // IPA output (the phonemizer call is commented out upstream), so they don't
    // affect any id actually used at runtime, and they come after LETTERS in the
    // upstream list so omitting them doesn't shift earlier ids.
    private val ALL_USED_SYMBOLS: List<Char> = listOf(PAD) + PUNCTUATION.toList() + LETTERS

    val symbolToId: Map<Char, Int> = ALL_USED_SYMBOLS.mapIndexed { index, c -> c to index }.toMap()
    val padId: Int = symbolToId.getValue(PAD)
}

/**
 * Port of cleaners.basic_cleaners + the number-to-Uzbek-words helper.
 */
private object Cleaners {

    private val ones = mapOf(
        1 to "bir", 2 to "ikki", 3 to "uch", 4 to "to'rt", 5 to "besh",
        6 to "olti", 7 to "yetti", 8 to "sakkiz", 9 to "to'qqiz"
    )
    private val tens = mapOf(
        10 to "o'n", 20 to "yigirma", 30 to "o'ttiz", 40 to "qirq", 50 to "ellik",
        60 to "oltmish", 70 to "yetmish", 80 to "sakson", 90 to "to'qson"
    )
    private val scales = listOf(1_000_000_000L to "milliard", 1_000_000L to "million", 1_000L to "ming")

    private fun threeDigitToUzbek(n: Int): String {
        require(n in 0..999)
        val parts = mutableListOf<String>()
        val hundreds = n / 100
        val remainder = n % 100
        if (hundreds > 0) {
            parts += if (hundreds == 1) "yuz" else "${ones.getValue(hundreds)} yuz"
        }
        if (remainder != 0) {
            when {
                remainder < 10 -> parts += ones.getValue(remainder)
                tens.containsKey(remainder) -> parts += tens.getValue(remainder)
                else -> {
                    val tenPart = (remainder / 10) * 10
                    val onePart = remainder % 10
                    if (tenPart > 0) parts += tens.getValue(tenPart)
                    if (onePart > 0) parts += ones.getValue(onePart)
                }
            }
        }
        return parts.joinToString(" ").trim()
    }

    fun numberToUzbek(num: Long): String {
        if (num == 0L) return "nol"
        var current = num
        val words = mutableListOf<String>()
        for ((scaleValue, scaleName) in scales) {
            if (current == 0L) break
            val chunk = (current / scaleValue).toInt()
            current %= scaleValue
            if (chunk == 0) continue
            words += "${threeDigitToUzbek(chunk)} $scaleName"
        }
        if (current > 0) words += threeDigitToUzbek(current.toInt())
        return words.joinToString(" ").trim()
    }

    private fun convertNumbersToUzbek(text: String): String {
        val regex = Regex("\\d+")
        return regex.replace(text) { match ->
            match.value.toLongOrNull()?.let { numberToUzbek(it) } ?: match.value
        }
    }

    /** Direct port of cleaners.basic_cleaners */
    fun basicCleaners(input: String): String {
        var text = input

        // text = re.sub(r'(?<!s)x', 'h', text); re.sub(r'(?<!s)X', 'H', text)
        text = Regex("(?<!s)x").replace(text, "h")
        text = Regex("(?<!s)X").replace(text, "H")

        // Curly single quotes -> straight apostrophe
        text = text.replace('\u2018', '\'').replace('\u2019', '\'')

        // Keep only alnum, whitespace, and ",.?!'"
        val allowedPunct = setOf(',', '.', '?', '!', '\'')
        text = text.filter { ch -> ch.isLetterOrDigit() || ch.isWhitespace() || ch in allowedPunct }

        text = convertNumbersToUzbek(text)
        text = text.lowercase()
        text = Regex("\\s+").replace(text, " ")

        return text
    }

    /** Adds the pad/blank symbol between every token and at both ends. */
    fun intersperse(ids: List<Int>, padId: Int): List<Int> {
        val result = MutableList(ids.size * 2 + 1) { padId }
        for (i in ids.indices) result[2 * i + 1] = ids[i]
        return result
    }
}

class Tokenizer {

    /** Converts raw input text into the exact id sequence the ONNX model expects for input "x". */
    fun textToIds(text: String): IntArray {
        val cleaned = Cleaners.basicCleaners(text)
        val ids = cleaned.mapNotNull { Symbols.symbolToId[it] }
        return Cleaners.intersperse(ids, Symbols.padId).toIntArray()
    }
}
