package com.jarvisauth.app.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PacketStreamTest {
    @Test
    fun writeThenReadRoundTrip() {
        val payload = "hello-pair".toByteArray()
        val out = ByteArrayOutputStream()
        PacketStream.write(out, PacketIds.PAIR_INIT, payload)
        val framed = PacketStream.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(PacketIds.PAIR_INIT, framed.id)
        assertArrayEquals(payload, framed.payload)
    }

    @Test
    fun skipsGarbageBeforeHeader() {
        val payload = byteArrayOf(1, 2, 3)
        val packet = PacketCodec.encode(PacketIds.PAIR_RESPONSE, payload)
        val noise = byteArrayOf(0x00, 0x11, 0xDB.toByte(), 0x00)
        val framed = PacketStream.read(ByteArrayInputStream(noise + packet))
        assertEquals(PacketIds.PAIR_RESPONSE, framed.id)
        assertArrayEquals(payload, framed.payload)
    }
}
