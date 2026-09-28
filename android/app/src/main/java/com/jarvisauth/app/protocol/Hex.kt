package com.jarvisauth.app.protocol

object Hex {
    private const val DIGITS = "0123456789ABCDEF"

    fun encode(data: ByteArray): String {
        val out = StringBuilder(data.size * 2)
        for (b in data) {
            val v = b.toInt() and 0xFF
            out.append(DIGITS[v ushr 4])
            out.append(DIGITS[v and 0x0F])
        }
        return out.toString()
    }

    fun decode(hex: String): ByteArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) {
            return null
        }
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = hex[i * 2].digitToIntOrNull(16) ?: return null
            val lo = hex[i * 2 + 1].digitToIntOrNull(16) ?: return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
