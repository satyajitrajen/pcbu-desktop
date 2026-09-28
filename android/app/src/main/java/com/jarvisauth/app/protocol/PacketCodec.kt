package com.jarvisauth.app.protocol

data class FramedPacket(
    val id: Int,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FramedPacket) return false
        return id == other.id && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int = 31 * id + payload.contentHashCode()
}

object PacketCodec {
    val HEADER_BYTES: ByteArray =
        byteArrayOf(
            0xDB.toByte(),
            0x06,
            0x5A,
            0xC7.toByte(),
            0xAF.toByte(),
            0xDF.toByte(),
            0xA4.toByte(),
            0xCC.toByte(),
        )

    fun encode(
        id: Int,
        payload: ByteArray,
    ): ByteArray {
        require(id in 0..0xFFFF)
        require(payload.size <= 0xFFFF)
        val out = ByteArray(HEADER_BYTES.size + 2 + 2 + payload.size)
        HEADER_BYTES.copyInto(out, 0)
        writeU16Be(out, HEADER_BYTES.size, id)
        writeU16Be(out, HEADER_BYTES.size + 2, payload.size)
        payload.copyInto(out, HEADER_BYTES.size + 4)
        return out
    }

    fun decode(buffer: ByteArray): FramedPacket? {
        val headerAt = indexOfHeader(buffer) ?: return null
        val idAt = headerAt + HEADER_BYTES.size
        if (buffer.size < idAt + 4) return null
        val id = readU16Be(buffer, idAt)
        val length = readU16Be(buffer, idAt + 2)
        val dataAt = idAt + 4
        if (length == 0 || buffer.size < dataAt + length) return null
        return FramedPacket(id, buffer.copyOfRange(dataAt, dataAt + length))
    }

    private fun indexOfHeader(buffer: ByteArray): Int? {
        var matched = 0
        for (i in buffer.indices) {
            if (buffer[i] == HEADER_BYTES[matched]) {
                matched++
                if (matched == HEADER_BYTES.size) {
                    return i - HEADER_BYTES.size + 1
                }
            } else {
                matched = if (buffer[i] == HEADER_BYTES[0]) 1 else 0
            }
        }
        return null
    }

    private fun writeU16Be(
        dst: ByteArray,
        offset: Int,
        value: Int,
    ) {
        dst[offset] = ((value ushr 8) and 0xFF).toByte()
        dst[offset + 1] = (value and 0xFF).toByte()
    }

    private fun readU16Be(
        src: ByteArray,
        offset: Int,
    ): Int {
        val hi = src[offset].toInt() and 0xFF
        val lo = src[offset + 1].toInt() and 0xFF
        return (hi shl 8) or lo
    }
}
