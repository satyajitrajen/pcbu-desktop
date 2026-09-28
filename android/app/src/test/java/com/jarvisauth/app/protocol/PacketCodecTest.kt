package com.jarvisauth.app.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PacketCodecTest {
    @Test
    fun roundTrip() {
        val payload = "device-id".toByteArray(Charsets.UTF_8)
        val encoded = PacketCodec.encode(PacketIds.DEVICE_ID, payload)
        val decoded = PacketCodec.decode(encoded)!!
        assertEquals(PacketIds.DEVICE_ID, decoded.id)
        assertArrayEquals(payload, decoded.payload)
        val asText = payload.toString(Charsets.UTF_8)
        assertEquals("device-id", asText)
        assertEquals(false, asText.trimStart().startsWith("{"))
    }

    @Test
    fun headerIsDesktopMagic() {
        assertArrayEquals(
            byteArrayOf(
                0xDB.toByte(),
                0x06,
                0x5A,
                0xC7.toByte(),
                0xAF.toByte(),
                0xDF.toByte(),
                0xA4.toByte(),
                0xCC.toByte(),
            ),
            PacketCodec.HEADER_BYTES,
        )
    }

    @Test
    fun skipsGarbageBeforeHeader() {
        val packet = PacketCodec.encode(PacketIds.PAIR_INIT, byteArrayOf(1, 2, 3))
        val noise = byteArrayOf(0x00, 0x11, 0xDB.toByte(), 0x00)
        val decoded = PacketCodec.decode(noise + packet)!!
        assertEquals(PacketIds.PAIR_INIT, decoded.id)
        assertArrayEquals(byteArrayOf(1, 2, 3), decoded.payload)
    }

    @Test
    fun truncatedPacketIsNull() {
        val packet = PacketCodec.encode(PacketIds.UNLOCK_REQUEST, ByteArray(8))
        assertNull(PacketCodec.decode(packet.copyOf(packet.size - 1)))
    }

    @Test
    fun emptyPayloadRejectedOnDecode() {
        val header = PacketCodec.HEADER_BYTES
        val idAndLen = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00)
        assertNull(PacketCodec.decode(header + idAndLen))
    }
}
