package com.jarvisauth.app.protocol

import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

object PacketStream {
    fun write(
        output: OutputStream,
        id: Int,
        payload: ByteArray,
    ) {
        output.write(PacketCodec.encode(id, payload))
        output.flush()
    }

    fun read(input: InputStream): FramedPacket {
        val header = PacketCodec.HEADER_BYTES
        var matched = 0
        while (matched < header.size) {
            val b = input.read()
            if (b < 0) throw EOFException("socket closed while reading header")
            val byte = b.toByte()
            if (byte == header[matched]) {
                matched++
            } else {
                matched = if (byte == header[0]) 1 else 0
            }
        }
        val idLen = readFully(input, 4)
        val id = ((idLen[0].toInt() and 0xFF) shl 8) or (idLen[1].toInt() and 0xFF)
        val length = ((idLen[2].toInt() and 0xFF) shl 8) or (idLen[3].toInt() and 0xFF)
        if (length == 0) throw IllegalArgumentException("empty packet")
        val payload = readFully(input, length)
        return FramedPacket(id, payload)
    }

    private fun readFully(
        input: InputStream,
        size: Int,
    ): ByteArray {
        val buf = ByteArray(size)
        var n = 0
        while (n < size) {
            val r = input.read(buf, n, size - n)
            if (r < 0) throw EOFException("socket closed while reading packet")
            n += r
        }
        return buf
    }
}
