package com.jarvisauth.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingBeaconTest {
    private val encKey = "beacon-enc-key"
    private val clock = EpochClock { 1_700_000_000_000L }

    @Test
    fun decryptsAesBeaconMatchingDesktopLayout() {
        val json = ProtocolJson.encode(PacketUdpPairBeacon(serverId = "Ab12CdEf", ip = "10.0.0.8", port = 43295))
        val blob =
            (AesGcmPacket.encrypt(json.toByteArray(Charsets.UTF_8), encKey, clock) as CryptResult.Ok).data
        val beacon = PairingBeacon.decrypt(blob, encKey, clock)!!
        assertEquals("Ab12CdEf", beacon.serverId)
        assertEquals("10.0.0.8", beacon.ip)
        assertEquals(43295, beacon.port)
    }

    @Test
    fun rejectsWrongKey() {
        val json = ProtocolJson.encode(PacketUdpPairBeacon(serverId = "x", ip = "1.1.1.1", port = 1))
        val blob =
            (AesGcmPacket.encrypt(json.toByteArray(Charsets.UTF_8), encKey, clock) as CryptResult.Ok).data
        assertNull(PairingBeacon.decrypt(blob, "other-key", clock))
    }
}
