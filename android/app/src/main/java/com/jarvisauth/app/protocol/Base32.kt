package com.jarvisauth.app.protocol

import java.io.ByteArrayOutputStream

/**
 * Unpadded Base32 matching desktop `StringUtils::ToBase32String`
 * (alphabet ABCDEFGHIJKLMNOPQRSTUVWXYZ234567, no RFC padding).
 */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(data: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                out.append(ALPHABET[(buffer ushr (bitsLeft - 5)) and 0x1F])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            out.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return out.toString()
    }

    fun decode(text: String): ByteArray? {
        val cleaned =
            buildString {
                for (c in text) {
                    if (c == '-' || c.isWhitespace()) continue
                    append(c.uppercaseChar())
                }
            }
        if (cleaned.isEmpty()) return null
        val out = ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (c in cleaned) {
            val v = ALPHABET.indexOf(c)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                out.write((buffer ushr (bits - 8)) and 0xFF)
                bits -= 8
            }
        }
        return out.toByteArray()
    }

    fun withSeparators(
        encoded: String,
        groupSize: Int = 5,
        separator: String = "-",
    ): String {
        if (groupSize <= 0) return encoded
        return encoded.chunked(groupSize).joinToString(separator)
    }
}
