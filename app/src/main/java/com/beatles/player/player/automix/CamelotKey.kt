package com.beatles.player.player.automix

import kotlin.math.abs

/**
 * Camelot Harmonic Key System for DJ mixing.
 *
 * Keys on the Camelot wheel:
 *  - 1A (Abm) through 12A (Dbm) for Minor keys
 *  - 1B (B) through 12B (E) for Major keys
 */
data class CamelotKey(
    val number: Int, // 1..12
    val letter: Char // 'A' or 'B'
) {
    override fun toString(): String = "$number$letter"

    companion object {
        fun from(tonic: String, scale: String): CamelotKey {
            val isMinor = scale.equals("MINOR", ignoreCase = true) || tonic.endsWith("m")
            val cleanTonic = tonic.replace("m", "").trim().uppercase()

            return if (isMinor) {
                when (cleanTonic) {
                    "AB", "G#" -> CamelotKey(1, 'A')
                    "EB", "D#" -> CamelotKey(2, 'A')
                    "BB", "A#" -> CamelotKey(3, 'A')
                    "F" -> CamelotKey(4, 'A')
                    "C" -> CamelotKey(5, 'A')
                    "G" -> CamelotKey(6, 'A')
                    "D" -> CamelotKey(7, 'A')
                    "A" -> CamelotKey(8, 'A')
                    "E" -> CamelotKey(9, 'A')
                    "B" -> CamelotKey(10, 'A')
                    "F#", "GB" -> CamelotKey(11, 'A')
                    "C#", "DB" -> CamelotKey(12, 'A')
                    else -> CamelotKey(8, 'A')
                }
            } else {
                when (cleanTonic) {
                    "B" -> CamelotKey(1, 'B')
                    "F#", "GB" -> CamelotKey(2, 'B')
                    "C#", "DB" -> CamelotKey(3, 'B')
                    "AB", "G#" -> CamelotKey(4, 'B')
                    "EB", "D#" -> CamelotKey(5, 'B')
                    "BB", "A#" -> CamelotKey(6, 'B')
                    "F" -> CamelotKey(7, 'B')
                    "C" -> CamelotKey(8, 'B')
                    "G" -> CamelotKey(9, 'B')
                    "D" -> CamelotKey(10, 'B')
                    "A" -> CamelotKey(11, 'B')
                    "E" -> CamelotKey(12, 'B')
                    else -> CamelotKey(8, 'B')
                }
            }
        }

        /**
         * Checks harmonic compatibility between two keys:
         * 1. Same key (e.g. 8A -> 8A)
         * 2. Adjacent key on Camelot wheel (e.g. 8A -> 7A or 9A)
         * 3. Relative major/minor (e.g. 8A -> 8B)
         */
        fun areHarmonicallyCompatible(key1: CamelotKey, key2: CamelotKey): Boolean {
            if (key1 == key2) return true
            if (key1.number == key2.number && key1.letter != key2.letter) return true
            if (key1.letter == key2.letter) {
                val diff = abs(key1.number - key2.number)
                return diff == 1 || diff == 11
            }
            return false
        }
    }
}
