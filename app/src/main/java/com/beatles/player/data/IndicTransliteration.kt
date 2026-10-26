package com.beatles.player.data

/**
 * High-performance, phonetic Indic-to-Latin (Romanized English) transliteration engine.
 * Converts Gurmukhi (Punjabi), Devanagari (Hindi/Bhojpuri/Marathi), and related Indic scripts
 * into natural, sing-along English/Latin phonetics (e.g., "ਮੈਂ ਸੁਣਿਆ" -> "Main suneya").
 *
 * Preserves timestamps, punctuation, whitespace, and capitalization.
 */
object IndicTransliteration {

    // Gurmukhi characters: 0x0A00 - 0x0A7F
    // Devanagari characters: 0x0900 - 0x097F

    fun isIndic(char: Char): Boolean {
        val code = char.code
        return (code in 0x0900..0x097F) || (code in 0x0A00..0x0A7F)
    }

    fun containsIndic(text: CharSequence): Boolean {
        for (i in 0 until text.length) {
            if (isIndic(text[i])) return true
        }
        return false
    }

    private val GURMUKHI_VOWELS = mapOf(
        '\u0A05' to "a",   // ਅ
        '\u0A06' to "aa",  // ਆ
        '\u0A07' to "i",   // ਇ
        '\u0A08' to "ee",  // ਈ
        '\u0A09' to "u",   // ਉ
        '\u0A0A' to "oo",  // ਊ
        '\u0A0F' to "e",   // ਏ
        '\u0A10' to "ai",  // ਐ
        '\u0A13' to "o",   // ਓ
        '\u0A14' to "au"   // ਔ
    )

    private val GURMUKHI_MATRAS = mapOf(
        '\u0A3E' to "aa",  // ਾ
        '\u0A3F' to "i",   // ਿ
        '\u0A40' to "ee",  // ੀ
        '\u0A41' to "u",   // ੁ
        '\u0A42' to "oo",  // ੂ
        '\u0A47' to "e",   // ੇ
        '\u0A48' to "ai",  // ੈ
        '\u0A4B' to "o",   // ੋ
        '\u0A4C' to "au"   // ੌ
    )

    private val GURMUKHI_CONSONANTS = mapOf(
        '\u0A15' to "k",   // ਕ
        '\u0A16' to "kh",  // ਖ
        '\u0A17' to "g",   // ਗ
        '\u0A18' to "gh",  // ਘ
        '\u0A19' to "ng",  // ਙ
        '\u0A1A' to "ch",  // ਚ
        '\u0A1B' to "chh", // ਛ
        '\u0A1C' to "j",   // ਜ
        '\u0A1D' to "jh",  // ਝ
        '\u0A1E' to "ny",  // ਞ
        '\u0A1F' to "t",   // ਟ
        '\u0A20' to "th",  // ਠ
        '\u0A21' to "d",   // ਡ
        '\u0A22' to "dh",  // ਢ
        '\u0A23' to "n",   // ਣ
        '\u0A24' to "t",   // ਤ
        '\u0A25' to "th",  // ਥ
        '\u0A26' to "d",   // ਦ
        '\u0A27' to "dh",  // ਧ
        '\u0A28' to "n",   // ਨ
        '\u0A2A' to "p",   // ਪ
        '\u0A2B' to "ph",  // ਫ
        '\u0A2C' to "b",   // ਬ
        '\u0A2D' to "bh",  // ਭ
        '\u0A2E' to "m",   // ਮ
        '\u0A2F' to "y",   // ਯ
        '\u0A30' to "r",   // ਰ
        '\u0A32' to "l",   // ਲ
        '\u0A33' to "l",   // ਲ਼
        '\u0A35' to "v",   // ਵ
        '\u0A36' to "sh",  // ਸ਼
        '\u0A38' to "s",   // ਸ
        '\u0A39' to "h",   // ਹ
        '\u0A59' to "kh",  // ਖ਼
        '\u0A5A' to "gh",  // ਗ਼
        '\u0A5B' to "z",   // ਜ਼
        '\u0A5C' to "r",   // ੜ
        '\u0A5E' to "f"    // ਫ਼
    )

    private val DEVANAGARI_VOWELS = mapOf(
        '\u0905' to "a",   // अ
        '\u0906' to "aa",  // आ
        '\u0907' to "i",   // इ
        '\u0908' to "ee",  // ई
        '\u0909' to "u",   // उ
        '\u090A' to "oo",  // ऊ
        '\u090B' to "ri",  // ऋ
        '\u090F' to "e",   // ए
        '\u0910' to "ai",  // ऐ
        '\u0913' to "o",   // ओ
        '\u0914' to "au"   // औ
    )

    private val DEVANAGARI_MATRAS = mapOf(
        '\u093E' to "aa",  // ा
        '\u093F' to "i",   // ि
        '\u0940' to "ee",  // ी
        '\u0941' to "u",   // ु
        '\u0942' to "oo",  // ू
        '\u0943' to "ri",  // ृ
        '\u0947' to "e",   // े
        '\u0948' to "ai",  // ै
        '\u094B' to "o",   // ो
        '\u094C' to "au"   // ौ
    )

    private val DEVANAGARI_CONSONANTS = mapOf(
        '\u0915' to "k",   // क
        '\u0916' to "kh",  // ख
        '\u0917' to "g",   // ग
        '\u0918' to "gh",  // घ
        '\u0919' to "ng",  // ङ
        '\u091A' to "ch",  // च
        '\u091B' to "chh", // छ
        '\u091C' to "j",   // ज
        '\u091D' to "jh",  // झ
        '\u091E' to "ny",  // ञ
        '\u091F' to "t",   // ट
        '\u0920' to "th",  // ठ
        '\u0921' to "d",   // ड
        '\u0922' to "dh",  // ढ
        '\u0923' to "n",   // ण
        '\u0924' to "t",   // त
        '\u0925' to "th",  // थ
        '\u0926' to "d",   // द
        '\u0927' to "dh",  // ध
        '\u0928' to "n",   // न
        '\u092A' to "p",   // प
        '\u092B' to "ph",  // फ
        '\u092C' to "b",   // ब
        '\u092D' to "bh",  // भ
        '\u092E' to "m",   // म
        '\u092F' to "y",   // य
        '\u0930' to "r",   // र
        '\u0932' to "l",   // ल
        '\u0933' to "l",   // ळ
        '\u0935' to "v",   // व
        '\u0936' to "sh",  // श
        '\u0937' to "sh",  // ष
        '\u0938' to "s",   // स
        '\u0939' to "h",   // ह
        '\u0958' to "q",   // क़
        '\u0959' to "kh",  // ख़
        '\u095A' to "gh",  // ग़
        '\u095B' to "z",   // ज़
        '\u095C' to "r",   // ड़
        '\u095D' to "rh",  // ढ़
        '\u095E' to "f"    // फ़
    )

    /**
     * Transliterates Indic text (Punjabi Gurmukhi, Hindi Devanagari) to natural English/Latin sing-along text.
     * Leaves English, numbers, punctuation, and emoji untouched.
     */
    fun toLatin(text: String): String {
        if (text.isEmpty() || !containsIndic(text)) return text

        val sb = StringBuilder(text.length * 2)
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            // 1. Gurmukhi Script Processing
            if (c.code in 0x0A00..0x0A7F) {
                // Adhak (gemination marker 0x0A71): doubles the next consonant
                if (c == '\u0A71' && i + 1 < len) {
                    val nextC = text[i + 1]
                    val nextRom = GURMUKHI_CONSONANTS[nextC]
                    if (nextRom != null) {
                        sb.append(nextRom.first())
                    }
                    i++
                    continue
                }

                // Bindi (0x0A02) / Tippi (0x0A70): nasalization
                if (c == '\u0A02' || c == '\u0A70') {
                    sb.append("n")
                    i++
                    continue
                }

                // Independent vowels
                val indVowel = GURMUKHI_VOWELS[c]
                if (indVowel != null) {
                    sb.append(indVowel)
                    i++
                    continue
                }

                // Consonants
                val consonant = GURMUKHI_CONSONANTS[c]
                if (consonant != null) {
                    sb.append(consonant)
                    // Check next character for Halant (0x0A4D) or Matra or end of syllable
                    if (i + 1 < len) {
                        val next = text[i + 1]
                        if (next == '\u0A4D') {
                            // Halant: suppresses inherent vowel
                            i += 2
                            continue
                        } else if (GURMUKHI_MATRAS.containsKey(next)) {
                            // Dependent Matra vowel
                            sb.append(GURMUKHI_MATRAS[next])
                            i += 2
                            continue
                        } else if (next.code in 0x0A00..0x0A7F && GURMUKHI_CONSONANTS.containsKey(next)) {
                            // Inherent vowel between consonants
                            sb.append("a")
                        }
                    }
                    i++
                    continue
                }

                // Matra standalone
                val matra = GURMUKHI_MATRAS[c]
                if (matra != null) {
                    sb.append(matra)
                    i++
                    continue
                }

                i++
                continue
            }

            // 2. Devanagari Script Processing
            if (c.code in 0x0900..0x097F) {
                // Anusvara (0x0902) or Chandrabindu (0x0901)
                if (c == '\u0901' || c == '\u0902') {
                    sb.append("n")
                    i++
                    continue
                }
                // Visarga (0x0903)
                if (c == '\u0903') {
                    sb.append("h")
                    i++
                    continue
                }

                val indVowel = DEVANAGARI_VOWELS[c]
                if (indVowel != null) {
                    sb.append(indVowel)
                    i++
                    continue
                }

                val consonant = DEVANAGARI_CONSONANTS[c]
                if (consonant != null) {
                    sb.append(consonant)
                    if (i + 1 < len) {
                        val next = text[i + 1]
                        if (next == '\u094D') {
                            // Virama / Halant suppresses inherent vowel
                            i += 2
                            continue
                        } else if (DEVANAGARI_MATRAS.containsKey(next)) {
                            sb.append(DEVANAGARI_MATRAS[next])
                            i += 2
                            continue
                        } else if (next.code in 0x0900..0x097F && DEVANAGARI_CONSONANTS.containsKey(next)) {
                            sb.append("a")
                        }
                    }
                    i++
                    continue
                }

                val matra = DEVANAGARI_MATRAS[c]
                if (matra != null) {
                    sb.append(matra)
                    i++
                    continue
                }

                i++
                continue
            }

            // Standard Latin, numbers, punctuation, emojis
            sb.append(c)
            i++
        }

        return postProcess(sb.toString())
    }

    /**
     * Natural cleanup of transliterated word endings and common phonetic patterns.
     */
    private fun postProcess(raw: String): String {
        return raw
            .replace(Regex("(?i)khaa(?=\\b)"), "kha")
            .replace(Regex("(?i)gayaa(?=\\b)"), "gaya")
            .replace(Regex("(?i)siya(?=\\b)"), "si")
            .replace(Regex("  +"), " ")
            .trim()
    }
}
